
package net.sf.freecol.common.util;

import java.util.concurrent.atomic.AtomicInteger;
import net.sf.freecol.util.test.FreeColTestCase;


/**
 * Control Flow Testing for CachingFunction.
 *
 * Focus:
 * - C2 Branch Coverage
 * - Cache hit and cache miss
 * - Null input
 * - Null cached value
 * - Repeated function calls
 */
public class CachingFunctionCFTTest extends FreeColTestCase {


    public void testCacheMiss() {
        AtomicInteger counter = new AtomicInteger(0);

        CachingFunction<String, Integer> function =
            new CachingFunction<>(key -> {
                counter.incrementAndGet();
                return key.length();
            });

        assertEquals(Integer.valueOf(5), function.apply("Hello"));
        assertEquals(1, counter.get());
    }


    public void testCacheHit() {
        AtomicInteger counter = new AtomicInteger(0);

        CachingFunction<String, Integer> function =
            new CachingFunction<>(key -> {
                counter.incrementAndGet();
                return key.length();
            });

        assertEquals(Integer.valueOf(5), function.apply("Hello"));
        assertEquals(Integer.valueOf(5), function.apply("Hello"));

        assertEquals(1, counter.get());
    }


    public void testDifferentKeys() {
        AtomicInteger counter = new AtomicInteger(0);

        CachingFunction<String, Integer> function =
            new CachingFunction<>(key -> {
                counter.incrementAndGet();
                return key.length();
            });

        assertEquals(Integer.valueOf(3), function.apply("ABC"));
        assertEquals(Integer.valueOf(4), function.apply("ABCD"));
        assertEquals(Integer.valueOf(3), function.apply("ABC"));

        assertEquals(2, counter.get());
    }


    public void testNullResultIsCached() {
        AtomicInteger counter = new AtomicInteger(0);

        CachingFunction<String, String> function =
            new CachingFunction<>(key -> {
                counter.incrementAndGet();
                return null;
            });

        assertNull(function.apply("null-key"));
        assertNull(function.apply("null-key"));

        // The wrapped function must only be executed once.
        assertEquals(1, counter.get());
    }


    public void testNullInput() {
        AtomicInteger counter = new AtomicInteger(0);

        CachingFunction<String, String> function =
            new CachingFunction<>(key -> {
                counter.incrementAndGet();
                return "result";
            });

        assertEquals("result", function.apply(null));
        assertEquals("result", function.apply(null));

        assertEquals(1, counter.get());
    }


    public void testNullInputAndNullResult() {
        AtomicInteger counter = new AtomicInteger(0);

        CachingFunction<String, String> function =
            new CachingFunction<>(key -> {
                counter.incrementAndGet();
                return null;
            });

        assertNull(function.apply(null));
        assertNull(function.apply(null));

        assertEquals(1, counter.get());
    }


    public void testCacheMultipleNullResults() {
        AtomicInteger counter = new AtomicInteger(0);

        CachingFunction<String, String> function =
            new CachingFunction<>(key -> {
                counter.incrementAndGet();
                return null;
            });

        assertNull(function.apply("A"));
        assertNull(function.apply("B"));
        assertNull(function.apply("A"));
        assertNull(function.apply("B"));

        assertEquals(2, counter.get());
    }
}
