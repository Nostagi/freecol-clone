
package net.sf.freecol.util.test;

import static org.junit.Assert.*;

import java.awt.DisplayMode;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Toolkit;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import javax.xml.transform.Transformer;

import org.junit.Assume;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import net.sf.freecol.common.util.Utils;

public class UtilsCFTTest {

    @Rule
    public TemporaryFolder temp = new TemporaryFolder();

    // equals(Object, Object)

    @Test
    public void UT01_equalsBothNull() {
        assertTrue(Utils.equals(null, null));
    }

    @Test
    public void UT02_equalsFirstNull() {
        assertFalse(Utils.equals(null, "Java"));
    }

    @Test
    public void UT03_equalsEqualObjects() {
        assertTrue(Utils.equals("Java", "Java"));
    }

    @Test
    public void UT04_equalsDifferentObjects() {
        assertFalse(Utils.equals("Java", "FreeCol"));
    }

    // hashCode(Object)

    @Test
    public void UT05_hashCodeNull() {
        assertEquals(31, Utils.hashCode(null));
    }

    @Test
    public void UT06_hashCodeObject() {
        String value = "FreeCol";
        assertEquals(value.hashCode(), Utils.hashCode(value));
    }

    // getRandomState(Random), restoreRandomState(String)

    @Test
    public void UT07_getRandomStateAndRestore() throws Exception {
        Random original = new Random(12345L);
        String state = Utils.getRandomState(original);

        assertNotNull(state);
        assertFalse(state.isEmpty());

        Random restored = Utils.restoreRandomState(state);

        assertNotNull(restored);
        assertEquals(original.nextInt(), restored.nextInt());
    }

    @Test
    public void UT08_restoreRandomStateNull() {
        assertNull(Utils.restoreRandomState(null));
    }

    @Test
    public void UT09_restoreRandomStateEmpty() {
        assertNull(Utils.restoreRandomState(""));
    }

    @Test
    public void UT10_restoreRandomStateInvalid() {
        assertNull(Utils.restoreRandomState("ZZ"));
    }

    // getFileUTF8Reader(File), getUTF8Contents(File)

    @Test
    public void UT11_getFileUTF8Reader() throws Exception {
        File file = temp.newFile("reader.txt");
        Files.write(file.toPath(), "FreeCol".getBytes(StandardCharsets.UTF_8));

        Reader reader = Utils.getFileUTF8Reader(file);

        assertNotNull(reader);
        char[] buffer = new char[20];
        int length = reader.read(buffer);
        reader.close();

        assertEquals("FreeCol", new String(buffer, 0, length));
    }

    @Test
    public void UT12_getFileUTF8ReaderMissingFile() {
        File file = new File(temp.getRoot(), "missing.txt");

        assertNull(Utils.getFileUTF8Reader(file));
    }

    @Test
    public void UT13_getUTF8Contents() throws Exception {
        File file = temp.newFile("content.txt");
        String expected = "FreeCol - Kiểm thử phần mềm";

        Files.write(file.toPath(),
            expected.getBytes(StandardCharsets.UTF_8));

        assertEquals(expected, Utils.getUTF8Contents(file));
    }

    @Test
    public void UT14_getUTF8ContentsMissingFile() {
        File file = new File(temp.getRoot(), "missing.txt");

        assertNull(Utils.getUTF8Contents(file));
    }

    @Test
    public void UT15_getUTF8ContentsEmptyFile() throws Exception {
        File file = temp.newFile("empty.txt");

        assertEquals("", Utils.getUTF8Contents(file));
    }

    // getUTF8Writer(OutputStream)

    @Test
    public void UT16_getUTF8Writer() throws Exception {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        Writer writer = Utils.getUTF8Writer(output);

        writer.write("FreeCol");
        writer.close();

        assertEquals("FreeCol",
            new String(output.toByteArray(), StandardCharsets.UTF_8));
    }

    // getFileUTF8Writer(File), getFileUTF8AppendWriter(File)

    @Test
    public void UT17_getFileUTF8Writer() throws Exception {
        File file = temp.newFile("write.txt");

        Writer writer = Utils.getFileUTF8Writer(file);
        assertNotNull(writer);

        writer.write("FreeCol");
        writer.close();

        assertEquals("FreeCol", Utils.getUTF8Contents(file));
    }

    @Test
    public void UT18_getFileUTF8AppendWriter() throws Exception {
        File file = temp.newFile("append.txt");
        Files.write(file.toPath(), "Free".getBytes(StandardCharsets.UTF_8));

        Writer writer = Utils.getFileUTF8AppendWriter(file);
        assertNotNull(writer);

        writer.write("Col");
        writer.close();

        assertEquals("FreeCol", Utils.getUTF8Contents(file));
    }

    // makeTransformer(boolean, boolean)

    @Test
    public void UT19_makeTransformerWithDeclarationAndIndent() {
        Transformer transformer = Utils.makeTransformer(true, true);

        assertNotNull(transformer);
        assertEquals("UTF-8",
            transformer.getOutputProperty("encoding"));
        assertEquals("xml",
            transformer.getOutputProperty("method"));
        assertEquals("yes",
            transformer.getOutputProperty("indent"));
    }

    @Test
    public void UT20_makeTransformerWithoutDeclaration() {
        Transformer transformer = Utils.makeTransformer(false, false);

        assertNotNull(transformer);
        assertEquals("yes",
            transformer.getOutputProperty("omit-xml-declaration"));
        assertEquals("no",
            transformer.getOutputProperty("indent"));
    }

    // deleteFile(File), deleteFiles(List<File>)

    @Test
    public void UT21_deleteFile() throws Exception {
        File file = temp.newFile("delete.txt");

        Utils.deleteFile(file);

        assertFalse(file.exists());
    }

    @Test
    public void UT22_deleteFiles() throws Exception {
        File first = temp.newFile("first.txt");
        File second = temp.newFile("second.txt");

        Utils.deleteFiles(Arrays.asList(first, second));

        assertFalse(first.exists());
        assertFalse(second.exists());
    }

    // fileAnySuffix(File, String...)

    @Test
    public void UT23_fileAnySuffixMatched() throws Exception {
        File file = temp.newFile("data.txt");

        assertTrue(Utils.fileAnySuffix(file, ".txt", ".xml"));
    }

    @Test
    public void UT24_fileAnySuffixNotMatched() throws Exception {
        File file = temp.newFile("data.txt");

        assertFalse(Utils.fileAnySuffix(file, ".xml", ".json"));
    }

    @Test
    public void UT25_fileAnySuffixNullFile() {
        assertFalse(Utils.fileAnySuffix(null, ".txt"));
    }

    @Test
    public void UT26_fileAnySuffixDirectory() {
        assertFalse(Utils.fileAnySuffix(temp.getRoot(), ".txt"));
    }

    // directoryAllPresent(File, String...)

    @Test
    public void UT27_directoryAllPresent() throws Exception {
        File directory = temp.newFolder("present");
        Files.createFile(new File(directory, "one.txt").toPath());
        Files.createFile(new File(directory, "two.txt").toPath());

        assertTrue(Utils.directoryAllPresent(
            directory, "one.txt", "two.txt"));
    }

    @Test
    public void UT28_directoryAllPresentMissingFile() throws Exception {
        File directory = temp.newFolder("missing");
        Files.createFile(new File(directory, "one.txt").toPath());

        assertFalse(Utils.directoryAllPresent(
            directory, "one.txt", "two.txt"));
    }

    @Test
    public void UT29_directoryAllPresentNullDirectory() {
        assertFalse(Utils.directoryAllPresent(null, "one.txt"));
    }

    @Test
    public void UT30_directoryAllPresentNotDirectory() throws Exception {
        File file = temp.newFile("not-directory.txt");

        assertFalse(Utils.directoryAllPresent(file, "one.txt"));
    }

    // delay(long, String)

    @Test
    public void UT31_delayNormal() {
        long start = System.nanoTime();

        Utils.delay(10, null);

        long elapsed = TimeUnit.NANOSECONDS.toMillis(
            System.nanoTime() - start);

        assertTrue(elapsed >= 0);
    }

    @Test
    public void UT32_delayInterruptedWithoutWarning() {
        try {
            Thread.currentThread().interrupt();

            Utils.delay(10, null);

            assertTrue(Thread.currentThread().isInterrupted());
        } finally {
            Thread.interrupted();
        }
    }

    @Test
    public void UT33_delayInterruptedWithWarning() {
        try {
            Thread.currentThread().interrupt();

            Utils.delay(10, "Test interrupted");

            assertFalse(Thread.currentThread().isInterrupted());
        } finally {
            Thread.interrupted();
        }
    }

    // now()

    @Test
    public void UT34_now() {
        long before = System.currentTimeMillis();
        long result = Utils.now();
        long after = System.currentTimeMillis();

        assertTrue(result >= before);
        assertTrue(result <= after);
    }

    // garbageCollect()

    @Test
    public void UT35_garbageCollect() {
        Utils.garbageCollect();
    }

    // isHeadless()

    @Test
    public void UT36_isHeadless() {
        String previous = System.getProperty("java.awt.headless");

        try {
            System.setProperty("java.awt.headless", "true");
            assertTrue(Utils.isHeadless());

            System.setProperty("java.awt.headless", "false");
            assertFalse(Utils.isHeadless());
        } finally {
            if (previous == null) {
                System.clearProperty("java.awt.headless");
            } else {
                System.setProperty("java.awt.headless", previous);
            }
        }
    }

    // getGoodGraphicsDevice()

    @Test
    public void UT37_getGoodGraphicsDevice() {
        GraphicsDevice device = Utils.getGoodGraphicsDevice();

        if (GraphicsEnvironment.isHeadless()) {
            assertNull(device);
        } else {
            assertNotNull(device);
        }
    }

    // determineDpi(GraphicsDevice)

    @Test
    public void UT38_determineDpi() {
        Assume.assumeFalse(GraphicsEnvironment.isHeadless());

        GraphicsDevice device = Utils.getGoodGraphicsDevice();
        Assume.assumeNotNull(device);

        int candidateDpi = Toolkit.getDefaultToolkit()
            .getScreenResolution();

        DisplayMode[] modes = device.getDisplayModes();

        int expected = candidateDpi;

        if (modes.length > 1) {
            int highest = Arrays.stream(modes)
                .map(DisplayMode::getHeight)
                .max(Integer::compare)
                .get();

            int current = device.getDisplayMode().getHeight();
            expected = (candidateDpi * current) / highest;
        }

        assertEquals(expected, Utils.determineDpi(device));
    }
}
