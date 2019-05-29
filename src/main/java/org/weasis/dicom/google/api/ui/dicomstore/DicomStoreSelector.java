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

package org.weasis.dicom.google.api.ui.dicomstore;

import org.weasis.dicom.google.api.GoogleAPIClient;
import org.weasis.dicom.google.api.model.Dataset;
import org.weasis.dicom.google.api.model.DicomStore;
import org.weasis.dicom.google.api.model.Location;
import org.weasis.dicom.google.api.model.ProjectDescriptor;
import org.weasis.dicom.google.api.model.StudyQuery;
import org.weasis.dicom.google.api.ui.StudiesTable;
import org.weasis.dicom.google.api.ui.StudyView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.weasis.dicom.google.explorer.Messages;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JOptionPane;
import javax.swing.JComboBox;
import javax.swing.JTextField;
import javax.swing.JList;
import javax.swing.ListCellRenderer;
import javax.swing.SwingWorker;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.plaf.basic.BasicComboBoxEditor;
import javax.swing.plaf.basic.BasicComboPopup;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Window;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

import static java.util.Collections.emptyList;

public class DicomStoreSelector extends JPanel {

    private static final Logger log = LoggerFactory.getLogger(DicomStoreSelector.class);

    private static final String TEXT_GOOGLE_SIGN_IN = Messages.getString("DicomStoreSelector.sign_in"); //$NON-NLS-1$
    private static final String DEFAULT_PROJECT_COMBOBOX_TEXT = Messages.getString("DicomStoreSelector.default_project_text"); //$NON-NLS-1$
    private static final String DEFAULT_LOCATION_COMBOBOX_TEXT = Messages.getString("DicomStoreSelector.default_location_text"); //$NON-NLS-1$
    private static final String DEFAULT_DATASET_COMBOBOX_TEXT = Messages.getString("DicomStoreSelector.default_dataset_text"); //$NON-NLS-1$
    private static final String DEFAULT_DICOMSTORE_COMBOBOX_TEXT = Messages.getString("DicomStoreSelector.default_dicomstore_text"); //$NON-NLS-1$
    private static final String ACTION_SIGN_IN = Messages.getString("DicomStoreSelector.sign_in");

    private final GoogleAPIClient googleAPIClient;

    private final DefaultComboBoxModel<Optional<ProjectDescriptor>> modelProject = new DefaultComboBoxModel<>();
    private final DefaultComboBoxModel<Optional<Location>> modelLocation = new DefaultComboBoxModel<>();
    private final DefaultComboBoxModel<Optional<Dataset>> modelDataset = new DefaultComboBoxModel<>();
    private final DefaultComboBoxModel<Optional<DicomStore>> modelDicomstore = new DefaultComboBoxModel<>();

    private final StudiesTable table;
    private List<ProjectDescriptor> projects;
    private final JProjectComboBox<Optional<ProjectDescriptor>> googleProjectCombobox = new JProjectComboBox<>(modelProject);
    private final JComboBox<Optional<Location>> googleLocationCombobox = new JComboBox<>(modelLocation);
    private final JComboBox<Optional<Dataset>> googleDatasetCombobox = new JComboBox<>(modelDataset);
    private final JComboBox<Optional<DicomStore>> googleDicomstoreCombobox = new JComboBox<>(modelDicomstore);
    private final JButton googleAuthButton = new JButton();


    private void processSignedIn(JButton googleAuthButton) {
        new GoogleLoginTask(googleAPIClient, googleAuthButton, this).execute();
    }

    private void toLogoutState(){
        googleAuthButton.setText(TEXT_GOOGLE_SIGN_IN);
        googleAuthButton.setActionCommand(ACTION_SIGN_IN);
        googleProjectCombobox.setEnabled(false);
        googleLocationCombobox.setEnabled(false);
        googleDatasetCombobox.setEnabled(false);
        googleDicomstoreCombobox.setEnabled(false);
        modelProject.removeAllElements();
        modelLocation.removeAllElements();
        modelDataset.removeAllElements();
        modelDicomstore.removeAllElements();
        table.clearTable();
        JTextField textField = (JTextField) googleProjectCombobox.getEditor().getEditorComponent();
        textField.setText("");
    }

    public DicomStoreSelector(GoogleAPIClient googleAPIClient, StudiesTable table) {
        UIManager.getLookAndFeelDefaults().put("ComboBox.noActionOnKeyNavigation", true);
        this.googleAPIClient = googleAPIClient;
        this.table = table;
        BoxLayout layout = new BoxLayout(this, BoxLayout.X_AXIS);
        setLayout(layout);

        googleProjectCombobox.setPrototypeDisplayValue(Optional.empty());
        googleLocationCombobox.setPrototypeDisplayValue(Optional.empty());
        googleDatasetCombobox.setPrototypeDisplayValue(Optional.empty());
        googleDicomstoreCombobox.setPrototypeDisplayValue(Optional.empty());

        if (googleAPIClient.isAuthorized()) {
            processSignedIn(googleAuthButton);
        } else {
            toLogoutState();
        }
        googleAuthButton.addActionListener(e -> {
            if (!googleAPIClient.isAuthorized()) {
                processSignedIn(googleAuthButton);
            } else {
                googleAPIClient.signOut();
                toLogoutState();
            }
        });

        add(googleProjectCombobox);
        add(Box.createHorizontalStrut(10));
        add(googleLocationCombobox);
        add(Box.createHorizontalStrut(10));
        add(googleDatasetCombobox);
        add(Box.createHorizontalStrut(10));
        add(googleDicomstoreCombobox);
        add(Box.createHorizontalStrut(10));
        add(googleAuthButton);
        add(Box.createHorizontalGlue());

        googleProjectCombobox.setRenderer(new ListRenderer<>(ProjectDescriptor::getName, DEFAULT_PROJECT_COMBOBOX_TEXT));
        googleLocationCombobox.setRenderer(new ListRenderer<>(Location::getId, DEFAULT_LOCATION_COMBOBOX_TEXT));
        googleDatasetCombobox.setRenderer(new ListRenderer<>(Dataset::getName, DEFAULT_DATASET_COMBOBOX_TEXT));
        googleDicomstoreCombobox.setRenderer(new ListRenderer<>(DicomStore::getName, DEFAULT_DICOMSTORE_COMBOBOX_TEXT));
        googleProjectCombobox.setLightWeightPopupEnabled(false);

        googleProjectCombobox.addItemListener(this.<ProjectDescriptor>selectedListener(
                project -> new LoadLocationsTask(project, googleAPIClient, this),
                nothing -> updateLocations(emptyList())
        ));

        AutoRefreshComboBoxExtension.wrap(googleProjectCombobox, () -> {
            log.info("Reloading projects");
            new LoadProjectsTask(googleAPIClient, DicomStoreSelector.this).execute();
            return true;
        });

        googleProjectCombobox.setEditor(new JProjectComboBoxEditor(DEFAULT_PROJECT_COMBOBOX_TEXT));
        googleProjectCombobox.getEditor().getEditorComponent().addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (googleProjectCombobox.firstFocusGain) {
                    ((JTextField) googleProjectCombobox.getEditor().getEditorComponent()).setText("");
                    googleProjectCombobox.firstFocusGain = false;
                }
                super.mousePressed(e);
            }
        });
        googleProjectCombobox.getEditor().getEditorComponent().addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent arg0) {
                JTextField textField = (JTextField) googleProjectCombobox.getEditor().getEditorComponent();
                if (googleProjectCombobox.firstFocusGain) {
                    textField.setText(textField.getText().replaceAll(DEFAULT_PROJECT_COMBOBOX_TEXT, ""));
                    googleProjectCombobox.firstFocusGain = false;
                }
                googleProjectCombobox.search(textField.getText());
            }
        });

        googleLocationCombobox.addItemListener(this.<Location>selectedListener(
                location -> new LoadDatasetsTask(location, googleAPIClient, this),
                nothing -> updateDatasets(emptyList())
        ));

        AutoRefreshComboBoxExtension.wrap(googleLocationCombobox, () ->
                getSelectedItem(modelProject).map(
                        (project) -> {
                            log.info("Reloading locations");
                            new LoadLocationsTask(project, googleAPIClient, DicomStoreSelector.this).execute();
                            return true;
                        }
                ).orElse(false)
        );

        googleDatasetCombobox.addItemListener(this.<Dataset>selectedListener(
                dataset -> new LoadDicomStoresTask(dataset, googleAPIClient, this),
                nothing -> updateDicomStores(emptyList())
        ));

        AutoRefreshComboBoxExtension.wrap(googleDatasetCombobox, () ->
                getSelectedItem(modelLocation).map(
                        (location) -> {
                            log.info("Reloading Datasets");
                            new LoadDatasetsTask(location, googleAPIClient, DicomStoreSelector.this).execute();
                            return true;
                        }
                ).orElse(false)
        );
        googleDicomstoreCombobox.addItemListener(item ->
                emitStoreUpdateUpdate(new StoreUpdateEvent(this)));

        AutoRefreshComboBoxExtension.wrap(googleDicomstoreCombobox, () ->
                getSelectedItem(modelDataset).map(
                        (dataset) -> {
                            log.info("Reloading Dicom stores");
                            new LoadDicomStoresTask(dataset, googleAPIClient, DicomStoreSelector.this).execute();
                            return true;
                        }
                ).orElse(false)
        );
    }

    private LoadStudiesTask loadStudiesTask(DicomStore store, StudyQuery query) {
        return new LoadStudiesTask(store, googleAPIClient, this, query);
    }

    public void updateProjects(List<ProjectDescriptor> result) {
        googleProjectCombobox.setEnabled(true);
        projects = result;
        if (updateModel(result, modelProject)) {
            googleProjectCombobox.firstFocusGain = true;
            JTextField textField = (JTextField) googleProjectCombobox.getEditor().getEditorComponent();
            textField.setText(DEFAULT_PROJECT_COMBOBOX_TEXT);
            updateLocations(emptyList());
        }
    }

    public void updateLocations(List<Location> result) {
        googleLocationCombobox.setEnabled(true);
        if (updateModel(result, modelLocation)) {
            updateDatasets(emptyList());
        }
    }

    public void updateDatasets(List<Dataset> result) {
        googleDatasetCombobox.setEnabled(true);
        if (updateModel(result, modelDataset)) {
            updateDicomStores(emptyList());
        }
    }

    public void updateDicomStores(List<DicomStore> result) {
        googleDicomstoreCombobox.setEnabled(true);
        if (updateModel(result, modelDicomstore)) {
            updateTable(emptyList());
        }
    }

    public void updateTable(List<StudyView> studies) {
        table.clearTable();
        studies.forEach(table::addStudy);
    }

    public Optional<DicomStore> getCurrentStore() {
        Object store = modelDicomstore.getSelectedItem();
        Optional<DicomStore> storeOptional = Optional.empty();
        if (Objects.nonNull(store)) {
            storeOptional = (Optional<DicomStore>) modelDicomstore.getSelectedItem();
        }
        return storeOptional;
    }

    /**
     * @return true if selected item changed, false otherwise
     */
    private <T> boolean updateModel(List<T> list, DefaultComboBoxModel<Optional<T>> model) {
        Optional<T> selectedItem = (Optional<T>) model.getSelectedItem();
        return Optional.ofNullable(selectedItem)
                .flatMap(x -> x)
                .filter(list::contains)
                .map(item -> {
                    replaceAllExcludingItem(item, list, model);
                    return false;
                })
                .orElseGet(() -> {
                    model.removeAllElements();
                    if (!list.isEmpty()) {
                        model.addElement(Optional.empty());
                        list.stream().map(Optional::of).forEach(model::addElement);
                        model.setSelectedItem(Optional.empty());
                    }
                    return true;
                });
    }

    private <T> void replaceAllExcludingItem(T selectedItem, List<T> list, DefaultComboBoxModel<Optional<T>> model) {
        List<Optional<T>> toDelete = new ArrayList<>();
        for (int i = 0; i < model.getSize(); i++) {
            Optional<T> currentItem = model.getElementAt(i);
            if (!Objects.equals(currentItem, Optional.of(selectedItem))) {
                toDelete.add(currentItem);
            }
        }
        toDelete.forEach(model::removeElement);

        int selectedIndex = list.indexOf(selectedItem);
        model.insertElementAt(Optional.empty(), 0);
        for (int i = 0; i < list.size(); i++) {
            if (selectedIndex != i) {
                if (selectedIndex > i) {
                    model.insertElementAt(Optional.of(list.get(i)), model.getSize() - 1);
                } else {
                    model.insertElementAt(Optional.of(list.get(i)), model.getSize());
                }
            }
        }

    }

    private <T> ItemListener selectedListener(Function<T, SwingWorker<?, ?>> taskFactory, Consumer<Void> onEmpty) {
        return e -> {
            if (e.getStateChange() == ItemEvent.SELECTED && e.getItem().getClass().equals(Optional.class)) {
                Optional<T> item = (Optional<T>) e.getItem();

                if (!item.isPresent()) {
                    onEmpty.accept(null);
                }

                item.map(taskFactory).ifPresent(SwingWorker::execute);
            }
        };
    }

    private static <T> Optional<T> getSelectedItem(DefaultComboBoxModel<Optional<T>> model) {
        return Optional.ofNullable(model.getSelectedItem())
                .flatMap(x -> (Optional<T>) x);
    }

    /** Loads studies
     * @param query parameters for study request
     */
    public void loadStudies(StudyQuery query) {
        getCurrentStore().ifPresent((store) -> {
            loadStudiesTask(store, query).execute();
        });
    }

    /** Notifying all store update listeners with update event
     * @param storeUpdateEvent event to pass
     */
    public void emitStoreUpdateUpdate(StoreUpdateEvent storeUpdateEvent) {
        Object[] listeners = listenerList.getListenerList();
        for (int i = 0; i < listeners.length; i = i + 2) {
            if (listeners[i] == StoreUpdateListener.class) {
                ((StoreUpdateListener) listeners[i + 1]).actionPerformed(storeUpdateEvent);
            }
        }
    }

    /** Adds StoreUpdateListener listener to common listeners list
     * @param listener listener to add
     */
    public void addStoreListener(StoreUpdateListener listener) {
        this.listenerList.add(StoreUpdateListener.class, listener);
    }

    private class ListRenderer<T> implements ListCellRenderer<Optional<T>> {

        private final DefaultListCellRenderer renderer = new DefaultListCellRenderer();
        private final String defaultLabel;
        private final Function<T, String> textExtractor;

        public ListRenderer(Function<T, String> textExtractor, String defaultLabel) {
            this.textExtractor = textExtractor;
            this.defaultLabel = defaultLabel;
        }

        @Override
        public Component getListCellRendererComponent(JList<? extends Optional<T>> list,
                                                      Optional<T> value,
                                                      int index,
                                                      boolean isSelected,
                                                      boolean cellHasFocus) {
            renderer.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            if (value != null) {
                String label = value.map(textExtractor).orElse(defaultLabel);
                renderer.setText(label);
            }
            return renderer;
        }
    }

    private class JProjectComboBoxEditor extends BasicComboBoxEditor {
        public JProjectComboBoxEditor(String defaultText) {
            editor.setText(defaultText);
        }

        public void setItem(Object anObject) {
            if (anObject != null) {
                Optional<ProjectDescriptor> item = anObject.getClass().equals(String.class) ? Optional.empty() : (Optional<ProjectDescriptor>) anObject;
                if (item.isPresent()) {
                    editor.setText(item.get().getName());
                }
            }
        }
    }

    private class JProjectComboBox<T> extends JComboBox<T> {

        private static final long serialVersionUID = 450383631220222610L;
        private boolean firstFocusGain = true;
        private String prevInput = "";

        public JProjectComboBox(DefaultComboBoxModel<T> model) {
            super(model);
        }

        public void search(String input) {
            if ((input == null && prevInput == null) || (input.toLowerCase().equals(prevInput.toLowerCase()))) {
                return;
            }
            removeAllItems();
            List<ProjectDescriptor> updated = new ArrayList<>();
            for (int i = 0; i < projects.size(); i++) {
                ProjectDescriptor item = projects.get(i);
                if (item.getName().toLowerCase().contains(input.toLowerCase())) {
                    updated.add(item);
                }
            }
            prevInput = input;
            updateModel(updated, modelProject);
            if (updated.isEmpty() || (updated.size() == 1 && updated.get(0).equals(input))) {
                this.setPopupVisible(false);
                return;
            }
            this.setPopupVisible(true);
            BasicComboPopup popup = (BasicComboPopup) this.getAccessibleContext().getAccessibleChild(0);
            Window popupWindow = SwingUtilities.windowForComponent(popup);
            Window comboWindow = SwingUtilities.windowForComponent(this);

            if (comboWindow.equals(popupWindow)) {
                Component c = popup.getParent();
                Dimension d = c.getPreferredSize();
                c.setSize(d);
            } else {
                popupWindow.pack();
            }
        }
    }

}
