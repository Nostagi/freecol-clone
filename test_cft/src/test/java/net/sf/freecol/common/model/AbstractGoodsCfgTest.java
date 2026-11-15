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
import java.util.function.Predicate;

import net.sf.freecol.util.test.FreeColTestCase;


/**
 * CFG-driven test suite for {@link AbstractGoods} derived from
 * {@code test_cft/graph/net/sf/freecol/common/model/AbstractGoods.json}.
 *
 * Covers all 24 methods (M1..M24, including all {@code <init>} overloads and
 * {@code <clinit>}) and achieves 100% feasible C3 (node + edge) coverage across
 * 34 Test Paths.
 *
 * Infeasible edges documented:
 * - M19 ({@code copyIn}): {@code N1 -> N2} ({@code B2:false}, {@code !super.copyIn(o)}) —
 *   When {@code o != null}, {@code copyInCast(other, AbstractGoods.class)} has
 *   already verified {@code other != null}, {@code idEquals(other)}, and
 *   {@code other instanceof AbstractGoods}; {@link FreeColObject#copyIn(FreeColObject)}
 *   only checks {@code copyInCast(o, FreeColObject.class) != null}, which is
 *   guaranteed {@code true}.
 */
public class AbstractGoodsCfgTest extends FreeColTestCase {

    private Specification spec;
    private GoodsType foodType;
    private GoodsType lumberType;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        // Pre-condition: Obtain standard specification and common GoodsTypes
        spec = spec();
        foodType = spec.getGoodsType("model.goods.food");
        lumberType = spec.getGoodsType("model.goods.lumber");
    }

    // ------------------------------------------------------------------
    // Pre-condition helpers & stubs
    // ------------------------------------------------------------------

    /**
     * Minimal {@link FreeColObject} stub (not an {@link AbstractGoods})
     * used to test {@code copyInCast} type mismatch.
     */
    private static class StubPlainFreeColObject extends FreeColObject {

        StubPlainFreeColObject(String id) {
            setId(id);
        }

        @Override
        public String getXMLTagName() {
            return "stubPlainFreeColObject";
        }
    }

    // ------------------------------------------------------------------
    // M1: <init>()V — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M1-TP1: ENTRY -> N0 -> EXIT (No-arg constructor)
    public void testM1_TP1_NoArgConstructor() {
        AbstractGoods ag = new AbstractGoods();
        assertNull(ag.getType());
        assertEquals(0, ag.getAmount());
    }

    // ------------------------------------------------------------------
    // M2: <init>(Lnet/sf/freecol/common/model/GoodsType;I)V — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M2-TP1: ENTRY -> N0 -> EXIT (Constructor with type and amount)
    public void testM2_TP1_TypeAndAmountConstructor() {
        AbstractGoods ag = new AbstractGoods(foodType, 50);
        assertSame(foodType, ag.getType());
        assertEquals(50, ag.getAmount());
        assertEquals(foodType.getId(), ag.getId());
    }

    // ------------------------------------------------------------------
    // M3: <init>(Lnet/sf/freecol/common/model/AbstractGoods;)V — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M3-TP1: ENTRY -> N0 -> EXIT (Copy constructor)
    public void testM3_TP1_CopyConstructor() {
        AbstractGoods original = new AbstractGoods(lumberType, 25);
        AbstractGoods copy = new AbstractGoods(original);
        assertSame(lumberType, copy.getType());
        assertEquals(25, copy.getAmount());
        assertEquals(lumberType.getId(), copy.getId());
    }

    // ------------------------------------------------------------------
    // M4: getType()Lnet/sf/freecol/common/model/GoodsType; — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M4-TP1: ENTRY -> N0 -> EXIT (Return type)
    public void testM4_TP1_GetType() {
        AbstractGoods ag = new AbstractGoods(foodType, 10);
        assertSame(foodType, ag.getType());
    }

    // ------------------------------------------------------------------
    // M5: setType(Lnet/sf/freecol/common/model/GoodsType;)V — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M5-TP1: ENTRY -> N0 -> EXIT (Update type)
    public void testM5_TP1_SetType() {
        AbstractGoods ag = new AbstractGoods(foodType, 10);
        ag.setType(lumberType);
        assertSame(lumberType, ag.getType());
    }

    // ------------------------------------------------------------------
    // M6: isFoodType()Z — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M6-TP1: ENTRY -> N0 -> EXIT (Delegate isFoodType() to GoodsType)
    public void testM6_TP1_IsFoodType() {
        assertTrue(new AbstractGoods(foodType, 10).isFoodType());
        assertFalse(new AbstractGoods(lumberType, 10).isFoodType());
    }

    // ------------------------------------------------------------------
    // M7: getAmount()I — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M7-TP1: ENTRY -> N0 -> EXIT (Return amount)
    public void testM7_TP1_GetAmount() {
        assertEquals(33, new AbstractGoods(foodType, 33).getAmount());
    }

    // ------------------------------------------------------------------
    // M8: setAmount(I)V — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M8-TP1: ENTRY -> N0 -> EXIT (Update amount)
    public void testM8_TP1_SetAmount() {
        AbstractGoods ag = new AbstractGoods(foodType, 10);
        ag.setAmount(99);
        assertEquals(99, ag.getAmount());
    }

    // ------------------------------------------------------------------
    // M9: isPositive()Z — 2 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M9-TP1: ENTRY -> N0 -(B1:true)-> N2 -> N3 -> EXIT (amount <= 0)
    public void testM9_TP1_IsPositiveFalse() {
        assertFalse(new AbstractGoods(foodType, 0).isPositive());
        assertFalse(new AbstractGoods(foodType, -5).isPositive());
    }

    // M9-TP2: ENTRY -> N0 -(B1:false)-> N1 -> N3 -> EXIT (amount > 0)
    public void testM9_TP2_IsPositiveTrue() {
        assertTrue(new AbstractGoods(foodType, 1).isPositive());
    }

    // ------------------------------------------------------------------
    // M10: isStorable()Z — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M10-TP1: ENTRY -> N0 -> EXIT (Delegate isStorable() to GoodsType)
    public void testM10_TP1_IsStorable() {
        assertTrue(new AbstractGoods(lumberType, 10).isStorable());
    }

    // ------------------------------------------------------------------
    // M11: getLabel()Lnet/sf/freecol/common/model/StringTemplate; — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M11-TP1: ENTRY -> N0 -> EXIT (Build label StringTemplate)
    public void testM11_TP1_GetLabel() {
        StringTemplate st = new AbstractGoods(foodType, 12).getLabel();
        assertNotNull(st);
        assertEquals("model.abstractGoods.label", st.getId());
    }

    // ------------------------------------------------------------------
    // M12: getLabel(Z)Lnet/sf/freecol/common/model/StringTemplate; — 2 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M12-TP1: ENTRY -> N0 -(B1:true)-> N2 -> N3 -> EXIT (sellable == false -> boycotted template)
    public void testM12_TP1_GetLabelNotSellable() {
        StringTemplate st = new AbstractGoods(foodType, 12).getLabel(false);
        assertNotNull(st);
        assertEquals("model.abstractGoods.boycotted", st.getId());
    }

    // M12-TP2: ENTRY -> N0 -(B1:false)-> N1 -> N3 -> EXIT (sellable == true -> normal label)
    public void testM12_TP2_GetLabelSellable() {
        StringTemplate st = new AbstractGoods(foodType, 12).getLabel(true);
        assertNotNull(st);
        assertEquals("model.abstractGoods.label", st.getId());
    }

    // ------------------------------------------------------------------
    // M13: getAbstractLabel(Ljava/lang/String;I)Lnet/sf/freecol/common/model/StringTemplate; — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M13-TP1: ENTRY -> N0 -> EXIT (Build abstract label StringTemplate)
    public void testM13_TP1_GetAbstractLabel() {
        StringTemplate st = AbstractGoods.getAbstractLabel("model.goods.food", 5);
        assertNotNull(st);
        assertEquals("model.abstractGoods.label", st.getId());
    }

    // ------------------------------------------------------------------
    // M14: getCount(Lnet/sf/freecol/common/model/GoodsType;Ljava/util/Collection;)I — 2 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M14-TP1: ENTRY -> N0 -(B1:true)-> N2 -> N3 -> EXIT (Matching AbstractGoods found)
    public void testM14_TP1_GetCountFound() {
        AbstractGoods ag = new AbstractGoods(lumberType, 40);
        assertEquals(40, AbstractGoods.getCount(lumberType, Collections.singletonList(ag)));
    }

    // M14-TP2: ENTRY -> N0 -(B1:false)-> N1 -> N3 -> EXIT (No matching AbstractGoods found)
    public void testM14_TP2_GetCountNotFound() {
        AbstractGoods ag = new AbstractGoods(foodType, 40);
        assertEquals(0, AbstractGoods.getCount(lumberType, Collections.singletonList(ag)));
    }

    // ------------------------------------------------------------------
    // M15: evaluateFor(Lnet/sf/freecol/common/model/Player;)I — 2 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M15-TP1: ENTRY -> N0 -(B1:true)-> N2 -> N3 -> EXIT (player.getMarket() != null)
    public void testM15_TP1_EvaluateForWithMarket() {
        Game game = getStandardGame();
        Player dutch = game.getPlayerByNationId("model.nation.dutch");
        AbstractGoods ag = new AbstractGoods(lumberType, 10);
        assertEquals(dutch.getMarket().getSalePrice(lumberType, 10), ag.evaluateFor(dutch));
    }

    // M15-TP2: ENTRY -> N0 -(B1:false)-> N1 -> N3 -> EXIT (player.getMarket() == null)
    public void testM15_TP2_EvaluateForWithoutMarket() {
        Game game = getStandardGame();
        Player noMarket = new Player(game, "player:nomarket");
        assertNull(noMarket.getMarket());
        AbstractGoods ag = new AbstractGoods(lumberType, 10);
        assertEquals(20, ag.evaluateFor(noMarket));
    }

    // ------------------------------------------------------------------
    // M16: matches(Lnet/sf/freecol/common/model/GoodsType;)Ljava/util/function/Predicate; — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M16-TP1: ENTRY -> N0 -> EXIT (Return Predicate matching GoodsType)
    public void testM16_TP1_Matches() {
        Predicate<? super AbstractGoods> pred = AbstractGoods.matches(foodType);
        assertTrue(pred.test(new AbstractGoods(foodType, 5)));
        assertFalse(pred.test(new AbstractGoods(lumberType, 5)));
    }

    // ------------------------------------------------------------------
    // M17: getNameKey()Ljava/lang/String; — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M17-TP1: ENTRY -> N0 -> EXIT (Delegate getNameKey() to getType().getNameKey())
    public void testM17_TP1_GetNameKey() {
        AbstractGoods ag = new AbstractGoods(foodType, 10);
        assertEquals(foodType.getNameKey(), ag.getNameKey());
    }

    // ------------------------------------------------------------------
    // M18: getXMLTagName()Ljava/lang/String; — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M18-TP1: ENTRY -> N0 -> EXIT (Return null)
    public void testM18_TP1_GetXMLTagName() {
        assertNull(new AbstractGoods(foodType, 10).getXMLTagName());
    }

    // ------------------------------------------------------------------
    // M19: copyIn(Lnet/sf/freecol/common/model/FreeColObject;)Z — 2 feasible Test Paths (100% feasible C3)
    // ------------------------------------------------------------------

    // M19-TP1: ENTRY -> N0 -(B1:true)-> N2 -> EXIT (o == null via null, wrong type, or mismatched ID)
    public void testM19_TP1_CopyInReturnsFalseWhenCastNull() {
        AbstractGoods target = new AbstractGoods(foodType, 10);
        assertFalse(target.copyIn(null));
        assertFalse(target.copyIn(new StubPlainFreeColObject(foodType.getId())));
        assertFalse(target.copyIn(new AbstractGoods(lumberType, 20)));
    }

    // M19-TP2: ENTRY -> N0 -(B1:false)-> N1 -(B2:true)-> N3 -> EXIT (o != null && super.copyIn(o) == true)
    public void testM19_TP2_CopyInCopiesFieldsWhenCompatible() {
        AbstractGoods target = new AbstractGoods(foodType, 10);
        AbstractGoods source = new AbstractGoods(foodType, 75);
        source.setType(lumberType); // keep same id as foodType so copyInCast succeeds

        assertTrue(target.copyIn(source));
        assertSame(lumberType, target.getType());
        assertEquals(75, target.getAmount());
    }

    // ------------------------------------------------------------------
    // M20: equals(Ljava/lang/Object;)Z — 6 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M20-TP1: ENTRY -> N0 -(B1:false)-> N1 -> EXIT (this == o, reflexive)
    public void testM20_TP1_EqualsSameInstance() {
        AbstractGoods ag = new AbstractGoods(foodType, 10);
        assertTrue(ag.equals(ag));
    }

    // M20-TP2: ENTRY -> N0 -(B1:true)-> N2 -(B2:true)-> N9 -> EXIT (!(o instanceof AbstractGoods))
    public void testM20_TP2_EqualsNonAbstractGoods() {
        AbstractGoods ag = new AbstractGoods(foodType, 10);
        assertFalse(ag.equals(null));
        assertFalse(ag.equals("notGoods"));
    }

    // M20-TP3: ENTRY -> N0 -(B1:true)-> N2 -(B2:false)-> N3 -(B3:true)-> N7 -> N8 -> EXIT (this.type != other.type)
    public void testM20_TP3_EqualsDifferentType() {
        AbstractGoods ag1 = new AbstractGoods(foodType, 10);
        AbstractGoods ag2 = new AbstractGoods(lumberType, 10);
        assertFalse(ag1.equals(ag2));
    }

    // M20-TP4: ENTRY -> N0 -(B1:true)-> N2 -(B2:false)-> N3 -(B3:false)-> N4 -(B4:true)-> N7 -> N8 -> EXIT (same type, different amount)
    public void testM20_TP4_EqualsDifferentAmount() {
        AbstractGoods ag1 = new AbstractGoods(foodType, 10);
        AbstractGoods ag2 = new AbstractGoods(foodType, 20);
        assertFalse(ag1.equals(ag2));
    }

    // M20-TP5: ENTRY -> N0 -(B1:true)-> N2 -(B2:false)-> N3 -(B3:false)-> N4 -(B4:false)-> N5 -(B5:true)-> N7 -> N8 -> EXIT (same type & amount, super.equals(other) == false)
    public void testM20_TP5_EqualsDifferentSuperId() {
        AbstractGoods ag1 = new AbstractGoods(foodType, 10);
        AbstractGoods ag2 = new AbstractGoods(foodType, 10);
        ag2.setId("different.id");
        assertFalse(ag1.equals(ag2));
    }

    // M20-TP6: ENTRY -> N0 -(B1:true)-> N2 -(B2:false)-> N3 -(B3:false)-> N4 -(B4:false)-> N5 -(B5:false)-> N6 -> N8 -> EXIT (distinct instance, equal type, amount, and id)
    public void testM20_TP6_EqualsEqualObjects() {
        AbstractGoods ag1 = new AbstractGoods(foodType, 10);
        AbstractGoods ag2 = new AbstractGoods(foodType, 10);
        assertTrue(ag1.equals(ag2));
    }

    // ------------------------------------------------------------------
    // M21: hashCode()I — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M21-TP1: ENTRY -> N0 -> EXIT (Compute consistent hashCode)
    public void testM21_TP1_HashCode() {
        AbstractGoods ag1 = new AbstractGoods(foodType, 10);
        AbstractGoods ag2 = new AbstractGoods(foodType, 10);
        assertEquals(ag1.hashCode(), ag2.hashCode());
    }

    // ------------------------------------------------------------------
    // M22: toString()Ljava/lang/String; — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M22-TP1: ENTRY -> N0 -> EXIT (Delegate toString() to toFullString)
    public void testM22_TP1_ToString() {
        AbstractGoods ag = new AbstractGoods(foodType, 15);
        assertEquals("15 " + foodType.getSuffix(), ag.toString());
    }

    // ------------------------------------------------------------------
    // M23: toFullString(Lnet/sf/freecol/common/model/GoodsType;I)Ljava/lang/String; — 2 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M23-TP1: ENTRY -> N0 -(B1:true)-> N2 -> N3 -> EXIT (goodsType != null)
    public void testM23_TP1_ToFullStringNonNullType() {
        assertEquals("7 " + lumberType.getSuffix(), AbstractGoods.toFullString(lumberType, 7));
    }

    // M23-TP2: ENTRY -> N0 -(B1:false)-> N1 -> N3 -> EXIT (goodsType == null)
    public void testM23_TP2_ToFullStringNullType() {
        assertEquals("7 (null)", AbstractGoods.toFullString(null, 7));
    }

    // ------------------------------------------------------------------
    // M24: <clinit>()V — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M24-TP1: ENTRY -> N0 -> EXIT (Static comparators initialized)
    public void testM24_TP1_ClassInitComparators() {
        AbstractGoods low = new AbstractGoods(foodType, 5);
        AbstractGoods high = new AbstractGoods(lumberType, 10);
        assertTrue(AbstractGoods.ascendingAmountComparator.compare(low, high) < 0);
        assertTrue(AbstractGoods.descendingAmountComparator.compare(low, high) > 0);
    }
}
