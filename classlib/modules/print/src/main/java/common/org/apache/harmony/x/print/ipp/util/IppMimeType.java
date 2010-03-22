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
package org.apache.harmony.x.print.ipp.util;

import org.apache.harmony.x.print.MimeType;

public class IppMimeType extends MimeType {

    private static final long serialVersionUID = 1492779006204043813L;
    
    /*
     * @param mimeType
     */
    public IppMimeType(String mimeType) {
        super(mimeType);
    }

    /*
     * returns IPP/CUPS specific for MimeType object.
     */
    public String getIppSpecificForm() {
        StringBuilder s = new StringBuilder();
        s.append(getType());
        s.append("/");
        s.append(getSubtype());
        for (int i = 0; i < getParams().length; i++) {
            s.append("; ");
            s.append(getParams()[i][0]);
            if (getParams()[i][0].equalsIgnoreCase("charset")) {
                s.append("=");
                s.append(getParams()[i][1]);
            } else {
                s.append("=\"");
                s.append(getParams()[i][1]);
                s.append("\"");
            }
        }
        return s.toString();
    }

}
