/**
 *  Copyright (C) 2002-2024   The FreeCol Team
 *
 *  This file is part of FreeCol.
 *
 *  FreeCol is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  FreeCol is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with FreeCol.  If not, see <http://www.gnu.org/licenses/>.
 */

package net.sf.freecol.common.model;

import java.lang.reflect.Constructor;

import net.sf.freecol.util.test.FreeColTestCase;


/**
 * CFG-driven test suite for {@link TileImprovementStyle} derived from
 * {@code test_cft/graph/net/sf/freecol/common/model/TileImprovementStyle.json}.
 *
 * Covers all 6 methods (M1..M6, including {@code <init>} and {@code <clinit>})
 * and achieves 100% feasible C3 (node + edge) coverage across 13 Test Paths.
 *
 * Infeasible edges documented:
 * - M4 ({@code getInstance}): {@code N5 -> N6} ({@code B5:false}) and
 *   {@code N6 -> N7} — {@code new TileImprovementStyle(key)} assigns
 *   {@code this.style = key}, so {@code result.getString().equals(key)} is
 *   always {@code true} for any non-null {@code key}.
 */
public class TileImprovementStyleCfgTest extends FreeColTestCase {

    @Override
    public void setUp() throws Exception {
        super.setUp();
        // Pre-condition: TileImprovementStyle is stateless except for its static cache
    }

    // ------------------------------------------------------------------
    // Pre-condition helpers & stubs
    // ------------------------------------------------------------------

    /**
     * Invoke the private constructor {@link TileImprovementStyle#TileImprovementStyle(String)}
     * via reflection to test all loop and character branches directly.
     */
    private TileImprovementStyle newStyleViaReflection(String style) throws Exception {
        Constructor<TileImprovementStyle> ctor =
            TileImprovementStyle.class.getDeclaredConstructor(String.class);
        ctor.setAccessible(true);
        return ctor.newInstance(style);
    }

    // ------------------------------------------------------------------
    // M1: <init>(Ljava/lang/String;)V — 3 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M1-TP1: ENTRY -> N0 -> N1 -(B1:true)-> N8 -> EXIT (Empty string: 0 iterations)
    public void testM1_TP1_ConstructorEmptyString() throws Exception {
        TileImprovementStyle style = newStyleViaReflection("");
        assertEquals("", style.getString());
        assertEquals("", style.getMask());
    }

    // M1-TP2: ENTRY -> N0 -> N1 -(B1:false)-> N2 -(B2:false)-> N3 -> N8 -> EXIT (Non-digit char breaks loop)
    public void testM1_TP2_ConstructorBreaksOnInvalidDigit() throws Exception {
        TileImprovementStyle style = newStyleViaReflection("0#10");
        assertEquals("0#10", style.getString());
        assertEquals("0", style.getMask());
    }

    // M1-TP3: ENTRY -> N0 -> N1 -(B1:false)-> N2 -(B2:true)-> N4 -(B3:false)-> N5 -> N7 -> N1 -(B1:false)-> N2 -(B2:true)-> N4 -(B3:true)-> N6 -> N7 -> N1 -(B1:true)-> N8 -> EXIT (Both '0' and non-'0' digits across loop)
    public void testM1_TP3_ConstructorZeroAndNonZeroDigits() throws Exception {
        TileImprovementStyle style = newStyleViaReflection("02a0");
        assertEquals("02a0", style.getString());
        assertEquals("0110", style.getMask());
    }

    // ------------------------------------------------------------------
    // M2: getString()Ljava/lang/String; — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M2-TP1: ENTRY -> N0 -> EXIT (Return style string)
    public void testM2_TP1_GetString() {
        TileImprovementStyle style = TileImprovementStyle.getInstance("0120");
        assertNotNull(style);
        assertEquals("0120", style.getString());
    }

    // ------------------------------------------------------------------
    // M3: getMask()Ljava/lang/String; — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M3-TP1: ENTRY -> N0 -> EXIT (Return derived binary mask string)
    public void testM3_TP1_GetMask() {
        TileImprovementStyle style = TileImprovementStyle.getInstance("0203");
        assertNotNull(style);
        assertEquals("0101", style.getMask());
    }

    // ------------------------------------------------------------------
    // M4: getInstance(Ljava/lang/String;)Lnet/sf/freecol/common/model/TileImprovementStyle; — 6 feasible Test Paths (100% feasible C3)
    // ------------------------------------------------------------------

    // M4-TP1: ENTRY -> N0 -(B1:true)-> N3 -> EXIT (key == null)
    public void testM4_TP1_GetInstanceNullKey() {
        assertNull(TileImprovementStyle.getInstance(null));
    }

    // M4-TP2: ENTRY -> N0 -(B1:false)-> N1 -(B2:false)-> N2 -(B3:false)-> N3 -> EXIT (key length != 4 && != 8)
    public void testM4_TP2_GetInstanceInvalidLength() {
        assertNull(TileImprovementStyle.getInstance(""));
        assertNull(TileImprovementStyle.getInstance("012"));
        assertNull(TileImprovementStyle.getInstance("01234"));
        assertNull(TileImprovementStyle.getInstance("012345678"));
    }

    // M4-TP3: ENTRY -> N0 -(B1:false)-> N1 -(B2:true)-> N4 -(B4:false)-> N5 -(B5:true)-> N7 -> EXIT (4-char key cache miss)
    public void testM4_TP3_GetInstanceFourCharMiss() {
        String key = "7071";
        TileImprovementStyle style = TileImprovementStyle.getInstance(key);
        assertNotNull(style);
        assertEquals(key, style.getString());
        assertEquals("1011", style.getMask());
    }

    // M4-TP4: ENTRY -> N0 -(B1:false)-> N1 -(B2:true)-> N4 -(B4:true)-> N7 -> EXIT (4-char key cache hit)
    public void testM4_TP4_GetInstanceFourCharHit() {
        String key = "8081";
        TileImprovementStyle first = TileImprovementStyle.getInstance(key);
        TileImprovementStyle second = TileImprovementStyle.getInstance(key);
        assertSame(first, second);
    }

    // M4-TP5: ENTRY -> N0 -(B1:false)-> N1 -(B2:false)-> N2 -(B3:true)-> N4 -(B4:false)-> N5 -(B5:true)-> N7 -> EXIT (8-char key cache miss)
    public void testM4_TP5_GetInstanceEightCharMiss() {
        String key = "10101010";
        TileImprovementStyle style = TileImprovementStyle.getInstance(key);
        assertNotNull(style);
        assertEquals(key, style.getString());
        assertEquals("10101010", style.getMask());
    }

    // M4-TP6: ENTRY -> N0 -(B1:false)-> N1 -(B2:false)-> N2 -(B3:true)-> N4 -(B4:true)-> N7 -> EXIT (8-char key cache hit)
    public void testM4_TP6_GetInstanceEightCharHit() {
        String key = "01010101";
        TileImprovementStyle first = TileImprovementStyle.getInstance(key);
        TileImprovementStyle second = TileImprovementStyle.getInstance(key);
        assertSame(first, second);
    }

    // ------------------------------------------------------------------
    // M5: toString()Ljava/lang/String; — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M5-TP1: ENTRY -> N0 -> EXIT (toString returns style)
    public void testM5_TP1_ToString() {
        TileImprovementStyle style = TileImprovementStyle.getInstance("3030");
        assertNotNull(style);
        assertEquals("3030", style.toString());
    }

    // ------------------------------------------------------------------
    // M6: <clinit>()V — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M6-TP1: ENTRY -> N0 -> EXIT (Static cache initialized on class load)
    public void testM6_TP1_ClassInitialization() {
        assertNotNull(TileImprovementStyle.getInstance("0000"));
    }
}
