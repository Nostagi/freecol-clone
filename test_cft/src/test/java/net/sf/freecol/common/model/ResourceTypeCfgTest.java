package net.sf.freecol.common.model;

import java.io.StringReader;
import java.io.StringWriter;

import net.sf.freecol.common.io.FreeColXMLReader;
import net.sf.freecol.common.io.FreeColXMLWriter;
import net.sf.freecol.common.io.FreeColXMLWriter.WriteScope;
import net.sf.freecol.common.model.Modifier.ModifierType;
import net.sf.freecol.util.test.FreeColTestCase;

/**
 * CFG-driven test suite for {@link ResourceType} derived from
 * {@code test_cft/graph/net/sf/freecol/common/model/ResourceType.json}.
 *
 * Covers all 9 methods (M1..M9) and achieves 100% feasible C3 (node + edge)
 * coverage across 12 Test Paths.
 *
 * Infeasible edges documented:
 * - M5 ({@code copyIn}): {@code N1 -> N2} ({@code B2:false}) — {@code ResourceType} is
 *   {@code final} and {@code super.copyIn(o)} delegates to {@code FreeColSpecObjectType.copyIn}
 *   and {@code FreeColObject.copyIn}, which always return {@code true} whenever {@code o != null}.
 */
public class ResourceTypeCfgTest extends FreeColTestCase {

    private Specification spec;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        // Pre-condition: load default Specification for GoodsTypes and XML serialization.
        spec = spec();
    }

    // ------------------------------------------------------------------
    // Pre-condition helpers & stubs
    // ------------------------------------------------------------------

    private String writeXml(ResourceType rt) throws Exception {
        StringWriter out = new StringWriter();
        try (FreeColXMLWriter xw = new FreeColXMLWriter(out, WriteScope.toSave(), false)) {
            rt.toXML(xw);
            xw.flush();
        }
        return out.toString();
    }

    private void readXml(ResourceType rt, String xml) throws Exception {
        try (FreeColXMLReader xr = new FreeColXMLReader(new StringReader(xml))) {
            xr.nextTag();
            rt.readFromXML(xr);
        }
    }

    // ------------------------------------------------------------------
    // M1: <init>(String, Specification) — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M1-TP1: ENTRY -> N0 -> EXIT (constructs ResourceType with id and specification)
    public void testM1_TP1_ConstructorSetsIdAndSpec() {
        ResourceType rt = new ResourceType("model.resource.cftTest", spec);
        assertEquals("model.resource.cftTest", rt.getId());
        assertSame(spec, rt.getSpecification());
    }

    // ------------------------------------------------------------------
    // M2: getMaxValue() — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M2-TP1: ENTRY -> N0 -> EXIT (returns maxValue field)
    public void testM2_TP1_GetMaxValue() throws Exception {
        ResourceType rt = new ResourceType("model.resource.cftMax", spec);
        readXml(rt, "<resource-type id=\"model.resource.cftMax\" maximum-value=\"50\" minimum-value=\"10\"/>");
        assertEquals(50, rt.getMaxValue());
    }

    // ------------------------------------------------------------------
    // M3: getMinValue() — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M3-TP1: ENTRY -> N0 -> EXIT (returns minValue field)
    public void testM3_TP1_GetMinValue() throws Exception {
        ResourceType rt = new ResourceType("model.resource.cftMin", spec);
        readXml(rt, "<resource-type id=\"model.resource.cftMin\" maximum-value=\"50\" minimum-value=\"10\"/>");
        assertEquals(10, rt.getMinValue());
    }

    // ------------------------------------------------------------------
    // M4: getBestGoodsType() — 2 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M4-TP1: ENTRY -> N0 -(B1:false)-> N1 -> N3 -> EXIT (no modifiers -> best == null -> returns null)
    public void testM4_TP1_GetBestGoodsTypeWithoutModifiersReturnsNull() {
        ResourceType rt = new ResourceType("model.resource.cftEmpty", spec);
        assertNull(rt.getBestGoodsType());
    }

    // M4-TP2: ENTRY -> N0 -(B1:true)-> N2 -> N3 -> EXIT (modifiers present -> best != null -> returns best GoodsType)
    public void testM4_TP2_GetBestGoodsTypeWithModifiersReturnsHighestValue() {
        ResourceType rt = new ResourceType("model.resource.cftRich", spec);
        rt.addModifier(new Modifier("model.goods.grain", 2.0f, ModifierType.ADDITIVE));
        rt.addModifier(new Modifier("model.goods.silver", 2.0f, ModifierType.ADDITIVE));
        GoodsType best = rt.getBestGoodsType();
        assertEquals(spec.getGoodsType("model.goods.silver"), best);
    }

    // ------------------------------------------------------------------
    // M5: copyIn(FreeColObject) — 2 feasible Test Paths (100% feasible C3)
    // ------------------------------------------------------------------

    // M5-TP1: ENTRY -> N0 -(B1:true)-> N2 -> EXIT (other is null or non-ResourceType -> returns false)
    public void testM5_TP1_CopyInWrongTypeOrNullReturnsFalse() {
        ResourceType rt = new ResourceType("model.resource.cftTarget", spec);
        assertFalse(rt.copyIn(null));
        assertFalse(rt.copyIn(new GoodsType("model.goods.cftOther", spec)));
    }

    // M5-TP2: ENTRY -> N0 -(B1:false)-> N1 -(B2:true)-> N3 -> EXIT (valid ResourceType -> copies min/max and returns true)
    public void testM5_TP2_CopyInValidResourceTypeCopiesValues() throws Exception {
        ResourceType source = new ResourceType("model.resource.cftSrc", spec);
        readXml(source, "<resource-type id=\"model.resource.cftSrc\" maximum-value=\"80\" minimum-value=\"20\"/>");
        ResourceType target = new ResourceType("model.resource.cftSrc", spec);
        target.getFeatureContainer();

        assertTrue(target.copyIn(source));
        assertEquals(80, target.getMaxValue());
        assertEquals(20, target.getMinValue());
    }

    // ------------------------------------------------------------------
    // M6: writeAttributes(FreeColXMLWriter) — 2 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M6-TP1: ENTRY -> N0 -(B1:true)-> N2 -> EXIT (maxValue <= -1 -> omits maximum-value and minimum-value)
    public void testM6_TP1_WriteAttributesOmitsUnsetMinMax() throws Exception {
        ResourceType rt = new ResourceType("model.resource.cftNoVal", spec);
        readXml(rt, "<resource-type id=\"model.resource.cftNoVal\"/>");
        String xml = writeXml(rt);
        assertFalse(xml.contains("maximum-value"));
        assertFalse(xml.contains("minimum-value"));
    }

    // M6-TP2: ENTRY -> N0 -(B1:false)-> N1 -> N2 -> EXIT (maxValue > -1 -> writes maximum-value and minimum-value)
    public void testM6_TP2_WriteAttributesWritesMinMaxWhenSet() throws Exception {
        ResourceType rt = new ResourceType("model.resource.cftWithVal", spec);
        readXml(rt, "<resource-type id=\"model.resource.cftWithVal\" maximum-value=\"40\" minimum-value=\"5\"/>");
        String xml = writeXml(rt);
        assertTrue(xml.contains("maximum-value=\"40\""));
        assertTrue(xml.contains("minimum-value=\"5\""));
    }

    // ------------------------------------------------------------------
    // M7: readAttributes(FreeColXMLReader) — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M7-TP1: ENTRY -> N0 -> EXIT (reads maximum-value and minimum-value attributes)
    public void testM7_TP1_ReadAttributesLoadsMinMax() throws Exception {
        ResourceType rt = new ResourceType("model.resource.cftRead", spec);
        readXml(rt, "<resource-type id=\"model.resource.cftRead\" maximum-value=\"99\" minimum-value=\"11\"/>");
        assertEquals(99, rt.getMaxValue());
        assertEquals(11, rt.getMinValue());
    }

    // ------------------------------------------------------------------
    // M8: getXMLTagName() — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M8-TP1: ENTRY -> N0 -> EXIT (returns ResourceType.TAG)
    public void testM8_TP1_GetXMLTagName() {
        ResourceType rt = new ResourceType("model.resource.cftTag", spec);
        assertEquals("resource-type", rt.getXMLTagName());
    }

    // ------------------------------------------------------------------
    // M9: lambda$getBestGoodsType$0(Specification, Modifier) — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M9-TP1: ENTRY -> N0 -> EXIT (invoked by comparator during getBestGoodsType())
    public void testM9_TP1_LambdaGetBestGoodsTypeEvaluatesModifierValue() {
        ResourceType rt = new ResourceType("model.resource.cftLambda", spec);
        rt.addModifier(new Modifier("model.goods.grain", 10.0f, ModifierType.ADDITIVE));
        rt.addModifier(new Modifier("model.goods.lumber", 5.0f, ModifierType.ADDITIVE));
        assertNotNull(rt.getBestGoodsType());
    }
}
