/**
 *  Copyright (C) 2002-2024   The FreeCol Team
 *
 *  This file is part of FreeCol.
 *
 *  FreeCol is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the FreeCol Foundation, either version 2 of the License, or
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
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import javax.xml.stream.XMLStreamException;

import net.sf.freecol.FreeCol;
import net.sf.freecol.common.io.FreeColXMLReader;
import net.sf.freecol.common.io.FreeColXMLWriter;
import net.sf.freecol.common.model.NationOptions.Advantages;
import net.sf.freecol.common.model.NationOptions.NationState;
import net.sf.freecol.util.test.FreeColTestCase;


/**
 * CFG-driven test suite for {@link NationOptions} derived from
 * {@code test_cft/graph/net/sf/freecol/common/model/NationOptions.json}.
 *
 * Covers all 17 methods (M1..M17, including {@code <init>} and lambdas) and
 * achieves 100% feasible C3 (node + edge) coverage across 30 Test Paths.
 *
 * Infeasible edges documented:
 * - M8 ({@code copyIn}): {@code N1 -> N2} ({@code B2:false}, {@code !super.copyIn(o)}) —
 *   Once {@code copyInCast(other, NationOptions.class)} returns non-null {@code o},
 *   {@code o} is non-null, shares the same ID, and is an instance of
 *   {@code NationOptions} (and therefore {@code FreeColSpecObject} and
 *   {@code FreeColObject}), so {@code super.copyIn(o)} is guaranteed to return
 *   {@code true}.
 */
public class NationOptionsCfgTest extends FreeColTestCase {

    private Specification spec;
    private Nation dutch;
    private Nation french;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        // Pre-condition: Obtain standard specification and sample nations
        spec = spec();
        dutch = spec.getNation("model.nation.dutch");
        french = spec.getNation("model.nation.french");
    }

    // ------------------------------------------------------------------
    // Pre-condition helpers & stubs
    // ------------------------------------------------------------------

    /**
     * Exposes protected {@code readAttributes}, {@code readChildren}, and
     * {@code readChild} for direct CFG path testing.
     */
    private static class TestableNationOptions extends NationOptions {

        TestableNationOptions(Specification specification) {
            super(specification);
        }

        void callSetNations(Map<Nation, NationState> map) {
            setNations(map);
        }

        void callReadAttributes(FreeColXMLReader xr) throws XMLStreamException {
            readAttributes(xr);
        }

        void callReadChildren(FreeColXMLReader xr) throws XMLStreamException {
            readChildren(xr);
        }

        void callReadChild(FreeColXMLReader xr) throws XMLStreamException {
            readChild(xr);
        }
    }

    private FreeColXMLReader openReaderAtStartTag(String xml) throws Exception {
        FreeColXMLReader xr = new FreeColXMLReader(new StringReader(xml));
        xr.nextTag();
        return xr;
    }

    // ------------------------------------------------------------------
    // M1: <init>(Lnet/sf/freecol/common/model/Specification;)V — 4 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M1-TP1: ENTRY -> N0 -(B1:true)-> N10 -> EXIT (specification == null)
    public void testM1_TP1_ConstructorNullSpec() {
        NationOptions opts = new NationOptions(null);
        assertEquals(FreeCol.getAdvantages(), opts.getNationalAdvantages());
        assertTrue(opts.getNations().isEmpty());
    }

    // M1-TP2: ENTRY -> N0 -(B1:false)-> N1 -> N2 -(B2:true)-> N10 -> EXIT (specification != null, empty nations list)
    public void testM1_TP2_ConstructorEmptySpec() {
        Specification emptySpec = new Specification();
        NationOptions opts = new NationOptions(emptySpec);
        assertTrue(opts.getNations().isEmpty());
    }

    // M1-TP3: ENTRY -> N0 -(B1:false)-> N1 -> N2 -(B2:false)-> N3 -(B3:false)-> N4 -(B4:false)-> N5 -(B5:false/true)-> N6/N7 -> N9 -> N2 -(B3:true)-> N8 -> N9 -> N2 -(B2:true)-> N10 -> EXIT
    public void testM1_TP3_ConstructorStandardSpec() {
        NationOptions opts = new NationOptions(spec);
        assertFalse(opts.getNations().isEmpty());
        assertEquals(NationState.AVAILABLE, opts.getNationState(dutch));
        // Native nations are non-European -> AI_ONLY (B3:true)
        Nation apache = spec.getNation("model.nation.apache");
        assertEquals(NationState.AI_ONLY, opts.getNationState(apache));
        // Standard spec has >4 selectable European nations, so later ones hit NOT_AVAILABLE (B5:true)
        assertTrue(opts.getNations().containsValue(NationState.NOT_AVAILABLE));
    }

    // M1-TP4: ENTRY -> N0 -(B1:false)-> N1 -> N2 -(B2:false)-> N3 -(B3:false)-> N4 -(B4:true)-> N8 -> N9 -> N2 -(B2:true)-> N10 -> EXIT (European non-selectable nation -> AI_ONLY)
    public void testM1_TP4_ConstructorEuropeanNonSelectable() throws Exception {
        Specification customSpec = new Specification();
        Method addType = Specification.class.getDeclaredMethod(
                "addType", String.class, FreeColSpecObjectType.class);
        addType.setAccessible(true);
        NationType euroType = new EuropeanNationType("model.nationType.cfgEuro", customSpec);
        addType.invoke(customSpec, euroType.getId(), euroType);
        Nation nonSelectableEuro = new Nation("model.nation.cfgNonSel", customSpec);
        nonSelectableEuro.setType(euroType);
        Field selectableField = Nation.class.getDeclaredField("selectable");
        selectableField.setAccessible(true);
        selectableField.setBoolean(nonSelectableEuro, false);
        addType.invoke(customSpec, nonSelectableEuro.getId(), nonSelectableEuro);
        Field nationsField = Specification.class.getDeclaredField("nations");
        nationsField.setAccessible(true);
        @SuppressWarnings("unchecked")
        java.util.List<Nation> nationsList = (java.util.List<Nation>) nationsField.get(customSpec);
        nationsList.add(nonSelectableEuro);

        NationOptions opts = new NationOptions(customSpec);
        assertEquals(NationState.AI_ONLY, opts.getNationState(nonSelectableEuro));
    }

    // ------------------------------------------------------------------
    // M2: <init>(Lnet/sf/freecol/common/io/FreeColXMLReader;Lnet/sf/freecol/common/model/Specification;)V — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M2-TP1: ENTRY -> N0 -> EXIT (Construct NationOptions from XML reader)
    public void testM2_TP1_ConstructorFromXML() throws Exception {
        String xml = "<nationOptions nationalAdvantages=\"fixed\">"
                + "<nationOption id=\"model.nation.dutch\" state=\"available\"/>"
                + "</nationOptions>";
        try (FreeColXMLReader xr = openReaderAtStartTag(xml)) {
            NationOptions opts = new NationOptions(xr, spec);
            assertEquals(Advantages.FIXED, opts.getNationalAdvantages());
            assertEquals(NationState.AVAILABLE, opts.getNationState(dutch));
        }
    }

    // ------------------------------------------------------------------
    // M3: getNationalAdvantages()Lnet/sf/freecol/common/model/NationOptions$Advantages; — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M3-TP1: ENTRY -> N0 -> EXIT (Return nationalAdvantages)
    public void testM3_TP1_GetNationalAdvantages() {
        NationOptions opts = new NationOptions(spec);
        assertEquals(FreeCol.getAdvantages(), opts.getNationalAdvantages());
    }

    // ------------------------------------------------------------------
    // M4: getNations()Ljava/util/Map; — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M4-TP1: ENTRY -> N0 -> EXIT (Return nations map)
    public void testM4_TP1_GetNations() {
        NationOptions opts = new NationOptions(spec);
        assertNotNull(opts.getNations());
        assertTrue(opts.getNations().containsKey(dutch));
    }

    // ------------------------------------------------------------------
    // M5: setNations(Ljava/util/Map;)V — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M5-TP1: ENTRY -> N0 -> EXIT (Clear and putAll nations map)
    public void testM5_TP1_SetNations() {
        TestableNationOptions opts = new TestableNationOptions(spec);
        Map<Nation, NationState> map = new HashMap<>();
        map.put(dutch, NationState.AI_ONLY);
        opts.callSetNations(map);
        assertEquals(1, opts.getNations().size());
        assertEquals(NationState.AI_ONLY, opts.getNationState(dutch));
    }

    // ------------------------------------------------------------------
    // M6: getNationState(Lnet/sf/freecol/common/model/Nation;)Lnet/sf/freecol/common/model/NationOptions$NationState; — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M6-TP1: ENTRY -> N0 -> EXIT (Return NationState for nation)
    public void testM6_TP1_GetNationState() {
        NationOptions opts = new NationOptions(spec);
        assertEquals(NationState.AVAILABLE, opts.getNationState(dutch));
    }

    // ------------------------------------------------------------------
    // M7: setNationState(Lnet/sf/freecol/common/model/Nation;Lnet/sf/freecol/common/model/NationOptions$NationState;)V — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M7-TP1: ENTRY -> N0 -> EXIT (Set NationState for nation)
    public void testM7_TP1_SetNationState() {
        NationOptions opts = new NationOptions(spec);
        opts.setNationState(dutch, NationState.NOT_AVAILABLE);
        assertEquals(NationState.NOT_AVAILABLE, opts.getNationState(dutch));
    }

    // ------------------------------------------------------------------
    // M8: copyIn(Lnet/sf/freecol/common/model/FreeColObject;)Z — 2 feasible Test Paths (100% feasible C3)
    // ------------------------------------------------------------------

    // M8-TP1: ENTRY -> N0 -(B1:true)-> N2 -> EXIT (o == null -> return false)
    public void testM8_TP1_CopyInNullOrWrongType() {
        NationOptions opts = new NationOptions(spec);
        assertFalse(opts.copyIn(null));

        NationOptions otherId = new NationOptions(spec);
        otherId.setId("different-id");
        assertFalse(opts.copyIn(otherId));
    }

    // M8-TP2: ENTRY -> N0 -(B1:false)-> N1 -(B2:true)-> N3 -> EXIT (o != null && super.copyIn(o) -> copy fields and return true)
    public void testM8_TP2_CopyInValid() throws Exception {
        NationOptions target = new NationOptions(null);
        target.setId("opts-1");
        NationOptions source = new NationOptions(null);
        source.setId("opts-1");
        source.setNationState(french, NationState.AI_ONLY);
        assertTrue(target.copyIn(source));
        assertEquals(NationState.AI_ONLY, target.getNationState(french));
    }

    // ------------------------------------------------------------------
    // M9: writeAttributes(Lnet/sf/freecol/common/io/FreeColXMLWriter;)V — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M9-TP1: ENTRY -> N0 -> EXIT (Write nationalAdvantages attribute)
    public void testM9_TP1_WriteAttributes() throws Exception {
        NationOptions opts = new NationOptions(null);
        StringWriter sw = new StringWriter();
        try (FreeColXMLWriter xw = new FreeColXMLWriter(sw, FreeColXMLWriter.WriteScope.toSave())) {
            xw.writeStartDocument("UTF-8", "1.0");
            xw.writeStartElement(NationOptions.TAG);
            opts.writeAttributes(xw);
            xw.writeEndElement();
            xw.writeEndDocument();
        }
        assertTrue(sw.toString().contains("nationalAdvantages=\""));
    }

    // ------------------------------------------------------------------
    // M10: writeChildren(Lnet/sf/freecol/common/io/FreeColXMLWriter;)V — 2 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M10-TP1: ENTRY -> N0 -> N1 -(B1:true)-> N3 -> EXIT (nations empty -> loop skipped)
    public void testM10_TP1_WriteChildrenEmpty() throws Exception {
        NationOptions opts = new NationOptions(null);
        StringWriter sw = new StringWriter();
        try (FreeColXMLWriter xw = new FreeColXMLWriter(sw, FreeColXMLWriter.WriteScope.toSave())) {
            xw.writeStartDocument("UTF-8", "1.0");
            xw.writeStartElement(NationOptions.TAG);
            opts.writeChildren(xw);
            xw.writeEndElement();
            xw.writeEndDocument();
        }
        assertFalse(sw.toString().contains("<nationOption "));
    }

    // M10-TP2: ENTRY -> N0 -> N1 -(B1:false)-> N2 -> N1 -(B1:true)-> N3 -> EXIT (nations non-empty -> loop body executed)
    public void testM10_TP2_WriteChildrenNonEmpty() throws Exception {
        NationOptions opts = new NationOptions(null);
        opts.setNationState(dutch, NationState.AVAILABLE);
        StringWriter sw = new StringWriter();
        try (FreeColXMLWriter xw = new FreeColXMLWriter(sw, FreeColXMLWriter.WriteScope.toSave())) {
            xw.writeStartDocument("UTF-8", "1.0");
            xw.writeStartElement(NationOptions.TAG);
            opts.writeChildren(xw);
            xw.writeEndElement();
            xw.writeEndDocument();
        }
        assertTrue(sw.toString().contains("<nationOption "));
        assertTrue(sw.toString().contains("id=\"model.nation.dutch\""));
    }

    // ------------------------------------------------------------------
    // M11: readAttributes(Lnet/sf/freecol/common/io/FreeColXMLReader;)V — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M11-TP1: ENTRY -> N0 -> EXIT (Read nationalAdvantages attribute)
    public void testM11_TP1_ReadAttributes() throws Exception {
        TestableNationOptions opts = new TestableNationOptions(spec);
        try (FreeColXMLReader xr = openReaderAtStartTag("<nationOptions nationalAdvantages=\"none\"/>")) {
            opts.callReadAttributes(xr);
        }
        assertEquals(Advantages.NONE, opts.getNationalAdvantages());
    }

    // ------------------------------------------------------------------
    // M12: readChildren(Lnet/sf/freecol/common/io/FreeColXMLReader;)V — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M12-TP1: ENTRY -> N0 -> EXIT (Clear nations and call super.readChildren)
    public void testM12_TP1_ReadChildren() throws Exception {
        TestableNationOptions opts = new TestableNationOptions(spec);
        assertFalse(opts.getNations().isEmpty());
        try (FreeColXMLReader xr = openReaderAtStartTag("<nationOptions/>")) {
            opts.callReadChildren(xr);
        }
        assertTrue(opts.getNations().isEmpty());
    }

    // ------------------------------------------------------------------
    // M13: readChild(Lnet/sf/freecol/common/io/FreeColXMLReader;)V — 9 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M13-TP1: ENTRY -> N0 -(B1:false)-> N1 -(B2:true)-> N4 -> N14 -> EXIT (nationOption with missing/unknown nation)
    public void testM13_TP1_ReadChildNationOptionNullNation() throws Exception {
        TestableNationOptions opts = new TestableNationOptions(spec);
        opts.getNations().clear();
        try (FreeColXMLReader xr = openReaderAtStartTag(
                "<nationOptions><nationOption state=\"available\"/></nationOptions>")) {
            xr.nextTag();
            opts.callReadChild(xr);
        }
        assertTrue(opts.getNations().isEmpty());
    }

    // M13-TP2: ENTRY -> N0 -(B1:false)-> N1 -(B2:false)-> N2 -(B3:true)-> N4 -> N14 -> EXIT (nationOption with valid nation, null state)
    public void testM13_TP2_ReadChildNationOptionNullState() throws Exception {
        TestableNationOptions opts = new TestableNationOptions(spec);
        opts.getNations().clear();
        try (FreeColXMLReader xr = openReaderAtStartTag(
                "<nationOptions><nationOption id=\"model.nation.dutch\"/></nationOptions>")) {
            xr.nextTag();
            opts.callReadChild(xr);
        }
        assertTrue(opts.getNations().isEmpty());
    }

    // M13-TP3: ENTRY -> N0 -(B1:false)-> N1 -(B2:false)-> N2 -(B3:false)-> N3 -> N4 -> N14 -> EXIT (nationOption with valid nation and state)
    public void testM13_TP3_ReadChildNationOptionValid() throws Exception {
        TestableNationOptions opts = new TestableNationOptions(spec);
        opts.getNations().clear();
        try (FreeColXMLReader xr = openReaderAtStartTag(
                "<nationOptions><nationOption id=\"model.nation.dutch\" state=\"available\"/></nationOptions>")) {
            xr.nextTag();
            opts.callReadChild(xr);
        }
        assertEquals(NationState.AVAILABLE, opts.getNationState(dutch));
    }

    // M13-TP4: ENTRY -> N0 -(B1:true)-> N5 -(B4:false)-> N6 -(B5:true)-> N14 -> EXIT (OLD_NATIONS_TAG empty)
    public void testM13_TP4_ReadChildOldNationsEmpty() throws Exception {
        TestableNationOptions opts = new TestableNationOptions(spec);
        opts.getNations().clear();
        try (FreeColXMLReader xr = openReaderAtStartTag("<nationOptions><Nations/></nationOptions>")) {
            xr.nextTag();
            opts.callReadChild(xr);
        }
        assertTrue(opts.getNations().isEmpty());
    }

    // M13-TP5: ENTRY -> N0 -(B1:true)-> N5 -(B4:false)-> N6 -(B5:false)-> N7 -(B6:true)-> N12 -> EXIT (OLD_NATIONS_TAG with bogus inner tag throws XMLStreamException)
    public void testM13_TP5_ReadChildOldNationsBogusTagThrows() throws Exception {
        TestableNationOptions opts = new TestableNationOptions(spec);
        try (FreeColXMLReader xr = openReaderAtStartTag(
                "<nationOptions><Nations><Bogus/></Nations></nationOptions>")) {
            xr.nextTag();
            try {
                opts.callReadChild(xr);
                fail("Expected XMLStreamException for bogus inner tag");
            } catch (XMLStreamException expected) {
                assertTrue(expected.getMessage().contains("Bogus"));
            }
        }
    }

    // M13-TP6: ENTRY -> N0 -(B1:true)-> N5 -(B4:false)-> N6 -(B5:false)-> N7 -(B6:false)-> N8 -(B7:true)-> N11 -> N6 -(B5:true)-> N14 -> EXIT (OLD_NATION_TAG with null nation)
    public void testM13_TP6_ReadChildOldNationsNullNation() throws Exception {
        TestableNationOptions opts = new TestableNationOptions(spec);
        opts.getNations().clear();
        try (FreeColXMLReader xr = openReaderAtStartTag(
                "<nationOptions><Nations><Nation state=\"available\"/></Nations></nationOptions>")) {
            xr.nextTag();
            opts.callReadChild(xr);
        }
        assertTrue(opts.getNations().isEmpty());
    }

    // M13-TP7: ENTRY -> N0 -(B1:true)-> N5 -(B4:false)-> N6 -(B5:false)-> N7 -(B6:false)-> N8 -(B7:false)-> N9 -(B8:true)-> N11 -> N6 -(B5:true)-> N14 -> EXIT (OLD_NATION_TAG with valid nation, null state)
    public void testM13_TP7_ReadChildOldNationsNullState() throws Exception {
        TestableNationOptions opts = new TestableNationOptions(spec);
        opts.getNations().clear();
        try (FreeColXMLReader xr = openReaderAtStartTag(
                "<nationOptions><Nations><Nation id=\"model.nation.dutch\"/></Nations></nationOptions>")) {
            xr.nextTag();
            opts.callReadChild(xr);
        }
        assertTrue(opts.getNations().isEmpty());
    }

    // M13-TP8: ENTRY -> N0 -(B1:true)-> N5 -(B4:false)-> N6 -(B5:false)-> N7 -(B6:false)-> N8 -(B7:false)-> N9 -(B8:false)-> N10 -> N11 -> N6 -(B5:true)-> N14 -> EXIT (OLD_NATION_TAG with valid nation and state)
    public void testM13_TP8_ReadChildOldNationsValid() throws Exception {
        TestableNationOptions opts = new TestableNationOptions(spec);
        opts.getNations().clear();
        try (FreeColXMLReader xr = openReaderAtStartTag(
                "<nationOptions><Nations><Nation id=\"model.nation.dutch\" state=\"ai_only\"/></Nations></nationOptions>")) {
            xr.nextTag();
            opts.callReadChild(xr);
        }
        assertEquals(NationState.AI_ONLY, opts.getNationState(dutch));
    }

    // M13-TP9: ENTRY -> N0 -(B1:true)-> N5 -(B4:true)-> N13 -> N14 -> EXIT (Unknown tag -> super.readChild)
    public void testM13_TP9_ReadChildUnknownTag() throws Exception {
        TestableNationOptions opts = new TestableNationOptions(spec);
        try (FreeColXMLReader xr = openReaderAtStartTag(
                "<nationOptions><unknownTag/></nationOptions>")) {
            xr.nextTag();
            try {
                opts.callReadChild(xr);
                fail("Expected XMLStreamException from super.readChild");
            } catch (XMLStreamException expected) {
                assertTrue(expected.getMessage().contains("unknownTag"));
            }
        }
    }

    // ------------------------------------------------------------------
    // M14: getXMLTagName()Ljava/lang/String; — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M14-TP1: ENTRY -> N0 -> EXIT (Return "nationOptions")
    public void testM14_TP1_GetXMLTagName() {
        NationOptions opts = new NationOptions(null);
        assertEquals("nationOptions", opts.getXMLTagName());
    }

    // ------------------------------------------------------------------
    // M15: toString()Ljava/lang/String; — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M15-TP1: ENTRY -> N0 -> EXIT (Format nationalAdvantages and nations)
    public void testM15_TP1_ToString() {
        NationOptions opts = new NationOptions(null);
        opts.setNationState(dutch, NationState.AVAILABLE);
        String s = opts.toString();
        assertTrue(s.contains("nationalAdvantages:"));
        assertTrue(s.contains("model.nation.dutch"));
    }

    // ------------------------------------------------------------------
    // M16: lambda$toString$0(Ljava/lang/StringBuilder;Ljava/util/Map$Entry;)V — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M16-TP1: ENTRY -> N0 -> EXIT (Executed via forEachMapEntry in toString with non-empty nations)
    public void testM16_TP1_LambdaToString() {
        NationOptions opts = new NationOptions(null);
        opts.setNationState(french, NationState.NOT_AVAILABLE);
        String s = opts.toString();
        assertTrue(s.contains("model.nation.french NOT_AVAILABLE"));
    }

    // ------------------------------------------------------------------
    // M17: lambda$new$0(Lnet/sf/freecol/common/model/Nation;)Z — 3 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M17-TP1..TP3: ENTRY -> N0 -(B1:true/false)-> N1 -(B2:true/false)-> N2/N3 -> N4 -> EXIT
    public void testM17_TP1_TP3_LambdaNewFilterNations() {
        NationOptions opts = new NationOptions(spec);
        // B1:true (unknownEnemy excluded)
        assertNull(opts.getNationState(spec.getNation("model.nation.unknownEnemy")));
        // B1:false, B2:true (REF nation excluded)
        assertNull(opts.getNationState(spec.getNation("model.nation.dutchREF")));
        // B1:false, B2:false (normal nation included)
        assertNotNull(opts.getNationState(dutch));
    }
}
