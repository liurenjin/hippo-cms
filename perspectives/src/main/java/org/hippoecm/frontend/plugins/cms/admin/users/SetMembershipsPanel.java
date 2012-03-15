/*
 *  Copyright 2008-2012 Hippo.
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
package org.hippoecm.frontend.plugins.cms.admin.users;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.jcr.RepositoryException;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.extensions.breadcrumb.IBreadCrumbModel;
import org.apache.wicket.extensions.breadcrumb.IBreadCrumbParticipant;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.ChoiceRenderer;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.model.StringResourceModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugins.cms.admin.AdminBreadCrumbPanel;
import org.hippoecm.frontend.plugins.cms.admin.AuditLogger;
import org.hippoecm.frontend.plugins.cms.admin.HippoAdminConstants;
import org.hippoecm.frontend.plugins.cms.admin.domains.Domain;
import org.hippoecm.frontend.plugins.cms.admin.domains.DomainDataProvider;
import org.hippoecm.frontend.plugins.cms.admin.groups.DetachableGroup;
import org.hippoecm.frontend.plugins.cms.admin.groups.Group;
import org.hippoecm.frontend.plugins.cms.admin.groups.ViewGroupActionLink;
import org.hippoecm.frontend.plugins.cms.admin.permissions.DomainLinkListPanel;
import org.hippoecm.frontend.plugins.cms.admin.permissions.PermissionBean;
import org.hippoecm.frontend.plugins.cms.admin.widgets.AjaxLinkLabel;
import org.hippoecm.frontend.session.UserSession;
import org.onehippo.cms7.event.HippoEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SetMembershipsPanel extends Panel {
    @SuppressWarnings("unused")
    private static final String SVN_ID = "$Id$";
    private static final long serialVersionUID = 1L;
    private static final Logger log = LoggerFactory.getLogger(SetMembershipsPanel.class);
    private Group selectedGroup;
    private final IModel userModel;
    private final ListView localList;
    private final IPluginContext context;

    public SetMembershipsPanel(final String id, final IPluginContext context,
                               final IBreadCrumbModel breadCrumbModel, final IModel<User> userModel) {
        super(id);

        setOutputMarkupId(true);

        selectedGroup = null;
        this.userModel = userModel;
        this.context = context;
        final User user = userModel.getObject();

        // All local groups
        Form form = new Form("form");

        AjaxButton submit = new AjaxButton("submit", form) {
            private static final long serialVersionUID = 1L;

            @Override
            protected void onSubmit(final AjaxRequestTarget target, final Form form) {
                try {
                    if (selectedGroup.getMembers().contains(user.getUsername())) {
                        info(getString("user-membership-already-member", new DetachableGroup(selectedGroup)));
                    } else {
                        selectedGroup.addMembership(user.getUsername());
                        final UserSession userSession = UserSession.get();
                        HippoEvent event = new HippoEvent(userSession.getApplicationName())
                                .user(userSession.getJcrSession().getUserID())
                                .action("add-user-to-group")
                                .category(HippoAdminConstants.CATEGORY_GROUP_MANAGEMENT)
                                .message("added user " + user.getUsername() + " to group " + selectedGroup.getGroupname());
                        AuditLogger.getLogger().info(event.toString());
                        info(getString("user-membership-added", new DetachableGroup(selectedGroup)));
                        localList.removeAll();
                    }
                } catch (RepositoryException e) {
                    error(getString("user-membership-add-failed", new DetachableGroup(selectedGroup)));
                    log.error("Failed to add memberships", e);
                }
                target.addComponent(SetMembershipsPanel.this);
            }

        };
        form.add(submit);

        List<Group> localGroups = Group.getLocalGroups();
        DropDownChoice<Group> ddc = new DropDownChoice<Group>("local-groups", new PropertyModel<Group>(this, "selectedGroup"),
                localGroups, new ChoiceRenderer<Group>("groupname"));
        ddc.setNullValid(false);
        ddc.setRequired(true);

        form.add(ddc);
        add(form);

        // local memberships
        Label localLabel = new Label("local-memberships-label", new ResourceModel("user-local-memberships"));
        localList = new MembershipsListEditView("local-memberships", user);
        form.add(localLabel);
        form.add(localList);

        // external memberships
        Label externalLabel = new Label("external-memberships-label", new ResourceModel("user-external-memberships"));
        ListView externalList = new MembershipsListView("external-memberships", "external-membership", user);
        externalLabel.setVisible((user.getExternalMemberships().size() > 0));
        externalList.setVisible((user.getExternalMemberships().size() > 0));
        form.add(externalLabel);
        form.add(externalList);


        // add a cancel/back button
        form.add(new AjaxButton("back-button") {
            private static final long serialVersionUID = 1L;

            @Override
            protected void onSubmit(final AjaxRequestTarget target, final Form form) {
                // one up
                List<IBreadCrumbParticipant> l = breadCrumbModel.allBreadCrumbParticipants();
                breadCrumbModel.setActive(l.get(l.size() - 2));
            }
        }.setDefaultFormProcessing(false));
    }

    /**
     * list view to be nested in the form.
     */
    private final class MembershipsListEditView extends ListView<Group> {

        private static final long serialVersionUID = 1L;

        private User user;

        public MembershipsListEditView(final String id, final User user) {
            super(id);
            ArrayList<Group> groups = new ArrayList<Group>(user.getLocalMembershipsAsListOfGroups());
            setModel(new Model<ArrayList<Group>>(groups));
            this.user = user;
            setReuseItems(false);
            setOutputMarkupId(true);
            DomainDataProvider.setDirty();
        }

        @Override
        protected void populateItem(final ListItem<Group> item) {
            item.setOutputMarkupId(true);
            final Group group = item.getModelObject();

            AdminBreadCrumbPanel viewUserPanel = this.findParent(ViewUserPanel.class);

            ViewGroupActionLink groupLink = new ViewGroupActionLink(
                    "link", new PropertyModel<String>(group, "groupname"),
                    group, context, viewUserPanel
            );

            item.add(groupLink);


            List<PermissionBean> groupPermissions = group.getPermissions();
            Map<Domain, List<String>> domainsWithRoles = new HashMap<Domain, List<String>>();
            for (PermissionBean permission : groupPermissions) {
                Domain domain = permission.getDomain().getObject();
                List<String> roles = domainsWithRoles.get(domain);
                if (roles == null) {
                    roles = new ArrayList<String>();
                }
                roles.add(permission.getAuthRole().getRole());
                domainsWithRoles.put(domain, roles);
            }

            DomainLinkListPanel domainLinkList = new DomainLinkListPanel(
                    "securityDomains", domainsWithRoles,
                    findParent(ViewUserPanel.class)
            );

            item.add(domainLinkList);
            item.add(new AjaxLinkLabel("remove", new ResourceModel("user-membership-remove-action")) {
                private static final long serialVersionUID = 1L;

                @Override
                public void onClick(final AjaxRequestTarget target) {
                    try {
                        group.removeMembership(user.getUsername());
                        final UserSession userSession = UserSession.get();
                        HippoEvent event = new HippoEvent(userSession.getApplicationName())
                                .user(userSession.getJcrSession().getUserID())
                                .action("remove-user-from-group")
                                .category(HippoAdminConstants.CATEGORY_GROUP_MANAGEMENT)
                                .message(
                                        "removed user " + user.getUsername()
                                                + " from group " + group.getGroupname());
                        AuditLogger.getLogger().info(event.toString());
                        info(getString("user-membership-removed", new Model<Group>(group)));
                        localList.removeAll();
                    } catch (RepositoryException e) {
                        error(getString("user-membership-remove-failed", new Model<Group>(group)));
                        log.error("Failed to remove memberships", e);
                    }
                    target.addComponent(SetMembershipsPanel.this);
                }
            });
        }
    }

    /**
     * list view to be nested in the form.
     */
    private final class MembershipsListView extends ListView<DetachableGroup> {
        private static final long serialVersionUID = 1L;
        private String labelId;

        public MembershipsListView(final String id, final String labelId, final User user) {
            super(id, new PropertyModel<List<DetachableGroup>>(user, "externalMemberships"));
            this.labelId = labelId;
            setReuseItems(true);
        }

        protected void populateItem(final ListItem item) {
            final DetachableGroup dg = (DetachableGroup) item.getModelObject();
            item.add(new Label(labelId, dg.getGroup().getGroupname()));
        }

    }

    /**
     * Return the resource id processed into a model.
     *
     * @param component The comonent.
     * @return The model containing the title extracted from the resource.
     */
    public IModel<String> getTitle(final Component component) {
        return new StringResourceModel("user-set-memberships-title", component, userModel);
    }

}
