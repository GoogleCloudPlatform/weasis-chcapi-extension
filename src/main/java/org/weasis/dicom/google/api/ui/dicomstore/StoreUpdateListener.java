package org.weasis.dicom.google.api.ui.dicomstore;

import java.util.EventListener;

public interface StoreUpdateListener extends EventListener {
    void actionPerformed(StoreUpdateEvent var1);
}