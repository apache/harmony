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
package javax.imageio.metadata;

import java.util.ArrayList;

import org.apache.harmony.x.imageio.metadata.IIOMetadataUtils;
import org.w3c.dom.Node;

public abstract class IIOMetadata {

    protected boolean standardFormatSupported;
    protected String nativeMetadataFormatName;
    protected String nativeMetadataFormatClassName;
    protected String[] extraMetadataFormatNames;
    protected String[] extraMetadataFormatClassNames;
    protected IIOMetadataController defaultController;
    protected IIOMetadataController controller;

    protected IIOMetadata() {}

    protected IIOMetadata(boolean standardMetadataFormatSupported,
                          String nativeMetadataFormatName,
                          String nativeMetadataFormatClassName,
                          String[] extraMetadataFormatNames,
                          String[] extraMetadataFormatClassNames) {
        standardFormatSupported = standardMetadataFormatSupported;
        this.nativeMetadataFormatName = nativeMetadataFormatName;
        this.nativeMetadataFormatClassName = nativeMetadataFormatClassName;
        if (extraMetadataFormatNames == null) {
            if (extraMetadataFormatClassNames != null) {
                throw new IllegalArgumentException(
                        "extraMetadataFormatNames == null && extraMetadataFormatClassNames != null!"
                );
            }
        } else {
            if (extraMetadataFormatClassNames == null) {
                throw new IllegalArgumentException(
                        "extraMetadataFormatNames != null && extraMetadataFormatClassNames == null!"
                );
            }
            if (extraMetadataFormatNames.length == 0) {
                throw new IllegalArgumentException("extraMetadataFormatNames.length == 0!");
            }
            if (extraMetadataFormatClassNames.length != extraMetadataFormatNames.length) {
                throw new IllegalArgumentException(
                        "extraMetadataFormatClassNames.length != extraMetadataFormatNames.length!"
                );
            }
            this.extraMetadataFormatNames = extraMetadataFormatNames.clone();
            this.extraMetadataFormatClassNames = extraMetadataFormatClassNames.clone();
        }
    }

    public abstract Node getAsTree(String formatName);
    public abstract boolean isReadOnly();
    public abstract void mergeTree(String formatName, Node root) throws IIOInvalidTreeException;
    public abstract void reset();

    public IIOMetadataController getController() {
        return controller;
    }

    public boolean hasController() {
        return getController() != null;
    }

    public boolean activateController() {
        if (!hasController()) {
            throw new IllegalStateException("hasController() == false!");
        }
        return getController().activate(this);
    }

    public IIOMetadataController getDefaultController() {
        return defaultController;
    }

    public String[] getExtraMetadataFormatNames() {
        return extraMetadataFormatNames == null ? null : extraMetadataFormatNames.clone();
    }

    public IIOMetadataFormat getMetadataFormat(String formatName) {
        return IIOMetadataUtils.instantiateMetadataFormat(
                formatName,
                standardFormatSupported,
                nativeMetadataFormatName, nativeMetadataFormatClassName,
                extraMetadataFormatNames, extraMetadataFormatClassNames
        );
    }

    public String getNativeMetadataFormatName() {
        return nativeMetadataFormatName;
    }

    public boolean isStandardMetadataFormatSupported() {
        return standardFormatSupported;
    }

    public String[] getMetadataFormatNames() {
        ArrayList<String> res = new ArrayList<String>();

        String nativeMetadataFormatName = getNativeMetadataFormatName();
        boolean standardFormatSupported = isStandardMetadataFormatSupported();
        String extraMetadataFormatNames[] = getExtraMetadataFormatNames();

        if (standardFormatSupported) {
            res.add(IIOMetadataFormatImpl.standardMetadataFormatName);
        }
        if (nativeMetadataFormatName != null) {
            res.add(nativeMetadataFormatName);
        }
        if (extraMetadataFormatNames != null) {
            for (String extraMetadataFormatName : extraMetadataFormatNames) {
                res.add(extraMetadataFormatName);
            }
        }

        return res.size() > 0 ? res.toArray(new String[0]) : null;
    }

    protected IIOMetadataNode getStandardChromaNode() {
        return null;
    }

    protected IIOMetadataNode getStandardCompressionNode() {
        return null;
    }

    protected IIOMetadataNode getStandardDataNode() {
        return null;
    }

    protected IIOMetadataNode getStandardDimensionNode() {
        return null;
    }

    protected IIOMetadataNode getStandardDocumentNode() {
        return null;
    }

    protected IIOMetadataNode getStandardTextNode() {
        return null;
    }

    protected IIOMetadataNode getStandardTileNode() {
        return null;
    }

    protected IIOMetadataNode getStandardTransparencyNode() {
        return null;
    }

    protected final IIOMetadataNode getStandardTree() {
        // Create root node
        IIOMetadataNode root = new IIOMetadataNode(IIOMetadataFormatImpl.standardMetadataFormatName);

        Node node;
        if ((node = getStandardChromaNode()) != null) {
            root.appendChild(node);
        }
        if ((node = getStandardCompressionNode()) != null) {
            root.appendChild(node);
        }
        if ((node = getStandardDataNode()) != null) {
            root.appendChild(node);
        }
        if ((node = getStandardDimensionNode()) != null) {
            root.appendChild(node);
        }
        if ((node = getStandardDocumentNode()) != null) {
            root.appendChild(node);
        }
        if ((node = getStandardTextNode()) != null) {
            root.appendChild(node);
        }
        if ((node = getStandardTileNode()) != null) {
            root.appendChild(node);
        }
        if ((node = getStandardTransparencyNode()) != null) {
            root.appendChild(node);
        }
        
        return root;
    }

    public void setController(IIOMetadataController controller) {
        this.controller = controller;
    }

    public void setFromTree(String formatName, Node root) throws IIOInvalidTreeException {
        reset();
        mergeTree(formatName, root);
    }
}
