package lib;
import java.util.Arrays;

public class Report {
    
    private long totalFiles;
    private final long[] sizeDistribution;

    /**
     * Costruttore del Report.
     * @param nb Il numero di bande (band range) definito dalla consegna.
     */
    public Report(int nb) {
        this.totalFiles = 0;
        this.sizeDistribution = new long[nb + 1];
    }

    /**
     * Metodo per registrare un file nel report, incrementando il totale e aggiornando la distribuzione.
     * 
     * @param bandIndex L'indice della fascia in cui ricade il file analizzato.
     */
    public void recordFile(int bandIndex) {
        this.totalFiles++;
        
        if (bandIndex >= 0 && bandIndex < sizeDistribution.length) {
            this.sizeDistribution[bandIndex]++;
        }
    }

    /**
     * Fonde un altro report dentro questo report, per aggregare i risultati parziali ottenuti da diversi thread.
     * 
     * @param other Il report parziale da sommare a questo.
     */
    public void merge(Report other) {
        this.totalFiles += other.totalFiles;
        
        for (int i = 0; i < this.sizeDistribution.length; i++) {
            this.sizeDistribution[i] += other.sizeDistribution[i];
        }
    }

    /**
     * Calcola l'indice dell'array (la fascia) in cui inserire un file in base alla sua dimensione.
     * 
     * @param fileSize La dimensione del file rilevato (in byte).
     * @param maxFS    La dimensione massima (MaxFS) considerata dalle fasce regolari.
     * @param nb       Il numero di fasce regolari (NB).
     * @return L'indice dell'array sizeDistribution in cui incrementare il conteggio.
     */
    public static int calculateBandIndex(long fileSize, long maxFS, int nb) {
        // Caso oltre limite: I file più grandi di MaxFS vanno nell'ultimo "secchio"
        if (fileSize > maxFS) {
            return nb; 
        }
        
        // Caso limite: Se il file è grande *esattamente* MaxFS, lo mettiamo nell'ultima fascia regolare (indice nb - 1)
        if (fileSize == maxFS) {
            return nb - 1;
        }

        // Caso normale: Calcolo della fascia
        double bandWidth = (double) maxFS / nb;
        return (int) (fileSize / bandWidth);
    }
    
    public long getTotalFiles() {
        return totalFiles;
    }

    public long[] getSizeDistribution() {
        return sizeDistribution;
    }
    
    @Override
    public String toString() {
        return "FSReport {" +
                "\n  Totale File: " + totalFiles +
                "\n  Distribuzione (per fascia): " + Arrays.toString(sizeDistribution) +
                "\n}";
    }
}
