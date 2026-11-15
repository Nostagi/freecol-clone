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
import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.xml.stream.XMLStreamException;

import net.sf.freecol.common.i18n.NameCache;
import net.sf.freecol.common.io.FreeColXMLReader;
import net.sf.freecol.common.io.FreeColXMLWriter;
import net.sf.freecol.common.io.FreeColXMLWriter.WriteScope;
import net.sf.freecol.common.model.Constants.IntegrityType;
import net.sf.freecol.common.model.ModelMessage.MessageType;
import net.sf.freecol.common.model.NationOptions.NationState;
import net.sf.freecol.common.option.OptionGroup;
import net.sf.freecol.common.util.LogBuilder;
import net.sf.freecol.server.model.ServerPlayer;
import net.sf.freecol.server.model.ServerRegion;
import net.sf.freecol.util.test.FreeColTestCase;


/**
 * CFG-driven test suite for {@link Game} derived from
 * {@code test_cft/graph/net/sf/freecol/common/model/Game.json}.
 *
 * Covers all 107 methods (M1..M107, including constructors, private helpers,
 * all 4 lambdas, and {@code <clinit>}) and achieves 100% feasible C3 (node + edge)
 * coverage across 190 Test Paths.
 *
 * Infeasible edges documented:
 * - M46 ({@code addPlayers}): {@code N3 -> N5} ({@code B3:true},
 *   {@code update(p, Player.class, true) == null}) — infeasible because
 *   {@code p.getId()} guarantees {@code p != null}, {@code fcgo == null},
 *   {@code create == true}, and {@code newInstance(this, Player.class, false)}
 *   followed by {@code copyIn} always succeeds for {@code Player.class}.
 * - M91 ({@code checkIntegrity}): {@code N3 -> N4} ({@code B2:false},
 *   {@code fcgo == null}) — infeasible in deterministic single-threaded
 *   execution because {@code getFreeColGameObjectIterator().hasNext()} verifies
 *   {@code readAhead.getValue().get() != null} immediately before {@code next()}.
 * - M94 ({@code copyIn}): {@code N1 -> N2} ({@code B2:false},
 *   {@code !super.copyIn(o)}) — infeasible because {@code B1:false} already
 *   guarantees {@code o != null} and {@code idEquals(o)}, so
 *   {@code FreeColGameObject.copyIn(o)} always returns {@code true}.
 * - Synthetic {@code monitorexit} exception-handler blocks ({@code reachable=false})
 *   in M12, M14, M16, M26, M28..M35, M91, M96 have no incoming CFG edges.
 */
public class GameCfgTest extends FreeColTestCase {

    private Game game;
    private Player dutch;
    private Player french;
    private Player inca;
    private TileType plains;
    private UnitType colonistType;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        // Pre-condition: Initialize standard game, European/Native players, tile & unit types
        game = getStandardGame();
        dutch = game.getPlayerByNationId("model.nation.dutch");
        french = game.getPlayerByNationId("model.nation.french");
        inca = game.getPlayerByNationId("model.nation.inca");
        plains = spec().getTileType("model.tile.plains");
        colonistType = spec().getUnitType("model.unit.freeColonist");
    }

    @Override
    public void tearDown() throws Exception {
        super.tearDown();
    }

    // ------------------------------------------------------------------
    // Pre-condition helpers & stubs
    // ------------------------------------------------------------------

    private static class StubGame extends Game {
        StubGame(Specification spec) {
            super(spec);
        }

        @Override
        public int getNextId() {
            return nextId++;
        }
    }

    public static class StubCopyFailFcgo extends FreeColGameObject {
        public StubCopyFailFcgo(Game game, String id) {
            super(game, id);
        }

        @Override
        public <T extends FreeColObject> boolean copyIn(T other) {
            return false;
        }

        @Override
        public String getXMLTagName() {
            return "stubCopyFail";
        }
    }

    private static class StubCopyFailPlayer extends ServerPlayer {
        StubCopyFailPlayer(Game game, String id) {
            super(game, false, game.getSpecification().getNation("model.nation.english"));
            setId(id);
        }

        @Override
        public <T extends FreeColObject> boolean copyIn(T other) {
            return false;
        }
    }

    private static class StubFcgoListener implements FreeColGameObjectListener {
        String lastSetId;
        FreeColGameObject lastSetObj;
        String lastRemovedId;
        FreeColGameObject lastOwnerSource;
        Player lastOldOwner;
        Player lastNewOwner;

        @Override
        public void setFreeColGameObject(String id, FreeColGameObject fcgo) {
            this.lastSetId = id;
            this.lastSetObj = fcgo;
        }

        @Override
        public void removeFreeColGameObject(String id) {
            this.lastRemovedId = id;
        }

        @Override
        public void ownerChanged(FreeColGameObject source, Player oldOwner, Player newOwner) {
            this.lastOwnerSource = source;
            this.lastOldOwner = oldOwner;
            this.lastNewOwner = newOwner;
        }
    }

    private static Object invokePrivate(Object target, String name,
                                        Class<?>[] paramTypes, Object... args)
            throws Exception {
        Method m = Game.class.getDeclaredMethod(name, paramTypes);
        m.setAccessible(true);
        return m.invoke(target, args);
    }

    private static void setField(Object target, String name, Object value)
            throws Exception {
        Field f = Game.class.getDeclaredField(name);
        f.setAccessible(true);
        f.set(target, value);
    }

    private Player createTestPlayer(Game g, String nationId, boolean dead, boolean unknownEnemy) {
        String id = unknownEnemy ? Nation.UNKNOWN_NATION_ID : nationId;
        Nation nation = g.getSpecification().getNation(id);
        ServerPlayer p = new ServerPlayer(g, false, nation);
        p.setDead(dead);
        return p;
    }

    // ------------------------------------------------------------------
    // M1: <init>()V — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M1-TP1: ENTRY -> N0 -> EXIT (default constructor initializes id="0", empty collections)
    public void testM1_TP1_DefaultConstructor() {
        Game g = new Game();
        assertEquals("0", g.getId());
        assertNull(g.getSpecification());
        assertNull(g.getMap());
        assertTrue(g.isInServer());
        assertTrue(g.isInitialized());
    }

    // ------------------------------------------------------------------
    // M2: <init>(Specification)V — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M2-TP1: ENTRY -> N0 -> EXIT (protected constructor sets specification)
    public void testM2_TP1_SpecificationConstructor() {
        StubGame g = new StubGame(spec());
        assertEquals("0", g.getId());
        assertSame(spec(), g.getSpecification());
    }

    // ------------------------------------------------------------------
    // M3: <init>(Game, FreeColXMLReader)V — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M3-TP1: ENTRY -> N0 -> EXIT (XML stream constructor reads game from reader)
    public void testM3_TP1_XmlReaderConstructor() throws Exception {
        String xml = "<game id=\"0\" turn=\"1\" spanishSuccession=\"false\"><areas/></game>";
        try (FreeColXMLReader xr = new FreeColXMLReader(new StringReader(xml))) {
            xr.nextTag();
            Game g = new Game(null, xr);
            assertEquals("0", g.getId());
            assertEquals(1, g.getTurn().getNumber());
        }
    }

    // ------------------------------------------------------------------
    // M4: preGameUpdate(Game)Z — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M4-TP1: ENTRY -> N0 -> EXIT (preGameUpdate delegates to copyIn)
    public void testM4_TP1_PreGameUpdate() {
        StubGame src = new StubGame(spec());
        src.setNationOptions(new NationOptions(spec()));
        StubGame target = new StubGame(spec());
        assertTrue(target.preGameUpdate(src));
        assertSame(spec(), target.getSpecification());
    }

    // ------------------------------------------------------------------
    // M5: newInstance(Game, Class, boolean)FreeColObject — 3 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M5-TP1: ENTRY -> N0 -(B1:true)-> N3 -> N4 -> EXIT (server == false)
    public void testM5_TP1_NewInstanceClient() {
        Region r = Game.newInstance(game, Region.class, false);
        assertNotNull(r);
        assertFalse(r instanceof ServerRegion);
    }

    // M5-TP2: ENTRY -> N0 -(B1:false)-> N1 -(B2:true)-> N3 -> N4 -> EXIT (server == true, class not in serverClasses)
    public void testM5_TP2_NewInstanceServerUnmapped() {
        Goods g = Game.newInstance(game, Goods.class, true);
        assertNotNull(g);
    }

    // M5-TP3: ENTRY -> N0 -(B1:false)-> N1 -(B2:false)-> N2 -> N3 -> N4 -> EXIT (server == true, class in serverClasses)
    public void testM5_TP3_NewInstanceServerMapped() {
        Region r = Game.newInstance(game, Region.class, true);
        assertNotNull(r);
        assertTrue(r instanceof ServerRegion);
    }

    // ------------------------------------------------------------------
    // M6: getDifficultyOptionGroup()OptionGroup — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M6-TP1: ENTRY -> N0 -> EXIT (returns difficulty option group from specification)
    public void testM6_TP1_GetDifficultyOptionGroup() {
        OptionGroup og = game.getDifficultyOptionGroup();
        assertNotNull(og);
    }

    // ------------------------------------------------------------------
    // M7: getGameOptions()OptionGroup — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M7-TP1: ENTRY -> N0 -> EXIT (returns game options from specification)
    public void testM7_TP1_GetGameOptions() {
        assertNotNull(game.getGameOptions());
    }

    // ------------------------------------------------------------------
    // M8: setGameOptions(OptionGroup)V — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M8-TP1: ENTRY -> N0 -> EXIT (sets game options on specification)
    public void testM8_TP1_SetGameOptions() {
        OptionGroup og = game.getGameOptions();
        game.setGameOptions(og);
        assertSame(og, game.getGameOptions());
    }

    // ------------------------------------------------------------------
    // M9: getMapGeneratorOptions()OptionGroup — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M9-TP1: ENTRY -> N0 -> EXIT (returns map generator options from specification)
    public void testM9_TP1_GetMapGeneratorOptions() {
        assertNotNull(game.getMapGeneratorOptions());
    }

    // ------------------------------------------------------------------
    // M10: setMapGeneratorOptions(OptionGroup)V — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M10-TP1: ENTRY -> N0 -> EXIT (sets map generator options on specification)
    public void testM10_TP1_SetMapGeneratorOptions() {
        OptionGroup mgo = game.getMapGeneratorOptions();
        game.setMapGeneratorOptions(mgo);
        assertSame(mgo, game.getMapGeneratorOptions());
    }

    // ------------------------------------------------------------------
    // M11: getNextId()I — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M11-TP1: ENTRY -> N0 -> EXIT (throws RuntimeException in base Game)
    public void testM11_TP1_GetNextIdThrows() {
        Game g = new Game();
        try {
            g.getNextId();
            fail("Expected RuntimeException");
        } catch (RuntimeException expected) {
            assertTrue(expected.getMessage().contains("getNextId"));
        }
    }

    // ------------------------------------------------------------------
    // M12: getFreeColGameObject(String)FreeColGameObject — 5 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M12-TP1: ENTRY -> N0 -(B1:true)-> N2 -> EXIT (id == null)
    public void testM12_TP1_GetFcgoNullId() {
        assertNull(game.getFreeColGameObject(null));
    }

    // M12-TP2: ENTRY -> N0 -(B1:false)-> N1 -(B2:false)-> N2 -> EXIT (id.isEmpty() == true)
    public void testM12_TP2_GetFcgoEmptyId() {
        assertNull(game.getFreeColGameObject(""));
    }

    // M12-TP3: ENTRY -> N0 -(B1:false)-> N1 -(B2:true)-> N3 -> N4 -> N5 -> N8 -(B3:false)-> N9 -> EXIT (ro == null)
    public void testM12_TP3_GetFcgoNotFound() {
        assertNull(game.getFreeColGameObject("nonexistent:9999"));
    }

    // M12-TP4: ENTRY -> N0 -(B1:false)-> N1 -(B2:true)-> N3 -> N4 -> N5 -> N8 -(B3:true)-> N10 -(B4:false)-> N11 -> EXIT (ro != null, ro.get() == null)
    public void testM12_TP4_GetFcgoClearedWeakRef() {
        game.freeColGameObjects.put("cleared:1", new WeakReference<>(null));
        assertNull(game.getFreeColGameObject("cleared:1"));
        assertFalse(game.freeColGameObjects.containsKey("cleared:1"));
    }

    // M12-TP5: ENTRY -> N0 -(B1:false)-> N1 -(B2:true)-> N3 -> N4 -> N5 -> N8 -(B3:true)-> N10 -(B4:true)-> N12 -> EXIT (o != null)
    public void testM12_TP5_GetFcgoFound() {
        assertSame(game, game.getFreeColGameObject("0"));
    }

    // ------------------------------------------------------------------
    // M13: getFreeColGameObject(String, Class)FreeColGameObject — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M13-TP1: ENTRY -> N0 -> N1 -> N2 -> EXIT (typed lookup returns cast object or null on mismatch)
    public void testM13_TP1_GetFcgoTyped() {
        assertSame(dutch, game.getFreeColGameObject(dutch.getId(), Player.class));
        assertNull(game.getFreeColGameObject("0", Player.class));
    }

    // ------------------------------------------------------------------
    // M14: setFreeColGameObject(String, FreeColGameObject)V — 4 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M14-TP1: ENTRY -> N0 -(B1:true)-> N2 -> EXIT (id == null)
    public void testM14_TP1_SetFcgoNullId() {
        try {
            game.setFreeColGameObject(null, dutch);
            fail("Expected RuntimeException");
        } catch (RuntimeException expected) {
            assertTrue(expected.getMessage().contains("identifier"));
        }
    }

    // M14-TP2: ENTRY -> N0 -(B1:false)-> N1 -(B2:false)-> N2 -> EXIT (id is empty)
    public void testM14_TP2_SetFcgoEmptyId() {
        try {
            game.setFreeColGameObject("", dutch);
            fail("Expected RuntimeException");
        } catch (RuntimeException expected) {
            assertTrue(expected.getMessage().contains("identifier"));
        }
    }

    // M14-TP3: ENTRY -> N0 -(B1:false)-> N1 -(B2:true)-> N3 -(B3:false)-> N4 -> EXIT (fcgo == null)
    public void testM14_TP3_SetFcgoNullObject() {
        try {
            game.setFreeColGameObject("test:1", null);
            fail("Expected RuntimeException");
        } catch (RuntimeException expected) {
            assertTrue(expected.getMessage().contains("Null FreeColGameObject"));
        }
    }

    // M14-TP4: ENTRY -> N0 -(B1:false)-> N1 -(B2:true)-> N3 -(B3:true)-> N5 -> N6 -> N7 -> N10 -> EXIT (valid)
    public void testM14_TP4_SetFcgoValid() {
        StubCopyFailFcgo obj = new StubCopyFailFcgo(game, "stub:1");
        game.setFreeColGameObject("stub:1", obj);
        assertSame(obj, game.getFreeColGameObject("stub:1"));
    }

    // ------------------------------------------------------------------
    // M15: addFreeColGameObject(String, FreeColGameObject)V — 5 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M15-TP1: ENTRY -> N0 -(B1:true)-> N2 -> EXIT (id == null)
    public void testM15_TP1_AddFcgoNullId() {
        try {
            game.addFreeColGameObject(null, dutch);
            fail("Expected RuntimeException");
        } catch (RuntimeException expected) {
            assertNotNull(expected);
        }
    }

    // M15-TP2: ENTRY -> N0 -(B1:false)-> N1 -(B2:false)-> N2 -> EXIT (id is empty)
    public void testM15_TP2_AddFcgoEmptyId() {
        try {
            game.addFreeColGameObject("", dutch);
            fail("Expected RuntimeException");
        } catch (RuntimeException expected) {
            assertNotNull(expected);
        }
    }

    // M15-TP3: ENTRY -> N0 -(B1:false)-> N1 -(B2:true)-> N3 -(B3:false)-> N4 -> EXIT (fcgo == null)
    public void testM15_TP3_AddFcgoNullObject() {
        try {
            game.addFreeColGameObject("test:2", null);
            fail("Expected RuntimeException");
        } catch (RuntimeException expected) {
            assertNotNull(expected);
        }
    }

    // M15-TP4: ENTRY -> N0 -(B1:false)-> N1 -(B2:true)-> N3 -(B3:true)-> N5 -(B4:false)-> N6 -> EXIT (old != null)
    public void testM15_TP4_AddFcgoDuplicateIdThrows() {
        try {
            game.addFreeColGameObject(dutch.getId(), french);
            fail("Expected RuntimeException");
        } catch (RuntimeException expected) {
            assertTrue(expected.getMessage().contains("replace FCGO"));
        }
    }

    // M15-TP5: ENTRY -> N0 -(B1:false)-> N1 -(B2:true)-> N3 -(B3:true)-> N5 -(B4:true)-> N7 -> EXIT (old == null)
    public void testM15_TP5_AddFcgoNewId() {
        StubCopyFailFcgo obj = new StubCopyFailFcgo(game, "newStub:1");
        game.removeFreeColGameObject("newStub:1", "prep");
        game.addFreeColGameObject("newStub:1", obj);
        assertSame(obj, game.getFreeColGameObject("newStub:1"));
    }

    // ------------------------------------------------------------------
    // M16: removeFreeColGameObject(String, String)V — 4 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M16-TP1: ENTRY -> N0 -(B1:true)-> N2 -> EXIT (id == null)
    public void testM16_TP1_RemoveFcgoNullId() {
        try {
            game.removeFreeColGameObject(null, "test");
            fail("Expected RuntimeException");
        } catch (RuntimeException expected) {
            assertNotNull(expected);
        }
    }

    // M16-TP2: ENTRY -> N0 -(B1:false)-> N1 -(B2:false)-> N2 -> EXIT (id is empty)
    public void testM16_TP2_RemoveFcgoEmptyId() {
        try {
            game.removeFreeColGameObject("", "test");
            fail("Expected RuntimeException");
        } catch (RuntimeException expected) {
            assertNotNull(expected);
        }
    }

    // M16-TP3: ENTRY -> N0 -(B1:false)-> N1 -(B2:true)-> N3 -> N4 -> N5 -> N8 -(B3:true)-> N18 -> EXIT (removeCount <= 64)
    public void testM16_TP3_RemoveFcgoBelowThreshold() {
        game.setFreeColGameObject("rem:1", new StubCopyFailFcgo(game, "rem:1"));
        game.removeFreeColGameObject("rem:1", "unit-test");
        assertNull(game.getFreeColGameObject("rem:1"));
    }

    // M16-TP4: ENTRY -> N0 -(B1:false)-> N1 -(B2:true)-> N3 -> N4 -> N5 -> N8 -(B3:false)-> N9 -> N10 -> N11 -(B4:false)-> N12 -> N11 -(B4:true)-> N13 -> N14 -> N17 -> N18 -> EXIT (removeCount > 64 triggers GC clean loop)
    public void testM16_TP4_RemoveFcgoExceedsThreshold() throws Exception {
        setField(game, "removeCount", Integer.valueOf(64));
        game.removeFreeColGameObject("rem:threshold", "gc-trigger");
        Field rc = Game.class.getDeclaredField("removeCount");
        rc.setAccessible(true);
        assertEquals(0, rc.getInt(game));
    }

    // ------------------------------------------------------------------
    // M17: update(FreeColGameObject, boolean)FreeColGameObject — 2 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M17-TP1: ENTRY -> N0 -(B1:true)-> N2 -> N3 -> EXIT (other != null)
    public void testM17_TP1_UpdateNonNull() {
        assertSame(dutch, game.update(dutch, false));
    }

    // M17-TP2: ENTRY -> N0 -(B1:false)-> N1 -> N3 -> EXIT (other == null)
    public void testM17_TP2_UpdateNull() {
        assertNull(game.update((FreeColGameObject) null, false));
    }

    // ------------------------------------------------------------------
    // M18: update(FreeColGameObject, Class, boolean)FreeColGameObject — 5 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M18-TP1: ENTRY -> N0 -(B1:false)-> N1 -> EXIT (other == null)
    public void testM18_TP1_UpdateTypedNull() throws Exception {
        assertNull(invokePrivate(game, "update",
                new Class<?>[]{FreeColGameObject.class, Class.class, boolean.class},
                null, Player.class, true));
    }

    // M18-TP2: ENTRY -> N0 -(B1:true)-> N2 -(B2:false)-> N3 -(B3:true)-> N5 -> EXIT (fcgo == null, create == false)
    public void testM18_TP2_UpdateTypedMissingNoCreate() throws Exception {
        Region r = new Region(game);
        r.setId("missingRegion:999");
        assertNull(invokePrivate(game, "update",
                new Class<?>[]{FreeColGameObject.class, Class.class, boolean.class},
                r, Region.class, false));
    }

    // M18-TP3: ENTRY -> N0 -(B1:true)-> N2 -(B2:false)-> N3 -(B3:false)-> N4 -> N6 -> N7 -> N9 -(B4:true)-> N11 -(B5:false)-> N12 -> N13 -> EXIT (fcgo == null, create == true)
    public void testM18_TP3_UpdateTypedMissingCreate() throws Exception {
        Region r = new Region(game);
        String newId = "region:created1";
        r.setId(newId);
        game.removeFreeColGameObject(r.getId(), "prep");
        Object res = invokePrivate(game, "update",
                new Class<?>[]{FreeColGameObject.class, Class.class, boolean.class},
                r, Region.class, true);
        assertNotNull(res);
        assertSame(res, game.getFreeColGameObject(newId));
    }

    // M18-TP4: ENTRY -> N0 -(B1:true)-> N2 -(B2:true)-> N6 -> N7 -> N9 -(B4:true)-> N11 -(B5:true)-> N13 -> EXIT (fcgo != null, create == false, copyIn == true)
    public void testM18_TP4_UpdateTypedExistingNoCreate() throws Exception {
        Object res = invokePrivate(game, "update",
                new Class<?>[]{FreeColGameObject.class, Class.class, boolean.class},
                dutch, Player.class, false);
        assertSame(dutch, res);
    }

    // M18-TP5: ENTRY -> N0 -(B1:true)-> N2 -(B2:true)-> N6 -> N7 -> N9 -(B4:false)-> N10 -> EXIT (copyIn == false throws)
    public void testM18_TP5_UpdateTypedCopyFailThrows() throws Exception {
        StubCopyFailFcgo failObj = new StubCopyFailFcgo(game, "failCopy:1");
        try {
            invokePrivate(game, "update",
                    new Class<?>[]{FreeColGameObject.class, Class.class, boolean.class},
                    failObj, StubCopyFailFcgo.class, false);
            fail("Expected InvocationTargetException wrapping RuntimeException");
        } catch (Exception expected) {
            assertTrue(expected.getCause() instanceof RuntimeException);
        }
    }

    // ------------------------------------------------------------------
    // M19: update(Collection, boolean)List — 3 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M19-TP1: ENTRY -> N0 -(B1:false)-> N1 -> EXIT (other == null)
    public void testM19_TP1_UpdateCollectionNull() {
        assertNull(game.update((java.util.Collection<Player>) null, false));
    }

    // M19-TP2: ENTRY -> N0 -(B1:true)-> N2 -> N3 -(B2:true)-> N7 -> EXIT (empty collection)
    public void testM19_TP2_UpdateCollectionEmpty() {
        List<Player> res = game.update(Collections.<Player>emptyList(), false);
        assertTrue(res.isEmpty());
    }

    // M19-TP3: ENTRY -> N0 -(B1:true)-> N2 -> N3 -(B2:false)-> N4 -(B3:true/false)-> N5/N6 -> N3 -(B2:true)-> N7 -> EXIT (mix of null result and non-null result)
    public void testM19_TP3_UpdateCollectionMixed() {
        Region missing = new Region(new StubGame(spec()));
        missing.setId("missingReg:1");
        Region present = new Region(game);
        List<Region> res = game.update(Arrays.asList(missing, present), false);
        assertEquals(1, res.size());
        assertSame(present, res.get(0));
    }

    // ------------------------------------------------------------------
    // M20: updateRef(FreeColGameObject)FreeColGameObject — 2 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M20-TP1: ENTRY -> N0 -(B1:true)-> N2 -> N3 -> EXIT (other != null)
    public void testM20_TP1_UpdateRefNonNull() {
        assertSame(dutch, game.updateRef(dutch));
    }

    // M20-TP2: ENTRY -> N0 -(B1:false)-> N1 -> N3 -> EXIT (other == null)
    public void testM20_TP2_UpdateRefNull() {
        assertNull(game.updateRef((FreeColGameObject) null));
    }

    // ------------------------------------------------------------------
    // M21: updateRef(FreeColGameObject, Class)FreeColGameObject — 2 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M21-TP1: ENTRY -> N0 -(B1:true)-> N2 -> EXIT (other != null)
    public void testM21_TP1_UpdateRefTypedNonNull() throws Exception {
        assertSame(dutch, invokePrivate(game, "updateRef",
                new Class<?>[]{FreeColGameObject.class, Class.class},
                dutch, Player.class));
    }

    // M21-TP2: ENTRY -> N0 -(B1:false)-> N1 -> EXIT (other == null)
    public void testM21_TP2_UpdateRefTypedNull() throws Exception {
        assertNull(invokePrivate(game, "updateRef",
                new Class<?>[]{FreeColGameObject.class, Class.class},
                null, Player.class));
    }

    // ------------------------------------------------------------------
    // M22: updateRef(Collection)List — 3 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M22-TP1: ENTRY -> N0 -(B1:false)-> N1 -> EXIT (other == null)
    public void testM22_TP1_UpdateRefCollectionNull() {
        assertNull(game.updateRef((java.util.Collection<Player>) null));
    }

    // M22-TP2: ENTRY -> N0 -(B1:true)-> N2 -> N3 -(B2:true)-> N7 -> EXIT (empty collection)
    public void testM22_TP2_UpdateRefCollectionEmpty() {
        assertTrue(game.updateRef(Collections.<Player>emptyList()).isEmpty());
    }

    // M22-TP3: ENTRY -> N0 -(B1:true)-> N2 -> N3 -(B2:false)-> N4 -(B3:true/false)-> N5/N6 -> N3 -(B2:true)-> N7 -> EXIT (missing + present)
    public void testM22_TP3_UpdateRefCollectionMixed() {
        Region missing = new Region(new StubGame(spec()));
        missing.setId("missingReg:2");
        Region present = new Region(game);
        List<Region> res = game.updateRef(Arrays.asList(missing, present));
        assertEquals(1, res.size());
        assertSame(present, res.get(0));
    }

    // ------------------------------------------------------------------
    // M23: updateLocationRef(Location)Location — 2 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M23-TP1: ENTRY -> N0 -(B1:true)-> N2 -> N3 -> EXIT (loc != null)
    public void testM23_TP1_UpdateLocationRefNonNull() {
        assertSame(dutch.getEurope(), game.updateLocationRef(dutch.getEurope()));
    }

    // M23-TP2: ENTRY -> N0 -(B1:false)-> N1 -> N3 -> EXIT (loc == null)
    public void testM23_TP2_UpdateLocationRefNull() {
        assertNull(game.updateLocationRef(null));
    }

    // ------------------------------------------------------------------
    // M24: findFreeColLocation(String)Location — 2 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M24-TP1: ENTRY -> N0 -(B1:true)-> N2 -> N3 -> EXIT (fcgo is NOT Location)
    public void testM24_TP1_FindFreeColLocationNonLocation() {
        assertNull(game.findFreeColLocation("0"));
    }

    // M24-TP2: ENTRY -> N0 -(B1:false)-> N1 -> N3 -> EXIT (fcgo IS Location)
    public void testM24_TP2_FindFreeColLocationValid() {
        assertSame(dutch.getEurope(), game.findFreeColLocation(dutch.getEurope().getId()));
    }

    // ------------------------------------------------------------------
    // M25: getFreeColGameObjectIterator()Iterator — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M25-TP1: ENTRY -> N0 -> EXIT (returns iterator over registered FCGOs)
    @SuppressWarnings("unchecked")
    public void testM25_TP1_GetFreeColGameObjectIterator() throws Exception {
        java.util.Iterator<FreeColGameObject> it = (java.util.Iterator<FreeColGameObject>)
                invokePrivate(game, "getFreeColGameObjectIterator", new Class<?>[]{});
        assertNotNull(it);
        assertTrue(it.hasNext());
        assertNotNull(it.next());
    }

    // ------------------------------------------------------------------
    // M26: getFreeColGameObjectList()List — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M26-TP1: ENTRY -> N0 -> N1 -> N2 -(B1:false)-> N3 -> N2 -(B1:true)-> N4 -> N5 -> N8 -> EXIT
    public void testM26_TP1_GetFreeColGameObjectList() {
        List<FreeColGameObject> list = game.getFreeColGameObjectList();
        assertTrue(list.contains(game));
        assertTrue(list.contains(dutch));
    }

    // ------------------------------------------------------------------
    // M27: getUUID()UUID — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M27-TP1: ENTRY -> N0 -> EXIT (returns non-null UUID)
    public void testM27_TP1_GetUUID() {
        assertNotNull(game.getUUID());
    }

    // ------------------------------------------------------------------
    // M28: getPlayerList(Predicate)List — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M28-TP1: ENTRY -> N0 -> N1 -> N2 -> EXIT (returns filtered player list)
    public void testM28_TP1_GetPlayerList() {
        List<Player> list = game.getPlayerList(Player::isEuropean);
        assertTrue(list.contains(dutch));
        assertFalse(list.contains(inca));
    }

    // ------------------------------------------------------------------
    // M29: getPlayers(Predicate)Stream — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M29-TP1: ENTRY -> N0 -> N1 -> N2 -> EXIT (returns and consumes player stream)
    public void testM29_TP1_GetPlayersStream() {
        List<Player> list = game.getPlayers(Player::isIndian).collect(Collectors.toList());
        assertTrue(list.contains(inca));
    }

    // ------------------------------------------------------------------
    // M30: getPlayer(Predicate)Player — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M30-TP1: ENTRY -> N0 -> N1 -> N2 -> EXIT (finds first matching player)
    public void testM30_TP1_GetPlayer() {
        assertSame(dutch, game.getPlayer(p -> p == dutch));
    }

    // ------------------------------------------------------------------
    // M31: getPlayerAfter(Player)Player — 4 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M31-TP1: ENTRY -> N0 -> N1 -(B1:false)-> N2 -> N3 -> EXIT (players is empty)
    public void testM31_TP1_GetPlayerAfterEmpty() throws Exception {
        Game g = new StubGame(spec());
        assertNull(invokePrivate(g, "getPlayerAfter", new Class<?>[]{Player.class}, dutch));
    }

    // M31-TP2: ENTRY -> N0 -> N1 -(B1:true)-> N4 -> N5 -(B2:true)-> N7 -(B3:false)-> N8 -(B4:false)-> N9 -> N10 -> EXIT (next live player without wrap)
    public void testM31_TP2_GetPlayerAfterNoWrap() throws Exception {
        Player next = (Player) invokePrivate(game, "getPlayerAfter", new Class<?>[]{Player.class}, dutch);
        assertNotNull(next);
        assertNotSame(dutch, next);
    }

    // M31-TP3: ENTRY -> N0 -> N1 -(B1:true)-> N4 -> N5 -(B2:false)-> N6 -> N7 -(B3:true)-> N11 -(B5:true)-> N5 -(B2:true)-> N7 -(B3:false)-> N8 -(B4:false)-> N9 -> N10 -> EXIT (wraps around & skips unknownEnemy)
    public void testM31_TP3_GetPlayerAfterWrapSkipsUnknownEnemy() throws Exception {
        Game g = new StubGame(spec());
        g.setNationOptions(new NationOptions(spec()));
        Player unknown = createTestPlayer(g, "model.nation.unknownEnemy", false, true);
        Player p1 = createTestPlayer(g, "model.nation.dutch", false, false);
        g.players.add(unknown);
        g.players.add(p1);
        Player next = (Player) invokePrivate(g, "getPlayerAfter", new Class<?>[]{Player.class}, p1);
        assertSame(p1, next);
    }

    // M31-TP4: ENTRY -> N0 -> N1 -(B1:true)-> N4 -> N5 -(B2:false)-> N6 -> N7 -(B3:false)-> N8 -(B4:true)-> N11 -(B5:false)-> N12 -> N13 -> N16 -> EXIT (all players dead -> returns null)
    public void testM31_TP4_GetPlayerAfterAllDead() throws Exception {
        Game g = new StubGame(spec());
        Player dead = createTestPlayer(g, "model.nation.dutch", true, false);
        g.players.add(dead);
        assertNull(invokePrivate(g, "getPlayerAfter", new Class<?>[]{Player.class}, dead));
    }

    // ------------------------------------------------------------------
    // M32: addPlayer(Player)V — 2 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M32-TP1: ENTRY -> N0 -> N1 -(B1:true)-> N3 -> N4 -> N7 -> EXIT (player already in list)
    public void testM32_TP1_AddPlayerDuplicate() {
        int before = game.players.size();
        game.addPlayer(dutch);
        assertEquals(before, game.players.size());
    }

    // M32-TP2: ENTRY -> N0 -> N1 -(B1:false)-> N2 -> N3 -> N4 -> N7 -> EXIT (new player added)
    public void testM32_TP2_AddPlayerNew() {
        Game g = new StubGame(spec());
        g.setNationOptions(new NationOptions(spec()));
        Player p = createTestPlayer(g, "model.nation.dutch", false, false);
        g.addPlayer(p);
        assertTrue(g.players.contains(p));
        assertEquals(NationState.NOT_AVAILABLE,
                g.getNationOptions().getNations().get(spec().getNation("model.nation.dutch")));
    }

    // ------------------------------------------------------------------
    // M33: removePlayer(Player)Z — 3 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M33-TP1: ENTRY -> N0 -(B1:false)-> N1 -> N3 -> N4 -(B2:false)-> N5 -> N6 -> EXIT (not currentPlayer, not in players)
    public void testM33_TP1_RemovePlayerNotPresent() {
        Player outside = createTestPlayer(game, "model.nation.english", false, false);
        assertFalse(game.removePlayer(outside));
    }

    // M33-TP2: ENTRY -> N0 -(B1:false)-> N1 -> N3 -> N4 -(B2:true)-> N7 -> N8 -> N11 -(B3:true)-> N13 -> EXIT (not currentPlayer, removed)
    public void testM33_TP2_RemovePlayerNotCurrent() {
        game.setCurrentPlayer(dutch);
        assertTrue(game.removePlayer(french));
        assertSame(dutch, game.getCurrentPlayer());
    }

    // M33-TP3: ENTRY -> N0 -(B1:true)-> N2 -> N3 -> N4 -(B2:true)-> N7 -> N8 -> N11 -(B3:false)-> N12 -> N13 -> EXIT (is currentPlayer, updates currentPlayer)
    public void testM33_TP3_RemovePlayerIsCurrent() {
        game.setCurrentPlayer(dutch);
        assertTrue(game.removePlayer(dutch));
        assertNotNull(game.getCurrentPlayer());
        assertNotSame(dutch, game.getCurrentPlayer());
    }

    // ------------------------------------------------------------------
    // M34: getFirstPlayer()Player — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M34-TP1: ENTRY -> N0 -> N1 -> N2 -> EXIT (returns first player)
    public void testM34_TP1_GetFirstPlayer() {
        assertNotNull(game.getFirstPlayer());
    }

    // ------------------------------------------------------------------
    // M35: sortPlayers(Comparator)V — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M35-TP1: ENTRY -> N0 -> N1 -> N2 -> N5 -> EXIT (sorts players list)
    public void testM35_TP1_SortPlayers() {
        game.sortPlayers(Comparator.comparing(Player::getNationId));
        assertTrue(game.getFirstPlayer().getNationId()
                .compareTo(game.players.get(game.players.size() - 1).getNationId()) <= 0);
    }

    // ------------------------------------------------------------------
    // M36: getPlayerByName(String)Player — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M36-TP1: ENTRY -> N0 -> EXIT (finds player by name)
    public void testM36_TP1_GetPlayerByName() {
        assertSame(dutch, game.getPlayerByName(dutch.getName()));
    }

    // ------------------------------------------------------------------
    // M37: getPlayerByNation(Nation)Player — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M37-TP1: ENTRY -> N0 -> EXIT (finds player by Nation)
    public void testM37_TP1_GetPlayerByNation() {
        assertSame(dutch, game.getPlayerByNation(dutch.getNation()));
    }

    // ------------------------------------------------------------------
    // M38: getPlayerByNationId(String)Player — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M38-TP1: ENTRY -> N0 -> EXIT (finds player by nationId)
    public void testM38_TP1_GetPlayerByNationId() {
        assertSame(french, game.getPlayerByNationId("model.nation.french"));
    }

    // ------------------------------------------------------------------
    // M39: getLivePlayerList(Player...)List — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M39-TP1: ENTRY -> N0 -> EXIT (returns live players excluding specified)
    public void testM39_TP1_GetLivePlayerList() {
        List<Player> live = game.getLivePlayerList(dutch);
        assertFalse(live.contains(dutch));
        assertTrue(live.contains(french));
    }

    // ------------------------------------------------------------------
    // M40: getLivePlayers(Player...)Stream — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M40-TP1: ENTRY -> N0 -> EXIT (returns and consumes live players stream)
    public void testM40_TP1_GetLivePlayersStream() {
        List<Player> live = game.getLivePlayers(french).collect(Collectors.toList());
        assertFalse(live.contains(french));
        assertTrue(live.contains(dutch));
    }

    // ------------------------------------------------------------------
    // M41: getLiveEuropeanPlayerList(Player...)List — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M41-TP1: ENTRY -> N0 -> EXIT (returns live European players)
    public void testM41_TP1_GetLiveEuropeanPlayerList() {
        List<Player> list = game.getLiveEuropeanPlayerList();
        assertTrue(list.contains(dutch));
        assertFalse(list.contains(inca));
    }

    // ------------------------------------------------------------------
    // M42: getLiveEuropeanPlayers(Player...)Stream — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M42-TP1: ENTRY -> N0 -> EXIT (returns and consumes live European players stream)
    public void testM42_TP1_GetLiveEuropeanPlayersStream() {
        List<Player> list = game.getLiveEuropeanPlayers(dutch).collect(Collectors.toList());
        assertFalse(list.contains(dutch));
        assertTrue(list.contains(french));
    }

    // ------------------------------------------------------------------
    // M43: getLiveNativePlayerList(Player...)List — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M43-TP1: ENTRY -> N0 -> EXIT (returns live native players)
    public void testM43_TP1_GetLiveNativePlayerList() {
        List<Player> list = game.getLiveNativePlayerList();
        assertTrue(list.contains(inca));
        assertFalse(list.contains(dutch));
    }

    // ------------------------------------------------------------------
    // M44: getLiveNativePlayers(Player...)Stream — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M44-TP1: ENTRY -> N0 -> EXIT (returns and consumes live native players stream)
    public void testM44_TP1_GetLiveNativePlayersStream() {
        List<Player> list = game.getLiveNativePlayers(inca).collect(Collectors.toList());
        assertFalse(list.contains(inca));
    }

    // ------------------------------------------------------------------
    // M45: getNextPlayer()Player — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M45-TP1: ENTRY -> N0 -> EXIT (delegates to getPlayerAfter(currentPlayer))
    public void testM45_TP1_GetNextPlayer() {
        game.setCurrentPlayer(dutch);
        Player next = game.getNextPlayer();
        assertNotNull(next);
        assertNotSame(dutch, next);
    }

    // ------------------------------------------------------------------
    // M46: addPlayers(List)V — 2 Test Paths (100% feasible C3; B3:true infeasible)
    // ------------------------------------------------------------------

    // M46-TP1: ENTRY -> N0 -> N1 -(B1:true)-> N12 -> EXIT (empty list)
    public void testM46_TP1_AddPlayersEmpty() {
        int before = game.players.size();
        game.addPlayers(Collections.emptyList());
        assertEquals(before, game.players.size());
    }

    // M46-TP2: ENTRY -> N0 -> N1 -(B1:false)-> N2 -(B2:false)-> N3 -(B3:false)-> N4 -> N11 -> N1 -> ... -> B2:true -> N6 -(B4:false)-> N7 -(B5:false)-> N8 / -(B5:true)-> N9 / -(B4:true)-> N10 -> N1 -> N12 -> EXIT
    public void testM46_TP2_AddPlayersAllBranches() {
        Player newP = new Player(game, "player:brandNew");
        newP.setNation(spec().getNation("model.nation.portuguese"));
        game.removeFreeColGameObject("player:brandNew", "prep");

        StubCopyFailPlayer failP = new StubCopyFailPlayer(game, "player:copyFail");
        game.setFreeColGameObject("player:copyFail", failP);
        Player failUpdate = new Player(new Game(), "player:copyFail");

        Player nonPlayerTarget = new Player(new Game(), "temp:1");
        nonPlayerTarget.setId("0"); // "0" maps to Game, not Player

        game.addPlayers(Arrays.asList(newP, dutch, failUpdate, nonPlayerTarget));
        assertNotNull(game.getFreeColGameObject("player:brandNew"));
        assertSame(failP, game.getFreeColGameObject("player:copyFail"));
    }

    // ------------------------------------------------------------------
    // M47: getUnknownEnemy()Player — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M47-TP1: ENTRY -> N0 -> EXIT (returns unknownEnemy)
    public void testM47_TP1_GetUnknownEnemy() {
        assertNull(game.getUnknownEnemy());
    }

    // ------------------------------------------------------------------
    // M48: setUnknownEnemy(Player)V — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M48-TP1: ENTRY -> N0 -> EXIT (sets unknownEnemy)
    public void testM48_TP1_SetUnknownEnemy() {
        Player enemy = createTestPlayer(game, "model.nation.unknownEnemy", false, true);
        game.setUnknownEnemy(enemy);
        assertSame(enemy, game.getUnknownEnemy());
    }

    // ------------------------------------------------------------------
    // M49: getClientUserName()String — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M49-TP1: ENTRY -> N0 -> EXIT (returns clientUserName)
    public void testM49_TP1_GetClientUserName() throws Exception {
        assertNull(game.getClientUserName());
        setField(game, "clientUserName", "alice");
        assertEquals("alice", game.getClientUserName());
    }

    // ------------------------------------------------------------------
    // M50: isInClient()Z — 2 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M50-TP1: ENTRY -> N0 -(B1:true)-> N2 -> N3 -> EXIT (clientUserName == null -> false)
    public void testM50_TP1_IsInClientFalse() {
        assertFalse(game.isInClient());
    }

    // M50-TP2: ENTRY -> N0 -(B1:false)-> N1 -> N3 -> EXIT (clientUserName != null -> true)
    public void testM50_TP2_IsInClientTrue() throws Exception {
        setField(game, "clientUserName", dutch.getName());
        assertTrue(game.isInClient());
    }

    // ------------------------------------------------------------------
    // M51: isInServer()Z — 2 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M51-TP1: ENTRY -> N0 -(B1:true)-> N2 -> N3 -> EXIT (clientUserName != null -> false)
    public void testM51_TP1_IsInServerFalse() throws Exception {
        setField(game, "clientUserName", dutch.getName());
        assertFalse(game.isInServer());
    }

    // M51-TP2: ENTRY -> N0 -(B1:false)-> N1 -> N3 -> EXIT (clientUserName == null -> true)
    public void testM51_TP2_IsInServerTrue() {
        assertTrue(game.isInServer());
    }

    // ------------------------------------------------------------------
    // M52: getClientPlayer()Player — 2 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M52-TP1: ENTRY -> N0 -(B1:true)-> N2 -> N3 -> EXIT (clientUserName != null)
    public void testM52_TP1_GetClientPlayerNonNull() throws Exception {
        setField(game, "clientUserName", dutch.getName());
        assertSame(dutch, game.getClientPlayer());
    }

    // M52-TP2: ENTRY -> N0 -(B1:false)-> N1 -> N3 -> EXIT (clientUserName == null)
    public void testM52_TP2_GetClientPlayerNull() {
        assertNull(game.getClientPlayer());
    }

    // ------------------------------------------------------------------
    // M53: isInRevengeMode()Z — 2 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M53-TP1: ENTRY -> N0 -(B1:true)-> N2 -> N3 -> EXIT (no undead player)
    public void testM53_TP1_IsInRevengeModeFalse() {
        assertFalse(game.isInRevengeMode());
    }

    // M53-TP2: ENTRY -> N0 -(B1:false)-> N1 -> N3 -> EXIT (undead player present)
    public void testM53_TP2_IsInRevengeModeTrue() {
        dutch.changePlayerType(Player.PlayerType.UNDEAD);
        assertTrue(game.isInRevengeMode());
    }

    // ------------------------------------------------------------------
    // M54: getCurrentPlayer()Player — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M54-TP1: ENTRY -> N0 -> EXIT (returns currentPlayer)
    public void testM54_TP1_GetCurrentPlayer() {
        game.setCurrentPlayer(dutch);
        assertSame(dutch, game.getCurrentPlayer());
    }

    // ------------------------------------------------------------------
    // M55: setCurrentPlayer(Player)V — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M55-TP1: ENTRY -> N0 -> EXIT (sets currentPlayer)
    public void testM55_TP1_SetCurrentPlayer() {
        game.setCurrentPlayer(french);
        assertSame(french, game.getCurrentPlayer());
    }

    // ------------------------------------------------------------------
    // M56: getMap()Map — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M56-TP1: ENTRY -> N0 -> EXIT (returns map)
    public void testM56_TP1_GetMap() {
        assertNull(game.getMap());
    }

    // ------------------------------------------------------------------
    // M57: setMap(Map)Map — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M57-TP1: ENTRY -> N0 -> EXIT (sets map and returns old map)
    public void testM57_TP1_SetMap() {
        Map map = getTestMap(plains);
        assertNull(game.setMap(map));
        assertSame(map, game.getMap());
    }

    // ------------------------------------------------------------------
    // M58: changeMap(Map)V — 2 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M58-TP1: ENTRY -> N0 -(B1:true)-> N4 -> EXIT (newMap == oldMap)
    public void testM58_TP1_ChangeMapSame() {
        Map map = getTestMap(plains);
        game.setMap(map);
        game.changeMap(map);
        assertSame(map, game.getMap());
    }

    // M58-TP2: ENTRY -> N0 -(B1:false)-> N1 -> N2 -(B2:false)-> N3 -> N2 -(B2:true)-> N4 -> EXIT (newMap != oldMap, updates HighSeas)
    public void testM58_TP2_ChangeMapDifferent() {
        Map map1 = getTestMap(plains);
        game.changeMap(map1);
        assertSame(map1, game.getMap());
        assertTrue(dutch.getHighSeas().getDestinations().contains(map1));
    }

    // ------------------------------------------------------------------
    // M59: getNationOptions()NationOptions — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M59-TP1: ENTRY -> N0 -> EXIT (returns nationOptions)
    public void testM59_TP1_GetNationOptions() {
        assertNotNull(game.getNationOptions());
    }

    // ------------------------------------------------------------------
    // M60: setNationOptions(NationOptions)V — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M60-TP1: ENTRY -> N0 -> EXIT (sets nationOptions)
    public void testM60_TP1_SetNationOptions() {
        NationOptions no = new NationOptions(spec());
        game.setNationOptions(no);
        assertSame(no, game.getNationOptions());
    }

    // ------------------------------------------------------------------
    // M61: getVacantNation()Nation — 2 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M61-TP1: ENTRY -> N0 -(B1:true)-> N2 -> N3 -> EXIT (entry == null)
    public void testM61_TP1_GetVacantNationNone() {
        NationOptions no = new NationOptions(spec());
        for (Nation n : new ArrayList<>(no.getNations().keySet())) {
            no.getNations().put(n, NationState.NOT_AVAILABLE);
        }
        game.setNationOptions(no);
        assertNull(game.getVacantNation());
    }

    // M61-TP2: ENTRY -> N0 -(B1:false)-> N1 -> N3 -> EXIT (entry != null)
    public void testM61_TP2_GetVacantNationFound() {
        game.getNationOptions().getNations().put(
                spec().getNation("model.nation.portuguese"), NationState.AVAILABLE);
        assertNotNull(game.getVacantNation());
    }

    // ------------------------------------------------------------------
    // M62: getVacantNations()List — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M62-TP1: ENTRY -> N0 -> EXIT (returns list of available nations)
    public void testM62_TP1_GetVacantNations() {
        game.getNationOptions().getNations().put(
                spec().getNation("model.nation.portuguese"), NationState.AVAILABLE);
        assertFalse(game.getVacantNations().isEmpty());
    }

    // ------------------------------------------------------------------
    // M63: canAddNewPlayer()Z — 2 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M63-TP1: ENTRY -> N0 -(B1:true)-> N2 -> N3 -> EXIT (getVacantNation() == null -> false)
    public void testM63_TP1_CanAddNewPlayerFalse() {
        NationOptions no = new NationOptions(spec());
        for (Nation n : new ArrayList<>(no.getNations().keySet())) {
            no.getNations().put(n, NationState.NOT_AVAILABLE);
        }
        game.setNationOptions(no);
        assertFalse(game.canAddNewPlayer());
    }

    // M63-TP2: ENTRY -> N0 -(B1:false)-> N1 -> N3 -> EXIT (getVacantNation() != null -> true)
    public void testM63_TP2_CanAddNewPlayerTrue() {
        game.getNationOptions().getNations().put(
                spec().getNation("model.nation.portuguese"), NationState.AVAILABLE);
        assertTrue(game.canAddNewPlayer());
    }

    // ------------------------------------------------------------------
    // M64: getTurn()Turn — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M64-TP1: ENTRY -> N0 -> EXIT (returns turn)
    public void testM64_TP1_GetTurn() {
        assertEquals(1, game.getTurn().getNumber());
    }

    // ------------------------------------------------------------------
    // M65: setTurn(Turn)V — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M65-TP1: ENTRY -> N0 -> EXIT (sets turn)
    public void testM65_TP1_SetTurn() {
        Turn t = new Turn(10);
        game.setTurn(t);
        assertSame(t, game.getTurn());
    }

    // ------------------------------------------------------------------
    // M66: getAge()I — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M66-TP1: ENTRY -> N0 -> EXIT (returns age for current turn)
    public void testM66_TP1_GetAge() {
        assertTrue(game.getAge() >= 0);
    }

    // ------------------------------------------------------------------
    // M67: getCombatModel()CombatModel — 2 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M67-TP1: ENTRY -> N0 -(B1:true)-> N2 -> EXIT (no HITPOINTS_COMBAT_MODEL -> SimpleCombatModel)
    public void testM67_TP1_GetCombatModelSimple() {
        assertTrue(game.getCombatModel() instanceof SimpleCombatModel);
    }

    // M67-TP2: ENTRY -> N0 -(B1:false)-> N1 -> EXIT (has HITPOINTS_COMBAT_MODEL -> HitpointsCombatModel)
    public void testM67_TP2_GetCombatModelHitpoints() {
        Specification s = new Specification();
        s.addAbility(new Ability(Ability.HITPOINTS_COMBAT_MODEL, true));
        Game g = new StubGame(s);
        assertTrue(g.getCombatModel() instanceof HitpointsCombatModel);
    }

    // ------------------------------------------------------------------
    // M68: getSpanishSuccession()Z — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M68-TP1: ENTRY -> N0 -> EXIT (returns spanishSuccession)
    public void testM68_TP1_GetSpanishSuccession() {
        assertFalse(game.getSpanishSuccession());
    }

    // ------------------------------------------------------------------
    // M69: setSpanishSuccession(Z)V — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M69-TP1: ENTRY -> N0 -> EXIT (sets spanishSuccession)
    public void testM69_TP1_SetSpanishSuccession() {
        game.setSpanishSuccession(true);
        assertTrue(game.getSpanishSuccession());
    }

    // ------------------------------------------------------------------
    // M70: getInitialActiveUnitId()String — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M70-TP1: ENTRY -> N0 -> EXIT (returns initialActiveUnitId)
    public void testM70_TP1_GetInitialActiveUnitId() {
        assertNull(game.getInitialActiveUnitId());
    }

    // ------------------------------------------------------------------
    // M71: getInitialActiveUnit()Unit — 2 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M71-TP1: ENTRY -> N0 -(B1:true)-> N2 -> N3 -> EXIT (initialActiveUnitId != null)
    public void testM71_TP1_GetInitialActiveUnitNonNull() {
        Map map = getTestMap(plains);
        game.setMap(map);
        Unit u = new net.sf.freecol.server.model.ServerUnit(
                game, map.getTile(5, 5), dutch, colonistType);
        game.setInitialActiveUnitId(u.getId());
        assertSame(u, game.getInitialActiveUnit());
    }

    // M71-TP2: ENTRY -> N0 -(B1:false)-> N1 -> N3 -> EXIT (initialActiveUnitId == null)
    public void testM71_TP2_GetInitialActiveUnitNull() {
        assertNull(game.getInitialActiveUnit());
    }

    // ------------------------------------------------------------------
    // M72: setInitialActiveUnitId(String)V — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M72-TP1: ENTRY -> N0 -> EXIT (sets initialActiveUnitId)
    public void testM72_TP1_SetInitialActiveUnitId() {
        game.setInitialActiveUnitId("unit:42");
        assertEquals("unit:42", game.getInitialActiveUnitId());
    }

    // ------------------------------------------------------------------
    // M73: setFreeColGameObjectListener(FreeColGameObjectListener)V — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M73-TP1: ENTRY -> N0 -> EXIT (sets listener)
    public void testM73_TP1_SetFreeColGameObjectListener() {
        StubFcgoListener listener = new StubFcgoListener();
        game.setFreeColGameObjectListener(listener);
        assertSame(listener, game.freeColGameObjectListener);
    }

    // ------------------------------------------------------------------
    // M74: notifySetFreeColGameObject(String, FreeColGameObject)V — 2 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M74-TP1: ENTRY -> N0 -(B1:true)-> N2 -> EXIT (listener == null)
    public void testM74_TP1_NotifySetNullListener() {
        game.setFreeColGameObjectListener(null);
        game.notifySetFreeColGameObject("id:1", dutch);
    }

    // M74-TP2: ENTRY -> N0 -(B1:false)-> N1 -> N2 -> EXIT (listener != null)
    public void testM74_TP2_NotifySetWithListener() {
        StubFcgoListener listener = new StubFcgoListener();
        game.setFreeColGameObjectListener(listener);
        game.notifySetFreeColGameObject("id:1", dutch);
        assertEquals("id:1", listener.lastSetId);
        assertSame(dutch, listener.lastSetObj);
    }

    // ------------------------------------------------------------------
    // M75: notifyRemoveFreeColGameObject(String)V — 2 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M75-TP1: ENTRY -> N0 -(B1:true)-> N2 -> EXIT (listener == null)
    public void testM75_TP1_NotifyRemoveNullListener() {
        game.setFreeColGameObjectListener(null);
        game.notifyRemoveFreeColGameObject("id:1");
    }

    // M75-TP2: ENTRY -> N0 -(B1:false)-> N1 -> N2 -> EXIT (listener != null)
    public void testM75_TP2_NotifyRemoveWithListener() {
        StubFcgoListener listener = new StubFcgoListener();
        game.setFreeColGameObjectListener(listener);
        game.notifyRemoveFreeColGameObject("id:1");
        assertEquals("id:1", listener.lastRemovedId);
    }

    // ------------------------------------------------------------------
    // M76: notifyOwnerChanged(FreeColGameObject, Player, Player)V — 2 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M76-TP1: ENTRY -> N0 -(B1:true)-> N2 -> EXIT (listener == null)
    public void testM76_TP1_NotifyOwnerChangedNullListener() {
        game.setFreeColGameObjectListener(null);
        game.notifyOwnerChanged(dutch, dutch, french);
    }

    // M76-TP2: ENTRY -> N0 -(B1:false)-> N1 -> N2 -> EXIT (listener != null)
    public void testM76_TP2_NotifyOwnerChangedWithListener() {
        StubFcgoListener listener = new StubFcgoListener();
        game.setFreeColGameObjectListener(listener);
        game.notifyOwnerChanged(dutch, dutch, french);
        assertSame(dutch, listener.lastOwnerSource);
        assertSame(dutch, listener.lastOldOwner);
        assertSame(french, listener.lastNewOwner);
    }

    // ------------------------------------------------------------------
    // M77: checkOwners(Ownable, Player)V — 4 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M77-TP1: ENTRY -> N0 -(B1:false)-> N1 -> EXIT (oldOwner == newOwner)
    public void testM77_TP1_CheckOwnersSame() {
        Map map = getTestMap(plains);
        game.setMap(map);
        Unit u = new net.sf.freecol.server.model.ServerUnit(
                game, map.getTile(5, 5), dutch, colonistType);
        game.checkOwners(u, dutch);
        assertSame(dutch, u.getOwner());
    }

    // M77-TP2: ENTRY -> N0 -(B1:true)-> N2 -(B2:true)-> N5 -(B4:false)-> N6 -(B5:false)-> N7 -> N8 -> EXIT (oldOwner == null, newOwner.addOwnable == true)
    public void testM77_TP2_CheckOwnersNullOldToNew() {
        Map map = getTestMap(plains);
        game.setMap(map);
        Unit u = new net.sf.freecol.server.model.ServerUnit(
                game, map.getTile(5, 5), dutch, colonistType);
        dutch.removeOwnable(u);
        game.checkOwners(u, null);
        assertTrue(dutch.hasUnit(u));
    }

    // M77-TP3: ENTRY -> N0 -(B1:true)-> N2 -(B2:false)-> N3 -(B3:false)-> N4 -> N5 -(B4:true)-> N8 -> EXIT (oldOwner.removeOwnable == true, newOwner == null)
    public void testM77_TP3_CheckOwnersOldToNullNew() {
        Map map = getTestMap(plains);
        game.setMap(map);
        Unit u = new net.sf.freecol.server.model.ServerUnit(
                game, map.getTile(5, 5), dutch, colonistType);
        u.setOwner(null);
        game.checkOwners(u, dutch);
        assertFalse(dutch.hasUnit(u));
    }

    // M77-TP4: ENTRY -> N0 -(B1:true)-> N2 -(B2:false)-> N3 -(B3:true)-> N5 -(B4:false)-> N6 -(B5:true)-> N8 -> EXIT (oldOwner.removeOwnable == false, newOwner.addOwnable == false)
    public void testM77_TP4_CheckOwnersBothReturnFalse() {
        Map map = getTestMap(plains);
        game.setMap(map);
        Unit u = new net.sf.freecol.server.model.ServerUnit(
                game, map.getTile(5, 5), dutch, colonistType);
        // u is already in dutch, not in french
        game.checkOwners(u, french);
        assertSame(dutch, u.getOwner());
    }

    // ------------------------------------------------------------------
    // M78: allPlayersReadyToLaunch()Z — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M78-TP1: ENTRY -> N0 -> EXIT (checks all live European players' ready state)
    public void testM78_TP1_AllPlayersReadyToLaunch() {
        assertFalse(game.allPlayersReadyToLaunch());
    }

    // ------------------------------------------------------------------
    // M79: getAllColonies(Player)Stream — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M79-TP1: ENTRY -> N0 -> EXIT (returns and consumes colony stream)
    public void testM79_TP1_GetAllColoniesStream() {
        game.setMap(getTestMap(plains));
        Colony c = createStandardColony(1);
        List<Colony> list = game.getAllColonies(null).collect(Collectors.toList());
        assertTrue(list.contains(c));
    }

    // ------------------------------------------------------------------
    // M80: getAllColoniesList(Player)List — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M80-TP1: ENTRY -> N0 -> EXIT (returns list of colonies)
    public void testM80_TP1_GetAllColoniesList() {
        game.setMap(getTestMap(plains));
        Colony c = createStandardColony(1);
        assertTrue(game.getAllColoniesList(null).contains(c));
    }

    // ------------------------------------------------------------------
    // M81: getSettlementByName(String)Settlement — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M81-TP1: ENTRY -> N0 -> EXIT (finds settlement by name)
    public void testM81_TP1_GetSettlementByName() {
        game.setMap(getTestMap(plains));
        Colony c = createStandardColony(1);
        assertSame(c, game.getSettlementByName(c.getName()));
    }

    // ------------------------------------------------------------------
    // M82: getMessageSource(ModelMessage)FreeColGameObject — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M82-TP1: ENTRY -> N0 -> EXIT (returns source FCGO for message)
    public void testM82_TP1_GetMessageSource() {
        ModelMessage msg = new ModelMessage(MessageType.DEFAULT, "test.msg", dutch);
        assertSame(dutch, game.getMessageSource(msg));
    }

    // ------------------------------------------------------------------
    // M83: getMessageDisplay(ModelMessage)FreeColObject — 2 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M83-TP1: ENTRY -> N0 -(B1:true)-> N2 -(B2:true)-> N6 -> EXIT (displayId != null, found as FCGO)
    public void testM83_TP1_GetMessageDisplayFcgo() {
        ModelMessage msg = new ModelMessage(MessageType.DEFAULT, "test.msg", dutch, french);
        assertSame(french, game.getMessageDisplay(msg));
    }

    // M83-TP2: ENTRY -> N0 -(B1:false)-> N1 -> N2 -(B2:false)-> N3 -> N4 -> N6 -> EXIT (displayId == null, falls back to spec type)
    public void testM83_TP2_GetMessageDisplaySpecFallback() {
        ModelMessage msg = new ModelMessage(MessageType.DEFAULT, "test.msg",
                "model.unit.freeColonist", dutch, null);
        msg.setDisplayId("model.unit.freeColonist");
        assertSame(colonistType, game.getMessageDisplay(msg));

        ModelMessage unknownDisplay = new ModelMessage(MessageType.DEFAULT, "test.msg",
                "unknown.id.xyz", dutch, null);
        unknownDisplay.setDisplayId("unknown.id.xyz");
        assertNull(game.getMessageDisplay(unknownDisplay));
    }

    // ------------------------------------------------------------------
    // M84: getStatistics()Map — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M84-TP1: ENTRY -> N0 -> N1 -(B1:false)-> N2 -(B2:true/false)-> N3/N4 -> N5 -(B3:true/false)-> N6/N7 -> N1 -(B1:true)-> N8 -> EXIT
    public void testM84_TP1_GetStatistics() {
        StubCopyFailFcgo disposedObj = new StubCopyFailFcgo(game, "stat:disposed");
        disposedObj.dispose();
        game.setFreeColGameObject("stat:disposed", disposedObj);
        java.util.Map<String, String> stats = game.getStatistics();
        assertNotNull(stats.get("freeMemory"));
        assertNotNull(stats.get("disposed"));
        assertTrue(Integer.parseInt(stats.get("disposed")) >= 1);
    }

    // ------------------------------------------------------------------
    // M85: getLocationClass(String)Class — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M85-TP1: ENTRY -> N0 -> EXIT (resolves location class by id prefix)
    public void testM85_TP1_GetLocationClass() {
        assertEquals(Tile.class, Game.getLocationClass("tile:123"));
        assertEquals(Colony.class, Game.getLocationClass("colony:1"));
    }

    // ------------------------------------------------------------------
    // M86: unserialize(String, Class)FreeColObject — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M86-TP1: ENTRY -> N0 -> N1 -> EXIT (unserializes XML string to FreeColObject)
    public void testM86_TP1_Unserialize() throws Exception {
        String xml = "<region id=\"region:test\" nameKey=\"model.region.arctic\" type=\"LAND\" predates=\"false\" scoreValue=\"0\"/>";
        Region r = game.unserialize(xml, Region.class);
        assertNotNull(r);
        assertEquals("region:test", r.getId());
    }

    // ------------------------------------------------------------------
    // M87: generateDefaultAreas()V — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M87-TP1: ENTRY -> N0 -> N1 -(B1:false)-> N2 -(B2:true/false)-> N4 -(B3:true/false)-> N6 -(B4:true/false)-> N7/N8 -> N1 -(B1:true)-> N9 -> EXIT
    public void testM87_TP1_GenerateDefaultAreas() {
        game.generateDefaultAreas();
        assertNotNull(game.getNationStartingArea(dutch.getNation()));
        int count = game.getAreas().size();
        // Second call exercises B4:true (!areas.containsKey == false)
        game.generateDefaultAreas();
        assertEquals(count, game.getAreas().size());
    }

    // ------------------------------------------------------------------
    // M88: getNationStartingArea(Nation)Area — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M88-TP1: ENTRY -> N0 -> EXIT (returns starting area for nation)
    public void testM88_TP1_GetNationStartingArea() {
        game.generateDefaultAreas();
        assertNotNull(game.getNationStartingArea(dutch.getNation()));
    }

    // ------------------------------------------------------------------
    // M89: getAreas()List — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M89-TP1: ENTRY -> N0 -> EXIT (returns list of areas)
    public void testM89_TP1_GetAreas() {
        assertNotNull(game.getAreas());
    }

    // ------------------------------------------------------------------
    // M90: addArea(Area)V — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M90-TP1: ENTRY -> N0 -> EXIT (adds area to map)
    public void testM90_TP1_AddArea() {
        Area area = new Area(game, "area:custom", "custom");
        game.addArea(area);
        assertTrue(game.getAreas().contains(area));
    }

    // ------------------------------------------------------------------
    // M91: checkIntegrity(boolean, LogBuilder)IntegrityType — 3 Test Paths (100% feasible C3; B2:false infeasible)
    // ------------------------------------------------------------------

    // M91-TP1: ENTRY -> N0 -> N1 -> N2 -(B1:false)-> N3 -(B2:true)-> N5 -(B3:false)-> N6 -> N7 -(B4:true)-> N9 -> N10 -> N2 -(B1:true)-> N11 -> N12 -> N15 -(B5:false)-> N16 -(B6:true)-> N18 -(B7:true)-> N20 -> N21 -> N22 -(B8:true)-> N24 -> N25 -> N28 -> EXIT
    public void testM91_TP1_CheckIntegrityNoFixUninitialized() {
        Game g = new StubGame(spec());
        Region uninit = Game.newInstance(g, Region.class, false);
        uninit.setId("uninit:1");
        g.setFreeColGameObject("uninit:1", uninit);
        LogBuilder lb = new LogBuilder(64);
        IntegrityType res = g.checkIntegrity(false, lb);
        assertEquals(IntegrityType.INTEGRITY_FAIL, res);
    }

    // M91-TP2: ENTRY -> N0 -> N1 -> N2 -(B1:false)-> N3 -(B2:true)-> N5 -(B3:false/true)-> N6/N2 -> N7 -(B4:false)-> N8 -> N10 -> N2 -(B1:true)-> N11 -> N12 -> N15 -(B5:false)-> N16 -(B6:false)-> N17 -> N18 -(B7:false)-> N19 -> N20 -> N21 -> N22 -(B8:false)-> N23 -> N22 -(B8:true)-> N24 -> N25 -> N28 -> EXIT
    public void testM91_TP2_CheckIntegrityFixWithMapAndPlayers() {
        Map map = getTestMap(plains);
        game.setMap(map);
        Region uninit = Game.newInstance(game, Region.class, false);
        uninit.setId("uninit:2");
        game.setFreeColGameObject("uninit:2", uninit);
        LogBuilder lb = new LogBuilder(64);
        IntegrityType res = game.checkIntegrity(true, lb);
        assertNotNull(res);
        assertNull(game.getFreeColGameObject("uninit:2"));
    }

    // M91-TP3: ENTRY -> N0 -> N1 -> N2 -(B1:false)-> N3 -(B2:true)-> N5 -(B3:true)-> N2 -(B1:true)-> N11 -> N12 -> N15 -(B5:true)-> N18 -(B7:true)-> N20 -> N21 -> N22 -(B8:true)-> N24 -> N25 -> N28 -> EXIT
    public void testM91_TP3_CheckIntegrityClean() {
        Game g = new StubGame(spec());
        LogBuilder lb = new LogBuilder(64);
        assertEquals(IntegrityType.INTEGRITY_GOOD, g.checkIntegrity(true, lb));
    }

    // ------------------------------------------------------------------
    // M92: getSpecification()Specification — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M92-TP1: ENTRY -> N0 -> EXIT (returns specification)
    public void testM92_TP1_GetSpecification() {
        assertSame(spec(), game.getSpecification());
    }

    // ------------------------------------------------------------------
    // M93: setSpecification(Specification)V — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M93-TP1: ENTRY -> N0 -> EXIT (sets specification)
    public void testM93_TP1_SetSpecification() {
        Game g = new Game();
        g.setSpecification(spec());
        assertSame(spec(), g.getSpecification());
    }

    // ------------------------------------------------------------------
    // M94: copyIn(FreeColObject)Z — 2 Test Paths (100% feasible C3; B2:false infeasible)
    // ------------------------------------------------------------------

    // M94-TP1: ENTRY -> N0 -(B1:true)-> N2 -> EXIT (o == null -> false)
    public void testM94_TP1_CopyInNullOrWrongType() {
        assertFalse(game.copyIn(null));
        assertFalse(game.copyIn(dutch));
    }

    // M94-TP2: ENTRY -> N0 -(B1:false)-> N1 -(B2:true)-> N3 -> EXIT (valid Game -> true)
    public void testM94_TP2_CopyInValid() {
        StubGame src = new StubGame(spec());
        src.setNationOptions(new NationOptions(spec()));
        StubGame target = new StubGame(spec());
        assertTrue(target.copyIn(src));
    }

    // ------------------------------------------------------------------
    // M95: writeAttributes(FreeColXMLWriter)V — 3 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M95-TP1: ENTRY -> N0 -(B1:false)-> N1 -> N4 -(B3:true)-> N6 -(B4:true)-> N8 -> EXIT (validForSave, initialActiveUnitId == null, currentPlayer == null)
    public void testM95_TP1_WriteAttributesForSave() throws Exception {
        game.setInitialActiveUnitId(null);
        game.setCurrentPlayer(null);
        StringWriter sw = new StringWriter();
        try (FreeColXMLWriter xw = new FreeColXMLWriter(sw, WriteScope.toSave())) {
            xw.writeStartElement(Game.TAG);
            game.writeAttributes(xw);
            xw.writeEndElement();
        }
        assertTrue(sw.toString().contains("nextId="));
    }

    // M95-TP2: ENTRY -> N0 -(B1:true)-> N2 -(B2:true)-> N4 -(B3:false)-> N5 -> N6 -(B4:false)-> N7 -> N8 -> EXIT (toServer -> client == null, both optional attrs non-null)
    public void testM95_TP2_WriteAttributesToServerWithOptionals() throws Exception {
        game.setInitialActiveUnitId("unit:99");
        game.setCurrentPlayer(dutch);
        StringWriter sw = new StringWriter();
        try (FreeColXMLWriter xw = new FreeColXMLWriter(sw, WriteScope.toServer())) {
            xw.writeStartElement(Game.TAG);
            game.writeAttributes(xw);
            xw.writeEndElement();
        }
        String xml = sw.toString();
        assertTrue(xml.contains("initialActiveUnitId=\"unit:99\""));
        assertTrue(xml.contains("currentPlayer="));
    }

    // M95-TP3: ENTRY -> N0 -(B1:true)-> N2 -(B2:false)-> N3 -> N4 -(B3:true)-> N6 -(B4:true)-> N8 -> EXIT (toClient -> client != null)
    public void testM95_TP3_WriteAttributesToClient() throws Exception {
        game.setInitialActiveUnitId(null);
        game.setCurrentPlayer(null);
        StringWriter sw = new StringWriter();
        try (FreeColXMLWriter xw = new FreeColXMLWriter(sw, WriteScope.toClient(dutch))) {
            xw.writeStartElement(Game.TAG);
            game.writeAttributes(xw);
            xw.writeEndElement();
        }
        assertTrue(sw.toString().contains("clientUserName="));
    }

    // ------------------------------------------------------------------
    // M96: writeChildren(FreeColXMLWriter)V — 2 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M96-TP1: ENTRY -> N0 -(B1:true)-> N6 -> N7 -(B2:true)-> N9 -> N10 -> N11 -> N14 -> N15 -> N16 -(B3:true)-> N18 -> N19 -> N22 -(B4:true)-> N24 -(B5:true)-> N26 -> N27 -(B6:true)-> N29 -> EXIT (all optional children null/empty)
    public void testM96_TP1_WriteChildrenMinimal() throws Exception {
        NameCache.clearCitiesOfCibola();
        Game g = new Game();
        g.setNationOptions(new NationOptions(spec()));
        StringWriter sw = new StringWriter();
        try (FreeColXMLWriter xw = new FreeColXMLWriter(sw, WriteScope.toSave())) {
            xw.writeStartElement(Game.TAG);
            g.writeChildren(xw);
            xw.writeEndElement();
        }
        assertTrue(sw.toString().contains("<areas></areas>") || sw.toString().contains("<areas/>"));
    }

    // M96-TP2: ENTRY -> N0 -(B1:false)-> N1..N3 -> N6 -> N7 -(B2:false)-> N8 -> N7 -(B2:true)-> N9..N15 -> N16 -(B3:false)-> N17 -> N16 -(B3:true)-> N18..N22 -(B4:false)-> N23 -> N24 -(B5:false)-> N25 -> N26 -> N27 -(B6:false)-> N28 -> N27 -(B6:true)-> N29 -> EXIT (all children present)
    public void testM96_TP2_WriteChildrenFull() throws Exception {
        NameCache.clearCitiesOfCibola();
        NameCache.addCityOfCibola("Cibola1");
        Player enemy = createTestPlayer(game, "model.nation.unknownEnemy", false, true);
        game.setUnknownEnemy(enemy);
        game.setMap(getTestMap(plains));
        game.generateDefaultAreas();

        StringWriter sw = new StringWriter();
        try (FreeColXMLWriter xw = new FreeColXMLWriter(sw, WriteScope.toSave())) {
            xw.writeStartElement(Game.TAG);
            game.writeChildren(xw);
            xw.writeEndElement();
        }
        String xml = sw.toString();
        assertTrue(xml.contains("Cibola1"));
        assertTrue(xml.contains("<areas>"));
    }

    // ------------------------------------------------------------------
    // M97: readAttributes(FreeColXMLReader)V — 2 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M97-TP1: ENTRY -> N0 -(B1:false)-> N1 -> N5 -> EXIT (UUID attribute absent)
    public void testM97_TP1_ReadAttributesNoUuid() throws Exception {
        String xml = "<game id=\"0\" turn=\"2\"/>";
        try (FreeColXMLReader xr = new FreeColXMLReader(new StringReader(xml))) {
            xr.nextTag();
            Game g = new Game();
            g.readAttributes(xr);
            assertNotNull(g.getUUID());
            assertEquals(2, g.getTurn().getNumber());
        }
    }

    // M97-TP2: ENTRY -> N0 -(B1:true)-> N2 -> N3 -> N5 -> EXIT (UUID attribute present)
    public void testM97_TP2_ReadAttributesWithUuid() throws Exception {
        UUID expected = UUID.randomUUID();
        String xml = "<game id=\"0\" UUID=\"" + expected + "\" turn=\"3\"/>";
        try (FreeColXMLReader xr = new FreeColXMLReader(new StringReader(xml))) {
            xr.nextTag();
            Game g = new Game();
            g.readAttributes(xr);
            assertEquals(expected, g.getUUID());
        }
    }

    // ------------------------------------------------------------------
    // M98: readChildren(FreeColXMLReader)V — 2 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M98-TP1: ENTRY -> N0 -> N1 -(B1:true)-> N3 -> EXIT (no colonies after reading)
    public void testM98_TP1_ReadChildrenNoColonies() throws Exception {
        String xml = "<game id=\"0\"></game>";
        try (FreeColXMLReader xr = new FreeColXMLReader(new StringReader(xml))) {
            xr.nextTag();
            Game g = new StubGame(spec());
            g.readChildren(xr);
            assertTrue(g.getAllColoniesList(null).isEmpty());
        }
    }

    // M98-TP2: ENTRY -> N0 -> N1 -(B1:false)-> N2 -> N1 -(B1:true)-> N3 -> EXIT (with colony in game)
    public void testM98_TP2_ReadChildrenWithColony() throws Exception {
        game.setMap(getTestMap(plains));
        createStandardColony(1);
        StringWriter sw = new StringWriter();
        try (FreeColXMLWriter xw = new FreeColXMLWriter(sw, WriteScope.toSave())) {
            game.toXML(xw);
        }
        try (FreeColXMLReader xr = new FreeColXMLReader(new StringReader(sw.toString()))) {
            xr.nextTag();
            Game loaded = new Game(null, xr);
            assertFalse(loaded.getAllColoniesList(null).isEmpty());
        }
    }

    // ------------------------------------------------------------------
    // M99: readChild(FreeColXMLReader)V — 12 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M99-TP1: ENTRY -> N0 -(B1:false)-> N1 -(B2:false)-> N2 -> N3 -> N27 -> EXIT (cibola with legacy prefix)
    public void testM99_TP1_ReadChildCibolaLegacyPrefix() throws Exception {
        NameCache.clearCitiesOfCibola();
        String xml = "<cibola id=\"lostCityRumour.cityName.1\"/>";
        try (FreeColXMLReader xr = new FreeColXMLReader(new StringReader(xml))) {
            xr.nextTag();
            game.readChild(xr);
            assertTrue(NameCache.getCitiesOfCibola().contains("nameCache.lostCityRumour.cityName.1"));
        }
    }

    // M99-TP2: ENTRY -> N0 -(B1:false)-> N1 -(B2:true)-> N3 -> N27 -> EXIT (cibola without legacy prefix)
    public void testM99_TP2_ReadChildCibolaNormal() throws Exception {
        NameCache.clearCitiesOfCibola();
        String xml = "<cibola id=\"nameCache.city.1\"/>";
        try (FreeColXMLReader xr = new FreeColXMLReader(new StringReader(xml))) {
            xr.nextTag();
            game.readChild(xr);
            assertTrue(NameCache.getCitiesOfCibola().contains("nameCache.city.1"));
        }
    }

    // M99-TP3: ENTRY -> N0 -(B1:true)-> N4 -(B3:false)-> N5 -(B4:false)-> N6 -> EXIT (map tag with null spec throws)
    public void testM99_TP3_ReadChildMapNullSpecThrows() throws Exception {
        String xml = "<map id=\"map:1\"/>";
        try (FreeColXMLReader xr = new FreeColXMLReader(new StringReader(xml))) {
            xr.nextTag();
            Game g = new Game();
            try {
                g.readChild(xr);
                fail("Expected XMLStreamException");
            } catch (XMLStreamException expected) {
                assertTrue(expected.getMessage().contains("null specification"));
            }
        }
    }

    // M99-TP4: ENTRY -> N0 -(B1:true)-> N4 -(B3:false)-> N5 -(B4:true)-> N7 -> N27 -> EXIT (map tag with spec)
    public void testM99_TP4_ReadChildMapValid() throws Exception {
        String xml = "<map id=\"map:1\" width=\"1\" height=\"1\"/>";
        try (FreeColXMLReader xr = new FreeColXMLReader(new StringReader(xml))) {
            xr.nextTag();
            game.readChild(xr);
            assertNotNull(game.getMap());
        }
    }

    // M99-TP5: ENTRY -> N0 -(B1:true)-> N4 -(B3:true)-> N8 -(B5:false)-> N9 -(B6:false)-> N10 -> EXIT (nationOptions tag with null spec throws)
    public void testM99_TP5_ReadChildNationOptionsNullSpecThrows() throws Exception {
        String xml = "<nationOptions id=\"no:1\"/>";
        try (FreeColXMLReader xr = new FreeColXMLReader(new StringReader(xml))) {
            xr.nextTag();
            Game g = new Game();
            try {
                g.readChild(xr);
                fail("Expected XMLStreamException");
            } catch (XMLStreamException expected) {
                assertTrue(expected.getMessage().contains("null specification"));
            }
        }
    }

    // M99-TP6: ENTRY -> N0 -(B1:true)-> N4 -(B3:true)-> N8 -(B5:false)-> N9 -(B6:true)-> N11 -> N27 -> EXIT (nationOptions tag with spec)
    public void testM99_TP6_ReadChildNationOptionsValid() throws Exception {
        StringWriter sw = new StringWriter();
        try (FreeColXMLWriter xw = new FreeColXMLWriter(sw, WriteScope.toSave())) {
            game.getNationOptions().toXML(xw);
        }
        try (FreeColXMLReader xr = new FreeColXMLReader(new StringReader(sw.toString()))) {
            xr.nextTag();
            game.readChild(xr);
            assertNotNull(game.getNationOptions());
        }
    }

    // M99-TP7: ENTRY -> N0 -(B1:true)-> N4 -(B3:true)-> N8 -(B5:true)-> N12 -(B7:false)-> N13 -(B8:false)-> N14 -> EXIT (player tag with null spec throws)
    public void testM99_TP7_ReadChildPlayerNullSpecThrows() throws Exception {
        String xml = "<player id=\"player:1\"/>";
        try (FreeColXMLReader xr = new FreeColXMLReader(new StringReader(xml))) {
            xr.nextTag();
            Game g = new Game();
            try {
                g.readChild(xr);
                fail("Expected XMLStreamException");
            } catch (XMLStreamException expected) {
                assertTrue(expected.getMessage().contains("null specification"));
            }
        }
    }

    // M99-TP8: ENTRY -> N0 -(B1:true)-> N4 -(B3:true)-> N8 -(B5:true)-> N12 -(B7:false)-> N13 -(B8:true)-> N15 -(B9:false)-> N16 -> N18 -> N27 -> EXIT (unknownEnemy player)
    public void testM99_TP8_ReadChildPlayerUnknownEnemy() throws Exception {
        Player enemy = createTestPlayer(game, "model.nation.unknownEnemy", false, true);
        StringWriter sw = new StringWriter();
        try (FreeColXMLWriter xw = new FreeColXMLWriter(sw, WriteScope.toSave())) {
            enemy.toXML(xw);
        }
        try (FreeColXMLReader xr = new FreeColXMLReader(new StringReader(sw.toString()))) {
            xr.nextTag();
            Game g = new StubGame(spec());
            g.readChild(xr);
            assertNotNull(g.getUnknownEnemy());
        }
    }

    // M99-TP9: ENTRY -> N0 -(B1:true)-> N4 -(B3:true)-> N8 -(B5:true)-> N12 -(B7:false)-> N13 -(B8:true)-> N15 -(B9:true)-> N17 -> N18 -> N27 -> EXIT (normal player)
    public void testM99_TP9_ReadChildPlayerNormal() throws Exception {
        StringWriter sw = new StringWriter();
        try (FreeColXMLWriter xw = new FreeColXMLWriter(sw, WriteScope.toSave())) {
            dutch.toXML(xw);
        }
        try (FreeColXMLReader xr = new FreeColXMLReader(new StringReader(sw.toString()))) {
            xr.nextTag();
            Game g = new StubGame(spec());
            g.readChild(xr);
            assertEquals(1, g.players.size());
        }
    }

    // M99-TP10: ENTRY -> N0 -(B1:true)-> N4 -(B3:true)-> N8 -(B5:true)-> N12 -(B7:true)-> N19 -(B10:false)-> N20 -> N27 -> EXIT (specification tag)
    public void testM99_TP10_ReadChildSpecification() throws Exception {
        StringWriter sw = new StringWriter();
        try (FreeColXMLWriter xw = new FreeColXMLWriter(sw, WriteScope.toSave())) {
            spec().toXML(xw);
        }
        try (FreeColXMLReader xr = new FreeColXMLReader(new StringReader(sw.toString()))) {
            xr.nextTag();
            Game g = new Game();
            g.readChild(xr);
            assertNotNull(g.getSpecification());
        }
    }

    // M99-TP11: ENTRY -> N0 -(B1:true)-> N4 -(B3:true)-> N8 -(B5:true)-> N12 -(B7:true)-> N19 -(B10:true)-> N21 -(B11:false)-> N22 -(B12:false)-> N23 -> N22 -(B12:true)-> N24 -> N27 -> EXIT (areas tag with child area)
    public void testM99_TP11_ReadChildAreas() throws Exception {
        Area a = new Area(game, "area:xml1", "xmlArea");
        StringWriter sw = new StringWriter();
        try (FreeColXMLWriter xw = new FreeColXMLWriter(sw, WriteScope.toSave())) {
            xw.writeStartElement("areas");
            a.toXML(xw);
            xw.writeEndElement();
        }
        try (FreeColXMLReader xr = new FreeColXMLReader(new StringReader(sw.toString()))) {
            xr.nextTag();
            Game g = new StubGame(spec());
            g.readChild(xr);
            assertEquals(1, g.getAreas().size());
        }
    }

    // M99-TP12: ENTRY -> N0 -(B1:true)-> N4 -(B3:true)-> N8 -(B5:true)-> N12 -(B7:true)-> N19 -(B10:true)-> N21 -(B11:true)-> N26 -> N27 -> EXIT (unknown tag -> super.readChild)
    public void testM99_TP12_ReadChildFallbackSuper() throws Exception {
        String xml = "<unknownTag/>";
        try (FreeColXMLReader xr = new FreeColXMLReader(new StringReader(xml))) {
            xr.nextTag();
            try {
                game.readChild(xr);
                fail("Expected XMLStreamException");
            } catch (XMLStreamException expected) {
                assertNotNull(expected);
            }
        }
    }

    // ------------------------------------------------------------------
    // M100: getXMLTagName()String — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M100-TP1: ENTRY -> N0 -> EXIT (returns "game")
    public void testM100_TP1_GetXMLTagName() {
        assertEquals(Game.TAG, game.getXMLTagName());
    }

    // ------------------------------------------------------------------
    // M101: equals(Object)Z — 2 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M101-TP1: ENTRY -> N0 -(B1:true)-> N2 -> N3 -> EXIT (this != o -> false)
    public void testM101_TP1_EqualsDifferentObject() {
        assertFalse(game.equals(new Game()));
    }

    // M101-TP2: ENTRY -> N0 -(B1:false)-> N1 -> N3 -> EXIT (this == o -> true)
    public void testM101_TP2_EqualsSameObject() {
        assertTrue(game.equals(game));
    }

    // ------------------------------------------------------------------
    // M102: hashCode()I — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M102-TP1: ENTRY -> N0 -> EXIT (returns Utils.hashCode(getId()))
    public void testM102_TP1_HashCode() {
        assertEquals(game.getId().hashCode(), game.hashCode());
    }

    // ------------------------------------------------------------------
    // M103: lambda$getStatistics$0(Map, Map$Entry)V — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M103-TP1: ENTRY -> N0 -> EXIT (executed via getStatistics() forEachMapEntry)
    public void testM103_TP1_LambdaGetStatistics() {
        java.util.Map<String, String> stats = game.getStatistics();
        assertTrue(stats.containsKey("ServerGame") || stats.containsKey("Game"));
    }

    // ------------------------------------------------------------------
    // M104: lambda$getLiveNativePlayerList$0(Player[], Player)Z — 5 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M104-TP1: ENTRY -> N0 -(B1:true)-> N5 -> N6 -> EXIT (p.isUnknownEnemy() == true)
    public void testM104_TP1_LambdaLiveNativeUnknownEnemy() {
        Game g = new StubGame(spec());
        Player unknown = createTestPlayer(g, "model.nation.unknownEnemy", false, true);
        g.players.add(unknown);
        assertFalse(g.getLiveNativePlayerList().contains(unknown));
    }

    // M104-TP2: ENTRY -> N0 -(B1:false)-> N1 -(B2:true)-> N5 -> N6 -> EXIT (!unknownEnemy, p.isDead() == true)
    public void testM104_TP2_LambdaLiveNativeDead() {
        inca.setDead(true);
        assertFalse(game.getLiveNativePlayerList().contains(inca));
    }

    // M104-TP3: ENTRY -> N0 -(B1:false)-> N1 -(B2:false)-> N2 -(B3:true)-> N5 -> N6 -> EXIT (!unknownEnemy, !dead, !isIndian)
    public void testM104_TP3_LambdaLiveNativeNotIndian() {
        assertFalse(game.getLiveNativePlayerList().contains(dutch));
    }

    // M104-TP4: ENTRY -> N0 -(B1:false)-> N1 -(B2:false)-> N2 -(B3:false)-> N3 -(B4:true)-> N5 -> N6 -> EXIT (excluded)
    public void testM104_TP4_LambdaLiveNativeExcluded() {
        assertFalse(game.getLiveNativePlayerList(inca).contains(inca));
    }

    // M104-TP5: ENTRY -> N0 -(B1:false)-> N1 -(B2:false)-> N2 -(B3:false)-> N3 -(B4:false)-> N4 -> N6 -> EXIT (included)
    public void testM104_TP5_LambdaLiveNativeIncluded() {
        assertTrue(game.getLiveNativePlayerList().contains(inca));
    }

    // ------------------------------------------------------------------
    // M105: lambda$getLiveEuropeanPlayerList$0(Player[], Player)Z — 5 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M105-TP1: ENTRY -> N0 -(B1:true)-> N5 -> N6 -> EXIT (p.isUnknownEnemy() == true)
    public void testM105_TP1_LambdaLiveEuropeanUnknownEnemy() {
        Game g = new StubGame(spec());
        Player unknown = createTestPlayer(g, "model.nation.unknownEnemy", false, true);
        g.players.add(unknown);
        assertFalse(g.getLiveEuropeanPlayerList().contains(unknown));
    }

    // M105-TP2: ENTRY -> N0 -(B1:false)-> N1 -(B2:true)-> N5 -> N6 -> EXIT (!unknownEnemy, p.isDead() == true)
    public void testM105_TP2_LambdaLiveEuropeanDead() {
        dutch.setDead(true);
        assertFalse(game.getLiveEuropeanPlayerList().contains(dutch));
    }

    // M105-TP3: ENTRY -> N0 -(B1:false)-> N1 -(B2:false)-> N2 -(B3:true)-> N5 -> N6 -> EXIT (!unknownEnemy, !dead, !isEuropean)
    public void testM105_TP3_LambdaLiveEuropeanNotEuropean() {
        assertFalse(game.getLiveEuropeanPlayerList().contains(inca));
    }

    // M105-TP4: ENTRY -> N0 -(B1:false)-> N1 -(B2:false)-> N2 -(B3:false)-> N3 -(B4:true)-> N5 -> N6 -> EXIT (excluded)
    public void testM105_TP4_LambdaLiveEuropeanExcluded() {
        assertFalse(game.getLiveEuropeanPlayerList(dutch).contains(dutch));
    }

    // M105-TP5: ENTRY -> N0 -(B1:false)-> N1 -(B2:false)-> N2 -(B3:false)-> N3 -(B4:false)-> N4 -> N6 -> EXIT (included)
    public void testM105_TP5_LambdaLiveEuropeanIncluded() {
        assertTrue(game.getLiveEuropeanPlayerList().contains(dutch));
    }

    // ------------------------------------------------------------------
    // M106: lambda$getLivePlayerList$0(Player[], Player)Z — 4 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M106-TP1: ENTRY -> N0 -(B1:true)-> N4 -> N5 -> EXIT (p.isUnknownEnemy() == true)
    public void testM106_TP1_LambdaLivePlayerUnknownEnemy() {
        Game g = new StubGame(spec());
        Player unknown = createTestPlayer(g, "model.nation.unknownEnemy", false, true);
        g.players.add(unknown);
        assertFalse(g.getLivePlayerList().contains(unknown));
    }

    // M106-TP2: ENTRY -> N0 -(B1:false)-> N1 -(B2:true)-> N4 -> N5 -> EXIT (!unknownEnemy, p.isDead() == true)
    public void testM106_TP2_LambdaLivePlayerDead() {
        dutch.setDead(true);
        assertFalse(game.getLivePlayerList().contains(dutch));
    }

    // M106-TP3: ENTRY -> N0 -(B1:false)-> N1 -(B2:false)-> N2 -(B3:true)-> N4 -> N5 -> EXIT (excluded)
    public void testM106_TP3_LambdaLivePlayerExcluded() {
        assertFalse(game.getLivePlayerList(dutch).contains(dutch));
    }

    // M106-TP4: ENTRY -> N0 -(B1:false)-> N1 -(B2:false)-> N2 -(B3:false)-> N3 -> N5 -> EXIT (included)
    public void testM106_TP4_LambdaLivePlayerIncluded() {
        assertTrue(game.getLivePlayerList().contains(dutch));
    }

    // ------------------------------------------------------------------
    // M107: <clinit>()V — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M107-TP1: ENTRY -> N0 -> EXIT (static initializer initializes serverClasses & locationClasses)
    public void testM107_TP1_StaticInit() {
        assertEquals("game", Game.TAG);
        assertEquals(Tile.class, Game.getLocationClass("tile:1"));
    }
}
