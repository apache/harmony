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
import javax.print.attribute.SetOfIntegerSyntax;
import javax.print.attribute.SupportedValuesAttribute;

import org.apache.harmony.print.internal.nls.Messages;

public final class JobKOctetsSupported extends SetOfIntegerSyntax implements
        SupportedValuesAttribute {
    private static final long serialVersionUID = -2867871140549897443L;

    public JobKOctetsSupported(int lowerBound, int upperBound) {
        super(lowerBound, upperBound);
        if (lowerBound > upperBound) {
		    //print.1D= Null range: lowerBound > upperBound
            throw new IllegalArgumentException(Messages.getString("print.1D")); //$NON-NLS-1$        
        } else if (lowerBound < 0) {
            //print.1E=Lower bound {0} is less than 0
            throw new IllegalArgumentException(Messages.getString("print.1E", lowerBound)); //$NON-NLs-1$        
        }
    }

    @Override
    public boolean equals(Object object) {
        if (!(object instanceof JobKOctetsSupported)) {
            return false;
        }
        return super.equals(object);
    }

    public Class<? extends Attribute> getCategory() {
        return JobKOctetsSupported.class;
    }

    public String getName() {
        return "job-k-octets-supported";
    }
}
