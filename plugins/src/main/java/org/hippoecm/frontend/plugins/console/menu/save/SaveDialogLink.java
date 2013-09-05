/*
 *  Copyright 2011 Hippo.
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
package org.hippoecm.frontend.plugins.console.menu.save;

import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.hippoecm.frontend.dialog.DialogLink;
import org.hippoecm.frontend.dialog.IDialogFactory;
import org.hippoecm.frontend.dialog.IDialogService;
import org.hippoecm.frontend.session.UserSession;

public class SaveDialogLink extends DialogLink {

    private static final long serialVersionUID = 1L;

    public SaveDialogLink(String id, IModel linktext, final IDialogFactory dialogFactory, final IDialogService dialogService) {
        super(id, linktext, dialogFactory, dialogService);
        Label label = new Label("dialog-link-text-extended", new AbstractReadOnlyModel<String>() {
            private static final long serialVersionUID = 1L;
            @Override
            public String getObject() {
                if (hasSessionChanges()) {
                    return "*";
                }
                return "";
            }
        });
        label.setOutputMarkupId(true);
        link.add(label);
    }
    
    private boolean hasSessionChanges() {
        Session session = ((UserSession) org.apache.wicket.Session.get()).getJcrSession();
        try {
            return session.hasPendingChanges();
        } catch (RepositoryException e) {
            return false;
        }
    }

}