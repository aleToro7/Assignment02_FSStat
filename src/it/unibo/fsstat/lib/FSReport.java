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

    public void addFile(long size) {
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
        } finally {
            lock.unlock();
        }
    }

    public FSReport getSnapshot() {
        lock.lock();
        try {
            return new FSReport(this);
        } finally {
            lock.unlock();
        }
    }

    public int getTotalFiles() { return totalFiles; }
    public int[] getDistribution() { return distribution; }
    public long getMaxFS() { return maxFS; }
    public int getNb() { return nb; }
}