
package net.sf.freecol.common.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.io.StringReader;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

import net.sf.freecol.common.io.FreeColXMLReader;
import net.sf.freecol.common.io.FreeColXMLWriter;

import net.sf.freecol.server.model.ServerUnit;
import net.sf.freecol.util.test.FreeColTestCase;

public class TileImprovementTypeCFTTest extends FreeColTestCase {

    private TileImprovementType getPlow() {
        return spec().getTileImprovementType("model.improvement.plow");
    }

    private TileImprovementType getRoad() {
        return spec().getTileImprovementType("model.improvement.road");
    }

    private TileImprovementType getRiver() {
        return spec().getTileImprovementType("model.improvement.river");
    }

    private TileImprovementType getClearForest() {
        return spec().getTileImprovementType("model.improvement.clearForest");
    }

    private TileType getPlains() {
        return spec().getTileType("model.tile.plains");
    }

    private GoodsType getGrain() {
        return spec().getGoodsType("model.goods.grain");
    }

    private UnitType getColonistType() {
        return spec().getDefaultUnitType();
    }

    private Unit createWorker() {
        Game game = getGame();
        net.sf.freecol.common.model.Map map = getTestMap();
        game.setMap(map);

        Player player = game.getPlayers(p -> true)
            .findFirst()
            .orElse(null);

        assertNotNull("Test player must exist", player);

        Tile tile = map.getTile(5, 8);
        assertNotNull("Test tile must exist", tile);

        return new ServerUnit(game, tile, player, getColonistType());
    }

    private Modifier firstModifier(
            TileImprovementType type, GoodsType goodsType) {
        return type.getModifiers(goodsType.getId())
            .findFirst()
            .orElse(null);
    }

    // ============================================================
    // Basic getters
    // ============================================================

    public void testTI01_getXMLTagName() {
        assertEquals(TileImprovementType.TAG, getPlow().getXMLTagName());
    }

    public void testTI02_basicGetters() {
        TileImprovementType type = getPlow();

        assertNotNull(type);
        assertTrue(type.getMagnitude() >= 0);
        assertTrue(type.getAddWorkTurns() >= 0);
        assertTrue(type.getExpendedAmount() >= 0);
        assertTrue(type.getExposeResourcePercent() >= 0);
    }

    public void testTI03_isNatural() {
        assertTrue(getRiver().isNatural());
        assertFalse(getPlow().isNatural());
    }

    public void testTI04_setAndGetZIndex() {
        TileImprovementType type = new TileImprovementType(
            "test.improvement.zindex", spec()
        );

        assertEquals(0, type.getZIndex());

        type.setZIndex(5);
        assertEquals(5, type.getZIndex());

        type.setZIndex(-2);
        assertEquals(-2, type.getZIndex());
    }

    // ============================================================
    // Movement cost
    // ============================================================

    public void testTI05_getMoveCost() {
        TileImprovementType type = getRoad();

        assertNotNull(type);
        assertTrue(type.getMoveCost() >= 0);
    }

    public void testTI06_getMoveCostBoundaryValues() {
        TileImprovementType[] types = {
            getPlow(), getRoad(), getRiver(), getClearForest()
        };

        int[] originalCosts = {
            -1, 0, 1, 2, 3, 5, 10, 20, 100
        };

        for (TileImprovementType type : types) {
            assertNotNull(type);

            int movementCost = type.getMoveCost();

            for (int originalCost : originalCosts) {
                int expected = (movementCost > 0
                    && movementCost < originalCost)
                    ? movementCost : originalCost;

                assertEquals(
                    "Improvement: " + type.getId()
                        + ", originalCost: " + originalCost,
                    expected,
                    type.getMoveCost(originalCost)
                );
            }
        }
    }

    // ============================================================
    // Worker restrictions
    // ============================================================

    public void testTI07_workerAllowedWhenRestrictionsAreNull() {
        TileImprovementType type = new TileImprovementType(
            "test.improvement.workers.null", spec()
        );

        assertTrue(type.isWorkerAllowed(createWorker()));
    }

    public void testTI08_workerAllowedWhenRestrictionsAreEmpty() {
        TileImprovementType type = new TileImprovementType(
            "test.improvement.workers.empty", spec()
        );

        type.setAllowedWorkers(new HashSet<>());

        assertNotNull(type.getAllowedWorkers());
        assertTrue(type.getAllowedWorkers().isEmpty());
        assertTrue(type.isWorkerAllowed(createWorker()));
    }

    public void testTI09_workerAllowedWhenTypeIsListed() {
        TileImprovementType type = new TileImprovementType(
            "test.improvement.workers.allowed", spec()
        );

        Set<String> workers = new HashSet<>();
        workers.add(getColonistType().getId());
        type.setAllowedWorkers(workers);

        assertTrue(type.isWorkerAllowed(createWorker()));
    }

    public void testTI10_workerDeniedWhenTypeIsNotListed() {
        TileImprovementType type = new TileImprovementType(
            "test.improvement.workers.denied", spec()
        );

        Set<String> workers = new HashSet<>();
        workers.add("model.unit.nonexistent");
        type.setAllowedWorkers(workers);

        assertFalse(type.isWorkerAllowed(createWorker()));
    }

    public void testTI11_setAllowedWorkersReplacesOldValues() {
        TileImprovementType type = new TileImprovementType(
            "test.improvement.workers.replace", spec()
        );

        Set<String> first = new HashSet<>();
        first.add("worker.first");
        type.setAllowedWorkers(first);

        Set<String> second = new HashSet<>();
        second.add("worker.second");
        type.setAllowedWorkers(second);

        assertEquals(1, type.getAllowedWorkers().size());
        assertTrue(type.getAllowedWorkers().contains("worker.second"));
        assertFalse(type.getAllowedWorkers().contains("worker.first"));
    }

    // ============================================================
    // Tile type scopes
    // ============================================================

    public void testTI12_isTileTypeAllowed() {
        TileImprovementType type = getPlow();
        TileType tileType = getPlains();

        assertNotNull(type);
        assertNotNull(tileType);

        boolean expected = type.getScopes()
            .allMatch(scope -> scope.appliesTo(tileType));

        assertEquals(expected, type.isTileTypeAllowed(tileType));
    }

    // ============================================================
    // Production modifier and bonus
    // ============================================================

    public void testTI13_getBonus() {
        TileImprovementType type = getPlow();
        GoodsType goods = getGrain();

        Modifier modifier = type.getProductionModifier(
            goods, type.getMagnitude()
        );

        int expected = modifier == null
            ? 0 : (int) modifier.getValue();

        assertEquals(expected, type.getBonus(goods));
    }

    public void testTI14_getProductionModifierWithoutModifier() {
        TileImprovementType type = new TileImprovementType(
            "test.improvement.no.modifier", spec()
        );

        assertNull(type.getProductionModifier(getGrain(), 1));
    }

    public void testTI15_getProductionModifierWithZeroMagnitude() {
        TileImprovementType type = getPlow();
        Modifier original = firstModifier(type, getGrain());

        assertSame(
            original,
            type.getProductionModifier(getGrain(), 0)
        );
    }

    public void testTI16_getProductionModifierWithNegativeMagnitude() {
        TileImprovementType type = getPlow();
        Modifier original = firstModifier(type, getGrain());

        assertSame(
            original,
            type.getProductionModifier(getGrain(), -1)
        );
    }

    public void testTI17_getProductionModifierWithPositiveMagnitude() {
        TileImprovementType type = getPlow();
        GoodsType goods = getGrain();
        Modifier original = firstModifier(type, goods);

        Modifier result = type.getProductionModifier(goods, 2);

        if (original == null || original.getValue() == 0) {
            assertSame(original, result);
        } else {
            assertNotNull(result);
            assertNotSame(original, result);
            assertEquals(
                original.getValue() * 2,
                result.getValue(),
                0.001
            );
        }
    }

    public void testTI18_getProductionModifierWithDifferentMagnitudes() {
        TileImprovementType type = getPlow();
        GoodsType goods = getGrain();
        Modifier original = firstModifier(type, goods);

        Modifier zero = type.getProductionModifier(goods, 0);
        Modifier negative = type.getProductionModifier(goods, -1);
        Modifier normal = type.getProductionModifier(goods, 1);
        Modifier larger = type.getProductionModifier(goods, 3);

        assertSame(original, zero);
        assertSame(original, negative);

        if (original == null || original.getValue() == 0) {
            assertSame(original, normal);
            assertSame(original, larger);
        } else {
            assertNotSame(original, normal);
            assertNotSame(original, larger);

            assertEquals(
                original.getValue(),
                normal.getValue(),
                0.001
            );
            assertEquals(
                original.getValue() * 3,
                larger.getValue(),
                0.001
            );
        }
    }

    // ============================================================
    // Production modifiers stream
    // ============================================================

    public void testTI19_productionModifiersWithNullGoods() {
        assertEquals(
            0,
            getPlow().getProductionModifiers(
                null, null, getPlains(), 1
            ).count()
        );
    }

    public void testTI20_productionModifiersWithoutModifier() {
        TileImprovementType type = new TileImprovementType(
            "test.improvement.no.production", spec()
        );

        assertEquals(
            0,
            type.getProductionModifiers(
                getGrain(), null, getPlains(), 1
            ).count()
        );
    }

    public void testTI21_naturalImprovementWithFoodAndNullUnit() {
        TileImprovementType type = getRiver();
        GoodsType goods = getGrain();

        assertTrue(type.isNatural());
        assertTrue(goods.isFoodType());

        assertEquals(
            0,
            type.getProductionModifiers(
                goods, null, getPlains(), 1
            ).count()
        );
    }

    public void testTI22_productionModifiersWithUnitType() {
        TileImprovementType type = getPlow();
        GoodsType goods = getGrain();
        UnitType unitType = getColonistType();

        Modifier modifier = type.getProductionModifier(
            goods, type.getMagnitude()
        );

        long expected = modifier == null ? 0 : 1;

        assertEquals(
            expected,
            type.getProductionModifiers(
                goods, unitType, getPlains(), type.getMagnitude()
            ).count()
        );
    }

    public void testTI23_productionModifiersWithZeroMagnitude() {
        TileImprovementType type = getPlow();
        GoodsType goods = getGrain();

        Modifier original = firstModifier(type, goods);

        long expected = original == null ? 0 : 1;

        assertEquals(
            expected,
            type.getProductionModifiers(
                goods, null, getPlains(), 0
            ).count()
        );
    }

    public void testTI24_productionModifiersWithNegativeMagnitude() {
        TileImprovementType type = getPlow();
        GoodsType goods = getGrain();

        Modifier original = firstModifier(type, goods);

        long expected = original == null ? 0 : 1;

        assertEquals(
            expected,
            type.getProductionModifiers(
                goods, null, getPlains(), -1
            ).count()
        );
    }

    // ============================================================
    // Tile type changes
    // ============================================================

    public void testTI25_tileTypeChangesInitiallyNull() {
        TileImprovementType type = new TileImprovementType(
            "test.improvement.changes", spec()
        );

        assertFalse(type.isChangeType());
        assertNull(type.getProduction(getPlains()));
        assertNull(type.getChange(getPlains()));
        assertFalse(type.changeContainsTarget(getPlains()));
    }

    public void testTI26_setTileTypeChangesEmpty() {
        TileImprovementType type = new TileImprovementType(
            "test.improvement.changes.empty", spec()
        );

        type.setTileTypeChanges(new HashMap<>());

        assertNotNull(type.getTileTypeChanges());
        assertTrue(type.getTileTypeChanges().isEmpty());
        assertFalse(type.isChangeType());
    }

    public void testTI27_setTileTypeChangesReplacesExistingValues() {
        TileImprovementType type = new TileImprovementType(
            "test.improvement.changes.replace", spec()
        );

        type.setTileTypeChanges(
            new HashMap<>(getClearForest().getTileTypeChanges())
        );

        assertTrue(type.isChangeType());

        type.setTileTypeChanges(new HashMap<>());

        assertTrue(type.getTileTypeChanges().isEmpty());
        assertFalse(type.isChangeType());
        assertNull(type.getChange(getPlains()));
    }

    public void testTI28_existingChangeType() {
        assertTrue(getClearForest().isChangeType());
    }

    public void testTI29_getChangeForMappedTileType() {
        TileImprovementType type = getClearForest();
        TileType from = spec().getTileType("model.tile.mixedForest");

        assertNotNull(from);
        assertNotNull(type.getChange(from));
    }

    public void testTI30_getChangeForUnmappedTileType() {
        TileImprovementType type = getClearForest();
        TileType ocean = spec().getTileType("model.tile.ocean");

        assertNotNull(ocean);
        assertNull(type.getChange(ocean));
    }

    public void testTI31_changeContainsTarget() {
        TileImprovementType type = getClearForest();
        TileType from = spec().getTileType("model.tile.mixedForest");

        TileType target = type.getChange(from);

        assertNotNull(target);
        assertTrue(type.changeContainsTarget(target));
    }

    public void testTI32_getProductionForMappedTileType() {
        TileImprovementType type = getClearForest();
        TileType from = spec().getTileType("model.tile.mixedForest");

        assertNotNull(from);

        TileTypeChange change = type.getTileTypeChanges().get(from);

        assertNotNull(change);
        assertSame(change.getProduction(), type.getProduction(from));
    }

    public void testTI33_getProductionForUnmappedTileType() {
        TileImprovementType type = getClearForest();
        TileType ocean = spec().getTileType("model.tile.ocean");

        assertNotNull(ocean);
        assertNull(type.getProduction(ocean));
    }

    public void testTI34_getProductionWithoutTileChanges() {
        TileImprovementType type = getPlow();

        assertNull(type.getProduction(getPlains()));
        assertNull(type.getChange(getPlains()));
    }

    // ============================================================
    // Improvement value
    // ============================================================

    public void testTI35_improvementValueForNonFarmedGoods() {
        GoodsType goods = spec().getGoodsTypeList().stream()
            .filter(g -> !g.isFarmed())
            .findFirst()
            .orElse(null);

        assertNotNull("Must have a non-farmed goods type", goods);

        assertEquals(
            0,
            getPlow().getImprovementValue(
                getPlains(), goods, getColonistType()
            )
        );
    }

    public void testTI36_improvementValueWithTileTypeChange() {
        TileImprovementType type = getClearForest();
        TileType from = spec().getTileType("model.tile.mixedForest");
        GoodsType goods = getGrain();
        UnitType unitType = getColonistType();

        assertNotNull(from);

        TileType to = type.getChange(from);
        assertNotNull(to);

        int oldProduction = from.getPotentialProduction(goods, unitType);
        int newProduction = to.getPotentialProduction(goods, unitType);

        assertEquals(
            newProduction - oldProduction,
            type.getImprovementValue(from, goods, unitType)
        );
    }

    public void testTI37_improvementValueWithoutTileTypeChange() {
        TileImprovementType type = getPlow();
        TileType tileType = getPlains();
        GoodsType goods = getGrain();
        UnitType unitType = getColonistType();

        assertNull(type.getChange(tileType));

        int production = tileType.getPotentialProduction(goods, unitType);
        int expected = 0;

        if (production > 0) {
            float changed = type.apply(
                production, null, goods.getId()
            );
            expected = (int) (changed - production);
        }

        assertEquals(
            expected,
            type.getImprovementValue(tileType, goods, unitType)
        );
    }

    public void testTI38_improvementValueWithZeroProduction() {
        TileType ocean = spec().getTileType("model.tile.ocean");
        GoodsType grain = getGrain();
        UnitType unitType = getColonistType();

        assertNotNull(ocean);
        assertEquals(0, ocean.getPotentialProduction(grain, unitType));
        assertNull(getPlow().getChange(ocean));

        assertEquals(
            0,
            getPlow().getImprovementValue(ocean, grain, unitType)
        );
    }

    public void testTI39_improvementValueWithTile() {
        Unit worker = createWorker();
        Tile tile = worker.getTile();

        assertNotNull(tile);

        TileImprovementType type = getPlow();
        GoodsType goods = getGrain();
        UnitType unitType = worker.getType();

        TileTypeChange change = type.getTileTypeChanges()
            .get(tile.getType());

        int expected;

        if (change != null) {
            expected = change.getTo().getPotentialProduction(
                goods, unitType
            ) - tile.getType().getPotentialProduction(
                goods, unitType
            );
        } else {
            int production = tile.getPotentialProduction(goods, unitType);
            expected = 0;

            if (production > 0) {
                float changed = type.apply(
                    production, null, goods.getId()
                );
                expected = (int) (changed - production);
            }
        }

        assertEquals(
            expected,
            type.getImprovementValue(tile, goods, unitType)
        );
    }

    public void testTI40_improvementValueWithNonFarmedGoodsAndTile() {
        Unit worker = createWorker();

        GoodsType goods = spec().getGoodsTypeList().stream()
            .filter(g -> !g.isFarmed())
            .findFirst()
            .orElse(null);

        assertNotNull("Must have a non-farmed goods type", goods);

        assertEquals(
            0,
            getPlow().getImprovementValue(
                worker.getTile(), goods, worker.getType()
            )
        );
    }

    // ============================================================
    // Disaster collection
    // ============================================================

    public void testTI41_disastersInitiallyEmpty() {
        TileImprovementType type = new TileImprovementType(
            "test.improvement.disasters", spec()
        );

        assertEquals(0, type.getDisasterChoices().count());
    }

    public void testTI42_setDisastersEmpty() {
        TileImprovementType type = new TileImprovementType(
            "test.improvement.disasters.empty", spec()
        );

        type.setDisasters(new ArrayList<>());

        assertNotNull(type.getDisasters());
        assertTrue(type.getDisasters().isEmpty());
        assertEquals(0, type.getDisasterChoices().count());
    }

    public void testTI43_setDisastersCopiesExistingCollection() {
        TileImprovementType type = new TileImprovementType(
            "test.improvement.disasters.copy", spec()
        );

        type.setDisasters(
            new ArrayList<>(getClearForest().getDisasters())
        );

        assertEquals(
            getClearForest().getDisasters().size(),
            type.getDisasters().size()
        );

        type.setDisasters(new ArrayList<>());

        assertTrue(type.getDisasters().isEmpty());
        assertEquals(0, type.getDisasterChoices().count());
    }

    // ============================================================
    // Lazy container initialization
    // ============================================================

    public void testTI44_getAllowedWorkersInitializesCollection() {
        TileImprovementType type = new TileImprovementType(
            "test.improvement.allowed.workers", spec()
        );

        assertNotNull(type.getAllowedWorkers());
        assertTrue(type.getAllowedWorkers().isEmpty());
    }

    public void testTI45_getTileTypeChangesInitializesCollection() {
        TileImprovementType type = new TileImprovementType(
            "test.improvement.tile.changes", spec()
        );

        assertNotNull(type.getTileTypeChanges());
        assertTrue(type.getTileTypeChanges().isEmpty());
        assertFalse(type.isChangeType());
    }

    public void testTI46_getDisastersInitializesCollection() {
        TileImprovementType type = new TileImprovementType(
            "test.improvement.disaster.collection", spec()
        );

        assertNotNull(type.getDisasters());
        assertTrue(type.getDisasters().isEmpty());
        assertEquals(0, type.getDisasterChoices().count());
    }

    // ============================================================
    // copyIn
    // ============================================================

    public void testTI47_copyInNull() {
        TileImprovementType target = new TileImprovementType(
            "test.improvement.copy.target", spec()
        );

        assertFalse(target.copyIn(null));
    }

    public void testTI48_copyInSameObject() {
        TileImprovementType type = getPlow();

        assertTrue(type.copyIn(type));
    }

    // ============================================================
    // XML serialization
    // ============================================================

    public void testTI49_writeAttributes() throws Exception {
        TileImprovementType improvement = getPlow();
        ByteArrayOutputStream output = new ByteArrayOutputStream();

        try (FreeColXMLWriter xw = new FreeColXMLWriter(
                output, FreeColXMLWriter.WriteScope.toSave(), true)) {
            xw.writeStartDocument("UTF-8", "1.0");
            xw.writeStartElement(improvement.getXMLTagName());
            improvement.writeAttributes(xw);
            xw.writeEndElement();
            xw.writeEndDocument();
        }

        String xml = output.toString(StandardCharsets.UTF_8);

        assertTrue(xml.contains("natural="));
        assertTrue(xml.contains("magnitude="));
        assertTrue(xml.contains("add-work-turns="));
        assertTrue(xml.contains("movement-cost="));
    }

    public void testTI50_writeChildrenEmpty() throws Exception {
        TileImprovementType improvement =
            new TileImprovementType("test.improvement.empty", spec());

        ByteArrayOutputStream output = new ByteArrayOutputStream();

        try (FreeColXMLWriter xw = new FreeColXMLWriter(
                output, FreeColXMLWriter.WriteScope.toSave(), true)) {
            xw.writeStartDocument("UTF-8", "1.0");
            xw.writeStartElement(improvement.getXMLTagName());
            improvement.writeChildren(xw);
            xw.writeEndElement();
            xw.writeEndDocument();
        }

        assertNotNull(output.toString(StandardCharsets.UTF_8));
    }

    public void testTI51_writeChildrenWithAllowedWorker()
            throws Exception {
        TileImprovementType improvement = getPlow();
        UnitType workerType = getColonistType();

        String xmlInput = "<improvement>"
            + "<worker id=\"" + workerType.getId() + "\"/>"
            + "</improvement>";

        try (FreeColXMLReader xr =
                 new FreeColXMLReader(new StringReader(xmlInput))) {
            xr.nextTag();
            xr.nextTag();
            improvement.readChild(xr);
        }

        ByteArrayOutputStream output = new ByteArrayOutputStream();

        try (FreeColXMLWriter xw = new FreeColXMLWriter(
                output, FreeColXMLWriter.WriteScope.toSave(), true)) {
            xw.writeStartDocument("UTF-8", "1.0");
            xw.writeStartElement(improvement.getXMLTagName());
            improvement.writeChildren(xw);
            xw.writeEndElement();
            xw.writeEndDocument();
        }

        String result = output.toString(StandardCharsets.UTF_8);

        assertTrue(result.contains("<worker"));
        assertTrue(result.contains(workerType.getId()));
    }

    public void testTI52_getImprovementValueWithoutUnitType() {
        Unit worker = createWorker();

        int result = getPlow().getImprovementValue(
            worker.getTile(), getGrain()
        );

        assertTrue(result >= 0);
    }

    // ============================================================
    // Additional C2 coverage
    // ============================================================

    public void testTI53_getBonusWithoutModifier() {
        TileImprovementType type = new TileImprovementType(
            "test.improvement.bonus.none", spec()
        );

        assertEquals(0, type.getBonus(getGrain()));
    }

    public void testTI54_workerRejectedByRequiredRole()
            throws Exception {
        TileImprovementType type = new TileImprovementType(
            "test.improvement.required.role", spec()
        );

        String xml = "<tile-improvement-type"
            + " required-role=\"model.role.pioneer\"/>";

        try (FreeColXMLReader xr =
                 new FreeColXMLReader(new StringReader(xml))) {
            xr.nextTag();
            type.readAttributes(xr);
        }

        Unit worker = createWorker();

        assertNotNull(type.getRequiredRole());
        assertFalse(type.isWorkerAllowed(worker));
    }

    public void testTI55_readAttributesWithDefaults()
            throws Exception {
        TileImprovementType type = new TileImprovementType(
            "test.improvement.read.defaults", spec()
        );

        String xml = "<tile-improvement-type/>";

        try (FreeColXMLReader xr =
                 new FreeColXMLReader(new StringReader(xml))) {
            xr.nextTag();
            type.readAttributes(xr);
        }

        assertFalse(type.isNatural());
        assertEquals(1, type.getMagnitude());
        assertEquals(0, type.getAddWorkTurns());
        assertNull(type.getRequiredImprovementType());
        assertNull(type.getRequiredRole());
        assertEquals(0, type.getExpendedAmount());
        assertEquals(0, type.getMoveCost());
        assertEquals(0, type.getZIndex());
        assertEquals(0, type.getExposeResourcePercent());
    }

    public void testTI56_readAttributesWithModernAttributes()
            throws Exception {
        TileImprovementType type = new TileImprovementType(
            "test.improvement.read.modern", spec()
        );

        String xml = "<tile-improvement-type"
            + " natural=\"true\""
            + " magnitude=\"3\""
            + " add-work-turns=\"4\""
            + " required-improvement=\"model.improvement.plow\""
            + " required-role=\"model.role.pioneer\""
            + " expended-amount=\"2\""
            + " movement-cost=\"5\""
            + " zIndex=\"7\""
            + " expose-resource-percent=\"8\"/>";

        try (FreeColXMLReader xr =
                 new FreeColXMLReader(new StringReader(xml))) {
            xr.nextTag();
            type.readAttributes(xr);
        }

        assertTrue(type.isNatural());
        assertEquals(3, type.getMagnitude());
        assertEquals(4, type.getAddWorkTurns());
        assertSame(getPlow(), type.getRequiredImprovementType());
        assertNotNull(type.getRequiredRole());
        assertEquals(2, type.getExpendedAmount());
        assertEquals(5, type.getMoveCost());
        assertEquals(7, type.getZIndex());
        assertEquals(8, type.getExposeResourcePercent());
    }

    public void testTI57_readAttributesWithLegacyAttributes()
            throws Exception {
        TileImprovementType type = new TileImprovementType(
            "test.improvement.read.legacy", spec()
        );

        String xml = "<tile-improvement-type"
            + " expended-equipment-type=\"model.goods.tools\""
            + " exposeResourcePercent=\"25\"/>";

        try (FreeColXMLReader xr =
                 new FreeColXMLReader(new StringReader(xml))) {
            xr.nextTag();
            type.readAttributes(xr);
        }

        assertNotNull(type.getRequiredRole());
        assertEquals(25, type.getExposeResourcePercent());
    }

    public void testTI58_writeAttributesWithOptionalValues()
            throws Exception {
        TileImprovementType improvement = new TileImprovementType(
            "test.improvement.write.optional", spec()
        );

        String xmlInput = "<tile-improvement-type"
            + " natural=\"true\""
            + " magnitude=\"3\""
            + " add-work-turns=\"4\""
            + " required-improvement=\"model.improvement.plow\""
            + " required-role=\"model.role.pioneer\""
            + " expended-amount=\"2\""
            + " movement-cost=\"5\""
            + " zIndex=\"7\""
            + " expose-resource-percent=\"8\"/>";

        try (FreeColXMLReader xr =
                 new FreeColXMLReader(new StringReader(xmlInput))) {
            xr.nextTag();
            improvement.readAttributes(xr);
        }

        ByteArrayOutputStream output = new ByteArrayOutputStream();

        try (FreeColXMLWriter xw = new FreeColXMLWriter(
                output, FreeColXMLWriter.WriteScope.toSave(), true)) {
            xw.writeStartDocument("UTF-8", "1.0");
            xw.writeStartElement(improvement.getXMLTagName());
            improvement.writeAttributes(xw);
            xw.writeEndElement();
            xw.writeEndDocument();
        }

        String result = output.toString(StandardCharsets.UTF_8);

        assertTrue(result.contains("required-improvement="));
        assertTrue(result.contains("required-role="));
        assertTrue(result.contains("expended-amount="));
        assertTrue(result.contains("expose-resource-percent="));
    }

    public void testTI59_writeChildrenWithWorkerAndTileChanges()
            throws Exception {
        TileImprovementType improvement = new TileImprovementType(
            "test.improvement.write.children", spec()
        );

        Set<String> workers = new HashSet<>();
        workers.add(getColonistType().getId());
        improvement.setAllowedWorkers(workers);

        improvement.setTileTypeChanges(
            new HashMap<>(getClearForest().getTileTypeChanges())
        );

        ByteArrayOutputStream output = new ByteArrayOutputStream();

        try (FreeColXMLWriter xw = new FreeColXMLWriter(
                output, FreeColXMLWriter.WriteScope.toSave(), true)) {
            xw.writeStartDocument("UTF-8", "1.0");
            xw.writeStartElement(improvement.getXMLTagName());
            improvement.writeChildren(xw);
            xw.writeEndElement();
            xw.writeEndDocument();
        }

        String result = output.toString(StandardCharsets.UTF_8);

        assertTrue(result.contains("<worker"));
        assertTrue(result.contains(getColonistType().getId()));
        assertTrue(result.contains("tile-type-change"));
    }

    public void testTI60_improvementValueWithoutUnitForNonFarmedGoods() {
        Unit worker = createWorker();

        GoodsType goods = spec().getGoodsTypeList().stream()
            .filter(g -> !g.isFarmed())
            .findFirst()
            .orElse(null);

        assertNotNull("A non-farmed goods type must exist", goods);

        assertEquals(
            0,
            getPlow().getImprovementValue(worker.getTile(), goods)
        );
    }

    public void testTI61_improvementValueWithoutUnitForFarmedGoods() {
        Unit worker = createWorker();

        assertEquals(
            getPlow().getImprovementValue(
                worker.getTile(), getGrain(), getColonistType()
            ),
            getPlow().getImprovementValue(
                worker.getTile(), getGrain()
            )
        );
    }

    public void testTI62_naturalImprovementWithNonFoodGoods() {
        GoodsType goods = spec().getGoodsTypeList().stream()
            .filter(g -> !g.isFoodType())
            .filter(g -> !g.isFarmed())
            .findFirst()
            .orElse(null);

        assertNotNull("Non-food goods must exist", goods);

        Modifier modifier = getRiver().getProductionModifier(goods, 1);
        long expected = modifier == null ? 0 : 1;

        assertEquals(
            expected,
            getRiver().getProductionModifiers(
                goods, null, getPlains(), 1
            ).count()
        );
    }

    public void testTI63_copyInIncompatibleObject() {
        TileImprovementType target = new TileImprovementType(
            "test.improvement.copy.incompatible", spec()
        );

        assertFalse(target.copyIn(getGrain()));
    }

    public void testTI64_writeChildrenWithEmptyWorkerCollection()
            throws Exception {
        TileImprovementType improvement = new TileImprovementType(
            "test.improvement.write.empty.workers", spec()
        );

        improvement.setAllowedWorkers(new HashSet<>());

        ByteArrayOutputStream output = new ByteArrayOutputStream();

        try (FreeColXMLWriter xw = new FreeColXMLWriter(
                output, FreeColXMLWriter.WriteScope.toSave(), true)) {
            xw.writeStartDocument("UTF-8", "1.0");
            xw.writeStartElement(improvement.getXMLTagName());
            improvement.writeChildren(xw);
            xw.writeEndElement();
            xw.writeEndDocument();
        }

        String result = output.toString(StandardCharsets.UTF_8);

        assertNotNull(result);
        assertFalse(result.contains("<worker"));
    }
        // ============================================================
    // Additional C2 coverage - TI65+
    // ============================================================

    public void testTI65_copyInCopiesTileImprovementData() {
    String id = "test.improvement.copy.same";

    TileImprovementType source = new TileImprovementType(id, spec());
    TileImprovementType target = new TileImprovementType(id, spec());

    // Initialize feature containers before copyIn
    source.getFeatureContainer();
    target.getFeatureContainer();

    Set<String> workers = new HashSet<>();
    workers.add(getColonistType().getId());
    source.setAllowedWorkers(workers);

    source.setTileTypeChanges(
        new HashMap<>(getClearForest().getTileTypeChanges())
    );

    source.setDisasters(
        new ArrayList<>(getClearForest().getDisasters())
    );

    source.setZIndex(9);

    assertNotSame(source, target);
    assertTrue(target.copyIn(source));

    assertEquals(source.getZIndex(), target.getZIndex());
    assertEquals(source.getAllowedWorkers(), target.getAllowedWorkers());
    assertEquals(source.getTileTypeChanges(), target.getTileTypeChanges());
    assertEquals(source.getDisasters().size(), target.getDisasters().size());
    assertTrue(target.isChangeType());
}
    public void testTI66_writeChildrenWithTileChangesOnly()
            throws Exception {
        TileImprovementType improvement = new TileImprovementType(
            "test.improvement.write.tile.changes.only", spec()
        );

        improvement.setTileTypeChanges(
            new HashMap<>(getClearForest().getTileTypeChanges())
        );

        ByteArrayOutputStream output = new ByteArrayOutputStream();

        try (FreeColXMLWriter xw = new FreeColXMLWriter(
                output, FreeColXMLWriter.WriteScope.toSave(), true)) {
            xw.writeStartDocument("UTF-8", "1.0");
            xw.writeStartElement(improvement.getXMLTagName());
            improvement.writeChildren(xw);
            xw.writeEndElement();
            xw.writeEndDocument();
        }

        String result = output.toString(StandardCharsets.UTF_8);

        assertTrue(result.contains("tile-type-change"));
        assertFalse(result.contains("<worker"));
    }

    public void testTI67_productionModifiersForNonFoodGoods() {
        GoodsType goods = spec().getGoodsTypeList().stream()
            .filter(g -> !g.isFoodType())
            .findFirst()
            .orElse(null);

        assertNotNull("A non-food goods type must exist", goods);

        TileImprovementType type = getPlow();

        Modifier modifier = type.getProductionModifier(goods, 1);

        boolean onlyNatural = spec().getBoolean(
            net.sf.freecol.common.option.GameOptions
                .ONLY_NATURAL_IMPROVEMENTS
        );

        long expected = (onlyNatural || modifier == null) ? 0 : 1;

        assertEquals(
            expected,
            type.getProductionModifiers(
                goods, null, getPlains(), 1
            ).count()
        );
    }
    public void testTI68_writeChildrenWithDisasters() throws Exception {
    TileImprovementType improvement = new TileImprovementType(
        "test.improvement.write.disasters", spec()
    );

    java.util.List<Disaster> disasters = spec().getDisasters();

    assertNotNull(disasters);
    assertFalse("Specification must contain at least one disaster",
        disasters.isEmpty());

    Disaster disaster = disasters.get(0);

    String xmlInput = "<tile-improvement-type>"
        + "<disaster id=\"" + disaster.getId()
        + "\" probability=\"37\"/>"
        + "</tile-improvement-type>";

    try (FreeColXMLReader xr =
             new FreeColXMLReader(new StringReader(xmlInput))) {
        xr.nextTag();
        xr.nextTag();
        improvement.readChild(xr);
    }

    assertEquals(1, improvement.getDisasterChoices().count());

    ByteArrayOutputStream output = new ByteArrayOutputStream();

    try (FreeColXMLWriter xw = new FreeColXMLWriter(
            output, FreeColXMLWriter.WriteScope.toSave(), true)) {
        xw.writeStartDocument("UTF-8", "1.0");
        xw.writeStartElement(improvement.getXMLTagName());
        improvement.writeChildren(xw);
        xw.writeEndElement();
        xw.writeEndDocument();
    }

    String xml = output.toString(StandardCharsets.UTF_8);

    assertTrue(xml.contains("<disaster"));
    assertTrue(xml.contains(disaster.getId()));
    assertTrue(xml.contains("probability=\"37\""));
}
public void testTI69_writeAttributesWithoutOptionalValues()
        throws Exception {
    TileImprovementType improvement = new TileImprovementType(
        "test.improvement.write.no.optional", spec()
    );

    ByteArrayOutputStream output = new ByteArrayOutputStream();

    try (FreeColXMLWriter xw = new FreeColXMLWriter(
            output, FreeColXMLWriter.WriteScope.toSave(), true)) {
        xw.writeStartDocument("UTF-8", "1.0");
        xw.writeStartElement(improvement.getXMLTagName());
        improvement.writeAttributes(xw);
        xw.writeEndElement();
        xw.writeEndDocument();
    }

    String xml = output.toString(StandardCharsets.UTF_8);

    assertFalse(xml.contains("required-improvement="));
    assertFalse(xml.contains("required-role="));
    assertFalse(xml.contains("expended-amount="));
}
public void testTI70_addMultipleDisasters() throws Exception {
    TileImprovementType improvement = new TileImprovementType(
        "test.improvement.multiple.disasters", spec()
    );

    java.util.List<Disaster> disasters = spec().getDisasters();

    assertNotNull(disasters);
    assertTrue("At least two disasters are required",
        disasters.size() >= 2);

    Disaster first = disasters.get(0);
    Disaster second = disasters.get(1);

    String xmlInput = "<tile-improvement-type>"
        + "<disaster id=\"" + first.getId()
        + "\" probability=\"30\"/>"
        + "<disaster id=\"" + second.getId()
        + "\" probability=\"70\"/>"
        + "</tile-improvement-type>";

    try (FreeColXMLReader xr =
             new FreeColXMLReader(new StringReader(xmlInput))) {
        xr.nextTag();

        xr.nextTag();
        improvement.readChild(xr);

        xr.nextTag();
        improvement.readChild(xr);
    }

    assertEquals(2, improvement.getDisasterChoices().count());
}
public void testTI71_productionModifiersWithMatchingExpertProduction()
        throws Exception {
    GoodsType grain = getGrain();

    UnitType expert = new UnitType(
        "test.unit.expert.grain", spec()
    );

    String xml = "<unit-type id=\"test.unit.expert.grain\""
        + " expert-production=\"" + grain.getId() + "\"/>";

    try (FreeColXMLReader xr =
             new FreeColXMLReader(new StringReader(xml))) {
        xr.nextTag();
        expert.readAttributes(xr);
    }

    assertSame(grain, expert.getExpertProduction());

    TileImprovementType improvement = getPlow();

    Modifier modifier = improvement.getProductionModifier(
        grain, improvement.getMagnitude()
    );

    assertNotNull("Plow must have a grain production modifier", modifier);

    assertEquals(
        1,
        improvement.getProductionModifiers(
            grain, expert, getPlains(), improvement.getMagnitude()
        ).count()
    );
}
public void testTI72_productionModifiersWithDifferentExpertProduction()
        throws Exception {
    GoodsType grain = getGrain();

    GoodsType otherGoods = spec().getGoodsTypeList().stream()
        .filter(g -> !g.equals(grain))
        .findFirst()
        .orElse(null);

    assertNotNull("Another goods type must exist", otherGoods);

    UnitType expert = new UnitType(
        "test.unit.expert.other", spec()
    );

    String xml = "<unit-type id=\"test.unit.expert.other\""
        + " expert-production=\"" + otherGoods.getId() + "\"/>";

    try (FreeColXMLReader xr =
             new FreeColXMLReader(new StringReader(xml))) {
        xr.nextTag();
        expert.readAttributes(xr);
    }

    assertSame(otherGoods, expert.getExpertProduction());

    TileImprovementType improvement = getPlow();

    Modifier modifier = improvement.getProductionModifier(
        grain, improvement.getMagnitude()
    );

    assertNotNull(modifier);

    assertEquals(
        1,
        improvement.getProductionModifiers(
            grain, expert, getPlains(), improvement.getMagnitude()
        ).count()
    );
}
}
