package net.sf.freecol.common.model;

import java.io.StringReader;
import java.io.StringWriter;
import javax.xml.stream.XMLStreamException;

import net.sf.freecol.common.io.FreeColXMLReader;
import net.sf.freecol.common.io.FreeColXMLWriter;
import net.sf.freecol.util.test.FreeColTestCase;

/**
 * CFG-driven test suite for {@link TileTypeChange} derived from
 * {@code test_cft/graph/net/sf/freecol/common/model/TileTypeChange.json}.
 *
 * Covers all 14 methods (M1..M14) and achieves 100% feasible C3 (node + edge)
 * coverage across 21 Test Paths.
 *
 * Infeasible edges documented:
 * - M6 ({@code copyIn}): {@code N1 -> N2} ({@code B2:false}) — {@code super.copyIn(o)}
 *   delegates through {@link FreeColSpecObjectType#copyIn(FreeColObject)} to
 *   {@link FreeColObject#copyIn(FreeColObject)}, which only returns {@code false} when
 *   {@code o == null}; once {@code o != null} at {@code N1}, {@code super.copyIn(o)}
 *   always returns {@code true}.
 */
public class TileTypeChangeCfgTest extends FreeColTestCase {

    private TileType plainsType;
    private TileType grasslandType;
    private GoodsType lumberType;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        // Pre-condition: standard specification with plains, grassland, and lumber types
        plainsType = spec().getTileType("model.tile.plains");
        grasslandType = spec().getTileType("model.tile.grassland");
        lumberType = spec().getGoodsType("model.goods.lumber");
    }

    // ------------------------------------------------------------------
    // Pre-condition helpers & stubs
    // ------------------------------------------------------------------

    private String writeTileTypeChangeXml(TileTypeChange change) throws Exception {
        StringWriter sw = new StringWriter();
        try (FreeColXMLWriter xw = new FreeColXMLWriter(sw,
                FreeColXMLWriter.WriteScope.toSave(), false)) {
            change.toXML(xw);
            xw.flush();
        }
        return sw.toString();
    }

    private FreeColXMLReader openReaderAtTag(String xml) throws Exception {
        FreeColXMLReader xr = new FreeColXMLReader(new StringReader(xml));
        xr.nextTag();
        return xr;
    }

    private TileTypeChange createSampleChange(boolean withProduction) throws Exception {
        String xml = withProduction
            ? "<tile-type-change from=\"model.tile.plains\" to=\"model.tile.grassland\">"
              + "<production goods-type=\"model.goods.lumber\" value=\"20\"/></tile-type-change>"
            : "<tile-type-change from=\"model.tile.plains\" to=\"model.tile.grassland\"/>";
        try (FreeColXMLReader xr = openReaderAtTag(xml)) {
            return new TileTypeChange(xr, spec());
        }
    }

    // ------------------------------------------------------------------
    // M1: <init>(Specification) — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M1-TP1: ENTRY -> N0 -> EXIT (constructor with Specification)
    public void testM1_TP1_SpecConstructor() {
        TileTypeChange change = new TileTypeChange(spec());
        assertSame(spec(), change.getSpecification());
        assertNull(change.getFrom());
        assertNull(change.getTo());
        assertNull(change.getProduction());
    }

    // ------------------------------------------------------------------
    // M2: <init>(FreeColXMLReader, Specification) — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M2-TP1: ENTRY -> N0 -> EXIT (XML reader constructor)
    public void testM2_TP1_XmlReaderConstructor() throws Exception {
        TileTypeChange change = createSampleChange(true);
        assertSame(plainsType, change.getFrom());
        assertSame(grasslandType, change.getTo());
        assertNotNull(change.getProduction());
        assertSame(lumberType, change.getProduction().getType());
        assertEquals(20, change.getProduction().getAmount());
    }

    // ------------------------------------------------------------------
    // M3: getFrom() — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M3-TP1: ENTRY -> N0 -> EXIT (returns original TileType)
    public void testM3_TP1_GetFrom() throws Exception {
        TileTypeChange change = createSampleChange(false);
        assertSame(plainsType, change.getFrom());
    }

    // ------------------------------------------------------------------
    // M4: getTo() — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M4-TP1: ENTRY -> N0 -> EXIT (returns destination TileType)
    public void testM4_TP1_GetTo() throws Exception {
        TileTypeChange change = createSampleChange(false);
        assertSame(grasslandType, change.getTo());
    }

    // ------------------------------------------------------------------
    // M5: getProduction() — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M5-TP1: ENTRY -> N0 -> EXIT (returns consequent AbstractGoods)
    public void testM5_TP1_GetProduction() throws Exception {
        TileTypeChange change = createSampleChange(true);
        assertEquals(20, change.getProduction().getAmount());
    }

    // ------------------------------------------------------------------
    // M6: copyIn(FreeColObject) — 2 Test Paths (100% feasible C3)
    // ------------------------------------------------------------------

    // M6-TP1: ENTRY -> N0 -(B1:true)-> N2 -> EXIT (o == null because other is null or wrong type)
    public void testM6_TP1_CopyInNullOrWrongType() {
        TileTypeChange target = new TileTypeChange(spec());
        assertFalse(target.copyIn((TileTypeChange) null));
        assertFalse(target.copyIn(plainsType));
        assertNull(target.getFrom());
    }

    // M6-TP2: ENTRY -> N0 -(B1:false)-> N1 -(B2:true)-> N3 -> EXIT (valid TileTypeChange copied)
    public void testM6_TP2_CopyInSuccess() throws Exception {
        TileTypeChange target = new TileTypeChange(spec());
        target.setId("ttc:1");
        target.getFeatureContainer();
        TileTypeChange source = createSampleChange(true);
        source.setId("ttc:1");
        assertTrue(target.copyIn(source));
        assertSame(plainsType, target.getFrom());
        assertSame(grasslandType, target.getTo());
        assertEquals(source.getProduction(), target.getProduction());
    }

    // ------------------------------------------------------------------
    // M7: writeAttributes(FreeColXMLWriter) — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M7-TP1: ENTRY -> N0 -> EXIT (writes from and to attributes)
    public void testM7_TP1_WriteAttributes() throws Exception {
        TileTypeChange change = createSampleChange(false);
        String xml = writeTileTypeChangeXml(change);
        assertTrue(xml.contains("from=\"model.tile.plains\""));
        assertTrue(xml.contains("to=\"model.tile.grassland\""));
    }

    // ------------------------------------------------------------------
    // M8: writeChildren(FreeColXMLWriter) — 2 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M8-TP1: ENTRY -> N0 -(B1:true)-> N2 -> EXIT (production == null)
    public void testM8_TP1_WriteChildrenWithoutProduction() throws Exception {
        TileTypeChange change = createSampleChange(false);
        String xml = writeTileTypeChangeXml(change);
        assertFalse(xml.contains("<production"));
    }

    // M8-TP2: ENTRY -> N0 -(B1:false)-> N1 -> N2 -> EXIT (production != null)
    public void testM8_TP2_WriteChildrenWithProduction() throws Exception {
        TileTypeChange change = createSampleChange(true);
        String xml = writeTileTypeChangeXml(change);
        assertTrue(xml.contains("<production"));
        assertTrue(xml.contains("goods-type=\"model.goods.lumber\""));
        assertTrue(xml.contains("value=\"20\""));
    }

    // ------------------------------------------------------------------
    // M9: readAttributes(FreeColXMLReader) — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M9-TP1: ENTRY -> N0 -> EXIT (reads from and to attributes)
    public void testM9_TP1_ReadAttributes() throws Exception {
        String xml = "<tile-type-change from=\"model.tile.plains\" to=\"model.tile.grassland\"/>";
        TileTypeChange change = new TileTypeChange(spec());
        try (FreeColXMLReader xr = openReaderAtTag(xml)) {
            change.readAttributes(xr);
        }
        assertSame(plainsType, change.getFrom());
        assertSame(grasslandType, change.getTo());
    }

    // ------------------------------------------------------------------
    // M10: clearContainers(FreeColXMLReader) — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M10-TP1: ENTRY -> N0 -> EXIT (clears production to null)
    public void testM10_TP1_ClearContainers() throws Exception {
        TileTypeChange change = createSampleChange(true);
        assertNotNull(change.getProduction());
        try (FreeColXMLReader xr = openReaderAtTag("<tile-type-change/>")) {
            change.clearContainers(xr);
        }
        assertNull(change.getProduction());
    }

    // ------------------------------------------------------------------
    // M11: readChildren(FreeColXMLReader) — 3 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M11-TP1: ENTRY -> N0 -> N1 -(B1:true)-> N6 -> EXIT (0 child tags)
    public void testM11_TP1_ReadChildrenEmpty() throws Exception {
        TileTypeChange change = new TileTypeChange(spec());
        try (FreeColXMLReader xr = openReaderAtTag("<tile-type-change/>")) {
            change.readChildren(xr);
        }
        assertNull(change.getProduction());
    }

    // M11-TP2: ENTRY -> N0 -> N1 -(B1:false)-> N2 -(B2:true)-> N4 -> EXIT (bogus child tag throws XMLStreamException)
    public void testM11_TP2_ReadChildrenBogusTagThrows() throws Exception {
        TileTypeChange change = new TileTypeChange(spec());
        String xml = "<tile-type-change><bogus/></tile-type-change>";
        try (FreeColXMLReader xr = openReaderAtTag(xml)) {
            change.readChildren(xr);
            fail("Expected XMLStreamException for bogus child tag");
        } catch (XMLStreamException expected) {
            assertTrue(expected.getMessage().contains("Bogus TileTypeChange tag"));
        }
    }

    // M11-TP3: ENTRY -> N0 -> N1 -(B1:false)-> N2 -(B2:false)-> N3 -> N5 -> N1 -(B1:true)-> N6 -> EXIT (production child tag)
    public void testM11_TP3_ReadChildrenWithProduction() throws Exception {
        TileTypeChange change = new TileTypeChange(spec());
        String xml = "<tile-type-change><production goods-type=\"model.goods.lumber\" value=\"15\"/></tile-type-change>";
        try (FreeColXMLReader xr = openReaderAtTag(xml)) {
            change.readChildren(xr);
        }
        assertNotNull(change.getProduction());
        assertSame(lumberType, change.getProduction().getType());
        assertEquals(15, change.getProduction().getAmount());
    }

    // ------------------------------------------------------------------
    // M12: getXMLTagName() — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M12-TP1: ENTRY -> N0 -> EXIT (returns "tile-type-change")
    public void testM12_TP1_GetXMLTagName() {
        TileTypeChange change = new TileTypeChange(spec());
        assertEquals(TileTypeChange.TAG, change.getXMLTagName());
        assertEquals("tile-type-change", change.getXMLTagName());
    }

    // ------------------------------------------------------------------
    // M13: equals(Object) — 4 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M13-TP1: ENTRY -> N0 -(B1:true)-> N6 -> EXIT (!(other instanceof TileTypeChange))
    public void testM13_TP1_EqualsNonTileTypeChange() {
        TileTypeChange change = new TileTypeChange(spec());
        assertFalse(change.equals("not-a-tile-type-change"));
        assertFalse(change.equals(null));
    }

    // M13-TP2: ENTRY -> N0 -(B1:false)-> N1 -(B2:true)-> N4 -> N5 -> EXIT (compareTo != 0)
    public void testM13_TP2_EqualsDifferentCompareTo() {
        TileTypeChange c1 = new TileTypeChange(spec());
        c1.setId("ttc:1");
        TileTypeChange c2 = new TileTypeChange(spec());
        c2.setId("ttc:2");
        assertFalse(c1.equals(c2));
    }

    // M13-TP3: ENTRY -> N0 -(B1:false)-> N1 -(B2:false)-> N2 -(B3:true)-> N4 -> N5 -> EXIT (compareTo == 0, super.equals false)
    public void testM13_TP3_EqualsZeroCompareToDifferentIdString() {
        TileTypeChange c1 = new TileTypeChange(spec());
        c1.setId("ttc:1");
        TileTypeChange c2 = new TileTypeChange(spec());
        c2.setId("ttc:01");
        assertEquals(0, c1.compareTo(c2));
        assertFalse(c1.equals(c2));
    }

    // M13-TP4: ENTRY -> N0 -(B1:false)-> N1 -(B2:false)-> N2 -(B3:false)-> N3 -> N5 -> EXIT (compareTo == 0, super.equals true)
    public void testM13_TP4_EqualsEqualIds() {
        TileTypeChange c1 = new TileTypeChange(spec());
        c1.setId("ttc:1");
        TileTypeChange c2 = new TileTypeChange(spec());
        c2.setId("ttc:1");
        assertTrue(c1.equals(c2));
    }

    // ------------------------------------------------------------------
    // M14: hashCode() — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M14-TP1: ENTRY -> N0 -> EXIT (computes hash from super.hashCode(), from, to, production)
    public void testM14_TP1_HashCode() throws Exception {
        TileTypeChange c1 = createSampleChange(true);
        TileTypeChange c2 = createSampleChange(true);
        assertEquals(c1.hashCode(), c2.hashCode());
    }
}
