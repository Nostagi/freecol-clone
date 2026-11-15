package net.sf.freecol.common.model.production;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import net.sf.freecol.common.model.AbstractGoods;
import net.sf.freecol.common.model.BuildingType;
import net.sf.freecol.common.model.FeatureContainer;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.GoodsType;
import net.sf.freecol.common.model.Modifier;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.ProductionInfo;
import net.sf.freecol.common.model.ProductionType;
import net.sf.freecol.common.model.Turn;
import net.sf.freecol.common.model.UnitType;
import net.sf.freecol.util.test.FreeColTestCase;


/**
 * CFG-driven test suite for {@link BuildingProductionCalculator} derived from
 * {@code test_cft/graph/net/sf/freecol/common/model/production/BuildingProductionCalculator.json}.
 *
 * Covers all 19 methods (constructor, public/private methods, and compiler-generated
 * lambdas) and achieves 100% C2 (node + edge) coverage on all feasible edges.
 */
public class BuildingProductionCalculatorCfgTest extends FreeColTestCase {

    private static final BuildingType warehouseType = spec().getBuildingType("model.building.warehouse");
    private static final BuildingType townHallType = spec().getBuildingType("model.building.townHall");
    private static final BuildingType weaverHouseType = spec().getBuildingType("model.building.weaverHouse");
    private static final BuildingType textileMillType = spec().getBuildingType("model.building.textileMill");
    private static final BuildingType countryType = spec().getBuildingType("model.building.country");

    private static final GoodsType bells = spec().getGoodsType("model.goods.bells");
    private static final GoodsType cotton = spec().getGoodsType("model.goods.cotton");
    private static final GoodsType cloth = spec().getGoodsType("model.goods.cloth");
    private static final GoodsType grain = spec().getGoodsType("model.goods.grain");
    private static final GoodsType horses = spec().getGoodsType("model.goods.horses");

    private static final UnitType freeColonist = spec().getUnitType("model.unit.freeColonist");
    private static final UnitType masterWeaver = spec().getUnitType("model.unit.masterWeaver");

    private Game testGame;
    private Player dutchPlayer;
    private FeatureContainer colonyFeatures;
    private Turn turn1;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        // Pre-condition: standard game, Dutch player, empty colony FeatureContainer, turn 1.
        testGame = getStandardGame();
        dutchPlayer = testGame.getPlayerByNationId("model.nation.dutch");
        colonyFeatures = new FeatureContainer();
        turn1 = new Turn(1);
    }

    // ------------------------------------------------------------------
    // Pre-condition helpers
    // ------------------------------------------------------------------

    private ProductionType bestProduction(BuildingType buildingType, GoodsType goodsType, boolean unattended) {
        return ProductionType.getBestProductionType(
                goodsType, buildingType.getAvailableProductionTypes(unattended));
    }

    private Object invokePrivate(Object target, String methodName, Class<?>[] paramTypes, Object... args)
            throws Exception {
        Method method = BuildingProductionCalculator.class.getDeclaredMethod(methodName, paramTypes);
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
    // M1: <init>(Player, FeatureContainer, int) — 1 Test Path (100% C2)
    // ------------------------------------------------------------------

    // M1-TP1: ENTRY -> N0 -> EXIT
    public void testM1_TP1_Constructor() {
        BuildingProductionCalculator calc = new BuildingProductionCalculator(dutchPlayer, colonyFeatures, 1);
        assertNotNull(calc);
    }

    // ------------------------------------------------------------------
    // M2: getAdjustedProductionInfo(...) — 100% C2
    // ------------------------------------------------------------------

    // M2-TP1: ENTRY -> N0 -(B1:false)-> N1 -> EXIT (buildingOutputs.isEmpty())
    public void testM2_TP1_GetAdjustedProductionInfoEmptyOutputs() {
        BuildingProductionCalculator calc = new BuildingProductionCalculator(dutchPlayer, colonyFeatures, 0);
        ProductionInfo pi = calc.getAdjustedProductionInfo(
                warehouseType, turn1, List.of(), List.of(), List.of(), 100);
        assertTrue(pi.getProduction().isEmpty());
        assertTrue(pi.getConsumption().isEmpty());
    }

    // M2-TP2: Normal building, sufficient inputs, no overflow (covers B2:true, B7, B8, B9:true, B10:true, B14:true, B15:true, B20, B21:true, B22, B23:true)
    public void testM2_TP2_GetAdjustedProductionInfoNormalSufficientInput() {
        BuildingProductionCalculator calc = new BuildingProductionCalculator(null, colonyFeatures, 0);
        ProductionType pt = bestProduction(weaverHouseType, cloth, false);
        List<WorkerAssignment> workers = List.of(new WorkerAssignment(freeColonist, pt));

        ProductionInfo pi = calc.getAdjustedProductionInfo(
                weaverHouseType, turn1, workers,
                List.of(new AbstractGoods(cotton, 50)),
                List.of(new AbstractGoods(cloth, 0)),
                100);

        assertFalse(pi.getProduction().isEmpty());
        assertEquals(3, pi.getProduction().get(0).getAmount());
        assertEquals(3, pi.getConsumption().get(0).getAmount());
        assertTrue(pi.getMaximumProduction().isEmpty());
    }

    // M2-TP3: Normal building, scarce input, no expert connections -> scales minimumRatio & adds maximumProduction/maximumConsumption
    // (covers B10:false, B11:true, B14:false, B21:false, B23:false)
    public void testM2_TP3_GetAdjustedProductionInfoScarceInputWithoutConnections() {
        BuildingProductionCalculator calc = new BuildingProductionCalculator(null, colonyFeatures, 0);
        ProductionType pt = bestProduction(weaverHouseType, cloth, false);
        List<WorkerAssignment> workers = List.of(new WorkerAssignment(freeColonist, pt));

        ProductionInfo pi = calc.getAdjustedProductionInfo(
                weaverHouseType, turn1, workers,
                List.of(new AbstractGoods(cotton, 1)),
                List.of(),
                100);

        assertEquals(1, pi.getProduction().get(0).getAmount());
        assertEquals(3, pi.getMaximumProduction().get(0).getAmount());
        assertEquals(1, pi.getConsumption().get(0).getAmount());
        assertEquals(3, pi.getMaximumConsumption().get(0).getAmount());
    }

    // M2-TP4: Factory with EXPERTS_USE_CONNECTIONS & expert worker, scarce input
    // (covers B11:false, B12:false, B13:false -> available = minimumGoodsInput)
    public void testM2_TP4_GetAdjustedProductionInfoExpertConnectionsApplied() {
        BuildingProductionCalculator calc = new BuildingProductionCalculator(null, colonyFeatures, 0);
        ProductionType pt = bestProduction(textileMillType, cloth, false);
        List<WorkerAssignment> workers = List.of(new WorkerAssignment(masterWeaver, pt));

        ProductionInfo pi = calc.getAdjustedProductionInfo(
                textileMillType, turn1, workers,
                List.of(new AbstractGoods(cotton, 0)),
                List.of(),
                100);

        assertFalse(pi.getProduction().get(0).getAmount() > 0);
    }

    // M2-TP5: Factory with EXPERTS_USE_CONNECTIONS, non-expert worker (minimumGoodsInput == 0 <= available)
    // (covers B12:false, B13:true)
    public void testM2_TP5_GetAdjustedProductionInfoNonExpertInFactory() {
        BuildingProductionCalculator calc = new BuildingProductionCalculator(null, colonyFeatures, 0);
        ProductionType pt = bestProduction(textileMillType, cloth, false);
        List<WorkerAssignment> workers = List.of(new WorkerAssignment(freeColonist, pt));

        ProductionInfo pi = calc.getAdjustedProductionInfo(
                textileMillType, turn1, workers,
                List.of(new AbstractGoods(cotton, 2)),
                List.of(),
                100);

        assertEquals(3, pi.getProduction().get(0).getAmount());
        assertFalse(pi.getMaximumProduction().isEmpty());
    }

    // M2-TP6: Auto-production building (country/stables), available >= warehouseCapacity -> 0
    // (covers B2:false, B3:false, B4:false)
    public void testM2_TP6_GetAdjustedProductionInfoAutoProductionAtCapacity() {
        BuildingProductionCalculator calc = new BuildingProductionCalculator(null, colonyFeatures, 0);
        ProductionInfo pi = calc.getAdjustedProductionInfo(
                countryType, turn1, List.of(),
                List.of(new AbstractGoods(grain, 50)),
                List.of(new AbstractGoods(horses, 100)),
                100);

        assertEquals(0, pi.getProduction().get(0).getAmount());
    }

    // M2-TP7: Auto-production building, available < breedingNumber -> production = 0, avoidOverflow skips <= 0
    // (covers B4:true, B5:true, B9:false, B15:false, B16:false, B17:false)
    public void testM2_TP7_GetAdjustedProductionInfoAutoProductionBelowBreedingNumber() {
        BuildingProductionCalculator calc = new BuildingProductionCalculator(null, colonyFeatures, 0);
        ProductionInfo pi = calc.getAdjustedProductionInfo(
                countryType, turn1, List.of(),
                List.of(new AbstractGoods(grain, 50)),
                List.of(new AbstractGoods(horses, 1)),
                100);

        assertEquals(0, pi.getProduction().get(0).getAmount());
    }

    // M2-TP8: Auto-production building, available >= breedingNumber, clamped by warehouse headroom
    // (covers B5:false, B6:true, B17:true, B18:false, B19:false)
    public void testM2_TP8_GetAdjustedProductionInfoAutoProductionBreedingAndOverflowClamp() {
        BuildingProductionCalculator calc = new BuildingProductionCalculator(null, colonyFeatures, 0);
        ProductionInfo pi = calc.getAdjustedProductionInfo(
                countryType, turn1, List.of(),
                List.of(new AbstractGoods(grain, 100)),
                List.of(new AbstractGoods(horses, 99)),
                100);

        assertEquals(1, pi.getProduction().get(0).getAmount());
    }

    // M2-TP9: avoidOverflow == true, production <= headroom (no clamp)
    // (covers B15:false, B17:true, B18:true, B19:true)
    public void testM2_TP9_GetAdjustedProductionInfoAvoidOverflowNoClamp() {
        BuildingProductionCalculator calc = new BuildingProductionCalculator(null, colonyFeatures, 0);
        ProductionType pt = bestProduction(townHallType, bells, false);
        List<WorkerAssignment> workers = List.of(new WorkerAssignment(freeColonist, pt));

        ProductionInfo pi = calc.getAdjustedProductionInfo(
                townHallType, turn1, workers,
                List.of(),
                List.of(new AbstractGoods(bells, 0)),
                100);

        assertTrue(pi.getProduction().get(0).getAmount() > 0);
    }

    // ------------------------------------------------------------------
    // M3: getExpertUnitType(BuildingType) — 2 Test Paths (100% C2)
    // ------------------------------------------------------------------

    // M3-TP1: ENTRY -> N0 -(B1:false)-> N1 -> N3 -> EXIT (pt == null)
    public void testM3_TP1_GetExpertUnitTypeNullProductionType() {
        BuildingProductionCalculator calc = new BuildingProductionCalculator(null, colonyFeatures, 0);
        assertNull(calc.getExpertUnitType(warehouseType));
    }

    // M3-TP2: ENTRY -> N0 -(B1:true)-> N2 -> N3 -> EXIT (pt != null, also covers lambda$getExpertUnitType$0)
    public void testM3_TP2_GetExpertUnitTypeValid() {
        BuildingProductionCalculator calc = new BuildingProductionCalculator(null, colonyFeatures, 0);
        assertEquals(masterWeaver, calc.getExpertUnitType(weaverHouseType));
    }

    // ------------------------------------------------------------------
    // M4: getBestProductionType(BuildingType) — 1 Test Path (100% C2)
    // ------------------------------------------------------------------

    // M4-TP1: ENTRY -> N0 -> EXIT
    public void testM4_TP1_GetBestProductionType() throws Exception {
        BuildingProductionCalculator calc = new BuildingProductionCalculator(null, colonyFeatures, 0);
        ProductionType pt = (ProductionType) invokePrivate(
                calc, "getBestProductionType",
                new Class<?>[]{BuildingType.class},
                weaverHouseType);
        assertNotNull(pt);
    }

    // ------------------------------------------------------------------
    // M5: getOutputs(BuildingType, List) — 2 Test Paths (100% C2)
    // ------------------------------------------------------------------

    // M5-TP1 & M5-TP2: empty outputs vs aggregated multiple worker outputs (covers B1 & B2, lambdas $0, $1, $2)
    @SuppressWarnings("unchecked")
    public void testM5_GetOutputsEmptyAndAggregated() throws Exception {
        BuildingProductionCalculator calc = new BuildingProductionCalculator(null, colonyFeatures, 0);

        List<AbstractGoods> empty = (List<AbstractGoods>) invokePrivate(
                calc, "getOutputs",
                new Class<?>[]{BuildingType.class, List.class},
                warehouseType, List.of());
        assertTrue(empty.isEmpty());

        ProductionType pt = bestProduction(weaverHouseType, cloth, false);
        List<WorkerAssignment> twoWorkers = List.of(
                new WorkerAssignment(freeColonist, pt),
                new WorkerAssignment(freeColonist, pt));
        List<AbstractGoods> aggregated = (List<AbstractGoods>) invokePrivate(
                calc, "getOutputs",
                new Class<?>[]{BuildingType.class, List.class},
                weaverHouseType, twoWorkers);
        assertEquals(1, aggregated.size());
        assertEquals(6, aggregated.get(0).getAmount());
    }

    // ------------------------------------------------------------------
    // M6: getInputs(BuildingType, List) — 2 Test Paths (100% C2)
    // ------------------------------------------------------------------

    // M6-TP1 & M6-TP2: empty inputs vs aggregated multiple worker inputs (covers B1 & B2, lambdas $0, $1, $2)
    @SuppressWarnings("unchecked")
    public void testM6_GetInputsEmptyAndAggregated() throws Exception {
        BuildingProductionCalculator calc = new BuildingProductionCalculator(null, colonyFeatures, 0);

        List<AbstractGoods> empty = (List<AbstractGoods>) invokePrivate(
                calc, "getInputs",
                new Class<?>[]{BuildingType.class, List.class},
                warehouseType, List.of());
        assertTrue(empty.isEmpty());

        ProductionType pt = bestProduction(weaverHouseType, cloth, false);
        List<WorkerAssignment> twoWorkers = List.of(
                new WorkerAssignment(freeColonist, pt),
                new WorkerAssignment(freeColonist, pt));
        List<AbstractGoods> aggregated = (List<AbstractGoods>) invokePrivate(
                calc, "getInputs",
                new Class<?>[]{BuildingType.class, List.class},
                weaverHouseType, twoWorkers);
        assertEquals(1, aggregated.size());
        assertEquals(6, aggregated.get(0).getAmount());
    }

    // ------------------------------------------------------------------
    // M7: determineProduction(...) & M8: getAvailable(...) — 100% C2
    // ------------------------------------------------------------------

    // M7-TP1: ENTRY -> N0 -> EXIT (also covers lambda$determineProduction$0)
    // M8-TP1: ENTRY -> N0 -> EXIT
    public void testM7_M8_DetermineProductionAndGetAvailable() throws Exception {
        BuildingProductionCalculator calc = new BuildingProductionCalculator(null, colonyFeatures, 0);
        ProductionType pt = bestProduction(weaverHouseType, cloth, false);
        List<WorkerAssignment> workers = List.of(new WorkerAssignment(freeColonist, pt));

        int prod = (int) invokePrivate(
                calc, "determineProduction",
                new Class<?>[]{BuildingType.class, List.class, Turn.class, GoodsType.class},
                weaverHouseType, workers, turn1, cloth);
        assertEquals(3, prod);

        int available = (int) invokePrivate(
                calc, "getAvailable",
                new Class<?>[]{GoodsType.class, List.class},
                cotton, List.of(new AbstractGoods(cotton, 12)));
        assertEquals(12, available);
    }

    // ------------------------------------------------------------------
    // M9: getUnitProduction(...) — 3 Test Paths (100% C2)
    // ------------------------------------------------------------------

    // Covers M9-TP1..TP3 and lambda$getUnitProduction$0
    public void testM9_GetUnitProductionAllBranches() throws Exception {
        BuildingProductionCalculator calc = new BuildingProductionCalculator(null, colonyFeatures, 0);
        ProductionType pt = bestProduction(weaverHouseType, cloth, false);

        // M9-TP1: ENTRY -> N0 -(B1:true)-> N2 -> EXIT (workerAssignment == null)
        assertEquals(0, (int) invokePrivate(
                calc, "getUnitProduction",
                new Class<?>[]{Turn.class, BuildingType.class, WorkerAssignment.class, GoodsType.class},
                turn1, weaverHouseType, null, cloth));

        // M9-TP2: ENTRY -> N0 -(B1:false)-> N1 -(B2:false)-> N2 -> EXIT (outputs.noneMatch == true)
        assertEquals(0, (int) invokePrivate(
                calc, "getUnitProduction",
                new Class<?>[]{Turn.class, BuildingType.class, WorkerAssignment.class, GoodsType.class},
                turn1, weaverHouseType, new WorkerAssignment(freeColonist, pt), bells));

        // M9-TP3: ENTRY -> N0 -(B1:false)-> N1 -(B2:true)-> N3 -> EXIT (valid)
        assertEquals(3, (int) invokePrivate(
                calc, "getUnitProduction",
                new Class<?>[]{Turn.class, BuildingType.class, WorkerAssignment.class, GoodsType.class},
                turn1, weaverHouseType, new WorkerAssignment(freeColonist, pt), cloth));
    }

    // ------------------------------------------------------------------
    // M10: getBaseProduction(...) — 2 Test Paths (100% C2)
    // ------------------------------------------------------------------

    // Covers M10-TP1..TP2
    public void testM10_GetBaseProductionAllBranches() throws Exception {
        BuildingProductionCalculator calc = new BuildingProductionCalculator(null, colonyFeatures, 0);
        ProductionType pt = bestProduction(weaverHouseType, cloth, false);

        // M10-TP1: ENTRY -> N0 -(B1:false)-> N1 -> N3 -> EXIT (buildingType == null)
        assertEquals(0, (int) invokePrivate(
                calc, "getBaseProduction",
                new Class<?>[]{BuildingType.class, ProductionType.class, GoodsType.class, UnitType.class},
                null, pt, cloth, freeColonist));

        // M10-TP2: ENTRY -> N0 -(B1:true)-> N2 -> N3 -> EXIT (buildingType != null)
        assertEquals(3, (int) invokePrivate(
                calc, "getBaseProduction",
                new Class<?>[]{BuildingType.class, ProductionType.class, GoodsType.class, UnitType.class},
                weaverHouseType, pt, cloth, freeColonist));
    }

    // ------------------------------------------------------------------
    // M11: getProductionModifiers(...) — 4 Test Paths (100% C2)
    // ------------------------------------------------------------------

    // Covers M11-TP1..TP4
    @SuppressWarnings("unchecked")
    public void testM11_GetProductionModifiersAllPaths() throws Exception {
        BuildingProductionCalculator calcNoOwner = new BuildingProductionCalculator(null, colonyFeatures, 0);
        BuildingProductionCalculator calcWithOwner = new BuildingProductionCalculator(dutchPlayer, colonyFeatures, 1);

        // M11-TP1: goodsType == null, unitType == null, owner == null
        List<Modifier> m1 = ((Stream<Modifier>) invokePrivate(
                calcNoOwner, "getProductionModifiers",
                new Class<?>[]{Turn.class, BuildingType.class, GoodsType.class, UnitType.class},
                turn1, weaverHouseType, null, null)).collect(Collectors.toList());
        assertNotNull(m1);

        // M11-TP2: goodsType != null, unitType == null, owner != null
        List<Modifier> m2 = ((Stream<Modifier>) invokePrivate(
                calcWithOwner, "getProductionModifiers",
                new Class<?>[]{Turn.class, BuildingType.class, GoodsType.class, UnitType.class},
                turn1, weaverHouseType, cloth, null)).collect(Collectors.toList());
        assertNotNull(m2);

        // M11-TP3: goodsType != null, unitType != null, owner == null
        List<Modifier> m3 = ((Stream<Modifier>) invokePrivate(
                calcNoOwner, "getProductionModifiers",
                new Class<?>[]{Turn.class, BuildingType.class, GoodsType.class, UnitType.class},
                turn1, weaverHouseType, cloth, freeColonist)).collect(Collectors.toList());
        assertNotNull(m3);

        // M11-TP4: goodsType != null, unitType != null, owner != null
        List<Modifier> m4 = ((Stream<Modifier>) invokePrivate(
                calcWithOwner, "getProductionModifiers",
                new Class<?>[]{Turn.class, BuildingType.class, GoodsType.class, UnitType.class},
                turn1, weaverHouseType, cloth, freeColonist)).collect(Collectors.toList());
        assertFalse(m4.isEmpty());
    }
}
