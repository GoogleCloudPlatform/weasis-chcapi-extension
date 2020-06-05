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

import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.LowLevelHttpRequest;
import com.google.api.client.http.LowLevelHttpResponse;
import com.google.api.client.json.Json;
import com.google.api.client.testing.http.HttpTesting;
import com.google.api.client.testing.http.MockHttpTransport;
import com.google.api.client.testing.http.MockLowLevelHttpRequest;
import com.google.api.client.testing.http.MockLowLevelHttpResponse;
import com.google.api.services.cloudresourcemanager.CloudResourceManager;
import com.google.api.services.cloudresourcemanager.CloudResourceManager.Projects;
import com.google.api.services.cloudresourcemanager.model.ListProjectsResponse;
import com.google.api.services.cloudresourcemanager.model.Project;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.Test;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;

import org.mockito.Mockito;
import org.mockito.stubbing.Answer;
import org.mockito.invocation.InvocationOnMock;

import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.modules.junit4.PowerMockRunner;
import org.weasis.dicom.google.api.model.Dataset;
import org.weasis.dicom.google.api.model.DicomStore;
import org.weasis.dicom.google.api.model.Location;
import org.weasis.dicom.google.api.model.ProjectDescriptor;
import org.weasis.dicom.google.api.model.StudyQuery;
import org.powermock.core.classloader.annotations.PrepareForTest;

import com.google.api.client.http.HttpHeaders;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.HttpStatusCodes;
import com.google.api.client.http.HttpResponseException;
import com.google.api.client.http.HttpResponseException.Builder;

import static org.junit.Assert.assertEquals;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.when;

/**
 * Tests for {@link GoogleAPIClient} class.
 * 
 * @author Mikhail Ukhlin
 */
@PowerMockIgnore("javax.swing.*")
@RunWith(PowerMockRunner.class)
@PrepareForTest(GoogleAPIClient.class)
public class GoogleAPIClientTest {
  
  /**
   * Sets up test environment.
   */
  @BeforeClass
  public static void setup() {
    // This is necessary to initialize static fields
    System.setProperty("weasis.resources.path", "/");
  }
  
  /**
   * Tests {@link GoogleAPIClient#executeGetRequest(String)} method for token expired case.
   */
  @Test
  public void testTokenExpired() throws Exception {
    // Given
    final String url = "https://test.com/test";
    final GoogleAPIClient client = PowerMockito.spy(
        GoogleAPIClientFactory.getInstance().createGoogleClient());
    
    // When
    Mockito.doReturn("TEST-TOKEN-1").when(client).signIn();
    Mockito.doReturn("TEST-TOKEN-2").when(client).refresh();
    
    PowerMockito.doAnswer(new Answer<HttpResponse>() {
      boolean tokenExpired = true;
      @Override public HttpResponse answer(InvocationOnMock invocation) throws Throwable {
        // Throw exception first time emulating token expired and return null second time
        if (tokenExpired) {
          tokenExpired = false;
          throw new HttpResponseException(
              new Builder(HttpStatusCodes.STATUS_CODE_UNAUTHORIZED, "TEST", new HttpHeaders())) {
            private static final long serialVersionUID = -1765357098467474492L;
          };
        }
        // Cannot return HTTPResponse because it has package private constructor
        return null;
      }
    }).when(client, "doExecuteGetRequest", Mockito.anyString(), Mockito.any());
    
    client.executeGetRequest(url);
    
    // Then
    Mockito.verify(client, Mockito.times(1)).signIn();
    Mockito.verify(client, Mockito.times(1)).refresh();
    Mockito.verify(client, Mockito.times(1)).executeGetRequest(Mockito.eq(url));
    Mockito.verify(client, Mockito.times(1)).executeGetRequest(Mockito.eq(url), Mockito.any());
    PowerMockito.verifyPrivate(client, Mockito.times(2)).invoke("doExecuteGetRequest",
        Mockito.eq(url), Mockito.any());
  }
  
  @Test
  public void testNullQuery() throws Exception {
      assertEquals("?includefield=all",GoogleAPIClient.formatQuery(null));
  }
  @Test
  public void testfuzzyMatching() throws Exception {
      StudyQuery query = new StudyQuery();

      query.setFuzzyMatching(true);
      assertEquals("?includefield=all",GoogleAPIClient.formatQuery(query));

      query.setPatientName("name");
      query.setFuzzyMatching(true);
      assertEquals("?PatientName=name&fuzzymatching=true",GoogleAPIClient.formatQuery(query));

      query.setPatientName("name");
      query.setFuzzyMatching(false);
      assertEquals("?PatientName=name&fuzzymatching=false",GoogleAPIClient.formatQuery(query));

  }


  @Test
  public void testPagination() throws Exception {
      StudyQuery query = new StudyQuery();

      query.setPage(0);
      assertEquals("?includefield=all",GoogleAPIClient.formatQuery(query));

      query.setPage(5);
      assertEquals("?includefield=all",GoogleAPIClient.formatQuery(query));

      query.setPage(0);
      query.setPageSize(100);
      assertEquals("?limit=100&offset=0",GoogleAPIClient.formatQuery(query));

      query.setPage(2);
      query.setPageSize(50);
      assertEquals("?limit=50&offset=100",GoogleAPIClient.formatQuery(query));
  }

  @Test
  public void testShouldReturnProjectsWithNotNullNamesAndIds() throws Exception {
    // Given
    GoogleAPIClient client = PowerMockito.spy(
        GoogleAPIClientFactory.getInstance().createGoogleClient());
    Mockito.doReturn("TEST").when(client).signIn();
    Field cloudResourceManagerField = GoogleAPIClient.class
        .getDeclaredField("cloudResourceManager");
    cloudResourceManagerField.setAccessible(true);
    CloudResourceManager cloudResourceManager = mock(CloudResourceManager.class);
    cloudResourceManagerField.set(null, cloudResourceManager);
    Projects projectsMock = mock(Projects.class);
    when(cloudResourceManager.projects()).thenReturn(projectsMock);
    Projects.List listMock = mock(Projects.List.class);
    when(projectsMock.list()).thenReturn(listMock);
    ListProjectsResponse response = new ListProjectsResponse();

    // First project
    Project project1 = new Project();
    project1.setName("Project1");
    project1.setProjectId("id1");
    // Second with null name
    Project project2 = new Project();
    project2.setName(null);
    project2.setProjectId("id2");
    // Third with null projectId
    Project project3 = new Project();
    project3.setName("Project3");
    project3.setProjectId(null);
    List<Project> projects = new ArrayList<>(Arrays.asList(project1, project2, project3));

    response.setProjects(projects);
    when(listMock.execute()).thenReturn(response);

    // When
    List<ProjectDescriptor> allProjects = client.fetchProjects();

    // Then
    assertEquals(1, allProjects.size());
  }

  @Test(expected = Exception.class)
  public void testFetchDatasetsShouldReturnExceptionIfResponseContainZeroDatasets()
      throws Exception {
    // Given
    final GoogleAPIClient client = PowerMockito.spy(
        GoogleAPIClientFactory.getInstance().createGoogleClient());
    HttpTransport transport = new MockHttpTransport() {
      @Override
      public LowLevelHttpRequest buildRequest(String method, String url) throws IOException {
        return new MockLowLevelHttpRequest() {
          @Override
          public LowLevelHttpResponse execute() throws IOException {
            MockLowLevelHttpResponse response = new MockLowLevelHttpResponse();
            response.setStatusCode(200);
            response.setContentType(Json.MEDIA_TYPE);
            response.setContent("{}");
            return response;
          }
        };
      }
    };
    HttpRequest request = transport.createRequestFactory()
        .buildGetRequest(HttpTesting.SIMPLE_GENERIC_URL);
    HttpResponse response = request.execute();
    PowerMockito.doReturn(response).when(client, "executeGetRequest", Mockito.any());
    ProjectDescriptor projectDescriptor = new ProjectDescriptor("Test-1", "1");
    Location location = new Location(projectDescriptor, "projects/1/locations/1", "1");

    // Then
    client.fetchDatasets(location);
  }

  @Test
  public void testFetchDatasetsShouldReturnDatasetsIfResponseContainDatasets() throws Exception {
    // Given
    final GoogleAPIClient client = PowerMockito.spy(
        GoogleAPIClientFactory.getInstance().createGoogleClient());
    HttpTransport transport = new MockHttpTransport() {
      @Override
      public LowLevelHttpRequest buildRequest(String method, String url) throws IOException {
        return new MockLowLevelHttpRequest() {
          @Override
          public LowLevelHttpResponse execute() throws IOException {
            MockLowLevelHttpResponse response = new MockLowLevelHttpResponse();
            response.setStatusCode(200);
            response.setContentType(Json.MEDIA_TYPE);
            response.setContent("{\n"
                + "  \"datasets\": [\n"
                + "    {\n"
                + "      \"name\": \"projects/1/locations/1/datasets/Test-1\"\n"
                + "    },\n"
                + "    {\n"
                + "      \"name\": \"projects/1/locations/1/datasets/Test-2\"\n"
                + "    }\n"
                + "  ]\n"
                + "}");
            return response;
          }
        };
      }
    };
    HttpRequest request = transport.createRequestFactory()
        .buildGetRequest(HttpTesting.SIMPLE_GENERIC_URL);
    HttpResponse response = request.execute();
    PowerMockito.doReturn(response).when(client, "executeGetRequest", Mockito.any());
    ProjectDescriptor projectDescriptor = new ProjectDescriptor("Test-1", "1");
    Location location = new Location(projectDescriptor, "projects/1/locations/1", "1");

    // When
    List<Dataset> datasetList = client.fetchDatasets(location);

    // Then
    assertEquals(2, datasetList.size());
  }

  @Test(expected = Exception.class)
  public void testFetchDicomstoresShouldReturnExceptionIfResponseContainZeroDicomstores()
      throws Exception {
    // Given
    final GoogleAPIClient client = PowerMockito.spy(
        GoogleAPIClientFactory.getInstance().createGoogleClient());
    HttpTransport transport = new MockHttpTransport() {
      @Override
      public LowLevelHttpRequest buildRequest(String method, String url) throws IOException {
        return new MockLowLevelHttpRequest() {
          @Override
          public LowLevelHttpResponse execute() throws IOException {
            MockLowLevelHttpResponse response = new MockLowLevelHttpResponse();
            response.setStatusCode(200);
            response.setContentType(Json.MEDIA_TYPE);
            response.setContent("{}");
            return response;
          }
        };
      }
    };
    HttpRequest request = transport.createRequestFactory()
        .buildGetRequest(HttpTesting.SIMPLE_GENERIC_URL);
    HttpResponse response = request.execute();
    PowerMockito.doReturn(response).when(client, "executeGetRequest", Mockito.any());
    ProjectDescriptor projectDescriptor = new ProjectDescriptor("Test-1", "1");
    Location location = new Location(projectDescriptor, "projects/1/locations/1", "1");
    Dataset dataset = new Dataset(location, "projects/1/locations/1/datasets/Test-1");

    // Then
    client.fetchDicomstores(dataset);
  }

  @Test
  public void testFetchDicomstoresShouldReturnDicomstoresIfResponseContainDicomstores()
      throws Exception {
    // Given
    final GoogleAPIClient client = PowerMockito.spy(
        GoogleAPIClientFactory.getInstance().createGoogleClient());
    HttpTransport transport = new MockHttpTransport() {
      @Override
      public LowLevelHttpRequest buildRequest(String method, String url) throws IOException {
        return new MockLowLevelHttpRequest() {
          @Override
          public LowLevelHttpResponse execute() throws IOException {
            MockLowLevelHttpResponse response = new MockLowLevelHttpResponse();
            response.setStatusCode(200);
            response.setContentType(Json.MEDIA_TYPE);
            response.setContent("{\n"
                + "  \"dicomStores\": [\n"
                + "    {\n"
                + "      \"name\": \"projects/1/locations/1/datasets/Test-1/dicomStores/Test-1\"\n"
                + "    },\n"
                + "    {\n"
                + "      \"name\": \"projects/1/locations/1/datasets/Test-1/dicomStores/Test-2\"\n"
                + "    }\n"
                + "  ]\n"
                + "}");
            return response;
          }
        };
      }
    };
    HttpRequest request = transport.createRequestFactory()
        .buildGetRequest(HttpTesting.SIMPLE_GENERIC_URL);
    HttpResponse response = request.execute();
    PowerMockito.doReturn(response).when(client, "executeGetRequest", Mockito.any());
    ProjectDescriptor projectDescriptor = new ProjectDescriptor("Test-1", "1");
    Location location = new Location(projectDescriptor, "projects/1/locations/1", "1");
    Dataset dataset = new Dataset(location, "projects/1/locations/1/datasets/Test-1");

    // When
    List<DicomStore> dicomStoreList = client.fetchDicomstores(dataset);

    // Then
    assertEquals(2, dicomStoreList.size());
  }
}
