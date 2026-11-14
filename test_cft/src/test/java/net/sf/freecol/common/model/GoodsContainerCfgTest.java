package net.sf.freecol.common.model;

import java.io.StringReader;
import java.io.StringWriter;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.xml.stream.XMLStreamException;
import junit.framework.TestCase;
import net.sf.freecol.common.io.FreeColXMLReader;
import net.sf.freecol.common.io.FreeColXMLWriter;
import net.sf.freecol.common.io.FreeColXMLWriter.WriteScope;
import net.sf.freecol.server.model.ServerGame;

public class GoodsContainerCfgTest extends TestCase {
    private Specification local;
    private Game game;
    private GoodsType g1;
    private GoodsType g2;
    private GoodsType ignored;
    private GoodsType unstorable;
    private GoodsContainer a;
    private GoodsContainer b;
    private CountingColony colony;
    private HighSeas plain;
    private Player p;
    private Player q;

    public static class CountingColony extends Colony {
        int invalidations;
        public CountingColony(Game game, String id) { super(game, id); }
        public void invalidateCache() { invalidations++; }
    }

    protected void setUp() throws Exception {
        super.setUp();
        local = new Specification();
        game = new ServerGame(local);
        g1 = type("model.goods.a", true, false);
        g2 = type("model.goods.b", true, false);
        ignored = type("model.goods.ignored", true, true);
        unstorable = type("model.goods.unstorable", false, false);
        specificationGoods(g1, g2, ignored, unstorable);
        a = new GoodsContainer(game, "goodsContainer:cftA");
        b = new GoodsContainer(game, "goodsContainer:cftB");
        p = new Player(game, "player:cftP");
        q = new Player(game, "player:cftQ");
        colony = new CountingColony(game, "colony:cft");
        colony.setOwner(p);
        colony.setGoodsContainer(a);
        plain = new HighSeas(game);
    }

    private GoodsType type(String id, boolean storable, boolean ignore) throws Exception {
        GoodsType type = local.findType(id, GoodsType.class);
        Field field = GoodsType.class.getDeclaredField("storable");
        field.setAccessible(true);
        field.setBoolean(type, storable);
        field = GoodsType.class.getDeclaredField("ignoreLimit");
        field.setAccessible(true);
        field.setBoolean(type, ignore);
        return type;
    }

    @SuppressWarnings("unchecked")
    private void specificationGoods(GoodsType... values) throws Exception {
        Field field = Specification.class.getDeclaredField("goodsTypeList");
        field.setAccessible(true);
        List<GoodsType> types = (List<GoodsType>)field.get(local);
        types.clear();
        types.addAll(Arrays.asList(values));
    }

    private Map<GoodsType, Integer> storage(GoodsType type, int amount) {
        Map<GoodsType, Integer> result = new HashMap<GoodsType, Integer>();
        result.put(type, amount);
        return result;
    }

    private String xml(GoodsContainer target, WriteScope scope) throws Exception {
        StringWriter out = new StringWriter();
        try (FreeColXMLWriter writer = new FreeColXMLWriter(out, scope, false)) {
            target.toXML(writer);
            writer.flush();
        }
        return out.toString();
    }

    private void read(GoodsContainer target, String text) throws Exception {
        try (FreeColXMLReader reader = new FreeColXMLReader(new StringReader(text))) {
            reader.nextTag();
            target.readFromXML(reader);
        }
    }

    private void child(GoodsContainer target, String text) throws Exception {
        try (FreeColXMLReader reader = new FreeColXMLReader(new StringReader(text))) {
            reader.nextTag();
            target.readChild(reader);
        }
    }

    public void testTC001() throws Exception {
        GoodsContainer created = new GoodsContainer(game, (Location)colony);
        assertSame(colony, created.getParent());
        assertSame(null, a.getParent());
        assertEquals("goodsContainer:cftA", a.getId());
        assertTrue(a.getStoredGoods().isEmpty());
        assertTrue(a.getOldStoredGoods().isEmpty());
        assertEquals("goodsContainer", a.getXMLTagName());
        assertEquals(100, GoodsContainer.CARGO_SIZE);
        assertEquals(10000, GoodsContainer.HUGE_CARGO_SIZE);
    }

    public void testTC002() throws Exception {
        a.setLocation(plain);
        assertSame(plain, a.getParent());
    }

    public void testTC003() throws Exception {
        a.setLocation(plain);
        try {
            a.setLocation(null);
            fail("Expected RuntimeException");
        } catch (RuntimeException e) {
            assertTrue(e.getMessage().contains("Null GoodsContainer location"));
        }
        assertSame(plain, a.getParent());
    }

    public void testTC004() throws Exception {
        Map<GoodsType, Integer> current = storage(g1, 37);
        Map<GoodsType, Integer> previous = storage(g2, 23);
        a.setStoredGoods(current);
        a.setOldStoredGoods(previous);
        current.clear();
        previous.clear();
        assertEquals(37, a.getGoodsCount(g1));
        assertEquals(23, a.getOldGoodsCount(g2));
        assertSame(a.getStoredGoods(), a.getStoredGoods());
        assertSame(a.getOldStoredGoods(), a.getOldStoredGoods());
    }

    public void testTC005() throws Exception {
        assertEquals(0, a.getGoodsCount(g1));
        assertEquals(0, a.getOldGoodsCount(g1));
    }

    public void testTC006() throws Exception {
        a.setAmount(g1, 37);
        assertTrue(a.contains(new AbstractGoods(g1, 36)));
        assertTrue(a.contains(new AbstractGoods(g1, 37)));
        assertFalse(a.contains(new AbstractGoods(g1, 38)));
    }

    public void testTC007() throws Exception {
        assertTrue(a.addGoods(new AbstractGoods(g1, 38)));
        assertEquals(38, a.getGoodsCount(g1));
    }

    public void testTC008() throws Exception {
        a.setAmount(g1, 37);
        assertTrue(a.addGoods(g1, 18));
        assertEquals(55, a.getGoodsCount(g1));
    }

    public void testTC009() throws Exception {
        a.setAmount(g1, 37);
        assertTrue(a.addGoods(g1, -12));
        assertEquals(25, a.getGoodsCount(g1));
    }

    public void testTC010() throws Exception {
        a.setAmount(g1, 37);
        assertTrue(a.addGoods(g1, -37));
        assertEquals(0, a.getGoodsCount(g1));
        assertFalse(a.getStoredGoods().containsKey(g1));
    }

    public void testTC011() throws Exception {
        a.setAmount(g1, 37);
        try {
            a.addGoods(g1, -38);
            fail("Expected IllegalStateException");
        } catch (IllegalStateException e) {
            assertTrue(e.getMessage().contains("Operation would leave -1"));
        }
        assertEquals(37, a.getGoodsCount(g1));
    }

    public void testTC012() throws Exception {
        assertTrue(a.addGoods(g1, 0));
        assertEquals(0, a.getGoodsCount(g1));
        assertFalse(a.getStoredGoods().containsKey(g1));
    }

    public void testTC013() throws Exception {
        assertSame(null, a.removeGoods(g1, 12));
    }

    public void testTC014() throws Exception {
        a.setAmount(g1, -4);
        assertSame(null, a.removeGoods(g1, 12));
        assertEquals(-4, a.getGoodsCount(g1));
    }

    public void testTC015() throws Exception {
        a.setAmount(g1, 37);
        Goods removed = a.removeGoods(g1, 12);
        assertEquals(12, removed.getAmount());
        assertSame(g1, removed.getType());
        assertSame(null, removed.getLocation());
        assertEquals(25, a.getGoodsCount(g1));
    }

    public void testTC016() throws Exception {
        a.setAmount(g1, 37);
        assertEquals(37, a.removeGoods(g1, 37).getAmount());
        assertEquals(0, a.getGoodsCount(g1));
        assertFalse(a.getStoredGoods().containsKey(g1));
    }

    public void testTC017() throws Exception {
        a.setAmount(g1, 37);
        assertEquals(37, a.removeGoods(g1, 80).getAmount());
        assertEquals(0, a.getGoodsCount(g1));
    }

    public void testTC018() throws Exception {
        a.setAmount(g1, 37);
        assertEquals(37, a.removeGoods(g1).getAmount());
        assertEquals(0, a.getGoodsCount(g1));
    }

    public void testTC019() throws Exception {
        a.setAmount(g1, 37);
        assertEquals(12, a.removeGoods(new AbstractGoods(g1, 12)).getAmount());
        assertEquals(25, a.getGoodsCount(g1));
    }

    public void testTC020() throws Exception {
        a.setAmount(g1, 37);
        assertEquals(0, a.removeGoods(g1, 0).getAmount());
        assertEquals(37, a.getGoodsCount(g1));
    }

    public void testTC021() throws Exception {
        a.setAmount(g1, 37);
        assertEquals(-4, a.removeGoods(g1, -4).getAmount());
        assertEquals(41, a.getGoodsCount(g1));
    }

    public void testTC022() throws Exception {
        a.setLocation(plain);
        a.setAmount(g1, 37);
        assertEquals(37, a.getGoodsCount(g1));
        assertEquals(0, colony.invalidations);
    }

    public void testTC023() throws Exception {
        a.setLocation(colony);
        a.setAmount(g1, 37);
        a.addGoods(g1, 5);
        a.removeGoods(g1, 3);
        a.removeAll();
        assertEquals(4, colony.invalidations);
        assertTrue(a.getStoredGoods().isEmpty());
    }

    public void testTC024() throws Exception {
        a.setAmount(g1, 37);
        assertEquals(37, a.getGoodsCount(g1));
        a.setAmount(g1, -4);
        assertEquals(-4, a.getGoodsCount(g1));
        a.setAmount(g1, 0);
        assertEquals(0, a.getGoodsCount(g1));
        assertFalse(a.getStoredGoods().containsKey(g1));
    }

    public void testTC025() throws Exception {
        a.setAmount(g1, 37);
        a.saveState();
        a.setAmount(g2, 23);
        a.removeAll();
        assertTrue(a.getStoredGoods().isEmpty());
        assertEquals(37, a.getOldGoodsCount(g1));
    }

    public void testTC026() throws Exception {
        a.setAmount(g1, 37);
        a.saveState();
        a.setAmount(g1, 55);
        a.setAmount(g2, 23);
        assertEquals(37, a.getOldGoodsCount(g1));
        a.restoreState();
        assertEquals(37, a.getGoodsCount(g1));
        assertEquals(0, a.getGoodsCount(g2));
        a.setAmount(g1, 60);
        assertEquals(37, a.getOldGoodsCount(g1));
    }

    public void testTC027() throws Exception {
        a.setAmount(g1, 37);
        a.restoreState();
        assertTrue(a.getStoredGoods().isEmpty());
    }

    public void testTC028() throws Exception {
        a.setLocation(colony);
        a.setStoredGoods(storage(g1, 37));
        a.setOldStoredGoods(storage(g1, 23));
        a.removeAbove(0);
        assertTrue(a.getStoredGoods().isEmpty());
        assertEquals(23, a.getOldGoodsCount(g1));
        assertEquals(0, colony.invalidations);
        a.setStoredGoods(storage(ignored, 70));
        a.removeAbove(-4);
        assertTrue(a.getStoredGoods().isEmpty());
        assertEquals(0, colony.invalidations);
    }

    public void testTC029() throws Exception {
        a.setLocation(colony);
        Map<GoodsType, Integer> values = storage(g1, 37);
        values.put(g2, 15);
        values.put(ignored, 70);
        values.put(unstorable, 80);
        a.setStoredGoods(values);
        a.removeAbove(20);
        assertEquals(20, a.getGoodsCount(g1));
        assertEquals(15, a.getGoodsCount(g2));
        assertEquals(70, a.getGoodsCount(ignored));
        assertEquals(80, a.getGoodsCount(unstorable));
        assertEquals(1, colony.invalidations);
    }

    public void testTC030() throws Exception {
        a.setLocation(colony);
        a.removeAbove(20);
        assertEquals(1, colony.invalidations);
        a.setStoredGoods(storage(g1, 20));
        a.removeAbove(20);
        assertEquals(20, a.getGoodsCount(g1));
        assertEquals(2, colony.invalidations);
    }

    public void testTC031() throws Exception {
        assertFalse(a.hasReachedCapacity(20));
    }

    public void testTC032() throws Exception {
        a.setAmount(g1, 20);
        assertFalse(a.hasReachedCapacity(20));
    }

    public void testTC033() throws Exception {
        a.setAmount(g1, 21);
        assertTrue(a.hasReachedCapacity(20));
    }

    public void testTC034() throws Exception {
        a.setAmount(ignored, 80);
        a.setAmount(unstorable, 90);
        assertFalse(a.hasReachedCapacity(20));
    }

    public void testTC035() throws Exception {
        assertEquals(0, a.getSpaceTaken());
    }

    public void testTC036() throws Exception {
        a.setAmount(g1, 200);
        a.setAmount(g2, 37);
        assertEquals(3, a.getSpaceTaken());
    }

    public void testTC037() throws Exception {
        assertTrue(a.getGoodsList().isEmpty());
    }

    public void testTC038() throws Exception {
        a.setLocation(colony);
        a.setAmount(g1, 250);
        List<Goods> result = a.getGoodsList();
        assertEquals(3, result.size());
        assertEquals(100, result.get(0).getAmount());
        assertEquals(100, result.get(1).getAmount());
        assertEquals(50, result.get(2).getAmount());
        assertSame(colony, result.get(0).getLocation());
    }

    public void testTC039() throws Exception {
        a.getStoredGoods().put(g1, 0);
        a.setAmount(g2, -4);
        assertTrue(a.getGoodsList().isEmpty());
    }

    public void testTC040() throws Exception {
        a.setAmount(g1, 100);
        a.setAmount(g2, 37);
        List<Goods> result = a.getGoodsList();
        assertEquals(2, result.size());
        assertEquals(137, result.stream().mapToInt(Goods::getAmount).sum());
    }

    public void testTC041() throws Exception {
        assertTrue(a.getCompactGoodsList().isEmpty());
    }

    public void testTC042() throws Exception {
        a.setLocation(colony);
        a.setAmount(g1, 250);
        a.getStoredGoods().put(g2, 0);
        a.setAmount(ignored, -4);
        List<Goods> result = a.getCompactGoodsList();
        assertEquals(1, result.size());
        assertSame(g1, result.get(0).getType());
        assertEquals(250, result.get(0).getAmount());
        assertSame(colony, result.get(0).getLocation());
    }

    public void testTC043() throws Exception {
        a.setAmount(g1, 37);
        a.saveState();
        assertFalse(a.hasChanged());
        a.setAmount(g1, 55);
        assertTrue(a.hasChanged());
    }

    public void testTC044() throws Exception {
        a.setAmount(g1, 37);
        a.saveState();
        List<String> events = new ArrayList<String>();
        a.addPropertyChangeListener(e -> events.add(e.getPropertyName()));
        assertFalse(a.fireChanges());
        assertTrue(events.isEmpty());
    }

    public void testTC045() throws Exception {
        a.setAmount(g1, 37);
        a.saveState();
        a.setAmount(g1, 55);
        List<String> events = new ArrayList<String>();
        a.addPropertyChangeListener(e -> events.add(e.getPropertyName() + ":" + e.getOldValue() + "->" + e.getNewValue()));
        assertTrue(a.fireChanges());
        assertEquals(1, events.size());
        assertEquals("model.goods.a:37->55", events.get(0));
    }

    public void testTC046() throws Exception {
        specificationGoods();
        assertFalse(a.fireChanges());
        assertFalse(a.hasChanged());
    }

    public void testTC047() throws Exception {
        a.setAmount(g1, 37);
        b.setAmount(g1, 10);
        GoodsContainer.moveGoods(a, g1, 12, b);
        assertEquals(25, a.getGoodsCount(g1));
        assertEquals(22, b.getGoodsCount(g1));
        assertEquals(37, a.getOldGoodsCount(g1));
        assertEquals(10, b.getOldGoodsCount(g1));
    }

    public void testTC048() throws Exception {
        b.setAmount(g1, 10);
        GoodsContainer.moveGoods(null, g1, 12, b);
        assertEquals(22, b.getGoodsCount(g1));
        assertEquals(10, b.getOldGoodsCount(g1));
    }

    public void testTC049() throws Exception {
        a.setAmount(g1, 37);
        GoodsContainer.moveGoods(a, g1, 12, null);
        assertEquals(25, a.getGoodsCount(g1));
        assertEquals(37, a.getOldGoodsCount(g1));
    }

    public void testTC050() throws Exception {
        a.setAmount(g1, 37);
        GoodsContainer.moveGoods(null, g1, 12, null);
        assertEquals(37, a.getGoodsCount(g1));
    }

    public void testTC051() throws Exception {
        a.setAmount(g1, 5);
        GoodsContainer.moveGoods(a, g1, 12, b);
        assertEquals(0, a.getGoodsCount(g1));
        assertEquals(12, b.getGoodsCount(g1));
    }

    public void testTC052() throws Exception {
        assertSame(null, a.getOwner());
        a.setLocation(plain);
        assertSame(null, a.getOwner());
        a.setLocation(colony);
        assertSame(p, a.getOwner());
    }

    public void testTC053() throws Exception {
        try {
            a.setOwner(p);
            fail("Expected UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            assertTrue(e.getMessage().contains("Can not set GoodsContainer owner"));
        }
    }

    public void testTC054() throws Exception {
        a.setAmount(g1, 37);
        a.saveState();
        a.disposeResources();
        assertTrue(a.getStoredGoods().isEmpty());
        assertTrue(a.getOldStoredGoods().isEmpty());
        assertTrue(a.isDisposed());
    }

    public void testTC055() throws Exception {
        assertFalse(a.copyIn((FreeColObject)null));
        assertFalse(a.copyIn(StringTemplate.key(a.getId())));
        assertFalse(a.copyIn(b));
    }

    public void testTC056() throws Exception {
        a.setLocation(plain);
        Game sourceGame = new ServerGame(local);
        GoodsContainer source = new GoodsContainer(sourceGame, a.getId());
        source.setLocation(new HighSeas(sourceGame));
        source.setStoredGoods(storage(g1, 37));
        source.setOldStoredGoods(storage(g2, 23));
        assertTrue(a.copyIn(source));
        assertSame(plain, a.getParent());
        assertEquals(37, a.getGoodsCount(g1));
        assertEquals(23, a.getOldGoodsCount(g2));
        assertFalse(a.getStoredGoods() == source.getStoredGoods());
        assertFalse(a.getOldStoredGoods() == source.getOldStoredGoods());
        source.setAmount(g1, 55);
        assertEquals(37, a.getGoodsCount(g1));
    }

    public void testTC057() throws Exception {
        a.setLocation(colony);
        a.setStoredGoods(storage(g1, 37));
        a.getStoredGoods().put(g2, 55);
        a.setOldStoredGoods(storage(g1, 23));
        String text = xml(a, WriteScope.toSave());
        assertTrue(text.contains("storedGoods"));
        assertTrue(text.contains("oldStoredGoods"));
        Game copyGame = new ServerGame(local);
        GoodsContainer copy = new GoodsContainer(copyGame, a.getId());
        HighSeas copyParent = new HighSeas(copyGame);
        copy.setLocation(copyParent);
        read(copy, text);
        assertEquals(37, copy.getGoodsCount(g1));
        assertEquals(55, copy.getGoodsCount(g2));
        assertEquals(23, copy.getOldGoodsCount(g1));
        assertSame(copyParent, copy.getParent());
    }

    public void testTC058() throws Exception {
        String text = xml(a, WriteScope.toSave());
        assertFalse(text.contains("storedGoods"));
        assertFalse(text.contains("oldStoredGoods"));
    }

    public void testTC059() throws Exception {
        a.setLocation(colony);
        a.setAmount(g1, 37);
        a.saveState();
        String text = xml(a, WriteScope.toClient(p));
        assertTrue(text.contains("storedGoods"));
        assertTrue(text.contains("oldStoredGoods"));
    }

    public void testTC060() throws Exception {
        a.setLocation(colony);
        a.setAmount(g1, 37);
        a.saveState();
        String text = xml(a, WriteScope.toClient(q));
        assertFalse(text.contains("storedGoods"));
        assertFalse(text.contains("oldStoredGoods"));
    }

    public void testTC061() throws Exception {
        a.setAmount(g1, 37);
        a.saveState();
        read(a, "<goodsContainer id=\"goodsContainer:cftA\"/>");
        assertTrue(a.getStoredGoods().isEmpty());
        assertTrue(a.getOldStoredGoods().isEmpty());
    }

    public void testTC062() throws Exception {
        read(a, "<goodsContainer id=\"goodsContainer:cftA\"><storedGoods/><oldStoredGoods/></goodsContainer>");
        assertTrue(a.getStoredGoods().isEmpty());
        assertTrue(a.getOldStoredGoods().isEmpty());
    }

    public void testTC063() throws Exception {
        try {
            child(a, "<storedGoods><unexpected/></storedGoods>");
            fail("Expected XMLStreamException");
        } catch (XMLStreamException e) {
            assertTrue(e.getMessage().contains("Bogus GoodsContainer tag: unexpected"));
        }
    }

    public void testTC064() throws Exception {
        try {
            child(a, "<unexpected/>");
            fail("Expected XMLStreamException");
        } catch (XMLStreamException e) {
            assertNotNull(e.getMessage());
        }
    }

    public void testTC065() throws Exception {
        child(a, "<storedGoods><goods type=\"model.goods.a\"/></storedGoods>");
        assertEquals(0, a.getGoodsCount(g1));
        assertTrue(a.getStoredGoods().containsKey(g1));
        child(a, "<storedGoods><goods type=\"model.goods.a\" amount=\"17\"/><goods type=\"model.goods.a\" amount=\"23\"/></storedGoods>");
        assertEquals(23, a.getGoodsCount(g1));
    }

    public void testTC066() throws Exception {
        assertEquals("[goodsContainer:cftA]]", a.toString());
    }

    public void testTC067() throws Exception {
        a.setStoredGoods(storage(g1, 37));
        a.setOldStoredGoods(storage(g2, 23));
        assertEquals("[goodsContainer:cftA [model.goods.a=37][model.goods.b=23]]", a.toString());
    }
}
