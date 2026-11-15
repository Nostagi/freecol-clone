
package net.sf.freecol.common.model;

import java.io.StringReader;
import java.io.StringWriter;
import java.util.List;

import net.sf.freecol.common.io.FreeColXMLReader;
import net.sf.freecol.common.io.FreeColXMLWriter;
import net.sf.freecol.common.option.GameOptions;
import net.sf.freecol.common.util.LogBuilder;
import net.sf.freecol.util.test.FreeColTestCase;


public class TradeRouteCFTTest extends FreeColTestCase {

    private Game game;
    private Player player;
    private Colony colony;
    private GoodsType grain;
    private GoodsType tools;
    private TradeRoute route;
    private TradeRouteStop stop1;
    private TradeRouteStop stop2;
    private TradeRouteStop stop3;

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        game = getGame();
        game.setMap(getTestMap(true));

        colony = createStandardColony();
        player = colony.getOwner();

        grain = spec().getGoodsType("model.goods.grain");
        tools = spec().getGoodsType("model.goods.tools");

        route = new TradeRoute(game, "Test route", player);

        stop1 = new TradeRouteStop(game, colony);
        stop2 = new TradeRouteStop(game, colony);
        stop3 = new TradeRouteStop(game, colony);
    }

    private TradeRouteStop createStop(GoodsType... cargo) {
        TradeRouteStop stop = new TradeRouteStop(game, colony);
        for (GoodsType type : cargo) {
            stop.addCargo(type);
        }
        return stop;
    }


    // Constructors, getters and setters

    public void testT01_constructorWithNameAndOwner() {
        TradeRoute tr = new TradeRoute(game, "Route A", player);

        assertEquals("Route A", tr.getName());
        assertSame(player, tr.getOwner());
        assertFalse(tr.isSilent());
        assertEquals(0, tr.getStopCount());
    }

    public void testT02_constructorWithId() {
        TradeRoute tr = new TradeRoute(game, "test.route.id");

        assertEquals("test.route.id", tr.getId());
        assertEquals(0, tr.getStopCount());
    }

    public void testT03_silentGetterSetter() {
        assertFalse(route.isSilent());

        route.setSilent(true);
        assertTrue(route.isSilent());

        route.setSilent(false);
        assertFalse(route.isSilent());
    }

    public void testT04_nameGetterSetter() {
        route.setName("New route");

        assertEquals("New route", route.getName());
    }

    public void testT05_ownerGetterSetter() {
        route.setOwner(null);
        assertNull(route.getOwner());

        route.setOwner(player);
        assertSame(player, route.getOwner());
    }


    // Stop collection operations

    public void testT06_emptyStopCollection() {
        assertEquals(0, route.getStopCount());
        assertTrue(route.getStopList().isEmpty());
        assertNull(route.getStop(0));
        assertNull(route.getStop(-1));
    }

    public void testT07_addStop() {
        route.addStop(stop1);

        assertEquals(1, route.getStopCount());
        assertSame(stop1, route.getStop(0));
    }

    public void testT08_addMultipleStops() {
        route.addStop(stop1);
        route.addStop(stop2);
        route.addStop(stop3);

        assertEquals(3, route.getStopCount());
        assertSame(stop1, route.getStop(0));
        assertSame(stop2, route.getStop(1));
        assertSame(stop3, route.getStop(2));
    }

    public void testT09_getStopNegativeIndex() {
        route.addStop(stop1);

        assertNull(route.getStop(-1));
    }

    public void testT10_getStopIndexEqualSize() {
        route.addStop(stop1);

        assertNull(route.getStop(1));
    }

    public void testT11_getStopIndexGreaterThanSize() {
        route.addStop(stop1);

        assertNull(route.getStop(10));
    }

    public void testT12_getStopListReturnsCopy() {
        route.addStop(stop1);

        List<TradeRouteStop> result = route.getStopList();
        result.clear();

        assertEquals(1, route.getStopCount());
        assertSame(stop1, route.getStop(0));
    }

    public void testT13_removeExistingStop() {
        route.addStop(stop1);
        route.addStop(stop2);

        route.removeStop(stop1);

        assertEquals(1, route.getStopCount());
        assertSame(stop2, route.getStop(0));
    }

    public void testT14_removeNonExistingStop() {
        route.addStop(stop1);

        route.removeStop(stop2);

        assertEquals(1, route.getStopCount());
        assertSame(stop1, route.getStop(0));
    }

    public void testT15_clearStops() {
        route.addStop(stop1);
        route.addStop(stop2);

        route.clearStops();

        assertEquals(0, route.getStopCount());
        assertTrue(route.getStopList().isEmpty());
    }


    // getIndex

    public void testT16_getIndexFirstStop() {
        route.addStop(stop1);
        route.addStop(stop2);

        assertEquals(0, route.getIndex(stop1));
    }

    public void testT17_getIndexMiddleStop() {
        route.addStop(stop1);
        route.addStop(stop2);
        route.addStop(stop3);

        assertEquals(1, route.getIndex(stop2));
    }

    public void testT18_getIndexLastStop() {
        route.addStop(stop1);
        route.addStop(stop2);
        route.addStop(stop3);

        assertEquals(2, route.getIndex(stop3));
    }

    public void testT19_getIndexMissingStop() {
        route.addStop(stop1);

        assertEquals(-1, route.getIndex(stop2));
    }

    public void testT20_getIndexNullStop() {
        route.addStop(stop1);

        assertEquals(-1, route.getIndex(null));
    }


    // getStopSublist

    public void testT21_getStopSublistNormalRange() {
        route.addStop(stop1);
        route.addStop(stop2);
        route.addStop(stop3);

        List<TradeRouteStop> result =
            route.getStopSublist(stop1, stop3);

        assertEquals(2, result.size());
        assertSame(stop1, result.get(0));
        assertSame(stop2, result.get(1));
    }

    public void testT22_getStopSublistWrapAround() {
        route.addStop(stop1);
        route.addStop(stop2);
        route.addStop(stop3);

        List<TradeRouteStop> result =
            route.getStopSublist(stop3, stop2);

        assertEquals(2, result.size());
        assertSame(stop3, result.get(0));
        assertSame(stop1, result.get(1));
    }

    public void testT23_getStopSublistSameStartAndEnd() {
        route.addStop(stop1);
        route.addStop(stop2);

        List<TradeRouteStop> result =
            route.getStopSublist(stop1, stop1);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    public void testT24_getStopSublistMissingStart() {
        route.addStop(stop1);
        route.addStop(stop2);

        assertNull(route.getStopSublist(stop3, stop2));
    }

    public void testT25_getStopSublistMissingEnd() {
        route.addStop(stop1);
        route.addStop(stop2);

        assertNull(route.getStopSublist(stop1, stop3));
    }

    public void testT26_getStopSublistBothMissing() {
        assertNull(route.getStopSublist(stop1, stop2));
    }


    // removeMatchingStops

    public void testT27_removeMatchingStopsEmptyRoute() {
        assertFalse(route.removeMatchingStops(colony));
        assertEquals(0, route.getStopCount());
    }

    public void testT28_removeMatchingStopsMatchingLocation() {
        route.addStop(stop1);
        route.addStop(stop2);

        assertTrue(route.removeMatchingStops(colony));
        assertEquals(0, route.getStopCount());
    }

    public void testT29_removeMatchingStopsNoMatchingLocation() {
        route.addStop(stop1);

        assertFalse(route.removeMatchingStops(null));
        assertEquals(1, route.getStopCount());
    }

    public void testT30_removeMatchingStopsOnlyMatchingStopsRemoved() {
        route.addStop(stop1);

        assertTrue(route.removeMatchingStops(colony));
        assertTrue(route.getStopList().isEmpty());
    }


    // Assigned units and stop validity

    public void testT31_getAssignedUnitsWithoutUnits() {
        assertNotNull(route.getAssignedUnits());
    }

    public void testT32_isStopValidNullPlayerStop() {
        assertFalse(TradeRoute.isStopValid(player, null));
    }

    public void testT33_isStopValidNullLocation() {
        TradeRouteStop invalid = new TradeRouteStop(game);

        assertFalse(TradeRoute.isStopValid(player, invalid));
    }

    public void testT34_isStopValidOwnedColony() {
        TradeRouteStop valid = new TradeRouteStop(game, colony);

        assertTrue(TradeRoute.isStopValid(player, valid));
    }

    public void testT35_isStopValidDifferentPlayer() {
        TradeRouteStop valid = new TradeRouteStop(game, colony);

        Player other = game.getPlayers(p -> p != player)
            .findFirst()
            .orElse(null);

        if (other != null) {
            assertFalse(TradeRoute.isStopValid(other, valid));
        }
    }


    // verify

    public void testT36_verifyNullName() {
        route.setName(null);

        assertNotNull(route.verify());
    }

    public void testT37_verifyNullOwner() {
        route.setOwner(null);

        assertNotNull(route.verify());
    }

    public void testT38_verifyNotEnoughStopsZero() {
        assertNotNull(route.verify());
    }

    public void testT39_verifyNotEnoughStopsOne() {
        route.addStop(stop1);

        assertNotNull(route.verify());
    }

    public void testT40_verifyInvalidStop() {
        TradeRouteStop invalid =
            new TradeRouteStop(game, colony.getTile());

        route.addStop(invalid);
        route.addStop(createStop(grain));

        assertNotNull(route.verify());
    }

    public void testT41_verifyAllCargoEmpty() {
        route.addStop(createStop());
        route.addStop(createStop());

        assertNotNull(route.verify());
    }

    public void testT42_verifyAtLeastOneNonEmptyStop() {
        route.addStop(createStop(grain));
        route.addStop(createStop());

        assertNull(route.verify());
    }

    public void testT43_verifyCargoPresentAtAllStops() {
        route.addStop(createStop(grain));
        route.addStop(createStop(grain));

        boolean enhanced = spec().getBoolean(
            GameOptions.ENHANCED_TRADE_ROUTES);

        assertEquals(enhanced, route.verify() == null);
    }

    public void testT44_verifyDifferentCargoAtStops() {
        route.addStop(createStop(grain));
        route.addStop(createStop(tools));

        assertNull(route.verify());
    }


    // verifyUniqueName

    public void testT45_verifyUniqueName() {
        assertNull(route.verifyUniqueName());
    }


    // copyIn

    public void testT46_copyInNull() {
        assertFalse(route.copyIn(null));
    }

    public void testT47_copyInWrongType() {
        assertFalse(route.copyIn(stop1));
    }

    public void testT48_copyInDifferentId() {
        TradeRoute source = new TradeRoute(game, "source.route");
        TradeRoute target = new TradeRoute(game, "target.route");

        assertFalse(target.copyIn(source));
    }

    public void testT49_copyInDifferentIdsReturnsFalse() {
        TradeRoute source =
            new TradeRoute(game, "source.route", player);
        source.addStop(createStop(grain));
        source.addStop(createStop(tools));

        TradeRoute target =
            new TradeRoute(game, "target.route", player);

        assertFalse(target.copyIn(source));
    }

    public void testT50_copyInDifferentIdsPreservesTargetStops() {
        TradeRoute source =
            new TradeRoute(game, "source.replace", player);
        source.addStop(createStop(grain));

        TradeRoute target =
            new TradeRoute(game, "target.replace", player);
        target.addStop(createStop(tools));
        target.addStop(createStop(grain));

        assertFalse(target.copyIn(source));
        assertEquals(2, target.getStopCount());
    }


    // XML tag and string representation

    public void testT51_getXMLTagName() {
        assertEquals(TradeRoute.TAG, route.getXMLTagName());
    }

    public void testT52_toStringWithoutStops() {
        String result = route.toString();

        assertTrue(result.contains("Test route"));
        assertTrue(result.contains("silent=false"));
    }

    public void testT53_toStringWithOwnerAndStops() {
        route.setSilent(true);
        route.addStop(createStop(grain));

        String result = route.toString();

        assertTrue(result.contains("Test route"));
        assertTrue(result.contains("owner=" + player.getId()));
        assertTrue(result.contains("silent=true"));
        assertTrue(result.contains(grain.toString()));
    }


    // Integrity

    public void testT54_checkIntegrityValidRoute() {
        route.addStop(createStop(grain));
        route.addStop(createStop(tools));

        LogBuilder lb = new LogBuilder(64);

        assertNotNull(route.checkIntegrity(false, lb));
    }

    public void testT55_checkIntegrityInvalidRoute() {
        route.setName(null);

        LogBuilder lb = new LogBuilder(64);

        assertNotNull(route.checkIntegrity(false, lb));
    }


    // XML serialization

    public void testT56_writeXMLAttributesAndChildren() throws Exception {
        route.setSilent(true);
        route.addStop(createStop(grain));
        route.addStop(createStop(tools));

        StringWriter output = new StringWriter();

        try (FreeColXMLWriter xw = new FreeColXMLWriter(output)) {
            route.toXML(xw);
        }

        String xml = output.toString();

        assertTrue(xml.contains(TradeRoute.TAG));
        assertTrue(xml.contains("Test route"));
        assertTrue(xml.contains("silent"));
        assertTrue(xml.contains(TradeRouteStop.TAG));
    }

    public void testT57_readXMLAttributesAndChildren() throws Exception {
        route.setSilent(true);
        route.addStop(createStop(grain));
        route.addStop(createStop(tools));

        StringWriter output = new StringWriter();

        try (FreeColXMLWriter xw = new FreeColXMLWriter(output)) {
            route.toXML(xw);
        }

        String xml = output.toString();

        try (FreeColXMLReader xr =
                 new FreeColXMLReader(new StringReader(xml))) {
            xr.nextTag();

            TradeRoute result =
                xr.readFreeColObject(game, TradeRoute.class);

            assertNotNull(result);
            assertEquals("Test route", result.getName());
            assertSame(player, result.getOwner());
            assertTrue(result.isSilent());
            assertEquals(2, result.getStopCount());
            assertEquals(createStop(grain).getCargo(),
                         result.getStop(0).getCargo());
            assertEquals(createStop(tools).getCargo(),
                         result.getStop(1).getCargo());
        }
    }
   public void testT58_verifyUniqueNameDuplicate() {
    TradeRoute existing =
        new TradeRoute(game, "Test route", player);

    player.addTradeRoute(existing);

    assertNotNull(route.verifyUniqueName());
}
public void testT59_copyInSameIdSuccessfully() {
    route.setName("Source route");
    route.setSilent(true);
    route.addStop(createStop(grain));
    route.addStop(createStop(tools));

    TradeRoute target = route.copy(game, TradeRoute.class);

    assertNotNull(target);
    assertEquals(route.getId(), target.getId());

    assertTrue(target.copyIn(route));

    assertEquals("Source route", target.getName());
    assertSame(player, target.getOwner());
    assertEquals(2, target.getStopCount());

    assertEquals(createStop(grain).getCargo(),
                 target.getStop(0).getCargo());
    assertEquals(createStop(tools).getCargo(),
                 target.getStop(1).getCargo());
}
}
