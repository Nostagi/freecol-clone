package net.sf.freecol.common.model;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.BlockJUnit4ClassRunner;

import net.sf.freecol.util.test.MockModelTestCase;


@RunWith(BlockJUnit4ClassRunner.class)
public class TileImprovementCfgTest extends MockModelTestCase {

    @Test
    public void testImprovementCompletionAndConnections() {
        Game game = getStandardGame();
        Map map = getTestMap(spec().getTileType("model.tile.plains"), true);
        game.changeMap(map);
        Tile tile = map.getTile(5, 5);
        TileImprovement road = new TileImprovement(game, tile,
            spec().getTileImprovementType("model.improvement.road"), null);

        assertTrue(road.isRoad());
        assertFalse(road.isRiver());
        assertFalse(road.isNatural());
        assertNotNull(road.getConnectionDirections());
        assertFalse(road.isComplete());
        road.setTurnsToComplete(0);
        assertTrue(road.isComplete());
        road.setTurnsToComplete(2);
        assertFalse(road.isComplete());
        road.setMagnitude(4);
        assertEquals(4, road.getMagnitude());
        road.setVirtual(true);
        assertTrue(road.getVirtual());

        TileImprovement river = new TileImprovement(game, tile,
            spec().getTileImprovementType("model.improvement.river"), null);
        assertTrue(river.isRiver());
        assertFalse(river.isRoad());
        assertTrue(river.isNatural());
        assertTrue(river.isComplete());
        assertNotNull(river.getConnectionDirections());
    }

    @Test
    public void testRoadStyleAndMovementCost() {
        Game game = getStandardGame();
        Tile tile = getTestMap(spec().getTileType("model.tile.plains"), true)
            .getTile(5, 5);
        TileImprovement road = new TileImprovement(game, tile,
            spec().getTileImprovementType("model.improvement.road"), null);
        Direction direction = Direction.allDirections.get(0);

        road.setStyle(TileImprovementStyle.getInstance("10000000"));
        assertTrue(road.isConnectedTo(direction));
        assertFalse(road.isConnectedTo(Direction.allDirections.get(1)));
        assertTrue(road.getConnectionsFromStyle() != 0L);
        assertEquals(3, road.getMoveCost(direction, 3));

        road.setTurnsToComplete(0);
        assertEquals(road.getType().getMoveCost(3),
                     road.getMoveCost(direction, 3));
        assertEquals(0L, new TileImprovement(game, tile,
            spec().getTileImprovementType("model.improvement.road"), null)
            .getConnectionsFromStyle());
    }

    @Test
    public void testRiverStyleCreatesAndRemovesConnections() {
        Game game = getStandardGame();
        Map map = getTestMap(spec().getTileType("model.tile.plains"), true);
        game.changeMap(map);
        Tile tile = map.getTile(5, 5);
        TileImprovement river = tile.addRiver(1, null);

        river.setRiverStyle("1000");
        assertTrue(river.isConnectedTo(Direction.longSides.get(0)));
        assertNotNull(tile.getNeighbourOrNull(Direction.longSides.get(0))
                      .getRiver());
        assertEquals(1, river.getMagnitude());

        river.updateRiverConnections(null);
        for (Direction direction : Direction.longSides) {
            assertFalse(river.isConnectedTo(direction));
        }
    }

    @Test
    public void testRoadConnectionUpdateRequiresCompletedRoad() {
        Game game = getStandardGame();
        Map map = getTestMap(spec().getTileType("model.tile.plains"), true);
        game.changeMap(map);
        Tile tile = map.getTile(5, 5);
        TileImprovement road = tile.addRoad();
        Direction direction = Direction.allDirections.get(0);
        Tile neighbour = tile.getNeighbourOrNull(direction);
        TileImprovement neighbourRoad = neighbour.addRoad();

        assertNull(road.updateRoadConnections(true));
        road.setTurnsToComplete(0);
        neighbourRoad.setTurnsToComplete(1);
        assertNotNull(road.updateRoadConnections(true));
        assertFalse(road.isConnectedTo(direction));

        neighbourRoad.setTurnsToComplete(0);
        road.updateRoadConnections(true);
        assertTrue(road.isConnectedTo(direction));
        assertTrue(neighbourRoad.isConnectedTo(direction.getReverseDirection()));
        road.updateRoadConnections(false);
        assertFalse(road.isConnectedTo(direction));
        assertFalse(neighbourRoad.isConnectedTo(direction.getReverseDirection()));
    }

    @Test
    public void testNullImprovementTypeIsRejected() {
        Game game = getStandardGame();
        Tile tile = getTestMap(spec().getTileType("model.tile.plains"), true)
            .getTile(5, 5);
        try {
            new TileImprovement(game, tile, null, null);
            fail("A tile improvement requires a type");
        } catch (RuntimeException expected) {
            assertTrue(expected.getMessage().contains("Type must not be null"));
        }
    }
}
