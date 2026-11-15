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

import net.sf.freecol.common.io.FreeColXMLReader;
import net.sf.freecol.common.io.FreeColXMLWriter;
import net.sf.freecol.common.model.Limit.Operator;
import net.sf.freecol.common.model.Operand.OperandType;
import net.sf.freecol.common.model.Operand.ScopeLevel;
import net.sf.freecol.util.test.FreeColTestCase;


/**
 * CFG-driven test suite for {@link Limit} derived from
 * {@code test_cft/graph/net/sf/freecol/common/model/Limit.json}.
 *
 * Covers all 22 methods (M1..M22, including private {@code evaluate(Integer, Integer)})
 * and achieves 100% feasible C3 (node + edge) coverage across 66 Test Paths.
 *
 * Infeasible edges documented:
 * - M15 ({@code evaluate(Integer, Integer)}): {@code N3 -> N24} ({@code B3:default}) —
 *   {@code operator} is a non-null {@link Operator} enum whose 5 constants ({@code EQ},
 *   {@code LT}, {@code GT}, {@code LE}, {@code GE}) are all handled by explicit
 *   {@code case} arms, so the compiler-generated {@code default} branch is unreachable.
 * - M16 ({@code copyIn}): {@code N1 -> N2} ({@code B2:false}) — once {@code o} is non-null
 *   ({@code B1:false}), {@code o} is guaranteed to be a {@code Limit} (subclass of
 *   {@code FreeColSpecObjectType} and {@code FreeColObject}), so {@code super.copyIn(o)}
 *   always returns {@code true}.
 */
public class LimitCfgTest extends FreeColTestCase {

    private Game game;
    private Colony colony;
    private Player player;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        // Pre-condition: Initialize standard Game, Colony (via createStandardColony), and Player
        game = getStandardGame();
        game.setMap(getTestMap());
        colony = createStandardColony(1);
        player = colony.getOwner();
    }

    // ------------------------------------------------------------------
    // Pre-condition helpers & stubs
    // ------------------------------------------------------------------

    private static Operand operandForScope(ScopeLevel scope) {
        if (scope == ScopeLevel.NONE) {
            return new Operand(1);
        }
        return new Operand(OperandType.UNITS, scope);
    }

    private static boolean invokePrivateEvaluate(Limit limit, Integer lhs, Integer rhs)
            throws Exception {
        Method m = Limit.class.getDeclaredMethod("evaluate", Integer.class, Integer.class);
        m.setAccessible(true);
        return (Boolean) m.invoke(limit, lhs, rhs);
    }

    private static FreeColXMLReader openAtRootElement(String xml) throws Exception {
        FreeColXMLReader xr = new FreeColXMLReader(new StringReader(xml));
        xr.nextTag();
        return xr;
    }

    // ------------------------------------------------------------------
    // M1: <init>(Specification)V — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M1-TP1: ENTRY -> N0 -> EXIT (constructs Limit with specification)
    public void testM1_TP1_ConstructorWithSpec() {
        Limit limit = new Limit(spec());
        assertEquals(spec(), limit.getSpecification());
        assertNull(limit.getOperator());
    }

    // ------------------------------------------------------------------
    // M2: <init>(FreeColXMLReader, Specification)V — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M2-TP1: ENTRY -> N0 -> EXIT (constructs Limit from XML reader)
    public void testM2_TP1_ConstructorFromXml() throws Exception {
        String xml = "<limit id=\"model.limit.test\" operator=\"LE\">"
                + "<left-hand-side operand-type=\"UNITS\" scope-level=\"PLAYER\"/>"
                + "<right-hand-side value=\"5\"/>"
                + "</limit>";
        try (FreeColXMLReader xr = openAtRootElement(xml)) {
            Limit limit = new Limit(xr, spec());
            assertEquals("model.limit.test", limit.getId());
            assertEquals(Operator.LE, limit.getOperator());
            assertEquals(OperandType.UNITS, limit.getLeftHandSide().getOperandType());
            assertEquals(Integer.valueOf(5), limit.getRightHandSide().getValue());
        }
    }

    // ------------------------------------------------------------------
    // M3: <init>(String, Operand, Operator, Operand)V — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M3-TP1: ENTRY -> N0 -> EXIT (constructs Limit with id, lhs, op, rhs)
    public void testM3_TP1_ConstructorWithFields() {
        Operand lhs = new Operand(OperandType.UNITS, ScopeLevel.PLAYER);
        Operand rhs = new Operand(3);
        Limit limit = new Limit("limit:id", lhs, Operator.LT, rhs);
        assertEquals("limit:id", limit.getId());
        assertSame(lhs, limit.getLeftHandSide());
        assertEquals(Operator.LT, limit.getOperator());
        assertSame(rhs, limit.getRightHandSide());
    }

    // ------------------------------------------------------------------
    // M4: getOperator()Limit$Operator; — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M4-TP1: ENTRY -> N0 -> EXIT (returns operator)
    public void testM4_TP1_GetOperator() {
        Limit limit = new Limit("l", new Operand(1), Operator.EQ, new Operand(1));
        assertEquals(Operator.EQ, limit.getOperator());
    }

    // ------------------------------------------------------------------
    // M5: setOperator(Limit$Operator;)V — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M5-TP1: ENTRY -> N0 -> EXIT (sets operator)
    public void testM5_TP1_SetOperator() {
        Limit limit = new Limit(spec());
        limit.setOperator(Operator.GE);
        assertEquals(Operator.GE, limit.getOperator());
    }

    // ------------------------------------------------------------------
    // M6: getLeftHandSide()Operand; — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M6-TP1: ENTRY -> N0 -> EXIT (returns leftHandSide)
    public void testM6_TP1_GetLeftHandSide() {
        Operand lhs = new Operand(2);
        Limit limit = new Limit("l", lhs, Operator.EQ, new Operand(2));
        assertSame(lhs, limit.getLeftHandSide());
    }

    // ------------------------------------------------------------------
    // M7: setLeftHandSide(Operand;)V — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M7-TP1: ENTRY -> N0 -> EXIT (sets leftHandSide)
    public void testM7_TP1_SetLeftHandSide() {
        Limit limit = new Limit(spec());
        Operand lhs = new Operand(4);
        limit.setLeftHandSide(lhs);
        assertSame(lhs, limit.getLeftHandSide());
    }

    // ------------------------------------------------------------------
    // M8: getRightHandSide()Operand; — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M8-TP1: ENTRY -> N0 -> EXIT (returns rightHandSide)
    public void testM8_TP1_GetRightHandSide() {
        Operand rhs = new Operand(7);
        Limit limit = new Limit("l", new Operand(1), Operator.LT, rhs);
        assertSame(rhs, limit.getRightHandSide());
    }

    // ------------------------------------------------------------------
    // M9: setRightHandSide(Operand;)V — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M9-TP1: ENTRY -> N0 -> EXIT (sets rightHandSide)
    public void testM9_TP1_SetRightHandSide() {
        Limit limit = new Limit(spec());
        Operand rhs = new Operand(9);
        limit.setRightHandSide(rhs);
        assertSame(rhs, limit.getRightHandSide());
    }

    // ------------------------------------------------------------------
    // M10: appliesTo(FreeColObject)Z — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M10-TP1: ENTRY -> N0 -> EXIT (delegates to leftHandSide.appliesTo)
    public void testM10_TP1_AppliesTo() {
        Limit limit = new Limit("l", new Operand(OperandType.UNITS, ScopeLevel.PLAYER),
                Operator.LT, new Operand(5));
        assertTrue(limit.appliesTo(colony));
    }

    // ------------------------------------------------------------------
    // M11: evaluate(Game)Z — 4 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M11-TP1: ENTRY -> N0 -(B1:default)-> N2 -> N3 -(B2:default)-> N5 -> N6 -> EXIT
    public void testM11_TP1_EvaluateGameDefaultDefault() {
        Limit limit = new Limit("l", operandForScope(ScopeLevel.NONE),
                Operator.EQ, operandForScope(ScopeLevel.NONE));
        assertTrue(limit.evaluate(game));
    }

    // M11-TP2: ENTRY -> N0 -(B1:default)-> N2 -> N3 -(B2:case:1)-> N4 -> N6 -> EXIT
    public void testM11_TP2_EvaluateGameDefaultGame() {
        Limit limit = new Limit("l", operandForScope(ScopeLevel.NONE),
                Operator.GE, operandForScope(ScopeLevel.GAME));
        limit.evaluate(game);
    }

    // M11-TP3: ENTRY -> N0 -(B1:case:1)-> N1 -> N3 -(B2:default)-> N5 -> N6 -> EXIT
    public void testM11_TP3_EvaluateGameGameDefault() {
        Limit limit = new Limit("l", operandForScope(ScopeLevel.GAME),
                Operator.GE, operandForScope(ScopeLevel.NONE));
        limit.evaluate(game);
    }

    // M11-TP4: ENTRY -> N0 -(B1:case:1)-> N1 -> N3 -(B2:case:1)-> N4 -> N6 -> EXIT
    public void testM11_TP4_EvaluateGameGameGame() {
        Limit limit = new Limit("l", operandForScope(ScopeLevel.GAME),
                Operator.EQ, operandForScope(ScopeLevel.GAME));
        assertTrue(limit.evaluate(game));
    }

    // ------------------------------------------------------------------
    // M12: evaluate(Player)Z — 9 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M12-TP1..TP9: All 3x3 combinations of {NONE (default), GAME (case:1), PLAYER (case:2)}
    public void testM12_TP1_to_TP9_EvaluatePlayerAllScopeCombinations() {
        ScopeLevel[] scopes = { ScopeLevel.NONE, ScopeLevel.GAME, ScopeLevel.PLAYER };
        for (ScopeLevel lhsScope : scopes) {
            for (ScopeLevel rhsScope : scopes) {
                Limit limit = new Limit("l", operandForScope(lhsScope),
                        Operator.GE, operandForScope(rhsScope));
                limit.evaluate(player);
            }
        }
    }

    // ------------------------------------------------------------------
    // M13: evaluate(Settlement)Z — 16 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M13-TP1..TP16: All 4x4 combinations of {NONE (default), GAME (case:1), PLAYER (case:2), SETTLEMENT (case:3)}
    public void testM13_TP1_to_TP16_EvaluateSettlementAllScopeCombinations() {
        ScopeLevel[] scopes = {
            ScopeLevel.NONE, ScopeLevel.GAME, ScopeLevel.PLAYER, ScopeLevel.SETTLEMENT
        };
        for (ScopeLevel lhsScope : scopes) {
            for (ScopeLevel rhsScope : scopes) {
                Limit limit = new Limit("l", operandForScope(lhsScope),
                        Operator.GE, operandForScope(rhsScope));
                limit.evaluate(colony);
            }
        }
    }

    // ------------------------------------------------------------------
    // M14: hasOperandType(OperandType)Z — 3 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M14-TP1: ENTRY -> N0 -(B1:true)-> N2 -> N4 -> EXIT (leftHandSide matches type)
    public void testM14_TP1_HasOperandTypeMatchesLeft() {
        Limit limit = new Limit("l", new Operand(OperandType.UNITS, ScopeLevel.PLAYER),
                Operator.LT, new Operand(5));
        assertTrue(limit.hasOperandType(OperandType.UNITS));
    }

    // M14-TP2: ENTRY -> N0 -(B1:false)-> N1 -(B2:false)-> N2 -> N4 -> EXIT
    // (leftHandSide does not match, rightHandSide matches)
    public void testM14_TP2_HasOperandTypeMatchesRight() {
        Limit limit = new Limit("l", new Operand(5),
                Operator.LT, new Operand(OperandType.BUILDINGS, ScopeLevel.SETTLEMENT));
        assertTrue(limit.hasOperandType(OperandType.BUILDINGS));
    }

    // M14-TP3: ENTRY -> N0 -(B1:false)-> N1 -(B2:true)-> N3 -> N4 -> EXIT
    // (neither leftHandSide nor rightHandSide matches)
    public void testM14_TP3_HasOperandTypeMatchesNeither() {
        Limit limit = new Limit("l", new Operand(OperandType.UNITS, ScopeLevel.PLAYER),
                Operator.LT, new Operand(5));
        assertFalse(limit.hasOperandType(OperandType.FOUNDING_FATHERS));
    }

    // ------------------------------------------------------------------
    // M15: evaluate(Integer, Integer)Z — 12 feasible Test Paths (100% feasible C3)
    // ------------------------------------------------------------------

    // M15-TP1: ENTRY -> N0 -(B1:true)-> N2 -> EXIT (lhs == null -> true)
    public void testM15_TP1_EvaluateIntegersLhsNull() throws Exception {
        Limit limit = new Limit("l", new Operand(1), Operator.EQ, new Operand(1));
        assertTrue(invokePrivateEvaluate(limit, null, 5));
    }

    // M15-TP2: ENTRY -> N0 -(B1:false)-> N1 -(B2:false)-> N2 -> EXIT (lhs != null, rhs == null -> true)
    public void testM15_TP2_EvaluateIntegersRhsNull() throws Exception {
        Limit limit = new Limit("l", new Operand(1), Operator.EQ, new Operand(1));
        assertTrue(invokePrivateEvaluate(limit, 5, null));
    }

    // M15-TP3: ENTRY -> N0 -(B1:false)-> N1 -(B2:true)-> N3 -(B3:case:0)-> N4 -(B4:true)-> N6 -> N7 -> EXIT (EQ false)
    public void testM15_TP3_EvaluateIntegersEqFalse() throws Exception {
        Limit limit = new Limit("l", new Operand(1), Operator.EQ, new Operand(1));
        assertFalse(invokePrivateEvaluate(limit, 3, 5));
    }

    // M15-TP4: ENTRY -> N0 -(B1:false)-> N1 -(B2:true)-> N3 -(B3:case:0)-> N4 -(B4:false)-> N5 -> N7 -> EXIT (EQ true)
    public void testM15_TP4_EvaluateIntegersEqTrue() throws Exception {
        Limit limit = new Limit("l", new Operand(1), Operator.EQ, new Operand(1));
        assertTrue(invokePrivateEvaluate(limit, 5, 5));
    }

    // M15-TP5: ENTRY -> N0 -(B1:false)-> N1 -(B2:true)-> N3 -(B3:case:1)-> N8 -(B5:true)-> N10 -> N11 -> EXIT (LT false)
    public void testM15_TP5_EvaluateIntegersLtFalse() throws Exception {
        Limit limit = new Limit("l", new Operand(1), Operator.LT, new Operand(1));
        assertFalse(invokePrivateEvaluate(limit, 5, 5));
    }

    // M15-TP6: ENTRY -> N0 -(B1:false)-> N1 -(B2:true)-> N3 -(B3:case:1)-> N8 -(B5:false)-> N9 -> N11 -> EXIT (LT true)
    public void testM15_TP6_EvaluateIntegersLtTrue() throws Exception {
        Limit limit = new Limit("l", new Operand(1), Operator.LT, new Operand(1));
        assertTrue(invokePrivateEvaluate(limit, 3, 5));
    }

    // M15-TP7: ENTRY -> N0 -(B1:false)-> N1 -(B2:true)-> N3 -(B3:case:2)-> N12 -(B6:true)-> N14 -> N15 -> EXIT (GT false)
    public void testM15_TP7_EvaluateIntegersGtFalse() throws Exception {
        Limit limit = new Limit("l", new Operand(1), Operator.GT, new Operand(1));
        assertFalse(invokePrivateEvaluate(limit, 5, 5));
    }

    // M15-TP8: ENTRY -> N0 -(B1:false)-> N1 -(B2:true)-> N3 -(B3:case:2)-> N12 -(B6:false)-> N13 -> N15 -> EXIT (GT true)
    public void testM15_TP8_EvaluateIntegersGtTrue() throws Exception {
        Limit limit = new Limit("l", new Operand(1), Operator.GT, new Operand(1));
        assertTrue(invokePrivateEvaluate(limit, 7, 5));
    }

    // M15-TP9: ENTRY -> N0 -(B1:false)-> N1 -(B2:true)-> N3 -(B3:case:3)-> N16 -(B7:true)-> N18 -> N19 -> EXIT (LE false)
    public void testM15_TP9_EvaluateIntegersLeFalse() throws Exception {
        Limit limit = new Limit("l", new Operand(1), Operator.LE, new Operand(1));
        assertFalse(invokePrivateEvaluate(limit, 6, 5));
    }

    // M15-TP10: ENTRY -> N0 -(B1:false)-> N1 -(B2:true)-> N3 -(B3:case:3)-> N16 -(B7:false)-> N17 -> N19 -> EXIT (LE true)
    public void testM15_TP10_EvaluateIntegersLeTrue() throws Exception {
        Limit limit = new Limit("l", new Operand(1), Operator.LE, new Operand(1));
        assertTrue(invokePrivateEvaluate(limit, 5, 5));
    }

    // M15-TP11: ENTRY -> N0 -(B1:false)-> N1 -(B2:true)-> N3 -(B3:case:4)-> N20 -(B8:true)-> N22 -> N23 -> EXIT (GE false)
    public void testM15_TP11_EvaluateIntegersGeFalse() throws Exception {
        Limit limit = new Limit("l", new Operand(1), Operator.GE, new Operand(1));
        assertFalse(invokePrivateEvaluate(limit, 4, 5));
    }

    // M15-TP12: ENTRY -> N0 -(B1:false)-> N1 -(B2:true)-> N3 -(B3:case:4)-> N20 -(B8:false)-> N21 -> N23 -> EXIT (GE true)
    public void testM15_TP12_EvaluateIntegersGeTrue() throws Exception {
        Limit limit = new Limit("l", new Operand(1), Operator.GE, new Operand(1));
        assertTrue(invokePrivateEvaluate(limit, 5, 5));
    }

    // ------------------------------------------------------------------
    // M16: copyIn(FreeColObject)Z — 2 feasible Test Paths (100% feasible C3)
    // ------------------------------------------------------------------

    // M16-TP1: ENTRY -> N0 -(B1:true)-> N2 -> EXIT (other is not Limit -> false)
    public void testM16_TP1_CopyInWrongTypeReturnsFalse() {
        Limit target = new Limit(spec());
        assertFalse(target.copyIn(colony));
    }

    // M16-TP2: ENTRY -> N0 -(B1:false)-> N1 -(B2:true)-> N3 -> EXIT (other is Limit -> true)
    public void testM16_TP2_CopyInValidLimitReturnsTrue() {
        Limit target = new Limit(spec());
        target.setId("limit:src");
        target.getFeatureContainer();
        Operand lhs = new Operand(OperandType.SETTLEMENTS, ScopeLevel.PLAYER);
        Operand rhs = new Operand(3);
        Limit source = new Limit("limit:src", lhs, Operator.LE, rhs);

        assertTrue(target.copyIn(source));
        assertEquals(Operator.LE, target.getOperator());
        assertSame(lhs, target.getLeftHandSide());
        assertSame(rhs, target.getRightHandSide());
    }

    // ------------------------------------------------------------------
    // M17: writeAttributes(FreeColXMLWriter)V — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M17-TP1: ENTRY -> N0 -> EXIT (writes operator attribute)
    public void testM17_TP1_WriteAttributes() throws Exception {
        Limit limit = new Limit("limit:wa", new Operand(1), Operator.GT, new Operand(0));
        StringWriter sw = new StringWriter();
        try (FreeColXMLWriter xw = new FreeColXMLWriter(
                sw, FreeColXMLWriter.WriteScope.toSave(), false)) {
            xw.writeStartDocument("UTF-8", "1.0");
            xw.writeStartElement(Limit.TAG);
            limit.writeAttributes(xw);
            xw.writeEndElement();
            xw.writeEndDocument();
        }
        assertTrue(sw.toString().contains("operator=\"gt\""));
    }

    // ------------------------------------------------------------------
    // M18: writeChildren(FreeColXMLWriter)V — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M18-TP1: ENTRY -> N0 -> EXIT (writes left-hand-side and right-hand-side elements)
    public void testM18_TP1_WriteChildren() throws Exception {
        Limit limit = new Limit("limit:wc", new Operand(2), Operator.EQ, new Operand(2));
        StringWriter sw = new StringWriter();
        try (FreeColXMLWriter xw = new FreeColXMLWriter(
                sw, FreeColXMLWriter.WriteScope.toSave(), false)) {
            xw.writeStartDocument("UTF-8", "1.0");
            xw.writeStartElement(Limit.TAG);
            limit.writeChildren(xw);
            xw.writeEndElement();
            xw.writeEndDocument();
        }
        String xml = sw.toString();
        assertTrue(xml.contains("left-hand-side"));
        assertTrue(xml.contains("right-hand-side"));
    }

    // ------------------------------------------------------------------
    // M19: readAttributes(FreeColXMLReader)V — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M19-TP1: ENTRY -> N0 -> EXIT (reads operator attribute)
    public void testM19_TP1_ReadAttributes() throws Exception {
        Limit limit = new Limit(spec());
        String xml = "<limit id=\"limit:ra\" operator=\"GE\"/>";
        try (FreeColXMLReader xr = openAtRootElement(xml)) {
            limit.readAttributes(xr);
        }
        assertEquals(Operator.GE, limit.getOperator());
    }

    // ------------------------------------------------------------------
    // M20: readChild(FreeColXMLReader)V — 5 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M20-TP1: ENTRY -> N0 -(B1:true)-> N2 -> N7 -> EXIT (tag equals "left-hand-side")
    public void testM20_TP1_ReadChildLeftHandSide() throws Exception {
        Limit limit = new Limit(spec());
        String xml = "<limit id=\"l1\"><left-hand-side value=\"3\"/></limit>";
        try (FreeColXMLReader xr = openAtRootElement(xml)) {
            xr.nextTag();
            limit.readChild(xr);
        }
        assertEquals(Integer.valueOf(3), limit.getLeftHandSide().getValue());
    }

    // M20-TP2: ENTRY -> N0 -(B1:false)-> N1 -(B2:false)-> N2 -> N7 -> EXIT (tag equals "leftHandSide")
    public void testM20_TP2_ReadChildOldLeftHandSide() throws Exception {
        Limit limit = new Limit(spec());
        String xml = "<limit id=\"l2\"><leftHandSide value=\"4\"/></limit>";
        try (FreeColXMLReader xr = openAtRootElement(xml)) {
            xr.nextTag();
            limit.readChild(xr);
        }
        assertEquals(Integer.valueOf(4), limit.getLeftHandSide().getValue());
    }

    // M20-TP3: ENTRY -> N0 -(B1:false)-> N1 -(B2:true)-> N3 -(B3:true)-> N5 -> N7 -> EXIT (tag equals "right-hand-side")
    public void testM20_TP3_ReadChildRightHandSide() throws Exception {
        Limit limit = new Limit(spec());
        String xml = "<limit id=\"l3\"><right-hand-side value=\"7\"/></limit>";
        try (FreeColXMLReader xr = openAtRootElement(xml)) {
            xr.nextTag();
            limit.readChild(xr);
        }
        assertEquals(Integer.valueOf(7), limit.getRightHandSide().getValue());
    }

    // M20-TP4: ENTRY -> N0 -(B1:false)-> N1 -(B2:true)-> N3 -(B3:false)-> N4 -(B4:false)-> N5 -> N7 -> EXIT (tag equals "rightHandSide")
    public void testM20_TP4_ReadChildOldRightHandSide() throws Exception {
        Limit limit = new Limit(spec());
        String xml = "<limit id=\"l4\"><rightHandSide value=\"8\"/></limit>";
        try (FreeColXMLReader xr = openAtRootElement(xml)) {
            xr.nextTag();
            limit.readChild(xr);
        }
        assertEquals(Integer.valueOf(8), limit.getRightHandSide().getValue());
    }

    // M20-TP5: ENTRY -> N0 -(B1:false)-> N1 -(B2:true)-> N3 -(B3:false)-> N4 -(B4:true)-> N6 -> N7 -> EXIT (other tag -> super.readChild)
    public void testM20_TP5_ReadChildDelegatesToSuper() throws Exception {
        Limit limit = new Limit(spec());
        String xml = "<limit id=\"l5\"><ability id=\"model.ability.test\" value=\"true\"/></limit>";
        try (FreeColXMLReader xr = openAtRootElement(xml)) {
            xr.nextTag();
            limit.readChild(xr);
        }
        assertTrue(limit.hasAbility("model.ability.test"));
    }

    // ------------------------------------------------------------------
    // M21: getXMLTagName()Ljava/lang/String; — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M21-TP1: ENTRY -> N0 -> EXIT (returns TAG)
    public void testM21_TP1_GetXMLTagName() {
        Limit limit = new Limit(spec());
        assertEquals(Limit.TAG, limit.getXMLTagName());
        assertEquals("limit", limit.getXMLTagName());
    }

    // ------------------------------------------------------------------
    // M22: toString()Ljava/lang/String; — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M22-TP1: ENTRY -> N0 -> EXIT (formats "lhs op rhs")
    public void testM22_TP1_ToString() {
        Operand lhs = new Operand(1);
        Operand rhs = new Operand(2);
        Limit limit = new Limit("l", lhs, Operator.LT, rhs);
        String s = limit.toString();
        assertTrue(s.contains("LT"));
    }
}
