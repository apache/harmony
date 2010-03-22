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
 * @author Rustem V. Rafikov
 */
package javax.imageio;

import javax.imageio.metadata.IIOMetadata;
import java.awt.image.RenderedImage;
import java.awt.image.Raster;
import java.awt.image.BufferedImage;
import java.util.List;

public class IIOImage {

    protected RenderedImage image;
    protected Raster raster;
    protected List<? extends BufferedImage> thumbnails;
    protected IIOMetadata metadata;

    public IIOImage(RenderedImage image, List<? extends BufferedImage> thumbnails, IIOMetadata metadata) {
        if (image == null) {
            throw new IllegalArgumentException("image should not be NULL");
        }
        this.raster = null;
        this.image = image;
        this.thumbnails = thumbnails;
        this.metadata = metadata;
    }

    public IIOImage(Raster raster, List<? extends BufferedImage> thumbnails, IIOMetadata metadata) {
        if (raster == null) {
            throw new IllegalArgumentException("raster should not be NULL");
        }
        this.image = null;
        this.raster = raster;
        this.thumbnails = thumbnails;
        this.metadata = metadata;
    }

    public RenderedImage getRenderedImage() {
        return image;
    }

    public void setRenderedImage(RenderedImage image) {
        if (image == null) {
            throw new IllegalArgumentException("image should not be NULL");
        }
        raster = null;
        this.image = image;
    }

    public boolean hasRaster() {
        return raster != null;
    }

    public Raster getRaster() {
        return raster;
    }

    public void setRaster(Raster raster) {
        if (raster == null) {
            throw new IllegalArgumentException("raster should not be NULL");
        }
        image = null;
        this.raster = raster;
    }

    public int getNumThumbnails() {
        return thumbnails != null ? thumbnails.size() : 0;
    }

    public BufferedImage getThumbnail(int index) {
        if (thumbnails != null) {
            return thumbnails.get(index);
        }
        throw new IndexOutOfBoundsException("no thumbnails were set");
    }

    public List<? extends BufferedImage> getThumbnails() {
        return thumbnails;
    }

    public void setThumbnails(List<? extends BufferedImage> thumbnails) {
        this.thumbnails = thumbnails;
    }

    public IIOMetadata getMetadata() {
        return metadata;
    }

    public void setMetadata(IIOMetadata metadata) {
        this.metadata = metadata;
    }
}
