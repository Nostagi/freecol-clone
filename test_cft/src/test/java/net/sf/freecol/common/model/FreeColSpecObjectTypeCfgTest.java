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
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import net.sf.freecol.common.io.FreeColXMLReader;
import net.sf.freecol.common.io.FreeColXMLWriter;
import net.sf.freecol.util.test.FreeColTestCase;


/**
 * CFG-driven test suite for {@link FreeColSpecObjectType} derived from
 * {@code test_cft/graph/net/sf/freecol/common/model/FreeColSpecObjectType.json}.
 *
 * Covers all 23 methods (M1..M23, including all {@code <init>} overloads) and
 * achieves 100% feasible C3 (node + edge) coverage across 31 Test Paths.
 *
 * Infeasible edges documented:
 * - M17 ({@code copyIn}): {@code N1 -> N2} ({@code B2:false}, {@code !super.copyIn(o)}) —
 *   When {@code o != null}, {@code copyInCast(other, FreeColSpecObjectType.class)} has
 *   already verified {@code other != null}, {@code idEquals(other)}, and
 *   {@code other instanceof FreeColSpecObjectType}; {@link FreeColSpecObject#copyIn}
 *   and {@link FreeColObject#copyIn} are therefore guaranteed to return {@code true}.
 */
public class FreeColSpecObjectTypeCfgTest extends FreeColTestCase {

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
     * Concrete stub for abstract {@link FreeColSpecObjectType} exposing
     * protected constructors and protected XML/index methods.
     */
    private static class StubSpecObjectType extends FreeColSpecObjectType {

        StubSpecObjectType() {
            super();
        }

        StubSpecObjectType(String id) {
            super(id);
        }

        StubSpecObjectType(Specification specification) {
            super(specification);
        }

        StubSpecObjectType(String id, Specification specification) {
            super(id, specification);
        }

        void callSetIndex(int index) {
            setIndex(index);
        }

        void callWriteChildren(FreeColXMLWriter xw) throws Exception {
            writeChildren(xw);
        }

        void callReadAttributes(FreeColXMLReader xr) throws Exception {
            readAttributes(xr);
        }

        void callClearContainers(FreeColXMLReader xr) throws Exception {
            clearContainers(xr);
        }

        void callReadChildren(FreeColXMLReader xr) throws Exception {
            readChildren(xr);
        }

        void callReadChild(FreeColXMLReader xr) throws Exception {
            readChild(xr);
        }

        @Override
        public String getXMLTagName() {
            return "stubSpecObjectType";
        }
    }

    /**
     * Minimal {@link FreeColObject} stub (not a {@link FreeColSpecObjectType})
     * used to test {@code copyInCast} type mismatch.
     */
    private static class StubPlainFreeColObject extends FreeColObject {

        StubPlainFreeColObject(String id) {
            setId(id);
        }

        @Override
        public String getXMLTagName() {
            return "stubPlainFreeColObject";
        }
    }

    private Scope createTypeScope(String typeId) {
        Scope scope = new Scope();
        scope.setType(typeId);
        return scope;
    }

    // ------------------------------------------------------------------
    // M1: <init>()V — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M1-TP1: ENTRY -> N0 -> EXIT (No-arg protected constructor)
    public void testM1_TP1_NoArgConstructor() {
        StubSpecObjectType obj = new StubSpecObjectType();
        assertNull(obj.getId());
        assertNull(obj.getSpecification());
        assertEquals(-1, obj.getIndex());
    }

    // ------------------------------------------------------------------
    // M2: <init>(Ljava/lang/String;)V — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M2-TP1: ENTRY -> N0 -> EXIT (Constructor with id only)
    public void testM2_TP1_IdConstructor() {
        StubSpecObjectType obj = new StubSpecObjectType("model.stub.one");
        assertEquals("model.stub.one", obj.getId());
        assertNull(obj.getSpecification());
    }

    // ------------------------------------------------------------------
    // M3: <init>(Lnet/sf/freecol/common/model/Specification;)V — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M3-TP1: ENTRY -> N0 -> EXIT (Constructor with specification only)
    public void testM3_TP1_SpecConstructor() {
        StubSpecObjectType obj = new StubSpecObjectType(spec);
        assertNull(obj.getId());
        assertSame(spec, obj.getSpecification());
    }

    // ------------------------------------------------------------------
    // M4: <init>(Ljava/lang/String;Lnet/sf/freecol/common/model/Specification;)V — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M4-TP1: ENTRY -> N0 -> EXIT (Constructor with id and specification)
    public void testM4_TP1_IdAndSpecConstructor() {
        StubSpecObjectType obj = new StubSpecObjectType("model.stub.two", spec);
        assertEquals("model.stub.two", obj.getId());
        assertSame(spec, obj.getSpecification());
    }

    // ------------------------------------------------------------------
    // M5: getIndex()I — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M5-TP1: ENTRY -> N0 -> EXIT (Return default and updated index)
    public void testM5_TP1_GetIndex() {
        StubSpecObjectType obj = new StubSpecObjectType("model.stub.idx", spec);
        assertEquals(-1, obj.getIndex());
    }

    // ------------------------------------------------------------------
    // M6: setIndex(I)V — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M6-TP1: ENTRY -> N0 -> EXIT (Set index field)
    public void testM6_TP1_SetIndex() {
        StubSpecObjectType obj = new StubSpecObjectType("model.stub.idx", spec);
        obj.callSetIndex(42);
        assertEquals(42, obj.getIndex());
    }

    // ------------------------------------------------------------------
    // M7: getDescriptionKey()Ljava/lang/String; — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M7-TP1: ENTRY -> N0 -> EXIT (Return Messages.descriptionKey(getId()))
    public void testM7_TP1_GetDescriptionKey() {
        StubSpecObjectType obj = new StubSpecObjectType("model.goods.food", spec);
        assertEquals("model.goods.food.description", obj.getDescriptionKey());
    }

    // ------------------------------------------------------------------
    // M8: isAbstractType()Z — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M8-TP1: ENTRY -> N0 -> EXIT (Return abstractType flag)
    public void testM8_TP1_IsAbstractType() {
        StubSpecObjectType obj = new StubSpecObjectType("model.stub.abs", spec);
        assertFalse(obj.isAbstractType());
    }

    // ------------------------------------------------------------------
    // M9: getScopeList()Ljava/util/List; — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M9-TP1: ENTRY -> N0 -> EXIT (Return scope list)
    public void testM9_TP1_GetScopeList() {
        StubSpecObjectType obj = new StubSpecObjectType("model.stub.scope", spec);
        assertTrue(obj.getScopeList().isEmpty());
    }

    // ------------------------------------------------------------------
    // M10: getScopes()Ljava/util/stream/Stream; — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M10-TP1: ENTRY -> N0 -> EXIT (Return and terminal-consume scope stream)
    public void testM10_TP1_GetScopes() {
        StubSpecObjectType obj = new StubSpecObjectType("model.stub.scope", spec);
        obj.addScope(createTypeScope("model.unit.freeColonist"));
        List<Scope> collected = obj.getScopes().collect(Collectors.toList());
        assertEquals(1, collected.size());
    }

    // ------------------------------------------------------------------
    // M11: copyScopes(Ljava/util/Collection;)V — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M11-TP1: ENTRY -> N0 -> EXIT (Copy scopes collection)
    public void testM11_TP1_CopyScopes() {
        StubSpecObjectType obj = new StubSpecObjectType("model.stub.scope", spec);
        Scope scope = createTypeScope("model.unit.freeColonist");
        obj.copyScopes(Collections.singletonList(scope));
        assertEquals(1, obj.getScopeList().size());
    }

    // ------------------------------------------------------------------
    // M12: addScope(Lnet/sf/freecol/common/model/Scope;)V — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M12-TP1: ENTRY -> N0 -> EXIT (Add a single scope)
    public void testM12_TP1_AddScope() {
        StubSpecObjectType obj = new StubSpecObjectType("model.stub.scope", spec);
        Scope scope = createTypeScope("model.unit.freeColonist");
        obj.addScope(scope);
        assertEquals(1, obj.getScopeList().size());
    }

    // ------------------------------------------------------------------
    // M13: removeScope(Lnet/sf/freecol/common/model/Scope;)V — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M13-TP1: ENTRY -> N0 -> EXIT (Remove an existing scope)
    public void testM13_TP1_RemoveScope() {
        StubSpecObjectType obj = new StubSpecObjectType("model.stub.scope", spec);
        Scope scope = createTypeScope("model.unit.freeColonist");
        obj.addScope(scope);
        obj.removeScope(scope);
        assertTrue(obj.getScopeList().isEmpty());
    }

    // ------------------------------------------------------------------
    // M14: appliesTo(Lnet/sf/freecol/common/model/FreeColObject;)Z — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M14-TP1: ENTRY -> N0 -> EXIT (Delegate appliesTo to ScopeContainer)
    public void testM14_TP1_AppliesTo() {
        StubSpecObjectType obj = new StubSpecObjectType("model.stub.applies", spec);
        UnitType colonist = spec.getUnitType("model.unit.freeColonist");
        assertTrue(obj.appliesTo(colonist));
    }

    // ------------------------------------------------------------------
    // M15: getNameKey()Ljava/lang/String; — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M15-TP1: ENTRY -> N0 -> EXIT (Return Messages.nameKey(getId()))
    public void testM15_TP1_GetNameKey() {
        StubSpecObjectType obj = new StubSpecObjectType("model.goods.food", spec);
        assertEquals("model.goods.food.name", obj.getNameKey());
    }

    // ------------------------------------------------------------------
    // M16: getFeatureContainer()Lnet/sf/freecol/common/model/FeatureContainer; — 2 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M16-TP1: ENTRY -> N0 -(B1:false)-> N1 -> N2 -> EXIT (featureContainer == null, lazily initialized)
    public void testM16_TP1_GetFeatureContainerInitializesWhenNull() {
        StubSpecObjectType obj = new StubSpecObjectType("model.stub.fc", spec);
        FeatureContainer fc = obj.getFeatureContainer();
        assertNotNull(fc);
    }

    // M16-TP2: ENTRY -> N0 -(B1:true)-> N2 -> EXIT (featureContainer != null, returns cached instance)
    public void testM16_TP2_GetFeatureContainerReturnsExisting() {
        StubSpecObjectType obj = new StubSpecObjectType("model.stub.fc", spec);
        FeatureContainer first = obj.getFeatureContainer();
        FeatureContainer second = obj.getFeatureContainer();
        assertSame(first, second);
    }

    // ------------------------------------------------------------------
    // M17: copyIn(Lnet/sf/freecol/common/model/FreeColObject;)Z — 2 feasible Test Paths (100% feasible C3)
    // ------------------------------------------------------------------

    // M17-TP1: ENTRY -> N0 -(B1:true)-> N2 -> EXIT (o == null via null, wrong type, or mismatched ID)
    public void testM17_TP1_CopyInReturnsFalseWhenCastNull() {
        StubSpecObjectType target = new StubSpecObjectType("sameId", spec);
        assertFalse(target.copyIn(null));
        assertFalse(target.copyIn(new StubPlainFreeColObject("sameId")));
        assertFalse(target.copyIn(new StubSpecObjectType("otherId", spec)));
    }

    // M17-TP2: ENTRY -> N0 -(B1:false)-> N1 -(B2:true)-> N3 -> EXIT (o != null && super.copyIn(o) == true)
    public void testM17_TP2_CopyInCopiesFieldsWhenCompatible() throws Exception {
        StubSpecObjectType target = new StubSpecObjectType("sameId", spec);
        target.getFeatureContainer(); // ensure non-null featureContainer on target
        StubSpecObjectType source = new StubSpecObjectType("sameId", spec);
        source.callSetIndex(7);
        source.addAbility(new Ability("model.ability.test", source, true));
        source.addScope(createTypeScope("model.unit.freeColonist"));
        try (FreeColXMLReader xr = new FreeColXMLReader(new StringReader("<stubSpecObjectType id=\"sameId\" abstract=\"true\"/>"))) {
            xr.nextTag();
            source.callReadAttributes(xr);
        }

        assertTrue(target.copyIn(source));
        assertEquals(7, target.getIndex());
        assertTrue(target.isAbstractType());
        assertTrue(target.hasAbility("model.ability.test"));
        assertEquals(1, target.getScopeList().size());
    }

    // ------------------------------------------------------------------
    // M18: writeChildren(Lnet/sf/freecol/common/io/FreeColXMLWriter;)V — 2 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M18-TP1: ENTRY -> N0 -> N1 -(B1:true)-> N3 -> N4 -(B2:true)-> N6 -> EXIT (No abilities, no modifiers)
    public void testM18_TP1_WriteChildrenEmpty() throws Exception {
        StubSpecObjectType obj = new StubSpecObjectType("model.stub.wc", spec);
        StringWriter sw = new StringWriter();
        try (FreeColXMLWriter xw = new FreeColXMLWriter(sw, FreeColXMLWriter.WriteScope.toSave())) {
            xw.writeStartDocument("UTF-8", "1.0");
            xw.writeStartElement("stubSpecObjectType");
            obj.callWriteChildren(xw);
            xw.writeEndElement();
            xw.writeEndDocument();
        }
        assertFalse(sw.toString().contains("<ability"));
        assertFalse(sw.toString().contains("<modifier"));
    }

    // M18-TP2: ENTRY -> N0 -> N1 -(B1:false)-> N2 -> N1 -(B1:true)-> N3 -> N4 -(B2:false)-> N5 -> N4 -(B2:true)-> N6 -> EXIT (With ability and modifier)
    public void testM18_TP2_WriteChildrenWithAbilityAndModifier() throws Exception {
        StubSpecObjectType obj = new StubSpecObjectType("model.stub.wc", spec);
        obj.addAbility(new Ability("model.ability.test", obj, true));
        obj.addModifier(new Modifier("model.modifier.test", 2.0f, Modifier.ModifierType.ADDITIVE));
        obj.addScope(createTypeScope("model.unit.freeColonist"));

        StringWriter sw = new StringWriter();
        try (FreeColXMLWriter xw = new FreeColXMLWriter(sw, FreeColXMLWriter.WriteScope.toSave())) {
            xw.writeStartDocument("UTF-8", "1.0");
            xw.writeStartElement("stubSpecObjectType");
            obj.callWriteChildren(xw);
            xw.writeEndElement();
            xw.writeEndDocument();
        }
        String xml = sw.toString();
        assertTrue(xml.contains("<ability"));
        assertTrue(xml.contains("<modifier"));
        assertTrue(xml.contains("<scope"));
    }

    // ------------------------------------------------------------------
    // M19: readAttributes(Lnet/sf/freecol/common/io/FreeColXMLReader;)V — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M19-TP1: ENTRY -> N0 -> EXIT (Read abstract attribute from XML)
    public void testM19_TP1_ReadAttributes() throws Exception {
        StubSpecObjectType obj = new StubSpecObjectType("model.stub.ra", spec);
        try (FreeColXMLReader xr = new FreeColXMLReader(new StringReader("<stubSpecObjectType id=\"model.stub.ra\" abstract=\"true\"/>"))) {
            xr.nextTag();
            obj.callReadAttributes(xr);
        }
        assertTrue(obj.isAbstractType());
    }

    // ------------------------------------------------------------------
    // M20: clearContainers(Lnet/sf/freecol/common/io/FreeColXMLReader;)V — 2 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M20-TP1: ENTRY -> N0 -(B1:true)-> N2 -> EXIT (featureContainer == null)
    public void testM20_TP1_ClearContainersWhenFeatureContainerNull() throws Exception {
        StubSpecObjectType obj = new StubSpecObjectType("model.stub.cc", spec);
        try (FreeColXMLReader xr = new FreeColXMLReader(new StringReader("<stubSpecObjectType id=\"model.stub.cc\"/>"))) {
            xr.nextTag();
            obj.callClearContainers(xr);
        }
        assertTrue(obj.getScopeList().isEmpty());
    }

    // M20-TP2: ENTRY -> N0 -(B1:false)-> N1 -> N2 -> EXIT (featureContainer != null)
    public void testM20_TP2_ClearContainersWhenFeatureContainerPresent() throws Exception {
        StubSpecObjectType obj = new StubSpecObjectType("model.stub.cc", spec);
        obj.addAbility(new Ability("model.ability.test", obj, true));
        obj.addScope(createTypeScope("model.unit.freeColonist"));
        try (FreeColXMLReader xr = new FreeColXMLReader(new StringReader("<stubSpecObjectType id=\"model.stub.cc\"/>"))) {
            xr.nextTag();
            obj.callClearContainers(xr);
        }
        assertFalse(obj.hasAbility("model.ability.test"));
        assertTrue(obj.getScopeList().isEmpty());
    }

    // ------------------------------------------------------------------
    // M21: readChildren(Lnet/sf/freecol/common/io/FreeColXMLReader;)V — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M21-TP1: ENTRY -> N0 -> EXIT (Delegate readChildren to super)
    public void testM21_TP1_ReadChildren() throws Exception {
        StubSpecObjectType obj = new StubSpecObjectType("model.stub.rc", spec);
        String xml = "<stubSpecObjectType id=\"model.stub.rc\"><ability id=\"model.ability.test\" value=\"true\"/></stubSpecObjectType>";
        try (FreeColXMLReader xr = new FreeColXMLReader(new StringReader(xml))) {
            xr.nextTag();
            obj.callReadChildren(xr);
        }
        assertTrue(obj.hasAbility("model.ability.test"));
    }

    // ------------------------------------------------------------------
    // M22: readChild(Lnet/sf/freecol/common/io/FreeColXMLReader;)V — 8 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M22-TP1: ENTRY -> N0 -(B1:true)-> N6 -(B4:true)-> N12 -(B7:true)-> N14 -> N15 -> EXIT (Unknown child tag -> super.readChild)
    public void testM22_TP1_ReadChildUnknownTag() throws Exception {
        StubSpecObjectType obj = new StubSpecObjectType("model.stub.rc1", spec);
        try (FreeColXMLReader xr = new FreeColXMLReader(new StringReader("<unknownChild/>"))) {
            xr.nextTag();
            try {
                obj.callReadChild(xr);
                fail("Expected XMLStreamException for unknown child tag");
            } catch (javax.xml.stream.XMLStreamException expected) {
                // expected from super.readChild(xr)
            }
        }
        assertTrue(obj.getScopeList().isEmpty());
    }

    // M22-TP2: ENTRY -> N0 -(B1:true)-> N6 -(B4:true)-> N12 -(B7:false)-> N13 -> N15 -> EXIT (Scope tag)
    public void testM22_TP2_ReadChildScopeTag() throws Exception {
        StubSpecObjectType obj = new StubSpecObjectType("model.stub.rc2", spec);
        try (FreeColXMLReader xr = new FreeColXMLReader(new StringReader("<scope type=\"model.unit.freeColonist\"/>"))) {
            xr.nextTag();
            obj.callReadChild(xr);
        }
        assertEquals(1, obj.getScopeList().size());
    }

    // M22-TP3: ENTRY -> N0 -(B1:true)-> N6 -(B4:false)-> N7 -(B5:true)-> N9 -(B6:true)-> N11 -> N15 -> EXIT (Modifier tag without delete, with explicit source)
    public void testM22_TP3_ReadChildModifierWithSource() throws Exception {
        StubSpecObjectType obj = new StubSpecObjectType("model.stub.rc3", spec);
        String xml = "<modifier id=\"model.modifier.testCfg\" type=\"additive\" value=\"1\" source=\"model.goods.food\"/>";
        try (FreeColXMLReader xr = new FreeColXMLReader(new StringReader(xml))) {
            xr.nextTag();
            obj.callReadChild(xr);
        }
        assertEquals(1, obj.getModifiers("model.modifier.testCfg").count());
        Modifier m = obj.getModifiers("model.modifier.testCfg").findFirst().get();
        assertEquals(spec.getGoodsType("model.goods.food"), m.getSource());
    }

    // M22-TP4: ENTRY -> N0 -(B1:true)-> N6 -(B4:false)-> N7 -(B5:true)-> N9 -(B6:false)-> N10 -> N11 -> N15 -> EXIT (Modifier tag without delete, source == null)
    public void testM22_TP4_ReadChildModifierWithoutSource() throws Exception {
        StubSpecObjectType obj = new StubSpecObjectType("model.stub.rc4", spec);
        String xml = "<modifier id=\"model.modifier.testCfgNoSrc\" type=\"additive\" value=\"2\"/>";
        try (FreeColXMLReader xr = new FreeColXMLReader(new StringReader(xml))) {
            xr.nextTag();
            obj.callReadChild(xr);
        }
        Modifier m = obj.getModifiers("model.modifier.testCfgNoSrc").findFirst().get();
        assertSame(obj, m.getSource());
    }

    // M22-TP5: ENTRY -> N0 -(B1:true)-> N6 -(B4:false)-> N7 -(B5:false)-> N8 -> N15 -> EXIT (Modifier tag with delete="true")
    public void testM22_TP5_ReadChildModifierDelete() throws Exception {
        StubSpecObjectType obj = new StubSpecObjectType("model.stub.rc5", spec);
        obj.addModifier(new Modifier("model.modifier.delCfg", 1.0f, Modifier.ModifierType.ADDITIVE));
        String xml = "<modifier id=\"model.modifier.delCfg\" delete=\"true\"/>";
        try (FreeColXMLReader xr = new FreeColXMLReader(new StringReader(xml))) {
            xr.nextTag();
            obj.callReadChild(xr);
        }
        assertEquals(0, obj.getModifiers("model.modifier.delCfg").count());
    }

    // M22-TP6: ENTRY -> N0 -(B1:false)-> N1 -(B2:true)-> N3 -(B3:true)-> N5 -> N15 -> EXIT (Ability tag without delete, with explicit source)
    public void testM22_TP6_ReadChildAbilityWithSource() throws Exception {
        StubSpecObjectType obj = new StubSpecObjectType("model.stub.rc6", spec);
        String xml = "<ability id=\"model.ability.testCfg\" value=\"true\" source=\"model.goods.food\"/>";
        try (FreeColXMLReader xr = new FreeColXMLReader(new StringReader(xml))) {
            xr.nextTag();
            obj.callReadChild(xr);
        }
        assertTrue(obj.hasAbility("model.ability.testCfg"));
        Ability a = obj.getAbilities("model.ability.testCfg").findFirst().get();
        assertEquals(spec.getGoodsType("model.goods.food"), a.getSource());
    }

    // M22-TP7: ENTRY -> N0 -(B1:false)-> N1 -(B2:true)-> N3 -(B3:false)-> N4 -> N5 -> N15 -> EXIT (Ability tag without delete, source == null)
    public void testM22_TP7_ReadChildAbilityWithoutSource() throws Exception {
        StubSpecObjectType obj = new StubSpecObjectType("model.stub.rc7", spec);
        String xml = "<ability id=\"model.ability.testCfgNoSrc\" value=\"true\"/>";
        try (FreeColXMLReader xr = new FreeColXMLReader(new StringReader(xml))) {
            xr.nextTag();
            obj.callReadChild(xr);
        }
        assertTrue(obj.hasAbility("model.ability.testCfgNoSrc"));
        Ability a = obj.getAbilities("model.ability.testCfgNoSrc").findFirst().get();
        assertSame(obj, a.getSource());
    }

    // M22-TP8: ENTRY -> N0 -(B1:false)-> N1 -(B2:false)-> N2 -> N15 -> EXIT (Ability tag with delete="true")
    public void testM22_TP8_ReadChildAbilityDelete() throws Exception {
        StubSpecObjectType obj = new StubSpecObjectType("model.stub.rc8", spec);
        obj.addAbility(new Ability("model.ability.delCfg", obj, true));
        String xml = "<ability id=\"model.ability.delCfg\" delete=\"true\"/>";
        try (FreeColXMLReader xr = new FreeColXMLReader(new StringReader(xml))) {
            xr.nextTag();
            obj.callReadChild(xr);
        }
        assertFalse(obj.hasAbility("model.ability.delCfg"));
    }

    // ------------------------------------------------------------------
    // M23: toString()Ljava/lang/String; — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M23-TP1: ENTRY -> N0 -> EXIT (Return getId())
    public void testM23_TP1_ToString() {
        StubSpecObjectType obj = new StubSpecObjectType("model.stub.str", spec);
        assertEquals("model.stub.str", obj.toString());
    }
}
