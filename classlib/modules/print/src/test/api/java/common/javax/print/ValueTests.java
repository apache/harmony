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
 * @author Irina A. Arkhipets 
 */ 

/*
 * ValueTests.java 
 * 
 * JUnit not-interactive Value tests for "Hello, World!" version (javax.print
 * package only).
 * 
 */

package javax.print;

import java.io.ByteArrayOutputStream;
import java.io.FileReader;
import java.io.InputStream;
import java.util.Locale;
import java.util.Vector;

import javax.print.attribute.Attribute;
import javax.print.attribute.DocAttribute;
import javax.print.attribute.DocAttributeSet;
import javax.print.attribute.HashDocAttributeSet;
import javax.print.attribute.HashPrintJobAttributeSet;
import javax.print.attribute.HashPrintRequestAttributeSet;
import javax.print.attribute.HashPrintServiceAttributeSet;
import javax.print.attribute.PrintJobAttribute;
import javax.print.attribute.PrintJobAttributeSet;
import javax.print.attribute.PrintRequestAttribute;
import javax.print.attribute.PrintRequestAttributeSet;
import javax.print.attribute.PrintServiceAttribute;
import javax.print.attribute.PrintServiceAttributeSet;
import javax.print.attribute.standard.ColorSupported;
import javax.print.attribute.standard.Copies;
import javax.print.attribute.standard.MediaName;
import javax.print.attribute.standard.OrientationRequested;
import javax.print.attribute.standard.PrintQuality;
import javax.print.attribute.standard.PrinterLocation;

import junit.framework.TestCase;

public class ValueTests extends TestCase {

private static String OS = null;

static {
    try {
        OS = System.getProperty("os.name");
        if (OS.startsWith("Windows")) {
            System.loadLibrary("ValueTestsLibrary");
        }
    } catch (Exception e) {
        if (OS == null) {
            System.out.println("WARNING: Can not get Operation System name!");
        } else {
            if (OS.startsWith("Windows")) {
                System.out.println(
                        "WARNING! Can not load ValueTestsLibrary library!");
            }
        }
        System.out.println("Some testcases probably will not be started!");
   }
}

public void testDocFlavor() {
    startTest("DocFlavor class testing...");
    DocFlavor flavor = DocFlavor.INPUT_STREAM.GIF;
    assertEquals(flavor, new DocFlavor.INPUT_STREAM("image/gif"));
    assertEquals(flavor.getMediaSubtype(), "gif");
    assertEquals(flavor.getMediaType(), "image");
    assertEquals(flavor.getMimeType(), "image/gif");
    assertEquals(flavor.getRepresentationClassName(), "java.io.InputStream");
    assertEquals(flavor.toString(), 
            "image/gif; class=\"java.io.InputStream\"");
}

public void testSimpleDoc() {
    startTest("SimpleDoc class testing...");
    
    DocFlavor flavor = DocFlavor.INPUT_STREAM.GIF;
    InputStream reader =
            getClass().getResourceAsStream("/Resources/picture.gif");
    if (reader == null) {
        fail("/Resources/picture.gif resource is not found!");
    }
    
    DocAttributeSet aSet = new HashDocAttributeSet();
    Doc doc = new SimpleDoc(reader, flavor, aSet);
    
    assertEquals(doc.getAttributes(), aSet);
    aSet.add(OrientationRequested.LANDSCAPE);
    aSet.add(MediaName.NA_LETTER_WHITE);
    assertEquals(doc.getAttributes(), aSet);
    assertEquals(doc.getDocFlavor(), DocFlavor.INPUT_STREAM.GIF);
    try {
        assertTrue(doc.getPrintData() instanceof java.io.InputStream);
        assertNull(doc.getReaderForText());
        assertEquals(doc.getStreamForBytes(), reader);
    } catch(Exception e) {
        e.printStackTrace();
        fail("Exception found: "+e);
    }
}

public void testDefaultPrintService() {
    boolean flg = false;

    startTest("PrintService class testing...");
    DocFlavor flavor = DocFlavor.INPUT_STREAM.GIF;
    PrintRequestAttributeSet printRequestSet =
            new HashPrintRequestAttributeSet();
    PrintService service = getPrintService(flavor, printRequestSet);
    DocFlavor [] flavors;
    String myName = getDefaultPrintService();
    
    System.out.println("OS is " + OS);
    if (service == null) {
        System.out.println(
        "WARNING: Can not get print service which supports INPUT_STREAM.GIF!");
    } else {
        assertTrue(service.isDocFlavorSupported(flavor));
        flavors = service.getSupportedDocFlavors();
        for (int i = 0; i < flavors.length; i++) {
            if (flavors[i].equals(flavor)) {
                flg = true;
                break;
            }
        }
        assertTrue(flg);
/*        if (myName != null) {
            assertEquals(service.getName(), myName);
        }
*/
    }
}

public void testPrintJob() {
    startTest("PrintJob class testing...");
    DocFlavor flavor = DocFlavor.INPUT_STREAM.GIF;
    PrintRequestAttributeSet printRequestSet = 
            new HashPrintRequestAttributeSet();
    PrintService service = getPrintService(flavor, printRequestSet);
    if (service != null) { 
        DocPrintJob job = service.createPrintJob();
        assertEquals(job.getPrintService(), service);
    } else {
        System.out.println(
        "WARNING: Can not get print service which supports INPUT_STREAM.GIF!");
    }
}

public void testStreamServiceFactory() {
    startTest("StreamPrintServiceFactory class testing...");
    boolean flg = false;
    DocFlavor flavor = DocFlavor.INPUT_STREAM.GIF;
    StreamPrintServiceFactory [] aFactory = StreamPrintServiceFactory
          .lookupStreamPrintServiceFactories(flavor, "application/postscript");
    StreamPrintServiceFactory streamFactory=null;
    StreamPrintService streamService;
    DocFlavor [] flavors;
    
    if ((aFactory == null) || (aFactory.length == 0)) {
        fail("Can not find stream print service factory!");
    } else {
        streamFactory = aFactory[0];
    }
    streamService = streamFactory.getPrintService(new ByteArrayOutputStream());
    assertEquals(streamFactory.getOutputFormat(), "application/postscript");
    assertEquals(streamFactory.getPrintService(new ByteArrayOutputStream()),
                 streamService);
    flavors = streamFactory.getSupportedDocFlavors();
    for (int i = 0; i < flavors.length; i++) {
        if (flavors[i].equals(flavor)) {
            flg = true;
            break;
        }
    }
    assertTrue(flg);
}

public void testStreamPrintService() {
    startTest("StreamPrintService class testing...");
    
    DocFlavor flavor = DocFlavor.INPUT_STREAM.GIF;
    StreamPrintServiceFactory [] aFactory = StreamPrintServiceFactory
          .lookupStreamPrintServiceFactories(flavor, "application/postscript");
    StreamPrintServiceFactory streamFactory = null;
    StreamPrintService streamService; 
    DocFlavor [] flavors;
    boolean flg = false;
    
    if ((aFactory == null) || (aFactory.length == 0)) {
        fail("Can not find stream print service factory!");
    } else {
        streamFactory = aFactory[0];
    }
    streamService = streamFactory.getPrintService(new ByteArrayOutputStream());
    
    assertEquals(streamService.getOutputFormat(), "application/postscript");
    assertTrue(streamService.isDocFlavorSupported(flavor));
    flavors = streamService.getSupportedDocFlavors();
    for (int i = 0; i < flavors.length; i++) {
        if (flavors[i].equals(flavor)) {
            flg = true;
            break;
        }
    }
    assertTrue(flg);
    streamService.dispose();
    assertTrue(streamService.isDisposed());
}

public void testStreamServicePrinting() throws Exception {
    startTest("StreamPrintServiceFactory class testing...");
    
    byte [] forChecking = {'%', '!', 'P', 'S', '-', 'A', 'd', 'o', 'b', 'e'};
    DocFlavor flavor = DocFlavor.INPUT_STREAM.GIF;
    StreamPrintServiceFactory [] aFactory = StreamPrintServiceFactory
          .lookupStreamPrintServiceFactories(flavor, "application/postscript");
    StreamPrintServiceFactory streamFactory = null;
    StreamPrintService streamService; 
    DocPrintJob aJob;
    InputStream aStream = 
        getClass().getResourceAsStream("/Resources/picture.gif");
    Doc doc;
    byte [] arr;
    boolean flg = true;
    
    if ((aFactory == null) || (aFactory.length == 0)) {
        fail("Can not find stream print service factory!");
    } else {
        streamFactory = aFactory[0];
    }
    if (aStream == null) {
        fail("/Resources/picture.gif resource is not found!");        
    }
    
    streamService = streamFactory.getPrintService(new ByteArrayOutputStream());
    aJob = streamService.createPrintJob();
    doc = new SimpleDoc(aStream, flavor, null);

    aJob.print(doc, null);

    arr = ((ByteArrayOutputStream) (streamService.getOutputStream()))
            .toByteArray();
    for (int i = 0; i < 10; i++) {
        if(arr[i] != forChecking[i]) {
            flg = false;
            break;
        }
    }
    assertTrue(flg);
}

public void testHashDocAttributeSet() {
    startTest("HashDocAttributeSet class testing...");

    DocAttributeSet set1 = new HashDocAttributeSet();
    DocAttributeSet set2 = 
        new HashDocAttributeSet(OrientationRequested.LANDSCAPE);
    DocAttributeSet set3 = new HashDocAttributeSet(set2);
    DocAttribute [] arr = {OrientationRequested.LANDSCAPE,
                           MediaName.NA_LETTER_WHITE};
    DocAttributeSet set4 = new HashDocAttributeSet(arr);
    Attribute [] attrArr;

    assertTrue(set1.isEmpty());
    assertFalse(set2.isEmpty());
    assertTrue(set3.equals(set2));
    assertFalse(set3.equals(set1));
    set3.clear();
    assertEquals(set3, set1);
    set3.add(OrientationRequested.LANDSCAPE);
    set3.add(MediaName.NA_LETTER_WHITE);
    assertTrue(set3.containsKey(OrientationRequested.LANDSCAPE.getClass()));
    assertFalse(set2.containsKey(MediaName.NA_LETTER_WHITE.getClass()));
    assertTrue(set3.containsValue(OrientationRequested.LANDSCAPE));
    assertFalse(set3.containsValue(OrientationRequested.PORTRAIT));
    assertFalse(set3.containsValue(PrintQuality.DRAFT));
    assertEquals(set1.size(), 0);
    assertEquals(set2.size(), 1);
    assertEquals(set3.size(), 2);
    assertTrue(set4.equals(set3));
    assertEquals(set3.get(OrientationRequested.PORTRAIT.getClass()),
                 OrientationRequested.LANDSCAPE);
    assertFalse((set3.get(OrientationRequested.PORTRAIT.getClass()))
            .equals(OrientationRequested.PORTRAIT));
    set1.addAll(set3);
    assertEquals(set3, set1);
    set1.remove(OrientationRequested.PORTRAIT.getClass());
    assertEquals(set1.size(), 1);
    attrArr = set1.toArray();
    assertEquals(attrArr.length, 1);
    assertEquals(attrArr[0], MediaName.NA_LETTER_WHITE);
}

public void testHashPrintJobAttributeSet() {
    startTest("HashPrintJobAttributeSet class testing...");

    PrintJobAttributeSet set1 = new HashPrintJobAttributeSet();
    PrintJobAttributeSet set2 = 
        new HashPrintJobAttributeSet(OrientationRequested.LANDSCAPE);
    PrintJobAttributeSet set3 = new HashPrintJobAttributeSet(set2);
    PrintJobAttribute [] arr = {OrientationRequested.LANDSCAPE,
                                MediaName.NA_LETTER_WHITE};
    PrintJobAttributeSet set4 = new HashPrintJobAttributeSet(arr);
    Attribute [] attrArr;
    
    assertTrue(set1.isEmpty());
    assertFalse(set2.isEmpty());
    assertTrue(set3.equals(set2));
    assertFalse(set3.equals(set1));
    set3.clear();
    assertEquals(set3, set1);
    set3.add(OrientationRequested.LANDSCAPE);
    set3.add(MediaName.NA_LETTER_WHITE);
    assertTrue(set3.containsKey(OrientationRequested.LANDSCAPE.getClass()));
    assertFalse(set2.containsKey(MediaName.NA_LETTER_WHITE.getClass()));
    assertTrue(set3.containsValue(OrientationRequested.LANDSCAPE));
    assertFalse(set3.containsValue(OrientationRequested.PORTRAIT));
    assertFalse(set3.containsValue(PrintQuality.DRAFT));
    assertEquals(set1.size(), 0);
    assertEquals(set2.size(), 1);
    assertEquals(set3.size(), 2);
    assertTrue(set4.equals(set3));
    assertEquals(set3.get(OrientationRequested.PORTRAIT.getClass()),
                 OrientationRequested.LANDSCAPE);
    assertFalse((set3.get(OrientationRequested.PORTRAIT.getClass()))
            .equals(OrientationRequested.PORTRAIT));
    set1.addAll(set3);
    assertEquals(set3, set1);
    set1.remove(OrientationRequested.PORTRAIT.getClass());
    assertEquals(set1.size(), 1);
    attrArr = set1.toArray();
    assertEquals(attrArr.length, 1);
    assertEquals(attrArr[0], MediaName.NA_LETTER_WHITE);
}

public void testHashPrintRequestAttributeSet() {
    startTest("HashPrintRequestAttributeSet class testing...");

    Copies copies = new Copies(2);
    PrintRequestAttributeSet set1 = new HashPrintRequestAttributeSet();
    PrintRequestAttributeSet set2 = new HashPrintRequestAttributeSet(copies);
    PrintRequestAttributeSet set3 = new HashPrintRequestAttributeSet(set2);
    PrintRequestAttribute [] arr = {copies, 
                                    MediaName.NA_LETTER_WHITE};
    PrintRequestAttributeSet set4 = new HashPrintRequestAttributeSet(arr);
    Attribute [] attrArr = set1.toArray();

    assertTrue(set1.isEmpty());
    assertFalse(set2.isEmpty());
    assertTrue(set3.equals(set2));
    assertFalse(set3.equals(set1));
    set3.clear();
    assertEquals(set3, set1);
    set3.add(copies);
    set3.add(MediaName.NA_LETTER_WHITE);
    assertTrue(set3.containsKey(copies.getClass()));
    assertFalse(set2.containsKey(MediaName.NA_LETTER_WHITE.getClass()));
    assertTrue(set3.containsValue(copies));
    assertFalse(set3.containsValue(OrientationRequested.PORTRAIT));
    assertFalse(set3.containsValue(PrintQuality.DRAFT));
    assertEquals(set1.size(), 0);
    assertEquals(set2.size(), 1);
    assertEquals(set3.size(), 2);
    assertTrue(set4.equals(set3));
    assertEquals(set3.get(copies.getClass()), copies);
    set1.addAll(set3);
    assertEquals(set3, set1);
    set1.remove(copies.getClass());
    assertEquals(set1.size(), 1);
    attrArr = set1.toArray();
    assertEquals(attrArr.length, 1);
    assertEquals(attrArr[0], MediaName.NA_LETTER_WHITE);
}

public void testHashPrintServiceAttributeSet() {
    startTest("HashPrintJobAttributeSet class testing...");

    PrintServiceAttributeSet set1 = new HashPrintServiceAttributeSet();
    PrintServiceAttributeSet set2 = new HashPrintServiceAttributeSet(
            ColorSupported.SUPPORTED);
    PrintServiceAttributeSet set3 = new HashPrintServiceAttributeSet(set2);
    PrinterLocation location = new PrinterLocation("room 222", Locale.ENGLISH);
    PrintServiceAttribute [] arr = { location, 
                                     ColorSupported.SUPPORTED };
    PrintServiceAttributeSet set4 = new HashPrintServiceAttributeSet(arr);
    
    assertTrue(set1.isEmpty());
    assertFalse(set2.isEmpty());
    assertTrue(set3.equals(set2));
    assertFalse(set3.equals(set1));
    set3.clear();
    assertEquals(set3, set1);
    set3.add(ColorSupported.SUPPORTED);
    set3.add(location);
    assertTrue(set3.containsKey(location.getClass()));
    assertFalse(set2.containsKey(MediaName.NA_LETTER_WHITE.getClass()));
    assertTrue(set4.equals(set3));
    assertEquals(set3.get(location.getClass()), location);
    set1.addAll(set3);
    assertEquals(set3, set1);
    set1.remove(location.getClass());
    assertEquals(set1.size(), 1);
}

// ---------------------------------------------------------------------------------
/* 
* This function search a PrintService which supports given 
* DocFlavor and PrintRequestAttributeSet.
* If default printer supports them, this function returns default printer.
* Overwise it returna first printer from all print services list.
*/
private PrintService getPrintService(DocFlavor aFlavor, 
                                     PrintRequestAttributeSet aSet)
{
    PrintService [] services = 
            PrintServiceLookup.lookupPrintServices(aFlavor, aSet);
    PrintService defaultService = 
            PrintServiceLookup.lookupDefaultPrintService();
    
    if (services.length <= 0) {
        System.out.println("Can not find default print service!");
        return null;
    }
    for (int i = 0; i < services.length; i++) {
        if (services[i].equals(defaultService)) {
            return services[i];
        }
    }
    System.out.println(
            "System Default PrintService does not support given attributes!");
    return services[0];
}

/*
 *  This functions returns a list of the available printers
*/
private String[] getPrintServices() {
    return (OS.startsWith("Windows")  
            ? getWindowsPrintServices() 
            : getLinuxPrintServices());
}

/*
 * This native function returns Windows printers list
 */
private native String [] getWindowsPrintServices();

/*
* This function returns Linus printers list.
* On Linux all CUPS printers are listed on /etc/printcap file
*/
private String [] getLinuxPrintServices() {
    Vector services = new Vector();
    String str = "";
    short state = 0;
    char c;
    String [] res;
    
    try {
        boolean flg = true;
        FileReader reader = new FileReader("/etc/printcap");
        while (flg) {
            c = (char) reader.read();
            if (( c <= 0) || (c == 65535)) {
                if (state == 2) {
                    services.add(str);
                }
                flg = false;
            }
            switch(state) {
                case 0:
                    if (c == '#') {
                        state = 1;
                    } else if (Character.isLetter(c)) {
                        str = str + c;
                        state = 2;
                    }
                    break;
                case 1:
                    if (c == '\n') {
                        state = 0;
                        str = "";
                    }
                    break;
                case 2:
                    if ((c == '|') || (c == '\n') || (c == ' ') || (c == 9)) {
                        services.add(str);
                        str = "";
                        state = (c == '\n') ? (short) 0 : (short) 1;
                    } else {
                        str = str + c;
                    }
                    break;
            }
        }
    } catch (Exception e) {
        e.printStackTrace();
        return null;
    }

    res = new String [services.size()];
    for (int i = 0; i < services.size(); i++) {
        res[i] = (String)services.get(i);
    }

    return res;
}

/*
 * This function returns system default printer
*/
private String getDefaultPrintService() {
    return OS.startsWith("Windows") 
           ? getDefaultWindowsPrintService() 
           : getDefaultLinuxPrintService();
}

/*
 * This function returns Windows default printer name
 */
private native String getDefaultWindowsPrintService();

/*
 * This function returns Linux default printer name
 * On Linux lpstat -d returns string like the following:
 * "system default destination: test"
 */
private String getDefaultLinuxPrintService() {
    try {
        Process process = Runtime.getRuntime().exec(
                "lpstat -d | grep \"system default destination\"");
        InputStream iStream;
        byte [] res;
        String resStr;
        
        process.waitFor();
        if (process.exitValue() != 0) {
            System.out.println("Can not exec \"lpstat -d\"");
            return null;
        }
        iStream = process.getInputStream();
        res = new byte [iStream.available()];
        iStream.read(res);
        resStr = new String(res);
        if (!resStr.startsWith("system default destination: ")) {
            System.out.println(
                    "WARNING: Can not recognize \"lpstat -d\" output:");
            System.out.println(resStr + "\n");
            return null;
        }
        // Last symbol in resStr is "\n"!
        return resStr.substring(28, resStr.length()-1);
    } catch (Exception e) {
        e.printStackTrace();
        return null;
    }
}

private void startTest(String s) {
    System.out.println("----------------------------------------");
    System.out.println(s);
}
}
