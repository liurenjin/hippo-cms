/**
  * Copyright 2011 Hippo
  *
  * Licensed under the Apache License, Version 2.0 (the  "License");
  * you may not use this file except in compliance with the License.
  * You may obtain a copy of the License at
  *
  * http://www.apache.org/licenses/LICENSE-2.0
  *
  * Unless required by applicable law or agreed to in writing, software
  * distributed under the License is distributed on an "AS IS" BASIS,
  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  * See the License for the specific language governing permissions and
  * limitations under the License.
*/
package org.hippoecm.frontend.plugins.standards.panelperspective;


import org.apache.wicket.ResourceReference;
import org.apache.wicket.extensions.breadcrumb.panel.IBreadCrumbPanelFactory;
import org.apache.wicket.model.IModel;
import org.hippoecm.frontend.plugin.IPlugin;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.standards.panelperspective.breadcrumb.AjaxBreadCrumbPanelFactory;
import org.hippoecm.frontend.plugins.standards.panelperspective.breadcrumb.PanelPluginBreadCrumbPanel;

public abstract class PanelPlugin extends AjaxBreadCrumbPanelFactory implements IPlugin, IBreadCrumbPanelFactory {

    private final IPluginContext context;

    public PanelPlugin(final IPluginContext context, final IPluginConfig config, Class<? extends PanelPluginBreadCrumbPanel> panelClass) {
        super(context, config, panelClass);
        this.context = context;
    }

    public abstract ResourceReference getImage();

    public abstract IModel<String> getTitle();

    public abstract IModel<String> getHelp();

    @Override
    public void start() {
        context.registerService(this, getPanelServiceId());
    }

    @Override
    public void stop() {
    }

    public abstract String getPanelServiceId();

}
