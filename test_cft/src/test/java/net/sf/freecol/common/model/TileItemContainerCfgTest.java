package net.sf.freecol.common.model;

import java.io.StringReader;
import java.io.StringWriter;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;
import javax.xml.stream.XMLStreamException;
import junit.framework.TestCase;
import net.sf.freecol.common.io.FreeColXMLReader;
import net.sf.freecol.common.io.FreeColXMLWriter;
import net.sf.freecol.common.model.Constants.IntegrityType;
import net.sf.freecol.common.model.LostCityRumour.RumourType;
import net.sf.freecol.common.model.Map.Layer;
import net.sf.freecol.common.model.Modifier.ModifierType;
import net.sf.freecol.common.util.LogBuilder;
import net.sf.freecol.server.model.ServerGame;

public class TileItemContainerCfgTest extends TestCase {
    private Specification local;
    private Game game;
    private Tile tile;
    private Tile otherTile;
    private TileType land;
    private TileType water;
    private GoodsType goods;
    private UnitType unit;
    private ResourceType resourceType;
    private TileImprovementType roadType;
    private TileImprovementType riverType;
    private TileImprovementType otherType;
    private TileItemContainer a;
    private TileItemContainer b;
    private CountingColony colony;

    public static class CountingColony extends Colony {
        boolean inUse;
        int invalidations;
        public CountingColony(Game game, String id) { super(game, id); }
        public boolean isTileInUse(Tile tile) { return inUse; }
        public void invalidateCache() { invalidations++; }
    }

    public static class FixtureItem extends TileItem {
        int z;
        int bonus;
        boolean natural;
        boolean complete = true;
        boolean allowed = true;
        boolean produces;
        IntegrityType integrity = IntegrityType.INTEGRITY_GOOD;
        public FixtureItem(Game game, Tile tile, int z) { super(game, tile); this.z = z; }
        public int getZIndex() { return z; }
        public boolean isTileTypeAllowed(TileType type) { return allowed; }
        public int applyBonus(GoodsType goods, UnitType unit, int potential) { return potential + bonus; }
        public boolean canProduce(GoodsType goods, UnitType unit) { return produces; }
        public Stream<Modifier> getProductionModifiers(GoodsType goods, UnitType unit) {
            return Stream.of(new Modifier(goods.getId(), bonus, ModifierType.ADDITIVE));
        }
        public boolean isNatural() { return natural; }
        public boolean isComplete() { return complete; }
        public Layer getLayer() { return Layer.LAND; }
        public String getNameKey() { return "fixture.item"; }
        public String getXMLTagName() { return "fixtureItem"; }
        public IntegrityType checkIntegrity(boolean fix, LogBuilder lb) { return integrity; }
    }

    protected void setUp() throws Exception {
        super.setUp();
        local = new Specification();
        game = new ServerGame(local);
        land = local.findType("model.tile.cftLand", TileType.class);
        water = local.findType("model.tile.cftWater", TileType.class);
        field(water, TileType.class, "water", true);
        goods = local.findType("model.goods.cftGoods", GoodsType.class);
        unit = local.findType("model.unit.cftWorker", UnitType.class);
        resourceType = local.findType("model.resource.cftResource", ResourceType.class);
        resourceType.addModifier(new Modifier(goods.getId(), 3, ModifierType.ADDITIVE));
        roadType = improvementType("model.improvement.road", 300, false);
        riverType = improvementType("model.improvement.river", 400, true);
        otherType = improvementType("model.improvement.cftOther", 200, false);
        field(roadType, TileImprovementType.class, "movementCost", 1);
        Map map = new Map(game, 5, 5);
        game.setMap(map);
        map.populateTiles((x, y) -> new Tile(game, water, x, y));
        tile = map.getTile(2, 2);
        tile.setType(land);
        otherTile = map.getTile(2, 3);
        otherTile.setType(land);
        a = new TileItemContainer(game, tile);
        tile.setTileItemContainer(a);
        b = new TileItemContainer(game, otherTile);
        otherTile.setTileItemContainer(b);
        colony = new CountingColony(game, "colony:cft");
    }

    private void field(Object target, Class<?> owner, String name, Object value) throws Exception {
        Field field = owner.getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }

    private TileImprovementType improvementType(String id, int z, boolean natural) throws Exception {
        TileImprovementType type = local.findType(id, TileImprovementType.class);
        type.setZIndex(z);
        type.addScope(new Scope());
        field(type, TileImprovementType.class, "natural", natural);
        field(type, TileImprovementType.class, "magnitude", 1);
        return type;
    }

    private TileImprovement improvement(TileImprovementType type, int magnitude, int turns, String style) {
        TileImprovement result = new TileImprovement(game, tile, type,
                style == null ? null : TileImprovementStyle.getInstance(style));
        result.setMagnitude(magnitude);
        result.setTurnsToComplete(turns);
        return result;
    }

    private Resource resource() { return new Resource(game, tile, resourceType, 37); }

    private LostCityRumour rumour() {
        return new LostCityRumour(game, tile, RumourType.CIBOLA, "City");
    }

    private void items(TileItem... values) { a.setTileItems(Arrays.asList(values)); }

    private void usedColony() {
        tile.setSettlement(colony);
        colony.inUse = true;
        colony.invalidations = 0;
    }

    private void blockRiver() {
        Scope scope = new Scope();
        scope.setType("model.tile.notAllowed");
        riverType.addScope(scope);
    }

    private TileItemContainer snapshotContainer() throws Exception {
        Game snapshotGame = new ServerGame(local);
        Tile snapshotTile = new Tile(snapshotGame, tile.getId());
        snapshotTile.setType(land);
        TileItemContainer result = new TileItemContainer(snapshotGame, a.getId());
        field(result, TileItemContainer.class, "tile", snapshotTile);
        snapshotTile.setTileItemContainer(result);
        return result;
    }

    private String xml(TileItemContainer target) throws Exception {
        StringWriter out = new StringWriter();
        try (FreeColXMLWriter writer = new FreeColXMLWriter(out,
                FreeColXMLWriter.WriteScope.toSave(), false)) {
            target.toXML(writer);
            writer.flush();
        }
        return out.toString();
    }

    private void read(TileItemContainer target, String text) throws Exception {
        try (FreeColXMLReader reader = new FreeColXMLReader(new StringReader(text))) {
            reader.nextTag();
            target.readFromXML(reader);
        }
    }

    private void child(String text) throws Exception {
        try (FreeColXMLReader reader = new FreeColXMLReader(new StringReader(text))) {
            reader.nextTag();
            a.readChild(reader);
        }
    }

    public void testTC001() throws Exception {
        TileItemContainer created = new TileItemContainer(game, tile);
        assertSame(tile, created.getTile());
        assertSame(tile, a.getTile());
        TileItemContainer deferred = new TileItemContainer(game, "tileItemContainer:deferred");
        assertSame(null, deferred.getTile());
        assertTrue(a.getCompleteItems().isEmpty());
        assertEquals("tileItemContainer", a.getXMLTagName());
    }

    public void testTC002() throws Exception {
        try {
            new TileItemContainer(game, (Tile)null);
            fail("Expected RuntimeException");
        } catch (RuntimeException e) {
            assertTrue(e.getMessage().contains("Tile must not be null"));
        }
    }

    public void testTC003() throws Exception {
        items(resource());
        a.setTileItems(null);
        assertTrue(a.getCompleteItems().isEmpty());
    }

    public void testTC004() throws Exception {
        Resource r = resource();
        List<TileItem> input = new ArrayList<TileItem>(Arrays.asList(r));
        a.setTileItems(input);
        input.clear();
        assertTrue(a.contains(r));
        List<TileItem> returned = a.getCompleteItems();
        returned.clear();
        assertTrue(a.contains(r));
    }

    public void testTC005() throws Exception {
        tile.setSettlement(colony);
        colony.inUse = false;
        a.setTileItems(null);
        assertEquals(0, colony.invalidations);
        colony.inUse = true;
        a.setTileItems(null);
        assertEquals(1, colony.invalidations);
    }

    public void testTC006() throws Exception {
        assertSame(null, a.tryAddTileItem(null));
    }

    public void testTC007() throws Exception {
        Resource r = resource();
        assertSame(r, a.tryAddTileItem(r));
        assertTrue(a.contains(r));
    }

    public void testTC008() throws Exception {
        TileImprovement road = improvement(roadType, 1, 0, "11111111");
        assertSame(road, a.tryAddTileItem(road));
        assertSame(road, a.getRoad());
    }

    public void testTC009() throws Exception {
        Resource r = resource();
        TileImprovement old = improvement(roadType, 1, 0, "00000000");
        items(r, old);
        usedColony();
        TileImprovement incoming = improvement(roadType, 3, 0, "11111111");
        incoming.setVirtual(true);
        assertSame(old, a.tryAddTileItem(incoming));
        assertEquals(3, old.getMagnitude());
        assertSame(incoming.getStyle(), old.getStyle());
        assertTrue(old.getVirtual());
        assertEquals(1, colony.invalidations);
        assertEquals(1, a.getImprovements().size());
    }

    public void testTC010() throws Exception {
        TileImprovement old = improvement(roadType, 3, 0, "11111111");
        items(old);
        usedColony();
        assertSame(old, a.tryAddTileItem(improvement(roadType, 3, 0, "00000000")));
        assertSame(old, a.tryAddTileItem(improvement(roadType, 1, 0, "00000000")));
        assertEquals(3, old.getMagnitude());
        assertEquals("11111111", old.getStyle().toString());
        assertEquals(0, colony.invalidations);
    }

    public void testTC011() throws Exception {
        TileImprovement old = improvement(roadType, 1, 0, "11111111");
        items(old);
        TileImprovement incoming = improvement(otherType, 1, 0, null);
        assertSame(incoming, a.tryAddTileItem(incoming));
        assertSame(old, a.getImprovements().get(0));
        assertSame(incoming, a.getImprovements().get(1));
    }

    public void testTC012() throws Exception {
        TileImprovement old = improvement(otherType, 1, 0, null);
        items(old);
        TileImprovement incoming = improvement(roadType, 1, 0, "11111111");
        assertSame(incoming, a.tryAddTileItem(incoming));
        assertSame(incoming, a.getImprovements().get(0));
        assertSame(old, a.getImprovements().get(1));
    }

    public void testTC013() throws Exception {
        otherType.setZIndex(300);
        TileImprovement old = improvement(otherType, 1, 0, null);
        TileImprovement incoming = improvement(roadType, 1, 0, "11111111");
        items(old);
        a.tryAddTileItem(incoming);
        assertSame(old, a.getImprovements().get(0));
        assertSame(incoming, a.getImprovements().get(1));
    }

    public void testTC014() throws Exception {
        Resource r = resource();
        items(r);
        usedColony();
        assertSame(r, a.removeTileItem(r));
        assertSame(null, r.getLocation());
        assertFalse(a.contains(r));
        assertEquals(1, colony.invalidations);
    }

    public void testTC015() throws Exception {
        assertSame(null, a.removeTileItem(resource()));
    }

    public void testTC016() throws Exception {
        Resource r = resource();
        TileImprovement road = improvement(roadType, 1, 0, "11111111");
        items(r, road);
        usedColony();
        a.removeAll(Resource.class);
        assertFalse(a.contains(r));
        assertTrue(a.contains(road));
        assertSame(tile, r.getLocation());
        assertEquals(0, colony.invalidations);
    }

    public void testTC017() throws Exception {
        Resource r = resource();
        TileImprovement done = improvement(roadType, 1, 0, "11111111");
        TileImprovement pending = improvement(otherType, 1, 4, null);
        items(r, done, pending);
        assertEquals(2, a.getCompleteItems().size());
    }

    public void testTC018() throws Exception {
        assertSame(null, a.getImprovement(roadType));
        TileImprovement road = improvement(roadType, 1, 0, "11111111");
        items(resource(), improvement(otherType, 1, 0, null), road);
        assertSame(road, a.getImprovement(roadType));
        assertSame(null, a.getImprovement(new TileImprovementType(roadType.getId(), local)));
    }

    public void testTC019() throws Exception {
        TileImprovement done = improvement(roadType, 1, 0, "11111111");
        TileImprovement pending = improvement(otherType, 1, 4, null);
        items(resource(), done, pending);
        assertEquals(2, a.getImprovements().size());
        assertEquals(1, a.getCompleteImprovements().size());
        assertSame(done, a.getCompleteImprovements().get(0));
    }

    public void testTC020() throws Exception {
        assertSame(null, a.getResource());
        assertSame(null, a.getLostCityRumour());
        Resource r = resource();
        LostCityRumour lcr = rumour();
        items(improvement(otherType, 1, 0, null), lcr, r);
        assertSame(r, a.getResource());
        assertSame(lcr, a.getLostCityRumour());
    }

    public void testTC021() throws Exception {
        assertSame(null, a.getRoad());
        assertSame(null, a.getRiver());
        TileImprovement road = improvement(roadType, 1, 0, "11111111");
        TileImprovement river = improvement(riverType, 1, 0, "1000");
        items(resource(), improvement(otherType, 1, 0, null), road, river);
        assertSame(road, a.getRoad());
        assertSame(river, a.getRiver());
    }

    public void testTC022() throws Exception {
        assertFalse(a.hasImprovement(roadType));
        TileImprovement road = improvement(roadType, 1, 4, "11111111");
        items(road);
        assertFalse(a.hasImprovement(roadType));
        road.setTurnsToComplete(0);
        assertTrue(a.hasImprovement(roadType));
    }

    public void testTC023() throws Exception {
        a.removeIncompatibleImprovements();
        assertTrue(a.getCompleteItems().isEmpty());
    }

    public void testTC024() throws Exception {
        FixtureItem allowed = new FixtureItem(game, tile, 20);
        FixtureItem blocked = new FixtureItem(game, tile, 10);
        blocked.allowed = false;
        items(allowed, blocked);
        usedColony();
        a.removeIncompatibleImprovements();
        assertTrue(a.contains(allowed));
        assertFalse(a.contains(blocked));
        assertEquals(1, colony.invalidations);
    }

    public void testTC025() throws Exception {
        TileImprovement river = improvement(riverType, 1, 0, "1000");
        items(river);
        a.removeIncompatibleImprovements();
        assertTrue(a.contains(river));
    }

    public void testTC026() throws Exception {
        blockRiver();
        TileImprovement river = improvement(riverType, 1, 0, "1000");
        items(river);
        usedColony();
        a.removeIncompatibleImprovements();
        assertFalse(a.contains(river));
        assertEquals(1, colony.invalidations);
    }

    public void testTC027() throws Exception {
        blockRiver();
        TileImprovement river = improvement(riverType, 1, 0, "1000");
        items(river);
        tile.setType(water);
        a.removeIncompatibleImprovements();
        assertFalse(a.contains(river));
        assertEquals("1000", river.getStyle().toString());
    }

    public void testTC028() throws Exception {
        assertEquals(7, a.getTotalBonusPotential(goods, unit, 7, false));
    }

    public void testTC029() throws Exception {
        FixtureItem natural = new FixtureItem(game, tile, 20);
        natural.natural = true;
        natural.bonus = 3;
        FixtureItem artificial = new FixtureItem(game, tile, 10);
        artificial.bonus = 5;
        items(natural, artificial);
        assertEquals(15, a.getTotalBonusPotential(goods, unit, 7, false));
        assertEquals(10, a.getTotalBonusPotential(goods, unit, 7, true));
    }

    public void testTC030() throws Exception {
        FixtureItem item = new FixtureItem(game, tile, 10);
        item.bonus = 5;
        items(resource(), item);
        assertEquals(2, (int)a.getProductionModifiers(goods, unit).count());
        assertEquals(1, (int)a.getProductionModifiersWithoutResource(goods, unit).count());
        assertEquals(5, (int)a.getProductionModifiersWithoutResource(goods, unit).findFirst().get().getValue());
    }

    public void testTC031() throws Exception {
        assertEquals(0, (int)a.getProductionModifiers(goods, unit).count());
        assertEquals(0, (int)a.getProductionModifiersWithoutResource(goods, unit).count());
        assertFalse(a.canProduce(goods, unit));
    }

    public void testTC032() throws Exception {
        FixtureItem item = new FixtureItem(game, tile, 10);
        items(rumour(), item);
        assertFalse(a.canProduce(goods, unit));
        item.produces = true;
        assertTrue(a.canProduce(goods, unit));
    }

    public void testTC033() throws Exception {
        assertEquals(6, a.getMoveCost(null, null, 6));
        items(resource(), improvement(roadType, 1, 4, "11111111"));
        assertEquals(6, a.getMoveCost(null, null, 6));
    }

    public void testTC034() throws Exception {
        items(improvement(roadType, 1, 0, "11111111"));
        assertEquals(1, a.getMoveCost(otherTile, tile, 6));
    }

    public void testTC035() throws Exception {
        items(improvement(roadType, 1, 0, "11111111"));
        assertEquals(Constants.INFINITY, a.getMoveCost(game.getMap().getTile(0, 0), tile, 6));
    }

    public void testTC036() throws Exception {
        b.tryAddTileItem(new Resource(game, otherTile, resourceType, 10));
        b.copyFrom(a, Layer.ALL);
        assertTrue(b.getCompleteItems().isEmpty());
    }

    public void testTC037() throws Exception {
        Resource r = resource();
        LostCityRumour lcr = rumour();
        TileImprovement road = improvement(roadType, 1, 0, "11111111");
        TileImprovement river = improvement(riverType, 2, 0, "1000");
        items(r, lcr, road, river);
        b.copyFrom(a, Layer.ALL);
        assertEquals(4, b.getCompleteItems().size());
        assertEquals(37, b.getResource().getQuantity());
        assertEquals("City", b.getLostCityRumour().getName());
        assertEquals(2, b.getRiver().getMagnitude());
        assertFalse(r == b.getResource());
        assertSame(otherTile, b.getResource().getTile());
        assertNotNull(b.getRoad());
    }

    public void testTC038() throws Exception {
        items(resource(), rumour(), improvement(roadType, 1, 0, "11111111"), improvement(riverType, 1, 0, "1000"));
        b.copyFrom(a, Layer.RESOURCES);
        assertEquals(2, b.getCompleteItems().size());
        assertNotNull(b.getResource());
        assertNotNull(b.getRiver());
        assertSame(null, b.getRoad());
        assertSame(null, b.getLostCityRumour());
    }

    public void testTC039() throws Exception {
        items(resource(), rumour(), improvement(roadType, 1, 0, "11111111"));
        b.copyFrom(a, Layer.LAND);
        assertTrue(b.getCompleteItems().isEmpty());
    }

    public void testTC040() throws Exception {
        items(new FixtureItem(game, tile, 10));
        b.copyFrom(a, Layer.ALL);
        assertTrue(b.getCompleteItems().isEmpty());
    }

    public void testTC041() throws Exception {
        Resource r = resource();
        assertFalse(a.contains(r));
        items(r);
        assertTrue(a.contains(r));
    }

    public void testTC042() throws Exception {
        items(resource());
        a.disposeResources();
        assertTrue(a.getCompleteItems().isEmpty());
        assertTrue(a.isDisposed());
    }

    public void testTC043() throws Exception {
        assertEquals("INTEGRITY_GOOD", a.checkIntegrity(false, new LogBuilder(64)).name());
    }

    public void testTC044() throws Exception {
        FixtureItem item = new FixtureItem(game, tile, 10);
        item.integrity = IntegrityType.INTEGRITY_FAIL;
        items(item);
        assertEquals("INTEGRITY_FAIL", a.checkIntegrity(false, new LogBuilder(64)).name());
        assertTrue(a.contains(item));
    }

    public void testTC045() throws Exception {
        FixtureItem item = new FixtureItem(game, tile, 10);
        item.integrity = IntegrityType.INTEGRITY_FAIL;
        items(item);
        assertEquals("INTEGRITY_FAIL", a.checkIntegrity(true, new LogBuilder(64)).name());
        assertFalse(a.contains(item));
    }

    public void testTC046() throws Exception {
        FixtureItem item = new FixtureItem(game, tile, 10);
        items(item);
        assertEquals("INTEGRITY_GOOD", a.checkIntegrity(true, new LogBuilder(64)).name());
        assertTrue(a.contains(item));
    }

    public void testTC047() throws Exception {
        TileImprovement item = improvement(otherType, 1, 0, null);
        items(item);
        assertEquals("INTEGRITY_GOOD", a.checkIntegrity(true, new LogBuilder(64)).name());
        assertTrue(a.contains(item));
    }

    public void testTC048() throws Exception {
        TileImprovement river = improvement(riverType, 1, 0, null);
        items(river);
        assertEquals("INTEGRITY_FAIL", a.checkIntegrity(true, new LogBuilder(64)).name());
        assertFalse(a.contains(river));
    }

    public void testTC049() throws Exception {
        TileImprovement river = improvement(riverType, 1, 0, "0000");
        items(river);
        assertEquals("INTEGRITY_FAIL", a.checkIntegrity(true, new LogBuilder(64)).name());
        assertFalse(a.contains(river));
    }

    public void testTC050() throws Exception {
        TileImprovement river = improvement(riverType, 1, 0, "1000");
        items(river);
        assertEquals("INTEGRITY_GOOD", a.checkIntegrity(true, new LogBuilder(64)).name());
        assertTrue(a.contains(river));
    }

    public void testTC051() throws Exception {
        assertFalse(a.copyIn((FreeColObject)null));
        assertFalse(a.copyIn(StringTemplate.key(a.getId())));
        assertFalse(a.copyIn(b));
    }

    public void testTC052() throws Exception {
        items(resource());
        TileItemContainer source = snapshotContainer();
        assertTrue(a.copyIn(source));
        assertSame(tile, a.getTile());
        assertTrue(a.getCompleteItems().isEmpty());
    }

    public void testTC053() throws Exception {
        TileItemContainer source = snapshotContainer();
        Game sourceGame = source.getGame();
        Tile sourceTile = source.getTile();
        Resource r = new Resource(sourceGame, sourceTile, resourceType, 37);
        LostCityRumour lcr = new LostCityRumour(sourceGame, sourceTile, RumourType.CIBOLA, "City");
        source.setTileItems(Arrays.asList(r, lcr));
        assertTrue(a.copyIn(source));
        assertSame(tile, a.getTile());
        assertEquals(2, a.getCompleteItems().size());
        assertEquals(37, a.getResource().getQuantity());
        assertEquals("City", a.getLostCityRumour().getName());
        assertSame(tile, a.getResource().getTile());
        assertFalse(r == a.getResource());
    }

    public void testTC054() throws Exception {
        TileItemContainer source = snapshotContainer();
        source.setTileItems(Arrays.asList((TileItem)null));
        try {
            a.copyIn(source);
            fail("Expected RuntimeException");
        } catch (RuntimeException e) {
            assertTrue(e.getMessage().contains("TileItem class fail null"));
        }
        assertTrue(a.getCompleteItems().isEmpty());
    }

    public void testTC055() throws Exception {
        String text = xml(a);
        assertTrue(text.contains("tile="));
        TileItemContainer copy = snapshotContainer();
        read(copy, text);
        assertEquals(tile.getId(), copy.getTile().getId());
        assertTrue(copy.getCompleteItems().isEmpty());
    }

    public void testTC056() throws Exception {
        items(resource(), rumour(), improvement(roadType, 1, 0, "11111111"));
        String text = xml(a);
        TileItemContainer copy = snapshotContainer();
        read(copy, text);
        assertEquals(3, copy.getCompleteItems().size());
        assertEquals(37, copy.getResource().getQuantity());
        assertEquals("City", copy.getLostCityRumour().getName());
        assertNotNull(copy.getRoad());
    }

    public void testTC057() throws Exception {
        items(resource());
        read(a, "<tileItemContainer id=\"" + a.getId() + "\" tile=\"" + tile.getId() + "\"/>");
        assertTrue(a.getCompleteItems().isEmpty());
    }

    public void testTC058() throws Exception {
        child("<lostCityRumour id=\"lostCityRumour:xml\" tile=\"" + tile.getId() + "\" name=\"XMLCity\"/>");
        assertEquals("XMLCity", a.getLostCityRumour().getName());
    }

    public void testTC059() throws Exception {
        child("<lostCityRumour/>");
        assertSame(null, a.getLostCityRumour());
    }

    public void testTC060() throws Exception {
        child("<resource id=\"resource:xml\" tile=\"" + tile.getId() + "\" type=\"model.resource.cftResource\" quantity=\"37\"/>");
        assertEquals(37, a.getResource().getQuantity());
    }

    public void testTC061() throws Exception {
        child("<tileImprovement id=\"tileImprovement:xml\" tile=\"" + tile.getId() + "\" type=\"model.improvement.road\" turns-to-complete=\"0\" style=\"11111111\"/>");
        assertNotNull(a.getRoad());
    }

    public void testTC062() throws Exception {
        child("<tileimprovement id=\"tileImprovement:legacy\" tile=\"" + tile.getId() + "\" type=\"model.improvement.road\" turns-to-complete=\"0\" style=\"11111111\"/>");
        assertNotNull(a.getRoad());
    }

    public void testTC063() throws Exception {
        try {
            child("<unexpected/>");
            fail("Expected XMLStreamException");
        } catch (XMLStreamException e) {
            assertNotNull(e.getMessage());
        }
    }

    public void testTC064() throws Exception {
        try {
            read(a, "<tileItemContainer id=\"" + a.getId() + "\"/>");
            fail("Expected XMLStreamException");
        } catch (XMLStreamException e) {
            assertTrue(e.getMessage().contains("tile"));
        }
    }

    public void testTC065() throws Exception {
        assertEquals("[" + a.getId() + "]", a.toString());
    }

    public void testTC066() throws Exception {
        Resource r = resource();
        items(r);
        String text = a.toString();
        assertTrue(text.startsWith("[" + a.getId() + " "));
        assertEquals("[" + a.getId() + " 37 model.resource.cftResource]", text);
        assertTrue(text.endsWith("]"));
    }
}
