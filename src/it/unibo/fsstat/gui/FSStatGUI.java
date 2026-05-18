package it.unibo.fsstat.gui;

import it.unibo.fsstat.lib.FSReport;
import it.unibo.fsstat.lib.FSStatLib;
import it.unibo.fsstat.lib.FSStatLib.TaskController;

import javax.swing.*;
import java.awt.*;
import java.io.File;

public class FSStatGUI extends JFrame {

    private final FSStatLib lib = new FSStatLib();
    private TaskController currentTaskController;

    private final JTextField pathField = new JTextField("/Users/Alessandro/Documents", 20); 
    private final JButton btnStart = new JButton("Start");
    private final JButton btnStop = new JButton("Stop");
    private final JTextArea consoleArea = new JTextArea(15, 45);

    public FSStatGUI() {
        super("FSStat - Analisi Interattiva (Virtual Threads)");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        initComponents();
        btnStop.setEnabled(false);
        pack();
        setLocationRelativeTo(null);
    }

    private void initComponents() {
        JPanel topPanel = new JPanel();
        topPanel.add(new JLabel("Directory:"));
        topPanel.add(pathField);
        topPanel.add(btnStart);
        topPanel.add(btnStop);

        consoleArea.setEditable(false);
        consoleArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        JScrollPane scrollPane = new JScrollPane(consoleArea);

        add(topPanel, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);

        // Collegamento dei pulsanti alle azioni
        btnStart.addActionListener(e -> startAnalysis());
        btnStop.addActionListener(e -> stopAnalysis());
    }

    private void startAnalysis() {
        String dir = pathField.getText();
        if (!new File(dir).exists()) {
            JOptionPane.showMessageDialog(this, "Directory inesistente o non valida!");
            return;
        }

        consoleArea.setText("Avvio analisi in corso...\n");
        btnStart.setEnabled(false);
        btnStop.setEnabled(true);

        currentTaskController = new TaskController();
        long maxFS = 1048576; // 1 MB
        int nb = 5;

        long startTime = System.currentTimeMillis();

        // lambda che aggiorna la GUI (usando invokeLater per non bloccare Swing)
        lib.getFSReport(dir, maxFS, nb, 
            report -> SwingUtilities.invokeLater(() -> updateDisplay(report, "IN ESECUZIONE...")),
            currentTaskController
        ).thenAccept(finalReport -> {
            long time = System.currentTimeMillis() - startTime;
            SwingUtilities.invokeLater(() -> {
                updateDisplay(finalReport, "COMPLETATO in " + time + " ms");
                btnStart.setEnabled(true);
                btnStop.setEnabled(false);
            });
        });
    }

    private void stopAnalysis() {
        if (currentTaskController != null) {
            currentTaskController.cancel();
            consoleArea.append("\n\n[!] Richiesta interruzione inoltrata ai Virtual Threads...\n");
            btnStop.setEnabled(false);
            btnStart.setEnabled(true);
        }
    }

    private void updateDisplay(FSReport report, String status) {
        StringBuilder sb = new StringBuilder();
        sb.append("Stato Elaborazione: ").append(status).append("\n");
        sb.append("Totale File Rilevati: ").append(report.getTotalFiles()).append("\n");
        sb.append("------------------------------------------\n");
        
        long step = report.getMaxFS() / report.getNb();
        int[] dist = report.getDistribution();
        
        for (int i = 0; i < report.getNb(); i++) {
            sb.append(String.format("Banda %d [%10d - %10d byte]: %d file\n", 
                    (i+1), i*step, (i+1)*step, dist[i]));
        }
        sb.append(String.format("Banda %d [ > %18d byte]: %d file\n", 
                (report.getNb() + 1), report.getMaxFS(), dist[report.getNb()]));

        // Sovrascrive il testo per un effetto di aggiornamento in tempo reale
        consoleArea.setText(sb.toString()); 
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new FSStatGUI().setVisible(true));
    }
}