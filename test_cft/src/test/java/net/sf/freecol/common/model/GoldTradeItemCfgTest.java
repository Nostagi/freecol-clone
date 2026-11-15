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
import javax.xml.stream.XMLStreamException;

import net.sf.freecol.common.io.FreeColXMLReader;
import net.sf.freecol.common.io.FreeColXMLWriter;
import net.sf.freecol.util.test.FreeColTestCase;
import static net.sf.freecol.common.model.TradeItem.INVALID_TRADE_ITEM;


/**
 * CFG-driven test suite for {@link GoldTradeItem} derived from
 * {@code test_cft/graph/net/sf/freecol/common/model/GoldTradeItem.json}.
 *
 * Covers all 15 methods (M1..M15) and achieves 100% feasible C3 (node + edge)
 * coverage across 23 Test Paths.
 *
 * Infeasible edges documented:
 * - M9 ({@code copyIn}): {@code N1 -> N2} ({@code B2:false}) — once
 *   {@code o = copyInCast(other, ColonyTradeItem.class)} is non-null, {@code o}
 *   is guaranteed to be a non-null {@code TradeItem} with matching id, so
 *   {@code super.copyIn(o)} always returns {@code true}.
 */
public class GoldTradeItemCfgTest extends FreeColTestCase {

    private Game game;
    private Player dutch;
    private Player french;
    private Player english;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        // Pre-condition: Standard game with European players and initial gold
        game = getStandardGame();
        dutch = game.getPlayerByNationId("model.nation.dutch");
        french = game.getPlayerByNationId("model.nation.french");
        english = game.getPlayerByNationId("model.nation.english");
        dutch.setGold(500);
        french.setGold(500);
    }

    // ------------------------------------------------------------------
    // Pre-condition helpers & stubs
    // ------------------------------------------------------------------

    private String serializeItem(GoldTradeItem item) throws Exception {
        StringWriter sw = new StringWriter();
        try (FreeColXMLWriter xw = new FreeColXMLWriter(sw,
                FreeColXMLWriter.WriteScope.toSave(), false)) {
            item.toXML(xw);
            xw.flush();
        }
        return sw.toString();
    }

    private GoldTradeItem deserializeItem(String xml) throws Exception {
        try (FreeColXMLReader xr = new FreeColXMLReader(new StringReader(xml))) {
            xr.nextTag();
            return new GoldTradeItem(game, xr);
        }
    }

    // ------------------------------------------------------------------
    // M1: <init>(Game, Player, Player, int)V — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M1-TP1: ENTRY -> N0 -> EXIT (construct GoldTradeItem with game, source, destination, gold)
    public void testM1_TP1_ConstructorWithParameters() {
        GoldTradeItem item = new GoldTradeItem(game, dutch, french, 250);
        assertSame(dutch, item.getSource());
        assertSame(french, item.getDestination());
        assertEquals(250, item.getGold());
    }

    // ------------------------------------------------------------------
    // M2: <init>(Game, FreeColXMLReader)V — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M2-TP1: ENTRY -> N0 -> EXIT (construct GoldTradeItem from FreeColXMLReader)
    public void testM2_TP1_ConstructorFromXMLReader() throws Exception {
        GoldTradeItem original = new GoldTradeItem(game, dutch, french, 320);
        String xml = serializeItem(original);
        GoldTradeItem parsed = deserializeItem(xml);
        assertEquals(320, parsed.getGold());
        assertSame(dutch, parsed.getSource());
        assertSame(french, parsed.getDestination());
    }

    // ------------------------------------------------------------------
    // M3: isValid()Z — 3 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M3-TP1: ENTRY -> N0 -(B1:true)-> N3 -> N4 -> EXIT (gold < 0)
    public void testM3_TP1_IsValidNegativeGold() {
        GoldTradeItem item = new GoldTradeItem(game, dutch, french, -1);
        assertFalse(item.isValid());
    }

    // M3-TP2: ENTRY -> N0 -(B1:false)-> N1 -(B2:true)-> N3 -> N4 -> EXIT (gold >= 0 && !source.checkGold(gold))
    public void testM3_TP2_IsValidInsufficientSourceGold() {
        GoldTradeItem item = new GoldTradeItem(game, dutch, french, 1000);
        assertFalse(item.isValid());
    }

    // M3-TP3: ENTRY -> N0 -(B1:false)-> N1 -(B2:false)-> N2 -> N4 -> EXIT (gold >= 0 && source.checkGold(gold))
    public void testM3_TP3_IsValidSufficientSourceGold() {
        GoldTradeItem item = new GoldTradeItem(game, dutch, french, 500);
        assertTrue(item.isValid());
    }

    // ------------------------------------------------------------------
    // M4: isUnique()Z — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M4-TP1: ENTRY -> N0 -> EXIT (returns true)
    public void testM4_TP1_IsUniqueReturnsTrue() {
        GoldTradeItem item = new GoldTradeItem(game, dutch, french, 100);
        assertTrue(item.isUnique());
    }

    // ------------------------------------------------------------------
    // M5: getLabel()StringTemplate — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M5-TP1: ENTRY -> N0 -> EXIT (builds StringTemplate with %amount%)
    public void testM5_TP1_GetLabel() {
        GoldTradeItem item = new GoldTradeItem(game, dutch, french, 150);
        StringTemplate label = item.getLabel();
        assertNotNull(label);
        assertEquals("model.tradeItem.gold.description", label.getId());
    }

    // ------------------------------------------------------------------
    // M6: getGold()I — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M6-TP1: ENTRY -> N0 -> EXIT (returns gold field)
    public void testM6_TP1_GetGold() {
        GoldTradeItem item = new GoldTradeItem(game, dutch, french, 42);
        assertEquals(42, item.getGold());
    }

    // ------------------------------------------------------------------
    // M7: setGold(int)V — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M7-TP1: ENTRY -> N0 -> EXIT (updates gold field)
    public void testM7_TP1_SetGold() {
        GoldTradeItem item = new GoldTradeItem(game, dutch, french, 10);
        item.setGold(99);
        assertEquals(99, item.getGold());
    }

    // ------------------------------------------------------------------
    // M8: evaluateFor(Player)I — 3 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M8-TP1: ENTRY -> N0 -(B1:false)-> N1 -> N5 -> EXIT (!isValid() -> INVALID_TRADE_ITEM)
    public void testM8_TP1_EvaluateForInvalidItem() {
        GoldTradeItem item = new GoldTradeItem(game, dutch, french, -5);
        assertEquals(INVALID_TRADE_ITEM, item.evaluateFor(dutch));
    }

    // M8-TP2: ENTRY -> N0 -(B1:true)-> N2 -(B2:false)-> N3 -> N5 -> EXIT (isValid() && getSource() == player -> -gold)
    public void testM8_TP2_EvaluateForSourcePlayer() {
        GoldTradeItem item = new GoldTradeItem(game, dutch, french, 200);
        assertEquals(-200, item.evaluateFor(dutch));
    }

    // M8-TP3: ENTRY -> N0 -(B1:true)-> N2 -(B2:true)-> N4 -> N5 -> EXIT (isValid() && getSource() != player -> +gold)
    public void testM8_TP3_EvaluateForOtherPlayer() {
        GoldTradeItem item = new GoldTradeItem(game, dutch, french, 200);
        assertEquals(200, item.evaluateFor(french));
    }

    // ------------------------------------------------------------------
    // M9: copyIn(FreeColObject)Z — 2 feasible Test Paths (100% feasible C3)
    // Infeasible: N1 -(B2:false)-> N2 (super.copyIn(o) always true when o != null)
    // ------------------------------------------------------------------

    // M9-TP1: ENTRY -> N0 -(B1:true)-> N2 -> EXIT (copyInCast returns null)
    public void testM9_TP1_CopyInNullOrWrongTypeReturnsFalse() {
        GoldTradeItem target = new GoldTradeItem(game, dutch, french, 100);
        GoldTradeItem other = new GoldTradeItem(game, french, english, 250);
        assertFalse(target.copyIn(null));
        assertFalse(target.copyIn(other));
        assertEquals(100, target.getGold());
    }

    // M9-TP2: ENTRY -> N0 -(B1:false)-> N1 -(B2:true)-> N3 -> EXIT (copyInCast to ColonyTradeItem with matching id)
    public void testM9_TP2_CopyInColonyTradeItemWithMatchingId() {
        game.setMap(getTestMap(true));
        Colony colony = createStandardColony(1);
        GoldTradeItem target = new GoldTradeItem(game, dutch, french, 100);
        ColonyTradeItem other = new ColonyTradeItem(game, dutch, english, colony);
        other.setId(target.getId());
        assertTrue(target.copyIn(other));
        assertSame(english, target.getDestination());
        assertEquals(0, target.getGold());
    }

    // ------------------------------------------------------------------
    // M10: writeAttributes(FreeColXMLWriter)V — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M10-TP1: ENTRY -> N0 -> EXIT (writes gold attribute via XML serialization)
    public void testM10_TP1_WriteAttributes() throws Exception {
        GoldTradeItem item = new GoldTradeItem(game, dutch, french, 123);
        String xml = serializeItem(item);
        assertTrue(xml.contains("gold=\"123\""));
    }

    // ------------------------------------------------------------------
    // M11: readAttributes(FreeColXMLReader)V — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M11-TP1: ENTRY -> N0 -> EXIT (reads gold attribute from XML)
    public void testM11_TP1_ReadAttributes() throws Exception {
        GoldTradeItem item = new GoldTradeItem(game, dutch, french, 456);
        GoldTradeItem parsed = deserializeItem(serializeItem(item));
        assertEquals(456, parsed.getGold());
    }

    // ------------------------------------------------------------------
    // M12: getXMLTagName()String — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M12-TP1: ENTRY -> N0 -> EXIT (returns "goldTradeItem")
    public void testM12_TP1_GetXMLTagName() {
        GoldTradeItem item = new GoldTradeItem(game, dutch, french, 50);
        assertEquals(GoldTradeItem.TAG, item.getXMLTagName());
        assertEquals("goldTradeItem", item.getXMLTagName());
    }

    // ------------------------------------------------------------------
    // M13: equals(Object)Z — 4 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M13-TP1: ENTRY -> N0 -(B1:true)-> N6 -> EXIT (!(other instanceof GoldTradeItem))
    public void testM13_TP1_EqualsNonGoldTradeItem() {
        GoldTradeItem item = new GoldTradeItem(game, dutch, french, 50);
        assertFalse(item.equals(null));
        assertFalse(item.equals("not a trade item"));
    }

    // M13-TP2: ENTRY -> N0 -(B1:false)-> N1 -(B2:true)-> N4 -> N5 -> EXIT (different gold)
    public void testM13_TP2_EqualsDifferentGold() {
        GoldTradeItem a = new GoldTradeItem(game, dutch, french, 50);
        GoldTradeItem b = new GoldTradeItem(game, dutch, french, 60);
        assertFalse(a.equals(b));
    }

    // M13-TP3: ENTRY -> N0 -(B1:false)-> N1 -(B2:false)-> N2 -(B3:true)-> N4 -> N5 -> EXIT (same gold, !super.equals)
    public void testM13_TP3_EqualsSameGoldDifferentSuper() {
        GoldTradeItem a = new GoldTradeItem(game, dutch, french, 50);
        GoldTradeItem b = new GoldTradeItem(game, dutch, english, 50);
        assertFalse(a.equals(b));
    }

    // M13-TP4: ENTRY -> N0 -(B1:false)-> N1 -(B2:false)-> N2 -(B3:false)-> N3 -> N5 -> EXIT (same gold && super.equals)
    public void testM13_TP4_EqualsEqualItems() {
        GoldTradeItem a = new GoldTradeItem(game, dutch, french, 50);
        GoldTradeItem b = new GoldTradeItem(game, dutch, french, 50);
        assertTrue(a.equals(b));
    }

    // ------------------------------------------------------------------
    // M14: hashCode()I — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M14-TP1: ENTRY -> N0 -> EXIT (computes hashCode consistent with equals)
    public void testM14_TP1_HashCode() {
        GoldTradeItem a = new GoldTradeItem(game, dutch, french, 50);
        GoldTradeItem b = new GoldTradeItem(game, dutch, french, 50);
        assertEquals(a.hashCode(), b.hashCode());
    }

    // ------------------------------------------------------------------
    // M15: toString()String — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M15-TP1: ENTRY -> N0 -> EXIT (formats "[<id> <gold>]")
    public void testM15_TP1_ToString() {
        GoldTradeItem item = new GoldTradeItem(game, dutch, french, 75);
        assertEquals("[" + item.getId() + " 75]", item.toString());
    }
}
