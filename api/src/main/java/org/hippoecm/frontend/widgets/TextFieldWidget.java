/*
 *  Copyright 2008 Hippo.
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
package org.hippoecm.frontend.widgets;

import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.IModel;
import org.apache.wicket.util.time.Duration;

public class TextFieldWidget extends AjaxUpdatingWidget<String> {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;

    private String size;

    public TextFieldWidget(String id, IModel<String> model) {
        this(id, model, null);
    }

    public TextFieldWidget(String id, IModel<String> model, IModel<String> labelModel) {
        this(id, model, labelModel, null);
    }

    public TextFieldWidget(String id, IModel<String> model, IModel<String> labelModel, Duration throttleDelay) {
        super(id, model, throttleDelay);
        TextField<String> t;
        addFormField(t = new TextField<String>("widget", model) {
            private static final long serialVersionUID = 1L;

            @Override
            protected void onComponentTag(final ComponentTag tag) {
                if (getSize() != null) {
                    tag.put("size", getSize());
                }
                super.onComponentTag(tag);
            }
        });
        t.setType(String.class);
        if (labelModel != null) {
           t.setLabel(labelModel);
        }
    }

    public void setSize(String size) {
        this.size = size;
    }

    public String getSize() {
        return size;
    }
}
