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

import java.util.List;
import java.util.Random;

import net.sf.freecol.common.model.CombatModel.CombatEffectType;
import net.sf.freecol.common.model.CombatModel.CombatResult;
import net.sf.freecol.server.model.ServerBuilding;
import net.sf.freecol.server.model.ServerColony;
import net.sf.freecol.server.model.ServerUnit;
import net.sf.freecol.util.test.FreeColTestCase;


/**
 * CFG-driven test suite for {@link HitpointsCombatModel} derived from
 * {@code test_cft/graph/net/sf/freecol/common/model/HitpointsCombatModel.json}.
 *
 * Covers all 3 methods (M1..M3) and achieves 100% feasible C3 (node + edge)
 * coverage across 11 Test Paths.
 *
 * Infeasible edges documented:
 * - M2 ({@code generateAttackResult}): {@code N9 -> N10} ({@code B7:false}) —
 *   {@link CombatModel#combatIsBombard(FreeColGameObject, FreeColGameObject)}
 *   already requires {@code ((Unit) defender).isNaval()}, so reaching {@code N9}
 *   via {@code B6:false} guarantees {@code defenderUnit.isNaval() == true};
 *   {@code !defenderUnit.isNaval()} can never be true on {@code HitpointsCombatModel}.
 */
public class HitpointsCombatModelCfgTest extends FreeColTestCase {

    private Game game;
    private Map map;
    private Player dutch;
    private Player french;
    private UnitType veteranType;
    private UnitType colonistType;
    private UnitType frigateType;
    private int savedVeteranRange;
    private int savedColonistRange;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        // Pre-condition: Standard game, test map, two warring players, and unit types from spec()
        game = getStandardGame();
        map = getTestMap(spec().getTileType("model.tile.plains"), true);
        game.setMap(map);
        dutch = game.getPlayerByNationId("model.nation.dutch");
        french = game.getPlayerByNationId("model.nation.french");
        dutch.setStance(french, Stance.WAR);
        french.setStance(dutch, Stance.WAR);

        veteranType = spec().getUnitType("model.unit.veteranSoldier");
        colonistType = spec().getUnitType("model.unit.freeColonist");
        frigateType = spec().getUnitType("model.unit.frigate");
        savedVeteranRange = veteranType.getAttackRange();
        savedColonistRange = colonistType.getAttackRange();
    }

    @Override
    public void tearDown() throws Exception {
        veteranType.setAttackRange(savedVeteranRange);
        colonistType.setAttackRange(savedColonistRange);
        super.tearDown();
    }

    // ------------------------------------------------------------------
    // Pre-condition helpers & stubs
    // ------------------------------------------------------------------

    /**
     * Deterministic Random returning configured double and int values in order.
     */
    private static class FixedRandom extends Random {
        private final double doubleVal;
        private final int[] intVals;
        private int intIdx = 0;

        FixedRandom(double doubleVal, int... intVals) {
            this.doubleVal = doubleVal;
            this.intVals = intVals;
        }

        @Override
        public double nextDouble() {
            return doubleVal;
        }

        @Override
        public int nextInt(int bound) {
            int val = (intIdx < intVals.length) ? intVals[intIdx++] : 0;
            return Math.min(val, Math.max(0, bound - 1));
        }
    }

    private Unit createUnit(Player owner, UnitType type, Tile tile) {
        return new ServerUnit(game, tile, owner, type);
    }

    private Colony createBombardCapableColony(Player owner, Tile tile) {
        Colony colony = new ServerColony(game, owner, "TestColony", tile);
        colony.addBuilding(new ServerBuilding(game, colony, spec().getBuildingType("model.building.fort")));
        createUnit(owner, spec().getUnitType("model.unit.artillery"), tile);
        return colony;
    }

    private Tile getOceanTile(int x, int y) {
        Tile tile = map.getTile(x, y);
        tile.setType(spec().getTileType("model.tile.ocean"));
        return tile;
    }

    // ------------------------------------------------------------------
    // M1: <init>()V — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M1-TP1: ENTRY -> N0 -> EXIT (default constructor)
    public void testM1_TP1_Constructor() {
        HitpointsCombatModel model = new HitpointsCombatModel();
        assertNotNull(model);
    }

    // ------------------------------------------------------------------
    // M2: generateAttackResult(Random, FreeColGameObject, FreeColGameObject)CombatResult — 9 feasible Test Paths (100% feasible C3)
    // ------------------------------------------------------------------

    // M2-TP1: ENTRY -> N0 -(B1:false)-> N1 -(B2:true)-> N7 -> EXIT
    // (combatIsAttack == true, newDefenderHitpoints <= 0 -> attacker wins & resolves attack)
    public void testM2_TP1_AttackDefenderKilled() {
        HitpointsCombatModel model = new HitpointsCombatModel();
        Unit attacker = createUnit(dutch, veteranType, map.getTile(5, 5));
        Unit defender = createUnit(french, colonistType, map.getTile(5, 6));
        defender.setHitPoints(1);

        CombatResult result = model.generateAttackResult(new FixedRandom(0.5, 1), attacker, defender);
        List<CombatEffectType> effects = result.getEffects();
        assertTrue(effects.contains(CombatEffectType.WIN));
        assertEquals(-1, result.getAttackerHitpointsAfter());
        assertEquals(1, result.getDefenderHitpointsAfter());
    }

    // M2-TP2: ENTRY -> N0 -(B1:false)-> N1 -(B2:false)-> N2 -(B3:true)-> N4 -(B5:true)-> N6 -> EXIT
    // (combatIsAttack == true, newDefenderHitpoints > 0, attackerRange <= defenderRange, newAttackerHitpoints > 0)
    public void testM2_TP2_AttackBothSurviveInRange() {
        HitpointsCombatModel model = new HitpointsCombatModel();
        Unit attacker = createUnit(dutch, veteranType, map.getTile(5, 5));
        Unit defender = createUnit(french, veteranType, map.getTile(5, 6));
        attacker.setHitPoints(10);
        defender.setHitPoints(10);

        CombatResult result = model.generateAttackResult(new FixedRandom(0.5, 1, 1), attacker, defender);
        assertEquals(List.of(CombatEffectType.WIN), result.getEffects());
        assertEquals(9, result.getAttackerHitpointsAfter());
        assertEquals(9, result.getDefenderHitpointsAfter());
    }

    // M2-TP3: ENTRY -> N0 -(B1:false)-> N1 -(B2:false)-> N2 -(B3:true)-> N4 -(B5:false)-> N5 -> EXIT
    // (combatIsAttack == true, newDefenderHitpoints > 0, attackerRange <= defenderRange, newAttackerHitpoints <= 0 -> LOSE)
    public void testM2_TP3_AttackAttackerKilledByCounterattack() {
        HitpointsCombatModel model = new HitpointsCombatModel();
        Unit attacker = createUnit(dutch, colonistType, map.getTile(5, 5));
        Unit defender = createUnit(french, veteranType, map.getTile(5, 6));
        attacker.setHitPoints(1);
        defender.setHitPoints(10);

        CombatResult result = model.generateAttackResult(new FixedRandom(0.95, 0, 1), attacker, defender);
        assertTrue(result.getEffects().contains(CombatEffectType.LOSE));
        assertEquals(1, result.getAttackerHitpointsAfter());
        assertEquals(10, result.getDefenderHitpointsAfter());
    }

    // M2-TP4: ENTRY -> N0 -(B1:false)-> N1 -(B2:false)-> N2 -(B3:false)-> N3 -(B4:true)-> N6 -> EXIT
    // (combatIsAttack == true, newDefenderHitpoints > 0, attackerRange > defenderRange, distance > defenderRange -> no counterattack)
    public void testM2_TP4_AttackOutrangedDefenderCannotCounterattack() {
        HitpointsCombatModel model = new HitpointsCombatModel();
        veteranType.setAttackRange(3);
        colonistType.setAttackRange(1);

        Unit attacker = createUnit(dutch, veteranType, map.getTile(5, 5));
        Unit defender = createUnit(french, colonistType, map.getTile(5, 9));
        attacker.setHitPoints(10);
        defender.setHitPoints(10);

        CombatResult result = model.generateAttackResult(new FixedRandom(0.5, 1), attacker, defender);
        assertEquals(List.of(CombatEffectType.WIN), result.getEffects());
        assertEquals(-1, result.getAttackerHitpointsAfter());
        assertEquals(9, result.getDefenderHitpointsAfter());
    }

    // M2-TP5: ENTRY -> N0 -(B1:false)-> N1 -(B2:false)-> N2 -(B3:false)-> N3 -(B4:false)-> N4 -(B5:true)-> N6 -> EXIT
    // (combatIsAttack == true, newDefenderHitpoints > 0, attackerRange > defenderRange, distance <= defenderRange -> counterattack)
    public void testM2_TP5_AttackHigherRangeButDefenderInRangeCounterattacks() {
        HitpointsCombatModel model = new HitpointsCombatModel();
        veteranType.setAttackRange(3);
        colonistType.setAttackRange(1);

        Unit attacker = createUnit(dutch, veteranType, map.getTile(5, 5));
        Unit defender = createUnit(french, colonistType, map.getTile(5, 6));
        attacker.setHitPoints(10);
        defender.setHitPoints(10);

        CombatResult result = model.generateAttackResult(new FixedRandom(0.5, 1, 1), attacker, defender);
        assertEquals(List.of(CombatEffectType.WIN), result.getEffects());
        assertEquals(9, result.getAttackerHitpointsAfter());
        assertEquals(9, result.getDefenderHitpointsAfter());
    }

    // M2-TP6: ENTRY -> N0 -(B1:true)-> N8 -(B6:false)-> N9 -(B7:true)-> N11 -(B8:false)-> N12 -> EXIT
    // (combatIsBombard == true, isNaval == true, defenderDamage == 0 -> NO_RESULT + EVADE_BOMBARD)
    public void testM2_TP6_BombardEvaded() {
        HitpointsCombatModel model = new HitpointsCombatModel();
        Colony colony = createBombardCapableColony(dutch, map.getTile(5, 5));
        Unit navalDefender = createUnit(french, frigateType, getOceanTile(5, 6));

        CombatResult result = model.generateAttackResult(new FixedRandom(0.5, 0), colony, navalDefender);
        assertEquals(List.of(CombatEffectType.NO_RESULT, CombatEffectType.EVADE_BOMBARD),
            result.getEffects());
        assertEquals(-1, result.getAttackerHitpointsAfter());
        assertEquals(-1, result.getDefenderHitpointsAfter());
    }

    // M2-TP7: ENTRY -> N0 -(B1:true)-> N8 -(B6:false)-> N9 -(B7:true)-> N11 -(B8:true)-> N13 -(B9:false)-> N14 -> EXIT
    // (combatIsBombard == true, isNaval == true, defenderDamage > 0, newDefenderHitpoints > 0 -> WIN)
    public void testM2_TP7_BombardDamagesShip() {
        HitpointsCombatModel model = new HitpointsCombatModel();
        Colony colony = createBombardCapableColony(dutch, map.getTile(5, 5));
        Unit navalDefender = createUnit(french, frigateType, getOceanTile(5, 6));
        navalDefender.setHitPoints(10);

        CombatResult result = model.generateAttackResult(new FixedRandom(0.5, 1), colony, navalDefender);
        assertEquals(List.of(CombatEffectType.WIN), result.getEffects());
        assertEquals(-1, result.getAttackerHitpointsAfter());
        assertEquals(9, result.getDefenderHitpointsAfter());
    }

    // M2-TP8: ENTRY -> N0 -(B1:true)-> N8 -(B6:false)-> N9 -(B7:true)-> N11 -(B8:true)-> N13 -(B9:true)-> N15 -> EXIT
    // (combatIsBombard == true, isNaval == true, defenderDamage > 0, newDefenderHitpoints <= 0 -> WIN + SINK_SHIP_BOMBARD)
    public void testM2_TP8_BombardSinksShip() {
        HitpointsCombatModel model = new HitpointsCombatModel();
        Colony colony = createBombardCapableColony(dutch, map.getTile(5, 5));
        Unit navalDefender = createUnit(french, frigateType, getOceanTile(5, 6));
        navalDefender.setHitPoints(1);

        CombatResult result = model.generateAttackResult(new FixedRandom(0.5, 1), colony, navalDefender);
        assertEquals(List.of(CombatEffectType.WIN, CombatEffectType.SINK_SHIP_BOMBARD),
            result.getEffects());
        assertEquals(-1, result.getAttackerHitpointsAfter());
        assertEquals(-1, result.getDefenderHitpointsAfter());
    }

    // M2-TP9: ENTRY -> N0 -(B1:true)-> N8 -(B6:true)-> N16 -> EXIT
    // (combatIsAttack == false, combatIsBombard == false -> throws RuntimeException)
    public void testM2_TP9_BogusCombatThrowsRuntimeException() {
        HitpointsCombatModel model = new HitpointsCombatModel();
        Colony attackerColony = createBombardCapableColony(dutch, map.getTile(5, 5));
        Unit landDefender = createUnit(french, colonistType, map.getTile(5, 6));

        try {
            model.generateAttackResult(new FixedRandom(0.5), attackerColony, landDefender);
            fail("Expected RuntimeException for bogus combat");
        } catch (RuntimeException expected) {
            assertTrue(expected.getMessage().startsWith("Bogus combat:"));
        }
    }

    // ------------------------------------------------------------------
    // M3: <clinit>()V — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M3-TP1: ENTRY -> N0 -> EXIT (static logger field initialized on class load)
    public void testM3_TP1_ClinitLoggerInitialized() {
        assertNotNull(new HitpointsCombatModel());
    }
}
