package net.sf.freecol.common.model;

import java.io.StringReader;
import java.io.StringWriter;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import junit.framework.TestCase;
import net.sf.freecol.common.io.FreeColXMLReader;
import net.sf.freecol.common.io.FreeColXMLWriter;
import net.sf.freecol.server.model.ServerGame;

public class MarketDataCfgTest extends TestCase {
    private Specification local;
    private Game game;
    private GoodsType goods;
    private GoodsType input;

    protected void setUp() throws Exception {
        super.setUp();
        local = new Specification();
        game = new ServerGame(local);
        goods = type("model.goods.cft");
        input = type("model.goods.cftInput");
    }

    private void field(Object target, Class<?> owner, String name, Object value) throws Exception {
        Field field = owner.getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }

    private GoodsType type(String id) throws Exception {
        GoodsType result = local.findType(id, GoodsType.class);
        field(result, GoodsType.class, "storable", true);
        field(result, GoodsType.class, "initialAmount", 100);
        field(result, GoodsType.class, "initialPrice", 5);
        field(result, GoodsType.class, "priceDiff", 2);
        return result;
    }

    private MarketData pricing(int buy, int sale, int quantity) {
        MarketData result = new MarketData(game, goods);
        result.setCostToBuy(buy);
        result.setPaidForSale(sale);
        result.setAmountInMarket(quantity);
        return result;
    }

    private List<String> events(MarketData data) {
        List<String> result = new ArrayList<String>();
        data.addPropertyChangeListener(e -> result.add(e.getPropertyName()
                + ":" + e.getOldValue() + "->" + e.getNewValue()));
        return result;
    }

    private String xml(MarketData data) throws Exception {
        StringWriter out = new StringWriter();
        try (FreeColXMLWriter writer = new FreeColXMLWriter(out,
                FreeColXMLWriter.WriteScope.toSave(), false)) {
            data.toXML(writer);
            writer.flush();
        }
        return out.toString();
    }

    private void read(MarketData data, String text) throws Exception {
        try (FreeColXMLReader reader = new FreeColXMLReader(new StringReader(text))) {
            reader.nextTag();
            data.readFromXML(reader);
        }
    }

    private MarketData readData(String attributes) throws Exception {
        MarketData result = new MarketData(game, "marketData:xml");
        read(result, "<marketData id=\"marketData:xml\" goods-type=\"model.goods.cft\" " + attributes + "/>");
        return result;
    }

    public void testTC001() throws Exception {
        MarketData d = new MarketData(game, goods);
        assertSame(goods, d.getGoodsType());
        assertEquals(7, d.getCostToBuy());
        assertEquals(5, d.getPaidForSale());
        assertEquals(100, d.getAmountInMarket());
        assertEquals(5, d.getInitialPrice());
        assertEquals(7, d.getOldPrice());
        assertEquals(0, d.getArrears());
        assertEquals(0, d.getSales());
        assertEquals(0, d.getIncomeBeforeTaxes());
        assertEquals(0, d.getIncomeAfterTaxes());
        assertFalse(d.getTraded());
        assertEquals("marketData", d.getXMLTagName());
        assertEquals(1, MarketData.MINIMUM_PRICE);
        assertEquals(19, MarketData.MAXIMUM_PRICE);
    }

    public void testTC002() throws Exception {
        MarketData d = new MarketData(game, "marketData:cft");
        assertEquals("marketData:cft", d.getId());
        assertSame(null, d.getGoodsType());
        assertEquals(0, d.getCostToBuy());
        assertEquals(0, d.getPaidForSale());
        assertEquals(0, d.getAmountInMarket());
        assertEquals(0, d.getInitialPrice());
        assertEquals(0, d.getOldPrice());
        assertFalse(d.getTraded());
    }

    public void testTC003() throws Exception {
        MarketData d = new MarketData(game, goods);
        d.setCostToBuy(12);
        d.setPaidForSale(10);
        d.setAmountInMarket(83);
        d.setInitialPrice(6);
        d.setArrears(37);
        d.setSales(23);
        d.setIncomeBeforeTaxes(230);
        d.setIncomeAfterTaxes(184);
        d.setOldPrice(9);
        d.setTraded(true);
        assertEquals(12, d.getCostToBuy());
        assertEquals(10, d.getPaidForSale());
        assertEquals(83, d.getAmountInMarket());
        assertEquals(6, d.getInitialPrice());
        assertEquals(37, d.getArrears());
        assertEquals(23, d.getSales());
        assertEquals(230, d.getIncomeBeforeTaxes());
        assertEquals(184, d.getIncomeAfterTaxes());
        assertEquals(9, d.getOldPrice());
        assertTrue(d.getTraded());
        d.setTraded(false);
        assertFalse(d.getTraded());
    }

    public void testTC004() throws Exception {
        MarketData d = new MarketData(game, goods);
        d.setCostToBuy(-3);
        d.setPaidForSale(-5);
        d.setAmountInMarket(-7);
        d.setInitialPrice(-9);
        d.setArrears(-11);
        d.setSales(-13);
        d.setIncomeBeforeTaxes(-17);
        d.setIncomeAfterTaxes(-19);
        d.setOldPrice(-23);
        assertEquals(-3, d.getCostToBuy());
        assertEquals(-5, d.getPaidForSale());
        assertEquals(-7, d.getAmountInMarket());
        assertEquals(-9, d.getInitialPrice());
        assertEquals(-11, d.getArrears());
        assertEquals(-13, d.getSales());
        assertEquals(-17, d.getIncomeBeforeTaxes());
        assertEquals(-19, d.getIncomeAfterTaxes());
        assertEquals(-23, d.getOldPrice());
    }

    public void testTC005() throws Exception {
        field(goods, GoodsType.class, "storable", false);
        MarketData d = pricing(7, 5, 100);
        List<String> changes = events(d);
        assertFalse(d.price());
        assertEquals(7, d.getCostToBuy());
        assertEquals(5, d.getPaidForSale());
        assertEquals(100, d.getAmountInMarket());
        assertTrue(changes.isEmpty());
    }

    public void testTC006() throws Exception {
        MarketData d = pricing(7, 5, 100);
        List<String> changes = events(d);
        assertFalse(d.price());
        assertEquals(7, d.getCostToBuy());
        assertEquals(5, d.getPaidForSale());
        assertEquals(100, d.getAmountInMarket());
        assertTrue(changes.isEmpty());
    }

    public void testTC007() throws Exception {
        MarketData d = pricing(7, 5, 83);
        List<String> changes = events(d);
        assertTrue(d.price());
        assertEquals(8, d.getCostToBuy());
        assertEquals(6, d.getPaidForSale());
        assertEquals(83, d.getAmountInMarket());
        assertEquals(1, changes.size());
        assertEquals("model.goods.cft:7->8", changes.get(0));
    }

    public void testTC008() throws Exception {
        MarketData d = pricing(7, 5, 125);
        assertTrue(d.price());
        assertEquals(6, d.getCostToBuy());
        assertEquals(4, d.getPaidForSale());
        assertEquals(125, d.getAmountInMarket());
    }

    public void testTC009() throws Exception {
        MarketData d = pricing(7, 5, 50);
        assertTrue(d.price());
        assertEquals(9, d.getCostToBuy());
        assertEquals(7, d.getPaidForSale());
        assertEquals(71, d.getAmountInMarket());
    }

    public void testTC010() throws Exception {
        MarketData d = pricing(7, 5, 250);
        assertTrue(d.price());
        assertEquals(5, d.getCostToBuy());
        assertEquals(3, d.getPaidForSale());
        assertEquals(167, d.getAmountInMarket());
    }

    public void testTC011() throws Exception {
        MarketData d = pricing(0, 5, 50);
        assertTrue(d.price());
        assertEquals(12, d.getCostToBuy());
        assertEquals(10, d.getPaidForSale());
        assertEquals(50, d.getAmountInMarket());
    }

    public void testTC012() throws Exception {
        field(goods, GoodsType.class, "newWorldGoods", true);
        MarketData d = pricing(7, 5, 20);
        assertTrue(d.price());
        assertEquals(9, d.getCostToBuy());
        assertEquals(7, d.getPaidForSale());
        assertEquals(20, d.getAmountInMarket());
    }

    public void testTC013() throws Exception {
        field(input, GoodsType.class, "newWorldGoods", true);
        field(goods, GoodsType.class, "madeFrom", input);
        MarketData d = pricing(0, 5, 20);
        assertTrue(d.price());
        assertEquals(9, d.getCostToBuy());
        assertEquals(7, d.getPaidForSale());
    }

    public void testTC014() throws Exception {
        field(goods, GoodsType.class, "madeFrom", input);
        MarketData d = pricing(0, 5, 50);
        assertTrue(d.price());
        assertEquals(12, d.getCostToBuy());
        assertEquals(10, d.getPaidForSale());
    }

    public void testTC015() throws Exception {
        field(goods, GoodsType.class, "newWorldGoods", true);
        MarketData d = pricing(7, 5, 71);
        assertTrue(d.price());
        assertEquals(9, d.getCostToBuy());
        assertEquals(7, d.getPaidForSale());
        assertEquals(71, d.getAmountInMarket());
    }

    public void testTC016() throws Exception {
        MarketData d = pricing(0, 5, 10);
        assertTrue(d.price());
        assertEquals(19, d.getCostToBuy());
        assertEquals(17, d.getPaidForSale());
        assertEquals(10, d.getAmountInMarket());
    }

    public void testTC017() throws Exception {
        MarketData d = pricing(0, 5, 2000);
        assertTrue(d.price());
        assertEquals(3, d.getCostToBuy());
        assertEquals(1, d.getPaidForSale());
        assertEquals(2000, d.getAmountInMarket());
    }

    public void testTC018() throws Exception {
        MarketData d = pricing(7, 1, 100);
        List<String> changes = events(d);
        assertTrue(d.price());
        assertEquals(7, d.getCostToBuy());
        assertEquals(5, d.getPaidForSale());
        assertEquals(1, changes.size());
        assertEquals("model.goods.cft:1->5", changes.get(0));
    }

    public void testTC019() throws Exception {
        MarketData d = pricing(19, 17, 10);
        List<String> changes = events(d);
        assertFalse(d.price());
        assertEquals(19, d.getCostToBuy());
        assertEquals(17, d.getPaidForSale());
        assertEquals(26, d.getAmountInMarket());
        assertTrue(changes.isEmpty());
    }

    public void testTC020() throws Exception {
        MarketData d = pricing(7, 5, 50);
        List<String> changes = events(d);
        d.update();
        assertEquals(12, d.getCostToBuy());
        assertEquals(10, d.getPaidForSale());
        assertEquals(50, d.getAmountInMarket());
        assertEquals(7, d.getOldPrice());
        assertEquals("model.goods.cft:-1->12", changes.get(0));
    }

    public void testTC021() throws Exception {
        field(goods, GoodsType.class, "storable", false);
        MarketData d = pricing(7, 5, 100);
        d.update();
        assertEquals(-1, d.getCostToBuy());
        assertEquals(5, d.getPaidForSale());
        assertEquals(7, d.getOldPrice());
    }

    public void testTC022() throws Exception {
        field(goods, GoodsType.class, "priceDiff", 0);
        MarketData d = pricing(7, 5, 50);
        assertTrue(d.price());
        assertEquals(7, d.getCostToBuy());
        assertEquals(7, d.getPaidForSale());
        assertEquals(71, d.getAmountInMarket());
    }

    public void testTC023() throws Exception {
        MarketData d = new MarketData(game, goods);
        assertFalse(d.copyIn((FreeColObject)null));
        assertFalse(d.copyIn(StringTemplate.key(d.getId())));
        assertFalse(d.copyIn(new MarketData(game, goods)));
    }

    public void testTC024() throws Exception {
        MarketData d = new MarketData(game, "marketData:copy");
        Game sourceGame = new ServerGame(local);
        MarketData source = new MarketData(sourceGame, d.getId());
        field(source, MarketData.class, "goodsType", goods);
        source.setCostToBuy(12);
        source.setPaidForSale(10);
        source.setAmountInMarket(83);
        source.setInitialPrice(6);
        source.setArrears(37);
        source.setSales(23);
        source.setIncomeBeforeTaxes(230);
        source.setIncomeAfterTaxes(184);
        source.setOldPrice(9);
        source.setTraded(true);
        assertTrue(d.copyIn(source));
        assertSame(goods, d.getGoodsType());
        assertEquals(12, d.getCostToBuy());
        assertEquals(10, d.getPaidForSale());
        assertEquals(83, d.getAmountInMarket());
        assertEquals(6, d.getInitialPrice());
        assertEquals(37, d.getArrears());
        assertEquals(23, d.getSales());
        assertEquals(230, d.getIncomeBeforeTaxes());
        assertEquals(184, d.getIncomeAfterTaxes());
        assertEquals(9, d.getOldPrice());
        assertTrue(d.getTraded());
    }

    public void testTC025() throws Exception {
        MarketData d = new MarketData(game, goods);
        d.setAmountInMarket(83);
        d.setArrears(37);
        d.setSales(23);
        d.setIncomeBeforeTaxes(230);
        d.setIncomeAfterTaxes(184);
        d.setTraded(true);
        String text = xml(d);
        assertTrue(text.contains("goods-type=\"model.goods.cft\""));
        assertTrue(text.contains("amount=\"83\""));
        assertTrue(text.contains("initialPrice=\"5\""));
        assertTrue(text.contains("arrears=\"37\""));
        assertTrue(text.contains("sales=\"23\""));
        assertTrue(text.contains("incomeBeforeTaxes=\"230\""));
        assertTrue(text.contains("incomeAfterTaxes=\"184\""));
        assertTrue(text.contains("traded=\"true\""));
        assertFalse(text.contains("costToBuy="));
        assertFalse(text.contains("paidForSale="));
        assertFalse(text.contains("oldPrice="));
    }

    public void testTC026() throws Exception {
        MarketData source = pricing(19, 17, 100);
        source.setOldPrice(23);
        source.setArrears(37);
        source.setSales(23);
        source.setIncomeBeforeTaxes(230);
        source.setIncomeAfterTaxes(184);
        source.setTraded(true);
        String text = xml(source);
        Game targetGame = new ServerGame(local);
        MarketData target = new MarketData(targetGame, source.getId());
        read(target, text);
        assertSame(goods, target.getGoodsType());
        assertEquals(7, target.getCostToBuy());
        assertEquals(5, target.getPaidForSale());
        assertEquals(7, target.getOldPrice());
        assertEquals(100, target.getAmountInMarket());
        assertEquals(5, target.getInitialPrice());
        assertEquals(37, target.getArrears());
        assertEquals(23, target.getSales());
        assertEquals(230, target.getIncomeBeforeTaxes());
        assertEquals(184, target.getIncomeAfterTaxes());
        assertTrue(target.getTraded());
    }

    public void testTC027() throws Exception {
        MarketData d = readData("");
        assertEquals(0, d.getAmountInMarket());
        assertEquals(-1, d.getInitialPrice());
        assertEquals(0, d.getArrears());
        assertEquals(0, d.getSales());
        assertEquals(0, d.getIncomeBeforeTaxes());
        assertEquals(0, d.getIncomeAfterTaxes());
        assertFalse(d.getTraded());
        assertEquals(3, d.getCostToBuy());
        assertEquals(1, d.getPaidForSale());
        assertEquals(3, d.getOldPrice());
    }

    public void testTC028() throws Exception {
        MarketData d = readData("amount=\"100\" initialPrice=\"5\" sales=\"37\"");
        assertTrue(d.getTraded());
        assertEquals(37, d.getSales());
        assertEquals(7, d.getCostToBuy());
        assertEquals(5, d.getPaidForSale());
    }

    public void testTC029() throws Exception {
        MarketData d = readData("amount=\"100\" initialPrice=\"5\" sales=\"37\" traded=\"false\"");
        assertFalse(d.getTraded());
        assertEquals(37, d.getSales());
    }

    public void testTC030() throws Exception {
        MarketData d = readData("amount=\"100\" initialPrice=\"5\" sales=\"0\" traded=\"true\"");
        assertTrue(d.getTraded());
        assertEquals(0, d.getSales());
    }

    public void testTC031() throws Exception {
        field(goods, GoodsType.class, "storable", false);
        MarketData d = readData("amount=\"100\" initialPrice=\"5\"");
        assertEquals(-1, d.getCostToBuy());
        assertEquals(0, d.getPaidForSale());
        assertEquals(-1, d.getOldPrice());
    }

    public void testTC032() throws Exception {
        MarketData d = new MarketData(game, "marketData:missingType");
        try {
            read(d, "<marketData id=\"marketData:missingType\"/>");
            fail("Expected NullPointerException");
        } catch (NullPointerException e) {
            assertSame(null, d.getGoodsType());
        }
    }

    public void testTC033() throws Exception {
        MarketData d = new MarketData(game, "marketData:nullType");
        assertEquals("[marketData:nullType null costToBuy=0 paidForSale=0 amountInMarket=0 initialPrice=0 arrears=0 sales=0 incomeBeforeTaxes=0 incomeAfterTaxes=0 oldPrice=0 traded=false]", d.toString());
    }

    public void testTC034() throws Exception {
        MarketData d = pricing(7, 5, 100);
        assertEquals("[" + d.getId() + " model.goods.cft costToBuy=7 paidForSale=5 amountInMarket=100 initialPrice=5 arrears=0 sales=0 incomeBeforeTaxes=0 incomeAfterTaxes=0 oldPrice=7 traded=false]", d.toString());
    }
}
