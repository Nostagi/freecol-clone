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

import net.sf.freecol.common.io.FreeColXMLReader;
import net.sf.freecol.common.io.FreeColXMLWriter;
import net.sf.freecol.util.test.FreeColTestCase;
import static net.sf.freecol.common.model.TradeItem.INVALID_TRADE_ITEM;


/**
 * CFG-driven test suite for {@link ColonyTradeItem} derived from
 * {@code test_cft/graph/net/sf/freecol/common/model/ColonyTradeItem.json}.
 *
 * Covers all 16 methods (M1..M16) and achieves 100% feasible C3 (node + edge)
 * coverage across 31 Test Paths.
 *
 * Infeasible edges documented:
 * - M10 ({@code copyIn}): {@code N1 -> N2} ({@code B2:false}) — once
 *   {@code o = copyInCast(other, ColonyTradeItem.class)} is non-null, {@code o}
 *   is guaranteed to be a non-null {@code TradeItem} with matching id, so
 *   {@code super.copyIn(o)} always returns {@code true}.
 */
public class ColonyTradeItemCfgTest extends FreeColTestCase {

    private Game game;
    private Player dutch;
    private Player french;
    private Player english;
    private Player inca;
    private Colony dutchColony;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        // Pre-condition: Standard game + map with a Dutch colony and European/native players
        game = getStandardGame();
        game.setMap(getTestMap(true));
        dutchColony = createStandardColony(1);
        dutch = game.getPlayerByNationId("model.nation.dutch");
        french = game.getPlayerByNationId("model.nation.french");
        english = game.getPlayerByNationId("model.nation.english");
        inca = game.getPlayerByNationId("model.nation.inca");
    }

    // ------------------------------------------------------------------
    // Pre-condition helpers & stubs
    // ------------------------------------------------------------------

    private String serializeItem(ColonyTradeItem item) throws Exception {
        StringWriter sw = new StringWriter();
        try (FreeColXMLWriter xw = new FreeColXMLWriter(sw,
                FreeColXMLWriter.WriteScope.toSave(), false)) {
            item.toXML(xw);
            xw.flush();
        }
        return sw.toString();
    }

    private ColonyTradeItem deserializeItem(String xml) throws Exception {
        try (FreeColXMLReader xr = new FreeColXMLReader(new StringReader(xml))) {
            xr.nextTag();
            return new ColonyTradeItem(game, xr);
        }
    }

    // ------------------------------------------------------------------
    // M1: <init>(Game, Player, Player, Colony)V — 4 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M1-TP1: ENTRY -> N0 -(B1:false)-> N1 -> EXIT (colony.getOwner() != source)
    public void testM1_TP1_ConstructorBadSourceThrows() {
        try {
            new ColonyTradeItem(game, french, english, dutchColony);
            fail("Expected IllegalArgumentException for bad source");
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage().contains("Bad source"));
        }
    }

    // M1-TP2: ENTRY -> N0 -(B1:true)-> N2 -(B2:true)-> N4 -> EXIT (destination == null)
    public void testM1_TP2_ConstructorNullDestinationThrows() {
        try {
            new ColonyTradeItem(game, dutch, null, dutchColony);
            fail("Expected IllegalArgumentException for null destination");
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage().contains("Bad destination"));
        }
    }

    // M1-TP3: ENTRY -> N0 -(B1:true)-> N2 -(B2:false)-> N3 -(B3:false)-> N4 -> EXIT (!destination.isEuropean())
    public void testM1_TP3_ConstructorNonEuropeanDestinationThrows() {
        assertFalse(inca.isEuropean());
        try {
            new ColonyTradeItem(game, dutch, inca, dutchColony);
            fail("Expected IllegalArgumentException for non-European destination");
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage().contains("Bad destination"));
        }
    }

    // M1-TP4: ENTRY -> N0 -(B1:true)-> N2 -(B2:false)-> N3 -(B3:true)-> N5 -> EXIT (valid arguments)
    public void testM1_TP4_ConstructorValid() {
        ColonyTradeItem item = new ColonyTradeItem(game, dutch, french, dutchColony);
        assertSame(dutch, item.getSource());
        assertSame(french, item.getDestination());
        assertEquals(dutchColony.getId(), item.getColonyId());
        assertEquals(dutchColony.getName(), item.getColonyName());
    }

    // ------------------------------------------------------------------
    // M2: <init>(Game, FreeColXMLReader)V — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M2-TP1: ENTRY -> N0 -> EXIT (construct ColonyTradeItem from FreeColXMLReader)
    public void testM2_TP1_ConstructorFromXMLReader() throws Exception {
        ColonyTradeItem original = new ColonyTradeItem(game, dutch, french, dutchColony);
        ColonyTradeItem parsed = deserializeItem(serializeItem(original));
        assertEquals(dutchColony.getId(), parsed.getColonyId());
        assertEquals(dutchColony.getName(), parsed.getColonyName());
    }

    // ------------------------------------------------------------------
    // M3: getColonyId()String — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M3-TP1: ENTRY -> N0 -> EXIT (returns colonyId)
    public void testM3_TP1_GetColonyId() {
        ColonyTradeItem item = new ColonyTradeItem(game, dutch, french, dutchColony);
        assertEquals(dutchColony.getId(), item.getColonyId());
    }

    // ------------------------------------------------------------------
    // M4: getColonyName()String — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M4-TP1: ENTRY -> N0 -> EXIT (returns colonyName)
    public void testM4_TP1_GetColonyName() {
        ColonyTradeItem item = new ColonyTradeItem(game, dutch, french, dutchColony);
        assertEquals(dutchColony.getName(), item.getColonyName());
    }

    // ------------------------------------------------------------------
    // M5: isValid()Z — 2 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M5-TP1: ENTRY -> N0 -(B1:true)-> N2 -> N3 -> EXIT (colonyId == null)
    public void testM5_TP1_IsValidNullColonyId() throws Exception {
        String xml = "<colonyTradeItem id=\"model.tradeItem.colony\" "
            + "source=\"" + dutch.getId() + "\" "
            + "destination=\"" + french.getId() + "\"/>";
        ColonyTradeItem item = deserializeItem(xml);
        assertFalse(item.isValid());
    }

    // M5-TP2: ENTRY -> N0 -(B1:false)-> N1 -> N3 -> EXIT (colonyId != null)
    public void testM5_TP2_IsValidNonNullColonyId() {
        ColonyTradeItem item = new ColonyTradeItem(game, dutch, french, dutchColony);
        assertTrue(item.isValid());
    }

    // ------------------------------------------------------------------
    // M6: isUnique()Z — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M6-TP1: ENTRY -> N0 -> EXIT (returns false)
    public void testM6_TP1_IsUniqueReturnsFalse() {
        ColonyTradeItem item = new ColonyTradeItem(game, dutch, french, dutchColony);
        assertFalse(item.isUnique());
    }

    // ------------------------------------------------------------------
    // M7: getLabel()StringTemplate — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M7-TP1: ENTRY -> N0 -> EXIT (builds StringTemplate with %colony%)
    public void testM7_TP1_GetLabel() {
        ColonyTradeItem item = new ColonyTradeItem(game, dutch, french, dutchColony);
        StringTemplate label = item.getLabel();
        assertNotNull(label);
        assertEquals("model.tradeItem.colony.description", label.getId());
    }

    // ------------------------------------------------------------------
    // M8: getColony(Game)Colony — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M8-TP1: ENTRY -> N0 -> EXIT (looks up colony in Game by colonyId)
    public void testM8_TP1_GetColony() {
        ColonyTradeItem item = new ColonyTradeItem(game, dutch, french, dutchColony);
        assertSame(dutchColony, item.getColony(game));
    }

    // ------------------------------------------------------------------
    // M9: evaluateFor(Player)I — 6 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M9-TP1: ENTRY -> N0 -(B1:true)-> N5 -> EXIT (colony == null)
    public void testM9_TP1_EvaluateForMissingColony() throws Exception {
        String xml = "<colonyTradeItem id=\"model.tradeItem.colony\" "
            + "source=\"" + dutch.getId() + "\" "
            + "destination=\"" + french.getId() + "\" "
            + "colony=\"colony:nonexistent\" colonyName=\"Ghost\"/>";
        ColonyTradeItem item = deserializeItem(xml);
        assertEquals(INVALID_TRADE_ITEM, item.evaluateFor(dutch));
    }

    // M9-TP2: ENTRY -> N0 -(B1:false)-> N1 -(B2:false)-> N2 -> N4 -(B3:false)-> N5 -> EXIT
    // (getSource() == player && !player.owns(colony))
    public void testM9_TP2_EvaluateForSourceNoLongerOwnsColony() {
        ColonyTradeItem item = new ColonyTradeItem(game, dutch, french, dutchColony);
        item.setSource(french);
        assertEquals(INVALID_TRADE_ITEM, item.evaluateFor(french));
    }

    // M9-TP3: ENTRY -> N0 -(B1:false)-> N1 -(B2:true)-> N3 -> N4 -(B3:false)-> N5 -> EXIT
    // (getSource() != player && player.owns(colony))
    public void testM9_TP3_EvaluateForNonSourceOwnsColony() {
        ColonyTradeItem item = new ColonyTradeItem(game, dutch, french, dutchColony);
        item.setSource(french);
        assertEquals(INVALID_TRADE_ITEM, item.evaluateFor(dutch));
    }

    // M9-TP4: ENTRY -> N0 -(B1:false)-> N1 -(B2:false)-> N2 -> N4 -(B3:true)-> N6 -(B4:false)-> N7 -> EXIT
    // (colony.evaluateFor(player) == Integer.MIN_VALUE when AI player has < TRADE_MARGIN settlements)
    public void testM9_TP4_EvaluateForMinValueReturnsInvalid() {
        dutch.setAI(true);
        ColonyTradeItem item = new ColonyTradeItem(game, dutch, french, dutchColony);
        assertEquals(INVALID_TRADE_ITEM, item.evaluateFor(dutch));
    }

    // M9-TP5: ENTRY -> N0 -(B1:false)-> N1 -(B2:false)-> N2 -> N4 -(B3:true)-> N6 -(B4:true)-> N8 -(B5:false)-> N9 -> N11 -> EXIT
    // (player.owns(colony) -> -value)
    public void testM9_TP5_EvaluateForOwnerReturnsNegativeValue() {
        dutch.setAI(false);
        ColonyTradeItem item = new ColonyTradeItem(game, dutch, french, dutchColony);
        int expected = -dutchColony.evaluateFor(dutch);
        assertEquals(expected, item.evaluateFor(dutch));
    }

    // M9-TP6: ENTRY -> N0 -(B1:false)-> N1 -(B2:true)-> N3 -> N4 -(B3:true)-> N6 -(B4:true)-> N8 -(B5:true)-> N10 -> N11 -> EXIT
    // (!player.owns(colony) -> +value)
    public void testM9_TP6_EvaluateForRecipientReturnsPositiveValue() {
        ColonyTradeItem item = new ColonyTradeItem(game, dutch, french, dutchColony);
        int expected = dutchColony.evaluateFor(french);
        assertEquals(expected, item.evaluateFor(french));
    }

    // ------------------------------------------------------------------
    // M10: copyIn(FreeColObject)Z — 2 feasible Test Paths (100% feasible C3)
    // Infeasible: N1 -(B2:false)-> N2 (super.copyIn(o) always true when o != null)
    // ------------------------------------------------------------------

    // M10-TP1: ENTRY -> N0 -(B1:true)-> N2 -> EXIT (copyInCast returns null)
    public void testM10_TP1_CopyInNullOrWrongTypeReturnsFalse() {
        ColonyTradeItem target = new ColonyTradeItem(game, dutch, french, dutchColony);
        GoldTradeItem wrongType = new GoldTradeItem(game, dutch, french, 50);
        assertFalse(target.copyIn(null));
        assertFalse(target.copyIn(wrongType));
    }

    // M10-TP2: ENTRY -> N0 -(B1:false)-> N1 -(B2:true)-> N3 -> EXIT (valid ColonyTradeItem)
    public void testM10_TP2_CopyInValidColonyTradeItem() {
        ColonyTradeItem target = new ColonyTradeItem(game, dutch, french, dutchColony);
        ColonyTradeItem other = new ColonyTradeItem(game, dutch, english, dutchColony);
        assertTrue(target.copyIn(other));
        assertSame(english, target.getDestination());
        assertEquals(dutchColony.getId(), target.getColonyId());
        assertEquals(dutchColony.getName(), target.getColonyName());
    }

    // ------------------------------------------------------------------
    // M11: writeAttributes(FreeColXMLWriter)V — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M11-TP1: ENTRY -> N0 -> EXIT (writes colony and colonyName attributes)
    public void testM11_TP1_WriteAttributes() throws Exception {
        ColonyTradeItem item = new ColonyTradeItem(game, dutch, french, dutchColony);
        String xml = serializeItem(item);
        assertTrue(xml.contains("colony=\"" + dutchColony.getId() + "\""));
        assertTrue(xml.contains("colonyName=\"" + dutchColony.getName() + "\""));
    }

    // ------------------------------------------------------------------
    // M12: readAttributes(FreeColXMLReader)V — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M12-TP1: ENTRY -> N0 -> EXIT (reads colony and colonyName attributes)
    public void testM12_TP1_ReadAttributes() throws Exception {
        ColonyTradeItem item = new ColonyTradeItem(game, dutch, french, dutchColony);
        ColonyTradeItem parsed = deserializeItem(serializeItem(item));
        assertEquals(dutchColony.getId(), parsed.getColonyId());
        assertEquals(dutchColony.getName(), parsed.getColonyName());
    }

    // ------------------------------------------------------------------
    // M13: getXMLTagName()String — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M13-TP1: ENTRY -> N0 -> EXIT (returns "colonyTradeItem")
    public void testM13_TP1_GetXMLTagName() {
        ColonyTradeItem item = new ColonyTradeItem(game, dutch, french, dutchColony);
        assertEquals(ColonyTradeItem.TAG, item.getXMLTagName());
        assertEquals("colonyTradeItem", item.getXMLTagName());
    }

    // ------------------------------------------------------------------
    // M14: equals(Object)Z — 5 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M14-TP1: ENTRY -> N0 -(B1:false)-> N1 -> EXIT (!(o instanceof ColonyTradeItem))
    public void testM14_TP1_EqualsNonColonyTradeItem() {
        ColonyTradeItem item = new ColonyTradeItem(game, dutch, french, dutchColony);
        assertFalse(item.equals(null));
        assertFalse(item.equals("other"));
    }

    // M14-TP2: ENTRY -> N0 -(B1:true)-> N2 -(B2:true)-> N6 -> N7 -> EXIT (different colonyId)
    public void testM14_TP2_EqualsDifferentColonyId() throws Exception {
        ColonyTradeItem a = new ColonyTradeItem(game, dutch, french, dutchColony);
        String xml = "<colonyTradeItem id=\"" + a.getId() + "\" "
            + "source=\"" + dutch.getId() + "\" "
            + "destination=\"" + french.getId() + "\" "
            + "colony=\"colony:other\" colonyName=\"" + dutchColony.getName() + "\"/>";
        ColonyTradeItem b = deserializeItem(xml);
        assertFalse(a.equals(b));
    }

    // M14-TP3: ENTRY -> N0 -(B1:true)-> N2 -(B2:false)-> N3 -(B3:true)-> N6 -> N7 -> EXIT (different colonyName)
    public void testM14_TP3_EqualsDifferentColonyName() throws Exception {
        ColonyTradeItem a = new ColonyTradeItem(game, dutch, french, dutchColony);
        String xml = "<colonyTradeItem id=\"" + a.getId() + "\" "
            + "source=\"" + dutch.getId() + "\" "
            + "destination=\"" + french.getId() + "\" "
            + "colony=\"" + dutchColony.getId() + "\" colonyName=\"DifferentName\"/>";
        ColonyTradeItem b = deserializeItem(xml);
        assertFalse(a.equals(b));
    }

    // M14-TP4: ENTRY -> N0 -(B1:true)-> N2 -(B2:false)-> N3 -(B3:false)-> N4 -(B4:true)-> N6 -> N7 -> EXIT (!super.equals)
    public void testM14_TP4_EqualsDifferentSuper() {
        ColonyTradeItem a = new ColonyTradeItem(game, dutch, french, dutchColony);
        ColonyTradeItem b = new ColonyTradeItem(game, dutch, english, dutchColony);
        assertFalse(a.equals(b));
    }

    // M14-TP5: ENTRY -> N0 -(B1:true)-> N2 -(B2:false)-> N3 -(B3:false)-> N4 -(B4:false)-> N5 -> N7 -> EXIT (equal items)
    public void testM14_TP5_EqualsEqualItems() {
        ColonyTradeItem a = new ColonyTradeItem(game, dutch, french, dutchColony);
        ColonyTradeItem b = new ColonyTradeItem(game, dutch, french, dutchColony);
        assertTrue(a.equals(b));
    }

    // ------------------------------------------------------------------
    // M15: hashCode()I — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M15-TP1: ENTRY -> N0 -> EXIT (computes hashCode consistent with equals)
    public void testM15_TP1_HashCode() {
        ColonyTradeItem a = new ColonyTradeItem(game, dutch, french, dutchColony);
        ColonyTradeItem b = new ColonyTradeItem(game, dutch, french, dutchColony);
        assertEquals(a.hashCode(), b.hashCode());
    }

    // ------------------------------------------------------------------
    // M16: toString()String — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M16-TP1: ENTRY -> N0 -> EXIT (formats "[<id> <colonyName>]")
    public void testM16_TP1_ToString() {
        ColonyTradeItem item = new ColonyTradeItem(game, dutch, french, dutchColony);
        assertEquals("[" + item.getId() + " " + dutchColony.getName() + "]", item.toString());
    }
}
