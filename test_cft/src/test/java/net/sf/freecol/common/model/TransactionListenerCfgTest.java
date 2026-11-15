package net.sf.freecol.common.model;

import net.sf.freecol.util.test.FreeColTestCase;

/**
 * CFG-driven test suite for {@link TransactionListener} derived from
 * {@code test_cft/graph/net/sf/freecol/common/model/TransactionListener.json}.
 *
 * Interface declares 0 concrete/default methods in CFG JSON; verifies
 * contract via minimal stub implementation.
 */
public class TransactionListenerCfgTest extends FreeColTestCase {

    @Override
    public void setUp() throws Exception {
        super.setUp();
    }

    public void testInterfaceContract() {
        final int[] calls = new int[2];
        TransactionListener listener = new TransactionListener() {
            @Override public void logPurchase(GoodsType goodsType, int amount, int price) { calls[0]++; }
            @Override public void logSale(GoodsType goodsType, int amount, int price, int tax) { calls[1]++; }
        };
        GoodsType food = spec().getGoodsType("model.goods.food");
        listener.logPurchase(food, 10, 5);
        listener.logSale(food, 10, 4, 10);
        assertEquals(1, calls[0]);
        assertEquals(1, calls[1]);
    }
}
