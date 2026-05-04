import io.reactivex.rxjava3.core.Single;
import lib.FSStatLib;
import lib.Report;

import java.util.concurrent.CountDownLatch;

public class Main {

    public static void main(String[] args) {
        
        String directoryToScan = "C:\\Users\\Alessandro\\Documents\\Università"; 
        long maxFS = 1048576;
        int nb = 5;

        System.out.println("Avvio analisi asincrona della directory: " + directoryToScan);
        System.out.println("Attendere prego...\n");

        // "semaforo" che bloccherà il main
        CountDownLatch latch = new CountDownLatch(1);

        Single<Report> reportSingle = FSStatLib.getFSReport(directoryToScan, maxFS, nb);

        reportSingle.subscribe(
            // Callback di SUCCESSO (onSuccess)
            report -> {
                System.out.println("--- SCANSIONE COMPLETATA ---");
                System.out.println(report.toString()); // Usa il toString() che abbiamo definito
                
                // Sblocca il main thread
                latch.countDown(); 
            },
            
            // Callback di ERRORE (onError)
            error -> {
                System.err.println("--- ERRORE DURANTE LA SCANSIONE ---");
                System.err.println("Motivo: " + error.getMessage());
                
                latch.countDown(); 
            }
        );

        try {
            // Attende che il latch venga sbloccato (cioè che la scansione sia completata o che si verifichi un errore)
            latch.await();
        } catch (InterruptedException e) {
            System.err.println("Il thread principale è stato interrotto.");
        }

        System.out.println("\nProgramma terminato regolarmente.");
    }
}