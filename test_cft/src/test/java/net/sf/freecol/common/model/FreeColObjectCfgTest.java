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

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.StringReader;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import javax.xml.stream.XMLStreamException;

import net.sf.freecol.common.io.FreeColXMLReader;
import net.sf.freecol.common.io.FreeColXMLWriter;
import net.sf.freecol.common.io.FreeColXMLWriter.WriteScope;
import net.sf.freecol.common.util.LogBuilder;
import net.sf.freecol.util.test.FreeColTestCase;


/**
 * CFG-driven test suite for {@link FreeColObject} derived from
 * {@code test_cft/graph/net/sf/freecol/common/model/FreeColObject.json}.
 *
 * Covers all 102 methods (M1..M102, including {@code <init>} and {@code <clinit>})
 * and achieves 100% feasible C3 (node + edge) coverage across 163 Test Paths.
 *
 * Infeasible edges documented:
 * - M73 ({@code save(File, WriteScope, boolean)}): {@code N2 -> N4} ({@code B1:true},
 *   {@code fos == null} in try-with-resources auto-close) is infeasible because
 *   {@code Files.newOutputStream(Path)} never returns {@code null} (it either returns
 *   a non-null {@code OutputStream} or throws {@code IOException}).
 */
public class FreeColObjectCfgTest extends FreeColTestCase {

    private Game game;
    private Player dutchPlayer;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        // Pre-condition: Initialize standard game and Dutch player from spec
        game = getStandardGame();
        dutchPlayer = game.getPlayerByNationId("model.nation.dutch");
    }

    // ------------------------------------------------------------------
    // Pre-condition helpers & stubs
    // ------------------------------------------------------------------

    /**
     * Minimal concrete stub for {@link FreeColObject}.
     */
    public static class StubFreeColObject extends FreeColObject {
        private FeatureContainer featureContainer;
        private String label = "defaultLabel";
        private boolean containersCleared;
        private boolean attributesRead;
        private boolean consumeChild;
        private boolean throwOnToXML;

        StubFreeColObject() {
            super();
        }

        StubFreeColObject(String id) {
            super();
            setId(id);
        }

        void setFeatureContainer(FeatureContainer fc) {
            this.featureContainer = fc;
        }

        @Override
        public FeatureContainer getFeatureContainer() {
            return this.featureContainer;
        }

        public String getLabel() {
            return this.label;
        }

        public void setLabel(String label) {
            this.label = label;
        }

        boolean isContainersCleared() {
            return this.containersCleared;
        }

        boolean isAttributesRead() {
            return this.attributesRead;
        }

        void setConsumeChild(boolean consumeChild) {
            this.consumeChild = consumeChild;
        }

        void setThrowOnToXML(boolean throwOnToXML) {
            this.throwOnToXML = throwOnToXML;
        }

        @Override
        protected void clearContainers(FreeColXMLReader xr) throws XMLStreamException {
            super.clearContainers(xr);
            this.containersCleared = true;
        }

        @Override
        protected void readAttributes(FreeColXMLReader xr) throws XMLStreamException {
            super.readAttributes(xr);
            this.attributesRead = true;
        }

        @Override
        protected void readChild(FreeColXMLReader xr) throws XMLStreamException {
            if (consumeChild) {
                xr.closeTag(xr.getLocalName());
            } else {
                super.readChild(xr);
            }
        }

        @Override
        public void toXML(FreeColXMLWriter xw, String tag) throws XMLStreamException {
            if (throwOnToXML) {
                throw new XMLStreamException("Simulated XML failure");
            }
            super.toXML(xw, tag);
        }

        @Override
        public String getXMLTagName() {
            return "stub";
        }
    }

    private static class RecordingListener implements PropertyChangeListener {
        final List<PropertyChangeEvent> events = new ArrayList<>();

        @Override
        public void propertyChange(PropertyChangeEvent evt) {
            events.add(evt);
        }
    }

    private static FreeColXMLReader openReaderAtRoot(String xml) throws XMLStreamException {
        FreeColXMLReader xr = new FreeColXMLReader(new StringReader(xml));
        xr.nextTag();
        return xr;
    }

    private static PropertyChangeSupport getPcsViaReflection(FreeColObject obj) throws Exception {
        Method m = FreeColObject.class.getDeclaredMethod("getPropertyChangeSupport");
        m.setAccessible(true);
        return (PropertyChangeSupport) m.invoke(obj);
    }

    // ------------------------------------------------------------------
    // M1: <init>()V — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M1-TP1: ENTRY -> N0 -> EXIT
    public void testM1_TP1_noArgConstructor() {
        StubFreeColObject obj = new StubFreeColObject();
        assertNull(obj.getId());
    }

    // ------------------------------------------------------------------
    // M2: getFreeColObjectClassByName(String) — 2 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M2-TP1: ENTRY -> N0 -(B1:true)-> N2 -> EXIT (c == null, unknown class)
    public void testM2_TP1_getFreeColObjectClassByNameNotFound() {
        assertNull(FreeColObject.getFreeColObjectClassByName("NonExistentFreeColClass"));
    }

    // M2-TP2: ENTRY -> N0 -(B1:false)-> N1 -> EXIT (c != null, valid class)
    public void testM2_TP2_getFreeColObjectClassByNameFound() {
        assertEquals(Unit.class, FreeColObject.getFreeColObjectClassByName("unit"));
    }

    // ------------------------------------------------------------------
    // M3: getFreeColObjectClass() — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M3-TP1: ENTRY -> N0 -> EXIT
    public void testM3_TP1_getFreeColObjectClass() {
        StubFreeColObject obj = new StubFreeColObject();
        assertEquals(StubFreeColObject.class, obj.getFreeColObjectClass());
    }

    // ------------------------------------------------------------------
    // M4: getId() — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M4-TP1: ENTRY -> N0 -> EXIT
    public void testM4_TP1_getId() {
        StubFreeColObject obj = new StubFreeColObject("unit:1");
        assertEquals("unit:1", obj.getId());
    }

    // ------------------------------------------------------------------
    // M5: setId(String) — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M5-TP1: ENTRY -> N0 -> EXIT
    public void testM5_TP1_setId() {
        StubFreeColObject obj = new StubFreeColObject();
        obj.setId("colony:42");
        assertEquals("colony:42", obj.getId());
    }

    // ------------------------------------------------------------------
    // M6: idEquals(FreeColObject) — 4 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M6-TP1: ENTRY -> N0 -(B1:true)-> N4 -> N5 -> EXIT (other == null)
    public void testM6_TP1_idEqualsNullOther() {
        StubFreeColObject a = new StubFreeColObject("id:1");
        assertFalse(a.idEquals(null));
    }

    // M6-TP2: ENTRY -> N0 -(B1:false)-> N1 -(B2:true)-> N4 -> N5 -> EXIT (this.id == null)
    public void testM6_TP2_idEqualsNullSelfId() {
        StubFreeColObject a = new StubFreeColObject(null);
        StubFreeColObject b = new StubFreeColObject("id:1");
        assertFalse(a.idEquals(b));
    }

    // M6-TP3: ENTRY -> N0 -(B1:false)-> N1 -(B2:false)-> N2 -(B3:true)-> N4 -> N5 -> EXIT (ids differ)
    public void testM6_TP3_idEqualsDifferentIds() {
        StubFreeColObject a = new StubFreeColObject("id:1");
        StubFreeColObject b = new StubFreeColObject("id:2");
        assertFalse(a.idEquals(b));
    }

    // M6-TP4: ENTRY -> N0 -(B1:false)-> N1 -(B2:false)-> N2 -(B3:false)-> N3 -> N5 -> EXIT (ids equal)
    public void testM6_TP4_idEqualsMatchingIds() {
        StubFreeColObject a = new StubFreeColObject("id:1");
        StubFreeColObject b = new StubFreeColObject("id:1");
        assertTrue(a.idEquals(b));
    }

    // ------------------------------------------------------------------
    // M7: getSuffix(String) — 2 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M7-TP1: ENTRY -> N0 -(B1:true)-> N2 -> N3 -> EXIT (does not start with prefix)
    public void testM7_TP1_getSuffixWithoutPrefix() {
        StubFreeColObject obj = new StubFreeColObject("model.goods.food");
        assertEquals("model.goods.food", obj.getSuffix("other."));
    }

    // M7-TP2: ENTRY -> N0 -(B1:false)-> N1 -> N3 -> EXIT (starts with prefix)
    public void testM7_TP2_getSuffixWithPrefix() {
        StubFreeColObject obj = new StubFreeColObject("model.goods.food");
        assertEquals("food", obj.getSuffix("model.goods."));
    }

    // ------------------------------------------------------------------
    // M8: getSuffix() — 2 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M8-TP1: ENTRY -> N0 -(B1:true)-> N2 -> N3 -> EXIT (id != null)
    public void testM8_TP1_getSuffixNonNullId() {
        StubFreeColObject obj = new StubFreeColObject("model.goods.grain");
        assertEquals("grain", obj.getSuffix());
    }

    // M8-TP2: ENTRY -> N0 -(B1:false)-> N1 -> N3 -> EXIT (id == null)
    public void testM8_TP2_getSuffixNullId() {
        StubFreeColObject obj = new StubFreeColObject(null);
        assertNull(obj.getSuffix());
    }

    // ------------------------------------------------------------------
    // M9: getIdTypeByName(String) — 3 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M9-TP1: ENTRY -> N0 -(B1:true)-> N5 -> EXIT (id == null)
    public void testM9_TP1_getIdTypeByNameNull() {
        assertNull(FreeColObject.getIdTypeByName(null));
    }

    // M9-TP2: ENTRY -> N0 -(B1:false)-> N1 -(B2:true)-> N3 -> N4 -> EXIT (no ':')
    public void testM9_TP2_getIdTypeByNameNoColon() {
        assertEquals("model.unit.freeColonist",
            FreeColObject.getIdTypeByName("model.unit.freeColonist"));
    }

    // M9-TP3: ENTRY -> N0 -(B1:false)-> N1 -(B2:false)-> N2 -> N4 -> EXIT (has ':')
    public void testM9_TP3_getIdTypeByNameWithColon() {
        assertEquals("unit", FreeColObject.getIdTypeByName("unit:123"));
    }

    // ------------------------------------------------------------------
    // M10: getIdType() — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M10-TP1: ENTRY -> N0 -> EXIT
    public void testM10_TP1_getIdType() {
        StubFreeColObject obj = new StubFreeColObject("colony:99");
        assertEquals("colony", obj.getIdType());
    }

    // ------------------------------------------------------------------
    // M11: getIdNumber() — 5 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M11-TP1: ENTRY -> N0 -(B1:true)-> N7 -> EXIT (id == null)
    public void testM11_TP1_getIdNumberNullId() {
        StubFreeColObject obj = new StubFreeColObject(null);
        assertEquals(-1, obj.getIdNumber());
    }

    // M11-TP2: ENTRY -> N0 -(B1:false)-> N1 -(B2:true)-> N7 -> EXIT (no ':')
    public void testM11_TP2_getIdNumberNoColon() {
        StubFreeColObject obj = new StubFreeColObject("nocolon");
        assertEquals(-1, obj.getIdNumber());
    }

    // M11-TP3: ENTRY -> N0 -(B1:false)-> N1 -(B2:false)-> N2 -(B3:true)-> N4 -> N5 -> EXIT (numeric suffix)
    public void testM11_TP3_getIdNumberNumeric() {
        StubFreeColObject obj = new StubFreeColObject("unit:42");
        assertEquals(42, obj.getIdNumber());
    }

    // M11-TP4: ENTRY -> N0 -(B1:false)-> N1 -(B2:false)-> N2 -(B3:false)-> N3 -> N4 -> N5 -> EXIT ("am" prefix)
    public void testM11_TP4_getIdNumberAmPrefix() {
        StubFreeColObject obj = new StubFreeColObject("ai:am77");
        assertEquals(77, obj.getIdNumber());
    }

    // M11-TP5: ENTRY -> N0 -(B1:false)-> N1 -(B2:false)-> N2 -(B3:true)-> N4 -(EXC)-> N6 -> N7 -> EXIT (non-numeric)
    public void testM11_TP5_getIdNumberInvalidNumber() {
        StubFreeColObject obj = new StubFreeColObject("unit:abc");
        assertEquals(-1, obj.getIdNumber());
    }

    // ------------------------------------------------------------------
    // M12: compareIds(FreeColObject, FreeColObject) — 9 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M12-TP1: ENTRY -> N0(T) -> N5(T) -> N7(T) -> N12(T) -> N14(T:cmp<=0) -> N16(T:cmp>=0) -> N18 -> N19 -> EXIT
    public void testM12_TP1_compareIdsSameTypeCompareNumbers() {
        StubFreeColObject a = new StubFreeColObject("unit:1");
        StubFreeColObject b = new StubFreeColObject("unit:2");
        assertEquals(-1, FreeColObject.compareIds(a, b));
    }

    // M12-TP2: ENTRY -> N0(T) -> N5(T) -> N7(T) -> N12(T) -> N14(T:cmp<=0) -> N16(F:cmp<0) -> N17 -> N19 -> EXIT
    public void testM12_TP2_compareIdsTypeLessThan() {
        StubFreeColObject a = new StubFreeColObject("colony:1");
        StubFreeColObject b = new StubFreeColObject("unit:1");
        assertEquals(-1, FreeColObject.compareIds(a, b));
    }

    // M12-TP3: ENTRY -> N0(T) -> N5(T) -> N7(T) -> N12(T) -> N14(F:cmp>0) -> N15 -> N19 -> EXIT
    public void testM12_TP3_compareIdsTypeGreaterThan() {
        StubFreeColObject a = new StubFreeColObject("unit:1");
        StubFreeColObject b = new StubFreeColObject("colony:1");
        assertEquals(1, FreeColObject.compareIds(a, b));
    }

    // M12-TP4: ENTRY -> N0(T) -> N5(T) -> N7(T) -> N12(F:id2==null) -> N13 -> EXIT
    public void testM12_TP4_compareIdsId2Null() {
        StubFreeColObject a = new StubFreeColObject("unit:1");
        StubFreeColObject b = new StubFreeColObject(null);
        assertEquals(1, FreeColObject.compareIds(a, b));
    }

    // M12-TP5: ENTRY -> N0(T) -> N5(T) -> N7(F:id1==null) -> N8(T:id2!=null) -> N10 -> N11 -> EXIT
    public void testM12_TP5_compareIdsId1NullId2NonNull() {
        StubFreeColObject a = new StubFreeColObject(null);
        StubFreeColObject b = new StubFreeColObject("unit:1");
        assertEquals(-1, FreeColObject.compareIds(a, b));
    }

    // M12-TP6: ENTRY -> N0(T) -> N5(T) -> N7(F:id1==null) -> N8(F:id2==null) -> N9 -> N11 -> EXIT
    public void testM12_TP6_compareIdsBothIdsNull() {
        StubFreeColObject a = new StubFreeColObject(null);
        StubFreeColObject b = new StubFreeColObject(null);
        assertEquals(0, FreeColObject.compareIds(a, b));
    }

    // M12-TP7: ENTRY -> N0(T) -> N5(F:fco2==null) -> N6 -> EXIT
    public void testM12_TP7_compareIdsFco2Null() {
        StubFreeColObject a = new StubFreeColObject("unit:1");
        assertEquals(1, FreeColObject.compareIds(a, null));
    }

    // M12-TP8: ENTRY -> N0(F:fco1==null) -> N1(T:fco2!=null) -> N3 -> N4 -> EXIT
    public void testM12_TP8_compareIdsFco1NullFco2NonNull() {
        StubFreeColObject b = new StubFreeColObject("unit:1");
        assertEquals(-1, FreeColObject.compareIds(null, b));
    }

    // M12-TP9: ENTRY -> N0(F:fco1==null) -> N1(F:fco2==null) -> N2 -> N4 -> EXIT
    public void testM12_TP9_compareIdsBothNull() {
        assertEquals(0, FreeColObject.compareIds(null, null));
    }

    // ------------------------------------------------------------------
    // M13: getClassIndex() — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M13-TP1: ENTRY -> N0 -> EXIT
    public void testM13_TP1_getClassIndex() {
        StubFreeColObject obj = new StubFreeColObject();
        assertEquals(FreeColObject.DEFAULT_CLASS_INDEX, obj.getClassIndex());
    }

    // ------------------------------------------------------------------
    // M14: getObjectClassIndex(Object) — 2 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M14-TP1: ENTRY -> N0 -(B1:true)-> N2 -> N3 -> EXIT (not a FreeColObject)
    public void testM14_TP1_getObjectClassIndexNonFco() {
        assertEquals(FreeColObject.DEFAULT_CLASS_INDEX,
            FreeColObject.getObjectClassIndex("not-an-fco"));
    }

    // M14-TP2: ENTRY -> N0 -(B1:false)-> N1 -> N3 -> EXIT (is a FreeColObject)
    public void testM14_TP2_getObjectClassIndexFco() {
        StubFreeColObject obj = new StubFreeColObject() {
            @Override
            public int getClassIndex() {
                return 77;
            }
        };
        assertEquals(77, FreeColObject.getObjectClassIndex(obj));
    }

    // ------------------------------------------------------------------
    // M15: getSpecification() — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M15-TP1: ENTRY -> N0 -> EXIT
    public void testM15_TP1_getSpecification() {
        StubFreeColObject obj = new StubFreeColObject();
        assertNull(obj.getSpecification());
    }

    // ------------------------------------------------------------------
    // M16: setSpecification(Specification) — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M16-TP1: ENTRY -> N0 -> EXIT
    public void testM16_TP1_setSpecification() {
        StubFreeColObject obj = new StubFreeColObject();
        obj.setSpecification(spec());
        assertNull(obj.getSpecification());
    }

    // ------------------------------------------------------------------
    // M17: getGame() — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M17-TP1: ENTRY -> N0 -> EXIT
    public void testM17_TP1_getGame() {
        StubFreeColObject obj = new StubFreeColObject();
        assertNull(obj.getGame());
    }

    // ------------------------------------------------------------------
    // M18: setGame(Game) — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M18-TP1: ENTRY -> N0 -> EXIT
    public void testM18_TP1_setGame() {
        StubFreeColObject obj = new StubFreeColObject();
        obj.setGame(game);
        assertNull(obj.getGame());
    }

    // ------------------------------------------------------------------
    // M19: getPropertyChangeSupport() — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M19-TP1: ENTRY -> N0 -> EXIT
    public void testM19_TP1_getPropertyChangeSupport() throws Exception {
        StubFreeColObject obj = new StubFreeColObject();
        assertNull(getPcsViaReflection(obj));
    }

    // ------------------------------------------------------------------
    // M20: addPropertyChangeListener(PropertyChangeListener) — 2 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M20-TP1: ENTRY -> N0 -(B1:false)-> N1 -> N2 -> EXIT (pcs == null)
    // M20-TP2: ENTRY -> N0 -(B1:true)-> N2 -> EXIT (pcs != null)
    public void testM20_TP1_TP2_addPropertyChangeListener() {
        StubFreeColObject obj = new StubFreeColObject();
        RecordingListener l1 = new RecordingListener();
        RecordingListener l2 = new RecordingListener();
        obj.addPropertyChangeListener(l1);
        obj.addPropertyChangeListener(l2);
        assertEquals(2, obj.getPropertyChangeListeners().length);
    }

    // ------------------------------------------------------------------
    // M21: addPropertyChangeListener(String, PropertyChangeListener) — 2 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M21-TP1: ENTRY -> N0 -(B1:false)-> N1 -> N2 -> EXIT (pcs == null)
    // M21-TP2: ENTRY -> N0 -(B1:true)-> N2 -> EXIT (pcs != null)
    public void testM21_TP1_TP2_addNamedPropertyChangeListener() {
        StubFreeColObject obj = new StubFreeColObject();
        RecordingListener l1 = new RecordingListener();
        RecordingListener l2 = new RecordingListener();
        obj.addPropertyChangeListener("propA", l1);
        obj.addPropertyChangeListener("propB", l2);
        assertTrue(obj.hasListeners("propA"));
        assertTrue(obj.hasListeners("propB"));
    }

    // ------------------------------------------------------------------
    // M22: fireIndexedPropertyChange(String, int, boolean, boolean) — 2 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M22-TP1: ENTRY -> N0 -(B1:true)-> N2 -> EXIT (pcs == null)
    public void testM22_TP1_fireIndexedBooleanPcsNull() {
        StubFreeColObject obj = new StubFreeColObject();
        obj.fireIndexedPropertyChange("flag", 0, false, true);
        assertEquals(0, obj.getPropertyChangeListeners().length);
    }

    // M22-TP2: ENTRY -> N0 -(B1:false)-> N1 -> N2 -> EXIT (pcs != null)
    public void testM22_TP2_fireIndexedBooleanPcsNonNull() {
        StubFreeColObject obj = new StubFreeColObject();
        RecordingListener l = new RecordingListener();
        obj.addPropertyChangeListener(l);
        obj.fireIndexedPropertyChange("flag", 0, false, true);
        assertEquals(1, l.events.size());
    }

    // ------------------------------------------------------------------
    // M23: fireIndexedPropertyChange(String, int, int, int) — 2 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M23-TP1: ENTRY -> N0 -(B1:true)-> N2 -> EXIT (pcs == null)
    public void testM23_TP1_fireIndexedIntPcsNull() {
        StubFreeColObject obj = new StubFreeColObject();
        obj.fireIndexedPropertyChange("count", 1, 0, 5);
        assertEquals(0, obj.getPropertyChangeListeners().length);
    }

    // M23-TP2: ENTRY -> N0 -(B1:false)-> N1 -> N2 -> EXIT (pcs != null)
    public void testM23_TP2_fireIndexedIntPcsNonNull() {
        StubFreeColObject obj = new StubFreeColObject();
        RecordingListener l = new RecordingListener();
        obj.addPropertyChangeListener(l);
        obj.fireIndexedPropertyChange("count", 1, 0, 5);
        assertEquals(1, l.events.size());
    }

    // ------------------------------------------------------------------
    // M24: fireIndexedPropertyChange(String, int, Object, Object) — 2 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M24-TP1: ENTRY -> N0 -(B1:true)-> N2 -> EXIT (pcs == null)
    public void testM24_TP1_fireIndexedObjectPcsNull() {
        StubFreeColObject obj = new StubFreeColObject();
        obj.fireIndexedPropertyChange("item", 2, "a", "b");
        assertEquals(0, obj.getPropertyChangeListeners().length);
    }

    // M24-TP2: ENTRY -> N0 -(B1:false)-> N1 -> N2 -> EXIT (pcs != null)
    public void testM24_TP2_fireIndexedObjectPcsNonNull() {
        StubFreeColObject obj = new StubFreeColObject();
        RecordingListener l = new RecordingListener();
        obj.addPropertyChangeListener(l);
        obj.fireIndexedPropertyChange("item", 2, "a", "b");
        assertEquals(1, l.events.size());
    }

    // ------------------------------------------------------------------
    // M25: firePropertyChange(PropertyChangeEvent) — 2 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M25-TP1: ENTRY -> N0 -(B1:true)-> N2 -> EXIT (pcs == null)
    public void testM25_TP1_firePropertyChangeEventPcsNull() {
        StubFreeColObject obj = new StubFreeColObject();
        obj.firePropertyChange(new PropertyChangeEvent(obj, "p", 1, 2));
        assertEquals(0, obj.getPropertyChangeListeners().length);
    }

    // M25-TP2: ENTRY -> N0 -(B1:false)-> N1 -> N2 -> EXIT (pcs != null)
    public void testM25_TP2_firePropertyChangeEventPcsNonNull() {
        StubFreeColObject obj = new StubFreeColObject();
        RecordingListener l = new RecordingListener();
        obj.addPropertyChangeListener(l);
        obj.firePropertyChange(new PropertyChangeEvent(obj, "p", 1, 2));
        assertEquals(1, l.events.size());
    }

    // ------------------------------------------------------------------
    // M26: firePropertyChange(String, boolean, boolean) — 2 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M26-TP1: ENTRY -> N0 -(B1:true)-> N2 -> EXIT (pcs == null)
    public void testM26_TP1_firePropertyChangeBooleanPcsNull() {
        StubFreeColObject obj = new StubFreeColObject();
        obj.firePropertyChange("b", false, true);
        assertEquals(0, obj.getPropertyChangeListeners().length);
    }

    // M26-TP2: ENTRY -> N0 -(B1:false)-> N1 -> N2 -> EXIT (pcs != null)
    public void testM26_TP2_firePropertyChangeBooleanPcsNonNull() {
        StubFreeColObject obj = new StubFreeColObject();
        RecordingListener l = new RecordingListener();
        obj.addPropertyChangeListener(l);
        obj.firePropertyChange("b", false, true);
        assertEquals(1, l.events.size());
    }

    // ------------------------------------------------------------------
    // M27: firePropertyChange(String, int, int) — 2 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M27-TP1: ENTRY -> N0 -(B1:true)-> N2 -> EXIT (pcs == null)
    public void testM27_TP1_firePropertyChangeIntPcsNull() {
        StubFreeColObject obj = new StubFreeColObject();
        obj.firePropertyChange("i", 10, 20);
        assertEquals(0, obj.getPropertyChangeListeners().length);
    }

    // M27-TP2: ENTRY -> N0 -(B1:false)-> N1 -> N2 -> EXIT (pcs != null)
    public void testM27_TP2_firePropertyChangeIntPcsNonNull() {
        StubFreeColObject obj = new StubFreeColObject();
        RecordingListener l = new RecordingListener();
        obj.addPropertyChangeListener(l);
        obj.firePropertyChange("i", 10, 20);
        assertEquals(1, l.events.size());
    }

    // ------------------------------------------------------------------
    // M28: firePropertyChange(String, Object, Object) — 2 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M28-TP1: ENTRY -> N0 -(B1:true)-> N2 -> EXIT (pcs == null)
    public void testM28_TP1_firePropertyChangeObjectPcsNull() {
        StubFreeColObject obj = new StubFreeColObject();
        obj.firePropertyChange("o", "old", "new");
        assertEquals(0, obj.getPropertyChangeListeners().length);
    }

    // M28-TP2: ENTRY -> N0 -(B1:false)-> N1 -> N2 -> EXIT (pcs != null)
    public void testM28_TP2_firePropertyChangeObjectPcsNonNull() {
        StubFreeColObject obj = new StubFreeColObject();
        RecordingListener l = new RecordingListener();
        obj.addPropertyChangeListener(l);
        obj.firePropertyChange("o", "old", "new");
        assertEquals(1, l.events.size());
    }

    // ------------------------------------------------------------------
    // M29: getPropertyChangeListeners() — 2 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M29-TP1: ENTRY -> N0 -(B1:false)-> N1 -> N3 -> EXIT (pcs == null)
    // M29-TP2: ENTRY -> N0 -(B1:true)-> N2 -> N3 -> EXIT (pcs != null)
    public void testM29_TP1_TP2_getPropertyChangeListeners() {
        StubFreeColObject obj = new StubFreeColObject();
        assertEquals(0, obj.getPropertyChangeListeners().length);
        obj.addPropertyChangeListener(new RecordingListener());
        assertEquals(1, obj.getPropertyChangeListeners().length);
    }

    // ------------------------------------------------------------------
    // M30: getPropertyChangeListeners(String) — 2 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M30-TP1: ENTRY -> N0 -(B1:false)-> N1 -> N3 -> EXIT (pcs == null)
    // M30-TP2: ENTRY -> N0 -(B1:true)-> N2 -> N3 -> EXIT (pcs != null)
    public void testM30_TP1_TP2_getPropertyChangeListenersNamed() {
        StubFreeColObject obj = new StubFreeColObject();
        assertEquals(0, obj.getPropertyChangeListeners("p").length);
        obj.addPropertyChangeListener("p", new RecordingListener());
        assertEquals(1, obj.getPropertyChangeListeners("p").length);
    }

    // ------------------------------------------------------------------
    // M31: hasListeners(String) — 2 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M31-TP1: ENTRY -> N0 -(B1:false)-> N1 -> N3 -> EXIT (pcs == null)
    // M31-TP2: ENTRY -> N0 -(B1:true)-> N2 -> N3 -> EXIT (pcs != null)
    public void testM31_TP1_TP2_hasListeners() {
        StubFreeColObject obj = new StubFreeColObject();
        assertFalse(obj.hasListeners("p"));
        obj.addPropertyChangeListener("p", new RecordingListener());
        assertTrue(obj.hasListeners("p"));
    }

    // ------------------------------------------------------------------
    // M32: removePropertyChangeListener(PropertyChangeListener) — 2 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M32-TP1: ENTRY -> N0 -(B1:true)-> N2 -> EXIT (pcs == null)
    public void testM32_TP1_removePropertyChangeListenerPcsNull() {
        StubFreeColObject obj = new StubFreeColObject();
        obj.removePropertyChangeListener(new RecordingListener());
        assertEquals(0, obj.getPropertyChangeListeners().length);
    }

    // M32-TP2: ENTRY -> N0 -(B1:false)-> N1 -> N2 -> EXIT (pcs != null)
    public void testM32_TP2_removePropertyChangeListenerPcsNonNull() {
        StubFreeColObject obj = new StubFreeColObject();
        RecordingListener l = new RecordingListener();
        obj.addPropertyChangeListener(l);
        obj.removePropertyChangeListener(l);
        assertEquals(0, obj.getPropertyChangeListeners().length);
    }

    // ------------------------------------------------------------------
    // M33: removePropertyChangeListener(String, PropertyChangeListener) — 2 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M33-TP1: ENTRY -> N0 -(B1:true)-> N2 -> EXIT (pcs == null)
    public void testM33_TP1_removeNamedPropertyChangeListenerPcsNull() {
        StubFreeColObject obj = new StubFreeColObject();
        obj.removePropertyChangeListener("p", new RecordingListener());
        assertFalse(obj.hasListeners("p"));
    }

    // M33-TP2: ENTRY -> N0 -(B1:false)-> N1 -> N2 -> EXIT (pcs != null)
    public void testM33_TP2_removeNamedPropertyChangeListenerPcsNonNull() {
        StubFreeColObject obj = new StubFreeColObject();
        RecordingListener l = new RecordingListener();
        obj.addPropertyChangeListener("p", l);
        obj.removePropertyChangeListener("p", l);
        assertFalse(obj.hasListeners("p"));
    }

    // ------------------------------------------------------------------
    // M34..M46: FeatureContainer & Ability methods — 100% C3
    // ------------------------------------------------------------------

    // M34-TP1: ENTRY -> N0 -> EXIT
    public void testM34_TP1_getFeatureContainer() {
        FreeColObject obj = new FreeColObject() {
            @Override
            public String getXMLTagName() {
                return "anon";
            }
        };
        assertNull(obj.getFeatureContainer());
    }

    // M35-TP1: ENTRY -> N0 -> EXIT
    public void testM35_TP1_hasAbility1Arg() {
        StubFreeColObject obj = new StubFreeColObject();
        obj.setFeatureContainer(new FeatureContainer());
        obj.addAbility(new Ability(Ability.FOUND_COLONY, true));
        assertTrue(obj.hasAbility(Ability.FOUND_COLONY));
    }

    // M36-TP1: ENTRY -> N0 -> EXIT
    public void testM36_TP1_hasAbility2Args() {
        StubFreeColObject obj = new StubFreeColObject();
        obj.setFeatureContainer(new FeatureContainer());
        obj.addAbility(new Ability(Ability.FOUND_COLONY, true));
        assertTrue(obj.hasAbility(Ability.FOUND_COLONY, null));
    }

    // M37-TP1: ENTRY -> N0 -> EXIT
    public void testM37_TP1_hasAbility3Args() {
        StubFreeColObject obj = new StubFreeColObject();
        obj.setFeatureContainer(new FeatureContainer());
        obj.addAbility(new Ability(Ability.FOUND_COLONY, true));
        assertTrue(obj.hasAbility(Ability.FOUND_COLONY, null, new Turn(1)));
    }

    // M38-TP1: ENTRY -> N0 -(B1:true)-> N2 -> N3 -> EXIT (key absent -> first(...) == null)
    // M38-TP2: ENTRY -> N0 -(B1:false)-> N1 -> N3 -> EXIT (key present -> first(...) != null)
    public void testM38_TP1_TP2_containsAbilityKey() {
        StubFreeColObject obj = new StubFreeColObject();
        obj.setFeatureContainer(new FeatureContainer());
        assertFalse(obj.containsAbilityKey(Ability.FOUND_COLONY));
        obj.addAbility(new Ability(Ability.FOUND_COLONY, true));
        assertTrue(obj.containsAbilityKey(Ability.FOUND_COLONY));
    }

    // M39-TP1: ENTRY -> N0 -> EXIT
    public void testM39_TP1_getSortedAbilities() {
        StubFreeColObject obj = new StubFreeColObject();
        obj.setFeatureContainer(new FeatureContainer());
        obj.addAbility(new Ability(Ability.FOUND_COLONY, true));
        assertEquals(1, obj.getSortedAbilities().size());
    }

    // M40-TP1: ENTRY -> N0 -> EXIT
    public void testM40_TP1_getAbilities0Args() {
        StubFreeColObject obj = new StubFreeColObject();
        obj.setFeatureContainer(new FeatureContainer());
        obj.addAbility(new Ability(Ability.FOUND_COLONY, true));
        assertEquals(1, obj.getAbilities().collect(Collectors.toList()).size());
    }

    // M41-TP1: ENTRY -> N0 -> EXIT
    public void testM41_TP1_getAbilities1Arg() {
        StubFreeColObject obj = new StubFreeColObject();
        obj.setFeatureContainer(new FeatureContainer());
        obj.addAbility(new Ability(Ability.FOUND_COLONY, true));
        assertEquals(1, obj.getAbilities(Ability.FOUND_COLONY).collect(Collectors.toList()).size());
    }

    // M42-TP1: ENTRY -> N0 -> EXIT
    public void testM42_TP1_getAbilities2Args() {
        StubFreeColObject obj = new StubFreeColObject();
        obj.setFeatureContainer(new FeatureContainer());
        obj.addAbility(new Ability(Ability.FOUND_COLONY, true));
        assertEquals(1, obj.getAbilities(Ability.FOUND_COLONY, null).collect(Collectors.toList()).size());
    }

    // M43-TP1: ENTRY -> N0 -(B1:false)-> N1 -> N3 -> EXIT (fc == null)
    // M43-TP2: ENTRY -> N0 -(B1:true)-> N2 -> N3 -> EXIT (fc != null)
    public void testM43_TP1_TP2_getAbilities3Args() {
        StubFreeColObject obj = new StubFreeColObject();
        assertEquals(0, obj.getAbilities(Ability.FOUND_COLONY, null, null).count());
        obj.setFeatureContainer(new FeatureContainer());
        obj.addAbility(new Ability(Ability.FOUND_COLONY, true));
        assertEquals(1, obj.getAbilities(Ability.FOUND_COLONY, null, null).count());
    }

    // M44-TP1: ENTRY -> N0 -(B1:false)-> N1 -> N3 -> EXIT (fc == null)
    // M44-TP2: ENTRY -> N0 -(B1:true)-> N2 -> N3 -> EXIT (fc != null)
    public void testM44_TP1_TP2_addAbility() {
        StubFreeColObject obj = new StubFreeColObject();
        Ability ab = new Ability(Ability.FOUND_COLONY, true);
        assertFalse(obj.addAbility(ab));
        obj.setFeatureContainer(new FeatureContainer());
        assertTrue(obj.addAbility(ab));
    }

    // M45-TP1: ENTRY -> N0 -(B1:false)-> N1 -> N3 -> EXIT (fc == null)
    // M45-TP2: ENTRY -> N0 -(B1:true)-> N2 -> N3 -> EXIT (fc != null)
    public void testM45_TP1_TP2_removeAbility() {
        StubFreeColObject obj = new StubFreeColObject();
        Ability ab = new Ability(Ability.FOUND_COLONY, true);
        assertNull(obj.removeAbility(ab));
        obj.setFeatureContainer(new FeatureContainer());
        obj.addAbility(ab);
        assertEquals(ab, obj.removeAbility(ab));
    }

    // M46-TP1: ENTRY -> N0 -(B1:true)-> N2 -> EXIT (fc == null)
    // M46-TP2: ENTRY -> N0 -(B1:false)-> N1 -> N2 -> EXIT (fc != null)
    public void testM46_TP1_TP2_removeAbilities() {
        StubFreeColObject obj = new StubFreeColObject();
        obj.removeAbilities(Ability.FOUND_COLONY);
        obj.setFeatureContainer(new FeatureContainer());
        obj.addAbility(new Ability(Ability.FOUND_COLONY, true));
        obj.removeAbilities(Ability.FOUND_COLONY);
        assertFalse(obj.containsAbilityKey(Ability.FOUND_COLONY));
    }

    // ------------------------------------------------------------------
    // M47..M65: Modifier & Feature transfer methods — 100% C3
    // ------------------------------------------------------------------

    // M47-TP1: ENTRY -> N0 -> EXIT
    public void testM47_TP1_hasModifier1Arg() {
        StubFreeColObject obj = new StubFreeColObject();
        obj.setFeatureContainer(new FeatureContainer());
        obj.addModifier(new Modifier(Modifier.DEFENCE, 50f, Modifier.ModifierType.PERCENTAGE));
        assertTrue(obj.hasModifier(Modifier.DEFENCE));
    }

    // M48-TP1: ENTRY -> N0 -> EXIT
    public void testM48_TP1_hasModifier2Args() {
        StubFreeColObject obj = new StubFreeColObject();
        obj.setFeatureContainer(new FeatureContainer());
        obj.addModifier(new Modifier(Modifier.DEFENCE, 50f, Modifier.ModifierType.PERCENTAGE));
        assertTrue(obj.hasModifier(Modifier.DEFENCE, null));
    }

    // M49-TP1: ENTRY -> N0 -> EXIT
    public void testM49_TP1_hasModifier3Args() {
        StubFreeColObject obj = new StubFreeColObject();
        obj.setFeatureContainer(new FeatureContainer());
        obj.addModifier(new Modifier(Modifier.DEFENCE, 50f, Modifier.ModifierType.PERCENTAGE));
        assertTrue(obj.hasModifier(Modifier.DEFENCE, null, new Turn(1)));
    }

    // M50-TP1: ENTRY -> N0 -> EXIT
    public void testM50_TP1_containsModifierKey() {
        StubFreeColObject obj = new StubFreeColObject();
        obj.setFeatureContainer(new FeatureContainer());
        assertFalse(obj.containsModifierKey(Modifier.DEFENCE));
        obj.addModifier(new Modifier(Modifier.DEFENCE, 50f, Modifier.ModifierType.PERCENTAGE));
        assertTrue(obj.containsModifierKey(Modifier.DEFENCE));
    }

    // M51-TP1: ENTRY -> N0 -> EXIT
    public void testM51_TP1_getSortedModifiers() {
        StubFreeColObject obj = new StubFreeColObject();
        obj.setFeatureContainer(new FeatureContainer());
        obj.addModifier(new Modifier(Modifier.DEFENCE, 50f, Modifier.ModifierType.PERCENTAGE));
        assertEquals(1, obj.getSortedModifiers().size());
    }

    // M52-TP1: ENTRY -> N0 -(B1:false)-> N1 -> N3 -> EXIT (fc == null)
    // M52-TP2: ENTRY -> N0 -(B1:true)-> N2 -> N3 -> EXIT (fc != null)
    public void testM52_TP1_TP2_getModifiers0Args() {
        StubFreeColObject obj = new StubFreeColObject();
        assertEquals(0, obj.getModifiers().count());
        obj.setFeatureContainer(new FeatureContainer());
        obj.addModifier(new Modifier(Modifier.DEFENCE, 50f, Modifier.ModifierType.PERCENTAGE));
        assertEquals(1, obj.getModifiers().count());
    }

    // M53-TP1: ENTRY -> N0 -> EXIT
    public void testM53_TP1_getModifiers1Arg() {
        StubFreeColObject obj = new StubFreeColObject();
        obj.setFeatureContainer(new FeatureContainer());
        obj.addModifier(new Modifier(Modifier.DEFENCE, 50f, Modifier.ModifierType.PERCENTAGE));
        assertEquals(1, obj.getModifiers(Modifier.DEFENCE).count());
    }

    // M54-TP1: ENTRY -> N0 -> EXIT
    public void testM54_TP1_getModifiers2Args() {
        StubFreeColObject obj = new StubFreeColObject();
        obj.setFeatureContainer(new FeatureContainer());
        obj.addModifier(new Modifier(Modifier.DEFENCE, 50f, Modifier.ModifierType.PERCENTAGE));
        assertEquals(1, obj.getModifiers(Modifier.DEFENCE, null).count());
    }

    // M55-TP1: ENTRY -> N0 -(B1:false)-> N1 -> N3 -> EXIT (fc == null)
    // M55-TP2: ENTRY -> N0 -(B1:true)-> N2 -> N3 -> EXIT (fc != null)
    public void testM55_TP1_TP2_getModifiers3Args() {
        StubFreeColObject obj = new StubFreeColObject();
        assertEquals(0, obj.getModifiers(Modifier.DEFENCE, null, null).count());
        obj.setFeatureContainer(new FeatureContainer());
        obj.addModifier(new Modifier(Modifier.DEFENCE, 50f, Modifier.ModifierType.PERCENTAGE));
        assertEquals(1, obj.getModifiers(Modifier.DEFENCE, null, null).count());
    }

    // M56-TP1: ENTRY -> N0 -> EXIT
    public void testM56_TP1_apply3Args() {
        StubFreeColObject obj = new StubFreeColObject();
        obj.setFeatureContainer(new FeatureContainer());
        obj.addModifier(new Modifier(Modifier.DEFENCE, 50f, Modifier.ModifierType.PERCENTAGE));
        assertEquals(15f, obj.apply(10f, new Turn(1), Modifier.DEFENCE), 0.001f);
    }

    // M57-TP1: ENTRY -> N0 -> EXIT
    public void testM57_TP1_apply4Args() {
        StubFreeColObject obj = new StubFreeColObject();
        obj.setFeatureContainer(new FeatureContainer());
        obj.addModifier(new Modifier(Modifier.DEFENCE, 50f, Modifier.ModifierType.PERCENTAGE));
        assertEquals(15f, obj.apply(10f, new Turn(1), Modifier.DEFENCE, null), 0.001f);
    }

    // M58-TP1: ENTRY -> N0 -> EXIT
    public void testM58_TP1_applyModifiersStream() {
        Modifier mod = new Modifier(Modifier.DEFENCE, 5f, Modifier.ModifierType.ADDITIVE);
        assertEquals(15f, FreeColObject.applyModifiers(10f, new Turn(1), Arrays.asList(mod).stream()), 0.001f);
    }

    // M59-TP1: ENTRY -> N0 -> EXIT
    public void testM59_TP1_applyModifiersCollection() {
        Modifier mod = new Modifier(Modifier.DEFENCE, 5f, Modifier.ModifierType.ADDITIVE);
        assertEquals(15f, FreeColObject.applyModifiers(10f, new Turn(1), Arrays.asList(mod)), 0.001f);
    }

    // M60-TP1: ENTRY -> N0 -(B1:false)-> N1 -> EXIT (fc == null)
    // M60-TP2: ENTRY -> N0 -(B1:true)-> N2 -> EXIT (fc != null)
    public void testM60_TP1_TP2_addModifier() {
        StubFreeColObject obj = new StubFreeColObject();
        Modifier mod = new Modifier(Modifier.DEFENCE, 5f, Modifier.ModifierType.ADDITIVE);
        assertFalse(obj.addModifier(mod));
        obj.setFeatureContainer(new FeatureContainer());
        assertTrue(obj.addModifier(mod));
    }

    // M61-TP1: ENTRY -> N0 -(B1:false)-> N1 -> EXIT (fc == null)
    // M61-TP2: ENTRY -> N0 -(B1:true)-> N2 -> EXIT (fc != null)
    public void testM61_TP1_TP2_removeModifier() {
        StubFreeColObject obj = new StubFreeColObject();
        Modifier mod = new Modifier(Modifier.DEFENCE, 5f, Modifier.ModifierType.ADDITIVE);
        assertNull(obj.removeModifier(mod));
        obj.setFeatureContainer(new FeatureContainer());
        obj.addModifier(mod);
        assertEquals(mod, obj.removeModifier(mod));
    }

    // M62-TP1: ENTRY -> N0 -(B1:true)-> N2 -> EXIT (fc == null)
    // M62-TP2: ENTRY -> N0 -(B1:false)-> N1 -> N2 -> EXIT (fc != null)
    public void testM62_TP1_TP2_removeModifiers() {
        StubFreeColObject obj = new StubFreeColObject();
        obj.removeModifiers(Modifier.DEFENCE);
        obj.setFeatureContainer(new FeatureContainer());
        obj.addModifier(new Modifier(Modifier.DEFENCE, 5f, Modifier.ModifierType.ADDITIVE));
        obj.removeModifiers(Modifier.DEFENCE);
        assertFalse(obj.containsModifierKey(Modifier.DEFENCE));
    }

    // M63-TP1: ENTRY -> N0 -(B1:true)-> N2 -> EXIT (fc == null)
    // M63-TP2: ENTRY -> N0 -(B1:false)-> N1 -> N2 -> EXIT (fc != null)
    public void testM63_TP1_TP2_addFeatures() {
        StubFreeColObject target = new StubFreeColObject();
        StubFreeColObject source = new StubFreeColObject();
        source.setFeatureContainer(new FeatureContainer());
        source.addAbility(new Ability(Ability.FOUND_COLONY, true));
        target.addFeatures(source);
        target.setFeatureContainer(new FeatureContainer());
        target.addFeatures(source);
        assertTrue(target.hasAbility(Ability.FOUND_COLONY));
    }

    // M64-TP1: ENTRY -> N0 -(B1:true)-> N2 -> EXIT (fc == null)
    // M64-TP2: ENTRY -> N0 -(B1:false)-> N1 -> N2 -> EXIT (fc != null)
    public void testM64_TP1_TP2_removeFeatures() {
        StubFreeColObject target = new StubFreeColObject();
        UnitType colonist = spec().getUnitType("model.unit.freeColonist");
        target.removeFeatures(colonist);
        target.setFeatureContainer(new FeatureContainer());
        target.addFeatures(colonist);
        assertTrue(target.hasAbility(Ability.FOUND_COLONY));
        target.removeFeatures(colonist);
        assertFalse(target.hasAbility(Ability.FOUND_COLONY));
    }

    // M65-TP1: ENTRY -> N0 -> EXIT
    public void testM65_TP1_getDefenceModifiers() {
        StubFreeColObject obj = new StubFreeColObject();
        obj.setFeatureContainer(new FeatureContainer());
        obj.addModifier(new Modifier(Modifier.DEFENCE, 25f, Modifier.ModifierType.PERCENTAGE));
        assertEquals(1, obj.getDefenceModifiers().size());
    }

    // ------------------------------------------------------------------
    // M66: compareTo(FreeColObject) — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M66-TP1: ENTRY -> N0 -> EXIT
    public void testM66_TP1_compareTo() {
        StubFreeColObject a = new StubFreeColObject("unit:1");
        StubFreeColObject b = new StubFreeColObject("unit:2");
        assertTrue(a.compareTo(b) < 0);
    }

    // ------------------------------------------------------------------
    // M67: logFreeColObjects(Collection, LogBuilder) — 2 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M67-TP1: ENTRY -> N0 -> N1 -(B1:true)-> N3 -> EXIT (empty collection)
    public void testM67_TP1_logFreeColObjectsEmpty() {
        LogBuilder lb = new LogBuilder(16);
        FreeColObject.logFreeColObjects(Collections.<StubFreeColObject>emptyList(), lb);
        assertEquals("[]", lb.toString());
    }

    // M67-TP2: ENTRY -> N0 -> N1 -(B1:false)-> N2 -> N1 -(B1:true)-> N3 -> EXIT (non-empty collection)
    public void testM67_TP2_logFreeColObjectsNonEmpty() {
        LogBuilder lb = new LogBuilder(32);
        FreeColObject.logFreeColObjects(
            Arrays.asList(new StubFreeColObject("a.b"), new StubFreeColObject("c.d")), lb);
        assertEquals("[b d]", lb.toString());
    }

    // ------------------------------------------------------------------
    // M68: invokeMethod(String, Class, Object) — 4 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M68-TP1: ENTRY -> N0 -(B1:true)-> N5 -> EXIT (methodName == null)
    public void testM68_TP1_invokeMethodNullName() {
        StubFreeColObject obj = new StubFreeColObject();
        assertEquals("fallback", obj.invokeMethod(null, String.class, "fallback"));
    }

    // M68-TP2: ENTRY -> N0 -(B1:false)-> N1 -(B2:true)-> N5 -> EXIT (returnClass == null)
    public void testM68_TP2_invokeMethodNullReturnClass() {
        StubFreeColObject obj = new StubFreeColObject();
        assertEquals("fallback", obj.invokeMethod("getLabel", null, "fallback"));
    }

    // M68-TP3: ENTRY -> N0 -(B1:false)-> N1 -(B2:false)-> N2 -> N3 -> EXIT (valid invocation)
    public void testM68_TP3_invokeMethodSuccess() {
        StubFreeColObject obj = new StubFreeColObject();
        obj.setLabel("ok");
        assertEquals("ok", obj.invokeMethod("getLabel", String.class, "fallback"));
    }

    // M68-TP4: ENTRY -> N0 -(B1:false)-> N1 -(B2:false)-> N2 -(EXC)-> N4 -> N5 -> EXIT (exception caught)
    public void testM68_TP4_invokeMethodException() {
        StubFreeColObject obj = new StubFreeColObject();
        assertEquals("fallback", obj.invokeMethod("nonExistentMethod", String.class, "fallback"));
    }

    // ------------------------------------------------------------------
    // M69: getDisplayObject() — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M69-TP1: ENTRY -> N0 -> EXIT
    public void testM69_TP1_getDisplayObject() {
        StubFreeColObject obj = new StubFreeColObject();
        assertSame(obj, obj.getDisplayObject());
    }

    // ------------------------------------------------------------------
    // M70: dumpObject() — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M70-TP1: ENTRY -> N0 -> EXIT
    public void testM70_TP1_dumpObject() {
        StubFreeColObject obj = new StubFreeColObject("stub:dump");
        java.io.PrintStream origErr = System.err;
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            System.setErr(new java.io.PrintStream(baos));
            obj.dumpObject();
            assertTrue(baos.toString().contains("stub:dump"));
        } finally {
            System.setErr(origErr);
        }
    }

    // ------------------------------------------------------------------
    // M71..M74: save(...) overloads — 100% feasible C3
    // ------------------------------------------------------------------

    // M71-TP1: ENTRY -> N0 -> EXIT
    public void testM71_TP1_saveFile() throws Exception {
        StubFreeColObject obj = new StubFreeColObject("stub:save1");
        File tmp = Files.createTempFile("fco_save1", ".xml").toFile();
        tmp.deleteOnExit();
        assertTrue(obj.save(tmp));
    }

    // M72-TP1: ENTRY -> N0 -> EXIT
    public void testM72_TP1_saveFileWithScope() throws Exception {
        StubFreeColObject obj = new StubFreeColObject("stub:save2");
        File tmp = Files.createTempFile("fco_save2", ".xml").toFile();
        tmp.deleteOnExit();
        assertTrue(obj.save(tmp, WriteScope.toSave()));
    }

    // M73-TP1: ENTRY -> N0 -> N1 -> N2 -(B1:false)-> N3 -> N4 -> EXIT (normal save; B1:true is infeasible)
    public void testM73_TP1_saveFileWithScopeAndPretty() throws Exception {
        StubFreeColObject obj = new StubFreeColObject("stub:save3");
        File tmp = Files.createTempFile("fco_save3", ".xml").toFile();
        tmp.deleteOnExit();
        assertTrue(obj.save(tmp, WriteScope.toSave(), true));
    }

    // M73-TP2: ENTRY -> N0 -(EXC)-> N10 -> EXIT (IOException caught)
    public void testM73_TP2_saveFileIOException() throws Exception {
        StubFreeColObject obj = new StubFreeColObject("stub:saveFail");
        File dir = Files.createTempDirectory("fco_dir").toFile();
        dir.deleteOnExit();
        assertFalse(obj.save(dir, WriteScope.toSave(), false));
    }

    // M74-TP1: ENTRY -> N0 -(B1:true)-> N2 -> N3 -> N4 -> N10 -> N13 -> EXIT (scope != null)
    public void testM74_TP1_saveOutputStreamNonNullScope() {
        StubFreeColObject obj = new StubFreeColObject("stub:stream1");
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        assertTrue(obj.save(out, WriteScope.toServer(), false));
        assertTrue(out.toString().contains("stub:stream1"));
    }

    // M74-TP2: ENTRY -> N0 -(B1:false)-> N1 -> N2 -> N3 -> N4 -> N10 -> N13 -> EXIT (scope == null)
    public void testM74_TP2_saveOutputStreamNullScope() {
        StubFreeColObject obj = new StubFreeColObject("stub:stream2");
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        assertTrue(obj.save(out, null, false));
        assertTrue(out.toString().contains("stub:stream2"));
    }

    // M74-TP3: XMLStreamException caught -> returns false
    public void testM74_TP3_saveOutputStreamXmlException() {
        StubFreeColObject obj = new StubFreeColObject("stub:streamErr");
        obj.setThrowOnToXML(true);
        assertFalse(obj.save(new ByteArrayOutputStream(), WriteScope.toServer(), false));
    }

    // M74-TP4: IOException caught in try-with-resources close() (ret already true)
    public void testM74_TP4_saveOutputStreamIOException() {
        StubFreeColObject obj = new StubFreeColObject("stub:ioErr");
        OutputStream failing = new OutputStream() {
            @Override
            public void write(int b) throws IOException {
                throw new IOException("disk full");
            }
            @Override
            public void write(byte[] b, int off, int len) throws IOException {
                throw new IOException("disk full");
            }
        };
        assertTrue(obj.save(failing, WriteScope.toServer(), false));
    }

    // ------------------------------------------------------------------
    // M75..M78: serialize(...) overloads — 100% C3
    // ------------------------------------------------------------------

    // M75-TP1: ENTRY -> N0 -> EXIT
    public void testM75_TP1_serializeNoArgs() throws Exception {
        StubFreeColObject obj = new StubFreeColObject("stub:ser1");
        assertTrue(obj.serialize().contains("stub:ser1"));
    }

    // M76-TP1: ENTRY -> N0 -> EXIT
    public void testM76_TP1_serializePlayer() throws Exception {
        StubFreeColObject obj = new StubFreeColObject("stub:ser2");
        assertTrue(obj.serialize(dutchPlayer).contains("stub:ser2"));
    }

    // M77-TP1: ENTRY -> N0 -> EXIT
    public void testM77_TP1_serializeScope() throws Exception {
        StubFreeColObject obj = new StubFreeColObject("stub:ser3");
        assertTrue(obj.serialize(WriteScope.toSave()).contains("stub:ser3"));
    }

    // M78-TP1: ENTRY -> N0 -> N1 -> N2 -(B1:false)-> N3 -> N5 -> N11 -> N13 -> EXIT (fields == null)
    public void testM78_TP1_serializeFull() throws Exception {
        StubFreeColObject obj = new StubFreeColObject("stub:ser4");
        String xml = obj.serialize(WriteScope.toServer(), null);
        assertTrue(xml.contains("stub:ser4"));
        assertFalse(xml.contains("partial=\"true\""));
    }

    // M78-TP2: ENTRY -> N0 -> N1 -> N2 -(B1:true)-> N4 -> N5 -> N11 -> N13 -> EXIT (fields != null)
    public void testM78_TP2_serializePartial() throws Exception {
        StubFreeColObject obj = new StubFreeColObject("stub:ser5");
        String xml = obj.serialize(WriteScope.toServer(), Arrays.asList("k", "v"));
        assertTrue(xml.contains("partial=\"true\""));
        assertTrue(xml.contains("k=\"v\""));
    }

    // ------------------------------------------------------------------
    // M79..M82: copy(...) overloads — 100% C3
    // ------------------------------------------------------------------

    // M79-TP1: ENTRY -> N0 -> EXIT
    public void testM79_TP1_copyGame() {
        StringTemplate st = StringTemplate.template("model.source.base");
        StringTemplate copied = st.copy(game);
        assertNotNull(copied);
    }

    // M80-TP1: ENTRY -> N0 -> EXIT
    public void testM80_TP1_copyGamePlayer() {
        StringTemplate st = StringTemplate.template("model.source.base");
        StringTemplate copied = st.copy(game, dutchPlayer);
        assertNotNull(copied);
    }

    // M81-TP1: ENTRY -> N0 -> N1 -> N2 -> N3 -> N9 -> N11 -> EXIT
    public void testM81_TP1_copyGameClass() {
        StringTemplate st = StringTemplate.template("model.source.base");
        StringTemplate copied = st.copy(game, StringTemplate.class);
        assertNotNull(copied);
    }

    // M81-TP2: XMLStreamException caught -> returns null
    public void testM81_TP2_copyGameClassXmlException() {
        StubFreeColObject obj = new StubFreeColObject("stub:copyErr");
        obj.setThrowOnToXML(true);
        assertNull(obj.copy(game, StubFreeColObject.class));
    }

    // M82-TP1: ENTRY -> N0 -> N1 -> N2 -> N3 -> N9 -> N11 -> EXIT
    public void testM82_TP1_copyGameClassPlayer() {
        StringTemplate st = StringTemplate.template("model.source.base");
        StringTemplate copied = st.copy(game, StringTemplate.class, dutchPlayer);
        assertNotNull(copied);
    }

    // M82-TP2: XMLStreamException caught -> returns null
    public void testM82_TP2_copyGameClassPlayerXmlException() {
        StubFreeColObject obj = new StubFreeColObject("stub:copyErrP");
        obj.setThrowOnToXML(true);
        assertNull(obj.copy(game, StubFreeColObject.class, dutchPlayer));
    }

    // ------------------------------------------------------------------
    // M83: copyIn(FreeColObject) — 3 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M83-TP1: ENTRY -> N0 -(B1:false)-> N1 -> EXIT (fco == null)
    public void testM83_TP1_copyInIncompatible() {
        StubFreeColObject target = new StubFreeColObject("stub:1");
        assertFalse(target.copyIn(null));
    }

    // M83-TP2: ENTRY -> N0 -(B1:true)-> N2 -> N3 -(B2:true)-> N5 -> EXIT (0 listeners)
    public void testM83_TP2_copyInNoListeners() {
        StubFreeColObject target = new StubFreeColObject("stub:1");
        StubFreeColObject other = new StubFreeColObject("stub:1");
        assertTrue(target.copyIn(other));
    }

    // M83-TP3: ENTRY -> N0 -(B1:true)-> N2 -> N3 -(B2:false)-> N4 -> N3 -(B2:true)-> N5 -> EXIT (>=1 listener)
    public void testM83_TP3_copyInTransfersListeners() {
        StubFreeColObject target = new StubFreeColObject("stub:1");
        StubFreeColObject other = new StubFreeColObject("stub:1");
        RecordingListener l = new RecordingListener();
        other.addPropertyChangeListener(l);
        assertTrue(target.copyIn(other));
        assertEquals(0, other.getPropertyChangeListeners().length);
        assertEquals(1, target.getPropertyChangeListeners().length);
    }

    // ------------------------------------------------------------------
    // M84: copyInCast(FreeColObject, Class) — 4 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M84-TP1: ENTRY -> N0 -(B1:false)-> N1 -> EXIT (other == null)
    public void testM84_TP1_copyInCastNullOther() {
        StubFreeColObject target = new StubFreeColObject("stub:1");
        assertNull(target.copyInCast(null, StubFreeColObject.class));
    }

    // M84-TP2: ENTRY -> N0 -(B1:true)-> N2 -(B2:false)-> N3 -> EXIT (!idEquals(other))
    public void testM84_TP2_copyInCastDifferentId() {
        StubFreeColObject target = new StubFreeColObject("stub:1");
        StubFreeColObject other = new StubFreeColObject("stub:2");
        assertNull(target.copyInCast(other, StubFreeColObject.class));
    }

    // M84-TP3: ENTRY -> N0 -(B1:true)-> N2 -(B2:true)-> N4 -> N5 -> EXIT (cast succeeds)
    public void testM84_TP3_copyInCastSuccess() {
        StubFreeColObject target = new StubFreeColObject("stub:1");
        StubFreeColObject other = new StubFreeColObject("stub:1");
        assertSame(other, target.copyInCast(other, StubFreeColObject.class));
    }

    // M84-TP4: ENTRY -> N0 -(B1:true)-> N2 -(B2:true)-> N4 -(EXC)-> N6 -> EXIT (ClassCastException)
    public void testM84_TP4_copyInCastIncompatibleType() {
        StubFreeColObject target = new StubFreeColObject("stub:1");
        StubFreeColObject other = new StubFreeColObject("stub:1");
        assertNull(target.copyInCast(other, Unit.class));
    }

    // ------------------------------------------------------------------
    // M85..M88: toXML, writeAttributes, writeChildren — 100% C3
    // ------------------------------------------------------------------

    // M85-TP1, M86-TP1, M87-TP1, M88-TP1: getId() != null
    public void testM85_M86_M87_M88_toXMLAndWriteAttributesWithId() throws Exception {
        StubFreeColObject obj = new StubFreeColObject("stub:xml1");
        String xml = obj.serialize();
        assertTrue(xml.contains("<stub id=\"stub:xml1\">"));
    }

    // M87-TP2: ENTRY -> N0 -(B1:false)-> N1 -> N3 -> EXIT (getId() == null)
    public void testM87_TP2_writeAttributesNullId() throws Exception {
        StubFreeColObject obj = new StubFreeColObject(null);
        String xml = obj.serialize();
        assertTrue(xml.contains("<stub></stub>") || xml.contains("<stub/>"));
    }

    // ------------------------------------------------------------------
    // M89: toXMLPartial(FreeColXMLWriter, String[]) — 3 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M89-TP1: ENTRY -> N0 -> N1 -(B1:true)-> N7 -> EXIT (empty fields array)
    public void testM89_TP1_toXMLPartialArrayEmpty() throws Exception {
        StubFreeColObject obj = new StubFreeColObject("stub:p1");
        java.io.StringWriter sw = new java.io.StringWriter();
        try (FreeColXMLWriter xw = new FreeColXMLWriter(sw, WriteScope.toServer())) {
            obj.toXMLPartial(xw, new String[0]);
        }
        assertTrue(sw.toString().contains("partial=\"true\""));
    }

    // M89-TP2: ENTRY -> N0 -> N1 -(B1:false)-> N2 -> N3 -> N4 -> N6 -> N1 -(B1:true)-> N7 -> EXIT
    public void testM89_TP2_toXMLPartialArrayWithField() throws Exception {
        StubFreeColObject obj = new StubFreeColObject("stub:p2");
        obj.setLabel("hello");
        java.io.StringWriter sw = new java.io.StringWriter();
        try (FreeColXMLWriter xw = new FreeColXMLWriter(sw, WriteScope.toServer())) {
            obj.toXMLPartial(xw, new String[] { "label" });
        }
        assertTrue(sw.toString().contains("label=\"hello\""));
    }

    // M89-TP3: IntrospectorException caught (N5)
    public void testM89_TP3_toXMLPartialArrayUnknownField() throws Exception {
        StubFreeColObject obj = new StubFreeColObject("stub:p3");
        java.io.StringWriter sw = new java.io.StringWriter();
        try (FreeColXMLWriter xw = new FreeColXMLWriter(sw, WriteScope.toServer())) {
            obj.toXMLPartial(xw, new String[] { "noSuchField" });
        }
        assertTrue(sw.toString().contains("partial=\"true\""));
    }

    // ------------------------------------------------------------------
    // M90: toXMLPartial(FreeColXMLWriter, List<String>) — 3 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M90-TP1: ENTRY -> N0 -> N1 -> N2 -(B1:true)-> N4 -> N5 -> N7 -> EXIT (empty list)
    public void testM90_TP1_toXMLPartialListEmpty() throws Exception {
        StubFreeColObject obj = new StubFreeColObject("stub:pl1");
        java.io.StringWriter sw = new java.io.StringWriter();
        try (FreeColXMLWriter xw = new FreeColXMLWriter(sw, WriteScope.toServer())) {
            obj.toXMLPartial(xw, Collections.<String>emptyList());
        }
        assertTrue(sw.toString().contains("partial=\"true\""));
    }

    // M90-TP2: ENTRY -> N0 -> N1 -> N2 -(B1:false)-> N3 -> N2 -(B1:true)-> N4 -> N5 -> N7 -> EXIT
    public void testM90_TP2_toXMLPartialListWithPairs() throws Exception {
        StubFreeColObject obj = new StubFreeColObject("stub:pl2");
        java.io.StringWriter sw = new java.io.StringWriter();
        try (FreeColXMLWriter xw = new FreeColXMLWriter(sw, WriteScope.toServer())) {
            obj.toXMLPartial(xw, Arrays.asList("attrA", "valA"));
        }
        assertTrue(sw.toString().contains("attrA=\"valA\""));
    }

    // M90-TP3: Exception caught when fields == null (N6)
    public void testM90_TP3_toXMLPartialListNullException() throws Exception {
        StubFreeColObject obj = new StubFreeColObject("stub:pl3");
        java.io.StringWriter sw = new java.io.StringWriter();
        try (FreeColXMLWriter xw = new FreeColXMLWriter(sw, WriteScope.toServer())) {
            obj.toXMLPartial(xw, (List<String>) null);
        }
        assertTrue(sw.toString().contains("partial=\"true\""));
    }

    // ------------------------------------------------------------------
    // M91: readFromXML(FreeColXMLReader) — 5 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M91-TP1: ENTRY -> N0 -(B1:false)-> N1 -> N7 -> EXIT (partial="true")
    public void testM91_TP1_readFromXMLPartialBranch() throws Exception {
        StubFreeColObject obj = new StubFreeColObject();
        try (FreeColXMLReader xr = openReaderAtRoot("<stub id=\"stub:r1\" partial=\"true\"/>")) {
            obj.readFromXML(xr);
        }
        assertEquals("stub:r1", obj.getId());
    }

    // M91-TP2: ENTRY -> N0 -(B1:true)-> N2 -(B2:true)-> N4 -(B3:true)-> N6 -> N7 -> EXIT
    public void testM91_TP2_readFromXMLPreserveBoth() throws Exception {
        StubFreeColObject obj = new StubFreeColObject("orig");
        try (FreeColXMLReader xr = openReaderAtRoot(
            "<stub id=\"stub:r2\" preserve=\"true\" preserve-attributes=\"true\"/>")) {
            obj.readFromXML(xr);
        }
        assertFalse(obj.isContainersCleared());
        assertFalse(obj.isAttributesRead());
        assertEquals("orig", obj.getId());
    }

    // M91-TP3: ENTRY -> N0 -(B1:true)-> N2 -(B2:true)-> N4 -(B3:false)-> N5 -> N6 -> N7 -> EXIT
    public void testM91_TP3_readFromXMLPreserveContainersOnly() throws Exception {
        StubFreeColObject obj = new StubFreeColObject("orig");
        try (FreeColXMLReader xr = openReaderAtRoot("<stub id=\"stub:r3\" preserve=\"true\"/>")) {
            obj.readFromXML(xr);
        }
        assertFalse(obj.isContainersCleared());
        assertTrue(obj.isAttributesRead());
        assertEquals("stub:r3", obj.getId());
    }

    // M91-TP4: ENTRY -> N0 -(B1:true)-> N2 -(B2:false)-> N3 -> N4 -(B3:true)-> N6 -> N7 -> EXIT
    public void testM91_TP4_readFromXMLPreserveAttributesOnly() throws Exception {
        StubFreeColObject obj = new StubFreeColObject("orig");
        try (FreeColXMLReader xr = openReaderAtRoot("<stub id=\"stub:r4\" preserve-attributes=\"true\"/>")) {
            obj.readFromXML(xr);
        }
        assertTrue(obj.isContainersCleared());
        assertFalse(obj.isAttributesRead());
        assertEquals("orig", obj.getId());
    }

    // M91-TP5: ENTRY -> N0 -(B1:true)-> N2 -(B2:false)-> N3 -> N4 -(B3:false)-> N5 -> N6 -> N7 -> EXIT
    public void testM91_TP5_readFromXMLFullNormal() throws Exception {
        StubFreeColObject obj = new StubFreeColObject("orig");
        try (FreeColXMLReader xr = openReaderAtRoot("<stub id=\"stub:r5\"/>")) {
            obj.readFromXML(xr);
        }
        assertTrue(obj.isContainersCleared());
        assertTrue(obj.isAttributesRead());
        assertEquals("stub:r5", obj.getId());
    }

    // ------------------------------------------------------------------
    // M92: clearContainers(FreeColXMLReader) — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M92-TP1: ENTRY -> N0 -> EXIT
    public void testM92_TP1_clearContainers() throws Exception {
        StubFreeColObject obj = new StubFreeColObject();
        try (FreeColXMLReader xr = openReaderAtRoot("<stub/>")) {
            obj.clearContainers(xr);
        }
        assertTrue(obj.isContainersCleared());
    }

    // ------------------------------------------------------------------
    // M93: readAttributes(FreeColXMLReader) — 2 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M93-TP1: ENTRY -> N0 -(B1:true)-> N2 -> EXIT (newId == null)
    public void testM93_TP1_readAttributesNoId() throws Exception {
        StubFreeColObject obj = new StubFreeColObject("keep");
        try (FreeColXMLReader xr = openReaderAtRoot("<stub/>")) {
            obj.readAttributes(xr);
        }
        assertEquals("keep", obj.getId());
    }

    // M93-TP2: ENTRY -> N0 -(B1:false)-> N1 -> N2 -> EXIT (newId != null)
    public void testM93_TP2_readAttributesWithId() throws Exception {
        StubFreeColObject obj = new StubFreeColObject("keep");
        try (FreeColXMLReader xr = openReaderAtRoot("<stub id=\"updated\"/>")) {
            obj.readAttributes(xr);
        }
        assertEquals("updated", obj.getId());
    }

    // ------------------------------------------------------------------
    // M94: readChildren(FreeColXMLReader) — 4 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M94-TP1: ENTRY -> N0 -(B1:false)-> N1 -> EXIT(throw) (tag == null)
    public void testM94_TP1_readChildrenNullTagThrows() throws Exception {
        StubFreeColObject obj = new StubFreeColObject();
        try (FreeColXMLReader xr = new FreeColXMLReader(new StringReader("<stub/>")) {
            @Override
            public String getLocalName() {
                return null;
            }
        }) {
            try {
                obj.readChildren(xr);
                fail("Expected XMLStreamException");
            } catch (XMLStreamException expected) {
                // expected
            }
        }
    }

    // M94-TP2: ENTRY -> N0 -(B1:true)-> N2 -(B2:true)-> N4 -> N6 -> EXIT (no child tags)
    public void testM94_TP2_readChildrenEmpty() throws Exception {
        StubFreeColObject obj = new StubFreeColObject();
        try (FreeColXMLReader xr = openReaderAtRoot("<stub/>")) {
            obj.readChildren(xr);
        }
    }

    // M94-TP3: ENTRY -> N0 -(B1:true)-> N2 -(B2:false)-> N3 -> N2 -(B2:true)-> N4 -> N6 -> EXIT (child consumed)
    public void testM94_TP3_readChildrenWithChild() throws Exception {
        StubFreeColObject obj = new StubFreeColObject();
        obj.setConsumeChild(true);
        try (FreeColXMLReader xr = openReaderAtRoot("<stub><child/></stub>")) {
            obj.readChildren(xr);
        }
    }

    // M94-TP4: readChild throws XMLStreamException -> caught at N5
    public void testM94_TP4_readChildrenCatchesUnexpectedChildException() throws Exception {
        StubFreeColObject obj = new StubFreeColObject();
        try (FreeColXMLReader xr = openReaderAtRoot("<stub><unexpected/></stub>")) {
            try {
                obj.readChildren(xr);
            } catch (XMLStreamException ignored) {
                // expectTag may throw after catching child exception
            }
        }
    }

    // ------------------------------------------------------------------
    // M95: readChild(FreeColXMLReader) — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M95-TP1: ENTRY -> N0 -> EXIT(throw)
    public void testM95_TP1_readChildThrows() throws Exception {
        StubFreeColObject obj = new StubFreeColObject();
        try (FreeColXMLReader xr = openReaderAtRoot("<stub><child/></stub>")) {
            xr.nextTag();
            try {
                obj.readChild(xr);
                fail("Expected XMLStreamException");
            } catch (XMLStreamException expected) {
                // expected
            }
        }
    }

    // ------------------------------------------------------------------
    // M96: readFromXMLPartial(FreeColXMLReader) — 3 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M96-TP1: ENTRY -> N0 -> N1 -(B1:true)-> N9 -> EXIT (0 attributes)
    public void testM96_TP1_readFromXMLPartialNoAttributes() throws Exception {
        StubFreeColObject obj = new StubFreeColObject("init");
        try (FreeColXMLReader xr = openReaderAtRoot("<stub/>")) {
            obj.readFromXMLPartial(xr);
        }
        assertNull(obj.getId());
    }

    // M96-TP2: ENTRY -> N0 -> N1 -(B1:false)-> N2 -(B2:true)-> N8 -> N1 -> N2 -(B2:false)-> N3 -(B3:false)-> N4 -> N8 -> N1 -> N2 -(B2:false)-> N3 -(B3:true)-> N5 -> N6 -> N8 -> N1 -(B1:true)-> N9 -> EXIT
    public void testM96_TP2_readFromXMLPartialAllAttributeBranches() throws Exception {
        StubFreeColObject obj = new StubFreeColObject();
        try (FreeColXMLReader xr = openReaderAtRoot(
            "<stub id=\"stub:part\" partial=\"true\" label=\"updated\"/>")) {
            obj.readFromXMLPartial(xr);
        }
        assertEquals("stub:part", obj.getId());
        assertEquals("updated", obj.getLabel());
    }

    // M96-TP3: Introspector setter fails -> caught at N7
    public void testM96_TP3_readFromXMLPartialUnknownAttribute() throws Exception {
        StubFreeColObject obj = new StubFreeColObject();
        try (FreeColXMLReader xr = openReaderAtRoot(
            "<stub id=\"stub:part2\" partial=\"true\" unknownField=\"x\"/>")) {
            obj.readFromXMLPartial(xr);
        }
        assertEquals("stub:part2", obj.getId());
    }

    // ------------------------------------------------------------------
    // M97: arrayKey(int) — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M97-TP1: ENTRY -> N0 -> EXIT
    public void testM97_TP1_arrayKey() {
        assertEquals("x5", FreeColObject.arrayKey(5));
    }

    // ------------------------------------------------------------------
    // M98: equals(Object) — 2 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M98-TP1: ENTRY -> N0 -(B1:true)-> N2 -> EXIT (not FreeColObject)
    public void testM98_TP1_equalsNonFreeColObject() {
        StubFreeColObject obj = new StubFreeColObject("id:1");
        assertFalse(obj.equals("id:1"));
    }

    // M98-TP2: ENTRY -> N0 -(B1:false)-> N1 -> EXIT (is FreeColObject)
    public void testM98_TP2_equalsFreeColObject() {
        StubFreeColObject a = new StubFreeColObject("id:1");
        StubFreeColObject b = new StubFreeColObject("id:1");
        StubFreeColObject c = new StubFreeColObject("id:2");
        assertTrue(a.equals(b));
        assertFalse(a.equals(c));
    }

    // ------------------------------------------------------------------
    // M99: hashCode() — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M99-TP1: ENTRY -> N0 -> EXIT
    public void testM99_TP1_hashCode() {
        StubFreeColObject a = new StubFreeColObject("id:1");
        StubFreeColObject b = new StubFreeColObject("id:1");
        assertEquals(a.hashCode(), b.hashCode());
    }

    // ------------------------------------------------------------------
    // M100: toString() — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M100-TP1: ENTRY -> N0 -> EXIT
    public void testM100_TP1_toString() {
        StubFreeColObject obj = new StubFreeColObject("id:9");
        assertEquals(StubFreeColObject.class.getName() + ":id:9", obj.toString());
    }

    // ------------------------------------------------------------------
    // M101: compareTo(Object) bridge — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M101-TP1: ENTRY -> N0 -> EXIT
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public void testM101_TP1_compareToBridge() {
        Comparable a = new StubFreeColObject("unit:1");
        StubFreeColObject b = new StubFreeColObject("unit:2");
        assertTrue(a.compareTo(b) < 0);
    }

    // ------------------------------------------------------------------
    // M102: <clinit>() — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M102-TP1: ENTRY -> N0 -> EXIT (class static initializer & fcoComparator)
    public void testM102_TP1_clinitAndComparator() {
        StubFreeColObject a = new StubFreeColObject("unit:1");
        StubFreeColObject b = new StubFreeColObject("unit:2");
        assertTrue(FreeColObject.fcoComparator.compare(a, b) < 0);
    }
}
