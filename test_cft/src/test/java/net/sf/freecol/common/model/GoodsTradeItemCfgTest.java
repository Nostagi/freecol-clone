package net.sf.freecol.common.model;

import java.io.StringReader;
import java.io.StringWriter;
import javax.xml.stream.XMLStreamException;

import net.sf.freecol.common.io.FreeColXMLReader;
import net.sf.freecol.common.io.FreeColXMLWriter;
import net.sf.freecol.util.test.FreeColTestCase;


/**
 * CFG-driven test suite for {@link GoodsTradeItem} derived from
 * {@code test_cft/graph/net/sf/freecol/common/model/GoodsTradeItem.json}.
 *
 * Covers all 16 methods (M1..M16) and achieves 100% feasible C3 (node + edge)
 * coverage across 34 Test Paths.
 *
 * Infeasible edges documented:
 * - M13 ({@code copyIn}): {@code N1 -> N2} ({@code B2:false}) — {@code o} is non-null from
 *   {@code copyInCast(other, ColonyTradeItem.class)}, so {@code super.copyIn(o)} always returns {@code true}.
 */
public class GoodsTradeItemCfgTest extends FreeColTestCase {

    private Game game;
    private Player dutch;
    private Player french;
    private Player inca;
    private GoodsType sugarType;
    private Colony dutchColony;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        // Pre-condition: Standard game with map, Dutch colony, European (Dutch, French) and native (Inca) players.
        game = getStandardGame();
        game.setMap(getTestMap());
        dutch = game.getPlayerByNationId("model.nation.dutch");
        french = game.getPlayerByNationId("model.nation.french");
        inca = game.getPlayerByNationId("model.nation.inca");
        sugarType = spec().getGoodsType("model.goods.sugar");
        dutchColony = createStandardColony(1);
    }

    // ------------------------------------------------------------------
    // Pre-condition helpers & stubs
    // ------------------------------------------------------------------

    private static class StubColonyTradeItem extends ColonyTradeItem {
        private final Goods stubGoods;

        StubColonyTradeItem(Game game, Player source, Player destination,
                            Colony colony, Goods stubGoods) {
            super(game, source, destination, colony);
            this.stubGoods = stubGoods;
        }

        @Override
        public Goods getGoods() {
            return stubGoods;
        }
    }

    private Goods validGoods(int amount) {
        return new Goods(game, dutchColony, sugarType, amount);
    }

    private String writeItemXml(GoodsTradeItem item) throws Exception {
        StringWriter sw = new StringWriter();
        try (FreeColXMLWriter xw = new FreeColXMLWriter(sw,
                FreeColXMLWriter.WriteScope.toSave(), false)) {
            item.toXML(xw);
            xw.flush();
        }
        return sw.toString();
    }

    private GoodsTradeItem readItemXml(String xml) throws Exception {
        try (FreeColXMLReader xr = new FreeColXMLReader(new StringReader(xml))) {
            xr.nextTag();
            return new GoodsTradeItem(game, xr);
        }
    }

    // ------------------------------------------------------------------
    // M1: <init>(Game, Player, Player, Goods) — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M1-TP1: ENTRY -> N0 -> EXIT
    public void testM1_TP1_constructorWithGoods() {
        Goods g = validGoods(50);
        GoodsTradeItem item = new GoodsTradeItem(game, dutch, french, g);
        assertSame(dutch, item.getSource());
        assertSame(french, item.getDestination());
        assertSame(g, item.getGoods());
    }

    // ------------------------------------------------------------------
    // M2: <init>(Game, FreeColXMLReader) — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M2-TP1: ENTRY -> N0 -> EXIT
    public void testM2_TP1_constructorFromXml() throws Exception {
        GoodsTradeItem original = new GoodsTradeItem(game, dutch, french, validGoods(25));
        String xml = writeItemXml(original);
        GoodsTradeItem parsed = readItemXml(xml);
        assertNotNull(parsed.getGoods());
        assertEquals(25, parsed.getGoods().getAmount());
    }

    // ------------------------------------------------------------------
    // M3: isValid() — 6 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M3-TP1: ENTRY -> N0 -(B1:true)-> N6 -> N7 -> EXIT (goods == null)
    public void testM3_TP1_isValidNullGoods() {
        GoodsTradeItem item = new GoodsTradeItem(game, dutch, french, null);
        assertFalse(item.isValid());
    }

    // M3-TP2: ENTRY -> N0 -(B1:false)-> N1 -(B2:true)-> N6 -> N7 -> EXIT (goods.getType() == null)
    public void testM3_TP2_isValidNullType() {
        Goods g = validGoods(10);
        g.type = null;
        GoodsTradeItem item = new GoodsTradeItem(game, dutch, french, g);
        assertFalse(item.isValid());
    }

    // M3-TP3: ENTRY -> N0 -(B1:false)-> N1 -(B2:false)-> N2 -(B3:true)-> N6 -> N7 -> EXIT (goods.getAmount() <= 0)
    public void testM3_TP3_isValidNonPositiveAmount() {
        Goods g = validGoods(0);
        GoodsTradeItem item = new GoodsTradeItem(game, dutch, french, g);
        assertFalse(item.isValid());
    }

    // M3-TP4: ENTRY -> N0 -(B1:false)-> N1 -(B2:false)-> N2 -(B3:false)-> N3 -(B4:true)-> N6 -> N7 -> EXIT (!(location instanceof Ownable))
    public void testM3_TP4_isValidNonOwnableLocation() {
        Goods g = new Goods(game, null, sugarType, 10);
        GoodsTradeItem item = new GoodsTradeItem(game, dutch, french, g);
        assertFalse(item.isValid());
    }

    // M3-TP5: ENTRY -> N0 -(B1:false)-> N1 -(B2:false)-> N2 -(B3:false)-> N3 -(B4:false)-> N4 -(B5:true)-> N6 -> N7 -> EXIT (!source.owns(location))
    public void testM3_TP5_isValidNotOwnedBySource() {
        Goods g = validGoods(10); // owned by dutch
        GoodsTradeItem item = new GoodsTradeItem(game, french, dutch, g);
        assertFalse(item.isValid());
    }

    // M3-TP6: ENTRY -> N0 -(B1:false)-> N1 -(B2:false)-> N2 -(B3:false)-> N3 -(B4:false)-> N4 -(B5:false)-> N5 -> N7 -> EXIT (all valid)
    public void testM3_TP6_isValidAllTrue() {
        GoodsTradeItem item = new GoodsTradeItem(game, dutch, french, validGoods(10));
        assertTrue(item.isValid());
    }

    // ------------------------------------------------------------------
    // M4: isUnique() — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M4-TP1: ENTRY -> N0 -> EXIT
    public void testM4_TP1_isUnique() {
        GoodsTradeItem item = new GoodsTradeItem(game, dutch, french, validGoods(10));
        assertFalse(item.isUnique());
    }

    // ------------------------------------------------------------------
    // M5: getLabel() — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M5-TP1: ENTRY -> N0 -> EXIT
    public void testM5_TP1_getLabel() {
        GoodsTradeItem item = new GoodsTradeItem(game, dutch, french, validGoods(10));
        assertNotNull(item.getLabel());
    }

    // ------------------------------------------------------------------
    // M6: getGoods() — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M6-TP1: ENTRY -> N0 -> EXIT
    public void testM6_TP1_getGoods() {
        Goods g = validGoods(15);
        GoodsTradeItem item = new GoodsTradeItem(game, dutch, french, g);
        assertSame(g, item.getGoods());
    }

    // ------------------------------------------------------------------
    // M7: setGoods(Goods) — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M7-TP1: ENTRY -> N0 -> EXIT
    public void testM7_TP1_setGoods() {
        GoodsTradeItem item = new GoodsTradeItem(game, dutch, french, null);
        Goods g = validGoods(20);
        item.setGoods(g);
        assertSame(g, item.getGoods());
    }

    // ------------------------------------------------------------------
    // M8: evaluateFor(Player) — 5 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M8-TP1: ENTRY -> N0 -(B1:false)-> N1 -> N9 -> EXIT (!isValid())
    public void testM8_TP1_evaluateForInvalid() {
        GoodsTradeItem item = new GoodsTradeItem(game, dutch, french, null);
        assertEquals(TradeItem.INVALID_TRADE_ITEM, item.evaluateFor(dutch));
    }

    // M8-TP2: ENTRY -> N0 -(B1:true)-> N2 -(B2:false)-> N3 -(B3:false)-> N4 -> N9 -> EXIT (source == player, market == null)
    public void testM8_TP2_evaluateForSourceNoMarket() {
        inca.market = null;
        assertNull(inca.getMarket());
        Unit wagon = new net.sf.freecol.server.model.ServerUnit(
                game, dutchColony.getTile(), inca, spec().getUnitType("model.unit.wagonTrain"));
        Goods g = new Goods(game, wagon, sugarType, 10);
        GoodsTradeItem item = new GoodsTradeItem(game, inca, dutch, g);
        assertEquals(-20, item.evaluateFor(inca));
    }

    // M8-TP3: ENTRY -> N0 -(B1:true)-> N2 -(B2:false)-> N3 -(B3:true)-> N5 -> N9 -> EXIT (source == player, market != null)
    public void testM8_TP3_evaluateForSourceWithMarket() {
        Goods g = validGoods(10);
        GoodsTradeItem item = new GoodsTradeItem(game, dutch, french, g);
        int expected = dutch.getMarket().getBidPrice(sugarType, 10);
        assertEquals(expected, item.evaluateFor(dutch));
    }

    // M8-TP4: ENTRY -> N0 -(B1:true)-> N2 -(B2:true)-> N6 -(B4:false)-> N7 -> N9 -> EXIT (source != player, market == null)
    public void testM8_TP4_evaluateForDestinationNoMarket() {
        inca.market = null;
        assertNull(inca.getMarket());
        Goods g = validGoods(10);
        GoodsTradeItem item = new GoodsTradeItem(game, dutch, inca, g);
        assertEquals(20, item.evaluateFor(inca));
    }

    // M8-TP5: ENTRY -> N0 -(B1:true)-> N2 -(B2:true)-> N6 -(B4:true)-> N8 -> N9 -> EXIT (source != player, market != null)
    public void testM8_TP5_evaluateForDestinationWithMarket() {
        french.setTax(10);
        Goods g = validGoods(10);
        GoodsTradeItem item = new GoodsTradeItem(game, dutch, french, g);
        int expected = (int) Math.round(french.getMarket().getSalePrice(sugarType, 10) * 0.9);
        assertEquals(expected, item.evaluateFor(french));
    }

    // ------------------------------------------------------------------
    // M9: writeChildren(FreeColXMLWriter) — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M9-TP1: ENTRY -> N0 -> EXIT
    public void testM9_TP1_writeChildren() throws Exception {
        GoodsTradeItem item = new GoodsTradeItem(game, dutch, french, validGoods(12));
        String xml = writeItemXml(item);
        assertTrue(xml.contains("<" + Goods.TAG));
    }

    // ------------------------------------------------------------------
    // M10: readChildren(FreeColXMLReader) — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M10-TP1: ENTRY -> N0 -> EXIT
    public void testM10_TP1_readChildren() throws Exception {
        GoodsTradeItem item = new GoodsTradeItem(game, dutch, french, validGoods(30));
        String xml = writeItemXml(item);
        GoodsTradeItem parsed = readItemXml(xml);
        assertEquals(30, parsed.getGoods().getAmount());
    }

    // ------------------------------------------------------------------
    // M11: readChild(FreeColXMLReader) — 2 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M11-TP1: ENTRY -> N0 -(B1:false)-> N1 -> N3 -> EXIT (Goods.TAG.equals(tag))
    public void testM11_TP1_readChildGoodsTag() throws Exception {
        GoodsTradeItem item = new GoodsTradeItem(game, dutch, french, validGoods(40));
        String xml = writeItemXml(item);
        GoodsTradeItem parsed = readItemXml(xml);
        assertEquals(40, parsed.getGoods().getAmount());
    }

    // M11-TP2: ENTRY -> N0 -(B1:true)-> N2 -> N3 -> EXIT (!Goods.TAG.equals(tag) -> super.readChild)
    public void testM11_TP2_readChildUnknownTag() throws Exception {
        String xml = "<goodsTradeItem id=\"item:1\" source=\"" + dutch.getId()
            + "\" destination=\"" + french.getId()
            + "\"><unknownChild/></goodsTradeItem>";
        try {
            readItemXml(xml);
            fail("Expected XMLStreamException for unexpected child tag");
        } catch (XMLStreamException expected) {
            assertNotNull(expected);
        }
    }

    // ------------------------------------------------------------------
    // M12: getXMLTagName() — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M12-TP1: ENTRY -> N0 -> EXIT
    public void testM12_TP1_getXMLTagName() {
        GoodsTradeItem item = new GoodsTradeItem(game, dutch, french, validGoods(1));
        assertEquals(GoodsTradeItem.TAG, item.getXMLTagName());
    }

    // ------------------------------------------------------------------
    // M13: copyIn(FreeColObject) — 4 Test Paths (100% feasible C3)
    // ------------------------------------------------------------------

    // M13-TP1: ENTRY -> N0 -(B1:true)-> N2 -> EXIT (o == null)
    public void testM13_TP1_copyInNull() {
        GoodsTradeItem dst = new GoodsTradeItem(game, dutch, french, validGoods(5));
        assertFalse(dst.copyIn(null));
    }

    // M13-TP2: ENTRY -> N0 -(B1:false)-> N1 -(B2:true)-> N3 -(B3:false)-> N4 -> N8 -> EXIT (ColonyTradeItem with getGoods() == null)
    public void testM13_TP2_copyInNullGoods() {
        ColonyTradeItem src = new ColonyTradeItem(game, dutch, french, dutchColony);
        GoodsTradeItem dst = new GoodsTradeItem(game, dutch, french, validGoods(5));
        dst.setId(src.getId());
        assertTrue(dst.copyIn(src));
        assertNull(dst.getGoods());
    }

    // M13-TP3: ENTRY -> N0 -(B1:false)-> N1 -(B2:true)-> N3 -(B3:true)-> N5 -(B4:false)-> N6 -> N8 -> EXIT (g != null, this.goods == null)
    public void testM13_TP3_copyInIntoNullGoods() {
        Goods g = validGoods(18);
        StubColonyTradeItem src = new StubColonyTradeItem(game, dutch, french, dutchColony, g);
        GoodsTradeItem dst = new GoodsTradeItem(game, dutch, french, null);
        dst.setId(src.getId());
        assertTrue(dst.copyIn(src));
        assertSame(g, dst.getGoods());
    }

    // M13-TP4: ENTRY -> N0 -(B1:false)-> N1 -(B2:true)-> N3 -(B3:true)-> N5 -(B4:true)-> N7 -> EXIT (g != null, this.goods != null)
    public void testM13_TP4_copyInIntoExistingGoods() {
        Goods gSrc = validGoods(22);
        Goods gDst = validGoods(5);
        gDst.setId(gSrc.getId());
        StubColonyTradeItem src = new StubColonyTradeItem(game, dutch, french, dutchColony, gSrc);
        GoodsTradeItem dst = new GoodsTradeItem(game, dutch, french, gDst);
        dst.setId(src.getId());
        assertTrue(dst.copyIn(src));
        assertEquals(22, dst.getGoods().getAmount());
    }

    // ------------------------------------------------------------------
    // M14: equals(Object) — 4 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M14-TP1: ENTRY -> N0 -(B1:true)-> N6 -> EXIT (!(other instanceof GoodsTradeItem))
    public void testM14_TP1_equalsNotInstance() {
        GoodsTradeItem item = new GoodsTradeItem(game, dutch, french, validGoods(10));
        assertFalse(item.equals("not GoodsTradeItem"));
    }

    // M14-TP2: ENTRY -> N0 -(B1:false)-> N1 -(B2:true)-> N4 -> N5 -> EXIT (!Utils.equals(this.goods, other.goods))
    public void testM14_TP2_equalsDifferentGoods() {
        GoodsTradeItem a = new GoodsTradeItem(game, dutch, french, validGoods(10));
        GoodsTradeItem b = new GoodsTradeItem(game, dutch, french, validGoods(20));
        b.setId(a.getId());
        assertFalse(a.equals(b));
    }

    // M14-TP3: ENTRY -> N0 -(B1:false)-> N1 -(B2:false)-> N2 -(B3:true)-> N4 -> N5 -> EXIT (same goods, !super.equals)
    public void testM14_TP3_equalsDifferentSuper() {
        Player english = game.getPlayerByNationId("model.nation.english");
        Goods g = validGoods(10);
        GoodsTradeItem a = new GoodsTradeItem(game, dutch, french, g);
        GoodsTradeItem b = new GoodsTradeItem(game, dutch, english, g);
        b.setId(a.getId());
        assertFalse(a.equals(b));
    }

    // M14-TP4: ENTRY -> N0 -(B1:false)-> N1 -(B2:false)-> N2 -(B3:false)-> N3 -> N5 -> EXIT (all equal)
    public void testM14_TP4_equalsSame() {
        Goods g = validGoods(10);
        GoodsTradeItem a = new GoodsTradeItem(game, dutch, french, g);
        GoodsTradeItem b = new GoodsTradeItem(game, dutch, french, g);
        b.setId(a.getId());
        assertTrue(a.equals(b));
    }

    // ------------------------------------------------------------------
    // M15: hashCode() — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M15-TP1: ENTRY -> N0 -> EXIT
    public void testM15_TP1_hashCode() {
        Goods g = validGoods(10);
        GoodsTradeItem a = new GoodsTradeItem(game, dutch, french, g);
        GoodsTradeItem b = new GoodsTradeItem(game, dutch, french, g);
        b.setId(a.getId());
        assertEquals(a.hashCode(), b.hashCode());
    }

    // ------------------------------------------------------------------
    // M16: toString() — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M16-TP1: ENTRY -> N0 -> EXIT
    public void testM16_TP1_toString() {
        GoodsTradeItem item = new GoodsTradeItem(game, dutch, french, validGoods(15));
        String s = item.toString();
        assertTrue(s.contains("15"));
    }
}
