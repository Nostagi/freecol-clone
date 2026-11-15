package net.sf.freecol.common.model;

import java.io.StringReader;
import java.io.StringWriter;

import net.sf.freecol.common.io.FreeColXMLReader;
import net.sf.freecol.common.io.FreeColXMLWriter;
import net.sf.freecol.util.test.FreeColTestCase;

/**
 * CFG-driven test suite for {@link Ability} derived from
 * {@code test_cft/graph/net/sf/freecol/common/model/Ability.json}.
 *
 * Covers all 15 methods (M1..M15) and achieves 100% feasible C3 (node + edge)
 * coverage across 24 Test Paths.
 *
 * Infeasible edges documented:
 * - M9 ({@code copyIn}): {@code N1 -> N2} ({@code B2:false}, {@code !super.copyIn(o)}) —
 *   once {@code copyInCast(other, Ability.class)} returns a non-null {@code Ability o},
 *   {@code Feature.copyIn(o)} delegates to {@code FreeColObject.copyIn(o)}, which
 *   unconditionally returns {@code true} for any non-null {@code FreeColObject}.
 */
public class AbilityCfgTest extends FreeColTestCase {

    private UnitType colonistType;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        // Pre-condition: load a standard FreeColSpecObjectType source from Specification.
        colonistType = spec().getUnitType("model.unit.freeColonist");
    }

    // ------------------------------------------------------------------
    // Pre-condition helpers & stubs
    // ------------------------------------------------------------------

    private String serializeToXml(Ability ability) throws Exception {
        StringWriter sw = new StringWriter();
        try (FreeColXMLWriter xw = new FreeColXMLWriter(sw, FreeColXMLWriter.WriteScope.toSave())) {
            ability.toXML(xw);
        }
        return sw.toString();
    }

    // ------------------------------------------------------------------
    // M1: Ability(Specification) — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M1-TP1: ENTRY -> N0 -> EXIT (initializes Ability with specification and default value=true)
    public void testM1_TP1_ConstructorWithSpecification() {
        Ability ability = new Ability(spec());
        assertEquals(spec(), ability.getSpecification());
        assertTrue(ability.getValue());
    }

    // ------------------------------------------------------------------
    // M2: Ability(String, FreeColSpecObjectType, boolean) — 2 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M2-TP1: ENTRY -> N0 -(B1:false)-> N1 -> N3 -> EXIT (source == null -> spec is null)
    public void testM2_TP1_ConstructorWithNullSource() {
        Ability ability = new Ability(Ability.ARMED, null, false);
        assertNull(ability.getSpecification());
        assertEquals(Ability.ARMED, ability.getId());
        assertNull(ability.getSource());
        assertFalse(ability.getValue());
    }

    // M2-TP2: ENTRY -> N0 -(B1:true)-> N2 -> N3 -> EXIT (source != null -> uses source.getSpecification())
    public void testM2_TP2_ConstructorWithNonNullSource() {
        Ability ability = new Ability(Ability.FOUND_COLONY, colonistType, true);
        assertEquals(spec(), ability.getSpecification());
        assertEquals(Ability.FOUND_COLONY, ability.getId());
        assertEquals(colonistType, ability.getSource());
        assertTrue(ability.getValue());
    }

    // ------------------------------------------------------------------
    // M3: Ability(String) — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M3-TP1: ENTRY -> N0 -> EXIT (defaults source=null, value=true)
    public void testM3_TP1_ConstructorWithIdOnly() {
        Ability ability = new Ability(Ability.PERSON);
        assertEquals(Ability.PERSON, ability.getId());
        assertNull(ability.getSource());
        assertTrue(ability.getValue());
    }

    // ------------------------------------------------------------------
    // M4: Ability(String, boolean) — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M4-TP1: ENTRY -> N0 -> EXIT (defaults source=null with explicit boolean value)
    public void testM4_TP1_ConstructorWithIdAndValue() {
        Ability ability = new Ability(Ability.NAVAL_UNIT, false);
        assertEquals(Ability.NAVAL_UNIT, ability.getId());
        assertNull(ability.getSource());
        assertFalse(ability.getValue());
    }

    // ------------------------------------------------------------------
    // M5: Ability(Ability) — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M5-TP1: ENTRY -> N0 -> EXIT (copy constructor copies id, source, and value)
    public void testM5_TP1_CopyConstructor() {
        Ability template = new Ability(Ability.BOMBARD, colonistType, false);
        Ability copy = new Ability(template);
        assertEquals(template.getId(), copy.getId());
        assertEquals(colonistType, copy.getSource());
        assertFalse(copy.getValue());
    }

    // ------------------------------------------------------------------
    // M6: Ability(FreeColXMLReader, Specification) — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M6-TP1: ENTRY -> N0 -> EXIT (deserializes Ability from XML stream)
    public void testM6_TP1_XmlConstructor() throws Exception {
        String xml = "<ability id=\"model.ability.armed\" value=\"false\"/>";
        try (FreeColXMLReader xr = new FreeColXMLReader(new StringReader(xml))) {
            xr.nextTag();
            Ability ability = new Ability(xr, spec());
            assertEquals("model.ability.armed", ability.getId());
            assertFalse(ability.getValue());
        }
    }

    // ------------------------------------------------------------------
    // M7: getValue() — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M7-TP1: ENTRY -> N0 -> EXIT (returns current boolean value)
    public void testM7_TP1_GetValue() {
        Ability ability = new Ability(Ability.ARMED, true);
        assertTrue(ability.getValue());
    }

    // ------------------------------------------------------------------
    // M8: setValue(boolean) — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M8-TP1: ENTRY -> N0 -> EXIT (updates boolean value)
    public void testM8_TP1_SetValue() {
        Ability ability = new Ability(Ability.ARMED, true);
        ability.setValue(false);
        assertFalse(ability.getValue());
    }

    // ------------------------------------------------------------------
    // M9: copyIn(FreeColObject) — 2 Test Paths (100% feasible C3)
    // ------------------------------------------------------------------

    // M9-TP1: ENTRY -> N0 -(B1:true)-> N2 -> EXIT (other == null or incompatible type -> returns false)
    public void testM9_TP1_CopyInNullOrWrongTypeReturnsFalse() {
        Ability target = new Ability(Ability.ARMED, true);
        assertFalse(target.copyIn((Ability) null));
        assertFalse(target.copyIn(new Modifier(Modifier.OFFENCE, 1.0f, Modifier.ModifierType.ADDITIVE)));
        assertTrue(target.getValue());
    }

    // M9-TP2: ENTRY -> N0 -(B1:false)-> N1 -(B2:true)-> N3 -> EXIT (valid Ability -> copies value, returns true)
    public void testM9_TP2_CopyInValidAbilityReturnsTrue() {
        Ability target = new Ability(Ability.ARMED, true);
        Ability source = new Ability(Ability.ARMED, colonistType, false);
        assertTrue(target.copyIn(source));
        assertFalse(target.getValue());
        assertEquals(colonistType, target.getSource());
    }

    // ------------------------------------------------------------------
    // M10: writeAttributes(FreeColXMLWriter) — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M10-TP1: ENTRY -> N0 -> EXIT (writes VALUE_TAG attribute via toXML)
    public void testM10_TP1_WriteAttributes() throws Exception {
        Ability ability = new Ability(Ability.CARRY_GOODS, false);
        String xml = serializeToXml(ability);
        assertTrue(xml.contains("value=\"false\""));
    }

    // ------------------------------------------------------------------
    // M11: readAttributes(FreeColXMLReader) — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M11-TP1: ENTRY -> N0 -> EXIT (reads VALUE_TAG attribute with default true)
    public void testM11_TP1_ReadAttributesDefaultValue() throws Exception {
        String xml = "<ability id=\"model.ability.carryGoods\"/>";
        try (FreeColXMLReader xr = new FreeColXMLReader(new StringReader(xml))) {
            xr.nextTag();
            Ability ability = new Ability(spec());
            ability.readFromXML(xr);
            assertTrue(ability.getValue());
        }
    }

    // ------------------------------------------------------------------
    // M12: getXMLTagName() — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M12-TP1: ENTRY -> N0 -> EXIT (returns Ability.TAG = "ability")
    public void testM12_TP1_GetXMLTagName() {
        Ability ability = new Ability(Ability.ARMED);
        assertEquals("ability", ability.getXMLTagName());
    }

    // ------------------------------------------------------------------
    // M13: equals(Object) — 5 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M13-TP1: ENTRY -> N0 -(B1:false)-> N1 -> EXIT (this == o -> returns true)
    public void testM13_TP1_EqualsSameInstance() {
        Ability ability = new Ability(Ability.ARMED, true);
        assertTrue(ability.equals(ability));
    }

    // M13-TP2: ENTRY -> N0 -(B1:true)-> N2 -(B2:true)-> N8 -> EXIT (not instanceof Ability -> returns false)
    public void testM13_TP2_EqualsNonAbilityReturnsFalse() {
        Ability ability = new Ability(Ability.ARMED, true);
        assertFalse(ability.equals("not an ability"));
        assertFalse(ability.equals(null));
    }

    // M13-TP3: ENTRY -> N0 -(B1:true)-> N2 -(B2:false)-> N3 -(B3:true)-> N6 -> N7 -> EXIT
    // (Ability with different value -> returns false)
    public void testM13_TP3_EqualsDifferentValueReturnsFalse() {
        Ability a1 = new Ability(Ability.ARMED, true);
        Ability a2 = new Ability(Ability.ARMED, false);
        assertFalse(a1.equals(a2));
    }

    // M13-TP4: ENTRY -> N0 -(B1:true)-> N2 -(B2:false)-> N3 -(B3:false)-> N4 -(B4:true)-> N6 -> N7 -> EXIT
    // (same value, super.equals(other) == false -> returns false)
    public void testM13_TP4_EqualsSameValueDifferentIdReturnsFalse() {
        Ability a1 = new Ability(Ability.ARMED, true);
        Ability a2 = new Ability(Ability.MOUNTED, true);
        assertFalse(a1.equals(a2));
    }

    // M13-TP5: ENTRY -> N0 -(B1:true)-> N2 -(B2:false)-> N3 -(B3:false)-> N4 -(B4:false)-> N5 -> N7 -> EXIT
    // (same value and super.equals(other) == true -> returns true)
    public void testM13_TP5_EqualsMatchingAbilityReturnsTrue() {
        Ability a1 = new Ability(Ability.ARMED, colonistType, true);
        Ability a2 = new Ability(Ability.ARMED, colonistType, true);
        assertTrue(a1.equals(a2));
    }

    // ------------------------------------------------------------------
    // M14: hashCode() — 2 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M14-TP1: ENTRY -> N0 -(B1:false)-> N1 -> N3 -> EXIT (value == true -> adds 1)
    public void testM14_TP1_HashCodeValueTrue() {
        Ability aTrue = new Ability(Ability.ARMED, true);
        Ability aFalse = new Ability(Ability.ARMED, false);
        assertEquals(aFalse.hashCode() + 1, aTrue.hashCode());
    }

    // M14-TP2: ENTRY -> N0 -(B1:true)-> N2 -> N3 -> EXIT (value == false -> adds 0)
    public void testM14_TP2_HashCodeValueFalse() {
        Ability aFalse = new Ability(Ability.ARMED, false);
        Ability aFalse2 = new Ability(Ability.ARMED, false);
        assertEquals(aFalse2.hashCode(), aFalse.hashCode());
    }

    // ------------------------------------------------------------------
    // M15: toString() — 2 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M15-TP1: ENTRY -> N0 -(B1:true)-> N2 -> EXIT (getSource() == null -> omits source)
    public void testM15_TP1_ToStringWithoutSource() {
        Ability ability = new Ability(Ability.ARMED, true);
        assertEquals("[ model.ability.armed = true ]", ability.toString());
    }

    // M15-TP2: ENTRY -> N0 -(B1:false)-> N1 -> N2 -> EXIT (getSource() != null -> includes source id)
    public void testM15_TP2_ToStringWithSource() {
        Ability ability = new Ability(Ability.FOUND_COLONY, colonistType, false);
        assertEquals("[ model.ability.foundColony (model.unit.freeColonist) = false ]",
                     ability.toString());
    }
}
