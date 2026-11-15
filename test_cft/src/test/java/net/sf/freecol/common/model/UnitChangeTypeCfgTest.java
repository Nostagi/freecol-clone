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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.sf.freecol.common.io.FreeColXMLReader;
import net.sf.freecol.common.io.FreeColXMLWriter;
import net.sf.freecol.util.test.FreeColTestCase;


/**
 * CFG-driven test suite for {@link UnitChangeType} derived from
 * {@code test_cft/graph/net/sf/freecol/common/model/UnitChangeType.json}.
 *
 * Covers all 18 methods (M1..M18) and achieves 100% feasible C3 (node + edge)
 * coverage across 25 Test Paths.
 *
 * Infeasible edges documented:
 * - M9 ({@code copyIn}): {@code N1 -> N2} ({@code B2:false}) — {@code o} is a
 *   non-null {@code UnitChangeType} already validated by {@code copyInCast}, so
 *   {@code FreeColSpecObjectType.copyIn(o)} always returns {@code true}.
 */
public class UnitChangeTypeCfgTest extends FreeColTestCase {

    private UnitType colonistType;
    private UnitType soldierType;
    private UnitType pioneerType;
    private UnitChangeType changeType;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        // Pre-condition: standard specification unit types and a UnitChangeType instance
        colonistType = spec().getUnitType("model.unit.freeColonist");
        soldierType = spec().getUnitType("model.unit.veteranSoldier");
        pioneerType = spec().getUnitType("model.unit.hardyPioneer");
        changeType = new UnitChangeType("model.unitChange.cftTest", spec());
    }

    // ------------------------------------------------------------------
    // Pre-condition helpers & stubs
    // ------------------------------------------------------------------

    private UnitTypeChange makeChange(String id, UnitType from, UnitType to) {
        UnitTypeChange uc = new UnitTypeChange(id, spec());
        uc.from = from;
        uc.to = to;
        uc.probability = 100;
        uc.turns = 0;
        return uc;
    }

    private String toXml(UnitChangeType target) throws Exception {
        StringWriter sw = new StringWriter();
        try (FreeColXMLWriter xw = new FreeColXMLWriter(sw,
                FreeColXMLWriter.WriteScope.toSave(), false)) {
            target.toXML(xw);
        }
        return sw.toString();
    }

    private FreeColXMLReader openReader(String xml) throws Exception {
        FreeColXMLReader xr = new FreeColXMLReader(new StringReader(xml));
        xr.nextTag();
        return xr;
    }

    // ------------------------------------------------------------------
    // M1: UnitChangeType(String, Specification) — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M1-TP1: ENTRY -> N0 -> EXIT (constructor initializes id, spec, defaults)
    public void testM1_TP1_constructor() {
        UnitChangeType uct = new UnitChangeType("model.unitChange.cftInit", spec());
        assertEquals("model.unitChange.cftInit", uct.getId());
        assertSame(spec(), uct.getSpecification());
        assertFalse(uct.getOwnerChange());
        assertTrue(uct.getChanges().isEmpty());
    }

    // ------------------------------------------------------------------
    // M2: getOwnerChange() — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M2-TP1: ENTRY -> N0 -> EXIT (return ownerChange)
    public void testM2_TP1_getOwnerChange() {
        assertFalse(changeType.getOwnerChange());
    }

    // ------------------------------------------------------------------
    // M3: getChanges() — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M3-TP1: ENTRY -> N0 -> EXIT (return changes map)
    public void testM3_TP1_getChanges() {
        assertNotNull(changeType.getChanges());
        assertTrue(changeType.getChanges().isEmpty());
    }

    // ------------------------------------------------------------------
    // M4: setChanges(Map) — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M4-TP1: ENTRY -> N0 -> EXIT (clear and putAll into changes map)
    public void testM4_TP1_setChanges() {
        UnitTypeChange uc = makeChange("cft.uc1", colonistType, soldierType);
        Map<UnitType, List<UnitTypeChange>> map = new HashMap<>();
        map.put(colonistType, Collections.singletonList(uc));

        changeType.setChanges(map);
        assertEquals(1, changeType.getChanges().size());
        assertSame(uc, changeType.getUnitChanges(colonistType).get(0));
    }

    // ------------------------------------------------------------------
    // M5: addUnitTypeChange(UnitTypeChange) — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M5-TP1: ENTRY -> N0 -> EXIT (append change to map list)
    public void testM5_TP1_addUnitTypeChange() {
        UnitTypeChange uc = makeChange("cft.ucAdd", colonistType, soldierType);
        changeType.addUnitTypeChange(uc);
        assertEquals(1, changeType.getUnitChanges(colonistType).size());
        assertSame(uc, changeType.getUnitChanges(colonistType).get(0));
    }

    // ------------------------------------------------------------------
    // M6: getUnitChanges(UnitType) — 2 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M6-TP1: ENTRY -> N0 -(B1:true)-> N2 -> N3 -> EXIT (ret != null -> returns list)
    public void testM6_TP1_getUnitChangesWhenPresent() {
        UnitTypeChange uc = makeChange("cft.ucPresent", colonistType, soldierType);
        changeType.addUnitTypeChange(uc);
        List<UnitTypeChange> list = changeType.getUnitChanges(colonistType);
        assertEquals(1, list.size());
        assertSame(uc, list.get(0));
    }

    // M6-TP2: ENTRY -> N0 -(B1:false)-> N1 -> N3 -> EXIT (ret == null -> returns emptyList)
    public void testM6_TP2_getUnitChangesWhenAbsent() {
        List<UnitTypeChange> list = changeType.getUnitChanges(pioneerType);
        assertNotNull(list);
        assertTrue(list.isEmpty());
    }

    // ------------------------------------------------------------------
    // M7: getUnitChange(UnitType, UnitType) — 2 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M7-TP1: ENTRY -> N0 -(B1:true)-> N2 -> N3 -> EXIT (toType != null -> matches uc.to == toType)
    public void testM7_TP1_getUnitChangeWithSpecificToType() {
        UnitTypeChange uc = makeChange("cft.ucSpec", colonistType, soldierType);
        changeType.addUnitTypeChange(uc);
        assertSame(uc, changeType.getUnitChange(colonistType, soldierType));
    }

    // M7-TP2: ENTRY -> N0 -(B1:false)-> N1 -> N3 -> EXIT (toType == null -> alwaysTrue predicate)
    public void testM7_TP2_getUnitChangeWithNullToType() {
        UnitTypeChange uc = makeChange("cft.ucAny", colonistType, soldierType);
        changeType.addUnitTypeChange(uc);
        assertSame(uc, changeType.getUnitChange(colonistType, null));
    }

    // ------------------------------------------------------------------
    // M8: deleteUnitChanges(UnitType) — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M8-TP1: ENTRY -> N0 -> EXIT (remove changes for from unitType)
    public void testM8_TP1_deleteUnitChanges() {
        changeType.addUnitTypeChange(makeChange("cft.ucDel", colonistType, soldierType));
        assertFalse(changeType.getUnitChanges(colonistType).isEmpty());
        changeType.deleteUnitChanges(colonistType);
        assertTrue(changeType.getUnitChanges(colonistType).isEmpty());
    }

    // ------------------------------------------------------------------
    // M9: copyIn(FreeColObject) — 2 Test Paths (100% feasible C3)
    // ------------------------------------------------------------------

    // M9-TP1: ENTRY -> N0 -(B1:true)-> N2 -> EXIT (o == null -> returns false)
    public void testM9_TP1_copyInNullOrIncompatibleReturnsFalse() {
        assertFalse(changeType.copyIn(null));
        assertFalse(changeType.copyIn(colonistType));
    }

    // M9-TP2: ENTRY -> N0 -(B1:false)-> N1 -(B2:true)-> N3 -> EXIT (o != null && super.copyIn(o))
    public void testM9_TP2_copyInValidUnitChangeTypeCopiesState() throws Exception {
        UnitChangeType other = new UnitChangeType("model.unitChange.cftTest", spec());
        other.addUnitTypeChange(makeChange("cft.ucCopy", colonistType, soldierType));
        try (FreeColXMLReader xr = openReader(
                "<unit-change-type id=\"model.unitChange.cftTest\" owner-change=\"true\"/>")) {
            other.readFromXML(xr);
        }
        other.addUnitTypeChange(makeChange("cft.ucCopy", colonistType, soldierType));

        changeType.getFeatureContainer();
        assertTrue(changeType.copyIn(other));
        assertTrue(changeType.getOwnerChange());
        assertEquals(1, changeType.getUnitChanges(colonistType).size());
    }

    // ------------------------------------------------------------------
    // M10: writeAttributes(FreeColXMLWriter) — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M10-TP1: ENTRY -> N0 -> EXIT (serialize owner-change attribute)
    public void testM10_TP1_writeAttributes() throws Exception {
        String xml = toXml(changeType);
        assertTrue(xml.contains("owner-change=\"false\""));
    }

    // ------------------------------------------------------------------
    // M11: writeChildren(FreeColXMLWriter) — 2 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M11-TP1: ENTRY -> N0 -> N1 -(B1:true)-> N6 -> EXIT (changes empty -> no child elements)
    public void testM11_TP1_writeChildrenEmpty() throws Exception {
        String xml = toXml(changeType);
        assertFalse(xml.contains("<unit-type-change"));
    }

    // M11-TP2: ENTRY -> N0 -> N1 -(B1:false)-> N2 -> N3 -(B2:false)-> N4 -> N3 -(B2:true)-> N5 -> N1 -(B1:true)-> N6 -> EXIT
    public void testM11_TP2_writeChildrenNonEmpty() throws Exception {
        changeType.addUnitTypeChange(makeChange("cft.ucWrite", colonistType, soldierType));
        String xml = toXml(changeType);
        assertTrue(xml.contains("<unit-type-change"));
    }

    // ------------------------------------------------------------------
    // M12: readAttributes(FreeColXMLReader) — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M12-TP1: ENTRY -> N0 -> EXIT (read owner-change attribute)
    public void testM12_TP1_readAttributes() throws Exception {
        String xml = "<unit-change-type id=\"model.unitChange.cftRead\" owner-change=\"true\"/>";
        try (FreeColXMLReader xr = openReader(xml)) {
            changeType.readAttributes(xr);
        }
        assertTrue(changeType.getOwnerChange());
    }

    // ------------------------------------------------------------------
    // M13: clearContainers(FreeColXMLReader) — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M13-TP1: ENTRY -> N0 -> EXIT (clear changes map before reading)
    public void testM13_TP1_clearContainers() throws Exception {
        changeType.addUnitTypeChange(makeChange("cft.ucClear", colonistType, soldierType));
        try (FreeColXMLReader xr = openReader("<unit-change-type id=\"model.unitChange.cftTest\"/>")) {
            changeType.clearContainers(xr);
        }
        assertTrue(changeType.getChanges().isEmpty());
    }

    // ------------------------------------------------------------------
    // M14: readChildren(FreeColXMLReader) — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M14-TP1: ENTRY -> N0 -> EXIT (delegate readChildren to super)
    public void testM14_TP1_readChildren() throws Exception {
        String xml = "<unit-change-type id=\"model.unitChange.cftTest\"/>";
        try (FreeColXMLReader xr = openReader(xml)) {
            changeType.readChildren(xr);
        }
        assertTrue(changeType.getChanges().isEmpty());
    }

    // ------------------------------------------------------------------
    // M15: readChild(FreeColXMLReader) — 2 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M15-TP1: ENTRY -> N0 -(B1:true)-> N2 -> N3 -> EXIT (non-unit-type-change tag -> super.readChild)
    public void testM15_TP1_readChildOtherTag() throws Exception {
        String xml = "<unit-change-type id=\"model.unitChange.cftTest\">"
            + "<ability id=\"model.ability.bornInColony\" value=\"true\"/>"
            + "</unit-change-type>";
        try (FreeColXMLReader xr = openReader(xml)) {
            changeType.readFromXML(xr);
        }
        assertTrue(changeType.hasAbility("model.ability.bornInColony"));
    }

    // M15-TP2: ENTRY -> N0 -(B1:false)-> N1 -> N3 -> EXIT (unit-type-change tag -> addUnitTypeChange)
    public void testM15_TP2_readChildUnitTypeChange() throws Exception {
        String xml = "<unit-change-type id=\"model.unitChange.cftTest\">"
            + "<unit-type-change from=\"model.unit.freeColonist\" "
            + "to=\"model.unit.veteranSoldier\" probability=\"100\" turns=\"0\"/>"
            + "</unit-change-type>";
        try (FreeColXMLReader xr = openReader(xml)) {
            changeType.readFromXML(xr);
        }
        assertEquals(1, changeType.getUnitChanges(colonistType).size());
    }

    // ------------------------------------------------------------------
    // M16: getXMLTagName() — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M16-TP1: ENTRY -> N0 -> EXIT (return TAG)
    public void testM16_TP1_getXMLTagName() {
        assertEquals(UnitChangeType.TAG, changeType.getXMLTagName());
    }

    // ------------------------------------------------------------------
    // M17: toString() — 2 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M17-TP1: ENTRY -> N0 -> N1 -(B1:true)-> N6 -> EXIT (changes empty)
    public void testM17_TP1_toStringEmpty() {
        String s = changeType.toString();
        assertTrue(s.contains("model.unitChange.cftTest"));
        assertTrue(s.contains("ownerChange=false"));
    }

    // M17-TP2: ENTRY -> N0 -> N1 -(B1:false)-> N2 -> N3 -(B2:false)-> N4 -> N3 -(B2:true)-> N5 -> N1 -(B1:true)-> N6 -> EXIT
    public void testM17_TP2_toStringWithChanges() {
        changeType.addUnitTypeChange(makeChange("cft.ucStr", colonistType, soldierType));
        String s = changeType.toString();
        assertTrue(s.contains("freeColonist"));
    }

    // ------------------------------------------------------------------
    // M18: lambda$getUnitChange$0(UnitType, UnitTypeChange) — 2 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M18-TP1: ENTRY -> N0 -(B1:true)-> N2 -> N3 -> EXIT (uc.to != toType -> returns false)
    public void testM18_TP1_lambdaGetUnitChangeMismatch() {
        changeType.addUnitTypeChange(makeChange("cft.ucLam1", colonistType, soldierType));
        assertNull(changeType.getUnitChange(colonistType, pioneerType));
    }

    // M18-TP2: ENTRY -> N0 -(B1:false)-> N1 -> N3 -> EXIT (uc.to == toType -> returns true)
    public void testM18_TP2_lambdaGetUnitChangeMatch() {
        UnitTypeChange uc = makeChange("cft.ucLam2", colonistType, soldierType);
        changeType.addUnitTypeChange(uc);
        assertSame(uc, changeType.getUnitChange(colonistType, soldierType));
    }
}
