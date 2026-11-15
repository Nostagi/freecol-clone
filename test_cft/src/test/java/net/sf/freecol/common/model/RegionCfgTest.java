package net.sf.freecol.common.model;

import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.BlockJUnit4ClassRunner;

import net.sf.freecol.util.test.MockModelTestCase;


@RunWith(BlockJUnit4ClassRunner.class)
public class RegionCfgTest extends MockModelTestCase {

    @Test
    public void testChildrenAndDiscoverableParent() {
        Game game = getStandardGame();
        Region parent = new Region(game);
        Region child = new Region(game);

        assertTrue(parent.isLeaf());
        assertTrue(parent.getChildren().isEmpty());
        assertNull(child.getDiscoverableRegion());

        parent.setDiscoverable(true);
        child.setParent(parent);
        assertSame(parent, child.getDiscoverableRegion());
        assertTrue(child.isLeaf());

        parent.addChild(child);
        assertFalse(parent.isLeaf());
        assertEquals(1, parent.getChildren().size());
        assertSame(child, parent.getChildren().get(0));
        assertFalse(parent.getDiscoverableRegion() == null);

        Region named = new Region(game);
        assertFalse(named.hasName());
        named.setName("Coast");
        assertTrue(named.hasName());
        assertEquals("Coast", named.getName());
    }

    @Test
    public void testDiscoveryReservesAndRevealsChildRegions() {
        Game game = getStandardGame();
        Player dutch = game.getPlayerByNationId("model.nation.dutch");
        Unit unit = new net.sf.freecol.server.model.ServerUnit(game, null, dutch,
            spec().getUnitType("model.unit.freeColonist"));
        Unit other = new net.sf.freecol.server.model.ServerUnit(game, null, dutch,
            spec().getUnitType("model.unit.freeColonist"));
        Turn turn = new Turn(20);
        Region parent = new Region(game);
        Region child = new Region(game);
        parent.setDiscoverable(true);
        child.setDiscoverable(true);
        parent.addChild(child);
        Region undiscoverable = new Region(game);

        assertTrue(parent.discover(dutch, unit, turn).isEmpty());
        assertFalse(undiscoverable.checkDiscover(unit));
        assertTrue(parent.checkDiscover(unit));
        assertTrue(parent.checkDiscover(unit));
        assertFalse(parent.checkDiscover(other));
        assertTrue(parent.discover(dutch, other, turn).isEmpty());
        assertTrue(parent.discover(dutch, null, turn).isEmpty());
        assertTrue(parent.getDiscoverable());

        List<Region> discovered = parent.discover(dutch, unit, turn);
        assertEquals(2, discovered.size());
        assertSame(parent, discovered.get(0));
        assertSame(child, discovered.get(1));
        assertSame(dutch, child.getDiscoveredBy());
        assertSame(turn, child.getDiscoveredIn());
        assertEquals(unit.getId(), child.getDiscoverer());
        assertFalse(parent.getDiscoverable());
        assertFalse(child.getDiscoverable());
    }

    @Test
    public void testDiscoverabilitySearchStopsAtNonDiscoverableRoot() {
        Game game = getStandardGame();
        Region root = new Region(game);
        Region middle = new Region(game);
        Region leaf = new Region(game);
        middle.setParent(root);
        leaf.setParent(middle);

        assertNull(leaf.getDiscoverableRegion());
        middle.setDiscoverable(true);
        assertSame(middle, leaf.getDiscoverableRegion());
        root.setDiscoverable(true);
        assertSame(middle, leaf.getDiscoverableRegion());
        middle.setDiscoverable(false);
        assertSame(root, leaf.getDiscoverableRegion());
    }
}
