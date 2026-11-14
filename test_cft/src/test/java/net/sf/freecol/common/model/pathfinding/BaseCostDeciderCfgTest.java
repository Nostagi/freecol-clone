package net.sf.freecol.common.model.pathfinding;

import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.Location;
import net.sf.freecol.common.model.Map;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.server.model.ServerUnit;
import net.sf.freecol.util.test.FreeColTestCase;

public class BaseCostDeciderCfgTest extends FreeColTestCase {
    private Game game;
    private Map map;
    private Player dutch;
    private Unit colonist;
    private BaseCostDecider cd;
    protected void setUp() throws Exception {
        super.setUp();
        game = getStandardGame();
        map = getTestMap(spec().getTileType("model.tile.plains"), true);
        game.changeMap(map);
        dutch = game.getPlayerByNationId("model.nation.dutch");
        colonist = new ServerUnit(game, a(), dutch, spec().getUnitType("model.unit.freeColonist"));
        cd = new BaseCostDecider();
    }
    private Tile a() { return map.getTile(8, 7); }
    private Tile b() { return map.getTile(8, 8); }
    private Unit ship() {
        colonist.setLocation(dutch.getEurope());
        a().setType(spec().getTileType("model.tile.ocean"));
        b().setType(spec().getTileType("model.tile.ocean"));
        b().setHighSeasCount(2);
        b().setMoveToEurope(true);
        return new ServerUnit(game, a(), dutch, spec().getUnitType("model.unit.caravel"));
    }
    private void state(int moves, int turns) {
        assertEquals(moves, cd.getMovesLeft());
        assertEquals(turns, cd.getNewTurns());
    }
    private static class ControlledUnit extends ServerUnit {
        private final MoveType moveType;
        private final boolean carried;
        ControlledUnit(Game game, Tile at, Player owner, MoveType moveType, boolean carried) {
            super(game, at, owner, game.getSpecification().getUnitType("model.unit.freeColonist"));
            this.moveType = moveType;
            this.carried = carried;
        }
        public MoveType getSimpleMoveType(Tile from, Tile target) { return moveType; }
        public boolean isOnCarrier() { return carried; }
    }

    public void testTC001EuropeToEurope() {
        assertEquals(-1, cd.getCost(colonist, dutch.getEurope(), dutch.getEurope(), 6));
    }

    public void testTC002EuropeToHighSeas() {
        assertEquals(-1, cd.getCost(colonist, dutch.getEurope(), dutch.getHighSeas(), 6));
    }

    public void testTC003EuropeDisconnected() {
        Unit ship = ship(); b().setMoveToEurope(false);
        assertEquals(-1, cd.getCost(ship, dutch.getEurope(), b(), 6));
    }

    public void testTC004EuropeLandUnit() {
        b().setType(spec().getTileType("model.tile.ocean")); b().setMoveToEurope(true);
        assertEquals(-1, cd.getCost(colonist, dutch.getEurope(), b(), 6));
    }

    public void testTC005EuropeArrival() {
        Unit ship = ship();
        assertEquals(36, cd.getCost(ship, dutch.getEurope(), b(), 6));
        state(12, 3);
    }

    public void testTC006SourceWithoutTile() {
        assertEquals(-1, cd.getCost(colonist, dutch.getHighSeas(), b(), 6));
    }

    public void testTC007EuropeDepartureLandUnit() {
        assertEquals(-1, cd.getCost(colonist, a(), dutch.getEurope(), 6));
    }

    public void testTC008EuropeDepartureShip() {
        Unit ship = ship();
        assertEquals(36, cd.getCost(ship, a(), dutch.getEurope(), 6));
        state(12, 3);
    }

    public void testTC009DestinationWithoutTile() {
        assertEquals(-1, cd.getCost(colonist, a(), dutch.getHighSeas(), 6));
    }

    public void testTC010DestinationUnexplored() {
        b().setType(null);
        assertEquals(-1, cd.getCost(colonist, a(), b(), 6));
    }

    public void testTC011NormalMove() {
        assertEquals(3, cd.getCost(colonist, a(), b(), 6));
        state(3, 0);
    }

    public void testTC012NormalMoveNextTurn() {
        assertEquals(3, cd.getCost(colonist, a(), b(), 0));
        state(0, 1);
    }

    public void testTC013AdjustWithinTurn() {
        assertEquals(3, cd.adjust(colonist, a(), b(), 6));
        state(3, 0);
    }

    public void testTC014AdjustNextTurn() {
        assertEquals(3, cd.adjust(colonist, a(), b(), 0));
        state(0, 1);
    }

    public void testTC015FreshGetters() {
        state(0, 0);
    }

    public void testTC016SwitchMOVEHIGHSEAS() {
        Unit controlled = new ControlledUnit(game, a(), dutch, Unit.MoveType.MOVE_HIGH_SEAS, false);
        assertEquals(3, cd.getCost(controlled, a(), b(), 6));
        state(3, 0);
    }

    public void testTC017SwitchATTACKUNIT() {
        Unit controlled = new ControlledUnit(game, a(), dutch, Unit.MoveType.ATTACK_UNIT, false);
        assertEquals(3, cd.getCost(controlled, a(), b(), 6));
        state(3, 0);
    }

    public void testTC018SwitchMOVE() {
        Unit controlled = new ControlledUnit(game, a(), dutch, Unit.MoveType.MOVE, false);
        assertEquals(3, cd.getCost(controlled, a(), b(), 6));
        state(3, 0);
    }

    public void testTC019SwitchATTACKSETTLEMENT() {
        Unit controlled = new ControlledUnit(game, a(), dutch, Unit.MoveType.ATTACK_SETTLEMENT, false);
        assertEquals(6, cd.getCost(controlled, a(), b(), 6));
        state(0, 0);
    }

    public void testTC020SwitchEXPLORELOSTCITYRUMOUR() {
        Unit controlled = new ControlledUnit(game, a(), dutch, Unit.MoveType.EXPLORE_LOST_CITY_RUMOUR, false);
        assertEquals(6, cd.getCost(controlled, a(), b(), 6));
        state(0, 0);
    }

    public void testTC021SwitchEMBARK() {
        Unit controlled = new ControlledUnit(game, a(), dutch, Unit.MoveType.EMBARK, false);
        assertEquals(6, cd.getCost(controlled, a(), b(), 6));
        state(0, 0);
    }

    public void testTC022SwitchENTERINDIANSETTLEMENTWITHFREECOLONIST() {
        Unit controlled = new ControlledUnit(game, a(), dutch, Unit.MoveType.ENTER_INDIAN_SETTLEMENT_WITH_FREE_COLONIST, false);
        assertEquals(6, cd.getCost(controlled, a(), b(), 6));
        state(0, 0);
    }

    public void testTC023SwitchENTERINDIANSETTLEMENTWITHSCOUT() {
        Unit controlled = new ControlledUnit(game, a(), dutch, Unit.MoveType.ENTER_INDIAN_SETTLEMENT_WITH_SCOUT, false);
        assertEquals(6, cd.getCost(controlled, a(), b(), 6));
        state(0, 0);
    }

    public void testTC024SwitchENTERINDIANSETTLEMENTWITHMISSIONARY() {
        Unit controlled = new ControlledUnit(game, a(), dutch, Unit.MoveType.ENTER_INDIAN_SETTLEMENT_WITH_MISSIONARY, false);
        assertEquals(6, cd.getCost(controlled, a(), b(), 6));
        state(0, 0);
    }

    public void testTC025SwitchENTERFOREIGNCOLONYWITHSCOUT() {
        Unit controlled = new ControlledUnit(game, a(), dutch, Unit.MoveType.ENTER_FOREIGN_COLONY_WITH_SCOUT, false);
        assertEquals(6, cd.getCost(controlled, a(), b(), 6));
        state(0, 0);
    }

    public void testTC026SwitchENTERSETTLEMENTWITHCARRIERANDGOODS() {
        Unit controlled = new ControlledUnit(game, a(), dutch, Unit.MoveType.ENTER_SETTLEMENT_WITH_CARRIER_AND_GOODS, false);
        assertEquals(6, cd.getCost(controlled, a(), b(), 6));
        state(0, 0);
    }

    public void testTC027SwitchMOVEILLEGAL() {
        Unit controlled = new ControlledUnit(game, a(), dutch, Unit.MoveType.MOVE_ILLEGAL, false);
        assertEquals(-1, cd.getCost(controlled, a(), b(), 6));
    }

    public void testTC028CarriedMOVE() {
        Unit controlled = new ControlledUnit(game, a(), dutch, Unit.MoveType.MOVE, true);
        assertEquals(6, cd.getCost(controlled, a(), b(), 6));
        state(0, 0);
    }

    public void testTC029CarriedATTACKUNIT() {
        Unit controlled = new ControlledUnit(game, a(), dutch, Unit.MoveType.ATTACK_UNIT, true);
        assertEquals(6, cd.getCost(controlled, a(), b(), 6));
        state(0, 0);
    }
}
