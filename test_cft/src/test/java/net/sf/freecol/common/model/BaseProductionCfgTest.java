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

import java.util.Collections;
import java.util.List;

import net.sf.freecol.util.test.FreeColTestCase;


/**
 * CFG-driven test suite for {@link BaseProduction} derived from
 * {@code test_cft/graph/net/sf/freecol/common/model/BaseProduction.json}.
 *
 * Covers all 1 method (M1: {@code getBaseProduction}) and achieves 100% C3 (node + edge)
 * coverage across 5 Test Paths.
 */
public class BaseProductionCfgTest extends FreeColTestCase {

    private GoodsType grainType;
    private GoodsType lumberType;
    private UnitType colonistType;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        // Pre-condition: Load standard GoodsType and UnitType instances from Specification
        grainType = spec().getGoodsType("model.goods.grain");
        lumberType = spec().getGoodsType("model.goods.lumber");
        colonistType = spec().getUnitType("model.unit.freeColonist");
    }

    // ------------------------------------------------------------------
    // Pre-condition helpers & stubs
    // ------------------------------------------------------------------

    private static class StubBaseProduction implements BaseProduction {
        private final List<ProductionType> attendedTypes;
        private final List<ProductionType> unattendedTypes;

        StubBaseProduction(List<ProductionType> attendedTypes,
                           List<ProductionType> unattendedTypes) {
            this.attendedTypes = attendedTypes;
            this.unattendedTypes = unattendedTypes;
        }

        @Override
        public List<ProductionType> getAvailableProductionTypes(boolean unattended) {
            return unattended ? unattendedTypes : attendedTypes;
        }
    }

    // ------------------------------------------------------------------
    // M1: getBaseProduction(ProductionType, GoodsType, UnitType)I — 5 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M1-TP1: ENTRY -> N0 -(B1:false)-> N1 -> EXIT (goodsType == null)
    public void testM1_TP1_NullGoodsTypeReturnsZero() {
        BaseProduction bp = new StubBaseProduction(Collections.emptyList(), Collections.emptyList());
        ProductionType pt = new ProductionType(new AbstractGoods(grainType, 5), false, null);

        assertEquals(0, bp.getBaseProduction(pt, null, colonistType));
    }

    // M1-TP2: ENTRY -> N0 -(B1:true)-> N2 -(B2:true)-> N7 -(B4:true)-> N9 -(B5:true)-> N11 -> N12 -> EXIT
    // (goodsType != null, explicit productionType != null with matching output)
    public void testM1_TP2_ExplicitProductionTypeWithMatchingOutput() {
        BaseProduction bp = new StubBaseProduction(Collections.emptyList(), Collections.emptyList());
        ProductionType pt = new ProductionType(new AbstractGoods(grainType, 5), false, null);

        assertEquals(5, bp.getBaseProduction(pt, grainType, colonistType));
    }

    // M1-TP3: ENTRY -> N0 -(B1:true)-> N2 -(B2:true)-> N7 -(B4:true)-> N9 -(B5:false)-> N10 -> N12 -> EXIT
    // (goodsType != null, explicit productionType != null without matching output -> best == null)
    public void testM1_TP3_ExplicitProductionTypeWithoutMatchingOutput() {
        BaseProduction bp = new StubBaseProduction(Collections.emptyList(), Collections.emptyList());
        ProductionType pt = new ProductionType(new AbstractGoods(lumberType, 4), false, null);

        assertEquals(0, bp.getBaseProduction(pt, grainType, colonistType));
    }

    // M1-TP4: ENTRY -> N0 -(B1:true)-> N2 -(B2:false)-> N3 -(B3:true)-> N5 -> N6 -> N7 -(B4:true)-> N9 -(B5:true)-> N11 -> N12 -> EXIT
    // (productionType == null, unitType != null -> unattended == false, best productionType found)
    public void testM1_TP4_NullProductionTypeWithUnitTypeFindsBest() {
        ProductionType attendedPt = new ProductionType(new AbstractGoods(grainType, 6), false, null);
        BaseProduction bp = new StubBaseProduction(
            Collections.singletonList(attendedPt),
            Collections.emptyList());

        assertEquals(6, bp.getBaseProduction(null, grainType, colonistType));
    }

    // M1-TP5: ENTRY -> N0 -(B1:true)-> N2 -(B2:false)-> N3 -(B3:false)-> N4 -> N6 -> N7 -(B4:false)-> N8 -> EXIT
    // (productionType == null, unitType == null -> unattended == true, no matching productionType -> null)
    public void testM1_TP5_NullProductionTypeAndNullUnitTypeNoMatchReturnsZero() {
        ProductionType attendedPt = new ProductionType(new AbstractGoods(grainType, 6), false, null);
        BaseProduction bp = new StubBaseProduction(
            Collections.singletonList(attendedPt),
            Collections.emptyList());

        assertEquals(0, bp.getBaseProduction(null, grainType, null));
    }
}
