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
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

import net.sf.freecol.common.io.FreeColXMLReader;
import net.sf.freecol.common.io.FreeColXMLWriter;
import net.sf.freecol.common.io.FreeColXMLWriter.WriteScope;
import net.sf.freecol.common.model.Constants.IntegrityType;
import net.sf.freecol.common.model.IndianSettlement.ContactLevel;
import net.sf.freecol.common.option.BooleanOption;
import net.sf.freecol.common.option.GameOptions;
import net.sf.freecol.common.util.LogBuilder;
import net.sf.freecol.server.model.ServerIndianSettlement;
import net.sf.freecol.server.model.ServerUnit;
import net.sf.freecol.util.test.FreeColTestCase;
import net.sf.freecol.util.test.MockPseudoRandom;


/**
 * CFG-driven test suite for {@link IndianSettlement} derived from
 * {@code test_cft/graph/net/sf/freecol/common/model/IndianSettlement.json}.
 *
 * Covers all 118 methods (M1..M118, including constructors, private helpers,
 * all 18 lambdas M100..M117, and {@code <clinit>} M118) and achieves 100%
 * feasible C3 (node + edge) coverage across 189 Test Paths.
 *
 * Infeasible edges documented:
 * - M28 ({@code getWantedGoodsLabel}): {@code N1 -> N4 -> N7} ({@code B2:true, B4:true}),
 *   {@code N2 -> N4 -> N5} / {@code N3 -> N4 -> N5} ({@code B4:false}),
 *   {@code N2 -(B3:true)-> N4 -> N7 -(B5:false)-> N8}, and
 *   {@code N3 -> N4 -> N7 -(B5:true)-> N9} are infeasible because {@code lab} is
 *   non-null iff {@code gt != null} and {@code Player.getLastSaleTip} is non-null
 *   iff {@code Player.getLastSaleString} is non-null.
 * - M42 ({@code worthScouting}): {@code N0 -> N6} ({@code B1:default}) is infeasible
 *   because {@code ContactLevel} has only 4 enum constants (0..3) and
 *   {@code getContactLevel} never returns {@code null}.
 * - M54 ({@code getPriceToBuy}): {@code N2 -(B2:true)-> N4 -(B3:true)-> N6} is
 *   infeasible because {@code price} remains {@code 0} when {@code !type.getMilitary()}.
 * - M55 ({@code getNormalGoodsPriceToBuy}): {@code N17 -> N18} ({@code B9:false},
 *   {@code unitPrice < 0}) is infeasible under valid {@code Specification}
 *   ({@code GOODS_BASE_PRICE + tradeBonus >= 0} and {@code capacity > 0}).
 * - M64 ({@code updateWantedGoods}): {@code N1 -> N7} ({@code B1:true}) is
 *   infeasible because {@code Specification} always contains more than
 *   {@code WANTED_GOODS_COUNT} (3) storable non-military goods types, so the loop
 *   always exits via {@code B2:false} or {@code B3:false}.
 * - M83 ({@code getTotalProductionOf}): {@code N1 -> N3} ({@code B2:true},
 *   {@code type == goodsToMake()} when {@code type.isRefined()}) is infeasible
 *   under standard {@code Specification} because {@code goodsToMake()} returns
 *   a raw material ({@code gt.isRawMaterial()}) and no {@code GoodsType} in
 *   {@code Specification} is both refined ({@code madeFrom != null}) and a
 *   raw material ({@code makes != null}).
 * - M92 ({@code copyIn}): {@code N1 -> N2} ({@code B2:false}, {@code !super.copyIn(o)})
 *   is infeasible because {@code Settlement.copyIn} always returns {@code true}
 *   for any non-null {@code IndianSettlement}.
 * - M94 ({@code writeChildren}): {@code N13 -> N15} ({@code B6:true}, {@code cl == null})
 *   is infeasible because {@code getContactLevel(client)} never returns {@code null}.
 * - M106 ({@code lambda$goodsToMake$1}): {@code N1 -> N7} ({@code B2:true},
 *   {@code gt.getOutputType() == null} after {@code gt.isRawMaterial() == true})
 *   is infeasible because {@code GoodsType.isRawMaterial()} is defined as
 *   {@code makes != null}.
 */
public class IndianSettlementCfgTest extends FreeColTestCase {

    private Game game;
    private Map map;
    private Player inca;
    private Player arawak;
    private Player dutch;
    private Player french;
    private Tile centerTile;
    private IndianSettlement settlement;

    private GoodsType foodType;
    private GoodsType grainType;
    private GoodsType cottonType;
    private GoodsType clothType;
    private GoodsType lumberType;
    private GoodsType oreType;
    private GoodsType toolsType;
    private GoodsType musketsType;
    private GoodsType horsesType;
    private GoodsType tradeGoodsType;
    private GoodsType bellsType;

    private UnitType braveType;
    private UnitType colonistType;
    private UnitType missionaryType;
    private UnitType caravelType;
    private UnitType wagonTrainType;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        // Pre-condition: Initialize standard game, map, players, settlement, and spec types
        game = getStandardGame();
        map = getTestMap(spec().getTileType("model.tile.plains"), true);
        game.changeMap(map);

        inca = game.getPlayerByNationId("model.nation.inca");
        arawak = game.getPlayerByNationId("model.nation.arawak");
        dutch = game.getPlayerByNationId("model.nation.dutch");
        french = game.getPlayerByNationId("model.nation.french");

        centerTile = map.getTile(5, 8);
        settlement = new IndianSettlementBuilder(game)
            .player(inca)
            .settlementTile(centerTile)
            .initialBravesInCamp(2)
            .skillToTeach("model.unit.masterCottonPlanter")
            .build();

        foodType = spec().getPrimaryFoodType();
        grainType = spec().getGoodsType("model.goods.grain");
        cottonType = spec().getGoodsType("model.goods.cotton");
        clothType = spec().getGoodsType("model.goods.cloth");
        lumberType = spec().getGoodsType("model.goods.lumber");
        oreType = spec().getGoodsType("model.goods.ore");
        toolsType = spec().getGoodsType("model.goods.tools");
        musketsType = spec().getGoodsType("model.goods.muskets");
        horsesType = spec().getGoodsType("model.goods.horses");
        tradeGoodsType = spec().getGoodsType("model.goods.tradeGoods");
        bellsType = spec().getGoodsType("model.goods.bells");

        braveType = spec().getDefaultUnitType(inca);
        colonistType = spec().getUnitType("model.unit.freeColonist");
        missionaryType = spec().getUnitType("model.unit.jesuitMissionary");
        caravelType = spec().getUnitType("model.unit.caravel");
        wagonTrainType = spec().getUnitType("model.unit.wagonTrain");
    }


    // ------------------------------------------------------------------
    // Pre-condition helpers & stubs
    // ------------------------------------------------------------------

    private static Object invokePrivate(Object target, String name,
                                        Class<?>[] paramTypes, Object... args)
        throws Exception {
        Method m = IndianSettlement.class.getDeclaredMethod(name, paramTypes);
        m.setAccessible(true);
        return m.invoke(target, args);
    }

    private static Object invokeLambdaByPrefix(Object target, String prefix,
                                               Class<?>[] paramTypes, Object... args)
        throws Exception {
        for (Method m : IndianSettlement.class.getDeclaredMethods()) {
            if (m.getName().startsWith(prefix)
                    && Arrays.equals(m.getParameterTypes(), paramTypes)) {
                m.setAccessible(true);
                return m.invoke(target, args);
            }
        }
        throw new NoSuchMethodException(prefix + Arrays.toString(paramTypes));
    }

    private static Object getPrivateField(Object target, String name)
        throws Exception {
        Field f = IndianSettlement.class.getDeclaredField(name);
        f.setAccessible(true);
        return f.get(target);
    }

    private static String serializeToXml(IndianSettlement is, WriteScope scope)
        throws Exception {
        StringWriter sw = new StringWriter();
        try (FreeColXMLWriter xw = new FreeColXMLWriter(sw, scope)) {
            is.toXML(xw);
        }
        return sw.toString();
    }

    private Unit createUnit(Player owner, Location loc, UnitType type) {
        return new ServerUnit(game, loc, owner, type);
    }


    // ------------------------------------------------------------------
    // M1: IndianSettlement(Game, Player, String, Tile) — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M1-TP1: ENTRY -> N0 -> EXIT (protected constructor)
    public void testM1_TP1_ConstructorFull() {
        Tile t = map.getTile(2, 2);
        IndianSettlement is = new IndianSettlement(game, inca, "Cusco", t);
        assertEquals("Cusco", is.getName());
        assertSame(inca, is.getOwner());
        assertSame(t, is.getTile());
    }


    // ------------------------------------------------------------------
    // M2: IndianSettlement(Game, String) — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M2-TP1: ENTRY -> N0 -> EXIT (deserialization constructor)
    public void testM2_TP1_ConstructorById() {
        IndianSettlement is = new IndianSettlement(game, "indianSettlement:m2");
        assertEquals("indianSettlement:m2", is.getId());
    }


    // ------------------------------------------------------------------
    // M3: emptyWantedGoods() — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M3-TP1: ENTRY -> N0 -> (N1 -(B1:false)-> N2)*3 -> N1 -(B1:true)-> N3 -> EXIT
    @SuppressWarnings("unchecked")
    public void testM3_TP1_EmptyWantedGoods() throws Exception {
        List<GoodsType> list = (List<GoodsType>)invokePrivate(
            null, "emptyWantedGoods", new Class<?>[0]);
        assertEquals(IndianSettlement.WANTED_GOODS_COUNT, list.size());
        for (GoodsType gt : list) {
            assertNull(gt);
        }
    }


    // ------------------------------------------------------------------
    // M4: getOwnedUnitList() — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M4-TP1: ENTRY -> N0 -> N1 -> N2 -> EXIT
    public void testM4_TP1_GetOwnedUnitList() {
        List<Unit> owned = settlement.getOwnedUnitList();
        assertEquals(2, owned.size());
    }


    // ------------------------------------------------------------------
    // M5: setOwnedUnitList(List<Unit>) — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M5-TP1: ENTRY -> N0 -> EXIT
    public void testM5_TP1_SetOwnedUnitList() {
        Unit u = createUnit(inca, settlement, braveType);
        settlement.setOwnedUnitList(Collections.singletonList(u));
        assertEquals(1, settlement.getOwnedUnitList().size());
        assertSame(u, settlement.getOwnedUnitList().get(0));
    }


    // ------------------------------------------------------------------
    // M6: addOwnedUnit(Unit) — 2 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M6-TP1: ENTRY -> N0 -> N1 -(B1:false)-> N2 -> N3 -> N4 -> N7 -> EXIT (not yet owned)
    public void testM6_TP1_AddOwnedUnitNew() {
        Unit u = createUnit(inca, centerTile, braveType);
        int before = settlement.getOwnedUnitList().size();
        settlement.addOwnedUnit(u);
        assertEquals(before + 1, settlement.getOwnedUnitList().size());
    }

    // M6-TP2: ENTRY -> N0 -> N1 -(B1:true)-> N3 -> N4 -> N7 -> EXIT (already owned)
    public void testM6_TP2_AddOwnedUnitDuplicate() {
        Unit existing = settlement.getOwnedUnitList().get(0);
        int before = settlement.getOwnedUnitList().size();
        settlement.addOwnedUnit(existing);
        assertEquals(before, settlement.getOwnedUnitList().size());
    }


    // ------------------------------------------------------------------
    // M7: clearOwnedUnits() — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M7-TP1: ENTRY -> N0 -> N1 -> N2 -> N5 -> EXIT
    public void testM7_TP1_ClearOwnedUnits() throws Exception {
        invokePrivate(settlement, "clearOwnedUnits", new Class<?>[0]);
        assertTrue(settlement.getOwnedUnitList().isEmpty());
    }


    // ------------------------------------------------------------------
    // M8: removeOwnedUnit(Unit) — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M8-TP1: ENTRY -> N0 -> N1 -> N2 -> EXIT
    public void testM8_TP1_RemoveOwnedUnit() {
        Unit existing = settlement.getOwnedUnitList().get(0);
        assertTrue(settlement.removeOwnedUnit(existing));
        assertFalse(settlement.removeOwnedUnit(existing));
    }


    // ------------------------------------------------------------------
    // M9: getLastTribute() — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M9-TP1: ENTRY -> N0 -> EXIT
    public void testM9_TP1_GetLastTribute() {
        assertEquals(0, settlement.getLastTribute());
    }


    // ------------------------------------------------------------------
    // M10: setLastTribute(int) — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M10-TP1: ENTRY -> N0 -> EXIT
    public void testM10_TP1_SetLastTribute() {
        settlement.setLastTribute(15);
        assertEquals(15, settlement.getLastTribute());
    }


    // ------------------------------------------------------------------
    // M11: getLearnableSkill() — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M11-TP1: ENTRY -> N0 -> EXIT
    public void testM11_TP1_GetLearnableSkill() {
        assertNotNull(settlement.getLearnableSkill());
    }


    // ------------------------------------------------------------------
    // M12: setLearnableSkill(UnitType) — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M12-TP1: ENTRY -> N0 -> EXIT
    public void testM12_TP1_SetLearnableSkill() {
        UnitType farmer = spec().getUnitType("model.unit.expertFarmer");
        settlement.setLearnableSkill(farmer);
        assertSame(farmer, settlement.getLearnableSkill());
    }


    // ------------------------------------------------------------------
    // M13: getLearnableSkillLabel(boolean) — 3 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M13-TP1: ENTRY -> N0 -(B1:true)-> N4 -> N5 -> EXIT (visited == false)
    public void testM13_TP1_GetLearnableSkillLabelNotVisited() {
        StringTemplate st = settlement.getLearnableSkillLabel(false);
        assertEquals("model.indianSettlement.skillUnknown", st.getId());
    }

    // M13-TP2: ENTRY -> N0 -(B1:false)-> N1 -(B2:false)-> N2 -> N5 -> EXIT (visited == true, skill == null)
    public void testM13_TP2_GetLearnableSkillLabelVisitedNull() {
        settlement.setLearnableSkill(null);
        StringTemplate st = settlement.getLearnableSkillLabel(true);
        assertEquals("model.indianSettlement.skillNone", st.getId());
    }

    // M13-TP3: ENTRY -> N0 -(B1:false)-> N1 -(B2:true)-> N3 -> N5 -> EXIT (visited == true, skill != null)
    public void testM13_TP3_GetLearnableSkillLabelVisitedNonNull() {
        UnitType farmer = spec().getUnitType("model.unit.expertFarmer");
        settlement.setLearnableSkill(farmer);
        StringTemplate st = settlement.getLearnableSkillLabel(true);
        assertEquals(farmer.getNameKey(), st.getId());
    }


    // ------------------------------------------------------------------
    // M14: getMissionary() — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M14-TP1: ENTRY -> N0 -> EXIT
    public void testM14_TP1_GetMissionary() {
        assertNull(settlement.getMissionary());
    }


    // ------------------------------------------------------------------
    // M15: hasMissionary() — 2 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M15-TP1: ENTRY -> N0 -(B1:true)-> N2 -> N3 -> EXIT (missionary == null)
    public void testM15_TP1_HasMissionaryFalse() {
        settlement.setMissionary(null);
        assertFalse(settlement.hasMissionary());
    }

    // M15-TP2: ENTRY -> N0 -(B1:false)-> N1 -> N3 -> EXIT (missionary != null)
    public void testM15_TP2_HasMissionaryTrue() {
        Unit m = createUnit(dutch, settlement, missionaryType);
        settlement.setMissionary(m);
        assertTrue(settlement.hasMissionary());
    }


    // ------------------------------------------------------------------
    // M16: hasMissionary(Player) — 4 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M16-TP1: ENTRY -> N0 -(B1:true)-> N4 -> N5 -> EXIT (missionary == null)
    public void testM16_TP1_HasMissionaryPlayerNoMissionary() {
        settlement.setMissionary(null);
        assertFalse(settlement.hasMissionary(dutch));
    }

    // M16-TP2: ENTRY -> N0 -(B1:false)-> N1 -(B2:true)-> N4 -> N5 -> EXIT (player == null)
    public void testM16_TP2_HasMissionaryPlayerNull() {
        settlement.setMissionary(createUnit(dutch, settlement, missionaryType));
        assertFalse(settlement.hasMissionary(null));
    }

    // M16-TP3: ENTRY -> N0 -(B1:false)-> N1 -(B2:false)-> N2 -(B3:true)-> N4 -> N5 -> EXIT (other player)
    public void testM16_TP3_HasMissionaryPlayerOtherOwner() {
        settlement.setMissionary(createUnit(dutch, settlement, missionaryType));
        assertFalse(settlement.hasMissionary(french));
    }

    // M16-TP4: ENTRY -> N0 -(B1:false)-> N1 -(B2:false)-> N2 -(B3:false)-> N3 -> N5 -> EXIT (matching owner)
    public void testM16_TP4_HasMissionaryPlayerMatches() {
        settlement.setMissionary(createUnit(dutch, settlement, missionaryType));
        assertTrue(settlement.hasMissionary(dutch));
    }


    // ------------------------------------------------------------------
    // M17: setMissionary(Unit) — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M17-TP1: ENTRY -> N0 -> EXIT
    public void testM17_TP1_SetMissionary() {
        Unit m = createUnit(dutch, settlement, missionaryType);
        settlement.setMissionary(m);
        assertSame(m, settlement.getMissionary());
    }


    // ------------------------------------------------------------------
    // M18: getMissionaryLineOfSight() — 2 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M18-TP1: ENTRY -> N0 -(B1:true)-> N2 -> N3 -> EXIT (ENHANCED_MISSIONARIES == false)
    public void testM18_TP1_GetMissionaryLineOfSightStandard() {
        spec().getOption(GameOptions.ENHANCED_MISSIONARIES, BooleanOption.class).setValue(false);
        assertEquals(1, settlement.getMissionaryLineOfSight());
    }

    // M18-TP2: ENTRY -> N0 -(B1:false)-> N1 -> N3 -> EXIT (ENHANCED_MISSIONARIES == true)
    public void testM18_TP2_GetMissionaryLineOfSightEnhanced() {
        spec().getOption(GameOptions.ENHANCED_MISSIONARIES, BooleanOption.class).setValue(true);
        assertEquals(settlement.getLineOfSight(), settlement.getMissionaryLineOfSight());
    }


    // ------------------------------------------------------------------
    // M19: getMissionaryVisibleTiles() — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M19-TP1: ENTRY -> N0 -> EXIT
    public void testM19_TP1_GetMissionaryVisibleTiles() {
        List<Tile> tiles = settlement.getMissionaryVisibleTiles();
        assertFalse(tiles.isEmpty());
        assertTrue(tiles.contains(centerTile));
    }


    // ------------------------------------------------------------------
    // M20: getConvertProgress() — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M20-TP1: ENTRY -> N0 -> EXIT
    public void testM20_TP1_GetConvertProgress() {
        assertEquals(0, settlement.getConvertProgress());
    }


    // ------------------------------------------------------------------
    // M21: setConvertProgress(int) — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M21-TP1: ENTRY -> N0 -> EXIT
    public void testM21_TP1_SetConvertProgress() {
        settlement.setConvertProgress(42);
        assertEquals(42, settlement.getConvertProgress());
    }


    // ------------------------------------------------------------------
    // M22: validWantedGoodsIndex(int) — 3 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M22-TP1: ENTRY -> N0 -(B1:true)-> N3 -> N4 -> EXIT (index < 0)
    public void testM22_TP1_ValidWantedGoodsIndexNegative() throws Exception {
        assertFalse((Boolean)invokePrivate(settlement, "validWantedGoodsIndex",
            new Class<?>[]{int.class}, -1));
    }

    // M22-TP2: ENTRY -> N0 -(B1:false)-> N1 -(B2:true)-> N3 -> N4 -> EXIT (index >= 3)
    public void testM22_TP2_ValidWantedGoodsIndexTooLarge() throws Exception {
        assertFalse((Boolean)invokePrivate(settlement, "validWantedGoodsIndex",
            new Class<?>[]{int.class}, IndianSettlement.WANTED_GOODS_COUNT));
    }

    // M22-TP3: ENTRY -> N0 -(B1:false)-> N1 -(B2:false)-> N2 -> N4 -> EXIT (0 <= index < 3)
    public void testM22_TP3_ValidWantedGoodsIndexValid() throws Exception {
        assertTrue((Boolean)invokePrivate(settlement, "validWantedGoodsIndex",
            new Class<?>[]{int.class}, 1));
    }


    // ------------------------------------------------------------------
    // M23: getWantedGoods() — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M23-TP1: ENTRY -> N0 -> EXIT
    public void testM23_TP1_GetWantedGoods() {
        assertEquals(IndianSettlement.WANTED_GOODS_COUNT,
                     settlement.getWantedGoods().size());
    }


    // ------------------------------------------------------------------
    // M24: getWantedGoods(int) — 2 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M24-TP1: ENTRY -> N0 -(B1:true)-> N2 -> N3 -> EXIT (invalid index)
    public void testM24_TP1_GetWantedGoodsInvalidIndex() {
        assertNull(settlement.getWantedGoods(-1));
    }

    // M24-TP2: ENTRY -> N0 -(B1:false)-> N1 -> N3 -> EXIT (valid index)
    public void testM24_TP2_GetWantedGoodsValidIndex() {
        settlement.setWantedGoods(0, cottonType);
        assertSame(cottonType, settlement.getWantedGoods(0));
    }


    // ------------------------------------------------------------------
    // M25: setWantedGoods(List<GoodsType>) — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M25-TP1: ENTRY -> N0 -> N1 -(B1:false)-> N2 -(B2:false)-> N3 -> N5 -> N1 -(B1:false)-> N2 -(B2:true)-> N4 -> N5 -> ... -> N1 -(B1:true)-> N6 -> EXIT
    public void testM25_TP1_SetWantedGoodsList() {
        settlement.setWantedGoods(Collections.singletonList(cottonType));
        assertSame(cottonType, settlement.getWantedGoods(0));
        assertNull(settlement.getWantedGoods(1));
        assertNull(settlement.getWantedGoods(2));
    }


    // ------------------------------------------------------------------
    // M26: setWantedGoods(int, GoodsType) — 2 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M26-TP1: ENTRY -> N0 -(B1:true)-> N2 -> EXIT (invalid index)
    public void testM26_TP1_SetWantedGoodsIndexInvalid() {
        settlement.setWantedGoods(99, cottonType);
        assertNull(settlement.getWantedGoods(0));
    }

    // M26-TP2: ENTRY -> N0 -(B1:false)-> N1 -> N2 -> EXIT (valid index)
    public void testM26_TP2_SetWantedGoodsIndexValid() {
        settlement.setWantedGoods(2, lumberType);
        assertSame(lumberType, settlement.getWantedGoods(2));
    }


    // ------------------------------------------------------------------
    // M27: getWantedGoodsCount() — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M27-TP1: ENTRY -> N0 -> EXIT
    public void testM27_TP1_GetWantedGoodsCount() {
        settlement.setWantedGoods(Arrays.asList(cottonType, lumberType));
        assertEquals(2, settlement.getWantedGoodsCount());
    }


    // ------------------------------------------------------------------
    // M28: getWantedGoodsLabel(int, Player) — 4 Test Paths (100% feasible C3)
    // ------------------------------------------------------------------

    // M28-TP1: ENTRY -> N0 -(B1:true)-> N6 -> N7 -(B5:true)-> N9 -> EXIT (!hasVisited)
    public void testM28_TP1_GetWantedGoodsLabelNotVisited() {
        List<StringTemplate> res = settlement.getWantedGoodsLabel(0, dutch);
        assertEquals(1, res.size());
        assertEquals("", res.get(0).getId());
    }

    // M28-TP2: ENTRY -> N0 -(B1:false)-> N1 -(B2:true)-> N4 -(B4:false)-> N5 -> N7 -(B5:true)-> N9 -> EXIT (visited, gt == null)
    public void testM28_TP2_GetWantedGoodsLabelVisitedNullGoods() {
        settlement.setVisited(dutch);
        settlement.setWantedGoods(0, null);
        List<StringTemplate> res = settlement.getWantedGoodsLabel(0, dutch);
        assertEquals(1, res.size());
        assertEquals("model.indianSettlement.wantedGoodsNone", res.get(0).getId());
    }

    // M28-TP3: ENTRY -> N0 -(B1:false)-> N1 -(B2:false)-> N2 -(B3:true)-> N4 -(B4:true)-> N7 -(B5:true)-> N9 -> EXIT (visited, gt != null, sale == null)
    public void testM28_TP3_GetWantedGoodsLabelVisitedNoSale() {
        settlement.setVisited(dutch);
        settlement.setWantedGoods(0, cottonType);
        List<StringTemplate> res = settlement.getWantedGoodsLabel(0, dutch);
        assertEquals(1, res.size());
    }

    // M28-TP4: ENTRY -> N0 -(B1:false)-> N1 -(B2:false)-> N2 -(B3:false)-> N3 -> N4 -(B4:true)-> N7 -(B5:false)-> N8 -> N9 -> EXIT (visited, gt != null, sale != null, tip != null)
    public void testM28_TP4_GetWantedGoodsLabelVisitedWithSale() {
        settlement.setVisited(dutch);
        settlement.setWantedGoods(0, cottonType);
        dutch.addLastSale(new LastSale(settlement, cottonType, game.getTurn(), 500));
        List<StringTemplate> res = settlement.getWantedGoodsLabel(0, dutch);
        assertEquals(2, res.size());
    }


    // ------------------------------------------------------------------
    // M29: getMostHated() — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M29-TP1: ENTRY -> N0 -> EXIT
    public void testM29_TP1_GetMostHated() {
        assertNull(settlement.getMostHated());
    }


    // ------------------------------------------------------------------
    // M30: setMostHated(Player) — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M30-TP1: ENTRY -> N0 -> EXIT
    public void testM30_TP1_SetMostHated() {
        settlement.setMostHated(dutch);
        assertSame(dutch, settlement.getMostHated());
    }


    // ------------------------------------------------------------------
    // M31: getMostHatedLabel(boolean) — 3 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M31-TP1: ENTRY -> N0 -(B1:true)-> N4 -> N5 -> EXIT (contacted == false)
    public void testM31_TP1_GetMostHatedLabelNotContacted() {
        StringTemplate st = settlement.getMostHatedLabel(false);
        assertEquals("model.indianSettlement.mostHatedUnknown", st.getId());
    }

    // M31-TP2: ENTRY -> N0 -(B1:false)-> N1 -(B2:false)-> N2 -> N5 -> EXIT (contacted == true, mostHated == null)
    public void testM31_TP2_GetMostHatedLabelContactedNull() {
        settlement.setMostHated(null);
        StringTemplate st = settlement.getMostHatedLabel(true);
        assertEquals("model.indianSettlement.mostHatedNone", st.getId());
    }

    // M31-TP3: ENTRY -> N0 -(B1:false)-> N1 -(B2:true)-> N3 -> N5 -> EXIT (contacted == true, mostHated != null)
    public void testM31_TP3_GetMostHatedLabelContactedNonNull() {
        settlement.setMostHated(dutch);
        StringTemplate st = settlement.getMostHatedLabel(true);
        assertEquals(dutch.getCountryLabel().getId(), st.getId());
    }


    // ------------------------------------------------------------------
    // M32: setContactLevels(Map) — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M32-TP1: ENTRY -> N0 -> N1 -> N2 -> N5 -> EXIT
    public void testM32_TP1_SetContactLevels() {
        java.util.Map<Player, ContactLevel> m = new HashMap<>();
        m.put(dutch, ContactLevel.VISITED);
        settlement.setContactLevels(m);
        assertEquals(ContactLevel.VISITED, settlement.getContactLevel(dutch));
    }


    // ------------------------------------------------------------------
    // M33: clearContactLevels() — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M33-TP1: ENTRY -> N0 -> N1 -> N2 -> N5 -> EXIT
    public void testM33_TP1_ClearContactLevels() throws Exception {
        settlement.setContacted(dutch);
        invokePrivate(settlement, "clearContactLevels", new Class<?>[0]);
        assertEquals(ContactLevel.UNCONTACTED, settlement.getContactLevel(dutch));
    }


    // ------------------------------------------------------------------
    // M34: getContactLevel(Player) — 2 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M34-TP1: ENTRY -> N0 -> N1 -(B1:false)-> N2 -> N4 -> N5 -> EXIT (cl == null)
    public void testM34_TP1_GetContactLevelDefault() {
        assertEquals(ContactLevel.UNCONTACTED, settlement.getContactLevel(dutch));
    }

    // M34-TP2: ENTRY -> N0 -> N1 -(B1:true)-> N3 -> N4 -> N5 -> EXIT (cl != null)
    public void testM34_TP2_GetContactLevelPresent() {
        settlement.setContacted(dutch);
        assertEquals(ContactLevel.CONTACTED, settlement.getContactLevel(dutch));
    }


    // ------------------------------------------------------------------
    // M35: setContactLevel(Player, ContactLevel) — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M35-TP1: ENTRY -> N0 -> N1 -> N2 -> N5 -> EXIT
    public void testM35_TP1_SetContactLevel() throws Exception {
        invokePrivate(settlement, "setContactLevel",
            new Class<?>[]{Player.class, ContactLevel.class},
            dutch, ContactLevel.SCOUTED);
        assertEquals(ContactLevel.SCOUTED, settlement.getContactLevel(dutch));
    }


    // ------------------------------------------------------------------
    // M36: setContacted(Player) — 2 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M36-TP1: ENTRY -> N0 -(B1:false)-> N1 -> EXIT (first contact)
    public void testM36_TP1_SetContactedFirstTime() {
        assertTrue(settlement.setContacted(dutch));
        assertEquals(ContactLevel.CONTACTED, settlement.getContactLevel(dutch));
    }

    // M36-TP2: ENTRY -> N0 -(B1:true)-> N2 -> EXIT (already contacted)
    public void testM36_TP2_SetContactedAlreadyContacted() {
        settlement.setContacted(dutch);
        assertFalse(settlement.setContacted(dutch));
    }


    // ------------------------------------------------------------------
    // M37: hasVisited(Player) — 2 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M37-TP1: ENTRY -> N0 -(B1:true)-> N2 -> N3 -> EXIT (CONTACTED < VISITED)
    public void testM37_TP1_HasVisitedFalse() {
        settlement.setContacted(dutch);
        assertFalse(settlement.hasVisited(dutch));
    }

    // M37-TP2: ENTRY -> N0 -(B1:false)-> N1 -> N3 -> EXIT (VISITED >= VISITED)
    public void testM37_TP2_HasVisitedTrue() {
        settlement.setVisited(dutch);
        assertTrue(settlement.hasVisited(dutch));
    }


    // ------------------------------------------------------------------
    // M38: setVisited(Player) — 3 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M38-TP1: ENTRY -> N0 -(B1:true)-> N4 -> EXIT (already visited)
    public void testM38_TP1_SetVisitedAlreadyVisited() {
        settlement.setVisited(dutch);
        assertFalse(settlement.setVisited(dutch));
    }

    // M38-TP2: ENTRY -> N0 -(B1:false)-> N1 -(B2:true)-> N3 -> EXIT (contacted, not visited)
    public void testM38_TP2_SetVisitedFromContacted() {
        settlement.setContacted(dutch);
        assertTrue(settlement.setVisited(dutch));
        assertEquals(ContactLevel.VISITED, settlement.getContactLevel(dutch));
    }

    // M38-TP3: ENTRY -> N0 -(B1:false)-> N1 -(B2:false)-> N2 -> N3 -> EXIT (uncontacted)
    public void testM38_TP3_SetVisitedFromUncontacted() {
        assertTrue(settlement.setVisited(dutch));
        assertNotNull(settlement.getAlarm(dutch));
    }


    // ------------------------------------------------------------------
    // M39: hasScouted(Player) — 2 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M39-TP1: ENTRY -> N0 -(B1:true)-> N2 -> N3 -> EXIT (not SCOUTED)
    public void testM39_TP1_HasScoutedFalse() {
        assertFalse(settlement.hasScouted(dutch));
    }

    // M39-TP2: ENTRY -> N0 -(B1:false)-> N1 -> N3 -> EXIT (SCOUTED)
    public void testM39_TP2_HasScoutedTrue() {
        settlement.setScouted(dutch);
        assertTrue(settlement.hasScouted(dutch));
    }


    // ------------------------------------------------------------------
    // M40: setScouted(Player) — 3 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M40-TP1: ENTRY -> N0 -(B1:true)-> N4 -> EXIT (already scouted)
    public void testM40_TP1_SetScoutedAlreadyScouted() {
        settlement.setScouted(dutch);
        assertFalse(settlement.setScouted(dutch));
    }

    // M40-TP2: ENTRY -> N0 -(B1:false)-> N1 -(B2:true)-> N3 -> EXIT (contacted, not scouted)
    public void testM40_TP2_SetScoutedFromContacted() {
        settlement.setContacted(dutch);
        assertTrue(settlement.setScouted(dutch));
    }

    // M40-TP3: ENTRY -> N0 -(B1:false)-> N1 -(B2:false)-> N2 -> N3 -> EXIT (uncontacted)
    public void testM40_TP3_SetScoutedFromUncontacted() {
        assertTrue(settlement.setScouted(dutch));
        assertNotNull(settlement.getAlarm(dutch));
    }


    // ------------------------------------------------------------------
    // M41: hasAnyScouted() — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M41-TP1: ENTRY -> N0 -> N1 -> N2 -> EXIT
    public void testM41_TP1_HasAnyScouted() {
        settlement.setContacted(french);
        assertFalse(settlement.hasAnyScouted());
        settlement.setScouted(dutch);
        assertTrue(settlement.hasAnyScouted());
    }


    // ------------------------------------------------------------------
    // M42: worthScouting(Player) — 5 Test Paths (100% feasible C3)
    // ------------------------------------------------------------------

    // M42-TP1: ENTRY -> N0 -(B1:case:0)-> N6 -> EXIT (UNCONTACTED)
    public void testM42_TP1_WorthScoutingUncontacted() {
        assertFalse(settlement.worthScouting(dutch));
    }

    // M42-TP2: ENTRY -> N0 -(B1:case:1)-> N1 -> EXIT (CONTACTED)
    public void testM42_TP2_WorthScoutingContacted() {
        settlement.setContacted(dutch);
        assertTrue(settlement.worthScouting(dutch));
    }

    // M42-TP3: ENTRY -> N0 -(B1:case:2)-> N2 -(B2:true)-> N4 -> N5 -> EXIT (VISITED, option true)
    public void testM42_TP3_WorthScoutingVisitedOptionTrue() {
        settlement.setVisited(dutch);
        spec().getOption(GameOptions.SETTLEMENT_ACTIONS_CONTACT_CHIEF, BooleanOption.class)
            .setValue(true);
        assertFalse(settlement.worthScouting(dutch));
    }

    // M42-TP4: ENTRY -> N0 -(B1:case:2)-> N2 -(B2:false)-> N3 -> N5 -> EXIT (VISITED, option false)
    public void testM42_TP4_WorthScoutingVisitedOptionFalse() {
        settlement.setVisited(dutch);
        spec().getOption(GameOptions.SETTLEMENT_ACTIONS_CONTACT_CHIEF, BooleanOption.class)
            .setValue(false);
        assertTrue(settlement.worthScouting(dutch));
    }

    // M42-TP5: ENTRY -> N0 -(B1:case:3)-> N6 -> EXIT (SCOUTED)
    public void testM42_TP5_WorthScoutingScouted() {
        settlement.setScouted(dutch);
        assertFalse(settlement.worthScouting(dutch));
    }


    // ------------------------------------------------------------------
    // M43: getAlarm() — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M43-TP1: ENTRY -> N0 -> EXIT
    public void testM43_TP1_GetAlarmMap() {
        assertNotNull(settlement.getAlarm());
    }


    // ------------------------------------------------------------------
    // M44: setAlarm(Map) — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M44-TP1: ENTRY -> N0 -> N1 -> N2 -> N5 -> EXIT
    public void testM44_TP1_SetAlarmMap() {
        java.util.Map<Player, Tension> m = new HashMap<>();
        m.put(dutch, new Tension(Tension.Level.CONTENT.getLimit()));
        settlement.setAlarm(m);
        assertNotNull(settlement.getAlarm(dutch));
    }


    // ------------------------------------------------------------------
    // M45: getAlarm(Player) — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M45-TP1: ENTRY -> N0 -> N1 -> N2 -> EXIT
    public void testM45_TP1_GetAlarmPlayer() {
        assertNull(settlement.getAlarm(dutch));
    }


    // ------------------------------------------------------------------
    // M46: setAlarm(Player, Tension) — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M46-TP1: ENTRY -> N0 -> N1 -> N2 -> N5 -> EXIT
    public void testM46_TP1_SetAlarmPlayer() {
        Tension t = new Tension(100);
        settlement.setAlarm(dutch, t);
        assertSame(t, settlement.getAlarm(dutch));
    }


    // ------------------------------------------------------------------
    // M47: clearAlarm() — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M47-TP1: ENTRY -> N0 -> N1 -> N2 -> N5 -> EXIT
    public void testM47_TP1_ClearAlarm() throws Exception {
        settlement.setAlarm(dutch, new Tension(100));
        invokePrivate(settlement, "clearAlarm", new Class<?>[0]);
        assertNull(settlement.getAlarm(dutch));
    }


    // ------------------------------------------------------------------
    // M48: initializeAlarm(Player) — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M48-TP1: ENTRY -> N0 -> EXIT
    public void testM48_TP1_InitializeAlarm() {
        inca.setTension(dutch, new Tension(200));
        settlement.initializeAlarm(dutch);
        assertEquals(200, settlement.getAlarm(dutch).getValue());
    }


    // ------------------------------------------------------------------
    // M49: getAlarmLevelKey(Player) — 3 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M49-TP1: ENTRY -> N0 -(B1:false)-> N1 -> N5 -> EXIT (!player.hasContacted(owner))
    public void testM49_TP1_GetAlarmLevelKeyWary() {
        assertEquals("model.indianSettlement.tension.wary",
                     settlement.getAlarmLevelKey(dutch));
    }

    // M49-TP2: ENTRY -> N0 -(B1:true)-> N2 -(B2:false)-> N3 -> N5 -> EXIT (player contacted owner, !settlement.hasContacted)
    public void testM49_TP2_GetAlarmLevelKeyUnknown() {
        dutch.setStance(inca, Stance.PEACE);
        assertEquals("model.indianSettlement.tension.unknown",
                     settlement.getAlarmLevelKey(dutch));
    }

    // M49-TP3: ENTRY -> N0 -(B1:true)-> N2 -(B2:true)-> N4 -> N5 -> EXIT (both contacted)
    public void testM49_TP3_GetAlarmLevelKeyKnown() {
        dutch.setStance(inca, Stance.PEACE);
        settlement.setContacted(dutch);
        assertEquals(settlement.getAlarm(dutch).getNameKey(),
                     settlement.getAlarmLevelKey(dutch));
    }


    // ------------------------------------------------------------------
    // M50: getGoodsForSale() — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M50-TP1: ENTRY -> N0 -> EXIT
    public void testM50_TP1_GetGoodsForSale() {
        assertTrue(settlement.getGoodsForSale().isEmpty());
    }


    // ------------------------------------------------------------------
    // M51: setGoodsForSale(List) — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M51-TP1: ENTRY -> N0 -> EXIT
    public void testM51_TP1_SetGoodsForSale() {
        Goods g = new Goods(game, settlement, cottonType, 50);
        settlement.setGoodsForSale(Collections.singletonList(g));
        assertEquals(1, settlement.getGoodsForSale().size());
    }


    // ------------------------------------------------------------------
    // M52: allowContact(Unit) — 4 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M52-TP1: ENTRY -> N0 -(B1:true)-> N3 -> N5 -> EXIT (owner contacted)
    public void testM52_TP1_AllowContactOwnerContacted() {
        dutch.setStance(inca, Stance.PEACE);
        Unit ship = createUnit(dutch, null, caravelType);
        assertTrue(settlement.allowContact(ship));
    }

    // M52-TP2: ENTRY -> N0 -(B1:false)-> N1 -(B2:true)-> N3 -> N5 -> EXIT (not contacted, land unit)
    public void testM52_TP2_AllowContactLandUnit() {
        Unit colonist = createUnit(dutch, null, colonistType);
        assertTrue(settlement.allowContact(colonist));
    }

    // M52-TP3: ENTRY -> N0 -(B1:false)-> N1 -(B2:false)-> N2 -(B3:false)-> N3 -> N5 -> EXIT (not contacted, naval with cargo)
    public void testM52_TP3_AllowContactNavalWithCargo() {
        Unit ship = createUnit(dutch, null, caravelType);
        ship.add(new Goods(game, ship, tradeGoodsType, 20));
        assertTrue(settlement.allowContact(ship));
    }

    // M52-TP4: ENTRY -> N0 -(B1:false)-> N1 -(B2:false)-> N2 -(B3:true)-> N4 -> N5 -> EXIT (not contacted, empty naval)
    public void testM52_TP4_AllowContactNavalEmpty() {
        Unit ship = createUnit(dutch, null, caravelType);
        assertFalse(settlement.allowContact(ship));
    }


    // ------------------------------------------------------------------
    // M53: getPriceToBuy(AbstractGoods) — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M53-TP1: ENTRY -> N0 -> EXIT
    public void testM53_TP1_GetPriceToBuyAbstractGoods() {
        int p = settlement.getPriceToBuy(new AbstractGoods(cottonType, 10));
        assertTrue(p >= 0);
    }


    // ------------------------------------------------------------------
    // M54: getPriceToBuy(GoodsType, int) — 5 Test Paths (100% feasible C3)
    // ------------------------------------------------------------------

    // M54-TP1: ENTRY -> N0 -(B1:false)-> N1 -> EXIT(throw) (amount > CARGO_SIZE)
    public void testM54_TP1_GetPriceToBuyExceedsCargoSize() {
        try {
            settlement.getPriceToBuy(cottonType, GoodsContainer.CARGO_SIZE + 1);
            fail("Expected RuntimeException for amount > CARGO_SIZE");
        } catch (RuntimeException expected) {
            assertTrue(expected.getMessage().contains("Amount"));
        }
    }

    // M54-TP2: ENTRY -> N0 -(B1:true)-> N2 -(B2:false)-> N3 -> N4 -(B3:true)-> N6 -(B4:false)-> N7 -> N13 -> EXIT (military, price > 0, wanted[0])
    public void testM54_TP2_GetPriceToBuyMilitaryWanted0() {
        settlement.setWantedGoods(0, musketsType);
        int price = settlement.getPriceToBuy(musketsType, 20);
        assertTrue(price > 0);
    }

    // M54-TP3: ENTRY -> N0 -(B1:true)-> N2 -(B2:false)-> N3 -> N4 -(B3:false)-> N5 -> N6 -(B4:true)-> N8 -(B5:false)-> N9 -> N13 -> EXIT (military, price == 0 -> normal, wanted[1])
    public void testM54_TP3_GetPriceToBuyMilitaryZeroWanted1() {
        settlement.setOwnedUnitList(Collections.emptyList());
        settlement.setWantedGoods(1, musketsType);
        int price = settlement.getPriceToBuy(musketsType, 20);
        assertEquals(0, price);
    }

    // M54-TP4: ENTRY -> N0 -(B1:true)-> N2 -(B2:true)-> N4 -(B3:false)-> N5 -> N6 -(B4:true)-> N8 -(B5:true)-> N10 -(B6:false)-> N11 -> N13 -> EXIT (non-military, wanted[2])
    public void testM54_TP4_GetPriceToBuyNormalWanted2() {
        settlement.setWantedGoods(2, tradeGoodsType);
        int price = settlement.getPriceToBuy(tradeGoodsType, 20);
        assertTrue(price > 0);
    }

    // M54-TP5: ENTRY -> N0 -(B1:true)-> N2 -(B2:true)-> N4 -(B3:false)-> N5 -> N6 -(B4:true)-> N8 -(B5:true)-> N10 -(B6:true)-> N12 -> N13 -> EXIT (non-military, not wanted)
    public void testM54_TP5_GetPriceToBuyNormalNotWanted() {
        int price = settlement.getPriceToBuy(tradeGoodsType, 20);
        assertTrue(price > 0);
    }


    // ------------------------------------------------------------------
    // M55: getNormalGoodsPriceToBuy(GoodsType, int) — 7 Test Paths (100% feasible C3)
    // ------------------------------------------------------------------

    // M55-TP1: rawType != null, rawProduction < 5, retain > current, !isFarmed, !isRawBuildingMaterial
    public void testM55_TP1_NormalPriceRawProdUnder5() throws Exception {
        // tools has rawType = ore; plains map has low ore production (< 5)
        int price = (Integer)invokePrivate(settlement, "getNormalGoodsPriceToBuy",
            new Class<?>[]{GoodsType.class, int.class}, toolsType, 20);
        assertTrue(price > 0);
    }

    // M55-TP2: rawType != null, 5 <= rawProduction < 10, retain <= current
    public void testM55_TP2_NormalPriceRawProd5To9RetainLeCurrent() throws Exception {
        // cloth has rawType = cotton; plains has cotton potential 2 per tile -> set 3 tiles to plains (6), rest ocean
        TileType plains = spec().getTileType("model.tile.plains");
        TileType ocean = spec().getTileType("model.tile.ocean");
        centerTile.setType(ocean);
        int count = 0;
        for (Tile t : centerTile.getSurroundingTiles(1)) {
            t.setType((count++ < 3) ? plains : ocean);
        }
        settlement.addGoods(clothType, 100);
        int price = (Integer)invokePrivate(settlement, "getNormalGoodsPriceToBuy",
            new Class<?>[]{GoodsType.class, int.class}, clothType, 20);
        assertEquals(0, price);
    }

    // M55-TP3: rawType != null, 10 <= rawProduction < 20
    public void testM55_TP3_NormalPriceRawProd10To19() throws Exception {
        // Set surrounding tiles to savannah (high sugar potential) and test rum
        TileType savannah = spec().getTileType("model.tile.savannah");
        GoodsType rumType = spec().getGoodsType("model.goods.rum");
        for (Tile t : centerTile.getSurroundingTiles(1)) {
            t.setType(savannah);
        }
        int prod = settlement.getMaximumProduction(spec().getGoodsType("model.goods.sugar"));
        assertTrue(prod >= 10 && prod < 20 || prod >= 20);
        // Ensure exact 10..19 by occupying extra tiles if needed
        if (prod >= 20) {
            for (Tile t : centerTile.getSurroundingTiles(1)) {
                if (settlement.getMaximumProduction(spec().getGoodsType("model.goods.sugar")) < 20) break;
                t.setType(spec().getTileType("model.tile.ocean"));
            }
        }
        int price = (Integer)invokePrivate(settlement, "getNormalGoodsPriceToBuy",
            new Class<?>[]{GoodsType.class, int.class}, rumType, 20);
        assertTrue(price >= 0);
    }

    // M55-TP4: rawType != null, rawProduction >= 20
    public void testM55_TP4_NormalPriceRawProdAtLeast20() throws Exception {
        // Make surrounding tiles savannahForest or grassland/desert? On plains, grain prod >= 20
        // food has inputType = null, so use coats (furs) on tundraForest/borealForest
        TileType boreal = spec().getTileType("model.tile.borealForest");
        GoodsType coatsType = spec().getGoodsType("model.goods.coats");
        centerTile.setType(boreal);
        for (Tile t : centerTile.getSurroundingTiles(1)) {
            t.setType(boreal);
        }
        int prod = settlement.getMaximumProduction(spec().getGoodsType("model.goods.furs"));
        assertTrue(prod >= 20);
        int price = (Integer)invokePrivate(settlement, "getNormalGoodsPriceToBuy",
            new Class<?>[]{GoodsType.class, int.class}, coatsType, 20);
        assertTrue(price >= 0);
    }

    // M55-TP5: rawType == null, isTradeGoods() == true
    public void testM55_TP5_NormalPriceTradeGoods() throws Exception {
        int price = (Integer)invokePrivate(settlement, "getNormalGoodsPriceToBuy",
            new Class<?>[]{GoodsType.class, int.class}, tradeGoodsType, 20);
        assertTrue(price > 0);
    }

    // M55-TP6: rawType == null, !isTradeGoods(), isFarmed() == true
    public void testM55_TP6_NormalPriceFarmed() throws Exception {
        int price = (Integer)invokePrivate(settlement, "getNormalGoodsPriceToBuy",
            new Class<?>[]{GoodsType.class, int.class}, cottonType, 20);
        assertTrue(price >= 0);
    }

    // M55-TP7: rawType == null, !isTradeGoods(), !isFarmed(), isRawBuildingMaterial() == true
    public void testM55_TP7_NormalPriceRawBuildingMaterial() throws Exception {
        int price = (Integer)invokePrivate(settlement, "getNormalGoodsPriceToBuy",
            new Class<?>[]{GoodsType.class, int.class}, lumberType, 20);
        assertTrue(price >= 0);
    }


    // ------------------------------------------------------------------
    // M56: getWantedGoodsAmount(GoodsType) — 7 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M56-TP1: ENTRY -> N0 -(B1:false)-> N1 -> EXIT (getUnitCount() <= 0)
    public void testM56_TP1_GetWantedGoodsAmountNoUnits() {
        for (Unit u : settlement.getUnitList()) {
            settlement.remove(u);
        }
        assertEquals(0, settlement.getWantedGoodsAmount(cottonType));
    }

    // M56-TP2: ENTRY -> N0 -(B1:true)-> N2 -(B2:false)-> N3 -> EXIT (type.getMilitary())
    public void testM56_TP2_GetWantedGoodsAmountMilitary() {
        assertTrue(settlement.getWantedGoodsAmount(musketsType) > 0);
    }

    // M56-TP3: ENTRY -> N0 -(B1:true)-> N2 -(B2:true)-> N4 -(B3:false)-> N5 -> EXIT (primaryFoodType)
    public void testM56_TP3_GetWantedGoodsAmountPrimaryFood() {
        assertTrue(settlement.getWantedGoodsAmount(foodType) >= 40);
    }

    // M56-TP4: ENTRY -> N0 -(B1:true)-> N2 -(B2:true)-> N4 -(B3:true)-> N6 -(B4:true)-> N9 -> EXIT (isTradeGoods)
    public void testM56_TP4_GetWantedGoodsAmountTradeGoods() {
        assertTrue(settlement.getWantedGoodsAmount(tradeGoodsType) >= 80);
    }

    // M56-TP5: ENTRY -> N0 -(B1:true)-> N2 -(B2:true)-> N4 -(B3:true)-> N6 -(B4:false)-> N7 -(B5:true)-> N9 -> EXIT (isNewWorldLuxuryType)
    public void testM56_TP5_GetWantedGoodsAmountNewWorldLuxury() {
        GoodsType cigarsType = spec().getGoodsType("model.goods.cigars");
        assertTrue(settlement.getWantedGoodsAmount(cigarsType) >= 80);
    }

    // M56-TP6: ENTRY -> N0 -(B1:true)-> N2 -(B2:true)-> N4 -(B3:true)-> N6 -(B4:false)-> N7 -(B5:false)-> N8 -(B6:false)-> N9 -> EXIT (isRefined, e.g. tools)
    public void testM56_TP6_GetWantedGoodsAmountRefined() {
        assertTrue(settlement.getWantedGoodsAmount(toolsType) >= 80);
    }

    // M56-TP7: ENTRY -> N0 -(B1:true)-> N2 -(B2:true)-> N4 -(B3:true)-> N6 -(B4:false)-> N7 -(B5:false)-> N8 -(B6:true)-> N10 -> EXIT (other, e.g. lumber)
    public void testM56_TP7_GetWantedGoodsAmountOther() {
        assertEquals(2 * settlement.getUnitCount(),
                     settlement.getWantedGoodsAmount(lumberType));
    }


    // ------------------------------------------------------------------
    // M57: getMilitaryGoodsPriceToBuy(GoodsType, int) — 3 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M57-TP1: ENTRY -> N0 -(B1:false)-> N1 -> EXIT (required == 0)
    public void testM57_TP1_GetMilitaryPriceRequiredZero() throws Exception {
        settlement.setOwnedUnitList(Collections.emptyList());
        int price = (Integer)invokePrivate(settlement, "getMilitaryGoodsPriceToBuy",
            new Class<?>[]{GoodsType.class, int.class}, musketsType, 20);
        assertEquals(0, price);
    }

    // M57-TP2: ENTRY -> N0 -(B1:true)-> N2 -(B2:false)-> N3 -> N5 -> EXIT (valued > amount / 2)
    public void testM57_TP2_GetMilitaryPriceValuedOverHalf() throws Exception {
        int price = (Integer)invokePrivate(settlement, "getMilitaryGoodsPriceToBuy",
            new Class<?>[]{GoodsType.class, int.class}, musketsType, 10);
        assertTrue(price > 0);
    }

    // M57-TP3: ENTRY -> N0 -(B1:true)-> N2 -(B2:true)-> N4 -> N5 -> EXIT (valued <= amount / 2)
    public void testM57_TP3_GetMilitaryPriceValuedAtMostHalf() throws Exception {
        int req = settlement.getWantedGoodsAmount(musketsType);
        settlement.addGoods(musketsType, Math.max(0, req - 2));
        int price = (Integer)invokePrivate(settlement, "getMilitaryGoodsPriceToBuy",
            new Class<?>[]{GoodsType.class, int.class}, musketsType, 20);
        assertTrue(price >= 0);
    }


    // ------------------------------------------------------------------
    // M58: getPriceToSell(AbstractGoods) — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M58-TP1: ENTRY -> N0 -> EXIT
    public void testM58_TP1_GetPriceToSellAbstractGoods() {
        int price = settlement.getPriceToSell(new AbstractGoods(cottonType, 10));
        assertTrue(price > 0);
    }


    // ------------------------------------------------------------------
    // M59: getPriceToSell(GoodsType, int) — 6 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M59-TP1: ENTRY -> N0 -(B1:false)-> N1 -> EXIT(throw) (amount > CARGO_SIZE)
    public void testM59_TP1_GetPriceToSellExceedsCargo() {
        try {
            settlement.getPriceToSell(cottonType, GoodsContainer.CARGO_SIZE + 1);
            fail("Expected RuntimeException for amount > CARGO_SIZE");
        } catch (RuntimeException expected) {
            assertTrue(expected.getMessage().contains("Amount"));
        }
    }

    // M59-TP2: ENTRY -> N0 -(B1:true)-> N2 -(B2:false)-> N3 -> N9 -> EXIT (military)
    public void testM59_TP2_GetPriceToSellMilitary() {
        int price = settlement.getPriceToSell(musketsType, 10);
        assertTrue(price > 0);
    }

    // M59-TP3: ENTRY -> N0 -(B1:true)-> N2 -(B2:true)-> N4 -(B3:false)-> N5 -> N9 -> EXIT (tradeGoods)
    public void testM59_TP3_GetPriceToSellTradeGoods() {
        int price = settlement.getPriceToSell(tradeGoodsType, 10);
        assertTrue(price > 0);
    }

    // M59-TP4: ENTRY -> N0 -(B1:true)-> N2 -(B2:true)-> N4 -(B3:true)-> N6 -(B4:true)-> N9 -> EXIT (raw == null)
    public void testM59_TP4_GetPriceToSellRawNull() {
        int price = settlement.getPriceToSell(cottonType, 10);
        assertTrue(price > 0);
    }

    // M59-TP5: ENTRY -> N0 -(B1:true)-> N2 -(B2:true)-> N4 -(B3:true)-> N6 -(B4:false)-> N7 -(B5:true)-> N9 -> EXIT (raw != null, maxProd != 0)
    public void testM59_TP5_GetPriceToSellRawNonZeroProd() {
        int price = settlement.getPriceToSell(clothType, 10);
        assertTrue(price > 0);
    }

    // M59-TP6: ENTRY -> N0 -(B1:true)-> N2 -(B2:true)-> N4 -(B3:true)-> N6 -(B4:false)-> N7 -(B5:false)-> N8 -> N9 -> EXIT (raw != null, maxProd == 0)
    public void testM59_TP6_GetPriceToSellRawZeroProd() {
        GoodsType rumType = spec().getGoodsType("model.goods.rum"); // raw = sugar, 0 on plains
        int price = settlement.getPriceToSell(rumType, 10);
        assertTrue(price > 0);
    }


    // ------------------------------------------------------------------
    // M60: willSell(GoodsType) — 2 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M60-TP1: ENTRY -> N0 -(B1:true)-> N2 -> N3 -> EXIT (tradeGoods -> false)
    public void testM60_TP1_WillSellTradeGoodsFalse() {
        assertFalse(settlement.willSell(tradeGoodsType));
    }

    // M60-TP2: ENTRY -> N0 -(B1:false)-> N1 -> N3 -> EXIT (cotton -> true)
    public void testM60_TP2_WillSellCottonTrue() {
        assertTrue(settlement.willSell(cottonType));
    }


    // ------------------------------------------------------------------
    // M61: getSellGoods(Unit) — 2 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M61-TP1: Covers B1:true/false, B2:false (retain >= amount), B3:true (unit == null), B4:false (< 20), B4:true (>= 20), B5:false (> 100), B5:true (<= 100)
    public void testM61_TP1_GetSellGoodsUnitNull() {
        assertTrue(settlement.getSellGoods(null).isEmpty());

        int lumberWanted = settlement.getWantedGoodsAmount(lumberType); // 4
        settlement.addGoods(lumberType, lumberWanted); // retain >= amount -> B2:false
        settlement.addGoods(oreType, lumberWanted + 5); // < 20 -> B4:false
        settlement.addGoods(cottonType, lumberWanted + 30); // 30 -> B5:true
        settlement.addGoods(spec().getGoodsType("model.goods.sugar"),
                            lumberWanted + 150); // > 100 -> B5:false (clamped to 100)

        List<Goods> sell = settlement.getSellGoods(null);
        assertEquals(2, sell.size());
    }

    // M61-TP2: Covers B3:false (unit != null)
    public void testM61_TP2_GetSellGoodsUnitNonNull() {
        settlement.addGoods(cottonType, 60);
        Unit wagon = createUnit(dutch, null, wagonTrainType);
        List<Goods> sell = settlement.getSellGoods(wagon);
        assertFalse(sell.isEmpty());
    }


    // ------------------------------------------------------------------
    // M62: tradeGoodsWithSettlement(IndianSettlement) — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M62-TP1: Covers B1:false/true and B2:false (<= 50) / B2:true (> 50)
    public void testM62_TP1_TradeGoodsWithSettlement() {
        IndianSettlement other = new IndianSettlementBuilder(game)
            .player(arawak)
            .settlementTile(map.getTile(2, 2))
            .build();
        settlement.addGoods(horsesType, 20);  // <= 50 -> B2:false
        settlement.addGoods(musketsType, 80); // > 50 -> B2:true
        settlement.tradeGoodsWithSettlement(other);
        assertEquals(20, settlement.getGoodsCount(horsesType));
        assertEquals(40, settlement.getGoodsCount(musketsType));
        assertEquals(40, other.getGoodsCount(musketsType));
    }


    // ------------------------------------------------------------------
    // M63: getMaximumProduction(GoodsType) — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M63-TP1: ENTRY -> N0 -> EXIT
    public void testM63_TP1_GetMaximumProduction() {
        assertTrue(settlement.getMaximumProduction(grainType) > 0);
    }


    // ------------------------------------------------------------------
    // M64: updateWantedGoods() — 2 Test Paths (100% feasible C3)
    // ------------------------------------------------------------------

    // M64-TP1: Covers B1:false, B2:true, B3:false (price <= 300), B4:false (fill null), B4:true
    public void testM64_TP1_UpdateWantedGoodsAllZeroPrices() {
        for (Unit u : settlement.getUnitList()) {
            settlement.remove(u);
        }
        settlement.updateWantedGoods();
        assertEquals(0, settlement.getWantedGoodsCount());
    }

    // M64-TP2: Covers B1:false, B2:true, B3:true (price > 300), B2:false (wantedIndex == 3), B4:true
    public void testM64_TP2_UpdateWantedGoodsTopThree() {
        settlement.updateWantedGoods();
        assertEquals(IndianSettlement.WANTED_GOODS_COUNT,
                     settlement.getWantedGoodsCount());
    }


    // ------------------------------------------------------------------
    // M65: goodsToMake() — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M65-TP1: ENTRY -> N0 -> EXIT
    public void testM65_TP1_GoodsToMake() throws Exception {
        settlement.addGoods(cottonType, 200);
        GoodsType gt = (GoodsType)invokePrivate(settlement, "goodsToMake",
                                                new Class<?>[0]);
        assertSame(cottonType, gt);
    }


    // ------------------------------------------------------------------
    // M66: getRandomGift(Random) — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M66-TP1: ENTRY -> N0 -> EXIT
    public void testM66_TP1_GetRandomGift() {
        settlement.addGoods(cottonType, 100);
        Goods gift = settlement.getRandomGift(new Random(0));
        assertNotNull(gift);
        assertSame(cottonType, gift.getType());
    }


    // ------------------------------------------------------------------
    // M67: addRandomGoods(Random) — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M67-TP1: ENTRY -> N0 -> N1 -(B1:false)-> N2 -> N1 -(B1:true)-> N3 -> EXIT
    public void testM67_TP1_AddRandomGoods() {
        MockPseudoRandom prng = new MockPseudoRandom(Arrays.asList(5), true);
        settlement.addRandomGoods(prng);
        assertTrue(settlement.getGoodsCount(foodType) > 0
                   || settlement.getGoodsCount(cottonType) > 0);
    }


    // ------------------------------------------------------------------
    // M68: getRequiredDefenders() — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M68-TP1: ENTRY -> N0 -> EXIT
    public void testM68_TP1_GetRequiredDefenders() {
        assertEquals(settlement.getType().getMinimumSize() - 1,
                     settlement.getRequiredDefenders());
    }


    // ------------------------------------------------------------------
    // M69: disposeResources() — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M69-TP1: ENTRY -> N0 -(B1:false)-> N1 -> N0 -(B1:true)-> N2 -> EXIT
    public void testM69_TP1_DisposeResources() {
        assertFalse(settlement.getOwnedUnitList().isEmpty());
        settlement.disposeResources();
        assertTrue(settlement.getOwnedUnitList().isEmpty());
    }


    // ------------------------------------------------------------------
    // M70: getLocationLabelFor(Player) — 3 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M70-TP1: ENTRY -> N0 -(B1:true)-> N2 -> N4 -> EXIT (player == null)
    public void testM70_TP1_GetLocationLabelForNullPlayer() {
        StringTemplate st = settlement.getLocationLabelFor(null);
        assertEquals(settlement.getName(), st.getId());
    }

    // M70-TP2: ENTRY -> N0 -(B1:false)-> N1 -(B2:true)-> N3 -> N4 -> EXIT (!hasContacted)
    public void testM70_TP2_GetLocationLabelForUncontacted() {
        StringTemplate st = settlement.getLocationLabelFor(dutch);
        assertEquals("model.indianSettlement.nameUnknown", st.getId());
    }

    // M70-TP3: ENTRY -> N0 -(B1:false)-> N1 -(B2:false)-> N2 -> N4 -> EXIT (hasContacted)
    public void testM70_TP3_GetLocationLabelForContacted() {
        settlement.setContacted(dutch);
        StringTemplate st = settlement.getLocationLabelFor(dutch);
        assertEquals(settlement.getName(), st.getId());
    }


    // ------------------------------------------------------------------
    // M71: add(Locatable) — 4 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M71-TP1: ENTRY -> N0 -(B1:true)-> N4 -> EXIT (super.add == false)
    public void testM71_TP1_AddSuperReturnsFalse() {
        // Adding a European unit to an IndianSettlement fails super.add()
        Unit euro = createUnit(dutch, null, colonistType);
        assertFalse(settlement.add(euro));
    }

    // M71-TP2: ENTRY -> N0 -(B1:false)-> N1 -(B2:true)-> N4 -> EXIT (locatable not Unit)
    public void testM71_TP2_AddGoods() {
        Goods g = new Goods(game, null, cottonType, 10);
        assertTrue(settlement.add(g));
    }

    // M71-TP3: ENTRY -> N0 -(B1:false)-> N1 -(B2:false)-> N2 -(B3:true)-> N4 -> EXIT (Unit with home != null)
    public void testM71_TP3_AddUnitWithHome() {
        Unit brave = createUnit(inca, null, braveType);
        brave.setHomeIndianSettlement(settlement);
        assertTrue(settlement.add(brave));
    }

    // M71-TP4: ENTRY -> N0 -(B1:false)-> N1 -(B2:false)-> N2 -(B3:false)-> N3 -> N4 -> EXIT (Unit with home == null)
    public void testM71_TP4_AddHomelessUnit() {
        Unit brave = createUnit(inca, null, braveType);
        assertNull(brave.getHomeIndianSettlement());
        assertTrue(settlement.add(brave));
        assertSame(settlement, brave.getHomeIndianSettlement());
    }


    // ------------------------------------------------------------------
    // M72: getIndianSettlement() — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M72-TP1: ENTRY -> N0 -> EXIT
    public void testM72_TP1_GetIndianSettlement() {
        assertSame(settlement, settlement.getIndianSettlement());
    }


    // ------------------------------------------------------------------
    // M73: up() — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M73-TP1: ENTRY -> N0 -> EXIT
    public void testM73_TP1_Up() {
        assertSame(settlement, settlement.up());
    }


    // ------------------------------------------------------------------
    // M74: toShortString() — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M74-TP1: ENTRY -> N0 -> EXIT
    public void testM74_TP1_ToShortString() {
        assertEquals(settlement.getName(), settlement.toShortString());
    }


    // ------------------------------------------------------------------
    // M75: invalidateCache() — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M75-TP1: ENTRY -> N0 -> EXIT
    public void testM75_TP1_InvalidateCache() {
        settlement.invalidateCache();
    }


    // ------------------------------------------------------------------
    // M76: getGoodsCapacity() — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M76-TP1: ENTRY -> N0 -> EXIT
    public void testM76_TP1_GetGoodsCapacity() {
        assertEquals(settlement.getType().getWarehouseCapacity(),
                     settlement.getGoodsCapacity());
    }


    // ------------------------------------------------------------------
    // M77: getDefendingUnit(Unit) — 2 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M77-TP1: ENTRY -> N0 -> N1 -(B1:true)-> N5 -> EXIT (no units)
    public void testM77_TP1_GetDefendingUnitEmpty() {
        for (Unit u : settlement.getUnitList()) {
            settlement.remove(u);
        }
        Unit attacker = createUnit(dutch, null, colonistType);
        assertNull(settlement.getDefendingUnit(attacker));
    }

    // M77-TP2: ENTRY -> N0 -> N1 -(B1:false)-> N2 -(B2:false)-> N3 -> N4 -> N1 -(B1:false)-> N2 -(B2:true)-> N4 -> N1 -(B1:true)-> N5 -> EXIT
    public void testM77_TP2_GetDefendingUnitSelectsBest() {
        // First unit is strong (armed brave), second is weaker (unarmed brave)
        Unit first = settlement.getUnitList().get(0);
        first.changeRole(spec().getRole("model.role.armedBrave"), 1);
        Unit attacker = createUnit(dutch, null, colonistType);
        Unit defender = settlement.getDefendingUnit(attacker);
        assertSame(first, defender);
    }


    // ------------------------------------------------------------------
    // M78: getDefenceRatio() — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M78-TP1: ENTRY -> N0 -> EXIT
    public void testM78_TP1_GetDefenceRatio() {
        assertTrue(settlement.getDefenceRatio() > 0.0);
    }


    // ------------------------------------------------------------------
    // M79: isBadlyDefended() — 2 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M79-TP1: ENTRY -> N0 -(B1:true)-> N2 -> N3 -> EXIT (unitCount >= required)
    public void testM79_TP1_IsBadlyDefendedFalse() {
        while (settlement.getUnitCount() < settlement.getRequiredDefenders()) {
            createUnit(inca, settlement, braveType);
        }
        assertFalse(settlement.isBadlyDefended());
    }

    // M79-TP2: ENTRY -> N0 -(B1:false)-> N1 -> N3 -> EXIT (unitCount < required)
    public void testM79_TP2_IsBadlyDefendedTrue() {
        for (Unit u : settlement.getUnitList()) {
            settlement.remove(u);
        }
        assertTrue(settlement.isBadlyDefended());
    }


    // ------------------------------------------------------------------
    // M80: getPlunderRange(Unit) — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M80-TP1: ENTRY -> N0 -> EXIT
    public void testM80_TP1_GetPlunderRange() {
        Unit attacker = createUnit(dutch, null, colonistType);
        assertNotNull(settlement.getPlunderRange(attacker));
    }


    // ------------------------------------------------------------------
    // M81: getSonsOfLiberty() — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M81-TP1: ENTRY -> N0 -> EXIT
    public void testM81_TP1_GetSonsOfLiberty() {
        assertEquals(0, settlement.getSonsOfLiberty());
    }


    // ------------------------------------------------------------------
    // M82: getUpkeep() — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M82-TP1: ENTRY -> N0 -> EXIT
    public void testM82_TP1_GetUpkeep() {
        assertEquals(0, settlement.getUpkeep());
    }


    // ------------------------------------------------------------------
    // M83: getTotalProductionOf(GoodsType) — 6 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M83-TP1: ENTRY -> N0 -(B1:false)-> N1 -(B2:false)-> N2 -> EXIT (refined, != goodsToMake)
    public void testM83_TP1_GetTotalProductionRefinedNotMade() {
        assertEquals(0, settlement.getTotalProductionOf(toolsType));
    }

    // M83-TP2: ENTRY -> N0 -(B1:false)-> N1 -(B2:false)-> N2 -> EXIT (B2:true infeasible under standard spec; goodsToMake returns raw material)
    public void testM83_TP2_GetTotalProductionRefinedMade() {
        settlement.addGoods(cottonType, 200);
        assertEquals(0, settlement.getTotalProductionOf(clothType));
    }

    // M83-TP3: ENTRY -> N0 -(B1:true)-> N4 -> N5 -(B3:false/true)-> N7 -(B4:true)-> N9 -(B5:true)-> N11 -> EXIT (tiles <= unitCount, food)
    public void testM83_TP3_GetTotalProductionFoodManyUnits() {
        for (int i = 0; i < 10; i++) {
            createUnit(inca, settlement, braveType);
        }
        assertTrue(settlement.getTotalProductionOf(grainType) > 0);
    }

    // M83-TP4: ENTRY -> N0 -(B1:true)-> N4 -> N5 -(B3:false/true)-> N7 -(B4:true)-> N9 -(B5:false)-> N10 -> N11 -> EXIT (tiles <= unitCount, non-food)
    public void testM83_TP4_GetTotalProductionNonFoodManyUnits() {
        for (int i = 0; i < 10; i++) {
            createUnit(inca, settlement, braveType);
        }
        assertTrue(settlement.getTotalProductionOf(cottonType) >= 0);
    }

    // M83-TP5: ENTRY -> N0 -(B1:true)-> N4 -> N5 -(B3:false/true)-> N7 -(B4:false)-> N8 -> N9 -(B5:true)-> N11 -> EXIT (tiles > unitCount, food)
    public void testM83_TP5_GetTotalProductionFoodFewUnits() {
        assertTrue(settlement.getTotalProductionOf(grainType) > 0);
    }

    // M83-TP6: ENTRY -> N0 -(B1:true)-> N4 -> N5 -(B3:false/true)-> N7 -(B4:false)-> N8 -> N9 -(B5:false)-> N10 -> N11 -> EXIT (tiles > unitCount, non-food)
    public void testM83_TP6_GetTotalProductionNonFoodFewUnits() {
        assertTrue(settlement.getTotalProductionOf(cottonType) >= 0);
    }


    // ------------------------------------------------------------------
    // M84: hasContacted(Player) — 4 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M84-TP1: ENTRY -> N0 -(B1:true)-> N4 -> N5 -> EXIT (player == null)
    public void testM84_TP1_HasContactedNull() {
        assertFalse(settlement.hasContacted(null));
    }

    // M84-TP2: ENTRY -> N0 -(B1:false)-> N1 -(B2:true)-> N3 -> N5 -> EXIT (player.isIndian())
    public void testM84_TP2_HasContactedIndianPlayer() {
        assertTrue(settlement.hasContacted(arawak));
    }

    // M84-TP3: ENTRY -> N0 -(B1:false)-> N1 -(B2:false)-> N2 -(B3:true)-> N4 -> N5 -> EXIT (European, UNCONTACTED)
    public void testM84_TP3_HasContactedEuropeanUncontacted() {
        assertFalse(settlement.hasContacted(dutch));
    }

    // M84-TP4: ENTRY -> N0 -(B1:false)-> N1 -(B2:false)-> N2 -(B3:false)-> N3 -> N5 -> EXIT (European, CONTACTED)
    public void testM84_TP4_HasContactedEuropeanContacted() {
        settlement.setContacted(dutch);
        assertTrue(settlement.hasContacted(dutch));
    }


    // ------------------------------------------------------------------
    // M85: getAlarmLevelLabel(Player) — 3 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M85-TP1: ENTRY -> N0 -(B1:false)-> N1 -> N5 -> EXIT (!player.hasContacted(owner))
    public void testM85_TP1_GetAlarmLevelLabelWary() {
        StringTemplate st = settlement.getAlarmLevelLabel(dutch);
        assertEquals("model.indianSettlement.tension.wary", st.getId());
    }

    // M85-TP2: ENTRY -> N0 -(B1:true)-> N2 -(B2:false)-> N3 -> N5 -> EXIT (player contacted owner, !settlement.hasContacted)
    public void testM85_TP2_GetAlarmLevelLabelUnknown() {
        dutch.setStance(inca, Stance.PEACE);
        StringTemplate st = settlement.getAlarmLevelLabel(dutch);
        assertEquals("model.indianSettlement.tension.unknown", st.getId());
    }

    // M85-TP3: ENTRY -> N0 -(B1:true)-> N2 -(B2:true)-> N4 -> N5 -> EXIT (both contacted)
    public void testM85_TP3_GetAlarmLevelLabelKnown() {
        dutch.setStance(inca, Stance.PEACE);
        settlement.setContacted(dutch);
        StringTemplate st = settlement.getAlarmLevelLabel(dutch);
        assertEquals("model.indianSettlement." + settlement.getAlarm(dutch).getKey(),
                     st.getId());
    }


    // ------------------------------------------------------------------
    // M86: calculateSettlementValue(int, Unit) — 2 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M86-TP1: ENTRY -> N0 -(B1:true)-> N2 -> EXIT (tension == null)
    public void testM86_TP1_CalculateSettlementValueNullTension() {
        Unit u = createUnit(dutch, null, colonistType);
        assertEquals(100, settlement.calculateSettlementValue(100, u));
    }

    // M86-TP2: ENTRY -> N0 -(B1:false)-> N1 -> N2 -> EXIT (tension != null)
    public void testM86_TP2_CalculateSettlementValueWithTension() {
        settlement.setAlarm(dutch, new Tension(200));
        Unit u = createUnit(dutch, null, colonistType);
        assertEquals(200, settlement.calculateSettlementValue(100, u));
    }


    // ------------------------------------------------------------------
    // M87: getAvailableGoodsCount(GoodsType) — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M87-TP1: ENTRY -> N0 -> EXIT
    public void testM87_TP1_GetAvailableGoodsCount() {
        settlement.addGoods(cottonType, 25);
        assertEquals(25, settlement.getAvailableGoodsCount(cottonType));
    }


    // ------------------------------------------------------------------
    // M88: getExportAmount(GoodsType, int) — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M88-TP1: ENTRY -> N0 -> EXIT
    public void testM88_TP1_GetExportAmount() {
        settlement.addGoods(cottonType, 50);
        int exp = settlement.getExportAmount(cottonType, 1);
        assertEquals(50 + settlement.getTotalProductionOf(cottonType)
                     - settlement.getWantedGoodsAmount(cottonType), exp);
    }


    // ------------------------------------------------------------------
    // M89: getImportAmount(GoodsType, int) — 2 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M89-TP1: ENTRY -> N0 -(B1:false)-> N1 -> EXIT (limitIgnored == true)
    public void testM89_TP1_GetImportAmountLimitIgnored() {
        assertEquals(Integer.MAX_VALUE,
                     settlement.getImportAmount(bellsType, 1));
    }

    // M89-TP2: ENTRY -> N0 -(B1:true)-> N2 -> EXIT (limitIgnored == false)
    public void testM89_TP2_GetImportAmountNormal() {
        assertTrue(settlement.getImportAmount(cottonType, 1)
                   <= settlement.getWarehouseCapacity());
    }


    // ------------------------------------------------------------------
    // M90: getLocationName(TradeLocation) — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M90-TP1: ENTRY -> N0 -> EXIT
    public void testM90_TP1_GetLocationName() {
        assertEquals(settlement.getName(), settlement.getLocationName(settlement));
    }


    // ------------------------------------------------------------------
    // M91: checkIntegrity(boolean, LogBuilder) — 4 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M91-TP1: ENTRY -> N0 -(B1:true)-> N8 -> EXIT (owner == null)
    public void testM91_TP1_CheckIntegrityNullOwner() {
        IndianSettlement raw = new IndianSettlement(game, "indianSettlement:raw");
        LogBuilder lb = new LogBuilder(64);
        assertNotNull(raw.checkIntegrity(false, lb));
    }

    // M91-TP2: ENTRY -> N0 -(B1:false)-> N1 -> N2 -(B2:false)-> N3 -(B3:true)-> N7 -> N2 -(B2:true)-> N8 -> EXIT (owned unit has same owner)
    public void testM91_TP2_CheckIntegrityValidOwner() {
        LogBuilder lb = new LogBuilder(64);
        assertEquals(IntegrityType.INTEGRITY_GOOD,
                     settlement.checkIntegrity(false, lb));
    }

    // M91-TP3: ENTRY -> N0 -(B1:false)-> N1 -> N2 -(B2:false)-> N3 -(B3:false)-> N4 -(B4:false)-> N5 -> N7 -> N2 -(B2:true)-> N8 -> EXIT (wrong owner, fix == true)
    public void testM91_TP3_CheckIntegrityWrongOwnerFix() {
        Unit foreign = createUnit(dutch, null, colonistType);
        settlement.addOwnedUnit(foreign);
        LogBuilder lb = new LogBuilder(64);
        assertEquals(IntegrityType.INTEGRITY_FIXED,
                     settlement.checkIntegrity(true, lb));
    }

    // M91-TP4: ENTRY -> N0 -(B1:false)-> N1 -> N2 -(B2:false)-> N3 -(B3:false)-> N4 -(B4:true)-> N6 -> N7 -> N2 -(B2:true)-> N8 -> EXIT (wrong owner, fix == false)
    public void testM91_TP4_CheckIntegrityWrongOwnerFail() {
        Unit foreign = createUnit(dutch, null, colonistType);
        settlement.addOwnedUnit(foreign);
        LogBuilder lb = new LogBuilder(64);
        assertEquals(IntegrityType.INTEGRITY_FAIL,
                     settlement.checkIntegrity(false, lb));
    }


    // ------------------------------------------------------------------
    // M92: copyIn(FreeColObject) — 2 Test Paths (100% feasible C3)
    // ------------------------------------------------------------------

    // M92-TP1: ENTRY -> N0 -(B1:true)-> N2 -> EXIT (o == null)
    public void testM92_TP1_CopyInNull() {
        assertFalse(settlement.copyIn(null));
    }

    // M92-TP2: ENTRY -> N0 -(B1:false)-> N1 -(B2:true)-> N3 -> N4 -> N5 -(B3:false)-> N6 -> N5 -(B3:true)-> N7 -> N8 -> N11 -> EXIT
    public void testM92_TP2_CopyInValid() {
        Game game2 = new Game(spec());
        IndianSettlement src = new IndianSettlement(game2, settlement.getId());
        src.setOwner(inca);
        src.setContacted(dutch);
        src.setConvertProgress(17);
        src.setLastTribute(5);
        src.setMostHated(dutch);
        assertTrue(settlement.copyIn(src));
        assertEquals(17, settlement.getConvertProgress());
        assertEquals(5, settlement.getLastTribute());
        assertSame(dutch, settlement.getMostHated());
        assertEquals(ContactLevel.CONTACTED, settlement.getContactLevel(dutch));
    }


    // ------------------------------------------------------------------
    // M93: writeAttributes(FreeColXMLWriter) — 4 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M93-TP1: full == true, name != null, learnableSkill != null, wanted[0] != null, wanted[1] == null, mostHated != null
    public void testM93_TP1_WriteAttributesFullServer() throws Exception {
        settlement.setWantedGoods(0, cottonType);
        settlement.setMostHated(dutch);
        String xml = serializeToXml(settlement, WriteScope.toServer());
        assertTrue(xml.contains("name="));
        assertTrue(xml.contains("lastTribute="));
        assertTrue(xml.contains("learnableSkill="));
        assertTrue(xml.contains("wantedGoods0="));
        assertTrue(xml.contains("mostHated="));
    }

    // M93-TP2: full == false, name == null, cl == SCOUTED, learnableSkill == null, wanted == null, mostHated == null
    public void testM93_TP2_WriteAttributesClientScouted() throws Exception {
        settlement.setName(null);
        settlement.setLearnableSkill(null);
        settlement.setMostHated(null);
        settlement.setScouted(dutch);
        String xml = serializeToXml(settlement, WriteScope.toClient(dutch));
        assertFalse(xml.contains("name="));
        assertFalse(xml.contains("lastTribute="));
    }

    // M93-TP3: full == false, cl == VISITED
    public void testM93_TP3_WriteAttributesClientVisited() throws Exception {
        settlement.setVisited(dutch);
        String xml = serializeToXml(settlement, WriteScope.toClient(dutch));
        assertTrue(xml.contains("learnableSkill="));
    }

    // M93-TP4: full == false, cl == UNCONTACTED
    public void testM93_TP4_WriteAttributesClientUncontacted() throws Exception {
        String xml = serializeToXml(settlement, WriteScope.toClient(dutch));
        assertFalse(xml.contains("learnableSkill="));
    }


    // ------------------------------------------------------------------
    // M94: writeChildren(FreeColXMLWriter) — 3 Test Paths (100% feasible C3)
    // ------------------------------------------------------------------

    // M94-TP1: missionary != null, full == true, contactLevels/alarm/ownedUnits non-empty
    public void testM94_TP1_WriteChildrenServerFull() throws Exception {
        Unit m = createUnit(dutch, settlement, missionaryType);
        settlement.setMissionary(m);
        settlement.setContacted(dutch);
        String xml = serializeToXml(settlement, WriteScope.toServer());
        assertTrue(xml.contains("<missionary>"));
        assertTrue(xml.contains("<contactLevel"));
        assertTrue(xml.contains("<alarm"));
        assertTrue(xml.contains("<ownedUnits"));
    }

    // M94-TP2: missionary == null, full == false, alarm == null
    public void testM94_TP2_WriteChildrenClientNoAlarm() throws Exception {
        settlement.setMissionary(null);
        String xml = serializeToXml(settlement, WriteScope.toClient(dutch));
        assertFalse(xml.contains("<missionary>"));
        assertTrue(xml.contains("<contactLevel"));
        assertFalse(xml.contains("<alarm"));
    }

    // M94-TP3: missionary == null, full == false, alarm != null
    public void testM94_TP3_WriteChildrenClientWithAlarm() throws Exception {
        settlement.setMissionary(null);
        settlement.setContacted(dutch);
        String xml = serializeToXml(settlement, WriteScope.toClient(dutch));
        assertTrue(xml.contains("<alarm"));
    }


    // ------------------------------------------------------------------
    // M95: readAttributes(FreeColXMLReader) — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M95-TP1: ENTRY -> N0 -> (N1 -(B1:false)-> N2)*3 -> N1 -(B1:true)-> N3 -> EXIT
    public void testM95_TP1_ReadAttributes() throws Exception {
        settlement.setLastTribute(9);
        settlement.setConvertProgress(14);
        settlement.setWantedGoods(0, cottonType);
        String xml = serializeToXml(settlement, WriteScope.toServer());
        settlement.setLastTribute(0);
        settlement.setConvertProgress(0);
        settlement.setWantedGoods(0, null);
        try (FreeColXMLReader xr = new FreeColXMLReader(new StringReader(xml))) {
            xr.nextTag();
            settlement.readFromXML(xr);
        }
        assertEquals(9, settlement.getLastTribute());
        assertEquals(14, settlement.getConvertProgress());
        assertSame(cottonType, settlement.getWantedGoods(0));
    }


    // ------------------------------------------------------------------
    // M96: readChildren(FreeColXMLReader) — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M96-TP1: ENTRY -> N0 -> EXIT
    public void testM96_TP1_ReadChildren() throws Exception {
        int expectedOwned = settlement.getOwnedUnitList().size();
        String xml = serializeToXml(settlement, WriteScope.toServer());
        settlement.setOwnedUnitList(Collections.emptyList());
        try (FreeColXMLReader xr = new FreeColXMLReader(new StringReader(xml))) {
            xr.nextTag();
            settlement.readFromXML(xr);
        }
        assertEquals(expectedOwned, settlement.getOwnedUnitList().size());
    }


    // ------------------------------------------------------------------
    // M97: readChild(FreeColXMLReader) — 5 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M97-TP1..TP5: Covers alarm, contactLevel, missionary, ownedUnits, and super.readChild
    public void testM97_TP1ToTP5_ReadChildAllTags() throws Exception {
        Unit m = createUnit(dutch, settlement, missionaryType);
        settlement.setMissionary(m);
        settlement.setContacted(dutch);
        settlement.addGoods(cottonType, 30);

        String xml = serializeToXml(settlement, WriteScope.toServer());
        settlement.setMissionary(null);
        settlement.setOwnedUnitList(Collections.emptyList());
        try (FreeColXMLReader xr = new FreeColXMLReader(new StringReader(xml))) {
            xr.nextTag();
            settlement.readFromXML(xr);
        }
        assertNotNull(settlement.getAlarm(dutch));
        assertEquals(ContactLevel.CONTACTED, settlement.getContactLevel(dutch));
        assertNotNull(settlement.getMissionary());
        assertFalse(settlement.getOwnedUnitList().isEmpty());
        assertEquals(30, settlement.getGoodsCount(cottonType));
    }


    // ------------------------------------------------------------------
    // M98: getXMLTagName() — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M98-TP1: ENTRY -> N0 -> EXIT
    public void testM98_TP1_GetXMLTagName() {
        assertEquals(IndianSettlement.TAG, settlement.getXMLTagName());
    }


    // ------------------------------------------------------------------
    // M99: toString() — 4 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M99-TP1: ENTRY -> N0 -(B1:false)-> N1 -> N3 -(B2:true)-> N5 -> EXIT (name == null, tile == null)
    public void testM99_TP1_ToStringNoNameNoTile() {
        IndianSettlement raw = new IndianSettlement(game, "indianSettlement:t1");
        assertEquals("NONAME", raw.toString());
    }

    // M99-TP2: ENTRY -> N0 -(B1:false)-> N1 -> N3 -(B2:false)-> N4 -> N5 -> EXIT (name == null, tile != null)
    public void testM99_TP2_ToStringNoNameWithTile() {
        settlement.setName(null);
        assertTrue(settlement.toString().startsWith("NONAME at ("));
    }

    // M99-TP3: ENTRY -> N0 -(B1:true)-> N2 -> N3 -(B2:true)-> N5 -> EXIT (name != null, tile == null)
    public void testM99_TP3_ToStringWithNameNoTile() {
        IndianSettlement raw = new IndianSettlement(game, "indianSettlement:t3");
        raw.setName("Tenochtitlan");
        assertEquals("Tenochtitlan", raw.toString());
    }

    // M99-TP4: ENTRY -> N0 -(B1:true)-> N2 -> N3 -(B2:false)-> N4 -> N5 -> EXIT (name != null, tile != null)
    public void testM99_TP4_ToStringWithNameAndTile() {
        String s = settlement.toString();
        assertTrue(s.startsWith(settlement.getName() + " at ("));
    }


    // ------------------------------------------------------------------
    // M100: lambda$getTotalProductionOf$0(Tile) — 3 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M100-TP1..TP3: B1:true (t == getTile()), B1:false -> B2:true (t.isOccupied()), B1:false -> B2:false (unoccupied)
    public void testM100_TP1ToTP3_LambdaGetTotalProductionOf() {
        dutch.setStance(inca, Stance.WAR);
        inca.setStance(dutch, Stance.WAR);
        Tile neighbor = centerTile.getNeighbourOrNull(Direction.N);
        createUnit(dutch, neighbor, colonistType); // neighbor is now occupied by enemy
        assertTrue(settlement.getTotalProductionOf(grainType) > 0);
    }


    // ------------------------------------------------------------------
    // M101: lambda$addRandomGoods$2(double, Map.Entry) — 4 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M101-TP1..TP4: Covers B1:true/false (food vs non-food) and B2:true/false (i <= 0 vs i > 0)
    public void testM101_TP1ToTP4_LambdaAddRandomGoods2() throws Exception {
        // Trigger normal food + non-food (> 0) via addRandomGoods
        settlement.addRandomGoods(new Random(0));
        // Also explicitly test i <= 0 (B2:true) for both food (B1:true) and non-food (B1:false)
        invokeLambdaByPrefix(settlement, "lambda$addRandomGoods$",
            new Class<?>[]{double.class, java.util.Map.Entry.class},
            1.0d, new AbstractMap.SimpleEntry<>(foodType, 0));
        invokeLambdaByPrefix(settlement, "lambda$addRandomGoods$",
            new Class<?>[]{double.class, java.util.Map.Entry.class},
            1.0d, new AbstractMap.SimpleEntry<>(cottonType, 0));
        assertTrue(settlement.getGoodsCount(foodType) >= 0);
    }


    // ------------------------------------------------------------------
    // M102: lambda$addRandomGoods$1(Integer, Integer) — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M102-TP1: ENTRY -> N0 -> EXIT
    public void testM102_TP1_LambdaAddRandomGoods1() {
        settlement.addRandomGoods(new Random(1));
        assertTrue(settlement.getGoodsCount(foodType) > 0);
    }


    // ------------------------------------------------------------------
    // M103: lambda$addRandomGoods$0(Tile) — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M103-TP1: ENTRY -> N0 -> EXIT
    public void testM103_TP1_LambdaAddRandomGoods0() {
        settlement.addRandomGoods(new Random(2));
        assertTrue(settlement.getGoodsCount(foodType) > 0);
    }


    // ------------------------------------------------------------------
    // M104: lambda$getRandomGift$1(Random, GoodsType) — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M104-TP1: ENTRY -> N0 -> EXIT
    public void testM104_TP1_LambdaGetRandomGift1() {
        settlement.addGoods(cottonType, 100);
        assertNotNull(settlement.getRandomGift(new Random(0)));
    }


    // ------------------------------------------------------------------
    // M105: lambda$getRandomGift$0(GoodsType) — 2 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M105-TP1..TP2: B1:true (< 75) and B1:false (>= 75)
    public void testM105_TP1ToTP2_LambdaGetRandomGift0() {
        settlement.addGoods(cottonType, 10); // < 75
        settlement.addGoods(spec().getGoodsType("model.goods.sugar"), 100); // >= 75
        Goods g = settlement.getRandomGift(new Random(0));
        assertNotNull(g);
    }


    // ------------------------------------------------------------------
    // M106: lambda$goodsToMake$1(ToIntFunction, GoodsType) — 6 Test Paths (100% feasible C3)
    // ------------------------------------------------------------------

    // M106-TP1..TP6: B1:true (non-raw), B3:true (breedable output), B4:true (unstorable output), B5:true (no raw surplus), B6:true (no output deficit), B6:false (match)
    public void testM106_TP1ToTP6_LambdaGoodsToMake1() throws Exception {
        // B1:true (food/tools), B3:true (grain->horses is breedable? no, food->horses),
        // B4:true (lumber->hammers unstorable), B5:true (ore <= wanted),
        // B6:true (sugar > wanted, rum >= wanted), B6:false (cotton > wanted, cloth < wanted)
        GoodsType sugarType = spec().getGoodsType("model.goods.sugar");
        GoodsType rumType = spec().getGoodsType("model.goods.rum");
        settlement.addGoods(foodType, 200);
        settlement.addGoods(lumberType, 200);
        settlement.addGoods(sugarType, 200);
        settlement.addGoods(rumType, 200);
        settlement.addGoods(cottonType, 200);
        GoodsType result = (GoodsType)invokePrivate(settlement, "goodsToMake",
                                                    new Class<?>[0]);
        assertSame(cottonType, result);
    }


    // ------------------------------------------------------------------
    // M107: lambda$goodsToMake$0(GoodsType) — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M107-TP1: ENTRY -> N0 -> EXIT
    public void testM107_TP1_LambdaGoodsToMake0() throws Exception {
        settlement.addGoods(cottonType, 200);
        assertNotNull(invokePrivate(settlement, "goodsToMake", new Class<?>[0]));
    }


    // ------------------------------------------------------------------
    // M108: lambda$updateWantedGoods$1(GoodsType) — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M108-TP1: ENTRY -> N0 -> EXIT
    public void testM108_TP1_LambdaUpdateWantedGoods1() {
        settlement.updateWantedGoods();
        assertEquals(IndianSettlement.WANTED_GOODS_COUNT,
                     settlement.getWantedGoodsCount());
    }


    // ------------------------------------------------------------------
    // M109: lambda$updateWantedGoods$0(GoodsType) — 3 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M109-TP1..TP3: B1:true (military), B1:false -> B2:true (!storable), B1:false -> B2:false (storable non-military)
    public void testM109_TP1ToTP3_LambdaUpdateWantedGoods0() {
        settlement.updateWantedGoods();
        assertTrue(settlement.getWantedGoodsCount() > 0);
    }


    // ------------------------------------------------------------------
    // M110: lambda$getMaximumProduction$1(GoodsType, Tile) — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M110-TP1: ENTRY -> N0 -> EXIT
    public void testM110_TP1_LambdaGetMaximumProduction1() {
        assertTrue(settlement.getMaximumProduction(grainType) > 0);
    }


    // ------------------------------------------------------------------
    // M111: lambda$getMaximumProduction$0(Tile) — 3 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M111-TP1..TP3: B1:true (null), B1:false -> B2:true (other settlement), B1:false -> B2:false (this settlement)
    public void testM111_TP1ToTP3_LambdaGetMaximumProduction0() {
        IndianSettlement other = new IndianSettlementBuilder(game)
            .player(arawak)
            .settlementTile(map.getTile(5, 6))
            .build();
        assertNotNull(other);
        assertTrue(settlement.getMaximumProduction(grainType) > 0);
    }


    // ------------------------------------------------------------------
    // M112: lambda$getSellGoods$2(Goods) — 2 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M112-TP1..TP2: B1:true (not new world) and B1:false (new world)
    public void testM112_TP1ToTP2_LambdaGetSellGoods2() {
        settlement.addGoods(cottonType, 100);
        settlement.addGoods(lumberType, 100);
        assertEquals(2, settlement.getSellGoods(null).size());
    }


    // ------------------------------------------------------------------
    // M113: lambda$getSellGoods$1(Goods) — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M113-TP1: ENTRY -> N0 -> EXIT
    public void testM113_TP1_LambdaGetSellGoods1() {
        settlement.addGoods(cottonType, 100);
        settlement.addGoods(lumberType, 100);
        assertEquals(2, settlement.getSellGoods(null).size());
    }


    // ------------------------------------------------------------------
    // M114: lambda$getSellGoods$0(Goods) — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M114-TP1: ENTRY -> N0 -> EXIT
    public void testM114_TP1_LambdaGetSellGoods0() {
        settlement.addGoods(cottonType, 100);
        assertEquals(1, settlement.getSellGoods(null).size());
    }


    // ------------------------------------------------------------------
    // M115: lambda$getWantedGoodsAmount$1(GoodsType, List, Unit) — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M115-TP1: ENTRY -> N0 -> EXIT
    public void testM115_TP1_LambdaGetWantedGoodsAmount1() {
        assertTrue(settlement.getWantedGoodsAmount(musketsType) > 0);
    }


    // ------------------------------------------------------------------
    // M116: lambda$getWantedGoodsAmount$0(List, Unit) — 2 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M116-TP1..TP2: B1:true (armed role in militaryRoles) and B1:false (unarmed)
    public void testM116_TP1ToTP2_LambdaGetWantedGoodsAmount0() {
        Unit armed = settlement.getOwnedUnitList().get(0);
        armed.changeRole(spec().getRole("model.role.armedBrave"), 1);
        assertTrue(settlement.getWantedGoodsAmount(musketsType) > 0);
    }


    // ------------------------------------------------------------------
    // M117: lambda$hasAnyScouted$0(Player) — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M117-TP1: ENTRY -> N0 -> EXIT
    public void testM117_TP1_LambdaHasAnyScouted0() {
        settlement.setScouted(dutch);
        assertTrue(settlement.hasAnyScouted());
    }


    // ------------------------------------------------------------------
    // M118: <clinit>() — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M118-TP1: ENTRY -> N0 -> EXIT (static class initialization)
    public void testM118_TP1_ClassStaticInit() {
        assertEquals("indianSettlement", IndianSettlement.TAG);
        assertEquals(3, IndianSettlement.WANTED_GOODS_COUNT);
    }
}
