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

package org.apache.harmony.x.imageio.plugins.png;

import java.util.Arrays;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.metadata.IIOMetadataFormat;
import javax.imageio.metadata.IIOMetadataFormatImpl;

public class PNGMetadataFormat extends IIOMetadataFormatImpl {
    
    private static IIOMetadataFormat instance = null;
    
    private PNGMetadataFormat() {
        super(PNGSpiConsts.nativeImageMetadataFormatName, 
              CHILD_POLICY_SOME);
        
        addElement("IHDR", 
                   PNGSpiConsts.nativeImageMetadataFormatName,
                   CHILD_POLICY_EMPTY);
        addAttribute("IHDR", "width", DATATYPE_INTEGER, true, null, 
                     "1", "2147483647", true, true);
        addAttribute("IHDR", "height", DATATYPE_INTEGER, true, null,
                     "1", "2147483647", true, true);
        String[] bitDepths = {"1", "2", "4", "8", "16"};
        addAttribute("IHDR", "bitDepth", DATATYPE_INTEGER, true, null,
                     Arrays.asList(bitDepths));
        String[] colorTypes = {"Grayscale", "RGB", "Palette", 
                              "GrayAlpha", "RGBAlpha"};
        addAttribute("IHDR", "colorType", DATATYPE_STRING, true, null,
                     Arrays.asList(colorTypes));
        String[] compressionMethods = {"deflate"};
        addAttribute("IHDR", "compressionMethod", DATATYPE_STRING, true, null,
                     Arrays.asList(compressionMethods));
        String[] filterMethods = {"adaptive"};
        addAttribute("IHDR", "filterMethod", DATATYPE_STRING, true, null,
                     Arrays.asList(filterMethods));
        String[] interlaceMethods = {"none", "adam7"};
        addAttribute("IHDR", "interlaceMethod", DATATYPE_STRING, true, null,
                     Arrays.asList(interlaceMethods));
        
        addElement("PLTE", 
                   PNGSpiConsts.nativeImageMetadataFormatName,
                   1, 256);
        
        addElement("PLTEEntry", "PLTE", CHILD_POLICY_EMPTY);
        addAttribute("PLTEEntry", "index", DATATYPE_INTEGER, true, null,
                     "0", "255", true, true);
        addAttribute("PLTEEntry", "red", DATATYPE_INTEGER, true, null,
                     "0", "255", true, true);
        addAttribute("PLTEEntry", "green", DATATYPE_INTEGER, true, null,
                    "0", "255", true, true);
        addAttribute("PLTEEntry", "blue", DATATYPE_INTEGER, true, null,
                    "0", "255", true, true);
        
        addElement("bKGD",
                   PNGSpiConsts.nativeImageMetadataFormatName,
                   CHILD_POLICY_CHOICE);
        
        addElement("bKGD_Grayscale", "bKGD", CHILD_POLICY_EMPTY);
        addAttribute("bKGD_Grayscale", "gray", DATATYPE_INTEGER, true, null,
                     "0", "65535", true, true);
        
        addElement("bKGD_RGB", "bKGD", CHILD_POLICY_EMPTY);
        addAttribute("bKGD_RGB", "red", DATATYPE_INTEGER, true, null,
                     "0", "65535", true, true);
        addAttribute("bKGD_RGB", "green", DATATYPE_INTEGER, true, null,
                     "0", "65535", true, true);
        addAttribute("bKGD_RGB", "blue", DATATYPE_INTEGER, true, null,
                     "0", "65535", true, true);
        
        addElement("bKGD_palette", "bKGD", CHILD_POLICY_EMPTY);
        addAttribute("bKGD_Palette", "index", DATATYPE_INTEGER, true, null,
                     "0", "255", true, true);
        
        addElement("cHRM",
                   PNGSpiConsts.nativeImageMetadataFormatName,
                   CHILD_POLICY_EMPTY);
        addAttribute("cHRM", "whitePointX", DATATYPE_INTEGER, true, null,
                     "0", "65535", true, true);
        addAttribute("cHRM", "whitePointY", DATATYPE_INTEGER, true, null,
                     "0", "65535", true, true);
        addAttribute("cHRM", "redX", DATATYPE_INTEGER, true, null,
                     "0", "65535", true, true);
        addAttribute("cHRM", "redY", DATATYPE_INTEGER, true, null,
                     "0", "65535", true, true);
        addAttribute("cHRM", "greenX", DATATYPE_INTEGER, true, null,
                     "0", "65535", true, true);
        addAttribute("cHRM", "greenY", DATATYPE_INTEGER, true, null,
                     "0", "65535", true, true);
        addAttribute("cHRM", "blueX", DATATYPE_INTEGER, true, null,
                     "0", "65535", true, true);
        addAttribute("cHRM", "blueY", DATATYPE_INTEGER, true, null,
                     "0", "65535", true, true);
        
        addElement("gAMA",
                   PNGSpiConsts.nativeImageMetadataFormatName,
                   CHILD_POLICY_EMPTY);
        addAttribute("gMAM", "value", DATATYPE_INTEGER, true, null,
                     "0", "2147483647", true, true);
        
        addElement("hIST",
                   PNGSpiConsts.nativeImageMetadataFormatName,
                   1, 256);
        
        addElement("hISTEntry", "hIST", CHILD_POLICY_EMPTY);
        addAttribute("hISTEntry", "index", DATATYPE_INTEGER, true, null,
                     "0", "255", true, true);
        addAttribute("hISTEntry", "value", DATATYPE_INTEGER, true, null,
                     "0", "65535", true, true);
        
        addElement("iCCP",
                   PNGSpiConsts.nativeImageMetadataFormatName,
                   CHILD_POLICY_EMPTY);
        addObjectValue("iCCP", byte.class, 0, Integer.MAX_VALUE);
        addAttribute("iCCP", "profileName", DATATYPE_STRING, true, null);
        String[] iCCPCompressionMethods = {"deflate"};
        addAttribute("iCCP", "compressionMethod", DATATYPE_STRING, true, null,
                     Arrays.asList(iCCPCompressionMethods));
        
        addElement("iTXt",
                   PNGSpiConsts.nativeImageMetadataFormatName,
                   1, Integer.MAX_VALUE);
        
        addElement("iTXtEntry", "iTXt", CHILD_POLICY_EMPTY);
        addAttribute("iTXtEntry", "keyword", DATATYPE_STRING, true, null);
        addBooleanAttribute("iTXtEntry", "compressionFlag", false, false);
        addAttribute("iTXtEntry", "compressionMethod", DATATYPE_STRING, true, null);
        addAttribute("iTXtEntry", "languageTag", DATATYPE_STRING, true, null);
        addAttribute("iTXtEntry", "translatedKeyword", DATATYPE_STRING, true, null);
        addAttribute("iTXtEntry", "text", DATATYPE_STRING, true, null);
        
        addElement("pHYS",
                   PNGSpiConsts.nativeImageMetadataFormatName,
                   CHILD_POLICY_EMPTY);
        addAttribute("pHYS", "pixelsPerUnitXAxis", DATATYPE_INTEGER, true, null,
                     "0", "2147483647", true, true);
        addAttribute("pHYS", "pixelsPerUnitYAxis", DATATYPE_INTEGER, true, null,
                     "0", "2147483647", true, true);
        String[] unitSpecifiers = {"unknown", "meter"};
        addAttribute("pHYS", "unitSpecifier", DATATYPE_STRING, true, null,
                     Arrays.asList(unitSpecifiers));
        
        addElement("sBIT",
                   PNGSpiConsts.nativeImageMetadataFormatName,
                   CHILD_POLICY_CHOICE);
        
        addElement("sBIT_Grayscale", "sBIT", CHILD_POLICY_EMPTY);
        addAttribute("sBIT_Grayscale", "gray", DATATYPE_INTEGER, true, null,
                     "0", "255", true, true);
        
        addElement("sBIT_GrayAlpha", "sBIT", CHILD_POLICY_EMPTY);
        addAttribute("sBIT_GrayAlpha", "gray", DATATYPE_INTEGER, true, null,
                     "0", "255", true, true);
        addAttribute("sBIT_GrayAlpha", "alpha", DATATYPE_INTEGER, true, null,
                     "0", "255", true, true);
        
        addElement("sBIT_RGB", "sBIT", CHILD_POLICY_EMPTY);
        addAttribute("sBIT_RGB", "red", DATATYPE_INTEGER, true, null,
                     "0", "255", true, true);
        addAttribute("sBIT_RGB", "green", DATATYPE_INTEGER, true, null,
                     "0", "255", true, true);
        addAttribute("sBIT_RGB", "blue", DATATYPE_INTEGER, true, null,
                     "0", "255", true, true);
        
        addElement("sBIT_RGBAlpha", "sBIT", CHILD_POLICY_EMPTY);
        addAttribute("sBIT_RGBAlpha", "red", DATATYPE_INTEGER, true, null,
                     "0", "255", true, true);
        addAttribute("sBIT_RGBAlpha", "green", DATATYPE_INTEGER, true, null,
                     "0", "255", true, true);
        addAttribute("sBIT_RGBAlpha", "blue", DATATYPE_INTEGER, true, null,
                     "0", "255", true, true);
        addAttribute("sBIT_RGBAlpha", "alpha", DATATYPE_INTEGER, true, null,
                     "0", "255", true, true);
        
        addElement("sBIT_Palette", "sBIT", CHILD_POLICY_EMPTY);
        addAttribute("sBIT_Palette", "red", DATATYPE_INTEGER, true, null,
                     "0", "255", true, true);
        addAttribute("sBIT_Palette", "green", DATATYPE_INTEGER, true, null,
                     "0", "255", true, true);
        addAttribute("sBIT_Palette", "blue", DATATYPE_INTEGER, true, null,
                     "0", "255", true, true);
        
        addElement("sPLT",
                   PNGSpiConsts.nativeImageMetadataFormatName,
                   1, 256);
        
        addElement("sPLTEntry", "sPLT", CHILD_POLICY_EMPTY);
        addAttribute("sPLTEntry", "index", DATATYPE_INTEGER, true, null,
                     "0", "255", true, true);
        addAttribute("sPLTEntry", "red", DATATYPE_INTEGER, true, null,
                     "0", "255", true, true);
        addAttribute("sPLTEntry", "green", DATATYPE_INTEGER, true, null,
                     "0", "255", true, true);
        addAttribute("sPLTEntry", "blue", DATATYPE_INTEGER, true, null,
                     "0", "255", true, true);
        addAttribute("sPLTEntry", "alpha", DATATYPE_INTEGER, true, null,
                     "0", "255", true, true);
        
        addElement("sRGB",
                   PNGSpiConsts.nativeImageMetadataFormatName,
                   CHILD_POLICY_EMPTY);
        String[] renderingIntents = {"Perceptual", 
                                    "Relative colorimetric", 
                                    "Saturation",
                                    "Absolute colorimetric"};
        addAttribute("sRGB", "renderingIntent", DATATYPE_STRING, true, null,
                     Arrays.asList(renderingIntents));
        
        addElement("tEXt",
                   PNGSpiConsts.nativeImageMetadataFormatName,
                   1, Integer.MAX_VALUE);
        
        addElement("tEXtEntry", "tEXt", CHILD_POLICY_EMPTY);
        addAttribute("eTXtEntry", "keyword", DATATYPE_STRING, true, null);
        addAttribute("eTXtEntry", "value", DATATYPE_STRING, true, null);
        
        addElement("tIME",
                   PNGSpiConsts.nativeImageMetadataFormatName,
                   CHILD_POLICY_EMPTY);
        addAttribute("tIME", "year", DATATYPE_INTEGER, true, null,
                     "0", "65535", true, true);
        addAttribute("tIME", "month", DATATYPE_INTEGER, true, null,
                     "1", "12", true, true);
        addAttribute("tIME", "day", DATATYPE_INTEGER, true, null,
                     "1", "31", true, true);
        addAttribute("tIME", "hour", DATATYPE_INTEGER, true, null,
                     "0", "23", true, true);
        addAttribute("tIME", "minute", DATATYPE_INTEGER, true, null,
                     "0", "59", true, true);
        addAttribute("tIME", "second", DATATYPE_INTEGER, true, null,
                     "0", "60", true, true);
        
        addElement("tRNS",
                   PNGSpiConsts.nativeImageMetadataFormatName,
                   CHILD_POLICY_CHOICE);
        
        addElement("tRNS_Grayscale", "tRNS", CHILD_POLICY_EMPTY);
        addAttribute("tRNS_Grayscale", "gray", DATATYPE_INTEGER, true, null,
                     "0", "65535", true, true);
        
        addElement("tRNS_RGB", "tRNS", CHILD_POLICY_EMPTY);
        addAttribute("tRNS_RGB", "red", DATATYPE_INTEGER, true, null,
                     "0", "65535", true, true);
        addAttribute("tRNS_RGB", "green", DATATYPE_INTEGER, true, null,
                     "0", "65535", true, true);
        addAttribute("tRNS_RGB", "blue", DATATYPE_INTEGER, true, null,
                     "0", "65535", true, true);
        
        addElement("tRNS_Palette", "tRNS", CHILD_POLICY_EMPTY);
        addAttribute("tRNS_RGB", "index", DATATYPE_INTEGER, true, null,
                     "0", "255", true, true);
        addAttribute("tRNS_RGB", "alpha", DATATYPE_INTEGER, true, null,
                     "0", "255", true, true);
        
        addElement("zTXt",
                   PNGSpiConsts.nativeImageMetadataFormatName,
                   1, Integer.MAX_VALUE);
        
        addElement("zTXtEntry", "zTXt", CHILD_POLICY_EMPTY);
        addObjectValue("zTXtEntry", byte.class, 0, Integer.MAX_VALUE);
        addAttribute("zTXtEntry", "keyword", DATATYPE_STRING, true, null);
        String[] zTXtCompressionMethods = {"deflate"};
        addAttribute("zTXtEntry", "compressionMethod", DATATYPE_STRING, true, null,
                     Arrays.asList(zTXtCompressionMethods));
        
        addElement("UnknownChunks",
                   PNGSpiConsts.nativeImageMetadataFormatName,
                   1, Integer.MAX_VALUE);
        
        addElement("UnknownChunk", "UnknownChunks", CHILD_POLICY_EMPTY);
        addObjectValue("UnknownChunk", byte.class, 0, Integer.MAX_VALUE);
        addAttribute("UnknownChunk", "type", DATATYPE_STRING, true, null);
    }

    @Override
    public boolean canNodeAppear(String elementName,
            ImageTypeSpecifier imageType) {
        // A PLTE chunk may not appear in a Gray or GrayAlpha image
        // A tRNS chunk may not appear in GrayAlpha and RGBA images
        return true;
    }
    
    public static IIOMetadataFormat getInstance() {
        if (instance == null) {
            instance = new PNGMetadataFormat();
        }
        return instance;
    }
}
