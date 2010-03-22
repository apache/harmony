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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;

import javax.print.DocFlavor;
import javax.print.PrintException;
import javax.print.attribute.PrintRequestAttributeSet;
import javax.print.attribute.standard.MediaSize;
import javax.print.attribute.standard.MediaSizeName;
import javax.print.attribute.standard.OrientationRequested;

/*
 * This class implements PostScript interpreter, which
 * converts PostScript instructions to GDI function calls.
 * Renders to specified printer's device context. 
 */
public class PSInterpreter {

    private GDIClient client = null;
    private String service = null;
    private int serviceID = -1;
    private PrintRequestAttributeSet attributes = null;
    
    private BufferedReader source = null;
    private ArrayList queue = new ArrayList();
    private String currentLine = null;
    private int lineIndex = 0;
    private int searchingIndex = 0;

    private int translateX = 0, translateY = 0;
    private int scaleWidth = -1, scaleHeight = -1;
    private double scaleX = 1.0, scaleY = 1.0;
    private boolean pathOpened = false;
    private boolean closedPathExists = false;
    private int logWidth = 0;
    private int logHeight = 0;
    
    private static final int COMMAND_MOVETO = 0;
    private static final int COMMAND_LINETO = 1;
    private static final int COMMAND_SETRGBCOLOR = 2;
    private static final int COMMAND_STROKE = 3;
    private static final int COMMAND_COLORIMAGE = 4;
    private static final int COMMAND_TRANSLATE = 5;
    private static final int COMMAND_SCALE = 6;
    private static final int COMMAND_ARC = 7;
    private static final int COMMAND_SHOW = 8;
    private static final int COMMAND_NEWPATH = 9;
    private static final int COMMAND_CLOSEPATH = 10;
    private static final int COMMAND_FILL = 11;
    private static final int COMMAND_FINDFONT = 12;
    private static final int COMMAND_SCALEFONT = 13;
    private static final int COMMAND_SETFONT = 14;
    private static final int COMMAND_ROTATE = 15;
    private static final int COMMAND_CLIP = 16;
    
    private static final String hexLetters = "0123456789abcdef";
    
    private static final String[] commands = {
        "moveto",
        "lineto",
        "setrgbcolor",
        "stroke",
        "colorimage",
        "translate",
        "scale",
        "arc",
        "show",
        "newpath",
        "closepath",
        "fill",
        "findfont",
        "scalefont",
        "setfont",
        "rotate",
        "clip"
    };

    private static final int[] commandsParams = {
        2, 2, 3, 0, 7, 2, 2, 5, 1, 0, 0, 0, 1, 1, 1, 0, 0
    };

    /*
     * Constructs new PSInterpreter instance.
     */
    public PSInterpreter(InputStream source, String service, GDIClient client,
            PrintRequestAttributeSet attributes) {
        this.source = new BufferedReader(new InputStreamReader(source));
        this.service = service;
        this.client = client;
        this.attributes = attributes;
        if (service != null) {
            getPaperDimensions(attributes);
            serviceID = obtainServiceID(service, logWidth, logHeight);
        }
    }
    
    /*
     * Sets up print service. Further rendering is made
     * to new print service's device context.
     */
    public void setPrintService(String service) {
        this.service = service;
        if (serviceID >= 0) {
            releaseServiceID(serviceID);
            serviceID = -1;
        }
        if (service != null) {
            getPaperDimensions(attributes);
            serviceID = obtainServiceID(service, logWidth, logHeight);
        }
    }
    
    /*
     * Starts parsing of source postscript and rendering
     * to print service's device context.
     */
    public void interpret() throws PrintException {
        if (serviceID < 0) {
            throw new PrintException(
                    "Unrecoverable internal error in GDI client.");
        }
        try {
            currentLine = source.readLine();
            while (currentLine != null) {
                if (currentLine.startsWith("%%EOF")) {
//                    /*
//                     * Code for debug. Prints additional page.
//                     */
//                    startPage(serviceID);
//                    setRGBColor(serviceID, 0.5, 0.5, 0.5);
//                    moveTo(serviceID, 50, 50);
//                    drawText(serviceID, "TEXT TEXT TEXT");
//                    endPage(serviceID);
                    if (!endDocument(serviceID)) {
                        releaseServiceID(serviceID);
                        serviceID = -1;
                        throw new PrintException(
                                "Unable to finish document printing.");
                    }
                    releaseServiceID(serviceID);
                    serviceID = -1;
                    return;
                } else if (currentLine.startsWith("%%")) {
                    interpretComment();
                } else if (currentLine.startsWith("%") ||
                           currentLine.startsWith("/")) {
                    // Nothing to do - simple comment.
                } else {
                    String lexem = getNextLexem();
                    while (lexem != null) {
//                        System.out.println("Lexem: " + lexem);
                        queue.add(lexem);
                        for (int i = 0; i < commands.length; i++) {
                            if (lexem.equals(commands[i])) {
                                interpretCommand(i);
                            }
                        }
                        lexem = getNextLexem();
                    }
                }
                currentLine = source.readLine();
                lineIndex = 0;
            }
            endDocument(serviceID);
        } catch (IOException ioe) {
            throw new PrintException(
                    "Unrecoverable internal error in GDI client.");
        }
    }
    
    private String getNextLexem() throws IOException {
        String lexem = getNextLexem(currentLine, lineIndex);
        if (lexem.endsWith(")") || lexem.endsWith("]")) {
            currentLine = source.readLine();
            lineIndex = 0;
            String prefix = (lexem.endsWith(")")) ? "( " : "[ ";
            String endOfLexem = getNextLexem(prefix + currentLine, lineIndex);
            if (lexem.length() == 1) {
                lexem = endOfLexem;
            } else {
                lexem = lexem.substring(0, lexem.length() - 1) + endOfLexem;
            }
            lineIndex = searchingIndex - 2;
        } else if (lexem.equals("}")) {
            while (lexem.equals("}")) {
                currentLine = source.readLine();
                lineIndex = 0;
                lexem = getNextLexem("{" + currentLine, lineIndex);
                lineIndex = searchingIndex;
            }
        } else {
            lineIndex = searchingIndex;
            if (lexem.equals("") && lineIndex >= currentLine.length()) {
                return null;
            }
        }
        return lexem;
    }

    private String getNextLexem(String line, int index) {
        String lexem = "";
        if (index < line.length()) {
            String character = line.substring(index++, index);
            while (index < line.length() && character.equals(" ")) {
                character = line.substring(index++, index);
            }
            if (!character.equals(" ")) {
                if (character.equals("%")) {
                    searchingIndex = line.length();
                    return lexem;
                } else if (character.equals("(")) {
                    if (index >= line.length()) {
                        searchingIndex = index;
                        return ")";
                    }
                    character = line.substring(index++, index);
                    while (!character.equals(")")) {
                        lexem += character;
                        if (index < line.length()) {
                            character = line.substring(index++, index);
                        } else {
                            lexem += ")";
                            break;
                        }
                    }
                } else if (character.equals("[")) {
                    if (index >= line.length()) {
                        searchingIndex = index;
                        return "]";
                    }
                    character = line.substring(index++, index);
                    while (!character.equals("]")) {
                        lexem += character;
                        if (index < line.length()) {
                            character = line.substring(index++, index);
                        } else {
                            lexem += "]";
                            break;
                        }
                    }
                } else if (character.equals("{")) {
                    if (index >= line.length()) {
                        searchingIndex = index;
                        return "}";
                    }
                    character = line.substring(index++, index);
                    while (!character.equals("}")) {
                        if (index < line.length()) {
                            character = line.substring(index++, index);
                        } else {
                            lexem = "}";
                            break;
                        }
                    }
                } else {
                    if (index >= line.length()) {
                        searchingIndex = index;
                        return character;
                    }
                    while (!character.equals(" ")) {
                        lexem += character;
                        if (index < line.length()) {
                            character = line.substring(index++, index);
                        } else {
                            break;
                        }
                    }
                }
            }
        }
        searchingIndex = index;
        return lexem;
    }

    private String getNextHexLetter() throws IOException {
        String hex = null;
        if (lineIndex < currentLine.length()) {
            String character =
                currentLine.substring(lineIndex++, lineIndex).toLowerCase();
            while (hexLetters.indexOf(character) < 0) {
                if (lineIndex < currentLine.length()) {
                    character =
                        currentLine.substring(lineIndex++,
                                              lineIndex).toLowerCase();
                } else {
                    currentLine = source.readLine();
                    lineIndex = 0;
                    return getNextHexLetter();
                }
            }
            hex = character;
        } else {
            currentLine = source.readLine();
            lineIndex = 0;
            return getNextHexLetter();
        }
        return hex;
    }

    private String getNextHex() throws IOException {
        return getNextHexLetter() + getNextHexLetter();
    }
    
    private int hex2decimal(String hex) {
        int multiplier = 1;
        int decimal = 0;
        for (int i = hex.length() - 1; i >= 0; i--) {
            decimal += hexLetters.indexOf(hex.substring(i, i + 1)) * multiplier;
            multiplier *= 16; 
        }
        return decimal;
    }
    
    private void interpretComment() throws PrintException {
        if (currentLine.startsWith("%%Page:")) {
            try {
                String pageName = getNextLexem(currentLine, 7);
                String pageNumber = getNextLexem(currentLine, searchingIndex);
                int number = Integer.parseInt(pageNumber);
                if (!startPage(serviceID)) {
                    endDocument(serviceID);
                    throw new PrintException("Unable to start page printing.");
                }
            } catch (NumberFormatException nfe) {
                System.out.println("NumberFormatException occured: " + nfe);
                nfe.printStackTrace(System.out);
            }
        } else if (currentLine.startsWith("%%EndPage:")) {
            try {
                String pageName = getNextLexem(currentLine, 10);
                String pageNumber = getNextLexem(currentLine, searchingIndex);
                int number = Integer.parseInt(pageNumber);
                if (pathOpened) {
                    closePath(serviceID);
                    pathOpened = false;
                }
                endPage(serviceID);
            } catch (NumberFormatException nfe) {
                System.out.println("NumberFormatException occured: " + nfe);
                nfe.printStackTrace(System.out);
            }
        } else if (currentLine.startsWith("%%EndSetup") ||
                   currentLine.startsWith("%%EndComments")) {
            if (!startDocument(service, serviceID, client.convertAttributes(
                    attributes,
                    new DocFlavor.INPUT_STREAM("INTERNAL/postscript")),
                    client.getJobName(attributes),
                    client.getDestination(attributes))) {
                throw new PrintException("Unable to start document printing.");
            }
        }
    }

    private void interpretCommand(int command) throws IOException {
        if (queue.size() < commandsParams[command] + 1) {
            System.out.println("Not enough parameters for PS command "
                    + commands[command]);
            return;
        }
        queue.remove(queue.size() - 1);
        switch (command) {
            case COMMAND_MOVETO:
                try {
                    double x, y;
                    y = Double.parseDouble(extractQueueLast());
                    x = Double.parseDouble(extractQueueLast());
                    if (!pathOpened) {
                        beginPath(serviceID);
                        pathOpened = true;
                    }
                    moveTo(serviceID, x, y);
                } catch (NumberFormatException nfe) {
                    System.out.println("NumberFormatException occured: " + nfe);
                    nfe.printStackTrace(System.out);
                }
                break;
            case COMMAND_LINETO:
                try {
                    double x, y;
                    y = Double.parseDouble(extractQueueLast());
                    x = Double.parseDouble(extractQueueLast());
                    if (!pathOpened) {
                        beginPath(serviceID);
                        pathOpened = true;
                    }
                    lineTo(serviceID, x, y);
                } catch (NumberFormatException nfe) {
                    System.out.println("NumberFormatException occured: " + nfe);
                    nfe.printStackTrace(System.out);
                }
                break;
            case COMMAND_SETRGBCOLOR:
                try {
                    double r, g, b;
                    b = Double.parseDouble(extractQueueLast());
                    g = Double.parseDouble(extractQueueLast());
                    r = Double.parseDouble(extractQueueLast());
                    setRGBColor(serviceID, r, g, b);
                } catch (NumberFormatException nfe) {
                    System.out.println("NumberFormatException occured: " + nfe);
                    nfe.printStackTrace(System.out);
                }
                break;
            case COMMAND_NEWPATH:
                beginPath(serviceID);
                pathOpened = true;
                break;
            case COMMAND_CLOSEPATH:
                if (pathOpened) {
                    closePath(serviceID);
                    pathOpened = false;
                    closedPathExists = true;
                }
                break;
            case COMMAND_STROKE:
                if (pathOpened) {
                    closePath(serviceID);
                    strokePath(serviceID);
                    pathOpened = false;
                } else if (closedPathExists) {
                    strokePath(serviceID);
                    closedPathExists = false;
                }
                break;
            case COMMAND_FILL:
                if (pathOpened) {
                    closePath(serviceID);
                    fillPath(serviceID);
                    pathOpened = false;
                } else if (closedPathExists) {
                    fillPath(serviceID);
                    closedPathExists = false;
                }
                break;
            case COMMAND_COLORIMAGE:
                if (extractQueueLast().equals("3") &&
                    extractQueueLast().equals("false")) {
                    try {
                        extractQueueLast(); // removing reading procedure
                        String imageParams = extractQueueLast();
                        int depth = Integer.parseInt(extractQueueLast());
                        int height = Integer.parseInt(extractQueueLast());
                        int width = Integer.parseInt(extractQueueLast());
                        if (depth == 8) {
                            int[] imageData = new int[width * height];
                            for (int j = 0; j < height; j++) {
                                for (int i = 0; i < width; i++) {
                                    int color = hex2decimal(getNextHex() +
                                                            getNextHex() +
                                                            getNextHex());
                                    imageData[j * width + i] = color;
                                }
                            }
                            if (scaleWidth < 0 || scaleHeight < 0) {
                                drawImage(serviceID, translateX, translateY,
                                       width * scaleX, height * scaleY,
                                       imageData, width, height);
                            } else {
                                drawImage(serviceID, translateX, translateY,
                                        scaleWidth, scaleHeight,
                                        imageData, width, height);
                            }
                        }
                    } catch (NumberFormatException nfe) {
                        System.out.println("NumberFormatException occured: " +
                                nfe);
                        nfe.printStackTrace(System.out);
                    }
                }
                break;
            case COMMAND_TRANSLATE:
                try {
                    translateY = Integer.parseInt(extractQueueLast());
                    translateX = Integer.parseInt(extractQueueLast());
                } catch (NumberFormatException nfe) {
                    System.out.println("NumberFormatException occured: " + nfe);
                    nfe.printStackTrace(System.out);
                }
                break;
            case COMMAND_SCALE:
                try {
                    scaleY = Double.parseDouble(extractQueueLast());
                    scaleX = Double.parseDouble(extractQueueLast());
                } catch (NumberFormatException nfe) {
                    System.out.println("NumberFormatException occured: " + nfe);
                    nfe.printStackTrace(System.out);
                }
                break;
            case COMMAND_ARC:
                try {
                    double x, y, r, a, b;
                    b = Double.parseDouble(extractQueueLast());
                    a = Double.parseDouble(extractQueueLast());
                    r = Double.parseDouble(extractQueueLast());
                    y = Double.parseDouble(extractQueueLast());
                    x = Double.parseDouble(extractQueueLast());
                    if (!pathOpened) {
                        beginPath(serviceID);
                        pathOpened = true;
                    }
                    drawArc(serviceID, x, y, r, a, b);
                } catch (NumberFormatException nfe) {
                    System.out.println("NumberFormatException occured: " + nfe);
                    nfe.printStackTrace(System.out);
                }
                break;
            case COMMAND_SHOW:
                String text = extractQueueLast();
                drawText(serviceID, text);
                break;
            default:
        }
    }

    private String extractQueueLast() {
        String lexem = (String)queue.get(queue.size() - 1);
        queue.remove(queue.size() - 1);
        return lexem;
    }

    private void getPaperDimensions(PrintRequestAttributeSet attrs) {
        MediaSize size = null;
        if (attrs != null) {
            if (attrs.containsKey(MediaSize.class)) {
                size = (MediaSize) attrs.get(MediaSize.class);
            } else if (attrs.containsKey(MediaSizeName.class)) {
                MediaSizeName name =
                    (MediaSizeName)attrs.get(MediaSizeName.class);
                size = MediaSize.getMediaSizeForName(name);
            } else {
                size = MediaSize.getMediaSizeForName(MediaSizeName.ISO_A4);
            }
        } else {
            size = MediaSize.getMediaSizeForName(MediaSizeName.ISO_A4);
        }
        logWidth = (int) (size.getX(MediaSize.INCH) * 72.0);
        logHeight = (int) (size.getY(MediaSize.INCH) * 72.0);
        if (attributes != null) {
            if (attributes.containsValue(
                    OrientationRequested.LANDSCAPE)) {
                int temp = logWidth;
                logWidth = logHeight;
                logHeight = temp;
            }
        }
    }
    
    private synchronized static native int obtainServiceID(String name,
            int width, int height);
    private synchronized static native void releaseServiceID(int serviceID);
    private static native boolean startDocument(String name, int serviceID,
            int[] attributes, String jobName, String destination);
    private static native boolean startPage(int serviceID);
    private static native boolean endPage(int serviceID);
    private static native boolean endDocument(int serviceID);
    
    private static native boolean setRGBColor(int serviceID, double red,
            double green, double blue);
    private static native boolean moveTo(int serviceID, double x, double y);
    private static native boolean lineTo(int serviceID, double x, double y);
    private static native boolean drawArc(int serviceID, double x, double y,
            double r, double a, double b);
    private static native boolean drawText(int serviceID, String text);
    private static native boolean drawImage(int serviceID, double x, double y,
            double scalew, double scaleh, int[] image, int w, int h);
    private static native boolean beginPath(int serviceID);
    private static native boolean closePath(int serviceID);
    private static native boolean strokePath(int serviceID);
    private static native boolean fillPath(int serviceID);
    private static native boolean clipPath(int serviceID);
    private static native boolean rotate(int serviceID, double alpha);
    private static native boolean setFont(int serviceID, String font);
}
