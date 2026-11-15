package net.sf.freecol.common.model.production;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import net.sf.freecol.common.model.BuildingType;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.GoodsType;
import net.sf.freecol.common.model.Modifier;
import net.sf.freecol.common.model.Resource;
import net.sf.freecol.common.model.ResourceType;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.TileType;
import net.sf.freecol.common.model.UnitType;
import net.sf.freecol.util.test.FreeColTestCase;


/**
 * CFG-driven test suite for {@link ProductionUtils} derived from
 * {@code test_cft/graph/net/sf/freecol/common/model/production/ProductionUtils.json}.
 *
 * Covers all 5 methods (private constructor, public static methods, private helper,
 * and compiler-generated lambda) and achieves 100% C2 (node + edge) coverage on all
 * feasible edges (edge N7->N11 in {@code getRebelProductionModifiersForTile} is
 * infeasible because {@code unitType == null} already returns at line 56).
 */
public class ProductionUtilsCfgTest extends FreeColTestCase {

    private static final BuildingType townHallType = spec().getBuildingType("model.building.townHall");

    private static final GoodsType grain = spec().getGoodsType("model.goods.grain");
    private static final GoodsType cotton = spec().getGoodsType("model.goods.cotton");
    private static final GoodsType bells = spec().getGoodsType("model.goods.bells");

    private static final TileType plains = spec().getTileType("model.tile.plains");
    private static final ResourceType grainResource = spec().getResourceType("model.resource.grain");

    private static final UnitType freeColonist = spec().getUnitType("model.unit.freeColonist");
    private static final UnitType expertFarmer = spec().getUnitType("model.unit.expertFarmer");

    private Game testGame;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        // Pre-condition: standard game.
        testGame = getStandardGame();
    }

    // ------------------------------------------------------------------
    // Pre-condition helpers
    // ------------------------------------------------------------------

    private Object invokePrivateStatic(String methodName, Class<?>[] paramTypes, Object... args)
            throws Exception {
        Method method = ProductionUtils.class.getDeclaredMethod(methodName, paramTypes);
        method.setAccessible(true);
        try {
            return method.invoke(null, args);
        } catch (InvocationTargetException e) {
            if (e.getCause() instanceof RuntimeException) {
                throw (RuntimeException) e.getCause();
            }
            throw e;
        }
    }

    // ------------------------------------------------------------------
    // M1: <init>() — 1 Test Path (100% C2)
    // ------------------------------------------------------------------

    // M1-TP1: ENTRY -> N0 -> EXIT
    public void testM1_TP1_PrivateConstructor() throws Exception {
        Constructor<ProductionUtils> ctor = ProductionUtils.class.getDeclaredConstructor();
        ctor.setAccessible(true);
        ProductionUtils instance = ctor.newInstance();
        assertNotNull(instance);
    }

    // ------------------------------------------------------------------
    // M2: getRebelProductionModifiersForBuilding(...) — 3 Test Paths (100% C2)
    // ------------------------------------------------------------------

    // M2-TP1: ENTRY -> N0 -(B1:false)-> N1 -> EXIT (colonyProductionBonus == 0)
    public void testM2_TP1_BuildingBonusZero() {
        List<Modifier> mods = ProductionUtils
                .getRebelProductionModifiersForBuilding(townHallType, 0, bells, freeColonist)
                .collect(Collectors.toList());
        assertTrue(mods.isEmpty());
    }

    // M2-TP2: ENTRY -> N0 -(B1:true)-> N2 -(B2:true)-> N4 -> N5 -> EXIT (buildingType == null)
    public void testM2_TP2_BuildingNullType() {
        List<Modifier> mods = ProductionUtils
                .getRebelProductionModifiersForBuilding(null, 2, bells, freeColonist)
                .collect(Collectors.toList());
        assertEquals(1, mods.size());
        assertEquals(2.0f, mods.get(0).getValue(), 0.001f);
    }

    // M2-TP3: ENTRY -> N0 -(B1:true)-> N2 -(B2:false)-> N3 -> N5 -> EXIT (buildingType != null)
    public void testM2_TP3_BuildingNonNullType() {
        List<Modifier> mods = ProductionUtils
                .getRebelProductionModifiersForBuilding(townHallType, 2, bells, freeColonist)
                .collect(Collectors.toList());
        assertEquals(1, mods.size());
    }

    // ------------------------------------------------------------------
    // M3: getRebelProductionModifiersForTile(...) — 6 Test Paths (100% feasible C2)
    // ------------------------------------------------------------------

    // M3-TP1: ENTRY -> N0 -(B1:false)-> N1 -> EXIT (colonyProductionBonus == 0)
    public void testM3_TP1_TileBonusZero() {
        Tile tile = new Tile(testGame, plains, 0, 0);
        List<Modifier> mods = ProductionUtils
                .getRebelProductionModifiersForTile(tile, 0, grain, freeColonist)
                .collect(Collectors.toList());
        assertTrue(mods.isEmpty());
    }

    // M3-TP2: ENTRY -> N0 -(B1:true)-> N2 -(B2:false)-> N3 -(B3:false)-> N4 -> EXIT
    // (unitType == null && colonyProductionBonus < 0)
    public void testM3_TP2_TileUnattendedNegativeBonus() {
        Tile tile = new Tile(testGame, plains, 0, 0);
        List<Modifier> mods = ProductionUtils
                .getRebelProductionModifiersForTile(tile, -1, grain, null)
                .collect(Collectors.toList());
        assertTrue(mods.isEmpty());
    }

    // M3-TP3: ENTRY -> N0 -(B1:true)-> N2 -(B2:false)-> N3 -(B3:true)-> N5 -(B4:false)-> N6 -> EXIT
    // (unitType == null && colonyProductionBonus > 0)
    public void testM3_TP3_TileUnattendedPositiveBonus() {
        Tile tile = new Tile(testGame, plains, 0, 0);
        List<Modifier> mods = ProductionUtils
                .getRebelProductionModifiersForTile(tile, 2, grain, null)
                .collect(Collectors.toList());
        assertEquals(1, mods.size());
        assertEquals(2.0f, mods.get(0).getValue(), 0.001f);
    }

    // M3-TP4: ENTRY -> N0 -> N2 -(B2:true)-> N5 -(B4:true)-> N7 -(B5:false)-> N8 -(B6:true)-> N11 -(B8:true)-> N14 -> EXIT
    // (unitType != null, expertProduction == null, tile == null)
    public void testM3_TP4_TileNullWithNonExpertUnit() {
        List<Modifier> mods = ProductionUtils
                .getRebelProductionModifiersForTile(null, 2, grain, freeColonist)
                .collect(Collectors.toList());
        assertEquals(1, mods.size());
        assertEquals(2.0f, mods.get(0).getValue(), 0.001f);
    }

    // M3-TP5: ENTRY -> N0 -> N2 -> N5 -> N7 -> N8 -(B6:false)-> N9 -(B7:true)-> N11 -(B8:false)-> N12 -(B9:true)-> N14 -> EXIT
    // (unitType expertProduction != goodsType, tile != null, resource == null)
    public void testM3_TP5_TileNoResourceWithMismatchedExpert() {
        Tile tile = new Tile(testGame, plains, 0, 0);
        List<Modifier> mods = ProductionUtils
                .getRebelProductionModifiersForTile(tile, 2, cotton, expertFarmer)
                .collect(Collectors.toList());
        assertEquals(1, mods.size());
        assertEquals(2.0f, mods.get(0).getValue(), 0.001f);
    }

    // M3-TP6: ENTRY -> N0 -> N2 -> N5 -> N7 -> N8 -> N9 -(B7:false)-> N10 -> N11 -(B8:false)-> N12 -(B9:false)-> N13 -> N14 -> EXIT
    // (unitType expertProduction == goodsType, tile != null, resource != null; also covers lambda$getRebelProductionModifiersForTile$0)
    public void testM3_TP6_TileWithResourceAndMatchingExpert() {
        Tile tile = new Tile(testGame, plains, 0, 0);
        tile.addResource(new Resource(testGame, tile, grainResource));
        List<Modifier> mods = ProductionUtils
                .getRebelProductionModifiersForTile(tile, 2, grain, expertFarmer)
                .collect(Collectors.toList());
        assertEquals(1, mods.size());
        assertEquals(4.0f, mods.get(0).getValue(), 0.001f);
    }

    // ------------------------------------------------------------------
    // M4: createRebelProductionModifierStream(int, GoodsType, int) — 1 Test Path (100% C2)
    // ------------------------------------------------------------------

    // M4-TP1: ENTRY -> N0 -> EXIT
    @SuppressWarnings("unchecked")
    public void testM4_TP1_CreateRebelProductionModifierStream() throws Exception {
        Stream<Modifier> stream = (Stream<Modifier>) invokePrivateStatic(
                "createRebelProductionModifierStream",
                new Class<?>[]{int.class, GoodsType.class, int.class},
                Modifier.COLONYTILE_PRODUCTION_INDEX, grain, 3);
        List<Modifier> mods = stream.collect(Collectors.toList());
        assertEquals(1, mods.size());
        assertEquals(3.0f, mods.get(0).getValue(), 0.001f);
    }
}
