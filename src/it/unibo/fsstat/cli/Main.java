package it.unibo.fsstat.cli;

import it.unibo.fsstat.lib.FSStatLib;

public class Main {
    public static void main(String[] args) {
        FSStatLib lib = new FSStatLib();

        String testDir = "/Users/Alessandro/Documents"; // Directory corrente (puoi cambiarla per testare path più grandi)
        long maxFileSize = 1048576; // 1 MB (espresso in byte)
        int numberOfBands = 5;      // Dividiamo i file tra 0 e 1MB in 5 fasce

        System.out.println("Avvio computazione asincrona con Virtual Threads...");
        long startTime = System.currentTimeMillis();

        lib.getFSReport(testDir, maxFileSize, numberOfBands)
                .thenAccept(report -> {
                    long time = System.currentTimeMillis() - startTime;
                    System.out.println("--- REPORT GENERATO in " + time + " ms ---");
                    System.out.println("Totale file scansionati: " + report.getTotalFiles());

                    int[] dist = report.getDistribution();
                    long bandSize = maxFileSize / numberOfBands;

                    for (int i = 0; i < numberOfBands; i++) {
                        long startBand = i * bandSize;
                        long endBand = (i + 1) * bandSize;
                        System.out.println("Banda " + (i+1) + " [" + startBand + " - " + endBand + " byte]: " + dist[i] + " file");
                    }
                    System.out.println("Banda " + (numberOfBands + 1) + " [> " + maxFileSize + " byte]: " + dist[numberOfBands] + " file");
                })
                .exceptionally(ex -> {
                    System.err.println("Si è verificato un errore durante l'analisi: " + ex.getMessage());
                    return null;
                })
                .join(); // join() fa attendere al thread Main la fine della CompletableFuture per non far terminare il programma

        System.out.println("Analisi conclusa.");
    }
}