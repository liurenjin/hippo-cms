/*
 *  Copyright 2008-2015 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.frontend.plugins.console.editor;

import java.math.BigDecimal;
import java.util.Calendar;

import javax.jcr.Property;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.ValueFactory;
import javax.jcr.nodetype.PropertyDefinition;

import org.apache.commons.lang.StringUtils;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.ReuseIfModelsEqualStrategy;
import org.apache.wicket.markup.repeater.data.DataView;
import org.apache.wicket.markup.repeater.data.IDataProvider;
import org.hippoecm.frontend.model.properties.JcrPropertyModel;
import org.hippoecm.frontend.plugins.standards.list.resolvers.TitleAttribute;
import org.hippoecm.frontend.session.UserSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PropertiesEditor extends DataView<Property> {

    private static final long serialVersionUID = 1L;

    static final Logger log = LoggerFactory.getLogger(PropertiesEditor.class);

    private String namespacePrefix;

    public PropertiesEditor(String id, IDataProvider<Property> model) {
        super(id, model);

        setItemReuseStrategy(ReuseIfModelsEqualStrategy.getInstance());

        String namespace = ((NodeEditor.NamespacePropertiesProvider) model).getNamespace();

        if (namespace.equals(NodeEditor.NONE_LABEL)) {
            namespacePrefix = StringUtils.EMPTY;
        } else {
            namespacePrefix = namespace + ":";
        }
    }

    @Override
    protected void populateItem(Item item) {
        JcrPropertyModel model = (JcrPropertyModel) item.getModel();

        try {
            final AjaxLink deleteLink = deleteLink("delete", model);
            item.add(deleteLink);
            deleteLink.setVisible(!model.getProperty().getDefinition().isProtected());

            JcrName propName = new JcrName(model.getProperty().getName());
            item.add(new Label("namespace", namespacePrefix));
            item.add(new Label("name", propName.getName()));

            item.add(new Label("type", PropertyType.nameFromValue(model.getProperty().getType())));

            WebMarkupContainer valuesContainer = new WebMarkupContainer("values-container");
            valuesContainer.setOutputMarkupId(true);
            item.add(valuesContainer);

            PropertyValueEditor editor = createPropertyValueEditor("values", model);
            valuesContainer.add(editor);

            final AjaxLink addLink = addLink("add", model, valuesContainer, editor);
            addLink.add(TitleAttribute.set(getString("property.value.add")));
            item.add(addLink);

            PropertyDefinition definition = model.getProperty().getDefinition();
            addLink.setVisible(definition.isMultiple() && !definition.isProtected());
        } catch (RepositoryException e) {
            log.error(e.getMessage());
        }
    }

    /**
     * Creates {@link PropertyValueEditor} for a property. If name equals 'hst:script' and parent node is of type
     * 'hst:template' a FreemarkerCodeMirrorPropertyValueEditor is returned instead.
     *
     * @throws RepositoryException
     */
    protected PropertyValueEditor createPropertyValueEditor(final String id, final JcrPropertyModel model) throws RepositoryException {

        // NOTES:
        // - Creates custom propertyValueEditor for freemarker template source.
        // - For now, it's hard-coded only for Freemarker templates, but maybe someday we can improve it to read
        //   from plugin configurations if we want to support more custom propertyEditors (e.g, css, js, etc.).

        Property prop = model.getProperty();

        if ("hst:script".equals(prop.getName()) && prop.getParent().isNodeType("hst:template")) {
            return new FreemarkerCodeMirrorPropertyValueEditor(id, model);
        }

        return new PropertyValueEditor(id, model);
    }

    // privates
    private AjaxLink deleteLink(String id, final JcrPropertyModel model) throws RepositoryException {
        AjaxLink deleteLink = new AjaxLink<Property>(id, model) {
            private static final long serialVersionUID = 1L;

            @Override
            protected void onComponentTag(final ComponentTag tag) {
                super.onComponentTag(tag);
                tag.put("class", "property-remove");
            }

            @Override
            public void onClick(AjaxRequestTarget target) {
                try {
                    Property prop = model.getProperty();
                    prop.remove();
                } catch (RepositoryException e) {
                    log.error(e.getMessage());
                }

                NodeEditor editor = findParent(NodeEditor.class);
                target.add(editor);
            }
        };

        deleteLink.add(TitleAttribute.set(getString("property.remove")));

        return deleteLink;
    }

    private AjaxLink addLink(String id, final JcrPropertyModel model, final WebMarkupContainer component,
                             final PropertyValueEditor editor) {

        return new AjaxLink<Property>(id, model) {
            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                Property prop = model.getProperty();
                Value[] newValues;
                try {
                    Value[] oldValues = prop.getValues();
                    newValues = new Value[oldValues.length + 1];
                    System.arraycopy(oldValues, 0, newValues, 0, oldValues.length);
                    newValues[newValues.length - 1] = createDefaultValue(prop.getType());
                    prop.setValue(newValues);

                    editor.setFocusOnLastItem(true);
                } catch (RepositoryException e) {
                    log.error(e.getMessage());
                    return;
                }
                target.add(component);
            }
        };
    }

    private Value createDefaultValue(int valueType) throws RepositoryException {
        final ValueFactory valueFactory = UserSession.get().getJcrSession().getValueFactory();
        switch (valueType) {
            case PropertyType.BOOLEAN : return valueFactory.createValue(false);
            case PropertyType.DATE : return valueFactory.createValue(Calendar.getInstance());
            case PropertyType.DECIMAL : return valueFactory.createValue(new BigDecimal(0d));
            case PropertyType.DOUBLE : return valueFactory.createValue(0d);
            case PropertyType.LONG : return valueFactory.createValue(0l);
            case PropertyType.NAME : return valueFactory.createValue("jcr:name", PropertyType.NAME);
            case PropertyType.PATH : return valueFactory.createValue("/", PropertyType.PATH);
            case PropertyType.URI : return valueFactory.createValue("http://www.onehippo.org/", PropertyType.URI);
            case PropertyType.REFERENCE : return valueFactory.createValue("cafebabe-cafe-babe-cafe-babecafebabe", PropertyType.REFERENCE);
            case PropertyType.WEAKREFERENCE : return valueFactory.createValue("cafebabe-cafe-babe-cafe-babecafebabe", PropertyType.REFERENCE);
            default : return valueFactory.createValue("");
        }
    }

}
