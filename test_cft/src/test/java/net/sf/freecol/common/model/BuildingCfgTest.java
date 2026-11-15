package net.sf.freecol.common.model;

import java.util.Collections;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.BlockJUnit4ClassRunner;

import net.sf.freecol.common.model.UnitLocation.NoAddReason;
import net.sf.freecol.server.model.ServerBuilding;
import net.sf.freecol.server.model.ServerUnit;
import net.sf.freecol.util.test.MockModelTestCase;


@RunWith(BlockJUnit4ClassRunner.class)
public class BuildingCfgTest extends MockModelTestCase {

    @Test
    public void testWorkplaceTypeAndAvailability() {
        Game game = getStandardGame();
        game.changeMap(getTestMap());
        Colony colony = createStandardColony();
        BuildingType buildingType
            = spec().getBuildingType("model.building.carpenterHouse");
        Building building = new ServerBuilding(game, colony, buildingType);
        colony.addBuilding(building);

        assertSame(buildingType, building.getType());
        assertEquals(buildingType.getWorkPlaces(), building.getUnitCapacity());
        assertTrue(building.isAvailable());
        assertTrue(building.isCurrent());
        assertEquals(!buildingType.isAutomaticBuild()
                     && !colony.isAutomaticBuild(buildingType),
                     building.canBeDamaged());

        UnitType allowed = null;
        UnitType disallowed = null;
        for (UnitType unitType : spec().getUnitTypeList()) {
            if (building.canAddType(unitType)) {
                if (allowed == null) allowed = unitType;
            } else if (disallowed == null) {
                disallowed = unitType;
            }
        }
        assertNotNull(allowed);
        assertNotNull(disallowed);
        assertTrue(building.canAddType(allowed));
        assertFalse(building.canAddType(disallowed));

        Unit unit = new ServerUnit(game, null,
            colony.getOwner(), allowed);
        assertTrue(building.goodSuggestionCheck(allowed, unit,
            spec().getPrimaryFoodType()));
        assertFalse(building.goodSuggestionCheck(disallowed, unit,
            spec().getPrimaryFoodType()));
    }

    @Test
    public void testUpgradeAndDowngradeRespectBuildability() {
        Game game = getStandardGame();
        game.changeMap(getTestMap());
        Colony colony = createStandardColony();
        BuildingType carpenterHouseType
            = spec().getBuildingType("model.building.carpenterHouse");
        Building building = new ServerBuilding(game, colony,
                                                carpenterHouseType);
        colony.addBuilding(building);

        assertFalse(building.canBuildNext());
        assertNull(building.upgrade());
        assertSame(carpenterHouseType, building.getType());
        assertNull(building.downgrade());

        BuildingType lumberMillType
            = spec().getBuildingType("model.building.lumberMill");
        Building lumberMill = new ServerBuilding(game, colony, lumberMillType);
        colony.addBuilding(lumberMill);
        assertTrue(lumberMill.canBeDamaged());
        assertNotNull(lumberMill.downgrade());
        assertSame(carpenterHouseType, lumberMill.getType());
    }

    @Test
    public void testBuildingUpgradeSucceedsWhenRequiredGoodsAreAvailable() {
        Game game = getStandardGame();
        game.changeMap(getTestMap());
        Colony colony = createStandardColony(4);
        BuildingType carpenterHouseType = spec().getBuildingType("model.building.carpenterHouse");
        Building carpenterHouse = new ServerBuilding(game, colony, carpenterHouseType);
        colony.addBuilding(carpenterHouse);
        GoodsType hammers = spec().getGoodsType("model.goods.hammers");
        colony.addGoods(hammers, 52);

        assertTrue(carpenterHouse.canBuildNext());
        assertNotNull(carpenterHouse.upgrade());
        
        BuildingType expectedUpgradedType = carpenterHouseType.getUpgradesTo();
        assertSame(expectedUpgradedType, carpenterHouse.getType());
    }

    @Test
    public void testProductionAndProductionModifiersControlFlow() {
        Game game = getStandardGame();
        game.changeMap(getTestMap());
        Colony colony = createStandardColony(3);
        BuildingType carpenterHouseType = spec().getBuildingType("model.building.carpenterHouse");
        Building building = new ServerBuilding(game, colony, carpenterHouseType);
        colony.addBuilding(building);

        GoodsType hammers = spec().getGoodsType("model.goods.hammers");
        GoodsType lumber = spec().getGoodsType("model.goods.lumber");
        UnitType expertCarpenter = spec().getUnitType("model.unit.expertCarpenter");

        assertFalse(building.canProduce(null, expertCarpenter));
        assertFalse(building.canProduce(lumber, expertCarpenter));
        assertTrue(building.getBaseProduction(null, hammers, expertCarpenter) >= 0);

        // Production modifiers with unitType != null
        assertNotNull(building.getProductionModifiers(hammers, expertCarpenter));

        // Production modifiers with unitType == null (building-only modifiers)
        assertNotNull(building.getProductionModifiers(hammers, null));

        // Production modifiers with goodsType == null
        assertNotNull(building.getProductionModifiers(null, null));
    }

    @Test
    public void testAvailableProductionTypesAndConsumerProperties() {
        Game game = getStandardGame();
        game.changeMap(getTestMap());
        Colony colony = createStandardColony(2);
        BuildingType blacksmithHouseType = spec().getBuildingType("model.building.blacksmithHouse");
        Building building = new ServerBuilding(game, colony, blacksmithHouseType);
        colony.addBuilding(building);

        List<ProductionType> unattendedTypes = building.getAvailableProductionTypes(true);
        assertNotNull(unattendedTypes);

        List<ProductionType> attendedTypes = building.getAvailableProductionTypes(false);
        assertNotNull(attendedTypes);

        assertTrue(building.getPriority() >= 0);
        assertNotNull(building.getConsumedGoods());
        assertNotNull(building.getConsumptionModifiers("model.goods.tools"));
        assertEquals(blacksmithHouseType.getNameKey(), building.getNameKey());
    }

    @Test
    public void testSuggestionCheckBranches() {
        Game game = getStandardGame();
        game.changeMap(getTestMap());
        Colony colony = createStandardColony(4);
        BuildingType carpenterHouseType = spec().getBuildingType("model.building.carpenterHouse");
        Building building = new ServerBuilding(game, colony, carpenterHouseType);
        colony.addBuilding(building);

        UnitType colonistType = spec().getUnitType("model.unit.freeColonist");
        GoodsType hammers = spec().getGoodsType("model.goods.hammers");
        GoodsType bells = spec().getGoodsType("model.goods.bells");

        // Subcase 1: unit != null -> returns true
        Unit unit = new ServerUnit(game, null, colony.getOwner(), colonistType);
        assertTrue(building.goodSuggestionCheck(colonistType, unit, hammers));

        // Subcase 2: unit == null, level == 1, but colony builds something requiring goods
        colony.setCurrentlyBuilding(spec().getBuildingType("model.building.lumberMill"));
        assertTrue(building.goodSuggestionCheck(colonistType, null, hammers));

        // Subcase 3: unit == null, but goods is not required by current buildable
        assertFalse(building.goodSuggestionCheck(colonistType, null, bells));
    }

    @Test
    public void testLocationHierarchyAndStringFormatting() {
        Game game = getStandardGame();
        game.changeMap(getTestMap());
        Colony colony = createStandardColony(2);
        BuildingType churchType = spec().getBuildingType("model.building.church");
        Building building = new ServerBuilding(game, colony, churchType);
        colony.addBuilding(building);

        assertSame(colony, building.up());
        assertNull(building.getWorkTile());
        assertNotNull(building.getLocationLabel());
        assertEquals(churchType.getLevel(), building.getLevel());
        assertEquals(churchType.getCompetenceFactor(), building.getCompetenceFactor(), 0.001f);
        assertEquals(churchType.getRebelFactor(), building.getRebelFactor(), 0.001f);
        assertSame(churchType, building.getDisplayObject());

        String shortString = building.toShortString();
        assertTrue(shortString.contains(colony.getName()));

        String fullString = building.toString();
        assertTrue(fullString.contains(colony.getName()));
    }

    @Test
    public void testCopyInAndNoAddReason() {
        Game game = getStandardGame();
        game.changeMap(getTestMap());
        Colony colony = createStandardColony(2);
        BuildingType churchType = spec().getBuildingType("model.building.church");
        Building building = new ServerBuilding(game, colony, churchType);
        colony.addBuilding(building);

        // CopyIn failure branches
        assertFalse(building.copyIn(null));
        assertFalse(building.copyIn(colony));

        // CopyIn success branch
        Game game2 = getStandardGame();
        Building copyTarget = new ServerBuilding(game2, building.getId());
        assertTrue(copyTarget.copyIn(building));
        assertSame(churchType, copyTarget.getType());

        // NoAddReason
        Goods goods = new Goods(game, null, spec().getPrimaryFoodType(), 10);
        assertEquals(NoAddReason.WRONG_TYPE, building.getNoAddReason(goods));
        Unit colonist = new ServerUnit(game, null, colony.getOwner(),
            spec().getUnitType("model.unit.freeColonist"));
        assertEquals(NoAddReason.NONE, building.getNoAddReason(colonist));
    }
}
