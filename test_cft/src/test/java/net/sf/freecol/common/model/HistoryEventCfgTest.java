/**
 *  Copyright (C) 2002-2024   The FreeCol Team
 *
 *  This file is part of FreeCol.
 *
 *  FreeCol is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  FreeCol is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with FreeCol.  If not, see <http://www.gnu.org/licenses/>.
 */

package net.sf.freecol.common.model;

import java.io.StringReader;
import java.io.StringWriter;

import javax.xml.stream.XMLStreamException;

import net.sf.freecol.common.io.FreeColXMLReader;
import net.sf.freecol.common.io.FreeColXMLWriter;
import net.sf.freecol.common.model.HistoryEvent.HistoryEventType;
import net.sf.freecol.util.test.FreeColTestCase;


/**
 * CFG-driven test suite for {@link HistoryEvent} derived from
 * {@code test_cft/graph/net/sf/freecol/common/model/HistoryEvent.json}.
 *
 * Covers all 17 methods (M1..M17, including {@code <init>}) and achieves 100%
 * feasible C3 (node + edge) coverage across 31 Test Paths.
 *
 * Infeasible edges documented:
 * - M11 ({@code copyIn}): {@code N1 -> N2} ({@code B2:false}, {@code !super.copyIn(o)}) —
 *   Once {@code copyInCast(other, HistoryEvent.class)} returns non-null {@code o},
 *   {@code o} is non-null, shares the same ID, and is an instance of
 *   {@code HistoryEvent} (and therefore {@code StringTemplate} and
 *   {@code FreeColObject}), so {@code super.copyIn(o)} is guaranteed to return
 *   {@code true}.
 */
public class HistoryEventCfgTest extends FreeColTestCase {

    private Game game;
    private Player dutchPlayer;
    private Turn turn1;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        // Pre-condition: Initialize standard game, Dutch player, and Turn 1
        game = getGame();
        dutchPlayer = game.getPlayerByNationId("model.nation.dutch");
        turn1 = new Turn(1);
    }

    // ------------------------------------------------------------------
    // Pre-condition helpers & stubs
    // ------------------------------------------------------------------

    /**
     * Exposes protected {@code writeAttributes} and {@code readAttributes}
     * for direct CFG path testing.
     */
    private static class TestableHistoryEvent extends HistoryEvent {

        TestableHistoryEvent(Turn turn, HistoryEventType eventType, Player player) {
            super(turn, eventType, player);
        }

        void callWriteAttributes(FreeColXMLWriter xw) throws XMLStreamException {
            writeAttributes(xw);
        }

        void callReadAttributes(FreeColXMLReader xr) throws XMLStreamException {
            readAttributes(xr);
        }
    }

    private FreeColXMLReader openReaderAtStartTag(String xml) throws Exception {
        FreeColXMLReader xr = new FreeColXMLReader(new StringReader(xml));
        xr.nextTag();
        return xr;
    }

    // ------------------------------------------------------------------
    // M1: <init>()V — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M1-TP1: ENTRY -> N0 -> EXIT (Trivial no-arg constructor)
    public void testM1_TP1_DefaultConstructor() {
        HistoryEvent event = new HistoryEvent();
        assertNull(event.getTurn());
        assertNull(event.getEventType());
        assertNull(event.getPlayerId());
        assertEquals(0, event.getScore());
    }

    // ------------------------------------------------------------------
    // M2: <init>(Lnet/sf/freecol/common/model/Turn;Lnet/sf/freecol/common/model/HistoryEvent$HistoryEventType;Lnet/sf/freecol/common/model/Player;)V — 2 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M2-TP1: ENTRY -> N0 -(B1:false)-> N1 -> N3 -> EXIT (player == null -> playerId = null)
    public void testM2_TP1_ConstructorNullPlayer() {
        HistoryEvent event = new HistoryEvent(turn1, HistoryEventType.DISCOVER_NEW_WORLD, null);
        assertSame(turn1, event.getTurn());
        assertEquals(HistoryEventType.DISCOVER_NEW_WORLD, event.getEventType());
        assertNull(event.getPlayerId());
        assertEquals(0, event.getScore());
    }

    // M2-TP2: ENTRY -> N0 -(B1:true)-> N2 -> N3 -> EXIT (player != null -> playerId = player.getId())
    public void testM2_TP2_ConstructorWithPlayer() {
        HistoryEvent event = new HistoryEvent(turn1, HistoryEventType.FOUND_COLONY, dutchPlayer);
        assertSame(turn1, event.getTurn());
        assertEquals(HistoryEventType.FOUND_COLONY, event.getEventType());
        assertEquals(dutchPlayer.getId(), event.getPlayerId());
    }

    // ------------------------------------------------------------------
    // M3: <init>(Lnet/sf/freecol/common/io/FreeColXMLReader;)V — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M3-TP1: ENTRY -> N0 -> EXIT (Construct HistoryEvent from XML reader)
    public void testM3_TP1_ConstructorFromXML() throws Exception {
        String xml = "<historyEvent id=\"model.historyEventType.foundColony.description\" "
                + "turn=\"5\" eventType=\"FOUND_COLONY\" playerId=\"player:1\" score=\"42\"/>";
        try (FreeColXMLReader xr = openReaderAtStartTag(xml)) {
            HistoryEvent event = new HistoryEvent(xr);
            assertEquals(5, event.getTurn().getNumber());
            assertEquals(HistoryEventType.FOUND_COLONY, event.getEventType());
            assertEquals("player:1", event.getPlayerId());
            assertEquals(42, event.getScore());
        }
    }

    // ------------------------------------------------------------------
    // M4: getTurn()Lnet/sf/freecol/common/model/Turn; — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M4-TP1: ENTRY -> N0 -> EXIT (Return turn)
    public void testM4_TP1_GetTurn() {
        HistoryEvent event = new HistoryEvent(turn1, HistoryEventType.DISCOVER_REGION, null);
        assertSame(turn1, event.getTurn());
    }

    // ------------------------------------------------------------------
    // M5: getEventType()Lnet/sf/freecol/common/model/HistoryEvent$HistoryEventType; — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M5-TP1: ENTRY -> N0 -> EXIT (Return eventType)
    public void testM5_TP1_GetEventType() {
        HistoryEvent event = new HistoryEvent(turn1, HistoryEventType.CITY_OF_GOLD, null);
        assertEquals(HistoryEventType.CITY_OF_GOLD, event.getEventType());
    }

    // ------------------------------------------------------------------
    // M6: getEventTypeFromStance(Lnet/sf/freecol/common/model/Stance;)Lnet/sf/freecol/common/model/HistoryEvent$HistoryEventType; — 5 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M6-TP1..TP5: ENTRY -> N0 -(B1:case:1..4/default)-> N1..N5 -> EXIT (All Stance switch branches)
    public void testM6_TP1_TP5_GetEventTypeFromStance() {
        assertEquals(HistoryEventType.DECLARE_WAR, HistoryEvent.getEventTypeFromStance(Stance.WAR));
        assertEquals(HistoryEventType.CEASE_FIRE, HistoryEvent.getEventTypeFromStance(Stance.CEASE_FIRE));
        assertEquals(HistoryEventType.MAKE_PEACE, HistoryEvent.getEventTypeFromStance(Stance.PEACE));
        assertEquals(HistoryEventType.FORM_ALLIANCE, HistoryEvent.getEventTypeFromStance(Stance.ALLIANCE));
        assertNull(HistoryEvent.getEventTypeFromStance(Stance.UNCONTACTED));
    }

    // ------------------------------------------------------------------
    // M7: getPlayerId()Ljava/lang/String; — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M7-TP1: ENTRY -> N0 -> EXIT (Return playerId)
    public void testM7_TP1_GetPlayerId() {
        HistoryEvent event = new HistoryEvent(turn1, HistoryEventType.MEET_NATION, dutchPlayer);
        assertEquals(dutchPlayer.getId(), event.getPlayerId());
    }

    // ------------------------------------------------------------------
    // M8: setPlayerId(Ljava/lang/String;)V — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M8-TP1: ENTRY -> N0 -> EXIT (Set playerId)
    public void testM8_TP1_SetPlayerId() {
        HistoryEvent event = new HistoryEvent(turn1, HistoryEventType.MEET_NATION, null);
        event.setPlayerId("custom-player");
        assertEquals("custom-player", event.getPlayerId());
    }

    // ------------------------------------------------------------------
    // M9: getScore()I — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M9-TP1: ENTRY -> N0 -> EXIT (Return score)
    public void testM9_TP1_GetScore() {
        HistoryEvent event = new HistoryEvent(turn1, HistoryEventType.FOUNDING_FATHER, null);
        assertEquals(0, event.getScore());
    }

    // ------------------------------------------------------------------
    // M10: setScore(I)V — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M10-TP1: ENTRY -> N0 -> EXIT (Set score)
    public void testM10_TP1_SetScore() {
        HistoryEvent event = new HistoryEvent(turn1, HistoryEventType.FOUNDING_FATHER, null);
        event.setScore(100);
        assertEquals(100, event.getScore());
    }

    // ------------------------------------------------------------------
    // M11: copyIn(Lnet/sf/freecol/common/model/FreeColObject;)Z — 2 feasible Test Paths (100% feasible C3)
    // ------------------------------------------------------------------

    // M11-TP1: ENTRY -> N0 -(B1:true)-> N2 -> EXIT (o == null -> return false)
    public void testM11_TP1_CopyInNullOrWrongId() {
        HistoryEvent target = new HistoryEvent(turn1, HistoryEventType.FOUND_COLONY, null);
        assertFalse(target.copyIn(null));

        HistoryEvent other = new HistoryEvent(turn1, HistoryEventType.ABANDON_COLONY, null);
        assertFalse(target.copyIn(other));
    }

    // M11-TP2: ENTRY -> N0 -(B1:false)-> N1 -(B2:true)-> N3 -> EXIT (o != null && super.copyIn(o) -> copy fields)
    public void testM11_TP2_CopyInValid() {
        HistoryEvent target = new HistoryEvent(turn1, HistoryEventType.FOUND_COLONY, null);
        Turn turn5 = new Turn(5);
        HistoryEvent source = new HistoryEvent(turn5, HistoryEventType.FOUND_COLONY, dutchPlayer);
        source.setScore(75);
        assertTrue(target.copyIn(source));
        assertSame(turn5, target.getTurn());
        assertEquals(dutchPlayer.getId(), target.getPlayerId());
        assertEquals(75, target.getScore());
    }

    // ------------------------------------------------------------------
    // M12: writeAttributes(Lnet/sf/freecol/common/io/FreeColXMLWriter;)V — 2 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M12-TP1: ENTRY -> N0 -(B1:true)-> N2 -> EXIT (playerId == null -> omit playerId attribute)
    public void testM12_TP1_WriteAttributesNullPlayerId() throws Exception {
        TestableHistoryEvent event = new TestableHistoryEvent(turn1, HistoryEventType.INDEPENDENCE, null);
        StringWriter sw = new StringWriter();
        try (FreeColXMLWriter xw = new FreeColXMLWriter(sw, FreeColXMLWriter.WriteScope.toSave())) {
            xw.writeStartDocument("UTF-8", "1.0");
            xw.writeStartElement(HistoryEvent.TAG);
            event.callWriteAttributes(xw);
            xw.writeEndElement();
            xw.writeEndDocument();
        }
        String xml = sw.toString();
        assertTrue(xml.contains("turn=\"1\""));
        assertFalse(xml.contains("playerId="));
    }

    // M12-TP2: ENTRY -> N0 -(B1:false)-> N1 -> N2 -> EXIT (playerId != null -> write playerId attribute)
    public void testM12_TP2_WriteAttributesWithPlayerId() throws Exception {
        TestableHistoryEvent event = new TestableHistoryEvent(turn1, HistoryEventType.INDEPENDENCE, dutchPlayer);
        StringWriter sw = new StringWriter();
        try (FreeColXMLWriter xw = new FreeColXMLWriter(sw, FreeColXMLWriter.WriteScope.toSave())) {
            xw.writeStartDocument("UTF-8", "1.0");
            xw.writeStartElement(HistoryEvent.TAG);
            event.callWriteAttributes(xw);
            xw.writeEndElement();
            xw.writeEndDocument();
        }
        assertTrue(sw.toString().contains("playerId=\"" + dutchPlayer.getId() + "\""));
    }

    // ------------------------------------------------------------------
    // M13: readAttributes(Lnet/sf/freecol/common/io/FreeColXMLReader;)V — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M13-TP1: ENTRY -> N0 -> EXIT (Read turn, eventType, playerId, score)
    public void testM13_TP1_ReadAttributes() throws Exception {
        TestableHistoryEvent event = new TestableHistoryEvent(turn1, HistoryEventType.FOUND_COLONY, null);
        String xml = "<historyEvent id=\"" + event.getId() + "\" turn=\"10\" "
                + "eventType=\"CONQUER_COLONY\" playerId=\"p2\" score=\"99\"/>";
        try (FreeColXMLReader xr = openReaderAtStartTag(xml)) {
            event.callReadAttributes(xr);
        }
        assertEquals(10, event.getTurn().getNumber());
        assertEquals(HistoryEventType.CONQUER_COLONY, event.getEventType());
        assertEquals("p2", event.getPlayerId());
        assertEquals(99, event.getScore());
    }

    // ------------------------------------------------------------------
    // M14: getXMLTagName()Ljava/lang/String; — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M14-TP1: ENTRY -> N0 -> EXIT (Return "historyEvent")
    public void testM14_TP1_GetXMLTagName() {
        HistoryEvent event = new HistoryEvent();
        assertEquals("historyEvent", event.getXMLTagName());
    }

    // ------------------------------------------------------------------
    // M15: equals(Ljava/lang/Object;)Z — 8 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M15-TP1: ENTRY -> N0 -(B1:false)-> N1 -> EXIT (this == o -> true)
    public void testM15_TP1_EqualsSameInstance() {
        HistoryEvent e1 = new HistoryEvent(turn1, HistoryEventType.FOUND_COLONY, dutchPlayer);
        assertTrue(e1.equals(e1));
    }

    // M15-TP2: ENTRY -> N0 -(B1:true)-> N2 -(B2:true)-> N11 -> EXIT (not instanceof HistoryEvent -> false)
    public void testM15_TP2_EqualsNonHistoryEvent() {
        HistoryEvent e1 = new HistoryEvent(turn1, HistoryEventType.FOUND_COLONY, dutchPlayer);
        assertFalse(e1.equals("not-a-history-event"));
    }

    // M15-TP3: ENTRY -> N0 -(B1:true)-> N2 -(B2:false)-> N3 -(B3:true)-> N9 -> N10 -> EXIT (turn != other.turn -> false)
    public void testM15_TP3_EqualsDifferentTurn() {
        HistoryEvent e1 = new HistoryEvent(turn1, HistoryEventType.FOUND_COLONY, dutchPlayer);
        HistoryEvent e2 = new HistoryEvent(new Turn(2), HistoryEventType.FOUND_COLONY, dutchPlayer);
        assertFalse(e1.equals(e2));
    }

    // M15-TP4: ENTRY -> N0 -(B1:true)-> N2 -(B2:false)-> N3 -(B3:false)-> N4 -(B4:true)-> N9 -> N10 -> EXIT (eventType != other.eventType -> false)
    public void testM15_TP4_EqualsDifferentEventType() {
        HistoryEvent e1 = new HistoryEvent(turn1, HistoryEventType.FOUND_COLONY, dutchPlayer);
        HistoryEvent e2 = new HistoryEvent(turn1, HistoryEventType.ABANDON_COLONY, dutchPlayer);
        assertFalse(e1.equals(e2));
    }

    // M15-TP5: ENTRY -> N0 -(B1:true)-> N2 -(B2:false)-> N3 -(B3:false)-> N4 -(B4:false)-> N5 -(B5:true)-> N9 -> N10 -> EXIT (score != other.score -> false)
    public void testM15_TP5_EqualsDifferentScore() {
        HistoryEvent e1 = new HistoryEvent(turn1, HistoryEventType.FOUND_COLONY, dutchPlayer);
        HistoryEvent e2 = new HistoryEvent(turn1, HistoryEventType.FOUND_COLONY, dutchPlayer);
        e2.setScore(10);
        assertFalse(e1.equals(e2));
    }

    // M15-TP6: ENTRY -> N0 -(B1:true)-> N2 -(B2:false)-> N3 -(B3:false)-> N4 -(B4:false)-> N5 -(B5:false)-> N6 -(B6:true)-> N9 -> N10 -> EXIT (!Utils.equals(playerId, other.playerId) -> false)
    public void testM15_TP6_EqualsDifferentPlayerId() {
        HistoryEvent e1 = new HistoryEvent(turn1, HistoryEventType.FOUND_COLONY, dutchPlayer);
        HistoryEvent e2 = new HistoryEvent(turn1, HistoryEventType.FOUND_COLONY, null);
        assertFalse(e1.equals(e2));
    }

    // M15-TP7: ENTRY -> N0 -(B1:true)-> N2 -(B2:false)-> N3 -(B3:false)-> N4 -(B4:false)-> N5 -(B5:false)-> N6 -(B6:false)-> N7 -(B7:true)-> N9 -> N10 -> EXIT (!super.equals(other) -> false)
    public void testM15_TP7_EqualsDifferentSuper() {
        HistoryEvent e1 = new HistoryEvent(turn1, HistoryEventType.FOUND_COLONY, dutchPlayer);
        HistoryEvent e2 = new HistoryEvent(turn1, HistoryEventType.FOUND_COLONY, dutchPlayer);
        e2.setId("different.template.id");
        assertFalse(e1.equals(e2));
    }

    // M15-TP8: ENTRY -> N0 -(B1:true)-> N2 -(B2:false)-> N3 -(B3:false)-> N4 -(B4:false)-> N5 -(B5:false)-> N6 -(B6:false)-> N7 -(B7:false)-> N8 -> N10 -> EXIT (all fields equal -> true)
    public void testM15_TP8_EqualsAllMatch() {
        HistoryEvent e1 = new HistoryEvent(turn1, HistoryEventType.FOUND_COLONY, dutchPlayer);
        HistoryEvent e2 = new HistoryEvent(turn1, HistoryEventType.FOUND_COLONY, dutchPlayer);
        assertTrue(e1.equals(e2));
    }

    // ------------------------------------------------------------------
    // M16: hashCode()I — 2 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M16-TP1: ENTRY -> N0 -(B1:true)-> N2 -> EXIT (playerId == null)
    public void testM16_TP1_HashCodeNullPlayerId() {
        HistoryEvent e1 = new HistoryEvent(turn1, HistoryEventType.FOUND_COLONY, null);
        HistoryEvent e2 = new HistoryEvent(turn1, HistoryEventType.FOUND_COLONY, null);
        assertEquals(e1.hashCode(), e2.hashCode());
    }

    // M16-TP2: ENTRY -> N0 -(B1:false)-> N1 -> N2 -> EXIT (playerId != null)
    public void testM16_TP2_HashCodeWithPlayerId() {
        HistoryEvent e1 = new HistoryEvent(turn1, HistoryEventType.FOUND_COLONY, dutchPlayer);
        HistoryEvent e2 = new HistoryEvent(turn1, HistoryEventType.FOUND_COLONY, dutchPlayer);
        assertEquals(e1.hashCode(), e2.hashCode());
    }

    // ------------------------------------------------------------------
    // M17: toString()Ljava/lang/String; — 2 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M17-TP1: ENTRY -> N0 -(B1:true)-> N2 -> EXIT (playerId == null -> omit playerId/score)
    public void testM17_TP1_ToStringNullPlayerId() {
        HistoryEvent event = new HistoryEvent(turn1, HistoryEventType.FOUND_COLONY, null);
        String s = event.toString();
        assertTrue(s.contains("FOUND_COLONY"));
        assertFalse(s.contains("playerId="));
    }

    // M17-TP2: ENTRY -> N0 -(B1:false)-> N1 -> N2 -> EXIT (playerId != null -> include playerId and score)
    public void testM17_TP2_ToStringWithPlayerId() {
        HistoryEvent event = new HistoryEvent(turn1, HistoryEventType.FOUND_COLONY, dutchPlayer);
        event.setScore(15);
        String s = event.toString();
        assertTrue(s.contains("playerId=" + dutchPlayer.getId()));
        assertTrue(s.contains("score=15"));
    }
}
