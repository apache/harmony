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

package javax.print;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Locale;

import javax.print.attribute.DocAttributeSet;
import javax.print.attribute.HashDocAttributeSet;
import javax.print.attribute.HashPrintRequestAttributeSet;
import javax.print.attribute.PrintRequestAttributeSet;
import javax.print.attribute.standard.Copies;
import javax.print.attribute.standard.DocumentName;
import javax.print.attribute.standard.JobName;
import javax.print.attribute.standard.MediaName;
import javax.print.attribute.standard.RequestingUserName;

import junit.framework.TestCase;

public class PrintTest extends TestCase {
    public static void main(String[] args) {
        junit.textui.TestRunner.run(PrintTest.class);
    }

    public void testPrintSomething() {
        /*Authenticator.setDefault(new PrintTestAuth());*/

        boolean testrun = true;

        System.out.println("============= START PrintTest ================");
        //if ((System.getProperty("os.name")).toLowerCase().indexOf("linux") >= 0) {
        if (!testrun) {
            System.out.println("The test is always pass temporary.");
            System.out.println("============= END PrintTest ================");
            return;
        }

        String file_txt = "/Resources/readme.txt";
        String file_gif = "/Resources/printservice.gif";
        String http_gif = "";
        String http_ps = "";

        PrintService[] services = PrintServiceLookup.lookupPrintServices(null,
                null);
        TestUtil.checkServices(services);

        PrintService service = PrintServiceLookup.lookupDefaultPrintService();

        System.out.println("Default: "
                + (service == null ? "null" : service.getName()));

        System.out.println("Print services:");
        for (int i = 0; i < services.length; i++) {
            System.out.println("\t" + services[i].getName());
        }

        DocAttributeSet dset = new HashDocAttributeSet();
        dset.add(new DocumentName("print doc name", Locale.US));

        PrintRequestAttributeSet aset = new HashPrintRequestAttributeSet();
        aset.add(new Copies(3));
        aset.add(MediaName.ISO_A4_WHITE);
        aset.add(new RequestingUserName("ichebyki", Locale.US));

        try {
            PrintService serv = services[0];
            InputStream stream;
            DocFlavor flavor;
            DocPrintJob job;
            Doc doc;
            URL url;
            Reader rdr;

            try {
                flavor = DocFlavor.URL.PDF;//TEXT_HTML_HOST;
                if (serv.isDocFlavorSupported(flavor)) {
                    aset.add(new JobName(flavor.toString(), Locale.US));
                    dset.add(new DocumentName(http_ps, Locale.US));
                    job = serv.createPrintJob();
                    url = new URL(http_ps);
                    doc = new SimpleDoc(url, flavor, dset);
                    System.out.println("\nPrinting on "
                            + job.getPrintService().getName() + "...");
                    job.print(doc, aset);
                    System.out.println("File '" + http_ps + "' was printed as "
                            + flavor.getRepresentationClassName());
                }

                flavor = DocFlavor.URL.GIF;
                if (serv.isDocFlavorSupported(flavor)) {
                    aset.add(new JobName(flavor.toString(), Locale.US));
                    dset.add(new DocumentName(http_gif, Locale.US));
                    job = serv.createPrintJob();
                    url = new URL(http_gif);
                    doc = new SimpleDoc(url, flavor, dset);
                    System.out.println("\nPrinting on "
                            + job.getPrintService().getName() + "...");
                    job.print(doc, aset);
                    System.out.println("File '" + http_gif + "' was printed as "
                            + flavor.getRepresentationClassName());
                }
            } catch (PrintException e1) {
                e1.printStackTrace();
            }

            flavor = DocFlavor.READER.TEXT_PLAIN;
            if (serv.isDocFlavorSupported(flavor)) {
                aset.add(new JobName(flavor.toString(), Locale.US));
                dset.add(new DocumentName(file_txt, Locale.US));
                job = serv.createPrintJob();
                rdr = new InputStreamReader(getClass().getResourceAsStream(
                        file_txt));
                doc = new SimpleDoc(rdr, flavor, dset);
                System.out.println("Printing on "
                        + job.getPrintService().getName() + "...");
                job.print(doc, aset);
                System.out.println("File '" + file_txt + "' was printed as "
                        + flavor.getRepresentationClassName());
            }

            flavor = DocFlavor.INPUT_STREAM.GIF;
            if (serv.isDocFlavorSupported(flavor)) {
                aset.add(new JobName(flavor.toString(), Locale.US));
                dset.add(new DocumentName(file_gif, Locale.US));
                job = serv.createPrintJob();
                stream = getClass().getResourceAsStream(file_gif);
                doc = new SimpleDoc(stream, flavor, dset);
                System.out.println("\nPrinting on "
                        + job.getPrintService().getName() + "...");
                job.print(doc, aset);
                System.out.println("File '" + file_gif + "' was printed as "
                        + flavor.getRepresentationClassName());
            }

            flavor = DocFlavor.BYTE_ARRAY.JPEG;
            if (serv.isDocFlavorSupported(flavor)) {
                aset.add(new JobName(flavor.toString(), Locale.US));
                dset.add(new DocumentName(file_gif, Locale.US));
                job = serv.createPrintJob();
                stream = getClass().getResourceAsStream(file_gif);
                byte[] gif_buf;
                byte[] buf = new byte[1024];
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                int count;
                while ((count = stream.read(buf, 0, 1024)) > 0) {
                    baos.write(buf, 0, count);
                }
                stream.close();
                gif_buf = baos.toByteArray();
                baos.close();
                doc = new SimpleDoc(gif_buf, flavor, dset);
                System.out.println("\nPrinting on "
                        + job.getPrintService().getName() + "...");
                job.print(doc, aset);
                System.out.println("File '" + file_gif + "' was printed as "
                        + flavor.getRepresentationClassName());
            }
        } catch (MalformedURLException e) {
            e.printStackTrace();
            fail("Unexpected exception occured!\n" + e.getLocalizedMessage());
        } catch (PrintException e) {
            e.printStackTrace();
            fail("PrintException occured!\n" + e.getLocalizedMessage());
        } catch (IOException e) {
            e.printStackTrace();
            fail("Unexpected exception occured!\n" + e.getLocalizedMessage());
        }

        System.out.println("============= END PrintTest ================");
    }

    /*
     * For authentication
     * uncomment when swing will support JTextField and etc. 
     */
    /*
    class PrintTestAuth extends Authenticator {
        protected PasswordAuthentication getPasswordAuthentication() {
            JTextField username = new JTextField();
            JTextField password = new JPasswordField();
            JPanel panel = new JPanel(new GridLayout(2, 2));
            panel.add(new JLabel("Login"));
            panel.add(username);
            panel.add(new JLabel("Password"));
            panel.add(password);
            int option = JOptionPane.CANCEL_OPTION; 
            
            option = JOptionPane.showConfirmDialog(null,
                    new Object[] { "Site: " + getRequestingHost(),
                            "Realm: " + getRequestingPrompt(),
                            panel }, "Enter Network Password",
                    JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
            if (option == JOptionPane.OK_OPTION) {
                String user = username.getText();
                char pass[] = password.getText().toCharArray();
                return new PasswordAuthentication(user, pass);
            }

            return null;
        }
    }
    */

}