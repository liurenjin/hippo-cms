/*
 *  Copyright 2017 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.frontend.plugins.richtext.processor;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.session.UserSession;
import org.onehippo.cms7.services.processor.html.model.Model;
import org.onehippo.cms7.services.processor.richtext.jcr.JcrNodeFactory;

public class WicketNodeFactory extends JcrNodeFactory {

    public static final JcrNodeFactory INSTANCE = new WicketNodeFactory();

    private WicketNodeFactory() {}

    @Override
    protected Session getSession() {
        return UserSession.get().getJcrSession();
    }

    @Override
    public Model<Node> getNodeModelByIdentifier(final String uuid) throws RepositoryException {
        return new WicketModel<>(new JcrNodeModel(getNodeByIdentifier(uuid)));
    }
}
