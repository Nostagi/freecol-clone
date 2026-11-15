
package net.sf.freecol.common.model;

import java.io.StringReader;
import net.sf.freecol.common.io.FreeColXMLReader;
import net.sf.freecol.server.model.ServerUnit;
import net.sf.freecol.util.test.FreeColTestCase;

/**
 * Tests for the Scope class.
 *
 * The tests are written in the JUnit 3 style used by FreeCol.
 */
public class ScopeCFTTest extends FreeColTestCase {

    private final UnitType carpenter
        = spec().getUnitType("model.unit.masterCarpenter");
    private final UnitType frigate
        = spec().getUnitType("model.unit.frigate");


    public void testEmptyScope() {
        Scope scope = new Scope();

        assertTrue(scope.appliesTo(carpenter));
        assertTrue(scope.appliesTo(frigate));

        scope.setMatchNegated(true);
        assertFalse(scope.appliesTo(carpenter));
        assertFalse(scope.appliesTo(frigate));
    }


    public void testTypeScope() {
        Scope scope = new Scope();

        scope.setType("model.unit.frigate");
        assertTrue(scope.appliesTo(frigate));
        assertFalse(scope.appliesTo(carpenter));

        scope.setMatchNegated(true);
        assertFalse(scope.appliesTo(frigate));
        assertTrue(scope.appliesTo(carpenter));

        scope.setMatchNegated(false);
        scope.setType("model.unit.masterCarpenter");
        assertFalse(scope.appliesTo(frigate));
        assertTrue(scope.appliesTo(carpenter));

        scope.setMatchNegated(true);
        assertTrue(scope.appliesTo(frigate));
        assertFalse(scope.appliesTo(carpenter));
    }


    public void testAbilityScope() {
        Scope scope = new Scope();

        scope.setAbilityId(Ability.NAVAL_UNIT);
        assertEquals(frigate.hasAbility(Ability.NAVAL_UNIT),
                     scope.appliesTo(frigate));
        assertEquals(carpenter.hasAbility(Ability.NAVAL_UNIT),
                     scope.appliesTo(carpenter));

        scope.setAbilityValue(false);
        assertFalse(scope.appliesTo(frigate));
        assertTrue(scope.appliesTo(carpenter));

        scope.setMatchNegated(true);
        assertTrue(scope.appliesTo(frigate));
        assertFalse(scope.appliesTo(carpenter));
    }


    public void testMethodScope() {
        Scope scope = new Scope();

        scope.setMethodName("getLineOfSight");
        scope.setMethodValue("1");

        assertTrue(frigate.getLineOfSight() != 1);
        assertFalse(scope.appliesTo(frigate));

        assertTrue(carpenter.getLineOfSight() == 1);
        assertTrue(scope.appliesTo(carpenter));

        scope.setMatchNegated(true);
        assertTrue(scope.appliesTo(frigate));
        assertFalse(scope.appliesTo(carpenter));
    }


    public void testCombinedScope() {
        Scope scope = new Scope();

        scope.setType("model.unit.frigate");
        scope.setAbilityId(Ability.NAVAL_UNIT);
        scope.setMethodName("getLineOfSight");
        scope.setMethodValue(String.valueOf(frigate.getLineOfSight()));

        assertTrue(scope.appliesTo(frigate));

        scope.setMethodValue("not-the-line-of-sight");
        assertFalse(scope.appliesTo(frigate));

        scope.setMethodValue(String.valueOf(frigate.getLineOfSight()));
        scope.setAbilityId(Ability.FOUND_COLONY);
        assertFalse(scope.appliesTo(frigate));

        scope.setMatchNegated(true);
        assertTrue(scope.appliesTo(frigate));
    }


    public void testMatchesNull() {
        Scope scope = new Scope();
        scope.setType("model.unit.frigate");

        assertTrue(scope.appliesTo(frigate));
        assertTrue(scope.appliesTo(null));

        scope.setMatchesNull(false);
        assertTrue(scope.appliesTo(frigate));
        assertFalse(scope.appliesTo(null));

        scope.setMatchNegated(true);
        assertFalse(scope.appliesTo(null));
    }


    public void testGameObjects() {
        Game game = getStandardGame();
        Map map = getTestMap();
        game.changeMap(map);

        Player dutch = game.getPlayerByNationId("model.nation.dutch");
        Tile unitTile = map.getTile(6, 8);
        UnitType colonistType
            = spec().getUnitType("model.unit.freeColonist");

        Unit unit = new ServerUnit(game, unitTile, dutch, colonistType);

        Scope scope = new Scope();
        scope.setAbilityId(Ability.FOUND_COLONY);
        assertTrue(scope.appliesTo(unit));

        scope.setType("model.unit.freeColonist");
        assertTrue(scope.appliesTo(unit));

        scope.setType("model.unit.hardyPioneer");
        assertFalse(scope.appliesTo(unit));

        scope.setMatchNegated(true);
        assertTrue(scope.appliesTo(unit));
    }


    public void testAppliesToNonSpecObjectWithNullType() {
        FreeColObject object = new FreeColObject() {
            @Override
            protected <T> T invokeMethod(String methodName,
                                         Class<T> returnClass,
                                         T defaultValue) {
                return defaultValue;
            }

            @Override
            public String getXMLTagName() {
                return "test";
            }
        };

        Scope scope = new Scope();
        scope.setType("model.unit.frigate");

        assertFalse(scope.appliesTo(object));

        scope.setMatchNegated(true);
        assertTrue(scope.appliesTo(object));
    }


    public void testGetKey() {
        Scope scope = new Scope();

        assertNull(scope.getKey());

        scope.setMethodName("getLineOfSight");
        assertEquals("scope.method.getLineOfSight", scope.getKey());

        scope.setAbilityId(Ability.NAVAL_UNIT);
        assertEquals(Ability.NAVAL_UNIT, scope.getKey());

        scope.setType("model.unit.frigate");
        assertEquals("model.unit.frigate", scope.getKey());
    }


    public void testGetFeatureString() {
        Scope scope = new Scope();

        assertEquals("", scope.getFeatureString());

        scope.setType("model.unit.frigate");
        String normal = scope.getFeatureString();
        assertNotNull(normal);
        assertFalse(normal.startsWith("!"));

        scope.setMatchNegated(true);
        String negated = scope.getFeatureString();
        assertTrue(negated.startsWith("!"));
    }


    public void testEquality() {
        Scope scope1 = new Scope();
        scope1.setType("model.unit.frigate");
        scope1.setAbilityId(Ability.NAVAL_UNIT);
        scope1.setMethodName("getLineOfSight");
        scope1.setMethodValue("2");
        scope1.setMatchesNull(true);
        scope1.setMatchNegated(false);

        Scope scope2 = new Scope();
        scope2.setType("model.unit.frigate");
        scope2.setAbilityId(Ability.NAVAL_UNIT);
        scope2.setMethodName("getLineOfSight");
        scope2.setMethodValue("2");
        scope2.setMatchesNull(true);
        scope2.setMatchNegated(false);

        assertTrue(scope1.equals(scope1));
        assertTrue(scope1.equals(scope2));
        assertTrue(scope2.equals(scope1));

        scope1.setType("model.unit.carpenter");
        assertFalse(scope1.equals(scope2));
        assertFalse(scope2.equals(scope1));

        scope1.setType("model.unit.frigate");
        scope1.setAbilityId(Ability.FOUND_COLONY);
        assertFalse(scope1.equals(scope2));
        assertFalse(scope2.equals(scope1));

        scope1.setAbilityId(Ability.NAVAL_UNIT);
        scope1.setAbilityValue(false);
        assertFalse(scope1.equals(scope2));
        assertFalse(scope2.equals(scope1));

        scope1.setAbilityValue(true);
        scope1.setMethodName("getOffence");
        assertFalse(scope1.equals(scope2));
        assertFalse(scope2.equals(scope1));

        scope1.setMethodName("getLineOfSight");
        scope1.setMethodValue("9");
        assertFalse(scope1.equals(scope2));
        assertFalse(scope2.equals(scope1));

        scope1.setMethodValue("2");
        scope1.setMatchesNull(false);
        assertFalse(scope1.equals(scope2));
        assertFalse(scope2.equals(scope1));

        scope1.setMatchesNull(true);
        scope1.setMatchNegated(true);
        assertFalse(scope1.equals(scope2));
        assertFalse(scope2.equals(scope1));

        scope1.setMatchNegated(false);
        assertTrue(scope1.equals(scope2));
        assertTrue(scope2.equals(scope1));

        assertFalse(scope1.equals(null));
        assertFalse(scope1.equals("not a Scope"));
    }


    public void testEqualityNullBranches() {
        Scope scope1 = new Scope();
        Scope scope2 = new Scope();

        assertTrue(scope1.equals(scope2));

        scope2.setType("model.unit.frigate");
        assertFalse(scope1.equals(scope2));
        assertFalse(scope2.equals(scope1));
        scope2.setType(null);

        scope2.setAbilityId(Ability.NAVAL_UNIT);
        assertFalse(scope1.equals(scope2));
        assertFalse(scope2.equals(scope1));
        scope2.setAbilityId(null);

        scope2.setMethodName("getLineOfSight");
        assertFalse(scope1.equals(scope2));
        assertFalse(scope2.equals(scope1));
        scope2.setMethodName(null);

        scope2.setMethodValue("1");
        assertFalse(scope1.equals(scope2));
        assertFalse(scope2.equals(scope1));
    }


    public void testCopyIn() {
        Scope source = new Scope();
        Scope target = new Scope();

        source.setId("scope.test");
        target.setId("scope.test");

        source.setType("model.unit.frigate");
        source.setAbilityId(Ability.NAVAL_UNIT);
        source.setAbilityValue(false);
        source.setMethodName("getLineOfSight");
        source.setMethodValue("2");
        source.setMatchesNull(false);
        source.setMatchNegated(true);

        assertTrue(target.copyIn(source));

        assertEquals(source.getType(), target.getType());
        assertEquals(source.getAbilityId(), target.getAbilityId());
        assertTrue(source.getAbilityValue() == target.getAbilityValue());
        assertEquals(source.getMethodName(), target.getMethodName());
        assertEquals(source.getMethodValue(), target.getMethodValue());

        // matchesNull is checked through the public appliesTo(null) behavior.
        assertFalse(source.appliesTo(null));
        assertFalse(target.appliesTo(null));

        assertTrue(source.getMatchNegated() == target.getMatchNegated());

        assertFalse(target.copyIn(null));

        Scope differentId = new Scope();
        differentId.setId("scope.other");
        assertFalse(target.copyIn(differentId));

        FreeColObject wrongClass = new FreeColObject() {
            @Override
            public String getXMLTagName() {
                return "test";
            }
        };

        wrongClass.setId("scope.test");
        assertFalse(target.copyIn(wrongClass));
    }


    public void testHashCode() {
        Scope scope1 = new Scope();
        Scope scope2 = new Scope();

        assertEquals(scope1.hashCode(), scope2.hashCode());

        scope1.setType("model.unit.frigate");
        scope1.setAbilityId(Ability.NAVAL_UNIT);
        scope1.setAbilityValue(false);
        scope1.setMethodName("getLineOfSight");
        scope1.setMethodValue("2");
        scope1.setMatchesNull(false);
        scope1.setMatchNegated(true);

        Scope scope3 = new Scope();
        scope3.setType("model.unit.frigate");
        scope3.setAbilityId(Ability.NAVAL_UNIT);
        scope3.setAbilityValue(false);
        scope3.setMethodName("getLineOfSight");
        scope3.setMethodValue("2");
        scope3.setMatchesNull(false);
        scope3.setMatchNegated(true);

        assertEquals(scope1.hashCode(), scope3.hashCode());
    }


    public void testToString() {
        Scope scope = new Scope();

        String result = scope.toString();
        assertTrue(result.contains("[Scope null"));
        assertTrue(result.contains("matches-null"));

        scope.setType("model.unit.frigate");
        scope.setAbilityId(Ability.NAVAL_UNIT);
        scope.setAbilityValue(false);
        scope.setMethodName("getLineOfSight");
        scope.setMethodValue("2");
        scope.setMatchesNull(false);
        scope.setMatchNegated(true);

        result = scope.toString();

        assertTrue(result.contains("model.unit.frigate"));
        assertTrue(result.contains(Ability.NAVAL_UNIT + "=false"));
        assertTrue(result.contains("getLineOfSight=2"));
        assertTrue(result.contains("match-negated"));
        assertFalse(result.contains("matches-null"));
    }


    /**
     * Tests all conditional branches in writeAttributes:
     * type, abilityId, methodName and methodValue.
     */
    public void testWriteAttributesBranches() throws Exception {
        // All optional attributes are null.
        Scope empty = new Scope();
        String emptyXml = empty.serialize();

        assertNotNull(emptyXml);
        assertTrue(emptyXml.contains("match-negated"));
        assertTrue(emptyXml.contains("matches-null"));
        assertFalse(emptyXml.contains("ability-id"));
        assertFalse(emptyXml.contains("method-name"));

        // All optional attributes are present.
        Scope full = new Scope();
        full.setType("model.unit.frigate");
        full.setAbilityId(Ability.NAVAL_UNIT);
        full.setAbilityValue(false);
        full.setMethodName("getLineOfSight");
        full.setMethodValue("2");

        String fullXml = full.serialize();

        assertNotNull(fullXml);
        assertTrue(fullXml.contains("model.unit.frigate"));
        assertTrue(fullXml.contains("ability-id"));
        assertTrue(fullXml.contains("ability-value"));
        assertTrue(fullXml.contains("method-name"));
        assertTrue(fullXml.contains("method-value"));

        // methodName is present but methodValue is null.
        Scope noMethodValue = new Scope();
        noMethodValue.setMethodName("getLineOfSight");

        String noMethodValueXml = noMethodValue.serialize();

        assertNotNull(noMethodValueXml);
        assertTrue(noMethodValueXml.contains("method-name"));
        assertFalse(noMethodValueXml.contains("method-value"));
    }


    /**
     * Tests the legacy camelCase attribute names supported by readAttributes.
     */
    public void testReadAttributesLegacyNames() throws Exception {
        String xml = "<scope matchNegated='true' "
            + "matchesNull='false' "
            + "type='model.unit.frigate' "
            + "ability-id='model.ability.navalUnit' "
            + "ability-value='false' "
            + "methodName='getLineOfSight' "
            + "methodValue='2'/>";

        try (FreeColXMLReader xr =
                 new FreeColXMLReader(new StringReader(xml))) {
            xr.nextTag();

            Scope scope = new Scope(xr);

            assertTrue(scope.getMatchNegated());
            assertFalse(scope.appliesTo(null));
            assertEquals("model.unit.frigate", scope.getType());
            assertEquals("model.ability.navalUnit", scope.getAbilityId());
            assertFalse(scope.getAbilityValue());
            assertEquals("getLineOfSight", scope.getMethodName());
            assertEquals("2", scope.getMethodValue());
        }
    }


    /**
     * Tests the current kebab-case attribute names supported by readAttributes.
     */
    public void testReadAttributesCurrentNames() throws Exception {
        String xml = "<scope match-negated='false' "
            + "matches-null='true' "
            + "type='model.unit.frigate' "
            + "ability-id='model.ability.navalUnit' "
            + "ability-value='true' "
            + "method-name='getLineOfSight' "
            + "method-value='1'/>";

        try (FreeColXMLReader xr =
                 new FreeColXMLReader(new StringReader(xml))) {
            xr.nextTag();

            Scope scope = new Scope(xr);

            assertFalse(scope.getMatchNegated());
            assertTrue(scope.appliesTo(null));
            assertEquals("model.unit.frigate", scope.getType());
            assertEquals("model.ability.navalUnit", scope.getAbilityId());
            assertTrue(scope.getAbilityValue());
            assertEquals("getLineOfSight", scope.getMethodName());
            assertEquals("1", scope.getMethodValue());
        }
    }


    /**
     * Tests readAttributes when optional attributes are absent.
     */
    public void testReadAttributesWithoutOptionalValues() throws Exception {
    String xml = "<scope/>";

    try (FreeColXMLReader xr =
             new FreeColXMLReader(new StringReader(xml))) {
        xr.nextTag();

        Scope scope = new Scope(xr);

        assertFalse(scope.getMatchNegated());
        assertTrue(scope.appliesTo(null));
        assertNull(scope.getType());
        assertNull(scope.getAbilityId());
        assertTrue(scope.getAbilityValue());
        assertNull(scope.getMethodName());
        assertNull(scope.getMethodValue());
    }
}

    public void testGetXMLTagName() {
        Scope scope = new Scope();

        assertEquals(Scope.TAG, scope.getXMLTagName());
        assertEquals("scope", scope.getXMLTagName());
    }
}
