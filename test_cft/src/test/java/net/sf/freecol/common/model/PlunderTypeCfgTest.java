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


/**
 * CFG-driven test suite for {@link PlunderType} derived from
 * {@code test_cft/graph/net/sf/freecol/common/model/PlunderType.json}.
 *
 * Covers all 7 methods (M1..M7, including both {@code <init>} constructors)
 * and achieves 100% feasible C3 (node + edge) coverage across 9 Test Paths.
 *
 * Infeasible edges documented:
 * - M4 ({@code copyIn}): {@code N1 -> N2} ({@code B2:false}, {@code !super.copyIn(o)}) —
 *   When {@code o != null}, {@code copyInCast(other, PlunderType.class)} has
 *   already verified {@code other != null}, {@code idEquals(other)}, and
 *   {@code o instanceof PlunderType}; the superclass chain
 *   ({@code FreeColSpecObjectType} -> {@code FreeColSpecObject} -> {@code FreeColObject})
 *   only performs {@code copyInCast} checks on superclasses of {@code PlunderType},
 *   which are guaranteed {@code true}.
 */
public class PlunderTypeCfgTest extends FreeColTestCase {

    private Specification spec;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        // Pre-condition: Obtain standard specification from FreeColTestCase
        spec = spec();
    }

    // ------------------------------------------------------------------
    // Pre-condition helpers & stubs
    // ------------------------------------------------------------------

    /**
     * Create a {@link PlunderType} from an XML element string using
     * {@link PlunderType#PlunderType(FreeColXMLReader, Specification)}.
     */
    private PlunderType readPlunderTypeFromXml(String xml) throws Exception {
        try (FreeColXMLReader xr = new FreeColXMLReader(new StringReader(xml))) {
            xr.nextTag();
            return new PlunderType(xr, spec);
        }
    }

    // ------------------------------------------------------------------
    // M1: <init>(Ljava/lang/String;Lnet/sf/freecol/common/model/Specification;)V — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M1-TP1: ENTRY -> N0 -> EXIT (Construct PlunderType with id and specification)
    public void testM1_TP1_ConstructorWithIdAndSpec() {
        PlunderType pt = new PlunderType("model.plunder.cfgTest", spec);
        assertEquals("model.plunder.cfgTest", pt.getId());
        assertSame(spec, pt.getSpecification());
        assertNull(pt.getPlunder());
    }

    // ------------------------------------------------------------------
    // M2: <init>(Lnet/sf/freecol/common/io/FreeColXMLReader;Lnet/sf/freecol/common/model/Specification;)V — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M2-TP1: ENTRY -> N0 -> EXIT (Construct PlunderType from FreeColXMLReader)
    public void testM2_TP1_ConstructorFromXmlReader() throws Exception {
        String xml = "<plunder id=\"model.plunder.xmlTest\" probability=\"75\" minimum=\"10\" maximum=\"50\" factor=\"2\"/>";
        PlunderType pt = readPlunderTypeFromXml(xml);
        assertEquals("model.plunder.xmlTest", pt.getId());
        assertSame(spec, pt.getSpecification());
        assertNotNull(pt.getPlunder());
        assertEquals(75, pt.getPlunder().getProbability());
        assertEquals(10, pt.getPlunder().getMinimum());
        assertEquals(50, pt.getPlunder().getMaximum());
        assertEquals(2, pt.getPlunder().getFactor());
    }

    // ------------------------------------------------------------------
    // M3: getPlunder()Lnet/sf/freecol/common/model/RandomRange; — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M3-TP1: ENTRY -> N0 -> EXIT (Return plunder RandomRange, null or populated)
    public void testM3_TP1_GetPlunder() throws Exception {
        PlunderType empty = new PlunderType("model.plunder.empty", spec);
        assertNull(empty.getPlunder());

        String xml = "<plunder id=\"model.plunder.filled\" probability=\"100\" minimum=\"5\" maximum=\"15\" factor=\"1\"/>";
        PlunderType filled = readPlunderTypeFromXml(xml);
        assertNotNull(filled.getPlunder());
        assertEquals(5, filled.getPlunder().getMinimum());
    }

    // ------------------------------------------------------------------
    // M4: copyIn(Lnet/sf/freecol/common/model/FreeColObject;)Z — 2 feasible Test Paths (100% feasible C3)
    // ------------------------------------------------------------------

    // M4-TP1: ENTRY -> N0 -(B1:true)-> N2 -> EXIT (o == null via null, wrong type, or different ID)
    public void testM4_TP1_CopyInReturnsFalseWhenCastNull() {
        PlunderType target = new PlunderType("model.plunder.target", spec);
        PlunderType otherId = new PlunderType("model.plunder.other", spec);
        GoodsType wrongType = spec.getGoodsType("model.goods.food");

        assertFalse(target.copyIn(null));
        assertFalse(target.copyIn(otherId));
        assertFalse(target.copyIn(wrongType));
    }

    // M4-TP2: ENTRY -> N0 -(B1:false)-> N1 -(B2:true)-> N3 -> EXIT (o != null && super.copyIn(o) == true)
    public void testM4_TP2_CopyInCopiesPlunderRange() throws Exception {
        PlunderType target = new PlunderType("model.plunder.sameId", spec);
        target.getFeatureContainer();
        String xml = "<plunder id=\"model.plunder.sameId\" probability=\"60\" minimum=\"20\" maximum=\"80\" factor=\"3\"/>";
        PlunderType source = readPlunderTypeFromXml(xml);

        assertTrue(target.copyIn(source));
        assertSame(source.getPlunder(), target.getPlunder());
    }

    // ------------------------------------------------------------------
    // M5: writeAttributes(Lnet/sf/freecol/common/io/FreeColXMLWriter;)V — 2 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M5-TP1: ENTRY -> N0 -(B1:true)-> N2 -> EXIT (this.plunder == null)
    public void testM5_TP1_WriteAttributesWithNullPlunder() throws Exception {
        PlunderType pt = new PlunderType("model.plunder.nullRange", spec);
        StringWriter out = new StringWriter();
        try (FreeColXMLWriter xw = new FreeColXMLWriter(out, FreeColXMLWriter.WriteScope.toSave(), false)) {
            pt.toXML(xw);
        }
        String xmlOut = out.toString();
        assertTrue(xmlOut.contains("id=\"model.plunder.nullRange\""));
        assertFalse(xmlOut.contains("probability="));
    }

    // M5-TP2: ENTRY -> N0 -(B1:false)-> N1 -> N2 -> EXIT (this.plunder != null)
    public void testM5_TP2_WriteAttributesWithNonNullPlunder() throws Exception {
        String xml = "<plunder id=\"model.plunder.withRange\" probability=\"80\" minimum=\"12\" maximum=\"34\" factor=\"4\"/>";
        PlunderType pt = readPlunderTypeFromXml(xml);
        StringWriter out = new StringWriter();
        try (FreeColXMLWriter xw = new FreeColXMLWriter(out, FreeColXMLWriter.WriteScope.toSave(), false)) {
            pt.toXML(xw);
        }
        String xmlOut = out.toString();
        assertTrue(xmlOut.contains("probability=\"80\""));
        assertTrue(xmlOut.contains("minimum=\"12\""));
        assertTrue(xmlOut.contains("maximum=\"34\""));
        assertTrue(xmlOut.contains("factor=\"4\""));
    }

    // ------------------------------------------------------------------
    // M6: readAttributes(Lnet/sf/freecol/common/io/FreeColXMLReader;)V — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M6-TP1: ENTRY -> N0 -> EXIT (Read attributes directly into existing PlunderType)
    public void testM6_TP1_ReadAttributes() throws Exception {
        PlunderType pt = new PlunderType("model.plunder.directRead", spec);
        String xml = "<plunder id=\"model.plunder.directRead\" probability=\"45\" minimum=\"1\" maximum=\"9\" factor=\"5\"/>";
        try (FreeColXMLReader xr = new FreeColXMLReader(new StringReader(xml))) {
            xr.nextTag();
            pt.readAttributes(xr);
        }
        assertNotNull(pt.getPlunder());
        assertEquals(45, pt.getPlunder().getProbability());
        assertEquals(1, pt.getPlunder().getMinimum());
        assertEquals(9, pt.getPlunder().getMaximum());
        assertEquals(5, pt.getPlunder().getFactor());
    }

    // ------------------------------------------------------------------
    // M7: getXMLTagName()Ljava/lang/String; — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M7-TP1: ENTRY -> N0 -> EXIT (Return PlunderType.TAG == "plunder")
    public void testM7_TP1_GetXMLTagName() {
        PlunderType pt = new PlunderType("model.plunder.tag", spec);
        assertEquals("plunder", pt.getXMLTagName());
        assertEquals(PlunderType.TAG, pt.getXMLTagName());
    }
}
