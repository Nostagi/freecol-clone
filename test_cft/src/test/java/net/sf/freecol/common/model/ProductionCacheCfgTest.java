package net.sf.freecol.common.model;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import net.sf.freecol.common.model.Modifier.ModifierType;
import net.sf.freecol.server.model.ServerUnit;
import net.sf.freecol.util.test.FreeColTestCase;

/**
 * CFG-driven test suite for {@link ProductionCache} derived from
 * {@code test_cft/graph/net/sf/freecol/common/model/ProductionCache.json}.
 *
 * Covers all 14 methods (M1..M14) and achieves 100% feasible C3 (node + edge)
 * coverage across 18 Test Paths.
 *
 * Infeasible edges documented:
 * - None (all edges in M1..M14 are feasible using standard Colony and stub Colony/Consumer).
 */
public class ProductionCacheCfgTest extends FreeColTestCase {

    private GoodsType foodType;
    private GoodsType bellsType;
    private GoodsType lumberType;
    private GoodsType hammersType;
    private GoodsType musketsType;
    private UnitType colonistType;
    private BuildingType carpenterHouseType;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        // Pre-condition: initialize standard GoodsTypes, UnitType, and BuildingType from Specification.
        foodType = spec().getGoodsType("model.goods.food");
        bellsType = spec().getGoodsType("model.goods.bells");
        lumberType = spec().getGoodsType("model.goods.lumber");
        hammersType = spec().getGoodsType("model.goods.hammers");
        musketsType = spec().getGoodsType("model.goods.muskets");
        colonistType = spec().getUnitType("model.unit.freeColonist");
        carpenterHouseType = spec().getBuildingType("model.building.carpenterHouse");
    }

    // ------------------------------------------------------------------
    // Pre-condition helpers & stubs
    // ------------------------------------------------------------------

    private static class StubColony extends Colony {
        private List<ColonyTile> stubTiles = Collections.emptyList();
        private List<Consumer> stubConsumers = Collections.emptyList();

        public StubColony(Game game, String id) {
            super(game, id);
        }

        public void setStubConsumers(List<Consumer> consumers) {
            this.stubConsumers = consumers;
        }

        @Override
        public List<ColonyTile> getColonyTiles() {
            return stubTiles;
        }

        @Override
        public List<Consumer> getConsumers() {
            return stubConsumers;
        }

        @Override
        public int getUnitCount() {
            return 0;
        }
    }

    private static class StubSurplusConsumer implements Consumer {
        private final GoodsType consumedType;
        private final Modifier modifier;

        public StubSurplusConsumer(GoodsType consumedType) {
            this.consumedType = consumedType;
            this.modifier = new Modifier(Modifier.CONSUME_ONLY_SURPLUS_PRODUCTION,
                                         1.0f, ModifierType.MULTIPLICATIVE);
        }

        @Override
        public List<AbstractGoods> getConsumedGoods() {
            return Collections.singletonList(new AbstractGoods(consumedType, 1));
        }

        @Override
        public int getPriority() {
            return Consumer.UNIT_PRIORITY;
        }

        @Override
        public Stream<Modifier> getConsumptionModifiers(String id) {
            if (Modifier.CONSUME_ONLY_SURPLUS_PRODUCTION.equals(id)) {
                return Stream.of(modifier);
            }
            return Stream.empty();
        }
    }

    private Colony createTestColony(int settlers) {
        Game game = getStandardGame();
        game.changeMap(getTestMap(true));
        return createStandardColony(settlers);
    }

    private boolean isUpToDate(ProductionCache cache) throws Exception {
        Field f = ProductionCache.class.getDeclaredField("upToDate");
        f.setAccessible(true);
        return f.getBoolean(cache);
    }

    private void invokePrivateUpdate(ProductionCache cache) throws Exception {
        Method m = ProductionCache.class.getDeclaredMethod("update");
        m.setAccessible(true);
        m.invoke(cache);
    }

    private int invokePrivateGetGoodsCount(ProductionCache cache, GoodsType type) throws Exception {
        Method m = ProductionCache.class.getDeclaredMethod("getGoodsCount", GoodsType.class);
        m.setAccessible(true);
        return (Integer) m.invoke(cache, type);
    }

    // ------------------------------------------------------------------
    // M1: ProductionCache(Colony) — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M1-TP1: ENTRY -> N0 -> EXIT (constructs cache with upToDate=false)
    public void testM1_TP1_ConstructorInitializesNotUpToDate() throws Exception {
        Colony colony = createTestColony(1);
        ProductionCache cache = new ProductionCache(colony);
        assertFalse(isUpToDate(cache));
    }

    // ------------------------------------------------------------------
    // M2: update() — 2 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M2-TP1: ENTRY -> N0 -(B1:true)-> N2 -> EXIT (upToDate == false -> calls reallyUpdate, sets upToDate=true)
    // M2-TP2: ENTRY -> N0 -(B1:false)-> N1 -> EXIT (upToDate == true -> returns immediately)
    public void testM2_TP1_And_TP2_UpdateColdThenWarm() throws Exception {
        Colony colony = createTestColony(1);
        ProductionCache cache = new ProductionCache(colony);
        assertFalse(isUpToDate(cache));
        // TP1: cold update
        invokePrivateUpdate(cache);
        assertTrue(isUpToDate(cache));
        // TP2: warm update (early return)
        invokePrivateUpdate(cache);
        assertTrue(isUpToDate(cache));
    }

    // ------------------------------------------------------------------
    // M3: reallyUpdate() — 3 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M3-TP1: ENTRY -> N0 -> N1 -(B1:true)-> N6 -> N7 -(B3:true)-> N29 -> EXIT
    // (empty colonyTiles and empty consumers)
    public void testM3_TP1_ReallyUpdateEmptyColony() {
        Game game = getStandardGame();
        StubColony stub = new StubColony(game, "colony:empty");
        ProductionCache cache = new ProductionCache(stub);
        assertEquals(0, cache.getNetProductionOf(bellsType));
        assertNotNull(cache.getProductionInfo(cache));
    }

    // M3-TP2: Covers colonyTiles loop (B1:false, B2:false/true), consumers loop (B3:false, B4:false/true,
    // B5:false), Building (B6:false), Unit (B7:false), BuildQueue (B8:false), info != null (B9:false),
    // info.getProduction() loop (B10:false/true), and info.getConsumption() loop (B11:false/true).
    public void testM3_TP2_ReallyUpdateStandardColonyWithBuildingUnitAndBuildQueue() {
        Game game = getGame();
        Colony colony = createTestColony(3);
        colony.addGoods(lumberType, 20);

        // Ensure a Building (Carpenter's House) has a worker producing hammers from lumber
        Building carpenterHouse = colony.getBuilding(carpenterHouseType);
        ServerUnit carpenter = new ServerUnit(game, colony.getTile(), colony.getOwner(), colonistType);
        carpenter.setLocation(carpenterHouse);

        // Ensure BuildQueue has a buildable item
        colony.setCurrentlyBuilding(carpenterHouseType);

        ProductionCache cache = new ProductionCache(colony);
        assertTrue(cache.isProducing(bellsType));
        assertTrue(cache.isProducing(hammersType));
        assertTrue(cache.isConsuming(foodType));
        assertTrue(cache.isConsuming(lumberType));
        assertNotNull(cache.getProductionInfo(carpenterHouse));
        assertNotNull(cache.getProductionInfo(carpenter));
    }

    // M3-TP3: Covers B5:true (!modifiers.isEmpty() -> FeatureContainer.applyModifiers),
    // B6:true, B7:true, B8:true (consumer not Building/Unit/BuildQueue), and B9:true (info == null).
    public void testM3_TP3_ReallyUpdateSurplusModifierAndCustomConsumer() {
        Game game = getStandardGame();
        StubColony stub = new StubColony(game, "colony:customConsumer");
        stub.setStubConsumers(Collections.<Consumer>singletonList(new StubSurplusConsumer(foodType)));
        ProductionCache cache = new ProductionCache(stub);

        // Triggers update() -> reallyUpdate() with custom Consumer
        assertFalse(cache.isConsuming(foodType));
        assertNull(cache.getProductionInfo(stub.getConsumers().get(0)));
    }

    // ------------------------------------------------------------------
    // M4: getGoodsCount(GoodsType) — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M4-TP1: ENTRY -> N0 -> EXIT (delegates to colony.getGoodsCount(type))
    public void testM4_TP1_GetGoodsCount() throws Exception {
        Colony colony = createTestColony(1);
        colony.addGoods(lumberType, 15);
        ProductionCache cache = new ProductionCache(colony);
        assertEquals(15, invokePrivateGetGoodsCount(cache, lumberType));
    }

    // ------------------------------------------------------------------
    // M5: invalidate() — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M5-TP1: ENTRY -> N0 -> EXIT (sets upToDate = false)
    public void testM5_TP1_InvalidateSetsUpToDateFalse() throws Exception {
        Colony colony = createTestColony(1);
        ProductionCache cache = new ProductionCache(colony);
        invokePrivateUpdate(cache);
        assertTrue(isUpToDate(cache));

        cache.invalidate();
        assertFalse(isUpToDate(cache));
    }

    // ------------------------------------------------------------------
    // M6: invalidate(GoodsType) — 2 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M6-TP1: ENTRY -> N0 -(B1:true)-> N2 -> EXIT (goodsUsed does not contain goodsType -> stays upToDate)
    public void testM6_TP1_InvalidateUnusedGoodsKeepsUpToDate() throws Exception {
        Colony colony = createTestColony(1);
        ProductionCache cache = new ProductionCache(colony);
        invokePrivateUpdate(cache);
        assertTrue(isUpToDate(cache));

        cache.invalidate(musketsType);
        assertTrue(isUpToDate(cache));
    }

    // M6-TP2: ENTRY -> N0 -(B1:false)-> N1 -> N2 -> EXIT (goodsUsed contains goodsType -> upToDate = false)
    public void testM6_TP2_InvalidateUsedGoodsClearsUpToDate() throws Exception {
        Colony colony = createTestColony(1);
        ProductionCache cache = new ProductionCache(colony);
        invokePrivateUpdate(cache);
        assertTrue(isUpToDate(cache));

        cache.invalidate(foodType);
        assertFalse(isUpToDate(cache));
    }

    // ------------------------------------------------------------------
    // M7: isProducing(GoodsType) — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M7-TP1: ENTRY -> N0 -> EXIT (updates cache and checks production entries)
    public void testM7_TP1_IsProducing() {
        Colony colony = createTestColony(1);
        ProductionCache cache = new ProductionCache(colony);
        assertTrue(cache.isProducing(bellsType));
        assertFalse(cache.isProducing(musketsType));
    }

    // ------------------------------------------------------------------
    // M8: isConsuming(GoodsType) — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M8-TP1: ENTRY -> N0 -> EXIT (updates cache and checks consumption entries)
    public void testM8_TP1_IsConsuming() {
        Colony colony = createTestColony(1);
        ProductionCache cache = new ProductionCache(colony);
        assertTrue(cache.isConsuming(foodType));
        assertFalse(cache.isConsuming(musketsType));
    }

    // ------------------------------------------------------------------
    // M9: getNetProductionOf(GoodsType) — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M9-TP1: ENTRY -> N0 -> EXIT (updates cache and returns net count)
    public void testM9_TP1_GetNetProductionOf() {
        Colony colony = createTestColony(1);
        ProductionCache cache = new ProductionCache(colony);
        assertTrue(cache.getNetProductionOf(bellsType) >= 1);
        assertEquals(0, cache.getNetProductionOf(musketsType));
    }

    // ------------------------------------------------------------------
    // M10: getProductionInfo(Object) — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M10-TP1: ENTRY -> N0 -> EXIT (updates cache and returns ProductionInfo for producer/consumer)
    public void testM10_TP1_GetProductionInfo() {
        Colony colony = createTestColony(1);
        ProductionCache cache = new ProductionCache(colony);
        assertNotNull(cache.getProductionInfo(cache));
        assertNull(cache.getProductionInfo(new Object()));
    }

    // ------------------------------------------------------------------
    // M11: getProductionMap() — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M11-TP1: ENTRY -> N0 -> EXIT (updates cache and returns copy of netProduction)
    public void testM11_TP1_GetProductionMap() {
        Colony colony = createTestColony(1);
        ProductionCache cache = new ProductionCache(colony);
        TypeCountMap<GoodsType> map = cache.getProductionMap();
        assertNotNull(map);
        assertTrue(map.getCount(bellsType) >= 1);
    }

    // ------------------------------------------------------------------
    // M12: lambda$isConsuming$0(GoodsType, ProductionInfo) — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M12-TP1: ENTRY -> N0 -> EXIT (lambda predicate invoked by isConsuming)
    public void testM12_TP1_LambdaIsConsuming() {
        Colony colony = createTestColony(1);
        ProductionCache cache = new ProductionCache(colony);
        assertTrue(cache.isConsuming(foodType));
    }

    // ------------------------------------------------------------------
    // M13: lambda$isProducing$0(GoodsType, ProductionInfo) — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M13-TP1: ENTRY -> N0 -> EXIT (lambda predicate invoked by isProducing)
    public void testM13_TP1_LambdaIsProducing() {
        Colony colony = createTestColony(1);
        ProductionCache cache = new ProductionCache(colony);
        assertTrue(cache.isProducing(bellsType));
    }

    // ------------------------------------------------------------------
    // M14: lambda$reallyUpdate$0(ProductionMap, AbstractGoods) — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M14-TP1: ENTRY -> N0 -> EXIT (lambda mapper invoked for Building outputs in reallyUpdate)
    public void testM14_TP1_LambdaReallyUpdateBuildingOutputMapper() {
        Game game = getGame();
        Colony colony = createTestColony(2);
        colony.addGoods(lumberType, 10);
        Building carpenterHouse = colony.getBuilding(carpenterHouseType);
        ServerUnit carpenter = new ServerUnit(game, colony.getTile(), colony.getOwner(), colonistType);
        carpenter.setLocation(carpenterHouse);

        ProductionCache cache = new ProductionCache(colony);
        assertTrue(cache.isProducing(hammersType));
    }
}
