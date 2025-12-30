package de.tjorven;

import com.formdev.flatlaf.FlatDarculaLaf;
import com.formdev.flatlaf.intellijthemes.materialthemeuilite.FlatMTMaterialDeepOceanIJTheme;
import de.tjorven.page.HomePage;
import de.tjorven.panels.PageablePanel;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;

@Getter
public class MetadataListUI extends JFrame {

    @Getter
    private static final Logger logger = LoggerFactory.getLogger(MetadataListUI.class);

    @Getter
    private static MetadataListUI instance;
    private final PageablePanel pageablePanel;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            FlatDarculaLaf.setup();

            try {
                UIManager.setLookAndFeel(new FlatMTMaterialDeepOceanIJTheme());
            } catch (UnsupportedLookAndFeelException e) {
                logger.error("Error setting LAF", e);
            }

            instance = new MetadataListUI();
            instance.setVisible(true);
        });
    }

    public MetadataListUI() {
        this.setTitle("Media Metadata Explorer");
        this.setSize(800, 600);
        this.setResizable(false);
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.setAlwaysOnTop(true);
        this.setLayout(new BorderLayout(10, 10));

        this.pageablePanel = new PageablePanel();
        this.add(this.pageablePanel, BorderLayout.CENTER);
        this.pageablePanel.open(new HomePage());
//
//        JPanel topPanel = new JPanel(new GridLayout(2, 1));
//        JButton selectBtn = new JButton("Select Music Folder");
//        this.pathLabel = new JLabel("No folder selected", SwingConstants.CENTER);
//
//        selectBtn.addActionListener(e -> this.selectFolder());
//
//        topPanel.add(selectBtn);
//        topPanel.add(this.pathLabel);
//
//        this.metadataDropdown = new JComboBox<>();
//        JPanel midPanel = new JPanel(new BorderLayout());
//        midPanel.setBorder(BorderFactory.createTitledBorder("Step 2: Filter by Attribute"));
//        midPanel.add(this.metadataDropdown, BorderLayout.CENTER);
//
//        JButton runButton = this.getRunButton();
//
//        this.add(topPanel, BorderLayout.NORTH);
//        this.add(midPanel, BorderLayout.CENTER);
//        this.add(runButton, BorderLayout.SOUTH);
//
        this.setLocationRelativeTo(null);
    }

    public void open(JPanel panel) {
        this.pageablePanel.open(panel);
    }
//
//
//    private void selectFolder() {
//        JFileChooser chooser = new JFileChooser();
//        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
//        chooser.setDialogTitle("Select the directory containing your media files");
//
//        int result = chooser.showOpenDialog(this);
//        if (result == JFileChooser.APPROVE_OPTION) {
//            File selectedFile = chooser.getSelectedFile();
//            this.selectedFolderPath = selectedFile.getAbsolutePath();
//            this.pathLabel.setText("Folder: " + selectedFile.getName());
//
//            this.loadFiles(this.selectedFolderPath);
//        }
//    }
//
//    private void loadFiles(String path) {
//        this.metadataDropdown.removeAllItems();
//        this.fileMetadataMap.clear();
//
//        try {
//            FolderAnalyser analyser = new FolderAnalyser(Paths.get(path));
//            List<Map.Entry<String, Integer>> similarities = analyser.findSimilarities();
//
//            for (Map.Entry<String, Integer> entry : similarities) {
//                this.metadataDropdown.addItem(entry.getKey());
//            }
//
//            for (String fileName : analyser.getFileNames()) {
//                this.fileMetadataMap.put(fileName, analyser.getMetadata(fileName));
//            }
//
//            JOptionPane.showMessageDialog(this, "Analyzed " + this.fileMetadataMap.size() + " files.");
//
//        } catch (Exception e) {
//            Logger.getLogger("info").log(Level.SEVERE, "Error reading metadata", e);
//            JOptionPane.showMessageDialog(this, "Error reading metadata: " + e.getMessage());
//        }
//    }


}