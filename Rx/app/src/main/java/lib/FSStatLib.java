package lib;
import io.reactivex.rxjava3.core.BackpressureStrategy;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.FlowableEmitter;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.schedulers.Schedulers;
import java.io.File;

public class FSStatLib {

    public static Single<Report> getFSReport(String directoryPath, long maxFS, int nb) {
        File rootDir = new File(directoryPath);
        
        if (!rootDir.exists() || !rootDir.isDirectory()) {
            return Single.error(new IllegalArgumentException("Il percorso non esiste o non è una directory."));
        }

        return exploreDirectory(rootDir)
            .parallel()     // Trasforma il flusso in un flusso parallelo RxJava crea automaticamente un numero di "binari" pari ai core della CPU.
            .runOn(Schedulers.io())     // Esegue questi binari su thread paralleli reali.
            
            // Ogni thread crea il mini-report per il suo specifico file.
            .map(file -> {
                Report partialReport = new Report(nb);
                int bandIndex = Report.calculateBandIndex(file.length(), maxFS, nb);
                partialReport.recordFile(bandIndex);
                return partialReport;
            })
            
            // Ogni thread aggrega i suoi file in un UNICO "Report di binario".
            .collect(
                () -> new Report(nb),
                (railReport, partialReport) -> railReport.merge(partialReport)
            )
            
            // Riporta i binari paralleli in un unico flusso sequenziale (Flowable standard).
            .sequential()
            
            // Aggrega i report dei vari thread in un singolo e definitivo Report finale.
            .collect(
                () -> new Report(nb),
                (mainReport, railReport) -> mainReport.merge(railReport)
            );
    }

    
    /**
     * Metodo privato che crea un flusso reattivo di file a partire da una cartella.
     */
    private static Flowable<File> exploreDirectory(File dir) {
        return Flowable.create(emitter -> {
            traverse(dir, emitter);
            
            emitter.onComplete();
        }, BackpressureStrategy.BUFFER); // Il BUFFER salva i file in coda se il consumatore è lento
    }

    
    private static void traverse(File currentDir, FlowableEmitter<File> emitter) {
        if (emitter.isCancelled()) return;

        File[] files = currentDir.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    // Se è una cartella, entra dentro ricorsivamente
                    traverse(file, emitter);
                } else {
                    // Se è un file, lo "sputa" nel flusso reattivo!
                    emitter.onNext(file);
                }
            }
        }
    }

}
