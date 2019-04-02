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

public class Location {

    private final ProjectDescriptor parent;

    private final String name;
    private final String id;

    public Location(ProjectDescriptor parent, String name, String id) {
        this.parent = parent;
        this.name = name;
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public String getId() {
        return id;
    }

    public ProjectDescriptor getParent() {
        return parent;
    }

    @Override
    public String toString() {
        return id;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Location location = (Location) o;
        return Objects.equals(parent, location.parent) &&
                Objects.equals(name, location.name) &&
                Objects.equals(id, location.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(parent, name, id);
    }
}
