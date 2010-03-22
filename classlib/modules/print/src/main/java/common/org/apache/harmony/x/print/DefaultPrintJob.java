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
 * @author Aleksei V. Ivaschenko 
 */ 

package org.apache.harmony.x.print;

import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;

import javax.print.Doc;
import javax.print.DocFlavor;
import javax.print.DocPrintJob;
import javax.print.PrintException;
import javax.print.PrintService;
import javax.print.SimpleDoc;
import javax.print.StreamPrintService;
import javax.print.StreamPrintServiceFactory;
import javax.print.attribute.AttributeSetUtilities;
import javax.print.attribute.HashPrintJobAttributeSet;
import javax.print.attribute.PrintJobAttributeSet;
import javax.print.attribute.PrintRequestAttributeSet;
import javax.print.event.PrintJobAttributeListener;
import javax.print.event.PrintJobListener;

public class DefaultPrintJob implements DocPrintJob {
    DefaultPrintService printService;
    PrintClient printClient;
    PrintJobAttributeSet printJobAS;
    boolean busyFlag;

    public DefaultPrintJob(DefaultPrintService printservice) {
        if (printservice == null) {
            throw new NullPointerException("Argument is null");
        }
        this.printService = printservice;
        this.printClient = printService.getPrintClient();
        printJobAS = new HashPrintJobAttributeSet();
        busyFlag = false;
    }

    public PrintService getPrintService() {
        return printService;
    }

    public PrintJobAttributeSet getAttributes() {
        return AttributeSetUtilities.unmodifiableView(printJobAS);
    }

    //=======================================================================//
    public void print(Doc userDoc, PrintRequestAttributeSet printRequestAS)
            throws PrintException {

        synchronized (this) {
            if (busyFlag) {
                throw new PrintException(
                        "Already printed. Need to create new DocPrintJob.");
            }
            busyFlag = true;
        }
        
        DocFlavor userDocDF = userDoc.getDocFlavor();

        /*
         * Checking if doc.DocFlavor is supported by the current
         * PrintService
         */
        if (!printService.isDocFlavorSupported(userDocDF)) {
            throw new PrintException("Doc flavor \'" + userDocDF
                    + "\' is not supported");
        }

        /*
         * Checking if doc.DocFlavor is supported directly by osClent. If it
         * is not: - get StereamPrintServiceFactory for doc.DocFlavor -
         * instantiate StreamPrintService - get PrintJob from it - run this
         * PrintJob in separate thread
         */
        printClient = printService.getPrintClient();
        if (printService.isDocFlavorSupportedByClient(userDocDF)) {
            printClient.print(userDoc, printRequestAS);
        } else {
            try {
                Doc clientDoc = userDoc;
                PipedOutputStream spsOutStream = new PipedOutputStream();
                PipedInputStream clientInputStream = new PipedInputStream(
                        spsOutStream);

                DocFlavor newFlavor = null;
                StreamPrintServiceFactory spsf = null;
                DocFlavor clientFlavors[] = printClient
                        .getSupportedDocFlavors();

                for (int i = 0; i < clientFlavors.length; i++) {
                    StreamPrintServiceFactory[] factories = StreamPrintServiceFactory
                            .lookupStreamPrintServiceFactories(userDocDF,
                                    clientFlavors[i].getMimeType());
                    if (factories.length > 0
                            && Class.forName(
                                    clientFlavors[i]
                                            .getRepresentationClassName())
                                    .isInstance(clientInputStream)) {
                        spsf = factories[0];
                        newFlavor = clientFlavors[i];
                        break;
                    }
                }

                if (spsf != null) {
                    StreamPrintService sps = spsf.getPrintService(spsOutStream);

                    /*
                     * Constructing new Doc object for client: - connecting
                     * InputStream of client with OutputStream of
                     * StreamPrintSrevice - constructing DocFlavor for
                     * StreamPrintSrevice output - creating new SimpleDoc
                     * for client
                     */
                    clientDoc = new SimpleDoc(clientInputStream, newFlavor,
                            userDoc.getAttributes());

                    PrintJobThread printThread = new PrintJobThread(this,
                            userDoc, printRequestAS, sps);
                    printThread.start();
                    printClient.print(clientDoc, printRequestAS);

                    if (printThread.exceptionOccured()) {
                        throw new PrintException(printThread
                                .getPrintException());
                    }
                } else {
                    throw new PrintException("Doc flavor "
                            + userDocDF.getRepresentationClassName()
                            + " is not supported");
                }
            } catch (ClassNotFoundException e) {
                throw new PrintException(e);
            } catch (IOException e) {
                throw new PrintException(e);
            } catch (PrintException e) {
                throw e;
            }
        }
    }

    //=======================================================================//
    public void addPrintJobAttributeListener(
            PrintJobAttributeListener listener, PrintJobAttributeSet attributes) {
        synchronized (this) {
            // TODO - add print job attribute listener
        }
    }

    public void addPrintJobListener(PrintJobListener listener) {
        synchronized (this) {
            // TODO - add print job listener
        }
    }

    public void removePrintJobAttributeListener(
            PrintJobAttributeListener listener) {
        synchronized (this) {
            // TODO - remove print job attribute listener
        }
    }

    public void removePrintJobListener(PrintJobListener listener) {
        synchronized (this) {
            // TODO - remove print job listener
        }
    }

    class PrintJobThread extends Thread {
        DefaultPrintJob printJob;
        Doc printDoc;
        PrintRequestAttributeSet printAttributeSet;
        Exception exception;
        boolean exceptionisnotnull;
        StreamPrintService streamservice;

        /**
         * job - parent DefaultPrintJob doc - doc to print attributeset -
         * attributes set spsDocPrintJob - stream print service's print job
         */
        PrintJobThread(DefaultPrintJob job, Doc doc,
                PrintRequestAttributeSet attributeset, StreamPrintService sps) {
            this.printJob = job;
            this.printDoc = doc;
            this.printAttributeSet = attributeset;
            this.streamservice = sps;
            this.exception = null;
            this.exceptionisnotnull = false;
        }

        public void run() {
            try {
                DocPrintJob spsDocPrintJob = streamservice.createPrintJob();
                spsDocPrintJob.print(printDoc, printAttributeSet);
            } catch (Exception e) {
                exception = e;
                exceptionisnotnull = true;
                try {
                    streamservice.getOutputStream().close();
                } catch (IOException ioe) {
                    // ignoring
                }
            }
        }

        boolean exceptionOccured() {
            return exceptionisnotnull;
        }

        Exception getPrintException() {
            return exception;
        }
    }
}