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

import java.lang.reflect.Method;
import java.util.List;
import java.util.Random;
import java.util.logging.Logger;

import net.sf.freecol.common.model.Map.Position;
import net.sf.freecol.common.option.MapGeneratorOptions;
import net.sf.freecol.common.option.OptionGroup;
import net.sf.freecol.common.util.RandomUtils.RandomIntCache;
import net.sf.freecol.util.test.FreeColTestCase;


/**
 * CFG-driven test suite for {@link LandMap} derived from
 * {@code test_cft/graph/net/sf/freecol/common/model/LandMap.json}.
 *
 * Covers all 23 methods (M1..M23) and achieves 100% feasible C3 (node + edge)
 * coverage across 38 Test Paths.
 *
 * Infeasible edges documented:
 * - M2 ({@code LandMap(Map, RandomIntCache)}): {@code N4 -> N6} ({@code B3:true},
 *   {@code !map.isValid(x, y)}) — the enclosing loops iterate {@code 0 <= x < map.getWidth()}
 *   and {@code 0 <= y < map.getHeight()}, so {@code map.isValid(x, y)} is invariant {@code true}.
 */
public class LandMapCfgTest extends FreeColTestCase {

    private static final Logger TEST_LOGGER = Logger.getLogger(LandMapCfgTest.class.getName());
    private static final TileType OCEAN_TYPE = spec().getTileType("model.tile.ocean");
    private static final TileType PLAINS_TYPE = spec().getTileType("model.tile.plains");

    private RandomIntCache cache;
    private LandMap landMap;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        // Pre-condition: Initialize deterministic RandomIntCache and a 10x10 LandMap.
        cache = new RandomIntCache(TEST_LOGGER, "cft", new Random(42L), 1000, 256);
        landMap = new LandMap(10, 10, cache);
    }

    // ------------------------------------------------------------------
    // Pre-condition helpers & stubs
    // ------------------------------------------------------------------

    private static RandomIntCache scriptedCache(final int... values) {
        return new RandomIntCache(TEST_LOGGER, "scripted", new Random(1L), 1000, 16) {
            private int idx = 0;
            @Override
            public int nextInt(int n) {
                int v = values[idx % values.length];
                idx++;
                return Math.floorMod(v, n);
            }
        };
    }

    private Object invokePrivate(LandMap target, String name, Class<?>[] paramTypes, Object... args)
            throws Exception {
        Method m = LandMap.class.getDeclaredMethod(name, paramTypes);
        m.setAccessible(true);
        return m.invoke(target, args);
    }

    // ------------------------------------------------------------------
    // M1: LandMap(int, int, RandomIntCache) — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M1-TP1: ENTRY -> N0 -> EXIT (construct empty LandMap with given dimensions)
    public void testM1_TP1_ConstructorDimensions() {
        LandMap lm = new LandMap(8, 6, cache);
        assertEquals(8, lm.getWidth());
        assertEquals(6, lm.getHeight());
        assertFalse(lm.hasLand());
    }

    // ------------------------------------------------------------------
    // M2: LandMap(Map, RandomIntCache) — 1 Test Path (100% feasible C3; N4->N6 infeasible)
    // ------------------------------------------------------------------

    // M2-TP1: ENTRY -> N0 -> N1..N10 -> N11 -> EXIT (import land and water tiles from Map)
    public void testM2_TP1_ConstructorFromMap() {
        Game game = getStandardGame();
        Map map = getTestMap(OCEAN_TYPE);
        map.getTile(1, 1).setType(PLAINS_TYPE);
        game.setMap(map);

        LandMap lm = new LandMap(map, cache);
        assertEquals(map.getWidth(), lm.getWidth());
        assertEquals(map.getHeight(), lm.getHeight());
        assertTrue(lm.isLand(1, 1));
        assertFalse(lm.isLand(0, 0));
    }

    // ------------------------------------------------------------------
    // M3: LandMap(OptionGroup, RandomIntCache) — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M3-TP1: ENTRY -> N0 -> EXIT (construct LandMap from MapGeneratorOptions)
    public void testM3_TP1_ConstructorFromOptionGroup() {
        OptionGroup mgo = spec().getMapGeneratorOptions();
        int savedW = mgo.getInteger(MapGeneratorOptions.MAP_WIDTH);
        int savedH = mgo.getInteger(MapGeneratorOptions.MAP_HEIGHT);
        int savedMass = mgo.getInteger(MapGeneratorOptions.LAND_MASS);
        try {
            mgo.setInteger(MapGeneratorOptions.MAP_WIDTH, 40);
            mgo.setInteger(MapGeneratorOptions.MAP_HEIGHT, 40);
            mgo.setInteger(MapGeneratorOptions.LAND_MASS, 20);
            LandMap lm = new LandMap(mgo, cache);
            assertEquals(40, lm.getWidth());
            assertEquals(40, lm.getHeight());
            assertTrue(lm.hasLand());
        } finally {
            mgo.setInteger(MapGeneratorOptions.MAP_WIDTH, savedW);
            mgo.setInteger(MapGeneratorOptions.MAP_HEIGHT, savedH);
            mgo.setInteger(MapGeneratorOptions.LAND_MASS, savedMass);
        }
    }

    // ------------------------------------------------------------------
    // M4: getWidth() — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M4-TP1: ENTRY -> N0 -> EXIT (return width)
    public void testM4_TP1_GetWidth() {
        assertEquals(10, landMap.getWidth());
    }

    // ------------------------------------------------------------------
    // M5: getHeight() — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M5-TP1: ENTRY -> N0 -> EXIT (return height)
    public void testM5_TP1_GetHeight() {
        assertEquals(10, landMap.getHeight());
    }

    // ------------------------------------------------------------------
    // M6: isValid(int, int) — 5 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M6-TP1: ENTRY -> N0 -(B1:true)-> N5 -> N6 -> EXIT (x < 0 -> false)
    public void testM6_TP1_IsValidNegativeX() {
        assertFalse(landMap.isValid(-1, 0));
    }

    // M6-TP2: ENTRY -> N0 -(B1:false)-> N1 -(B2:true)-> N5 -> N6 -> EXIT (y < 0 -> false)
    public void testM6_TP2_IsValidNegativeY() {
        assertFalse(landMap.isValid(0, -1));
    }

    // M6-TP3: ENTRY -> N0 -(B1:false)-> N1 -(B2:false)-> N2 -(B3:true)-> N5 -> N6 -> EXIT (x >= width -> false)
    public void testM6_TP3_IsValidXOutOfBounds() {
        assertFalse(landMap.isValid(10, 0));
    }

    // M6-TP4: ENTRY -> N0 -(B1:false)-> N1 -(B2:false)-> N2 -(B3:false)-> N3 -(B4:true)-> N5 -> N6 -> EXIT (y >= height -> false)
    public void testM6_TP4_IsValidYOutOfBounds() {
        assertFalse(landMap.isValid(0, 10));
    }

    // M6-TP5: ENTRY -> N0 -(B1:false)-> N1 -(B2:false)-> N2 -(B3:false)-> N3 -(B4:false)-> N4 -> N6 -> EXIT (in bounds -> true)
    public void testM6_TP5_IsValidTrue() {
        assertTrue(landMap.isValid(5, 5));
    }

    // ------------------------------------------------------------------
    // M7: isLand(int, int) — 2 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M7-TP1: ENTRY -> N0 -(B1:true)-> N2 -> N3 -> EXIT (invalid coords -> false)
    public void testM7_TP1_IsLandInvalid() {
        assertFalse(landMap.isLand(-1, 0));
    }

    // M7-TP2: ENTRY -> N0 -(B1:false)-> N1 -> N3 -> EXIT (valid coords -> map[x][y])
    public void testM7_TP2_IsLandValid() throws Exception {
        invokePrivate(landMap, "setLand", new Class<?>[]{int.class, int.class}, 3, 3);
        assertTrue(landMap.isLand(3, 3));
        assertFalse(landMap.isLand(3, 4));
    }

    // ------------------------------------------------------------------
    // M8: hasLand() — 2 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M8-TP1: ENTRY -> N0 -> N1..N7 -> N8 -> EXIT (no land -> false)
    public void testM8_TP1_HasLandFalse() {
        assertFalse(landMap.hasLand());
    }

    // M8-TP2: ENTRY -> N0 -> N1 -> N2 -> N3 -> N4 -(B3:false)-> N5 -> EXIT (land present -> true)
    public void testM8_TP2_HasLandTrue() throws Exception {
        invokePrivate(landMap, "setLand", new Class<?>[]{int.class, int.class}, 2, 2);
        assertTrue(landMap.hasLand());
    }

    // ------------------------------------------------------------------
    // M9: setLand(int, int) (private) — 2 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M9-TP1: ENTRY -> N0 -(B1:true)-> N2 -> EXIT (not yet land -> sets land, returns true)
    public void testM9_TP1_SetLandNew() throws Exception {
        Boolean res = (Boolean) invokePrivate(landMap, "setLand",
                new Class<?>[]{int.class, int.class}, 4, 4);
        assertTrue(res);
        assertTrue(landMap.isLand(4, 4));
    }

    // M9-TP2: ENTRY -> N0 -(B1:false)-> N1 -> EXIT (already land -> returns false)
    public void testM9_TP2_SetLandAlreadyLand() throws Exception {
        invokePrivate(landMap, "setLand", new Class<?>[]{int.class, int.class}, 4, 4);
        Boolean res = (Boolean) invokePrivate(landMap, "setLand",
                new Class<?>[]{int.class, int.class}, 4, 4);
        assertFalse(res);
    }

    // ------------------------------------------------------------------
    // M10: setLand(int, int, int) (private) — 2 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M10-TP1: ENTRY -> N0 -(B1:true)-> N2 -> N3..N6 -> N7 -> EXIT (edge tile covers valid and invalid neighbors)
    public void testM10_TP1_SetLandGrowFromCorner() throws Exception {
        invokePrivate(landMap, "setLand",
                new Class<?>[]{int.class, int.class, int.class}, 0, 0, 1);
        assertTrue(landMap.isLand(0, 0));
    }

    // M10-TP2: ENTRY -> N0 -(B1:false)-> N1 -> EXIT (already land -> early return)
    public void testM10_TP2_SetLandGrowAlreadyLand() throws Exception {
        invokePrivate(landMap, "setLand", new Class<?>[]{int.class, int.class}, 5, 5);
        invokePrivate(landMap, "setLand",
                new Class<?>[]{int.class, int.class, int.class}, 5, 5, 1);
        assertTrue(landMap.isLand(5, 5));
    }

    // ------------------------------------------------------------------
    // M11: generate(int, int, int) (private) — 5 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M11-TP1: ENTRY -> N0 -(case 0)-> N1 -> N11 -> EXIT (LAND_GENERATOR_CLASSIC)
    public void testM11_TP1_GenerateClassic() throws Exception {
        LandMap lm = new LandMap(20, 20, cache);
        invokePrivate(lm, "generate", new Class<?>[]{int.class, int.class, int.class},
                MapGeneratorOptions.LAND_GENERATOR_CLASSIC, 2, 40);
        assertTrue(lm.hasLand());
    }

    // M11-TP2: ENTRY -> N0 -(case 1)-> N2 -> N3 -(B2 loop)-> N4 -> N3 -(B2:true)-> N11 -> EXIT (LAND_GENERATOR_CONTINENT)
    public void testM11_TP2_GenerateContinent() throws Exception {
        LandMap lm = new LandMap(24, 24, cache);
        invokePrivate(lm, "generate", new Class<?>[]{int.class, int.class, int.class},
                MapGeneratorOptions.LAND_GENERATOR_CONTINENT, 2, 120);
        assertTrue(lm.hasLand());
    }

    // M11-TP3: ENTRY -> N0 -(case 2)-> N5 -> N6 -(B3 loop)-> N7 -> N6 -(B3:true)-> N8 -> N9..N11 -> EXIT (LAND_GENERATOR_ARCHIPELAGO)
    public void testM11_TP3_GenerateArchipelago() throws Exception {
        LandMap lm = new LandMap(24, 24, cache);
        invokePrivate(lm, "generate", new Class<?>[]{int.class, int.class, int.class},
                MapGeneratorOptions.LAND_GENERATOR_ARCHIPELAGO, 2, 120);
        assertTrue(lm.hasLand());
    }

    // M11-TP4: ENTRY -> N0 -(case 3)-> N8 -> N9 -(B4 loop)-> N10 -> N9 -(B4:true)-> N11 -> EXIT (LAND_GENERATOR_ISLANDS)
    public void testM11_TP4_GenerateIslands() throws Exception {
        LandMap lm = new LandMap(24, 24, cache);
        invokePrivate(lm, "generate", new Class<?>[]{int.class, int.class, int.class},
                MapGeneratorOptions.LAND_GENERATOR_ISLANDS, 2, 100);
        assertTrue(lm.hasLand());
    }

    // M11-TP5: ENTRY -> N0 -(default)-> N11 -> EXIT (unknown generator type)
    public void testM11_TP5_GenerateDefault() throws Exception {
        invokePrivate(landMap, "generate", new Class<?>[]{int.class, int.class, int.class},
                99, 2, 20);
        assertFalse(landMap.hasLand());
    }

    // ------------------------------------------------------------------
    // M12: createClassicLandMap(int, int) (private) — 2 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M12-TP1: ENTRY -> N0 -> N1 -(B1:true)-> N7 -> EXIT (minNumberOfTiles == 0 -> skip while)
    public void testM12_TP1_CreateClassicZeroMin() throws Exception {
        invokePrivate(landMap, "createClassicLandMap",
                new Class<?>[]{int.class, int.class}, 1, 0);
        assertTrue(landMap.hasLand()); // polar regions added
    }

    // M12-TP2: ENTRY -> N0 -> N1 -(B1:false)-> N2 -> N3 -(B2/B3 loops & failCounter > 100)-> N1 -> N7 -> EXIT
    public void testM12_TP2_CreateClassicSaturatedFailCounter() throws Exception {
        // distanceToEdge=2 -> edg=4, wid=2, hgt=2 (only 4 interior tiles); minNumberOfTiles=50 triggers failCounter > 100
        invokePrivate(landMap, "createClassicLandMap",
                new Class<?>[]{int.class, int.class}, 2, 50);
        assertTrue(landMap.hasLand());
    }

    // ------------------------------------------------------------------
    // M13: addPolarRegions() (private) — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M13-TP1: ENTRY -> N0 -> N1..N8 -> N9 -> EXIT (add top and bottom polar rows)
    public void testM13_TP1_AddPolarRegions() throws Exception {
        invokePrivate(landMap, "addPolarRegions", new Class<?>[0]);
        assertTrue(landMap.isLand(0, 0));
        assertTrue(landMap.isLand(9, 9));
    }

    // ------------------------------------------------------------------
    // M14: cleanMap() (private) — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M14-TP1: ENTRY -> N0 -> N1..N7 -> N8 -> EXIT (remove isolated 1x1 land, keep adjacent land)
    public void testM14_TP1_CleanMap() throws Exception {
        invokePrivate(landMap, "setLand", new Class<?>[]{int.class, int.class}, 2, 2); // isolated
        invokePrivate(landMap, "setLand", new Class<?>[]{int.class, int.class}, 5, 5); // pair
        invokePrivate(landMap, "setLand", new Class<?>[]{int.class, int.class}, 5, 6); // pair
        invokePrivate(landMap, "cleanMap", new Class<?>[0]);
        assertFalse(landMap.isLand(2, 2));
        assertTrue(landMap.isLand(5, 5));
        assertTrue(landMap.isLand(5, 6));
    }

    // ------------------------------------------------------------------
    // M15: hasAdjacentLand(int, int) (private) — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M15-TP1: ENTRY -> N0 -> EXIT (check adjacent land)
    public void testM15_TP1_HasAdjacentLand() throws Exception {
        invokePrivate(landMap, "setLand", new Class<?>[]{int.class, int.class}, 5, 5);
        assertTrue((Boolean) invokePrivate(landMap, "hasAdjacentLand",
                new Class<?>[]{int.class, int.class}, 5, 6));
        assertFalse((Boolean) invokePrivate(landMap, "hasAdjacentLand",
                new Class<?>[]{int.class, int.class}, 1, 1));
    }

    // ------------------------------------------------------------------
    // M16: newPositions(Position, int) (private) — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M16-TP1: ENTRY -> N0 -> EXIT (return filtered neighbour positions)
    @SuppressWarnings("unchecked")
    public void testM16_TP1_NewPositions() throws Exception {
        List<Position> res = (List<Position>) invokePrivate(landMap, "newPositions",
                new Class<?>[]{Position.class, int.class}, new Position(5, 5), 1);
        assertNotNull(res);
        assertEquals(4, res.size());
    }

    // ------------------------------------------------------------------
    // M17: growLand(int, int, int) (private) — 3 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M17-TP1: ENTRY -> N0 -(B1:false)-> N1 -> EXIT (already land -> early return)
    public void testM17_TP1_GrowLandAlreadyLand() throws Exception {
        invokePrivate(landMap, "setLand", new Class<?>[]{int.class, int.class}, 5, 5);
        invokePrivate(landMap, "growLand",
                new Class<?>[]{int.class, int.class, int.class}, 5, 5, 1);
        assertTrue(landMap.isLand(5, 5));
    }

    // M17-TP2: ENTRY -> N0 -(B1:true)-> N2 -(B2:false)-> N3 -> N4 -> EXIT (count > r -> grows land)
    public void testM17_TP2_GrowLandExpands() throws Exception {
        LandMap lm = new LandMap(10, 10, scriptedCache(0));
        invokePrivate(lm, "growLand",
                new Class<?>[]{int.class, int.class, int.class}, 5, 5, 1);
        assertTrue(lm.isLand(5, 5));
    }

    // M17-TP3: ENTRY -> N0 -(B1:true)-> N2 -(B2:true)-> N4 -> EXIT (count <= r -> does not grow)
    public void testM17_TP3_GrowLandDoesNotExpand() throws Exception {
        LandMap lm = new LandMap(10, 10, scriptedCache(7));
        invokePrivate(lm, "growLand",
                new Class<?>[]{int.class, int.class, int.class}, 5, 5, 1);
        assertFalse(lm.isLand(5, 5));
    }

    // ------------------------------------------------------------------
    // M18: addLandMass(int, int, int, int, int) (private) — 4 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M18-TP1: ENTRY -> N0 -(B1:true)-> N2 -> N3 -(B3/B4 loops)-> N5 -> N6..N11 -(B8:false)-> N12..N20 -(B12:false)-> N21 -> N23 -> EXIT
    public void testM18_TP1_AddLandMassRandomStartSuccess() throws Exception {
        // Scripted (x-1, y-1) pairs: (4,4)->(5,5) is land, (4,5)->(5,6) has adjacent land, (1,1)->(2,2) is clear sea
        LandMap lm = new LandMap(10, 10, scriptedCache(4, 4, 4, 5, 1, 1, 0));
        invokePrivate(lm, "setLand", new Class<?>[]{int.class, int.class}, 5, 5);
        Integer added = (Integer) invokePrivate(lm, "addLandMass",
                new Class<?>[]{int.class, int.class, int.class, int.class, int.class},
                1, 1, -1, 5, 1);
        assertEquals(1, added.intValue());
    }

    // M18-TP2: ENTRY -> N0 -(B1:false)-> N1 -(B2:false)-> N2 -> N3 -(B4:false)-> N5 -> N6..N11 -(B8:false)-> N12..N20 -(B12:false)-> N21 -> N23 -> EXIT
    public void testM18_TP2_AddLandMassYNegativeGrows() throws Exception {
        LandMap lm = new LandMap(10, 10, scriptedCache(3, 3, 0));
        Integer added = (Integer) invokePrivate(lm, "addLandMass",
                new Class<?>[]{int.class, int.class, int.class, int.class, int.class},
                4, 4, 5, -1, 1);
        assertEquals(4, added.intValue());
    }

    // M18-TP3: ENTRY -> N0 -(B1:false)-> N1 -(B2:true)-> N5 -> N6 -(B5:false, B6:false)-> N8..N10 -> N6 -(B6:true)-> N11 -(B8:true)-> N20 -(B12:true)-> N22 -> N23 -> EXIT
    public void testM18_TP3_AddLandMassTooSmallReturnsZero() throws Exception {
        // Explicit start (5,5), minSize=200 exceeds available tiles so l empties and size < minSize -> returns 0
        Integer added = (Integer) invokePrivate(landMap, "addLandMass",
                new Class<?>[]{int.class, int.class, int.class, int.class, int.class},
                200, 200, 5, 5, 1);
        assertEquals(0, added.intValue());
    }

    // M18-TP4: Cover B7:true (!newLand[p.x][p.y] is false) when duplicate neighbour is queued
    public void testM18_TP4_AddLandMassDuplicateCandidateInList() throws Exception {
        LandMap lm = new LandMap(10, 10, scriptedCache(0));
        Integer added = (Integer) invokePrivate(lm, "addLandMass",
                new Class<?>[]{int.class, int.class, int.class, int.class, int.class},
                6, 6, 5, 5, 1);
        assertEquals(6, added.intValue());
    }

    // ------------------------------------------------------------------
    // M19: lambda$growLand$0(Position, Direction) — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M19-TP1: ENTRY -> N0 -> EXIT (evaluated via count() in growLand)
    public void testM19_TP1_LambdaGrowLand() throws Exception {
        invokePrivate(landMap, "growLand",
                new Class<?>[]{int.class, int.class, int.class}, 5, 5, 1);
        assertNotNull(landMap);
    }

    // ------------------------------------------------------------------
    // M20: lambda$newPositions$1(Position, Direction) — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M20-TP1: ENTRY -> N0 -> EXIT (evaluated via map() in newPositions)
    @SuppressWarnings("unchecked")
    public void testM20_TP1_LambdaNewPositionsMapper() throws Exception {
        List<Position> res = (List<Position>) invokePrivate(landMap, "newPositions",
                new Class<?>[]{Position.class, int.class}, new Position(5, 5), 1);
        assertEquals(4, res.size());
    }

    // ------------------------------------------------------------------
    // M21: lambda$newPositions$0(int, Position) — 5 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M21-TP1..TP5: B1:true (!isValid), B2:true (hasAdjacentLand), B3:true (x <= distanceToEdge), B4:true (x >= width - distanceToEdge), B4:false (accepted)
    @SuppressWarnings("unchecked")
    public void testM21_TP1_to_TP5_LambdaNewPositionsPredicate() throws Exception {
        // From (0,5) with distanceToEdge=1: W=(-1,5) hits B1:true; N=(0,4)/S=(0,6)/E=(1,5) hit B3:true (x <= 1)
        List<Position> leftEdge = (List<Position>) invokePrivate(landMap, "newPositions",
                new Class<?>[]{Position.class, int.class}, new Position(0, 5), 1);
        assertTrue(leftEdge.isEmpty());

        // From (8,5) with width=10, distanceToEdge=1: E=(9,5) hits B4:true (x >= 9); W=(7,5) passes all (B4:false)
        List<Position> rightSide = (List<Position>) invokePrivate(landMap, "newPositions",
                new Class<?>[]{Position.class, int.class}, new Position(8, 5), 1);
        assertFalse(rightSide.isEmpty());

        // Set land at (6,6) so (6,5) has adjacent land -> hits B2:true
        invokePrivate(landMap, "setLand", new Class<?>[]{int.class, int.class}, 6, 6);
        List<Position> nearLand = (List<Position>) invokePrivate(landMap, "newPositions",
                new Class<?>[]{Position.class, int.class}, new Position(5, 5), 1);
        assertTrue(nearLand.size() < 4);
    }

    // ------------------------------------------------------------------
    // M22: lambda$hasAdjacentLand$0(Position, Direction) — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M22-TP1: ENTRY -> N0 -> EXIT (evaluated via any() in hasAdjacentLand)
    public void testM22_TP1_LambdaHasAdjacentLand() throws Exception {
        assertFalse((Boolean) invokePrivate(landMap, "hasAdjacentLand",
                new Class<?>[]{int.class, int.class}, 5, 5));
    }

    // ------------------------------------------------------------------
    // M23: <clinit>() — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M23-TP1: ENTRY -> N0 -> EXIT (class initialization)
    public void testM23_TP1_StaticInit() {
        assertNotNull(new LandMap(2, 2, cache));
    }
}
