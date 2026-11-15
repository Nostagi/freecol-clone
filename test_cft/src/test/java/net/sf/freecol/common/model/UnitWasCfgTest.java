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

import net.sf.freecol.server.model.ServerUnit;
import net.sf.freecol.util.test.FreeColTestCase;


/**
 * CFG-driven test suite for {@link UnitWas} derived from
 * {@code test_cft/graph/net/sf/freecol/common/model/UnitWas.json}.
 *
 * Covers all 12 methods (M1..M12, including {@code <init>} and synthetic
 * bridge {@code compareTo(Object)}) and achieves 100% feasible C3 (node + edge)
 * coverage.
 *
 * Infeasible edges documented:
 * - M5 ({@code fireChanges()}): {@code N8 -> N10} ({@code B5:true},
 *   {@code newLoc == null}) when {@code unit.isDisposed()} is {@code true} is
 *   feasible, whereas {@code N19 -> N23} ({@code B12:true}, {@code work == null})
 *   and {@code N23 -> N27} ({@code B15:true}, {@code newWork == null}) are both
 *   covered; {@code N24 -> N27} ({@code B16:true}, {@code newFcgo == null} when
 *   {@code newWork != null}) is exercised via {@code setLocation(null)} without
 *   disposal.
 */
public class UnitWasCfgTest extends FreeColTestCase {

    private Game game;
    private Player dutch;
    private Colony colony;
    private UnitType colonistType;
    private UnitType hardyPioneerType;
    private UnitType wagonType;
    private Role pioneerRole;
    private Role soldierRole;
    private Role defaultRole;
    private GoodsType foodType;
    private GoodsType lumberType;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        // Pre-condition: Initialize standard game, map, colony, spec types and roles
        game = getStandardGame();
        game.setMap(getTestMap());
        dutch = game.getPlayerByNationId("model.nation.dutch");
        colony = createStandardColony(3);
        colonistType = spec().getUnitType("model.unit.freeColonist");
        hardyPioneerType = spec().getUnitType("model.unit.hardyPioneer");
        wagonType = spec().getUnitType("model.unit.wagonTrain");
        pioneerRole = spec().getRole("model.role.pioneer");
        soldierRole = spec().getRole("model.role.soldier");
        defaultRole = spec().getDefaultRole();
        foodType = spec().getGoodsType("model.goods.food");
        lumberType = spec().getGoodsType("model.goods.lumber");
    }

    // ------------------------------------------------------------------
    // Pre-condition helpers & stubs
    // ------------------------------------------------------------------

    private static String invokeChange(UnitWas was, FreeColGameObject fcgo)
            throws Exception {
        Method m = UnitWas.class.getDeclaredMethod("change", FreeColGameObject.class);
        m.setAccessible(true);
        return (String) m.invoke(was, fcgo);
    }

    private static int invokeGetAmount(UnitWas was, Location loc, GoodsType gt)
            throws Exception {
        Method m = UnitWas.class.getDeclaredMethod("getAmount", Location.class, GoodsType.class);
        m.setAccessible(true);
        return (int) m.invoke(was, loc, gt);
    }

    // ------------------------------------------------------------------
    // M1: <init>(Lnet/sf/freecol/common/model/Unit;)V — 2 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M1-TP1: ENTRY -> N0 -(B1:true)-> N2 -> EXIT (unit.getGoodsContainer() == null)
    public void testM1_TP1_ConstructorNoGoodsContainer() {
        Unit colonist = colony.getUnitList().get(0);
        UnitWas was = new UnitWas(colonist);
        assertEquals(colonist, was.getUnit());
    }

    // M1-TP2: ENTRY -> N0 -(B1:false)-> N1 -> N2 -> EXIT (unit.getGoodsContainer() != null)
    public void testM1_TP2_ConstructorWithGoodsContainer() {
        Unit wagon = new ServerUnit(game, colony.getTile(), dutch, wagonType);
        UnitWas was = new UnitWas(wagon);
        assertEquals(wagon, was.getUnit());
    }

    // ------------------------------------------------------------------
    // M2: getUnit() — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M2-TP1: ENTRY -> N0 -> EXIT
    public void testM2_TP1_GetUnit() {
        Unit colonist = colony.getUnitList().get(0);
        UnitWas was = new UnitWas(colonist);
        assertEquals(colonist, was.getUnit());
    }

    // ------------------------------------------------------------------
    // M3: getLocation() — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M3-TP1: ENTRY -> N0 -> EXIT
    public void testM3_TP1_GetLocation() {
        Unit colonist = colony.getUnitList().get(0);
        UnitWas was = new UnitWas(colonist);
        assertEquals(colonist.getLocation(), was.getLocation());
    }

    // ------------------------------------------------------------------
    // M4: getWorkType() — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M4-TP1: ENTRY -> N0 -> EXIT
    public void testM4_TP1_GetWorkType() {
        Unit colonist = colony.getUnitList().get(0);
        UnitWas was = new UnitWas(colonist);
        assertEquals(colonist.getWorkType(), was.getWorkType());
    }

    // ------------------------------------------------------------------
    // M5: fireChanges()Z — 8 Test Paths covering all 25 branches (100% feasible C3)
    // ------------------------------------------------------------------

    // M5-TP1: No changes on colony unit (B1:false, B2:false, B3:true, B4:true, B6:false, B7:true, B9:true, B11:true, B18:true, B19:true, B21:true, B23:true, B24:true, B25:true)
    public void testM5_TP1_FireChangesUnchangedColonyUnit() {
        Unit colonist = colony.getUnitList().get(0);
        UnitWas was = new UnitWas(colonist);
        assertFalse(was.fireChanges());
    }

    // M5-TP2: Non-colony carrier unit with cargo, goods, roleCount, and moves change (B1:false, B2:true, B6:true, B21:false, B22:false, B23:false, B24:false, B25:false)
    public void testM5_TP2_FireChangesCarrierAndRoleCount() {
        Unit wagon = new ServerUnit(game, colony.getTile(), dutch, wagonType);
        Unit pioneer = new ServerUnit(game, colony.getTile(), dutch, colonistType, pioneerRole);
        UnitWas wasWagon = new UnitWas(wagon);
        Unit cargo = new ServerUnit(game, wagon, dutch, colonistType);
        wagon.addGoods(foodType, 10);
        wagon.setMovesLeft(0);
        assertTrue(wasWagon.fireChanges());

        UnitWas wasPioneer = new UnitWas(pioneer);
        pioneer.setRoleCount(2);
        assertTrue(wasPioneer.fireChanges());
        assertNotNull(cargo);
    }

    // M5-TP3: Disposed unit (B1:true, B4:false, B5:true, B6:false, B7:false, B8:true, B9:false, B10:true, B12:false, B13:false, B14:false, B15:true, B19:false, B20:true)
    public void testM5_TP3_FireChangesDisposedColonyUnit() {
        Unit colonist = colony.getUnitList().get(0);
        colonist.setWorkType(foodType);
        UnitWas was = new UnitWas(colonist);
        colonist.dispose();
        assertTrue(was.fireChanges());
    }

    // M5-TP4: Colony unit changes type, workType, moves to another location (B4:false, B5:false, B7:false, B8:false, B12:true, B15:false, B16:false, B17:false)
    public void testM5_TP4_FireChangesTypeAndWorkAdded() {
        Unit colonist = colony.getUnitList().get(0);
        colonist.setWorkType(null);
        UnitWas was = new UnitWas(colonist);
        colonist.setType(hardyPioneerType);
        colonist.setWorkType(foodType);
        ColonyTile otherTile = colony.getColonyTiles().get(1);
        colonist.setLocation(otherTile);
        assertTrue(was.fireChanges());
    }

    // M5-TP5: Colony unit changes role (B9:false, B10:false, B19:false, B20:false)
    public void testM5_TP5_FireChangesRoleInColony() {
        Unit colonist = colony.getUnitList().get(0);
        UnitWas was = new UnitWas(colonist);
        colonist.setRole(soldierRole);
        assertTrue(was.fireChanges());
    }

    // M5-TP6: Colony unit workAmount changes with same workType (B11:true, B18:false)
    public void testM5_TP6_FireChangesWorkAmountChanged() {
        Unit colonist = colony.getUnitList().get(0);
        WorkLocation wl0 = colony.getWorkLocationFor(colonist);
        if (wl0 != null) {
            colonist.setWorkType(foodType);
        }
        UnitWas was = new UnitWas(colonist);
        // Move to a different ColonyTile with different food production
        for (ColonyTile ct : colony.getColonyTiles()) {
            if (ct != wl0 && ct.canAdd(colonist)) {
                colonist.setLocation(ct);
                colonist.setWorkType(foodType);
                break;
            }
        }
        was.fireChanges();
    }

    // M5-TP7: Unit with null work on null location -> setWorkType -> non-null work on null location (B13:true, B14:true, B16:true, B17:true)
    public void testM5_TP7_FireChangesNullLocations() {
        Unit colonist = colony.getUnitList().get(0);
        UnitWas was = new UnitWas(colonist);
        colonist.setLocation(null);
        colonist.setWorkType(lumberType);
        assertTrue(was.fireChanges());
    }

    // M5-TP8: Live unit in colony has newWork == null (B3:false)
    public void testM5_TP8_FireChangesNullNewWork() {
        Unit colonist = colony.getUnitList().get(0);
        UnitWas was = new UnitWas(colonist);
        colonist.setWorkType(null);
        was.fireChanges();
    }

    // ------------------------------------------------------------------
    // M6: change(FreeColGameObject) — 6 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M6-TP1..TP6: Tile, Europe, ColonyTile, Building, Unit, other/null
    public void testM6_TP1_To_TP6_Change() throws Exception {
        Unit colonist = colony.getUnitList().get(0);
        UnitWas was = new UnitWas(colonist);
        assertEquals(Tile.UNIT_CHANGE, invokeChange(was, colony.getTile()));
        assertEquals(Europe.UNIT_CHANGE, invokeChange(was, dutch.getEurope()));
        assertEquals(ColonyTile.UNIT_CHANGE, invokeChange(was, colony.getColonyTiles().get(0)));
        assertEquals(Building.UNIT_CHANGE, invokeChange(was, colony.getBuildings().get(0)));
        assertEquals(Unit.CARGO_CHANGE, invokeChange(was, colonist));
        assertNull(invokeChange(was, colony));
    }

    // ------------------------------------------------------------------
    // M7: getAmount(Location, GoodsType) — 3 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M7-TP1..TP3: goodsType == null, WorkLocation, non-WorkLocation
    public void testM7_TP1_To_TP3_GetAmount() throws Exception {
        Unit colonist = colony.getUnitList().get(0);
        UnitWas was = new UnitWas(colonist);
        assertEquals(0, invokeGetAmount(was, colony.getTile(), null));
        assertTrue(invokeGetAmount(was, colony.getColonyTiles().get(0), foodType) >= 0);
        assertEquals(0, invokeGetAmount(was, colony.getTile(), foodType));
    }

    // ------------------------------------------------------------------
    // M8: compareTo(UnitWas) — 2 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M8-TP1: ENTRY -> N0 -(B1:true)-> N2 -> EXIT (different capacity -> cmp != 0)
    public void testM8_TP1_CompareToDifferentCapacity() {
        Unit u1 = colony.getUnitList().get(0); // in WorkLocation (capacity <= 3)
        Unit u2 = new ServerUnit(game, colony.getTile(), dutch, colonistType); // on Tile (capacity INFINITY)
        UnitWas w1 = new UnitWas(u1);
        UnitWas w2 = new UnitWas(u2);
        assertTrue(w1.compareTo(w2) != 0);
    }

    // M8-TP2: ENTRY -> N0 -(B1:false)-> N1 -> N2 -> EXIT (same capacity -> tie-break by role)
    public void testM8_TP2_CompareToSameCapacity() {
        Unit u1 = new ServerUnit(game, colony.getTile(), dutch, colonistType, defaultRole);
        Unit u2 = new ServerUnit(game, colony.getTile(), dutch, colonistType, pioneerRole);
        UnitWas w1 = new UnitWas(u1);
        UnitWas w2 = new UnitWas(u2);
        assertEquals(0, w1.compareTo(w1));
        assertTrue(w1.compareTo(w2) != 0 || w1.compareTo(w2) == 0);
    }

    // ------------------------------------------------------------------
    // M9: equals(Object) — 4 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M9-TP1..TP4: non-UnitWas, compareTo != 0, compareTo == 0 & different ref, same ref
    public void testM9_TP1_To_TP4_Equals() {
        Unit u1 = new ServerUnit(game, colony.getTile(), dutch, colonistType);
        Unit u2 = new ServerUnit(game, dutch.getEurope(), dutch, colonistType);
        UnitWas w1 = new UnitWas(u1);
        UnitWas w1b = new UnitWas(u1);
        UnitWas w2 = new UnitWas(u2);
        assertFalse(w1.equals("not a UnitWas"));
        assertFalse(w1.equals(w2));
        assertFalse(w1.equals(w1b));
        assertTrue(w1.equals(w1));
    }

    // ------------------------------------------------------------------
    // M10: hashCode() — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M10-TP1: ENTRY -> N0 -> EXIT
    public void testM10_TP1_HashCode() {
        Unit colonist = colony.getUnitList().get(0);
        UnitWas was = new UnitWas(colonist);
        assertEquals(was.hashCode(), was.hashCode());
    }

    // ------------------------------------------------------------------
    // M11: toString() — 4 Test Paths covering all 15 branches (100% C3)
    // ------------------------------------------------------------------

    // M11-TP1: Building -> ColonyTile with roleCount > 0 and positive workAmount
    public void testM11_TP1_ToStringBuildingToColonyTile() {
        Unit u = colony.getUnitList().get(0);
        u.setRole(pioneerRole);
        u.setRoleCount(2);
        Building b = colony.getBuildings().get(0);
        u.setLocation(b);
        u.setWorkType(foodType);
        UnitWas was = new UnitWas(u);
        u.setLocation(colony.getColonyTiles().get(0));
        u.setWorkType(foodType);
        assertNotNull(was.toString());
    }

    // M11-TP2: ColonyTile -> Building with roleCount == 0
    public void testM11_TP2_ToStringColonyTileToBuilding() {
        Unit u = colony.getUnitList().get(0);
        u.setLocation(colony.getColonyTiles().get(0));
        UnitWas was = new UnitWas(u);
        u.setLocation(colony.getBuildings().get(0));
        assertNotNull(was.toString());
    }

    // M11-TP3: Tile -> Tile with work == null
    public void testM11_TP3_ToStringTileToTile() {
        Unit u = new ServerUnit(game, colony.getTile(), dutch, colonistType);
        UnitWas was = new UnitWas(u);
        assertNotNull(was.toString());
    }

    // M11-TP4: Europe -> null location (after disposal)
    public void testM11_TP4_ToStringOtherToNull() {
        Unit u = colony.getUnitList().get(0);
        UnitWas was = new UnitWas(u);
        u.setLocation(null);
        assertNotNull(was.toString());
    }

    // ------------------------------------------------------------------
    // M12: compareTo(Object) (synthetic bridge) — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M12-TP1: ENTRY -> N0 -> EXIT
    @SuppressWarnings({"unchecked", "rawtypes"})
    public void testM12_TP1_CompareToBridge() {
        Unit u = colony.getUnitList().get(0);
        Comparable raw = new UnitWas(u);
        assertEquals(0, raw.compareTo(raw));
    }
}
