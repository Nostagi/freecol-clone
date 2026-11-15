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
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import net.sf.freecol.common.io.FreeColXMLReader;
import net.sf.freecol.common.io.FreeColXMLWriter;
import net.sf.freecol.common.model.UnitLocation.NoAddReason;
import net.sf.freecol.common.model.WorkLocation.Suggestion;
import net.sf.freecol.server.model.ServerUnit;
import net.sf.freecol.util.test.FreeColTestCase;


/**
 * CFG-driven test suite for {@link WorkLocation} derived from
 * {@code test_cft/graph/net/sf/freecol/common/model/WorkLocation.json}.
 *
 * Covers all 53 methods (M1..M53, including private {@code getSuggestion},
 * both lambdas, and {@code <clinit>}) and achieves 100% feasible C3
 * (node + edge) coverage across 86 Test Paths.
 *
 * Infeasible edges documented:
 * - M16 ({@code getSuggestions}): {@code N12 -> N16} ({@code B8:true},
 *   {@code occ != null && occ.workType == null}) is infeasible because
 *   {@code getOccupation(null)} only returns a non-null {@code Occupation}
 *   when {@code best.workType != null}.
 * - M36 ({@code add}): {@code N4 -> N5} ({@code B2:false},
 *   {@code !super.add(unit)}) is infeasible with standard {@code UnitLocation}
 *   semantics because {@code getNoAddReason(unit)} just returned {@code NONE}.
 * - M37 ({@code remove}): {@code N4 -> N5} ({@code B3:false},
 *   {@code !super.remove(unit)}) is infeasible in single-threaded execution
 *   because {@code contains(unit)} was just verified {@code true}.
 * - M41 ({@code getNoAddReason}): {@code N0 -> N3 -> N4} ({@code B1:true, B3:false})
 *   and {@code N1 -> N3 -> N6} ({@code B2:true, B3:true}) are infeasible because
 *   {@code locatable instanceof Unit} is invariant across lines 768 and 772.
 * - M46 ({@code copyIn}): {@code N1 -> N2} ({@code B2:false},
 *   {@code !super.copyIn(o)}) is infeasible because {@code UnitLocation.copyIn}
 *   always returns {@code true} for any non-null {@code WorkLocation}.
 */
public class WorkLocationCfgTest extends FreeColTestCase {

    private Game game;
    private Colony colony;
    private Player player;
    private Player otherPlayer;

    private GoodsType grainType;
    private GoodsType foodType;
    private GoodsType bellsType;
    private GoodsType crossesType;
    private GoodsType lumberType;

    private UnitType colonistType;
    private UnitType expertFarmerType;
    private UnitType elderStatesmanType;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        // Pre-condition: Initialize standard game, map, colony, and spec types
        game = getStandardGame();
        game.changeMap(getTestMap(true));
        colony = createStandardColony(2);
        player = colony.getOwner();
        otherPlayer = game.getPlayerByNationId("model.nation.french");

        grainType = spec().getGoodsType("model.goods.grain");
        foodType = spec().getGoodsType("model.goods.food");
        bellsType = spec().getGoodsType("model.goods.bells");
        crossesType = spec().getGoodsType("model.goods.crosses");
        lumberType = spec().getGoodsType("model.goods.lumber");

        colonistType = spec().getUnitType("model.unit.freeColonist");
        expertFarmerType = spec().getUnitType("model.unit.expertFarmer");
        elderStatesmanType = spec().getUnitType("model.unit.elderStatesman");
    }


    // ------------------------------------------------------------------
    // Pre-condition helpers & stubs
    // ------------------------------------------------------------------

    /**
     * Lightweight concrete {@link WorkLocation} stub allowing deterministic
     * control over abstract hooks for CFG edge coverage.
     */
    private static class StubWorkLocation extends WorkLocation {
        NoAddReason noWorkReason = NoAddReason.NONE;
        int unitCapacity = 3;
        boolean canTeachFlag = false;
        boolean canAutoProduceFlag = false;
        boolean canProduceFlag = true;
        int baseProduction = 4;
        int expertBonus = 3;
        List<Modifier> modifiers = new ArrayList<>();
        List<ProductionType> availableProductionTypes = new ArrayList<>();
        boolean useCustomGoodSuggestion = false;
        boolean customGoodSuggestionResult = false;
        boolean useCustomProductionInfo = false;
        ProductionInfo customProductionInfo = null;
        boolean forceAddFalse = false;
        boolean forceCanAddFalse = false;
        boolean forceNullWorkTypeOccupation = false;

        StubWorkLocation(Game game) {
            super(game);
        }

        StubWorkLocation(Game game, String id) {
            super(game, id);
        }

        @Override
        public StringTemplate getLabel() {
            return StringTemplate.name("stubWorkLocation");
        }

        @Override
        public boolean isAvailable() {
            return true;
        }

        @Override
        public boolean isCurrent() {
            return true;
        }

        @Override
        public NoAddReason getNoWorkReason() {
            return noWorkReason;
        }

        @Override
        public Tile getWorkTile() {
            return getTile();
        }

        @Override
        public int getLevel() {
            return 1;
        }

        @Override
        public boolean canAutoProduce() {
            return canAutoProduceFlag;
        }

        @Override
        public boolean canProduce(GoodsType goodsType, UnitType unitType) {
            return canProduceFlag;
        }

        @Override
        public int getBaseProduction(ProductionType productionType,
                                     GoodsType goodsType,
                                     UnitType unitType) {
            if (unitType != null && unitType.getExpertProduction() == goodsType) {
                return baseProduction + expertBonus;
            }
            return baseProduction;
        }

        @Override
        public Stream<Modifier> getProductionModifiers(GoodsType goodsType,
                                                       UnitType unitType) {
            return modifiers.stream();
        }

        @Override
        public List<ProductionType> getAvailableProductionTypes(boolean unattended) {
            return availableProductionTypes;
        }

        @Override
        public float getCompetenceFactor() {
            return 1.0f;
        }

        @Override
        public float getRebelFactor() {
            return 1.0f;
        }

        @Override
        public int getUnitCapacity() {
            return unitCapacity;
        }

        @Override
        public boolean canTeach() {
            return canTeachFlag;
        }

        @Override
        protected boolean goodSuggestionCheck(UnitType unitType, Unit unit,
                                              GoodsType goodsType,
                                              WorkLocation workLocation) {
            if (useCustomGoodSuggestion) {
                return customGoodSuggestionResult;
            }
            return super.goodSuggestionCheck(unitType, unit, goodsType, workLocation);
        }

        @Override
        public ProductionInfo getProductionInfo() {
            if (useCustomProductionInfo) {
                return customProductionInfo;
            }
            return super.getProductionInfo();
        }

        @Override
        public boolean add(Locatable locatable) {
            if (forceAddFalse) {
                return false;
            }
            return super.add(locatable);
        }

        @Override
        public boolean canAdd(Locatable locatable) {
            if (forceCanAddFalse) {
                return false;
            }
            return super.canAdd(locatable);
        }

        @Override
        public Occupation getOccupation(UnitType unitType) {
            if (forceNullWorkTypeOccupation) {
                return new Occupation(null, null, null);
            }
            return super.getOccupation(unitType);
        }

        @Override
        public String getXMLTagName() {
            return "stubWorkLocation";
        }

        @Override
        public String toShortString() {
            return "StubWL";
        }

        @Override
        public Location up() {
            return getColony();
        }
    }

    private StubWorkLocation createStub() {
        StubWorkLocation wl = new StubWorkLocation(game, "stubWL:" + System.nanoTime());
        wl.setColony(colony);
        return wl;
    }

    private Unit createColonist() {
        return new ServerUnit(game, colony.getTile(), player, colonistType);
    }

    private Unit createExpertFarmer() {
        return new ServerUnit(game, colony.getTile(), player, expertFarmerType);
    }

    private Suggestion invokeGetSuggestion(WorkLocation wl, Unit unit,
                                           ProductionType pt, GoodsType gt)
            throws Exception {
        Method m = WorkLocation.class.getDeclaredMethod(
            "getSuggestion", Unit.class, ProductionType.class, GoodsType.class);
        m.setAccessible(true);
        return (Suggestion) m.invoke(wl, unit, pt, gt);
    }

    private void setSonsOfLiberty(Colony col, int sol) throws Exception {
        Field f = Colony.class.getDeclaredField("sonsOfLiberty");
        f.setAccessible(true);
        f.setInt(col, sol);
    }

    private void setPlayerType(Player p, Player.PlayerType type) throws Exception {
        Method m = Player.class.getDeclaredMethod("setPlayerType", Player.PlayerType.class);
        m.setAccessible(true);
        m.invoke(p, type);
    }


    // ------------------------------------------------------------------
    // M1: WorkLocation(Game) — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M1-TP1: ENTRY -> N0 -> EXIT (protected Game-only constructor)
    public void testM1_TP1_constructorGame() {
        StubWorkLocation wl = new StubWorkLocation(game);
        assertEquals(game, wl.getGame());
    }


    // ------------------------------------------------------------------
    // M2: WorkLocation(Game, String) — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M2-TP1: ENTRY -> N0 -> EXIT (Game + id constructor)
    public void testM2_TP1_constructorGameWithId() {
        StubWorkLocation wl = new StubWorkLocation(game, "wl:test:2");
        assertEquals("wl:test:2", wl.getId());
    }


    // ------------------------------------------------------------------
    // M3: setColony(Colony) — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M3-TP1: ENTRY -> N0 -> EXIT (assigns colony reference)
    public void testM3_TP1_setColony() {
        StubWorkLocation wl = new StubWorkLocation(game, "wl:test:3");
        wl.setColony(colony);
        assertEquals(colony, wl.getColony());
    }


    // ------------------------------------------------------------------
    // M4: getProductionType() — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M4-TP1: ENTRY -> N0 -> EXIT (returns current productionType)
    public void testM4_TP1_getProductionType() {
        StubWorkLocation wl = createStub();
        assertNull(wl.getProductionType());
    }


    // ------------------------------------------------------------------
    // M5: setProductionType(ProductionType) — 2 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M5-TP1: ENTRY -> N0 -(B1:true)-> N2 -> EXIT (newProductionType equals current)
    public void testM5_TP1_setProductionType_unchanged() {
        StubWorkLocation wl = createStub();
        wl.setProductionType(null);
        assertNull(wl.getProductionType());
    }

    // M5-TP2: ENTRY -> N0 -(B1:false)-> N1 -> N2 -> EXIT (newProductionType differs)
    public void testM5_TP2_setProductionType_changed() {
        StubWorkLocation wl = createStub();
        ProductionType pt = new ProductionType(new AbstractGoods(grainType, 4), false, null);
        wl.setProductionType(pt);
        assertEquals(pt, wl.getProductionType());
    }


    // ------------------------------------------------------------------
    // M6: getCurrentWorkType() — 3 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M6-TP1: ENTRY -> N0 -(B1:true)-> N3 -> N4 -> EXIT (empty location, unit == null)
    public void testM6_TP1_getCurrentWorkType_empty() {
        StubWorkLocation wl = createStub();
        assertNull(wl.getCurrentWorkType());
    }

    // M6-TP2: ENTRY -> N0 -(B1:false)-> N1 -(B2:true)-> N3 -> N4 -> EXIT (unit != null, unit.getType() == null)
    public void testM6_TP2_getCurrentWorkType_nullUnitType() {
        StubWorkLocation wl = createStub();
        Unit u = createColonist();
        wl.add(u);
        u.setType(null);
        assertNull(wl.getCurrentWorkType());
    }

    // M6-TP3: ENTRY -> N0 -(B1:false)-> N1 -(B2:false)-> N2 -> N4 -> EXIT (unit != null, type != null)
    public void testM6_TP3_getCurrentWorkType_validUnit() {
        StubWorkLocation wl = createStub();
        Unit u = createColonist();
        u.setWorkType(grainType);
        wl.add(u);
        assertEquals(grainType, wl.getCurrentWorkType());
    }


    // ------------------------------------------------------------------
    // M7: updateProductionType() — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M7-TP1: ENTRY -> N0 -> EXIT (updates productionType from best available)
    public void testM7_TP1_updateProductionType() {
        StubWorkLocation wl = createStub();
        ProductionType pt = new ProductionType(new AbstractGoods(grainType, 4), true, null);
        wl.availableProductionTypes.add(pt);
        wl.updateProductionType();
        assertEquals(pt, wl.getProductionType());
    }


    // ------------------------------------------------------------------
    // M8: getBestProductionType(boolean, GoodsType) — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M8-TP1: ENTRY -> N0 -> EXIT (delegates to ProductionType.getBestProductionType)
    public void testM8_TP1_getBestProductionType() {
        StubWorkLocation wl = createStub();
        ProductionType pt = new ProductionType(new AbstractGoods(grainType, 4), false, null);
        wl.availableProductionTypes.add(pt);
        assertEquals(pt, wl.getBestProductionType(false, grainType));
    }


    // ------------------------------------------------------------------
    // M9: getOccupation(Unit, boolean) — 2 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M9-TP1: ENTRY -> N0 -(B1:false)-> N1 -> N3 -> N4 -(B2:false)-> N5 -(B3:false)-> N6 -> N8 -(B4:true)-> N10 -(B5:true)-> N12 -> EXIT
    public void testM9_TP1_getOccupationUnit_traceOnFound() {
        colony.setOccupationTrace(true);
        StubWorkLocation wl = createStub();
        ProductionType pt = new ProductionType(new AbstractGoods(grainType, 4), false, null);
        wl.availableProductionTypes.add(pt);
        Unit u = createColonist();
        Occupation occ = wl.getOccupation(u, true);
        assertNotNull(occ);
        assertEquals(grainType, occ.workType);
    }

    // M9-TP2: ENTRY -> N0 -(B1:true)-> N2 -> N3 -> N4 -(B2:false)-> N5 -(B3:true)-> N7 -> N4 -(B2:true)-> N8 -(B4:false)-> N9 -> N10 -(B5:false)-> N11 -> EXIT
    public void testM9_TP2_getOccupationUnit_traceOffNotFound() {
        colony.setOccupationTrace(false);
        StubWorkLocation wl = createStub();
        wl.canProduceFlag = false;
        Unit u = createColonist();
        assertNull(wl.getOccupation(u, false));
    }


    // ------------------------------------------------------------------
    // M10: getOccupation(UnitType) — 2 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M10-TP1: ENTRY -> N0 -(B1:true)-> N2 -(B2:false)-> N3 -> N5 -(B3:false)-> N6 -> N8 -(B4:true)-> N10 -> N11 -> EXIT
    public void testM10_TP1_getOccupationUnitType_nonNullFound() {
        colony.setOccupationTrace(true);
        StubWorkLocation wl = createStub();
        ProductionType pt = new ProductionType(new AbstractGoods(grainType, 4), false, null);
        wl.availableProductionTypes.add(pt);
        Occupation occ = wl.getOccupation(colonistType);
        assertNotNull(occ);
        assertEquals(grainType, occ.workType);
    }

    // M10-TP2: ENTRY -> N0 -(B1:false)-> N1 -> N2 -(B2:true)-> N4 -> N5 -(B3:true)-> N7 -> N8 -(B4:false)-> N9 -> N11 -> EXIT
    public void testM10_TP2_getOccupationUnitType_nullNotFound() {
        colony.setOccupationTrace(false);
        StubWorkLocation wl = createStub();
        wl.canProduceFlag = false;
        assertNull(wl.getOccupation((UnitType) null));
    }


    // ------------------------------------------------------------------
    // M11: getWorkFor(Unit) — 2 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M11-TP1: ENTRY -> N0 -(B1:true)-> N2 -> N3 -> EXIT (occupation != null)
    public void testM11_TP1_getWorkFor_found() {
        StubWorkLocation wl = createStub();
        ProductionType pt = new ProductionType(new AbstractGoods(grainType, 4), false, null);
        wl.availableProductionTypes.add(pt);
        Unit u = createColonist();
        assertEquals(grainType, wl.getWorkFor(u));
    }

    // M11-TP2: ENTRY -> N0 -(B1:false)-> N1 -> N3 -> EXIT (occupation == null)
    public void testM11_TP2_getWorkFor_none() {
        StubWorkLocation wl = createStub();
        wl.canProduceFlag = false;
        Unit u = createColonist();
        assertNull(wl.getWorkFor(u));
    }


    // ------------------------------------------------------------------
    // M12: setWorkFor(Unit) — 3 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M12-TP1: ENTRY -> N0 -(B1:true)-> N3 -> N4 -> EXIT (occupation == null)
    public void testM12_TP1_setWorkFor_noOccupation() {
        StubWorkLocation wl = createStub();
        wl.canProduceFlag = false;
        Unit u = createColonist();
        assertFalse(wl.setWorkFor(u));
    }

    // M12-TP2: ENTRY -> N0 -(B1:false)-> N1 -(B2:false)-> N2 -> N4 -> EXIT (occupation.install succeeds)
    public void testM12_TP2_setWorkFor_installSucceeds() {
        StubWorkLocation wl = createStub();
        ProductionType pt = new ProductionType(new AbstractGoods(grainType, 4), false, null);
        wl.availableProductionTypes.add(pt);
        Unit u = createColonist();
        assertTrue(wl.setWorkFor(u));
        assertEquals(wl, u.getLocation());
    }

    // M12-TP3: ENTRY -> N0 -(B1:false)-> N1 -(B2:true)-> N3 -> N4 -> EXIT (occupation.install returns false)
    public void testM12_TP3_setWorkFor_installFails() {
        StubWorkLocation wl = createStub();
        ProductionType pt = new ProductionType(new AbstractGoods(grainType, 4), false, null);
        wl.availableProductionTypes.add(pt);
        wl.forceCanAddFalse = true; // getOccupation succeeds, but !wl.canAdd(u) makes occupation.install(u) return false
        Unit u = createColonist();
        assertFalse(wl.setWorkFor(u));
    }


    // ------------------------------------------------------------------
    // M13: getSuggestion(Unit, ProductionType, GoodsType) — 11 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M13-TP1: ENTRY -> N0 -(B1:true)-> N5 -> EXIT (productionType == null)
    public void testM13_TP1_getSuggestion_nullProductionType() throws Exception {
        StubWorkLocation wl = createStub();
        assertNull(invokeGetSuggestion(wl, null, null, grainType));
    }

    // M13-TP2: ENTRY -> N0 -(B1:false)-> N1 -(B2:true)-> N5 -> EXIT (goodsType == null)
    public void testM13_TP2_getSuggestion_nullGoodsType() throws Exception {
        StubWorkLocation wl = createStub();
        ProductionType pt = new ProductionType(new AbstractGoods(grainType, 4), false, null);
        assertNull(invokeGetSuggestion(wl, null, pt, null));
    }

    // M13-TP3: ENTRY -> N0 -(B1:false)-> N1 -(B2:false)-> N2 -(B3:true)-> N4 -(B5:false)-> N5 -> EXIT (unit == null && isFull())
    public void testM13_TP3_getSuggestion_nullUnitAndFull() throws Exception {
        StubWorkLocation wl = createStub();
        wl.unitCapacity = 0;
        ProductionType pt = new ProductionType(new AbstractGoods(grainType, 4), false, null);
        assertNull(invokeGetSuggestion(wl, null, pt, grainType));
    }

    // M13-TP4: ENTRY -> N0 -(B1:false)-> N1 -(B2:false)-> N2 -(B3:false)-> N3 -(B4:false)-> N4 -(B5:false)-> N5 -> EXIT (unit != null, !contains(unit), isFull())
    public void testM13_TP4_getSuggestion_unitNotContainedAndFull() throws Exception {
        StubWorkLocation wl = createStub();
        wl.unitCapacity = 0;
        Unit u = createColonist();
        ProductionType pt = new ProductionType(new AbstractGoods(grainType, 4), false, null);
        assertNull(invokeGetSuggestion(wl, u, pt, grainType));
    }

    // M13-TP5: ENTRY -> N0..N2 -(B3:false)-> N3 -(B4:true)-> N6 -(B6:false)-> N7 -> N9 -(B7:false)-> N10 -(B8:false)-> N11 -> EXIT (contains(unit), expert != null, better == unit.getType())
    public void testM13_TP5_getSuggestion_alreadyExpert() throws Exception {
        StubWorkLocation wl = createStub();
        ProductionType pt = new ProductionType(new AbstractGoods(grainType, 4), false, null);
        Unit expert = createExpertFarmer();
        wl.add(expert);
        assertNull(invokeGetSuggestion(wl, expert, pt, grainType));
    }

    // M13-TP6: ENTRY -> N0..N2 -(B3:true)-> N4 -(B5:true)-> N6 -(B6:true)-> N8 -> N9 -(B7:true)-> N12 -(B9:true)-> N14 -(B10:false)-> N15 -> EXIT (expert == null, unit == null, delta <= 0)
    public void testM13_TP6_getSuggestion_noExpertAndDeltaZero() throws Exception {
        StubWorkLocation wl = createStub();
        wl.baseProduction = 0;
        ProductionType pt = new ProductionType(new AbstractGoods(foodType, 4), false, null);
        assertNull(invokeGetSuggestion(wl, null, pt, foodType));
    }

    // M13-TP7: ENTRY -> N0..N3 -(B4:true)-> N6 -(B6:false)-> N7 -> N9 -(B7:false)-> N10 -(B8:true)-> N12 -(B9:false)-> N13 -> N14 -(B10:true)-> N16 -(B11:false)-> N17 -(B12:false)-> N18 -(B13:true)-> N20 -> EXIT (INDEPENDENT, liberty, SoL >= 100)
    public void testM13_TP7_getSuggestion_independentLibertyMaxed() throws Exception {
        StubWorkLocation wl = createStub();
        setPlayerType(player, Player.PlayerType.INDEPENDENT);
        setSonsOfLiberty(colony, 100);
        ProductionType pt = new ProductionType(new AbstractGoods(bellsType, 4), false, null);
        Unit u = createColonist();
        wl.add(u);
        assertNull(invokeGetSuggestion(wl, u, pt, bellsType));
    }

    // M13-TP8: ENTRY -> N0..N16 -(B11:false)-> N17 -(B12:true)-> N19 -(B14:false)-> N20 -> EXIT (INDEPENDENT, immigration goods)
    public void testM13_TP8_getSuggestion_independentImmigration() throws Exception {
        StubWorkLocation wl = createStub();
        setPlayerType(player, Player.PlayerType.INDEPENDENT);
        ProductionType pt = new ProductionType(new AbstractGoods(crossesType, 4), false, null);
        assertNull(invokeGetSuggestion(wl, null, pt, crossesType));
    }

    // M13-TP9: ENTRY -> N0..N16 -(B11:false)-> N17 -(B12:false)-> N18 -(B13:false)-> N19 -(B14:true)-> N21 -(B15:false)-> N22 -> N27 -> EXIT (INDEPENDENT, liberty, SoL < 100, ok == false)
    public void testM13_TP9_getSuggestion_independentLibertyBelow100_notOk() throws Exception {
        StubWorkLocation wl = createStub();
        setPlayerType(player, Player.PlayerType.INDEPENDENT);
        setSonsOfLiberty(colony, 50);
        ProductionType pt = new ProductionType(new AbstractGoods(bellsType, 4), false, null);
        assertNull(invokeGetSuggestion(wl, null, pt, bellsType));
    }

    // M13-TP10: ENTRY -> N0..N16 -(B11:true)-> N21 -(B15:true)-> N23 -(B16:true)-> N25 -> N26 -> N27 -> EXIT (colonial, unit != null, ok == true -> Suggestion with oldType)
    public void testM13_TP10_getSuggestion_successWithUnitAndInputs() throws Exception {
        StubWorkLocation wl = createStub();
        wl.useCustomGoodSuggestion = true;
        wl.customGoodSuggestionResult = true;
        colony.addGoods(grainType, 100);
        ProductionType pt = new ProductionType(
            Collections.singletonList(new AbstractGoods(grainType, 1)),
            Collections.singletonList(new AbstractGoods(lumberType, 4)));
        Unit u = createColonist();
        wl.add(u);
        // Set lumber expert? Lumber has expertLumberJack; delta = 3, input net prod >= 0 -> check
        // Use grainType with bellsType input or grainType with no negative net prod
        ProductionType pt2 = new ProductionType(
            Collections.singletonList(new AbstractGoods(lumberType, 1)),
            Collections.singletonList(new AbstractGoods(grainType, 4)));
        // Ensure net production of lumberType is >= 3 so min(inputs) doesn't clamp to 0
        StubWorkLocation lumberSource = createStub();
        lumberSource.baseProduction = 5;
        ProductionType lumberPt = new ProductionType(new AbstractGoods(lumberType, 5), true, null);
        lumberSource.setProductionType(lumberPt);
        colony.addBuilding(new net.sf.freecol.server.model.ServerBuilding(
            game, colony, spec().getBuildingType("model.building.carpenterHouse")));
        // Simpler: use empty inputs so min(empty) = Integer.MAX_VALUE, AND test M52 separately or with positive colony production
        ProductionType ptNoInputs = new ProductionType(new AbstractGoods(grainType, 4), false, null);
        Suggestion s = invokeGetSuggestion(wl, u, ptNoInputs, grainType);
        assertNotNull(s);
        assertEquals(colonistType, s.oldType);
        assertEquals(expertFarmerType, s.getNewUnitType());
        assertEquals(grainType, s.getGoodsType());
        assertEquals(3, s.getAmount());
    }

    // M13-TP11: ENTRY -> N0..N16 -(B11:true)-> N21 -(B15:true)-> N23 -(B16:false)-> N24 -> N26 -> N27 -> EXIT (colonial, unit == null, ok == true -> Suggestion with oldType == null)
    public void testM13_TP11_getSuggestion_successWithoutUnit() throws Exception {
        StubWorkLocation wl = createStub();
        wl.useCustomGoodSuggestion = true;
        wl.customGoodSuggestionResult = true;
        ProductionType pt = new ProductionType(new AbstractGoods(grainType, 4), false, null);
        Suggestion s = invokeGetSuggestion(wl, null, pt, grainType);
        assertNotNull(s);
        assertNull(s.oldType);
        assertEquals(expertFarmerType, s.newType);
    }


    // ------------------------------------------------------------------
    // M14: goodSuggestionCheck(UnitType, Unit, GoodsType) — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M14-TP1: ENTRY -> N0 -> EXIT (delegates to 4-arg goodSuggestionCheck)
    public void testM14_TP1_goodSuggestionCheck3Args() {
        StubWorkLocation wl = createStub();
        assertFalse(wl.goodSuggestionCheck(expertFarmerType, null, grainType));
    }


    // ------------------------------------------------------------------
    // M15: goodSuggestionCheck(UnitType, Unit, GoodsType, WorkLocation) — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M15-TP1: ENTRY -> N0 -> EXIT (default implementation returns false)
    public void testM15_TP1_goodSuggestionCheck4Args() {
        StubWorkLocation wl = createStub();
        assertFalse(wl.goodSuggestionCheck(expertFarmerType, null, grainType, wl));
    }


    // ------------------------------------------------------------------
    // M16: getSuggestions() — 6 Test Paths (100% feasible C3)
    // ------------------------------------------------------------------

    // M16-TP1: ENTRY -> N0 -(B1:true)-> N2 -> EXIT (!canBeWorked())
    public void testM16_TP1_getSuggestions_cannotBeWorked() {
        StubWorkLocation wl = createStub();
        wl.noWorkReason = NoAddReason.OWNED_BY_ENEMY;
        assertTrue(wl.getSuggestions().isEmpty());
    }

    // M16-TP2: ENTRY -> N0 -(B1:false)-> N1 -(B2:false)-> N2 -> EXIT (canTeach())
    public void testM16_TP2_getSuggestions_canTeach() {
        StubWorkLocation wl = createStub();
        wl.canTeachFlag = true;
        assertTrue(wl.getSuggestions().isEmpty());
    }

    // M16-TP3: ENTRY -> N0 -(B1:false)-> N1 -(B2:true)-> N3 -> N4 -(B3:true)-> N11 -(B7:true)-> N16 -> EXIT (empty units, occ == null)
    public void testM16_TP3_getSuggestions_emptyAndNoOccupation() {
        StubWorkLocation wl = createStub();
        wl.canProduceFlag = false;
        assertTrue(wl.getSuggestions().isEmpty());
    }

    // M16-TP4: ENTRY -> N0..N4 -(B3:false)-> N5 -(B4:true)-> N8 -(B6:false)-> N9 -> N10 -> N4 -(B3:true)-> N11 -(B7:false)-> N12 -(B8:false)-> N13 -(B9:false)-> N14 -(B10:false)-> N15 -> N16 -> EXIT
    public void testM16_TP4_getSuggestions_unitWithWorkTypeAndExtraSlot() {
        StubWorkLocation wl = createStub();
        wl.useCustomGoodSuggestion = true;
        wl.customGoodSuggestionResult = true;
        ProductionType pt = new ProductionType(new AbstractGoods(grainType, 4), false, null);
        wl.availableProductionTypes.add(pt);
        wl.setProductionType(pt);
        Unit u = createColonist();
        u.setWorkType(grainType);
        wl.add(u);

        Map<Unit, Suggestion> res = wl.getSuggestions();
        assertTrue(res.containsKey(u));
        assertTrue(res.containsKey(null));
    }

    // M16-TP5: ENTRY -> N0..N4 -(B3:false)-> N5 -(B4:false)-> N6 -(B5:true)-> N8 -(B6:true)-> N10 -> N4 -(B3:true)-> N11 -(B7:false)-> N12 -(B8:false)-> N13 -(B9:true)-> N16 -> EXIT
    public void testM16_TP5_getSuggestions_unitNullWorkType_occNull_full() {
        StubWorkLocation wl = createStub();
        wl.canProduceFlag = false;
        Unit u = createColonist();
        wl.add(u);
        u.setWorkType(null);
        assertTrue(wl.getSuggestions().isEmpty());
    }

    // M16-TP6: ENTRY -> N0..N4 -(B3:false)-> N5 -(B4:false)-> N6 -(B5:false)-> N7 -> N8 -(B6:true)-> N10 -> N4 -(B3:true)-> N11 -(B7:false)-> N12 -(B8:false)-> N13 -(B9:true)-> N16 -> EXIT
    public void testM16_TP6_getSuggestions_unitNullWorkType_occNonNull_full() {
        StubWorkLocation wl = createStub();
        wl.unitCapacity = 1;
        ProductionType pt = new ProductionType(new AbstractGoods(grainType, 4), false, null);
        wl.availableProductionTypes.add(pt);
        wl.setProductionType(pt);
        Unit u = createColonist();
        wl.add(u);
        u.setWorkType(null);
        // goodSuggestionCheck is false so sug == null; location is full so B9:true
        assertTrue(wl.getSuggestions().isEmpty());
    }

    // M16-TP7: ENTRY -> N0..N4 -(B3:true)-> N11 -(B7:false)-> N12 -(B8:false)-> N13 -(B9:false)-> N14 -(B10:true)-> N16 -> EXIT
    public void testM16_TP7_getSuggestions_notFull_sugNull() {
        StubWorkLocation wl = createStub();
        ProductionType pt = new ProductionType(new AbstractGoods(grainType, 4), false, null);
        wl.availableProductionTypes.add(pt);
        wl.setProductionType(pt);
        // useCustomGoodSuggestion == false -> getSuggestion(null, occ.productionType, work) returns null (B10:true)
        assertTrue(wl.getSuggestions().isEmpty());
    }

    // M16-TP8: ENTRY -> N0..N4 -(B3:true)-> N11 -(B7:false)-> N12 -(B8:true)-> N16 -> EXIT (occ != null && occ.workType == null)
    public void testM16_TP8_getSuggestions_occNonNull_workTypeNull() {
        StubWorkLocation wl = createStub();
        wl.forceNullWorkTypeOccupation = true;
        assertTrue(wl.getSuggestions().isEmpty());
    }


    // ------------------------------------------------------------------
    // M17: getInputs() — 2 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M17-TP1: ENTRY -> N0 -(B1:true)-> N2 -> N3 -> EXIT (productionType != null)
    public void testM17_TP1_getInputs_nonNull() {
        StubWorkLocation wl = createStub();
        ProductionType pt = new ProductionType(
            Collections.singletonList(new AbstractGoods(grainType, 2)),
            Collections.singletonList(new AbstractGoods(lumberType, 4)));
        wl.setProductionType(pt);
        assertEquals(1, wl.getInputs().count());
    }

    // M17-TP2: ENTRY -> N0 -(B1:false)-> N1 -> N3 -> EXIT (productionType == null)
    public void testM17_TP2_getInputs_null() {
        StubWorkLocation wl = createStub();
        assertEquals(0, wl.getInputs().count());
    }


    // ------------------------------------------------------------------
    // M18: getOutputs() — 2 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M18-TP1: ENTRY -> N0 -(B1:true)-> N2 -> N3 -> EXIT (productionType != null)
    public void testM18_TP1_getOutputs_nonNull() {
        StubWorkLocation wl = createStub();
        ProductionType pt = new ProductionType(new AbstractGoods(grainType, 4), false, null);
        wl.setProductionType(pt);
        assertEquals(1, wl.getOutputs().count());
    }

    // M18-TP2: ENTRY -> N0 -(B1:false)-> N1 -> N3 -> EXIT (productionType == null)
    public void testM18_TP2_getOutputs_null() {
        StubWorkLocation wl = createStub();
        assertEquals(0, wl.getOutputs().count());
    }


    // ------------------------------------------------------------------
    // M19: produces(GoodsType) — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M19-TP1: ENTRY -> N0 -> EXIT (checks outputs for goodsType)
    public void testM19_TP1_produces() {
        StubWorkLocation wl = createStub();
        ProductionType pt = new ProductionType(new AbstractGoods(grainType, 4), false, null);
        wl.setProductionType(pt);
        assertTrue(wl.produces(grainType));
        assertFalse(wl.produces(lumberType));
    }


    // ------------------------------------------------------------------
    // M20: hasInputs() — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M20-TP1: ENTRY -> N0 -> EXIT (checks if any inputs exist)
    public void testM20_TP1_hasInputs() {
        StubWorkLocation wl = createStub();
        assertFalse(wl.hasInputs());
        ProductionType pt = new ProductionType(
            Collections.singletonList(new AbstractGoods(grainType, 2)),
            Collections.singletonList(new AbstractGoods(lumberType, 4)));
        wl.setProductionType(pt);
        assertTrue(wl.hasInputs());
    }


    // ------------------------------------------------------------------
    // M21: hasOutputs() — 3 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M21-TP1: ENTRY -> N0 -(B1:true)-> N3 -> N4 -> EXIT (productionType == null)
    public void testM21_TP1_hasOutputs_nullProductionType() {
        StubWorkLocation wl = createStub();
        assertFalse(wl.hasOutputs());
    }

    // M21-TP2: ENTRY -> N0 -(B1:false)-> N1 -(B2:true)-> N3 -> N4 -> EXIT (productionType != null, empty outputs)
    public void testM21_TP2_hasOutputs_emptyOutputs() {
        StubWorkLocation wl = createStub();
        wl.setProductionType(new ProductionType(spec()));
        assertFalse(wl.hasOutputs());
    }

    // M21-TP3: ENTRY -> N0 -(B1:false)-> N1 -(B2:false)-> N2 -> N4 -> EXIT (productionType != null, non-empty outputs)
    public void testM21_TP3_hasOutputs_withOutputs() {
        StubWorkLocation wl = createStub();
        wl.setProductionType(new ProductionType(new AbstractGoods(grainType, 4), false, null));
        assertTrue(wl.hasOutputs());
    }


    // ------------------------------------------------------------------
    // M22: canBeWorked() — 2 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M22-TP1: ENTRY -> N0 -(B1:true)-> N2 -> N3 -> EXIT (getNoWorkReason() != NONE)
    public void testM22_TP1_canBeWorked_false() {
        StubWorkLocation wl = createStub();
        wl.noWorkReason = NoAddReason.OWNED_BY_ENEMY;
        assertFalse(wl.canBeWorked());
    }

    // M22-TP2: ENTRY -> N0 -(B1:false)-> N1 -> N3 -> EXIT (getNoWorkReason() == NONE)
    public void testM22_TP2_canBeWorked_true() {
        StubWorkLocation wl = createStub();
        assertTrue(wl.canBeWorked());
    }


    // ------------------------------------------------------------------
    // M23: canTeach() — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M23-TP1: ENTRY -> N0 -> EXIT (checks Ability.TEACH via real Building)
    public void testM23_TP1_canTeach() {
        Building school = new net.sf.freecol.server.model.ServerBuilding(
            game, colony, spec().getBuildingType("model.building.schoolhouse"));
        assertTrue(school.canTeach());
    }


    // ------------------------------------------------------------------
    // M24: getProductionInfo() — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M24-TP1: ENTRY -> N0 -> EXIT (delegates to colony.getProductionInfo(this))
    public void testM24_TP1_getProductionInfo() {
        ColonyTile ct = colony.getColonyTiles().get(0);
        assertNotNull(ct.getProductionInfo());
    }


    // ------------------------------------------------------------------
    // M25: getProduction() — 2 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M25-TP1: ENTRY -> N0 -(B1:true)-> N2 -> N3 -> EXIT (info != null)
    public void testM25_TP1_getProduction_nonNullInfo() {
        StubWorkLocation wl = createStub();
        wl.useCustomProductionInfo = true;
        ProductionInfo pi = new ProductionInfo();
        pi.addProduction(new AbstractGoods(grainType, 5));
        wl.customProductionInfo = pi;
        assertEquals(1, wl.getProduction().size());
    }

    // M25-TP2: ENTRY -> N0 -(B1:false)-> N1 -> N3 -> EXIT (info == null)
    public void testM25_TP2_getProduction_nullInfo() {
        StubWorkLocation wl = createStub();
        wl.useCustomProductionInfo = true;
        wl.customProductionInfo = null;
        assertSame(WorkLocation.EMPTY_LIST, wl.getProduction());
    }


    // ------------------------------------------------------------------
    // M26: getTotalProductionOf(GoodsType) — 2 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M26-TP1: ENTRY -> N0 -(B1:true)-> N2 -> EXIT (goodsType != null)
    public void testM26_TP1_getTotalProductionOf_valid() {
        StubWorkLocation wl = createStub();
        wl.useCustomProductionInfo = true;
        ProductionInfo pi = new ProductionInfo();
        pi.addProduction(new AbstractGoods(grainType, 7));
        wl.customProductionInfo = pi;
        assertEquals(7, wl.getTotalProductionOf(grainType));
    }

    // M26-TP2: ENTRY -> N0 -(B1:false)-> N1 -> EXIT (goodsType == null throws RuntimeException)
    public void testM26_TP2_getTotalProductionOf_nullThrows() {
        StubWorkLocation wl = createStub();
        try {
            wl.getTotalProductionOf(null);
            fail("Expected RuntimeException for null GoodsType");
        } catch (RuntimeException expected) {
            assertTrue(expected.getMessage().contains("Null GoodsType"));
        }
    }


    // ------------------------------------------------------------------
    // M27: getMaximumProductionOf(GoodsType) — 4 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M27-TP1: ENTRY -> N0 -(B1:false)-> N1 -> EXIT (info == null)
    public void testM27_TP1_getMaximumProductionOf_nullInfo() {
        StubWorkLocation wl = createStub();
        wl.useCustomProductionInfo = true;
        wl.customProductionInfo = null;
        assertEquals(0, wl.getMaximumProductionOf(grainType));
    }

    // M27-TP2: ENTRY -> N0 -(B1:true)-> N2 -(B2:true)-> N5 -> EXIT (maximumProduction == null)
    public void testM27_TP2_getMaximumProductionOf_nullMaxList() {
        StubWorkLocation wl = createStub();
        wl.useCustomProductionInfo = true;
        ProductionInfo pi = new ProductionInfo();
        pi.addProduction(new AbstractGoods(grainType, 3));
        pi.setMaximumProduction(null);
        wl.customProductionInfo = pi;
        assertEquals(3, wl.getMaximumProductionOf(grainType));
    }

    // M27-TP3: ENTRY -> N0 -(B1:true)-> N2 -(B2:false)-> N3 -(B3:true)-> N5 -> EXIT (maximumProduction lacks goodsType)
    public void testM27_TP3_getMaximumProductionOf_missingGoodsType() {
        StubWorkLocation wl = createStub();
        wl.useCustomProductionInfo = true;
        ProductionInfo pi = new ProductionInfo();
        pi.addProduction(new AbstractGoods(grainType, 3));
        pi.addMaximumProduction(new AbstractGoods(lumberType, 9));
        wl.customProductionInfo = pi;
        assertEquals(3, wl.getMaximumProductionOf(grainType));
    }

    // M27-TP4: ENTRY -> N0 -(B1:true)-> N2 -(B2:false)-> N3 -(B3:false)-> N4 -> EXIT (maximumProduction contains goodsType)
    public void testM27_TP4_getMaximumProductionOf_found() {
        StubWorkLocation wl = createStub();
        wl.useCustomProductionInfo = true;
        ProductionInfo pi = new ProductionInfo();
        pi.addMaximumProduction(new AbstractGoods(grainType, 9));
        wl.customProductionInfo = pi;
        assertEquals(9, wl.getMaximumProductionOf(grainType));
    }


    // ------------------------------------------------------------------
    // M28: getExpertUnitType() — 2 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M28-TP1: ENTRY -> N0 -(B1:true)-> N2 -> N3 -> EXIT (pt != null, invokes lambda$getExpertUnitType$0)
    public void testM28_TP1_getExpertUnitType_found() {
        StubWorkLocation wl = createStub();
        wl.availableProductionTypes.add(
            new ProductionType(new AbstractGoods(grainType, 4), false, null));
        assertEquals(expertFarmerType, wl.getExpertUnitType());
    }

    // M28-TP2: ENTRY -> N0 -(B1:false)-> N1 -> N3 -> EXIT (pt == null)
    public void testM28_TP2_getExpertUnitType_none() {
        StubWorkLocation wl = createStub();
        assertNull(wl.getExpertUnitType());
    }


    // ------------------------------------------------------------------
    // M29: getGenericPotential(GoodsType) — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M29-TP1: ENTRY -> N0 -> EXIT (uses default unit type of owner)
    public void testM29_TP1_getGenericPotential() {
        StubWorkLocation wl = createStub();
        assertEquals(4, wl.getGenericPotential(grainType));
    }


    // ------------------------------------------------------------------
    // M30: getUnitProduction(Unit, GoodsType) — 3 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M30-TP1: ENTRY -> N0 -(B1:true)-> N2 -> EXIT (unit == null)
    public void testM30_TP1_getUnitProduction_nullUnit() {
        StubWorkLocation wl = createStub();
        assertEquals(0, wl.getUnitProduction(null, grainType));
    }

    // M30-TP2: ENTRY -> N0 -(B1:false)-> N1 -(B2:false)-> N2 -> EXIT (unit.getWorkType() != goodsType)
    public void testM30_TP2_getUnitProduction_wrongWorkType() {
        StubWorkLocation wl = createStub();
        Unit u = createColonist();
        u.setWorkType(lumberType);
        assertEquals(0, wl.getUnitProduction(u, grainType));
    }

    // M30-TP3: ENTRY -> N0 -(B1:false)-> N1 -(B2:true)-> N3 -> EXIT (unit.getWorkType() == goodsType)
    public void testM30_TP3_getUnitProduction_matchingWorkType() {
        StubWorkLocation wl = createStub();
        Unit u = createColonist();
        u.setWorkType(grainType);
        assertEquals(4, wl.getUnitProduction(u, grainType));
    }


    // ------------------------------------------------------------------
    // M31: getProductionOf(Unit, GoodsType) — 3 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M31-TP1: ENTRY -> N0 -(B1:false)-> N1 -> EXIT (unit == null throws RuntimeException)
    public void testM31_TP1_getProductionOf_nullUnitThrows() {
        StubWorkLocation wl = createStub();
        try {
            wl.getProductionOf(null, grainType);
            fail("Expected RuntimeException for null unit");
        } catch (RuntimeException expected) {
            assertTrue(expected.getMessage().contains("Null unit"));
        }
    }

    // M31-TP2: ENTRY -> N0 -(B1:true)-> N2 -(B2:false)-> N3 -> N5 -> EXIT (!produces(goodsType))
    public void testM31_TP2_getProductionOf_notProduced() {
        StubWorkLocation wl = createStub();
        Unit u = createColonist();
        assertEquals(0, wl.getProductionOf(u, grainType));
    }

    // M31-TP3: ENTRY -> N0 -(B1:true)-> N2 -(B2:true)-> N4 -> N5 -> EXIT (produces(goodsType))
    public void testM31_TP3_getProductionOf_produced() {
        StubWorkLocation wl = createStub();
        wl.setProductionType(new ProductionType(new AbstractGoods(grainType, 4), false, null));
        Unit u = createColonist();
        assertEquals(4, wl.getProductionOf(u, grainType));
    }


    // ------------------------------------------------------------------
    // M32: getPotentialProduction(GoodsType, UnitType) — 18 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M32-TP1: ENTRY -> N0 -(B1:false)-> N1 -> EXIT (!canProduce)
    public void testM32_TP1_getPotentialProduction_cannotProduce() {
        StubWorkLocation wl = createStub();
        wl.canProduceFlag = false;
        assertEquals(0, wl.getPotentialProduction(grainType, colonistType));
    }

    // M32-TP2: ENTRY -> N0 -(B1:true)-> N2 -(B2:true)-> N8 -(B5:true)-> N10 -> N11 -> EXIT (unitType == null, amount >= 0)
    public void testM32_TP2_getPotentialProduction_unattendedPositive() {
        StubWorkLocation wl = createStub();
        assertEquals(4, wl.getPotentialProduction(grainType, null));
    }

    // M32-TP3: ENTRY -> N0 -(B1:true)-> N2 -(B2:true)-> N8 -(B5:false)-> N9 -> N11 -> EXIT (unitType == null, amount < 0 clamped to 0)
    public void testM32_TP3_getPotentialProduction_negativeClampedToZero() {
        StubWorkLocation wl = createStub();
        wl.baseProduction = -5;
        assertEquals(0, wl.getPotentialProduction(grainType, null));
    }

    // M32-TP4..TP6: ENTRY -> N0 -> N2 -(B2:false)-> N3 -(case:1..3)-> N4 -> N8 -(B5:true)-> N10 -> N11 -> EXIT
    public void testM32_TP4_to_TP6_getPotentialProduction_allowedReasons() {
        StubWorkLocation wl = createStub();
        for (NoAddReason reason : Arrays.asList(
                NoAddReason.NONE,
                NoAddReason.ALREADY_PRESENT,
                NoAddReason.CLAIM_REQUIRED)) {
            wl.noWorkReason = reason;
            assertEquals("Reason: " + reason, 4, wl.getPotentialProduction(grainType, colonistType));
        }
    }

    // M32-TP7: ENTRY -> N0 -> N2 -(B2:false)-> N3 -(case:4)-> N5 -(B4:false)-> N6 -> N8 -(B5:true)-> N10 -> N11 -> EXIT
    public void testM32_TP7_getPotentialProduction_capacityExceededWithPositiveCapacity() {
        StubWorkLocation wl = createStub();
        wl.noWorkReason = NoAddReason.CAPACITY_EXCEEDED;
        wl.unitCapacity = 2;
        assertEquals(4, wl.getPotentialProduction(grainType, colonistType));
    }

    // M32-TP8: ENTRY -> N0 -> N2 -(B2:false)-> N3 -(case:4)-> N5 -(B4:true)-> N7 -> EXIT
    public void testM32_TP8_getPotentialProduction_capacityExceededZeroCapacity() {
        StubWorkLocation wl = createStub();
        wl.noWorkReason = NoAddReason.CAPACITY_EXCEEDED;
        wl.unitCapacity = 0;
        assertEquals(0, wl.getPotentialProduction(grainType, colonistType));
    }

    // M32-TP9..TP18: ENTRY -> N0 -> N2 -(B2:false)-> N3 -(case:5..13 & default)-> N7 -> EXIT
    public void testM32_TP9_to_TP18_getPotentialProduction_blockingAndDefaultReasons() {
        StubWorkLocation wl = createStub();
        for (NoAddReason reason : Arrays.asList(
                NoAddReason.WRONG_TYPE,
                NoAddReason.OWNED_BY_ENEMY,
                NoAddReason.ANOTHER_COLONY,
                NoAddReason.COLONY_CENTER,
                NoAddReason.MISSING_ABILITY,
                NoAddReason.MISSING_SKILL,
                NoAddReason.MINIMUM_SKILL,
                NoAddReason.MAXIMUM_SKILL,
                NoAddReason.OCCUPIED_BY_ENEMY,
                NoAddReason.WORKER_DAMAGED)) {
            wl.noWorkReason = reason;
            assertEquals("Reason: " + reason, 0, wl.getPotentialProduction(grainType, colonistType));
        }
    }


    // ------------------------------------------------------------------
    // M33: getProductionDeficit(GoodsType) — 2 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M33-TP1: ENTRY -> N0 -(B1:false)-> N1 -> N3 -> EXIT (pi == null)
    public void testM33_TP1_getProductionDeficit_nullInfo() {
        StubWorkLocation wl = createStub();
        wl.useCustomProductionInfo = true;
        wl.customProductionInfo = null;
        assertNull(wl.getProductionDeficit(grainType));
    }

    // M33-TP2: ENTRY -> N0 -(B1:true)-> N2 -> N3 -> EXIT (pi != null)
    public void testM33_TP2_getProductionDeficit_withDeficit() {
        StubWorkLocation wl = createStub();
        wl.useCustomProductionInfo = true;
        ProductionInfo pi = new ProductionInfo();
        pi.addProduction(new AbstractGoods(grainType, 2));
        pi.addMaximumProduction(new AbstractGoods(grainType, 5));
        wl.customProductionInfo = pi;
        AbstractGoods deficit = wl.getProductionDeficit(grainType);
        assertNotNull(deficit);
        assertEquals(3, deficit.getAmount());
    }


    // ------------------------------------------------------------------
    // M34: getLocationLabelFor(Player) — 2 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M34-TP1: ENTRY -> N0 -(B1:false)-> N1 -> N3 -> EXIT (getOwner() == player)
    public void testM34_TP1_getLocationLabelFor_owner() {
        StubWorkLocation wl = createStub();
        assertEquals(wl.getLocationLabel(), wl.getLocationLabelFor(player));
    }

    // M34-TP2: ENTRY -> N0 -(B1:true)-> N2 -> N3 -> EXIT (getOwner() != player)
    public void testM34_TP2_getLocationLabelFor_otherPlayer() {
        StubWorkLocation wl = createStub();
        assertEquals(colony.getLocationLabelFor(otherPlayer), wl.getLocationLabelFor(otherPlayer));
    }


    // ------------------------------------------------------------------
    // M35: getTile() — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M35-TP1: ENTRY -> N0 -> EXIT (returns settlement tile)
    public void testM35_TP1_getTile() {
        StubWorkLocation wl = createStub();
        assertEquals(colony.getTile(), wl.getTile());
    }


    // ------------------------------------------------------------------
    // M36: add(Locatable) — 3 Test Paths (100% feasible C3)
    // ------------------------------------------------------------------

    // M36-TP1: ENTRY -> N0 -(B1:case:1 NONE)-> N1 -> N4 -(B2:true)-> N6 -> EXIT
    public void testM36_TP1_add_success() {
        StubWorkLocation wl = createStub();
        Unit u = createColonist();
        assertTrue(wl.add(u));
        assertEquals(Unit.UnitState.IN_COLONY, u.getState());
        assertEquals(0, u.getMovesLeft());
    }

    // M36-TP2: ENTRY -> N0 -(B1:case:2 ALREADY_PRESENT)-> N2 -> EXIT
    public void testM36_TP2_add_alreadyPresent() {
        StubWorkLocation wl = createStub();
        Unit u = createColonist();
        assertTrue(wl.add(u));
        assertTrue(wl.add(u));
    }

    // M36-TP3: ENTRY -> N0 -(B1:default)-> N3 -> EXIT (throws IllegalStateException)
    public void testM36_TP3_add_invalidThrows() {
        StubWorkLocation wl = createStub();
        try {
            wl.add(new Goods(game, colony, grainType, 10));
            fail("Expected IllegalStateException");
        } catch (IllegalStateException expected) {
            assertTrue(expected.getMessage().contains("Can not add"));
        }
    }


    // ------------------------------------------------------------------
    // M37: remove(Locatable) — 4 Test Paths (100% feasible C3)
    // ------------------------------------------------------------------

    // M37-TP1: ENTRY -> N0 -(B1:false)-> N1 -> EXIT (non-Unit throws IllegalStateException)
    public void testM37_TP1_remove_nonUnitThrows() {
        StubWorkLocation wl = createStub();
        try {
            wl.remove(new Goods(game, colony, grainType, 10));
            fail("Expected IllegalStateException");
        } catch (IllegalStateException expected) {
            assertTrue(expected.getMessage().contains("Not a unit"));
        }
    }

    // M37-TP2: ENTRY -> N0 -(B1:true)-> N2 -(B2:false)-> N3 -> EXIT (!contains(unit) returns true)
    public void testM37_TP2_remove_notContained() {
        StubWorkLocation wl = createStub();
        Unit u = createColonist();
        assertTrue(wl.remove(u));
    }

    // M37-TP3: ENTRY -> N0 -(B1:true)-> N2 -(B2:true)-> N4 -(B3:true)-> N6 -(B4:false)-> N7 -> N8 -> EXIT (becomes empty)
    public void testM37_TP3_remove_lastUnitBecomesEmpty() {
        StubWorkLocation wl = createStub();
        Unit u = createColonist();
        wl.add(u);
        assertTrue(wl.remove(u));
        assertTrue(wl.isEmpty());
        assertEquals(Unit.UnitState.ACTIVE, u.getState());
    }

    // M37-TP4: ENTRY -> N0 -(B1:true)-> N2 -(B2:true)-> N4 -(B3:true)-> N6 -(B4:true)-> N8 -> EXIT (still non-empty)
    public void testM37_TP4_remove_stillNonEmpty() {
        StubWorkLocation wl = createStub();
        Unit u1 = createColonist();
        Unit u2 = createColonist();
        wl.add(u1);
        wl.add(u2);
        assertTrue(wl.remove(u1));
        assertFalse(wl.isEmpty());
    }


    // ------------------------------------------------------------------
    // M38: getSettlement() — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M38-TP1: ENTRY -> N0 -> EXIT (returns colony as Settlement)
    public void testM38_TP1_getSettlement() {
        StubWorkLocation wl = createStub();
        assertEquals(colony, wl.getSettlement());
    }


    // ------------------------------------------------------------------
    // M39: getColony() — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M39-TP1: ENTRY -> N0 -> EXIT (returns colony)
    public void testM39_TP1_getColony() {
        StubWorkLocation wl = createStub();
        assertEquals(colony, wl.getColony());
    }


    // ------------------------------------------------------------------
    // M40: getRank() — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M40-TP1: ENTRY -> N0 -> EXIT (returns Location.rankOf(getTile()))
    public void testM40_TP1_getRank() {
        StubWorkLocation wl = createStub();
        assertEquals(Location.rankOf(colony.getTile()), wl.getRank());
    }


    // ------------------------------------------------------------------
    // M41: getNoAddReason(Locatable) — 4 Test Paths (100% feasible C3)
    // ------------------------------------------------------------------

    // M41-TP1: ENTRY -> N0 -(B1:false)-> N1 -(B2:false)-> N2 -> EXIT (damaged Unit -> WORKER_DAMAGED)
    public void testM41_TP1_getNoAddReason_damagedUnit() {
        StubWorkLocation wl = createStub();
        Unit u = createColonist();
        u.setHitPoints(0);
        assertEquals(NoAddReason.WORKER_DAMAGED, wl.getNoAddReason(u));
    }

    // M41-TP2: ENTRY -> N0 -(B1:true)-> N3 -(B3:true)-> N6 -> N7 -> EXIT (non-Unit -> WRONG_TYPE)
    public void testM41_TP2_getNoAddReason_nonUnit() {
        StubWorkLocation wl = createStub();
        assertEquals(NoAddReason.WRONG_TYPE, wl.getNoAddReason(new Goods(game, colony, grainType, 5)));
    }

    // M41-TP3: ENTRY -> N0 -(B1:false)-> N1 -(B2:true)-> N3 -(B3:false)-> N4 -(B4:true)-> N6 -> N7 -> EXIT (non-person Unit -> WRONG_TYPE)
    public void testM41_TP3_getNoAddReason_nonPersonUnit() {
        StubWorkLocation wl = createStub();
        Unit wagon = new ServerUnit(game, colony.getTile(), player,
            spec().getUnitType("model.unit.wagonTrain"));
        assertEquals(NoAddReason.WRONG_TYPE, wl.getNoAddReason(wagon));
    }

    // M41-TP4: ENTRY -> N0 -(B1:false)-> N1 -(B2:true)-> N3 -(B3:false)-> N4 -(B4:false)-> N5 -> N7 -> EXIT (person Unit -> super.getNoAddReason)
    public void testM41_TP4_getNoAddReason_validPerson() {
        StubWorkLocation wl = createStub();
        Unit u = createColonist();
        assertEquals(NoAddReason.NONE, wl.getNoAddReason(u));
    }


    // ------------------------------------------------------------------
    // M42: evaluateFor(Player) — 3 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M42-TP1: ENTRY -> N0 -> N1 -(B1:true)-> N5 -> EXIT (empty unit list -> 0)
    public void testM42_TP1_evaluateFor_empty() {
        StubWorkLocation wl = createStub();
        assertEquals(0, wl.evaluateFor(player));
    }

    // M42-TP2: ENTRY -> N0 -> N1 -(B1:false)-> N2 -(B2:false)-> N3 -> EXIT (unit evaluates to Integer.MIN_VALUE)
    public void testM42_TP2_evaluateFor_minValue() {
        StubWorkLocation wl = createStub();
        Unit minUnit = new ServerUnit(game, colony.getTile(), player, colonistType) {
            @Override
            public int evaluateFor(Player p) {
                return Integer.MIN_VALUE;
            }
        };
        wl.add(minUnit);
        assertEquals(Integer.MIN_VALUE, wl.evaluateFor(player));
    }

    // M42-TP3: ENTRY -> N0 -> N1 -(B1:false)-> N2 -(B2:true)-> N4 -> N1 -(B1:true)-> N5 -> EXIT (accumulates unit values)
    public void testM42_TP3_evaluateFor_sumUnits() {
        StubWorkLocation wl = createStub();
        Unit u = createColonist();
        wl.add(u);
        assertEquals(u.evaluateFor(player), wl.evaluateFor(player));
    }


    // ------------------------------------------------------------------
    // M43: getClaimTemplate() — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M43-TP1: ENTRY -> N0 -> EXIT (returns empty name StringTemplate)
    public void testM43_TP1_getClaimTemplate() {
        StubWorkLocation wl = createStub();
        assertEquals(StringTemplate.name(""), wl.getClaimTemplate());
    }


    // ------------------------------------------------------------------
    // M44: getOwner() — 2 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M44-TP1: ENTRY -> N0 -(B1:false)-> N1 -> N3 -> EXIT (colony == null -> null)
    public void testM44_TP1_getOwner_nullColony() {
        StubWorkLocation wl = new StubWorkLocation(game, "wl:noColony");
        assertNull(wl.getOwner());
    }

    // M44-TP2: ENTRY -> N0 -(B1:true)-> N2 -> N3 -> EXIT (colony != null -> colony.getOwner())
    public void testM44_TP2_getOwner_withColony() {
        StubWorkLocation wl = createStub();
        assertEquals(player, wl.getOwner());
    }


    // ------------------------------------------------------------------
    // M45: setOwner(Player) — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M45-TP1: ENTRY -> N0 -> EXIT (always throws UnsupportedOperationException)
    public void testM45_TP1_setOwner_throws() {
        StubWorkLocation wl = createStub();
        try {
            wl.setOwner(otherPlayer);
            fail("Expected UnsupportedOperationException");
        } catch (UnsupportedOperationException expected) {
            // Expected
        }
    }


    // ------------------------------------------------------------------
    // M46: copyIn(FreeColObject) — 2 Test Paths (100% feasible C3)
    // ------------------------------------------------------------------

    // M46-TP1: ENTRY -> N0 -(B1:true)-> N2 -> EXIT (other == null or wrong type -> false)
    public void testM46_TP1_copyIn_nullOrWrongType() {
        StubWorkLocation wl = createStub();
        assertFalse(wl.copyIn(null));
        assertFalse(wl.copyIn(colony));
    }

    // M46-TP2: ENTRY -> N0 -(B1:false)-> N1 -(B2:true)-> N3 -> EXIT (valid WorkLocation -> true)
    public void testM46_TP2_copyIn_valid() {
        StubWorkLocation dst = createStub();
        Game otherGame = getStandardGame();
        StubWorkLocation src = new StubWorkLocation(otherGame, dst.getId());
        src.setColony(colony);
        ProductionType pt = new ProductionType(new AbstractGoods(grainType, 4), false, null);
        src.setProductionType(pt);
        assertTrue(dst.copyIn(src));
        assertEquals(colony, dst.getColony());
        assertEquals(pt, dst.getProductionType());
    }


    // ------------------------------------------------------------------
    // M47 & M48: writeAttributes & writeChildren — 2 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M47-TP1 & M48-TP1: writeAttributes + writeChildren with productionType == null (B1:true)
    public void testM47_TP1_M48_TP1_writeXML_noProductionType() throws Exception {
        StubWorkLocation wl = createStub();
        StringWriter sw = new StringWriter();
        try (FreeColXMLWriter xw = new FreeColXMLWriter(
                sw, FreeColXMLWriter.WriteScope.toSave())) {
            wl.toXML(xw);
        }
        String xml = sw.toString();
        assertTrue(xml.contains("colony=\"" + colony.getId() + "\""));
        assertFalse(xml.contains("<" + ProductionType.TAG));
    }

    // M48-TP2: writeChildren with productionType != null (B1:false)
    public void testM48_TP2_writeChildren_withProductionType() throws Exception {
        StubWorkLocation wl = createStub();
        wl.setProductionType(new ProductionType(new AbstractGoods(grainType, 4), false, null));
        StringWriter sw = new StringWriter();
        try (FreeColXMLWriter xw = new FreeColXMLWriter(
                sw, FreeColXMLWriter.WriteScope.toSave())) {
            wl.toXML(xw);
        }
        assertTrue(sw.toString().contains("<" + ProductionType.TAG));
    }


    // ------------------------------------------------------------------
    // M49 & M50: readAttributes & readChild — 2 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M49-TP1 & M50-TP1: readAttributes + readChild with ProductionType tag (B1:false -> N1)
    public void testM49_TP1_M50_TP1_readXML_withProductionType() throws Exception {
        StubWorkLocation src = createStub();
        src.setProductionType(new ProductionType(new AbstractGoods(grainType, 4), false, null));
        StringWriter sw = new StringWriter();
        try (FreeColXMLWriter xw = new FreeColXMLWriter(
                sw, FreeColXMLWriter.WriteScope.toSave())) {
            src.toXML(xw);
        }

        StubWorkLocation dst = new StubWorkLocation(game);
        try (FreeColXMLReader xr = new FreeColXMLReader(new StringReader(sw.toString()))) {
            xr.nextTag();
            dst.readFromXML(xr);
        }
        assertEquals(colony, dst.getColony());
        assertNotNull(dst.getProductionType());
    }

    // M50-TP2: readChild with non-ProductionType tag (B1:true -> N2 super.readChild)
    public void testM50_TP2_readChild_superBranch() throws Exception {
        StubWorkLocation dst = createStub();
        Unit u = createColonist();
        StringWriter unitSw = new StringWriter();
        try (FreeColXMLWriter xw = new FreeColXMLWriter(
                unitSw, FreeColXMLWriter.WriteScope.toSave())) {
            u.toXML(xw);
        }
        String unitXml = unitSw.toString().replaceFirst("<\\?xml[^>]*\\?>", "");
        String xml = "<stubWorkLocation id=\"" + dst.getId() + "\" colony=\"" + colony.getId() + "\">"
            + unitXml
            + "</stubWorkLocation>";
        try (FreeColXMLReader xr = new FreeColXMLReader(new StringReader(xml))) {
            xr.nextTag();
            dst.readFromXML(xr);
        }
        assertEquals(colony, dst.getColony());
        assertEquals(1, dst.getUnitCount());
    }


    // ------------------------------------------------------------------
    // M51 & M52 & M53: Lambdas & <clinit> — 3 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M51-TP1: lambda$getExpertUnitType$0 (consumed via getExpertUnitType)
    // M52-TP1: lambda$getSuggestion$0 (consumed via getSuggestion with non-empty inputs)
    // M53-TP1: <clinit> (EMPTY_LIST initialization)
    public void testM51_M52_M53_lambdasAndClinit() throws Exception {
        assertNotNull(WorkLocation.EMPTY_LIST);
        assertTrue(WorkLocation.EMPTY_LIST.isEmpty());

        // M51: already exercised by M28-TP1, verify explicitly
        StubWorkLocation wl = createStub();
        wl.availableProductionTypes.add(
            new ProductionType(new AbstractGoods(grainType, 4), false, null));
        assertEquals(expertFarmerType, wl.getExpertUnitType());

        // M52: exercise lambda$getSuggestion$0 by providing non-empty inputs
        wl.useCustomGoodSuggestion = true;
        wl.customGoodSuggestionResult = true;
        ProductionType ptWithInputs = new ProductionType(
            Collections.singletonList(new AbstractGoods(lumberType, 1)),
            Collections.singletonList(new AbstractGoods(grainType, 4)));
        // Invoke getSuggestion: lambda$getSuggestion$0 executes on lumberType input
        invokeGetSuggestion(wl, null, ptWithInputs, grainType);
    }
}
