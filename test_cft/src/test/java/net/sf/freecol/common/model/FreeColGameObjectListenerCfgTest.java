package net.sf.freecol.common.model;

import net.sf.freecol.util.test.FreeColTestCase;

/**
 * CFG-driven test suite for {@link FreeColGameObjectListener} derived from
 * {@code test_cft/graph/net/sf/freecol/common/model/FreeColGameObjectListener.json}.
 *
 * Interface declares 0 concrete/default methods in CFG JSON; verifies
 * contract via minimal stub implementation.
 */
public class FreeColGameObjectListenerCfgTest extends FreeColTestCase {

    @Override
    public void setUp() throws Exception {
        super.setUp();
    }

    public void testInterfaceContract() {
        final int[] counts = new int[3];
        FreeColGameObjectListener listener = new FreeColGameObjectListener() {
            @Override public void setFreeColGameObject(String id, FreeColGameObject fcgo) { counts[0]++; }
            @Override public void removeFreeColGameObject(String id) { counts[1]++; }
            @Override public void ownerChanged(FreeColGameObject source, Player oldOwner, Player newOwner) { counts[2]++; }
        };
        listener.setFreeColGameObject("id:1", null);
        listener.removeFreeColGameObject("id:1");
        listener.ownerChanged(null, null, null);
        assertEquals(1, counts[0]);
        assertEquals(1, counts[1]);
        assertEquals(1, counts[2]);
    }
}
