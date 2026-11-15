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

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.Location;
import net.sf.freecol.common.model.Map;
import net.sf.freecol.util.test.FreeColTestCase;


/**
 * CFG-driven test suite for {@link LogBuilder} derived from
 * {@code test_cft/graph/net/sf/freecol/common/util/LogBuilder.json}.
 *
 * Covers all 14 methods (M1..M14) and achieves 100% feasible C3 (node + edge)
 * coverage across 44 Test Paths.
 *
 * Infeasible edges documented:
 * - None (100% of CFG nodes and edges in LogBuilder.json are feasible).
 */
public class LogBuilderCfgTest extends FreeColTestCase {

    private Method o2sMethod;
    private Method addInternalMethod;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        // Pre-condition: Standard FreeColTestCase environment initialized;
        // reflection handles prepared for private static helpers o2s and addInternal.
        o2sMethod = LogBuilder.class.getDeclaredMethod("o2s", Object.class);
        o2sMethod.setAccessible(true);
        addInternalMethod = LogBuilder.class.getDeclaredMethod(
            "addInternal", StringBuilder.class, Object[].class);
        addInternalMethod.setAccessible(true);
    }

    // ------------------------------------------------------------------
    // Pre-condition helpers & stubs
    // ------------------------------------------------------------------

    private String invokeO2s(Object o) throws Exception {
        return (String) o2sMethod.invoke(null, o);
    }

    private void invokeAddInternal(StringBuilder sb, Object... objects) throws Exception {
        addInternalMethod.invoke(null, sb, objects);
    }

    private static final class CapturingHandler extends Handler {
        final List<LogRecord> records = new ArrayList<>();

        @Override
        public void publish(LogRecord record) {
            records.add(record);
        }

        @Override
        public void flush() {
        }

        @Override
        public void close() {
        }
    }

    // ------------------------------------------------------------------
    // M1: <init>(int) — 2 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M1-TP1: ENTRY -> N0 -(B1:true)-> N2 -> N3 -> EXIT (size > 0)
    public void testM1_TP1_ConstructorPositiveSize() {
        LogBuilder lb = new LogBuilder(16);
        lb.add("ok");
        assertEquals(2, lb.size());
        assertEquals("ok", lb.toString());
    }

    // M1-TP2: ENTRY -> N0 -(B1:false)-> N1 -> N3 -> EXIT (size <= 0)
    public void testM1_TP2_ConstructorNonPositiveSize() {
        LogBuilder lb = new LogBuilder(0);
        lb.add("ignored");
        assertEquals(0, lb.size());
        assertEquals("", lb.toString());
    }

    // ------------------------------------------------------------------
    // M2: o2s(Object) — 5 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M2-TP1: ENTRY -> N0 -(B1:false)-> N1 -> N9 -> EXIT (o == null)
    public void testM2_TP1_O2sNull() throws Exception {
        assertEquals("null", invokeO2s(null));
    }

    // M2-TP2: ENTRY -> N0 -(B1:true)-> N2 -(B2:false)-> N3 -> N9 -> EXIT (o instanceof Class<?>)
    public void testM2_TP2_O2sClass() throws Exception {
        assertEquals(LogBuilder.class.getName(), invokeO2s(LogBuilder.class));
    }

    // M2-TP3: ENTRY -> N0 -(B1:true)-> N2 -(B2:true)-> N4 -(B3:false)-> N5 -> N9 -> EXIT (o instanceof String)
    public void testM2_TP3_O2sString() throws Exception {
        assertEquals("hello", invokeO2s("hello"));
    }

    // M2-TP4: ENTRY -> N0 -(B1:true)-> N2 -(B2:true)-> N4 -(B3:true)-> N6 -(B4:false)-> N7 -> N9 -> EXIT (o instanceof Location)
    public void testM2_TP4_O2sLocation() throws Exception {
        Game game = getStandardGame();
        Map map = getTestMap();
        game.setMap(map);
        Location loc = map.getTile(0, 0);
        assertEquals(loc.toShortString(), invokeO2s(loc));
    }

    // M2-TP5: ENTRY -> N0 -(B1:true)-> N2 -(B2:true)-> N4 -(B3:true)-> N6 -(B4:true)-> N8 -> N9 -> EXIT (other Object)
    public void testM2_TP5_O2sOtherObject() throws Exception {
        assertEquals("42", invokeO2s(Integer.valueOf(42)));
    }

    // ------------------------------------------------------------------
    // M3: addInternal(StringBuilder, Object...) — 3 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M3-TP1: ENTRY -> N0 -> N1 -(B1:true)-> N9 -> EXIT (objects empty)
    public void testM3_TP1_AddInternalEmpty() throws Exception {
        StringBuilder sb = new StringBuilder();
        invokeAddInternal(sb);
        assertEquals("", sb.toString());
    }

    // M3-TP2: ENTRY -> N0 -> N1 -(B1:false)-> N2 -(B2:true)-> N7 -> N8 -> N1 -(B1:true)-> N9 -> EXIT (non-array element)
    public void testM3_TP2_AddInternalSingleObject() throws Exception {
        StringBuilder sb = new StringBuilder();
        invokeAddInternal(sb, "abc");
        assertEquals("abc", sb.toString());
    }

    // M3-TP3: ENTRY -> N0 -> N1 -(B1:false)-> N2 -(B2:false)-> N3 -> N4 -(B3:false)-> N5 -> N4 -(B3:true)-> N6 -> N8 -> N1 -(B1:true)-> N9 -> EXIT (Object[] element)
    public void testM3_TP3_AddInternalNestedArray() throws Exception {
        StringBuilder sb = new StringBuilder();
        invokeAddInternal(sb, (Object) new Object[] { "x", Integer.valueOf(1) });
        assertEquals("x1", sb.toString());
    }

    // ------------------------------------------------------------------
    // M4: add(Object...) — 2 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M4-TP1: ENTRY -> N0 -(B1:true)-> N2 -> EXIT (sb == null)
    public void testM4_TP1_AddWhenNullBuffer() {
        LogBuilder lb = new LogBuilder(0);
        lb.add("a", "b");
        assertEquals("", lb.toString());
    }

    // M4-TP2: ENTRY -> N0 -(B1:false)-> N1 -> N2 -> EXIT (sb != null)
    public void testM4_TP2_AddWhenActiveBuffer() {
        LogBuilder lb = new LogBuilder(16);
        lb.add("a", "b");
        assertEquals("ab", lb.toString());
    }

    // ------------------------------------------------------------------
    // M5: addCollection(String, Collection) — 3 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M5-TP1: ENTRY -> N0 -(B1:true)-> N6 -> EXIT (sb == null)
    public void testM5_TP1_AddCollectionWhenNullBuffer() {
        LogBuilder lb = new LogBuilder(-1);
        lb.addCollection(",", Arrays.asList("a", "b"));
        assertEquals("", lb.toString());
    }

    // M5-TP2: ENTRY -> N0 -(B1:false)-> N1 -> N2 -(B2:true)-> N4 -(B3:true)-> N6 -> EXIT (sb != null, c.isEmpty())
    public void testM5_TP2_AddCollectionEmpty() {
        LogBuilder lb = new LogBuilder(16);
        lb.addCollection(",", Collections.<String>emptyList());
        assertEquals("", lb.toString());
    }

    // M5-TP3: ENTRY -> N0 -(B1:false)-> N1 -> N2 -(B2:false)-> N3 -> N2 -(B2:true)-> N4 -(B3:false)-> N5 -> N6 -> EXIT (sb != null, !c.isEmpty())
    public void testM5_TP3_AddCollectionNonEmpty() {
        LogBuilder lb = new LogBuilder(16);
        lb.addCollection(",", Arrays.asList("a", "b"));
        assertEquals("a,b", lb.toString());
    }

    // ------------------------------------------------------------------
    // M6: addStackTrace() — 2 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M6-TP1: ENTRY -> N0 -(B1:true)-> N2 -> EXIT (sb == null)
    public void testM6_TP1_AddStackTraceWhenNullBuffer() {
        LogBuilder lb = new LogBuilder(0);
        lb.addStackTrace();
        assertEquals(0, lb.size());
    }

    // M6-TP2: ENTRY -> N0 -(B1:false)-> N1 -> N2 -> EXIT (sb != null)
    public void testM6_TP2_AddStackTraceWhenActiveBuffer() {
        LogBuilder lb = new LogBuilder(128);
        lb.addStackTrace();
        assertTrue(lb.size() > 0);
        assertTrue(lb.toString().contains("testM6_TP2_AddStackTraceWhenActiveBuffer"));
    }

    // ------------------------------------------------------------------
    // M7: truncate(int) — 3 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M7-TP1: ENTRY -> N0 -(B1:true)-> N3 -> EXIT (sb == null)
    public void testM7_TP1_TruncateWhenNullBuffer() {
        LogBuilder lb = new LogBuilder(0);
        lb.truncate(2);
        assertEquals(0, lb.size());
    }

    // M7-TP2: ENTRY -> N0 -(B1:false)-> N1 -(B2:true)-> N3 -> EXIT (sb != null, sb.length() <= size)
    public void testM7_TP2_TruncateWhenLengthWithinSize() {
        LogBuilder lb = new LogBuilder(16);
        lb.add("abc");
        lb.truncate(5);
        assertEquals("abc", lb.toString());
    }

    // M7-TP3: ENTRY -> N0 -(B1:false)-> N1 -(B2:false)-> N2 -> N3 -> EXIT (sb != null, sb.length() > size)
    public void testM7_TP3_TruncateWhenLengthExceedsSize() {
        LogBuilder lb = new LogBuilder(16);
        lb.add("abcdef");
        lb.truncate(3);
        assertEquals("abc", lb.toString());
    }

    // ------------------------------------------------------------------
    // M8: mark() — 2 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M8-TP1: ENTRY -> N0 -(B1:true)-> N2 -> EXIT (sb == null)
    public void testM8_TP1_MarkWhenNullBuffer() {
        LogBuilder lb = new LogBuilder(0);
        lb.mark();
        assertFalse(lb.grew("prefix:"));
    }

    // M8-TP2: ENTRY -> N0 -(B1:false)-> N1 -> N2 -> EXIT (sb != null)
    public void testM8_TP2_MarkWhenActiveBuffer() {
        LogBuilder lb = new LogBuilder(16);
        lb.add("start:");
        lb.mark();
        lb.add("end");
        assertTrue(lb.grew("[", "]"));
        assertEquals("start:[]end", lb.toString());
    }

    // ------------------------------------------------------------------
    // M9: grew(Object...) — 3 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M9-TP1: ENTRY -> N0 -(B1:false)-> N1 -> EXIT (sb == null)
    public void testM9_TP1_GrewWhenNullBuffer() {
        LogBuilder lb = new LogBuilder(0);
        assertFalse(lb.grew("x"));
    }

    // M9-TP2: ENTRY -> N0 -(B1:true)-> N2 -(B2:false)-> N3 -> EXIT (sb != null, sb.length() <= p)
    public void testM9_TP2_GrewWhenNotGrown() {
        LogBuilder lb = new LogBuilder(16);
        lb.add("base");
        lb.mark();
        assertFalse(lb.grew("prefix:"));
        assertEquals("base", lb.toString());
    }

    // M9-TP3: ENTRY -> N0 -(B1:true)-> N2 -(B2:true)-> N4 -> EXIT (sb != null, sb.length() > p)
    public void testM9_TP3_GrewWhenGrown() {
        LogBuilder lb = new LogBuilder(16);
        lb.add("base:");
        lb.mark();
        lb.add("added");
        assertTrue(lb.grew("prefix="));
        assertEquals("base:prefix=added", lb.toString());
    }

    // ------------------------------------------------------------------
    // M10: shrink(String) — 5 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M10-TP1: ENTRY -> N0 -(B1:true)-> N5 -> EXIT (sb == null)
    public void testM10_TP1_ShrinkWhenNullBuffer() {
        LogBuilder lb = new LogBuilder(0);
        lb.shrink(",");
        assertEquals("", lb.toString());
    }

    // M10-TP2: ENTRY -> N0 -(B1:false)-> N1 -(B2:true)-> N5 -> EXIT (sb != null, delim == null)
    public void testM10_TP2_ShrinkWhenNullDelim() {
        LogBuilder lb = new LogBuilder(16);
        lb.add("abc");
        lb.shrink(null);
        assertEquals("abc", lb.toString());
    }

    // M10-TP3: ENTRY -> N0 -(B1:false)-> N1 -(B2:false)-> N2 -(B3:true)-> N5 -> EXIT (idx < 0)
    public void testM10_TP3_ShrinkWhenDelimLongerThanBuffer() {
        LogBuilder lb = new LogBuilder(16);
        lb.add("a");
        lb.shrink("---");
        assertEquals("a", lb.toString());
    }

    // M10-TP4: ENTRY -> N0 -(B1:false)-> N1 -(B2:false)-> N2 -(B3:false)-> N3 -(B4:true)-> N5 -> EXIT (idx >= 0, suffix != delim)
    public void testM10_TP4_ShrinkWhenSuffixDoesNotMatch() {
        LogBuilder lb = new LogBuilder(16);
        lb.add("abc");
        lb.shrink("xy");
        assertEquals("abc", lb.toString());
    }

    // M10-TP5: ENTRY -> N0 -(B1:false)-> N1 -(B2:false)-> N2 -(B3:false)-> N3 -(B4:false)-> N4 -> N5 -> EXIT (idx >= 0, suffix == delim)
    public void testM10_TP5_ShrinkWhenSuffixMatches() {
        LogBuilder lb = new LogBuilder(16);
        lb.add("abc,");
        lb.shrink(",");
        assertEquals("abc", lb.toString());
    }

    // ------------------------------------------------------------------
    // M11: log(Logger, Level) — 5 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M11-TP1: ENTRY -> N0 -(B1:true)-> N5 -> EXIT (sb == null)
    public void testM11_TP1_LogWhenNullBuffer() {
        Logger logger = Logger.getLogger("LogBuilderCfgTest.TP1");
        LogBuilder lb = new LogBuilder(0);
        lb.log(logger, Level.INFO);
        assertEquals(0, lb.size());
    }

    // M11-TP2: ENTRY -> N0 -(B1:false)-> N1 -(B2:true)-> N5 -> EXIT (sb != null, logger == null)
    public void testM11_TP2_LogWhenNullLogger() {
        LogBuilder lb = new LogBuilder(16);
        lb.add("msg");
        lb.log(null, Level.INFO);
        assertEquals("msg", lb.toString());
    }

    // M11-TP3: ENTRY -> N0 -(B1:false)-> N1 -(B2:false)-> N2 -(B3:true)-> N5 -> EXIT (sb != null, logger != null, level == null)
    public void testM11_TP3_LogWhenNullLevel() {
        Logger logger = Logger.getLogger("LogBuilderCfgTest.TP3");
        LogBuilder lb = new LogBuilder(16);
        lb.add("msg");
        lb.log(logger, null);
        assertEquals("msg", lb.toString());
    }

    // M11-TP4: ENTRY -> N0 -(B1:false)-> N1 -(B2:false)-> N2 -(B3:false)-> N3 -(B4:true)-> N5 -> EXIT (!logger.isLoggable(level))
    public void testM11_TP4_LogWhenNotLoggable() {
        Logger logger = Logger.getLogger("LogBuilderCfgTest.TP4");
        logger.setUseParentHandlers(false);
        logger.setLevel(Level.SEVERE);
        CapturingHandler handler = new CapturingHandler();
        logger.addHandler(handler);
        try {
            LogBuilder lb = new LogBuilder(16);
            lb.add("suppressed");
            lb.log(logger, Level.FINE);
            assertEquals(0, handler.records.size());
        } finally {
            logger.removeHandler(handler);
        }
    }

    // M11-TP5: ENTRY -> N0 -(B1:false)-> N1 -(B2:false)-> N2 -(B3:false)-> N3 -(B4:false)-> N4 -> N5 -> EXIT (logger.isLoggable(level))
    public void testM11_TP5_LogWhenLoggable() {
        Logger logger = Logger.getLogger("LogBuilderCfgTest.TP5");
        logger.setUseParentHandlers(false);
        logger.setLevel(Level.ALL);
        CapturingHandler handler = new CapturingHandler();
        logger.addHandler(handler);
        try {
            LogBuilder lb = new LogBuilder(16);
            lb.add("logged");
            lb.log(logger, Level.INFO);
            assertEquals(1, handler.records.size());
            assertEquals("logged", handler.records.get(0).getMessage());
            assertEquals(Level.INFO, handler.records.get(0).getLevel());
        } finally {
            logger.removeHandler(handler);
        }
    }

    // ------------------------------------------------------------------
    // M12: size() — 2 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M12-TP1: ENTRY -> N0 -(B1:true)-> N2 -> N3 -> EXIT (sb != null)
    public void testM12_TP1_SizeWhenActiveBuffer() {
        LogBuilder lb = new LogBuilder(16);
        lb.add("abcd");
        assertEquals(4, lb.size());
    }

    // M12-TP2: ENTRY -> N0 -(B1:false)-> N1 -> N3 -> EXIT (sb == null)
    public void testM12_TP2_SizeWhenNullBuffer() {
        LogBuilder lb = new LogBuilder(0);
        assertEquals(0, lb.size());
    }

    // ------------------------------------------------------------------
    // M13: wide(int, Object...) — 5 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M13-TP1: ENTRY -> N0 -(B1:false)-> N1 -> EXIT (size == 0)
    public void testM13_TP1_WideZeroSize() {
        assertEquals("", LogBuilder.wide(0, "abc"));
    }

    // M13-TP2: ENTRY -> N0 -(B1:true)-> N2 -(B2:false)-> N3 -> N5 -(B3:true)-> N7 -(B4:false)-> N8 -(B5:false)-> N9 -> N8 -(B5:true)-> N12 -(B7:true)-> N14 -> EXIT (size > 0, padded right)
    public void testM13_TP2_WideLeftAlignedPadded() {
        assertEquals("ab   ", LogBuilder.wide(5, "ab"));
    }

    // M13-TP3: ENTRY -> N0 -(B1:true)-> N2 -(B2:false)-> N3 -> N5 -(B3:true)-> N7 -(B4:false)-> N8 -(B5:true)-> N12 -(B7:false)-> N13 -> N14 -> EXIT (size > 0, truncated)
    public void testM13_TP3_WideLeftAlignedTruncated() {
        assertEquals("abc", LogBuilder.wide(3, "abcdef"));
    }

    // M13-TP4: ENTRY -> N0 -(B1:true)-> N2 -(B2:true)-> N4 -> N5 -(B3:false)-> N6 -> N7 -(B4:true)-> N10 -(B6:false)-> N11 -> N10 -(B6:true)-> N12 -(B7:true)-> N14 -> EXIT (size < 0, padded left)
    public void testM13_TP4_WideRightAlignedPadded() {
        assertEquals("   ab", LogBuilder.wide(-5, "ab"));
    }

    // M13-TP5: ENTRY -> N0 -(B1:true)-> N2 -(B2:true)-> N4 -> N5 -(B3:false)-> N6 -> N7 -(B4:true)-> N10 -(B6:true)-> N12 -(B7:false)-> N13 -> N14 -> EXIT (size < 0, truncated)
    public void testM13_TP5_WideRightAlignedTruncated() {
        assertEquals("abc", LogBuilder.wide(-3, "abcdef"));
    }

    // ------------------------------------------------------------------
    // M14: toString() — 2 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M14-TP1: ENTRY -> N0 -(B1:true)-> N2 -> N3 -> EXIT (sb != null)
    public void testM14_TP1_ToStringWhenActiveBuffer() {
        LogBuilder lb = new LogBuilder(16);
        lb.add("text");
        assertEquals("text", lb.toString());
    }

    // M14-TP2: ENTRY -> N0 -(B1:false)-> N1 -> N3 -> EXIT (sb == null)
    public void testM14_TP2_ToStringWhenNullBuffer() {
        LogBuilder lb = new LogBuilder(0);
        assertEquals("", lb.toString());
    }
}
