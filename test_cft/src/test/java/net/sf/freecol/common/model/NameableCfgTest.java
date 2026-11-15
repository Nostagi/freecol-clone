package net.sf.freecol.common.model;

import net.sf.freecol.util.test.FreeColTestCase;

/**
 * CFG-driven test suite for {@link Nameable} derived from
 * {@code test_cft/graph/net/sf/freecol/common/model/Nameable.json}.
 *
 * Interface declares 0 concrete/default methods in CFG JSON; verifies
 * contract via minimal stub implementation.
 */
public class NameableCfgTest extends FreeColTestCase {

    @Override
    public void setUp() throws Exception {
        super.setUp();
    }

    public void testInterfaceContract() {
        Nameable nameable = new Nameable() {
            private String name = "Initial";
            @Override public String getName() { return name; }
            @Override public void setName(String newName) { this.name = newName; }
        };
        assertEquals("Initial", nameable.getName());
        nameable.setName("Renamed");
        assertEquals("Renamed", nameable.getName());
    }
}
