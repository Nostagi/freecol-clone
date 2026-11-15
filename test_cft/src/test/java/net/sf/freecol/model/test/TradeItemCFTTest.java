package net.sf.freecol.common.model;

import java.io.StringReader;
import java.io.StringWriter;

import net.sf.freecol.common.io.FreeColXMLReader;
import net.sf.freecol.common.io.FreeColXMLWriter;
import net.sf.freecol.util.test.FreeColTestCase;

public class TradeItemCFTTest extends FreeColTestCase {

    private Game game;
    private Player source;
    private Player destination;
    private Colony colony;
    private TestTradeItem item;

    private static class TestTradeItem extends TradeItem {
        private boolean failSuperCopyIn = false;

public void setFailSuperCopyIn(boolean value) {
    this.failSuperCopyIn = value;
}

@Override
protected <T extends FreeColObject, R extends FreeColObject> R
    copyInCast(T other, Class<R> returnClass) {

    if (failSuperCopyIn
        && returnClass == FreeColObject.class) {
        return null;
    }

    return super.copyInCast(other, returnClass);
}

        TestTradeItem(Game game, String id,
                      Player source, Player destination) {
            super(game, id, source, destination);
        }

        TestTradeItem(Game game, FreeColXMLReader xr) throws Exception {
            super(game, xr);
        }

        @Override
        public boolean isValid() {
            return true;
        }

        @Override
        public boolean isUnique() {
            return false;
        }

        @Override
        public StringTemplate getLabel() {
            return StringTemplate.template("test");
        }

        @Override
        public int evaluateFor(Player player) {
            return 0;
        }

        @Override
        public String getXMLTagName() {
            return "testTradeItem";
        }
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        game = getGame();
        game.setMap(getTestMap(true));

        colony = createStandardColony();
        source = colony.getOwner();

        destination = game.getPlayers(p -> p != source)
            .findFirst()
            .orElse(null);

        item = new TestTradeItem(
            game, "test.trade.item", source, destination);
    }

    // Constructor

    public void testT01_constructor() {
        assertNotNull(item);
        assertEquals("test.trade.item", item.getId());
        assertSame(source, item.getSource());
        assertSame(destination, item.getDestination());
    }

    // Source and destination

    public void testT02_getSource() {
        assertSame(source, item.getSource());
    }

    public void testT03_setSource() {
        item.setSource(null);
        assertNull(item.getSource());

        item.setSource(source);
        assertSame(source, item.getSource());
    }

    public void testT04_getDestination() {
        assertSame(destination, item.getDestination());
    }

    public void testT05_setDestination() {
        item.setDestination(null);
        assertNull(item.getDestination());

        item.setDestination(destination);
        assertSame(destination, item.getDestination());
    }

    // getOther

    public void testT06_getOtherSource() {
        assertSame(destination, item.getOther(source));
    }

    public void testT07_getOtherDestination() {
        assertSame(source, item.getOther(destination));
    }

    public void testT08_getOtherNull() {
        assertSame(source, item.getOther(null));
    }

    public void testT09_getOtherDifferentPlayer() {
        Player other = game.getPlayers(
            p -> p != source && p != destination)
            .findFirst()
            .orElse(null);

        if (other != null) {
            assertSame(source, item.getOther(other));
        }
    }

    // Abstract methods

    public void testT10_isValid() {
        assertTrue(item.isValid());
    }

    public void testT11_isUnique() {
        assertFalse(item.isUnique());
    }

    public void testT12_getLabel() {
        assertNotNull(item.getLabel());
    }

    public void testT13_evaluateFor() {
        assertEquals(0, item.evaluateFor(source));
    }

    // Default methods

    public void testT14_getColony() {
        assertNull(item.getColony(game));
    }

    public void testT15_getGoods() {
        assertNull(item.getGoods());
    }

    public void testT16_setGoods() {
        item.setGoods(null);
        assertNull(item.getGoods());
    }

    public void testT17_getGold() {
        assertEquals(0, item.getGold());
    }

    public void testT18_setGold() {
        item.setGold(100);
        assertEquals(0, item.getGold());
    }

    public void testT19_getVictim() {
        assertNull(item.getVictim());
    }

    public void testT20_getStance() {
        assertNull(item.getStance());
    }

    public void testT21_setStance() {
        item.setStance(null);
        assertNull(item.getStance());
    }

    public void testT22_getUnit() {
        assertNull(item.getUnit());
    }

    public void testT23_setUnit() {
        item.setUnit(null);
        assertNull(item.getUnit());
    }

    // isInternable

    public void testT24_isInternable() {
        assertFalse(item.isInternable());
    }

    // copyIn

    public void testT25_copyInNull() {
        assertFalse(item.copyIn(null));
    }

    public void testT26_copyInWrongType() {
        assertFalse(item.copyIn(colony));
    }

    public void testT27_copyInDifferentId() {
        TestTradeItem other = new TestTradeItem(
            game, "different.id", destination, source);

        assertFalse(item.copyIn(other));
    }

    public void testT28_copyInSameId() {
        TestTradeItem other = new TestTradeItem(
            game, item.getId(), destination, source);

        assertTrue(item.copyIn(other));

        assertSame(destination, item.getSource());
        assertSame(source, item.getDestination());
    }

    public void testT29_copyInNullPlayers() {
        TestTradeItem other = new TestTradeItem(
            game, item.getId(), null, null);

        assertTrue(item.copyIn(other));

        assertNull(item.getSource());
        assertNull(item.getDestination());
    }
    public void testT46_copyInSuperReturnsFalse() {
    TestTradeItem target = new TestTradeItem(
        game, "test.copyin.super.false", source, destination);

    TestTradeItem other = new TestTradeItem(
        game, "test.copyin.super.false", destination, source);

    target.setFailSuperCopyIn(true);

    assertFalse(target.copyIn(other));
}

    // equals

    public void testT30_equalsSameObject() {
        assertTrue(item.equals(item));
    }

    public void testT31_equalsNull() {
        assertFalse(item.equals(null));
    }

    public void testT32_equalsDifferentType() {
        assertFalse(item.equals(colony));
    }

    public void testT33_equalsDifferentId() {
        TestTradeItem other = new TestTradeItem(
            game, "other.id", source, destination);

        assertFalse(item.equals(other));
    }

    public void testT34_equalsDifferentSource() {
        TestTradeItem other = new TestTradeItem(
            game, item.getId(), destination, destination);

        assertFalse(item.equals(other));
    }

    public void testT35_equalsDifferentDestination() {
        TestTradeItem other = new TestTradeItem(
            game, item.getId(), source, source);

        assertFalse(item.equals(other));
    }

    public void testT36_equalsSameValues() {
        TestTradeItem other = new TestTradeItem(
            game, item.getId(), source, destination);

        assertTrue(item.equals(other));
    }

    public void testT37_equalsBothPlayersNull() {
        TestTradeItem first = new TestTradeItem(
            game, "null.players", null, null);

        TestTradeItem second = new TestTradeItem(
            game, "null.players", null, null);

        assertTrue(first.equals(second));
    }

    public void testT38_equalsSourceNull() {
        TestTradeItem first = new TestTradeItem(
            game, "source.null", null, destination);

        TestTradeItem second = new TestTradeItem(
            game, "source.null", source, destination);

        assertFalse(first.equals(second));
    }

    public void testT39_equalsDestinationNull() {
        TestTradeItem first = new TestTradeItem(
            game, "destination.null", source, null);

        TestTradeItem second = new TestTradeItem(
            game, "destination.null", source, destination);

        assertFalse(first.equals(second));
    }

    // hashCode

    public void testT40_hashCode() {
        assertEquals(item.hashCode(), item.hashCode());
    }

    public void testT41_hashCodeNullPlayers() {
        TestTradeItem other = new TestTradeItem(
            game, "null.hash", null, null);

        assertEquals(other.hashCode(), other.hashCode());
    }

    // XML tag

    public void testT42_getXMLTagName() {
        assertEquals("testTradeItem", item.getXMLTagName());
    }

    // XML serialization

    public void testT43_writeXMLAttributes() throws Exception {
        StringWriter output = new StringWriter();

        try (FreeColXMLWriter xw = new FreeColXMLWriter(output)) {
            item.toXML(xw);
        }

        String xml = output.toString();

        assertTrue(xml.contains("testTradeItem"));
        assertTrue(xml.contains("source"));
        assertTrue(xml.contains("destination"));
        assertTrue(xml.contains(source.getId()));

        if (destination != null) {
            assertTrue(xml.contains(destination.getId()));
        }
    }

    public void testT44_writeXMLNullPlayers() throws Exception {
        TestTradeItem nullItem = new TestTradeItem(
            game, "null.xml.item", null, null);

        StringWriter output = new StringWriter();

        try (FreeColXMLWriter xw = new FreeColXMLWriter(output)) {
            nullItem.toXML(xw);
        }

        String xml = output.toString();

        assertNotNull(xml);
        assertTrue(xml.contains("testTradeItem"));
    }

    // XML deserialization

    public void testT45_readXMLAttributes() throws Exception {
        StringWriter output = new StringWriter();

        try (FreeColXMLWriter xw = new FreeColXMLWriter(output)) {
            item.toXML(xw);
        }

        String xml = output.toString();

        try (FreeColXMLReader xr =
                 new FreeColXMLReader(new StringReader(xml))) {
            xr.nextTag();

            TestTradeItem copy = new TestTradeItem(game, xr);

            assertEquals(source, copy.getSource());
            assertEquals(destination, copy.getDestination());
        }
    }
}