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
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.weasis.dicom.google.api.model.StudyQuery;

import static org.junit.Assert.assertEquals;

@RunWith(JUnit4.class)
public class GoogleAPIClientTest {

    static{
        System.setProperty("weasis.resources.path","./");
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
}
