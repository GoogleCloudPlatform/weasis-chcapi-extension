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

package org.weasis.dicom.google.api;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.weasis.dicom.google.api.model.Dataset;
import org.weasis.dicom.google.api.model.DicomStore;
import org.weasis.dicom.google.api.model.Location;
import org.weasis.dicom.google.api.model.ProjectDescriptor;
import org.weasis.dicom.google.api.model.StudyModel;
import org.weasis.dicom.google.api.model.StudyQuery;
import org.weasis.dicom.google.api.ui.OAuth2Browser;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpHeaders;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.HttpResponseException;
import com.google.api.client.http.HttpStatusCodes;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.store.DataStoreFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.cloudresourcemanager.CloudResourceManager;
import com.google.api.services.cloudresourcemanager.model.ListProjectsResponse;
import com.google.api.services.cloudresourcemanager.model.Project;
import com.google.api.services.oauth2.Oauth2;
import com.google.api.services.oauth2.model.Tokeninfo;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

import java.io.*;
import java.security.GeneralSecurityException;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static org.weasis.dicom.google.api.util.StringUtils.*;

public class GoogleAPIClient {

    private static final String APPLICATION_NAME = "Weasis-GoogleDICOMExplorer/1.0";

    /**
     * Directory to store user credentials.
     */
    private static final java.io.File DATA_STORE_DIR = new java.io.File(System.getProperty("user.home"), ".weasis/google_auth");
    private static final String GOOGLE_API_BASE_PATH  = "https://healthcare.googleapis.com/v1beta1";
    private static final String SECRETS_FILE_NAME = "client_secrets.json";
    /**
     * Global instance of the {@link DataStoreFactory}. The best practice is to make
     * it a single globally shared instance across your application.
     */
    private static FileDataStoreFactory dataStoreFactory;

    /**
     * Global instance of the HTTP transport.
     */
    private static HttpTransport httpTransport;

    /**
     * Global instance of the JSON factory.
     */
    private static final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();

    /**
     * OAuth 2.0 scopes.
     */
    private static final List<String> SCOPES = Arrays.asList(
    		"https://www.googleapis.com/auth/cloud-healthcare",
    		"https://www.googleapis.com/auth/cloudplatformprojects.readonly"
    		);

    private static Oauth2 oauth2;
    private static GoogleClientSecrets clientSecrets;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");
    /**
     * Instance of Google Cloud Resource Manager
     */
    private static CloudResourceManager cloudResourceManager;

    private boolean isSignedIn = false;
    private String accessToken;

    protected GoogleAPIClient() {
    }

    private static GoogleAuthorizationCodeFlow authorizationCodeFlow() throws GeneralSecurityException, IOException {
        httpTransport = GoogleNetHttpTransport.newTrustedTransport();
        dataStoreFactory = new FileDataStoreFactory(DATA_STORE_DIR);

        try (InputStream in = getSecret()) {
            clientSecrets = GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(in));
        }
        if (clientSecrets.getDetails().getClientId().startsWith("Enter")
                || clientSecrets.getDetails().getClientSecret().startsWith("Enter ")) {
            throw new RuntimeException(SECRETS_FILE_NAME + " not found");
        }
        return new GoogleAuthorizationCodeFlow.Builder(httpTransport, JSON_FACTORY,
                clientSecrets, SCOPES).setDataStoreFactory(dataStoreFactory).build();
    }

    private static Credential authorize() throws Exception {
        // set up authorization code flow
        GoogleAuthorizationCodeFlow flow = authorizationCodeFlow();
        // authorize
        return new AuthorizationCodeInstalledApp(flow, new LocalServerReceiver(),
            OAuth2Browser.INSTANCE).authorize("user");
    }

  private static InputStream getSecret() throws IOException {
    String extendedProperties = System.getProperty("felix.extended.config.properties")
        .replace("file:", "");
    Path extendedPropertiesFolderPath = Paths.get(extendedProperties).getParent();
    Path clientSecretsFile = extendedPropertiesFolderPath.resolve(SECRETS_FILE_NAME);
    if (Files.exists(clientSecretsFile)) {
      System.out.println("Loading user's client_secret.json");
      return Files.newInputStream(clientSecretsFile);
    } else {
      System.out.println("Loading embedded client_secret.json");
      return GoogleAPIClient.class.getResource("/" + SECRETS_FILE_NAME).openStream();
    }
  }

    public String getAccessToken() {
        if (accessToken == null) {
            isSignedIn = false;
            signIn();
        }
        return accessToken;
    }

    public String signIn() {
        if (!isSignedIn) {
            int tryCount = 0;
            Exception error;
            do {
                try {
                    tryCount++;
                    // authorization
                    Credential credential = authorize();
                    // set up global Oauth2 instance
                    oauth2 = new Oauth2.Builder(httpTransport, JSON_FACTORY, credential).setApplicationName(APPLICATION_NAME)
                            .build();

                    cloudResourceManager = new CloudResourceManager.Builder(httpTransport, JSON_FACTORY, credential)
                            .build();
                    accessToken = credential.getAccessToken();
                    // run commands
                    tokenInfo(accessToken);
                    error = null;
                    isSignedIn = true;
                } catch (Exception e) {
                    error = e;
                }
            } while (!isSignedIn && tryCount < 4);
            if (error != null) {
                throw new IllegalStateException(error);
            }
        }
        return accessToken;
    }

    public void signOut() {
        clearSignIn();
        isSignedIn = false;
    }

    public String refresh() {
        if (isSignedIn) {
            isSignedIn = false;
            return signIn();
        }
        return getAccessToken();
    }

    private void clearSignIn() {
        accessToken = null;
        deleteDir(DATA_STORE_DIR);
    }

    public boolean isAuthorized() {
        boolean isAuthorized;
        try {
            GoogleAuthorizationCodeFlow flow = authorizationCodeFlow();
            isAuthorized = Objects.nonNull(flow.loadCredential("user"));
        } catch (IOException| GeneralSecurityException e) {
            isAuthorized = false;
        }
        return isAuthorized;
    }

    private void deleteDir(File file) {
        if (!file.exists()) {
            return;
        }
        if (file.isDirectory()) {
            for (File child : file.listFiles()) {
                deleteDir(child);
            }
        }
        file.delete();
    }

    private static void tokenInfo(String accessToken) throws IOException {
        Tokeninfo tokeninfo = oauth2.tokeninfo().setAccessToken(accessToken).execute();
        if (!tokeninfo.getAudience().equals(clientSecrets.getDetails().getClientId())) {
            System.err.println("ERROR: audience does not match our client ID!");
        }
    }

    public List<org.weasis.dicom.google.api.model.ProjectDescriptor> fetchProjects() throws Exception {
        refresh();
        List<org.weasis.dicom.google.api.model.ProjectDescriptor> result = new ArrayList<org.weasis.dicom.google.api.model.ProjectDescriptor>();
        CloudResourceManager.Projects.List request = cloudResourceManager.projects().list();
        ListProjectsResponse response;
        do {
            response = request.execute();
            if (response.getProjects() == null) {
                continue;
            }
            for (Project project : response.getProjects()) {
                String name = project.getName();
                String projectId = project.getProjectId();
                if (name != null && projectId != null) {
                    result.add(new org.weasis.dicom.google.api.model.ProjectDescriptor(name, projectId));
                }
            }
            request.setPageToken(response.getNextPageToken());
        } while (response.getNextPageToken() != null);
        return result;
    }

    private String parseName(String name) {
        return name.substring(name.lastIndexOf("/") + 1);
    }

    /**
     * Executes HTTP GET request using the specified URL.
     * 
     * @param url HTTP request URL.
     * @return HTTP response.
     * @throws IOException if an IO error occurred.
     * @throws HttpResponseException if an error status code is detected in response.
     * @see #executeGetRequest(String, HttpHeaders)
     */
    public HttpResponse executeGetRequest(String url) throws IOException {
      return executeGetRequest(url, new HttpHeaders());
    }
    
    /**
     * Executes a HTTP GET request with the specified URL and headers. GCP authorization is done
     * if the user is not already signed in. The access token is refreshed if it has expired
     * (HTTP 401 is returned from the server) and the request is retried with the new access token.
     * 
     * @param url HTTP request URL.
     * @param headers HTTP request headers.
     * @return HTTP response.
     * @throws IOException if an IO error occurred.
     * @throws HttpResponseException if an error status code is detected in response.
     * @see #signIn()
     * @see #refresh()
     */
    public HttpResponse executeGetRequest(String url, HttpHeaders headers) throws IOException {
      signIn();
      try {
        return doExecuteGetRequest(url, headers);
      } catch (HttpResponseException e) {
        // Token expired?
        if (e.getStatusCode() == HttpStatusCodes.STATUS_CODE_UNAUTHORIZED) {
          // Refresh token and try again
          refresh();
          return doExecuteGetRequest(url, headers);
        }
        throw e;
      }
    }
    
    /**
     * Performs actual HTTP GET request using the specified URL and headers. This method also adds
     * {@code Authorization} header initialized with OAuth access token. 
     * 
     * @param url HTTP request URL.
     * @param headers HTTP request headers.
     * @return HTTP response.
     * @throws IOException if an IO error occurred.
     * @throws HttpResponseException if an error status code is detected in response.
     * @see #doExecuteGetRequest(String, HttpHeaders)
     */
    private HttpResponse doExecuteGetRequest(String url, HttpHeaders headers) throws IOException {
      final HttpRequest request = httpTransport.createRequestFactory().buildGetRequest(
          new GenericUrl(url));
      headers.setAuthorization("Bearer " + accessToken);
      request.setHeaders(headers);
      return request.execute();
    }

    public List<org.weasis.dicom.google.api.model.Location> fetchLocations(ProjectDescriptor project) throws Exception {
        String url = GOOGLE_API_BASE_PATH + "/projects/" + project.getId() + "/locations";
        String data = executeGetRequest(url).parseAsString();
        JsonParser parser = new JsonParser();
        JsonElement jsonTree = parser.parse(data);
        JsonArray jsonObject = jsonTree.getAsJsonObject().get("locations").getAsJsonArray();
        return StreamSupport.stream(jsonObject.spliterator(), false)
                .map(JsonElement::getAsJsonObject)
                .map(obj -> new org.weasis.dicom.google.api.model.Location(project,
                        obj.get("name").getAsString(),
                        obj.get("locationId").getAsString()))
                .collect(Collectors.toList());
    }

    public List<Dataset> fetchDatasets(Location location) throws Exception {
        String url = GOOGLE_API_BASE_PATH + "/projects/" + location.getParent().getId() + "/locations/" + location.getId() + "/datasets";
        String data = executeGetRequest(url).parseAsString();
        JsonParser parser = new JsonParser();
        JsonElement jsonTree = parser.parse(data);
        JsonArray jsonObject = jsonTree.getAsJsonObject().get("datasets").getAsJsonArray();
        return StreamSupport.stream(jsonObject.spliterator(), false)
                .map(obj -> obj.getAsJsonObject().get("name").getAsString())
                .map(this::parseName)
                .map(name -> new Dataset(location, name))
                .collect(Collectors.toList());
    }

    public List<DicomStore> fetchDicomstores(Dataset dataset) throws Exception {
        String url = GOOGLE_API_BASE_PATH
                + "/projects/" + dataset.getProject().getId()
                + "/locations/" + dataset.getParent().getId()
                + "/datasets/" + dataset.getName() + "/dicomStores";
        String data = executeGetRequest(url).parseAsString();
        JsonParser parser = new JsonParser();
        JsonElement jsonTree = parser.parse(data);
        JsonArray jsonObject = jsonTree.getAsJsonObject().get("dicomStores").getAsJsonArray();

        return StreamSupport.stream(jsonObject.spliterator(), false)
                .map(obj -> obj.getAsJsonObject().get("name").getAsString())
                .map(this::parseName)
                .map(name -> new DicomStore(dataset, name))
                .collect(Collectors.toList());
    }

    public List<StudyModel> fetchStudies(DicomStore store, StudyQuery query) throws Exception {
        String url = GOOGLE_API_BASE_PATH
                + "/projects/" + store.getProject().getId()
                + "/locations/" + store.getLocation().getId()
                + "/datasets/" + store.getParent().getName()
                + "/dicomStores/" + store.getName()
                + "/dicomWeb/studies" + formatQuery(query);
        String data = executeGetRequest(url).parseAsString();
        List<StudyModel> studies = objectMapper.readValue(data, new TypeReference<List<StudyModel>>() {
        });

        return studies;
    }

    public static String getImageUrl(DicomStore store, String studyId) {
        return GOOGLE_API_BASE_PATH
                + "/projects/" + store.getProject().getId()
                + "/locations/" + store.getLocation().getId()
                + "/datasets/" + store.getParent().getName()
                + "/dicomStores/" + store.getName()
                + "/dicomWeb/studies/" + studyId;
    }

  /**
   * Generate String with GET variables for study request url
   * 
   * @param query source of data for GET variables
   * @return GET variables
   * @see StudyQuery
   */
  public static String formatQuery(StudyQuery query) {
    String allItems = "?includefield=all";
    if (Objects.isNull(query)) {
      return allItems;
    }

    List<String> parameters = new ArrayList<>();
    if (isNotBlank(query.getPatientName())) {
      parameters.add("PatientName=" + urlEncode(query.getPatientName()));
      parameters.add("fuzzymatching=" + (query.getFuzzyMatching() ? "true" : "false"));
    }

    if (isNotBlank(query.getPatientId())) {
      parameters.add("PatientID=" + urlEncode(query.getPatientId()));
    }

    if (isNotBlank(query.getAccessionNumber())) {
      parameters.add("AccessionNumber=" + urlEncode(query.getAccessionNumber()));
    }

    if (query.getStartDate() != null && query.getEndDate() != null) {
      parameters.add("StudyDate=" + urlEncode(DATE_FORMAT.format(query.getStartDate())) + "-"
          + urlEncode(DATE_FORMAT.format(query.getEndDate())));
    }

    int pageNumber = query.getPage();
    int pageSize = query.getPageSize();

    if (pageSize > 0) {
      parameters.add("limit=" + String.valueOf(pageSize));

      if (pageNumber >= 0) {
        parameters.add("offset=" + String.valueOf(pageNumber * pageSize));
      }
    }

    if (isNotBlank(query.getPhysicianName())) {
      parameters.add("ReferringPhysicianName=" + urlEncode(query.getPhysicianName()));
    }

    if (parameters.isEmpty()) {
      return allItems;
    } else {
      return "?" + join(parameters, "&");
    }
  }
  
}

