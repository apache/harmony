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

package javax.print;

import java.io.ByteArrayInputStream;
import java.io.CharArrayReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import javax.print.attribute.DocAttributeSet;

public final class SimpleDoc implements Doc {
    private Object printdata;

    private DocFlavor flavor;

    private DocAttributeSet attributes;

    private Reader reader;

    private InputStream instream;

    public SimpleDoc(Object printData, DocFlavor docflavor, DocAttributeSet docattributes) {
        /*
         * IllegalArgumentException - if flavor or printData is null, or the
         * printData does not correspond to the specified doc flavor--for
         * example, the data is not of the type specified as the representation
         * in the DocFlavor.
         */
        if (docflavor == null || printData == null) {
            throw new IllegalArgumentException("Argument is null");
        }
        try {
            Class<?> clazz = Class.forName(docflavor.getRepresentationClassName());
            if (!clazz.isInstance(printData)) {
                throw new IllegalArgumentException("");
            }
        } catch (Exception e) {
            throw new IllegalArgumentException("Wrong type of print data");
        }
        this.printdata = printData;
        this.flavor = docflavor;
        this.attributes = docattributes;
        this.reader = null;
        this.instream = null;
    }

    public DocAttributeSet getAttributes() {
        return attributes;
    }

    public DocFlavor getDocFlavor() {
        return flavor;
    }

    public Object getPrintData() throws IOException {
        return printdata;
    }

    public Reader getReaderForText() throws IOException {
        synchronized (this) {
            if (reader != null) {
                return reader;
            }
            if (printdata instanceof Reader) {
                reader = (Reader) printdata;
            } else if (printdata instanceof char[]) {
                reader = new CharArrayReader((char[]) printdata);
            } else if (printdata instanceof String) {
                reader = new StringReader((String) printdata);
            }
        }
        return reader;
    }

    public InputStream getStreamForBytes() throws IOException {
        synchronized (this) {
            if (instream != null) {
                return instream;
            }
            if (printdata instanceof InputStream) {
                instream = (InputStream) printdata;
            } else if (printdata instanceof byte[]) {
                instream = new ByteArrayInputStream((byte[]) printdata);
            }
        }
        return instream;
    }
}
