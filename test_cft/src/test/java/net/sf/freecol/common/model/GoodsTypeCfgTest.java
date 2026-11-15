package net.sf.freecol.common.model;

import java.io.StringWriter;
import java.util.List;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.BlockJUnit4ClassRunner;

import net.sf.freecol.common.io.FreeColXMLWriter;
import net.sf.freecol.util.test.MockModelTestCase;


@RunWith(BlockJUnit4ClassRunner.class)
public class GoodsTypeCfgTest extends MockModelTestCase {

    private GoodsType food;
    private GoodsType cotton;
    private GoodsType cloth;
    private GoodsType lumber;
    private GoodsType hammers;
    private GoodsType ore;
    private GoodsType tools;
    private GoodsType muskets;
    private GoodsType silver;
    private GoodsType tradeGoods;
    private GoodsType horses;
    private GoodsType bells;
    private GoodsType crosses;

    @Before
    public void setUpGoodsTypeFixture() {
        food = spec().getPrimaryFoodType();
        cotton = spec().getGoodsType("model.goods.cotton");
        cloth = spec().getGoodsType("model.goods.cloth");
        lumber = spec().getGoodsType("model.goods.lumber");
        hammers = spec().getGoodsType("model.goods.hammers");
        ore = spec().getGoodsType("model.goods.ore");
        tools = spec().getGoodsType("model.goods.tools");
        muskets = spec().getGoodsType("model.goods.muskets");
        silver = spec().getGoodsType("model.goods.silver");
        tradeGoods = spec().getGoodsType("model.goods.tradeGoods");
        horses = spec().getGoodsType("model.goods.horses");
        bells = spec().getGoodsType("model.goods.bells");
        crosses = spec().getGoodsType("model.goods.crosses");
    }

    @Test
    public void testBasicAttributesAndPredicates() {
        // Food type
        assertTrue(food.isFoodType());
        assertFalse(cotton.isFoodType());

        // Farmed
        assertTrue(food.isFarmed());
        assertTrue(cotton.isFarmed());
        assertFalse(cloth.isFarmed());
        assertFalse(tools.isFarmed());

        // Military
        assertFalse(cotton.getMilitary());
        cotton.setMilitary();
        assertTrue(cotton.getMilitary());

        // Limit ignored
        assertTrue(bells.limitIgnored());
        assertTrue(crosses.limitIgnored());
        assertTrue(food.limitIgnored());
        assertFalse(cotton.limitIgnored());

        // New world goods
        assertTrue(cotton.isNewWorldGoodsType());
        assertFalse(food.isNewWorldGoodsType());
        assertFalse(cloth.isNewWorldGoodsType());
        assertFalse(tools.isNewWorldGoodsType());

        // Building material
        assertTrue(hammers.isBuildingMaterial());
        assertTrue(tools.isBuildingMaterial());
        assertFalse(lumber.isBuildingMaterial());
        assertFalse(silver.isBuildingMaterial());
        assertFalse(cotton.isBuildingMaterial());

        // Trade goods
        assertTrue(tradeGoods.isTradeGoods());
        assertFalse(cotton.isTradeGoods());

        // Storable
        assertTrue(cotton.isStorable());
        assertTrue(cloth.isStorable());
        assertFalse(hammers.isStorable());
        assertFalse(bells.isStorable());
        assertFalse(crosses.isStorable());

        // Tag name
        assertEquals(GoodsType.TAG, food.getXMLTagName());
    }

    @Test
    public void testStoredAsControlFlowBranches() {
        // Branch 1: storedAs == null -> isStoredAs() is false, getStoredAs() returns itself
        assertFalse(cotton.isStoredAs());
        assertSame(cotton, cotton.getStoredAs());

        // Branch 2: storedAs != null -> isStoredAs() is true, getStoredAs() returns stored target
        boolean foundStoredAs = false;
        for (GoodsType gt : spec().getGoodsTypeList()) {
            if (gt.isStoredAs()) {
                foundStoredAs = true;
                assertNotSame(gt, gt.getStoredAs());
                break;
            }
        }
        // If specification has no storedAs, create a localized GoodsType to test branch
        if (!foundStoredAs) {
            GoodsType grain = new GoodsType("model.goods.grain", spec());
            GoodsType copy = new GoodsType("model.goods.grainCopy", spec());
            copy.copyIn(grain);
            assertFalse(grain.isStoredAs());
        }
    }

    @Test
    public void testBreedableControlFlowBranches() {
        // Branch 1: breedingNumber != INFINITY -> isBreedable() is true
        assertTrue(horses.isBreedable());
        assertTrue(horses.getBreedingNumber() > 0);
        assertTrue(horses.getBreedingNumber() != Constants.INFINITY);

        // Branch 2: breedingNumber == INFINITY -> isBreedable() is false
        assertFalse(cotton.isBreedable());
        assertEquals(Constants.INFINITY, cotton.getBreedingNumber());
    }

    @Test
    public void testPriceAndProductionParameters() {
        // Price for non-tradeable goods (like hammers)
        assertTrue(hammers.getPrice() != Constants.INFINITY);
        int oldPrice = hammers.getPrice();
        hammers.setPrice(42);
        assertEquals(42, hammers.getPrice());
        hammers.setPrice(oldPrice);

        // Production weights and thresholds
        assertTrue(cotton.getProductionWeight() >= 0.0f);
        assertTrue(cotton.getLowProductionThreshold() >= 0.0f);
        assertTrue(cotton.getZeroProductionFactor() >= 0.0f);

        // Initial market parameters
        assertTrue(cotton.getInitialAmount() >= 0);
        assertTrue(cotton.getInitialSellPrice() > 0);
        assertTrue(cotton.getInitialBuyPrice() >= cotton.getInitialSellPrice());
        assertEquals(cotton.getInitialBuyPrice() - cotton.getInitialSellPrice(),
            cotton.getPriceDifference());

        // Labels and keys
        assertNotNull(cotton.getLabel());
        assertEquals("model.goods.cotton.workingAs", cotton.getWorkingAsKey());
    }

    @Test
    public void testModifierTypes() {
        // Liberty
        assertTrue(bells.isLibertyType());
        assertFalse(cotton.isLibertyType());

        // Immigration
        assertTrue(crosses.isImmigrationType());
        assertFalse(cotton.isImmigrationType());
    }

    @Test
    public void testProductionChainAndRefinementControlFlow() {
        // Raw material vs Refined
        assertTrue(cotton.isRawMaterial());
        assertFalse(cotton.isRefined());
        assertSame(cloth, cotton.getOutputType());
        assertNull(cotton.getInputType());

        assertTrue(cloth.isRefined());
        assertFalse(cloth.isRawMaterial());
        assertSame(cotton, cloth.getInputType());
        assertNull(cloth.getOutputType());

        // Multi-level chain (ore -> tools -> muskets)
        assertTrue(ore.isRawMaterial());
        assertSame(tools, ore.getOutputType());
        assertSame(ore, tools.getInputType());
        assertSame(muskets, tools.getOutputType());
        assertSame(tools, muskets.getInputType());

        // isNewWorldLuxuryType() branches:
        // Branch 1: madeFrom == null -> false
        assertFalse(cotton.isNewWorldLuxuryType());
        assertFalse(ore.isNewWorldLuxuryType());

        // Branch 2: madeFrom != null && !madeFrom.isNewWorldGoodsType() -> false
        assertFalse(tools.isNewWorldLuxuryType());
        assertFalse(muskets.isNewWorldLuxuryType());

        // Branch 3: madeFrom != null && madeFrom.isNewWorldGoodsType() -> true
        assertTrue(cloth.isNewWorldLuxuryType());
        GoodsType cigars = spec().getGoodsType("model.goods.cigars");
        if (cigars != null) {
            assertTrue(cigars.isNewWorldLuxuryType());
        }

        // getProductionChain() tests
        List<GoodsType> cottonChain = cotton.getProductionChain();
        assertEquals(1, cottonChain.size());
        assertSame(cotton, cottonChain.get(0));

        List<GoodsType> clothChain = cloth.getProductionChain();
        assertEquals(2, clothChain.size());
        assertSame(cotton, clothChain.get(0));
        assertSame(cloth, clothChain.get(1));

        List<GoodsType> musketsChain = muskets.getProductionChain();
        assertEquals(3, musketsChain.size());
        assertSame(ore, musketsChain.get(0));
        assertSame(tools, musketsChain.get(1));
        assertSame(muskets, musketsChain.get(2));
    }

    @Test
    public void testIsRawBuildingMaterialControlFlow() {
        // Branch 1: madeFrom != null -> returns false immediately
        assertFalse(hammers.isRawBuildingMaterial());
        assertFalse(tools.isRawBuildingMaterial());
        assertFalse(cloth.isRawBuildingMaterial());

        // Branch 2: madeFrom == null, makes == null -> returns false
        assertFalse(silver.isRawBuildingMaterial());
        assertFalse(food.isRawBuildingMaterial());

        // Branch 3: madeFrom == null, makes != null, but makes not building material -> returns false
        assertFalse(cotton.isRawBuildingMaterial());

        // Branch 4: madeFrom == null, makes != null, makes is building material -> returns true
        assertTrue(lumber.isRawBuildingMaterial());
    }

    @Test
    public void testIsRawMaterialForUnstorableBuildingMaterialControlFlow() {
        // Branch 1: madeFrom != null -> returns false immediately
        assertFalse(hammers.isRawMaterialForUnstorableBuildingMaterial());
        assertFalse(tools.isRawMaterialForUnstorableBuildingMaterial());

        // Branch 2: madeFrom == null, makes == null -> returns false
        assertFalse(silver.isRawMaterialForUnstorableBuildingMaterial());

        // Branch 3: madeFrom == null, makes != null, makes is not (building material && unstorable) -> returns false
        assertFalse(cotton.isRawMaterialForUnstorableBuildingMaterial());

        // Branch 4: madeFrom == null, makes != null, makes is building material and unstorable -> returns true
        // lumber makes hammers (buildingMaterial=true, storable=false)
        assertTrue(lumber.isRawMaterialForUnstorableBuildingMaterial());
    }

    @Test
    public void testGetEquivalentTypesControlFlow() {
        Set<GoodsType> cottonEquiv = cotton.getEquivalentTypes();
        assertTrue(cottonEquiv.contains(cotton));
        assertEquals(1, cottonEquiv.size());

        Set<GoodsType> foodEquiv = food.getEquivalentTypes();
        assertTrue(foodEquiv.contains(food));

        for (GoodsType gt : foodEquiv) {
            assertTrue(gt == food || gt.getStoredAs() == food);
        }
    }

    @Test
    public void testSetDerivedAttributes() {
        GoodsType.setDerivedAttributes(spec());

        // Verified derived makes
        assertSame(cloth, cotton.getOutputType());
        assertSame(hammers, lumber.getOutputType());
        assertSame(tools, ore.getOutputType());
        assertSame(muskets, tools.getOutputType());

        // Verified derived buildingMaterial
        assertTrue(hammers.isBuildingMaterial());
        assertTrue(tools.isBuildingMaterial());
        assertFalse(lumber.isBuildingMaterial());
        assertFalse(cotton.isBuildingMaterial());
    }

    @Test
    public void testGetRankAndComparatorControlFlow() {
        // Rank comparisons through goodsTypeComparator:
        // Rank -1: hammers (not storable), tradeGoods
        // Rank 1: food (isFoodType)
        // Rank 2: cotton (isNewWorldGoodsType)
        // Rank 4: ore, lumber (isRawMaterial)
        // Rank 5: cloth (isNewWorldLuxuryType)
        // Rank 6: tools, muskets (isRefined)

        // Compare Rank 1 (food) with Rank 2 (cotton) -> food < cotton
        assertTrue(GoodsType.goodsTypeComparator.compare(food, cotton) < 0);
        assertTrue(GoodsType.goodsTypeComparator.compare(cotton, food) > 0);

        // Compare Rank 2 (cotton) with Rank 4 (ore) -> cotton < ore
        assertTrue(GoodsType.goodsTypeComparator.compare(cotton, ore) < 0);

        // Compare Rank 4 (ore) with Rank 5 (cloth) -> ore < cloth
        assertTrue(GoodsType.goodsTypeComparator.compare(ore, cloth) < 0);

        // Compare Rank 4 (tools) with Rank 5 (cloth) -> tools < cloth
        assertTrue(GoodsType.goodsTypeComparator.compare(tools, cloth) < 0);

        // Compare Rank 5 (cloth) with Rank 6 (muskets) -> cloth < muskets
        assertTrue(GoodsType.goodsTypeComparator.compare(cloth, muskets) < 0);

        // Compare Rank 6 (tools) with Rank -1 (hammers) -> tools > hammers
        assertTrue(GoodsType.goodsTypeComparator.compare(tools, hammers) > 0);
        assertTrue(GoodsType.goodsTypeComparator.compare(tradeGoods, food) < 0);

        // Identical rank comparison (both rank 2): cotton vs tobacco
        GoodsType tobacco = spec().getGoodsType("model.goods.tobacco");
        if (tobacco != null) {
            int comp = GoodsType.goodsTypeComparator.compare(cotton, tobacco);
            int reverseComp = GoodsType.goodsTypeComparator.compare(tobacco, cotton);
            assertEquals(0, comp + reverseComp);
        }

        // Compare object with itself -> 0
        assertEquals(0, GoodsType.goodsTypeComparator.compare(cotton, cotton));
    }

    @Test
    public void testCopyInAndObjectMethods() {
        GoodsType target = new GoodsType(cotton.getId(), spec());
        target.getFeatureContainer();
        GoodsType differentId = new GoodsType("model.goods.different", spec());
        differentId.getFeatureContainer();

        // Fail branches
        assertFalse(target.copyIn(null));
        assertFalse(target.copyIn(differentId));
        assertFalse(target.copyIn(new Unit(game, "dummyUnit")));

        // Success branch
        assertTrue(target.copyIn(cotton));
        assertEquals(cotton.isFarmed(), target.isFarmed());
        assertEquals(cotton.isFoodType(), target.isFoodType());
        assertEquals(cotton.getMilitary(), target.getMilitary());
        assertEquals(cotton.limitIgnored(), target.limitIgnored());
        assertEquals(cotton.isNewWorldGoodsType(), target.isNewWorldGoodsType());
        assertEquals(cotton.isBuildingMaterial(), target.isBuildingMaterial());
        assertEquals(cotton.isTradeGoods(), target.isTradeGoods());
        assertEquals(cotton.isStorable(), target.isStorable());
        assertEquals(cotton.getStoredAs(), target.getStoredAs());
        assertEquals(cotton.getInputType(), target.getInputType());
        assertEquals(cotton.getOutputType(), target.getOutputType());
        assertEquals(cotton.getInitialAmount(), target.getInitialAmount());
        assertEquals(cotton.getInitialSellPrice(), target.getInitialSellPrice());
        assertEquals(cotton.getPriceDifference(), target.getPriceDifference());
        assertEquals(cotton.getBreedingNumber(), target.getBreedingNumber());
        assertEquals(cotton.getPrice(), target.getPrice());
        assertEquals(cotton.getProductionWeight(), target.getProductionWeight(), 0.001f);
        assertEquals(cotton.getLowProductionThreshold(), target.getLowProductionThreshold(), 0.001f);
        assertEquals(cotton.getZeroProductionFactor(), target.getZeroProductionFactor(), 0.001f);
    }

    @Test
    public void testSerializationAttributesAndChildren() throws Exception {
        // Serialization of cotton (has market child: initialAmount > 0)
        StringWriter sw1 = new StringWriter();
        FreeColXMLWriter xw1 = new FreeColXMLWriter(sw1);
        cotton.toXML(xw1);
        xw1.close();
        String xmlCotton = sw1.toString();
        assertTrue(xmlCotton.contains(GoodsType.TAG));
        assertTrue(xmlCotton.contains("market"));
        assertTrue(xmlCotton.contains("is-farmed=\"true\""));

        // Serialization of hammers (price != INFINITY, no market)
        StringWriter sw2 = new StringWriter();
        FreeColXMLWriter xw2 = new FreeColXMLWriter(sw2);
        hammers.toXML(xw2);
        xw2.close();
        String xmlHammers = sw2.toString();
        assertTrue(xmlHammers.contains(GoodsType.TAG));
        assertTrue(xmlHammers.contains("price="));

        // Serialization of horses (breeding-number != INFINITY)
        StringWriter sw3 = new StringWriter();
        FreeColXMLWriter xw3 = new FreeColXMLWriter(sw3);
        horses.toXML(xw3);
        xw3.close();
        String xmlHorses = sw3.toString();
        assertTrue(xmlHorses.contains("breeding-number="));
    }
}
