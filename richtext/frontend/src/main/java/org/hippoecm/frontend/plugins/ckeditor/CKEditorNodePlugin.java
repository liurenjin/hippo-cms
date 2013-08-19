package org.hippoecm.frontend.plugins.ckeditor;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;

import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.model.properties.JcrPropertyModel;
import org.hippoecm.frontend.model.properties.JcrPropertyValueModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugin.config.impl.JavaPluginConfig;
import org.hippoecm.frontend.plugins.ckeditor.dialog.images.CKEditorImageService;
import org.hippoecm.frontend.plugins.ckeditor.dialog.images.ImagePickerBehavior;
import org.hippoecm.frontend.plugins.ckeditor.dialog.links.CKEditorLinkService;
import org.hippoecm.frontend.plugins.ckeditor.dialog.links.DocumentPickerBehavior;
import org.hippoecm.frontend.plugins.richtext.IImageURLProvider;
import org.hippoecm.frontend.plugins.richtext.IRichTextImageFactory;
import org.hippoecm.frontend.plugins.richtext.IRichTextLinkFactory;
import org.hippoecm.frontend.plugins.richtext.RichTextImageURLProvider;
import org.hippoecm.frontend.plugins.richtext.RichTextModel;
import org.hippoecm.frontend.plugins.richtext.jcr.JcrRichTextImageFactory;
import org.hippoecm.frontend.plugins.richtext.jcr.JcrRichTextLinkFactory;
import org.hippoecm.frontend.plugins.richtext.model.PrefixingModel;
import org.hippoecm.frontend.plugins.richtext.view.RichTextDiffWithLinksAndImagesPanel;
import org.hippoecm.frontend.plugins.richtext.view.RichTextPreviewWithLinksAndImagesPanel;
import org.hippoecm.frontend.plugins.standards.picker.NodePickerControllerSettings;
import org.hippoecm.frontend.service.IBrowseService;
import org.hippoecm.repository.HippoStdNodeType;
import org.hippoecm.repository.util.JcrUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Node field plugin for editing HTML in the String property {@link HippoStdNodeType.HIPPOSTD_CONTENT}
 * using CKEditor. The plugin enables the 'hippopicker' CKEditor plugin for picking internal links and images.
 * Configuration properties:
 * <ul>
 *     <li>imagepicker: child node with node picker controller settings for the image picker dialog.
 *         Default image picker settings: @{link DEFAULT_IMAGE_PICKER_CONFIG}</li>
 *     <li>documentpicker: child node with node picker controller settings for the document picker dialog
 *         opened by the internal link picker button.
 *         Default document picker settings: @{link DEFAULT_DOCUMENT_PICKER_CONFIG}</li>
 * </ul>
 *
 * @see NodePickerControllerSettings
 */
public class CKEditorNodePlugin extends AbstractCKEditorPlugin {

    public static final String CONFIG_CHILD_IMAGE_PICKER = "imagepicker";
    public static final String CONFIG_CHILD_DOCUMENT_PICKER = "documentpicker";

    public static final IPluginConfig DEFAULT_IMAGE_PICKER_CONFIG = createNodePickerSettings(
            "cms-pickers/images", "ckeditor-imagepicker", "hippostd:gallery");

    public static final IPluginConfig DEFAULT_DOCUMENT_PICKER_CONFIG = createNodePickerSettings(
            "cms-pickers/documents", "ckeditor-documentpicker", "hippostd:folder");

    private static final Logger log = LoggerFactory.getLogger(CKEditorNodePlugin.class);

    public CKEditorNodePlugin(final IPluginContext context, final IPluginConfig config) {
        super(context, config);
    }

    /**
     * @return a panel that shows a preview version of the HTML, including clickable links and visible images.
     */
    @Override
    protected Panel createViewPanel(final String id) {
        return new RichTextPreviewWithLinksAndImagesPanel(id, getNodeModel(), getHtmlModel(), getBrowser());
    }

    /**
     * Creates the {@link CKEditorPanel} with the CKEditor instance to edit HTML.
     * Override this method to add custom server-side {@link CKEditorPanelBehavior}
     * to the returned panel.
     *
     * @param id the Wicket ID of the panel
     * @param editorConfigJson the JSON configuration of the CKEditor instance to create.
     * @return the CKEditorPanel for editing the HTML content.
     */
    @Override
    protected CKEditorPanel createEditPanel(final String id, final String editorConfigJson) {
        final CKEditorPanel editPanel = new CKEditorPanel(id, editorConfigJson, createEditModel());
        addPickerBehavior(editPanel);
        return editPanel;
    }

    private void addPickerBehavior(final CKEditorPanel editPanel) {
        final String editorId = editPanel.getEditorId();
        final DocumentPickerBehavior documentPickerBehavior = createDocumentPickerBehavior(editorId);
        final ImagePickerBehavior imagePickerBehavior = createImagePickerBehavior(editorId);
        final CKEditorPanelPickerBehavior pickerBehavior = new CKEditorPanelPickerBehavior(documentPickerBehavior, imagePickerBehavior);
        editPanel.addBehavior(pickerBehavior);
    }

    private DocumentPickerBehavior createDocumentPickerBehavior(final String editorId) {
        final IPluginConfig documentPickerConfig = getChildPluginConfig(CONFIG_CHILD_DOCUMENT_PICKER, DEFAULT_DOCUMENT_PICKER_CONFIG);

        final IRichTextLinkFactory linkFactory = createLinkFactory();
        CKEditorLinkService linkService = new CKEditorLinkService(linkFactory, editorId);

        return new DocumentPickerBehavior(getPluginContext(), documentPickerConfig, linkService, editorId);
    }

    private ImagePickerBehavior createImagePickerBehavior(final String editorId) {
        final IPluginConfig imagePickerConfig = getChildPluginConfig(CONFIG_CHILD_IMAGE_PICKER, DEFAULT_IMAGE_PICKER_CONFIG);
        final IRichTextImageFactory imageFactory = new JcrRichTextImageFactory(getNodeModel());
        final CKEditorImageService imageService = new CKEditorImageService(imageFactory, editorId);
        return new ImagePickerBehavior(getPluginContext(), imagePickerConfig, imageService, editorId);
    }

    protected IModel<String> createEditModel() {
        final IRichTextLinkFactory linkFactory = createLinkFactory();

        final IModel<String> htmlModel = getHtmlModel();
        final RichTextModel model = new RichTextModel(htmlModel);
        model.setCleaner(getHtmlCleanerOrNull());
        model.setLinkFactory(linkFactory);

        final IRichTextImageFactory imageFactory = createImageFactory();
        final IImageURLProvider urlProvider = createImageUrlProvider(imageFactory, linkFactory);

        return new PrefixingModel(model, urlProvider);
    }

    protected IRichTextLinkFactory createLinkFactory() {
        return new JcrRichTextLinkFactory(getNodeModel());
    }

    protected IRichTextImageFactory createImageFactory() {
        return new JcrRichTextImageFactory(getNodeModel());
    }

    protected IImageURLProvider createImageUrlProvider(final IRichTextImageFactory imageFactory, final IRichTextLinkFactory linkFactory) {
        return new RichTextImageURLProvider(imageFactory, linkFactory);
    }

    @Override
    protected Panel createComparePanel(final String id, final IModel baseModel, final IModel currentModel) {
        final JcrNodeModel baseNodeModel = (JcrNodeModel) baseModel;
        final JcrNodeModel currentNodeModel = (JcrNodeModel) currentModel;
        return new RichTextDiffWithLinksAndImagesPanel(id, baseNodeModel, currentNodeModel, getBrowser());
    }

    private IPluginConfig getChildPluginConfig(final String key, IPluginConfig defaultConfig) {
        IPluginConfig childConfig = getPluginConfig().getPluginConfig(key);
        return childConfig != null ? childConfig : defaultConfig;
    }

    /**
     * @return a model for the the String property {@link HippoStdNodeType.HIPPOSTD_CONTENT} of the model node.
     */
    @Override
    protected IModel<String> getHtmlModel() {
        final JcrNodeModel nodeModel = (JcrNodeModel) getDefaultModel();
        final Node contentNode = nodeModel.getNode();
        try {
            Property contentProperty = contentNode.getProperty(HippoStdNodeType.HIPPOSTD_CONTENT);
            return new JcrPropertyValueModel<String>(new JcrPropertyModel(contentProperty));
        } catch (RepositoryException e) {
            final String nodePath = JcrUtils.getNodePathQuietly(contentNode);
            final String propertyPath = nodePath + "/@" + HippoStdNodeType.HIPPOSTD_CONTENT;
            log.warn("Cannot get value of HTML field property '{}' in plugin {}, using null instead",
                    propertyPath, getPluginConfig().getName());
        }
        return null;
    }

    private IBrowseService getBrowser() {
        final String browserId = getPluginConfig().getString(IBrowseService.BROWSER_ID, "service.browse");
        return getPluginContext().getService(browserId, IBrowseService.class);
    }

    private JcrNodeModel getNodeModel() {
        return (JcrNodeModel) getDefaultModel();
    }

    private static IPluginConfig createNodePickerSettings(final String clusterName, final String lastVisitedKey, final String lastVisitedNodeTypes) {
        JavaPluginConfig config = new JavaPluginConfig();
        config.put("cluster.name", clusterName);
        config.put(NodePickerControllerSettings.LAST_VISITED_KEY, lastVisitedKey);
        config.put(NodePickerControllerSettings.LAST_VISITED_NODETYPES, lastVisitedNodeTypes);
        config.makeImmutable();
        return config;
    }

}
