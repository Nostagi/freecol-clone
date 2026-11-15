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

import java.util.Collections;
import java.util.List;

import net.sf.freecol.server.model.ServerUnit;
import net.sf.freecol.util.test.FreeColTestCase;


/**
 * CFG-driven test suite for {@link ColonyWas} derived from
 * {@code test_cft/graph/net/sf/freecol/common/model/ColonyWas.json}.
 *
 * Covers all 2 methods (M1..M2, including {@code <init>}) and achieves 100% C3
 * (node + edge) coverage across 5 Test Paths.
 */
public class ColonyWasCfgTest extends FreeColTestCase {

    private Game game;
    private Colony colony;
    private GoodsType foodType;
    private UnitType colonistType;
    private BuildingType churchType;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        // Pre-condition: Initialize standard game, map, colony, and spec types
        game = getStandardGame();
        game.setMap(getTestMap());
        colony = createStandardColony(3);
        foodType = spec().getGoodsType("model.goods.food");
        colonistType = spec().getUnitType("model.unit.freeColonist");
        churchType = spec().getBuildingType("model.building.chapel");
    }

    // ------------------------------------------------------------------
    // Pre-condition helpers & stubs
    // ------------------------------------------------------------------

    /**
     * Stub Colony returning {@code null} for {@code getGoodsContainer()} to
     * exercise {@code B4:true} in {@link ColonyWas#fireChanges()}.
     */
    private static class StubNullGoodsColony extends Colony {
        private int unitCount;
        private int bonus;
        private List<BuildableType> queue;

        StubNullGoodsColony(Game game, String id, int unitCount, int bonus,
                            List<BuildableType> queue) {
            super(game, id);
            this.unitCount = unitCount;
            this.bonus = bonus;
            this.queue = queue;
            setGoodsContainer(null);
        }

        @Override
        public int getUnitCount() {
            return unitCount;
        }

        @Override
        public int getProductionBonus() {
            return bonus;
        }

        @Override
        public List<BuildableType> getBuildQueue() {
            return queue;
        }
    }

    // ------------------------------------------------------------------
    // M1: <init>(Lnet/sf/freecol/common/model/Colony;)V — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M1-TP1: ENTRY -> N0 -> EXIT (snapshots colony population, bonus, and buildQueue)
    public void testM1_TP1_Constructor() {
        ColonyWas was = new ColonyWas(colony);
        assertFalse(was.fireChanges());
    }

    // ------------------------------------------------------------------
    // M2: fireChanges()Z — 4 Test Paths covering all edges & branches (100% C3)
    // ------------------------------------------------------------------

    // M2-TP1: ENTRY -> N0 -(B1:true)-> N2 -(B2:true)-> N4 -(B3:true)-> N6 -(B4:false)-> N7 -> N8 -> EXIT (no changes, non-null GoodsContainer)
    public void testM2_TP1_FireChangesUnchangedWithGoodsContainer() {
        ColonyWas was = new ColonyWas(colony);
        assertFalse(was.fireChanges());
    }

    // M2-TP2: ENTRY -> N0 -(B1:true)-> N2 -(B2:true)-> N4 -(B3:true)-> N6 -(B4:true)-> N8 -> EXIT (no changes, null GoodsContainer)
    public void testM2_TP2_FireChangesUnchangedNullGoodsContainer() {
        StubNullGoodsColony stub = new StubNullGoodsColony(
            game, "colony:stub1", 3, 0, Collections.<BuildableType>emptyList());
        ColonyWas was = new ColonyWas(stub);
        assertFalse(was.fireChanges());
    }

    // M2-TP3: ENTRY -> N0 -(B1:false)-> N1 -> N2 -(B2:false)-> N3 -> N4 -(B3:false)-> N5 -> N6 -(B4:false)-> N7 -> N8 -> EXIT (all three colony properties changed)
    public void testM2_TP3_FireChangesAllChanged() {
        ColonyWas was = new ColonyWas(colony);
        // Change population and production bonus (by adding colonists), and build queue
        for (int i = 0; i < 6; i++) {
            new ServerUnit(game, colony.getTile(), colony.getOwner(), colonistType)
                .setLocation(colony);
        }
        colony.setCurrentlyBuilding(churchType);
        colony.addGoods(foodType, 10);
        assertTrue(was.fireChanges());
    }

    // M2-TP4: ENTRY -> N0 -(B1:true)-> N2 -(B2:true)-> N4 -(B3:false)-> N5 -> N6 -(B4:true)-> N8 -> EXIT (buildQueue changed only, null GoodsContainer)
    public void testM2_TP4_FireChangesBuildQueueOnlyNullGoodsContainer() {
        StubNullGoodsColony stub = new StubNullGoodsColony(
            game, "colony:stub2", 2, 0, Collections.<BuildableType>emptyList());
        ColonyWas was = new ColonyWas(stub);
        stub.queue = Collections.<BuildableType>singletonList(churchType);
        assertTrue(was.fireChanges());
    }
}
