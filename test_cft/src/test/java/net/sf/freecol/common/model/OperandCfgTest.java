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
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Collections;

import net.sf.freecol.common.io.FreeColXMLReader;
import net.sf.freecol.common.io.FreeColXMLWriter;
import net.sf.freecol.common.model.Operand.OperandType;
import net.sf.freecol.common.model.Operand.ScopeLevel;
import net.sf.freecol.common.option.GameOptions;
import net.sf.freecol.server.model.ServerUnit;
import net.sf.freecol.util.test.FreeColTestCase;


/**
 * CFG-driven test suite for {@link Operand} derived from
 * {@code test_cft/graph/net/sf/freecol/common/model/Operand.json}.
 *
 * Covers all 24 methods (M1..M24, including all {@code <init>} overloads and
 * {@code lambda$...} methods) and achieves 100% feasible C3 (node + edge)
 * coverage across 62 Test Paths.
 *
 * Infeasible edges documented:
 * - M12 ({@code getValue(Game)}): {@code N10 -> N15} ({@code B5:default}) —
 *   The outer {@code TABLESWITCH} ({@code B3}) already handles {@code YEAR},
 *   {@code OPTION}, and {@code NONE}, so {@code N10} is only reachable when
 *   {@code operandType} is {@code UNITS}, {@code BUILDINGS}, {@code SETTLEMENTS},
 *   or {@code FOUNDING_FATHERS} (all 4 enum constants are handled by {@code B5}).
 * - M15 ({@code copyIn}): {@code N1 -> N2} ({@code B2:false}, {@code !super.copyIn(o)}) —
 *   When {@code o != null}, {@code copyInCast(other, Operand.class)} has already
 *   verified {@code other != null}, {@code idEquals(other)}, and
 *   {@code other instanceof Operand}; {@link Scope#copyIn} and
 *   {@link FreeColObject#copyIn} are therefore guaranteed to return {@code true}.
 */
public class OperandCfgTest extends FreeColTestCase {

    private Specification spec;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        // Pre-condition: Obtain standard specification from FreeColTestCase
        spec = spec();
    }

    // ------------------------------------------------------------------
    // Pre-condition helpers & stubs
    // ------------------------------------------------------------------

    /**
     * Minimal {@link FreeColObject} stub (not an {@link Operand})
     * used to test {@code copyInCast} type mismatch.
     */
    private static class StubPlainFreeColObject extends FreeColObject {

        StubPlainFreeColObject(String id) {
            setId(id);
        }

        @Override
        public String getXMLTagName() {
            return "stubPlainFreeColObject";
        }
    }

    /**
     * Invoke private {@link Operand#ourCount(Collection)} via reflection.
     */
    private Integer invokeOurCount(Operand op, Collection<? extends FreeColObject> objects)
            throws Exception {
        Method m = Operand.class.getDeclaredMethod("ourCount", Collection.class);
        m.setAccessible(true);
        return (Integer) m.invoke(op, objects);
    }

    /**
     * Helper to run {@code readAttributes} on an {@link Operand} from XML.
     */
    private Operand readOperandAttributes(String xml) throws Exception {
        Operand op = new Operand();
        try (FreeColXMLReader xr = new FreeColXMLReader(new StringReader(xml))) {
            xr.nextTag();
            op.readAttributes(xr);
        }
        return op;
    }

    private Colony createColonyWithMap() {
        getGame().setMap(getTestMap());
        return createStandardColony(1);
    }

    // ------------------------------------------------------------------
    // M1: <init>()V — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M1-TP1: ENTRY -> N0 -> EXIT (No-arg constructor)
    public void testM1_TP1_NoArgConstructor() {
        Operand op = new Operand();
        assertEquals(OperandType.NONE, op.getOperandType());
        assertEquals(ScopeLevel.NONE, op.getScopeLevel());
        assertNull(op.getValue());
    }

    // ------------------------------------------------------------------
    // M2: <init>(I)V — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M2-TP1: ENTRY -> N0 -> EXIT (Value constructor)
    public void testM2_TP1_ValueConstructor() {
        Operand op = new Operand(42);
        assertEquals(Integer.valueOf(42), op.getValue());
    }

    // ------------------------------------------------------------------
    // M3: <init>(Lnet/sf/freecol/common/model/Operand$OperandType;Lnet/sf/freecol/common/model/Operand$ScopeLevel;)V — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M3-TP1: ENTRY -> N0 -> EXIT (OperandType and ScopeLevel constructor)
    public void testM3_TP1_TypeAndLevelConstructor() {
        Operand op = new Operand(OperandType.UNITS, ScopeLevel.PLAYER);
        assertEquals(OperandType.UNITS, op.getOperandType());
        assertEquals(ScopeLevel.PLAYER, op.getScopeLevel());
        assertNull(op.getValue());
    }

    // ------------------------------------------------------------------
    // M4: <init>(Lnet/sf/freecol/common/io/FreeColXMLReader;)V — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M4-TP1: ENTRY -> N0 -> EXIT (XML reader constructor)
    public void testM4_TP1_XmlConstructor() throws Exception {
        String xml = "<operand operand-type=\"UNITS\" scope-level=\"PLAYER\" value=\"5\"/>";
        try (FreeColXMLReader xr = new FreeColXMLReader(new StringReader(xml))) {
            xr.nextTag();
            Operand op = new Operand(xr);
            assertEquals(OperandType.UNITS, op.getOperandType());
            assertEquals(ScopeLevel.PLAYER, op.getScopeLevel());
            assertEquals(Integer.valueOf(5), op.getValue());
        }
    }

    // ------------------------------------------------------------------
    // M5: getOperandType()Lnet/sf/freecol/common/model/Operand$OperandType; — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M5-TP1: ENTRY -> N0 -> EXIT (Return operandType)
    public void testM5_TP1_GetOperandType() {
        Operand op = new Operand(OperandType.YEAR, ScopeLevel.GAME);
        assertEquals(OperandType.YEAR, op.getOperandType());
    }

    // ------------------------------------------------------------------
    // M6: setOperandType(Lnet/sf/freecol/common/model/Operand$OperandType;)V — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M6-TP1: ENTRY -> N0 -> EXIT (Update operandType)
    public void testM6_TP1_SetOperandType() {
        Operand op = new Operand();
        op.setOperandType(OperandType.BUILDINGS);
        assertEquals(OperandType.BUILDINGS, op.getOperandType());
    }

    // ------------------------------------------------------------------
    // M7: getScopeLevel()Lnet/sf/freecol/common/model/Operand$ScopeLevel; — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M7-TP1: ENTRY -> N0 -> EXIT (Return scopeLevel)
    public void testM7_TP1_GetScopeLevel() {
        Operand op = new Operand(OperandType.UNITS, ScopeLevel.SETTLEMENT);
        assertEquals(ScopeLevel.SETTLEMENT, op.getScopeLevel());
    }

    // ------------------------------------------------------------------
    // M8: setScopeLevel(Lnet/sf/freecol/common/model/Operand$ScopeLevel;)V — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M8-TP1: ENTRY -> N0 -> EXIT (Update scopeLevel)
    public void testM8_TP1_SetScopeLevel() {
        Operand op = new Operand();
        op.setScopeLevel(ScopeLevel.GAME);
        assertEquals(ScopeLevel.GAME, op.getScopeLevel());
    }

    // ------------------------------------------------------------------
    // M9: getValue()Ljava/lang/Integer; — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M9-TP1: ENTRY -> N0 -> EXIT (Return value)
    public void testM9_TP1_GetValue() {
        Operand op = new Operand(10);
        assertEquals(Integer.valueOf(10), op.getValue());
    }

    // ------------------------------------------------------------------
    // M10: setValue(Ljava/lang/Integer;)V — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M10-TP1: ENTRY -> N0 -> EXIT (Update value)
    public void testM10_TP1_SetValue() {
        Operand op = new Operand();
        op.setValue(99);
        assertEquals(Integer.valueOf(99), op.getValue());
    }

    // ------------------------------------------------------------------
    // M11: ourCount(Ljava/util/Collection;)Ljava/lang/Integer; — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M11-TP1: ENTRY -> N0 -> EXIT (Count matching objects in collection via reflection)
    public void testM11_TP1_OurCount() throws Exception {
        Operand op = new Operand();
        UnitType colonist = spec.getUnitType("model.unit.freeColonist");
        assertEquals(Integer.valueOf(1), invokeOurCount(op, Collections.singletonList(colonist)));
    }

    // ------------------------------------------------------------------
    // M12: getValue(Lnet/sf/freecol/common/model/Game;)Ljava/lang/Integer; — 10 feasible Test Paths (100% feasible C3)
    // ------------------------------------------------------------------

    // M12-TP1: ENTRY -> N0 -(B1:false)-> N1 -> EXIT (this.value != null)
    public void testM12_TP1_GetValueGameConstant() {
        Game game = getStandardGame();
        Operand op = new Operand(7);
        assertEquals(Integer.valueOf(7), op.getValue(game));
    }

    // M12-TP2: ENTRY -> N0 -(B1:true)-> N2 -(B2:false)-> N3 -> EXIT (scopeLevel != ScopeLevel.GAME)
    public void testM12_TP2_GetValueGameWrongScope() {
        Game game = getStandardGame();
        Operand op = new Operand(OperandType.YEAR, ScopeLevel.PLAYER);
        assertNull(op.getValue(game));
    }

    // M12-TP3: ENTRY -> N0 -(B1:true)-> N2 -(B2:true)-> N4 -(B3:case:4)-> N6 -> N17 -> EXIT (OperandType.YEAR)
    public void testM12_TP3_GetValueGameYear() {
        Game game = getStandardGame();
        Operand op = new Operand(OperandType.YEAR, ScopeLevel.GAME);
        assertEquals(Integer.valueOf(game.getTurn().getYear()), op.getValue(game));
    }

    // M12-TP4: ENTRY -> N0 -(B1:true)-> N2 -(B2:true)-> N4 -(B3:case:5)-> N7 -> N17 -> EXIT (OperandType.OPTION)
    public void testM12_TP4_GetValueGameOption() {
        Game game = getStandardGame();
        Operand op = new Operand(OperandType.OPTION, ScopeLevel.GAME);
        op.setType(GameOptions.STARTING_MONEY);
        assertEquals(Integer.valueOf(spec.getInteger(GameOptions.STARTING_MONEY)), op.getValue(game));
    }

    // M12-TP5: ENTRY -> N0 -(B1:true)-> N2 -(B2:true)-> N4 -(B3:case:6)-> N5 -> N17 -> EXIT (OperandType.NONE)
    public void testM12_TP5_GetValueGameNone() {
        Game game = getStandardGame();
        Operand op = new Operand(OperandType.NONE, ScopeLevel.GAME);
        assertEquals(Integer.valueOf(0), op.getValue(game));
    }

    // M12-TP6: ENTRY -> N0 -(B1:true)-> N2 -(B2:true)-> N4 -(B3:default)-> N8 -> N9 -(B4:true)-> N17 -> EXIT (Empty live player list)
    public void testM12_TP6_GetValueGameEmptyPlayers() {
        Game emptyGame = new Game();
        Operand op = new Operand(OperandType.UNITS, ScopeLevel.GAME);
        assertEquals(Integer.valueOf(0), op.getValue(emptyGame));
    }

    // M12-TP7: ENTRY -> N0 -(B1:true)-> N2 -(B2:true)-> N4 -(B3:default)-> N8 -> N9 -(B4:false)-> N10 -(B5:case:0)-> N11 -> N16 -> N9 -(B4:true)-> N17 -> EXIT (UNITS across players)
    public void testM12_TP7_GetValueGameUnits() {
        Game game = getStandardGame();
        createColonyWithMap();
        Operand op = new Operand(OperandType.UNITS, ScopeLevel.GAME);
        assertTrue(op.getValue(game) >= 1);
    }

    // M12-TP8: ENTRY -> N0 -(B1:true)-> N2 -(B2:true)-> N4 -(B3:default)-> N8 -> N9 -(B4:false)-> N10 -(B5:case:1)-> N12 -> N16 -> N9 -(B4:true)-> N17 -> EXIT (BUILDINGS across players)
    public void testM12_TP8_GetValueGameBuildings() {
        Game game = getStandardGame();
        createColonyWithMap();
        Operand op = new Operand(OperandType.BUILDINGS, ScopeLevel.GAME);
        assertTrue(op.getValue(game) >= 1);
    }

    // M12-TP9: ENTRY -> N0 -(B1:true)-> N2 -(B2:true)-> N4 -(B3:default)-> N8 -> N9 -(B4:false)-> N10 -(B5:case:2)-> N13 -> N16 -> N9 -(B4:true)-> N17 -> EXIT (SETTLEMENTS across players)
    public void testM12_TP9_GetValueGameSettlements() {
        Game game = getStandardGame();
        createColonyWithMap();
        Operand op = new Operand(OperandType.SETTLEMENTS, ScopeLevel.GAME);
        assertTrue(op.getValue(game) >= 1);
    }

    // M12-TP10: ENTRY -> N0 -(B1:true)-> N2 -(B2:true)-> N4 -(B3:default)-> N8 -> N9 -(B4:false)-> N10 -(B5:case:3)-> N14 -> N16 -> N9 -(B4:true)-> N17 -> EXIT (FOUNDING_FATHERS across players)
    public void testM12_TP10_GetValueGameFoundingFathers() {
        Game game = getStandardGame();
        Operand op = new Operand(OperandType.FOUNDING_FATHERS, ScopeLevel.GAME);
        assertEquals(Integer.valueOf(0), op.getValue(game));
    }

    // ------------------------------------------------------------------
    // M13: getValue(Lnet/sf/freecol/common/model/Player;)Ljava/lang/Integer; — 9 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M13-TP1: ENTRY -> N0 -(B1:false)-> N1 -> EXIT (this.value != null)
    public void testM13_TP1_GetValuePlayerConstant() {
        Game game = getStandardGame();
        Player dutch = game.getPlayerByNationId("model.nation.dutch");
        Operand op = new Operand(12);
        assertEquals(Integer.valueOf(12), op.getValue(dutch));
    }

    // M13-TP2: ENTRY -> N0 -(B1:true)-> N2 -(B2:default)-> N5 -> EXIT (Inapplicable scopeLevel)
    public void testM13_TP2_GetValuePlayerInapplicableScope() {
        Game game = getStandardGame();
        Player dutch = game.getPlayerByNationId("model.nation.dutch");
        Operand op = new Operand(OperandType.UNITS, ScopeLevel.SETTLEMENT);
        assertNull(op.getValue(dutch));
    }

    // M13-TP3: ENTRY -> N0 -(B1:true)-> N2 -(B2:case:2)-> N3 -> EXIT (ScopeLevel.GAME delegates to getValue(Game))
    public void testM13_TP3_GetValuePlayerGameScope() {
        Game game = getStandardGame();
        Player dutch = game.getPlayerByNationId("model.nation.dutch");
        Operand op = new Operand(OperandType.YEAR, ScopeLevel.GAME);
        assertEquals(Integer.valueOf(game.getTurn().getYear()), op.getValue(dutch));
    }

    // M13-TP4: ENTRY -> N0 -(B1:true)-> N2 -(B2:case:1)-> N4 -> N6 -(B3:case:0)-> N7 -> EXIT (UNITS)
    public void testM13_TP4_GetValuePlayerUnits() {
        Game game = getStandardGame();
        Colony colony = createColonyWithMap();
        Operand op = new Operand(OperandType.UNITS, ScopeLevel.PLAYER);
        assertTrue(op.getValue(colony.getOwner()) >= 1);
    }

    // M13-TP5: ENTRY -> N0 -(B1:true)-> N2 -(B2:case:1)-> N4 -> N6 -(B3:case:1)-> N8 -> EXIT (BUILDINGS)
    public void testM13_TP5_GetValuePlayerBuildings() {
        Game game = getStandardGame();
        Colony colony = createColonyWithMap();
        Operand op = new Operand(OperandType.BUILDINGS, ScopeLevel.PLAYER);
        assertTrue(op.getValue(colony.getOwner()) >= 1);
    }

    // M13-TP6: ENTRY -> N0 -(B1:true)-> N2 -(B2:case:1)-> N4 -> N6 -(B3:case:2)-> N9 -(B4:false)-> N10 -> EXIT (SETTLEMENTS with methodName == null)
    public void testM13_TP6_GetValuePlayerSettlementsNoMethod() {
        Game game = getStandardGame();
        Colony colony = createColonyWithMap();
        Operand op = new Operand(OperandType.SETTLEMENTS, ScopeLevel.PLAYER);
        int expected = 1 + spec.getInteger(GameOptions.SETTLEMENT_LIMIT_MODIFIER);
        assertEquals(Integer.valueOf(expected), op.getValue(colony.getOwner()));
    }

    // M13-TP7: ENTRY -> N0 -(B1:true)-> N2 -(B2:case:1)-> N4 -> N6 -(B3:case:2)-> N9 -(B4:true)-> N11 -> EXIT (SETTLEMENTS with methodName != null)
    public void testM13_TP7_GetValuePlayerSettlementsWithMethod() {
        Game game = getStandardGame();
        Colony colony = createColonyWithMap();
        Operand op = new Operand(OperandType.SETTLEMENTS, ScopeLevel.PLAYER);
        op.setMethodName("isConnectedPort");
        op.setMethodValue("false");
        assertNotNull(op.getValue(colony.getOwner()));
    }

    // M13-TP8: ENTRY -> N0 -(B1:true)-> N2 -(B2:case:1)-> N4 -> N6 -(B3:case:3)-> N12 -> EXIT (FOUNDING_FATHERS)
    public void testM13_TP8_GetValuePlayerFoundingFathers() {
        Game game = getStandardGame();
        Player dutch = game.getPlayerByNationId("model.nation.dutch");
        Operand op = new Operand(OperandType.FOUNDING_FATHERS, ScopeLevel.PLAYER);
        assertEquals(Integer.valueOf(0), op.getValue(dutch));
    }

    // M13-TP9: ENTRY -> N0 -(B1:true)-> N2 -(B2:case:1)-> N4 -> N6 -(B3:default)-> N13 -> EXIT (NONE / default invokeMethod)
    public void testM13_TP9_GetValuePlayerDefaultInvoke() {
        Game game = getStandardGame();
        Player dutch = game.getPlayerByNationId("model.nation.dutch");
        Operand op = new Operand(OperandType.NONE, ScopeLevel.PLAYER);
        assertNull(op.getValue(dutch));
    }

    // ------------------------------------------------------------------
    // M14: getValue(Lnet/sf/freecol/common/model/Settlement;)Ljava/lang/Integer; — 6 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M14-TP1: ENTRY -> N0 -(B1:false)-> N1 -> EXIT (this.value != null)
    public void testM14_TP1_GetValueSettlementConstant() {
        getStandardGame();
        Colony colony = createColonyWithMap();
        Operand op = new Operand(3);
        assertEquals(Integer.valueOf(3), op.getValue(colony));
    }

    // M14-TP2: ENTRY -> N0 -(B1:true)-> N2 -(B2:true)-> N4 -> EXIT (scopeLevel != ScopeLevel.SETTLEMENT)
    public void testM14_TP2_GetValueSettlementWrongScope() {
        getStandardGame();
        Colony colony = createColonyWithMap();
        Operand op = new Operand(OperandType.UNITS, ScopeLevel.PLAYER);
        assertNull(op.getValue(colony));
    }

    // M14-TP3: ENTRY -> N0 -(B1:true)-> N2 -(B2:false)-> N3 -(B3:false)-> N4 -> EXIT (!(settlement instanceof Colony))
    public void testM14_TP3_GetValueSettlementNotColony() {
        Operand op = new Operand(OperandType.UNITS, ScopeLevel.SETTLEMENT);
        assertNull(op.getValue((Settlement) null));
    }

    // M14-TP4: ENTRY -> N0 -(B1:true)-> N2 -(B2:false)-> N3 -(B3:true)-> N5 -(B4:case:0)-> N6 -> EXIT (UNITS in Colony)
    public void testM14_TP4_GetValueSettlementUnits() {
        Game game = getStandardGame();
        Colony colony = createColonyWithMap();
        UnitType colonist = spec.getUnitType("model.unit.freeColonist");
        new ServerUnit(game, colony.getTile(), colony.getOwner(), colonist);
        Operand op = new Operand(OperandType.UNITS, ScopeLevel.SETTLEMENT);
        assertTrue(op.getValue(colony) >= 1);
    }

    // M14-TP5: ENTRY -> N0 -(B1:true)-> N2 -(B2:false)-> N3 -(B3:true)-> N5 -(B4:case:1)-> N7 -> EXIT (BUILDINGS in Colony)
    public void testM14_TP5_GetValueSettlementBuildings() {
        getStandardGame();
        Colony colony = createColonyWithMap();
        Operand op = new Operand(OperandType.BUILDINGS, ScopeLevel.SETTLEMENT);
        assertTrue(op.getValue(colony) >= 1);
    }

    // M14-TP6: ENTRY -> N0 -(B1:true)-> N2 -(B2:false)-> N3 -(B3:true)-> N5 -(B4:default)-> N8 -> EXIT (NONE / default invokeMethod)
    public void testM14_TP6_GetValueSettlementDefaultInvoke() {
        getStandardGame();
        Colony colony = createColonyWithMap();
        Operand op = new Operand(OperandType.NONE, ScopeLevel.SETTLEMENT);
        assertNull(op.getValue(colony));
    }

    // ------------------------------------------------------------------
    // M15: copyIn(Lnet/sf/freecol/common/model/FreeColObject;)Z — 2 feasible Test Paths (100% feasible C3)
    // ------------------------------------------------------------------

    // M15-TP1: ENTRY -> N0 -(B1:true)-> N2 -> EXIT (o == null via null, wrong type, or mismatched ID)
    public void testM15_TP1_CopyInReturnsFalseWhenCastNull() {
        Operand target = new Operand(OperandType.UNITS, ScopeLevel.PLAYER);
        assertFalse(target.copyIn(null));
        assertFalse(target.copyIn(new StubPlainFreeColObject(target.getId())));
        Operand diffId = new Operand(OperandType.UNITS, ScopeLevel.PLAYER);
        diffId.setId("other.id");
        assertFalse(target.copyIn(diffId));
    }

    // M15-TP2: ENTRY -> N0 -(B1:false)-> N1 -(B2:true)-> N3 -> EXIT (o != null && super.copyIn(o) == true)
    public void testM15_TP2_CopyInCopiesFieldsWhenCompatible() {
        Operand target = new Operand(OperandType.NONE, ScopeLevel.NONE);
        target.setId("op:copyin");
        Operand source = new Operand(OperandType.BUILDINGS, ScopeLevel.SETTLEMENT);
        source.setId("op:copyin");
        source.setValue(8);

        assertTrue(target.copyIn(source));
        assertEquals(OperandType.BUILDINGS, target.getOperandType());
        assertEquals(ScopeLevel.SETTLEMENT, target.getScopeLevel());
        assertEquals(Integer.valueOf(8), target.getValue());
    }

    // ------------------------------------------------------------------
    // M16: writeAttributes(Lnet/sf/freecol/common/io/FreeColXMLWriter;)V — 2 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M16-TP1: ENTRY -> N0 -(B1:true)-> N2 -> EXIT (this.value == null)
    public void testM16_TP1_WriteAttributesWithoutValue() throws Exception {
        Operand op = new Operand(OperandType.UNITS, ScopeLevel.PLAYER);
        StringWriter sw = new StringWriter();
        try (FreeColXMLWriter xw = new FreeColXMLWriter(sw, FreeColXMLWriter.WriteScope.toSave())) {
            xw.writeStartDocument("UTF-8", "1.0");
            xw.writeStartElement("operand");
            op.writeAttributes(xw);
            xw.writeEndElement();
            xw.writeEndDocument();
        }
        String xml = sw.toString();
        assertTrue(xml.toLowerCase().contains("operand-type=\"units\""));
        assertFalse(xml.contains(" value="));
    }

    // M16-TP2: ENTRY -> N0 -(B1:false)-> N1 -> N2 -> EXIT (this.value != null)
    public void testM16_TP2_WriteAttributesWithValue() throws Exception {
        Operand op = new Operand(OperandType.UNITS, ScopeLevel.PLAYER);
        op.setValue(15);
        StringWriter sw = new StringWriter();
        try (FreeColXMLWriter xw = new FreeColXMLWriter(sw, FreeColXMLWriter.WriteScope.toSave())) {
            xw.writeStartDocument("UTF-8", "1.0");
            xw.writeStartElement("operand");
            op.writeAttributes(xw);
            xw.writeEndElement();
            xw.writeEndDocument();
        }
        String xml = sw.toString();
        assertTrue(xml.contains("value=\"15\""));
    }

    // ------------------------------------------------------------------
    // M17: readAttributes(Lnet/sf/freecol/common/io/FreeColXMLReader;)V — 8 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M17-TP1: ENTRY -> N0 -(B1:true)-> N2 -> N3 -(B2:true)-> N5 -> N6 -(B3:true)-> N8 -> EXIT (new tags, no value)
    public void testM17_TP1_ReadAttributesNewTagsNoValue() throws Exception {
        Operand op = readOperandAttributes("<operand operand-type=\"UNITS\" scope-level=\"PLAYER\"/>");
        assertEquals(OperandType.UNITS, op.getOperandType());
        assertEquals(ScopeLevel.PLAYER, op.getScopeLevel());
        assertNull(op.getValue());
    }

    // M17-TP2: ENTRY -> N0 -(B1:true)-> N2 -> N3 -(B2:true)-> N5 -> N6 -(B3:false)-> N7 -> N8 -> EXIT (new tags, with value)
    public void testM17_TP2_ReadAttributesNewTagsWithValue() throws Exception {
        Operand op = readOperandAttributes("<operand operand-type=\"UNITS\" scope-level=\"PLAYER\" value=\"4\"/>");
        assertEquals(OperandType.UNITS, op.getOperandType());
        assertEquals(ScopeLevel.PLAYER, op.getScopeLevel());
        assertEquals(Integer.valueOf(4), op.getValue());
    }

    // M17-TP3: ENTRY -> N0 -(B1:true)-> N2 -> N3 -(B2:false)-> N4 -> N6 -(B3:true)-> N8 -> EXIT (new operand-type, old scopeLevel, no value)
    public void testM17_TP3_ReadAttributesNewTypeOldScopeNoValue() throws Exception {
        Operand op = readOperandAttributes("<operand operand-type=\"BUILDINGS\" scopeLevel=\"SETTLEMENT\"/>");
        assertEquals(OperandType.BUILDINGS, op.getOperandType());
        assertEquals(ScopeLevel.SETTLEMENT, op.getScopeLevel());
        assertNull(op.getValue());
    }

    // M17-TP4: ENTRY -> N0 -(B1:true)-> N2 -> N3 -(B2:false)-> N4 -> N6 -(B3:false)-> N7 -> N8 -> EXIT (new operand-type, old scopeLevel, with value)
    public void testM17_TP4_ReadAttributesNewTypeOldScopeWithValue() throws Exception {
        Operand op = readOperandAttributes("<operand operand-type=\"BUILDINGS\" scopeLevel=\"SETTLEMENT\" value=\"9\"/>");
        assertEquals(OperandType.BUILDINGS, op.getOperandType());
        assertEquals(ScopeLevel.SETTLEMENT, op.getScopeLevel());
        assertEquals(Integer.valueOf(9), op.getValue());
    }

    // M17-TP5: ENTRY -> N0 -(B1:false)-> N1 -> N3 -(B2:true)-> N5 -> N6 -(B3:true)-> N8 -> EXIT (old operandType, new scope-level, no value)
    public void testM17_TP5_ReadAttributesOldTypeNewScopeNoValue() throws Exception {
        Operand op = readOperandAttributes("<operand operandType=\"YEAR\" scope-level=\"GAME\"/>");
        assertEquals(OperandType.YEAR, op.getOperandType());
        assertEquals(ScopeLevel.GAME, op.getScopeLevel());
        assertNull(op.getValue());
    }

    // M17-TP6: ENTRY -> N0 -(B1:false)-> N1 -> N3 -(B2:true)-> N5 -> N6 -(B3:false)-> N7 -> N8 -> EXIT (old operandType, new scope-level, with value)
    public void testM17_TP6_ReadAttributesOldTypeNewScopeWithValue() throws Exception {
        Operand op = readOperandAttributes("<operand operandType=\"YEAR\" scope-level=\"GAME\" value=\"2\"/>");
        assertEquals(OperandType.YEAR, op.getOperandType());
        assertEquals(ScopeLevel.GAME, op.getScopeLevel());
        assertEquals(Integer.valueOf(2), op.getValue());
    }

    // M17-TP7: ENTRY -> N0 -(B1:false)-> N1 -> N3 -(B2:false)-> N4 -> N6 -(B3:true)-> N8 -> EXIT (old operandType, old scopeLevel, no value)
    public void testM17_TP7_ReadAttributesOldTagsNoValue() throws Exception {
        Operand op = readOperandAttributes("<operand operandType=\"SETTLEMENTS\" scopeLevel=\"PLAYER\"/>");
        assertEquals(OperandType.SETTLEMENTS, op.getOperandType());
        assertEquals(ScopeLevel.PLAYER, op.getScopeLevel());
        assertNull(op.getValue());
    }

    // M17-TP8: ENTRY -> N0 -(B1:false)-> N1 -> N3 -(B2:false)-> N4 -> N6 -(B3:false)-> N7 -> N8 -> EXIT (old operandType, old scopeLevel, with value)
    public void testM17_TP8_ReadAttributesOldTagsWithValue() throws Exception {
        Operand op = readOperandAttributes("<operand operandType=\"SETTLEMENTS\" scopeLevel=\"PLAYER\" value=\"6\"/>");
        assertEquals(OperandType.SETTLEMENTS, op.getOperandType());
        assertEquals(ScopeLevel.PLAYER, op.getScopeLevel());
        assertEquals(Integer.valueOf(6), op.getValue());
    }

    // ------------------------------------------------------------------
    // M18: equals(Ljava/lang/Object;)Z — 7 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M18-TP1: ENTRY -> N0 -(B1:false)-> N1 -> EXIT (this == o)
    public void testM18_TP1_EqualsSameInstance() {
        Operand op = new Operand(5);
        assertTrue(op.equals(op));
    }

    // M18-TP2: ENTRY -> N0 -(B1:true)-> N2 -(B2:true)-> N10 -> EXIT (!(o instanceof Operand))
    public void testM18_TP2_EqualsNonOperand() {
        Operand op = new Operand(5);
        assertFalse(op.equals(null));
        assertFalse(op.equals("operand"));
    }

    // M18-TP3: ENTRY -> N0 -(B1:true)-> N2 -(B2:false)-> N3 -(B3:true)-> N8 -> N9 -> EXIT (operandType != other.operandType)
    public void testM18_TP3_EqualsDifferentOperandType() {
        Operand op1 = new Operand(OperandType.UNITS, ScopeLevel.PLAYER);
        Operand op2 = new Operand(OperandType.BUILDINGS, ScopeLevel.PLAYER);
        assertFalse(op1.equals(op2));
    }

    // M18-TP4: ENTRY -> N0 -(B1:true)-> N2 -(B2:false)-> N3 -(B3:false)-> N4 -(B4:true)-> N8 -> N9 -> EXIT (scopeLevel != other.scopeLevel)
    public void testM18_TP4_EqualsDifferentScopeLevel() {
        Operand op1 = new Operand(OperandType.UNITS, ScopeLevel.PLAYER);
        Operand op2 = new Operand(OperandType.UNITS, ScopeLevel.GAME);
        assertFalse(op1.equals(op2));
    }

    // M18-TP5: ENTRY -> N0 -(B1:true)-> N2 -(B2:false)-> N3 -(B3:false)-> N4 -(B4:false)-> N5 -(B5:true)-> N8 -> N9 -> EXIT (value != other.value)
    public void testM18_TP5_EqualsDifferentValue() {
        Operand op1 = new Operand(1);
        Operand op2 = new Operand(2);
        assertFalse(op1.equals(op2));
    }

    // M18-TP6: ENTRY -> N0 -(B1:true)-> N2 -(B2:false)-> N3 -(B3:false)-> N4 -(B4:false)-> N5 -(B5:false)-> N6 -(B6:true)-> N8 -> N9 -> EXIT (super.equals(other) == false)
    public void testM18_TP6_EqualsDifferentScopeFields() {
        Operand op1 = new Operand(OperandType.UNITS, ScopeLevel.PLAYER);
        op1.setType("model.unit.freeColonist");
        Operand op2 = new Operand(OperandType.UNITS, ScopeLevel.PLAYER);
        op2.setType("model.unit.veteranSoldier");
        assertFalse(op1.equals(op2));
    }

    // M18-TP7: ENTRY -> N0 -(B1:true)-> N2 -(B2:false)-> N3 -(B3:false)-> N4 -(B4:false)-> N5 -(B5:false)-> N6 -(B6:false)-> N7 -> N9 -> EXIT (Equal distinct instances)
    public void testM18_TP7_EqualsEqualObjects() {
        Operand op1 = new Operand(10);
        Operand op2 = new Operand(10);
        assertTrue(op1.equals(op2));
    }

    // ------------------------------------------------------------------
    // M19: hashCode()I — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M19-TP1: ENTRY -> N0 -> EXIT (Compute consistent hashCode)
    public void testM19_TP1_HashCode() {
        Operand op1 = new Operand(10);
        Operand op2 = new Operand(10);
        assertEquals(op1.hashCode(), op2.hashCode());
    }

    // ------------------------------------------------------------------
    // M20: toString()Ljava/lang/String; — 2 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M20-TP1: ENTRY -> N0 -(B1:false)-> N1 -> EXIT (this.value != null)
    public void testM20_TP1_ToStringWithValue() {
        assertEquals("7", new Operand(7).toString());
    }

    // M20-TP2: ENTRY -> N0 -(B1:true)-> N2 -> EXIT (this.value == null)
    public void testM20_TP2_ToStringWithoutValue() {
        String s = new Operand(OperandType.UNITS, ScopeLevel.PLAYER).toString();
        assertTrue(s.contains("Operand type=UNITS"));
        assertTrue(s.contains("scopeLevel=PLAYER"));
    }

    // ------------------------------------------------------------------
    // M21: lambda$getValue$2(Ljava/lang/String;Ljava/lang/String;Lnet/sf/freecol/common/model/Settlement;)Z — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M21-TP1: ENTRY -> N0 -> EXIT (Evaluate settlement predicate lambda in getValue(Player))
    public void testM21_TP1_LambdaGetValueSettlementPredicate() {
        getStandardGame();
        Colony colony = createColonyWithMap();
        Operand op = new Operand(OperandType.SETTLEMENTS, ScopeLevel.PLAYER);
        op.setMethodName("isConnectedPort");
        op.setMethodValue("false");
        assertNotNull(op.getValue(colony.getOwner()));
    }

    // ------------------------------------------------------------------
    // M22: lambda$getValue$1(Lnet/sf/freecol/common/model/Colony;)I — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M22-TP1: ENTRY -> N0 -> EXIT (Evaluate colony buildings sum lambda in getValue(Player))
    public void testM22_TP1_LambdaGetValuePlayerColonyBuildings() {
        getStandardGame();
        Colony colony = createColonyWithMap();
        Operand op = new Operand(OperandType.BUILDINGS, ScopeLevel.PLAYER);
        assertTrue(op.getValue(colony.getOwner()) >= 1);
    }

    // ------------------------------------------------------------------
    // M23: lambda$getValue$0(Lnet/sf/freecol/common/model/Colony;)I — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M23-TP1: ENTRY -> N0 -> EXIT (Evaluate colony buildings sum lambda in getValue(Game))
    public void testM23_TP1_LambdaGetValueGameColonyBuildings() {
        Game game = getStandardGame();
        createColonyWithMap();
        Operand op = new Operand(OperandType.BUILDINGS, ScopeLevel.GAME);
        assertTrue(op.getValue(game) >= 1);
    }

    // ------------------------------------------------------------------
    // M24: lambda$ourCount$0(Lnet/sf/freecol/common/model/FreeColObject;)Z — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M24-TP1: ENTRY -> N0 -> EXIT (Evaluate appliesTo lambda in ourCount)
    public void testM24_TP1_LambdaOurCount() throws Exception {
        Operand op = new Operand();
        UnitType colonist = spec.getUnitType("model.unit.freeColonist");
        assertEquals(Integer.valueOf(1), invokeOurCount(op, Collections.singletonList(colonist)));
    }
}
