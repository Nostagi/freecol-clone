package net.sf.freecol.common.model.mission;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.BlockJUnit4ClassRunner;

import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.server.model.ServerUnit;
import net.sf.freecol.util.test.MockModelTestCase;


@RunWith(BlockJUnit4ClassRunner.class)
public class AbstractMissionCfgTest extends MockModelTestCase {

    private static final class TestMission extends AbstractMission {
        TestMission(Game game) {
            super(game);
        }

        @Override
        public MissionState doMission() {
            return MissionState.OK;
        }

        @Override
        public String getXMLTagName() {
            return "testMission";
        }
    }

    @Test
    public void testStateAndValidityConditions() {
        Player dutch = game.getPlayerByNationId("model.nation.dutch");
        Unit unit = new ServerUnit(game, null, dutch,
            spec().getUnitType("model.unit.freeColonist"));
        TestMission mission = new TestMission(game);

        assertNull(mission.getUnit());
        assertEquals(1, mission.getRepeatCount());
        assertEquals(0, mission.getTurnCount());
        assertFalse(mission.isValid());
        assertFalse(AbstractMission.isValidFor(unit));

        mission.setUnit(unit);
        assertTrue(mission.isValid());
        mission.setRepeatCount(0);
        assertFalse(mission.isValid());
        mission.setRepeatCount(2);
        mission.setTurnCount(4);
        assertEquals(2, mission.getRepeatCount());
        assertEquals(4, mission.getTurnCount());
        assertTrue(mission.isValid());

        unit.dispose();
        assertFalse(mission.isValid());
    }
}
