package net.sf.freecol.common.model.pathfinding;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import net.sf.freecol.common.model.Ability;
import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.Location;
import net.sf.freecol.common.model.Map;
import net.sf.freecol.common.model.PathNode;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Stance;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.server.model.ServerUnit;
import net.sf.freecol.util.test.FreeColTestCase;
import net.sf.freecol.util.test.FreeColTestUtils;

public class GoalDecidersCfgTest extends FreeColTestCase {
    private Game game;
    private Map map;
    private Player dutch, french;
    private Unit unit;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        game = getStandardGame();
        map = getTestMap(spec().getTileType("model.tile.plains"), true);
        game.changeMap(map);
        dutch = game.getPlayerByNationId("model.nation.dutch");
        french = game.getPlayerByNationId("model.nation.french");
        unit = new ServerUnit(game, tile(4, 4), dutch,
                spec().getUnitType("model.unit.freeColonist"));
    }

    private Tile tile(int x, int y) { return map.getTile(x, y); }

    /** A concrete path with cost 100*turns + 100-moves. */
    private PathNode path(Location location, int turns, int moves) {
        return new PathNode(location, moves, turns, false, null, null);
    }

    private PathNode path(Location location) { return path(location, 3, 2); }

    private Colony colony(Player owner, Tile at) {
        return FreeColTestUtils.getColonyBuilder().player(owner)
                .colonyTile(at).initialColonists(1).build();
    }

    private Tile ocean(Tile t) {
        t.setType(spec().getTileType("model.tile.ocean"));
        t.setHighSeasCount(2);
        t.setMoveToEurope(true);
        return t;
    }

    /** Invoke the real returned object's two accessor methods as well. */
    private GoalDecider observe(GoalDecider gd) {
        gd.getGoal();
        gd.hasSubGoals();
        return gd;
    }

    /** Controlled collaborator for composition; the target is never overridden. */
    private static final class ConfigurableGoal implements GoalDecider {
        boolean accept, sub;
        ConfigurableGoal(boolean accept, boolean sub) {
            this.accept = accept;
            this.sub = sub;
        }
        public PathNode getGoal() { return null; }
        public boolean hasSubGoals() { return sub; }
        public boolean check(Unit u, PathNode p) { return accept; }
    }

    public void testTC001ComposedRejectsEmptyList() {
        try {
            GoalDeciders.getComposedGoalDecider(true);
            fail("short list must throw");
        } catch (RuntimeException expected) {
            assertTrue(expected.getMessage().startsWith("Short GoalDecider list"));
        }
    }

    public void testTC002ComposedRejectsOneDelegate() {
        try {
            GoalDeciders.getComposedGoalDecider(false, new ConfigurableGoal(true, true));
            fail("short list must throw");
        } catch (RuntimeException expected) {
            assertTrue(expected.getMessage().endsWith("1"));
        }
    }

    public void testTC003ComposedAndAllAccept() {
        GoalDecider gd = observe(GoalDeciders.getComposedGoalDecider(true,
                new ConfigurableGoal(true, true), new ConfigurableGoal(true, true)));
        PathNode p = path(tile(8, 8));
        assertTrue(gd.check(unit, p));
        assertSame(p, gd.getGoal());
        gd.hasSubGoals(); // Characterize source behavior, not a separate game oracle.
    }

    public void testTC004ComposedAndFirstRejects() {
        GoalDecider gd = observe(GoalDeciders.getComposedGoalDecider(true,
                new ConfigurableGoal(false, false), new ConfigurableGoal(true, true)));
        assertFalse(gd.check(unit, path(tile(8, 8))));
        assertNull(gd.getGoal());
    }

    public void testTC005ComposedAndLaterRejects() {
        GoalDecider gd = observe(GoalDeciders.getComposedGoalDecider(true,
                new ConfigurableGoal(true, true), new ConfigurableGoal(false, false)));
        assertFalse(gd.check(unit, path(tile(8, 8))));
    }

    public void testTC006ComposedOrAllReject() {
        GoalDecider gd = observe(GoalDeciders.getComposedGoalDecider(false,
                new ConfigurableGoal(false, false), new ConfigurableGoal(false, false)));
        assertFalse(gd.check(unit, path(tile(8, 8))));
    }

    public void testTC007ComposedOrFirstAccepts() {
        GoalDecider gd = observe(GoalDeciders.getComposedGoalDecider(false,
                new ConfigurableGoal(true, true), new ConfigurableGoal(false, false)));
        assertTrue(gd.check(unit, path(tile(8, 8))));
    }

    public void testTC008ComposedOrSecondAccepts() {
        GoalDecider gd = observe(GoalDeciders.getComposedGoalDecider(false,
                new ConfigurableGoal(false, false), new ConfigurableGoal(true, true)));
        assertTrue(gd.check(unit, path(tile(8, 8))));
        gd.hasSubGoals();
    }

    public void testTC009ComposedWinnerLimitsLaterChecks() {
        ConfigurableGoal first = new ConfigurableGoal(true, false);
        GoalDecider gd = observe(GoalDeciders.getComposedGoalDecider(false,
                first, new ConfigurableGoal(true, false)));
        PathNode accepted = path(tile(8, 8));
        assertTrue(gd.check(unit, accepted)); // winner = 0
        first.accept = false;
        gd.hasSubGoals(); // i=1 exceeds winner: break
        assertFalse(gd.check(unit, path(tile(9, 8)))); // same break in check
        assertSame(accepted, gd.getGoal());
    }

    public void testTC010ClosestWithoutSettlement() {
        GoalDecider gd = observe(GoalDeciders.getOurClosestSettlementGoalDecider());
        assertFalse(gd.check(unit, path(tile(8, 8))));
        assertNull(gd.getGoal());
    }

    public void testTC011ClosestForeignSettlement() {
        Colony c = colony(french, tile(8, 8));
        assertFalse(observe(GoalDeciders.getOurClosestSettlementGoalDecider())
                .check(unit, path(c)));
    }

    public void testTC012ClosestInlandKeepsBetterPath() {
        Colony c = colony(dutch, tile(8, 8));
        assertFalse(c.isConnectedPort());
        GoalDecider gd = observe(GoalDeciders.getOurClosestSettlementGoalDecider());
        PathNode first = path(c, 3, 2);
        assertTrue(gd.check(unit, first));
        assertFalse(gd.check(unit, path(c, 6, 2)));
        assertSame(first, gd.getGoal());
    }

    public void testTC013ClosestPortImproves() {
        Tile at = tile(8, 8);
        ocean(at.getSurroundingTiles(1, 1).iterator().next());
        Colony c = colony(dutch, at);
        assertTrue(c.isConnectedPort());
        GoalDecider gd = observe(GoalDeciders.getOurClosestSettlementGoalDecider());
        assertTrue(gd.check(unit, path(c, 6, 2)));
        PathNode better = path(c, 2, 2);
        assertTrue(gd.check(unit, better));
        assertFalse(gd.check(unit, path(c, 2, 2)));
        assertSame(better, gd.getGoal());
    }

    public void testTC014HighSeasWithoutTile() {
        assertFalse(observe(GoalDeciders.getHighSeasGoalDecider())
                .check(unit, path(dutch.getEurope())));
    }

    public void testTC015HighSeasUnexplored() {
        Tile t = ocean(tile(8, 8));
        t.setExplored(dutch, false);
        assertFalse(observe(GoalDeciders.getHighSeasGoalDecider()).check(unit, path(t)));
    }

    public void testTC016HighSeasNotConnected() {
        Tile t = tile(8, 8);
        t.setMoveToEurope(false);
        assertFalse(observe(GoalDeciders.getHighSeasGoalDecider()).check(unit, path(t)));
    }

    public void testTC017HighSeasOccupiedByEnemy() {
        Tile t = ocean(tile(8, 8));
        new ServerUnit(game, t, french, spec().getUnitType("model.unit.galleon"));
        assertFalse(observe(GoalDeciders.getHighSeasGoalDecider()).check(unit, path(t)));
    }

    public void testTC018HighSeasSelectsLowerCost() {
        Tile t = ocean(tile(8, 8));
        GoalDecider gd = observe(GoalDeciders.getHighSeasGoalDecider());
        assertTrue(gd.check(unit, path(t, 5, 2)));
        assertFalse(gd.check(unit, path(t, 7, 2)));
        PathNode better = path(t, 2, 2);
        assertTrue(gd.check(unit, better));
        assertSame(better, gd.getGoal());
    }

    public void testTC019SimpleHighSeasRejects() {
        GoalDecider gd = observe(GoalDeciders.getSimpleHighSeasGoalDecider());
        assertFalse(gd.check(unit, path(dutch.getEurope())));
        tile(8, 8).setMoveToEurope(false);
        assertFalse(gd.check(unit, path(tile(8, 8))));
    }

    public void testTC020SimpleHighSeasAccepts() {
        GoalDecider gd = observe(GoalDeciders.getSimpleHighSeasGoalDecider());
        PathNode p = path(ocean(tile(8, 8)));
        assertTrue(gd.check(null, p));
        assertSame(p, gd.getGoal());
    }

    public void testTC021LocationMisses() {
        GoalDecider gd = observe(GoalDeciders.getLocationGoalDecider(tile(8, 8)));
        assertFalse(gd.check(unit, path(tile(12, 8))));
        assertNull(gd.getGoal());
    }

    public void testTC022LocationTracksBestCost() {
        Tile t = tile(8, 8);
        GoalDecider gd = observe(GoalDeciders.getLocationGoalDecider(t));
        PathNode first = path(t, 5, 2);
        assertTrue(gd.check(unit, first));
        assertTrue(gd.check(unit, path(t, 7, 2)));
        assertSame(first, gd.getGoal());
        PathNode better = path(t, 2, 2);
        assertTrue(gd.check(unit, better));
        assertSame(better, gd.getGoal());
    }

    public void testTC023AdjacentTargetWithoutTile() {
        assertNull(GoalDeciders.getAdjacentLocationGoalDecider(dutch.getEurope()));
    }

    public void testTC024AdjacentNearFarAndNull() {
        Tile target = tile(8, 8);
        GoalDecider gd = observe(GoalDeciders.getAdjacentLocationGoalDecider(target));
        assertFalse(gd.check(unit, path(dutch.getEurope())));
        assertFalse(gd.check(unit, path(tile(2, 2))));
        PathNode p = path(target.getSurroundingTiles(1, 1).iterator().next());
        assertTrue(gd.check(unit, p));
        assertSame(p, gd.getGoal());
    }

    public void testTC025EnemyInvalidTiles() {
        GoalDecider gd = observe(GoalDeciders.getEnemySettlementGoalDecider(Arrays.asList(french)));
        assertFalse(gd.check(unit, path(dutch.getEurope())));
        assertFalse(gd.check(unit, path(ocean(tile(8, 8)))));
    }

    public void testTC026EnemyLandWithoutSettlement() {
        assertFalse(observe(GoalDeciders.getEnemySettlementGoalDecider(Arrays.asList(french)))
                .check(unit, path(tile(8, 8))));
    }

    public void testTC027EnemyOwnerMembership() {
        Colony enemy = colony(french, tile(8, 8));
        Colony own = colony(dutch, tile(12, 8));
        GoalDecider gd = observe(GoalDeciders.getEnemySettlementGoalDecider(Arrays.asList(french)));
        assertFalse(gd.check(unit, path(own)));
        PathNode p = path(enemy);
        assertTrue(gd.check(unit, p));
        assertSame(p, gd.getGoal());
    }

    public void testTC028DisembarkRejectsInvalidCandidates() {
        GoalDecider gd = observe(GoalDeciders.getDisembarkGoalDecider(tile(12, 8)));
        assertFalse(gd.check(unit, path(dutch.getEurope())));
        assertFalse(gd.check(unit, path(ocean(tile(8, 8)))));
        assertFalse(gd.check(unit, path(unit.getTile())));
        assertFalse(gd.check(unit, path(colony(dutch, tile(12, 8)))));
    }

    public void testTC029DisembarkWithoutDock() {
        GoalDecider gd = observe(GoalDeciders.getDisembarkGoalDecider(tile(12, 8)));
        assertFalse(gd.check(unit, path(tile(8, 8))));
    }

    private void disembark(boolean fort, boolean war, boolean piracy) {
        Tile landing = tile(8, 8);
        Tile dock = ocean(landing.getSurroundingTiles(1, 1).iterator().next());
        if (fort) {
            Tile fortTile = dock.getSurroundingTiles(1, 1).stream()
                    .filter(t -> t != landing && t.isLand() && t != unit.getTile())
                    .findFirst().get();
            Colony c = colony(french, fortTile);
            c.addAbility(new Ability(Ability.BOMBARD_SHIPS));
        }
        dutch.setStance(french, war ? Stance.WAR : Stance.PEACE);
        if (piracy) dutch.addAbility(new Ability(Ability.PIRACY));
        GoalDecider gd = observe(GoalDeciders.getDisembarkGoalDecider(tile(12, 8)));
        PathNode p = path(landing);
        assertTrue(gd.check(unit, p));
        assertFalse(gd.check(unit, path(landing, 7, 2)));
        assertSame(p, gd.getGoal());
    }

    public void testTC030DisembarkSafeDock() { disembark(false, false, false); }
    public void testTC031DisembarkEnemyFortAtWar() { disembark(true, true, false); }
    public void testTC032DisembarkEnemyFortAtPeace() { disembark(true, false, false); }
    public void testTC033DisembarkPiracyNearFort() { disembark(true, false, true); }

    public void testTC034StealthVisibleAndHidden() {
        new ServerUnit(game, tile(8, 8), french, spec().getUnitType("model.unit.freeColonist"));
        french.invalidateCanSeeTiles();
        GoalDecider gd = observe(GoalDeciders.getStealthyGoalDecider(french));
        assertFalse(gd.check(unit, path(tile(8, 8))));
        PathNode hidden = path(tile(2, 2));
        assertTrue(gd.check(unit, hidden));
        assertSame(hidden, gd.getGoal());
    }

    public void testTC035ReduceHighSeasCountState() {
        unit.getTile().setHighSeasCount(9);
        Colony own = colony(dutch, tile(8, 8));
        own.getTile().setHighSeasCount(5);
        Colony enemy = colony(french, tile(12, 8));
        enemy.getTile().setHighSeasCount(3);
        GoalDecider gd = observe(GoalDeciders.getReduceHighSeasCountGoalDecider(unit));
        assertFalse(gd.check(unit, path(enemy)));
        PathNode p = path(own);
        assertTrue(gd.check(unit, p));
        assertFalse(gd.check(unit, path(own)));
        tile(6, 8).setHighSeasCount(2);
        assertFalse(gd.check(unit, path(tile(6, 8))));
        assertSame(p, gd.getGoal());
    }

    public void testTC036CornerTracksScore() {
        GoalDecider gd = observe(GoalDeciders.getCornerGoalDecider());
        Tile t = tile(8, 8);
        t.setHighSeasCount(4);
        assertFalse(gd.check(unit, path(t)));
        // isRiverCorner uses navigable greatRiver tiles, not river improvements.
        t.setType(spec().getTileType("model.tile.greatRiver"));
        assertTrue(t.isRiverCorner());
        PathNode p = path(t);
        assertTrue(gd.check(unit, p));
        assertFalse(gd.check(unit, path(t)));
        assertSame(p, gd.getGoal());
    }

    public void testTC037MultipleEmptyAndNullTile() {
        GoalDeciders.MultipleAdjacentDecider multiple =
                new GoalDeciders.MultipleAdjacentDecider(Collections.emptyList());
        GoalDecider gd = observe(multiple.getGoalDecider());
        assertFalse(gd.check(unit, path(dutch.getEurope())));
        assertFalse(gd.check(unit, path(tile(8, 8))));
        assertTrue(multiple.getResults().isEmpty());
    }

    public void testTC038MultipleStoresAndImprovesPaths() {
        Tile target = tile(8, 8);
        List<Location> locs = Arrays.asList(target, tile(2, 2));
        GoalDeciders.MultipleAdjacentDecider multiple =
                new GoalDeciders.MultipleAdjacentDecider(locs);
        GoalDecider gd = observe(multiple.getGoalDecider());
        Tile near = target.getSurroundingTiles(1, 1).iterator().next();
        PathNode first = path(near, 5, 2);
        assertFalse(gd.check(unit, first));
        assertSame(first, multiple.getResults().get(target));
        assertFalse(gd.check(unit, path(near, 7, 2)));
        assertSame(first, multiple.getResults().get(target));
        PathNode better = path(near, 2, 2);
        assertFalse(gd.check(unit, better));
        assertSame(better, multiple.getResults().get(target));
    }

    public void testTC039ClosestUsesLastNode() {
        Colony c = colony(dutch, tile(8, 8));
        GoalDecider gd = observe(GoalDeciders.getOurClosestSettlementGoalDecider());
        PathNode head = path(tile(6, 8));
        PathNode tail = path(c);
        head.next = tail;
        tail.previous = head;
        assertTrue(gd.check(unit, head));
        assertSame(head, gd.getGoal());
    }

    public void testTC040HighSeasOwnedOccupant() {
        Tile t = ocean(tile(8, 8));
        new ServerUnit(game, t, dutch, spec().getUnitType("model.unit.galleon"));
        GoalDecider gd = observe(GoalDeciders.getHighSeasGoalDecider());
        assertTrue(gd.check(unit, path(t)));
        assertNotNull(gd.getGoal());
    }
}
