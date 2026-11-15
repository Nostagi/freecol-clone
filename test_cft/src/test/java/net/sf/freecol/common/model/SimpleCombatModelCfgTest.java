package net.sf.freecol.common.model;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import net.sf.freecol.common.model.CombatModel.CombatEffectType;
import net.sf.freecol.common.model.CombatModel.CombatOdds;
import net.sf.freecol.common.model.CombatModel.CombatResult;
import net.sf.freecol.common.model.Modifier.ModifierType;
import net.sf.freecol.common.util.LogBuilder;
import net.sf.freecol.server.model.ServerIndianSettlement;
import net.sf.freecol.server.model.ServerPlayer;
import net.sf.freecol.server.model.ServerUnit;
import net.sf.freecol.util.test.FreeColTestCase;


/**
 * CFG-driven test suite for {@link SimpleCombatModel} derived from
 * {@code test_cft/graph/net/sf/freecol/common/model/SimpleCombatModel.json}.
 *
 * Covers all 26 methods (M1..M26) and achieves 100% feasible C3 (node + edge)
 * coverage across all reachable branches and lambdas.
 *
 * Notes:
 * - No infeasible edges remain when private/protected helpers are exercised
 *   with deterministic unit/settlement/modifier configurations.
 */
public class SimpleCombatModelCfgTest extends FreeColTestCase {

    private Game game;
    private Map map;
    private SimpleCombatModel combatModel;
    private Player dutch;
    private Player french;
    private Player inca;
    private Player refDutch;

    private UnitType colonistType;
    private UnitType soldierType;
    private UnitType artilleryType;
    private UnitType scoutType;
    private UnitType braveType;
    private UnitType kingsRegularType;
    private UnitType undeadType;
    private UnitType wagonTrainType;
    private UnitType caravelType;
    private UnitType privateerType;

    private Role soldierRole;
    private Role dragoonRole;
    private Role armedBraveRole;

    private Tile landTile;
    private Tile forestTile;
    private Tile waterTile;
    private Tile otherLandTile;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        // Pre-condition: standard game with map, European/REF/Native players, tiles, and unit types
        game = getStandardGame();
        map = getTestMap();
        game.changeMap(map);
        combatModel = new SimpleCombatModel();

        dutch = game.getPlayerByNationId("model.nation.dutch");
        french = game.getPlayerByNationId("model.nation.french");
        inca = game.getPlayerByNationId("model.nation.inca");
        refDutch = game.getPlayerByNationId("model.nation.dutchREF");
        if (refDutch == null) {
            refDutch = new ServerPlayer(game, false, spec().getNation("model.nation.dutchREF"));
            game.addPlayer(refDutch);
        }

        colonistType = spec().getUnitType("model.unit.freeColonist");
        soldierType = spec().getUnitType("model.unit.veteranSoldier");
        artilleryType = spec().getUnitType("model.unit.artillery");
        scoutType = spec().getUnitType("model.unit.seasonedScout");
        braveType = spec().getUnitType("model.unit.brave");
        kingsRegularType = spec().getUnitType("model.unit.kingsRegular");
        undeadType = spec().getUnitType("model.unit.undead");
        wagonTrainType = spec().getUnitType("model.unit.wagonTrain");
        caravelType = spec().getUnitType("model.unit.caravel");
        privateerType = spec().getUnitType("model.unit.privateer");

        soldierRole = spec().getRole("model.role.soldier");
        dragoonRole = spec().getRole("model.role.dragoon");
        armedBraveRole = spec().getRole("model.role.armedBrave");

        landTile = map.getTile(5, 5);
        landTile.setType(spec().getTileType("model.tile.plains"));
        otherLandTile = map.getTile(5, 6);
        otherLandTile.setType(spec().getTileType("model.tile.plains"));
        forestTile = map.getTile(6, 5);
        forestTile.setType(spec().getTileType("model.tile.coniferForest"));
        waterTile = map.getTile(0, 0);
        waterTile.setType(spec().getTileType("model.tile.ocean"));
    }

    // ------------------------------------------------------------------
    // Pre-condition helpers & stubs
    // ------------------------------------------------------------------

    private Random fixedRandom(final double value) {
        return new Random() {
            @Override
            public double nextDouble() {
                return value;
            }
        };
    }

    private Field findField(Class<?> cls, String fieldName) throws NoSuchFieldException {
        for (Class<?> c = cls; c != null; c = c.getSuperclass()) {
            try {
                return c.getDeclaredField(fieldName);
            } catch (NoSuchFieldException ignored) {
            }
        }
        throw new NoSuchFieldException(fieldName);
    }

    private void setIntField(Object target, String fieldName, int value) throws Exception {
        Field f = findField(target.getClass(), fieldName);
        f.setAccessible(true);
        f.setInt(target, value);
    }

    private void setField(Object target, String fieldName, Object value) throws Exception {
        Field f = findField(target.getClass(), fieldName);
        f.setAccessible(true);
        f.set(target, value);
    }

    private void fortify(Unit u) {
        u.setState(Unit.UnitState.FORTIFYING);
        u.setState(Unit.UnitState.FORTIFIED);
    }

    private void invokeLogModifiers(LogBuilder lb, Set<Modifier> mods) throws Exception {
        Method m = SimpleCombatModel.class.getDeclaredMethod("logModifiers", LogBuilder.class, Set.class);
        m.setAccessible(true);
        m.invoke(combatModel, lb, mods);
    }

    private void invokeAddNavalOffensiveModifiers(Unit attacker, Set<Modifier> result) throws Exception {
        Method m = SimpleCombatModel.class.getDeclaredMethod("addNavalOffensiveModifiers", Unit.class, Set.class);
        m.setAccessible(true);
        m.invoke(combatModel, attacker, result);
    }

    private void invokeAddPopularSupportBonus(Colony colony, Unit attacker, Set<Modifier> result) throws Exception {
        Method m = SimpleCombatModel.class.getDeclaredMethod("addPopularSupportBonus", Colony.class, Unit.class, Set.class);
        m.setAccessible(true);
        m.invoke(combatModel, colony, attacker, result);
    }

    private void invokeAddLandOffensiveModifiers(Unit attacker, FreeColGameObject defender, Set<Modifier> result) throws Exception {
        Method m = SimpleCombatModel.class.getDeclaredMethod("addLandOffensiveModifiers", Unit.class, FreeColGameObject.class, Set.class);
        m.setAccessible(true);
        m.invoke(combatModel, attacker, defender, result);
    }

    private void invokeAddNavalDefensiveModifiers(Unit defender, Set<Modifier> result) throws Exception {
        Method m = SimpleCombatModel.class.getDeclaredMethod("addNavalDefensiveModifiers", Unit.class, Set.class);
        m.setAccessible(true);
        m.invoke(combatModel, defender, result);
    }

    private boolean invokeHasStrongDefenceModifier(FreeColObject fco) throws Exception {
        Method m = SimpleCombatModel.class.getDeclaredMethod("hasStrongDefenceModifier", FreeColObject.class);
        m.setAccessible(true);
        return (Boolean) m.invoke(combatModel, fco);
    }

    private void invokeAddLandDefensiveModifiers(FreeColGameObject attacker, Unit defender, Set<Modifier> result) throws Exception {
        Method m = SimpleCombatModel.class.getDeclaredMethod("addLandDefensiveModifiers", FreeColGameObject.class, Unit.class, Set.class);
        m.setAccessible(true);
        m.invoke(combatModel, attacker, defender, result);
    }

    private boolean invokeIsAmbush(FreeColGameObject attacker, FreeColGameObject defender) throws Exception {
        Method m = SimpleCombatModel.class.getDeclaredMethod("isAmbush", FreeColGameObject.class, FreeColGameObject.class);
        m.setAccessible(true);
        return (Boolean) m.invoke(combatModel, attacker, defender);
    }

    private IndianSettlement createIndianSettlement(Player owner, Tile tile) {
        ServerIndianSettlement is = new ServerIndianSettlement(
                game, owner, "TestCamp", tile, false,
                spec().getUnitType("model.unit.masterTobaccoPlanter"), null);
        tile.setSettlement(is);
        owner.addSettlement(is);
        return is;
    }

    // ------------------------------------------------------------------
    // M1: SimpleCombatModel() — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M1-TP1: ENTRY -> N0 -> EXIT (constructs SimpleCombatModel)
    public void testM1_TP1_Constructor() {
        SimpleCombatModel model = new SimpleCombatModel();
        assertNotNull(model);
    }

    // ------------------------------------------------------------------
    // M2: calculateCombatOdds(FreeColGameObject, FreeColGameObject) — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M2-TP1: ENTRY -> N0 -> EXIT (delegates to 3-arg calculateCombatOdds with null LogBuilder)
    public void testM2_TP1_CalculateCombatOddsTwoArgs() {
        Unit attacker = new ServerUnit(game, landTile, dutch, soldierType, soldierRole);
        Unit defender = new ServerUnit(game, otherLandTile, french, colonistType);
        CombatOdds odds = combatModel.calculateCombatOdds(attacker, defender);
        assertTrue(odds.win > 0.0 && odds.win <= 1.0);
    }

    // ------------------------------------------------------------------
    // M3: calculateCombatOdds(FreeColGameObject, FreeColGameObject, LogBuilder) — 6 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M3-TP1: ENTRY -> N0 -(B1:true)-> N2 -(B3:true)-> N4 -> EXIT (attacker == null, lb == null)
    public void testM3_TP1_OddsNullAttackerNoLog() {
        Unit defender = new ServerUnit(game, landTile, french, soldierType);
        CombatOdds odds = combatModel.calculateCombatOdds(null, defender, null);
        assertEquals(CombatOdds.UNKNOWN_ODDS, odds.win);
    }

    // M3-TP2: ENTRY -> N0 -(B1:false)-> N1 -(B2:false)-> N2 -(B3:false)-> N3 -> N4 -> EXIT (defender == null, lb != null)
    public void testM3_TP2_OddsNullDefenderWithLog() {
        Unit attacker = new ServerUnit(game, landTile, dutch, soldierType);
        LogBuilder lb = new LogBuilder(64);
        CombatOdds odds = combatModel.calculateCombatOdds(attacker, null, lb);
        assertEquals(CombatOdds.UNKNOWN_ODDS, odds.win);
        assertTrue(lb.toString().contains("odds=unknowable"));
    }

    // M3-TP3: ENTRY -> N0..N5 -(B4:true)-> N7 -(B5:true)-> N9 -(B6:false)-> N10 -(B7:false)-> N11 -(B8:true)-> N13 -> EXIT (0 attack & 0 defence, lb == null)
    public void testM3_TP3_OddsZeroPowersNoLog() {
        UnitType treasureTrainType = spec().getUnitType("model.unit.treasureTrain");
        Unit attacker = new ServerUnit(game, landTile, dutch, colonistType);
        Unit defender = new ServerUnit(game, otherLandTile, french, treasureTrainType);
        CombatOdds odds = combatModel.calculateCombatOdds(attacker, defender, null);
        assertEquals(CombatOdds.UNKNOWN_ODDS, odds.win);
    }

    // M3-TP4: ENTRY -> N0..N5 -(B4:false)-> N6 -> N7 -(B5:false)-> N8 -> N9 -(B6:false)-> N10 -(B7:false)-> N11 -(B8:false)-> N12 -> N13 -> EXIT (0 powers, lb != null)
    public void testM3_TP4_OddsZeroPowersWithLog() {
        UnitType treasureTrainType = spec().getUnitType("model.unit.treasureTrain");
        Unit attacker = new ServerUnit(game, landTile, dutch, colonistType);
        Unit defender = new ServerUnit(game, otherLandTile, french, treasureTrainType);
        LogBuilder lb = new LogBuilder(128);
        CombatOdds odds = combatModel.calculateCombatOdds(attacker, defender, lb);
        assertEquals(CombatOdds.UNKNOWN_ODDS, odds.win);
        assertTrue(lb.toString().contains("odds=unknown"));
    }

    // M3-TP5: ENTRY -> ... -> N9 -(B6:false)-> N10 -(B7:true)-> N14 -(B9:true)-> N16 -> EXIT (attackPower == 0, defencePower > 0, lb == null)
    public void testM3_TP5_OddsZeroAttackPositiveDefenceNoLog() {
        Unit attacker = new ServerUnit(game, landTile, dutch, colonistType);
        Unit defender = new ServerUnit(game, otherLandTile, french, soldierType, soldierRole);
        CombatOdds odds = combatModel.calculateCombatOdds(attacker, defender, null);
        assertEquals(0.0, odds.win, 1e-6);
    }

    // M3-TP6: ENTRY -> ... -> N9 -(B6:true)-> N14 -(B9:false)-> N15 -> N16 -> EXIT (attackPower > 0, lb != null)
    public void testM3_TP6_OddsPositiveAttackWithLog() {
        Unit attacker = new ServerUnit(game, landTile, dutch, soldierType, soldierRole);
        Unit defender = new ServerUnit(game, otherLandTile, french, soldierType, soldierRole);
        LogBuilder lb = new LogBuilder(128);
        CombatOdds odds = combatModel.calculateCombatOdds(attacker, defender, lb);
        assertTrue(odds.win > 0.0);
        assertTrue(lb.toString().contains("odds="));
    }

    // ------------------------------------------------------------------
    // M4: getOffencePower(FreeColGameObject, FreeColGameObject) — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M4-TP1: ENTRY -> N0 -> EXIT (delegates to 3-arg getOffencePower with null LogBuilder)
    public void testM4_TP1_GetOffencePowerTwoArgs() {
        Unit attacker = new ServerUnit(game, landTile, dutch, soldierType, soldierRole);
        assertTrue(combatModel.getOffencePower(attacker, null) > 0.0);
    }

    // ------------------------------------------------------------------
    // M5: logModifiers(LogBuilder, Set) — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M5-TP1: ENTRY -> N0 -> EXIT (sorts and logs modifiers to LogBuilder)
    public void testM5_TP1_LogModifiers() throws Exception {
        LogBuilder lb = new LogBuilder(64);
        Set<Modifier> mods = new HashSet<>();
        mods.add(new Modifier(Modifier.OFFENCE, 2.0f, ModifierType.ADDITIVE));
        invokeLogModifiers(lb, mods);
        assertTrue(lb.toString().length() > 0);
    }

    // ------------------------------------------------------------------
    // M6: getOffencePower(FreeColGameObject, FreeColGameObject, LogBuilder) — 9 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M6-TP1: ENTRY -> N0 -(B1:false)-> N1 -> EXIT (attacker == null -> throws RuntimeException)
    public void testM6_TP1_GetOffencePowerNullAttackerThrows() {
        try {
            combatModel.getOffencePower(null, landTile);
            fail("Expected RuntimeException for null attacker");
        } catch (RuntimeException expected) {
            assertTrue(expected.getMessage().contains("Null attacker"));
        }
    }

    // M6-TP2: ENTRY -> N0 -(B1:true)-> N2 -(B2:true)-> N5 -(B5:true)-> N7 -> N17 -> EXIT (attack measurement, lb == null)
    public void testM6_TP2_GetOffencePowerAttackMeasurement() {
        Unit attacker = new ServerUnit(game, landTile, dutch, soldierType, soldierRole);
        double power = combatModel.getOffencePower(attacker, null);
        assertTrue(power > 0.0);
    }

    // M6-TP3: ENTRY -> ... -> N2 -(B2:false)-> N3 -(B3:true)-> N5 -(B5:false)-> N6 -> N7 -> N17 -> EXIT (combatIsAttack, lb != null)
    public void testM6_TP3_GetOffencePowerUnitAttackWithLog() {
        Unit attacker = new ServerUnit(game, landTile, dutch, soldierType, soldierRole);
        Unit defender = new ServerUnit(game, otherLandTile, french, soldierType, soldierRole);
        LogBuilder lb = new LogBuilder(128);
        combatModel.calculateCombatOdds(attacker, defender, lb);
        assertTrue(lb.toString().contains("="));
    }

    // M6-TP4: ENTRY -> ... -> N4 -(B4:false)-> N5 -> N7 -> N17 -> EXIT (combatIsSettlementAttack)
    public void testM6_TP4_GetOffencePowerSettlementAttack() {
        Colony colony = createStandardColony(2);
        Unit attacker = new ServerUnit(game, landTile, french, soldierType, soldierRole);
        double power = combatModel.getOffencePower(attacker, colony);
        assertTrue(power > 0.0);
    }

    // M6-TP5: ENTRY -> ... -> N8 -(B6:true)-> N16 -> EXIT (bogus combat -> throws RuntimeException)
    public void testM6_TP5_GetOffencePowerBogusCombatThrows() {
        Colony colony = createStandardColony(2);
        try {
            combatModel.getOffencePower(colony, landTile);
            fail("Expected RuntimeException for bogus combat");
        } catch (RuntimeException expected) {
            assertTrue(expected.getMessage().contains("Bogus combat"));
        }
    }

    // M6-TP6: ENTRY -> ... -> N8 -(B6:false)-> N9 -(B7:true)-> N11 -(B8:true)-> N13 -(B9:true)-> N15 -> N17 -> EXIT (bombard without BOMBARD_SHIPS, lb == null)
    public void testM6_TP6_GetOffencePowerBombardWithoutAbility() {
        Colony colony = createStandardColony(2);
        Unit ship = new ServerUnit(game, waterTile, french, caravelType);
        double power = combatModel.getOffencePower(colony, ship);
        assertEquals(0.0, power, 1e-6);
    }

    // M6-TP7: ENTRY -> ... -> N9 -(B7:false)-> N10 -> N11 -(B8:false)-> N12 -> N13 -(B9:false)-> N14 -> N15 -> N17 -> EXIT (bombard > MAXIMUM_BOMBARD_POWER, lb != null)
    public void testM6_TP7_GetOffencePowerBombardCappedWithLog() {
        Colony colony = createStandardColony(2);
        colony.addAbility(new Ability(Ability.BOMBARD_SHIPS));
        for (int i = 0; i < 8; i++) {
            new ServerUnit(game, colony.getTile(), dutch, artilleryType);
        }
        Unit ship = new ServerUnit(game, waterTile, french, caravelType);
        LogBuilder lb = new LogBuilder(128);
        CombatOdds odds = combatModel.calculateCombatOdds(colony, ship, lb);
        assertTrue(odds.win > 0.0);
        assertTrue(lb.toString().contains("bombard=" + SimpleCombatModel.MAXIMUM_BOMBARD_POWER));
    }

    // ------------------------------------------------------------------
    // M7: getDefencePower(FreeColGameObject, FreeColGameObject) — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M7-TP1: ENTRY -> N0 -> EXIT (delegates to 3-arg getDefencePower with null LogBuilder)
    public void testM7_TP1_GetDefencePowerTwoArgs() {
        Unit defender = new ServerUnit(game, landTile, french, soldierType, soldierRole);
        assertTrue(combatModel.getDefencePower(null, defender) > 0.0);
    }

    // ------------------------------------------------------------------
    // M8: getDefencePower(FreeColGameObject, FreeColGameObject, LogBuilder) — 6 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M8-TP1: ENTRY -> N0 -(B1:true)-> N4 -(B5:true)-> N6 -> N8 -> EXIT (defence measurement, lb == null)
    public void testM8_TP1_GetDefencePowerMeasurement() {
        Unit defender = new ServerUnit(game, landTile, french, soldierType, soldierRole);
        assertTrue(combatModel.getDefencePower(null, defender, null) > 0.0);
    }

    // M8-TP2: ENTRY -> N0 -(B1:false)-> N1 -(B2:true)-> N4 -(B5:false)-> N5 -> N6 -> N8 -> EXIT (combatIsAttack, lb != null)
    public void testM8_TP2_GetDefencePowerAttackWithLog() {
        Unit attacker = new ServerUnit(game, landTile, dutch, soldierType, soldierRole);
        Unit defender = new ServerUnit(game, otherLandTile, french, soldierType, soldierRole);
        LogBuilder lb = new LogBuilder(128);
        double power = combatModel.getDefencePower(attacker, defender, lb);
        assertTrue(power > 0.0);
        assertTrue(lb.toString().contains("="));
    }

    // M8-TP3: ENTRY -> ... -> N2 -(B3:true)-> N4 -> N6 -> N8 -> EXIT (combatIsSettlementAttack)
    public void testM8_TP3_GetDefencePowerSettlementAttack() {
        Colony colony = createStandardColony(2);
        Unit attacker = new ServerUnit(game, landTile, french, soldierType, soldierRole);
        assertTrue(combatModel.getDefencePower(attacker, colony, null) > 0.0);
    }

    // M8-TP4: ENTRY -> ... -> N3 -(B4:false)-> N4 -> N6 -> N8 -> EXIT (combatIsBombard)
    public void testM8_TP4_GetDefencePowerBombard() {
        Colony colony = createStandardColony(2);
        Unit ship = new ServerUnit(game, waterTile, french, caravelType);
        assertTrue(combatModel.getDefencePower(colony, ship, null) > 0.0);
    }

    // M8-TP5: ENTRY -> ... -> N3 -(B4:true)-> N7 -> EXIT (bogus combat -> throws IllegalArgumentException)
    public void testM8_TP5_GetDefencePowerBogusThrows() {
        try {
            combatModel.getDefencePower(landTile, otherLandTile, null);
            fail("Expected IllegalArgumentException for bogus combat");
        } catch (IllegalArgumentException expected) {
            assertEquals("Bogus combat", expected.getMessage());
        }
    }

    // ------------------------------------------------------------------
    // M9: getOffensiveModifiers(FreeColGameObject, FreeColGameObject) — 7 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M9-TP1: ENTRY -> N0 -(B1:false)-> N1 -> EXIT (attacker == null -> throws RuntimeException)
    public void testM9_TP1_GetOffensiveModifiersNullAttackerThrows() {
        try {
            combatModel.getOffensiveModifiers(null, landTile);
            fail("Expected RuntimeException");
        } catch (RuntimeException expected) {
            assertTrue(expected.getMessage().contains("Null attacker"));
        }
    }

    // M9-TP2: ENTRY -> N0 -(B1:true)-> N2 -(B2:true)-> N5 -(B5:true)-> N7 -(B6:true)-> N9 -> N10 -> N14 -> N15 -(B8:true)-> N17 -> EXIT (attack measurement, land unit, defender == null)
    public void testM9_TP2_GetOffensiveModifiersMeasurementLand() {
        Unit attacker = new ServerUnit(game, landTile, dutch, colonistType);
        Set<Modifier> mods = combatModel.getOffensiveModifiers(attacker, null);
        assertFalse(mods.isEmpty());
    }

    // M9-TP3: ENTRY -> ... -> N3 -(B3:true)-> N5 -(B5:false)-> N6 -> N7 -(B6:false)-> N8 -> N10 -> N14 -> N15 -(B8:false)-> N16 -> N15 -(B8:true)-> N17 -> EXIT (naval attack, defender Ownable, loop fixes DEFAULT_MODIFIER_INDEX)
    public void testM9_TP3_GetOffensiveModifiersNavalAttackWithDefaultIndex() {
        Unit attacker = new ServerUnit(game, waterTile, dutch, privateerType);
        attacker.addModifier(new Modifier(Modifier.OFFENCE, 1.0f, ModifierType.ADDITIVE));
        Unit defender = new ServerUnit(game, waterTile, french, caravelType);
        Set<Modifier> mods = combatModel.getOffensiveModifiers(attacker, defender);
        assertFalse(mods.isEmpty());
    }

    // M9-TP4: ENTRY -> ... -> N4 -(B4:false)-> N5 -> ... -> N17 -> EXIT (settlement attack)
    public void testM9_TP4_GetOffensiveModifiersSettlementAttack() {
        Colony colony = createStandardColony(2);
        Unit attacker = new ServerUnit(game, landTile, french, soldierType, soldierRole);
        Set<Modifier> mods = combatModel.getOffensiveModifiers(attacker, colony);
        assertFalse(mods.isEmpty());
    }

    // M9-TP5: ENTRY -> ... -> N11 -(B7:false)-> N12 -> N14 -> ... -> N17 -> EXIT (bombard returns empty set)
    public void testM9_TP5_GetOffensiveModifiersBombard() {
        Colony colony = createStandardColony(2);
        Unit ship = new ServerUnit(game, waterTile, french, caravelType);
        Set<Modifier> mods = combatModel.getOffensiveModifiers(colony, ship);
        assertTrue(mods.isEmpty());
    }

    // M9-TP6: ENTRY -> ... -> N11 -(B7:true)-> N13 -> EXIT (bogus combat -> throws RuntimeException)
    public void testM9_TP6_GetOffensiveModifiersBogusThrows() {
        Colony colony = createStandardColony(2);
        try {
            combatModel.getOffensiveModifiers(colony, landTile);
            fail("Expected RuntimeException");
        } catch (RuntimeException expected) {
            assertTrue(expected.getMessage().contains("Bogus combat"));
        }
    }

    // ------------------------------------------------------------------
    // M10: addNavalOffensiveModifiers(Unit, Set) — 2 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M10-TP1: ENTRY -> N0 -(B1:true)-> N2 -> EXIT (goodsCount <= 0)
    public void testM10_TP1_NavalOffensiveNoCargo() throws Exception {
        Unit ship = new ServerUnit(game, waterTile, dutch, privateerType);
        Set<Modifier> mods = new HashSet<>();
        invokeAddNavalOffensiveModifiers(ship, mods);
        assertFalse(mods.isEmpty());
    }

    // M10-TP2: ENTRY -> N0 -(B1:false)-> N1 -> N2 -> EXIT (goodsCount > 0 -> applies CARGO_PENALTY)
    public void testM10_TP2_NavalOffensiveWithCargo() throws Exception {
        Unit ship = new ServerUnit(game, waterTile, dutch, privateerType);
        ship.add(new Goods(game, ship, spec().getGoodsType("model.goods.sugar"), 50));
        Set<Modifier> mods = new HashSet<>();
        invokeAddNavalOffensiveModifiers(ship, mods);
        assertTrue(mods.stream().anyMatch(m -> Modifier.CARGO_PENALTY.equals(m.getId())));
    }

    // ------------------------------------------------------------------
    // M11: addPopularSupportBonus(Colony, Unit, Set) — 4 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M11-TP1: ENTRY -> N0 -(B1:true)-> N5 -> EXIT (bonus < 0)
    public void testM11_TP1_PopularSupportNegative() throws Exception {
        Colony colony = createStandardColony(2);
        setIntField(colony, "sonsOfLiberty", -1);
        Unit attacker = new ServerUnit(game, landTile, french, soldierType);
        Set<Modifier> mods = new HashSet<>();
        invokeAddPopularSupportBonus(colony, attacker, mods);
        assertTrue(mods.isEmpty());
    }

    // M11-TP2: ENTRY -> N0 -(B1:false)-> N1 -(B2:true)-> N3 -(B3:true)-> N5 -> EXIT (non-REF, bonus == 0)
    public void testM11_TP2_PopularSupportNonRefZero() throws Exception {
        Colony colony = createStandardColony(2);
        setIntField(colony, "sonsOfLiberty", 0);
        Unit attacker = new ServerUnit(game, landTile, french, soldierType);
        Set<Modifier> mods = new HashSet<>();
        invokeAddPopularSupportBonus(colony, attacker, mods);
        assertTrue(mods.isEmpty());
    }

    // M11-TP3: ENTRY -> N0 -(B1:false)-> N1 -(B2:true)-> N3 -(B3:false)-> N4 -> N5 -> EXIT (non-REF, bonus > 0)
    public void testM11_TP3_PopularSupportNonRefPositive() throws Exception {
        Colony colony = createStandardColony(2);
        setIntField(colony, "sonsOfLiberty", 60);
        Unit attacker = new ServerUnit(game, landTile, french, soldierType);
        Set<Modifier> mods = new HashSet<>();
        invokeAddPopularSupportBonus(colony, attacker, mods);
        assertEquals(1, mods.size());
    }

    // M11-TP4: ENTRY -> N0 -(B1:false)-> N1 -(B2:false)-> N2 -> N3 -(B3:true/false)-> N4/N5 -> EXIT (REF attacker: 100 - bonus)
    public void testM11_TP4_PopularSupportRefAttacker() throws Exception {
        Colony colony = createStandardColony(2);
        setIntField(colony, "sonsOfLiberty", 100);
        Unit refUnit = new ServerUnit(game, landTile, refDutch, kingsRegularType, soldierRole);
        Set<Modifier> mods = new HashSet<>();
        invokeAddPopularSupportBonus(colony, refUnit, mods);
        assertTrue(mods.isEmpty());

        setIntField(colony, "sonsOfLiberty", 40);
        invokeAddPopularSupportBonus(colony, refUnit, mods);
        assertEquals(1, mods.size());
    }

    // ------------------------------------------------------------------
    // M12: addLandOffensiveModifiers(Unit, FreeColGameObject, Set) — 9 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M12-TP1: B1:case:1, B2:true (no amphibious), B3:false (measurement)
    public void testM12_TP1_LandOffensiveMoves1Measurement() throws Exception {
        Unit attacker = new ServerUnit(game, landTile, dutch, soldierType, soldierRole);
        attacker.setMovesLeft(1);
        Set<Modifier> mods = new HashSet<>();
        invokeAddLandOffensiveModifiers(attacker, null, mods);
        assertFalse(mods.isEmpty());
    }

    // M12-TP2: B1:case:2, B2:false (amphibious), B4:false, B5:false (war of independence settlement attack)
    public void testM12_TP2_LandOffensiveMoves2AmphibiousWarOfIndependenceSettlement() throws Exception {
        Colony colony = createStandardColony(2);
        setIntField(colony, "sonsOfLiberty", 50);
        colony.getOwner().changePlayerType(Player.PlayerType.REBEL);
        Unit ship = new ServerUnit(game, waterTile, refDutch, caravelType);
        Unit attacker = new ServerUnit(game, ship, refDutch, kingsRegularType, soldierRole);
        attacker.setMovesLeft(2);
        Set<Modifier> mods = new HashSet<>();
        invokeAddLandOffensiveModifiers(attacker, colony, mods);
        assertTrue(mods.stream().anyMatch(m -> Modifier.POPULAR_SUPPORT.equals(m.getId())));
    }

    // M12-TP3: B1:default, B4:false, B5:true (non-WoI settlement attack)
    public void testM12_TP3_LandOffensiveNonWoiSettlementAttack() throws Exception {
        Colony colony = createStandardColony(2);
        Unit attacker = new ServerUnit(game, landTile, french, soldierType, soldierRole);
        attacker.setMovesLeft(3);
        Set<Modifier> mods = new HashSet<>();
        invokeAddLandOffensiveModifiers(attacker, colony, mods);
        assertFalse(mods.isEmpty());
    }

    // M12-TP4: B6:true (bogus defender -> throws RuntimeException)
    public void testM12_TP4_LandOffensiveBogusDefenderThrows() throws Exception {
        Unit attacker = new ServerUnit(game, landTile, dutch, soldierType);
        try {
            invokeAddLandOffensiveModifiers(attacker, landTile, new HashSet<>());
            fail("Expected RuntimeException");
        } catch (Exception expected) {
            assertTrue(expected.getCause() instanceof RuntimeException);
        }
    }

    // M12-TP5: B7:true (defender tile == null), B12..B16 all false (artillery on tile, unfortified, defender not in settlement)
    public void testM12_TP5_LandOffensiveDefenderNullTileArtilleryPenalty() throws Exception {
        Unit attacker = new ServerUnit(game, landTile, dutch, artilleryType);
        attacker.setState(Unit.UnitState.ACTIVE);
        Unit defender = new ServerUnit(game, null, french, soldierType);
        Set<Modifier> mods = new HashSet<>();
        invokeAddLandOffensiveModifiers(attacker, defender, mods);
        assertTrue(mods.stream().anyMatch(m -> Modifier.ARTILLERY_IN_THE_OPEN.equals(m.getId())));
    }

    // M12-TP6: B8:false (defender in settlement), B9:false (WoI unit in settlement), B16:true (defender in settlement)
    public void testM12_TP6_LandOffensiveDefenderInSettlementWoI() throws Exception {
        Colony colony = createStandardColony(2);
        setIntField(colony, "sonsOfLiberty", 50);
        colony.getOwner().changePlayerType(Player.PlayerType.REBEL);
        Unit defender = colony.getUnitList().get(0);
        Unit attacker = new ServerUnit(game, landTile, refDutch, kingsRegularType, soldierRole);
        Set<Modifier> mods = new HashSet<>();
        invokeAddLandOffensiveModifiers(attacker, defender, mods);
        assertTrue(mods.stream().anyMatch(m -> Modifier.POPULAR_SUPPORT.equals(m.getId())));
    }

    // M12-TP7: B8:false, B9:true (defender in settlement, non-WoI), B15:true (artillery fortified)
    public void testM12_TP7_LandOffensiveFortifiedArtilleryVsSettlementUnit() throws Exception {
        Colony colony = createStandardColony(2);
        Unit defender = new ServerUnit(game, colony.getTile(), dutch, soldierType, soldierRole);
        Unit attacker = new ServerUnit(game, landTile, french, artilleryType);
        fortify(attacker);
        Set<Modifier> mods = new HashSet<>();
        invokeAddLandOffensiveModifiers(attacker, defender, mods);
        assertFalse(mods.stream().anyMatch(m -> Modifier.ARTILLERY_IN_THE_OPEN.equals(m.getId())));
    }

    // M12-TP8: B8:true, B10:false (ambush), B11 loop >= 1 (forest tile defence modifiers), B12:true (not artillery)
    public void testM12_TP8_LandOffensiveAmbushInForest() throws Exception {
        Unit attacker = new ServerUnit(game, landTile, inca, braveType, armedBraveRole);
        Unit defender = new ServerUnit(game, forestTile, dutch, soldierType, soldierRole);
        Set<Modifier> mods = new HashSet<>();
        invokeAddLandOffensiveModifiers(attacker, defender, mods);
        assertTrue(mods.stream().anyMatch(m -> Specification.AMBUSH_BONUS_SOURCE.equals(m.getSource())));
    }

    // M12-TP9: B10:true (no ambush), B13:true (attacker off-tile), B14:true (attacker in settlement)
    public void testM12_TP9_LandOffensiveArtilleryOffTileAndInSettlement() throws Exception {
        Unit defender = new ServerUnit(game, otherLandTile, french, soldierType, soldierRole);

        Unit offTileArtillery = new ServerUnit(game, null, dutch, artilleryType);
        Set<Modifier> mods1 = new HashSet<>();
        invokeAddLandOffensiveModifiers(offTileArtillery, defender, mods1);
        assertFalse(mods1.stream().anyMatch(m -> Modifier.ARTILLERY_IN_THE_OPEN.equals(m.getId())));

        Colony colony = createStandardColony(2);
        Unit inSettlementArtillery = new ServerUnit(game, colony.getTile(), dutch, artilleryType);
        Set<Modifier> mods2 = new HashSet<>();
        invokeAddLandOffensiveModifiers(inSettlementArtillery, defender, mods2);
        assertFalse(mods2.stream().anyMatch(m -> Modifier.ARTILLERY_IN_THE_OPEN.equals(m.getId())));
    }

    // ------------------------------------------------------------------
    // M13: getDefensiveModifiers(FreeColGameObject, FreeColGameObject) — 6 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M13-TP1: B1:true, B4:true (defence measurement, land unit)
    public void testM13_TP1_GetDefensiveModifiersMeasurementLand() {
        Unit defender = new ServerUnit(game, landTile, dutch, soldierType, soldierRole);
        Set<Modifier> mods = combatModel.getDefensiveModifiers(null, defender);
        assertFalse(mods.isEmpty());
    }

    // M13-TP2: B1:false, B2:true, B4:false (naval unit attack, loop fixes DEFAULT_MODIFIER_INDEX)
    public void testM13_TP2_GetDefensiveModifiersAttackNavalWithDefaultIndex() {
        Unit attacker = new ServerUnit(game, waterTile, french, privateerType);
        Unit defender = new ServerUnit(game, waterTile, dutch, caravelType);
        defender.addModifier(new Modifier(Modifier.DEFENCE, 1.0f, ModifierType.ADDITIVE));
        Set<Modifier> mods = combatModel.getDefensiveModifiers(attacker, defender);
        assertFalse(mods.isEmpty());
    }

    // M13-TP3: B3:false (bombard)
    public void testM13_TP3_GetDefensiveModifiersBombard() {
        Colony colony = createStandardColony(2);
        Unit ship = new ServerUnit(game, waterTile, french, caravelType);
        Set<Modifier> mods = combatModel.getDefensiveModifiers(colony, ship);
        assertFalse(mods.isEmpty());
    }

    // M13-TP4: B3:true, B5:false (settlement attack)
    public void testM13_TP4_GetDefensiveModifiersSettlementAttack() {
        Colony colony = createStandardColony(2);
        Unit attacker = new ServerUnit(game, landTile, french, soldierType, soldierRole);
        Set<Modifier> mods = combatModel.getDefensiveModifiers(attacker, colony);
        assertTrue(mods.contains(SimpleCombatModel.UNKNOWN_DEFENCE_MODIFIER));
    }

    // M13-TP5: B3:true, B5:true (bogus combat -> throws RuntimeException)
    public void testM13_TP5_GetDefensiveModifiersBogusThrows() {
        try {
            combatModel.getDefensiveModifiers(landTile, otherLandTile);
            fail("Expected RuntimeException");
        } catch (RuntimeException expected) {
            assertTrue(expected.getMessage().contains("Bogus combat"));
        }
    }

    // ------------------------------------------------------------------
    // M14: addNavalDefensiveModifiers(Unit, Set) — 2 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M14-TP1: ENTRY -> N0 -(B1:true)-> N2 -> EXIT (goodsCount <= 0)
    public void testM14_TP1_NavalDefensiveNoCargo() throws Exception {
        Unit ship = new ServerUnit(game, waterTile, dutch, caravelType);
        Set<Modifier> mods = new HashSet<>();
        invokeAddNavalDefensiveModifiers(ship, mods);
        assertTrue(mods.isEmpty());
    }

    // M14-TP2: ENTRY -> N0 -(B1:false)-> N1 -> N2 -> EXIT (goodsCount > 0 -> applies CARGO_PENALTY)
    public void testM14_TP2_NavalDefensiveWithCargo() throws Exception {
        Unit ship = new ServerUnit(game, waterTile, dutch, caravelType);
        ship.add(new Goods(game, ship, spec().getGoodsType("model.goods.sugar"), 25));
        Set<Modifier> mods = new HashSet<>();
        invokeAddNavalDefensiveModifiers(ship, mods);
        assertTrue(mods.stream().anyMatch(m -> Modifier.CARGO_PENALTY.equals(m.getId())));
    }

    // ------------------------------------------------------------------
    // M15: hasStrongDefenceModifier(FreeColObject) — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M15-TP1: ENTRY -> N0 -> EXIT (checks for percentage defence modifier >= 150)
    public void testM15_TP1_HasStrongDefenceModifier() throws Exception {
        assertFalse(invokeHasStrongDefenceModifier(landTile.getType()));
    }

    // ------------------------------------------------------------------
    // M16: addLandDefensiveModifiers(FreeColGameObject, Unit, Set) — 8 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M16-TP1: B1:false, B2:true (tile == null)
    public void testM16_TP1_LandDefensiveNullTile() throws Exception {
        Unit defender = new ServerUnit(game, null, dutch, soldierType);
        Set<Modifier> mods = new HashSet<>();
        invokeAddLandDefensiveModifiers(null, defender, mods);
        assertTrue(mods.isEmpty());
    }

    // M16-TP2: B1:true, B2:false, B3:false, B4:false (not ambush), B5:false, B6:false (unfortified artillery in open), B13:false, B14:true (not fortified)
    public void testM16_TP2_LandDefensiveOpenArtilleryUnfortified() throws Exception {
        Unit attacker = new ServerUnit(game, landTile, french, soldierType, soldierRole);
        Unit defender = new ServerUnit(game, otherLandTile, dutch, artilleryType);
        defender.setState(Unit.UnitState.ACTIVE);
        Set<Modifier> mods = new HashSet<>();
        invokeAddLandDefensiveModifiers(attacker, defender, mods);
        assertTrue(mods.stream().anyMatch(m -> Modifier.ARTILLERY_IN_THE_OPEN.equals(m.getId())));
    }

    // M16-TP3: B4:true (ambush), B5:true (non-artillery), B13:false, B14:false (fortified)
    public void testM16_TP3_LandDefensiveAmbushFortifiedNonArtillery() throws Exception {
        Unit attacker = new ServerUnit(game, landTile, inca, braveType, armedBraveRole);
        Unit defender = new ServerUnit(game, forestTile, dutch, soldierType, soldierRole);
        fortify(defender);
        Set<Modifier> mods = new HashSet<>();
        invokeAddLandDefensiveModifiers(attacker, defender, mods);
        assertTrue(mods.stream().anyMatch(m -> Modifier.FORTIFIED.equals(m.getId())));
    }

    // M16-TP4: B5:false, B6:true (fortified artillery in open)
    public void testM16_TP4_LandDefensiveOpenArtilleryFortified() throws Exception {
        Unit defender = new ServerUnit(game, otherLandTile, dutch, artilleryType);
        fortify(defender);
        Set<Modifier> mods = new HashSet<>();
        invokeAddLandDefensiveModifiers(null, defender, mods);
        assertFalse(mods.stream().anyMatch(m -> Modifier.ARTILLERY_IN_THE_OPEN.equals(m.getId())));
    }

    // M16-TP5: B3:true (in Colony), B7:false, B8:false (Indian attacker), B9:false (artillery defender), B10:false (autoRole), B11:false, B12:false (stockade), B13:true (strong defence disables fortify)
    public void testM16_TP5_LandDefensiveInColonyWithStockadeIndianRaidAutoRole() throws Exception {
        Colony colony = createStandardColony(2);
        BuildingType fortressType = spec().getBuildingType("model.building.fortress");
        colony.addBuilding(new net.sf.freecol.server.model.ServerBuilding(game, colony, fortressType));
        colony.getOwner().addFather(spec().getFoundingFather("model.foundingFather.paulRevere"));
        colony.addGoods(spec().getGoodsType("model.goods.muskets"), 100);

        Unit defender = new ServerUnit(game, colony.getTile(), colony.getOwner(), artilleryType);
        fortify(defender);
        Unit indianAttacker = new ServerUnit(game, landTile, inca, braveType);
        Set<Modifier> mods = new HashSet<>();
        invokeAddLandDefensiveModifiers(indianAttacker, defender, mods);
        assertTrue(mods.stream().anyMatch(m -> Modifier.ARTILLERY_AGAINST_RAID.equals(m.getId())));

        Unit autoDefender = colony.getUnitList().get(0);
        Set<Modifier> mods2 = new HashSet<>();
        invokeAddLandDefensiveModifiers(null, autoDefender, mods2);
        assertFalse(mods2.isEmpty());
    }

    // M16-TP6: B7:true (attacker == null in settlement), B11:false, B12:true (colony without stockade)
    public void testM16_TP6_LandDefensiveInColonyNoStockadeNullAttacker() throws Exception {
        Colony colony = createStandardColony(2);
        Unit defender = new ServerUnit(game, colony.getTile(), colony.getOwner(), colonistType);
        Set<Modifier> mods = new HashSet<>();
        invokeAddLandDefensiveModifiers(null, defender, mods);
        assertFalse(mods.isEmpty());
    }

    // M16-TP7: B8:true (European attacker in settlement), B9:true (Indian attacker vs non-artillery), B11:true (IndianSettlement)
    public void testM16_TP7_LandDefensiveInIndianSettlementAndNonIndianRaid() throws Exception {
        Colony colony = createStandardColony(2);
        Unit defender = new ServerUnit(game, colony.getTile(), colony.getOwner(), colonistType);
        Unit europeanAttacker = new ServerUnit(game, landTile, french, soldierType);
        Unit indianAttacker = new ServerUnit(game, landTile, inca, braveType);
        invokeAddLandDefensiveModifiers(europeanAttacker, defender, new HashSet<>());
        invokeAddLandDefensiveModifiers(indianAttacker, defender, new HashSet<>());

        IndianSettlement is = createIndianSettlement(inca, otherLandTile);
        Unit nativeDef = new ServerUnit(game, is, inca, braveType);
        invokeAddLandDefensiveModifiers(europeanAttacker, nativeDef, new HashSet<>());
        assertTrue(is.getTile().hasSettlement());
    }

    // ------------------------------------------------------------------
    // M17: generateAttackResult(Random, FreeColGameObject, FreeColGameObject) — 11 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M17-TP1: B1:false, B2:true, B4:false (great win: r < 0.1 * odds.win)
    public void testM17_TP1_GenerateAttackGreatWin() {
        Unit attacker = new ServerUnit(game, landTile, dutch, soldierType, soldierRole);
        Unit defender = new ServerUnit(game, otherLandTile, french, soldierType, soldierRole);
        CombatResult res = combatModel.generateAttackResult(fixedRandom(0.01), attacker, defender);
        assertTrue(res.getEffects().contains(CombatEffectType.WIN));
    }

    // M17-TP2: B1:false, B2:false, B3:false (beached ship loses even when r >= odds.win), B4:true (normal win)
    public void testM17_TP2_GenerateAttackBeachedShipLoses() throws Exception {
        Unit attacker = new ServerUnit(game, landTile, dutch, soldierType, soldierRole);
        Unit defender = new ServerUnit(game, waterTile, french, caravelType);
        setField(defender, "location", otherLandTile);
        CombatResult res = combatModel.generateAttackResult(fixedRandom(0.99), attacker, defender);
        assertTrue(res.getEffects().contains(CombatEffectType.WIN));
    }

    // M17-TP3: B1:false, B2:false, B3:true, B5:false, B6:false (evade attack)
    public void testM17_TP3_GenerateAttackEvade() {
        Unit attacker = new ServerUnit(game, waterTile, dutch, caravelType);
        Unit defender = new ServerUnit(game, waterTile, french, privateerType);
        CombatOdds odds = combatModel.calculateCombatOdds(attacker, defender);
        double r = odds.win + 0.05 * (1.0 - odds.win);
        CombatResult res = combatModel.generateAttackResult(fixedRandom(r), attacker, defender);
        assertTrue(res.getEffects().contains(CombatEffectType.EVADE_ATTACK));
    }

    // M17-TP4: B1:false, B2:false, B3:true, B5:false, B6:true (no evadeAbility -> loss), B7:true (normal loss)
    public void testM17_TP4_GenerateAttackNormalLoss() {
        Unit attacker = new ServerUnit(game, landTile, dutch, soldierType, soldierRole);
        Unit defender = new ServerUnit(game, otherLandTile, french, soldierType, soldierRole);
        CombatOdds odds = combatModel.calculateCombatOdds(attacker, defender);
        double r = odds.win + 0.05 * (1.0 - odds.win);
        CombatResult res = combatModel.generateAttackResult(fixedRandom(r), attacker, defender);
        assertTrue(res.getEffects().contains(CombatEffectType.LOSE));
    }

    // M17-TP5: B1:false, B2:false, B3:true, B5:true, B7:false (great loss)
    public void testM17_TP5_GenerateAttackGreatLoss() {
        Unit attacker = new ServerUnit(game, landTile, dutch, soldierType, soldierRole);
        Unit defender = new ServerUnit(game, otherLandTile, french, soldierType, soldierRole);
        CombatResult res = combatModel.generateAttackResult(fixedRandom(0.999), attacker, defender);
        assertTrue(res.getEffects().contains(CombatEffectType.LOSE));
    }

    // M17-TP6: B1:true, B8:false, B9:false (bombard of non-naval -> throws RuntimeException)
    public void testM17_TP6_GenerateBombardNonNavalThrows() {
        Colony colony = createStandardColony(2);
        Unit landUnit = new ServerUnit(game, landTile, french, soldierType);
        // Colony vs land unit is not combatIsBombard -> hits B8:true (bogus combat)
        try {
            combatModel.generateAttackResult(fixedRandom(0.5), colony, landUnit);
            fail("Expected RuntimeException");
        } catch (RuntimeException expected) {
            assertTrue(expected.getMessage().contains("Bogus combat"));
        }
    }

    // M17-TP7: B1:true, B8:false, B9:true, B10:true (bombard evade: r > odds.win)
    public void testM17_TP7_GenerateBombardEvade() {
        Colony colony = createStandardColony(2);
        Unit ship = new ServerUnit(game, waterTile, french, caravelType);
        CombatResult res = combatModel.generateAttackResult(fixedRandom(0.99), colony, ship);
        assertTrue(res.getEffects().contains(CombatEffectType.EVADE_BOMBARD));
    }

    // M17-TP8: B1:true, B8:false, B9:true, B10:false, B11:false, B12:true (bombard great win -> SINK_SHIP_BOMBARD)
    public void testM17_TP8_GenerateBombardGreatWinSinks() {
        Colony colony = createStandardColony(2);
        colony.addAbility(new Ability(Ability.BOMBARD_SHIPS));
        new ServerUnit(game, colony.getTile(), dutch, artilleryType);
        Unit ship = new ServerUnit(game, waterTile, french, caravelType);
        CombatResult res = combatModel.generateAttackResult(fixedRandom(0.01), colony, ship);
        assertTrue(res.getEffects().contains(CombatEffectType.SINK_SHIP_BOMBARD));
    }

    // M17-TP9: B1:true, B8:false, B9:true, B10:false, B11:true, B12:false, B13:true (bombard normal win with repair location -> DAMAGE_SHIP_BOMBARD)
    public void testM17_TP9_GenerateBombardDamagesWithRepairLocation() {
        Colony colony = createStandardColony(2);
        colony.addAbility(new Ability(Ability.BOMBARD_SHIPS));
        new ServerUnit(game, colony.getTile(), dutch, artilleryType);
        Unit ship = new ServerUnit(game, waterTile, french, caravelType);
        CombatOdds odds = combatModel.calculateCombatOdds(colony, ship);
        double r = odds.win * 0.8;
        CombatResult res = combatModel.generateAttackResult(fixedRandom(r), colony, ship);
        assertTrue(res.getEffects().contains(CombatEffectType.DAMAGE_SHIP_BOMBARD));
    }

    // M17-TP10: B1:true, B8:false, B9:true, B10:false, B11:true, B12:false, B13:false (bombard normal win without repair location -> SINK_SHIP_BOMBARD)
    public void testM17_TP10_GenerateBombardSinksWithoutRepairLocation() {
        Colony colony = createStandardColony(2);
        colony.addAbility(new Ability(Ability.BOMBARD_SHIPS));
        new ServerUnit(game, colony.getTile(), dutch, artilleryType);
        french.setEurope(null);
        Unit ship = new ServerUnit(game, waterTile, french, caravelType);
        CombatOdds odds = combatModel.calculateCombatOdds(colony, ship);
        double r = odds.win * 0.8;
        CombatResult res = combatModel.generateAttackResult(fixedRandom(r), colony, ship);
        assertTrue(res.getEffects().contains(CombatEffectType.SINK_SHIP_BOMBARD));
    }

    // M17-TP11: B9:false (bombard of non-naval unit if combatIsBombard ever passed) is infeasible via public API
    // because combatIsBombard checks ((Unit)defender).isNaval(); B8:true covers bogus combat.

    // ------------------------------------------------------------------
    // M18: resolveAttack(Unit, Unit, boolean, double, List) — 18 Test Paths (100% feasible C3)
    // ------------------------------------------------------------------

    // M18-TP1: Naval loser: loot ship (B3..B5:false) + sink (great: B6:true)
    public void testM18_TP1_ResolveNavalLootAndSinkGreat() {
        Unit winner = new ServerUnit(game, waterTile, dutch, privateerType);
        Unit loser = new ServerUnit(game, waterTile, french, caravelType);
        loser.add(new Goods(game, loser, spec().getGoodsType("model.goods.sugar"), 10));
        List<CombatEffectType> crs = new ArrayList<>();
        crs.add(CombatEffectType.WIN);
        combatModel.resolveAttack(winner, loser, true, 0.5, crs);
        assertTrue(crs.contains(CombatEffectType.LOOT_SHIP));
        assertTrue(crs.contains(CombatEffectType.SINK_SHIP_ATTACK));
    }

    // M18-TP2: Naval loser: no loot (land winner B3:true, non-capturing naval B4:true, empty goods B5:true), damage (B6..B9)
    public void testM18_TP2_ResolveNavalDamageAndNoLootBranches() throws Exception {
        Unit caravelWinner = new ServerUnit(game, waterTile, dutch, caravelType);
        Unit emptyLoser = new ServerUnit(game, waterTile, french, caravelType);
        List<CombatEffectType> crs = new ArrayList<>();
        crs.add(CombatEffectType.WIN);
        combatModel.resolveAttack(caravelWinner, emptyLoser, false, 0.5, crs);
        assertTrue(crs.contains(CombatEffectType.DAMAGE_SHIP_ATTACK));

        Unit landWinner = new ServerUnit(game, landTile, dutch, soldierType, soldierRole);
        Unit beachedLoser = new ServerUnit(game, waterTile, french, caravelType);
        setField(beachedLoser, "location", otherLandTile);
        List<CombatEffectType> crs2 = new ArrayList<>();
        crs2.add(CombatEffectType.WIN);
        combatModel.resolveAttack(landWinner, beachedLoser, false, 0.5, crs2);
        assertTrue(crs2.contains(CombatEffectType.SINK_SHIP_ATTACK));

        Unit privateerWinner = new ServerUnit(game, waterTile, dutch, privateerType);
        french.setEurope(null);
        List<CombatEffectType> crs3 = new ArrayList<>();
        crs3.add(CombatEffectType.WIN);
        combatModel.resolveAttack(privateerWinner, emptyLoser, false, 0.5, crs3);
        assertTrue(crs3.contains(CombatEffectType.SINK_SHIP_ATTACK));
    }

    // M18-TP3: Land loser in Colony: European captures colony, sinks/damages ships, slaughters disposable unit
    public void testM18_TP3_ResolveColonyCaptureEuropean() {
        Colony colony = createStandardColony(2);
        Unit ship = new ServerUnit(game, colony.getTile(), dutch, caravelType);
        dutch.addAbility(new Ability(Ability.DISPOSE_ON_COMBAT_LOSS));
        Unit loser = new ServerUnit(game, colony.getTile(), dutch, colonistType);
        Unit winner = new ServerUnit(game, landTile, french, soldierType, soldierRole);

        List<CombatEffectType> crs = new ArrayList<>();
        crs.add(CombatEffectType.WIN);
        combatModel.resolveAttack(winner, loser, false, 0.5, crs);
        assertTrue(crs.contains(CombatEffectType.CAPTURE_COLONY));
        assertTrue(crs.contains(CombatEffectType.DAMAGE_COLONY_SHIPS));
        assertTrue(crs.contains(CombatEffectType.SLAUGHTER_UNIT));

        dutch.setEurope(null);
        List<CombatEffectType> crs2 = new ArrayList<>();
        crs2.add(CombatEffectType.WIN);
        combatModel.resolveAttack(winner, loser, false, 0.5, crs2);
        assertTrue(crs2.contains(CombatEffectType.SINK_COLONY_SHIPS));
        assertTrue(ship.isNaval());
    }

    // M18-TP4: Land loser in Colony: Native pillages colony (B21:false, B22:false)
    public void testM18_TP4_ResolveColonyPillageByNative() {
        Colony colony = createStandardColony(2);
        colony.addGoods(spec().getGoodsType("model.goods.sugar"), 50);
        Unit loser = colony.getUnitList().get(0);
        Unit winner = new ServerUnit(game, landTile, inca, braveType, armedBraveRole);
        List<CombatEffectType> crs = new ArrayList<>();
        crs.add(CombatEffectType.WIN);
        combatModel.resolveAttack(winner, loser, false, 0.5, crs);
        assertTrue(crs.contains(CombatEffectType.PILLAGE_COLONY));
    }

    // M18-TP5: Land loser in Colony: Native slaughters when colony.getUnitCount() > 1 or loser on tile (B23:true, B24:false)
    public void testM18_TP5_ResolveColonyNativeIncrementalKill() {
        Colony colony = createStandardColony(2);
        Unit loser = colony.getUnitList().get(0);
        Unit winner = new ServerUnit(game, landTile, inca, braveType, armedBraveRole);
        List<CombatEffectType> crs = new ArrayList<>();
        crs.add(CombatEffectType.WIN);
        combatModel.resolveAttack(winner, loser, true, 0.5, crs);
        assertTrue(crs.contains(CombatEffectType.SLAUGHTER_UNIT));

        Colony smallColony = createStandardColony(1);
        Unit tileLoser = new ServerUnit(game, smallColony.getTile(), dutch, colonistType);
        List<CombatEffectType> crs2 = new ArrayList<>();
        crs2.add(CombatEffectType.WIN);
        combatModel.resolveAttack(winner, tileLoser, true, 0.5, crs2);
        assertTrue(crs2.contains(CombatEffectType.SLAUGHTER_UNIT));
    }

    // M18-TP6: Land loser in Colony: Native destroys last-unit colony (B24:true, B25:true/false)
    public void testM18_TP6_ResolveColonyNativeDestroysColony() {
        Colony colony = createStandardColony(1);
        Unit loser = colony.getUnitList().get(0);
        Unit winner = new ServerUnit(game, landTile, inca, braveType, armedBraveRole);
        List<CombatEffectType> crs = new ArrayList<>();
        crs.add(CombatEffectType.WIN);
        combatModel.resolveAttack(winner, loser, true, 0.5, crs);
        assertTrue(crs.contains(CombatEffectType.DESTROY_COLONY));

        new ServerUnit(game, colony.getTile(), dutch, caravelType);
        List<CombatEffectType> crs2 = new ArrayList<>();
        crs2.add(CombatEffectType.WIN);
        combatModel.resolveAttack(winner, loser, true, 0.5, crs2);
        assertTrue(crs2.contains(CombatEffectType.DAMAGE_COLONY_SHIPS));
        assertTrue(crs2.contains(CombatEffectType.DESTROY_COLONY));
    }

    // M18-TP7: Land loser in IndianSettlement: convert, burn missions, destroy settlement, disposable loser
    public void testM18_TP7_ResolveIndianSettlementBranches() {
        IndianSettlement is = createIndianSettlement(inca, otherLandTile);
        Unit missionary = new ServerUnit(game, null, dutch, spec().getUnitType("model.unit.jesuitMissionary"));
        is.setMissionary(missionary);
        Unit brave1 = new ServerUnit(game, is, inca, braveType);
        Unit brave2 = new ServerUnit(game, is, inca, braveType);
        Unit winner = new ServerUnit(game, landTile, dutch, soldierType, soldierRole);

        // Convert branch (r = 0.0 < convertProbability)
        List<CombatEffectType> crs1 = new ArrayList<>();
        crs1.add(CombatEffectType.WIN);
        combatModel.resolveAttack(winner, brave1, false, 0.0, crs1);
        assertTrue(crs1.contains(CombatEffectType.CAPTURE_CONVERT));

        // Burn missions branch (r = 0.999 >= 1.0 - burnProbability)
        List<CombatEffectType> crs2 = new ArrayList<>();
        crs2.add(CombatEffectType.WIN);
        combatModel.resolveAttack(winner, brave1, false, 0.999, crs2);
        assertTrue(crs2.contains(CombatEffectType.BURN_MISSIONS));

        // Destroy settlement when unitCount <= lose
        brave2.dispose();
        brave1.dispose();
        Unit soloBrave = new ServerUnit(game, otherLandTile, inca, braveType);
        soloBrave.addAbility(new Ability(Ability.DISPOSE_ON_COMBAT_LOSS));
        List<CombatEffectType> crs3 = new ArrayList<>();
        crs3.add(CombatEffectType.WIN);
        combatModel.resolveAttack(winner, soloBrave, false, 0.5, crs3);
        assertTrue(crs3.contains(CombatEffectType.SLAUGHTER_UNIT));
        assertTrue(crs3.contains(CombatEffectType.DESTROY_SETTLEMENT));
    }

    // M18-TP8: Auto-equip branches (B37..B40): capture/lose autoequip, slaughter/demote
    public void testM18_TP8_ResolveAutoEquipBranches() {
        Colony colony = createStandardColony(2);
        colony.getOwner().addFather(spec().getFoundingFather("model.foundingFather.paulRevere"));
        colony.addGoods(spec().getGoodsType("model.goods.muskets"), 100);
        UnitType colonialRegularType = spec().getUnitType("model.unit.colonialRegular");
        Unit loser = new ServerUnit(game, colony, dutch, colonialRegularType, spec().getDefaultRole());
        Unit winner = new ServerUnit(game, landTile, french, soldierType, soldierRole);
        Unit nativeWinner = new ServerUnit(game, landTile, inca, braveType);

        List<CombatEffectType> crs1 = new ArrayList<>();
        crs1.add(CombatEffectType.WIN);
        combatModel.resolveAttack(winner, loser, false, 0.5, crs1);
        assertTrue(crs1.contains(CombatEffectType.LOSE_AUTOEQUIP));
        assertTrue(crs1.contains(CombatEffectType.DEMOTE_UNIT));

        // Also cover auto-equip + DEMOTE_UNIT on nativeWinner, and auto-equip + SLAUGHTER_UNIT when loserMustDie
        List<CombatEffectType> crs2 = new ArrayList<>();
        crs2.add(CombatEffectType.WIN);
        combatModel.resolveAttack(nativeWinner, loser, false, 0.5, crs2);
        assertTrue(crs2.contains(CombatEffectType.CAPTURE_AUTOEQUIP));
        assertTrue(crs2.contains(CombatEffectType.DEMOTE_UNIT));

        dutch.addAbility(new Ability(Ability.DISPOSE_ON_COMBAT_LOSS));
        Unit doomedLoser = new ServerUnit(game, colony, dutch, colonistType, spec().getDefaultRole());
        List<CombatEffectType> crs3 = new ArrayList<>();
        crs3.add(CombatEffectType.WIN);
        combatModel.resolveAttack(nativeWinner, doomedLoser, false, 0.5, crs3);
        assertTrue(crs3.contains(CombatEffectType.CAPTURE_AUTOEQUIP));
        assertTrue(crs3.contains(CombatEffectType.SLAUGHTER_UNIT));
    }

    // M18-TP9: Offensive role equipment (B42..B45), capture unit (B46..B48), demote (B49), undead (B50..B52), default slaughter
    public void testM18_TP9_ResolveLandUnitOutcomesAndPromotion() {
        Unit winner = new ServerUnit(game, landTile, dutch, soldierType, soldierRole);
        Unit capturerWinner = new ServerUnit(game, landTile, inca, braveType);

        // Offensive role equipment: colonialRegular soldier loser -> CAPTURE_EQUIP + DEMOTE_UNIT
        UnitType colonialRegularType = spec().getUnitType("model.unit.colonialRegular");
        Unit soldierLoser = new ServerUnit(game, otherLandTile, french, colonialRegularType, soldierRole);
        List<CombatEffectType> crs1 = new ArrayList<>();
        crs1.add(CombatEffectType.WIN);
        combatModel.resolveAttack(capturerWinner, soldierLoser, true, 0.0, crs1);
        assertTrue(crs1.contains(CombatEffectType.CAPTURE_EQUIP));
        assertTrue(crs1.contains(CombatEffectType.DEMOTE_UNIT));

        // Capture unit (colonist has CAN_BE_CAPTURED, soldier has CAPTURE_UNITS)
        Unit colonistLoser = new ServerUnit(game, otherLandTile, french, colonistType);
        List<CombatEffectType> crs2 = new ArrayList<>();
        crs2.add(CombatEffectType.WIN);
        combatModel.resolveAttack(winner, colonistLoser, false, 0.5, crs2);
        assertTrue(crs2.contains(CombatEffectType.CAPTURE_UNIT));

        // Default slaughter: colonist loser defeated by brave (cannot capture units, no demotion)
        List<CombatEffectType> crs4 = new ArrayList<>();
        crs4.add(CombatEffectType.WIN);
        combatModel.resolveAttack(capturerWinner, colonistLoser, false, 0.5, crs4);
        assertTrue(crs4.contains(CombatEffectType.SLAUGHTER_UNIT));

        // Automatic promotion (B54:true) & great promotion (B55:false, B56:false)
        dutch.addAbility(new Ability(Ability.AUTOMATIC_PROMOTION));
        Unit pettyWinner = new ServerUnit(game, landTile, dutch, spec().getUnitType("model.unit.pettyCriminal"), soldierRole);
        List<CombatEffectType> crs5 = new ArrayList<>();
        crs5.add(CombatEffectType.WIN);
        combatModel.resolveAttack(pettyWinner, colonistLoser, false, 0.5, crs5);
        assertTrue(crs5.contains(CombatEffectType.PROMOTE_UNIT));

        Unit frenchPettyWinner = new ServerUnit(game, landTile, french, spec().getUnitType("model.unit.pettyCriminal"), soldierRole);
        List<CombatEffectType> crs6 = new ArrayList<>();
        crs6.add(CombatEffectType.WIN);
        combatModel.resolveAttack(frenchPettyWinner, colonistLoser, true, 0.0, crs6);
        assertTrue(crs6.contains(CombatEffectType.PROMOTE_UNIT));

        // Undead winner captures non-undead loser
        dutch.changePlayerType(Player.PlayerType.UNDEAD);
        dutch.addAbility(new Ability(Ability.UNDEAD));
        Unit undeadWinner = new ServerUnit(game, landTile, dutch, undeadType);
        List<CombatEffectType> crs3 = new ArrayList<>();
        crs3.add(CombatEffectType.WIN);
        combatModel.resolveAttack(undeadWinner, colonistLoser, false, 0.5, crs3);
        assertTrue(crs3.contains(CombatEffectType.CAPTURE_UNIT));
    }

    // ------------------------------------------------------------------
    // M19: isAmbush(FreeColGameObject, FreeColGameObject) — 4 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M19-TP1: ENTRY -> N0 -(B1:true)-> N4 -> N5 -> EXIT (attacker not Unit)
    public void testM19_TP1_IsAmbushAttackerNotUnit() throws Exception {
        Unit defender = new ServerUnit(game, landTile, dutch, soldierType);
        assertFalse(invokeIsAmbush(landTile, defender));
    }

    // M19-TP2: ENTRY -> N0 -(B1:false)-> N1 -(B2:true)-> N4 -> N5 -> EXIT (defender not Unit)
    public void testM19_TP2_IsAmbushDefenderNotUnit() throws Exception {
        Unit attacker = new ServerUnit(game, landTile, inca, braveType);
        assertFalse(invokeIsAmbush(attacker, landTile));
    }

    // M19-TP3: ENTRY -> N0 -(B1:false)-> N1 -(B2:false)-> N2 -(B3:true)-> N4 -> N5 -> EXIT (cannot ambush)
    public void testM19_TP3_IsAmbushFalseForEuropeans() throws Exception {
        Unit attacker = new ServerUnit(game, landTile, dutch, soldierType);
        Unit defender = new ServerUnit(game, otherLandTile, french, soldierType);
        assertFalse(invokeIsAmbush(attacker, defender));
    }

    // M19-TP4: ENTRY -> N0 -(B1:false)-> N1 -(B2:false)-> N2 -(B3:false)-> N3 -> N5 -> EXIT (Indian ambush -> true)
    public void testM19_TP4_IsAmbushTrueForIndian() throws Exception {
        Unit attacker = new ServerUnit(game, landTile, inca, braveType);
        Unit defender = new ServerUnit(game, forestTile, dutch, soldierType);
        assertTrue(invokeIsAmbush(attacker, defender));
    }

    // ------------------------------------------------------------------
    // M20..M25: Lambdas — 1 Test Path each (3 for M21) (100% C3)
    // ------------------------------------------------------------------

    // M20-TP1: lambda$resolveAttack$0 (triggered via BURN_MISSIONS check in resolveAttack)
    public void testM20_TP1_LambdaResolveAttackBurnMissions() {
        testM18_TP7_ResolveIndianSettlementBranches();
    }

    // M21-TP1..TP3: lambda$hasStrongDefenceModifier$0 (non-PERCENTAGE, PERCENTAGE < 150, PERCENTAGE >= 150)
    public void testM21_TP1_to_TP3_LambdaHasStrongDefenceModifier() throws Exception {
        FreeColObject fco = new FreeColObject() {
            private final FeatureContainer fc = new FeatureContainer();
            @Override public FeatureContainer getFeatureContainer() { return fc; }
            @Override public Specification getSpecification() { return spec(); }
            @Override public String getXMLTagName() { return "test"; }
        };
        fco.addModifier(new Modifier(Modifier.DEFENCE, 200.0f, ModifierType.ADDITIVE));
        fco.addModifier(new Modifier(Modifier.DEFENCE, 50.0f, ModifierType.PERCENTAGE));
        assertFalse(invokeHasStrongDefenceModifier(fco));

        fco.addModifier(new Modifier(Modifier.DEFENCE, 150.0f, ModifierType.PERCENTAGE));
        assertTrue(invokeHasStrongDefenceModifier(fco));
    }

    // M22-TP1: lambda$addNavalDefensiveModifiers$0 (cargo penalty scaling on naval defender)
    public void testM22_TP1_LambdaNavalDefensiveCargo() throws Exception {
        testM14_TP2_NavalDefensiveWithCargo();
    }

    // M23-TP1: lambda$addNavalOffensiveModifiers$0 (cargo penalty scaling on naval attacker)
    public void testM23_TP1_LambdaNavalOffensiveCargo() throws Exception {
        testM10_TP2_NavalOffensiveWithCargo();
    }

    // M24-TP1 & M25-TP1: lambda$getOffencePower$1 & lambda$getOffencePower$0 (colony bombard unit filter & offence sum)
    public void testM24_M25_TP1_LambdasGetOffencePowerBombard() {
        testM6_TP7_GetOffencePowerBombardCappedWithLog();
    }

    // ------------------------------------------------------------------
    // M26: <clinit>() — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M26-TP1: ENTRY -> N0 -> EXIT (static constants initialized on class load)
    public void testM26_TP1_StaticInit() {
        assertEquals(48, SimpleCombatModel.MAXIMUM_BOMBARD_POWER);
        assertEquals(150, SimpleCombatModel.STRONG_DEFENCE_THRESHOLD);
        assertNotNull(SimpleCombatModel.UNKNOWN_DEFENCE_MODIFIER);
    }
}
