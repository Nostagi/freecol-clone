package net.sf.freecol.common.model;

import java.io.ByteArrayInputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import javax.xml.stream.XMLStreamException;
import net.sf.freecol.common.io.FreeColXMLReader;
import net.sf.freecol.common.io.FreeColXMLWriter;
import net.sf.freecol.common.model.Monarch.MonarchAction;
import net.sf.freecol.common.option.AbstractOption;
import net.sf.freecol.common.option.AbstractUnitOption;
import net.sf.freecol.common.option.GameOptions;
import net.sf.freecol.common.option.IntegerOption;
import net.sf.freecol.common.option.UnitListOption;
import net.sf.freecol.server.model.ServerGame;
import net.sf.freecol.util.test.FreeColTestCase;

public class MonarchCfgTest extends FreeColTestCase {
    private static byte[] baseXml;
    private Specification local;
    private Game game;
    private FixturePlayer p;
    private FixturePlayer q;
    private Monarch m;

    public static class FixturePlayer extends Player {
        boolean settlements = true;
        boolean war = true;
        boolean potentialEnemy = true;
        boolean potentialFriend = true;
        int strength;
        int purchasePrice = 100;
        int hirePrice = 100;
        public FixturePlayer(Game game, String id) { super(game, id); }
        public boolean hasSettlements() { return settlements; }
        public boolean isAtWar() { return war; }
        public boolean isPotentialEnemy(Player other) { return potentialEnemy; }
        public boolean isPotentialFriend(Player other) { return potentialFriend; }
        public int calculateStrength(boolean naval) { return strength; }
        public int getEuropeanPurchasePrice(AbstractUnit unit) { return purchasePrice; }
        public int getMercenaryHirePrice(AbstractUnit unit) {
            return hirePrice == Constants.INFINITY ? Constants.INFINITY : hirePrice * unit.getNumber();
        }
    }

    public static class FixedRandom extends Random {
        private final boolean high;
        private final double decimal;
        public FixedRandom(boolean high, double decimal) {
            super(20261114L);
            this.high = high;
            this.decimal = decimal;
        }
        public int nextInt(int bound) {
            if (bound <= 0) throw new IllegalArgumentException("bound");
            return high ? bound - 1 : 0;
        }
        public double nextDouble() { return decimal; }
    }

    protected void setUp() throws Exception {
        super.setUp();
        if (baseXml == null) {
            StringWriter out = new StringWriter();
            try (FreeColXMLWriter writer = new FreeColXMLWriter(out,
                    FreeColXMLWriter.WriteScope.toSave(), false)) {
                spec().toXML(writer);
                writer.flush();
            }
            baseXml = out.toString().getBytes(StandardCharsets.UTF_8);
        }
        local = new Specification(new ByteArrayInputStream(baseXml));
        local.applyDifficultyLevel("model.difficulty.medium");
        game = new ServerGame(local);
        game.setNationOptions(new NationOptions(local));
        p = player("dutch", "player:cftDutch");
        q = player("french", "player:cftFrench");
        game.addPlayer(p);
        game.addPlayer(q);
        p.setTax(40);
        p.setGold(10000);
        p.setAttackedByPrivateers(true);
        option(GameOptions.MAXIMUM_TAX, 80);
        option(GameOptions.TAX_ADJUSTMENT, 3);
        option(GameOptions.MONARCH_MEDDLING, 1);
        option(GameOptions.MERCENARY_PRICE, 100);
        game.setTurn(new Turn(120));
        m = new Monarch(game, p);
    }

    private FixturePlayer player(String nation, String id) {
        FixturePlayer value = new FixturePlayer(game, id);
        Nation n = local.getNation("model.nation." + nation);
        value.setNation(n);
        value.setNationType(n.getType());
        value.changePlayerType(Player.PlayerType.COLONIAL);
        return value;
    }

    private Random zero() { return new FixedRandom(false, 0.0); }
    private Random high() { return new FixedRandom(true, 0.9); }

    private void field(Object target, Class<?> owner, String name, Object value) throws Exception {
        Field f = owner.getDeclaredField(name);
        f.setAccessible(true);
        f.set(target, value);
    }

    private Object field(Object target, Class<?> owner, String name) throws Exception {
        Field f = owner.getDeclaredField(name);
        f.setAccessible(true);
        return f.get(target);
    }

    private void option(String name, int value) {
        IntegerOption opt = local.getOption(name, IntegerOption.class);
        opt.setMinimumValue(Math.min(opt.getMinimumValue(), value));
        opt.setMaximumValue(Math.max(opt.getMaximumValue(), value));
        opt.setValue(value);
    }

    private void forceOption(String name, AbstractUnit... units) {
        List<AbstractOption<AbstractUnit>> values = new ArrayList<>();
        for (int i = 0; i < units.length; i++) {
            AbstractUnitOption opt = new AbstractUnitOption("cft.force." + i, local);
            opt.setValue(units[i]);
            values.add(opt);
        }
        local.getOption(name, UnitListOption.class).setValue(values);
    }

    private AbstractUnit au(String type, String role, int count) {
        return new AbstractUnit(local.getUnitType("model.unit." + type), "model.role." + role, count);
    }

    private int total(List<AbstractUnit> units) {
        int result = 0;
        for (AbstractUnit unit : units) result += unit.getNumber();
        return result;
    }

    private void warm() throws Exception {
        Method method = Monarch.class.getDeclaredMethod("initializeCaches");
        method.setAccessible(true);
        method.invoke(m);
    }

    private void cached(String name, String... ids) throws Exception {
        warm();
        List<UnitType> types = new ArrayList<>();
        for (String id : ids) types.add(local.getUnitType("model.unit." + id));
        field(m, Monarch.class, name, types);
    }

    private void ref(AbstractUnit... units) throws Exception {
        field(m, Monarch.class, "expeditionaryForce", new Force(local, Arrays.asList(units), null));
    }

    private String xml(FreeColXMLWriter.WriteScope scope) throws Exception {
        StringWriter out = new StringWriter();
        try (FreeColXMLWriter writer = new FreeColXMLWriter(out, scope, false)) {
            m.toXML(writer);
            writer.flush();
        }
        return out.toString();
    }

    private void read(String text) throws Exception {
        try (FreeColXMLReader reader = new FreeColXMLReader(new StringReader(text))) {
            reader.nextTag();
            m.readFromXML(reader);
        }
    }

    private void child(String text) throws Exception {
        try (FreeColXMLReader reader = new FreeColXMLReader(new StringReader(text))) {
            reader.nextTag();
            m.readChild(reader);
        }
    }

    public void testTC001() throws Exception {
        Monarch fresh = new Monarch(game, p);
        assertSame(p, fresh.getPlayer());
        assertFalse(fresh.getSupportSea());
        assertFalse(fresh.getDispleasure());
        assertEquals("monarch", fresh.getXMLTagName());
    }

    public void testTC002() throws Exception {
        try {
            new Monarch(game, (Player)null);
            fail("Expected RuntimeException");
        } catch (RuntimeException e) {
            assertTrue(e.getMessage().contains("player == null"));
        }
    }

    public void testTC003() throws Exception {
        Monarch fresh = new Monarch(game, "monarch:cftId");
        assertEquals("monarch:cftId", fresh.getId());
        assertSame(null, fresh.getPlayer());
    }

    public void testTC004() throws Exception {
        MonarchAction action = MonarchAction.RAISE_TAX_ACT;
        assertEquals("model.monarch.action.raiseTaxAct.text", action.getTextKey());
        assertEquals("model.monarch.action.raiseTaxAct.yes", action.getYesKey());
        assertEquals("model.monarch.action.raiseTaxAct.no", action.getNoKey());
        assertEquals("model.monarch.action.raiseTaxAct.header", action.getHeaderKey());
    }

    public void testTC005() throws Exception {
        Force ref = m.getExpeditionaryForce();
        assertSame(ref, m.getExpeditionaryForce());
        assertFalse(ref.isEmpty());
        Force ivf = m.getInterventionForce();
        assertSame(ivf, m.getInterventionForce());
        assertFalse(ivf.isEmpty());
        assertFalse(m.getMercenaryForce() == m.getMercenaryForce());
        assertFalse(m.getWarSupportForce() == m.getWarSupportForce());
    }

    public void testTC006() throws Exception {
        m.setSupportSea(true);
        m.setDispleasure(true);
        assertTrue(m.getSupportSea());
        assertTrue(m.getDispleasure());
        m.setSupportSea(false);
        m.setDispleasure(false);
        assertFalse(m.getSupportSea());
        assertFalse(m.getDispleasure());
    }

    public void testTC007() throws Exception {
        UnitType plainSupport = new UnitType("model.unit.cftPlainSupport", local);
        plainSupport.addAbility(new Ability(Ability.SUPPORT_UNIT, true));
        local.getUnitTypeList().add(plainSupport);
        Role nonArmed = new Role("model.role.cftUnarmedOffensive", local);
        nonArmed.addModifier(new Modifier(Modifier.OFFENCE, 1, Modifier.ModifierType.ADDITIVE));
        field(local, Specification.class, "militaryRoles", Arrays.asList(local.getRole("model.role.dragoon"), local.getRole("model.role.dragoon"), local.getRole("model.role.cavalry"), local.getRole("model.role.cavalry"), local.getRole("model.role.soldier"), local.getRole("model.role.soldier"), local.getRole("model.role.infantry"), local.getRole("model.role.infantry"), nonArmed));
        assertNotNull(m.getNavalREFUnitType());
        assertNotNull(m.getNavalREFUnitType());
        assertNotNull(field(m, Monarch.class, "mountedRole"));
        assertNotNull(field(m, Monarch.class, "armedRole"));
        assertNotNull(field(m, Monarch.class, "refMountedRole"));
        assertNotNull(field(m, Monarch.class, "refArmedRole"));
    }

    public void testTC008() throws Exception {
        assertEquals(1, m.collectPotentialEnemies().size());
        assertEquals(1, m.collectPotentialFriends().size());
        q.potentialEnemy = false;
        q.potentialFriend = false;
        assertTrue(m.collectPotentialEnemies().isEmpty());
        assertTrue(m.collectPotentialFriends().isEmpty());
        p.addAbility(new Ability(Ability.IGNORE_EUROPEAN_WARS, true));
        q.potentialEnemy = true;
        assertTrue(m.collectPotentialEnemies().isEmpty());
    }

    public void testTC009() throws Exception {
        boolean[] expected = {true, true, true, false, true, true, true, true, true, true, true, true, true, true, false};
        for (MonarchAction action : MonarchAction.values()) {
            assertEquals(expected[action.ordinal()], m.actionIsValid(action));
        }
    }

    public void testTC010() throws Exception {
        p.setTax(80);
        assertFalse(m.actionIsValid(MonarchAction.RAISE_TAX_ACT));
        assertFalse(m.actionIsValid(MonarchAction.RAISE_TAX_WAR));
        p.setTax(30);
        assertFalse(m.actionIsValid(MonarchAction.LOWER_TAX_WAR));
        assertFalse(m.actionIsValid(MonarchAction.LOWER_TAX_OTHER));
    }

    public void testTC011() throws Exception {
        cached("navalREFUnitTypes");
        assertFalse(m.actionIsValid(MonarchAction.ADD_TO_REF));
        cached("navalREFUnitTypes", "manOWar");
        cached("landREFUnitTypes");
        assertFalse(m.actionIsValid(MonarchAction.ADD_TO_REF));
    }

    public void testTC012() throws Exception {
        p.setAttackedByPrivateers(false);
        assertFalse(m.actionIsValid(MonarchAction.SUPPORT_SEA));
        p.setAttackedByPrivateers(true);
        m.setSupportSea(true);
        assertFalse(m.actionIsValid(MonarchAction.SUPPORT_SEA));
        m.setSupportSea(false);
        m.setDispleasure(true);
        assertFalse(m.actionIsValid(MonarchAction.SUPPORT_SEA));
    }

    public void testTC013() throws Exception {
        p.war = false;
        assertFalse(m.actionIsValid(MonarchAction.SUPPORT_LAND));
        p.war = true;
        m.setDispleasure(true);
        assertFalse(m.actionIsValid(MonarchAction.MONARCH_MERCENARIES));
        m.setDispleasure(false);
        p.settlements = false;
        assertFalse(m.actionIsValid(MonarchAction.SUPPORT_LAND));
        p.setGold(4999);
        assertFalse(m.actionIsValid(MonarchAction.HESSIAN_MERCENARIES));
        p.setGold(5000);
        assertFalse(m.actionIsValid(MonarchAction.HESSIAN_MERCENARIES));
    }

    public void testTC014() throws Exception {
        game.setTurn(new Turn(1));
        assertTrue(m.getActionChoices().isEmpty());
    }

    public void testTC015() throws Exception {
        p.settlements = false;
        assertTrue(m.getActionChoices().isEmpty());
    }

    public void testTC016() throws Exception {
        p.changePlayerType(Player.PlayerType.REBEL);
        assertTrue(m.getActionChoices().isEmpty());
    }

    public void testTC017() throws Exception {
        p.setGold(1000);
        option(GameOptions.MONARCH_MEDDLING, 1);
        assertEquals(10, m.getActionChoices().size());
    }

    public void testTC018() throws Exception {
        p.setGold(0);
        option(GameOptions.MONARCH_MEDDLING, 1);
        assertEquals(10, m.getActionChoices().size());
    }

    public void testTC019() throws Exception {
        p.setGold(0);
        option(GameOptions.MONARCH_MEDDLING, 3);
        assertEquals(9, m.getActionChoices().size());
    }

    public void testTC020() throws Exception {
        p.setGold(6000);
        option(GameOptions.MONARCH_MEDDLING, 1);
        assertEquals(11, m.getActionChoices().size());
    }

    public void testTC021() throws Exception {
        assertEquals(41, m.raiseTax(zero()));
        assertEquals(47, m.raiseTax(high()));
        p.setTax(79);
        assertEquals(80, m.raiseTax(high()));
    }

    public void testTC022() throws Exception {
        assertEquals(39, m.lowerTax(zero()));
        assertEquals(33, m.lowerTax(high()));
        p.setTax(22);
        assertEquals(20, m.lowerTax(high()));
    }

    public void testTC023() throws Exception {
        ref(au("kingsRegular", "infantry", 1));
        cached("navalREFUnitTypes", "manOWar");
        assertTrue(m.shouldAddNavalUnit());
        AbstractUnit unit = m.addToREF(zero());
        assertEquals("model.unit.manOWar", unit.getId());
        assertEquals(1, unit.getNumber());
        assertFalse(m.shouldAddNavalUnit());
    }

    public void testTC024() throws Exception {
        ref(au("kingsRegular", "infantry", 1));
        cached("navalREFUnitTypes");
        assertSame(null, m.addToREF(zero()));
    }

    public void testTC025() throws Exception {
        ref();
        cached("landREFUnitTypes");
        assertFalse(m.shouldAddNavalUnit());
        assertSame(null, m.addToREF(zero()));
    }

    public void testTC026() throws Exception {
        ref();
        cached("landREFUnitTypes", "artillery");
        AbstractUnit unit = m.addToREF(zero());
        assertEquals("model.unit.artillery", unit.getId());
        assertEquals(Specification.DEFAULT_ROLE_ID, unit.getRoleId());
        assertEquals(1, unit.getNumber());
    }

    public void testTC027() throws Exception {
        ref();
        cached("landREFUnitTypes", "kingsRegular");
        AbstractUnit unit = m.addToREF(zero());
        assertEquals("model.role.cavalry", unit.getRoleId());
        assertEquals(1, unit.getNumber());
    }

    public void testTC028() throws Exception {
        ref();
        cached("landREFUnitTypes", "kingsRegular");
        AbstractUnit unit = m.addToREF(high());
        assertEquals("model.role.infantry", unit.getRoleId());
        assertEquals(3, unit.getNumber());
    }

    public void testTC029() throws Exception {
        option(GameOptions.INTERVENTION_TURNS, 100);
        game.setTurn(new Turn(1));
        Force force = m.getInterventionForce();
        int before = total(force.getLandUnitsList());
        m.updateInterventionForce();
        assertEquals(before, total(force.getLandUnitsList()));
    }

    public void testTC030() throws Exception {
        forceOption(GameOptions.INTERVENTION_FORCE, au("kingsRegular", "infantry", 2), au("manOWar", "default", 1));
        option(GameOptions.INTERVENTION_TURNS, 10);
        game.setTurn(new Turn(30));
        m.updateInterventionForce();
        assertEquals(5, total(m.getInterventionForce().getLandUnitsList()));
    }

    public void testTC031() throws Exception {
        forceOption(GameOptions.INTERVENTION_FORCE);
        option(GameOptions.INTERVENTION_TURNS, 10);
        game.setTurn(new Turn(30));
        m.updateInterventionForce();
        assertTrue(m.getInterventionForce().isEmpty());
    }

    public void testTC032() throws Exception {
        option(GameOptions.INTERVENTION_TURNS, -10);
        m.updateInterventionForce();
        assertFalse(m.getInterventionForce().isEmpty());
    }

    public void testTC033() throws Exception {
        List<AbstractUnit> result = m.getSupport(zero(), true);
        assertEquals(1, result.size());
        assertEquals(1, total(result));
        assertTrue(m.getSupportSea());
    }

    public void testTC034() throws Exception {
        option(GameOptions.MONARCH_SUPPORT, 0);
        List<AbstractUnit> result = m.getSupport(zero(), false);
        assertEquals(1, result.size());
        assertEquals(1, total(result));
        assertFalse(m.getSupportSea());
    }

    public void testTC035() throws Exception {
        option(GameOptions.MONARCH_SUPPORT, 1);
        List<AbstractUnit> result = m.getSupport(zero(), false);
        assertEquals(2, result.size());
        assertEquals(2, total(result));
        assertFalse(m.getSupportSea());
    }

    public void testTC036() throws Exception {
        option(GameOptions.MONARCH_SUPPORT, 2);
        List<AbstractUnit> result = m.getSupport(zero(), false);
        assertEquals(1, result.size());
        assertEquals(2, total(result));
        assertFalse(m.getSupportSea());
    }

    public void testTC037() throws Exception {
        option(GameOptions.MONARCH_SUPPORT, 3);
        List<AbstractUnit> result = m.getSupport(zero(), false);
        assertEquals(2, result.size());
        assertEquals(3, total(result));
        assertFalse(m.getSupportSea());
    }

    public void testTC038() throws Exception {
        option(GameOptions.MONARCH_SUPPORT, 4);
        List<AbstractUnit> result = m.getSupport(zero(), false);
        assertEquals(2, result.size());
        assertEquals(3, total(result));
        assertFalse(m.getSupportSea());
    }

    public void testTC039() throws Exception {
        option(GameOptions.MONARCH_SUPPORT, 5);
        List<AbstractUnit> result = m.getSupport(zero(), false);
        assertEquals(0, result.size());
        assertEquals(0, total(result));
        assertFalse(m.getSupportSea());
    }

    public void testTC040() throws Exception {
        p.strength = 100;
        q.strength = 0;
        forceOption(GameOptions.WAR_SUPPORT_FORCE, au("artillery", "default", 3));
        List<AbstractUnit> result = m.getWarSupport(q, zero());
        assertEquals(0, result.size());
        assertEquals(0, total(result));
    }

    public void testTC041() throws Exception {
        p.strength = 55;
        q.strength = 45;
        forceOption(GameOptions.WAR_SUPPORT_FORCE, au("artillery", "default", 3));
        List<AbstractUnit> result = m.getWarSupport(q, high());
        assertEquals(0, result.size());
        assertEquals(0, total(result));
    }

    public void testTC042() throws Exception {
        p.strength = 1;
        q.strength = 1000;
        forceOption(GameOptions.WAR_SUPPORT_FORCE, au("artillery", "default", 3));
        List<AbstractUnit> result = m.getWarSupport(q, zero());
        assertEquals(1, result.size());
        assertEquals(2, total(result));
    }

    public void testTC043() throws Exception {
        p.strength = 0;
        q.strength = 0;
        forceOption(GameOptions.WAR_SUPPORT_FORCE, au("artillery", "default", 3), au("veteranSoldier", "soldier", 2));
        List<AbstractUnit> result = m.getWarSupport(q, zero());
        assertEquals(1, result.size());
        assertEquals(1, total(result));
    }

    public void testTC044() throws Exception {
        p.strength = 0;
        q.strength = 10;
        forceOption(GameOptions.WAR_SUPPORT_FORCE, au("artillery", "default", 3));
        List<AbstractUnit> result = m.getWarSupport(q, zero());
        assertEquals(1, result.size());
        assertEquals(2, total(result));
    }

    public void testTC045() throws Exception {
        p.strength = 0;
        q.strength = 5;
        forceOption(GameOptions.WAR_SUPPORT_FORCE, au("artillery", "default", 3));
        List<AbstractUnit> result = m.getWarSupport(q, zero());
        assertEquals(1, result.size());
        assertEquals(1, total(result));
    }

    public void testTC046() throws Exception {
        p.strength = 0;
        q.strength = 1;
        forceOption(GameOptions.WAR_SUPPORT_FORCE, au("artillery", "default", 3));
        List<AbstractUnit> result = m.getWarSupport(q, zero());
        assertEquals(1, result.size());
        assertEquals(1, total(result));
    }

    public void testTC047() throws Exception {
        p.strength = 55;
        q.strength = 45;
        forceOption(GameOptions.WAR_SUPPORT_FORCE, au("artillery", "default", 3));
        List<AbstractUnit> result = m.getWarSupport(q, zero());
        assertEquals(1, result.size());
        assertEquals(1, total(result));
    }

    public void testTC048() throws Exception {
        p.strength = 1;
        q.strength = 1000;
        forceOption(GameOptions.WAR_SUPPORT_FORCE);
        List<AbstractUnit> result = m.getWarSupport(q, zero());
        assertEquals(0, result.size());
        assertEquals(0, total(result));
    }

    public void testTC049() throws Exception {
        p.strength = 0;
        q.strength = 0;
        forceOption(GameOptions.WAR_SUPPORT_FORCE, au("artillery", "default", 3));
        List<AbstractUnit> result = m.getWarSupport(q, zero());
        assertEquals(1, result.size());
        assertEquals(1, total(result));
    }

    public void testTC050() throws Exception {
        p.strength = 0;
        q.strength = 1;
        forceOption(GameOptions.WAR_SUPPORT_FORCE, au("artillery", "default", 1));
        List<AbstractUnit> result = m.getWarSupport(q, zero());
        assertEquals(1, result.size());
        assertEquals(1, total(result));
    }

    public void testTC051() throws Exception {
        cached("mercenaryTypes");
        p.purchasePrice = 100;
        p.setGold(1000);
        List<AbstractUnit> mercs = new ArrayList<>();
        mercs.add(au("artillery", "default", 9));
        assertEquals(0, m.loadMercenaries(zero(), mercs));
        assertEquals(0, total(mercs));
    }

    public void testTC052() throws Exception {
        cached("mercenaryTypes", "veteranSoldier");
        p.purchasePrice = 100;
        p.setGold(1000);
        List<AbstractUnit> mercs = new ArrayList<>();
        mercs.add(au("artillery", "default", 9));
        assertEquals(100, m.loadMercenaries(zero(), mercs));
        assertEquals(1, total(mercs));
    }

    public void testTC053() throws Exception {
        cached("mercenaryTypes", "artillery");
        p.purchasePrice = 100;
        p.setGold(1000);
        List<AbstractUnit> mercs = new ArrayList<>();
        mercs.add(au("artillery", "default", 9));
        assertEquals(100, m.loadMercenaries(zero(), mercs));
        assertEquals(1, total(mercs));
    }

    public void testTC054() throws Exception {
        cached("mercenaryTypes", "artillery");
        p.purchasePrice = 100;
        p.setGold(0);
        List<AbstractUnit> mercs = new ArrayList<>();
        mercs.add(au("artillery", "default", 9));
        assertEquals(0, m.loadMercenaries(zero(), mercs));
        assertEquals(0, total(mercs));
    }

    public void testTC055() throws Exception {
        cached("mercenaryTypes", "artillery");
        p.purchasePrice = 0;
        p.setGold(1000);
        List<AbstractUnit> mercs = new ArrayList<>();
        mercs.add(au("artillery", "default", 9));
        assertEquals(0, m.loadMercenaries(zero(), mercs));
        assertEquals(0, total(mercs));
    }

    public void testTC056() throws Exception {
        cached("mercenaryTypes", "artillery");
        p.purchasePrice = 2147483647;
        p.setGold(1000);
        List<AbstractUnit> mercs = new ArrayList<>();
        mercs.add(au("artillery", "default", 9));
        assertEquals(0, m.loadMercenaries(zero(), mercs));
        assertEquals(0, total(mercs));
    }

    public void testTC057() throws Exception {
        cached("mercenaryTypes", "veteranSoldier");
        p.purchasePrice = 100;
        p.setGold(150);
        List<AbstractUnit> mercs = new ArrayList<>();
        mercs.add(au("artillery", "default", 9));
        assertEquals(100, m.loadMercenaries(high(), mercs));
        assertEquals(1, total(mercs));
    }

    public void testTC058() throws Exception {
        cached("mercenaryTypes", "veteranSoldier", "artillery");
        p.purchasePrice = 100;
        p.setGold(1000);
        List<AbstractUnit> mercs = new ArrayList<>();
        mercs.add(au("artillery", "default", 9));
        assertEquals(300, m.loadMercenaries(high(), mercs));
        assertEquals(3, total(mercs));
    }

    public void testTC059() throws Exception {
        forceOption(GameOptions.MERCENARY_FORCE);
        p.hirePrice = 100;
        p.setGold(1000);
        List<AbstractUnit> mercs = new ArrayList<>();
        assertEquals(-1, m.loadMercenaryForce(zero(), mercs));
        assertEquals(0, total(mercs));
    }

    public void testTC060() throws Exception {
        forceOption(GameOptions.MERCENARY_FORCE, au("artillery", "default", 2), au("veteranSoldier", "soldier", 1));
        p.hirePrice = 100;
        p.setGold(1000);
        List<AbstractUnit> mercs = new ArrayList<>();
        assertEquals(300, m.loadMercenaryForce(zero(), mercs));
        assertEquals(3, total(mercs));
    }

    public void testTC061() throws Exception {
        forceOption(GameOptions.MERCENARY_FORCE, au("artillery", "default", 1));
        p.hirePrice = 0;
        p.setGold(1000);
        List<AbstractUnit> mercs = new ArrayList<>();
        assertEquals(-1, m.loadMercenaryForce(zero(), mercs));
        assertEquals(0, total(mercs));
    }

    public void testTC062() throws Exception {
        forceOption(GameOptions.MERCENARY_FORCE, au("artillery", "default", 1));
        p.hirePrice = 2147483647;
        p.setGold(1000);
        List<AbstractUnit> mercs = new ArrayList<>();
        assertEquals(-1, m.loadMercenaryForce(zero(), mercs));
        assertEquals(0, total(mercs));
    }

    public void testTC063() throws Exception {
        forceOption(GameOptions.MERCENARY_FORCE, au("artillery", "default", 3));
        p.hirePrice = 100;
        p.setGold(150);
        List<AbstractUnit> mercs = new ArrayList<>();
        assertEquals(100, m.loadMercenaryForce(zero(), mercs));
        assertEquals(1, total(mercs));
    }

    public void testTC064() throws Exception {
        forceOption(GameOptions.MERCENARY_FORCE, au("artillery", "default", 1));
        p.hirePrice = 100;
        p.setGold(0);
        List<AbstractUnit> mercs = new ArrayList<>();
        assertEquals(-1, m.loadMercenaryForce(zero(), mercs));
        assertEquals(0, total(mercs));
    }

    public void testTC065() throws Exception {
        forceOption(GameOptions.MERCENARY_FORCE, au("artillery", "default", 1), au("veteranSoldier", "soldier", 1));
        p.hirePrice = 100;
        p.setGold(100);
        List<AbstractUnit> mercs = new ArrayList<>();
        assertEquals(100, m.loadMercenaryForce(zero(), mercs));
        assertEquals(1, total(mercs));
    }

    public void testTC066() throws Exception {
        assertEquals("model.nation.dutch.ruler", m.getNameKey());
    }

    public void testTC067() throws Exception {
        assertFalse(m.copyIn((FreeColObject)null));
        Goods wrong = new Goods(game, null, local.getGoodsType("model.goods.food"), 25);
        wrong.setId(m.getId());
        assertFalse(m.copyIn(wrong));
    }

    public void testTC068() throws Exception {
        m.setSupportSea(true);
        m.setDispleasure(true);
        Monarch target = new Monarch(game, (String)null);
        target.setId(m.getId());
        assertTrue(target.copyIn(m));
        assertSame(p, target.getPlayer());
        assertTrue(target.getSupportSea());
        assertTrue(target.getDispleasure());
        assertSame(m.getExpeditionaryForce(), target.getExpeditionaryForce());
        assertSame(m.getInterventionForce(), target.getInterventionForce());
    }

    public void testTC069() throws Exception {
        m.setSupportSea(true);
        m.setDispleasure(true);
        String text = xml(FreeColXMLWriter.WriteScope.toSave());
        assertTrue(text.contains("supportSea=\"true\""));
        assertTrue(text.contains("<expeditionaryForce"));
        assertTrue(text.contains("<interventionForce"));
        field(m, Monarch.class, "expeditionaryForce", null);
        field(m, Monarch.class, "interventionForce", null);
        m.setSupportSea(false);
        m.setDispleasure(false);
        read(text);
        assertTrue(m.getSupportSea());
        assertTrue(m.getDispleasure());
        assertSame(p, m.getPlayer());
        assertFalse(m.getExpeditionaryForce().isEmpty());
        assertFalse(m.getInterventionForce().isEmpty());
        read(text);
        assertFalse(m.getExpeditionaryForce().isEmpty());
    }

    public void testTC070() throws Exception {
        m.setSupportSea(true);
        m.setDispleasure(true);
        String text = xml(FreeColXMLWriter.WriteScope.toClient(q));
        assertFalse(text.contains("supportSea="));
        assertFalse(text.contains("displeasure="));
        assertFalse(text.contains("<expeditionaryForce"));
        assertFalse(text.contains("<interventionForce"));
    }

    public void testTC071() throws Exception {
        read("<monarch id=\"" + m.getId() + "\" player=\"" + p.getId() + "\"/>");
        assertSame(p, m.getPlayer());
        assertFalse(m.getSupportSea());
        assertFalse(m.getDispleasure());
        assertTrue(m.getExpeditionaryForce().isEmpty());
        assertTrue(m.getInterventionForce().isEmpty());
    }

    public void testTC072() throws Exception {
        m.getExpeditionaryForce();
        child("<expeditionaryForce/>");
        assertTrue(m.getExpeditionaryForce().isEmpty());
    }

    public void testTC073() throws Exception {
        m.getInterventionForce();
        child("<interventionForce/>");
        assertTrue(m.getInterventionForce().isEmpty());
    }

    public void testTC074() throws Exception {
        Force before = m.getExpeditionaryForce();
        child("<mercenaryForce/>");
        assertSame(before, m.getExpeditionaryForce());
    }

    public void testTC075() throws Exception {
        try {
            child("<unexpected/>");
            fail("Expected XMLStreamException");
        } catch (XMLStreamException e) {
            assertNotNull(e.getMessage());
        }
    }
}
