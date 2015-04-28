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
package org.hippoecm.frontend.plugins.gallery.editor;

import java.io.IOException;
import java.util.Calendar;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.upload.FileUpload;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.util.lang.Bytes;
import org.hippoecm.frontend.behaviors.EventStoppingBehavior;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.gallery.imageutil.ImageBinary;
import org.hippoecm.frontend.plugins.gallery.model.DefaultGalleryProcessor;
import org.hippoecm.frontend.plugins.gallery.model.GalleryException;
import org.hippoecm.frontend.plugins.gallery.model.GalleryProcessor;
import org.hippoecm.frontend.plugins.jquery.upload.FileUploadViolationException;
import org.hippoecm.frontend.plugins.jquery.upload.SingleFileUploadWidget;
import org.hippoecm.frontend.plugins.yui.upload.validation.FileUploadValidationService;
import org.hippoecm.frontend.service.render.RenderPlugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Plugin for uploading images. The plugin can be configured by setting configuration options found in the {@link
 * org.hippoecm.frontend.plugins.jquery.upload.FileUploadWidgetSettings}.
 */
public class ImageUploadPlugin extends RenderPlugin {

    private static final long serialVersionUID = 1L;

    private static final Logger log = LoggerFactory.getLogger(ImageUploadPlugin.class);
    private FileUploadForm form;
    private final String mode;
    public ImageUploadPlugin(final IPluginContext context, IPluginConfig config) {
        super(context, config);
        mode = config.getString("mode", "edit");
        addNewUploadForm();
        add(new EventStoppingBehavior("onclick"));
        setOutputMarkupId(true);
    }

    /**
     * Reset form so we can upload same image again
     * after "restore" function is invoked.
     *
     */
    private void resetForm() {
        final AjaxRequestTarget ajaxRequestTarget = RequestCycle.get().find(AjaxRequestTarget.class);
        if (ajaxRequestTarget != null && form != null) {
            remove(form);
            addNewUploadForm();
            ajaxRequestTarget.add(this);
        }
    }

    private void addNewUploadForm() {
        form = new FileUploadForm("form", this);
        form.setVisible("edit".equals(mode));
        form.setOutputMarkupId(true);
        add(form);
    }

    private class FileUploadForm extends Form {
        private static final long serialVersionUID = 1L;

        private SingleFileUploadWidget widget;

        public FileUploadForm(String name, final ImageUploadPlugin parent) {
            super(name);
            setMultiPart(true);

            String serviceId = getPluginConfig().getString(FileUploadValidationService.VALIDATE_ID, "service.gallery.image.validation");
            FileUploadValidationService validator = getPluginContext().getService(serviceId, FileUploadValidationService.class);

            add(widget = new SingleFileUploadWidget("fileUploadPanel", getPluginConfig() ,validator) {
                private static final long serialVersionUID = 1L;
                @Override
                protected void onFileUpload(FileUpload fileUpload) throws FileUploadViolationException {
                    handleUpload(fileUpload);
                    parent.resetForm();
                }
            });

            setMaxSize(Bytes.bytes(widget.getSettings().getMaxFileSize()));
        }

        @Override
        protected void onSubmit() {
            try {
                widget.onSubmit();
            } catch (FileUploadViolationException e) {
                e.getViolationMessages().forEach(this::error);
            }
        }

    }

    private void handleUpload(FileUpload upload) throws FileUploadViolationException {
        String fileName = upload.getClientFileName();
        String mimeType = upload.getContentType();

        String serviceId = getPluginConfig().getString("gallery.processor.id", "gallery.processor.service");
        GalleryProcessor processor = getPluginContext().getService(serviceId, GalleryProcessor.class);
        if (processor == null) {
            processor = new DefaultGalleryProcessor();
        }

        JcrNodeModel nodeModel = (JcrNodeModel) getDefaultModel();
        Node node = nodeModel.getNode();

        try {
            ImageBinary image = new ImageBinary(node, upload.getInputStream(), fileName, mimeType);
            processor.initGalleryResource(node, image.getStream(), image.getMimeType(), image.getFileName(), Calendar.getInstance());
        } catch (IOException | GalleryException | RepositoryException e) {
            if (log.isDebugEnabled()) {
                log.error("Cannot upload image ", e);
            } else {
                log.error("Cannot upload image: {}", e.getMessage());
            }
            throw new FileUploadViolationException(e.getMessage());

        }
    }
}
