package net.sf.freecol.common.model;

import net.sf.freecol.util.test.FreeColTestCase;

/**
 * CFG-driven test suite for {@link Locatable} derived from
 * {@code test_cft/graph/net/sf/freecol/common/model/Locatable.json}.
 *
 * Interface declares 0 concrete/default methods in CFG JSON; verifies
 * contract via minimal stub implementation.
 */
public class LocatableCfgTest extends FreeColTestCase {

    @Override
    public void setUp() throws Exception {
        super.setUp();
    }

    public void testInterfaceContract() {
        Locatable loc = new Locatable() {
            private Location current;
            @Override public Location getLocation() { return current; }
            @Override public boolean setLocation(Location newLocation) { this.current = newLocation; return true; }
            @Override public boolean isInEurope() { return false; }
            @Override public Tile getTile() { return null; }
            @Override public int getSpaceTaken() { return 1; }
        };
        assertNull(loc.getLocation());
        assertTrue(loc.setLocation(null));
        assertFalse(loc.isInEurope());
        assertNull(loc.getTile());
        assertEquals(1, loc.getSpaceTaken());
    }
}
