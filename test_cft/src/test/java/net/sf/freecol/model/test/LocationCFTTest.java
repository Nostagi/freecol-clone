package net.sf.freecol.common.model;

import net.sf.freecol.util.test.FreeColTestCase;

/**
 * Tests for the Location interface.
 *
 * The tests are written in the JUnit 3 style used by FreeCol.
 */
public class LocationCFTTest extends FreeColTestCase {

    private Game game;
    private Colony colony;

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        game = getGame();
        game.setMap(getTestMap(true));

        colony = createStandardColony();
    }


    public void testUpLocWithNull() {
        assertNull(Location.upLoc(null));
    }


    public void testUpLocWithLocation() {
        Location location = colony;

        assertSame(location.up(), Location.upLoc(location));
    }


    public void testRankOfWithNull() {
        assertEquals(Location.LOCATION_RANK_NOWHERE,
                     Location.rankOf(null));
    }


    public void testRankOfWithLocation() {
        Location location = colony;

        assertEquals(location.getRank(), Location.rankOf(location));
    }


    public void testGetLocationImageKeyDefault() {
    Location location = new Location() {
        @Override
        public String getId() {
            return "test.location";
        }

        @Override
        public Tile getTile() {
            return null;
        }

        @Override
        public StringTemplate getLocationLabel() {
            return null;
        }

        @Override
        public StringTemplate getLocationLabelFor(Player player) {
            return null;
        }

        @Override
        public boolean add(Locatable locatable) {
            return false;
        }

        @Override
        public boolean remove(Locatable locatable) {
            return false;
        }

        @Override
        public boolean contains(Locatable locatable) {
            return false;
        }

        @Override
        public boolean canAdd(Locatable locatable) {
            return false;
        }

        @Override
        public int getUnitCount() {
            return 0;
        }

        @Override
        public java.util.stream.Stream<Unit> getUnits() {
            return java.util.stream.Stream.empty();
        }

        @Override
        public java.util.List<Unit> getUnitList() {
            return java.util.Collections.emptyList();
        }

        @Override
        public GoodsContainer getGoodsContainer() {
            return null;
        }

        @Override
        public Settlement getSettlement() {
            return null;
        }

        @Override
        public Colony getColony() {
            return null;
        }

        @Override
        public IndianSettlement getIndianSettlement() {
            return null;
        }

        @Override
        public Location up() {
            return this;
        }

        @Override
        public int getRank() {
            return 0;
        }

        @Override
        public String toShortString() {
            return "test";
        }
    };

    assertNull(location.getLocationImageKey());
}
}