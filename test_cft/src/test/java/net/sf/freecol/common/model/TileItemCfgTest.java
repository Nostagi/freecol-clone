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
import java.util.stream.Stream;

import net.sf.freecol.common.io.FreeColXMLReader;
import net.sf.freecol.common.model.Constants.IntegrityType;
import net.sf.freecol.common.model.Map.Layer;
import net.sf.freecol.common.util.LogBuilder;
import net.sf.freecol.util.test.FreeColTestCase;


/**
 * CFG-driven test suite for {@link TileItem} derived from
 * {@code test_cft/graph/net/sf/freecol/common/model/TileItem.json}.
 *
 * Covers all 10 methods (M1..M10) and achieves 100% feasible C3 (node + edge)
 * coverage across 15 Test Paths.
 *
 * Infeasible edges documented:
 * - M10 ({@code copyIn}): {@code N1 -> N2} ({@code B2:false}) — {@code super.copyIn(o)}
 *   in {@link FreeColGameObject} and {@link FreeColObject} always returns {@code true}
 *   whenever {@code o != null} is a {@link TileItem}.
 */
public class TileItemCfgTest extends FreeColTestCase {

    private Game game;
    private Player dutch;
    private Tile tile1;
    private Tile tile2;
    private StubTileItem item;


    @Override
    public void setUp() throws Exception {
        super.setUp();
        // Pre-condition: standard game with map, Dutch player, two tiles, and a StubTileItem on tile1
        game = getGame();
        Map map = getTestMap(true);
        game.changeMap(map);
        dutch = game.getPlayerByNationId("model.nation.dutch");
        tile1 = map.getTile(1, 1);
        tile2 = map.getTile(2, 2);
        item = new StubTileItem(game, tile1);
    }


    // ------------------------------------------------------------------
    // Pre-condition helpers & stubs
    // ------------------------------------------------------------------

    private static final class StubTileItem extends TileItem {
        StubTileItem(Game game, Tile tile) {
            super(game, tile);
        }

        StubTileItem(Game game, FreeColXMLReader xr) {
            super(game, xr);
        }

        StubTileItem(Game game, String id) {
            super(game, id);
        }

        @Override
        public int getZIndex() {
            return 0;
        }

        @Override
        public boolean isTileTypeAllowed(TileType tileType) {
            return true;
        }

        @Override
        public int applyBonus(GoodsType goodsType, UnitType unitType, int potential) {
            return potential;
        }

        @Override
        public boolean canProduce(GoodsType goodsType, UnitType unitType) {
            return false;
        }

        @Override
        public Stream<Modifier> getProductionModifiers(GoodsType goodsType, UnitType unitType) {
            return Stream.empty();
        }

        @Override
        public boolean isNatural() {
            return false;
        }

        @Override
        public boolean isComplete() {
            return true;
        }

        @Override
        public Layer getLayer() {
            return Layer.RESOURCES;
        }

        @Override
        public String getNameKey() {
            return "stub.tileItem";
        }

        @Override
        public String getXMLTagName() {
            return "stubTileItem";
        }
    }


    // ------------------------------------------------------------------
    // M1: TileItem(Game, Tile) — 2 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M1-TP1: ENTRY -> N0 -(B1:true)-> N2 -> EXIT (tile != null)
    public void testM1_TP1_ConstructorWithTile() {
        StubTileItem created = new StubTileItem(game, tile1);
        assertSame(tile1, created.getTile());
    }

    // M1-TP2: ENTRY -> N0 -(B1:false)-> N1 -> EXIT (tile == null -> throws RuntimeException)
    public void testM1_TP2_ConstructorNullTileThrows() {
        try {
            new StubTileItem(game, (Tile) null);
            fail("Expected RuntimeException when tile is null");
        } catch (RuntimeException expected) {
            assertTrue(expected.getMessage().contains("Tile must not be null"));
        }
    }


    // ------------------------------------------------------------------
    // M2: TileItem(Game, FreeColXMLReader) — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M2-TP1: ENTRY -> N0 -> EXIT
    public void testM2_TP1_ConstructorWithXMLReader() throws Exception {
        try (FreeColXMLReader xr = new FreeColXMLReader(new StringReader("<stub/>"))) {
            StubTileItem created = new StubTileItem(game, xr);
            assertNotNull(created);
            assertNull(created.getTile());
        }
    }


    // ------------------------------------------------------------------
    // M3: TileItem(Game, String) — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M3-TP1: ENTRY -> N0 -> EXIT
    public void testM3_TP1_ConstructorWithId() {
        StubTileItem created = new StubTileItem(game, "tileItem:stub1");
        assertEquals("tileItem:stub1", created.getId());
    }


    // ------------------------------------------------------------------
    // M4: getLocation() — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M4-TP1: ENTRY -> N0 -> EXIT
    public void testM4_TP1_GetLocation() {
        assertSame(tile1, item.getLocation());
    }


    // ------------------------------------------------------------------
    // M5: setLocation(Location) — 3 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M5-TP1: ENTRY -> N0 -(B1:true)-> N2 -> EXIT (newLocation == null)
    public void testM5_TP1_SetLocationNull() {
        assertTrue(item.setLocation(null));
        assertNull(item.getTile());
    }

    // M5-TP2: ENTRY -> N0 -(B1:false)-> N1 -(B2:true)-> N3 -> EXIT (newLocation != null && !(newLocation instanceof Tile))
    public void testM5_TP2_SetLocationNonTileThrows() {
        Location europe = dutch.getEurope();
        try {
            item.setLocation(europe);
            fail("Expected RuntimeException when newLocation is not a Tile");
        } catch (RuntimeException expected) {
            assertTrue(expected.getMessage().contains("newLocation is not a Tile"));
        }
    }

    // M5-TP3: ENTRY -> N0 -(B1:false)-> N1 -(B2:false)-> N2 -> EXIT (newLocation instanceof Tile)
    public void testM5_TP3_SetLocationTile() {
        assertTrue(item.setLocation(tile2));
        assertSame(tile2, item.getTile());
    }


    // ------------------------------------------------------------------
    // M6: isInEurope() — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M6-TP1: ENTRY -> N0 -> EXIT
    public void testM6_TP1_IsInEurope() {
        assertFalse(item.isInEurope());
    }


    // ------------------------------------------------------------------
    // M7: getTile() — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M7-TP1: ENTRY -> N0 -> EXIT
    public void testM7_TP1_GetTile() {
        assertSame(tile1, item.getTile());
    }


    // ------------------------------------------------------------------
    // M8: getSpaceTaken() — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M8-TP1: ENTRY -> N0 -> EXIT
    public void testM8_TP1_GetSpaceTaken() {
        assertEquals(0, item.getSpaceTaken());
    }


    // ------------------------------------------------------------------
    // M9: checkIntegrity(boolean, LogBuilder) — 2 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M9-TP1: ENTRY -> N0 -(B1:true)-> N2 -> EXIT (tile != null)
    public void testM9_TP1_CheckIntegrityValidTile() {
        LogBuilder lb = new LogBuilder(64);
        assertEquals(IntegrityType.INTEGRITY_GOOD, item.checkIntegrity(false, lb));
    }

    // M9-TP2: ENTRY -> N0 -(B1:false)-> N1 -> N2 -> EXIT (tile == null)
    public void testM9_TP2_CheckIntegrityNullTile() {
        item.setLocation(null);
        LogBuilder lb = new LogBuilder(64);
        assertEquals(IntegrityType.INTEGRITY_FAIL, item.checkIntegrity(false, lb));
        assertTrue(lb.toString().contains("Tile item with no tile:"));
    }


    // ------------------------------------------------------------------
    // M10: copyIn(FreeColObject) — 2 Test Paths (100% feasible C3; N1->N2 infeasible)
    // ------------------------------------------------------------------

    // M10-TP1: ENTRY -> N0 -(B1:true)-> N2 -> EXIT (other is null or not TileItem)
    public void testM10_TP1_CopyInNullOrWrongType() {
        assertFalse(item.copyIn(null));
        assertFalse(item.copyIn(tile2));
    }

    // M10-TP2: ENTRY -> N0 -(B1:false)-> N1 -(B2:true)-> N3 -> EXIT (other is TileItem with matching id)
    public void testM10_TP2_CopyInTileItem() {
        Game otherGame = new net.sf.freecol.server.model.ServerGame(spec());
        StubTileItem source = new StubTileItem(otherGame, item.getId());
        source.setLocation(tile2);
        assertTrue(item.copyIn(source));
        assertSame(tile2, item.getTile());
    }
}
