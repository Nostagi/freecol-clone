
package net.sf.freecol.util.test;

import static org.junit.Assert.*;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;

import net.sf.freecol.common.util.CachingFunction;

public class CachingFunctionCFTTest {

    @Test
    public void testCF01ConstructorAndFirstApplication() {
        AtomicInteger count = new AtomicInteger(0);

        CachingFunction<String, String> function =
            new CachingFunction<>(s -> {
                count.incrementAndGet();
                return s.toUpperCase();
            });

        assertNotNull(function);
        assertEquals("HELLO", function.apply("hello"));
        assertEquals(1, count.get());
    }

    @Test
    public void testCF02ReuseCachedResult() {
        AtomicInteger count = new AtomicInteger(0);

        CachingFunction<String, Integer> function =
            new CachingFunction<>(s -> count.incrementAndGet());

        assertEquals(Integer.valueOf(1), function.apply("A"));
        assertEquals(Integer.valueOf(1), function.apply("A"));
        assertEquals(Integer.valueOf(1), function.apply("A"));
        assertEquals(1, count.get());
    }

    @Test
    public void testCF03DifferentInputs() {
        AtomicInteger count = new AtomicInteger(0);

        CachingFunction<String, String> function =
            new CachingFunction<>(s -> {
                count.incrementAndGet();
                return s + "!";
            });

        assertEquals("A!", function.apply("A"));
        assertEquals("B!", function.apply("B"));
        assertEquals("C!", function.apply("C"));
        assertEquals(3, count.get());
    }

    @Test
    public void testCF04CacheNullResult() {
        AtomicInteger count = new AtomicInteger(0);

        CachingFunction<String, String> function =
            new CachingFunction<>(s -> {
                count.incrementAndGet();
                return null;
            });

        assertNull(function.apply("A"));
        assertNull(function.apply("A"));
        assertEquals(1, count.get());
    }

    @Test
    public void testCF05NullInput() {
        AtomicInteger count = new AtomicInteger(0);

        CachingFunction<String, String> function =
            new CachingFunction<>(s -> {
                count.incrementAndGet();
                return "NULL";
            });

        assertEquals("NULL", function.apply(null));
        assertEquals("NULL", function.apply(null));
        assertEquals(1, count.get());
    }

    @Test
    public void testCF06EqualButDifferentObjects() {
        AtomicInteger count = new AtomicInteger(0);

        CachingFunction<String, Integer> function =
            new CachingFunction<>(s -> count.incrementAndGet());

        String first = new String("ABC");
        String second = new String("ABC");

        assertNotSame(first, second);
        assertEquals(Integer.valueOf(1), function.apply(first));
        assertEquals(Integer.valueOf(1), function.apply(second));
        assertEquals(1, count.get());
    }

    @Test
    public void testCF07DifferentKeysWithSameHashCode() {
        AtomicInteger count = new AtomicInteger(0);

        CachingFunction<String, Integer> function =
            new CachingFunction<>(s -> count.incrementAndGet());

        String first = "FB";
        String second = "Ea";

        assertEquals(first.hashCode(), second.hashCode());
        assertNotEquals(first, second);

        assertEquals(Integer.valueOf(1), function.apply(first));
        assertEquals(Integer.valueOf(2), function.apply(second));
        assertEquals(Integer.valueOf(1), function.apply(first));
        assertEquals(Integer.valueOf(2), function.apply(second));
        assertEquals(2, count.get());
    }

    @Test
    public void testCF08ExceptionIsNotCached() {
        AtomicInteger count = new AtomicInteger(0);

        CachingFunction<String, String> function =
            new CachingFunction<>(s -> {
                if (count.incrementAndGet() == 1) {
                    throw new IllegalArgumentException("Test exception");
                }
                return "SUCCESS";
            });

        try {
            function.apply("A");
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            assertEquals("Test exception", e.getMessage());
        }

        assertEquals("SUCCESS", function.apply("A"));
        assertEquals("SUCCESS", function.apply("A"));
        assertEquals(2, count.get());
    }

    @Test
    public void testCF09MultipleCachedInputs() {
        AtomicInteger count = new AtomicInteger(0);

        CachingFunction<Integer, Integer> function =
            new CachingFunction<>(n -> {
                count.incrementAndGet();
                return n * n;
            });

        assertEquals(Integer.valueOf(4), function.apply(2));
        assertEquals(Integer.valueOf(9), function.apply(3));
        assertEquals(Integer.valueOf(16), function.apply(4));
        assertEquals(Integer.valueOf(4), function.apply(2));
        assertEquals(Integer.valueOf(9), function.apply(3));
        assertEquals(Integer.valueOf(16), function.apply(4));
        assertEquals(3, count.get());
    }

    @Test
    public void testCF10SameInputAfterDifferentInputs() {
        AtomicInteger count = new AtomicInteger(0);

        CachingFunction<String, String> function =
            new CachingFunction<>(s -> {
                count.incrementAndGet();
                return s.toLowerCase();
            });

        assertEquals("a", function.apply("A"));
        assertEquals("b", function.apply("B"));
        assertEquals("a", function.apply("A"));
        assertEquals("c", function.apply("C"));
        assertEquals("b", function.apply("B"));
        assertEquals(3, count.get());
    }

    @Test
    public void testCF11FunctionReturnsConstantValue() {
        AtomicInteger count = new AtomicInteger(0);

        CachingFunction<String, String> function =
            new CachingFunction<>(s -> {
                count.incrementAndGet();
                return "CONSTANT";
            });

        assertEquals("CONSTANT", function.apply("A"));
        assertEquals("CONSTANT", function.apply("B"));
        assertEquals("CONSTANT", function.apply("A"));
        assertEquals(2, count.get());
    }

    @Test
    public void testCF12FunctionReturnsZero() {
        AtomicInteger count = new AtomicInteger(0);

        CachingFunction<Integer, Integer> function =
            new CachingFunction<>(n -> {
                count.incrementAndGet();
                return 0;
            });

        assertEquals(Integer.valueOf(0), function.apply(1));
        assertEquals(Integer.valueOf(0), function.apply(1));
        assertEquals(1, count.get());
    }
}
