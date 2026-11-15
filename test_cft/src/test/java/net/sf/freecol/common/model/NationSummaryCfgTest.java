package net.sf.freecol.common.model;

import java.io.StringReader;
import java.io.StringWriter;

import net.sf.freecol.common.io.FreeColXMLReader;
import net.sf.freecol.common.io.FreeColXMLWriter;
import net.sf.freecol.util.test.FreeColTestCase;


/**
 * CFG-driven test suite for {@link NationSummary} derived from
 * {@code test_cft/graph/net/sf/freecol/common/model/NationSummary.json}.
 *
 * Covers all 15 methods (M1..M15) and achieves 100% feasible C3 (node + edge)
 * coverage across 20 Test Paths.
 *
 * Infeasible edges documented:
 * - M12 ({@code copyIn}): {@code N1 -> N2} ({@code B2:false}) — {@code o} is non-null from
 *   {@code copyInCast(other, NationSummary.class)} so {@code o.getId().equals(getId())} holds
 *   and {@code FreeColObject.copyIn(o)} always returns {@code true}.
 */
public class NationSummaryCfgTest extends FreeColTestCase {

    private Game game;
    private Player dutch;
    private Player french;
    private Player inca;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        // Pre-condition: Standard game with European (Dutch, French) and native (Inca) players.
        game = getStandardGame();
        dutch = game.getPlayerByNationId("model.nation.dutch");
        french = game.getPlayerByNationId("model.nation.french");
        inca = game.getPlayerByNationId("model.nation.inca");
    }

    // ------------------------------------------------------------------
    // Pre-condition helpers & stubs
    // ------------------------------------------------------------------

    private String writeSummaryXml(NationSummary summary) throws Exception {
        StringWriter sw = new StringWriter();
        try (FreeColXMLWriter xw = new FreeColXMLWriter(sw,
                FreeColXMLWriter.WriteScope.toSave(), false)) {
            summary.toXML(xw);
            xw.flush();
        }
        return sw.toString();
    }

    private NationSummary readSummaryXml(String xml) throws Exception {
        NationSummary result = new NationSummary();
        try (FreeColXMLReader xr = new FreeColXMLReader(new StringReader(xml))) {
            xr.nextTag();
            result.readFromXML(xr);
        }
        return result;
    }

    // ------------------------------------------------------------------
    // M1: <init>() — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M1-TP1: ENTRY -> N0 -> EXIT (default constructor sets empty id)
    public void testM1_TP1_defaultConstructor() {
        NationSummary ns = new NationSummary();
        assertEquals("", ns.getId());
        assertNull(ns.getStance());
    }

    // ------------------------------------------------------------------
    // M2: <init>(Player, Player) — 4 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M2-TP1: ENTRY -> N0 -(B1:false)-> N1 -> N2 -(B2:true)-> N7 -> N8 -> EXIT (UNCONTACTED->PEACE, !isEuropean)
    public void testM2_TP1_uncontactedNativePlayer() {
        inca.setStance(dutch, Stance.UNCONTACTED);
        NationSummary ns = new NationSummary(inca, dutch);
        assertEquals(Stance.PEACE, ns.getStance());
        assertEquals(inca.getSettlementCount(), ns.getNumberOfSettlements());
        assertEquals(-1, ns.getNumberOfUnits());
        assertEquals(-1, ns.getMilitaryStrength());
        assertEquals(-1, ns.getNavalStrength());
        assertEquals(-1, ns.getGold());
        assertEquals(-1, ns.getSoL());
        assertEquals(-1, ns.getFoundingFathers());
        assertEquals(-1, ns.getTax());
    }

    // M2-TP2: ENTRY -> N0 -(B1:true)-> N2 -(B2:false)-> N3 -(B3:true)-> N5 -> N8 -> EXIT (stance!=UNCONTACTED, European, player==requester)
    public void testM2_TP2_selfEuropeanSummary() {
        dutch.modifyGold(500);
        dutch.setTax(15);
        NationSummary ns = new NationSummary(dutch, dutch);
        assertNotNull(ns.getStance());
        assertEquals(dutch.getUnitCount(), ns.getNumberOfUnits());
        assertEquals(500, ns.getGold());
        assertEquals(dutch.getSoL(), ns.getSoL());
        assertEquals(dutch.getFatherCount(), ns.getFoundingFathers());
        assertEquals(15, ns.getTax());
    }

    // M2-TP3: ENTRY -> N0 -(B1:true)-> N2 -(B2:false)-> N3 -(B3:false)-> N4 -(B4:true)-> N6 -> N8 -> EXIT (European, player!=requester, no BETTER_FOREIGN_AFFAIRS_REPORT)
    public void testM2_TP3_otherEuropeanWithoutBetterReport() {
        french.setStance(dutch, Stance.WAR);
        french.modifyGold(250);
        NationSummary ns = new NationSummary(french, dutch);
        assertEquals(Stance.WAR, ns.getStance());
        assertEquals(250, ns.getGold());
        assertEquals(-1, ns.getSoL());
        assertEquals(-1, ns.getFoundingFathers());
        assertEquals(-1, ns.getTax());
    }

    // M2-TP4: ENTRY -> N0 -(B1:true)-> N2 -(B2:false)-> N3 -(B3:false)-> N4 -(B4:false)-> N5 -> N8 -> EXIT (European, player!=requester, has BETTER_FOREIGN_AFFAIRS_REPORT)
    public void testM2_TP4_otherEuropeanWithBetterReport() {
        french.setStance(dutch, Stance.PEACE);
        french.setTax(20);
        dutch.addAbility(new Ability(Ability.BETTER_FOREIGN_AFFAIRS_REPORT));
        NationSummary ns = new NationSummary(french, dutch);
        assertEquals(Stance.PEACE, ns.getStance());
        assertEquals(french.getSoL(), ns.getSoL());
        assertEquals(french.getFatherCount(), ns.getFoundingFathers());
        assertEquals(20, ns.getTax());
    }

    // ------------------------------------------------------------------
    // M3: getStance() — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M3-TP1: ENTRY -> N0 -> EXIT
    public void testM3_TP1_getStance() {
        dutch.setStance(french, Stance.CEASE_FIRE);
        NationSummary ns = new NationSummary(dutch, french);
        assertEquals(Stance.CEASE_FIRE, ns.getStance());
    }

    // ------------------------------------------------------------------
    // M4: getNumberOfSettlements() — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M4-TP1: ENTRY -> N0 -> EXIT
    public void testM4_TP1_getNumberOfSettlements() {
        game.setMap(getTestMap());
        createStandardColony(1);
        NationSummary ns = new NationSummary(dutch, dutch);
        assertEquals(1, ns.getNumberOfSettlements());
    }

    // ------------------------------------------------------------------
    // M5: getNumberOfUnits() — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M5-TP1: ENTRY -> N0 -> EXIT
    public void testM5_TP1_getNumberOfUnits() {
        game.setMap(getTestMap());
        createStandardColony(2);
        NationSummary ns = new NationSummary(dutch, dutch);
        assertEquals(dutch.getUnitCount(), ns.getNumberOfUnits());
    }

    // ------------------------------------------------------------------
    // M6: getMilitaryStrength() — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M6-TP1: ENTRY -> N0 -> EXIT
    public void testM6_TP1_getMilitaryStrength() {
        NationSummary ns = new NationSummary(dutch, dutch);
        assertEquals(dutch.calculateStrength(false), ns.getMilitaryStrength());
    }

    // ------------------------------------------------------------------
    // M7: getNavalStrength() — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M7-TP1: ENTRY -> N0 -> EXIT
    public void testM7_TP1_getNavalStrength() {
        NationSummary ns = new NationSummary(dutch, dutch);
        assertEquals(dutch.calculateStrength(true), ns.getNavalStrength());
    }

    // ------------------------------------------------------------------
    // M8: getGold() — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M8-TP1: ENTRY -> N0 -> EXIT
    public void testM8_TP1_getGold() {
        dutch.modifyGold(333);
        NationSummary ns = new NationSummary(dutch, dutch);
        assertEquals(333, ns.getGold());
    }

    // ------------------------------------------------------------------
    // M9: getFoundingFathers() — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M9-TP1: ENTRY -> N0 -> EXIT
    public void testM9_TP1_getFoundingFathers() {
        NationSummary ns = new NationSummary(dutch, dutch);
        assertEquals(dutch.getFatherCount(), ns.getFoundingFathers());
    }

    // ------------------------------------------------------------------
    // M10: getSoL() — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M10-TP1: ENTRY -> N0 -> EXIT
    public void testM10_TP1_getSoL() {
        NationSummary ns = new NationSummary(dutch, dutch);
        assertEquals(dutch.getSoL(), ns.getSoL());
    }

    // ------------------------------------------------------------------
    // M11: getTax() — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M11-TP1: ENTRY -> N0 -> EXIT
    public void testM11_TP1_getTax() {
        dutch.setTax(12);
        NationSummary ns = new NationSummary(dutch, dutch);
        assertEquals(12, ns.getTax());
    }

    // ------------------------------------------------------------------
    // M12: copyIn(FreeColObject) — 2 Test Paths (100% feasible C3)
    // ------------------------------------------------------------------

    // M12-TP1: ENTRY -> N0 -(B1:true)-> N2 -> EXIT (o == null when other is null)
    public void testM12_TP1_copyInNull() {
        NationSummary target = new NationSummary();
        assertFalse(target.copyIn(null));
    }

    // M12-TP2: ENTRY -> N0 -(B1:false)-> N1 -(B2:true)-> N3 -> EXIT (o != null && super.copyIn(o))
    public void testM12_TP2_copyInValid() {
        dutch.modifyGold(777);
        dutch.setTax(18);
        NationSummary src = new NationSummary(dutch, dutch);
        NationSummary dst = new NationSummary();
        assertTrue(dst.copyIn(src));
        assertEquals(src.getStance(), dst.getStance());
        assertEquals(777, dst.getGold());
        assertEquals(18, dst.getTax());
    }

    // ------------------------------------------------------------------
    // M13: writeAttributes(FreeColXMLWriter) — 2 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M13-TP1: ENTRY -> N0 -(B1:false)-> N1 -> N2 -(B2:false)-> N3 -> N4 -(B3:false)-> N5 -> N6 -> EXIT (soL >= 0, foundingFathers >= 0, tax >= 0)
    public void testM13_TP1_writeAttributesAllNonNegative() throws Exception {
        dutch.setTax(10);
        NationSummary ns = new NationSummary(dutch, dutch);
        String xml = writeSummaryXml(ns);
        assertTrue(xml.contains("SoL="));
        assertTrue(xml.contains("foundingFathers="));
        assertTrue(xml.contains("tax="));
    }

    // M13-TP2: ENTRY -> N0 -(B1:true)-> N2 -(B2:true)-> N4 -(B3:true)-> N6 -> EXIT (soL < 0, foundingFathers < 0, tax < 0)
    public void testM13_TP2_writeAttributesNegativeHiddenFields() throws Exception {
        french.setStance(dutch, Stance.PEACE);
        NationSummary ns = new NationSummary(french, dutch);
        String xml = writeSummaryXml(ns);
        assertFalse(xml.contains("SoL="));
        assertFalse(xml.contains("foundingFathers="));
        assertFalse(xml.contains("tax="));
    }

    // ------------------------------------------------------------------
    // M14: readAttributes(FreeColXMLReader) — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M14-TP1: ENTRY -> N0 -> EXIT (round-trip readAttributes from XML)
    public void testM14_TP1_readAttributes() throws Exception {
        dutch.modifyGold(450);
        dutch.setTax(25);
        NationSummary original = new NationSummary(dutch, dutch);
        String xml = writeSummaryXml(original);
        NationSummary parsed = readSummaryXml(xml);
        assertEquals(original.getStance(), parsed.getStance());
        assertEquals(450, parsed.getGold());
        assertEquals(25, parsed.getTax());
        assertEquals(original.getSoL(), parsed.getSoL());
        assertEquals(original.getFoundingFathers(), parsed.getFoundingFathers());
    }

    // ------------------------------------------------------------------
    // M15: getXMLTagName() — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M15-TP1: ENTRY -> N0 -> EXIT
    public void testM15_TP1_getXMLTagName() {
        assertEquals(NationSummary.TAG, new NationSummary().getXMLTagName());
    }
}
