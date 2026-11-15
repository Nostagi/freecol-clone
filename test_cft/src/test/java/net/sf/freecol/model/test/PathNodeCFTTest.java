
package net.sf.freecol.common.model;

import net.sf.freecol.util.test.FreeColTestCase;


public class PathNodeCFTTest extends FreeColTestCase {

    private PathNode node(int movesLeft, int turns, boolean onCarrier) {
        return new PathNode(null, movesLeft, turns, onCarrier, null, null);
    }

    private PathNode link(PathNode previous, PathNode next,
                          int movesLeft, int turns, boolean onCarrier) {
        return new PathNode(null, movesLeft, turns,
                            onCarrier, previous, next);
    }

    // P01
    public void testP01_constructorAndGetters() {
        PathNode n = node(5, 2, true);

        assertNull(n.getLocation());
        assertNull(n.getTile());
        assertEquals(5, n.getMovesLeft());
        assertEquals(2, n.getTurns());
        assertTrue(n.isOnCarrier());
    }

    // P02
    public void testP02_setMovesLeft() {
        PathNode n = node(5, 2, false);
        n.setMovesLeft(3);

        assertEquals(3, n.getMovesLeft());
    }

    // P03
    public void testP03_setTurns() {
        PathNode n = node(5, 2, false);
        n.setTurns(4);

        assertEquals(4, n.getTurns());
    }

    // P04
    public void testP04_addTurnsSingleNode() {
        PathNode n = node(5, 2, false);
        n.addTurns(3);

        assertEquals(5, n.getTurns());
    }

    // P05
    public void testP05_addTurnsMultipleNodes() {
        PathNode n1 = node(5, 1, false);
        PathNode n2 = link(n1, null, 4, 2, false);
        PathNode n3 = link(n2, null, 3, 3, false);

        n1.next = n2;
        n2.next = n3;

        n1.addTurns(2);

        assertEquals(3, n1.getTurns());
        assertEquals(4, n2.getTurns());
        assertEquals(5, n3.getTurns());
    }

    // P06
    public void testP06_addZeroTurns() {
        PathNode n1 = node(5, 1, false);
        PathNode n2 = link(n1, null, 4, 2, false);
        n1.next = n2;

        n1.addTurns(0);

        assertEquals(1, n1.getTurns());
        assertEquals(2, n2.getTurns());
    }

    // P07
    public void testP07_setOnCarrier() {
        PathNode n = node(5, 2, false);
        n.setOnCarrier(true);

        assertTrue(n.isOnCarrier());
    }

    // P08
    public void testP08_getLengthSingleNode() {
        PathNode n = node(5, 2, false);

        assertEquals(1, n.getLength());
    }

    // P09
    public void testP09_getLengthMultipleNodes() {
        PathNode n1 = node(5, 1, false);
        PathNode n2 = link(n1, null, 4, 2, false);
        PathNode n3 = link(n2, null, 3, 3, false);

        n1.next = n2;
        n2.next = n3;

        assertEquals(3, n1.getLength());
    }

    // P10
    public void testP10_getDirectionWithoutPrevious() {
        PathNode n = node(5, 2, false);

        assertNull(n.getDirection());
    }

    // P11
    public void testP11_getDirectionWithNullPreviousTile() {
        PathNode previous = node(5, 1, false);
        PathNode current = link(previous, null, 4, 2, false);

        assertNull(current.getDirection());
    }

    // P12
    public void testP12_getDirectionWithNullCurrentTile() {
        PathNode previous = node(5, 1, false);
        PathNode current = link(previous, null, 4, 2, false);

        previous.next = current;

        assertNull(current.getDirection());
    }

    // P13
    public void testP13_getTransportDropNodeWithoutCarrier() {
        PathNode n1 = node(5, 1, false);
        PathNode n2 = link(n1, null, 4, 2, false);
        n1.next = n2;

        assertSame(n1, n1.getTransportDropNode());
    }

    // P14
    public void testP14_getTransportDropNodeWithCarrier() {
        PathNode n1 = node(5, 1, true);
        PathNode n2 = link(n1, null, 4, 2, false);
        PathNode n3 = link(n2, null, 3, 3, false);

        n1.next = n2;
        n2.next = n3;

        assertSame(n2, n1.getTransportDropNode());
    }

    // P15
    public void testP15_getTransportDropNodeAllCarrier() {
        PathNode n1 = node(5, 1, true);
        PathNode n2 = link(n1, null, 4, 2, true);

        n1.next = n2;

        assertSame(n2, n1.getTransportDropNode());
    }

    // P16
    public void testP16_getFirstNodeSingleNode() {
        PathNode n = node(5, 2, false);

        assertSame(n, n.getFirstNode());
    }

    // P17
    public void testP17_getFirstNodeMultipleNodes() {
        PathNode n1 = node(5, 1, false);
        PathNode n2 = link(n1, null, 4, 2, false);
        PathNode n3 = link(n2, null, 3, 3, false);

        n1.next = n2;
        n2.next = n3;

        assertSame(n1, n3.getFirstNode());
    }

    // P18
    public void testP18_getLastNodeSingleNode() {
        PathNode n = node(5, 2, false);

        assertSame(n, n.getLastNode());
    }

    // P19
    public void testP19_getLastNodeMultipleNodes() {
        PathNode n1 = node(5, 1, false);
        PathNode n2 = link(n1, null, 4, 2, false);
        PathNode n3 = link(n2, null, 3, 3, false);

        n1.next = n2;
        n2.next = n3;

        assertSame(n3, n1.getLastNode());
    }

    // P20
    public void testP20_getTotalTurnsWithRemainingMoves() {
        PathNode n1 = node(5, 1, false);
        PathNode n2 = link(n1, null, 3, 4, false);
        n1.next = n2;

        assertEquals(4, n1.getTotalTurns());
    }

    // P21
    public void testP21_getTotalTurnsWithoutRemainingMoves() {
        PathNode n1 = node(5, 1, false);
        PathNode n2 = link(n1, null, 0, 4, false);
        n1.next = n2;

        assertEquals(5, n1.getTotalTurns());
    }

    // P22
    public void testP22_getTransportDropTurns() {
        PathNode n1 = node(5, 1, true);
        PathNode n2 = link(n1, null, 4, 3, false);

        n1.next = n2;

        assertEquals(3, n1.getTransportDropTurns());
    }

    // P23
    public void testP23_getNodeCost() {
        assertEquals(0, PathNode.getNodeCost(0, 100));
        assertEquals(97, PathNode.getNodeCost(0, 3));
        assertEquals(298, PathNode.getNodeCost(2, 2));
        assertEquals(305, PathNode.getNodeCost(2, -5));
    }

    // P24
    public void testP24_getNodeCostInfinity() {
        assertEquals(Constants.INFINITY,
                     PathNode.getNodeCost(Integer.MAX_VALUE, 0));
    }

    // P25
    public void testP25_getCost() {
        PathNode n = node(3, 2, false);

        assertEquals(PathNode.getNodeCost(2, 3), n.getCost());
    }

    // P26
    public void testP26_getCarrierMoveWithoutCarrier() {
        PathNode n1 = node(5, 1, false);
        PathNode n2 = link(n1, null, 4, 2, false);
        n1.next = n2;

        assertNull(n1.getCarrierMove());
    }

    // P27
    public void testP27_getCarrierMoveAtFirstNode() {
        PathNode n1 = node(5, 1, true);
        PathNode n2 = link(n1, null, 4, 2, false);
        n1.next = n2;

        assertSame(n1, n1.getCarrierMove());
    }

    // P28
    public void testP28_getCarrierMoveAtLaterNode() {
        PathNode n1 = node(5, 1, false);
        PathNode n2 = link(n1, null, 4, 2, true);
        PathNode n3 = link(n2, null, 3, 3, false);

        n1.next = n2;
        n2.next = n3;

        assertSame(n2, n1.getCarrierMove());
    }

    // P29
    public void testP29_usesCarrierTrue() {
        PathNode n1 = node(5, 1, false);
        PathNode n2 = link(n1, null, 4, 2, true);
        n1.next = n2;

        assertTrue(n1.usesCarrier());
    }

    // P30
    public void testP30_usesCarrierFalse() {
        PathNode n1 = node(5, 1, false);
        PathNode n2 = link(n1, null, 4, 2, false);
        n1.next = n2;

        assertFalse(n1.usesCarrier());
    }

    // P31
    public void testP31_embarkedThisTurnTrue() {
        PathNode p1 = node(5, 2, false);
        PathNode p2 = link(p1, null, 4, 2, true);
        PathNode p3 = link(p2, null, 3, 2, true);

        p2.previous = p1;
        p3.previous = p2;

        assertTrue(p3.embarkedThisTurn(2));
    }

    // P32
    public void testP32_embarkedThisTurnEarlierTurn() {
        PathNode p1 = node(5, 1, false);
        PathNode p2 = link(p1, null, 4, 2, true);

        p2.previous = p1;

        assertFalse(p2.embarkedThisTurn(3));
    }

    // P33
    public void testP33_embarkedThisTurnAllNodesOnCarrier() {
        PathNode p1 = node(5, 2, true);
        PathNode p2 = link(p1, null, 4, 2, true);
        PathNode p3 = link(p2, null, 3, 2, true);

        p2.previous = p1;
        p3.previous = p2;

        assertFalse(p3.embarkedThisTurn(2));
    }

    // P34
    public void testP34_embarkedThisTurnNonCarrierAtCurrentNode() {
        PathNode p1 = node(5, 2, true);
        PathNode p2 = link(p1, null, 4, 3, false);

        p2.previous = p1;

        assertTrue(p2.embarkedThisTurn(3));
    }

    // P35
    public void testP35_convertToGoodsDeliveryPathSingleNode() {
        PathNode n = node(5, 2, false);

        n.convertToGoodsDeliveryPath();

        assertTrue(n.isOnCarrier());
        assertNotNull(n.next);
        assertFalse(n.next.isOnCarrier());
    }

    // P36
    public void testP36_convertToGoodsDeliveryPathMultipleNodes() {
        PathNode n1 = node(5, 1, false);
        PathNode n2 = link(n1, null, 4, 2, false);
        PathNode n3 = link(n2, null, 3, 3, false);

        n1.next = n2;
        n2.next = n3;

        n1.convertToGoodsDeliveryPath();

        assertTrue(n1.isOnCarrier());
        assertTrue(n2.isOnCarrier());
        assertTrue(n3.isOnCarrier());
        assertNotNull(n3.next);
        assertFalse(n3.next.isOnCarrier());
    }

    // P37
    public void testP37_ensureDisembarkWhenNotOnCarrier() {
        PathNode n = node(5, 2, false);

        n.ensureDisembark();

        assertNull(n.next);
    }

    // P38
    public void testP38_ensureDisembarkWhenOnCarrier() {
        PathNode n = node(5, 2, true);

        n.ensureDisembark();

        assertNotNull(n.next);
        assertFalse(n.next.isOnCarrier());
        assertSame(n, n.next.previous);
        assertEquals(n.getMovesLeft(), n.next.getMovesLeft());
        assertEquals(n.getTurns(), n.next.getTurns());
    }

    // P39
    public void testP39_toString() {
        PathNode n = node(3, 2, false);

        String result = n.toString();

        assertNotNull(result);
        assertTrue(result.length() > 0);
    }

    // P40
    public void testP40_fullPathToString() {
        PathNode n1 = node(5, 1, false);
        PathNode n2 = link(n1, null, 4, 2, false);

        n1.next = n2;

        String result = n1.fullPathToString();

        assertNotNull(result);
        assertTrue(result.length() > 0);
        assertTrue(result.contains("\n"));
    }
}
