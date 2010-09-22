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

package java.awt;

import org.apache.harmony.awt.internal.nls.Messages;

public final class JobAttributes implements Cloneable {

    private int copies;
    private int fromPage;
    private int maxPage;
    private int minPage;
    private int pageRanges[][];
    private int firstPage;
    private int lastPage;
    private int toPage;
    private String fileName;
    private String printer;
    private DefaultSelectionType defaultSelection;
    private DestinationType destination;
    private MultipleDocumentHandlingType multiDocHandling;
    private DialogType dialog;
    private SidesType sides;


    /* section of the nested classes */
    public static final class DefaultSelectionType {
        public static final DefaultSelectionType ALL
                                    = new DefaultSelectionType(0);
        public static final DefaultSelectionType RANGE
                                    = new DefaultSelectionType(1);
        public static final DefaultSelectionType SELECTION
                                    = new DefaultSelectionType(2);

        private DefaultSelectionType(int i){
            super();
        }

        private DefaultSelectionType(){
            this(0);
        }
    }

    public static final class DestinationType {
        public static final DestinationType FILE = new DestinationType(0);
        public static final DestinationType PRINTER = new DestinationType(1);

        private DestinationType(int i) {
            super();
        }
        private DestinationType() {
            this(0);
        }
    }

    public static final class DialogType{
        public static final DialogType COMMON = new DialogType(0);
        public static final DialogType NATIVE = new DialogType(1);
        public static final DialogType NONE = new DialogType(2);

        private DialogType(int i){
            super();
        }
        private DialogType(){
            this(0);
        }

    }

    public static final class MultipleDocumentHandlingType {
        public static final MultipleDocumentHandlingType
                            SEPARATE_DOCUMENTS_COLLATED_COPIES
                                = new MultipleDocumentHandlingType(0);
        public static final MultipleDocumentHandlingType
                            SEPARATE_DOCUMENTS_UNCOLLATED_COPIES
                            = new MultipleDocumentHandlingType(1);

        private MultipleDocumentHandlingType(int i){
            super();
        }

        private MultipleDocumentHandlingType(){
            this(0);
        }
    }

    public static final class SidesType{
        public static final SidesType ONE_SIDED = new SidesType(0);
        public static final SidesType TWO_SIDED_LONG_EDGE  = new SidesType(1);
        public static final SidesType TWO_SIDED_SHORT_EDGE = new SidesType(2);

        private SidesType(int i){
            super();
        }

        private SidesType(){
            this(0);
        }
    }
    /* end of the nested classes */

    public JobAttributes() {
        setDefaultSelection(DefaultSelectionType.ALL);
        setDestination(DestinationType.PRINTER);
        setDialog(DialogType.NATIVE);
        setMultipleDocumentHandlingToDefault();
        setSidesToDefault();
        setCopiesToDefault();
        setMaxPage(0x7fffffff);
        setMinPage(1);
    }

    public JobAttributes(JobAttributes obj){
            set(obj);
    }

    public JobAttributes(int copies,
            JobAttributes.DefaultSelectionType defaultSelection,
            JobAttributes.DestinationType destination,
            JobAttributes.DialogType dialog,
            String fileName,
            int maxPage,
            int minPage,
            JobAttributes.MultipleDocumentHandlingType multipleDocumentHandling,
            int[][] pageRanges,
            String printer,
            JobAttributes.SidesType sides){

        setCopies(copies);
        setDefaultSelection(defaultSelection);
        setDestination(destination);
        setDialog(dialog);
        setFileName(fileName);
        setMinPage(minPage);
        setMaxPage(maxPage);
        setMultipleDocumentHandling(multipleDocumentHandling);
        setPageRanges(pageRanges);
        setPrinter(printer);
        setSides(sides);
    }

    public void setCopiesToDefault() {
        setCopies(1);
    }

    public void setMultipleDocumentHandlingToDefault() {
        setMultipleDocumentHandling
           (MultipleDocumentHandlingType.SEPARATE_DOCUMENTS_UNCOLLATED_COPIES);
    }

    public void setSidesToDefault(){
        setSides(SidesType.ONE_SIDED);
    }

    public int getCopies(){
        return copies;
    }

    public void setCopies(int copies) {
        if(copies <= 0) {
            // awt.152=Invalid number of copies
            throw new IllegalArgumentException(Messages.getString("awt.152")); //$NON-NLS-1$
        }
        this.copies = copies;
    }

    public int getMaxPage(){
        return maxPage;
    }

    public void setMaxPage(int imaxPage) {
        if (imaxPage <= 0 || imaxPage < minPage) {
            // awt.153=Invalid value for maxPage
            throw new IllegalArgumentException(Messages.getString("awt.153")); //$NON-NLS-1$
        }
        maxPage = imaxPage;
    }

    public int getMinPage(){
        return minPage;
    }

    public void setMinPage(int iminPage) {
        if (iminPage <= 0 || iminPage > maxPage) {
            // awt.154=Invalid value for minPage
            throw new IllegalArgumentException(Messages.getString("awt.154")); //$NON-NLS-1$
        }
        minPage = iminPage;
    }

    public int getFromPage() {
        if (fromPage != 0) {
            return fromPage;
        }
        if (toPage != 0) {
            return getMinPage();
        }
        if (pageRanges != null) {
            return firstPage;
        }
        return getMinPage();
    }

    public void setFromPage(int ifromPage) {
        if (ifromPage <= 0 || ifromPage > toPage
                || ifromPage < minPage || ifromPage > maxPage) {
            // awt.155=Invalid value for fromPage
            throw new IllegalArgumentException(Messages.getString("awt.155")); //$NON-NLS-1$
        }
        fromPage = ifromPage;
    }

    public int getToPage() {
        if (toPage != 0) {
            return toPage;
        }
        if (fromPage != 0) {
            return fromPage;
        }
        if (pageRanges != null) {
            return lastPage;
        }
        return getMinPage();
    }

    public void setToPage(int itoPage) {
        if (itoPage <= 0 || itoPage < fromPage
                || itoPage < minPage
                || itoPage > maxPage) {
            // awt.156=Invalid value for toPage
            throw new IllegalArgumentException(Messages.getString("awt.156")); //$NON-NLS-1$
        }
        toPage = itoPage;
    }

    public String getPrinter(){
        return printer;
    }

    public void setPrinter(String printer){
        this.printer = printer;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getFileName() {
        return fileName;
    }

    public int[][] getPageRanges() {
        int prl = pageRanges.length;
        int pr[][];
        if (pageRanges != null) {
            pr = new int[prl][2];
            for (int i = 0; i < prl; i++) {
                pr[i][0] = pageRanges[i][0];
                pr[i][1] = pageRanges[i][1];
            }
            return pr;
        }
        pr = new int[1][2];
        if (fromPage != 0 || toPage != 0) {
            pr[0][0] = fromPage;
            pr[0][1] = toPage;
        } else {
            pr[0][0] = minPage;
            pr[0][1] = minPage;
        }
        return pr;
    }

    public void setPageRanges(int[][] pr) {
        // awt.157=Invalid value for pageRanges
        String msg = Messages.getString("awt.157"); //$NON-NLS-1$

        if(pr == null) {
            throw new IllegalArgumentException(msg);
        }
        
        int n1 = 0;
        int n2 = 0;
        int prl = pr.length;

        for(int k = 0; k < prl; k++) {
            if(pr[k] == null || pr[k].length != 2
                    || pr[k][0] <= n2 || pr[k][1] < pr[k][0]) {
                throw new IllegalArgumentException(msg);
            }

            n2 = pr[k][1];
            if(n1 == 0) {
                n1 = pr[k][0];
            }
        }

        if(n1 < minPage || n2 > maxPage) {
            throw new IllegalArgumentException(msg);
        }

        pageRanges = new int[prl][2];

        for(int k = 0; k < prl; k++) {
            pageRanges[k][0] = pr[k][0];
            pageRanges[k][1] = pr[k][1];
        }
        firstPage = n1;
        lastPage = n2;
    }

    public DestinationType getDestination() {
        return destination;
    }

    public void setDestination(JobAttributes.DestinationType destination) {
        if(destination == null){
            // awt.158=Invalid value for destination
            throw new IllegalArgumentException(Messages.getString("awt.158")); //$NON-NLS-1$
        }
        this.destination = destination;
    }

    public DialogType getDialog() {
        return dialog;
    }

    public void setDialog(JobAttributes.DialogType dialog) {
        if(dialog == null) {
            // awt.159=Invalid value for dialog
            throw new IllegalArgumentException(Messages.getString("awt.159")); //$NON-NLS-1$
        }
        this.dialog = dialog;
    }


    public JobAttributes.DefaultSelectionType getDefaultSelection() {
        return defaultSelection;
    }

    public void setDefaultSelection(
            JobAttributes.DefaultSelectionType a_defaultSelection) {
        if (a_defaultSelection == null) {
            // awt.15A=Invalid value for defaultSelection
            throw new IllegalArgumentException(Messages.getString("awt.15A")); //$NON-NLS-1$
        }
        this.defaultSelection = a_defaultSelection;
    }

    public JobAttributes.MultipleDocumentHandlingType
            getMultipleDocumentHandling(){
        return multiDocHandling;
    }

    public void setMultipleDocumentHandling
        (JobAttributes.MultipleDocumentHandlingType multipleDocumentHandling){

        if(multipleDocumentHandling == null) {
            // awt.15B=Invalid value for multipleDocumentHandling
            throw new IllegalArgumentException(Messages.getString("awt.15B")); //$NON-NLS-1$
        }
        multiDocHandling = multipleDocumentHandling;
    }

    public JobAttributes.SidesType getSides(){
        return sides;
    }

    public void setSides(JobAttributes.SidesType sides){

        if(sides == null) {
            // awt.15C=Invalid value for attribute sides
            throw new IllegalArgumentException(Messages.getString("awt.15C")); //$NON-NLS-1$
        }
        this.sides = sides;
    }

    public void set(JobAttributes obj) {
        copies = obj.copies;
        defaultSelection = obj.defaultSelection;
        destination = obj.destination;
        dialog = obj.dialog;
        fileName = obj.fileName;
        printer = obj.printer;
        multiDocHandling = obj.multiDocHandling;
        firstPage = obj.firstPage;
        lastPage = obj.lastPage;
        sides = obj.sides;
        fromPage = obj.fromPage;
        toPage = obj.toPage;
        maxPage = obj.maxPage;
        minPage = obj.minPage;
        if (obj.pageRanges == null) {
            pageRanges = null;
        } else {
            setPageRanges(obj.pageRanges);
        }
    }

    @Override
    public String toString(){
        /* The format is based on 1.5 release behavior 
         * which can be revealed by the following code:
         * System.out.println(new JobAttributes());
         */

        String s = "Page-ranges ["; //$NON-NLS-1$
        int k = pageRanges.length-1;
        for(int i = 0; i <= k ; i++)            {
            s += pageRanges[i][0] + "-" //$NON-NLS-1$
               + pageRanges[i][1] + ((i < k)? ",": ""); //$NON-NLS-1$ //$NON-NLS-2$
        }
        s += "], copies=" + getCopies() //$NON-NLS-1$
            + ",defSelection=" + getDefaultSelection() //$NON-NLS-1$
            + ",dest=" + getDestination() //$NON-NLS-1$
            + ",fromPg=" + getFromPage() //$NON-NLS-1$
            + ",toPg=" + getToPage() //$NON-NLS-1$
            + ",minPg=" + getMinPage() //$NON-NLS-1$
            + ",maxPg=" + getMaxPage() //$NON-NLS-1$
            + ",multiple-document-handling=" //$NON-NLS-1$
            + getMultipleDocumentHandling()
            + ",fileName=" + getFileName() //$NON-NLS-1$
            + ",printer=" + getPrinter() //$NON-NLS-1$
            + ",dialog=" + getDialog() //$NON-NLS-1$
            + ",sides=" + getSides(); //$NON-NLS-1$
        return s;
    }

    @Override
    public int hashCode() {
        int hash = this.toString().hashCode();
        return hash;
    }

    @Override
    public Object clone() {
        JobAttributes ja = new JobAttributes(this);
        return ja;
    }

    @Override
    public boolean equals(Object obj){
        if(!(obj instanceof JobAttributes)){
            return false;
        }
        JobAttributes ja = (JobAttributes)obj;

        if(fileName == null){
            if(ja.fileName != null) {
                return false;
            }
        } else {
            if(!fileName.equals(ja.fileName)) {
                return false;
            }
        }

        if(printer == null) {
            if(ja.printer != null){
                return false;
            }
        } else {
            if(!printer.equals(ja.printer)){
                return false;
            }
        }

        if(pageRanges == null) {
            if(ja.pageRanges != null) {
                return false;
            }
        } else {
            if(ja.pageRanges == null){
                return false;
            }
            if(pageRanges.length != ja.pageRanges.length){
                return false;
            }
            for (int[] element : pageRanges) {
                if(element[0] != element[0]
                || element[1] != element[1]){
                    return false;
                }
            }
        }
        if(copies != ja.copies){
            return false;
        }
        if(defaultSelection != ja.defaultSelection){
            return false;
        }
        if(destination != ja.destination){
            return false;
        }
        if(dialog != ja.dialog){
            return false;
        }
        if(maxPage != ja.maxPage){
            return false;
        }
        if(minPage != ja.minPage){
            return false;
        }
        if(multiDocHandling != ja.multiDocHandling){
            return false;
        }
        if(firstPage != ja.firstPage){
            return false;
        }
        if(lastPage != ja.lastPage){
            return false;
        }
        if(sides != ja.sides){
            return false;
        }
        if(toPage != ja.toPage){
            return false;
        }
        if(fromPage != ja.fromPage){
            return false;
        }
        return true;
    }
}


