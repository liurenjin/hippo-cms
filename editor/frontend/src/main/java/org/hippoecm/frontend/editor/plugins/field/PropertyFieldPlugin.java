/*
 *  Copyright 2008-2014 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.frontend.editor.plugins.field;

import java.util.Iterator;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.ValueFormatException;

import org.apache.wicket.Component;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.hippoecm.frontend.editor.plugins.fieldhint.FieldHint;
import org.hippoecm.frontend.model.AbstractProvider;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.model.PropertyValueProvider;
import org.hippoecm.frontend.model.event.IEvent;
import org.hippoecm.frontend.model.event.IObserver;
import org.hippoecm.frontend.model.properties.JcrPropertyModel;
import org.hippoecm.frontend.model.properties.JcrPropertyValueModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.service.IRenderService;
import org.hippoecm.frontend.types.IFieldDescriptor;
import org.hippoecm.frontend.types.ITypeDescriptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PropertyFieldPlugin extends AbstractFieldPlugin<Property, JcrPropertyValueModel> {

    private static final long serialVersionUID = 1L;

    static final Logger log = LoggerFactory.getLogger(PropertyFieldPlugin.class);

    private JcrNodeModel nodeModel;
    private JcrPropertyModel propertyModel;
    private int nrValues;
    private int[] order;
    
    private IObserver propertyObserver;

    public PropertyFieldPlugin(IPluginContext context, IPluginConfig config) {
        super(context, config);

        IFieldDescriptor field = getFieldHelper().getField();

        nodeModel = (JcrNodeModel) getDefaultModel();

        // use caption for backwards compatibility; i18n should use field name
        add(new Label("name", getCaptionModel()));

        add(createNrItemsLabel());

        Label required = new Label("required", "*");
        if (field != null) {
            subscribe(field);
            if (!field.getValidators().contains("required")) {
                required.setVisible(false);
            }
        }
        add(required);

        add(new FieldHint("hint-panel", config.getString("hint")));
        add(createAddLink());
    }

    private JcrPropertyModel newPropertyModel(JcrNodeModel model) {
        IFieldDescriptor field = getFieldHelper().getField();
        if (field != null) {
            String fieldAbsPath = model.getItemModel().getPath() + "/" + field.getPath();
            return new JcrPropertyModel(fieldAbsPath);
        } else {
            return new JcrPropertyModel((Property) null);
        }
    }

    protected void subscribe(final IFieldDescriptor field) {
        if (!field.getPath().equals("*")) {
            propertyModel = newPropertyModel((JcrNodeModel) getDefaultModel());

            order = getOrder();
            nrValues = propertyModel.size();

            getPluginContext().registerService(propertyObserver = new IObserver<JcrPropertyModel>() {
                private static final long serialVersionUID = 1L;

                public JcrPropertyModel getObservable() {
                    return propertyModel;
                }

                public void onEvent(Iterator<? extends IEvent<JcrPropertyModel>> events) {

                    int[] newOrder = getOrder();
                    int newNrValues = propertyModel.size();

                    //Only redraw if the number of properties or their order has changed.
                    if (newNrValues != nrValues || orderHasChanged(newOrder)) {
                        nrValues = newNrValues;
                        resetValidation();
                        redraw();
                    }

                    order = newOrder;
                }

            }, IObserver.class.getName());
        }
    }

    protected void unsubscribe(IFieldDescriptor field) {
        if (!field.getPath().equals("*")) {
            getPluginContext().unregisterService(propertyObserver, IObserver.class.getName());
            propertyModel = null;
        }
    }

    @Override
    protected AbstractProvider<Property, JcrPropertyValueModel> newProvider(IFieldDescriptor descriptor, ITypeDescriptor type,
            IModel<Node> nodeModel) {
        if (!descriptor.getPath().equals("*")) {
            return new PropertyValueProvider(descriptor, type, newPropertyModel((JcrNodeModel) nodeModel).getItemModel());
        }
        return null;
    }

    @Override
    public void onModelChanged() {
        // filter out changes in the node model itself.
        // The property model observation takes care of that.
        if (!nodeModel.equals(getDefaultModel()) || (propertyModel != null && propertyModel.size() != nrValues)) {
            IFieldDescriptor field = getFieldHelper().getField();
            if (field != null) {
                unsubscribe(field);
                subscribe(field);
            }
            redraw();
        }
    }

    @Override
    protected void onBeforeRender() {
        replace(createAddLink());
        super.onBeforeRender();
    }

    @Override
    protected void onDetach() {
        if (propertyModel != null) {
            propertyModel.detach();
        }
        super.onDetach();
    }

    @Override
    protected void populateViewItem(Item<IRenderService> item) {
        Fragment fragment = new TransparentFragment("fragment", "view-fragment", this);
        item.add(fragment);
    }

    @Override
    protected void populateEditItem(Item item, final JcrPropertyValueModel model) {
        Fragment fragment = new TransparentFragment("fragment", "edit-fragment", this);

        WebMarkupContainer controls = new WebMarkupContainer("controls");
        controls.setVisible(canRemoveItem() || canReorderItems());
        fragment.add(controls);

        MarkupContainer remove = new AjaxLink("remove") {
            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                onRemoveItem(model, target);
            }
        };
        if (!canRemoveItem()) {
            remove.setVisible(false);
        }
        controls.add(remove);

        MarkupContainer upLink = new AjaxLink("up") {
            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                onMoveItemUp(model, target);
            }
        };
        boolean isFirst = (model.getIndex() == 0);
        if (!canReorderItems()) {
            upLink.setVisible(false);
        }
        upLink.setEnabled(!isFirst);
        controls.add(upLink);

        MarkupContainer downLink = new AjaxLink("down") {
            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                JcrPropertyValueModel nextModel = new JcrPropertyValueModel(model.getIndex() + 1, model
                        .getJcrPropertymodel());
                onMoveItemUp(nextModel, target);
            }
        };
        boolean isLast = (model.getIndex() == provider.size() - 1);
        if (!canReorderItems()) {
            downLink.setVisible(false);
        }
        downLink.setEnabled(!isLast);
        controls.add(downLink);

        item.add(fragment);
    }

    @Override
    protected void populateCompareItem(Item<IRenderService> item) {
        Fragment fragment = new TransparentFragment("fragment", "view-fragment", this);
        item.add(fragment);
    }

    protected Component createAddLink() {
        if (canAddItem()) {
            return new AjaxLink("add") {
                private static final long serialVersionUID = 1L;

                @Override
                public void onClick(AjaxRequestTarget target) {
                    target.focusComponent(this);
                    PropertyFieldPlugin.this.onAddItem(target);
                }
            };
        } else {
            return new Label("add").setVisible(false);
        }
    }

    // If property is multiple return an array containing the hash codes of all values, otherwise return null.
    private int[] getOrder() {
        Property prop = propertyModel.getProperty();

        if (prop != null) {
            try {
                if (prop.isMultiple()) {
                    Value[] values = prop.getValues();
                    int[] hashCodes = new int[values.length];
                    for (int i = 0; i < values.length; i++) {
                        try {
                            hashCodes[i] = values[i].getString().hashCode();
                        } catch (ValueFormatException e) {
                            log.warn("Failed to retrieve hashcode of value with index {} for property {}. " +
                                    "Order comparison is not possible.", i, propertyModel.getItemModel().getPath());
                            hashCodes[i] = 0;
                        }
                    }
                    return hashCodes;
                }
            } catch (RepositoryException e) {
                log.warn("Failed to retrieve hashcodes of property "
                        + propertyModel.getItemModel().getPath() + " for order comparison.", e);
            }
        }
        return null;
    }

    // If two or more values at the same index are not equal, order has changed. We assume that both arrays (if not null)
    // are of the same length. If both are null, order has not changed. If only one is null, order has changed.
    private boolean orderHasChanged(int[] newOrder) {
        if (order == null && newOrder == null) {
            return false;
        }

        if (order == null || newOrder == null) {
            return true;
        }

        int changed = 0;
        for (int i = 0; i < order.length && changed < 2; i++) {
            if (order[i] != newOrder[i]) {
                changed++;
            }
        }
        return changed > 1;
    }
}
