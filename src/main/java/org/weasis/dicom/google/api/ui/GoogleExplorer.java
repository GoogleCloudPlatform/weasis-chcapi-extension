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

import org.weasis.dicom.google.api.GoogleAPIClient;
import org.weasis.dicom.google.api.ui.dicomstore.DicomStoreSelector;
import org.weasis.dicom.google.explorer.DownloadManager;

import javax.swing.JPanel;
import javax.swing.BoxLayout;
import javax.swing.Box;
import java.awt.*;

import static javax.swing.BoxLayout.PAGE_AXIS;

public class GoogleExplorer extends JPanel {

    private final StudiesTable table;

    private final GoogleAPIClient googleAPIClient;
    private final DicomStoreSelector storeSelector;

    private final SearchPanel searchPanel;

    public GoogleExplorer(GoogleAPIClient googleAPIClient) {
        this.googleAPIClient = googleAPIClient;

        BorderLayout layout = new BorderLayout();

        layout.setHgap(15);
        setLayout(layout);

        table = new StudiesTable(this);
        storeSelector = new DicomStoreSelector(googleAPIClient, table);
        searchPanel = new SearchPanel(googleAPIClient, storeSelector);

        add(centralComponent(), BorderLayout.CENTER);
        add(searchPanel, BorderLayout.WEST);
    }

    public Component centralComponent() {
        JPanel panel = new JPanel();
        BoxLayout layout = new BoxLayout(panel, PAGE_AXIS);
        panel.setLayout(layout);

        panel.add(storeSelector);
        panel.add(Box.createVerticalStrut(10));
        panel.add(table);

        return panel;
    }
    
    public void fireStudySelected(String studyId) {
        storeSelector.getCurrentStore()
                .map(store -> GoogleAPIClient.getImageUrl(store, studyId))
                .ifPresent(image -> {
                	DownloadManager.getLoadingExecutor().submit(
                        new DownloadManager.LoadGoogleDicom(image,  null, googleAPIClient, new DownloadManager.DownloadListener() {
							@Override
							public void downloadFinished() {
								table.hideLoadIcon(studyId);
							}
						}));
                	table.showLoadIcon(studyId);
                });
    }

}
