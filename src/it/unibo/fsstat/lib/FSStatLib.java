package it.unibo.fsstat.lib;

import java.io.IOException;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

public class FSStatLib {

    /**
     * Calcola asincronamente le statistiche dei file.
     * Restituisce una CompletableFuture per l'approccio asincrono richiesto.
     */
    public CompletableFuture<FSReport> getFSReport(String directoryPath, long maxFS, int nb) {
        // Avviamo l'elaborazione asincrona su un Virtual Thread dedicato.
        return CompletableFuture.supplyAsync(() -> {
            // Creiamo un executor che lancia un Virtual Thread per ogni nuovo task
            try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
                Path rootPath = Paths.get(directoryPath);
                // processDirectory restituisce un Future. Usiamo .get() che non blocca il SO,
                // ma fa l'unmount del Virtual Thread
                return processDirectory(rootPath, maxFS, nb, executor).get();
            } catch (Exception e) {
                throw new CompletionException(e);
            }
            // Il blocco try-with-resources garantisce la chiusura e l'attesa di tutti i task (implicitamente)
        });
    }

    private Future<FSReport> processDirectory(Path dir, long maxFS, int nb, ExecutorService executor) {
        // Submit di un nuovo task (assegnato a un Virtual Thread) per processare questa directory
        return executor.submit(() -> {
            FSReport localReport = new FSReport(maxFS, nb);
            List<Future<FSReport>> subTasks = new ArrayList<>();

            try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir)) {
                for (Path entry : stream) {
                    if (Files.isDirectory(entry)) {
                        // Per ogni sotto-directory sottomettiamo ricorsivamente un nuovo task.
                        subTasks.add(processDirectory(entry, maxFS, nb, executor));
                    } else if (Files.isRegularFile(entry)) {
                        // Operazione di I/O (Files.size): se dovesse bloccarsi,
                        // il virtual thread andrà in unmount automaticamente
                        localReport.addFile(Files.size(entry));
                    }
                }
            } catch (IOException e) {
                System.err.println("Impossibile accedere a: " + dir + " (" + e.getMessage() + ")");
            }

            // Ricongiungimento (Join) dei risultati: raccogliamo i report delle sotto-directory.
            for (Future<FSReport> task : subTasks) {
                // Questa chiamata get() blocca il Virtual Thread, causandone l'unmount
                // finché il task figlio non è completato, liberando il carrier thread
                localReport.merge(task.get());
            }

            return localReport;
        });
    }
}