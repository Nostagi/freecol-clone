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
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import net.sf.freecol.common.util.LogBuilder;
import net.sf.freecol.server.model.ServerUnit;
import net.sf.freecol.util.test.FreeColTestCase;


/**
 * CFG-driven test suite for {@link Occupation} derived from
 * {@code test_cft/graph/net/sf/freecol/common/model/Occupation.json}.
 *
 * Covers all 8 methods (M1..M8, including {@code <init>} and both
 * {@code lambda$improve$0} / {@code lambda$improve$1}) and achieves 100%
 * C3 (node + edge) coverage across 20 Test Paths.
 */
public class OccupationCfgTest extends FreeColTestCase {

    private Game game;
    private Colony colony;
    private GoodsType lumberType;
    private GoodsType hammersType;
    private GoodsType foodType;
    private UnitType colonistType;
    private Building carpenterHouse;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        // Pre-condition: Initialize standard game, map, colony, goods types, and carpenter house
        game = getStandardGame();
        game.setMap(getTestMap());
        colony = createStandardColony(3);
        lumberType = spec().getGoodsType("model.goods.lumber");
        hammersType = spec().getGoodsType("model.goods.hammers");
        foodType = spec().getGoodsType("model.goods.food");
        colonistType = spec().getUnitType("model.unit.freeColonist");
        BuildingType carpenterType = spec().getBuildingType("model.building.carpenterHouse");
        carpenterHouse = colony.getBuilding(carpenterType);
        colony.addGoods(lumberType, 100);
    }

    // ------------------------------------------------------------------
    // Pre-condition helpers & stubs
    // ------------------------------------------------------------------

    /**
     * Invoke private {@link Occupation#improve(UnitType, WorkLocation, int, Collection, boolean, LogBuilder)}
     * via reflection.
     */
    private int invokePrivateImprove(Occupation occ, UnitType unitType,
                                     WorkLocation wl, int bestAmount,
                                     Collection<GoodsType> workTypes,
                                     boolean alone, LogBuilder lb) throws Exception {
        Method m = Occupation.class.getDeclaredMethod(
            "improve", UnitType.class, WorkLocation.class, int.class,
            Collection.class, boolean.class, LogBuilder.class);
        m.setAccessible(true);
        return (Integer) m.invoke(occ, unitType, wl, bestAmount, workTypes, alone, lb);
    }

    /**
     * Find a {@link ColonyTile} in {@code colony} that already contains a unit.
     */
    private ColonyTile findOccupiedColonyTile() {
        for (ColonyTile ct : colony.getColonyTiles()) {
            if (!ct.isColonyCenterTile() && !ct.isEmpty()) {
                return ct;
            }
        }
        return null;
    }

    /**
     * Clear all units out of {@code carpenterHouse} onto the colony tile.
     */
    private void clearCarpenterHouse() {
        for (Unit u : carpenterHouse.getUnitList()) {
            u.setLocation(colony.getTile());
        }
    }

    // ------------------------------------------------------------------
    // M1: <init>(Lnet/sf/freecol/common/model/WorkLocation;Lnet/sf/freecol/common/model/ProductionType;Lnet/sf/freecol/common/model/GoodsType;)V — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M1-TP1: ENTRY -> N0 -> EXIT (Construct Occupation with workLocation, productionType, workType)
    public void testM1_TP1_Constructor() {
        ProductionType pt = carpenterHouse.getAvailableProductionTypes(false).get(0);
        Occupation occ = new Occupation(carpenterHouse, pt, hammersType);
        assertSame(carpenterHouse, occ.workLocation);
        assertSame(pt, occ.productionType);
        assertSame(hammersType, occ.workType);
    }

    // ------------------------------------------------------------------
    // M2: install(Lnet/sf/freecol/common/model/Unit;)Z — 5 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M2-TP1: ENTRY -> N0 -(B1:true)-> N2 -(B2:true)-> N4 -(B3:true)-> N6 -> EXIT (setLocation=true, productionType matches, workType matches)
    public void testM2_TP1_InstallSameProductionAndWorkType() {
        clearCarpenterHouse();
        Unit unit = colony.getFirstUnit();
        ProductionType pt = carpenterHouse.getAvailableProductionTypes(false).get(0);
        carpenterHouse.setProductionType(pt);
        unit.changeWorkType(hammersType);

        Occupation occ = new Occupation(carpenterHouse, pt, hammersType);
        assertTrue(occ.install(unit));
        assertSame(carpenterHouse, unit.getLocation());
        assertSame(pt, carpenterHouse.getProductionType());
        assertSame(hammersType, unit.getWorkType());
    }

    // M2-TP2: ENTRY -> N0 -(B1:true)-> N2 -(B2:true)-> N4 -(B3:false)-> N5 -> N6 -> EXIT (setLocation=true, productionType matches, workType differs)
    public void testM2_TP2_InstallChangesWorkType() {
        clearCarpenterHouse();
        Unit unit = colony.getFirstUnit();
        ProductionType pt = carpenterHouse.getAvailableProductionTypes(false).get(0);
        carpenterHouse.setProductionType(pt);
        unit.changeWorkType(foodType);

        Occupation occ = new Occupation(carpenterHouse, pt, hammersType);
        assertTrue(occ.install(unit));
        assertSame(hammersType, unit.getWorkType());
    }

    // M2-TP3: ENTRY -> N0 -(B1:true)-> N2 -(B2:false)-> N3 -> N4 -(B3:true)-> N6 -> EXIT (setLocation=true, productionType differs, workType matches)
    public void testM2_TP3_InstallChangesProductionType() {
        clearCarpenterHouse();
        Unit unit = colony.getFirstUnit();
        ProductionType pt = carpenterHouse.getAvailableProductionTypes(false).get(0);
        carpenterHouse.setProductionType(null);
        unit.changeWorkType(hammersType);

        Occupation occ = new Occupation(carpenterHouse, pt, hammersType);
        assertTrue(occ.install(unit));
        assertSame(pt, carpenterHouse.getProductionType());
    }

    // M2-TP4: ENTRY -> N0 -(B1:true)-> N2 -(B2:false)-> N3 -> N4 -(B3:false)-> N5 -> N6 -> EXIT (setLocation=true, productionType differs, workType differs)
    public void testM2_TP4_InstallChangesProductionAndWorkType() {
        clearCarpenterHouse();
        Unit unit = colony.getFirstUnit();
        ProductionType pt = carpenterHouse.getAvailableProductionTypes(false).get(0);
        carpenterHouse.setProductionType(null);
        unit.changeWorkType(foodType);

        Occupation occ = new Occupation(carpenterHouse, pt, hammersType);
        assertTrue(occ.install(unit));
        assertSame(pt, carpenterHouse.getProductionType());
        assertSame(hammersType, unit.getWorkType());
    }

    // M2-TP5: ENTRY -> N0 -(B1:false)-> N1 -> EXIT (unit.setLocation(workLocation) returns false)
    public void testM2_TP5_InstallFailsWhenSetLocationFails() {
        ColonyTile occupiedTile = findOccupiedColonyTile();
        assertNotNull(occupiedTile);
        Unit outsider = new ServerUnit(game, colony.getTile(), colony.getOwner(), colonistType);
        Occupation occ = new Occupation(occupiedTile, null, foodType);
        assertFalse(occ.install(outsider));
    }

    // ------------------------------------------------------------------
    // M3: improve(Lnet/sf/freecol/common/model/UnitType;Lnet/sf/freecol/common/model/WorkLocation;ILjava/util/Collection;ZLnet/sf/freecol/common/util/LogBuilder;)I — 4 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M3-TP1: ENTRY -> N0 -(B1:true)-> N2 -> N3 -> N4 -(B2:true)-> N14 -> EXIT (alone=false, productionType==null -> empty outer loop)
    public void testM3_TP1_PrivateImproveNotAloneNullProductionType() throws Exception {
        carpenterHouse.setProductionType(null);
        Occupation occ = new Occupation(null, null, null);
        LogBuilder lb = new LogBuilder(64);
        int result = invokePrivateImprove(occ, colonistType, carpenterHouse,
            5, Collections.singletonList(hammersType), false, lb);
        assertEquals(5, result);
        assertNull(occ.workLocation);
    }

    // M3-TP2: ENTRY -> N0 -(B1:false)-> N1 -> N3 -> N4 -(B2:false)-> N5 -> N6 -(B3:true)-> N13 -> N4 -(B2:true)-> N14 -> EXIT (alone=true, outer loop runs, inner loop empty due to empty workTypes)
    public void testM3_TP2_PrivateImproveAloneEmptyWorkTypes() throws Exception {
        Occupation occ = new Occupation(null, null, null);
        LogBuilder lb = new LogBuilder(64);
        int result = invokePrivateImprove(occ, colonistType, carpenterHouse,
            5, Collections.<GoodsType>emptyList(), true, lb);
        assertEquals(5, result);
        assertNull(occ.workLocation);
    }

    // M3-TP3: ENTRY -> N0 -(B1:false)-> N1 -> N3 -> N4 -(B2:false)-> N5 -> N6 -(B3:false)-> N7 -(B4:false)-> N8 -> N10 -(B5:false)-> N11 -> N12 -> N6 -(B3:true)-> N13 -> N4 -(B2:true)-> N14 -> EXIT (alone=true, bestAmount < amount updates Occupation)
    public void testM3_TP3_PrivateImproveAloneImprovesBestAmount() throws Exception {
        Occupation occ = new Occupation(null, null, null);
        LogBuilder lb = new LogBuilder(64);
        int result = invokePrivateImprove(occ, colonistType, carpenterHouse,
            0, Collections.singletonList(hammersType), true, lb);
        assertTrue(result > 0);
        assertSame(carpenterHouse, occ.workLocation);
        assertNotNull(occ.productionType);
        assertSame(hammersType, occ.workType);
    }

    // M3-TP4: ENTRY -> N0 -(B1:true)-> N2 -> N3 -> N4 -(B2:false)-> N5 -> N6 -(B3:false)-> N7 -(B4:true)-> N9 -> N10 -(B5:true)-> N12 -> N6 -(B3:true)-> N13 -> N4 -(B2:true)-> N14 -> EXIT (alone=false, bestAmount >= amount does not update Occupation)
    public void testM3_TP4_PrivateImproveNotAloneDoesNotBeatBestAmount() throws Exception {
        ProductionType pt = carpenterHouse.getAvailableProductionTypes(false).get(0);
        carpenterHouse.setProductionType(pt);
        Occupation occ = new Occupation(null, null, null);
        LogBuilder lb = new LogBuilder(64);
        int result = invokePrivateImprove(occ, colonistType, carpenterHouse,
            1000, Collections.singletonList(hammersType), false, lb);
        assertEquals(1000, result);
        assertNull(occ.workLocation);
    }

    // ------------------------------------------------------------------
    // M4: improve(Lnet/sf/freecol/common/model/Unit;Lnet/sf/freecol/common/model/WorkLocation;ILjava/util/Collection;Lnet/sf/freecol/common/util/LogBuilder;)I — 6 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M4-TP1: ENTRY -> N0 -(B1:true)-> N2 -> N3 -(B2:false)-> N4 -(B3:false)-> N5 -> N7 -(B4:false)-> N8 -(B5:false)-> N9 -> EXIT (present=false, canAdd=false -> returns bestAmount)
    public void testM4_TP1_ImproveUnitNotPresentAndCannotAdd() {
        ColonyTile occupiedTile = findOccupiedColonyTile();
        assertNotNull(occupiedTile);
        Unit outsider = new ServerUnit(game, colony.getTile(), colony.getOwner(), colonistType);
        Occupation occ = new Occupation(null, null, null);
        LogBuilder lb = new LogBuilder(64);
        int result = occ.improve(outsider, occupiedTile, 7,
            Collections.singletonList(foodType), lb);
        assertEquals(7, result);
        assertNull(occ.workLocation);
    }

    // M4-TP2: ENTRY -> N0 -(B1:true)-> N2 -> N3 -(B2:false)-> N4 -(B3:true)-> N6 -> N7 -(B4:false)-> N8 -(B5:true)-> N10 -(B6:true)-> N14 -> N16 -> EXIT (present=false, canAdd=true, productionType==null -> alone=true)
    public void testM4_TP2_ImproveUnitNotPresentNullProductionType() {
        clearCarpenterHouse();
        carpenterHouse.setProductionType(null);
        Unit outsider = new ServerUnit(game, colony.getTile(), colony.getOwner(), colonistType);
        Occupation occ = new Occupation(null, null, null);
        LogBuilder lb = new LogBuilder(64);
        int result = occ.improve(outsider, carpenterHouse, 0,
            Collections.singletonList(hammersType), lb);
        assertTrue(result > 0);
        assertSame(carpenterHouse, occ.workLocation);
    }

    // M4-TP3: ENTRY -> N0 -(B1:true)-> N2 -> N3 -(B2:false)-> N4 -(B3:true)-> N6 -> N7 -(B4:false)-> N8 -(B5:true)-> N10 -(B6:false)-> N11 -(B7:true)-> N14 -> N16 -> EXIT (present=false, canAdd=true, productionType!=null, wl.isEmpty()==true -> alone=true)
    public void testM4_TP3_ImproveUnitNotPresentEmptyWorkLocation() {
        clearCarpenterHouse();
        ProductionType pt = carpenterHouse.getAvailableProductionTypes(false).get(0);
        carpenterHouse.setProductionType(pt);
        Unit outsider = new ServerUnit(game, colony.getTile(), colony.getOwner(), colonistType);
        Occupation occ = new Occupation(null, null, null);
        LogBuilder lb = new LogBuilder(64);
        int result = occ.improve(outsider, carpenterHouse, 0,
            Collections.singletonList(hammersType), lb);
        assertTrue(result > 0);
        assertSame(carpenterHouse, occ.workLocation);
    }

    // M4-TP4: ENTRY -> N0 -(B1:true)-> N2 -> N3 -(B2:false)-> N4 -(B3:true)-> N6 -> N7 -(B4:false)-> N8 -(B5:true)-> N10 -(B6:false)-> N11 -(B7:false)-> N12 -(B8:true)-> N15 -> N16 -> EXIT (present=false, canAdd=true, productionType!=null, wl.isEmpty()==false -> alone=false)
    public void testM4_TP4_ImproveUnitNotPresentOccupiedWorkLocation() {
        clearCarpenterHouse();
        ProductionType pt = carpenterHouse.getAvailableProductionTypes(false).get(0);
        carpenterHouse.setProductionType(pt);
        Unit resident = colony.getFirstUnit();
        resident.setLocation(carpenterHouse);

        Unit outsider = new ServerUnit(game, colony.getTile(), colony.getOwner(), colonistType);
        Occupation occ = new Occupation(null, null, null);
        LogBuilder lb = new LogBuilder(64);
        int result = occ.improve(outsider, carpenterHouse, 0,
            Collections.singletonList(hammersType), lb);
        assertTrue(result > 0);
        assertSame(carpenterHouse, occ.workLocation);
    }

    // M4-TP5: ENTRY -> N0 -(B1:false)-> N1 -> N3 -(B2:true)-> N6 -> N7 -(B4:true)-> N10 -(B6:false)-> N11 -(B7:false)-> N12 -(B8:false)-> N13 -(B9:false)-> N14 -> N16 -> EXIT (present=true, productionType!=null, wl.isEmpty()==false, unitCount==1 -> alone=true)
    public void testM4_TP5_ImproveUnitPresentAlone() {
        clearCarpenterHouse();
        ProductionType pt = carpenterHouse.getAvailableProductionTypes(false).get(0);
        carpenterHouse.setProductionType(pt);
        Unit resident = colony.getFirstUnit();
        resident.setLocation(carpenterHouse);

        Occupation occ = new Occupation(null, null, null);
        LogBuilder lb = new LogBuilder(64);
        int result = occ.improve(resident, carpenterHouse, 0,
            Collections.singletonList(hammersType), lb);
        assertTrue(result > 0);
        assertSame(carpenterHouse, occ.workLocation);
    }

    // M4-TP6: ENTRY -> N0 -(B1:false)-> N1 -> N3 -(B2:true)-> N6 -> N7 -(B4:true)-> N10 -(B6:false)-> N11 -(B7:false)-> N12 -(B8:false)-> N13 -(B9:true)-> N15 -> N16 -> EXIT (present=true, productionType!=null, wl.isEmpty()==false, unitCount>1 -> alone=false)
    public void testM4_TP6_ImproveUnitPresentWithCoworker() {
        clearCarpenterHouse();
        ProductionType pt = carpenterHouse.getAvailableProductionTypes(false).get(0);
        carpenterHouse.setProductionType(pt);
        List<Unit> units = colony.getUnitList();
        Unit u1 = units.get(0);
        Unit u2 = units.get(1);
        u1.setLocation(carpenterHouse);
        u2.setLocation(carpenterHouse);

        Occupation occ = new Occupation(null, null, null);
        LogBuilder lb = new LogBuilder(64);
        int result = occ.improve(u1, carpenterHouse, 0,
            Collections.singletonList(hammersType), lb);
        assertTrue(result > 0);
        assertSame(carpenterHouse, occ.workLocation);
    }

    // ------------------------------------------------------------------
    // M5: improve(Lnet/sf/freecol/common/model/UnitType;Lnet/sf/freecol/common/model/WorkLocation;ILjava/util/Collection;Lnet/sf/freecol/common/util/LogBuilder;)I — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M5-TP1: ENTRY -> N0 -> EXIT (Delegates to private improve with alone = wl.isEmpty())
    public void testM5_TP1_ImproveByUnitType() {
        clearCarpenterHouse();
        Occupation occ = new Occupation(null, null, null);
        LogBuilder lb = new LogBuilder(64);
        int result = occ.improve(colonistType, carpenterHouse, 0,
            Collections.singletonList(hammersType), lb);
        assertTrue(result > 0);
        assertSame(carpenterHouse, occ.workLocation);
    }

    // ------------------------------------------------------------------
    // M6: toString()Ljava/lang/String; — 2 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M6-TP1: ENTRY -> N0 -(B1:true)-> N2 -> EXIT (workType == null)
    public void testM6_TP1_ToStringWithNullWorkType() {
        Occupation occ = new Occupation(carpenterHouse, null, null);
        String s = occ.toString();
        assertTrue(s.startsWith("[Occupation "));
        assertTrue(s.endsWith("]"));
        assertFalse(s.contains(hammersType.getSuffix()));
    }

    // M6-TP2: ENTRY -> N0 -(B1:false)-> N1 -> N2 -> EXIT (workType != null)
    public void testM6_TP2_ToStringWithNonNullWorkType() {
        Occupation occ = new Occupation(carpenterHouse, null, hammersType);
        String s = occ.toString();
        assertTrue(s.startsWith("[Occupation "));
        assertTrue(s.contains(hammersType.getSuffix()));
        assertTrue(s.endsWith("]"));
    }

    // ------------------------------------------------------------------
    // M7: lambda$improve$1(Lnet/sf/freecol/common/model/Colony;Lnet/sf/freecol/common/model/AbstractGoods;)I — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M7-TP1: ENTRY -> N0 -> EXIT (Evaluated when pt.getInputs() is non-empty, e.g. carpenterHouse consuming lumber)
    public void testM7_TP1_LambdaImproveInputsMin() {
        clearCarpenterHouse();
        Occupation occ = new Occupation(null, null, null);
        LogBuilder lb = new LogBuilder(64);
        int result = occ.improve(colonistType, carpenterHouse, 0,
            Collections.singletonList(hammersType), lb);
        assertTrue(result > 0);
    }

    // ------------------------------------------------------------------
    // M8: lambda$improve$0(Lnet/sf/freecol/common/model/ProductionType;Lnet/sf/freecol/common/model/GoodsType;)Lnet/sf/freecol/common/model/AbstractGoods; — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M8-TP1: ENTRY -> N0 -> EXIT (Evaluated when filtering workTypes via pt.getOutput(g))
    public void testM8_TP1_LambdaImproveOutputFilter() {
        clearCarpenterHouse();
        Occupation occ = new Occupation(null, null, null);
        LogBuilder lb = new LogBuilder(64);
        int result = occ.improve(colonistType, carpenterHouse, 0,
            Collections.singletonList(foodType), lb);
        assertEquals(0, result);
        assertNull(occ.workLocation);
    }
}
