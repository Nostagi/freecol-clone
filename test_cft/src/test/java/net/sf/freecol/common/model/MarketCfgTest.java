package net.sf.freecol.common.model;

import java.io.StringReader;
import java.io.StringWriter;
import java.util.Comparator;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.BlockJUnit4ClassRunner;

import net.sf.freecol.common.io.FreeColXMLReader;
import net.sf.freecol.common.io.FreeColXMLWriter;
import net.sf.freecol.util.test.MockModelTestCase;


@RunWith(BlockJUnit4ClassRunner.class)
public class MarketCfgTest extends MockModelTestCase {

    private Player dutch;
    private Player english;
    private Market market;
    private GoodsType food;
    private GoodsType cotton;
    private GoodsType hammers;

    @Before
    public void setUpMarketFixture() {
        dutch = game.getPlayerByNationId("model.nation.dutch");
        english = game.getPlayerByNationId("model.nation.english");
        market = dutch.getMarket();
        food = spec().getPrimaryFoodType();
        cotton = spec().getGoodsType("model.goods.cotton");
        hammers = spec().getGoodsType("model.goods.hammers");
    }

    @Test
    public void testConstructorsAndOwnership() {
        assertEquals(dutch, market.getOwner());
        assertEquals(Market.TAG, market.getXMLTagName());

        Market idMarket = new Market(game, "customMarketId");
        assertEquals("customMarketId", idMarket.getId());
        assertNull(idMarket.getOwner());

        idMarket.setOwner(english);
        assertEquals(english, idMarket.getOwner());

        // Access enum
        assertEquals(2, Market.Access.values().length);
        assertEquals(Market.Access.EUROPE, Market.Access.valueOf("EUROPE"));
        assertEquals(Market.Access.CUSTOM_HOUSE, Market.Access.valueOf("CUSTOM_HOUSE"));
    }

    @Test
    public void testMarketDataQueries() {
        assertNull(market.getMarketData(null));
        assertNull(market.getMarketData(hammers)); // unstorable goods have no market data

        MarketData foodData = market.getMarketData(food);
        assertNotNull(foodData);
        assertEquals(food, foodData.getGoodsType());

        MarketData cottonData = market.getMarketData(cotton);
        assertNotNull(cottonData);
        assertEquals(cotton, cottonData.getGoodsType());

        assertEquals(spec().getStorableGoodsTypeList().size(), market.getMarketDataValues().size());
    }

    @Test
    public void testPricingMethodsControlFlow() {
        // getCostToBuy: null vs non-null
        assertEquals(0, market.getCostToBuy(null));
        assertEquals(0, market.getCostToBuy(hammers));
        assertTrue(market.getCostToBuy(food) > 0);

        // getPaidForSale: null vs non-null
        assertEquals(0, market.getPaidForSale(null));
        assertEquals(0, market.getPaidForSale(hammers));
        assertTrue(market.getPaidForSale(food) > 0);

        // getBidPrice: null, zero amount, non-zero amount
        assertEquals(0, market.getBidPrice(null, 10));
        assertEquals(0, market.getBidPrice(hammers, 10));
        assertEquals(0, market.getBidPrice(food, 0));
        assertEquals(5 * market.getCostToBuy(food), market.getBidPrice(food, 5));
        assertEquals(-2 * market.getCostToBuy(food), market.getBidPrice(food, -2));

        // getSalePrice(type, amount): null, zero amount, non-zero amount
        assertEquals(0, market.getSalePrice(null, 10));
        assertEquals(0, market.getSalePrice(hammers, 10));
        assertEquals(0, market.getSalePrice(food, 0));
        assertEquals(4 * market.getPaidForSale(food), market.getSalePrice(food, 4));

        // getSalePrice(AbstractGoods)
        Goods goods = new Goods(game, null, food, 7);
        assertEquals(market.getSalePrice(food, 7), market.getSalePrice(goods));
    }

    @Test
    public void testMarketTradingAndBottomlessCapacityControlFlow() {
        // hasBeenTraded: null vs untraded vs traded
        assertFalse(market.hasBeenTraded(null));
        assertFalse(market.hasBeenTraded(hammers));
        assertFalse(market.hasBeenTraded(food));

        int initialAmount = market.getAmountInMarket(food);
        assertTrue(initialAmount > 0);

        // Add goods: increases amount and marks traded
        market.addGoodsToMarket(food, 25);
        assertTrue(market.hasBeenTraded(food));
        assertEquals(initialAmount + 25, market.getAmountInMarket(food));

        // Add goods to unstorable/new goods type (triggers requireMarketData branch)
        GoodsType newType = new GoodsType("model.goods.customNew", spec());
        market.addGoodsToMarket(newType, 50);
        assertTrue(market.hasBeenTraded(newType));
        assertNotNull(market.getMarketData(newType));

        // Bottomless capacity: amount never drops below MINIMUM_AMOUNT (100)
        market.addGoodsToMarket(food, -1_000_000);
        assertTrue(market.getAmountInMarket(food) >= Market.MINIMUM_AMOUNT);

        // update(goodsType)
        market.update(food);
        market.update(newType);
    }

    @Test
    public void testSalesAndAccountingControlFlow() {
        // getSales: null vs non-null
        assertEquals(0, market.getSales(null));
        assertEquals(0, market.getSales(cotton));

        // modifySales: amount == 0 does nothing (doesn't mark traded)
        market.modifySales(cotton, 0);
        assertEquals(0, market.getSales(cotton));
        assertFalse(market.hasBeenTraded(cotton));

        // modifySales: amount != 0 updates sales and marks traded
        market.modifySales(cotton, 18);
        assertEquals(18, market.getSales(cotton));
        assertTrue(market.hasBeenTraded(cotton));

        market.modifySales(cotton, -6);
        assertEquals(12, market.getSales(cotton));

        // Income before taxes
        assertEquals(0, market.getIncomeBeforeTaxes(null));
        assertEquals(0, market.getIncomeBeforeTaxes(cotton));
        market.modifyIncomeBeforeTaxes(cotton, 140);
        assertEquals(140, market.getIncomeBeforeTaxes(cotton));

        // Income after taxes
        assertEquals(0, market.getIncomeAfterTaxes(null));
        assertEquals(0, market.getIncomeAfterTaxes(cotton));
        market.modifyIncomeAfterTaxes(cotton, 110);
        assertEquals(110, market.getIncomeAfterTaxes(cotton));

        // Arrears
        assertEquals(0, market.getArrears(null));
        assertEquals(0, market.getArrears(cotton));
        market.setArrears(cotton, 45);
        assertEquals(45, market.getArrears(cotton));

        // Initial price
        market.setInitialPrice(cotton, 11);
        assertEquals(11, market.getInitialPrice(cotton));

        // Amount in market: null vs non-null
        assertEquals(0, market.getAmountInMarket(null));
        assertTrue(market.getAmountInMarket(cotton) > 0);
    }

    @Test
    public void testPriceChangeDetectionAndMessageControlFlow() {
        // null or unstorable
        assertFalse(market.hasPriceChanged(null));
        assertFalse(market.hasPriceChanged(hammers));

        MarketData data = market.getMarketData(cotton);
        int currentPrice = data.getCostToBuy();

        // Initial state: oldPrice == 0 -> hasPriceChanged is false
        data.setOldPrice(0);
        assertFalse(market.hasPriceChanged(cotton));

        // oldPrice == costToBuy -> hasPriceChanged is false
        data.setOldPrice(currentPrice);
        assertFalse(market.hasPriceChanged(cotton));
        assertNull(market.makePriceChangeMessage(cotton));

        // Branch 1: newPrice > oldPrice (Price Increase message)
        data.setOldPrice(currentPrice - 2);
        assertTrue(market.hasPriceChanged(cotton));
        ModelMessage incMsg = market.makePriceChangeMessage(cotton);
        assertNotNull(incMsg);
        assertEquals("model.market.priceIncrease", incMsg.getId());

        // Branch 2: newPrice < oldPrice (Price Decrease message)
        data.setOldPrice(currentPrice + 2);
        assertTrue(market.hasPriceChanged(cotton));
        ModelMessage decMsg = market.makePriceChangeMessage(cotton);
        assertNotNull(decMsg);
        assertEquals("model.market.priceDecrease", decMsg.getId());

        // flushPriceChange: null does nothing, valid resets oldPrice
        market.flushPriceChange(null);
        market.flushPriceChange(cotton);
        assertFalse(market.hasPriceChanged(cotton));
        assertEquals(currentPrice, data.getOldPrice());
    }

    @Test
    public void testSalePriceComparatorControlFlow() {
        Comparator<AbstractGoods> comp = market.getSalePriceComparator();
        assertNotNull(comp);

        GoodsType silver = spec().getGoodsType("model.goods.silver");
        AbstractGoods expensiveGoods = new AbstractGoods(silver, 100);
        AbstractGoods cheapGoods = new AbstractGoods(food, 1);

        // Comparator is reversed by sale price: expensive before cheap (< 0)
        assertTrue(comp.compare(expensiveGoods, cheapGoods) < 0);
        assertTrue(comp.compare(cheapGoods, expensiveGoods) > 0);
        assertEquals(0, comp.compare(expensiveGoods, expensiveGoods));
    }

    @Test
    public void testTransactionListenersControlFlow() {
        TransactionListener listener1 = new TransactionListener() {
            @Override
            public void logPurchase(GoodsType type, int amount, int price) {}
            @Override
            public void logSale(GoodsType type, int amount, int price, int tax) {}
        };
        TransactionListener listener2 = new TransactionListener() {
            @Override
            public void logPurchase(GoodsType type, int amount, int price) {}
            @Override
            public void logSale(GoodsType type, int amount, int price, int tax) {}
        };

        assertEquals(0, market.getTransactionListener().length);

        market.addTransactionListener(listener1);
        assertEquals(1, market.getTransactionListener().length);
        assertSame(listener1, market.getTransactionListener()[0]);

        market.addTransactionListener(listener2);
        assertEquals(2, market.getTransactionListener().length);

        // Remove listener1
        market.removeTransactionListener(listener1);
        assertEquals(1, market.getTransactionListener().length);
        assertSame(listener2, market.getTransactionListener()[0]);

        // Removing non-registered listener leaves listeners unchanged
        market.removeTransactionListener(listener1);
        assertEquals(1, market.getTransactionListener().length);

        // Remove listener2
        market.removeTransactionListener(listener2);
        assertEquals(0, market.getTransactionListener().length);
    }

    @Test
    public void testGetLinkTargetControlFlow() {
        // Branch 1: player == owner -> returns owner.getEurope()
        assertEquals(dutch.getEurope(), market.getLinkTarget(dutch));

        // Branch 2: player != owner -> returns null
        assertNull(market.getLinkTarget(english));
        assertNull(market.getLinkTarget(null));
    }

    @Test
    public void testCopyInAndToStringControlFlow() {
        Game game2 = getStandardGame();
        Market target = game2.getPlayerByNationId("model.nation.dutch").getMarket();

        // Fail branches
        assertFalse(target.copyIn(null));
        assertFalse(target.copyIn(dutch));
        Market differentIdMarket = new Market(game2, "differentId");
        assertFalse(target.copyIn(differentIdMarket));

        // Success branch
        market.modifySales(food, 15);
        market.setArrears(food, 20);
        assertTrue(target.copyIn(market));
        assertEquals(dutch.getId(), target.getOwner().getId());
        assertEquals(15, target.getSales(food));
        assertEquals(20, target.getArrears(food));

        // toString branch: owner != null
        String strWithOwner = market.toString();
        assertTrue(strWithOwner.contains(market.getId()));
        assertTrue(strWithOwner.contains("owner=" + dutch.getId()));

        // toString branch: owner == null
        Market noOwnerMarket = new Market(game, "noOwnerId");
        String strNullOwner = noOwnerMarket.toString();
        assertTrue(strNullOwner.contains("owner=null"));
    }

    @Test
    public void testSerializationRoundTrip() throws Exception {
        market.modifySales(food, 50);
        market.setArrears(food, 10);

        String xml = market.serialize();
        assertNotNull(xml);
        assertTrue(xml.contains(Market.TAG));
        assertTrue(xml.contains(MarketData.TAG));

        Market loadedMarket = new Market(game, "newId");
        FreeColXMLReader reader = new FreeColXMLReader(new StringReader(xml));
        reader.nextTag();
        loadedMarket.readFromXML(reader);
        reader.close();

        assertEquals(dutch, loadedMarket.getOwner());
        assertEquals(50, loadedMarket.getSales(food));
        assertEquals(10, loadedMarket.getArrears(food));
    }
}
