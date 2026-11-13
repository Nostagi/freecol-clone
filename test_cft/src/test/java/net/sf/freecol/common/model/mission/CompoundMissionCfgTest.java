package net.sf.freecol.common.model.mission;

import java.io.ByteArrayOutputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Deque;
import java.util.List;

import javax.xml.stream.XMLStreamException;

import net.sf.freecol.common.io.FreeColXMLReader;
import net.sf.freecol.common.io.FreeColXMLWriter;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.Map;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.TileType;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.model.UnitType;
import net.sf.freecol.common.model.mission.Mission.MissionState;
import net.sf.freecol.server.model.ServerUnit;
import net.sf.freecol.util.test.FreeColTestCase;


/**
 * CFG-driven test suite for {@link CompoundMission} derived from
 * {@code test_cft/graph/net/sf/freecol/common/model/mission/CompoundMission.json}.
 *
 * Covers all 14 methods and achieves 100% C2 (node + edge) coverage across
 * 23 Test Paths (1 Test Path = 1 test case), including loop back-edges not
 * enumerated by simple-path extraction.
 */
public class CompoundMissionCfgTest extends FreeColTestCase {

    private Game game;
    private Unit unit;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        // Pre-condition: standard game, explored plains map, Dutch player, one colonist on (5, 5).
        TileType plains = spec().getTileType("model.tile.plains");
        UnitType colonistType = spec().getUnitType("model.unit.freeColonist");
        game = getStandardGame();
        Map map = getTestMap(plains, true);
        game.changeMap(map);
        Player dutch = game.getPlayerByNationId("model.nation.dutch");
        Tile tile = map.getTile(5, 5);
        unit = new ServerUnit(game, tile, dutch, colonistType);
    }

    // ------------------------------------------------------------------
    // Pre-condition helpers & stubs
    // ------------------------------------------------------------------

    private CompoundMission newMission(Mission... children) {
        CompoundMission mission = new CompoundMission(game);
        mission.setUnit(unit);
        mission.setMissions(new ArrayList<>(Arrays.asList(children)));
        return mission;
    }

    private static final class StubMission implements Mission {
        private final Deque<MissionState> script = new ArrayDeque<>();
        private final boolean valid;
        private MissionState last = MissionState.OK;
        int calls;

        StubMission(boolean valid, MissionState... states) {
            this.valid = valid;
            this.script.addAll(Arrays.asList(states));
        }

        @Override
        public MissionState doMission() {
            calls++;
            if (!script.isEmpty()) last = script.removeFirst();
            return last;
        }

        @Override
        public boolean isValid() { return valid; }

        @Override
        public Unit getUnit() { return unitRef; }
        private Unit unitRef;

        @Override
        public void toXML(FreeColXMLWriter xw) throws XMLStreamException {
            xw.writeStartElement("stubMission");
            xw.writeEndElement();
        }
    }

    // ------------------------------------------------------------------
    // M1: <init>(Game) — 1 Test Path
    // ------------------------------------------------------------------

    // M1-TP1: ENTRY -> N0 -> EXIT
    public void testM1_TP1_ConstructorWithGame() {
        CompoundMission mission = new CompoundMission(game);
        assertNotNull(mission.getId());
        assertTrue(mission.getId().startsWith(CompoundMission.TAG));
        assertEquals(1, mission.getRepeatCount());
        assertEquals(0, mission.getIndex());
        assertNull(mission.getMissions());
    }

    // ------------------------------------------------------------------
    // M2: <init>(Game, FreeColXMLReader) — 1 Test Path
    // ------------------------------------------------------------------

    // M2-TP1: ENTRY -> N0 -> EXIT
    public void testM2_TP1_ConstructorWithReader() throws Exception {
        try (FreeColXMLReader xr = new FreeColXMLReader(new StringReader("<compoundMission/>"))) {
            CompoundMission mission = new CompoundMission(game, xr);
            assertNotNull(mission);
            assertNull(mission.getId());
        }
    }

    // ------------------------------------------------------------------
    // M3: <init>(Game, String) — 1 Test Path
    // ------------------------------------------------------------------

    // M3-TP1: ENTRY -> N0 -> EXIT
    public void testM3_TP1_ConstructorWithId() {
        CompoundMission mission = new CompoundMission(game, "compoundMission:test-1");
        assertEquals("compoundMission:test-1", mission.getId());
    }

    // ------------------------------------------------------------------
    // M4: getMissions() — 1 Test Path
    // ------------------------------------------------------------------

    // M4-TP1: ENTRY -> N0 -> EXIT
    public void testM4_TP1_GetMissions() {
        CompoundMission mission = new CompoundMission(game);
        List<Mission> expected = new ArrayList<>();
        mission.setMissions(expected);
        assertSame(expected, mission.getMissions());
    }

    // ------------------------------------------------------------------
    // M5: setMissions(List) — 1 Test Path
    // ------------------------------------------------------------------

    // M5-TP1: ENTRY -> N0 -> EXIT
    public void testM5_TP1_SetMissions() {
        CompoundMission mission = new CompoundMission(game);
        List<Mission> children = Collections.singletonList(new StubMission(true));
        mission.setMissions(children);
        assertSame(children, mission.getMissions());
    }

    // ------------------------------------------------------------------
    // M6: getIndex() — 1 Test Path
    // ------------------------------------------------------------------

    // M6-TP1: ENTRY -> N0 -> EXIT
    public void testM6_TP1_GetIndex() {
        CompoundMission mission = new CompoundMission(game);
        assertEquals(0, mission.getIndex());
    }

    // ------------------------------------------------------------------
    // M7: setIndex(int) — 1 Test Path
    // ------------------------------------------------------------------

    // M7-TP1: ENTRY -> N0 -> EXIT
    public void testM7_TP1_SetIndex() {
        CompoundMission mission = new CompoundMission(game);
        mission.setIndex(3);
        assertEquals(3, mission.getIndex());
    }

    // ------------------------------------------------------------------
    // M8: isValid() — 3 Test Paths (100% C2)
    // ------------------------------------------------------------------

    // M8-TP1: ENTRY -> N0 -(B1:true)-> N3 -> N4 -> EXIT (super.isValid() == false)
    public void testM8_TP1_IsValidFalseWhenSuperInvalid() {
        CompoundMission mission = new CompoundMission(game); // unit is null -> super.isValid() == false
        mission.setMissions(Collections.singletonList(new StubMission(true)));
        assertFalse(mission.isValid());
    }

    // M8-TP2: ENTRY -> N0 -(B1:false)-> N1 -(B2:true)-> N3 -> N4 -> EXIT (missions.isEmpty() == true)
    public void testM8_TP2_IsValidFalseWhenMissionsEmpty() {
        CompoundMission mission = newMission();
        assertFalse(mission.isValid());
    }

    // M8-TP3: ENTRY -> N0 -(B1:false)-> N1 -(B2:false)-> N2 -> N4 -> EXIT (all(missions, Mission::isValid))
    public void testM8_TP3_IsValidChecksAllChildren() {
        CompoundMission mission = newMission(new StubMission(true), new StubMission(true));
        assertTrue(mission.isValid());
    }

    // ------------------------------------------------------------------
    // M9: doMission() — 5 Test Paths (100% C2, includes loop back-edge)
    // ------------------------------------------------------------------

    // M9-TP1: ENTRY -> N0 -(B1:true)-> N7 -> EXIT (child != COMPLETED)
    public void testM9_TP1_DoMissionReturnsNonCompletedState() {
        StubMission child = new StubMission(true, MissionState.BLOCKED);
        CompoundMission mission = newMission(child);
        assertEquals(MissionState.BLOCKED, mission.doMission());
        assertEquals(0, mission.getIndex());
    }

    // M9-TP2: ENTRY -> N0 -(B1:false)-> N1 -(B2:true)-> N5 -(B4:true)-> N7 -> EXIT
    // (child == COMPLETED, index < size, movesLeft <= 0)
    public void testM9_TP2_DoMissionAdvancesIndexAndStopsWithoutMoves() {
        unit.setMovesLeft(0);
        StubMission second = new StubMission(true, MissionState.OK);
        CompoundMission mission = newMission(
                new StubMission(true, MissionState.COMPLETED), second);
        assertEquals(MissionState.COMPLETED, mission.doMission());
        assertEquals(1, mission.getIndex());
        assertEquals(0, second.calls);
    }

    // M9-TP3: ENTRY -> N0 -(B1:false)-> N1 -(B2:false)-> N2 -(B3:true)-> N4 -> EXIT
    // (child == COMPLETED, index == size, repeatCount <= 0 after decrement)
    public void testM9_TP3_DoMissionCompletesWhenNoRepeatsLeft() {
        CompoundMission mission = newMission(new StubMission(true, MissionState.COMPLETED));
        assertEquals(MissionState.COMPLETED, mission.doMission());
        assertEquals(0, mission.getRepeatCount());
    }

    // M9-TP4: ENTRY -> N0 -(B1:false)-> N1 -(B2:false)-> N2 -(B3:false)-> N3 -> N5 -(B4:true)-> N7 -> EXIT
    // (child == COMPLETED, index == size, repeatCount > 0 after decrement, movesLeft <= 0)
    public void testM9_TP4_DoMissionResetsIndexOnRepeatWithoutMoves() {
        unit.setMovesLeft(0);
        CompoundMission mission = newMission(new StubMission(true, MissionState.COMPLETED));
        mission.setRepeatCount(2);
        assertEquals(MissionState.COMPLETED, mission.doMission());
        assertEquals(1, mission.getRepeatCount());
        assertEquals(0, mission.getIndex());
    }

    // M9-TP5: ENTRY -> N0 -(B1:false)-> N1 -(B2:true)-> N5 -(B4:false)-> N6 -> N0 -(B1:true)-> N7 -> EXIT
    // (loop path covering N5->N6 and N6->N0 when movesLeft > 0)
    public void testM9_TP5_DoMissionLoopsToNextChildWhenMovesLeft() {
        unit.setMovesLeft(3);
        StubMission second = new StubMission(true, MissionState.OK);
        CompoundMission mission = newMission(
                new StubMission(true, MissionState.COMPLETED), second);
        assertEquals(MissionState.OK, mission.doMission());
        assertEquals(1, mission.getIndex());
        assertEquals(1, second.calls);
    }

    // ------------------------------------------------------------------
    // M10: writeAttributes(FreeColXMLWriter) — 1 Test Path
    // ------------------------------------------------------------------

    // M10-TP1: ENTRY -> N0 -> EXIT
    public void testM10_TP1_WriteAttributes() throws Exception {
        CompoundMission mission = newMission();
        mission.setIndex(2);
        StringWriter sw = new StringWriter();
        try (FreeColXMLWriter xw = new FreeColXMLWriter(sw)) {
            xw.writeStartElement(CompoundMission.TAG);
            mission.writeAttributes(xw);
            xw.writeEndElement();
        }
        String xml = sw.toString();
        assertTrue(xml, xml.contains("index=\"2\""));
        assertTrue(xml, xml.contains("unit=\"" + unit.getId() + "\""));
    }

    // ------------------------------------------------------------------
    // M11: writeChildren(FreeColXMLWriter) — 2 Test Paths (100% C2)
    // ------------------------------------------------------------------

    // M11-TP1: ENTRY -> N0 -> N1 -(B1:true)-> N3 -> EXIT (empty missions)
    public void testM11_TP1_WriteChildrenEmpty() throws Exception {
        CompoundMission mission = newMission();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        assertTrue(mission.save(out, null, false));
        String xml = new String(out.toByteArray(), StandardCharsets.UTF_8);
        assertFalse(xml, xml.contains("<stubMission"));
    }

    // M11-TP2: ENTRY -> N0 -> N1 -(B1:false)-> N2 -> N1 -(B1:true)-> N3 -> EXIT (non-empty missions)
    public void testM11_TP2_WriteChildrenNonEmpty() throws Exception {
        CompoundMission mission = newMission(new StubMission(true));
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        assertTrue(mission.save(out, null, false));
        String xml = new String(out.toByteArray(), StandardCharsets.UTF_8);
        assertTrue(xml, xml.contains("<stubMission"));
    }

    // ------------------------------------------------------------------
    // M12: readAttributes(FreeColXMLReader) — 1 Test Path
    // ------------------------------------------------------------------

    // M12-TP1: ENTRY -> N0 -> EXIT
    public void testM12_TP1_ReadAttributes() throws Exception {
        String xml = "<compoundMission id=\"compoundMission:attr-1\" unit=\"" + unit.getId()
                + "\" index=\"4\" repeatCount=\"2\" turnCount=\"5\"/>";
        CompoundMission mission = new CompoundMission(game, "compoundMission:attr-1");
        try (FreeColXMLReader xr = new FreeColXMLReader(new StringReader(xml))) {
            xr.nextTag();
            mission.readAttributes(xr);
        }
        assertEquals(4, mission.getIndex());
        assertEquals(2, mission.getRepeatCount());
        assertEquals(5, mission.getTurnCount());
        assertSame(unit, mission.getUnit());
    }

    // ------------------------------------------------------------------
    // M13: readChildren(FreeColXMLReader) — 3 Test Paths (100% C2)
    // ------------------------------------------------------------------

    // M13-TP1: ENTRY -> N0 -> N1 -(B1:true)-> N5 -> EXIT (no child tags)
    public void testM13_TP1_ReadChildrenEmpty() throws Exception {
        String xml = "<compoundMission/>";
        CompoundMission mission = new CompoundMission(game, "compoundMission:rc-1");
        mission.setMissions(new ArrayList<>(Collections.singletonList(new StubMission(true))));
        try (FreeColXMLReader xr = new FreeColXMLReader(new StringReader(xml))) {
            xr.nextTag();
            mission.readChildren(xr);
        }
        assertTrue(mission.getMissions().isEmpty());
    }

    // M13-TP2: ENTRY -> N0 -> N1 -(B1:false)-> N2 -(B2:true)-> N4 -> N1 -(B1:true)-> N5 -> EXIT
    // (unknown child tag -> MissionManager.getMission returns null)
    public void testM13_TP2_ReadChildrenSkipsUnknownTag() throws Exception {
        String xml = "<compoundMission><unknownMission/></compoundMission>";
        CompoundMission mission = new CompoundMission(game, "compoundMission:rc-2");
        mission.setMissions(new ArrayList<>());
        try (FreeColXMLReader xr = new FreeColXMLReader(new StringReader(xml))) {
            xr.nextTag();
            mission.readChildren(xr);
        }
        assertTrue(mission.getMissions().isEmpty());
    }

    // M13-TP3: ENTRY -> N0 -> N1 -(B1:false)-> N2 -(B2:false)-> N3 -> N4 -> N1 -(B1:true)-> N5 -> EXIT
    // (known child tag <goToMission/> -> MissionManager.getMission returns GoToMission)
    public void testM13_TP3_ReadChildrenAddsValidMission() throws Exception {
        String xml = "<compoundMission><goToMission/></compoundMission>";
        CompoundMission mission = new CompoundMission(game, "compoundMission:rc-3");
        mission.setMissions(new ArrayList<>());
        try (FreeColXMLReader xr = new FreeColXMLReader(new StringReader(xml))) {
            xr.nextTag();
            mission.readChildren(xr);
        }
        assertEquals(1, mission.getMissions().size());
        assertTrue(mission.getMissions().get(0) instanceof GoToMission);
    }

    // ------------------------------------------------------------------
    // M14: getXMLTagName() — 1 Test Path
    // ------------------------------------------------------------------

    // M14-TP1: ENTRY -> N0 -> EXIT
    public void testM14_TP1_GetXMLTagName() {
        assertEquals("compoundMission", new CompoundMission(game).getXMLTagName());
    }
}
