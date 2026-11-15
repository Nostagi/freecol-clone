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

import javax.swing.JLabel;

import net.sf.freecol.util.test.FreeColTestCase;


/**
 * CFG-driven test suite for {@link TradeLocation} derived from
 * {@code test_cft/graph/net/sf/freecol/common/model/TradeLocation.json}.
 *
 * Covers all 2 methods (M1..M2) and achieves 100% C3 (node + edge)
 * coverage across 2 Test Paths.
 */
public class TradeLocationCfgTest extends FreeColTestCase {

    @Override
    public void setUp() throws Exception {
        super.setUp();
        // Pre-condition: Interface default methods tested via concrete stub; no game state required
    }

    // ------------------------------------------------------------------
    // Pre-condition helpers & stubs
    // ------------------------------------------------------------------

    private static class StubTradeLocation implements TradeLocation {
        private final String name;

        StubTradeLocation(String name) {
            this.name = name;
        }

        @Override
        public int getAvailableGoodsCount(GoodsType goodsType) {
            return 0;
        }

        @Override
        public int getExportAmount(GoodsType goodsType, int turns) {
            return 0;
        }

        @Override
        public int getImportAmount(GoodsType goodsType, int turns) {
            return 0;
        }

        @Override
        public String getLocationName(TradeLocation tradeLocation) {
            return (tradeLocation == this) ? name : "";
        }
    }

    // ------------------------------------------------------------------
    // M1: getNameAsJlabel()Ljavax/swing/JLabel; — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M1-TP1: ENTRY -> N0 -> EXIT (returns JLabel initialized with getLocationName(this))
    public void testM1_TP1_GetNameAsJlabel() {
        TradeLocation loc = new StubTradeLocation("New Amsterdam");
        JLabel label = loc.getNameAsJlabel();

        assertNotNull(label);
        assertEquals("New Amsterdam", label.getText());
    }

    // ------------------------------------------------------------------
    // M2: canBeInput()Z — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M2-TP1: ENTRY -> N0 -> EXIT (default implementation returns false)
    public void testM2_TP1_CanBeInputDefaultFalse() {
        TradeLocation loc = new StubTradeLocation("New Amsterdam");
        assertFalse(loc.canBeInput());
    }
}
