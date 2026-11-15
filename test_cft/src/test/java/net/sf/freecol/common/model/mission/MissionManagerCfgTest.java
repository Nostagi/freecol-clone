package net.sf.freecol.common.model.mission;

import java.io.StringReader;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.BlockJUnit4ClassRunner;

import net.sf.freecol.common.io.FreeColXMLReader;
import net.sf.freecol.util.test.MockModelTestCase;


@RunWith(BlockJUnit4ClassRunner.class)
public class MissionManagerCfgTest extends MockModelTestCase {

    @Test
    public void testKnownAndUnknownMissionTags() throws Exception {
        assertTrue(MissionManager.isMissionTag(CompoundMission.TAG));
        assertTrue(MissionManager.isMissionTag(GoToMission.TAG));
        assertTrue(MissionManager.isMissionTag(ImprovementMission.TAG));
        assertFalse(MissionManager.isMissionTag("unknownMission"));
        assertFalse(MissionManager.isMissionTag(null));

        try (FreeColXMLReader xr = new FreeColXMLReader(
                 new StringReader("<unknownMission/>"))) {
            xr.nextTag();
            assertNull(MissionManager.getMission(game, xr));
        }
    }

    @Test
    public void testKnownMissionConstruction() throws Exception {
        try (FreeColXMLReader xr = new FreeColXMLReader(
                 new StringReader("<improvementMission/>"))) {
            xr.nextTag();
            assertTrue(MissionManager.getMission(game, xr)
                instanceof ImprovementMission);
        }
    }
}
