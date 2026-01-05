package de.tjorven.panels;

import lombok.Getter;

import javax.swing.*;
import java.awt.*;

@Getter
public class PageablePanel extends JPanel {

    private final JPanel buttons;
    private JPanel view;
    private JPanel previousView;
    private JButton backButton;

    public PageablePanel() {
        this.setLayout(new BorderLayout());

        this.buttons = new JPanel(new FlowLayout(FlowLayout.LEFT));
        this.view = null;

        this.add(this.buttons, BorderLayout.SOUTH);
    }

    public void open(JPanel newView) {
        if (this.view != null) {
            this.remove(this.view);
            this.previousView = this.view;
        }

        this.view = newView;
        this.add(this.view, BorderLayout.CENTER);

        this.updateNavigation();

        this.revalidate();
        this.repaint();
    }

    private void updateNavigation() {
        this.buttons.removeAll();

        if (this.previousView != null) {
            this.backButton = new JButton("Back");
            this.backButton.addActionListener(e -> {
                this.openPrevious();
            });
            this.buttons.add(this.backButton);
        }

        this.buttons.revalidate();
        this.buttons.repaint();
    }

    private void openPrevious() {
        if (this.previousView == null) {
            return;
        }

        JPanel target = this.previousView;
        this.previousView = null;
        this.open(target);
    }
}