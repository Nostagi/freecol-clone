package net.sf.freecol.common.model.mission;

import java.util.EnumSet;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.BlockJUnit4ClassRunner;

import net.sf.freecol.util.test.MockModelTestCase;


@RunWith(BlockJUnit4ClassRunner.class)
public class MissionCfgTest extends MockModelTestCase {

    @Test
    public void testMissionStates() {
        EnumSet<Mission.MissionState> states
            = EnumSet.allOf(Mission.MissionState.class);
        assertEquals(4, states.size());
        assertTrue(states.contains(Mission.MissionState.OK));
        assertTrue(states.contains(Mission.MissionState.COMPLETED));
        assertTrue(states.contains(Mission.MissionState.BLOCKED));
        assertTrue(states.contains(Mission.MissionState.ABORTED));
    }
}
