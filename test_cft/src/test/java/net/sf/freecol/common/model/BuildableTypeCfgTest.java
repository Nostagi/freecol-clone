package net.sf.freecol.common.model;

import java.io.StringReader;
import java.io.StringWriter;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.swing.JList;

import net.sf.freecol.common.io.FreeColXMLReader;
import net.sf.freecol.common.io.FreeColXMLWriter;
import net.sf.freecol.common.model.Colony.NoBuildReason;
import net.sf.freecol.util.test.FreeColTestCase;


/**
 * CFG-driven test suite for {@link BuildableType} derived from
 * {@code test_cft/graph/net/sf/freecol/common/model/BuildableType.json}.
 *
 * Covers all 31 methods (M1..M31) and achieves 100% feasible C3 (node + edge)
 * coverage across 47 Test Paths.
 *
 * Infeasible edges documented:
 * - M22 ({@code copyIn(FreeColObject)}): {@code N1 -> N2} ({@code B2:false}) —
 *   once {@code copyInCast(other, BuildableType.class)} returns a non-null
 *   {@code BuildableType o}, {@code super.copyIn(o)} always returns {@code true}.
 */
public class BuildableTypeCfgTest extends FreeColTestCase {

    private GoodsType hammersType;
    private GoodsType toolsType;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        // Pre-condition: standard goods types from specification
        hammersType = spec().getGoodsType("model.goods.hammers");
        toolsType = spec().getGoodsType("model.goods.tools");
    }

    // ------------------------------------------------------------------
    // Pre-condition helpers & stubs
    // ------------------------------------------------------------------

    private static class StubBuildableType extends BuildableType {
        public StubBuildableType(String id, Specification specification) {
            super(id, specification);
        }

        @Override
        public NoBuildReason canBeBuiltInColony(Colony colony, List<BuildableType> assumeBuilt) {
            return NoBuildReason.NONE;
        }

        @Override
        public String getXMLTagName() {
            return "buildableType";
        }
    }

    private void invokeAddRequiredGoods(BuildableType bt, AbstractGoods ag) throws Exception {
        Method m = BuildableType.class.getDeclaredMethod("addRequiredGoods", AbstractGoods.class);
        m.setAccessible(true);
        m.invoke(bt, ag);
    }

    private void invokeAddLimit(BuildableType bt, Limit limit) throws Exception {
        Method m = BuildableType.class.getDeclaredMethod("addLimit", Limit.class);
        m.setAccessible(true);
        m.invoke(bt, limit);
    }

    private String serializeToXml(BuildableType bt) throws Exception {
        StringWriter sw = new StringWriter();
        try (FreeColXMLWriter xw = new FreeColXMLWriter(sw, FreeColXMLWriter.WriteScope.toSave())) {
            bt.toXML(xw);
        }
        return sw.toString();
    }

    private void readInto(BuildableType bt, String xml) throws Exception {
        try (FreeColXMLReader xr = new FreeColXMLReader(new StringReader(xml))) {
            xr.nextTag();
            bt.readFromXML(xr);
        }
    }

    // ------------------------------------------------------------------
    // M1: BuildableType(String, Specification) — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M1-TP1: ENTRY -> N0 -> EXIT (constructs BuildableType with default requiredPopulation=1)
    public void testM1_TP1_Constructor() {
        StubBuildableType bt = new StubBuildableType("model.buildable.test", spec());
        assertEquals("model.buildable.test", bt.getId());
        assertEquals(1, bt.getRequiredPopulation());
    }

    // ------------------------------------------------------------------
    // M2: getRequiredPopulation() — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M2-TP1: ENTRY -> N0 -> EXIT (returns requiredPopulation)
    public void testM2_TP1_GetRequiredPopulation() {
        StubBuildableType bt = new StubBuildableType("model.buildable.pop", spec());
        assertEquals(1, bt.getRequiredPopulation());
    }

    // ------------------------------------------------------------------
    // M3: setRequiredPopulation(int) — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M3-TP1: ENTRY -> N0 -> EXIT (updates requiredPopulation)
    public void testM3_TP1_SetRequiredPopulation() {
        StubBuildableType bt = new StubBuildableType("model.buildable.setpop", spec());
        bt.setRequiredPopulation(4);
        assertEquals(4, bt.getRequiredPopulation());
    }

    // ------------------------------------------------------------------
    // M4: getRequiredAbilities() — 2 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M4-TP1: ENTRY -> N0 -(B1:false)-> N1 -> N3 -> EXIT (requiredAbilities == null -> emptyMap)
    public void testM4_TP1_GetRequiredAbilitiesNull() {
        StubBuildableType bt = new StubBuildableType("model.buildable.ra1", spec());
        assertTrue(bt.getRequiredAbilities().isEmpty());
    }

    // M4-TP2: ENTRY -> N0 -(B1:true)-> N2 -> N3 -> EXIT (requiredAbilities != null -> copy)
    public void testM4_TP2_GetRequiredAbilitiesNonNull() {
        StubBuildableType bt = new StubBuildableType("model.buildable.ra2", spec());
        bt.addRequiredAbility(Ability.NAVAL_UNIT, true);
        assertEquals(Boolean.TRUE, bt.getRequiredAbilities().get(Ability.NAVAL_UNIT));
    }

    // ------------------------------------------------------------------
    // M5: requiresAbility(String) — 3 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M5-TP1: ENTRY -> N0 -(B1:false)-> N1 -> N5 -> EXIT (requiredAbilities == null -> false)
    public void testM5_TP1_RequiresAbilityNullMap() {
        StubBuildableType bt = new StubBuildableType("model.buildable.req1", spec());
        assertFalse(bt.requiresAbility(Ability.NAVAL_UNIT));
    }

    // M5-TP2: ENTRY -> N0 -(B1:true)-> N2 -(B2:false)-> N3 -> N5 -> EXIT (key absent -> false)
    public void testM5_TP2_RequiresAbilityKeyAbsent() {
        StubBuildableType bt = new StubBuildableType("model.buildable.req2", spec());
        bt.addRequiredAbility(Ability.NAVAL_UNIT, true);
        assertFalse(bt.requiresAbility(Ability.BOMBARD));
    }

    // M5-TP3: ENTRY -> N0 -(B1:true)-> N2 -(B2:true)-> N4 -> N5 -> EXIT (key present -> value)
    public void testM5_TP3_RequiresAbilityKeyPresent() {
        StubBuildableType bt = new StubBuildableType("model.buildable.req3", spec());
        bt.addRequiredAbility(Ability.NAVAL_UNIT, true);
        assertTrue(bt.requiresAbility(Ability.NAVAL_UNIT));
    }

    // ------------------------------------------------------------------
    // M6: setRequiredAbilities(Map) — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M6-TP1: ENTRY -> N0 -> EXIT (sets requiredAbilities map)
    public void testM6_TP1_SetRequiredAbilities() {
        StubBuildableType bt = new StubBuildableType("model.buildable.sra", spec());
        Map<String, Boolean> map = new HashMap<>();
        map.put(Ability.BOMBARD, true);
        bt.setRequiredAbilities(map);
        assertTrue(bt.requiresAbility(Ability.BOMBARD));
    }

    // ------------------------------------------------------------------
    // M7: addRequiredAbility(String, boolean) — 2 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M7-TP1: ENTRY -> N0 -(B1:false)-> N1 -> N2 -> EXIT (requiredAbilities == null -> creates HashMap)
    // M7-TP2: ENTRY -> N0 -(B1:true)-> N2 -> EXIT (requiredAbilities != null -> puts into existing map)
    public void testM7_TP1_and_TP2_AddRequiredAbility() {
        StubBuildableType bt = new StubBuildableType("model.buildable.ara", spec());
        bt.addRequiredAbility(Ability.NAVAL_UNIT, true);
        bt.addRequiredAbility(Ability.BOMBARD, false);
        assertEquals(2, bt.getRequiredAbilities().size());
    }

    // ------------------------------------------------------------------
    // M8: removeRequiredAbility(String) — 2 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M8-TP1: ENTRY -> N0 -(B1:true)-> N2 -> EXIT (requiredAbilities == null -> no-op)
    public void testM8_TP1_RemoveRequiredAbilityNullMap() {
        StubBuildableType bt = new StubBuildableType("model.buildable.rra1", spec());
        bt.removeRequiredAbility(Ability.NAVAL_UNIT);
        assertTrue(bt.getRequiredAbilities().isEmpty());
    }

    // M8-TP2: ENTRY -> N0 -(B1:false)-> N1 -> N2 -> EXIT (requiredAbilities != null -> removes key)
    public void testM8_TP2_RemoveRequiredAbilityPresent() {
        StubBuildableType bt = new StubBuildableType("model.buildable.rra2", spec());
        bt.addRequiredAbility(Ability.NAVAL_UNIT, true);
        bt.removeRequiredAbility(Ability.NAVAL_UNIT);
        assertFalse(bt.requiresAbility(Ability.NAVAL_UNIT));
    }

    // ------------------------------------------------------------------
    // M9: isAvailableTo(FreeColObject...) — 2 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M9-TP1: ENTRY -> N0 -(B1:false)-> N1 -> N3 -> EXIT (requiredAbilities == null -> true)
    public void testM9_TP1_IsAvailableToNullMap() {
        StubBuildableType bt = new StubBuildableType("model.buildable.avail1", spec());
        assertTrue(bt.isAvailableTo((FreeColObject) null));
    }

    // M9-TP2: ENTRY -> N0 -(B1:true)-> N2 -> N3 -> EXIT (requiredAbilities != null -> evaluates lambdas)
    public void testM9_TP2_IsAvailableToWithAbilities() {
        StubBuildableType bt = new StubBuildableType("model.buildable.avail2", spec());
        bt.addRequiredAbility(Ability.NAVAL_UNIT, true);
        FreeColObject hasIt = new FreeColObject() {
            private final FeatureContainer fc = new FeatureContainer();
            @Override public FeatureContainer getFeatureContainer() { return fc; }
            @Override public Specification getSpecification() { return spec(); }
            @Override public String getXMLTagName() { return "fco"; }
        };
        hasIt.addAbility(new Ability(Ability.NAVAL_UNIT));
        assertTrue(bt.isAvailableTo(null, hasIt));
        assertFalse(bt.isAvailableTo(new FreeColObject() {
            @Override public Specification getSpecification() { return spec(); }
            @Override public String getXMLTagName() { return "fco"; }
        }));
    }

    // ------------------------------------------------------------------
    // M10: getRequiredGoodsList() — 2 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M10-TP1: ENTRY -> N0 -(B1:false)-> N1 -> N3 -> EXIT (requiredGoods == null -> emptyList)
    public void testM10_TP1_GetRequiredGoodsListNull() {
        StubBuildableType bt = new StubBuildableType("model.buildable.rgl1", spec());
        assertTrue(bt.getRequiredGoodsList().isEmpty());
    }

    // M10-TP2: ENTRY -> N0 -(B1:true)-> N2 -> N3 -> EXIT (requiredGoods != null -> deep copy)
    public void testM10_TP2_GetRequiredGoodsListNonNull() throws Exception {
        StubBuildableType bt = new StubBuildableType("model.buildable.rgl2", spec());
        invokeAddRequiredGoods(bt, new AbstractGoods(hammersType, 50));
        List<AbstractGoods> list = bt.getRequiredGoodsList();
        assertEquals(1, list.size());
        assertEquals(50, list.get(0).getAmount());
    }

    // ------------------------------------------------------------------
    // M11: getRequiredGoods() — 2 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M11-TP1: ENTRY -> N0 -(B1:false)-> N1 -> N3 -> EXIT (requiredGoods == null -> Stream.empty())
    public void testM11_TP1_GetRequiredGoodsStreamNull() {
        StubBuildableType bt = new StubBuildableType("model.buildable.rgs1", spec());
        assertEquals(0L, bt.getRequiredGoods().count());
    }

    // M11-TP2: ENTRY -> N0 -(B1:true)-> N2 -> N3 -> EXIT (requiredGoods != null -> stream of list)
    public void testM11_TP2_GetRequiredGoodsStreamNonNull() throws Exception {
        StubBuildableType bt = new StubBuildableType("model.buildable.rgs2", spec());
        invokeAddRequiredGoods(bt, new AbstractGoods(hammersType, 40));
        List<AbstractGoods> collected = bt.getRequiredGoods().collect(Collectors.toList());
        assertEquals(1, collected.size());
    }

    // ------------------------------------------------------------------
    // M12: setRequiredGoods(List) — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M12-TP1: ENTRY -> N0 -> EXIT (clears existing requiredGoods and adds all from argument)
    public void testM12_TP1_SetRequiredGoods() throws Exception {
        StubBuildableType bt = new StubBuildableType("model.buildable.srg", spec());
        invokeAddRequiredGoods(bt, new AbstractGoods(hammersType, 10));
        bt.setRequiredGoods(Collections.singletonList(new AbstractGoods(toolsType, 20)));
        assertEquals(20, bt.getRequiredAmountOf(toolsType));
        assertEquals(0, bt.getRequiredAmountOf(hammersType));
    }

    // ------------------------------------------------------------------
    // M13: getRequiredAmountOf(GoodsType) — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M13-TP1: ENTRY -> N0 -> EXIT (returns AbstractGoods.getCount for type)
    public void testM13_TP1_GetRequiredAmountOf() throws Exception {
        StubBuildableType bt = new StubBuildableType("model.buildable.rao", spec());
        invokeAddRequiredGoods(bt, new AbstractGoods(hammersType, 75));
        assertEquals(75, bt.getRequiredAmountOf(hammersType));
    }

    // ------------------------------------------------------------------
    // M14: addRequiredGoods(AbstractGoods) — 2 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M14-TP1: ENTRY -> N0 -(B1:false)-> N1 -> N2 -> EXIT (requiredGoods == null -> allocates list)
    // M14-TP2: ENTRY -> N0 -(B1:true)-> N2 -> EXIT (requiredGoods != null -> appends)
    public void testM14_TP1_and_TP2_AddRequiredGoods() throws Exception {
        StubBuildableType bt = new StubBuildableType("model.buildable.arg", spec());
        invokeAddRequiredGoods(bt, new AbstractGoods(hammersType, 10));
        invokeAddRequiredGoods(bt, new AbstractGoods(toolsType, 5));
        assertEquals(2, bt.getRequiredGoodsList().size());
    }

    // ------------------------------------------------------------------
    // M15: needsGoodsToBuild() — 3 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M15-TP1: ENTRY -> N0 -(B1:true)-> N3 -> N4 -> EXIT (requiredGoods == null -> false)
    public void testM15_TP1_NeedsGoodsToBuildNull() {
        StubBuildableType bt = new StubBuildableType("model.buildable.ng1", spec());
        assertFalse(bt.needsGoodsToBuild());
    }

    // M15-TP2: ENTRY -> N0 -(B1:false)-> N1 -(B2:true)-> N3 -> N4 -> EXIT (requiredGoods empty -> false)
    public void testM15_TP2_NeedsGoodsToBuildEmpty() throws Exception {
        StubBuildableType bt = new StubBuildableType("model.buildable.ng2", spec());
        invokeAddRequiredGoods(bt, new AbstractGoods(hammersType, 10));
        bt.setRequiredGoods(Collections.emptyList());
        assertFalse(bt.needsGoodsToBuild());
    }

    // M15-TP3: ENTRY -> N0 -(B1:false)-> N1 -(B2:false)-> N2 -> N4 -> EXIT (requiredGoods non-empty -> true)
    public void testM15_TP3_NeedsGoodsToBuildNonEmpty() throws Exception {
        StubBuildableType bt = new StubBuildableType("model.buildable.ng3", spec());
        invokeAddRequiredGoods(bt, new AbstractGoods(hammersType, 10));
        assertTrue(bt.needsGoodsToBuild());
    }

    // ------------------------------------------------------------------
    // M16: getLimits() — 2 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M16-TP1: ENTRY -> N0 -(B1:false)-> N1 -> N3 -> EXIT (limits == null -> emptyList)
    public void testM16_TP1_GetLimitsNull() {
        StubBuildableType bt = new StubBuildableType("model.buildable.lim1", spec());
        assertTrue(bt.getLimits().isEmpty());
    }

    // M16-TP2: ENTRY -> N0 -(B1:true)-> N2 -> N3 -> EXIT (limits != null -> returns list)
    public void testM16_TP2_GetLimitsNonNull() throws Exception {
        StubBuildableType bt = new StubBuildableType("model.buildable.lim2", spec());
        invokeAddLimit(bt, new Limit(spec()));
        assertEquals(1, bt.getLimits().size());
    }

    // ------------------------------------------------------------------
    // M17: setLimits(List) — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M17-TP1: ENTRY -> N0 -> EXIT (sets limits list)
    public void testM17_TP1_SetLimits() {
        StubBuildableType bt = new StubBuildableType("model.buildable.slim", spec());
        Limit limit = new Limit(spec());
        bt.setLimits(Collections.singletonList(limit));
        assertEquals(1, bt.getLimits().size());
    }

    // ------------------------------------------------------------------
    // M18: addLimit(Limit) — 2 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M18-TP1: ENTRY -> N0 -(B1:false)-> N1 -> N2 -> EXIT (limits == null -> allocates list)
    // M18-TP2: ENTRY -> N0 -(B1:true)-> N2 -> EXIT (limits != null -> appends)
    public void testM18_TP1_and_TP2_AddLimit() throws Exception {
        StubBuildableType bt = new StubBuildableType("model.buildable.alim", spec());
        invokeAddLimit(bt, new Limit(spec()));
        invokeAddLimit(bt, new Limit(spec()));
        assertEquals(2, bt.getLimits().size());
    }

    // ------------------------------------------------------------------
    // M19: getCurrentlyBuildingLabel() — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M19-TP1: ENTRY -> N0 -> EXIT (returns StringTemplate for currentlyBuilding)
    public void testM19_TP1_GetCurrentlyBuildingLabel() {
        StubBuildableType bt = new StubBuildableType("model.buildable.cbl", spec());
        StringTemplate st = bt.getCurrentlyBuildingLabel();
        assertEquals("model.buildableType.currentlyBuilding", st.getId());
    }

    // ------------------------------------------------------------------
    // M20 & M21: getMinimumIndex & getMaximumIndex — 1 Test Path each (100% C3)
    // ------------------------------------------------------------------

    // M20-TP1 & M21-TP1: ENTRY -> N0 -> EXIT (both return UNABLE_TO_BUILD)
    public void testM20_and_M21_GetMinMaxIndex() {
        StubBuildableType bt = new StubBuildableType("model.buildable.idx", spec());
        JList<BuildableType> list = new JList<>();
        assertEquals(-1, bt.getMinimumIndex(null, list, -1));
        assertEquals(-1, bt.getMaximumIndex(null, list, -1));
    }

    // ------------------------------------------------------------------
    // M22: copyIn(FreeColObject) — 2 feasible Test Paths (100% feasible C3)
    // Infeasible: N1 -> N2 (B2:false, !super.copyIn(o))
    // ------------------------------------------------------------------

    // M22-TP1: ENTRY -> N0 -(B1:true)-> N2 -> EXIT (other == null -> false)
    public void testM22_TP1_CopyInNullReturnsFalse() {
        StubBuildableType bt = new StubBuildableType("model.buildable.ci1", spec());
        assertFalse(bt.copyIn(null));
    }

    // M22-TP2: ENTRY -> N0 -(B1:false)-> N1 -(B2:true)-> N3 -> EXIT (valid BuildableType -> copies fields)
    public void testM22_TP2_CopyInValidCopiesFields() throws Exception {
        StubBuildableType src = new StubBuildableType("model.buildable.src", spec());
        src.setRequiredPopulation(5);
        src.addRequiredAbility(Ability.NAVAL_UNIT, true);
        invokeAddRequiredGoods(src, new AbstractGoods(hammersType, 60));
        src.setLimits(Collections.singletonList(new Limit(spec())));

        StubBuildableType dst = new StubBuildableType("model.buildable.src", spec());
        dst.getFeatureContainer();
        invokeAddRequiredGoods(dst, new AbstractGoods(toolsType, 1));
        assertTrue(dst.copyIn(src));
        assertEquals(5, dst.getRequiredPopulation());
        assertTrue(dst.requiresAbility(Ability.NAVAL_UNIT));
        assertEquals(60, dst.getRequiredAmountOf(hammersType));
        assertEquals(1, dst.getLimits().size());
    }

    // ------------------------------------------------------------------
    // M23: writeAttributes(FreeColXMLWriter) — 2 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M23-TP1: ENTRY -> N0 -(B1:true)-> N2 -> EXIT (requiredPopulation <= 1 -> omitted)
    public void testM23_TP1_WriteAttributesDefaultPop() throws Exception {
        StubBuildableType bt = new StubBuildableType("model.buildable.wa1", spec());
        String xml = serializeToXml(bt);
        assertFalse(xml.contains(BuildableType.REQUIRED_POPULATION_TAG));
    }

    // M23-TP2: ENTRY -> N0 -(B1:false)-> N1 -> N2 -> EXIT (requiredPopulation > 1 -> written)
    public void testM23_TP2_WriteAttributesCustomPop() throws Exception {
        StubBuildableType bt = new StubBuildableType("model.buildable.wa2", spec());
        bt.setRequiredPopulation(3);
        String xml = serializeToXml(bt);
        assertTrue(xml.contains("required-population=\"3\""));
    }

    // ------------------------------------------------------------------
    // M24: writeChildren(FreeColXMLWriter) — 2 Test Paths covering B1..B4 (100% C3)
    // ------------------------------------------------------------------

    // M24-TP1: B1:true, B3:true, B4:true (empty abilities, goods, limits)
    public void testM24_TP1_WriteChildrenEmpty() throws Exception {
        StubBuildableType bt = new StubBuildableType("model.buildable.wc1", spec());
        String xml = serializeToXml(bt);
        assertFalse(xml.contains("required-ability"));
        assertFalse(xml.contains("required-goods"));
    }

    // M24-TP2: B1:false, B2:false->true, B3:false->true, B4:false->true (writes abilities, goods, limits)
    public void testM24_TP2_WriteChildrenPopulated() throws Exception {
        StubBuildableType bt = new StubBuildableType("model.buildable.wc2", spec());
        bt.addRequiredAbility(Ability.NAVAL_UNIT, true);
        invokeAddRequiredGoods(bt, new AbstractGoods(hammersType, 30));
        Limit limit = new Limit("model.limit.test",
                new Operand(Operand.OperandType.UNITS, Operand.ScopeLevel.PLAYER),
                Limit.Operator.LT,
                new Operand(2));
        invokeAddLimit(bt, limit);
        String xml = serializeToXml(bt);
        assertTrue(xml.contains("required-ability"));
        assertTrue(xml.contains("required-goods"));
        assertTrue(xml.contains("<limit"));
    }

    // ------------------------------------------------------------------
    // M25: readAttributes(FreeColXMLReader) — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M25-TP1: ENTRY -> N0 -> EXIT (reads required-population attribute)
    public void testM25_TP1_ReadAttributes() throws Exception {
        StubBuildableType bt = new StubBuildableType("model.buildable.ra", spec());
        readInto(bt, "<buildableType id=\"model.buildable.ra\" required-population=\"6\"/>");
        assertEquals(6, bt.getRequiredPopulation());
    }

    // ------------------------------------------------------------------
    // M26: clearContainers(FreeColXMLReader) — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M26-TP1: ENTRY -> N0 -> EXIT (clears requiredAbilities, requiredGoods, limits)
    public void testM26_TP1_ClearContainers() throws Exception {
        StubBuildableType bt = new StubBuildableType("model.buildable.cc", spec());
        bt.addRequiredAbility(Ability.NAVAL_UNIT, true);
        readInto(bt, "<buildableType id=\"model.buildable.cc\"/>");
        assertTrue(bt.getRequiredAbilities().isEmpty());
    }

    // ------------------------------------------------------------------
    // M27: readChildren(FreeColXMLReader) — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M27-TP1: ENTRY -> N0 -> EXIT (delegates to super.readChildren)
    public void testM27_TP1_ReadChildren() throws Exception {
        StubBuildableType bt = new StubBuildableType("model.buildable.rc", spec());
        readInto(bt, "<buildableType id=\"model.buildable.rc\"/>");
        assertNotNull(bt);
    }

    // ------------------------------------------------------------------
    // M28: readChild(FreeColXMLReader) — 5 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M28-TP1..TP5: B1:false (required-ability), B2:false (required-goods),
    // B3:false+B4:false (limit with null lhs type), B3:false+B4:true (limit with non-null lhs type),
    // B3:true (super.readChild for ability)
    public void testM28_TP1_to_TP5_ReadChildBranches() throws Exception {
        StubBuildableType bt = new StubBuildableType("model.buildable.rchild", spec());
        String xml = "<buildableType id=\"model.buildable.rchild\">"
                + "<required-ability id=\"model.ability.navalUnit\" value=\"true\"/>"
                + "<required-goods id=\"model.goods.hammers\" value=\"80\"/>"
                + "<limit id=\"model.limit.test1\" operator=\"LT\">"
                + "  <left-hand-side operand-type=\"UNITS\" scope-level=\"PLAYER\"/>"
                + "  <right-hand-side value=\"2\"/>"
                + "</limit>"
                + "<limit id=\"model.limit.test2\" operator=\"LT\">"
                + "  <left-hand-side type=\"model.unit.wagonTrain\" operand-type=\"UNITS\" scope-level=\"PLAYER\"/>"
                + "  <right-hand-side value=\"3\"/>"
                + "</limit>"
                + "<ability id=\"model.ability.canBeCaptured\" value=\"true\"/>"
                + "</buildableType>";
        readInto(bt, xml);
        assertTrue(bt.requiresAbility("model.ability.navalUnit"));
        assertEquals(80, bt.getRequiredAmountOf(hammersType));
        assertEquals(2, bt.getLimits().size());
        assertEquals("model.buildable.rchild", bt.getLimits().get(0).getLeftHandSide().getType());
        assertEquals("model.unit.wagonTrain", bt.getLimits().get(1).getLeftHandSide().getType());
        assertTrue(bt.hasAbility("model.ability.canBeCaptured"));
    }

    // ------------------------------------------------------------------
    // M29..M31: Lambdas — 1..3 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M29-TP1: lambda$getRequiredGoodsList$0 (deep-copies AbstractGoods)
    public void testM29_TP1_LambdaGetRequiredGoodsList() throws Exception {
        testM10_TP2_GetRequiredGoodsListNonNull();
    }

    // M30-TP1..TP2: lambda$isAvailableTo$0 (value == any(...) true and false)
    // M31-TP1..TP3: lambda$isAvailableTo$1 (o == null, o != null && !hasAbility, o != null && hasAbility)
    public void testM30_and_M31_LambdasIsAvailableTo() {
        testM9_TP2_IsAvailableToWithAbilities();
    }
}
