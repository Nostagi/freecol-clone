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

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

import net.sf.freecol.util.test.FreeColTestCase;


/**
 * CFG-driven test suite for {@link MarketWas} derived from
 * {@code test_cft/graph/net/sf/freecol/common/model/MarketWas.json}.
 *
 * Covers all 5 methods (M1..M5, including {@code <init>}) and achieves 100% C3
 * (node + edge) coverage across 11 Test Paths.
 */
public class MarketWasCfgTest extends FreeColTestCase {

    private Game game;
    private Player dutchPlayer;
    private Market market;
    private GoodsType sugarType;
    private GoodsType rumType;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        // Pre-condition: Initialize standard game, Dutch player, Market, and spec GoodsTypes
        game = getStandardGame();
        dutchPlayer = game.getPlayerByNationId("model.nation.dutch");
        market = dutchPlayer.getMarket();
        sugarType = spec().getGoodsType("model.goods.sugar");
        rumType = spec().getGoodsType("model.goods.rum");
    }

    // ------------------------------------------------------------------
    // Pre-condition helpers & stubs
    // ------------------------------------------------------------------

    private static class StubTransactionListener implements TransactionListener {
        int purchases = 0;
        int sales = 0;

        @Override
        public void logPurchase(GoodsType goodsType, int amount, int price) {
            purchases++;
        }

        @Override
        public void logSale(GoodsType goodsType, int amount, int price, int tax) {
            sales++;
        }
    }

    private static void invokeFireChange(MarketWas was, GoodsType type, int amount)
            throws Exception {
        Method m = MarketWas.class.getDeclaredMethod("fireChange", GoodsType.class, int.class);
        m.setAccessible(true);
        m.invoke(was, type, amount);
    }

    // ------------------------------------------------------------------
    // M1: <init>(Lnet/sf/freecol/common/model/Player;)V — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M1-TP1: ENTRY -> N0 -> N1 -(B1:false)-> N2 -> N1 -(B1:true)-> N3 -> EXIT (snapshots market data)
    public void testM1_TP1_Constructor() {
        MarketWas was = new MarketWas(dutchPlayer);
        assertFalse(was.fireChanges());
    }

    // ------------------------------------------------------------------
    // M2: add(Lnet/sf/freecol/common/model/AbstractGoods;)V — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M2-TP1: ENTRY -> N0 -> EXIT (adds a single transaction)
    public void testM2_TP1_AddSingle() {
        MarketWas was = new MarketWas(dutchPlayer);
        was.add(new AbstractGoods(sugarType, 10));
        assertTrue(was.fireChanges());
    }

    // ------------------------------------------------------------------
    // M3: addAll(Ljava/util/List;)V — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M3-TP1: ENTRY -> N0 -> EXIT (adds multiple transactions)
    public void testM3_TP1_AddAll() {
        MarketWas was = new MarketWas(dutchPlayer);
        List<AbstractGoods> list = Arrays.asList(
            new AbstractGoods(sugarType, 5),
            new AbstractGoods(rumType, -5)
        );
        was.addAll(list);
        assertTrue(was.fireChanges());
    }

    // ------------------------------------------------------------------
    // M4: fireChange(Lnet/sf/freecol/common/model/GoodsType;I)V — 6 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M4-TP1: ENTRY -> N0 -> N1 -(B1:true)-> N10 -> EXIT (no TransactionListeners registered)
    public void testM4_TP1_FireChangeNoListeners() throws Exception {
        MarketWas was = new MarketWas(dutchPlayer);
        invokeFireChange(was, sugarType, 10);
    }

    // M4-TP2: ENTRY -> N0 -> N1 -(B1:false)-> N2 -(B2:false)-> N3 -(B3:true)-> N5 -> N9 -> N1 -(B1:true)-> N10 -> EXIT (purchase, price unchanged)
    public void testM4_TP2_FireChangePurchaseSamePrice() throws Exception {
        StubTransactionListener listener = new StubTransactionListener();
        market.addTransactionListener(listener);
        MarketWas was = new MarketWas(dutchPlayer);
        invokeFireChange(was, sugarType, -10);
        assertEquals(1, listener.purchases);
    }

    // M4-TP3: ENTRY -> N0 -> N1 -(B1:false)-> N2 -(B2:false)-> N3 -(B3:false)-> N4 -> N5 -> N9 -> N1 -(B1:true)-> N10 -> EXIT (purchase, price changed)
    public void testM4_TP3_FireChangePurchasePriceChanged() throws Exception {
        StubTransactionListener listener = new StubTransactionListener();
        market.addTransactionListener(listener);
        MarketWas was = new MarketWas(dutchPlayer);
        market.addGoodsToMarket(sugarType, -10000);
        invokeFireChange(was, sugarType, -10000);
        assertEquals(1, listener.purchases);
    }

    // M4-TP4: ENTRY -> N0 -> N1 -(B1:false)-> N2 -(B2:true)-> N6 -(B4:true)-> N9 -> N1 -(B1:true)-> N10 -> EXIT (amount == 0)
    public void testM4_TP4_FireChangeZeroAmount() throws Exception {
        StubTransactionListener listener = new StubTransactionListener();
        market.addTransactionListener(listener);
        MarketWas was = new MarketWas(dutchPlayer);
        invokeFireChange(was, sugarType, 0);
        assertEquals(0, listener.purchases);
        assertEquals(0, listener.sales);
    }

    // M4-TP5: ENTRY -> N0 -> N1 -(B1:false)-> N2 -(B2:true)-> N6 -(B4:false)-> N7 -(B5:true)-> N9 -> N1 -(B1:true)-> N10 -> EXIT (sale, price unchanged)
    public void testM4_TP5_FireChangeSaleSamePrice() throws Exception {
        StubTransactionListener listener = new StubTransactionListener();
        market.addTransactionListener(listener);
        MarketWas was = new MarketWas(dutchPlayer);
        invokeFireChange(was, sugarType, 10);
        assertEquals(1, listener.sales);
    }

    // M4-TP6: ENTRY -> N0 -> N1 -(B1:false)-> N2 -(B2:true)-> N6 -(B4:false)-> N7 -(B5:false)-> N8 -> N9 -> N1 -(B1:true)-> N10 -> EXIT (sale, price changed)
    public void testM4_TP6_FireChangeSalePriceChanged() throws Exception {
        StubTransactionListener listener = new StubTransactionListener();
        market.addTransactionListener(listener);
        MarketWas was = new MarketWas(dutchPlayer);
        market.addGoodsToMarket(sugarType, 10000);
        invokeFireChange(was, sugarType, 10000);
        assertEquals(1, listener.sales);
    }

    // ------------------------------------------------------------------
    // M5: fireChanges()Z — 2 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M5-TP1: ENTRY -> N0 -(B1:false)-> N1 -> EXIT (transactions.isEmpty() == true returns false)
    public void testM5_TP1_FireChangesEmpty() {
        MarketWas was = new MarketWas(dutchPlayer);
        assertFalse(was.fireChanges());
    }

    // M5-TP2: ENTRY -> N0 -(B1:true)-> N2 -> N3 -(B2:false)-> N4 -> N3 -(B2:true)-> N5 -> EXIT (iterates transactions, clears, returns true)
    public void testM5_TP2_FireChangesNonEmpty() {
        MarketWas was = new MarketWas(dutchPlayer);
        was.add(new AbstractGoods(sugarType, 10));
        assertTrue(was.fireChanges());
        assertFalse(was.fireChanges());
    }
}
