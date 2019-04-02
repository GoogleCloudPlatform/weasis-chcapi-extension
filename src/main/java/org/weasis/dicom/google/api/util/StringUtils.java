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

package org.weasis.dicom.google.api.util;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Collection;

public final class StringUtils {

    private StringUtils() {
    }

    public static boolean isNotBlank(String str) {
        return str != null
                && !str.trim().isEmpty();
    }

    public static String urlEncode(String str) {
        try {
            return URLEncoder.encode(str, "UTF-8");
        } catch (UnsupportedEncodingException ex) {
            throw new IllegalStateException("Error on encoding url " + str, ex);
        }
    }

    public static String join(Collection<String> collection, String joinString) {
        StringBuilder builder = new StringBuilder();
        for (String str : collection) {
            if (builder.length() > 0) {
                builder.append(joinString);
            }
            builder.append(str);
        }

        return builder.toString();
    }
}
