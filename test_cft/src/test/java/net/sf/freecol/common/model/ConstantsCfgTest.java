package net.sf.freecol.common.model;

import net.sf.freecol.util.test.FreeColTestCase;

/**
 * CFG-driven test suite for {@link Constants} derived from
 * {@code test_cft/graph/net/sf/freecol/common/model/Constants.json}.
 *
 * Interface declares 0 methods in CFG JSON; verifies core constants.
 */
public class ConstantsCfgTest extends FreeColTestCase {

    @Override
    public void setUp() throws Exception {
        super.setUp();
    }

    public void testInterfaceConstants() {
        assertEquals(Integer.MAX_VALUE, Constants.INFINITY);
        assertEquals(Integer.MIN_VALUE, Constants.UNDEFINED);
        assertTrue(Constants.IntegrityType.INTEGRITY_GOOD.safe());
        assertFalse(Constants.IntegrityType.INTEGRITY_FAIL.safe());
    }
}
