package net.sf.freecol.common.model.mission;

import java.io.ByteArrayOutputStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.BlockJUnit4ClassRunner;

import net.sf.freecol.common.model.Ability;
import net.sf.freecol.common.io.FreeColXMLReader;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.Map;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.TileImprovement;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.model.UnitType;
import net.sf.freecol.server.model.ServerUnit;
import net.sf.freecol.util.test.MockModelTestCase;


@RunWith(BlockJUnit4ClassRunner.class)
public class ImprovementMissionCfgTest extends MockModelTestCase {

    private Player dutch;
    private Tile tile;
    private Unit colonist;
    private UnitType expertPioneerType;

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        game = getStandardGame();
        Map map = getTestMap(spec().getTileType("model.tile.plains"), true);
        game.changeMap(map);
        tile = map.getTile(5, 5);
        dutch = game.getPlayerByNationId("model.nation.dutch");
        colonist = new ServerUnit(game, tile, dutch,
            spec().getUnitType("model.unit.freeColonist"));
        colonist.setRole(spec().getRole("model.role.pioneer"));

        for (UnitType type : spec().getUnitTypeList()) {
            if (type.hasAbility(Ability.EXPERT_PIONEER)) {
                expertPioneerType = type;
                break;
            }
        }
        assertNotNull("Specification should contain an expert pioneer type",
                      expertPioneerType);
    }

    @Test
    public void testMissionAccessorsAndValidity() {
        ImprovementMission mission = new ImprovementMission(game);
        assertEquals(ImprovementMission.TAG, mission.getXMLTagName());
        assertNull(mission.getImprovement());
        assertFalse(mission.isValid());
        try {
            ImprovementMission.isValidFor(null);
            fail("A null unit has no game specification");
        } catch (NullPointerException expected) {
            assertNotNull(expected);
        }
        mission.setUnit(colonist);
        mission.setUnit(colonist);
        TileImprovement improvement = new TileImprovement(game, tile,
            spec().getTileImprovementType("model.improvement.road"), null);
        assertTrue(ImprovementMission.isValidFor(colonist));
        assertFalse(mission.isValid());

        mission.setImprovement(improvement);
        assertSame(improvement, mission.getImprovement());
        assertTrue(mission.isValid());

        mission.setRepeatCount(0);
        assertFalse(mission.isValid());
        mission.setRepeatCount(1);
        assertTrue(mission.isValid());

        Unit ship = new ServerUnit(game, null, dutch,
            spec().getUnitType("model.unit.galleon"));
        assertTrue(ImprovementMission.isValidFor(ship));
        mission.setUnit(ship);
        assertFalse(mission.isValid());
    }

    @Test
    public void testImprovementValidityAndXmlAttributes() throws Exception {
        ImprovementMission mission = new ImprovementMission(game);
        mission.setUnit(colonist);
        TileImprovement improvement = new TileImprovement(game, tile,
            spec().getTileImprovementType("model.improvement.road"), null);
        mission.setImprovement(improvement);
        assertTrue(mission.isValid());

        Unit ship = new ServerUnit(game, null, dutch,
            spec().getUnitType("model.unit.galleon"));
        mission.setUnit(ship);
        assertFalse(mission.isValid());

        mission.setUnit(colonist);
        String xml = "<improvementMission unit=\"" + colonist.getId()
            + "\" improvement=\"" + improvement.getId()
            + "\" repeatCount=\"2\" turnCount=\"3\"/>";
        try (FreeColXMLReader xr = new FreeColXMLReader(new StringReader(xml))) {
            xr.nextTag();
            mission.readAttributes(xr);
        }
        assertSame(colonist, mission.getUnit());
        assertSame(improvement, mission.getImprovement());
        assertEquals(2, mission.getRepeatCount());
        assertEquals(3, mission.getTurnCount());

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        assertTrue(mission.save(out, null, false));
        String saved = new String(out.toByteArray(), StandardCharsets.UTF_8);
        assertTrue(saved.contains("improvement=\"" + improvement.getId() + "\""));
    }

    @Test
    public void testDoMissionWorkAndCompletion() {
        ImprovementMission regularMission = new ImprovementMission(game);
        regularMission.setUnit(colonist);
        regularMission.setTurnCount(2);
        colonist.setMovesLeft(3);

        assertEquals(Mission.MissionState.OK, regularMission.doMission());
        assertEquals(1, regularMission.getTurnCount());
        assertEquals(0, colonist.getMovesLeft());
        assertEquals(Mission.MissionState.COMPLETED,
                     regularMission.doMission());
        assertEquals(0, regularMission.getTurnCount());

        Unit expertPioneer = new ServerUnit(game, tile, dutch,
                                            expertPioneerType);
        assertTrue(expertPioneer.hasAbility(Ability.EXPERT_PIONEER));
        ImprovementMission expertMission = new ImprovementMission(game);
        expertMission.setUnit(expertPioneer);
        expertMission.setTurnCount(3);
        expertPioneer.setMovesLeft(2);

        assertEquals(Mission.MissionState.OK, expertMission.doMission());
        assertEquals(1, expertMission.getTurnCount());
        assertEquals(0, expertPioneer.getMovesLeft());
        assertEquals(Mission.MissionState.COMPLETED,
                     expertMission.doMission());
        assertEquals(-1, expertMission.getTurnCount());
    }

}
