package net.sf.freecol.common.model;

import java.util.Arrays;
import java.util.Collections;
import net.sf.freecol.common.option.GameOptions;
import net.sf.freecol.util.test.FreeColTestCase;

public class PlayerCfgTest extends FreeColTestCase {
    private Game game;
    private Player p;
    private Player q;
    private boolean originalOverflow;
    protected void setUp() throws Exception {
        super.setUp();
        originalOverflow = spec().getBoolean(GameOptions.SAVE_PRODUCTION_OVERFLOW);
        game = getStandardGame();
        p = new Player(game, "player:cftDutch");
        p.setNation(spec().getNation("model.nation.dutch"));
        p.setNationType(game.getPlayerByNationId("model.nation.dutch").getNationType());
        p.changePlayerType(Player.PlayerType.COLONIAL);
        q = new Player(game, "player:cftFrench");
        q.setNation(spec().getNation("model.nation.french"));
        q.changePlayerType(Player.PlayerType.COLONIAL);
    }
    protected void tearDown() throws Exception {
        spec().setBoolean(GameOptions.SAVE_PRODUCTION_OVERFLOW, originalOverflow);
        super.tearDown();
    }

    public void testTC001() {
        p.changePlayerType(Player.PlayerType.NATIVE);
        assertEquals("NATIVE", p.getPlayerType().name());
        assertFalse(p.isColonial());
        assertFalse(p.isEuropean());
        assertTrue(p.isIndian());
        assertFalse(p.isRebel());
        assertFalse(p.isUndead());
        assertFalse(p.isREF());
    }

    public void testTC002() {
        p.changePlayerType(Player.PlayerType.COLONIAL);
        assertEquals("COLONIAL", p.getPlayerType().name());
        assertTrue(p.isColonial());
        assertTrue(p.isEuropean());
        assertFalse(p.isIndian());
        assertFalse(p.isRebel());
        assertFalse(p.isUndead());
        assertFalse(p.isREF());
    }

    public void testTC003() {
        p.changePlayerType(Player.PlayerType.REBEL);
        assertEquals("REBEL", p.getPlayerType().name());
        assertFalse(p.isColonial());
        assertTrue(p.isEuropean());
        assertFalse(p.isIndian());
        assertTrue(p.isRebel());
        assertFalse(p.isUndead());
        assertFalse(p.isREF());
    }

    public void testTC004() {
        p.changePlayerType(Player.PlayerType.INDEPENDENT);
        assertEquals("INDEPENDENT", p.getPlayerType().name());
        assertFalse(p.isColonial());
        assertTrue(p.isEuropean());
        assertFalse(p.isIndian());
        assertFalse(p.isRebel());
        assertFalse(p.isUndead());
        assertFalse(p.isREF());
    }

    public void testTC005() {
        p.changePlayerType(Player.PlayerType.ROYAL);
        assertEquals("ROYAL", p.getPlayerType().name());
        assertFalse(p.isColonial());
        assertTrue(p.isEuropean());
        assertFalse(p.isIndian());
        assertFalse(p.isRebel());
        assertFalse(p.isUndead());
        assertTrue(p.isREF());
    }

    public void testTC006() {
        p.changePlayerType(Player.PlayerType.UNDEAD);
        assertEquals("UNDEAD", p.getPlayerType().name());
        assertFalse(p.isColonial());
        assertFalse(p.isEuropean());
        assertFalse(p.isIndian());
        assertFalse(p.isRebel());
        assertTrue(p.isUndead());
        assertFalse(p.isREF());
    }

    public void testTC007() {
        p.changePlayerType(Player.PlayerType.RETIRED);
        assertEquals("RETIRED", p.getPlayerType().name());
        assertFalse(p.isColonial());
        assertFalse(p.isEuropean());
        assertFalse(p.isIndian());
        assertFalse(p.isRebel());
        assertFalse(p.isUndead());
        assertFalse(p.isREF());
    }

    public void testTC008() {
        p.changePlayerType(Player.PlayerType.REBEL);
        p.changePlayerType(Player.PlayerType.COLONIAL);
        assertEquals("COLONIAL", p.getPlayerType().name());
    }

    public void testTC009() {
        p.changePlayerType(Player.PlayerType.INDEPENDENT);
        p.changePlayerType(Player.PlayerType.COLONIAL);
        assertEquals("COLONIAL", p.getPlayerType().name());
    }

    public void testTC010() {
        p.setGold(100);
        assertEquals(137, p.modifyGold(37));
        assertEquals(137, p.getGold());
    }

    public void testTC011() {
        p.setGold(100);
        assertEquals(55, p.modifyGold(-45));
        assertEquals(55, p.getGold());
    }

    public void testTC012() {
        p.setGold(100);
        assertEquals(0, p.modifyGold(-140));
        assertEquals(0, p.getGold());
    }

    public void testTC013() {
        p.setGold(-2147483648);
        assertEquals(-2147483648, p.modifyGold(37));
        assertEquals(-2147483648, p.getGold());
    }

    public void testTC014() {
        p.setGold(100);
        assertTrue(p.checkGold(37));
    }

    public void testTC015() {
        p.setGold(100);
        assertFalse(p.checkGold(140));
    }

    public void testTC016() {
        p.setGold(-2147483648);
        assertTrue(p.checkGold(140));
    }

    public void testTC017() {
        p.setImmigration(75);
        assertEquals(75, p.getImmigration());
    }

    public void testTC018() {
        p.setImmigration(75);
        p.changePlayerType(Player.PlayerType.REBEL);
        p.setImmigration(99);
        assertEquals(0, p.getImmigration());
    }

    public void testTC019() {
        spec().setBoolean(GameOptions.SAVE_PRODUCTION_OVERFLOW, true);
        p.setImmigration(90);
        p.setImmigrationRequired(40);
        p.reduceImmigration();
        assertEquals(50, p.getImmigration());
    }

    public void testTC020() {
        spec().setBoolean(GameOptions.SAVE_PRODUCTION_OVERFLOW, true);
        p.setImmigration(30);
        p.setImmigrationRequired(40);
        p.reduceImmigration();
        assertEquals(0, p.getImmigration());
    }

    public void testTC021() {
        spec().setBoolean(GameOptions.SAVE_PRODUCTION_OVERFLOW, false);
        p.setImmigration(90);
        p.setImmigrationRequired(40);
        p.reduceImmigration();
        assertEquals(0, p.getImmigration());
    }

    public void testTC022() {
        p.setImmigration(75);
        p.changePlayerType(Player.PlayerType.REBEL);
        p.reduceImmigration();
        p.changePlayerType(Player.PlayerType.COLONIAL);
        assertEquals(75, p.getImmigration());
    }

    public void testTC023() {
        p.setImmigration(30);
        p.modifyImmigration(20);
        assertEquals(50, p.getImmigration());
    }

    public void testTC024() {
        p.setImmigration(30);
        p.modifyImmigration(-50);
        assertEquals(0, p.getImmigration());
    }

    public void testTC025() {
        p.setImmigration(70);
        p.setImmigrationRequired(40);
        p.changePlayerType(Player.PlayerType.COLONIAL);
        assertTrue(p.checkEmigrate());
    }

    public void testTC026() {
        p.setImmigration(20);
        p.setImmigrationRequired(40);
        p.changePlayerType(Player.PlayerType.COLONIAL);
        assertFalse(p.checkEmigrate());
    }

    public void testTC027() {
        p.setImmigration(70);
        p.setImmigrationRequired(40);
        p.changePlayerType(Player.PlayerType.REBEL);
        assertFalse(p.checkEmigrate());
    }

    public void testTC028() {
        p.setStance(q, Stance.PEACE);
        assertEquals(true, p.setStance(q, Stance.WAR));
        assertEquals("WAR", p.getStance(q).name());
    }

    public void testTC029() {
        p.setStance(q, Stance.WAR);
        assertEquals(true, p.setStance(q, Stance.CEASE_FIRE));
        assertEquals("CEASE_FIRE", p.getStance(q).name());
    }

    public void testTC030() {
        p.setStance(q, Stance.PEACE);
        assertEquals(false, p.setStance(q, Stance.CEASE_FIRE));
        assertEquals("CEASE_FIRE", p.getStance(q).name());
    }

    public void testTC031() {
        p.setStance(q, Stance.PEACE);
        assertEquals(false, p.setStance(q, Stance.UNCONTACTED));
        assertEquals("UNCONTACTED", p.getStance(q).name());
    }

    public void testTC032() {
        p.setStance(q, Stance.PEACE);
        assertEquals(true, p.setStance(q, Stance.PEACE));
        assertEquals("PEACE", p.getStance(q).name());
    }

    public void testTC033() {
        p.setStance(q, Stance.WAR);
        assertTrue(p.setStance(q, null));
        assertEquals("UNCONTACTED", p.getStance(q).name());
    }

    public void testTC034() {
        try { p.setStance(null, Stance.PEACE); fail("Expected RuntimeException"); } catch (RuntimeException e) { assertNotNull(e.getMessage()); }
    }

    public void testTC035() {
        try { p.setStance(p, Stance.PEACE); fail("Expected RuntimeException"); } catch (RuntimeException e) { assertNotNull(e.getMessage()); }
    }

    public void testTC036() {
        assertEquals("UNCONTACTED", p.getStance(null).name());
        assertEquals("UNCONTACTED", p.getStance(q).name());
        assertFalse(p.hasContacted(q));
        assertFalse(p.atWarWith(q));
    }

    public void testTC037() {
        Player.makeContact(p, q);
        assertEquals("PEACE", p.getStance(q).name());
        assertEquals("PEACE", q.getStance(p).name());
        assertTrue(p.hasContacted(q));
        p.setStance(q, Stance.WAR);
        assertTrue(p.atWarWith(q));
    }

    public void testTC038() {
        try { p.getTension((Player)null); fail("Expected RuntimeException"); } catch (RuntimeException e) { assertNotNull(e.getMessage()); }
    }

    public void testTC039() {
        p.removeTension(q);
        Tension first = p.getTension(q);
        assertEquals(0, first.getValue());
        assertSame(first, p.getTension(q));
    }

    public void testTC040() {
        p.setTension(p, new Tension(10));
        p.setTension(null, new Tension(20));
        assertFalse(p.getTension().containsKey(p));
        assertFalse(p.getTension().containsKey(null));
    }

    public void testTC041() {
        p.setTension(q, new Tension(75));
        assertEquals(75, p.getTension(q).getValue());
        p.removeTension(null);
        p.removeTension(q);
        assertEquals(0, p.getTension(q).getValue());
    }

    public void testTC042() {
        assertTrue(p.getBannedMissions().isEmpty());
        assertFalse(p.missionsBanned(q));
        p.removeMissionBan(q);
        assertFalse(p.missionsBanned(q));
    }

    public void testTC043() {
        p.addMissionBan(q);
        p.addMissionBan(p);
        assertTrue(p.missionsBanned(q));
        p.removeMissionBan(q);
        assertFalse(p.missionsBanned(q));
        assertEquals(1, p.getBannedMissions().size());
    }

    public void testTC044() {
        p.setBannedMissions(Arrays.asList(q));
        assertTrue(p.missionsBanned(q));
        p.setBannedMissions(Collections.<Player>emptyList());
        assertTrue(p.getBannedMissions().isEmpty());
    }

    public void testTC045() {
        NationType old = p.getNationType();
        p.changeNationType(null);
        assertFalse(p.canBuildColonies());
        assertFalse(p.canHaveFoundingFathers());
        p.changeNationType(old);
        assertTrue(p.canBuildColonies());
        assertTrue(p.canHaveFoundingFathers());
    }

    public void testTC046() {
        p.setLiberty(75);
        assertEquals(75, p.getLiberty());
        p.modifyLiberty(-100);
        assertEquals(0, p.getLiberty());
    }

    public void testTC047() {
        p.changeNationType(null);
        p.setLiberty(75);
        assertEquals(0, p.getLiberty());
    }

    public void testTC048() {
        p.changePlayerType(Player.PlayerType.REBEL);
        p.modifyLiberty(25);
        assertEquals(25, p.getInterventionBells());
        assertEquals(25, p.getLiberty());
    }

    public void testTC049() {
        p.setNewLandName(null);
        assertFalse(p.isNewLandNamed());
        p.setNewLandName("");
        assertTrue(p.isNewLandNamed());
        p.setNewLandName("CFT land");
        assertEquals("CFT land", p.getNewLandName());
    }

    public void testTC050() {
        assertEquals("0.0", Double.toString(Player.strengthRatio(0.0, 50.0)));
    }

    public void testTC051() {
        assertEquals("0.3", Double.toString(Player.strengthRatio(30.0, 70.0)));
    }

    public void testTC052() {
        assertEquals("1.0", Double.toString(Player.strengthRatio(50.0, 0.0)));
    }

    public void testTC053() {
        p.setTension(Collections.singletonMap(q, new Tension(44)));
        assertEquals(44, p.getTension(q).getValue());
        assertEquals(1, p.getTension().size());
    }
}

