package de.tjorven.page;

import de.tjorven.MetadataListUI;
import de.tjorven.folder.FolderAnalyser;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class HomePage extends JPanel {

    private String selectedFolderPath;
    private final Map<String, Map<String, String>> fileMetadataMap = new HashMap<>();
    private final List<String> metadataDropdown = new ArrayList<>();
    private final JLabel pathLabel;
    private final JPanel selection;
    private JButton scan = null;
    private JButton select = null;

    public HomePage() {
        this.setLayout(new BorderLayout());
        JPanel topPanel = new JPanel(new GridLayout(2, 1));
        JButton selectBtn = new JButton("Select Folder");
        this.pathLabel = new JLabel("No folder selected", SwingConstants.CENTER);

        selectBtn.addActionListener(e -> this.selectFolder());

        topPanel.add(selectBtn);
        topPanel.add(this.pathLabel);

        this.selection = new JPanel(new GridLayout(2, 1));
        this.add(topPanel, BorderLayout.CENTER);
        this.add(this.selection, BorderLayout.SOUTH);

        this.revalidate();
        this.updateUI();
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

            this.setScanButton();
        }
    }

    private void loadFiles(String path) {
        this.metadataDropdown.clear();
        this.fileMetadataMap.clear();

        try {
            FolderAnalyser analyser = new FolderAnalyser(Paths.get(path));
            List<Map.Entry<String, Integer>> similarities = analyser.findSimilarities();

            for (Map.Entry<String, Integer> entry : similarities) {
                this.metadataDropdown.add(entry.getKey());
            }

            for (String fileName : analyser.getFileNames()) {
                this.fileMetadataMap.put(fileName, analyser.getMetadata().get(fileName));
            }

            JOptionPane.showMessageDialog(this, "Analyzed " + this.fileMetadataMap.size() + " files.");
        } catch (Exception e) {
            Logger.getLogger("info").log(Level.SEVERE, "Error reading metadata", e);
            JOptionPane.showMessageDialog(this, "Error reading metadata: " + e.getMessage());
        }
    }

    public void setScanButton() {
        if (this.scan != null) {
            return;
        }

        this.scan = new JButton("Scan");
        this.scan.addActionListener(actionEvent -> {
            this.loadFiles(this.selectedFolderPath);
            this.setSelectButton();
        });

        this.selection.add(this.scan, BorderLayout.SOUTH);
        this.selection.updateUI();
    }

    private void setSelectButton() {
        if (this.select != null) {
            return;
        }

        this.select = new JButton("Select");

        this.select.addActionListener(actionEvent -> {
            SortOptionsPage sortOptionsPage = new SortOptionsPage(this.selectedFolderPath, this.fileMetadataMap, this.metadataDropdown);

            MetadataListUI.getInstance().open(sortOptionsPage);
        });

        this.selection.add(this.select, BorderLayout.SOUTH);

        this.selection.updateUI();
    }

}
