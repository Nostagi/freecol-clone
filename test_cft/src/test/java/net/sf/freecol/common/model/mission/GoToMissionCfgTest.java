package net.sf.freecol.common.model.mission;

import java.io.ByteArrayOutputStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.BlockJUnit4ClassRunner;

import net.sf.freecol.common.io.FreeColXMLReader;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.model.UnitType;
import net.sf.freecol.server.model.ServerUnit;
import net.sf.freecol.util.test.MockModelTestCase;


@RunWith(BlockJUnit4ClassRunner.class)
public class GoToMissionCfgTest extends MockModelTestCase {

    private static final class StationaryUnit extends ServerUnit {
        StationaryUnit(Game game, Player owner, UnitType type) {
            super(game, null, owner, type);
        }

        @Override
        public int getInitialMovesLeft() {
            return 0;
        }
    }

    @Test
    public void testDestinationStateAndMissionResult() {
        Player dutch = game.getPlayerByNationId("model.nation.dutch");
        Unit carrier = new ServerUnit(game, null, dutch,
            spec().getUnitType("model.unit.galleon"));
        Unit unit = new ServerUnit(game, null, dutch,
            spec().getUnitType("model.unit.freeColonist"));
        GoToMission mission = new GoToMission(game);
        mission.setUnit(unit);

        assertFalse(mission.isValid());
        mission.setDestination(carrier);
        assertSame(carrier, mission.getDestination());
        assertTrue(mission.isValid());
        assertEquals(Mission.MissionState.OK, mission.doMission());

        mission.setBlockedCount(3);
        assertEquals(3, mission.getBlockedCount());
        mission.setRepeatCount(0);
        assertFalse(mission.isValid());
        mission.setRepeatCount(1);
        mission.setDestination(null);
        assertFalse(mission.isValid());

        assertTrue(GoToMission.isValidFor(unit));
        Unit stationary = new StationaryUnit(game, dutch,
            spec().getUnitType("model.unit.freeColonist"));
        assertEquals(0, stationary.getInitialMovesLeft());
        assertFalse(GoToMission.isValidFor(stationary));
    }

    @Test
    public void testInvalidDestinationAndXmlAttributes() throws Exception {
        Player dutch = game.getPlayerByNationId("model.nation.dutch");
        Unit unit = new ServerUnit(game, null, dutch,
            spec().getUnitType("model.unit.freeColonist"));
        GoToMission mission = new GoToMission(game);
        mission.setUnit(unit);
        mission.setDestination(unit);
        assertFalse(mission.isValid());

        String xml = "<goToMission unit=\"" + unit.getId()
            + "\" destination=\"" + unit.getId()
            + "\" blockedCount=\"4\" repeatCount=\"2\" turnCount=\"3\"/>";
        try (FreeColXMLReader xr = new FreeColXMLReader(new StringReader(xml))) {
            xr.nextTag();
            mission.readAttributes(xr);
        }
        assertSame(unit, mission.getUnit());
        assertSame(unit, mission.getDestination());
        assertEquals(4, mission.getBlockedCount());
        assertEquals(2, mission.getRepeatCount());
        assertEquals(3, mission.getTurnCount());
        assertFalse(mission.isValid());

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        assertTrue(mission.save(out, null, false));
        String saved = new String(out.toByteArray(), StandardCharsets.UTF_8);
        assertTrue(saved, saved.contains("destination="));
        assertTrue(saved.contains("blockedCount=\"4\""));
    }
}
