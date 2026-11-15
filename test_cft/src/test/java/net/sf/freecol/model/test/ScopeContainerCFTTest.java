
package net.sf.freecol.common.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import net.sf.freecol.util.test.FreeColTestCase;


public class ScopeContainerCFTTest extends FreeColTestCase {

    private Scope newScope(boolean matchesNull) {
        Scope scope = new Scope();
        scope.setMatchesNull(matchesNull);
        return scope;
    }

    private ScopeContainer container(Scope... scopes) {
        ScopeContainer sc = new ScopeContainer();
        sc.addAll(Arrays.asList(scopes));
        return sc;
    }

    // S01
   public void testS01_constructorIsEmpty() {
    ScopeContainer sc = new ScopeContainer();

    assertTrue(sc.isEmpty());
    assertTrue(sc.getList().isEmpty());
}
    // S02
    public void testS02_isEmptyWithNullScopes() {
        ScopeContainer sc = new ScopeContainer();

        assertTrue(sc.isEmpty());
    }

    // S03
    public void testS03_isEmptyWithEmptyList() {
        ScopeContainer sc = new ScopeContainer();
        sc.set(new ArrayList<Scope>());

        assertTrue(sc.isEmpty());
    }

    // S04
    public void testS04_isNotEmptyAfterAdd() {
        ScopeContainer sc = new ScopeContainer();
        sc.add(newScope(true));

        assertFalse(sc.isEmpty());
    }

    // S05
    public void testS05_getListWhenEmpty() {
        ScopeContainer sc = new ScopeContainer();

        assertNotNull(sc.getList());
        assertTrue(sc.getList().isEmpty());
    }

    // S06
    public void testS06_getListWhenNotEmpty() {
        Scope scope = newScope(true);
        ScopeContainer sc = container(scope);

        assertEquals(1, sc.getList().size());
        assertSame(scope, sc.getList().get(0));
    }

    // S07
    public void testS07_getStreamWhenEmpty() {
        ScopeContainer sc = new ScopeContainer();

        assertEquals(0, sc.get().count());
    }

    // S08
    public void testS08_getStreamWhenNotEmpty() {
        ScopeContainer sc = container(newScope(true), newScope(false));

        assertEquals(2, sc.get().count());
    }

    // S09
    public void testS09_setWhenCurrentScopesAreNull() {
        ScopeContainer sc = new ScopeContainer();
        List<Scope> scopes = Arrays.asList(newScope(true), newScope(false));

        sc.set(scopes);

        assertEquals(2, sc.getList().size());
        assertSame(scopes, sc.getList());
    }

    // S10
    public void testS10_setNullWhenCurrentScopesAreNull() {
        ScopeContainer sc = new ScopeContainer();

        sc.set(null);

        assertTrue(sc.isEmpty());
        assertNull(sc.getList().isEmpty() ? null : sc.getList().get(0));
    }

    // S11
    public void testS11_setWhenCurrentScopesExist() {
        ScopeContainer sc = container(newScope(true));
        List<Scope> newScopes = Arrays.asList(newScope(false), newScope(true));

        sc.set(newScopes);

        assertEquals(2, sc.getList().size());
        assertSame(newScopes.get(0), sc.getList().get(0));
        assertSame(newScopes.get(1), sc.getList().get(1));
    }

    // S12
    public void testS12_setNullWhenCurrentScopesExist() {
        ScopeContainer sc = container(newScope(true));

        sc.set(null);

        assertTrue(sc.isEmpty());
        assertEquals(0, sc.getList().size());
    }

    // S13
    public void testS13_clearWhenScopesAreNull() {
        ScopeContainer sc = new ScopeContainer();

        sc.clear();

        assertTrue(sc.isEmpty());
    }

    // S14
    public void testS14_clearWhenScopesExist() {
        ScopeContainer sc = container(newScope(true), newScope(false));

        sc.clear();

        assertTrue(sc.isEmpty());
        assertEquals(0, sc.getList().size());
    }

    // S15
    public void testS15_addNullScope() {
        ScopeContainer sc = new ScopeContainer();

        sc.add(null);

        assertTrue(sc.isEmpty());
    }

    // S16
    public void testS16_addFirstScope() {
        ScopeContainer sc = new ScopeContainer();
        Scope scope = newScope(true);

        sc.add(scope);

        assertEquals(1, sc.getList().size());
        assertSame(scope, sc.getList().get(0));
    }

    // S17
    public void testS17_addMultipleScopes() {
        ScopeContainer sc = new ScopeContainer();
        Scope s1 = newScope(true);
        Scope s2 = newScope(false);

        sc.add(s1);
        sc.add(s2);

        assertEquals(2, sc.getList().size());
        assertSame(s1, sc.getList().get(0));
        assertSame(s2, sc.getList().get(1));
    }

    // S18
    public void testS18_addAllNullCollection() {
        ScopeContainer sc = new ScopeContainer();

        sc.addAll((java.util.Collection<Scope>) null);

        assertTrue(sc.isEmpty());
    }

    // S19
    public void testS19_addAllEmptyCollection() {
        ScopeContainer sc = new ScopeContainer();

        sc.addAll(Collections.<Scope>emptyList());

        assertTrue(sc.isEmpty());
    }

    // S20
    public void testS20_addAllNonEmptyCollection() {
        ScopeContainer sc = new ScopeContainer();
        List<Scope> scopes = Arrays.asList(newScope(true), newScope(false));

        sc.addAll(scopes);

        assertEquals(2, sc.getList().size());
    }

    // S21
    public void testS21_addAllCollectionToExistingScopes() {
        ScopeContainer sc = container(newScope(true));
        sc.addAll(Arrays.asList(newScope(false), newScope(true)));

        assertEquals(3, sc.getList().size());
    }

    // S22
    public void testS22_addAllNullContainer() {
        ScopeContainer sc = new ScopeContainer();

        sc.addAll((ScopeContainer) null);

        assertTrue(sc.isEmpty());
    }

    // S23
    public void testS23_addAllEmptyContainer() {
        ScopeContainer sc = new ScopeContainer();
        ScopeContainer other = new ScopeContainer();

        sc.addAll(other);

        assertTrue(sc.isEmpty());
    }

    // S24
    public void testS24_addAllFromAnotherContainer() {
        Scope scope = newScope(true);
        ScopeContainer source = container(scope);
        ScopeContainer target = new ScopeContainer();

        target.addAll(source);

        assertEquals(1, target.getList().size());
        assertSame(scope, target.getList().get(0));
    }

    // S25
    public void testS25_removeWhenScopesAreNull() {
        ScopeContainer sc = new ScopeContainer();

        sc.remove(newScope(true));

        assertTrue(sc.isEmpty());
    }

    // S26
    public void testS26_removeExistingScope() {
        Scope scope = newScope(true);
        ScopeContainer sc = container(scope);

        sc.remove(scope);

        assertTrue(sc.isEmpty());
    }

    // S27
    public void testS27_removeNonExistingScope() {
        ScopeContainer sc = container(newScope(true));

        sc.remove(newScope(false));

        assertEquals(1, sc.getList().size());
    }

    // S28
    public void testS28_sortWhenScopesAreNull() {
        ScopeContainer sc = new ScopeContainer();

        sc.sort(Comparator.comparingInt(Object::hashCode));

        assertTrue(sc.isEmpty());
    }

    // S29
    public void testS29_sortWithComparator() {
        Scope s1 = newScope(true);
        Scope s2 = newScope(false);
        Scope s3 = newScope(true);

        ScopeContainer sc = container(s1, s2, s3);
        Comparator<Scope> comparator
            = Comparator.comparingInt(System::identityHashCode);

        sc.sort(comparator);

        List<Scope> expected = new ArrayList<>(Arrays.asList(s1, s2, s3));
        expected.sort(comparator);

        assertEquals(expected, sc.getList());
    }

    // S30
    public void testS30_appliesToEmptyContainer() {
        ScopeContainer sc = new ScopeContainer();

        assertTrue(sc.appliesTo(null));
    }

    // S31
    public void testS31_appliesToMatchingNullScope() {
        ScopeContainer sc = container(newScope(true));

        assertTrue(sc.appliesTo(null));
    }

    // S32
    public void testS32_appliesToNonMatchingNullScope() {
        ScopeContainer sc = container(newScope(false));

        assertFalse(sc.appliesTo(null));
    }

    // S33
    public void testS33_toXMLWhenScopesAreNull() throws Exception {
        ScopeContainer sc = new ScopeContainer();

        sc.toXML(null);

        assertTrue(sc.isEmpty());
    }

    // S34
    public void testS34_toXMLWhenScopeListIsEmpty() throws Exception {
        ScopeContainer sc = new ScopeContainer();
        sc.set(new ArrayList<Scope>());

        sc.toXML(null);

        assertTrue(sc.isEmpty());
    }

    // S35
    public void testS35_equalsSameObject() {
        ScopeContainer sc = container(newScope(true));

        assertTrue(sc.equals(sc));
    }

    // S36
    public void testS36_equalsNullObject() {
        ScopeContainer sc = new ScopeContainer();

        assertFalse(sc.equals(null));
    }

    // S37
    public void testS37_equalsDifferentClass() {
        ScopeContainer sc = new ScopeContainer();

        assertFalse(sc.equals("ScopeContainer"));
    }

    // S38
    public void testS38_equalsTwoEmptyContainers() {
        ScopeContainer sc1 = new ScopeContainer();
        ScopeContainer sc2 = new ScopeContainer();

        assertTrue(sc1.equals(sc2));
    }

    // S39
    public void testS39_equalsEmptyAndNonEmptyContainers() {
        ScopeContainer sc1 = new ScopeContainer();
        ScopeContainer sc2 = container(newScope(true));

        assertFalse(sc1.equals(sc2));
        assertFalse(sc2.equals(sc1));
    }

    // S40
    public void testS40_equalsContainersWithSameScopes() {
        Scope s1 = newScope(true);
        Scope s2 = newScope(false);

        ScopeContainer sc1 = container(s1, s2);
        ScopeContainer sc2 = container(s2, s1);

        assertTrue(sc1.equals(sc2));
    }

    // S41
    public void testS41_equalsContainersWithDifferentScopes() {
        ScopeContainer sc1 = container(newScope(true));
        ScopeContainer sc2 = container(newScope(false));

        assertFalse(sc1.equals(sc2));
    }

    // S42
    public void testS42_equalsContainersWithDifferentSizes() {
        ScopeContainer sc1 = container(newScope(true));
        ScopeContainer sc2 = container(newScope(true), newScope(false));

        assertFalse(sc1.equals(sc2));
    }

    // S43
    public void testS43_hashCode() {
        ScopeContainer sc = container(newScope(true), newScope(false));

        int hash = sc.hashCode();

        assertEquals(hash, sc.hashCode());
    }

    // S44
    public void testS44_isScopeContainerEmptyWithNull() {
        assertTrue(ScopeContainer.isScopeContainerEmpty(null));
    }

    // S45
    public void testS45_isScopeContainerEmptyWithEmptyContainer() {
        assertTrue(ScopeContainer.isScopeContainerEmpty(
            new ScopeContainer()));
    }

    // S46
    public void testS46_isScopeContainerEmptyWithNonEmptyContainer() {
        assertFalse(ScopeContainer.isScopeContainerEmpty(
            container(newScope(true))));
    }

    // S47
    public void testS47_getScopeListWithNull() {
        List<Scope> scopes = ScopeContainer.getScopeList(null);

        assertNotNull(scopes);
        assertTrue(scopes.isEmpty());
    }

    // S48
    public void testS48_getScopeListWithContainer() {
        Scope scope = newScope(true);
        ScopeContainer sc = container(scope);

        List<Scope> scopes = ScopeContainer.getScopeList(sc);

        assertEquals(1, scopes.size());
        assertSame(scope, scopes.get(0));
    }

    // S49
    public void testS49_getScopesWithNull() {
    assertNotNull(ScopeContainer.getScopes(null));
}

    // S50
    public void testS50_getScopesWithContainer() {
        ScopeContainer sc = container(newScope(true), newScope(false));

        assertEquals(2, ScopeContainer.getScopes(sc).count());
    }

    // S51
    public void testS51_addScopeWithNullContainer() {
        Scope scope = newScope(true);

        ScopeContainer sc = ScopeContainer.addScope(null, scope);

        assertNotNull(sc);
        assertEquals(1, sc.getList().size());
        assertSame(scope, sc.getList().get(0));
    }

    // S52
    public void testS52_addScopeWithExistingContainer() {
        ScopeContainer sc = new ScopeContainer();
        Scope scope = newScope(true);

        ScopeContainer result = ScopeContainer.addScope(sc, scope);

        assertSame(sc, result);
        assertEquals(1, sc.getList().size());
    }

    // S53
    public void testS53_addNullScopeWithNullContainer() {
        ScopeContainer sc = ScopeContainer.addScope(null, null);

        assertNotNull(sc);
        assertTrue(sc.isEmpty());
    }

    // S54
    public void testS54_removeScopeWithNullContainer() {
        ScopeContainer.removeScope(null, newScope(true));
    }

    // S55
    public void testS55_removeScopeWithExistingContainer() {
        Scope scope = newScope(true);
        ScopeContainer sc = container(scope);

        ScopeContainer.removeScope(sc, scope);

        assertTrue(sc.isEmpty());
    }

    // S56
    public void testS56_setScopesNullContainerAndNullCollection() {
        assertNull(ScopeContainer.setScopes(null, null));
    }

    // S57
    public void testS57_setScopesNullContainerAndEmptyCollection() {
        assertNull(ScopeContainer.setScopes(
            null, Collections.<Scope>emptyList()));
    }

    // S58
    public void testS58_setScopesNullContainerAndNonEmptyCollection() {
        List<Scope> scopes = Arrays.asList(newScope(true), newScope(false));

        ScopeContainer sc = ScopeContainer.setScopes(null, scopes);

        assertNotNull(sc);
        assertEquals(2, sc.getList().size());
    }

    // S59
    public void testS59_setScopesExistingContainer() {
        ScopeContainer sc = container(newScope(true));
        List<Scope> scopes = Arrays.asList(newScope(false), newScope(true));

        ScopeContainer result = ScopeContainer.setScopes(sc, scopes);

        assertSame(sc, result);
        assertEquals(2, sc.getList().size());
    }

    // S60
    public void testS60_setScopesExistingContainerWithNullCollection() {
        ScopeContainer sc = container(newScope(true));

        ScopeContainer result = ScopeContainer.setScopes(sc, null);

        assertSame(sc, result);
        assertTrue(sc.isEmpty());
    }

    // S61
    public void testS61_clearScopesWithNull() {
        ScopeContainer.clearScopes(null);
    }

    // S62
    public void testS62_clearScopesWithExistingContainer() {
        ScopeContainer sc = container(newScope(true));

        ScopeContainer.clearScopes(sc);

        assertTrue(sc.isEmpty());
    }

    // S63
    public void testS63_equalScopesBothNull() {
        assertTrue(ScopeContainer.equalScopes(null, null));
    }

    // S64
    public void testS64_equalScopesFirstNull() {
        assertFalse(ScopeContainer.equalScopes(null,
            new ScopeContainer()));
    }

    // S65
    public void testS65_equalScopesSecondNull() {
        assertFalse(ScopeContainer.equalScopes(
            new ScopeContainer(), null));
    }

    // S66
    public void testS66_equalScopesEqualContainers() {
        ScopeContainer sc1 = container(newScope(true));
        ScopeContainer sc2 = container(newScope(true));

        assertTrue(ScopeContainer.equalScopes(sc1, sc2));
    }

    // S67
    public void testS67_equalScopesDifferentContainers() {
        ScopeContainer sc1 = container(newScope(true));
        ScopeContainer sc2 = container(newScope(false));

        assertFalse(ScopeContainer.equalScopes(sc1, sc2));
    }

    // S68
    public void testS68_scopeContainerAppliesToNullContainer() {
        assertTrue(ScopeContainer.scopeContainerAppliesTo(null, null));
    }

    // S69
    public void testS69_scopeContainerAppliesToEmptyContainer() {
        ScopeContainer sc = new ScopeContainer();

        assertTrue(ScopeContainer.scopeContainerAppliesTo(sc, null));
    }

    // S70
    public void testS70_scopeContainerAppliesToMatchingScope() {
        ScopeContainer sc = container(newScope(true));

        assertTrue(ScopeContainer.scopeContainerAppliesTo(sc, null));
    }

    // S71
    public void testS71_scopeContainerAppliesToNonMatchingScope() {
        ScopeContainer sc = container(newScope(false));

        assertFalse(ScopeContainer.scopeContainerAppliesTo(sc, null));
    }

    // S72
    public void testS72_scopeContainerToXMLWithNullContainer()
        throws Exception {
        ScopeContainer.scopeContainerToXML(null, null);
    }

    // S73
    public void testS73_scopeContainerToXMLWithEmptyContainer()
        throws Exception {
        ScopeContainer sc = new ScopeContainer();

        ScopeContainer.scopeContainerToXML(sc, null);

        assertTrue(sc.isEmpty());
    }

    // S74
    public void testS74_scopeContainerToXMLWithEmptyList()
        throws Exception {
        ScopeContainer sc = new ScopeContainer();
        sc.set(new ArrayList<Scope>());

        ScopeContainer.scopeContainerToXML(sc, null);

        assertTrue(sc.isEmpty());
    }
}
