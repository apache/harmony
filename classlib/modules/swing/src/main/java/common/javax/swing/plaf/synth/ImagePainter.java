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

package javax.swing.plaf.synth;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Insets;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;
import javax.swing.JComponent;

import org.apache.harmony.x.swing.internal.nls.Messages;
import org.xml.sax.SAXException;

class ImagePainter extends SynthPainter {

    /**
     * The array of images: parts of cropped source image
     */
    private BufferedImage[][] imageParts = new BufferedImage[3][3];

    /**
     * Below are painter parameters
     */
    private final Insets destinationInsets;

    private final boolean paintCenter;

    private final boolean stretch;

    /**
     * Below are private image parameters. Used in cropImage and paintImage
     * methods only
     */
    private int imageWidth;

    private int imageHeight;

    private int imageInsetsTop;

    private int imageInsetsLeft;

    private int imageInsetsRight;

    private int imageInsetsBottom;

    /**
     * XMLImagePainter is single for every image and parameters. i.e. for 2
     * images (the same image but different painting parameters) there are 2
     * ImagePainters
     */
    ImagePainter(String path, Insets sourceInsets, Insets destinationInsets,
            boolean paintCenter, boolean stretch, Class<?> base)
            throws IOException, SAXException {

        String imagePath = base.getResource(path).getPath();

        if ((imagePath == null) || (sourceInsets == null)) {
            throw new SAXException(Messages.getString("swing.err.23")); //$NON-NLS-1$
        }

        this.destinationInsets = destinationInsets;
        this.stretch = stretch;
        this.paintCenter = paintCenter;

        cropImage(ImageIO.read(new File(imagePath)), sourceInsets);
    }

    /**
     * Cropped image according sourceInsets and place the cuts into imageParts
     * array
     * 
     * @param input
     *            image to crop
     * @param cropping
     *            parameters
     */
    private void cropImage(BufferedImage input, Insets insets) {

        /* Define image parameters */
        imageWidth = input.getWidth();
        imageHeight = input.getHeight();
        imageInsetsTop = insets.top;
        imageInsetsLeft = insets.left;
        imageInsetsRight = insets.right;
        imageInsetsBottom = insets.bottom;

        if ((imageHeight < (imageInsetsTop + imageInsetsBottom))
                || (imageWidth < (imageInsetsLeft + imageInsetsRight))) {
            return;
        }

        /* Crop image into array of images */
        if ((imageInsetsTop > 0)) {

            if (imageInsetsLeft >= 0)

                imageParts[0][1] = input.getSubimage(imageInsetsLeft, 0,
                        imageWidth - imageInsetsRight - imageInsetsLeft,
                        imageInsetsTop);

            if (imageInsetsLeft > 0) {

                imageParts[0][0] = input.getSubimage(0, 0, imageInsetsLeft,
                        imageInsetsTop);
            }
            if ((imageInsetsRight > 0) && (imageWidth >= imageInsetsRight)) {

                imageParts[0][2] = input
                        .getSubimage(imageWidth - imageInsetsRight, 0,
                                imageInsetsRight, imageInsetsTop);
            }
        }

        if ((imageInsetsLeft > 0) && (imageInsetsTop >= 0)) {

            imageParts[1][0] = input.getSubimage(0, imageInsetsTop,
                    imageInsetsLeft, imageHeight - imageInsetsTop
                            - imageInsetsBottom);

        }

        if (paintCenter && (imageInsetsLeft >= 0) && (imageInsetsTop >= 0)) {

            imageParts[1][1] = input.getSubimage(imageInsetsLeft,
                    imageInsetsTop, imageWidth - imageInsetsLeft
                            - imageInsetsRight, imageHeight - imageInsetsTop
                            - imageInsetsBottom);
        }

        if ((imageInsetsRight > 0) && (imageWidth >= imageInsetsRight)
                && (imageInsetsTop >= 0)) {

            imageParts[1][2] = input.getSubimage(imageWidth - imageInsetsRight,
                    imageInsetsTop, imageInsetsRight, imageHeight
                            - imageInsetsTop - imageInsetsBottom);
        }
        if ((imageInsetsBottom > 0)) {

            if ((imageInsetsLeft >= 0) && (imageHeight >= imageInsetsBottom)) {

                imageParts[2][1] = input.getSubimage(imageInsetsLeft,
                        imageHeight - imageInsetsBottom, imageWidth
                                - imageInsetsRight - imageInsetsLeft,
                        imageInsetsBottom);
            }

            if ((imageInsetsLeft > 0) && (imageHeight >= imageInsetsBottom)) {

                imageParts[2][0] = input
                        .getSubimage(0, imageHeight - imageInsetsBottom,
                                imageInsetsLeft, imageInsetsBottom);
            }

            if ((imageInsetsRight > 0) && (imageWidth > imageInsetsRight)
                    && (imageHeight > imageInsetsBottom)) {

                imageParts[2][2] = input.getSubimage(imageWidth
                        - imageInsetsRight, imageHeight - imageInsetsBottom,
                        imageInsetsRight, imageInsetsBottom);
            }
        }
    }

    /**
     * Paints the image cropped by cropImage method into array of images
     * according specified parameters, defined in constructor:<br>
     * <br>
     * paintCenter - should center of image painted<br>
     * stretch - either stretch or tile side parts of image<br>
     * destinationInsets - size of side and corner tiles<br>
     * <br>
     * Note: a,b - affineTransform coefficients: x(draw)=a*x(image)
     * y(draw)=b*y(image)
     */
    @SuppressWarnings("unused")
    private void paintImage(SynthContext context, Graphics g, int x, int y,
            int w, int h) {

        // affineTransform coefficients
        double a;
        double b;

        // destinationInsets - size of side and corner tiles. Defined to
        // simplify readability
        int insetsTop = destinationInsets.top;
        int insetsLeft = destinationInsets.left;
        int insetsRight = destinationInsets.right;
        int insetsBottom = destinationInsets.bottom;

        // Define tiling parameters
        int sourceWidth = imageWidth - imageInsetsLeft - imageInsetsRight;
        int destWidth = w - insetsLeft - insetsRight;
        int fullHorisontalPaintsNum = (int) Math.ceil(destWidth / sourceWidth);
        int sourceHeight = imageHeight - imageInsetsBottom - imageInsetsTop;
        int destHeight = h - insetsTop - insetsBottom;
        int fullVerticalPaintsNum = (int) Math.ceil(destHeight / sourceHeight);

        // Define synth parameters
        // JComponent c = context.getComponent();
        // Color color = context.getStyle()
        // .getColor(context, ColorType.BACKGROUND);
        JComponent c = null;
        Color color = Color.RED;

        if ((insetsTop > 0)) {

            if ((insetsLeft > 0) && (imageParts[0][0] != null)) {

                g.drawImage(imageParts[0][0], x, y, insetsLeft, insetsTop,
                        color, c);

            }

            if ((w > insetsLeft - insetsRight) && (imageParts[0][1] != null)
                    && (insetsLeft >= 0)) {

                if (stretch) {
                    g.drawImage(imageParts[0][1], x + insetsLeft, y, w
                            - insetsLeft - insetsRight, insetsTop, c);

                } else {

                    // Stretch the image horizontally
                    b = (double) (insetsTop) / (double) (imageInsetsTop);
                    BufferedImage result = new AffineTransformOp(
                            new AffineTransform(1, 0, 0, b, 0, 0),
                            AffineTransformOp.TYPE_BICUBIC).filter(
                            imageParts[0][1], null);

                    // Draw multiple number of images
                    for (int i = 0; i < fullHorisontalPaintsNum + 1; i++) {
                        g.drawImage(result, x + insetsLeft + i * sourceWidth,
                                y, color, c);
                    }

                }
            }

            if ((insetsRight > 0) && (imageParts[0][2] != null)
                    && (w >= insetsRight)) {

                g.drawImage(imageParts[0][2], x + w - insetsRight, y,
                        insetsRight, insetsTop, color, c);
            }
        }

        if ((insetsLeft > 0) && (imageParts[1][0] != null) && (insetsTop >= 0)
                && (h > insetsTop + insetsBottom)) {

            if (stretch) {

                g.drawImage(imageParts[1][0], x, y + insetsTop, insetsLeft, h
                        - insetsTop - insetsBottom, color, c);
            } else {

                // Stretch the image vertically
                a = (double) (insetsRight) / (double) (imageInsetsRight);
                BufferedImage result = new AffineTransformOp(
                        new AffineTransform(a, 0, 0, 1, 0, 0),
                        AffineTransformOp.TYPE_BICUBIC).filter(
                        imageParts[1][0], null);

                // draw multiple number of pictures
                for (int i = 0; i < fullVerticalPaintsNum + 1; i++) {
                    g.drawImage(result, x, y + i * sourceHeight + insetsTop,
                            color, c);
                }

            }
        }

        if (paintCenter && (imageParts[1][1] != null)
                && (w > insetsLeft + insetsRight)
                && (h > insetsBottom + insetsTop)) {

            g.drawImage(imageParts[1][1], x + insetsLeft, y + insetsTop, w
                    - insetsLeft - insetsRight, h - insetsTop - insetsBottom,
                    color, c);

        }

        if ((insetsRight > 0) && (imageParts[1][2] != null)
                && (h > insetsBottom + insetsTop) && (w >= insetsRight)
                && (insetsTop >= 0)) {

            if (stretch) {

                g.drawImage(imageParts[1][2], x + w - insetsRight, y
                        + insetsTop, insetsRight, h - insetsTop - insetsBottom,
                        color, c);
            } else {

                // Stretch the image vertically
                a = (double) (insetsRight) / (double) (imageInsetsRight);
                BufferedImage result = new AffineTransformOp(
                        new AffineTransform(a, 0, 0, 1, 0, 0),
                        AffineTransformOp.TYPE_BICUBIC).filter(
                        imageParts[1][2], null);

                // draw multiple number of pictures
                for (int i = 0; i < fullVerticalPaintsNum + 1; i++) {
                    g.drawImage(result, x + w - insetsRight, y + i
                            * sourceHeight + insetsTop, color, c);
                }

            }
        }

        if ((insetsBottom > 0)) {

            if ((insetsLeft >= 0) && (h >= insetsBottom)
                    && (w > insetsRight + insetsLeft)
                    && (imageParts[2][0] != null)) {

                g.drawImage(imageParts[2][0], x, y + h - insetsBottom,
                        insetsLeft, insetsBottom, color, c);

            }

            if ((imageParts[2][1] != null) && (imageInsetsBottom > 0)
                    && (w > insetsRight + insetsLeft) && (insetsBottom > 0)
                    && (insetsLeft >= 0) && (h >= insetsBottom)) {

                if (stretch) {

                    g.drawImage(imageParts[2][1], x + insetsLeft, y + h
                            - insetsBottom, w - insetsRight - insetsLeft,
                            insetsBottom, color, c);
                } else {

                    // Stretch the image horizontally
                    b = (double) (insetsBottom) / (double) (imageInsetsBottom);
                    BufferedImage result = new AffineTransformOp(
                            new AffineTransform(1, 0, 0, b, 0, 0),
                            AffineTransformOp.TYPE_BICUBIC).filter(
                            imageParts[2][1], null);

                    // draw multiple number of pictures
                    for (int i = 0; i < fullHorisontalPaintsNum + 1; i++) {
                        g.drawImage(result, x + insetsLeft + i * sourceWidth, y
                                + h - insetsBottom, color, c);
                    }

                }

            }

            if ((insetsRight > 0) && (imageParts[2][2] != null)
                    && (insetsBottom > 0) && (w >= insetsRight)
                    && (h >= insetsBottom) && (imageInsetsBottom > 0)) {

                g.drawImage(imageParts[2][2], x + w - insetsRight, y + h
                        - insetsBottom, insetsRight, insetsBottom, color, c);

            }
        }

    }

    @Override
    public void paintArrowButtonBackground(SynthContext context, Graphics g,
            int x, int y, int w, int h) {
        paintImage(context, g, x, y, w, h);
    }

    @Override
    public void paintArrowButtonBorder(SynthContext context, Graphics g, int x,
            int y, int w, int h) {
        paintImage(context, g, x, y, w, h);
    }

    @Override
    public void paintArrowButtonForeground(SynthContext context, Graphics g,
            int x, int y, int w, int h, int direction) {
        paintImage(context, g, x, y, w, h);
    }

    @Override
    public void paintButtonBackground(SynthContext context, Graphics g, int x,
            int y, int w, int h) {
        paintImage(context, g, x, y, w, h);
    }

    @Override
    public void paintButtonBorder(SynthContext context, Graphics g, int x,
            int y, int w, int h) {
        paintImage(context, g, x, y, w, h);
    }

    @Override
    public void paintCheckBoxBackground(SynthContext context, Graphics g,
            int x, int y, int w, int h) {
        paintImage(context, g, x, y, w, h);
    }

    @Override
    public void paintCheckBoxBorder(SynthContext context, Graphics g, int x,
            int y, int w, int h) {
        paintImage(context, g, x, y, w, h);
    }

    @Override
    public void paintCheckBoxMenuItemBackground(SynthContext context,
            Graphics g, int x, int y, int w, int h) {
        paintImage(context, g, x, y, w, h);
    }

    @Override
    public void paintCheckBoxMenuItemBorder(SynthContext context, Graphics g,
            int x, int y, int w, int h) {
        paintImage(context, g, x, y, w, h);
    }

    @Override
    public void paintColorChooserBackground(SynthContext context, Graphics g,
            int x, int y, int w, int h) {
        paintImage(context, g, x, y, w, h);
    }

    @Override
    public void paintColorChooserBorder(SynthContext context, Graphics g,
            int x, int y, int w, int h) {
        paintImage(context, g, x, y, w, h);
    }

    @Override
    public void paintComboBoxBackground(SynthContext context, Graphics g,
            int x, int y, int w, int h) {
        paintImage(context, g, x, y, w, h);
    }

    @Override
    public void paintComboBoxBorder(SynthContext context, Graphics g, int x,
            int y, int w, int h) {
        paintImage(context, g, x, y, w, h);
    }

    @Override
    public void paintDesktopIconBackground(SynthContext context, Graphics g,
            int x, int y, int w, int h) {
        paintImage(context, g, x, y, w, h);
    }

    @Override
    public void paintDesktopIconBorder(SynthContext context, Graphics g, int x,
            int y, int w, int h) {
        paintImage(context, g, x, y, w, h);
    }

    @Override
    public void paintDesktopPaneBackground(SynthContext context, Graphics g,
            int x, int y, int w, int h) {
        paintImage(context, g, x, y, w, h);
    }

    @Override
    public void paintDesktopPaneBorder(SynthContext context, Graphics g, int x,
            int y, int w, int h) {
        paintImage(context, g, x, y, w, h);
    }

    @Override
    public void paintEditorPaneBackground(SynthContext context, Graphics g,
            int x, int y, int w, int h) {
        paintImage(context, g, x, y, w, h);
    }

    @Override
    public void paintEditorPaneBorder(SynthContext context, Graphics g, int x,
            int y, int w, int h) {
        paintImage(context, g, x, y, w, h);
    }

    @Override
    public void paintFileChooserBackground(SynthContext context, Graphics g,
            int x, int y, int w, int h) {
        paintImage(context, g, x, y, w, h);
    }

    @Override
    public void paintFileChooserBorder(SynthContext context, Graphics g, int x,
            int y, int w, int h) {
        paintImage(context, g, x, y, w, h);
    }

    @Override
    public void paintFormattedTextFieldBackground(SynthContext context,
            Graphics g, int x, int y, int w, int h) {
        paintImage(context, g, x, y, w, h);
    }

    @Override
    public void paintFormattedTextFieldBorder(SynthContext context, Graphics g,
            int x, int y, int w, int h) {
        paintImage(context, g, x, y, w, h);
    }

    @Override
    public void paintInternalFrameBackground(SynthContext context, Graphics g,
            int x, int y, int w, int h) {
        paintImage(context, g, x, y, w, h);
    }

    @Override
    public void paintInternalFrameBorder(SynthContext context, Graphics g,
            int x, int y, int w, int h) {
        paintImage(context, g, x, y, w, h);
    }

    @Override
    public void paintInternalFrameTitlePaneBackground(SynthContext context,
            Graphics g, int x, int y, int w, int h) {
        paintImage(context, g, x, y, w, h);
    }

    @Override
    public void paintInternalFrameTitlePaneBorder(SynthContext context,
            Graphics g, int x, int y, int w, int h) {
        paintImage(context, g, x, y, w, h);
    }

    @Override
    public void paintLabelBackground(SynthContext context, Graphics g, int x,
            int y, int w, int h) {
        g.setColor(Color.RED);
        g.fillRect(x, y, x + w, y + h);
    }

    @Override
    public void paintLabelBorder(SynthContext context, Graphics g, int x,
            int y, int w, int h) {
        paintImage(context, g, x, y, w, h);
    }

    @Override
    public void paintListBackground(SynthContext context, Graphics g, int x,
            int y, int w, int h) {
        paintImage(context, g, x, y, w, h);
    }

    @Override
    public void paintListBorder(SynthContext context, Graphics g, int x, int y,
            int w, int h) {
        paintImage(context, g, x, y, w, h);
    }

    @Override
    public void paintMenuBackground(SynthContext context, Graphics g, int x,
            int y, int w, int h) {
        paintImage(context, g, x, y, w, h);
    }

    @Override
    public void paintMenuBarBackground(SynthContext context, Graphics g, int x,
            int y, int w, int h) {
        paintImage(context, g, x, y, w, h);
    }

    @Override
    public void paintMenuBarBorder(SynthContext context, Graphics g, int x,
            int y, int w, int h) {
        paintImage(context, g, x, y, w, h);
    }

    @Override
    public void paintMenuBorder(SynthContext context, Graphics g, int x, int y,
            int w, int h) {
        paintImage(context, g, x, y, w, h);
    }

    @Override
    public void paintMenuItemBackground(SynthContext context, Graphics g,
            int x, int y, int w, int h) {
        paintImage(context, g, x, y, w, h);
    }

    @Override
    public void paintMenuItemBorder(SynthContext context, Graphics g, int x,
            int y, int w, int h) {
        paintImage(context, g, x, y, w, h);
    }

    @Override
    public void paintOptionPaneBackground(SynthContext context, Graphics g,
            int x, int y, int w, int h) {
        paintImage(context, g, x, y, w, h);
    }

    @Override
    public void paintOptionPaneBorder(SynthContext context, Graphics g, int x,
            int y, int w, int h) {
        paintImage(context, g, x, y, w, h);
    }

    @Override
    public void paintPanelBackground(SynthContext context, Graphics g, int x,
            int y, int w, int h) {
        paintImage(context, g, x, y, w, h);
    }

    @Override
    public void paintPanelBorder(SynthContext context, Graphics g, int x,
            int y, int w, int h) {
        paintImage(context, g, x, y, w, h);
    }

    @Override
    public void paintPasswordFieldBackground(SynthContext context, Graphics g,
            int x, int y, int w, int h) {
        paintImage(context, g, x, y, w, h);
    }

    @Override
    public void paintPasswordFieldBorder(SynthContext context, Graphics g,
            int x, int y, int w, int h) {
        paintImage(context, g, x, y, w, h);
    }

    @Override
    public void paintPopupMenuBackground(SynthContext context, Graphics g,
            int x, int y, int w, int h) {
        paintImage(context, g, x, y, w, h);
    }

    @Override
    public void paintPopupMenuBorder(SynthContext context, Graphics g, int x,
            int y, int w, int h) {
        paintImage(context, g, x, y, w, h);
    }

    @Override
    public void paintProgressBarBackground(SynthContext context, Graphics g,
            int x, int y, int w, int h) {
        paintImage(context, g, x, y, w, h);
    }

    @Override
    public void paintProgressBarBorder(SynthContext context, Graphics g, int x,
            int y, int w, int h) {
        paintImage(context, g, x, y, w, h);
    }

    @Override
    public void paintProgressBarForeground(SynthContext context, Graphics g,
            int x, int y, int w, int h, int orientation) {
        paintImage(context, g, x, y, w, h);
    }

    @Override
    public void paintRadioButtonBackground(SynthContext context, Graphics g,
            int x, int y, int w, int h) {
        paintImage(context, g, x, y, w, h);
    }

    @Override
    public void paintRadioButtonBorder(SynthContext context, Graphics g, int x,
            int y, int w, int h) {
        paintImage(context, g, x, y, w, h);
    }

    @Override
    public void paintRadioButtonMenuItemBackground(SynthContext context,
            Graphics g, int x, int y, int w, int h) {
        paintImage(context, g, x, y, w, h);
    }

    @Override
    public void paintRadioButtonMenuItemBorder(SynthContext context,
            Graphics g, int x, int y, int w, int h) {
        paintImage(context, g, x, y, w, h);
    }

    @Override
    public void paintRootPaneBackground(SynthContext context, Graphics g,
            int x, int y, int w, int h) {
        paintImage(context, g, x, y, w, h);
    }

    @Override
    public void paintRootPaneBorder(SynthContext context, Graphics g, int x,
            int y, int w, int h) {
        paintImage(context, g, x, y, w, h);
    }

    @Override
    public void paintScrollBarBackground(SynthContext context, Graphics g,
            int x, int y, int w, int h) {
        paintImage(context, g, x, y, w, h);
    }

    @Override
    public void paintScrollBarBorder(SynthContext context, Graphics g, int x,
            int y, int w, int h) {
        paintImage(context, g, x, y, w, h);
    }

    @Override
    public void paintScrollBarThumbBackground(SynthContext context, Graphics g,
            int x, int y, int w, int h, int orientation) {
        paintImage(context, g, x, y, w, h);
    }

    @Override
    public void paintScrollBarThumbBorder(SynthContext context, Graphics g,
            int x, int y, int w, int h, int orientation) {
        paintImage(context, g, x, y, w, h);
    }

    @Override
    public void paintScrollBarTrackBackground(SynthContext context, Graphics g,
            int x, int y, int w, int h) {
        paintImage(context, g, x, y, w, h);
    }

    @Override
    public void paintScrollBarTrackBorder(SynthContext context, Graphics g,
            int x, int y, int w, int h) {
        paintImage(context, g, x, y, w, h);
    }

    @Override
    public void paintScrollPaneBackground(SynthContext context, Graphics g,
            int x, int y, int w, int h) {
        paintImage(context, g, x, y, w, h);
    }

    @Override
    public void paintScrollPaneBorder(SynthContext context, Graphics g, int x,
            int y, int w, int h) {
        paintImage(context, g, x, y, w, h);
    }

    @Override
    public void paintSeparatorBackground(SynthContext context, Graphics g,
            int x, int y, int w, int h) {
        paintImage(context, g, x, y, w, h);
    }

    @Override
    public void paintSeparatorBorder(SynthContext context, Graphics g, int x,
            int y, int w, int h) {
        paintImage(context, g, x, y, w, h);
    }

    @Override
    public void paintSeparatorForeground(SynthContext context, Graphics g,
            int x, int y, int w, int h, int orientation) {
        paintImage(context, g, x, y, w, h);
    }

    @Override
    public void paintSliderBackground(SynthContext context, Graphics g, int x,
            int y, int w, int h) {
        paintImage(context, g, x, y, w, h);
    }

    @Override
    public void paintSliderBorder(SynthContext context, Graphics g, int x,
            int y, int w, int h) {
        paintImage(context, g, x, y, w, h);
    }

    @Override
    public void paintSliderThumbBackground(SynthContext context, Graphics g,
            int x, int y, int w, int h, int orientation) {
        paintImage(context, g, x, y, w, h);
    }

    @Override
    public void paintSliderThumbBorder(SynthContext context, Graphics g, int x,
            int y, int w, int h, int orientation) {
        paintImage(context, g, x, y, w, h);
    }

    @Override
    public void paintSliderTrackBackground(SynthContext context, Graphics g,
            int x, int y, int w, int h) {
        paintImage(context, g, x, y, w, h);
    }

    @Override
    public void paintSliderTrackBorder(SynthContext context, Graphics g, int x,
            int y, int w, int h) {
        paintImage(context, g, x, y, w, h);
    }

    @Override
    public void paintSpinnerBackground(SynthContext context, Graphics g, int x,
            int y, int w, int h) {
        paintImage(context, g, x, y, w, h);
    }

    @Override
    public void paintSpinnerBorder(SynthContext context, Graphics g, int x,
            int y, int w, int h) {
        paintImage(context, g, x, y, w, h);
    }

    @Override
    public void paintSplitPaneBackground(SynthContext context, Graphics g,
            int x, int y, int w, int h) {
        paintImage(context, g, x, y, w, h);
    }

    @Override
    public void paintSplitPaneBorder(SynthContext context, Graphics g, int x,
            int y, int w, int h) {
        paintImage(context, g, x, y, w, h);
    }

    @Override
    public void paintSplitPaneDividerBackground(SynthContext context,
            Graphics g, int x, int y, int w, int h) {
        paintImage(context, g, x, y, w, h);
    }

    @Override
    public void paintSplitPaneDividerForeground(SynthContext context,
            Graphics g, int x, int y, int w, int h, int orientation) {
        paintImage(context, g, x, y, w, h);
    }

    @Override
    public void paintSplitPaneDragDivider(SynthContext context, Graphics g,
            int x, int y, int w, int h, int orientation) {
        paintImage(context, g, x, y, w, h);
    }

    @Override
    public void paintTabbedPaneBackground(SynthContext context, Graphics g,
            int x, int y, int w, int h) {
        paintImage(context, g, x, y, w, h);
    }

    @Override
    public void paintTabbedPaneBorder(SynthContext context, Graphics g, int x,
            int y, int w, int h) {
        paintImage(context, g, x, y, w, h);
    }

    @Override
    public void paintTabbedPaneContentBackground(SynthContext context,
            Graphics g, int x, int y, int w, int h) {
        paintImage(context, g, x, y, w, h);
    }

    @Override
    public void paintTabbedPaneContentBorder(SynthContext context, Graphics g,
            int x, int y, int w, int h) {
        paintImage(context, g, x, y, w, h);
    }

    @Override
    public void paintTabbedPaneTabAreaBackground(SynthContext context,
            Graphics g, int x, int y, int w, int h) {
        paintImage(context, g, x, y, w, h);
    }

    @Override
    public void paintTabbedPaneTabAreaBorder(SynthContext context, Graphics g,
            int x, int y, int w, int h) {
        paintImage(context, g, x, y, w, h);
    }

    @Override
    public void paintTabbedPaneTabBackground(SynthContext context, Graphics g,
            int x, int y, int w, int h, int tabIndex) {
        paintImage(context, g, x, y, w, h);
    }

    @Override
    public void paintTabbedPaneTabBorder(SynthContext context, Graphics g,
            int x, int y, int w, int h, int tabIndex) {
        paintImage(context, g, x, y, w, h);
    }

    @Override
    public void paintTableBackground(SynthContext context, Graphics g, int x,
            int y, int w, int h) {
        paintImage(context, g, x, y, w, h);
    }

    @Override
    public void paintTableBorder(SynthContext context, Graphics g, int x,
            int y, int w, int h) {
        paintImage(context, g, x, y, w, h);
    }

    @Override
    public void paintTableHeaderBackground(SynthContext context, Graphics g,
            int x, int y, int w, int h) {
        paintImage(context, g, x, y, w, h);
    }

    @Override
    public void paintTableHeaderBorder(SynthContext context, Graphics g, int x,
            int y, int w, int h) {
        paintImage(context, g, x, y, w, h);
    }

    @Override
    public void paintTextAreaBackground(SynthContext context, Graphics g,
            int x, int y, int w, int h) {
        paintImage(context, g, x, y, w, h);
    }

    @Override
    public void paintTextAreaBorder(SynthContext context, Graphics g, int x,
            int y, int w, int h) {
        paintImage(context, g, x, y, w, h);
    }

    @Override
    public void paintTextFieldBackground(SynthContext context, Graphics g,
            int x, int y, int w, int h) {
        paintImage(context, g, x, y, w, h);
    }

    @Override
    public void paintTextFieldBorder(SynthContext context, Graphics g, int x,
            int y, int w, int h) {
        paintImage(context, g, x, y, w, h);
    }

    @Override
    public void paintTextPaneBackground(SynthContext context, Graphics g,
            int x, int y, int w, int h) {
        paintImage(context, g, x, y, w, h);
    }

    @Override
    public void paintTextPaneBorder(SynthContext context, Graphics g, int x,
            int y, int w, int h) {
        paintImage(context, g, x, y, w, h);
    }

    @Override
    public void paintToggleButtonBackground(SynthContext context, Graphics g,
            int x, int y, int w, int h) {
        paintImage(context, g, x, y, w, h);
    }

    @Override
    public void paintToggleButtonBorder(SynthContext context, Graphics g,
            int x, int y, int w, int h) {
        paintImage(context, g, x, y, w, h);
    }

    @Override
    public void paintToolBarBackground(SynthContext context, Graphics g, int x,
            int y, int w, int h) {
        paintImage(context, g, x, y, w, h);
    }

    @Override
    public void paintToolBarBorder(SynthContext context, Graphics g, int x,
            int y, int w, int h) {
        paintImage(context, g, x, y, w, h);
    }

    @Override
    public void paintToolBarContentBackground(SynthContext context, Graphics g,
            int x, int y, int w, int h) {
        paintImage(context, g, x, y, w, h);
    }

    @Override
    public void paintToolBarContentBorder(SynthContext context, Graphics g,
            int x, int y, int w, int h) {
        paintImage(context, g, x, y, w, h);
    }

    @Override
    public void paintToolBarDragWindowBackground(SynthContext context,
            Graphics g, int x, int y, int w, int h) {
        paintImage(context, g, x, y, w, h);
    }

    @Override
    public void paintToolBarDragWindowBorder(SynthContext context, Graphics g,
            int x, int y, int w, int h) {
        paintImage(context, g, x, y, w, h);
    }

    @Override
    public void paintToolTipBackground(SynthContext context, Graphics g, int x,
            int y, int w, int h) {
        paintImage(context, g, x, y, w, h);
    }

    @Override
    public void paintToolTipBorder(SynthContext context, Graphics g, int x,
            int y, int w, int h) {
        paintImage(context, g, x, y, w, h);
    }

    @Override
    public void paintTreeBackground(SynthContext context, Graphics g, int x,
            int y, int w, int h) {
        paintImage(context, g, x, y, w, h);
    }

    @Override
    public void paintTreeBorder(SynthContext context, Graphics g, int x, int y,
            int w, int h) {
        paintImage(context, g, x, y, w, h);
    }

    @Override
    public void paintTreeCellBackground(SynthContext context, Graphics g,
            int x, int y, int w, int h) {
        paintImage(context, g, x, y, w, h);
    }

    @Override
    public void paintTreeCellBorder(SynthContext context, Graphics g, int x,
            int y, int w, int h) {
        paintImage(context, g, x, y, w, h);
    }

    @Override
    public void paintTreeCellFocus(SynthContext context, Graphics g, int x,
            int y, int w, int h) {
        paintImage(context, g, x, y, w, h);
    }

    @Override
    public void paintViewportBackground(SynthContext context, Graphics g,
            int x, int y, int w, int h) {
        paintImage(context, g, x, y, w, h);
    }

    @Override
    public void paintViewportBorder(SynthContext context, Graphics g, int x,
            int y, int w, int h) {
        paintImage(context, g, x, y, w, h);
    }
}
