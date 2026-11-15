package net.sf.freecol.common.model;

import net.sf.freecol.util.test.FreeColTestCase;

/**
 * CFG-driven test suite for {@link Movable} derived from
 * {@code test_cft/graph/net/sf/freecol/common/model/Movable.json}.
 *
 * Interface declares 0 concrete/default methods in CFG JSON; verifies
 * contract via minimal stub implementation.
 */
public class MovableCfgTest extends FreeColTestCase {

    @Override
    public void setUp() throws Exception {
        super.setUp();
    }

    public void testInterfaceContract() {
        Movable m = new Movable() {
            @Override public int getMovesLeft() { return 3; }
            @Override public int getInitialMovesLeft() { return 6; }
        };
        assertEquals(3, m.getMovesLeft());
        assertEquals(6, m.getInitialMovesLeft());
    }
}
