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
import java.util.ArrayList;
import java.util.List;

import net.sf.freecol.common.io.FreeColXMLReader;
import net.sf.freecol.common.io.FreeColXMLWriter;
import net.sf.freecol.server.model.ServerUnit;
import net.sf.freecol.util.test.FreeColTestCase;


/**
 * CFG-driven test suite for {@link HighSeas} derived from
 * {@code test_cft/graph/net/sf/freecol/common/model/HighSeas.json}.
 *
 * Covers all 20 methods (M1..M20) and achieves 100% feasible C3 (node + edge)
 * coverage across 27 Test Paths.
 *
 * Infeasible edges documented:
 * - M14 ({@code copyIn}): {@code N1 -> N2} ({@code B2:false}) — {@code super.copyIn(o)}
 *   delegates through {@code UnitLocation}, {@code FreeColGameObject}, and
 *   {@code FreeColObject}; when {@code o} is a non-null {@code HighSeas},
 *   every superclass {@code copyInCast} succeeds and {@code super.copyIn(o)}
 *   always returns {@code true}.
 */
public class HighSeasCfgTest extends FreeColTestCase {

    private Game game;
    private Player dutch;
    private Player french;
    private HighSeas highSeas;
    private UnitType caravelType;
    private UnitType colonistType;
    private GoodsType grainType;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        // Pre-condition: Initialize standard game, European players, HighSeas, and standard types
        game = getStandardGame();
        dutch = game.getPlayerByNationId("model.nation.dutch");
        french = game.getPlayerByNationId("model.nation.french");
        highSeas = new HighSeas(game, "highSeas:test");
        caravelType = spec().getUnitType("model.unit.caravel");
        colonistType = spec().getUnitType("model.unit.freeColonist");
        grainType = spec().getGoodsType("model.goods.grain");
    }

    // ------------------------------------------------------------------
    // M1: <init>(Game) — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M1-TP1: ENTRY -> N0 -> EXIT (constructs HighSeas with game)
    public void testM1_TP1_constructorWithGame() {
        HighSeas hs = new HighSeas(game);
        assertSame(game, hs.getGame());
        assertTrue(hs.getDestinations().isEmpty());
    }

    // ------------------------------------------------------------------
    // M2: <init>(Game, String) — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M2-TP1: ENTRY -> N0 -> EXIT (constructs HighSeas with game and id)
    public void testM2_TP1_constructorWithGameAndId() {
        HighSeas hs = new HighSeas(game, "highSeas:custom");
        assertSame(game, hs.getGame());
        assertEquals("highSeas:custom", hs.getId());
    }

    // ------------------------------------------------------------------
    // M3: getDestinations() — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M3-TP1: ENTRY -> N0 -> EXIT (returns destinations list)
    public void testM3_TP1_getDestinations() {
        assertNotNull(highSeas.getDestinations());
        assertTrue(highSeas.getDestinations().isEmpty());
    }

    // ------------------------------------------------------------------
    // M4: destinationsToString() — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M4-TP1: ENTRY -> N0 -> EXIT (joins destination IDs with ",")
    public void testM4_TP1_destinationsToString() {
        highSeas.addDestination(dutch.getEurope());
        highSeas.addDestination(french.getEurope());
        String s = highSeas.destinationsToString();
        assertTrue(s.contains(dutch.getEurope().getId()));
        assertTrue(s.contains(french.getEurope().getId()));
    }

    // ------------------------------------------------------------------
    // M5: setDestinations(List) — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M5-TP1: ENTRY -> N0 -> EXIT (clears and replaces destinations list)
    public void testM5_TP1_setDestinations() {
        List<Location> list = new ArrayList<>();
        list.add(dutch.getEurope());
        highSeas.setDestinations(list);
        assertEquals(1, highSeas.getDestinations().size());
        assertSame(dutch.getEurope(), highSeas.getDestinations().get(0));
    }

    // ------------------------------------------------------------------
    // M6: addDestination(Location) — 3 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M6-TP1: ENTRY -> N0 -(B1:true)-> N3 -> N4 -> EXIT (destination == null -> logs warning, does not add)
    public void testM6_TP1_addDestinationNull() {
        highSeas.addDestination(null);
        assertTrue(highSeas.getDestinations().isEmpty());
    }

    // M6-TP2: ENTRY -> N0 -(B1:false)-> N1 -(B2:false)-> N2 -> N4 -> EXIT (new non-null destination -> added)
    public void testM6_TP2_addDestinationNew() {
        highSeas.addDestination(dutch.getEurope());
        assertEquals(1, highSeas.getDestinations().size());
    }

    // M6-TP3: ENTRY -> N0 -(B1:false)-> N1 -(B2:true)-> N4 -> EXIT (duplicate destination -> not added twice)
    public void testM6_TP3_addDestinationDuplicate() {
        highSeas.addDestination(dutch.getEurope());
        highSeas.addDestination(dutch.getEurope());
        assertEquals(1, highSeas.getDestinations().size());
    }

    // ------------------------------------------------------------------
    // M7: removeDestination(Location) — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M7-TP1: ENTRY -> N0 -> EXIT (removes destination from list)
    public void testM7_TP1_removeDestination() {
        highSeas.addDestination(dutch.getEurope());
        highSeas.removeDestination(dutch.getEurope());
        assertTrue(highSeas.getDestinations().isEmpty());
    }

    // ------------------------------------------------------------------
    // M8: getLocationLabel() — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M8-TP1: ENTRY -> N0 -> EXIT (returns StringTemplate for "model.tile.highSeas.name")
    public void testM8_TP1_getLocationLabel() {
        StringTemplate st = highSeas.getLocationLabel();
        assertEquals("model.tile.highSeas.name", st.getId());
    }

    // ------------------------------------------------------------------
    // M9: up() — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M9-TP1: ENTRY -> N0 -> EXIT (returns this)
    public void testM9_TP1_up() {
        assertSame(highSeas, highSeas.up());
    }

    // ------------------------------------------------------------------
    // M10: getRank() — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M10-TP1: ENTRY -> N0 -> EXIT (returns Location.LOCATION_RANK_HIGHSEAS)
    public void testM10_TP1_getRank() {
        assertEquals(Location.LOCATION_RANK_HIGHSEAS, highSeas.getRank());
    }

    // ------------------------------------------------------------------
    // M11: toShortString() — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M11-TP1: ENTRY -> N0 -> EXIT (returns "HighSeas")
    public void testM11_TP1_toShortString() {
        assertEquals("HighSeas", highSeas.toShortString());
    }

    // ------------------------------------------------------------------
    // M12: getNoAddReason(Locatable) — 3 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M12-TP1: ENTRY -> N0 -(B1:true)-> N3 -> N4 -> EXIT (not instanceof Unit -> WRONG_TYPE)
    public void testM12_TP1_getNoAddReasonNonUnit() {
        Goods goods = new Goods(game, null, grainType, 10);
        assertEquals(UnitLocation.NoAddReason.WRONG_TYPE, highSeas.getNoAddReason(goods));
    }

    // M12-TP2: ENTRY -> N0 -(B1:false)-> N1 -(B2:true)-> N3 -> N4 -> EXIT (Unit, not naval -> WRONG_TYPE)
    public void testM12_TP2_getNoAddReasonLandUnit() {
        Unit colonist = new ServerUnit(game, dutch.getEurope(), dutch, colonistType);
        assertEquals(UnitLocation.NoAddReason.WRONG_TYPE, highSeas.getNoAddReason(colonist));
    }

    // M12-TP3: ENTRY -> N0 -(B1:false)-> N1 -(B2:false)-> N2 -> N4 -> EXIT (Unit and naval -> NONE)
    public void testM12_TP3_getNoAddReasonNavalUnit() {
        Unit caravel = new ServerUnit(game, dutch.getEurope(), dutch, caravelType);
        assertEquals(UnitLocation.NoAddReason.NONE, highSeas.getNoAddReason(caravel));
    }

    // ------------------------------------------------------------------
    // M13: getLinkTarget(Player) — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M13-TP1: ENTRY -> N0 -> EXIT (returns player.getEurope())
    public void testM13_TP1_getLinkTarget() {
        assertSame(dutch.getEurope(), highSeas.getLinkTarget(dutch));
    }

    // ------------------------------------------------------------------
    // M14: copyIn(FreeColObject) — 3 feasible Test Paths (100% feasible C3)
    // ------------------------------------------------------------------

    // M14-TP1: ENTRY -> N0 -(B1:true)-> N2 -> EXIT (o == null when other is null or wrong type -> false)
    public void testM14_TP1_copyInNullOrWrongType() {
        assertFalse(highSeas.copyIn(null));
        assertFalse(highSeas.copyIn(dutch));
    }

    // M14-TP2: ENTRY -> N0 -(B1:false)-> N1 -(B2:true)-> N3 -> N4 -(B3:true)-> N6 -> EXIT (o has empty destinations)
    public void testM14_TP2_copyInEmptyDestinations() {
        Game game2 = getStandardGame();
        HighSeas other = new HighSeas(game2, highSeas.getId());
        assertTrue(highSeas.copyIn(other));
        assertTrue(highSeas.getDestinations().isEmpty());
    }

    // M14-TP3: ENTRY -> N0 -(B1:false)-> N1 -(B2:true)-> N3 -> N4 -(B3:false)-> N5 -> N4 -(B3:true)-> N6 -> EXIT (o has destinations)
    public void testM14_TP3_copyInWithDestinations() {
        Game game2 = getStandardGame();
        HighSeas other = new HighSeas(game2, highSeas.getId());
        other.addDestination(dutch.getEurope());
        assertTrue(highSeas.copyIn(other));
        assertEquals(1, highSeas.getDestinations().size());
    }

    // ------------------------------------------------------------------
    // M15: writeChildren(FreeColXMLWriter) — 2 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M15-TP1: ENTRY -> N0 -> N1 -(B1:true)-> N3 -> EXIT (destinations empty)
    public void testM15_TP1_writeChildrenEmpty() throws Exception {
        StringWriter sw = new StringWriter();
        try (FreeColXMLWriter xw = new FreeColXMLWriter(sw, FreeColXMLWriter.WriteScope.toSave())) {
            xw.writeStartElement(HighSeas.TAG);
            highSeas.writeChildren(xw);
            xw.writeEndElement();
        }
        assertFalse(sw.toString().contains("<destination"));
    }

    // M15-TP2: ENTRY -> N0 -> N1 -(B1:false)-> N2 -> N1 -(B1:true)-> N3 -> EXIT (destinations non-empty)
    public void testM15_TP2_writeChildrenWithDestinations() throws Exception {
        highSeas.addDestination(dutch.getEurope());
        StringWriter sw = new StringWriter();
        try (FreeColXMLWriter xw = new FreeColXMLWriter(sw, FreeColXMLWriter.WriteScope.toSave())) {
            xw.writeStartElement(HighSeas.TAG);
            highSeas.writeChildren(xw);
            xw.writeEndElement();
        }
        assertTrue(sw.toString().contains("<destination"));
    }

    // ------------------------------------------------------------------
    // M16: readChildren(FreeColXMLReader) — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M16-TP1: ENTRY -> N0 -> EXIT (clears destinations and delegates to super.readChildren)
    public void testM16_TP1_readChildren() throws Exception {
        highSeas.addDestination(dutch.getEurope());
        String xml = "<highSeas id=\"" + highSeas.getId() + "\"/>";
        try (FreeColXMLReader xr = new FreeColXMLReader(new StringReader(xml))) {
            xr.nextTag();
            highSeas.readChildren(xr);
        }
        assertTrue(highSeas.getDestinations().isEmpty());
    }

    // ------------------------------------------------------------------
    // M17: readChild(FreeColXMLReader) — 2 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M17-TP1: ENTRY -> N0 -(B1:false)-> N1 -> N3 -> EXIT (DESTINATION_TAG -> adds destination)
    public void testM17_TP1_readChildDestination() throws Exception {
        String xml = "<highSeas id=\"" + highSeas.getId() + "\">"
                + "<destination id=\"" + dutch.getEurope().getId() + "\"/>"
                + "</highSeas>";
        try (FreeColXMLReader xr = new FreeColXMLReader(new StringReader(xml))) {
            xr.nextTag();
            xr.nextTag();
            highSeas.readChild(xr);
        }
        assertEquals(1, highSeas.getDestinations().size());
        assertSame(dutch.getEurope(), highSeas.getDestinations().get(0));
    }

    // M17-TP2: ENTRY -> N0 -(B1:true)-> N2 -> N3 -> EXIT (non-destination tag -> delegates to super.readChild)
    public void testM17_TP2_readChildUnit() throws Exception {
        Unit caravel = new ServerUnit(game, dutch.getEurope(), dutch, caravelType);
        StringWriter sw = new StringWriter();
        try (FreeColXMLWriter xw = new FreeColXMLWriter(sw, FreeColXMLWriter.WriteScope.toSave())) {
            xw.writeStartElement(HighSeas.TAG);
            caravel.toXML(xw);
            xw.writeEndElement();
        }
        try (FreeColXMLReader xr = new FreeColXMLReader(new StringReader(sw.toString()))) {
            xr.nextTag();
            xr.nextTag();
            highSeas.readChild(xr);
        }
        assertEquals(1, highSeas.getUnitCount());
    }

    // ------------------------------------------------------------------
    // M18: getXMLTagName() — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M18-TP1: ENTRY -> N0 -> EXIT (returns TAG "highSeas")
    public void testM18_TP1_getXMLTagName() {
        assertEquals(HighSeas.TAG, highSeas.getXMLTagName());
    }

    // ------------------------------------------------------------------
    // M19: toString() — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M19-TP1: ENTRY -> N0 -> EXIT (returns getId())
    public void testM19_TP1_toString() {
        assertEquals("highSeas:test", highSeas.toString());
    }

    // ------------------------------------------------------------------
    // M20: <clinit>() — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M20-TP1: ENTRY -> N0 -> EXIT (initializes static logger and TAG)
    public void testM20_TP1_staticInitialization() {
        assertEquals("highSeas", HighSeas.TAG);
    }
}
