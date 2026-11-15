
package net.sf.freecol.common.model;

import java.util.HashMap;
import java.util.HashSet;
import java.util.ArrayList;
import java.util.Set;
import java.util.Map;
import java.util.List;

import org.junit.Test;

import static org.junit.Assert.*;

public class TileImprovementTypeCFTTest {

    private TileImprovementType createType() {
        return new TileImprovementType("model.improvement.cftTest", null);
    }

    // ==========================================
    // 1. Basic attributes
    // ==========================================

    @Test
    public void TI01_getXMLTagName() {
        TileImprovementType type = createType();
        assertEquals("tile-improvement-type", type.getXMLTagName());
    }

    @Test
    public void TI02_defaultNatural() {
        TileImprovementType type = createType();
        assertFalse(type.isNatural());
    }

    @Test
    public void TI03_defaultMagnitude() {
        TileImprovementType type = createType();
        assertEquals(0, type.getMagnitude());
    }

    @Test
    public void TI04_defaultAddWorkTurns() {
        TileImprovementType type = createType();
        assertEquals(0, type.getAddWorkTurns());
    }

    @Test
    public void TI05_defaultRequiredImprovementType() {
        TileImprovementType type = createType();
        assertNull(type.getRequiredImprovementType());
    }

    @Test
    public void TI06_defaultRequiredRole() {
        TileImprovementType type = createType();
        assertNull(type.getRequiredRole());
    }

    @Test
    public void TI07_defaultExpendedAmount() {
        TileImprovementType type = createType();
        assertEquals(0, type.getExpendedAmount());
    }

    @Test
    public void TI08_defaultMoveCost() {
        TileImprovementType type = createType();
        assertEquals(-1, type.getMoveCost());
    }

    @Test
    public void TI09_defaultExposeResourcePercent() {
        TileImprovementType type = createType();
        assertEquals(0, type.getExposeResourcePercent());
    }

    @Test
    public void TI10_defaultZIndex() {
        TileImprovementType type = createType();
        assertEquals(0, type.getZIndex());
    }

    @Test
    public void TI11_setZIndexPositive() {
        TileImprovementType type = createType();
        type.setZIndex(10);
        assertEquals(10, type.getZIndex());
    }

    @Test
    public void TI12_setZIndexNegative() {
        TileImprovementType type = createType();
        type.setZIndex(-5);
        assertEquals(-5, type.getZIndex());
    }

    @Test
    public void TI13_setZIndexZero() {
        TileImprovementType type = createType();
        type.setZIndex(0);
        assertEquals(0, type.getZIndex());
    }

    // ==========================================
    // 2. Movement cost
    // ==========================================

    @Test
    public void TI14_getMoveCostWithDefaultMovementCost() {
        TileImprovementType type = createType();
        assertEquals(3, type.getMoveCost(3));
    }

    @Test
    public void TI15_getMoveCostWithZeroOriginalCost() {
        TileImprovementType type = createType();
        assertEquals(0, type.getMoveCost(0));
    }

    @Test
    public void TI16_getMoveCostWithNegativeOriginalCost() {
        TileImprovementType type = createType();
        assertEquals(-2, type.getMoveCost(-2));
    }

    // ==========================================
    // 3. Worker containers
    // ==========================================

    @Test
    public void TI17_getAllowedWorkersInitiallyEmpty() {
        TileImprovementType type = createType();
        assertNotNull(type.getAllowedWorkers());
        assertTrue(type.getAllowedWorkers().isEmpty());
    }

    @Test
    public void TI18_addAllowedWorkers() {
        TileImprovementType type = createType();

        type.getAllowedWorkers().add("model.unit.freeColonist");
        type.getAllowedWorkers().add("model.unit.expertFarmer");

        assertEquals(2, type.getAllowedWorkers().size());
        assertTrue(type.getAllowedWorkers()
            .contains("model.unit.freeColonist"));
        assertTrue(type.getAllowedWorkers()
            .contains("model.unit.expertFarmer"));
    }

    @Test
    public void TI19_setAllowedWorkers() {
        TileImprovementType type = createType();

        Set<String> workers = new HashSet<>();
        workers.add("model.unit.freeColonist");
        workers.add("model.unit.expertFarmer");

        type.setAllowedWorkers(workers);

        assertEquals(2, type.getAllowedWorkers().size());
        assertTrue(type.getAllowedWorkers()
            .contains("model.unit.freeColonist"));
    }

    @Test
    public void TI20_setAllowedWorkersEmpty() {
        TileImprovementType type = createType();

        Set<String> workers = new HashSet<>();
        workers.add("model.unit.freeColonist");
        type.setAllowedWorkers(workers);

        type.setAllowedWorkers(new HashSet<>());

        assertTrue(type.getAllowedWorkers().isEmpty());
    }

    // ==========================================
    // 4. Tile type changes
    // ==========================================

    @Test
    public void TI21_isChangeTypeInitiallyFalse() {
        TileImprovementType type = createType();
        assertFalse(type.isChangeType());
    }

    @Test
    public void TI22_getTileTypeChangesInitiallyEmpty() {
        TileImprovementType type = createType();

        assertNotNull(type.getTileTypeChanges());
        assertTrue(type.getTileTypeChanges().isEmpty());
    }

    @Test
    public void TI23_setTileTypeChangesEmpty() {
        TileImprovementType type = createType();

        type.setTileTypeChanges(new HashMap<>());

        assertFalse(type.isChangeType());
    }

    @Test
    public void TI24_getProductionWithEmptyChanges() {
        TileImprovementType type = createType();

        type.setTileTypeChanges(new HashMap<>());

        assertNull(type.getProduction(null));
    }

    @Test
    public void TI25_getChangeWithEmptyChanges() {
        TileImprovementType type = createType();

        type.setTileTypeChanges(new HashMap<>());

        assertNull(type.getChange(null));
    }

    @Test
    public void TI26_changeContainsTargetWithNullMap() {
        TileImprovementType type = createType();

        assertFalse(type.changeContainsTarget(null));
    }

    @Test
    public void TI27_changeContainsTargetWithEmptyMap() {
        TileImprovementType type = createType();

        type.setTileTypeChanges(new HashMap<>());

        assertFalse(type.changeContainsTarget(null));
    }

    // ==========================================
    // 5. Disaster containers
    // ==========================================

    @Test
    public void TI28_getDisastersInitiallyEmpty() {
        TileImprovementType type = createType();

        assertNotNull(type.getDisasters());
        assertTrue(type.getDisasters().isEmpty());
    }

    @Test
    public void TI29_getDisasterChoicesInitiallyEmpty() {
        TileImprovementType type = createType();

        assertEquals(0, type.getDisasterChoices().count());
    }

    @Test
    public void TI30_setDisastersEmpty() {
        TileImprovementType type = createType();

        type.setDisasters(new ArrayList<>());

        assertEquals(0, type.getDisasterChoices().count());
    }

    // ==========================================
    // 6. Container state transitions
    // ==========================================

    @Test
    public void TI31_clearTileTypeChanges() {
        TileImprovementType type = createType();

        type.setTileTypeChanges(new HashMap<>());
        type.getTileTypeChanges().clear();

        assertFalse(type.isChangeType());
    }

    @Test
    public void TI32_clearAllowedWorkers() {
        TileImprovementType type = createType();

        Set<String> workers = new HashSet<>();
        workers.add("model.unit.freeColonist");

        type.setAllowedWorkers(workers);
        type.setAllowedWorkers(new HashSet<>());

        assertTrue(type.getAllowedWorkers().isEmpty());
    }

    @Test
    public void TI33_clearDisasters() {
        TileImprovementType type = createType();

        type.setDisasters(new ArrayList<>());
        type.setDisasters(new ArrayList<>());

        assertTrue(type.getDisasters().isEmpty());
    }

    @Test
    public void TI34_getProductionAfterContainerInitialization() {
        TileImprovementType type = createType();

        type.getTileTypeChanges();

        assertNull(type.getProduction(null));
    }

    @Test
    public void TI35_getChangeAfterContainerInitialization() {
        TileImprovementType type = createType();

        type.getTileTypeChanges();

        assertNull(type.getChange(null));
    }

    @Test
    public void TI36_disasterStreamCanBeConsumed() {
        TileImprovementType type = createType();

        long count = type.getDisasterChoices().count();

        assertEquals(0L, count);
    }
}
