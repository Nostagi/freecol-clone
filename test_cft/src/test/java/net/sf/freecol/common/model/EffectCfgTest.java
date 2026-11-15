package net.sf.freecol.common.model;

import java.io.StringReader;
import java.io.StringWriter;

import net.sf.freecol.common.io.FreeColXMLReader;
import net.sf.freecol.common.io.FreeColXMLWriter;
import net.sf.freecol.common.io.FreeColXMLWriter.WriteScope;
import net.sf.freecol.util.test.FreeColTestCase;

/**
 * CFG-driven test suite for {@link Effect} derived from
 * {@code test_cft/graph/net/sf/freecol/common/model/Effect.json}.
 *
 * Covers all 9 methods (M1..M9) and achieves 100% feasible C3 (node + edge)
 * coverage across 11 Test Paths.
 *
 * Infeasible edges documented:
 * - M5 ({@code copyIn}): {@code N1 -> N2} ({@code B2:false}) — {@code super.copyIn(o)}
 *   delegates to {@code FreeColSpecObjectType.copyIn} and {@code FreeColObject.copyIn},
 *   which always return {@code true} whenever {@code o != null}.
 */
public class EffectCfgTest extends FreeColTestCase {

    private Specification spec;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        // Pre-condition: load default Specification for XML serialization and Scope types.
        spec = spec();
    }

    // ------------------------------------------------------------------
    // Pre-condition helpers & stubs
    // ------------------------------------------------------------------

    private Effect readEffectFromXml(String xml) throws Exception {
        try (FreeColXMLReader xr = new FreeColXMLReader(new StringReader(xml))) {
            xr.nextTag();
            return new Effect(xr, spec);
        }
    }

    private String writeXml(Effect effect) throws Exception {
        StringWriter out = new StringWriter();
        try (FreeColXMLWriter xw = new FreeColXMLWriter(out, WriteScope.toSave(), false)) {
            effect.toXML(xw);
            xw.flush();
        }
        return out.toString();
    }

    // ------------------------------------------------------------------
    // M1: <init>() — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M1-TP1: ENTRY -> N0 -> EXIT (protected empty constructor)
    public void testM1_TP1_ProtectedNoArgConstructor() {
        Effect effect = new Effect();
        assertNotNull(effect);
        assertEquals(0, effect.getProbability());
    }

    // ------------------------------------------------------------------
    // M2: <init>(FreeColXMLReader, Specification) — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M2-TP1: ENTRY -> N0 -> EXIT (constructs Effect from XML stream)
    public void testM2_TP1_XmlConstructor() throws Exception {
        Effect effect = readEffectFromXml(
                "<effect id=\"" + Effect.LOSS_OF_MONEY + "\" probability=\"45\"/>");
        assertEquals(Effect.LOSS_OF_MONEY, effect.getId());
        assertSame(spec, effect.getSpecification());
        assertEquals(45, effect.getProbability());
    }

    // ------------------------------------------------------------------
    // M3: <init>(Effect) — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M3-TP1: ENTRY -> N0 -> EXIT (copy constructor copies id, spec, probability, scopes, features)
    public void testM3_TP1_CopyConstructor() throws Exception {
        Effect template = readEffectFromXml(
                "<effect id=\"" + Effect.LOSS_OF_UNIT + "\" probability=\"60\">"
                + "<scope type=\"model.unit.freeColonist\"/>"
                + "</effect>");
        Effect copy = new Effect(template);
        assertEquals(Effect.LOSS_OF_UNIT, copy.getId());
        assertSame(spec, copy.getSpecification());
        assertEquals(60, copy.getProbability());
        assertEquals(1, copy.getScopeList().size());
    }

    // ------------------------------------------------------------------
    // M4: getProbability() — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M4-TP1: ENTRY -> N0 -> EXIT (returns probability field)
    public void testM4_TP1_GetProbability() throws Exception {
        Effect effect = readEffectFromXml(
                "<effect id=\"" + Effect.DAMAGED_UNIT + "\" probability=\"80\"/>");
        assertEquals(80, effect.getProbability());
    }

    // ------------------------------------------------------------------
    // M5: copyIn(FreeColObject) — 2 feasible Test Paths (100% feasible C3)
    // ------------------------------------------------------------------

    // M5-TP1: ENTRY -> N0 -(B1:true)-> N2 -> EXIT (other is null or non-Effect -> returns false)
    public void testM5_TP1_CopyInNullOrWrongTypeReturnsFalse() {
        Effect effect = new Effect();
        assertFalse(effect.copyIn(null));
        assertFalse(effect.copyIn(spec.getGoodsType("model.goods.food")));
    }

    // M5-TP2: ENTRY -> N0 -(B1:false)-> N1 -(B2:true)-> N3 -> EXIT (valid Effect -> copies probability)
    public void testM5_TP2_CopyInValidEffectCopiesProbability() throws Exception {
        Effect source = readEffectFromXml(
                "<effect id=\"" + Effect.LOSS_OF_GOODS + "\" probability=\"35\"/>");
        Effect target = new Effect();
        target.setSpecification(spec);
        target.setId(Effect.LOSS_OF_GOODS);
        target.getFeatureContainer();
        assertTrue(target.copyIn(source));
        assertEquals(35, target.getProbability());
    }

    // ------------------------------------------------------------------
    // M6: writeAttributes(FreeColXMLWriter) — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M6-TP1: ENTRY -> N0 -> EXIT (writes probability attribute)
    public void testM6_TP1_WriteAttributesWritesProbability() throws Exception {
        Effect effect = readEffectFromXml(
                "<effect id=\"" + Effect.LOSS_OF_BUILDING + "\" probability=\"25\"/>");
        String xml = writeXml(effect);
        assertTrue(xml.contains("probability=\"25\""));
    }

    // ------------------------------------------------------------------
    // M7: readAttributes(FreeColXMLReader) — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M7-TP1: ENTRY -> N0 -> EXIT (reads probability attribute with default 0)
    public void testM7_TP1_ReadAttributesLoadsProbability() throws Exception {
        Effect effect = new Effect();
        effect.setSpecification(spec);
        try (FreeColXMLReader xr = new FreeColXMLReader(new StringReader(
                "<effect id=\"" + Effect.LOSS_OF_TILE_PRODUCTION + "\" probability=\"90\"/>"))) {
            xr.nextTag();
            effect.readFromXML(xr);
        }
        assertEquals(90, effect.getProbability());
    }

    // ------------------------------------------------------------------
    // M8: getXMLTagName() — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M8-TP1: ENTRY -> N0 -> EXIT (returns Effect.TAG)
    public void testM8_TP1_GetXMLTagName() {
        Effect effect = new Effect();
        assertEquals("effect", effect.getXMLTagName());
    }

    // ------------------------------------------------------------------
    // M9: toString() — 2 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M9-TP1: ENTRY -> N0 -> N1 -(B1:true)-> N3 -> EXIT (empty scopeList -> 0 iterations)
    public void testM9_TP1_ToStringWithoutScopes() throws Exception {
        Effect effect = readEffectFromXml(
                "<effect id=\"" + Effect.LOSS_OF_BUILDING_PRODUCTION + "\" probability=\"15\"/>");
        assertEquals("[" + Effect.LOSS_OF_BUILDING_PRODUCTION + " probability=15%]",
                effect.toString());
    }

    // M9-TP2: ENTRY -> N0 -> N1 -(B1:false)-> N2 -> N1 -(B1:true)-> N3 -> EXIT (non-empty scopeList -> loop body + back-edge)
    public void testM9_TP2_ToStringWithScopes() throws Exception {
        Effect effect = readEffectFromXml(
                "<effect id=\"" + Effect.LOSS_OF_UNIT + "\" probability=\"50\">"
                + "<scope type=\"model.unit.freeColonist\"/>"
                + "</effect>");
        String str = effect.toString();
        assertTrue(str.startsWith("[" + Effect.LOSS_OF_UNIT + " probability=50% "));
        assertTrue(str.endsWith("]"));
    }
}
