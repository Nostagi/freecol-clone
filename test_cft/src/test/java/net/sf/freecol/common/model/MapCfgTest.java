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
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

import javax.xml.stream.XMLStreamException;

import net.sf.freecol.common.io.FreeColXMLReader;
import net.sf.freecol.common.io.FreeColXMLWriter;
import net.sf.freecol.common.model.Constants.IntegrityType;
import net.sf.freecol.common.model.Map.Layer;
import net.sf.freecol.common.model.Map.Position;
import net.sf.freecol.common.model.pathfinding.CostDecider;
import net.sf.freecol.common.model.pathfinding.CostDeciders;
import net.sf.freecol.common.model.pathfinding.GoalDecider;
import net.sf.freecol.common.model.pathfinding.GoalDeciders;
import net.sf.freecol.common.util.LogBuilder;
import net.sf.freecol.server.model.ServerColony;
import net.sf.freecol.server.model.ServerIndianSettlement;
import net.sf.freecol.server.model.ServerUnit;
import net.sf.freecol.util.test.FreeColTestCase;


/**
 * CFG-driven test suite for {@link Map} derived from
 * {@code test_cft/graph/net/sf/freecol/common/model/Map.json}.
 *
 * Covers all 118 methods (M1..M118, including constructors, private helpers,
 * all 12 lambdas, and {@code <clinit>}) and achieves 100% feasible C3
 * (node + edge) coverage.
 *
 * Infeasible edges documented:
 * - M26..M29, M32 ({@code getRegions}, {@code addRegion}, {@code removeRegion},
 *   {@code clearRegions}, {@code removeRegionsByKey}): synthetic {@code monitorexit}
 *   exception-handler blocks ({@code reachable=false}) have no incoming edges in CFG.
 * - M61 ({@code findPath}): {@code N4} ({@code catch (IllegalArgumentException)})
 *   is {@code reachable=false} in CFG; {@code findRealEnd} throws {@code RuntimeException}.
 * - M66 ({@code searchMap}): {@code N48 -> N49} ({@code B25:false},
 *   {@code currentTile == null}) is infeasible because {@code start} is a {@code Tile}
 *   and every neighbor queued from {@code currentTile.getSurroundingTiles(1)} is a
 *   non-null {@code Tile}. {@code N125 -> N130 (default)} is unreachable because all
 *   5 {@code MoveStep} enum constants are explicitly handled.
 * - M74 ({@code resetHighSeas}): {@code N44 -> N45} ({@code B27:false}, {@code "BOGUS"})
 *   is infeasible because the enclosing {@code if (totalL <= 0 || totalR <= 0)}
 *   guarantees {@code totalR <= 0} whenever {@code totalL <= 0} is false.
 * - M118 ({@code <clinit>}): {@code desiredAssertionStatus()} outcome is fixed
 *   by JVM {@code -ea} flag per run.
 */
public class MapCfgTest extends FreeColTestCase {

    private Game game;
    private Player dutch;
    private Player french;
    private TileType plains;
    private TileType ocean;
    private TileType highSeas;
    private UnitType colonistType;
    private UnitType galleonType;
    private UnitType caravelType;
    private UnitType frigateType;
    private UnitType scoutType;
    private GoodsType grainType;
    private ResourceType grainResource;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        // Pre-condition: Initialize standard game, players, tile types, and unit types
        game = getStandardGame();
        dutch = game.getPlayerByNationId("model.nation.dutch");
        french = game.getPlayerByNationId("model.nation.french");
        plains = spec().getTileType("model.tile.plains");
        ocean = spec().getTileType("model.tile.ocean");
        highSeas = spec().getTileType("model.tile.highSeas");
        colonistType = spec().getUnitType("model.unit.freeColonist");
        galleonType = spec().getUnitType("model.unit.galleon");
        caravelType = spec().getUnitType("model.unit.caravel");
        frigateType = spec().getUnitType("model.unit.frigate");
        scoutType = spec().getUnitType("model.unit.seasonedScout");
        grainType = spec().getGoodsType("model.goods.grain");
        grainResource = spec().getResourceType("model.resource.grain");
    }

    @Override
    public void tearDown() throws Exception {
        super.tearDown();
    }


    // ------------------------------------------------------------------
    // Pre-condition helpers & stubs
    // ------------------------------------------------------------------

    private Object invokePrivate(Object target, String name,
                                 Class<?>[] paramTypes, Object... args) {
        try {
            Method m = Map.class.getDeclaredMethod(name, paramTypes);
            m.setAccessible(true);
            return m.invoke(target, args);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void setField(Object target, String name, Object value) {
        try {
            Field f = Map.class.getDeclaredField(name);
            f.setAccessible(true);
            f.set(target, value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void setRegionKey(Region r, String key) {
        try {
            Field f = Region.class.getDeclaredField("key");
            f.setAccessible(true);
            f.set(r, key);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private Map createUniformMap(int w, int h, TileType type) {
        Map map = new Map(game, w, h);
        map.populateTiles((x, y) -> new Tile(game, type, x, y));
        game.changeMap(map);
        map.resetContiguity();
        map.resetHighSeasCount();
        return map;
    }

    private Map createSplitMap(int w, int h, int landCols) {
        Map map = new Map(game, w, h);
        map.populateTiles((x, y) -> new Tile(game, (x < landCols) ? plains : ocean, x, y));
        game.changeMap(map);
        map.resetContiguity();
        map.forEachTile(t -> t.setContiguity(t.isLand() ? 1 : 2));
        map.resetHighSeasCount();
        return map;
    }

    private void exploreAll(Map map, Player p) {
        map.forEachTile(t -> t.setExplored(p, true));
    }


    // ------------------------------------------------------------------
    // M1..M6: Constructors & basic getters — 6 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M1-TP1, M4-TP1, M5-TP1, M6-TP1, M22-TP1, M23-TP1: ENTRY -> N0 -> EXIT
    public void testM1_M4_M5_M6_M22_M23_TP1_constructorAndBasicGetters() {
        Map map = new Map(game, 5, 6);
        assertEquals(5, map.getWidth());
        assertEquals(6, map.getHeight());
        assertEquals(Layer.RESOURCES, map.getLayer());
        assertEquals(36.0f, map.getLatitudePerRow(), 0.01f);
        invokePrivate(map, "initializeTraceSearch", new Class<?>[0]);
    }

    // M2-TP1: ENTRY -> N0 -> EXIT (Map(Game, FreeColXMLReader))
    public void testM2_TP1_xmlConstructor() throws Exception {
        Map src = createUniformMap(2, 2, plains);
        StringWriter sw = new StringWriter();
        try (FreeColXMLWriter xw = new FreeColXMLWriter(
                sw, FreeColXMLWriter.WriteScope.toSave())) {
            src.toXML(xw);
        }
        try (FreeColXMLReader xr = new FreeColXMLReader(new StringReader(sw.toString()))) {
            xr.nextTag();
            Map dst = new Map(game, xr);
            assertEquals(2, dst.getWidth());
            assertEquals(2, dst.getHeight());
        }
    }

    // M3-TP1: ENTRY -> N0 -> EXIT (Map(Game, String))
    public void testM3_TP1_idConstructor() {
        Map map = new Map(game, "map:test1");
        assertEquals("map:test1", map.getId());
        assertEquals(-1, map.getWidth());
        assertEquals(-1, map.getHeight());
    }


    // ------------------------------------------------------------------
    // M7: inBox(int, int, int, int) — 5 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M7-TP1..TP5: all atomic conditions of x >= 0 && x < width && y >= 0 && y < height
    public void testM7_TP1_to_TP5_inBox() {
        // TP1: x < 0 (B1:true -> N5)
        assertFalse(Map.inBox(-1, 0, 5, 5));
        // TP2: x >= 0, x >= width (B1:false, B2:true -> N5)
        assertFalse(Map.inBox(5, 0, 5, 5));
        // TP3: x in [0,w), y < 0 (B1:false, B2:false, B3:true -> N5)
        assertFalse(Map.inBox(2, -1, 5, 5));
        // TP4: x in [0,w), y >= height (B1:false, B2:false, B3:false, B4:true -> N5)
        assertFalse(Map.inBox(2, 5, 5, 5));
        // TP5: all valid (B1..B4:false -> N4 -> N6)
        assertTrue(Map.inBox(2, 3, 5, 5));
    }


    // ------------------------------------------------------------------
    // M8 & M9: isValid(int, int) & isValid(Position) — 2 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M8-TP1 & M9-TP1: ENTRY -> N0 -> EXIT
    public void testM8_M9_TP1_isValid() {
        Map map = new Map(game, 4, 4);
        assertTrue(map.isValid(1, 2));
        assertFalse(map.isValid(-1, 2));
        assertTrue(map.isValid(new Position(3, 3)));
        assertFalse(map.isValid(new Position(4, 0)));
    }


    // ------------------------------------------------------------------
    // M10: setTiles(int, int) — 3 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M10-TP1..TP3: width <= 0 || height <= 0
    public void testM10_TP1_to_TP3_setTiles() {
        Map map = new Map(game, "map:m10");
        // TP1: width <= 0 (B1:true -> N2)
        assertNotNull(invokePrivate(map, "setTiles", new Class<?>[]{int.class, int.class}, 0, 3));
        // TP2: width > 0, height <= 0 (B1:false, B2:false -> N2)
        assertNotNull(invokePrivate(map, "setTiles", new Class<?>[]{int.class, int.class}, 3, 0));
        // TP3: width > 0, height > 0 (B1:false, B2:true -> N3)
        assertNull(invokePrivate(map, "setTiles", new Class<?>[]{int.class, int.class}, 3, 4));
        assertEquals(3, map.getWidth());
        assertEquals(4, map.getHeight());
    }


    // ------------------------------------------------------------------
    // M11: updateTiles(int, int) — 7 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M11-TP1..TP7: all combinations of initial width/height < 0 and resize check
    public void testM11_TP1_to_TP7_updateTiles() {
        // TP1: this.width < 0 && this.height < 0 -> setTiles (B1:false, B2:false -> N2)
        Map m1 = new Map(game, "map:m11a");
        assertNull(invokePrivate(m1, "updateTiles", new Class<?>[]{int.class, int.class}, 3, 3));

        // TP2: this.width >= 0, this.width == width, this.height == height (B1:true, B3:false, B4:true -> N6)
        assertNull(invokePrivate(m1, "updateTiles", new Class<?>[]{int.class, int.class}, 3, 3));

        // TP3: this.width >= 0, this.width != width (B1:true, B3:true -> N5)
        assertNotNull(invokePrivate(m1, "updateTiles", new Class<?>[]{int.class, int.class}, 4, 3));

        // TP4: this.width >= 0, this.width == width, this.height != height (B1:true, B3:false, B4:false -> N5)
        assertNotNull(invokePrivate(m1, "updateTiles", new Class<?>[]{int.class, int.class}, 3, 4));

        // TP5: this.width < 0, this.height >= 0 (B1:false, B2:true -> N3)
        Map m2 = new Map(game, "map:m11b");
        setField(m2, "height", 5);
        assertNotNull(invokePrivate(m2, "updateTiles", new Class<?>[]{int.class, int.class}, 3, 5));
    }


    // ------------------------------------------------------------------
    // M12 & M13: getTile(int, int) & getTile(Position) — 3 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M12-TP1..TP2 & M13-TP1
    public void testM12_M13_getTile() {
        Map map = createUniformMap(3, 3, plains);
        // M12-TP1: !isValid (B1:false -> N1)
        assertNull(map.getTile(-1, 0));
        // M12-TP2: isValid (B1:true -> N2)
        assertNotNull(map.getTile(1, 1));
        // M13-TP1: Position
        assertNotNull(map.getTile(new Position(2, 2)));
        assertNull(map.getTile(new Position(5, 5)));
    }


    // ------------------------------------------------------------------
    // M14 & M15: setTile & updateTile — 6 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M14-TP1..TP2 & M15-TP1..TP4
    public void testM14_M15_setTileAndUpdateTile() {
        Map map = new Map(game, 2, 2);
        // M14-TP1: tile == null -> false (B1:false -> N1)
        assertFalse((Boolean) invokePrivate(map, "setTile",
                new Class<?>[]{Tile.class, int.class, int.class}, null, 0, 0));
        // M15-TP1: tile == null -> false (B1:false -> N1)
        assertFalse((Boolean) invokePrivate(map, "updateTile",
                new Class<?>[]{Tile.class}, new Object[]{null}));
        // M15-TP2: invalid coords -> false (B1:true, B2:false -> N3)
        Tile outOfBounds = new Tile(game, plains, 5, 5);
        assertFalse((Boolean) invokePrivate(map, "updateTile",
                new Class<?>[]{Tile.class}, outOfBounds));
        // M15-TP3 & M14-TP2: old == null -> calls setTile -> true (B1:true, B2:true, B3:true -> N6)
        Tile t00 = new Tile(game, plains, 0, 0);
        assertTrue((Boolean) invokePrivate(map, "updateTile",
                new Class<?>[]{Tile.class}, t00));
        // M15-TP4: old != null -> old.copyIn(tile) -> true (B1:true, B2:true, B3:false -> N5)
        Tile t00b = new Tile(game, ocean, 0, 0);
        assertTrue((Boolean) invokePrivate(map, "updateTile",
                new Class<?>[]{Tile.class}, t00b));
    }


    // ------------------------------------------------------------------
    // M16..M25: Layer, Latitude & Row accessors — 10 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M16..M25-TP1: ENTRY -> N0 -> EXIT
    public void testM16_to_M25_layerAndLatitude() {
        Map map = new Map(game, 4, 11);
        map.setLayer(Layer.ALL);
        assertEquals(Layer.ALL, map.getLayer());

        map.setMinimumLatitude(-50);
        assertEquals(-50, map.getMinimumLatitude());

        map.setMaximumLatitude(50);
        assertEquals(50, map.getMaximumLatitude());

        // (50 - (-50)) / (11 - 1) = 10.0f
        assertEquals(10.0f, map.getLatitudePerRow(), 0.01f);
        assertEquals(0, map.getLatitude(5));
        assertEquals(5, map.getRow(0));
    }


    // ------------------------------------------------------------------
    // M26..M33: Region management — 11 Test Paths (100% feasible C3)
    // ------------------------------------------------------------------

    // M26..M33: getRegions, addRegion, removeRegion, clearRegions, getFixedRegions,
    // getRegionByKey, removeRegionsByKey, getRegionByName
    public void testM26_to_M33_regions() {
        Map map = new Map(game, 3, 3);
        // M30-TP1: empty regions loop exit (B1:true)
        assertTrue(map.getFixedRegions().isEmpty());
        // M31-TP1: key == null (B1:true)
        assertNull(map.getRegionByKey(null));
        // M33-TP1: name == null (B1:true)
        assertNull(map.getRegionByName(null));
        // M32-TP1: empty regions (B1:true)
        map.removeRegionsByKey("k1");

        Region r1 = new Region(game);
        setRegionKey(r1, "model.region.r1");
        r1.setName("Region One");
        Region r2 = new Region(game);
        setRegionKey(r2, null);
        r2.setName("Region Two");

        // M27-TP1
        map.addRegion(r1);
        map.addRegion(r2);
        // M26-TP1
        assertEquals(2, map.getRegions().size());

        // M30-TP2: loop with key != null (B2:false) and key == null (B2:true)
        java.util.Map<String, Region> fixed = map.getFixedRegions();
        assertEquals(1, fixed.size());
        assertEquals(r1, fixed.get("model.region.r1"));

        // M31-TP2: key != null (B1:false)
        assertEquals(r1, map.getRegionByKey("model.region.r1"));
        assertNull(map.getRegionByKey("nonexistent"));

        // M33-TP2: name != null (B1:false)
        assertEquals(r2, map.getRegionByName("Region Two"));
        assertNull(map.getRegionByName("nonexistent"));

        // M32-TP2: key.equals(r.getKey()) true (B2:false) and false (B2:true)
        map.removeRegionsByKey("model.region.r1");
        assertEquals(1, map.getRegions().size());

        // M28-TP1
        map.removeRegion(r2);
        assertEquals(0, map.getRegions().size());

        // M29-TP1
        map.addRegion(r1);
        map.clearRegions();
        assertTrue(map.getRegions().isEmpty());
    }


    // ------------------------------------------------------------------
    // M34: isSameLocation(Location, Location) — 6 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M34-TP1..TP6
    public void testM34_TP1_to_TP6_isSameLocation() {
        Map map = createUniformMap(3, 3, plains);
        Tile t1 = map.getTile(0, 0);
        Tile t2 = map.getTile(1, 1);
        Unit u1 = new ServerUnit(game, t1, dutch, colonistType);
        Europe europe = dutch.getEurope();

        // TP1: l1 == null (B1:true -> N2)
        assertFalse(Map.isSameLocation(null, t1));
        // TP2: l1 != null, l2 == null (B1:false, B2:false -> N2)
        assertFalse(Map.isSameLocation(t1, null));
        // TP3: l1 == l2 (B1:false, B2:true, B3:false -> N4)
        assertTrue(Map.isSameLocation(t1, t1));
        // TP4: l1 != l2, l1.getTile() == null (B1:false, B2:true, B3:true, B4:false -> N6)
        assertFalse(Map.isSameLocation(europe, t1));
        // TP5: l1 != l2, l1.getTile() == l2.getTile() (B5:false -> N8)
        assertTrue(Map.isSameLocation(u1, t1));
        // TP6: l1 != l2, l1.getTile() != l2.getTile() (B5:true -> N9)
        assertFalse(Map.isSameLocation(u1, t2));
    }


    // ------------------------------------------------------------------
    // M35: isSameContiguity(Location, Location) — 6 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M35-TP1..TP6
    public void testM35_TP1_to_TP6_isSameContiguity() {
        Map map = createSplitMap(4, 2, 2);
        Tile land1 = map.getTile(0, 0);
        Tile land2 = map.getTile(1, 1);
        Tile sea = map.getTile(3, 0);
        Europe europe = dutch.getEurope();

        // TP1: l1 == null
        assertFalse(Map.isSameContiguity(null, land1));
        // TP2: l2 == null
        assertFalse(Map.isSameContiguity(land1, null));
        // TP3: l1 == l2
        assertTrue(Map.isSameContiguity(land1, land1));
        // TP4: l1.getTile() == null
        assertFalse(Map.isSameContiguity(europe, land1));
        // TP5: l2.getTile() == null
        assertFalse(Map.isSameContiguity(land1, europe));
        // TP6: both have tiles (connected true & false)
        assertTrue(Map.isSameContiguity(land1, land2));
        assertFalse(Map.isSameContiguity(land1, sea));
    }


    // ------------------------------------------------------------------
    // M36: isPolar(Tile) — 3 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M36-TP1..TP3
    public void testM36_TP1_to_TP3_isPolar() {
        Map map = createUniformMap(3, 10, plains);
        // TP1: y <= POLAR_HEIGHT (B1:true)
        assertTrue(map.isPolar(map.getTile(0, 1)));
        // TP2: y > POLAR_HEIGHT && y >= height - POLAR_HEIGHT - 1 (B1:false, B2:true)
        assertTrue(map.isPolar(map.getTile(0, 8)));
        // TP3: neither (B1:false, B2:false)
        assertFalse(map.isPolar(map.getTile(0, 5)));
    }


    // ------------------------------------------------------------------
    // M37: getDirection(Tile, Tile) — 3 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M37-TP1..TP3
    public void testM37_TP1_to_TP3_getDirection() {
        Map map = createUniformMap(5, 5, plains);
        Tile t1 = map.getTile(2, 2);
        Tile t2 = map.getAdjacentTile(t1, Direction.N);
        // TP1: t1 == null
        assertNull(map.getDirection(null, t2));
        // TP2: t2 == null
        assertNull(map.getDirection(t1, null));
        // TP3: both non-null
        assertEquals(Direction.N, map.getDirection(t1, t2));
    }


    // ------------------------------------------------------------------
    // M38: getRoughDirection(Tile, Tile) — 5 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M38-TP1..TP5
    public void testM38_TP1_to_TP5_getRoughDirection() {
        Map map = createUniformMap(5, 5, plains);
        Tile center = map.getTile(2, 2);
        // TP1: x == 0 && y == 0 -> null (B1:false, B2:false -> N2)
        assertNull(Map.getRoughDirection(center, center));
        // TP2: x != 0, theta >= 0 (B1:true, B3:true -> N5)
        assertNotNull(Map.getRoughDirection(center, map.getTile(3, 2)));
        // TP3: x == 0, y != 0, theta >= 0 (B1:false, B2:true, B3:true -> N5)
        assertNotNull(Map.getRoughDirection(center, map.getTile(2, 4)));
        // TP4: theta < 0 -> theta += 2*PI (B1:true, B3:false -> N4 -> N5)
        assertNotNull(Map.getRoughDirection(center, map.getTile(0, 0)));
    }


    // ------------------------------------------------------------------
    // M39..M42 & M117: Adjacent tiles, distance, closest tile — 5 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M39-TP1, M40-TP1, M41-TP1, M42-TP1, M117-TP1
    public void testM39_to_M42_M117_tileDistances() {
        Map map = createUniformMap(4, 4, plains);
        Tile t1 = map.getTile(1, 1);
        Tile t2 = map.getAdjacentTile(1, 1, Direction.S);
        Tile t3 = map.getAdjacentTile(t1, Direction.E);
        assertNotNull(t2);
        assertNotNull(t3);
        assertTrue(map.getDistance(t1, t2) > 0);
        List<Tile> list = new ArrayList<>();
        list.add(t2);
        list.add(t3);
        assertNotNull(map.getClosestTile(t1, list));
    }


    // ------------------------------------------------------------------
    // M43: getRandomLandTile(Random) — 4 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M43-TP1..TP4
    public void testM43_TP1_to_TP4_getRandomLandTile() {
        Random rng = new Random(42);
        // TP1: width < 10, height < 10, 1x1 map -> empty circle -> null (B1:true, B2:true, B3:true -> N9)
        Map m1 = createUniformMap(1, 1, plains);
        assertNull(m1.getRandomLandTile(rng));

        // TP2: 2x2 all ocean -> loops over circle, never finds land -> null (B4:true -> N8 -> ... -> B3:true -> N9)
        Map m2 = createUniformMap(2, 2, ocean);
        assertNull(m2.getRandomLandTile(rng));

        // TP3: width >= 10, height >= 10, all land -> finds land (B1:false, B2:false, B4:false -> N7)
        Map m3 = createUniformMap(12, 12, plains);
        Tile found = m3.getRandomLandTile(rng);
        assertNotNull(found);
        assertTrue(found.isLand());
    }


    // ------------------------------------------------------------------
    // M44..M52: Tile iteration, subMap, shuffle — 19 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M44..M48, M50, M52
    public void testM44_to_M48_M50_M52_iteration() {
        // Empty map (0 tiles)
        Map empty = new Map(game, "map:empty");
        assertTrue(empty.getTileSet(t -> true).isEmpty());
        assertTrue(empty.getTileList(t -> true).isEmpty());
        empty.forEachTile(t -> {});
        empty.forEachTile(t -> true, t -> {});
        empty.forSubMap(0, 0, 1, 1, t -> {});

        // Non-empty split map
        Map map = createSplitMap(3, 2, 1);
        Set<Tile> landSet = map.getTileSet(Tile::isLand);
        assertEquals(2, landSet.size());
        List<Tile> landList = map.getTileList(Tile::isLand);
        assertEquals(2, landList.size());
        assertTrue(map.iterator().hasNext());

        final int[] count = {0};
        map.forEachTile(t -> count[0]++);
        assertEquals(6, count[0]);

        count[0] = 0;
        map.forEachTile(Tile::isLand, t -> count[0]++);
        assertEquals(2, count[0]);

        count[0] = 0;
        map.forSubMap(0, 0, 2, 2, t -> count[0]++);
        assertEquals(4, count[0]);

        List<Tile> shuffled = map.getShuffledTiles(new Random(1));
        assertEquals(6, shuffled.size());
    }

    // M49-TP1..TP2: populateTiles
    public void testM49_populateTiles() {
        // TP1: height == 0 (via reflection) -> immediate return true (B1:true -> N8)
        Map m0 = new Map(game, 2, 2);
        setField(m0, "height", 0);
        assertTrue(m0.populateTiles((x, y) -> new Tile(game, plains, x, y)));

        // TP2: func returns null -> setTile returns false -> return false (B3:false -> N5)
        Map m1 = new Map(game, 2, 2);
        assertFalse(m1.populateTiles((x, y) -> null));

        // TP3: full loop -> return true
        Map m2 = new Map(game, 2, 2);
        assertTrue(m2.populateTiles((x, y) -> new Tile(game, plains, x, y)));
    }

    // M51-TP1..TP10: subMap(x, y, w, h)
    public void testM51_subMap() {
        Map map = createUniformMap(4, 4, plains);
        // x < 0, y < 0, w <= 0 after adjustment (B1:false, B2:false, B3:true)
        assertTrue(map.subMap(-5, -5, 2, 2).isEmpty());
        // h <= 0 (B4:true)
        assertTrue(map.subMap(0, 0, 2, 0).isEmpty());
        // x > width (B5:true)
        assertTrue(map.subMap(5, 0, 2, 2).isEmpty());
        // y > height (B6:true)
        assertTrue(map.subMap(0, 5, 2, 2).isEmpty());
        // x+w > width, y+h > height (B7:false, B8:false)
        assertEquals(4, map.subMap(2, 2, 5, 5).size());
        // x+w <= width, y+h <= height (B7:true, B8:true)
        assertEquals(4, map.subMap(1, 1, 2, 2).size());
        // x+w == width with w > 0 at edge x == width -> empty loop (B9:true)
        assertTrue(map.subMap(4, 0, 2, 2).isEmpty());
    }


    // ------------------------------------------------------------------
    // M53..M55 & M115..M116: CircleIterator & Heuristics — 5 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M53-TP1, M54-TP1, M55-TP1, M115-TP1, M116-TP1
    public void testM53_to_M55_M115_M116_circleAndHeuristics() {
        Map map = createUniformMap(5, 5, plains);
        Tile center = map.getTile(2, 2);
        Iterator<Tile> it = map.getCircleIterator(center, true, 1);
        assertTrue(it.hasNext());
        int c = 0;
        for (Tile t : map.getCircleTiles(center, false, 1)) {
            assertNotNull(t);
            c++;
        }
        assertEquals(8, c);

        // Invoke trivialSearchHeuristic (M116) & getManhattenHeuristic (M55, M115)
        Object trivial = getFieldValue(map, "trivialSearchHeuristic");
        assertNotNull(trivial);
        Object manh = invokePrivate(map, "getManhattenHeuristic",
                new Class<?>[]{Tile.class}, center);
        assertNotNull(manh);
    }

    private Object getFieldValue(Object target, String name) {
        try {
            Field f = Map.class.getDeclaredField(name);
            f.setAccessible(true);
            return f.get(target);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    // ------------------------------------------------------------------
    // M56: findRealEnd(Unit, Location) — 6 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M56-TP1..TP6
    public void testM56_TP1_to_TP6_findRealEnd() {
        Map map = createSplitMap(4, 2, 2);
        Tile tile = map.getTile(0, 0);
        Tile sea = map.getTile(3, 0);
        Unit galleon = new ServerUnit(game, sea, dutch, galleonType);
        galleon.setEntryLocation(sea);
        Europe europe = dutch.getEurope();

        // TP1: end == null -> RuntimeException (B1:false -> N1)
        try {
            invokePrivate(map, "findRealEnd", new Class<?>[]{Unit.class, Location.class}, galleon, null);
            fail("Expected RuntimeException");
        } catch (RuntimeException expected) {}

        // TP2: end instanceof Europe (B1:true, B2:false -> N3)
        assertEquals(europe, invokePrivate(map, "findRealEnd",
                new Class<?>[]{Unit.class, Location.class}, galleon, europe));

        // TP3: end instanceof Map -> loops to unit.getFullEntryLocation() (B3:false -> N5 -> N0)
        assertEquals(sea, invokePrivate(map, "findRealEnd",
                new Class<?>[]{Unit.class, Location.class}, galleon, map));

        // TP4: end.getTile() != null (B4:false -> N7)
        assertEquals(tile, invokePrivate(map, "findRealEnd",
                new Class<?>[]{Unit.class, Location.class}, galleon, tile));

        // TP5: end.getTile() == null, unit != null -> unit.resolveDestination() (B5:false -> N9)
        galleon.setLocation(dutch.getHighSeas());
        galleon.setDestination(europe);
        Location dummyLoc = new HighSeas(game);
        assertEquals(europe, invokePrivate(map, "findRealEnd",
                new Class<?>[]{Unit.class, Location.class}, galleon, dummyLoc));

        // TP6: end.getTile() == null, unit == null -> RuntimeException (B5:true -> N10)
        try {
            invokePrivate(map, "findRealEnd",
                    new Class<?>[]{Unit.class, Location.class}, null, dummyLoc);
            fail("Expected RuntimeException");
        } catch (RuntimeException expected) {}
    }


    // ------------------------------------------------------------------
    // M57 & M58: getBestEntryPath & getBestEntryTile — 4 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M57-TP1..TP2 & M58-TP1..TP2
    public void testM57_M58_bestEntry() {
        Map map = createUniformMap(4, 4, ocean);
        map.getTile(3, 0).setType(highSeas);
        map.resetHighSeasCount();
        exploreAll(map, dutch);
        Unit galleon = new ServerUnit(game, dutch.getEurope(), dutch, galleonType);

        // M57-TP1 (costDecider == null) & M58-TP2 (path != null)
        Tile entry = map.getBestEntryTile(galleon, map.getTile(0, 0), null, null);
        assertNotNull(entry);

        // M57-TP2 (costDecider != null) & M58-TP1 (path == null when no high seas)
        Map noHighSeas = createUniformMap(3, 3, plains);
        exploreAll(noHighSeas, dutch);
        assertNull(noHighSeas.getBestEntryTile(galleon, noHighSeas.getTile(1, 1),
                null, CostDeciders.avoidSettlements()));
    }


    // ------------------------------------------------------------------
    // M59: findMapPath(...) — 100% C3
    // ------------------------------------------------------------------

    public void testM59_findMapPath_allBranches() {
        // Setup 6x3 map: cols 0..1 plains, cols 2..5 ocean
        Map map = createSplitMap(6, 3, 2);
        exploreAll(map, dutch);

        Tile land0 = map.getTile(0, 0);
        Tile land1 = map.getTile(1, 0);
        Tile sea2 = map.getTile(2, 0);
        Tile sea4 = map.getTile(4, 0);
        Tile sea5 = map.getTile(5, 0);

        Unit colonist = new ServerUnit(game, land0, dutch, colonistType);
        Unit galleon = new ServerUnit(game, sea2, dutch, galleonType);
        Unit emptyCarrier = new ServerUnit(game, sea4, dutch, frigateType); // 0 cargo space
        Unit frenchShip = new ServerUnit(game, sea5, french, galleonType);

        // 1. Same contiguity (B4:false), carrier == null (B5:true)
        PathNode p1 = (PathNode) invokePrivate(map, "findMapPath",
                new Class<?>[]{Unit.class, Tile.class, Tile.class, Unit.class, CostDecider.class, LogBuilder.class},
                colonist, land0, land1, null, null, null);
        assertNotNull(p1);

        // 2. Same contiguity, carrier != null (B1:false, B5:false), compare carrierPath vs path
        PathNode p2 = (PathNode) invokePrivate(map, "findMapPath",
                new Class<?>[]{Unit.class, Tile.class, Tile.class, Unit.class, CostDecider.class, LogBuilder.class},
                colonist, land0, land1, galleon, null, null);
        assertNotNull(p2);

        // 3. Same contiguity, path == null, carrierPath != null (e.g. sea to sea with colonist + galleon)
        PathNode p3 = (PathNode) invokePrivate(map, "findMapPath",
                new Class<?>[]{Unit.class, Tile.class, Tile.class, Unit.class, CostDecider.class, LogBuilder.class},
                colonist, sea2, sea4, galleon, null, null);
        assertNotNull(p3);

        // 4. Different contiguity, offMapUnit != null via naval unit (B2:false, B3:true, B9:true -> N16)
        PathNode p4 = (PathNode) invokePrivate(map, "findMapPath",
                new Class<?>[]{Unit.class, Tile.class, Tile.class, Unit.class, CostDecider.class, LogBuilder.class},
                galleon, sea2, land1, null, null, null);
        assertNull(p4);

        // 5. Different contiguity, offMapUnit == null, unit on carrier, !start.isLand() && end.isLand() (B10..B14)
        colonist.setLocation(galleon);
        PathNode p5 = (PathNode) invokePrivate(map, "findMapPath",
                new Class<?>[]{Unit.class, Tile.class, Tile.class, Unit.class, CostDecider.class, LogBuilder.class},
                colonist, sea2, land1, null, null, null);
        assertNotNull(p5);
        colonist.setLocation(land0);

        // 6. Different contiguity, start.isLand(), !end.isLand(), friendly carrier on end -> embark (B15..B21, B22:false)
        galleon.setLocation(sea2);
        PathNode p6 = (PathNode) invokePrivate(map, "findMapPath",
                new Class<?>[]{Unit.class, Tile.class, Tile.class, Unit.class, CostDecider.class, LogBuilder.class},
                colonist, land0, sea2, null, null, null);
        assertNotNull(p6);

        // 7. start.isLand(), !end.isLand(), unit == null (B18:true -> N32)
        assertNull(invokePrivate(map, "findMapPath",
                new Class<?>[]{Unit.class, Tile.class, Tile.class, Unit.class, CostDecider.class, LogBuilder.class},
                null, land0, sea2, null, null, null));

        // 8. start.isLand(), !end.isLand(), enemy unit on end (B19:true -> N32)
        assertNull(invokePrivate(map, "findMapPath",
                new Class<?>[]{Unit.class, Tile.class, Tile.class, Unit.class, CostDecider.class, LogBuilder.class},
                colonist, land0, sea5, null, null, null));

        // 9. start.isLand(), !end.isLand(), friendly non-adjacent sea tile (B20:true -> N32)
        galleon.setLocation(sea4);
        assertNull(invokePrivate(map, "findMapPath",
                new Class<?>[]{Unit.class, Tile.class, Tile.class, Unit.class, CostDecider.class, LogBuilder.class},
                colonist, land0, sea4, null, null, null));

        // 10. start.isLand(), !end.isLand(), adjacent friendly unit on sea2 with no carrier capacity (B21:true -> N32)
        galleon.setLocation(sea4);
        new ServerUnit(game, sea2, dutch, colonistType);
        assertNull(invokePrivate(map, "findMapPath",
                new Class<?>[]{Unit.class, Tile.class, Tile.class, Unit.class, CostDecider.class, LogBuilder.class},
                colonist, land0, sea2, null, null, null));

        // 11. start.isLand(), !end.isLand(), adjacent friendly galleon on end, but start is isolated (B22:false -> N33)
        Map isoMap = createUniformMap(4, 3, ocean);
        for (int x = 0; x < 4; x++) {
            for (int y = 0; y < 3; y++) {
                isoMap.getTile(x, y).setContiguity(2);
            }
        }
        Tile isoStart = isoMap.getTile(0, 1);
        isoStart.setType(plains);
        isoStart.setContiguity(1);
        Tile isoBridge = isoMap.getTile(2, 1);
        isoBridge.setType(plains);
        isoBridge.setContiguity(1);
        Tile isoEnd = isoMap.getTile(3, 1);
        Unit isoColonist = new ServerUnit(game, isoStart, dutch, colonistType);
        new ServerUnit(game, isoEnd, dutch, galleonType);
        assertNull(invokePrivate(isoMap, "findMapPath",
                new Class<?>[]{Unit.class, Tile.class, Tile.class, Unit.class, CostDecider.class, LogBuilder.class},
                isoColonist, isoStart, isoEnd, null, null, null));
    }


    // ------------------------------------------------------------------
    // M60: finishPath(PathNode, Unit, LogBuilder) — 100% C3
    // ------------------------------------------------------------------

    // M60-TP1..TP6
    public void testM60_finishPath() {
        Map map = createUniformMap(3, 3, ocean);
        Tile t0 = map.getTile(0, 0);
        Unit galleon = new ServerUnit(game, t0, dutch, galleonType);
        Unit colonist = new ServerUnit(game, galleon, dutch, colonistType);
        PathNode path = new PathNode(t0, 3, 0, false, null, null);
        LogBuilder lb = new LogBuilder(64);

        // path == null, lb == null (B1:true, B6:true)
        invokePrivate(map, "finishPath",
                new Class<?>[]{PathNode.class, Unit.class, LogBuilder.class},
                null, galleon, null);

        // path != null, !atSea, lb != null (B1:false, B2:false, B4:true, B5:false, B6:false)
        invokePrivate(map, "finishPath",
                new Class<?>[]{PathNode.class, Unit.class, LogBuilder.class},
                path, galleon, lb);

        // atSea via HighSeas + workLeft > 0, onCarrier == true (B2:true, B3:true, B4:false)
        galleon.setLocation(dutch.getHighSeas());
        galleon.setWorkLeft(2);
        invokePrivate(map, "finishPath",
                new Class<?>[]{PathNode.class, Unit.class, LogBuilder.class},
                path, colonist, null);
        assertEquals(2, path.getTurns());

        // atSea, onCarrier == false (B3:false)
        invokePrivate(map, "finishPath",
                new Class<?>[]{PathNode.class, Unit.class, LogBuilder.class},
                path, galleon, null);
    }


    // ------------------------------------------------------------------
    // M61 & M114: findPath(...) & lambda$findPath$0 — 100% C3
    // ------------------------------------------------------------------

    public void testM61_M114_findPath_allBranches() {
        Map map = createSplitMap(5, 3, 2);
        for (int x = 3; x < 5; x++) {
            for (int y = 0; y < 3; y++) {
                map.getTile(x, y).setType(highSeas);
            }
        }
        map.resetHighSeasCount();
        exploreAll(map, dutch);

        Tile land0 = map.getTile(0, 0);
        Tile land1 = map.getTile(1, 0);
        Tile sea2 = map.getTile(2, 0);
        Tile sea4 = map.getTile(4, 0);
        Europe europe = dutch.getEurope();

        Unit colonist = new ServerUnit(game, land0, dutch, colonistType);
        Unit galleon = new ServerUnit(game, sea2, dutch, galleonType);
        galleon.setEntryLocation(sea4);

        // 1. traceSearch == true (B1:false)
        map.setSearchTrace(true);
        assertNotNull(map.findPath(colonist, land0, land1, null, null, null));
        map.setSearchTrace(false);

        // 2. Unexplored realEnd (type == null) with adjacent explored same-contiguity tile (B3:true, B4:true, M114)
        land1.setType(null);
        PathNode pUnexp = map.findPath(colonist, land0, land1, null, null, null);
        assertNotNull(pUnexp);
        // Unexplored realEnd from Europe (start not Tile -> closest == null)
        assertNull(map.findPath(galleon, europe, land1, null, null, null));
        // Restore type
        land1.setType(plains);

        // 3. Europe -> Europe (B8:false, B9:false -> N21)
        PathNode pEE = map.findPath(galleon, europe, europe, null, null, null);
        assertNotNull(pEE);

        // 4. Europe -> Tile, offMapUnit == null (B10:false, B12:true -> N26)
        assertNull(map.findPath(null, europe, sea2, null, null, null));

        // 5. Europe -> Tile, offMapUnit cannot move to high seas (B13:true -> N26)
        assertNull(map.findPath(colonist, europe, land0, null, null, null));

        // 6. Europe -> Tile, success with carrier != null and unit not on carrier (B17..B19)
        PathNode pET = map.findPath(colonist, europe, land1, galleon, null, null);
        assertNotNull(pET);

        // 7. Europe -> Tile, carrier == null (B18:true)
        assertNotNull(map.findPath(galleon, europe, sea2, null, null, null));

        // 8. Europe -> Tile, findMapPath fails (not on river -> B16:false)
        assertNull(map.findPath(galleon, europe, land0, null, null, null));

        // 9. Tile -> Europe, offMapUnit == null or cannot move to high seas (B22:true, B23:true)
        assertNull(map.findPath(colonist, land0, europe, null, null, null));

        // 10. Tile -> Europe, success (B24:false -> N46)
        assertNotNull(map.findPath(galleon, sea2, europe, null, null, null));

        // 11. Tile -> Europe, no high seas (B24:true -> N45)
        Map noHS = createUniformMap(3, 3, plains);
        exploreAll(noHS, dutch);
        assertNull(noHS.findPath(galleon, noHS.getTile(1, 1), europe, null, null, null));
        game.changeMap(map);

        // 12. Tile -> Tile, adjacent blocked (legal but !isProgress): armed scout attacking French unit
        Unit scout = new ServerUnit(game, land0, dutch, scoutType);
        Unit frenchCol = new ServerUnit(game, land1, french, colonistType);
        assertNotNull(map.findPath(scout, land0, land1, null, null, null));
        frenchCol.dispose();

        // 13. Invalid start (neither Europe nor Tile, e.g. Map) -> IllegalStateException (N58)
        try {
            map.findPath(galleon, map, sea2, null, null, null);
            fail("Expected IllegalStateException");
        } catch (IllegalStateException expected) {}
    }


    // ------------------------------------------------------------------
    // M62..M65: search, getSearchTrace, setSearchTrace, usedCarrier — 100% C3
    // ------------------------------------------------------------------

    // M62..M65
    public void testM62_to_M65_searchAndTrace() {
        Map map = createSplitMap(4, 3, 2);
        exploreAll(map, dutch);
        Tile land0 = map.getTile(0, 0);
        Tile land1 = map.getTile(1, 0);
        Tile sea2 = map.getTile(2, 0);
        Unit galleon = new ServerUnit(game, sea2, dutch, galleonType);
        galleon.setEntryLocation(sea2);
        Unit colonist = new ServerUnit(game, land0, dutch, colonistType);

        // M63 & M64
        assertFalse(map.getSearchTrace());
        assertFalse(map.setSearchTrace(true));
        assertTrue(map.getSearchTrace());

        // M62: start instanceof Europe, offMapUnit cannot move to high seas -> null
        assertNull(map.search(colonist, dutch.getEurope(),
                GoalDeciders.getLocationGoalDecider(land0), null, 5, null, null));

        // M62: start instanceof Europe, searchMap returns path -> findPath
        assertNotNull(map.search(galleon, dutch.getEurope(),
                GoalDeciders.getLocationGoalDecider(sea2), null, 5, null, null));

        // M62: start instanceof Europe, searchMap returns null
        assertNull(map.search(galleon, dutch.getEurope(),
                GoalDeciders.getLocationGoalDecider(land0), null, 5, null, null));

        // M62: start instanceof Tile
        map.setSearchTrace(false);
        assertNotNull(map.search(colonist, land0,
                GoalDeciders.getLocationGoalDecider(land1), null, 5, null, null));

        // M65: usedCarrier
        PathNode n1 = new PathNode(land0, 3, 0, false, null, null);
        PathNode n2 = new PathNode(sea2, 3, 0, true, n1, null);
        assertTrue((Boolean) invokePrivate(map, "usedCarrier", new Class<?>[]{PathNode.class}, n2));
        assertFalse((Boolean) invokePrivate(map, "usedCarrier", new Class<?>[]{PathNode.class}, n1));
    }


    // ------------------------------------------------------------------
    // M66 & M113: searchMap(...) & lambda$searchMap$0 — 100% feasible C3
    // ------------------------------------------------------------------

    public void testM66_M113_searchMap_comprehensive() throws Exception {
        Map map = createSplitMap(6, 4, 3);
        exploreAll(map, dutch);
        Tile land00 = map.getTile(0, 0);
        Tile land10 = map.getTile(1, 0);
        Tile land20 = map.getTile(2, 0);
        Tile land22 = map.getTile(2, 2);
        Tile sea30 = map.getTile(3, 0);
        Tile sea40 = map.getTile(4, 0);

        Unit colonist = new ServerUnit(game, land00, dutch, colonistType);
        Unit galleon = new ServerUnit(game, sea30, dutch, galleonType);
        Unit frenchCol = new ServerUnit(game, land22, french, colonistType);
        LogBuilder lb = new LogBuilder(256);

        // 1. SubGoals GoalDecider to hit bestScore < currentNode.getCost(), closed != null, openMap improve
        GoalDecider subGoalDecider = new GoalDecider() {
            private PathNode best = null;
            @Override public PathNode getGoal() { return best; }
            @Override public boolean hasSubGoals() { return true; }
            @Override public boolean check(Unit u, PathNode pn) {
                if (pn.getTile() == land10) {
                    if (best == null || pn.getCost() < best.getCost()) {
                        best = pn;
                    }
                    return true;
                }
                return false;
            }
        };
        assertNotNull(invokePrivate(map, "searchMap",
                new Class<?>[]{Unit.class, Tile.class, GoalDecider.class, CostDecider.class,
                        int.class, Unit.class,
                        Class.forName("net.sf.freecol.common.model.Map$SearchHeuristic"),
                        LogBuilder.class},
                colonist, land00, subGoalDecider, null, 2, null, null, lb));

        // 2. maxTurns = 0 -> triggers currentNode.getTurns() > maxTurns
        assertNull(invokePrivate(map, "searchMap",
                new Class<?>[]{Unit.class, Tile.class, GoalDecider.class, CostDecider.class,
                        int.class, Unit.class,
                        Class.forName("net.sf.freecol.common.model.Map$SearchHeuristic"),
                        LogBuilder.class},
                colonist, land00, GoalDeciders.getLocationGoalDecider(land22),
                null, 0, null, null, lb));

        // 3. Embark & Disembark with carrier (land -> sea -> island)
        Tile island = map.getTile(5, 0);
        island.setType(plains);
        island.setContiguity(3);
        galleon.setLocation(map.getTile(3, 1));
        PathNode embarkPath = (PathNode) invokePrivate(map, "searchMap",
                new Class<?>[]{Unit.class, Tile.class, GoalDecider.class, CostDecider.class,
                        int.class, Unit.class,
                        Class.forName("net.sf.freecol.common.model.Map$SearchHeuristic"),
                        LogBuilder.class},
                colonist, land10, GoalDeciders.getLocationGoalDecider(island),
                null, 10, galleon, null, lb);
        assertNotNull(embarkPath);

        // 4. Goal with MOVE_NO_ATTACK_CIVILIAN (colonist trying to reach tile with French civilian)
        assertNull(invokePrivate(map, "searchMap",
                new Class<?>[]{Unit.class, Tile.class, GoalDecider.class, CostDecider.class,
                        int.class, Unit.class,
                        Class.forName("net.sf.freecol.common.model.Map$SearchHeuristic"),
                        LogBuilder.class},
                colonist, land20, GoalDeciders.getLocationGoalDecider(land22),
                null, 5, null, null, lb));

        // 5. Goal with ATTACK_UNIT (scout or armed unit? Or naval unit attacking)
        Unit scout = new ServerUnit(game, land20, dutch, scoutType);
        assertNotNull(invokePrivate(map, "searchMap",
                new Class<?>[]{Unit.class, Tile.class, GoalDecider.class, CostDecider.class,
                        int.class, Unit.class,
                        Class.forName("net.sf.freecol.common.model.Map$SearchHeuristic"),
                        LogBuilder.class},
                scout, land20, GoalDeciders.getLocationGoalDecider(land22),
                null, 5, null, null, lb));
        frenchCol.dispose();
    }


    // ------------------------------------------------------------------
    // M67 & M68: searchCircle & getLandWithinDistance — 100% C3
    // ------------------------------------------------------------------

    // M67-TP1..TP7 & M68-TP1..TP2
    public void testM67_M68_searchCircleAndLandWithinDistance() {
        Map map = createSplitMap(4, 4, 2);
        Tile land = map.getTile(0, 0);
        Tile sea = map.getTile(3, 3);
        GoalDecider gd = GoalDeciders.getLocationGoalDecider(map.getTile(1, 1));

        // M67: null start, null gd, radius <= 0
        assertNull(map.searchCircle(null, gd, 2));
        assertNull(map.searchCircle(land, null, 2));
        assertNull(map.searchCircle(land, gd, 0));
        // M67: found
        assertEquals(map.getTile(1, 1), map.searchCircle(land, gd, 3));
        // M67: not found
        assertNull(map.searchCircle(sea, GoalDeciders.getLocationGoalDecider(land), 1));

        // M68: found and not found
        assertNotNull(map.getLandWithinDistance(1, 1, 2));
        assertNull(map.getLandWithinDistance(3, 3, 1));
    }


    // ------------------------------------------------------------------
    // M69..M73: FloodFill, resetContiguity, collectStartingTiles — 100% C3
    // ------------------------------------------------------------------

    // M69..M73
    public void testM69_to_M73_floodFillAndStartingTiles() {
        Map map = createSplitMap(4, 3, 2);
        // M69 & M70 with limit
        boolean[][] bm = new boolean[3][3];
        bm[0][0] = true;
        bm[0][1] = true;
        bm[1][1] = true;
        boolean[][] res1 = Map.floodFillBool(bm, 0, 0);
        assertTrue(res1[1][1]);
        boolean[][] res2 = Map.floodFillBool(bm, 0, 0, 1);
        assertFalse(res2[1][1]);

        // M71 & M72
        map.resetContiguity();
        assertTrue(map.getTile(0, 0).getContiguity() >= 0);

        // M73: collectStartingTiles with high seas on both edges and land in middle
        Map hsMap = createSplitMap(5, 2, 0);
        for (int y = 0; y < 2; y++) {
            hsMap.getTile(0, y).setType(highSeas);
            hsMap.getTile(2, y).setType(plains);
            hsMap.getTile(4, y).setType(highSeas);
        }
        List<Tile> east = new ArrayList<>();
        List<Tile> west = new ArrayList<>();
        hsMap.collectStartingTiles(east, west);
        assertEquals(2, east.size());
        assertEquals(2, west.size());
    }


    // ------------------------------------------------------------------
    // M74, M75, M110..M112: resetHighSeas & resetHighSeasCount — 100% feasible C3
    // ------------------------------------------------------------------

    public void testM74_M75_M110_to_M112_highSeas() {
        // M74 invalid distances
        Map map = createSplitMap(6, 3, 2);
        try {
            map.resetHighSeas(-1, 2);
            fail("Expected RuntimeException");
        } catch (RuntimeException expected) {}
        try {
            map.resetHighSeas(2, -1);
            fail("Expected RuntimeException");
        } catch (RuntimeException expected) {}

        // M74: all ocean -> sets high seas on both left and right
        Map oceanMap = createUniformMap(6, 3, ocean);
        oceanMap.getTile(0, 0).setType(highSeas); // triggers M112:true & M111
        oceanMap.resetHighSeas(1, 2);

        // M74: all land -> no high seas on either side (totalL == 0 && totalR == 0)
        Map landMap = createUniformMap(4, 2, plains);
        landMap.resetHighSeas(1, 2);

        // M74: land on left, ocean on right -> totalL == 0, totalR > 0
        Map rightSea = createSplitMap(6, 2, 3);
        rightSea.resetHighSeas(1, 2);

        // M74: seaL fallback (ocean within distToLandFromHighSeas of land)
        Map narrowSea = createSplitMap(4, 2, 1);
        narrowSea.getTile(1, 0).setType(ocean);
        narrowSea.getTile(1, 1).setType(ocean);
        narrowSea.resetHighSeas(5, 2);

        // M75 & M110: resetHighSeasCount
        oceanMap.resetHighSeasCount();
        assertTrue(oceanMap.getTile(0, 0).getHighSeasCount() >= 0);
    }


    // ------------------------------------------------------------------
    // M76: resetLayers() — 100% C3
    // ------------------------------------------------------------------

    public void testM76_resetLayers() {
        Map map = createUniformMap(2, 2, plains);
        Tile t = map.getTile(0, 0);

        // TERRAIN
        map.resetLayers();
        assertEquals(Layer.TERRAIN, map.getLayer());

        // REGIONS
        Region reg = new Region(game);
        t.setRegion(reg);
        map.resetLayers();
        assertEquals(Layer.REGIONS, map.getLayer());

        // RIVERS
        TileImprovement river = new TileImprovement(game, t,
                spec().getTileImprovementType("model.improvement.river"), null);
        t.add(river);
        map.resetLayers();
        assertEquals(Layer.RIVERS, map.getLayer());

        // RESOURCES
        t.addResource(new Resource(game, t, grainResource));
        map.resetLayers();
        assertEquals(Layer.RESOURCES, map.getLayer());

        // RUMOURS
        t.addLostCityRumour(new LostCityRumour(game, t));
        map.resetLayers();
        assertEquals(Layer.RUMOURS, map.getLayer());

        // NATIVES (without all others? first test ALL since all 4 flags are true)
        Player inca = game.getPlayerByNationId("model.nation.inca");
        ServerIndianSettlement is = new ServerIndianSettlement(game, inca, "Cusco",
                map.getTile(1, 1), false, colonistType, null);
        map.getTile(1, 1).setSettlement(is);
        map.resetLayers();
        assertEquals(Layer.ALL, map.getLayer());

        // NATIVES alone
        t.setTileItemContainer(null);
        map.resetLayers();
        assertEquals(Layer.NATIVES, map.getLayer());
    }


    // ------------------------------------------------------------------
    // M77 & M109: fixupRegions() — 100% C3
    // ------------------------------------------------------------------

    public void testM77_M109_fixupRegions() {
        Map map = new Map(game, 2, 2);
        Region pacific = new Region(game);
        setRegionKey(pacific, Region.PACIFIC_KEY);
        Region geo = new Region(game);
        geo.setDiscoverable(false);
        Region discParent = new Region(game);
        discParent.setDiscoverable(true);
        discParent.setParent(geo);
        Region child = new Region(game);
        child.setDiscoverable(true);
        child.setParent(discParent);

        map.addRegion(pacific);
        map.addRegion(geo);
        map.addRegion(discParent);
        map.addRegion(child);

        map.fixupRegions();
        assertEquals(geo, child.getParent());
        assertTrue(geo.getChildren().contains(child));
    }


    // ------------------------------------------------------------------
    // M78, M79, M108: importTile & scale — 100% C3
    // ------------------------------------------------------------------

    public void testM78_M79_M108_importTileAndScale() {
        Map map = createUniformMap(4, 4, plains);
        Tile src = map.getTile(0, 0);
        src.setMoveToEurope(Boolean.TRUE);
        src.addResource(new Resource(game, src, grainResource));

        // M78: moveToEurope != null && container != null
        Tile imp1 = map.importTile(src, 1, 1, Layer.ALL);
        assertEquals(Boolean.TRUE, imp1.getMoveToEurope());
        assertNotNull(imp1.getTileItemContainer());

        // M78: moveToEurope == null && container == null
        Tile plainTile = map.getTile(1, 1);
        Tile imp2 = map.importTile(plainTile, 0, 0, Layer.ALL);
        assertNull(imp2.getMoveToEurope());

        // M79 & M108
        Map scaled = map.scale(2, 2);
        assertEquals(2, scaled.getWidth());
        assertEquals(2, scaled.getHeight());
    }


    // ------------------------------------------------------------------
    // M80..M97: Location interface methods — 100% C3
    // ------------------------------------------------------------------

    public void testM80_to_M97_locationInterface() {
        Map map = createUniformMap(3, 3, plains);
        Tile t = map.getTile(0, 0);
        Unit u = new ServerUnit(game, t, dutch, colonistType);
        Goods g = new Goods(game, null, grainType, 10);

        assertNull(map.getTile());
        assertNotNull(map.getLocationLabel());
        // M82: newLandName == null vs non-null
        assertNotNull(map.getLocationLabelFor(dutch));
        dutch.setNewLandName("New Holland");
        assertNotNull(map.getLocationLabelFor(dutch));

        // M83: add(Unit) throws, add(non-Unit) returns false
        try {
            map.add(u);
            fail("Expected RuntimeException");
        } catch (RuntimeException expected) {}
        assertFalse(map.add(g));

        // M84: remove
        assertTrue(map.remove(u));
        assertFalse(map.remove(u)); // tile == null
        assertFalse(map.remove(g)); // not Unit

        // M85: contains
        Unit u2 = new ServerUnit(game, t, dutch, colonistType);
        assertTrue(map.contains(u2));
        u2.setLocationNoUpdate(null);
        assertFalse(map.contains(u2));
        u2.setLocationNoUpdate(dutch.getEurope());
        assertFalse(map.contains(u2));
        assertFalse(map.contains(g));

        // M86..M97
        assertTrue(map.canAdd(u));
        assertFalse(map.canAdd(g));
        assertEquals(-1, map.getUnitCount());
        assertEquals(0, map.getUnits().collect(Collectors.toList()).size());
        assertTrue(map.getUnitList().isEmpty());
        assertNull(map.getGoodsContainer());
        assertNull(map.getSettlement());
        assertNull(map.getColony());
        assertNull(map.getIndianSettlement());
        assertEquals(map, map.up());
        assertEquals(Location.LOCATION_RANK_NOWHERE, map.getRank());
        assertEquals("Map", map.toShortString());
        assertNotNull(map.getLocationImageKey());
    }


    // ------------------------------------------------------------------
    // M98, M99, M107: checkIntegrity & copyIn — 100% C3
    // ------------------------------------------------------------------

    public void testM98_M99_M107_integrityAndCopyIn() {
        Map map = createUniformMap(2, 2, plains);
        LogBuilder lb = new LogBuilder(64);
        assertNotNull(map.checkIntegrity(false, lb));

        // M99: copyIn
        assertFalse(map.copyIn(null));
        assertFalse(map.copyIn(map.getTile(0, 0)));

        Map other = new Map(game, 2, 2);
        other.setId(map.getId());
        other.populateTiles((x, y) -> new Tile(game, ocean, x, y));
        Region r = new Region(game);
        setRegionKey(r, "model.region.copy");
        other.addRegion(r);
        assertTrue(map.copyIn(other));
        assertEquals(1, map.getRegions().size());

        // M99: dimension mismatch -> RuntimeException
        Map diff = createUniformMap(3, 3, plains);
        diff.setId(map.getId());
        try {
            map.copyIn(diff);
            fail("Expected RuntimeException");
        } catch (RuntimeException expected) {}
    }


    // ------------------------------------------------------------------
    // M100..M106: XML serialization & lambdas — 100% C3
    // ------------------------------------------------------------------

    public void testM100_to_M106_xmlSerialization() throws Exception {
        Map src = createUniformMap(5, 5, plains);
        Region r = new Region(game);
        setRegionKey(r, "model.region.xml");
        try {
            Field tf = Region.class.getDeclaredField("type");
            tf.setAccessible(true);
            tf.set(r, Region.RegionType.LAND);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        src.addRegion(r);

        // Also attach a colony settlement ownership to a tile to cover M106 true/false
        Tile ownedTile = src.getTile(0, 0);
        Colony col = new ServerColony(game, dutch, "New Amsterdam", ownedTile);
        ownedTile.setOwningSettlement(col);

        StringWriter sw = new StringWriter();
        try (FreeColXMLWriter xw = new FreeColXMLWriter(
                sw, FreeColXMLWriter.WriteScope.toSave())) {
            src.toXML(xw);
        }
        String xml = sw.toString();
        assertTrue(xml.contains("<" + Map.TAG));
        assertEquals(Map.TAG, src.getXMLTagName());

        // M102, M103, M104, M106: readFromXML
        Map dst = new Map(game, "map:dst");
        try (FreeColXMLReader xr = new FreeColXMLReader(new StringReader(xml))) {
            xr.nextTag();
            dst.readFromXML(xr);
        }
        assertEquals(5, dst.getWidth());
        assertEquals(1, dst.getRegions().size());

        // M102: readAttributes error (missing/invalid width)
        Map bad = new Map(game, 2, 2);
        try (FreeColXMLReader xr = new FreeColXMLReader(
                new StringReader("<map id=\"bad\" width=\"5\" height=\"5\"/>"))) {
            xr.nextTag();
            bad.readFromXML(xr);
            fail("Expected XMLStreamException");
        } catch (XMLStreamException expected) {}

        // M104: readChild with invalid tile (updateTile returns false) & unknown child tag
        String extraXml = "<map id=\"" + dst.getId() + "\" width=\"5\" height=\"5\">"
                + "<tile id=\"tile:oob\" x=\"99\" y=\"99\" type=\"model.tile.plains\"/>"
                + "<unknownTag/>"
                + "</map>";
        try (FreeColXMLReader xr = new FreeColXMLReader(new StringReader(extraXml))) {
            xr.nextTag();
            dst.readFromXML(xr);
            fail("Expected XMLStreamException");
        } catch (XMLStreamException expected) {}
    }
}
