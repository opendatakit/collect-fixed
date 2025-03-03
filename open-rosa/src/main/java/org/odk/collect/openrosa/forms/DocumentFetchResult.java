/*
 * Copyright (C) 2011 University of Washington
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package org.odk.collect.openrosa.forms;

import org.kxml2.kdom.Document;

public class DocumentFetchResult {
    public final String errorMessage;
    public final int responseCode;
    public final Document doc;
    public final boolean isOpenRosaResponse;
    private String hash;

    public DocumentFetchResult(String msg, int response) {
        responseCode = response;
        errorMessage = msg;
        doc = null;
        isOpenRosaResponse = false;
    }

    public DocumentFetchResult(Document doc, boolean isOpenRosaResponse, String hash) {
        responseCode = 0;
        errorMessage = null;
        this.doc = doc;
        this.isOpenRosaResponse = isOpenRosaResponse;
        this.hash = hash;
    }

    public String getHash() {
        return hash;
    }
}
