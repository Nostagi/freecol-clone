package net.sf.freecol.common.model;

import java.util.HashMap;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.BlockJUnit4ClassRunner;

import net.sf.freecol.common.FreeColException;
import net.sf.freecol.server.model.ServerEurope;
import net.sf.freecol.server.model.ServerUnit;
import net.sf.freecol.util.test.MockModelTestCase;


@RunWith(BlockJUnit4ClassRunner.class)
public class EuropeCfgTest extends MockModelTestCase {

    @Test
    public void testMigrantIndexAndSlotBoundaries() {
        assertEquals(3, Europe.MigrationType.getMigrantCount());
        assertEquals(0, Europe.MigrationType.getUnspecificSlot());
        assertEquals(1, Europe.MigrationType.getDefaultSlot());

        assertFalse(Europe.MigrationType.validMigrantIndex(-1));
        assertTrue(Europe.MigrationType.validMigrantIndex(0));
        assertTrue(Europe.MigrationType.validMigrantIndex(2));
        assertFalse(Europe.MigrationType.validMigrantIndex(3));
        assertEquals(1, Europe.MigrationType.migrantIndexToSlot(0));
        assertEquals(2, Europe.MigrationType.migrantSlotToIndex(3));

        assertTrue(Europe.MigrationType.validMigrantSlot(0));
        assertTrue(Europe.MigrationType.validMigrantSlot(3));
        assertFalse(Europe.MigrationType.validMigrantSlot(4));
        assertTrue(Europe.MigrationType.specificMigrantSlot(1));
        assertTrue(Europe.MigrationType.specificMigrantSlot(3));
        assertFalse(Europe.MigrationType.specificMigrantSlot(0));
        assertTrue(Europe.MigrationType.unspecificMigrantSlot(0));
        assertFalse(Europe.MigrationType.unspecificMigrantSlot(1));
        assertEquals(0, Europe.MigrationType.convertToMigrantSlot(null));
        assertEquals(0, Europe.MigrationType.convertToMigrantSlot(-1));
        assertEquals(0, Europe.MigrationType.convertToMigrantSlot(4));
        assertEquals(2, Europe.MigrationType.convertToMigrantSlot(2));
    }

    @Test
    public void testExpandedRecruitablesRespectLimitAndFullMode() {
        Game game = getStandardGame();
        Player dutch = game.getPlayerByNationId("model.nation.dutch");
        Europe europe = dutch.getEurope();
        UnitType colonist = spec().getUnitType("model.unit.freeColonist");
        UnitType other = null;
        for (UnitType candidate : spec().getUnitTypeList()) {
            if (candidate != colonist && candidate.isAvailableTo(dutch)) {
                other = candidate;
                break;
            }
        }
        assertNotNull(other);
        Role role = spec().getDefaultRole();
        europe.getRecruitables().clear();
        europe.addRecruitable(new AbstractUnit(colonist, role.getId(), 4), true);
        europe.addRecruitable(new AbstractUnit(other, role.getId(), 1), true);

        List<AbstractUnit> limited = europe.getExpandedRecruitables(false);
        assertEquals(Europe.MigrationType.getMigrantCount(), limited.size());
        for (AbstractUnit recruit : limited) {
            assertEquals(1, recruit.getNumber());
            assertSame(colonist, recruit.getType(spec()));
        }

        List<AbstractUnit> full = europe.getExpandedRecruitables(true);
        assertEquals(5, full.size());
        assertEquals(1, full.get(4).getNumber());
        assertSame(other, full.get(4).getType(spec()));
    }

    @Test
    public void testImmigrationIsClampedAgainstNegativeProduction() {
        Game game = getStandardGame();
        Player dutch = game.getPlayerByNationId("model.nation.dutch");
        Europe europe = dutch.getEurope();
        Unit colonist = new ServerUnit(game, europe, dutch,
            spec().getUnitType("model.unit.freeColonist"));

        int positiveProduction = europe.getImmigration(0);
        assertTrue(positiveProduction >= 0);
        assertEquals(5, europe.getImmigration(-5));
        colonist.dispose();
    }

    @Test
    public void testRecruitableLimitAndUnitPriceOverrides() {
        Game game = getStandardGame();
        Player dutch = game.getPlayerByNationId("model.nation.dutch");
        Europe europe = dutch.getEurope();
        UnitType type = spec().getUnitType("model.unit.freeColonist");
        Role role = spec().getDefaultRole();
        europe.getRecruitables().clear();
        for (int i = 0; i < Europe.MigrationType.getMigrantCount(); i++) {
            assertTrue(europe.addRecruitable(
                new AbstractUnit(type, role.getId(), 1), false));
        }
        assertFalse(europe.addRecruitable(
            new AbstractUnit(type, role.getId(), 1), false));
        assertTrue(europe.addRecruitable(
            new AbstractUnit(type, role.getId(), 1), true));
        assertEquals(4, europe.getRecruitables().size());

        assertEquals(type.getPrice(), europe.getUnitPrice(type));
        HashMap<UnitType, Integer> prices = new HashMap<>();
        prices.put(type, 73);
        europe.setUnitPrices(prices);
        assertEquals(73, europe.getUnitPrice(type));
        dutch.setImmigrationRequired(12);
        dutch.setImmigration(0);
        assertEquals(europe.getBaseRecruitPrice(),
                     europe.getCurrentRecruitPrice());
        dutch.setImmigration(20);
        assertEquals(europe.getRecruitLowerCap(),
                     europe.getCurrentRecruitPrice());

        Player arawak = game.getPlayerByNationId("model.nation.arawak");
        Europe nativeEurope = new ServerEurope(game, arawak);
        assertEquals(-1, nativeEurope.getCurrentRecruitPrice());
    }

    @Test
    public void testEuropePricesPurchasesAndSales() throws Exception {
        Game game = getStandardGame();
        Europe europe = game.getPlayerByNationId("model.nation.dutch").getEurope();
        GoodsType food = spec().getPrimaryFoodType();
        List<AbstractGoods> goods = java.util.Arrays.asList(
            new AbstractGoods(food, 2), new AbstractGoods(food, -1));

        int expected = europe.getOwner().getMarket().getBidPrice(food, 2)
            + europe.getOwner().getMarket().getSalePrice(food, -1);
        assertEquals(expected, europe.priceGoods(goods));
        assertTrue(europe.canBeInput());
        assertTrue(europe.getAvailableGoodsCount(food) > 0);
    }

    @Test
    public void testAddingUnitsSetsTheirEuropeState() {
        Game game = getStandardGame();
        Player dutch = game.getPlayerByNationId("model.nation.dutch");
        Europe europe = dutch.getEurope();
        UnitType colonistType = spec().getUnitType("model.unit.freeColonist");
        Unit colonist = new ServerUnit(game, null, dutch, colonistType);

        assertTrue(europe.add(colonist));
        assertSame(europe, colonist.getLocation());
        assertEquals(Unit.UnitState.SENTRY, colonist.getState());
        assertTrue(europe.remove(colonist));

        UnitType carrierType = spec().getUnitType("model.unit.caravel");
        Unit carrier = new ServerUnit(game, null, dutch, carrierType);
        assertTrue(carrier.canCarryUnits());
        assertTrue(europe.add(carrier));
        assertEquals(Unit.UnitState.ACTIVE, carrier.getState());
        assertTrue(europe.remove(carrier));
    }

    @Test
    public void testImportAndExportAmountsFollowTradePermission() {
        Game game = getStandardGame();
        Europe europe = game.getPlayerByNationId("model.nation.dutch").getEurope();
        GoodsType goodsType = spec().getPrimaryFoodType();

        assertTrue(europe.getOwner().canTrade(goodsType));
        assertEquals(GoodsContainer.HUGE_CARGO_SIZE,
                     europe.getImportAmount(goodsType, 1));
        assertEquals(GoodsContainer.HUGE_CARGO_SIZE,
                     europe.getExportAmount(goodsType, 1));
    }

    @Test
    public void testBoycottedGoodsCannotBeTradedAtEurope() throws Exception {
        Game game = getStandardGame();
        Europe europe = game.getPlayerByNationId("model.nation.dutch").getEurope();
        GoodsType goodsType = spec().getPrimaryFoodType();
        europe.getOwner().getMarket().setArrears(goodsType, 1);

        assertFalse(europe.getOwner().canTrade(goodsType));
        assertEquals(0, europe.getImportAmount(goodsType, 1));
        assertEquals(0, europe.getExportAmount(goodsType, 1));
        try {
            europe.priceGoods(java.util.Collections.singletonList(
                new AbstractGoods(goodsType, 1)));
            fail("Europe must reject trade in boycotted goods");
        } catch (FreeColException expected) {
            assertTrue(expected.getMessage().contains(goodsType.toString()));
        }
    }
}
