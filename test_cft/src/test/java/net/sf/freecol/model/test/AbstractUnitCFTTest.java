
package net.sf.freecol.common.model;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import net.sf.freecol.util.test.FreeColTestCase;


public class AbstractUnitCFTTest extends FreeColTestCase {

    private Specification spec;

    private AbstractUnit unit;
    private AbstractUnit sameUnit;
    private AbstractUnit differentUnit;

    private static final String TYPE_ID = "model.unit.freeColonist";
    private static final String ROLE_ID = Specification.DEFAULT_ROLE_ID;


    @Override
    public void setUp() throws Exception {
        super.setUp();

        spec = spec();

        unit = new AbstractUnit(TYPE_ID, ROLE_ID, 2);
        sameUnit = new AbstractUnit(TYPE_ID, ROLE_ID, 2);
        differentUnit = new AbstractUnit(
            "model.unit.veteranSoldier", ROLE_ID, 3);
    }


    // Constructors and accessors

    public void testDefaultConstructor() {
        AbstractUnit au = new AbstractUnit();

        assertEquals(Specification.DEFAULT_ROLE_ID, au.getRoleId());
        assertEquals(1, au.getNumber());
    }


    public void testConstructorWithValues() {
        AbstractUnit au = new AbstractUnit("test.unit", "test.role", 5);

        assertEquals("test.unit", au.getId());
        assertEquals("test.role", au.getRoleId());
        assertEquals(5, au.getNumber());
    }


    public void testConstructorWithUnitType() {
        UnitType type = spec.getUnitType(TYPE_ID);

        assertNotNull(type);

        AbstractUnit au = new AbstractUnit(type, ROLE_ID, 4);

        assertEquals(type.getId(), au.getId());
        assertEquals(ROLE_ID, au.getRoleId());
        assertEquals(4, au.getNumber());
    }


    public void testSetRoleId() {
        unit.setRoleId("model.role.soldier");

        assertEquals("model.role.soldier", unit.getRoleId());
    }


    public void testSetNumber() {
        unit.setNumber(7);

        assertEquals(7, unit.getNumber());
    }


    public void testAddToNumberPositive() {
        unit.addToNumber(3);

        assertEquals(5, unit.getNumber());
    }


    public void testAddToNumberNegative() {
        unit.addToNumber(-1);

        assertEquals(1, unit.getNumber());
    }


    public void testAddToNumberZero() {
        unit.addToNumber(0);

        assertEquals(2, unit.getNumber());
    }


    // Labels and descriptions

    public void testGetSingleLabel() {
        assertNotNull(unit.getSingleLabel());
    }


    public void testGetLabel() {
        assertNotNull(unit.getLabel());
    }


    public void testGetDescription() {
        assertNotNull(unit.getDescription());
    }


    public void testGetType() {
        assertEquals(spec.getUnitType(TYPE_ID), unit.getType(spec));
    }


    public void testGetRole() {
        assertEquals(spec.getRole(ROLE_ID), unit.getRole(spec));
    }


    // Offence

    public void testGetOffence() {
        Role role = spec.getRole(ROLE_ID);
        UnitType type = spec.getUnitType(TYPE_ID);

        double expected = 2 * (type.getOffence() + role.getOffence());

        assertEquals(expected, unit.getOffence(spec), 0.0001);
    }


    public void testGetOffenceWithZeroNumber() {
        unit.setNumber(0);

        assertEquals(0.0, unit.getOffence(spec), 0.0001);
    }


    public void testCalculateStrengthEmptyList() {
        List<AbstractUnit> units = Collections.emptyList();

        assertEquals(0.0, AbstractUnit.calculateStrength(spec, units),
                     0.0001);
    }


    public void testCalculateStrengthSingleUnit() {
        List<AbstractUnit> units = Collections.singletonList(unit);

        assertEquals(unit.getOffence(spec),
                     AbstractUnit.calculateStrength(spec, units),
                     0.0001);
    }


    public void testCalculateStrengthMultipleUnits() {
        List<AbstractUnit> units = Arrays.asList(unit, sameUnit);

        double expected = unit.getOffence(spec) + sameUnit.getOffence(spec);

        assertEquals(expected, AbstractUnit.calculateStrength(spec, units),
                     0.0001);
    }


    // Collection operations

    public void testDeepCopyEmptyList() {
        List<AbstractUnit> result =
            AbstractUnit.deepCopy(Collections.emptyList());

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }


    public void testDeepCopyMultipleUnits() {
        List<AbstractUnit> source = Arrays.asList(unit, differentUnit);

        List<AbstractUnit> result = AbstractUnit.deepCopy(source);

        assertEquals(2, result.size());
        assertTrue(result.get(0).matchAll(unit));
        assertTrue(result.get(1).matchAll(differentUnit));
        assertNotSame(unit, result.get(0));
        assertNotSame(differentUnit, result.get(1));
    }


    public void testGetListLabelEmptyList() {
        StringTemplate result =
            AbstractUnit.getListLabel("test.label", Collections.emptyList());

        assertNotNull(result);
    }


    public void testGetListLabelMultipleUnits() {
        List<AbstractUnit> units = Arrays.asList(unit, differentUnit);

        StringTemplate result =
            AbstractUnit.getListLabel("test.label", units);

        assertNotNull(result);
    }


    // matchAll

    public void testMatchAllTrue() {
        assertTrue(unit.matchAll(sameUnit));
    }


    public void testMatchAllDifferentId() {
        assertFalse(unit.matchAll(
            new AbstractUnit("other.unit", ROLE_ID, 2)));
    }


    public void testMatchAllDifferentRole() {
        assertFalse(unit.matchAll(
            new AbstractUnit(TYPE_ID, "model.role.soldier", 2)));
    }


    public void testMatchAllDifferentNumber() {
        assertFalse(unit.matchAll(
            new AbstractUnit(TYPE_ID, ROLE_ID, 5)));
    }


    // matcher(UnitType, String)

    public void testMatcherUnitTypeAndRoleTrue() {
        UnitType type = spec.getUnitType(TYPE_ID);

        assertNotNull(type);
        assertTrue(AbstractUnit.matcher(type, ROLE_ID).test(unit));
    }


    public void testMatcherUnitTypeAndRoleDifferentType() {
        UnitType type = spec.getUnitType(TYPE_ID);

        assertNotNull(type);
        assertFalse(AbstractUnit.matcher(type, ROLE_ID).test(differentUnit));
    }


    public void testMatcherUnitTypeAndRoleDifferentRole() {
        UnitType type = spec.getUnitType(TYPE_ID);

        assertNotNull(type);

        AbstractUnit other =
            new AbstractUnit(TYPE_ID, "model.role.soldier", 2);

        assertFalse(AbstractUnit.matcher(type, ROLE_ID).test(other));
    }


    // matcher(AbstractUnit)

    public void testMatcherAbstractUnitTrue() {
        assertTrue(AbstractUnit.matcher(unit).test(sameUnit));
    }


    public void testMatcherAbstractUnitDifferentType() {
        assertFalse(AbstractUnit.matcher(unit).test(differentUnit));
    }


    public void testMatcherAbstractUnitDifferentRole() {
        AbstractUnit other =
            new AbstractUnit(TYPE_ID, "model.role.soldier", 2);

        assertFalse(AbstractUnit.matcher(unit).test(other));
    }


    // matchUnits

    public void testMatchUnitsDifferentSizes() {
        assertFalse(AbstractUnit.matchUnits(
            Collections.singletonList(unit),
            Collections.emptyList()));
    }


    public void testMatchUnitsBothEmpty() {
        assertTrue(AbstractUnit.matchUnits(
            Collections.emptyList(),
            Collections.emptyList()));
    }


    public void testMatchUnitsNoMatchingElement() {
        assertFalse(AbstractUnit.matchUnits(
            Collections.singletonList(unit),
            Collections.singletonList(differentUnit)));
    }


    public void testMatchUnitsMatchingElement() {
        assertTrue(AbstractUnit.matchUnits(
            Collections.singletonList(unit),
            Collections.singletonList(sameUnit)));
    }


    public void testMatchUnitsMultipleElements() {
        List<AbstractUnit> first = Arrays.asList(unit, differentUnit);
        List<AbstractUnit> second = Arrays.asList(
            new AbstractUnit(TYPE_ID, ROLE_ID, 2),
            new AbstractUnit("model.unit.veteranSoldier", ROLE_ID, 3));

        assertTrue(AbstractUnit.matchUnits(first, second));
    }


    public void testMatchUnitsUnmatchedAfterFirstMatch() {
        List<AbstractUnit> first = Arrays.asList(unit, differentUnit);
        List<AbstractUnit> second = Arrays.asList(
            new AbstractUnit(TYPE_ID, ROLE_ID, 2),
            new AbstractUnit("model.unit.artillery", ROLE_ID, 1));

        assertFalse(AbstractUnit.matchUnits(first, second));
    }


    // copyIn

    public void testCopyInNull() {
        assertFalse(unit.copyIn(null));
    }


    public void testCopyInWrongType() {
        UnitType other = spec.getUnitType(TYPE_ID);

        assertNotNull(other);
        assertEquals(unit.getId(), other.getId());
        assertFalse(unit.copyIn(other));
    }


    public void testCopyInDifferentId() {
        AbstractUnit other =
            new AbstractUnit("other.unit", "other.role", 8);

        assertFalse(unit.copyIn(other));
    }


    public void testCopyInSuccess() {
        AbstractUnit other =
            new AbstractUnit(unit.getId(), "model.role.soldier", 6);

        assertTrue(unit.copyIn(other));

        assertEquals("model.role.soldier", unit.getRoleId());
        assertEquals(6, unit.getNumber());
    }


    // XML tag and Object methods

    public void testGetXMLTagName() {
        assertEquals(AbstractUnit.TAG, unit.getXMLTagName());
    }


    public void testToString() {
        String result = unit.toString();

        assertNotNull(result);
        assertTrue(result.contains("2"));
        assertTrue(result.contains(TYPE_ID));
        assertTrue(result.contains(ROLE_ID));
    }
}
