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

package org.apache.harmony.x.swing.text.rtf;

import javax.swing.text.*;
import java.util.Stack;

/**
 * @author Aleksey Lagoshin
 */
public class DocumentRTFHandler implements RTFHandler {

  private StyledDocument sdoc;
  private Document doc;

  private int offset;

  private Stack<MutableAttributeSet> stylesStack;
  private MutableAttributeSet currentStyle;

  public DocumentRTFHandler(Document doc, int position) {
    stylesStack = new Stack<MutableAttributeSet>();
    currentStyle = new SimpleAttributeSet();

    if (doc instanceof StyledDocument)
      sdoc = (StyledDocument) doc;
    else
      this.doc = doc;

    offset = position;
  }

  public void startGroup() {
    stylesStack.push(currentStyle);
    currentStyle = new SimpleAttributeSet(currentStyle);
  }

  public void endGroup() {
    currentStyle = stylesStack.pop();
  }

  public void addText(String text) {
    try {
      if (sdoc != null)
        sdoc.insertString(offset, text, currentStyle);
      else
        doc.insertString(offset, text, null);

      offset += text.length();
    }
    catch (BadLocationException e) {
      //todo: throw it to RTFEditorKit?
    }
  }

  public void newParagraph() {
    addText("\n");
  }

  public void setBold(boolean enable) {
    StyleConstants.setBold(currentStyle, enable);
  }

  public void setItalic(boolean enable) {
    StyleConstants.setItalic(currentStyle, enable);
  }

  public void setUnderline(boolean enable) {
    StyleConstants.setUnderline(currentStyle, enable);
  }

}


