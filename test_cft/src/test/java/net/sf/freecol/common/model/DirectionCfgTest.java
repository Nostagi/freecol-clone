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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.logging.Logger;

import net.sf.freecol.util.test.FreeColTestCase;
import net.sf.freecol.util.test.MockPseudoRandom;


/**
 * CFG-driven test suite for {@link Direction} derived from
 * {@code test_cft/graph/net/sf/freecol/common/model/Direction.json}.
 *
 * Covers all 17 methods (M1..M17, including {@code values}, {@code valueOf},
 * {@code <init>}, {@code $values}, and {@code <clinit>}) and achieves 100% C3
 * (node + edge) coverage across 26 Test Paths.
 */
public class DirectionCfgTest extends FreeColTestCase {

    private static final Logger logger = Logger.getLogger(DirectionCfgTest.class.getName());

    @Override
    public void setUp() throws Exception {
        super.setUp();
        // Pre-condition: Direction enum constants and static lists initialized
    }

    // ------------------------------------------------------------------
    // M1: values()[Direction — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M1-TP1: ENTRY -> N0 -> EXIT (returns all 8 Direction constants)
    public void testM1_TP1_Values() {
        Direction[] dirs = Direction.values();
        assertEquals(8, dirs.length);
        assertEquals(Direction.N, dirs[0]);
        assertEquals(Direction.NW, dirs[7]);
    }

    // ------------------------------------------------------------------
    // M2: valueOf(String)Direction — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M2-TP1: ENTRY -> N0 -> EXIT (resolves enum constant by name)
    public void testM2_TP1_ValueOf() {
        assertEquals(Direction.NE, Direction.valueOf("NE"));
        assertEquals(Direction.SW, Direction.valueOf("SW"));
    }

    // ------------------------------------------------------------------
    // M3: <init>(String, int, int, int, int, int)V — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M3-TP1: ENTRY -> N0 -> EXIT (enum constructor invoked during class initialization)
    public void testM3_TP1_EnumInitDeltas() {
        Map.Position evenStep = Direction.NE.step(4, 4);
        assertEquals(4, evenStep.getX());
        assertEquals(3, evenStep.getY());
    }

    // ------------------------------------------------------------------
    // M4: step(int, int)Map$Position — 2 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M4-TP1: ENTRY -> N0 -(B1:true)-> N2 -> N3 -> EXIT ((y & 1) == 0, even y)
    public void testM4_TP1_StepEvenY() {
        Map.Position pos = Direction.NE.step(5, 4);
        assertEquals(5, pos.getX());
        assertEquals(3, pos.getY());
    }

    // M4-TP2: ENTRY -> N0 -(B1:false)-> N1 -> N3 -> EXIT ((y & 1) != 0, odd y)
    public void testM4_TP2_StepOddY() {
        Map.Position pos = Direction.NE.step(5, 5);
        assertEquals(6, pos.getX());
        assertEquals(4, pos.getY());
    }

    // ------------------------------------------------------------------
    // M5: rotate(int)Direction — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M5-TP1: ENTRY -> N0 -> EXIT (rotates direction by n places modulo 8)
    public void testM5_TP1_Rotate() {
        assertEquals(Direction.E, Direction.N.rotate(2));
        assertEquals(Direction.NW, Direction.N.rotate(-1));
    }

    // ------------------------------------------------------------------
    // M6: getNextDirection()Direction — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M6-TP1: ENTRY -> N0 -> EXIT (rotate(1))
    public void testM6_TP1_GetNextDirection() {
        assertEquals(Direction.NE, Direction.N.getNextDirection());
        assertEquals(Direction.N, Direction.NW.getNextDirection());
    }

    // ------------------------------------------------------------------
    // M7: getPreviousDirection()Direction — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M7-TP1: ENTRY -> N0 -> EXIT (rotate(-1))
    public void testM7_TP1_GetPreviousDirection() {
        assertEquals(Direction.NW, Direction.N.getPreviousDirection());
        assertEquals(Direction.W, Direction.NW.getPreviousDirection());
    }

    // ------------------------------------------------------------------
    // M8: getReverseDirection()Direction — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M8-TP1: ENTRY -> N0 -> EXIT (rotate(NUMBER_OF_DIRECTIONS / 2))
    public void testM8_TP1_GetReverseDirection() {
        assertEquals(Direction.S, Direction.N.getReverseDirection());
        assertEquals(Direction.SW, Direction.NE.getReverseDirection());
    }

    // ------------------------------------------------------------------
    // M9: getEWMirroredDirection()Direction — 8 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M9-TP1: ENTRY -> N0 -(B1:default)-> N7 -> EXIT (N -> N)
    public void testM9_TP1_GetEWMirroredNorth() {
        assertEquals(Direction.N, Direction.N.getEWMirroredDirection());
    }

    // M9-TP2: ENTRY -> N0 -(B1:case:1)-> N4 -> EXIT (NE -> NW)
    public void testM9_TP2_GetEWMirroredNorthEast() {
        assertEquals(Direction.NW, Direction.NE.getEWMirroredDirection());
    }

    // M9-TP3: ENTRY -> N0 -(B1:case:2)-> N5 -> EXIT (E -> W)
    public void testM9_TP3_GetEWMirroredEast() {
        assertEquals(Direction.W, Direction.E.getEWMirroredDirection());
    }

    // M9-TP4: ENTRY -> N0 -(B1:case:3)-> N6 -> EXIT (SE -> SW)
    public void testM9_TP4_GetEWMirroredSouthEast() {
        assertEquals(Direction.SW, Direction.SE.getEWMirroredDirection());
    }

    // M9-TP5: ENTRY -> N0 -(B1:case:4)-> N7 -> EXIT (S -> S)
    public void testM9_TP5_GetEWMirroredSouth() {
        assertEquals(Direction.S, Direction.S.getEWMirroredDirection());
    }

    // M9-TP6: ENTRY -> N0 -(B1:case:5)-> N3 -> EXIT (SW -> SE)
    public void testM9_TP6_GetEWMirroredSouthWest() {
        assertEquals(Direction.SE, Direction.SW.getEWMirroredDirection());
    }

    // M9-TP7: ENTRY -> N0 -(B1:case:6)-> N2 -> EXIT (W -> E)
    public void testM9_TP7_GetEWMirroredWest() {
        assertEquals(Direction.E, Direction.W.getEWMirroredDirection());
    }

    // M9-TP8: ENTRY -> N0 -(B1:case:7)-> N1 -> EXIT (NW -> NE)
    public void testM9_TP8_GetEWMirroredNorthWest() {
        assertEquals(Direction.NE, Direction.NW.getEWMirroredDirection());
    }

    // ------------------------------------------------------------------
    // M10: getRandomDirection(String, Logger, Random)Direction — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M10-TP1: ENTRY -> N0 -> EXIT (picks Direction from randomInt)
    public void testM10_TP1_GetRandomDirection() {
        Random prng = new MockPseudoRandom(Collections.singletonList(2), false);
        assertEquals(Direction.E, Direction.getRandomDirection("test", logger, prng));
    }

    // ------------------------------------------------------------------
    // M11: getRandomDirections(String, Logger, Random)[Direction — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M11-TP1: ENTRY -> N0 -> EXIT (shuffles allDirections)
    public void testM11_TP1_GetRandomDirections() {
        Direction[] dirs = Direction.getRandomDirections("test", logger, new Random(1L));
        assertEquals(Direction.NUMBER_OF_DIRECTIONS, dirs.length);
        List<Direction> list = Arrays.asList(dirs);
        for (Direction d : Direction.values()) {
            assertTrue(list.contains(d));
        }
    }

    // ------------------------------------------------------------------
    // M12: getClosestDirections(String, Logger, Random)[Direction — 2 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M12-TP1: ENTRY -> N0 -> N1 -(B1:false)-> N2 -(B2:false)-> N3 -> N5 -(B3:false)-> N6 -> N8 -> N1 ... -(B1:true)-> N9 -> EXIT
    // (r = 0: (r & mask) == 0 on all 3 loop iterations)
    public void testM12_TP1_GetClosestDirectionsAllZeroBits() {
        Random prng = new MockPseudoRandom(Collections.singletonList(0), false);
        Direction[] closest = Direction.N.getClosestDirections("test", logger, prng);
        assertEquals(8, closest.length);
        assertEquals(Direction.N, closest[0]);
        assertEquals(Direction.NE, closest[1]);
        assertEquals(Direction.NW, closest[2]);
        assertEquals(Direction.E, closest[3]);
        assertEquals(Direction.W, closest[4]);
        assertEquals(Direction.SE, closest[5]);
        assertEquals(Direction.SW, closest[6]);
        assertEquals(Direction.S, closest[7]);
    }

    // M12-TP2: ENTRY -> N0 -> N1 -(B1:false)-> N2 -(B2:true)-> N4 -> N5 -(B3:true)-> N7 -> N8 -> N1 ... -(B1:true)-> N9 -> EXIT
    // (r = 7: (r & mask) != 0 on all 3 loop iterations)
    public void testM12_TP2_GetClosestDirectionsAllOneBits() {
        Random prng = new MockPseudoRandom(Collections.singletonList(7), false);
        Direction[] closest = Direction.N.getClosestDirections("test", logger, prng);
        assertEquals(8, closest.length);
        assertEquals(Direction.N, closest[0]);
        assertEquals(Direction.NW, closest[1]);
        assertEquals(Direction.NE, closest[2]);
        assertEquals(Direction.W, closest[3]);
        assertEquals(Direction.E, closest[4]);
        assertEquals(Direction.SW, closest[5]);
        assertEquals(Direction.SE, closest[6]);
        assertEquals(Direction.S, closest[7]);
    }

    // ------------------------------------------------------------------
    // M13: angleToDirection(double)Direction — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M13-TP1: ENTRY -> N0 -> EXIT (maps angle in radians to Direction)
    public void testM13_TP1_AngleToDirection() {
        assertEquals(Direction.N, Direction.angleToDirection(0.0));
        assertEquals(Direction.E, Direction.angleToDirection(Math.PI / 2));
    }

    // ------------------------------------------------------------------
    // M14: getKey()String — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M14-TP1: ENTRY -> N0 -> EXIT (returns "direction." + this)
    public void testM14_TP1_GetKey() {
        assertEquals("direction.NE", Direction.NE.getKey());
    }

    // ------------------------------------------------------------------
    // M15: getNameKey()String — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M15-TP1: ENTRY -> N0 -> EXIT (returns Messages.nameKey("model." + getKey()))
    public void testM15_TP1_GetNameKey() {
        assertEquals("model.direction.N.name", Direction.N.getNameKey());
    }

    // ------------------------------------------------------------------
    // M16: $values()[Direction — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M16-TP1: ENTRY -> N0 -> EXIT (synthetic enum $values() invoked during <clinit>)
    public void testM16_TP1_SyntheticValuesArray() {
        assertEquals(8, Direction.NUMBER_OF_DIRECTIONS);
    }

    // ------------------------------------------------------------------
    // M17: <clinit>()V — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M17-TP1: ENTRY -> N0 -> EXIT (static lists allDirections, longSides, corners initialized)
    public void testM17_TP1_StaticInitializer() {
        assertEquals(8, Direction.allDirections.size());
        assertEquals(4, Direction.longSides.size());
        assertEquals(4, Direction.corners.size());
    }
}
