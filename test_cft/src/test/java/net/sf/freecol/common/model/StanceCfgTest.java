package net.sf.freecol.common.model;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import net.sf.freecol.util.test.FreeColTestCase;

/**
 * CFG-driven test suite for {@link Stance} derived from
 * {@code test_cft/graph/net/sf/freecol/common/model/Stance.json}.
 *
 * Covers all 14 methods (M1..M14) and achieves 100% feasible C3 (node + edge)
 * coverage across 45 Test Paths.
 *
 * Infeasible edges documented:
 * - M6 ({@code getStanceFromTension}): {@code N0 -> N8} ({@code B1:default}) and
 *   {@code N8 -> N9} — {@code Stance} is an enum with 5 constants (ordinals 0..4),
 *   so {@code TABLESWITCH} on {@code this.ordinal()} can never take {@code default}.
 * - M7 ({@code getTensionModifier}): {@code N0 -> N30 -> EXIT}, {@code N2 -> N8 -> N31},
 *   {@code N9 -> N15 -> N31}, {@code N16 -> N22 -> N31}, {@code N23 -> N29 -> N31}
 *   ({@code B1..B5:default}) — enum constants only have ordinals 0..4 (and {@code null}
 *   throws {@code NullPointerException} before entering the switch).
 * - M7 ({@code getTensionModifier}): {@code N1 -> N31}, {@code N3 -> N4},
 *   {@code N17 -> N18}, {@code N18 -> N19}, {@code N19 -> N20}, {@code N31 -> EXIT} —
 *   {@code badTransition(newStance)} unconditionally throws {@code RuntimeException},
 *   so fallthrough / break edges after {@code badTransition} are unreachable.
 */
public class StanceCfgTest extends FreeColTestCase {

    @Override
    public void setUp() throws Exception {
        super.setUp();
        // Pre-condition: Stance is a self-contained enum; Tension constants provide thresholds.
    }

    // ------------------------------------------------------------------
    // Pre-condition helpers & stubs
    // ------------------------------------------------------------------

    private Object invokePrivate(Stance target, String name, Class<?>[] paramTypes, Object... args)
            throws Exception {
        Method m = Stance.class.getDeclaredMethod(name, paramTypes);
        m.setAccessible(true);
        return m.invoke(target, args);
    }

    // ------------------------------------------------------------------
    // M1: values() — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M1-TP1: ENTRY -> N0 -> EXIT (returns all 5 Stance enum constants)
    public void testM1_TP1_ValuesReturnsAllConstants() {
        Stance[] values = Stance.values();
        assertEquals(5, values.length);
        assertEquals(Stance.UNCONTACTED, values[0]);
        assertEquals(Stance.WAR, values[4]);
    }

    // ------------------------------------------------------------------
    // M2: valueOf(String) — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M2-TP1: ENTRY -> N0 -> EXIT (resolves enum constant by name)
    public void testM2_TP1_ValueOfResolvesConstant() {
        assertEquals(Stance.PEACE, Stance.valueOf("PEACE"));
    }

    // ------------------------------------------------------------------
    // M3: <init>(String, int) — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M3-TP1: ENTRY -> N0 -> EXIT (enum constructor executed during class initialization)
    public void testM3_TP1_EnumConstructorInitializedConstants() {
        assertNotNull(Stance.UNCONTACTED);
        assertEquals(0, Stance.UNCONTACTED.ordinal());
        assertEquals(4, Stance.WAR.ordinal());
    }

    // ------------------------------------------------------------------
    // M4: badStance() — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M4-TP1: ENTRY -> N0 -> EXIT (private no-op helper invoked via reflection)
    public void testM4_TP1_BadStanceNoOp() throws Exception {
        invokePrivate(Stance.PEACE, "badStance", new Class<?>[0]);
    }

    // ------------------------------------------------------------------
    // M5: badTransition(Stance) — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M5-TP1: ENTRY -> N0 -> EXIT (throws RuntimeException describing invalid transition)
    public void testM5_TP1_BadTransitionThrowsRuntimeException() throws Exception {
        try {
            invokePrivate(Stance.PEACE, "badTransition", new Class<?>[] { Stance.class },
                          Stance.UNCONTACTED);
            fail("Expected RuntimeException");
        } catch (InvocationTargetException e) {
            assertTrue(e.getCause() instanceof RuntimeException);
            assertTrue(e.getCause().getMessage().contains("PEACE -> UNCONTACTED"));
        }
    }

    // ------------------------------------------------------------------
    // M6: getStanceFromTension(Tension) — 9 Test Paths (100% feasible C3)
    // ------------------------------------------------------------------

    // M6-TP1: ENTRY -> N0 -(B1:case:0)-> N7 -> N9 -> EXIT (UNCONTACTED stays UNCONTACTED)
    public void testM6_TP1_UncontactedStaysUncontacted() {
        assertEquals(Stance.UNCONTACTED, Stance.UNCONTACTED.getStanceFromTension(new Tension(1000)));
    }

    // M6-TP2: ENTRY -> N0 -(B1:case:4)-> N1 -(B2:false)-> N2 -> EXIT
    // (WAR with value <= CONTENT.getLimit() - DELTA transitions to CEASE_FIRE)
    public void testM6_TP2_WarLowTensionBecomesCeaseFire() {
        int threshold = Tension.Level.CONTENT.getLimit() - Tension.DELTA;
        assertEquals(Stance.CEASE_FIRE, Stance.WAR.getStanceFromTension(new Tension(threshold)));
    }

    // M6-TP3: ENTRY -> N0 -(B1:case:4)-> N1 -(B2:true)-> N9 -> EXIT
    // (WAR with value > CONTENT.getLimit() - DELTA stays WAR)
    public void testM6_TP3_WarHighTensionStaysWar() {
        int threshold = Tension.Level.CONTENT.getLimit() - Tension.DELTA + 1;
        assertEquals(Stance.WAR, Stance.WAR.getStanceFromTension(new Tension(threshold)));
    }

    // M6-TP4: ENTRY -> N0 -(B1:case:3)-> N3 -(B3:false)-> N4 -> EXIT
    // (CEASE_FIRE with value <= HAPPY.getLimit() - DELTA transitions to PEACE)
    public void testM6_TP4_CeaseFireLowTensionBecomesPeace() {
        int threshold = Tension.Level.HAPPY.getLimit() - Tension.DELTA;
        assertEquals(Stance.PEACE, Stance.CEASE_FIRE.getStanceFromTension(new Tension(threshold)));
    }

    // M6-TP5: ENTRY -> N0 -(B1:case:3)-> N3 -(B3:true)-> N5 -(B4:true)-> N9 -> EXIT
    // (CEASE_FIRE with HAPPY - DELTA < value <= HATEFUL + DELTA stays CEASE_FIRE)
    public void testM6_TP5_CeaseFireMidTensionStaysCeaseFire() {
        int value = Tension.Level.HATEFUL.getLimit() + Tension.DELTA;
        assertEquals(Stance.CEASE_FIRE, Stance.CEASE_FIRE.getStanceFromTension(new Tension(value)));
    }

    // M6-TP6: ENTRY -> N0 -(B1:case:3)-> N3 -(B3:true)-> N5 -(B4:false)-> N6 -> EXIT
    // (CEASE_FIRE with value > HATEFUL + DELTA transitions to WAR)
    public void testM6_TP6_CeaseFireHighTensionBecomesWar() {
        int value = Tension.Level.HATEFUL.getLimit() + Tension.DELTA + 1;
        assertEquals(Stance.WAR, Stance.CEASE_FIRE.getStanceFromTension(new Tension(value)));
    }

    // M6-TP7: ENTRY -> N0 -(B1:case:1)-> N5 -(B4:true)-> N9 -> EXIT
    // (ALLIANCE with value <= HATEFUL + DELTA stays ALLIANCE)
    public void testM6_TP7_AllianceNormalTensionStaysAlliance() {
        int value = Tension.Level.CONTENT.getLimit();
        assertEquals(Stance.ALLIANCE, Stance.ALLIANCE.getStanceFromTension(new Tension(value)));
    }

    // M6-TP8: ENTRY -> N0 -(B1:case:1)-> N5 -(B4:false)-> N6 -> EXIT
    // (ALLIANCE with value > HATEFUL + DELTA transitions to WAR)
    public void testM6_TP8_AllianceHighTensionBecomesWar() {
        int value = Tension.Level.HATEFUL.getLimit() + Tension.DELTA + 1;
        assertEquals(Stance.WAR, Stance.ALLIANCE.getStanceFromTension(new Tension(value)));
    }

    // M6-TP9: ENTRY -> N0 -(B1:case:2)-> N5 -(B4:true)-> N9 -> EXIT
    // (PEACE with value <= HATEFUL + DELTA stays PEACE)
    public void testM6_TP9_PeaceNormalTensionStaysPeace() {
        int value = Tension.Level.CONTENT.getLimit();
        assertEquals(Stance.PEACE, Stance.PEACE.getStanceFromTension(new Tension(value)));
    }

    // ------------------------------------------------------------------
    // M7: getTensionModifier(Stance) — 21 Test Paths (100% feasible C3)
    // ------------------------------------------------------------------

    // M7-TP1: ENTRY -> N0 -(B1:case:0)-> N1 -> EXIT(throw via badTransition)
    public void testM7_TP1_ToUncontactedThrows() {
        try {
            Stance.PEACE.getTensionModifier(Stance.UNCONTACTED);
            fail("Expected RuntimeException");
        } catch (RuntimeException expected) {
            // Expected
        }
    }

    // M7-TP2..TP6: ENTRY -> N0 -(B1:case:1)-> N2 -(B2:case:0..4)-> N3..N7 -> EXIT
    public void testM7_TP2_To6_ToAllianceAllSources() {
        // TP2: UNCONTACTED -> ALLIANCE throws
        try {
            Stance.UNCONTACTED.getTensionModifier(Stance.ALLIANCE);
            fail("Expected RuntimeException");
        } catch (RuntimeException expected) {
            // Expected
        }
        // TP3: ALLIANCE -> ALLIANCE
        assertEquals(0, Stance.ALLIANCE.getTensionModifier(Stance.ALLIANCE));
        // TP4: PEACE -> ALLIANCE
        assertEquals(Tension.ALLIANCE_MODIFIER, Stance.PEACE.getTensionModifier(Stance.ALLIANCE));
        // TP5: CEASE_FIRE -> ALLIANCE
        assertEquals(Tension.ALLIANCE_MODIFIER + Tension.PEACE_TREATY_MODIFIER,
                     Stance.CEASE_FIRE.getTensionModifier(Stance.ALLIANCE));
        // TP6: WAR -> ALLIANCE
        assertEquals(Tension.ALLIANCE_MODIFIER + Tension.CEASE_FIRE_MODIFIER + Tension.PEACE_TREATY_MODIFIER,
                     Stance.WAR.getTensionModifier(Stance.ALLIANCE));
    }

    // M7-TP7..TP11: ENTRY -> N0 -(B1:case:2)-> N9 -(B3:case:0..4)-> N10..N14 -> EXIT
    public void testM7_TP7_To11_ToPeaceAllSources() {
        assertEquals(Tension.CONTACT_MODIFIER, Stance.UNCONTACTED.getTensionModifier(Stance.PEACE));
        assertEquals(Tension.DROP_ALLIANCE_MODIFIER, Stance.ALLIANCE.getTensionModifier(Stance.PEACE));
        assertEquals(0, Stance.PEACE.getTensionModifier(Stance.PEACE));
        assertEquals(Tension.PEACE_TREATY_MODIFIER, Stance.CEASE_FIRE.getTensionModifier(Stance.PEACE));
        assertEquals(Tension.CEASE_FIRE_MODIFIER + Tension.PEACE_TREATY_MODIFIER,
                     Stance.WAR.getTensionModifier(Stance.PEACE));
    }

    // M7-TP12..TP16: ENTRY -> N0 -(B1:case:3)-> N16 -(B4:case:0..4)-> N17..N21 -> EXIT
    public void testM7_TP12_To16_ToCeaseFireAllSources() {
        for (Stance invalidFrom : new Stance[] { Stance.UNCONTACTED, Stance.ALLIANCE, Stance.PEACE }) {
            try {
                invalidFrom.getTensionModifier(Stance.CEASE_FIRE);
                fail("Expected RuntimeException from " + invalidFrom);
            } catch (RuntimeException expected) {
                // Expected
            }
        }
        assertEquals(0, Stance.CEASE_FIRE.getTensionModifier(Stance.CEASE_FIRE));
        assertEquals(Tension.CEASE_FIRE_MODIFIER, Stance.WAR.getTensionModifier(Stance.CEASE_FIRE));
    }

    // M7-TP17..TP21: ENTRY -> N0 -(B1:case:4)-> N23 -(B5:case:0..4)-> N24..N28 -> EXIT
    public void testM7_TP17_To21_ToWarAllSources() {
        assertEquals(Tension.WAR_MODIFIER, Stance.UNCONTACTED.getTensionModifier(Stance.WAR));
        assertEquals(Tension.WAR_MODIFIER, Stance.ALLIANCE.getTensionModifier(Stance.WAR));
        assertEquals(Tension.WAR_MODIFIER, Stance.PEACE.getTensionModifier(Stance.WAR));
        assertEquals(Tension.RESUME_WAR_MODIFIER, Stance.CEASE_FIRE.getTensionModifier(Stance.WAR));
        assertEquals(0, Stance.WAR.getTensionModifier(Stance.WAR));
    }

    // ------------------------------------------------------------------
    // M8: isIncitable() — 3 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M8-TP1: ENTRY -> N0 -(B1:case:2)-> N1 -> EXIT (PEACE is incitable)
    public void testM8_TP1_PeaceIsIncitable() {
        assertTrue(Stance.PEACE.isIncitable());
    }

    // M8-TP2: ENTRY -> N0 -(B1:case:3)-> N1 -> EXIT (CEASE_FIRE is incitable)
    public void testM8_TP2_CeaseFireIsIncitable() {
        assertTrue(Stance.CEASE_FIRE.isIncitable());
    }

    // M8-TP3: ENTRY -> N0 -(B1:default)-> N2 -> EXIT (WAR/ALLIANCE/UNCONTACTED are not incitable)
    public void testM8_TP3_OtherStancesNotIncitable() {
        assertFalse(Stance.UNCONTACTED.isIncitable());
        assertFalse(Stance.ALLIANCE.isIncitable());
        assertFalse(Stance.WAR.isIncitable());
    }

    // ------------------------------------------------------------------
    // M9: getKey() — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M9-TP1: ENTRY -> N0 -> EXIT (returns "stance." + enum key)
    public void testM9_TP1_GetKey() {
        assertEquals("stance.peace", Stance.PEACE.getKey());
        assertEquals("stance.ceaseFire", Stance.CEASE_FIRE.getKey());
    }

    // ------------------------------------------------------------------
    // M10: getStanceChangeKey() — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M10-TP1: ENTRY -> N0 -> EXIT (returns "model.player." + getKey() + ".declared")
    public void testM10_TP1_GetStanceChangeKey() {
        assertEquals("model.player.stance.war.declared", Stance.WAR.getStanceChangeKey());
    }

    // ------------------------------------------------------------------
    // M11: getOtherStanceChangeKey() — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M11-TP1: ENTRY -> N0 -> EXIT (returns "model.player." + getKey() + ".others")
    public void testM11_TP1_GetOtherStanceChangeKey() {
        assertEquals("model.player.stance.alliance.others", Stance.ALLIANCE.getOtherStanceChangeKey());
    }

    // ------------------------------------------------------------------
    // M12: getNameKey() — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M12-TP1: ENTRY -> N0 -> EXIT (returns Messages.nameKey("model." + getKey()))
    public void testM12_TP1_GetNameKey() {
        assertEquals("model.stance.peace.name", Stance.PEACE.getNameKey());
    }

    // ------------------------------------------------------------------
    // M13: $values() — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M13-TP1: ENTRY -> N0 -> EXIT (synthetic enum $values() invoked via reflection)
    public void testM13_TP1_SyntheticValuesMethod() throws Exception {
        Stance[] values = (Stance[]) invokePrivate(null, "$values", new Class<?>[0]);
        assertEquals(5, values.length);
    }

    // ------------------------------------------------------------------
    // M14: <clinit>() — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M14-TP1: ENTRY -> N0 -> EXIT (static class initializer populates enum constants)
    public void testM14_TP1_StaticInitializer() {
        assertEquals(5, Stance.values().length);
    }
}
