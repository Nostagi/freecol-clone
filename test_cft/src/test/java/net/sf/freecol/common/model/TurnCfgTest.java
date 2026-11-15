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

import net.sf.freecol.util.test.FreeColTestCase;


/**
 * CFG-driven test suite for {@link Turn} derived from
 * {@code test_cft/graph/net/sf/freecol/common/model/Turn.json}.
 *
 * Covers all 23 methods (M1..M23, including {@code <init>} and {@code <clinit>})
 * and achieves 100% feasible C3 (node + edge) coverage across 34 Test Paths.
 *
 * Infeasible edges documented:
 * - None.
 */
public class TurnCfgTest extends FreeColTestCase {

    @Override
    public void setUp() throws Exception {
        super.setUp();
        // Pre-condition: Ensure standard Col1 Turn constants (1492, 1600, 2)
        Turn.initialize(1492, 1600, 2);
    }

    @Override
    public void tearDown() throws Exception {
        Turn.initialize(1492, 1600, 2);
        super.tearDown();
    }

    // ------------------------------------------------------------------
    // Pre-condition helpers & stubs
    // ------------------------------------------------------------------
    // No stubs needed for concrete class Turn.

    // ------------------------------------------------------------------
    // M1: <init>(I)V — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M1-TP1: ENTRY -> N0 -> EXIT (Construct Turn with turn number)
    public void testM1_TP1_Constructor() {
        Turn t = new Turn(5);
        assertEquals(5, t.getNumber());
    }

    // ------------------------------------------------------------------
    // M2: initialize(III)V — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M2-TP1: ENTRY -> N0 -> EXIT (Initialize static year and season constants)
    public void testM2_TP1_Initialize() {
        Turn.initialize(1500, 1650, 4);
        assertEquals(1500, Turn.getStartingYear());
        assertEquals(1650, Turn.getSeasonYear());
        assertEquals(4, Turn.getSeasonNumber());
    }

    // ------------------------------------------------------------------
    // M3: getStartingYear()I — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M3-TP1: ENTRY -> N0 -> EXIT (Return startingYear)
    public void testM3_TP1_GetStartingYear() {
        assertEquals(1492, Turn.getStartingYear());
    }

    // ------------------------------------------------------------------
    // M4: getSeasonYear()I — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M4-TP1: ENTRY -> N0 -> EXIT (Return seasonYear)
    public void testM4_TP1_GetSeasonYear() {
        assertEquals(1600, Turn.getSeasonYear());
    }

    // ------------------------------------------------------------------
    // M5: getSeasonNumber()I — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M5-TP1: ENTRY -> N0 -> EXIT (Return seasonNumber)
    public void testM5_TP1_GetSeasonNumber() {
        assertEquals(2, Turn.getSeasonNumber());
    }

    // ------------------------------------------------------------------
    // M6: yearToTurn(I)I — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M6-TP1: ENTRY -> N0 -> EXIT (Delegate yearToTurn(year) to yearToTurn(year, 0))
    public void testM6_TP1_YearToTurnSingleArg() {
        assertEquals(1, Turn.yearToTurn(1492));
        assertEquals(109, Turn.yearToTurn(1600));
    }

    // ------------------------------------------------------------------
    // M7: yearToTurn(II)I — 3 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M7-TP1: ENTRY -> N0 -(B1:true)-> N3 -> EXIT (year < startingYear)
    public void testM7_TP1_YearToTurnBeforeStartingYear() {
        assertEquals(1, Turn.yearToTurn(1400, 0));
    }

    // M7-TP2: ENTRY -> N0 -(B1:false)-> N1 -(B2:true)-> N3 -> EXIT (startingYear <= year < seasonYear)
    public void testM7_TP2_YearToTurnBeforeSeasonYear() {
        assertEquals(9, Turn.yearToTurn(1500, 1));
    }

    // M7-TP3: ENTRY -> N0 -(B1:false)-> N1 -(B2:false)-> N2 -> N3 -> EXIT (year >= seasonYear)
    public void testM7_TP3_YearToTurnAtOrAfterSeasonYear() {
        assertEquals(110, Turn.yearToTurn(1600, 1));
    }

    // ------------------------------------------------------------------
    // M8: next()Lnet/sf/freecol/common/model/Turn; — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M8-TP1: ENTRY -> N0 -> EXIT (Return new Turn(turn + 1))
    public void testM8_TP1_Next() {
        Turn t = new Turn(10);
        Turn next = t.next();
        assertEquals(11, next.getNumber());
    }

    // ------------------------------------------------------------------
    // M9: getNumber()I — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M9-TP1: ENTRY -> N0 -> EXIT (Return turn number)
    public void testM9_TP1_GetNumber() {
        assertEquals(7, new Turn(7).getNumber());
    }

    // ------------------------------------------------------------------
    // M10: getYear()I — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M10-TP1: ENTRY -> N0 -> EXIT (Delegate getYear() to getTurnYear(turn))
    public void testM10_TP1_GetYear() {
        assertEquals(1492, new Turn(1).getYear());
    }

    // ------------------------------------------------------------------
    // M11: getTurnYear(I)I — 2 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M11-TP1: ENTRY -> N0 -(B1:true)-> N2 -> N3 -> EXIT (year >= seasonYear)
    public void testM11_TP1_GetTurnYearAtOrAfterSeasonYear() {
        int turn1600 = Turn.yearToTurn(1600);
        assertEquals(1600, Turn.getTurnYear(turn1600));
        assertEquals(1600, Turn.getTurnYear(turn1600 + 1));
    }

    // M11-TP2: ENTRY -> N0 -(B1:false)-> N1 -> N3 -> EXIT (year < seasonYear)
    public void testM11_TP2_GetTurnYearBeforeSeasonYear() {
        assertEquals(1492, Turn.getTurnYear(1));
    }

    // ------------------------------------------------------------------
    // M12: getTurnSeason(I)I — 2 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M12-TP1: ENTRY -> N0 -(B1:true)-> N2 -> N3 -> EXIT (year >= seasonYear)
    public void testM12_TP1_GetTurnSeasonAtOrAfterSeasonYear() {
        int turn1600 = Turn.yearToTurn(1600);
        assertEquals(0, Turn.getTurnSeason(turn1600));
        assertEquals(1, Turn.getTurnSeason(turn1600 + 1));
    }

    // M12-TP2: ENTRY -> N0 -(B1:false)-> N1 -> N3 -> EXIT (year < seasonYear)
    public void testM12_TP2_GetTurnSeasonBeforeSeasonYear() {
        assertEquals(-1, Turn.getTurnSeason(1));
    }

    // ------------------------------------------------------------------
    // M13: getSeason()I — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M13-TP1: ENTRY -> N0 -> EXIT (Delegate getSeason() to getTurnSeason(turn))
    public void testM13_TP1_GetSeason() {
        assertEquals(-1, new Turn(1).getSeason());
    }

    // ------------------------------------------------------------------
    // M14: getLabel()Lnet/sf/freecol/common/model/StringTemplate; — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M14-TP1: ENTRY -> N0 -> EXIT (Delegate getLabel() to getTurnLabel(turn))
    public void testM14_TP1_GetLabel() {
        StringTemplate st = new Turn(1).getLabel();
        assertNotNull(st);
    }

    // ------------------------------------------------------------------
    // M15: getTurnLabel(I)Lnet/sf/freecol/common/model/StringTemplate; — 2 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M15-TP1: ENTRY -> N0 -(B1:true)-> N2 -> EXIT (season < 0, before seasonYear)
    public void testM15_TP1_GetTurnLabelWithoutSeason() {
        StringTemplate st = Turn.getTurnLabel(1);
        assertNotNull(st);
        assertEquals(1, st.entryList().size());
    }

    // M15-TP2: ENTRY -> N0 -(B1:false)-> N1 -> N2 -> EXIT (season >= 0, at/after seasonYear)
    public void testM15_TP2_GetTurnLabelWithSeason() {
        StringTemplate st = Turn.getTurnLabel(Turn.yearToTurn(1600));
        assertNotNull(st);
        assertEquals(3, st.entryList().size());
    }

    // ------------------------------------------------------------------
    // M16: isFirstTurn()Z — 2 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M16-TP1: ENTRY -> N0 -(B1:true)-> N2 -> N3 -> EXIT (turn != 1)
    public void testM16_TP1_IsFirstTurnFalse() {
        assertFalse(new Turn(2).isFirstTurn());
    }

    // M16-TP2: ENTRY -> N0 -(B1:false)-> N1 -> N3 -> EXIT (turn == 1)
    public void testM16_TP2_IsFirstTurnTrue() {
        assertTrue(new Turn(1).isFirstTurn());
    }

    // ------------------------------------------------------------------
    // M17: isFirstSeasonTurn()Z — 2 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M17-TP1: ENTRY -> N0 -(B1:true)-> N2 -> N3 -> EXIT (turn != yearToTurn(seasonYear))
    public void testM17_TP1_IsFirstSeasonTurnFalse() {
        assertFalse(new Turn(1).isFirstSeasonTurn());
    }

    // M17-TP2: ENTRY -> N0 -(B1:false)-> N1 -> N3 -> EXIT (turn == yearToTurn(seasonYear))
    public void testM17_TP2_IsFirstSeasonTurnTrue() {
        assertTrue(new Turn(Turn.yearToTurn(Turn.getSeasonYear())).isFirstSeasonTurn());
    }

    // ------------------------------------------------------------------
    // M18: getSaveGameSuffix()Ljava/lang/String; — 2 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M18-TP1: ENTRY -> N0 -(B1:true)-> N2 -> EXIT (season < 0)
    public void testM18_TP1_GetSaveGameSuffixWithoutSeason() {
        assertEquals("1492", new Turn(1).getSaveGameSuffix());
    }

    // M18-TP2: ENTRY -> N0 -(B1:false)-> N1 -> N2 -> EXIT (season >= 0)
    public void testM18_TP2_GetSaveGameSuffixWithSeason() {
        String suffix = new Turn(Turn.yearToTurn(1600)).getSaveGameSuffix();
        assertTrue(suffix.startsWith("1600_1_"));
    }

    // ------------------------------------------------------------------
    // M19: getTurnsText(I)Ljava/lang/String; — 3 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M19-TP1: ENTRY -> N0 -(B1:true)-> N2 -(B2:true)-> N4 -> N5 -> EXIT (turns != UNDEFINED && turns < 0)
    public void testM19_TP1_GetTurnsTextNegative() {
        assertEquals(">2", Turn.getTurnsText(-3));
    }

    // M19-TP2: ENTRY -> N0 -(B1:true)-> N2 -(B2:false)-> N3 -> N5 -> EXIT (turns != UNDEFINED && turns >= 0)
    public void testM19_TP2_GetTurnsTextNonNegative() {
        assertEquals("5", Turn.getTurnsText(5));
        assertEquals("0", Turn.getTurnsText(0));
    }

    // M19-TP3: ENTRY -> N0 -(B1:false)-> N1 -> N5 -> EXIT (turns == UNDEFINED)
    public void testM19_TP3_GetTurnsTextUndefined() {
        String text = Turn.getTurnsText(Constants.UNDEFINED);
        assertNotNull(text);
        assertFalse(text.isEmpty());
    }

    // ------------------------------------------------------------------
    // M20: equals(Ljava/lang/Object;)Z — 3 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M20-TP1: ENTRY -> N0 -(B1:true)-> N5 -> EXIT (!(o instanceof Turn))
    public void testM20_TP1_EqualsNonTurn() {
        Turn t = new Turn(1);
        assertFalse(t.equals(null));
        assertFalse(t.equals("1"));
    }

    // M20-TP2: ENTRY -> N0 -(B1:false)-> N1 -(B2:true)-> N3 -> N4 -> EXIT (o instanceof Turn && turn != o.turn)
    public void testM20_TP2_EqualsDifferentTurn() {
        assertFalse(new Turn(1).equals(new Turn(2)));
    }

    // M20-TP3: ENTRY -> N0 -(B1:false)-> N1 -(B2:false)-> N2 -> N4 -> EXIT (o instanceof Turn && turn == o.turn)
    public void testM20_TP3_EqualsSameTurn() {
        assertTrue(new Turn(3).equals(new Turn(3)));
    }

    // ------------------------------------------------------------------
    // M21: hashCode()I — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M21-TP1: ENTRY -> N0 -> EXIT (Return turn as hashCode)
    public void testM21_TP1_HashCode() {
        assertEquals(42, new Turn(42).hashCode());
    }

    // ------------------------------------------------------------------
    // M22: toString()Ljava/lang/String; — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M22-TP1: ENTRY -> N0 -> EXIT (Return String.valueOf(turn))
    public void testM22_TP1_ToString() {
        assertEquals("15", new Turn(15).toString());
    }

    // ------------------------------------------------------------------
    // M23: <clinit>()V — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M23-TP1: ENTRY -> N0 -> EXIT (Static initializer sets default startingYear=1492, seasonYear=1600, seasonNumber=2)
    public void testM23_TP1_ClassInitDefaults() {
        assertEquals(1492, Turn.getStartingYear());
        assertEquals(1600, Turn.getSeasonYear());
        assertEquals(2, Turn.getSeasonNumber());
    }
}
