package it.unibo.fsstat.lib;

import java.io.IOException;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.function.Consumer;

public class FSStatLib {

    /** Ogni quante notifiche di nuovi file inviare un aggiornamento tramite onUpdate. */
    private static final int NOTIFY_EVERY_N_FILES = 10;

    // Classe per gestire l'interruzione asincrona
    public static class TaskController {
        private volatile boolean cancelled = false;
        public void cancel() { cancelled = true; }
        public boolean isCancelled() { return cancelled; }
    }

    /**
     * Versione "semplice" richiesta dalla consegna: nessun aggiornamento incrementale,
     * nessuna possibilita' di interruzione. Delega alla versione interattiva.
     */
    public CompletableFuture<FSReport> getFSReport(String directoryPath, long maxFS, int nb) {
        return getFSReport(directoryPath, maxFS, nb, null, new TaskController());
    }

    public CompletableFuture<FSReport> getFSReport(
            String directoryPath,
            long maxFS,
            int nb,
            Consumer<FSReport> onUpdate,
            TaskController controller) {

        TaskController effectiveController = controller != null ? controller : new TaskController();
        // Report condiviso tra tutti i Virtual Thread, protetto da lock interno.
        FSReport sharedReport = new FSReport(maxFS, nb);

        return CompletableFuture.supplyAsync(() -> {
            try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
                Path rootPath = Paths.get(directoryPath);
                // Aspettiamo la fine dell'esplorazione dell'intero albero di directory
                processDirectory(rootPath, executor, sharedReport, onUpdate, effectiveController).get();
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
                        // Per ogni sotto-directory sottomettiamo ricorsivamente un nuovo task.
                        subTasks.add(processDirectory(entry, executor, sharedReport, onUpdate, controller));
                    } else if (Files.isRegularFile(entry)) {
                        // addFile restituisce il nuovo totale in modo atomico: evita la race
                        // che si avrebbe leggendo il contatore separatamente dopo l'incremento.
                        int newTotal = sharedReport.addFile(Files.size(entry));

                        if (onUpdate != null && newTotal % NOTIFY_EVERY_N_FILES == 0) {
                            onUpdate.accept(sharedReport.getSnapshot());
                        }
                    }
                }
            } catch (IOException e) {
                System.err.println("Impossibile accedere a: " + dir + " (" + e.getMessage() + ")");
            }

            // Attesa del completamento delle sub-directory o propagazione della cancellazione
            for (Future<Void> task : subTasks) {
                if (controller.isCancelled()) {
                    task.cancel(true);
                } else {
                    try {
                        task.get();
                    } catch (CancellationException | InterruptedException ignored) {
                        // Interruzione volontaria: nessun errore da segnalare.
                    } catch (ExecutionException e) {
                        System.err.println("Errore durante la scansione di una sottodirectory: " + e.getCause());
                    }
                }
            }
            return null;
        });
    }
}
