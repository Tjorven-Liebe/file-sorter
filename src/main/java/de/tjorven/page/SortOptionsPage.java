package de.tjorven.page;

import de.tjorven.algorithm.RecursiveSorter;
import lombok.Getter;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import java.awt.*;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

@Getter
public class SortOptionsPage extends JPanel {

    private final ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newCachedThreadPool();

    // UI Komponenten für den Baum
    private final DefaultMutableTreeNode rootNode = new DefaultMutableTreeNode("Preview Root");
    private final DefaultTreeModel treeModel = new DefaultTreeModel(rootNode);
    private final JTree previewTree = new JTree(treeModel);

    private final DefaultListModel<String> selectedLevelsModel = new DefaultListModel<>();
    private final JList<String> levelsList = new JList<>(this.selectedLevelsModel);

    private final JButton revertButton = new JButton("Revert Last Sort");
    private final JProgressBar progressBar = new JProgressBar();

    private final String selectedFolderPath;
    private final Map<String, Map<String, String>> fileMetadataMap;
    private final RecursiveSorter recursiveSorter;
    private final List<String> metadataOptions;

    public SortOptionsPage(String selectedFolderPath, Map<String, Map<String, String>> fileMetadataMap, List<String> metadataOptions) {
        this.selectedFolderPath = selectedFolderPath;
        this.fileMetadataMap = fileMetadataMap;
        this.recursiveSorter = new RecursiveSorter(this.selectedFolderPath, this.fileMetadataMap);
        this.metadataOptions = metadataOptions;

        this.init();
    }

    private void init() {
        this.setLayout(new BorderLayout(15, 15));
        this.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel controls = new JPanel(new GridLayout(1, 2, 10, 0));
        controls.add(this.createHierarchyBuilder(this.metadataOptions));

        // Preview Panel mit JTree
        JPanel previewPanel = new JPanel(new BorderLayout());
        previewPanel.add(new JLabel("Move Preview (Tree View):"), BorderLayout.NORTH);
        this.previewTree.setFont(new Font("SansSerif", Font.PLAIN, 12));
        previewPanel.add(new JScrollPane(this.previewTree), BorderLayout.CENTER);
        controls.add(previewPanel);

        // Action Panel
        JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        this.revertButton.setEnabled(false);
        this.revertButton.addActionListener(e -> handleRevert());

        JButton runButton = new JButton("Run Recursive Sort");
        runButton.setFont(new Font("SansSerif", Font.BOLD, 14));
        runButton.addActionListener(e -> this.startSorting());

        actionPanel.add(this.revertButton);
        actionPanel.add(runButton);

        this.add(controls, BorderLayout.CENTER);
        this.add(actionPanel, BorderLayout.SOUTH);
    }

    private void updatePreview() {
        // Root zurücksetzen
        rootNode.removeAllChildren();
        rootNode.setUserObject(selectedFolderPath != null ? selectedFolderPath : "Preview");

        if (this.fileMetadataMap == null || this.selectedLevelsModel.isEmpty()) {
            treeModel.reload();
            return;
        }

        // Für jede Datei den Pfad im Baum generieren
        this.fileMetadataMap.forEach((fileName, metadata) -> {
            DefaultMutableTreeNode currentNode = rootNode;

            // Ordner-Ebenen durchlaufen
            for (int i = 0; i < this.selectedLevelsModel.size(); i++) {
                String attr = this.selectedLevelsModel.get(i);
                String folderName = metadata.getOrDefault(attr, "Unknown").replaceAll("[\\\\/:*?\"<>|]", "_");

                currentNode = getOrCreateChild(currentNode, folderName);
            }

            // Die Datei als Blatt (Leaf) hinzufügen
            currentNode.add(new DefaultMutableTreeNode(fileName));
        });

        // Baum aktualisieren und alle Pfade aufklappen
        treeModel.reload();
        for (int i = 0; i < previewTree.getRowCount(); i++) {
            previewTree.expandRow(i);
        }
    }

    /**
     * Hilfsmethode: Findet einen Kind-Knoten oder erstellt ihn, falls nicht vorhanden.
     */
    private DefaultMutableTreeNode getOrCreateChild(DefaultMutableTreeNode parent, String name) {
        for (int i = 0; i < parent.getChildCount(); i++) {
            DefaultMutableTreeNode child = (DefaultMutableTreeNode) parent.getChildAt(i);
            if (child.getUserObject().equals(name)) {
                return child;
            }
        }
        DefaultMutableTreeNode newNode = new DefaultMutableTreeNode(name);
        parent.add(newNode);
        return newNode;
    }

    // --- Restliche Hilfsmethoden (Hierarchy Builder & Logic) ---

    private JPanel createHierarchyBuilder(List<String> options) {
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.add(new JLabel("Sorting Hierarchy (Drag or Add Levels)"), BorderLayout.NORTH);

        JComboBox<String> combo = new JComboBox<>(options.toArray(new String[0]));
        JButton addButton = new JButton("Add");
        JButton removeButton = new JButton("Remove");
        JButton clearButton = new JButton("Clear");

        addButton.addActionListener(e -> {
            String selected = (String) combo.getSelectedItem();
            if (selected != null && !this.selectedLevelsModel.contains(selected)) {
                this.selectedLevelsModel.addElement(selected);
                this.updatePreview();
            }
        });

        removeButton.addActionListener(e -> {
            int idx = levelsList.getSelectedIndex();
            if (idx != -1) {
                this.selectedLevelsModel.remove(idx);
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
        btnPanel.add(removeButton);
        btnPanel.add(clearButton);

        panel.add(new JScrollPane(this.levelsList), BorderLayout.CENTER);
        panel.add(btnPanel, BorderLayout.SOUTH);
        return panel;
    }

    private void startSorting() {
        List<String> attributes = Collections.list(this.selectedLevelsModel.elements());
        if (this.selectedFolderPath == null || attributes.isEmpty()) return;

        this.executor.execute(() -> {
            try {
                this.recursiveSorter.runFilter(Paths.get(this.selectedFolderPath), attributes);
                SwingUtilities.invokeLater(() -> {
                    this.revertButton.setEnabled(true);
                    JOptionPane.showMessageDialog(this, "Sorting complete!");
                    this.updatePreview();
                });
            } catch (IOException ex) {
                SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage()));
            }
        });
    }

    private void handleRevert() {
        try {
            this.recursiveSorter.revertSort();
            this.revertButton.setEnabled(false);
            this.updatePreview();
            JOptionPane.showMessageDialog(this, "Revert complete!");
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "Error during revert: " + ex.getMessage());
        }
    }
}