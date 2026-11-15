
package net.sf.freecol.common.model;

import java.io.StringReader;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import javax.xml.stream.XMLStreamException;

import net.sf.freecol.common.io.FreeColXMLReader;
import net.sf.freecol.common.io.FreeColXMLWriter;
import net.sf.freecol.util.test.FreeColTestCase;


public class GoodsLocationCFTTest extends FreeColTestCase {

    private Game game;
    private Specification spec;
    private GoodsType food;
    private TestGoodsLocation location;


    private static class TestGoodsLocation extends GoodsLocation {

        private int invalidationCount = 0;
        private int capacity = 100;

        TestGoodsLocation(Game game, String id) {
            super(game, id);
        }

        TestGoodsLocation(Game game) {
            super(game);
        }

        @Override
        public void invalidateCache() {
            invalidationCount++;
        }

        @Override
        public Location up() {
            return null;
        }

        @Override
        public int getGoodsCapacity() {
            return capacity;
        }

        @Override
        public String getXMLTagName() {
            return "testGoodsLocation";
        }

        @Override
        public String toShortString() {
            return getId();
        }

        @Override
        public int getRank() {
            return 0;
        }

        int getInvalidationCount() {
            return invalidationCount;
        }

        void setCapacity(int capacity) {
            this.capacity = capacity;
        }
    }


    private static class TestLocatable implements Locatable {

        private Location location;

        @Override
        public Location getLocation() {
            return location;
        }

        @Override
        public boolean setLocation(Location newLocation) {
            location = newLocation;
            return true;
        }

        @Override
        public boolean isInEurope() {
            return false;
        }

        @Override
        public Tile getTile() {
            return null;
        }

        @Override
        public int getSpaceTaken() {
            return 0;
        }
    }


    @Override
    public void setUp() throws Exception {
        super.setUp();

        game = getGame();
        spec = spec();
        food = spec.getGoodsType("model.goods.food");

        assertNotNull(food);

        location = new TestGoodsLocation(game, "test.goods.location");
    }


    private void ensureContainer(TestGoodsLocation target) {
        if (target.getGoodsContainer() == null) {
            target.addGoods(food, 0);
        }
    }


    private Goods createGoods(GoodsType type, int amount) {
        return new Goods(game, null, type, amount);
    }


    // Constructors and container

    public void testConstructor() {
        TestGoodsLocation result =
            new TestGoodsLocation(game, "test.constructor");

        assertNotNull(result);
        assertNull(result.getGoodsContainer());
    }


    public void testConstructorWithGameOnly() {
        TestGoodsLocation result = new TestGoodsLocation(game);

        assertNotNull(result);
        assertNotNull(result.getId());
        assertNull(result.getGoodsContainer());
    }


    public void testSetGoodsContainer() {
        GoodsContainer container = new GoodsContainer(game, location);

        location.setGoodsContainer(container);

        assertSame(container, location.getGoodsContainer());
    }


    public void testGetGoodsCountWithoutContainer() {
        assertEquals(0, location.getGoodsCount(food));
    }


    public void testGetGoodsListWithoutContainer() {
        assertNotNull(location.getGoodsList());
        assertTrue(location.getGoodsList().isEmpty());
    }


    public void testGetCompactGoodsListWithoutContainer() {
        assertNotNull(location.getCompactGoodsList());
        assertTrue(location.getCompactGoodsList().isEmpty());
    }


    // Add goods

    public void testAddGoodsByType() {
        assertTrue(location.addGoods(food, 50));

        assertEquals(50, location.getGoodsCount(food));
        assertNotNull(location.getGoodsContainer());
        assertTrue(location.getInvalidationCount() > 0);
    }


    public void testAddGoodsByAbstractGoods() {
        AbstractGoods goods = new AbstractGoods(food, 30);

        assertTrue(location.addGoods(goods));

        assertEquals(30, location.getGoodsCount(food));
    }


    public void testAddGoodsList() {
        AbstractGoods first = new AbstractGoods(food, 20);
        AbstractGoods second = new AbstractGoods(food, 30);

        assertTrue(location.addGoods(Arrays.asList(first, second)));

        assertEquals(50, location.getGoodsCount(food));
    }


    public void testAddGoodsEmptyList() {
        assertTrue(location.addGoods(Collections.emptyList()));
        assertNull(location.getGoodsContainer());
    }


    public void testAddGoodsListMultipleEntries() {
        List<AbstractGoods> goods = Arrays.asList(
            new AbstractGoods(food, 10),
            new AbstractGoods(food, 20));

        assertTrue(location.addGoods(goods));
        assertEquals(30, location.getGoodsCount(food));
    }


    public void testAddGoodsZeroAmount() {
        assertTrue(location.addGoods(food, 0));

        assertNotNull(location.getGoodsContainer());
        assertEquals(0, location.getGoodsCount(food));
    }


    public void testAddGoodsTwiceReusesContainer() {
        location.addGoods(food, 10);
        GoodsContainer first = location.getGoodsContainer();

        location.addGoods(food, 15);

        assertSame(first, location.getGoodsContainer());
        assertEquals(25, location.getGoodsCount(food));
    }


    // Remove goods

    public void testRemoveGoodsByTypeAndAmount() {
        location.addGoods(food, 80);

        Goods removed = location.removeGoods(food, 30);

        assertNotNull(removed);
        assertEquals(30, removed.getAmount());
        assertEquals(50, location.getGoodsCount(food));
    }


    public void testRemoveGoodsWithoutContainer() {
        assertNull(location.removeGoods(food, 10));
    }


    public void testRemoveGoodsAbstractGoods() {
        location.addGoods(food, 80);

        Goods removed = location.removeGoods(new AbstractGoods(food, 25));

        assertNotNull(removed);
        assertEquals(25, removed.getAmount());
        assertEquals(55, location.getGoodsCount(food));
    }


    public void testRemoveGoodsAllOfType() {
        location.addGoods(food, 60);

        Goods removed = location.removeGoods(food);

        assertNotNull(removed);
        assertEquals(60, removed.getAmount());
        assertEquals(0, location.getGoodsCount(food));
    }


    public void testRemoveGoodsWhenRequestedAmountExceedsAvailable() {
        location.addGoods(food, 10);

        Goods removed = location.removeGoods(food, 20);

        assertNotNull(removed);
        assertEquals(10, removed.getAmount());
        assertEquals(0, location.getGoodsCount(food));
    }


    public void testRemoveGoodsWhenNoneAvailable() {
        location.addGoods(food, 0);

        Goods removed = location.removeGoods(food, 10);

        assertNull(removed);
        assertEquals(0, location.getGoodsCount(food));
    }


    public void testRemoveGoodsZeroAmount() {
        location.addGoods(food, 20);

        location.removeGoods(food, 0);

        assertEquals(20, location.getGoodsCount(food));
    }


    public void testRemoveGoodsOtherTypeWhenContainerExists() {
        GoodsType muskets = spec.getGoodsType("model.goods.muskets");
        assertNotNull(muskets);

        location.addGoods(food, 20);

        Goods removed = location.removeGoods(muskets, 5);

        assertNull(removed);
        assertEquals(20, location.getGoodsCount(food));
    }


    // Contains goods

    public void testContainsGoodsEmptyList() {
        assertTrue(location.containsGoods(Collections.emptyList()));
    }


    public void testContainsGoodsWithoutContainer() {
        List<AbstractGoods> requirements =
            Collections.singletonList(new AbstractGoods(food, 10));

        assertFalse(location.containsGoods(requirements));
    }


    public void testContainsGoodsEnoughAmount() {
        location.addGoods(food, 100);

        List<AbstractGoods> requirements =
            Collections.singletonList(new AbstractGoods(food, 50));

        assertTrue(location.containsGoods(requirements));
    }


    public void testContainsGoodsExactAmount() {
        location.addGoods(food, 50);

        List<AbstractGoods> requirements =
            Collections.singletonList(new AbstractGoods(food, 50));

        assertTrue(location.containsGoods(requirements));
    }


    public void testContainsGoodsInsufficientAmount() {
        location.addGoods(food, 20);

        List<AbstractGoods> requirements =
            Collections.singletonList(new AbstractGoods(food, 50));

        assertFalse(location.containsGoods(requirements));
    }


    public void testContainsGoodsMultipleRequirements() {
        GoodsType muskets = spec.getGoodsType("model.goods.muskets");
        assertNotNull(muskets);

        location.addGoods(food, 50);

        List<AbstractGoods> enough = Arrays.asList(
            new AbstractGoods(food, 20),
            new AbstractGoods(food, 30));

        List<AbstractGoods> insufficient = Arrays.asList(
            new AbstractGoods(food, 10),
            new AbstractGoods(muskets, 10));

        assertTrue(location.containsGoods(enough));
        assertFalse(location.containsGoods(insufficient));
    }


    // Remove all

    public void testRemoveAllWithoutContainer() {
        location.removeAll();

        assertNull(location.getGoodsContainer());
        assertEquals(0, location.getInvalidationCount());
    }


    public void testRemoveAllWithContainer() {
        location.addGoods(food, 50);

        int previousCount = location.getInvalidationCount();

        location.removeAll();

        assertEquals(0, location.getGoodsCount(food));
        assertTrue(location.getInvalidationCount() > previousCount);
    }


    public void testRemoveAllTwice() {
        location.addGoods(food, 30);

        location.removeAll();
        location.removeAll();

        assertEquals(0, location.getGoodsCount(food));
    }


    // Goods lists

    public void testGetGoodsListWithContainer() {
        location.addGoods(food, 250);

        List<Goods> result = location.getGoodsList();

        assertNotNull(result);
        assertEquals(3, result.size());

        int total = 0;
        for (Goods goods : result) {
            assertTrue(goods.getAmount() <= GoodsContainer.CARGO_SIZE);
            total += goods.getAmount();
            assertSame(location, goods.getLocation());
        }

        assertEquals(250, total);
    }


    public void testGetCompactGoodsListWithContainer() {
        location.addGoods(food, 250);

        List<Goods> result = location.getCompactGoodsList();

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(250, result.get(0).getAmount());
        assertSame(location, result.get(0).getLocation());
    }


    public void testGetGoodsCountOtherType() {
        GoodsType muskets = spec.getGoodsType("model.goods.muskets");
        assertNotNull(muskets);

        location.addGoods(food, 50);

        assertEquals(0, location.getGoodsCount(muskets));
    }


    // Move goods

    public void testMoveGoodsBothLocations() {
        TestGoodsLocation destination =
            new TestGoodsLocation(game, "test.destination");

        location.addGoods(food, 100);
        ensureContainer(destination);

        GoodsLocation.moveGoods(location, food, 40, destination);

        assertEquals(60, location.getGoodsCount(food));
        assertEquals(40, destination.getGoodsCount(food));
    }


    public void testMoveGoodsNullSource() {
        TestGoodsLocation destination =
            new TestGoodsLocation(game, "test.destination.null.source");

        ensureContainer(destination);

        GoodsLocation.moveGoods(null, food, 20, destination);

        assertEquals(20, destination.getGoodsCount(food));
    }


    public void testMoveGoodsNullDestination() {
        location.addGoods(food, 50);

        GoodsLocation.moveGoods(location, food, 20, null);

        assertEquals(30, location.getGoodsCount(food));
    }


    public void testMoveGoodsBothNull() {
        GoodsLocation.moveGoods(null, food, 20, null);
    }


    public void testMoveGoodsFromEmptySource() {
        TestGoodsLocation destination =
            new TestGoodsLocation(game, "test.destination.empty.source");

        ensureContainer(location);
        ensureContainer(destination);

        GoodsLocation.moveGoods(location, food, 10, destination);

        assertEquals(0, location.getGoodsCount(food));
        assertEquals(10, destination.getGoodsCount(food));
    }


    // Locatable operations

    public void testAddGoodsLocatable() {
        ensureContainer(location);

        Goods goods = createGoods(food, 40);

        assertTrue(location.add(goods));
        assertEquals(40, location.getGoodsCount(food));
    }


    public void testAddGoodsLocatableWithoutContainer() {
        Goods goods = createGoods(food, 10);

        assertTrue(location.add(goods));
        assertEquals(10, location.getGoodsCount(food));
    }


    public void testContainsGoodsLocatable() {
        location.addGoods(food, 40);

        Goods goods = createGoods(food, 30);

        assertTrue(location.contains(goods));
    }


    public void testContainsGoodsLocatableInsufficientAmount() {
        location.addGoods(food, 20);

        Goods goods = createGoods(food, 30);

        assertFalse(location.contains(goods));
    }


    public void testContainsGoodsLocatableWithoutContainer() {
        Goods goods = createGoods(food, 10);

        try {
            location.contains(goods);
            fail("Expected NullPointerException");
        } catch (NullPointerException expected) {
            // Expected: goodsContainer is null.
        }
    }


    public void testContainsGoodsLocatableExactAmount() {
        location.addGoods(food, 30);

        Goods goods = createGoods(food, 30);

        assertTrue(location.contains(goods));
    }


    public void testRemoveGoodsLocatable() {
        location.addGoods(food, 50);

        Goods goods = createGoods(food, 30);

        assertTrue(location.remove(goods));
        assertEquals(20, location.getGoodsCount(food));
    }


    public void testRemoveGoodsLocatableWhenInsufficient() {
        location.addGoods(food, 20);

        Goods goods = createGoods(food, 30);

        assertTrue(location.remove(goods));
        assertEquals(0, location.getGoodsCount(food));
    }


    public void testRemoveGoodsLocatableWithoutContainer() {
        Goods goods = createGoods(food, 10);

        assertFalse(location.remove(goods));
        assertNull(location.getGoodsContainer());
    }


    public void testRemoveGoodsLocatableExactAmount() {
        location.addGoods(food, 30);

        Goods goods = createGoods(food, 30);

        assertTrue(location.remove(goods));
        assertEquals(0, location.getGoodsCount(food));
    }


    public void testRemoveGoodsLocatableOtherType() {
        GoodsType muskets = spec.getGoodsType("model.goods.muskets");
        assertNotNull(muskets);

        location.addGoods(food, 30);

        Goods goods = createGoods(muskets, 10);

        assertFalse(location.remove(goods));
        assertEquals(30, location.getGoodsCount(food));
    }


    public void testAddNonGoodsLocatable() {
        TestLocatable locatable = new TestLocatable();

        assertFalse(location.add(locatable));
    }


    public void testRemoveNonGoodsLocatable() {
        TestLocatable locatable = new TestLocatable();

        assertFalse(location.remove(locatable));
    }


    public void testContainsNonGoodsLocatable() {
        TestLocatable locatable = new TestLocatable();

        assertFalse(location.contains(locatable));
    }


    // Goods capacity

    public void testGetNoAddReasonWithinCapacity() {
        Goods goods = createGoods(food, 10);
        location.setCapacity(goods.getSpaceTaken());

        assertEquals("NONE", location.getNoAddReason(goods).toString());
    }


    public void testGetNoAddReasonCapacityExceeded() {
        Goods goods = createGoods(food, GoodsContainer.CARGO_SIZE + 1);
        location.setCapacity(0);

        assertEquals("CAPACITY_EXCEEDED",
                     location.getNoAddReason(goods).toString());
    }


    public void testGetNoAddReasonNonGoodsLocatable() {
        TestLocatable locatable = new TestLocatable();

        assertEquals("WRONG_TYPE",
                     location.getNoAddReason(locatable).toString());
    }


    public void testGetNoAddReasonWithoutContainer() {
        Goods goods = createGoods(food, 10);
        location.setCapacity(goods.getSpaceTaken());

        assertEquals("NONE", location.getNoAddReason(goods).toString());
    }


    public void testGetNoAddReasonAtExactCapacity() {
        Goods goods = createGoods(food, GoodsContainer.CARGO_SIZE);
        location.setCapacity(goods.getSpaceTaken());

        assertEquals("NONE", location.getNoAddReason(goods).toString());
    }


    public void testGetNoAddReasonJustOverCapacity() {
        Goods goods = createGoods(food, GoodsContainer.CARGO_SIZE + 1);
        location.setCapacity(goods.getSpaceTaken() - 1);

        assertEquals("CAPACITY_EXCEEDED",
                     location.getNoAddReason(goods).toString());
    }


    public void testGetNoAddReasonWithExistingGoods() {
        location.addGoods(food, 40);

        Goods incoming = createGoods(food, 10);
        int existingSpace = location.getGoodsContainer().getSpaceTaken();
        int incomingSpace = incoming.getSpaceTaken();

        location.setCapacity(existingSpace + incomingSpace);

        assertEquals("NONE",
                     location.getNoAddReason(incoming).toString());

        location.setCapacity(existingSpace + incomingSpace - 1);

        assertEquals("CAPACITY_EXCEEDED",
                     location.getNoAddReason(incoming).toString());
    }


    // Disposables

    public void testGetDisposablesWithoutContainer() {
        try (Stream<FreeColGameObject> result = location.getDisposables()) {
            assertNotNull(result);
            assertEquals(1, result.count());
        }
    }


    public void testGetDisposablesWithContainer() {
        location.addGoods(food, 50);

        try (Stream<FreeColGameObject> result = location.getDisposables()) {
            assertNotNull(result);
            assertTrue(result.count() > 1);
        }
    }


    // copyIn

    public void testCopyInNull() {
        assertFalse(location.copyIn(null));
    }


    public void testCopyInWrongId() {
        TestGoodsLocation other =
            new TestGoodsLocation(game, "different.goods.location");

        assertFalse(location.copyIn(other));
    }


    public void testCopyInSuccess() {
        assertNull(location.getGoodsContainer());

        assertTrue(location.copyIn(location));

        assertNull(location.getGoodsContainer());
    }


    public void testCopyInNonGoodsLocation() {
        assertFalse(location.copyIn(game));
    }


    // XML serialization

    public void testWriteChildrenWithoutGoodsContainer() throws Exception {
        StringWriter output = new StringWriter();
        FreeColXMLWriter xw = new FreeColXMLWriter(output);

        xw.writeStartElement(location.getXMLTagName());
        location.writeChildren(xw);
        xw.writeEndElement();
        xw.close();

        assertFalse(output.toString().contains(GoodsContainer.TAG));
    }


    public void testWriteChildrenWithGoodsContainer() throws Exception {
        location.addGoods(food, 50);

        StringWriter output = new StringWriter();
        FreeColXMLWriter xw = new FreeColXMLWriter(output);

        xw.writeStartElement(location.getXMLTagName());
        location.writeChildren(xw);
        xw.writeEndElement();
        xw.close();

        assertTrue(output.toString().contains(GoodsContainer.TAG));
        assertTrue(output.toString().contains(
            GoodsContainer.STORED_GOODS_TAG));
    }


    public void testReadChildGoodsContainer() throws Exception {
        location.addGoods(food, 75);

        StringWriter output = new StringWriter();
        FreeColXMLWriter xw = new FreeColXMLWriter(output);

        location.getGoodsContainer().toXML(xw);
        xw.close();

        FreeColXMLReader xr =
            new FreeColXMLReader(new StringReader(output.toString()));

        try {
            xr.nextTag();
            location.setGoodsContainer(null);
            location.readChild(xr);

            assertNotNull(location.getGoodsContainer());
            assertSame(location, location.getGoodsContainer().getParent());
            assertEquals(75, location.getGoodsCount(food));
        } finally {
            xr.close();
        }
    }


    public void testReadChildUnknownTag() throws Exception {
        String xml = "<testGoodsLocation><unknown/></testGoodsLocation>";

        try (FreeColXMLReader xr = new FreeColXMLReader(
                new StringReader(xml))) {
            xr.nextTag();
            xr.nextTag();

            try {
                location.readChild(xr);
                fail("Expected XMLStreamException for unknown tag");
            } catch (XMLStreamException expected) {
                assertNotNull(expected.getMessage());
            }
        }
    }


    public void testReadChildrenClearsExistingGoods() throws Exception {
        location.addGoods(food, 75);

        FreeColXMLReader xr =
            new FreeColXMLReader(
                new StringReader("<testGoodsLocation/>"));

        try {
            xr.nextTag();
            location.readChildren(xr);

            assertNotNull(location.getGoodsContainer());
            assertEquals(0, location.getGoodsCount(food));
        } finally {
            xr.close();
        }
    }


    public void testReadChildrenWithoutGoodsContainer() throws Exception {
        assertNull(location.getGoodsContainer());

        FreeColXMLReader xr =
            new FreeColXMLReader(
                new StringReader("<testGoodsLocation/>"));

        try {
            xr.nextTag();
            location.readChildren(xr);

            assertNull(location.getGoodsContainer());
        } finally {
            xr.close();
        }
    }


    public void testReadChildGoodsContainerWhenContainerExists()
        throws Exception {
        location.addGoods(food, 75);

        StringWriter output = new StringWriter();
        FreeColXMLWriter xw = new FreeColXMLWriter(output);

        location.getGoodsContainer().toXML(xw);
        xw.close();

        FreeColXMLReader xr =
            new FreeColXMLReader(new StringReader(output.toString()));

        try {
            xr.nextTag();
            location.readChild(xr);

            assertNotNull(location.getGoodsContainer());
            assertEquals(75, location.getGoodsCount(food));
        } finally {
            xr.close();
        }
    }


    // XML tag

    public void testGetXMLTagName() {
        assertEquals("testGoodsLocation", location.getXMLTagName());
    }
}
