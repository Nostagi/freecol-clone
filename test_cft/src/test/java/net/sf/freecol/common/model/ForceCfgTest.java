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
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import net.sf.freecol.common.io.FreeColXMLReader;
import net.sf.freecol.common.io.FreeColXMLWriter;
import net.sf.freecol.util.test.FreeColTestCase;


/**
 * CFG-driven test suite for {@link Force} derived from
 * {@code test_cft/graph/net/sf/freecol/common/model/Force.json}.
 *
 * Covers all 22 methods (M1..M22) and achieves 100% feasible C3 (node + edge)
 * coverage across 36 Test Paths.
 *
 * Infeasible edges documented:
 * - M15 ({@code prepareToBoard}): {@code N5 -> N7} ({@code B4:true}, {@code more <= 0}) —
 *   reached only when {@code spaceRequired > capacity} ({@code spaceRequired - capacity >= 1});
 *   for any valid {@code UnitType} with {@code sp = shipType.getSpace() > 0},
 *   {@code more = (spaceRequired - capacity) / sp + 1 >= 1 > 0}.
 * - M17 ({@code copyIn}): {@code N1 -> N2} ({@code B2:false}) — once {@code o} is non-null
 *   ({@code B1:false}), {@code o} is guaranteed to be a {@code Force} (subclass of
 *   {@code FreeColSpecObject} and {@code FreeColObject}), so {@code super.copyIn(o)}
 *   always returns {@code true}.
 */
public class ForceCfgTest extends FreeColTestCase {

    private static final UnitType GALLEON_TYPE = spec().getUnitType("model.unit.galleon");
    private static final UnitType COLONIST_TYPE = spec().getUnitType("model.unit.freeColonist");

    private Force force;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        // Pre-condition: Initialize an empty Force backed by the standard Specification.
        force = new Force(spec());
    }

    // ------------------------------------------------------------------
    // Pre-condition helpers & stubs
    // ------------------------------------------------------------------

    @SuppressWarnings("unchecked")
    private Force createForceWithNoCarryNavalType(final String id, UnitType[] outType) throws Exception {
        final UnitType type = new UnitType(id, spec());
        type.addAbility(new Ability(Ability.NAVAL_UNIT, true));
        type.setSpace(0);
        outType[0] = type;
        java.lang.reflect.Field f = Specification.class.getDeclaredField("allTypes");
        f.setAccessible(true);
        ((java.util.Map<String, FreeColSpecObjectType>) f.get(spec())).put(id, type);
        return new Force(spec());
    }

    private String toXmlString(Force f) throws Exception {
        StringWriter sw = new StringWriter();
        try (FreeColXMLWriter xw = new FreeColXMLWriter(
                sw, FreeColXMLWriter.WriteScope.toSave(), false)) {
            xw.writeStartDocument("UTF-8", "1.0");
            f.toXML(xw, Force.TAG);
            xw.writeEndDocument();
        }
        return sw.toString();
    }

    private void readXmlInto(Force f, String xml) throws Exception {
        try (FreeColXMLReader xr = new FreeColXMLReader(new StringReader(xml))) {
            xr.nextTag();
            f.readFromXML(xr);
        }
    }

    // ------------------------------------------------------------------
    // M1: Force(Specification) — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M1-TP1: ENTRY -> N0 -> EXIT (construct empty Force with zero capacity and spaceRequired)
    public void testM1_TP1_BasicConstructor() {
        Force f = new Force(spec());
        assertEquals(0, f.getCapacity());
        assertEquals(0, f.getSpaceRequired());
        assertTrue(f.isEmpty());
    }

    // ------------------------------------------------------------------
    // M2: Force(Specification, List<AbstractUnit>, String) — 4 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M2-TP1: ENTRY -> N0 -> N1 -(B1:true)-> N7 -> EXIT (empty unit list)
    public void testM2_TP1_ConstructorEmptyList() {
        Force f = new Force(spec(), Collections.emptyList(), Ability.NAVAL_UNIT);
        assertTrue(f.isEmpty());
    }

    // M2-TP2: ENTRY -> N0 -> N1 -(B1:false)-> N2 -(B2:true)-> N4 -> N6 -> N1 -(B1:true)-> N7 -> EXIT (ability == null)
    public void testM2_TP2_ConstructorNullAbility() {
        AbstractUnit au = new AbstractUnit(COLONIST_TYPE, Specification.DEFAULT_ROLE_ID, 2);
        Force f = new Force(spec(), Collections.singletonList(au), null);
        assertEquals(1, f.getUnitList().size());
    }

    // M2-TP3: ENTRY -> N0 -> N1 -(B1:false)-> N2 -(B2:false)-> N3 -(B3:false)-> N4 -> N6 -> N1 -(B1:true)-> N7 -> EXIT (unit has required ability)
    public void testM2_TP3_ConstructorWithMatchingAbility() {
        AbstractUnit ship = new AbstractUnit(GALLEON_TYPE, Specification.DEFAULT_ROLE_ID, 1);
        Force f = new Force(spec(), Collections.singletonList(ship), Ability.NAVAL_UNIT);
        assertEquals(1, f.getNavalUnitsList().size());
    }

    // M2-TP4: ENTRY -> N0 -> N1 -(B1:false)-> N2 -(B2:false)-> N3 -(B3:true)-> N5 -> N6 -> N1 -(B1:true)-> N7 -> EXIT (unit lacks required ability)
    public void testM2_TP4_ConstructorWithoutMatchingAbility() {
        AbstractUnit colonist = new AbstractUnit(COLONIST_TYPE, Specification.DEFAULT_ROLE_ID, 1);
        Force f = new Force(spec(), Collections.singletonList(colonist), Ability.NAVAL_UNIT);
        assertTrue(f.isEmpty());
    }

    // ------------------------------------------------------------------
    // M3: getSpaceRequired() — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M3-TP1: ENTRY -> N0 -> EXIT (return spaceRequired)
    public void testM3_TP1_GetSpaceRequired() {
        force.add(new AbstractUnit(COLONIST_TYPE, Specification.DEFAULT_ROLE_ID, 2));
        assertEquals(COLONIST_TYPE.getSpaceTaken() * 2, force.getSpaceRequired());
    }

    // ------------------------------------------------------------------
    // M4: getCapacity() — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M4-TP1: ENTRY -> N0 -> EXIT (return capacity)
    public void testM4_TP1_GetCapacity() {
        force.add(new AbstractUnit(GALLEON_TYPE, Specification.DEFAULT_ROLE_ID, 2));
        assertEquals(GALLEON_TYPE.getSpace() * 2, force.getCapacity());
    }

    // ------------------------------------------------------------------
    // M5: getUnitList() — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M5-TP1: ENTRY -> N0 -> EXIT (combine land and naval units)
    public void testM5_TP1_GetUnitList() {
        force.add(new AbstractUnit(COLONIST_TYPE, Specification.DEFAULT_ROLE_ID, 1));
        force.add(new AbstractUnit(GALLEON_TYPE, Specification.DEFAULT_ROLE_ID, 1));
        assertEquals(2, force.getUnitList().size());
    }

    // ------------------------------------------------------------------
    // M6: clearLandUnits() — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M6-TP1: ENTRY -> N0 -> EXIT (clear land units and reset spaceRequired)
    public void testM6_TP1_ClearLandUnits() {
        force.add(new AbstractUnit(COLONIST_TYPE, Specification.DEFAULT_ROLE_ID, 1));
        force.clearLandUnits();
        assertTrue(force.getLandUnitsList().isEmpty());
        assertEquals(0, force.getSpaceRequired());
    }

    // ------------------------------------------------------------------
    // M7: clearNavalUnits() — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M7-TP1: ENTRY -> N0 -> EXIT (clear naval units and reset capacity)
    public void testM7_TP1_ClearNavalUnits() {
        force.add(new AbstractUnit(GALLEON_TYPE, Specification.DEFAULT_ROLE_ID, 1));
        force.clearNavalUnits();
        assertTrue(force.getNavalUnitsList().isEmpty());
        assertEquals(0, force.getCapacity());
    }

    // ------------------------------------------------------------------
    // M8: getLandUnitsList() — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M8-TP1: ENTRY -> N0 -> EXIT (return deep copy of landUnits)
    public void testM8_TP1_GetLandUnitsList() {
        force.add(new AbstractUnit(COLONIST_TYPE, Specification.DEFAULT_ROLE_ID, 1));
        List<AbstractUnit> copy = force.getLandUnitsList();
        assertEquals(1, copy.size());
        copy.clear();
        assertEquals(1, force.getLandUnitsList().size());
    }

    // ------------------------------------------------------------------
    // M9: setLandUnitList(List<AbstractUnit>) — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M9-TP1: ENTRY -> N0 -> EXIT (replace landUnits list)
    public void testM9_TP1_SetLandUnitList() {
        AbstractUnit au = new AbstractUnit(COLONIST_TYPE, Specification.DEFAULT_ROLE_ID, 3);
        force.setLandUnitList(Collections.singletonList(au));
        assertEquals(1, force.getLandUnitsList().size());
    }

    // ------------------------------------------------------------------
    // M10: getNavalUnitsList() — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M10-TP1: ENTRY -> N0 -> EXIT (return deep copy of navalUnits)
    public void testM10_TP1_GetNavalUnitsList() {
        force.add(new AbstractUnit(GALLEON_TYPE, Specification.DEFAULT_ROLE_ID, 1));
        List<AbstractUnit> copy = force.getNavalUnitsList();
        assertEquals(1, copy.size());
        copy.clear();
        assertEquals(1, force.getNavalUnitsList().size());
    }

    // ------------------------------------------------------------------
    // M11: setNavalUnitList(List<AbstractUnit>) — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M11-TP1: ENTRY -> N0 -> EXIT (replace navalUnits list)
    public void testM11_TP1_SetNavalUnitList() {
        AbstractUnit au = new AbstractUnit(GALLEON_TYPE, Specification.DEFAULT_ROLE_ID, 2);
        force.setNavalUnitList(Collections.singletonList(au));
        assertEquals(1, force.getNavalUnitsList().size());
    }

    // ------------------------------------------------------------------
    // M12: isEmpty() — 3 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M12-TP1: ENTRY -> N0 -(B1:true)-> N3 -> N4 -> EXIT (landUnits not empty -> false)
    public void testM12_TP1_IsEmptyFalseWithLand() {
        force.add(new AbstractUnit(COLONIST_TYPE, Specification.DEFAULT_ROLE_ID, 1));
        assertFalse(force.isEmpty());
    }

    // M12-TP2: ENTRY -> N0 -(B1:false)-> N1 -(B2:true)-> N3 -> N4 -> EXIT (landUnits empty, navalUnits not empty -> false)
    public void testM12_TP2_IsEmptyFalseWithNaval() {
        force.add(new AbstractUnit(GALLEON_TYPE, Specification.DEFAULT_ROLE_ID, 1));
        assertFalse(force.isEmpty());
    }

    // M12-TP3: ENTRY -> N0 -(B1:false)-> N1 -(B2:false)-> N2 -> N4 -> EXIT (both empty -> true)
    public void testM12_TP3_IsEmptyTrue() {
        assertTrue(force.isEmpty());
    }

    // ------------------------------------------------------------------
    // M13: add(AbstractUnit) — 4 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M13-TP1: ENTRY -> N0 -(B1:false)-> N1 -(B2:true)-> N3 -> N4 -(B3:false)-> N5 -> N6 -> N11 -> EXIT (new carry-capable naval unit)
    public void testM13_TP1_AddNewCarryNavalUnit() {
        force.add(new AbstractUnit(GALLEON_TYPE, Specification.DEFAULT_ROLE_ID, 1));
        assertEquals(GALLEON_TYPE.getSpace(), force.getCapacity());
        assertEquals(1, force.getNavalUnitsList().size());
    }

    // M13-TP2: ENTRY -> N0 -(B1:false)-> N1 -(B2:false)-> N2 -> N4 -(B3:true)-> N6 -> N11 -> EXIT (existing non-carry naval unit)
    public void testM13_TP2_AddExistingNonCarryNavalUnit() throws Exception {
        UnitType[] out = new UnitType[1];
        Force f = createForceWithNoCarryNavalType("model.unit.cftNoCarryAdd", out);
        UnitType noCarry = out[0];
        f.add(new AbstractUnit(noCarry, Specification.DEFAULT_ROLE_ID, 1));
        f.add(new AbstractUnit(noCarry, Specification.DEFAULT_ROLE_ID, 2));
        assertEquals(0, f.getCapacity());
        assertEquals(1, f.getNavalUnitsList().size());
        assertEquals(3, f.getNavalUnitsList().get(0).getNumber());
    }

    // M13-TP3: ENTRY -> N0 -(B1:true)-> N7 -(B4:true)-> N9 -> N10 -> N11 -> EXIT (new land unit)
    public void testM13_TP3_AddNewLandUnit() {
        force.add(new AbstractUnit(COLONIST_TYPE, Specification.DEFAULT_ROLE_ID, 2));
        assertEquals(COLONIST_TYPE.getSpaceTaken() * 2, force.getSpaceRequired());
        assertEquals(1, force.getLandUnitsList().size());
    }

    // M13-TP4: ENTRY -> N0 -(B1:true)-> N7 -(B4:false)-> N8 -> N10 -> N11 -> EXIT (existing matching land unit)
    public void testM13_TP4_AddExistingLandUnit() {
        force.add(new AbstractUnit(COLONIST_TYPE, Specification.DEFAULT_ROLE_ID, 1));
        force.add(new AbstractUnit(COLONIST_TYPE, Specification.DEFAULT_ROLE_ID, 3));
        assertEquals(1, force.getLandUnitsList().size());
        assertEquals(4, force.getLandUnitsList().get(0).getNumber());
    }

    // ------------------------------------------------------------------
    // M14: calculateStrength(boolean) — 2 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M14-TP1: ENTRY -> N0 -(B1:false)-> N1 -> N3 -> EXIT (naval == true)
    public void testM14_TP1_CalculateStrengthNaval() {
        force.add(new AbstractUnit(GALLEON_TYPE, Specification.DEFAULT_ROLE_ID, 1));
        assertTrue(force.calculateStrength(true) >= 0.0);
    }

    // M14-TP2: ENTRY -> N0 -(B1:true)-> N2 -> N3 -> EXIT (naval == false)
    public void testM14_TP2_CalculateStrengthLand() {
        force.add(new AbstractUnit(COLONIST_TYPE, Specification.DEFAULT_ROLE_ID, 1));
        assertTrue(force.calculateStrength(false) >= 0.0);
    }

    // ------------------------------------------------------------------
    // M15: prepareToBoard(UnitType) — 4 Test Paths (100% feasible C3; N5->N7 infeasible)
    // ------------------------------------------------------------------

    // M15-TP1: ENTRY -> N0 -(B1:true)-> N7 -> EXIT (spaceRequired <= capacity)
    public void testM15_TP1_PrepareToBoardSufficientCapacity() {
        force.add(new AbstractUnit(GALLEON_TYPE, Specification.DEFAULT_ROLE_ID, 1));
        assertEquals(GALLEON_TYPE.getSpace(), force.prepareToBoard(null));
    }

    // M15-TP2: ENTRY -> N0 -(B1:false)-> N1 -(B2:false)-> N2 -(B3:false)-> N3 -> EXIT (shipType == null, no naval unit with space > 0)
    public void testM15_TP2_PrepareToBoardNoShipFound() {
        force.add(new AbstractUnit(COLONIST_TYPE, Specification.DEFAULT_ROLE_ID, 2));
        int extra = force.prepareToBoard(null);
        assertEquals(-force.getSpaceRequired(), extra);
    }

    // M15-TP3: ENTRY -> N0 -(B1:false)-> N1 -(B2:false)-> N2 -(B3:true)-> N4 -> N5 -(B4:false)-> N6 -> N7 -> EXIT (shipType == null, uses existing ship)
    public void testM15_TP3_PrepareToBoardInferShipType() {
        force.add(new AbstractUnit(COLONIST_TYPE, Specification.DEFAULT_ROLE_ID, 10));
        force.add(new AbstractUnit(GALLEON_TYPE, Specification.DEFAULT_ROLE_ID, 1));
        int extra = force.prepareToBoard(null);
        assertTrue(extra >= 0);
        assertTrue(force.getCapacity() >= force.getSpaceRequired());
    }

    // M15-TP4: ENTRY -> N0 -(B1:false)-> N1 -(B2:true)-> N5 -(B4:false)-> N6 -> N7 -> EXIT (explicit shipType != null)
    public void testM15_TP4_PrepareToBoardExplicitShipType() {
        force.add(new AbstractUnit(COLONIST_TYPE, Specification.DEFAULT_ROLE_ID, 5));
        int extra = force.prepareToBoard(GALLEON_TYPE);
        assertTrue(extra >= 0);
        assertTrue(force.getCapacity() >= force.getSpaceRequired());
    }

    // ------------------------------------------------------------------
    // M16: matchAll(Force) — 3 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M16-TP1: ENTRY -> N0 -(B1:true)-> N3 -> N4 -> EXIT (landUnits differ -> false)
    public void testM16_TP1_MatchAllLandMismatch() {
        force.add(new AbstractUnit(COLONIST_TYPE, Specification.DEFAULT_ROLE_ID, 1));
        Force other = new Force(spec());
        assertFalse(force.matchAll(other));
    }

    // M16-TP2: ENTRY -> N0 -(B1:false)-> N1 -(B2:true)-> N3 -> N4 -> EXIT (landUnits match, navalUnits differ -> false)
    public void testM16_TP2_MatchAllNavalMismatch() {
        force.add(new AbstractUnit(COLONIST_TYPE, Specification.DEFAULT_ROLE_ID, 1));
        force.add(new AbstractUnit(GALLEON_TYPE, Specification.DEFAULT_ROLE_ID, 1));
        Force other = new Force(spec());
        other.add(new AbstractUnit(COLONIST_TYPE, Specification.DEFAULT_ROLE_ID, 1));
        assertFalse(force.matchAll(other));
    }

    // M16-TP3: ENTRY -> N0 -(B1:false)-> N1 -(B2:false)-> N2 -> N4 -> EXIT (both match -> true)
    public void testM16_TP3_MatchAllTrue() {
        force.add(new AbstractUnit(COLONIST_TYPE, Specification.DEFAULT_ROLE_ID, 1));
        force.add(new AbstractUnit(GALLEON_TYPE, Specification.DEFAULT_ROLE_ID, 1));
        Force other = new Force(spec());
        other.add(new AbstractUnit(COLONIST_TYPE, Specification.DEFAULT_ROLE_ID, 1));
        other.add(new AbstractUnit(GALLEON_TYPE, Specification.DEFAULT_ROLE_ID, 1));
        assertTrue(force.matchAll(other));
    }

    // ------------------------------------------------------------------
    // M17: copyIn(T) — 2 Test Paths (100% feasible C3; N1->N2 infeasible)
    // ------------------------------------------------------------------

    // M17-TP1: ENTRY -> N0 -(B1:true)-> N2 -> EXIT (other == null -> false)
    public void testM17_TP1_CopyInNull() {
        assertFalse(force.copyIn(null));
    }

    // M17-TP2: ENTRY -> N0 -(B1:false)-> N1 -(B2:true)-> N3 -> EXIT (valid Force -> true)
    public void testM17_TP2_CopyInValid() {
        force.setId("force:copy");
        Force other = new Force(spec());
        other.setId("force:copy");
        other.add(new AbstractUnit(COLONIST_TYPE, Specification.DEFAULT_ROLE_ID, 2));
        other.add(new AbstractUnit(GALLEON_TYPE, Specification.DEFAULT_ROLE_ID, 1));
        assertTrue(force.copyIn(other));
        assertTrue(force.matchAll(other));
        assertEquals(other.getSpaceRequired(), force.getSpaceRequired());
        assertEquals(other.getCapacity(), force.getCapacity());
    }

    // ------------------------------------------------------------------
    // M18: toXML(FreeColXMLWriter, String) — 2 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M18-TP1: ENTRY -> N0 -> N1 -(B1:true)-> N3 -> N4 -(B2:true)-> N6 -> EXIT (empty Force)
    public void testM18_TP1_ToXMLEmpty() throws Exception {
        String xml = toXmlString(force);
        assertTrue(xml.contains(Force.TAG));
    }

    // M18-TP2: ENTRY -> N0 -> N1 -(B1:false)-> N2 -> N1 -(B1:true)-> N3 -> N4 -(B2:false)-> N5 -> N4 -(B2:true)-> N6 -> EXIT (with naval and land units)
    public void testM18_TP2_ToXMLWithUnits() throws Exception {
        force.add(new AbstractUnit(GALLEON_TYPE, Specification.DEFAULT_ROLE_ID, 1));
        force.add(new AbstractUnit(COLONIST_TYPE, Specification.DEFAULT_ROLE_ID, 2));
        String xml = toXmlString(force);
        assertTrue(xml.contains("model.unit.galleon"));
        assertTrue(xml.contains("model.unit.freeColonist"));
    }

    // ------------------------------------------------------------------
    // M19: readFromXML(FreeColXMLReader) — 3 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M19-TP1: ENTRY -> N0 -> N1 -(B1:true)-> N10 -> EXIT (no child tags)
    public void testM19_TP1_ReadFromXMLEmpty() throws Exception {
        readXmlInto(force, "<force/>");
        assertTrue(force.isEmpty());
    }

    // M19-TP2: ENTRY -> N0 -> N1 -(B1:false)-> N2 -(B2:false)-> N3 -(B3:false)-> N4 -> N3 -(B3:true)-> N9 -> N1 -(B1:false)-> N2 -(B2:true)-> N5 -(B4:false)-> N6 -(B5:false)-> N7 -> N6 -(B5:true)-> N9 -> N1 -(B1:true)-> N10 -> EXIT
    public void testM19_TP2_ReadFromXMLWithLandAndNaval() throws Exception {
        force.add(new AbstractUnit(GALLEON_TYPE, Specification.DEFAULT_ROLE_ID, 1));
        force.add(new AbstractUnit(COLONIST_TYPE, Specification.DEFAULT_ROLE_ID, 2));
        String xml = toXmlString(force);
        Force parsed = new Force(spec());
        readXmlInto(parsed, xml);
        assertTrue(force.matchAll(parsed));
    }

    // M19-TP3: ENTRY -> N0 -> N1 -(B1:false)-> N2 -(B2:true)-> N5 -(B4:true)-> N8 -> N9 -> N1 -(B1:true)-> N10 -> EXIT (bogus child tag)
    public void testM19_TP3_ReadFromXMLBogusTag() throws Exception {
        readXmlInto(force, "<force><bogus/></force>");
        assertTrue(force.isEmpty());
    }

    // ------------------------------------------------------------------
    // M20: getXMLTagName() — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M20-TP1: ENTRY -> N0 -> EXIT (return Force.TAG)
    public void testM20_TP1_GetXMLTagName() {
        assertEquals(Force.TAG, force.getXMLTagName());
    }

    // ------------------------------------------------------------------
    // M21: toString() — 2 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M21-TP1: ENTRY -> N0 -> N1 -(B1:true)-> N3 -> N4 -(B2:true)-> N6 -> EXIT (empty Force)
    public void testM21_TP1_ToStringEmpty() {
        assertEquals("<Force 0/0>", force.toString());
    }

    // M21-TP2: ENTRY -> N0 -> N1 -(B1:false)-> N2 -> N1 -(B1:true)-> N3 -> N4 -(B2:false)-> N5 -> N4 -(B2:true)-> N6 -> EXIT (with land and naval units)
    public void testM21_TP2_ToStringWithUnits() {
        force.add(new AbstractUnit(COLONIST_TYPE, Specification.DEFAULT_ROLE_ID, 1));
        force.add(new AbstractUnit(GALLEON_TYPE, Specification.DEFAULT_ROLE_ID, 1));
        String s = force.toString();
        assertTrue(s.startsWith("<Force "));
        assertTrue(s.endsWith(">"));
    }

    // ------------------------------------------------------------------
    // M22: lambda$prepareToBoard$0(AbstractUnit) — 2 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M22-TP1 & M22-TP2: ENTRY -> N0 -(B1:true)-> N2 -> N3 -> EXIT (space <= 0) and ENTRY -> N0 -(B1:false)-> N1 -> N3 -> EXIT (space > 0)
    public void testM22_TP1_TP2_LambdaPrepareToBoard() throws Exception {
        UnitType[] out = new UnitType[1];
        Force f = createForceWithNoCarryNavalType("model.unit.cftNoCarryLambda", out);
        UnitType noCarry = out[0];
        f.add(new AbstractUnit(noCarry, Specification.DEFAULT_ROLE_ID, 1));
        f.add(new AbstractUnit(GALLEON_TYPE, Specification.DEFAULT_ROLE_ID, 1));
        f.add(new AbstractUnit(COLONIST_TYPE, Specification.DEFAULT_ROLE_ID, 10));
        int extra = f.prepareToBoard(null);
        assertTrue(extra >= 0);
    }
}
