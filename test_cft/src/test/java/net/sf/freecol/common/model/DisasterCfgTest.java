package net.sf.freecol.common.model;

import java.io.StringReader;
import java.io.StringWriter;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;

import net.sf.freecol.common.io.FreeColXMLReader;
import net.sf.freecol.common.io.FreeColXMLWriter;
import net.sf.freecol.common.util.RandomChoice;
import net.sf.freecol.util.test.FreeColTestCase;

/**
 * CFG-driven test suite for {@link Disaster} derived from
 * {@code test_cft/graph/net/sf/freecol/common/model/Disaster.json}.
 *
 * Covers all 15 methods (M1..M15) and achieves 100% feasible C3 (node + edge)
 * coverage across 25 Test Paths.
 *
 * Infeasible edges documented:
 * - M7 ({@code copyIn}): {@code N1 -> N2} ({@code B2:false}, {@code !super.copyIn(o)}) —
 *   once {@code copyInCast(other, Disaster.class)} returns a non-null {@code Disaster o},
 *   {@code FreeColSpecObjectType.copyIn(o)} delegates up to {@code FreeColObject.copyIn(o)},
 *   which unconditionally returns {@code true} for any non-null {@code FreeColObject}.
 */
public class DisasterCfgTest extends FreeColTestCase {

    /**
     * Subclass exposing protected {@link Disaster#setEffects}, {@link Disaster#clearContainers},
     * and {@link Disaster#readChildren} for direct CFG branch coverage without modifying {@code src/}.
     */
    private static class TestableDisaster extends Disaster {
        public TestableDisaster(String id, Specification specification) {
            super(id, specification);
        }

        public void callSetEffects(List<RandomChoice<Effect>> effects) {
            super.setEffects(effects);
        }

        public void callClearContainers(FreeColXMLReader xr) throws Exception {
            super.clearContainers(xr);
        }

        public void callReadChildren(FreeColXMLReader xr) throws Exception {
            super.readChildren(xr);
        }
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();
        // Pre-condition: Specification is initialized by FreeColTestCase.
    }

    // ------------------------------------------------------------------
    // Pre-condition helpers & stubs
    // ------------------------------------------------------------------

    private Effect createEffect(String id, int probability) throws Exception {
        String xml = "<effect id=\"" + id + "\" probability=\"" + probability + "\"/>";
        try (FreeColXMLReader xr = new FreeColXMLReader(new StringReader(xml))) {
            xr.nextTag();
            return new Effect(xr, spec());
        }
    }

    private void invokeAddEffect(Disaster disaster, Effect effect) throws Exception {
        Method m = Disaster.class.getDeclaredMethod("addEffect", Effect.class);
        m.setAccessible(true);
        m.invoke(disaster, effect);
    }

    private String serializeToXml(Disaster disaster) throws Exception {
        StringWriter sw = new StringWriter();
        try (FreeColXMLWriter xw = new FreeColXMLWriter(sw, FreeColXMLWriter.WriteScope.toSave())) {
            disaster.toXML(xw);
        }
        return sw.toString();
    }

    private void registerType(Disaster disaster) throws Exception {
        Method m = Specification.class.getDeclaredMethod("addType", String.class, FreeColSpecObjectType.class);
        m.setAccessible(true);
        m.invoke(spec(), disaster.getId(), disaster);
    }

    // ------------------------------------------------------------------
    // M1: Disaster(String, Specification) — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M1-TP1: ENTRY -> N0 -> EXIT (initializes id, spec, natural=false, numberOfEffects=ONE)
    public void testM1_TP1_Constructor() {
        Disaster d = new Disaster("model.disaster.test", spec());
        assertEquals("model.disaster.test", d.getId());
        assertEquals(spec(), d.getSpecification());
        assertFalse(d.isNatural());
        assertEquals(Disaster.Effects.ONE, d.getNumberOfEffects());
    }

    // ------------------------------------------------------------------
    // M2: isNatural() — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M2-TP1: ENTRY -> N0 -> EXIT (returns natural flag)
    public void testM2_TP1_IsNatural() {
        Disaster d = new Disaster("model.disaster.test", spec());
        assertFalse(d.isNatural());
    }

    // ------------------------------------------------------------------
    // M3: getNumberOfEffects() — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M3-TP1: ENTRY -> N0 -> EXIT (returns numberOfEffects enum)
    public void testM3_TP1_GetNumberOfEffects() {
        Disaster d = new Disaster("model.disaster.test", spec());
        assertEquals(Disaster.Effects.ONE, d.getNumberOfEffects());
    }

    // ------------------------------------------------------------------
    // M4: getEffects() — 2 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M4-TP1: ENTRY -> N0 -(B1:false)-> N1 -> N3 -> EXIT (effects == null -> returns emptyList)
    public void testM4_TP1_GetEffectsWhenNullReturnsEmptyList() {
        Disaster d = new Disaster("model.disaster.test", spec());
        assertTrue(d.getEffects().isEmpty());
    }

    // M4-TP2: ENTRY -> N0 -(B1:true)-> N2 -> N3 -> EXIT (effects != null -> returns effects list)
    public void testM4_TP2_GetEffectsWhenNonNullReturnsList() throws Exception {
        Disaster d = new Disaster("model.disaster.test", spec());
        invokeAddEffect(d, createEffect("model.disaster.effect.damagedBuilding", 50));
        assertEquals(1, d.getEffects().size());
    }

    // ------------------------------------------------------------------
    // M5: setEffects(List) — 2 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M5-TP1: ENTRY -> N0 -(B1:false)-> N1 -> N3 -> EXIT (this.effects == null -> allocates new list)
    public void testM5_TP1_SetEffectsWhenNull() throws Exception {
        TestableDisaster d = new TestableDisaster("model.disaster.test", spec());
        Effect e = createEffect("model.disaster.effect.damagedBuilding", 50);
        d.callSetEffects(Collections.singletonList(new RandomChoice<>(e, 50)));
        assertEquals(1, d.getEffects().size());
    }

    // M5-TP2: ENTRY -> N0 -(B1:true)-> N2 -> N3 -> EXIT (this.effects != null -> clears and replaces)
    public void testM5_TP2_SetEffectsWhenNonNullClearsPrevious() throws Exception {
        TestableDisaster d = new TestableDisaster("model.disaster.test", spec());
        Effect e1 = createEffect("model.disaster.effect.damagedBuilding", 50);
        Effect e2 = createEffect("model.disaster.effect.lossOfMoney", 25);
        d.callSetEffects(Collections.singletonList(new RandomChoice<>(e1, 50)));
        d.callSetEffects(Collections.singletonList(new RandomChoice<>(e2, 25)));
        assertEquals(1, d.getEffects().size());
        assertEquals("model.disaster.effect.lossOfMoney", d.getEffects().get(0).getObject().getId());
    }

    // ------------------------------------------------------------------
    // M6: addEffect(Effect) — 2 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M6-TP1: ENTRY -> N0 -(B1:false)-> N1 -> N2 -> EXIT (effects == null -> allocates list and adds)
    // M6-TP2: ENTRY -> N0 -(B1:true)-> N2 -> EXIT (effects != null -> adds directly)
    public void testM6_TP1_And_TP2_AddEffectNullThenNonNull() throws Exception {
        Disaster d = new Disaster("model.disaster.test", spec());
        // TP1: effects == null
        invokeAddEffect(d, createEffect("model.disaster.effect.damagedBuilding", 50));
        assertEquals(1, d.getEffects().size());
        // TP2: effects != null
        invokeAddEffect(d, createEffect("model.disaster.effect.lossOfMoney", 25));
        assertEquals(2, d.getEffects().size());
    }

    // ------------------------------------------------------------------
    // M7: copyIn(FreeColObject) — 2 Test Paths (100% feasible C3)
    // ------------------------------------------------------------------

    // M7-TP1: ENTRY -> N0 -(B1:true)-> N2 -> EXIT (other == null or incompatible type -> false)
    public void testM7_TP1_CopyInNullOrWrongTypeReturnsFalse() {
        Disaster target = new Disaster("model.disaster.test", spec());
        assertFalse(target.copyIn((Disaster) null));
        assertFalse(target.copyIn(new Ability(Ability.ARMED)));
    }

    // M7-TP2: ENTRY -> N0 -(B1:false)-> N1 -(B2:true)-> N3 -> EXIT (valid Disaster -> true)
    public void testM7_TP2_CopyInValidDisasterReturnsTrue() throws Exception {
        Disaster target = new Disaster("model.disaster.test", spec());
        target.addAbility(new Ability(Ability.ARMED));
        Disaster source = new Disaster("model.disaster.test", spec());
        source.addAbility(new Ability(Ability.ARMED));
        invokeAddEffect(source, createEffect("model.disaster.effect.damagedBuilding", 50));
        assertTrue(target.copyIn(source));
        assertEquals(1, target.getEffects().size());
    }

    // ------------------------------------------------------------------
    // M8: writeAttributes(FreeColXMLWriter) — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M8-TP1: ENTRY -> N0 -> EXIT (writes natural and effects attributes)
    public void testM8_TP1_WriteAttributes() throws Exception {
        Disaster d = new Disaster("model.disaster.test", spec());
        String xml = serializeToXml(d);
        assertTrue(xml.contains("natural=\"false\""));
        assertTrue(xml.contains("effects=\"one\""));
    }

    // ------------------------------------------------------------------
    // M9: writeChildren(FreeColXMLWriter) — 2 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M9-TP1: ENTRY -> N0 -> N1 -(B1:true)-> N3 -> EXIT (empty effects -> no effect elements)
    public void testM9_TP1_WriteChildrenEmpty() throws Exception {
        Disaster d = new Disaster("model.disaster.test", spec());
        String xml = serializeToXml(d);
        assertFalse(xml.contains("<effect "));
    }

    // M9-TP2: ENTRY -> N0 -> N1 -(B1:false)-> N2 -> N1 -(B1:true)-> N3 -> EXIT (writes each effect)
    public void testM9_TP2_WriteChildrenWithEffects() throws Exception {
        Disaster d = new Disaster("model.disaster.test", spec());
        invokeAddEffect(d, createEffect("model.disaster.effect.damagedBuilding", 50));
        String xml = serializeToXml(d);
        assertTrue(xml.contains("<effect "));
        assertTrue(xml.contains("model.disaster.effect.damagedBuilding"));
    }

    // ------------------------------------------------------------------
    // M10: readAttributes(FreeColXMLReader) — 2 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M10-TP1: ENTRY -> N0 -(B1:false)-> N1 -> N3 -> EXIT (has EFFECTS_TAG attribute)
    public void testM10_TP1_ReadAttributesWithEffectsAttr() throws Exception {
        String xml = "<disaster id=\"model.disaster.cfgTest1\" natural=\"true\" effects=\"ALL\"/>";
        try (FreeColXMLReader xr = new FreeColXMLReader(new StringReader(xml))) {
            xr.nextTag();
            Disaster d = new Disaster("model.disaster.cfgTest1", spec());
            d.readFromXML(xr);
            assertTrue(d.isNatural());
            assertEquals(Disaster.Effects.ALL, d.getNumberOfEffects());
        }
    }

    // M10-TP2: ENTRY -> N0 -(B1:true)-> N2 -> N3 -> EXIT (no EFFECTS_TAG -> inherits parent.numberOfEffects)
    public void testM10_TP2_ReadAttributesWithoutEffectsAttr() throws Exception {
        String xml = "<disaster id=\"model.disaster.cfgTest2\" natural=\"true\"/>";
        try (FreeColXMLReader xr = new FreeColXMLReader(new StringReader(xml))) {
            xr.nextTag();
            Disaster d = new Disaster("model.disaster.cfgTest2", spec());
            d.readFromXML(xr);
            assertTrue(d.isNatural());
            assertEquals(Disaster.Effects.ONE, d.getNumberOfEffects());
        }
    }

    // ------------------------------------------------------------------
    // M11: clearContainers(FreeColXMLReader) — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M11-TP1: ENTRY -> N0 -> EXIT (resets effects to null)
    public void testM11_TP1_ClearContainersResetsEffects() throws Exception {
        TestableDisaster d = new TestableDisaster("model.disaster.test", spec());
        invokeAddEffect(d, createEffect("model.disaster.effect.damagedBuilding", 50));
        assertEquals(1, d.getEffects().size());
        try (FreeColXMLReader xr = new FreeColXMLReader(new StringReader("<disaster id=\"x\"/>"))) {
            xr.nextTag();
            d.callClearContainers(xr);
        }
        assertTrue(d.getEffects().isEmpty());
    }

    // ------------------------------------------------------------------
    // M12: readChildren(FreeColXMLReader) — 4 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M12-TP1: ENTRY -> N0 -(B1:true)-> N7 -> EXIT (parent == this -> skips parent effect copy)
    public void testM12_TP1_ReadChildrenNoExtendsParentIsThis() throws Exception {
        String xml = "<disaster id=\"model.disaster.cfgNoParent\"/>";
        try (FreeColXMLReader xr = new FreeColXMLReader(new StringReader(xml))) {
            xr.nextTag();
            Disaster d = new Disaster("model.disaster.cfgNoParent", spec());
            d.readFromXML(xr);
            assertTrue(d.getEffects().isEmpty());
        }
    }

    // M12-TP2: ENTRY -> N0 -(B1:false)-> N1 -(B2:true)-> N7 -> EXIT
    // (parent != this, parent.getEffects().isEmpty() == true)
    public void testM12_TP2_ReadChildrenWithEmptyParent() throws Exception {
        Disaster parent = new Disaster("model.disaster.cfgEmptyParent", spec());
        registerType(parent);
        String xml = "<disaster id=\"model.disaster.cfgChildOfEmpty\" extends=\"model.disaster.cfgEmptyParent\"/>";
        try (FreeColXMLReader xr = new FreeColXMLReader(new StringReader(xml))) {
            xr.nextTag();
            Disaster child = new Disaster("model.disaster.cfgChildOfEmpty", spec());
            child.readFromXML(xr);
            assertTrue(child.getEffects().isEmpty());
        }
    }

    // M12-TP3: ENTRY -> N0 -(B1:false)-> N1 -(B2:false)-> N2 -(B3:false)-> N3 -> N4 -> N5 -(B4:false)-> N6 -> N5 -(B4:true)-> N7 -> EXIT
    // (parent != this, non-empty parent effects, child effects == null -> allocates list and copies)
    public void testM12_TP3_ReadChildrenCopiesParentEffectsWhenNull() throws Exception {
        Disaster parent = new Disaster("model.disaster.cfgParentWithEffect", spec());
        invokeAddEffect(parent, createEffect("model.disaster.effect.damagedBuilding", 50));
        registerType(parent);
        String xml = "<disaster id=\"model.disaster.cfgChild1\" extends=\"model.disaster.cfgParentWithEffect\"/>";
        try (FreeColXMLReader xr = new FreeColXMLReader(new StringReader(xml))) {
            xr.nextTag();
            Disaster child = new Disaster("model.disaster.cfgChild1", spec());
            child.readFromXML(xr);
            assertEquals(1, child.getEffects().size());
        }
    }

    // M12-TP4: ENTRY -> N0 -(B1:false)-> N1 -(B2:false)-> N2 -(B3:true)-> N4 -> N5 -(B4:false)-> N6 -> N5 -(B4:true)-> N7 -> EXIT
    // (parent != this, non-empty parent effects, child effects != null -> appends without re-allocating)
    public void testM12_TP4_ReadChildrenCopiesParentEffectsWhenNonNull() throws Exception {
        Disaster parent = new Disaster("model.disaster.cfgParentWithEffect2", spec());
        invokeAddEffect(parent, createEffect("model.disaster.effect.damagedBuilding", 50));
        registerType(parent);

        TestableDisaster child = new TestableDisaster("model.disaster.cfgChild2", spec());
        invokeAddEffect(child, createEffect("model.disaster.effect.lossOfMoney", 25));

        String xml = "<disaster id=\"model.disaster.cfgChild2\" extends=\"model.disaster.cfgParentWithEffect2\"/>";
        try (FreeColXMLReader xr = new FreeColXMLReader(new StringReader(xml))) {
            xr.nextTag();
            child.callReadChildren(xr);
        }
        assertEquals(2, child.getEffects().size());
    }

    // ------------------------------------------------------------------
    // M13: readChild(FreeColXMLReader) — 2 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M13-TP1: ENTRY -> N0 -(B1:false)-> N1 -> N3 -> EXIT (tag == "effect" -> reads Effect child)
    public void testM13_TP1_ReadChildEffectTag() throws Exception {
        String xml = "<disaster id=\"model.disaster.cfgReadEffect\">"
                   + "<effect id=\"model.disaster.effect.damagedBuilding\" probability=\"40\"/>"
                   + "</disaster>";
        try (FreeColXMLReader xr = new FreeColXMLReader(new StringReader(xml))) {
            xr.nextTag();
            Disaster d = new Disaster("model.disaster.cfgReadEffect", spec());
            d.readFromXML(xr);
            assertEquals(1, d.getEffects().size());
        }
    }

    // M13-TP2: ENTRY -> N0 -(B1:true)-> N2 -> N3 -> EXIT (non-effect tag -> delegates to super.readChild)
    public void testM13_TP2_ReadChildOtherTagDelegatesToSuper() throws Exception {
        String xml = "<disaster id=\"model.disaster.cfgReadAbility\">"
                   + "<ability id=\"model.ability.armed\" value=\"true\"/>"
                   + "</disaster>";
        try (FreeColXMLReader xr = new FreeColXMLReader(new StringReader(xml))) {
            xr.nextTag();
            Disaster d = new Disaster("model.disaster.cfgReadAbility", spec());
            d.readFromXML(xr);
            assertTrue(d.hasAbility(Ability.ARMED));
        }
    }

    // ------------------------------------------------------------------
    // M14: getXMLTagName() — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M14-TP1: ENTRY -> N0 -> EXIT (returns Disaster.TAG = "disaster")
    public void testM14_TP1_GetXMLTagName() {
        Disaster d = new Disaster("model.disaster.test", spec());
        assertEquals("disaster", d.getXMLTagName());
    }

    // ------------------------------------------------------------------
    // M15: toString() — 2 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M15-TP1: ENTRY -> N0 -> N1 -(B1:true)-> N3 -> EXIT (no effects -> "[id]")
    public void testM15_TP1_ToStringEmptyEffects() {
        Disaster d = new Disaster("model.disaster.test", spec());
        assertEquals("[model.disaster.test]", d.toString());
    }

    // M15-TP2: ENTRY -> N0 -> N1 -(B1:false)-> N2 -> N1 -(B1:true)-> N3 -> EXIT (includes each effect)
    public void testM15_TP2_ToStringWithEffects() throws Exception {
        Disaster d = new Disaster("model.disaster.test", spec());
        invokeAddEffect(d, createEffect("model.disaster.effect.damagedBuilding", 50));
        String s = d.toString();
        assertTrue(s.startsWith("[model.disaster.test "));
        assertTrue(s.endsWith("]"));
    }
}
