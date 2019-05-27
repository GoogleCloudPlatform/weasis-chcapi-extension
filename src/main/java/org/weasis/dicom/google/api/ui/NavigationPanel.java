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

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.FlowLayout;

public class NavigationPanel extends JPanel {
    private SearchPanel searchPanel;

    public NavigationPanel(SearchPanel searchPanel) {
        this.searchPanel = searchPanel;
        JButton previousButton = searchPanel.getPageNumberButtonPrevious();
        JButton nextButton = searchPanel.getPageNumberButtonNext();
        JLabel pageNumberLabel = searchPanel.getPageNumberLabel();
        this.setLayout(new FlowLayout(FlowLayout.LEFT));
        this.add(previousButton);
        this.add(pageNumberLabel);
        this.add(nextButton);
    }

}