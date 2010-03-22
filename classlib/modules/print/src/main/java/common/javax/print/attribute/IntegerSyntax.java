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

package javax.print.attribute;

import java.io.Serializable;

public abstract class IntegerSyntax implements Cloneable, Serializable {
    private static final long serialVersionUID = 3644574816328081943L;
    
    private final int value;

    protected IntegerSyntax(int intValue) {
        super();
        value = intValue;
    }

    protected IntegerSyntax(int intValue, int lowerBound, int upperBound) {
        super();
        if ((intValue < lowerBound) || (intValue > upperBound)) {
            throw new IllegalArgumentException("Value " + intValue + " not in valid range ("
                    + lowerBound + "," + upperBound + ")");
        }
        value = intValue;
    }

    public int getValue() {
        return value;
    }

    @Override
    public boolean equals(Object object) {
        if ((object instanceof IntegerSyntax) && value == ((IntegerSyntax) object).value) {
            return true;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return value;
    }

    @Override
    public String toString() {
        return Integer.toString(value);
    }
}
