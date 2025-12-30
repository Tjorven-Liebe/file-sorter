package de.tjorven.page;

import lombok.Getter;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

@Getter
public class SortOptionsPage extends JPanel {

    private final String selectedFolderPath;
    private final Map<String, Map<String, String>> fileMetadataMap;

    private final DefaultListModel<String> selectedLevelsModel = new DefaultListModel<>();
    private final JList<String> levelsList = new JList<>(this.selectedLevelsModel);

    private final DefaultListModel<String> previewModel = new DefaultListModel<>();
    private final JList<String> previewList = new JList<>(this.previewModel);

    private record MoveHistory(Path source, Path target) {
    }

    private final List<MoveHistory> lastOperationHistory = new ArrayList<>();
    private final JButton revertButton = new JButton("Revert Last Sort");

    public SortOptionsPage(String selectedFolderPath, Map<String, Map<String, String>> fileMetadataMap, List<String> metadataOptions) {
        this.selectedFolderPath = selectedFolderPath;
        this.fileMetadataMap = fileMetadataMap;

        this.setLayout(new BorderLayout(15, 15));
        this.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel controls = new JPanel(new GridLayout(1, 2, 10, 0));
        controls.add(this.createHierarchyBuilder(metadataOptions));

        JPanel previewPanel = new JPanel(new BorderLayout());
        previewPanel.add(new JLabel("Move Preview:"), BorderLayout.NORTH);
        this.previewList.setFont(new Font("Monospaced", Font.PLAIN, 12));
        previewPanel.add(new JScrollPane(this.previewList), BorderLayout.CENTER);
        controls.add(previewPanel);

        JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        this.revertButton.setEnabled(false);
        this.revertButton.addActionListener(e -> this.revertSort());

        JButton runButton = new JButton("Run Recursive Sort");
        runButton.setFont(new Font("SansSerif", Font.BOLD, 14));
        runButton.addActionListener(e -> this.startSorting());

        actionPanel.add(this.revertButton);
        actionPanel.add(runButton);

        this.add(controls, BorderLayout.CENTER);
        this.add(actionPanel, BorderLayout.SOUTH);
    }

    private JPanel createHierarchyBuilder(List<String> options) {
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.add(new JLabel("Sorting Hierarchy (Artist > Album > ...)"), BorderLayout.NORTH);

        JComboBox<String> combo = new JComboBox<>(options.toArray(new String[0]));
        JButton addButton = new JButton("Add Level");
        JButton clearButton = new JButton("Clear");

        addButton.addActionListener(e -> {
            String selected = (String) combo.getSelectedItem();
            if (selected != null && !this.selectedLevelsModel.contains(selected)) {
                this.selectedLevelsModel.addElement(selected);
                this.updatePreview();
            }
        });

        clearButton.addActionListener(e -> {
            this.selectedLevelsModel.clear();
            this.updatePreview();
        });

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        btnPanel.add(combo);
        btnPanel.add(addButton);
        btnPanel.add(clearButton);

        panel.add(new JScrollPane(this.levelsList), BorderLayout.CENTER);
        panel.add(btnPanel, BorderLayout.SOUTH);
        this.updateUI();
        return panel;
    }

    private void updatePreview() {
        this.previewModel.clear();
        if (this.fileMetadataMap == null || this.selectedLevelsModel.isEmpty()) return;

        this.fileMetadataMap.forEach((fileName, metadata) -> {
            StringBuilder pathPreview = new StringBuilder();
            for (int i = 0; i < this.selectedLevelsModel.size(); i++) {
                String attr = this.selectedLevelsModel.get(i);
                String val = metadata.getOrDefault(attr, "Unknown");
                pathPreview.append(val.replaceAll("[\\\\/:*?\"<>|]", "_")).append("/");
            }
            this.previewModel.addElement(fileName + " -> " + pathPreview + fileName);
        });
    }

    private void startSorting() {
        if (this.selectedFolderPath == null || this.selectedLevelsModel.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please select a folder and at least one sorting level.");
            return;
        }

        try {
            List<String> attributes = Collections.list(this.selectedLevelsModel.elements());
            this.runFilter(Paths.get(this.selectedFolderPath), attributes);
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage());
        }
    }

    public void runFilter(Path rootPath, List<String> selectedAttributes) throws IOException {
        this.lastOperationHistory.clear();
        List<Path> filesInFolder;
        try (Stream<Path> stream = Files.list(rootPath)) {
            filesInFolder = stream.filter(Files::isRegularFile).toList();
        }

        for (Path filePath : filesInFolder) {
            Map<String, String> fileMeta = this.fileMetadataMap.get(filePath.getFileName().toString());
            if (fileMeta == null) continue;

            Path currentTargetDir = rootPath;
            for (String attr : selectedAttributes) {
                String rawValue = fileMeta.get(attr);
                String folderName = (rawValue == null || rawValue.isBlank()) ? "Unknown_" + attr : rawValue.replaceAll("[\\\\/:*?\"<>|]", "_").trim();
                currentTargetDir = currentTargetDir.resolve(folderName);
            }

            if (!Files.exists(currentTargetDir)) Files.createDirectories(currentTargetDir);

            Path targetFile = currentTargetDir.resolve(filePath.getFileName());
            Files.move(filePath, targetFile, StandardCopyOption.REPLACE_EXISTING);
            this.lastOperationHistory.add(new MoveHistory(filePath, targetFile));
        }

        this.revertButton.setEnabled(!this.lastOperationHistory.isEmpty());
        this.updatePreview();
        JOptionPane.showMessageDialog(this, "Successfully sorted files!");
    }

    private void revertSort() {
        if (this.lastOperationHistory.isEmpty()) return;
        try {
            for (MoveHistory move : this.lastOperationHistory) {
                if (Files.exists(move.target())) {
                    Files.move(move.target(), move.source(), StandardCopyOption.REPLACE_EXISTING);
                }
            }
            this.cleanUpEmptyFolders();
            this.lastOperationHistory.clear();
            this.revertButton.setEnabled(false);
            this.updatePreview();
            JOptionPane.showMessageDialog(this, "Revert complete!");
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "Error during revert: " + ex.getMessage());
        }
    }

    private void cleanUpEmptyFolders() {
        this.lastOperationHistory.stream()
                .map(move -> move.target().getParent())
                .distinct()
                .forEach(dir -> {
                    try {
                        while (dir != null && !dir.equals(Paths.get(this.selectedFolderPath))) {
                            try (Stream<Path> s = Files.list(dir)) {
                                if (s.findAny().isPresent()) {
                                    break;
                                }

                                Files.delete(dir);
                                dir = dir.getParent();
                            }
                        }
                    } catch (IOException ignored) {
                    }
                });
    }
}