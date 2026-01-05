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
        SwingUtilities.invokeLater(MetadataListUI::initApp);
    }

    private static void initApp() {
        FlatDarculaLaf.setup();

        try {
            UIManager.setLookAndFeel(new FlatMTMaterialDeepOceanIJTheme());
        } catch (UnsupportedLookAndFeelException e) {
            logger.error("Error setting LAF", e);
        }

        instance = new MetadataListUI();
        instance.setVisible(true);
    }

    public MetadataListUI() {
        this.setTitle("File-Sorter");
        this.setSize(1200, 600);

        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.setLayout(new BorderLayout(10, 10));

        this.pageablePanel = new PageablePanel();
        this.add(this.pageablePanel, BorderLayout.CENTER);
        this.pageablePanel.open(new HomePage());

        this.setLocationRelativeTo(null);
    }

    public void open(JPanel panel) {
        this.pageablePanel.open(panel);
    }
}