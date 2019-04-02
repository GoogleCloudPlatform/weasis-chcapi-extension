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

package org.weasis.dicom.google.api.model;

import java.util.Objects;

public class DicomStore {

    private final Dataset parent;

    private final String name;

    public DicomStore(Dataset parent, String name) {
        this.parent = parent;
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public Dataset getParent() {
        return parent;
    }

    public ProjectDescriptor getProject() {
        return parent.getProject();
    }

    public Location getLocation() {
        return parent.getParent();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DicomStore that = (DicomStore) o;
        return Objects.equals(parent, that.parent) &&
                Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(parent, name);
    }
}
