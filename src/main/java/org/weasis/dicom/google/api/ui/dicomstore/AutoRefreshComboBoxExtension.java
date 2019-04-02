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

import javax.swing.JComboBox;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.function.Supplier;

public class AutoRefreshComboBoxExtension {

    private static final long TIME_TO_INVALIDATE_CACHE_MS = 10_000;

    private long lastUpdateTime = System.currentTimeMillis();

    private AutoRefreshComboBoxExtension(JComboBox<?> comboBox, Supplier<Boolean> reload) {
        addDataUpdateListener(comboBox);
        addDataReloadListener(comboBox, reload);
    }

    public static AutoRefreshComboBoxExtension wrap(JComboBox<?> comboBox, Supplier<Boolean> reload) {
        return new AutoRefreshComboBoxExtension(comboBox, reload);
    }

    private void addDataReloadListener(JComboBox<?> comboBox, Supplier<Boolean> reload) {
        comboBox.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (isTimeoutPassed()) {
                    if (Boolean.TRUE.equals(reload.get())) {
                        lastUpdateTime = System.currentTimeMillis();
                    }
                }
            }
        });
    }

    private void addDataUpdateListener(JComboBox<?> comboBox) {
        comboBox.getModel().addListDataListener(new ListDataListener() {
            @Override
            public void intervalAdded(ListDataEvent e) {
                lastUpdateTime = System.currentTimeMillis();
            }

            @Override
            public void intervalRemoved(ListDataEvent e) {
                lastUpdateTime = System.currentTimeMillis();
            }

            @Override
            public void contentsChanged(ListDataEvent e) {
                lastUpdateTime = System.currentTimeMillis();
            }
        });
    }

    private boolean isTimeoutPassed() {
        return (System.currentTimeMillis() - lastUpdateTime) > TIME_TO_INVALIDATE_CACHE_MS;
    }

}
