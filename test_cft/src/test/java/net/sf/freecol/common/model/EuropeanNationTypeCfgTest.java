/**
 *  Copyright (C) 2002-2024   The FreeCol Team
 *
 *  This file is part of FreeCol.
 *
 *  FreeCol is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  FreeCol is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with FreeCol.  If not, see <http://www.gnu.org/licenses/>.
 */

package net.sf.freecol.common.model;

import java.io.StringReader;
import java.io.StringWriter;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.sf.freecol.common.io.FreeColXMLReader;
import net.sf.freecol.common.io.FreeColXMLWriter;
import net.sf.freecol.common.option.GameOptions;
import net.sf.freecol.util.test.FreeColTestCase;


/**
 * CFG-driven test suite for {@link EuropeanNationType} derived from
 * {@code test_cft/graph/net/sf/freecol/common/model/EuropeanNationType.json}.
 *
 * Covers all 20 methods (M1..M20) and achieves 100% feasible C3 (node + edge)
 * coverage across 30 Test Paths.
 *
 * Infeasible edges documented:
 * - M11 ({@code copyIn}): {@code N1 -> N2} ({@code B2:false}) — {@code super.copyIn(o)}
 *   delegates through {@code NationType}, {@code FreeColSpecObjectType},
 *   {@code FreeColSpecObject}, and {@code FreeColObject}; when {@code o} is a
 *   non-null {@code EuropeanNationType}, every superclass {@code copyInCast}
 *   succeeds and {@code super.copyIn(o)} always returns {@code true}.
 * - M13 ({@code writeChildren}): {@code N0 -> N10} ({@code B1:true}) —
 *   {@code startingUnitMap} is a {@code private final} field initialized inline
 *   to {@code new HashMap<>()} and can never be {@code null}.
 */
public class EuropeanNationTypeCfgTest extends FreeColTestCase {

    private EuropeanNationType nationType;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        // Pre-condition: Create a fresh EuropeanNationType bound to standard Specification
        nationType = new EuropeanNationType("test.european.nationType", spec());
    }

    // ------------------------------------------------------------------
    // Pre-condition helpers & stubs
    // ------------------------------------------------------------------

    private String invokeGetMapKey(boolean b) throws Exception {
        Method m = EuropeanNationType.class.getDeclaredMethod("getMapKey", boolean.class);
        m.setAccessible(true);
        return (String) m.invoke(nationType, b);
    }

    private void invokeAddStartingUnit(String id, AbstractUnit unit, boolean expert) throws Exception {
        Method m = EuropeanNationType.class.getDeclaredMethod(
                "addStartingUnit", String.class, AbstractUnit.class, boolean.class);
        m.setAccessible(true);
        m.invoke(nationType, id, unit, expert);
    }

    private void invokeWriteUnit(FreeColXMLWriter xw, String id, AbstractUnit au, boolean expert)
            throws Exception {
        Method m = EuropeanNationType.class.getDeclaredMethod(
                "writeUnit", FreeColXMLWriter.class, String.class, AbstractUnit.class, boolean.class);
        m.setAccessible(true);
        m.invoke(nationType, xw, id, au, expert);
    }

    // ------------------------------------------------------------------
    // M1: <init>(String, Specification) — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M1-TP1: ENTRY -> N0 -> EXIT (constructs EuropeanNationType with id and specification)
    public void testM1_TP1_constructor() {
        EuropeanNationType ent = new EuropeanNationType("test.id", spec());
        assertEquals("test.id", ent.getId());
        assertSame(spec(), ent.getSpecification());
        assertFalse(ent.isREF());
    }

    // ------------------------------------------------------------------
    // M2: getStartingUnitMap() — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M2-TP1: ENTRY -> N0 -> EXIT (returns startingUnitMap)
    public void testM2_TP1_getStartingUnitMap() {
        Map<String, Map<String, AbstractUnit>> map = nationType.getStartingUnitMap();
        assertNotNull(map);
        assertTrue(map.isEmpty());
    }

    // ------------------------------------------------------------------
    // M3: setStartingUnitMap(Map) — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M3-TP1: ENTRY -> N0 -> EXIT (clears and populates startingUnitMap)
    public void testM3_TP1_setStartingUnitMap() {
        Map<String, Map<String, AbstractUnit>> map = new HashMap<>();
        Map<String, AbstractUnit> def = new HashMap<>();
        def.put("u1", new AbstractUnit("model.unit.freeColonist", Specification.DEFAULT_ROLE_ID, 1));
        map.put("default", def);

        nationType.setStartingUnitMap(map);
        assertEquals(1, nationType.getStartingUnitMap().size());
        assertTrue(nationType.getStartingUnitMap().containsKey("default"));
    }

    // ------------------------------------------------------------------
    // M4: getMapKey(boolean) — 2 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M4-TP1: ENTRY -> N0 -(B1:true)-> N2 -> N3 -> EXIT (b == false -> "default")
    public void testM4_TP1_getMapKeyDefault() throws Exception {
        assertEquals("default", invokeGetMapKey(false));
    }

    // M4-TP2: ENTRY -> N0 -(B1:false)-> N1 -> N3 -> EXIT (b == true -> "expert")
    public void testM4_TP2_getMapKeyExpert() throws Exception {
        assertEquals("expert", invokeGetMapKey(true));
    }

    // ------------------------------------------------------------------
    // M5: getStartingUnits() — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M5-TP1: ENTRY -> N0 -> EXIT (reads EXPERT_STARTING_UNITS option and delegates to getStartingUnits(String))
    public void testM5_TP1_getStartingUnitsNoArg() throws Exception {
        invokeAddStartingUnit("pioneer",
                new AbstractUnit("model.unit.freeColonist", Specification.DEFAULT_ROLE_ID, 1), false);
        spec().setBoolean(GameOptions.EXPERT_STARTING_UNITS, false);
        List<AbstractUnit> units = nationType.getStartingUnits();
        assertEquals(1, units.size());
    }

    // ------------------------------------------------------------------
    // M6: getStartingUnits(String) — 4 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M6-TP1: ENTRY -> N0 -(B1:true)-> N2 -(B2:true)-> N4 -> EXIT (defaultMap == null, difficultyMap == null)
    public void testM6_TP1_getStartingUnitsBothNull() {
        List<AbstractUnit> units = nationType.getStartingUnits("expert");
        assertTrue(units.isEmpty());
    }

    // M6-TP2: ENTRY -> N0 -(B1:true)-> N2 -(B2:false)-> N3 -> N4 -> EXIT (defaultMap == null, difficultyMap != null)
    public void testM6_TP2_getStartingUnitsOnlyDifficultyMap() throws Exception {
        invokeAddStartingUnit("u1",
                new AbstractUnit("model.unit.hardyPioneer", Specification.DEFAULT_ROLE_ID, 1), true);
        List<AbstractUnit> units = nationType.getStartingUnits("expert");
        assertEquals(1, units.size());
    }

    // M6-TP3: ENTRY -> N0 -(B1:false)-> N1 -> N2 -(B2:true)-> N4 -> EXIT (defaultMap != null, difficultyMap == null)
    public void testM6_TP3_getStartingUnitsOnlyDefaultMap() throws Exception {
        invokeAddStartingUnit("u1",
                new AbstractUnit("model.unit.freeColonist", Specification.DEFAULT_ROLE_ID, 1), false);
        List<AbstractUnit> units = nationType.getStartingUnits("expert");
        assertEquals(1, units.size());
    }

    // M6-TP4: ENTRY -> N0 -(B1:false)-> N1 -> N2 -(B2:false)-> N3 -> N4 -> EXIT (defaultMap != null, difficultyMap != null)
    public void testM6_TP4_getStartingUnitsBothNonNull() throws Exception {
        invokeAddStartingUnit("u1",
                new AbstractUnit("model.unit.freeColonist", Specification.DEFAULT_ROLE_ID, 1), false);
        invokeAddStartingUnit("u2",
                new AbstractUnit("model.unit.hardyPioneer", Specification.DEFAULT_ROLE_ID, 1), true);
        List<AbstractUnit> units = nationType.getStartingUnits("expert");
        assertEquals(2, units.size());
    }

    // ------------------------------------------------------------------
    // M7: addStartingUnit(String, AbstractUnit, boolean) — 2 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M7-TP1: ENTRY -> N0 -(B1:false)-> N1 -> N2 -> EXIT (units == null, creates map and inserts unit)
    public void testM7_TP1_addStartingUnitNewBucket() throws Exception {
        invokeAddStartingUnit("u1",
                new AbstractUnit("model.unit.freeColonist", Specification.DEFAULT_ROLE_ID, 1), false);
        assertEquals(1, nationType.getStartingUnitMap().get("default").size());
    }

    // M7-TP2: ENTRY -> N0 -(B1:true)-> N2 -> EXIT (units != null, reuses existing map bucket)
    public void testM7_TP2_addStartingUnitExistingBucket() throws Exception {
        invokeAddStartingUnit("u1",
                new AbstractUnit("model.unit.freeColonist", Specification.DEFAULT_ROLE_ID, 1), false);
        invokeAddStartingUnit("u2",
                new AbstractUnit("model.unit.caravel", Specification.DEFAULT_ROLE_ID, 1), false);
        assertEquals(2, nationType.getStartingUnitMap().get("default").size());
    }

    // ------------------------------------------------------------------
    // M8: isREF() — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M8-TP1: ENTRY -> N0 -> EXIT (returns ref flag)
    public void testM8_TP1_isREF() {
        assertFalse(nationType.isREF());
        EuropeanNationType refType = (EuropeanNationType) spec().getNationType("model.nationType.ref");
        assertTrue(refType.isREF());
    }

    // ------------------------------------------------------------------
    // M9: isEuropean() — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M9-TP1: ENTRY -> N0 -> EXIT (always returns true)
    public void testM9_TP1_isEuropean() {
        assertTrue(nationType.isEuropean());
    }

    // ------------------------------------------------------------------
    // M10: isIndian() — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M10-TP1: ENTRY -> N0 -> EXIT (always returns false)
    public void testM10_TP1_isIndian() {
        assertFalse(nationType.isIndian());
    }

    // ------------------------------------------------------------------
    // M11: copyIn(FreeColObject) — 2 feasible Test Paths (100% feasible C3)
    // ------------------------------------------------------------------

    // M11-TP1: ENTRY -> N0 -(B1:true)-> N2 -> EXIT (o == null when other is null or wrong type -> false)
    public void testM11_TP1_copyInNullOrWrongType() {
        assertFalse(nationType.copyIn(null));
        assertFalse(nationType.copyIn(spec().getGoodsType("model.goods.grain")));
    }

    // M11-TP2: ENTRY -> N0 -(B1:false)-> N1 -(B2:true)-> N3 -> EXIT (o != null and super.copyIn(o) == true -> copies startingUnitMap)
    public void testM11_TP2_copyInValidEuropeanNationType() throws Exception {
        EuropeanNationType other = new EuropeanNationType(nationType.getId(), spec());
        Map<String, Map<String, AbstractUnit>> map = new HashMap<>();
        Map<String, AbstractUnit> def = new HashMap<>();
        def.put("u1", new AbstractUnit("model.unit.freeColonist", Specification.DEFAULT_ROLE_ID, 1));
        map.put("default", def);
        other.setStartingUnitMap(map);

        nationType.getFeatureContainer();
        assertTrue(nationType.copyIn(other));
        assertEquals(1, nationType.getStartingUnitMap().size());
    }

    // ------------------------------------------------------------------
    // M12: writeAttributes(FreeColXMLWriter) — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M12-TP1: ENTRY -> N0 -> EXIT (writes ref attribute)
    public void testM12_TP1_writeAttributes() throws Exception {
        StringWriter sw = new StringWriter();
        try (FreeColXMLWriter xw = new FreeColXMLWriter(sw, FreeColXMLWriter.WriteScope.toSave())) {
            nationType.toXML(xw);
        }
        assertTrue(sw.toString().contains("ref=\"false\""));
    }

    // ------------------------------------------------------------------
    // M13: writeChildren(FreeColXMLWriter) — 3 feasible Test Paths (100% feasible C3)
    // ------------------------------------------------------------------

    // M13-TP1: ENTRY -> N0 -(B1:false)-> N1 -(B2:true)-> N10 -> EXIT (startingUnitMap is empty)
    public void testM13_TP1_writeChildrenEmpty() throws Exception {
        StringWriter sw = new StringWriter();
        try (FreeColXMLWriter xw = new FreeColXMLWriter(sw, FreeColXMLWriter.WriteScope.toSave())) {
            xw.writeStartElement(EuropeanNationType.TAG);
            nationType.writeChildren(xw);
            xw.writeEndElement();
        }
        assertFalse(sw.toString().contains("<unit"));
    }

    // M13-TP2: ENTRY -> N0 -(B1:false)-> N1 -(B2:false)-> N2 -(B3:false)-> N3 -> N4 -(B4:false)-> N5 -> N4 -(B4:true)-> N6 -(B5:true)-> N10 -> EXIT (default map populated, expert map null)
    public void testM13_TP2_writeChildrenDefaultOnly() throws Exception {
        invokeAddStartingUnit("u1",
                new AbstractUnit("model.unit.freeColonist", Specification.DEFAULT_ROLE_ID, 1), false);
        StringWriter sw = new StringWriter();
        try (FreeColXMLWriter xw = new FreeColXMLWriter(sw, FreeColXMLWriter.WriteScope.toSave())) {
            xw.writeStartElement(EuropeanNationType.TAG);
            nationType.writeChildren(xw);
            xw.writeEndElement();
        }
        String xml = sw.toString();
        assertTrue(xml.contains("id=\"u1\""));
        assertFalse(xml.contains("expert-starting-units=\"true\""));
    }

    // M13-TP3: ENTRY -> N0 -(B1:false)-> N1 -(B2:false)-> N2 -(B3:true)-> N6 -(B5:false)-> N7 -> N8 -(B6:false)-> N9 -> N8 -(B6:true)-> N10 -> EXIT (default map null, expert map populated)
    public void testM13_TP3_writeChildrenExpertOnly() throws Exception {
        invokeAddStartingUnit("u2",
                new AbstractUnit("model.unit.hardyPioneer", Specification.DEFAULT_ROLE_ID, 1), true);
        StringWriter sw = new StringWriter();
        try (FreeColXMLWriter xw = new FreeColXMLWriter(sw, FreeColXMLWriter.WriteScope.toSave())) {
            xw.writeStartElement(EuropeanNationType.TAG);
            nationType.writeChildren(xw);
            xw.writeEndElement();
        }
        String xml = sw.toString();
        assertTrue(xml.contains("id=\"u2\""));
        assertTrue(xml.contains("expert-starting-units=\"true\""));
    }

    // ------------------------------------------------------------------
    // M14: writeUnit(FreeColXMLWriter, String, AbstractUnit, boolean) — 2 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M14-TP1: ENTRY -> N0 -(B1:true)-> N2 -> EXIT (expert == false, omits expert-starting-units attribute)
    public void testM14_TP1_writeUnitNonExpert() throws Exception {
        StringWriter sw = new StringWriter();
        AbstractUnit au = new AbstractUnit("model.unit.freeColonist", Specification.DEFAULT_ROLE_ID, 1);
        try (FreeColXMLWriter xw = new FreeColXMLWriter(sw, FreeColXMLWriter.WriteScope.toSave())) {
            invokeWriteUnit(xw, "u1", au, false);
        }
        String xml = sw.toString();
        assertTrue(xml.contains("id=\"u1\""));
        assertFalse(xml.contains("expert-starting-units"));
    }

    // M14-TP2: ENTRY -> N0 -(B1:false)-> N1 -> N2 -> EXIT (expert == true, writes expert-starting-units="true")
    public void testM14_TP2_writeUnitExpert() throws Exception {
        StringWriter sw = new StringWriter();
        AbstractUnit au = new AbstractUnit("model.unit.hardyPioneer", Specification.DEFAULT_ROLE_ID, 1);
        try (FreeColXMLWriter xw = new FreeColXMLWriter(sw, FreeColXMLWriter.WriteScope.toSave())) {
            invokeWriteUnit(xw, "u2", au, true);
        }
        String xml = sw.toString();
        assertTrue(xml.contains("id=\"u2\""));
        assertTrue(xml.contains("expert-starting-units=\"true\""));
    }

    // ------------------------------------------------------------------
    // M15: readAttributes(FreeColXMLReader) — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M15-TP1: ENTRY -> N0 -> EXIT (reads ref attribute from XML)
    public void testM15_TP1_readAttributes() throws Exception {
        String xml = "<european-nation-type id=\"test.ent\" ref=\"true\"/>";
        try (FreeColXMLReader xr = new FreeColXMLReader(new StringReader(xml))) {
            xr.nextTag();
            nationType.readAttributes(xr);
        }
        assertTrue(nationType.isREF());
    }

    // ------------------------------------------------------------------
    // M16: clearContainers(FreeColXMLReader) — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M16-TP1: ENTRY -> N0 -> EXIT (clears startingUnitMap)
    public void testM16_TP1_clearContainers() throws Exception {
        invokeAddStartingUnit("u1",
                new AbstractUnit("model.unit.freeColonist", Specification.DEFAULT_ROLE_ID, 1), false);
        String xml = "<european-nation-type id=\"test.ent\"/>";
        try (FreeColXMLReader xr = new FreeColXMLReader(new StringReader(xml))) {
            xr.nextTag();
            nationType.clearContainers(xr);
        }
        assertTrue(nationType.getStartingUnitMap().isEmpty());
    }

    // ------------------------------------------------------------------
    // M17: readChildren(FreeColXMLReader) — 2 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M17-TP1: ENTRY -> N0 -(B1:true)-> N2 -> EXIT (parent == this when no extends attribute)
    public void testM17_TP1_readChildrenNoExtends() throws Exception {
        String xml = "<european-nation-type id=\"test.ent\"/>";
        try (FreeColXMLReader xr = new FreeColXMLReader(new StringReader(xml))) {
            xr.nextTag();
            nationType.readChildren(xr);
        }
        assertTrue(nationType.getStartingUnitMap().isEmpty());
    }

    // M17-TP2: ENTRY -> N0 -(B1:false)-> N1 -> N2 -> EXIT (parent != this when extends attribute present, copies parent startingUnitMap)
    public void testM17_TP2_readChildrenWithExtends() throws Exception {
        String xml = "<european-nation-type id=\"test.ent\" extends=\"model.nationType.trade\"/>";
        try (FreeColXMLReader xr = new FreeColXMLReader(new StringReader(xml))) {
            xr.nextTag();
            nationType.readChildren(xr);
        }
        assertFalse(nationType.getStartingUnitMap().isEmpty());
    }

    // ------------------------------------------------------------------
    // M18: readChild(FreeColXMLReader) — 2 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M18-TP1: ENTRY -> N0 -(B1:true)-> N2 -> N3 -> EXIT (non-unit child tag delegates to super.readChild)
    public void testM18_TP1_readChildNonUnit() throws Exception {
        String xml = "<european-nation-type id=\"test.ent\">"
                + "<ability id=\"model.ability.navalUnit\" value=\"true\"/>"
                + "</european-nation-type>";
        try (FreeColXMLReader xr = new FreeColXMLReader(new StringReader(xml))) {
            xr.nextTag();
            xr.nextTag();
            nationType.readChild(xr);
        }
        assertTrue(nationType.hasAbility("model.ability.navalUnit"));
    }

    // M18-TP2: ENTRY -> N0 -(B1:false)-> N1 -> N3 -> EXIT (unit child tag parses unit and adds starting unit)
    public void testM18_TP2_readChildUnit() throws Exception {
        String xml = "<european-nation-type id=\"test.ent\">"
                + "<unit id=\"colonist\" type=\"model.unit.freeColonist\" expert-starting-units=\"false\"/>"
                + "</european-nation-type>";
        try (FreeColXMLReader xr = new FreeColXMLReader(new StringReader(xml))) {
            xr.nextTag();
            xr.nextTag();
            nationType.readChild(xr);
        }
        assertEquals(1, nationType.getStartingUnits("default").size());
    }

    // ------------------------------------------------------------------
    // M19: getXMLTagName() — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M19-TP1: ENTRY -> N0 -> EXIT (returns TAG "european-nation-type")
    public void testM19_TP1_getXMLTagName() {
        assertEquals(EuropeanNationType.TAG, nationType.getXMLTagName());
    }

    // ------------------------------------------------------------------
    // M20: lambda$readChildren$0(Map.Entry) — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M20-TP1: ENTRY -> N0 -> EXIT (executed via readChildren with extends="model.nationType.trade")
    public void testM20_TP1_lambdaReadChildrenCopiesParentEntries() throws Exception {
        String xml = "<european-nation-type id=\"test.ent.lambda\" extends=\"model.nationType.trade\"/>";
        try (FreeColXMLReader xr = new FreeColXMLReader(new StringReader(xml))) {
            xr.nextTag();
            nationType.readChildren(xr);
        }
        EuropeanNationType trade = (EuropeanNationType) spec().getNationType("model.nationType.trade");
        assertEquals(trade.getStartingUnitMap().size(), nationType.getStartingUnitMap().size());
    }
}
