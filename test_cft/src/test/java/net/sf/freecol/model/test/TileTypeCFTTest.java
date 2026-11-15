
package net.sf.freecol.common.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.io.StringReader;
import java.io.StringWriter;
import net.sf.freecol.common.util.RandomChoice;

import net.sf.freecol.common.io.FreeColXMLReader;
import net.sf.freecol.common.io.FreeColXMLWriter;

import net.sf.freecol.util.test.FreeColTestCase;

public class TileTypeCFTTest extends FreeColTestCase {

    private TileType createTileType() {
        return new TileType("test.tile", spec());
    }

    private String writeTileTypeXML(TileType tileType) throws Exception {
    StringWriter sw = new StringWriter();

    try (FreeColXMLWriter xw = new FreeColXMLWriter(sw)) {
        xw.writeStartDocument("UTF-8", "1.0");
        tileType.toXML(xw);
        xw.writeEndDocument();
    }

    return sw.toString();
}

private TileType readTileTypeXML(String xml) throws Exception {
    TileType result = createTileType();

    try (FreeColXMLReader xr = new FreeColXMLReader(
            new StringReader(xml))) {
        xr.nextTag();
        result.readFromXML(xr);
    }

    return result;
}

    private void initializeFeatureContainer(TileType tile) {
        tile.addAbility(new Ability("test.ability", true));
    }

    // Constructor and basic properties

    public void testT01_constructor() {
        TileType tile = createTileType();

        assertNotNull(tile);
        assertEquals("test.tile", tile.getId());
    }

    public void testT02_waterConstant() {
        assertNotNull(TileType.WATER);
        assertTrue(TileType.WATER.isWater());
    }

    public void testT03_landConstant() {
        assertNotNull(TileType.LAND);
        assertFalse(TileType.LAND.isWater());
    }

    public void testT04_defaultProperties() {
        TileType tile = createTileType();

        assertFalse(tile.isForested());
        assertFalse(tile.isWater());
        assertFalse(tile.canSettle());
        assertFalse(tile.isHighSeasConnected());
        assertFalse(tile.isElevation());
    }

    public void testT05_defaultMovementCost() {
        assertEquals(0, createTileType().getBasicMoveCost());
    }

    public void testT06_defaultWorkTurns() {
        assertEquals(0, createTileType().getBasicWorkTurns());
    }

    public void testT07_directlyHighSeasConnectedByDefault() {
        assertFalse(createTileType().isDirectlyHighSeasConnected());
    }

    public void testT08_xmlTagName() {
        assertEquals(TileType.TAG, createTileType().getXMLTagName());
    }

    // Humidity, temperature and altitude

    public void testT09_defaultHumidity() {
        TileType tile = createTileType();

        assertEquals(0, tile.getHumidity(0));
        assertEquals(0, tile.getHumidity(1));
    }

    public void testT10_setHumidity() {
        TileType tile = createTileType();

        tile.setHumidity(0, 20);
        tile.setHumidity(1, 80);

        assertEquals(20, tile.getHumidity(0));
        assertEquals(80, tile.getHumidity(1));
    }

    public void testT11_defaultTemperature() {
        TileType tile = createTileType();

        assertEquals(0, tile.getTemperature(0));
        assertEquals(0, tile.getTemperature(1));
    }

    public void testT12_setTemperature() {
        TileType tile = createTileType();

        tile.setTemperature(0, -10);
        tile.setTemperature(1, 35);

        assertEquals(-10, tile.getTemperature(0));
        assertEquals(35, tile.getTemperature(1));
    }

    public void testT13_defaultAltitude() {
        TileType tile = createTileType();

        assertEquals(0, tile.getAltitude(0));
        assertEquals(0, tile.getAltitude(1));
    }

    public void testT14_setAltitude() {
        TileType tile = createTileType();

        tile.setAltitude(0, 5);
        tile.setAltitude(1, 30);

        assertEquals(5, tile.getAltitude(0));
        assertEquals(30, tile.getAltitude(1));
    }

    public void testT15_withinHumidityRange() {
        TileType tile = createTileType();
        tile.setHumidity(0, 20);
        tile.setHumidity(1, 80);

        assertTrue(tile.withinRange(TileType.RangeType.HUMIDITY, 20));
        assertTrue(tile.withinRange(TileType.RangeType.HUMIDITY, 50));
        assertTrue(tile.withinRange(TileType.RangeType.HUMIDITY, 80));
    }

    public void testT16_outsideHumidityRange() {
        TileType tile = createTileType();
        tile.setHumidity(0, 20);
        tile.setHumidity(1, 80);

        assertFalse(tile.withinRange(TileType.RangeType.HUMIDITY, 19));
        assertFalse(tile.withinRange(TileType.RangeType.HUMIDITY, 81));
    }

    public void testT17_withinTemperatureRange() {
        TileType tile = createTileType();
        tile.setTemperature(0, -10);
        tile.setTemperature(1, 30);

        assertTrue(tile.withinRange(TileType.RangeType.TEMPERATURE, -10));
        assertTrue(tile.withinRange(TileType.RangeType.TEMPERATURE, 10));
        assertTrue(tile.withinRange(TileType.RangeType.TEMPERATURE, 30));
    }

    public void testT18_outsideTemperatureRange() {
        TileType tile = createTileType();
        tile.setTemperature(0, -10);
        tile.setTemperature(1, 30);

        assertFalse(tile.withinRange(TileType.RangeType.TEMPERATURE, -11));
        assertFalse(tile.withinRange(TileType.RangeType.TEMPERATURE, 31));
    }

    public void testT19_withinAltitudeRange() {
        TileType tile = createTileType();
        tile.setAltitude(0, 5);
        tile.setAltitude(1, 40);

        assertTrue(tile.withinRange(TileType.RangeType.ALTITUDE, 5));
        assertTrue(tile.withinRange(TileType.RangeType.ALTITUDE, 25));
        assertTrue(tile.withinRange(TileType.RangeType.ALTITUDE, 40));
    }

    public void testT20_outsideAltitudeRange() {
        TileType tile = createTileType();
        tile.setAltitude(0, 5);
        tile.setAltitude(1, 40);

        assertFalse(tile.withinRange(TileType.RangeType.ALTITUDE, 4));
        assertFalse(tile.withinRange(TileType.RangeType.ALTITUDE, 41));
    }

    public void testT21_isHillsAtBoundary() {
        TileType tile = createTileType();
        tile.setAltitude(0, TileType.HILLS_ALTITUDE);
        tile.setAltitude(1, TileType.HILLS_ALTITUDE);

        assertTrue(tile.isHills());
    }

    public void testT22_isHillsRangeIncludesBoundary() {
        TileType tile = createTileType();
        tile.setAltitude(0, 10);
        tile.setAltitude(1, 20);

        assertTrue(tile.isHills());
    }

    public void testT23_isNotHillsBelowRange() {
        TileType tile = createTileType();
        tile.setAltitude(0, 0);
        tile.setAltitude(1, 14);

        assertFalse(tile.isHills());
    }

    public void testT24_isNotHillsAboveRange() {
        TileType tile = createTileType();
        tile.setAltitude(0, 16);
        tile.setAltitude(1, 30);

        assertFalse(tile.isHills());
    }

    public void testT25_isMountainsAtBoundary() {
        TileType tile = createTileType();
        tile.setAltitude(0, TileType.MOUNTAINS_ALTITUDE);
        tile.setAltitude(1, TileType.MOUNTAINS_ALTITUDE);

        assertTrue(tile.isMountains());
    }

    public void testT26_isMountainsRangeIncludesBoundary() {
        TileType tile = createTileType();
        tile.setAltitude(0, 20);
        tile.setAltitude(1, 30);

        assertTrue(tile.isMountains());
    }

    public void testT27_isNotMountainsBelowRange() {
        TileType tile = createTileType();
        tile.setAltitude(0, 0);
        tile.setAltitude(1, 24);

        assertFalse(tile.isMountains());
    }

    public void testT28_isNotMountainsAboveRange() {
        TileType tile = createTileType();
        tile.setAltitude(0, 26);
        tile.setAltitude(1, 40);

        assertFalse(tile.isMountains());
    }

    // Resource types

    public void testT29_getResourceTypesInitializesList() {
        TileType tile = createTileType();

        assertNotNull(tile.getResourceTypes());
        assertTrue(tile.getResourceTypes().isEmpty());
    }

    public void testT30_getResourceTypesReturnsSameList() {
        TileType tile = createTileType();

        List<?> first = tile.getResourceTypes();
        List<?> second = tile.getResourceTypes();

        assertSame(first, second);
    }

    public void testT31_setResourceTypesWhenNull() {
        TileType tile = createTileType();

        tile.setResourceTypes(new ArrayList<>());

        assertNotNull(tile.getResourceTypes());
        assertTrue(tile.getResourceTypes().isEmpty());
    }

    public void testT32_setResourceTypesWhenAlreadyInitialized() {
        TileType tile = createTileType();

        tile.getResourceTypes();
        tile.setResourceTypes(new ArrayList<>());

        assertNotNull(tile.getResourceTypes());
        assertTrue(tile.getResourceTypes().isEmpty());
    }

    public void testT33_getResourceTypeValuesWhenEmpty() {
        TileType tile = createTileType();

        assertTrue(tile.getResourceTypeValues().isEmpty());
    }

    public void testT34_canHaveResourceTypeWhenEmpty() {
        TileType tile = createTileType();

        assertFalse(tile.canHaveResourceType(null));
    }

    // Disaster choices

    public void testT35_getDisastersInitiallyNull() {
        TileType tile = createTileType();

        assertNull(tile.getDisasters());
    }

    public void testT36_getDisasterChoicesWhenNull() {
        TileType tile = createTileType();

        Stream<?> choices = tile.getDisasterChoices();

        assertNotNull(choices);
        assertEquals(0L, choices.count());
    }

    public void testT37_setDisastersWhenNull() {
        TileType tile = createTileType();

        tile.setDisasters(new ArrayList<>());

        assertNotNull(tile.getDisasters());
        assertTrue(tile.getDisasters().isEmpty());
    }

    public void testT38_setDisastersWhenAlreadyInitialized() {
        TileType tile = createTileType();

        tile.setDisasters(new ArrayList<>());
        tile.setDisasters(new ArrayList<>());

        assertNotNull(tile.getDisasters());
        assertTrue(tile.getDisasters().isEmpty());
    }

    public void testT39_getDisasterChoicesWhenEmpty() {
        TileType tile = createTileType();
        tile.setDisasters(Collections.emptyList());

        assertEquals(0L, tile.getDisasterChoices().count());
    }

    // Production types

    public void testT40_getProductionTypesInitiallyEmpty() {
        TileType tile = createTileType();

        assertNotNull(tile.getProductionTypes());
        assertTrue(tile.getProductionTypes().isEmpty());
    }

    public void testT41_setProductionTypesWithEmptyList() {
        TileType tile = createTileType();

        tile.setProductionTypes(new ArrayList<>());

        assertTrue(tile.getProductionTypes().isEmpty());
    }

    public void testT42_getAvailableProductionTypesUnattendedEmpty() {
        TileType tile = createTileType();

        assertTrue(tile.getAvailableProductionTypes(true, null).isEmpty());
    }

    public void testT43_getAvailableProductionTypesAttendedEmpty() {
        TileType tile = createTileType();

        assertTrue(tile.getAvailableProductionTypes(false, null).isEmpty());
    }

    public void testT44_getPossibleProductionUnattendedEmpty() {
        TileType tile = createTileType();

        assertEquals(0L, tile.getPossibleProduction(true).count());
    }

    public void testT45_getPossibleProductionAttendedEmpty() {
        TileType tile = createTileType();

        assertEquals(0L, tile.getPossibleProduction(false).count());
    }

    // Production utilities

    public void testT46_canProduceNullGoodsType() {
        TileType tile = createTileType();

        assertFalse(tile.canProduce(null, null));
    }

    public void testT47_getPotentialProductionNullGoodsType() {
        TileType tile = createTileType();

        assertEquals(0, tile.getPotentialProduction(null, null));
    }

    public void testT54_getAvailableProductionTypesFiltersUnattended() {
        GoodsType food = spec().getGoodsType("model.goods.food");
        assertNotNull(food);

        TileType tile = createTileType();

        ProductionType attended
            = new ProductionType(new AbstractGoods(food, 3), false, null);
        ProductionType unattended
            = new ProductionType(new AbstractGoods(food, 5), true, null);

        List<ProductionType> types = new ArrayList<>();
        types.add(attended);
        types.add(unattended);
        tile.setProductionTypes(types);

        List<ProductionType> result
            = tile.getAvailableProductionTypes(true, null);

        assertEquals(1, result.size());
        assertSame(unattended, result.get(0));
    }

    public void testT55_getAvailableProductionTypesFiltersAttended() {
        GoodsType food = spec().getGoodsType("model.goods.food");
        assertNotNull(food);

        TileType tile = createTileType();

        ProductionType attended
            = new ProductionType(new AbstractGoods(food, 3), false, null);
        ProductionType unattended
            = new ProductionType(new AbstractGoods(food, 5), true, null);

        List<ProductionType> types = new ArrayList<>();
        types.add(attended);
        types.add(unattended);
        tile.setProductionTypes(types);

        List<ProductionType> result
            = tile.getAvailableProductionTypes(false, null);

        assertEquals(1, result.size());
        assertSame(attended, result.get(0));
    }

    public void testT56_getAvailableProductionTypesPrefersExactLevel() {
        GoodsType food = spec().getGoodsType("model.goods.food");
        assertNotNull(food);

        TileType tile = createTileType();

        ProductionType general
            = new ProductionType(new AbstractGoods(food, 3), true, null);
        ProductionType exact
            = new ProductionType(new AbstractGoods(food, 5), true, "hard");

        List<ProductionType> types = new ArrayList<>();
        types.add(general);
        types.add(exact);
        tile.setProductionTypes(types);

        List<ProductionType> result
            = tile.getAvailableProductionTypes(true, "hard");

        assertEquals(1, result.size());
        assertSame(exact, result.get(0));
    }

    public void testT57_getAvailableProductionTypesUsesGeneralLevel() {
        GoodsType food = spec().getGoodsType("model.goods.food");
        assertNotNull(food);

        TileType tile = createTileType();

        ProductionType general
            = new ProductionType(new AbstractGoods(food, 3), true, null);
        ProductionType otherLevel
            = new ProductionType(new AbstractGoods(food, 5), true, "hard");

        List<ProductionType> types = new ArrayList<>();
        types.add(general);
        types.add(otherLevel);
        tile.setProductionTypes(types);

        List<ProductionType> result
            = tile.getAvailableProductionTypes(true, "medium");

        assertEquals(1, result.size());
        assertSame(general, result.get(0));
    }

    public void testT58_getAvailableProductionTypesNoMatchingLevel() {
        GoodsType food = spec().getGoodsType("model.goods.food");
        assertNotNull(food);

        TileType tile = createTileType();

        ProductionType production
            = new ProductionType(new AbstractGoods(food, 5), true, "hard");

        tile.setProductionTypes(Collections.singletonList(production));

        assertTrue(
            tile.getAvailableProductionTypes(true, "medium").isEmpty()
        );
    }

    public void testT59_canProducePositiveOutput() {
        GoodsType food = spec().getGoodsType("model.goods.food");
        assertNotNull(food);

        TileType tile = createTileType();

        ProductionType production
            = new ProductionType(new AbstractGoods(food, 5), true, null);

        tile.setProductionTypes(Collections.singletonList(production));

        assertTrue(tile.canProduce(food, null));
    }

    public void testT60_canProduceZeroOutput() {
        GoodsType food = spec().getGoodsType("model.goods.food");
        assertNotNull(food);

        TileType tile = createTileType();

        ProductionType production
            = new ProductionType(new AbstractGoods(food, 0), true, null);

        tile.setProductionTypes(Collections.singletonList(production));

        assertFalse(tile.canProduce(food, null));
    }

    public void testT61_canProduceDifferentGoodsType() {
        GoodsType food = spec().getGoodsType("model.goods.food");
        GoodsType lumber = spec().getGoodsType("model.goods.lumber");

        assertNotNull(food);
        assertNotNull(lumber);

        TileType tile = createTileType();

        ProductionType production
            = new ProductionType(new AbstractGoods(food, 5), true, null);

        tile.setProductionTypes(Collections.singletonList(production));

        assertFalse(tile.canProduce(lumber, null));
    }

    public void testT62_getPossibleProductionWithOutput() {
        GoodsType food = spec().getGoodsType("model.goods.food");
        assertNotNull(food);

        TileType tile = createTileType();

        ProductionType production
            = new ProductionType(new AbstractGoods(food, 5), true, null);

        tile.setProductionTypes(Collections.singletonList(production));

        List<AbstractGoods> result = tile.getPossibleProduction(true)
            .collect(Collectors.toList());

        assertEquals(1, result.size());
        assertSame(food, result.get(0).getType());
        assertEquals(5, result.get(0).getAmount());
    }

    public void testT63_getPossibleProductionFiltersUnattended() {
        GoodsType food = spec().getGoodsType("model.goods.food");
        assertNotNull(food);

        TileType tile = createTileType();

        ProductionType attended
            = new ProductionType(new AbstractGoods(food, 3), false, null);
        ProductionType unattended
            = new ProductionType(new AbstractGoods(food, 5), true, null);

        List<ProductionType> types = new ArrayList<>();
        types.add(attended);
        types.add(unattended);
        tile.setProductionTypes(types);

        List<AbstractGoods> result = tile.getPossibleProduction(true)
            .collect(Collectors.toList());

        assertEquals(1, result.size());
        assertEquals(5, result.get(0).getAmount());
    }

    // copyIn

    public void testT48_copyInNull() {
        TileType tile = createTileType();

        assertFalse(tile.copyIn(null));
    }

    public void testT49_copyInNullDoesNotChangeProperties() {
        TileType tile = createTileType();
        tile.setHumidity(0, 10);
        tile.setHumidity(1, 90);
        tile.setTemperature(0, -5);
        tile.setTemperature(1, 35);
        tile.setAltitude(0, 5);
        tile.setAltitude(1, 40);

        assertFalse(tile.copyIn(null));

        assertEquals(10, tile.getHumidity(0));
        assertEquals(90, tile.getHumidity(1));
        assertEquals(-5, tile.getTemperature(0));
        assertEquals(35, tile.getTemperature(1));
        assertEquals(5, tile.getAltitude(0));
        assertEquals(40, tile.getAltitude(1));
    }

    public void testT50_copyInValidTileType() {
        TileType source = createTileType();
        TileType target = createTileType();

        initializeFeatureContainer(source);
        initializeFeatureContainer(target);

        source.setHumidity(0, 15);
        source.setHumidity(1, 85);
        source.setTemperature(0, -5);
        source.setTemperature(1, 35);
        source.setAltitude(0, 10);
        source.setAltitude(1, 40);

        source.setResourceTypes(new ArrayList<>());
        source.setDisasters(new ArrayList<>());
        source.setProductionTypes(new ArrayList<>());

        assertTrue(target.copyIn(source));

        assertEquals(15, target.getHumidity(0));
        assertEquals(85, target.getHumidity(1));
        assertEquals(-5, target.getTemperature(0));
        assertEquals(35, target.getTemperature(1));
        assertEquals(10, target.getAltitude(0));
        assertEquals(40, target.getAltitude(1));

        assertNotNull(target.getResourceTypes());
        assertNotNull(target.getDisasters());
        assertNotNull(target.getProductionTypes());

        assertTrue(target.getResourceTypes().isEmpty());
        assertTrue(target.getDisasters().isEmpty());
        assertTrue(target.getProductionTypes().isEmpty());
    }

    public void testT51_copyInCopiesResourceTypes() {
        TileType source = createTileType();
        TileType target = createTileType();

        initializeFeatureContainer(source);
        initializeFeatureContainer(target);

        source.setResourceTypes(new ArrayList<>());
        source.setDisasters(new ArrayList<>());
        source.setProductionTypes(new ArrayList<>());

        target.setResourceTypes(new ArrayList<>());
        target.setDisasters(new ArrayList<>());

        assertTrue(target.copyIn(source));
        assertNotNull(target.getResourceTypes());
        assertTrue(target.getResourceTypes().isEmpty());
    }

    public void testT52_copyInCopiesDisasters() {
        TileType source = createTileType();
        TileType target = createTileType();

        initializeFeatureContainer(source);
        initializeFeatureContainer(target);

        source.setResourceTypes(new ArrayList<>());
        source.setDisasters(new ArrayList<>());
        source.setProductionTypes(new ArrayList<>());

        target.setResourceTypes(new ArrayList<>());
        target.setDisasters(new ArrayList<>());

        assertTrue(target.copyIn(source));
        assertNotNull(target.getDisasters());
        assertTrue(target.getDisasters().isEmpty());
    }

    public void testT53_copyInCopiesProductionTypes() {
        TileType source = createTileType();
        TileType target = createTileType();

        initializeFeatureContainer(source);
        initializeFeatureContainer(target);

        source.setResourceTypes(new ArrayList<>());
        source.setDisasters(new ArrayList<>());
        source.setProductionTypes(new ArrayList<>());

        target.setResourceTypes(new ArrayList<>());
        target.setDisasters(new ArrayList<>());

        assertTrue(target.copyIn(source));
        assertNotNull(target.getProductionTypes());
        assertTrue(target.getProductionTypes().isEmpty());
    }

        public void testT64_canProduceWithUnitType() {
        GoodsType food = spec().getGoodsType("model.goods.food");
        UnitType unitType = spec().getUnitType("model.unit.freeColonist");

        assertNotNull(food);
        assertNotNull(unitType);

        TileType tile = createTileType();

        ProductionType production
            = new ProductionType(new AbstractGoods(food, 5), false, null);

        tile.setProductionTypes(Collections.singletonList(production));

        assertTrue(tile.canProduce(food, unitType));
    }

    public void testT65_canProduceWithUnitTypeNoMatchingProduction() {
        GoodsType food = spec().getGoodsType("model.goods.food");
        UnitType unitType = spec().getUnitType("model.unit.freeColonist");

        assertNotNull(food);
        assertNotNull(unitType);

        TileType tile = createTileType();

        assertFalse(tile.canProduce(food, unitType));
    }

    public void testT66_getPotentialProductionUnattended() {
        GoodsType food = spec().getGoodsType("model.goods.food");
        assertNotNull(food);

        TileType tile = createTileType();

        ProductionType production
            = new ProductionType(new AbstractGoods(food, 5), true, null);

        tile.setProductionTypes(Collections.singletonList(production));

        assertEquals(5, tile.getPotentialProduction(food, null));
    }

    public void testT67_getPotentialProductionWithUnitType() {
        GoodsType food = spec().getGoodsType("model.goods.food");
        UnitType unitType = spec().getUnitType("model.unit.freeColonist");

        assertNotNull(food);
        assertNotNull(unitType);

        TileType tile = createTileType();

        ProductionType production
            = new ProductionType(new AbstractGoods(food, 5), false, null);

        tile.setProductionTypes(Collections.singletonList(production));

        assertEquals(5, tile.getPotentialProduction(food, unitType));
    }

    public void testT68_getPotentialProductionWithoutMatchingProduction() {
        GoodsType food = spec().getGoodsType("model.goods.food");
        UnitType unitType = spec().getUnitType("model.unit.freeColonist");

        assertNotNull(food);
        assertNotNull(unitType);

        TileType tile = createTileType();

        assertEquals(0, tile.getPotentialProduction(food, unitType));
    }

    public void testT69_getPotentialProductionNegativeAmount() {
        GoodsType food = spec().getGoodsType("model.goods.food");
        assertNotNull(food);

        TileType tile = createTileType();

        ProductionType production
            = new ProductionType(new AbstractGoods(food, -5), true, null);

        tile.setProductionTypes(Collections.singletonList(production));

        assertEquals(0, tile.getPotentialProduction(food, null));
    }
    public void testGetPotentialProductionNullGoods() {
    TileType tile = createTileType();

    assertEquals(0, tile.getPotentialProduction(null, null));
}

public void testGetPotentialProductionWithoutProductionType() {
    TileType tile = createTileType();
    GoodsType goods = spec().getGoodsTypeList().get(0);

    assertEquals(0, tile.getPotentialProduction(goods, null));
}

public void testGetPotentialProductionWithUnattendedProduction() {
    TileType tile = createTileType();
    GoodsType goods = spec().getGoodsTypeList().get(0);

    ProductionType production = new ProductionType(
        new AbstractGoods(goods, 5), true, null);

    tile.getProductionTypes().add(production);

    assertEquals(5, tile.getPotentialProduction(goods, null));
}

public void testGetPotentialProductionWithUnitType() {
    TileType tile = createTileType();
    GoodsType goods = spec().getGoodsTypeList().get(0);

    ProductionType production = new ProductionType(
        new AbstractGoods(goods, 5), false, null);

    tile.getProductionTypes().add(production);

    UnitType unitType = spec().getUnitTypeList().get(0);

    assertTrue(tile.getPotentialProduction(goods, unitType) >= 0);
}

public void testGetPotentialProductionWithDifferentGoodsType() {
    TileType tile = createTileType();
    GoodsType goods = spec().getGoodsTypeList().get(0);
    GoodsType other = spec().getGoodsTypeList().get(1);

    tile.getProductionTypes().add(new ProductionType(
        new AbstractGoods(goods, 5), true, null));

    assertEquals(0, tile.getPotentialProduction(other, null));
}
public void testWriteAttributes() throws Exception {
    TileType tile = createTileType();

    String xml = writeTileTypeXML(tile);

    assertTrue(xml.contains("basic-move-cost"));
    assertTrue(xml.contains("basic-work-turns"));
    assertTrue(xml.contains("is-forest"));
    assertTrue(xml.contains("is-water"));
    assertTrue(xml.contains("is-elevation"));
    assertTrue(xml.contains("is-connected"));
    assertTrue(xml.contains("can-settle"));
}

public void testWriteChildrenWithEmptyLists() throws Exception {
    TileType tile = createTileType();

    String xml = writeTileTypeXML(tile);

    assertTrue(xml.contains("<gen"));
    assertTrue(xml.contains("humidity-minimum"));
    assertTrue(xml.contains("temperature-minimum"));
    assertTrue(xml.contains("altitude-minimum"));
}

public void testWriteChildrenWithProduction() throws Exception {
    TileType tile = createTileType();
    GoodsType goods = spec().getGoodsTypeList().get(0);

    tile.getProductionTypes().add(new ProductionType(
        new AbstractGoods(goods, 5), true, null));

    String xml = writeTileTypeXML(tile);

    assertTrue(xml.contains("<production"));
}

public void testWriteChildrenWithResource() throws Exception {
    TileType tile = createTileType();

    ResourceType resource = spec().getResourceTypeList().get(0);
    tile.getResourceTypes().add(new RandomChoice<>(resource, 75));

    String xml = writeTileTypeXML(tile);

    assertTrue(xml.contains("<resource"));
    assertTrue(xml.contains("probability=\"75\""));
}

public void testWriteChildrenWithDisaster() throws Exception {
    TileType tile = createTileType();

    assertFalse(spec().getDisasters().isEmpty());

    Disaster disaster = spec().getDisasters().get(0);

    tile.setDisasters(Collections.singletonList(
        new RandomChoice<>(disaster, 60)));

    String xml = writeTileTypeXML(tile);

    assertTrue(xml.contains("<disaster"));
    assertTrue(xml.contains("probability=\"60\""));
}
public void testReadTileTypeXMLAttributes() throws Exception {
    String xml =
        "<tile-type id=\"test.tile\""
        + " basic-move-cost=\"3\""
        + " basic-work-turns=\"4\""
        + " is-forest=\"true\""
        + " is-water=\"false\""
        + " is-elevation=\"true\""
        + " is-connected=\"true\""
        + " can-settle=\"true\">"
        + "<gen humidity-minimum=\"10\" humidity-maximum=\"80\""
        + " temperature-minimum=\"-5\" temperature-maximum=\"35\""
        + " altitude-minimum=\"5\" altitude-maximum=\"30\"/>"
        + "</tile-type>";

    TileType tile = readTileTypeXML(xml);

    assertEquals(3, tile.getBasicMoveCost());
    assertEquals(4, tile.getBasicWorkTurns());
    assertTrue(tile.isForested());
    assertFalse(tile.isWater());
    assertTrue(tile.isElevation());
    assertTrue(tile.isHighSeasConnected());
    assertTrue(tile.canSettle());

    assertTrue(tile.withinRange(TileType.RangeType.HUMIDITY, 10));
    assertTrue(tile.withinRange(TileType.RangeType.TEMPERATURE, 35));
    assertTrue(tile.withinRange(TileType.RangeType.ALTITUDE, 30));
}

public void testReadTileTypeXMLDefaultAttributes() throws Exception {
    String xml =
        "<tile-type id=\"test.tile\">"
        + "<gen/>"
        + "</tile-type>";

    TileType tile = readTileTypeXML(xml);

    assertEquals(1, tile.getBasicMoveCost());
    assertEquals(1, tile.getBasicWorkTurns());
    assertFalse(tile.isForested());
    assertFalse(tile.isWater());
    assertFalse(tile.isElevation());
    assertFalse(tile.isHighSeasConnected());
    assertTrue(tile.canSettle());
}

public void testReadTileTypeXMLLegacyRangeAttributes() throws Exception {
    String xml =
        "<tile-type id=\"test.tile\">"
        + "<gen humidityMin=\"15\" humidityMax=\"75\""
        + " temperatureMin=\"-10\" temperatureMax=\"30\""
        + " altitudeMin=\"5\" altitudeMax=\"25\"/>"
        + "</tile-type>";

    TileType tile = readTileTypeXML(xml);

    assertTrue(tile.withinRange(TileType.RangeType.HUMIDITY, 15));
    assertTrue(tile.withinRange(TileType.RangeType.TEMPERATURE, 30));
    assertTrue(tile.withinRange(TileType.RangeType.ALTITUDE, 25));
}
public void testReadTileTypeXMLDeleteProduction() throws Exception {
    TileType tile = createTileType();
    GoodsType goods = spec().getGoodsTypeList().get(0);

    tile.getProductionTypes().add(new ProductionType(
        new AbstractGoods(goods, 5), true, null));

    String xml =
        "<tile-type id=\"test.tile\">"
        + "<production delete=\"true\"/>"
        + "</tile-type>";

    try (FreeColXMLReader xr = new FreeColXMLReader(
            new StringReader(xml))) {
        xr.nextTag();
        tile.readFromXML(xr);
    }

    assertTrue(tile.getProductionTypes().isEmpty());
}

public void testReadTileTypeXMLLegacyPrimaryProduction()
    throws Exception {
    GoodsType goods = spec().getGoodsTypeList().get(0);

    String xml =
        "<tile-type id=\"test.tile\">"
        + "<primary-production goods-type=\"" + goods.getId()
        + "\" value=\"4\"/>"
        + "</tile-type>";

    TileType tile = readTileTypeXML(xml);

    assertFalse(tile.getProductionTypes().isEmpty());
    assertTrue(tile.getProductionTypes().get(0).getUnattended());
}

public void testReadTileTypeXMLLegacySecondaryProduction()
    throws Exception {
    GoodsType goods = spec().getGoodsTypeList().get(0);

    String xml =
        "<tile-type id=\"test.tile\">"
        + "<secondary-production goods-type=\"" + goods.getId()
        + "\" value=\"3\"/>"
        + "</tile-type>";

    TileType tile = readTileTypeXML(xml);

    assertFalse(tile.getProductionTypes().isEmpty());
    assertTrue(tile.getProductionTypes().get(0).getUnattended());
}
}
