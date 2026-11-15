package net.sf.freecol.common.model;

import java.io.StringReader;
import java.io.StringWriter;
import java.lang.reflect.Method;
import java.util.List;
import java.util.stream.Collectors;

import net.sf.freecol.common.io.FreeColXMLReader;
import net.sf.freecol.common.io.FreeColXMLWriter;
import net.sf.freecol.common.model.Constants.IntegrityType;
import net.sf.freecol.common.model.Map.Layer;
import net.sf.freecol.common.util.LogBuilder;
import net.sf.freecol.util.test.FreeColTestCase;


/**
 * CFG-driven test suite for {@link Resource} derived from
 * {@code test_cft/graph/net/sf/freecol/common/model/Resource.json}.
 *
 * Covers all 26 methods (M1..M26) and achieves 100% feasible C3 (node + edge)
 * coverage across 38 Test Paths.
 *
 * Infeasible edges documented:
 * - M21 ({@code copyIn(FreeColObject)}): {@code N1 -> N2} ({@code B2:false}) —
 *   once {@code copyInCast(other, Resource.class)} returns a non-null {@code Resource o},
 *   {@code TileItem.copyIn(o)} delegates to {@code FreeColGameObject.copyIn(o)} and
 *   {@code FreeColObject.copyIn(o)}, which always return {@code true} for non-null input.
 */
public class ResourceCfgTest extends FreeColTestCase {

    private Game game;
    private Map map;
    private Tile tile;
    private ResourceType silverResource;
    private ResourceType grainResource;
    private GoodsType silverGoods;
    private GoodsType grainGoods;
    private UnitType colonistType;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        // Pre-condition: standard game with test map, silver/grain resource types, goods types, and unit type
        game = getStandardGame();
        map = getTestMap();
        game.changeMap(map);
        tile = map.getTile(0, 0);
        silverResource = spec().getResourceType("model.resource.silver");
        grainResource = spec().getResourceType("model.resource.grain");
        silverGoods = spec().getGoodsType("model.goods.silver");
        grainGoods = spec().getGoodsType("model.goods.grain");
        colonistType = spec().getUnitType("model.unit.freeColonist");
    }

    // ------------------------------------------------------------------
    // Pre-condition helpers & stubs
    // ------------------------------------------------------------------

    private int invokePrivateUseQuantity(Resource resource, int usedQuantity) throws Exception {
        Method m = Resource.class.getDeclaredMethod("useQuantity", int.class);
        m.setAccessible(true);
        return (Integer) m.invoke(resource, usedQuantity);
    }

    private String serializeToXml(Resource resource) throws Exception {
        StringWriter sw = new StringWriter();
        try (FreeColXMLWriter xw = new FreeColXMLWriter(sw, FreeColXMLWriter.WriteScope.toSave())) {
            resource.toXML(xw);
        }
        return sw.toString();
    }

    // ------------------------------------------------------------------
    // M1: Resource(Game, Tile, ResourceType, int) — 2 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M1-TP1: ENTRY -> N0 -(B1:true)-> N2 -> EXIT (type != null: initializes type and quantity)
    public void testM1_TP1_ConstructorValidType() {
        Resource resource = new Resource(game, tile, silverResource, 250);
        assertSame(silverResource, resource.getType());
        assertEquals(250, resource.getQuantity());
    }

    // M1-TP2: ENTRY -> N0 -(B1:false)-> N1 -> EXIT (type == null: throws RuntimeException)
    public void testM1_TP2_ConstructorNullTypeThrows() {
        try {
            new Resource(game, tile, null, 100);
            fail("Expected RuntimeException for null ResourceType");
        } catch (RuntimeException expected) {
            assertTrue(expected.getMessage().contains("Type must not be null"));
        }
    }

    // ------------------------------------------------------------------
    // M2: Resource(Game, Tile, ResourceType) — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M2-TP1: ENTRY -> N0 -> EXIT (delegates to 4-arg constructor with type.getMaxValue())
    public void testM2_TP1_ConstructorDefaultQuantity() {
        Resource resource = new Resource(game, tile, silverResource);
        assertSame(silverResource, resource.getType());
        assertEquals(silverResource.getMaxValue(), resource.getQuantity());
    }

    // ------------------------------------------------------------------
    // M3: Resource(Game, String) — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M3-TP1: ENTRY -> N0 -> EXIT (initializes Resource by id via super(game, id))
    public void testM3_TP1_ConstructorWithId() {
        Resource resource = new Resource(game, "resource:1");
        assertEquals("resource:1", resource.getId());
        assertNull(resource.getType());
    }

    // ------------------------------------------------------------------
    // M4: getType() — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M4-TP1: ENTRY -> N0 -> EXIT (returns configured ResourceType)
    public void testM4_TP1_GetType() {
        Resource resource = new Resource(game, tile, grainResource, -1);
        assertSame(grainResource, resource.getType());
    }

    // ------------------------------------------------------------------
    // M5: isUnlimited() — 2 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M5-TP1: ENTRY -> N0 -(B1:true)-> N2 -> N3 -> EXIT (quantity != -1 -> false)
    public void testM5_TP1_IsUnlimitedFalse() {
        Resource resource = new Resource(game, tile, silverResource, 100);
        assertFalse(resource.isUnlimited());
    }

    // M5-TP2: ENTRY -> N0 -(B1:false)-> N1 -> N3 -> EXIT (quantity == -1 -> true)
    public void testM5_TP2_IsUnlimitedTrue() {
        Resource resource = new Resource(game, tile, grainResource, -1);
        assertTrue(resource.isUnlimited());
    }

    // ------------------------------------------------------------------
    // M6: getQuantity() — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M6-TP1: ENTRY -> N0 -> EXIT (returns current quantity)
    public void testM6_TP1_GetQuantity() {
        Resource resource = new Resource(game, tile, silverResource, 42);
        assertEquals(42, resource.getQuantity());
    }

    // ------------------------------------------------------------------
    // M7: setQuantity(int) — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M7-TP1: ENTRY -> N0 -> EXIT (updates quantity)
    public void testM7_TP1_SetQuantity() {
        Resource resource = new Resource(game, tile, silverResource, 42);
        resource.setQuantity(99);
        assertEquals(99, resource.getQuantity());
    }

    // ------------------------------------------------------------------
    // M8: getBestGoodsType() — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M8-TP1: ENTRY -> N0 -> EXIT (delegates to type.getBestGoodsType())
    public void testM8_TP1_GetBestGoodsType() {
        Resource resource = new Resource(game, tile, silverResource, 100);
        assertSame(silverGoods, resource.getBestGoodsType());
    }

    // ------------------------------------------------------------------
    // M9: useQuantity(GoodsType, UnitType, int) — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M9-TP1: ENTRY -> N0 -> EXIT (computes bonus delta and delegates to useQuantity(int))
    public void testM9_TP1_UseQuantityWithGoodsAndUnit() {
        Resource resource = new Resource(game, tile, silverResource, 100);
        int remaining = resource.useQuantity(silverGoods, colonistType, 2);
        assertEquals(98, remaining);
    }

    // ------------------------------------------------------------------
    // M10: useQuantity(int) — 3 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M10-TP1: ENTRY -> N0 -(B1:true)-> N2 -(B2:true)-> N4 -> N5 -> EXIT (quantity != -1 && quantity < usedQuantity -> clamps to 0)
    public void testM10_TP1_UseQuantityInsufficientClampsToZero() throws Exception {
        Resource resource = new Resource(game, tile, silverResource, 3);
        int remaining = invokePrivateUseQuantity(resource, 10);
        assertEquals(0, remaining);
    }

    // M10-TP2: ENTRY -> N0 -(B1:true)-> N2 -(B2:false)-> N3 -> N5 -> EXIT (quantity != -1 && quantity >= usedQuantity -> subtracts)
    public void testM10_TP2_UseQuantitySufficientSubtracts() throws Exception {
        Resource resource = new Resource(game, tile, silverResource, 10);
        int remaining = invokePrivateUseQuantity(resource, 4);
        assertEquals(6, remaining);
    }

    // M10-TP3: ENTRY -> N0 -(B1:false)-> N1 -> N5 -> EXIT (quantity == -1 -> unlimited, unchanged)
    public void testM10_TP3_UseQuantityUnlimitedUnchanged() throws Exception {
        Resource resource = new Resource(game, tile, grainResource, -1);
        int remaining = invokePrivateUseQuantity(resource, 5);
        assertEquals(-1, remaining);
    }

    // ------------------------------------------------------------------
    // M11: getNameKey() — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M11-TP1: ENTRY -> N0 -> EXIT (returns type.getNameKey())
    public void testM11_TP1_GetNameKey() {
        Resource resource = new Resource(game, tile, silverResource, 100);
        assertEquals(silverResource.getNameKey(), resource.getNameKey());
    }

    // ------------------------------------------------------------------
    // M12: getZIndex() — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M12-TP1: ENTRY -> N0 -> EXIT (returns Tile.RESOURCE_ZINDEX)
    public void testM12_TP1_GetZIndex() {
        Resource resource = new Resource(game, tile, silverResource, 100);
        assertEquals(Tile.RESOURCE_ZINDEX, resource.getZIndex());
    }

    // ------------------------------------------------------------------
    // M13: isTileTypeAllowed(TileType) — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M13-TP1: ENTRY -> N0 -> EXIT (delegates to tileType.canHaveResourceType(getType()))
    public void testM13_TP1_IsTileTypeAllowed() {
        Resource resource = new Resource(game, tile, silverResource, 100);
        TileType mountains = spec().getTileType("model.tile.mountains");
        assertEquals(mountains.canHaveResourceType(silverResource), resource.isTileTypeAllowed(mountains));
    }

    // ------------------------------------------------------------------
    // M14: applyBonus(GoodsType, UnitType, int) — 3 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M14-TP1: ENTRY -> N0 -(B1:true)-> N2 -> N4 -> EXIT (quantity == -1 -> returns potential + amount)
    public void testM14_TP1_ApplyBonusUnlimited() {
        Resource resource = new Resource(game, tile, grainResource, -1);
        int result = resource.applyBonus(grainGoods, colonistType, 3);
        assertEquals(5, result);
    }

    // M14-TP2: ENTRY -> N0 -(B1:false)-> N1 -(B2:true)-> N3 -> N4 -> EXIT (quantity != -1 && quantity <= amount -> caps at quantity)
    public void testM14_TP2_ApplyBonusCappedByQuantity() {
        Resource resource = new Resource(game, tile, silverResource, 1);
        int result = resource.applyBonus(silverGoods, colonistType, 2);
        assertEquals(3, result);
    }

    // M14-TP3: ENTRY -> N0 -(B1:false)-> N1 -(B2:false)-> N2 -> N4 -> EXIT (quantity != -1 && quantity > amount -> adds full amount)
    public void testM14_TP3_ApplyBonusFullAmountWhenQuantityGreater() {
        Resource resource = new Resource(game, tile, silverResource, 50);
        int result = resource.applyBonus(silverGoods, colonistType, 2);
        assertEquals(4, result);
    }

    // ------------------------------------------------------------------
    // M15: canProduce(GoodsType, UnitType) — 3 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M15-TP1: ENTRY -> N0 -(B1:true)-> N2 -(B2:true)-> N4 -> N5 -> EXIT (goodsType != null, modifier result <= 0 -> false)
    public void testM15_TP1_CanProduceNonMatchingGoodsFalse() {
        Resource resource = new Resource(game, tile, silverResource, 100);
        assertFalse(resource.canProduce(grainGoods, colonistType));
    }

    // M15-TP2: ENTRY -> N0 -(B1:true)-> N2 -(B2:false)-> N3 -> N5 -> EXIT (goodsType != null, modifier result > 0 -> true)
    public void testM15_TP2_CanProduceMatchingGoodsTrue() {
        Resource resource = new Resource(game, tile, silverResource, 100);
        assertTrue(resource.canProduce(silverGoods, colonistType));
    }

    // M15-TP3: ENTRY -> N0 -(B1:false)-> N1 -> EXIT (goodsType == null -> false)
    public void testM15_TP3_CanProduceNullGoodsFalse() {
        Resource resource = new Resource(game, tile, silverResource, 100);
        assertFalse(resource.canProduce(null, colonistType));
    }

    // ------------------------------------------------------------------
    // M16: getProductionModifiers(GoodsType, UnitType) — 2 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M16-TP1: ENTRY -> N0 -(B1:true)-> N2 -> N3 -> EXIT (goodsType != null -> returns type modifiers)
    public void testM16_TP1_GetProductionModifiersNonNull() {
        Resource resource = new Resource(game, tile, silverResource, 100);
        List<Modifier> mods = resource.getProductionModifiers(silverGoods, colonistType)
                .collect(Collectors.toList());
        assertFalse(mods.isEmpty());
    }

    // M16-TP2: ENTRY -> N0 -(B1:false)-> N1 -> N3 -> EXIT (goodsType == null -> returns empty stream)
    public void testM16_TP2_GetProductionModifiersNull() {
        Resource resource = new Resource(game, tile, silverResource, 100);
        long count = resource.getProductionModifiers(null, colonistType).count();
        assertEquals(0L, count);
    }

    // ------------------------------------------------------------------
    // M17: isNatural() — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M17-TP1: ENTRY -> N0 -> EXIT (always returns true)
    public void testM17_TP1_IsNatural() {
        Resource resource = new Resource(game, tile, silverResource, 100);
        assertTrue(resource.isNatural());
    }

    // ------------------------------------------------------------------
    // M18: isComplete() — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M18-TP1: ENTRY -> N0 -> EXIT (always returns true)
    public void testM18_TP1_IsComplete() {
        Resource resource = new Resource(game, tile, silverResource, 100);
        assertTrue(resource.isComplete());
    }

    // ------------------------------------------------------------------
    // M19: getLayer() — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M19-TP1: ENTRY -> N0 -> EXIT (returns Layer.RESOURCES)
    public void testM19_TP1_GetLayer() {
        Resource resource = new Resource(game, tile, silverResource, 100);
        assertEquals(Layer.RESOURCES, resource.getLayer());
    }

    // ------------------------------------------------------------------
    // M20: checkIntegrity(boolean, LogBuilder) — 2 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M20-TP1: ENTRY -> N0 -(B1:true)-> N2 -> EXIT (type != null -> OK)
    public void testM20_TP1_CheckIntegrityValid() {
        Resource resource = new Resource(game, tile, silverResource, 100);
        LogBuilder lb = new LogBuilder(64);
        assertEquals(IntegrityType.INTEGRITY_GOOD, resource.checkIntegrity(false, lb));
    }

    // M20-TP2: ENTRY -> N0 -(B1:false)-> N1 -> EXIT (type == null -> FAIL)
    public void testM20_TP2_CheckIntegrityNullTypeFails() {
        Resource resource = new Resource(game, "resource:notype");
        LogBuilder lb = new LogBuilder(64);
        assertEquals(IntegrityType.INTEGRITY_FAIL, resource.checkIntegrity(false, lb));
        assertTrue(lb.toString().contains("Resource without type"));
    }

    // ------------------------------------------------------------------
    // M21: copyIn(FreeColObject) — 2 feasible Test Paths (100% feasible C3)
    // Infeasible: N1 -> N2 (B2:false, !super.copyIn(o))
    // ------------------------------------------------------------------

    // M21-TP1: ENTRY -> N0 -(B1:true)-> N2 -> EXIT (other is null / non-Resource -> returns false)
    public void testM21_TP1_CopyInNullOrWrongTypeReturnsFalse() {
        Resource target = new Resource(game, tile, silverResource, 100);
        assertFalse(target.copyIn(null));
        assertFalse(target.copyIn(tile));
    }

    // M21-TP2: ENTRY -> N0 -(B1:false)-> N1 -(B2:true)-> N3 -> EXIT (valid Resource -> copies type and quantity)
    public void testM21_TP2_CopyInValidResourceCopiesFields() {
        Resource target = new Resource(game, tile, silverResource, 100);
        Game game2 = getStandardGame();
        Map map2 = getTestMap();
        game2.setMap(map2);
        Resource source = new Resource(game2, map2.getTile(0, 0), grainResource, -1);
        source.setId(target.getId());
        assertTrue(target.copyIn(source));
        assertSame(grainResource, target.getType());
        assertEquals(-1, target.getQuantity());
    }

    // ------------------------------------------------------------------
    // M22: writeAttributes(FreeColXMLWriter) — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M22-TP1: ENTRY -> N0 -> EXIT (writes tile, type, quantity attributes via toXML)
    public void testM22_TP1_WriteAttributes() throws Exception {
        Resource resource = new Resource(game, tile, silverResource, 350);
        String xml = serializeToXml(resource);
        assertTrue(xml.contains("type=\"model.resource.silver\""));
        assertTrue(xml.contains("quantity=\"350\""));
    }

    // ------------------------------------------------------------------
    // M23: readAttributes(FreeColXMLReader) — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M23-TP1: ENTRY -> N0 -> EXIT (reads tile, type, quantity attributes via readFromXML)
    public void testM23_TP1_ReadAttributes() throws Exception {
        Resource original = new Resource(game, tile, silverResource, 350);
        String xml = serializeToXml(original).replace(original.getId(), "resource:restored");
        Resource restored = new Resource(game, "resource:restored");
        try (FreeColXMLReader xr = new FreeColXMLReader(new StringReader(xml))) {
            xr.nextTag();
            restored.readFromXML(xr);
        }
        assertSame(tile, restored.getTile());
        assertSame(silverResource, restored.getType());
        assertEquals(350, restored.getQuantity());
    }

    // ------------------------------------------------------------------
    // M24: getXMLTagName() — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M24-TP1: ENTRY -> N0 -> EXIT (returns Resource.TAG)
    public void testM24_TP1_GetXMLTagName() {
        Resource resource = new Resource(game, tile, silverResource, 100);
        assertEquals(Resource.TAG, resource.getXMLTagName());
    }

    // ------------------------------------------------------------------
    // M25: toString() — 4 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M25-TP1: ENTRY -> N0 -(B1:true)-> N2 -> N3 -(B2:true)-> N5 -> N6 -> EXIT (type != null, quantity != -1)
    public void testM25_TP1_ToStringTypedLimited() {
        Resource resource = new Resource(game, tile, silverResource, 50);
        assertEquals("50 model.resource.silver", resource.toString());
    }

    // M25-TP2: ENTRY -> N0 -(B1:true)-> N2 -> N3 -(B2:false)-> N4 -> N6 -> EXIT (type != null, quantity == -1)
    public void testM25_TP2_ToStringTypedUnlimited() {
        Resource resource = new Resource(game, tile, grainResource, -1);
        assertEquals("model.resource.grain", resource.toString());
    }

    // M25-TP3: ENTRY -> N0 -(B1:false)-> N1 -> N3 -(B2:true)-> N5 -> N6 -> EXIT (type == null, quantity != -1)
    public void testM25_TP3_ToStringNullTypeLimited() {
        Resource resource = new Resource(game, "resource:null1");
        resource.setQuantity(12);
        assertEquals("12 NOTYPE", resource.toString());
    }

    // M25-TP4: ENTRY -> N0 -(B1:false)-> N1 -> N3 -(B2:false)-> N4 -> N6 -> EXIT (type == null, quantity == -1)
    public void testM25_TP4_ToStringNullTypeUnlimited() {
        Resource resource = new Resource(game, "resource:null2");
        resource.setQuantity(-1);
        assertEquals("NOTYPE", resource.toString());
    }

    // ------------------------------------------------------------------
    // M26: <clinit>() — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M26-TP1: ENTRY -> N0 -> EXIT (initializes static TAG and logger on class load)
    public void testM26_TP1_StaticInit() {
        assertEquals("resource", Resource.TAG);
    }
}
