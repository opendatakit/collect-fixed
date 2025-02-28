/*
 * Copyright 2018 Nafundi
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
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

package org.odk.collect.android.widgets.items;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.LinearLayout;

import org.javarosa.core.model.SelectChoice;
import org.javarosa.core.model.data.helper.Selection;
import org.javarosa.core.reference.InvalidReferenceException;
import org.javarosa.form.api.FormEntryPrompt;
import org.odk.collect.android.databinding.SelectImageMapWidgetAnswerBinding;
import org.odk.collect.android.formentry.questions.QuestionDetails;
import org.odk.collect.android.utilities.HtmlUtils;
import org.odk.collect.android.widgets.QuestionWidget;
import org.odk.collect.android.widgets.interfaces.SelectChoiceLoader;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.File;
import java.io.FileInputStream;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import timber.log.Timber;

/**
 * A base widget class which is responsible for sharing the code used by image map select widgets like
 * {@link SelectOneImageMapWidget} and {@link SelectMultiImageMapWidget}.
 */
public abstract class SelectImageMapWidget extends QuestionWidget {
    private static final String WEB_VIEW_CONTENT =
            "<!DOCTYPE html> <html>\n" +
                    "    <body>\n" +
                    "           %s" + //inject an svg map here
                    "        <script src=\"file:///android_asset/svg_map_helper.js\"></script>\n" +
                    "    </body>\n" +
                    "</html>";
    private final boolean isSingleSelect;
    protected List<Selection> selections = new ArrayList<>();
    private String imageMapFilePath;
    SelectImageMapWidgetAnswerBinding binding;

    final List<SelectChoice> items;

    public SelectImageMapWidget(Context context, QuestionDetails prompt, SelectChoiceLoader selectChoiceLoader, Dependencies dependencies) {
        super(context, dependencies, prompt);
        render();

        items = ItemsWidgetUtils.loadItemsAndHandleErrors(this, questionDetails.getPrompt(), selectChoiceLoader);

        isSingleSelect = this instanceof SelectOneImageMapWidget;

        try {
            imageMapFilePath = getReferenceManager().deriveReference(prompt.getPrompt().getImageText()).getLocalURI();
        } catch (InvalidReferenceException e) {
            Timber.w(e);
        }
        setUpWebView();
    }

    private static String convertDocumentToString(Document doc) {
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer;
        try {
            transformer = transformerFactory.newTransformer();
            StringWriter writer = new StringWriter();
            transformer.transform(new DOMSource(doc), new StreamResult(writer));

            return writer.getBuffer().toString();
        } catch (TransformerException e) {
            Timber.w(e);
        }

        return null;
    }

    @Override
    public void clearAnswer() {
        selections.clear();
        binding.imageMap.loadUrl("javascript:clearAreas()");
        binding.selectedElements.setVisibility(GONE);
        widgetValueChanged();
    }

    @Override
    public boolean shouldSuppressFlingGesture() {
        return binding.imageMap.suppressFlingGesture();
    }

    @Override
    protected View onCreateAnswerView(Context context, FormEntryPrompt prompt, int answerFontSize) {
        binding = SelectImageMapWidgetAnswerBinding.inflate(((Activity) context).getLayoutInflater());
        binding.selectedElements.setTextSize(TypedValue.COMPLEX_UNIT_DIP, answerFontSize);
        binding.selectedElements.setVisibility(binding.selectedElements.getText().toString().isBlank() ? GONE : VISIBLE);
        return binding.getRoot();
    }

    @SuppressLint({"SetJavaScriptEnabled", "AddJavascriptInterface"})
    private void setUpWebView() {
        String svgMap = null;
        if (imageMapFilePath != null && !imageMapFilePath.isEmpty()) {
            svgMap = getParsedSVGFile();
        }
        if (svgMap != null) {
            binding.imageMap.getSettings().setJavaScriptEnabled(true);
            binding.imageMap.getSettings().setBuiltInZoomControls(true);
            binding.imageMap.getSettings().setDisplayZoomControls(false);
            binding.imageMap.addJavascriptInterface(new JavaScriptInterface(), "imageMapInterface");
            binding.imageMap.loadDataWithBaseURL(null, String.format(WEB_VIEW_CONTENT, svgMap), "text/html", "UTF-8", null);
            binding.imageMap.setInitialScale(1);
            binding.imageMap.getSettings().setUseWideViewPort(true);
            int height = (int) (getResources().getDisplayMetrics().heightPixels / 1.7); // about 60% of a screen
            binding.imageMap.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, height));
            binding.imageMap.setClickable(!getFormEntryPrompt().isReadOnly());
            binding.imageMap.setWebViewClient(new WebViewClient() {
                @Override
                public void onPageFinished(WebView view, String url) {
                    view.loadUrl("javascript:setSelectMode(" + isSingleSelect + ")");
                    for (SelectChoice selectChoice : items) {
                        view.loadUrl("javascript:addArea('" + selectChoice.getValue() + "')");
                    }
                    highlightSelections(view);
                }
            });
        }
    }

    protected void selectArea(String areaId) {
        SelectChoice selectChoice = null;
        for (SelectChoice sc : items) {
            if (areaId.equalsIgnoreCase(sc.getValue())) {
                selectChoice = sc;
            }
        }
        if (selectChoice != null) {
            selections.add(new Selection(selectChoice));
        }
        ((Activity) getContext()).runOnUiThread(this::widgetValueChanged);
    }

    private void unselectArea(String areaId) {
        Selection selectionToRemove = null;

        for (Selection selection : selections) {
            if (areaId.equalsIgnoreCase(selection.getValue())) {
                selectionToRemove = selection;
            }
        }

        selections.remove(selectionToRemove);
        ((Activity) getContext()).runOnUiThread(this::widgetValueChanged);
    }

    private void notifyChanges() {
        refreshSelectedItemsLabel();
    }

    private String getParsedSVGFile() {
        File initialFile = new File(imageMapFilePath);

        try (FileInputStream inputStream = new FileInputStream(initialFile)) {
            DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
            Document document = documentBuilder.parse(inputStream);

            Element element = document.getDocumentElement();
            element.normalize();

            // Add default svg size if not specified
            NodeList nodeList = document.getElementsByTagName("svg");
            addSizeAttributesIfNeeded(nodeList);

            // Add onClick attributes
            nodeList = document.getElementsByTagName("g");
            addOnClickAttributes(nodeList);
            nodeList = document.getElementsByTagName("path");
            addOnClickAttributes(nodeList);
            nodeList = document.getElementsByTagName("rect");
            addOnClickAttributes(nodeList);
            nodeList = document.getElementsByTagName("circle");
            addOnClickAttributes(nodeList);
            nodeList = document.getElementsByTagName("ellipse");
            addOnClickAttributes(nodeList);
            nodeList = document.getElementsByTagName("polygon");
            addOnClickAttributes(nodeList);
            return convertDocumentToString(document);
        } catch (Exception e) {
            Timber.w(e);
            return getContext().getString(org.odk.collect.strings.R.string.svg_file_does_not_exist);
        }
    }

    private void addOnClickAttributes(NodeList nodes) {
        for (int i = 0; i < nodes.getLength(); i++) {
            Node node = nodes.item(i);
            Node elementId = node.getAttributes().getNamedItem("id");
            if (node.getNodeType() == Node.ELEMENT_NODE && elementId != null && doesElementExistInDataSet(elementId.getNodeValue())) {
                ((Element) node).setAttribute("onClick", "clickOnArea(this.id)");
            }
        }
    }

    private boolean doesElementExistInDataSet(String elementId) {
        for (SelectChoice item : items) {
            if (item.getValue().equals(elementId)) {
                return true;
            }
        }
        return false;
    }

    private void addSizeAttributesIfNeeded(NodeList nodes) {
        Node svg = nodes.item(0);
        if (svg.getAttributes().getNamedItem("width") == null) {
            ((Element) svg).setAttribute("width", "1000");
        }
        if (svg.getAttributes().getNamedItem("height") == null) {
            ((Element) svg).setAttribute("height", "1000");
        }
    }

    protected void refreshSelectedItemsLabel() {
        StringBuilder stringBuilder = new StringBuilder();
        if (!selections.isEmpty()) {
            stringBuilder
                    .append("<b>")
                    .append(getContext().getString(org.odk.collect.strings.R.string.selected))
                    .append("</b> ");
            for (Selection selection : selections) {
                String answer = getFormEntryPrompt().getSelectChoiceText(selection.choice);
                stringBuilder.append(answer);
                if (selections.indexOf(selection) < selections.size() - 1) {
                    stringBuilder.append(", ");
                }
            }
        }

        ((Activity) getContext()).runOnUiThread(() -> {
            binding.selectedElements.setText(HtmlUtils.textToHtml(stringBuilder.toString()));
            binding.selectedElements.setVisibility(binding.selectedElements.getText().toString().isBlank() ? GONE : VISIBLE);
        });
    }

    protected abstract void highlightSelections(WebView view);

    private class JavaScriptInterface {
        @android.webkit.JavascriptInterface
        public void selectArea(String areaId) {
            SelectImageMapWidget.this.selectArea(areaId);
        }

        @android.webkit.JavascriptInterface
        public void unselectArea(String areaId) {
            SelectImageMapWidget.this.unselectArea(areaId);
        }

        @android.webkit.JavascriptInterface
        public void notifyChanges() {
            SelectImageMapWidget.this.notifyChanges();
        }
    }
}
