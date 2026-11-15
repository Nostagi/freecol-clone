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
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import net.sf.freecol.common.io.FreeColXMLReader;
import net.sf.freecol.common.io.FreeColXMLWriter;
import net.sf.freecol.util.test.FreeColTestCase;


/**
 * CFG-driven test suite for {@link Event} derived from
 * {@code test_cft/graph/net/sf/freecol/common/model/Event.json}.
 *
 * Covers all 19 methods (M1..M19) and achieves 100% feasible C3 (node + edge)
 * coverage across 30 Test Paths.
 *
 * Infeasible edges documented:
 * - M12 ({@code copyIn}): {@code N1 -> N2} ({@code B2:false}) — {@code o} is a
 *   non-null {@code Event} already validated by {@code copyInCast}, so
 *   {@code FreeColSpecObjectType.copyIn(o)} always returns {@code true}.
 */
public class EventCfgTest extends FreeColTestCase {

    private Event event;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        // Pre-condition: standard specification and default Event instance
        event = new Event("model.event.cftTest", spec());
    }

    // ------------------------------------------------------------------
    // Pre-condition helpers & stubs
    // ------------------------------------------------------------------

    private Limit makeLimit(String id) {
        return new Limit(id,
            new Operand(Operand.OperandType.UNITS, Operand.ScopeLevel.PLAYER),
            Limit.Operator.GE,
            new Operand(1));
    }

    private void invokeAddLimit(Event target, Limit limit) throws Exception {
        Method m = Event.class.getDeclaredMethod("addLimit", Limit.class);
        m.setAccessible(true);
        m.invoke(target, limit);
    }

    private String toXml(Event target) throws Exception {
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
    // M1: Event(String, Specification) — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M1-TP1: ENTRY -> N0 -> EXIT (constructor with id and specification)
    public void testM1_TP1_idSpecConstructor() {
        Event e = new Event("model.event.cftInit", spec());
        assertEquals("model.event.cftInit", e.getId());
        assertSame(spec(), e.getSpecification());
        assertNull(e.getValue());
        assertEquals(0, e.getScoreValue());
        assertNull(e.getLimits());
    }

    // ------------------------------------------------------------------
    // M2: Event(FreeColXMLReader, Specification) — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M2-TP1: ENTRY -> N0 -> EXIT (constructor reading from XML stream)
    public void testM2_TP1_xmlReaderConstructor() throws Exception {
        String xml = "<event id=\"model.event.cftXml\" value=\"restricted\" score-value=\"15\"/>";
        try (FreeColXMLReader xr = openReader(xml)) {
            Event e = new Event(xr, spec());
            assertEquals("model.event.cftXml", e.getId());
            assertEquals("restricted", e.getValue());
            assertEquals(15, e.getScoreValue());
        }
    }

    // ------------------------------------------------------------------
    // M3: getValue() — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M3-TP1: ENTRY -> N0 -> EXIT (return value)
    public void testM3_TP1_getValue() {
        assertNull(event.getValue());
    }

    // ------------------------------------------------------------------
    // M4: setValue(String) — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M4-TP1: ENTRY -> N0 -> EXIT (set value)
    public void testM4_TP1_setValue() {
        event.setValue("european");
        assertEquals("european", event.getValue());
    }

    // ------------------------------------------------------------------
    // M5: getScoreValue() — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M5-TP1: ENTRY -> N0 -> EXIT (return scoreValue)
    public void testM5_TP1_getScoreValue() {
        assertEquals(0, event.getScoreValue());
    }

    // ------------------------------------------------------------------
    // M6: setScoreValue(int) — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M6-TP1: ENTRY -> N0 -> EXIT (set scoreValue)
    public void testM6_TP1_setScoreValue() {
        event.setScoreValue(25);
        assertEquals(25, event.getScoreValue());
    }

    // ------------------------------------------------------------------
    // M7: getLimits() — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M7-TP1: ENTRY -> N0 -> EXIT (return limits map)
    public void testM7_TP1_getLimits() {
        assertNull(event.getLimits());
    }

    // ------------------------------------------------------------------
    // M8: setLimits(Map) — 2 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M8-TP1: ENTRY -> N0 -(B1:true)-> N2 -> N3 -> EXIT (limits != null -> clears and puts)
    public void testM8_TP1_setLimitsWhenNonNull() {
        Limit first = makeLimit("cft.limit1");
        Limit second = makeLimit("cft.limit2");
        event.setLimits(Collections.singletonMap(first.getId(), first));

        Map<String, Limit> replacement = new HashMap<>();
        replacement.put(second.getId(), second);
        event.setLimits(replacement);

        assertEquals(1, event.getLimits().size());
        assertNull(event.getLimit("cft.limit1"));
        assertSame(second, event.getLimit("cft.limit2"));
    }

    // M8-TP2: ENTRY -> N0 -(B1:false)-> N1 -> N3 -> EXIT (limits == null -> allocates HashMap)
    public void testM8_TP2_setLimitsWhenNull() {
        Limit l = makeLimit("cft.limitInit");
        event.setLimits(Collections.singletonMap(l.getId(), l));
        assertNotNull(event.getLimits());
        assertSame(l, event.getLimit("cft.limitInit"));
    }

    // ------------------------------------------------------------------
    // M9: getLimitValues() — 2 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M9-TP1: ENTRY -> N0 -(B1:true)-> N2 -> N3 -> EXIT (limits != null -> returns values)
    public void testM9_TP1_getLimitValuesWhenNonNull() {
        Limit l = makeLimit("cft.limitVal");
        event.setLimits(Collections.singletonMap(l.getId(), l));
        assertEquals(1, event.getLimitValues().size());
    }

    // M9-TP2: ENTRY -> N0 -(B1:false)-> N1 -> N3 -> EXIT (limits == null -> returns emptyList)
    public void testM9_TP2_getLimitValuesWhenNull() {
        assertTrue(event.getLimitValues().isEmpty());
    }

    // ------------------------------------------------------------------
    // M10: getLimit(String) — 2 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M10-TP1: ENTRY -> N0 -(B1:true)-> N2 -> N3 -> EXIT (limits != null -> lookup in map)
    public void testM10_TP1_getLimitWhenNonNull() {
        Limit l = makeLimit("cft.limitGet");
        event.setLimits(Collections.singletonMap(l.getId(), l));
        assertSame(l, event.getLimit("cft.limitGet"));
        assertNull(event.getLimit("cft.missing"));
    }

    // M10-TP2: ENTRY -> N0 -(B1:false)-> N1 -> N3 -> EXIT (limits == null -> returns null)
    public void testM10_TP2_getLimitWhenNull() {
        assertNull(event.getLimit("cft.limitGet"));
    }

    // ------------------------------------------------------------------
    // M11: addLimit(Limit) — 2 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M11-TP1: ENTRY -> N0 -(B1:true)-> N2 -> EXIT (limits != null -> puts directly)
    public void testM11_TP1_addLimitWhenNonNull() throws Exception {
        Limit l1 = makeLimit("cft.l1");
        Limit l2 = makeLimit("cft.l2");
        invokeAddLimit(event, l1);
        invokeAddLimit(event, l2);
        assertEquals(2, event.getLimits().size());
    }

    // M11-TP2: ENTRY -> N0 -(B1:false)-> N1 -> N2 -> EXIT (limits == null -> initializes map)
    public void testM11_TP2_addLimitWhenNull() throws Exception {
        Limit l1 = makeLimit("cft.l1");
        invokeAddLimit(event, l1);
        assertSame(l1, event.getLimit("cft.l1"));
    }

    // ------------------------------------------------------------------
    // M12: copyIn(FreeColObject) — 2 Test Paths (100% feasible C3)
    // ------------------------------------------------------------------

    // M12-TP1: ENTRY -> N0 -(B1:true)-> N2 -> EXIT (o == null -> returns false)
    public void testM12_TP1_copyInNullOrIncompatibleReturnsFalse() {
        assertFalse(event.copyIn(null));
        assertFalse(event.copyIn(spec().getGoodsType("model.goods.sugar")));
    }

    // M12-TP2: ENTRY -> N0 -(B1:false)-> N1 -(B2:true)-> N3 -> EXIT (o != null && super.copyIn(o))
    public void testM12_TP2_copyInValidEventCopiesState() {
        Event other = new Event("model.event.cftTest", spec());
        other.setValue("cftVal");
        other.setScoreValue(42);
        Limit l = makeLimit("cft.copyLimit");
        other.setLimits(Collections.singletonMap(l.getId(), l));

        event.getFeatureContainer();
        assertTrue(event.copyIn(other));
        assertEquals("cftVal", event.getValue());
        assertEquals(42, event.getScoreValue());
        assertSame(l, event.getLimit("cft.copyLimit"));
    }

    // ------------------------------------------------------------------
    // M13: writeAttributes(FreeColXMLWriter) — 4 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M13-TP1: ENTRY -> N0 -(B1:true)-> N2 -(B2:true)-> N4 -> EXIT (value == null && scoreValue == 0)
    public void testM13_TP1_writeAttributesNeitherValueNorScore() throws Exception {
        String xml = toXml(event);
        assertFalse(xml.contains("value="));
        assertFalse(xml.contains("score-value="));
    }

    // M13-TP2: ENTRY -> N0 -(B1:true)-> N2 -(B2:false)-> N3 -> N4 -> EXIT (value == null && scoreValue != 0)
    public void testM13_TP2_writeAttributesScoreOnly() throws Exception {
        event.setScoreValue(10);
        String xml = toXml(event);
        assertFalse(xml.contains(" value="));
        assertTrue(xml.contains("score-value=\"10\""));
    }

    // M13-TP3: ENTRY -> N0 -(B1:false)-> N1 -> N2 -(B2:true)-> N4 -> EXIT (value != null && scoreValue == 0)
    public void testM13_TP3_writeAttributesValueOnly() throws Exception {
        event.setValue("testVal");
        String xml = toXml(event);
        assertTrue(xml.contains("value=\"testVal\""));
        assertFalse(xml.contains("score-value="));
    }

    // M13-TP4: ENTRY -> N0 -(B1:false)-> N1 -> N2 -(B2:false)-> N3 -> N4 -> EXIT (value != null && scoreValue != 0)
    public void testM13_TP4_writeAttributesValueAndScore() throws Exception {
        event.setValue("testVal");
        event.setScoreValue(10);
        String xml = toXml(event);
        assertTrue(xml.contains("value=\"testVal\""));
        assertTrue(xml.contains("score-value=\"10\""));
    }

    // ------------------------------------------------------------------
    // M14: writeChildren(FreeColXMLWriter) — 2 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M14-TP1: ENTRY -> N0 -> N1 -(B1:true)-> N3 -> EXIT (no limits -> no limit child elements)
    public void testM14_TP1_writeChildrenEmpty() throws Exception {
        String xml = toXml(event);
        assertFalse(xml.contains("<limit"));
    }

    // M14-TP2: ENTRY -> N0 -> N1 -(B1:false)-> N2 -> N1 -(B1:true)-> N3 -> EXIT (limits present -> writes limit)
    public void testM14_TP2_writeChildrenWithLimit() throws Exception {
        invokeAddLimit(event, makeLimit("cft.writeLimit"));
        String xml = toXml(event);
        assertTrue(xml.contains("<limit"));
    }

    // ------------------------------------------------------------------
    // M15: readAttributes(FreeColXMLReader) — 2 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M15-TP1: ENTRY -> N0 -(B1:true)-> N2 -> N3 -> EXIT (no scoreValue -> reads score-value)
    public void testM15_TP1_readAttributesNewScoreValue() throws Exception {
        String xml = "<event id=\"model.event.cftTest\" value=\"v1\" score-value=\"33\"/>";
        try (FreeColXMLReader xr = openReader(xml)) {
            event.readAttributes(xr);
        }
        assertEquals("v1", event.getValue());
        assertEquals(33, event.getScoreValue());
    }

    // M15-TP2: ENTRY -> N0 -(B1:false)-> N1 -> N3 -> EXIT (has old scoreValue -> reads scoreValue)
    public void testM15_TP2_readAttributesOldScoreValue() throws Exception {
        String xml = "<event id=\"model.event.cftTest\" scoreValue=\"77\"/>";
        try (FreeColXMLReader xr = openReader(xml)) {
            event.readAttributes(xr);
        }
        assertEquals(77, event.getScoreValue());
    }

    // ------------------------------------------------------------------
    // M16: clearContainers(FreeColXMLReader) — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M16-TP1: ENTRY -> N0 -> EXIT (clears limits to null)
    public void testM16_TP1_clearContainers() throws Exception {
        invokeAddLimit(event, makeLimit("cft.clearLimit"));
        try (FreeColXMLReader xr = openReader("<event id=\"model.event.cftTest\"/>")) {
            event.clearContainers(xr);
        }
        assertNull(event.getLimits());
    }

    // ------------------------------------------------------------------
    // M17: readChildren(FreeColXMLReader) — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M17-TP1: ENTRY -> N0 -> EXIT (delegates readChildren to super)
    public void testM17_TP1_readChildren() throws Exception {
        try (FreeColXMLReader xr = openReader("<event id=\"model.event.cftTest\"/>")) {
            event.readChildren(xr);
        }
        assertNull(event.getLimits());
    }

    // ------------------------------------------------------------------
    // M18: readChild(FreeColXMLReader) — 2 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M18-TP1: ENTRY -> N0 -(B1:true)-> N2 -> N3 -> EXIT (non-limit tag -> super.readChild)
    public void testM18_TP1_readChildOtherTag() throws Exception {
        String xml = "<event id=\"model.event.cftTest\">"
            + "<ability id=\"model.ability.bornInColony\" value=\"true\"/>"
            + "</event>";
        try (FreeColXMLReader xr = openReader(xml)) {
            event.readFromXML(xr);
        }
        assertTrue(event.hasAbility("model.ability.bornInColony"));
    }

    // M18-TP2: ENTRY -> N0 -(B1:false)-> N1 -> N3 -> EXIT (limit tag -> addLimit)
    public void testM18_TP2_readChildLimitTag() throws Exception {
        String xml = "<event id=\"model.event.cftTest\">"
            + "<limit id=\"cft.xmlLimit\" operator=\"GE\">"
            + "<left-hand-side operand-type=\"UNITS\" scope-level=\"PLAYER\"/>"
            + "<right-hand-side value=\"1\"/>"
            + "</limit>"
            + "</event>";
        try (FreeColXMLReader xr = openReader(xml)) {
            event.readFromXML(xr);
        }
        assertNotNull(event.getLimit("cft.xmlLimit"));
    }

    // ------------------------------------------------------------------
    // M19: getXMLTagName() — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M19-TP1: ENTRY -> N0 -> EXIT (return TAG)
    public void testM19_TP1_getXMLTagName() {
        assertEquals(Event.TAG, event.getXMLTagName());
    }
}
