package it.unibo.fsstat.lib;

import java.util.Arrays;

public class FSReport {
    private int totalFiles = 0;
    private final int[] distribution; // Indice da 0 a NB-1 per le bande, indice NB per i file > MaxFS
    private final long maxFS; // Dimensione massima (MaxFS) per classificare i file nelle bande
    private final int nb; // Numero di bande in cui dividere i file tra 0 e MaxFS

    public FSReport(long maxFS, int nb) {
        this.maxFS = maxFS;
        this.nb = nb;
        this.distribution = new int[nb + 1];
    }

    // Metodo per aggiungere un file al report, classificandolo per fascia di dimensione
    public void addFile(long size) {
        this.totalFiles++;
        if (size > maxFS) {
            this.distribution[nb]++; // File che superano MaxFS
        } else {
            // Calcolo a quale fascia (banda) appartiene il file
            double step = (double) maxFS / nb;
            int bandIndex = (int) (size / step);
            if (bandIndex == nb) {
                bandIndex = nb - 1; // Caso limite in cui size == maxFS
            }
            this.distribution[bandIndex]++;
        }
    }

    // Metodo per unire i risultati di un'altra directory (Map-Reduce)
    public void merge(FSReport other) {
        this.totalFiles += other.totalFiles;
        for (int i = 0; i < this.distribution.length; i++) {
            this.distribution[i] += other.distribution[i];
        }
    }

    public int getTotalFiles() { return totalFiles; }
    public int[] getDistribution() { return distribution; }
    public long getMaxFS() { return maxFS; }
    public int getNb() { return nb; }

    @Override
    public String toString() {
        return "FSReport{" +
                "totalFiles=" + totalFiles +
                ", distribution=" + Arrays.toString(distribution) +
                '}';
    }
}