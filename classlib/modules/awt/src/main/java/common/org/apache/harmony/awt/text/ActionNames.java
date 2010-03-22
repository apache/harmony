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
 * @author Evgeniya G. Maenkova
 */
package org.apache.harmony.awt.text;

/**
 *
 * Containts names of text actions.
 */
public interface ActionNames {
    String backwardAction = "caret-backward"; //$NON-NLS-1$

    String beepAction = "beep"; //$NON-NLS-1$

    String beginAction = "caret-begin"; //$NON-NLS-1$

    String beginLineAction = "caret-begin-line"; //$NON-NLS-1$

    String beginParagraphAction = "caret-begin-paragraph"; //$NON-NLS-1$

    String beginWordAction = "caret-begin-word"; //$NON-NLS-1$

    String copyAction = "copy-to-clipboard"; //$NON-NLS-1$

    String cutAction = "cut-to-clipboard"; //$NON-NLS-1$

    String defaultKeyTypedAction = "default-typed"; //$NON-NLS-1$

    String deleteNextCharAction = "delete-next"; //$NON-NLS-1$

    String deletePrevCharAction = "delete-previous"; //$NON-NLS-1$

    String downAction = "caret-down"; //$NON-NLS-1$

    String dumpModelAction = "dump-model"; //$NON-NLS-1$

    String endAction = "caret-end"; //$NON-NLS-1$

    String endLineAction = "caret-end-line"; //$NON-NLS-1$

    String endParagraphAction = "caret-end-paragraph"; //$NON-NLS-1$

    String endWordAction = "caret-end-word"; //$NON-NLS-1$

    String forwardAction = "caret-forward"; //$NON-NLS-1$

    String insertBreakAction = "insert-break"; //$NON-NLS-1$

    String insertContentAction = "insert-content"; //$NON-NLS-1$

    String insertTabAction = "insert-tab"; //$NON-NLS-1$

    String nextWordAction = "caret-next-word"; //$NON-NLS-1$

    String pageDownAction = "page-down"; //$NON-NLS-1$

    String pageUpAction = "page-up"; //$NON-NLS-1$

    String pasteAction = "paste-from-clipboard"; //$NON-NLS-1$

    String previousWordAction = "caret-previous-word"; //$NON-NLS-1$

    String readOnlyAction = "set-read-only"; //$NON-NLS-1$

    String selectAllAction = "select-all"; //$NON-NLS-1$

    String selectionBackwardAction = "selection-backward"; //$NON-NLS-1$

    String selectionBeginAction = "selection-begin"; //$NON-NLS-1$

    String selectionBeginLineAction = "selection-begin-line"; //$NON-NLS-1$

    String selectionBeginParagraphAction = "selection-begin-paragraph"; //$NON-NLS-1$

    String selectionBeginWordAction = "selection-begin-word"; //$NON-NLS-1$

    String selectionDownAction = "selection-down"; //$NON-NLS-1$

    String selectionEndAction = "selection-end"; //$NON-NLS-1$

    String selectionEndLineAction = "selection-end-line"; //$NON-NLS-1$

    String selectionEndParagraphAction = "selection-end-paragraph"; //$NON-NLS-1$

    String selectionEndWordAction = "selection-end-word"; //$NON-NLS-1$

    String selectionForwardAction = "selection-forward"; //$NON-NLS-1$

    String selectionNextWordAction = "selection-next-word"; //$NON-NLS-1$

    String selectionPageDownAction = "selection-page-down"; //$NON-NLS-1$

    String selectionPageLeftAction = "selection-page-left"; //$NON-NLS-1$

    String selectionPageRightAction = "selection-page-right"; //$NON-NLS-1$

    String selectionPageUpAction = "selection-page-up"; //$NON-NLS-1$

    String selectionPreviousWordAction = "selection-previous-word"; //$NON-NLS-1$

    String selectionUpAction = "selection-up"; //$NON-NLS-1$

    String selectLineAction = "select-line"; //$NON-NLS-1$

    String selectParagraphAction = "select-paragraph"; //$NON-NLS-1$

    String selectWordAction = "select-word"; //$NON-NLS-1$

    String toggleComponentOrientationAction = "toggle-componentOrientation"; //$NON-NLS-1$

    String unselectAction = "unselect"; //$NON-NLS-1$

    String upAction = "caret-up"; //$NON-NLS-1$

    String writableAction = "set-writable"; //$NON-NLS-1$

    int NONE = 0;

    int COPY = 1;

    int MOVE = 2;

    int COPY_OR_MOVE = 3;
}

