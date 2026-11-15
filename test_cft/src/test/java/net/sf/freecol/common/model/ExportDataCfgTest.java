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
 * CFG-driven test suite for {@link ExportData} derived from
 * {@code test_cft/graph/net/sf/freecol/common/model/ExportData.json}.
 *
 * Covers all 18 methods (M1..M18) and achieves 100% feasible C3 (node + edge)
 * coverage across 19 Test Paths.
 *
 * Infeasible edges documented:
 * - M15 ({@code copyIn}): {@code N1 -> N2} ({@code B2:false}) — {@code o} is a
 *   non-null {@code ExportData} already validated by {@code copyInCast}, so
 *   {@code FreeColObject.copyIn(o)} always returns {@code true}.
 */
public class ExportDataCfgTest extends FreeColTestCase {

    private GoodsType sugarType;
    private ExportData data;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        // Pre-condition: standard specification goods type and default ExportData instance
        sugarType = spec().getGoodsType("model.goods.sugar");
        data = new ExportData(sugarType, 25);
    }

    // ------------------------------------------------------------------
    // Pre-condition helpers & stubs
    // ------------------------------------------------------------------

    private String toXml(ExportData target) throws Exception {
        StringWriter sw = new StringWriter();
        try (FreeColXMLWriter xw = new FreeColXMLWriter(sw,
                FreeColXMLWriter.WriteScope.toSave(), false)) {
            target.toXML(xw);
        }
        return sw.toString();
    }

    private FreeColXMLReader openReader(String xml) throws Exception {
        FreeColXMLReader xr = new FreeColXMLReader(new StringReader(xml));
        xr.nextTag();
        return xr;
    }

    // ------------------------------------------------------------------
    // M1: ExportData() — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M1-TP1: ENTRY -> N0 -> EXIT (default no-arg constructor)
    public void testM1_TP1_defaultConstructor() {
        ExportData ed = new ExportData();
        assertEquals(90, ed.getHighLevel());
        assertEquals(10, ed.getLowLevel());
        assertEquals(-1, ed.getImportLevel());
        assertEquals(50, ed.getExportLevel());
        assertFalse(ed.getExported());
    }

    // ------------------------------------------------------------------
    // M2: ExportData(GoodsType, int) — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M2-TP1: ENTRY -> N0 -> EXIT (constructor with goodsType and importLevel)
    public void testM2_TP1_goodsTypeConstructor() {
        ExportData ed = new ExportData(sugarType, 40);
        assertEquals(sugarType.getId(), ed.getId());
        assertEquals(40, ed.getImportLevel());
    }

    // ------------------------------------------------------------------
    // M3: ExportData(FreeColXMLReader) — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M3-TP1: ENTRY -> N0 -> EXIT (constructor reading from XML stream)
    public void testM3_TP1_xmlReaderConstructor() throws Exception {
        String xml = "<exportData id=\"model.goods.sugar\" exported=\"true\" "
            + "highLevel=\"80\" lowLevel=\"20\" importLevel=\"30\" exportLevel=\"60\"/>";
        try (FreeColXMLReader xr = openReader(xml)) {
            ExportData ed = new ExportData(xr);
            assertEquals("model.goods.sugar", ed.getId());
            assertTrue(ed.getExported());
            assertEquals(80, ed.getHighLevel());
            assertEquals(20, ed.getLowLevel());
            assertEquals(30, ed.getImportLevel());
            assertEquals(60, ed.getExportLevel());
        }
    }

    // ------------------------------------------------------------------
    // M4: getHighLevel() — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M4-TP1: ENTRY -> N0 -> EXIT (return highLevel)
    public void testM4_TP1_getHighLevel() {
        assertEquals(90, data.getHighLevel());
    }

    // ------------------------------------------------------------------
    // M5: setHighLevel(int) — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M5-TP1: ENTRY -> N0 -> EXIT (set highLevel and return this)
    public void testM5_TP1_setHighLevel() {
        ExportData ret = data.setHighLevel(75);
        assertSame(data, ret);
        assertEquals(75, data.getHighLevel());
    }

    // ------------------------------------------------------------------
    // M6: getLowLevel() — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M6-TP1: ENTRY -> N0 -> EXIT (return lowLevel)
    public void testM6_TP1_getLowLevel() {
        assertEquals(10, data.getLowLevel());
    }

    // ------------------------------------------------------------------
    // M7: setLowLevel(int) — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M7-TP1: ENTRY -> N0 -> EXIT (set lowLevel and return this)
    public void testM7_TP1_setLowLevel() {
        ExportData ret = data.setLowLevel(15);
        assertSame(data, ret);
        assertEquals(15, data.getLowLevel());
    }

    // ------------------------------------------------------------------
    // M8: getImportLevel() — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M8-TP1: ENTRY -> N0 -> EXIT (return importLevel)
    public void testM8_TP1_getImportLevel() {
        assertEquals(25, data.getImportLevel());
    }

    // ------------------------------------------------------------------
    // M9: getEffectiveImportLevel(int) — 2 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M9-TP1: ENTRY -> N0 -(B1:true)-> N2 -> N3 -> EXIT (importLevel < 0 -> returns capacity)
    public void testM9_TP1_getEffectiveImportLevelUsesCapacityWhenNegative() {
        data.setImportLevel(-1);
        assertEquals(100, data.getEffectiveImportLevel(100));
    }

    // M9-TP2: ENTRY -> N0 -(B1:false)-> N1 -> N3 -> EXIT (importLevel >= 0 -> returns importLevel)
    public void testM9_TP2_getEffectiveImportLevelUsesImportLevelWhenNonNegative() {
        data.setImportLevel(0);
        assertEquals(0, data.getEffectiveImportLevel(100));
        data.setImportLevel(45);
        assertEquals(45, data.getEffectiveImportLevel(100));
    }

    // ------------------------------------------------------------------
    // M10: setImportLevel(int) — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M10-TP1: ENTRY -> N0 -> EXIT (set importLevel and return this)
    public void testM10_TP1_setImportLevel() {
        ExportData ret = data.setImportLevel(60);
        assertSame(data, ret);
        assertEquals(60, data.getImportLevel());
    }

    // ------------------------------------------------------------------
    // M11: getExportLevel() — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M11-TP1: ENTRY -> N0 -> EXIT (return exportLevel)
    public void testM11_TP1_getExportLevel() {
        assertEquals(50, data.getExportLevel());
    }

    // ------------------------------------------------------------------
    // M12: setExportLevel(int) — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M12-TP1: ENTRY -> N0 -> EXIT (set exportLevel and return this)
    public void testM12_TP1_setExportLevel() {
        ExportData ret = data.setExportLevel(35);
        assertSame(data, ret);
        assertEquals(35, data.getExportLevel());
    }

    // ------------------------------------------------------------------
    // M13: getExported() — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M13-TP1: ENTRY -> N0 -> EXIT (return exported)
    public void testM13_TP1_getExported() {
        assertFalse(data.getExported());
    }

    // ------------------------------------------------------------------
    // M14: setExported(boolean) — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M14-TP1: ENTRY -> N0 -> EXIT (set exported)
    public void testM14_TP1_setExported() {
        data.setExported(true);
        assertTrue(data.getExported());
    }

    // ------------------------------------------------------------------
    // M15: copyIn(FreeColObject) — 2 Test Paths (100% feasible C3)
    // ------------------------------------------------------------------

    // M15-TP1: ENTRY -> N0 -(B1:true)-> N2 -> EXIT (o == null -> returns false)
    public void testM15_TP1_copyInNullOrIncompatibleReturnsFalse() {
        assertFalse(data.copyIn(null));
        assertFalse(data.copyIn(sugarType));
    }

    // M15-TP2: ENTRY -> N0 -(B1:false)-> N1 -(B2:true)-> N3 -> EXIT (o != null && super.copyIn(o) -> copies fields)
    public void testM15_TP2_copyInValidExportDataCopiesFields() {
        ExportData other = new ExportData(sugarType, 70)
            .setHighLevel(95)
            .setLowLevel(5)
            .setExportLevel(55);
        other.setExported(true);

        assertTrue(data.copyIn(other));
        assertEquals(95, data.getHighLevel());
        assertEquals(5, data.getLowLevel());
        assertEquals(70, data.getImportLevel());
        assertEquals(55, data.getExportLevel());
        assertTrue(data.getExported());
    }

    // ------------------------------------------------------------------
    // M16: writeAttributes(FreeColXMLWriter) — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M16-TP1: ENTRY -> N0 -> EXIT (serialize attributes to XML)
    public void testM16_TP1_writeAttributes() throws Exception {
        data.setExported(true);
        data.setHighLevel(85);
        data.setLowLevel(12);
        data.setExportLevel(42);
        String xml = toXml(data);
        assertTrue(xml.contains("exported=\"true\""));
        assertTrue(xml.contains("highLevel=\"85\""));
        assertTrue(xml.contains("lowLevel=\"12\""));
        assertTrue(xml.contains("importLevel=\"25\""));
        assertTrue(xml.contains("exportLevel=\"42\""));
    }

    // ------------------------------------------------------------------
    // M17: readAttributes(FreeColXMLReader) — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M17-TP1: ENTRY -> N0 -> EXIT (read attributes with defaults when omitted)
    public void testM17_TP1_readAttributes() throws Exception {
        String xml = "<exportData id=\"model.goods.sugar\"/>";
        ExportData ed = new ExportData();
        try (FreeColXMLReader xr = openReader(xml)) {
            ed.readFromXML(xr);
        }
        assertFalse(ed.getExported());
        assertEquals(90, ed.getHighLevel());
        assertEquals(10, ed.getLowLevel());
        assertEquals(-1, ed.getImportLevel());
        assertEquals(50, ed.getExportLevel());
    }

    // ------------------------------------------------------------------
    // M18: getXMLTagName() — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M18-TP1: ENTRY -> N0 -> EXIT (return TAG)
    public void testM18_TP1_getXMLTagName() {
        assertEquals(ExportData.TAG, data.getXMLTagName());
    }
}
