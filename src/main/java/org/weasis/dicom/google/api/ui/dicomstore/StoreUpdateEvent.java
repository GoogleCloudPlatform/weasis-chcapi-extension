package org.weasis.dicom.google.api.ui.dicomstore;

import java.awt.*;
public class StoreUpdateEvent extends AWTEvent {
    static int id;
    public StoreUpdateEvent(Object source) {
        super(source, id++);
    }

}