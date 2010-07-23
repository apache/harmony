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

import org.apache.harmony.print.internal.nls.Messages;

public final class PageRanges extends SetOfIntegerSyntax implements DocAttribute,
        PrintJobAttribute, PrintRequestAttribute {
    private static final long serialVersionUID = 8639895197656148392L;

    public PageRanges(int value) {
        super(value);
        if (value < 1) {
            //print.1A= Value {0} is less than 1
            throw new IllegalArgumentException(Messages.getString("print.1A", value)); //NON-NLS-1$        
        }
    }

    public PageRanges(int lowerBound, int upperBound) {
        super(lowerBound, upperBound);
        if (lowerBound > upperBound) {
            //print.1D= Null range: lowerBound > upperBound
            throw new IllegalArgumentException(Messages.getString("print.1D")); //$NON-NLS-1$        
        } else if (lowerBound < 1) {
             //print.1C= Lower bound {0} is less than 1
            throw new IllegalArgumentException(Messages.getString("print.1C", lowerBound)); //$NON-NLS-1$        
        }
    }

    public PageRanges(int[][] members) {
        super(members);
        if (members == null) {
             //print.22=Null int [][] parameter
            throw new NullPointerException(Messages.getString("print.22")); //$NON-NLS-1$        
        }
        precisionCheck();
    }

    public PageRanges(String string) {
        super(string);
        if (string == null) {
            //print.25=Null string parameter
            throw new NullPointerException(Messages.getString("print.25")); //$NON-NLs-1$            
        }
        precisionCheck();
    }

    private void precisionCheck() {
        int[][] canonicalArray = getMembers();
        if (canonicalArray.length == 0) {
            //print.23=Zero-length array
            throw new IllegalArgumentException(Messages.getString("print.23")); //$NON-NLS-1$        
        }
        for (int i = 0; i < canonicalArray.length; i++) {
            if (canonicalArray[i][0] < 1) {
                //print.24= valid values are not less than 1
                throw new IllegalArgumentException(Messages.getString("print.24")); //$NON-NLs-1$           
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
