package net.sf.freecol.common.model;

import java.io.StringReader;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.BlockJUnit4ClassRunner;

import net.sf.freecol.common.io.FreeColXMLReader;
import net.sf.freecol.common.model.NativeTrade.NativeTradeAction;
import net.sf.freecol.common.model.Stance;
import net.sf.freecol.server.model.ServerUnit;
import net.sf.freecol.util.test.MockModelTestCase;


@RunWith(BlockJUnit4ClassRunner.class)
public class NativeTradeCfgTest extends MockModelTestCase {

    private Player dutch;
    private Player arawak;
    private IndianSettlement settlement;
    private Unit ship;
    private Unit colonist;
    private GoodsType foodType;
    private GoodsType cottonType;

    @Before
    public void setUpNativeTradeFixture() {
        Map map = getTestMap(spec().getTileType("model.tile.plains"), true);
        game.changeMap(map);
        dutch = game.getPlayerByNationId("model.nation.dutch");
        arawak = game.getPlayerByNationId("model.nation.arawak");
        settlement = new IndianSettlementBuilder(game).player(arawak).build();
        ship = new ServerUnit(game, null, dutch,
            spec().getUnitType("model.unit.caravel"));
        colonist = new ServerUnit(game, null, dutch,
            spec().getUnitType("model.unit.freeColonist"));
        foodType = spec().getPrimaryFoodType();
        cottonType = spec().getGoodsType("model.goods.cotton");
    }

    @Test
    public void testNativeTradeActionEnumAttributes() {
        for (NativeTradeAction action : NativeTradeAction.values()) {
            assertNotNull(action.name());
        }

        // Test isClosing() branches
        assertTrue(NativeTradeAction.CLOSE.isClosing());
        assertTrue(NativeTradeAction.NAK_HAGGLE.isClosing());
        assertTrue(NativeTradeAction.NAK_HOSTILE.isClosing());
        assertTrue(NativeTradeAction.NAK_NOSALE.isClosing());
        assertTrue(NativeTradeAction.NAK_INVALID.isClosing());

        assertFalse(NativeTradeAction.OPEN.isClosing());
        assertFalse(NativeTradeAction.BUY.isClosing());
        assertFalse(NativeTradeAction.SELL.isClosing());
        assertFalse(NativeTradeAction.GIFT.isClosing());
        assertFalse(NativeTradeAction.ACK_OPEN.isClosing());
        assertFalse(NativeTradeAction.ACK_BUY.isClosing());
        assertFalse(NativeTradeAction.ACK_SELL.isClosing());
        assertFalse(NativeTradeAction.ACK_GIFT.isClosing());
        assertFalse(NativeTradeAction.ACK_BUY_HAGGLE.isClosing());
        assertFalse(NativeTradeAction.ACK_SELL_HAGGLE.isClosing());
        assertFalse(NativeTradeAction.NAK_GOODS.isClosing());

        // Test isEuropean() branches
        assertTrue(NativeTradeAction.OPEN.isEuropean());
        assertTrue(NativeTradeAction.CLOSE.isEuropean());
        assertTrue(NativeTradeAction.BUY.isEuropean());
        assertTrue(NativeTradeAction.SELL.isEuropean());
        assertTrue(NativeTradeAction.GIFT.isEuropean());

        assertFalse(NativeTradeAction.ACK_OPEN.isEuropean());
        assertFalse(NativeTradeAction.ACK_BUY.isEuropean());
        assertFalse(NativeTradeAction.ACK_SELL.isEuropean());
        assertFalse(NativeTradeAction.ACK_GIFT.isEuropean());
        assertFalse(NativeTradeAction.ACK_BUY_HAGGLE.isEuropean());
        assertFalse(NativeTradeAction.ACK_SELL_HAGGLE.isEuropean());
        assertFalse(NativeTradeAction.NAK_GOODS.isEuropean());
        assertFalse(NativeTradeAction.NAK_HAGGLE.isEuropean());
        assertFalse(NativeTradeAction.NAK_HOSTILE.isEuropean());
        assertFalse(NativeTradeAction.NAK_NOSALE.isEuropean());
        assertFalse(NativeTradeAction.NAK_INVALID.isEuropean());
    }

    @Test
    public void testConstructorsAndBasicGetters() {
        NativeTrade rawTrade = new NativeTrade(game, "customTradeId");
        assertEquals("customTradeId", rawTrade.getId());
        assertEquals(NativeTrade.TAG, rawTrade.getXMLTagName());
        assertFalse(rawTrade.isInternable());

        NativeTrade trade = new NativeTrade(ship, settlement);
        assertSame(ship, trade.getUnit());
        assertSame(settlement, trade.getIndianSettlement());
        assertEquals(0, trade.getCount());
        assertTrue(trade.getBuy());
        assertTrue(trade.getSell());
        assertTrue(trade.getGift());

        String expectedKey = ship.getId() + "-" + settlement.getId();
        assertEquals(expectedKey, trade.getKey());
        assertEquals(expectedKey, NativeTrade.getNativeTradeKey(ship, settlement));
    }

    @Test
    public void testTradeFlagsAndHasNotTradedControlFlow() {
        NativeTrade trade = new NativeTrade(ship, settlement);

        // Path: (buy=T, sell=T, gift=T) -> true
        assertTrue(trade.hasNotTraded());

        // Path: (buy=F, sell=T, gift=T) -> false
        trade.setBuy(false);
        assertFalse(trade.getBuy());
        assertFalse(trade.hasNotTraded());
        trade.setBuy(true);

        // Path: (buy=T, sell=F, gift=T) -> false
        trade.setSell(false);
        assertFalse(trade.getSell());
        assertFalse(trade.hasNotTraded());
        trade.setSell(true);

        // Path: (buy=T, sell=T, gift=F) -> false
        trade.setGift(false);
        assertFalse(trade.getGift());
        assertFalse(trade.hasNotTraded());

        // Path: (buy=F, sell=F, gift=F) -> false
        trade.setBuy(false);
        trade.setSell(false);
        assertFalse(trade.hasNotTraded());
    }

    @Test
    public void testHaggleCalculationsAndBoundaries() {
        // haggleUp: (price * 11) / 10
        assertEquals(0, NativeTrade.haggleUp(0));
        assertEquals(11, NativeTrade.haggleUp(10));
        assertEquals(22, NativeTrade.haggleUp(20));
        assertEquals(60, NativeTrade.haggleUp(55));
        assertEquals(110, NativeTrade.haggleUp(100));

        // haggleDown: (price * 9) / 10
        assertEquals(0, NativeTrade.haggleDown(0));
        assertEquals(9, NativeTrade.haggleDown(10));
        assertEquals(18, NativeTrade.haggleDown(20));
        assertEquals(49, NativeTrade.haggleDown(55));
        assertEquals(90, NativeTrade.haggleDown(100));
    }

    @Test
    public void testCanBuyControlFlowBranches() {
        NativeTrade trade = new NativeTrade(ship, settlement);
        NativeTradeItem validItem = new NativeTradeItem(game, arawak, dutch,
            new Goods(game, settlement, foodType, 50));
        validItem.setPrice(30);

        // Branch 1: getBuy() == false -> false
        trade.setBuy(false);
        trade.getSettlementToUnit().add(validItem);
        assertFalse(trade.canBuy());
        trade.setBuy(true);

        // Branch 2: atWar() == true -> false
        arawak.setStance(dutch, Stance.WAR);
        assertFalse(trade.canBuy());
        arawak.setStance(dutch, Stance.PEACE);

        // Branch 3: spaceLeft <= 0 -> false
        int initialSpace = ship.getSpaceLeft();
        assertTrue(initialSpace > 0);
        for (int i = 0; i < initialSpace; i++) {
            ship.getGoodsContainer().addGoods(foodType, 100);
        }
        assertEquals(0, ship.getSpaceLeft());
        assertFalse(trade.canBuy());

        // Restore space
        ship.getGoodsContainer().removeAll();
        assertTrue(ship.getSpaceLeft() > 0);

        // Branch 4: settlementToUnit is empty -> false
        trade.getSettlementToUnit().clear();
        assertFalse(trade.canBuy());

        // Branch 5: settlementToUnit has items, but no valid price -> false
        NativeTradeItem invalidItem = new NativeTradeItem(game, arawak, dutch,
            new Goods(game, settlement, foodType, 50));
        invalidItem.setPrice(NativeTradeItem.PRICE_INVALID);
        NativeTradeItem unsetPriceItem = new NativeTradeItem(game, arawak, dutch,
            new Goods(game, settlement, foodType, 50));
        unsetPriceItem.setPrice(NativeTradeItem.PRICE_UNSET);
        trade.getSettlementToUnit().add(invalidItem);
        trade.getSettlementToUnit().add(unsetPriceItem);
        assertFalse(trade.canBuy());

        // Branch 6: all conditions satisfied -> true
        trade.getSettlementToUnit().add(validItem);
        assertTrue(trade.canBuy());
    }

    @Test
    public void testCanSellControlFlowBranches() {
        NativeTrade trade = new NativeTrade(ship, settlement);
        NativeTradeItem validSale = new NativeTradeItem(game, dutch, arawak,
            new Goods(game, ship, cottonType, 100));
        validSale.setPrice(45);

        // Branch 1: getSell() == false -> false
        trade.setSell(false);
        trade.addToUnit(validSale);
        assertFalse(trade.canSell());
        trade.setSell(true);

        // Branch 2: atWar() == true -> false
        arawak.setStance(dutch, Stance.WAR);
        assertFalse(trade.canSell());
        arawak.setStance(dutch, Stance.PEACE);

        // Branch 3: unitToSettlement is empty -> false
        trade.getUnitToSettlement().clear();
        assertFalse(trade.canSell());

        // Branch 4: unitToSettlement items have invalid prices -> false
        NativeTradeItem invalidSale = new NativeTradeItem(game, dutch, arawak,
            new Goods(game, ship, cottonType, 100));
        invalidSale.setPrice(NativeTradeItem.PRICE_INVALID);
        trade.addToUnit(invalidSale);
        assertFalse(trade.canSell());

        // Branch 5: all conditions satisfied -> true
        trade.addToUnit(validSale);
        assertTrue(trade.canSell());
    }

    @Test
    public void testCanGiftControlFlowBranches() {
        NativeTrade trade = new NativeTrade(ship, settlement);

        // Branch 1: gift == false, no goods -> false
        trade.setGift(false);
        assertFalse(ship.hasGoodsCargo());
        assertFalse(trade.canGift());

        // Branch 2: gift == true, no cargo -> false
        trade.setGift(true);
        assertFalse(trade.canGift());

        // Branch 3: gift == true, has cargo -> true
        ship.getGoodsContainer().addGoods(foodType, 50);
        assertTrue(ship.hasGoodsCargo());
        assertTrue(trade.canGift());

        // Branch 4: gift == false, has cargo -> false
        trade.setGift(false);
        assertFalse(trade.canGift());
    }

    @Test
    public void testGetDoneControlFlowBranches() {
        NativeTrade trade = new NativeTrade(ship, settlement);

        // Subcase 1: count < 0 (via setDone()) -> true
        trade.setDone();
        assertEquals(-1, trade.getCount());
        assertTrue(trade.getDone());

        // Reset count >= 0
        trade.setCount(2);
        assertEquals(2, trade.getCount());

        // Subcase 2: count >= 0, canGift is true -> getDone() is false
        ship.getGoodsContainer().addGoods(foodType, 100);
        trade.setBuy(false);
        trade.setSell(false);
        trade.setGift(true);
        assertTrue(trade.canGift());
        assertFalse(trade.getDone());

        // Subcase 3: count >= 0, canSell is true -> getDone() is false
        trade.setGift(false);
        NativeTradeItem validSale = new NativeTradeItem(game, dutch, arawak,
            new Goods(game, ship, cottonType, 100));
        validSale.setPrice(50);
        trade.addToUnit(validSale);
        trade.setSell(true);
        assertTrue(trade.canSell());
        assertFalse(trade.getDone());

        // Subcase 4: count >= 0, canBuy is true -> getDone() is false
        trade.setSell(false);
        NativeTradeItem validPurchase = new NativeTradeItem(game, arawak, dutch,
            new Goods(game, settlement, foodType, 50));
        validPurchase.setPrice(25);
        trade.getSettlementToUnit().add(validPurchase);
        trade.setBuy(true);
        assertTrue(trade.canBuy());
        assertFalse(trade.getDone());

        // Subcase 5: count >= 0, canBuy=false, canSell=false, canGift=false -> getDone() is true
        trade.setBuy(false);
        trade.setSell(false);
        trade.setGift(false);
        assertFalse(trade.canBuy());
        assertFalse(trade.canSell());
        assertFalse(trade.canGift());
        assertTrue(trade.getDone());
    }

    @Test
    public void testItemManagementAndRemoveFromUnit() {
        NativeTrade trade = new NativeTrade(ship, settlement);
        assertNull(trade.getItem());

        NativeTradeItem item1 = new NativeTradeItem(game, dutch, arawak,
            new Goods(game, ship, cottonType, 100));
        item1.setPrice(40);
        trade.setItem(item1);
        assertSame(item1, trade.getItem());

        NativeTradeItem item2 = new NativeTradeItem(game, dutch, arawak,
            new Goods(game, ship, foodType, 80));
        item2.setPrice(30);

        trade.addToUnit(item1);
        trade.addToUnit(item2);
        assertEquals(2, trade.getUnitToSettlement().size());

        // Remove item1 matching goods
        trade.removeFromUnit(item1);
        assertEquals(1, trade.getUnitToSettlement().size());
        assertSame(item2, trade.getUnitToSettlement().get(0));

        // Removing non-existent item should not alter list
        trade.removeFromUnit(item1);
        assertEquals(1, trade.getUnitToSettlement().size());
    }

    @Test
    public void testLimitSettlementToUnitControlFlow() {
        NativeTrade trade = new NativeTrade(ship, settlement);

        NativeTradeItem item1 = new NativeTradeItem(game, arawak, dutch,
            new Goods(game, settlement, foodType, 100));
        item1.setPrice(100);

        NativeTradeItem item2 = new NativeTradeItem(game, arawak, dutch,
            new Goods(game, settlement, cottonType, 100));
        item2.setPrice(60);

        NativeTradeItem item3 = new NativeTradeItem(game, arawak, dutch,
            new Goods(game, settlement, foodType, 50));
        item3.setPrice(30);

        NativeTradeItem invalidItem = new NativeTradeItem(game, arawak, dutch,
            new Goods(game, settlement, cottonType, 50));
        invalidItem.setPrice(NativeTradeItem.PRICE_INVALID);

        trade.getSettlementToUnit().add(item1);
        trade.getSettlementToUnit().add(item2);
        trade.getSettlementToUnit().add(item3);
        trade.getSettlementToUnit().add(invalidItem);

        // Branch 1: best.size() <= n -> returns early, no modification
        trade.limitSettlementToUnit(5);
        assertEquals(100, item1.getPrice());
        assertEquals(60, item2.getPrice());
        assertEquals(30, item3.getPrice());
        assertEquals(NativeTradeItem.PRICE_INVALID, invalidItem.getPrice());

        // Branch 2: best.size() > n -> sublist beyond n set to PRICE_INVALID
        trade.limitSettlementToUnit(1);
        assertEquals(100, item1.getPrice());
        assertEquals(NativeTradeItem.PRICE_INVALID, item2.getPrice());
        assertEquals(NativeTradeItem.PRICE_INVALID, item3.getPrice());
        assertEquals(NativeTradeItem.PRICE_INVALID, invalidItem.getPrice());
    }

    @Test
    public void testCompatibilityAndMergeFromControlFlow() {
        NativeTrade trade1 = new NativeTrade(ship, settlement);
        trade1.internId("trade1");
        NativeTrade trade2 = new NativeTrade(ship, settlement);
        trade2.internId("trade2");

        Unit otherUnit = new ServerUnit(game, null, dutch,
            spec().getUnitType("model.unit.caravel"));
        IndianSettlement otherSettlement = new IndianSettlementBuilder(game)
            .player(arawak).build();
        NativeTrade diffUnitTrade = new NativeTrade(otherUnit, settlement);
        NativeTrade diffSettlementTrade = new NativeTrade(ship, otherSettlement);

        // Compatibility check
        assertTrue(trade1.isCompatible(trade2));
        assertFalse(trade1.isCompatible(diffUnitTrade));
        assertFalse(trade1.isCompatible(diffSettlementTrade));

        // Set content on trade2
        NativeTradeItem itemSale = new NativeTradeItem(game, dutch, arawak,
            new Goods(game, ship, cottonType, 100));
        itemSale.setPrice(55);
        NativeTradeItem itemBuy = new NativeTradeItem(game, arawak, dutch,
            new Goods(game, settlement, foodType, 100));
        itemBuy.setPrice(40);
        trade2.addToUnit(itemSale);
        trade2.getSettlementToUnit().add(itemBuy);
        trade2.setItem(itemSale);

        // Merge branch 1: incompatible -> no change
        trade1.mergeFrom(diffUnitTrade);
        assertEquals(0, trade1.getUnitToSettlement().size());
        assertEquals(0, trade1.getSettlementToUnit().size());
        assertNull(trade1.getItem());

        // Merge branch 2: this.equals(nt) -> no change
        trade1.mergeFrom(trade1);
        assertEquals(0, trade1.getUnitToSettlement().size());

        // Merge branch 3: compatible and not equal -> copies collections and item
        trade1.mergeFrom(trade2);
        assertEquals(1, trade1.getUnitToSettlement().size());
        assertSame(itemSale, trade1.getUnitToSettlement().get(0));
        assertEquals(1, trade1.getSettlementToUnit().size());
        assertSame(itemBuy, trade1.getSettlementToUnit().get(0));
        assertSame(itemSale, trade1.getItem());
    }

    @Test
    public void testInitializeWithUnitCargoAndSettlementGoods() {
        ship.getGoodsContainer().addGoods(cottonType, 100);
        settlement.getGoodsContainer().addGoods(foodType, 200);

        NativeTrade trade = new NativeTrade(ship, settlement);
        trade.initialize();

        assertTrue(trade.getUnitToSettlement().size() >= 1);
        assertEquals(cottonType, trade.getUnitToSettlement().get(0).getGoods().getType());
        assertTrue(trade.getSettlementToUnit().size() >= 1);
    }

    @Test
    public void testCopyInAndToString() {
        NativeTrade trade1 = new NativeTrade(ship, settlement);
        trade1.setCount(3);
        trade1.setBuy(false);
        trade1.setSell(true);
        trade1.setGift(false);

        NativeTradeItem item = new NativeTradeItem(game, dutch, arawak,
            new Goods(game, ship, cottonType, 100));
        item.setPrice(35);
        item.intern();
        trade1.setItem(item);
        trade1.addToUnit(item);

        // copyIn failure branches
        assertFalse(trade1.copyIn(null));
        assertFalse(trade1.copyIn(colonist));

        // copyIn success branch
        NativeTrade trade2 = new NativeTrade(colonist, settlement);
        assertTrue(trade2.copyIn(trade1));
        assertEquals(ship, trade2.getUnit());
        assertEquals(settlement, trade2.getIndianSettlement());
        assertEquals(3, trade2.getCount());
        assertFalse(trade2.getBuy());
        assertTrue(trade2.getSell());
        assertFalse(trade2.getGift());

        // toString branch testing: with item != null and with item == null
        String strWithItem = trade1.toString();
        assertTrue(strWithItem.contains(NativeTrade.TAG));
        assertTrue(strWithItem.contains(ship.getId()));
        assertTrue(strWithItem.contains("buy=false"));

        trade1.setItem(null);
        String strNullItem = trade1.toString();
        assertTrue(strNullItem.contains("item=null"));
    }

    @Test
    public void testSerializationRoundTrip() throws Exception {
        NativeTrade originalTrade = new NativeTrade(ship, settlement);
        originalTrade.setCount(1);
        originalTrade.setBuy(true);
        originalTrade.setSell(false);
        originalTrade.setGift(true);

        NativeTradeItem item = new NativeTradeItem(game, dutch, arawak,
            new Goods(game, ship, cottonType, 100));
        item.setPrice(44);
        originalTrade.addToUnit(item);
        originalTrade.setItem(item);

        String xml = originalTrade.serialize();
        assertNotNull(xml);
        assertTrue(xml.contains(NativeTrade.TAG));

        NativeTrade loadedTrade = new NativeTrade(game, "dummyId");
        FreeColXMLReader reader = new FreeColXMLReader(new StringReader(xml));
        reader.nextTag();
        loadedTrade.readFromXML(reader);
        reader.close();

        assertEquals(ship, loadedTrade.getUnit());
        assertEquals(settlement, loadedTrade.getIndianSettlement());
        assertEquals(1, loadedTrade.getCount());
        assertTrue(loadedTrade.getBuy());
        assertFalse(loadedTrade.getSell());
        assertTrue(loadedTrade.getGift());
        assertEquals(1, loadedTrade.getUnitToSettlement().size());
        assertNotNull(loadedTrade.getItem());
        assertEquals(44, loadedTrade.getItem().getPrice());
    }
}
