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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import net.sf.freecol.common.model.BuildQueue.CompletionAction;
import net.sf.freecol.common.option.BooleanOption;
import net.sf.freecol.common.option.GameOptions;
import net.sf.freecol.util.test.FreeColTestCase;


/**
 * CFG-driven test suite for {@link BuildQueue} derived from
 * {@code test_cft/graph/net/sf/freecol/common/model/BuildQueue.json}.
 *
 * Covers all 17 methods (M1..M17, including {@code <init>}) and achieves 100%
 * C3 (node + edge) coverage across 30 Test Paths.
 */
public class BuildQueueCfgTest extends FreeColTestCase {

    private Colony colony;
    private Specification spec;
    private BuildingType warehouseType;
    private BuildingType carpenterHouseType;
    private UnitType wagonTrainType;
    private GoodsType hammersType;
    private GoodsType toolsType;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        // Pre-condition: Initialize standard game, colony, and specification types
        Game game = getStandardGame();
        game.setMap(getTestMap(true));
        colony = createStandardColony(3);
        spec = colony.getSpecification();
        warehouseType = spec.getBuildingType("model.building.warehouse");
        carpenterHouseType = spec.getBuildingType("model.building.carpenterHouse");
        wagonTrainType = spec.getUnitType("model.unit.wagonTrain");
        hammersType = spec.getGoodsType("model.goods.hammers");
        toolsType = spec.getGoodsType("model.goods.tools");
    }

    // ------------------------------------------------------------------
    // Pre-condition helpers & stubs
    // ------------------------------------------------------------------

    private BuildQueue<BuildableType> newQueue() {
        return new BuildQueue<>(colony, CompletionAction.REMOVE_EXCEPT_LAST,
                Consumer.COLONY_PRIORITY);
    }

    // ------------------------------------------------------------------
    // M1: <init>(Lnet/sf/freecol/common/model/Colony;Lnet/sf/freecol/common/model/BuildQueue$CompletionAction;I)V — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M1-TP1: ENTRY -> N0 -> EXIT (Construct BuildQueue with colony, action, and priority)
    public void testM1_TP1_Constructor() {
        BuildQueue<BuildableType> bq = new BuildQueue<>(colony, CompletionAction.SHUFFLE, 42);
        assertEquals(CompletionAction.SHUFFLE, bq.getCompletionAction());
        assertEquals(42, bq.getPriority());
        assertTrue(bq.isEmpty());
    }

    // ------------------------------------------------------------------
    // M2: clear()V — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M2-TP1: ENTRY -> N0 -> EXIT (Clear all queued items)
    public void testM2_TP1_Clear() {
        BuildQueue<BuildableType> bq = newQueue();
        bq.add(warehouseType);
        bq.clear();
        assertTrue(bq.isEmpty());
    }

    // ------------------------------------------------------------------
    // M3: add(Lnet/sf/freecol/common/model/BuildableType;)V — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M3-TP1: ENTRY -> N0 -> EXIT (Add buildable to queue)
    public void testM3_TP1_Add() {
        BuildQueue<BuildableType> bq = newQueue();
        bq.add(warehouseType);
        assertEquals(1, bq.size());
        assertSame(warehouseType, bq.getValues().get(0));
    }

    // ------------------------------------------------------------------
    // M4: getValues()Ljava/util/List; — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M4-TP1: ENTRY -> N0 -> EXIT (Return underlying queue list)
    public void testM4_TP1_GetValues() {
        BuildQueue<BuildableType> bq = newQueue();
        bq.add(warehouseType);
        List<BuildableType> values = bq.getValues();
        assertEquals(1, values.size());
        assertSame(warehouseType, values.get(0));
    }

    // ------------------------------------------------------------------
    // M5: setValues(Ljava/util/List;)V — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M5-TP1: ENTRY -> N0 -> EXIT (Replace queue contents with given list)
    public void testM5_TP1_SetValues() {
        BuildQueue<BuildableType> bq = newQueue();
        bq.add(warehouseType);
        bq.setValues(Arrays.asList(carpenterHouseType, wagonTrainType));
        assertEquals(2, bq.size());
        assertSame(carpenterHouseType, bq.getValues().get(0));
    }

    // ------------------------------------------------------------------
    // M6: remove(I)V — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M6-TP1: ENTRY -> N0 -> EXIT (Remove item at index)
    public void testM6_TP1_RemoveByIndex() {
        BuildQueue<BuildableType> bq = newQueue();
        bq.add(warehouseType);
        bq.add(carpenterHouseType);
        bq.remove(0);
        assertEquals(1, bq.size());
        assertSame(carpenterHouseType, bq.getValues().get(0));
    }

    // ------------------------------------------------------------------
    // M7: size()I — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M7-TP1: ENTRY -> N0 -> EXIT (Return queue size)
    public void testM7_TP1_Size() {
        BuildQueue<BuildableType> bq = newQueue();
        assertEquals(0, bq.size());
        bq.add(warehouseType);
        assertEquals(1, bq.size());
    }

    // ------------------------------------------------------------------
    // M8: isEmpty()Z — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M8-TP1: ENTRY -> N0 -> EXIT (Return whether queue is empty)
    public void testM8_TP1_IsEmpty() {
        BuildQueue<BuildableType> bq = newQueue();
        assertTrue(bq.isEmpty());
        bq.add(warehouseType);
        assertFalse(bq.isEmpty());
    }

    // ------------------------------------------------------------------
    // M9: getCompletionAction()Lnet/sf/freecol/common/model/BuildQueue$CompletionAction; — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M9-TP1: ENTRY -> N0 -> EXIT (Return completionAction)
    public void testM9_TP1_GetCompletionAction() {
        BuildQueue<BuildableType> bq = newQueue();
        assertEquals(CompletionAction.REMOVE_EXCEPT_LAST, bq.getCompletionAction());
    }

    // ------------------------------------------------------------------
    // M10: setCompletionAction(Lnet/sf/freecol/common/model/BuildQueue$CompletionAction;)V — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M10-TP1: ENTRY -> N0 -> EXIT (Update completionAction)
    public void testM10_TP1_SetCompletionAction() {
        BuildQueue<BuildableType> bq = newQueue();
        bq.setCompletionAction(CompletionAction.ADD_RANDOM);
        assertEquals(CompletionAction.ADD_RANDOM, bq.getCompletionAction());
    }

    // ------------------------------------------------------------------
    // M11: getCurrentlyBuilding()Lnet/sf/freecol/common/model/BuildableType; — 2 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M11-TP1: ENTRY -> N0 -(B1:false)-> N1 -> N3 -> EXIT (queue is empty -> return null)
    public void testM11_TP1_GetCurrentlyBuildingEmpty() {
        BuildQueue<BuildableType> bq = newQueue();
        assertNull(bq.getCurrentlyBuilding());
    }

    // M11-TP2: ENTRY -> N0 -(B1:true)-> N2 -> N3 -> EXIT (queue non-empty -> return queue.get(0))
    public void testM11_TP2_GetCurrentlyBuildingNonEmpty() {
        BuildQueue<BuildableType> bq = newQueue();
        bq.add(warehouseType);
        bq.add(carpenterHouseType);
        assertSame(warehouseType, bq.getCurrentlyBuilding());
    }

    // ------------------------------------------------------------------
    // M12: setCurrentlyBuilding(Lnet/sf/freecol/common/model/BuildableType;)V — 4 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M12-TP1: ENTRY -> N0 -(B1:false)-> N1 -> N6 -> EXIT (buildable == null -> clear queue)
    public void testM12_TP1_SetCurrentlyBuildingNull() {
        BuildQueue<BuildableType> bq = newQueue();
        bq.add(warehouseType);
        bq.setCurrentlyBuilding(null);
        assertTrue(bq.isEmpty());
    }

    // M12-TP2: ENTRY -> N0 -(B1:true)-> N2 -(B2:true)-> N5 -> N6 -> EXIT (buildable not BuildingType -> add at index 0)
    public void testM12_TP2_SetCurrentlyBuildingUnitType() {
        BuildQueue<BuildableType> bq = newQueue();
        bq.add(wagonTrainType);
        bq.setCurrentlyBuilding(wagonTrainType);
        assertEquals(2, bq.size());
        assertSame(wagonTrainType, bq.getCurrentlyBuilding());
    }

    // M12-TP3: ENTRY -> N0 -(B1:true)-> N2 -(B2:false)-> N3 -(B3:true)-> N5 -> N6 -> EXIT (BuildingType not in queue -> add at index 0)
    public void testM12_TP3_SetCurrentlyBuildingNewBuildingType() {
        BuildQueue<BuildableType> bq = newQueue();
        bq.add(carpenterHouseType);
        bq.setCurrentlyBuilding(warehouseType);
        assertEquals(2, bq.size());
        assertSame(warehouseType, bq.getCurrentlyBuilding());
    }

    // M12-TP4: ENTRY -> N0 -(B1:true)-> N2 -(B2:false)-> N3 -(B3:false)-> N4 -> N5 -> N6 -> EXIT (BuildingType already in queue -> remove then add at index 0)
    public void testM12_TP4_SetCurrentlyBuildingExistingBuildingType() {
        BuildQueue<BuildableType> bq = newQueue();
        bq.add(carpenterHouseType);
        bq.add(warehouseType);
        bq.setCurrentlyBuilding(warehouseType);
        assertEquals(2, bq.size());
        assertSame(warehouseType, bq.getCurrentlyBuilding());
        assertSame(carpenterHouseType, bq.getValues().get(1));
    }

    // ------------------------------------------------------------------
    // M13: getProductionInfo(Ljava/util/List;)Lnet/sf/freecol/common/model/ProductionInfo; — 7 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M13-TP1: ENTRY -> N0 -(B1:true)-> N13 -> EXIT (current == null -> empty ProductionInfo)
    public void testM13_TP1_GetProductionInfoNoCurrent() {
        BuildQueue<BuildableType> bq = newQueue();
        ProductionInfo info = bq.getProductionInfo(Collections.emptyList());
        assertTrue(info.getConsumption().isEmpty());
    }

    // M13-TP2: ENTRY -> N0 -(B1:false)-> N1 -> N2 -(B2:true)-> N12 -> N13 -> EXIT (current has 0 required goods -> loop skipped)
    public void testM13_TP2_GetProductionInfoNoRequiredGoods() {
        BuildQueue<BuildableType> bq = newQueue();
        BuildingType freeBuilding = new BuildingType("model.building.cfgFree", spec);
        bq.add(freeBuilding);
        ProductionInfo info = bq.getProductionInfo(Collections.emptyList());
        assertTrue(info.getConsumption().isEmpty());
    }

    // M13-TP3: ENTRY -> N0 -(B1:false)-> N1 -> N2 -(B2:false)-> N3 -(B3:true)-> N10 -> EXIT (required goods missing in input -> return empty)
    public void testM13_TP3_GetProductionInfoMissingInput() {
        BuildQueue<BuildableType> bq = newQueue();
        bq.add(warehouseType);
        ProductionInfo info = bq.getProductionInfo(Collections.emptyList());
        assertTrue(info.getConsumption().isEmpty());
    }

    // M13-TP4: ENTRY -> N0 -(B1:false)-> N1 -> N2 -(B2:false)-> N3 -(B3:false)-> N4 -(B4:true)-> N10 -> EXIT (available amount < required amount -> return empty)
    public void testM13_TP4_GetProductionInfoInsufficientInput() {
        BuildQueue<BuildableType> bq = newQueue();
        bq.add(warehouseType);
        List<AbstractGoods> input = Collections.singletonList(new AbstractGoods(hammersType, 1));
        ProductionInfo info = bq.getProductionInfo(input);
        assertTrue(info.getConsumption().isEmpty());
    }

    // M13-TP5: ENTRY -> N0 -(B1:false)-> N1 -> N2 -(B2:false)-> N3 -(B3:false)-> N4 -(B4:false)-> N5 -(B5:true)-> N7 -> N9 -> N11 -> N2 -(B2:true)-> N12 -> N13 -> EXIT (overflow == true -> consume required amount)
    public void testM13_TP5_GetProductionInfoOverflowTrue() {
        spec.getOption(GameOptions.SAVE_PRODUCTION_OVERFLOW, BooleanOption.class).setValue(true);
        BuildQueue<BuildableType> bq = newQueue();
        bq.add(warehouseType);
        int reqHammers = warehouseType.getRequiredAmountOf(hammersType);
        List<AbstractGoods> input = Collections.singletonList(
                new AbstractGoods(hammersType, reqHammers + 50));
        ProductionInfo info = bq.getProductionInfo(input);
        assertEquals(1, info.getConsumption().size());
        assertEquals(reqHammers, info.getConsumption().get(0).getAmount());
    }

    // M13-TP6: ENTRY -> N0 -(B1:false)-> N1 -> N2 -(B2:false)-> N3 -(B3:false)-> N4 -(B4:false)-> N5 -(B5:false)-> N6 -(B6:true)-> N8 -> N9 -> N11 -> N2 -(B2:true)-> N12 -> N13 -> EXIT (overflow == false, non-storable -> consume all available)
    public void testM13_TP6_GetProductionInfoNoOverflowNonStorable() {
        spec.getOption(GameOptions.SAVE_PRODUCTION_OVERFLOW, BooleanOption.class).setValue(false);
        BuildQueue<BuildableType> bq = newQueue();
        bq.add(warehouseType);
        int reqHammers = warehouseType.getRequiredAmountOf(hammersType);
        List<AbstractGoods> input = Collections.singletonList(
                new AbstractGoods(hammersType, reqHammers + 25));
        ProductionInfo info = bq.getProductionInfo(input);
        assertEquals(1, info.getConsumption().size());
        assertEquals(reqHammers + 25, info.getConsumption().get(0).getAmount());
    }

    // M13-TP7: ENTRY -> N0 -(B1:false)-> N1 -> N2 -(B2:false)-> N3 -(B3:false)-> N4 -(B4:false)-> N5 -(B5:false)-> N6 -(B6:false)-> N7 -> N9 -> N11 -> N2 -(B2:true)-> N12 -> N13 -> EXIT (overflow == false, storable -> consume required amount)
    public void testM13_TP7_GetProductionInfoNoOverflowStorable() {
        spec.getOption(GameOptions.SAVE_PRODUCTION_OVERFLOW, BooleanOption.class).setValue(false);
        UnitType artilleryType = spec.getUnitType("model.unit.artillery");
        BuildQueue<BuildableType> bq = newQueue();
        bq.add(artilleryType);
        int reqHammers = artilleryType.getRequiredAmountOf(hammersType);
        int reqTools = artilleryType.getRequiredAmountOf(toolsType);
        assertTrue(reqTools > 0);
        List<AbstractGoods> input = Arrays.asList(
                new AbstractGoods(hammersType, reqHammers + 10),
                new AbstractGoods(toolsType, reqTools + 15));
        ProductionInfo info = bq.getProductionInfo(input);
        assertEquals(2, info.getConsumption().size());
        assertEquals(reqHammers + 10, AbstractGoods.getCount(hammersType, info.getConsumption()));
        assertEquals(reqTools, AbstractGoods.getCount(toolsType, info.getConsumption()));
    }

    // ------------------------------------------------------------------
    // M14: getConsumedGoods()Ljava/util/List; — 2 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M14-TP1: ENTRY -> N0 -(B1:false)-> N1 -> N3 -> EXIT (current == null -> emptyList)
    public void testM14_TP1_GetConsumedGoodsEmpty() {
        BuildQueue<BuildableType> bq = newQueue();
        assertTrue(bq.getConsumedGoods().isEmpty());
    }

    // M14-TP2: ENTRY -> N0 -(B1:true)-> N2 -> N3 -> EXIT (current != null -> requiredGoodsList)
    public void testM14_TP2_GetConsumedGoodsNonEmpty() {
        BuildQueue<BuildableType> bq = newQueue();
        bq.add(warehouseType);
        assertEquals(warehouseType.getRequiredGoodsList(), bq.getConsumedGoods());
    }

    // ------------------------------------------------------------------
    // M15: getPriority()I — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M15-TP1: ENTRY -> N0 -> EXIT (Return priority)
    public void testM15_TP1_GetPriority() {
        BuildQueue<BuildableType> bq = newQueue();
        assertEquals(Consumer.COLONY_PRIORITY, bq.getPriority());
    }

    // ------------------------------------------------------------------
    // M16: getConsumptionModifiers(Ljava/lang/String;)Ljava/util/stream/Stream; — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M16-TP1: ENTRY -> N0 -> EXIT (Return empty Stream<Modifier>)
    public void testM16_TP1_GetConsumptionModifiers() {
        BuildQueue<BuildableType> bq = newQueue();
        assertEquals(0L, bq.getConsumptionModifiers("model.goods.hammers").count());
    }

    // ------------------------------------------------------------------
    // M17: toString()Ljava/lang/String; — 2 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M17-TP1: ENTRY -> N0 -> N1 -(B1:true)-> N3 -> EXIT (empty queue -> loop skipped)
    public void testM17_TP1_ToStringEmpty() {
        BuildQueue<BuildableType> bq = newQueue();
        String s = bq.toString();
        assertTrue(s.startsWith("[BuildQueue ("));
        assertTrue(s.endsWith(")]"));
    }

    // M17-TP2: ENTRY -> N0 -> N1 -(B1:false)-> N2 -> N1 -(B1:true)-> N3 -> EXIT (non-empty queue -> loop body executed)
    public void testM17_TP2_ToStringNonEmpty() {
        BuildQueue<BuildableType> bq = newQueue();
        bq.add(warehouseType);
        bq.add(wagonTrainType);
        String s = bq.toString();
        assertTrue(s.contains(warehouseType.getId()));
        assertTrue(s.contains(wagonTrainType.getId()));
    }
}
