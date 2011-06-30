package org.onehippo.cms7.reports.layout.portal;


import java.util.List;

import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.wicket.markup.html.CSSPackageResource;
import org.apache.wicket.markup.html.JavascriptPackageResource;
import org.hippoecm.frontend.extjs.ExtHippoThemeReportingBehavior;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.session.UserSession;
import org.json.JSONException;
import org.json.JSONObject;
import org.onehippo.cms7.reports.ExtPlugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wicketstuff.js.ext.ExtComponent;
import org.wicketstuff.js.ext.ExtPanel;
import org.wicketstuff.js.ext.util.ExtClass;


@ExtClass("Hippo.Reports.Portal")
public class PortalPanel extends ExtPanel {

    private static final long serialVersionUID = 1L;

    private static final Logger log = LoggerFactory.getLogger(PortalPanel.class);

    private IPluginContext context;
    private boolean rendered = false;
    private String serviceName;

    public PortalPanel(String id, IPluginContext context, String serviceName) {
        super(id);

        this.context = context;
        this.serviceName = serviceName;

        add(new ExtHippoThemeReportingBehavior());

        add(CSSPackageResource.getHeaderContribution(PortalPanel.class, "Hippo.Reports.Portal.css"));
        add(JavascriptPackageResource.getHeaderContribution(PortalPanel.class, "Hippo.Reports.Portal.js"));
    }

    @Override
    protected void onBeforeRender() {
        try {
            Session session = ((UserSession) getSession()).getJcrSession();
            session.save();
            session.refresh(false);
        } catch (RepositoryException repositoryException) {
            log.error("Error refreshing jcr session.", repositoryException);
        }
        if (!rendered) {
            final List<ExtPlugin> reportPluginServices = context.getServices(this.serviceName, ExtPlugin.class);

            for (ExtPlugin reportExtPlugin : reportPluginServices) {
                final ExtComponent pluginComponent = reportExtPlugin.getExtComponent();
                add(pluginComponent);
            }
            this.rendered = true;
        }
        super.onBeforeRender();
    }

    @Override
    protected void onRenderProperties(JSONObject properties) throws JSONException {
        super.onRenderProperties(properties);
    }

}
