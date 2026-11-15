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

import net.sf.freecol.server.model.ServerUnit;
import net.sf.freecol.util.test.FreeColTestCase;


/**
 * CFG-driven test suite for {@link EuropeWas} derived from
 * {@code test_cft/graph/net/sf/freecol/common/model/EuropeWas.json}.
 *
 * Covers all 3 methods (M1..M3, including {@code <init>}) and achieves 100% C3
 * (node + edge) coverage across 5 Test Paths.
 */
public class EuropeWasCfgTest extends FreeColTestCase {

    private Game game;
    private Player dutchPlayer;
    private Europe europe;
    private UnitType colonistType;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        // Pre-condition: Initialize standard game, Dutch player, Europe, and freeColonist type
        game = getStandardGame();
        dutchPlayer = game.getPlayerByNationId("model.nation.dutch");
        europe = dutchPlayer.getEurope();
        colonistType = spec().getUnitType("model.unit.freeColonist");
    }

    // ------------------------------------------------------------------
    // Pre-condition helpers & stubs
    // ------------------------------------------------------------------

    // ------------------------------------------------------------------
    // M1: <init>(Lnet/sf/freecol/common/model/Europe;)V — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M1-TP1: ENTRY -> N0 -> EXIT (snapshots europe and initial unitCount)
    public void testM1_TP1_Constructor() {
        EuropeWas was = new EuropeWas(europe);
        assertNull(was.getNewUnit());
    }

    // ------------------------------------------------------------------
    // M2: getNewUnit()Lnet/sf/freecol/common/model/Unit; — 2 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M2-TP1: ENTRY -> N0 -(B1:false)-> N1 -> N3 -> EXIT (europe.getUnitCount() <= unitCount returns null)
    public void testM2_TP1_GetNewUnitNoneAdded() {
        EuropeWas was = new EuropeWas(europe);
        assertNull(was.getNewUnit());
    }

    // M2-TP2: ENTRY -> N0 -(B1:true)-> N2 -> N3 -> EXIT (europe.getUnitCount() > unitCount returns newest unit)
    public void testM2_TP2_GetNewUnitAdded() {
        EuropeWas was = new EuropeWas(europe);
        Unit added = new ServerUnit(game, europe, dutchPlayer, colonistType);
        Unit newest = was.getNewUnit();
        assertNotNull(newest);
        assertEquals(added.getId(), newest.getId());
    }

    // ------------------------------------------------------------------
    // M3: fireChanges()Z — 2 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M3-TP1: ENTRY -> N0 -(B1:true)-> N2 -> EXIT (newCount == unitCount returns false)
    public void testM3_TP1_FireChangesUnchanged() {
        EuropeWas was = new EuropeWas(europe);
        assertFalse(was.fireChanges());
    }

    // M3-TP2: ENTRY -> N0 -(B1:false)-> N1 -> EXIT (newCount != unitCount fires property change and returns true)
    public void testM3_TP2_FireChangesChanged() {
        EuropeWas was = new EuropeWas(europe);
        new ServerUnit(game, europe, dutchPlayer, colonistType);
        assertTrue(was.fireChanges());
    }
}
