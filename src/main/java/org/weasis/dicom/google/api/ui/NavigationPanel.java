package org.weasis.dicom.google.api.ui;

import javax.swing.*;

import static javax.swing.BoxLayout.LINE_AXIS;

public class NavigationPanel extends JPanel {
    private SearchPanel searchPanel;

    public NavigationPanel(SearchPanel searchPanel) {
        this.searchPanel = searchPanel;
        JButton previousButton = new JButton();
        JButton nextButton = new JButton();
        JLabel pageNumberLabel = searchPanel.getPageNumberLabel();
        previousButton.setText("previous");
        nextButton.setText("next");
        previousButton.addActionListener((action) -> {
            this.searchPanel.prevPage();
        });

        nextButton.addActionListener((action) -> {
            this.searchPanel.nextPage();
        });
        
        BoxLayout navigationLayout = new BoxLayout(this, LINE_AXIS);
        this.setLayout(navigationLayout);
        this.add(previousButton);
        this.add(pageNumberLabel);
        this.add(nextButton);
    }

}