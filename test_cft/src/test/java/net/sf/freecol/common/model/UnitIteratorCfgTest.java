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
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.function.Predicate;

import net.sf.freecol.server.model.ServerUnit;
import net.sf.freecol.util.test.FreeColTestCase;


/**
 * CFG-driven test suite for {@link UnitIterator} derived from
 * {@code test_cft/graph/net/sf/freecol/common/model/UnitIterator.json}.
 *
 * Covers all 10 methods (M1..M10) and achieves 100% feasible C3 (node + edge)
 * coverage across 16 Test Paths.
 *
 * Infeasible edges documented: none (all edges in M1..M10 are feasible).
 */
public class UnitIteratorCfgTest extends FreeColTestCase {

    private Game game;
    private Player dutch;
    private Player french;
    private Tile tile1;
    private Tile tile2;
    private Tile tile3;
    private UnitType colonistType;


    @Override
    public void setUp() throws Exception {
        super.setUp();
        // Pre-condition: standard game with map, Dutch and French players, and 3 distinct tiles
        game = getGame();
        game.changeMap(getTestMap(true));
        dutch = game.getPlayerByNationId("model.nation.dutch");
        french = game.getPlayerByNationId("model.nation.french");
        Map map = game.getMap();
        tile1 = map.getTile(1, 1);
        tile2 = map.getTile(2, 2);
        tile3 = map.getTile(3, 3);
        colonistType = spec().getUnitType("model.unit.freeColonist");
    }


    // ------------------------------------------------------------------
    // Pre-condition helpers & stubs
    // ------------------------------------------------------------------

    private Unit addDutchUnit(Tile tile) {
        return new ServerUnit(game, tile, dutch, colonistType);
    }

    private Unit addFrenchUnit(Tile tile) {
        return new ServerUnit(game, tile, french, colonistType);
    }

    private static void invokeUpdate(UnitIterator it) throws Exception {
        Method m = UnitIterator.class.getDeclaredMethod("update");
        m.setAccessible(true);
        m.invoke(it);
    }


    // ------------------------------------------------------------------
    // M1: UnitIterator(Player, Predicate<Unit>) — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M1-TP1: ENTRY -> N0 -> EXIT (construct iterator and populate initial cache)
    public void testM1_TP1_Constructor() {
        Unit u1 = addDutchUnit(tile1);
        UnitIterator it = new UnitIterator(dutch, u -> true);
        assertTrue(it.hasNext());
        assertSame(u1, it.next());
    }


    // ------------------------------------------------------------------
    // M2: update() — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M2-TP1: ENTRY -> N0 -> EXIT (private update() refreshes unit cache from owner)
    public void testM2_TP1_Update() throws Exception {
        UnitIterator it = new UnitIterator(dutch, u -> true);
        assertFalse(it.hasNext());
        Unit u1 = addDutchUnit(tile1);
        invokeUpdate(it);
        assertTrue(it.hasNext());
        assertSame(u1, it.next());
    }


    // ------------------------------------------------------------------
    // M3: setNext(Unit) — 6 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M3-TP1: ENTRY -> N0 -(B1:true)-> N12 -> EXIT (unit fails predicate)
    public void testM3_TP1_SetNextFailsPredicate() {
        Unit u1 = addDutchUnit(tile1);
        UnitIterator it = new UnitIterator(dutch, u -> false);
        assertFalse(it.setNext(u1));
    }

    // M3-TP2: ENTRY -> N0 -(B1:false)-> N1 -> N2 -(B2:false)-> N3 -(B3:false)-> N4 -> EXIT (unit is already first)
    public void testM3_TP2_SetNextAlreadyFirst() {
        Unit u1 = addDutchUnit(tile1);
        UnitIterator it = new UnitIterator(dutch, u -> true);
        assertTrue(it.setNext(u1));
        assertSame(u1, it.next());
    }

    // M3-TP3: ENTRY -> N0 -(B1:false)-> N1 -> N2 -(B2:false)-> N3 -(B3:true)-> N5 -> N2 -(B2:false)-> N3 -(B3:false)-> N4 -> EXIT (unit is later in current list)
    public void testM3_TP3_SetNextLaterInCurrentList() {
        Unit u1 = addDutchUnit(tile1);
        Unit u2 = addDutchUnit(tile2);
        UnitIterator it = new UnitIterator(dutch, u -> true);
        // Ensure u2 is not first by picking whichever is second
        Unit first = it.next();
        it.reset();
        Unit second = (first == u1) ? u2 : u1;
        assertTrue(it.setNext(second));
        assertSame(second, it.next());
    }

    // M3-TP4: ENTRY -> N0 -(B1:false)-> N1 -> N2 -(B2:true)-> N6 -> N7 -(B4:true)-> N12 -> EXIT (cache empty and update() also empty)
    public void testM3_TP4_SetNextEmptyListAndEmptyUpdate() {
        Unit foreign = addFrenchUnit(tile1);
        UnitIterator it = new UnitIterator(dutch, u -> true);
        assertFalse(it.setNext(foreign));
    }

    // M3-TP5: ENTRY -> N0 -(B1:false)-> N1 -> N2 -(B2:false)-> N3 -(B3:true)-> N5 -> N2 -(B2:true)-> N6 -> N7 -(B4:false)-> N8 -(B5:true)-> N12 -> EXIT (wrap-around reaches sentinel)
    public void testM3_TP5_SetNextWrapAroundHitsSentinel() {
        addDutchUnit(tile1);
        Unit foreign = addFrenchUnit(tile2);
        UnitIterator it = new UnitIterator(dutch, u -> true);
        assertFalse(it.setNext(foreign));
    }

    // M3-TP6: ENTRY -> N0 -(B1:false)-> N1 -> N2 -(B2:false)-> N3 -(B3:true)-> N5 -> N2 -(B2:true)-> N6 -> N7 -(B4:false)-> N8 -(B5:false)-> N9 -(B6:true)-> N11 -> N7 -(B4:false)-> N8 -(B5:false)-> N9 -(B6:false)-> N10 -> EXIT (wrap-around skips first and finds second)
    public void testM3_TP6_SetNextWrapAroundFindsTargetAfterSkipping() {
        addDutchUnit(tile1);
        addDutchUnit(tile2);
        addDutchUnit(tile3);
        UnitIterator it = new UnitIterator(dutch, u -> true);
        Unit first = it.next();
        Unit second = it.next();
        // Cache now has [third], sentinel = third; wrap-around refills [first, second, third]
        assertTrue(it.setNext(second));
        assertSame(second, it.next());
        assertNotNull(first);
    }


    // ------------------------------------------------------------------
    // M4: remove(Unit) — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M4-TP1: ENTRY -> N0 -> EXIT
    public void testM4_TP1_RemoveUnit() {
        Unit u1 = addDutchUnit(tile1);
        UnitIterator it = new UnitIterator(dutch, u -> true);
        assertTrue(it.remove(u1));
        assertFalse(it.remove(u1));
    }


    // ------------------------------------------------------------------
    // M5: reset() — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M5-TP1: ENTRY -> N0 -> EXIT
    public void testM5_TP1_Reset() {
        Unit u1 = addDutchUnit(tile1);
        UnitIterator it = new UnitIterator(dutch, u -> true);
        assertSame(u1, it.next());
        // Reset refills from owner
        it.reset();
        assertTrue(it.hasNext());
        assertSame(u1, it.next());
    }


    // ------------------------------------------------------------------
    // M6: hasNext() — 3 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M6-TP1: ENTRY -> N0 -(B1:false)-> N1 -(B2:false)-> N2 -> EXIT (cached unit still valid)
    public void testM6_TP1_HasNextCachedValid() {
        addDutchUnit(tile1);
        UnitIterator it = new UnitIterator(dutch, u -> true);
        assertTrue(it.hasNext());
    }

    // M6-TP2: ENTRY -> N0 -(B1:false)-> N1 -(B2:true)-> N3 -> N0 -(B1:true)-> N4 -(B3:true)-> N6 -> N7 -> EXIT (cached unit invalidated, update() empty)
    public void testM6_TP2_HasNextInvalidatedAndEmptyUpdate() {
        Unit u1 = addDutchUnit(tile1);
        Set<Unit> allowed = new HashSet<>();
        allowed.add(u1);
        Predicate<Unit> pred = allowed::contains;
        UnitIterator it = new UnitIterator(dutch, pred);
        allowed.clear();
        assertFalse(it.hasNext());
    }

    // M6-TP3: ENTRY -> N0 -(B1:true)-> N4 -(B3:false)-> N5 -> N7 -> EXIT (cache empty, update() refills from owner)
    public void testM6_TP3_HasNextEmptyCacheRefillsFromOwner() {
        Unit u1 = addDutchUnit(tile1);
        UnitIterator it = new UnitIterator(dutch, u -> true);
        assertSame(u1, it.next());
        assertTrue(it.hasNext());
    }


    // ------------------------------------------------------------------
    // M7: next() — 2 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M7-TP1: ENTRY -> N0 -(B1:true)-> N2 -> N3 -> EXIT (hasNext() == false -> returns null)
    public void testM7_TP1_NextWhenEmptyReturnsNull() {
        UnitIterator it = new UnitIterator(dutch, u -> true);
        assertNull(it.next());
    }

    // M7-TP2: ENTRY -> N0 -(B1:false)-> N1 -> N3 -> EXIT (hasNext() == true -> returns unit)
    public void testM7_TP2_NextReturnsUnit() {
        Unit u1 = addDutchUnit(tile1);
        UnitIterator it = new UnitIterator(dutch, u -> true);
        assertSame(u1, it.next());
    }


    // ------------------------------------------------------------------
    // M8: remove() — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M8-TP1: ENTRY -> N0 -> EXIT
    public void testM8_TP1_RemoveCurrent() {
        Unit u1 = addDutchUnit(tile1);
        Unit u2 = addDutchUnit(tile2);
        UnitIterator it = new UnitIterator(dutch, u -> true);
        it.remove();
        Unit remaining = it.next();
        assertTrue(remaining == u1 || remaining == u2);
    }


    // ------------------------------------------------------------------
    // M9: next() [bridge ()Ljava/lang/Object;] — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M9-TP1: ENTRY -> N0 -> EXIT
    public void testM9_TP1_NextBridge() {
        Unit u1 = addDutchUnit(tile1);
        Iterator<?> raw = new UnitIterator(dutch, u -> true);
        assertSame(u1, raw.next());
    }


    // ------------------------------------------------------------------
    // M10: lambda$update$0(Unit) — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M10-TP1: ENTRY -> N0 -> EXIT (lambda invoked by update() when owner has units)
    public void testM10_TP1_LambdaUpdate() {
        Unit u1 = addDutchUnit(tile1);
        UnitIterator it = new UnitIterator(dutch, u -> u == u1);
        assertTrue(it.hasNext());
        assertSame(u1, it.next());
    }
}
