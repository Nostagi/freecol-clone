package net.sf.freecol.common.model;

import net.sf.freecol.util.test.FreeColTestCase;

/**
 * CFG-driven test suite for {@link Ownable} derived from
 * {@code test_cft/graph/net/sf/freecol/common/model/Ownable.json}.
 *
 * Interface declares 0 concrete/default methods in CFG JSON; verifies
 * contract via minimal stub implementation.
 */
public class OwnableCfgTest extends FreeColTestCase {

    @Override
    public void setUp() throws Exception {
        super.setUp();
    }

    public void testInterfaceContract() {
        Game game = getStandardGame();
        Player dutch = game.getPlayerByNationId("model.nation.dutch");
        Ownable ownable = new Ownable() {
            private Player owner = dutch;
            @Override public Player getOwner() { return owner; }
            @Override public void setOwner(Player p) { this.owner = p; }
        };
        assertEquals(dutch, ownable.getOwner());
        ownable.setOwner(null);
        assertNull(ownable.getOwner());
    }
}
