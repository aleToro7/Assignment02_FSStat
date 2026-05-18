package it.unibo.fsstat.lib;

import java.io.IOException;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.function.Consumer;

public class FSStatLib {

    // Classe per gestire l'interruzione asincrona
    public static class TaskController {
        private volatile boolean cancelled = false;
        public void cancel() { cancelled = true; }
        public boolean isCancelled() { return cancelled; }
    }

    public CompletableFuture<FSReport> getFSReport(
            String directoryPath, 
            long maxFS, 
            int nb, 
            Consumer<FSReport> onUpdate, 
            TaskController controller) {
        
        // Report condiviso tra tutti i Virtual Threads
        FSReport sharedReport = new FSReport(maxFS, nb);

        return CompletableFuture.supplyAsync(() -> {
            try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
                Path rootPath = Paths.get(directoryPath);
                // Aspettiamo la fine dell'esplorazione dell'intero albero di directory
                processDirectory(rootPath, executor, sharedReport, onUpdate, controller).get();
                return sharedReport.getSnapshot();
            } catch (Exception e) {
                throw new CompletionException(e);
            }
        });
    }

    private Future<Void> processDirectory(
            Path dir, 
            ExecutorService executor, 
            FSReport sharedReport, 
            Consumer<FSReport> onUpdate, 
            TaskController controller) {
        
        return executor.submit(() -> {
            if (controller.isCancelled() || Thread.currentThread().isInterrupted()) return null;

            List<Future<Void>> subTasks = new ArrayList<>();

            try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir)) {
                for (Path entry : stream) {
                    if (controller.isCancelled()) break;

                    if (Files.isDirectory(entry)) {
                        subTasks.add(processDirectory(entry, executor, sharedReport, onUpdate, controller));
                    } else if (Files.isRegularFile(entry)) {
                        sharedReport.addFile(Files.size(entry));
                        
                        try {
                            // Sospende questo Virtual Thread per 100 millisecondi.
                            // Il carrier thread del S.O. viene liberato nel frattempo (unmount).
                            Thread.sleep(100); 
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt(); // Ripristina lo stato di interruzione
                        }

                        if (onUpdate != null && sharedReport.getTotalFiles() % 10 == 0) {
                            onUpdate.accept(sharedReport.getSnapshot());
                        }
                    }
                }
            } catch (IOException ignored) {}

            // Attesa del completamento delle sub-directory o propagazione della cancellazione
            for (Future<Void> task : subTasks) {
                if (controller.isCancelled()) {
                    task.cancel(true);
                } else {
                    try { task.get(); } catch (Exception ignored) {}
                }
            }
            return null;
        });
    }
}