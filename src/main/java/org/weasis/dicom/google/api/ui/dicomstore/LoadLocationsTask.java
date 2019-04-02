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
import org.weasis.dicom.google.api.model.Location;
import org.weasis.dicom.google.api.model.ProjectDescriptor;

import java.util.Comparator;
import java.util.List;

public class LoadLocationsTask extends AbstractDicomSelectorTask<List<Location>> {

    private final ProjectDescriptor project;

    public LoadLocationsTask(ProjectDescriptor project,
                             GoogleAPIClient api,
                             DicomStoreSelector view) {
        super(api, view);
        this.project = project;
    }

    @Override
    protected List<Location> doInBackground() throws Exception {
        List<Location> locations = api.fetchLocations(project);
        locations.sort(Comparator.comparing(Location::getName));
        return locations;
    }

    @Override
    protected void onCompleted(List<Location> result) {
        view.updateLocations(result);
    }
}
