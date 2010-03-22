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
 * @author Igor A. Pyankov
 */
package java.awt.print;

import java.util.Vector;

import org.apache.harmony.awt.internal.nls.Messages;

public class Book implements Pageable {

    private final Vector<innerPage> bookPages;

    // innerPage -  class to describe inner structure of Book
    private class innerPage {
        private final PageFormat pageFormat;
        private final Printable pagePainter;

        /* constructor */
        innerPage(Printable painter, PageFormat page) {
            super();
            if (painter == null) {
                // awt.01='{0}' parameter is null
                throw new NullPointerException(Messages.getString(
                        "awt.01", "painter")); //$NON-NLS-1$ //$NON-NLS-2$
            }
            if (page == null) {
                // awt.01='{0}' parameter is null
                throw new NullPointerException(Messages.getString(
                        "awt.01", "page")); //$NON-NLS-1$ //$NON-NLS-2$
            }
            pagePainter = painter;
            pageFormat = page;
            return;
        }

        PageFormat getPageFormat() {
            return pageFormat;
        }

        Printable getPrintable() {
            return pagePainter;
        }
    }

    public Book() {
        bookPages = new Vector<innerPage>();
    }

    public void append(Printable painter, PageFormat page) {
        bookPages.addElement(new innerPage(painter, page));
    }

    public void append(Printable painter, PageFormat page, int numPages) {
        innerPage newpage = new innerPage(painter, page);
        int orign_length = bookPages.size();
        int new_length = orign_length + numPages;
        bookPages.setSize(new_length);
        for (int i = orign_length; i < new_length; i++) {
            bookPages.setElementAt(newpage, i);
        }
    }

    public int getNumberOfPages() {
        return bookPages.size();
    }

    public PageFormat getPageFormat(int pageIndex)
            throws IndexOutOfBoundsException {

        if (pageIndex >= getNumberOfPages()) {
            // awt.5E=pageIndex is more than book size
            throw new IndexOutOfBoundsException(Messages.getString("awt.5E")); //$NON-NLS-1$
        }
        return bookPages.elementAt(pageIndex).getPageFormat();
    }

    public Printable getPrintable(int pageIndex)
            throws IndexOutOfBoundsException {

        if (pageIndex >= getNumberOfPages()) {
            // awt.5E=pageIndex is more than book size
            throw new IndexOutOfBoundsException(Messages.getString("awt.5E")); //$NON-NLS-1$
        }
        return bookPages.elementAt(pageIndex).getPrintable();
    }

    public void setPage(int pageIndex, Printable painter, PageFormat page)
            throws IndexOutOfBoundsException {

        if(painter == null) {
            throw new NullPointerException(Messages.getString("awt.01", "painter")); //$NON-NLS-1$
        }
        
        if(page == null) {
            throw new NullPointerException(Messages.getString("awt.01", "page")); //$NON-NLS-1$
        }
        
        if (pageIndex >= getNumberOfPages()) {
            // awt.5E=pageIndex is more than book size
            throw new IndexOutOfBoundsException(Messages.getString("awt.5E")); //$NON-NLS-1$
        }
        bookPages.setElementAt(new innerPage(painter, page), pageIndex);
    }
}
