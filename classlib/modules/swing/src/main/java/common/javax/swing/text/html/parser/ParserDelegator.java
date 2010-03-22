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
package javax.swing.text.html.parser;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.Reader;
import java.io.Serializable;
import java.net.URL;

import javax.swing.text.html.HTMLEditorKit;

/**
 * This class is a wrapper of {@link DocumentParser}. <br>
 * <br>
 * Internally stores a reference to a DTD ({@link ParserDelegator#defaultDTD})
 * which is filled with <code>html32.bdtd</code>, a file located at the classpath
 * with the default DTD content (HTML 3.2) in ASN1 format. Then, instances
 * a {@link DocumentParser} with this dtd. <br>
 * <br>
 * So, when the method
 * {@link ParserDelegator#parse(Reader, javax.swing.text.html.HTMLEditorKit.ParserCallback, boolean) parse(Reader, HTMLEditorKit.ParserCallback, boolean)}
 * is invoked, its <em>"delegates"</em> to a {@link DocumentParser}.
 */
public class ParserDelegator extends HTMLEditorKit.Parser implements
        Serializable {

    /**
     * The name of the default dtd.
     */
    private static final String DEFAULT_DTD_NAME = "html32";

    /**
     * Stores the default DTD (the content of the file <code>html32.bdtd</code>).
     */
    private static DTD defaultDTD;

    /**
     * Sets {@link ParserDelegator#defaultDTD} with the <code>html32.bdtd</code>
     * file by calling the method {@link ParserDelegator#setDefaultDTD()}.
     */
    public ParserDelegator() {
        setDefaultDTD();
    }

    /**
     * Simply calls the method
     * {@link javax.swing.text.html.parser.DocumentParser#parse(Reader, javax.swing.text.html.HTMLEditorKit.ParserCallback, boolean) parse(Reader, HTMLEditorKit.ParserCallback, boolean)}
     * of the wrapped {@link javax.swing.text.html.parser.DocumentParser} with
     * the same arguments
     * 
     * @param r
     *            the reader
     * @param cb
     *            the callback
     * @param ignoreCharSet
     *            the ignoreCharSet
     * @throws IOException
     *             if the {@link DocumentParser} propagates it
     */
    public void parse(final Reader r, final HTMLEditorKit.ParserCallback cb,
            final boolean ignoreCharSet) throws IOException {
        DocumentParser dp = new DocumentParser(defaultDTD); 
        dp.parse(r, cb, ignoreCharSet);
    }

    /**
     * Reads the DTD content from the file called <code>name</code> + "bdtd"
     * located in the classpath (if there is any) and fills the <code>dtd</code>
     * with it. Then, returns <code>dtd</code>. <br>
     * <br>
     * The complete behavior is the following:
     * <ol>
     * <li> Finds the resource (the ASN1 binary file) in the classpath by
     * appending ".bdtd" to <code>name</code>.
     * <li> Creates an stream from this file and adds the information contained
     * into <code>dtd</code> by calling its {@link DTD#read(DataInputStream)}
     * method
     * <li> Returns the <code>dtd</code>
     * </ol>
     * <br>
     * Notice that this method catches any {@link Exception} and ignores it.
     * It's the same behavior as the reference implementation.
     * 
     * @param dtd
     *            the dtd to be filled
     * @param name
     *            the name of the file located in the classpath (without .bdtd)
     * @return the <code>dtd</code>
     */
    protected static DTD createDTD(final DTD dtd, final String name) {
        try {
            String oldName = dtd.name;
            // gets the location of the harcoded file that is located in the
            // classpath ...
            // fills the DTD ...
            dtd.read(new DataInputStream(
                    ParserDelegator.class.getResourceAsStream(name + ".bdtd")));
            dtd.name = oldName;
            DTD.putDTDHash(name, dtd);
        } catch (Exception e) {
            // ignores any exception (same as Reference Implementation)
        }
        return dtd;
    }

    /**
     * Sets the content of {@link ParserDelegator#defaultDTD}. Internally,
     * calls {@link ParserDelegator#createDTD(DTD, String)} with
     * {@link ParserDelegator#defaultDTD} and
     * {@link ParserDelegator#DEFAULT_DTD_NAME} as arguments.
     * 
     * Notice that {@link ParserDelegator#defaultDTD} is cached. It means that
     * it's filled only once.
     */
    protected static synchronized void setDefaultDTD() {
        if (defaultDTD == null) {
            defaultDTD = new DTD(DEFAULT_DTD_NAME);
            createDTD(defaultDTD, DEFAULT_DTD_NAME);
        }
    }
}