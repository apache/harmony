/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package org.apache.harmony.tools.jarsigner;

/**
 * Class to build the base file names for .SF and .DSA files.
 * It is designed to be used only in JSParameters.
 */
class FileNameGenerator {
    private static final int fileNameLength = 8;
    private static final int maxExtLength = 3;
    
    /**
     * Generates the file name for .SF and .DSA files using
     * sigFileName or alias given on the command line. 
     * 
     * @param param
     * @return
     * @throws NullPointerException - if both parameters are null 
     */
    static String generateFileName(String sigFileName, String alias){
        if (sigFileName != null){
            return convertString(sigFileName.toUpperCase());
        }
        if (alias == null){
            throw new NullPointerException("Alias is null.");
        }
        int length = alias.length();
        if (length > fileNameLength){
            alias = alias.substring(0, fileNameLength);
            length = fileNameLength;
        } 
        
        alias = convertString(alias);
        return alias.toUpperCase();
    }
 
    /**
     *  Generates signature block file name, based on key algorithm.
     * 
     * @param sigFileName
     * @param keyAlg
     * @return
     */ 
    static String generateSigBlockName(String sigFileName, String keyAlg) {
        // make an extension
        String sigBlockFileExt;
        // max allowed extension length is 3 symbols
        if (keyAlg.length() > maxExtLength) {
            sigBlockFileExt = "."
                    + (keyAlg.substring(0, maxExtLength)).toUpperCase();
        } else {
            sigBlockFileExt = "." + keyAlg.toUpperCase();
        }

        // add a prefix if necessary
        if (keyAlg.equalsIgnoreCase("DSA") || keyAlg.equalsIgnoreCase("RSA")) {
            // no prefix
            return sigFileName + sigBlockFileExt;
        } else {
            // add prefix "SIG-"
            return "SIG-" + sigFileName + sigBlockFileExt;
        }
    }

    
    // Finds disallowed letters in input String and converts
    // them to underscores ("_"). Allowed characters are letters, digits,
    // hyphens and underscores. If no changes are made, the input string itself
    // is returned (not a copy!).
    private static String convertString(String input){
        char [] chars = input.toCharArray();
        boolean isChanged = false; 
        for (int i = 0; i < chars.length; i++){
            char current = chars[i];
            if ((current >= 'A' && current<= 'Z') || 
                    (current >= 'a' && current <= 'z') ||
                    (current >= '0' && current <= '9') ||
                    current == '-' || current == '_'){
                continue;
            }
            
            isChanged = true;
            chars[i] = '_';
        }
        if (isChanged){
            return new String(chars);
        } else {
            return input;
        }
    }
}

