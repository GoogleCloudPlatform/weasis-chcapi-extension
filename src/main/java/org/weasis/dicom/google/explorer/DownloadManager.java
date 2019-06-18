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

package org.weasis.dicom.google.explorer;

import org.apache.commons.fileupload.MultipartStream;
import org.dcm4che3.data.Tag;
import org.slf4j.LoggerFactory;
import org.weasis.core.api.explorer.model.DataExplorerModel;
import org.weasis.core.api.gui.util.AppProperties;
import org.weasis.core.api.media.MimeInspector;
import org.weasis.core.api.media.data.*;
import org.weasis.core.api.util.FileUtil;
import org.weasis.core.api.util.ThreadUtil;
import org.weasis.core.ui.editor.FileModel;
import org.weasis.core.ui.editor.ViewerPluginBuilder;
import org.weasis.dicom.codec.DicomMediaIO;
import org.weasis.dicom.codec.DicomCodec;
import org.weasis.dicom.codec.TagD;
import org.weasis.dicom.google.api.GoogleAPIClient;
import com.google.api.client.http.HttpHeaders;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.HttpStatusCodes;
import javax.swing.SwingWorker;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;

public class DownloadManager {

    static final ExecutorService LOADING_EXECUTOR = ThreadUtil.buildNewFixedThreadExecutor(2, "Google Dicom Explorer"); //$NON-NLS-1$

    public static ExecutorService getLoadingExecutor() {
        return LOADING_EXECUTOR;
    }

    public interface DownloadListener {
    	public void downloadFinished();
    }
    
    public static class LoadGoogleDicom extends SwingWorker<Boolean, Void> {

        private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(LoadGoogleDicom.class);
        private final GoogleAPIClient client;
        private File[] files;
        private final String url;
        private final FileModel dicomModel;
        private static final Map<String, File[]> fileCache = new HashMap<>();
        public static final File DICOM_TMP_DIR = AppProperties.buildAccessibleTempDirectory("gcp_cache"); //$NON-NLS-1$
        private DownloadListener downloadListener;


        public LoadGoogleDicom(String url, DataExplorerModel explorerModel, GoogleAPIClient client, DownloadListener listener) {
            this.url = url;
            this.dicomModel = ViewerPluginBuilder.DefaultDataModel;
            this.client = client;
            this.downloadListener = listener;
        }

        @Override
        protected Boolean doInBackground() throws Exception {
            if (url == null) {
                throw new IllegalArgumentException("invalid parameters"); //$NON-NLS-1$
            }
            if (fileCache.containsKey(url)) {
                LOGGER.info("Loading from local cache");
                files = fileCache.get(url);
            } else {
                LOGGER.info("Loading from Google Healthcare API");
                files = downloadFiles(url);
                fileCache.put(url, files);
            }
            LOGGER.debug(Arrays.stream(files).map(f -> f.getName()).collect(Collectors.joining("\n")));

            addSelectionAndnotify(files);
            return true;
        }

        @Override
        protected void done() {
            LOGGER.info("End of loading DICOM from Google Healthcare API"); //$NON-NLS-1$
            downloadListener.downloadFinished();
        }

        public void addSelectionAndnotify(File[] file) {
            if (file == null || file.length < 1) {
                return;
            }

            MediaSeries<MediaElement> dicomSeries = null;
            for (File file1 : file) {
                if (isCancelled()) {
                    LOGGER.info("Download cancelled, returning");
                    return;
                }

                if (file1 == null) {
                    continue;

                } else {

                    if (file1.canRead()) {
                        if (FileUtil.isFileExtensionMatching(file1, DicomCodec.FILE_EXTENSIONS)
                                || MimeInspector.isMatchingMimeTypeFromMagicNumber(file1, DicomMediaIO.MIMETYPE)) {
                            DicomMediaIO loader = new DicomMediaIO(file1);
                            if (loader.isReadableDicom()) {
                                if (dicomSeries == null) {
                                    dicomSeries = loader.getMediaSeries();
                                } else {
                                    dicomSeries.addAll(Arrays.asList(loader.getMediaElement()));
                                }
                            }
                        }
                    }
                }
            }
            List<MediaElement> sortedSeries = dicomSeries.getSortedMedias(instanceNumberComparator);
            dicomSeries.dispose();
            dicomSeries.addAll(sortedSeries);
            ViewerPluginBuilder.openSequenceInDefaultPlugin(dicomSeries, dicomModel, true, true);
        }

        private File[] downloadFiles(String dicomUrl) {
            try {
                final HttpHeaders headers = new HttpHeaders();
                headers.setAccept("multipart/related; type=application/dicom; transfer-syntax=*");
            	final HttpResponse response = client.executeGetRequest(dicomUrl, headers);
            	final int responseCode = response.getStatusCode();
            	
                if (responseCode == HttpStatusCodes.STATUS_CODE_OK) {
                    String contentType = response.getContentType();
                    //find multipart boundary of multipart/related response
                    int indexStart = contentType.indexOf("boundary=") + 9;
                    int indexEnd = contentType.indexOf(";", indexStart + 1);
                    if (indexEnd == -1) {
                        indexEnd = contentType.length() - 1;
                    }
                    String boundary = contentType.substring(indexStart, indexEnd);

                    MultipartStream multipart = new MultipartStream(response.getContent(), boundary.getBytes());
                    boolean nextPart = multipart.skipPreamble();

                    ArrayList<File> files = new ArrayList<>();
                    long start = System.currentTimeMillis();
                    while (nextPart) {
                        File outFile = File.createTempFile("gcp_", ".dcm", getDicomTmpDir()); //$NON-NLS-1$ //$NON-NLS-2$
                        String header = multipart.readHeaders();

                        try (OutputStream output = new FileOutputStream(outFile)) {
                            multipart.readBodyData(output);
                        }
                        files.add(outFile);
                        nextPart = multipart.readBoundary();
                    }
                    LOGGER.debug("Elapsed time: {} ", System.currentTimeMillis() - start);
                    return files.toArray(new File[0]);
                } else {
                    throw new RuntimeException("Error processing HTTP request. Response code: " + responseCode);
                }
            } catch (Exception e) {
                LOGGER.error("Error occured ", e);
                throw new RuntimeException(e);
            }
        }

        // Solves missing tmp folder problem (on Windows).
        private static File getDicomTmpDir() {
            if (!DICOM_TMP_DIR.exists()) {
                LOGGER.info("DICOM tmp dir not found. Re-creating it."); //$NON-NLS-1$
                AppProperties.buildAccessibleTempDirectory("gcp_cache"); //$NON-NLS-1$
            }
            return DICOM_TMP_DIR;
        }
    }

    private static final Comparator<MediaElement> instanceNumberComparator = (m1, m2) -> {
        Integer val1 = TagD.getTagValue(m1, Tag.InstanceNumber, Integer.class);
        Integer val2 = TagD.getTagValue(m2, Tag.InstanceNumber, Integer.class);
        if (val1 == null || val2 == null) {
            return 0;
        }
        return val1.compareTo(val2);
    };
}
