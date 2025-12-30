package de.tjorven.page;

import de.tjorven.MetadataListUI;
import de.tjorven.folder.FolderAnalyser;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.io.File;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HomePage extends JPanel {

    private String selectedFolderPath;
    private final Map<String, Map<String, String>> fileMetadataMap = new HashMap<>();
    private final List<String> metadataDropdown = new ArrayList<>();

    private final JLabel pathLabel;
    private final JButton selectBtn;
    private final JButton scanBtn;
    private final JButton nextBtn;
    private final JProgressBar progressBar;

    public HomePage() {
        this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        this.setBorder(new EmptyBorder(40, 40, 40, 40));

        JLabel title = new JLabel("Metadata Organizer");
        title.setFont(new Font("SansSerif", Font.BOLD, 24));
        title.setAlignmentX(Component.CENTER_ALIGNMENT);

        this.selectBtn = new JButton("1. Select Folder");
        this.selectBtn.setAlignmentX(Component.CENTER_ALIGNMENT);

        this.pathLabel = new JLabel("No directory selected");
        this.pathLabel.setForeground(Color.GRAY);
        this.pathLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        this.pathLabel.setBorder(new EmptyBorder(10, 0, 20, 0));

        this.scanBtn = new JButton("2. Analyze Files");
        this.scanBtn.setEnabled(false);
        this.scanBtn.setAlignmentX(Component.CENTER_ALIGNMENT);

        this.progressBar = new JProgressBar();
        this.progressBar.setIndeterminate(true);
        this.progressBar.setVisible(false);
        this.progressBar.setMaximumSize(new Dimension(300, 20));

        this.nextBtn = new JButton("3. Configure Sorting →");
        this.nextBtn.setEnabled(false);
        this.nextBtn.setAlignmentX(Component.CENTER_ALIGNMENT);

        this.selectBtn.addActionListener(e -> this.selectFolder());
        this.scanBtn.addActionListener(e -> this.startAnalysis());
        this.nextBtn.addActionListener(e -> this.goToSortPage());

        this.add(title);
        this.add(Box.createRigidArea(new Dimension(0, 30)));
        this.add(this.selectBtn);
        this.add(this.pathLabel);
        this.add(Box.createRigidArea(new Dimension(0, 10)));
        this.add(this.scanBtn);
        this.add(Box.createRigidArea(new Dimension(0, 10)));
        this.add(this.progressBar);
        this.add(Box.createVerticalGlue()); // Push next button to bottom
        this.add(this.nextBtn);
    }

    private void selectFolder() {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File selectedFile = chooser.getSelectedFile();
            this.selectedFolderPath = selectedFile.getAbsolutePath();
            this.pathLabel.setText(selectedFile.getAbsolutePath());
            this.pathLabel.setForeground(new Color(30, 130, 70));
            this.scanBtn.setEnabled(true);
            this.nextBtn.setEnabled(false);
        }
    }

    private void startAnalysis() {
        this.scanBtn.setEnabled(false);
        this.progressBar.setVisible(true);

        new Thread(() -> {
            try {
                this.loadFiles(this.selectedFolderPath);

                SwingUtilities.invokeLater(() -> {
                    this.progressBar.setVisible(false);
                    this.nextBtn.setEnabled(true);
                    this.scanBtn.setText("Re-scan Files");
                    this.scanBtn.setEnabled(true);
                });
            } catch (Exception e) {
                SwingUtilities.invokeLater(() -> {
                    this.progressBar.setVisible(false);
                    this.scanBtn.setEnabled(true);
                    JOptionPane.showMessageDialog(this, "Error: " + e.getMessage());
                });
            }
        }).start();
    }

    private void loadFiles(String path) throws Exception {
        this.metadataDropdown.clear();
        this.fileMetadataMap.clear();

        FolderAnalyser analyser = new FolderAnalyser(Paths.get(path));

        analyser.findSimilarities().forEach(entry -> this.metadataDropdown.add(entry.getKey()));

        this.fileMetadataMap.putAll(analyser.getMetadata());
    }

    private void goToSortPage() {
        SortOptionsPage sortOptionsPage = new SortOptionsPage(
                this.selectedFolderPath,
                this.fileMetadataMap,
                this.metadataDropdown
        );
        MetadataListUI.getInstance().open(sortOptionsPage);
    }
}