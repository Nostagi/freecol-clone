package net.sf.freecol.common.model;

import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.xml.stream.XMLStreamException;
import net.sf.freecol.common.io.FreeColXMLReader;
import net.sf.freecol.common.io.FreeColXMLWriter;
import net.sf.freecol.server.model.ServerUnit;
import net.sf.freecol.util.test.FreeColTestCase;
import net.sf.freecol.common.model.DiplomaticTrade.TradeContext;
import net.sf.freecol.common.model.DiplomaticTrade.TradeStatus;

public class DiplomaticTradeCfgTest extends FreeColTestCase {
    private Game game;
    private Player p;
    private Player q;
    private Player r;
    private DiplomaticTrade dt;

    protected void setUp() throws Exception {
        super.setUp();
        game = getStandardGame();
        p = game.getPlayerByNationId("model.nation.dutch");
        q = game.getPlayerByNationId("model.nation.french");
        r = game.getPlayerByNationId("model.nation.english");
        dt = new DiplomaticTrade(game, TradeContext.TRADE, p, q, null, 3);
    }

    private GoldTradeItem gold(Player source, Player destination, int amount) {
        return new GoldTradeItem(game, source, destination, amount);
    }

    private Goods goods(String type, int amount) {
        return new Goods(game, null, spec().getGoodsType("model.goods." + type), amount);
    }

    private Unit unit(Player owner) {
        return new ServerUnit(game, null, owner, spec().getUnitType("model.unit.freeColonist"));
    }

    private Colony colony(Player owner, String name) {
        Colony result = new Colony(game, "colony:cft" + name);
        result.setName(name);
        result.setOwner(owner);
        return result;
    }

    private String xml(FreeColObject value) throws Exception {
        StringWriter out = new StringWriter();
        try (FreeColXMLWriter writer = new FreeColXMLWriter(out,
                FreeColXMLWriter.WriteScope.toSave(), false)) {
            value.toXML(writer);
            writer.flush();
        }
        return out.toString();
    }

    private DiplomaticTrade read(String text) throws Exception {
        DiplomaticTrade result = new DiplomaticTrade(game, "ignored");
        try (FreeColXMLReader reader = new FreeColXMLReader(new StringReader(text))) {
            reader.nextTag();
            result.readFromXML(reader);
        }
        return result;
    }

    public void testTC001() throws Exception {
        DiplomaticTrade value = new DiplomaticTrade(game, "ignored");
        assertEquals("", value.getId());
        assertSame(null, value.getContext());
        assertSame(null, value.getStatus());
        assertSame(null, value.getSender());
        assertSame(null, value.getRecipient());
        assertEquals(0, value.getVersion());
        assertTrue(value.isEmpty());
        assertFalse(value.isInternable());
        assertEquals("diplomaticTrade", value.getXMLTagName());
    }

    public void testTC002() throws Exception {
        DiplomaticTrade value = new DiplomaticTrade(game, TradeContext.TRADE, p, q, null, 3);
        assertEquals("TRADE", value.getContext().name());
        assertEquals("PROPOSE_TRADE", value.getStatus().name());
        assertSame(p, value.getSender());
        assertSame(q, value.getRecipient());
        assertEquals(3, value.getVersion());
        assertTrue(value.isEmpty());
    }

    public void testTC003() throws Exception {
        List<TradeItem> input = new ArrayList<TradeItem>();
        TradeItem item = gold(p, q, 75);
        input.add(item);
        DiplomaticTrade value = new DiplomaticTrade(game, TradeContext.DIPLOMATIC, p, q, input, 4);
        input.clear();
        assertEquals(1, value.getItems().size());
        assertSame(item, value.getItems().get(0));
        assertEquals(4, value.getVersion());
    }

    public void testTC004() throws Exception {
        assertEquals("contact", TradeContext.CONTACT.getKey());
    }

    public void testTC005() throws Exception {
        assertEquals("diplomatic", TradeContext.DIPLOMATIC.getKey());
    }

    public void testTC006() throws Exception {
        assertEquals("trade", TradeContext.TRADE.getKey());
    }

    public void testTC007() throws Exception {
        assertEquals("tribute", TradeContext.TRIBUTE.getKey());
    }

    public void testTC008() throws Exception {
        dt.setStatus(TradeStatus.ACCEPT_TRADE);
        assertEquals("ACCEPT_TRADE", dt.getStatus().name());
        dt.setStatus(TradeStatus.REJECT_TRADE);
        assertEquals("REJECT_TRADE", dt.getStatus().name());
        dt.setSender(q);
        dt.setRecipient(p);
        assertSame(q, dt.getSender());
        assertSame(p, dt.getRecipient());
        dt.incrementVersion();
        assertEquals(4, dt.getVersion());
    }

    public void testTC009() throws Exception {
        assertSame(q, dt.getOtherPlayer(p));
        assertSame(p, dt.getOtherPlayer(q));
        assertSame(p, dt.getOtherPlayer(r));
        assertSame(p, dt.getOtherPlayer(null));
    }

    public void testTC010() throws Exception {
        DiplomaticTrade peace = DiplomaticTrade.makePeaceTreaty(TradeContext.CONTACT, p, q);
        assertEquals("CONTACT", peace.getContext().name());
        assertEquals("PROPOSE_TRADE", peace.getStatus().name());
        assertEquals(0, peace.getVersion());
        assertEquals(1, peace.getItems().size());
        assertEquals("PEACE", peace.getStance().name());
        assertSame(q, peace.getItems().get(0).getSource());
        assertSame(p, peace.getItems().get(0).getDestination());
    }

    public void testTC011() throws Exception {
        Colony destination = colony(q, "CFT colony");
        assertEquals("model.diplomaticTrade.send.trade", dt.getSendMessage(p, destination).getId());
    }

    public void testTC012() throws Exception {
        assertEquals("model.diplomaticTrade.receive.trade", dt.getReceiveMessage(p).getId());
    }

    public void testTC013() throws Exception {
        dt.add(gold(p, q, 75));
        dt.add(gold(q, p, 120));
        assertEquals(1, dt.getItems().size());
        assertEquals(-1, dt.getGoldGivenBy(p));
        assertEquals(120, dt.getGoldGivenBy(q));
    }

    public void testTC014() throws Exception {
        dt.add(new GoodsTradeItem(game, p, q, goods("food", 25)));
        dt.add(new GoodsTradeItem(game, p, q, goods("cotton", 35)));
        assertEquals(2, dt.getItems().size());
        assertEquals(2, dt.getGoodsGivenBy(p).size());
    }

    public void testTC015() throws Exception {
        TradeItem item = gold(p, q, 75);
        dt.add(item);
        dt.remove(item);
        assertTrue(dt.isEmpty());
        dt.remove(item);
        assertTrue(dt.isEmpty());
    }

    public void testTC016() throws Exception {
        TradeItem a = new GoodsTradeItem(game, p, q, goods("food", 25));
        TradeItem b = new GoodsTradeItem(game, q, p, goods("cotton", 35));
        dt.add(a);
        dt.add(b);
        dt.remove(0);
        assertEquals(1, dt.getItems().size());
        assertSame(b, dt.getItems().get(0));
    }

    public void testTC017() throws Exception {
        dt.add(new GoodsTradeItem(game, p, q, goods("food", 25)));
        dt.add(new GoodsTradeItem(game, p, q, goods("cotton", 35)));
        dt.add(gold(p, q, 75));
        dt.removeType(GoodsTradeItem.class);
        assertEquals(1, dt.getItems().size());
        assertEquals(75, dt.getGoldGivenBy(p));
    }

    public void testTC018() throws Exception {
        dt.add(gold(p, q, 75));
        assertFalse(dt.isEmpty());
        dt.clear();
        assertTrue(dt.isEmpty());
    }

    public void testTC019() throws Exception {
        dt.add(gold(p, q, 75));
        dt.getItems().clear();
        assertTrue(dt.isEmpty());
    }

    public void testTC020() throws Exception {
        dt.add(gold(p, q, 75));
        dt.add(new GoodsTradeItem(game, q, p, goods("food", 25)));
        dt.add(new UnitTradeItem(game, p, q, unit(p)));
        assertEquals(2, dt.getItemsGivenBy(p).size());
        assertEquals(1, dt.getItemsGivenBy(q).size());
        assertEquals(0, dt.getItemsGivenBy(r).size());
    }

    public void testTC021() throws Exception {
        assertSame(null, dt.getStance());
        assertEquals(-1, dt.getGoldGivenBy(p));
        assertSame(null, dt.getVictim());
        assertTrue(dt.getColoniesGivenBy(p).isEmpty());
        assertTrue(dt.getGoodsGivenBy(p).isEmpty());
        assertTrue(dt.getUnitsGivenBy(p).isEmpty());
    }

    public void testTC022() throws Exception {
        dt.add(gold(p, q, 75));
        dt.add(new StanceTradeItem(game, p, q, Stance.PEACE));
        assertEquals("PEACE", dt.getStance().name());
    }

    public void testTC023() throws Exception {
        dt.add(new GoodsTradeItem(game, p, q, goods("food", 25)));
        dt.add(gold(q, p, 75));
        assertEquals(-1, dt.getGoldGivenBy(p));
        assertEquals(75, dt.getGoldGivenBy(q));
    }

    public void testTC024() throws Exception {
        dt.add(gold(p, q, 75));
        dt.add(new InciteTradeItem(game, p, q, r));
        assertSame(r, dt.getVictim());
    }

    public void testTC025() throws Exception {
        Colony a = colony(p, "A");
        Colony b = colony(q, "B");
        dt.add(gold(p, q, 75));
        dt.add(new ColonyTradeItem(game, p, q, a));
        dt.add(new ColonyTradeItem(game, q, p, b));
        assertEquals(1, dt.getColoniesGivenBy(p).size());
        assertSame(a, dt.getColoniesGivenBy(p).get(0));
        assertEquals(1, dt.getColoniesGivenBy(q).size());
        assertSame(b, dt.getColoniesGivenBy(q).get(0));
    }

    public void testTC026() throws Exception {
        Goods a = goods("food", 25);
        Goods b = goods("cotton", 35);
        dt.add(gold(p, q, 75));
        dt.add(new GoodsTradeItem(game, p, q, a));
        dt.add(new GoodsTradeItem(game, q, p, b));
        assertEquals(1, dt.getGoodsGivenBy(p).size());
        assertSame(a, dt.getGoodsGivenBy(p).get(0));
        assertEquals(1, dt.getGoodsGivenBy(q).size());
        assertSame(b, dt.getGoodsGivenBy(q).get(0));
    }

    public void testTC027() throws Exception {
        Unit a = unit(p);
        Unit b = unit(q);
        dt.add(gold(p, q, 75));
        dt.add(new UnitTradeItem(game, p, q, a));
        dt.add(new UnitTradeItem(game, q, p, b));
        assertEquals(1, dt.getUnitsGivenBy(p).size());
        assertSame(a, dt.getUnitsGivenBy(p).get(0));
        assertEquals(1, dt.getUnitsGivenBy(q).size());
        assertSame(b, dt.getUnitsGivenBy(q).get(0));
    }

    public void testTC028() throws Exception {
        assertFalse(dt.copyIn((FreeColObject)null));
        Goods wrong = goods("food", 25);
        wrong.setId(dt.getId());
        assertFalse(dt.copyIn(wrong));
    }

    public void testTC029() throws Exception {
        DiplomaticTrade source = new DiplomaticTrade(game, TradeContext.TRIBUTE, q, p, null, 7);
        source.setStatus(TradeStatus.ACCEPT_TRADE);
        dt.add(gold(p, q, 75));
        assertTrue(dt.copyIn(source));
        assertEquals("TRIBUTE", dt.getContext().name());
        assertEquals("ACCEPT_TRADE", dt.getStatus().name());
        assertSame(q, dt.getSender());
        assertSame(p, dt.getRecipient());
        assertEquals(7, dt.getVersion());
        assertTrue(dt.isEmpty());
    }

    public void testTC030() throws Exception {
        DiplomaticTrade source = new DiplomaticTrade(game, TradeContext.TRADE, p, q, Arrays.<TradeItem>asList(gold(p, q, 75)), 8);
        assertTrue(dt.copyIn(source));
        assertEquals(8, dt.getVersion());
        assertEquals(0, dt.getItems().size());
    }

    public void testTC031() throws Exception {
        String text = xml(dt);
        DiplomaticTrade value = read(text);
        assertEquals("TRADE", value.getContext().name());
        assertEquals("PROPOSE_TRADE", value.getStatus().name());
        assertSame(p, value.getSender());
        assertSame(q, value.getRecipient());
        assertEquals(3, value.getVersion());
        assertTrue(value.isEmpty());
    }

    public void testTC032() throws Exception {
        DiplomaticTrade value = read("<diplomaticTrade/>");
        assertSame(null, value.getContext());
        assertSame(null, value.getSender());
        assertSame(null, value.getRecipient());
        assertEquals("REJECT_TRADE", value.getStatus().name());
        assertEquals(0, value.getVersion());
        assertTrue(value.isEmpty());
    }

    public void testTC033() throws Exception {
        Colony a = colony(p, "A");
        dt.add(new ColonyTradeItem(game, p, q, a));
        DiplomaticTrade value = read(xml(dt));
        assertEquals(1, value.getItems().size());
        assertEquals("ColonyTradeItem", value.getItems().get(0).getClass().getSimpleName());
        assertSame(a, value.getColoniesGivenBy(p).get(0));
    }

    public void testTC034() throws Exception {
        dt.add(gold(p, q, 75));
        DiplomaticTrade value = read(xml(dt));
        assertEquals(1, value.getItems().size());
        assertEquals("GoldTradeItem", value.getItems().get(0).getClass().getSimpleName());
        assertEquals(75, value.getGoldGivenBy(p));
    }

    public void testTC035() throws Exception {
        Goods a = goods("food", 25);
        dt.add(new GoodsTradeItem(game, p, q, a));
        DiplomaticTrade value = read(xml(dt));
        assertEquals(1, value.getItems().size());
        assertEquals("GoodsTradeItem", value.getItems().get(0).getClass().getSimpleName());
        assertEquals(25, value.getGoodsGivenBy(p).get(0).getAmount());
    }

    public void testTC036() throws Exception {
        dt.add(new InciteTradeItem(game, p, q, r));
        DiplomaticTrade value = read(xml(dt));
        assertEquals(1, value.getItems().size());
        assertEquals("InciteTradeItem", value.getItems().get(0).getClass().getSimpleName());
        assertSame(r, value.getVictim());
    }

    public void testTC037() throws Exception {
        dt.add(new StanceTradeItem(game, p, q, Stance.PEACE));
        DiplomaticTrade value = read(xml(dt));
        assertEquals(1, value.getItems().size());
        assertEquals("StanceTradeItem", value.getItems().get(0).getClass().getSimpleName());
        assertEquals("PEACE", value.getStance().name());
    }

    public void testTC038() throws Exception {
        Unit a = unit(p);
        dt.add(new UnitTradeItem(game, p, q, a));
        DiplomaticTrade value = read(xml(dt));
        assertEquals(1, value.getItems().size());
        assertEquals("UnitTradeItem", value.getItems().get(0).getClass().getSimpleName());
        assertSame(a, value.getUnitsGivenBy(p).get(0));
    }

    public void testTC039() throws Exception {
        try (FreeColXMLReader reader = new FreeColXMLReader(new StringReader("<unexpected/>"))) {
            reader.nextTag();
            try {
                dt.readChild(reader);
                fail("Expected XMLStreamException");
            } catch (XMLStreamException e) {
                assertNotNull(e.getMessage());
            }
        }
    }

    public void testTC040() throws Exception {
        dt.add(gold(p, q, 75));
        try (FreeColXMLReader reader = new FreeColXMLReader(new StringReader("<diplomaticTrade/>"))) {
            reader.nextTag();
            dt.readChildren(reader);
        }
        assertTrue(dt.isEmpty());
    }

    public void testTC041() throws Exception {
        String text = dt.toString();
        assertTrue(text.contains("TRADE PROPOSE_TRADE"));
        assertTrue(text.contains("from=" + p.getId()));
        assertTrue(text.contains("to=" + q.getId()));
        assertTrue(text.contains("version=3"));
        assertTrue(text.endsWith(" [ ]]"));
    }

    public void testTC042() throws Exception {
        TradeItem item = gold(p, q, 75);
        dt.add(item);
        String text = dt.toString();
        assertTrue(text.contains(item.toString()));
        assertTrue(text.endsWith(" ]]"));
    }
}
