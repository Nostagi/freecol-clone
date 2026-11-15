package net.sf.freecol.common.model;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import net.sf.freecol.common.model.ProductionMap.ProductionTree;
import net.sf.freecol.util.test.FreeColTestCase;

/**
 * CFG-driven test suite for {@link ProductionMap} derived from
 * {@code test_cft/graph/net/sf/freecol/common/model/ProductionMap.json}.
 *
 * Covers all 8 methods (M1..M8) and achieves 100% feasible C3 (node + edge)
 * coverage across 18 Test Paths.
 *
 * Infeasible edges documented:
 * - None (all edges in M1..M8 are feasible).
 */
public class ProductionMapCfgTest extends FreeColTestCase {

    private GoodsType foodType;
    private GoodsType grainType;
    private GoodsType fishType;
    private GoodsType lumberType;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        // Pre-condition: load standard GoodsTypes from Specification;
        // grainType and fishType have storedAs == foodType, lumberType has storedAs == lumberType.
        foodType = spec().getGoodsType("model.goods.food");
        grainType = spec().getGoodsType("model.goods.grain");
        fishType = spec().getGoodsType("model.goods.fish");
        lumberType = spec().getGoodsType("model.goods.lumber");
    }

    // ------------------------------------------------------------------
    // Pre-condition helpers & stubs
    // ------------------------------------------------------------------

    private AbstractGoods goods(GoodsType type, int amount) {
        return new AbstractGoods(type, amount);
    }

    // ------------------------------------------------------------------
    // M1: <init>() — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M1-TP1: ENTRY -> N0 -> EXIT (default constructor initializes empty cache)
    public void testM1_TP1_ConstructorInitializesEmptyCache() {
        ProductionMap map = new ProductionMap();
        assertNotNull(map);
        assertEquals(0, map.get(foodType).getAmount());
    }

    // ------------------------------------------------------------------
    // M2: get(GoodsType) — 3 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M2-TP1: ENTRY -> N0 -(B1:false)-> N1 -> EXIT (value == null -> returns 0 amount)
    public void testM2_TP1_GetMissingGoodsReturnsZero() {
        ProductionMap map = new ProductionMap();
        AbstractGoods result = map.get(lumberType);
        assertEquals(lumberType, result.getType());
        assertEquals(0, result.getAmount());
    }

    // M2-TP2: ENTRY -> N0 -(B1:true)-> N2 -(B2:false)-> N3 -> EXIT (value instanceof Integer)
    public void testM2_TP2_GetIntegerEntryReturnsAmount() {
        ProductionMap map = new ProductionMap();
        map.add(goods(lumberType, 7));
        AbstractGoods result = map.get(lumberType);
        assertEquals(lumberType, result.getType());
        assertEquals(7, result.getAmount());
    }

    // M2-TP3: ENTRY -> N0 -(B1:true)-> N2 -(B2:true)-> N4 -> EXIT (value instanceof ProductionTree)
    public void testM2_TP3_GetProductionTreeEntryDelegatesToTree() {
        ProductionMap map = new ProductionMap();
        map.add(goods(grainType, 5));
        AbstractGoods grainResult = map.get(grainType);
        AbstractGoods foodResult = map.get(foodType);
        assertEquals(5, grainResult.getAmount());
        assertEquals(5, foodResult.getAmount());
    }

    // ------------------------------------------------------------------
    // M3: add(AbstractGoods) — 6 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M3-TP1: ENTRY -> N0 -(B1:false)-> N1 -(B2:false)-> N2 -> N10 -> N14 -> EXIT
    // (value == null, rootType == goodsType -> stores Integer directly)
    public void testM3_TP1_AddNewRootGoodsStoresInteger() {
        ProductionMap map = new ProductionMap();
        map.add(goods(lumberType, 10));
        assertEquals(10, map.get(lumberType).getAmount());
    }

    // M3-TP2: ENTRY -> N0 -(B1:false)-> N1 -(B2:true)-> N3 -(B3:true)-> N5 -(B4:false)-> N6 -> N8 -> N9 -> N10 -> N14 -> EXIT
    // (value == null, rootType != goodsType, cache.get(rootType) == null -> new ProductionTree with amount=0)
    public void testM3_TP2_AddLeafWithoutExistingRootCreatesTreeWithZeroBase() {
        ProductionMap map = new ProductionMap();
        map.add(goods(grainType, 4));
        assertEquals(4, map.get(grainType).getAmount());
        assertEquals(4, map.get(foodType).getAmount());
    }

    // M3-TP3: ENTRY -> N0 -(B1:false)-> N1 -(B2:true)-> N3 -(B3:true)-> N5 -(B4:true)-> N7 -> N8 -> N9 -> N10 -> N14 -> EXIT
    // (value == null, rootType != goodsType, cache.get(rootType) is Integer -> new ProductionTree seeded with root Integer)
    public void testM3_TP3_AddLeafWithExistingIntegerRootPromotesToTree() {
        ProductionMap map = new ProductionMap();
        map.add(goods(foodType, 3));
        map.add(goods(grainType, 4));
        assertEquals(4, map.get(grainType).getAmount());
        assertEquals(7, map.get(foodType).getAmount());
    }

    // M3-TP4: ENTRY -> N0 -(B1:false)-> N1 -(B2:true)-> N3 -(B3:false)-> N4 -> N9 -> N10 -> N14 -> EXIT
    // (value == null, rootType != goodsType, cache.get(rootType) is ProductionTree -> adds leaf to existing tree)
    public void testM3_TP4_AddSecondLeafToExistingProductionTree() {
        ProductionMap map = new ProductionMap();
        map.add(goods(grainType, 4));
        map.add(goods(fishType, 6));
        assertEquals(4, map.get(grainType).getAmount());
        assertEquals(6, map.get(fishType).getAmount());
        assertEquals(10, map.get(foodType).getAmount());
    }

    // M3-TP5: ENTRY -> N0 -(B1:true)-> N11 -(B5:false)-> N12 -> N14 -> EXIT
    // (value != null, value instanceof Integer -> accumulates Integer)
    public void testM3_TP5_AddExistingIntegerEntryAccumulates() {
        ProductionMap map = new ProductionMap();
        map.add(goods(lumberType, 4));
        map.add(goods(lumberType, 5));
        assertEquals(9, map.get(lumberType).getAmount());
    }

    // M3-TP6: ENTRY -> N0 -(B1:true)-> N11 -(B5:true)-> N13 -> N14 -> EXIT
    // (value != null, value instanceof ProductionTree -> delegates to tree.add)
    public void testM3_TP6_AddExistingLeafEntryAccumulatesInTree() {
        ProductionMap map = new ProductionMap();
        map.add(goods(grainType, 4));
        map.add(goods(grainType, 5));
        assertEquals(9, map.get(grainType).getAmount());
        assertEquals(9, map.get(foodType).getAmount());
    }

    // ------------------------------------------------------------------
    // M4: remove(AbstractGoods) — 2 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M4-TP1: ENTRY -> N0 -(B1:false)-> N1 -> N3 -> EXIT (value instanceof ProductionTree -> delegates to tree.remove)
    public void testM4_TP1_RemoveFromProductionTree() {
        ProductionMap map = new ProductionMap();
        map.add(goods(grainType, 8));
        map.remove(goods(grainType, 3));
        assertEquals(5, map.get(grainType).getAmount());
        assertEquals(5, map.get(foodType).getAmount());
    }

    // M4-TP2: ENTRY -> N0 -(B1:true)-> N2 -> N3 -> EXIT (value not ProductionTree -> calls add(-amount))
    public void testM4_TP2_RemoveFromIntegerOrMissingEntry() {
        ProductionMap map = new ProductionMap();
        map.add(goods(lumberType, 10));
        map.remove(goods(lumberType, 4));
        assertEquals(6, map.get(lumberType).getAmount());
    }

    // ------------------------------------------------------------------
    // M5: add(List<AbstractGoods>) — 2 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M5-TP1: ENTRY -> N0 -> N1 -(B1:true)-> N3 -> EXIT (empty list -> 0 iterations)
    public void testM5_TP1_AddEmptyList() {
        ProductionMap map = new ProductionMap();
        map.add(Collections.<AbstractGoods>emptyList());
        assertEquals(0, map.get(lumberType).getAmount());
    }

    // M5-TP2: ENTRY -> N0 -> N1 -(B1:false)-> N2 -> N1 -(B1:true)-> N3 -> EXIT (non-empty list -> loop body + back-edge)
    public void testM5_TP2_AddNonEmptyList() {
        ProductionMap map = new ProductionMap();
        List<AbstractGoods> list = Arrays.asList(goods(lumberType, 3), goods(grainType, 5));
        map.add(list);
        assertEquals(3, map.get(lumberType).getAmount());
        assertEquals(5, map.get(grainType).getAmount());
    }

    // ------------------------------------------------------------------
    // M6: remove(List<AbstractGoods>) — 2 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M6-TP1: ENTRY -> N0 -> N1 -(B1:true)-> N3 -> EXIT (empty list -> 0 iterations)
    public void testM6_TP1_RemoveEmptyList() {
        ProductionMap map = new ProductionMap();
        map.add(goods(lumberType, 5));
        map.remove(Collections.<AbstractGoods>emptyList());
        assertEquals(5, map.get(lumberType).getAmount());
    }

    // M6-TP2: ENTRY -> N0 -> N1 -(B1:false)-> N2 -> N1 -(B1:true)-> N3 -> EXIT (non-empty list -> loop body + back-edge)
    public void testM6_TP2_RemoveNonEmptyList() {
        ProductionMap map = new ProductionMap();
        map.add(goods(lumberType, 10));
        map.add(goods(grainType, 8));
        map.remove(Arrays.asList(goods(lumberType, 4), goods(grainType, 3)));
        assertEquals(6, map.get(lumberType).getAmount());
        assertEquals(5, map.get(grainType).getAmount());
    }

    // ------------------------------------------------------------------
    // M7: toString() — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M7-TP1: ENTRY -> N0 -> EXIT (formats cache entries via forEachMapEntry)
    public void testM7_TP1_ToStringFormatsEntries() {
        ProductionMap map = new ProductionMap();
        map.add(goods(lumberType, 2));
        String str = map.toString();
        assertTrue(str.contains("lumber:2"));
    }

    // ------------------------------------------------------------------
    // M8: lambda$toString$0(StringBuilder, Map.Entry) — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M8-TP1: ENTRY -> N0 -> EXIT (invoked by toString() for each entry in cache)
    public void testM8_TP1_LambdaToStringAppendsEntry() {
        ProductionMap map = new ProductionMap();
        map.add(goods(grainType, 3));
        String str = map.toString();
        assertTrue(str.contains("grain:"));
        assertTrue(str.contains("food:"));
    }

    // ------------------------------------------------------------------
    // Inner class ProductionTree public API coverage
    // ------------------------------------------------------------------

    public void testProductionTreePublicApi() {
        ProductionTree emptyTree = new ProductionTree(goods(foodType, 5));
        assertEquals(5, emptyTree.getRoot().getAmount());
        assertNull(emptyTree.getLeafs());

        ProductionTree tree = new ProductionTree(goods(foodType, 2), goods(grainType, 3));
        assertEquals(5, tree.getRoot().getAmount());
        assertEquals(1, tree.getLeafs().size());
        assertNull(tree.get(fishType));
        assertTrue(tree.toString().contains("food:"));

        tree.remove(goods(foodType, 3));
        assertEquals(2, tree.getRoot().getAmount());
        assertEquals(2, tree.get(grainType).getAmount());

        tree.remove(goods(fishType, 1));
        assertEquals(2, tree.getRoot().getAmount());

        tree.setRoot(goods(foodType, 10));
        tree.setLeafs(Arrays.asList(goods(grainType, 10)));
        assertEquals(10, tree.getRoot().getAmount());

        try {
            tree.add(goods(lumberType, 1));
            fail("Expected IllegalArgumentException for mismatched storedAs");
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage().contains("is not stored as"));
        }
    }
}
