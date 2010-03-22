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
 * @author Michael Danilov
 */
package org.apache.harmony.awt.datatransfer;

import java.awt.datatransfer.DataFlavor;
import java.util.Comparator;

/**
 * Flavors comparator. Used for sorting text flavors.
 */
public class FlavorsComparator implements Comparator<DataFlavor> {

    public int compare(DataFlavor flav1, DataFlavor flav2) {

        if (!flav1.isFlavorTextType() && !flav2.isFlavorTextType()) {
            return 0;
        } else if (!flav1.isFlavorTextType() && flav2.isFlavorTextType()) {
            return -1;
        } else if (flav1.isFlavorTextType() && !flav2.isFlavorTextType()) {
            return 1;
        } else {
            DataFlavor df = DataFlavor.selectBestTextFlavor(new DataFlavor[] { flav1, flav2 });
            return (df == flav1) ? -1 : 1;
        }
    }

}
