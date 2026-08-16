package it.unibo.fsstat.lib;

import java.util.Arrays;
import java.util.concurrent.locks.ReentrantLock;

public class FSReport {
    private int totalFiles = 0;
    private final int[] distribution;
    private final long maxFS;
    private final int nb;

    // ReentrantLock per evitare il "Thread Pinning" dei Virtual Threads
    private final ReentrantLock lock = new ReentrantLock();

    public FSReport(long maxFS, int nb) {
        this.maxFS = maxFS;
        this.nb = nb;
        this.distribution = new int[nb + 1];
    }

    private FSReport(FSReport other) {
        this.maxFS = other.maxFS;
        this.nb = other.nb;
        this.totalFiles = other.totalFiles;
        this.distribution = Arrays.copyOf(other.distribution, other.distribution.length);
    }

    /**
     * Registra un file nel report. Restituisce il nuovo totale di file, calcolato
     * atomicamente insieme all'incremento, cosi' il chiamante puo' decidere se
     * notificare un aggiornamento senza dover rileggere lo stato condiviso
     * separatamente (evitando la race tra incremento e lettura).
     */
    public int addFile(long size) {
        lock.lock();
        try {
            this.totalFiles++;
            if (size > maxFS) {
                this.distribution[nb]++;
            } else {
                double step = (double) maxFS / nb;
                int bandIndex = (int) (size / step);
                if (bandIndex == nb) bandIndex = nb - 1;
                this.distribution[bandIndex]++;
            }
            return this.totalFiles;
        } finally {
            lock.unlock();
        }
    }

    /** Istantanea coerente e indipendente dello stato corrente del report. */
    public FSReport getSnapshot() {
        lock.lock();
        try {
            return new FSReport(this);
        } finally {
            lock.unlock();
        }
    }

    public int getTotalFiles() {
        lock.lock();
        try {
            return totalFiles;
        } finally {
            lock.unlock();
        }
    }

    /** Restituisce una copia difensiva: il chiamante non puo' mutare lo stato interno. */
    public int[] getDistribution() {
        lock.lock();
        try {
            return Arrays.copyOf(distribution, distribution.length);
        } finally {
            lock.unlock();
        }
    }

    public long getMaxFS() { return maxFS; }
    public int getNb() { return nb; }
}
