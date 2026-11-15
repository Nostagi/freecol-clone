package net.sf.freecol.common.model;

import java.io.StringReader;
import java.io.StringWriter;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JButton;

import net.sf.freecol.common.io.FreeColXMLReader;
import net.sf.freecol.common.io.FreeColXMLWriter;
import net.sf.freecol.util.test.FreeColTestCase;


public class ModelMessageCFTTest extends FreeColTestCase {

    private Game game;
    private Player player;
    private Colony colony;
    private Colony colony2;

    private ModelMessage message;


    @Override
    protected void setUp() throws Exception {
        super.setUp();

        game = getGame();
        game.setMap(getTestMap(true));

        colony = createStandardColony();
        player = colony.getOwner();
        colony2 = createStandardColony();

        message = new ModelMessage(
            ModelMessage.MessageType.DEFAULT,
            "model.message.test",
            colony
        );
    }


    private ModelMessage createMessage(
            ModelMessage.MessageType type,
            FreeColGameObject source) {
        return new ModelMessage(type, "model.message.test", source);
    }


    private ModelMessage createMessage(
            ModelMessage.MessageType type,
            FreeColGameObject source,
            FreeColObject display) {
        return new ModelMessage(type, "model.message.test", source, display);
    }


    // Constructors

    public void testT01_defaultConstructor() {
        ModelMessage result = new ModelMessage();

        assertNotNull(result);
        assertNull(result.getSourceId());
        assertNull(result.getDisplayId());
        assertNull(result.getMessageType());
        assertFalse(result.getDisplayed());
    }


    public void testT02_constructorWithAllArguments() {
        ModelMessage result = new ModelMessage(
            ModelMessage.MessageType.WARNING,
            "message.id",
            "default.id",
            colony,
            player
        );

        assertEquals("message.id", result.getId());
        assertEquals("default.id", result.getDefaultId());
        assertEquals(colony.getId(), result.getSourceId());
        assertEquals(player.getId(), result.getDisplayId());
        assertEquals(
            ModelMessage.MessageType.WARNING,
            result.getMessageType()
        );
    }


    public void testT03_constructorWithNullDisplay() {
        ModelMessage result = new ModelMessage(
            ModelMessage.MessageType.WARNING,
            "message.id",
            colony,
            null
        );

        assertEquals(colony.getId(), result.getSourceId());
        assertEquals(colony.getId(), result.getDisplayId());
    }


    public void testT04_constructorWithSourceAndDisplay() {
        ModelMessage result = new ModelMessage(
            ModelMessage.MessageType.WARNING,
            "message.id",
            colony,
            player
        );

        assertEquals(colony.getId(), result.getSourceId());
        assertEquals(player.getId(), result.getDisplayId());
    }


    public void testT05_constructorWithSourceOnly() {
        ModelMessage result = new ModelMessage(
            ModelMessage.MessageType.UNIT_ADDED,
            "message.id",
            colony
        );

        assertEquals(colony.getId(), result.getSourceId());
        assertEquals(
            ModelMessage.MessageType.UNIT_ADDED,
            result.getMessageType()
        );
    }


    // MessageType

    public void testT06_messageTypeGetNameKey() {
        for (ModelMessage.MessageType type
                : ModelMessage.MessageType.values()) {
            assertNotNull(type.getNameKey());
        }
    }


    public void testT07_messageTypeGetOptionName() {
        assertEquals(
            "",
            ModelMessage.MessageType.DEFAULT.getOptionName()
        );

        assertNull(
            ModelMessage.MessageType.COMBAT_RESULT.getOptionName()
        );

        assertNull(
            ModelMessage.MessageType.LOST_CITY_RUMOUR.getOptionName()
        );

        assertNotNull(
            ModelMessage.MessageType.WARNING.getOptionName()
        );
    }


    // Getters and setters

    public void testT08_sourceIdGetterSetter() {
        assertEquals(colony.getId(), message.getSourceId());

        message.setSourceId("new.source");

        assertEquals("new.source", message.getSourceId());

        message.setSourceId(null);

        assertNull(message.getSourceId());
    }


    public void testT09_displayIdGetterSetter() {
        message.setDisplayId("new.display");

        assertEquals("new.display", message.getDisplayId());

        message.setDisplayId(null);

        assertNull(message.getDisplayId());
    }


    public void testT10_messageTypeGetterSetter() {
        message.setMessageType(ModelMessage.MessageType.WARNING);

        assertEquals(
            ModelMessage.MessageType.WARNING,
            message.getMessageType()
        );

        message.setMessageType(null);

        assertNull(message.getMessageType());
    }


    public void testT11_displayedGetterSetter() {
        assertFalse(message.getDisplayed());

        message.setDisplayed(true);

        assertTrue(message.getDisplayed());

        message.setDisplayed(false);

        assertFalse(message.getDisplayed());
    }


    public void testT12_getOptionNameWithNullType() {
        message.setMessageType(null);

        assertNull(message.getOptionName());
    }


    public void testT13_getOptionNameWithValidType() {
        message.setMessageType(ModelMessage.MessageType.WARNING);

        assertEquals(
            ModelMessage.MessageType.WARNING.getOptionName(),
            message.getOptionName()
        );
    }


    // divert

    public void testT14_divertWhenDisplayEqualsSource() {
        ModelMessage result = createMessage(
            ModelMessage.MessageType.WARNING,
            colony
        );

        assertEquals(result.getSourceId(), result.getDisplayId());

        result.divert(colony2);

        assertEquals(colony2.getId(), result.getSourceId());
        assertEquals(colony2.getId(), result.getDisplayId());
    }


    public void testT15_divertWhenDisplayDiffersFromSource() {
        ModelMessage result = createMessage(
            ModelMessage.MessageType.WARNING,
            colony,
            player
        );

        assertFalse(result.getSourceId().equals(result.getDisplayId()));

        result.divert(colony2);

        assertEquals(colony2.getId(), result.getSourceId());
        assertEquals(player.getId(), result.getDisplayId());
    }


    // getDefaultDisplay

    public void testT16_defaultDisplayForEveryMessageType() {
        for (ModelMessage.MessageType type
                : ModelMessage.MessageType.values()) {
            ModelMessage result = createMessage(type, colony);

            assertNotNull(result);
            assertEquals(type, result.getMessageType());
        }
    }


    public void testT17_defaultDisplayForPlayerSource() {
        ModelMessage result = createMessage(
            ModelMessage.MessageType.DEFAULT,
            player
        );

        assertEquals(player.getId(), result.getDisplayId());
    }


    public void testT18_defaultDisplayForNonPlayerSource() {
        ModelMessage result = createMessage(
            ModelMessage.MessageType.DEFAULT,
            colony
        );

        assertEquals(colony.getId(), result.getDisplayId());
    }


    public void testT19_defaultDisplayForBells() {
        ModelMessage result = createMessage(
            ModelMessage.MessageType.SONS_OF_LIBERTY,
            colony
        );

        assertEquals(
            spec().getGoodsType("model.goods.bells").getId(),
            result.getDisplayId()
        );
    }


    public void testT20_defaultDisplayForHammers() {
        ModelMessage result = createMessage(
            ModelMessage.MessageType.BUILDING_COMPLETED,
            colony
        );

        assertEquals(
            spec().getGoodsType("model.goods.hammers").getId(),
            result.getDisplayId()
        );
    }


    public void testT21_defaultDisplayUsesSource() {
        ModelMessage.MessageType[] types = {
            ModelMessage.MessageType.UNIT_IMPROVED,
            ModelMessage.MessageType.UNIT_DEMOTED,
            ModelMessage.MessageType.UNIT_LOST,
            ModelMessage.MessageType.UNIT_ADDED,
            ModelMessage.MessageType.LOST_CITY_RUMOUR,
            ModelMessage.MessageType.COMBAT_RESULT,
            ModelMessage.MessageType.DEMANDS,
            ModelMessage.MessageType.GOODS_MOVEMENT
        };

        for (ModelMessage.MessageType type : types) {
            ModelMessage result = createMessage(type, colony);

            assertEquals(colony.getId(), result.getDisplayId());
        }
    }


    // getIgnoredMessageKey

    public void testT22_ignoredMessageKeyForOtherType() {
        message.setMessageType(ModelMessage.MessageType.WARNING);

        assertNull(message.getIgnoredMessageKey());
    }


    public void testT23_ignoredMessageKeyWarehouseCapacityWithGoods() {
        ModelMessage result = createMessage(
            ModelMessage.MessageType.WAREHOUSE_CAPACITY,
            colony
        );

        result.addName("%goods%", "grain");

        assertEquals(
            colony.getId() + "-grain",
            result.getIgnoredMessageKey()
        );
    }


    public void testT24_ignoredMessageKeyWarehouseCapacityWithoutGoods() {
        ModelMessage result = createMessage(
            ModelMessage.MessageType.WAREHOUSE_CAPACITY,
            colony
        );

        assertEquals(
            colony.getId(),
            result.getIgnoredMessageKey()
        );
    }


    public void testT59_ignoredMessageKeyWarehouseCapacityNonTemplate()
            throws Exception {
        String xml =
            "<modelMessage id=\"model.message.test\""
            + " source=\"" + colony.getId() + "\""
            + " messageType=\"WAREHOUSE_CAPACITY\""
            + " templateType=\"KEY\""
            + " hasBeenDisplayed=\"false\"/>";

        try (FreeColXMLReader xr =
                 new FreeColXMLReader(new StringReader(xml))) {
            xr.nextTag();

            ModelMessage result = new ModelMessage(xr);

            assertEquals(
                colony.getId(),
                result.getIgnoredMessageKey()
            );
        }
    }


    // messageTypeComparator

    public void testT25_messageTypeComparatorSameType() {
        ModelMessage m1 = createMessage(
            ModelMessage.MessageType.WARNING,
            colony
        );

        ModelMessage m2 = createMessage(
            ModelMessage.MessageType.WARNING,
            colony
        );

        assertEquals(
            0,
            ModelMessage.messageTypeComparator.compare(m1, m2)
        );
    }


    public void testT26_messageTypeComparatorFirstTypeSmaller() {
        ModelMessage m1 = createMessage(
            ModelMessage.MessageType.DEFAULT,
            colony
        );

        ModelMessage m2 = createMessage(
            ModelMessage.MessageType.WARNING,
            colony
        );

        assertTrue(
            ModelMessage.messageTypeComparator.compare(m1, m2) < 0
        );
    }


    public void testT27_messageTypeComparatorFirstTypeGreater() {
        ModelMessage m1 = createMessage(
            ModelMessage.MessageType.WARNING,
            colony
        );

        ModelMessage m2 = createMessage(
            ModelMessage.MessageType.DEFAULT,
            colony
        );

        assertTrue(
            ModelMessage.messageTypeComparator.compare(m1, m2) > 0
        );
    }


    // getSourceComparator

    public void testT28_sourceComparatorSameSourceId() {
        ModelMessage m1 = createMessage(
            ModelMessage.MessageType.DEFAULT,
            colony
        );

        ModelMessage m2 = createMessage(
            ModelMessage.MessageType.WARNING,
            colony
        );

        Comparator<ModelMessage> comparator =
            ModelMessage.getSourceComparator(game, null);

        assertTrue(comparator.compare(m1, m2) < 0);
    }


    public void testT29_sourceComparatorDifferentSourceClasses() {
        ModelMessage m1 = createMessage(
            ModelMessage.MessageType.WARNING,
            colony
        );

        ModelMessage m2 = createMessage(
            ModelMessage.MessageType.DEFAULT,
            player
        );

        Comparator<ModelMessage> comparator =
            ModelMessage.getSourceComparator(game, null);

        assertTrue(comparator.compare(m1, m2) != 0);
    }


    public void testT30_sourceComparatorWithSpecializedComparator() {
        ModelMessage m1 = createMessage(
            ModelMessage.MessageType.WARNING,
            colony
        );

        ModelMessage m2 = createMessage(
            ModelMessage.MessageType.WARNING,
            colony2
        );

        Map<String, Comparator<?>> specialized = new HashMap<>();

        specialized.put(
            colony.getClass().getName(),
            Comparator.comparing(FreeColObject::getId)
        );

        Comparator<ModelMessage> comparator =
            ModelMessage.getSourceComparator(game, specialized);

        int result = comparator.compare(m1, m2);

        assertTrue(result != 0);
    }


    public void testT31_sourceComparatorWithoutSpecializedComparator() {
        ModelMessage m1 = createMessage(
            ModelMessage.MessageType.WARNING,
            colony
        );

        ModelMessage m2 = createMessage(
            ModelMessage.MessageType.WARNING,
            colony2
        );

        Comparator<ModelMessage> comparator =
            ModelMessage.getSourceComparator(game, new HashMap<>());

        assertEquals(0, comparator.compare(m1, m2));
    }


    // splitLinks

    public void testT32_splitLinksWithoutReplacements() {
        ModelMessage result = createMessage(
            ModelMessage.MessageType.WARNING,
            colony
        );

        List<Object> parts = result.splitLinks(player);

        assertNotNull(parts);
        assertFalse(parts.isEmpty());
    }


    public void testT33_splitLinksWithInvalidLinkReplacement() {
        ModelMessage result = createMessage(
            ModelMessage.MessageType.WARNING,
            colony
        );

        result.addName("%name%", "Test");

        List<Object> parts = result.splitLinks(player);

        assertNotNull(parts);
        assertFalse(parts.isEmpty());

        for (Object part : parts) {
            assertFalse(part instanceof JButton);
        }
    }


    public void testT60_splitLinksWithNullReplacementValue() {
    ModelMessage result = createMessage(
        ModelMessage.MessageType.DEFAULT, colony);

    result.addStringTemplate("%colony%", StringTemplate.name(""));

    assertNotNull(result);
}

    public void testT61_splitLinksWithColonyReplacement() {
        String colonyName = colony.getName();

        ModelMessage result = new ModelMessage(
            ModelMessage.MessageType.WARNING,
            colonyName,
            colony
        );

        result.addName("%colony%", colonyName);

        List<Object> parts = result.splitLinks(player);

        assertNotNull(parts);
        assertFalse(parts.isEmpty());
    }


    public void testT62_splitLinksWithMultipleReplacements() {
        String colonyName = colony.getName();

        ModelMessage result = new ModelMessage(
            ModelMessage.MessageType.WARNING,
            colonyName,
            colony
        );

        result.addName("%colony%", colonyName);
        result.addName("%settlement%", colonyName);

        List<Object> parts = result.splitLinks(player);

        assertNotNull(parts);
        assertFalse(parts.isEmpty());
    }


    // copyIn

    public void testT34_copyInNull() {
        assertFalse(message.copyIn(null));
    }


    public void testT35_copyInWrongType() {
        TradeRouteStop stop = new TradeRouteStop(game);

        assertFalse(message.copyIn(stop));
    }


    public void testT36_copyInDifferentId() {
        ModelMessage source = createMessage(
            ModelMessage.MessageType.WARNING,
            colony
        );

        ModelMessage target = createMessage(
            ModelMessage.MessageType.DEFAULT,
            colony2
        );

        assertTrue(target.copyIn(source));

        assertEquals(source.getSourceId(), target.getSourceId());
        assertEquals(source.getDisplayId(), target.getDisplayId());
        assertEquals(source.getMessageType(), target.getMessageType());
        assertEquals(source.getDisplayed(), target.getDisplayed());
    }


    public void testT37_copyInSameIdSuccessfully() {
        message.setMessageType(ModelMessage.MessageType.WARNING);
        message.setDisplayed(true);
        message.setDisplayId(player.getId());

        ModelMessage target = message.copy(game, ModelMessage.class);

        assertNotNull(target);
        assertEquals(message.getId(), target.getId());

        assertTrue(target.copyIn(message));

        assertEquals(message.getSourceId(), target.getSourceId());
        assertEquals(message.getDisplayId(), target.getDisplayId());
        assertEquals(message.getMessageType(), target.getMessageType());
        assertEquals(message.getDisplayed(), target.getDisplayed());
    }


    // XML tag

    public void testT38_getXMLTagName() {
        assertEquals(ModelMessage.TAG, message.getXMLTagName());
    }


    // equals

    public void testT39_equalsSameObject() {
        assertTrue(message.equals(message));
    }


    public void testT40_equalsNull() {
        assertFalse(message.equals(null));
    }


    public void testT41_equalsDifferentClass() {
        assertFalse(message.equals("message"));
    }


    public void testT42_equalsSameValues() {
        ModelMessage m1 = new ModelMessage(
            ModelMessage.MessageType.WARNING,
            "same.id",
            "default.id",
            colony,
            player
        );

        ModelMessage m2 = new ModelMessage(
            ModelMessage.MessageType.WARNING,
            "same.id",
            "default.id",
            colony,
            player
        );

        assertTrue(m1.equals(m2));
        assertTrue(m2.equals(m1));
    }


    public void testT43_equalsDifferentMessageType() {
        ModelMessage m1 = createMessage(
            ModelMessage.MessageType.WARNING,
            colony
        );

        ModelMessage m2 = createMessage(
            ModelMessage.MessageType.DEFAULT,
            colony
        );

        assertFalse(m1.equals(m2));
    }


    public void testT44_equalsDifferentDisplayed() {
        ModelMessage m1 = createMessage(
            ModelMessage.MessageType.WARNING,
            colony
        );

        ModelMessage m2 = createMessage(
            ModelMessage.MessageType.WARNING,
            colony
        );

        m2.setDisplayed(true);

        assertFalse(m1.equals(m2));
    }


    public void testT45_equalsDifferentSourceId() {
        ModelMessage m1 = createMessage(
            ModelMessage.MessageType.WARNING,
            colony
        );

        ModelMessage m2 = createMessage(
            ModelMessage.MessageType.WARNING,
            colony
        );

        m2.setSourceId("different.source");

        assertFalse(m1.equals(m2));
    }


    public void testT46_equalsDifferentDisplayId() {
        ModelMessage m1 = createMessage(
            ModelMessage.MessageType.WARNING,
            colony
        );

        ModelMessage m2 = createMessage(
            ModelMessage.MessageType.WARNING,
            colony
        );

        m2.setDisplayId("different.display");

        assertFalse(m1.equals(m2));
    }


    // hashCode

    public void testT47_hashCodeSameValues() {
        ModelMessage m1 = new ModelMessage(
            ModelMessage.MessageType.WARNING,
            "same.id",
            "default.id",
            colony,
            player
        );

        ModelMessage m2 = new ModelMessage(
            ModelMessage.MessageType.WARNING,
            "same.id",
            "default.id",
            colony,
            player
        );

        assertEquals(m1.hashCode(), m2.hashCode());
    }


    public void testT48_hashCodeDifferentSource() {
        ModelMessage m1 = createMessage(
            ModelMessage.MessageType.WARNING,
            colony
        );

        ModelMessage m2 = createMessage(
            ModelMessage.MessageType.WARNING,
            colony
        );

        m2.setSourceId("different.source");

        assertTrue(m1.hashCode() != m2.hashCode());
    }


    public void testT49_hashCodeDifferentDisplay() {
        ModelMessage m1 = createMessage(
            ModelMessage.MessageType.WARNING,
            colony
        );

        ModelMessage m2 = createMessage(
            ModelMessage.MessageType.WARNING,
            colony
        );

        m2.setDisplayId("different.display");

        assertTrue(m1.hashCode() != m2.hashCode());
    }


    public void testT50_hashCodeDifferentDisplayed() {
        ModelMessage m1 = createMessage(
            ModelMessage.MessageType.WARNING,
            colony
        );

        ModelMessage m2 = createMessage(
            ModelMessage.MessageType.WARNING,
            colony
        );

        m2.setDisplayed(true);

        assertTrue(m1.hashCode() != m2.hashCode());
    }


    // toString

    public void testT51_toString() {
        String result = message.toString();

        assertNotNull(result);
        assertTrue(result.contains("ModelMessage"));
        assertTrue(result.contains(colony.getId()));
        assertTrue(result.contains("DEFAULT"));
        assertTrue(result.contains("false"));
    }


    public void testT52_toStringWithNullSourceAndDisplay() {
        message.setSourceId(null);
        message.setDisplayId(null);

        String result = message.toString();

        assertNotNull(result);
        assertTrue(result.contains("null/null"));
    }


    // XML serialization

    public void testT53_writeXMLAttributes() throws Exception {
        message.setMessageType(ModelMessage.MessageType.WARNING);
        message.setDisplayed(true);
        message.setDisplayId(player.getId());

        StringWriter output = new StringWriter();

        try (FreeColXMLWriter xw = new FreeColXMLWriter(output)) {
            message.toXML(xw);
        }

        String xml = output.toString();

        assertNotNull(xml);
        assertFalse(xml.isEmpty());
        assertTrue(xml.contains(colony.getId()));
        assertTrue(xml.contains(player.getId()));
    }


    public void testT54_writeXMLWithNullDisplay() throws Exception {
        message.setDisplayId(null);

        StringWriter output = new StringWriter();

        try (FreeColXMLWriter xw = new FreeColXMLWriter(output)) {
            message.toXML(xw);
        }

        String xml = output.toString();

        assertTrue(xml.contains(ModelMessage.TAG));
        assertTrue(xml.contains(colony.getId()));
    }


    public void testT55_readXMLAttributes() throws Exception {
        message.setMessageType(ModelMessage.MessageType.WARNING);
        message.setDisplayed(true);
        message.setDisplayId(player.getId());

        StringWriter output = new StringWriter();

        try (FreeColXMLWriter xw = new FreeColXMLWriter(output)) {
            message.toXML(xw);
        }

        String xml = output.toString();

        try (FreeColXMLReader xr =
                 new FreeColXMLReader(new StringReader(xml))) {
            xr.nextTag();

            ModelMessage result =
                xr.readFreeColObject(game, ModelMessage.class);

            assertNotNull(result);
            assertEquals(message.getSourceId(), result.getSourceId());
            assertEquals(message.getDisplayId(), result.getDisplayId());
            assertEquals(message.getMessageType(), result.getMessageType());
            assertEquals(message.getDisplayed(), result.getDisplayed());
        }
    }


    public void testT56_readXMLDefaultAttributes() throws Exception {
        String xml =
            "<modelMessage id=\"test.message\""
            + " source=\"" + colony.getId() + "\""
            + " messageType=\"DEFAULT\""
            + " hasBeenDisplayed=\"false\"/>";

        try (FreeColXMLReader xr =
                 new FreeColXMLReader(new StringReader(xml))) {
            xr.nextTag();

            ModelMessage result =
                xr.readFreeColObject(game, ModelMessage.class);

            assertNotNull(result);
            assertEquals(colony.getId(), result.getSourceId());
            assertEquals(
                ModelMessage.MessageType.DEFAULT,
                result.getMessageType()
            );
            assertFalse(result.getDisplayed());
        }
    }


    // Default ID

    public void testT57_addDefaultId() {
        ModelMessage result = message.addDefaultId("new.default");

        assertSame(message, result);
        assertEquals("new.default", message.getDefaultId());
    }


    // XML constructor

    public void testT58_constructorFromXMLReader() throws Exception {
        message.setMessageType(ModelMessage.MessageType.WARNING);
        message.setDisplayed(true);
        message.setDisplayId(player.getId());

        StringWriter output = new StringWriter();

        try (FreeColXMLWriter xw = new FreeColXMLWriter(output)) {
            message.toXML(xw);
        }

        String xml = output.toString();

        try (FreeColXMLReader xr =
                 new FreeColXMLReader(new StringReader(xml))) {
            xr.nextTag();

            ModelMessage result = new ModelMessage(xr);

            assertNotNull(result);
            assertEquals(message.getSourceId(), result.getSourceId());
            assertEquals(message.getDisplayId(), result.getDisplayId());
            assertEquals(message.getMessageType(), result.getMessageType());
            assertEquals(message.getDisplayed(), result.getDisplayed());
        }
    }
}
