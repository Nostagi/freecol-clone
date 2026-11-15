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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import net.sf.freecol.common.model.Modifier.ModifierType;
import net.sf.freecol.util.test.FreeColTestCase;


/**
 * CFG-driven test suite for {@link FeatureContainer} derived from
 * {@code test_cft/graph/net/sf/freecol/common/model/FeatureContainer.json}.
 *
 * Covers all 33 methods (M1..M33) and achieves 100% feasible C3 (node + edge)
 * coverage across 98 Test Paths.
 *
 * Infeasible / unreachable notes documented:
 * - Compiler-generated {@code synchronized} monitor-exit {@code finally_or_any}
 *   exception blocks in M2..M7, M11..M15, M19..M23, M25 are unreachable during
 *   normal execution.
 * - M27 ({@code toString}): correlated combinations {@code (B1:true, B2:false)}
 *   and {@code (B3:true, B4:false)} are infeasible because {@code sb.length() > siz}
 *   holds iff at least one loop iteration executed; all individual nodes and
 *   edges of M27 are 100% covered.
 */
public class FeatureContainerCfgTest extends FreeColTestCase {

    private static final String ABILITY_ID_1 = "model.ability.test1";
    private static final String ABILITY_ID_2 = "model.ability.test2";
    private static final String MODIFIER_ID_1 = "model.modifier.test1";
    private static final String MODIFIER_ID_2 = "model.modifier.test2";

    private Game game;
    private FeatureContainer fc;
    private GoodsType sourceA;
    private GoodsType sourceB;
    private Turn turn1;
    private Turn turn10;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        // Pre-condition: standard FreeCol game, fresh FeatureContainer, two
        // distinct FreeColSpecObjectType sources, and two Turn instances.
        game = getStandardGame();
        fc = new FeatureContainer();
        sourceA = new GoodsType("model.goods.testSourceA", spec());
        sourceB = new GoodsType("model.goods.testSourceB", spec());
        turn1 = new Turn(1);
        turn10 = new Turn(10);
    }

    // ------------------------------------------------------------------
    // Pre-condition helpers & stubs
    // ------------------------------------------------------------------

    private boolean invokeAbilitiesPresent(FeatureContainer target)
            throws Exception {
        Method m = FeatureContainer.class.getDeclaredMethod("abilitiesPresent");
        m.setAccessible(true);
        return (Boolean) m.invoke(target);
    }

    private void invokeRequireAbilities(FeatureContainer target)
            throws Exception {
        Method m = FeatureContainer.class.getDeclaredMethod("requireAbilities");
        m.setAccessible(true);
        m.invoke(target);
    }

    private boolean invokeModifiersPresent(FeatureContainer target)
            throws Exception {
        Method m = FeatureContainer.class.getDeclaredMethod("modifiersPresent");
        m.setAccessible(true);
        return (Boolean) m.invoke(target);
    }

    private void invokeRequireModifiers(FeatureContainer target)
            throws Exception {
        Method m = FeatureContainer.class.getDeclaredMethod("requireModifiers");
        m.setAccessible(true);
        m.invoke(target);
    }

    private float invokeApplyModifiersInternal(float number, Turn turn,
                                               Collection<Modifier> mods)
            throws Exception {
        Method m = FeatureContainer.class.getDeclaredMethod(
                "applyModifiersInternal", float.class, Turn.class, Collection.class);
        m.setAccessible(true);
        try {
            return (Float) m.invoke(null, number, turn, mods);
        } catch (InvocationTargetException e) {
            if (e.getCause() instanceof RuntimeException) {
                throw (RuntimeException) e.getCause();
            }
            throw e;
        }
    }

    // ------------------------------------------------------------------
    // M1: <init>()V — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M1-TP1: ENTRY -> N0 -> EXIT (default constructor initializes locks and null maps)
    public void testM1_TP1_ConstructorInitialState() throws Exception {
        FeatureContainer fresh = new FeatureContainer();
        assertFalse(invokeAbilitiesPresent(fresh));
        assertFalse(invokeModifiersPresent(fresh));
    }

    // ------------------------------------------------------------------
    // M2: abilitiesPresent()Z — 2 Test Paths (100% feasible C3)
    // ------------------------------------------------------------------

    // M2-TP1: ENTRY -> N0 -> N1 -(B1:true)-> N3 -> N4 -> N5 -> EXIT (abilities == null)
    public void testM2_TP1_AbilitiesPresentNull() throws Exception {
        assertFalse(invokeAbilitiesPresent(fc));
    }

    // M2-TP2: ENTRY -> N0 -> N1 -(B1:false)-> N2 -> N4 -> N5 -> EXIT (abilities != null)
    public void testM2_TP2_AbilitiesPresentNonNull() throws Exception {
        invokeRequireAbilities(fc);
        assertTrue(invokeAbilitiesPresent(fc));
    }

    // ------------------------------------------------------------------
    // M3: requireAbilities()V — 2 Test Paths (100% feasible C3)
    // ------------------------------------------------------------------

    // M3-TP1: ENTRY -> N0 -> N1 -(B1:false)-> N2 -> N3 -> N4 -> N7 -> EXIT (abilities == null -> allocates map)
    public void testM3_TP1_RequireAbilitiesInitializes() throws Exception {
        assertFalse(invokeAbilitiesPresent(fc));
        invokeRequireAbilities(fc);
        assertTrue(invokeAbilitiesPresent(fc));
    }

    // M3-TP2: ENTRY -> N0 -> N1 -(B1:true)-> N3 -> N4 -> N7 -> EXIT (abilities != null -> no-op)
    public void testM3_TP2_RequireAbilitiesAlreadyPresent() throws Exception {
        Ability a = new Ability(ABILITY_ID_1, sourceA, true);
        fc.addAbility(a);
        invokeRequireAbilities(fc);
        assertTrue(fc.hasAbility(ABILITY_ID_1, null, null));
    }

    // ------------------------------------------------------------------
    // M4: modifiersPresent()Z — 2 Test Paths (100% feasible C3)
    // ------------------------------------------------------------------

    // M4-TP1: ENTRY -> N0 -> N1 -(B1:true)-> N3 -> N4 -> N5 -> EXIT (modifiers == null)
    public void testM4_TP1_ModifiersPresentNull() throws Exception {
        assertFalse(invokeModifiersPresent(fc));
    }

    // M4-TP2: ENTRY -> N0 -> N1 -(B1:false)-> N2 -> N4 -> N5 -> EXIT (modifiers != null)
    public void testM4_TP2_ModifiersPresentNonNull() throws Exception {
        invokeRequireModifiers(fc);
        assertTrue(invokeModifiersPresent(fc));
    }

    // ------------------------------------------------------------------
    // M5: requireModifiers()V — 2 Test Paths (100% feasible C3)
    // ------------------------------------------------------------------

    // M5-TP1: ENTRY -> N0 -> N1 -(B1:false)-> N2 -> N3 -> N4 -> N7 -> EXIT (modifiers == null -> allocates map)
    public void testM5_TP1_RequireModifiersInitializes() throws Exception {
        assertFalse(invokeModifiersPresent(fc));
        invokeRequireModifiers(fc);
        assertTrue(invokeModifiersPresent(fc));
    }

    // M5-TP2: ENTRY -> N0 -> N1 -(B1:true)-> N3 -> N4 -> N7 -> EXIT (modifiers != null -> no-op)
    public void testM5_TP2_RequireModifiersAlreadyPresent() throws Exception {
        Modifier mod = new Modifier(MODIFIER_ID_1, 2.0f, ModifierType.ADDITIVE);
        fc.addModifier(mod);
        invokeRequireModifiers(fc);
        assertEquals(1, fc.getModifierValues().size());
    }

    // ------------------------------------------------------------------
    // M6: getAbilityValues()Ljava/util/Collection; — 3 Test Paths (100% feasible C3)
    // ------------------------------------------------------------------

    // M6-TP1: ENTRY -> N0 -> N1 -(B1:true)-> N5 -> N6 -> N9 -> EXIT (abilities == null)
    public void testM6_TP1_GetAbilityValuesWhenNull() {
        Collection<Ability> values = fc.getAbilityValues();
        assertTrue(values.isEmpty());
    }

    // M6-TP2: ENTRY -> N0 -> N1 -(B1:false)-> N2 -> N3 -(B2:true)-> N5 -> N6 -> N9 -> EXIT (abilities != null, empty map)
    public void testM6_TP2_GetAbilityValuesWhenEmptyMap() throws Exception {
        invokeRequireAbilities(fc);
        Collection<Ability> values = fc.getAbilityValues();
        assertTrue(values.isEmpty());
    }

    // M6-TP3: ENTRY -> N0 -> N1 -(B1:false)-> N2 -> N3 -(B2:false)-> N4 -> N3 -(B2:true)-> N5 -> N6 -> N9 -> EXIT (non-empty map)
    public void testM6_TP3_GetAbilityValuesWithEntries() {
        Ability a = new Ability(ABILITY_ID_1, sourceA, true);
        fc.addAbility(a);
        Collection<Ability> values = fc.getAbilityValues();
        assertEquals(1, values.size());
        assertTrue(values.contains(a));
    }

    // ------------------------------------------------------------------
    // M7: getModifierValues()Ljava/util/Collection; — 3 Test Paths (100% feasible C3)
    // ------------------------------------------------------------------

    // M7-TP1: ENTRY -> N0 -> N1 -(B1:true)-> N5 -> N6 -> N9 -> EXIT (modifiers == null)
    public void testM7_TP1_GetModifierValuesWhenNull() {
        Collection<Modifier> values = fc.getModifierValues();
        assertTrue(values.isEmpty());
    }

    // M7-TP2: ENTRY -> N0 -> N1 -(B1:false)-> N2 -> N3 -(B2:true)-> N5 -> N6 -> N9 -> EXIT (modifiers != null, empty map)
    public void testM7_TP2_GetModifierValuesWhenEmptyMap() throws Exception {
        invokeRequireModifiers(fc);
        Collection<Modifier> values = fc.getModifierValues();
        assertTrue(values.isEmpty());
    }

    // M7-TP3: ENTRY -> N0 -> N1 -(B1:false)-> N2 -> N3 -(B2:false)-> N4 -> N3 -(B2:true)-> N5 -> N6 -> N9 -> EXIT (non-empty map)
    public void testM7_TP3_GetModifierValuesWithEntries() {
        Modifier mod = new Modifier(MODIFIER_ID_1, 3.0f, ModifierType.ADDITIVE);
        fc.addModifier(mod);
        Collection<Modifier> values = fc.getModifierValues();
        assertEquals(1, values.size());
        assertTrue(values.contains(mod));
    }

    // ------------------------------------------------------------------
    // M8: allAbilities(Ljava/util/stream/Stream;)Z — 3 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M8-TP1: ENTRY -> N0 -> N1 -(B1:true)-> N5 -> EXIT (empty stream -> false)
    public void testM8_TP1_AllAbilitiesEmptyStream() {
        assertFalse(FeatureContainer.allAbilities(Stream.empty()));
    }

    // M8-TP2: ENTRY -> N0 -> N1 -(B1:false)-> N2 -(B2:false)-> N3 -> EXIT (ability.getValue() == false -> false)
    public void testM8_TP2_AllAbilitiesContainsFalseAbility() {
        Ability aFalse = new Ability(ABILITY_ID_1, sourceA, false);
        assertFalse(FeatureContainer.allAbilities(Stream.of(aFalse)));
    }

    // M8-TP3: ENTRY -> N0 -> N1 -(B1:false)-> N2 -(B2:true)-> N4 -> N1 -(B1:true)-> N5 -> EXIT (all abilities true -> true)
    public void testM8_TP3_AllAbilitiesAllTrue() {
        Ability aTrue = new Ability(ABILITY_ID_1, sourceA, true);
        assertTrue(FeatureContainer.allAbilities(Stream.of(aTrue)));
    }

    // ------------------------------------------------------------------
    // M9: hasAbility(String, FreeColSpecObjectType, Turn)Z — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M9-TP1: ENTRY -> N0 -> EXIT (delegates to allAbilities(getAbilities(...)))
    public void testM9_TP1_HasAbility() {
        fc.addAbility(new Ability(ABILITY_ID_1, sourceA, true));
        assertTrue(fc.hasAbility(ABILITY_ID_1, null, null));
        assertFalse(fc.hasAbility(ABILITY_ID_2, null, null));
    }

    // ------------------------------------------------------------------
    // M10: containsAbilityKey(String)Z — 2 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M10-TP1: ENTRY -> N0 -(B1:true)-> N2 -> N3 -> EXIT (first(...) == null -> false)
    public void testM10_TP1_ContainsAbilityKeyAbsent() {
        assertFalse(fc.containsAbilityKey(ABILITY_ID_1));
    }

    // M10-TP2: ENTRY -> N0 -(B1:false)-> N1 -> N3 -> EXIT (first(...) != null -> true)
    public void testM10_TP2_ContainsAbilityKeyPresent() {
        fc.addAbility(new Ability(ABILITY_ID_1, sourceA, false));
        assertTrue(fc.containsAbilityKey(ABILITY_ID_1));
    }

    // ------------------------------------------------------------------
    // M11: getAbilities(String, FreeColSpecObjectType, Turn)Stream — 5 Test Paths (100% feasible C3)
    // ------------------------------------------------------------------

    // M11-TP1: ENTRY -> N0 -(B1:true)-> N14 -> EXIT (!abilitiesPresent())
    public void testM11_TP1_GetAbilitiesNotPresent() {
        List<Ability> list = fc.getAbilities(ABILITY_ID_1, null, null)
                .collect(Collectors.toList());
        assertTrue(list.isEmpty());
    }

    // M11-TP2: ENTRY -> N0 -(B1:false)-> N1 -> N2 -(B2:true)-> N7 -(B4:true)-> N9 -> N10 -> N13 -> N14 -> EXIT (id != null, missing key)
    public void testM11_TP2_GetAbilitiesByIdNotFound() {
        fc.addAbility(new Ability(ABILITY_ID_1, sourceA, true));
        List<Ability> list = fc.getAbilities(ABILITY_ID_2, null, null)
                .collect(Collectors.toList());
        assertTrue(list.isEmpty());
    }

    // M11-TP3: ENTRY -> N0 -(B1:false)-> N1 -> N2 -(B2:true)-> N7 -(B4:false)-> N8 -> N9 -> N10 -> N13 -> N14 -> EXIT (id != null, key found)
    public void testM11_TP3_GetAbilitiesByIdFound() {
        Ability a = new Ability(ABILITY_ID_1, sourceA, true);
        fc.addAbility(a);
        List<Ability> list = fc.getAbilities(ABILITY_ID_1, null, null)
                .collect(Collectors.toList());
        assertEquals(1, list.size());
        assertEquals(a, list.get(0));
    }

    // M11-TP4: ENTRY -> N0 -(B1:false)-> N1 -> N2 -(B2:false)-> N3 -> N4 -(B3:true)-> N6 -> N9 -> N10 -> N13 -> N14 -> EXIT (id == null, empty map)
    public void testM11_TP4_GetAbilitiesNullIdEmptyMap() throws Exception {
        invokeRequireAbilities(fc);
        List<Ability> list = fc.getAbilities(null, null, null)
                .collect(Collectors.toList());
        assertTrue(list.isEmpty());
    }

    // M11-TP5: ENTRY -> N0 -(B1:false)-> N1 -> N2 -(B2:false)-> N3 -> N4 -(B3:false)-> N5 -> N4 -(B3:true)-> N6 -> N9 -> N10 -> N13 -> N14 -> EXIT (id == null, non-empty map)
    public void testM11_TP5_GetAbilitiesNullIdNonEmptyMap() {
        Ability a1 = new Ability(ABILITY_ID_1, sourceA, true);
        Ability a2 = new Ability(ABILITY_ID_2, sourceB, true);
        fc.addAbility(a1);
        fc.addAbility(a2);
        List<Ability> list = fc.getAbilities(null, null, null)
                .collect(Collectors.toList());
        assertEquals(2, list.size());
    }

    // ------------------------------------------------------------------
    // M12: addAbility(Ability)Z — 3 Test Paths (100% feasible C3)
    // ------------------------------------------------------------------

    // M12-TP1: ENTRY -> N0 -(B1:false)-> N1 -> EXIT (ability == null -> false)
    public void testM12_TP1_AddAbilityNull() {
        assertFalse(fc.addAbility(null));
    }

    // M12-TP2: ENTRY -> N0 -(B1:true)-> N2 -> N3 -(B2:false)-> N4 -> N5 -> N6 -> EXIT (new id -> creates set)
    public void testM12_TP2_AddAbilityNewKey() {
        Ability a = new Ability(ABILITY_ID_1, sourceA, true);
        assertTrue(fc.addAbility(a));
        assertTrue(fc.containsAbilityKey(ABILITY_ID_1));
    }

    // M12-TP3: ENTRY -> N0 -(B1:true)-> N2 -> N3 -(B2:true)-> N5 -> N6 -> EXIT (existing id -> adds to existing set)
    public void testM12_TP3_AddAbilityExistingKey() {
        Ability a1 = new Ability(ABILITY_ID_1, sourceA, true);
        Ability a2 = new Ability(ABILITY_ID_1, sourceB, false);
        assertTrue(fc.addAbility(a1));
        assertTrue(fc.addAbility(a2));
        assertEquals(2, fc.getAbilityValues().size());
    }

    // ------------------------------------------------------------------
    // M13: removeAbility(Ability)Ability — 5 Test Paths (100% feasible C3)
    // ------------------------------------------------------------------

    // M13-TP1: ENTRY -> N0 -(B1:true)-> N2 -> EXIT (ability == null -> null)
    public void testM13_TP1_RemoveAbilityNull() {
        assertNull(fc.removeAbility(null));
    }

    // M13-TP2: ENTRY -> N0 -(B1:false)-> N1 -(B2:false)-> N2 -> EXIT (!abilitiesPresent() -> null)
    public void testM13_TP2_RemoveAbilityWhenNotPresent() {
        Ability a = new Ability(ABILITY_ID_1, sourceA, true);
        assertNull(fc.removeAbility(a));
    }

    // M13-TP3: ENTRY -> N0 -(B1:false)-> N1 -(B2:true)-> N3 -> N4 -(B3:true)-> N6 -> N8 -> N9 -> EXIT (abilitySet == null -> null)
    public void testM13_TP3_RemoveAbilityMissingKey() {
        fc.addAbility(new Ability(ABILITY_ID_1, sourceA, true));
        Ability other = new Ability(ABILITY_ID_2, sourceA, true);
        assertNull(fc.removeAbility(other));
    }

    // M13-TP4: ENTRY -> N0 -(B1:false)-> N1 -(B2:true)-> N3 -> N4 -(B3:false)-> N5 -(B4:false)-> N6 -> N8 -> N9 -> EXIT (ability not in set -> null)
    public void testM13_TP4_RemoveAbilityNotInSet() {
        Ability a1 = new Ability(ABILITY_ID_1, sourceA, true);
        Ability a2 = new Ability(ABILITY_ID_1, sourceB, false);
        fc.addAbility(a1);
        assertNull(fc.removeAbility(a2));
    }

    // M13-TP5: ENTRY -> N0 -(B1:false)-> N1 -(B2:true)-> N3 -> N4 -(B3:false)-> N5 -(B4:true)-> N7 -> N8 -> N9 -> EXIT (removed -> ability)
    public void testM13_TP5_RemoveAbilitySuccess() {
        Ability a1 = new Ability(ABILITY_ID_1, sourceA, true);
        fc.addAbility(a1);
        assertSame(a1, fc.removeAbility(a1));
        assertFalse(fc.hasAbility(ABILITY_ID_1, null, null));
    }

    // ------------------------------------------------------------------
    // M14: removeAbilities(String)V — 2 Test Paths (100% feasible C3)
    // ------------------------------------------------------------------

    // M14-TP1: ENTRY -> N0 -(B1:false)-> N1 -> EXIT (!abilitiesPresent() -> early return)
    public void testM14_TP1_RemoveAbilitiesWhenNotPresent() {
        fc.removeAbilities(ABILITY_ID_1);
        assertFalse(fc.containsAbilityKey(ABILITY_ID_1));
    }

    // M14-TP2: ENTRY -> N0 -(B1:true)-> N2 -> N3 -> N4 -> N7 -> EXIT (abilitiesPresent() -> removes key)
    public void testM14_TP2_RemoveAbilitiesWhenPresent() {
        fc.addAbility(new Ability(ABILITY_ID_1, sourceA, true));
        fc.removeAbilities(ABILITY_ID_1);
        assertFalse(fc.containsAbilityKey(ABILITY_ID_1));
    }

    // ------------------------------------------------------------------
    // M15: getModifiers(String, FreeColSpecObjectType, Turn)Stream — 6 Test Paths (100% feasible C3)
    // ------------------------------------------------------------------

    // M15-TP1: ENTRY -> N0 -(B1:false)-> N1 -> EXIT (!modifiersPresent())
    public void testM15_TP1_GetModifiersNotPresent() {
        List<Modifier> list = fc.getModifiers(MODIFIER_ID_1, null, null)
                .collect(Collectors.toList());
        assertTrue(list.isEmpty());
    }

    // M15-TP2: ENTRY -> N0 -(B1:true)-> N2 -> N3 -(B2:true)-> N8 -(B4:true)-> N10 -> N11 -> N14 -(B5:true)-> N16 -> N17 -> EXIT (id != null, missing key)
    public void testM15_TP2_GetModifiersByIdNotFound() {
        fc.addModifier(new Modifier(MODIFIER_ID_1, 1.0f, ModifierType.ADDITIVE));
        List<Modifier> list = fc.getModifiers(MODIFIER_ID_2, null, null)
                .collect(Collectors.toList());
        assertTrue(list.isEmpty());
    }

    // M15-TP3: ENTRY -> N0 -(B1:true)-> N2 -> N3 -(B2:true)-> N8 -(B4:false)-> N9 -> N10 -> N11 -> N14 -(B5:true)-> N16 -> N17 -> EXIT (id != null, all filtered out)
    public void testM15_TP3_GetModifiersByIdFilteredOut() {
        Modifier mod = new Modifier(MODIFIER_ID_1, 1.0f, ModifierType.ADDITIVE);
        mod.setFirstTurn(turn10);
        fc.addModifier(mod);
        List<Modifier> list = fc.getModifiers(MODIFIER_ID_1, null, turn1)
                .collect(Collectors.toList());
        assertTrue(list.isEmpty());
    }

    // M15-TP4: ENTRY -> N0 -(B1:true)-> N2 -> N3 -(B2:true)-> N8 -(B4:false)-> N9 -> N10 -> N11 -> N14 -(B5:false)-> N15 -> N17 -> EXIT (id != null, matches)
    public void testM15_TP4_GetModifiersByIdFound() {
        Modifier mod = new Modifier(MODIFIER_ID_1, 5.0f, ModifierType.ADDITIVE);
        fc.addModifier(mod);
        List<Modifier> list = fc.getModifiers(MODIFIER_ID_1, null, null)
                .collect(Collectors.toList());
        assertEquals(1, list.size());
        assertEquals(mod, list.get(0));
    }

    // M15-TP5: ENTRY -> N0 -(B1:true)-> N2 -> N3 -(B2:false)-> N4 -> N5 -(B3:true)-> N7 -> N10 -> N11 -> N14 -(B5:true)-> N16 -> N17 -> EXIT (id == null, empty map)
    public void testM15_TP5_GetModifiersNullIdEmptyMap() throws Exception {
        invokeRequireModifiers(fc);
        List<Modifier> list = fc.getModifiers(null, null, null)
                .collect(Collectors.toList());
        assertTrue(list.isEmpty());
    }

    // M15-TP6: ENTRY -> N0 -(B1:true)-> N2 -> N3 -(B2:false)-> N4 -> N5 -(B3:false)-> N6 -> N5 -(B3:true)-> N7 -> N10 -> N11 -> N14 -(B5:false)-> N15 -> N17 -> EXIT (id == null, non-empty)
    public void testM15_TP6_GetModifiersNullIdNonEmptyMap() {
        fc.addModifier(new Modifier(MODIFIER_ID_1, 2.0f, ModifierType.ADDITIVE));
        fc.addModifier(new Modifier(MODIFIER_ID_2, 3.0f, ModifierType.ADDITIVE));
        List<Modifier> list = fc.getModifiers(null, null, null)
                .collect(Collectors.toList());
        assertEquals(2, list.size());
    }

    // ------------------------------------------------------------------
    // M16: applyModifiers(float, Turn, Collection)F — 3 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M16-TP1: ENTRY -> N0 -(B1:true)-> N2 -> N4 -> EXIT (mods == null -> number)
    public void testM16_TP1_ApplyModifiersCollectionNull() {
        assertEquals(10.0f, FeatureContainer.applyModifiers(10.0f, turn1, (Collection<Modifier>) null), 0.001f);
    }

    // M16-TP2: ENTRY -> N0 -(B1:false)-> N1 -(B2:false)-> N2 -> N4 -> EXIT (mods.isEmpty() -> number)
    public void testM16_TP2_ApplyModifiersCollectionEmpty() {
        assertEquals(10.0f, FeatureContainer.applyModifiers(10.0f, turn1, Collections.emptyList()), 0.001f);
    }

    // M16-TP3: ENTRY -> N0 -(B1:false)-> N1 -(B2:true)-> N3 -> N4 -> EXIT (non-empty -> applies modifiers)
    public void testM16_TP3_ApplyModifiersCollectionNonEmpty() {
        List<Modifier> mods = Collections.singletonList(
                new Modifier(MODIFIER_ID_1, 4.0f, ModifierType.ADDITIVE));
        assertEquals(14.0f, FeatureContainer.applyModifiers(10.0f, turn1, mods), 0.001f);
    }

    // ------------------------------------------------------------------
    // M17: applyModifiers(float, Turn, Stream)F — 2 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M17-TP1: ENTRY -> N0 -(B1:false)-> N1 -> N3 -> EXIT (mods == null -> number)
    public void testM17_TP1_ApplyModifiersStreamNull() {
        assertEquals(10.0f, FeatureContainer.applyModifiers(10.0f, turn1, (Stream<Modifier>) null), 0.001f);
    }

    // M17-TP2: ENTRY -> N0 -(B1:true)-> N2 -> N3 -> EXIT (mods != null -> applies modifiers)
    public void testM17_TP2_ApplyModifiersStreamNonNull() {
        Stream<Modifier> mods = Stream.of(
                new Modifier(MODIFIER_ID_1, 5.0f, ModifierType.ADDITIVE));
        assertEquals(15.0f, FeatureContainer.applyModifiers(10.0f, turn1, mods), 0.001f);
    }

    // ------------------------------------------------------------------
    // M18: applyModifiersInternal(float, Turn, Collection)F — 3 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M18-TP1: ENTRY -> N0 -> N1 -(B1:true)-> N5 -> EXIT (empty collection -> number)
    public void testM18_TP1_ApplyModifiersInternalEmpty() throws Exception {
        float res = invokeApplyModifiersInternal(10.0f, turn1, Collections.emptyList());
        assertEquals(10.0f, res, 0.001f);
    }

    // M18-TP2: ENTRY -> N0 -> N1 -(B1:false)-> N2 -(B2:false)-> N3 -> EXIT (value == Modifier.UNKNOWN -> returns UNKNOWN)
    public void testM18_TP2_ApplyModifiersInternalUnknownValue() throws Exception {
        List<Modifier> mods = Collections.singletonList(
                new Modifier(MODIFIER_ID_1, Modifier.UNKNOWN, ModifierType.ADDITIVE));
        float res = invokeApplyModifiersInternal(10.0f, turn1, mods);
        assertEquals(Modifier.UNKNOWN, res);
    }

    // M18-TP3: ENTRY -> N0 -> N1 -(B1:false)-> N2 -(B2:true)-> N4 -> N1 -(B1:true)-> N5 -> EXIT (normal value applied)
    public void testM18_TP3_ApplyModifiersInternalNormal() throws Exception {
        List<Modifier> mods = new ArrayList<>();
        mods.add(new Modifier(MODIFIER_ID_1, 3.0f, ModifierType.ADDITIVE));
        float res = invokeApplyModifiersInternal(10.0f, turn1, mods);
        assertEquals(13.0f, res, 0.001f);
    }

    // ------------------------------------------------------------------
    // M19: addModifier(Modifier)Z — 3 Test Paths (100% feasible C3)
    // ------------------------------------------------------------------

    // M19-TP1: ENTRY -> N0 -(B1:false)-> N1 -> EXIT (modifier == null -> false)
    public void testM19_TP1_AddModifierNull() {
        assertFalse(fc.addModifier(null));
    }

    // M19-TP2: ENTRY -> N0 -(B1:true)-> N2 -> N3 -(B2:false)-> N4 -> N5 -> N6 -> EXIT (new id -> creates set)
    public void testM19_TP2_AddModifierNewKey() {
        Modifier mod = new Modifier(MODIFIER_ID_1, 1.0f, ModifierType.ADDITIVE);
        assertTrue(fc.addModifier(mod));
        assertEquals(1, fc.getModifierValues().size());
    }

    // M19-TP3: ENTRY -> N0 -(B1:true)-> N2 -> N3 -(B2:true)-> N5 -> N6 -> EXIT (existing id -> adds to set)
    public void testM19_TP3_AddModifierExistingKey() {
        Modifier m1 = new Modifier(MODIFIER_ID_1, 1.0f, ModifierType.ADDITIVE);
        Modifier m2 = new Modifier(MODIFIER_ID_1, 2.0f, ModifierType.ADDITIVE);
        assertTrue(fc.addModifier(m1));
        assertTrue(fc.addModifier(m2));
        assertEquals(2, fc.getModifierValues().size());
    }

    // ------------------------------------------------------------------
    // M20: removeModifier(Modifier)Modifier — 5 Test Paths (100% feasible C3)
    // ------------------------------------------------------------------

    // M20-TP1: ENTRY -> N0 -(B1:true)-> N2 -> EXIT (modifier == null -> null)
    public void testM20_TP1_RemoveModifierNull() {
        assertNull(fc.removeModifier(null));
    }

    // M20-TP2: ENTRY -> N0 -(B1:false)-> N1 -(B2:false)-> N2 -> EXIT (!modifiersPresent() -> null)
    public void testM20_TP2_RemoveModifierWhenNotPresent() {
        Modifier mod = new Modifier(MODIFIER_ID_1, 1.0f, ModifierType.ADDITIVE);
        assertNull(fc.removeModifier(mod));
    }

    // M20-TP3: ENTRY -> N0 -(B1:false)-> N1 -(B2:true)-> N3 -> N4 -(B3:true)-> N6 -> N8 -> N9 -> EXIT (modifierSet == null -> null)
    public void testM20_TP3_RemoveModifierMissingKey() {
        fc.addModifier(new Modifier(MODIFIER_ID_1, 1.0f, ModifierType.ADDITIVE));
        Modifier other = new Modifier(MODIFIER_ID_2, 1.0f, ModifierType.ADDITIVE);
        assertNull(fc.removeModifier(other));
    }

    // M20-TP4: ENTRY -> N0 -(B1:false)-> N1 -(B2:true)-> N3 -> N4 -(B3:false)-> N5 -(B4:false)-> N6 -> N8 -> N9 -> EXIT (not in set -> null)
    public void testM20_TP4_RemoveModifierNotInSet() {
        Modifier m1 = new Modifier(MODIFIER_ID_1, 1.0f, ModifierType.ADDITIVE);
        Modifier m2 = new Modifier(MODIFIER_ID_1, 2.0f, ModifierType.ADDITIVE);
        fc.addModifier(m1);
        assertNull(fc.removeModifier(m2));
    }

    // M20-TP5: ENTRY -> N0 -(B1:false)-> N1 -(B2:true)-> N3 -> N4 -(B3:false)-> N5 -(B4:true)-> N7 -> N8 -> N9 -> EXIT (removed -> modifier)
    public void testM20_TP5_RemoveModifierSuccess() {
        Modifier m1 = new Modifier(MODIFIER_ID_1, 1.0f, ModifierType.ADDITIVE);
        fc.addModifier(m1);
        assertSame(m1, fc.removeModifier(m1));
        assertTrue(fc.getModifierValues().isEmpty());
    }

    // ------------------------------------------------------------------
    // M21: removeModifiers(String)V — 2 Test Paths (100% feasible C3)
    // ------------------------------------------------------------------

    // M21-TP1: ENTRY -> N0 -(B1:false)-> N1 -> EXIT (!modifiersPresent() -> early return)
    public void testM21_TP1_RemoveModifiersWhenNotPresent() {
        fc.removeModifiers(MODIFIER_ID_1);
        assertTrue(fc.getModifierValues().isEmpty());
    }

    // M21-TP2: ENTRY -> N0 -(B1:true)-> N2 -> N3 -> N4 -> N7 -> EXIT (modifiersPresent() -> removes key)
    public void testM21_TP2_RemoveModifiersWhenPresent() {
        fc.addModifier(new Modifier(MODIFIER_ID_1, 1.0f, ModifierType.ADDITIVE));
        fc.removeModifiers(MODIFIER_ID_1);
        assertTrue(fc.getModifierValues().isEmpty());
    }

    // ------------------------------------------------------------------
    // M22: addFeatures(FreeColObject)V — 5 Test Paths (100% feasible C3)
    // ------------------------------------------------------------------

    // M22-TP1: ENTRY -> N0 -(B1:false)-> N1 -> EXIT (fco.getFeatureContainer() == null)
    public void testM22_TP1_AddFeaturesNullContainer() {
        fc.addFeatures(game);
        assertTrue(fc.getAbilityValues().isEmpty());
        assertTrue(fc.getModifierValues().isEmpty());
    }

    // M22-TP2: ENTRY -> N0 -(B1:true)-> N2 -(B2:true)-> N13 -(B3:true)-> N24 -> EXIT (c has neither abilities nor modifiers)
    public void testM22_TP2_AddFeaturesEmptyContainer() {
        fc.addFeatures(sourceA);
        assertTrue(fc.getAbilityValues().isEmpty());
        assertTrue(fc.getModifierValues().isEmpty());
    }

    // M22-TP3: ENTRY -> N0 -(B1:true)-> N2 -(B2:true)-> N13 -(B3:false)-> N14..N24 -> EXIT (c has modifiers only)
    public void testM22_TP3_AddFeaturesModifiersOnly() {
        sourceA.addModifier(new Modifier(MODIFIER_ID_1, 2.0f, ModifierType.ADDITIVE));
        fc.addFeatures(sourceA);
        assertTrue(fc.getAbilityValues().isEmpty());
        assertEquals(1, fc.getModifierValues().size());
    }

    // M22-TP4: ENTRY -> N0 -(B1:true)-> N2 -(B2:false)-> N3..N10 -> N13 -(B3:true)-> N24 -> EXIT (c has abilities only)
    public void testM22_TP4_AddFeaturesAbilitiesOnly() {
        sourceA.addAbility(new Ability(ABILITY_ID_1, sourceA, true));
        fc.addFeatures(sourceA);
        assertEquals(1, fc.getAbilityValues().size());
        assertTrue(fc.getModifierValues().isEmpty());
    }

    // M22-TP5: ENTRY -> N0 -(B1:true)-> N2 -(B2:false)-> N3..N10 -> N13 -(B3:false)-> N14..N24 -> EXIT (c has both abilities and modifiers)
    public void testM22_TP5_AddFeaturesBoth() {
        sourceA.addAbility(new Ability(ABILITY_ID_1, sourceA, true));
        sourceA.addModifier(new Modifier(MODIFIER_ID_1, 2.0f, ModifierType.ADDITIVE));
        fc.addFeatures(sourceA);
        assertEquals(1, fc.getAbilityValues().size());
        assertEquals(1, fc.getModifierValues().size());
    }

    // ------------------------------------------------------------------
    // M23: removeFeatures(FreeColObject)V — 8 Test Paths (100% feasible C3)
    // ------------------------------------------------------------------

    // M23-TP1: ENTRY -> N0 -(B1:false)-> N1 -> EXIT (c == null)
    public void testM23_TP1_RemoveFeaturesNullContainer() {
        fc.addAbility(new Ability(ABILITY_ID_1, sourceA, true));
        fc.removeFeatures(game);
        assertEquals(1, fc.getAbilityValues().size());
    }

    // M23-TP2: ENTRY -> N0 -(B1:true)-> N2 -(B2:true)-> N24 -(B8:true)-> N46 -> EXIT (!abilitiesPresent() && !modifiersPresent())
    public void testM23_TP2_RemoveFeaturesNeitherPresentInThis() {
        sourceA.addAbility(new Ability(ABILITY_ID_1, sourceA, true));
        fc.removeFeatures(sourceA);
        assertTrue(fc.getAbilityValues().isEmpty());
    }

    // M23-TP3: ENTRY -> N0 -(B1:true)-> N2 -(B2:false)-> N3 -(B3:true)-> N24 -(B8:false)-> N25 -(B9:true)-> N46 -> EXIT (this present, c not present)
    public void testM23_TP3_RemoveFeaturesEmptySourceContainer() {
        fc.addAbility(new Ability(ABILITY_ID_1, sourceA, true));
        fc.addModifier(new Modifier(MODIFIER_ID_1, 1.0f, ModifierType.ADDITIVE, sourceA));
        fc.removeFeatures(sourceA);
        assertEquals(1, fc.getAbilityValues().size());
        assertEquals(1, fc.getModifierValues().size());
    }

    // M23-TP4: ENTRY -> ... -> N11 -(B4:true)-> N20 -> ... -> N33 -(B10:true)-> N42 -> N43 -> N46 -> EXIT (c maps present but empty)
    public void testM23_TP4_RemoveFeaturesEmptyMapsInSource() {
        fc.addAbility(new Ability(ABILITY_ID_1, sourceA, true));
        fc.addModifier(new Modifier(MODIFIER_ID_1, 1.0f, ModifierType.ADDITIVE, sourceA));
        Ability tempA = new Ability(ABILITY_ID_1, sourceA, true);
        Modifier tempM = new Modifier(MODIFIER_ID_1, 1.0f, ModifierType.ADDITIVE, sourceA);
        sourceA.addAbility(tempA);
        sourceA.addModifier(tempM);
        sourceA.getFeatureContainer().clear();

        fc.removeFeatures(sourceA);
        assertEquals(1, fc.getAbilityValues().size());
        assertEquals(1, fc.getModifierValues().size());
    }

    // M23-TP5: ENTRY -> ... -> N11 -(B4:false)-> N12 -(B5:false)-> N13 -> N11 -> ... -> N33 -(B10:false)-> N34 -(B11:false)-> N35 -> N33 -> ... -> EXIT (keys absent in this)
    public void testM23_TP5_RemoveFeaturesMissingKeysInThis() {
        fc.addAbility(new Ability(ABILITY_ID_1, sourceA, true));
        fc.addModifier(new Modifier(MODIFIER_ID_1, 1.0f, ModifierType.ADDITIVE, sourceA));
        sourceA.addAbility(new Ability(ABILITY_ID_2, sourceA, true));
        sourceA.addModifier(new Modifier(MODIFIER_ID_2, 1.0f, ModifierType.ADDITIVE, sourceA));

        fc.removeFeatures(sourceA);
        assertEquals(1, fc.getAbilityValues().size());
        assertEquals(1, fc.getModifierValues().size());
    }

    // M23-TP6: ENTRY -> ... -> N12 -(B5:true)-> N14 -> N15 -(B6:true)-> N19 -> N11 -> ... -> N34 -(B11:true)-> N36 -> N37 -(B12:true)-> N41 -> N33 -> ... -> EXIT (empty sets in this)
    public void testM23_TP6_RemoveFeaturesEmptySetsInThis() {
        Ability a = new Ability(ABILITY_ID_1, sourceA, true);
        Modifier m = new Modifier(MODIFIER_ID_1, 1.0f, ModifierType.ADDITIVE, sourceA);
        fc.addAbility(a);
        fc.removeAbility(a);
        fc.addModifier(m);
        fc.removeModifier(m);

        sourceA.addAbility(new Ability(ABILITY_ID_1, sourceA, true));
        sourceA.addModifier(new Modifier(MODIFIER_ID_1, 1.0f, ModifierType.ADDITIVE, sourceA));

        fc.removeFeatures(sourceA);
        assertTrue(fc.getAbilityValues().isEmpty());
        assertTrue(fc.getModifierValues().isEmpty());
    }

    // M23-TP7: ENTRY -> ... -> N15 -(B6:false)-> N16 -(B7:true)-> N18 -> N15 -> ... -> N37 -(B12:false)-> N38 -(B13:true)-> N40 -> N37 -> ... -> EXIT (source != fco -> kept)
    public void testM23_TP7_RemoveFeaturesDifferentSourceKept() {
        fc.addAbility(new Ability(ABILITY_ID_1, sourceB, true));
        fc.addModifier(new Modifier(MODIFIER_ID_1, 1.0f, ModifierType.ADDITIVE, sourceB));

        sourceA.addAbility(new Ability(ABILITY_ID_1, sourceA, true));
        sourceA.addModifier(new Modifier(MODIFIER_ID_1, 1.0f, ModifierType.ADDITIVE, sourceA));

        fc.removeFeatures(sourceA);
        assertEquals(1, fc.getAbilityValues().size());
        assertEquals(1, fc.getModifierValues().size());
    }

    // M23-TP8: ENTRY -> ... -> N15 -(B6:false)-> N16 -(B7:false)-> N17 -> N18 -> N15 -> ... -> N37 -(B12:false)-> N38 -(B13:false)-> N39 -> N40 -> N37 -> ... -> EXIT (source == fco -> removed)
    public void testM23_TP8_RemoveFeaturesMatchingSourceRemoved() {
        fc.addAbility(new Ability(ABILITY_ID_1, sourceA, true));
        fc.addModifier(new Modifier(MODIFIER_ID_1, 1.0f, ModifierType.ADDITIVE, sourceA));

        sourceA.addAbility(new Ability(ABILITY_ID_1, sourceA, true));
        sourceA.addModifier(new Modifier(MODIFIER_ID_1, 1.0f, ModifierType.ADDITIVE, sourceA));

        fc.removeFeatures(sourceA);
        assertTrue(fc.getAbilityValues().isEmpty());
        assertTrue(fc.getModifierValues().isEmpty());
    }

    // ------------------------------------------------------------------
    // M24: copy(FeatureContainer)V — 3 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M24-TP1: ENTRY -> N0 -(B1:true)-> N7 -> EXIT (other == null -> clears this)
    public void testM24_TP1_CopyNull() {
        fc.addAbility(new Ability(ABILITY_ID_1, sourceA, true));
        fc.copy(null);
        assertTrue(fc.getAbilityValues().isEmpty());
    }

    // M24-TP2: ENTRY -> N0 -(B1:false)-> N1 -> N2 -(B2:true)-> N4 -> N5 -(B3:true)-> N7 -> EXIT (other empty)
    public void testM24_TP2_CopyEmpty() {
        fc.addAbility(new Ability(ABILITY_ID_1, sourceA, true));
        fc.copy(new FeatureContainer());
        assertTrue(fc.getAbilityValues().isEmpty());
        assertTrue(fc.getModifierValues().isEmpty());
    }

    // M24-TP3: ENTRY -> N0 -(B1:false)-> N1 -> N2 -(B2:false)-> N3 -> N2 -(B2:true)-> N4 -> N5 -(B3:false)-> N6 -> N5 -(B3:true)-> N7 -> EXIT (other has abilities & modifiers)
    public void testM24_TP3_CopyNonEmpty() {
        FeatureContainer other = new FeatureContainer();
        other.addAbility(new Ability(ABILITY_ID_1, sourceA, true));
        other.addModifier(new Modifier(MODIFIER_ID_1, 2.0f, ModifierType.ADDITIVE));
        fc.copy(other);
        assertEquals(1, fc.getAbilityValues().size());
        assertEquals(1, fc.getModifierValues().size());
    }

    // ------------------------------------------------------------------
    // M25: clear()V — 4 Test Paths (100% feasible C3)
    // ------------------------------------------------------------------

    // M25-TP1: ENTRY -> N0 -(B1:true)-> N6 -(B2:true)-> N12 -> EXIT (!abilitiesPresent() && !modifiersPresent())
    public void testM25_TP1_ClearNeitherPresent() {
        fc.clear();
        assertTrue(fc.getAbilityValues().isEmpty());
        assertTrue(fc.getModifierValues().isEmpty());
    }

    // M25-TP2: ENTRY -> N0 -(B1:true)-> N6 -(B2:false)-> N7 -> N8 -> N9 -> N12 -> EXIT (modifiers only)
    public void testM25_TP2_ClearModifiersOnly() {
        fc.addModifier(new Modifier(MODIFIER_ID_1, 1.0f, ModifierType.ADDITIVE));
        fc.clear();
        assertTrue(fc.getModifierValues().isEmpty());
    }

    // M25-TP3: ENTRY -> N0 -(B1:false)-> N1 -> N2 -> N3 -> N6 -(B2:true)-> N12 -> EXIT (abilities only)
    public void testM25_TP3_ClearAbilitiesOnly() {
        fc.addAbility(new Ability(ABILITY_ID_1, sourceA, true));
        fc.clear();
        assertTrue(fc.getAbilityValues().isEmpty());
    }

    // M25-TP4: ENTRY -> N0 -(B1:false)-> N1 -> N2 -> N3 -> N6 -(B2:false)-> N7 -> N8 -> N9 -> N12 -> EXIT (both present)
    public void testM25_TP4_ClearBothPresent() {
        fc.addAbility(new Ability(ABILITY_ID_1, sourceA, true));
        fc.addModifier(new Modifier(MODIFIER_ID_1, 1.0f, ModifierType.ADDITIVE));
        fc.clear();
        assertTrue(fc.getAbilityValues().isEmpty());
        assertTrue(fc.getModifierValues().isEmpty());
    }

    // ------------------------------------------------------------------
    // M26: replaceSource(FreeColSpecObjectType, FreeColSpecObjectType)V — 2 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M26-TP1: ENTRY -> N0 -> N1 -(B1:true)-> N3 -> N4 -(B2:true)-> N6 -> EXIT (no matching abilities or modifiers)
    public void testM26_TP1_ReplaceSourceEmpty() {
        fc.replaceSource(sourceA, sourceB);
        assertTrue(fc.getAbilityValues().isEmpty());
        assertTrue(fc.getModifierValues().isEmpty());
    }

    // M26-TP2: ENTRY -> N0 -> N1 -(B1:false)-> N2 -> N1 -(B1:true)-> N3 -> N4 -(B2:false)-> N5 -> N4 -(B2:true)-> N6 -> EXIT (replaces matching)
    public void testM26_TP2_ReplaceSourceMatches() {
        fc.addAbility(new Ability(ABILITY_ID_1, sourceA, true));
        fc.addModifier(new Modifier(MODIFIER_ID_1, 2.0f, ModifierType.ADDITIVE, sourceA));
        fc.replaceSource(sourceA, sourceB);
        assertEquals(sourceB, fc.getAbilityValues().iterator().next().getSource());
        assertEquals(sourceB, fc.getModifierValues().iterator().next().getSource());
    }

    // ------------------------------------------------------------------
    // M27: toString()String — 2 Test Paths (100% feasible C3)
    // ------------------------------------------------------------------

    // M27-TP1: ENTRY -> N0 -> N1 -(B1:true)-> N3 -(B2:true)-> N5 -> N6 -(B3:true)-> N8 -(B4:true)-> N10 -> EXIT (empty)
    public void testM27_TP1_ToStringEmpty() {
        assertEquals("[FeatureContainer]", fc.toString());
    }

    // M27-TP2: ENTRY -> N0 -> N1 -(B1:false)-> N2 -> N1 -(B1:true)-> N3 -(B2:false)-> N4 -> N5 -> N6 -(B3:false)-> N7 -> N6 -(B3:true)-> N8 -(B4:false)-> N9 -> N10 -> EXIT (non-empty)
    public void testM27_TP2_ToStringNonEmpty() {
        fc.addAbility(new Ability(ABILITY_ID_1, sourceA, true));
        fc.addModifier(new Modifier(MODIFIER_ID_1, 2.0f, ModifierType.ADDITIVE));
        String s = fc.toString();
        assertTrue(s.contains("[abilities"));
        assertTrue(s.contains("[modifiers"));
    }

    // ------------------------------------------------------------------
    // M28: lambda$replaceSource$1(FreeColSpecObjectType, Modifier)Z — 3 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M28-TP1: ENTRY -> N0 -(B1:true)-> N2 -> N4 -> EXIT (oldSource == null -> true)
    public void testM28_TP1_LambdaReplaceSourceModifierOldSourceNull() {
        fc.addModifier(new Modifier(MODIFIER_ID_1, 2.0f, ModifierType.ADDITIVE, sourceA));
        fc.replaceSource(null, sourceB);
        assertEquals(sourceB, fc.getModifierValues().iterator().next().getSource());
    }

    // M28-TP2: ENTRY -> N0 -(B1:false)-> N1 -(B2:true)-> N3 -> N4 -> EXIT (oldSource != null && m.getSource() != oldSource -> false)
    public void testM28_TP2_LambdaReplaceSourceModifierMismatch() {
        fc.addModifier(new Modifier(MODIFIER_ID_1, 2.0f, ModifierType.ADDITIVE, sourceB));
        fc.replaceSource(sourceA, sourceB);
        assertEquals(sourceB, fc.getModifierValues().iterator().next().getSource());
    }

    // M28-TP3: ENTRY -> N0 -(B1:false)-> N1 -(B2:false)-> N2 -> N4 -> EXIT (oldSource != null && m.getSource() == oldSource -> true)
    public void testM28_TP3_LambdaReplaceSourceModifierMatch() {
        fc.addModifier(new Modifier(MODIFIER_ID_1, 2.0f, ModifierType.ADDITIVE, sourceA));
        fc.replaceSource(sourceA, sourceB);
        assertEquals(sourceB, fc.getModifierValues().iterator().next().getSource());
    }

    // ------------------------------------------------------------------
    // M29: lambda$replaceSource$0(FreeColSpecObjectType, Ability)Z — 3 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M29-TP1: ENTRY -> N0 -(B1:true)-> N2 -> N4 -> EXIT (oldSource == null -> true)
    public void testM29_TP1_LambdaReplaceSourceAbilityOldSourceNull() {
        fc.addAbility(new Ability(ABILITY_ID_1, sourceA, true));
        fc.replaceSource(null, sourceB);
        assertEquals(sourceB, fc.getAbilityValues().iterator().next().getSource());
    }

    // M29-TP2: ENTRY -> N0 -(B1:false)-> N1 -(B2:true)-> N3 -> N4 -> EXIT (oldSource != null && a.getSource() != oldSource -> false)
    public void testM29_TP2_LambdaReplaceSourceAbilityMismatch() {
        fc.addAbility(new Ability(ABILITY_ID_1, sourceB, true));
        fc.replaceSource(sourceA, sourceB);
        assertEquals(sourceB, fc.getAbilityValues().iterator().next().getSource());
    }

    // M29-TP3: ENTRY -> N0 -(B1:false)-> N1 -(B2:false)-> N2 -> N4 -> EXIT (oldSource != null && a.getSource() == oldSource -> true)
    public void testM29_TP3_LambdaReplaceSourceAbilityMatch() {
        fc.addAbility(new Ability(ABILITY_ID_1, sourceA, true));
        fc.replaceSource(sourceA, sourceB);
        assertEquals(sourceB, fc.getAbilityValues().iterator().next().getSource());
    }

    // ------------------------------------------------------------------
    // M30: lambda$addFeatures$1(Map.Entry)V — 2 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M30-TP1: ENTRY -> N0 -(B1:false)-> N1 -> N2 -> EXIT (modifierSet == null -> creates set)
    public void testM30_TP1_LambdaAddFeaturesModifierNewKey() {
        sourceA.addModifier(new Modifier(MODIFIER_ID_1, 2.0f, ModifierType.ADDITIVE));
        fc.addFeatures(sourceA);
        assertEquals(1, fc.getModifierValues().size());
    }

    // M30-TP2: ENTRY -> N0 -(B1:true)-> N2 -> EXIT (modifierSet != null -> reuses existing set)
    public void testM30_TP2_LambdaAddFeaturesModifierExistingKey() {
        fc.addModifier(new Modifier(MODIFIER_ID_1, 1.0f, ModifierType.ADDITIVE));
        sourceA.addModifier(new Modifier(MODIFIER_ID_1, 2.0f, ModifierType.ADDITIVE));
        fc.addFeatures(sourceA);
        assertEquals(2, fc.getModifierValues().size());
    }

    // ------------------------------------------------------------------
    // M31: lambda$addFeatures$0(Map.Entry)V — 2 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M31-TP1: ENTRY -> N0 -(B1:false)-> N1 -> N2 -> EXIT (abilitySet == null -> creates set)
    public void testM31_TP1_LambdaAddFeaturesAbilityNewKey() {
        sourceA.addAbility(new Ability(ABILITY_ID_1, sourceA, true));
        fc.addFeatures(sourceA);
        assertEquals(1, fc.getAbilityValues().size());
    }

    // M31-TP2: ENTRY -> N0 -(B1:true)-> N2 -> EXIT (abilitySet != null -> reuses existing set)
    public void testM31_TP2_LambdaAddFeaturesAbilityExistingKey() {
        fc.addAbility(new Ability(ABILITY_ID_1, sourceB, false));
        sourceA.addAbility(new Ability(ABILITY_ID_1, sourceA, true));
        fc.addFeatures(sourceA);
        assertEquals(2, fc.getAbilityValues().size());
    }

    // ------------------------------------------------------------------
    // M32: lambda$getModifiers$0(FreeColSpecObjectType, Turn, Modifier)Z — 2 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M32-TP1: ENTRY -> N0 -(B1:true)-> N2 -> N3 -> EXIT (!m.appliesTo(fcgot, turn) == true -> removed)
    public void testM32_TP1_LambdaGetModifiersDoesNotApply() {
        Modifier mod = new Modifier(MODIFIER_ID_1, 2.0f, ModifierType.ADDITIVE);
        mod.setFirstTurn(turn10);
        fc.addModifier(mod);
        List<Modifier> list = fc.getModifiers(MODIFIER_ID_1, null, turn1)
                .collect(Collectors.toList());
        assertTrue(list.isEmpty());
    }

    // M32-TP2: ENTRY -> N0 -(B1:false)-> N1 -> N3 -> EXIT (!m.appliesTo(fcgot, turn) == false -> kept)
    public void testM32_TP2_LambdaGetModifiersApplies() {
        Modifier mod = new Modifier(MODIFIER_ID_1, 2.0f, ModifierType.ADDITIVE);
        mod.setFirstTurn(turn1);
        fc.addModifier(mod);
        List<Modifier> list = fc.getModifiers(MODIFIER_ID_1, null, turn10)
                .collect(Collectors.toList());
        assertEquals(1, list.size());
    }

    // ------------------------------------------------------------------
    // M33: lambda$getAbilities$0(FreeColSpecObjectType, Turn, Ability)Z — 2 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M33-TP1: ENTRY -> N0 -(B1:true)-> N2 -> N3 -> EXIT (!a.appliesTo(fcgot, turn) == true -> removed)
    public void testM33_TP1_LambdaGetAbilitiesDoesNotApply() {
        Ability a = new Ability(ABILITY_ID_1, sourceA, true);
        a.setFirstTurn(turn10);
        fc.addAbility(a);
        List<Ability> list = fc.getAbilities(ABILITY_ID_1, null, turn1)
                .collect(Collectors.toList());
        assertTrue(list.isEmpty());
    }

    // M33-TP2: ENTRY -> N0 -(B1:false)-> N1 -> N3 -> EXIT (!a.appliesTo(fcgot, turn) == false -> kept)
    public void testM33_TP2_LambdaGetAbilitiesApplies() {
        Ability a = new Ability(ABILITY_ID_1, sourceA, true);
        a.setFirstTurn(turn1);
        fc.addAbility(a);
        List<Ability> list = fc.getAbilities(ABILITY_ID_1, null, turn10)
                .collect(Collectors.toList());
        assertEquals(1, list.size());
    }
}
