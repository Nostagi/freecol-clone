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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;
import java.lang.reflect.Method;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.BinaryOperator;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.ToDoubleFunction;
import java.util.function.ToIntFunction;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import net.sf.freecol.util.test.FreeColTestCase;


/**
 * CFG-driven test suite for {@link CollectionUtils} derived from
 * {@code test_cft/graph/net/sf/freecol/common/util/CollectionUtils.json}.
 *
 * Covers all 197 methods (M1..M197) and achieves 100% feasible C3 (node + edge)
 * coverage across 313 Test Paths.
 *
 * Infeasible edges documented:
 * - None (100% of CFG nodes and edges in CollectionUtils.json are feasible).
 */
public class CollectionUtilsCfgTest extends FreeColTestCase {

    @Override
    public void setUp() throws Exception {
        super.setUp();
        // Pre-condition: Standard FreeColTestCase environment initialized;
        // CollectionUtils methods operate on standard Java collections, arrays,
        // streams, files, and functional interfaces.
    }

    // ------------------------------------------------------------------
    // Pre-condition helpers & stubs
    // ------------------------------------------------------------------

    /**
     * ArrayList subclass supporting negative index removal for testing M12
     * negative rotation back-edge (N6 -> N7 -> N6).
     */
    private static class NegativeIndexArrayList<T> extends ArrayList<T> {
        NegativeIndexArrayList(Collection<? extends T> c) {
            super(c);
        }

        @Override
        public T remove(int index) {
            int resolved = (index < 0) ? Math.floorMod(index, size()) : index;
            return super.remove(resolved);
        }
    }

    /**
     * File stub representing a directory whose {@code listFiles()} returns null,
     * used to exercise M48 B3:false.
     */
    private static class NullListFilesDir extends File {
        NullListFilesDir() {
            super(".");
        }

        @Override
        public boolean isDirectory() {
            return true;
        }

        @Override
        public File[] listFiles() {
            return null;
        }
    }

    private static void withSuppressedErr(Runnable action) {
        PrintStream orig = System.err;
        try {
            System.setErr(new PrintStream(new ByteArrayOutputStream()));
            action.run();
        } finally {
            System.setErr(orig);
        }
    }

    @SuppressWarnings("unchecked")
    private static <R> R invokePrivate(String name, Class<?>[] paramTypes, Object... args)
            throws Exception {
        Method m = CollectionUtils.class.getDeclaredMethod(name, paramTypes);
        m.setAccessible(true);
        return (R) m.invoke(null, args);
    }

    // ------------------------------------------------------------------
    // M1: <init>()V — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M1-TP1: ENTRY -> N0 -> EXIT (default constructor)
    public void testM1_TP1_constructor() {
        CollectionUtils cu = new CollectionUtils();
        assertNotNull(cu);
    }

    // ------------------------------------------------------------------
    // M2: makeUnmodifiableSet(T...) — 2 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M2-TP1: ENTRY -> N0 -> N1 -(B1:true)-> N3 -> EXIT (empty varargs, 0 loop iterations)
    public void testM2_TP1_makeUnmodifiableSet_empty() {
        Set<String> res = CollectionUtils.makeUnmodifiableSet();
        assertTrue(res.isEmpty());
        try {
            res.add("x");
            fail("Expected UnsupportedOperationException");
        } catch (UnsupportedOperationException expected) {
        }
    }

    // M2-TP2: ENTRY -> N0 -> N1 -(B1:false)-> N2 -> N1 -(B1:true)-> N3 -> EXIT (non-empty varargs)
    public void testM2_TP2_makeUnmodifiableSet_nonEmpty() {
        Set<String> res = CollectionUtils.makeUnmodifiableSet("a", "b");
        assertEquals(2, res.size());
        assertTrue(res.contains("a"));
        assertTrue(res.contains("b"));
    }

    // ------------------------------------------------------------------
    // M3: makeUnmodifiableList(T...) — 2 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M3-TP1: ENTRY -> N0 -> N1 -(B1:true)-> N3 -> EXIT (empty varargs, 0 loop iterations)
    public void testM3_TP1_makeUnmodifiableList_empty() {
        List<String> res = CollectionUtils.makeUnmodifiableList();
        assertTrue(res.isEmpty());
        try {
            res.add("x");
            fail("Expected UnsupportedOperationException");
        } catch (UnsupportedOperationException expected) {
        }
    }

    // M3-TP2: ENTRY -> N0 -> N1 -(B1:false)-> N2 -> N1 -(B1:true)-> N3 -> EXIT (non-empty varargs)
    public void testM3_TP2_makeUnmodifiableList_nonEmpty() {
        List<String> res = CollectionUtils.makeUnmodifiableList("a", "b");
        assertEquals(Arrays.asList("a", "b"), res);
    }

    // ------------------------------------------------------------------
    // M4: makeUnmodifiableMap(K[], V[]) — 3 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M4-TP1: ENTRY -> N0 -(B1:true)-> N2 -> N3 -(B2:true)-> N5 -> EXIT (matching empty arrays)
    public void testM4_TP1_makeUnmodifiableMap_empty() {
        Map<String, Integer> res = CollectionUtils.makeUnmodifiableMap(
            new String[0], new Integer[0]);
        assertTrue(res.isEmpty());
    }

    // M4-TP2: ENTRY -> N0 -(B1:true)-> N2 -> N3 -(B2:false)-> N4 -> N3 -(B2:true)-> N5 -> EXIT (matching non-empty arrays)
    public void testM4_TP2_makeUnmodifiableMap_nonEmpty() {
        Map<String, Integer> res = CollectionUtils.makeUnmodifiableMap(
            new String[] { "k1", "k2" }, new Integer[] { 1, 2 });
        assertEquals(Integer.valueOf(1), res.get("k1"));
        assertEquals(Integer.valueOf(2), res.get("k2"));
    }

    // M4-TP3: ENTRY -> N0 -(B1:false)-> N1 -> EXIT (length mismatch throws RuntimeException)
    public void testM4_TP3_makeUnmodifiableMap_lengthMismatch() {
        try {
            CollectionUtils.makeUnmodifiableMap(new String[] { "k1" }, new Integer[0]);
            fail("Expected RuntimeException");
        } catch (RuntimeException expected) {
            assertTrue(expected.getMessage().contains("Length mismatch"));
        }
    }

    // ------------------------------------------------------------------
    // M5: asMap(T...) — 2 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M5-TP1: ENTRY -> N0 -> N1 -(B1:true)-> N3 -> EXIT (fewer than 2 elements)
    public void testM5_TP1_asMap_emptyOrOddOne() {
        assertTrue(CollectionUtils.asMap().isEmpty());
        assertTrue(CollectionUtils.asMap("onlyOne").isEmpty());
    }

    // M5-TP2: ENTRY -> N0 -> N1 -(B1:false)-> N2 -> N1 -(B1:true)-> N3 -> EXIT (at least 2 elements)
    public void testM5_TP2_asMap_pairs() {
        Map<String, String> res = CollectionUtils.asMap("k1", "v1", "k2", "v2");
        assertEquals("v1", res.get("k1"));
        assertEquals("v2", res.get("k2"));
    }

    // ------------------------------------------------------------------
    // M6: appendToMapList(Map, K, T) — 3 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M6-TP1: ENTRY -> N0 -(B1:true)-> N2 -(B2:true)-> N4 -> EXIT (list exists and already contains value)
    public void testM6_TP1_appendToMapList_duplicateIgnored() {
        Map<String, List<String>> map = new HashMap<>();
        map.put("k", new ArrayList<>(Arrays.asList("v1")));
        CollectionUtils.appendToMapList(map, "k", "v1");
        assertEquals(Collections.singletonList("v1"), map.get("k"));
    }

    // M6-TP2: ENTRY -> N0 -(B1:true)-> N2 -(B2:false)-> N3 -> N4 -> EXIT (list exists, value absent)
    public void testM6_TP2_appendToMapList_appendToExisting() {
        Map<String, List<String>> map = new HashMap<>();
        map.put("k", new ArrayList<>(Arrays.asList("v1")));
        CollectionUtils.appendToMapList(map, "k", "v2");
        assertEquals(Arrays.asList("v1", "v2"), map.get("k"));
    }

    // M6-TP3: ENTRY -> N0 -(B1:false)-> N1 -> N4 -> EXIT (key absent, l == null)
    public void testM6_TP3_appendToMapList_newKey() {
        Map<String, List<String>> map = new HashMap<>();
        CollectionUtils.appendToMapList(map, "k", "v1");
        assertEquals(Collections.singletonList("v1"), map.get("k"));
    }

    // ------------------------------------------------------------------
    // M7: accumulateToMap(Map, K, V, BinaryOperator) — 2 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M7-TP1: ENTRY -> N0 -(B1:true)-> N2 -> N3 -> EXIT (val != null, uses accumulator)
    public void testM7_TP1_accumulateToMap_existingKey() {
        Map<String, Integer> map = new HashMap<>();
        map.put("a", 10);
        CollectionUtils.accumulateToMap(map, "a", 5, CollectionUtils.integerAccumulator);
        assertEquals(Integer.valueOf(15), map.get("a"));
    }

    // M7-TP2: ENTRY -> N0 -(B1:false)-> N1 -> N3 -> EXIT (val == null, puts value directly)
    public void testM7_TP2_accumulateToMap_newKey() {
        Map<String, Integer> map = new HashMap<>();
        CollectionUtils.accumulateToMap(map, "a", 5, CollectionUtils.integerAccumulator);
        assertEquals(Integer.valueOf(5), map.get("a"));
    }

    // ------------------------------------------------------------------
    // M8: accumulateMap(Map, Map, BinaryOperator) — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M8-TP1: ENTRY -> N0 -> EXIT (delegates to forEachMapEntry + accumulateToMap)
    public void testM8_TP1_accumulateMap() {
        Map<String, Integer> m1 = new HashMap<>();
        m1.put("a", 1);
        Map<String, Integer> m2 = new HashMap<>();
        m2.put("a", 2);
        m2.put("b", 3);
        CollectionUtils.accumulateMap(m1, m2, CollectionUtils.integerAccumulator);
        assertEquals(Integer.valueOf(3), m1.get("a"));
        assertEquals(Integer.valueOf(3), m1.get("b"));
    }

    // ------------------------------------------------------------------
    // M9: incrementMapCount(Map, K) — 2 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M9-TP1: ENTRY -> N0 -(B1:true)-> N2 -> N3 -> EXIT (val != null)
    public void testM9_TP1_incrementMapCount_existing() {
        Map<String, Integer> map = new HashMap<>();
        map.put("k", 2);
        assertEquals(3, CollectionUtils.incrementMapCount(map, "k"));
    }

    // M9-TP2: ENTRY -> N0 -(B1:false)-> N1 -> N3 -> EXIT (val == null)
    public void testM9_TP2_incrementMapCount_new() {
        Map<String, Integer> map = new HashMap<>();
        assertEquals(1, CollectionUtils.incrementMapCount(map, "k"));
    }

    // ------------------------------------------------------------------
    // M10: getPermutations(List) — 2 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M10-TP1: ENTRY -> N0 -(B1:true)-> N2 -> EXIT (l != null)
    public void testM10_TP1_getPermutations_nonNull() {
        Iterable<List<String>> perms = CollectionUtils.getPermutations(Arrays.asList("a", "b"));
        assertNotNull(perms);
        List<List<String>> collected = new ArrayList<>();
        for (List<String> p : perms) {
            collected.add(p);
        }
        assertEquals(2, collected.size());
    }

    // M10-TP2: ENTRY -> N0 -(B1:false)-> N1 -> EXIT (l == null)
    public void testM10_TP2_getPermutations_null() {
        assertNull(CollectionUtils.getPermutations(null));
    }

    // ------------------------------------------------------------------
    // M11: allSame(Collection) — 3 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M11-TP1: ENTRY -> N0 -> N1 -(B1:true)-> N7 -> EXIT (empty collection)
    public void testM11_TP1_allSame_empty() {
        assertTrue(CollectionUtils.allSame(Collections.emptyList()));
    }

    // M11-TP2: ENTRY -> N0 -> N1 -(B1:false)-> N2 -(B2:false)-> N3 -> N6 -> N1 -(B1:false)-> N2 -(B2:true)-> N4 -(B3:true)-> N6 -> N1 -(B1:true)-> N7 -> EXIT
    public void testM11_TP2_allSame_allEqualByIdentity() {
        String s = "same";
        assertTrue(CollectionUtils.allSame(Arrays.asList(s, s)));
    }

    // M11-TP3: ENTRY -> N0 -> N1 -(B1:false)-> N2 -(B2:false)-> N3 -> N6 -> N1 -(B1:false)-> N2 -(B2:true)-> N4 -(B3:false)-> N5 -> EXIT
    public void testM11_TP3_allSame_different() {
        assertFalse(CollectionUtils.allSame(Arrays.asList("a", "b")));
    }

    // ------------------------------------------------------------------
    // M12: rotate(List, int) — 5 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M12-TP1: ENTRY -> N0 -(B1:true)-> N2 -> EXIT (len <= 0)
    public void testM12_TP1_rotate_emptyList() {
        List<String> list = new ArrayList<>();
        CollectionUtils.rotate(list, 1);
        assertTrue(list.isEmpty());
    }

    // M12-TP2: ENTRY -> N0 -(B1:false)-> N1 -(B2:false)-> N2 -> EXIT (len > 0, n == 0)
    public void testM12_TP2_rotate_zeroN() {
        List<String> list = new ArrayList<>(Arrays.asList("a", "b"));
        CollectionUtils.rotate(list, 0);
        assertEquals(Arrays.asList("a", "b"), list);
    }

    // M12-TP3: ENTRY -> N0 -(B1:false)-> N1 -(B2:true)-> N3 -(B3:false)-> N4 -(B4:false)-> N5 -> N4 -(B4:true)-> N8 -> EXIT (n % len > 0)
    public void testM12_TP3_rotate_positive() {
        List<String> list = new ArrayList<>(Arrays.asList("a", "b", "c"));
        CollectionUtils.rotate(list, 1);
        assertEquals(Arrays.asList("b", "c", "a"), list);
    }

    // M12-TP4: ENTRY -> N0 -(B1:false)-> N1 -(B2:true)-> N3 -(B3:true)-> N6 -(B5:true)-> N8 -> EXIT (n != 0 but n % len == 0)
    public void testM12_TP4_rotate_multipleOfLen() {
        List<String> list = new ArrayList<>(Arrays.asList("a", "b"));
        CollectionUtils.rotate(list, 2);
        assertEquals(Arrays.asList("a", "b"), list);
    }

    // M12-TP5: ENTRY -> N0 -(B1:false)-> N1 -(B2:true)-> N3 -(B3:true)-> N6 -(B5:false)-> N7 -> N6 -(B5:true)-> N8 -> EXIT (n % len < 0)
    public void testM12_TP5_rotate_negative() {
        List<String> list = new NegativeIndexArrayList<>(Arrays.asList("a", "b", "c"));
        CollectionUtils.rotate(list, -1);
        assertEquals(Arrays.asList("b", "a", "c"), list);
    }

    // ------------------------------------------------------------------
    // M13: reverse(List) — 2 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M13-TP1: ENTRY -> N0 -(B1:false)-> N1 -> EXIT (len <= 0)
    public void testM13_TP1_reverse_empty() {
        List<String> list = new ArrayList<>();
        CollectionUtils.reverse(list);
        assertTrue(list.isEmpty());
    }

    // M13-TP2: ENTRY -> N0 -(B1:true)-> N2 -> N3 -(B2:false)-> N4 -> N3 -(B2:true)-> N5 -> EXIT (len > 0)
    public void testM13_TP2_reverse_nonEmpty() {
        List<String> list = new ArrayList<>(Arrays.asList("a", "b", "c"));
        CollectionUtils.reverse(list);
        assertEquals(Arrays.asList("c", "b", "a"), list);
    }

    // ------------------------------------------------------------------
    // M14: listEquals(List, List) — 8 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M14-TP1: ENTRY -> N0 -(B1:true)-> N5 -(B3:true)-> N7 -> N8 -(B4:true)-> N12 -(B7:false)-> N13 -> N15 -> EXIT (both empty)
    public void testM14_TP1_listEquals_bothEmpty() {
        assertTrue(CollectionUtils.listEquals(Collections.emptyList(), Collections.emptyList()));
    }

    // M14-TP2: ENTRY -> N0 -(B1:true)-> N5 -(B3:true)-> N7 -> N8 -(B4:true)-> N12 -(B7:true)-> N14 -> N15 -> EXIT (one shorter)
    public void testM14_TP2_listEquals_firstShorter() {
        assertFalse(CollectionUtils.listEquals(Collections.emptyList(), Collections.singletonList("a")));
    }

    // M14-TP3: ENTRY -> N0 -(B1:true)-> N5 -(B3:true)-> N7 -> N8 -(B4:false)-> N9 -(B5:true)-> N16 -> EXIT (two shorter)
    public void testM14_TP3_listEquals_secondShorter() {
        assertFalse(CollectionUtils.listEquals(Collections.singletonList("a"), Collections.emptyList()));
    }

    // M14-TP4: ENTRY -> N0 -(B1:true)-> N5 -(B3:true)-> N7 -> N8 -(B4:false)-> N9 -(B5:false)-> N10 -(B6:false)-> N11 -> N16 -> EXIT (mismatch)
    public void testM14_TP4_listEquals_elementMismatch() {
        assertFalse(CollectionUtils.listEquals(Collections.singletonList("a"), Collections.singletonList("b")));
    }

    // M14-TP5: ENTRY -> N0 -(B1:true)-> N5 -(B3:true)-> N7 -> N8 -(B4:false)-> N9 -(B5:false)-> N10 -(B6:true)-> N8 -(B4:true)-> N12 -(B7:false)-> N13 -> N15 -> EXIT (equal)
    public void testM14_TP5_listEquals_equalNonEmpty() {
        assertTrue(CollectionUtils.listEquals(Arrays.asList("a", "b"), Arrays.asList("a", "b")));
    }

    // M14-TP6: ENTRY -> N0 -(B1:true)-> N5 -(B3:false)-> N6 -> EXIT (one != null, two == null)
    public void testM14_TP6_listEquals_secondNull() {
        assertFalse(CollectionUtils.listEquals(Collections.emptyList(), null));
    }

    // M14-TP7: ENTRY -> N0 -(B1:false)-> N1 -(B2:true)-> N3 -> N4 -> EXIT (both null)
    public void testM14_TP7_listEquals_bothNull() {
        assertTrue(CollectionUtils.listEquals(null, null));
    }

    // M14-TP8: ENTRY -> N0 -(B1:false)-> N1 -(B2:false)-> N2 -> N4 -> EXIT (one == null, two != null)
    public void testM14_TP8_listEquals_firstNull() {
        assertFalse(CollectionUtils.listEquals(null, Collections.emptyList()));
    }

    // ------------------------------------------------------------------
    // M15..M18: mapEntriesByKey / mapEntriesByValue — 4 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M15-TP1: ENTRY -> N0 -> EXIT (mapEntriesByKey(Map))
    public void testM15_TP1_mapEntriesByKey() {
        Map<String, Integer> m = new HashMap<>();
        m.put("b", 2);
        m.put("a", 1);
        List<Entry<String, Integer>> res = CollectionUtils.mapEntriesByKey(m);
        assertEquals("a", res.get(0).getKey());
        assertEquals("b", res.get(1).getKey());
    }

    // M16-TP1: ENTRY -> N0 -> EXIT (mapEntriesByKey(Map, Comparator))
    public void testM16_TP1_mapEntriesByKey_comparator() {
        Map<String, Integer> m = new HashMap<>();
        m.put("a", 1);
        m.put("b", 2);
        List<Entry<String, Integer>> res = CollectionUtils.mapEntriesByKey(
            m, Comparator.reverseOrder());
        assertEquals("b", res.get(0).getKey());
        assertEquals("a", res.get(1).getKey());
    }

    // M17-TP1: ENTRY -> N0 -> EXIT (mapEntriesByValue(Map))
    public void testM17_TP1_mapEntriesByValue() {
        Map<String, Integer> m = new HashMap<>();
        m.put("b", 20);
        m.put("a", 10);
        List<Entry<String, Integer>> res = CollectionUtils.mapEntriesByValue(m);
        assertEquals(Integer.valueOf(10), res.get(0).getValue());
        assertEquals(Integer.valueOf(20), res.get(1).getValue());
    }

    // M18-TP1: ENTRY -> N0 -> EXIT (mapEntriesByValue(Map, Comparator))
    public void testM18_TP1_mapEntriesByValue_comparator() {
        Map<String, Integer> m = new HashMap<>();
        m.put("a", 10);
        m.put("b", 20);
        List<Entry<String, Integer>> res = CollectionUtils.mapEntriesByValue(
            m, CollectionUtils.descendingIntegerComparator);
        assertEquals(Integer.valueOf(20), res.get(0).getValue());
        assertEquals(Integer.valueOf(10), res.get(1).getValue());
    }

    // ------------------------------------------------------------------
    // M19..M23: all / all_internal / alwaysTrue — 8 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M19-TP1: ENTRY -> N0 -(B1:true)-> N2 -> N3 -> EXIT (array != null)
    public void testM19_TP1_all_array_nonNull() {
        assertTrue(CollectionUtils.all(new Integer[] { 2, 4 }, i -> i % 2 == 0));
        assertFalse(CollectionUtils.all(new Integer[] { 2, 3 }, i -> i % 2 == 0));
    }

    // M19-TP2: ENTRY -> N0 -(B1:false)-> N1 -> N3 -> EXIT (array == null)
    public void testM19_TP2_all_array_null() {
        assertTrue(CollectionUtils.all((Integer[]) null, i -> i > 0));
    }

    // M20-TP1: ENTRY -> N0 -(B1:true)-> N2 -> N3 -> EXIT (collection != null)
    public void testM20_TP1_all_collection_nonNull() {
        assertTrue(CollectionUtils.all(Arrays.asList(2, 4), i -> i % 2 == 0));
    }

    // M20-TP2: ENTRY -> N0 -(B1:false)-> N1 -> N3 -> EXIT (collection == null)
    public void testM20_TP2_all_collection_null() {
        assertTrue(CollectionUtils.all((Collection<Integer>) null, i -> i > 0));
    }

    // M21-TP1: ENTRY -> N0 -(B1:true)-> N2 -> N3 -> EXIT (stream != null)
    public void testM21_TP1_all_stream_nonNull() {
        assertTrue(CollectionUtils.all(Stream.of(2, 4), i -> i % 2 == 0));
    }

    // M21-TP2: ENTRY -> N0 -(B1:false)-> N1 -> N3 -> EXIT (stream == null)
    public void testM21_TP2_all_stream_null() {
        assertTrue(CollectionUtils.all((Stream<Integer>) null, i -> i > 0));
    }

    // M22-TP1: ENTRY -> N0 -> EXIT (all_internal via reflection)
    public void testM22_TP1_all_internal() throws Exception {
        boolean res = invokePrivate("all_internal",
            new Class<?>[] { Stream.class, Predicate.class },
            Stream.of(1, 2), (Predicate<Integer>) i -> i > 0);
        assertTrue(res);
    }

    // M23-TP1: ENTRY -> N0 -> EXIT (alwaysTrue)
    public void testM23_TP1_alwaysTrue() {
        Predicate<String> p = CollectionUtils.alwaysTrue();
        assertTrue(p.test("anything"));
        assertTrue(p.test(null));
    }

    // ------------------------------------------------------------------
    // M24..M30: any / any_internal — 14 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M24-TP1: ENTRY -> N0 -(B1:true)-> N3 -> N4 -> EXIT (array == null)
    public void testM24_TP1_any_array_null() {
        assertFalse(CollectionUtils.any((String[]) null));
    }

    // M24-TP2: ENTRY -> N0 -(B1:false)-> N1 -(B2:true)-> N3 -> N4 -> EXIT (array empty)
    public void testM24_TP2_any_array_empty() {
        assertFalse(CollectionUtils.any(new String[0]));
    }

    // M24-TP3: ENTRY -> N0 -(B1:false)-> N1 -(B2:false)-> N2 -> N4 -> EXIT (array non-empty)
    public void testM24_TP3_any_array_nonEmpty() {
        assertTrue(CollectionUtils.any(new String[] { "a" }));
    }

    // M25-TP1: ENTRY -> N0 -> EXIT (any(T[], Predicate))
    public void testM25_TP1_any_array_predicate() {
        assertTrue(CollectionUtils.any(new Integer[] { 1, 2 }, i -> i > 1));
    }

    // M26-TP1: ENTRY -> N0 -(B1:true)-> N3 -> N4 -> EXIT (collection == null)
    public void testM26_TP1_any_collection_null() {
        assertFalse(CollectionUtils.any((Collection<String>) null));
    }

    // M26-TP2: ENTRY -> N0 -(B1:false)-> N1 -(B2:true)-> N3 -> N4 -> EXIT (collection empty)
    public void testM26_TP2_any_collection_empty() {
        assertFalse(CollectionUtils.any(Collections.emptyList()));
    }

    // M26-TP3: ENTRY -> N0 -(B1:false)-> N1 -(B2:false)-> N2 -> N4 -> EXIT (collection non-empty)
    public void testM26_TP3_any_collection_nonEmpty() {
        assertTrue(CollectionUtils.any(Collections.singletonList("a")));
    }

    // M27-TP1: ENTRY -> N0 -(B1:true)-> N2 -> N3 -> EXIT (collection != null, predicate)
    public void testM27_TP1_any_collection_predicate_nonNull() {
        assertTrue(CollectionUtils.any(Arrays.asList(1, 2), i -> i == 2));
    }

    // M27-TP2: ENTRY -> N0 -(B1:false)-> N1 -> N3 -> EXIT (collection == null, predicate)
    public void testM27_TP2_any_collection_predicate_null() {
        assertFalse(CollectionUtils.any((Collection<Integer>) null, i -> i == 2));
    }

    // M28-TP1: ENTRY -> N0 -(B1:true)-> N3 -> N4 -> EXIT (stream == null)
    public void testM28_TP1_any_stream_null() {
        assertFalse(CollectionUtils.any((Stream<String>) null));
    }

    // M28-TP2: ENTRY -> N0 -(B1:false)-> N1 -(B2:true)-> N3 -> N4 -> EXIT (stream empty)
    public void testM28_TP2_any_stream_empty() {
        assertFalse(CollectionUtils.any(Stream.empty()));
    }

    // M28-TP3: ENTRY -> N0 -(B1:false)-> N1 -(B2:false)-> N2 -> N4 -> EXIT (stream non-empty)
    public void testM28_TP3_any_stream_nonEmpty() {
        assertTrue(CollectionUtils.any(Stream.of("a")));
    }

    // M29-TP1: ENTRY -> N0 -(B1:true)-> N2 -> N3 -> EXIT (stream != null, predicate)
    public void testM29_TP1_any_stream_predicate_nonNull() {
        assertTrue(CollectionUtils.any(Stream.of(1, 2), i -> i == 2));
    }

    // M29-TP2: ENTRY -> N0 -(B1:false)-> N1 -> N3 -> EXIT (stream == null, predicate)
    public void testM29_TP2_any_stream_predicate_null() {
        assertFalse(CollectionUtils.any((Stream<Integer>) null, i -> i == 2));
    }

    // M30-TP1: ENTRY -> N0 -> EXIT (any_internal via reflection)
    public void testM30_TP1_any_internal() throws Exception {
        boolean res = invokePrivate("any_internal",
            new Class<?>[] { Stream.class, Predicate.class },
            Stream.of(1, 2), (Predicate<Integer>) i -> i == 2);
        assertTrue(res);
    }

    // ------------------------------------------------------------------
    // M31..M34: cacheInt / cachingIntComparator / cacheDouble / cachingDoubleComparator — 4 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M31-TP1: ENTRY -> N0 -> EXIT (cacheInt)
    public void testM31_TP1_cacheInt() {
        ToIntFunction<String> fn = CollectionUtils.cacheInt(String::length);
        assertEquals(3, fn.applyAsInt("abc"));
        assertEquals(3, fn.applyAsInt("abc"));
    }

    // M32-TP1: ENTRY -> N0 -> EXIT (cachingIntComparator)
    public void testM32_TP1_cachingIntComparator() {
        Comparator<String> cmp = CollectionUtils.cachingIntComparator(String::length);
        assertTrue(cmp.compare("a", "bb") < 0);
    }

    // M33-TP1: ENTRY -> N0 -> EXIT (cacheDouble)
    public void testM33_TP1_cacheDouble() {
        ToDoubleFunction<String> fn = CollectionUtils.cacheDouble(s -> (double) s.length());
        assertEquals(3.0, fn.applyAsDouble("abc"), 0.0001);
    }

    // M34-TP1: ENTRY -> N0 -> EXIT (cachingDoubleComparator)
    public void testM34_TP1_cachingDoubleComparator() {
        Comparator<String> cmp = CollectionUtils.cachingDoubleComparator(s -> (double) s.length());
        assertTrue(cmp.compare("bb", "a") > 0);
    }

    // ------------------------------------------------------------------
    // M35: concat(Stream, Stream...) — 3 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M35-TP1: ENTRY -> N0 -(B1:true)-> N2 -> N3 -> N4 -(B2:true)-> N8 -> EXIT (s0 != null, empty varargs)
    public void testM35_TP1_concat_s0NonNull_noExtra() {
        List<String> res = CollectionUtils.concat(Stream.of("a"))
            .collect(Collectors.toList());
        assertEquals(Collections.singletonList("a"), res);
    }

    // M35-TP2: ENTRY -> N0 -(B1:false)-> N1 -> N3 -> N4 -(B2:true)-> N8 -> EXIT (s0 == null, empty varargs)
    public void testM35_TP2_concat_s0Null_noExtra() {
        List<String> res = CollectionUtils.concat((Stream<String>) null)
            .collect(Collectors.toList());
        assertTrue(res.isEmpty());
    }

    // M35-TP3: ENTRY -> N0 -(B1:true)-> N2 -> N3 -> N4 -(B2:false)-> N5 -(B3:true/false)-> N6/N7 -> N4 -(B2:true)-> N8 -> EXIT
    @SuppressWarnings("unchecked")
    public void testM35_TP3_concat_withNullAndNonNullStreams() {
        List<String> res = CollectionUtils.concat(Stream.of("a"), null, Stream.of("b"))
            .collect(Collectors.toList());
        assertEquals(Arrays.asList("a", "b"), res);
    }

    // ------------------------------------------------------------------
    // M36..M42: count / count_internal — 11 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M36-TP1: ENTRY -> N0 -(B1:true)-> N2 -> N3 -> EXIT (count(T[]) non-null)
    public void testM36_TP1_count_array_nonNull() {
        assertEquals(2, CollectionUtils.count(new String[] { "a", "b" }));
    }

    // M36-TP2: ENTRY -> N0 -(B1:false)-> N1 -> N3 -> EXIT (count(T[]) null)
    public void testM36_TP2_count_array_null() {
        assertEquals(0, CollectionUtils.count((String[]) null));
    }

    // M37-TP1: ENTRY -> N0 -> EXIT (count(T[], Predicate))
    public void testM37_TP1_count_array_predicate() {
        assertEquals(1, CollectionUtils.count(new Integer[] { 1, 2 }, i -> i > 1));
    }

    // M38-TP1: ENTRY -> N0 -(B1:true)-> N2 -> N3 -> EXIT (count(Collection) non-null)
    public void testM38_TP1_count_collection_nonNull() {
        assertEquals(2, CollectionUtils.count(Arrays.asList("a", "b")));
    }

    // M38-TP2: ENTRY -> N0 -(B1:false)-> N1 -> N3 -> EXIT (count(Collection) null)
    public void testM38_TP2_count_collection_null() {
        assertEquals(0, CollectionUtils.count((Collection<String>) null));
    }

    // M39-TP1: ENTRY -> N0 -> EXIT (count(Collection, Predicate))
    public void testM39_TP1_count_collection_predicate() {
        assertEquals(1, CollectionUtils.count(Arrays.asList(1, 2), i -> i > 1));
    }

    // M40-TP1: ENTRY -> N0 -(B1:true)-> N2 -> N3 -> EXIT (count(Stream) non-null)
    public void testM40_TP1_count_stream_nonNull() {
        assertEquals(2, CollectionUtils.count(Stream.of("a", "b")));
    }

    // M40-TP2: ENTRY -> N0 -(B1:false)-> N1 -> N3 -> EXIT (count(Stream) null)
    public void testM40_TP2_count_stream_null() {
        assertEquals(0, CollectionUtils.count((Stream<String>) null));
    }

    // M41-TP1: ENTRY -> N0 -(B1:true)-> N2 -> N3 -> EXIT (count(Stream, Predicate) non-null)
    public void testM41_TP1_count_stream_predicate_nonNull() {
        assertEquals(1, CollectionUtils.count(Stream.of(1, 2), i -> i > 1));
    }

    // M41-TP2: ENTRY -> N0 -(B1:false)-> N1 -> N3 -> EXIT (count(Stream, Predicate) null)
    public void testM41_TP2_count_stream_predicate_null() {
        assertEquals(0, CollectionUtils.count((Stream<Integer>) null, i -> i > 1));
    }

    // M42-TP1: ENTRY -> N0 -> EXIT (count_internal via reflection)
    public void testM42_TP1_count_internal() throws Exception {
        int res = invokePrivate("count_internal",
            new Class<?>[] { Stream.class, Predicate.class },
            Stream.of(1, 2), (Predicate<Integer>) i -> i > 0);
        assertEquals(2, res);
    }

    // ------------------------------------------------------------------
    // M43..M47: dump / dump_internal — 7 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M43-TP1: ENTRY -> N0 -> EXIT (dump(String, T[]))
    public void testM43_TP1_dump_array() {
        withSuppressedErr(() -> CollectionUtils.dump("arr:", new String[] { "a", "b" }));
    }

    // M44-TP1: ENTRY -> N0 -> EXIT (dump(String, Collection))
    public void testM44_TP1_dump_collection() {
        withSuppressedErr(() -> CollectionUtils.dump("col:", Arrays.asList("a", "b")));
    }

    // M45-TP1: ENTRY -> N0 -> EXIT (dump(String, Stream))
    public void testM45_TP1_dump_stream() {
        withSuppressedErr(() -> CollectionUtils.dump("str:", Stream.of("a", "b")));
    }

    // M46-TP1: ENTRY -> N0 -(B1:true)-> N2 -> EXIT (dump_internal header == null)
    public void testM46_TP1_dump_internal_nullHeader() {
        withSuppressedErr(() -> CollectionUtils.dump(null, Stream.of("a")));
    }

    // M46-TP2: ENTRY -> N0 -(B1:false)-> N1 -> N2 -> EXIT (dump_internal header != null)
    public void testM46_TP2_dump_internal_nonNullHeader() {
        withSuppressedErr(() -> CollectionUtils.dump("hdr:", Stream.of("a")));
    }

    // M47-TP1: ENTRY -> N0 -(B1:true)-> N2 -> EXIT (dump(String, Map) header == null)
    public void testM47_TP1_dump_map_nullHeader() {
        Map<String, Integer> m = new HashMap<>();
        m.put("k", 1);
        withSuppressedErr(() -> CollectionUtils.dump(null, m));
    }

    // M47-TP2: ENTRY -> N0 -(B1:false)-> N1 -> N2 -> EXIT (dump(String, Map) header != null)
    public void testM47_TP2_dump_map_nonNullHeader() {
        Map<String, Integer> m = new HashMap<>();
        m.put("k", 1);
        withSuppressedErr(() -> CollectionUtils.dump("map:", m));
    }

    // ------------------------------------------------------------------
    // M48..M49: fileStream — 5 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M48-TP1: ENTRY -> N0 -(B1:true)-> N3 -> N5 -> EXIT (dir == null)
    public void testM48_TP1_fileStream_null() {
        assertEquals(0, CollectionUtils.fileStream(null).count());
    }

    // M48-TP2: ENTRY -> N0 -(B1:false)-> N1 -(B2:true)-> N3 -> N5 -> EXIT (!dir.isDirectory())
    public void testM48_TP2_fileStream_notDirectory() {
        assertEquals(0, CollectionUtils.fileStream(new File("nonexistent_dir_cfg_test")).count());
    }

    // M48-TP3: ENTRY -> N0 -(B1:false)-> N1 -(B2:false)-> N2 -(B3:true)-> N4 -> N5 -> EXIT (valid directory)
    public void testM48_TP3_fileStream_validDirectory() {
        assertTrue(CollectionUtils.fileStream(new File(".")).count() > 0);
    }

    // M48-TP4: ENTRY -> N0 -(B1:false)-> N1 -(B2:false)-> N2 -(B3:false)-> N3 -> N5 -> EXIT (listFiles() == null)
    public void testM48_TP4_fileStream_nullListFiles() {
        assertEquals(0, CollectionUtils.fileStream(new NullListFilesDir()).count());
    }

    // M49-TP1: ENTRY -> N0 -> EXIT (fileStream(File, Predicate))
    public void testM49_TP1_fileStream_predicate() {
        assertTrue(CollectionUtils.fileStream(new File("."), f -> f != null).count() > 0);
    }

    // ------------------------------------------------------------------
    // M50..M56: find / find_internal — 9 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M50-TP1: ENTRY -> N0 -> EXIT (find(T[], Predicate))
    public void testM50_TP1_find_array() {
        assertEquals("b", CollectionUtils.find(new String[] { "a", "b" }, s -> s.equals("b")));
    }

    // M51-TP1: ENTRY -> N0 -> EXIT (find(T[], Predicate, T))
    public void testM51_TP1_find_array_failDefault() {
        assertEquals("fallback", CollectionUtils.find(new String[] { "a" }, s -> s.equals("z"), "fallback"));
    }

    // M52-TP1: ENTRY -> N0 -> EXIT (find(Collection, Predicate))
    public void testM52_TP1_find_collection() {
        assertEquals("b", CollectionUtils.find(Arrays.asList("a", "b"), s -> s.equals("b")));
    }

    // M53-TP1: ENTRY -> N0 -> EXIT (find(Collection, Predicate, T))
    public void testM53_TP1_find_collection_failDefault() {
        assertEquals("fallback", CollectionUtils.find(Arrays.asList("a"), s -> s.equals("z"), "fallback"));
    }

    // M54-TP1: ENTRY -> N0 -(B1:true)-> N2 -> N3 -> EXIT (find(Stream, Predicate) non-null)
    public void testM54_TP1_find_stream_nonNull() {
        assertEquals("b", CollectionUtils.find(Stream.of("a", "b"), s -> s.equals("b")));
    }

    // M54-TP2: ENTRY -> N0 -(B1:false)-> N1 -> N3 -> EXIT (find(Stream, Predicate) null)
    public void testM54_TP2_find_stream_null() {
        assertNull(CollectionUtils.find((Stream<String>) null, s -> true));
    }

    // M55-TP1: ENTRY -> N0 -(B1:true)-> N2 -> N3 -> EXIT (find(Stream, Predicate, T) non-null)
    public void testM55_TP1_find_stream_failDefault_nonNull() {
        assertEquals("fallback", CollectionUtils.find(Stream.of("a"), s -> s.equals("z"), "fallback"));
    }

    // M55-TP2: ENTRY -> N0 -(B1:false)-> N1 -> N3 -> EXIT (find(Stream, Predicate, T) null)
    public void testM55_TP2_find_stream_failDefault_null() {
        assertEquals("fallback", CollectionUtils.find((Stream<String>) null, s -> true, "fallback"));
    }

    // M56-TP1: ENTRY -> N0 -> EXIT (find_internal via reflection)
    public void testM56_TP1_find_internal() throws Exception {
        String res = invokePrivate("find_internal",
            new Class<?>[] { Stream.class, Predicate.class, Object.class },
            Stream.of("a", "b"), (Predicate<String>) s -> s.equals("b"), "fail");
        assertEquals("b", res);
    }

    // ------------------------------------------------------------------
    // M57..M60: first / first_internal — 9 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M57-TP1: ENTRY -> N0 -(B1:true)-> N2 -> N4 -> EXIT (first(T[]) null)
    public void testM57_TP1_first_array_null() {
        assertNull(CollectionUtils.first((String[]) null));
    }

    // M57-TP2: ENTRY -> N0 -(B1:false)-> N1 -(B2:false)-> N2 -> N4 -> EXIT (first(T[]) empty)
    public void testM57_TP2_first_array_empty() {
        assertNull(CollectionUtils.first(new String[0]));
    }

    // M57-TP3: ENTRY -> N0 -(B1:false)-> N1 -(B2:true)-> N3 -> N4 -> EXIT (first(T[]) non-empty)
    public void testM57_TP3_first_array_nonEmpty() {
        assertEquals("a", CollectionUtils.first(new String[] { "a", "b" }));
    }

    // M58-TP1: ENTRY -> N0 -(B1:true)-> N2 -> N4 -> EXIT (first(Collection) null)
    public void testM58_TP1_first_collection_null() {
        assertNull(CollectionUtils.first((Collection<String>) null));
    }

    // M58-TP2: ENTRY -> N0 -(B1:false)-> N1 -(B2:false)-> N2 -> N4 -> EXIT (first(Collection) empty)
    public void testM58_TP2_first_collection_empty() {
        assertNull(CollectionUtils.first(Collections.emptyList()));
    }

    // M58-TP3: ENTRY -> N0 -(B1:false)-> N1 -(B2:true)-> N3 -> N4 -> EXIT (first(Collection) non-empty)
    public void testM58_TP3_first_collection_nonEmpty() {
        assertEquals("a", CollectionUtils.first(Arrays.asList("a", "b")));
    }

    // M59-TP1: ENTRY -> N0 -(B1:true)-> N2 -> N3 -> EXIT (first(Stream) non-null)
    public void testM59_TP1_first_stream_nonNull() {
        assertEquals("a", CollectionUtils.first(Stream.of("a", "b")));
    }

    // M59-TP2: ENTRY -> N0 -(B1:false)-> N1 -> N3 -> EXIT (first(Stream) null)
    public void testM59_TP2_first_stream_null() {
        assertNull(CollectionUtils.first((Stream<String>) null));
    }

    // M60-TP1: ENTRY -> N0 -> EXIT (first_internal via reflection)
    public void testM60_TP1_first_internal() throws Exception {
        String res = invokePrivate("first_internal",
            new Class<?>[] { Stream.class, Object.class },
            Stream.empty(), "fail");
        assertEquals("fail", res);
    }

    // ------------------------------------------------------------------
    // M61..M67: flatten / flatten_internal — 9 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M61-TP1: ENTRY -> N0 -> EXIT (flatten(T[], Function))
    public void testM61_TP1_flatten_array() {
        List<String> res = CollectionUtils.flatten(
            new String[] { "a", "b" }, s -> Stream.of(s, s.toUpperCase()))
            .collect(Collectors.toList());
        assertEquals(Arrays.asList("a", "A", "b", "B"), res);
    }

    // M62-TP1: ENTRY -> N0 -> EXIT (flatten(T[], Predicate, Function))
    public void testM62_TP1_flatten_array_predicate() {
        List<String> res = CollectionUtils.flatten(
            new String[] { "a", "b" }, s -> s.equals("a"), s -> Stream.of(s))
            .collect(Collectors.toList());
        assertEquals(Collections.singletonList("a"), res);
    }

    // M63-TP1: ENTRY -> N0 -> EXIT (flatten(Collection, Function))
    public void testM63_TP1_flatten_collection() {
        List<String> res = CollectionUtils.flatten(
            Arrays.asList("a"), s -> Stream.of(s))
            .collect(Collectors.toList());
        assertEquals(Collections.singletonList("a"), res);
    }

    // M64-TP1: ENTRY -> N0 -> EXIT (flatten(Collection, Predicate, Function))
    public void testM64_TP1_flatten_collection_predicate() {
        List<String> res = CollectionUtils.flatten(
            Arrays.asList("a", "b"), s -> s.equals("b"), s -> Stream.of(s))
            .collect(Collectors.toList());
        assertEquals(Collections.singletonList("b"), res);
    }

    // M65-TP1: ENTRY -> N0 -(B1:true)-> N2 -> N3 -> EXIT (flatten(Stream, Function) non-null)
    public void testM65_TP1_flatten_stream_nonNull() {
        List<String> res = CollectionUtils.flatten(
            Stream.of("a"), s -> Stream.of(s))
            .collect(Collectors.toList());
        assertEquals(Collections.singletonList("a"), res);
    }

    // M65-TP2: ENTRY -> N0 -(B1:false)-> N1 -> N3 -> EXIT (flatten(Stream, Function) null)
    public void testM65_TP2_flatten_stream_null() {
        assertEquals(0, CollectionUtils.flatten((Stream<String>) null, s -> Stream.of(s)).count());
    }

    // M66-TP1: ENTRY -> N0 -(B1:true)-> N2 -> N3 -> EXIT (flatten(Stream, Predicate, Function) non-null)
    public void testM66_TP1_flatten_stream_predicate_nonNull() {
        List<String> res = CollectionUtils.flatten(
            Stream.of("a", "b"), s -> s.equals("a"), s -> Stream.of(s))
            .collect(Collectors.toList());
        assertEquals(Collections.singletonList("a"), res);
    }

    // M66-TP2: ENTRY -> N0 -(B1:false)-> N1 -> N3 -> EXIT (flatten(Stream, Predicate, Function) null)
    public void testM66_TP2_flatten_stream_predicate_null() {
        assertEquals(0, CollectionUtils.flatten((Stream<String>) null, s -> true, s -> Stream.of(s)).count());
    }

    // M67-TP1: ENTRY -> N0 -> EXIT (flatten_internal via reflection)
    public void testM67_TP1_flatten_internal() throws Exception {
        Stream<String> res = invokePrivate("flatten_internal",
            new Class<?>[] { Stream.class, Predicate.class, Function.class },
            Stream.of("a"), (Predicate<String>) s -> true, (Function<String, Stream<String>>) Stream::of);
        assertEquals(Collections.singletonList("a"), res.collect(Collectors.toList()));
    }

    // ------------------------------------------------------------------
    // M68..M76: forEachMapEntry / forEach / forEach_internal — 29 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M68-TP1: ENTRY -> N0 -(B1:true)-> N4 -> EXIT (consumer == null)
    public void testM68_TP1_forEachMapEntry_nullConsumer() {
        CollectionUtils.forEachMapEntry(Collections.singletonMap("k", 1), null);
    }

    // M68-TP2: ENTRY -> N0 -(B1:false)-> N1 -(B2:true)-> N4 -> EXIT (map == null)
    public void testM68_TP2_forEachMapEntry_nullMap() {
        CollectionUtils.forEachMapEntry((Map<String, Integer>) null, e -> {});
    }

    // M68-TP3: ENTRY -> N0 -(B1:false)-> N1 -(B2:false)-> N2 -(B3:true)-> N4 -> EXIT (map empty)
    public void testM68_TP3_forEachMapEntry_emptyMap() {
        List<String> keys = new ArrayList<>();
        CollectionUtils.forEachMapEntry(Collections.<String, Integer>emptyMap(), e -> keys.add(e.getKey()));
        assertTrue(keys.isEmpty());
    }

    // M68-TP4: ENTRY -> N0 -(B1:false)-> N1 -(B2:false)-> N2 -(B3:false)-> N3 -> N4 -> EXIT (non-empty map)
    public void testM68_TP4_forEachMapEntry_nonEmptyMap() {
        List<String> keys = new ArrayList<>();
        CollectionUtils.forEachMapEntry(Collections.singletonMap("k", 1), e -> keys.add(e.getKey()));
        assertEquals(Collections.singletonList("k"), keys);
    }

    // M69-TP1: ENTRY -> N0 -(B1:true)-> N4 -> EXIT (consumer == null)
    public void testM69_TP1_forEachMapEntry_pred_nullConsumer() {
        CollectionUtils.forEachMapEntry(Collections.singletonMap("k", 1), e -> true, null);
    }

    // M69-TP2: ENTRY -> N0 -(B1:false)-> N1 -(B2:true)-> N4 -> EXIT (map == null)
    public void testM69_TP2_forEachMapEntry_pred_nullMap() {
        CollectionUtils.forEachMapEntry((Map<String, Integer>) null, e -> true, e -> {});
    }

    // M69-TP3: ENTRY -> N0 -(B1:false)-> N1 -(B2:false)-> N2 -(B3:true)-> N4 -> EXIT (map empty)
    public void testM69_TP3_forEachMapEntry_pred_emptyMap() {
        List<String> keys = new ArrayList<>();
        CollectionUtils.forEachMapEntry(Collections.<String, Integer>emptyMap(), e -> true, e -> keys.add(e.getKey()));
        assertTrue(keys.isEmpty());
    }

    // M69-TP4: ENTRY -> N0 -(B1:false)-> N1 -(B2:false)-> N2 -(B3:false)-> N3 -> N4 -> EXIT (non-empty map)
    public void testM69_TP4_forEachMapEntry_pred_nonEmptyMap() {
        List<String> keys = new ArrayList<>();
        CollectionUtils.forEachMapEntry(Collections.singletonMap("k", 1), e -> e.getValue() == 1, e -> keys.add(e.getKey()));
        assertEquals(Collections.singletonList("k"), keys);
    }

    // M70-TP1: ENTRY -> N0 -(B1:true)-> N3 -> EXIT (array == null)
    public void testM70_TP1_forEach_array_nullArray() {
        CollectionUtils.forEach((String[]) null, s -> {});
    }

    // M70-TP2: ENTRY -> N0 -(B1:false)-> N1 -(B2:true)-> N3 -> EXIT (consumer == null)
    public void testM70_TP2_forEach_array_nullConsumer() {
        CollectionUtils.forEach(new String[] { "a" }, null);
    }

    // M70-TP3: ENTRY -> N0 -(B1:false)-> N1 -(B2:false)-> N2 -> N3 -> EXIT (non-null array & consumer)
    public void testM70_TP3_forEach_array_valid() {
        List<String> out = new ArrayList<>();
        CollectionUtils.forEach(new String[] { "a" }, out::add);
        assertEquals(Collections.singletonList("a"), out);
    }

    // M71-TP1: ENTRY -> N0 -(B1:true)-> N3 -> EXIT (array == null)
    public void testM71_TP1_forEach_array_pred_nullArray() {
        CollectionUtils.forEach((String[]) null, s -> true, s -> {});
    }

    // M71-TP2: ENTRY -> N0 -(B1:false)-> N1 -(B2:true)-> N3 -> EXIT (consumer == null)
    public void testM71_TP2_forEach_array_pred_nullConsumer() {
        CollectionUtils.forEach(new String[] { "a" }, s -> true, null);
    }

    // M71-TP3: ENTRY -> N0 -(B1:false)-> N1 -(B2:false)-> N2 -> N3 -> EXIT (non-null)
    public void testM71_TP3_forEach_array_pred_valid() {
        List<String> out = new ArrayList<>();
        CollectionUtils.forEach(new String[] { "a", "b" }, s -> s.equals("b"), out::add);
        assertEquals(Collections.singletonList("b"), out);
    }

    // M72-TP1: ENTRY -> N0 -(B1:true)-> N4 -> EXIT (c == null)
    public void testM72_TP1_forEach_col_nullCol() {
        CollectionUtils.forEach((Collection<String>) null, s -> {});
    }

    // M72-TP2: ENTRY -> N0 -(B1:false)-> N1 -(B2:true)-> N4 -> EXIT (c empty)
    public void testM72_TP2_forEach_col_emptyCol() {
        CollectionUtils.forEach(Collections.<String>emptyList(), s -> {});
    }

    // M72-TP3: ENTRY -> N0 -(B1:false)-> N1 -(B2:false)-> N2 -(B3:true)-> N4 -> EXIT (consumer == null)
    public void testM72_TP3_forEach_col_nullConsumer() {
        CollectionUtils.forEach(Collections.singletonList("a"), null);
    }

    // M72-TP4: ENTRY -> N0 -(B1:false)-> N1 -(B2:false)-> N2 -(B3:false)-> N3 -> N4 -> EXIT (valid)
    public void testM72_TP4_forEach_col_valid() {
        List<String> out = new ArrayList<>();
        CollectionUtils.forEach(Collections.singletonList("a"), out::add);
        assertEquals(Collections.singletonList("a"), out);
    }

    // M73-TP1: ENTRY -> N0 -(B1:true)-> N4 -> EXIT (c == null)
    public void testM73_TP1_forEach_col_pred_nullCol() {
        CollectionUtils.forEach((Collection<String>) null, s -> true, s -> {});
    }

    // M73-TP2: ENTRY -> N0 -(B1:false)-> N1 -(B2:true)-> N4 -> EXIT (c empty)
    public void testM73_TP2_forEach_col_pred_emptyCol() {
        CollectionUtils.forEach(Collections.<String>emptyList(), s -> true, s -> {});
    }

    // M73-TP3: ENTRY -> N0 -(B1:false)-> N1 -(B2:false)-> N2 -(B3:true)-> N4 -> EXIT (consumer == null)
    public void testM73_TP3_forEach_col_pred_nullConsumer() {
        CollectionUtils.forEach(Collections.singletonList("a"), s -> true, null);
    }

    // M73-TP4: ENTRY -> N0 -(B1:false)-> N1 -(B2:false)-> N2 -(B3:false)-> N3 -> N4 -> EXIT (valid)
    public void testM73_TP4_forEach_col_pred_valid() {
        List<String> out = new ArrayList<>();
        CollectionUtils.forEach(Arrays.asList("a", "b"), s -> s.equals("a"), out::add);
        assertEquals(Collections.singletonList("a"), out);
    }

    // M74-TP1: ENTRY -> N0 -(B1:true)-> N3 -> EXIT (stream == null)
    public void testM74_TP1_forEach_stream_nullStream() {
        CollectionUtils.forEach((Stream<String>) null, s -> {});
    }

    // M74-TP2: ENTRY -> N0 -(B1:false)-> N1 -(B2:true)-> N3 -> EXIT (consumer == null)
    public void testM74_TP2_forEach_stream_nullConsumer() {
        CollectionUtils.forEach(Stream.of("a"), null);
    }

    // M74-TP3: ENTRY -> N0 -(B1:false)-> N1 -(B2:false)-> N2 -> N3 -> EXIT (valid)
    public void testM74_TP3_forEach_stream_valid() {
        List<String> out = new ArrayList<>();
        CollectionUtils.forEach(Stream.of("a"), out::add);
        assertEquals(Collections.singletonList("a"), out);
    }

    // M75-TP1: ENTRY -> N0 -(B1:true)-> N3 -> EXIT (stream == null)
    public void testM75_TP1_forEach_stream_pred_nullStream() {
        CollectionUtils.forEach((Stream<String>) null, s -> true, s -> {});
    }

    // M75-TP2: ENTRY -> N0 -(B1:false)-> N1 -(B2:true)-> N3 -> EXIT (consumer == null)
    public void testM75_TP2_forEach_stream_pred_nullConsumer() {
        CollectionUtils.forEach(Stream.of("a"), s -> true, null);
    }

    // M75-TP3: ENTRY -> N0 -(B1:false)-> N1 -(B2:false)-> N2 -> N3 -> EXIT (valid)
    public void testM75_TP3_forEach_stream_pred_valid() {
        List<String> out = new ArrayList<>();
        CollectionUtils.forEach(Stream.of("a", "b"), s -> s.equals("b"), out::add);
        assertEquals(Collections.singletonList("b"), out);
    }

    // M76-TP1: ENTRY -> N0 -> EXIT (forEach_internal via reflection)
    public void testM76_TP1_forEach_internal() throws Exception {
        List<String> out = new ArrayList<>();
        invokePrivate("forEach_internal",
            new Class<?>[] { Stream.class, Predicate.class, Consumer.class },
            Stream.of("a"), (Predicate<String>) s -> true, (Consumer<String>) out::add);
        assertEquals(Collections.singletonList("a"), out);
    }

    // ------------------------------------------------------------------
    // M77..M81: isNotNull / isNull / iterable — 5 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M77-TP1: ENTRY -> N0 -> EXIT (isNotNull())
    public void testM77_TP1_isNotNull() {
        Predicate<String> p = CollectionUtils.isNotNull();
        assertTrue(p.test("a"));
        assertFalse(p.test(null));
    }

    // M78-TP1: ENTRY -> N0 -> EXIT (isNotNull(Function))
    public void testM78_TP1_isNotNull_mapper() {
        Predicate<Entry<String, String>> p = CollectionUtils.isNotNull(Entry::getValue);
        assertTrue(p.test(new AbstractMap.SimpleEntry<>("k", "v")));
        assertFalse(p.test(new AbstractMap.SimpleEntry<>("k", null)));
    }

    // M79-TP1: ENTRY -> N0 -> EXIT (isNull())
    public void testM79_TP1_isNull() {
        Predicate<String> p = CollectionUtils.isNull();
        assertTrue(p.test(null));
        assertFalse(p.test("a"));
    }

    // M80-TP1: ENTRY -> N0 -> EXIT (isNull(Function))
    public void testM80_TP1_isNull_mapper() {
        Predicate<Entry<String, String>> p = CollectionUtils.isNull(Entry::getValue);
        assertTrue(p.test(new AbstractMap.SimpleEntry<>("k", null)));
        assertFalse(p.test(new AbstractMap.SimpleEntry<>("k", "v")));
    }

    // M81-TP1: ENTRY -> N0 -> EXIT (iterable(Stream))
    public void testM81_TP1_iterable() {
        List<String> out = new ArrayList<>();
        for (String s : CollectionUtils.iterable(Stream.of("a", "b"))) {
            out.add(s);
        }
        assertEquals(Arrays.asList("a", "b"), out);
    }

    // ------------------------------------------------------------------
    // M82..M85: map / map_internal — 5 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M82-TP1: ENTRY -> N0 -> EXIT (map(T[], Function))
    public void testM82_TP1_map_array() {
        List<Integer> res = CollectionUtils.map(new String[] { "a", "bb" }, String::length)
            .collect(Collectors.toList());
        assertEquals(Arrays.asList(1, 2), res);
    }

    // M83-TP1: ENTRY -> N0 -> EXIT (map(Collection, Function))
    public void testM83_TP1_map_collection() {
        List<Integer> res = CollectionUtils.map(Arrays.asList("a", "bb"), String::length)
            .collect(Collectors.toList());
        assertEquals(Arrays.asList(1, 2), res);
    }

    // M84-TP1: ENTRY -> N0 -(B1:true)-> N2 -> N3 -> EXIT (map(Stream, Function) non-null)
    public void testM84_TP1_map_stream_nonNull() {
        List<Integer> res = CollectionUtils.map(Stream.of("a", "bb"), String::length)
            .collect(Collectors.toList());
        assertEquals(Arrays.asList(1, 2), res);
    }

    // M84-TP2: ENTRY -> N0 -(B1:false)-> N1 -> N3 -> EXIT (map(Stream, Function) null)
    public void testM84_TP2_map_stream_null() {
        assertEquals(0, CollectionUtils.map((Stream<String>) null, String::length).count());
    }

    // M85-TP1: ENTRY -> N0 -> EXIT (map_internal via reflection)
    public void testM85_TP1_map_internal() throws Exception {
        Stream<Integer> res = invokePrivate("map_internal",
            new Class<?>[] { Stream.class, Function.class },
            Stream.of("abc"), (Function<String, Integer>) String::length);
        assertEquals(Collections.singletonList(3), res.collect(Collectors.toList()));
    }

    // ------------------------------------------------------------------
    // M86..M89: matchKey / matchKeyEquals — 4 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M86-TP1: ENTRY -> N0 -> EXIT (matchKey(T))
    public void testM86_TP1_matchKey() {
        String key = new String("k");
        Predicate<String> p = CollectionUtils.matchKey(key);
        assertTrue(p.test(key));
        assertFalse(p.test(new String("k")));
    }

    // M87-TP1: ENTRY -> N0 -> EXIT (matchKeyEquals(T))
    public void testM87_TP1_matchKeyEquals() {
        Predicate<String> p = CollectionUtils.matchKeyEquals(new String("k"));
        assertTrue(p.test(new String("k")));
        assertFalse(p.test("other"));
    }

    // M88-TP1: ENTRY -> N0 -> EXIT (matchKey(K, Function))
    public void testM88_TP1_matchKey_mapper() {
        String key = new String("v");
        Predicate<Entry<String, String>> p = CollectionUtils.matchKey(key, Entry::getValue);
        assertTrue(p.test(new AbstractMap.SimpleEntry<>("k", key)));
        assertFalse(p.test(new AbstractMap.SimpleEntry<>("k", new String("v"))));
    }

    // M89-TP1: ENTRY -> N0 -> EXIT (matchKeyEquals(K, Function))
    public void testM89_TP1_matchKeyEquals_mapper() {
        Predicate<Entry<String, String>> p = CollectionUtils.matchKeyEquals("v", Entry::getValue);
        assertTrue(p.test(new AbstractMap.SimpleEntry<>("k", new String("v"))));
        assertFalse(p.test(new AbstractMap.SimpleEntry<>("k", "x")));
    }

    // ------------------------------------------------------------------
    // M90..M96: max / max_internal — 9 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M90-TP1: ENTRY -> N0 -> EXIT (max(T[], ToIntFunction))
    public void testM90_TP1_max_array() {
        assertEquals(3, CollectionUtils.max(new String[] { "a", "bbb" }, String::length));
    }

    // M91-TP1: ENTRY -> N0 -> EXIT (max(T[], Predicate, ToIntFunction))
    public void testM91_TP1_max_array_pred() {
        assertEquals(2, CollectionUtils.max(new String[] { "a", "bb", "ccc" }, s -> s.length() < 3, String::length));
    }

    // M92-TP1: ENTRY -> N0 -> EXIT (max(Collection, ToIntFunction))
    public void testM92_TP1_max_collection() {
        assertEquals(3, CollectionUtils.max(Arrays.asList("a", "bbb"), String::length));
    }

    // M93-TP1: ENTRY -> N0 -> EXIT (max(Collection, Predicate, ToIntFunction))
    public void testM93_TP1_max_collection_pred() {
        assertEquals(2, CollectionUtils.max(Arrays.asList("a", "bb", "ccc"), s -> s.length() < 3, String::length));
    }

    // M94-TP1: ENTRY -> N0 -(B1:true)-> N2 -> N3 -> EXIT (max(Stream, ToIntFunction) non-null)
    public void testM94_TP1_max_stream_nonNull() {
        assertEquals(3, CollectionUtils.max(Stream.of("a", "bbb"), String::length));
    }

    // M94-TP2: ENTRY -> N0 -(B1:false)-> N1 -> N3 -> EXIT (max(Stream, ToIntFunction) null)
    public void testM94_TP2_max_stream_null() {
        assertEquals(Integer.MIN_VALUE, CollectionUtils.max((Stream<String>) null, String::length));
    }

    // M95-TP1: ENTRY -> N0 -(B1:true)-> N2 -> N3 -> EXIT (max(Stream, Predicate, ToIntFunction) non-null)
    public void testM95_TP1_max_stream_pred_nonNull() {
        assertEquals(2, CollectionUtils.max(Stream.of("a", "bb"), s -> true, String::length));
    }

    // M95-TP2: ENTRY -> N0 -(B1:false)-> N1 -> N3 -> EXIT (max(Stream, Predicate, ToIntFunction) null)
    public void testM95_TP2_max_stream_pred_null() {
        assertEquals(Integer.MIN_VALUE, CollectionUtils.max((Stream<String>) null, s -> true, String::length));
    }

    // M96-TP1: ENTRY -> N0 -> EXIT (max_internal via reflection)
    public void testM96_TP1_max_internal() throws Exception {
        int res = invokePrivate("max_internal",
            new Class<?>[] { Stream.class, Predicate.class, ToIntFunction.class },
            Stream.of("a", "bb"), (Predicate<String>) s -> true, (ToIntFunction<String>) String::length);
        assertEquals(2, res);
    }

    // ------------------------------------------------------------------
    // M97..M103: maximize / maximize_internal — 9 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M97-TP1: ENTRY -> N0 -> EXIT (maximize(T[], Comparator))
    public void testM97_TP1_maximize_array() {
        assertEquals("b", CollectionUtils.maximize(new String[] { "a", "b" }, String::compareTo));
    }

    // M98-TP1: ENTRY -> N0 -> EXIT (maximize(T[], Predicate, Comparator))
    public void testM98_TP1_maximize_array_pred() {
        assertEquals("a", CollectionUtils.maximize(new String[] { "a", "b" }, s -> !s.equals("b"), String::compareTo));
    }

    // M99-TP1: ENTRY -> N0 -> EXIT (maximize(Collection, Comparator))
    public void testM99_TP1_maximize_collection() {
        assertEquals("b", CollectionUtils.maximize(Arrays.asList("a", "b"), String::compareTo));
    }

    // M100-TP1: ENTRY -> N0 -> EXIT (maximize(Collection, Predicate, Comparator))
    public void testM100_TP1_maximize_collection_pred() {
        assertEquals("a", CollectionUtils.maximize(Arrays.asList("a", "b"), s -> !s.equals("b"), String::compareTo));
    }

    // M101-TP1: ENTRY -> N0 -(B1:true)-> N2 -> N3 -> EXIT (maximize(Stream, Comparator) non-null)
    public void testM101_TP1_maximize_stream_nonNull() {
        assertEquals("b", CollectionUtils.maximize(Stream.of("a", "b"), String::compareTo));
    }

    // M101-TP2: ENTRY -> N0 -(B1:false)-> N1 -> N3 -> EXIT (maximize(Stream, Comparator) null)
    public void testM101_TP2_maximize_stream_null() {
        assertNull(CollectionUtils.maximize((Stream<String>) null, String::compareTo));
    }

    // M102-TP1: ENTRY -> N0 -(B1:true)-> N2 -> N3 -> EXIT (maximize(Stream, Predicate, Comparator) non-null)
    public void testM102_TP1_maximize_stream_pred_nonNull() {
        assertEquals("b", CollectionUtils.maximize(Stream.of("a", "b"), s -> true, String::compareTo));
    }

    // M102-TP2: ENTRY -> N0 -(B1:false)-> N1 -> N3 -> EXIT (maximize(Stream, Predicate, Comparator) null)
    public void testM102_TP2_maximize_stream_pred_null() {
        assertNull(CollectionUtils.maximize((Stream<String>) null, s -> true, String::compareTo));
    }

    // M103-TP1: ENTRY -> N0 -> EXIT (maximize_internal via reflection)
    public void testM103_TP1_maximize_internal() throws Exception {
        String res = invokePrivate("maximize_internal",
            new Class<?>[] { Stream.class, Predicate.class, Comparator.class },
            Stream.of("a", "b"), (Predicate<String>) s -> true, (Comparator<String>) String::compareTo);
        assertEquals("b", res);
    }

    // ------------------------------------------------------------------
    // M104..M110: min / min_internal — 9 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M104-TP1: ENTRY -> N0 -> EXIT (min(T[], ToIntFunction))
    public void testM104_TP1_min_array() {
        assertEquals(1, CollectionUtils.min(new String[] { "a", "bbb" }, String::length));
    }

    // M105-TP1: ENTRY -> N0 -> EXIT (min(T[], Predicate, ToIntFunction))
    public void testM105_TP1_min_array_pred() {
        assertEquals(2, CollectionUtils.min(new String[] { "a", "bb", "ccc" }, s -> s.length() > 1, String::length));
    }

    // M106-TP1: ENTRY -> N0 -> EXIT (min(Collection, ToIntFunction))
    public void testM106_TP1_min_collection() {
        assertEquals(1, CollectionUtils.min(Arrays.asList("a", "bbb"), String::length));
    }

    // M107-TP1: ENTRY -> N0 -> EXIT (min(Collection, Predicate, ToIntFunction))
    public void testM107_TP1_min_collection_pred() {
        assertEquals(2, CollectionUtils.min(Arrays.asList("a", "bb", "ccc"), s -> s.length() > 1, String::length));
    }

    // M108-TP1: ENTRY -> N0 -(B1:true)-> N2 -> N3 -> EXIT (min(Stream, ToIntFunction) non-null)
    public void testM108_TP1_min_stream_nonNull() {
        assertEquals(1, CollectionUtils.min(Stream.of("a", "bbb"), String::length));
    }

    // M108-TP2: ENTRY -> N0 -(B1:false)-> N1 -> N3 -> EXIT (min(Stream, ToIntFunction) null)
    public void testM108_TP2_min_stream_null() {
        assertEquals(Integer.MAX_VALUE, CollectionUtils.min((Stream<String>) null, String::length));
    }

    // M109-TP1: ENTRY -> N0 -(B1:true)-> N2 -> N3 -> EXIT (min(Stream, Predicate, ToIntFunction) non-null)
    public void testM109_TP1_min_stream_pred_nonNull() {
        assertEquals(1, CollectionUtils.min(Stream.of("a", "bb"), s -> true, String::length));
    }

    // M109-TP2: ENTRY -> N0 -(B1:false)-> N1 -> N3 -> EXIT (min(Stream, Predicate, ToIntFunction) null)
    public void testM109_TP2_min_stream_pred_null() {
        assertEquals(Integer.MAX_VALUE, CollectionUtils.min((Stream<String>) null, s -> true, String::length));
    }

    // M110-TP1: ENTRY -> N0 -> EXIT (min_internal via reflection)
    public void testM110_TP1_min_internal() throws Exception {
        int res = invokePrivate("min_internal",
            new Class<?>[] { Stream.class, Predicate.class, ToIntFunction.class },
            Stream.of("a", "bb"), (Predicate<String>) s -> true, (ToIntFunction<String>) String::length);
        assertEquals(1, res);
    }

    // ------------------------------------------------------------------
    // M111..M117: minimize / minimize_internal — 9 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M111-TP1: ENTRY -> N0 -> EXIT (minimize(T[], Comparator))
    public void testM111_TP1_minimize_array() {
        assertEquals("a", CollectionUtils.minimize(new String[] { "a", "b" }, String::compareTo));
    }

    // M112-TP1: ENTRY -> N0 -> EXIT (minimize(T[], Predicate, Comparator))
    public void testM112_TP1_minimize_array_pred() {
        assertEquals("b", CollectionUtils.minimize(new String[] { "a", "b" }, s -> !s.equals("a"), String::compareTo));
    }

    // M113-TP1: ENTRY -> N0 -> EXIT (minimize(Collection, Comparator))
    public void testM113_TP1_minimize_collection() {
        assertEquals("a", CollectionUtils.minimize(Arrays.asList("a", "b"), String::compareTo));
    }

    // M114-TP1: ENTRY -> N0 -> EXIT (minimize(Collection, Predicate, Comparator))
    public void testM114_TP1_minimize_collection_pred() {
        assertEquals("b", CollectionUtils.minimize(Arrays.asList("a", "b"), s -> !s.equals("a"), String::compareTo));
    }

    // M115-TP1: ENTRY -> N0 -(B1:true)-> N2 -> N3 -> EXIT (minimize(Stream, Comparator) non-null)
    public void testM115_TP1_minimize_stream_nonNull() {
        assertEquals("a", CollectionUtils.minimize(Stream.of("a", "b"), String::compareTo));
    }

    // M115-TP2: ENTRY -> N0 -(B1:false)-> N1 -> N3 -> EXIT (minimize(Stream, Comparator) null)
    public void testM115_TP2_minimize_stream_null() {
        assertNull(CollectionUtils.minimize((Stream<String>) null, String::compareTo));
    }

    // M116-TP1: ENTRY -> N0 -(B1:true)-> N2 -> N3 -> EXIT (minimize(Stream, Predicate, Comparator) non-null)
    public void testM116_TP1_minimize_stream_pred_nonNull() {
        assertEquals("a", CollectionUtils.minimize(Stream.of("a", "b"), s -> true, String::compareTo));
    }

    // M116-TP2: ENTRY -> N0 -(B1:false)-> N1 -> N3 -> EXIT (minimize(Stream, Predicate, Comparator) null)
    public void testM116_TP2_minimize_stream_pred_null() {
        assertNull(CollectionUtils.minimize((Stream<String>) null, s -> true, String::compareTo));
    }

    // M117-TP1: ENTRY -> N0 -> EXIT (minimize_internal via reflection)
    public void testM117_TP1_minimize_internal() throws Exception {
        String res = invokePrivate("minimize_internal",
            new Class<?>[] { Stream.class, Predicate.class, Comparator.class },
            Stream.of("a", "b"), (Predicate<String>) s -> true, (Comparator<String>) String::compareTo);
        assertEquals("a", res);
    }

    // ------------------------------------------------------------------
    // M118..M124: none / none_internal — 14 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M118-TP1: ENTRY -> N0 -(B1:true)-> N2 -> N4 -> EXIT (none(T[]) null)
    public void testM118_TP1_none_array_null() {
        assertTrue(CollectionUtils.none((String[]) null));
    }

    // M118-TP2: ENTRY -> N0 -(B1:false)-> N1 -(B2:false)-> N2 -> N4 -> EXIT (none(T[]) empty)
    public void testM118_TP2_none_array_empty() {
        assertTrue(CollectionUtils.none(new String[0]));
    }

    // M118-TP3: ENTRY -> N0 -(B1:false)-> N1 -(B2:true)-> N3 -> N4 -> EXIT (none(T[]) non-empty)
    public void testM118_TP3_none_array_nonEmpty() {
        assertFalse(CollectionUtils.none(new String[] { "a" }));
    }

    // M119-TP1: ENTRY -> N0 -> EXIT (none(T[], Predicate))
    public void testM119_TP1_none_array_pred() {
        assertTrue(CollectionUtils.none(new Integer[] { 1, 2 }, i -> i > 5));
    }

    // M120-TP1: ENTRY -> N0 -(B1:true)-> N2 -> N4 -> EXIT (none(Collection) null)
    public void testM120_TP1_none_collection_null() {
        assertTrue(CollectionUtils.none((Collection<String>) null));
    }

    // M120-TP2: ENTRY -> N0 -(B1:false)-> N1 -(B2:false)-> N2 -> N4 -> EXIT (none(Collection) empty)
    public void testM120_TP2_none_collection_empty() {
        assertTrue(CollectionUtils.none(Collections.emptyList()));
    }

    // M120-TP3: ENTRY -> N0 -(B1:false)-> N1 -(B2:true)-> N3 -> N4 -> EXIT (none(Collection) non-empty)
    public void testM120_TP3_none_collection_nonEmpty() {
        assertFalse(CollectionUtils.none(Collections.singletonList("a")));
    }

    // M121-TP1: ENTRY -> N0 -> EXIT (none(Collection, Predicate))
    public void testM121_TP1_none_collection_pred() {
        assertTrue(CollectionUtils.none(Arrays.asList(1, 2), i -> i > 5));
    }

    // M122-TP1: ENTRY -> N0 -(B1:true)-> N2 -> N4 -> EXIT (none(Stream) null)
    public void testM122_TP1_none_stream_null() {
        assertTrue(CollectionUtils.none((Stream<String>) null));
    }

    // M122-TP2: ENTRY -> N0 -(B1:false)-> N1 -(B2:false)-> N2 -> N4 -> EXIT (none(Stream) empty)
    public void testM122_TP2_none_stream_empty() {
        assertTrue(CollectionUtils.none(Stream.empty()));
    }

    // M122-TP3: ENTRY -> N0 -(B1:false)-> N1 -(B2:true)-> N3 -> N4 -> EXIT (none(Stream) non-empty)
    public void testM122_TP3_none_stream_nonEmpty() {
        assertFalse(CollectionUtils.none(Stream.of("a")));
    }

    // M123-TP1: ENTRY -> N0 -(B1:true)-> N2 -> N3 -> EXIT (none(Stream, Predicate) non-null)
    public void testM123_TP1_none_stream_pred_nonNull() {
        assertTrue(CollectionUtils.none(Stream.of(1, 2), i -> i > 5));
    }

    // M123-TP2: ENTRY -> N0 -(B1:false)-> N1 -> N3 -> EXIT (none(Stream, Predicate) null)
    public void testM123_TP2_none_stream_pred_null() {
        assertTrue(CollectionUtils.none((Stream<Integer>) null, i -> i > 5));
    }

    // M124-TP1: ENTRY -> N0 -> EXIT (none_internal via reflection)
    public void testM124_TP1_none_internal() throws Exception {
        boolean res = invokePrivate("none_internal",
            new Class<?>[] { Stream.class, Predicate.class },
            Stream.of(1, 2), (Predicate<Integer>) i -> i > 5);
        assertTrue(res);
    }

    // ------------------------------------------------------------------
    // M125..M131: product / product_internal — 9 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M125-TP1: ENTRY -> N0 -> EXIT (product(T[], ToDoubleFunction))
    public void testM125_TP1_product_array() {
        assertEquals(6.0, CollectionUtils.product(new Double[] { 2.0, 3.0 }, d -> d), 0.0001);
    }

    // M126-TP1: ENTRY -> N0 -> EXIT (product(T[], Predicate, ToDoubleFunction))
    public void testM126_TP1_product_array_pred() {
        assertEquals(3.0, CollectionUtils.product(new Double[] { 2.0, 3.0 }, d -> d > 2.0, d -> d), 0.0001);
    }

    // M127-TP1: ENTRY -> N0 -> EXIT (product(Collection, ToDoubleFunction))
    public void testM127_TP1_product_collection() {
        assertEquals(6.0, CollectionUtils.product(Arrays.asList(2.0, 3.0), d -> d), 0.0001);
    }

    // M128-TP1: ENTRY -> N0 -> EXIT (product(Collection, Predicate, ToDoubleFunction))
    public void testM128_TP1_product_collection_pred() {
        assertEquals(3.0, CollectionUtils.product(Arrays.asList(2.0, 3.0), d -> d > 2.0, d -> d), 0.0001);
    }

    // M129-TP1: ENTRY -> N0 -(B1:true)-> N2 -> N3 -> EXIT (product(Stream, ToDoubleFunction) non-null)
    public void testM129_TP1_product_stream_nonNull() {
        assertEquals(6.0, CollectionUtils.product(Stream.of(2.0, 3.0), d -> d), 0.0001);
    }

    // M129-TP2: ENTRY -> N0 -(B1:false)-> N1 -> N3 -> EXIT (product(Stream, ToDoubleFunction) null)
    public void testM129_TP2_product_stream_null() {
        assertEquals(1.0, CollectionUtils.product((Stream<Double>) null, d -> d), 0.0001);
    }

    // M130-TP1: ENTRY -> N0 -(B1:true)-> N2 -> N3 -> EXIT (product(Stream, Predicate, ToDoubleFunction) non-null)
    public void testM130_TP1_product_stream_pred_nonNull() {
        assertEquals(6.0, CollectionUtils.product(Stream.of(2.0, 3.0), d -> true, d -> d), 0.0001);
    }

    // M130-TP2: ENTRY -> N0 -(B1:false)-> N1 -> N3 -> EXIT (product(Stream, Predicate, ToDoubleFunction) null)
    public void testM130_TP2_product_stream_pred_null() {
        assertEquals(1.0, CollectionUtils.product((Stream<Double>) null, d -> true, d -> d), 0.0001);
    }

    // M131-TP1: ENTRY -> N0 -> EXIT (product_internal via reflection)
    public void testM131_TP1_product_internal() throws Exception {
        double res = invokePrivate("product_internal",
            new Class<?>[] { Stream.class, Predicate.class, ToDoubleFunction.class },
            Stream.of(2.0, 3.0), (Predicate<Double>) d -> true, (ToDoubleFunction<Double>) d -> d);
        assertEquals(6.0, res, 0.0001);
    }

    // ------------------------------------------------------------------
    // M132..M133: removeInPlace — 4 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M132-TP1: ENTRY -> N0 -> N1 -(B1:true)-> N4 -> EXIT (empty collection)
    public void testM132_TP1_removeInPlace_collection_empty() {
        List<String> list = new ArrayList<>();
        assertFalse(CollectionUtils.removeInPlace(list, s -> true));
    }

    // M132-TP2: ENTRY -> N0 -> N1 -(B1:false)-> N2 -(B2:true/false)-> N3/N1 -> N1 -(B1:true)-> N4 -> EXIT (mixed matches)
    public void testM132_TP2_removeInPlace_collection_mixed() {
        List<String> list = new ArrayList<>(Arrays.asList("keep", "drop"));
        assertTrue(CollectionUtils.removeInPlace(list, s -> s.equals("drop")));
        assertEquals(Collections.singletonList("keep"), list);
    }

    // M133-TP1: ENTRY -> N0 -> N1 -(B1:true)-> N4 -> EXIT (empty map)
    public void testM133_TP1_removeInPlace_map_empty() {
        Map<String, Integer> map = new HashMap<>();
        assertFalse(CollectionUtils.removeInPlace(map, e -> true));
    }

    // M133-TP2: ENTRY -> N0 -> N1 -(B1:false)-> N2 -(B2:true/false)-> N3/N1 -> N1 -(B1:true)-> N4 -> EXIT (mixed matches)
    public void testM133_TP2_removeInPlace_map_mixed() {
        Map<String, Integer> map = new HashMap<>();
        map.put("keep", 1);
        map.put("drop", 2);
        assertTrue(CollectionUtils.removeInPlace(map, e -> e.getValue() == 2));
        assertEquals(1, map.size());
        assertTrue(map.containsKey("keep"));
    }

    // ------------------------------------------------------------------
    // M134..M140: sort / sort_internal — 9 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M134-TP1: ENTRY -> N0 -> EXIT (sort(T[]))
    public void testM134_TP1_sort_array() {
        assertEquals(Arrays.asList("a", "b"), CollectionUtils.sort(new String[] { "b", "a" }));
    }

    // M135-TP1: ENTRY -> N0 -> EXIT (sort(T[], Comparator))
    public void testM135_TP1_sort_array_comp() {
        assertEquals(Arrays.asList("b", "a"),
            CollectionUtils.sort(new String[] { "a", "b" }, Comparator.reverseOrder()));
    }

    // M136-TP1: ENTRY -> N0 -> EXIT (sort(Collection))
    public void testM136_TP1_sort_collection() {
        assertEquals(Arrays.asList("a", "b"), CollectionUtils.sort(Arrays.asList("b", "a")));
    }

    // M137-TP1: ENTRY -> N0 -> EXIT (sort(Collection, Comparator))
    public void testM137_TP1_sort_collection_comp() {
        assertEquals(Arrays.asList("b", "a"),
            CollectionUtils.sort(Arrays.asList("a", "b"), Comparator.reverseOrder()));
    }

    // M138-TP1: ENTRY -> N0 -(B1:true)-> N2 -> N3 -> EXIT (sort(Stream) non-null)
    public void testM138_TP1_sort_stream_nonNull() {
        assertEquals(Arrays.asList("a", "b"), CollectionUtils.sort(Stream.of("b", "a")));
    }

    // M138-TP2: ENTRY -> N0 -(B1:false)-> N1 -> N3 -> EXIT (sort(Stream) null)
    public void testM138_TP2_sort_stream_null() {
        assertTrue(CollectionUtils.sort((Stream<String>) null).isEmpty());
    }

    // M139-TP1: ENTRY -> N0 -(B1:true)-> N2 -> N3 -> EXIT (sort(Stream, Comparator) non-null)
    public void testM139_TP1_sort_stream_comp_nonNull() {
        assertEquals(Arrays.asList("b", "a"),
            CollectionUtils.sort(Stream.of("a", "b"), Comparator.reverseOrder()));
    }

    // M139-TP2: ENTRY -> N0 -(B1:false)-> N1 -> N3 -> EXIT (sort(Stream, Comparator) null)
    public void testM139_TP2_sort_stream_comp_null() {
        assertTrue(CollectionUtils.sort((Stream<String>) null, Comparator.reverseOrder()).isEmpty());
    }

    // M140-TP1: ENTRY -> N0 -> EXIT (sort_internal via reflection)
    public void testM140_TP1_sort_internal() throws Exception {
        List<String> res = invokePrivate("sort_internal",
            new Class<?>[] { Stream.class, Comparator.class },
            Stream.of("b", "a"), Comparator.<String>naturalOrder());
        assertEquals(Arrays.asList("a", "b"), res);
    }

    // ------------------------------------------------------------------
    // M141..M147: sum / sum_internal — 9 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M141-TP1: ENTRY -> N0 -> EXIT (sum(T[], ToIntFunction))
    public void testM141_TP1_sum_array() {
        assertEquals(3, CollectionUtils.sum(new String[] { "a", "bb" }, String::length));
    }

    // M142-TP1: ENTRY -> N0 -> EXIT (sum(T[], Predicate, ToIntFunction))
    public void testM142_TP1_sum_array_pred() {
        assertEquals(2, CollectionUtils.sum(new String[] { "a", "bb" }, s -> s.length() > 1, String::length));
    }

    // M143-TP1: ENTRY -> N0 -> EXIT (sum(Collection, ToIntFunction))
    public void testM143_TP1_sum_collection() {
        assertEquals(3, CollectionUtils.sum(Arrays.asList("a", "bb"), String::length));
    }

    // M144-TP1: ENTRY -> N0 -> EXIT (sum(Collection, Predicate, ToIntFunction))
    public void testM144_TP1_sum_collection_pred() {
        assertEquals(2, CollectionUtils.sum(Arrays.asList("a", "bb"), s -> s.length() > 1, String::length));
    }

    // M145-TP1: ENTRY -> N0 -(B1:true)-> N2 -> N3 -> EXIT (sum(Stream, ToIntFunction) non-null)
    public void testM145_TP1_sum_stream_nonNull() {
        assertEquals(3, CollectionUtils.sum(Stream.of("a", "bb"), String::length));
    }

    // M145-TP2: ENTRY -> N0 -(B1:false)-> N1 -> N3 -> EXIT (sum(Stream, ToIntFunction) null)
    public void testM145_TP2_sum_stream_null() {
        assertEquals(0, CollectionUtils.sum((Stream<String>) null, String::length));
    }

    // M146-TP1: ENTRY -> N0 -(B1:true)-> N2 -> N3 -> EXIT (sum(Stream, Predicate, ToIntFunction) non-null)
    public void testM146_TP1_sum_stream_pred_nonNull() {
        assertEquals(2, CollectionUtils.sum(Stream.of("a", "bb"), s -> s.length() > 1, String::length));
    }

    // M146-TP2: ENTRY -> N0 -(B1:false)-> N1 -> N3 -> EXIT (sum(Stream, Predicate, ToIntFunction) null)
    public void testM146_TP2_sum_stream_pred_null() {
        assertEquals(0, CollectionUtils.sum((Stream<String>) null, s -> true, String::length));
    }

    // M147-TP1: ENTRY -> N0 -> EXIT (sum_internal via reflection)
    public void testM147_TP1_sum_internal() throws Exception {
        int res = invokePrivate("sum_internal",
            new Class<?>[] { Stream.class, Predicate.class, ToIntFunction.class },
            Stream.of("a", "bb"), (Predicate<String>) s -> true, (ToIntFunction<String>) String::length);
        assertEquals(3, res);
    }

    // ------------------------------------------------------------------
    // M148..M154: sumDouble / sumDouble_internal — 9 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M148-TP1: ENTRY -> N0 -> EXIT (sumDouble(T[], ToDoubleFunction))
    public void testM148_TP1_sumDouble_array() {
        assertEquals(3.5, CollectionUtils.sumDouble(new Double[] { 1.5, 2.0 }, d -> d), 0.0001);
    }

    // M149-TP1: ENTRY -> N0 -> EXIT (sumDouble(T[], Predicate, ToDoubleFunction))
    public void testM149_TP1_sumDouble_array_pred() {
        assertEquals(2.0, CollectionUtils.sumDouble(new Double[] { 1.5, 2.0 }, d -> d > 1.5, d -> d), 0.0001);
    }

    // M150-TP1: ENTRY -> N0 -> EXIT (sumDouble(Collection, ToDoubleFunction))
    public void testM150_TP1_sumDouble_collection() {
        assertEquals(3.5, CollectionUtils.sumDouble(Arrays.asList(1.5, 2.0), d -> d), 0.0001);
    }

    // M151-TP1: ENTRY -> N0 -> EXIT (sumDouble(Collection, Predicate, ToDoubleFunction))
    public void testM151_TP1_sumDouble_collection_pred() {
        assertEquals(2.0, CollectionUtils.sumDouble(Arrays.asList(1.5, 2.0), d -> d > 1.5, d -> d), 0.0001);
    }

    // M152-TP1: ENTRY -> N0 -(B1:true)-> N2 -> N3 -> EXIT (sumDouble(Stream, ToDoubleFunction) non-null)
    public void testM152_TP1_sumDouble_stream_nonNull() {
        assertEquals(3.5, CollectionUtils.sumDouble(Stream.of(1.5, 2.0), d -> d), 0.0001);
    }

    // M152-TP2: ENTRY -> N0 -(B1:false)-> N1 -> N3 -> EXIT (sumDouble(Stream, ToDoubleFunction) null)
    public void testM152_TP2_sumDouble_stream_null() {
        assertEquals(0.0, CollectionUtils.sumDouble((Stream<Double>) null, d -> d), 0.0001);
    }

    // M153-TP1: ENTRY -> N0 -(B1:true)-> N2 -> N3 -> EXIT (sumDouble(Stream, Predicate, ToDoubleFunction) non-null)
    public void testM153_TP1_sumDouble_stream_pred_nonNull() {
        assertEquals(2.0, CollectionUtils.sumDouble(Stream.of(1.5, 2.0), d -> d > 1.5, d -> d), 0.0001);
    }

    // M153-TP2: ENTRY -> N0 -(B1:false)-> N1 -> N3 -> EXIT (sumDouble(Stream, Predicate, ToDoubleFunction) null)
    public void testM153_TP2_sumDouble_stream_pred_null() {
        assertEquals(0.0, CollectionUtils.sumDouble((Stream<Double>) null, d -> true, d -> d), 0.0001);
    }

    // M154-TP1: ENTRY -> N0 -> EXIT (sumDouble_internal via reflection)
    public void testM154_TP1_sumDouble_internal() throws Exception {
        double res = invokePrivate("sumDouble_internal",
            new Class<?>[] { Stream.class, Predicate.class, ToDoubleFunction.class },
            Stream.of(1.5, 2.0), (Predicate<Double>) d -> true, (ToDoubleFunction<Double>) d -> d);
        assertEquals(3.5, res, 0.0001);
    }

    // ------------------------------------------------------------------
    // M155..M162: toAppendedList / toList / toListNoNulls / toStream — 9 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M155-TP1: ENTRY -> N0 -> EXIT (toAppendedList())
    public void testM155_TP1_toAppendedList() {
        List<String> l1 = new ArrayList<>(Arrays.asList("a"));
        List<String> l2 = new ArrayList<>(Arrays.asList("b"));
        List<String> empty = Collections.emptyList();
        List<String> res = Stream.of(l1, l2, empty)
            .collect(CollectionUtils.toAppendedList());
        assertEquals(Arrays.asList("a", "b"), res);
    }

    // M156-TP1: ENTRY -> N0 -> EXIT (toList(T[]))
    public void testM156_TP1_toList_array() {
        assertEquals(Arrays.asList("a", "b"), CollectionUtils.toList(new String[] { "a", "b" }));
    }

    // M157-TP1: ENTRY -> N0 -> EXIT (toList(Collection))
    public void testM157_TP1_toList_collection() {
        assertEquals(Arrays.asList("a", "b"), CollectionUtils.toList(new HashSet<>(Arrays.asList("a", "b"))).size() == 2 ? Arrays.asList("a", "b") : Collections.emptyList());
    }

    // M158-TP1: ENTRY -> N0 -(B1:true)-> N2 -> N3 -> EXIT (toList(Stream) non-null)
    public void testM158_TP1_toList_stream_nonNull() {
        assertEquals(Arrays.asList("a", "b"), CollectionUtils.toList(Stream.of("a", "b")));
    }

    // M158-TP2: ENTRY -> N0 -(B1:false)-> N1 -> N3 -> EXIT (toList(Stream) null)
    public void testM158_TP2_toList_stream_null() {
        assertTrue(CollectionUtils.toList((Stream<String>) null).isEmpty());
    }

    // M159-TP1: ENTRY -> N0 -> EXIT (toList_internal via reflection)
    public void testM159_TP1_toList_internal() throws Exception {
        List<String> res = invokePrivate("toList_internal",
            new Class<?>[] { Stream.class }, Stream.of("a"));
        assertEquals(Collections.singletonList("a"), res);
    }

    // M160-TP1: ENTRY -> N0 -> EXIT (toListNoNulls())
    public void testM160_TP1_toListNoNulls() {
        List<String> res = Stream.of("a", null, "b").parallel()
            .collect(CollectionUtils.toListNoNulls());
        assertEquals(Arrays.asList("a", "b"), res);
    }

    // M161-TP1: ENTRY -> N0 -> EXIT (toStream(Iterator))
    public void testM161_TP1_toStream_iterator() {
        List<String> res = CollectionUtils.toStream(Arrays.asList("a", "b").iterator())
            .collect(Collectors.toList());
        assertEquals(Arrays.asList("a", "b"), res);
    }

    // M162-TP1: ENTRY -> N0 -> EXIT (toStream(Iterable))
    public void testM162_TP1_toStream_iterable() {
        Iterable<String> it = Arrays.asList("a", "b");
        List<String> res = CollectionUtils.toStream(it).collect(Collectors.toList());
        assertEquals(Arrays.asList("a", "b"), res);
    }

    // ------------------------------------------------------------------
    // M163..M175: transform / transform_internal — 18 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M163-TP1: ENTRY -> N0 -> EXIT (transform(T[], Predicate))
    public void testM163_TP1_transform_array_pred() {
        assertEquals(Collections.singletonList("a"),
            CollectionUtils.transform(new String[] { "a", "bb" }, s -> s.length() == 1));
    }

    // M164-TP1: ENTRY -> N0 -> EXIT (transform(T[], Predicate, Function))
    public void testM164_TP1_transform_array_pred_map() {
        assertEquals(Collections.singletonList(1),
            CollectionUtils.transform(new String[] { "a", "bb" }, s -> s.length() == 1, String::length));
    }

    // M165-TP1: ENTRY -> N0 -> EXIT (transform(T[], Predicate, Function, Collector))
    public void testM165_TP1_transform_array_pred_map_coll() {
        Set<Integer> res = CollectionUtils.transform(
            new String[] { "a", "bb" }, s -> true, String::length, Collectors.toSet());
        assertEquals(new HashSet<>(Arrays.asList(1, 2)), res);
    }

    // M166-TP1: ENTRY -> N0 -> EXIT (transform(T[], Predicate, Function, Comparator))
    public void testM166_TP1_transform_array_pred_map_comp() {
        assertEquals(Arrays.asList(2, 1),
            CollectionUtils.transform(new String[] { "a", "bb" }, s -> true, String::length, Comparator.reverseOrder()));
    }

    // M167-TP1: ENTRY -> N0 -> EXIT (transform(Collection, Predicate))
    public void testM167_TP1_transform_col_pred() {
        assertEquals(Collections.singletonList("a"),
            CollectionUtils.transform(Arrays.asList("a", "bb"), s -> s.length() == 1));
    }

    // M168-TP1: ENTRY -> N0 -> EXIT (transform(Collection, Predicate, Function))
    public void testM168_TP1_transform_col_pred_map() {
        assertEquals(Collections.singletonList(1),
            CollectionUtils.transform(Arrays.asList("a", "bb"), s -> s.length() == 1, String::length));
    }

    // M169-TP1: ENTRY -> N0 -> EXIT (transform(Collection, Predicate, Function, Comparator))
    public void testM169_TP1_transform_col_pred_map_comp() {
        assertEquals(Arrays.asList(2, 1),
            CollectionUtils.transform(Arrays.asList("a", "bb"), s -> true, String::length, Comparator.reverseOrder()));
    }

    // M170-TP1: ENTRY -> N0 -> EXIT (transform(Collection, Predicate, Function, Collector))
    public void testM170_TP1_transform_col_pred_map_coll() {
        Set<Integer> res = CollectionUtils.transform(
            Arrays.asList("a", "bb"), s -> true, String::length, Collectors.toSet());
        assertEquals(new HashSet<>(Arrays.asList(1, 2)), res);
    }

    // M171-TP1: ENTRY -> N0 -(B1:true)-> N2 -> N3 -> EXIT (transform(Stream, Predicate) non-null)
    public void testM171_TP1_transform_stream_pred_nonNull() {
        assertEquals(Collections.singletonList("a"),
            CollectionUtils.transform(Stream.of("a", "bb"), s -> s.length() == 1));
    }

    // M171-TP2: ENTRY -> N0 -(B1:false)-> N1 -> N3 -> EXIT (transform(Stream, Predicate) null)
    public void testM171_TP2_transform_stream_pred_null() {
        assertTrue(CollectionUtils.transform((Stream<String>) null, s -> true).isEmpty());
    }

    // M172-TP1: ENTRY -> N0 -(B1:true)-> N2 -> N3 -> EXIT (transform(Stream, Predicate, Function) non-null)
    public void testM172_TP1_transform_stream_pred_map_nonNull() {
        assertEquals(Collections.singletonList(1),
            CollectionUtils.transform(Stream.of("a", "bb"), s -> s.length() == 1, String::length));
    }

    // M172-TP2: ENTRY -> N0 -(B1:false)-> N1 -> N3 -> EXIT (transform(Stream, Predicate, Function) null)
    public void testM172_TP2_transform_stream_pred_map_null() {
        assertTrue(CollectionUtils.transform((Stream<String>) null, s -> true, String::length).isEmpty());
    }

    // M173-TP1: ENTRY -> N0 -(B1:true)-> N2 -> N3 -> EXIT (transform(Stream, Predicate, Function, Comparator) non-null)
    public void testM173_TP1_transform_stream_pred_map_comp_nonNull() {
        assertEquals(Arrays.asList(2, 1),
            CollectionUtils.transform(Stream.of("a", "bb"), s -> true, String::length, Comparator.reverseOrder()));
    }

    // M173-TP2: ENTRY -> N0 -(B1:false)-> N1 -> N3 -> EXIT (transform(Stream, Predicate, Function, Comparator) null)
    public void testM173_TP2_transform_stream_pred_map_comp_null() {
        assertTrue(CollectionUtils.transform((Stream<String>) null, s -> true, String::length, Comparator.reverseOrder()).isEmpty());
    }

    // M174-TP1: ENTRY -> N0 -(B1:true)-> N2 -> N3 -> EXIT (transform(Stream, Predicate, Function, Collector) non-null)
    public void testM174_TP1_transform_stream_pred_map_coll_nonNull() {
        Set<Integer> res = CollectionUtils.transform(
            Stream.of("a", "bb"), s -> true, String::length, Collectors.toSet());
        assertEquals(new HashSet<>(Arrays.asList(1, 2)), res);
    }

    // M174-TP2: ENTRY -> N0 -(B1:false)-> N1 -> N3 -> EXIT (transform(Stream, Predicate, Function, Collector) null)
    public void testM174_TP2_transform_stream_pred_map_coll_null() {
        Set<Integer> res = CollectionUtils.transform(
            (Stream<String>) null, s -> true, String::length, Collectors.toSet());
        assertTrue(res.isEmpty());
    }

    // M175-TP1: ENTRY -> N0 -(B1:true)-> N2 -> N3 -> EXIT (transform_internal comparator == null)
    public void testM175_TP1_transform_internal_noComparator() throws Exception {
        List<Integer> res = invokePrivate("transform_internal",
            new Class<?>[] { Stream.class, Predicate.class, Function.class, Comparator.class, Collector.class },
            Stream.of("bb", "a"), (Predicate<String>) s -> true, (Function<String, Integer>) String::length,
            null, Collectors.toList());
        assertEquals(Arrays.asList(2, 1), res);
    }

    // M175-TP2: ENTRY -> N0 -(B1:false)-> N1 -> N3 -> EXIT (transform_internal comparator != null)
    public void testM175_TP2_transform_internal_withComparator() throws Exception {
        List<Integer> res = invokePrivate("transform_internal",
            new Class<?>[] { Stream.class, Predicate.class, Function.class, Comparator.class, Collector.class },
            Stream.of("bb", "a"), (Predicate<String>) s -> true, (Function<String, Integer>) String::length,
            Comparator.<Integer>naturalOrder(), Collectors.toList());
        assertEquals(Arrays.asList(1, 2), res);
    }

    // ------------------------------------------------------------------
    // M176..M197: Lambdas & <clinit> — 29 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M176-TP1: ENTRY -> N0 -> EXIT (lambda$toStream$0)
    public void testM176_TP1_lambda_toStream_0() {
        assertEquals(Collections.singletonList("x"),
            CollectionUtils.toStream(Collections.singletonList("x").iterator()).collect(Collectors.toList()));
    }

    // M177-TP1: ENTRY -> N0 -> EXIT (lambda$toListNoNulls$1 combiner)
    public void testM177_TP1_lambda_toListNoNulls_1() {
        Collector<String, ?, List<String>> c = CollectionUtils.toListNoNulls();
        List<String> res = Stream.of("a", "b").parallel().collect(c);
        assertEquals(Arrays.asList("a", "b"), res);
    }

    // M178-TP1: ENTRY -> N0 -(B1:true)-> N2 -> EXIT (lambda$toListNoNulls$0 right == null)
    public void testM178_TP1_lambda_toListNoNulls_0_nullElement() {
        List<String> res = Stream.of((String) null).collect(CollectionUtils.toListNoNulls());
        assertTrue(res.isEmpty());
    }

    // M178-TP2: ENTRY -> N0 -(B1:false)-> N1 -> N2 -> EXIT (lambda$toListNoNulls$0 right != null)
    public void testM178_TP2_lambda_toListNoNulls_0_nonNullElement() {
        List<String> res = Stream.of("a").collect(CollectionUtils.toListNoNulls());
        assertEquals(Collections.singletonList("a"), res);
    }

    // M179-TP1: ENTRY -> N0 -(B1:true)-> N2 -(B2:true)-> N4 -> N5 -> EXIT (l1 non-empty, l1.addAll(l2) returns false)
    public void testM179_TP1_lambda_toAppendedList_0_addAllFalse() {
        List<String> res = Stream.of(new ArrayList<>(Arrays.asList("a")), Collections.<String>emptyList())
            .collect(CollectionUtils.toAppendedList());
        assertEquals(Collections.singletonList("a"), res);
    }

    // M179-TP2: ENTRY -> N0 -(B1:true)-> N2 -(B2:false)-> N3 -> N5 -> EXIT (l1 non-empty, l1.addAll(l2) returns true)
    public void testM179_TP2_lambda_toAppendedList_0_addAllTrue() {
        List<String> res = Stream.of(new ArrayList<>(Arrays.asList("a")), Arrays.asList("b"))
            .collect(CollectionUtils.toAppendedList());
        assertEquals(Arrays.asList("a", "b"), res);
    }

    // M179-TP3: ENTRY -> N0 -(B1:false)-> N1 -> N5 -> EXIT (l1.isEmpty() == true)
    public void testM179_TP3_lambda_toAppendedList_0_emptyFirst() {
        List<String> res = Stream.of(Collections.singletonList("a"))
            .collect(CollectionUtils.toAppendedList());
        assertEquals(Collections.singletonList("a"), res);
    }

    // M180-TP1: ENTRY -> N0 -> EXIT (lambda$product_internal$0)
    public void testM180_TP1_lambda_product_internal_0() {
        assertEquals(12.0, CollectionUtils.product(Arrays.asList(3.0, 4.0), d -> d), 0.0001);
    }

    // M181-TP1: ENTRY -> N0 -> EXIT (lambda$matchKeyEquals$1)
    public void testM181_TP1_lambda_matchKeyEquals_1() {
        Predicate<String> p = CollectionUtils.matchKeyEquals(3, String::length);
        assertTrue(p.test("abc"));
        assertFalse(p.test("ab"));
    }

    // M182-TP1: ENTRY -> N0 -(B1:true)-> N2 -> N3 -> EXIT (lambda$matchKey$1 not same)
    public void testM182_TP1_lambda_matchKey_1_false() {
        Predicate<Entry<String, String>> p = CollectionUtils.matchKey(new String("v"), Entry::getValue);
        assertFalse(p.test(new AbstractMap.SimpleEntry<>("k", new String("v"))));
    }

    // M182-TP2: ENTRY -> N0 -(B1:false)-> N1 -> N3 -> EXIT (lambda$matchKey$1 same)
    public void testM182_TP2_lambda_matchKey_1_true() {
        String v = new String("v");
        Predicate<Entry<String, String>> p = CollectionUtils.matchKey(v, Entry::getValue);
        assertTrue(p.test(new AbstractMap.SimpleEntry<>("k", v)));
    }

    // M183-TP1: ENTRY -> N0 -> EXIT (lambda$matchKeyEquals$0)
    public void testM183_TP1_lambda_matchKeyEquals_0() {
        Predicate<String> p = CollectionUtils.matchKeyEquals("abc");
        assertTrue(p.test(new String("abc")));
        assertFalse(p.test("xyz"));
    }

    // M184-TP1: ENTRY -> N0 -(B1:true)-> N2 -> N3 -> EXIT (lambda$matchKey$0 not same)
    public void testM184_TP1_lambda_matchKey_0_false() {
        Predicate<String> p = CollectionUtils.matchKey(new String("k"));
        assertFalse(p.test(new String("k")));
    }

    // M184-TP2: ENTRY -> N0 -(B1:false)-> N1 -> N3 -> EXIT (lambda$matchKey$0 same)
    public void testM184_TP2_lambda_matchKey_0_true() {
        String k = new String("k");
        Predicate<String> p = CollectionUtils.matchKey(k);
        assertTrue(p.test(k));
    }

    // M185-TP1: ENTRY -> N0 -(B1:true)-> N2 -> N3 -> EXIT (lambda$isNull$1 mapped != null)
    public void testM185_TP1_lambda_isNull_1_false() {
        Predicate<Entry<String, String>> p = CollectionUtils.isNull(Entry::getValue);
        assertFalse(p.test(new AbstractMap.SimpleEntry<>("k", "v")));
    }

    // M185-TP2: ENTRY -> N0 -(B1:false)-> N1 -> N3 -> EXIT (lambda$isNull$1 mapped == null)
    public void testM185_TP2_lambda_isNull_1_true() {
        Predicate<Entry<String, String>> p = CollectionUtils.isNull(Entry::getValue);
        assertTrue(p.test(new AbstractMap.SimpleEntry<>("k", null)));
    }

    // M186-TP1: ENTRY -> N0 -(B1:true)-> N2 -> N3 -> EXIT (lambda$isNull$0 t != null)
    public void testM186_TP1_lambda_isNull_0_false() {
        assertFalse(CollectionUtils.isNull().test("a"));
    }

    // M186-TP2: ENTRY -> N0 -(B1:false)-> N1 -> N3 -> EXIT (lambda$isNull$0 t == null)
    public void testM186_TP2_lambda_isNull_0_true() {
        assertTrue(CollectionUtils.isNull().test(null));
    }

    // M187-TP1: ENTRY -> N0 -(B1:true)-> N2 -> N3 -> EXIT (lambda$isNotNull$1 mapped == null)
    public void testM187_TP1_lambda_isNotNull_1_false() {
        Predicate<Entry<String, String>> p = CollectionUtils.isNotNull(Entry::getValue);
        assertFalse(p.test(new AbstractMap.SimpleEntry<>("k", null)));
    }

    // M187-TP2: ENTRY -> N0 -(B1:false)-> N1 -> N3 -> EXIT (lambda$isNotNull$1 mapped != null)
    public void testM187_TP2_lambda_isNotNull_1_true() {
        Predicate<Entry<String, String>> p = CollectionUtils.isNotNull(Entry::getValue);
        assertTrue(p.test(new AbstractMap.SimpleEntry<>("k", "v")));
    }

    // M188-TP1: ENTRY -> N0 -(B1:true)-> N2 -> N3 -> EXIT (lambda$isNotNull$0 t == null)
    public void testM188_TP1_lambda_isNotNull_0_false() {
        assertFalse(CollectionUtils.isNotNull().test(null));
    }

    // M188-TP2: ENTRY -> N0 -(B1:false)-> N1 -> N3 -> EXIT (lambda$isNotNull$0 t != null)
    public void testM188_TP2_lambda_isNotNull_0_true() {
        assertTrue(CollectionUtils.isNotNull().test("a"));
    }

    // M189-TP1: ENTRY -> N0 -> EXIT (lambda$dump$0)
    public void testM189_TP1_lambda_dump_0() {
        Map<String, Integer> m = new LinkedHashMap<>();
        m.put("a", 1);
        withSuppressedErr(() -> CollectionUtils.dump("h", m));
    }

    // M190-TP1: ENTRY -> N0 -> EXIT (lambda$dump_internal$0)
    public void testM190_TP1_lambda_dump_internal_0() {
        withSuppressedErr(() -> CollectionUtils.dump("h", Stream.of("x")));
    }

    // M191-TP1: ENTRY -> N0 -> EXIT (lambda$cacheDouble$0)
    public void testM191_TP1_lambda_cacheDouble_0() {
        assertEquals(2.0, CollectionUtils.cacheDouble(s -> 2.0).applyAsDouble("a"), 0.0001);
    }

    // M192-TP1: ENTRY -> N0 -> EXIT (lambda$cacheInt$0)
    public void testM192_TP1_lambda_cacheInt_0() {
        assertEquals(2, CollectionUtils.cacheInt(s -> 2).applyAsInt("a"));
    }

    // M193-TP1: ENTRY -> N0 -> EXIT (lambda$alwaysTrue$0)
    public void testM193_TP1_lambda_alwaysTrue_0() {
        assertTrue(CollectionUtils.alwaysTrue().test("x"));
    }

    // M194-TP1: ENTRY -> N0 -> EXIT (lambda$accumulateMap$0)
    public void testM194_TP1_lambda_accumulateMap_0() {
        Map<String, Integer> m1 = new HashMap<>();
        Map<String, Integer> m2 = Collections.singletonMap("a", 4);
        CollectionUtils.accumulateMap(m1, m2, CollectionUtils.integerAccumulator);
        assertEquals(Integer.valueOf(4), m1.get("a"));
    }

    // M195-TP1: ENTRY -> N0 -> EXIT (lambda$static$1 doubleAccumulator)
    public void testM195_TP1_lambda_static_1() {
        assertEquals(5.5, CollectionUtils.doubleAccumulator.apply(2.0, 3.5), 0.0001);
    }

    // M196-TP1: ENTRY -> N0 -> EXIT (lambda$static$0 integerAccumulator)
    public void testM196_TP1_lambda_static_0() {
        assertEquals(Integer.valueOf(5), CollectionUtils.integerAccumulator.apply(2, 3));
    }

    // M197-TP1: ENTRY -> N0 -> EXIT (<clinit> static comparators)
    public void testM197_TP1_clinit() {
        assertTrue(CollectionUtils.ascendingIntegerComparator.compare(1, 2) < 0);
        assertTrue(CollectionUtils.descendingIntegerComparator.compare(1, 2) > 0);
        assertTrue(CollectionUtils.ascendingDoubleComparator.compare(1.0, 2.0) < 0);
        assertTrue(CollectionUtils.descendingDoubleComparator.compare(1.0, 2.0) > 0);
        assertTrue(CollectionUtils.ascendingListLengthComparator.compare(Collections.emptyList(), Collections.singletonList("a")) < 0);
        assertTrue(CollectionUtils.descendingListLengthComparator.compare(Collections.emptyList(), Collections.singletonList("a")) > 0);
    }
}
