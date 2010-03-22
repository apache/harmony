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
 * @author Ilya S. Okomin
 *
 */
package org.apache.harmony.awt.gl.font;

/**
 * Windows FontProperty implementation, applicable for Windows formats of 
 * font property files. 
 */
public class WinFontProperty extends FontProperty {
    
    /** charset name that is applicable for windows font.properties */ 
    String charset;

    public WinFontProperty(String _fileName, String _name, String _charset, int _style, int[] exclusionRange, String _encoding){
        this.name = _name;
        this.charset = _charset;
        this.exclRange = exclusionRange;
        this.style = _style;
        this.fileName = _fileName;
        this.encoding = _encoding;
    }

    @Override
    public String getEncoding(){
        return this.encoding;
    }
    @Override
    public String toString(){
        return this.getClass().getName() +
                "[name=" + this.name + //$NON-NLS-1$
                ",fileName="+ this.fileName + //$NON-NLS-1$
                ",style=" + this.style + //$NON-NLS-1$
                ",Charset=" + this.charset + //$NON-NLS-1$
                ",exclRange=" + this.exclRange + //$NON-NLS-1$
                ",encoding=" + this.encoding + "]"; //$NON-NLS-1$ //$NON-NLS-2$
    }

}
