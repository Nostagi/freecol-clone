package net.sf.freecol.common.model;

import java.io.StringReader;
import java.io.StringWriter;

import net.sf.freecol.common.io.FreeColXMLReader;
import net.sf.freecol.common.io.FreeColXMLWriter;
import net.sf.freecol.util.test.FreeColTestCase;

/**
 * CFG-driven test suite for {@link InciteTradeItem} derived from
 * {@code test_cft/graph/net/sf/freecol/common/model/InciteTradeItem.json}.
 *
 * Covers all 14 methods (M1..M14) and achieves 100% feasible C3 (node + edge)
 * coverage across 23 Test Paths.
 *
 * Infeasible edges documented:
 * - M8 ({@code copyIn}): {@code N1 -> N2} ({@code B2:false}) — {@code super.copyIn(o)}
 *   delegates through {@link TradeItem#copyIn(FreeColObject)} to
 *   {@link FreeColObject#copyIn(FreeColObject)}, which only returns {@code false} when
 *   {@code o == null}; once {@code o != null} at {@code N1}, {@code super.copyIn(o)}
 *   always returns {@code true}.
 */
public class InciteTradeItemCfgTest extends FreeColTestCase {

    private Game game;
    private Player dutch;
    private Player french;
    private Player english;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        // Pre-condition: standard game with three European players (Dutch, French, English)
        game = getStandardGame();
        dutch = game.getPlayerByNationId("model.nation.dutch");
        french = game.getPlayerByNationId("model.nation.french");
        english = game.getPlayerByNationId("model.nation.english");
    }

    // ------------------------------------------------------------------
    // Pre-condition helpers & stubs
    // ------------------------------------------------------------------

    private String writeTradeItemXml(InciteTradeItem item) throws Exception {
        StringWriter sw = new StringWriter();
        try (FreeColXMLWriter xw = new FreeColXMLWriter(sw,
                FreeColXMLWriter.WriteScope.toSave(), false)) {
            item.toXML(xw);
            xw.flush();
        }
        return sw.toString();
    }

    private FreeColXMLReader openReaderAtTag(String xml) throws Exception {
        FreeColXMLReader xr = new FreeColXMLReader(new StringReader(xml));
        xr.nextTag();
        return xr;
    }

    // ------------------------------------------------------------------
    // M1: <init>(Game, Player, Player, Player) — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M1-TP1: ENTRY -> N0 -> EXIT (constructor with game, source, destination, victim)
    public void testM1_TP1_Constructor() {
        InciteTradeItem item = new InciteTradeItem(game, dutch, french, english);
        assertSame(dutch, item.getSource());
        assertSame(french, item.getDestination());
        assertSame(english, item.getVictim());
    }

    // ------------------------------------------------------------------
    // M2: <init>(Game, FreeColXMLReader) — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M2-TP1: ENTRY -> N0 -> EXIT (XML reader constructor)
    public void testM2_TP1_XmlReaderConstructor() throws Exception {
        InciteTradeItem original = new InciteTradeItem(game, dutch, french, english);
        String xml = writeTradeItemXml(original);
        try (FreeColXMLReader xr = openReaderAtTag(xml)) {
            InciteTradeItem loaded = new InciteTradeItem(game, xr);
            assertSame(dutch, loaded.getSource());
            assertSame(french, loaded.getDestination());
            assertSame(english, loaded.getVictim());
        }
    }

    // ------------------------------------------------------------------
    // M3: isValid() — 4 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M3-TP1: ENTRY -> N0 -(B1:true)-> N4 -> N5 -> EXIT (victim == null -> false)
    public void testM3_TP1_IsValidNullVictim() {
        InciteTradeItem item = new InciteTradeItem(game, dutch, french, null);
        assertFalse(item.isValid());
    }

    // M3-TP2: ENTRY -> N0 -(B1:false)-> N1 -(B2:true)-> N4 -> N5 -> EXIT (victim == source -> false)
    public void testM3_TP2_IsValidVictimEqualsSource() {
        InciteTradeItem item = new InciteTradeItem(game, dutch, french, dutch);
        assertFalse(item.isValid());
    }

    // M3-TP3: ENTRY -> N0 -(B1:false)-> N1 -(B2:false)-> N2 -(B3:true)-> N4 -> N5 -> EXIT (victim == destination -> false)
    public void testM3_TP3_IsValidVictimEqualsDestination() {
        InciteTradeItem item = new InciteTradeItem(game, dutch, french, french);
        assertFalse(item.isValid());
    }

    // M3-TP4: ENTRY -> N0 -(B1:false)-> N1 -(B2:false)-> N2 -(B3:false)-> N3 -> N5 -> EXIT (distinct non-null victim -> true)
    public void testM3_TP4_IsValidTrue() {
        InciteTradeItem item = new InciteTradeItem(game, dutch, french, english);
        assertTrue(item.isValid());
    }

    // ------------------------------------------------------------------
    // M4: isUnique() — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M4-TP1: ENTRY -> N0 -> EXIT (always returns true)
    public void testM4_TP1_IsUnique() {
        InciteTradeItem item = new InciteTradeItem(game, dutch, french, english);
        assertTrue(item.isUnique());
    }

    // ------------------------------------------------------------------
    // M5: getLabel() — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M5-TP1: ENTRY -> N0 -> EXIT (returns StringTemplate with victim nation label)
    public void testM5_TP1_GetLabel() {
        InciteTradeItem item = new InciteTradeItem(game, dutch, french, english);
        StringTemplate label = item.getLabel();
        assertNotNull(label);
        assertEquals("model.tradeItem.incite.description", label.getId());
    }

    // ------------------------------------------------------------------
    // M6: getVictim() — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M6-TP1: ENTRY -> N0 -> EXIT (returns victim player)
    public void testM6_TP1_GetVictim() {
        InciteTradeItem item = new InciteTradeItem(game, dutch, french, english);
        assertSame(english, item.getVictim());
    }

    // ------------------------------------------------------------------
    // M7: evaluateFor(Player) — 3 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M7-TP1: ENTRY -> N0 -(B1:case:1)-> N1 -> EXIT (Stance.ALLIANCE -> INVALID_TRADE_ITEM)
    public void testM7_TP1_EvaluateForAlliance() {
        french.setStance(english, Stance.ALLIANCE);
        InciteTradeItem item = new InciteTradeItem(game, dutch, french, english);
        assertEquals(TradeItem.INVALID_TRADE_ITEM, item.evaluateFor(french));
    }

    // M7-TP2: ENTRY -> N0 -(B1:case:2)-> N2 -> EXIT (Stance.WAR -> 0)
    public void testM7_TP2_EvaluateForWar() {
        french.setStance(english, Stance.WAR);
        InciteTradeItem item = new InciteTradeItem(game, dutch, french, english);
        assertEquals(0, item.evaluateFor(french));
    }

    // M7-TP3: ENTRY -> N0 -(B1:default)-> N3 -> EXIT (Stance.PEACE -> strength ratio formula)
    public void testM7_TP3_EvaluateForPeace() {
        french.setStance(english, Stance.PEACE);
        InciteTradeItem item = new InciteTradeItem(game, dutch, french, english);
        int expected = -(int) Math.round(50.0 / french.getStrengthRatio(english, false));
        assertEquals(expected, item.evaluateFor(french));
    }

    // ------------------------------------------------------------------
    // M8: copyIn(FreeColObject) — 2 Test Paths (100% feasible C3)
    // ------------------------------------------------------------------

    // M8-TP1: ENTRY -> N0 -(B1:true)-> N2 -> EXIT (o == null because other is null or wrong type)
    public void testM8_TP1_CopyInNullOrWrongType() {
        InciteTradeItem target = new InciteTradeItem(game, dutch, french, english);
        assertFalse(target.copyIn((InciteTradeItem) null));
        assertFalse(target.copyIn(dutch));
        assertSame(english, target.getVictim());
    }

    // M8-TP2: ENTRY -> N0 -(B1:false)-> N1 -(B2:true)-> N3 -> EXIT (valid InciteTradeItem copied)
    public void testM8_TP2_CopyInSuccess() {
        InciteTradeItem target = new InciteTradeItem(game, dutch, french, english);
        InciteTradeItem source = new InciteTradeItem(game, french, english, dutch);
        assertTrue(target.copyIn(source));
        assertSame(french, target.getSource());
        assertSame(english, target.getDestination());
        assertSame(dutch, target.getVictim());
    }

    // ------------------------------------------------------------------
    // M9: writeAttributes(FreeColXMLWriter) — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M9-TP1: ENTRY -> N0 -> EXIT (writes source, destination, and victim attributes)
    public void testM9_TP1_WriteAttributes() throws Exception {
        InciteTradeItem item = new InciteTradeItem(game, dutch, french, english);
        String xml = writeTradeItemXml(item);
        assertTrue(xml.contains("victim=\"" + english.getId() + "\""));
    }

    // ------------------------------------------------------------------
    // M10: readAttributes(FreeColXMLReader) — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M10-TP1: ENTRY -> N0 -> EXIT (reads source, destination, and victim attributes)
    public void testM10_TP1_ReadAttributes() throws Exception {
        InciteTradeItem original = new InciteTradeItem(game, dutch, french, english);
        String xml = writeTradeItemXml(original);
        InciteTradeItem target = new InciteTradeItem(game, dutch, dutch, null);
        try (FreeColXMLReader xr = openReaderAtTag(xml)) {
            target.readAttributes(xr);
        }
        assertSame(english, target.getVictim());
    }

    // ------------------------------------------------------------------
    // M11: getXMLTagName() — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M11-TP1: ENTRY -> N0 -> EXIT (returns "inciteTradeItem")
    public void testM11_TP1_GetXMLTagName() {
        InciteTradeItem item = new InciteTradeItem(game, dutch, french, english);
        assertEquals(InciteTradeItem.TAG, item.getXMLTagName());
        assertEquals("inciteTradeItem", item.getXMLTagName());
    }

    // ------------------------------------------------------------------
    // M12: equals(Object) — 4 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M12-TP1: ENTRY -> N0 -(B1:false)-> N1 -> EXIT (!(o instanceof InciteTradeItem))
    public void testM12_TP1_EqualsNonInciteTradeItem() {
        InciteTradeItem item = new InciteTradeItem(game, dutch, french, english);
        assertFalse(item.equals("not-an-incite-item"));
        assertFalse(item.equals(null));
    }

    // M12-TP2: ENTRY -> N0 -(B1:true)-> N2 -(B2:true)-> N5 -> N6 -> EXIT (different victim)
    public void testM12_TP2_EqualsDifferentVictim() {
        InciteTradeItem item1 = new InciteTradeItem(game, dutch, french, english);
        InciteTradeItem item2 = new InciteTradeItem(game, dutch, french, dutch);
        assertFalse(item1.equals(item2));
    }

    // M12-TP3: ENTRY -> N0 -(B1:true)-> N2 -(B2:false)-> N3 -(B3:true)-> N5 -> N6 -> EXIT (same victim, super.equals false)
    public void testM12_TP3_EqualsSameVictimDifferentSuper() {
        InciteTradeItem item1 = new InciteTradeItem(game, dutch, french, english);
        InciteTradeItem item2 = new InciteTradeItem(game, french, dutch, english);
        assertFalse(item1.equals(item2));
    }

    // M12-TP4: ENTRY -> N0 -(B1:true)-> N2 -(B2:false)-> N3 -(B3:false)-> N4 -> N6 -> EXIT (same victim and super.equals true)
    public void testM12_TP4_EqualsEqualObjects() {
        InciteTradeItem item1 = new InciteTradeItem(game, dutch, french, english);
        InciteTradeItem item2 = new InciteTradeItem(game, dutch, french, english);
        assertTrue(item1.equals(item2));
    }

    // ------------------------------------------------------------------
    // M13: hashCode() — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M13-TP1: ENTRY -> N0 -> EXIT (computes hash from super.hashCode() and victim)
    public void testM13_TP1_HashCode() {
        InciteTradeItem item1 = new InciteTradeItem(game, dutch, french, english);
        InciteTradeItem item2 = new InciteTradeItem(game, dutch, french, english);
        assertEquals(item1.hashCode(), item2.hashCode());
    }

    // ------------------------------------------------------------------
    // M14: toString() — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M14-TP1: ENTRY -> N0 -> EXIT (formats [id victim.id])
    public void testM14_TP1_ToString() {
        InciteTradeItem item = new InciteTradeItem(game, dutch, french, english);
        String str = item.toString();
        assertTrue(str.contains(english.getId()));
    }
}
