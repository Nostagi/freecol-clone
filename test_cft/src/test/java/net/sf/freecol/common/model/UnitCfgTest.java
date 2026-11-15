package net.sf.freecol.common.model;

import java.util.Arrays;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.BlockJUnit4ClassRunner;

import net.sf.freecol.common.model.Ability;
import net.sf.freecol.server.model.ServerUnit;
import net.sf.freecol.util.test.MockModelTestCase;


@RunWith(BlockJUnit4ClassRunner.class)
public class UnitCfgTest extends MockModelTestCase {

    private Map map;
    private Player dutch;
    private Player english;
    private Tile tile;
    private Unit colonist;
    private Unit ship;

    @Before
    public void setUpUnitFixture() {
        map = getTestMap(spec().getTileType("model.tile.plains"), true);
        game.changeMap(map);
        dutch = game.getPlayerByNationId("model.nation.dutch");
        english = game.getPlayerByNationId("model.nation.english");
        tile = map.getTile(5, 5);
        colonist = new ServerUnit(game, tile, dutch,
            spec().getUnitType("model.unit.freeColonist"));
        ship = new ServerUnit(game, null, dutch,
            spec().getUnitType("model.unit.caravel"));
    }

    @Test
    public void testClassificationMovementAndCarriedUnits() {
        assertTrue(colonist.isPerson());
        assertFalse(colonist.isNaval());
        assertFalse(colonist.isCarrier());
        assertTrue(colonist.isColonist());
        assertTrue(ship.isNaval());
        assertTrue(ship.isCarrier());
        assertFalse(ship.isPerson());

        colonist.setMovesLeft(3);
        assertEquals(3, colonist.getMovesLeft());
        colonist.setMovesLeft(-1);
        assertEquals(0, colonist.getMovesLeft());

        Unit passenger = new ServerUnit(game, ship, dutch,
            spec().getUnitType("model.unit.freeColonist"));
        assertTrue(passenger.isOnCarrier());
        assertSame(ship, passenger.getCarrier());
        assertFalse(passenger.isOnTile());
        assertFalse(passenger.hasTile());
        assertNull(ship.getCarriedUnitById(null));
        assertNull(ship.getCarriedUnitById("missing"));
        assertSame(passenger, ship.getCarriedUnitById(passenger.getId()));

        assertTrue(colonist.isOnTile());
        assertTrue(colonist.hasTile());
        assertNull(colonist.getCarrier());
    }

    @Test
    public void testStateTransitionConditionsAndExceptions() {
        colonist.setMovesLeft(3);
        assertFalse(colonist.checkSetState(Unit.UnitState.ACTIVE));
        assertFalse(colonist.checkSetState(Unit.UnitState.FORTIFIED));
        assertTrue(colonist.checkSetState(Unit.UnitState.FORTIFYING));
        assertTrue(colonist.checkSetState(Unit.UnitState.IMPROVING));
        assertTrue(colonist.checkSetState(Unit.UnitState.IN_COLONY));
        assertTrue(colonist.checkSetState(Unit.UnitState.SENTRY));
        assertTrue(colonist.checkSetState(Unit.UnitState.SKIPPED));

        colonist.setMovesLeft(0);
        assertFalse(colonist.checkSetState(Unit.UnitState.FORTIFYING));
        assertFalse(colonist.checkSetState(Unit.UnitState.IMPROVING));
        assertFalse(ship.checkSetState(Unit.UnitState.IN_COLONY));

        try {
            colonist.setState(Unit.UnitState.FORTIFIED);
            fail("A unit cannot become fortified without first fortifying");
        } catch (IllegalStateException expected) {
            assertNotNull(expected.getMessage());
        }

        colonist.setMovesLeft(3);
        colonist.setState(Unit.UnitState.FORTIFYING);
        assertTrue(colonist.checkSetState(Unit.UnitState.FORTIFIED));
        colonist.setState(Unit.UnitState.FORTIFIED);
        assertEquals(Unit.UnitState.FORTIFIED, colonist.getState());
        assertEquals(0, colonist.getMovesLeft());
        assertFalse(colonist.checkSetState(Unit.UnitState.SKIPPED));

        colonist.setState(Unit.UnitState.ACTIVE);
        assertTrue(colonist.checkSetState(Unit.UnitState.SKIPPED));
        colonist.setState(Unit.UnitState.SKIPPED);
        assertFalse(colonist.checkSetState(Unit.UnitState.SKIPPED));
    }

    @Test
    public void testUncheckedStateInitializationAndMoveLabelBoundaries() {
        for (Unit.UnitState state : Unit.UnitState.values()) {
            colonist.setStateUnchecked(state);
            assertEquals(state, colonist.getState());
        }

        int initialMoves = colonist.getInitialMovesLeft();
        colonist.setMovesLeft(0);
        assertEquals("0/" + initialMoves / 3, colonist.getMovesAsString());
        colonist.setMovesLeft(1);
        assertEquals("(1/3) /" + initialMoves / 3,
                     colonist.getMovesAsString());
        colonist.setMovesLeft(3);
        assertEquals("1/" + initialMoves / 3, colonist.getMovesAsString());
    }

    @Test
    public void testRoleFilteringAndRoleCountBoundaries() {
        Role defaultRole = spec().getDefaultRole();
        Role available = null;
        Role unavailable = null;
        for (Role candidate : spec().getRolesList()) {
            if (candidate == defaultRole) continue;
            if (available == null && colonist.roleIsAvailable(candidate)) {
                available = candidate;
            } else if (unavailable == null
                       && !colonist.roleIsAvailable(candidate)) {
                unavailable = candidate;
            }
        }
        assertNotNull("Specification should have an available non-default role",
                      available);
        assertNotNull("Specification should have a role unavailable to colonists",
                      unavailable);

        List<Role> filtered = colonist.getAvailableRolesList(
            Arrays.asList(available, unavailable));
        assertEquals(1, filtered.size());
        assertSame(available, filtered.get(0));
        assertFalse(colonist.getAvailableRolesList(null).isEmpty());

        colonist.changeRole(available, 2);
        assertSame(available, colonist.getRole());
        assertEquals(2, colonist.getRoleCount());
        assertFalse(colonist.changeRoleCount(-1));
        assertEquals(1, colonist.getRoleCount());
        assertTrue(colonist.changeRoleCount(-1));
        assertSame(defaultRole, colonist.getRole());
        assertEquals(0, colonist.getRoleCount());

        colonist.changeRole(defaultRole, 5);
        assertEquals(0, colonist.getRoleCount());
        assertTrue(colonist.hasDefaultRole());
        assertEquals(Role.getRoleIdSuffix(defaultRole.getId()),
                     colonist.getRoleSuffix());
    }

    @Test
    public void testExperienceWorkAndDamageCalculations() {
        GoodsType food = spec().getPrimaryFoodType();
        GoodsType cotton = spec().getGoodsType("model.goods.cotton");

        colonist.setExperience(2);
        colonist.changeExperienceType(food);
        assertEquals(0, colonist.getExperience());
        colonist.setExperience(3);
        colonist.changeExperienceType(food);
        assertEquals(3, colonist.getExperience());
        colonist.changeExperienceType(cotton);
        assertEquals(0, colonist.getExperience());

        colonist.setExperience(Integer.MAX_VALUE);
        assertEquals(colonist.getType().getMaximumExperience(),
                     colonist.getExperience());
        colonist.modifyExperience(-1);
        assertEquals(colonist.getType().getMaximumExperience() - 1,
                     colonist.getExperience());

        colonist.setWorkLeft(5);
        assertEquals(5, colonist.getWorkTurnsLeft());
        UnitType expert = null;
        for (UnitType candidate : spec().getUnitTypeList()) {
            if (candidate.hasAbility(Ability.EXPERT_PIONEER)) {
                expert = candidate;
                break;
            }
        }
        assertNotNull("Specification should have an expert pioneer", expert);
        Unit pioneer = new ServerUnit(game, tile, dutch, expert);
        assertEquals(-1, pioneer.getWorkTurnsLeft());
        pioneer.setStateUnchecked(Unit.UnitState.IMPROVING);
        pioneer.setWorkLeft(5);
        assertEquals(3, pioneer.getWorkTurnsLeft());
        colonist.setStateUnchecked(Unit.UnitState.IMPROVING);
        colonist.setWorkLeft(5);
        assertEquals(5, colonist.getWorkTurnsLeft());

        assertFalse(colonist.isDamaged());
        assertEquals(0, colonist.getTurnsForRepair());
        colonist.setHitPoints(colonist.getMaximumHitPoints() - 2);
        assertTrue(colonist.isDamaged());
        assertEquals(2, colonist.getTurnsForRepair());
        assertEquals(colonist.isDamaged()
                     && !specification.hasAbility(Ability.HITPOINTS_COMBAT_MODEL),
                     colonist.isDamagedAndUnderForcedRepair());
    }

    @Test
    public void testOwnerChangeRecursesThroughCarrierAndSameOwnerIsNoOp() {
        Unit passenger = new ServerUnit(game, ship, dutch,
            spec().getUnitType("model.unit.freeColonist"));
        colonist.changeOwner(dutch);
        assertSame(dutch, colonist.getOwner());

        ship.changeOwner(english);
        assertSame(english, ship.getOwner());
        assertSame(english, passenger.getOwner());
        assertSame(dutch, colonist.getOwner());
    }

    @Test
    public void testChildStateAndEntryLocationBranches() {
        Unit passenger = new ServerUnit(game, ship, dutch,
            spec().getUnitType("model.unit.freeColonist"));
        ship.setStateToAllChildren(Unit.UnitState.SENTRY);
        assertEquals(Unit.UnitState.SENTRY, passenger.getState());

        colonist.setEntryLocation(tile);
        assertSame(tile, colonist.getEntryLocation());
        assertSame(tile, colonist.getFullEntryLocation());
        assertSame(tile, dutch.getEntryTile());

        colonist.setEntryLocation(null);
        assertNull(colonist.getEntryLocation());
        assertSame(tile, colonist.getFullEntryLocation());

        colonist.setStateToAllChildren(Unit.UnitState.SENTRY);
        assertEquals(Unit.UnitState.ACTIVE, colonist.getState());
    }

    @Test
    public void testTradeLocationAndMissionState() {
        Colony colony = createStandardColony();
        Unit resident = colony.getUnitList().get(0);
        assertSame(colony, resident.getTradeLocation());

        Unit missionary = new ServerUnit(game, null, dutch,
            spec().getUnitType("model.unit.freeColonist"));
        missionary.setRole(spec().getRole("model.role.missionary"));
        assertTrue(missionary.hasAbility(Ability.ESTABLISH_MISSION));
        assertFalse(missionary.isInMission());
        assertNull(missionary.getTradeLocation());

        IndianSettlement settlement = newIndianSettlementForUnitTest();
        missionary.setLocationNoUpdate(settlement);
        assertTrue(missionary.isInMission());
        assertSame(settlement, missionary.getTradeLocation());
    }

    private IndianSettlement newIndianSettlementForUnitTest() {
        Player inca = game.getPlayerByNationId("model.nation.inca");
        return new IndianSettlementBuilder(game).player(inca)
            .settlementTile(map.getTile(10, 8)).build();
    }

    @Test
    public void testLabelsAndNullTypeDefaults() {
        assertNotNull(colonist.getLabel(Unit.UnitLabelType.PLAIN));
        assertNotNull(colonist.getLabel(Unit.UnitLabelType.NATIONAL));
        assertNotNull(colonist.getLabel(Unit.UnitLabelType.FULL));
        assertNotNull(colonist.getDescription());
        ship.setDestination(tile);
        assertNotNull(ship.getDestinationLabel());
        assertNotNull(colonist.getRepairLabel());

        Unit uninitialized = new Unit(game, "unit:cfg-null-type");
        assertEquals(0, uninitialized.getScoreValue());
        assertFalse(uninitialized.isNaval());
        assertFalse(uninitialized.isOwnerHidden());
        assertFalse(uninitialized.isPerson());
    }

    @Test
    public void testBetterDefenderSelectionBranches() {
        assertTrue(Unit.betterDefender(null, 0.0, colonist, 0.0));

        Role soldier = spec().getRole("model.role.soldier");
        Unit armed = new ServerUnit(game, null, dutch,
            spec().getUnitType("model.unit.freeColonist"));
        armed.setRole(soldier);
        assertTrue(armed.isArmed());
        assertTrue(Unit.betterDefender(colonist, 1.0, armed, 0.0));
        assertFalse(Unit.betterDefender(armed, 1.0, colonist, 100.0));
        assertFalse(Unit.betterDefender(colonist, 3.0, ship, 2.0));
        assertTrue(Unit.betterDefender(colonist, 1.0, ship, 2.0));
    }
}
