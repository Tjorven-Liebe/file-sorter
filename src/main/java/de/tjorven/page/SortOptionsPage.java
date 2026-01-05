package de.tjorven.page;

import de.tjorven.algorithm.RecursiveSorter;
import lombok.Getter;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
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

    private final DefaultMutableTreeNode rootNode = new DefaultMutableTreeNode("Preview Root");
    private final DefaultTreeModel treeModel = new DefaultTreeModel(this.rootNode);
    private final JTree previewTree = new JTree(this.treeModel);

    private final DefaultListModel<String> selectedLevelsModel = new DefaultListModel<>();
    private final JList<String> levelsList = new JList<>(this.selectedLevelsModel);

    private final JTextField extensionFilterField = new JTextField(10);
    private final JButton revertButton = new JButton("Revert Last Sort");

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
        this.setLayout(new BorderLayout());

        JPanel leftPanel = new JPanel(new BorderLayout(10, 10));
        leftPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 5));

        JPanel topControls = new JPanel(new BorderLayout(5, 5));
        topControls.add(this.createFilterPanel(), BorderLayout.NORTH);
        topControls.add(this.createHierarchyBuilder(this.metadataOptions), BorderLayout.CENTER);
        leftPanel.add(topControls, BorderLayout.CENTER);

        JPanel previewPanel = new JPanel(new BorderLayout());
        previewPanel.setBorder(BorderFactory.createTitledBorder("Move Preview"));
        this.previewTree.setFont(new Font("SansSerif", Font.PLAIN, 12));
        previewPanel.add(new JScrollPane(this.previewTree), BorderLayout.CENTER);
        previewPanel.setMinimumSize(new Dimension(200, 100));

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftPanel, previewPanel);
        splitPane.setDividerLocation(700);
        splitPane.setContinuousLayout(true);
        splitPane.setOneTouchExpandable(true);

        JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        this.revertButton.setEnabled(false);
        this.revertButton.addActionListener(e -> this.handleRevert());

        JButton runButton = new JButton("Run Recursive Sort");
        runButton.setFont(new Font("SansSerif", Font.BOLD, 14));
        runButton.addActionListener(e -> this.startSorting());

        actionPanel.add(this.revertButton);
        actionPanel.add(runButton);

        this.add(splitPane, BorderLayout.CENTER);
        this.add(actionPanel, BorderLayout.SOUTH);

        this.setupDragAndDrop();
    }

    private void setupDragAndDrop() {
        this.levelsList.setDragEnabled(true);
        this.levelsList.setDropMode(DropMode.INSERT);
        this.levelsList.setTransferHandler(new TransferHandler() {
            private int index = -1;

            @Override
            public int getSourceActions(JComponent c) {
                return MOVE;
            }

            @Override
            protected Transferable createTransferable(JComponent c) {
                this.index = SortOptionsPage.this.levelsList.getSelectedIndex();
                return new StringSelection(SortOptionsPage.this.levelsList.getSelectedValue());
            }

            @Override
            public boolean canImport(TransferSupport support) {
                return support.isDataFlavorSupported(DataFlavor.stringFlavor);
            }

            @Override
            public boolean importData(TransferSupport support) {
                if (!this.canImport(support)) return false;
                JList.DropLocation dl = (JList.DropLocation) support.getDropLocation();
                int dropIndex = dl.getIndex();
                try {
                    String data = (String) support.getTransferable().getTransferData(DataFlavor.stringFlavor);
                    if (this.index != -1) {
                        SortOptionsPage.this.selectedLevelsModel.remove(this.index);
                        if (dropIndex > this.index) dropIndex--;
                    }
                    SortOptionsPage.this.selectedLevelsModel.add(dropIndex, data);
                    SortOptionsPage.this.updatePreview();
                    return true;
                } catch (Exception e) {
                    return false;
                }
            }
        });
    }

    private void updatePreview() {
        this.rootNode.removeAllChildren();
        this.rootNode.setUserObject(this.selectedFolderPath != null ? this.selectedFolderPath : "Preview");

        if (this.fileMetadataMap == null || this.selectedLevelsModel.isEmpty()) {
            this.treeModel.reload();
            return;
        }

        String filterText = this.extensionFilterField.getText().toLowerCase().trim();
        String[] allowedExtensions = filterText.isEmpty() ? new String[0] : filterText.split("[, ]+");

        this.fileMetadataMap.forEach((fileName, metadata) -> {
            if (allowedExtensions.length > 0) {
                boolean matches = false;
                for (String ext : allowedExtensions) {
                    if (fileName.toLowerCase().endsWith(ext.startsWith(".") ? ext : "." + ext)) {
                        matches = true;
                        break;
                    }
                }
                if (!matches) return;
            }

            DefaultMutableTreeNode currentNode = this.rootNode;
            for (int i = 0; i < this.selectedLevelsModel.size(); i++) {
                String attr = this.selectedLevelsModel.get(i);
                String folderName = metadata.getOrDefault(attr, "Unknown").replaceAll("[\\\\/:*?\"<>|]", "_");
                currentNode = this.getOrCreateChild(currentNode, folderName);
            }
            currentNode.add(new DefaultMutableTreeNode(fileName));
        });

        this.treeModel.reload();
        for (int i = 0; i < this.previewTree.getRowCount(); i++) {
            this.previewTree.expandRow(i);
        }
    }

    private DefaultMutableTreeNode getOrCreateChild(DefaultMutableTreeNode parent, String name) {
        for (int i = 0; i < parent.getChildCount(); i++) {
            DefaultMutableTreeNode child = (DefaultMutableTreeNode) parent.getChildAt(i);
            if (child.getUserObject().equals(name)) return child;
        }
        DefaultMutableTreeNode newNode = new DefaultMutableTreeNode(name);
        parent.add(newNode);
        return newNode;
    }

    private JPanel createHierarchyBuilder(List<String> options) {
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.setBorder(BorderFactory.createTitledBorder("Sorting Hierarchy"));

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
            int idx = this.levelsList.getSelectedIndex();
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

    private JPanel createFilterPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        panel.setBorder(BorderFactory.createTitledBorder("Filter"));
        panel.add(new JLabel("Types (e.g. mp3, flac):"));
        panel.add(this.extensionFilterField);

        this.extensionFilterField.addActionListener(e -> this.updatePreview());
        return panel;
    }

    private void startSorting() {
        List<String> attributes = Collections.list(this.selectedLevelsModel.elements());
        String filter = this.extensionFilterField.getText();

        this.executor.execute(() -> {
            try {
                this.recursiveSorter.runFilter(Paths.get(this.selectedFolderPath), attributes, List.of(filter));
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
            JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage());
        }
    }
}