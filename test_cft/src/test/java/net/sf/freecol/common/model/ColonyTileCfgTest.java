package net.sf.freecol.common.model;

import java.io.StringReader;
import java.io.StringWriter;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import javax.xml.stream.XMLStreamException;
import net.sf.freecol.common.io.FreeColXMLReader;
import net.sf.freecol.common.io.FreeColXMLWriter;
import net.sf.freecol.server.model.ServerUnit;
import net.sf.freecol.util.test.FreeColTestCase;

public class ColonyTileCfgTest extends FreeColTestCase {
    private Game game;
    private ClaimPlayer player;
    private Player enemy;
    private FixtureColony colony;
    private Tile center;
    private Tile work;
    private ColonyTile ct;

    public static class ClaimPlayer extends Player {
        NoClaimReason claim = NoClaimReason.NONE;
        public ClaimPlayer(Game game, String id) { super(game, id); }
        public NoClaimReason canClaimForSettlementReason(Tile tile) { return claim; }
    }

    public static class FixtureColony extends Colony {
        ProductionInfo observed = new ProductionInfo();
        boolean consuming;
        int net;
        public FixtureColony(Game game, String id) { super(game, id); }
        public ProductionInfo getProductionInfo(Object object) { return observed; }
        public boolean isConsuming(GoodsType type) { return consuming; }
        public int getNetProductionOf(GoodsType type) { return net; }
        public int getProductionBonus() { return 0; }
    }

    protected void setUp() throws Exception {
        super.setUp();
        game = getStandardGame();
        game.changeMap(getTestMap(spec().getTileType("model.tile.plains"), true));
        player = new ClaimPlayer(game, "player:cftClaim");
        player.setNation(spec().getNation("model.nation.dutch"));
        player.setNationType(game.getPlayerByNationId("model.nation.dutch").getNationType());
        player.changePlayerType(Player.PlayerType.COLONIAL);
        enemy = game.getPlayerByNationId("model.nation.french");
        center = game.getMap().getTile(8, 7);
        work = game.getMap().getTile(9, 7);
        colony = new FixtureColony(game, "colony:cftTile");
        colony.setOwner(player);
        colony.setName("CFT");
        field(colony, Settlement.class, "tile", center);
        center.setSettlement(colony);
        center.setOwningSettlement(colony);
        center.setOwner(player);
        work.setOwningSettlement(colony);
        work.setOwner(player);
        ct = new ColonyTile(game, "colonyTile:cft");
        ct.colony = colony;
        ct.setWorkTile(work);
    }

    private void field(Object target, Class<?> owner, String name, Object value) throws Exception {
        Field f = owner.getDeclaredField(name);
        f.setAccessible(true);
        f.set(target, value);
    }

    private GoodsType goods(String name) { return spec().getGoodsType("model.goods." + name); }

    private Unit worker() {
        return new ServerUnit(game, null, player, spec().getUnitType("model.unit.freeColonist"));
    }

    private void put(Unit unit) throws Exception {
        Method method = UnitLocation.class.getDeclaredMethod("addUnit", Unit.class);
        method.setAccessible(true);
        method.invoke(ct, unit);
    }

    private ProductionType production(boolean unattended, int food, int tools) {
        ProductionType pt = new ProductionType(spec());
        pt.setUnattended(unattended);
        if (food != 0) pt.addOutput(new AbstractGoods(goods("food"), food));
        if (tools != 0) pt.addOutput(new AbstractGoods(goods("tools"), tools));
        return pt;
    }

    private TileType terrain(String id, int attendedFood, int unattendedFood, int unattendedTools) {
        TileType type = new TileType("model.tile." + id, spec());
        type.setProductionTypes(Arrays.asList(production(false, attendedFood, 0),
                production(true, unattendedFood, unattendedTools)));
        return type;
    }

    private TileImprovementType improvement() {
        TileImprovementType ti = new TileImprovementType("model.improvement.cft", spec());
        ti.addScope(new Scope());
        ti.addModifier(new Modifier(goods("food").getId(), 1, Modifier.ModifierType.ADDITIVE));
        return ti;
    }

    private TileImprovementType change(TileType from, TileType to) throws Exception {
        TileImprovementType ti = improvement();
        TileTypeChange mapping = new TileTypeChange(spec());
        field(mapping, TileTypeChange.class, "from", from);
        field(mapping, TileTypeChange.class, "to", to);
        ti.setTileTypeChanges(Collections.singletonMap(from, mapping));
        return ti;
    }

    private TileImprovementType unattendedChange(int currentFood, int currentTools,
                                                 boolean consuming, int net, int futureTools) throws Exception {
        TileType old = terrain("cftOld", 2, 3, 2);
        TileType next = terrain("cftNew", 5, 6, futureTools);
        work.setType(old);
        ct.setProductionType(production(true, 3, 2));
        colony.observed = new ProductionInfo();
        if (currentFood != 0) colony.observed.addProduction(new AbstractGoods(goods("food"), currentFood));
        if (currentTools != 0) colony.observed.addProduction(new AbstractGoods(goods("tools"), currentTools));
        colony.consuming = consuming;
        colony.net = net;
        return change(old, next);
    }

    private String xml(ColonyTile value) throws Exception {
        StringWriter out = new StringWriter();
        try (FreeColXMLWriter writer = new FreeColXMLWriter(out,
                FreeColXMLWriter.WriteScope.toSave(), false)) {
            value.toXML(writer);
            writer.flush();
        }
        return out.toString();
    }

    public void testTC001() throws Exception {
        ColonyTile fresh = new ColonyTile(game, "colonyTile:fresh");
        assertEquals("colonyTile:fresh", fresh.getId());
        assertSame(null, fresh.getWorkTile());
        assertTrue(fresh.toString().contains("NO-COLONY"));
        assertEquals("colonyTile", fresh.getXMLTagName());
    }

    public void testTC002() throws Exception {
        ColonyTile fresh = new ColonyTile(game, colony, work);
        assertSame(colony, fresh.getColony());
        assertSame(work, fresh.getWorkTile());
        assertFalse(fresh.isColonyCenterTile());
    }

    public void testTC003() throws Exception {
        assertSame(work, ct.getWorkTile());
        assertFalse(ct.isColonyCenterTile());
        ct.setWorkTile(center);
        assertSame(center, ct.getWorkTile());
        assertTrue(ct.isColonyCenterTile());
        assertSame(colony, ct.up());
        assertEquals(0, ct.getLevel());
        assertEquals("1.0", Float.toString(ct.getCompetenceFactor()));
        assertEquals("1.0", Float.toString(ct.getRebelFactor()));
    }

    public void testTC004() throws Exception {
        assertEquals(1, ct.getUnitCapacity());
        assertFalse(ct.canAutoProduce());
        ct.setWorkTile(center);
        assertEquals(0, ct.getUnitCapacity());
        assertTrue(ct.canAutoProduce());
    }

    public void testTC005() throws Exception {
        ct.setWorkTile(null);
        assertSame(null, ct.getLocationLabel());
        assertSame(null, ct.getLabel());
    }

    public void testTC006() throws Exception {
        assertEquals("model.tile.nameLocation", ct.getLocationLabel().getId());
        assertEquals("model.tile.plains.name", ct.getLabel().getId());
        assertEquals("CFT-plains-E", ct.toShortString());
        assertTrue(ct.toString().contains("/CFT]"));
    }

    public void testTC007() throws Exception {
        assertSame(null, ct.getOccupyingUnit());
        assertFalse(ct.isOccupied());
    }

    public void testTC008() throws Exception {
        enemy.setStance(player, Stance.WAR);
        Unit invader = new ServerUnit(game, work, enemy, spec().getUnitType("model.unit.veteranSoldier"));
        assertSame(invader, ct.getOccupyingUnit());
        assertTrue(ct.isOccupied());
    }

    public void testTC009() throws Exception {
        assertEquals("WRONG_TYPE", ct.getNoAddReason(null).name());
    }

    public void testTC010() throws Exception {
        assertEquals("NONE", ct.getNoAddReason(worker()).name());
    }

    public void testTC011() throws Exception {
        ct.setWorkTile(center);
        assertEquals("COLONY_CENTER", ct.getNoWorkReason().name());
    }

    public void testTC012() throws Exception {
        work.setType(spec().getTileType("model.tile.ocean"));
        assertEquals("MISSING_ABILITY", ct.getNoWorkReason().name());
    }

    public void testTC013() throws Exception {
        assertEquals("NONE", ct.getNoWorkReason().name());
    }

    public void testTC014() throws Exception {
        work.setOwningSettlement(null);
        player.claim = Player.NoClaimReason.NONE;
        assertEquals("CLAIM_REQUIRED", ct.getNoWorkReason().name());
    }

    public void testTC015() throws Exception {
        work.setOwningSettlement(null);
        player.claim = Player.NoClaimReason.TERRAIN;
        assertEquals("MISSING_ABILITY", ct.getNoWorkReason().name());
    }

    public void testTC016() throws Exception {
        work.setOwningSettlement(null);
        player.claim = Player.NoClaimReason.RUMOUR;
        assertEquals("MISSING_ABILITY", ct.getNoWorkReason().name());
    }

    public void testTC017() throws Exception {
        work.setOwningSettlement(null);
        player.claim = Player.NoClaimReason.WATER;
        assertEquals("MISSING_ABILITY", ct.getNoWorkReason().name());
    }

    public void testTC018() throws Exception {
        work.setOwningSettlement(null);
        player.claim = Player.NoClaimReason.OCCUPIED;
        assertEquals("OCCUPIED_BY_ENEMY", ct.getNoWorkReason().name());
    }

    public void testTC019() throws Exception {
        work.setOwningSettlement(null);
        player.claim = Player.NoClaimReason.WORKED;
        assertEquals("ANOTHER_COLONY", ct.getNoWorkReason().name());
    }

    public void testTC020() throws Exception {
        work.setOwningSettlement(null);
        player.claim = Player.NoClaimReason.EUROPEANS;
        assertEquals("OWNED_BY_ENEMY", ct.getNoWorkReason().name());
    }

    public void testTC021() throws Exception {
        work.setOwningSettlement(null);
        player.claim = Player.NoClaimReason.NATIVES;
        assertEquals("CLAIM_REQUIRED", ct.getNoWorkReason().name());
    }

    public void testTC022() throws Exception {
        work.setOwningSettlement(null);
        Colony other = new Colony(game, "colony:cftOther");
        other.setOwner(player);
        work.setSettlement(other);
        player.claim = Player.NoClaimReason.SETTLEMENT;
        assertEquals("ANOTHER_COLONY", ct.getNoWorkReason().name());
    }

    public void testTC023() throws Exception {
        work.setOwningSettlement(null);
        Colony other = new Colony(game, "colony:cftOther");
        other.setOwner(enemy);
        work.setSettlement(other);
        player.claim = Player.NoClaimReason.SETTLEMENT;
        assertEquals("OWNED_BY_ENEMY", ct.getNoWorkReason().name());
    }

    public void testTC024() throws Exception {
        work.setOwningSettlement(null);
        player.claim = null;
        assertEquals("WRONG_TYPE", ct.getNoWorkReason().name());
    }

    public void testTC025() throws Exception {
        assertTrue(ct.goodSuggestionCheck(null, null, goods("food")));
        assertTrue(ct.isCurrent());
        assertTrue(ct.isAvailable());
    }

    public void testTC026() throws Exception {
        work.setOwningSettlement(null);
        work.setOwner(null);
        player.claim = Player.NoClaimReason.NONE;
        assertTrue(ct.goodSuggestionCheck(null, null, goods("food")));
        assertFalse(ct.isCurrent());
        assertTrue(ct.isAvailable());
    }

    public void testTC027() throws Exception {
        work.setOwningSettlement(null);
        work.setOwner(enemy);
        player.claim = Player.NoClaimReason.EUROPEANS;
        assertFalse(ct.goodSuggestionCheck(null, null, goods("food")));
        assertFalse(ct.isCurrent());
        assertFalse(ct.isAvailable());
    }

    public void testTC028() throws Exception {
        assertEquals("model.colonyTile.claim", ct.getClaimTemplate().getId());
        ct.setWorkTile(center);
        assertEquals("", ct.getClaimTemplate().getId());
    }

    public void testTC029() throws Exception {
        ct.setWorkTile(null);
        assertFalse(ct.canProduce(goods("food"), spec().getUnitType("model.unit.freeColonist")));
        assertEquals(0, ct.getBaseProduction(null, goods("food"), null));
    }

    public void testTC030() throws Exception {
        TileType type = terrain("cftProduction", 2, 3, 2);
        work.setType(type);
        assertTrue(ct.canProduce(goods("food"), spec().getUnitType("model.unit.freeColonist")));
        assertFalse(ct.canProduce(goods("lumber"), spec().getUnitType("model.unit.freeColonist")));
        assertEquals(2, ct.getBaseProduction(null, goods("food"), spec().getUnitType("model.unit.freeColonist")));
    }

    public void testTC031() throws Exception {
        ct.setWorkTile(null);
        assertTrue(ct.getAvailableProductionTypes(false).isEmpty());
        ct.setWorkTile(new Tile(game, "tile:cftUnknown"));
        assertTrue(ct.getAvailableProductionTypes(false).isEmpty());
    }

    public void testTC032() throws Exception {
        TileType type = terrain("cftProduction", 2, 3, 2);
        work.setType(type);
        center.setType(type);
        assertEquals(1, ct.getAvailableProductionTypes(false).size());
        assertTrue(ct.getAvailableProductionTypes(true).isEmpty());
        ct.setWorkTile(center);
        assertEquals(1, ct.getAvailableProductionTypes(true).size());
        assertTrue(ct.getAvailableProductionTypes(false).isEmpty());
    }

    public void testTC033() throws Exception {
        assertTrue(ct.getBasicProductionInfo().getProduction().isEmpty());
    }

    public void testTC034() throws Exception {
        TileType type = terrain("cftProduction", 2, 3, 2);
        work.setType(type);
        ct.setProductionType(production(false, 2, 0));
        put(worker());
        assertEquals(2, AbstractGoods.getCount(goods("food"), ct.getBasicProductionInfo().getProduction()));
    }

    public void testTC035() throws Exception {
        TileType type = terrain("cftProduction", 2, 3, 2);
        center.setType(type);
        ct.setWorkTile(center);
        ct.setProductionType(production(true, 3, 2));
        ProductionInfo pi = ct.getBasicProductionInfo();
        assertEquals(3, AbstractGoods.getCount(goods("food"), pi.getProduction()));
        assertEquals(2, AbstractGoods.getCount(goods("tools"), pi.getProduction()));
    }

    public void testTC036() throws Exception {
        work.setType(terrain("cftProduction", 2, 3, 2));
        assertEquals(0, (int)ct.getProductionModifiers(goods("food"), spec().getUnitType("model.unit.freeColonist")).count());
    }

    public void testTC037() throws Exception {
        assertEquals(0, ct.evaluateFor(player));
        colony.observed.addProduction(new AbstractGoods(goods("food"), 3));
        assertEquals(6, ct.evaluateFor(player));
    }

    public void testTC038() throws Exception {
        assertSame(center.getType(), ct.getDisplayObject());
    }

    public void testTC039() throws Exception {
        TileImprovementType ti = improvement();
        ct.setWorkTile(null);
        assertEquals(0, ct.improvedBy(ti));
    }

    public void testTC040() throws Exception {
        work.setOwningSettlement(null);
        assertEquals(0, ct.improvedBy(improvement()));
    }

    public void testTC041() throws Exception {
        TileImprovementType ti = improvement();
        TileImprovement installed = new TileImprovement(game, work, ti, null);
        installed.setTurnsToComplete(0);
        work.setTileItemContainer(new TileItemContainer(game, work));
        work.getTileItemContainer().tryAddTileItem(installed);
        assertEquals(0, ct.improvedBy(ti));
    }

    public void testTC042() throws Exception {
        TileImprovementType ti = improvement();
        Scope scope = new Scope();
        scope.setType("model.tile.ocean");
        ti.addScope(scope);
        assertEquals(0, ct.improvedBy(ti));
    }

    public void testTC043() throws Exception {
        assertEquals(0, ct.improvedBy(improvement()));
    }

    public void testTC044() throws Exception {
        ct.setProductionType(production(true, 3, 0));
        assertEquals(1, ct.improvedBy(improvement()));
    }

    public void testTC045() throws Exception {
        TileImprovementType ti = unattendedChange(0, 0, false, 0, 2);
        assertEquals(6, ct.improvedBy(ti));
    }

    public void testTC046() throws Exception {
        TileImprovementType ti = unattendedChange(3, 0, false, 0, 2);
        assertEquals(3, ct.improvedBy(ti));
    }

    public void testTC047() throws Exception {
        TileImprovementType ti = unattendedChange(0, 5, false, 0, 2);
        assertEquals(6, ct.improvedBy(ti));
    }

    public void testTC048() throws Exception {
        TileImprovementType ti = unattendedChange(0, 5, true, 1, 2);
        assertEquals(-3, ct.improvedBy(ti));
    }

    public void testTC049() throws Exception {
        TileImprovementType ti = unattendedChange(0, 5, true, 10, 2);
        assertEquals(6, ct.improvedBy(ti));
    }

    public void testTC050() throws Exception {
        TileImprovementType ti = unattendedChange(0, 2, true, 0, 5);
        assertEquals(6, ct.improvedBy(ti));
    }

    public void testTC051() throws Exception {
        ct.setProductionType(production(false, 2, 0));
        assertEquals(0, ct.improvedBy(improvement()));
    }

    public void testTC052() throws Exception {
        ct.setProductionType(production(false, 2, 0));
        Unit u = worker();
        u.changeWorkType(null);
        put(u);
        assertEquals(0, ct.improvedBy(improvement()));
    }

    public void testTC053() throws Exception {
        ct.setProductionType(production(false, 2, 0));
        Unit u = worker();
        u.changeWorkType(goods("food"));
        put(u);
        assertEquals(1, ct.improvedBy(improvement()));
    }

    public void testTC054() throws Exception {
        TileType old = terrain("cftOld", 2, 3, 2);
        TileType next = terrain("cftNew", 5, 6, 2);
        work.setType(old);
        ct.setProductionType(production(false, 2, 0));
        Unit u = worker();
        u.changeWorkType(goods("food"));
        put(u);
        assertEquals(3, ct.improvedBy(change(old, next)));
    }

    public void testTC055() throws Exception {
        TileType old = terrain("cftOld", 2, 3, 2);
        TileType next = terrain("cftNew", 5, 6, 2);
        work.setType(old);
        ResourceType resourceType = new ResourceType("model.resource.cft", spec());
        resourceType.addModifier(new Modifier(goods("food").getId(), 2, Modifier.ModifierType.ADDITIVE));
        work.addResource(new Resource(game, work, resourceType, 100));
        ct.setProductionType(production(false, 2, 0));
        Unit u = worker();
        u.changeWorkType(goods("food"));
        put(u);
        assertEquals(1, ct.improvedBy(change(old, next)));
    }

    public void testTC056() throws Exception {
        assertFalse(ct.copyIn((FreeColObject)null));
        Goods wrong = new Goods(game, null, goods("food"), 25);
        wrong.setId(ct.getId());
        assertFalse(ct.copyIn(wrong));
    }

    public void testTC057() throws Exception {
        ColonyTile target = new ColonyTile(game, (String)null);
        target.setId(ct.getId());
        assertTrue(target.copyIn(ct));
        assertSame(colony, target.getColony());
        assertSame(work, target.getWorkTile());
    }

    public void testTC058() throws Exception {
        String text = xml(ct);
        assertTrue(text.contains("workTile=\"" + work.getId() + "\""));
        ct.setWorkTile(center);
        try (FreeColXMLReader reader = new FreeColXMLReader(new StringReader(text))) {
            reader.nextTag();
            ct.readFromXML(reader);
        }
        assertSame(work, ct.getWorkTile());
        assertSame(colony, ct.getColony());
    }

    public void testTC059() throws Exception {
        String text = "<colonyTile id=\"" + ct.getId() + "\" colony=\"" + colony.getId() + "\"/>";
        try (FreeColXMLReader reader = new FreeColXMLReader(new StringReader(text))) {
            reader.nextTag();
            try {
                ct.readAttributes(reader);
                fail("Expected XMLStreamException");
            } catch (XMLStreamException e) {
                assertNotNull(e.getMessage());
            }
        }
    }

    public void testTC060() throws Exception {
        work.setType(spec().getTileType("model.tile.ocean"));
        colony.addAbility(new Ability(Ability.PRODUCE_IN_WATER, true));
        assertEquals("NONE", ct.getNoWorkReason().name());
    }
}
