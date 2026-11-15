package net.sf.freecol.common.model;

import java.io.StringReader;
import java.io.StringWriter;

import net.sf.freecol.common.io.FreeColXMLReader;
import net.sf.freecol.common.io.FreeColXMLWriter;
import net.sf.freecol.common.io.FreeColXMLWriter.WriteScope;
import net.sf.freecol.util.test.FreeColTestCase;

/**
 * CFG-driven test suite for {@link UnitTypeChange} derived from
 * {@code test_cft/graph/net/sf/freecol/common/model/UnitTypeChange.json}.
 *
 * Covers all 9 methods (M1..M9, including {@code <clinit>}) and achieves
 * 100% feasible C3 (node + edge) coverage across 14 Test Paths.
 *
 * Infeasible edges documented:
 * - M4 ({@code copyIn}): {@code N1 -> N2} ({@code B2:false}) — {@code super.copyIn(o)}
 *   delegates to {@code FreeColSpecObjectType.copyIn} and {@code FreeColObject.copyIn},
 *   which always return {@code true} whenever {@code o != null}.
 */
public class UnitTypeChangeCfgTest extends FreeColTestCase {

    private Specification spec;
    private UnitType colonistType;
    private UnitType veteranType;
    private Game game;
    private Player dutchPlayer;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        // Pre-condition: load Specification, UnitTypes, and a standard Game/Player.
        spec = spec();
        colonistType = spec.getUnitType("model.unit.freeColonist");
        veteranType = spec.getUnitType("model.unit.veteranSoldier");
        game = getStandardGame();
        dutchPlayer = game.getPlayerByNationId("model.nation.dutch");
    }

    // ------------------------------------------------------------------
    // Pre-condition helpers & stubs
    // ------------------------------------------------------------------

    private String writeXml(UnitTypeChange utc) throws Exception {
        StringWriter out = new StringWriter();
        try (FreeColXMLWriter xw = new FreeColXMLWriter(out, WriteScope.toSave(), false)) {
            utc.toXML(xw);
            xw.flush();
        }
        return out.toString();
    }

    private void readXml(UnitTypeChange utc, String xml) throws Exception {
        try (FreeColXMLReader xr = new FreeColXMLReader(new StringReader(xml))) {
            xr.nextTag();
            utc.readFromXML(xr);
        }
    }

    // ------------------------------------------------------------------
    // M1: <init>(String, Specification) — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M1-TP1: ENTRY -> N0 -> EXIT (constructs UnitTypeChange with id and specification)
    public void testM1_TP1_ConstructorWithIdAndSpec() {
        UnitTypeChange utc = new UnitTypeChange("model.unitChange.cft1", spec);
        assertEquals("model.unitChange.cft1", utc.getId());
        assertSame(spec, utc.getSpecification());
    }

    // ------------------------------------------------------------------
    // M2: <init>(FreeColXMLReader, Specification) — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M2-TP1: ENTRY -> N0 -> EXIT (constructs UnitTypeChange from XML stream)
    public void testM2_TP1_ConstructorFromXmlReader() throws Exception {
        String xml = "<unit-type-change id=\"model.unitChange.cftXml\" "
                + "from=\"model.unit.freeColonist\" to=\"model.unit.veteranSoldier\" "
                + "probability=\"50\" turns=\"3\"/>";
        try (FreeColXMLReader xr = new FreeColXMLReader(new StringReader(xml))) {
            xr.nextTag();
            UnitTypeChange utc = new UnitTypeChange(xr, spec);
            assertEquals("model.unitChange.cftXml", utc.getId());
            assertEquals(colonistType, utc.from);
            assertEquals(veteranType, utc.to);
            assertEquals(50, utc.probability);
            assertEquals(3, utc.turns);
        }
    }

    // ------------------------------------------------------------------
    // M3: isAvailableTo(Player) — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M3-TP1: ENTRY -> N0 -> EXIT (delegates availability check to target UnitType)
    public void testM3_TP1_IsAvailableToDelegatesToTargetUnitType() {
        UnitTypeChange utc = new UnitTypeChange("model.unitChange.cftAvail", spec);
        utc.from = colonistType;
        utc.to = veteranType;
        assertEquals(veteranType.isAvailableTo(dutchPlayer), utc.isAvailableTo(dutchPlayer));
    }

    // ------------------------------------------------------------------
    // M4: copyIn(FreeColObject) — 2 feasible Test Paths (100% feasible C3)
    // ------------------------------------------------------------------

    // M4-TP1: ENTRY -> N0 -(B1:true)-> N2 -> EXIT (other is null or non-UnitTypeChange -> returns false)
    public void testM4_TP1_CopyInNullOrWrongTypeReturnsFalse() {
        UnitTypeChange utc = new UnitTypeChange("model.unitChange.cftTarget", spec);
        assertFalse(utc.copyIn(null));
        assertFalse(utc.copyIn(colonistType));
    }

    // M4-TP2: ENTRY -> N0 -(B1:false)-> N1 -(B2:true)-> N3 -> EXIT (valid UnitTypeChange -> copies fields)
    public void testM4_TP2_CopyInValidUnitTypeChangeCopiesFields() {
        UnitTypeChange source = new UnitTypeChange("model.unitChange.cftSrc", spec);
        source.from = colonistType;
        source.to = veteranType;
        source.probability = 75;
        source.turns = 4;

        UnitTypeChange target = new UnitTypeChange("model.unitChange.cftSrc", spec);
        target.getFeatureContainer();
        assertTrue(target.copyIn(source));
        assertEquals(colonistType, target.from);
        assertEquals(veteranType, target.to);
        assertEquals(75, target.probability);
        assertEquals(4, target.turns);
    }

    // ------------------------------------------------------------------
    // M5: writeAttributes(FreeColXMLWriter) — 2 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M5-TP1: ENTRY -> N0 -(B1:true)-> N2 -> EXIT (turns <= 0 -> omits turns attribute)
    public void testM5_TP1_WriteAttributesWithoutTurns() throws Exception {
        UnitTypeChange utc = new UnitTypeChange("model.unitChange.cftNoTurns", spec);
        utc.from = colonistType;
        utc.to = veteranType;
        utc.probability = 100;
        utc.turns = 0;

        String xml = writeXml(utc);
        assertTrue(xml.contains("probability=\"100\""));
        assertFalse(xml.contains("turns="));
    }

    // M5-TP2: ENTRY -> N0 -(B1:false)-> N1 -> N2 -> EXIT (turns > 0 -> writes turns attribute)
    public void testM5_TP2_WriteAttributesWithPositiveTurns() throws Exception {
        UnitTypeChange utc = new UnitTypeChange("model.unitChange.cftWithTurns", spec);
        utc.from = colonistType;
        utc.to = veteranType;
        utc.probability = 25;
        utc.turns = 5;

        String xml = writeXml(utc);
        assertTrue(xml.contains("turns=\"5\""));
    }

    // ------------------------------------------------------------------
    // M6: readAttributes(FreeColXMLReader) — 3 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M6-TP1: ENTRY -> N0 -(B1:true)-> N2 -> N3 -> EXIT (id == null -> generates fake id)
    public void testM6_TP1_ReadAttributesNullIdGeneratesFakeId() throws Exception {
        UnitTypeChange utc = new UnitTypeChange((String) null, spec);
        readXml(utc, "<unit-type-change from=\"model.unit.freeColonist\" "
                + "to=\"model.unit.veteranSoldier\" probability=\"10\"/>");
        assertNotNull(utc.getId());
        assertTrue(utc.getId().startsWith("model.unitChange.faked."));
    }

    // M6-TP2: ENTRY -> N0 -(B1:false)-> N1 -(B2:true)-> N3 -> EXIT (id non-null and non-empty -> preserves id)
    public void testM6_TP2_ReadAttributesValidIdPreserved() throws Exception {
        UnitTypeChange utc = new UnitTypeChange("model.unitChange.cftKeep", spec);
        readXml(utc, "<unit-type-change id=\"model.unitChange.cftKeep\" "
                + "from=\"model.unit.freeColonist\" to=\"model.unit.veteranSoldier\" probability=\"20\"/>");
        assertEquals("model.unitChange.cftKeep", utc.getId());
    }

    // M6-TP3: ENTRY -> N0 -(B1:false)-> N1 -(B2:false)-> N2 -> N3 -> EXIT (id == "" -> generates fake id)
    public void testM6_TP3_ReadAttributesEmptyIdGeneratesFakeId() throws Exception {
        UnitTypeChange utc = new UnitTypeChange("", spec);
        readXml(utc, "<unit-type-change from=\"model.unit.freeColonist\" "
                + "to=\"model.unit.veteranSoldier\" probability=\"30\"/>");
        assertTrue(utc.getId().startsWith("model.unitChange.faked."));
    }

    // ------------------------------------------------------------------
    // M7: getXMLTagName() — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M7-TP1: ENTRY -> N0 -> EXIT (returns UnitTypeChange.TAG)
    public void testM7_TP1_GetXMLTagName() {
        UnitTypeChange utc = new UnitTypeChange("model.unitChange.cftTag", spec);
        assertEquals("unit-type-change", utc.getXMLTagName());
    }

    // ------------------------------------------------------------------
    // M8: toString() — 2 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M8-TP1: ENTRY -> N0 -(B1:true)-> N2 -> EXIT (turns <= 0 -> omits /turns suffix)
    public void testM8_TP1_ToStringWithoutTurns() {
        UnitTypeChange utc = new UnitTypeChange("model.unitChange.cftStr1", spec);
        utc.from = colonistType;
        utc.to = veteranType;
        utc.probability = 40;
        utc.turns = 0;
        assertEquals("freeColonist->veteranSoldier/40", utc.toString());
    }

    // M8-TP2: ENTRY -> N0 -(B1:false)-> N1 -> N2 -> EXIT (turns > 0 -> appends /turns suffix)
    public void testM8_TP2_ToStringWithPositiveTurns() {
        UnitTypeChange utc = new UnitTypeChange("model.unitChange.cftStr2", spec);
        utc.from = colonistType;
        utc.to = veteranType;
        utc.probability = 40;
        utc.turns = 6;
        assertEquals("freeColonist->veteranSoldier/40/6", utc.toString());
    }

    // ------------------------------------------------------------------
    // M9: <clinit>() — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M9-TP1: ENTRY -> N0 -> EXIT (static initializer runs on class load, initializing TAG and fakeIdIndex)
    public void testM9_TP1_StaticInitializer() {
        assertEquals("unit-type-change", UnitTypeChange.TAG);
    }
}
