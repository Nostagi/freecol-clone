package net.sf.freecol.common.model;

import java.io.StringReader;
import java.io.StringWriter;

import net.sf.freecol.common.io.FreeColXMLReader;
import net.sf.freecol.common.io.FreeColXMLWriter;
import net.sf.freecol.util.test.FreeColTestCase;

/**
 * CFG-driven test suite for {@link LastSale} derived from
 * {@code test_cft/graph/net/sf/freecol/common/model/LastSale.json}.
 *
 * Covers all 12 methods (M1..M12) and achieves 100% feasible C3 (node + edge)
 * coverage across 13 Test Paths.
 *
 * Infeasible edges documented:
 * - M8 ({@code copyIn}): {@code N1 -> N2} ({@code B2:false}) — {@code super.copyIn(o)}
 *   delegates to {@link FreeColObject#copyIn(FreeColObject)}, which only returns
 *   {@code false} when {@code o} is {@code null} or not a {@code FreeColObject};
 *   once {@code o != null} at {@code N1}, {@code super.copyIn(o)} always returns {@code true}.
 */
public class LastSaleCfgTest extends FreeColTestCase {

    private Game game;
    private Player dutch;
    private GoodsType foodType;
    private Location saleLocation;
    private Turn turn5;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        // Pre-condition: standard game with Dutch player, food goods type, Europe location, and Turn 5
        game = getStandardGame();
        dutch = game.getPlayerByNationId("model.nation.dutch");
        foodType = spec().getGoodsType("model.goods.food");
        saleLocation = dutch.getEurope();
        turn5 = new Turn(5);
    }

    // ------------------------------------------------------------------
    // Pre-condition helpers & stubs
    // ------------------------------------------------------------------

    private String writeLastSaleXml(LastSale sale) throws Exception {
        StringWriter sw = new StringWriter();
        try (FreeColXMLWriter xw = new FreeColXMLWriter(sw,
                FreeColXMLWriter.WriteScope.toSave(), false)) {
            sale.toXML(xw);
            xw.flush();
        }
        return sw.toString();
    }

    private FreeColXMLReader openReaderAtTag(String xml) throws Exception {
        FreeColXMLReader xr = new FreeColXMLReader(new StringReader(xml));
        xr.nextTag();
        return xr;
    }

    // ------------------------------------------------------------------
    // M1: <init>() — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M1-TP1: ENTRY -> N0 -> EXIT (default constructor)
    public void testM1_TP1_DefaultConstructor() {
        LastSale sale = new LastSale();
        assertNull(sale.getWhen());
        assertEquals(0, sale.getPrice());
    }

    // ------------------------------------------------------------------
    // M2: <init>(Location, GoodsType, Turn, int) — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M2-TP1: ENTRY -> N0 -> EXIT (constructor with Location, GoodsType, Turn, price)
    public void testM2_TP1_LocationGoodsConstructor() {
        LastSale sale = new LastSale(saleLocation, foodType, turn5, 12);
        assertEquals(LastSale.makeKey(saleLocation, foodType), sale.getId());
        assertSame(turn5, sale.getWhen());
        assertEquals(12, sale.getPrice());
    }

    // ------------------------------------------------------------------
    // M3: <init>(String, Turn, int) — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M3-TP1: ENTRY -> N0 -> EXIT (constructor with id, Turn, price)
    public void testM3_TP1_IdConstructor() {
        LastSale sale = new LastSale("custom-key", turn5, 25);
        assertEquals("custom-key", sale.getId());
        assertSame(turn5, sale.getWhen());
        assertEquals(25, sale.getPrice());
    }

    // ------------------------------------------------------------------
    // M4: <init>(FreeColXMLReader) — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M4-TP1: ENTRY -> N0 -> EXIT (XML reader constructor)
    public void testM4_TP1_XmlReaderConstructor() throws Exception {
        String xml = "<lastSale id=\"model.goods.food-europe\" when=\"7\" price=\"19\"/>";
        try (FreeColXMLReader xr = openReaderAtTag(xml)) {
            LastSale sale = new LastSale(xr);
            assertEquals("model.goods.food-europe", sale.getId());
            assertEquals(7, sale.getWhen().getNumber());
            assertEquals(19, sale.getPrice());
        }
    }

    // ------------------------------------------------------------------
    // M5: getWhen() — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M5-TP1: ENTRY -> N0 -> EXIT (returns Turn when sale occurred)
    public void testM5_TP1_GetWhen() {
        LastSale sale = new LastSale("k1", turn5, 10);
        assertSame(turn5, sale.getWhen());
    }

    // ------------------------------------------------------------------
    // M6: getPrice() — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M6-TP1: ENTRY -> N0 -> EXIT (returns sale price)
    public void testM6_TP1_GetPrice() {
        LastSale sale = new LastSale("k2", turn5, 42);
        assertEquals(42, sale.getPrice());
    }

    // ------------------------------------------------------------------
    // M7: makeKey(Location, GoodsType) — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M7-TP1: ENTRY -> N0 -> EXIT (formats goodsType.id + "-" + location.id)
    public void testM7_TP1_MakeKey() {
        String key = LastSale.makeKey(saleLocation, foodType);
        assertEquals(foodType.getId() + "-" + saleLocation.getId(), key);
    }

    // ------------------------------------------------------------------
    // M8: copyIn(FreeColObject) — 2 Test Paths (100% feasible C3)
    // ------------------------------------------------------------------

    // M8-TP1: ENTRY -> N0 -(B1:true)-> N2 -> EXIT (o == null because other is null)
    public void testM8_TP1_CopyInNullOrWrongType() {
        LastSale target = new LastSale("t1", turn5, 10);
        assertFalse(target.copyIn((LastSale) null));
        assertFalse(target.copyIn(dutch));
        assertEquals(10, target.getPrice());
    }

    // M8-TP2: ENTRY -> N0 -(B1:false)-> N1 -(B2:true)-> N3 -> EXIT (valid LastSale copied)
    public void testM8_TP2_CopyInSuccess() {
        LastSale target = new LastSale("t2", new Turn(1), 5);
        Turn turn9 = new Turn(9);
        LastSale source = new LastSale("t2", turn9, 99);
        assertTrue(target.copyIn(source));
        assertSame(turn9, target.getWhen());
        assertEquals(99, target.getPrice());
    }

    // ------------------------------------------------------------------
    // M9: writeAttributes(FreeColXMLWriter) — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M9-TP1: ENTRY -> N0 -> EXIT (writes id, when, price attributes to XML)
    public void testM9_TP1_WriteAttributes() throws Exception {
        LastSale sale = new LastSale("sale-key", turn5, 33);
        String xml = writeLastSaleXml(sale);
        assertTrue(xml.contains("id=\"sale-key\""));
        assertTrue(xml.contains("when=\"5\""));
        assertTrue(xml.contains("price=\"33\""));
    }

    // ------------------------------------------------------------------
    // M10: readAttributes(FreeColXMLReader) — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M10-TP1: ENTRY -> N0 -> EXIT (reads id, when, price attributes from XML)
    public void testM10_TP1_ReadAttributes() throws Exception {
        String xml = "<lastSale id=\"read-key\" when=\"11\" price=\"77\"/>";
        LastSale sale = new LastSale();
        try (FreeColXMLReader xr = openReaderAtTag(xml)) {
            sale.readAttributes(xr);
        }
        assertEquals("read-key", sale.getId());
        assertEquals(11, sale.getWhen().getNumber());
        assertEquals(77, sale.getPrice());
    }

    // ------------------------------------------------------------------
    // M11: getXMLTagName() — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M11-TP1: ENTRY -> N0 -> EXIT (returns "lastSale")
    public void testM11_TP1_GetXMLTagName() {
        LastSale sale = new LastSale();
        assertEquals(LastSale.TAG, sale.getXMLTagName());
        assertEquals("lastSale", sale.getXMLTagName());
    }

    // ------------------------------------------------------------------
    // M12: toString() — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M12-TP1: ENTRY -> N0 -> EXIT (formats [id when=... price=...])
    public void testM12_TP1_ToString() {
        LastSale sale = new LastSale("str-key", turn5, 15);
        String str = sale.toString();
        assertTrue(str.contains("str-key"));
        assertTrue(str.contains("when="));
        assertTrue(str.contains("price=15"));
    }
}
