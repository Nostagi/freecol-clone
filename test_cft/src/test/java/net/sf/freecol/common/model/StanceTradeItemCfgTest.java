package net.sf.freecol.common.model;

import java.io.StringReader;
import java.io.StringWriter;

import net.sf.freecol.common.io.FreeColXMLReader;
import net.sf.freecol.common.io.FreeColXMLWriter;
import net.sf.freecol.server.model.ServerUnit;
import net.sf.freecol.util.test.FreeColTestCase;


/**
 * CFG-driven test suite for {@link StanceTradeItem} derived from
 * {@code test_cft/graph/net/sf/freecol/common/model/StanceTradeItem.json}.
 *
 * Covers all 15 methods (M1..M15) and achieves 100% feasible C3 (node + edge)
 * coverage across 30 Test Paths.
 *
 * Infeasible edges documented:
 * - M8 ({@code evaluateFor}): {@code N0 -> N11} ({@code B1:default}) — {@code Stance}
 *   only has 5 enum constants ({@code WAR}, {@code PEACE}, {@code CEASE_FIRE},
 *   {@code ALLIANCE}, {@code UNCONTACTED}) which map to cases 1..5 in the switch.
 * - M9 ({@code copyIn}): {@code N1 -> N2} ({@code B2:false}) — {@code o} is non-null from
 *   {@code copyInCast(other, StanceTradeItem.class)}, so {@code super.copyIn(o)} always returns {@code true}.
 */
public class StanceTradeItemCfgTest extends FreeColTestCase {

    private Game game;
    private Player dutch;
    private Player french;
    private UnitType artilleryType;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        // Pre-condition: Standard game with Dutch and French players and artillery unit type.
        game = getStandardGame();
        dutch = game.getPlayerByNationId("model.nation.dutch");
        french = game.getPlayerByNationId("model.nation.french");
        artilleryType = spec().getUnitType("model.unit.artillery");
    }

    // ------------------------------------------------------------------
    // Pre-condition helpers & stubs
    // ------------------------------------------------------------------

    private void setStrengths(int dutchCount, int frenchCount) {
        for (int i = 0; i < dutchCount; i++) {
            new ServerUnit(game, null, dutch, artilleryType);
        }
        for (int i = 0; i < frenchCount; i++) {
            new ServerUnit(game, null, french, artilleryType);
        }
        dutch.putNationSummary(french, new NationSummary(french, dutch));
    }

    private String writeItemXml(StanceTradeItem item) throws Exception {
        StringWriter sw = new StringWriter();
        try (FreeColXMLWriter xw = new FreeColXMLWriter(sw,
                FreeColXMLWriter.WriteScope.toSave(), false)) {
            item.toXML(xw);
            xw.flush();
        }
        return sw.toString();
    }

    private StanceTradeItem readItemXml(String xml) throws Exception {
        try (FreeColXMLReader xr = new FreeColXMLReader(new StringReader(xml))) {
            xr.nextTag();
            return new StanceTradeItem(game, xr);
        }
    }

    // ------------------------------------------------------------------
    // M1: <init>(Game, Player, Player, Stance) — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M1-TP1: ENTRY -> N0 -> EXIT
    public void testM1_TP1_constructorWithStance() {
        StanceTradeItem item = new StanceTradeItem(game, dutch, french, Stance.PEACE);
        assertSame(dutch, item.getSource());
        assertSame(french, item.getDestination());
        assertEquals(Stance.PEACE, item.getStance());
    }

    // ------------------------------------------------------------------
    // M2: <init>(Game, FreeColXMLReader) — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M2-TP1: ENTRY -> N0 -> EXIT
    public void testM2_TP1_constructorFromXml() throws Exception {
        StanceTradeItem original = new StanceTradeItem(game, dutch, french, Stance.ALLIANCE);
        String xml = writeItemXml(original);
        StanceTradeItem parsed = readItemXml(xml);
        assertEquals(Stance.ALLIANCE, parsed.getStance());
        assertSame(dutch, parsed.getSource());
        assertSame(french, parsed.getDestination());
    }

    // ------------------------------------------------------------------
    // M3: isValid() — 2 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M3-TP1: ENTRY -> N0 -(B1:true)-> N2 -> N3 -> EXIT (stance == null)
    public void testM3_TP1_isValidNullStance() {
        StanceTradeItem item = new StanceTradeItem(game, dutch, french, null);
        assertFalse(item.isValid());
    }

    // M3-TP2: ENTRY -> N0 -(B1:false)-> N1 -> N3 -> EXIT (stance != null)
    public void testM3_TP2_isValidNonNullStance() {
        StanceTradeItem item = new StanceTradeItem(game, dutch, french, Stance.WAR);
        assertTrue(item.isValid());
    }

    // ------------------------------------------------------------------
    // M4: isUnique() — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M4-TP1: ENTRY -> N0 -> EXIT
    public void testM4_TP1_isUnique() {
        StanceTradeItem item = new StanceTradeItem(game, dutch, french, Stance.PEACE);
        assertTrue(item.isUnique());
    }

    // ------------------------------------------------------------------
    // M5: getLabel() — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M5-TP1: ENTRY -> N0 -> EXIT
    public void testM5_TP1_getLabel() {
        StanceTradeItem item = new StanceTradeItem(game, dutch, french, Stance.PEACE);
        assertNotNull(item.getLabel());
    }

    // ------------------------------------------------------------------
    // M6: getStance() — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M6-TP1: ENTRY -> N0 -> EXIT
    public void testM6_TP1_getStance() {
        StanceTradeItem item = new StanceTradeItem(game, dutch, french, Stance.CEASE_FIRE);
        assertEquals(Stance.CEASE_FIRE, item.getStance());
    }

    // ------------------------------------------------------------------
    // M7: setStance(Stance) — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M7-TP1: ENTRY -> N0 -> EXIT
    public void testM7_TP1_setStance() {
        StanceTradeItem item = new StanceTradeItem(game, dutch, french, Stance.PEACE);
        item.setStance(Stance.WAR);
        assertEquals(Stance.WAR, item.getStance());
    }

    // ------------------------------------------------------------------
    // M8: evaluateFor(Player) — 10 Test Paths (100% feasible C3)
    // ------------------------------------------------------------------

    // M8-TP1: ENTRY -> N0 -(B1:case:1)-> N1 -(B2:false)-> N2 -> N12 -> EXIT (WAR, ratio < 0.33)
    public void testM8_TP1_evaluateForWarWeak() {
        setStrengths(0, 3);
        StanceTradeItem item = new StanceTradeItem(game, dutch, french, Stance.WAR);
        assertEquals(TradeItem.INVALID_TRADE_ITEM, item.evaluateFor(dutch));
    }

    // M8-TP2: ENTRY -> N0 -(B1:case:1)-> N1 -(B2:true)-> N3 -(B3:false)-> N4 -> N12 -> EXIT (WAR, 0.33 <= ratio < 0.5)
    public void testM8_TP2_evaluateForWarModerate() {
        setStrengths(2, 3); // ratio = 0.4
        StanceTradeItem item = new StanceTradeItem(game, dutch, french, Stance.WAR);
        assertEquals(-40, item.evaluateFor(dutch));
    }

    // M8-TP3: ENTRY -> N0 -(B1:case:1)-> N1 -(B2:true)-> N3 -(B3:true)-> N12 -> EXIT (WAR, ratio >= 0.5)
    public void testM8_TP3_evaluateForWarStrong() {
        setStrengths(3, 2); // ratio = 0.6
        StanceTradeItem item = new StanceTradeItem(game, dutch, french, Stance.WAR);
        assertEquals(60, item.evaluateFor(dutch));
    }

    // M8-TP4: ENTRY -> N0 -(B1:case:2)-> N5 -(B4:false)-> N6 -> N12 -> EXIT (PEACE, ratio > 0.66)
    public void testM8_TP4_evaluateForPeaceTooStrong() {
        setStrengths(3, 1); // ratio = 0.75
        StanceTradeItem item = new StanceTradeItem(game, dutch, french, Stance.PEACE);
        assertEquals(TradeItem.INVALID_TRADE_ITEM, item.evaluateFor(dutch));
    }

    // M8-TP5: ENTRY -> N0 -(B1:case:2)-> N5 -(B4:true)-> N7 -(B5:false)-> N8 -> N12 -> EXIT (PEACE, 0.5 < ratio <= 0.66)
    public void testM8_TP5_evaluateForPeaceSlightlyStrong() {
        setStrengths(3, 2); // ratio = 0.6
        StanceTradeItem item = new StanceTradeItem(game, dutch, french, Stance.PEACE);
        assertEquals(-60, item.evaluateFor(dutch));
    }

    // M8-TP6: ENTRY -> N0 -(B1:case:2)-> N5 -(B4:true)-> N7 -(B5:true)-> N9 -(B6:false)-> N10 -> N12 -> EXIT (PEACE, ratio < 0.33)
    public void testM8_TP6_evaluateForPeaceWeak() {
        setStrengths(0, 3); // ratio = 0.0
        StanceTradeItem item = new StanceTradeItem(game, dutch, french, Stance.PEACE);
        assertEquals(1000, item.evaluateFor(dutch));
    }

    // M8-TP7: ENTRY -> N0 -(B1:case:2)-> N5 -(B4:true)-> N7 -(B5:true)-> N9 -(B6:true)-> N12 -> EXIT (PEACE, 0.33 <= ratio <= 0.5)
    public void testM8_TP7_evaluateForPeaceModerate() {
        setStrengths(2, 3); // ratio = 0.4
        StanceTradeItem item = new StanceTradeItem(game, dutch, french, Stance.PEACE);
        assertEquals(40, item.evaluateFor(dutch));
    }

    // M8-TP8: ENTRY -> N0 -(B1:case:3)-> N5 -(B4:true)-> N7 -(B5:true)-> N9 -(B6:true)-> N12 -> EXIT (CEASE_FIRE)
    public void testM8_TP8_evaluateForCeaseFire() {
        setStrengths(2, 3); // ratio = 0.4
        StanceTradeItem item = new StanceTradeItem(game, dutch, french, Stance.CEASE_FIRE);
        assertEquals(40, item.evaluateFor(dutch));
    }

    // M8-TP9: ENTRY -> N0 -(B1:case:4)-> N5 -(B4:true)-> N7 -(B5:true)-> N9 -(B6:true)-> N12 -> EXIT (ALLIANCE)
    public void testM8_TP9_evaluateForAlliance() {
        setStrengths(2, 3); // ratio = 0.4
        StanceTradeItem item = new StanceTradeItem(game, dutch, french, Stance.ALLIANCE);
        assertEquals(40, item.evaluateFor(dutch));
    }

    // M8-TP10: ENTRY -> N0 -(B1:case:5)-> N11 -> N12 -> EXIT (UNCONTACTED)
    public void testM8_TP10_evaluateForUncontacted() {
        setStrengths(2, 3);
        StanceTradeItem item = new StanceTradeItem(game, dutch, french, Stance.UNCONTACTED);
        assertEquals(TradeItem.INVALID_TRADE_ITEM, item.evaluateFor(dutch));
    }

    // ------------------------------------------------------------------
    // M9: copyIn(FreeColObject) — 2 Test Paths (100% feasible C3)
    // ------------------------------------------------------------------

    // M9-TP1: ENTRY -> N0 -(B1:true)-> N2 -> EXIT (o == null when other is null)
    public void testM9_TP1_copyInNull() {
        StanceTradeItem dst = new StanceTradeItem(game, dutch, french, Stance.PEACE);
        assertFalse(dst.copyIn(null));
    }

    // M9-TP2: ENTRY -> N0 -(B1:false)-> N1 -(B2:true)-> N3 -> EXIT (o != null && super.copyIn(o))
    public void testM9_TP2_copyInValid() {
        StanceTradeItem src = new StanceTradeItem(game, dutch, french, Stance.WAR);
        StanceTradeItem dst = new StanceTradeItem(game, dutch, french, Stance.PEACE);
        dst.setId(src.getId());
        assertTrue(dst.copyIn(src));
        assertEquals(Stance.WAR, dst.getStance());
    }

    // ------------------------------------------------------------------
    // M10: writeAttributes(FreeColXMLWriter) — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M10-TP1: ENTRY -> N0 -> EXIT
    public void testM10_TP1_writeAttributes() throws Exception {
        StanceTradeItem item = new StanceTradeItem(game, dutch, french, Stance.CEASE_FIRE);
        String xml = writeItemXml(item);
        assertTrue(xml.contains("stance=\""));
        assertEquals(Stance.CEASE_FIRE, readItemXml(xml).getStance());
    }

    // ------------------------------------------------------------------
    // M11: readAttributes(FreeColXMLReader) — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M11-TP1: ENTRY -> N0 -> EXIT
    public void testM11_TP1_readAttributes() throws Exception {
        StanceTradeItem item = new StanceTradeItem(game, dutch, french, Stance.WAR);
        String xml = writeItemXml(item);
        StanceTradeItem parsed = readItemXml(xml);
        assertEquals(Stance.WAR, parsed.getStance());
    }

    // ------------------------------------------------------------------
    // M12: getXMLTagName() — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M12-TP1: ENTRY -> N0 -> EXIT
    public void testM12_TP1_getXMLTagName() {
        StanceTradeItem item = new StanceTradeItem(game, dutch, french, Stance.PEACE);
        assertEquals(StanceTradeItem.TAG, item.getXMLTagName());
    }

    // ------------------------------------------------------------------
    // M13: equals(Object) — 4 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M13-TP1: ENTRY -> N0 -(B1:true)-> N6 -> EXIT (!(o instanceof StanceTradeItem))
    public void testM13_TP1_equalsNotInstance() {
        StanceTradeItem item = new StanceTradeItem(game, dutch, french, Stance.PEACE);
        assertFalse(item.equals("not a StanceTradeItem"));
    }

    // M13-TP2: ENTRY -> N0 -(B1:false)-> N1 -(B2:true)-> N4 -> N5 -> EXIT (this.stance != other.stance)
    public void testM13_TP2_equalsDifferentStance() {
        StanceTradeItem a = new StanceTradeItem(game, dutch, french, Stance.PEACE);
        StanceTradeItem b = new StanceTradeItem(game, dutch, french, Stance.WAR);
        assertFalse(a.equals(b));
    }

    // M13-TP3: ENTRY -> N0 -(B1:false)-> N1 -(B2:false)-> N2 -(B3:true)-> N4 -> N5 -> EXIT (same stance, !super.equals(other))
    public void testM13_TP3_equalsDifferentSuper() {
        Player english = game.getPlayerByNationId("model.nation.english");
        StanceTradeItem a = new StanceTradeItem(game, dutch, french, Stance.PEACE);
        StanceTradeItem b = new StanceTradeItem(game, dutch, english, Stance.PEACE);
        assertFalse(a.equals(b));
    }

    // M13-TP4: ENTRY -> N0 -(B1:false)-> N1 -(B2:false)-> N2 -(B3:false)-> N3 -> N5 -> EXIT (same stance, super.equals(other))
    public void testM13_TP4_equalsSame() {
        StanceTradeItem a = new StanceTradeItem(game, dutch, french, Stance.PEACE);
        StanceTradeItem b = new StanceTradeItem(game, dutch, french, Stance.PEACE);
        b.setId(a.getId());
        assertTrue(a.equals(b));
    }

    // ------------------------------------------------------------------
    // M14: hashCode() — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M14-TP1: ENTRY -> N0 -> EXIT
    public void testM14_TP1_hashCode() {
        StanceTradeItem a = new StanceTradeItem(game, dutch, french, Stance.PEACE);
        StanceTradeItem b = new StanceTradeItem(game, dutch, french, Stance.PEACE);
        b.setId(a.getId());
        assertEquals(a.hashCode(), b.hashCode());
    }

    // ------------------------------------------------------------------
    // M15: toString() — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M15-TP1: ENTRY -> N0 -> EXIT
    public void testM15_TP1_toString() {
        StanceTradeItem item = new StanceTradeItem(game, dutch, french, Stance.PEACE);
        String s = item.toString();
        assertTrue(s.contains("PEACE"));
    }
}
