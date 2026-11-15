
package net.sf.freecol.common.model;

import java.io.StringReader;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.List;

import net.sf.freecol.common.io.FreeColXMLReader;
import net.sf.freecol.common.io.FreeColXMLWriter;
import net.sf.freecol.server.model.ServerUnit;
import net.sf.freecol.util.test.FreeColTestCase;

/**
 * Tests for the TradeRouteStop class.
 *
 * The tests are written in the JUnit 3 style used by FreeCol.
 */
public class TradeRouteStopCFTTest extends FreeColTestCase {

    private Game game;
    private Colony colony;
    private Player player;

    private GoodsType grain;
    private GoodsType tools;


    @Override
    protected void setUp() throws Exception {
        super.setUp();

        game = getGame();
        game.setMap(getTestMap(true));

        colony = createStandardColony();
        player = colony.getOwner();

        grain = spec().getGoodsType("model.goods.grain");
        tools = spec().getGoodsType("model.goods.tools");
    }


    private TradeRouteStop createStop(GoodsType... goods) {
        TradeRouteStop stop = new TradeRouteStop(game, colony);

        for (GoodsType type : goods) {
            stop.addCargo(type);
        }

        return stop;
    }


    private Unit createUnit() {
        UnitType unitType
            = spec().getUnitType("model.unit.freeColonist");

        return new ServerUnit(game, colony.getTile(), player, unitType);
    }


    public void testConstructor() {
        TradeRouteStop stop = new TradeRouteStop(game);

        assertNotNull(stop);
        assertNull(stop.getLocation());
        assertTrue(stop.getCargo().isEmpty());

        TradeRouteStop located = new TradeRouteStop(game, colony);

        assertSame(colony, located.getLocation());
        assertTrue(located.getCargo().isEmpty());
    }


    public void testCopyConstructor() {
        TradeRouteStop source = createStop(grain, tools, grain);

        TradeRouteStop copy = new TradeRouteStop(source);

        assertSame(source.getLocation(), copy.getLocation());
        assertEquals(source.getCargo().size(), copy.getCargo().size());
        assertEquals(source.getCargo(), copy.getCargo());

        assertTrue(source.getCargo() != copy.getCargo());
    }


    public void testGetLocation() {
        TradeRouteStop stop = createStop();

        assertSame(colony, stop.getLocation());
    }


    public void testGetTradeLocation() {
        TradeRouteStop stop = createStop();

        assertSame(colony, stop.getTradeLocation());
    }


    public void testIsValid() {
        TradeRouteStop stop = createStop();

        assertTrue(stop.isValid(player));

        TradeRouteStop empty = new TradeRouteStop(game);

        assertFalse(empty.isValid(player));
    }


    public void testIsValidWithNonTradeLocation() {
        Unit unit = createUnit();
        TradeRouteStop stop = new TradeRouteStop(game, unit);

        assertFalse(stop.isValid(player));
    }


    public void testCargoOperations() {
        TradeRouteStop stop = createStop();

        assertTrue(stop.getCargo().isEmpty());

        stop.addCargo(grain);
        assertEquals(1, stop.getCargo().size());
        assertSame(grain, stop.getCargo().get(0));

        stop.addCargo(tools);
        assertEquals(2, stop.getCargo().size());

        stop.setCargo(Arrays.asList(grain, grain, tools));

        assertEquals(3, stop.getCargo().size());
        assertSame(grain, stop.getCargo().get(0));
        assertSame(grain, stop.getCargo().get(1));
        assertSame(tools, stop.getCargo().get(2));
    }


    public void testSetCargoReplacesPreviousCargo() {
        TradeRouteStop stop = createStop(grain, tools);

        stop.setCargo(Arrays.asList(tools));

        assertEquals(1, stop.getCargo().size());
        assertSame(tools, stop.getCargo().get(0));
    }


    public void testGetCompactCargoEmpty() {
        TradeRouteStop stop = createStop();

        List<AbstractGoods> compact = stop.getCompactCargo();

        assertNotNull(compact);
        assertTrue(compact.isEmpty());
    }


    public void testGetCompactCargoWithoutDuplicates() {
        TradeRouteStop stop = createStop(grain, tools);

        List<AbstractGoods> compact = stop.getCompactCargo();

        assertEquals(2, compact.size());

        assertSame(grain, compact.get(0).getType());
        assertEquals(GoodsContainer.CARGO_SIZE,
                     compact.get(0).getAmount());

        assertSame(tools, compact.get(1).getType());
        assertEquals(GoodsContainer.CARGO_SIZE,
                     compact.get(1).getAmount());
    }


    public void testGetCompactCargoWithDuplicates() {
        TradeRouteStop stop = createStop(grain, grain, tools, grain);

        List<AbstractGoods> compact = stop.getCompactCargo();

        assertEquals(2, compact.size());

        assertSame(grain, compact.get(0).getType());
        assertEquals(3 * GoodsContainer.CARGO_SIZE,
                     compact.get(0).getAmount());

        assertSame(tools, compact.get(1).getType());
        assertEquals(GoodsContainer.CARGO_SIZE,
                     compact.get(1).getAmount());
    }


    public void testHasWorkWithoutCargo() {
        TradeRouteStop stop = createStop();
        Unit unit = createUnit();

        assertFalse(stop.hasWork(unit, 0));
    }

    public void testHasWorkWithInsufficientExportGoods() {
    TradeRouteStop stop = createStop(grain);
    Unit unit = createUnit();

    colony.getExportData(grain).setExportLevel(100);
    colony.addGoods(grain, 100);

    assertFalse(stop.hasWork(unit, 0));
}

public void testHasWorkWithGoodsToUnload() {
    TradeRouteStop stop = createStop(grain);
    Unit unit = createUnit();

    colony.getExportData(grain).setExportLevel(100);
    unit.addGoods(tools, 10);

    assertTrue(stop.hasWork(unit, 0));
}

public void testHasWorkWithGoodsNotAcceptedByStop() {
    TradeRouteStop stop = createStop(grain);
    Unit unit = createUnit();

    colony.getExportData(grain).setExportLevel(100);
    colony.getExportData(tools).setExportLevel(100);
    unit.addGoods(grain, 10);

    assertFalse(stop.hasWork(unit, 0));
}


    public void testHasWorkWithCargoToLoad() {
    TradeRouteStop stop = createStop(grain);
    Unit unit = createUnit();

    colony.getExportData(grain).setExportLevel(0);
    colony.addGoods(grain, 100);

    assertTrue(stop.hasWork(unit, 0));
}


    public void testHasWorkWithCargoWithoutTradeLocation() {
        TradeRouteStop stop = new TradeRouteStop(game);
        stop.addCargo(grain);

        Unit unit = createUnit();

        assertFalse(stop.hasWork(unit, 0));
    }


    public void testGetAvailableGoodsCount() {
        TradeRouteStop stop = createStop();

        assertEquals(colony.getAvailableGoodsCount(grain),
                     stop.getAvailableGoodsCount(grain));
    }


    public void testGetExportAmount() {
        TradeRouteStop stop = createStop();

        assertEquals(colony.getExportAmount(grain, 0),
                     stop.getExportAmount(grain, 0));
    }


    public void testGetImportAmount() {
        TradeRouteStop stop = createStop();

        assertEquals(colony.getImportAmount(grain, 0),
                     stop.getImportAmount(grain, 0));
    }


    public void testTradeLocationMethodsWithoutTradeLocation() {
        TradeRouteStop stop = new TradeRouteStop(game);

        assertEquals(0, stop.getAvailableGoodsCount(grain));
        assertEquals(0, stop.getExportAmount(grain, 0));
        assertEquals(0, stop.getImportAmount(grain, 0));
    }


    public void testGetLocationName() {
        TradeRouteStop stop = createStop();

        assertNull(stop.getLocationName(colony));
    }


    public void testIsInternable() {
        TradeRouteStop stop = createStop();

        assertFalse(stop.isInternable());
    }


    public void testCopyIn() {
        TradeRouteStop source = createStop(grain, tools, grain);
        TradeRouteStop target = createStop();

        source.setId("tradeRouteStop.test");
        target.setId("tradeRouteStop.test");

        assertTrue(target.copyIn(source));

        assertSame(colony, target.getLocation());
        assertEquals(source.getCargo(), target.getCargo());

        assertTrue(source.getCargo() != target.getCargo());
    }


    public void testCopyInNull() {
        TradeRouteStop target = createStop();

        assertFalse(target.copyIn(null));
    }


    public void testCopyInDifferentId() {
        TradeRouteStop source = createStop(grain);
        TradeRouteStop target = createStop();

        source.setId("tradeRouteStop.source");
        target.setId("tradeRouteStop.target");

        assertFalse(target.copyIn(source));
    }


    public void testCopyInWrongClass() {
        TradeRouteStop target = createStop();

        target.setId("tradeRouteStop.test");

        FreeColObject other = new FreeColObject() {
            @Override
            public String getXMLTagName() {
                return "test";
            }
        };

        other.setId("tradeRouteStop.test");

        assertFalse(target.copyIn(other));
    }


    public void testEquals() {
        TradeRouteStop stop = createStop(grain);

        assertTrue(stop.equals(stop));
    }


    public void testEqualsDifferentObjects() {
        TradeRouteStop stop1 = createStop(grain);
        TradeRouteStop stop2 = createStop(grain);

        assertFalse(stop1.equals(stop2));
        assertFalse(stop1.equals(null));
        assertFalse(stop1.equals("not a TradeRouteStop"));
    }


    public void testGetXMLTagName() {
        TradeRouteStop stop = createStop();

        assertEquals(TradeRouteStop.TAG, stop.getXMLTagName());
        assertEquals("tradeRouteStop", stop.getXMLTagName());
    }


    public void testToString() {
        TradeRouteStop stop = createStop(grain, tools);

        String result = stop.toString();

        assertNotNull(result);
        assertTrue(result.startsWith("["));
        assertTrue(result.contains(colony.getId()));
        assertTrue(result.contains(grain.toString()));
        assertTrue(result.contains(tools.toString()));
        assertTrue(result.endsWith("]"));
    }


    public void testGetLabelFor() {
        TradeRouteStop stop = createStop();

        StringTemplate label
            = stop.getLabelFor("test.tradeRouteStop", player);

        assertNotNull(label);
    }


    public void testInvalidStopLabel() {
        TradeRouteStop stop = createStop();

        StringTemplate label = stop.invalidStopLabel(player);

        assertNotNull(label);
    }


    public void testXMLConstructorWithCargo() throws Exception {
        String xml = "<tradeRouteStop location=\"" + colony.getId() + "\">"
            + "<cargo id=\"" + grain.getId() + "\"/>"
            + "<cargo id=\"" + tools.getId() + "\"/>"
            + "</tradeRouteStop>";

        FreeColXMLReader xr
            = new FreeColXMLReader(new StringReader(xml));

        try {
            xr.nextTag();

            TradeRouteStop stop = new TradeRouteStop(game, xr);

            assertSame(colony, stop.getLocation());
            assertEquals(2, stop.getCargo().size());
            assertSame(grain, stop.getCargo().get(0));
            assertSame(tools, stop.getCargo().get(1));
        } finally {
            xr.close();
        }
    }


    public void testXMLConstructorWithoutCargo() throws Exception {
        String xml = "<tradeRouteStop location=\"" + colony.getId() + "\"/>";

        FreeColXMLReader xr
            = new FreeColXMLReader(new StringReader(xml));

        try {
            xr.nextTag();

            TradeRouteStop stop = new TradeRouteStop(game, xr);

            assertSame(colony, stop.getLocation());
            assertTrue(stop.getCargo().isEmpty());
        } finally {
            xr.close();
        }
    }


    public void testWriteAttributesAndChildren() throws Exception {
        TradeRouteStop stop = createStop(grain, tools);

        StringWriter output = new StringWriter();
        FreeColXMLWriter xw = new FreeColXMLWriter(output);

        try {
            xw.writeStartElement(TradeRouteStop.TAG);

            stop.writeAttributes(xw);
            stop.writeChildren(xw);

            xw.writeEndElement();
            xw.flush();
        } finally {
            xw.close();
        }

        String xml = output.toString();

        assertTrue(xml.contains("location=\"" + colony.getId() + "\""));
        assertTrue(xml.contains("<cargo"));
        assertTrue(xml.contains(grain.getId()));
        assertTrue(xml.contains(tools.getId()));
    }
    public void testHasWorkWhenUnitHasEnoughCargo() {
    TradeRouteStop stop = createStop(grain);
    Unit unit = createUnit();

    colony.getExportData(grain).setExportLevel(0);
    colony.addGoods(grain, 100);
    unit.addGoods(grain, GoodsContainer.CARGO_SIZE);

    assertFalse(stop.hasWork(unit, 0));
}


public void testHasWorkWhenCargoIsAlreadyExpectedAtStop() {
    TradeRouteStop stop = createStop(grain);
    Unit unit = createUnit();

    colony.getExportData(grain).setExportLevel(100);
    unit.addGoods(grain, 10);

    assertFalse(stop.hasWork(unit, 0));
}
public void testIsValidWithDifferentOwner() {
    TradeRouteStop stop = createStop();

    Player otherPlayer = game.getPlayers(p -> p != player)
        .findFirst()
        .orElse(null);

    assertNotNull(otherPlayer);
    assertFalse(stop.isValid(otherPlayer));
}

public void testHasWorkWithNoImportDemand() {
    TradeRouteStop stop = createStop(grain);
    Unit unit = createUnit();

    unit.addGoods(tools, 10);

    int capacity = colony.getWarehouseCapacity();
    colony.addGoods(tools, capacity);

    assertEquals(0, colony.getImportAmount(tools, 0));
    assertEquals(0, stop.getImportAmount(tools, 0));
    assertFalse(stop.hasWork(unit, 0));
}

public void testIsValidWithDisposedLocation() {
    TradeRouteStop stop = createStop();

    colony.fundamentalDispose();

    assertFalse(stop.isValid(player));
}

public void testIsValidWithUninitializedLocation() {
    Colony uninitializedColony = new Colony(game, "colony:uninitialized");
    TradeRouteStop stop = new TradeRouteStop(game, uninitializedColony);

    assertFalse(uninitializedColony.isInitialized());
    assertFalse(stop.isValid(player));
}

}
