
package net.sf.freecol.common.model;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.sf.freecol.common.io.FreeColXMLReader;
import net.sf.freecol.util.test.FreeColTestCase;


public class ModifierCFTTest extends FreeColTestCase {

    private Modifier createModifier(Modifier.ModifierType type,
                                    float value) {
        return new Modifier("test.modifier", value, type);
    }

    public void testM01_constructors() {
        Modifier m1 = new Modifier(spec());
        assertNotNull(m1);
        assertEquals(Modifier.DEFAULT_MODIFIER_INDEX,
                     m1.getModifierIndex());

        Modifier m2 = new Modifier("test.add", 5.0f,
                                   Modifier.ModifierType.ADDITIVE);
        assertEquals("test.add", m2.getId());
        assertEquals(5.0f, m2.getValue(), 0.001f);
        assertEquals(Modifier.ModifierType.ADDITIVE, m2.getType());

        Modifier m3 = new Modifier("test.mul", 2.0f,
                                   Modifier.ModifierType.MULTIPLICATIVE,
                                   null, 30);
        assertEquals(30, m3.getModifierIndex());

        Modifier m4 = new Modifier("test.percent", 10.0f,
                                   Modifier.ModifierType.PERCENTAGE,
                                   null);
        assertEquals(Modifier.DEFAULT_MODIFIER_INDEX,
                     m4.getModifierIndex());
    }

    public void testM02_floatKnown() {
        assertTrue(Modifier.isFloatKnown(0.0f));
        assertTrue(Modifier.isFloatKnown(1.5f));
        assertFalse(Modifier.isFloatKnown(Modifier.UNKNOWN));

        Modifier known = createModifier(
            Modifier.ModifierType.ADDITIVE, 10.0f);
        Modifier unknown = createModifier(
            Modifier.ModifierType.ADDITIVE, Modifier.UNKNOWN);

        assertTrue(known.isKnown());
        assertFalse(unknown.isKnown());
    }

    public void testM03_settersAndGetters() {
        Modifier m = createModifier(
            Modifier.ModifierType.ADDITIVE, 5.0f);

        m.setType(Modifier.ModifierType.MULTIPLICATIVE);
        m.setValue(8.0f);
        m.setModifierIndex(40);

        assertEquals(Modifier.ModifierType.MULTIPLICATIVE, m.getType());
        assertEquals(8.0f, m.getValue(), 0.001f);
        assertEquals(40, m.getModifierIndex());

        m.setIncrement(2.5f);
        m.setIncrementType(Modifier.ModifierType.ADDITIVE);

        assertTrue(m.hasIncrement());
        assertEquals(2.5f, m.getIncrement(), 0.001f);
        assertEquals(Modifier.ModifierType.ADDITIVE,
                     m.getIncrementType());

        m.setIncrementType(null);
        assertFalse(m.hasIncrement());
    }

    public void testM04_applyAdditive() {
        Modifier m = createModifier(
            Modifier.ModifierType.ADDITIVE, 5.0f);

        assertEquals(15.0f, m.apply(10.0f, 5.0f), 0.001f);
        assertEquals(15.0f, m.applyTo(10.0f), 0.001f);
    }

    public void testM05_applyMultiplicative() {
        Modifier m = createModifier(
            Modifier.ModifierType.MULTIPLICATIVE, 3.0f);

        assertEquals(30.0f, m.apply(10.0f, 3.0f), 0.001f);
        assertEquals(30.0f, m.applyTo(10.0f), 0.001f);
    }

    public void testM06_applyPercentage() {
        Modifier m = createModifier(
            Modifier.ModifierType.PERCENTAGE, 20.0f);

        assertEquals(12.0f, m.apply(10.0f, 20.0f), 0.001f);
        assertEquals(12.0f, m.applyTo(10.0f), 0.001f);
    }

    public void testM07_applyZeroAndNegativeValues() {
        Modifier zero = createModifier(
            Modifier.ModifierType.ADDITIVE, 0.0f);
        Modifier negative = createModifier(
            Modifier.ModifierType.ADDITIVE, -5.0f);

        assertEquals(10.0f, zero.applyTo(10.0f), 0.001f);
        assertEquals(5.0f, negative.applyTo(10.0f), 0.001f);
    }

    public void testM08_applyToWithoutIncrement() {
        Modifier m = createModifier(
            Modifier.ModifierType.ADDITIVE, 5.0f);

        assertFalse(m.hasIncrement());
        assertEquals(15.0f, m.applyTo(10.0f, new Turn(1)), 0.001f);
    }

    public void testM09_setIncrementNullFirstTurn() {
        Modifier m = createModifier(
            Modifier.ModifierType.ADDITIVE, 10.0f);

        try {
            m.setIncrement(Modifier.ModifierType.ADDITIVE,
                           2.0f, null, new Turn(5));
            fail("Expected RuntimeException");
        } catch (RuntimeException expected) {
            assertNotNull(expected.getMessage());
        }
    }

    public void testM10_incrementAdditive() {
        Modifier m = createModifier(
            Modifier.ModifierType.ADDITIVE, 10.0f);

        m.setIncrement(Modifier.ModifierType.ADDITIVE,
                       2.0f, new Turn(1), new Turn(5));

        assertTrue(m.hasIncrement());
        assertEquals(10.0f, m.getValue(new Turn(1)), 0.001f);
        assertEquals(12.0f, m.getValue(new Turn(2)), 0.001f);
        assertEquals(14.0f, m.getValue(new Turn(3)), 0.001f);
        assertEquals(22.0f, m.applyTo(10.0f, new Turn(2)), 0.001f);
    }

    public void testM11_incrementMultiplicative() {
        Modifier m = createModifier(
            Modifier.ModifierType.MULTIPLICATIVE, 10.0f);

        m.setIncrement(Modifier.ModifierType.MULTIPLICATIVE,
                       2.0f, new Turn(1), new Turn(5));

        assertEquals(20.0f, m.getValue(new Turn(2)), 0.001f);
        assertEquals(40.0f, m.getValue(new Turn(3)), 0.001f);
    }

    public void testM12_incrementPercentage() {
        Modifier m = createModifier(
            Modifier.ModifierType.PERCENTAGE, 10.0f);

        m.setIncrement(Modifier.ModifierType.PERCENTAGE,
                       5.0f, new Turn(1), new Turn(5));

        assertEquals(10.0f, m.getValue(new Turn(1)), 0.001f);
        assertEquals(10.5f, m.getValue(new Turn(2)), 0.001f);
        assertEquals(11.0f, m.getValue(new Turn(3)), 0.001f);
    }

    public void testM13_incrementOutsideTimeRange() {
        Modifier m = createModifier(
            Modifier.ModifierType.ADDITIVE, 10.0f);

        m.setIncrement(Modifier.ModifierType.ADDITIVE,
                       2.0f, new Turn(2), new Turn(4));

        assertEquals(0.0f, m.getValue(new Turn(1)), 0.001f);
        assertEquals(10.0f, m.getValue(new Turn(2)), 0.001f);
        assertEquals(14.0f, m.getValue(new Turn(4)), 0.001f);
        assertEquals(0.0f, m.getValue(new Turn(5)), 0.001f);
    }

    public void testM14_copyIn() {
        Modifier source = new Modifier(
            "test.copy", 15.0f,
            Modifier.ModifierType.MULTIPLICATIVE, null, 50);

        source.setIncrement(Modifier.ModifierType.ADDITIVE,
                            3.0f, new Turn(1), new Turn(4));

        Modifier target = new Modifier(
            "test.copy", 1.0f,
            Modifier.ModifierType.ADDITIVE);

        assertTrue(target.copyIn(source));

        assertEquals(source.getType(), target.getType());
        assertEquals(source.getValue(), target.getValue(), 0.001f);
        assertEquals(source.getIncrement(), target.getIncrement(),
                     0.001f);
        assertEquals(source.getIncrementType(),
                     target.getIncrementType());
        assertEquals(source.getModifierIndex(),
                     target.getModifierIndex());
    }

    public void testM15_copyInNull() {
        Modifier target = createModifier(
            Modifier.ModifierType.ADDITIVE, 5.0f);

        assertFalse(target.copyIn(null));
    }

    public void testM16_makeModifier() {
        Modifier source = new Modifier(
            "test.original", 12.0f,
            Modifier.ModifierType.PERCENTAGE, null, 25);

        source.setIncrement(Modifier.ModifierType.ADDITIVE,
                            2.0f, new Turn(1), new Turn(3));

        Modifier copy = Modifier.makeModifier(source);

        assertNotSame(source, copy);
        assertEquals(source.getId(), copy.getId());
        assertEquals(source.getValue(), copy.getValue(), 0.001f);
        assertEquals(source.getType(), copy.getType());
        assertEquals(source.getIncrement(), copy.getIncrement(),
                     0.001f);
        assertEquals(source.getIncrementType(),
                     copy.getIncrementType());
        assertEquals(source.getModifierIndex(),
                     copy.getModifierIndex());
    }

    public void testM17_equalsSameObject() {
        Modifier m = createModifier(
            Modifier.ModifierType.ADDITIVE, 5.0f);

        assertTrue(m.equals(m));
    }

    public void testM18_equalsDifferentObjectType() {
        Modifier m = createModifier(
            Modifier.ModifierType.ADDITIVE, 5.0f);

        assertFalse(m.equals(null));
        assertFalse(m.equals("not a modifier"));
    }

    public void testM19_equalsEqualModifiers() {
        Modifier m1 = new Modifier(
            "test.equal", 5.0f,
            Modifier.ModifierType.ADDITIVE, null, 10);

        Modifier m2 = new Modifier(
            "test.equal", 5.0f,
            Modifier.ModifierType.ADDITIVE, null, 10);

        assertTrue(m1.equals(m2));
        assertEquals(m1.hashCode(), m2.hashCode());
    }

    public void testM20_equalsDifferentValues() {
        Modifier m1 = createModifier(
            Modifier.ModifierType.ADDITIVE, 5.0f);
        Modifier m2 = createModifier(
            Modifier.ModifierType.ADDITIVE, 6.0f);

        assertFalse(m1.equals(m2));
    }

    public void testM21_equalsDifferentIncrement() {
        Modifier m1 = createModifier(
            Modifier.ModifierType.ADDITIVE, 5.0f);
        Modifier m2 = createModifier(
            Modifier.ModifierType.ADDITIVE, 5.0f);

        m1.setIncrement(2.0f);
        m2.setIncrement(3.0f);

        assertFalse(m1.equals(m2));
    }

    public void testM22_equalsDifferentType() {
        Modifier m1 = createModifier(
            Modifier.ModifierType.ADDITIVE, 5.0f);
        Modifier m2 = createModifier(
            Modifier.ModifierType.MULTIPLICATIVE, 5.0f);

        assertFalse(m1.equals(m2));
    }

    public void testM23_equalsDifferentIndex() {
        Modifier m1 = new Modifier(
            "test.index", 5.0f,
            Modifier.ModifierType.ADDITIVE, null, 10);
        Modifier m2 = new Modifier(
            "test.index", 5.0f,
            Modifier.ModifierType.ADDITIVE, null, 20);

        assertFalse(m1.equals(m2));
    }

    public void testM24_hashCodeChangesWithFields() {
        Modifier m1 = new Modifier(
            "test.hash", 5.0f,
            Modifier.ModifierType.ADDITIVE, null, 10);
        Modifier m2 = new Modifier(
            "test.hash", 5.0f,
            Modifier.ModifierType.ADDITIVE, null, 20);

        assertFalse(m1.hashCode() == m2.hashCode());
    }

    public void testM25_toStringWithoutSource() {
        Modifier m = new Modifier(
            "test.string", 5.0f,
            Modifier.ModifierType.ADDITIVE);

        String result = m.toString();

        assertTrue(result.contains("test.string"));
        assertTrue(result.contains("ADDITIVE"));
        assertTrue(result.contains("5.0"));
        assertTrue(result.contains("index=0"));
    }

    public void testM26_toStringNegativeIndex() {
        Modifier m = new Modifier(
            "test.negative", 5.0f,
            Modifier.ModifierType.ADDITIVE, null, -1);

        assertFalse(m.toString().contains("index="));
    }

    public void testM27_getXMLTagName() {
        Modifier m = createModifier(
            Modifier.ModifierType.ADDITIVE, 5.0f);

        assertEquals(Modifier.TAG, m.getXMLTagName());
        assertEquals("modifier", m.getXMLTagName());
    }

    public void testM28_comparatorByIndex() {
        Modifier low = new Modifier(
            "test.low", 1.0f,
            Modifier.ModifierType.ADDITIVE, null, 10);
        Modifier high = new Modifier(
            "test.high", 1.0f,
            Modifier.ModifierType.ADDITIVE, null, 20);

        List<Modifier> modifiers = new ArrayList<>();
        modifiers.add(high);
        modifiers.add(low);

        Collections.sort(
            modifiers, Modifier.ascendingModifierIndexComparator);

        assertSame(low, modifiers.get(0));
        assertSame(high, modifiers.get(1));
    }

    public void testM29_comparatorByType() {
        Modifier additive = new Modifier(
            "test.add", 1.0f,
            Modifier.ModifierType.ADDITIVE, null, 10);
        Modifier percentage = new Modifier(
            "test.percent", 1.0f,
            Modifier.ModifierType.PERCENTAGE, null, 10);

        List<Modifier> modifiers = new ArrayList<>();
        modifiers.add(percentage);
        modifiers.add(additive);

        Collections.sort(
            modifiers, Modifier.ascendingModifierIndexComparator);

        assertSame(additive, modifiers.get(0));
        assertSame(percentage, modifiers.get(1));
    }

    public void testM30_comparatorSameModifier() {
        Modifier m = new Modifier(
            "test.same", 1.0f,
            Modifier.ModifierType.ADDITIVE, null, 10);

        assertEquals(0, Modifier.ascendingModifierIndexComparator
                     .compare(m, m));
    }

    public void testM31_makeTimedModifierWithZeroDuration() {
        Modifier template = new Modifier(
            "test.timed.zero", 10.0f,
            Modifier.ModifierType.ADDITIVE);

        template.setIncrementType(Modifier.ModifierType.ADDITIVE);
        template.setIncrement(-2.0f);
        template.setTemporary(true);

        Modifier result = Modifier.makeTimedModifier(
            "test.timed.result", template, new Turn(10));

        assertNotNull(result);
        assertEquals("test.timed.result", result.getId());
        assertEquals(-2.0f, result.getIncrement(), 0.001f);
        assertEquals(Modifier.ModifierType.ADDITIVE,
                     result.getIncrementType());
        assertEquals(10, result.getFirstTurn().getNumber());
        assertEquals(15, result.getLastTurn().getNumber());
        assertTrue(result.isTemporary());
    }

    public void testM32_makeTimedModifierWithNonZeroDuration() {
        Modifier template = new Modifier(
            "test.timed.duration", 20.0f,
            Modifier.ModifierType.ADDITIVE);

        template.setIncrementType(Modifier.ModifierType.ADDITIVE);
        template.setIncrement(-2.0f);
        template.setDuration(5);

        Modifier result = Modifier.makeTimedModifier(
            "test.timed.duration.result", template, new Turn(20));

        assertNotNull(result);
        assertEquals(Modifier.ModifierType.ADDITIVE,
                     result.getIncrementType());
        assertEquals(20, result.getFirstTurn().getNumber());
        assertEquals(25, result.getLastTurn().getNumber());
    }

    public void testM33_equalsDifferentSources() {
        Modifier m1 = new Modifier(
            "test.equals.source", 10.0f,
            Modifier.ModifierType.ADDITIVE);
        Modifier m2 = new Modifier(
            "test.equals.source", 10.0f,
            Modifier.ModifierType.ADDITIVE);

        m1.setSource(new Modifier("source.one", 1.0f,
                                  Modifier.ModifierType.ADDITIVE));
        m2.setSource(new Modifier("source.two", 1.0f,
                                  Modifier.ModifierType.ADDITIVE));

        assertFalse(m1.equals(m2));
    }

    public void testM34_equalsDifferentDuration() {
        Modifier m1 = new Modifier(
            "test.equals.duration", 10.0f,
            Modifier.ModifierType.ADDITIVE);
        Modifier m2 = new Modifier(
            "test.equals.duration", 10.0f,
            Modifier.ModifierType.ADDITIVE);

        m1.setDuration(5);
        m2.setDuration(10);

        assertFalse(m1.equals(m2));
    }

    public void testM35_equalsDifferentTemporary() {
        Modifier m1 = new Modifier(
            "test.equals.temporary", 10.0f,
            Modifier.ModifierType.ADDITIVE);
        Modifier m2 = new Modifier(
            "test.equals.temporary", 10.0f,
            Modifier.ModifierType.ADDITIVE);

        m1.setTemporary(true);

        assertFalse(m1.equals(m2));
    }

    public void testM36_equalsDifferentFirstTurn() {
        Modifier m1 = new Modifier(
            "test.equals.firstTurn", 10.0f,
            Modifier.ModifierType.ADDITIVE);
        Modifier m2 = new Modifier(
            "test.equals.firstTurn", 10.0f,
            Modifier.ModifierType.ADDITIVE);

        m1.setFirstTurn(new Turn(5));
        m2.setFirstTurn(new Turn(10));

        assertFalse(m1.equals(m2));
    }

    public void testM37_equalsDifferentLastTurn() {
        Modifier m1 = new Modifier(
            "test.equals.lastTurn", 10.0f,
            Modifier.ModifierType.ADDITIVE);
        Modifier m2 = new Modifier(
            "test.equals.lastTurn", 10.0f,
            Modifier.ModifierType.ADDITIVE);

        m1.setLastTurn(new Turn(15));
        m2.setLastTurn(new Turn(20));

        assertFalse(m1.equals(m2));
    }

    public void testM38_copyInFeatureAttributes() {
        Modifier source = new Modifier(
            "test.copy.feature", 10.0f,
            Modifier.ModifierType.ADDITIVE);

        source.setSource(new Modifier(
            "test.copy.source", 1.0f,
            Modifier.ModifierType.ADDITIVE));
        source.setFirstTurn(new Turn(5));
        source.setLastTurn(new Turn(15));
        source.setDuration(10);
        source.setTemporary(true);

        Modifier target = new Modifier(
            "test.copy.feature", 1.0f,
            Modifier.ModifierType.ADDITIVE);

        assertTrue(target.copyIn(source));

        assertSame(source.getSource(), target.getSource());
        assertEquals(5, target.getFirstTurn().getNumber());
        assertEquals(15, target.getLastTurn().getNumber());
        assertEquals(10, target.getDuration());
        assertTrue(target.isTemporary());
    }

    public void testM39_getValueWithoutIncrement() {
        Modifier modifier = new Modifier(
            "test.value.noIncrement", 15.0f,
            Modifier.ModifierType.ADDITIVE);

        modifier.setFirstTurn(new Turn(5));
        modifier.setLastTurn(new Turn(15));

        assertEquals(15.0f, modifier.getValue(new Turn(10)), 0.001f);
        assertEquals(0.0f, modifier.getValue(new Turn(20)), 0.001f);
    }

    public void testM40_toStringWithoutSourceAndNegativeIndex() {
        Modifier modifier = new Modifier(
            "test.modifier", 10.0f,
            Modifier.ModifierType.ADDITIVE
        );
        modifier.setModifierIndex(-1);

        String result = modifier.toString();

        assertTrue(result.contains("[Modifier test.modifier"));
        assertTrue(result.contains("ADDITIVE"));
        assertTrue(result.contains("10.0"));
        assertFalse(result.contains("index="));
    }

    public void testM41_toStringWithSourceAndIndex() {
        FreeColObject source = new Modifier(
            "test.source", 1.0f, Modifier.ModifierType.ADDITIVE
        );

        Modifier modifier = new Modifier(
            "test.modifier", 5.0f,
            Modifier.ModifierType.MULTIPLICATIVE, source, 10
        );

        String result = modifier.toString();

        assertTrue(result.contains("(test.source)"));
        assertTrue(result.contains("MULTIPLICATIVE"));
        assertTrue(result.contains("index=10"));
    }

    public void testM42_writeAttributesWithoutIncrement() throws Exception {
        Modifier modifier = new Modifier(
            "test.write.default", 10.0f,
            Modifier.ModifierType.ADDITIVE
        );
        modifier.setModifierIndex(-1);

        String xml = modifier.serialize();
        System.out.println("M42 XML = [" + xml + "]");

        assertTrue(xml.contains("value=\"10.0\""));
assertTrue(xml.contains("type=\"additive\""));
assertFalse(xml.contains("increment-type="));
assertFalse(xml.contains("increment="));
assertFalse(xml.contains("index="));
    }

    public void testM43_writeAttributesWithIncrementAndIndex()
        throws Exception {
        Modifier modifier = new Modifier(
            "test.write.full", 20.0f,
            Modifier.ModifierType.PERCENTAGE
        );
        modifier.setIncrementType(Modifier.ModifierType.ADDITIVE);
        modifier.setIncrement(2.5f);
        modifier.setModifierIndex(10);

        String xml = modifier.serialize();
        System.out.println("M43 XML = [" + xml + "]");

        assertTrue(xml.contains("increment-type=\"additive\""));
assertTrue(xml.contains("increment=\"2.5\""));
assertTrue(xml.contains("index=\"10\""));
    }

    public void testM44_readAttributesLegacyIncrementType()
        throws Exception {
        String xml = "<modifier id=\"test.legacy\" "
            + "value=\"10.0\" type=\"ADDITIVE\" "
            + "incrementType=\"MULTIPLICATIVE\" "
            + "increment=\"2.0\" index=\"5\"/>";

        try (FreeColXMLReader xr =
                 new FreeColXMLReader(new StringReader(xml))) {
            xr.nextTag();

            Modifier modifier = new Modifier(xr, spec());

            assertEquals("test.legacy", modifier.getId());
            assertEquals(10.0f, modifier.getValue(), 0.001f);
            assertEquals(Modifier.ModifierType.ADDITIVE,
                         modifier.getType());
            assertEquals(Modifier.ModifierType.MULTIPLICATIVE,
                         modifier.getIncrementType());
            assertEquals(2.0f, modifier.getIncrement(), 0.001f);
            assertEquals(5, modifier.getModifierIndex());
        }
    }

    public void testM45_readAttributesNewIncrementType()
        throws Exception {
        String xml = "<modifier id=\"test.new\" "
            + "value=\"15.0\" type=\"PERCENTAGE\" "
            + "increment-type=\"ADDITIVE\" "
            + "increment=\"3.0\" index=\"8\"/>";

        try (FreeColXMLReader xr =
                 new FreeColXMLReader(new StringReader(xml))) {
            xr.nextTag();

            Modifier modifier = new Modifier(xr, spec());

            assertEquals(Modifier.ModifierType.PERCENTAGE,
                         modifier.getType());
            assertEquals(Modifier.ModifierType.ADDITIVE,
                         modifier.getIncrementType());
            assertEquals(3.0f, modifier.getIncrement(), 0.001f);
            assertEquals(8, modifier.getModifierIndex());
        }
    }

    public void testM46_readAttributesWithoutIncrement()
        throws Exception {
        String xml = "<modifier id=\"test.no.increment\" "
            + "value=\"7.0\" type=\"ADDITIVE\"/>";

        try (FreeColXMLReader xr =
                 new FreeColXMLReader(new StringReader(xml))) {
            xr.nextTag();

            Modifier modifier = new Modifier(xr, spec());

            assertEquals(7.0f, modifier.getValue(), 0.001f);
            assertEquals(Modifier.ModifierType.ADDITIVE,
                         modifier.getType());
            assertFalse(modifier.hasIncrement());
            assertEquals(0.0f, modifier.getIncrement(), 0.001f);
            assertEquals(Modifier.DEFAULT_MODIFIER_INDEX,
                         modifier.getModifierIndex());
        }
    }

    public void testM47_copyInNullObject() {
        Modifier modifier = new Modifier(
            "test.copy.null", 10.0f,
            Modifier.ModifierType.ADDITIVE
        );

        assertFalse(modifier.copyIn((FreeColObject) null));
    }

    public void testM48_equalsDifferentIncrementType() {
        Modifier m1 = createModifier(
            Modifier.ModifierType.ADDITIVE, 5.0f);
        Modifier m2 = createModifier(
            Modifier.ModifierType.ADDITIVE, 5.0f);

        m1.setIncrementType(Modifier.ModifierType.ADDITIVE);
        m2.setIncrementType(Modifier.ModifierType.MULTIPLICATIVE);

        assertFalse(m1.equals(m2));
    }

    public void testM49_comparatorByFreeColObjectOrder() {
        Modifier first = new Modifier(
            "test.a", 1.0f,
            Modifier.ModifierType.ADDITIVE, null, 10);
        Modifier second = new Modifier(
            "test.b", 1.0f,
            Modifier.ModifierType.ADDITIVE, null, 10);

        assertTrue(
            Modifier.ascendingModifierIndexComparator
                .compare(first, second) < 0);
    }

    public void testM50_comparatorBySource() {
        FreeColObject sourceA = new Modifier(
            "source.a", 1.0f, Modifier.ModifierType.ADDITIVE);
        FreeColObject sourceB = new Modifier(
            "source.b", 1.0f, Modifier.ModifierType.ADDITIVE);

        Modifier first = new Modifier(
            "test.same", 1.0f,
            Modifier.ModifierType.ADDITIVE, sourceA, 10);
        Modifier second = new Modifier(
            "test.same", 1.0f,
            Modifier.ModifierType.ADDITIVE, sourceB, 10);

        assertTrue(
            Modifier.ascendingModifierIndexComparator
                .compare(first, second) < 0);
    }

    public void testM51_copyInDifferentObjectType() {
        Modifier modifier = createModifier(
            Modifier.ModifierType.ADDITIVE, 5.0f);

        Ability ability = new Ability("test.ability", true);

        assertFalse(modifier.copyIn(ability));
    }

    public void testM52_readAttributesDefaultValues()
        throws Exception {
        String xml = "<modifier id=\"test.defaults\"/>";

        try (FreeColXMLReader xr =
                 new FreeColXMLReader(new StringReader(xml))) {
            xr.nextTag();

            Modifier modifier = new Modifier(xr, spec());

            assertEquals("test.defaults", modifier.getId());
            assertEquals(Modifier.UNKNOWN,
                         modifier.getValue(), 0.001f);
            assertNull(modifier.getType());
            assertFalse(modifier.hasIncrement());
            assertEquals(0.0f, modifier.getIncrement(), 0.001f);
            assertEquals(Modifier.DEFAULT_MODIFIER_INDEX,
                         modifier.getModifierIndex());
        }
    }
}
