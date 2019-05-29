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

import org.junit.Test;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;

import org.mockito.Mockito;
import org.mockito.stubbing.Answer;
import org.mockito.invocation.InvocationOnMock;

import org.powermock.api.mockito.PowerMockito;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.core.classloader.annotations.PrepareForTest;

import com.google.api.client.http.HttpHeaders;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.HttpStatusCodes;
import com.google.api.client.http.HttpResponseException;
import com.google.api.client.http.HttpResponseException.Builder;

/**
 * Tests for {@link GoogleAPIClient} class.
 * 
 * @author Mikhail Ukhlin
 */
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
  
}
