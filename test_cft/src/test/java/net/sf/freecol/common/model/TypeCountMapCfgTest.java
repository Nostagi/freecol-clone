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

import java.util.Collection;
import java.util.Map;
import java.util.Set;

import net.sf.freecol.util.test.FreeColTestCase;


/**
 * CFG-driven test suite for {@link TypeCountMap} derived from
 * {@code test_cft/graph/net/sf/freecol/common/model/TypeCountMap.json}.
 *
 * Covers all 15 methods (M1..M15, including {@code lambda$add$0} and
 * {@code lambda$toString$0}) and achieves 100% C3 (node + edge) coverage across
 * 18 Test Paths.
 */
public class TypeCountMapCfgTest extends FreeColTestCase {

    private GoodsType grainType;
    private GoodsType lumberType;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        // Pre-condition: Load standard GoodsType instances from Specification
        grainType = spec().getGoodsType("model.goods.grain");
        lumberType = spec().getGoodsType("model.goods.lumber");
    }

    // ------------------------------------------------------------------
    // Pre-condition helpers & stubs
    // ------------------------------------------------------------------

    private TypeCountMap<GoodsType> createSampleMap() {
        TypeCountMap<GoodsType> map = new TypeCountMap<>();
        map.incrementCount(grainType, 5);
        map.incrementCount(lumberType, 3);
        return map;
    }

    // ------------------------------------------------------------------
    // M1: <init>()V — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M1-TP1: ENTRY -> N0 -> EXIT (construct empty TypeCountMap)
    public void testM1_TP1_Constructor() {
        TypeCountMap<GoodsType> map = new TypeCountMap<>();
        assertTrue(map.isEmpty());
        assertEquals(0, map.size());
    }

    // ------------------------------------------------------------------
    // M2: getValues()Map — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M2-TP1: ENTRY -> N0 -> EXIT (returns backing map)
    public void testM2_TP1_GetValues() {
        TypeCountMap<GoodsType> map = createSampleMap();
        Map<GoodsType, Integer> values = map.getValues();
        assertEquals(2, values.size());
        assertEquals(Integer.valueOf(5), values.get(grainType));
    }

    // ------------------------------------------------------------------
    // M3: getCount(FreeColSpecObjectType)I — 2 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M3-TP1: ENTRY -> N0 -(B1:false)-> N1 -> N3 -> EXIT (value == null -> 0)
    public void testM3_TP1_GetCountMissingKeyReturnsZero() {
        TypeCountMap<GoodsType> map = new TypeCountMap<>();
        assertEquals(0, map.getCount(grainType));
    }

    // M3-TP2: ENTRY -> N0 -(B1:true)-> N2 -> N3 -> EXIT (value != null -> value)
    public void testM3_TP2_GetCountPresentKeyReturnsValue() {
        TypeCountMap<GoodsType> map = createSampleMap();
        assertEquals(5, map.getCount(grainType));
    }

    // ------------------------------------------------------------------
    // M4: incrementCount(FreeColSpecObjectType, int)Integer — 3 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M4-TP1: ENTRY -> N0 -(B1:false)-> N1 -> EXIT (oldValue == null -> put newCount)
    public void testM4_TP1_IncrementCountNewKey() {
        TypeCountMap<GoodsType> map = new TypeCountMap<>();
        assertNull(map.incrementCount(grainType, 7));
        assertEquals(7, map.getCount(grainType));
    }

    // M4-TP2: ENTRY -> N0 -(B1:true)-> N2 -(B2:false)-> N3 -> EXIT (oldValue == -newCount -> remove & return null)
    public void testM4_TP2_IncrementCountCancelsToZeroRemovesKey() {
        TypeCountMap<GoodsType> map = new TypeCountMap<>();
        map.incrementCount(grainType, 4);
        assertNull(map.incrementCount(grainType, -4));
        assertFalse(map.containsKey(grainType));
        assertEquals(0, map.getCount(grainType));
    }

    // M4-TP3: ENTRY -> N0 -(B1:true)-> N2 -(B2:true)-> N4 -> EXIT (oldValue != -newCount -> put oldValue + newCount)
    public void testM4_TP3_IncrementCountUpdatesExistingKey() {
        TypeCountMap<GoodsType> map = new TypeCountMap<>();
        map.incrementCount(grainType, 4);
        assertEquals(Integer.valueOf(4), map.incrementCount(grainType, 6));
        assertEquals(10, map.getCount(grainType));
    }

    // ------------------------------------------------------------------
    // M5: add(TypeCountMap)V — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M5-TP1: ENTRY -> N0 -> EXIT (merges counts from other via forEachMapEntry)
    public void testM5_TP1_Add() {
        TypeCountMap<GoodsType> target = new TypeCountMap<>();
        target.incrementCount(grainType, 2);
        TypeCountMap<GoodsType> other = createSampleMap();
        target.add(other);
        assertEquals(7, target.getCount(grainType));
        assertEquals(3, target.getCount(lumberType));
    }

    // ------------------------------------------------------------------
    // M6: clear()V — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M6-TP1: ENTRY -> N0 -> EXIT (clears all entries)
    public void testM6_TP1_Clear() {
        TypeCountMap<GoodsType> map = createSampleMap();
        map.clear();
        assertTrue(map.isEmpty());
    }

    // ------------------------------------------------------------------
    // M7: keySet()Set — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M7-TP1: ENTRY -> N0 -> EXIT (returns key set)
    public void testM7_TP1_KeySet() {
        TypeCountMap<GoodsType> map = createSampleMap();
        Set<GoodsType> keys = map.keySet();
        assertEquals(2, keys.size());
        assertTrue(keys.contains(grainType));
        assertTrue(keys.contains(lumberType));
    }

    // ------------------------------------------------------------------
    // M8: values()Collection — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M8-TP1: ENTRY -> N0 -> EXIT (returns values collection)
    public void testM8_TP1_Values() {
        TypeCountMap<GoodsType> map = createSampleMap();
        Collection<Integer> vals = map.values();
        assertEquals(2, vals.size());
        assertTrue(vals.contains(5));
        assertTrue(vals.contains(3));
    }

    // ------------------------------------------------------------------
    // M9: containsKey(FreeColSpecObjectType)Z — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M9-TP1: ENTRY -> N0 -> EXIT (delegates to values.containsKey)
    public void testM9_TP1_ContainsKey() {
        TypeCountMap<GoodsType> map = new TypeCountMap<>();
        assertFalse(map.containsKey(grainType));
        map.incrementCount(grainType, 1);
        assertTrue(map.containsKey(grainType));
    }

    // ------------------------------------------------------------------
    // M10: isEmpty()Z — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M10-TP1: ENTRY -> N0 -> EXIT (delegates to values.isEmpty)
    public void testM10_TP1_IsEmpty() {
        TypeCountMap<GoodsType> map = new TypeCountMap<>();
        assertTrue(map.isEmpty());
        map.incrementCount(grainType, 1);
        assertFalse(map.isEmpty());
    }

    // ------------------------------------------------------------------
    // M11: size()I — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M11-TP1: ENTRY -> N0 -> EXIT (delegates to values.size)
    public void testM11_TP1_Size() {
        TypeCountMap<GoodsType> map = createSampleMap();
        assertEquals(2, map.size());
    }

    // ------------------------------------------------------------------
    // M12: putAll(TypeCountMap)V — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M12-TP1: ENTRY -> N0 -> EXIT (delegates to values.putAll)
    public void testM12_TP1_PutAll() {
        TypeCountMap<GoodsType> target = new TypeCountMap<>();
        target.incrementCount(grainType, 99);
        TypeCountMap<GoodsType> other = createSampleMap();
        target.putAll(other);
        assertEquals(5, target.getCount(grainType));
        assertEquals(3, target.getCount(lumberType));
    }

    // ------------------------------------------------------------------
    // M13: toString()String — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M13-TP1: ENTRY -> N0 -> EXIT (formats "[<className> [<index>,<count>]...]")
    public void testM13_TP1_ToString() {
        TypeCountMap<GoodsType> map = new TypeCountMap<>();
        map.incrementCount(grainType, 4);
        String str = map.toString();
        assertTrue(str.startsWith("[" + TypeCountMap.class.getName()));
        assertTrue(str.contains("[" + grainType.getIndex() + ",4]"));
    }

    // ------------------------------------------------------------------
    // M14: lambda$toString$0(StringBuilder, Map.Entry)V — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M14-TP1: ENTRY -> N0 -> EXIT (invoked by forEachMapEntry inside toString)
    public void testM14_TP1_LambdaToString() {
        TypeCountMap<GoodsType> map = createSampleMap();
        String str = map.toString();
        assertTrue(str.contains("[" + grainType.getIndex() + ",5]"));
        assertTrue(str.contains("[" + lumberType.getIndex() + ",3]"));
    }

    // ------------------------------------------------------------------
    // M15: lambda$add$0(Map.Entry)V — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M15-TP1: ENTRY -> N0 -> EXIT (invoked by forEachMapEntry inside add)
    public void testM15_TP1_LambdaAdd() {
        TypeCountMap<GoodsType> target = new TypeCountMap<>();
        TypeCountMap<GoodsType> other = createSampleMap();
        target.add(other);
        assertEquals(5, target.getCount(grainType));
        assertEquals(3, target.getCount(lumberType));
    }
}
