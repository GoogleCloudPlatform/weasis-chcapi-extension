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

import java.awt.AWTEvent;

/** Store update event to notify listeners
 */
public class StoreUpdateEvent extends AWTEvent {
    static int id;

    /** Store update event constructor calls AWTEvent constructor with source object and unique id
     * @return Store update event.
     */
    public StoreUpdateEvent(Object source) {
        super(source, id++);
    }

}