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
 * @author Oleg V. Khaschansky
 */

package java.awt.font;


import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.geom.GeneralPath;
import java.text.AttributedCharacterIterator;
import java.text.AttributedString;
import java.util.Map;

import org.apache.harmony.awt.gl.font.BasicMetrics;
import org.apache.harmony.awt.gl.font.CaretManager;
import org.apache.harmony.awt.gl.font.TextMetricsCalculator;
import org.apache.harmony.awt.gl.font.TextRunBreaker;
import org.apache.harmony.awt.internal.nls.Messages;

public final class TextLayout implements Cloneable {

    public static class CaretPolicy {

        public CaretPolicy() {
            // Nothing to do
        }

        public TextHitInfo getStrongCaret(TextHitInfo hit1, TextHitInfo hit2, TextLayout layout) {
            // Stronger hit is the one with greater level.
            // If the level is same, leading edge is stronger.

            int level1 = layout.getCharacterLevel(hit1.getCharIndex());
            int level2 = layout.getCharacterLevel(hit2.getCharIndex());

            if (level1 == level2) {
                return (hit2.isLeadingEdge() && (!hit1.isLeadingEdge())) ? hit2 : hit1;
            }
            return level1 > level2 ? hit1 : hit2;
        }

    }

    public static final TextLayout.CaretPolicy DEFAULT_CARET_POLICY = new CaretPolicy();

    private TextRunBreaker breaker;
    private boolean metricsValid = false;
    private TextMetricsCalculator tmc;
    private BasicMetrics metrics;
    private CaretManager caretManager;
    float justificationWidth = -1;

    public TextLayout(String string, Font font, FontRenderContext frc) {
        if (string == null){
            // awt.01='{0}' parameter is null
            throw new IllegalArgumentException(Messages.getString("awt.01", "string")); //$NON-NLS-1$ //$NON-NLS-2$
        }
        
        if (font == null){
            // awt.01='{0}' parameter is null
            throw new IllegalArgumentException(Messages.getString("awt.01", "font")); //$NON-NLS-1$ //$NON-NLS-2$
        }

        if (string.length() == 0){
            // awt.02='{0}' parameter has zero length
            throw new IllegalArgumentException(Messages.getString("awt.02", "string")); //$NON-NLS-1$ //$NON-NLS-2$
        }

        AttributedString as = new AttributedString(string);
        as.addAttribute(TextAttribute.FONT, font);
        this.breaker = new TextRunBreaker(as.getIterator(), frc);
        caretManager = new CaretManager(breaker);
    }

    public TextLayout(
            String string,
            Map<? extends java.text.AttributedCharacterIterator.Attribute, ?> attributes,
            FontRenderContext frc ) {
        if (string == null){
            // awt.01='{0}' parameter is null
            throw new IllegalArgumentException(Messages.getString("awt.01", "string")); //$NON-NLS-1$ //$NON-NLS-2$
        }
        
        if (attributes == null){
            // awt.01='{0}' parameter is null
            throw new IllegalArgumentException(Messages.getString("awt.01", "attributes")); //$NON-NLS-1$ //$NON-NLS-2$
        }
        
        if (string.length() == 0){
            // awt.02='{0}' parameter has zero length
            throw new IllegalArgumentException(Messages.getString("awt.02", "string")); //$NON-NLS-1$ //$NON-NLS-2$
        }
        
        
        AttributedString as = new AttributedString(string);
        as.addAttributes(attributes, 0, string.length());
        this.breaker = new TextRunBreaker(as.getIterator(), frc);
        caretManager = new CaretManager(breaker);
    }

    public TextLayout(AttributedCharacterIterator text, FontRenderContext frc) {
        if (text == null){
            // awt.03='{0}' iterator parameter is null
            throw new IllegalArgumentException(Messages.getString("awt.03", "text")); //$NON-NLS-1$ //$NON-NLS-2$
        }
        
        if (text.getBeginIndex() == text.getEndIndex()){
            // awt.04='{0}' iterator parameter has zero length
            throw new IllegalArgumentException(Messages.getString("awt.04", "text")); //$NON-NLS-1$ //$NON-NLS-2$
        }

        this.breaker = new TextRunBreaker(text, frc);
        caretManager = new CaretManager(breaker);
    }

    TextLayout(TextRunBreaker breaker) {
        this.breaker = breaker;
        caretManager = new CaretManager(this.breaker);
    }

    @Override
    public int hashCode() {
        return breaker.hashCode();
    }

    @Override
    protected Object clone() {
        TextLayout res = new TextLayout((TextRunBreaker) breaker.clone());

        if (justificationWidth >= 0) {
            res.handleJustify(justificationWidth);
        }

        return res;
    }

    public boolean equals(TextLayout layout) {
        if (layout == null) {
            return false;
        }
        return this.breaker.equals(layout.breaker);
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof TextLayout ? equals((TextLayout) obj) : false;
    }

    @Override
    public String toString() { // what for?
        return super.toString();
    }

    public void draw(Graphics2D g2d, float x, float y) {
        updateMetrics();
        breaker.drawSegments(g2d, x ,y);
    }

    private void updateMetrics() {
        if (!metricsValid) {
            breaker.createAllSegments();
            tmc = new TextMetricsCalculator(breaker);
            metrics = tmc.createMetrics();
            metricsValid = true;
        }
    }

    public float getAdvance() {
        updateMetrics();
        return metrics.getAdvance();
    }

    public float getAscent() {
        updateMetrics();
        return metrics.getAscent();
    }

    public byte getBaseline() {
        updateMetrics();
        return (byte) metrics.getBaseLineIndex();
    }

    public float[] getBaselineOffsets() {
        updateMetrics();
        return tmc.getBaselineOffsets();
    }

    public Shape getBlackBoxBounds(int firstEndpoint, int secondEndpoint) {
        updateMetrics();
        if (firstEndpoint < secondEndpoint) {
            return breaker.getBlackBoxBounds(firstEndpoint, secondEndpoint);
        }
        return breaker.getBlackBoxBounds(secondEndpoint, firstEndpoint);
    }

    public Rectangle2D getBounds() {
        updateMetrics();
        return breaker.getVisualBounds();
    }

    public float[] getCaretInfo(TextHitInfo hitInfo) {
        updateMetrics();
        return caretManager.getCaretInfo(hitInfo);
    }

    public float[] getCaretInfo(TextHitInfo hitInfo, Rectangle2D bounds) {
        updateMetrics();
        return caretManager.getCaretInfo(hitInfo);
    }

    public Shape getCaretShape(TextHitInfo hitInfo, Rectangle2D bounds) {
        updateMetrics();
        return caretManager.getCaretShape(hitInfo, this);
    }

    public Shape getCaretShape(TextHitInfo hitInfo) {
        updateMetrics();
        return caretManager.getCaretShape(hitInfo, this);
    }

    public Shape[] getCaretShapes(int offset) {
        return getCaretShapes(offset, null, TextLayout.DEFAULT_CARET_POLICY);
    }

    public Shape[] getCaretShapes(int offset, Rectangle2D bounds) {
        return getCaretShapes(offset, bounds, TextLayout.DEFAULT_CARET_POLICY);
    }

    public Shape[] getCaretShapes(int offset, Rectangle2D bounds, TextLayout.CaretPolicy policy) {
        if (offset < 0 || offset > breaker.getCharCount()) {
            // awt.195=Offset is out of bounds
            throw new IllegalArgumentException(Messages.getString("awt.195")); //$NON-NLS-1$
        }

        updateMetrics();
        return caretManager.getCaretShapes(offset, bounds, policy, this);
    }

    public int getCharacterCount() {
        return breaker.getCharCount();
    }

    public byte getCharacterLevel(int index) {
        if (index == -1 || index == getCharacterCount()) {
            return (byte) breaker.getBaseLevel();
        }
        return breaker.getLevel(index);
    }

    public float getDescent() {
        updateMetrics();
        return metrics.getDescent();
    }

    public TextLayout getJustifiedLayout(float justificationWidth) throws Error {
        float justification = breaker.getJustification();

        if (justification < 0) {
            // awt.196=Justification impossible, layout already justified
            throw new Error(Messages.getString("awt.196")); //$NON-NLS-1$
        } else if (justification == 0) {
            return this;
        }

        TextLayout justifiedLayout = new TextLayout((TextRunBreaker) breaker.clone());
        justifiedLayout.handleJustify(justificationWidth);
        return justifiedLayout;
    }

    public float getLeading() {
        updateMetrics();
        return metrics.getLeading();
    }

    public Shape getLogicalHighlightShape(int firstEndpoint, int secondEndpoint) {
        updateMetrics();
        return getLogicalHighlightShape(firstEndpoint, secondEndpoint, breaker.getLogicalBounds());
    }

    public Shape getLogicalHighlightShape(
            int firstEndpoint,
            int secondEndpoint,
            Rectangle2D bounds
    ) {
        updateMetrics();

        if (firstEndpoint > secondEndpoint) {
            if (secondEndpoint < 0 || firstEndpoint > breaker.getCharCount()) {
                // awt.197=Endpoints are out of range
                throw new IllegalArgumentException(Messages.getString("awt.197")); //$NON-NLS-1$
            }
            return caretManager.getLogicalHighlightShape(
                    secondEndpoint,
                    firstEndpoint,
                    bounds,
                    this
            );
        }
        if (firstEndpoint < 0 || secondEndpoint > breaker.getCharCount()) {
            // awt.197=Endpoints are out of range
            throw new IllegalArgumentException(Messages.getString("awt.197")); //$NON-NLS-1$
        }
        return caretManager.getLogicalHighlightShape(
                firstEndpoint,
                secondEndpoint,
                bounds,
                this
        );
    }

    public int[] getLogicalRangesForVisualSelection(TextHitInfo hit1, TextHitInfo hit2) {
        return caretManager.getLogicalRangesForVisualSelection(hit1, hit2);
    }

    public TextHitInfo getNextLeftHit(int offset) {
        return getNextLeftHit(offset, DEFAULT_CARET_POLICY);
    }

    public TextHitInfo getNextLeftHit(TextHitInfo hitInfo) {
        breaker.createAllSegments();
        return caretManager.getNextLeftHit(hitInfo);
    }

    public TextHitInfo getNextLeftHit(int offset, TextLayout.CaretPolicy policy) {
        if (offset < 0 || offset > breaker.getCharCount()) {
            // awt.195=Offset is out of bounds
            throw new IllegalArgumentException(Messages.getString("awt.195")); //$NON-NLS-1$
        }

        TextHitInfo hit = TextHitInfo.afterOffset(offset);
        TextHitInfo strongHit = policy.getStrongCaret(hit, hit.getOtherHit(), this);
        TextHitInfo nextLeftHit = getNextLeftHit(strongHit);

        if (nextLeftHit != null) {
            return policy.getStrongCaret(getVisualOtherHit(nextLeftHit), nextLeftHit, this);
        }
        return null;
    }

    public TextHitInfo getNextRightHit(TextHitInfo hitInfo) {
        breaker.createAllSegments();
        return caretManager.getNextRightHit(hitInfo);
    }

    public TextHitInfo getNextRightHit(int offset) {
        return getNextRightHit(offset, DEFAULT_CARET_POLICY);
    }

    public TextHitInfo getNextRightHit(int offset, TextLayout.CaretPolicy policy) {
        if (offset < 0 || offset > breaker.getCharCount()) {
            // awt.195=Offset is out of bounds
            throw new IllegalArgumentException(Messages.getString("awt.195")); //$NON-NLS-1$
        }

        TextHitInfo hit = TextHitInfo.afterOffset(offset);
        TextHitInfo strongHit = policy.getStrongCaret(hit, hit.getOtherHit(), this);
        TextHitInfo nextRightHit = getNextRightHit(strongHit);

        if (nextRightHit != null) {
            return policy.getStrongCaret(getVisualOtherHit(nextRightHit), nextRightHit, this);
        }
        return null;
    }

    public Shape getOutline(AffineTransform xform) {
        breaker.createAllSegments();

        GeneralPath outline = breaker.getOutline();

        if (outline != null && xform != null) {
            outline.transform(xform);
        }

        return outline;
    }

    public float getVisibleAdvance() {
        updateMetrics();

        // Trailing whitespace _SHOULD_ be reordered (Unicode spec) to
        // base direction, so it is also trailing
        // in logical representation. We use this fact.
        int lastNonWhitespace = breaker.getLastNonWhitespace();

        if (lastNonWhitespace < 0) {
            return 0;
        } else if (lastNonWhitespace == getCharacterCount()-1) {
            return getAdvance();
        } else if (justificationWidth >= 0) { // Layout is justified
            return justificationWidth;
        } else {
            breaker.pushSegments(
                    breaker.getACI().getBeginIndex(),
                    lastNonWhitespace + breaker.getACI().getBeginIndex() + 1
            );

            breaker.createAllSegments();

            float visAdvance = tmc.createMetrics().getAdvance();

            breaker.popSegments();
            return visAdvance;
        }
    }

    public Shape getVisualHighlightShape(TextHitInfo hit1, TextHitInfo hit2, Rectangle2D bounds) {
        return caretManager.getVisualHighlightShape(hit1, hit2, bounds, this);
    }

    public Shape getVisualHighlightShape(TextHitInfo hit1, TextHitInfo hit2) {
        breaker.createAllSegments();
        return caretManager.getVisualHighlightShape(hit1, hit2, breaker.getLogicalBounds(), this);
    }

    public TextHitInfo getVisualOtherHit(TextHitInfo hitInfo) {
        return caretManager.getVisualOtherHit(hitInfo);
    }

    protected void handleJustify(float justificationWidth) {
        float justification = breaker.getJustification();

        if (justification < 0) {
            // awt.196=Justification impossible, layout already justified
            throw new IllegalStateException(Messages.getString("awt.196")); //$NON-NLS-1$
        } else if (justification == 0) {
            return;
        }

        float gap = (justificationWidth - getVisibleAdvance()) * justification;
        breaker.justify(gap);
        this.justificationWidth = justificationWidth;

        // Correct metrics
        tmc = new TextMetricsCalculator(breaker);
        tmc.correctAdvance(metrics);
    }

    public TextHitInfo hitTestChar(float x, float y) {
        return hitTestChar(x, y, getBounds());
    }

    public TextHitInfo hitTestChar(float x, float y, Rectangle2D bounds) {
        if (x > bounds.getMaxX()) {
            return breaker.isLTR() ?
                    TextHitInfo.trailing(breaker.getCharCount() - 1) : TextHitInfo.leading(0);
        }

        if (x < bounds.getMinX()) {
            return breaker.isLTR() ?
                    TextHitInfo.leading(0) : TextHitInfo.trailing(breaker.getCharCount() - 1);
        }

        return breaker.hitTest(x, y);
    }

    public boolean isLeftToRight() {
        return breaker.isLTR();
    }

    public boolean isVertical() {
        return false;
    }
}

