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

package net.sf.freecol.common.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.stream.Stream;

import net.sf.freecol.util.test.FreeColTestCase;
import net.sf.freecol.util.test.MockPseudoRandom;


/**
 * CFG-driven test suite for {@link RandomUtils} derived from
 * {@code test_cft/graph/net/sf/freecol/common/util/RandomUtils.json}.
 *
 * Covers all 9 methods (M1..M9) and achieves 100% feasible C3 (node + edge)
 * coverage across 24 Test Paths.
 *
 * Infeasible edges documented:
 * - None (100% of CFG nodes and edges in RandomUtils.json are feasible).
 */
public class RandomUtilsCfgTest extends FreeColTestCase {

    private RecordingHandler logHandler;
    private Logger finestLogger;
    private Logger infoLogger;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        // Pre-condition: Standard FreeColTestCase environment initialized;
        // isolated anonymous loggers configured at FINEST and INFO levels
        // with an in-memory RecordingHandler to observe log output.
        logHandler = new RecordingHandler();
        finestLogger = createIsolatedLogger(Level.FINEST, logHandler);
        infoLogger = createIsolatedLogger(Level.INFO, logHandler);
    }

    // ------------------------------------------------------------------
    // Pre-condition helpers & stubs
    // ------------------------------------------------------------------

    /**
     * In-memory log handler that records emitted log messages for assertion.
     */
    private static final class RecordingHandler extends Handler {
        private final List<String> messages = new ArrayList<>();

        @Override
        public void publish(LogRecord record) {
            if (record != null) {
                messages.add(record.getMessage());
            }
        }

        @Override
        public void flush() {
        }

        @Override
        public void close() {
            messages.clear();
        }

        List<String> getMessages() {
            return messages;
        }
    }

    /**
     * Creates an isolated anonymous logger at the requested level without
     * propagating records to parent handlers.
     */
    private static Logger createIsolatedLogger(Level level, Handler handler) {
        Logger logger = Logger.getAnonymousLogger();
        logger.setUseParentHandlers(false);
        logger.setLevel(level);
        handler.setLevel(Level.ALL);
        logger.addHandler(handler);
        return logger;
    }

    // ------------------------------------------------------------------
    // M1: RandomUtils() — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M1-TP1: ENTRY -> N0 -> EXIT (default constructor invocation)
    public void testM1_TP1_Constructor() {
        RandomUtils utils = new RandomUtils();
        assertNotNull(utils);
    }

    // ------------------------------------------------------------------
    // M2: randomInt(Logger, String, Random, int) — 2 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M2-TP1: ENTRY -> N0 -(B1:true)-> N2 -> EXIT (logger == null)
    public void testM2_TP1_RandomIntNullLogger() {
        MockPseudoRandom random = new MockPseudoRandom(Collections.singletonList(4), false);
        int result = RandomUtils.randomInt(null, "test", random, 10);
        assertEquals(4, result);
        assertTrue(logHandler.getMessages().isEmpty());
    }

    // M2-TP2: ENTRY -> N0 -(B1:false)-> N1 -> N2 -> EXIT (logger != null)
    public void testM2_TP2_RandomIntNonNullLogger() {
        MockPseudoRandom random = new MockPseudoRandom(Collections.singletonList(7), false);
        int result = RandomUtils.randomInt(finestLogger, "roll", random, 10);
        assertEquals(7, result);
        assertEquals(1, logHandler.getMessages().size());
        assertEquals("roll random(10) = 7", logHandler.getMessages().get(0));
    }

    // ------------------------------------------------------------------
    // M3: logArray(Logger, String, int[]) — 4 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M3-TP1: ENTRY -> N0 -(B1:true)-> N6 -> EXIT (logger == null)
    public void testM3_TP1_LogArrayNullLogger() {
        RandomUtils.logArray(null, "arr", new int[] { 1, 2 });
        assertTrue(logHandler.getMessages().isEmpty());
    }

    // M3-TP2: ENTRY -> N0 -(B1:false)-> N1 -(B2:true)-> N6 -> EXIT (logger != null && !isLoggable(FINEST))
    public void testM3_TP2_LogArrayNotLoggableFinest() {
        RandomUtils.logArray(infoLogger, "arr", new int[] { 1, 2 });
        assertTrue(logHandler.getMessages().isEmpty());
    }

    // M3-TP3: ENTRY -> N0 -(B1:false)-> N1 -(B2:false)-> N2 -> N3 -(B3:true)-> N5 -> N6 -> EXIT (empty array, 0 loop iterations)
    public void testM3_TP3_LogArrayEmptyArray() {
        RandomUtils.logArray(finestLogger, "empty", new int[0]);
        assertEquals(1, logHandler.getMessages().size());
        assertEquals("empty random = [ ]", logHandler.getMessages().get(0));
    }

    // M3-TP4: ENTRY -> N0 -(B1:false)-> N1 -(B2:false)-> N2 -> N3 -(B3:false)-> N4 -> N3 -(B3:true)-> N5 -> N6 -> EXIT (non-empty array, loop body & back-edge)
    public void testM3_TP4_LogArrayNonEmptyArray() {
        RandomUtils.logArray(finestLogger, "vals", new int[] { 3, 5, 8 });
        assertEquals(1, logHandler.getMessages().size());
        assertEquals("vals random = [ 3 5 8 ]", logHandler.getMessages().get(0));
    }

    // ------------------------------------------------------------------
    // M4: randomInts(Logger, String, Random, int, int) — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M4-TP1: ENTRY -> N0 -> EXIT (generate n ints in [0, range) and delegate logging)
    public void testM4_TP1_RandomInts() {
        Random random = new Random(42L);
        int[] result = RandomUtils.randomInts(finestLogger, "batch", random, 100, 4);
        assertEquals(4, result.length);
        for (int v : result) {
            assertTrue(v >= 0 && v < 100);
        }
        assertEquals(1, logHandler.getMessages().size());
        assertTrue(logHandler.getMessages().get(0).startsWith("batch random = ["));
    }

    // ------------------------------------------------------------------
    // M5: randomFloat(Logger, String, Random) — 3 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M5-TP1: ENTRY -> N0 -(B1:true)-> N3 -> EXIT (logger == null)
    public void testM5_TP1_RandomFloatNullLogger() {
        MockPseudoRandom random = new MockPseudoRandom(Collections.singletonList(0), false);
        float result = RandomUtils.randomFloat(null, "flt", random);
        assertEquals(0.0f, result, 1e-6f);
        assertTrue(logHandler.getMessages().isEmpty());
    }

    // M5-TP2: ENTRY -> N0 -(B1:false)-> N1 -(B2:true)-> N3 -> EXIT (logger != null && !isLoggable(FINEST))
    public void testM5_TP2_RandomFloatNotLoggableFinest() {
        MockPseudoRandom random = new MockPseudoRandom(Collections.singletonList(0), false);
        float result = RandomUtils.randomFloat(infoLogger, "flt", random);
        assertEquals(0.0f, result, 1e-6f);
        assertTrue(logHandler.getMessages().isEmpty());
    }

    // M5-TP3: ENTRY -> N0 -(B1:false)-> N1 -(B2:false)-> N2 -> N3 -> EXIT (logger != null && isLoggable(FINEST))
    public void testM5_TP3_RandomFloatLoggableFinest() {
        MockPseudoRandom random = new MockPseudoRandom(Collections.singletonList(0), false);
        float result = RandomUtils.randomFloat(finestLogger, "flt", random);
        assertEquals(0.0f, result, 1e-6f);
        assertEquals(1, logHandler.getMessages().size());
        assertTrue(logHandler.getMessages().get(0).startsWith("flt random(1.0f) = "));
    }

    // ------------------------------------------------------------------
    // M6: randomDouble(Logger, String, Random) — 3 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M6-TP1: ENTRY -> N0 -(B1:true)-> N3 -> EXIT (logger == null)
    public void testM6_TP1_RandomDoubleNullLogger() {
        MockPseudoRandom random = new MockPseudoRandom(Collections.singletonList(0), false);
        double result = RandomUtils.randomDouble(null, "dbl", random);
        assertEquals(0.0, result, 1e-9);
        assertTrue(logHandler.getMessages().isEmpty());
    }

    // M6-TP2: ENTRY -> N0 -(B1:false)-> N1 -(B2:true)-> N3 -> EXIT (logger != null && !isLoggable(FINEST))
    public void testM6_TP2_RandomDoubleNotLoggableFinest() {
        MockPseudoRandom random = new MockPseudoRandom(Collections.singletonList(0), false);
        double result = RandomUtils.randomDouble(infoLogger, "dbl", random);
        assertEquals(0.0, result, 1e-9);
        assertTrue(logHandler.getMessages().isEmpty());
    }

    // M6-TP3: ENTRY -> N0 -(B1:false)-> N1 -(B2:false)-> N2 -> N3 -> EXIT (logger != null && isLoggable(FINEST))
    public void testM6_TP3_RandomDoubleLoggableFinest() {
        MockPseudoRandom random = new MockPseudoRandom(Collections.singletonList(0), false);
        double result = RandomUtils.randomDouble(finestLogger, "dbl", random);
        assertEquals(0.0, result, 1e-9);
        assertEquals(1, logHandler.getMessages().size());
        assertTrue(logHandler.getMessages().get(0).startsWith("dbl random(1.0) = "));
    }

    // ------------------------------------------------------------------
    // M7: getRandomMember(Logger, String, List, Random) — 3 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M7-TP1: ENTRY -> N0 -(B1:case:0)-> N1 -> EXIT (list.size() == 0)
    public void testM7_TP1_GetRandomMemberEmptyList() {
        Random random = new Random(1L);
        String result = RandomUtils.getRandomMember(finestLogger, "pick", Collections.<String>emptyList(), random);
        assertNull(result);
    }

    // M7-TP2: ENTRY -> N0 -(B1:case:1)-> N2 -> EXIT (list.size() == 1)
    public void testM7_TP2_GetRandomMemberSingletonList() {
        Random random = new Random(1L);
        String result = RandomUtils.getRandomMember(finestLogger, "pick", Collections.singletonList("only"), random);
        assertEquals("only", result);
    }

    // M7-TP3: ENTRY -> N0 -(B1:default)-> N3 -> EXIT (list.size() >= 2)
    public void testM7_TP3_GetRandomMemberMultiElementList() {
        MockPseudoRandom random = new MockPseudoRandom(Collections.singletonList(1), false);
        List<String> items = Arrays.asList("alpha", "beta", "gamma");
        String result = RandomUtils.getRandomMember(finestLogger, "pick", items, random);
        assertEquals("beta", result);
        assertEquals(1, logHandler.getMessages().size());
        assertEquals("pick random(3) = 1", logHandler.getMessages().get(0));
    }

    // ------------------------------------------------------------------
    // M8: getRandomMember(Logger, String, Stream, Random) — 2 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M8-TP1: ENTRY -> N0 -(B1:false)-> N1 -> N3 -> EXIT (stream == null)
    public void testM8_TP1_GetRandomMemberNullStream() {
        Random random = new Random(1L);
        String result = RandomUtils.getRandomMember(finestLogger, "stream", (Stream<String>) null, random);
        assertNull(result);
    }

    // M8-TP2: ENTRY -> N0 -(B1:true)-> N2 -> N3 -> EXIT (stream != null)
    public void testM8_TP2_GetRandomMemberNonNullStream() {
        MockPseudoRandom random = new MockPseudoRandom(Collections.singletonList(2), false);
        Stream<String> stream = Stream.of("a", "b", "c");
        String result = RandomUtils.getRandomMember(finestLogger, "stream", stream, random);
        assertEquals("c", result);
    }

    // ------------------------------------------------------------------
    // M9: randomShuffle(Logger, String, List, Random) — 5 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M9-TP1: ENTRY -> N0 -(B1:true)-> N2 -> EXIT (list.size() <= 1)
    public void testM9_TP1_RandomShuffleSingleElementList() {
        List<String> list = new ArrayList<>(Collections.singletonList("solo"));
        RandomUtils.randomShuffle(finestLogger, "shuf", list, new Random(1L));
        assertEquals(Collections.singletonList("solo"), list);
        assertTrue(logHandler.getMessages().isEmpty());
    }

    // M9-TP2: ENTRY -> N0 -(B1:false)-> N1 -(B2:false)-> N2 -> EXIT (list.size() > 1 && random == null)
    public void testM9_TP2_RandomShuffleNullRandom() {
        List<String> list = new ArrayList<>(Arrays.asList("a", "b", "c"));
        RandomUtils.randomShuffle(finestLogger, "shuf", list, null);
        assertEquals(Arrays.asList("a", "b", "c"), list);
        assertTrue(logHandler.getMessages().isEmpty());
    }

    // M9-TP3: ENTRY -> N0 -(B1:false)-> N1 -(B2:true)-> N3 -(B3:true)-> N6 -> EXIT (list.size() > 1 && random != null && logger == null)
    public void testM9_TP3_RandomShuffleNullLogger() {
        List<String> list = new ArrayList<>(Arrays.asList("a", "b", "c", "d"));
        RandomUtils.randomShuffle(null, "shuf", list, new Random(42L));
        assertEquals(4, list.size());
        assertTrue(list.containsAll(Arrays.asList("a", "b", "c", "d")));
        assertTrue(logHandler.getMessages().isEmpty());
    }

    // M9-TP4: ENTRY -> N0 -(B1:false)-> N1 -(B2:true)-> N3 -(B3:false)-> N4 -(B4:true)-> N6 -> EXIT (logger != null && !isLoggable(FINEST))
    public void testM9_TP4_RandomShuffleNotLoggableFinest() {
        List<String> list = new ArrayList<>(Arrays.asList("a", "b", "c", "d"));
        RandomUtils.randomShuffle(infoLogger, "shuf", list, new Random(42L));
        assertEquals(4, list.size());
        assertTrue(list.containsAll(Arrays.asList("a", "b", "c", "d")));
        assertTrue(logHandler.getMessages().isEmpty());
    }

    // M9-TP5: ENTRY -> N0 -(B1:false)-> N1 -(B2:true)-> N3 -(B3:false)-> N4 -(B4:false)-> N5 -> N6 -> EXIT (logger != null && isLoggable(FINEST))
    public void testM9_TP5_RandomShuffleLoggableFinest() {
        List<String> list = new ArrayList<>(Arrays.asList("a", "b", "c", "d"));
        RandomUtils.randomShuffle(finestLogger, "shuf", list, new Random(42L));
        assertEquals(4, list.size());
        assertTrue(list.containsAll(Arrays.asList("a", "b", "c", "d")));
        assertEquals(1, logHandler.getMessages().size());
        assertEquals("shuf shuffle.", logHandler.getMessages().get(0));
    }
}
