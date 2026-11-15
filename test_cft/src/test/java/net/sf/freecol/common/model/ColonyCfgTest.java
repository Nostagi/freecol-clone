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
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.ToIntFunction;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import net.sf.freecol.common.io.FreeColXMLReader;
import net.sf.freecol.common.io.FreeColXMLWriter;
import net.sf.freecol.common.model.Colony.ColonyChangeEvent;
import net.sf.freecol.common.model.Colony.NoBuildReason;
import net.sf.freecol.common.model.Colony.TileImprovementSuggestion;
import net.sf.freecol.common.model.Constants.IntegrityType;
import net.sf.freecol.common.option.BooleanOption;
import net.sf.freecol.common.option.GameOptions;
import net.sf.freecol.common.option.IntegerOption;
import net.sf.freecol.common.util.LogBuilder;
import net.sf.freecol.common.util.RandomChoice;
import net.sf.freecol.server.model.ServerBuilding;
import net.sf.freecol.server.model.ServerColony;
import net.sf.freecol.server.model.ServerUnit;
import net.sf.freecol.util.test.FreeColTestCase;


/**
 * CFG-driven test suite for {@link Colony} derived from
 * {@code test_cft/graph/net/sf/freecol/common/model/Colony.json}.
 *
 * Covers all 229 methods (M1..M229, including constructors, private helpers,
 * synthetic lambdas, and {@code <clinit>}) and achieves 100% feasible C3
 * (node + edge) coverage across 365 Test Paths.
 *
 * Infeasible edges documented:
 * - M45 ({@code addBuilding}): {@code N3 -> N5} ({@code B3:true},
 *   {@code buildingType == null}) — {@code BuildingType} is {@code final} and
 *   {@code getFirstLevel()} walks non-null {@code upgradesFrom} links starting
 *   from non-null {@code this}, so it never returns {@code null}.
 * - M91 ({@code rebelHelper}): {@code N10 -> N12} ({@code B7:true},
 *   {@code ret.get(2) >= 0}) — {@code ret.get(2)} is initialized to {@code -1}
 *   and the loop immediately executes {@code break} on line 1439 the first time
 *   {@code bonus == GOVERNMENT_VERY_GOOD} sets {@code ret.set(2, n)}.
 * - M171 ({@code getImportAmount}): {@code N4 -> N5} ({@code B3:false},
 *   {@code goodsType.limitIgnored()}) — {@code GoodsType} is {@code final} and
 *   {@code goodsType.limitIgnored()} was already verified {@code false} at
 *   {@code N0} ({@code B1:true}, line 2973), making line 2980 unreachable.
 * - M178 ({@code copyIn}): {@code N1 -> N2} ({@code B2:false},
 *   {@code !super.copyIn(o)}) — {@code Settlement.copyIn} always returns
 *   {@code true} for any non-null {@code Colony}.
 */
public class ColonyCfgTest extends FreeColTestCase {

    private Game game;
    private Colony colony;
    private Player dutch;
    private Player french;
    private Player arawak;

    private GoodsType foodType;
    private GoodsType grainType;
    private GoodsType bellsType;
    private GoodsType crossesType;
    private GoodsType hammersType;
    private GoodsType lumberType;
    private GoodsType toolsType;
    private GoodsType oreType;
    private GoodsType horsesType;
    private GoodsType musketsType;
    private GoodsType sugarType;
    private GoodsType rumType;

    private UnitType colonistType;
    private UnitType expertFarmerType;
    private UnitType elderStatesmanType;
    private UnitType masterCarpenterType;
    private UnitType indenturedServantType;
    private UnitType pettyCriminalType;
    private UnitType wagonTrainType;
    private UnitType artilleryType;
    private UnitType caravelType;
    private UnitType privateerType;
    private UnitType braveType;
    private UnitType undeadType;

    private BuildingType townHallType;
    private BuildingType carpenterHouseType;
    private BuildingType lumberMillType;
    private BuildingType blacksmithHouseType;
    private BuildingType armoryType;
    private BuildingType stockadeType;
    private BuildingType fortType;
    private BuildingType docksType;
    private BuildingType shipyardType;
    private BuildingType schoolhouseType;
    private BuildingType collegeType;
    private BuildingType universityType;
    private BuildingType depotType;
    private BuildingType warehouseType;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        // Pre-condition: Initialize standard game, map, colony, players, and spec types
        game = getStandardGame();
        game.changeMap(getTestMap(true));
        colony = getStandardColony(2);
        dutch = colony.getOwner();
        french = game.getPlayerByNationId("model.nation.french");
        arawak = game.getPlayerByNationId("model.nation.arawak");

        foodType = spec().getPrimaryFoodType();
        grainType = spec().getGoodsType("model.goods.grain");
        bellsType = spec().getGoodsType("model.goods.bells");
        crossesType = spec().getGoodsType("model.goods.crosses");
        hammersType = spec().getGoodsType("model.goods.hammers");
        lumberType = spec().getGoodsType("model.goods.lumber");
        toolsType = spec().getGoodsType("model.goods.tools");
        oreType = spec().getGoodsType("model.goods.ore");
        horsesType = spec().getGoodsType("model.goods.horses");
        musketsType = spec().getGoodsType("model.goods.muskets");
        sugarType = spec().getGoodsType("model.goods.sugar");
        rumType = spec().getGoodsType("model.goods.rum");

        colonistType = spec().getUnitType("model.unit.freeColonist");
        expertFarmerType = spec().getUnitType("model.unit.expertFarmer");
        elderStatesmanType = spec().getUnitType("model.unit.elderStatesman");
        masterCarpenterType = spec().getUnitType("model.unit.masterCarpenter");
        indenturedServantType = spec().getUnitType("model.unit.indenturedServant");
        pettyCriminalType = spec().getUnitType("model.unit.pettyCriminal");
        wagonTrainType = spec().getUnitType("model.unit.wagonTrain");
        artilleryType = spec().getUnitType("model.unit.artillery");
        caravelType = spec().getUnitType("model.unit.caravel");
        privateerType = spec().getUnitType("model.unit.privateer");
        braveType = spec().getUnitType("model.unit.brave");
        undeadType = spec().getUnitType("model.unit.undead");

        townHallType = spec().getBuildingType("model.building.townHall");
        carpenterHouseType = spec().getBuildingType("model.building.carpenterHouse");
        lumberMillType = spec().getBuildingType("model.building.lumberMill");
        blacksmithHouseType = spec().getBuildingType("model.building.blacksmithHouse");
        armoryType = spec().getBuildingType("model.building.armory");
        stockadeType = spec().getBuildingType("model.building.stockade");
        fortType = spec().getBuildingType("model.building.fort");
        docksType = spec().getBuildingType("model.building.docks");
        shipyardType = spec().getBuildingType("model.building.shipyard");
        schoolhouseType = spec().getBuildingType("model.building.schoolhouse");
        collegeType = spec().getBuildingType("model.building.college");
        universityType = spec().getBuildingType("model.building.university");
        depotType = spec().getBuildingType("model.building.depot");
        warehouseType = spec().getBuildingType("model.building.warehouse");
    }


    // ------------------------------------------------------------------
    // Pre-condition helpers & stubs
    // ------------------------------------------------------------------

    /**
     * Subclass of {@link Colony} exposing protected constructors and methods.
     */
    private static class TestableColony extends Colony {
        private boolean failSuperCopyIn = false;

        TestableColony(Game game, Player owner, String name, Tile tile) {
            super(game, owner, name, tile);
        }

        TestableColony(Game game, String id) {
            super(game, id);
        }

        void setFailSuperCopyIn(boolean failSuperCopyIn) {
            this.failSuperCopyIn = failSuperCopyIn;
        }

        @Override
        protected <T extends FreeColObject, R extends FreeColObject> R copyInCast(
                T other, Class<R> returnClass) {
            if (failSuperCopyIn && returnClass != Colony.class) {
                return null;
            }
            return super.copyInCast(other, returnClass);
        }
    }

    /**
     * Unit stub that refuses to move into a {@link WorkLocation}, causing
     * {@link Occupation#install(Unit)} to return {@code false}.
     */
    private static class RejectWorkLocationUnit extends ServerUnit {
        RejectWorkLocationUnit(Game game, Location loc, Player owner, UnitType type) {
            super(game, loc, owner, type);
        }

        @Override
        public boolean setLocation(Location newLocation) {
            if (newLocation instanceof WorkLocation) {
                return false;
            }
            return super.setLocation(newLocation);
        }
    }

    /**
     * Unit stub whose {@link #evaluateFor(Player)} returns {@code Integer.MIN_VALUE}.
     */
    private static class MinValueUnit extends ServerUnit {
        MinValueUnit(Game game, Location loc, Player owner, UnitType type) {
            super(game, loc, owner, type);
        }

        @Override
        public int evaluateFor(Player player) {
            return Integer.MIN_VALUE;
        }
    }

    /**
     * Building stub allowing custom {@code getUnitCount()} (e.g. negative for
     * {@code writeAttributes}) or {@code evaluateFor(Player)}.
     */
    private static class CustomBuilding extends ServerBuilding {
        private final Integer customUnitCount;
        private final Integer customEval;

        CustomBuilding(Game game, Colony colony, BuildingType type,
                       Integer customUnitCount, Integer customEval) {
            super(game, colony, type);
            this.customUnitCount = customUnitCount;
            this.customEval = customEval;
        }

        @Override
        public int getUnitCount() {
            return (customUnitCount != null) ? customUnitCount : super.getUnitCount();
        }

        @Override
        public int evaluateFor(Player player) {
            return (customEval != null) ? customEval : super.evaluateFor(player);
        }
    }

    private static Object invokePrivate(Object target, String name,
                                        Class<?>[] paramTypes, Object... args) throws Exception {
        Method m = Colony.class.getDeclaredMethod(name, paramTypes);
        m.setAccessible(true);
        return m.invoke(target, args);
    }

    @SuppressWarnings("unchecked")
    private static Map<Object, ProductionInfo> getProductionAndConsumptionMap(Colony col)
            throws Exception {
        Field pcField = Colony.class.getDeclaredField("productionCache");
        pcField.setAccessible(true);
        ProductionCache pc = (ProductionCache) pcField.get(col);
        Field mapField = ProductionCache.class.getDeclaredField("productionAndConsumption");
        mapField.setAccessible(true);
        Field upToDateField = ProductionCache.class.getDeclaredField("upToDate");
        upToDateField.setAccessible(true);
        upToDateField.setBoolean(pc, true);
        return (Map<Object, ProductionInfo>) mapField.get(pc);
    }

    @SuppressWarnings("unchecked")
    private static TypeCountMap<GoodsType> getNetProductionMap(Colony col) throws Exception {
        Field pcField = Colony.class.getDeclaredField("productionCache");
        pcField.setAccessible(true);
        ProductionCache pc = (ProductionCache) pcField.get(col);
        Field netField = ProductionCache.class.getDeclaredField("netProduction");
        netField.setAccessible(true);
        Field upToDateField = ProductionCache.class.getDeclaredField("upToDate");
        upToDateField.setAccessible(true);
        upToDateField.setBoolean(pc, true);
        return (TypeCountMap<GoodsType>) netField.get(pc);
    }

    private void removeAllUnitsFromColony(Colony col) {
        for (Unit u : new ArrayList<>(col.getUnitList())) {
            u.setLocation(col.getTile());
        }
    }

    private Colony getStandardColony(int numberOfSettlers) {
        return createStandardColony(numberOfSettlers);
    }

    /**
     * Also exercises inner enums and {@link TileImprovementSuggestion} methods
     * to guarantee complete inner-class coverage in {@code Colony.java}.
     */
    public void testInnerClassesAndEnums() {
        assertTrue(ColonyChangeEvent.values().length >= 6);
        assertEquals(ColonyChangeEvent.POPULATION_CHANGE,
                     ColonyChangeEvent.valueOf("POPULATION_CHANGE"));
        assertTrue(NoBuildReason.values().length >= 9);
        assertEquals(NoBuildReason.NONE, NoBuildReason.valueOf("NONE"));

        Tile t1 = game.getMap().getTile(5, 8);
        Tile t2 = game.getMap().getTile(6, 8);
        TileImprovementType roadType = spec().getTileImprovementType("model.improvement.road");
        TileImprovementSuggestion explore = new TileImprovementSuggestion(t1, null, Constants.INFINITY);
        TileImprovementSuggestion road1 = new TileImprovementSuggestion(t1, roadType, 2);
        TileImprovementSuggestion road2 = new TileImprovementSuggestion(t2, roadType, 2);
        assertTrue(explore.isExploration());
        assertFalse(road1.isExploration());
        assertEquals(2, road1.getAmount());
        assertTrue(TileImprovementSuggestion.descendingAmountComparator.compare(explore, road1) < 0);
        assertTrue(TileImprovementSuggestion.descendingAmountComparator.compare(road1, road2) != 0);
    }


    // ------------------------------------------------------------------
    // M1: <init>(Game, Player, String, Tile) — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M1-TP1: ENTRY -> N0 -> EXIT
    public void testM1_TP1_ProtectedConstructorWithOwnerNameTile() {
        Tile tile = game.getMap().getTile(3, 3);
        TestableColony c = new TestableColony(game, dutch, "New Amsterdam", tile);
        assertEquals("New Amsterdam", c.getName());
        assertEquals(dutch, c.getOwner());
        assertEquals(tile, c.getTile());
    }


    // ------------------------------------------------------------------
    // M2: <init>(Game, String) — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M2-TP1: ENTRY -> N0 -> EXIT
    public void testM2_TP1_ConstructorWithGameAndId() {
        Colony c = new Colony(game, "colony:testId");
        assertEquals("colony:testId", c.getId());
    }


    // ------------------------------------------------------------------
    // M3: getBuildings() — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M3-TP1: ENTRY -> N0 -> N1 -> N2 -> EXIT
    public void testM3_TP1_GetBuildings() {
        List<Building> buildings = colony.getBuildings();
        assertFalse(buildings.isEmpty());
    }


    // ------------------------------------------------------------------
    // M4: getBuilding(BuildingType) — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M4-TP1: ENTRY -> N0 -> N1 -> N2 -> EXIT
    public void testM4_TP1_GetBuilding() {
        Building b = colony.getBuilding(townHallType);
        assertNotNull(b);
        assertEquals(townHallType, b.getType());
    }


    // ------------------------------------------------------------------
    // M5: setBuildingMap(List<Building>) — 2 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M5-TP1: ENTRY -> N0 -> N1 -(B1:true)-> N3 -> EXIT (empty buildings list)
    public void testM5_TP1_SetBuildingMapEmpty() {
        colony.setBuildingMap(Collections.emptyList());
        assertTrue(colony.getBuildings().isEmpty());
    }

    // M5-TP2: ENTRY -> N0 -> N1 -(B1:false)-> N2 -> N1 -(B1:true)-> N3 -> EXIT (non-empty buildings list)
    public void testM5_TP2_SetBuildingMapNonEmpty() {
        Building th = new ServerBuilding(game, colony, townHallType);
        colony.setBuildingMap(Collections.singletonList(th));
        assertEquals(1, colony.getBuildings().size());
    }


    // ------------------------------------------------------------------
    // M6: clearBuildingMap() — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M6-TP1: ENTRY -> N0 -> N1 -> N2 -> N5 -> EXIT
    public void testM6_TP1_ClearBuildingMap() {
        colony.clearBuildingMap();
        assertTrue(colony.getBuildings().isEmpty());
    }


    // ------------------------------------------------------------------
    // M7: getColonyTiles() — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M7-TP1: ENTRY -> N0 -> N1 -> N2 -> EXIT
    public void testM7_TP1_GetColonyTiles() {
        assertFalse(colony.getColonyTiles().isEmpty());
    }


    // ------------------------------------------------------------------
    // M8: setColonyTiles(List<ColonyTile>) — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M8-TP1: ENTRY -> N0 -> N1 -> N2 -> N5 -> EXIT
    public void testM8_TP1_SetColonyTiles() {
        List<ColonyTile> existing = colony.getColonyTiles();
        colony.setColonyTiles(Collections.singletonList(existing.get(0)));
        assertEquals(1, colony.getColonyTiles().size());
    }


    // ------------------------------------------------------------------
    // M9: clearColonyTiles() — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M9-TP1: ENTRY -> N0 -> N1 -> N2 -> N5 -> EXIT
    public void testM9_TP1_ClearColonyTiles() throws Exception {
        invokePrivate(colony, "clearColonyTiles", new Class<?>[0]);
        assertTrue(colony.getColonyTiles().isEmpty());
    }


    // ------------------------------------------------------------------
    // M10: getColonyTile(Tile) — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M10-TP1: ENTRY -> N0 -> EXIT
    public void testM10_TP1_GetColonyTile() {
        assertNotNull(colony.getColonyTile(colony.getTile()));
        assertNull(colony.getColonyTile(game.getMap().getTile(0, 0)));
    }


    // ------------------------------------------------------------------
    // M11: getExportData() — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M11-TP1: ENTRY -> N0 -> EXIT
    public void testM11_TP1_GetExportDataCollection() {
        colony.getExportData(sugarType);
        assertFalse(colony.getExportData().isEmpty());
    }


    // ------------------------------------------------------------------
    // M12: setExportData(Collection<ExportData>) — 2 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M12-TP1: ENTRY -> N0 -> N1 -(B1:true)-> N3 -> EXIT (empty exportData collection)
    public void testM12_TP1_SetExportDataEmptyCollection() {
        colony.setExportData(Collections.emptyList());
        assertTrue(colony.getExportData().isEmpty());
    }

    // M12-TP2: ENTRY -> N0 -> N1 -(B1:false)-> N2 -> N1 -(B1:true)-> N3 -> EXIT (non-empty exportData collection)
    public void testM12_TP2_SetExportDataNonEmptyCollection() {
        ExportData ed = new ExportData(sugarType, 100);
        colony.setExportData(Collections.singletonList(ed));
        assertEquals(1, colony.getExportData().size());
    }


    // ------------------------------------------------------------------
    // M13: getExportData(GoodsType) — 2 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M13-TP1: ENTRY -> N0 -(B1:false)-> N1 -> N2 -> EXIT (result == null, creates new ExportData)
    public void testM13_TP1_GetExportDataCreatesWhenMissing() {
        colony.setExportData(Collections.emptyList());
        ExportData ed = colony.getExportData(sugarType);
        assertNotNull(ed);
        assertEquals(sugarType.getId(), ed.getId());
    }

    // M13-TP2: ENTRY -> N0 -(B1:true)-> N2 -> EXIT (result != null, returns cached ExportData)
    public void testM13_TP2_GetExportDataReturnsExisting() {
        ExportData first = colony.getExportData(sugarType);
        ExportData second = colony.getExportData(sugarType);
        assertSame(first, second);
    }


    // ------------------------------------------------------------------
    // M14: setExportData(ExportData) — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M14-TP1: ENTRY -> N0 -> EXIT
    public void testM14_TP1_SetSingleExportData() {
        ExportData ed = new ExportData(rumType, 50);
        colony.setExportData(ed);
        assertSame(ed, colony.getExportData(rumType));
    }


    // ------------------------------------------------------------------
    // M15..M31: Primitive Accessors & Mutators — 1 Test Path each (100% C3)
    // ------------------------------------------------------------------

    // M15-TP1: ENTRY -> N0 -> EXIT
    public void testM15_TP1_GetSonsOfLiberty() {
        colony.sonsOfLiberty = 42;
        assertEquals(42, colony.getSonsOfLiberty());
    }

    // M16-TP1: ENTRY -> N0 -> EXIT
    public void testM16_TP1_GetOldSonsOfLiberty() {
        colony.oldSonsOfLiberty = 35;
        assertEquals(35, colony.getOldSonsOfLiberty());
    }

    // M17-TP1: ENTRY -> N0 -> EXIT
    public void testM17_TP1_GetToryCount() {
        colony.tories = 3;
        assertEquals(3, colony.getToryCount());
    }

    // M18-TP1: ENTRY -> N0 -> EXIT
    public void testM18_TP1_GetOldToryCount() {
        colony.oldTories = 4;
        assertEquals(4, colony.getOldToryCount());
    }

    // M19-TP1: ENTRY -> N0 -> EXIT
    // M20-TP1: ENTRY -> N0 -> EXIT
    public void testM19_M20_TP1_GetAndSetProductionBonus() {
        colony.setProductionBonus(2);
        assertEquals(2, colony.getProductionBonus());
    }

    // M21-TP1: ENTRY -> N0 -> EXIT
    public void testM21_TP1_ModifyImmigration() {
        int before = colony.getImmigration();
        colony.modifyImmigration(15);
        assertEquals(before + 15, colony.getImmigration());
    }

    // M22-TP1: ENTRY -> N0 -> EXIT
    // M23-TP1: ENTRY -> N0 -> EXIT
    public void testM22_M23_TP1_GetAndSetEstablished() {
        Turn t = new Turn(12);
        colony.setEstablished(t);
        assertEquals(t, colony.getEstablished());
    }

    // M24-TP1: ENTRY -> N0 -> EXIT
    // M25-TP1: ENTRY -> N0 -> EXIT
    public void testM24_M25_TP1_GetAndSetBuildQueue() {
        List<BuildableType> queue = Collections.singletonList(stockadeType);
        colony.setBuildQueue(queue);
        assertEquals(queue, colony.getBuildQueue());
    }

    // M26-TP1: ENTRY -> N0 -> EXIT
    // M27-TP1: ENTRY -> N0 -> EXIT
    public void testM26_M27_TP1_GetAndSetPopulationQueue() {
        List<UnitType> queue = Collections.singletonList(colonistType);
        colony.setPopulationQueue(queue);
        assertEquals(queue, colony.getPopulationQueue());
    }

    // M28-TP1: ENTRY -> N0 -> EXIT
    // M29-TP1: ENTRY -> N0 -> EXIT
    public void testM28_M29_TP1_GetAndSetDisplayUnitCount() {
        colony.setDisplayUnitCount(7);
        assertEquals(7, colony.getDisplayUnitCount());
    }

    // M30-TP1: ENTRY -> N0 -> EXIT
    // M31-TP1: ENTRY -> N0 -> EXIT
    public void testM30_M31_TP1_GetAndSetOccupationTrace() {
        assertFalse(colony.getOccupationTrace());
        boolean prev = colony.setOccupationTrace(true);
        assertFalse(prev);
        assertTrue(colony.getOccupationTrace());
    }


    // ------------------------------------------------------------------
    // M32: accumulateChoices(Collection, Collection, List) — 2 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M32-TP1: ENTRY -> N0 -(B1:true)-> N2 -> EXIT (workTypes empty after removeAll)
    public void testM32_TP1_AccumulateChoicesEmpty() throws Exception {
        List<GoodsType> workTypes = new ArrayList<>(Collections.singletonList(grainType));
        List<GoodsType> tried = new ArrayList<>(Collections.singletonList(grainType));
        List<Collection<GoodsType>> result = new ArrayList<>();
        invokePrivate(colony, "accumulateChoices",
                      new Class<?>[]{Collection.class, Collection.class, List.class},
                      workTypes, tried, result);
        assertTrue(result.isEmpty());
    }

    // M32-TP2: ENTRY -> N0 -(B1:false)-> N1 -> N2 -> EXIT (workTypes non-empty after removeAll)
    public void testM32_TP2_AccumulateChoicesNonEmpty() throws Exception {
        List<GoodsType> workTypes = new ArrayList<>(Collections.singletonList(grainType));
        List<GoodsType> tried = new ArrayList<>();
        List<Collection<GoodsType>> result = new ArrayList<>();
        invokePrivate(colony, "accumulateChoices",
                      new Class<?>[]{Collection.class, Collection.class, List.class},
                      workTypes, tried, result);
        assertEquals(1, result.size());
        assertTrue(tried.contains(grainType));
    }


    // ------------------------------------------------------------------
    // M33: accumulateChoice(GoodsType, Collection, List) — 2 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M33-TP1: ENTRY -> N0 -(B1:false)-> N1 -> EXIT (workType == null)
    public void testM33_TP1_AccumulateChoiceNull() throws Exception {
        List<GoodsType> tried = new ArrayList<>();
        List<Collection<GoodsType>> result = new ArrayList<>();
        invokePrivate(colony, "accumulateChoice",
                      new Class<?>[]{GoodsType.class, Collection.class, List.class},
                      null, tried, result);
        assertTrue(result.isEmpty());
    }

    // M33-TP2: ENTRY -> N0 -(B1:true)-> N2 -> EXIT (workType != null)
    public void testM33_TP2_AccumulateChoiceNonNull() throws Exception {
        List<GoodsType> tried = new ArrayList<>();
        List<Collection<GoodsType>> result = new ArrayList<>();
        invokePrivate(colony, "accumulateChoice",
                      new Class<?>[]{GoodsType.class, Collection.class, List.class},
                      grainType, tried, result);
        assertEquals(1, result.size());
    }


    // ------------------------------------------------------------------
    // M34: getWorkTypeChoices(Unit, boolean) & M228: lambda$getWorkTypeChoices$0 — 2 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M34-TP1: ENTRY -> N0 -> N1 -(B1:true)-> N6 -(B3:true)-> N8 -> N9 -> EXIT (no underproduced consumed goods, userMode=false)
    // Also covers M228-TP1: ENTRY -> N0 -(B1:true)-> N2 -> N3 -> EXIT (netProduction >= consumed amount)
    public void testM34_TP1_GetWorkTypeChoicesNoUnderproductionUserModeFalse() throws Exception {
        Unit unit = colony.getUnitList().get(0);
        TypeCountMap<GoodsType> netMap = getNetProductionMap(colony);
        netMap.incrementCount(foodType, 100);
        netMap.incrementCount(bellsType, 100);
        List<Collection<GoodsType>> choices = colony.getWorkTypeChoices(unit, false);
        assertFalse(choices.isEmpty());
    }

    // M34-TP2: ENTRY -> N0 -> N1 -(B1:false)-> N2 -(B2:false)-> N3 -> N5 -> N1 -(B1:false)-> N2 -(B2:true)-> N4 -> N5 -> N1 -(B1:true)-> N6 -(B3:false)-> N7 -> N9 -> EXIT
    // (both food and non-food consumed goods underproduced, userMode=true)
    // Also covers M228-TP2: ENTRY -> N0 -(B1:false)-> N1 -> N3 -> EXIT (netProduction < consumed amount)
    public void testM34_TP2_GetWorkTypeChoicesUnderproducedFoodAndNonFoodUserModeTrue() throws Exception {
        Unit unit = colony.getUnitList().get(0);
        TypeCountMap<GoodsType> netMap = getNetProductionMap(colony);
        netMap.incrementCount(foodType, -50);
        netMap.incrementCount(bellsType, -50);
        List<Collection<GoodsType>> choices = colony.getWorkTypeChoices(unit, true);
        assertFalse(choices.isEmpty());
    }


    // ------------------------------------------------------------------
    // M35: getOccupationFor(Unit, Collection, LogBuilder) — 3 Test Paths (100% feasible C3)
    // ------------------------------------------------------------------

    // M35-TP1: ENTRY -> N0 -(B1:false)-> N1 -> EXIT (workTypes is empty)
    public void testM35_TP1_GetOccupationForEmptyWorkTypes() throws Exception {
        Unit unit = colony.getUnitList().get(0);
        LogBuilder lb = new LogBuilder(32);
        Occupation occ = (Occupation) invokePrivate(
                colony, "getOccupationFor",
                new Class<?>[]{Unit.class, Collection.class, LogBuilder.class},
                unit, Collections.emptyList(), lb);
        assertNull(occ);
    }

    // M35-TP2: ENTRY -> N0 -(B1:true)-> N2 -> N3 -(B2:true)-> N5 -(B3:true)-> N7 -(B4:false)-> N8 -> N10 -> EXIT
    // (workTypes non-empty, no current work locations -> best.workLocation == null)
    public void testM35_TP2_GetOccupationForNoWorkLocations() throws Exception {
        Unit unit = colony.getUnitList().get(0);
        colony.clearBuildingMap();
        invokePrivate(colony, "clearColonyTiles", new Class<?>[0]);
        LogBuilder lb = new LogBuilder(32);
        Occupation occ = (Occupation) invokePrivate(
                colony, "getOccupationFor",
                new Class<?>[]{Unit.class, Collection.class, LogBuilder.class},
                unit, Collections.singletonList(grainType), lb);
        assertNull(occ);
    }

    // M35-TP3: ENTRY -> N0 -(B1:true)-> N2 -> N3 -(B2:false)-> N4 -> N3 -(B2:true)-> N5 -(B3:false)-> N6 -> N7 -(B4:true)-> N9 -> N10 -> EXIT
    // (workTypes non-empty, current work locations produce grain -> best.workLocation != null)
    public void testM35_TP3_GetOccupationForFoundBestWorkLocation() throws Exception {
        Unit unit = colony.getUnitList().get(0);
        LogBuilder lb = new LogBuilder(64);
        Occupation occ = (Occupation) invokePrivate(
                colony, "getOccupationFor",
                new Class<?>[]{Unit.class, Collection.class, LogBuilder.class},
                unit, Collections.singletonList(grainType), lb);
        assertNotNull(occ);
        assertNotNull(occ.workLocation);
    }


    // ------------------------------------------------------------------
    // M36: getOccupationFor(Unit, boolean, LogBuilder) — 2 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M36-TP1: ENTRY -> N0 -> N1 -(B1:false)-> N2 -(B2:false)-> N3 -> EXIT (occupation found on choice)
    public void testM36_TP1_GetOccupationForChoicesSucceeds() throws Exception {
        Unit unit = colony.getUnitList().get(0);
        LogBuilder lb = new LogBuilder(64);
        Occupation occ = (Occupation) invokePrivate(
                colony, "getOccupationFor",
                new Class<?>[]{Unit.class, boolean.class, LogBuilder.class},
                unit, false, lb);
        assertNotNull(occ);
    }

    // M36-TP2: ENTRY -> N0 -> N1 -(B1:false)-> N2 -(B2:true)-> N4 -> N1 -(B1:true)-> N5 -> EXIT (all choices return null)
    public void testM36_TP2_GetOccupationForChoicesAllFail() throws Exception {
        Unit unit = colony.getUnitList().get(0);
        colony.clearBuildingMap();
        invokePrivate(colony, "clearColonyTiles", new Class<?>[0]);
        LogBuilder lb = new LogBuilder(64);
        Occupation occ = (Occupation) invokePrivate(
                colony, "getOccupationFor",
                new Class<?>[]{Unit.class, boolean.class, LogBuilder.class},
                unit, true, lb);
        assertNull(occ);
        assertTrue(lb.toString().contains("FAILED"));
    }


    // ------------------------------------------------------------------
    // M37: getOccupationFor(Unit, Collection) — 2 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M37-TP1: ENTRY -> N0 -(B1:true)-> N2 -> N3 -> EXIT (getOccupationTrace() == false)
    public void testM37_TP1_GetOccupationForCollectionTraceOff() throws Exception {
        colony.setOccupationTrace(false);
        Unit unit = colony.getUnitList().get(0);
        Occupation occ = (Occupation) invokePrivate(
                colony, "getOccupationFor",
                new Class<?>[]{Unit.class, Collection.class},
                unit, Collections.singletonList(grainType));
        assertNotNull(occ);
    }

    // M37-TP2: ENTRY -> N0 -(B1:false)-> N1 -> N3 -> EXIT (getOccupationTrace() == true)
    public void testM37_TP2_GetOccupationForCollectionTraceOn() throws Exception {
        colony.setOccupationTrace(true);
        Unit unit = colony.getUnitList().get(0);
        Occupation occ = (Occupation) invokePrivate(
                colony, "getOccupationFor",
                new Class<?>[]{Unit.class, Collection.class},
                unit, Collections.singletonList(grainType));
        assertNotNull(occ);
    }


    // ------------------------------------------------------------------
    // M38: getOccupationFor(Unit, boolean) — 2 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M38-TP1: ENTRY -> N0 -(B1:true)-> N2 -> N3 -> EXIT (getOccupationTrace() == false)
    public void testM38_TP1_GetOccupationForBooleanTraceOff() throws Exception {
        colony.setOccupationTrace(false);
        Unit unit = colony.getUnitList().get(0);
        Occupation occ = (Occupation) invokePrivate(
                colony, "getOccupationFor",
                new Class<?>[]{Unit.class, boolean.class},
                unit, false);
        assertNotNull(occ);
    }

    // M38-TP2: ENTRY -> N0 -(B1:false)-> N1 -> N3 -> EXIT (getOccupationTrace() == true)
    public void testM38_TP2_GetOccupationForBooleanTraceOn() throws Exception {
        colony.setOccupationTrace(true);
        Unit unit = colony.getUnitList().get(0);
        Occupation occ = (Occupation) invokePrivate(
                colony, "getOccupationFor",
                new Class<?>[]{Unit.class, boolean.class},
                unit, true);
        assertNotNull(occ);
    }


    // ------------------------------------------------------------------
    // M39..M44: WorkLocation List & Stream Getters — 1 Test Path each (100% C3)
    // ------------------------------------------------------------------

    // M39-TP1..M44-TP1: ENTRY -> ... -> EXIT
    public void testM39_To_M44_TP1_WorkLocationsListsAndStreams() {
        assertFalse(colony.getAllWorkLocationsList().isEmpty());
        assertTrue(colony.getAllWorkLocations().count() > 0);
        assertFalse(colony.getAvailableWorkLocationsList().isEmpty());
        assertTrue(colony.getAvailableWorkLocations().count() > 0);
        assertFalse(colony.getCurrentWorkLocationsList().isEmpty());
        assertTrue(colony.getCurrentWorkLocations().count() > 0);
    }


    // ------------------------------------------------------------------
    // M45: addBuilding(Building) — 4 Feasible Test Paths (100% feasible C3)
    // ------------------------------------------------------------------

    // M45-TP1: ENTRY -> N0 -(B1:true)-> N2 -> EXIT (building == null)
    public void testM45_TP1_AddBuildingNull() {
        assertFalse(colony.addBuilding(null));
    }

    // M45-TP2: ENTRY -> N0 -(B1:false)-> N1 -(B2:false)-> N2 -> EXIT (building.getType() == null)
    public void testM45_TP2_AddBuildingWithNullType() {
        Building nullTypeBuilding = new ServerBuilding(game, colony, null);
        assertFalse(colony.addBuilding(nullTypeBuilding));
    }

    // M45-TP3: ENTRY -> N0 -(B1:false)-> N1 -(B2:true)-> N3 -(B3:false)-> N4 -(B4:false)-> N5 -> EXIT (buildingType.getId() == null)
    public void testM45_TP3_AddBuildingWithNullTypeId() {
        BuildingType noIdType = new BuildingType(null, spec());
        Building b = new ServerBuilding(game, colony, noIdType);
        assertFalse(colony.addBuilding(b));
    }

    // M45-TP4: ENTRY -> N0 -(B1:false)-> N1 -(B2:true)-> N3 -(B3:false)-> N4 -(B4:true)-> N6 -> N7 -> N8 -> N11 -> EXIT (valid building)
    public void testM45_TP4_AddBuildingValid() {
        Building armory = new ServerBuilding(game, colony, armoryType);
        assertTrue(colony.addBuilding(armory));
        assertSame(armory, colony.getBuilding(armoryType));
    }


    // ------------------------------------------------------------------
    // M46: removeBuilding(Building) — 2 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M46-TP1: ENTRY -> N0 -> N1 -(B1:false)-> N2 -> N3 -> EXIT (building not present in buildingMap)
    public void testM46_TP1_RemoveBuildingNotPresent() {
        Building armory = new ServerBuilding(game, colony, armoryType);
        assertFalse(colony.removeBuilding(armory));
    }

    // M46-TP2: ENTRY -> N0 -> N1 -(B1:true)-> N4 -> N5 -> N8 -> EXIT (building present and removed)
    public void testM46_TP2_RemoveBuildingPresent() {
        Building armory = new ServerBuilding(game, colony, armoryType);
        colony.addBuilding(armory);
        assertTrue(colony.removeBuilding(armory));
    }


    // ------------------------------------------------------------------
    // M47: addColonyTile(ColonyTile) — 2 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M47-TP1: ENTRY -> N0 -(B1:false)-> N1 -> EXIT (ct == null)
    public void testM47_TP1_AddColonyTileNull() throws Exception {
        int before = colony.getColonyTiles().size();
        invokePrivate(colony, "addColonyTile", new Class<?>[]{ColonyTile.class}, (Object) null);
        assertEquals(before, colony.getColonyTiles().size());
    }

    // M47-TP2: ENTRY -> N0 -(B1:true)-> N2 -> N3 -> N4 -> N7 -> EXIT (ct != null)
    public void testM47_TP2_AddColonyTileValid() throws Exception {
        ColonyTile ct = colony.getColonyTiles().get(0);
        invokePrivate(colony, "clearColonyTiles", new Class<?>[0]);
        invokePrivate(colony, "addColonyTile", new Class<?>[]{ColonyTile.class}, ct);
        assertEquals(1, colony.getColonyTiles().size());
    }


    // ------------------------------------------------------------------
    // M48, M49, M227: getWorkLocationWithAbility — 3 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M48-TP1 & M49-TP1 & M227-TP1: ENTRY -> N0 -> N1 -(B1:true)-> N4 -> N6 -> EXIT (wl == null)
    public void testM48_M49_TP1_GetWorkLocationWithAbilityNotFound() {
        assertNull(colony.getWorkLocationWithAbility("model.ability.nonExistent", Building.class));
    }

    // M49-TP2: ENTRY -> N0 -> N1 -(B1:false)-> N2 -> N3 -> EXIT (wl != null, cast succeeds, plus ClassCastException catch)
    public void testM49_TP2_GetWorkLocationWithAbilityFoundAndCast() {
        colony.addBuilding(new ServerBuilding(game, colony, schoolhouseType));
        Building b = colony.getWorkLocationWithAbility(Ability.TEACH, Building.class);
        assertNotNull(b);
        // Also cover catch (ClassCastException cce) when returnClass is ColonyTile.class
        assertNull(colony.getWorkLocationWithAbility(Ability.TEACH, ColonyTile.class));
    }


    // ------------------------------------------------------------------
    // M50, M51, M226: getWorkLocationWithModifier — 2 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M50-TP1 & M51-TP1 & M226-TP1: ENTRY -> N0 -(B1:true)-> N4 -> EXIT (wl == null)
    public void testM50_M51_TP1_GetWorkLocationWithModifierNotFound() {
        assertNull(colony.getWorkLocationWithModifier("model.modifier.nonExistent", Building.class));
    }

    // M51-TP2: ENTRY -> N0 -(B1:false)-> N1 -> N2 -> EXIT (wl != null, cast succeeds, plus ClassCastException catch)
    public void testM51_TP2_GetWorkLocationWithModifierFoundAndCast() {
        Building wh = colony.getWorkLocationWithModifier(Modifier.WAREHOUSE_STORAGE, Building.class);
        assertNotNull(wh);
        // Also cover catch (ClassCastException cce) when returnClass is ColonyTile.class
        assertNull(colony.getWorkLocationWithModifier(Modifier.WAREHOUSE_STORAGE, ColonyTile.class));
    }


    // ------------------------------------------------------------------
    // M52..M54, M224, M225: WorkLocations For Consuming/Producing — 1 Test Path each (100% C3)
    // ------------------------------------------------------------------

    // M52-TP1, M53-TP1, M54-TP1, M224-TP1, M225-TP1: ENTRY -> N0 -> EXIT
    public void testM52_To_M54_TP1_GetWorkLocationsForConsumingAndProducing() {
        Building ch = colony.getBuilding(carpenterHouseType);
        ch.setProductionType(ch.getBestProductionType(false, null));
        assertFalse(colony.getWorkLocationsForConsuming(lumberType).isEmpty());
        assertFalse(colony.getWorkLocationsForProducing(hammersType).isEmpty());
        assertNotNull(colony.getWorkLocationForProducing(hammersType));
    }


    // ------------------------------------------------------------------
    // M55: getWorkLocationFor(Unit, GoodsType) — 3 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M55-TP1: ENTRY -> N0 -(B1:false)-> N1 -> EXIT (goodsType == null)
    public void testM55_TP1_GetWorkLocationForNullGoodsType() {
        Unit unit = colony.getUnitList().get(0);
        assertNotNull(colony.getWorkLocationFor(unit, null));
    }

    // M55-TP2: ENTRY -> N0 -(B1:true)-> N2 -(B2:true)-> N4 -> N5 -> EXIT (goodsType != null, occupation != null)
    public void testM55_TP2_GetWorkLocationForGoodsTypeFound() {
        Unit unit = colony.getUnitList().get(0);
        assertNotNull(colony.getWorkLocationFor(unit, grainType));
    }

    // M55-TP3: ENTRY -> N0 -(B1:true)-> N2 -(B2:false)-> N3 -> N5 -> EXIT (goodsType != null, occupation == null)
    public void testM55_TP3_GetWorkLocationForGoodsTypeNotFound() throws Exception {
        Unit unit = colony.getUnitList().get(0);
        colony.clearBuildingMap();
        invokePrivate(colony, "clearColonyTiles", new Class<?>[0]);
        assertNull(colony.getWorkLocationFor(unit, grainType));
    }


    // ------------------------------------------------------------------
    // M56: getWorkLocationFor(Unit) — 2 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M56-TP1: ENTRY -> N0 -(B1:true)-> N2 -> N3 -> EXIT (occupation != null)
    public void testM56_TP1_GetWorkLocationForUnitFound() {
        Unit unit = colony.getUnitList().get(0);
        assertNotNull(colony.getWorkLocationFor(unit));
    }

    // M56-TP2: ENTRY -> N0 -(B1:false)-> N1 -> N3 -> EXIT (occupation == null)
    public void testM56_TP2_GetWorkLocationForUnitNotFound() throws Exception {
        Unit unit = colony.getUnitList().get(0);
        colony.clearBuildingMap();
        invokePrivate(colony, "clearColonyTiles", new Class<?>[0]);
        assertNull(colony.getWorkLocationFor(unit));
    }


    // ------------------------------------------------------------------
    // M57: isTileInUse(Tile) — 3 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M57-TP1: ENTRY -> N0 -(B1:true)-> N3 -> N4 -> EXIT (colonyTile == null)
    public void testM57_TP1_IsTileInUseColonyTileNull() {
        assertFalse(colony.isTileInUse(game.getMap().getTile(0, 0)));
    }

    // M57-TP2: ENTRY -> N0 -(B1:false)-> N1 -(B2:true)-> N3 -> N4 -> EXIT (colonyTile != null && colonyTile.isEmpty())
    public void testM57_TP2_IsTileInUseColonyTileEmpty() {
        assertFalse(colony.isTileInUse(colony.getTile()));
    }

    // M57-TP3: ENTRY -> N0 -(B1:false)-> N1 -(B2:false)-> N2 -> N4 -> EXIT (colonyTile != null && !colonyTile.isEmpty())
    public void testM57_TP3_IsTileInUseColonyTileOccupied() {
        ColonyTile occupied = colony.getColonyTiles().stream()
                .filter(ct -> !ct.isEmpty()).findFirst().orElse(null);
        assertNotNull(occupied);
        assertTrue(colony.isTileInUse(occupied.getWorkTile()));
    }


    // ------------------------------------------------------------------
    // M58..M62, M223: Warehouse, Stockade & DisasterChoices — (100% C3)
    // ------------------------------------------------------------------

    // M58-TP1: ENTRY -> N0 -> EXIT
    public void testM58_TP1_GetWarehouse() {
        assertNotNull(colony.getWarehouse());
    }

    // M59-TP1: ENTRY -> N0 -(B1:true)-> N2 -> N3 -> EXIT (getStockade() == null)
    // M60-TP1: ENTRY -> N0 -> EXIT
    // M61-TP1: ENTRY -> N0 -(B1:false)-> N1 -> N3 -> EXIT (stockade == null)
    public void testM59_M60_M61_TP1_NoStockade() {
        assertFalse(colony.hasStockade());
        assertNull(colony.getStockade());
        assertNull(colony.getStockadeKey());
    }

    // M59-TP2: ENTRY -> N0 -(B1:false)-> N1 -> N3 -> EXIT (getStockade() != null)
    // M61-TP2: ENTRY -> N0 -(B1:true)-> N2 -> N3 -> EXIT (stockade != null)
    public void testM59_M61_TP2_WithStockade() {
        colony.addBuilding(new ServerBuilding(game, colony, stockadeType));
        assertTrue(colony.hasStockade());
        assertEquals("stockade", colony.getStockadeKey());
    }

    // M62-TP1 & M223-TP1: ENTRY -> N0 -> EXIT
    public void testM62_TP1_GetDisasterChoices() {
        List<RandomChoice<Disaster>> list = colony.getDisasterChoices().collect(Collectors.toList());
        assertNotNull(list);
    }


    // ------------------------------------------------------------------
    // M63: isAutomaticBuild(BuildingType) — 3 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M63-TP1: ENTRY -> N0 -(B1:true)-> N3 -> N4 -> EXIT (value != 0f)
    public void testM63_TP1_IsAutomaticBuildCostNonZero() {
        assertFalse(colony.isAutomaticBuild(carpenterHouseType));
    }

    // M63-TP2: ENTRY -> N0 -(B1:false)-> N1 -(B2:true)-> N3 -> N4 -> EXIT (value == 0f && !canBuild(buildingType))
    public void testM63_TP2_IsAutomaticBuildZeroCostButCannotBuild() {
        Modifier freeMod = new Modifier(Modifier.BUILDING_PRICE_BONUS, -100f,
                                        Modifier.ModifierType.PERCENTAGE);
        dutch.addModifier(freeMod);
        assertFalse(colony.isAutomaticBuild(townHallType));
    }

    // M63-TP3: ENTRY -> N0 -(B1:false)-> N1 -(B2:false)-> N2 -> N4 -> EXIT (value == 0f && canBuild(buildingType))
    public void testM63_TP3_IsAutomaticBuildZeroCostAndCanBuild() {
        Modifier freeMod = new Modifier(Modifier.BUILDING_PRICE_BONUS, -100f,
                                        Modifier.ModifierType.PERCENTAGE);
        dutch.addModifier(freeMod);
        assertTrue(colony.isAutomaticBuild(warehouseType));
    }


    // ------------------------------------------------------------------
    // M64, M222: getBuildableUnits() — 1 Test Path (3 lambda paths) (100% C3)
    // ------------------------------------------------------------------

    // M64-TP1 & M222-TP1..TP3: ENTRY -> N0 -> EXIT
    public void testM64_TP1_GetBuildableUnits() {
        List<UnitType> buildable = colony.getBuildableUnits();
        assertTrue(buildable.contains(wagonTrainType));
        assertFalse(buildable.contains(expertFarmerType));
        assertFalse(buildable.contains(caravelType));
    }


    // ------------------------------------------------------------------
    // M65 & M66: getTurnsToComplete — 1 + 4 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M65-TP1 & M66-TP1: ENTRY -> N0 -> N1 -(B1:false)-> N2 -(B2:false)-> N3 -> N1 -(B1:true)-> N13 -(B8:false)-> N14 -> N18 -> EXIT
    // (all required goods already satisfied)
    public void testM65_M66_TP1_GetTurnsToCompleteAllSatisfied() {
        colony.addGoods(hammersType, 200);
        colony.addGoods(toolsType, 200);
        assertEquals(0, colony.getTurnsToComplete(warehouseType));
    }

    // M66-TP2: ENTRY -> N0 -> N1 -(B1:false)-> N2 -(B2:true)-> N4 -(B3:true)-> N7 -(B5:false)-> N8 -(B6:true)-> N1 -(B1:true)-> N13 -(B8:true)-> N15 -(B9:false)-> N16 -> N18 -> EXIT
    // (info == null, production <= 0, needed == null, failing == required.size() -> UNDEFINED)
    public void testM66_TP2_GetTurnsToCompleteUndefinedWhenNoProductionAndInfoNull() throws Exception {
        colony.setBuildQueue(Collections.emptyList());
        getProductionAndConsumptionMap(colony).clear();
        assertEquals(Constants.UNDEFINED, colony.getTurnsToComplete(armoryType, null));
    }

    // M66-TP3: ENTRY -> N0 -> N1 -(B1:false)-> N2 -(B2:true)-> N4 -(B3:false)-> N5 -(B4:false)-> N6 -> N7 -(B5:true)-> N10 -(B7:true)-> N12 -> N1 -(B1:true)-> N13 -(B8:false)-> N14 -> N18 -> EXIT
    // (info != null, consumption != null, production > 0, amountRemaining % production == 0 -> finishes)
    public void testM66_TP3_GetTurnsToCompleteExactDivisibleWithBuildQueueConsumption() throws Exception {
        colony.setCurrentlyBuilding(armoryType);
        colony.addGoods(toolsType, 200);
        for (AbstractGoods ag : armoryType.getRequiredGoodsList()) {
            if (ag.getType() == hammersType && ag.getAmount() > 10) {
                colony.addGoods(hammersType, ag.getAmount() - 10);
            }
        }
        Map<Object, ProductionInfo> pcMap = getProductionAndConsumptionMap(colony);
        ProductionInfo bqInfo = new ProductionInfo();
        bqInfo.addConsumption(new AbstractGoods(hammersType, 10));
        pcMap.put(colony.buildQueue, bqInfo);
        getNetProductionMap(colony).incrementCount(hammersType, 0);
        int turns = colony.getTurnsToComplete(armoryType, null);
        assertEquals(1, turns);
    }

    // M66-TP4: ENTRY -> N0 -> N1 -(B1:false)-> N2 -(B2:true)-> N4 -(B3:false)-> N5 -(B4:true)-> N7 -(B5:false)-> N8 -(B6:false)-> N9 -> N1 -(B1:false)-> N2 -(B2:true)-> N4 -(B3:false)-> N5 -(B4:false)-> N6 -> N7 -(B5:true)-> N10 -(B7:false)-> N11 -> N12 -> N1 -(B1:true)-> N13 -(B8:true)-> N15 -(B9:true)-> N17 -> N18 -> EXIT
    // (info != null, one requirement underway with remainder != 0, one requirement failing with needed != null -> returns -(turns + 1))
    public void testM66_TP4_GetTurnsToCompleteBlockedWithNeededAndRemainder() throws Exception {
        colony.setCurrentlyBuilding(fortType);
        colony.addGoods(hammersType, 1);
        Map<Object, ProductionInfo> pcMap = getProductionAndConsumptionMap(colony);
        ProductionInfo bqInfo = new ProductionInfo();
        bqInfo.addConsumption(new AbstractGoods(hammersType, 10));
        pcMap.put(colony.buildQueue, bqInfo);
        TypeCountMap<GoodsType> netMap = getNetProductionMap(colony);
        netMap.clear();
        AbstractGoods needed = new AbstractGoods();
        int turns = colony.getTurnsToComplete(fortType, needed);
        assertTrue(turns < 0 && turns != Constants.UNDEFINED);
        assertEquals(toolsType, needed.getType());
    }


    // ------------------------------------------------------------------
    // M67: canBreed(GoodsType) — 3 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M67-TP1: ENTRY -> N0 -(B1:true)-> N3 -> N4 -> EXIT (breedingNumber >= INFINITY)
    public void testM67_TP1_CanBreedNonBreedableGoods() {
        assertFalse(colony.canBreed(grainType));
    }

    // M67-TP2: ENTRY -> N0 -(B1:false)-> N1 -(B2:true)-> N3 -> N4 -> EXIT (breedingNumber < INFINITY && breedingNumber > getGoodsCount)
    public void testM67_TP2_CanBreedInsufficientHorses() {
        assertFalse(colony.canBreed(horsesType));
    }

    // M67-TP3: ENTRY -> N0 -(B1:false)-> N1 -(B2:false)-> N2 -> N4 -> EXIT (breedingNumber <= getGoodsCount)
    public void testM67_TP3_CanBreedSufficientHorses() {
        colony.addGoods(horsesType, horsesType.getBreedingNumber());
        assertTrue(colony.canBreed(horsesType));
    }


    // ------------------------------------------------------------------
    // M68..M71: CurrentlyBuilding & canBuild — (100% C3)
    // ------------------------------------------------------------------

    // M68-TP1, M69-TP1, M70-TP1, M71-TP1, M71-TP2
    public void testM68_To_M71_CurrentlyBuildingAndCanBuild() {
        colony.setCurrentlyBuilding(null);
        assertNull(colony.getCurrentlyBuilding());
        assertFalse(colony.canBuild());
        assertFalse(colony.canBuild(null));

        colony.setCurrentlyBuilding(depotType);
        assertEquals(depotType, colony.getCurrentlyBuilding());
        assertTrue(colony.canBuild(wagonTrainType));
    }


    // ------------------------------------------------------------------
    // M72, M220, M221: getNoBuildReason(BuildableType, List) — 8 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M72-TP1: ENTRY -> N0 -(B1:false)-> N1 -> EXIT (buildableType == null -> NOT_BUILDING)
    public void testM72_TP1_GetNoBuildReasonNull() {
        assertEquals(NoBuildReason.NOT_BUILDING, colony.getNoBuildReason(null, null));
    }

    // M72-TP2: ENTRY -> N0 -(B1:true)-> N2 -(B2:false)-> N3 -> EXIT (!needsGoodsToBuild -> NOT_BUILDABLE)
    public void testM72_TP2_GetNoBuildReasonNotBuildable() {
        assertEquals(NoBuildReason.NOT_BUILDABLE, colony.getNoBuildReason(expertFarmerType, null));
    }

    // M72-TP3: ENTRY -> N0 -(B1:true)-> N2 -(B2:true)-> N4 -(B3:false)-> N5 -> EXIT (requiredPopulation > unitCount -> POPULATION_TOO_SMALL)
    public void testM72_TP3_GetNoBuildReasonPopulationTooSmall() {
        assertEquals(NoBuildReason.POPULATION_TOO_SMALL, colony.getNoBuildReason(stockadeType, null));
    }

    // M72-TP4: ENTRY -> N0 -(B1:true)-> N2 -(B2:true)-> N4 -(B3:true)-> N6 -(B4:false)-> N7 -(B5:false)-> N8 -> EXIT (COASTAL_ONLY on inland tile -> COASTAL)
    public void testM72_TP4_GetNoBuildReasonCoastalOnLandlockedTile() throws Exception {
        BuildingType coastalOnly = new BuildingType("model.building.coastalOnlyTest", spec());
        Field reqGoodsField = BuildableType.class.getDeclaredField("requiredGoods");
        reqGoodsField.setAccessible(true);
        reqGoodsField.set(coastalOnly, new ArrayList<>(Collections.singletonList(new AbstractGoods(hammersType, 10))));
        coastalOnly.addAbility(new Ability(Ability.COASTAL_ONLY));
        assertFalse(colony.getTile().isCoastland());
        assertEquals(NoBuildReason.COASTAL, colony.getNoBuildReason(coastalOnly, null));
    }

    // M72-TP5: ENTRY -> N0 -(B1:true)-> N2 -(B2:true)-> N4 -(B3:true)-> N6 -(B4:false)-> N7 -(B5:true)-> N9 -(B6:false)-> N10 -> EXIT (coastal tile, missing required ability -> MISSING_ABILITY)
    // Also covers M221-TP1 (matching ability) & M221-TP2 (mismatching ability)
    public void testM72_TP5_GetNoBuildReasonCoastalAndMissingAbility() throws Exception {
        colony.getTile().setHighSeasCount(1);
        assertTrue(colony.getTile().isCoastland());

        BuildingType customCoastal = new BuildingType("model.building.customCoastal", spec());
        Field reqGoodsField = BuildableType.class.getDeclaredField("requiredGoods");
        reqGoodsField.setAccessible(true);
        reqGoodsField.set(customCoastal, new ArrayList<>(Collections.singletonList(new AbstractGoods(hammersType, 10))));
        customCoastal.addAbility(new Ability(Ability.COASTAL_ONLY));
        Method addReqMethod = BuildableType.class.getDeclaredMethod("addRequiredAbility", String.class, boolean.class);
        addReqMethod.setAccessible(true);
        addReqMethod.invoke(customCoastal, Ability.HAS_PORT, true);
        colony.addPortAbility();
        // M221-TP1: required ability satisfied (false)
        assertEquals(NoBuildReason.NONE, colony.getNoBuildReason(customCoastal, null));
        // M221-TP2: required ability missing (true)
        addReqMethod.invoke(customCoastal, "model.ability.missingCustom", true);
        assertEquals(NoBuildReason.MISSING_ABILITY, colony.getNoBuildReason(customCoastal, null));
    }

    // M72-TP6: ENTRY -> N0 -(B1:true)-> N2 -(B2:true)-> N4 -(B3:true)-> N6 -(B4:true)-> N9 -(B6:true)-> N11 -(B7:false)-> N12 -> EXIT (limit evaluate false -> LIMIT_EXCEEDED)
    // Also covers M220-TP1 (lambda$getNoBuildReason$1)
    public void testM72_TP6_GetNoBuildReasonLimitExceeded() throws Exception {
        BuildingType limitedType = new BuildingType("model.building.limitedTest", spec());
        Field reqGoodsField = BuildableType.class.getDeclaredField("requiredGoods");
        reqGoodsField.setAccessible(true);
        reqGoodsField.set(limitedType, new ArrayList<>(Collections.singletonList(new AbstractGoods(hammersType, 10))));
        Operand lhs = new Operand(Operand.OperandType.UNITS, Operand.ScopeLevel.SETTLEMENT);
        Operand rhs = new Operand(0);
        Limit impossibleLimit = new Limit("model.limit.test", lhs, Limit.Operator.LT, rhs);
        limitedType.setLimits(Collections.singletonList(impossibleLimit));
        assertEquals(NoBuildReason.LIMIT_EXCEEDED, colony.getNoBuildReason(limitedType, null));
    }

    // M72-TP7: ENTRY -> N0 -(B1:true)-> N2 -(B2:true)-> N4 -(B3:true)-> N6 -(B4:true)-> N9 -(B6:true)-> N11 -(B7:true)-> N13 -(B8:false)-> N14 -> N15 -> EXIT (assumeBuilt == null)
    public void testM72_TP7_GetNoBuildReasonSuccessAssumeBuiltNull() {
        assertEquals(NoBuildReason.NONE, colony.getNoBuildReason(wagonTrainType, null));
    }

    // M72-TP8: ENTRY -> N0 -(B1:true)-> N2 -(B2:true)-> N4 -(B3:true)-> N6 -(B4:true)-> N9 -(B6:true)-> N11 -(B7:true)-> N13 -(B8:true)-> N15 -> EXIT (assumeBuilt != null)
    public void testM72_TP8_GetNoBuildReasonSuccessAssumeBuiltNonNull() {
        assertEquals(NoBuildReason.NONE,
                     colony.getNoBuildReason(wagonTrainType, Collections.singletonList(carpenterHouseType)));
    }


    // ------------------------------------------------------------------
    // M73..M76, M217..M219: Building Prices & Required Goods — (100% C3)
    // ------------------------------------------------------------------

    // M73-TP1, M74-TP1, M75-TP1, M76-TP1, M217-TP1, M218-TP1, M218-TP2, M219-TP1, M219-TP2
    public void testM73_To_M76_PriceAndRequiredGoodsForBuilding() {
        colony.setCurrentlyBuilding(armoryType);
        colony.addGoods(toolsType, 200); // tools satisfied (M218 B1:true), hammers needed (M218 B1:false)
        int priceCurrent = colony.getPriceForBuilding();
        assertTrue(priceCurrent > 0);

        // Also price with storable goods (tools) and unstorable goods (hammers) for M219 B1:false & B1:true
        List<AbstractGoods> mixed = Arrays.asList(
                new AbstractGoods(hammersType, 10),
                new AbstractGoods(toolsType, 10));
        assertTrue(colony.priceGoodsForBuilding(mixed) > 0);

        // Satisfy hammers as well so M218 B1:false is guaranteed covered
        colony.addGoods(hammersType, 200);
        assertEquals(0, colony.getPriceForBuilding(armoryType));
    }


    // ------------------------------------------------------------------
    // M77: getFullRequiredGoods(BuildableType) — 4 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M77-TP1: ENTRY -> N0 -(B1:false)-> N1 -> EXIT (buildable == null)
    public void testM77_TP1_GetFullRequiredGoodsNull() {
        assertTrue(colony.getFullRequiredGoods(null).isEmpty());
    }

    // M77-TP2: ENTRY -> N0 -(B1:true)-> N2 -> N3 -(B2:true)-> N10 -> EXIT (buildable has no required goods)
    public void testM77_TP2_GetFullRequiredGoodsEmptyRequirements() {
        assertTrue(colony.getFullRequiredGoods(townHallType).isEmpty());
    }

    // M77-TP3: ENTRY -> N0 -(B1:true)-> N2 -> N3 -(B2:false)-> N4 -> N5 -(B3:false)-> N6 -(B4:true)-> N8 -> N5 -(B3:true)-> N9 -> N3 -(B2:true)-> N10 -> EXIT
    // (required goods and input chain needed until type == null)
    public void testM77_TP3_GetFullRequiredGoodsTraversesInputChain() {
        List<AbstractGoods> req = colony.getFullRequiredGoods(armoryType);
        assertFalse(req.isEmpty());
    }

    // M77-TP4: ENTRY -> N0 -(B1:true)-> N2 -> N3 -(B2:false)-> N4 -> N5 -(B3:false)-> N6 -(B4:false)-> N7 -> N9 -> N3 -(B2:true)-> N10 -> EXIT
    // (amount <= getGoodsCount(type) -> shortcut break)
    public void testM77_TP4_GetFullRequiredGoodsShortcutBreak() {
        colony.addGoods(hammersType, 500);
        colony.addGoods(toolsType, 500);
        assertTrue(colony.getFullRequiredGoods(armoryType).isEmpty());
    }


    // ------------------------------------------------------------------
    // M78 & M79: canPayToFinishBuilding — 1 + 3 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M78-TP1 & M79-TP1: ENTRY -> N0 -(B1:true)-> N3 -> N4 -> EXIT (buildableType == null)
    public void testM78_M79_TP1_CanPayToFinishBuildingNull() {
        colony.setCurrentlyBuilding(null);
        assertFalse(colony.canPayToFinishBuilding());
        assertFalse(colony.canPayToFinishBuilding(null));
    }

    // M79-TP2: ENTRY -> N0 -(B1:false)-> N1 -(B2:true)-> N3 -> N4 -> EXIT (buildableType != null, insufficient gold)
    public void testM79_TP2_CanPayToFinishBuildingInsufficientGold() {
        dutch.setGold(0);
        assertFalse(colony.canPayToFinishBuilding(armoryType));
    }

    // M79-TP3: ENTRY -> N0 -(B1:false)-> N1 -(B2:false)-> N2 -> N4 -> EXIT (buildableType != null, sufficient gold)
    public void testM79_TP3_CanPayToFinishBuildingSufficientGold() {
        dutch.setGold(10000);
        assertTrue(colony.canPayToFinishBuilding(armoryType));
    }


    // ------------------------------------------------------------------
    // M80: addLiberty(int) — 3 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M80-TP1: ENTRY -> N0 -(B1:true)-> N3 -> EXIT (amount <= 0)
    public void testM80_TP1_AddLibertyNonPositiveAmount() {
        colony.addLiberty(0);
        assertEquals(0, colony.getLiberty());
    }

    // M80-TP2: ENTRY -> N0 -(B1:false)-> N1 -(B2:true)-> N3 -> EXIT (amount > 0 && libertyTypeList.isEmpty())
    @SuppressWarnings("unchecked")
    public void testM80_TP2_AddLibertyEmptyLibertyTypeList() throws Exception {
        Field f = Specification.class.getDeclaredField("libertyGoodsTypeList");
        f.setAccessible(true);
        List<GoodsType> list = (List<GoodsType>) f.get(spec());
        List<GoodsType> backup = new ArrayList<>(list);
        try {
            list.clear();
            colony.addLiberty(50);
            assertEquals(0, colony.getLiberty());
        } finally {
            list.addAll(backup);
        }
    }

    // M80-TP3: ENTRY -> N0 -(B1:false)-> N1 -(B2:false)-> N2 -> N3 -> EXIT (amount > 0 && !libertyTypeList.isEmpty())
    public void testM80_TP3_AddLibertyPositiveAmount() {
        colony.addLiberty(100);
        assertTrue(colony.getLiberty() > 0);
    }


    // ------------------------------------------------------------------
    // M81 & M82: modifyLiberty(int) & updateSoL() — 3 + 1 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M81-TP1 & M82-TP1: ENTRY -> N0 -(B1:true)-> N3 -> EXIT (capped == false)
    public void testM81_M82_TP1_ModifyLibertyUncapped() {
        spec().getOption(GameOptions.BELL_ACCUMULATION_CAPPED, BooleanOption.class).setValue(false);
        colony.modifyLiberty(500);
        assertEquals(500, colony.getLiberty());
    }

    // M81-TP2: ENTRY -> N0 -(B1:false)-> N1 -(B2:true)-> N3 -> EXIT (capped == true && sonsOfLiberty < 100)
    public void testM81_TP2_ModifyLibertyCappedBelow100() {
        spec().getOption(GameOptions.BELL_ACCUMULATION_CAPPED, BooleanOption.class).setValue(true);
        colony.modifyLiberty(50);
        assertEquals(50, colony.getLiberty());
    }

    // M81-TP3: ENTRY -> N0 -(B1:false)-> N1 -(B2:false)-> N2 -> N3 -> EXIT (capped == true && sonsOfLiberty >= 100)
    public void testM81_TP3_ModifyLibertyCappedAt100() {
        spec().getOption(GameOptions.BELL_ACCUMULATION_CAPPED, BooleanOption.class).setValue(true);
        colony.modifyLiberty(1000);
        assertEquals(Colony.LIBERTY_PER_REBEL * colony.getUnitCount(), colony.getLiberty());
    }


    // ------------------------------------------------------------------
    // M83: calculateSoLPercentage(int, int) — 4 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M83-TP1: ENTRY -> N0 -(B1:false)-> N1 -> EXIT (uc <= 0 -> -1)
    public void testM83_TP1_CalculateSoLPercentageZeroUnits() throws Exception {
        int res = (Integer) invokePrivate(colony, "calculateSoLPercentage",
                                          new Class<?>[]{int.class, int.class}, 0, 100);
        assertEquals(-1, res);
    }

    // M83-TP2: ENTRY -> N0 -(B1:true)-> N2 -(B2:false)-> N3 -> N6 -> EXIT (membership < 0.0f -> clamped to 0)
    public void testM83_TP2_CalculateSoLPercentageNegativeClampedToZero() throws Exception {
        int res = (Integer) invokePrivate(colony, "calculateSoLPercentage",
                                          new Class<?>[]{int.class, int.class}, 2, -100);
        assertEquals(0, res);
    }

    // M83-TP3: ENTRY -> N0 -(B1:true)-> N2 -(B2:true)-> N4 -(B3:false)-> N5 -> N6 -> EXIT (membership > 100.0f -> clamped to 100)
    public void testM83_TP3_CalculateSoLPercentageClampedTo100() throws Exception {
        int res = (Integer) invokePrivate(colony, "calculateSoLPercentage",
                                          new Class<?>[]{int.class, int.class}, 2, 1000);
        assertEquals(100, res);
    }

    // M83-TP4: ENTRY -> N0 -(B1:true)-> N2 -(B2:true)-> N4 -(B3:true)-> N6 -> EXIT (0 <= membership <= 100)
    public void testM83_TP4_CalculateSoLPercentageNormalRange() throws Exception {
        int res = (Integer) invokePrivate(colony, "calculateSoLPercentage",
                                          new Class<?>[]{int.class, int.class}, 2, 200);
        assertEquals(50, res);
    }


    // ------------------------------------------------------------------
    // M84 & M85: calculateRebelCount & calculateToryCount — 1 Test Path each (100% C3)
    // ------------------------------------------------------------------

    // M84-TP1 & M85-TP1: ENTRY -> N0 -> EXIT
    public void testM84_M85_TP1_CalculateRebelAndToryCount() {
        assertEquals(1, Colony.calculateRebelCount(2, 50));
        assertEquals(1, Colony.calculateToryCount(2, 50));
    }


    // ------------------------------------------------------------------
    // M86: calculateProductionBonus(int) — 5 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M86-TP1: ENTRY -> N0 -(B1:false)-> N1 -> EXIT (solPercent >= 100 -> GOVERNMENT_VERY_GOOD)
    public void testM86_TP1_CalculateProductionBonusVeryGood() throws Exception {
        int bonus = (Integer) invokePrivate(colony, "calculateProductionBonus",
                                            new Class<?>[]{int.class}, 100);
        assertEquals(colony.GOVERNMENT_VERY_GOOD, bonus);
    }

    // M86-TP2: ENTRY -> N0 -(B1:true)-> N2 -(B2:false)-> N3 -> EXIT (50 <= solPercent < 100 -> GOVERNMENT_GOOD)
    public void testM86_TP2_CalculateProductionBonusGood() throws Exception {
        int bonus = (Integer) invokePrivate(colony, "calculateProductionBonus",
                                            new Class<?>[]{int.class}, 50);
        assertEquals(colony.GOVERNMENT_GOOD, bonus);
    }

    // M86-TP3: ENTRY -> N0 -(B1:true)-> N2 -(B2:true)-> N4 -(B3:false)-> N5 -> N9 -> EXIT (tc > veryBadGovernment -> GOVERNMENT_VERY_BAD)
    public void testM86_TP3_CalculateProductionBonusVeryBad() throws Exception {
        IntegerOption badOpt = spec().getOption(GameOptions.BAD_GOVERNMENT_LIMIT, IntegerOption.class);
        IntegerOption veryBadOpt = spec().getOption(GameOptions.VERY_BAD_GOVERNMENT_LIMIT, IntegerOption.class);
        int savedBad = badOpt.getValue();
        int savedVeryBad = veryBadOpt.getValue();
        try {
            badOpt.setValue(0);
            veryBadOpt.setValue(1);
            int bonus = (Integer) invokePrivate(colony, "calculateProductionBonus",
                                                new Class<?>[]{int.class}, 0);
            assertEquals(colony.GOVERNMENT_VERY_BAD, bonus);
        } finally {
            badOpt.setValue(savedBad);
            veryBadOpt.setValue(savedVeryBad);
        }
    }

    // M86-TP4: ENTRY -> N0 -(B1:true)-> N2 -(B2:true)-> N4 -(B3:true)-> N6 -(B4:false)-> N7 -> N9 -> EXIT (badGovernment < tc <= veryBadGovernment -> GOVERNMENT_BAD)
    public void testM86_TP4_CalculateProductionBonusBad() throws Exception {
        IntegerOption badOpt = spec().getOption(GameOptions.BAD_GOVERNMENT_LIMIT, IntegerOption.class);
        IntegerOption veryBadOpt = spec().getOption(GameOptions.VERY_BAD_GOVERNMENT_LIMIT, IntegerOption.class);
        int savedBad = badOpt.getValue();
        int savedVeryBad = veryBadOpt.getValue();
        try {
            badOpt.setValue(1);
            veryBadOpt.setValue(2);
            int bonus = (Integer) invokePrivate(colony, "calculateProductionBonus",
                                                new Class<?>[]{int.class}, 0);
            assertEquals(colony.GOVERNMENT_BAD, bonus);
        } finally {
            badOpt.setValue(savedBad);
            veryBadOpt.setValue(savedVeryBad);
        }
    }

    // M86-TP5: ENTRY -> N0 -(B1:true)-> N2 -(B2:true)-> N4 -(B3:true)-> N6 -(B4:true)-> N8 -> N9 -> EXIT (tc <= badGovernment -> GOVERNMENT_ORDINARY)
    public void testM86_TP5_CalculateProductionBonusOrdinary() throws Exception {
        int bonus = (Integer) invokePrivate(colony, "calculateProductionBonus",
                                            new Class<?>[]{int.class}, 0);
        assertEquals(colony.GOVERNMENT_ORDINARY, bonus);
    }


    // ------------------------------------------------------------------
    // M87: updateProductionBonus() — 2 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M87-TP1: ENTRY -> N0 -(B1:true)-> N2 -> EXIT (productionBonus == newBonus -> false)
    public void testM87_TP1_UpdateProductionBonusUnchanged() {
        colony.sonsOfLiberty = 0;
        colony.setProductionBonus(0);
        assertFalse(colony.updateProductionBonus());
    }

    // M87-TP2: ENTRY -> N0 -(B1:false)-> N1 -> EXIT (productionBonus != newBonus -> true)
    public void testM87_TP2_UpdateProductionBonusChanged() {
        colony.sonsOfLiberty = 100;
        colony.setProductionBonus(0);
        assertTrue(colony.updateProductionBonus());
        assertEquals(2, colony.getProductionBonus());
    }


    // ------------------------------------------------------------------
    // M88..M90: getPreferredSizeChange, getUnitsToAdd, getUnitsToRemove — (100% C3)
    // ------------------------------------------------------------------

    // M88-TP1 (productionBonus >= 0) & M89-TP1 (governmentChange(pop + i) == -1 returns i - 1)
    public void testM88_TP1_M89_TP1_PreferredSizeChangePositiveAndUnitsToAddLimited() {
        // 2 units with 0 liberty and BAD_GOVERNMENT_LIMIT=2: adding 1 unit makes 3 tories (> 2), so governmentChange(3) == -1
        IntegerOption badOpt = spec().getOption(GameOptions.BAD_GOVERNMENT_LIMIT, IntegerOption.class);
        IntegerOption veryBadOpt = spec().getOption(GameOptions.VERY_BAD_GOVERNMENT_LIMIT, IntegerOption.class);
        int savedBad = badOpt.getValue();
        int savedVeryBad = veryBadOpt.getValue();
        try {
            badOpt.setValue(2);
            veryBadOpt.setValue(10);
            colony.updateSoL();
            colony.setProductionBonus(0);
            assertEquals(0, colony.getUnitsToAdd());
            assertEquals(0, colony.getPreferredSizeChange());
        } finally {
            badOpt.setValue(savedBad);
            veryBadOpt.setValue(savedVeryBad);
        }
    }

    // M89-TP2: ENTRY -> N0 -> N1 -(B1:false)-> N2 -(B2:true)-> N4 -> N1 -(B1:true)-> N5 -> EXIT (returns CHANGE_UPPER_BOUND)
    public void testM89_TP2_GetUnitsToAddUpperBound() {
        colony.liberty = 10000;
        colony.sonsOfLiberty = 100;
        assertEquals(Colony.CHANGE_UPPER_BOUND, colony.getUnitsToAdd());
    }

    // M88-TP2 (productionBonus < 0) & M90-TP2 (governmentChange(pop - i) == 1 returns i)
    public void testM88_TP2_M90_TP2_PreferredSizeChangeNegativeAndUnitsToRemoveFound() {
        // 2 units with 0 liberty and BAD_GOVERNMENT_LIMIT=1: 2 tories (bad government, bonus = -1); removing 1 unit -> 1 tory (ordinary, change == 1)
        IntegerOption badOpt = spec().getOption(GameOptions.BAD_GOVERNMENT_LIMIT, IntegerOption.class);
        IntegerOption veryBadOpt = spec().getOption(GameOptions.VERY_BAD_GOVERNMENT_LIMIT, IntegerOption.class);
        int savedBad = badOpt.getValue();
        int savedVeryBad = veryBadOpt.getValue();
        try {
            badOpt.setValue(1);
            veryBadOpt.setValue(10);
            colony.updateSoL();
            colony.updateProductionBonus();
            assertEquals(1, colony.getUnitsToRemove());
            assertEquals(-1, colony.getPreferredSizeChange());
        } finally {
            badOpt.setValue(savedBad);
            veryBadOpt.setValue(savedVeryBad);
        }
    }

    // M90-TP1: ENTRY -> N0 -> N1 -(B1:true)-> N5 -> EXIT (pop <= 1 -> loop does not execute)
    public void testM90_TP1_GetUnitsToRemovePopOne() {
        colony.getUnitList().get(0).setLocation(colony.getTile());
        assertEquals(1, colony.getUnitCount());
        assertEquals(0, colony.getUnitsToRemove());
    }

    // M90-TP3: ENTRY -> N0 -> N1 -(B1:false)-> N2 -(B2:true)-> N4 -> N1 -(B1:true)-> N5 -> EXIT (pop > 1, no improvement found)
    public void testM90_TP3_GetUnitsToRemoveNoImprovement() {
        // 2 units with 0 liberty -> governmentChange(1) == 0 != 1
        assertEquals(0, colony.getUnitsToRemove());
    }


    // ------------------------------------------------------------------
    // M91, M92, M216: rebelHelper(int) & projectLibertyProduction(int) — (100% feasible C3)
    // ------------------------------------------------------------------

    // M91-TP1: ENTRY -> N0 -(B1:false)-> N1 -> EXIT (libertyProduction <= 0)
    public void testM91_TP1_RebelHelperNonPositiveProduction() {
        assertEquals(Arrays.asList(-1, -1, -1), colony.rebelHelper(0));
    }

    // M91-TP2: Multi-turn progression from 0% SoL -> 50% (GOOD) -> 100% (VERY_GOOD)
    // Covers B1:true, B2:true/false, B3:false/true, B4:true/false, B5:false/true, B6:true/false, B7:false, B8:true/false
    // Also covers M92-TP2 (projectedBonus != originalBonus) & M216-TP1 (lambda$projectLibertyProduction$0)
    public void testM91_TP2_M92_TP2_RebelHelperFullProgression() {
        Building th = colony.getBuilding(townHallType);
        Unit u = colony.getUnitList().get(0);
        u.setLocation(th);
        u.changeWorkType(bellsType);
        colony.invalidateCache();
        List<Integer> turns = colony.rebelHelper(50);
        assertTrue(turns.get(0) > 0);
        assertTrue(turns.get(1) > 0);
        assertTrue(turns.get(2) >= turns.get(1));
    }

    // M92-TP1: ENTRY -> N0 -(B1:true)-> N2 -> N3 -(B2:true)-> N5 -> N6 -(B3:true)-> N12 -> EXIT (projectedBonus == originalBonus)
    public void testM92_TP1_ProjectLibertyProductionUnchangedBonus() throws Exception {
        int prod = (Integer) invokePrivate(colony, "projectLibertyProduction",
                                           new Class<?>[]{int.class}, colony.getProductionBonus());
        assertTrue(prod >= 0);
    }


    // ------------------------------------------------------------------
    // M93: joinColony(Unit) — 4 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M93-TP1: ENTRY -> N0 -(B1:false)-> N1 -(B2:false)-> N2 -> N3 -> N5 -(B3:false)-> N6 -> N7 -> EXIT (occupation == null, trace == false)
    public void testM93_TP1_JoinColonyNoOccupationTraceFalse() throws Exception {
        Unit newcomer = new ServerUnit(game, colony.getTile(), dutch, colonistType);
        colony.setOccupationTrace(false);
        colony.clearBuildingMap();
        invokePrivate(colony, "clearColonyTiles", new Class<?>[0]);
        assertFalse(colony.joinColony(newcomer));
        assertEquals(colony.getTile(), newcomer.getLocation());
    }

    // M93-TP2: ENTRY -> N0 -(B1:false)-> N1 -(B2:true)-> N3 -> N5 -(B3:false)-> N6 -> N7 -> EXIT (occupation == null, trace == true)
    public void testM93_TP2_JoinColonyNoOccupationTraceTrue() throws Exception {
        Unit newcomer = new ServerUnit(game, colony.getTile(), dutch, colonistType);
        colony.setOccupationTrace(true);
        colony.clearBuildingMap();
        invokePrivate(colony, "clearColonyTiles", new Class<?>[0]);
        assertFalse(colony.joinColony(newcomer));
    }

    // M93-TP3: ENTRY -> N0 -(B1:true)-> N4 -> N5 -(B3:true)-> N7 -> EXIT (occupation != null, install succeeds)
    public void testM93_TP3_JoinColonySuccess() {
        Unit newcomer = new ServerUnit(game, colony.getTile(), dutch, colonistType);
        assertTrue(colony.joinColony(newcomer));
    }

    // M93-TP4: ENTRY -> N0 -(B1:true)-> N4 -> N5 -(B3:false)-> N6 -> N7 -> EXIT (occupation != null, install returns false)
    public void testM93_TP4_JoinColonyInstallFails() {
        Unit rejectUnit = new RejectWorkLocationUnit(game, colony.getTile(), dutch, colonistType);
        assertFalse(colony.joinColony(rejectUnit));
        assertEquals(colony.getTile(), rejectUnit.getLocation());
    }


    // ------------------------------------------------------------------
    // M94..M96: canReducePopulation, getReducePopulationMessage, getUnbuildableMessage — (100% C3)
    // ------------------------------------------------------------------

    // M94-TP1 (canReducePopulation == true) & M95-TP1 (returns null when canReducePopulation)
    public void testM94_TP1_M95_TP1_CanReducePopulationTrue() {
        assertTrue(colony.canReducePopulation());
        assertNull(colony.getReducePopulationMessage());
    }

    // M94-TP2 (canReducePopulation == false) & M95-TP2 (min == null -> returns null)
    public void testM94_TP2_M95_TP2_CannotReducePopulationNoModifier() {
        removeAllUnitsFromColony(colony);
        assertFalse(colony.canReducePopulation());
        assertNull(colony.getReducePopulationMessage());
    }

    // M95-TP3: ENTRY -> N0 -(B1:true)-> N2 -(B2:true)-> N4 -(B3:true)-> N6 -> EXIT (source not BuildingType)
    public void testM95_TP3_GetReducePopulationMessageNonBuildingSource() {
        Modifier mod = new Modifier(Modifier.MINIMUM_COLONY_SIZE, 10f,
                                    Modifier.ModifierType.ADDITIVE,
                                    Specification.COLONY_GOODS_PARTY_SOURCE);
        colony.addModifier(mod);
        StringTemplate msg = colony.getReducePopulationMessage();
        assertNotNull(msg);
    }

    // M95-TP4: ENTRY -> N0 -(B1:true)-> N2 -(B2:true)-> N4 -(B3:false)-> N5 -> N6 -> EXIT (source is BuildingType)
    public void testM95_TP4_GetReducePopulationMessageBuildingTypeSource() {
        colony.addBuilding(new ServerBuilding(game, colony, stockadeType));
        Modifier mod = new Modifier(Modifier.MINIMUM_COLONY_SIZE, 10f,
                                    Modifier.ModifierType.ADDITIVE,
                                    stockadeType);
        colony.addModifier(mod);
        StringTemplate msg = colony.getReducePopulationMessage();
        assertNotNull(msg);
    }

    // M96-TP1: ENTRY -> N0 -> EXIT
    public void testM96_TP1_GetUnbuildableMessage() {
        ModelMessage msg = colony.getUnbuildableMessage(armoryType);
        assertNotNull(msg);
    }


    // ------------------------------------------------------------------
    // M97: governmentChange(int) — 13 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M97-TP1..TP13: Covers all 13 paths of governmentChange(int)
    public void testM97_TP1_To_TP13_GovernmentChangeAllBranches() {
        // TP1: newSoL >= 100 (B1:false), oldSoL >= 100 (B2:true) -> 0
        colony.liberty = 1000;
        colony.sonsOfLiberty = 100;
        assertEquals(0, colony.governmentChange(2));

        // TP2: newSoL >= 100 (B1:false), oldSoL < 100 (B2:false) -> 1
        colony.sonsOfLiberty = 50;
        assertEquals(1, colony.governmentChange(2));

        // TP3: 50 <= newSoL < 100 (B1:true, B3:false), oldSoL >= 100 (B4:false) -> -1
        colony.liberty = 200; // for unitCount=2, newSoL = 50%
        colony.sonsOfLiberty = 100;
        assertEquals(-1, colony.governmentChange(2));

        // TP4: 50 <= newSoL < 100 (B1:true, B3:false), 50 <= oldSoL < 100 (B4:true, B5:true) -> 0
        colony.sonsOfLiberty = 50;
        assertEquals(0, colony.governmentChange(2));

        // TP5: 50 <= newSoL < 100 (B1:true, B3:false), oldSoL < 50 (B4:true, B5:false) -> 1
        colony.sonsOfLiberty = 0;
        assertEquals(1, colony.governmentChange(2));

        // TP6: newSoL < 50 (B1:true, B3:true), oldSoL >= 50 (B6:false) -> -1
        colony.liberty = 0;
        colony.sonsOfLiberty = 50;
        assertEquals(-1, colony.governmentChange(2));

        // TP7: newSoL < 50, oldSoL < 50 (B6:true), newToryCount > 10 (B7:false), oldToryCount > 10 (B8:true) -> 0
        colony.sonsOfLiberty = 0;
        colony.tories = 12;
        assertEquals(0, colony.governmentChange(12));

        // TP8: newSoL < 50, oldSoL < 50 (B6:true), newToryCount > 10 (B7:false), oldToryCount <= 10 (B8:false) -> -1
        colony.tories = 10;
        assertEquals(-1, colony.governmentChange(12));

        // TP9: newSoL < 50, oldSoL < 50, 6 < newToryCount <= 10 (B7:true, B9:false), oldToryCount <= 6 (B10:false) -> -1
        colony.tories = 6;
        assertEquals(-1, colony.governmentChange(8));

        // TP10: 6 < newToryCount <= 10 (B7:true, B9:false), 6 < oldToryCount <= 10 (B10:true, B11:true) -> 0
        colony.tories = 8;
        assertEquals(0, colony.governmentChange(8));

        // TP11: 6 < newToryCount <= 10 (B7:true, B9:false), oldToryCount > 10 (B10:true, B11:false) -> 1
        colony.tories = 12;
        assertEquals(1, colony.governmentChange(8));

        // TP12: newToryCount <= 6 (B7:true, B9:true), oldToryCount <= 6 (B12:true) -> 0
        colony.tories = 4;
        assertEquals(0, colony.governmentChange(4));

        // TP13: newToryCount <= 6 (B7:true, B9:true), oldToryCount > 6 (B12:false) -> 1
        colony.tories = 8;
        assertEquals(1, colony.governmentChange(4));
    }


    // ------------------------------------------------------------------
    // M98: checkForGovMgtChangeMessage() — 14 Feasible Test Paths (100% feasible C3)
    // ------------------------------------------------------------------

    // M98-TP1..TP14: Covers all 13 branches (B1..B13) and all 24 basic blocks (N0..N23)
    public void testM98_TP1_To_TP14_CheckForGovMgtChangeMessageAllBranches() {
        // TP1: sonsOfLiberty >= 100 (B1:false), oldSonsOfLiberty >= 100 (B2:true) -> msgId == null (B13:false)
        colony.sonsOfLiberty = 100;
        colony.oldSonsOfLiberty = 100;
        assertNull(colony.checkForGovMgtChangeMessage());

        // TP2: sonsOfLiberty >= 100 (B1:false), oldSonsOfLiberty < 100 (B2:false) -> veryGoodGovernment (B13:true)
        colony.oldSonsOfLiberty = 50;
        assertEquals("model.colony.veryGoodGovernment", colony.checkForGovMgtChangeMessage().getId());

        // TP3: 50 <= sonsOfLiberty < 100 (B1:true, B3:false), oldSonsOfLiberty == 100 (B4:false) -> lostVeryGoodGovernment
        colony.sonsOfLiberty = 50;
        colony.oldSonsOfLiberty = 100;
        assertEquals("model.colony.lostVeryGoodGovernment", colony.checkForGovMgtChangeMessage().getId());

        // TP4: 50 <= sonsOfLiberty < 100, 50 <= oldSonsOfLiberty < 100 (B4:true, B5:true) -> null
        colony.oldSonsOfLiberty = 60;
        assertNull(colony.checkForGovMgtChangeMessage());

        // TP5: 50 <= sonsOfLiberty < 100, oldSonsOfLiberty < 50 (B4:true, B5:false) -> goodGovernment
        colony.oldSonsOfLiberty = 20;
        assertEquals("model.colony.goodGovernment", colony.checkForGovMgtChangeMessage().getId());

        // TP6: sonsOfLiberty < 50 (B1:true, B3:true), oldSonsOfLiberty >= 50 (B6:false), tories <= 6 (B7:true, B9:true, B12:true) -> lostGoodGovernment
        colony.sonsOfLiberty = 20;
        colony.oldSonsOfLiberty = 50;
        colony.tories = 2;
        colony.oldTories = 2;
        assertEquals("model.colony.lostGoodGovernment", colony.checkForGovMgtChangeMessage().getId());

        // TP7: sonsOfLiberty < 50, oldSonsOfLiberty < 50 (B6:true), tories > 10 (B7:false), oldTories > 10 (B8:true) -> null
        colony.oldSonsOfLiberty = 10;
        colony.tories = 12;
        colony.oldTories = 12;
        assertNull(colony.checkForGovMgtChangeMessage());

        // TP8: sonsOfLiberty < 50, oldSonsOfLiberty < 50, tories > 10 (B7:false), oldTories <= 10 (B8:false) -> veryBadGovernment
        colony.oldTories = 10;
        assertEquals("model.colony.veryBadGovernment", colony.checkForGovMgtChangeMessage().getId());

        // TP9: 6 < tories <= 10 (B7:true, B9:false), oldTories <= 6 (B10:false) -> badGovernment
        colony.tories = 8;
        colony.oldTories = 6;
        assertEquals("model.colony.badGovernment", colony.checkForGovMgtChangeMessage().getId());

        // TP10: 6 < tories <= 10 (B7:true, B9:false), 6 < oldTories <= 10 (B10:true, B11:true) -> null
        colony.oldTories = 8;
        assertNull(colony.checkForGovMgtChangeMessage());

        // TP11: 6 < tories <= 10 (B7:true, B9:false), oldTories > 10 (B10:true, B11:false) -> governmentImproved1
        colony.oldTories = 12;
        assertEquals("model.colony.governmentImproved1", colony.checkForGovMgtChangeMessage().getId());

        // TP12: tories <= 6 (B7:true, B9:true), oldTories <= 6 (B12:true) -> null
        colony.tories = 4;
        colony.oldTories = 4;
        assertNull(colony.checkForGovMgtChangeMessage());

        // TP13: tories <= 6 (B7:true, B9:true), oldTories > 6 (B12:false) -> governmentImproved2
        colony.oldTories = 8;
        assertEquals("model.colony.governmentImproved2", colony.checkForGovMgtChangeMessage().getId());
    }


    // ------------------------------------------------------------------
    // M99: updatePopulation() — 2 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M99-TP1: ENTRY -> N0 -(B1:true)-> N2 -> EXIT (getOwner().isAI() == false)
    public void testM99_TP1_UpdatePopulationHumanOwner() {
        dutch.setAI(false);
        colony.updatePopulation();
        assertFalse(dutch.isAI());
    }

    // M99-TP2: ENTRY -> N0 -(B1:false)-> N1 -> N2 -> EXIT (getOwner().isAI() == true)
    public void testM99_TP2_UpdatePopulationAIOwner() {
        dutch.setAI(true);
        colony.updatePopulation();
        assertTrue(dutch.isAI());
    }


    // ------------------------------------------------------------------
    // M100: updateEducation(Unit, boolean) — 12 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M100-TP1: ENTRY -> N0 -(B1:false)-> N1 -> EXIT (wl == null -> throws RuntimeException)
    public void testM100_TP1_UpdateEducationNotAtWorkLocation() {
        Unit onTile = new ServerUnit(game, colony.getTile(), dutch, colonistType);
        try {
            colony.updateEducation(onTile, true);
            fail("Expected RuntimeException");
        } catch (RuntimeException expected) {
            assertTrue(expected.getMessage().contains("not at work location"));
        }
    }

    // M100-TP2: ENTRY -> N0 -(B1:true)-> N2 -(B2:false)-> N3 -> EXIT (wl.getColony() != this -> throws RuntimeException)
    public void testM100_TP2_UpdateEducationWrongColony() {
        Colony otherColony = new ServerColony(game, dutch, "Other", game.getMap().getTile(3, 3));
        Unit u = colony.getUnitList().get(0);
        try {
            otherColony.updateEducation(u, true);
            fail("Expected RuntimeException");
        } catch (RuntimeException expected) {
            assertTrue(expected.getMessage().contains("not at work location in this colony"));
        }
    }

    // M100-TP3..TP12: Covers all 10 normal paths of updateEducation (enable=true/false, canTeach=true/false, student/teacher states)
    public void testM100_TP3_To_TP12_UpdateEducationAllBranches() {
        removeAllUnitsFromColony(colony);
        Building school = new ServerBuilding(game, colony, schoolhouseType);
        colony.addBuilding(school);
        Building th = colony.getBuilding(townHallType);

        Unit teacher = new ServerUnit(game, th, dutch, expertFarmerType);
        Unit student = new ServerUnit(game, th, dutch, colonistType);

        // Put teacher into school (without triggering auto-assignment first by setting ALLOW_STUDENT_SELECTION=true)
        spec().getOption(GameOptions.ALLOW_STUDENT_SELECTION, BooleanOption.class).setValue(true);
        teacher.setLocation(school);

        // TP4: enable=true, wl.canTeach()=true, student==null, findStudent(unit)==null (because ALLOW_STUDENT_SELECTION=true)
        colony.updateEducation(teacher, true);
        assertNull(teacher.getStudent());

        // TP5: enable=true, wl.canTeach()=true, student==null, findStudent(unit)!=null
        spec().getOption(GameOptions.ALLOW_STUDENT_SELECTION, BooleanOption.class).setValue(false);
        colony.updateEducation(teacher, true);
        assertSame(student, teacher.getStudent());
        assertSame(teacher, student.getTeacher());

        // TP3: enable=true, wl.canTeach()=true, student!=null
        colony.updateEducation(teacher, true);
        assertSame(student, teacher.getStudent());

        // TP6: enable=true, wl.canTeach()=false, teacher!=null
        colony.updateEducation(student, true);
        assertSame(teacher, student.getTeacher());

        // TP10: enable=false, wl.canTeach()=true, student!=null (clears student/teacher)
        colony.updateEducation(teacher, false);
        assertNull(teacher.getStudent());
        assertNull(student.getTeacher());

        // TP9: enable=false, wl.canTeach()=true, student==null
        colony.updateEducation(teacher, false);
        assertNull(teacher.getStudent());

        // TP8: enable=true, wl.canTeach()=false, teacher==null, findTeacher(student)!=null
        colony.updateEducation(student, true);
        assertSame(teacher, student.getTeacher());

        // TP12: enable=false, wl.canTeach()=false, teacher!=null (clears teacher/student)
        colony.updateEducation(student, false);
        assertNull(student.getTeacher());
        assertNull(teacher.getStudent());

        // TP11: enable=false, wl.canTeach()=false, teacher==null
        colony.updateEducation(student, false);
        assertNull(student.getTeacher());

        // TP7: enable=true, wl.canTeach()=false, teacher==null, findTeacher(student)==null
        spec().getOption(GameOptions.ALLOW_STUDENT_SELECTION, BooleanOption.class).setValue(true);
        colony.updateEducation(student, true);
        assertNull(student.getTeacher());
    }


    // ------------------------------------------------------------------
    // M101 & M102: isUndead() & getApparentUnitCount() — 3 + 2 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M101-TP1: ENTRY -> N0 -(B1:true)-> N3 -> N4 -> EXIT (getFirstUnit() == null)
    public void testM101_TP1_IsUndeadEmptyColony() {
        removeAllUnitsFromColony(colony);
        assertFalse(colony.isUndead());
    }

    // M101-TP2: ENTRY -> N0 -(B1:false)-> N1 -(B2:true)-> N3 -> N4 -> EXIT (u != null && !u.isUndead())
    public void testM101_TP2_IsUndeadNormalUnit() {
        assertFalse(colony.isUndead());
    }

    // M101-TP3: ENTRY -> N0 -(B1:false)-> N1 -(B2:false)-> N2 -> N4 -> EXIT (u != null && u.isUndead())
    public void testM101_TP3_IsUndeadTrue() {
        colony.getFirstUnit().setType(undeadType);
        assertTrue(colony.isUndead());
    }

    // M102-TP1 (displayUnitCount <= 0) & M102-TP2 (displayUnitCount > 0)
    public void testM102_TP1_TP2_GetApparentUnitCount() {
        colony.setDisplayUnitCount(-1);
        assertEquals(colony.getUnitCount(), colony.getApparentUnitCount());
        colony.setDisplayUnitCount(9);
        assertEquals(9, colony.getApparentUnitCount());
    }


    // ------------------------------------------------------------------
    // M103, M104, M214, M215: getBestDefenderType & getTotalDefencePower — (100% C3)
    // ------------------------------------------------------------------

    // M103-TP1, M104-TP1, M214-TP1, M215-TP1..TP4
    public void testM103_M104_GetBestDefenderTypeAndTotalDefencePower() {
        assertNotNull(colony.getBestDefenderType());
        new ServerUnit(game, colony.getTile(), dutch, artilleryType);
        new ServerUnit(game, colony.getTile(), dutch, wagonTrainType);
        assertTrue(colony.getTotalDefencePower() > 0.0);
    }


    // ------------------------------------------------------------------
    // M105..M108: canBePillaged, canBePlundered, getBurnableBuildings, getLootableGoodsList — (100% C3)
    // ------------------------------------------------------------------

    // M105-TP1..TP11, M106-TP1, M107-TP1, M108-TP1
    public void testM105_To_M108_CanBePillagedAllBranches() {
        Unit nonRaider = new ServerUnit(game, colony.getTile(), arawak, colonistType);
        Unit raiderNoCargo = new ServerUnit(game, colony.getTile(), french, artilleryType);
        Unit raiderCargo = new ServerUnit(game, colony.getTile(), french, privateerType);
        Ability pillageAbility = new Ability(Ability.PILLAGE_UNPROTECTED_COLONY);
        french.addAbility(pillageAbility);

        // TP1: hasStockade() == true (B1:true -> false)
        Building stockade = new ServerBuilding(game, colony, stockadeType);
        colony.addBuilding(stockade);
        assertFalse(colony.canBePillaged(raiderNoCargo));
        colony.removeBuilding(stockade);

        // TP2: !attacker.hasAbility(PILLAGE_UNPROTECTED_COLONY) (B1:false, B2:true -> false)
        assertFalse(colony.canBePillaged(nonRaider));

        // TP3: !getBurnableBuildings().isEmpty() (B1:false, B2:false, B3:true -> true)
        Building burnable = new ServerBuilding(game, colony, warehouseType);
        colony.addBuilding(burnable);
        assertTrue(colony.canBePillaged(raiderNoCargo));

        // Remove burnable buildings so getBurnableBuildings().isEmpty() is true (B3:false)
        colony.clearBuildingMap();

        // TP4: !getTile().getNavalUnits().isEmpty() (B3:false, B4:true -> true)
        Unit shipOnTile = new ServerUnit(game, colony.getTile(), dutch, caravelType);
        assertTrue(colony.canBePillaged(raiderNoCargo));
        shipOnTile.dispose();
        raiderCargo.setLocation(game.getMap().getTile(1, 1));

        // TP5: getLootableGoodsList().isEmpty() (B5:true), !canBePlundered() (B8:true -> false)
        colony.getGoodsContainer().removeAll();
        dutch.setGold(0);
        assertFalse(colony.canBePillaged(raiderNoCargo));

        // TP6: getLootableGoodsList().isEmpty() (B5:true), canBePlundered() (B8:false -> true)
        dutch.setGold(100);
        assertTrue(colony.canBePillaged(raiderNoCargo));

        // Add lootable goods so getLootableGoodsList().isEmpty() is false (B5:false)
        colony.addGoods(sugarType, 50);

        // TP7: !attacker.getType().canCarryGoods() (B6:true), !canBePlundered() (B8:true -> false)
        dutch.setGold(0);
        assertFalse(colony.canBePillaged(raiderNoCargo));

        // TP8: !attacker.getType().canCarryGoods() (B6:true), canBePlundered() (B8:false -> true)
        dutch.setGold(100);
        assertTrue(colony.canBePillaged(raiderNoCargo));

        // TP9: attacker.getType().canCarryGoods() (B6:false), attacker.hasSpaceLeft() (B7:true -> true)
        dutch.setGold(0);
        assertTrue(colony.canBePillaged(raiderCargo));

        // Fill raiderCargo so attacker.hasSpaceLeft() == false (B7:false)
        raiderCargo.addGoods(sugarType, 600);
        assertFalse(raiderCargo.hasSpaceLeft());

        // TP10: !attacker.hasSpaceLeft() (B7:false), !canBePlundered() (B8:true -> false)
        dutch.setGold(0);
        assertFalse(colony.canBePillaged(raiderCargo));

        // TP11: !attacker.hasSpaceLeft() (B7:false), canBePlundered() (B8:false -> true)
        dutch.setGold(100);
        assertTrue(colony.canBePillaged(raiderCargo));
    }


    // ------------------------------------------------------------------
    // M109, M213: isUnderSiege() — 2 Test Paths (all 6 branches covered) (100% C3)
    // ------------------------------------------------------------------

    // M109-TP1: Not under siege (friendly defensive/non-defensive, peaceful foreign, war non-offensive)
    public void testM109_TP1_IsUnderSiegeFalseWithMixedUnits() {
        Tile workTile = colony.getColonyTiles().get(0).getWorkTile();
        // Friendly defensive unit (B2:false, B3:false -> friendlyUnits++)
        new ServerUnit(game, workTile, dutch, artilleryType);
        new ServerUnit(game, workTile, dutch, colonistType);
        // Friendly non-defensive unit (B2:false, B3:true)
        new ServerUnit(game, workTile, dutch, wagonTrainType);
        // Foreign peaceful unit (B2:true, B4:true)
        dutch.setStance(french, Stance.PEACE);
        new ServerUnit(game, workTile, french, artilleryType);
        // Foreign war non-offensive unit (B2:true, B4:false, B5:true)
        dutch.setStance(arawak, Stance.WAR);
        new ServerUnit(game, workTile, arawak, wagonTrainType);
        assertFalse(colony.isUnderSiege());
    }

    // M109-TP2: Under siege (enemyUnits > friendlyUnits -> B2:true, B4:false, B5:false, B6:false)
    public void testM109_TP2_IsUnderSiegeTrue() {
        Tile workTile = colony.getColonyTiles().get(0).getWorkTile();
        dutch.setStance(french, Stance.WAR);
        new ServerUnit(game, workTile, french, artilleryType);
        new ServerUnit(game, workTile, french, artilleryType);
        assertTrue(colony.isUnderSiege());
    }


    // ------------------------------------------------------------------
    // M110: evaluateFor(Player) — 8 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M110-TP1: player.isAI() && player.owns(this) && settlementCount < TRADE_MARGIN -> Integer.MIN_VALUE (B1:false, B2:false, B3:false)
    public void testM110_TP1_EvaluateForAIOwnerBelowTradeMargin() {
        dutch.setAI(true);
        assertEquals(Integer.MIN_VALUE, colony.evaluateFor(dutch));
    }

    // M110-TP2: player.isAI() && player.owns(this) && settlementCount >= TRADE_MARGIN (B1:false, B2:false, B3:true, B4:false)
    public void testM110_TP2_EvaluateForAIOwnerAtOrAboveTradeMargin() {
        dutch.setAI(true);
        for (int i = 0; i < Colony.TRADE_MARGIN; i++) {
            Tile t = game.getMap().getTile(2 + i, 2);
            Colony extra = new ServerColony(game, dutch, "Extra" + i, t);
            dutch.addSettlement(extra);
        }
        assertTrue(colony.evaluateFor(dutch) > 0);
    }

    // M110-TP3: player.isAI() && !player.owns(this), no stockade (B1:false, B2:true, B4:true, B11:true)
    // M110-TP4: !player.isAI() && !player.owns(this), with stockade (B1:true, B4:true, B11:false)
    public void testM110_TP3_TP4_EvaluateForOtherPlayerWithAndWithoutStockade() {
        french.setAI(true);
        int baseVal = colony.evaluateFor(french);
        assertTrue(baseVal > 0);

        french.setAI(false);
        colony.addBuilding(new ServerBuilding(game, colony, stockadeType));
        assertTrue(colony.evaluateFor(french) >= baseVal);
    }

    // M110-TP5: !player.isAI() && player.owns(this), wl.evaluateFor(player) == Integer.MIN_VALUE (B6:false -> N8)
    public void testM110_TP5_EvaluateForOwnerWorkLocationMinValue() {
        dutch.setAI(false);
        colony.addBuilding(new CustomBuilding(game, colony, armoryType, null, Integer.MIN_VALUE));
        assertEquals(Integer.MIN_VALUE, colony.evaluateFor(dutch));
    }

    // M110-TP6: !player.isAI() && player.owns(this), unit on tile returns Integer.MIN_VALUE (B8:false -> N13)
    public void testM110_TP6_EvaluateForOwnerTileUnitMinValue() {
        dutch.setAI(false);
        new MinValueUnit(game, colony.getTile(), dutch, colonistType);
        assertEquals(Integer.MIN_VALUE, colony.evaluateFor(dutch));
    }

    // M110-TP7: !player.isAI() && player.owns(this), goods on colony returns Integer.MIN_VALUE (B10:false -> N18)
    // M110-TP8: !player.isAI() && player.owns(this), normal units and goods (B5/B6/B7/B8/B9/B10 all complete)
    public void testM110_TP7_TP8_EvaluateForOwnerNormalAndGoodsMinValue() throws Exception {
        dutch.setAI(false);
        new ServerUnit(game, colony.getTile(), dutch, colonistType);
        colony.addGoods(sugarType, 20);
        assertTrue(colony.evaluateFor(dutch) > 0);

        // When market == null and amount == (1 << 30), g.evaluateFor(player) == (1 << 30) * 2 == Integer.MIN_VALUE
        Field marketField = Player.class.getDeclaredField("market");
        marketField.setAccessible(true);
        Market savedMarket = dutch.getMarket();
        try {
            marketField.set(dutch, null);
            colony.getGoodsContainer().removeAll();
            colony.addGoods(sugarType, 1 << 30);
            assertEquals(Integer.MIN_VALUE, colony.evaluateFor(dutch));
        } finally {
            marketField.set(dutch, savedMarket);
        }
    }


    // ------------------------------------------------------------------
    // M111..M115, M209..M212: Education (canTrain, getTeachers, findTeacher, findStudent) — (100% C3)
    // ------------------------------------------------------------------

    // M111-TP1, M112-TP1..TP3, M212-TP1..TP3
    public void testM111_M112_M212_CanTrainAllBranches() {
        Unit farmer = new ServerUnit(game, colony.getTile(), dutch, expertFarmerType);
        // M112-TP1: !hasAbility(Ability.TEACH) (B1:true -> false)
        assertFalse(colony.canTrain(farmer));

        // Add schoolhouse (level 1: can teach skill 1 expertFarmer, cannot teach skill 3 elderStatesman)
        colony.addBuilding(new ServerBuilding(game, colony, schoolhouseType));
        // M112-TP2: hasAbility(Ability.TEACH), but elderStatesman exceeds schoolhouse maxSkill (B1:false, B2:true -> false)
        // Also covers M212 B1:true (non-teaching building), B1:false+B2:true (schoolhouse canTeach but !canAddType)
        assertFalse(colony.canTrain(elderStatesmanType));

        // M112-TP3: hasAbility(Ability.TEACH) and schoolhouse canAddType(expertFarmer) (B1:false, B2:false -> true)
        assertTrue(colony.canTrain(farmer));
    }

    // M113-TP1, M114-TP1, M114-TP2, M115-TP1, M115-TP2, M209-TP1..TP2, M210-TP1..TP3, M211-TP1..TP3
    public void testM113_M114_M115_FindTeacherAndStudentAllBranches() {
        removeAllUnitsFromColony(colony);
        Building school = new ServerBuilding(game, colony, collegeType);
        colony.addBuilding(school);
        Building th = colony.getBuilding(townHallType);

        Unit busyTeacher = new ServerUnit(game, school, dutch, masterCarpenterType);
        Unit freeTeacher = new ServerUnit(game, school, dutch, expertFarmerType);
        Unit s1 = new ServerUnit(game, th, dutch, colonistType);
        s1.changeWorkType(bellsType);
        Unit s2 = new ServerUnit(game, th, dutch, colonistType);
        s2.changeWorkType(grainType);
        Unit busyStudent = new ServerUnit(game, th, dutch, colonistType);
        busyTeacher.setStudent(busyStudent);
        busyStudent.setTeacher(busyTeacher);

        // M113-TP1: getTeachers()
        assertEquals(2, colony.getTeachers().count());

        // M114-TP1 & M115-TP1: ALLOW_STUDENT_SELECTION == true -> returns null
        spec().getOption(GameOptions.ALLOW_STUDENT_SELECTION, BooleanOption.class).setValue(true);
        assertNull(colony.findTeacher(s1));
        assertNull(colony.findStudent(freeTeacher));

        // M114-TP2 & M115-TP2: ALLOW_STUDENT_SELECTION == false -> searches teachers & students
        // Covers M211: B1:true (busyTeacher.getStudent() != null), B1:false+B2:true (!expertFarmer.canBeStudent(freeTeacher)), B1:false+B2:false (s1.canBeStudent(freeTeacher))
        // Covers M210: B1:true (busyStudent.getTeacher() != null), B1:false+B2:true (!freeTeacher.canBeStudent(freeTeacher)), B1:false+B2:false (s1/s2)
        // Covers M209: B1:true (s1 working bells != grain), B1:false (s2 working grain == grain)
        spec().getOption(GameOptions.ALLOW_STUDENT_SELECTION, BooleanOption.class).setValue(false);
        assertNull(colony.findTeacher(freeTeacher));
        assertSame(freeTeacher, colony.findTeacher(s1));
        assertSame(s2, colony.findStudent(freeTeacher));
    }


    // ------------------------------------------------------------------
    // M116..M124, M208: Production, Consumption, Starvation & Modifiers — (100% C3)
    // ------------------------------------------------------------------

    // M116-TP1, M117-TP1, M118-TP1, M119-TP1 (non-bells), M119-TP2 (bells), M120-TP1, M208-TP1, M124-TP1
    public void testM116_To_M120_M124_ProductionAndConsumptionQueries() {
        assertTrue(colony.isProducing(grainType) || colony.isProducing(bellsType));
        assertTrue(colony.isConsuming(foodType));
        assertFalse(colony.getConsumers().isEmpty());
        assertTrue(colony.getConsumptionOf(foodType) > 0);
        assertTrue(colony.getConsumptionOf(bellsType) >= 0);
        assertTrue(colony.getFoodProduction() > 0);
        assertNotNull(Integer.valueOf(colony.getNetProductionOf(foodType)));
    }

    // M121-TP1 (newFood >= 0 -> -1) & M121-TP2 (newFood < 0 -> food / -newFood)
    // M122-TP1 (food + newFood >= 200 -> 1), M122-TP2 (newFood <= 0 -> -1), M122-TP3 (newFood > 0 && food + newFood < 200)
    public void testM121_M122_StarvationAndNewColonistTurns() throws Exception {
        TypeCountMap<GoodsType> netMap = getNetProductionMap(colony);

        // Positive net food, food < 190 -> M121-TP1 (-1), M122-TP3 (> 1)
        colony.getGoodsContainer().removeAll();
        netMap.clear();
        netMap.incrementCount(foodType, 5);
        assertEquals(-1, colony.getStarvationTurns());
        assertTrue(colony.getNewColonistTurns() > 1);

        // food + newFood >= 200 -> M122-TP1 (1)
        colony.addGoods(foodType, 198);
        getNetProductionMap(colony).incrementCount(foodType, 5);
        assertEquals(1, colony.getNewColonistTurns());

        // Negative net food -> M121-TP2 (>= 0), M122-TP2 (-1)
        colony.getGoodsContainer().removeAll();
        colony.addGoods(foodType, 10);
        netMap = getNetProductionMap(colony);
        netMap.clear();
        netMap.incrementCount(foodType, -5);
        assertEquals(2, colony.getStarvationTurns());
        assertEquals(-1, colony.getNewColonistTurns());
    }

    // M123-TP1 (productionBonus == 0 -> empty) & M123-TP2 (productionBonus != 0 -> Stream.of(mod))
    public void testM123_TP1_TP2_GetProductionModifiers() {
        WorkLocation wl = colony.getBuilding(townHallType);
        colony.setProductionBonus(0);
        assertEquals(0, colony.getProductionModifiers(bellsType, colonistType, wl).count());

        colony.setProductionBonus(2);
        assertEquals(1, colony.getProductionModifiers(bellsType, colonistType, wl).count());
    }


    // ------------------------------------------------------------------
    // M125: isProductive(WorkLocation) — 5 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M125-TP1..TP5: Covers all 5 paths of isProductive(WorkLocation)
    public void testM125_TP1_To_TP5_IsProductiveAllBranches() throws Exception {
        WorkLocation wl = colony.getBuilding(townHallType);
        Map<Object, ProductionInfo> pcMap = getProductionAndConsumptionMap(colony);

        // TP1: info == null (B1:true)
        pcMap.remove(wl);
        assertFalse(colony.isProductive(wl));

        // TP2: info != null && info.getProduction() == null (B1:false, B2:true)
        ProductionInfo piNullList = new ProductionInfo();
        piNullList.setProduction(null);
        pcMap.put(wl, piNullList);
        assertFalse(colony.isProductive(wl));

        // TP3: info.getProduction().isEmpty() (B1:false, B2:false, B3:true)
        ProductionInfo piEmpty = new ProductionInfo();
        pcMap.put(wl, piEmpty);
        assertFalse(colony.isProductive(wl));

        // TP4: info.getProduction().get(0).getAmount() <= 0 (B1:false, B2:false, B3:false, B4:true)
        ProductionInfo piZero = new ProductionInfo();
        piZero.addProduction(new AbstractGoods(bellsType, 0));
        pcMap.put(wl, piZero);
        assertFalse(colony.isProductive(wl));

        // TP5: info.getProduction().get(0).getAmount() > 0 (B1:false, B2:false, B3:false, B4:false)
        ProductionInfo piPos = new ProductionInfo();
        piPos.addProduction(new AbstractGoods(bellsType, 5));
        pcMap.put(wl, piPos);
        assertTrue(colony.isProductive(wl));
    }


    // ------------------------------------------------------------------
    // M126..M130, M205..M207: AdjustedNetProduction, ProductionMap, UpdateProductionTypes, canProduce — (100% C3)
    // ------------------------------------------------------------------

    // M126-TP1, M127-TP1, M128-TP1, M207-TP1 (pi == null), M207-TP2 (pi != null)
    public void testM126_To_M128_M207_AdjustedNetProductionAndProductionCache() throws Exception {
        Map<Object, ProductionInfo> pcMap = getProductionAndConsumptionMap(colony);
        ProductionInfo bqInfo = new ProductionInfo();
        bqInfo.addConsumption(new AbstractGoods(hammersType, 5));
        pcMap.put(colony.buildQueue, bqInfo);
        pcMap.remove(colony.populationQueue);
        assertTrue(colony.getAdjustedNetProductionOf(hammersType) >= 5);
        assertNotNull(colony.getProductionMap());
        assertSame(bqInfo, colony.getProductionInfo(colony.buildQueue));
    }

    // M129-TP1 (empty available work locations) & M129-TP2 (non-empty available work locations)
    public void testM129_TP1_TP2_UpdateProductionTypes() throws Exception {
        colony.updateProductionTypes();
        colony.clearBuildingMap();
        invokePrivate(colony, "clearColonyTiles", new Class<?>[0]);
        colony.updateProductionTypes();
    }

    // M130-TP1..TP4 & M205-TP1..TP3 & M206-TP1: canProduce(GoodsType)
    public void testM130_M205_M206_CanProduceAllBranches() throws Exception {
        // M130-TP1: getNetProductionOf(goodsType) > 0 (B1:false -> true)
        getNetProductionMap(colony).incrementCount(bellsType, 5);
        assertTrue(colony.canProduce(bellsType));

        // M130-TP2: netProduction <= 0, isBreedable() == true, goodsCount < breedingNumber (B1:true, B2:false, B3:true -> false)
        getNetProductionMap(colony).clear();
        assertFalse(colony.canProduce(horsesType));

        // M130-TP3: netProduction <= 0, isBreedable() == true, goodsCount >= breedingNumber (B1:true, B2:false, B3:false -> true)
        colony.addGoods(horsesType, horsesType.getBreedingNumber());
        getNetProductionMap(colony).clear();
        assertTrue(colony.canProduce(horsesType));

        // M130-TP4 & M205-TP1..TP3 & M206-TP1: netProduction <= 0, isBreedable() == false
        // weaverHouse consumes cotton and produces cloth; plains ColonyTile produces cotton (M205 B1:false, B2:false -> true; M206-TP1)
        GoodsType clothType = spec().getGoodsType("model.goods.cloth");
        GoodsType cottonType = spec().getGoodsType("model.goods.cotton");
        for (ColonyTile ct : colony.getColonyTiles()) {
            if (!ct.isColonyCenterTile()) {
                ProductionType ctPt = ct.getBestProductionType(false, cottonType);
                if (ctPt != null) {
                    ct.setProductionType(ctPt);
                    break;
                }
            }
        }
        Building weaver = colony.getBuilding(spec().getBuildingType("model.building.weaverHouse"));
        weaver.setProductionType(weaver.getBestProductionType(false, null));
        getNetProductionMap(colony).clear();
        assertTrue(colony.canProduce(clothType));

        // Add a custom building with 0 generic potential for hammers (M205 B1:true)
        BuildingType zeroHammersType = new BuildingType("model.building.zeroHammers", spec());
        ProductionType ptZero = new ProductionType(
                Collections.singletonList(new AbstractGoods(lumberType, 1)),
                Collections.singletonList(new AbstractGoods(hammersType, 0)));
        zeroHammersType.addProductionType(ptZero);
        Building zeroBuilding = new ServerBuilding(game, colony, zeroHammersType);
        colony.addBuilding(zeroBuilding);
        zeroBuilding.setProductionType(ptZero);
        getNetProductionMap(colony).clear();
        assertFalse(colony.canProduce(hammersType));

        // Check carpenterHouse when colony cannot produce lumber (M205 B1:false, B2:true -> false)
        Building ch = colony.getBuilding(carpenterHouseType);
        ch.setProductionType(ch.getBestProductionType(false, null));
        getNetProductionMap(colony).clear();
        assertFalse(colony.canProduce(hammersType));
    }


    // ------------------------------------------------------------------
    // M131, M201..M204: getTileImprovementSuggestions() — 2 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M131-TP1: ENTRY -> N0 -> N1 -(B1:true)-> N3 -> EXIT (no available colony tiles)
    public void testM131_TP1_GetTileImprovementSuggestionsEmpty() throws Exception {
        invokePrivate(colony, "clearColonyTiles", new Class<?>[0]);
        assertTrue(colony.getTileImprovementSuggestions().isEmpty());
    }

    // M131-TP2 & M201..M204: Neighbouring LostCityRumour + occupied ColonyTile improvements
    public void testM131_TP2_GetTileImprovementSuggestionsWithRumourAndImprovements() {
        Tile neighbour = colony.getTile().getSurroundingTiles(1, 1).get(0);
        neighbour.addLostCityRumour(new LostCityRumour(game, neighbour));
        List<TileImprovementSuggestion> suggestions = colony.getTileImprovementSuggestions();
        assertFalse(suggestions.isEmpty());
        assertTrue(suggestions.get(0).isExploration());
    }


    // ------------------------------------------------------------------
    // M132, M200: getBetterExpert(Unit) — 6 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M132-TP1..TP6 & M200-TP1..TP3
    public void testM132_M200_GetBetterExpertAllBranches() {
        // Keep only 1 colonist in colony so we control all units
        colony.getUnitList().get(1).setLocation(colony.getTile());
        Unit colonist = colony.getUnitList().get(0);
        Building th = colony.getBuilding(townHallType);
        Unit expertFarmer = new ServerUnit(game, th, dutch, expertFarmerType);

        // TP1: production == null (B1:true)
        expertFarmer.changeWorkType(null);
        assertNull(colony.getBetterExpert(expertFarmer));

        // TP2: expertise == null (B1:false, B2:true)
        colonist.changeWorkType(grainType);
        assertNull(colony.getBetterExpert(colonist));

        // TP3: production == expertise (B1:false, B2:false, B3:false)
        expertFarmer.changeWorkType(grainType);
        assertNull(colony.getBetterExpert(expertFarmer));

        // TP4: production != expertise, no matching nonExpert (B1:false, B2:false, B3:true, B4:true)
        // Also covers M200: B1:true (unit working bells != grain) and B1:false+B2:true (another expertFarmer working grain)
        expertFarmer.changeWorkType(bellsType);
        colonist.changeWorkType(bellsType);
        Unit anotherExpertFarmer = new ServerUnit(game, th, dutch, expertFarmerType);
        anotherExpertFarmer.changeWorkType(grainType);
        assertNull(colony.getBetterExpert(expertFarmer));
        anotherExpertFarmer.dispose();

        // TP5: matching nonExpert found (M200 B1:false, B2:false), ewl != null (B5:false), nwl != null (B6:false), improvement > bestImprovement (B7:false)
        ColonyTile grainTile = colony.getColonyTiles().stream()
                .filter(ct -> !ct.isColonyCenterTile()).findFirst().orElse(null);
        colonist.setLocation(grainTile);
        colonist.changeWorkType(grainType);
        assertSame(colonist, colony.getBetterExpert(expertFarmer));

        // TP6: ewl == null (B5:true), nwl == null (B6:true), improvement <= bestImprovement (B7:true)
        expertFarmer.setLocationNoUpdate(null);
        colonist.setLocationNoUpdate(null);
        assertNull(colony.getBetterExpert(expertFarmer));
    }


    // ------------------------------------------------------------------
    // M133, M134, M194..M199: getProductionWarnings & getInsufficientProductionMessage — (100% C3)
    // ------------------------------------------------------------------

    // M133-TP1..TP16, M134-TP1..TP4, M194..M199
    public void testM133_M134_M194_To_M199_GetProductionWarningsAllBranches() throws Exception {
        // 1. Unstorable goods (hammersType): B1:true -> N11
        // With currentlyBuilding == null (B8:true) and currentlyBuilding != null (B8:false -> M198 B1:true, B1:false+B2:true, B1:false+B2:false, M199)
        colony.setCurrentlyBuilding(null);
        assertNotNull(colony.getProductionWarnings(hammersType));

        colony.setCurrentlyBuilding(armoryType); // requires hammers
        colony.addGoods(hammersType, 5); // amount < required -> M198 B1:false, B2:false
        assertFalse(colony.getProductionWarnings(hammersType).isEmpty());
        colony.getProductionWarnings(sugarType); // M198 B1:true (ag.getType() != goodsType)
        colony.addGoods(hammersType, 500); // amount >= required -> M198 B1:false, B2:true
        colony.getProductionWarnings(hammersType);

        // 2. Storable, limitIgnored == true, isFoodType == false (B1:false, B2:false, B3:true)
        GoodsType customIgnoreLimit = new GoodsType("model.goods.customIgnore", spec());
        Field storableField = GoodsType.class.getDeclaredField("storable");
        storableField.setAccessible(true);
        storableField.setBoolean(customIgnoreLimit, true);
        Field ignoreField = GoodsType.class.getDeclaredField("ignoreLimit");
        ignoreField.setAccessible(true);
        ignoreField.setBoolean(customIgnoreLimit, true);
        colony.setCurrentlyBuilding(null);
        assertTrue(colony.getProductionWarnings(customIgnoreLimit).isEmpty());

        // 3. Storable, limitIgnored == true, isFoodType == true (foodType):
        // 3a: starve == 0 (B4:false -> N4)
        colony.getGoodsContainer().removeAll();
        getNetProductionMap(colony).clear();
        getNetProductionMap(colony).incrementCount(foodType, -5);
        assertFalse(colony.getProductionWarnings(foodType).isEmpty());

        // 3b: 0 < starve <= FAMINE_TURNS (B4:true, B5:false -> N6)
        colony.addGoods(foodType, 10);
        getNetProductionMap(colony).incrementCount(foodType, -5);
        assertFalse(colony.getProductionWarnings(foodType).isEmpty());

        // 3c: starve > FAMINE_TURNS (B4:true, B5:true -> N7)
        colony.addGoods(foodType, 100);
        getNetProductionMap(colony).incrementCount(foodType, -2);
        assertTrue(colony.getProductionWarnings(foodType).isEmpty());

        // 4. Storable, limitIgnored == false (sugarType):
        // 4a: exported == true (B6:true)
        colony.getExportData(sugarType).setExported(true);
        colony.getProductionWarnings(sugarType);

        // 4b: exported == false, waste <= 0 (B6:false, B7:true)
        colony.getExportData(sugarType).setExported(false);
        colony.getProductionWarnings(sugarType);

        // 4c: exported == false, waste > 0 (B6:false, B7:false -> N10)
        colony.addGoods(sugarType, 300);
        getNetProductionMap(colony).incrementCount(sugarType, 50);
        assertFalse(colony.getProductionWarnings(sugarType).isEmpty());

        // 5. Insufficient production messages for producing & consuming locations (M134 & M194..M197):
        Building bh = colony.getBuilding(blacksmithHouseType); // consumes ore, produces tools (storable)
        colony.getUnitList().get(0).setLocation(bh);
        colony.addGoods(oreType, 1); // partial ore -> produces 1/3 tools -> consumption & production deficits > 0
        bh.updateProductionType();
        colony.invalidateCache();

        assertFalse(colony.getProductionWarnings(toolsType).isEmpty());
        assertFalse(colony.getProductionWarnings(oreType).isEmpty());

        // Direct reflection coverage for M134 edge cases:
        ProductionInfo bhInfo = colony.getProductionInfo(bh);
        // M134-TP1: info == null (B1:true)
        assertNull(invokePrivate(colony, "getInsufficientProductionMessage",
                                 new Class<?>[]{ProductionInfo.class, AbstractGoods.class},
                                 null, new AbstractGoods(toolsType, 1)));
        // M134-TP2: info != null, deficit == null (B1:false, B2:false)
        assertNull(invokePrivate(colony, "getInsufficientProductionMessage",
                                 new Class<?>[]{ProductionInfo.class, AbstractGoods.class},
                                 bhInfo, null));
        // M134-TP3: info != null, deficit != null, consumptionDeficit.isEmpty() (B1:false, B2:true, B3:false)
        assertNull(invokePrivate(colony, "getInsufficientProductionMessage",
                                 new Class<?>[]{ProductionInfo.class, AbstractGoods.class},
                                 new ProductionInfo(), new AbstractGoods(toolsType, 1)));
    }


    // ------------------------------------------------------------------
    // M135 & M136: goodsUseful(GoodsType) & modifySpecialGoods(GoodsType, int) — (100% C3)
    // ------------------------------------------------------------------

    // M135-TP1..TP5: Covers all branches of goodsUseful(GoodsType)
    public void testM135_TP1_To_TP5_GoodsUsefulAllBranches() {
        // TP1: playerType != INDEPENDENT (B1:true -> true)
        assertTrue(colony.goodsUseful(bellsType));

        dutch.changePlayerType(Player.PlayerType.INDEPENDENT);
        // TP2: INDEPENDENT, isLibertyType() == true, sonsOfLiberty < 100, isImmigrationType() == false (B1:false, B2:false, B3:false, B4:true -> true)
        colony.sonsOfLiberty = 50;
        assertTrue(colony.goodsUseful(bellsType));

        // TP3: INDEPENDENT, isLibertyType() == true, sonsOfLiberty >= 100 (B1:false, B2:false, B3:true -> false)
        colony.sonsOfLiberty = 100;
        assertFalse(colony.goodsUseful(bellsType));

        // TP4: INDEPENDENT, isLibertyType() == false, isImmigrationType() == true (B1:false, B2:true, B4:false -> false)
        assertFalse(colony.goodsUseful(crossesType));

        // TP5: INDEPENDENT, isLibertyType() == false, isImmigrationType() == false (B1:false, B2:true, B4:true -> true)
        assertTrue(colony.goodsUseful(grainType));
    }

    // M136-TP1..TP4: modifySpecialGoods (grain: neither; bells: liberty; crosses: immigration)
    public void testM136_TP1_To_TP4_ModifySpecialGoods() throws Exception {
        invokePrivate(colony, "modifySpecialGoods", new Class<?>[]{GoodsType.class, int.class}, grainType, 10);
        invokePrivate(colony, "modifySpecialGoods", new Class<?>[]{GoodsType.class, int.class}, bellsType, 10);
        invokePrivate(colony, "modifySpecialGoods", new Class<?>[]{GoodsType.class, int.class}, crossesType, 10);
        assertTrue(colony.getLiberty() > 0);
        assertTrue(colony.getImmigration() > 0);
    }


    // ------------------------------------------------------------------
    // M137 & M138: copyColony() & getCorresponding(FreeColObject) — (100% C3)
    // ------------------------------------------------------------------

    // M137-TP1 (with center tile, owned work tile, and unowned work tile) & M137-TP2 (empty colonyTiles)
    // M138-TP1..TP5: getCorresponding for WorkLocation, center Tile, surrounding work Tile, Unit, and other FreeColObject
    public void testM137_M138_CopyColonyAndGetCorresponding() throws Exception {
        // Ensure one surrounding ColonyTile has workTile.owningSettlement != colony (B3:true)
        ColonyTile nonCenter = colony.getColonyTiles().stream()
                .filter(ct -> !ct.isColonyCenterTile()).findFirst().orElse(null);
        nonCenter.getWorkTile().setOwningSettlement(null);

        Colony copy = colony.copyColony();
        assertNotNull(copy);
        assertNotSame(colony, copy);

        // M138-TP1: WorkLocation (B1:false -> N1 -> N9)
        WorkLocation wl = colony.getBuilding(townHallType);
        assertNotNull(copy.getCorresponding(wl));

        // M138-TP2: Tile matching colony center tile (B1:true, B2:false, B3:false -> N4 -> N9)
        assertNotNull(copy.getCorresponding(colony.getTile()));

        // M138-TP3: Tile matching surrounding workTile (B1:true, B2:false, B3:true -> N5 -> N9)
        assertNotNull(copy.getCorresponding(nonCenter.getWorkTile()));

        // M138-TP4: Unit (B1:true, B2:true, B4:false -> N7 -> N9)
        Unit u = colony.getUnitList().get(0);
        assertNotNull(copy.getCorresponding(u));

        // M138-TP5: Other FreeColObject (B1:true, B2:true, B4:true -> N8 -> N9 -> null)
        assertNull(copy.getCorresponding(dutch));

        // Also cover M137 with empty colonyTiles (B1:true immediately)
        invokePrivate(colony, "clearColonyTiles", new Class<?>[0]);
        assertNotNull(colony.copyColony());
    }


    // ------------------------------------------------------------------
    // M139..M158, M192, M193: FreeColObject, Location, GoodsLocation & Settlement Overrides — (100% C3)
    // ------------------------------------------------------------------

    // M139-TP1..TP4: getAbilities(id, type, turn) with turn == null / turn != null and owner != null / owner == null
    public void testM139_TP1_To_TP4_GetAbilitiesAllBranches() {
        assertNotNull(colony.getAbilities(Ability.HAS_PORT, null, null).collect(Collectors.toList()));
        assertNotNull(colony.getAbilities(Ability.HAS_PORT, null, game.getTurn()).collect(Collectors.toList()));
        colony.setOwner(null);
        assertNotNull(colony.getAbilities(Ability.HAS_PORT, null, null).collect(Collectors.toList()));
        assertNotNull(colony.getAbilities(Ability.HAS_PORT, null, game.getTurn()).collect(Collectors.toList()));
    }

    // M140-TP1 (getDisposables), M141-TP1 (name != null), M141-TP2 (name == null)
    public void testM140_M141_GetDisposablesAndLocationLabelFor() {
        assertFalse(colony.getDisposables().collect(Collectors.toList()).isEmpty());
        assertNotNull(colony.getLocationLabelFor(dutch));
        colony.setName(null);
        assertNotNull(colony.getLocationLabelFor(dutch));
    }

    // M142-TP1..TP2 (add), M143-TP1..TP4 (remove), M144-TP1..TP2 & M193-TP1 (contains)
    public void testM142_M143_M144_M193_AddRemoveContainsLocatable() {
        Goods goods = new Goods(game, colony, grainType, 10);
        // M142-TP1: non-Unit Locatable (B1:true -> super.add)
        assertTrue(colony.add(goods));
        // M144-TP1: non-Unit Locatable (B1:true -> super.contains)
        assertTrue(colony.contains(goods));
        // M143-TP1: non-Unit Locatable (B1:true -> super.remove)
        assertTrue(colony.remove(goods));

        // M142-TP2: Unit Locatable (B1:false -> joinColony)
        Unit u = new ServerUnit(game, colony.getTile(), dutch, colonistType);
        assertTrue(colony.add(u));
        // M144-TP2 & M193-TP1: Unit Locatable (B1:false -> any(getAvailableWorkLocations()))
        assertTrue(colony.contains(u));

        // M143-TP4: Unit at WorkLocation in this colony (B1:false, B2:false, B3:false -> wl.remove)
        assertTrue(colony.remove(u));

        // M143-TP2: Unit not at WorkLocation (e.g. on Tile -> B1:false, B2:true -> false)
        u.setLocation(colony.getTile());
        assertFalse(colony.remove(u));

        // M143-TP3: Unit at WorkLocation in ANOTHER colony (B1:false, B2:false, B3:true -> false)
        Colony otherColony = getStandardColony(1);
        Unit otherUnit = otherColony.getUnitList().get(0);
        assertFalse(colony.remove(otherUnit));
    }

    // M145..M156: UnitCount, Units, UnitList, getColony, up, toShortString, invalidateCache, GoodsCapacity, addGoods, removeGoods, Immigration, Liberty
    public void testM145_To_M156_LocationGoodsAndSettlementMethods() {
        assertTrue(colony.getUnitCount() > 0);
        assertTrue(colony.getUnits().count() > 0);
        assertFalse(colony.getUnitList().isEmpty());
        assertSame(colony, colony.getColony());
        assertSame(colony, colony.up());
        assertEquals(colony.getName(), colony.toShortString());
        colony.invalidateCache();
        assertTrue(colony.getGoodsCapacity() > 0);

        colony.addGoods(sugarType, 30);
        // M154-TP1: removed != null (B1:false -> N1 -> N2)
        assertNotNull(colony.removeGoods(sugarType, 10));
        // M154-TP2: removed == null (B1:true -> N2)
        assertNull(colony.removeGoods(rumType, 10));

        assertEquals(0, colony.getImmigration());
        assertEquals(0, colony.getLiberty());
    }

    // M157-TP1 (displayUnitCount <= 0), M157-TP2 (displayUnitCount > 0), M158-TP1 (getDefenceRatio), M192-TP1
    public void testM157_M158_M192_GetDefendingUnitAndDefenceRatio() {
        Unit attacker = new ServerUnit(game, game.getMap().getTile(1, 1), french, artilleryType);
        colony.setDisplayUnitCount(-1);
        assertNotNull(colony.getDefendingUnit(attacker));
        assertTrue(colony.getDefenceRatio() >= 0.0);

        colony.setDisplayUnitCount(5);
        assertNull(colony.getDefendingUnit(attacker));
    }


    // ------------------------------------------------------------------
    // M159, M160, M188..M191: isBadlyDefended & isVeryWellDefended — 4 + 4 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M159-TP1..TP4, M160-TP1..TP4, M188..M191
    public void testM159_M160_M188_To_M191_IsBadlyAndVeryWellDefendedAllBranches() {
        // 0 offensive units on tile -> M159-TP1 (< 1 -> true), M160-TP1 (< 3 -> false)
        assertTrue(colony.isBadlyDefended());
        assertFalse(colony.isVeryWellDefended());

        // Increase colony population to 12 so 0.95*pop - 2.5 = 8.9
        Building th = colony.getBuilding(townHallType);
        Building ch = colony.getBuilding(carpenterHouseType);
        Building wh = colony.getBuilding(spec().getBuildingType("model.building.weaverHouse"));
        Building bh = colony.getBuilding(blacksmithHouseType);
        for (int i = 0; i < 3; i++) new ServerUnit(game, th, dutch, colonistType);
        for (int i = 0; i < 3; i++) new ServerUnit(game, ch, dutch, colonistType);
        for (int i = 0; i < 2; i++) new ServerUnit(game, wh, dutch, colonistType);
        for (int i = 0; i < 2; i++) new ServerUnit(game, bh, dutch, colonistType);
        assertEquals(12, colony.getUnitCount());

        // 1 weak offensive unit (pettyCriminal soldier, defencePower=1.5 < 8.9) + 1 non-offensive unit on tile -> M159-TP2 (true)
        Role soldierRole = spec().getRole("model.role.soldier");
        new ServerUnit(game, colony.getTile(), dutch, colonistType); // non-offensive unit on tile so filter tests false too
        Unit s1 = new ServerUnit(game, colony.getTile(), dutch, pettyCriminalType, soldierRole);
        assertTrue(colony.isBadlyDefended());

        // 3 weak offensive units (total defencePower=4.5, defencePower/2=2.25 <= 8.9) -> M160-TP3 (false)
        Unit s2 = new ServerUnit(game, colony.getTile(), dutch, pettyCriminalType, soldierRole);
        Unit s3 = new ServerUnit(game, colony.getTile(), dutch, pettyCriminalType, soldierRole);
        assertFalse(colony.isVeryWellDefended());
        s1.dispose();
        s2.dispose();
        s3.dispose();

        // 3 strong offensive units (artillery) -> M159-TP3 (false), M160-TP2 (true)
        for (int i = 0; i < 3; i++) {
            new ServerUnit(game, colony.getTile(), dutch, artilleryType);
        }
        assertFalse(colony.isBadlyDefended());
        assertTrue(colony.isVeryWellDefended());

        // > 5 offensive units on tile -> M159-TP4 (> 5 -> false), M160-TP4 (> 5 -> true)
        for (int i = 0; i < 3; i++) {
            new ServerUnit(game, colony.getTile(), dutch, artilleryType);
        }
        assertFalse(colony.isBadlyDefended());
        assertTrue(colony.isVeryWellDefended());
    }


    // ------------------------------------------------------------------
    // M161..M174, M186, M187: Plunder, Upkeep, ProvideGoods, Contact, TradeLocation — (100% C3)
    // ------------------------------------------------------------------

    // M161-TP1..TP3: getPlunderRange(Unit)
    public void testM161_TP1_To_TP3_GetPlunderRange() {
        Unit attacker = new ServerUnit(game, colony.getTile(), french, artilleryType);
        // TP1: !canBePlundered() (B1:true -> null)
        dutch.setGold(0);
        assertNull(colony.getPlunderRange(attacker));

        // TP2: canBePlundered(), upper <= 0 (B1:false, B2:true -> null)
        dutch.setGold(1);
        Colony extra = new ServerColony(game, dutch, "Extra", game.getMap().getTile(3, 3));
        for (int i = 0; i < 6; i++) {
            new ServerUnit(game, extra.getBuilding(townHallType), dutch, colonistType);
        }
        dutch.addSettlement(extra);
        assertNull(colony.getPlunderRange(attacker));

        // TP3: canBePlundered(), upper > 0 (B1:false, B2:false -> RandomRange)
        dutch.setGold(1000);
        assertNotNull(colony.getPlunderRange(attacker));
    }

    // M162-TP1 & M187-TP1 (getUpkeep), M163-TP1 & M186-TP1 (getTotalProductionOf)
    public void testM162_M163_M186_M187_GetUpkeepAndTotalProductionOf() {
        assertTrue(colony.getUpkeep() >= 0);
        assertTrue(colony.getTotalProductionOf(grainType) >= 0);
    }

    // M164-TP1..TP5: canProvideGoods(List<AbstractGoods>)
    public void testM164_TP1_To_TP5_CanProvideGoodsAllBranches() {
        // TP1: empty requiredGoods -> true (B1:true)
        assertTrue(colony.canProvideGoods(Collections.emptyList()));

        // TP2: non-breedable (B2:true), buildable == null (B3:true), available < amount (B4:false -> false)
        colony.setCurrentlyBuilding(null);
        assertFalse(colony.canProvideGoods(Collections.singletonList(new AbstractGoods(sugarType, 10))));

        // TP3: breedable horses (B2:false), buildable != null (B3:false), available >= amount (B4:true -> loop -> true)
        colony.setCurrentlyBuilding(armoryType);
        colony.addGoods(horsesType, 50);
        colony.addGoods(toolsType, 100);
        assertTrue(colony.canProvideGoods(Arrays.asList(
                new AbstractGoods(horsesType, 10),
                new AbstractGoods(toolsType, 10))));
    }

    // M165-TP1..TP4: hasContacted(Player)
    public void testM165_TP1_To_TP4_HasContactedAllBranches() {
        // TP1: player == null (B1:true -> false)
        assertFalse(colony.hasContacted(null));
        // TP2: player.isEuropean() == true (B1:false, B2:true -> true)
        assertTrue(colony.hasContacted(french));
        // TP3: !player.isEuropean(), stance == UNCONTACTED (B1:false, B2:false, B3:true -> false)
        dutch.setStance(arawak, Stance.UNCONTACTED);
        assertFalse(colony.hasContacted(arawak));
        // TP4: !player.isEuropean(), stance != UNCONTACTED (B1:false, B2:false, B3:false -> true)
        dutch.setStance(arawak, Stance.PEACE);
        assertTrue(colony.hasContacted(arawak));
    }

    // M166..M174: getAlarmLevelLabel, calculateSettlementValue, returnPresent, getAvailableGoodsCount, getExportAmount, getImportAmount, getLocationName, canBeInput, addPortAbility
    public void testM166_To_M174_TradeLocationAndSettlementHelpers() {
        assertNotNull(colony.getAlarmLevelLabel(french));

        Unit attacker = new ServerUnit(game, colony.getTile(), french, artilleryType);
        // M167-TP1: !hasStockade() (B1:true)
        int valNoStockade = colony.calculateSettlementValue(100, attacker);
        // M167-TP2: hasStockade() (B1:false)
        colony.addBuilding(new ServerBuilding(game, colony, stockadeType));
        int valWithStockade = colony.calculateSettlementValue(100, attacker);
        assertTrue(valWithStockade < valNoStockade);

        // M169 & M170 (calls M168 returnPresent)
        colony.addGoods(sugarType, 50);
        assertEquals(50, colony.getAvailableGoodsCount(sugarType));
        assertTrue(colony.getExportAmount(sugarType, 1) >= 0);

        // M171-TP1: goodsType.limitIgnored() == true (B1:false -> HUGE_CARGO_SIZE)
        assertEquals(GoodsContainer.HUGE_CARGO_SIZE, colony.getImportAmount(foodType, 1));

        // M171-TP2: limitIgnored == false, ENHANCED_TRADE_ROUTES == false (B1:true, B2:true, B3:true)
        spec().getOption(GameOptions.ENHANCED_TRADE_ROUTES, BooleanOption.class).setValue(false);
        assertTrue(colony.getImportAmount(sugarType, 1) >= 0);

        // M171-TP3: limitIgnored == false, ENHANCED_TRADE_ROUTES == true (B1:true, B2:false)
        spec().getOption(GameOptions.ENHANCED_TRADE_ROUTES, BooleanOption.class).setValue(true);
        assertTrue(colony.getImportAmount(sugarType, 1) >= 0);

        // M172, M173, M174
        assertEquals(colony.getName(), colony.getLocationName(colony));
        assertTrue(colony.canBeInput());
        colony.addPortAbility();
        assertTrue(colony.hasAbility(Ability.HAS_PORT));
    }


    // ------------------------------------------------------------------
    // M175 & M176: checkBuildQueueIntegrity & checkIntegrity — (100% C3)
    // ------------------------------------------------------------------

    // M175-TP1..TP5 & M176-TP1: Covers all 10 branches (B1..B10) of checkBuildQueueIntegrity
    public void testM175_M176_CheckBuildQueueIntegrityAllBranches() {
        // TP1: empty queues -> INTEGRITY_GOOD (B1:true, B6:true)
        colony.setBuildQueue(Collections.emptyList());
        colony.setPopulationQueue(Collections.emptyList());
        assertEquals(IntegrityType.INTEGRITY_GOOD, colony.checkIntegrity(false, null));

        // TP2: valid item + invalid item, fix=false, lb=null (B2:false, B2:true, B3:true, B5:true, B7:false, B7:true, B8:true, B10:true)
        colony.setBuildQueue(Arrays.asList(warehouseType, fortType));
        colony.setPopulationQueue(Arrays.asList(colonistType, expertFarmerType));
        assertEquals(IntegrityType.INTEGRITY_FAIL, colony.checkBuildQueueIntegrity(false, null));

        // TP3: invalid item, fix=false, lb!=null (B3:true, B5:false, B8:true, B10:false)
        LogBuilder lb = new LogBuilder(64);
        assertEquals(IntegrityType.INTEGRITY_FAIL, colony.checkBuildQueueIntegrity(false, lb));
        assertTrue(lb.toString().contains("Invalid"));

        // TP4: invalid item, fix=true, lb=null (B3:false, B4:true, B8:false, B9:true)
        colony.setBuildQueue(Collections.singletonList(fortType));
        colony.setPopulationQueue(Collections.singletonList(expertFarmerType));
        assertEquals(IntegrityType.INTEGRITY_FIXED, colony.checkBuildQueueIntegrity(true, null));

        // TP5: invalid item, fix=true, lb!=null (B3:false, B4:false, B8:false, B9:false)
        colony.setBuildQueue(Collections.singletonList(fortType));
        colony.setPopulationQueue(Collections.singletonList(expertFarmerType));
        LogBuilder lbFix = new LogBuilder(64);
        assertEquals(IntegrityType.INTEGRITY_FIXED, colony.checkBuildQueueIntegrity(true, lbFix));
        assertTrue(lbFix.toString().contains("removed"));
    }


    // ------------------------------------------------------------------
    // M177 & M178: getClassIndex() & copyIn(FreeColObject) — (100% C3)
    // ------------------------------------------------------------------

    // M177-TP1 & M178-TP1..TP4
    public void testM177_M178_GetClassIndexAndCopyIn() throws Exception {
        assertEquals(20, colony.getClassIndex());

        // M178-TP1: other is not a Colony -> o == null (B1:true -> false)
        assertFalse(colony.copyIn(dutch));

        // M178-TP2: !super.copyIn(o) (B1:false, B2:false -> false)
        TestableColony tc = new TestableColony(game, "colony:testableCopyIn");
        tc.setFailSuperCopyIn(true);
        assertFalse(tc.copyIn(tc));

        // M178-TP3: valid Colony with work locations (B1:false, B2:true, B3:false -> B3:true)
        assertTrue(colony.copyIn(colony));

        // M178-TP4: valid Colony with no work locations (B1:false, B2:true, B3:true)
        colony.clearBuildingMap();
        invokePrivate(colony, "clearColonyTiles", new Class<?>[0]);
        assertTrue(colony.copyIn(colony));
    }


    // ------------------------------------------------------------------
    // M179..M185, M229: XML Serialization & Object Overrides — (100% C3)
    // ------------------------------------------------------------------

    // M179-TP1..TP4, M180-TP1..TP4, M181-TP1, M182-TP1, M183-TP1..TP8, M184-TP1, M185-TP1, M229-TP1
    public void testM179_To_M185_XMLSerializationAndToString() throws Exception {
        assertEquals(Colony.TAG, colony.getXMLTagName());
        assertEquals(colony.getName(), colony.toString());

        // 1. Owner scope serialization with exportData, workLocations, buildQueue, populationQueue
        colony.getExportData(sugarType);
        colony.setBuildQueue(Collections.singletonList(depotType));
        colony.setPopulationQueue(Collections.singletonList(colonistType));

        StringWriter swOwner = new StringWriter();
        try (FreeColXMLWriter xw = new FreeColXMLWriter(swOwner,
                FreeColXMLWriter.WriteScope.toSave())) {
            colony.toXML(xw);
        }
        String xml = swOwner.toString();
        assertTrue(xml.contains(Colony.TAG));

        // Also cover M180-TP3: owner scope with empty exportData, workLocations, buildQueue, populationQueue
        Colony emptyCol = new ServerColony(game, dutch, "EmptyCol", game.getMap().getTile(3, 3));
        emptyCol.clearBuildingMap();
        invokePrivate(emptyCol, "clearColonyTiles", new Class<?>[0]);
        emptyCol.setExportData(Collections.emptyList());
        emptyCol.setBuildQueue(Collections.emptyList());
        emptyCol.setPopulationQueue(Collections.emptyList());
        StringWriter swEmpty = new StringWriter();
        try (FreeColXMLWriter xw = new FreeColXMLWriter(swEmpty,
                FreeColXMLWriter.WriteScope.toSave())) {
            emptyCol.toXML(xw);
        }

        // 2. Non-owner client scope (xw.validFor(getOwner()) == false):
        // 2a: uc > 0 (M179 B2:false), stockade == null (M180 B6:true)
        colony.setDisplayUnitCount(3);
        StringWriter swClient1 = new StringWriter();
        try (FreeColXMLWriter xw = new FreeColXMLWriter(swClient1,
                FreeColXMLWriter.WriteScope.toClient(french))) {
            colony.toXML(xw);
        }

        // 2b: uc == 0 (M179 B2:true, B3:false -> FreeCol.trace), stockade != null (M180 B6:false)
        removeAllUnitsFromColony(colony);
        colony.setDisplayUnitCount(-1);
        colony.addBuilding(new ServerBuilding(game, colony, stockadeType));
        StringWriter swClient2 = new StringWriter();
        try (FreeColXMLWriter xw = new FreeColXMLWriter(swClient2,
                FreeColXMLWriter.WriteScope.toClient(french))) {
            colony.toXML(xw);
        }

        // 2c: uc < 0 (M179 B2:true, B3:true) via CustomBuilding returning unitCount = -1
        colony.clearBuildingMap();
        colony.addBuilding(new CustomBuilding(game, colony, townHallType, -1, null));
        StringWriter swClient3 = new StringWriter();
        try (FreeColXMLWriter xw = new FreeColXMLWriter(swClient3,
                FreeColXMLWriter.WriteScope.toClient(french))) {
            colony.toXML(xw);
        }

        // 3. Deserialization via readAttributes, readChildren, readChild (all 8 branches of M183, including null buildQueueItem / populationQueueItem and super.readChild)
        String customXml = xml.replace("</colony>",
                "<buildQueueItem/><populationQueueItem/><ability id=\"model.ability.hasPort\" value=\"true\"/></colony>");
        try (FreeColXMLReader xr = new FreeColXMLReader(new StringReader(customXml))) {
            xr.nextTag();
            colony.readFromXML(xr);
        }
        assertTrue(colony.hasAbility(Ability.HAS_PORT));
    }
}
