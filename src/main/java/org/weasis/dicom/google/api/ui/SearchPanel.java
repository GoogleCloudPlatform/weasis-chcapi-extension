// Copyright 2019 Google LLC
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package org.weasis.dicom.google.api.ui;

import org.weasis.dicom.google.api.model.StudyQuery;
import org.weasis.dicom.google.api.ui.dicomstore.DicomStoreSelector;
import org.jdatepicker.DateModel;
import org.jdatepicker.impl.JDatePanelImpl;
import org.jdatepicker.impl.JDatePickerImpl;
import org.jdatepicker.impl.UtilDateModel;

import javax.swing.JCheckBox;
import javax.swing.border.Border;
import javax.swing.BoxLayout;
import javax.swing.Box;
import javax.swing.BorderFactory;
import javax.swing.JFormattedTextField;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.Calendar;
import java.util.Properties;

import static javax.swing.BoxLayout.LINE_AXIS;
import static javax.swing.BoxLayout.PAGE_AXIS;
import static javax.swing.BoxLayout.X_AXIS;

public class SearchPanel extends JPanel {
    private static final int PAGE_SIZE=100;
    private static final int DEFAULT_PAGE=0;
    private static final String DEFAULT_PAGE_PREFIX="Page ";
    private static final String DEFAULT_PAGE_LABEL=DEFAULT_PAGE_PREFIX + DEFAULT_PAGE;
    private final JLabel pageNumberLabel = label("Page 0");
    private final JButton pageNumberButtonNext = new JButton("Next");
    private final JButton pageNumberButtonPrevious = new JButton("Prev");
    private final JTextField patientName = textField();
    private final JCheckBox fuzzyMatching = textBox("fuzzy match", false);
    private final JTextField patientId = textField();
    private final JDatePickerImpl startDate = createDatePicker();
    private final JDatePickerImpl endDate = createDatePicker();
    private final JTextField accessionNumber = textField();
    private final JTextField referringPhd = textField();

    private final DicomStoreSelector storeSelector;
    private int pageNumber;

    public SearchPanel(DicomStoreSelector storeSelector) {
        this.storeSelector = storeSelector;
        initSearchPanel();
    }

    private void initSearchPanel() {
        this.storeSelector.addStoreListener((event) -> reloadTable());
        BoxLayout layout = new BoxLayout(this, PAGE_AXIS);
        setLayout(layout);
        Border border = BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 0, 2, Color.GRAY),
                BorderFactory.createEmptyBorder(5, 5, 5, 15));
        setBorder(border);
        setPreferredSize(new Dimension(300, 300));

        JLabel searchLabel = label("Search");
        searchLabel.setFont(new Font("Sans-serif", Font.BOLD, 18));
        add(searchLabel);

        add(Box.createVerticalStrut(10));

        JLabel studyDateLabel = label("Study date");
        add(studyDateLabel);
        JPanel datePanel = new JPanel();
        BoxLayout datePanelLayout = new BoxLayout(datePanel, LINE_AXIS);
        datePanel.setLayout(datePanelLayout);
        datePanel.add(startDate);
        datePanel.add(endDate);
        add(datePanel);

        add(Box.createVerticalStrut(20));

        JLabel patientNameLabel = label("Patient Name");
        add(patientNameLabel);
        add(patientName);
        JPanel fuzzyMatchingPanel = new JPanel();
        BoxLayout fuzzyMatchingPanelLayout = new BoxLayout(fuzzyMatchingPanel, X_AXIS);
        fuzzyMatchingPanel.setLayout(fuzzyMatchingPanelLayout);
        fuzzyMatchingPanel.add(fuzzyMatching);
        add(fuzzyMatchingPanel);

        add(Box.createVerticalStrut(20));

        JLabel patientIdLabel = label("Patient ID");
        add(patientIdLabel);
        add(patientId);

        add(Box.createVerticalStrut(20));

        JLabel accessionNumberLabel = label("Accession number");
        add(accessionNumberLabel);
        add(accessionNumber);

        add(Box.createVerticalStrut(20));

        JLabel referringPhdLabel = label("Referring Physician");
        add(referringPhdLabel);
        add(referringPhd);

        add(Box.createVerticalStrut(20));

        JButton searchButton = new JButton("Search");
        searchButton.addActionListener((action) -> reloadTable());
        JButton reset = new JButton("Reset");
        setPageNumber(DEFAULT_PAGE);

        reset.addActionListener((action) -> {
            clearSearchForm();
            reloadTable();
            pageNumberLabel.setText(DEFAULT_PAGE_LABEL);
        });

        pageNumberButtonPrevious.addActionListener((action) -> {
            previousPage();
        });

        pageNumberButtonNext.addActionListener((action) -> {
            nextPage();
        });

        JPanel buttonPanel = new JPanel();
        BoxLayout buttonPanelLayout = new BoxLayout(buttonPanel, LINE_AXIS);
        buttonPanel.setLayout(buttonPanelLayout);
        buttonPanel.add(searchButton);
        buttonPanel.add(Box.createHorizontalGlue());
        buttonPanel.add(reset);
        add(buttonPanel);

        add(Box.createVerticalGlue());
    }

    public void loadTable(int page) {
        storeSelector.loadStudies(buildQuery(page));
    }

    public void reloadTable() {
        setPageNumber(0);
        loadTable(pageNumber);
    }


    public void previousPage() {
        if(this.pageNumber >0){
            setPageNumber(pageNumber-1);
            loadTable(pageNumber);
        }
    }
    public void nextPage() {
        setPageNumber(pageNumber+1);
        loadTable(pageNumber);
    }

    private void clearSearchForm() {
        patientName.setText("");
        patientId.setText("");
        accessionNumber.setText("");
        referringPhd.setText("");
        startDate.getModel().setSelected(false);
        endDate.getModel().setSelected(false);
    }

    private StudyQuery buildQuery(int page) {
        StudyQuery query = new StudyQuery();
        query.setPatientName(patientName.getText());
        query.setFuzzyMatching(fuzzyMatching.isSelected());
        query.setPatientId(patientId.getText());
        query.setAccessionNumber(accessionNumber.getText());
        query.setPhysicianName(referringPhd.getText());
        query.setPage(page);
        query.setPageSize(PAGE_SIZE);
        DateModel<?> startDateModel = startDate.getModel();
        if (startDateModel.isSelected()) {
            query.setStartDate(LocalDate.of(startDateModel.getYear(), startDateModel.getMonth() + 1, startDateModel.getDay()));
        }

        DateModel<?> endDateModel = endDate.getModel();
        if (endDateModel.isSelected()) {
            query.setEndDate(LocalDate.of(endDateModel.getYear(), endDateModel.getMonth() + 1, endDateModel.getDay()));
        }

        return query;
    }

    private JDatePickerImpl createDatePicker() {
        UtilDateModel model = new UtilDateModel();
        Properties p = new Properties();
        p.put("text.today", "Today");
        p.put("text.month", "Month");
        p.put("text.year", "Year");
        JDatePanelImpl datePanel = new JDatePanelImpl(model, p);
        return new JDatePickerImpl(datePanel, new DateLabelFormatter());
    }

    private JTextField textField() {
        JTextField result = new JTextField();
        result.setMaximumSize(
                new Dimension(Integer.MAX_VALUE, result.getPreferredSize().height));

        return result;
    }

    private JCheckBox textBox(String text, boolean selected) {
        JCheckBox checkBox = new JCheckBox(text, selected);
        checkBox.setMaximumSize(
                new Dimension(Integer.MAX_VALUE, checkBox.getPreferredSize().height));
        return checkBox;
    }

    private JLabel label(String text) {
        JLabel result = new JLabel(text);

        result.setAlignmentX(CENTER_ALIGNMENT);
        result.setHorizontalTextPosition(SwingConstants.CENTER);

        return result;
    }

    private void setPageNumber(int pageNumber) {
        this.pageNumber = pageNumber;
        this.pageNumberLabel.setText(DEFAULT_PAGE_PREFIX+String.valueOf(pageNumber));
        if(pageNumber==DEFAULT_PAGE){
            pageNumberButtonPrevious.setVisible(false);
        }else{
            pageNumberButtonPrevious.setVisible(true);
        }
    }

    public JLabel getPageNumberLabel() {
        return pageNumberLabel;
    }
    public JButton getPageNumberButtonNext() {
        return pageNumberButtonNext;
    }
    public JButton getPageNumberButtonPrevious() {
        return pageNumberButtonPrevious;
    }


    public class DateLabelFormatter extends JFormattedTextField.AbstractFormatter {

        private String datePattern = "yyyy-MM-dd";
        private SimpleDateFormat dateFormatter = new SimpleDateFormat(datePattern);

        @Override
        public Object stringToValue(String text) throws ParseException {
            return dateFormatter.parseObject(text);
        }

        @Override
        public String valueToString(Object value) throws ParseException {
            if (value != null) {
                Calendar cal = (Calendar) value;
                return dateFormatter.format(cal.getTime());
            }

            return "";
        }

    }
}
