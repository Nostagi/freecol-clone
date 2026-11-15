package net.sf.freecol.common.model;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.sf.freecol.common.io.FreeColXMLReader;
import net.sf.freecol.util.test.FreeColTestCase;


public class FeatureCFTTest extends FreeColTestCase {

    private Specification specification;
    private TestFeature feature;

    private static class TestFeature extends Feature {

        TestFeature(Specification specification) {
            super(specification);
        }

        @Override
        public String getXMLTagName() {
            return "testFeature";
        }

        public boolean testAppliesTo(Turn turn) {
            return appliesTo(turn);
        }

        public boolean testAppliesTo(FreeColSpecObjectType type,
                                     Turn turn) {
            return appliesTo(type, turn);
        }
    }


    @Override
    protected void setUp() throws Exception {
        super.setUp();
        specification = getGame().getSpecification();
        feature = new TestFeature(specification);
        feature.setId("test.feature");
    }


    private TestFeature newFeature() {
        TestFeature result = new TestFeature(specification);
        result.setId("test.feature");
        return result;
    }


    private Scope newScope(String type) {
        Scope scope = new Scope();
        scope.setType(type);
        return scope;
    }


    private TestFeature readFeature(String xml) throws Exception {
        FreeColXMLReader xr =
            new FreeColXMLReader(new StringReader(xml));
        try {
            xr.nextTag();
            TestFeature result = newFeature();
            result.readFromXML(xr);
            return result;
        } finally {
            xr.close();
        }
    }


    public void testT01_constructor() {
        TestFeature result = new TestFeature(specification);

        assertNotNull(result);
        assertEquals(0, result.getDuration());
        assertFalse(result.isTemporary());
        assertNull(result.getSource());
        assertNull(result.getFirstTurn());
        assertNull(result.getLastTurn());
    }


    public void testT02_hasTimeLimitWithoutTurns() {
        assertFalse(feature.hasTimeLimit());
    }


    public void testT03_hasTimeLimitWithFirstTurn() {
        feature.setFirstTurn(new Turn(10));
        assertTrue(feature.hasTimeLimit());
    }


    public void testT04_hasTimeLimitWithLastTurn() {
        feature.setLastTurn(new Turn(20));
        assertTrue(feature.hasTimeLimit());
    }


    public void testT05_getFirstTurn() {
        Turn turn = new Turn(10);
        feature.setFirstTurn(turn);
        assertSame(turn, feature.getFirstTurn());
    }


    public void testT06_getLastTurn() {
        Turn turn = new Turn(20);
        feature.setLastTurn(turn);
        assertSame(turn, feature.getLastTurn());
    }


    public void testT07_setFirstTurnNull() {
        feature.setFirstTurn(new Turn(10));
        feature.setFirstTurn(null);

        assertNull(feature.getFirstTurn());
        assertFalse(feature.hasTimeLimit());
    }


    public void testT08_setLastTurnNull() {
        feature.setLastTurn(new Turn(20));
        feature.setLastTurn(null);

        assertNull(feature.getLastTurn());
        assertFalse(feature.hasTimeLimit());
    }


    public void testT09_getSourceAndSetSource() {
        StringTemplate source = StringTemplate.key("test.source");
        feature.setSource(source);
        assertSame(source, feature.getSource());
    }


    public void testT10_setSourceNull() {
        feature.setSource(StringTemplate.key("test.source"));
        feature.setSource(null);
        assertNull(feature.getSource());
    }


    public void testT11_getDurationAndSetDuration() {
        feature.setDuration(5);
        assertEquals(5, feature.getDuration());

        feature.setDuration(-1);
        assertEquals(-1, feature.getDuration());
    }


    public void testT12_setDurationZero() {
        feature.setDuration(10);
        feature.setDuration(0);
        assertEquals(0, feature.getDuration());
    }


    public void testT13_isTemporaryAndSetTemporary() {
        assertFalse(feature.isTemporary());

        feature.setTemporary(true);
        assertTrue(feature.isTemporary());

        feature.setTemporary(false);
        assertFalse(feature.isTemporary());
    }


    public void testT14_appliesToNullTurn() {
        assertTrue(feature.testAppliesTo((Turn)null));
    }


    public void testT15_appliesToTurnWithoutLimits() {
        assertTrue(feature.testAppliesTo(new Turn(10)));
    }


    public void testT16_appliesToBeforeFirstTurn() {
        feature.setFirstTurn(new Turn(10));
        assertFalse(feature.testAppliesTo(new Turn(9)));
    }


    public void testT17_appliesToAtFirstTurn() {
        feature.setFirstTurn(new Turn(10));
        assertTrue(feature.testAppliesTo(new Turn(10)));
    }


    public void testT18_appliesToAfterFirstTurn() {
        feature.setFirstTurn(new Turn(10));
        assertTrue(feature.testAppliesTo(new Turn(11)));
    }


    public void testT19_appliesToAfterLastTurn() {
        feature.setLastTurn(new Turn(20));
        assertFalse(feature.testAppliesTo(new Turn(21)));
    }


    public void testT20_appliesToAtLastTurn() {
        feature.setLastTurn(new Turn(20));
        assertTrue(feature.testAppliesTo(new Turn(20)));
    }


    public void testT21_appliesToBeforeLastTurn() {
        feature.setLastTurn(new Turn(20));
        assertTrue(feature.testAppliesTo(new Turn(19)));
    }


    public void testT22_appliesToWithinBothLimits() {
        feature.setFirstTurn(new Turn(10));
        feature.setLastTurn(new Turn(20));
        assertTrue(feature.testAppliesTo(new Turn(15)));
    }


    public void testT23_appliesToBeforeBothLimits() {
        feature.setFirstTurn(new Turn(10));
        feature.setLastTurn(new Turn(20));
        assertFalse(feature.testAppliesTo(new Turn(5)));
    }


    public void testT24_appliesToAfterBothLimits() {
        feature.setFirstTurn(new Turn(10));
        feature.setLastTurn(new Turn(20));
        assertFalse(feature.testAppliesTo(new Turn(25)));
    }


    public void testT25_appliesToTypeWithNullTurn() {
        assertTrue(feature.testAppliesTo((FreeColSpecObjectType)null,
                                         null));
    }


    public void testT26_appliesToTypeWithValidTurn() {
        assertTrue(feature.testAppliesTo((FreeColSpecObjectType)null,
                                         new Turn(10)));
    }


    public void testT27_appliesToTypeWithInvalidTurn() {
        feature.setFirstTurn(new Turn(10));
        assertFalse(feature.testAppliesTo((FreeColSpecObjectType)null,
                                          new Turn(9)));
    }


    public void testT28_isOutOfDateNullTurn() {
        feature.setLastTurn(new Turn(10));
        assertFalse(feature.isOutOfDate(null));
    }


    public void testT29_isOutOfDateWithoutLastTurn() {
        assertFalse(feature.isOutOfDate(new Turn(20)));
    }


    public void testT30_isOutOfDateBeforeLastTurn() {
        feature.setLastTurn(new Turn(20));
        assertFalse(feature.isOutOfDate(new Turn(19)));
    }


    public void testT31_isOutOfDateAtLastTurn() {
        feature.setLastTurn(new Turn(20));
        assertFalse(feature.isOutOfDate(new Turn(20)));
    }


    public void testT32_isOutOfDateAfterLastTurn() {
        feature.setLastTurn(new Turn(20));
        assertTrue(feature.isOutOfDate(new Turn(21)));
    }


    public void testT33_isIndependentWithoutSource() {
        assertTrue(feature.isIndependent());
    }


    public void testT34_isIndependentWithGenericSource() {
        feature.setSource(StringTemplate.key("test.source"));
        assertTrue(feature.isIndependent());
    }


    public void testT35_isIndependentWithBuildingType() {
        feature.setSource(new BuildingType("test.building",
                                           specification));
        assertFalse(feature.isIndependent());
    }


    public void testT36_isIndependentWithFoundingFather() {
        feature.setSource(new FoundingFather("test.father",
                                             specification));
        assertFalse(feature.isIndependent());
    }


    public void testT37_isIndependentWithNationType() {
    NationType source = new NationType("test.nation",
                                       specification) {
        @Override
        public boolean isEuropean() {
            return false;
        }

        @Override
        public boolean isIndian() {
            return false;
        }

        @Override
        public boolean isREF() {
            return false;
        }

        @Override
        public String getXMLTagName() {
            return "nation-type";
        }
    };

    feature.setSource(source);
    assertFalse(feature.isIndependent());
}


    public void testT38_isIndependentWithSettlementType() {
        feature.setSource(new SettlementType("test.settlement",
                                             specification));
        assertFalse(feature.isIndependent());
    }


    public void testT39_getNameKey() {
        feature.setId("model.feature.test");
        assertNotNull(feature.getNameKey());
    }


    public void testT40_hasScopeWithoutScopes() {
        assertFalse(feature.hasScope());
    }


    public void testT41_getScopeListWithoutScopes() {
        assertNotNull(feature.getScopeList());
        assertTrue(feature.getScopeList().isEmpty());
    }


    public void testT42_getScopesWithoutScopes() {
        assertNotNull(feature.getScopes());
        assertFalse(feature.getScopes().findAny().isPresent());
    }


    public void testT43_copyScopesEmpty() {
        feature.copyScopes(Collections.<Scope>emptyList());

        assertFalse(feature.hasScope());
        assertTrue(feature.getScopeList().isEmpty());
    }


    public void testT44_addScope() {
        Scope scope = new Scope();
        feature.addScope(scope);

        assertTrue(feature.hasScope());
        assertEquals(1, feature.getScopeList().size());
        assertSame(scope, feature.getScopeList().get(0));
    }


    public void testT45_copyScopes() {
        Scope scope1 = new Scope();
        Scope scope2 = new Scope();

        List<Scope> scopes = new ArrayList<>();
        scopes.add(scope1);
        scopes.add(scope2);

        feature.copyScopes(scopes);

        assertTrue(feature.hasScope());
        assertEquals(2, feature.getScopeList().size());
    }


    public void testT46_appliesToObjectWithoutScopes() {
        assertTrue(feature.appliesTo(StringTemplate.key("test.object")));
    }


    public void testT47_appliesToObjectMatchingScope() {
        FreeColSpecObjectType type =
            specification.getTileTypeList().get(0);

        feature.addScope(newScope(type.getId()));

        assertTrue(feature.appliesTo(type));
    }


    public void testT48_appliesToObjectNotMatchingScope() {
        List<TileType> types = specification.getTileTypeList();
        assertTrue(types.size() > 1);

        TileType first = types.get(0);
        TileType second = types.get(1);

        feature.addScope(newScope(first.getId()));

        assertFalse(feature.appliesTo(second));
    }


    public void testT49_appliesToTypeAndTurnMatchingScope() {
        TileType type = specification.getTileTypeList().get(0);
        feature.addScope(newScope(type.getId()));

        assertTrue(feature.testAppliesTo(type, new Turn(10)));
    }


    public void testT50_appliesToTypeAndTurnNonMatchingScope() {
        List<TileType> types = specification.getTileTypeList();
        assertTrue(types.size() > 1);

        feature.addScope(newScope(types.get(0).getId()));

        assertFalse(feature.testAppliesTo(types.get(1), new Turn(10)));
    }


    public void testT51_appliesToTypeAndTurnInvalidTurn() {
        TileType type = specification.getTileTypeList().get(0);

        feature.addScope(newScope(type.getId()));
        feature.setFirstTurn(new Turn(10));

        assertFalse(feature.testAppliesTo(type, new Turn(9)));
    }


    public void testT52_copyInNull() {
        assertFalse(feature.copyIn(null));
    }


    public void testT53_copyInWrongType() {
        assertFalse(feature.copyIn(StringTemplate.key("test.object")));
    }


    public void testT54_copyInFeature() {
        TestFeature other = newFeature();
        other.setDuration(5);
        other.setTemporary(true);
        other.setFirstTurn(new Turn(10));
        other.setLastTurn(new Turn(20));
        other.setSource(StringTemplate.key("test.source"));

        assertTrue(feature.copyIn(other));

        assertEquals(5, feature.getDuration());
        assertTrue(feature.isTemporary());
        assertEquals(10, feature.getFirstTurn().getNumber());
        assertEquals(20, feature.getLastTurn().getNumber());
        assertSame(other.getSource(), feature.getSource());
    }


    public void testT55_copyInFeatureWithNullTurns() {
        TestFeature other = newFeature();

        assertTrue(feature.copyIn(other));

        assertNull(feature.getFirstTurn());
        assertNull(feature.getLastTurn());
    }


    public void testT56_copyInFeatureWithScopes() {
        TestFeature other = newFeature();
        other.addScope(new Scope());

        assertTrue(feature.copyIn(other));
        assertEquals(1, feature.getScopeList().size());
    }


    public void testT57_copyInDifferentId() {
        TestFeature other = new TestFeature(specification);
        other.setId("different.feature");

        assertFalse(feature.copyIn(other));
    }


    public void testT58_equalsSameObject() {
        assertTrue(feature.equals(feature));
    }


    public void testT59_equalsNull() {
        assertFalse(feature.equals(null));
    }


    public void testT60_equalsDifferentType() {
        assertFalse(feature.equals(StringTemplate.key("test")));
    }


    public void testT61_equalsDifferentSource() {
        TestFeature other = newFeature();

        feature.setSource(StringTemplate.key("source.one"));
        other.setSource(StringTemplate.key("source.two"));

        assertFalse(feature.equals(other));
    }


    public void testT62_equalsDifferentDuration() {
        TestFeature other = newFeature();

        feature.setDuration(1);

        assertFalse(feature.equals(other));
    }


    public void testT63_equalsDifferentTemporary() {
        TestFeature other = newFeature();

        feature.setTemporary(true);

        assertFalse(feature.equals(other));
    }


    public void testT64_equalsDifferentFirstTurnNull() {
        TestFeature other = newFeature();

        feature.setFirstTurn(new Turn(10));

        assertFalse(feature.equals(other));
    }


    public void testT65_equalsDifferentFirstTurnNumber() {
        TestFeature other = newFeature();

        feature.setFirstTurn(new Turn(10));
        other.setFirstTurn(new Turn(11));

        assertFalse(feature.equals(other));
    }


    public void testT66_equalsDifferentLastTurnNull() {
        TestFeature other = newFeature();

        feature.setLastTurn(new Turn(10));

        assertFalse(feature.equals(other));
    }


    public void testT67_equalsDifferentLastTurnNumber() {
        TestFeature other = newFeature();

        feature.setLastTurn(new Turn(10));
        other.setLastTurn(new Turn(11));

        assertFalse(feature.equals(other));
    }


    public void testT68_equalsDifferentScopes() {
        TestFeature other = newFeature();

        feature.addScope(newScope("type.one"));
        other.addScope(newScope("type.two"));

        assertFalse(feature.equals(other));
    }


    public void testT69_equalsOneScopeMissing() {
        TestFeature other = newFeature();

        feature.addScope(newScope("type.one"));

        assertFalse(feature.equals(other));
    }


    public void testT70_equalsSameScopes() {
        TestFeature other = newFeature();

        feature.addScope(newScope("type.one"));
        other.addScope(newScope("type.one"));

        assertTrue(feature.equals(other));
    }


    public void testT71_equalsSameValues() {
        TestFeature other = newFeature();

        feature.setDuration(5);
        other.setDuration(5);

        feature.setTemporary(true);
        other.setTemporary(true);

        feature.setFirstTurn(new Turn(10));
        other.setFirstTurn(new Turn(10));

        feature.setLastTurn(new Turn(20));
        other.setLastTurn(new Turn(20));

        assertTrue(feature.equals(other));
    }


    public void testT72_hashCode() {
        assertEquals(feature.hashCode(), feature.hashCode());
    }


    public void testT73_hashCodeWithFields() {
        feature.setSource(StringTemplate.key("test.source"));
        feature.setFirstTurn(new Turn(10));
        feature.setLastTurn(new Turn(20));
        feature.setDuration(5);
        feature.setTemporary(true);
        feature.addScope(new Scope());

        assertEquals(feature.hashCode(), feature.hashCode());
    }


    public void testT74_serializeWithoutOptionalAttributes() throws Exception {
        String xml = feature.serialize();

        assertNotNull(xml);
        assertTrue(xml.contains("testFeature"));
    }


    public void testT75_serializeWithAllOptionalAttributes() throws Exception {
        feature.setSource(StringTemplate.key("test.source"));
        feature.setFirstTurn(new Turn(10));
        feature.setLastTurn(new Turn(20));
        feature.setDuration(5);
        feature.setTemporary(true);
        feature.addScope(new Scope());

        String xml = feature.serialize();

        assertNotNull(xml);
        assertTrue(xml.contains("source="));
        assertTrue(xml.contains("firstTurn="));
        assertTrue(xml.contains("lastTurn="));
        assertTrue(xml.contains("duration="));
        assertTrue(xml.contains("temporary="));
        assertTrue(xml.contains("<scope"));
    }


    public void testT76_readAttributesWithoutOptionalAttributes()
        throws Exception {
        String xml = "<testFeature id=\"test.feature\"/>";

        TestFeature result = readFeature(xml);

        assertNull(result.getSource());
        assertNull(result.getFirstTurn());
        assertNull(result.getLastTurn());
        assertEquals(0, result.getDuration());
        assertFalse(result.isTemporary());
    }


    public void testT77_readAttributesWithOptionalAttributes()
        throws Exception {
        String xml = "<testFeature id=\"test.feature\""
            + " source=\"unknown.source\""
            + " firstTurn=\"10\""
            + " lastTurn=\"20\""
            + " duration=\"5\""
            + " temporary=\"true\"/>";

        TestFeature result = readFeature(xml);

        assertEquals(10, result.getFirstTurn().getNumber());
        assertEquals(20, result.getLastTurn().getNumber());
        assertEquals(5, result.getDuration());
        assertTrue(result.isTemporary());
    }


    public void testT78_readChildrenWithScope() throws Exception {
        String xml = "<testFeature id=\"test.feature\">"
            + "<scope type=\"test.type\"/>"
            + "</testFeature>";

        TestFeature result = readFeature(xml);

        assertEquals(1, result.getScopeList().size());
        assertEquals("test.type",
                     result.getScopeList().get(0).getType());
    }


    public void testT79_readChildrenWithoutScope() throws Exception {
        String xml = "<testFeature id=\"test.feature\"/>";

        TestFeature result = readFeature(xml);

        assertTrue(result.getScopeList().isEmpty());
    }


    public void testT80_getXMLTagName() {
        assertEquals("testFeature", feature.getXMLTagName());
    }
}