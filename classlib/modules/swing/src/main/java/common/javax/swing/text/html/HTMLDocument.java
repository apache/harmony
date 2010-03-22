/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
/**
* @author Alexander T. Simbirtsev
*/
package javax.swing.text.html;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.URL;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.Stack;
import java.util.Vector;

import javax.swing.ButtonGroup;
import javax.swing.event.DocumentEvent.EventType;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultEditorKit;
import javax.swing.text.DefaultStyledDocument;
import javax.swing.text.Element;
import javax.swing.text.ElementIterator;
import javax.swing.text.MutableAttributeSet;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.html.HTML.Tag;
import javax.swing.text.html.HTMLEditorKit.Parser;
import javax.swing.text.html.parser.ParserDelegator;

import org.apache.harmony.x.swing.text.html.form.Form;
import org.apache.harmony.x.swing.text.html.form.FormAttributes;
import org.apache.harmony.x.swing.text.html.form.FormButtonModel;
import org.apache.harmony.x.swing.text.html.form.FormElement;
import org.apache.harmony.x.swing.text.html.form.FormFieldsetModel;
import org.apache.harmony.x.swing.text.html.form.FormOption;
import org.apache.harmony.x.swing.text.html.form.FormOptionGroup;
import org.apache.harmony.x.swing.text.html.form.FormSelectComboBoxModel;
import org.apache.harmony.x.swing.text.html.form.FormSelectListModel;
import org.apache.harmony.x.swing.text.html.form.FormSelectModel;
import org.apache.harmony.x.swing.text.html.form.FormTextModel;
import org.apache.harmony.x.swing.text.html.form.FormToggleButtonModel;

import org.apache.harmony.x.swing.internal.nls.Messages;

public class HTMLDocument extends DefaultStyledDocument {

    public class BlockElement extends BranchElement {
        public BlockElement(final Element parent, final AttributeSet attr) {
            super(parent, attr);
        }

        public String getName() {
            final Object tag = getAttribute(StyleConstants.NameAttribute);
            return tag != null ? tag.toString() : super.getName();
        }

        public AttributeSet getResolveParent() {
            return null;
        }
    }

    public class RunElement extends LeafElement {
        public RunElement(final Element parent, final AttributeSet a,
                          final int start, final int end) {
            super(parent, a, start, end);
        }

        public String getName() {
            final Object tag = getAttribute(StyleConstants.NameAttribute);
            return tag != null ? tag.toString() : super.getName();
        }

        public AttributeSet getResolveParent() {
            return null;
        }
    }

    public class HTMLReader extends HTMLEditorKit.ParserCallback {
       
        private boolean anchorReferenceEncountered = false;
        
        public class TagAction {
            public void start(final Tag tag, final MutableAttributeSet attr) {
            }

            public void end(final Tag tag) {
            }
        }

        public class BlockAction extends TagAction {
            public void start(final Tag tag, final MutableAttributeSet attr) {
                addJoinPreviousSpec = true;
                checkInsertTag(tag);
                skipAddingBlockSpec = false;
                blockOpen(tag, attr);
            }

            public void end(final Tag tag) {
                blockClose(tag);
            }
        }

        public class CharacterAction extends TagAction {
            public void start(final Tag tag, final MutableAttributeSet attr) {
                addJoinPreviousSpec = true;
                checkInsertTag(tag);
                pushCharacterStyle();
                charAttr.addAttribute(tag, attr.copyAttributes());
            }

            public void end(final Tag tag) {
                popCharacterStyle();
            }
        }

        public class FormAction extends SpecialAction {
            private static final String NO_NAME_ATTRIBUTE = "___no_name___";
            
            private Form currentForm;
            private HashMap radioGroupped = new HashMap();
            
            public void start(final Tag tag, final MutableAttributeSet attr) {
                if (Tag.OPTION.equals(tag) || Tag.OPTGROUP.equals(tag)) {
                    checkInsertTag(tag);
                    handleOption(tag, attr);
                    return;
                } 
                super.start(tag, attr);
                
                final ElementSpec spec = getLastSpec();
                assert spec != null : "we've just created a spec in super.start()";
                FormElement model = null;
                final MutableAttributeSet specAttr = (MutableAttributeSet)spec.getAttributes();
                if (Tag.INPUT.equals(tag)) {
                    model = handleInput(attr, specAttr);
                } else if (Tag.TEXTAREA.equals(tag)) {
                    model = new FormTextModel(getCurrentForm(), attr);
                    openedBlocks.add(Tag.TEXTAREA);
                } else if (Tag.BUTTON.equals(tag)) {
                    model = new FormButtonModel(getCurrentForm(), attr);
                    openedBlocks.add(Tag.BUTTON);
                } else if (Tag.LEGEND.equals(tag)) {
                    openedBlocks.add(Tag.LEGEND);
                    if (openedBlocks.contains(Tag.FIELDSET)) {
                        handleLegend(null, specAttr);
                    }
                } else if (Tag.FIELDSET.equals(tag)) {
                    model = new FormFieldsetModel(getCurrentForm(), attr);
                    openedBlocks.add(Tag.FIELDSET);
                } else if (Tag.SELECT.equals(tag)) {
                    if (FormAttributes.isListSelect(specAttr)) {
                        selectModel = new FormSelectListModel(getCurrentForm(), attr);
                    } else {
                        selectModel = new FormSelectComboBoxModel(getCurrentForm(), attr);
                    }
                    model = selectModel;
                    openedBlocks.add(Tag.SELECT);
                }
                if (model != null) {
                    specAttr.addAttribute(StyleConstants.ModelAttribute, model);
                    assert currentForm != null : "creating model with getCurrentForm() in constructor assures this";
                    currentForm.addElement(model);
                }
            }

            public void end(final Tag tag) {
                openedBlocks.remove(tag);
                if (Tag.SELECT.equals(tag)) {
                    selectModel = null;
                } else if (Tag.OPTGROUP.equals(tag)) {
                    if (selectModel != null) {
                        selectModel.getRootOptionGroup().popGroup();
                    }
                }
            }
            
            void openForm(final AttributeSet attr) {
                currentForm = new Form(attr);
            }
            
            void closeForm() {
                currentForm = null;
                selectModel = null;
                radioGroupped.clear();
            }
            
            private FormElement handleInput(final AttributeSet attr, final MutableAttributeSet specAttr) {
                FormElement result = null;
                String inputType = (String)attr.getAttribute(HTML.Attribute.TYPE);
                if (inputType == null) {
                    inputType = FormAttributes.INPUT_TYPE_TEXT;
                    specAttr.addAttribute(HTML.Attribute.TYPE, inputType);
                }
                int inputTypeIndex = FormAttributes.getTypeAttributeIndex((String)inputType);
                
                switch (inputTypeIndex) {
                case FormAttributes.INPUT_TYPE_TEXT_INDEX:
                case FormAttributes.INPUT_TYPE_PASSWORD_INDEX:
                    result = new FormTextModel(getCurrentForm(), attr, FormTextModel.ENABLE_MAX_LENGTH_BOUND);
                    break;
                case FormAttributes.INPUT_TYPE_FILE_INDEX:
                    result = new FormTextModel(getCurrentForm(), attr);
                    break;
                case FormAttributes.INPUT_TYPE_SUBMIT_INDEX:
                case FormAttributes.INPUT_TYPE_RESET_INDEX:
                case FormAttributes.INPUT_TYPE_BUTTON_INDEX:
                case FormAttributes.INPUT_TYPE_IMAGE_INDEX:
                    result = new FormButtonModel(getCurrentForm(), attr);
                    break;
                case FormAttributes.INPUT_TYPE_RADIO_INDEX:
                    FormToggleButtonModel buttonModel = new FormToggleButtonModel(getCurrentForm(), attr);
                    manageRadioGroup(attr, buttonModel);
                    result = buttonModel;
                    break;
                case FormAttributes.INPUT_TYPE_CHECKBOX_INDEX:
                    result = new FormToggleButtonModel(getCurrentForm(), attr);
                    break;
                default:
                    break;
                }
                return result;
            }

            private void manageRadioGroup(final AttributeSet attr,
                                          final FormToggleButtonModel buttonModel) {
                String name = (String)attr.getAttribute(HTML.Attribute.NAME);
                if (name == null) {
                    name = NO_NAME_ATTRIBUTE;
                }
                Object groupped = radioGroupped.get(name);
                if (groupped instanceof FormToggleButtonModel) {
                    FormToggleButtonModel grouppedModel = (FormToggleButtonModel)groupped;
                    ButtonGroup buttonGroup;
                    if (grouppedModel.getGroup() != null) {
                        buttonGroup = grouppedModel.getGroup();
                    } else {
                        buttonGroup = new ButtonGroup();
                        grouppedModel.setGroup(buttonGroup);
                    }
                    buttonModel.setGroup(buttonGroup);
                } else {
                    radioGroupped.put(name, buttonModel);
                }
            }

            private void handleOption(final Tag tag, final AttributeSet attr) {
                FormOption option = null;
                FormOptionGroup currentGroup = (selectModel != null) ? selectModel
                        .getRootOptionGroup().getCurrentGroup() : null;
                        
                if (Tag.OPTION.equals(tag)) {
                    option = new FormOption(currentGroup, attr);
                    openedBlocks.add(Tag.OPTION);
                } else if (Tag.OPTGROUP.equals(tag)) {
                    currentGroup = new FormOptionGroup(currentGroup, attr);
                    option = currentGroup;
                }
                
                if (option != null && selectModel != null) {
                    selectModel.addOption(option);
                    selectModel.getRootOptionGroup().pushGroup(currentGroup);
                }
            }
            
            private ElementSpec getLastSpec() {
                return !parseBuffer.isEmpty() ? (ElementSpec)parseBuffer.get(parseBuffer.size() - 1) : null;
            }

            private Form getCurrentForm() {
                return (currentForm != null) ? currentForm : (currentForm = new Form(SimpleAttributeSet.EMPTY));
            }
        }

        public class HiddenAction extends TagAction {
            public void start(final Tag tag, final MutableAttributeSet attr) {
                addSpecialElement(tag, attr);
            }

            public void end(final Tag tag) {
                addSpecialElement(tag, createMutableSet(HTML.Attribute.ENDTAG, Boolean.TRUE));
            }
        }

        public class IsindexAction extends TagAction {
            public void start(final Tag tag, final MutableAttributeSet attr) {
                addJoinPreviousSpec = true;
                checkInsertTag(tag);
                blockOpen(Tag.IMPLIED, new SimpleAttributeSet());
                addSpecialElement(tag, attr);
                blockClose(Tag.IMPLIED);
            }

        }

        public class ParagraphAction extends BlockAction {
            public void start(final Tag tag, final MutableAttributeSet attr) {
                super.start(tag, attr);
                openedBlocks.add(PARAGRAPH_TAG);
            }
        
            public void end(final Tag tag) {
                super.end(tag);
                openedBlocks.remove(PARAGRAPH_TAG);
            }
        }

        public class PreAction extends BlockAction {
            public void start(final Tag tag, final MutableAttributeSet attr) {
                super.start(tag, attr);
                MutableAttributeSet blockAttr = new SimpleAttributeSet(attr); 
                SimpleAttributeSet defaultAttr = getDefaultCSSAttributes(tag);
                if (defaultAttr != null) {
                    blockAttr.addAttributes(defaultAttr);
                }
                blockOpen(Tag.IMPLIED, blockAttr);
                impliedBlockOpen = true;
                needImpliedNewLine = true;
            }
        
            public void end(final Tag tag) {
                blockClose(Tag.IMPLIED);
                super.end(tag);
            }
        }

        public class SpecialAction extends TagAction {
            public void start(final Tag tag, final MutableAttributeSet attr) {
                addJoinPreviousSpec = true;
                addSpecialElement(tag, attr);
            }
        }

        class AdvancedCharacterAction extends CharacterAction {
            public void start(final Tag tag, final MutableAttributeSet attr) {
                super.start(tag, attr);
                final AttributeSet attrs = getDefaultCSSAttributes(tag);
                if (attrs != null) {
                    charAttr.addAttributes(attrs);
                }
            }
        }

        class LabelAction extends BlockAction {
            // TODO
        }
        
        class AnchorAction extends CharacterAction {
            public void start(final Tag tag, final MutableAttributeSet attr) {
                anchorReferenceEncountered = attr.isDefined(HTML.Attribute.HREF);
                //anchorReferenceEncountered verification added according to H4606
                if (anchorReferenceEncountered) {
                    super.start(tag, attr);
                    openedBlocks.add(Tag.A);
                }
            }
            
            public void end(final Tag tag) {
                // According to H4574 Empty AncorTextEncoured verification has
                // been removed, but according H4606 anchorReferenceEncountered
                // has been added
                if (anchorReferenceEncountered) {
                    super.end(tag);
                    openedBlocks.remove(Tag.A);
                    anchorReferenceEncountered = false;
                }
            }
        }
        
        class FontAction extends CharacterAction {
            final HTML.Attribute[] specialHTMLAttributes = new HTML.Attribute[] {
                                                             HTML.Attribute.FACE,
                                                             HTML.Attribute.COLOR,
                                                             HTML.Attribute.SIZE};

            final CSS.Attribute[] specialCSSAttributes = new CSS.Attribute[] {
                                                             CSS.Attribute.FONT_FAMILY,
                                                             CSS.Attribute.COLOR,
                                                             CSS.Attribute.FONT_SIZE};
            
            public void start(final Tag tag, final MutableAttributeSet attr) {
                super.start(tag, attr);
                
                final StyleSheet styleSheet = getStyleSheet();
                for (int i = 0; i < specialHTMLAttributes.length; i++) {
                    final String value = (String)attr.getAttribute(specialHTMLAttributes[i]);
                    if (value != null) {
                        styleSheet.addCSSAttributeFromHTML(charAttr, specialCSSAttributes[i], value);
                    }
                }
            }
        }
        
        class FormTagAction extends BlockAction {
            private Tag[] formActionTags = new Tag[] {Tag.TEXTAREA, Tag.SELECT,
                                                 Tag.INPUT, Tag.OPTION,
                                                 Tag.OPTGROUP, Tag.BUTTON,
                                                 Tag.LEGEND, Tag.FIELDSET};
            
            public void start(final Tag tag, final MutableAttributeSet attr) {
                super.start(tag, attr);
                FormAction formAction = getFormAction();
                if (formAction != null) {
                    formAction.openForm(attr);
                }
            }

            public void end(final Tag tag) {
                super.end(tag);
                FormAction formAction = getFormAction();
                if (formAction != null) {
                    formAction.closeForm();
                }
            }

            private FormAction getFormAction() {
                for (int i = 0; i < formActionTags.length; i++) {
                    final Tag tag = formActionTags[i];
                    TagAction action = getAction(tag);
                    if (action instanceof FormAction) {
                        return (FormAction)action;
                    }
                }
                return null;
            }
        }
        
        class ImageAction extends SpecialAction {

            @Override
            public void start(Tag tag, MutableAttributeSet attr) {

                if (anchorReferenceEncountered) {

                    attr.addAttributes(charAttr.copyAttributes());
                }

                super.start(tag, attr);
            }
        }
        
        class BaseAction extends TagAction {
            public void start(final Tag tag, final MutableAttributeSet attr) {
                checkInsertTag(tag);
                if (attr == null) {
                    return;
                }
                final String href = (String)attr.getAttribute(HTML.Attribute.HREF);
                if (href == null) {
                    return;
                }

                final URL url = HTML.resolveURL(href, getBase());
                setBase(url);
                putProperty(INITIAL_BASE_PROPERTY, url);
            }
        }

        class HeadAction extends BlockAction {
            public void end(final Tag tag) {
                super.end(tag);
                if (styleRule != null) {
                    getStyleSheet().addRule(styleRule);
                    styleRule = null;
                }
            }
        }

        class TitleAction extends BlockAction {
            public void start(final Tag tag, final MutableAttributeSet attr) {
                addJoinPreviousSpec = true;
                checkInsertTag(tag);
                addSpecialElement(tag, attr);
                openedBlocks.add(Tag.TITLE);
            }

            public void end(final Tag tag) {
                addSpecialElement(tag, createMutableSet(HTML.Attribute.ENDTAG, Boolean.TRUE));
                openedBlocks.remove(Tag.TITLE);
            }
        }

        class AppletAction extends HiddenAction {
            // TODO: implement
            public void start(final Tag tag, final MutableAttributeSet attr) {
                addJoinPreviousSpec = true;
                super.start(tag, attr);
            }
        }

        class AreaAction extends HiddenAction {
            // TODO: implement
        }

        class MapAction extends HiddenAction {
            // TODO: implement
        }

        class ScriptAction extends HiddenAction {
            // TODO: implement
            public void start(final Tag tag, final MutableAttributeSet attr) {
                addJoinPreviousSpec = true;
                super.start(tag, attr);
            }
        }

        class LinkAction extends HiddenAction {
            public void start(final Tag tag, final MutableAttributeSet attr) {
                addJoinPreviousSpec = true;
                super.start(tag, attr);
                if (attr.containsAttribute(HTML.Attribute.TYPE, "text/css")) {
                    loadCSS(attr);
                }
            }
            
            public void end(final Tag tag) {
            }
            
            private void loadCSS(final AttributeSet attr) {
                String href = (String)attr.getAttribute(HTML.Attribute.HREF);
                final URL url = HTML.resolveURL(href, getBase());
                try {
                    getStyleSheet().loadRules(new BufferedReader(new InputStreamReader(url.openStream())), url);
                } catch (IOException e) {
                }
            }
        }

        class MetaAction extends SpecialAction {
            public void end(final Tag tag) {
            }
        }

        class StyleAction extends TagAction {
            public void start(Tag tag, MutableAttributeSet attr) {
                checkInsertTag(tag);
                openedBlocks.add(Tag.STYLE);
            }
            
            public void end(Tag tag) {
                openedBlocks.remove(Tag.STYLE);
            }
        }

        protected MutableAttributeSet charAttr = new SimpleAttributeSet();

        protected Vector<DefaultStyledDocument.ElementSpec> parseBuffer =
                new Vector<DefaultStyledDocument.ElementSpec>();

        private static final String PARAGRAPH_TAG = "_paragraph_tag_";
        private static final int IMPLIED_HTML_DOCUMENT_START_SPECS_NUMBER = 8;
        private static final int TOKEN_THRESHOLD_MULTIPLIER = 5;

        private final HashMap tagActionMap = new HashMap();
        private final TagAction emptyAction = new TagAction(); 
        private final Stack attrStack = new Stack();
        private final Set openedBlocks = new HashSet();
        private boolean impliedBlockOpen;
        private int numBlocksOpen;

        private boolean needImpliedNewLine;
        private String styleRule;
        private FormSelectModel selectModel;
        private int tokenThreshold; 
        
        private int offset;
        private int popDepth;
        private int pushDepth;
        private Tag insertTag;
        private boolean insertTagFound;
        private boolean implicitSpecsRemove;
        private boolean skipAddingBlockSpec;
        private boolean addJoinPreviousSpec;
        private int specsCount;

        public HTMLReader(final int offset) {
            this(offset, 0, 0, null);
        }

        public HTMLReader(final int offset, final int popDepth,
                          final int pushDepth, final Tag insertTag) {
            this.offset = offset;
            this.popDepth = popDepth;
            this.pushDepth = pushDepth;
            this.insertTag = insertTag;
            insertTagFound = (insertTag == null);
            tokenThreshold = getTokenThreshold();
            fillTagActionMap();
        }

        public void handleComment(final char[] data, final int pos) {
            final String comment = new String(data);
            if (openedBlocks.contains(Tag.P)) {
                addSpecialElement(Tag.COMMENT, createMutableSet(HTML.Attribute.COMMENT, comment));
            } else {
                Vector comments = (Vector)getProperty(AdditionalComments);
                if (comments == null) {
                    comments = new Vector();
                    putProperty(AdditionalComments, comments);
                }
                comments.add(comment);
            }
        }

        public void handleEndOfLineString(final String eol) {
            putProperty(DefaultEditorKit.EndOfLineStringProperty, eol);
        }

        public void handleSimpleTag(final Tag tag,
                                    final MutableAttributeSet attr,
                                    final int pos) {
            final TagAction action = getAction(tag);
            MutableAttributeSet tagAttr = handleStyleAttribute(attr);
            if (action != emptyAction || !getPreservesUnknownTags()) {
                action.start(tag, tagAttr);
                action.end(tag);
            } else {
                addSpecialElement(tag, tagAttr);
            }
        }

        public void handleStartTag(final Tag tag,
                                   final MutableAttributeSet attr, final int pos) {
            final TagAction action = getAction(tag);
            action.start(tag, handleStyleAttribute(attr));
        }

        public void handleEndTag(final Tag tag, final int pos) {
            final TagAction action = getAction(tag);
            action.end(tag);
        }

        public void handleText(final char[] data, final int pos) {
            if (openedBlocks.contains(Tag.TITLE)) {
                putProperty(TitleProperty, new String(data));
                return;
            } 
            if (openedBlocks.contains(Tag.STYLE) && openedBlocks.contains(Tag.HEAD)) {
                final String newStyle = new String(data);
                if (styleRule == null) {
                    styleRule = newStyle;
                } else {
                    styleRule += newStyle;
                }
                return;
            } 
            if (openedBlocks.contains(Tag.TEXTAREA)) {
                textAreaContent(data);
                return;
            } 
            if (openedBlocks.contains(Tag.PRE)) {
                preContent(data);
                return;
            } 
            if (openedBlocks.contains(Tag.OPTION) && openedBlocks.contains(Tag.SELECT)) {
                final Option option = selectModel.getLastOption();
                if (option != null && !(option instanceof FormOptionGroup)) {
                    option.setLabel(new String(data));
                }
            } 
            if (openedBlocks.contains(Tag.LEGEND) && openedBlocks.contains(Tag.FIELDSET)) {
                if (handleLegend(new String(data), null)) {
                    return;
                }
            } 
            if (numBlocksOpen > 0) {
                addContent(data, 0, data.length);
            }
        }

        public void flush() throws BadLocationException {
            flushImpl(true);
        }

        protected void registerTag(final Tag tag,
                                   final TagAction action) {
            tagActionMap.put(tag, action);
        }

        protected void pushCharacterStyle() {
            if (charAttr == null) {
                throw new NullPointerException();
            }
            attrStack.push(charAttr.copyAttributes());
        }

        protected void popCharacterStyle() {
            if (!attrStack.empty()) {
                charAttr = (MutableAttributeSet)attrStack.pop();
            }
        }

        protected void preContent(char[] data) {
            int offset = 0;

            for (int i = 0; i < data.length; i++) {
                if ((data[i] == '\n') || (data[i] == '\r')) {
                    addContent(data, offset, i - offset);
                    blockClose(HTML.Tag.IMPLIED);

                    blockOpen(HTML.Tag.IMPLIED, new SimpleAttributeSet());
                    offset = i + 1;
                }
            }

            if (offset < data.length) {
                addContent(data, offset, data.length - offset);
            }
        }

        protected void addContent(final char[] data, final int offset,
                                  final int length) {
            addContent(data, offset, length, true);
        }

        protected void addContent(final char[] data, final int offset,
                                  final int length,
                                  final boolean createImpliedPIfNecessary) {
            addContentSpec(Tag.CONTENT, data, offset, length, charAttr, createImpliedPIfNecessary);
            
            if (parseBuffer.size() > tokenThreshold) {
                try {
                    flushImpl(false);
                } catch (BadLocationException e) {
                }
                tokenThreshold *= TOKEN_THRESHOLD_MULTIPLIER;
            }
        }

        protected void addSpecialElement(final Tag tag,
                                         final MutableAttributeSet attr) {
            final boolean needImpliedBlock = !Tag.FRAME.equals(tag);
            if (!needImpliedBlock) {
                needImpliedNewLine = false;
            }
            addContentSpec(tag, new char[] {' '}, 0, 1, attr, needImpliedBlock);
        }
        
        protected void textAreaContent(final char[] data) {
            final ElementSpec textareaSpec = findLastSpec(Tag.TEXTAREA);
            if (textareaSpec == null) {
                return;
            }
            FormTextModel doc = (FormTextModel)getModel(textareaSpec);
            if (doc != null) {
                doc.setInitialContent(new String(data));
            }
        }

        protected void blockOpen(final Tag tag,
                                 final MutableAttributeSet attr) {
            if (impliedBlockOpen && !Tag.IMPLIED.equals(tag)) {
                blockClose(Tag.IMPLIED);
                impliedBlockOpen = false;
            }
            if (!skipAddingBlockSpec) {
                ElementSpec blockSpec = new ElementSpec(deriveSpecAttributes(tag, attr), ElementSpec.StartTagType);
                addSpec(blockSpec);
            } 
            skipAddingBlockSpec = false;
            needImpliedNewLine = true;
            openedBlocks.add(tag);
            numBlocksOpen++;
        }

        protected void blockClose(final Tag tag) {
            if (needImpliedNewLine) {
                addImpliedNewLine();
                if (impliedBlockOpen && !Tag.IMPLIED.equals(tag)) {
                    blockClose(Tag.IMPLIED);
                    impliedBlockOpen = false;
                }
            }
            numBlocksOpen--;
            openedBlocks.remove(tag);
            addSpec(new ElementSpec(null, ElementSpec.EndTagType));
        }

        private Object getModel(final ElementSpec spec) {
            return spec.getAttributes().getAttribute(StyleConstants.ModelAttribute);
        }

        private void addImpliedNewLine() {
            pushCharacterStyle();
            charAttr.addAttribute(HTML.Attribute.IMPLIED_NEW_LINE, Boolean.TRUE);
            addContent(new char[] {'\n'}, 0, 1, true);
            popCharacterStyle();
            needImpliedNewLine = false;
        }

        private MutableAttributeSet handleStyleAttribute(final MutableAttributeSet attr) {
            final String style = (String)attr.getAttribute(HTML.Attribute.STYLE);
            if (style != null) {
                attr.addAttributes(getStyleSheet().getDeclaration(style));
                attr.removeAttribute(HTML.Attribute.STYLE);
            }
            return attr;
        }

        private void createImpliedBlock() {
            if (paragraphOpened()) {
                return;
            }
            blockOpen(Tag.IMPLIED, new SimpleAttributeSet());
            impliedBlockOpen = true;
        }

        private MutableAttributeSet createMutableSet(final Object key, final Object value) {
            MutableAttributeSet specAttr = new SimpleAttributeSet();
            specAttr.addAttribute(key, value);
            return specAttr;
        }

        private boolean paragraphOpened() {
            return openedBlocks.contains(PARAGRAPH_TAG) || openedBlocks.contains(Tag.IMPLIED) && impliedBlockOpen;
        }

        private boolean handleLegend(final String legend, final MutableAttributeSet legendAttr) {
            final ElementSpec fieldSetSpec = findLastSpec(Tag.FIELDSET);
            FormFieldsetModel fieldSet = (FormFieldsetModel)getModel(fieldSetSpec);
            if (fieldSet == null || fieldSet.getLegend() != null) {
                return false;
            }                
            if (legend != null) {
                fieldSet.setLegend(legend);
            }
            if (legendAttr != null) {
                fieldSet.setLegendAttributes(legendAttr);
            }
            return true;
        }

        private void setRemoveImplicitSpecs(final boolean implicitSpecsRemove) {
            this.implicitSpecsRemove = implicitSpecsRemove;
        }

        private void flushImpl(final boolean isFinal) throws BadLocationException {
            if (parseBuffer.isEmpty()) {
                return;
            }
            if (isCreate()) {
                ElementSpec[] specs = vectorToArray(parseBuffer);
                create(specs);
                removeDefaultBody();
            } else {
                if (needAddingPopPushSpecs()) {
                    addPopPushSpecs(parseBuffer);
                }
                ElementSpec[] specs = isFinal ? trimEndSpecs(parseBuffer)
                        : vectorToArray(parseBuffer);
                insert(offset, specs);
            }
            
            parseBuffer.clear();
        }

        private void removeDefaultBody() {
            Element impliedLF = getCharacterElement(getLength() - 1);
            try {
                remove(getLength() - 1, 1);
            } catch (BadLocationException e) {
            }
            BranchElement root = (BranchElement)getDefaultRootElement();
            final int oddBodyIndex = root.getElementCount() - 1;
            Element oddBody = root.getElement(oddBodyIndex);
            final Element[] emptyArray = new Element[0];
            root.replace(oddBodyIndex, 1, emptyArray);
            
            Element lf = getCharacterElement(getLength());
            writeLock();
            try {
                ((MutableAttributeSet)lf).removeAttributes(lf.getAttributes().getAttributeNames());
                ((MutableAttributeSet)lf).addAttributes(impliedLF.getAttributes());
            } finally {
                writeUnlock();
            }
            
            final DefaultDocumentEvent removeEvent = new DefaultDocumentEvent(oddBody.getStartOffset(), oddBody.getEndOffset() - oddBody.getStartOffset(), EventType.REMOVE);
            removeEvent.addEdit(new ElementEdit(root, oddBodyIndex, new Element[] {oddBody}, emptyArray));
            fireRemoveUpdate(removeEvent);
        }

        private boolean isCreate() {
            return offset == 0 && insertTag == null && getLength() == 0 && !implicitSpecsRemove;
        }

        private ElementSpec[] trimEndSpecs(final Vector buffer) {
            if ((implicitSpecsRemove || insertTag != null && insertTagFound)) {
                for (int i = 0; i < 4; i++) {
                    buffer.remove(buffer.size() - 1);
                }
            }
            return vectorToArray(buffer);
        }

        private ElementSpec[] vectorToArray(final Vector buffer) {
            return (ElementSpec[])buffer.toArray(new ElementSpec[buffer.size()]);
        }

        private boolean needAddingPopPushSpecs() {
            return (insertTag != null && insertTagFound || insertTag == null) && (popDepth != 0 || pushDepth != 0);
        }

        private void addContentSpec(final Tag tag, final char[] data,
                                    final int offset, final int length,
                                    final MutableAttributeSet attr,
                                    final boolean createImpliedBlock) {
            if (createImpliedBlock) {
                createImpliedBlock();
            }
            checkInsertTag(tag);
            addSpec(new ElementSpec(deriveSpecAttributes(tag, attr),
                                    ElementSpec.ContentType, data, offset,
                                    length));
        }

        private SimpleAttributeSet deriveSpecAttributes(final Tag tag, final MutableAttributeSet attr) {
            attr.removeAttribute(IMPLIED);
            attr.addAttribute(StyleConstants.NameAttribute, tag);
            return new SimpleAttributeSet(attr);
        }

        private void addSpec(final ElementSpec spec) {
            if (insertTagFound && (!implicitSpecsRemove || 
                    specsCount >= IMPLIED_HTML_DOCUMENT_START_SPECS_NUMBER)) {
                parseBuffer.add(spec);
            }
            specsCount++;
        }

        private void fillTagActionMap() {
            HiddenAction hiddenAction = new HiddenAction();
            CharacterAction characterAction = new CharacterAction();
            AdvancedCharacterAction advancedCharacterAction = new AdvancedCharacterAction();
            BlockAction blockAction = new BlockAction();
            ParagraphAction paragraphAction = new ParagraphAction();
            SpecialAction specialAction = new SpecialAction();
            FormAction formAction = new FormAction();
            
            tagActionMap.put(Tag.A,  new AnchorAction());
            tagActionMap.put(Tag.ABBR, characterAction);
            tagActionMap.put(Tag.ACRONYM, characterAction);
            tagActionMap.put(Tag.ADDRESS, characterAction);
            tagActionMap.put(Tag.APPLET, new AppletAction());
            tagActionMap.put(Tag.AREA, new AreaAction());
            tagActionMap.put(Tag.B, advancedCharacterAction);
            tagActionMap.put(Tag.BASE, new BaseAction());
            tagActionMap.put(Tag.BASEFONT, characterAction);
            tagActionMap.put(Tag.BIG, characterAction);
            tagActionMap.put(Tag.BDO, characterAction);
            tagActionMap.put(Tag.BLOCKQUOTE, blockAction);
            tagActionMap.put(Tag.BODY, blockAction);
            tagActionMap.put(Tag.BR, specialAction);
            tagActionMap.put(Tag.BUTTON, formAction);
            tagActionMap.put(Tag.CAPTION, blockAction);
            tagActionMap.put(Tag.CENTER, blockAction);
            tagActionMap.put(Tag.CITE, characterAction);
            tagActionMap.put(Tag.CODE, characterAction);
            tagActionMap.put(Tag.COL, hiddenAction);
            tagActionMap.put(Tag.COLGROUP, hiddenAction);
            tagActionMap.put(Tag.DD, blockAction);
            tagActionMap.put(Tag.DFN, characterAction);
            tagActionMap.put(Tag.DEL, characterAction);
            tagActionMap.put(Tag.DIR, blockAction);
            tagActionMap.put(Tag.DIV, blockAction);
            tagActionMap.put(Tag.DL, blockAction);
            tagActionMap.put(Tag.DT, paragraphAction);
            tagActionMap.put(Tag.EM, characterAction);
            tagActionMap.put(Tag.FIELDSET, formAction);
            tagActionMap.put(Tag.FONT, new FontAction());
            tagActionMap.put(Tag.FORM, new FormTagAction());
            tagActionMap.put(Tag.FRAME, specialAction);
            tagActionMap.put(Tag.FRAMESET, blockAction);
            tagActionMap.put(Tag.H1, paragraphAction);
            tagActionMap.put(Tag.H2, paragraphAction);
            tagActionMap.put(Tag.H3, paragraphAction);
            tagActionMap.put(Tag.H4, paragraphAction);
            tagActionMap.put(Tag.H5, paragraphAction);
            tagActionMap.put(Tag.H6, paragraphAction);
            tagActionMap.put(Tag.HEAD, new HeadAction());
            tagActionMap.put(Tag.HR, specialAction);
            tagActionMap.put(Tag.HTML, blockAction);
            tagActionMap.put(Tag.I, advancedCharacterAction);
            tagActionMap.put(Tag.IFRAME, hiddenAction);
            tagActionMap.put(Tag.IMG, new ImageAction());
            tagActionMap.put(Tag.INPUT, formAction);
            tagActionMap.put(Tag.INS, characterAction);
            tagActionMap.put(Tag.ISINDEX, new IsindexAction());
            tagActionMap.put(Tag.KBD, characterAction);
            tagActionMap.put(Tag.LABEL, new LabelAction());
            tagActionMap.put(Tag.LEGEND, formAction);
            tagActionMap.put(Tag.LI, blockAction);
            tagActionMap.put(Tag.LINK, new LinkAction());
            tagActionMap.put(Tag.MAP, new MapAction());
            tagActionMap.put(Tag.MENU, blockAction);
            tagActionMap.put(Tag.META, new MetaAction());
            tagActionMap.put(Tag.NOFRAMES, blockAction);
            tagActionMap.put(Tag.NOSCRIPT, blockAction);
            tagActionMap.put(Tag.OBJECT, specialAction);
            tagActionMap.put(Tag.OL, blockAction);
            tagActionMap.put(Tag.OPTION, formAction);
            tagActionMap.put(Tag.OPTGROUP, formAction);
            tagActionMap.put(Tag.P, paragraphAction);
            tagActionMap.put(Tag.PARAM, hiddenAction);
            tagActionMap.put(Tag.PRE, new PreAction());
            tagActionMap.put(Tag.Q, characterAction);
            tagActionMap.put(Tag.SAMP, characterAction);
            tagActionMap.put(Tag.SCRIPT, new ScriptAction());
            tagActionMap.put(Tag.SELECT, formAction);
            tagActionMap.put(Tag.SMALL, characterAction);
            tagActionMap.put(Tag.STRIKE, advancedCharacterAction);
            tagActionMap.put(Tag.S, characterAction);
            tagActionMap.put(Tag.SPAN, characterAction);
            tagActionMap.put(Tag.STRONG, characterAction);
            tagActionMap.put(Tag.STYLE, new StyleAction());
            tagActionMap.put(Tag.SUB, advancedCharacterAction);
            tagActionMap.put(Tag.SUP, advancedCharacterAction);
            tagActionMap.put(Tag.TABLE, blockAction);
            tagActionMap.put(Tag.TBODY, blockAction);
            tagActionMap.put(Tag.TD, blockAction);
            tagActionMap.put(Tag.TEXTAREA, formAction);
            tagActionMap.put(Tag.TFOOT, blockAction);
            tagActionMap.put(Tag.THEAD, blockAction);
            tagActionMap.put(Tag.TH, blockAction);
            tagActionMap.put(Tag.TITLE, new TitleAction());
            tagActionMap.put(Tag.TR, blockAction);
            tagActionMap.put(Tag.TT, characterAction);
            tagActionMap.put(Tag.U, advancedCharacterAction);
            tagActionMap.put(Tag.UL, blockAction);
            tagActionMap.put(Tag.VAR, characterAction);
        }

        private TagAction getAction(final Tag tag) {
            TagAction action = (TagAction)tagActionMap.get(tag);
            if (action == null) {
                action = emptyAction;
            }
            return action;
        }

        private ElementSpec findLastSpec(final Tag tag) {
            for (int i = parseBuffer.size() - 1; i >= 0; i--) {
                ElementSpec spec = (ElementSpec)parseBuffer.get(i);
                final AttributeSet specAttr = spec.getAttributes();
                if (specAttr != null && specAttr.containsAttribute(StyleConstants.NameAttribute, tag)) {
                    return spec;
                }
            }
            return null;
        }
        
        private boolean checkInsertTag(final Tag tag) {
            if (!insertTagFound && insertTag.equals(tag)) {
                insertTagFound = true;
                skipAddingBlockSpec = true;  
                addPopPushSpecs(parseBuffer);
            }
            
            return insertTagFound;
        }

        private void addPopPushSpecs(final Vector buffer) {
            addJoinPreviousSpec &= (pushDepth > 0 || popDepth > 0);
//            boolean addNewLineTag = (pushDepth == 0 && popDepth > 0);
//            if (addNewLineTag && !implicitSpecsRemove && offset == 0) {
//                parseBuffer.add(0, new ElementSpec(null, ElementSpec.ContentType, new char[] {'\n'}, 0, 1));
//            }
            for (int i = 0; i < pushDepth; i++) {
                final ElementSpec pushSpec = new ElementSpec(null, ElementSpec.StartTagType);
                pushSpec.setDirection(ElementSpec.JoinNextDirection);
                buffer.add(0, pushSpec);
            }
            pushDepth = 0;
            for (int i = 0; i < popDepth; i++) {
                final ElementSpec popSpec = new ElementSpec(null, ElementSpec.EndTagType);
                buffer.add(0, popSpec);
            }
            popDepth = 0;
            if (addJoinPreviousSpec && !implicitSpecsRemove && offset == 0) {
                ElementSpec joinPreviousSpec = new ElementSpec(null, ElementSpec.ContentType, new char[] {'\n'}, 0, 1);
                joinPreviousSpec.setDirection(ElementSpec.JoinPreviousDirection);
                parseBuffer.add(0, joinPreviousSpec);
                addJoinPreviousSpec = false;
            }
        }
    }

    public abstract static class Iterator {
        public abstract AttributeSet getAttributes();
        public abstract int getEndOffset();
        public abstract int getStartOffset();
        public abstract Tag getTag();
        public abstract boolean isValid();
        public abstract void next();
    }

    public static final String AdditionalComments = "AdditionalComments";
    static final String INITIAL_BASE_PROPERTY = "_initialBase_";
    
    private static final String TARGET_TOP = "_top";
    private static final String TARGET_SELF = "_self";
    private static final String TARGET_PARENT = "_parent";

    private static final SimpleAttributeSet DEFAULT_CHARACTER_ATTRIBUTES = new SimpleAttributeSet();  

    private URL base;
    private Parser parser;
    private boolean preservesUnknownTags = true;
    private int threshold = Integer.MAX_VALUE;
    
    static {
        initDefaultCharacterAttributes();
    }

    public HTMLDocument(final Content c, final StyleSheet styles) {
        super(c, styles);
    }

    public HTMLDocument(final StyleSheet styles) {
        super(styles);
    }

    public HTMLDocument() {
        super(new StyleSheet());
    }

    public void setBase(final URL base) {
        this.base = base;
        getStyleSheet().setBase(base);
    }

    public URL getBase() {
        return base;
    }

    public void setParser(final HTMLEditorKit.Parser parser) {
        this.parser = parser;
    }

    public HTMLEditorKit.Parser getParser() {
        return parser;
    }

    public void setPreservesUnknownTags(final boolean preservesTags) {
        preservesUnknownTags = preservesTags;
    }

    public boolean getPreservesUnknownTags() {
        return preservesUnknownTags;
    }

    public void setTokenThreshold(final int threshold) {
        this.threshold = threshold;
    }

    public int getTokenThreshold() {
        return threshold;
    }

    public Element getElement(final Element e, final Object attribute,
                              final Object value) {
        final ElementIterator it = new ElementIterator(e);
        while (it.next() != null) {
            final Element current = it.current();
            if (current.getAttributes().containsAttribute(attribute, value)) {
                return current;
            }
        }
        return null;
    }

    public Element getElement(final String id) {
        return getElement(getDefaultRootElement(), HTML.Attribute.ID, id);
    }

    public Iterator getIterator(final Tag tag) {
        return new TagIterator(tag, this);
    }

    public HTMLEditorKit.ParserCallback getReader(final int pos,
                                                  final int popDepth,
                                                  final int pushDepth,
                                                  final Tag insertTag) {
        return new HTMLReader(pos, popDepth, pushDepth, insertTag);
    }

    public HTMLEditorKit.ParserCallback getReader(final int pos) {
        return new HTMLReader(pos);
    }

    public StyleSheet getStyleSheet() {
        return (StyleSheet)getAttributeContext();
    }

    public void insertBeforeEnd(final Element elem, final String htmlText)
            throws BadLocationException, IOException {
        checkParser();
        checkLeaf(elem);
        int offset = isParagraph(elem) ? elem.getEndOffset() - 1 : elem.getEndOffset();
        insertHTMLText(elem, offset, htmlText);
    }
    
    private boolean isParagraph(final Element elem) {
        final AttributeSet attr = elem.getAttributes();
        return attr != null && Tag.P.equals(attr.getAttribute(StyleConstants.NameAttribute));
    }

    public void insertAfterEnd(final Element elem, final String htmlText)
            throws BadLocationException, IOException {
        checkParser();
        if (elem == null) {
            return;
        }
        int offset = elem.getEndOffset() > getLength() ? getLength() : elem.getEndOffset();
        insertHTMLText(elem.getParentElement(), offset, htmlText);
    }

    public void insertBeforeStart(final Element elem, final String htmlText)
            throws BadLocationException, IOException {
        checkParser();
        if (elem == null) {
            return;
        }
        insertHTMLText(elem.getParentElement(), elem.getStartOffset(), htmlText);
    }
    
    public void insertAfterStart(final Element elem, final String htmlText)
            throws BadLocationException, IOException {
        checkParser();
        checkLeaf(elem);
        insertHTMLText(elem, elem.getStartOffset(), htmlText);
    }

    public void setInnerHTML(final Element elem, final String htmlText)
            throws BadLocationException, IOException {
        checkParser();
        if (elem == null) {
            return;
        }
        checkLeaf(elem);

        int numRemovingElements = elem.getElementCount();
        insertHTMLText(elem, elem.getStartOffset(), htmlText);
        removeElements(elem, elem.getElementCount() - numRemovingElements, numRemovingElements);
        throw new UnsupportedOperationException(Messages.getString("swing.27")); //$NON-NLS-1$
    }
    
    public void setOuterHTML(final Element elem, final String htmlText)
            throws BadLocationException, IOException {
        checkParser();
        if (elem == null) {
            return;
        }
        
        int length = elem.getEndOffset() - elem.getStartOffset() - 1;
        final BranchElement parent = (BranchElement)elem.getParentElement();
        final int indexBefore = getElementIndex(parent, elem);
        final int numElementsBefore = parent.getElementCount();
        insertHTMLText(elem.getParentElement(), elem.getStartOffset(), htmlText);
        removeElements(parent, indexBefore + (parent.getElementCount() - numElementsBefore), 1);
        throw new UnsupportedOperationException(Messages.getString("swing.27")); //$NON-NLS-1$
    }
    
    public void processHTMLFrameHyperlinkEvent(final HTMLFrameHyperlinkEvent event) {
        final String target = event.getTarget();

        if (TARGET_TOP.equals(target)){
            return;
        }

        final Element source = event.getSourceElement();
        final String src = event.getURL().toString();
        if (source != null && TARGET_SELF.equals(target)) {
            processTarget(source, src);
        } else if (source != null && TARGET_PARENT.equals(target)) {
            final Element parent = source.getParentElement();
            if (getParser() == null) {
                setParser(new ParserDelegator());
            }
            try {
                setOuterHTML(parent, "<frame src=\"" + src + "\">");
            } catch (BadLocationException e) {
            } catch (IOException e) {
            }
        } else {
            final ElementIterator frameIterator = new ElementIterator(getDefaultRootElement());
            if (frameIterator != null) {
                while (frameIterator.next() != null) {
                    final Element element = frameIterator.current();
                    if (Tag.FRAME.equals(element.getName())
                        && element.getAttributes().containsAttribute(HTML.Attribute.NAME, target)) {
                        
                        processTarget(element, src);
                    }
                }
            }
        }
    }
    
    protected AbstractElement createDefaultRoot() {
        final BlockElement root = new BlockElement(null, null);
        writeLock();
        try {
            root.addAttribute(StyleConstants.NameAttribute, Tag.HTML);
            AttributeSet attr = getAttributeContext().getEmptySet();
            final BranchElement body = (BranchElement)createBranchElement(root,
                                                                          null);
            body.addAttribute(StyleConstants.NameAttribute, Tag.BODY);
            
            final BranchElement p = (BranchElement)createBranchElement(body, null);
            p.addAttribute(StyleConstants.NameAttribute, Tag.P);
            p.addAttribute(CSS.Attribute.MARGIN_TOP, 
                           CSS.Attribute.MARGIN_TOP.getDefaultValue());
            
            final LeafElement content =
                (LeafElement)createLeafElement(p, null, getStartPosition().getOffset(),
                                              getEndPosition().getOffset());
            content.addAttribute(StyleConstants.NameAttribute, Tag.CONTENT);
            p.replace(0, 0, new Element[] {content});
            body.replace(0, 0, new Element[] {p});
            root.replace(0, 0, new Element[] {body});
        } finally {
            writeUnlock();
        }
        return root;
    }

    protected Element createLeafElement(final Element parent,
                                        final AttributeSet a,
                                        final int start, final int end) {
        return new RunElement(parent, a, start, end);
    }

    protected Element createBranchElement(final Element parent,
                                          final AttributeSet attr) {
        return new BlockElement(parent, attr);
    }
    
    protected void insertUpdate(final DefaultDocumentEvent event, final AttributeSet attrs) {
        AttributeSet contentAttr = attrs;
        if (contentAttr == null) {
            contentAttr = new SimpleAttributeSet();
            ((SimpleAttributeSet)contentAttr).addAttribute(StyleConstants.NameAttribute, Tag.CONTENT);
        }
        super.insertUpdate(event, contentAttr);
    }

    private void processTarget(final Element target, final String src) {
        final MutableAttributeSet targetAttr = (MutableAttributeSet)target.getAttributes();
        writeLock();
        try {
            targetAttr.addAttribute(HTML.Attribute.SRC, src);
        } finally {
            writeUnlock();
        }
        fireChangedUpdate(new DefaultDocumentEvent(target.getStartOffset(), target.getEndOffset() - target.getStartOffset(), EventType.CHANGE));
    }

    private void removeElements(final Element parent, final int startRemoveIndex, final int numRemoved) {
        final Element[] emptyArray = new Element[0];
        final Element[] removedArray = new Element[numRemoved];
        final BranchElement branch = (BranchElement)parent;
        final int numElements = branch.getElementCount();
        for (int i = 0; i < numRemoved; i++) {
            removedArray[i] = branch.getElement(startRemoveIndex + i);
        }
        branch.replace(startRemoveIndex, numRemoved, emptyArray);
        final int eventLength = removedArray[numRemoved - 1].getEndOffset() - removedArray[0].getStartOffset();
        DefaultDocumentEvent removeEvent = new DefaultDocumentEvent(removedArray[0].getStartOffset(), 
                                                                    eventLength, 
                                                                    EventType.REMOVE);
        removeEvent.addEdit(new ElementEdit(parent, startRemoveIndex, removedArray, emptyArray));
        fireRemoveUpdate(removeEvent);
    }

    private void insertHTMLText(final Element elem, final int offset,
                                final String htmlText) throws IOException {
        Element preceedBranch = getInsertionStartElement(offset - 1);
        int pop = -1;
        int push = -1;
        do {
            pop++;
            preceedBranch = preceedBranch.getParentElement();
            push = getAncestorDepth(preceedBranch, elem);
        } while (preceedBranch != null && push < 0);
        if (push == -1) {
            return;
        }
        
        final HTMLReader reader = new HTMLReader(offset, pop, push, null);
        reader.setRemoveImplicitSpecs(true);
        parser.parse(new StringReader(htmlText), reader, true);
        try {
            reader.flush();
        } catch (BadLocationException e) {
            e.printStackTrace();
        }
    }

    private void checkParser() {
        if (parser == null) {
            throw new IllegalStateException(Messages.getString("swing.9D")); //$NON-NLS-1$
        }
    }

    private void checkLeaf(final Element elem) {
        if (elem.isLeaf()) {
            throw new IllegalArgumentException(Messages.getString("swing.9E")); //$NON-NLS-1$
        }
    }
    
    private int getElementIndex(final BranchElement branch, final Element child) {
        final int numChildren = branch.getElementCount();
        for (int i = 0; i < numChildren; i++) {
            if (branch.getElement(i) == child) {
                return i;
            }
        }
        return -1;
    }
    
    private Element getInsertionStartElement(final int offset) {
        Element result = getDefaultRootElement();
        do {
            result = result.getElement(result.getElementIndex(offset));
        } while (!result.isLeaf());
        
        return result;
    }

    private int getAncestorDepth(final Element elem, final Element child) {
        int depth = 0;
        Element parent = child;
        while (parent != null) {
            if (elem == parent) {
                return depth;
            }
            depth++;
            parent = parent.getParentElement();
        }
        return -1;
    }
    
    private static void initDefaultCharacterAttributes() {
        addDefaultCSSAttribute(Tag.B, CSS.Attribute.FONT_WEIGHT, "bold");
        addDefaultCSSAttribute(Tag.I, CSS.Attribute.FONT_STYLE, "italic");
        addDefaultCSSAttribute(Tag.STRIKE, CSS.Attribute.TEXT_DECORATION, "line-through");
        addDefaultCSSAttribute(Tag.SUB, CSS.Attribute.VERTICAL_ALIGN, "sub");
        addDefaultCSSAttribute(Tag.SUP, CSS.Attribute.VERTICAL_ALIGN, "super");
        addDefaultCSSAttribute(Tag.U, CSS.Attribute.TEXT_DECORATION, "underline");
        addDefaultCSSAttribute(Tag.PRE, CSS.Attribute.WHITE_SPACE, "pre");
    }

    private static void addDefaultCSSAttribute(final Tag tag, final CSS.Attribute attr, final String value) {
        SimpleAttributeSet attrSet = new SimpleAttributeSet();
        attrSet.addAttribute(attr, attr.getConverter().toCSS(value));
        DEFAULT_CHARACTER_ATTRIBUTES.addAttribute(tag, attrSet);
    }

    private static SimpleAttributeSet getDefaultCSSAttributes(final Tag tag) {
        return (SimpleAttributeSet)DEFAULT_CHARACTER_ATTRIBUTES.getAttribute(tag);
    }
}
