package net.sf.freecol.common.model.production;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.stream.Collectors;

import net.sf.freecol.common.model.AbstractGoods;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.GoodsType;
import net.sf.freecol.common.model.Modifier;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.ProductionInfo;
import net.sf.freecol.common.model.ProductionType;
import net.sf.freecol.common.model.Specification;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.TileImprovement;
import net.sf.freecol.common.model.TileImprovementType;
import net.sf.freecol.common.model.TileType;
import net.sf.freecol.common.model.Turn;
import net.sf.freecol.common.model.UnitType;
import net.sf.freecol.common.model.production.TileProductionCalculator.MaximumPotentialProduction;
import net.sf.freecol.util.test.FreeColTestCase;


/**
 * CFG-driven test suite for {@link TileProductionCalculator} derived from
 * {@code test_cft/graph/net/sf/freecol/common/model/production/TileProductionCalculator.json}.
 *
 * Covers all 17 methods (constructor, public/private methods, and compiler-generated
 * lambdas) and achieves 100% C2 (node + edge) coverage on all feasible edges
 * (edges N17->N18 and N18->N19 in {@code getMaximumPotentialProduction} are infeasible
 * under the standard specification).
 */
public class TileProductionCalculatorCfgTest extends FreeColTestCase {

    private static final GoodsType grain = spec().getGoodsType("model.goods.grain");
    private static final GoodsType cotton = spec().getGoodsType("model.goods.cotton");
    private static final GoodsType lumber = spec().getGoodsType("model.goods.lumber");
    private static final GoodsType fish = spec().getGoodsType("model.goods.fish");
    private static final GoodsType bells = spec().getGoodsType("model.goods.bells"); // isFarmed() == false

    private static final TileType plains = spec().getTileType("model.tile.plains");
    private static final TileType mixedForest = spec().getTileType("model.tile.mixedForest");
    private static final TileType highSeas = spec().getTileType("model.tile.highSeas");

    private static final TileImprovementType plow = spec().getTileImprovementType("model.improvement.plow");
    private static final TileImprovementType road = spec().getTileImprovementType("model.improvement.road");
    private static final TileImprovementType clearForest = spec().getTileImprovementType("model.improvement.clearForest");

    private static final UnitType freeColonist = spec().getUnitType("model.unit.freeColonist");

    private Game testGame;
    private Player dutchPlayer;
    private Turn turn1;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        // Pre-condition: standard game, Dutch player, turn 1.
        testGame = getStandardGame();
        dutchPlayer = testGame.getPlayerByNationId("model.nation.dutch");
        turn1 = new Turn(1);
    }

    // ------------------------------------------------------------------
    // Pre-condition helpers
    // ------------------------------------------------------------------

    private ProductionType bestProduction(TileType tileType, GoodsType goodsType, boolean unattended) {
        return ProductionType.getBestProductionType(
                goodsType, tileType.getAvailableProductionTypes(unattended));
    }

    private Object invokePrivate(Object target, String methodName, Class<?>[] paramTypes, Object... args)
            throws Exception {
        Method method = TileProductionCalculator.class.getDeclaredMethod(methodName, paramTypes);
        method.setAccessible(true);
        try {
            return method.invoke(target, args);
        } catch (InvocationTargetException e) {
            if (e.getCause() instanceof RuntimeException) {
                throw (RuntimeException) e.getCause();
            }
            throw e;
        }
    }

    // ------------------------------------------------------------------
    // M1: <init>(Player, int) & M2: getBasicProductionInfo(4 args) — 100% C2
    // ------------------------------------------------------------------

    // M1-TP1: ENTRY -> N0 -> EXIT
    // M2-TP1: ENTRY -> N0 -> EXIT
    public void testM1_M2_TP1_ConstructorAndFourArgGetBasicProductionInfo() {
        TileProductionCalculator calc = new TileProductionCalculator(null, 0);
        Tile tile = new Tile(testGame, plains, 0, 0);
        ProductionType prodGrain = bestProduction(plains, grain, false);
        WorkerAssignment assignment = new WorkerAssignment(freeColonist, prodGrain);

        ProductionInfo pi = calc.getBasicProductionInfo(tile, turn1, assignment, false);
        assertEquals(1, pi.getProduction().size());
        assertEquals(grain, pi.getProduction().get(0).getType());
        assertEquals(5, pi.getProduction().get(0).getAmount());
    }

    // ------------------------------------------------------------------
    // M3: getBasicProductionInfo(5 args) & lambdas $0, $1 — 3 Test Paths (100% C2)
    // ------------------------------------------------------------------

    // M3-TP1: ENTRY -> N0 -(B1:false)-> N1 -> EXIT (productionType == null)
    public void testM3_TP1_GetBasicProductionInfoNullProductionType() throws Exception {
        TileProductionCalculator calc = new TileProductionCalculator(null, 0);
        Tile tile = new Tile(testGame, plains, 0, 0);
        WorkerAssignment assignment = new WorkerAssignment(freeColonist, null);

        ProductionInfo pi = (ProductionInfo) invokePrivate(
                calc, "getBasicProductionInfo",
                new Class<?>[]{Tile.class, Turn.class, WorkerAssignment.class, boolean.class, List.class},
                tile, turn1, assignment, false, List.of());

        assertTrue(pi.getProduction().isEmpty());
    }

    // M3-TP2: ENTRY -> N0 -(B1:true)-> N2 -(B2:false)-> N3 -> N5 -> EXIT (colonyCenterTile == true)
    // Also covers lambda$getBasicProductionInfo$0 (B1:false when n > 0, B1:true when n <= 0)
    public void testM3_TP2_GetBasicProductionInfoCenterTilePositiveAndZero() throws Exception {
        TileProductionCalculator calc = new TileProductionCalculator(null, 0);

        // lambda$getBasicProductionInfo$0: ENTRY -> N0 -(B1:false)-> N1 -> N2 -> EXIT (n > 0)
        Tile plainsTile = new Tile(testGame, plains, 0, 0);
        WorkerAssignment centerPlains = new WorkerAssignment(null, bestProduction(plains, grain, true));
        ProductionInfo piPositive = (ProductionInfo) invokePrivate(
                calc, "getBasicProductionInfo",
                new Class<?>[]{Tile.class, Turn.class, WorkerAssignment.class, boolean.class, List.class},
                plainsTile, turn1, centerPlains, true, List.of());
        assertFalse(piPositive.getProduction().isEmpty());

        // lambda$getBasicProductionInfo$0: ENTRY -> N0 -(B1:true)-> N2 -> EXIT (n <= 0)
        Tile seaTile = new Tile(testGame, highSeas, 0, 1);
        ProductionType zeroGrainProd = new ProductionType(new AbstractGoods(grain, 0), true, null);
        WorkerAssignment centerSea = new WorkerAssignment(null, zeroGrainProd);
        ProductionInfo piZero = (ProductionInfo) invokePrivate(
                calc, "getBasicProductionInfo",
                new Class<?>[]{Tile.class, Turn.class, WorkerAssignment.class, boolean.class, List.class},
                seaTile, turn1, centerSea, true, List.of());
        assertTrue(piZero.getProduction().isEmpty());
    }

    // M3-TP3: ENTRY -> N0 -(B1:true)-> N2 -(B2:true)-> N4 -> N5 -> EXIT (colonyCenterTile == false)
    // Also covers lambda$getBasicProductionInfo$1 (B1:false when n > 0, B1:true when n <= 0)
    public void testM3_TP3_GetBasicProductionInfoWorkerPositiveAndZero() throws Exception {
        Tile tile = new Tile(testGame, plains, 0, 0);
        WorkerAssignment assignment = new WorkerAssignment(freeColonist, bestProduction(plains, grain, false));

        // lambda$getBasicProductionInfo$1: ENTRY -> N0 -(B1:false)-> N1 -> N2 -> EXIT (n > 0)
        TileProductionCalculator calcNormal = new TileProductionCalculator(null, 0);
        ProductionInfo piPositive = (ProductionInfo) invokePrivate(
                calcNormal, "getBasicProductionInfo",
                new Class<?>[]{Tile.class, Turn.class, WorkerAssignment.class, boolean.class, List.class},
                tile, turn1, assignment, false, List.of());
        assertEquals(5, piPositive.getProduction().get(0).getAmount());

        // lambda$getBasicProductionInfo$1: ENTRY -> N0 -(B1:true)-> N2 -> EXIT (n <= 0 via penalty)
        TileProductionCalculator calcPenalty = new TileProductionCalculator(null, -10);
        ProductionInfo piZero = (ProductionInfo) invokePrivate(
                calcPenalty, "getBasicProductionInfo",
                new Class<?>[]{Tile.class, Turn.class, WorkerAssignment.class, boolean.class, List.class},
                tile, turn1, assignment, false, List.of());
        assertTrue(piZero.getProduction().isEmpty());
    }

    // ------------------------------------------------------------------
    // M4: getMaximumPotentialProduction(GoodsType, Tile, UnitType) — 4 Test Paths
    // ------------------------------------------------------------------

    // M4-TP1: ENTRY -> N0 -(B1:false)-> N1 -> N3 -> N4 -(B2:true)-> N10 -> N11 -(B5:true)-> N20 -> EXIT
    // (unitType == null, both improvement loops empty)
    public void testM4_TP1_GetMaximumPotentialProductionHighSeasUnattended() {
        TileProductionCalculator calc = new TileProductionCalculator(null, 0);
        Tile seaTile = new Tile(testGame, highSeas, 0, 0);

        MaximumPotentialProduction mpp = calc.getMaximumPotentialProduction(fish, seaTile, null);
        assertEquals(0, mpp.getAmount(fish));
        assertTrue(mpp.getTileImprovementType().isEmpty());
    }

    // M4-TP2: ENTRY -> N0 -(B1:true)-> N2 -> N3 -> N4 -(B2:false)-> N5 -(B3:false)-> N6 -> N4
    //         -(B2:false)-> N5 -(B3:true)-> N7 -(B4:false)-> N8 -> N9 -> N4 -(B2:true)-> N10 -> N11 -(B5:true)-> N20 -> EXIT
    // (road already present -> continue; plow allowed and increases production)
    public void testM4_TP2_GetMaximumPotentialProductionPlainsWithExistingRoad() {
        TileProductionCalculator calc = new TileProductionCalculator(null, 0);
        Tile tile = new Tile(testGame, plains, 0, 0);
        TileImprovement existingRoad = new TileImprovement(testGame, tile, road, null);
        existingRoad.setTurnsToComplete(0);
        tile.add(existingRoad); // causes tile.isImprovementTypeAllowed(road) == false

        MaximumPotentialProduction mpp = calc.getMaximumPotentialProduction(grain, tile, freeColonist);
        assertEquals(6, mpp.getAmount(grain));
        assertEquals(List.of(plow), mpp.getTileImprovementType());
    }

    // M4-TP3: ENTRY -> N0 -> N2 -> N3 -> N4 -> N5 -> N7 -(B4:true)-> N9 -> N4 -> N10 -> N11
    //         -(B5:false)-> N12 -> N13 -(B6:false)-> N14 -(B7:false)-> N15 -> N16 -> N13
    //         -(B6:true)-> N17 -(B8:true)-> N19 -> N11 -(B5:true)-> N20 -> EXIT
    // (also covers lambda$getMaximumPotentialProduction$0 all 4 branches)
    public void testM4_TP3_GetMaximumPotentialProductionForestGrain() {
        TileProductionCalculator calc = new TileProductionCalculator(null, 0);
        Tile tile = new Tile(testGame, mixedForest, 0, 0);

        MaximumPotentialProduction mpp = calc.getMaximumPotentialProduction(grain, tile, freeColonist);
        assertEquals(6, mpp.getAmount(grain));
        assertEquals(List.of(clearForest, plow), mpp.getTileImprovementType());
    }

    // M4-TP4: ENTRY -> N0 -> N2 -> N3 -> N4 -> N5 -> N7 -(B4:false)-> N8 -> N9 -> N4 -> N10 -> N11
    //         -(B5:false)-> N12 -> N13 -(B6:false)-> N14 -(B7:true)-> N16 -> N13
    //         -(B6:true)-> N17 -(B8:true)-> N19 -> N11 -(B5:true)-> N20 -> EXIT
    // (inner loop amount <= currentMaximumPotentialProduction)
    public void testM4_TP4_GetMaximumPotentialProductionForestLumber() {
        TileProductionCalculator calc = new TileProductionCalculator(null, 0);
        Tile tile = new Tile(testGame, mixedForest, 0, 0);

        MaximumPotentialProduction mpp = calc.getMaximumPotentialProduction(lumber, tile, freeColonist);
        assertEquals(8, mpp.getAmount(lumber));
        assertEquals(List.of(road), mpp.getTileImprovementType());
    }

    // ------------------------------------------------------------------
    // M5: getNormalImprovements(Specification, TileType) — 1 Test Path (100% C2)
    // ------------------------------------------------------------------

    // M5-TP1: ENTRY -> N0 -> EXIT (also covers lambda$getNormalImprovements$0 all 4 branches)
    @SuppressWarnings("unchecked")
    public void testM5_TP1_GetNormalImprovements() throws Exception {
        List<TileImprovementType> normal = (List<TileImprovementType>) invokePrivate(
                null, "getNormalImprovements",
                new Class<?>[]{Specification.class, TileType.class},
                spec(), mixedForest);

        assertTrue(normal.contains(road));
        assertFalse(normal.contains(plow));
        assertFalse(normal.contains(clearForest));
    }

    // ------------------------------------------------------------------
    // M6: productionOfGoodsType(GoodsType, ProductionInfo) — 1 Test Path (100% C2)
    // ------------------------------------------------------------------

    // M6-TP1: ENTRY -> N0 -> EXIT (also covers lambda$productionOfGoodsType$0)
    public void testM6_TP1_ProductionOfGoodsType() throws Exception {
        ProductionInfo pi = new ProductionInfo();
        pi.addProduction(new AbstractGoods(grain, 5));

        int found = (int) invokePrivate(
                null, "productionOfGoodsType",
                new Class<?>[]{GoodsType.class, ProductionInfo.class},
                grain, pi);
        int notFound = (int) invokePrivate(
                null, "productionOfGoodsType",
                new Class<?>[]{GoodsType.class, ProductionInfo.class},
                cotton, pi);

        assertEquals(5, found);
        assertEquals(0, notFound);
    }

    // ------------------------------------------------------------------
    // M7: getUnitProduction(...) — 4 Test Paths (100% C2)
    // ------------------------------------------------------------------

    // M7-TP1: ENTRY -> N0 -(B1:true)-> N3 -> EXIT (workerAssignment == null)
    public void testM7_TP1_GetUnitProductionNullAssignment() {
        TileProductionCalculator calc = new TileProductionCalculator(null, 0);
        Tile tile = new Tile(testGame, plains, 0, 0);

        assertEquals(0, calc.getUnitProduction(turn1, tile, null, grain, List.of()));
    }

    // M7-TP2: ENTRY -> N0 -(B1:false)-> N1 -(B2:true)-> N3 -> EXIT
    // (outputs.noneMatch(goodsType) == true; also covers lambda$getUnitProduction$0)
    public void testM7_TP2_GetUnitProductionNoMatchingGoodsType() {
        TileProductionCalculator calc = new TileProductionCalculator(null, 0);
        Tile tile = new Tile(testGame, plains, 0, 0);
        WorkerAssignment assignment = new WorkerAssignment(freeColonist, bestProduction(plains, grain, false));

        assertEquals(0, calc.getUnitProduction(turn1, tile, assignment, cotton, List.of()));
    }

    // M7-TP3: ENTRY -> N0 -(B1:false)-> N1 -(B2:false)-> N2 -(B3:false)-> N3 -> EXIT (unitType == null)
    public void testM7_TP3_GetUnitProductionNullUnitType() {
        TileProductionCalculator calc = new TileProductionCalculator(null, 0);
        Tile tile = new Tile(testGame, plains, 0, 0);
        WorkerAssignment assignment = new WorkerAssignment(null, bestProduction(plains, grain, false));

        assertEquals(0, calc.getUnitProduction(turn1, tile, assignment, grain, List.of()));
    }

    // M7-TP4: ENTRY -> N0 -(B1:false)-> N1 -(B2:false)-> N2 -(B3:true)-> N4 -> EXIT (valid assignment)
    public void testM7_TP4_GetUnitProductionValid() {
        TileProductionCalculator calc = new TileProductionCalculator(null, 0);
        Tile tile = new Tile(testGame, plains, 0, 0);
        WorkerAssignment assignment = new WorkerAssignment(freeColonist, bestProduction(plains, grain, false));

        assertEquals(6, calc.getUnitProduction(turn1, tile, assignment, grain, List.of(plow)));
    }

    // ------------------------------------------------------------------
    // M8: getCenterTileProduction(...) — 1 Test Path (100% C2)
    // ------------------------------------------------------------------

    // M8-TP1: ENTRY -> N0 -> EXIT
    public void testM8_TP1_GetCenterTileProduction() throws Exception {
        TileProductionCalculator calc = new TileProductionCalculator(null, 0);
        Tile tile = new Tile(testGame, plains, 0, 0);

        int prod = (int) invokePrivate(
                calc, "getCenterTileProduction",
                new Class<?>[]{Turn.class, Tile.class, GoodsType.class, List.class},
                turn1, tile, grain, List.of());

        assertEquals(3, prod);
    }

    // ------------------------------------------------------------------
    // M9: getBaseProduction(...) — 5 Test Paths (100% C2)
    // ------------------------------------------------------------------

    // Covers M9-TP1..TP5
    public void testM9_GetBaseProductionAllBranches() throws Exception {
        TileProductionCalculator calc = new TileProductionCalculator(null, 0);
        ProductionType validProd = bestProduction(plains, grain, false);
        ProductionType negativeProd = new ProductionType(new AbstractGoods(grain, -1), false, null);
        ProductionType nonFarmedProd = new ProductionType(new AbstractGoods(bells, 3), false, null);

        // M9-TP1: ENTRY -> N0 -(B1:true)-> N3 -> EXIT (tileType == null)
        assertEquals(0, (int) invokePrivate(calc, "getBaseProduction",
                new Class<?>[]{TileType.class, ProductionType.class, GoodsType.class, UnitType.class},
                null, validProd, grain, freeColonist));

        // M9-TP2: ENTRY -> N0 -(B1:false)-> N1 -(B2:true)-> N3 -> EXIT (goodsType == null)
        assertEquals(0, (int) invokePrivate(calc, "getBaseProduction",
                new Class<?>[]{TileType.class, ProductionType.class, GoodsType.class, UnitType.class},
                plains, validProd, null, freeColonist));

        // M9-TP3: ENTRY -> N0 -(B1:false)-> N1 -(B2:false)-> N2 -(B3:false)-> N3 -> EXIT (!goodsType.isFarmed())
        assertEquals(0, (int) invokePrivate(calc, "getBaseProduction",
                new Class<?>[]{TileType.class, ProductionType.class, GoodsType.class, UnitType.class},
                plains, nonFarmedProd, bells, freeColonist));

        // M9-TP4: ENTRY -> N0 -> N1 -> N2 -(B3:true)-> N4 -(B4:false)-> N5 -> N7 -> EXIT (amount < 0)
        assertEquals(0, (int) invokePrivate(calc, "getBaseProduction",
                new Class<?>[]{TileType.class, ProductionType.class, GoodsType.class, UnitType.class},
                plains, negativeProd, grain, freeColonist));

        // M9-TP5: ENTRY -> N0 -> N1 -> N2 -(B3:true)-> N4 -(B4:true)-> N6 -> N7 -> EXIT (amount >= 0)
        assertEquals(5, (int) invokePrivate(calc, "getBaseProduction",
                new Class<?>[]{TileType.class, ProductionType.class, GoodsType.class, UnitType.class},
                plains, validProd, grain, freeColonist));
    }

    // ------------------------------------------------------------------
    // M10: getResultingTileType(TileType, List) — 3 Test Paths (100% C2)
    // ------------------------------------------------------------------

    // Covers M10-TP1..TP3 and lambda$getResultingTileType$0
    public void testM10_GetResultingTileTypeAllBranches() throws Exception {
        TileProductionCalculator calc = new TileProductionCalculator(null, 0);

        // M10-TP1: ENTRY -> N0 -(B1:false)-> N1 -> EXIT (tileTypeChange.size() > 1 -> throw)
        try {
            invokePrivate(calc, "getResultingTileType",
                    new Class<?>[]{TileType.class, List.class},
                    mixedForest, List.of(clearForest, clearForest));
            fail("Expected IllegalArgumentException when multiple tile type changes are provided");
        } catch (IllegalArgumentException expected) {
            // Expected exception path
        }

        // M10-TP2: ENTRY -> N0 -(B1:true)-> N2 -(B2:false)-> N3 -> EXIT (tileTypeChange.isEmpty())
        TileType unchanged = (TileType) invokePrivate(calc, "getResultingTileType",
                new Class<?>[]{TileType.class, List.class},
                mixedForest, List.of(plow));
        assertEquals(mixedForest, unchanged);

        // M10-TP3: ENTRY -> N0 -(B1:true)-> N2 -(B2:true)-> N4 -> EXIT (tileTypeChange.size() == 1)
        TileType changed = (TileType) invokePrivate(calc, "getResultingTileType",
                new Class<?>[]{TileType.class, List.class},
                mixedForest, List.of(clearForest));
        assertEquals(plains, changed);
    }

    // ------------------------------------------------------------------
    // M11: getProductionModifiers(...) — 6 Test Paths (100% C2)
    // ------------------------------------------------------------------

    // Covers M11-TP1..TP6 and lambdas $0, $1
    public void testM11_GetProductionModifiersAllPaths() {
        TileProductionCalculator calcNoOwner = new TileProductionCalculator(null, 0);
        TileProductionCalculator calcWithOwner = new TileProductionCalculator(dutchPlayer, 1);

        // M11-TP1: ENTRY -> N0 -(B1:true)-> N2 -> N3 -(B2:false)-> N4 -> N8 -(B4:true)-> N10 -(B6:false)-> N11 -> EXIT
        // (tileTypeChanged == false, !tile.canProduce)
        Tile plainsTile = new Tile(testGame, plains, 0, 0);
        List<Modifier> m1 = calcNoOwner
                .getProductionModifiers(turn1, plainsTile, fish, freeColonist, List.of())
                .collect(Collectors.toList());
        assertTrue(m1.isEmpty());

        // M11-TP2: ENTRY -> N0 -(B1:false)-> N1 -> N3 -(B2:true)-> N5 -(B3:true)-> N7 -> N8 -(B4:false)-> N9 -(B5:true)-> N11 -> EXIT
        // (tileTypeChanged == true, tileItemContainer == null, !newTileType.canProduce)
        Tile forestNoItems = new Tile(testGame, mixedForest, 0, 1);
        List<Modifier> m2 = calcNoOwner
                .getProductionModifiers(turn1, forestNoItems, lumber, freeColonist, List.of(clearForest))
                .collect(Collectors.toList());
        assertTrue(m2.isEmpty());

        // M11-TP3: ENTRY -> N0 -(B1:false)-> N1 -> N3 -(B2:true)-> N5 -(B3:false)-> N6 -> N8 -(B4:false)-> N9 -(B5:false)-> N10 -(B6:false)-> N11 -> EXIT
        // (tileTypeChanged == true, tileItemContainer != null, newTileType.canProduce == true, !tile.canProduce)
        Tile forestWithRoad = new Tile(testGame, mixedForest, 0, 2);
        TileImprovement roadImp = new TileImprovement(testGame, forestWithRoad, road, null);
        roadImp.setTurnsToComplete(0);
        forestWithRoad.add(roadImp);
        List<Modifier> m3 = calcNoOwner
                .getProductionModifiers(turn1, forestWithRoad, cotton, null, List.of(clearForest))
                .collect(Collectors.toList());
        assertTrue(m3.isEmpty());

        // M11-TP4: ENTRY -> N0 -> N2 -> N3 -> N4 -> N8 -> N10 -(B6:true)-> N12 -(B7:false)-> N13 -> N15 -(B8:true)-> N18 -> N19 -(B10:false)-> N20 -> N22 -> EXIT
        // (tileTypeChanged == false, tile.canProduce == true, unitType != null, owner == null)
        List<Modifier> m4 = calcNoOwner
                .getProductionModifiers(turn1, plainsTile, grain, freeColonist, List.of(plow))
                .collect(Collectors.toList());
        assertFalse(m4.isEmpty());

        // M11-TP5: ENTRY -> N0 -> N1 -> N3 -> N5 -> N7 -> N8 -> N9 -> N10 -(B6:true)-> N12 -(B7:false)-> N13 -> N15 -(B8:false)-> N16 -(B9:false)-> N17 -> N19 -(B10:true)-> N21 -> N22 -> EXIT
        // (tileTypeChanged == true, tileItemContainer == null, canProduce == true, unitType != null, owner != null)
        List<Modifier> m5 = calcWithOwner
                .getProductionModifiers(turn1, forestNoItems, grain, freeColonist, List.of(clearForest, plow))
                .collect(Collectors.toList());
        assertFalse(m5.isEmpty());

        // M11-TP6: ENTRY -> N0 -> N2 -> N3 -> N4 -> N8 -> N10 -(B6:true)-> N12 -(B7:true)-> N14 -> N15 -(B8:false)-> N16 -(B9:true)-> N18 -> N19 -(B10:false)-> N20 -> N22 -> EXIT
        // (tileTypeChanged == false, tile.canProduce == true, unitType == null, owner != null)
        List<Modifier> m6 = calcWithOwner
                .getProductionModifiers(turn1, plainsTile, grain, null, List.of())
                .collect(Collectors.toList());
        assertFalse(m6.isEmpty());
    }

    // ------------------------------------------------------------------
    // M12: getCenterTileProductionModifiers(...) — 1 Test Path (100% C2)
    // ------------------------------------------------------------------

    // M12-TP1: ENTRY -> N0 -> EXIT
    public void testM12_TP1_GetCenterTileProductionModifiers() {
        TileProductionCalculator calc = new TileProductionCalculator(dutchPlayer, 1);
        Tile plainsTile = new Tile(testGame, plains, 0, 0);

        List<Modifier> mods = calc
                .getCenterTileProductionModifiers(turn1, plainsTile, grain, List.of())
                .collect(Collectors.toList());
        assertFalse(mods.isEmpty());
    }
}
