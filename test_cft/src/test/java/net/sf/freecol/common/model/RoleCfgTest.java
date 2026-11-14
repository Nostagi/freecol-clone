package net.sf.freecol.common.model;

import java.io.StringReader;
import java.io.StringWriter;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import javax.xml.stream.XMLStreamException;
import junit.framework.TestCase;
import net.sf.freecol.common.io.FreeColXMLReader;
import net.sf.freecol.common.io.FreeColXMLWriter;
import net.sf.freecol.common.model.Modifier.ModifierType;
import net.sf.freecol.server.model.ServerGame;

public class RoleCfgTest extends TestCase {
    private Specification local;
    private Role a;
    private Role b;
    private Role defaultRole;
    private GoodsType g1;
    private GoodsType g2;
    private GoodsType g3;
    private UnitType unit;

    protected void setUp() throws Exception {
        super.setUp();
        local = new Specification();
        a = local.findType("model.role.a", Role.class);
        b = local.findType("model.role.b", Role.class);
        defaultRole = local.findType(Specification.DEFAULT_ROLE_ID, Role.class);
        g1 = local.findType("model.goods.cftOne", GoodsType.class);
        g2 = local.findType("model.goods.cftTwo", GoodsType.class);
        g3 = local.findType("model.goods.cftThree", GoodsType.class);
        unit = local.findType("model.unit.cftExpert", UnitType.class);
    }

    private void goods(Role role, AbstractGoods... values) throws Exception {
        Field field = BuildableType.class.getDeclaredField("requiredGoods");
        field.setAccessible(true);
        field.set(role, new ArrayList<AbstractGoods>(Arrays.asList(values)));
    }

    private int count(List<AbstractGoods> values, GoodsType type) {
        return AbstractGoods.getCount(type, values);
    }

    private String xml(Role role) throws Exception {
        StringWriter out = new StringWriter();
        try (FreeColXMLWriter writer = new FreeColXMLWriter(out,
                FreeColXMLWriter.WriteScope.toSave(), false)) {
            role.toXML(writer);
            writer.flush();
        }
        return out.toString();
    }

    private void read(Role role, String text) throws Exception {
        try (FreeColXMLReader reader = new FreeColXMLReader(new StringReader(text))) {
            reader.nextTag();
            role.readFromXML(reader);
        }
    }

    private void child(Role role, String text) throws Exception {
        try (FreeColXMLReader reader = new FreeColXMLReader(new StringReader(text))) {
            reader.nextTag();
            role.readChild(reader);
        }
    }

    private void strength(Role role, float offence, float defence) {
        role.addModifier(new Modifier(Modifier.OFFENCE, offence, ModifierType.ADDITIVE));
        role.addModifier(new Modifier(Modifier.DEFENCE, defence, ModifierType.ADDITIVE));
    }

    public void testTC001() throws Exception {
        Role r = new Role("model.role.fresh", local);
        assertEquals("model.role.fresh", r.getId());
        assertSame(local, r.getSpecification());
        assertSame(null, r.getDowngrade());
        assertSame(null, r.getExpertUnit());
        assertEquals(1, r.getMaximumCount());
        assertEquals("role", r.getXMLTagName());
    }

    public void testTC002() throws Exception {
        assertTrue(Role.isDefaultRoleId(Specification.DEFAULT_ROLE_ID));
        assertFalse(Role.isDefaultRoleId(a.getId()));
        assertFalse(Role.isDefaultRoleId(null));
        assertTrue(defaultRole.isDefaultRole());
        assertFalse(a.isDefaultRole());
    }

    public void testTC003() throws Exception {
        assertEquals("a", a.getRoleSuffix());
        assertEquals("scout", Role.getRoleIdSuffix("model.role.scout"));
        assertEquals("plain", Role.getRoleIdSuffix("plain"));
    }

    public void testTC004() throws Exception {
        a.setDowngrade(b);
        a.setMaximumCount(7);
        a.setExpertUnit(unit);
        assertSame(b, a.getDowngrade());
        assertEquals(7, a.getMaximumCount());
        assertSame(unit, a.getExpertUnit());
        a.setDowngrade(null);
        a.setExpertUnit(null);
        assertSame(null, a.getDowngrade());
        assertSame(null, a.getExpertUnit());
    }

    public void testTC005() throws Exception {
        goods(a, new AbstractGoods(g1, 40), new AbstractGoods(g2, 25));
        List<AbstractGoods> result = a.getRequiredGoodsList(1);
        assertEquals(2, result.size());
        assertEquals(40, count(result, g1));
        assertEquals(25, count(result, g2));
        result.get(0).setAmount(999);
        assertEquals(40, a.getRequiredAmountOf(g1));
    }

    public void testTC006() throws Exception {
        goods(a, new AbstractGoods(g1, 40), new AbstractGoods(g2, 25));
        List<AbstractGoods> result = a.getRequiredGoodsList(3);
        assertEquals(120, count(result, g1));
        assertEquals(75, count(result, g2));
        assertEquals(40, a.getRequiredAmountOf(g1));
    }

    public void testTC007() throws Exception {
        assertTrue(a.getRequiredGoodsList(4).isEmpty());
    }

    public void testTC008() throws Exception {
        goods(a, new AbstractGoods(g1, 40));
        assertEquals(40, count(a.getRequiredGoodsList(0), g1));
        assertEquals(40, count(a.getRequiredGoodsList(-2), g1));
    }

    public void testTC009() throws Exception {
        goods(a, new AbstractGoods(g1, 40), new AbstractGoods(g2, 25));
        assertEquals(130, a.getRequiredGoods(2).mapToInt(AbstractGoods::getAmount).sum());
    }

    public void testTC010() throws Exception {
        assertEquals(0, a.getRequiredGoodsPrice(null));
    }

    public void testTC011() throws Exception {
        goods(a, new AbstractGoods(g1, 40), new AbstractGoods(g2, 25));
        a.setMaximumCount(3);
        Game game = new ServerGame(local);
        Market market = new Market(game, "market:cft");
        market.setInitialPrice(g1, 4);
        market.setInitialPrice(g2, 7);
        market.getMarketData(g1).setCostToBuy(4);
        market.getMarketData(g2).setCostToBuy(7);
        assertEquals(1005, a.getRequiredGoodsPrice(market));
    }

    public void testTC012() throws Exception {
        List<Role.RoleChange> first = a.getRoleChanges();
        assertTrue(first.isEmpty());
        assertSame(first, a.getRoleChanges());
    }

    public void testTC013() throws Exception {
        Role.RoleChange change = new Role.RoleChange(a.getId(), b.getId());
        a.setRoleChanges(Arrays.asList(change));
        assertEquals(1, a.getRoleChanges().size());
        assertEquals(a.getId(), change.from);
        assertEquals(b.getId(), change.capture);
        assertSame(a, change.getFrom(local));
        assertSame(b, change.getCapture(local));
    }

    public void testTC014() throws Exception {
        a.setRoleChanges(Arrays.asList(new Role.RoleChange(a.getId(), b.getId())));
        List<Role.RoleChange> first = a.getRoleChanges();
        a.setRoleChanges(Arrays.asList(new Role.RoleChange(b.getId(), a.getId())));
        assertSame(first, a.getRoleChanges());
        assertEquals(1, first.size());
        assertEquals(b.getId(), first.get(0).from);
    }

    public void testTC015() throws Exception {
        Role.RoleChange change = new Role.RoleChange(null, null);
        assertSame(null, change.from);
        assertSame(null, change.capture);
    }

    public void testTC016() throws Exception {
        strength(a, 5, 3);
        strength(b, 2, 1);
        assertTrue(Role.militaryComparator.compare(a, b) < 0);
        assertTrue(Role.militaryComparator.compare(b, a) > 0);
        assertEquals(0, Role.militaryComparator.compare(a, a));
    }

    public void testTC017() throws Exception {
        assertTrue(a.getOffence() == 0.0);
        assertFalse(a.isOffensive());
        assertFalse(a.isDefensive());
        strength(a, 4, 2);
        assertTrue(a.getOffence() == 4.0);
        assertTrue(a.isOffensive());
        assertTrue(a.isDefensive());
    }

    public void testTC018() throws Exception {
        assertTrue(Role.rolesCompatible(null, null));
    }

    public void testTC019() throws Exception {
        assertFalse(Role.rolesCompatible(null, a));
    }

    public void testTC020() throws Exception {
        assertFalse(Role.rolesCompatible(a, null));
    }

    public void testTC021() throws Exception {
        assertTrue(Role.rolesCompatible(a, a));
    }

    public void testTC022() throws Exception {
        a.setDowngrade(b);
        assertTrue(Role.rolesCompatible(a, b));
    }

    public void testTC023() throws Exception {
        b.setDowngrade(a);
        assertTrue(Role.rolesCompatible(a, b));
    }

    public void testTC024() throws Exception {
        assertFalse(Role.rolesCompatible(a, b));
        assertFalse(a.isCompatibleWith(b));
        assertTrue(a.isCompatibleWith(a));
    }

    public void testTC025() throws Exception {
        goods(a, new AbstractGoods(g1, 40));
        assertTrue(Role.getGoodsDifference(a, 1, a, 3).isEmpty());
    }

    public void testTC026() throws Exception {
        assertTrue(Role.getGoodsDifference(null, 1, defaultRole, 1).isEmpty());
    }

    public void testTC027() throws Exception {
        goods(a, new AbstractGoods(g1, 40), new AbstractGoods(g2, 25));
        List<AbstractGoods> result = Role.getGoodsDifference(null, 1, a, 2);
        assertEquals(2, result.size());
        assertEquals(80, count(result, g1));
        assertEquals(50, count(result, g2));
    }

    public void testTC028() throws Exception {
        assertTrue(Role.getGoodsDifference(a, 1, b, 1).isEmpty());
    }

    public void testTC029() throws Exception {
        goods(a, new AbstractGoods(g1, 40), new AbstractGoods(g2, 25));
        goods(b, new AbstractGoods(g1, 40), new AbstractGoods(g3, 20));
        List<AbstractGoods> result = Role.getGoodsDifference(a, 1, b, 1);
        assertEquals(2, result.size());
        assertEquals(0, count(result, g1));
        assertEquals(-25, count(result, g2));
        assertEquals(20, count(result, g3));
    }

    public void testTC030() throws Exception {
        goods(a, new AbstractGoods(g1, 40));
        goods(b, new AbstractGoods(g1, 70));
        List<AbstractGoods> result = Role.getGoodsDifference(a, 1, b, 1);
        assertEquals(1, result.size());
        assertEquals(30, count(result, g1));
    }

    public void testTC031() throws Exception {
        goods(a, new AbstractGoods(g1, 70));
        goods(b, new AbstractGoods(g1, 40));
        List<AbstractGoods> result = Role.getGoodsDifference(a, 1, b, 1);
        assertEquals(1, result.size());
        assertEquals(-30, count(result, g1));
    }

    public void testTC032() throws Exception {
        goods(a, new AbstractGoods(g1, 40), new AbstractGoods(g2, 25));
        goods(b, new AbstractGoods(g1, 40));
        List<AbstractGoods> result = Role.getGoodsDifference(a, 2, b, 3);
        assertEquals(2, result.size());
        assertEquals(40, count(result, g1));
        assertEquals(-50, count(result, g2));
    }

    public void testTC033() throws Exception {
        goods(a, new AbstractGoods(g1, 40), new AbstractGoods(g2, 25));
        List<AbstractGoods> result = Role.getGoodsDifference(a, 1, defaultRole, 1);
        assertEquals(2, result.size());
        assertEquals(-40, count(result, g1));
        assertEquals(-25, count(result, g2));
    }

    public void testTC034() throws Exception {
        assertEquals(10, a.getRoleIndex());
        goods(a, new AbstractGoods(g1, 40), new AbstractGoods(g2, 25));
        assertEquals(8, a.getRoleIndex());
    }

    public void testTC035() throws Exception {
        a.addRequiredAbility(Ability.REF_UNIT, true);
        goods(a, new AbstractGoods(g1, 40));
        assertEquals(19, a.getRoleIndex());
    }

    public void testTC036() throws Exception {
        a.addRequiredAbility(Ability.NATIVE, true);
        a.addRequiredAbility(Ability.REF_UNIT, true);
        goods(a, new AbstractGoods(g1, 40), new AbstractGoods(g2, 25));
        assertEquals(28, a.getRoleIndex());
    }

    public void testTC037() throws Exception {
        assertTrue(Role.getAvailableRoles(null, unit, Collections.<Role>emptyList()).isEmpty());
    }

    public void testTC038() throws Exception {
        String ability = "model.ability.cftRequired";
        unit.addAbility(new Ability(ability, true));
        a.addRequiredAbility(ability, true);
        b.addRequiredAbility(ability, false);
        List<Role> result = Role.getAvailableRoles(null, unit, Arrays.asList(defaultRole, a, b));
        assertEquals(2, result.size());
        assertSame(defaultRole, result.get(0));
        assertSame(a, result.get(1));
    }

    public void testTC039() throws Exception {
        assertEquals("[Role a]", a.toFullString());
    }

    public void testTC040() throws Exception {
        a.addRequiredAbility("model.ability.cftRequired", true);
        assertEquals("[Role a model.ability.cftRequired=true]", a.toFullString());
    }

    public void testTC041() throws Exception {
        assertEquals("NONE", a.canBeBuiltInColony(null, null).name());
    }

    public void testTC042() throws Exception {
        assertFalse(a.copyIn((FreeColObject)null));
        assertFalse(a.copyIn(new GoodsType(a.getId(), local)));
        assertFalse(a.copyIn(b));
    }

    public void testTC043() throws Exception {
        Role source = new Role(a.getId(), local);
        goods(a);
        a.getFeatureContainer();
        goods(source, new AbstractGoods(g1, 40));
        source.setDowngrade(b);
        source.setMaximumCount(4);
        source.setExpertUnit(unit);
        source.setRoleChanges(Arrays.asList(new Role.RoleChange(b.getId(), defaultRole.getId())));
        assertTrue(a.copyIn(source));
        assertSame(b, a.getDowngrade());
        assertEquals(4, a.getMaximumCount());
        assertSame(unit, a.getExpertUnit());
        assertEquals(40, a.getRequiredAmountOf(g1));
        assertEquals(1, a.getRoleChanges().size());
        assertFalse(a.getRoleChanges() == source.getRoleChanges());
        assertSame(source.getRoleChanges().get(0), a.getRoleChanges().get(0));
    }

    public void testTC044() throws Exception {
        String text = xml(a);
        assertFalse(text.contains("downgrade="));
        assertFalse(text.contains("expert-unit="));
        assertFalse(text.contains("maximum-count="));
        assertFalse(text.contains("role-change"));
    }

    public void testTC045() throws Exception {
        a.setDowngrade(b);
        a.setMaximumCount(4);
        a.setExpertUnit(unit);
        a.setRoleChanges(Arrays.asList(new Role.RoleChange(b.getId(), defaultRole.getId()), new Role.RoleChange(defaultRole.getId(), b.getId())));
        goods(a, new AbstractGoods(g1, 40));
        String text = xml(a);
        assertTrue(text.contains("downgrade="));
        assertTrue(text.contains("expert-unit="));
        assertTrue(text.contains("maximum-count="));
        Role copy = new Role(a.getId(), local);
        read(copy, text);
        assertSame(b, copy.getDowngrade());
        assertSame(unit, copy.getExpertUnit());
        assertEquals(4, copy.getMaximumCount());
        assertEquals(2, copy.getRoleChanges().size());
        assertEquals(b.getId(), copy.getRoleChanges().get(0).from);
        assertEquals(defaultRole.getId(), copy.getRoleChanges().get(0).capture);
        assertEquals(40, copy.getRequiredAmountOf(g1));
    }

    public void testTC046() throws Exception {
        read(a, "<role id=\"model.role.a\" downgrade=\"model.role.b\" expertUnit=\"model.unit.cftExpert\" maximumCount=\"7\"/>");
        assertSame(b, a.getDowngrade());
        assertSame(unit, a.getExpertUnit());
        assertEquals(7, a.getMaximumCount());
    }

    public void testTC047() throws Exception {
        local.findType("model.unit.cftOther", UnitType.class);
        read(a, "<role id=\"model.role.a\" expertUnit=\"model.unit.cftExpert\" expert-unit=\"model.unit.cftOther\" maximumCount=\"7\" maximum-count=\"9\"/>");
        assertSame(unit, a.getExpertUnit());
        assertEquals(7, a.getMaximumCount());
    }

    public void testTC048() throws Exception {
        a.setDowngrade(b);
        a.setExpertUnit(unit);
        a.setMaximumCount(8);
        a.setRoleChanges(Arrays.asList(new Role.RoleChange(b.getId(), a.getId())));
        read(a, "<role id=\"model.role.a\"/>");
        assertSame(null, a.getDowngrade());
        assertSame(null, a.getExpertUnit());
        assertEquals(1, a.getMaximumCount());
        assertTrue(a.getRoleChanges().isEmpty());
    }

    public void testTC049() throws Exception {
        child(a, "<role-change from=\"model.role.a\" capture=\"model.role.b\"/>");
        child(a, "<role-change from=\"model.role.b\" capture=\"model.role.default\"/>");
        assertEquals(2, a.getRoleChanges().size());
        assertEquals(a.getId(), a.getRoleChanges().get(0).from);
        assertEquals(b.getId(), a.getRoleChanges().get(1).from);
    }

    public void testTC050() throws Exception {
        child(a, "<required-goods id=\"model.goods.cftOne\" value=\"17\"/>");
        assertEquals(17, a.getRequiredAmountOf(g1));
    }

    public void testTC051() throws Exception {
        try {
            child(a, "<unexpected/>");
            fail("Expected XMLStreamException");
        } catch (XMLStreamException e) {
            assertNotNull(e.getMessage());
        }
    }

    public void testTC052() throws Exception {
        a.setRoleChanges(Arrays.asList(new Role.RoleChange(a.getId(), b.getId())));
        goods(a, new AbstractGoods(g1, 40));
        try (FreeColXMLReader reader = new FreeColXMLReader(new StringReader("<role/>"))) {
            reader.nextTag();
            a.clearContainers(reader);
        }
        assertTrue(a.getRoleChanges().isEmpty());
        assertTrue(a.getRequiredGoodsList().isEmpty());
    }

    public void testTC053() throws Exception {
        List<Role.RoleChange> incoming = new ArrayList<Role.RoleChange>();
        incoming.add(new Role.RoleChange(a.getId(), b.getId()));
        a.setRoleChanges(incoming);
        incoming.clear();
        assertEquals(1, a.getRoleChanges().size());
        a.setRoleChanges(Collections.<Role.RoleChange>emptyList());
        assertTrue(a.getRoleChanges().isEmpty());
    }
}
