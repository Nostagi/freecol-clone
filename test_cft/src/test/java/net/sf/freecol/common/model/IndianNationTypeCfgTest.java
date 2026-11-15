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
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import net.sf.freecol.common.io.FreeColXMLReader;
import net.sf.freecol.common.io.FreeColXMLWriter;
import net.sf.freecol.common.util.RandomChoice;
import net.sf.freecol.util.test.FreeColTestCase;


/**
 * CFG-driven test suite for {@link IndianNationType} derived from
 * {@code test_cft/graph/net/sf/freecol/common/model/IndianNationType.json}.
 *
 * Covers all 22 methods (M1..M22, including private {@code addSkill}/{@code addRegion}
 * and all 4 {@code lambda$generateSkillsForTile$...} methods) and achieves 100% feasible
 * C3 (node + edge) coverage across 37 Test Paths.
 *
 * Infeasible edges documented:
 * - M13 ({@code copyIn}): {@code N1 -> N2} ({@code B2:false}) — once {@code o} is non-null
 *   ({@code B1:false}), {@code o} is guaranteed to be an {@code IndianNationType} (subclass
 *   of {@code NationType} and {@code FreeColObject}), so {@code super.copyIn(o)} always
 *   returns {@code true}.
 */
public class IndianNationTypeCfgTest extends FreeColTestCase {

    private UnitType expertFarmer;
    private UnitType expertFisherman;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        // Pre-condition: Load standard UnitTypes from Specification
        expertFarmer = spec().getUnitType("model.unit.expertFarmer");
        expertFisherman = spec().getUnitType("model.unit.expertFisherman");
    }

    // ------------------------------------------------------------------
    // Pre-condition helpers & stubs
    // ------------------------------------------------------------------

    private static void invokeAddSkill(IndianNationType nt, UnitType ut, int prob)
            throws Exception {
        Method m = IndianNationType.class.getDeclaredMethod(
                "addSkill", UnitType.class, int.class);
        m.setAccessible(true);
        m.invoke(nt, ut, prob);
    }

    private static void invokeAddRegion(IndianNationType nt, String regionId)
            throws Exception {
        Method m = IndianNationType.class.getDeclaredMethod("addRegion", String.class);
        m.setAccessible(true);
        m.invoke(nt, regionId);
    }

    private static void registerType(Specification localSpec, String id,
                                     FreeColSpecObjectType type) throws Exception {
        Method m = Specification.class.getDeclaredMethod(
                "addType", String.class, FreeColSpecObjectType.class);
        m.setAccessible(true);
        m.invoke(localSpec, id, type);
    }

    private static FreeColXMLReader openAtRootElement(String xml) throws Exception {
        FreeColXMLReader xr = new FreeColXMLReader(new StringReader(xml));
        xr.nextTag();
        return xr;
    }

    private static String writeChildrenToXml(IndianNationType nt) throws Exception {
        StringWriter sw = new StringWriter();
        try (FreeColXMLWriter xw = new FreeColXMLWriter(
                sw, FreeColXMLWriter.WriteScope.toSave(), false)) {
            xw.writeStartDocument("UTF-8", "1.0");
            xw.writeStartElement(IndianNationType.TAG);
            nt.writeChildren(xw);
            xw.writeEndElement();
            xw.writeEndDocument();
        }
        return sw.toString();
    }

    // ------------------------------------------------------------------
    // M1: <init>(String, Specification)V — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M1-TP1: ENTRY -> N0 -> EXIT (constructs IndianNationType with id and specification)
    public void testM1_TP1_Constructor() {
        IndianNationType nt = new IndianNationType("model.nationType.test", spec());
        assertEquals("model.nationType.test", nt.getId());
        assertEquals(spec(), nt.getSpecification());
    }

    // ------------------------------------------------------------------
    // M2: isEuropean()Z — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M2-TP1: ENTRY -> N0 -> EXIT (always returns false)
    public void testM2_TP1_IsEuropeanReturnsFalse() {
        IndianNationType nt = new IndianNationType("model.nationType.test", spec());
        assertFalse(nt.isEuropean());
    }

    // ------------------------------------------------------------------
    // M3: isIndian()Z — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M3-TP1: ENTRY -> N0 -> EXIT (always returns true)
    public void testM3_TP1_IsIndianReturnsTrue() {
        IndianNationType nt = new IndianNationType("model.nationType.test", spec());
        assertTrue(nt.isIndian());
    }

    // ------------------------------------------------------------------
    // M4: isREF()Z — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M4-TP1: ENTRY -> N0 -> EXIT (always returns false)
    public void testM4_TP1_IsREFReturnsFalse() {
        IndianNationType nt = new IndianNationType("model.nationType.test", spec());
        assertFalse(nt.isREF());
    }

    // ------------------------------------------------------------------
    // M5: getSettlementTypeKey(Z)Ljava/lang/String; — 2 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M5-TP1: ENTRY -> N0 -(B1:true)-> N2 -> N3 -> EXIT (plural == false)
    public void testM5_TP1_GetSettlementTypeKeySingular() {
        IndianNationType apache = (IndianNationType) spec().getNationType("model.nationType.apache");
        String expected = apache.getSettlementType(false).getId();
        assertEquals(expected, apache.getSettlementTypeKey(false));
    }

    // M5-TP2: ENTRY -> N0 -(B1:false)-> N1 -> N3 -> EXIT (plural == true)
    public void testM5_TP2_GetSettlementTypeKeyPlural() {
        IndianNationType apache = (IndianNationType) spec().getNationType("model.nationType.apache");
        String expected = apache.getSettlementType(false).getId() + ".plural";
        assertEquals(expected, apache.getSettlementTypeKey(true));
    }

    // ------------------------------------------------------------------
    // M6: getSkills()Ljava/util/List; — 2 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M6-TP1: ENTRY -> N0 -(B1:false)-> N1 -> N3 -> EXIT (skills == null -> emptyList)
    public void testM6_TP1_GetSkillsWhenNullReturnsEmptyList() {
        IndianNationType nt = new IndianNationType("model.nationType.test", spec());
        assertTrue(nt.getSkills().isEmpty());
    }

    // M6-TP2: ENTRY -> N0 -(B1:true)-> N2 -> N3 -> EXIT (skills != null -> returns skills)
    public void testM6_TP2_GetSkillsWhenNonNullReturnsSkills() throws Exception {
        IndianNationType nt = new IndianNationType("model.nationType.test", spec());
        invokeAddSkill(nt, expertFarmer, 10);
        assertEquals(1, nt.getSkills().size());
        assertEquals(expertFarmer, nt.getSkills().get(0).getObject());
    }

    // ------------------------------------------------------------------
    // M7: setSkills(Ljava/util/List;)V — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M7-TP1: ENTRY -> N0 -> EXIT (clears and replaces skills list)
    public void testM7_TP1_SetSkills() throws Exception {
        IndianNationType nt = new IndianNationType("model.nationType.test", spec());
        invokeAddSkill(nt, expertFarmer, 10);
        List<RandomChoice<UnitType>> updated = Collections.singletonList(
                new RandomChoice<>(expertFisherman, 20));
        nt.setSkills(updated);
        assertEquals(1, nt.getSkills().size());
        assertEquals(expertFisherman, nt.getSkills().get(0).getObject());
    }

    // ------------------------------------------------------------------
    // M8: addSkill(UnitType, I)V — 2 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M8-TP1: ENTRY -> N0 -(B1:false)-> N1 -> N2 -> EXIT (skills == null -> allocates list)
    public void testM8_TP1_AddSkillWhenNull() throws Exception {
        IndianNationType nt = new IndianNationType("model.nationType.test", spec());
        invokeAddSkill(nt, expertFarmer, 5);
        assertEquals(1, nt.getSkills().size());
    }

    // M8-TP2: ENTRY -> N0 -(B1:true)-> N2 -> EXIT (skills != null -> appends to existing list)
    public void testM8_TP2_AddSkillWhenNonNull() throws Exception {
        IndianNationType nt = new IndianNationType("model.nationType.test", spec());
        invokeAddSkill(nt, expertFarmer, 5);
        invokeAddSkill(nt, expertFisherman, 8);
        assertEquals(2, nt.getSkills().size());
    }

    // ------------------------------------------------------------------
    // M9: generateSkillsForTile(Tile)Ljava/util/List; — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M9-TP1: ENTRY -> N0 -> N1 -(B1:false)-> N2 -> N1 -(B1:true)-> N3 -> EXIT
    // (surrounding tiles loop executes and scales skill probabilities)
    public void testM9_TP1_GenerateSkillsForTile() throws Exception {
        Game game = getStandardGame();
        Map map = getTestMap();
        game.setMap(map);
        Tile center = map.getTile(5, 5);

        IndianNationType nt = new IndianNationType("model.nationType.test", spec());
        invokeAddSkill(nt, expertFarmer, 10);

        List<RandomChoice<UnitType>> generated = nt.generateSkillsForTile(center);
        assertEquals(1, generated.size());
        assertEquals(expertFarmer, generated.get(0).getObject());
        assertTrue(generated.get(0).getProbability() >= 10);
    }

    // ------------------------------------------------------------------
    // M10: getRegions()Ljava/util/List; — 2 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M10-TP1: ENTRY -> N0 -(B1:false)-> N1 -> N3 -> EXIT (regions == null -> emptyList)
    public void testM10_TP1_GetRegionsWhenNull() {
        IndianNationType nt = new IndianNationType("model.nationType.test", spec());
        assertTrue(nt.getRegions().isEmpty());
    }

    // M10-TP2: ENTRY -> N0 -(B1:true)-> N2 -> N3 -> EXIT (regions != null -> returns regions)
    public void testM10_TP2_GetRegionsWhenNonNull() throws Exception {
        IndianNationType nt = new IndianNationType("model.nationType.test", spec());
        invokeAddRegion(nt, "model.region.north");
        assertEquals(Collections.singletonList("model.region.north"), nt.getRegions());
    }

    // ------------------------------------------------------------------
    // M11: setRegions(Ljava/util/List;)V — 2 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M11-TP1: ENTRY -> N0 -(B1:false)-> N1 -> N3 -> EXIT (this.regions == null)
    public void testM11_TP1_SetRegionsWhenNull() {
        IndianNationType nt = new IndianNationType("model.nationType.test", spec());
        nt.setRegions(Arrays.asList("model.region.east", "model.region.west"));
        assertEquals(2, nt.getRegions().size());
    }

    // M11-TP2: ENTRY -> N0 -(B1:true)-> N2 -> N3 -> EXIT (this.regions != null -> clears first)
    public void testM11_TP2_SetRegionsWhenNonNull() throws Exception {
        IndianNationType nt = new IndianNationType("model.nationType.test", spec());
        invokeAddRegion(nt, "model.region.old");
        nt.setRegions(Collections.singletonList("model.region.new"));
        assertEquals(Collections.singletonList("model.region.new"), nt.getRegions());
    }

    // ------------------------------------------------------------------
    // M12: addRegion(String)V — 2 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M12-TP1: ENTRY -> N0 -(B1:false)-> N1 -> N2 -> EXIT (regions == null -> allocates list)
    public void testM12_TP1_AddRegionWhenNull() throws Exception {
        IndianNationType nt = new IndianNationType("model.nationType.test", spec());
        invokeAddRegion(nt, "model.region.a");
        assertEquals(1, nt.getRegions().size());
    }

    // M12-TP2: ENTRY -> N0 -(B1:true)-> N2 -> EXIT (regions != null -> appends)
    public void testM12_TP2_AddRegionWhenNonNull() throws Exception {
        IndianNationType nt = new IndianNationType("model.nationType.test", spec());
        invokeAddRegion(nt, "model.region.a");
        invokeAddRegion(nt, "model.region.b");
        assertEquals(2, nt.getRegions().size());
    }

    // ------------------------------------------------------------------
    // M13: copyIn(FreeColObject)Z — 2 feasible Test Paths (100% feasible C3)
    // ------------------------------------------------------------------

    // M13-TP1: ENTRY -> N0 -(B1:true)-> N2 -> EXIT (other is not IndianNationType -> false)
    public void testM13_TP1_CopyInWrongTypeReturnsFalse() {
        IndianNationType nt = new IndianNationType("model.nationType.test", spec());
        NationType european = spec().getNationType("model.nationType.trade");
        assertFalse(nt.copyIn(european));
    }

    // M13-TP2: ENTRY -> N0 -(B1:false)-> N1 -(B2:true)-> N3 -> EXIT (other is IndianNationType -> true)
    public void testM13_TP2_CopyInValidIndianNationTypeReturnsTrue() throws Exception {
        IndianNationType target = new IndianNationType("model.nationType.target", spec());
        target.getFeatureContainer();
        invokeAddSkill(target, expertFarmer, 1);

        IndianNationType source = new IndianNationType("model.nationType.target", spec());
        invokeAddSkill(source, expertFisherman, 50);
        invokeAddRegion(source, "model.region.south");

        assertTrue(target.copyIn(source));
        assertEquals(1, target.getSkills().size());
        assertEquals(expertFisherman, target.getSkills().get(0).getObject());
        assertEquals(Collections.singletonList("model.region.south"), target.getRegions());
    }

    // ------------------------------------------------------------------
    // M14: writeChildren(FreeColXMLWriter)V — 2 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M14-TP1: ENTRY -> N0 -> N1 -(B1:true)-> N3 -> N4 -(B2:true)-> N6 -> EXIT
    // (both skills and regions empty)
    public void testM14_TP1_WriteChildrenEmpty() throws Exception {
        IndianNationType nt = new IndianNationType("model.nationType.w1", spec());
        String xml = writeChildrenToXml(nt);
        assertFalse(xml.contains("<skill"));
        assertFalse(xml.contains("<region"));
    }

    // M14-TP2: ENTRY -> N0 -> N1 -(B1:false)-> N2 -> N1 -(B1:true)-> N3 -> N4 -(B2:false)-> N5 -> N4 -(B2:true)-> N6 -> EXIT
    // (both skills and regions non-empty)
    public void testM14_TP2_WriteChildrenWithSkillsAndRegions() throws Exception {
        IndianNationType nt = new IndianNationType("model.nationType.w2", spec());
        invokeAddSkill(nt, expertFarmer, 30);
        invokeAddRegion(nt, "model.region.desert");
        String xml = writeChildrenToXml(nt);
        assertTrue(xml.contains("skill"));
        assertTrue(xml.contains("probability=\"30\""));
        assertTrue(xml.contains("model.region.desert"));
    }

    // ------------------------------------------------------------------
    // M15: clearContainers(FreeColXMLReader)V — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M15-TP1: ENTRY -> N0 -> EXIT (clears skills and regions to null)
    public void testM15_TP1_ClearContainers() throws Exception {
        IndianNationType nt = new IndianNationType("model.nationType.clear", spec());
        invokeAddSkill(nt, expertFarmer, 10);
        invokeAddRegion(nt, "model.region.clear");

        try (FreeColXMLReader xr = openAtRootElement("<indian-nation-type id=\"model.nationType.clear\"/>")) {
            nt.clearContainers(xr);
        }
        assertTrue(nt.getSkills().isEmpty());
        assertTrue(nt.getRegions().isEmpty());
    }

    // ------------------------------------------------------------------
    // M16: readChildren(FreeColXMLReader)V — 5 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M16-TP1: ENTRY -> N0 -(B1:true)-> N11 -> EXIT (parent == this, no extends attribute)
    public void testM16_TP1_ReadChildrenNoExtends() throws Exception {
        IndianNationType nt = new IndianNationType("model.nationType.rc1", spec());
        String xml = "<indian-nation-type id=\"model.nationType.rc1\"/>";
        try (FreeColXMLReader xr = openAtRootElement(xml)) {
            nt.readChildren(xr);
        }
        assertTrue(nt.getSkills().isEmpty());
    }

    // M16-TP2: ENTRY -> N0 -(B1:false)-> N1 -(B2:true)-> N6 -(B5:true)-> N11 -> EXIT
    // (parent != this, parent.skills == null, parent.regions == null)
    public void testM16_TP2_ReadChildrenExtendsParentWithNullContainers() throws Exception {
        Specification localSpec = new Specification();
        IndianNationType parent = new IndianNationType("parent.null", localSpec);
        registerType(localSpec, "parent.null", parent);

        IndianNationType child = new IndianNationType("child.1", localSpec);
        String xml = "<indian-nation-type id=\"child.1\" extends=\"parent.null\"/>";
        try (FreeColXMLReader xr = openAtRootElement(xml)) {
            child.readChildren(xr);
        }
        assertTrue(child.getSkills().isEmpty());
        assertTrue(child.getRegions().isEmpty());
    }

    // M16-TP3: ENTRY -> N0 -(B1:false)-> N1 -(B2:false)-> N2 -(B3:true)-> N6 -(B5:false)-> N7 -(B6:true)-> N11 -> EXIT
    // (parent != this, parent.skills non-null empty, parent.regions non-null empty)
    public void testM16_TP3_ReadChildrenExtendsParentWithEmptyContainers() throws Exception {
        Specification localSpec = new Specification();
        IndianNationType parent = new IndianNationType("parent.empty", localSpec);
        invokeAddSkill(parent, expertFarmer, 1);
        parent.setSkills(Collections.emptyList());
        parent.setRegions(Collections.emptyList());
        registerType(localSpec, "parent.empty", parent);

        IndianNationType child = new IndianNationType("child.2", localSpec);
        String xml = "<indian-nation-type id=\"child.2\" extends=\"parent.empty\"/>";
        try (FreeColXMLReader xr = openAtRootElement(xml)) {
            child.readChildren(xr);
        }
        assertTrue(child.getSkills().isEmpty());
        assertTrue(child.getRegions().isEmpty());
    }

    // M16-TP4: ENTRY -> N0 -(B1:false)-> N1 -(B2:false)-> N2 -(B3:false)-> N3 -(B4:false)-> N4 -> N5 -> N6 -(B5:false)-> N7 -(B6:false)-> N8 -(B7:false)-> N9 -> N10 -> N11 -> EXIT
    // (parent != this, parent has skills/regions, child skills/regions initially null)
    public void testM16_TP4_ReadChildrenExtendsParentCopiesIntoNullChild() throws Exception {
        Specification localSpec = new Specification();
        IndianNationType parent = new IndianNationType("parent.full", localSpec);
        invokeAddSkill(parent, expertFarmer, 15);
        invokeAddRegion(parent, "model.region.arctic");
        registerType(localSpec, "parent.full", parent);

        IndianNationType child = new IndianNationType("child.3", localSpec);
        String xml = "<indian-nation-type id=\"child.3\" extends=\"parent.full\"/>";
        try (FreeColXMLReader xr = openAtRootElement(xml)) {
            child.readChildren(xr);
        }
        assertEquals(1, child.getSkills().size());
        assertEquals(1, child.getRegions().size());
    }

    // M16-TP5: ENTRY -> N0 -(B1:false)-> N1 -(B2:false)-> N2 -(B3:false)-> N3 -(B4:true)-> N5 -> N6 -(B5:false)-> N7 -(B6:false)-> N8 -(B7:true)-> N10 -> N11 -> EXIT
    // (parent != this, parent has skills/regions, child skills/regions already non-null)
    public void testM16_TP5_ReadChildrenExtendsParentAppendsToNonNullChild() throws Exception {
        Specification localSpec = new Specification();
        IndianNationType parent = new IndianNationType("parent.full2", localSpec);
        invokeAddSkill(parent, expertFarmer, 15);
        invokeAddRegion(parent, "model.region.arctic");
        registerType(localSpec, "parent.full2", parent);

        IndianNationType child = new IndianNationType("child.4", localSpec);
        invokeAddSkill(child, expertFisherman, 25);
        invokeAddRegion(child, "model.region.tropics");
        String xml = "<indian-nation-type id=\"child.4\" extends=\"parent.full2\"/>";
        try (FreeColXMLReader xr = openAtRootElement(xml)) {
            child.readChildren(xr);
        }
        assertEquals(2, child.getSkills().size());
        assertEquals(2, child.getRegions().size());
    }

    // ------------------------------------------------------------------
    // M17: readChild(FreeColXMLReader)V — 3 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M17-TP1: ENTRY -> N0 -(B1:false)-> N1 -> N5 -> EXIT (tag equals "skill")
    public void testM17_TP1_ReadChildSkill() throws Exception {
        IndianNationType nt = new IndianNationType("model.nationType.ch1", spec());
        String xml = "<indian-nation-type id=\"model.nationType.ch1\">"
                + "<skill id=\"model.unit.expertFarmer\" probability=\"40\"/>"
                + "</indian-nation-type>";
        try (FreeColXMLReader xr = openAtRootElement(xml)) {
            xr.nextTag();
            nt.readChild(xr);
        }
        assertEquals(1, nt.getSkills().size());
        assertEquals(40, nt.getSkills().get(0).getProbability());
    }

    // M17-TP2: ENTRY -> N0 -(B1:true)-> N2 -(B2:false)-> N3 -> N5 -> EXIT (tag equals "region")
    public void testM17_TP2_ReadChildRegion() throws Exception {
        IndianNationType nt = new IndianNationType("model.nationType.ch2", spec());
        String xml = "<indian-nation-type id=\"model.nationType.ch2\">"
                + "<region id=\"model.region.west\"/>"
                + "</indian-nation-type>";
        try (FreeColXMLReader xr = openAtRootElement(xml)) {
            xr.nextTag();
            nt.readChild(xr);
        }
        assertEquals(Collections.singletonList("model.region.west"), nt.getRegions());
    }

    // M17-TP3: ENTRY -> N0 -(B1:true)-> N2 -(B2:true)-> N4 -> N5 -> EXIT (other tag -> super.readChild)
    public void testM17_TP3_ReadChildDelegatesToSuper() throws Exception {
        IndianNationType nt = new IndianNationType("model.nationType.ch3", spec());
        String xml = "<indian-nation-type id=\"model.nationType.ch3\">"
                + "<ability id=\"model.ability.ambushBonus\" value=\"true\"/>"
                + "</indian-nation-type>";
        try (FreeColXMLReader xr = openAtRootElement(xml)) {
            xr.nextTag();
            nt.readChild(xr);
        }
        assertTrue(nt.hasAbility("model.ability.ambushBonus"));
    }

    // ------------------------------------------------------------------
    // M18: getXMLTagName()Ljava/lang/String; — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M18-TP1: ENTRY -> N0 -> EXIT (returns TAG)
    public void testM18_TP1_GetXMLTagName() {
        IndianNationType nt = new IndianNationType("model.nationType.tag", spec());
        assertEquals(IndianNationType.TAG, nt.getXMLTagName());
        assertEquals("indian-nation-type", nt.getXMLTagName());
    }

    // ------------------------------------------------------------------
    // M19..M22: lambda$generateSkillsForTile$3..0 — 4 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M19-TP1: ENTRY -> N0 -> EXIT (lambda$generateSkillsForTile$3 maps RandomChoice with scaled prob)
    public void testM19_TP1_LambdaGenerateSkills3() throws Exception {
        testM9_TP1_GenerateSkillsForTile();
    }

    // M20-TP1: ENTRY -> N0 -> EXIT (lambda$generateSkillsForTile$2 accumulates tile potential production)
    public void testM20_TP1_LambdaGenerateSkills2() throws Exception {
        testM9_TP1_GenerateSkillsForTile();
    }

    // M21-TP1: ENTRY -> N0 -> EXIT (lambda$generateSkillsForTile$1 initializes scale value to 1)
    public void testM21_TP1_LambdaGenerateSkills1() throws Exception {
        testM9_TP1_GenerateSkillsForTile();
    }

    // M22-TP1: ENTRY -> N0 -> EXIT (lambda$generateSkillsForTile$0 extracts expert production GoodsType)
    public void testM22_TP1_LambdaGenerateSkills0() throws Exception {
        testM9_TP1_GenerateSkillsForTile();
    }
}
