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
import java.util.ArrayList;
import java.util.List;

import net.sf.freecol.common.io.FreeColXMLReader;
import net.sf.freecol.common.io.FreeColXMLWriter;
import net.sf.freecol.util.test.FreeColTestCase;


/**
 * CFG-driven test suite for {@link FoundingFather} derived from
 * {@code test_cft/graph/net/sf/freecol/common/model/FoundingFather.json}.
 *
 * Covers all 20 methods (M1..M20) and achieves 100% feasible C3 (node + edge)
 * coverage across 31 Test Paths.
 *
 * Infeasible edges documented:
 * - M13 ({@code copyIn}): {@code N1 -> N2} ({@code B2:false}) — {@code super.copyIn(o)}
 *   delegates through {@code FreeColSpecObjectType}, {@code FreeColSpecObject}, and
 *   {@code FreeColObject}; when {@code o} is a non-null {@code FoundingFather},
 *   every superclass {@code copyInCast} succeeds and {@code super.copyIn(o)}
 *   always returns {@code true}.
 */
public class FoundingFatherCfgTest extends FreeColTestCase {

    private FoundingFather father;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        // Pre-condition: Create a fresh FoundingFather bound to standard Specification
        father = new FoundingFather("test.foundingFather", spec());
        father.setType(FoundingFather.FoundingFatherType.TRADE);
    }

    // ------------------------------------------------------------------
    // Pre-condition helpers & stubs
    // ------------------------------------------------------------------

    private void invokeAddEvent(Event event) throws Exception {
        Method m = FoundingFather.class.getDeclaredMethod("addEvent", Event.class);
        m.setAccessible(true);
        m.invoke(father, event);
    }

    private void invokeAddUnit(AbstractUnit unit) throws Exception {
        Method m = FoundingFather.class.getDeclaredMethod("addUnit", AbstractUnit.class);
        m.setAccessible(true);
        m.invoke(father, unit);
    }

    // ------------------------------------------------------------------
    // M1: <init>(String, Specification) — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M1-TP1: ENTRY -> N0 -> EXIT (constructs FoundingFather with id and specification)
    public void testM1_TP1_constructor() {
        FoundingFather ff = new FoundingFather("test.ff.id", spec());
        assertEquals("test.ff.id", ff.getId());
        assertSame(spec(), ff.getSpecification());
    }

    // ------------------------------------------------------------------
    // M2: getType() — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M2-TP1: ENTRY -> N0 -> EXIT (returns current FoundingFatherType)
    public void testM2_TP1_getType() {
        assertEquals(FoundingFather.FoundingFatherType.TRADE, father.getType());
    }

    // ------------------------------------------------------------------
    // M3: setType(FoundingFatherType) — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M3-TP1: ENTRY -> N0 -> EXIT (updates type field)
    public void testM3_TP1_setType() {
        father.setType(FoundingFather.FoundingFatherType.MILITARY);
        assertEquals(FoundingFather.FoundingFatherType.MILITARY, father.getType());
    }

    // ------------------------------------------------------------------
    // M4: getTypeKey() — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M4-TP1: ENTRY -> N0 -> EXIT (returns type message key)
    public void testM4_TP1_getTypeKey() {
        assertEquals("model.foundingFather.trade", father.getTypeKey());
    }

    // ------------------------------------------------------------------
    // M5: getWeight(int) — 3 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M5-TP1: ENTRY -> N0 -(B1:true)-> N3 -> N4 -> EXIT (age < 0 -> returns 0)
    public void testM5_TP1_getWeightNegativeAge() {
        assertEquals(0, father.getWeight(-1));
    }

    // M5-TP2: ENTRY -> N0 -(B1:false)-> N1 -(B2:true)-> N3 -> N4 -> EXIT (age >= weight.length -> returns 0)
    public void testM5_TP2_getWeightOutOfBoundsAge() {
        assertEquals(0, father.getWeight(Specification.NUMBER_OF_AGES));
    }

    // M5-TP3: ENTRY -> N0 -(B1:false)-> N1 -(B2:false)-> N2 -> N4 -> EXIT (0 <= age < weight.length -> returns weight[age])
    public void testM5_TP3_getWeightValidAge() {
        FoundingFather adamSmith = spec().getFoundingFather("model.foundingFather.adamSmith");
        assertEquals(2, adamSmith.getWeight(0));
    }

    // ------------------------------------------------------------------
    // M6: getEvents() — 2 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M6-TP1: ENTRY -> N0 -(B1:false)-> N1 -> N3 -> EXIT (events == null -> returns emptyList)
    public void testM6_TP1_getEventsWhenNull() {
        assertTrue(father.getEvents().isEmpty());
    }

    // M6-TP2: ENTRY -> N0 -(B1:true)-> N2 -> N3 -> EXIT (events != null -> returns events list)
    public void testM6_TP2_getEventsWhenNonNull() {
        List<Event> list = new ArrayList<>();
        list.add(new Event("model.event.declareIndependence", spec()));
        father.setEvents(list);
        assertEquals(1, father.getEvents().size());
    }

    // ------------------------------------------------------------------
    // M7: setEvents(List) — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M7-TP1: ENTRY -> N0 -> EXIT (assigns events list)
    public void testM7_TP1_setEvents() {
        List<Event> list = new ArrayList<>();
        list.add(new Event("e1", spec()));
        father.setEvents(list);
        assertSame(list, father.getEvents());
    }

    // ------------------------------------------------------------------
    // M8: addEvent(Event) — 2 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M8-TP1: ENTRY -> N0 -(B1:false)-> N1 -> N2 -> EXIT (events == null -> creates list and adds)
    public void testM8_TP1_addEventFirst() throws Exception {
        invokeAddEvent(new Event("e1", spec()));
        assertEquals(1, father.getEvents().size());
    }

    // M8-TP2: ENTRY -> N0 -(B1:true)-> N2 -> EXIT (events != null -> appends to existing list)
    public void testM8_TP2_addEventSecond() throws Exception {
        invokeAddEvent(new Event("e1", spec()));
        invokeAddEvent(new Event("e2", spec()));
        assertEquals(2, father.getEvents().size());
    }

    // ------------------------------------------------------------------
    // M9: getUnitList() — 2 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M9-TP1: ENTRY -> N0 -(B1:false)-> N1 -> N3 -> EXIT (units == null -> returns emptyList)
    public void testM9_TP1_getUnitListWhenNull() {
        assertTrue(father.getUnitList().isEmpty());
    }

    // M9-TP2: ENTRY -> N0 -(B1:true)-> N2 -> N3 -> EXIT (units != null -> returns units list)
    public void testM9_TP2_getUnitListWhenNonNull() {
        List<AbstractUnit> list = new ArrayList<>();
        list.add(new AbstractUnit("model.unit.freeColonist", Specification.DEFAULT_ROLE_ID, 1));
        father.setUnits(list);
        assertEquals(1, father.getUnitList().size());
    }

    // ------------------------------------------------------------------
    // M10: setUnits(List) — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M10-TP1: ENTRY -> N0 -> EXIT (assigns units list)
    public void testM10_TP1_setUnits() {
        List<AbstractUnit> list = new ArrayList<>();
        list.add(new AbstractUnit("model.unit.freeColonist", Specification.DEFAULT_ROLE_ID, 1));
        father.setUnits(list);
        assertSame(list, father.getUnitList());
    }

    // ------------------------------------------------------------------
    // M11: addUnit(AbstractUnit) — 2 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M11-TP1: ENTRY -> N0 -(B1:false)-> N1 -> N2 -> EXIT (units == null -> creates list and adds)
    public void testM11_TP1_addUnitFirst() throws Exception {
        invokeAddUnit(new AbstractUnit("model.unit.freeColonist", Specification.DEFAULT_ROLE_ID, 1));
        assertEquals(1, father.getUnitList().size());
    }

    // M11-TP2: ENTRY -> N0 -(B1:true)-> N2 -> EXIT (units != null -> appends to existing list)
    public void testM11_TP2_addUnitSecond() throws Exception {
        invokeAddUnit(new AbstractUnit("model.unit.freeColonist", Specification.DEFAULT_ROLE_ID, 1));
        invokeAddUnit(new AbstractUnit("model.unit.caravel", Specification.DEFAULT_ROLE_ID, 1));
        assertEquals(2, father.getUnitList().size());
    }

    // ------------------------------------------------------------------
    // M12: isAvailableTo(Player) — 2 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M12-TP1: ENTRY -> N0 -(B1:false)-> N1 -> N3 -> EXIT (!player.isEuropean() is true -> false)
    public void testM12_TP1_isAvailableToNativePlayer() {
        Game game = getStandardGame();
        Player tupi = game.getPlayerByNationId("model.nation.tupi");
        assertFalse(father.isAvailableTo(tupi));
    }

    // M12-TP2: ENTRY -> N0 -(B1:true)-> N2 -> N3 -> EXIT (player.isEuropean() is true -> appliesTo(player))
    public void testM12_TP2_isAvailableToEuropeanPlayer() {
        Game game = getStandardGame();
        Player dutch = game.getPlayerByNationId("model.nation.dutch");
        assertTrue(father.isAvailableTo(dutch));
    }

    // ------------------------------------------------------------------
    // M13: copyIn(FreeColObject) — 2 feasible Test Paths (100% feasible C3)
    // ------------------------------------------------------------------

    // M13-TP1: ENTRY -> N0 -(B1:true)-> N2 -> EXIT (o == null when other is null or wrong type -> false)
    public void testM13_TP1_copyInNullOrWrongType() {
        assertFalse(father.copyIn(null));
        assertFalse(father.copyIn(spec().getGoodsType("model.goods.grain")));
    }

    // M13-TP2: ENTRY -> N0 -(B1:false)-> N1 -(B2:true)-> N3 -> N4 -(B3:false)-> N5 -> N4 -(B3:true)-> N6 -> EXIT (copies type, weights, events, units)
    public void testM13_TP2_copyInValidFoundingFather() {
        FoundingFather src = spec().getFoundingFather("model.foundingFather.adamSmith");
        FoundingFather target = new FoundingFather(src.getId(), spec());
        target.getFeatureContainer();
        assertTrue(target.copyIn(src));
        assertEquals(src.getType(), target.getType());
        assertEquals(src.getWeight(0), target.getWeight(0));
    }

    // ------------------------------------------------------------------
    // M14: writeAttributes(FreeColXMLWriter) — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M14-TP1: ENTRY -> N0 -> N1 -(B1:false)-> N2 -> N1 -(B1:true)-> N3 -> EXIT (writes type and weight1..weight3)
    public void testM14_TP1_writeAttributes() throws Exception {
        FoundingFather adamSmith = spec().getFoundingFather("model.foundingFather.adamSmith");
        StringWriter sw = new StringWriter();
        try (FreeColXMLWriter xw = new FreeColXMLWriter(sw, FreeColXMLWriter.WriteScope.toSave())) {
            adamSmith.toXML(xw);
        }
        String xml = sw.toString();
        assertTrue(xml.contains("type=\"trade\""));
        assertTrue(xml.contains("weight1=\"2\""));
    }

    // ------------------------------------------------------------------
    // M15: writeChildren(FreeColXMLWriter) — 2 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M15-TP1: ENTRY -> N0 -> N1 -(B1:true)-> N3 -> N4 -(B2:true)-> N6 -> EXIT (events empty, units empty)
    public void testM15_TP1_writeChildrenEmpty() throws Exception {
        StringWriter sw = new StringWriter();
        try (FreeColXMLWriter xw = new FreeColXMLWriter(sw, FreeColXMLWriter.WriteScope.toSave())) {
            xw.writeStartElement(FoundingFather.TAG);
            father.writeChildren(xw);
            xw.writeEndElement();
        }
        String xml = sw.toString();
        assertFalse(xml.contains("<unit"));
        assertFalse(xml.contains("<event"));
    }

    // M15-TP2: ENTRY -> N0 -> N1 -(B1:false)-> N2 -> N1 -(B1:true)-> N3 -> N4 -(B2:false)-> N5 -> N4 -(B2:true)-> N6 -> EXIT (events non-empty, units non-empty)
    public void testM15_TP2_writeChildrenWithEventsAndUnits() throws Exception {
        invokeAddEvent(new Event("model.event.declareIndependence", spec()));
        invokeAddUnit(new AbstractUnit("model.unit.freeColonist", Specification.DEFAULT_ROLE_ID, 1));
        StringWriter sw = new StringWriter();
        try (FreeColXMLWriter xw = new FreeColXMLWriter(sw, FreeColXMLWriter.WriteScope.toSave())) {
            xw.writeStartElement(FoundingFather.TAG);
            father.writeChildren(xw);
            xw.writeEndElement();
        }
        String xml = sw.toString();
        assertTrue(xml.contains("<event"));
        assertTrue(xml.contains("<unit"));
    }

    // ------------------------------------------------------------------
    // M16: readAttributes(FreeColXMLReader) — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M16-TP1: ENTRY -> N0 -> N1 -(B1:false)-> N2 -> N1 -(B1:true)-> N3 -> EXIT (reads type and weight1..weight3)
    public void testM16_TP1_readAttributes() throws Exception {
        String xml = "<founding-father id=\"test.ff\" type=\"political\" weight1=\"5\" weight2=\"7\" weight3=\"9\"/>";
        try (FreeColXMLReader xr = new FreeColXMLReader(new StringReader(xml))) {
            xr.nextTag();
            father.readAttributes(xr);
        }
        assertEquals(FoundingFather.FoundingFatherType.POLITICAL, father.getType());
        assertEquals(5, father.getWeight(0));
        assertEquals(7, father.getWeight(1));
        assertEquals(9, father.getWeight(2));
    }

    // ------------------------------------------------------------------
    // M17: clearContainers(FreeColXMLReader) — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M17-TP1: ENTRY -> N0 -> EXIT (clears events and units to null)
    public void testM17_TP1_clearContainers() throws Exception {
        invokeAddEvent(new Event("e1", spec()));
        invokeAddUnit(new AbstractUnit("model.unit.freeColonist", Specification.DEFAULT_ROLE_ID, 1));
        String xml = "<founding-father id=\"test.ff\"/>";
        try (FreeColXMLReader xr = new FreeColXMLReader(new StringReader(xml))) {
            xr.nextTag();
            father.clearContainers(xr);
        }
        assertTrue(father.getEvents().isEmpty());
        assertTrue(father.getUnitList().isEmpty());
    }

    // ------------------------------------------------------------------
    // M18: readChildren(FreeColXMLReader) — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M18-TP1: ENTRY -> N0 -> EXIT (delegates to super.readChildren)
    public void testM18_TP1_readChildren() throws Exception {
        String xml = "<founding-father id=\"test.ff\"/>";
        try (FreeColXMLReader xr = new FreeColXMLReader(new StringReader(xml))) {
            xr.nextTag();
            father.readChildren(xr);
        }
        assertTrue(father.getEvents().isEmpty());
    }

    // ------------------------------------------------------------------
    // M19: readChild(FreeColXMLReader) — 4 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M19-TP1: ENTRY -> N0 -(B1:false)-> N1 -> N7 -> EXIT (UNIT_TAG -> adds AbstractUnit)
    public void testM19_TP1_readChildUnit() throws Exception {
        String xml = "<founding-father id=\"test.ff\">"
                + "<unit id=\"model.unit.freeColonist\"/>"
                + "</founding-father>";
        try (FreeColXMLReader xr = new FreeColXMLReader(new StringReader(xml))) {
            xr.nextTag();
            xr.nextTag();
            father.readChild(xr);
        }
        assertEquals(1, father.getUnitList().size());
    }

    // M19-TP2: ENTRY -> N0 -(B1:true)-> N2 -(B2:false)-> N3 -> N7 -> EXIT (Event.TAG -> adds Event)
    public void testM19_TP2_readChildEvent() throws Exception {
        String xml = "<founding-father id=\"test.ff\">"
                + "<event id=\"model.event.declareIndependence\"/>"
                + "</founding-father>";
        try (FreeColXMLReader xr = new FreeColXMLReader(new StringReader(xml))) {
            xr.nextTag();
            xr.nextTag();
            father.readChild(xr);
        }
        assertEquals(1, father.getEvents().size());
    }

    // M19-TP3: ENTRY -> N0 -(B1:true)-> N2 -(B2:true)-> N4 -(B3:false)-> N5 -> N7 -> EXIT (UPGRADE_TAG -> closes tag)
    public void testM19_TP3_readChildUpgrade() throws Exception {
        String xml = "<founding-father id=\"test.ff\">"
                + "<upgrade id=\"legacy.upgrade\"/>"
                + "</founding-father>";
        try (FreeColXMLReader xr = new FreeColXMLReader(new StringReader(xml))) {
            xr.nextTag();
            xr.nextTag();
            father.readChild(xr);
        }
        assertTrue(father.getEvents().isEmpty());
    }

    // M19-TP4: ENTRY -> N0 -(B1:true)-> N2 -(B2:true)-> N4 -(B3:true)-> N6 -> N7 -> EXIT (other tag -> delegates to super.readChild)
    public void testM19_TP4_readChildOther() throws Exception {
        String xml = "<founding-father id=\"test.ff\">"
                + "<ability id=\"model.ability.buildFactory\" value=\"true\"/>"
                + "</founding-father>";
        try (FreeColXMLReader xr = new FreeColXMLReader(new StringReader(xml))) {
            xr.nextTag();
            xr.nextTag();
            father.readChild(xr);
        }
        assertTrue(father.hasAbility("model.ability.buildFactory"));
    }

    // ------------------------------------------------------------------
    // M20: getXMLTagName() — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M20-TP1: ENTRY -> N0 -> EXIT (returns TAG "founding-father")
    public void testM20_TP1_getXMLTagName() {
        assertEquals(FoundingFather.TAG, father.getXMLTagName());
    }
}
