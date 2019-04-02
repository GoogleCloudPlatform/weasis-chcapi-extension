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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.SwingWorker;
import javax.swing.JOptionPane;
import java.util.concurrent.ExecutionException;

public abstract class AbstractDicomSelectorTask<T> extends SwingWorker<T, Void> {
    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractDicomSelectorTask.class);

    protected final GoogleAPIClient api;
    protected final DicomStoreSelector view;

    public AbstractDicomSelectorTask(GoogleAPIClient api, DicomStoreSelector view) {
        this.api = api;
        this.view = view;
    }

    @Override
    protected final void done() {
        try {
            T result = get();
            onCompleted(result);
        } catch (ExecutionException ex) {
            LOGGER.error("Error on dicom task", ex.getCause());
            JOptionPane.showMessageDialog(null, "Unexpected error on fetching google api: " + ex.getCause().getMessage());
        } catch (InterruptedException ex) {
            LOGGER.error("Interrupted", ex);
        }
    }

    protected abstract void onCompleted(T result);
}
