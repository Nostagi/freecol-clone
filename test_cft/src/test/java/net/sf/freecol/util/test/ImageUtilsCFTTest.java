
package net.sf.freecol.util.test;

import static org.junit.Assert.*;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.Paint;
import java.awt.image.BufferedImage;

import javax.swing.JPanel;

import org.junit.Test;

import net.sf.freecol.common.util.ImageUtils;

public class ImageUtilsCFTTest {

    private BufferedImage createImage(int width, int height, Color color) {
        BufferedImage image = new BufferedImage(
                width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        g.setColor(color);
        g.fillRect(0, 0, width, height);
        g.dispose();
        return image;
    }

    @Test
    public void IU01_createBufferedImageNull() {
        assertNull(ImageUtils.createBufferedImage(null));
    }

    @Test
    public void IU02_createBufferedImageFromImage() {
        BufferedImage source = createImage(4, 3, Color.RED);
        BufferedImage result = ImageUtils.createBufferedImage(source);

        assertNotNull(result);
        assertEquals(4, result.getWidth());
        assertEquals(3, result.getHeight());
    }

    @Test
    public void IU03_createBufferedImageWithSize() {
        BufferedImage result = ImageUtils.createBufferedImage(8, 6);

        assertNotNull(result);
        assertEquals(8, result.getWidth());
        assertEquals(6, result.getHeight());
    }

    @Test
    public void IU04_createGrayscaleImageNull() {
        assertNull(ImageUtils.createGrayscaleImage(null));
    }

    @Test
    public void IU05_createGrayscaleImage() {
        BufferedImage source = createImage(4, 4, Color.RED);
        BufferedImage result = ImageUtils.createGrayscaleImage(source);

        assertNotNull(result);
        assertEquals(4, result.getWidth());
        assertEquals(4, result.getHeight());

        Color pixel = new Color(result.getRGB(1, 1), true);
        assertEquals(pixel.getRed(), pixel.getGreen());
        assertEquals(pixel.getGreen(), pixel.getBlue());
    }

    @Test
    public void IU06_createHalvedImageNull() {
        assertNull(ImageUtils.createHalvedImage(null));
    }

    @Test
    public void IU07_createHalvedImageEvenSize() {
        BufferedImage source = createImage(8, 6, Color.RED);
        BufferedImage result = ImageUtils.createHalvedImage(source);

        assertEquals(4, result.getWidth());
        assertEquals(3, result.getHeight());
    }

    @Test
    public void IU08_createHalvedImageOddSize() {
        BufferedImage source = createImage(5, 7, Color.BLUE);
        BufferedImage result = ImageUtils.createHalvedImage(source);

        assertEquals(3, result.getWidth());
        assertEquals(4, result.getHeight());
    }

    @Test
    public void IU09_createMirroredImageNull() {
        assertNull(ImageUtils.createMirroredImage(null));
    }

    @Test
    public void IU10_createMirroredImage() {
        BufferedImage source = new BufferedImage(
                2, 1, BufferedImage.TYPE_INT_ARGB);
        source.setRGB(0, 0, Color.RED.getRGB());
        source.setRGB(1, 0, Color.BLUE.getRGB());

        BufferedImage result = ImageUtils.createMirroredImage(source);

        assertEquals(2, result.getWidth());
        assertEquals(1, result.getHeight());
        assertEquals(Color.BLUE.getRGB(), result.getRGB(0, 0));
        assertEquals(Color.RED.getRGB(), result.getRGB(1, 0));
    }

    @Test
    public void IU11_createResizedImageDefault() {
        BufferedImage source = createImage(4, 4, Color.RED);
        BufferedImage result = ImageUtils.createResizedImage(source, 8, 6);

        assertEquals(8, result.getWidth());
        assertEquals(6, result.getHeight());
    }

    @Test
    public void IU12_createResizedImageInterpolationTrue() {
        BufferedImage source = createImage(4, 4, Color.GREEN);
        BufferedImage result =
                ImageUtils.createResizedImage(source, 10, 8, true);

        assertEquals(10, result.getWidth());
        assertEquals(8, result.getHeight());
    }

    @Test
    public void IU13_createResizedImageInterpolationFalse() {
        BufferedImage source = createImage(4, 4, Color.BLUE);
        BufferedImage result =
                ImageUtils.createResizedImage(source, 6, 5, false);

        assertEquals(6, result.getWidth());
        assertEquals(5, result.getHeight());
    }

    @Test
    public void IU14_drawTiledImageNullWithoutInsets() {
        BufferedImage canvas = new BufferedImage(
                10, 10, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = canvas.createGraphics();

        JPanel panel = new JPanel();
        panel.setSize(10, 10);
        panel.setBackground(Color.GREEN);

        ImageUtils.drawTiledImage(null, g, panel, null);

        assertEquals(Color.GREEN.getRGB(), canvas.getRGB(5, 5));
        g.dispose();
    }

    @Test
    public void IU15_drawTiledImageNullWithInsets() {
        BufferedImage canvas = new BufferedImage(
                10, 10, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = canvas.createGraphics();

        JPanel panel = new JPanel();
        panel.setSize(10, 10);
        panel.setBackground(Color.GREEN);

        ImageUtils.drawTiledImage(
                null, g, panel, new Insets(2, 3, 2, 3));

        assertEquals(Color.GREEN.getRGB(), canvas.getRGB(3, 2));
        assertEquals(0, canvas.getRGB(0, 0));
        g.dispose();
    }

    @Test
    public void IU16_drawTiledImageWithImage() {
        BufferedImage canvas = new BufferedImage(
                6, 6, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = canvas.createGraphics();

        JPanel panel = new JPanel();
        panel.setSize(6, 6);

        BufferedImage tile = createImage(2, 2, Color.RED);
        ImageUtils.drawTiledImage(tile, g, panel, null);

        assertEquals(Color.RED.getRGB(), canvas.getRGB(0, 0));
        assertEquals(Color.RED.getRGB(), canvas.getRGB(5, 5));
        g.dispose();
    }

    @Test
    public void IU17_drawTiledImageWithImageAndInsets() {
        BufferedImage canvas = new BufferedImage(
                8, 8, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = canvas.createGraphics();

        JPanel panel = new JPanel();
        panel.setSize(8, 8);

        BufferedImage tile = createImage(2, 2, Color.BLUE);
        ImageUtils.drawTiledImage(
                tile, g, panel, new Insets(1, 1, 1, 1));

        assertEquals(Color.BLUE.getRGB(), canvas.getRGB(1, 1));
        assertEquals(0, canvas.getRGB(0, 0));
        g.dispose();
    }

    @Test
    public void IU18_fillTextureRestoresPaint() {
        BufferedImage canvas = new BufferedImage(
                8, 8, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = canvas.createGraphics();

        BufferedImage texture = createImage(2, 2, Color.RED);
        Paint oldPaint = Color.BLUE;
        g.setPaint(oldPaint);

        ImageUtils.fillTexture(g, texture, 1, 1, 5, 5);

        assertSame(oldPaint, g.getPaint());
        assertEquals(Color.RED.getRGB(), canvas.getRGB(2, 2));
        g.dispose();
    }

    @Test
    public void IU19_createCenteredImageWithDimension() {
        BufferedImage source = createImage(2, 2, Color.RED);
        BufferedImage result = ImageUtils.createCenteredImage(
                source, new Dimension(6, 6));

        assertEquals(6, result.getWidth());
        assertEquals(6, result.getHeight());
        assertEquals(Color.RED.getRGB(), result.getRGB(2, 2));
    }

    @Test
    public void IU20_createCenteredImageWithWidthHeight() {
        BufferedImage source = createImage(2, 2, Color.BLUE);
        BufferedImage result =
                ImageUtils.createCenteredImage(source, 8, 6);

        assertEquals(8, result.getWidth());
        assertEquals(6, result.getHeight());
        assertEquals(Color.BLUE.getRGB(), result.getRGB(3, 2));
    }

    @Test
    public void IU21_wildcardDimensionBothWildcards() {
        Dimension nominal = new Dimension(100, 50);
        Dimension result = ImageUtils.wildcardDimension(
                new Dimension(0, -1), nominal);

        assertSame(nominal, result);
    }

    @Test
    public void IU22_wildcardDimensionWidthWildcard() {
        Dimension result = ImageUtils.wildcardDimension(
                new Dimension(0, 25), new Dimension(100, 50));

        assertEquals(50, result.width);
        assertEquals(25, result.height);
    }

    @Test
    public void IU23_wildcardDimensionHeightWildcard() {
        Dimension result = ImageUtils.wildcardDimension(
                new Dimension(50, 0), new Dimension(100, 50));

        assertEquals(50, result.width);
        assertEquals(25, result.height);
    }

    @Test
    public void IU24_wildcardDimensionWidthTooLarge() {
        Dimension result = ImageUtils.wildcardDimension(
                new Dimension(100, 25), new Dimension(100, 50));

        assertEquals(50, result.width);
        assertEquals(25, result.height);
    }

    @Test
    public void IU25_wildcardDimensionHeightTooLarge() {
        Dimension result = ImageUtils.wildcardDimension(
                new Dimension(25, 50), new Dimension(100, 50));

        assertEquals(25, result.width);
        assertEquals(13, result.height);
    }

    @Test
    public void IU26_wildcardDimensionSameAspectRatio() {
        Dimension result = ImageUtils.wildcardDimension(
                new Dimension(100, 50), new Dimension(100, 50));

        assertEquals(100, result.width);
        assertEquals(50, result.height);
    }

    @Test
    public void IU27_fadeImage() {
        BufferedImage source = createImage(4, 4, Color.RED);
        BufferedImage result = ImageUtils.fadeImage(
                source, 0.5f, 0.5f);

        assertNotNull(result);
        assertEquals(4, result.getWidth());
        assertEquals(4, result.getHeight());
    }

    @Test
    public void IU28_imageWithAlphaFromMask() {
        BufferedImage source = createImage(2, 1, Color.RED);
        BufferedImage mask = new BufferedImage(
                2, 1, BufferedImage.TYPE_INT_ARGB);

        mask.setRGB(0, 0, 0x00000000);
        mask.setRGB(1, 0, 0xFFFFFFFF);

        BufferedImage result =
                ImageUtils.imageWithAlphaFromMask(source, mask);

        assertNotNull(result);
        assertEquals(0, (result.getRGB(0, 0) >>> 24) & 0xFF);
        assertEquals(255, (result.getRGB(1, 0) >>> 24) & 0xFF);
    }
}
