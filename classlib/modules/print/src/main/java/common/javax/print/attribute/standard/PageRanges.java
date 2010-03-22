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

package javax.print.attribute.standard;

import javax.print.attribute.Attribute;
import javax.print.attribute.DocAttribute;
import javax.print.attribute.PrintJobAttribute;
import javax.print.attribute.PrintRequestAttribute;
import javax.print.attribute.SetOfIntegerSyntax;

public final class PageRanges extends SetOfIntegerSyntax implements DocAttribute,
        PrintJobAttribute, PrintRequestAttribute {
    private static final long serialVersionUID = 8639895197656148392L;

    public PageRanges(int value) {
        super(value);
        if (value < 1) {
            throw new IllegalArgumentException("Value" + value + "is less than 1");
        }
    }

    public PageRanges(int lowerBound, int upperBound) {
        super(lowerBound, upperBound);
        if (lowerBound > upperBound) {
            throw new IllegalArgumentException("Null range: lowerBound " + "> upperBound");
        } else if (lowerBound < 1) {
            throw new IllegalArgumentException("Lower bound " + lowerBound + " is less than 1");
        }
    }

    public PageRanges(int[][] members) {
        super(members);
        if (members == null) {
            throw new NullPointerException("Null int[][] parameter");
        }
        precisionCheck();
    }

    public PageRanges(String string) {
        super(string);
        if (string == null) {
            throw new NullPointerException("Null string parameter");
        }
        precisionCheck();
    }

    private void precisionCheck() {
        int[][] canonicalArray = getMembers();
        if (canonicalArray.length == 0) {
            throw new IllegalArgumentException("Zero-length array");
        }
        for (int i = 0; i < canonicalArray.length; i++) {
            if (canonicalArray[i][0] < 1) {
                throw new IllegalArgumentException("Valid values are not " + "less than 1");
            }
        }
    }

    @Override
    public boolean equals(Object object) {
        if (!(object instanceof PageRanges)) {
            return false;
        }
        return super.equals(object);
    }

    public Class<? extends Attribute> getCategory() {
        return PageRanges.class;
    }

    public String getName() {
        return "page-ranges";
    }
}
