package net.sf.freecol.common.model;

import net.sf.freecol.util.test.FreeColTestCase;

/**
 * CFG-driven test suite for {@link Named} derived from
 * {@code test_cft/graph/net/sf/freecol/common/model/Named.json}.
 *
 * Interface declares 0 concrete/default methods in CFG JSON; verifies
 * contract via minimal stub implementation.
 */
public class NamedCfgTest extends FreeColTestCase {

    @Override
    public void setUp() throws Exception {
        super.setUp();
    }

    public void testInterfaceContract() {
        Named named = () -> "model.goods.food.name";
        assertEquals("model.goods.food.name", named.getNameKey());
    }
}
