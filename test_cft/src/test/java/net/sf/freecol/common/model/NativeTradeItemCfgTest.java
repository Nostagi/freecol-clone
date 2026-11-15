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

import java.io.StringReader;
import java.io.StringWriter;
import java.util.function.Predicate;

import net.sf.freecol.common.io.FreeColXMLReader;
import net.sf.freecol.common.io.FreeColXMLWriter;
import net.sf.freecol.util.test.FreeColTestCase;


/**
 * CFG-driven test suite for {@link NativeTradeItem} derived from
 * {@code test_cft/graph/net/sf/freecol/common/model/NativeTradeItem.json}.
 *
 * Covers all 19 methods (M1..M19) and achieves 100% feasible C3 (node + edge)
 * coverage across 25 Test Paths.
 *
 * Infeasible edges documented:
 * - M11 ({@code copyIn}): {@code N1 -> N3} ({@code B2:true}) — {@code super.copyIn(o)}
 *   invokes {@code GoodsTradeItem.copyIn}, which casts {@code other} to
 *   {@code ColonyTradeItem.class}; since {@code NativeTradeItem} is not a
 *   {@code ColonyTradeItem}, {@code super.copyIn(o)} always returns {@code false}.
 */
public class NativeTradeItemCfgTest extends FreeColTestCase {

    private Game game;
    private Player dutch;
    private Player tupi;
    private GoodsType grainType;
    private GoodsType lumberType;
    private Goods grainGoods;
    private Goods lumberGoods;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        // Pre-condition: Initialize standard game, two players, and standard Goods instances
        game = getStandardGame();
        dutch = game.getPlayerByNationId("model.nation.dutch");
        tupi = game.getPlayerByNationId("model.nation.tupi");
        grainType = spec().getGoodsType("model.goods.grain");
        lumberType = spec().getGoodsType("model.goods.lumber");
        grainGoods = new Goods(game, null, grainType, 10);
        lumberGoods = new Goods(game, null, lumberType, 20);
    }

    // ------------------------------------------------------------------
    // Pre-condition helpers & stubs
    // ------------------------------------------------------------------

    private NativeTradeItem createItem(Goods goods, int price, int haggleCount) {
        NativeTradeItem item = new NativeTradeItem(game, dutch, tupi, goods);
        item.setPrice(price);
        item.setHaggleCount(haggleCount);
        return item;
    }

    // ------------------------------------------------------------------
    // M1: <init>(Game, Player, Player, Goods) — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M1-TP1: ENTRY -> N0 -> EXIT (constructs NativeTradeItem with default PRICE_UNSET and haggleCount 0)
    public void testM1_TP1_constructorWithGamePlayersGoods() {
        NativeTradeItem item = new NativeTradeItem(game, dutch, tupi, grainGoods);
        assertSame(dutch, item.getSource());
        assertSame(tupi, item.getDestination());
        assertSame(grainGoods, item.getGoods());
        assertEquals(NativeTradeItem.PRICE_UNSET, item.getPrice());
        assertEquals(0, item.getHaggleCount());
    }

    // ------------------------------------------------------------------
    // M2: <init>(NativeTradeItem) — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M2-TP1: ENTRY -> N0 -> EXIT (copy constructor preserves source, destination, goods, price, haggleCount)
    public void testM2_TP1_copyConstructor() {
        NativeTradeItem orig = createItem(grainGoods, 50, 2);
        NativeTradeItem copy = new NativeTradeItem(orig);
        assertSame(dutch, copy.getSource());
        assertSame(tupi, copy.getDestination());
        assertSame(grainGoods, copy.getGoods());
        assertEquals(50, copy.getPrice());
        assertEquals(2, copy.getHaggleCount());
    }

    // ------------------------------------------------------------------
    // M3: <init>(Game, FreeColXMLReader) — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M3-TP1: ENTRY -> N0 -> EXIT (reads serialized NativeTradeItem from FreeColXMLReader)
    public void testM3_TP1_xmlReaderConstructor() throws Exception {
        NativeTradeItem orig = createItem(grainGoods, 75, 3);
        StringWriter sw = new StringWriter();
        try (FreeColXMLWriter xw = new FreeColXMLWriter(sw, FreeColXMLWriter.WriteScope.toSave())) {
            orig.toXML(xw);
        }
        try (FreeColXMLReader xr = new FreeColXMLReader(new StringReader(sw.toString()))) {
            xr.nextTag();
            NativeTradeItem read = new NativeTradeItem(game, xr);
            assertEquals(75, read.getPrice());
            assertEquals(3, read.getHaggleCount());
            assertEquals(grainType, read.getGoods().getType());
        }
    }

    // ------------------------------------------------------------------
    // M4: priceIsSet() — 2 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M4-TP1: ENTRY -> N0 -(B1:true)-> N2 -> N3 -> EXIT (price == PRICE_UNSET -> false)
    public void testM4_TP1_priceIsSetFalseWhenUnset() {
        NativeTradeItem item = new NativeTradeItem(game, dutch, tupi, grainGoods);
        assertFalse(item.priceIsSet());
    }

    // M4-TP2: ENTRY -> N0 -(B1:false)-> N1 -> N3 -> EXIT (price != PRICE_UNSET -> true)
    public void testM4_TP2_priceIsSetTrueWhenSet() {
        NativeTradeItem item = createItem(grainGoods, 10, 0);
        assertTrue(item.priceIsSet());
    }

    // ------------------------------------------------------------------
    // M5: priceIsValid() — 2 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M5-TP1: ENTRY -> N0 -(B1:true)-> N2 -> N3 -> EXIT (price <= 0 -> false)
    public void testM5_TP1_priceIsValidFalseWhenNonPositive() {
        NativeTradeItem item = createItem(grainGoods, 0, 0);
        assertFalse(item.priceIsValid());
        item.setPrice(NativeTradeItem.PRICE_INVALID);
        assertFalse(item.priceIsValid());
    }

    // M5-TP2: ENTRY -> N0 -(B1:false)-> N1 -> N3 -> EXIT (price > 0 -> true)
    public void testM5_TP2_priceIsValidTrueWhenPositive() {
        NativeTradeItem item = createItem(grainGoods, 25, 0);
        assertTrue(item.priceIsValid());
    }

    // ------------------------------------------------------------------
    // M6: getPrice() — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M6-TP1: ENTRY -> N0 -> EXIT (returns current price)
    public void testM6_TP1_getPrice() {
        NativeTradeItem item = createItem(grainGoods, 42, 0);
        assertEquals(42, item.getPrice());
    }

    // ------------------------------------------------------------------
    // M7: setPrice(int) — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M7-TP1: ENTRY -> N0 -> EXIT (updates price field)
    public void testM7_TP1_setPrice() {
        NativeTradeItem item = new NativeTradeItem(game, dutch, tupi, grainGoods);
        item.setPrice(99);
        assertEquals(99, item.getPrice());
    }

    // ------------------------------------------------------------------
    // M8: getHaggleCount() — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M8-TP1: ENTRY -> N0 -> EXIT (returns current haggleCount)
    public void testM8_TP1_getHaggleCount() {
        NativeTradeItem item = createItem(grainGoods, 10, 4);
        assertEquals(4, item.getHaggleCount());
    }

    // ------------------------------------------------------------------
    // M9: setHaggleCount(int) — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M9-TP1: ENTRY -> N0 -> EXIT (updates haggleCount field)
    public void testM9_TP1_setHaggleCount() {
        NativeTradeItem item = new NativeTradeItem(game, dutch, tupi, grainGoods);
        item.setHaggleCount(5);
        assertEquals(5, item.getHaggleCount());
    }

    // ------------------------------------------------------------------
    // M10: goodsMatcher() — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M10-TP1: ENTRY -> N0 -> EXIT (returns predicate matching items with equal goods)
    public void testM10_TP1_goodsMatcher() {
        NativeTradeItem item = createItem(grainGoods, 10, 0);
        NativeTradeItem sameGoods = createItem(new Goods(game, null, grainType, 10), 20, 1);
        NativeTradeItem diffGoods = createItem(lumberGoods, 10, 0);
        Predicate<NativeTradeItem> pred = item.goodsMatcher();
        assertTrue(pred.test(sameGoods));
        assertFalse(pred.test(diffGoods));
    }

    // ------------------------------------------------------------------
    // M11: copyIn(FreeColObject) — 2 feasible Test Paths (100% feasible C3)
    // ------------------------------------------------------------------

    // M11-TP1: ENTRY -> N0 -(B1:true)-> N2 -> EXIT (o == null when other is null or wrong type -> false)
    public void testM11_TP1_copyInNullOrWrongType() {
        NativeTradeItem item = createItem(grainGoods, 10, 1);
        assertFalse(item.copyIn(null));
        assertFalse(item.copyIn(grainGoods));
    }

    // M11-TP2: ENTRY -> N0 -(B1:false)-> N1 -(B2:false)-> N2 -> EXIT (o != null, super.copyIn(o) returns false -> false)
    public void testM11_TP2_copyInNativeTradeItemSuperFails() {
        NativeTradeItem item = createItem(grainGoods, 10, 1);
        NativeTradeItem other = createItem(lumberGoods, 88, 7);
        assertFalse(item.copyIn(other));
    }

    // ------------------------------------------------------------------
    // M12: writeAttributes(FreeColXMLWriter) — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M12-TP1: ENTRY -> N0 -> EXIT (writes price and haggleCount attributes to XML)
    public void testM12_TP1_writeAttributes() throws Exception {
        NativeTradeItem item = createItem(grainGoods, 60, 2);
        StringWriter sw = new StringWriter();
        try (FreeColXMLWriter xw = new FreeColXMLWriter(sw, FreeColXMLWriter.WriteScope.toSave())) {
            item.toXML(xw);
        }
        String xml = sw.toString();
        assertTrue(xml.contains("price=\"60\""));
        assertTrue(xml.contains("haggleCount=\"2\""));
    }

    // ------------------------------------------------------------------
    // M13: readAttributes(FreeColXMLReader) — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M13-TP1: ENTRY -> N0 -> EXIT (reads price and haggleCount attributes from XML)
    public void testM13_TP1_readAttributes() throws Exception {
        String xml = "<nativeTradeItem source=\"" + dutch.getId()
            + "\" destination=\"" + tupi.getId()
            + "\" price=\"120\" haggleCount=\"4\">"
            + "<goods type=\"" + grainType.getId() + "\" amount=\"10\"/>"
            + "</nativeTradeItem>";
        NativeTradeItem item = new NativeTradeItem(game, dutch, tupi, grainGoods);
        try (FreeColXMLReader xr = new FreeColXMLReader(new StringReader(xml))) {
            xr.nextTag();
            item.readAttributes(xr);
        }
        assertEquals(120, item.getPrice());
        assertEquals(4, item.getHaggleCount());
    }

    // ------------------------------------------------------------------
    // M14: getXMLTagName() — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M14-TP1: ENTRY -> N0 -> EXIT (returns TAG "nativeTradeItem")
    public void testM14_TP1_getXMLTagName() {
        NativeTradeItem item = new NativeTradeItem(game, dutch, tupi, grainGoods);
        assertEquals(NativeTradeItem.TAG, item.getXMLTagName());
    }

    // ------------------------------------------------------------------
    // M15: equals(Object) — 5 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M15-TP1: ENTRY -> N0 -(B1:true)-> N2 -(B2:true)-> N6 -> N7 -> EXIT (price differs -> false)
    public void testM15_TP1_equalsPriceDiffers() {
        NativeTradeItem a = createItem(grainGoods, 10, 1);
        NativeTradeItem b = createItem(grainGoods, 20, 1);
        assertFalse(a.equals(b));
    }

    // M15-TP2: ENTRY -> N0 -(B1:true)-> N2 -(B2:false)-> N3 -(B3:true)-> N6 -> N7 -> EXIT (price equal, haggleCount differs -> false)
    public void testM15_TP2_equalsHaggleCountDiffers() {
        NativeTradeItem a = createItem(grainGoods, 10, 1);
        NativeTradeItem b = createItem(grainGoods, 10, 2);
        assertFalse(a.equals(b));
    }

    // M15-TP3: ENTRY -> N0 -(B1:true)-> N2 -(B2:false)-> N3 -(B3:false)-> N4 -(B4:true)-> N6 -> N7 -> EXIT (price & haggleCount equal, super.equals false -> false)
    public void testM15_TP3_equalsSuperDiffers() {
        NativeTradeItem a = createItem(grainGoods, 10, 1);
        NativeTradeItem b = createItem(lumberGoods, 10, 1);
        assertFalse(a.equals(b));
    }

    // M15-TP4: ENTRY -> N0 -(B1:true)-> N2 -(B2:false)-> N3 -(B3:false)-> N4 -(B4:false)-> N5 -> N7 -> EXIT (all fields and super equal -> true)
    public void testM15_TP4_equalsTrue() {
        NativeTradeItem a = createItem(grainGoods, 10, 1);
        NativeTradeItem b = createItem(grainGoods, 10, 1);
        assertTrue(a.equals(b));
    }

    // M15-TP5: ENTRY -> N0 -(B1:false)-> N1 -> EXIT (not instanceof NativeTradeItem -> false)
    public void testM15_TP5_equalsNonNativeTradeItem() {
        NativeTradeItem a = createItem(grainGoods, 10, 1);
        assertFalse(a.equals(null));
        assertFalse(a.equals("not a trade item"));
    }

    // ------------------------------------------------------------------
    // M16: hashCode() — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M16-TP1: ENTRY -> N0 -> EXIT (computes deterministic hashCode consistent with equals)
    public void testM16_TP1_hashCode() {
        NativeTradeItem a = createItem(grainGoods, 30, 2);
        NativeTradeItem b = createItem(grainGoods, 30, 2);
        assertEquals(a.hashCode(), b.hashCode());
    }

    // ------------------------------------------------------------------
    // M17: toString() — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M17-TP1: ENTRY -> N0 -> EXIT (formats id, goods amount/name, price, haggleCount)
    public void testM17_TP1_toString() {
        NativeTradeItem item = createItem(grainGoods, 55, 3);
        String str = item.toString();
        assertNotNull(str);
        assertTrue(str.contains("55"));
        assertTrue(str.contains("3"));
    }

    // ------------------------------------------------------------------
    // M18: lambda$static$0(NativeTradeItem) — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M18-TP1: ENTRY -> N0 -> EXIT (invoked by descendingPriceComparator tie-breaker on goods type)
    public void testM18_TP1_lambdaDescendingPriceComparatorTieBreaker() {
        NativeTradeItem a = createItem(grainGoods, 50, 1);
        NativeTradeItem b = createItem(lumberGoods, 50, 1);
        int cmp = NativeTradeItem.descendingPriceComparator.compare(a, b);
        assertEquals(GoodsType.goodsTypeComparator.compare(grainType, lumberType), cmp);
    }

    // ------------------------------------------------------------------
    // M19: <clinit>() — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M19-TP1: ENTRY -> N0 -> EXIT (initializes static TAG, PRICE_UNSET, PRICE_INVALID, descendingPriceComparator)
    public void testM19_TP1_staticInitialization() {
        assertEquals("nativeTradeItem", NativeTradeItem.TAG);
        assertEquals(0, NativeTradeItem.PRICE_UNSET);
        assertEquals(-1, NativeTradeItem.PRICE_INVALID);
        assertNotNull(NativeTradeItem.descendingPriceComparator);
    }
}
