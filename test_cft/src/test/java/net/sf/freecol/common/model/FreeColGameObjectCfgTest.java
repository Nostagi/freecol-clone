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
import java.util.List;
import java.util.stream.Collectors;

import javax.xml.stream.XMLStreamException;

import net.sf.freecol.common.io.FreeColXMLReader;
import net.sf.freecol.common.io.FreeColXMLReader.ReadScope;
import net.sf.freecol.common.io.FreeColXMLWriter;
import net.sf.freecol.common.model.Constants.IntegrityType;
import net.sf.freecol.common.util.LogBuilder;
import net.sf.freecol.util.test.FreeColTestCase;


/**
 * CFG-driven test suite for {@link FreeColGameObject} derived from
 * {@code test_cft/graph/net/sf/freecol/common/model/FreeColGameObject.json}.
 *
 * Covers all 25 methods (M1..M25, including constructors and {@code <clinit>})
 * and achieves 100% feasible C3 (node + edge) coverage across 42 Test Paths.
 *
 * Infeasible edges documented:
 * - M2 ({@code <init>(Game)}): {@code N0 -> N2} ({@code B1:true},
 *   {@code getId() == null}) is infeasible with standard {@code FreeColObject}
 *   ID semantics because {@code getXMLTagName() + ":" + game.getNextId()} is
 *   always non-null and {@code internId} sets it via {@code setId}.
 * - M10 ({@code copyIn(FreeColObject)}): {@code N5 -> N6} ({@code B3:false},
 *   {@code !super.copyIn(fcgo)}) is infeasible because
 *   {@code copyInCast(other, FreeColGameObject.class)} already succeeded
 *   ({@code fcgo != null && idEquals(fcgo)}), so {@code FreeColObject.copyIn(fcgo)}
 *   always returns {@code true}.
 */
public class FreeColGameObjectCfgTest extends FreeColTestCase {

    private Game game;
    private Player dutchPlayer;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        // Pre-condition: Initialize standard game and Dutch player from spec
        game = getStandardGame();
        dutchPlayer = game.getPlayerByNationId("model.nation.dutch");
    }

    // ------------------------------------------------------------------
    // Pre-condition helpers & stubs
    // ------------------------------------------------------------------

    /**
     * Minimal concrete stub for {@link FreeColGameObject}.
     */
    private static class StubFreeColGameObject extends FreeColGameObject {
        private boolean nonInternable = false;

        StubFreeColGameObject(Game game) {
            super(game);
        }

        StubFreeColGameObject(Game game, String id) {
            super(game, id);
        }

        void setInternable(boolean internable) {
            this.nonInternable = !internable;
        }

        @Override
        public boolean isInternable() {
            return !this.nonInternable;
        }

        @Override
        protected void writeAttributes(FreeColXMLWriter xw) throws XMLStreamException {
            super.writeAttributes(xw);
        }

        @Override
        public String getXMLTagName() {
            return "stubFcgo";
        }
    }

    /**
     * Concrete stub implementing {@link Ownable} to exercise {@code Ownable}
     * branches in {@link FreeColGameObject#copyIn(FreeColObject)}.
     */
    private static class StubOwnableFreeColGameObject extends FreeColGameObject
            implements Ownable {
        private Player owner;

        StubOwnableFreeColGameObject(Game game, String id, Player owner) {
            super(game, id);
            this.owner = owner;
        }

        @Override
        public Player getOwner() {
            return this.owner;
        }

        @Override
        public void setOwner(Player p) {
            this.owner = p;
        }

        @Override
        protected void writeAttributes(FreeColXMLWriter xw) throws XMLStreamException {
            super.writeAttributes(xw);
        }

        @Override
        public String getXMLTagName() {
            return "stubOwnableFcgo";
        }
    }

    private FreeColXMLReader openReader(String xml, ReadScope scope) throws Exception {
        FreeColXMLReader xr = new FreeColXMLReader(new StringReader(xml));
        xr.setReadScope(scope);
        xr.nextTag();
        return xr;
    }

    // ------------------------------------------------------------------
    // M1: <init>()V — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M1-TP1: ENTRY -> N0 -> EXIT (protected no-arg constructor via Game())
    public void testM1_TP1_noArgConstructorViaGame() {
        Game bareGame = new Game();
        assertSame(bareGame, bareGame.getGame());
        assertFalse(bareGame.isDisposed());
        assertTrue(bareGame.isInitialized());
    }

    // ------------------------------------------------------------------
    // M2: <init>(Game)V — 1 Test Path (100% feasible C3)
    // ------------------------------------------------------------------

    // M2-TP1: ENTRY -> N0 -(B1:false)-> N1 -> N3 -> EXIT (getId() != null -> initialized = true)
    public void testM2_TP1_gameConstructorInitializesIdAndFlag() {
        StubFreeColGameObject obj = new StubFreeColGameObject(game);
        assertSame(game, obj.getGame());
        assertNotNull(obj.getId());
        assertTrue(obj.isInitialized());
        assertFalse(obj.isDisposed());
        assertSame(obj, game.getFreeColGameObject(obj.getId()));
    }

    // ------------------------------------------------------------------
    // M3: <init>(Game, String)V — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M3-TP1: ENTRY -> N0 -> EXIT (constructor with explicit id leaves initialized = false)
    public void testM3_TP1_gameAndIdConstructor() {
        StubFreeColGameObject obj = new StubFreeColGameObject(game, "stub:m3");
        assertSame(game, obj.getGame());
        assertEquals("stub:m3", obj.getId());
        assertFalse(obj.isInitialized());
        assertFalse(obj.isDisposed());
        assertSame(obj, game.getFreeColGameObject("stub:m3"));
    }

    // ------------------------------------------------------------------
    // M4: internId(String)V — 6 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M4-TP1: ENTRY -> N0 -(B1:true)-> N9 -> N10 -> EXIT (this.game == null)
    public void testM4_TP1_internIdWhenGameIsNull() {
        StubFreeColGameObject obj = new StubFreeColGameObject(null, "stub:nullgame");
        obj.internId("stub:updated");
        assertEquals("stub:updated", obj.getId());
    }

    // M4-TP2: ENTRY -> N0 -(B1:false)-> N1 -(B2:true)-> N9 -> N10 -> EXIT (this.game != null, newId == null)
    public void testM4_TP2_internIdWhenNewIdIsNull() {
        StubFreeColGameObject obj = new StubFreeColGameObject(game, "stub:m4_tp2");
        obj.internId(null);
        assertNull(obj.getId());
    }

    // M4-TP3: ENTRY -> N0 -(B1:false)-> N1 -(B2:false)-> N2 -(B3:true)-> N9 -> N10 -> EXIT (!isInternable())
    public void testM4_TP3_internIdWhenNotInternable() {
        StubFreeColGameObject obj = new StubFreeColGameObject(game, null);
        obj.setInternable(false);
        obj.internId("stub:noninternable");
        assertEquals("stub:noninternable", obj.getId());
        assertNull(game.getFreeColGameObject("stub:noninternable"));
    }

    // M4-TP4: ENTRY -> N0 -(B1:false)-> N1 -(B2:false)-> N2 -(B3:false)-> N3 -(B4:true)-> N5 -(B5:true)-> N7 -> N8 -> N10 -> EXIT (oldId == null)
    public void testM4_TP4_internIdWhenOldIdIsNull() {
        StubFreeColGameObject obj = new StubFreeColGameObject(game, null);
        obj.internId("stub:m4_tp4");
        assertEquals("stub:m4_tp4", obj.getId());
        assertSame(obj, game.getFreeColGameObject("stub:m4_tp4"));
    }

    // M4-TP5: ENTRY -> N0 -(B1:false)-> N1 -(B2:false)-> N2 -(B3:false)-> N3 -(B4:true)-> N5 -(B5:false)-> N6 -> N7 -> N8 -> N10 -> EXIT (oldId != null && !newId.equals(oldId))
    public void testM4_TP5_internIdOverridesExistingId() {
        StubFreeColGameObject obj = new StubFreeColGameObject(game, "stub:m4_old");
        obj.internId("stub:m4_new");
        assertEquals("stub:m4_new", obj.getId());
        assertNull(game.getFreeColGameObject("stub:m4_old"));
        assertSame(obj, game.getFreeColGameObject("stub:m4_new"));
    }

    // M4-TP6: ENTRY -> N0 -(B1:false)-> N1 -(B2:false)-> N2 -(B3:false)-> N3 -(B4:false)-> N4 -> N8 -> N10 -> EXIT (newId.equals(oldId))
    public void testM4_TP6_internIdSameIdUpdatesGame() {
        StubFreeColGameObject obj = new StubFreeColGameObject(game, "stub:m4_same");
        obj.internId("stub:m4_same");
        assertEquals("stub:m4_same", obj.getId());
        assertSame(obj, game.getFreeColGameObject("stub:m4_same"));
    }

    // ------------------------------------------------------------------
    // M5: intern()V — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M5-TP1: ENTRY -> N0 -> EXIT (interns using current getId())
    public void testM5_TP1_internReRegistersCurrentId() {
        StubFreeColGameObject obj = new StubFreeColGameObject(game, null);
        obj.setId("stub:m5");
        obj.intern();
        assertSame(obj, game.getFreeColGameObject("stub:m5"));
    }

    // ------------------------------------------------------------------
    // M6: isInitialized()Z — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M6-TP1: ENTRY -> N0 -> EXIT
    public void testM6_TP1_isInitialized() {
        StubFreeColGameObject uninit = new StubFreeColGameObject(game, "stub:m6_uninit");
        StubFreeColGameObject init = new StubFreeColGameObject(game);
        assertFalse(uninit.isInitialized());
        assertTrue(init.isInitialized());
    }

    // ------------------------------------------------------------------
    // M7: isDisposed()Z — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M7-TP1: ENTRY -> N0 -> EXIT
    public void testM7_TP1_isDisposed() {
        StubFreeColGameObject obj = new StubFreeColGameObject(game, "stub:m7");
        assertFalse(obj.isDisposed());
        obj.dispose();
        assertTrue(obj.isDisposed());
    }

    // ------------------------------------------------------------------
    // M8: dispose()V — 2 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M8-TP1: ENTRY -> N0 -(B1:false)-> N1 -> EXIT (already disposed -> early return)
    public void testM8_TP1_disposeWhenAlreadyDisposed() {
        StubFreeColGameObject obj = new StubFreeColGameObject(game, "stub:m8_tp1");
        obj.dispose();
        assertTrue(obj.isDisposed());
        // Second call hits B1:false early return
        obj.dispose();
        assertTrue(obj.isDisposed());
    }

    // M8-TP2: ENTRY -> N0 -(B1:true)-> N2 -> N3 -(B2:false)-> N4 -> N3 -(B2:true)-> N5 -> EXIT (not disposed -> iterates disposables and disposes)
    public void testM8_TP2_disposeActiveObject() {
        StubFreeColGameObject obj = new StubFreeColGameObject(game, "stub:m8_tp2");
        assertSame(obj, game.getFreeColGameObject("stub:m8_tp2"));
        obj.dispose();
        assertTrue(obj.isDisposed());
        assertNull(game.getFreeColGameObject("stub:m8_tp2"));
    }

    // ------------------------------------------------------------------
    // M9: fundamentalDispose()V — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M9-TP1: ENTRY -> N0 -> EXIT
    public void testM9_TP1_fundamentalDispose() {
        StubFreeColGameObject obj = new StubFreeColGameObject(game, "stub:m9");
        obj.fundamentalDispose();
        assertTrue(obj.isDisposed());
        assertNull(game.getFreeColGameObject("stub:m9"));
    }

    // ------------------------------------------------------------------
    // M10: copyIn(FreeColObject)Z — 4 Test Paths (100% feasible C3)
    // ------------------------------------------------------------------

    // M10-TP1: ENTRY -> N0 -(B1:false)-> N1 -> EXIT (copyInCast returns null -> returns false)
    public void testM10_TP1_copyInNullOrIncompatibleReturnsFalse() {
        StubFreeColGameObject dst = new StubFreeColGameObject(game, "stub:m10_tp1");
        assertFalse(dst.copyIn(null));
        StubFreeColGameObject differentId = new StubFreeColGameObject(game, "stub:m10_other");
        assertFalse(dst.copyIn(differentId));
    }

    // M10-TP2: ENTRY -> N0 -(B1:true)-> N2 -(B2:true)-> N4 -> N5 -(B3:true)-> N7 -(B4:true)-> N9 -> EXIT (not Ownable, owner == null)
    public void testM10_TP2_copyInNonOwnable() {
        StubFreeColGameObject dst = new StubFreeColGameObject(game, "stub:m10_tp2");
        Game otherGame = new Game();
        StubFreeColGameObject src = new StubFreeColGameObject(otherGame, "stub:m10_tp2");
        src.fundamentalDispose();
        assertTrue(dst.copyIn(src));
        assertTrue(dst.isDisposed());
        assertFalse(dst.isInitialized());
    }

    // M10-TP3: ENTRY -> N0 -(B1:true)-> N2 -(B2:false)-> N3 -> N5 -(B3:true)-> N7 -(B4:true)-> N9 -> EXIT (Ownable with owner == null)
    public void testM10_TP3_copyInOwnableWithNullOwner() {
        StubOwnableFreeColGameObject dst = new StubOwnableFreeColGameObject(game, "stub:m10_tp3", null);
        Game otherGame = new Game();
        StubOwnableFreeColGameObject src = new StubOwnableFreeColGameObject(otherGame, "stub:m10_tp3", null);
        assertTrue(dst.copyIn(src));
        assertNull(dst.getOwner());
    }

    // M10-TP4: ENTRY -> N0 -(B1:true)-> N2 -(B2:false)-> N3 -> N5 -(B3:true)-> N7 -(B4:false)-> N8 -> N9 -> EXIT (Ownable with owner != null)
    public void testM10_TP4_copyInOwnableWithNonNullOwner() {
        StubOwnableFreeColGameObject dst = new StubOwnableFreeColGameObject(game, "stub:m10_tp4", dutchPlayer);
        Game otherGame = new Game();
        StubOwnableFreeColGameObject src = new StubOwnableFreeColGameObject(otherGame, "stub:m10_tp4", dutchPlayer);
        assertTrue(dst.copyIn(src));
        assertSame(dutchPlayer, dst.getOwner());
    }

    // ------------------------------------------------------------------
    // M11: isInternable()Z — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M11-TP1: ENTRY -> N0 -> EXIT
    public void testM11_TP1_isInternableDefaultTrue() {
        FreeColGameObject obj = new FreeColGameObject(game, "stub:m11") {
            @Override
            public String getXMLTagName() {
                return "anon";
            }
        };
        assertTrue(obj.isInternable());
    }

    // ------------------------------------------------------------------
    // M12: getDisposables()Ljava/util/stream/Stream; — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M12-TP1: ENTRY -> N0 -> EXIT (terminal-consume Stream)
    public void testM12_TP1_getDisposables() {
        StubFreeColGameObject obj = new StubFreeColGameObject(game, "stub:m12");
        List<FreeColGameObject> disposables = obj.getDisposables().collect(Collectors.toList());
        assertEquals(1, disposables.size());
        assertSame(obj, disposables.get(0));
    }

    // ------------------------------------------------------------------
    // M13: disposeResources()V — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M13-TP1: ENTRY -> N0 -> EXIT
    public void testM13_TP1_disposeResources() {
        StubFreeColGameObject obj = new StubFreeColGameObject(game, "stub:m13");
        obj.disposeResources();
        assertTrue(obj.isDisposed());
        assertNull(game.getFreeColGameObject("stub:m13"));
    }

    // ------------------------------------------------------------------
    // M14: getLinkTarget(Player)Lnet/sf/freecol/common/model/FreeColGameObject; — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M14-TP1: ENTRY -> N0 -> EXIT
    public void testM14_TP1_getLinkTargetReturnsNull() {
        StubFreeColGameObject obj = new StubFreeColGameObject(game, "stub:m14");
        assertNull(obj.getLinkTarget(dutchPlayer));
    }

    // ------------------------------------------------------------------
    // M15: checkIntegrity(boolean, LogBuilder)IntegrityType — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M15-TP1: ENTRY -> N0 -> EXIT
    public void testM15_TP1_checkIntegrityReturnsGood() {
        StubFreeColGameObject obj = new StubFreeColGameObject(game, "stub:m15");
        assertEquals(IntegrityType.INTEGRITY_GOOD, obj.checkIntegrity(true, new LogBuilder(16)));
    }

    // ------------------------------------------------------------------
    // M16: getSpecification()Specification — 2 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M16-TP1: ENTRY -> N0 -(B1:true)-> N2 -> N3 -> EXIT (this.game != null)
    public void testM16_TP1_getSpecificationWithGame() {
        StubFreeColGameObject obj = new StubFreeColGameObject(game, "stub:m16_tp1");
        assertSame(game.getSpecification(), obj.getSpecification());
    }

    // M16-TP2: ENTRY -> N0 -(B1:false)-> N1 -> N3 -> EXIT (this.game == null)
    public void testM16_TP2_getSpecificationWithoutGame() {
        StubFreeColGameObject obj = new StubFreeColGameObject(null, "stub:m16_tp2");
        assertNull(obj.getSpecification());
    }

    // ------------------------------------------------------------------
    // M17: setSpecification(Specification)V — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M17-TP1: ENTRY -> N0 -> EXIT (throws RuntimeException)
    public void testM17_TP1_setSpecificationThrows() {
        StubFreeColGameObject obj = new StubFreeColGameObject(game, "stub:m17");
        try {
            obj.setSpecification(spec());
            fail("Expected RuntimeException");
        } catch (RuntimeException expected) {
            assertTrue(expected.getMessage().contains("Can not set specification"));
        }
    }

    // ------------------------------------------------------------------
    // M18: getGame()Game — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M18-TP1: ENTRY -> N0 -> EXIT
    public void testM18_TP1_getGame() {
        StubFreeColGameObject obj = new StubFreeColGameObject(game, "stub:m18");
        assertSame(game, obj.getGame());
    }

    // ------------------------------------------------------------------
    // M19: setGame(Game)V — 2 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M19-TP1: ENTRY -> N0 -(B1:true)-> N2 -> EXIT (game != null)
    public void testM19_TP1_setGameNonNull() {
        StubFreeColGameObject obj = new StubFreeColGameObject(null, "stub:m19_tp1");
        obj.setGame(game);
        assertSame(game, obj.getGame());
    }

    // M19-TP2: ENTRY -> N0 -(B1:false)-> N1 -> EXIT (game == null -> throws RuntimeException)
    public void testM19_TP2_setGameNullThrows() {
        StubFreeColGameObject obj = new StubFreeColGameObject(game, "stub:m19_tp2");
        try {
            obj.setGame(null);
            fail("Expected RuntimeException");
        } catch (RuntimeException expected) {
            assertTrue(expected.getMessage().contains("Null game"));
        }
    }

    // ------------------------------------------------------------------
    // M20: getClassIndex()I — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M20-TP1: ENTRY -> N0 -> EXIT
    public void testM20_TP1_getClassIndex() {
        StubFreeColGameObject obj = new StubFreeColGameObject(game, "stub:m20");
        assertEquals(50, obj.getClassIndex());
    }

    // ------------------------------------------------------------------
    // M21: readAttributes(FreeColXMLReader)V — 2 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M21-TP1: ENTRY -> N0 -(B1:true)-> N2 -> EXIT (xr.shouldIntern() == false)
    public void testM21_TP1_readAttributesNoIntern() throws Exception {
        StubFreeColGameObject obj = new StubFreeColGameObject(game, null);
        String xml = "<stubFcgo id=\"stub:m21_nointern\"/>";
        try (FreeColXMLReader xr = openReader(xml, ReadScope.NOINTERN)) {
            obj.readAttributes(xr);
        }
        assertEquals("stub:m21_nointern", obj.getId());
        assertNull(game.getFreeColGameObject("stub:m21_nointern"));
    }

    // M21-TP2: ENTRY -> N0 -(B1:false)-> N1 -> N2 -> EXIT (xr.shouldIntern() == true)
    public void testM21_TP2_readAttributesWithIntern() throws Exception {
        StubFreeColGameObject obj = new StubFreeColGameObject(game, null);
        String xml = "<stubFcgo id=\"stub:m21_intern\"/>";
        try (FreeColXMLReader xr = openReader(xml, ReadScope.NORMAL)) {
            obj.readAttributes(xr);
        }
        assertEquals("stub:m21_intern", obj.getId());
        assertSame(obj, game.getFreeColGameObject("stub:m21_intern"));
    }

    // ------------------------------------------------------------------
    // M22: readFromXML(FreeColXMLReader)V — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M22-TP1: ENTRY -> N0 -> EXIT (sets initialized = true and delegates to super.readFromXML)
    public void testM22_TP1_readFromXMLSetsInitialized() throws Exception {
        StubFreeColGameObject obj = new StubFreeColGameObject(game, null);
        assertFalse(obj.isInitialized());
        String xml = "<stubFcgo id=\"stub:m22\"/>";
        try (FreeColXMLReader xr = openReader(xml, ReadScope.NORMAL)) {
            obj.readFromXML(xr);
        }
        assertTrue(obj.isInitialized());
        assertEquals("stub:m22", obj.getId());
        assertSame(obj, game.getFreeColGameObject("stub:m22"));
    }

    // ------------------------------------------------------------------
    // M23: equals(Object)Z — 5 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M23-TP1: ENTRY -> N0 -(B1:false)-> N1 -> EXIT (this == o)
    public void testM23_TP1_equalsSameReference() {
        StubFreeColGameObject obj = new StubFreeColGameObject(game, "stub:m23_tp1");
        assertTrue(obj.equals(obj));
    }

    // M23-TP2: ENTRY -> N0 -(B1:true)-> N2 -(B2:true)-> N8 -> EXIT (!(o instanceof FreeColGameObject))
    public void testM23_TP2_equalsNonFreeColGameObject() {
        StubFreeColGameObject obj = new StubFreeColGameObject(game, "stub:m23_tp2");
        assertFalse(obj.equals("not-an-fcgo"));
        assertFalse(obj.equals(null));
    }

    // M23-TP3: ENTRY -> N0 -(B1:true)-> N2 -(B2:false)-> N3 -(B3:true)-> N6 -> N7 -> EXIT (different Game)
    public void testM23_TP3_equalsDifferentGame() {
        StubFreeColGameObject a = new StubFreeColGameObject(game, "stub:m23_sameid");
        Game otherGame = new Game();
        StubFreeColGameObject b = new StubFreeColGameObject(otherGame, "stub:m23_sameid");
        assertFalse(a.equals(b));
    }

    // M23-TP4: ENTRY -> N0 -(B1:true)-> N2 -(B2:false)-> N3 -(B3:false)-> N4 -(B4:true)-> N6 -> N7 -> EXIT (same Game, different ID)
    public void testM23_TP4_equalsSameGameDifferentId() {
        StubFreeColGameObject a = new StubFreeColGameObject(game, "stub:m23_a");
        StubFreeColGameObject b = new StubFreeColGameObject(game, "stub:m23_b");
        assertFalse(a.equals(b));
    }

    // M23-TP5: ENTRY -> N0 -(B1:true)-> N2 -(B2:false)-> N3 -(B3:false)-> N4 -(B4:false)-> N5 -> N7 -> EXIT (same Game, same ID, distinct instances)
    public void testM23_TP5_equalsSameGameSameId() {
        StubFreeColGameObject a = new StubFreeColGameObject(game, "stub:m23_shared");
        StubFreeColGameObject b = new StubFreeColGameObject(game, null);
        b.setId("stub:m23_shared");
        assertNotSame(a, b);
        assertTrue(a.equals(b));
    }

    // ------------------------------------------------------------------
    // M24: hashCode()I — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M24-TP1: ENTRY -> N0 -> EXIT
    public void testM24_TP1_hashCodeConsistentWithEquals() {
        StubFreeColGameObject a = new StubFreeColGameObject(game, "stub:m24");
        StubFreeColGameObject b = new StubFreeColGameObject(game, null);
        b.setId("stub:m24");
        assertEquals(a.hashCode(), b.hashCode());
    }

    // ------------------------------------------------------------------
    // M25: <clinit>()V — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M25-TP1: ENTRY -> N0 -> EXIT (static initializer runs on class load)
    public void testM25_TP1_staticInitializerLoaded() {
        assertEquals(50, new StubFreeColGameObject(game, "stub:m25").getClassIndex());
    }
}
