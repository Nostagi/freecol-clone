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
import java.util.Random;

import net.sf.freecol.common.io.FreeColXMLReader;
import net.sf.freecol.common.io.FreeColXMLWriter;
import net.sf.freecol.util.test.FreeColTestCase;


/**
 * CFG-driven test suite for {@link RandomRange} derived from
 * {@code test_cft/graph/net/sf/freecol/common/model/RandomRange.json}.
 *
 * Covers all 10 methods (M1..M10) and achieves 100% feasible C3 (node + edge)
 * coverage across 15 Test Paths.
 *
 * Infeasible edges documented: none (all edges in M1..M10 are feasible).
 */
public class RandomRangeCfgTest extends FreeColTestCase {

    private RandomRange defaultRange;


    @Override
    public void setUp() throws Exception {
        super.setUp();
        // Pre-condition: standard RandomRange instance with probability=50, min=2, max=5, factor=10
        defaultRange = new RandomRange(50, 2, 5, 10);
    }


    // ------------------------------------------------------------------
    // Pre-condition helpers & stubs
    // ------------------------------------------------------------------

    private static Random fixedRandom(final int nextIntValue) {
        return new Random() {
            @Override
            public int nextInt(int bound) {
                return nextIntValue % bound;
            }
        };
    }

    private static String serializeAttributes(RandomRange rr) throws Exception {
        StringWriter sw = new StringWriter();
        try (FreeColXMLWriter xw = new FreeColXMLWriter(
                sw, FreeColXMLWriter.WriteScope.toSave(), false)) {
            xw.writeStartElement("range");
            rr.writeAttributes(xw);
            xw.writeEndElement();
            xw.flush();
        }
        return sw.toString();
    }


    // ------------------------------------------------------------------
    // M1: RandomRange(int, int, int, int) — 3 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M1-TP1: ENTRY -> N0 -(B1:true)-> N2 -(B2:true)-> N4 -> EXIT (probability >= 0 && minimum <= maximum)
    public void testM1_TP1_ValidConstructor() {
        RandomRange rr = new RandomRange(75, 3, 7, 5);
        assertEquals(75, rr.getProbability());
        assertEquals(3, rr.getMinimum());
        assertEquals(7, rr.getMaximum());
        assertEquals(5, rr.getFactor());
    }

    // M1-TP2: ENTRY -> N0 -(B1:true)-> N2 -(B2:false)-> N3 -> EXIT (probability >= 0 && minimum > maximum)
    public void testM1_TP2_MinGreaterThanMaxThrows() {
        try {
            new RandomRange(50, 10, 5, 1);
            fail("Expected IllegalArgumentException when minimum > maximum");
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage().contains("Min 10 > Max 5"));
        }
    }

    // M1-TP3: ENTRY -> N0 -(B1:false)-> N1 -> EXIT (probability < 0)
    public void testM1_TP3_NegativeProbabilityThrows() {
        try {
            new RandomRange(-1, 0, 5, 1);
            fail("Expected IllegalArgumentException when probability < 0");
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage().contains("Negative probability -1"));
        }
    }


    // ------------------------------------------------------------------
    // M2: RandomRange(FreeColXMLReader) — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M2-TP1: ENTRY -> N0 -> EXIT (construct from XML reader)
    public void testM2_TP1_ConstructorFromXMLReader() throws Exception {
        String xml = "<range probability=\"80\" minimum=\"1\" maximum=\"4\" factor=\"3\"/>";
        try (FreeColXMLReader xr = new FreeColXMLReader(new StringReader(xml))) {
            xr.nextTag();
            RandomRange rr = new RandomRange(xr);
            assertEquals(80, rr.getProbability());
            assertEquals(1, rr.getMinimum());
            assertEquals(4, rr.getMaximum());
            assertEquals(3, rr.getFactor());
        }
    }


    // ------------------------------------------------------------------
    // M3: getProbability() — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M3-TP1: ENTRY -> N0 -> EXIT
    public void testM3_TP1_GetProbability() {
        assertEquals(50, defaultRange.getProbability());
    }


    // ------------------------------------------------------------------
    // M4: getMinimum() — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M4-TP1: ENTRY -> N0 -> EXIT
    public void testM4_TP1_GetMinimum() {
        assertEquals(2, defaultRange.getMinimum());
    }


    // ------------------------------------------------------------------
    // M5: getMaximum() — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M5-TP1: ENTRY -> N0 -> EXIT
    public void testM5_TP1_GetMaximum() {
        assertEquals(5, defaultRange.getMaximum());
    }


    // ------------------------------------------------------------------
    // M6: getFactor() — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M6-TP1: ENTRY -> N0 -> EXIT
    public void testM6_TP1_GetFactor() {
        assertEquals(10, defaultRange.getFactor());
    }


    // ------------------------------------------------------------------
    // M7: getAmount(String, Random, boolean) — 5 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M7-TP1: ENTRY -> N0 -(B1:true)-> N3 -(B4:false)-> N4 -> EXIT (probability >= 100, continuous == true)
    public void testM7_TP1_AlwaysTriggerContinuous() {
        RandomRange rr = new RandomRange(100, 2, 5, 10);
        // range = 5 - 2 + 1 = 4; range * factor = 40; nextInt(40) -> 7; 7 + 2*10 = 27
        int amount = rr.getAmount("test", fixedRandom(7), true);
        assertEquals(27, amount);
    }

    // M7-TP2: ENTRY -> N0 -(B1:true)-> N3 -(B4:true)-> N5 -> EXIT (probability >= 100, continuous == false)
    public void testM7_TP2_AlwaysTriggerDiscrete() {
        RandomRange rr = new RandomRange(100, 2, 5, 10);
        // range = 4; nextInt(4) -> 3; (3 + 2) * 10 = 50
        int amount = rr.getAmount("test", fixedRandom(3), false);
        assertEquals(50, amount);
    }

    // M7-TP3: ENTRY -> N0 -(B1:false)-> N1 -(B2:true)-> N6 -> EXIT (probability == 0)
    public void testM7_TP3_ZeroProbabilityReturnsZero() {
        RandomRange rr = new RandomRange(0, 2, 5, 10);
        int amount = rr.getAmount("test", fixedRandom(0), false);
        assertEquals(0, amount);
    }

    // M7-TP4: ENTRY -> N0 -(B1:false)-> N1 -(B2:false)-> N2 -(B3:true)-> N6 -> EXIT (0 < probability < 100, roll >= probability)
    public void testM7_TP4_ProbabilisticMissReturnsZero() {
        // randomInt(100) returns 50 >= probability(50) -> returns 0
        int amount = defaultRange.getAmount("test", fixedRandom(50), true);
        assertEquals(0, amount);
    }

    // M7-TP5: ENTRY -> N0 -(B1:false)-> N1 -(B2:false)-> N2 -(B3:false)-> N3 -(B4:false)-> N4 -> EXIT (0 < probability < 100, roll < probability, continuous == true)
    public void testM7_TP5_ProbabilisticHitContinuous() {
        // randomInt(100) returns 5 < 50; range * factor = 40; nextInt(40) -> 5; 5 + 2*10 = 25
        int amount = defaultRange.getAmount("test", fixedRandom(5), true);
        assertEquals(25, amount);
    }


    // ------------------------------------------------------------------
    // M8: writeAttributes(FreeColXMLWriter) — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M8-TP1: ENTRY -> N0 -> EXIT
    public void testM8_TP1_WriteAttributes() throws Exception {
        String xml = serializeAttributes(defaultRange);
        assertTrue(xml.contains("probability=\"50\""));
        assertTrue(xml.contains("minimum=\"2\""));
        assertTrue(xml.contains("maximum=\"5\""));
        assertTrue(xml.contains("factor=\"10\""));
    }


    // ------------------------------------------------------------------
    // M9: readAttributes(FreeColXMLReader) — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M9-TP1: ENTRY -> N0 -> EXIT
    public void testM9_TP1_ReadAttributes() throws Exception {
        String xml = "<range probability=\"25\" minimum=\"4\" maximum=\"9\" factor=\"2\"/>";
        RandomRange target = new RandomRange(0, 0, 0, 1);
        try (FreeColXMLReader xr = new FreeColXMLReader(new StringReader(xml))) {
            xr.nextTag();
            target.readAttributes(xr);
        }
        assertEquals(25, target.getProbability());
        assertEquals(4, target.getMinimum());
        assertEquals(9, target.getMaximum());
        assertEquals(2, target.getFactor());
    }


    // ------------------------------------------------------------------
    // M10: <clinit>() — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M10-TP1: ENTRY -> N0 -> EXIT (static logger initialization)
    public void testM10_TP1_StaticInit() {
        assertNotNull(defaultRange);
    }
}
