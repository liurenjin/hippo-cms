/*
 *  Copyright 2010 Hippo.
 * 
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 * 
 *       http://www.apache.org/licenses/LICENSE-2.0
 * 
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.hippoecm.frontend.translation.components.document;

import java.util.Arrays;
import java.util.List;

import org.apache.wicket.util.lang.PropertyResolver;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wicketstuff.js.ext.data.ExtField;
import org.wicketstuff.js.ext.data.ExtJsonStore;
import org.wicketstuff.js.ext.util.ExtProperty;
import org.wicketstuff.js.ext.util.JSONIdentifier;

final class FolderTranslationStore extends ExtJsonStore<FolderTranslation> {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;

    static final Logger log = LoggerFactory.getLogger(FolderTranslationStore.class);

    @SuppressWarnings("unused")
    @ExtProperty
    private boolean autoSave = false;

    private final List<FolderTranslation> translations;

    FolderTranslationStore(List<FolderTranslation> translations) {
        super(Arrays.asList(new ExtField("name"), new ExtField("namefr"), new ExtField("url"),
                new ExtField("urlfr"), new ExtField("id"), new ExtField("type"), new ExtField(
                        "editable", Boolean.class)));
        this.translations = translations;
    }

    @Override
    protected JSONObject updateRecord(JSONObject record) throws JSONException {
        String id = record.getString("id");
        for (FolderTranslation data : translations) {
            if (data.getId().equals(id)) {
                if (record.has("namefr") && !"".equals(record.getString("namefr"))) {
                    data.setNamefr(record.getString("namefr"));
                }
                if (record.has("urlfr") && !"".equals(record.getString("urlfr"))) {
                    data.setUrlfr(record.getString("urlfr"));
                }

                JSONObject jsonLine = new JSONObject();
                for (ExtField field : getFields()) {
                    Object value = PropertyResolver.getValue(field.getName(), data);
                    jsonLine.put(field.getName(), value);
                }
                return jsonLine;
            }
        }
        return null;
    }

    @Override
    protected JSONObject getProperties() throws JSONException {
        JSONObject properties = super.getProperties();
        properties.put("writer", new JSONIdentifier("new Ext.data.JsonWriter()"));
        return properties;
    }

    @Override
    protected JSONArray getData() throws JSONException {
        JSONArray jsonData = new JSONArray();
        for (FolderTranslation record : translations) {
            JSONObject jsonLine = new JSONObject();
            for (ExtField field : getFields()) {
                Object value = PropertyResolver.getValue(field.getName(), record);
                jsonLine.put(field.getName(), value);
            }
            jsonData.put(jsonLine);
        }
        return jsonData;
    }
}