package de.tjorven;

import com.formdev.flatlaf.FlatDarculaLaf;
import de.tjorven.folder.FolderAnalyser;
import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;

public class MetadataListUI extends JFrame {

    private final Map<String, Map<String, String>> fileMetadataMap = new HashMap<>();
    private final JComboBox<String> metadataDropdown;
    private String selectedFolderPath;
    private final JLabel pathLabel;

    public MetadataListUI() {
        this.setTitle("Media Metadata Explorer");
        this.setSize(500, 300);
        this.setResizable(false);
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.setAlwaysOnTop(true);
        this.setLayout(new BorderLayout(10, 10));

        JPanel topPanel = new JPanel(new GridLayout(2, 1));
        JButton selectBtn = new JButton("Select Music Folder");
        this.pathLabel = new JLabel("No folder selected", SwingConstants.CENTER);

        selectBtn.addActionListener(e -> this.selectFolder());

        topPanel.add(selectBtn);
        topPanel.add(this.pathLabel);

        this.metadataDropdown = new JComboBox<>();
        JPanel midPanel = new JPanel(new BorderLayout());
        midPanel.setBorder(BorderFactory.createTitledBorder("Step 2: Filter by Attribute"));
        midPanel.add(this.metadataDropdown, BorderLayout.CENTER);

        JButton runButton = this.getRunButton();

        this.add(topPanel, BorderLayout.NORTH);
        this.add(midPanel, BorderLayout.CENTER);
        this.add(runButton, BorderLayout.SOUTH);

        this.setLocationRelativeTo(null);
    }

    private JButton getRunButton() {
        JButton runButton = new JButton("Run Sorting");
        runButton.setFont(new Font("SansSerif", Font.BOLD, 14));
        runButton.addActionListener(e -> {
            if (this.selectedFolderPath == null || this.metadataDropdown.getSelectedItem() == null) {
                JOptionPane.showMessageDialog(this, "Please select a folder and an attribute first.");
                return;
            }

            try {
                this.runFilter(Paths.get(this.selectedFolderPath), (String) this.metadataDropdown.getSelectedItem());
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage());
            }
        });

        return runButton;
    }

    private void selectFolder() {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        chooser.setDialogTitle("Select the directory containing your media files");

        int result = chooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedFile = chooser.getSelectedFile();
            this.selectedFolderPath = selectedFile.getAbsolutePath();
            this.pathLabel.setText("Folder: " + selectedFile.getName());

            this.loadFiles(this.selectedFolderPath);
        }
    }

    private void loadFiles(String path) {
        this.metadataDropdown.removeAllItems();
        this.fileMetadataMap.clear();

        try {
            FolderAnalyser analyser = new FolderAnalyser(Paths.get(path));
            List<Map.Entry<String, Integer>> similarities = analyser.findSimilarities();

            for (Map.Entry<String, Integer> entry : similarities) {
                this.metadataDropdown.addItem(entry.getKey());
            }

            for (String fileName : analyser.getFileNames()) {
                this.fileMetadataMap.put(fileName, analyser.getMetadata(fileName));
            }

            JOptionPane.showMessageDialog(this, "Analyzed " + this.fileMetadataMap.size() + " files.");

        } catch (Exception e) {
            Logger.getLogger("info").log(Level.SEVERE, "Error reading metadata", e);
            JOptionPane.showMessageDialog(this, "Error reading metadata: " + e.getMessage());
        }
    }

    public void runFilter(Path rootPath, String selectedAttribute) throws IOException {
        try (Stream<Path> files = Files.list(rootPath)) {
            List<Path> filesInFolder = files.filter(Files::isRegularFile).toList();

            for (Path filePath : filesInFolder) {
                String fileName = filePath.getFileName().toString();
                Map<String, String> metadata = this.fileMetadataMap.get(fileName);

                if (metadata == null || !metadata.containsKey(selectedAttribute)) {
                    continue;
                }

                String attributeValue = metadata.get(selectedAttribute);
                String safeFolderName = attributeValue.replaceAll("[\\\\/:*?\"<>|]", "_");
                Path targetDir = rootPath.resolve(safeFolderName);

                if (!Files.exists(targetDir)) {
                    Files.createDirectories(targetDir);
                }

                Path targetFile = targetDir.resolve(fileName);
                Files.move(filePath, targetFile, StandardCopyOption.REPLACE_EXISTING);
            }
        }

        JOptionPane.showMessageDialog(this, "Sorting complete!");
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            FlatDarculaLaf.setup();

            try {
                UIManager.setLookAndFeel(new FlatDarculaLaf());
            } catch (UnsupportedLookAndFeelException e) {
                Logger.getLogger("info").log(Level.SEVERE, "Error setting LAF", e);
            }

            new MetadataListUI().setVisible(true);
        });
    }
}