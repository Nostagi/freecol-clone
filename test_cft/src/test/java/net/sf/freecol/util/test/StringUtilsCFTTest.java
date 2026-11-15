
package net.sf.freecol.util.test;

import static org.junit.Assert.*;

import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import org.junit.Test;

import net.sf.freecol.common.util.StringUtils;

public class StringUtilsCFTTest {

    private enum TestEnum {
        HELLO_WORLD,
        FIRST_SECOND_THIRD,
        VALUE_,
        A__B
    }

    private FontMetrics getFontMetrics() {
        BufferedImage image = new BufferedImage(
            200, 100, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        FontMetrics metrics = graphics.getFontMetrics();
        graphics.dispose();
        return metrics;
    }

    // SU01 - SU05: join(String, String...)

    @Test
    public void SU01_joinNullArray() {
        assertNull(StringUtils.join(",", (String[]) null));
    }

    @Test
    public void SU02_joinEmptyArray() {
        assertNull(StringUtils.join(",", new String[0]));
    }

    @Test
    public void SU03_joinSingleElement() {
        assertEquals("Java", StringUtils.join(",", "Java"));
    }

    @Test
    public void SU04_joinMultipleElements() {
        assertEquals("A,B,C", StringUtils.join(",", "A", "B", "C"));
    }

    @Test
    public void SU05_joinWithNullElement() {
        assertEquals("A,null,B", StringUtils.join(",", "A", null, "B"));
    }

    // SU06 - SU07: join(String, Collection<String>)

    @Test
    public void SU06_joinCollection() {
        assertEquals("A-B-C",
            StringUtils.join("-", Arrays.asList("A", "B", "C")));
    }

    @Test
    public void SU07_joinEmptyCollection() {
        assertNull(StringUtils.join(",", Collections.<String>emptyList()));
    }

    // SU08 - SU11: chop(String, int)

    @Test
    public void SU08_chopLongerString() {
        assertEquals("Free", StringUtils.chop("FreeCol", 4));
    }

    @Test
    public void SU09_chopEqualLength() {
        assertEquals("Java", StringUtils.chop("Java", 4));
    }

    @Test
    public void SU10_chopShorterString() {
        assertEquals("Hi", StringUtils.chop("Hi", 5));
    }

    @Test
    public void SU11_chopZeroLength() {
        assertEquals("", StringUtils.chop("Java", 0));
    }

    // SU12 - SU14: firstPart(String, String)

    @Test
    public void SU12_firstPartNullString() {
        assertNull(StringUtils.firstPart(null, "."));
    }

    @Test
    public void SU13_firstPartDelimiterFound() {
        assertEquals("freecol",
            StringUtils.firstPart("freecol.game", "."));
    }

    @Test
    public void SU14_firstPartDelimiterNotFound() {
        assertEquals("freecol",
            StringUtils.firstPart("freecol", "."));
    }

    // SU15 - SU18: lastPart(String, String)

    @Test
    public void SU15_lastPartNullString() {
        assertNull(StringUtils.lastPart(null, "."));
    }

    @Test
    public void SU16_lastPartDelimiterFound() {
        assertEquals("java",
            StringUtils.lastPart("net.sf.java", "."));
    }

    @Test
    public void SU17_lastPartMultipleDelimiters() {
        assertEquals("java",
            StringUtils.lastPart("net.sf.freecol.java", "."));
    }

    @Test
    public void SU18_lastPartDelimiterNotFound() {
        assertEquals("freecol",
            StringUtils.lastPart("freecol", "."));
    }

    // SU19 - SU22: getEnumKey(Enum<?>)

    @Test
    public void SU19_getEnumKeySingleUnderscore() {
        assertEquals("helloWorld",
            StringUtils.getEnumKey(TestEnum.HELLO_WORLD));
    }

    @Test
    public void SU20_getEnumKeyMultipleUnderscores() {
        assertEquals("firstSecondThird",
            StringUtils.getEnumKey(TestEnum.FIRST_SECOND_THIRD));
    }

    @Test
    public void SU21_getEnumKeyTrailingUnderscore() {
        assertEquals("value", StringUtils.getEnumKey(TestEnum.VALUE_));
    }

    @Test
    public void SU22_getEnumKeyConsecutiveUnderscores() {
        assertEquals("aB", StringUtils.getEnumKey(TestEnum.A__B));
    }

    // SU23 - SU25: getBreakingPoint(String)

    @Test
    public void SU23_getBreakingPointAtCenter() {
        assertEquals(5, StringUtils.getBreakingPoint("hello world"));
    }

    @Test
    public void SU24_getBreakingPointNoDelimiter() {
        assertEquals(-1, StringUtils.getBreakingPoint("helloworld"));
    }

    @Test
    public void SU25_getBreakingPointNearCenter() {
        assertEquals(4, StringUtils.getBreakingPoint("hell oworld"));
    }

    // SU26 - SU30: splitText(String, String, FontMetrics, int)

    @Test
    public void SU26_splitTextEmptyString() {
        assertTrue(StringUtils.splitText(
            "", " ", getFontMetrics(), 100).isEmpty());
    }

    @Test
    public void SU27_splitTextFitsWidth() {
        List<String> result = StringUtils.splitText(
            "one two", " ", getFontMetrics(), 200);

        assertEquals(Arrays.asList("one two"), result);
    }

    @Test
    public void SU28_splitTextSplitsWords() {
        FontMetrics metrics = getFontMetrics();
        int width = Math.max(
            metrics.stringWidth("one"),
            Math.max(metrics.stringWidth("two"),
                     metrics.stringWidth("three")));

        List<String> result = StringUtils.splitText(
            "one two three", " ", metrics, width);

        assertEquals(Arrays.asList("one", "two", "three"), result);
    }

    @Test
    public void SU29_splitTextWordExceedsWidth() {
        List<String> result = StringUtils.splitText(
            "longword", " ", getFontMetrics(), 1);

        assertEquals(Arrays.asList("longword"), result);
    }

    @Test
    public void SU30_splitTextMultipleDelimiters() {
        List<String> result = StringUtils.splitText(
            "one   two", " ", getFontMetrics(), 200);

        assertEquals(Arrays.asList("one two"), result);
    }

    // SU31 - SU36: capitalize()

    @Test
    public void SU31_capitalizeNull() {
        assertNull(StringUtils.capitalize(null));
    }

    @Test
    public void SU32_capitalizeEmptyString() {
        assertEquals("", StringUtils.capitalize(""));
    }

    @Test
    public void SU33_capitalizeLowercaseString() {
        assertEquals("Freecol", StringUtils.capitalize("freecol"));
    }

    @Test
    public void SU34_capitalizeUppercaseString() {
        assertEquals("Java", StringUtils.capitalize("Java"));
    }

    @Test
    public void SU35_capitalizeWithTurkishLocale() {
        assertEquals("İstanbul",
            StringUtils.capitalize("istanbul", new Locale("tr")));
    }

    @Test
    public void SU36_capitalizeNullWithLocale() {
        assertNull(StringUtils.capitalize(null, Locale.US));
    }

    // SU37 - SU39: downCase()

    @Test
    public void SU37_downCaseNull() {
        assertNull(StringUtils.downCase(null));
    }

    @Test
    public void SU38_downCaseEmptyString() {
        assertEquals("", StringUtils.downCase(""));
    }

    @Test
    public void SU39_downCaseUppercaseString() {
        assertEquals("freecol", StringUtils.downCase("FREECOL"));
    }

    // SU40 - SU42: upCase()

    @Test
    public void SU40_upCaseNull() {
        assertNull(StringUtils.upCase(null));
    }

    @Test
    public void SU41_upCaseEmptyString() {
        assertEquals("", StringUtils.upCase(""));
    }

    @Test
    public void SU42_upCaseLowercaseString() {
        assertEquals("FREECOL", StringUtils.upCase("freecol"));
    }
}
