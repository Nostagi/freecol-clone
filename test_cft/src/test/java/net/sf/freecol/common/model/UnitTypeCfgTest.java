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
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import javax.swing.DefaultListModel;
import javax.swing.JList;

import net.sf.freecol.common.io.FreeColXMLReader;
import net.sf.freecol.common.io.FreeColXMLWriter;
import net.sf.freecol.server.model.ServerBuilding;
import net.sf.freecol.util.test.FreeColTestCase;


/**
 * CFG-based test suite (C2 branch/edge coverage) for {@link UnitType}.
 *
 * Covers all 69 CFG methods in {@code UnitType.json} against
 * {@code data/rules/classic/specification.xml}.
 * Note: in M57 ({@code copyIn}), edge {@code N1 -> N2} ({@code B2:false},
 * non-null {@code UnitType} failing {@code super.copyIn}) is infeasible
 * because {@code UnitType} is {@code final} and {@code BuildableType},
 * {@code FreeColSpecObjectType}, and {@code FreeColObject.copyIn} always
 * return {@code true} for any non-null {@code UnitType}.
 */
public class UnitTypeCfgTest extends FreeColTestCase {

    // Cached unit types from classic specification
    private UnitType colonistType() {
        return spec().getUnitType("model.unit.freeColonist");
    }

    private UnitType artilleryType() {
        return spec().getUnitType("model.unit.artillery");
    }

    private UnitType indianConvertType() {
        return spec().getUnitType("model.unit.indianConvert");
    }

    private UnitType caravelType() {
        return spec().getUnitType("model.unit.caravel");
    }

    private UnitType manOWarType() {
        return spec().getUnitType("model.unit.manOWar");
    }

    private UnitType expertFarmerType() {
        return spec().getUnitType("model.unit.expertFarmer");
    }

    private UnitType colonialRegularType() {
        return spec().getUnitType("model.unit.colonialRegular");
    }

    private UnitType veteranSoldierType() {
        return spec().getUnitType("model.unit.veteranSoldier");
    }

    private UnitType jesuitMissionaryType() {
        return spec().getUnitType("model.unit.jesuitMissionary");
    }

    private UnitType treasureTrainType() {
        return spec().getUnitType("model.unit.treasureTrain");
    }

    private UnitType wagonTrainType() {
        return spec().getUnitType("model.unit.wagonTrain");
    }

    private UnitType indenturedServantType() {
        return spec().getUnitType("model.unit.indenturedServant");
    }

    private UnitType pettyCriminalType() {
        return spec().getUnitType("model.unit.pettyCriminal");
    }

    private UnitType frigateType() {
        return spec().getUnitType("model.unit.frigate");
    }

    private GoodsType foodType() {
        return spec().getGoodsType("model.goods.food");
    }

    private GoodsType grainType() {
        return spec().getGoodsType("model.goods.grain");
    }

    private GoodsType bellsType() {
        return spec().getGoodsType("model.goods.bells");
    }

    private BuildingType armoryType() {
        return spec().getBuildingType("model.building.armory");
    }

    private BuildingType carpenterHouseType() {
        return spec().getBuildingType("model.building.carpenterHouse");
    }

    private JList<BuildableType> buildQueue(BuildableType... items) {
        DefaultListModel<BuildableType> model = new DefaultListModel<>();
        for (BuildableType item : items) {
            model.addElement(item);
        }
        return new JList<>(model);
    }

    private Colony createColonyWithGame(boolean withArmory) {
        Game game = getStandardGame();
        game.changeMap(getTestMap(true));
        Colony colony = createStandardColony();
        if (withArmory) {
            colony.addBuilding(new ServerBuilding(game, colony, armoryType()));
        }
        return colony;
    }


    // ------------------------------------------------------------------
    // M1: UnitType(String id, Specification specification)
    // ------------------------------------------------------------------

    // M1-TP1: ENTRY -> N0 -> EXIT
    // Pre-condition: Valid spec and non-null id
    public void testM1_TP1_constructorInitializesDefaultRole() {
        UnitType custom = new UnitType("test.unit.custom", spec());
        assertEquals("test.unit.custom", custom.getId());
        assertEquals(spec().getDefaultRole(), custom.getDefaultRole());
    }


    // ------------------------------------------------------------------
    // M2..M8: Basic identity & combat getters
    // ------------------------------------------------------------------

    // M2-TP1: ENTRY -> N0 -> EXIT
    // Pre-condition: freeColonist unit type from classic specification
    public void testM2_TP1_getWorkingAsKey() {
        assertEquals("model.unit.freeColonist.workingAs",
                     colonistType().getWorkingAsKey());
    }

    // M3-TP1: ENTRY -> N0 -> EXIT
    // Pre-condition: caravel has carryUnits ability; freeColonist does not
    public void testM3_TP1_canCarryUnits() {
        assertTrue(caravelType().canCarryUnits());
        assertFalse(colonistType().canCarryUnits());
    }

    // M4-TP1: ENTRY -> N0 -> EXIT
    // Pre-condition: caravel has carryGoods ability
    public void testM4_TP1_canCarryGoods() {
        assertTrue(caravelType().canCarryGoods());
        assertFalse(colonistType().canCarryGoods());
    }

    // M5-TP1: ENTRY -> N0 -> EXIT
    // Pre-condition: treasureTrain has carryTreasure ability
    public void testM5_TP1_canCarryTreasure() {
        assertTrue(treasureTrainType().canCarryTreasure());
        assertFalse(colonistType().canCarryTreasure());
    }

    // M6-TP1: ENTRY -> N0 -> EXIT
    // Pre-condition: freeColonist has score-value=3
    public void testM6_TP1_getScoreValue() {
        assertEquals(3, colonistType().getScoreValue());
    }

    // M7-TP1: ENTRY -> N0 -> EXIT
    // Pre-condition: artillery has baseOffence=7
    public void testM7_TP1_getBaseOffence() {
        assertEquals(7, artilleryType().getBaseOffence());
    }

    // M8-TP1: ENTRY -> N0 -> EXIT
    // Pre-condition: artillery has offence=7.0
    public void testM8_TP1_getOffence() {
        assertEquals(7.0, artilleryType().getOffence(), 0.001);
    }


    // ------------------------------------------------------------------
    // M9: isOffensive()
    // ------------------------------------------------------------------

    // M9-TP1: ENTRY -> N0 -> B1:true -> N2 -> N3 -> EXIT (baseOffence <= 0)
    // Pre-condition: freeColonist has offence=0
    public void testM9_TP1_isOffensive_false() {
        assertFalse(colonistType().isOffensive());
    }

    // M9-TP2: ENTRY -> N0 -> B1:false -> N1 -> N3 -> EXIT (baseOffence > 0)
    // Pre-condition: artillery has offence=7
    public void testM9_TP2_isOffensive_true() {
        assertTrue(artilleryType().isOffensive());
    }


    // ------------------------------------------------------------------
    // M10..M11: Defence getters
    // ------------------------------------------------------------------

    // M10-TP1: ENTRY -> N0 -> EXIT
    // Pre-condition: artillery has baseDefence=5
    public void testM10_TP1_getBaseDefence() {
        assertEquals(5, artilleryType().getBaseDefence());
    }

    // M11-TP1: ENTRY -> N0 -> EXIT
    // Pre-condition: artillery has defence=5.0
    public void testM11_TP1_getDefence() {
        assertEquals(5.0, artilleryType().getDefence(), 0.001);
    }


    // ------------------------------------------------------------------
    // M12: isDefensive()
    // ------------------------------------------------------------------

    // M12-TP1: ENTRY -> N0 -> B1:true -> N2 -> N3 -> EXIT (baseDefence <= 1)
    // Pre-condition: freeColonist has defence=1
    public void testM12_TP1_isDefensive_false() {
        assertFalse(colonistType().isDefensive());
    }

    // M12-TP2: ENTRY -> N0 -> B1:false -> N1 -> N3 -> EXIT (baseDefence > 1)
    // Pre-condition: artillery has defence=5
    public void testM12_TP2_isDefensive_true() {
        assertTrue(artilleryType().isDefensive());
    }


    // ------------------------------------------------------------------
    // M13..M21: Default unit flag & size/sight/hp getters/setters
    // ------------------------------------------------------------------

    // M13-TP1: ENTRY -> N0 -> EXIT
    // Pre-condition: freeColonist is default unit type; artillery is not
    public void testM13_TP1_isDefaultUnitType() {
        assertTrue(colonistType().isDefaultUnitType());
        assertFalse(artilleryType().isDefaultUnitType());
    }

    // M14-TP1: ENTRY -> N0 -> EXIT
    // Pre-condition: frigate has line-of-sight=2
    public void testM14_TP1_getLineOfSight() {
        assertEquals(2, frigateType().getLineOfSight());
    }

    // M15-TP1: ENTRY -> N0 -> EXIT
    // Pre-condition: fresh UnitType instance
    public void testM15_TP1_setLineOfSight() {
        UnitType custom = new UnitType("test.los", spec());
        custom.setLineOfSight(4);
        assertEquals(4, custom.getLineOfSight());
    }

    // M16-TP1: ENTRY -> N0 -> EXIT
    // Pre-condition: caravel has space=2
    public void testM16_TP1_getSpace() {
        assertEquals(2, caravelType().getSpace());
    }

    // M17-TP1: ENTRY -> N0 -> EXIT
    // Pre-condition: fresh UnitType instance
    public void testM17_TP1_setSpace() {
        UnitType custom = new UnitType("test.space", spec());
        custom.setSpace(8);
        assertEquals(8, custom.getSpace());
    }

    // M18-TP1: ENTRY -> N0 -> EXIT
    // Pre-condition: artillery has hit-points=6
    public void testM18_TP1_getHitPoints() {
        assertEquals(6, artilleryType().getHitPoints());
    }

    // M19-TP1: ENTRY -> N0 -> EXIT
    // Pre-condition: fresh UnitType instance
    public void testM19_TP1_setHitPoints() {
        UnitType custom = new UnitType("test.hp", spec());
        custom.setHitPoints(10);
        assertEquals(10, custom.getHitPoints());
    }

    // M20-TP1: ENTRY -> N0 -> EXIT
    // Pre-condition: wagonTrain has spaceTaken=12
    public void testM20_TP1_getSpaceTaken() {
        assertEquals(12, wagonTrainType().getSpaceTaken());
    }

    // M21-TP1: ENTRY -> N0 -> EXIT
    // Pre-condition: fresh UnitType instance
    public void testM21_TP1_setSpaceTaken() {
        UnitType custom = new UnitType("test.spaceTaken", spec());
        custom.setSpaceTaken(16);
        assertEquals(16, custom.getSpaceTaken());
    }


    // ------------------------------------------------------------------
    // M22: isRecruitable()
    // ------------------------------------------------------------------

    // M22-TP1: ENTRY -> N0 -> B1:true -> N2 -> N3 -> EXIT (recruitProbability <= 0)
    // Pre-condition: artillery has default recruitProbability=0
    public void testM22_TP1_isRecruitable_false() {
        assertFalse(artilleryType().isRecruitable());
    }

    // M22-TP2: ENTRY -> N0 -> B1:false -> N1 -> N3 -> EXIT (recruitProbability > 0)
    // Pre-condition: freeColonist has recruit-probability=20
    public void testM22_TP2_isRecruitable_true() {
        assertTrue(colonistType().isRecruitable());
    }


    // ------------------------------------------------------------------
    // M23..M30: Recruit, skill, price, movement, experience
    // ------------------------------------------------------------------

    // M23-TP1: ENTRY -> N0 -> EXIT
    // Pre-condition: freeColonist has recruit-probability=20
    public void testM23_TP1_getRecruitProbability() {
        assertEquals(20, colonistType().getRecruitProbability());
    }

    // M24-TP1: ENTRY -> N0 -> EXIT
    // Pre-condition: expertFarmer has skill=1
    public void testM24_TP1_getSkill() {
        assertEquals(1, expertFarmerType().getSkill());
    }

    // M25-TP1: ENTRY -> N0 -> EXIT
    // Pre-condition: fresh UnitType instance
    public void testM25_TP1_setSkill() {
        UnitType custom = new UnitType("test.skill", spec());
        custom.setSkill(2);
        assertEquals(2, custom.getSkill());
    }

    // M26-TP1: ENTRY -> N0 -> EXIT
    // Pre-condition: artillery has price=500
    public void testM26_TP1_getPrice() {
        assertEquals(500, artilleryType().getPrice());
    }

    // M27-TP1: ENTRY -> N0 -> EXIT
    // Pre-condition: manOWar has mercenary-price=10000
    public void testM27_TP1_getMercenaryPrice() {
        assertEquals(10000, manOWarType().getMercenaryPrice());
    }

    // M28-TP1: ENTRY -> N0 -> EXIT
    // Pre-condition: fresh UnitType instance
    public void testM28_TP1_setMercenaryPrice() {
        UnitType custom = new UnitType("test.merc", spec());
        custom.setMercenaryPrice(4500);
        assertEquals(4500, custom.getMercenaryPrice());
    }

    // M29-TP1: ENTRY -> N0 -> EXIT
    // Pre-condition: freeColonist inherits movement=3 from colonist
    public void testM29_TP1_getMovement() {
        assertEquals(3, colonistType().getMovement());
    }

    // M30-TP1: ENTRY -> N0 -> EXIT
    // Pre-condition: freeColonist has maximum-experience=200
    public void testM30_TP1_getMaximumExperience() {
        assertEquals(200, colonistType().getMaximumExperience());
    }


    // ------------------------------------------------------------------
    // M31: hasMaximumAttrition()
    // ------------------------------------------------------------------

    // M31-TP1: ENTRY -> N0 -> B1:true -> N2 -> N3 -> EXIT (maximumAttrition == INFINITY)
    // Pre-condition: freeColonist has default maximumAttrition=INFINITY
    public void testM31_TP1_hasMaximumAttrition_false() {
        assertFalse(colonistType().hasMaximumAttrition());
    }

    // M31-TP2: ENTRY -> N0 -> B1:false -> N1 -> N3 -> EXIT (maximumAttrition != INFINITY)
    // Pre-condition: indianConvert has maximum-attrition=8
    public void testM31_TP2_hasMaximumAttrition_true() {
        assertTrue(indianConvertType().hasMaximumAttrition());
    }


    // ------------------------------------------------------------------
    // M32..M36: Attrition, production, teaching, role getters
    // ------------------------------------------------------------------

    // M32-TP1: ENTRY -> N0 -> EXIT
    // Pre-condition: indianConvert has maximum-attrition=8
    public void testM32_TP1_getMaximumAttrition() {
        assertEquals(8, indianConvertType().getMaximumAttrition());
    }

    // M33-TP1: ENTRY -> N0 -> EXIT
    // Pre-condition: expertFarmer has expert-production=model.goods.grain
    public void testM33_TP1_getExpertProduction() {
        assertEquals(grainType(), expertFarmerType().getExpertProduction());
    }

    // M34-TP1: ENTRY -> N0 -> EXIT
    // Pre-condition: colonialRegular has skill-taught=model.unit.veteranSoldier
    public void testM34_TP1_getSkillTaught() {
        assertEquals(veteranSoldierType(), colonialRegularType().getSkillTaught());
    }

    // M35-TP1: ENTRY -> N0 -> EXIT
    // Pre-condition: jesuitMissionary has default-role=model.role.missionary
    public void testM35_TP1_getDefaultRole() {
        assertEquals("model.role.missionary",
                     jesuitMissionaryType().getDefaultRole().getId());
    }

    // M36-TP1: ENTRY -> N0 -> EXIT
    // Pre-condition: veteranSoldier is expert-unit for soldier and dragoon roles
    public void testM36_TP1_getExpertRoles() {
        List<Role> roles = veteranSoldierType().getExpertRoles();
        assertFalse(roles.isEmpty());
        assertTrue(roles.contains(spec().getRole("model.role.soldier")));
    }


    // ------------------------------------------------------------------
    // M37: getDisplayRoleId()
    // ------------------------------------------------------------------

    // M37-TP1: ENTRY -> N0 -> B1:true -> N2 -> N3 -> EXIT (first(getExpertRoles()) == null)
    // Pre-condition: artillery has no expert roles
    public void testM37_TP1_getDisplayRoleId_defaultRole() {
        assertEquals(Specification.DEFAULT_ROLE_ID, artilleryType().getDisplayRoleId());
    }

    // M37-TP2: ENTRY -> N0 -> B1:false -> N1 -> N3 -> EXIT (first(getExpertRoles()) != null)
    // Pre-condition: veteranSoldier has expert roles
    public void testM37_TP2_getDisplayRoleId_expertRole() {
        assertEquals("model.role.soldier", veteranSoldierType().getDisplayRoleId());
    }


    // ------------------------------------------------------------------
    // M38: getTeachingType(UnitType teacherType)
    // ------------------------------------------------------------------

    // M38-TP1: ENTRY -> N0 -> B1:false -> N1 -> EXIT
    // Pre-condition: student skill (1) >= taughtLevel (1)
    public void testM38_TP1_getTeachingType_alreadyAtOrAboveTeacherLevel() {
        assertNull(expertFarmerType().getTeachingType(expertFarmerType()));
    }

    // M38-TP2: ENTRY -> N0 -> B1:true -> N2 -> N3 -> B2:false -> N4 -> B3:false -> N5 -> EXIT
    // Pre-condition: freeColonist (skill=0) upgrades directly to expertFarmer (skill=1)
    public void testM38_TP2_getTeachingType_directMatch() {
        assertEquals(expertFarmerType(),
                     colonistType().getTeachingType(expertFarmerType()));
    }

    // M38-TP3: ENTRY -> N0 -> B1:true -> N2 -> N3 -> B2:false -> N4 -> B3:true -> N6
    //          -> B4:true -> N8 -> N3 -> B2:true -> N9 -> N10 -> B5:true -> N14 -> EXIT
    // Pre-condition: pettyCriminal (-2) -> indenturedServant (-1); wagonTrain skill=UNDEFINED (-1)
    public void testM38_TP3_getTeachingType_candidateNotBelowTaughtLevel() {
        assertNull(pettyCriminalType().getTeachingType(wagonTrainType()));
    }

    // M38-TP4: ENTRY -> N0 -> B1:true -> N2 -> N3 -> B2:false -> N4 -> B3:true -> N6
    //          -> B4:false -> N7 -> N8 -> N3 -> B2:true -> N9 -> N10 -> B5:false -> N11 -> B6:false -> N12 -> EXIT
    // Pre-condition: indenturedServant (-1) -> freeColonist (0) -> expertFarmer (1)
    public void testM38_TP4_getTeachingType_indirectStepMatch() {
        assertEquals(colonistType(),
                     indenturedServantType().getTeachingType(expertFarmerType()));
    }

    // M38-TP5: ENTRY -> N0 -> B1:true -> N2 -> N3 -> B2:false -> N4 -> B3:true -> N6
    //          -> B4:false -> N7 -> N8 -> N3 -> B2:true -> N9 -> N10 -> B5:false -> N11 -> B6:true -> N13
    //          -> N10 -> B5:true -> N14 -> EXIT
    // Pre-condition: temporary education link student (-1) -> mid (0) with no path to colonialRegular (3)
    public void testM38_TP5_getTeachingType_recursiveCandidateReturnsNull() {
        UnitType student = new UnitType("test.teach.student", spec());
        student.setSkill(-1);
        UnitType mid = new UnitType("test.teach.mid", spec());
        mid.setSkill(0);

        UnitTypeChange uc = new UnitTypeChange("test.uc.studentToMid", spec());
        uc.from = student;
        uc.to = mid;
        uc.probability = 100;

        UnitChangeType edu = spec().getUnitChangeType(UnitChangeType.EDUCATION);
        edu.addUnitTypeChange(uc);
        try {
            assertNull(student.getTeachingType(colonialRegularType()));
        } finally {
            edu.deleteUnitChanges(student);
        }
    }


    // ------------------------------------------------------------------
    // M39..M42: Ability predicate getters
    // ------------------------------------------------------------------

    // M39-TP1: ENTRY -> N0 -> EXIT
    // Pre-condition: caravel has navalUnit ability
    public void testM39_TP1_isNaval() {
        assertTrue(caravelType().isNaval());
        assertFalse(colonistType().isNaval());
    }

    // M40-TP1: ENTRY -> N0 -> EXIT
    // Pre-condition: freeColonist has person ability
    public void testM40_TP1_isPerson() {
        assertTrue(colonistType().isPerson());
        assertFalse(caravelType().isPerson());
    }

    // M41-TP1: ENTRY -> N0 -> EXIT
    // Pre-condition: freeColonist has foundColony ability
    public void testM41_TP1_canBuildColony() {
        assertTrue(colonistType().canBuildColony());
        assertFalse(caravelType().canBuildColony());
    }

    // M42-TP1: ENTRY -> N0 -> EXIT
    // Pre-condition: caravel has moveToEurope ability
    public void testM42_TP1_canMoveToHighSeas() {
        assertTrue(caravelType().canMoveToHighSeas());
        assertFalse(colonistType().canMoveToHighSeas());
    }


    // ------------------------------------------------------------------
    // M43: hasSkill()
    // ------------------------------------------------------------------

    // M43-TP1: ENTRY -> N0 -> B1:true -> N2 -> N3 -> EXIT (skill == UNDEFINED)
    // Pre-condition: artillery has no skill (UNDEFINED)
    public void testM43_TP1_hasSkill_false() {
        assertFalse(artilleryType().hasSkill());
    }

    // M43-TP2: ENTRY -> N0 -> B1:false -> N1 -> N3 -> EXIT (skill != UNDEFINED)
    // Pre-condition: freeColonist has skill=0
    public void testM43_TP2_hasSkill_true() {
        assertTrue(colonistType().hasSkill());
    }


    // ------------------------------------------------------------------
    // M44: hasPrice()
    // ------------------------------------------------------------------

    // M44-TP1: ENTRY -> N0 -> B1:true -> N2 -> N3 -> EXIT (price == UNDEFINED)
    // Pre-condition: freeColonist has no Europe purchase price (UNDEFINED)
    public void testM44_TP1_hasPrice_false() {
        assertFalse(colonistType().hasPrice());
    }

    // M44-TP2: ENTRY -> N0 -> B1:false -> N1 -> N3 -> EXIT (price != UNDEFINED)
    // Pre-condition: artillery has price=500
    public void testM44_TP2_hasPrice_true() {
        assertTrue(artilleryType().hasPrice());
    }


    // ------------------------------------------------------------------
    // M45..M46: Protected consumption container accessors
    // ------------------------------------------------------------------

    // M45-TP1: ENTRY -> N0 -> EXIT
    // Pre-condition: freeColonist has consumption map (food=2, bells=1)
    public void testM45_TP1_getConsumption() {
        TypeCountMap<GoodsType> consumption = colonistType().getConsumption();
        assertNotNull(consumption);
        assertEquals(2, consumption.getCount(foodType()));
    }

    // M46-TP1: ENTRY -> N0 -> EXIT
    // Pre-condition: fresh UnitType and populated TypeCountMap
    public void testM46_TP1_setConsumption() {
        UnitType custom = new UnitType("test.setCons", spec());
        TypeCountMap<GoodsType> map = new TypeCountMap<>();
        map.incrementCount(foodType(), 3);
        custom.setConsumption(map);
        assertSame(map, custom.getConsumption());
    }


    // ------------------------------------------------------------------
    // M47: getConsumptionOf(GoodsType goodsType)
    // ------------------------------------------------------------------

    // M47-TP1: ENTRY -> N0 -> B1:false -> N1 -> N3 -> EXIT (consumption == null)
    // Pre-condition: caravel has consumption == null
    public void testM47_TP1_getConsumptionOf_nullMap() {
        assertEquals(0, caravelType().getConsumptionOf(foodType()));
    }

    // M47-TP2: ENTRY -> N0 -> B1:true -> N2 -> N3 -> EXIT (consumption != null)
    // Pre-condition: freeColonist consumes 2 food
    public void testM47_TP2_getConsumptionOf_presentMap() {
        assertEquals(2, colonistType().getConsumptionOf(foodType()));
    }


    // ------------------------------------------------------------------
    // M48: addConsumption(GoodsType goodsType, int amount) [private]
    // ------------------------------------------------------------------

    // M48-TP1: ENTRY -> N0 -> B1:false -> N1 -> N2 -> EXIT (consumption == null)
    // Pre-condition: fresh UnitType with consumption == null
    public void testM48_TP1_addConsumption_initializesMap() throws Exception {
        UnitType custom = new UnitType("test.addCons1", spec());
        Method m = UnitType.class.getDeclaredMethod("addConsumption", GoodsType.class, int.class);
        m.setAccessible(true);
        m.invoke(custom, foodType(), 2);
        assertEquals(2, custom.getConsumptionOf(foodType()));
    }

    // M48-TP2: ENTRY -> N0 -> B1:true -> N2 -> EXIT (consumption != null)
    // Pre-condition: custom UnitType already has non-null consumption map
    public void testM48_TP2_addConsumption_appendsExistingMap() throws Exception {
        UnitType custom = new UnitType("test.addCons2", spec());
        Method m = UnitType.class.getDeclaredMethod("addConsumption", GoodsType.class, int.class);
        m.setAccessible(true);
        m.invoke(custom, foodType(), 2);
        m.invoke(custom, bellsType(), 1);
        assertEquals(2, custom.getConsumptionOf(foodType()));
        assertEquals(1, custom.getConsumptionOf(bellsType()));
    }


    // ------------------------------------------------------------------
    // M49: canBeBuiltInColony(Colony, List<BuildableType>)
    // ------------------------------------------------------------------

    // M49-TP1: ENTRY -> N0 -> B1:true -> N4 -> EXIT
    // Pre-condition: freeColonist has PERSON ability
    public void testM49_TP1_canBeBuiltInColony_personUnit() {
        Colony colony = createColonyWithGame(false);
        assertEquals(Colony.NoBuildReason.NONE,
                     colonistType().canBeBuiltInColony(colony, Collections.emptyList()));
    }

    // M49-TP2: ENTRY -> N0 -> B1:false -> N1 -> B2:true -> N4 -> EXIT
    // Pre-condition: artillery (non-person); colony has armory (Ability.BUILD for artillery)
    public void testM49_TP2_canBeBuiltInColony_colonyHasBuildAbility() {
        Colony colony = createColonyWithGame(true);
        assertEquals(Colony.NoBuildReason.NONE,
                     artilleryType().canBeBuiltInColony(colony, Collections.emptyList()));
    }

    // M49-TP3: ENTRY -> N0 -> B1:false -> N1 -> B2:false -> N2 -> B3:true -> N4 -> EXIT
    // (Also covers M68 lambda$canBeBuiltInColony$0)
    // Pre-condition: colony lacks armory, assumeBuilt includes armory
    public void testM49_TP3_canBeBuiltInColony_assumedBuiltHasAbility() {
        Colony colony = createColonyWithGame(false);
        List<BuildableType> assumed = List.of(carpenterHouseType(), armoryType());
        assertEquals(Colony.NoBuildReason.NONE,
                     artilleryType().canBeBuiltInColony(colony, assumed));
    }

    // M49-TP4: ENTRY -> N0 -> B1:false -> N1 -> B2:false -> N2 -> B3:false -> N3 -> EXIT
    // Pre-condition: colony lacks armory and assumeBuilt does not grant BUILD for artillery
    public void testM49_TP4_canBeBuiltInColony_missingBuildAbility() {
        Colony colony = createColonyWithGame(false);
        assertEquals(Colony.NoBuildReason.MISSING_BUILD_ABILITY,
                     artilleryType().canBeBuiltInColony(colony, Collections.emptyList()));
    }


    // ------------------------------------------------------------------
    // M50: getMinimumIndex(Colony, JList<BuildableType>, int UNABLE_TO_BUILD)
    // ------------------------------------------------------------------

    // M50-TP1: ENTRY -> N0 -> B1:false -> N1 -> EXIT
    // Pre-condition: colony can build artillery (has armory)
    public void testM50_TP1_getMinimumIndex_canBuildInColony() {
        Colony colony = createColonyWithGame(true);
        assertEquals(0, artilleryType().getMinimumIndex(colony, buildQueue(), -1));
    }

    // M50-TP2: ENTRY -> N0 -> B1:true -> N2 -> N3 -> B2:true -> N7 -> EXIT
    // Pre-condition: colony cannot build artillery (no armory), buildQueue is empty
    public void testM50_TP2_getMinimumIndex_emptyQueueUnableToBuild() {
        Colony colony = createColonyWithGame(false);
        assertEquals(-1, artilleryType().getMinimumIndex(colony, buildQueue(), -1));
    }

    // M50-TP3: ENTRY -> N0 -> B1:true -> N2 -> N3 -> B2:false -> N4 -> B3:false -> N5 -> EXIT
    // Pre-condition: colony lacks armory; first queue element is armory
    public void testM50_TP3_getMinimumIndex_firstQueueElementMatches() {
        Colony colony = createColonyWithGame(false);
        JList<BuildableType> queue = buildQueue(armoryType());
        assertEquals(1, artilleryType().getMinimumIndex(colony, queue, -1));
    }

    // M50-TP4: ENTRY -> N0 -> B1:true -> N2 -> N3 -> B2:false -> N4 -> B3:true -> N6
    //          -> N3 -> B2:false -> N4 -> B3:false -> N5 -> EXIT
    // Pre-condition: colony lacks armory; buildQueue has [carpenterHouse, armory] (covers loop back-edge)
    public void testM50_TP4_getMinimumIndex_queueWithNonMatchingThenProvider() {
        Colony colony = createColonyWithGame(false);
        JList<BuildableType> queue = buildQueue(carpenterHouseType(), armoryType());
        assertEquals(2, artilleryType().getMinimumIndex(colony, queue, -1));
    }


    // ------------------------------------------------------------------
    // M51: getMaximumIndex(Colony, JList<BuildableType>, int UNABLE_TO_BUILD)
    // ------------------------------------------------------------------

    // M51-TP1: ENTRY -> N0 -> B1:false -> N1 -> N2 -> B2:false -> N3 -> EXIT
    // Pre-condition: colony can build artillery (has armory); queue size 1
    public void testM51_TP1_getMaximumIndex_canBuildInColony() {
        Colony colony = createColonyWithGame(true);
        JList<BuildableType> queue = buildQueue(carpenterHouseType());
        assertEquals(1, artilleryType().getMaximumIndex(colony, queue, -1));
    }

    // M51-TP2: ENTRY -> N0 -> B1:true -> N2 -> B2:true -> N4 -> N5 -> B3:false -> N6
    //          -> B4:false -> N7 -> N10 -> N5 -> B3:false -> N6 -> B4:true -> N8
    //          -> B5:true -> N10 -> N5 -> B3:false -> N6 -> B4:true -> N8 -> B5:false -> N9 -> EXIT
    // Pre-condition: colony lacks armory; queue is [artillery, carpenterHouse, armory]
    public void testM51_TP2_getMaximumIndex_queueEnablesBuildAfterSelfAndOther() {
        Colony colony = createColonyWithGame(false);
        JList<BuildableType> queue = buildQueue(artilleryType(),
                                                carpenterHouseType(),
                                                armoryType());
        assertEquals(3, artilleryType().getMaximumIndex(colony, queue, -1));
    }

    // M51-TP3: ENTRY -> N0 -> B1:true -> N2 -> B2:true -> N4 -> N5 -> B3:true -> N11 -> EXIT
    // Pre-condition: colony lacks armory; queue is empty
    public void testM51_TP3_getMaximumIndex_unableToBuild() {
        Colony colony = createColonyWithGame(false);
        assertEquals(-1, artilleryType().getMaximumIndex(colony, buildQueue(), -1));
    }


    // ------------------------------------------------------------------
    // M52, M66, M67: getConsumedGoods() and stream lambdas
    // ------------------------------------------------------------------

    // M52-TP1: ENTRY -> N0 -> B1:true -> N2 -> N3 -> EXIT
    // (Also covers M66 lambda$getConsumedGoods$1 and M67 B1:false)
    // Pre-condition: freeColonist has non-zero consumption entries (food=2, bells=1)
    public void testM52_TP1_getConsumedGoods_nonEmpty() {
        List<AbstractGoods> goods = colonistType().getConsumedGoods();
        assertEquals(2, goods.size());
    }

    // M52-TP2: ENTRY -> N0 -> B1:false -> N1 -> N3 -> EXIT
    // Pre-condition: caravel has consumption == null
    public void testM52_TP2_getConsumedGoods_nullConsumption() {
        assertTrue(caravelType().getConsumedGoods().isEmpty());
    }


    // ------------------------------------------------------------------
    // M53..M56: Consumer & attack range methods
    // ------------------------------------------------------------------

    // M53-TP1: ENTRY -> N0 -> EXIT
    // Pre-condition: freeColonist priority=1000
    public void testM53_TP1_getPriority() {
        assertEquals(1000, colonistType().getPriority());
    }

    // M54-TP1: ENTRY -> N0 -> EXIT
    // Pre-condition: artillery has attackRange=0
    public void testM54_TP1_getAttackRange() {
        assertEquals(0, artilleryType().getAttackRange());
    }

    // M55-TP1: ENTRY -> N0 -> EXIT
    // Pre-condition: fresh UnitType instance
    public void testM55_TP1_setAttackRange() {
        UnitType custom = new UnitType("test.range", spec());
        custom.setAttackRange(3);
        assertEquals(3, custom.getAttackRange());
    }

    // M56-TP1: ENTRY -> N0 -> EXIT
    // Pre-condition: freeColonist consumption modifiers stream for a given id
    public void testM56_TP1_getConsumptionModifiers() {
        List<Modifier> mods = colonistType()
            .getConsumptionModifiers("model.modifier.consumeOnlySurplusFood")
            .collect(Collectors.toList());
        assertNotNull(mods);
    }


    // ------------------------------------------------------------------
    // M57: copyIn(FreeColObject other)
    // ------------------------------------------------------------------

    // M57-TP1: ENTRY -> N0 -> B1:true -> N2 -> EXIT (o == null or incompatible type)
    // Pre-condition: null or GoodsType argument
    public void testM57_TP1_copyIn_nullOrWrongType() {
        UnitType target = new UnitType("test.copy.target", spec());
        assertFalse(target.copyIn((UnitType) null));
        assertFalse(target.copyIn(foodType()));
    }

    // M57-TP2: ENTRY -> N0 -> B1:false -> N1 -> B2:true -> N3 -> EXIT
    // (Infeasible edge: N1 -> N2 [B2:false], since super.copyIn always returns true for non-null UnitType)
    // Pre-condition: non-null artillery UnitType copied into a UnitType with matching ID and initialized featureContainer/requiredGoods
    public void testM57_TP2_copyIn_validUnitType() throws Exception {
        String initXml = "<unit-type id=\"model.unit.artillery\">"
            + "<required-goods id=\"model.goods.hammers\" value=\"1\"/>"
            + "<ability id=\"model.ability.bombard\" value=\"true\"/>"
            + "</unit-type>";
        UnitType target = new UnitType("model.unit.artillery", spec());
        try (FreeColXMLReader xr = new FreeColXMLReader(new StringReader(initXml))) {
            xr.nextTag();
            target.readFromXML(xr);
        }
        assertTrue(target.copyIn(artilleryType()));
        assertEquals(artilleryType().getBaseOffence(), target.getBaseOffence());
        assertEquals(artilleryType().getPrice(), target.getPrice());
    }


    // ------------------------------------------------------------------
    // M58: writeAttributes(FreeColXMLWriter xw)
    // ------------------------------------------------------------------

    // M58-TP1: ENTRY -> N0 -> B1:true -> N2 -> B2:false -> N3 -> N4 -> B3:true -> N6
    //          -> B4:true -> N8 -> B5:true -> N10 -> EXIT
    // Pre-condition: freeColonist (no maxAttrition, has skill, no price, no mercPrice, no expertProduction)
    public void testM58_TP1_writeAttributes_colonist() throws Exception {
        StringWriter sw = new StringWriter();
        try (FreeColXMLWriter xw = new FreeColXMLWriter(sw)) {
            xw.writeStartElement(UnitType.TAG);
            colonistType().writeAttributes(xw);
            xw.writeEndElement();
        }
        String xml = sw.toString();
        assertTrue(xml.contains("skill=\"0\""));
        assertFalse(xml.contains("maximum-attrition"));
    }

    // M58-TP2: ENTRY -> N0 -> B1:false -> N1 -> N2 -> B2:true -> N4 -> B3:false -> N5 -> N6
    //          -> B4:false -> N7 -> N8 -> B5:false -> N9 -> N10 -> EXIT
    // Pre-condition: custom UnitType with maxAttrition=5, skill=UNDEFINED, price=100, mercPrice=200, expertProduction=grain;
    // plus classic artillery (skill=UNDEFINED, price=500), indianConvert (maxAttrition), manOWar (mercPrice), expertFarmer (expertProduction)
    public void testM58_TP2_writeAttributes_allOptionalAttributesPresent() throws Exception {
        UnitType custom = new UnitType("test.write.all", spec());
        String xmlIn = "<unit-type id=\"test.write.all\" maximum-attrition=\"5\" price=\"100\" mercenary-price=\"200\" expert-production=\"model.goods.grain\"/>";
        try (FreeColXMLReader xr = new FreeColXMLReader(new StringReader(xmlIn))) {
            xr.nextTag();
            custom.readAttributes(xr);
        }

        StringWriter sw = new StringWriter();
        try (FreeColXMLWriter xw = new FreeColXMLWriter(sw)) {
            xw.writeStartElement(UnitType.TAG);
            custom.writeAttributes(xw);
            xw.writeEndElement();
        }
        String xmlOut = sw.toString();
        assertTrue(xmlOut.contains("maximum-attrition=\"5\""));
        assertTrue(xmlOut.contains("price=\"100\""));
        assertTrue(xmlOut.contains("mercenary-price=\"200\""));
        assertTrue(xmlOut.contains("expert-production=\"model.goods.grain\""));
        assertFalse(xmlOut.contains("skill=\""));

        // Verify classic spec variants as well
        StringWriter swSpec = new StringWriter();
        try (FreeColXMLWriter xw = new FreeColXMLWriter(swSpec)) {
            xw.writeStartElement(UnitType.TAG);
            manOWarType().writeAttributes(xw);
            xw.writeEndElement();
            xw.writeStartElement(UnitType.TAG);
            expertFarmerType().writeAttributes(xw);
            xw.writeEndElement();
            xw.writeStartElement(UnitType.TAG);
            indianConvertType().writeAttributes(xw);
            xw.writeEndElement();
        }
        assertTrue(swSpec.toString().contains("mercenary-price=\"10000\""));
    }


    // ------------------------------------------------------------------
    // M59: writeChildren(FreeColXMLWriter xw)
    // ------------------------------------------------------------------

    // M59-TP1: ENTRY -> N0 -> B1:true -> N3 -> B3:true -> N7 -> EXIT
    // Pre-condition: custom UnitType with defaultRole == null and consumption == null
    public void testM59_TP1_writeChildren_nullRoleAndNullConsumption() throws Exception {
        UnitType custom = new UnitType("test.wc.nulls", spec());
        Field roleField = UnitType.class.getDeclaredField("defaultRole");
        roleField.setAccessible(true);
        roleField.set(custom, null);

        StringWriter sw = new StringWriter();
        try (FreeColXMLWriter xw = new FreeColXMLWriter(sw)) {
            xw.writeStartElement(UnitType.TAG);
            custom.writeChildren(xw);
            xw.writeEndElement();
        }
        String xml = sw.toString();
        assertFalse(xml.contains("default-role"));
        assertFalse(xml.contains("consumes"));
    }

    // M59-TP2: ENTRY -> N0 -> B1:false -> N1 -> B2:true -> N3 -> B3:false -> N4 -> N5
    //          -> B4:false -> N6 -> N5 -> B4:true -> N7 -> EXIT
    // Pre-condition: freeColonist has spec defaultRole (B2:true skips default-role) and consumption (food, bells)
    public void testM59_TP2_writeChildren_defaultRoleAndConsumes() throws Exception {
        StringWriter sw = new StringWriter();
        try (FreeColXMLWriter xw = new FreeColXMLWriter(sw)) {
            xw.writeStartElement(UnitType.TAG);
            colonistType().writeChildren(xw);
            xw.writeEndElement();
        }
        String xml = sw.toString();
        assertFalse(xml.contains("<default-role"));
        assertTrue(xml.contains("<consumes"));
    }

    // M59-TP3: ENTRY -> N0 -> B1:false -> N1 -> B2:false -> N2 -> N3 -> B3:true -> N7 -> EXIT
    // Pre-condition: custom UnitType with non-default role (missionary) and consumption == null
    public void testM59_TP3_writeChildren_nonDefaultRoleNoConsumption() throws Exception {
        UnitType custom = new UnitType("test.wc.missionary", spec());
        String xmlIn = "<unit-type id=\"test.wc.missionary\"><default-role id=\"model.role.missionary\"/></unit-type>";
        try (FreeColXMLReader xr = new FreeColXMLReader(new StringReader(xmlIn))) {
            xr.nextTag();
            custom.readFromXML(xr);
        }

        StringWriter sw = new StringWriter();
        try (FreeColXMLWriter xw = new FreeColXMLWriter(sw)) {
            xw.writeStartElement(UnitType.TAG);
            custom.writeChildren(xw);
            xw.writeEndElement();
        }
        String xml = sw.toString();
        assertTrue(xml.contains("model.role.missionary"));
        assertFalse(xml.contains("<consumes"));
    }


    // ------------------------------------------------------------------
    // M60: readAttributes(FreeColXMLReader xr)
    // ------------------------------------------------------------------

    // M60-TP1: B1..B9:false (all OLD_* compat attributes present), B10:false (extends), B11:false (no required-population)
    // Pre-condition: XML with all 9 legacy 0.11.3 attributes, extends="colonist", omitting required-population
    public void testM60_TP1_readAttributes_legacyCompatAndInheritsRequiredPop() throws Exception {
        String xml = "<unit-type id=\"test.ra.legacy\" extends=\"colonist\""
            + " defaultUnit=\"true\""
            + " lineOfSight=\"4\""
            + " scoreValue=\"9\""
            + " hitPoints=\"8\""
            + " spaceTaken=\"14\""
            + " maximumExperience=\"300\""
            + " maximumAttrition=\"7\""
            + " skillTaught=\"model.unit.veteranSoldier\""
            + " recruitProbability=\"15\"/>";
        UnitType custom = new UnitType("test.ra.legacy", spec());
        try (FreeColXMLReader xr = new FreeColXMLReader(new StringReader(xml))) {
            xr.nextTag();
            custom.readAttributes(xr);
        }
        assertTrue(custom.isDefaultUnitType());
        assertEquals(4, custom.getLineOfSight());
        assertEquals(9, custom.getScoreValue());
        assertEquals(8, custom.getHitPoints());
        assertEquals(14, custom.getSpaceTaken());
        assertEquals(300, custom.getMaximumExperience());
        assertEquals(7, custom.getMaximumAttrition());
        assertEquals(veteranSoldierType(), custom.getSkillTaught());
        assertEquals(15, custom.getRecruitProbability());
        assertEquals(colonistType().getRequiredPopulation(), custom.getRequiredPopulation());
    }

    // M60-TP2: B1..B9:true (modern attributes only), B10:true (no extends)
    // Pre-condition: standalone XML without OLD_* attributes and without extends
    public void testM60_TP2_readAttributes_modernStandalone() throws Exception {
        String xml = "<unit-type id=\"test.ra.modern\" default-unit=\"true\""
            + " offence=\"6\" defence=\"4\" movement=\"5\" line-of-sight=\"3\""
            + " score-value=\"7\" space=\"2\" hit-points=\"9\" space-taken=\"10\""
            + " skill=\"2\" maximum-experience=\"150\" recruit-probability=\"12\""
            + " skill-taught=\"model.unit.expertFarmer\""
            + " expert-production=\"model.goods.grain\"/>";
        UnitType custom = new UnitType("test.ra.modern", spec());
        try (FreeColXMLReader xr = new FreeColXMLReader(new StringReader(xml))) {
            xr.nextTag();
            custom.readAttributes(xr);
        }
        assertTrue(custom.isDefaultUnitType());
        assertEquals(6, custom.getBaseOffence());
        assertEquals(4, custom.getBaseDefence());
        assertEquals(5, custom.getMovement());
        assertEquals(3, custom.getLineOfSight());
        assertEquals(grainType(), custom.getExpertProduction());
    }

    // M60-TP3: B1..B9:true, B10:false (extends="colonist"), B11:true (explicit required-population)
    // Pre-condition: XML extending colonist with explicit required-population="5"
    public void testM60_TP3_readAttributes_extendsWithExplicitRequiredPopulation() throws Exception {
        String xml = "<unit-type id=\"test.ra.explicitPop\" extends=\"colonist\" required-population=\"5\"/>";
        UnitType custom = new UnitType("test.ra.explicitPop", spec());
        try (FreeColXMLReader xr = new FreeColXMLReader(new StringReader(xml))) {
            xr.nextTag();
            custom.readAttributes(xr);
        }
        assertEquals(5, custom.getRequiredPopulation());
    }


    // ------------------------------------------------------------------
    // M61: clearContainers(FreeColXMLReader xr)
    // ------------------------------------------------------------------

    // M61-TP1: ENTRY -> N0 -> EXIT
    // Pre-condition: custom UnitType with non-null consumption map
    public void testM61_TP1_clearContainers() throws Exception {
        UnitType custom = new UnitType("test.clear", spec());
        TypeCountMap<GoodsType> map = new TypeCountMap<>();
        map.incrementCount(foodType(), 2);
        custom.setConsumption(map);
        assertNotNull(custom.getConsumption());

        try (FreeColXMLReader xr = new FreeColXMLReader(new StringReader("<unit-type/>"))) {
            xr.nextTag();
            custom.clearContainers(xr);
        }
        assertNull(custom.getConsumption());
    }


    // ------------------------------------------------------------------
    // M62: readChildren(FreeColXMLReader xr)
    // ------------------------------------------------------------------

    // M62-TP1: ENTRY -> N0 -> B1:true -> N7 -> EXIT (parent == this)
    // Pre-condition: XML without extends attribute
    public void testM62_TP1_readChildren_noParent() throws Exception {
        UnitType custom = new UnitType("test.rc.noparent", spec());
        try (FreeColXMLReader xr = new FreeColXMLReader(new StringReader("<unit-type id=\"test.rc.noparent\"/>"))) {
            xr.nextTag();
            custom.readChildren(xr);
        }
        assertEquals(spec().getDefaultRole(), custom.getDefaultRole());
    }

    // M62-TP2: ENTRY -> N0 -> B1:false -> N1 -> B2:true -> N5 -> B4:true -> N7 -> EXIT
    // Pre-condition: extends="model.unit.wagonTrain" (concrete type with consumption == null)
    public void testM62_TP2_readChildren_concreteParentNoConsumption() throws Exception {
        UnitType custom = new UnitType("test.rc.wagon", spec());
        String xml = "<unit-type id=\"test.rc.wagon\" extends=\"model.unit.wagonTrain\"/>";
        try (FreeColXMLReader xr = new FreeColXMLReader(new StringReader(xml))) {
            xr.nextTag();
            custom.readChildren(xr);
        }
        assertNull(custom.getConsumption());
    }

    // M62-TP3: ENTRY -> N0 -> B1:false -> N1 -> B2:false -> N2 -> B3:false -> N3 -> N4
    //          -> N5 -> B4:false -> N6 -> N7 -> EXIT
    // Pre-condition: extends="colonist" (abstract parent with consumption != null); child consumption == null
    public void testM62_TP3_readChildren_abstractParentInitializesConsumption() throws Exception {
        UnitType custom = new UnitType("test.rc.absNull", spec());
        String xml = "<unit-type id=\"test.rc.absNull\" extends=\"colonist\"/>";
        try (FreeColXMLReader xr = new FreeColXMLReader(new StringReader(xml))) {
            xr.nextTag();
            custom.readChildren(xr);
        }
        assertEquals(2, custom.getConsumptionOf(foodType()));
    }

    // M62-TP4: ENTRY -> N0 -> B1:false -> N1 -> B2:false -> N2 -> B3:true -> N4
    //          -> N5 -> B4:false -> N6 -> N7 -> EXIT
    // Pre-condition: extends="colonist" (abstract parent with consumption != null); child consumption already initialized
    public void testM62_TP4_readChildren_abstractParentMergesExistingConsumption() throws Exception {
        UnitType custom = new UnitType("test.rc.absNonNull", spec());
        TypeCountMap<GoodsType> existing = new TypeCountMap<>();
        existing.incrementCount(grainType(), 1);
        custom.setConsumption(existing);

        String xml = "<unit-type id=\"test.rc.absNonNull\" extends=\"colonist\"/>";
        try (FreeColXMLReader xr = new FreeColXMLReader(new StringReader(xml))) {
            xr.nextTag();
            custom.readChildren(xr);
        }
        assertEquals(1, custom.getConsumptionOf(grainType()));
        assertEquals(2, custom.getConsumptionOf(foodType()));
    }


    // ------------------------------------------------------------------
    // M63: readChild(FreeColXMLReader xr)
    // ------------------------------------------------------------------

    // M63-TP1..TP6: covers B1:false (<consumes>), B2:false (<default-equipment>),
    // B3:false (<default-role>), B4:true (<downgrade>), B5:true (<upgrade>), and super.readChild (<ability>)
    // Pre-condition: XML containing all UnitType child tag kinds + fallback <ability>
    public void testM63_TP1_readChild_allSupportedTags() throws Exception {
        String xml = "<unit-type id=\"test.rchild\">"
            + "<consumes id=\"model.goods.food\" value=\"3\"/>"
            + "<default-equipment id=\"model.equipment.tools\"/>"
            + "<default-role id=\"model.role.missionary\"/>"
            + "<downgrade unit=\"model.unit.freeColonist\"/>"
            + "<upgrade unit=\"model.unit.veteranSoldier\"/>"
            + "<ability id=\"model.ability.person\" value=\"true\"/>"
            + "</unit-type>";
        UnitType custom = new UnitType("test.rchild", spec());
        try (FreeColXMLReader xr = new FreeColXMLReader(new StringReader(xml))) {
            xr.nextTag();
            while (xr.moreTags()) {
                custom.readChild(xr);
            }
        }
        assertEquals(3, custom.getConsumptionOf(foodType()));
        assertEquals("model.role.missionary", custom.getDefaultRole().getId());
        assertTrue(custom.hasAbility(Ability.PERSON));
    }


    // ------------------------------------------------------------------
    // M64..M65: Serialization tag & string representation
    // ------------------------------------------------------------------

    // M64-TP1: ENTRY -> N0 -> EXIT
    // Pre-condition: freeColonist unit type
    public void testM64_TP1_getXMLTagName() {
        assertEquals(UnitType.TAG, colonistType().getXMLTagName());
        assertEquals("unit-type", UnitType.TAG);
    }

    // M65-TP1: ENTRY -> N0 -> EXIT
    // Pre-condition: freeColonist unit type
    public void testM65_TP1_toString() {
        assertEquals("model.unit.freeColonist", colonistType().toString());
    }


    // ------------------------------------------------------------------
    // M66..M68: Lambdas in getConsumedGoods and canBeBuiltInColony
    // ------------------------------------------------------------------

    // M66-TP1: lambda$getConsumedGoods$1 (AbstractGoods mapper)
    // Pre-condition: covered via colonistType().getConsumedGoods()
    public void testM66_TP1_lambdaGetConsumedGoodsMapper() {
        List<AbstractGoods> list = colonistType().getConsumedGoods();
        assertTrue(list.stream().anyMatch(ag -> ag.getType().equals(foodType()) && ag.getAmount() == 2));
    }

    // M67-TP1: ENTRY -> N0 -> B1:true -> N2 -> N3 -> EXIT (count == 0 excluded),
    //          ENTRY -> N0 -> B1:false -> N1 -> N3 -> EXIT (count != 0 included)
    // Pre-condition: custom UnitType with food=2 and grain=0 in consumption map
    public void testM67_TP1_lambdaGetConsumedGoodsFilter_zeroAndNonZero() {
        UnitType custom = new UnitType("test.zeroFilter", spec());
        TypeCountMap<GoodsType> map = new TypeCountMap<>();
        map.incrementCount(foodType(), 2);
        map.incrementCount(grainType(), 0);
        custom.setConsumption(map);

        List<AbstractGoods> consumed = custom.getConsumedGoods();
        assertEquals(1, consumed.size());
        assertEquals(foodType(), consumed.get(0).getType());
    }

    // M68-TP1: lambda$canBeBuiltInColony$0 (assumeBuilt predicate)
    // Pre-condition: colony without armory, assumeBuilt contains armory
    public void testM68_TP1_lambdaCanBeBuiltInColonyPredicate() {
        Colony colony = createColonyWithGame(false);
        assertEquals(Colony.NoBuildReason.NONE,
                     artilleryType().canBeBuiltInColony(colony, List.of(armoryType())));
    }


    // ------------------------------------------------------------------
    // M69: <clinit>() static initializer
    // ------------------------------------------------------------------

    // M69-TP1: ENTRY -> N0 -> EXIT
    // Pre-condition: UnitType static fields initialized on class load
    public void testM69_TP1_staticInitializer() {
        assertEquals("unit-type", UnitType.TAG);
    }
}
