/*
 * Copyright 2015 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.hippoecm.frontend.plugins.standardworkflow.validators;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.wicket.Component;
import org.apache.wicket.markup.html.form.validation.IFormValidator;
import org.apache.wicket.model.StringResourceModel;
import org.hippoecm.frontend.dialog.AbstractDialog;

abstract class DocumentFormValidator implements IFormValidator {

    private final AbstractDialog dialog;
    private final Component component;

    public DocumentFormValidator(final AbstractDialog dialog, final Component component) {
        this.dialog = dialog;
        this.component = component;
    }

    /**
     * Return true if <code>parentNode</code> contains a child having the same localized name with the specified
     * <code>localizedName</code>
     */
    protected boolean hasChildWithLocalizedName(final Node parentNode, final String localizedName) throws RepositoryException {
        return SameNameSiblingsUtil.hasChildWithLocalizedName(parentNode, localizedName);
    }

    protected void showError(final String key, Object... parameters) {
        dialog.error(new StringResourceModel(key, component, null, parameters).getObject());
    }
}
