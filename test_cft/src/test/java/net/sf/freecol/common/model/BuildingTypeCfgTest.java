package net.sf.freecol.common.model;

import java.io.StringReader;
import java.io.StringWriter;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import javax.swing.JList;
import net.sf.freecol.common.io.FreeColXMLReader;
import net.sf.freecol.common.io.FreeColXMLWriter;
import net.sf.freecol.server.model.ServerBuilding;
import net.sf.freecol.util.test.FreeColTestCase;

public class BuildingTypeCfgTest extends FreeColTestCase {
    private Game game;
    private Specification local;
    private BuildingType a;
    private BuildingType b;
    private BuildingType c;
    private Colony colony;

    protected void setUp() throws Exception {
        super.setUp();
        game = getStandardGame();
        local = new Specification();
        a = local.findType("model.building.cftA", BuildingType.class);
        b = local.findType("model.building.cftB", BuildingType.class);
        c = local.findType("model.building.cftC", BuildingType.class);
        colony = new Colony(game, "colony:cftBuilding");
        colony.setOwner(game.getPlayerByNationId("model.nation.dutch"));
        colony.setName("CFT");
    }

    private void field(Object target, Class<?> owner, String name, Object value) throws Exception {
        Field f = owner.getDeclaredField(name);
        f.setAccessible(true);
        f.set(target, value);
    }

    private void value(BuildingType target, String name, Object value) throws Exception {
        field(target, BuildingType.class, name, value);
    }

    private void link(BuildingType lower, BuildingType upper) throws Exception {
        value(lower, "upgradesTo", upper);
        value(upper, "upgradesFrom", lower);
        value(upper, "level", lower.getLevel() + 1);
    }

    private void buildable(BuildingType type) throws Exception {
        type.setRequiredPopulation(0);
        field(type, BuildableType.class, "requiredGoods", new ArrayList<AbstractGoods>(
                Arrays.asList(new AbstractGoods(spec().getGoodsType("model.goods.hammers"), 20))));
    }

    private void existing(BuildingType type) {
        colony.addBuilding(new ServerBuilding(game, colony, type));
    }

    private JList<BuildableType> queue(BuildableType... values) {
        return new JList<BuildableType>(values);
    }

    private UnitType worker(int skill) {
        UnitType result = new UnitType("model.unit.cftWorker", spec());
        result.setSkill(skill);
        return result;
    }

    private GoodsType goods(String name) {
        return spec().getGoodsType("model.goods." + name);
    }

    private ProductionType production(boolean unattended, String level, int amount) {
        return new ProductionType(new AbstractGoods(goods("tools"), amount), unattended, level);
    }

    private void read(BuildingType target, String attributes) throws Exception {
        String text = "<building-type id=\"" + target.getId() + "\" " + attributes + "/>";
        try (FreeColXMLReader reader = new FreeColXMLReader(new StringReader(text))) {
            reader.nextTag();
            target.readFromXML(reader);
        }
    }

    private void child(BuildingType target, String text) throws Exception {
        try (FreeColXMLReader reader = new FreeColXMLReader(new StringReader(text))) {
            reader.nextTag();
            target.readChild(reader);
        }
    }

    private String xml(BuildingType type) throws Exception {
        StringWriter out = new StringWriter();
        try (FreeColXMLWriter writer = new FreeColXMLWriter(out,
                FreeColXMLWriter.WriteScope.toSave(), false)) {
            type.toXML(writer);
            writer.flush();
        }
        return out.toString();
    }

    public void testTC001() throws Exception {
        BuildingType fresh = new BuildingType("model.building.fresh", local);
        assertEquals("model.building.fresh", fresh.getId());
        assertEquals(1, fresh.getLevel());
        assertEquals(3, fresh.getWorkPlaces());
        assertEquals(Constants.UNDEFINED, fresh.getMinimumSkill());
        assertEquals(Constants.INFINITY, fresh.getMaximumSkill());
        assertEquals(0, fresh.getUpkeep());
        assertEquals(0, fresh.getExpertConnectionProduction());
        assertEquals("1.0", Float.toString(fresh.getCompetenceFactor()));
        assertEquals("1.0", Float.toString(fresh.getRebelFactor()));
        assertEquals(Consumer.BUILDING_PRIORITY, fresh.getPriority());
        assertSame(fresh, fresh.getType());
        assertSame(null, fresh.getUpgradesFrom());
        assertSame(null, fresh.getUpgradesTo());
        assertTrue(fresh.getProductionTypes().isEmpty());
        assertEquals("building-type", fresh.getXMLTagName());
    }

    public void testTC002() throws Exception {
        value(a, "workPlaces", 0);
        UnitType u = worker(3);
        assertEquals("CAPACITY_EXCEEDED", a.getNoAddReason(u).name());
        assertFalse(a.canAdd(u));
    }

    public void testTC003() throws Exception {
        UnitType u = new UnitType("model.unit.noSkill", local);
        assertEquals("MISSING_SKILL", a.getNoAddReason(u).name());
        assertFalse(a.canAdd(u));
    }

    public void testTC004() throws Exception {
        value(a, "minSkill", 2);
        UnitType u = worker(0);
        assertEquals("MINIMUM_SKILL", a.getNoAddReason(u).name());
        assertFalse(a.canAdd(u));
    }

    public void testTC005() throws Exception {
        value(a, "maxSkill", 4);
        UnitType u = worker(6);
        assertEquals("MAXIMUM_SKILL", a.getNoAddReason(u).name());
        assertFalse(a.canAdd(u));
    }

    public void testTC006() throws Exception {
        value(a, "minSkill", 1);
        value(a, "maxSkill", 5);
        UnitType u = worker(3);
        assertEquals("NONE", a.getNoAddReason(u).name());
        assertTrue(a.canAdd(u));
    }

    public void testTC007() throws Exception {
        assertSame(a, a.getFirstLevel());
    }

    public void testTC008() throws Exception {
        link(a, b);
        link(b, c);
        assertSame(a, c.getFirstLevel());
        assertSame(a, b.getUpgradesFrom());
        assertSame(b, a.getUpgradesTo());
        assertEquals(3, c.getLevel());
    }

    public void testTC009() throws Exception {
        assertTrue(a.isAutomaticBuild());
        buildable(a);
        assertFalse(a.isAutomaticBuild());
        link(a, b);
        assertFalse(b.isAutomaticBuild());
    }

    public void testTC010() throws Exception {
        a.addProductionType(null);
        assertEquals(0, a.getProductionTypes().size());
        ProductionType pt = production(false, null, 3);
        a.addProductionType(pt);
        assertEquals(1, a.getProductionTypes().size());
        assertSame(pt, a.getProductionTypes().get(0));
    }

    public void testTC011() throws Exception {
        a.addProductionType(production(false, null, 2));
        ProductionType pt = production(true, "easy", 5);
        a.setProductionTypes(Arrays.asList(pt));
        assertEquals(1, a.getProductionTypes().size());
        assertSame(pt, a.getProductionTypes().get(0));
    }

    public void testTC012() throws Exception {
        ProductionType attended = production(false, "easy", 3);
        ProductionType unattended = production(true, "hard", 5);
        a.addProductionType(attended);
        a.addProductionType(unattended);
        assertEquals(1, a.getAvailableProductionTypes(false).size());
        assertEquals(1, a.getAvailableProductionTypes(true).size());
        assertEquals(1, a.getAvailableProductionTypes(false, "easy").size());
        assertEquals(0, a.getAvailableProductionTypes(false, "hard").size());
        assertSame(unattended, a.getAvailableProductionTypes(true, "hard").get(0));
    }

    public void testTC013() throws Exception {
        assertSame(null, a.getProducedGoodsType());
    }

    public void testTC014() throws Exception {
        a.addProductionType(new ProductionType(local));
        assertSame(null, a.getProducedGoodsType());
    }

    public void testTC015() throws Exception {
        a.addProductionType(production(false, null, 3));
        assertSame(goods("tools"), a.getProducedGoodsType());
    }

    public void testTC016() throws Exception {
        assertFalse(a.isDefenceType());
        a.addModifier(new Modifier(Modifier.DEFENCE, 25, Modifier.ModifierType.ADDITIVE));
        assertTrue(a.isDefenceType());
    }

    public void testTC017() throws Exception {
        a.addProductionType(production(false, null, 3));
        assertFalse(a.canProduce(null, worker(3)));
        assertTrue(a.canProduce(goods("tools"), worker(3)));
        assertFalse(a.canProduce(goods("food"), worker(3)));
        assertFalse(a.canProduce(goods("tools"), null));
        a.addProductionType(production(true, null, 2));
        assertTrue(a.canProduce(goods("tools"), null));
    }

    public void testTC018() throws Exception {
        BuildingType type = new BuildingType("model.building.cftProduction", spec());
        ProductionType pt = new ProductionType(Arrays.asList(new AbstractGoods(goods("ore"), 3)), Arrays.asList(new AbstractGoods(goods("food"), 2), new AbstractGoods(goods("tools"), 3)));
        type.addProductionType(pt);
        assertSame(null, type.getGame());
        assertEquals(3, type.getPotentialProduction(goods("tools"), spec().getUnitType("model.unit.freeColonist")));
    }

    public void testTC019() throws Exception {
        BuildingType type = new BuildingType("model.building.cftProduction", spec());
        ProductionType pt = new ProductionType(Arrays.asList(new AbstractGoods(goods("ore"), 3)), Arrays.asList(new AbstractGoods(goods("food"), 2), new AbstractGoods(goods("tools"), 3)));
        type.addProductionType(pt);
        assertSame(null, type.getGame());
        assertEquals(0, type.getPotentialProduction(goods("lumber"), spec().getUnitType("model.unit.freeColonist")));
    }

    public void testTC020() throws Exception {
        assertEquals("NONE", a.canBeBuiltInColony(colony, Collections.<BuildableType>emptyList()).name());
    }

    public void testTC021() throws Exception {
        link(a, b);
        assertEquals("WRONG_UPGRADE", b.canBeBuiltInColony(colony, Collections.<BuildableType>emptyList()).name());
    }

    public void testTC022() throws Exception {
        link(a, b);
        assertEquals("NONE", b.canBeBuiltInColony(colony, Arrays.<BuildableType>asList(a)).name());
    }

    public void testTC023() throws Exception {
        link(a, b);
        existing(a);
        assertEquals("NONE", b.canBeBuiltInColony(colony, Collections.<BuildableType>emptyList()).name());
    }

    public void testTC024() throws Exception {
        link(a, b);
        link(b, c);
        existing(a);
        assertEquals("WRONG_UPGRADE", c.canBeBuiltInColony(colony, Collections.<BuildableType>emptyList()).name());
    }

    public void testTC025() throws Exception {
        link(a, b);
        link(b, c);
        existing(a);
        assertEquals("NONE", c.canBeBuiltInColony(colony, Arrays.<BuildableType>asList(b)).name());
    }

    public void testTC026() throws Exception {
        assertEquals(0, a.getMinimumIndex(colony, queue(), -1));
    }

    public void testTC027() throws Exception {
        link(a, b);
        assertEquals(-1, b.getMinimumIndex(colony, queue(), -1));
    }

    public void testTC028() throws Exception {
        link(a, b);
        assertEquals(2, b.getMinimumIndex(colony, queue(spec().getUnitType("model.unit.freeColonist"), a), -1));
    }

    public void testTC029() throws Exception {
        link(a, b);
        existing(a);
        assertEquals(0, b.getMinimumIndex(colony, queue(), -1));
    }

    public void testTC030() throws Exception {
        link(a, b);
        link(b, c);
        existing(c);
        assertEquals(-1, b.getMinimumIndex(colony, queue(spec().getUnitType("model.unit.freeColonist")), -1));
    }

    public void testTC031() throws Exception {
        assertEquals(-1, a.getMaximumIndex(colony, queue(), -1));
    }

    public void testTC032() throws Exception {
        buildable(a);
        assertEquals(1, a.getMaximumIndex(colony, queue(spec().getUnitType("model.unit.freeColonist")), -1));
    }

    public void testTC033() throws Exception {
        link(a, b);
        buildable(a);
        assertEquals(0, a.getMaximumIndex(colony, queue(), -1));
    }

    public void testTC034() throws Exception {
        link(a, b);
        buildable(a);
        assertEquals(2, a.getMaximumIndex(colony, queue(a, spec().getUnitType("model.unit.freeColonist"), b), -1));
    }

    public void testTC035() throws Exception {
        link(a, b);
        link(b, c);
        buildable(b);
        assertEquals(3, b.getMaximumIndex(colony, queue(spec().getUnitType("model.unit.freeColonist"), b, a, c), -1));
    }

    public void testTC036() throws Exception {
        link(a, b);
        link(b, c);
        buildable(c);
        assertEquals(1, c.getMaximumIndex(colony, queue(b), -1));
    }

    public void testTC037() throws Exception {
        link(a, b);
        link(b, c);
        buildable(b);
        assertEquals(1, b.getMaximumIndex(colony, queue(a), -1));
    }

    public void testTC038() throws Exception {
        link(a, b);
        link(b, c);
        buildable(b);
        existing(a);
        assertEquals(0, b.getMaximumIndex(colony, queue(c), -1));
    }

    public void testTC039() throws Exception {
        link(a, b);
        buildable(a);
        Ability ability = new Ability(Ability.BUILD, a, true);
        Scope scope = new Scope();
        scope.setType("model.unit.freeColonist");
        ability.addScope(scope);
        a.addAbility(ability);
        assertEquals(0, a.getMaximumIndex(colony, queue(spec().getUnitType("model.unit.freeColonist")), -1));
    }

    public void testTC040() throws Exception {
        UnitType u = worker(3);
        Modifier additive = new Modifier("cft.production", 4, Modifier.ModifierType.ADDITIVE);
        u.addModifier(additive);
        List<Modifier> modifiers = a.getCompetenceModifiers("cft.production", u, new Turn(1)).collect(Collectors.toList());
        assertEquals(1, modifiers.size());
        assertSame(additive, modifiers.get(0));
    }

    public void testTC041() throws Exception {
        value(a, "competenceFactor", 0.5f);
        UnitType u = worker(3);
        Modifier additive = new Modifier("cft.production", 4, Modifier.ModifierType.ADDITIVE);
        Modifier multiplicative = new Modifier("cft.production", 3, Modifier.ModifierType.MULTIPLICATIVE);
        u.addModifier(additive);
        u.addModifier(multiplicative);
        List<Modifier> modifiers = a.getCompetenceModifiers("cft.production", u, new Turn(1)).collect(Collectors.toList());
        assertEquals(2, modifiers.size());
        assertTrue(modifiers.stream().anyMatch(m -> m.getType() == Modifier.ModifierType.ADDITIVE && m.getValue() == 2.0f && m != additive));
        assertTrue(modifiers.stream().anyMatch(m -> m == multiplicative && m.getValue() == 3.0f));
        assertEquals("4.0", Float.toString(additive.getValue()));
    }

    public void testTC042() throws Exception {
        a.setIndex(2);
        b.setIndex(5);
        assertEquals(-3, a.compareTo(b));
        assertEquals(3, b.compareTo(a));
    }

    public void testTC043() throws Exception {
        a.setIndex(2);
        b.setIndex(2);
        assertEquals(-1, Integer.signum(a.compareTo(b)));
        assertEquals(0, a.compareTo(a));
        UnitType u = new UnitType("model.unit.cft", local);
        assertEquals(-1, Integer.signum(a.compareTo(u)));
    }

    public void testTC044() throws Exception {
        assertFalse(a.copyIn((FreeColObject)null));
        UnitType wrong = new UnitType(a.getId(), local);
        assertFalse(a.copyIn(wrong));
    }

    public void testTC045() throws Exception {
        value(a, "workPlaces", 5);
        value(a, "upkeep", 4);
        value(a, "competenceFactor", 0.5f);
        link(b, a);
        a.addProductionType(production(false, null, 3));
        BuildingType target = new BuildingType(a.getId(), local);
        target.getFeatureContainer();
        buildable(target);
        assertTrue(target.copyIn(a));
        assertEquals(5, target.getWorkPlaces());
        assertEquals(4, target.getUpkeep());
        assertEquals("0.5", Float.toString(target.getCompetenceFactor()));
        assertSame(b, target.getUpgradesFrom());
        assertEquals(1, target.getProductionTypes().size());
    }

    public void testTC046() throws Exception {
        read(a, "");
        String text = xml(a);
        assertEquals(1, a.getLevel());
        assertEquals(3, a.getWorkPlaces());
        assertFalse(text.contains("upgrades-from="));
        assertFalse(text.contains("minimum-skill="));
        assertFalse(text.contains("maximum-skill="));
        assertFalse(text.contains("upkeep="));
        assertFalse(text.contains("priority="));
        assertFalse(text.contains("<production"));
    }

    public void testTC047() throws Exception {
        read(a, "workplaces=\"5\" minimum-skill=\"2\" maximum-skill=\"7\" upkeep=\"4\" priority=\"900\" experts-with-connections-production=\"6\" competence-factor=\"0.5\" rebel-factor=\"1.5\"");
        String text = xml(a);
        assertEquals(5, a.getWorkPlaces());
        assertEquals(2, a.getMinimumSkill());
        assertEquals(7, a.getMaximumSkill());
        assertEquals(4, a.getUpkeep());
        assertEquals(900, a.getPriority());
        assertEquals(6, a.getExpertConnectionProduction());
        assertEquals("0.5", Float.toString(a.getCompetenceFactor()));
        assertEquals("1.5", Float.toString(a.getRebelFactor()));
        assertTrue(text.contains("minimum-skill=\"2\""));
        assertTrue(text.contains("maximum-skill=\"7\""));
        assertTrue(text.contains("upkeep=\"4\""));
        assertTrue(text.contains("priority=\"900\""));
    }

    public void testTC048() throws Exception {
        read(b, "upgradesFrom=\"" + a.getId() + "\" minSkill=\"1\" maxSkill=\"5\"");
        assertSame(a, b.getUpgradesFrom());
        assertSame(b, a.getUpgradesTo());
        assertEquals(2, b.getLevel());
        assertEquals(1, b.getMinimumSkill());
        assertEquals(5, b.getMaximumSkill());
        assertTrue(xml(b).contains("upgrades-from=\"" + a.getId() + "\""));
    }

    public void testTC049() throws Exception {
        read(b, "upgrades-from=\"" + a.getId() + "\"");
        read(c, "upgrades-from=\"" + b.getId() + "\"");
        assertEquals(3, c.getLevel());
        assertSame(a, c.getFirstLevel());
    }

    public void testTC050() throws Exception {
        read(a, "abstract=\"true\" workplaces=\"5\" required-population=\"4\"");
        a.addAbility(new Ability("cft.parentAbility", a, true));
        read(b, "extends=\"" + a.getId() + "\"");
        assertEquals(5, b.getWorkPlaces());
        assertEquals(4, b.getRequiredPopulation());
        assertTrue(b.hasAbility("cft.parentAbility"));
        assertTrue(b.getAbilities("cft.parentAbility").allMatch(x -> x.getSource() == b));
    }

    public void testTC051() throws Exception {
        a.setRequiredPopulation(4);
        read(b, "extends=\"" + a.getId() + "\" required-population=\"2\"");
        assertEquals(2, b.getRequiredPopulation());
    }

    public void testTC052() throws Exception {
        local.findType("model.goods.tools", GoodsType.class);
        child(a, "<production><output goods-type=\"model.goods.tools\" value=\"3\"/></production>");
        assertEquals(1, a.getProductionTypes().size());
        assertEquals("model.goods.tools", a.getProducedGoodsType().getId());
        assertTrue(xml(a).contains("<production"));
    }

    public void testTC053() throws Exception {
        a.addProductionType(production(false, null, 3));
        child(a, "<production delete=\"true\"/>");
        assertTrue(a.getProductionTypes().isEmpty());
    }

    public void testTC054() throws Exception {
        child(a, "<ability id=\"" + Ability.EXPERTS_USE_CONNECTIONS + "\" value=\"true\"/>");
        assertTrue(a.hasAbility(Ability.EXPERTS_USE_CONNECTIONS));
        assertEquals(4, a.getExpertConnectionProduction());
        child(a, "<production/>");
        assertEquals(4, a.getExpertConnectionProduction());
    }

    public void testTC055() throws Exception {
        BuildingType type = new BuildingType("model.building.cftUnattended", spec());
        ProductionType pt = production(true, null, 3);
        type.addProductionType(pt);
        assertEquals(6, type.getPotentialProduction(goods("tools"), null));
    }
}
