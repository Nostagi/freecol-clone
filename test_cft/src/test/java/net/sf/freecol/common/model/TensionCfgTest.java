package net.sf.freecol.common.model;

import net.sf.freecol.util.test.FreeColTestCase;

/**
 * CFG-driven test suite for {@link Tension} derived from
 * {@code test_cft/graph/net/sf/freecol/common/model/Tension.json}.
 *
 * Covers all 13 methods (M1..M13, including {@code lambda$getLevel$0} and {@code <clinit>})
 * and achieves 100% feasible C3 (node + edge) coverage across 19 Test Paths.
 *
 * Infeasible edges documented:
 * - None. All nodes and edges in {@code Tension.json} are feasible and covered.
 */
public class TensionCfgTest extends FreeColTestCase {

    @Override
    public void setUp() throws Exception {
        super.setUp();
        // Pre-condition: standard FreeCol test environment with loaded message bundle
    }

    // ------------------------------------------------------------------
    // Pre-condition helpers & stubs
    // ------------------------------------------------------------------

    // No external stubs required; Tension is a standalone value/model class.

    // ------------------------------------------------------------------
    // M1: <init>() — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M1-TP1: ENTRY -> N0 -> EXIT (default constructor sets HAPPY limit 100)
    public void testM1_TP1_DefaultConstructor() {
        Tension tension = new Tension();
        assertEquals(Tension.Level.HAPPY.getLimit(), tension.getValue());
        assertEquals(Tension.Level.HAPPY, tension.getLevel());
    }

    // ------------------------------------------------------------------
    // M2: <init>(int) — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M2-TP1: ENTRY -> N0 -> EXIT (parameterized constructor delegates to setValue)
    public void testM2_TP1_ParameterizedConstructor() {
        Tension tension = new Tension(500);
        assertEquals(500, tension.getValue());
    }

    // ------------------------------------------------------------------
    // M3: getValue() — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M3-TP1: ENTRY -> N0 -> EXIT (returns current tension value)
    public void testM3_TP1_GetValue() {
        Tension tension = new Tension(650);
        assertEquals(650, tension.getValue());
    }

    // ------------------------------------------------------------------
    // M4: setValue(int) — 3 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M4-TP1: ENTRY -> N0 -(B1:false)-> N1 -> N5 -> EXIT (newValue < TENSION_MIN)
    public void testM4_TP1_SetValueBelowMin() {
        Tension tension = new Tension();
        tension.setValue(-50);
        assertEquals(Tension.TENSION_MIN, tension.getValue());
    }

    // M4-TP2: ENTRY -> N0 -(B1:true)-> N2 -(B2:false)-> N3 -> N5 -> EXIT (newValue > TENSION_MAX)
    public void testM4_TP2_SetValueAboveMax() {
        Tension tension = new Tension();
        tension.setValue(Tension.TENSION_MAX + 500);
        assertEquals(Tension.TENSION_MAX, tension.getValue());
    }

    // M4-TP3: ENTRY -> N0 -(B1:true)-> N2 -(B2:true)-> N4 -> N5 -> EXIT (TENSION_MIN <= newValue <= TENSION_MAX)
    public void testM4_TP3_SetValueInRange() {
        Tension tension = new Tension();
        tension.setValue(750);
        assertEquals(750, tension.getValue());
    }

    // ------------------------------------------------------------------
    // M5: getLevel() — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M5-TP1: ENTRY -> N0 -> EXIT (returns Level matching current value)
    public void testM5_TP1_GetLevel() {
        Tension tension = new Tension(600);
        assertEquals(Tension.Level.CONTENT, tension.getLevel());
    }

    // ------------------------------------------------------------------
    // M6: modify(int) — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M6-TP1: ENTRY -> N0 -> EXIT (adds delta via setValue)
    public void testM6_TP1_Modify() {
        Tension tension = new Tension(100);
        tension.modify(200);
        assertEquals(300, tension.getValue());
    }

    // ------------------------------------------------------------------
    // M7: getKey() — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M7-TP1: ENTRY -> N0 -> EXIT (returns "tension." + level.getKey())
    public void testM7_TP1_GetKey() {
        Tension tension = new Tension(100);
        assertEquals("tension." + Tension.Level.HAPPY.getKey(), tension.getKey());
    }

    // ------------------------------------------------------------------
    // M8: getNameKey() — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M8-TP1: ENTRY -> N0 -> EXIT (returns Messages.nameKey("model." + getKey()))
    public void testM8_TP1_GetNameKey() {
        Tension tension = new Tension(100);
        assertEquals("model." + tension.getKey() + ".name", tension.getNameKey());
    }

    // ------------------------------------------------------------------
    // M9: equals(Object) — 4 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M9-TP1: ENTRY -> N0 -(B1:false)-> N1 -> EXIT (this == o)
    public void testM9_TP1_EqualsSameInstance() {
        Tension tension = new Tension(300);
        assertTrue(tension.equals(tension));
    }

    // M9-TP2: ENTRY -> N0 -(B1:true)-> N2 -(B2:true)-> N7 -> EXIT (!(o instanceof Tension))
    public void testM9_TP2_EqualsNonTension() {
        Tension tension = new Tension(300);
        assertFalse(tension.equals("not-a-tension"));
        assertFalse(tension.equals(null));
    }

    // M9-TP3: ENTRY -> N0 -(B1:true)-> N2 -(B2:false)-> N3 -(B3:true)-> N5 -> N6 -> EXIT (different value)
    public void testM9_TP3_EqualsDifferentValue() {
        Tension t1 = new Tension(300);
        Tension t2 = new Tension(400);
        assertFalse(t1.equals(t2));
    }

    // M9-TP4: ENTRY -> N0 -(B1:true)-> N2 -(B2:false)-> N3 -(B3:false)-> N4 -> N6 -> EXIT (equal value)
    public void testM9_TP4_EqualsSameValue() {
        Tension t1 = new Tension(300);
        Tension t2 = new Tension(300);
        assertTrue(t1.equals(t2));
    }

    // ------------------------------------------------------------------
    // M10: hashCode() — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M10-TP1: ENTRY -> N0 -> EXIT (computes hash from super.hashCode() and value)
    public void testM10_TP1_HashCode() {
        Tension t = new Tension(250);
        int expected = 31 * System.identityHashCode(t) + 250;
        assertEquals(expected, t.hashCode());
    }

    // ------------------------------------------------------------------
    // M11: toString() — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M11-TP1: ENTRY -> N0 -> EXIT (returns getLevel().toString())
    public void testM11_TP1_ToString() {
        Tension t = new Tension(700);
        assertEquals(Tension.Level.DISPLEASED.toString(), t.toString());
    }

    // ------------------------------------------------------------------
    // M12: lambda$getLevel$0(Level) — 2 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M12-TP1: ENTRY -> N0 -(B1:true)-> N2 -> N3 -> EXIT (value > level.getLimit() -> false)
    public void testM12_TP1_LambdaGetLevelExceedsAllLimits() {
        Tension t = new Tension(Tension.TENSION_MAX);
        assertEquals(Tension.Level.HATEFUL, t.getLevel());
    }

    // M12-TP2: ENTRY -> N0 -(B1:false)-> N1 -> N3 -> EXIT (value <= level.getLimit() -> true)
    public void testM12_TP2_LambdaGetLevelMatchesFirstLimit() {
        Tension t = new Tension(Tension.Level.HAPPY.getLimit());
        assertEquals(Tension.Level.HAPPY, t.getLevel());
    }

    // ------------------------------------------------------------------
    // M13: <clinit>() — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M13-TP1: ENTRY -> N0 -> EXIT (static class constants initialized)
    public void testM13_TP1_StaticInit() {
        assertEquals(0, Tension.TENSION_MIN);
        assertEquals(Tension.Level.HATEFUL.getLimit() + 100, Tension.TENSION_MAX);
        assertEquals((Tension.Level.CONTENT.getLimit() + Tension.Level.HAPPY.getLimit()) / 2,
                     Tension.SURRENDERED);
    }
}
