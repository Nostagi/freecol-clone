
package net.sf.freecol.common.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.io.StringReader;
import java.io.StringWriter;

import net.sf.freecol.common.io.FreeColXMLReader;
import net.sf.freecol.common.io.FreeColXMLWriter;

import net.sf.freecol.util.test.FreeColTestCase;


/**
 * CFT tests for ProductionType.
 */
public class ProductionTypeCFTTest extends FreeColTestCase {

    private GoodsType food;
    private GoodsType tools;
    private GoodsType ore;

    private ProductionType empty;
    private ProductionType production;

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        food = new GoodsType("test.food", null);
        tools = new GoodsType("test.tools", null);
        ore = new GoodsType("test.ore", null);

        empty = new ProductionType((Specification) null);

        production = new ProductionType(
            Arrays.asList(
                new AbstractGoods(food, 5),
                new AbstractGoods(ore, 3)
            ),
            Arrays.asList(
                new AbstractGoods(tools, 10),
                new AbstractGoods(food, 20)
            )
        );
    }


    private AbstractGoods goods(GoodsType type, int amount) {
        return new AbstractGoods(type, amount);
    }

    private ProductionType outputType(GoodsType type, int amount) {
        return new ProductionType(
            goods(type, amount), false, null
        );
    }


    // ------------------------------------------------------------
    // Constructors
    // ------------------------------------------------------------

    public void testT01_constructorWithSpecification() {
        ProductionType pt = new ProductionType((Specification) null);

        assertNotNull(pt);
        assertFalse(pt.getUnattended());
        assertNull(pt.getProductionLevel());
        assertTrue(pt.getInputList().isEmpty());
        assertTrue(pt.getOutputList().isEmpty());
    }

    public void testT02_constructorWithInputAndOutputLists() {
        List<AbstractGoods> inputs = new ArrayList<>();
        List<AbstractGoods> outputs = new ArrayList<>();

        inputs.add(goods(food, 5));
        outputs.add(goods(tools, 10));

        ProductionType pt = new ProductionType(inputs, outputs);

        assertEquals(1, pt.getInputList().size());
        assertEquals(1, pt.getOutputList().size());
        assertEquals(food, pt.getInputList().get(0).getType());
        assertEquals(tools, pt.getOutputList().get(0).getType());
    }

    public void testT03_constructorWithSingleOutput() {
        ProductionType pt = new ProductionType(
            goods(food, 10), true, "model.difficulty.easy"
        );

        assertEquals(1, pt.getOutputList().size());
        assertEquals(food, pt.getOutputList().get(0).getType());
        assertTrue(pt.getUnattended());
        assertEquals("model.difficulty.easy", pt.getProductionLevel());
    }

    public void testT04_constructorWithNullInput() {
        ProductionType pt = new ProductionType(
            (GoodsType) null, tools, 10
        );

        assertTrue(pt.getInputList().isEmpty());
        assertEquals(1, pt.getOutputList().size());
    }

    public void testT05_constructorWithNullOutput() {
        ProductionType pt = new ProductionType(
            food, (GoodsType) null, 10
        );

        assertEquals(1, pt.getInputList().size());
        assertTrue(pt.getOutputList().isEmpty());
    }

    public void testT06_constructorWithNullInputAndOutput() {
        ProductionType pt = new ProductionType(
            (GoodsType) null, (GoodsType) null, 10
        );

        assertTrue(pt.getInputList().isEmpty());
        assertTrue(pt.getOutputList().isEmpty());
    }


    // ------------------------------------------------------------
    // Unattended and production level
    // ------------------------------------------------------------

    public void testT07_getAndSetUnattended() {
        ProductionType pt = new ProductionType((Specification) null);

        assertFalse(pt.getUnattended());

        pt.setUnattended(true);
        assertTrue(pt.getUnattended());

        pt.setUnattended(false);
        assertFalse(pt.getUnattended());
    }

    public void testT08_getProductionLevel() {
        ProductionType pt = new ProductionType(
            goods(food, 5), false, "easy"
        );

        assertEquals("easy", pt.getProductionLevel());
    }

    public void testT09_appliesToNullLevel() {
        assertTrue(production.appliesTo(null));
    }

    public void testT10_appliesToNullProductionLevel() {
        ProductionType pt = new ProductionType(
            goods(food, 5), false, null
        );

        assertTrue(pt.appliesTo("easy"));
    }

    public void testT11_appliesToMatchingLevel() {
        ProductionType pt = new ProductionType(
            goods(food, 5), false, "easy"
        );

        assertTrue(pt.appliesTo("easy"));
    }

    public void testT12_appliesToDifferentLevel() {
        ProductionType pt = new ProductionType(
            goods(food, 5), false, "easy"
        );

        assertFalse(pt.appliesTo("hard"));
    }

    public void testT13_appliesExactlyMatchingLevel() {
        ProductionType pt = new ProductionType(
            goods(food, 5), false, "easy"
        );

        assertTrue(pt.appliesExactly("easy"));
    }

    public void testT14_appliesExactlyNullLevel() {
        assertFalse(production.appliesExactly(null));
    }

    public void testT15_appliesExactlyDifferentLevel() {
        ProductionType pt = new ProductionType(
            goods(food, 5), false, "easy"
        );

        assertFalse(pt.appliesExactly("hard"));
    }


    // ------------------------------------------------------------
    // Input goods
    // ------------------------------------------------------------

    public void testT16_getInputListWhenNull() {
        assertTrue(empty.getInputList().isEmpty());
    }

    public void testT17_getInputListWhenNotNull() {
        assertEquals(2, production.getInputList().size());
    }

    public void testT18_getInputsWhenNull() {
        assertEquals(0, empty.getInputs().count());
    }

    public void testT19_getInputsWhenNotNull() {
        assertEquals(2, production.getInputs().count());
    }

    public void testT20_setInputs() {
        List<AbstractGoods> inputs = new ArrayList<>();
        inputs.add(goods(ore, 7));

        empty.setInputs(inputs);

        assertEquals(1, empty.getInputList().size());
        assertEquals(ore, empty.getInputList().get(0).getType());
    }

    public void testT21_setInputsNull() {
        production.setInputs(null);

        assertTrue(production.getInputList().isEmpty());
    }


    // ------------------------------------------------------------
    // Output goods
    // ------------------------------------------------------------

    public void testT22_getOutputListWhenNull() {
        assertTrue(empty.getOutputList().isEmpty());
    }

    public void testT23_getOutputListWhenNotNull() {
        assertEquals(2, production.getOutputList().size());
    }

    public void testT24_getOutputsWhenNull() {
        assertEquals(0, empty.getOutputs().count());
    }

    public void testT25_getOutputsWhenNotNull() {
        assertEquals(2, production.getOutputs().count());
    }

    public void testT26_setOutputs() {
        List<AbstractGoods> outputs = new ArrayList<>();
        outputs.add(goods(tools, 15));

        empty.setOutputs(outputs);

        assertEquals(1, empty.getOutputList().size());
        assertEquals(tools, empty.getOutputList().get(0).getType());
    }

    public void testT27_setOutputsNull() {
        production.setOutputs(null);

        assertTrue(production.getOutputList().isEmpty());
    }

    public void testT28_addOutput() {
        empty.addOutput(goods(food, 10));

        assertEquals(1, empty.getOutputList().size());
        assertEquals(food, empty.getOutputList().get(0).getType());
    }

    
public void testT29_addOutputToExistingList() {
    ProductionType pt = new ProductionType(
        new ArrayList<AbstractGoods>(),
        new ArrayList<AbstractGoods>()
    );

    pt.addOutput(goods(food, 10));
    pt.addOutput(goods(ore, 8));

    assertEquals(2, pt.getOutputList().size());
}


    public void testT30_getOutputFound() {
        AbstractGoods result = production.getOutput(tools);

        assertNotNull(result);
        assertEquals(tools, result.getType());
        assertEquals(10, result.getAmount());
    }

    public void testT31_getOutputNotFound() {
        assertNull(production.getOutput(ore));
    }

    public void testT32_getOutputWhenNull() {
        assertNull(empty.getOutput(food));
    }

    public void testT33_getBestOutputType() {
        assertEquals(food, production.getBestOutputType());
    }

    public void testT34_getBestOutputTypeWhenNull() {
        assertNull(empty.getBestOutputType());
    }

    public void testT35_getBestOutputTypeWhenEmpty() {
        ProductionType pt = new ProductionType(
            Collections.<AbstractGoods>emptyList(),
            Collections.<AbstractGoods>emptyList()
        );

        assertNull(pt.getBestOutputType());
    }


    // ------------------------------------------------------------
    // Static production methods
    // ------------------------------------------------------------

    public void testT36_canProducePositiveAmount() {
        ProductionType pt = outputType(food, 10);

        assertTrue(ProductionType.canProduce(
            food, Collections.singletonList(pt)
        ));
    }

    public void testT37_canProduceWrongGoodsType() {
        ProductionType pt = outputType(food, 10);

        assertFalse(ProductionType.canProduce(
            tools, Collections.singletonList(pt)
        ));
    }

    public void testT38_canProduceZeroAmount() {
        ProductionType pt = outputType(food, 0);

        assertFalse(ProductionType.canProduce(
            food, Collections.singletonList(pt)
        ));
    }

    public void testT39_canProduceNegativeAmount() {
        ProductionType pt = outputType(food, -5);

        assertFalse(ProductionType.canProduce(
            food, Collections.singletonList(pt)
        ));
    }

    public void testT40_canProduceEmptyCollection() {
        assertFalse(ProductionType.canProduce(
            food, Collections.<ProductionType>emptyList()
        ));
    }

    public void testT41_getBestProductionType() {
        ProductionType low = outputType(food, 5);
        ProductionType high = outputType(food, 20);

        ProductionType result = ProductionType.getBestProductionType(
            food, Arrays.asList(low, high)
        );

        assertSame(high, result);
    }

    public void testT42_getBestProductionTypeWithNullGoodsType() {
        ProductionType low = outputType(food, 5);
        ProductionType high = outputType(tools, 20);

        ProductionType result = ProductionType.getBestProductionType(
            null, Arrays.asList(low, high)
        );

        assertSame(high, result);
    }

    public void testT43_getBestProductionTypeEmptyCollection() {
        assertNull(ProductionType.getBestProductionType(
            food, Collections.<ProductionType>emptyList()
        ));
    }


    // ------------------------------------------------------------
    // copyIn
    // ------------------------------------------------------------

    public void testT44_copyInNull() {
        assertFalse(empty.copyIn(null));
    }


    // ------------------------------------------------------------
    // XML tag
    // ------------------------------------------------------------

    public void testT45_getXMLTagName() {
        assertEquals("production", empty.getXMLTagName());
    }


    // ------------------------------------------------------------
    // equals
    // ------------------------------------------------------------

    public void testT46_equalsSameObject() {
        assertTrue(production.equals(production));
    }

    public void testT47_equalsNull() {
        assertFalse(production.equals(null));
    }

    public void testT48_equalsDifferentClass() {
        assertFalse(production.equals(food));
    }


    // ------------------------------------------------------------
    // hashCode
    // ------------------------------------------------------------

    public void testT49_hashCodeConsistency() {
        assertEquals(production.hashCode(), production.hashCode());
    }

    public void testT50_hashCodeWithNullLists() {
        assertNotNull(Integer.valueOf(empty.hashCode()));
    }


    // ------------------------------------------------------------
    // toString
    // ------------------------------------------------------------

    public void testT51_toStringEmpty() {
        String result = empty.toString();

        assertNotNull(result);
        assertTrue(result.startsWith("["));
        assertTrue(result.endsWith("]"));
    }

    public void testT52_toStringWithProductionLevel() {
        ProductionType pt = new ProductionType(
            goods(food, 5), false, "easy"
        );

        assertTrue(pt.toString().contains("easy"));
    }

    public void testT53_toStringWithUnattended() {
        ProductionType pt = new ProductionType(
            goods(food, 5), true, null
        );

        assertTrue(pt.toString().contains("unattended"));
    }

    public void testT54_toStringWithInputsAndOutputs() {
        String result = production.toString();

        assertTrue(result.contains("inputs"));
        assertTrue(result.contains("outputs"));
    }

    public void testT55_toStringWithEmptyLists() {
        ProductionType pt = new ProductionType(
            Collections.<AbstractGoods>emptyList(),
            Collections.<AbstractGoods>emptyList()
        );

        String result = pt.toString();

        assertFalse(result.contains("inputs"));
        assertFalse(result.contains("outputs"));
    }

    // ------------------------------------------------------------
    // XML attributes and serialization
    // ------------------------------------------------------------

    public void testT56_writeAttributesEmpty() throws Exception {
        ProductionType pt = new ProductionType(spec());

        StringWriter output = new StringWriter();
        try (FreeColXMLWriter xw = new FreeColXMLWriter(output)) {
            xw.writeStartElement(ProductionType.TAG);
            pt.writeAttributes(xw);
            xw.writeEndElement();
        }

        String xml = output.toString();
        assertFalse(xml.contains("unattended="));
        assertFalse(xml.contains("production-level="));
    }

    public void testT57_writeAttributesAll() throws Exception {
        ProductionType pt = new ProductionType(
            goods(spec().getGoodsType("model.goods.grain"), 10),
            true,
            "model.difficulty.easy"
        );

        StringWriter output = new StringWriter();
        try (FreeColXMLWriter xw = new FreeColXMLWriter(output)) {
            xw.writeStartElement(ProductionType.TAG);
            pt.writeAttributes(xw);
            xw.writeEndElement();
        }

        String xml = output.toString();
        assertTrue(xml.contains("unattended=\"true\""));
        assertTrue(xml.contains("production-level="));
    }

    public void testT58_writeChildrenEmpty() throws Exception {
        ProductionType pt = new ProductionType(spec());

        StringWriter output = new StringWriter();
        try (FreeColXMLWriter xw = new FreeColXMLWriter(output)) {
            xw.writeStartElement(ProductionType.TAG);
            pt.writeChildren(xw);
            xw.writeEndElement();
        }

        String xml = output.toString();
        assertFalse(xml.contains("<input"));
        assertFalse(xml.contains("<output"));
    }

    public void testT59_writeChildrenInputsOnly() throws Exception {
        GoodsType grain = spec().getGoodsType("model.goods.grain");

        ProductionType pt = new ProductionType(spec());
        pt.setInputs(Arrays.asList(goods(grain, 5)));

        StringWriter output = new StringWriter();
        try (FreeColXMLWriter xw = new FreeColXMLWriter(output)) {
            xw.writeStartElement(ProductionType.TAG);
            pt.writeChildren(xw);
            xw.writeEndElement();
        }

        String xml = output.toString();
        assertTrue(xml.contains("<input"));
        assertFalse(xml.contains("<output"));
    }

    public void testT60_writeChildrenOutputsOnly() throws Exception {
        GoodsType grain = spec().getGoodsType("model.goods.grain");

        ProductionType pt = new ProductionType(spec());
        pt.setOutputs(Arrays.asList(goods(grain, 10)));

        StringWriter output = new StringWriter();
        try (FreeColXMLWriter xw = new FreeColXMLWriter(output)) {
            xw.writeStartElement(ProductionType.TAG);
            pt.writeChildren(xw);
            xw.writeEndElement();
        }

        String xml = output.toString();
        assertFalse(xml.contains("<input"));
        assertTrue(xml.contains("<output"));
    }

    public void testT61_writeChildrenInputsAndOutputs() throws Exception {
        GoodsType grain = spec().getGoodsType("model.goods.grain");

        ProductionType pt = new ProductionType(
            Arrays.asList(goods(grain, 5)),
            Arrays.asList(goods(grain, 10))
        );

        StringWriter output = new StringWriter();
        try (FreeColXMLWriter xw = new FreeColXMLWriter(output)) {
            xw.writeStartElement(ProductionType.TAG);
            pt.writeChildren(xw);
            xw.writeEndElement();
        }

        String xml = output.toString();
        assertTrue(xml.contains("<input"));
        assertTrue(xml.contains("<output"));
    }


    // ------------------------------------------------------------
    // XML deserialization
    // ------------------------------------------------------------

    public void testT62_readAttributesDefaults() throws Exception {
        String xml = "<production/>";

        try (FreeColXMLReader xr =
                 new FreeColXMLReader(new StringReader(xml))) {
            xr.nextTag();

            ProductionType pt = new ProductionType(spec());
            pt.readAttributes(xr);

            assertFalse(pt.getUnattended());
            assertNull(pt.getProductionLevel());
        }
    }

    public void testT63_readAttributesUnattendedAndLevel()
        throws Exception {
        String xml = "<production unattended=\"true\" "
            + "production-level=\"easy\"/>";

        try (FreeColXMLReader xr =
                 new FreeColXMLReader(new StringReader(xml))) {
            xr.nextTag();

            ProductionType pt = new ProductionType(spec());
            pt.readAttributes(xr);

            assertTrue(pt.getUnattended());
            assertEquals("easy", pt.getProductionLevel());
        }
    }

    public void testT64_readAttributesOldProductionLevel()
        throws Exception {
        String xml = "<production productionLevel=\"old-level\"/>";

        try (FreeColXMLReader xr =
                 new FreeColXMLReader(new StringReader(xml))) {
            xr.nextTag();

            ProductionType pt = new ProductionType(spec());
            pt.readAttributes(xr);

            assertEquals("old-level", pt.getProductionLevel());
        }
    }

    public void testT65_readChildInput() throws Exception {
        GoodsType grain = spec().getGoodsType("model.goods.grain");
        String xml = "<production>"
            + "<input goods-type=\"" + grain.getId()
            + "\" value=\"7\"/>"
            + "</production>";

        try (FreeColXMLReader xr =
                 new FreeColXMLReader(new StringReader(xml))) {
            xr.nextTag();
            xr.nextTag();

            ProductionType pt = new ProductionType(spec());
            pt.readChild(xr);

            assertEquals(1, pt.getInputList().size());
            assertEquals(7, pt.getInputList().get(0).getAmount());
            assertEquals(grain, pt.getInputList().get(0).getType());
        }
    }

    public void testT66_readChildOutput() throws Exception {
        GoodsType grain = spec().getGoodsType("model.goods.grain");
        String xml = "<production>"
            + "<output goods-type=\"" + grain.getId()
            + "\" value=\"12\"/>"
            + "</production>";

        try (FreeColXMLReader xr =
                 new FreeColXMLReader(new StringReader(xml))) {
            xr.nextTag();
            xr.nextTag();

            ProductionType pt = new ProductionType(spec());
            pt.readChild(xr);

            assertEquals(1, pt.getOutputList().size());
            assertEquals(12, pt.getOutputList().get(0).getAmount());
            assertEquals(grain, pt.getOutputList().get(0).getType());
        }
    }

    
public void testT67_readChildInputNullType() throws Exception {
    String xml = "<production>"
        + "<input value=\"5\"/>"
        + "</production>";

    try (FreeColXMLReader xr =
             new FreeColXMLReader(new StringReader(xml))) {
        xr.nextTag();
        xr.nextTag();

        ProductionType pt = new ProductionType(spec());
        pt.readChild(xr);

        assertTrue(pt.getInputList().isEmpty());
    }
}


public void testT68_readChildOutputNullType() throws Exception {
    String xml = "<production>"
        + "<output value=\"5\"/>"
        + "</production>";

    try (FreeColXMLReader xr =
             new FreeColXMLReader(new StringReader(xml))) {
        xr.nextTag();
        xr.nextTag();

        ProductionType pt = new ProductionType(spec());
        pt.readChild(xr);

        assertTrue(pt.getOutputList().isEmpty());
    }
}


    public void testT69_xmlConstructor() throws Exception {
        GoodsType grain = spec().getGoodsType("model.goods.grain");
        String xml = "<production unattended=\"true\" "
            + "production-level=\"easy\">"
            + "<input goods-type=\"" + grain.getId()
            + "\" value=\"3\"/>"
            + "<output goods-type=\"" + grain.getId()
            + "\" value=\"8\"/>"
            + "</production>";

        try (FreeColXMLReader xr =
                 new FreeColXMLReader(new StringReader(xml))) {
            xr.nextTag();

            ProductionType pt = new ProductionType(xr, spec());

            assertTrue(pt.getUnattended());
            assertEquals("easy", pt.getProductionLevel());
            assertEquals(1, pt.getInputList().size());
            assertEquals(1, pt.getOutputList().size());
        }
    }

    public void testT70_readChildrenClearsExistingLists()
        throws Exception {
        GoodsType grain = spec().getGoodsType("model.goods.grain");

        ProductionType pt = new ProductionType(spec());
        pt.setInputs(new ArrayList<>(
            Arrays.asList(goods(grain, 100))
        ));
        pt.setOutputs(new ArrayList<>(
            Arrays.asList(goods(grain, 200))
        ));

        String xml = "<production>"
            + "<input goods-type=\"" + grain.getId()
            + "\" value=\"5\"/>"
            + "<output goods-type=\"" + grain.getId()
            + "\" value=\"10\"/>"
            + "</production>";

        try (FreeColXMLReader xr =
                 new FreeColXMLReader(new StringReader(xml))) {
            xr.nextTag();
            pt.readChildren(xr);

            assertEquals(1, pt.getInputList().size());
            assertEquals(5, pt.getInputList().get(0).getAmount());
            assertEquals(1, pt.getOutputList().size());
            assertEquals(10, pt.getOutputList().get(0).getAmount());
        }
    }


    // ------------------------------------------------------------
    // Private addInput and addOutput through XML
    // ------------------------------------------------------------

    public void testT71_addInputInitializesList() throws Exception {
        testT65_readChildInput();
    }

    public void testT72_addOutputInitializesList() throws Exception {
        testT66_readChildOutput();
    }

    // ------------------------------------------------------------
    // copyIn
    // ------------------------------------------------------------

    public void testT73_copyInWrongType() {
        assertFalse(empty.copyIn(food));
    }

  
public void testT74_copyInValidProductionType() {
    Specification specification = spec();
    GoodsType grain = specification.getGoodsType("model.goods.grain");

    ProductionType source = new ProductionType(specification);
    source.setId("test.production");
    source.setUnattended(true);
    source.setInputs(new ArrayList<>(
        Arrays.asList(goods(grain, 5))
    ));
    source.setOutputs(new ArrayList<>(
        Arrays.asList(goods(grain, 15))
    ));

    ProductionType target = new ProductionType(specification);
    target.setId("test.production");

    boolean result = target.copyIn(source);

    assertTrue(result);
    assertTrue(target.getUnattended());
    assertEquals(1, target.getInputList().size());
    assertEquals(1, target.getOutputList().size());
    assertEquals(5, target.getInputList().get(0).getAmount());
    assertEquals(15, target.getOutputList().get(0).getAmount());
}



    // ------------------------------------------------------------
    // equals - individual field differences
    // ------------------------------------------------------------

    public void testT75_equalsEqualObjects() {
        GoodsType grain = spec().getGoodsType("model.goods.grain");

        ProductionType first = new ProductionType(
            goods(grain, 10), false, "easy"
        );
        ProductionType second = new ProductionType(
            goods(grain, 10), false, "easy"
        );

        assertTrue(first.equals(second));
    }

    public void testT76_equalsDifferentUnattended() {
        GoodsType grain = spec().getGoodsType("model.goods.grain");

        ProductionType first = new ProductionType(
            goods(grain, 10), false, "easy"
        );
        ProductionType second = new ProductionType(
            goods(grain, 10), true, "easy"
        );

        assertFalse(first.equals(second));
    }

    public void testT77_equalsDifferentProductionLevel() {
        GoodsType grain = spec().getGoodsType("model.goods.grain");

        ProductionType first = new ProductionType(
            goods(grain, 10), false, "easy"
        );
        ProductionType second = new ProductionType(
            goods(grain, 10), false, "hard"
        );

        assertFalse(first.equals(second));
    }

    public void testT78_equalsDifferentOutputs() {
        GoodsType grain = spec().getGoodsType("model.goods.grain");

        ProductionType first = new ProductionType(
            goods(grain, 10), false, null
        );
        ProductionType second = new ProductionType(
            goods(grain, 20), false, null
        );

        assertFalse(first.equals(second));
    }

    public void testT79_equalsDifferentInputs() {
        GoodsType grain = spec().getGoodsType("model.goods.grain");

        ProductionType first = new ProductionType(spec());
        first.setInputs(Arrays.asList(goods(grain, 5)));

        ProductionType second = new ProductionType(spec());
        second.setInputs(Arrays.asList(goods(grain, 10)));

        assertFalse(first.equals(second));
    }

    public void testT80_equalsNullLists() {
        ProductionType first = new ProductionType(spec());
        ProductionType second = new ProductionType(spec());

        assertTrue(first.equals(second));
    }


    // ------------------------------------------------------------
    // Best production - additional lambda branches
    // ------------------------------------------------------------

   
public void testT81_bestProductionWithNoMatchingOutput() {
    ProductionType pt = outputType(food, 10);

    ProductionType result = ProductionType.getBestProductionType(
        tools, Collections.singletonList(pt)
    );

    assertSame(pt, result);
}

    public void testT82_bestProductionWithEmptyOutputs() {
        ProductionType pt = new ProductionType(spec());

        assertSame(pt, ProductionType.getBestProductionType(
            food, Collections.singletonList(pt)
        ));
    }

    
public void testT83_bestOutputForDifferentGoods() {
    ProductionType pt = new ProductionType(
        Arrays.asList(
            goods(food, 5),
            goods(tools, 20)
        ),
        new ArrayList<AbstractGoods>()
    );

    assertNull(pt.getBestOutputType());

    assertSame(pt, ProductionType.getBestProductionType(
        ore, Collections.singletonList(pt)
    ));
}



    // ------------------------------------------------------------
    // hashCode - input/output combinations
    // ------------------------------------------------------------

    public void testT84_hashCodeWithInputsOnly() {
        GoodsType grain = spec().getGoodsType("model.goods.grain");

        ProductionType pt = new ProductionType(spec());
        pt.setInputs(Arrays.asList(goods(grain, 5)));

        assertEquals(pt.hashCode(), pt.hashCode());
    }

    public void testT85_hashCodeWithOutputsOnly() {
        GoodsType grain = spec().getGoodsType("model.goods.grain");

        ProductionType pt = new ProductionType(spec());
        pt.setOutputs(Arrays.asList(goods(grain, 5)));

        assertEquals(pt.hashCode(), pt.hashCode());
    }

    public void testT86_hashCodeWithBothLists() {
        GoodsType grain = spec().getGoodsType("model.goods.grain");

        ProductionType pt = new ProductionType(
            Arrays.asList(goods(grain, 5)),
            Arrays.asList(goods(grain, 10))
        );

        assertEquals(pt.hashCode(), pt.hashCode());
    }

}
