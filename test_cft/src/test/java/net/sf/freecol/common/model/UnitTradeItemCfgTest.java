package net.sf.freecol.common.model;

import java.io.StringReader;
import java.io.StringWriter;

import net.sf.freecol.common.io.FreeColXMLReader;
import net.sf.freecol.common.io.FreeColXMLWriter;
import net.sf.freecol.server.model.ServerUnit;
import net.sf.freecol.util.test.FreeColTestCase;


/**
 * CFG-driven test suite for {@link UnitTradeItem} derived from
 * {@code test_cft/graph/net/sf/freecol/common/model/UnitTradeItem.json}.
 *
 * Covers all 15 methods (M1..M15) and achieves 100% feasible C3 (node + edge)
 * coverage across 26 Test Paths.
 *
 * Infeasible edges documented:
 * - M9 ({@code copyIn}): {@code N1 -> N2} ({@code B2:false}) — {@code o} is non-null from
 *   {@code copyInCast(other, UnitTradeItem.class)}, so {@code super.copyIn(o)} always returns {@code true}.
 */
public class UnitTradeItemCfgTest extends FreeColTestCase {

    private Game game;
    private Player dutch;
    private Player french;
    private UnitType colonistType;
    private UnitType braveType;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        // Pre-condition: Standard game with Dutch and French players and unit types.
        game = getStandardGame();
        dutch = game.getPlayerByNationId("model.nation.dutch");
        french = game.getPlayerByNationId("model.nation.french");
        colonistType = spec().getUnitType("model.unit.freeColonist");
        braveType = spec().getUnitType("model.unit.brave");
    }

    // ------------------------------------------------------------------
    // Pre-condition helpers & stubs
    // ------------------------------------------------------------------

    private Unit createUnit(Player owner, UnitType type) {
        return new ServerUnit(game, null, owner, type);
    }

    private String writeItemXml(UnitTradeItem item) throws Exception {
        StringWriter sw = new StringWriter();
        try (FreeColXMLWriter xw = new FreeColXMLWriter(sw,
                FreeColXMLWriter.WriteScope.toSave(), false)) {
            item.toXML(xw);
            xw.flush();
        }
        return sw.toString();
    }

    private UnitTradeItem readItemXml(String xml) throws Exception {
        try (FreeColXMLReader xr = new FreeColXMLReader(new StringReader(xml))) {
            xr.nextTag();
            return new UnitTradeItem(game, xr);
        }
    }

    // ------------------------------------------------------------------
    // M1: <init>(Game, Player, Player, Unit) — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M1-TP1: ENTRY -> N0 -> EXIT
    public void testM1_TP1_constructorWithUnit() {
        Unit u = createUnit(dutch, colonistType);
        UnitTradeItem item = new UnitTradeItem(game, dutch, french, u);
        assertSame(dutch, item.getSource());
        assertSame(french, item.getDestination());
        assertSame(u, item.getUnit());
    }

    // ------------------------------------------------------------------
    // M2: <init>(Game, FreeColXMLReader) — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M2-TP1: ENTRY -> N0 -> EXIT
    public void testM2_TP1_constructorFromXml() throws Exception {
        Unit u = createUnit(dutch, colonistType);
        UnitTradeItem original = new UnitTradeItem(game, dutch, french, u);
        String xml = writeItemXml(original);
        UnitTradeItem parsed = readItemXml(xml);
        assertSame(u, parsed.getUnit());
    }

    // ------------------------------------------------------------------
    // M3: isValid() — 3 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M3-TP1: ENTRY -> N0 -(B1:true)-> N3 -> N4 -> EXIT (unit.getOwner() != getSource())
    public void testM3_TP1_isValidWrongOwner() {
        Unit u = createUnit(french, colonistType);
        UnitTradeItem item = new UnitTradeItem(game, dutch, french, u);
        assertFalse(item.isValid());
    }

    // M3-TP2: ENTRY -> N0 -(B1:false)-> N1 -(B2:true)-> N3 -> N4 -> EXIT (owner == source, !isAvailableTo(destination))
    public void testM3_TP2_isValidUnavailableUnitType() {
        Unit u = createUnit(dutch, braveType);
        UnitTradeItem item = new UnitTradeItem(game, dutch, french, u);
        assertFalse(item.isValid());
    }

    // M3-TP3: ENTRY -> N0 -(B1:false)-> N1 -(B2:false)-> N2 -> N4 -> EXIT (owner == source, isAvailableTo(destination))
    public void testM3_TP3_isValidTrue() {
        Unit u = createUnit(dutch, colonistType);
        UnitTradeItem item = new UnitTradeItem(game, dutch, french, u);
        assertTrue(item.isValid());
    }

    // ------------------------------------------------------------------
    // M4: isUnique() — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M4-TP1: ENTRY -> N0 -> EXIT
    public void testM4_TP1_isUnique() {
        Unit u = createUnit(dutch, colonistType);
        UnitTradeItem item = new UnitTradeItem(game, dutch, french, u);
        assertFalse(item.isUnique());
    }

    // ------------------------------------------------------------------
    // M5: getLabel() — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M5-TP1: ENTRY -> N0 -> EXIT
    public void testM5_TP1_getLabel() {
        Unit u = createUnit(dutch, colonistType);
        UnitTradeItem item = new UnitTradeItem(game, dutch, french, u);
        assertNotNull(item.getLabel());
    }

    // ------------------------------------------------------------------
    // M6: getUnit() — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M6-TP1: ENTRY -> N0 -> EXIT
    public void testM6_TP1_getUnit() {
        Unit u = createUnit(dutch, colonistType);
        UnitTradeItem item = new UnitTradeItem(game, dutch, french, u);
        assertSame(u, item.getUnit());
    }

    // ------------------------------------------------------------------
    // M7: setUnit(Unit) — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M7-TP1: ENTRY -> N0 -> EXIT
    public void testM7_TP1_setUnit() {
        Unit u1 = createUnit(dutch, colonistType);
        Unit u2 = createUnit(dutch, colonistType);
        UnitTradeItem item = new UnitTradeItem(game, dutch, french, u1);
        item.setUnit(u2);
        assertSame(u2, item.getUnit());
    }

    // ------------------------------------------------------------------
    // M8: evaluateFor(Player) — 5 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M8-TP1: ENTRY -> N0 -(B1:false)-> N1 -> N8 -> EXIT (!isValid())
    public void testM8_TP1_evaluateForInvalid() {
        Unit u = createUnit(french, colonistType);
        UnitTradeItem item = new UnitTradeItem(game, dutch, french, u);
        assertEquals(TradeItem.INVALID_TRADE_ITEM, item.evaluateFor(dutch));
    }

    // M8-TP2: ENTRY -> N0 -(B1:true)-> N2 -(B2:true)-> N7 -> N8 -> EXIT (isValid(), player != source)
    public void testM8_TP2_evaluateForDestination() {
        Unit u = createUnit(dutch, colonistType);
        UnitTradeItem item = new UnitTradeItem(game, dutch, french, u);
        assertEquals(u.evaluateFor(french), item.evaluateFor(french));
    }

    // M8-TP3: ENTRY -> N0 -(B1:true)-> N2 -(B2:false)-> N3 -(B3:true)-> N6 -> N8 -> EXIT (player == source, !player.isAI())
    public void testM8_TP3_evaluateForHumanSource() {
        dutch.setAI(false);
        Unit u = createUnit(dutch, colonistType);
        UnitTradeItem item = new UnitTradeItem(game, dutch, french, u);
        assertEquals(-u.evaluateFor(dutch), item.evaluateFor(dutch));
    }

    // M8-TP4: ENTRY -> N0 -(B1:true)-> N2 -(B2:false)-> N3 -(B3:false)-> N4 -(B4:false)-> N5 -> N8 -> EXIT (player == source, isAI(), unitCount < 10)
    public void testM8_TP4_evaluateForAISourceFewUnits() {
        dutch.setAI(true);
        Unit u = createUnit(dutch, colonistType);
        assertTrue(dutch.getUnitCount() < 10);
        UnitTradeItem item = new UnitTradeItem(game, dutch, french, u);
        assertEquals(TradeItem.INVALID_TRADE_ITEM, item.evaluateFor(dutch));
    }

    // M8-TP5: ENTRY -> N0 -(B1:true)-> N2 -(B2:false)-> N3 -(B3:false)-> N4 -(B4:true)-> N6 -> N8 -> EXIT (player == source, isAI(), unitCount >= 10)
    public void testM8_TP5_evaluateForAISourceManyUnits() {
        dutch.setAI(true);
        Unit u = createUnit(dutch, colonistType);
        for (int i = 0; i < 10; i++) {
            createUnit(dutch, colonistType);
        }
        assertTrue(dutch.getUnitCount() >= 10);
        UnitTradeItem item = new UnitTradeItem(game, dutch, french, u);
        assertEquals(-u.evaluateFor(dutch), item.evaluateFor(dutch));
    }

    // ------------------------------------------------------------------
    // M9: copyIn(FreeColObject) — 2 Test Paths (100% feasible C3)
    // ------------------------------------------------------------------

    // M9-TP1: ENTRY -> N0 -(B1:true)-> N2 -> EXIT (o == null)
    public void testM9_TP1_copyInNull() {
        Unit u = createUnit(dutch, colonistType);
        UnitTradeItem dst = new UnitTradeItem(game, dutch, french, u);
        assertFalse(dst.copyIn(null));
    }

    // M9-TP2: ENTRY -> N0 -(B1:false)-> N1 -(B2:true)-> N3 -> EXIT (o != null && super.copyIn(o))
    public void testM9_TP2_copyInValid() {
        Unit u1 = createUnit(dutch, colonistType);
        Unit u2 = createUnit(dutch, colonistType);
        UnitTradeItem src = new UnitTradeItem(game, dutch, french, u2);
        UnitTradeItem dst = new UnitTradeItem(game, dutch, french, u1);
        dst.setId(src.getId());
        assertTrue(dst.copyIn(src));
        assertSame(u2, dst.getUnit());
    }

    // ------------------------------------------------------------------
    // M10: writeAttributes(FreeColXMLWriter) — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M10-TP1: ENTRY -> N0 -> EXIT
    public void testM10_TP1_writeAttributes() throws Exception {
        Unit u = createUnit(dutch, colonistType);
        UnitTradeItem item = new UnitTradeItem(game, dutch, french, u);
        String xml = writeItemXml(item);
        assertTrue(xml.contains("unit=\"" + u.getId() + "\""));
    }

    // ------------------------------------------------------------------
    // M11: readAttributes(FreeColXMLReader) — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M11-TP1: ENTRY -> N0 -> EXIT
    public void testM11_TP1_readAttributes() throws Exception {
        Unit u = createUnit(dutch, colonistType);
        UnitTradeItem item = new UnitTradeItem(game, dutch, french, u);
        String xml = writeItemXml(item);
        UnitTradeItem parsed = readItemXml(xml);
        assertSame(u, parsed.getUnit());
    }

    // ------------------------------------------------------------------
    // M12: getXMLTagName() — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M12-TP1: ENTRY -> N0 -> EXIT
    public void testM12_TP1_getXMLTagName() {
        Unit u = createUnit(dutch, colonistType);
        UnitTradeItem item = new UnitTradeItem(game, dutch, french, u);
        assertEquals(UnitTradeItem.TAG, item.getXMLTagName());
    }

    // ------------------------------------------------------------------
    // M13: equals(Object) — 4 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M13-TP1: ENTRY -> N0 -(B1:true)-> N6 -> EXIT (!(other instanceof UnitTradeItem))
    public void testM13_TP1_equalsNotInstance() {
        Unit u = createUnit(dutch, colonistType);
        UnitTradeItem item = new UnitTradeItem(game, dutch, french, u);
        assertFalse(item.equals("not UnitTradeItem"));
    }

    // M13-TP2: ENTRY -> N0 -(B1:false)-> N1 -(B2:true)-> N4 -> N5 -> EXIT (!Utils.equals(this.unit, other.unit))
    public void testM13_TP2_equalsDifferentUnit() {
        Unit u1 = createUnit(dutch, colonistType);
        Unit u2 = createUnit(dutch, colonistType);
        UnitTradeItem a = new UnitTradeItem(game, dutch, french, u1);
        UnitTradeItem b = new UnitTradeItem(game, dutch, french, u2);
        b.setId(a.getId());
        assertFalse(a.equals(b));
    }

    // M13-TP3: ENTRY -> N0 -(B1:false)-> N1 -(B2:false)-> N2 -(B3:true)-> N4 -> N5 -> EXIT (same unit, !super.equals)
    public void testM13_TP3_equalsDifferentSuper() {
        Player english = game.getPlayerByNationId("model.nation.english");
        Unit u = createUnit(dutch, colonistType);
        UnitTradeItem a = new UnitTradeItem(game, dutch, french, u);
        UnitTradeItem b = new UnitTradeItem(game, dutch, english, u);
        b.setId(a.getId());
        assertFalse(a.equals(b));
    }

    // M13-TP4: ENTRY -> N0 -(B1:false)-> N1 -(B2:false)-> N2 -(B3:false)-> N3 -> N5 -> EXIT (all equal)
    public void testM13_TP4_equalsSame() {
        Unit u = createUnit(dutch, colonistType);
        UnitTradeItem a = new UnitTradeItem(game, dutch, french, u);
        UnitTradeItem b = new UnitTradeItem(game, dutch, french, u);
        b.setId(a.getId());
        assertTrue(a.equals(b));
    }

    // ------------------------------------------------------------------
    // M14: hashCode() — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M14-TP1: ENTRY -> N0 -> EXIT
    public void testM14_TP1_hashCode() {
        Unit u = createUnit(dutch, colonistType);
        UnitTradeItem a = new UnitTradeItem(game, dutch, french, u);
        UnitTradeItem b = new UnitTradeItem(game, dutch, french, u);
        b.setId(a.getId());
        assertEquals(a.hashCode(), b.hashCode());
    }

    // ------------------------------------------------------------------
    // M15: toString() — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M15-TP1: ENTRY -> N0 -> EXIT
    public void testM15_TP1_toString() {
        Unit u = createUnit(dutch, colonistType);
        UnitTradeItem item = new UnitTradeItem(game, dutch, french, u);
        String s = item.toString();
        assertTrue(s.contains(u.getId()));
    }
}
