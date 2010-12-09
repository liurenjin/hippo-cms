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
package org.hippoecm.frontend.translation.list;

import java.util.ArrayList;
import java.util.List;

import javax.jcr.Node;

import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.standards.ClassResourceModel;
import org.hippoecm.frontend.plugins.standards.list.AbstractListColumnProviderPlugin;
import org.hippoecm.frontend.plugins.standards.list.ListColumn;
import org.hippoecm.frontend.plugins.standards.list.resolvers.IconAttributeModifier;
import org.hippoecm.frontend.translation.ILocaleProvider;
import org.hippoecm.frontend.translation.list.resolvers.TranslationRenderer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TranslationsColumnProviderPlugin extends AbstractListColumnProviderPlugin {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id: DocumentListingPlugin.java 24575 2010-10-22 10:23:51Z bvanhalderen $";

    private static final long serialVersionUID = 1L;

    static final Logger log = LoggerFactory.getLogger(TranslationsColumnProviderPlugin.class);

    public TranslationsColumnProviderPlugin(IPluginContext context, IPluginConfig config) {
        super(context, config);
    }

    @Override
    public List<ListColumn<Node>> getExpandedColumns() {
        List<ListColumn<Node>> columns = new ArrayList<ListColumn<Node>>(1);
        ListColumn<Node> column;

        //publication date
        column = new ListColumn<Node>(new ClassResourceModel("doclisting-translations", getClass()), "translations");
        column.setRenderer(new TranslationRenderer(getLocaleProvider()));
        column.setAttributeModifier(new IconAttributeModifier());
        column.setCssClass("doclisting-translations");
        columns.add(column);

        return columns;
    }

    protected ILocaleProvider getLocaleProvider() {
        return getPluginContext().getService(
                getPluginConfig().getString(ILocaleProvider.SERVICE_ID, ILocaleProvider.class.getName()),
                ILocaleProvider.class);
    }

}
