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
package org.hippoecm.frontend.plugins.yui.widget;

import net.sf.json.JsonConfig;
import org.apache.wicket.Component;
import org.apache.wicket.util.template.PackagedTextTemplate;
import org.hippoecm.frontend.plugins.yui.AbstractYuiBehavior;
import org.hippoecm.frontend.plugins.yui.HippoNamespace;
import org.hippoecm.frontend.plugins.yui.JsFunction;
import org.hippoecm.frontend.plugins.yui.JsFunctionProcessor;
import org.hippoecm.frontend.plugins.yui.header.IYuiContext;
import org.hippoecm.frontend.plugins.yui.header.templates.DynamicTextTemplate;

public class WidgetBehavior extends AbstractYuiBehavior{
    private static final long serialVersionUID = 1L;

    @SuppressWarnings("unused")
    private static final String SVN_ID = "$Id$";

    Component component;
    WidgetTemplate template;

    public WidgetBehavior() {
        this(new WidgetSettings());
    }

    public WidgetBehavior(WidgetSettings settings) {
        template = new WidgetTemplate() {

            @Override
            public String getId() {
                return getComponent().getMarkupId();
            }

            @Override
            protected void decorateJsonConfig(JsonConfig jsonConfig) {
                jsonConfig.registerJsonValueProcessor(JsFunction.class, new JsFunctionProcessor());
            }
        };
        template.setConfiguration(settings);
    }

    @Override
    public void bind(Component component) {
        super.bind(component);
        this.component = component;
    }


    @Override
    public void addHeaderContribution(IYuiContext context) {
        context.addModule(HippoNamespace.NS, "hippowidget");
        context.addTemplate(template);
        context.addOnWinLoad("YAHOO.hippo.WidgetManager.render();");
    }

    public String getMarkupId() {
        return component.getMarkupId();
    }

    protected WidgetTemplate getTemplate() {
        return template;
    }

    public String getUpdateScript() {
        return "YAHOO.hippo.WidgetManager.update('" + getMarkupId() + "');";
    }

}
