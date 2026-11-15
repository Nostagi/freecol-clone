package net.sf.freecol.common.model;

import java.io.StringReader;
import java.io.StringWriter;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;
import javax.xml.stream.XMLStreamException;
import net.sf.freecol.common.FreeColException;
import net.sf.freecol.common.io.FreeColXMLReader;
import net.sf.freecol.common.io.FreeColXMLWriter;
import net.sf.freecol.server.model.ServerUnit;
import net.sf.freecol.util.test.FreeColTestCase;

public class UnitLocationCfgTest extends FreeColTestCase {
    private Game game;
    private Player dutch;
    private Player french;
    private TestLocation loc;

    public static class TestLocation extends UnitLocation {
        Integer capacity;
        Settlement settlement;
        public TestLocation(Game game) { super(game); }
        public TestLocation(Game game, String id) { super(game, id); }
        public int getRank() { return 0; }
        public Location up() { return this; }
        public String toShortString() { return getId(); }
        public String getXMLTagName() { return "cftUnitLocation"; }
        public int getUnitCapacity() {
            return capacity == null ? super.getUnitCapacity() : capacity;
        }
        public Settlement getSettlement() {
            return settlement == null ? super.getSettlement() : settlement;
        }
    }

    protected void setUp() throws Exception {
        super.setUp();
        game = getStandardGame();
        dutch = game.getPlayerByNationId("model.nation.dutch");
        french = game.getPlayerByNationId("model.nation.french");
        loc = new TestLocation(game, "cftUnitLocation:test");
    }

    private Unit unit(String type, Player owner) {
        return new ServerUnit(game, null, owner, spec().getUnitType("model.unit." + type));
    }

    private Unit colonist() { return unit("freeColonist", dutch); }
    private Unit ship() { return unit("caravel", dutch); }

    private String xml(FreeColObject value) throws Exception {
        StringWriter out = new StringWriter();
        try (FreeColXMLWriter writer = new FreeColXMLWriter(out,
                FreeColXMLWriter.WriteScope.toSave(), false)) {
            value.toXML(writer);
            writer.flush();
        }
        return out.toString();
    }

    public void testTC001() throws Exception {
        TestLocation fresh = new TestLocation(game);
        assertTrue(fresh.getId().startsWith("cftUnitLocation:"));
        assertTrue(fresh.isEmpty());
        assertSame(fresh, game.getFreeColGameObject(fresh.getId()));
    }

    public void testTC002() throws Exception {
        assertEquals("cftUnitLocation:test", loc.getId());
        assertEquals(Integer.MAX_VALUE, loc.getUnitCapacity());
        assertTrue(loc.isEmpty());
        assertFalse(loc.isFull());
        assertSame(null, loc.getTile());
        assertSame(null, loc.getGoodsContainer());
        assertSame(null, loc.getSettlement());
        assertSame(null, loc.getColony());
        assertSame(null, loc.getIndianSettlement());
        assertEquals("cftUnitLocation:test", loc.getLocationLabel().getId());
        assertEquals("cftUnitLocation:test", loc.getLocationLabelFor(dutch).getId());
    }

    public void testTC003() throws Exception {
        assertEquals("model.noAddReason.none.description", UnitLocation.NoAddReason.NONE.getDescriptionKey());
    }

    public void testTC004() throws Exception {
        assertEquals("model.noAddReason.wrongType.description", UnitLocation.NoAddReason.WRONG_TYPE.getDescriptionKey());
    }

    public void testTC005() throws Exception {
        assertEquals(0, loc.getUnitCount());
        assertEquals(0, (int)loc.getUnits().count());
        assertTrue(loc.getUnitList().isEmpty());
        assertSame(null, loc.getFirstUnit());
        assertSame(null, loc.getLastUnit());
        assertEquals(0, loc.getSpaceTaken());
        assertEquals(0, loc.getTotalUnitCount());
    }

    public void testTC006() throws Exception {
        Unit u = colonist();
        assertTrue(loc.canAdd(u));
        assertEquals("NONE", loc.getNoAddReason(u).name());
        assertTrue(loc.add(u));
        assertSame(loc, u.getLocation());
        assertEquals(1, loc.getUnitCount());
        assertFalse(loc.isEmpty());
        assertTrue(loc.contains(u));
    }

    public void testTC007() throws Exception {
        Unit u = colonist();
        loc.add(u);
        assertTrue(loc.add(u));
        assertFalse(loc.canAdd(u));
        assertEquals("ALREADY_PRESENT", loc.getNoAddReason(u).name());
        assertEquals(1, loc.getUnitCount());
    }

    public void testTC008() throws Exception {
        loc.capacity = 1;
        Unit first = colonist();
        Unit second = colonist();
        assertFalse(loc.isFull());
        loc.add(first);
        assertTrue(loc.isFull());
        assertEquals("CAPACITY_EXCEEDED", loc.getNoAddReason(second).name());
        assertFalse(loc.add(second));
        assertEquals(1, loc.getUnitCount());
        assertSame(null, second.getLocation());
    }

    public void testTC009() throws Exception {
        loc.add(colonist());
        Unit enemy = unit("freeColonist", french);
        assertEquals("OCCUPIED_BY_ENEMY", loc.getNoAddReason(enemy).name());
        assertFalse(loc.add(enemy));
        assertEquals(1, loc.getUnitCount());
    }

    public void testTC010() throws Exception {
        assertEquals("WRONG_TYPE", loc.getNoAddReason(null).name());
        assertFalse(loc.canAdd(null));
        assertFalse(loc.add(null));
        assertFalse(loc.contains(null));
    }

    public void testTC011() throws Exception {
        Unit vessel = ship();
        Goods goods = new Goods(game, vessel, spec().getGoodsType("model.goods.food"), 25);
        assertEquals("WRONG_TYPE", loc.getNoAddReason(goods).name());
        assertTrue(loc.add(goods));
        assertSame(null, goods.getLocation());
        assertEquals(0, loc.getUnitCount());
    }

    public void testTC012() throws Exception {
        Unit u = colonist();
        loc.add(u);
        assertTrue(loc.remove(u));
        assertSame(null, u.getLocation());
        assertEquals(0, loc.getUnitCount());
    }

    public void testTC013() throws Exception {
        Unit u = colonist();
        assertFalse(loc.remove(u));
        assertFalse(loc.contains(u));
    }

    public void testTC014() throws Exception {
        Goods goods = new Goods(game, null, spec().getGoodsType("model.goods.food"), 25);
        assertFalse(loc.remove(goods));
        assertFalse(loc.remove(null));
        assertFalse(loc.contains(goods));
    }

    public void testTC015() throws Exception {
        Unit a = colonist();
        Unit b = colonist();
        loc.add(a);
        loc.add(b);
        assertSame(a, loc.getFirstUnit());
        assertSame(b, loc.getLastUnit());
    }

    public void testTC016() throws Exception {
        Unit u = colonist();
        loc.add(u);
        List<Unit> snapshot = loc.getUnitList();
        snapshot.clear();
        assertEquals(1, loc.getUnitCount());
        assertEquals(1, (int)loc.getUnits().count());
        assertEquals(0, snapshot.size());
    }

    public void testTC017() throws Exception {
        Unit a = colonist();
        Unit b = colonist();
        loc.add(a);
        loc.add(b);
        loc.moveToFront(b);
        assertSame(b, loc.getFirstUnit());
        assertSame(a, loc.getLastUnit());
        assertEquals(2, loc.getUnitCount());
    }

    public void testTC018() throws Exception {
        Unit a = colonist();
        loc.add(a);
        loc.moveToFront(colonist());
        assertSame(a, loc.getFirstUnit());
        assertEquals(1, loc.getUnitCount());
    }

    public void testTC019() throws Exception {
        Unit u = colonist();
        loc.add(u);
        loc.clearUnitList();
        assertTrue(loc.isEmpty());
        assertSame(loc, u.getLocation());
    }

    public void testTC020() throws Exception {
        loc.add(colonist());
        loc.add(colonist());
        assertEquals(2, loc.getSpaceTaken());
        assertEquals(2, loc.getTotalUnitCount());
    }

    public void testTC021() throws Exception {
        Unit vessel = ship();
        Unit passenger = colonist();
        passenger.setLocation(vessel);
        loc.add(vessel);
        assertEquals(1, loc.getUnitCount());
        assertEquals(2, loc.getTotalUnitCount());
    }

    public void testTC022() throws Exception {
        assertFalse(loc.hasCarrierWithSpace(1));
        assertSame(null, loc.getCarrierForUnit(colonist()));
        assertTrue(loc.getNavalUnits().isEmpty());
    }

    public void testTC023() throws Exception {
        loc.add(colonist());
        assertFalse(loc.hasCarrierWithSpace(1));
        assertSame(null, loc.getCarrierForUnit(colonist()));
    }

    public void testTC024() throws Exception {
        Unit vessel = ship();
        loc.add(vessel);
        assertEquals(2, vessel.getSpaceLeft());
        assertTrue(loc.hasCarrierWithSpace(1));
        assertTrue(loc.hasCarrierWithSpace(2));
        assertFalse(loc.hasCarrierWithSpace(3));
        assertSame(vessel, loc.getCarrierForUnit(colonist()));
    }

    public void testTC025() throws Exception {
        Unit vessel = ship();
        vessel.setHitPoints(1);
        loc.add(vessel);
        assertTrue(vessel.isDamagedAndUnderForcedRepair());
        assertFalse(loc.hasCarrierWithSpace(1));
    }

    public void testTC026() throws Exception {
        Unit land = colonist();
        Unit vessel = ship();
        loc.add(land);
        loc.add(vessel);
        assertEquals(1, loc.getNavalUnits().size());
        assertSame(vessel, loc.getNavalUnits().get(0));
    }

    public void testTC027() throws Exception {
        Colony colony = new Colony(game, "colony:cft");
        loc.settlement = colony;
        assertSame(colony, loc.getColony());
        assertSame(null, loc.getIndianSettlement());
    }

    public void testTC028() throws Exception {
        IndianSettlement village = new IndianSettlement(game, "indianSettlement:cft");
        loc.settlement = village;
        assertSame(null, loc.getColony());
        assertSame(village, loc.getIndianSettlement());
    }

    public void testTC029() throws Exception {
        assertFalse(loc.equipForRole(colonist(), spec().getRole("model.role.soldier"), 1));
    }

    public void testTC030() throws Exception {
        assertEquals(0, loc.priceGoods(Collections.<AbstractGoods>emptyList()));
    }

    public void testTC031() throws Exception {
        try {
            loc.priceGoods(Collections.singletonList(new AbstractGoods(spec().getGoodsType("model.goods.food"), 25)));
            fail("Expected FreeColException");
        } catch (FreeColException e) {
            assertTrue(e.getMessage().startsWith("Not available:"));
        }
    }

    public void testTC032() throws Exception {
        loc.intern();
        assertSame(loc, game.getFreeColGameObject(loc.getId()));
    }

    public void testTC033() throws Exception {
        Unit a = colonist();
        Unit b = colonist();
        loc.add(a);
        loc.add(b);
        loc.intern();
        assertSame(loc, game.getFreeColGameObject(loc.getId()));
        assertSame(a, game.getFreeColGameObject(a.getId()));
        assertSame(b, game.getFreeColGameObject(b.getId()));
    }

    public void testTC034() throws Exception {
        assertEquals(1, (int)loc.getDisposables().count());
        assertTrue(loc.getDisposables().anyMatch(x -> x == loc));
    }

    public void testTC035() throws Exception {
        Unit vessel = ship();
        Unit passenger = colonist();
        passenger.setLocation(vessel);
        loc.add(vessel);
        assertEquals(4, (int)loc.getDisposables().count());
        assertTrue(loc.getDisposables().anyMatch(x -> x == passenger));
    }

    public void testTC036() throws Exception {
        Unit u = colonist();
        loc.add(u);
        loc.disposeResources();
        assertTrue(loc.isDisposed());
        assertEquals(0, loc.getUnitCount());
        assertFalse(u.isDisposed());
    }

    public void testTC037() throws Exception {
        Method method = UnitLocation.class.getDeclaredMethod("addUnit", Unit.class);
        method.setAccessible(true);
        assertEquals(Boolean.FALSE, method.invoke(loc, new Object[] { null }));
        assertTrue(loc.isEmpty());
    }

    public void testTC038() throws Exception {
        assertFalse(loc.copyIn((FreeColObject)null));
        Goods wrong = new Goods(game, null, spec().getGoodsType("model.goods.food"), 25);
        wrong.setId(loc.getId());
        assertFalse(loc.copyIn(wrong));
    }

    public void testTC039() throws Exception {
        Game sourceGame = getStandardGame();
        TestLocation source = new TestLocation(sourceGame, loc.getId());
        TestLocation target = loc;
        target.add(colonist());
        assertTrue(target.copyIn(source));
        assertTrue(target.isEmpty());
    }

    public void testTC040() throws Exception {
        Game sourceGame = getStandardGame();
        TestLocation source = new TestLocation(sourceGame, loc.getId());
        Player owner = sourceGame.getPlayerByNationId("model.nation.dutch");
        Unit a = new ServerUnit(sourceGame, null, owner, spec().getUnitType("model.unit.freeColonist"));
        Unit b = new ServerUnit(sourceGame, null, owner, spec().getUnitType("model.unit.freeColonist"));
        source.add(a);
        source.add(b);
        TestLocation target = loc;
        assertTrue(target.copyIn(source));
        assertEquals(2, target.getUnitCount());
        assertSame(target, target.getFirstUnit().getLocation());
        assertSame(target, target.getLastUnit().getLocation());
    }

    public void testTC041() throws Exception {
        String text = xml(loc);
        assertTrue(text.contains("cftUnitLocation"));
        assertFalse(text.contains("<unit "));
    }

    public void testTC042() throws Exception {
        Unit u = colonist();
        loc.add(u);
        String text = xml(loc);
        assertTrue(text.contains("<unit "));
        assertTrue(text.contains(u.getId()));
        assertSame(loc, u.getLocation());
    }

    public void testTC043() throws Exception {
        Unit u = colonist();
        loc.add(u);
        u.setLocationNoUpdate(null);
        String text = xml(loc);
        assertTrue(text.contains(u.getId()));
        assertSame(loc, u.getLocation());
    }

    public void testTC044() throws Exception {
        loc.add(colonist());
        try (FreeColXMLReader reader = new FreeColXMLReader(new StringReader("<cftUnitLocation/>"))) {
            reader.nextTag();
            loc.readChildren(reader);
        }
        assertTrue(loc.isEmpty());
    }

    public void testTC045() throws Exception {
        Unit u = colonist();
        String text = xml(u);
        try (FreeColXMLReader reader = new FreeColXMLReader(new StringReader(text))) {
            reader.nextTag();
            loc.readChild(reader);
        }
        assertEquals(1, loc.getUnitCount());
        assertSame(u, loc.getFirstUnit());
        assertSame(loc, u.getLocation());
    }

    public void testTC046() throws Exception {
        try (FreeColXMLReader reader = new FreeColXMLReader(new StringReader("<unexpected/>"))) {
            reader.nextTag();
            try {
                loc.readChild(reader);
                fail("Expected XMLStreamException");
            } catch (XMLStreamException e) {
                assertNotNull(e.getMessage());
            }
        }
    }

    public void testTC047() throws Exception {
        Unit u = colonist();
        String text = "<cftUnitLocation>" + xml(u) + "</cftUnitLocation>";
        loc.add(colonist());
        try (FreeColXMLReader reader = new FreeColXMLReader(new StringReader(text))) {
            reader.nextTag();
            loc.readChildren(reader);
        }
        assertEquals(1, loc.getUnitCount());
        assertSame(u, loc.getFirstUnit());
        assertSame(loc, u.getLocation());
    }
}
