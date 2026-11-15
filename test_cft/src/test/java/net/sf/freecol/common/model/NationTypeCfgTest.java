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
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import net.sf.freecol.common.io.FreeColXMLReader;
import net.sf.freecol.common.io.FreeColXMLWriter;
import net.sf.freecol.common.model.NationType.AggressionLevel;
import net.sf.freecol.common.model.NationType.SettlementNumber;
import net.sf.freecol.util.test.FreeColTestCase;


/**
 * CFG-driven test suite for {@link NationType} derived from
 * {@code test_cft/graph/net/sf/freecol/common/model/NationType.json}.
 *
 * Covers all 17 methods (M1..M17, including {@code <init>}) and achieves 100%
 * feasible C3 (node + edge) coverage across 27 Test Paths.
 *
 * Infeasible edges documented:
 * - M11 ({@code copyIn}): {@code N1 -> N2} ({@code B2:false}, {@code !super.copyIn(o)}) —
 *   Once {@code copyInCast(other, NationType.class)} returns non-null {@code o},
 *   {@code o} is non-null, shares the same ID, and is an instance of
 *   {@code NationType} (and therefore {@code FreeColSpecObjectType},
 *   {@code FreeColSpecObject}, and {@code FreeColObject}), so
 *   {@code super.copyIn(o)} is guaranteed to return {@code true}.
 */
public class NationTypeCfgTest extends FreeColTestCase {

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
     * Concrete stub for abstract {@link NationType}.
     */
    private static class StubNationType extends NationType {

        StubNationType(String id, Specification specification) {
            super(id, specification);
        }

        @Override
        public boolean isEuropean() {
            return true;
        }

        @Override
        public boolean isIndian() {
            return false;
        }

        @Override
        public boolean isREF() {
            return false;
        }

        @Override
        public String getXMLTagName() {
            return "stub-nation-type";
        }

        void callWriteAttributes(FreeColXMLWriter xw) throws Exception {
            writeAttributes(xw);
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
    }

    private SettlementType createSettlementType(String id, boolean capital) {
        SettlementType st = new SettlementType(id, spec);
        // Set capital flag via reflection or XML if needed
        try {
            Field f = SettlementType.class.getDeclaredField("capital");
            f.setAccessible(true);
            f.setBoolean(st, capital);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return st;
    }

    private void invokeAddSettlementType(NationType target, SettlementType st) throws Exception {
        Method m = NationType.class.getDeclaredMethod("addSettlementType", SettlementType.class);
        m.setAccessible(true);
        m.invoke(target, st);
    }

    private void invokeAddSettlementTypes(NationType target, List<SettlementType> list) throws Exception {
        Method m = NationType.class.getDeclaredMethod("addSettlementTypes", List.class);
        m.setAccessible(true);
        m.invoke(target, list);
    }

    private void setAbstractType(FreeColSpecObjectType type, boolean value) {
        try {
            Field f = FreeColSpecObjectType.class.getDeclaredField("abstractType");
            f.setAccessible(true);
            f.setBoolean(type, value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void addTypeToSpec(FreeColSpecObjectType type) {
        try {
            Method m = Specification.class.getDeclaredMethod(
                    "addType", String.class, FreeColSpecObjectType.class);
            m.setAccessible(true);
            m.invoke(spec, type.getId(), type);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private FreeColXMLReader openReaderAtStartTag(String xml) throws Exception {
        FreeColXMLReader xr = new FreeColXMLReader(new StringReader(xml));
        xr.nextTag();
        return xr;
    }

    // ------------------------------------------------------------------
    // M1: <init>(Ljava/lang/String;Lnet/sf/freecol/common/model/Specification;)V — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M1-TP1: ENTRY -> N0 -> EXIT (Construct NationType with id and specification)
    public void testM1_TP1_Constructor() {
        StubNationType nt = new StubNationType("model.nationType.test", spec);
        assertEquals("model.nationType.test", nt.getId());
        assertSame(spec, nt.getSpecification());
        assertEquals(SettlementNumber.AVERAGE, nt.getNumberOfSettlements());
        assertEquals(AggressionLevel.AVERAGE, nt.getAggression());
    }

    // ------------------------------------------------------------------
    // M2: getSettlementTypes()Ljava/util/List; — 2 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M2-TP1: ENTRY -> N0 -(B1:false)-> N1 -> N3 -> EXIT (settlementTypes == null -> emptyList)
    public void testM2_TP1_GetSettlementTypesWhenNull() {
        StubNationType nt = new StubNationType("model.nationType.test", spec);
        List<SettlementType> result = nt.getSettlementTypes();
        assertTrue(result.isEmpty());
    }

    // M2-TP2: ENTRY -> N0 -(B1:true)-> N2 -> N3 -> EXIT (settlementTypes != null -> returns list)
    public void testM2_TP2_GetSettlementTypesWhenNonNull() {
        StubNationType nt = new StubNationType("model.nationType.test", spec);
        SettlementType st = createSettlementType("model.settlement.camp", false);
        nt.setSettlementTypes(Collections.singletonList(st));
        List<SettlementType> result = nt.getSettlementTypes();
        assertEquals(1, result.size());
        assertSame(st, result.get(0));
    }

    // ------------------------------------------------------------------
    // M3: setSettlementTypes(Ljava/util/List;)V — 2 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M3-TP1: ENTRY -> N0 -(B1:false)-> N1 -> N3 -> EXIT (this.settlementTypes == null -> allocate and addAll)
    public void testM3_TP1_SetSettlementTypesInitiallyNull() {
        StubNationType nt = new StubNationType("model.nationType.test", spec);
        SettlementType st1 = createSettlementType("model.settlement.s1", false);
        nt.setSettlementTypes(Collections.singletonList(st1));
        assertEquals(1, nt.getSettlementTypes().size());
        assertSame(st1, nt.getSettlementTypes().get(0));
    }

    // M3-TP2: ENTRY -> N0 -(B1:true)-> N2 -> N3 -> EXIT (this.settlementTypes != null -> clear and addAll)
    public void testM3_TP2_SetSettlementTypesReplacesExisting() {
        StubNationType nt = new StubNationType("model.nationType.test", spec);
        SettlementType st1 = createSettlementType("model.settlement.s1", false);
        SettlementType st2 = createSettlementType("model.settlement.s2", true);
        nt.setSettlementTypes(Collections.singletonList(st1));
        nt.setSettlementTypes(Collections.singletonList(st2));
        assertEquals(1, nt.getSettlementTypes().size());
        assertSame(st2, nt.getSettlementTypes().get(0));
    }

    // ------------------------------------------------------------------
    // M4: addSettlementType(Lnet/sf/freecol/common/model/SettlementType;)V — 2 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M4-TP1: ENTRY -> N0 -(B1:false)-> N1 -> N2 -> EXIT (settlementTypes == null -> allocate and add)
    public void testM4_TP1_AddSettlementTypeWhenNull() throws Exception {
        StubNationType nt = new StubNationType("model.nationType.test", spec);
        SettlementType st1 = createSettlementType("model.settlement.s1", false);
        invokeAddSettlementType(nt, st1);
        assertEquals(1, nt.getSettlementTypes().size());
    }

    // M4-TP2: ENTRY -> N0 -(B1:true)-> N2 -> EXIT (settlementTypes != null -> add directly)
    public void testM4_TP2_AddSettlementTypeWhenNonNull() throws Exception {
        StubNationType nt = new StubNationType("model.nationType.test", spec);
        SettlementType st1 = createSettlementType("model.settlement.s1", false);
        SettlementType st2 = createSettlementType("model.settlement.s2", true);
        invokeAddSettlementType(nt, st1);
        invokeAddSettlementType(nt, st2);
        assertEquals(2, nt.getSettlementTypes().size());
    }

    // ------------------------------------------------------------------
    // M5: addSettlementTypes(Ljava/util/List;)V — 2 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M5-TP1: ENTRY -> N0 -(B1:false)-> N1 -> N2 -> EXIT (settlementTypes == null -> allocate and addAll)
    public void testM5_TP1_AddSettlementTypesWhenNull() throws Exception {
        StubNationType nt = new StubNationType("model.nationType.test", spec);
        SettlementType st1 = createSettlementType("model.settlement.s1", false);
        invokeAddSettlementTypes(nt, Collections.singletonList(st1));
        assertEquals(1, nt.getSettlementTypes().size());
    }

    // M5-TP2: ENTRY -> N0 -(B1:true)-> N2 -> EXIT (settlementTypes != null -> addAll directly)
    public void testM5_TP2_AddSettlementTypesWhenNonNull() throws Exception {
        StubNationType nt = new StubNationType("model.nationType.test", spec);
        SettlementType st1 = createSettlementType("model.settlement.s1", false);
        SettlementType st2 = createSettlementType("model.settlement.s2", true);
        invokeAddSettlementTypes(nt, Collections.singletonList(st1));
        invokeAddSettlementTypes(nt, Collections.singletonList(st2));
        assertEquals(2, nt.getSettlementTypes().size());
    }

    // ------------------------------------------------------------------
    // M6: getCapitalType()Lnet/sf/freecol/common/model/SettlementType; — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M6-TP1: ENTRY -> N0 -> EXIT (Delegates to getSettlementType(true))
    public void testM6_TP1_GetCapitalType() {
        StubNationType nt = new StubNationType("model.nationType.test", spec);
        SettlementType normal = createSettlementType("model.settlement.normal", false);
        SettlementType capital = createSettlementType("model.settlement.capital", true);
        nt.setSettlementTypes(Arrays.asList(normal, capital));
        assertSame(capital, nt.getCapitalType());
    }

    // ------------------------------------------------------------------
    // M7: getSettlementType(Z)Lnet/sf/freecol/common/model/SettlementType; — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M7-TP1: ENTRY -> N0 -> EXIT (Find settlement type matching isCapital)
    public void testM7_TP1_GetSettlementTypeByCapitalFlag() {
        StubNationType nt = new StubNationType("model.nationType.test", spec);
        SettlementType normal = createSettlementType("model.settlement.normal", false);
        SettlementType capital = createSettlementType("model.settlement.capital", true);
        nt.setSettlementTypes(Arrays.asList(normal, capital));
        assertSame(normal, nt.getSettlementType(false));
        assertSame(capital, nt.getSettlementType(true));
    }

    // ------------------------------------------------------------------
    // M8: getSettlementType(Ljava/lang/String;)Lnet/sf/freecol/common/model/SettlementType; — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M8-TP1: ENTRY -> N0 -> EXIT (Find settlement type matching id)
    public void testM8_TP1_GetSettlementTypeById() {
        StubNationType nt = new StubNationType("model.nationType.test", spec);
        SettlementType s1 = createSettlementType("model.settlement.s1", false);
        SettlementType s2 = createSettlementType("model.settlement.s2", true);
        nt.setSettlementTypes(Arrays.asList(s1, s2));
        assertSame(s2, nt.getSettlementType("model.settlement.s2"));
        assertNull(nt.getSettlementType("model.settlement.missing"));
    }

    // ------------------------------------------------------------------
    // M9: getNumberOfSettlements()Lnet/sf/freecol/common/model/NationType$SettlementNumber; — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M9-TP1: ENTRY -> N0 -> EXIT (Return numberOfSettlements)
    public void testM9_TP1_GetNumberOfSettlements() {
        StubNationType nt = new StubNationType("model.nationType.test", spec);
        assertEquals(SettlementNumber.AVERAGE, nt.getNumberOfSettlements());
    }

    // ------------------------------------------------------------------
    // M10: getAggression()Lnet/sf/freecol/common/model/NationType$AggressionLevel; — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M10-TP1: ENTRY -> N0 -> EXIT (Return aggression)
    public void testM10_TP1_GetAggression() {
        StubNationType nt = new StubNationType("model.nationType.test", spec);
        assertEquals(AggressionLevel.AVERAGE, nt.getAggression());
    }

    // ------------------------------------------------------------------
    // M11: copyIn(Lnet/sf/freecol/common/model/FreeColObject;)Z — 2 feasible Test Paths (100% feasible C3)
    // ------------------------------------------------------------------

    // M11-TP1: ENTRY -> N0 -(B1:true)-> N2 -> EXIT (o == null -> return false)
    public void testM11_TP1_CopyInNullOrWrongId() {
        StubNationType nt = new StubNationType("model.nationType.test", spec);
        assertFalse(nt.copyIn(null));

        StubNationType otherId = new StubNationType("model.nationType.other", spec);
        assertFalse(nt.copyIn(otherId));
    }

    // M11-TP2: ENTRY -> N0 -(B1:false)-> N1 -(B2:true)-> N3 -> EXIT (o != null && super.copyIn(o) -> copy fields and return true)
    public void testM11_TP2_CopyInValid() throws Exception {
        StubNationType target = new StubNationType("model.nationType.test", spec);
        target.getFeatureContainer();
        StubNationType source = new StubNationType("model.nationType.test", spec);
        source.getFeatureContainer();
        SettlementType st = createSettlementType("model.settlement.s1", true);
        source.setSettlementTypes(Collections.singletonList(st));
        try (FreeColXMLReader xr = openReaderAtStartTag(
                "<stub-nation-type id=\"model.nationType.test\" number-of-settlements=\"high\" aggression=\"low\"/>")) {
            source.callReadAttributes(xr);
        }

        assertTrue(target.copyIn(source));
        assertEquals(SettlementNumber.HIGH, target.getNumberOfSettlements());
        assertEquals(AggressionLevel.LOW, target.getAggression());
        assertEquals(1, target.getSettlementTypes().size());
    }

    // ------------------------------------------------------------------
    // M12: writeAttributes(Lnet/sf/freecol/common/io/FreeColXMLWriter;)V — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M12-TP1: ENTRY -> N0 -> EXIT (Write number-of-settlements and aggression attributes)
    public void testM12_TP1_WriteAttributes() throws Exception {
        StubNationType nt = new StubNationType("model.nationType.test", spec);
        StringWriter sw = new StringWriter();
        try (FreeColXMLWriter xw = new FreeColXMLWriter(sw, FreeColXMLWriter.WriteScope.toSave())) {
            xw.writeStartDocument("UTF-8", "1.0");
            xw.writeStartElement(nt.getXMLTagName());
            nt.callWriteAttributes(xw);
            xw.writeEndElement();
            xw.writeEndDocument();
        }
        String xml = sw.toString();
        assertTrue(xml.contains("number-of-settlements=\"average\""));
        assertTrue(xml.contains("aggression=\"average\""));
    }

    // ------------------------------------------------------------------
    // M13: writeChildren(Lnet/sf/freecol/common/io/FreeColXMLWriter;)V — 2 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M13-TP1: ENTRY -> N0 -> N1 -(B1:true)-> N3 -> EXIT (0 settlement types -> loop skipped)
    public void testM13_TP1_WriteChildrenEmpty() throws Exception {
        StubNationType nt = new StubNationType("model.nationType.test", spec);
        StringWriter sw = new StringWriter();
        try (FreeColXMLWriter xw = new FreeColXMLWriter(sw, FreeColXMLWriter.WriteScope.toSave())) {
            xw.writeStartDocument("UTF-8", "1.0");
            xw.writeStartElement(nt.getXMLTagName());
            nt.callWriteChildren(xw);
            xw.writeEndElement();
            xw.writeEndDocument();
        }
        assertFalse(sw.toString().contains("<settlement "));
    }

    // M13-TP2: ENTRY -> N0 -> N1 -(B1:false)-> N2 -> N1 -(B1:true)-> N3 -> EXIT (>=1 settlement types -> loop body executed)
    public void testM13_TP2_WriteChildrenWithSettlement() throws Exception {
        StubNationType nt = new StubNationType("model.nationType.test", spec);
        SettlementType st = createSettlementType("model.settlement.s1", false);
        nt.setSettlementTypes(Collections.singletonList(st));
        StringWriter sw = new StringWriter();
        try (FreeColXMLWriter xw = new FreeColXMLWriter(sw, FreeColXMLWriter.WriteScope.toSave())) {
            xw.writeStartDocument("UTF-8", "1.0");
            xw.writeStartElement(nt.getXMLTagName());
            nt.callWriteChildren(xw);
            xw.writeEndElement();
            xw.writeEndDocument();
        }
        assertTrue(sw.toString().contains("<settlement "));
    }

    // ------------------------------------------------------------------
    // M14: readAttributes(Lnet/sf/freecol/common/io/FreeColXMLReader;)V — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M14-TP1: ENTRY -> N0 -> EXIT (Read number-of-settlements and aggression attributes)
    public void testM14_TP1_ReadAttributes() throws Exception {
        StubNationType nt = new StubNationType("model.nationType.test", spec);
        try (FreeColXMLReader xr = openReaderAtStartTag(
                "<stub-nation-type id=\"model.nationType.test\" number-of-settlements=\"low\" aggression=\"high\"/>")) {
            nt.callReadAttributes(xr);
        }
        assertEquals(SettlementNumber.LOW, nt.getNumberOfSettlements());
        assertEquals(AggressionLevel.HIGH, nt.getAggression());
    }

    // ------------------------------------------------------------------
    // M15: clearContainers(Lnet/sf/freecol/common/io/FreeColXMLReader;)V — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M15-TP1: ENTRY -> N0 -> EXIT (Clear settlementTypes to null)
    public void testM15_TP1_ClearContainers() throws Exception {
        StubNationType nt = new StubNationType("model.nationType.test", spec);
        nt.setSettlementTypes(Collections.singletonList(createSettlementType("model.settlement.s1", false)));
        try (FreeColXMLReader xr = openReaderAtStartTag("<stub-nation-type id=\"model.nationType.test\"/>")) {
            nt.callClearContainers(xr);
        }
        assertTrue(nt.getSettlementTypes().isEmpty());
    }

    // ------------------------------------------------------------------
    // M16: readChildren(Lnet/sf/freecol/common/io/FreeColXMLReader;)V — 5 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M16-TP1: ENTRY -> N0 -(B1:true)-> N5 -> EXIT (parent == this, no extends attribute)
    public void testM16_TP1_ReadChildrenNoExtends() throws Exception {
        StubNationType nt = new StubNationType("model.nationType.test", spec);
        try (FreeColXMLReader xr = openReaderAtStartTag("<stub-nation-type id=\"model.nationType.test\"/>")) {
            nt.callReadChildren(xr);
        }
        assertTrue(nt.getSettlementTypes().isEmpty());
    }

    // M16-TP2: ENTRY -> N0 -(B1:false)-> N1 -(B2:true)-> N3 -(B3:true)-> N5 -> EXIT (parent != this, parent.settlementTypes == null, !parent.isAbstractType())
    public void testM16_TP2_ReadChildrenExtendsConcreteNoSettlements() throws Exception {
        StubNationType parent = new StubNationType("model.nationType.cfgParentConcreteEmpty", spec);
        setAbstractType(parent, false);
        addTypeToSpec(parent);

        StubNationType child = new StubNationType("model.nationType.cfgChild1", spec);
        try (FreeColXMLReader xr = openReaderAtStartTag(
                "<stub-nation-type id=\"model.nationType.cfgChild1\" extends=\"model.nationType.cfgParentConcreteEmpty\"/>")) {
            child.callReadChildren(xr);
        }
        assertTrue(child.getSettlementTypes().isEmpty());
    }

    // M16-TP3: ENTRY -> N0 -(B1:false)-> N1 -(B2:true)-> N3 -(B3:false)-> N4 -> N5 -> EXIT (parent != this, parent.settlementTypes == null, parent.isAbstractType())
    public void testM16_TP3_ReadChildrenExtendsAbstractNoSettlements() throws Exception {
        StubNationType parent = new StubNationType("model.nationType.cfgParentAbstractEmpty", spec);
        setAbstractType(parent, true);
        addTypeToSpec(parent);

        StubNationType child = new StubNationType("model.nationType.cfgChild2", spec);
        try (FreeColXMLReader xr = openReaderAtStartTag(
                "<stub-nation-type id=\"model.nationType.cfgChild2\" extends=\"model.nationType.cfgParentAbstractEmpty\"/>")) {
            child.callReadChildren(xr);
        }
        assertTrue(child.getSettlementTypes().isEmpty());
    }

    // M16-TP4: ENTRY -> N0 -(B1:false)-> N1 -(B2:false)-> N2 -> N3 -(B3:true)-> N5 -> EXIT (parent != this, parent.settlementTypes != null, !parent.isAbstractType())
    public void testM16_TP4_ReadChildrenExtendsConcreteWithSettlements() throws Exception {
        StubNationType parent = new StubNationType("model.nationType.cfgParentConcreteFull", spec);
        setAbstractType(parent, false);
        parent.setSettlementTypes(Collections.singletonList(createSettlementType("model.settlement.p1", false)));
        addTypeToSpec(parent);

        StubNationType child = new StubNationType("model.nationType.cfgChild3", spec);
        try (FreeColXMLReader xr = openReaderAtStartTag(
                "<stub-nation-type id=\"model.nationType.cfgChild3\" extends=\"model.nationType.cfgParentConcreteFull\"/>")) {
            child.callReadChildren(xr);
        }
        assertEquals(1, child.getSettlementTypes().size());
    }

    // M16-TP5: ENTRY -> N0 -(B1:false)-> N1 -(B2:false)-> N2 -> N3 -(B3:false)-> N4 -> N5 -> EXIT (parent != this, parent.settlementTypes != null, parent.isAbstractType())
    public void testM16_TP5_ReadChildrenExtendsAbstractWithSettlements() throws Exception {
        StubNationType parent = new StubNationType("model.nationType.cfgParentAbstractFull", spec);
        setAbstractType(parent, true);
        parent.setSettlementTypes(Collections.singletonList(createSettlementType("model.settlement.p2", true)));
        addTypeToSpec(parent);

        StubNationType child = new StubNationType("model.nationType.cfgChild4", spec);
        try (FreeColXMLReader xr = openReaderAtStartTag(
                "<stub-nation-type id=\"model.nationType.cfgChild4\" extends=\"model.nationType.cfgParentAbstractFull\"/>")) {
            child.callReadChildren(xr);
        }
        assertEquals(1, child.getSettlementTypes().size());
    }

    // ------------------------------------------------------------------
    // M17: readChild(Lnet/sf/freecol/common/io/FreeColXMLReader;)V — 2 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M17-TP1: ENTRY -> N0 -(B1:false)-> N1 -> N3 -> EXIT (SETTLEMENT_TAG.equals(tag) == true -> addSettlementType)
    public void testM17_TP1_ReadChildSettlementTag() throws Exception {
        StubNationType nt = new StubNationType("model.nationType.test", spec);
        try (FreeColXMLReader xr = openReaderAtStartTag(
                "<stub-nation-type id=\"model.nationType.test\"><settlement id=\"model.settlement.s1\"/></stub-nation-type>")) {
            xr.nextTag();
            nt.callReadChild(xr);
        }
        assertEquals(1, nt.getSettlementTypes().size());
        assertEquals("model.settlement.s1", nt.getSettlementTypes().get(0).getId());
    }

    // M17-TP2: ENTRY -> N0 -(B1:true)-> N2 -> N3 -> EXIT (SETTLEMENT_TAG.equals(tag) == false -> super.readChild)
    public void testM17_TP2_ReadChildOtherTag() throws Exception {
        StubNationType nt = new StubNationType("model.nationType.test", spec);
        try (FreeColXMLReader xr = openReaderAtStartTag(
                "<stub-nation-type id=\"model.nationType.test\"><ability id=\"model.ability.foundColony\" value=\"true\"/></stub-nation-type>")) {
            xr.nextTag();
            nt.callReadChild(xr);
        }
        assertTrue(nt.hasAbility("model.ability.foundColony"));
    }
}
