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

import java.util.Comparator;
import java.util.List;

public class LoadDicomStoresTask extends AbstractDicomSelectorTask<List<DicomStore>> {

    private final Dataset dataset;

    public LoadDicomStoresTask(Dataset dataset,
                               GoogleAPIClient api,
                               DicomStoreSelector view) {
        super(api, view);
        this.dataset = dataset;
    }

    @Override
    protected List<DicomStore> doInBackground() throws Exception {
        List<DicomStore> locations = api.fetchDicomstores(dataset);
        locations.sort(Comparator.comparing(DicomStore::getName));
        return locations;
    }

    @Override
    protected void onCompleted(List<DicomStore> result) {
        view.updateDicomStores(result);
    }
}
