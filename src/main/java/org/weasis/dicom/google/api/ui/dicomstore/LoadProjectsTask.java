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
import org.weasis.dicom.google.api.model.ProjectDescriptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Comparator;
import java.util.List;

public class LoadProjectsTask extends AbstractDicomSelectorTask<List<ProjectDescriptor>> {
    private static final Logger LOGGER = LoggerFactory.getLogger(LoadProjectsTask.class);

    public LoadProjectsTask(GoogleAPIClient api, DicomStoreSelector view) {
        super(api, view);
    }

    @Override
    protected List<ProjectDescriptor> doInBackground() throws Exception {
        List<ProjectDescriptor> projects = api.fetchProjects();
        projects.sort(Comparator.comparing(ProjectDescriptor::getName));
        return projects;
    }

    @Override
    protected void onCompleted(List<ProjectDescriptor> result) {
        LOGGER.debug("Loaded projects list " + result);
        view.updateProjects(result);
    }
}
