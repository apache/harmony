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
package javax.swing.text;

import java.text.ParseException;
import java.util.BitSet;

import javax.swing.JFormattedTextField;
import javax.swing.SwingConstants;
import javax.swing.text.DocumentFilter.FilterBypass;
import javax.swing.text.Position.Bias;

import org.apache.harmony.x.swing.internal.nls.Messages;


public class MaskFormatter extends DefaultFormatter {
    private String mask;
    private String placeholder;
    private char placeHolderCharacter = ' ';
    private String validCharacters;
    private String invalidCharacters;
    private boolean valueContainsLiteralCharacters = true;
    private static final String nonLiteralCharacters = "#ULA?*H'";
    private static final String hexLetters = "abcdef";
    private BitSet literalMask;
    private BitSet escapeMask;
    private static final char ESC = '\'';
    private NavigationFilter navigationFilter;

    private class NavigationFilterImpl extends NavigationFilter {
        public int getNextVisualPositionFrom(final JTextComponent c,
                                             final int pos,
                                             final Bias bias,
                                             final int direction,
                                             final Bias[] biasRet)
                throws BadLocationException {
            int result = pos;
            biasRet[0] = bias;
            do {
               result = super.getNextVisualPositionFrom(c, result, biasRet[0],
                                                        direction, biasRet);
            } while (literalMask.get(result)
                    && !(result == 0 && direction == SwingConstants.EAST)
                    && !(result == c.getDocument().getLength()
                            && direction == SwingConstants.WEST));
            return result;
        }

        public void moveDot(final NavigationFilter.FilterBypass filterBypass,
                            final int dot,
                            final Bias bias) {
            super.moveDot(filterBypass, dot, bias);
        }

        public void setDot(final NavigationFilter.FilterBypass filterBypass,
                           final int dot,
                           final Bias bias) {
            super.setDot(filterBypass, dot, bias);
        }
    }

    public MaskFormatter(final String mask) throws ParseException {
        setAllowsInvalid(false);
        setMask(mask);
    }

    public MaskFormatter() {
        setAllowsInvalid(false);
    }

    public String getMask() {
        return mask;
    }

    protected NavigationFilter getNavigationFilter() {
        if (navigationFilter == null) {
            navigationFilter = new NavigationFilterImpl();
        }
        return navigationFilter;
    }

    public String getInvalidCharacters() {
        return invalidCharacters;
    }

    public String getPlaceholder() {
        return placeholder;
    }

    public char getPlaceholderCharacter() {
        return placeHolderCharacter;
    }

    public String getValidCharacters() {
        return validCharacters;
    }

    public boolean getValueContainsLiteralCharacters() {
        return valueContainsLiteralCharacters;
    }

    public void install(final JFormattedTextField ftf) {
        boolean oldValue = getAllowsInvalid();
        setAllowsInvalid(true);
        super.install(ftf);
        setAllowsInvalid(oldValue);
    }

    public void setInvalidCharacters(final String invalidCharacters) {
        this.invalidCharacters = invalidCharacters;
    }

    public void setMask(final String mask) throws ParseException {
        this.mask = mask;
        buildMasks();
    }

    private void buildMasks() {
        prepareMasks();
        int index = -1;
        int i = 0;
        while (++index < mask.length()) {
            boolean result = true;
            char maskCharacter = mask.charAt(index);
            boolean isEscapeCharacter = maskCharacter == ESC;

            if (isEscapeCharacter) {
                    result = true;
                    index++;
            } else {
                result = nonLiteralCharacters.indexOf(maskCharacter) < 0;
            }
            literalMask.set(i, result);
            escapeMask.set(i++, result && isEscapeCharacter);
        }
    }

    private void prepareMasks() {
        literalMask = initMask(literalMask);
        escapeMask = initMask(escapeMask);
    }

    private BitSet initMask(final BitSet set) {
        BitSet result = set;
        if (set == null) {
            result = new BitSet();
        } else  {
            result.clear();
        }
        return result;
    }

    public void setPlaceholder(final String placeholder) {
        this.placeholder = placeholder;
    }

    public void setPlaceholderCharacter(final char placeholderCharacter) {
        this.placeHolderCharacter = placeholderCharacter;

    }

    public void setValidCharacters(final String validCharacters) {
        this.validCharacters = validCharacters;
    }

    public void
        setValueContainsLiteralCharacters(final boolean
                                          valueContainsLiteralCharacters) {
        this.valueContainsLiteralCharacters = valueContainsLiteralCharacters;
    }

    public Object stringToValue(final String string) throws ParseException {
        String parsedString = mask == null ? string : parseString(string);
        return super.stringToValue(parsedString);
    }

    public String valueToString(final Object value) throws ParseException {
        if ((mask == null) && (escapeMask == null) && (literalMask == null)) {
            return "";
        }
        String result = super.valueToString(value);
        result = fillAll(result);
        result = fillByPlaceholder(result);
        result = fillByPlaceHolderCharacter(result);
        return result;
    }

    private String fillAll(final String string) throws ParseException {
        String result = "";
        int length = string.length();
        int maskLength = mask.length();
        int escapeLength = escapeMask.cardinality();
        int literalsLength = literalMask.cardinality();

        if (maskLength - escapeLength
                - (valueContainsLiteralCharacters ? 0 : literalsLength)
                < length) {
            throw new ParseException(Messages.getString("swing.92"), 0); //$NON-NLS-1$
        }
        int index = 0;
        int maskIndex = 0;
        int indexInString = 0;
        char ch;
        while (indexInString < length) {
            maskIndex = getNextMaskIndex(index, maskIndex);
            char maskCharacter = mask.charAt(maskIndex);
            boolean isLiteral = literalMask.get(index);
            if (valueContainsLiteralCharacters || !isLiteral) {
                ch = string.charAt(indexInString);
                indexInString++;
            } else {
                ch = maskCharacter;
            }
            result += ch;

            checkCharacter(maskCharacter, ch, isLiteral, index);
            index++;
            maskIndex++;
        }
        return result;
    }

    private void checkCharacter(final char maskCharacter,
                                final char textCharacter,
                                final boolean isLiteral,
                                final int index) throws ParseException {
        if (!acceptCharacter(maskCharacter,
                             textCharacter,
                             isLiteral)) {
            throw new ParseException(Messages.getString("swing.93"), index); //$NON-NLS-1$
        }
    }


    private String fillByPlaceholder(final String string) {
        int length = string.length();
        if (length >= mask.length() || placeholder == null
                || placeholder.length() < length) {
            return string;
        } else {
            return string + getPlaceHolderSubstring(length);
        }
    }

    private String getPlaceHolderSubstring(final int start) {
        String result = "";
        int maskIndex = getIndexInMask(start);
        for (int i = start; i < Math.min(placeholder.length(),
                                         mask.length()
                                         - escapeMask.cardinality());
           i++, maskIndex++) {
           maskIndex = getNextMaskIndex(i, maskIndex);
           result += literalMask.get(i) ? mask.charAt(maskIndex)
                   : placeholder.charAt(i);
        }
        return result;
    }

    private String fillByPlaceHolderCharacter(final String string) {
        int start = string.length();
        int maskLength = mask.length();
        String result = "";
        int maskIndex = getIndexInMask(start);
        for (int i = start; i < maskLength - escapeMask.cardinality();
            maskIndex++, i++) {
            maskIndex = getNextMaskIndex(i, maskIndex);
            result += literalMask.get(i) ? mask.charAt(maskIndex)
                    : placeHolderCharacter;
        }

        return string + result;
    }

    private int getIndexInMask(final int startIndex) {
        return escapeMask.get(0, startIndex).cardinality() + startIndex;
    }

    private int getNextMaskIndex(final int index, final int prevIndexInMask) {
        return escapeMask.get(index) ? prevIndexInMask + 1 : prevIndexInMask;
    }

    private String parseString(final String string) throws ParseException {
        int length = string.length();
        int maskLength = mask.length();
        int index = 0;
        if (maskLength - length != escapeMask.cardinality()) {
            throw new ParseException(Messages.getString("swing.94"), 0); //$NON-NLS-1$
        }
        int accumulator = 0;
        String result = "";
        while (index < length) {
            char ch = string.charAt(index);
            accumulator = getNextMaskIndex(index, accumulator);
            char maskCharacter = mask.charAt(accumulator);
            boolean isLiteral = literalMask.get(index);

            if (!isLiteral || valueContainsLiteralCharacters) {
                result += ch;
            }
            checkCharacter(maskCharacter, ch, isLiteral, index);
            index++;
            accumulator++;
        }
        return result;
    }

    private boolean checkString(final int offset,
                                final String string) {
        int length = string.length();
        int index = offset;
        int accumulator = getIndexInMask(index);
        if (length + offset > literalMask.size()) {
            return false;
        }
        while (index < length + offset) {
            char ch = string.charAt(index - offset);
            accumulator = getNextMaskIndex(index, accumulator);
            if (accumulator >= mask.length()) {
                return false;
            }
            char maskCharacter = mask.charAt(accumulator);
            boolean isLiteral = literalMask.get(index);
            try {
                checkCharacter(maskCharacter, ch, isLiteral, index);
            } catch (ParseException e) {
                return false;
            }
            index++;
            accumulator++;
        }
        return true;
    }

    private boolean isValid(final char ch) {
        boolean isValidCharacter = validCharacters == null
                                   || validCharacters.indexOf(ch) >= 0;
        boolean isNotInvalid = invalidCharacters == null
                                   || invalidCharacters.indexOf(ch) < 0;
        return isValidCharacter && isNotInvalid;
    }

    private boolean acceptCharacter(final char maskCharacter,
                                    final char ch,
                                    final boolean compareLiterals) {
        if (compareLiterals) {
            return acceptCharacter(maskCharacter, ch);
        }
        switch (maskCharacter) {
        case '#':
            return Character.isDigit(ch) && isValid(ch);
        case 'U':
            return Character.isLetter(ch) && isValid(ch);
        case 'L':
            return Character.isLetter(ch) && isValid(ch);
        case 'A':
            return (Character.isLetter(ch) || Character.isDigit(ch))
               && isValid(ch);
        case '?':
            return Character.isLetter(ch) && isValid(ch);
        case '*':
            return isValid(ch);
        case 'H':
            char chToL = Character.toLowerCase(ch);
            return Character.isDigit(ch) || hexLetters.indexOf(chToL) >= 0
                   && isValid(ch);
        default:
            return acceptCharacter(maskCharacter, ch);
        }
    }

    private boolean acceptCharacter(final char maskCharacter,
                                    final char ch) {
        return maskCharacter == ch && isValid(ch);
    }

    final void replaceImpl(final FilterBypass filterBypass,
                           final int offset, final int length,
                           final String text, final AttributeSet attrs)
            throws BadLocationException {
        if (getAllowsInvalid()) {
            filterBypass.replace(offset, length, text, attrs);
        } else if (checkString(offset, text)) {
            String text1 = addLiteralSympolsToString(offset, text);
            filterBypass.replace(offset, text1.length(), text1, null);
        }
    }

    //This method will not be called to get valid text (as text length will be
    //increased). So if getAllows invalid return false it does nothing
    final void insertStringImpl(final FilterBypass filterBypass,
                                final int offset, final String string,
                                final AttributeSet attrs)
        throws BadLocationException {
        if (getAllowsInvalid()) {
            filterBypass.insertString(offset, string, attrs);
        }
    }

    void removeImpl(final FilterBypass filterBypass, final int offset,
                    final int length)
            throws BadLocationException {
        if (getAllowsInvalid()) {
            filterBypass.remove(offset, length);
        } else {
            String text = getSubstring(offset,
                                       getMaxLengthToRemove(filterBypass,
                                                            offset, length));
            filterBypass.replace(offset, text.length(), text, null);
            JFormattedTextField textField = getFormattedTextField();
            //perhaps it is temporary solution
            textField.setCaretPosition(offset);
        }
    }

    private String getSubstring(final int offset, final int length) {
        int start = offset;
        String result = "";
        int maskIndex = getIndexInMask(start);
        int literalsNumber = literalMask.get(offset, offset + length)
                    .cardinality();
        int escapeNumber = escapeMask.get(offset, offset + length)
                    .cardinality();
        for (int i = start; i < start + length + literalsNumber + escapeNumber;
            maskIndex++, i++) {
            maskIndex = getNextMaskIndex(i, maskIndex);
            result += literalMask.get(i) ? mask.charAt(maskIndex)
                    : placeHolderCharacter;
        }
        return result;
    }

    private String addLiteralSympolsToString(final int offset,
                                               final String text) {
          String result = text;
          int index = offset + text.length();
          int maskIndex = getIndexInMask(index);
          while (literalMask.get(index) && maskIndex < mask.length()) {
              maskIndex = getNextMaskIndex(index, maskIndex);
              result += mask.charAt(maskIndex);
              index++;
              maskIndex++;
          }
          return result;
      }
}


