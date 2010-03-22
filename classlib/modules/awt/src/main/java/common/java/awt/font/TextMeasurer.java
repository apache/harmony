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
/*
 * @author Oleg V. Khaschansky
 */

package java.awt.font;


import java.text.AttributedCharacterIterator;

import org.apache.harmony.awt.gl.font.TextMetricsCalculator;
import org.apache.harmony.awt.gl.font.TextRunBreaker;

public final class TextMeasurer implements Cloneable {
    AttributedCharacterIterator aci;
    FontRenderContext frc;
    TextRunBreaker breaker = null;
    TextMetricsCalculator tmc = null;

    public TextMeasurer(AttributedCharacterIterator text, FontRenderContext frc) {
        this.aci = text;
        this.frc = frc;
        breaker = new TextRunBreaker(aci, this.frc);
        tmc = new TextMetricsCalculator(breaker);
    }

    public void insertChar(AttributedCharacterIterator newParagraph, int insertPos) {
        AttributedCharacterIterator oldAci = aci;
        aci = newParagraph;
        if ((oldAci.getEndIndex() - oldAci.getBeginIndex()) -
           (aci.getEndIndex() - aci.getBeginIndex()) != -1) {
            breaker = new TextRunBreaker(aci, this.frc);
            tmc = new TextMetricsCalculator(breaker);
        } else {
            breaker.insertChar(newParagraph, insertPos);
        }
    }

    public void deleteChar(AttributedCharacterIterator newParagraph, int deletePos) {
        AttributedCharacterIterator oldAci = aci;
        aci = newParagraph;
        if ((oldAci.getEndIndex() - oldAci.getBeginIndex()) -
           (aci.getEndIndex() - aci.getBeginIndex()) != 1) {
            breaker = new TextRunBreaker(aci, this.frc);
            tmc = new TextMetricsCalculator(breaker);
        } else {
            breaker.deleteChar(newParagraph, deletePos);
        }
    }

    @Override
    protected Object clone() {
        return new TextMeasurer((AttributedCharacterIterator) aci.clone(), frc);
    }

    public TextLayout getLayout(int start, int limit) {
        breaker.pushSegments(start - aci.getBeginIndex(), limit - aci.getBeginIndex());

        breaker.createAllSegments();
        TextLayout layout = new TextLayout((TextRunBreaker) breaker.clone());

        breaker.popSegments();
        return layout;
    }

    public float getAdvanceBetween(int start, int end) {
        breaker.pushSegments(start - aci.getBeginIndex(), end - aci.getBeginIndex());

        breaker.createAllSegments();
        float retval = tmc.createMetrics().getAdvance();

        breaker.popSegments();
        return retval;
    }

    public int getLineBreakIndex(int start, float maxAdvance) {
        breaker.createAllSegments();
        return breaker.getLineBreakIndex(
                start - aci.getBeginIndex(), maxAdvance) + aci.getBeginIndex();
    }
}

