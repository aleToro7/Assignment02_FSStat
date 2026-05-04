import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import io.reactivex.rxjava3.observers.TestObserver;
import lib.FSStatLib;
import lib.Report;

import java.util.concurrent.TimeUnit;

public class FSStatLibTest {

    @Test
    public void testGetFSReportSuccesso() throws InterruptedException {
        String directory = "C:\\Users\\Alessandro\\Documents\\Università"; 
        long maxFS = 80000;
        int nb = 5;

        TestObserver<Report> testObserver = FSStatLib.getFSReport(directory, maxFS, nb).test();

        testObserver.awaitDone(5, TimeUnit.SECONDS);

        testObserver.assertComplete(); // Assicura che sia stato chiamato onComplete
        testObserver.assertNoErrors(); // Assicura che non ci siano state eccezioni
        testObserver.assertValueCount(1); // Essendo un Single, deve emettere esattamente 1 valore

        Report report = testObserver.values().get(0);
        
        assertNotNull(report);
        assertTrue(report.getTotalFiles() >= 0, "Il numero di file non può essere negativo");
        assertEquals(nb + 1, report.getSizeDistribution().length, "L'array delle fasce deve essere NB + 1");
    }

    @Test
    public void testGetFSReportCartellaInesistente() throws InterruptedException {
        String directoryInesistente = "./cartella_che_non_esiste_123";
        
        TestObserver<Report> testObserver = FSStatLib.getFSReport(directoryInesistente, 1000, 5).test();
        
        testObserver.awaitDone(2, TimeUnit.SECONDS);

        testObserver.assertNotComplete();
        
        testObserver.assertError(IllegalArgumentException.class);
        
        testObserver.assertError(error -> 
            error.getMessage().equals("Il percorso non esiste o non è una directory.")
        );
    }
}