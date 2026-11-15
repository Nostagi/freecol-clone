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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import net.sf.freecol.util.test.FreeColTestCase;


/**
 * CFG-driven test suite for {@link ProductionInfo} derived from
 * {@code test_cft/graph/net/sf/freecol/common/model/ProductionInfo.json}.
 *
 * Covers all 21 methods (M1..M21, including private {@code append} and both
 * {@code lambda$...} methods) and achieves 100% C3 (node + edge) coverage
 * across 33 Test Paths.
 */
public class ProductionInfoCfgTest extends FreeColTestCase {

    private GoodsType grainType;
    private GoodsType lumberType;
    private GoodsType toolsType;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        // Pre-condition: Load standard GoodsTypes from Specification
        grainType = spec().getGoodsType("model.goods.grain");
        lumberType = spec().getGoodsType("model.goods.lumber");
        toolsType = spec().getGoodsType("model.goods.tools");
    }

    // ------------------------------------------------------------------
    // Pre-condition helpers & stubs
    // ------------------------------------------------------------------

    // No external stubs required; ProductionInfo is tested with real GoodsType instances.

    // ------------------------------------------------------------------
    // M1: <init>()V — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M1-TP1: ENTRY -> N0 -> EXIT (default constructor initializes empty lists)
    public void testM1_TP1_DefaultConstructor() {
        ProductionInfo info = new ProductionInfo();
        assertTrue(info.getConsumption().isEmpty());
        assertTrue(info.getProduction().isEmpty());
        assertTrue(info.getMaximumConsumption().isEmpty());
        assertTrue(info.getMaximumProduction().isEmpty());
    }

    // ------------------------------------------------------------------
    // M2: getConsumption()Ljava/util/List; — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M2-TP1: ENTRY -> N0 -> EXIT (returns consumption list)
    public void testM2_TP1_GetConsumption() {
        ProductionInfo info = new ProductionInfo();
        assertNotNull(info.getConsumption());
        assertEquals(0, info.getConsumption().size());
    }

    // ------------------------------------------------------------------
    // M3: setConsumption(Ljava/util/List;)V — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M3-TP1: ENTRY -> N0 -> EXIT (replaces consumption list)
    public void testM3_TP1_SetConsumption() {
        ProductionInfo info = new ProductionInfo();
        List<AbstractGoods> list = Collections.singletonList(new AbstractGoods(grainType, 4));
        info.setConsumption(list);
        assertEquals(list, info.getConsumption());
    }

    // ------------------------------------------------------------------
    // M4: addConsumption(Lnet/sf/freecol/common/model/AbstractGoods;)V — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M4-TP1: ENTRY -> N0 -> EXIT (adds item to consumption list)
    public void testM4_TP1_AddConsumption() {
        ProductionInfo info = new ProductionInfo();
        AbstractGoods ag = new AbstractGoods(lumberType, 2);
        info.addConsumption(ag);
        assertEquals(1, info.getConsumption().size());
        assertEquals(ag, info.getConsumption().get(0));
    }

    // ------------------------------------------------------------------
    // M5: getProduction()Ljava/util/List; — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M5-TP1: ENTRY -> N0 -> EXIT (returns production list)
    public void testM5_TP1_GetProduction() {
        ProductionInfo info = new ProductionInfo();
        assertNotNull(info.getProduction());
        assertTrue(info.getProduction().isEmpty());
    }

    // ------------------------------------------------------------------
    // M6: setProduction(Ljava/util/List;)V — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M6-TP1: ENTRY -> N0 -> EXIT (replaces production list)
    public void testM6_TP1_SetProduction() {
        ProductionInfo info = new ProductionInfo();
        List<AbstractGoods> list = Collections.singletonList(new AbstractGoods(grainType, 5));
        info.setProduction(list);
        assertEquals(list, info.getProduction());
    }

    // ------------------------------------------------------------------
    // M7: addProduction(Lnet/sf/freecol/common/model/AbstractGoods;)V — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M7-TP1: ENTRY -> N0 -> EXIT (adds single AbstractGoods to production)
    public void testM7_TP1_AddSingleProduction() {
        ProductionInfo info = new ProductionInfo();
        AbstractGoods ag = new AbstractGoods(toolsType, 3);
        info.addProduction(ag);
        assertEquals(1, info.getProduction().size());
        assertEquals(ag, info.getProduction().get(0));
    }

    // ------------------------------------------------------------------
    // M8: addProduction(Ljava/util/List;)V — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M8-TP1: ENTRY -> N0 -> EXIT (adds list of AbstractGoods to production)
    public void testM8_TP1_AddProductionList() {
        ProductionInfo info = new ProductionInfo();
        info.addProduction(Arrays.asList(
                new AbstractGoods(grainType, 2),
                new AbstractGoods(lumberType, 4)));
        assertEquals(2, info.getProduction().size());
    }

    // ------------------------------------------------------------------
    // M9: getMaximumProduction()Ljava/util/List; — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M9-TP1: ENTRY -> N0 -> EXIT (returns maximumProduction list)
    public void testM9_TP1_GetMaximumProduction() {
        ProductionInfo info = new ProductionInfo();
        assertNotNull(info.getMaximumProduction());
        assertTrue(info.getMaximumProduction().isEmpty());
    }

    // ------------------------------------------------------------------
    // M10: setMaximumProduction(Ljava/util/List;)V — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M10-TP1: ENTRY -> N0 -> EXIT (replaces maximumProduction list)
    public void testM10_TP1_SetMaximumProduction() {
        ProductionInfo info = new ProductionInfo();
        List<AbstractGoods> list = Collections.singletonList(new AbstractGoods(grainType, 10));
        info.setMaximumProduction(list);
        assertEquals(list, info.getMaximumProduction());
    }

    // ------------------------------------------------------------------
    // M11: addMaximumProduction(Lnet/sf/freecol/common/model/AbstractGoods;)V — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M11-TP1: ENTRY -> N0 -> EXIT (adds item to maximumProduction)
    public void testM11_TP1_AddMaximumProduction() {
        ProductionInfo info = new ProductionInfo();
        AbstractGoods ag = new AbstractGoods(grainType, 8);
        info.addMaximumProduction(ag);
        assertEquals(1, info.getMaximumProduction().size());
    }

    // ------------------------------------------------------------------
    // M12: getProductionDeficit()Ljava/util/List; — 2 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M12-TP1: ENTRY -> N0 -(B1:false)-> N1 -> N3 -> EXIT (maximumProduction is empty)
    public void testM12_TP1_GetProductionDeficitWhenMaxEmpty() {
        ProductionInfo info = new ProductionInfo();
        info.addProduction(new AbstractGoods(grainType, 3));
        assertSame(WorkLocation.EMPTY_LIST, info.getProductionDeficit());
    }

    // M12-TP2: ENTRY -> N0 -(B1:true)-> N2 -> N3 -> EXIT (maximumProduction is non-empty)
    public void testM12_TP2_GetProductionDeficitWhenMaxNonEmpty() {
        ProductionInfo info = new ProductionInfo();
        info.addProduction(new AbstractGoods(grainType, 3));
        info.addMaximumProduction(new AbstractGoods(grainType, 7));
        List<AbstractGoods> deficit = info.getProductionDeficit();
        assertEquals(1, deficit.size());
        assertEquals(grainType, deficit.get(0).getType());
        assertEquals(4, deficit.get(0).getAmount());
    }

    // ------------------------------------------------------------------
    // M13: getConsumptionDeficit()Ljava/util/List; — 2 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M13-TP1: ENTRY -> N0 -(B1:false)-> N1 -> N3 -> EXIT (maximumConsumption is empty)
    public void testM13_TP1_GetConsumptionDeficitWhenMaxEmpty() {
        ProductionInfo info = new ProductionInfo();
        info.addConsumption(new AbstractGoods(lumberType, 2));
        assertSame(WorkLocation.EMPTY_LIST, info.getConsumptionDeficit());
    }

    // M13-TP2: ENTRY -> N0 -(B1:true)-> N2 -> N3 -> EXIT (maximumConsumption is non-empty)
    public void testM13_TP2_GetConsumptionDeficitWhenMaxNonEmpty() {
        ProductionInfo info = new ProductionInfo();
        info.addConsumption(new AbstractGoods(lumberType, 2));
        info.addMaximumConsumption(new AbstractGoods(lumberType, 6));
        List<AbstractGoods> deficit = info.getConsumptionDeficit();
        assertEquals(1, deficit.size());
        assertEquals(lumberType, deficit.get(0).getType());
        assertEquals(4, deficit.get(0).getAmount());
    }

    // ------------------------------------------------------------------
    // M14: atMaximumProduction()Z — 5 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M14-TP1: ENTRY -> N0 -(B1:false)-> N1 -> EXIT (maximumProduction is empty -> true)
    public void testM14_TP1_AtMaximumProductionWhenMaxEmpty() {
        ProductionInfo info = new ProductionInfo();
        assertTrue(info.atMaximumProduction());
    }

    // M14-TP2: ENTRY -> N0 -(B1:true)-> N2 -> N3 -(B2:true)-> N8 -> EXIT
    // (maximumProduction non-empty, production empty -> true)
    public void testM14_TP2_AtMaximumProductionWhenProductionEmpty() {
        ProductionInfo info = new ProductionInfo();
        info.addMaximumProduction(new AbstractGoods(grainType, 5));
        assertTrue(info.atMaximumProduction());
    }

    // M14-TP3: ENTRY -> N0 -(B1:true)-> N2 -> N3 -(B2:false)-> N4 -(B3:true)-> N7 -> N3 -(B2:true)-> N8 -> EXIT
    // (agMax == null in maximumConsumption -> continues loop and returns true)
    public void testM14_TP3_AtMaximumProductionWhenNoMatchingMaxConsumption() {
        ProductionInfo info = new ProductionInfo();
        info.addMaximumProduction(new AbstractGoods(grainType, 5));
        info.addProduction(new AbstractGoods(grainType, 3));
        assertTrue(info.atMaximumProduction());
    }

    // M14-TP4: ENTRY -> N0 -(B1:true)-> N2 -> N3 -(B2:false)-> N4 -(B3:false)-> N5 -(B4:true)-> N7 -> N3 -(B2:true)-> N8 -> EXIT
    // (agMax != null and agMax.amount <= ag.amount -> returns true)
    public void testM14_TP4_AtMaximumProductionWhenMaxNotGreater() {
        ProductionInfo info = new ProductionInfo();
        info.addMaximumProduction(new AbstractGoods(grainType, 5));
        info.addMaximumConsumption(new AbstractGoods(grainType, 4));
        info.addProduction(new AbstractGoods(grainType, 4));
        assertTrue(info.atMaximumProduction());
    }

    // M14-TP5: ENTRY -> N0 -(B1:true)-> N2 -> N3 -(B2:false)-> N4 -(B3:false)-> N5 -(B4:false)-> N6 -> EXIT
    // (agMax != null and agMax.amount > ag.amount -> returns false)
    public void testM14_TP5_AtMaximumProductionWhenBelowMaxReturnsFalse() {
        ProductionInfo info = new ProductionInfo();
        info.addMaximumProduction(new AbstractGoods(grainType, 5));
        info.addMaximumConsumption(new AbstractGoods(grainType, 6));
        info.addProduction(new AbstractGoods(grainType, 4));
        assertFalse(info.atMaximumProduction());
    }

    // ------------------------------------------------------------------
    // M15: getMaximumConsumption()Ljava/util/List; — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M15-TP1: ENTRY -> N0 -> EXIT (returns maximumConsumption list)
    public void testM15_TP1_GetMaximumConsumption() {
        ProductionInfo info = new ProductionInfo();
        assertNotNull(info.getMaximumConsumption());
        assertTrue(info.getMaximumConsumption().isEmpty());
    }

    // ------------------------------------------------------------------
    // M16: setMaximumConsumption(Ljava/util/List;)V — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M16-TP1: ENTRY -> N0 -> EXIT (replaces maximumConsumption list)
    public void testM16_TP1_SetMaximumConsumption() {
        ProductionInfo info = new ProductionInfo();
        List<AbstractGoods> list = Collections.singletonList(new AbstractGoods(lumberType, 5));
        info.setMaximumConsumption(list);
        assertEquals(list, info.getMaximumConsumption());
    }

    // ------------------------------------------------------------------
    // M17: addMaximumConsumption(Lnet/sf/freecol/common/model/AbstractGoods;)V — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M17-TP1: ENTRY -> N0 -> EXIT (adds item to maximumConsumption)
    public void testM17_TP1_AddMaximumConsumption() {
        ProductionInfo info = new ProductionInfo();
        AbstractGoods ag = new AbstractGoods(toolsType, 3);
        info.addMaximumConsumption(ag);
        assertEquals(1, info.getMaximumConsumption().size());
    }

    // ------------------------------------------------------------------
    // M18: append(StringBuilder, String, List)V — 3 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M18-TP1: ENTRY -> N0 -(B1:false)-> N1 -> EXIT (empty list -> returns early)
    public void testM18_TP1_AppendEmptyList() {
        ProductionInfo info = new ProductionInfo();
        assertEquals("", info.toString());
    }

    // M18-TP2: ENTRY -> N0 -(B1:true)-> N2 -> N3 -(B2:false)-> N4 -(B3:true)-> N6 -> N3 -(B2:true)-> N7 -> EXIT
    // (goodsType.getStoredAs() == goodsType, e.g. lumber)
    public void testM18_TP2_AppendStoredAsSelf() {
        ProductionInfo info = new ProductionInfo();
        info.addProduction(new AbstractGoods(lumberType, 5));
        String str = info.toString();
        assertTrue(str.startsWith("Production: "));
        assertFalse(str.contains("["));
    }

    // M18-TP3: ENTRY -> N0 -(B1:true)-> N2 -> N3 -(B2:false)-> N4 -(B3:false)-> N5 -> N6 -> N3 -(B2:true)-> N7 -> EXIT
    // (goodsType.getStoredAs() != goodsType, e.g. grain stored as food)
    public void testM18_TP3_AppendStoredAsDifferentType() {
        ProductionInfo info = new ProductionInfo();
        info.addProduction(new AbstractGoods(grainType, 5));
        String str = info.toString();
        assertTrue(str.contains("[model.goods.food]"));
    }

    // ------------------------------------------------------------------
    // M19: toString()Ljava/lang/String; — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M19-TP1: ENTRY -> N0 -> EXIT (formats all four sections)
    public void testM19_TP1_ToStringAllSections() {
        ProductionInfo info = new ProductionInfo();
        info.addProduction(new AbstractGoods(lumberType, 3));
        info.addConsumption(new AbstractGoods(toolsType, 1));
        info.addMaximumProduction(new AbstractGoods(lumberType, 5));
        info.addMaximumConsumption(new AbstractGoods(toolsType, 2));
        String str = info.toString();
        assertTrue(str.contains("Production:"));
        assertTrue(str.contains("Consumption:"));
        assertTrue(str.contains("Maximum Production:"));
        assertTrue(str.contains("Maximum Consumption:"));
    }

    // ------------------------------------------------------------------
    // M20: lambda$getConsumptionDeficit$0(AbstractGoods)AbstractGoods — 3 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M20-TP1: ENTRY -> N0 -(B1:true)-> N2 -> N3 -(B2:true)-> N5 -> N6 -> EXIT
    // (agMax != null and amount != 0 -> returns deficit)
    public void testM20_TP1_ConsumptionDeficitLambdaNonZero() {
        ProductionInfo info = new ProductionInfo();
        info.addConsumption(new AbstractGoods(lumberType, 2));
        info.addMaximumConsumption(new AbstractGoods(lumberType, 5));
        List<AbstractGoods> deficit = info.getConsumptionDeficit();
        assertEquals(1, deficit.size());
        assertEquals(3, deficit.get(0).getAmount());
    }

    // M20-TP2: ENTRY -> N0 -(B1:true)-> N2 -> N3 -(B2:false)-> N4 -> N6 -> EXIT
    // (agMax != null and amount == 0 -> returns null, filtered out)
    public void testM20_TP2_ConsumptionDeficitLambdaZero() {
        ProductionInfo info = new ProductionInfo();
        info.addConsumption(new AbstractGoods(lumberType, 5));
        info.addMaximumConsumption(new AbstractGoods(lumberType, 5));
        assertTrue(info.getConsumptionDeficit().isEmpty());
    }

    // M20-TP3: ENTRY -> N0 -(B1:false)-> N1 -> N3 -(B2:false)-> N4 -> N6 -> EXIT
    // (agMax == null -> amount = 0 -> returns null, filtered out)
    public void testM20_TP3_ConsumptionDeficitLambdaMissingMax() {
        ProductionInfo info = new ProductionInfo();
        info.addConsumption(new AbstractGoods(grainType, 3));
        info.addMaximumConsumption(new AbstractGoods(lumberType, 5));
        assertTrue(info.getConsumptionDeficit().isEmpty());
    }

    // ------------------------------------------------------------------
    // M21: lambda$getProductionDeficit$0(AbstractGoods)AbstractGoods — 3 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M21-TP1: ENTRY -> N0 -(B1:true)-> N2 -> N3 -(B2:true)-> N5 -> N6 -> EXIT
    // (agMax != null and amount > 0 -> returns deficit)
    public void testM21_TP1_ProductionDeficitLambdaPositive() {
        ProductionInfo info = new ProductionInfo();
        info.addProduction(new AbstractGoods(grainType, 2));
        info.addMaximumProduction(new AbstractGoods(grainType, 6));
        List<AbstractGoods> deficit = info.getProductionDeficit();
        assertEquals(1, deficit.size());
        assertEquals(4, deficit.get(0).getAmount());
    }

    // M21-TP2: ENTRY -> N0 -(B1:true)-> N2 -> N3 -(B2:false)-> N4 -> N6 -> EXIT
    // (agMax != null and amount <= 0 -> returns null, filtered out)
    public void testM21_TP2_ProductionDeficitLambdaNonPositive() {
        ProductionInfo info = new ProductionInfo();
        info.addProduction(new AbstractGoods(grainType, 6));
        info.addMaximumProduction(new AbstractGoods(grainType, 4));
        assertTrue(info.getProductionDeficit().isEmpty());
    }

    // M21-TP3: ENTRY -> N0 -(B1:false)-> N1 -> N3 -(B2:false)-> N4 -> N6 -> EXIT
    // (agMax == null -> amount = 0 <= 0 -> returns null, filtered out)
    public void testM21_TP3_ProductionDeficitLambdaMissingMax() {
        ProductionInfo info = new ProductionInfo();
        info.addProduction(new AbstractGoods(toolsType, 2));
        info.addMaximumProduction(new AbstractGoods(grainType, 5));
        assertTrue(info.getProductionDeficit().isEmpty());
    }
}
