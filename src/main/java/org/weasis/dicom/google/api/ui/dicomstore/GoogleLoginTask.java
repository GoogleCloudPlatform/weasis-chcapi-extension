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

import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.SwingWorker;

import org.weasis.core.api.gui.util.GuiExecutor;
import org.weasis.dicom.google.api.GoogleAPIClient;
import org.weasis.dicom.google.explorer.Messages;

public class GoogleLoginTask extends SwingWorker<Void, Void> {
    protected final GoogleAPIClient googleAPIClient;
    protected final JButton googleAuthButton;
    protected final DicomStoreSelector view;

    private static final String TEXT_GOOGLE_SIGN_IN = Messages.getString("DicomStoreSelector.sign_in"); //$NON-NLS-1$
    private static final String TEXT_GOOGLE_SIGN_OUT = Messages.getString("DicomStoreSelector.sign_out"); //$NON-NLS-1$
    private static final String ACTION_SIGN_OUT = Messages.getString("DicomStoreSelector.sign_out");
    private static final String ACTION_SIGN_IN = Messages.getString("DicomStoreSelector.sign_in");

    public GoogleLoginTask(GoogleAPIClient apiClient, JButton googleAuthButton, DicomStoreSelector view) {
        this.googleAPIClient = apiClient;
        this.googleAuthButton = googleAuthButton;
        this.view = view;
    }


    @Override
    protected Void doInBackground() {
        try {
            googleAPIClient.signIn();
            googleAuthButton.setText(TEXT_GOOGLE_SIGN_OUT);
            googleAuthButton.setActionCommand(ACTION_SIGN_OUT);
            new LoadProjectsTask(googleAPIClient, view).execute();
        } catch (Exception ex) {
            GuiExecutor.instance().invokeAndWait(() -> JOptionPane.showMessageDialog(null,
                "Error occured on fetching google API.\n" +
                        "Make sure you created OAuth Client ID credential \n" +
                        "in Google Cloud console at https://console.cloud.google.com/apis/credentials \n" +
                        "and copied your client_secrets.json to Weasis root folder.\n" +
                        "Error message:" + ex.getCause().getMessage()));

            googleAPIClient.signOut();
            googleAuthButton.setText(TEXT_GOOGLE_SIGN_IN);
            googleAuthButton.setActionCommand(ACTION_SIGN_IN);
        }
        return null;
    }

}
