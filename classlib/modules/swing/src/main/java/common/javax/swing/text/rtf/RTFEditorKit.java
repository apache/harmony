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

package javax.swing.text.rtf;

import org.apache.harmony.luni.util.NotImplementedException;
import org.apache.harmony.x.swing.text.rtf.RTFParser;
import org.apache.harmony.x.swing.text.rtf.ParseException;
import org.apache.harmony.x.swing.text.rtf.RTFHandler;
import org.apache.harmony.x.swing.text.rtf.DocumentRTFHandler;

import javax.swing.text.StyledEditorKit;
import javax.swing.text.Document;
import javax.swing.text.BadLocationException;
import java.io.*;

/**
 * @author Aleksey Lagoshin
 */
public class RTFEditorKit extends StyledEditorKit {

  public RTFEditorKit() {
	  super();
  }

  public String getContentType() {
    return "text/rtf";
  }

  public void read(InputStream in, Document doc, int pos) throws IOException, BadLocationException {
    RTFParser parser = new RTFParser(in);
    try {
      RTFHandler handler = new DocumentRTFHandler(doc, pos);
      parser.parse(handler);
    }
    catch (ParseException e) {
      IOException ioex = new IOException(e.toString());

      ioex.initCause(e);
      throw ioex;
    }
  }

  public void read(Reader in, Document doc, int pos) throws IOException, BadLocationException {
    RTFParser parser = new RTFParser(in);
    try {
      RTFHandler handler = new DocumentRTFHandler(doc, pos);
      parser.parse(handler);
    }
    catch (ParseException e) {
      IOException ioex = new IOException(e.toString());

      ioex.initCause(e);
      throw ioex;
    }
  }

  public void write(OutputStream out, Document doc, int pos, int len) throws IOException, BadLocationException, NotImplementedException {
      throw new NotImplementedException();
  }

  public void write(Writer out, Document doc, int pos, int len) throws IOException, BadLocationException {
      throw new IOException("Cannot write RTF");
  }

}