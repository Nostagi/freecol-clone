package net.sf.freecol.common.model.pathfinding;

import static net.sf.freecol.common.model.Constants.INFINITY;
import static net.sf.freecol.common.model.pathfinding.CostDecider.ILLEGAL_MOVE;

import net.sf.freecol.common.model.Ability;
import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.Location;
import net.sf.freecol.common.model.LostCityRumour;
import net.sf.freecol.common.model.Map;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Stance;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.TradeRoute;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.server.model.ServerUnit;
import net.sf.freecol.util.test.FreeColTestCase;
import net.sf.freecol.util.test.FreeColTestUtils;

public class CostDecidersCfgTest extends FreeColTestCase {
    private Game game;
    private Map map;
    private Player dutch;
    private Player french;
    private Unit colonist;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        game = getStandardGame();
        map = getTestMap(spec().getTileType("model.tile.plains"), true);
        game.changeMap(map);
        dutch = game.getPlayerByNationId("model.nation.dutch");
        french = game.getPlayerByNationId("model.nation.french");
        colonist = unit("freeColonist", dutch, tile(8, 7));
    }

    private Tile tile(int x, int y) {
        return map.getTile(x, y);
    }

    private Unit unit(String type, Player owner, Location location) {
        return new ServerUnit(game, location, owner,
                spec().getUnitType("model.unit." + type));
    }

    private Tile sea(Tile tile) {
        tile.setType(spec().getTileType("model.tile.ocean"));
        tile.setHighSeasCount(2);
        tile.setMoveToEurope(true);
        return tile;
    }

    private Colony colony(Player owner, Tile tile) {
        return FreeColTestUtils.getColonyBuilder().player(owner)
                .colonyTile(tile).initialColonists(1).build();
    }

    private void state(CostDecider cd, int moves, int turns) {
        assertEquals(moves, cd.getMovesLeft());
        assertEquals(turns, cd.getNewTurns());
    }

    private void voyage(CostDecider cd) {
        colonist.setLocation(dutch.getEurope());
        Unit ship = unit("caravel", dutch, sea(tile(8, 7)));
        int turns = ship.getSailTurns();
        int moves = ship.getInitialMovesLeft();
        assertEquals(turns * moves,
                cd.getCost(ship, ship.getTile(), dutch.getEurope(), 6));
        state(cd, moves, turns);
    }

    private Unit ship() {
        colonist.setLocation(dutch.getEurope());
        sea(tile(8, 8));
        return unit("caravel", dutch, sea(tile(8, 7)));
    }

    private Tile adjacent(Tile center) {
        for (Tile t : center.getSurroundingTiles(1, 1)) {
            if (t != tile(8, 7)) return t;
        }
        throw new IllegalStateException("Missing adjacent tile");
    }

    private void war() {
        dutch.setStance(french, Stance.WAR);
        french.setStance(dutch, Stance.WAR);
    }

    private void fort() {
        Colony fort = colony(french, adjacent(tile(8, 8)));
        fort.addAbility(new Ability(Ability.BOMBARD_SHIPS));
        war();
    }

    private static final class FixedCost implements CostDecider {
        private int cost;
        private int moves;
        private int turns;

        FixedCost(int cost, int moves, int turns) {
            this.cost = cost;
            this.moves = moves;
            this.turns = turns;
        }

        public int getCost(Unit unit, Location oldLocation,
                           Location newLocation, int movesLeft) {
            return cost;
        }

        public int getMovesLeft() {
            return moves;
        }

        public int getNewTurns() {
            return turns;
        }
    }

    public void testTC001TrivialNullDestination() {
        CostDecider cd = CostDeciders.numberOfTiles();
        assertEquals(ILLEGAL_MOVE, cd.getCost(null, null, null, 6));
        state(cd, 0, 0);
    }

    public void testTC002TrivialEurope() {
        assertEquals(1, CostDeciders.numberOfTiles()
                .getCost(null, null, dutch.getEurope(), 6));
    }

    public void testTC003TrivialLocationWithoutTile() {
        assertEquals(ILLEGAL_MOVE, CostDeciders.numberOfTiles()
                .getCost(null, null, dutch.getHighSeas(), 6));
    }

    public void testTC004TrivialIgnoresLegality() {
        assertEquals(1, CostDeciders.numberOfTiles()
                .getCost(colonist, tile(8, 7), sea(tile(8, 8)), 6));
    }

    public void testTC005TileCostNullSource() {
        assertEquals(ILLEGAL_MOVE, CostDeciders.tileCost()
                .getCost(colonist, null, tile(8, 8), 6));
    }

    public void testTC006TileCostNullDestination() {
        assertEquals(ILLEGAL_MOVE, CostDeciders.tileCost()
                .getCost(colonist, tile(8, 7), null, 6));
    }

    public void testTC007TileCostToEurope() {
        assertEquals(1, CostDeciders.tileCost()
                .getCost(colonist, tile(8, 7), dutch.getEurope(), 6));
    }

    public void testTC008TileCostFromEurope() {
        assertEquals(1, CostDeciders.tileCost()
                .getCost(colonist, dutch.getEurope(), tile(8, 8), 6));
    }

    public void testTC009TileCostDestinationWithoutTile() {
        assertEquals(ILLEGAL_MOVE, CostDeciders.tileCost()
                .getCost(colonist, tile(8, 7), dutch.getHighSeas(), 6));
    }

    public void testTC010TileCostSourceWithoutTile() {
        assertEquals(ILLEGAL_MOVE, CostDeciders.tileCost()
                .getCost(colonist, dutch.getHighSeas(), tile(8, 8), 6));
    }

    public void testTC011TileCostWithinTurn() {
        CostDecider cd = CostDeciders.tileCost();
        assertEquals(3, cd.getCost(colonist, tile(8, 7), tile(8, 8), 6));
        state(cd, 3, 0);
    }

    public void testTC012TileCostNextTurn() {
        CostDecider cd = CostDeciders.tileCost();
        assertEquals(3, cd.getCost(colonist, tile(8, 7), tile(8, 8), 0));
        state(cd, colonist.getInitialMovesLeft() - 3, 1);
    }

    public void testTC013LegalTilesNullDestination() {
        CostDecider cd = CostDeciders.numberOfLegalTiles();
        assertEquals(ILLEGAL_MOVE, cd.getCost(null, null, null, 6));
        state(cd, 0, 1);
    }

    public void testTC014LegalTilesEurope() {
        assertEquals(1, CostDeciders.numberOfLegalTiles()
                .getCost(null, null, dutch.getEurope(), 6));
    }

    public void testTC015LegalTilesLocationWithoutTile() {
        assertEquals(ILLEGAL_MOVE, CostDeciders.numberOfLegalTiles()
                .getCost(colonist, tile(8, 7), dutch.getHighSeas(), 6));
    }

    public void testTC016LegalTilesAccessibleLand() {
        assertEquals(1, CostDeciders.numberOfLegalTiles()
                .getCost(colonist, tile(8, 7), tile(8, 8), 6));
    }

    public void testTC017LegalTilesInaccessibleSea() {
        assertEquals(ILLEGAL_MOVE, CostDeciders.numberOfLegalTiles()
                .getCost(colonist, tile(8, 7), sea(tile(8, 8)), 6));
    }

    public void testTC018ServerIllegalBaseMove() {
        assertEquals(ILLEGAL_MOVE, CostDeciders.serverAvoidIllegal()
                .getCost(colonist, tile(8, 7), sea(tile(8, 8)), 6));
    }

    public void testTC019ServerEurope() {
        voyage(CostDeciders.serverAvoidIllegal());
    }

    public void testTC020ServerUnexploredByOwner() {
        Tile target = tile(8, 8);
        target.setExplored(dutch, false);
        assertEquals(ILLEGAL_MOVE, CostDeciders.serverAvoidIllegal()
                .getCost(colonist, tile(8, 7), target, 6));
    }

    public void testTC021ServerExploredByOwner() {
        CostDecider cd = CostDeciders.serverAvoidIllegal();
        assertEquals(3, cd.getCost(colonist, tile(8, 7), tile(8, 8), 6));
        state(cd, 3, 0);
    }

    public void testTC022AvoidSettlementsIllegalBaseMove() {
        assertEquals(ILLEGAL_MOVE, CostDeciders.avoidSettlements()
                .getCost(colonist, tile(8, 7), sea(tile(8, 8)), 6));
    }

    public void testTC023AvoidSettlementsEmptyTile() {
        CostDecider cd = CostDeciders.avoidSettlements();
        assertEquals(3, cd.getCost(colonist, tile(8, 7), tile(8, 8), 6));
        state(cd, 3, 0);
    }

    public void testTC024AvoidSettlementsOwnColony() {
        Colony own = colony(dutch, tile(8, 8));
        assertEquals(3, CostDeciders.avoidSettlements()
                .getCost(colonist, tile(8, 7), own, 6));
    }

    public void testTC025AvoidSettlementsEnemyColony() {
        Unit attacker = unit("artillery", dutch, tile(8, 7));
        Colony enemy = colony(french, tile(8, 8));
        assertEquals(ILLEGAL_MOVE, CostDeciders.avoidSettlements()
                .getCost(attacker, tile(8, 7), enemy, 6));
    }

    public void testTC026BlockingIllegalBaseMove() {
        assertEquals(ILLEGAL_MOVE, CostDeciders.avoidSettlementsAndBlockingUnits()
                .getCost(colonist, tile(8, 7), sea(tile(8, 8)), 6));
    }

    public void testTC027BlockingEurope() {
        voyage(CostDeciders.avoidSettlementsAndBlockingUnits());
    }

    public void testTC028BlockingEnemyUnit() {
        Unit attacker = unit("artillery", dutch, tile(8, 7));
        unit("freeColonist", french, tile(8, 8));
        assertEquals(ILLEGAL_MOVE, CostDeciders.avoidSettlementsAndBlockingUnits()
                .getCost(attacker, tile(8, 7), tile(8, 8), 6));
    }

    public void testTC029BlockingFriendlyUnit() {
        unit("freeColonist", dutch, tile(8, 8));
        assertEquals(3, CostDeciders.avoidSettlementsAndBlockingUnits()
                .getCost(colonist, tile(8, 7), tile(8, 8), 6));
    }

    public void testTC030BlockingTradeRouteRumour() {
        colonist.setTradeRoute(new TradeRoute(game, "C2 route", dutch));
        tile(8, 8).addLostCityRumour(new LostCityRumour(game, tile(8, 8)));
        assertEquals(ILLEGAL_MOVE, CostDeciders.avoidSettlementsAndBlockingUnits()
                .getCost(colonist, tile(8, 7), tile(8, 8), 6));
    }

    public void testTC031BlockingRumourWithoutRoute() {
        tile(8, 8).addLostCityRumour(new LostCityRumour(game, tile(8, 8)));
        CostDecider cd = CostDeciders.avoidSettlementsAndBlockingUnits();
        assertEquals(6, cd.getCost(colonist, tile(8, 7), tile(8, 8), 6));
        state(cd, 0, 0);
    }

    public void testTC032BlockingTradeRouteWithoutRumour() {
        colonist.setTradeRoute(new TradeRoute(game, "C2 route", dutch));
        assertEquals(3, CostDeciders.avoidSettlementsAndBlockingUnits()
                .getCost(colonist, tile(8, 7), tile(8, 8), 6));
    }

    public void testTC033NavalIllegalMove() {
        Unit ship = ship();
        assertEquals(ILLEGAL_MOVE, CostDeciders.avoidNavalDanger()
                .getCost(ship, tile(8, 7), tile(12, 8), 6));
    }

    public void testTC034NavalEurope() {
        voyage(CostDeciders.avoidNavalDanger());
    }

    public void testTC035NavalSafeSea() {
        Unit ship = ship();
        CostDecider cd = CostDeciders.avoidNavalDanger();
        assertEquals(3, cd.getCost(ship, tile(8, 7), tile(8, 8), 6));
        state(cd, 3, 0);
    }

    public void testTC036NavalAdjacentPirate() {
        Unit ship = ship();
        unit("privateer", french, sea(adjacent(tile(8, 8))));
        CostDecider cd = CostDeciders.avoidNavalDanger();
        assertEquals(6, cd.getCost(ship, tile(8, 7), tile(8, 8), 6));
        state(cd, 0, 1);
    }

    public void testTC037NavalEnemyWarshipAtWar() {
        Unit ship = ship();
        unit("frigate", french, sea(adjacent(tile(8, 8))));
        war();
        CostDecider cd = CostDeciders.avoidNavalDanger();
        assertEquals(6, cd.getCost(ship, tile(8, 7), tile(8, 8), 6));
        state(cd, 0, 1);
    }

    public void testTC038NavalWarshipAtPeace() {
        Unit ship = ship();
        unit("frigate", french, sea(adjacent(tile(8, 8))));
        dutch.setStance(french, Stance.PEACE);
        french.setStance(dutch, Stance.PEACE);
        CostDecider cd = CostDeciders.avoidNavalDanger();
        assertEquals(3, cd.getCost(ship, tile(8, 7), tile(8, 8), 6));
        state(cd, 3, 0);
    }

    public void testTC039NavalBombardmentOnly() {
        Unit ship = ship();
        fort();
        CostDecider cd = CostDeciders.avoidNavalDanger();
        assertEquals(6, cd.getCost(ship, tile(8, 7), tile(8, 8), 6));
        state(cd, 3, 0);
    }

    public void testTC040NavalThreatAndBombardment() {
        Unit ship = ship();
        fort();
        Tile threat = null;
        for (Tile t : tile(8, 8).getSurroundingTiles(1, 1)) {
            if (t != tile(8, 7) && t.getSettlement() == null) {
                threat = sea(t);
                break;
            }
        }
        assertNotNull(threat);
        unit("privateer", french, threat);
        CostDecider cd = CostDeciders.avoidNavalDanger();
        assertEquals(12, cd.getCost(ship, tile(8, 7), tile(8, 8), 6));
        state(cd, 0, 1);
    }

    public void testTC041NavalNonThreateningUnits() {
        Unit ship = ship();
        Tile neighbour = sea(adjacent(tile(8, 8)));
        unit("frigate", dutch, neighbour);
        for (Tile t : tile(8, 8).getSurroundingTiles(1, 1)) {
            if (t != neighbour && t != tile(8, 7)) {
                unit("freeColonist", french, t);
                break;
            }
        }
        CostDecider cd = CostDeciders.avoidNavalDanger();
        assertEquals(3, cd.getCost(ship, tile(8, 7), tile(8, 8), 6));
        state(cd, 3, 0);
    }

    public void testTC042CompositionEmptyList() {
        try {
            CostDeciders.getComposedCostDecider();
            fail("Expected RuntimeException");
        } catch (RuntimeException e) {
            assertEquals("Short CostDecider list: 0", e.getMessage());
        }
    }

    public void testTC043CompositionSingleDelegate() {
        try {
            CostDeciders.getComposedCostDecider(new FixedCost(3, 2, 1));
            fail("Expected RuntimeException");
        } catch (RuntimeException e) {
            assertEquals("Short CostDecider list: 1", e.getMessage());
        }
    }

    public void testTC044CompositionBeforeCost() {
        CostDecider cd = CostDeciders.getComposedCostDecider(
                new FixedCost(3, 2, 1), new FixedCost(7, 4, 2));
        state(cd, 0, 0);
    }

    public void testTC045CompositionMaximumAndTie() {
        CostDecider cd = CostDeciders.getComposedCostDecider(
                new FixedCost(3, 1, 0), new FixedCost(7, 2, 1),
                new FixedCost(7, 5, 3), new FixedCost(4, 6, 4));
        assertEquals(7, cd.getCost(null, null, null, 6));
        state(cd, 2, 1);
    }

    public void testTC046CompositionIllegalMoveDominates() {
        CostDecider cd = CostDeciders.getComposedCostDecider(
                new FixedCost(3, 2, 0), new FixedCost(ILLEGAL_MOVE, 4, 2),
                new FixedCost(9, 5, 3));
        assertEquals(ILLEGAL_MOVE, cd.getCost(null, null, null, 6));
        state(cd, 4, 2);
    }

    public void testTC047CompositionInfinityDominates() {
        CostDecider cd = CostDeciders.getComposedCostDecider(
                new FixedCost(INFINITY, 5, 3), new FixedCost(4, 6, 4));
        assertEquals(ILLEGAL_MOVE, cd.getCost(null, null, null, 6));
        state(cd, 5, 3);
    }

    public void testTC048CompositionRepeatedCallsRetainMaximum() {
        FixedCost first = new FixedCost(9, 1, 0);
        FixedCost second = new FixedCost(4, 2, 1);
        CostDecider cd = CostDeciders.getComposedCostDecider(first, second);
        assertEquals(9, cd.getCost(null, null, null, 6));
        state(cd, 1, 0);
        first.cost = 2;
        first.moves = 3;
        first.turns = 2;
        second.cost = 3;
        assertEquals(9, cd.getCost(null, null, null, 6));
        state(cd, 3, 2);
    }

    public void testTC049DefaultNullUnit() {
        assertSame(CostDeciders.avoidIllegal(),
                CostDeciders.defaultCostDeciderFor(null));
    }

    public void testTC050DefaultNavalTransport() {
        Unit transport = ship();
        assertEquals("AvoidNavalDangerCostDecider",
                CostDeciders.defaultCostDeciderFor(transport).getClass().getSimpleName());
    }

    public void testTC051DefaultNavalOffensive() {
        colonist.setLocation(dutch.getEurope());
        Unit warship = unit("frigate", dutch, sea(tile(8, 7)));
        assertSame(CostDeciders.avoidSettlements(),
                CostDeciders.defaultCostDeciderFor(warship));
    }

    public void testTC052DefaultLandOffensive() {
        Unit attacker = unit("artillery", dutch, tile(8, 7));
        assertSame(CostDeciders.avoidSettlements(),
                CostDeciders.defaultCostDeciderFor(attacker));
    }

    public void testTC053DefaultLandCivilian() {
        assertSame(CostDeciders.avoidSettlementsAndBlockingUnits(),
                CostDeciders.defaultCostDeciderFor(colonist));
    }
}
