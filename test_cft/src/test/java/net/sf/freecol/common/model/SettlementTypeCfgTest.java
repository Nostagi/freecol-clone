package net.sf.freecol.common.model;

import java.io.StringReader;
import java.io.StringWriter;
import java.lang.reflect.Method;
import java.util.Collections;

import net.sf.freecol.common.io.FreeColXMLReader;
import net.sf.freecol.common.io.FreeColXMLWriter;
import net.sf.freecol.server.model.ServerUnit;
import net.sf.freecol.util.test.FreeColTestCase;


/**
 * CFG-driven test suite for {@link SettlementType} derived from
 * {@code test_cft/graph/net/sf/freecol/common/model/SettlementType.json}.
 *
 * Covers all 28 methods (M1..M28) and achieves 100% feasible C3 (node + edge)
 * coverage across 39 Test Paths.
 *
 * Infeasible edges documented:
 * - M20 ({@code copyIn(FreeColObject)}): {@code N1 -> N2} ({@code B2:false}) —
 *   once {@code copyInCast(other, SettlementType.class)} returns a non-null
 *   {@code SettlementType o}, {@code super.copyIn(o)} always returns {@code true}.
 */
public class SettlementTypeCfgTest extends FreeColTestCase {

    private Game game;
    private Player dutch;
    private UnitType colonistType;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        // Pre-condition: standard game, player, and colonist unit type for plunder checks
        game = getStandardGame();
        dutch = game.getPlayerByNationId("model.nation.dutch");
        colonistType = spec().getUnitType("model.unit.freeColonist");
    }

    // ------------------------------------------------------------------
    // Pre-condition helpers & stubs
    // ------------------------------------------------------------------

    private void invokeAddPlunderType(SettlementType st, PlunderType pt) throws Exception {
        Method m = SettlementType.class.getDeclaredMethod("addPlunderType", PlunderType.class);
        m.setAccessible(true);
        m.invoke(st, pt);
    }

    private String serializeToXml(SettlementType st) throws Exception {
        StringWriter sw = new StringWriter();
        try (FreeColXMLWriter xw = new FreeColXMLWriter(sw, FreeColXMLWriter.WriteScope.toSave())) {
            st.toXML(xw);
        }
        return sw.toString();
    }

    private SettlementType readFromXml(String xml) throws Exception {
        try (FreeColXMLReader xr = new FreeColXMLReader(new StringReader(xml))) {
            xr.nextTag();
            return new SettlementType(xr, spec());
        }
    }

    // ------------------------------------------------------------------
    // M1: SettlementType(String, Specification) — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M1-TP1: ENTRY -> N0 -> EXIT (constructs SettlementType with default field values)
    public void testM1_TP1_ConstructorWithIdAndSpec() {
        SettlementType st = new SettlementType("model.settlement.test", spec());
        assertEquals("model.settlement.test", st.getId());
        assertFalse(st.isCapital());
    }

    // ------------------------------------------------------------------
    // M2: SettlementType(FreeColXMLReader, Specification) — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M2-TP1: ENTRY -> N0 -> EXIT (deserializes SettlementType from XML)
    public void testM2_TP1_ConstructorFromXml() throws Exception {
        String xml = "<settlementType id=\"model.settlement.xml\" capital=\"true\"/>";
        SettlementType st = readFromXml(xml);
        assertEquals("model.settlement.xml", st.getId());
        assertTrue(st.isCapital());
    }

    // ------------------------------------------------------------------
    // M3..M13: Simple getters — 1 Test Path each (100% C3)
    // ------------------------------------------------------------------

    // M3-TP1..M13-TP1: ENTRY -> N0 -> EXIT (all 11 scalar getters return defaults/configured values)
    public void testM3_to_M13_ScalarGetters() {
        SettlementType st = new SettlementType("model.settlement.defaults", spec());
        assertFalse(st.isCapital());                 // M3: isCapital
        assertEquals(3, st.getMinimumSize());        // M4: getMinimumSize
        assertEquals(10, st.getMaximumSize());       // M5: getMaximumSize
        assertEquals(2, st.getVisibleRadius());      // M6: getVisibleRadius
        assertEquals(1, st.getClaimableRadius());    // M7: getClaimableRadius
        assertEquals(2, st.getExtraClaimableRadius()); // M8: getExtraClaimableRadius
        assertEquals(4, st.getWanderingRadius());    // M9: getWanderingRadius
        assertEquals(1, st.getMinimumGrowth());      // M10: getMinimumGrowth
        assertEquals(10, st.getMaximumGrowth());     // M11: getMaximumGrowth
        assertEquals(1, st.getTradeBonus());         // M12: getTradeBonus
        assertEquals(100, st.getConvertThreshold()); // M13: getConvertThreshold
    }

    // ------------------------------------------------------------------
    // M14: getPlunderTypes() — 2 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M14-TP1: ENTRY -> N0 -(B1:true)-> N2 -> N3 -> EXIT (plunderTypes != null)
    public void testM14_TP1_GetPlunderTypesNonNull() throws Exception {
        SettlementType st = new SettlementType("model.settlement.p1", spec());
        PlunderType pt = new PlunderType("model.plunder.test", spec());
        invokeAddPlunderType(st, pt);
        assertEquals(1, st.getPlunderTypes().size());
    }

    // M14-TP2: ENTRY -> N0 -(B1:false)-> N1 -> N3 -> EXIT (plunderTypes == null -> emptyList)
    public void testM14_TP2_GetPlunderTypesNull() {
        SettlementType st = new SettlementType("model.settlement.p2", spec());
        assertTrue(st.getPlunderTypes().isEmpty());
    }

    // ------------------------------------------------------------------
    // M15: setPlunderTypes(List) — 2 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M15-TP1: ENTRY -> N0 -(B1:false)-> N1 -> N3 -> EXIT (this.plunderTypes == null -> allocates list)
    // M15-TP2: ENTRY -> N0 -(B1:true)-> N2 -> N3 -> EXIT (this.plunderTypes != null -> clears and adds)
    public void testM15_TP1_and_TP2_SetPlunderTypes() {
        SettlementType st = new SettlementType("model.settlement.setp", spec());
        PlunderType pt1 = new PlunderType("model.plunder.test", spec());
        PlunderType pt2 = new PlunderType("model.plunder.test", spec());
        st.setPlunderTypes(Collections.singletonList(pt1));
        assertEquals(1, st.getPlunderTypes().size());
        st.setPlunderTypes(Collections.singletonList(pt2));
        assertEquals(1, st.getPlunderTypes().size());
        assertSame(pt2, st.getPlunderTypes().get(0));
    }

    // ------------------------------------------------------------------
    // M16: getPlunderRange(Unit) — 3 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M16-TP1: ENTRY -> N0 -(B1:false)-> N1 -> EXIT (plunderTypes == null -> returns null)
    public void testM16_TP1_GetPlunderRangeNullList() {
        SettlementType st = new SettlementType("model.settlement.pr1", spec());
        Unit unit = new ServerUnit(game, null, dutch, colonistType);
        assertNull(st.getPlunderRange(unit));
    }

    // M16-TP2: ENTRY -> N0 -(B1:true)-> N2 -(B2:true)-> N4 -> N5 -> EXIT (no matching PlunderType -> null)
    public void testM16_TP2_GetPlunderRangeNoMatch() throws Exception {
        String xml = "<settlementType id=\"model.settlement.pr2\">"
                + "<plunder minimum=\"10\" maximum=\"20\" factor=\"1\">"
                + "<scope type=\"model.unit.artillery\"/>"
                + "</plunder></settlementType>";
        SettlementType st = readFromXml(xml);
        Unit unit = new ServerUnit(game, null, dutch, colonistType);
        assertNull(st.getPlunderRange(unit));
    }

    // M16-TP3: ENTRY -> N0 -(B1:true)-> N2 -(B2:false)-> N3 -> N5 -> EXIT (matching PlunderType -> RandomRange)
    public void testM16_TP3_GetPlunderRangeMatch() throws Exception {
        String xml = "<settlementType id=\"model.settlement.pr3\">"
                + "<plunder minimum=\"10\" maximum=\"20\" factor=\"1\"/>"
                + "</settlementType>";
        SettlementType st = readFromXml(xml);
        Unit unit = new ServerUnit(game, null, dutch, colonistType);
        assertNotNull(st.getPlunderRange(unit));
    }

    // ------------------------------------------------------------------
    // M17: addPlunderType(PlunderType) — 2 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M17-TP1: ENTRY -> N0 -(B1:false)-> N1 -> N2 -> EXIT (plunderTypes == null -> initializes list)
    // M17-TP2: ENTRY -> N0 -(B1:true)-> N2 -> EXIT (plunderTypes != null -> appends)
    public void testM17_TP1_and_TP2_AddPlunderType() throws Exception {
        SettlementType st = new SettlementType("model.settlement.addp", spec());
        invokeAddPlunderType(st, new PlunderType("model.plunder.test", spec()));
        invokeAddPlunderType(st, new PlunderType("model.plunder.test", spec()));
        assertEquals(2, st.getPlunderTypes().size());
    }

    // ------------------------------------------------------------------
    // M18: getGifts() — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M18-TP1: ENTRY -> N0 -> EXIT (returns gifts RandomRange)
    public void testM18_TP1_GetGifts() {
        SettlementType st = new SettlementType("model.settlement.gifts", spec());
        assertNull(st.getGifts());
    }

    // ------------------------------------------------------------------
    // M19: getWarehouseCapacity() — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M19-TP1: ENTRY -> N0 -> EXIT (returns CARGO_SIZE * getClaimableRadius())
    public void testM19_TP1_GetWarehouseCapacity() {
        SettlementType st = new SettlementType("model.settlement.wh", spec());
        assertEquals(GoodsContainer.CARGO_SIZE * st.getClaimableRadius(), st.getWarehouseCapacity());
    }

    // ------------------------------------------------------------------
    // M20: copyIn(FreeColObject) — 2 feasible Test Paths (100% feasible C3)
    // Infeasible: N1 -> N2 (B2:false, !super.copyIn(o))
    // ------------------------------------------------------------------

    // M20-TP1: ENTRY -> N0 -(B1:true)-> N2 -> EXIT (other == null or wrong type -> false)
    public void testM20_TP1_CopyInNullReturnsFalse() {
        SettlementType st = new SettlementType("model.settlement.c1", spec());
        assertFalse(st.copyIn(null));
    }

    // M20-TP2: ENTRY -> N0 -(B1:false)-> N1 -(B2:true)-> N3 -> EXIT (valid SettlementType -> copies all fields)
    public void testM20_TP2_CopyInValidCopiesFields() throws Exception {
        String xml = "<settlementType id=\"model.settlement.src\" capital=\"true\" "
                + "minimum-size=\"5\" maximum-size=\"15\" visible-radius=\"3\" "
                + "claimable-radius=\"2\" extra-claimable-radius=\"4\" wandering-radius=\"6\" "
                + "minimum-growth=\"2\" maximum-growth=\"12\" trade-bonus=\"3\" convert-threshold=\"200\">"
                + "<gifts minimum=\"5\" maximum=\"15\" factor=\"1\"/>"
                + "</settlementType>";
        SettlementType src = readFromXml(xml);
        SettlementType dst = new SettlementType("model.settlement.src", spec());
        dst.getFeatureContainer();
        assertTrue(dst.copyIn(src));
        assertTrue(dst.isCapital());
        assertEquals(5, dst.getMinimumSize());
        assertEquals(15, dst.getMaximumSize());
        assertEquals(3, dst.getVisibleRadius());
        assertEquals(2, dst.getClaimableRadius());
        assertEquals(4, dst.getExtraClaimableRadius());
        assertEquals(6, dst.getWanderingRadius());
        assertEquals(2, dst.getMinimumGrowth());
        assertEquals(12, dst.getMaximumGrowth());
        assertEquals(3, dst.getTradeBonus());
        assertEquals(200, dst.getConvertThreshold());
        assertNotNull(dst.getGifts());
    }

    // ------------------------------------------------------------------
    // M21: writeAttributes(FreeColXMLWriter) — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M21-TP1: ENTRY -> N0 -> EXIT (writes all settlement type attributes)
    public void testM21_TP1_WriteAttributes() throws Exception {
        SettlementType st = new SettlementType("model.settlement.wa", spec());
        String xml = serializeToXml(st);
        assertTrue(xml.contains("capital=\"false\""));
        assertTrue(xml.contains("minimum-size=\"3\""));
    }

    // ------------------------------------------------------------------
    // M22: writeChildren(FreeColXMLWriter) — 2 Test Paths covering B1..B3 (100% C3)
    // ------------------------------------------------------------------

    // M22-TP1: ENTRY -> N0 -(B1:true)-> N4 -(B3:true)-> N6 -> EXIT (plunderTypes == null, gifts == null)
    public void testM22_TP1_WriteChildrenEmpty() throws Exception {
        SettlementType st = new SettlementType("model.settlement.wc1", spec());
        String xml = serializeToXml(st);
        assertFalse(xml.contains("<gifts"));
    }

    // M22-TP2: ENTRY -> N0 -(B1:false)-> N1 -> N2 -(B2:false)-> N3 -> N2 -(B2:true)-> N4 -(B3:false)-> N5 -> N6 -> EXIT
    public void testM22_TP2_WriteChildrenWithPlunderAndGifts() throws Exception {
        String xml = "<settlementType id=\"model.settlement.wc2\">"
                + "<plunder minimum=\"10\" maximum=\"20\" factor=\"1\"/>"
                + "<gifts minimum=\"5\" maximum=\"15\" factor=\"1\"/>"
                + "</settlementType>";
        SettlementType st = readFromXml(xml);
        String out = serializeToXml(st);
        assertTrue(out.contains("<plunder"));
        assertTrue(out.contains("<gifts"));
    }

    // ------------------------------------------------------------------
    // M23: readAttributes(FreeColXMLReader) — 2 Test Paths covering B1..B10 (100% C3)
    // ------------------------------------------------------------------

    // M23-TP1: B1..B10 true (new hyphenated attribute names)
    public void testM23_TP1_ReadAttributesNewFormat() throws Exception {
        String xml = "<settlementType id=\"model.settlement.ra1\" capital=\"true\" "
                + "minimum-size=\"4\" maximum-size=\"12\" visible-radius=\"3\" "
                + "claimable-radius=\"2\" extra-claimable-radius=\"3\" wandering-radius=\"5\" "
                + "minimum-growth=\"2\" maximum-growth=\"8\" trade-bonus=\"2\" convert-threshold=\"150\"/>";
        SettlementType st = readFromXml(xml);
        assertEquals(4, st.getMinimumSize());
        assertEquals(12, st.getMaximumSize());
        assertEquals(150, st.getConvertThreshold());
    }

    // M23-TP2: B1..B10 false (legacy @compat 0.11.3 camelCase attribute names)
    public void testM23_TP2_ReadAttributesOldFormat() throws Exception {
        String xml = "<settlementType id=\"model.settlement.ra2\" capital=\"false\" "
                + "minimumSize=\"4\" maximumSize=\"12\" visibleRadius=\"3\" "
                + "claimableRadius=\"2\" extraClaimableRadius=\"3\" wanderingRadius=\"5\" "
                + "minimumGrowth=\"2\" maximumGrowth=\"8\" tradeBonus=\"2\" convertThreshold=\"150\"/>";
        SettlementType st = readFromXml(xml);
        assertEquals(4, st.getMinimumSize());
        assertEquals(12, st.getMaximumSize());
        assertEquals(3, st.getVisibleRadius());
        assertEquals(2, st.getClaimableRadius());
        assertEquals(3, st.getExtraClaimableRadius());
        assertEquals(5, st.getWanderingRadius());
        assertEquals(2, st.getMinimumGrowth());
        assertEquals(8, st.getMaximumGrowth());
        assertEquals(2, st.getTradeBonus());
        assertEquals(150, st.getConvertThreshold());
    }

    // ------------------------------------------------------------------
    // M24: clearContainers(FreeColXMLReader) — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M24-TP1: ENTRY -> N0 -> EXIT (resets plunderTypes and gifts to null during readFromXML)
    public void testM24_TP1_ClearContainers() throws Exception {
        String xml = "<settlementType id=\"model.settlement.cc\">"
                + "<gifts minimum=\"1\" maximum=\"2\" factor=\"1\"/>"
                + "</settlementType>";
        SettlementType st = readFromXml(xml);
        assertNotNull(st.getGifts());
        try (FreeColXMLReader xr = new FreeColXMLReader(new StringReader("<settlementType id=\"model.settlement.cc\"/>"))) {
            xr.nextTag();
            st.readFromXML(xr);
        }
        assertNull(st.getGifts());
    }

    // ------------------------------------------------------------------
    // M25: readChildren(FreeColXMLReader) — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M25-TP1: ENTRY -> N0 -> EXIT (delegates to super.readChildren)
    public void testM25_TP1_ReadChildren() throws Exception {
        SettlementType st = readFromXml("<settlementType id=\"model.settlement.rc\"/>");
        assertNotNull(st);
    }

    // ------------------------------------------------------------------
    // M26: readChild(FreeColXMLReader) — 3 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M26-TP1: ENTRY -> N0 -(B1:false)-> N1 -> N5 -> EXIT (GIFTS_TAG)
    // M26-TP2: ENTRY -> N0 -(B1:true)-> N2 -(B2:false)-> N3 -> N5 -> EXIT (PlunderType.TAG)
    // M26-TP3: ENTRY -> N0 -(B1:true)-> N2 -(B2:true)-> N4 -> N5 -> EXIT (super.readChild, e.g. ability)
    public void testM26_TP1_to_TP3_ReadChildBranches() throws Exception {
        String xml = "<settlementType id=\"model.settlement.rchild\">"
                + "<gifts minimum=\"5\" maximum=\"10\" factor=\"1\"/>"
                + "<plunder minimum=\"10\" maximum=\"20\" factor=\"1\"/>"
                + "<ability id=\"model.ability.canBeCaptured\" value=\"true\"/>"
                + "</settlementType>";
        SettlementType st = readFromXml(xml);
        assertNotNull(st.getGifts());
        assertEquals(1, st.getPlunderTypes().size());
        assertTrue(st.hasAbility("model.ability.canBeCaptured"));
    }

    // ------------------------------------------------------------------
    // M27: getXMLTagName() — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M27-TP1: ENTRY -> N0 -> EXIT (returns SettlementType.TAG)
    public void testM27_TP1_GetXMLTagName() {
        SettlementType st = new SettlementType("model.settlement.tag", spec());
        assertEquals(SettlementType.TAG, st.getXMLTagName());
    }

    // ------------------------------------------------------------------
    // M28: lambda$getPlunderRange$0(Unit, PlunderType) — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M28-TP1: ENTRY -> N0 -> EXIT (evaluated by getPlunderRange when plunderTypes is non-empty)
    public void testM28_TP1_LambdaGetPlunderRange() throws Exception {
        testM16_TP3_GetPlunderRangeMatch();
    }
}
