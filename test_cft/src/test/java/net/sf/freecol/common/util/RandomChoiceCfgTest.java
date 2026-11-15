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

package net.sf.freecol.common.util;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.logging.Logger;
import java.util.stream.Stream;

import net.sf.freecol.util.test.FreeColTestCase;


/**
 * CFG-driven test suite for {@link RandomChoice} derived from
 * {@code test_cft/graph/net/sf/freecol/common/util/RandomChoice.json}.
 *
 * Covers all 8 methods (M1..M8, including {@code <init>}) and achieves 100%
 * feasible C3 (node + edge) coverage across 18 Test Paths.
 *
 * Infeasible edges documented:
 * - M4 ({@code select(Collection, int)}): {@code N3 -> N7} ({@code B2:true})
 *   directly on the first check without entering {@code N4} is infeasible because
 *   {@code B1:true} already guaranteed {@code !input.isEmpty()}; {@code N3 -> N7}
 *   is reached after completing at least one loop iteration via {@code N6 -> N3}.
 * - M8 ({@code normalize(Collection, int)}): {@code N5 -> N7} ({@code B4:true})
 *   directly on the first check without entering {@code N6} is infeasible because
 *   {@code B2:false} already guaranteed {@code !input.isEmpty()}; {@code N5 -> N7}
 *   is reached after the loop finishes via {@code N6 -> N5}.
 */
public class RandomChoiceCfgTest extends FreeColTestCase {

    private static final Logger LOGGER = Logger.getLogger(RandomChoiceCfgTest.class.getName());

    @Override
    public void setUp() throws Exception {
        super.setUp();
        // Pre-condition: Stateless utility/value class; no game state required
    }

    // ------------------------------------------------------------------
    // Pre-condition helpers & stubs
    // ------------------------------------------------------------------

    @SuppressWarnings("unchecked")
    private static <T> T invokeSelect(Collection<RandomChoice<T>> input, int probability)
            throws Exception {
        Method m = RandomChoice.class.getDeclaredMethod("select", Collection.class, int.class);
        m.setAccessible(true);
        return (T) m.invoke(null, input, probability);
    }

    // ------------------------------------------------------------------
    // M1: <init>(Ljava/lang/Object;I)V — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M1-TP1: ENTRY -> N0 -> EXIT (constructs RandomChoice with object and probability)
    public void testM1_TP1_Constructor() {
        RandomChoice<String> rc = new RandomChoice<>("gold", 25);
        assertEquals("gold", rc.getObject());
        assertEquals(25, rc.getProbability());
    }

    // ------------------------------------------------------------------
    // M2: getProbability()I — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M2-TP1: ENTRY -> N0 -> EXIT (returns probability field)
    public void testM2_TP1_GetProbability() {
        RandomChoice<String> rc = new RandomChoice<>("silver", 40);
        assertEquals(40, rc.getProbability());
    }

    // ------------------------------------------------------------------
    // M3: getObject()Ljava/lang/Object; — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M3-TP1: ENTRY -> N0 -> EXIT (returns object field)
    public void testM3_TP1_GetObject() {
        RandomChoice<String> rc = new RandomChoice<>("furs", 10);
        assertEquals("furs", rc.getObject());
    }

    // ------------------------------------------------------------------
    // M4: select(Ljava/util/Collection;I)Ljava/lang/Object; — 3 Test Paths (100% feasible C3)
    // ------------------------------------------------------------------

    // M4-TP1: ENTRY -> N0 -(B1:false)-> N1 -> EXIT (input.isEmpty() == true returns null)
    public void testM4_TP1_SelectEmpty() throws Exception {
        assertNull(invokeSelect(Collections.emptyList(), 0));
    }

    // M4-TP2: ENTRY -> N0 -(B1:true)-> N2 -> N3 -(B2:false)-> N4 -(B3:false)-> N5 -> EXIT (probability < first item's probability)
    public void testM4_TP2_SelectFirstItem() throws Exception {
        List<RandomChoice<String>> list = Arrays.asList(
            new RandomChoice<>("first", 10),
            new RandomChoice<>("second", 20)
        );
        assertEquals("first", invokeSelect(list, 5));
    }

    // M4-TP3: ENTRY -> N0 -(B1:true)-> N2 -> N3 -(B2:false)-> N4 -(B3:true)-> N6 -> N3 -(B2:false)-> N4 -(B3:false)-> N5 -> EXIT (advances to second item)
    public void testM4_TP3_SelectSecondItem() throws Exception {
        List<RandomChoice<String>> list = Arrays.asList(
            new RandomChoice<>("first", 10),
            new RandomChoice<>("second", 20)
        );
        assertEquals("second", invokeSelect(list, 15));
    }

    // M4-TP4: ENTRY -> N0 -(B1:true)-> N2 -> N3 -(B2:false)-> N4 -(B3:true)-> N6 -> N3 -(B2:true)-> N7 -> EXIT (probability >= total falls back to first)
    public void testM4_TP4_SelectOutOfBoundsFallback() throws Exception {
        List<RandomChoice<String>> list = Arrays.asList(
            new RandomChoice<>("first", 10),
            new RandomChoice<>("second", 20)
        );
        assertEquals("first", invokeSelect(list, 100));
    }

    // ------------------------------------------------------------------
    // M5: getTotalProbability(Ljava/util/Collection;)I — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M5-TP1: ENTRY -> N0 -> EXIT (sums probabilities via CollectionUtils.sum)
    public void testM5_TP1_GetTotalProbability() {
        List<RandomChoice<String>> list = Arrays.asList(
            new RandomChoice<>("a", 15),
            new RandomChoice<>("b", 35)
        );
        assertEquals(50, RandomChoice.getTotalProbability(list));
    }

    // ------------------------------------------------------------------
    // M6: getWeightedRandom(Logger, String, Collection, Random) — 5 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M6-TP1: ENTRY -> N0 -(B1:true)-> N3 -> N7 -> EXIT (input == null returns null)
    public void testM6_TP1_GetWeightedRandomNullCollection() {
        assertNull(RandomChoice.getWeightedRandom(LOGGER, "test", (Collection<RandomChoice<String>>) null, new Random(1L)));
    }

    // M6-TP2: ENTRY -> N0 -(B1:false)-> N1 -(B2:true)-> N3 -> N7 -> EXIT (input.isEmpty() == true returns null)
    public void testM6_TP2_GetWeightedRandomEmptyCollection() {
        assertNull(RandomChoice.getWeightedRandom(LOGGER, "test", Collections.<RandomChoice<String>>emptyList(), new Random(1L)));
    }

    // M6-TP3: ENTRY -> N0 -(B1:false)-> N1 -(B2:false)-> N2 -(B3:false)-> N3 -> N7 -> EXIT (total probability <= 0 returns null)
    public void testM6_TP3_GetWeightedRandomZeroTotal() {
        List<RandomChoice<String>> list = Arrays.asList(
            new RandomChoice<>("a", 0),
            new RandomChoice<>("b", 0)
        );
        assertNull(RandomChoice.getWeightedRandom(LOGGER, "test", list, new Random(1L)));
    }

    // M6-TP4: ENTRY -> N0 -(B1:false)-> N1 -(B2:false)-> N2 -(B3:true)-> N4 -(B4:false)-> N5 -> N7 -> EXIT (single element returns first directly)
    public void testM6_TP4_GetWeightedRandomSingleElement() {
        List<RandomChoice<String>> list = Collections.singletonList(
            new RandomChoice<>("only", 10)
        );
        assertEquals("only", RandomChoice.getWeightedRandom(LOGGER, "test", list, new Random(1L)));
    }

    // M6-TP5: ENTRY -> N0 -(B1:false)-> N1 -(B2:false)-> N2 -(B3:true)-> N4 -(B4:true)-> N6 -> N7 -> EXIT (multiple elements delegates to select)
    public void testM6_TP5_GetWeightedRandomMultipleElements() {
        List<RandomChoice<String>> list = Arrays.asList(
            new RandomChoice<>("a", 10),
            new RandomChoice<>("b", 20)
        );
        String picked = RandomChoice.getWeightedRandom(LOGGER, "test", list, new Random(42L));
        assertTrue("a".equals(picked) || "b".equals(picked));
    }

    // ------------------------------------------------------------------
    // M7: getWeightedRandom(Logger, String, Stream, Random) — 2 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M7-TP1: ENTRY -> N0 -(B1:false)-> N1 -> N3 -> EXIT (stream == null returns null)
    public void testM7_TP1_GetWeightedRandomNullStream() {
        assertNull(RandomChoice.getWeightedRandom(LOGGER, "test", (Stream<RandomChoice<String>>) null, new Random(1L)));
    }

    // M7-TP2: ENTRY -> N0 -(B1:true)-> N2 -> N3 -> EXIT (stream != null collects to list and delegates)
    public void testM7_TP2_GetWeightedRandomNonNullStream() {
        Stream<RandomChoice<String>> stream = Stream.of(new RandomChoice<>("streamItem", 5));
        assertEquals("streamItem", RandomChoice.getWeightedRandom(LOGGER, "test", stream, new Random(1L)));
    }

    // ------------------------------------------------------------------
    // M8: normalize(Ljava/util/Collection;I)V — 4 Test Paths (100% feasible C3)
    // ------------------------------------------------------------------

    // M8-TP1: ENTRY -> N0 -(B1:true)-> N3 -> EXIT (input == null returns immediately)
    public void testM8_TP1_NormalizeNull() {
        RandomChoice.normalize(null, 100);
    }

    // M8-TP2: ENTRY -> N0 -(B1:false)-> N1 -(B2:true)-> N3 -> EXIT (input.isEmpty() returns immediately)
    public void testM8_TP2_NormalizeEmpty() {
        RandomChoice.normalize(Collections.<RandomChoice<String>>emptyList(), 100);
    }

    // M8-TP3: ENTRY -> N0 -(B1:false)-> N1 -(B2:false)-> N2 -(B3:false)-> N3 -> EXIT (total <= 0 returns immediately)
    public void testM8_TP3_NormalizeNonPositiveTotal() {
        List<RandomChoice<String>> list = Collections.singletonList(new RandomChoice<>("a", 0));
        RandomChoice.normalize(list, 100);
        assertEquals(0, list.get(0).getProbability());
    }

    // M8-TP4: ENTRY -> N0 -(B1:false)-> N1 -(B2:false)-> N2 -(B3:true)-> N4 -> N5 -(B4:false)-> N6 -> N5 -(B4:true)-> N7 -> EXIT (scales probabilities)
    public void testM8_TP4_NormalizePositiveTotal() {
        List<RandomChoice<String>> list = Arrays.asList(
            new RandomChoice<>("a", 1),
            new RandomChoice<>("b", 3)
        );
        RandomChoice.normalize(list, 100);
        assertEquals(25, list.get(0).getProbability());
        assertEquals(75, list.get(1).getProbability());
    }
}
