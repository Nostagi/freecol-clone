package net.sf.freecol.common.model;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.Random;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.BlockJUnit4ClassRunner;

import net.sf.freecol.common.FreeColException;
import net.sf.freecol.server.model.ServerUnit;
import net.sf.freecol.util.test.MockModelTestCase;


@RunWith(BlockJUnit4ClassRunner.class)
public class SettlementCfgTest extends MockModelTestCase {

    private Map map;
    private Player inca;
    private Player arawak;
    private Player dutch;

    @Before
    public void setUpSettlementFixture() {
        map = getTestMap(spec().getTileType("model.tile.plains"), true);
        game.changeMap(map);
        inca = game.getPlayerByNationId("model.nation.inca");
        arawak = game.getPlayerByNationId("model.nation.arawak");
        dutch = game.getPlayerByNationId("model.nation.dutch");
    }

    private IndianSettlement newIndianSettlement(Player owner, int x, int y) {
        return new IndianSettlementBuilder(game)
            .player(owner)
            .settlementTile(map.getTile(x, y))
            .build();
    }

    @Test
    public void testCapitalTypeAndOwnedTileDefensiveCopy() {
        IndianSettlement settlement = newIndianSettlement(inca, 5, 8);

        assertFalse(settlement.isCapital());
        settlement.setCapital(false);
        assertFalse(settlement.isCapital());
        settlement.setCapital(true);
        assertTrue(settlement.isCapital());
        settlement.setCapital(true);
        assertTrue(settlement.isCapital());
        settlement.setCapital(false);
        assertFalse(settlement.isCapital());

        Set<Tile> owned = settlement.getOwnedTiles();
        assertFalse(owned.isEmpty());
        owned.clear();
        assertFalse(settlement.getOwnedTiles().isEmpty());
        assertEquals(settlement.getType().getClaimableRadius(),
                     settlement.getRadius());
        assertTrue(settlement.getLineOfSight() > 0);

        Tile extra = map.getTile(6, 8);
        settlement.addTile(extra);
        settlement.addTile(extra);
        assertTrue(settlement.getOwnedTiles().contains(extra));
        settlement.removeTile(extra);
        settlement.removeTile(extra);
        assertFalse(settlement.getOwnedTiles().contains(extra));
    }

    @Test
    public void testUnitAggregationAndVisibilityTargets() {
        IndianSettlement settlement = newIndianSettlement(inca, 5, 8);
        Unit resident = settlement.getUnitList().get(0);
        Unit onTile = new ServerUnit(game, settlement.getTile(), inca,
            spec().getUnitType("model.unit.freeColonist"));

        assertTrue(settlement.getAllUnitsList().contains(resident));
        assertTrue(settlement.getAllUnitsList().contains(onTile));
        assertSame(settlement, settlement.getLinkTarget(inca));
        assertSame(settlement.getTile(), settlement.getLinkTarget(dutch));
        assertSame(settlement, settlement.getSettlement());
        assertSame(settlement.getTile(), settlement.getTile());
        assertNotNull(settlement.getLocationLabel());
        assertFalse(settlement.getVisibleTileSet().isEmpty());

        Set<Tile> visible = settlement.getVisibleTileSet();
        visible.clear();
        assertFalse(settlement.getVisibleTileSet().isEmpty());
    }

    @Test
    public void testPlacementExcisingAndRoadBranches() {
        IndianSettlement settlement = newIndianSettlement(inca, 5, 8);
        Tile center = settlement.getTile();
        assertSame(settlement, center.getSettlement());

        assertNotNull(center.getRoad());
        settlement.placeSettlement(false);
        assertNotNull(center.getRoad());

        center.removeRoad();
        settlement.placeSettlement(false);
        assertSame(settlement, center.getSettlement());
        assertNotNull(center.getRoad());
        assertTrue(center.getRoad().getVirtual());
        assertTrue(settlement.getOwnedTiles().contains(center));

        settlement.exciseSettlement();
        assertNull(center.getSettlement());
        assertNull(center.getOwner());
        assertNull(center.getRoad());
        for (Tile owned : settlement.getOwnedTiles()) {
            assertNull(owned.getOwner());
            assertNull(owned.getOwningSettlement());
        }

        IndianSettlement retainedRoad = newIndianSettlement(inca, 10, 8);
        retainedRoad.getTile().getRoad().setVirtual(false);
        retainedRoad.exciseSettlement();
        assertNotNull(retainedRoad.getTile().getRoad());
    }

    @Test
    public void testOwnerChangeAndNativeEuropeanGuard() {
        IndianSettlement settlement = newIndianSettlement(inca, 5, 8);
        Tile center = settlement.getTile();
        settlement.changeOwner(arawak);
        assertSame(arawak, settlement.getOwner());
        assertSame(arawak, center.getOwner());

        try {
            settlement.changeOwner(dutch);
            fail("A settlement cannot transfer between native and European owners");
        } catch (RuntimeException expected) {
            assertTrue(expected.getMessage().contains("Can not transfer settlements"));
        }
        assertSame(arawak, settlement.getOwner());
    }

    @Test
    public void testPlunderRangeAndPlunderAmount() {
        Colony colony = createStandardColony();
        Unit attacker = new ServerUnit(game, null, dutch,
            spec().getUnitType("model.unit.freeColonist"));

        dutch.setGold(0);
        assertNull(colony.getPlunderRange(attacker));
        assertEquals(0, colony.getPlunder(attacker, new Random(1)));

        dutch.setGold(1000);
        RandomRange range = colony.getPlunderRange(attacker);
        assertNotNull(range);
        int plunder = colony.getPlunder(attacker, new Random(1));
        assertTrue(plunder > 0);
    }

    @Test
    public void testLandLockedPortAndHighSeasConditions() {
        IndianSettlement settlement = newIndianSettlement(inca, 5, 8);
        assertTrue(settlement.isLandLocked());
        assertFalse(settlement.isConnectedPort());
        assertEquals(Constants.INFINITY, settlement.getHighSeasCount());
        assertFalse(settlement.canBombardEnemyShip());

        Tile sea = map.getTile(4, 8);
        sea.setType(spec().getTileType("model.tile.ocean"));
        assertFalse(settlement.isLandLocked());
        assertFalse(settlement.isConnectedPort());
        assertFalse(settlement.canBombardEnemyShip());

        sea.setHighSeasCount(2);
        assertTrue(settlement.isConnectedPort());
        assertEquals(2, settlement.getHighSeasCount());
        settlement.addAbility(new Ability(Ability.BOMBARD_SHIPS, true));
        assertTrue(settlement.canBombardEnemyShip());
    }

    @Test
    public void testFoodGoodsPricingAndEquipmentBranches() throws Exception {
        Colony colony = createStandardColony(4);
        GoodsType muskets = spec().getGoodsType("model.goods.muskets");
        GoodsType horses = spec().getGoodsType("model.goods.horses");
        Unit unit = colony.getUnitList().get(0);
        Role soldier = spec().getRole("model.role.soldier");

        assertTrue(colony.getFoodConsumption() >= 0);
        assertEquals(0, colony.getConsumptionOf((List<GoodsType>)null));
        assertFalse(colony.canProvideGoods(Arrays.asList(
            new AbstractGoods(muskets, 1))));
        assertNull(colony.canImproveUnitMilitaryRole(unit));

        colony.addGoods(muskets, 50);
        assertTrue(colony.priceGoods(Arrays.asList(
            new AbstractGoods(muskets, 50))) >= 0);
        try {
            colony.priceGoods(Arrays.asList(new AbstractGoods(horses, 1)));
            fail("Pricing must reject unavailable goods");
        } catch (FreeColException expected) {
            assertTrue(expected.getMessage().contains("Goods missing"));
        }

        assertFalse(colony.canProvideGoods(Arrays.asList(
            new AbstractGoods(muskets, 100))));
        colony.addGoods(muskets, 100);
        assertTrue(colony.canProvideGoods(soldier.getRequiredGoodsList()));
        assertTrue(colony.equipForRole(unit, soldier, 1));
        assertSame(soldier, unit.getRole());
        assertNull(colony.canImproveUnitMilitaryRole(unit));

        Role unavailable = null;
        for (Role candidate : spec().getRolesList()) {
            if (!unit.roleIsAvailable(candidate)) {
                unavailable = candidate;
                break;
            }
        }
        assertNotNull(unavailable);
        assertFalse(colony.equipForRole(unit, unavailable, 1));
    }

    @Test
    public void testSettlementNoAddReasonAndContactedUnitVisibility() {
        IndianSettlement settlement = newIndianSettlement(inca, 5, 8);
        Unit friendly = new ServerUnit(game, null, inca,
            spec().getUnitType("model.unit.freeColonist"));
        Unit enemy = new ServerUnit(game, null, dutch,
            spec().getUnitType("model.unit.freeColonist"));

        assertEquals(UnitLocation.NoAddReason.NONE,
                     settlement.getNoAddReason(friendly));
        assertEquals(UnitLocation.NoAddReason.OWNED_BY_ENEMY,
                     settlement.getNoAddReason(enemy));
        Goods goods = new Goods(game, settlement,
            spec().getGoodsType("model.goods.food"), 1);
        assertEquals(UnitLocation.NoAddReason.NONE,
                     settlement.getNoAddReason(goods));
        assertTrue(settlement.add(friendly));
        assertTrue(settlement.getAllUnitsList().contains(friendly));
        assertSame(settlement, settlement.getLinkTarget(inca));
        assertSame(settlement.getTile(), settlement.getLinkTarget(dutch));
    }

    @Test
    public void testDisposeRemovesSettlementFromOwner() {
        IndianSettlement settlement = newIndianSettlement(inca, 5, 8);
        assertTrue(inca.getSettlementList().contains(settlement));
        settlement.disposeResources();
        assertFalse(inca.getSettlementList().contains(settlement));
    }

    @Test
    public void testOwnedTileClaimRadiusAndFoodConsumptionLoopBoundaries() {
        Colony colony = createStandardColony(1);
        assertEquals(1, colony.getUnitCount());
        assertTrue(colony.getOwnedTiles().size() > 1);
        assertEquals(colony.getType().getClaimableRadius(), colony.getRadius());
        assertTrue(colony.getFoodConsumption() > 0);

        Colony empty = createStandardColony(1, 12, 8);
        Unit resident = empty.getUnitList().get(0);
        resident.setLocation(map.getTile(12, 8));
        assertEquals(0, empty.getUnitCount());
        assertTrue(empty.getFoodConsumption() >= 0);

        List<Unit> all = empty.getAllUnitsList();
        assertTrue(all.isEmpty() || all.contains(resident));
    }
}
