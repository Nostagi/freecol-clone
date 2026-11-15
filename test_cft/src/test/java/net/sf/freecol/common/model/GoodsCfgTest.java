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

import javax.xml.stream.XMLStreamException;

import net.sf.freecol.common.io.FreeColXMLReader;
import net.sf.freecol.common.io.FreeColXMLWriter;
import net.sf.freecol.server.model.ServerUnit;
import net.sf.freecol.util.test.FreeColTestCase;


/**
 * CFG-driven test suite for {@link Goods} derived from
 * {@code test_cft/graph/net/sf/freecol/common/model/Goods.json}.
 *
 * Covers all 23 methods (M1..M23) and achieves 100% feasible C3 (node + edge)
 * coverage across 40 Test Paths.
 *
 * Infeasible edges documented:
 * - M18 ({@code copyIn}): {@code N1 -> N2} ({@code B2:false}) — once {@code o} is non-null
 *   ({@code B1:false}), {@code o} is guaranteed to be a {@code Goods} instance (subclass of
 *   {@code AbstractGoods} and {@code FreeColObject}), so {@code super.copyIn(o)} always
 *   returns {@code true}.
 */
public class GoodsCfgTest extends FreeColTestCase {

    private static final GoodsType GRAIN_TYPE = spec().getGoodsType("model.goods.grain");
    private static final GoodsType SUGAR_TYPE = spec().getGoodsType("model.goods.sugar");
    private static final UnitType GALLEON_TYPE = spec().getUnitType("model.unit.galleon");

    private Game game;
    private Colony colony;
    private Tile tile;
    private Player dutch;
    private Goods goods;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        // Pre-condition: Initialize standard Game, Colony, Tile, Player, and Goods.
        game = getStandardGame();
        game.setMap(getTestMap());
        colony = createStandardColony(1);
        tile = colony.getTile();
        dutch = colony.getOwner();
        goods = new Goods(game, colony, GRAIN_TYPE, 50);
    }

    // ------------------------------------------------------------------
    // Pre-condition helpers & stubs
    // ------------------------------------------------------------------

    private String writeAttributesToXml(Goods g) throws Exception {
        StringWriter sw = new StringWriter();
        try (FreeColXMLWriter xw = new FreeColXMLWriter(
                sw, FreeColXMLWriter.WriteScope.toSave(), false)) {
            xw.writeStartDocument("UTF-8", "1.0");
            xw.writeStartElement(Goods.TAG);
            g.writeAttributes(xw);
            xw.writeEndElement();
            xw.writeEndDocument();
        }
        return sw.toString();
    }

    // ------------------------------------------------------------------
    // M1: Goods(Game) (private) — 2 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M1-TP1: ENTRY -> N0 -(B1:false)-> N1 -> EXIT (game == null -> throws RuntimeException)
    public void testM1_TP1_PrivateConstructorNullGame() {
        try {
            new Goods((Game) null, "goods:null");
            fail("Expected RuntimeException for null game");
        } catch (RuntimeException expected) {
            assertTrue(expected.getMessage().contains("Null game"));
        }
    }

    // M1-TP2: ENTRY -> N0 -(B1:true)-> N2 -> EXIT (game != null)
    public void testM1_TP2_PrivateConstructorValidGame() {
        Goods g = new Goods(game, "goods:ok");
        assertEquals(game, g.getGame());
    }

    // ------------------------------------------------------------------
    // M2: Goods(Game, String) — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M2-TP1: ENTRY -> N0 -> EXIT (construct Goods with game and ignored id)
    public void testM2_TP1_ConstructorGameId() {
        Goods g = new Goods(game, "ignored");
        assertEquals(game, g.getGame());
    }

    // ------------------------------------------------------------------
    // M3: Goods(Game, Location, GoodsType, int) — 4 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M3-TP1: ENTRY -> N0 -(B1:false)-> N1 -> EXIT (type == null -> throws RuntimeException)
    public void testM3_TP1_ConstructorNullType() {
        try {
            new Goods(game, colony, null, 10);
            fail("Expected RuntimeException for null type");
        } catch (RuntimeException expected) {
            assertTrue(expected.getMessage().contains("Null type"));
        }
    }

    // M3-TP2: ENTRY -> N0 -(B1:true)-> N2 -(B2:false)-> N3 -(B3:false)-> N4 -> EXIT (location has no GoodsContainer)
    public void testM3_TP2_ConstructorLocationWithoutGoodsContainer() {
        try {
            new Goods(game, tile, GRAIN_TYPE, 10);
            fail("Expected RuntimeException for location without GoodsContainer");
        } catch (RuntimeException expected) {
            assertTrue(expected.getMessage().contains("Can not store goods at"));
        }
    }

    // M3-TP3: ENTRY -> N0 -(B1:true)-> N2 -(B2:true)-> N5 -> EXIT (location == null)
    public void testM3_TP3_ConstructorNullLocation() {
        Goods g = new Goods(game, null, GRAIN_TYPE, 25);
        assertNull(g.getLocation());
        assertEquals(GRAIN_TYPE, g.getType());
        assertEquals(25, g.getAmount());
    }

    // M3-TP4: ENTRY -> N0 -(B1:true)-> N2 -(B2:false)-> N3 -(B3:true)-> N5 -> EXIT (valid location with GoodsContainer)
    public void testM3_TP4_ConstructorValidLocation() {
        Goods g = new Goods(game, colony, GRAIN_TYPE, 30);
        assertEquals(colony, g.getLocation());
        assertEquals(30, g.getAmount());
    }

    // ------------------------------------------------------------------
    // M4: Goods(Game, FreeColXMLReader) — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M4-TP1: ENTRY -> N0 -> EXIT (construct Goods from XML reader)
    public void testM4_TP1_ConstructorXMLReader() throws Exception {
        String xml = writeAttributesToXml(goods);
        try (FreeColXMLReader xr = new FreeColXMLReader(new StringReader(xml))) {
            xr.nextTag();
            Goods parsed = new Goods(game, xr);
            assertEquals(GRAIN_TYPE, parsed.getType());
            assertEquals(50, parsed.getAmount());
            assertEquals(colony, parsed.getLocation());
        }
    }

    // ------------------------------------------------------------------
    // M5: adjustAmount() — 4 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M5-TP1: ENTRY -> N0 -(B1:false)-> N1 -> EXIT (location == null)
    public void testM5_TP1_AdjustAmountNullLocation() {
        Goods g = new Goods(game, null, GRAIN_TYPE, 100);
        g.adjustAmount();
        assertEquals(100, g.getAmount());
    }

    // M5-TP2: ENTRY -> N0 -(B1:true)-> N2 -(B2:true)-> N5 -> EXIT (location.getGoodsContainer() == null)
    public void testM5_TP2_AdjustAmountNullGoodsContainer() {
        Goods g = new Goods(game, null, GRAIN_TYPE, 100);
        g.setLocation(tile);
        g.adjustAmount();
        assertEquals(100, g.getAmount());
    }

    // M5-TP3: ENTRY -> N0 -(B1:true)-> N2 -(B2:false)-> N3 -(B3:true)-> N5 -> EXIT (amount <= maxAmount)
    public void testM5_TP3_AdjustAmountWithinMax() {
        colony.addGoods(GRAIN_TYPE, 80);
        Goods g = new Goods(game, colony, GRAIN_TYPE, 50);
        g.adjustAmount();
        assertEquals(50, g.getAmount());
    }

    // M5-TP4: ENTRY -> N0 -(B1:true)-> N2 -(B2:false)-> N3 -(B3:false)-> N4 -> N5 -> EXIT (amount > maxAmount)
    public void testM5_TP4_AdjustAmountExceedsMax() {
        colony.removeGoods(GRAIN_TYPE);
        colony.addGoods(GRAIN_TYPE, 20);
        Goods g = new Goods(game, colony, GRAIN_TYPE, 50);
        g.adjustAmount();
        assertEquals(20, g.getAmount());
    }

    // ------------------------------------------------------------------
    // M6: getLocation() — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M6-TP1: ENTRY -> N0 -> EXIT (return location)
    public void testM6_TP1_GetLocation() {
        assertEquals(colony, goods.getLocation());
    }

    // ------------------------------------------------------------------
    // M7: setLocation(Location) — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M7-TP1: ENTRY -> N0 -> EXIT (set location and return true)
    public void testM7_TP1_SetLocation() {
        assertTrue(goods.setLocation(null));
        assertNull(goods.getLocation());
    }

    // ------------------------------------------------------------------
    // M8: isInEurope() — 4 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M8-TP1: ENTRY -> N0 -(B1:true)-> N3 -> N5 -> EXIT (location instanceof Europe -> true)
    public void testM8_TP1_IsInEuropeDirect() {
        Europe europe = dutch.getEurope();
        goods.setLocation(europe);
        assertTrue(goods.isInEurope());
    }

    // M8-TP2: ENTRY -> N0 -(B1:false)-> N1 -(B2:true)-> N4 -> N5 -> EXIT (location is Colony -> false)
    public void testM8_TP2_IsInEuropeColony() {
        assertFalse(goods.isInEurope());
    }

    // M8-TP3: ENTRY -> N0 -(B1:false)-> N1 -(B2:false)-> N2 -(B3:true)-> N4 -> N5 -> EXIT (location is Unit not in Europe -> false)
    public void testM8_TP3_IsInEuropeUnitOnMap() {
        Unit ship = new ServerUnit(game, tile, dutch, GALLEON_TYPE);
        goods.setLocation(ship);
        assertFalse(goods.isInEurope());
    }

    // M8-TP4: ENTRY -> N0 -(B1:false)-> N1 -(B2:false)-> N2 -(B3:false)-> N3 -> N5 -> EXIT (location is Unit in Europe -> true)
    public void testM8_TP4_IsInEuropeUnitInEurope() {
        Unit ship = new ServerUnit(game, dutch.getEurope(), dutch, GALLEON_TYPE);
        goods.setLocation(ship);
        assertTrue(goods.isInEurope());
    }

    // ------------------------------------------------------------------
    // M9: getTile() — 2 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M9-TP1: ENTRY -> N0 -(B1:false)-> N1 -> N3 -> EXIT (location == null -> null)
    public void testM9_TP1_GetTileNullLocation() {
        goods.setLocation(null);
        assertNull(goods.getTile());
    }

    // M9-TP2: ENTRY -> N0 -(B1:true)-> N2 -> N3 -> EXIT (location != null -> location.getTile())
    public void testM9_TP2_GetTileNonNullLocation() {
        assertEquals(tile, goods.getTile());
    }

    // ------------------------------------------------------------------
    // M10: getSpaceTaken() — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M10-TP1: ENTRY -> N0 -> EXIT (return 1)
    public void testM10_TP1_GetSpaceTaken() {
        assertEquals(1, goods.getSpaceTaken());
    }

    // ------------------------------------------------------------------
    // M11: getOwner() — 2 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M11-TP1: ENTRY -> N0 -(B1:false)-> N1 -> N3 -> EXIT (location is Ownable -> owner)
    public void testM11_TP1_GetOwnerOwnableLocation() {
        assertEquals(dutch, goods.getOwner());
    }

    // M11-TP2: ENTRY -> N0 -(B1:true)-> N2 -> N3 -> EXIT (location not Ownable -> null)
    public void testM11_TP2_GetOwnerNullLocation() {
        goods.setLocation(null);
        assertNull(goods.getOwner());
    }

    // ------------------------------------------------------------------
    // M12: setOwner(Player) — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M12-TP1: ENTRY -> N0 -> EXIT (throws UnsupportedOperationException)
    public void testM12_TP1_SetOwner() {
        try {
            goods.setOwner(dutch);
            fail("Expected UnsupportedOperationException");
        } catch (UnsupportedOperationException expected) {
            // Expected
        }
    }

    // ------------------------------------------------------------------
    // M13: getSpecification() — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M13-TP1: ENTRY -> N0 -> EXIT (return game.getSpecification())
    public void testM13_TP1_GetSpecification() {
        assertEquals(spec(), goods.getSpecification());
    }

    // ------------------------------------------------------------------
    // M14: setSpecification(Specification) — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M14-TP1: ENTRY -> N0 -> EXIT (throws RuntimeException)
    public void testM14_TP1_SetSpecification() {
        try {
            goods.setSpecification(spec());
            fail("Expected RuntimeException");
        } catch (RuntimeException expected) {
            assertTrue(expected.getMessage().contains("Can not set specification"));
        }
    }

    // ------------------------------------------------------------------
    // M15: getGame() — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M15-TP1: ENTRY -> N0 -> EXIT (return game)
    public void testM15_TP1_GetGame() {
        assertEquals(game, goods.getGame());
    }

    // ------------------------------------------------------------------
    // M16: setGame(Game) — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M16-TP1: ENTRY -> N0 -> EXIT (throws RuntimeException)
    public void testM16_TP1_SetGame() {
        try {
            goods.setGame(game);
            fail("Expected RuntimeException");
        } catch (RuntimeException expected) {
            assertTrue(expected.getMessage().contains("Can not set game"));
        }
    }

    // ------------------------------------------------------------------
    // M17: getDisplayObject() — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M17-TP1: ENTRY -> N0 -> EXIT (return GoodsType)
    public void testM17_TP1_GetDisplayObject() {
        assertEquals(GRAIN_TYPE, goods.getDisplayObject());
    }

    // ------------------------------------------------------------------
    // M18: copyIn(T) — 2 Test Paths (100% feasible C3; N1->N2 infeasible)
    // ------------------------------------------------------------------

    // M18-TP1: ENTRY -> N0 -(B1:true)-> N2 -> EXIT (other == null -> false)
    public void testM18_TP1_CopyInNull() {
        assertFalse(goods.copyIn(null));
    }

    // M18-TP2: ENTRY -> N0 -(B1:false)-> N1 -(B2:true)-> N3 -> EXIT (valid Goods -> true)
    public void testM18_TP2_CopyInValid() {
        Goods other = new Goods(game, null, GRAIN_TYPE, 15);
        assertTrue(goods.copyIn(other));
        assertEquals(GRAIN_TYPE, goods.getType());
        assertEquals(15, goods.getAmount());
        assertNull(goods.getLocation());
    }

    // ------------------------------------------------------------------
    // M19: writeAttributes(FreeColXMLWriter) — 2 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M19-TP1: ENTRY -> N0 -(B1:true)-> N2 -> EXIT (location == null)
    public void testM19_TP1_WriteAttributesNullLocation() throws Exception {
        Goods g = new Goods(game, null, GRAIN_TYPE, 10);
        String xml = writeAttributesToXml(g);
        assertTrue(xml.contains("type="));
        assertFalse(xml.contains("location="));
    }

    // M19-TP2: ENTRY -> N0 -(B1:false)-> N1 -> N2 -> EXIT (location != null)
    public void testM19_TP2_WriteAttributesWithLocation() throws Exception {
        String xml = writeAttributesToXml(goods);
        assertTrue(xml.contains("location="));
    }

    // ------------------------------------------------------------------
    // M20: readAttributes(FreeColXMLReader) — 2 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M20-TP1: ENTRY -> N0 -(B1:false)-> N1 -> EXIT (missing type -> throws XMLStreamException)
    public void testM20_TP1_ReadAttributesNullType() throws Exception {
        String xml = "<goods amount=\"10\"/>";
        try (FreeColXMLReader xr = new FreeColXMLReader(new StringReader(xml))) {
            xr.nextTag();
            Goods g = new Goods(game, "ignored");
            try {
                g.readAttributes(xr);
                fail("Expected XMLStreamException for missing goods type");
            } catch (XMLStreamException expected) {
                assertTrue(expected.getMessage().contains("Null goods type"));
            }
        }
    }

    // M20-TP2: ENTRY -> N0 -(B1:true)-> N2 -> EXIT (valid type, amount, location)
    public void testM20_TP2_ReadAttributesValid() throws Exception {
        String xml = writeAttributesToXml(goods);
        Goods parsed = new Goods(game, "ignored");
        try (FreeColXMLReader xr = new FreeColXMLReader(new StringReader(xml))) {
            xr.nextTag();
            parsed.readAttributes(xr);
        }
        assertEquals(GRAIN_TYPE, parsed.getType());
        assertEquals(50, parsed.getAmount());
        assertEquals(colony, parsed.getLocation());
    }

    // ------------------------------------------------------------------
    // M21: getXMLTagName() — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M21-TP1: ENTRY -> N0 -> EXIT (return Goods.TAG)
    public void testM21_TP1_GetXMLTagName() {
        assertEquals(Goods.TAG, goods.getXMLTagName());
    }

    // ------------------------------------------------------------------
    // M22: equals(Object) — 4 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M22-TP1: ENTRY -> N0 -(B1:true)-> N6 -> EXIT (o not instanceof Goods -> false)
    public void testM22_TP1_EqualsNonGoods() {
        assertFalse(goods.equals("notGoods"));
    }

    // M22-TP2: ENTRY -> N0 -(B1:false)-> N1 -(B2:true)-> N4 -> N5 -> EXIT (different location -> false)
    public void testM22_TP2_EqualsDifferentLocation() {
        Goods other = new Goods(game, null, GRAIN_TYPE, 50);
        assertFalse(goods.equals(other));
    }

    // M22-TP3: ENTRY -> N0 -(B1:false)-> N1 -(B2:false)-> N2 -(B3:true)-> N4 -> N5 -> EXIT (same location, different super fields -> false)
    public void testM22_TP3_EqualsDifferentAmount() {
        Goods other = new Goods(game, colony, GRAIN_TYPE, 10);
        assertFalse(goods.equals(other));
    }

    // M22-TP4: ENTRY -> N0 -(B1:false)-> N1 -(B2:false)-> N2 -(B3:false)-> N3 -> N5 -> EXIT (same location and super fields -> true)
    public void testM22_TP4_EqualsTrue() {
        Goods other = new Goods(game, colony, GRAIN_TYPE, 50);
        assertTrue(goods.equals(other));
    }

    // ------------------------------------------------------------------
    // M23: hashCode() — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M23-TP1: ENTRY -> N0 -> EXIT (consistent hashCode)
    public void testM23_TP1_HashCode() {
        Goods other = new Goods(game, colony, GRAIN_TYPE, 50);
        assertEquals(goods.hashCode(), other.hashCode());
    }
}
