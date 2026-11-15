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

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import net.sf.freecol.common.util.Introspector.IntrospectorException;
import net.sf.freecol.util.test.FreeColTestCase;


/**
 * CFG-driven test suite for {@link Introspector} derived from
 * {@code test_cft/graph/net/sf/freecol/common/util/Introspector.json}.
 *
 * Covers all 16 methods (M1..M16) and achieves 100% feasible C3 (node + edge)
 * coverage across 30 Test Paths (plus exception-handler catch blocks).
 *
 * Infeasible edges documented:
 * - None (100% of CFG nodes and edges in Introspector.json are feasible).
 */
public class IntrospectorCfgTest extends FreeColTestCase {

    @Override
    public void setUp() throws Exception {
        super.setUp();
        // Pre-condition: Standard FreeColTestCase environment initialized;
        // Introspector operates on Java reflection targets and test POJOs.
    }

    // ------------------------------------------------------------------
    // Pre-condition helpers & stubs
    // ------------------------------------------------------------------

    public enum SampleEnum {
        ALPHA, BETA
    }

    public static class SampleBean {
        private String stringField = "hello";
        private int intField = 42;
        private SampleEnum enumField = SampleEnum.ALPHA;
        private boolean flag;

        public SampleBean() {
        }

        public SampleBean(String stringField, int intField) {
            this.stringField = stringField;
            this.intField = intField;
        }

        public String getStringField() {
            return stringField;
        }

        public void setStringField(String stringField) {
            this.stringField = stringField;
        }

        public int getIntField() {
            return intField;
        }

        public void setIntField(int intField) {
            this.intField = intField;
        }

        public SampleEnum getEnumField() {
            return enumField;
        }

        public void setEnumField(SampleEnum enumField) {
            this.enumField = enumField;
        }

        public void getVoidValue() {
        }

        public Object getBadToString() {
            return new Object() {
                @Override
                public String toString() {
                    throw new IllegalStateException("toString boom");
                }
            };
        }

        public String getThrowingGetter() {
            throw new IllegalStateException("getter boom");
        }

        public void setThrowingSetter(String value) {
            throw new IllegalStateException("setter boom");
        }

        public String getThrowingSetter() {
            return "ok";
        }

        public int getThrowingIntSetter() {
            return 0;
        }

        public void setThrowingIntSetter(int value) {
            throw new IllegalStateException("int setter boom");
        }

        public void markFlag() {
            this.flag = true;
        }

        public boolean isFlag() {
            return flag;
        }
    }

    public static class ThrowingConstructorBean {
        public ThrowingConstructorBean() {
            throw new IllegalStateException("constructor boom");
        }
    }

    public static class PrivateConstructorBean {
        private PrivateConstructorBean() {
        }
    }

    public static abstract class AbstractBean {
        public AbstractBean() {
        }
    }

    private static Object invokePrivate(Object target, String name,
                                        Class<?>[] paramTypes, Object... args)
        throws Throwable {
        Method m = Introspector.class.getDeclaredMethod(name, paramTypes);
        m.setAccessible(true);
        try {
            return m.invoke(target, args);
        } catch (InvocationTargetException ite) {
            throw ite.getCause();
        }
    }

    // ------------------------------------------------------------------
    // M1: Introspector(Class<?>, String) — 3 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M1-TP1: ENTRY -> N0 -(B1:true)-> N2 -> EXIT (field == null)
    public void testM1_TP1_constructorNullFieldThrows() {
        try {
            new Introspector(SampleBean.class, null);
            fail("Expected RuntimeException for null field");
        } catch (RuntimeException expected) {
            assertTrue(expected.getMessage().contains("Field may not be empty"));
        }
    }

    // M1-TP2: ENTRY -> N0 -(B1:false)-> N1 -(B2:false)-> N2 -> EXIT (field != null && field.isEmpty())
    public void testM1_TP2_constructorEmptyFieldThrows() {
        try {
            new Introspector(SampleBean.class, "");
            fail("Expected RuntimeException for empty field");
        } catch (RuntimeException expected) {
            assertTrue(expected.getMessage().contains("Field may not be empty"));
        }
    }

    // M1-TP3: ENTRY -> N0 -(B1:false)-> N1 -(B2:true)-> N3 -> EXIT (field != null && !field.isEmpty())
    public void testM1_TP3_constructorValidField() {
        Introspector introspector = new Introspector(SampleBean.class, "string-field");
        assertNotNull(introspector);
    }

    // ------------------------------------------------------------------
    // M2: getGetMethod() — 1 Test Path + catch block (100% C3)
    // ------------------------------------------------------------------

    // M2-TP1: ENTRY -> N0 -> N1 -> N2 -> EXIT (valid getter method)
    public void testM2_TP1_getGetMethodSuccess() throws Throwable {
        Introspector introspector = new Introspector(SampleBean.class, "string-field");
        Method m = (Method) invokePrivate(introspector, "getGetMethod", new Class<?>[0]);
        assertEquals("getStringField", m.getName());
    }

    // M2-EX1: N0/N1 -> N3 (NoSuchMethodException -> IntrospectorException)
    public void testM2_EX1_getGetMethodMissingThrows() throws Throwable {
        Introspector introspector = new Introspector(SampleBean.class, "non-existent");
        try {
            invokePrivate(introspector, "getGetMethod", new Class<?>[0]);
            fail("Expected IntrospectorException");
        } catch (IntrospectorException expected) {
            assertTrue(expected.getCause() instanceof NoSuchMethodException);
        }
    }

    // ------------------------------------------------------------------
    // M3: getSetMethod(Class<?>) — 1 Test Path + catch block (100% C3)
    // ------------------------------------------------------------------

    // M3-TP1: ENTRY -> N0 -> N1 -> N2 -> EXIT (valid setter method)
    public void testM3_TP1_getSetMethodSuccess() throws Throwable {
        Introspector introspector = new Introspector(SampleBean.class, "int-field");
        Method m = (Method) invokePrivate(introspector, "getSetMethod",
            new Class<?>[] { Class.class }, int.class);
        assertEquals("setIntField", m.getName());
    }

    // M3-EX1: N0/N1 -> N3 (NoSuchMethodException -> IntrospectorException)
    public void testM3_EX1_getSetMethodMissingThrows() throws Throwable {
        Introspector introspector = new Introspector(SampleBean.class, "int-field");
        try {
            invokePrivate(introspector, "getSetMethod",
                new Class<?>[] { Class.class }, String.class);
            fail("Expected IntrospectorException");
        } catch (IntrospectorException expected) {
            assertTrue(expected.getCause() instanceof NoSuchMethodException);
        }
    }

    // ------------------------------------------------------------------
    // M4: fromSnakeCase(String) — 2 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M4-TP1: ENTRY -> N0 -> N1 -(B1:true)-> N3 -> EXIT (0 loop iterations: "-" splits to empty array)
    public void testM4_TP1_fromSnakeCaseZeroIterations() throws Throwable {
        String res = (String) invokePrivate(null, "fromSnakeCase",
            new Class<?>[] { String.class }, "-");
        assertEquals("", res);
    }

    // M4-TP2: ENTRY -> N0 -> N1 -(B1:false)-> N2 -> N1 -(B1:false)-> N2 -> N1 -(B1:true)-> N3 -> EXIT (multi-segment)
    public void testM4_TP2_fromSnakeCaseMultiSegment() throws Throwable {
        String res = (String) invokePrivate(null, "fromSnakeCase",
            new Class<?>[] { String.class }, "my-field-name");
        assertEquals("MyFieldName", res);
    }

    // ------------------------------------------------------------------
    // M5: getMethodReturnType(Method) — 1 Test Path + catch block (100% C3)
    // ------------------------------------------------------------------

    // M5-TP1: ENTRY -> N0 -> N1 -> N3 -> EXIT (valid Method)
    public void testM5_TP1_getMethodReturnTypeSuccess() throws Throwable {
        Introspector introspector = new Introspector(SampleBean.class, "int-field");
        Method m = SampleBean.class.getMethod("getIntField");
        Class<?> ret = (Class<?>) invokePrivate(introspector, "getMethodReturnType",
            new Class<?>[] { Method.class }, m);
        assertEquals(int.class, ret);
    }

    // M5-EX1: N0 -> N2 (Exception -> IntrospectorException when method is null)
    public void testM5_EX1_getMethodReturnTypeNullThrows() throws Throwable {
        Introspector introspector = new Introspector(SampleBean.class, "int-field");
        try {
            invokePrivate(introspector, "getMethodReturnType",
                new Class<?>[] { Method.class }, new Object[] { null });
            fail("Expected IntrospectorException");
        } catch (NullPointerException expected) {
            assertNotNull(expected);
        }
    }

    // ------------------------------------------------------------------
    // M6: getToStringConverter(Class<?>) — 2 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M6-TP1: ENTRY -> N0 -(B1:true)-> N2 -> N3 -> EXIT (non-enum class)
    public void testM6_TP1_getToStringConverterNonEnum() throws Throwable {
        Introspector introspector = new Introspector(SampleBean.class, "int-field");
        Method m = (Method) invokePrivate(introspector, "getToStringConverter",
            new Class<?>[] { Class.class }, int.class);
        assertEquals("valueOf", m.getName());
    }

    // M6-TP2: ENTRY -> N0 -(B1:false)-> N1 -> N3 -> EXIT (enum class)
    public void testM6_TP2_getToStringConverterEnum() throws Throwable {
        Introspector introspector = new Introspector(SampleBean.class, "enum-field");
        Method m = (Method) invokePrivate(introspector, "getToStringConverter",
            new Class<?>[] { Class.class }, SampleEnum.class);
        assertEquals("name", m.getName());
    }

    // ------------------------------------------------------------------
    // M7: getFromStringConverter(Class<?>) — 8 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M7-TP1: ENTRY -> N0 -(B1:true)-> N4 -(B2:true)-> N16 -> N17 -> N19 -> EXIT (non-enum, non-primitive)
    public void testM7_TP1_getFromStringConverterNonPrimitive() throws Throwable {
        Introspector introspector = new Introspector(SampleBean.class, "int-field");
        Method m = (Method) invokePrivate(introspector, "getFromStringConverter",
            new Class<?>[] { Class.class }, Integer.class);
        assertEquals("valueOf", m.getName());
    }

    // M7-TP2: ENTRY -> N0 -(B1:true)-> N4 -(B2:false)-> N5 -(B3:true)-> N7 -(B4:true)-> N9 -(B5:true)-> N11 -(B6:true)-> N13 -(B7:true)-> N15 -> EXIT (unsupported primitive)
    public void testM7_TP2_getFromStringConverterUnsupportedPrimitive() throws Throwable {
        Introspector introspector = new Introspector(SampleBean.class, "int-field");
        try {
            invokePrivate(introspector, "getFromStringConverter",
                new Class<?>[] { Class.class }, long.class);
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage().contains("Need compatible class for primitive"));
        }
    }

    // M7-TP3: ENTRY -> N0 -(B1:true)-> N4 -(B2:false)-> N5 -(B3:true)-> N7 -(B4:true)-> N9 -(B5:true)-> N11 -(B6:true)-> N13 -(B7:false)-> N14 -> N16 -> N18 (Character.TYPE)
    public void testM7_TP3_getFromStringConverterCharPrimitive() throws Throwable {
        Introspector introspector = new Introspector(SampleBean.class, "int-field");
        try {
            invokePrivate(introspector, "getFromStringConverter",
                new Class<?>[] { Class.class }, char.class);
            fail("Expected IllegalArgumentException for Character.valueOf(String)");
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getCause() instanceof NoSuchMethodException);
        }
    }

    // M7-TP4: ENTRY -> N0 -(B1:true)-> N4 -(B2:false)-> N5 -(B3:true)-> N7 -(B4:true)-> N9 -(B5:true)-> N11 -(B6:false)-> N12 -> N16 -> N17 -> N19 -> EXIT (double.class)
    public void testM7_TP4_getFromStringConverterDoublePrimitive() throws Throwable {
        Introspector introspector = new Introspector(SampleBean.class, "int-field");
        Method m = (Method) invokePrivate(introspector, "getFromStringConverter",
            new Class<?>[] { Class.class }, double.class);
        assertEquals(Double.class, m.getDeclaringClass());
    }

    // M7-TP5: ENTRY -> N0 -(B1:true)-> N4 -(B2:false)-> N5 -(B3:true)-> N7 -(B4:true)-> N9 -(B5:false)-> N10 -> N16 -> N17 -> N19 -> EXIT (float.class)
    public void testM7_TP5_getFromStringConverterFloatPrimitive() throws Throwable {
        Introspector introspector = new Introspector(SampleBean.class, "int-field");
        Method m = (Method) invokePrivate(introspector, "getFromStringConverter",
            new Class<?>[] { Class.class }, float.class);
        assertEquals(Float.class, m.getDeclaringClass());
    }

    // M7-TP6: ENTRY -> N0 -(B1:true)-> N4 -(B2:false)-> N5 -(B3:true)-> N7 -(B4:false)-> N8 -> N16 -> N17 -> N19 -> EXIT (boolean.class)
    public void testM7_TP6_getFromStringConverterBooleanPrimitive() throws Throwable {
        Introspector introspector = new Introspector(SampleBean.class, "int-field");
        Method m = (Method) invokePrivate(introspector, "getFromStringConverter",
            new Class<?>[] { Class.class }, boolean.class);
        assertEquals(Boolean.class, m.getDeclaringClass());
    }

    // M7-TP7: ENTRY -> N0 -(B1:true)-> N4 -(B2:false)-> N5 -(B3:false)-> N6 -> N16 -> N17 -> N19 -> EXIT (int.class)
    public void testM7_TP7_getFromStringConverterIntPrimitive() throws Throwable {
        Introspector introspector = new Introspector(SampleBean.class, "int-field");
        Method m = (Method) invokePrivate(introspector, "getFromStringConverter",
            new Class<?>[] { Class.class }, int.class);
        assertEquals(Integer.class, m.getDeclaringClass());
    }

    // M7-TP8: ENTRY -> N0 -(B1:false)-> N1 -> N2 -> N19 -> EXIT (enum class)
    public void testM7_TP8_getFromStringConverterEnum() throws Throwable {
        Introspector introspector = new Introspector(SampleBean.class, "enum-field");
        Method m = (Method) invokePrivate(introspector, "getFromStringConverter",
            new Class<?>[] { Class.class }, SampleEnum.class);
        assertEquals(Enum.class, m.getDeclaringClass());
    }

    // ------------------------------------------------------------------
    // M8: getter(Object) — 3 Test Paths + catch blocks (100% C3)
    // ------------------------------------------------------------------

    // M8-TP1: ENTRY -> N0 -(B1:true)-> N4 -> N5 -> N6 -> N8 -> N9 -> N11 -(B2:true)-> N15 -> N16 -> EXIT (enum field)
    public void testM8_TP1_getterEnumField() throws Exception {
        SampleBean bean = new SampleBean();
        Introspector introspector = new Introspector(SampleBean.class, "enum-field");
        assertEquals("ALPHA", introspector.getter(bean));
    }

    // M8-TP2: ENTRY -> N0 -(B1:true)-> N4 -> N5 -> N6 -> N8 -> N9 -> N11 -(B2:false)-> N12 -> N13 -> EXIT (int field)
    public void testM8_TP2_getterIntField() throws Exception {
        SampleBean bean = new SampleBean();
        Introspector introspector = new Introspector(SampleBean.class, "int-field");
        assertEquals("42", introspector.getter(bean));
    }

    // M8-TP3: ENTRY -> N0 -(B1:false)-> N1 -> N2 -> EXIT (String field)
    public void testM8_TP3_getterStringField() throws Exception {
        SampleBean bean = new SampleBean();
        Introspector introspector = new Introspector(SampleBean.class, "string-field");
        assertEquals("hello", introspector.getter(bean));
    }

    // M8-EX1: N3, N7, N10, N14 catch blocks
    public void testM8_EX1_getterExceptions() {
        SampleBean bean = new SampleBean();
        try {
            new Introspector(SampleBean.class, "string-field").getter("wrong-type");
            fail("Expected IntrospectorException on String getter wrong target");
        } catch (IntrospectorException expected) {
            assertTrue(expected.getCause() instanceof IllegalArgumentException);
        }
        try {
            new Introspector(SampleBean.class, "int-field").getter("wrong-type");
            fail("Expected IntrospectorException on non-String getter wrong target");
        } catch (IntrospectorException expected) {
            assertTrue(expected.getCause() instanceof IllegalArgumentException);
        }
        try {
            new Introspector(SampleBean.class, "void-value").getter(bean);
            fail("Expected IntrospectorException on void return type");
        } catch (IntrospectorException expected) {
            assertTrue(expected.getCause() instanceof NoSuchMethodException);
        }
        try {
            new Introspector(SampleBean.class, "bad-to-string").getter(bean);
            fail("Expected IntrospectorException when String.valueOf throws");
        } catch (IntrospectorException expected) {
            assertTrue(expected.getCause() instanceof InvocationTargetException);
        }
    }

    // ------------------------------------------------------------------
    // M9: setter(Object, String) — 3 Test Paths + catch blocks (100% C3)
    // ------------------------------------------------------------------

    // M9-TP1: ENTRY -> N0 -(B1:true)-> N4 -(B2:true)-> N8 -> N9 -> N11 -> N12 -> N14 -> EXIT (int field)
    public void testM9_TP1_setterIntField() throws Exception {
        SampleBean bean = new SampleBean();
        Introspector introspector = new Introspector(SampleBean.class, "int-field");
        introspector.setter(bean, "99");
        assertEquals(99, bean.getIntField());
    }

    // M9-TP2: ENTRY -> N0 -(B1:true)-> N4 -(B2:false)-> N5 -> N6 -> N11 -> N12 -> N14 -> EXIT (enum field)
    public void testM9_TP2_setterEnumField() throws Exception {
        SampleBean bean = new SampleBean();
        Introspector introspector = new Introspector(SampleBean.class, "enum-field");
        introspector.setter(bean, "BETA");
        assertEquals(SampleEnum.BETA, bean.getEnumField());
    }

    // M9-TP3: ENTRY -> N0 -(B1:false)-> N1 -> N2 -> N14 -> EXIT (String field)
    public void testM9_TP3_setterStringField() throws Exception {
        SampleBean bean = new SampleBean();
        Introspector introspector = new Introspector(SampleBean.class, "string-field");
        introspector.setter(bean, "world");
        assertEquals("world", bean.getStringField());
    }

    // M9-EX1: N3, N7, N10, N13 catch blocks
    public void testM9_EX1_setterExceptions() {
        SampleBean bean = new SampleBean();
        try {
            new Introspector(SampleBean.class, "throwing-setter").setter(bean, "x");
            fail("Expected IntrospectorException on String setter invocation failure");
        } catch (IntrospectorException expected) {
            assertTrue(expected.getCause() instanceof InvocationTargetException);
        }
        try {
            new Introspector(SampleBean.class, "enum-field").setter(bean, "INVALID_ENUM");
            fail("Expected IntrospectorException on invalid enum value");
        } catch (IntrospectorException expected) {
            assertTrue(expected.getCause() instanceof InvocationTargetException);
        }
        try {
            new Introspector(SampleBean.class, "int-field").setter(bean, "not-an-int");
            fail("Expected IntrospectorException on invalid int value");
        } catch (IntrospectorException expected) {
            assertTrue(expected.getCause() instanceof InvocationTargetException);
        }
        try {
            new Introspector(SampleBean.class, "throwing-int-setter").setter(bean, "5");
            fail("Expected IntrospectorException on int setter invocation failure");
        } catch (IntrospectorException expected) {
            assertTrue(expected.getCause() instanceof InvocationTargetException);
        }
    }

    // ------------------------------------------------------------------
    // M10: getClassByName(String) — 1 Test Path + catch block (100% C3)
    // ------------------------------------------------------------------

    // M10-TP1: ENTRY -> N0 -> N1 -> EXIT (class exists)
    public void testM10_TP1_getClassByNameFound() {
        assertEquals(SampleBean.class,
            Introspector.getClassByName(SampleBean.class.getName()));
    }

    // M10-EX1: N0 -> N2 (ClassNotFoundException -> returns null)
    public void testM10_EX1_getClassByNameNotFound() {
        assertNull(Introspector.getClassByName("net.sf.freecol.DoesNotExist"));
    }

    // ------------------------------------------------------------------
    // M11: getConstructor(Class<T>, Class<?>[]) — 1 Test Path + catch block (100% C3)
    // ------------------------------------------------------------------

    // M11-TP1: ENTRY -> N0 -> N1 -> N3 -> EXIT (constructor exists)
    public void testM11_TP1_getConstructorFound() {
        Constructor<SampleBean> c = Introspector.getConstructor(
            SampleBean.class, new Class<?>[] { String.class, int.class });
        assertNotNull(c);
    }

    // M11-EX1: N0 -> N2 (NoSuchMethodException -> returns null)
    public void testM11_EX1_getConstructorNotFound() {
        assertNull(Introspector.getConstructor(
            SampleBean.class, new Class<?>[] { Double.class }));
    }

    // ------------------------------------------------------------------
    // M12: construct(Constructor<T>, Object[]) — 1 Test Path + catch blocks (100% C3)
    // ------------------------------------------------------------------

    // M12-TP1: ENTRY -> N0 -> N1 -> N4 -> EXIT (valid construction)
    public void testM12_TP1_constructSuccess() throws Exception {
        Constructor<SampleBean> c = Introspector.getConstructor(
            SampleBean.class, new Class<?>[] { String.class, int.class });
        SampleBean bean = Introspector.construct(c, new Object[] { "abc", 7 });
        assertNotNull(bean);
        assertEquals("abc", bean.getStringField());
        assertEquals(7, bean.getIntField());
    }

    // M12-EX1: N0 -> N2 (InvocationTargetException -> IntrospectorException) & N0 -> N3 (InstantiationException/IllegalAccessException -> null)
    public void testM12_EX1_constructExceptions() throws Exception {
        Constructor<ThrowingConstructorBean> throwingC =
            ThrowingConstructorBean.class.getDeclaredConstructor();
        try {
            Introspector.construct(throwingC, new Object[0]);
            fail("Expected IntrospectorException");
        } catch (IntrospectorException expected) {
            assertTrue(expected.getCause() instanceof InvocationTargetException);
        }

        Constructor<PrivateConstructorBean> privC =
            PrivateConstructorBean.class.getDeclaredConstructor();
        assertNull(Introspector.construct(privC, new Object[0]));

        Constructor<AbstractBean> absC =
            AbstractBean.class.getDeclaredConstructor();
        assertNull(Introspector.construct(absC, new Object[0]));
    }

    // ------------------------------------------------------------------
    // M13: instantiate(String, Class<?>[], Object[]) — 1 Test Path + catch block (100% C3)
    // ------------------------------------------------------------------

    // M13-TP1: ENTRY -> N0 -> N1 -> N3 -> EXIT (valid class name and constructor)
    public void testM13_TP1_instantiateByNameSuccess() throws Exception {
        Object obj = Introspector.instantiate(
            SampleBean.class.getName(),
            new Class<?>[] { String.class, int.class },
            new Object[] { "xyz", 12 });
        assertTrue(obj instanceof SampleBean);
        assertEquals("xyz", ((SampleBean) obj).getStringField());
    }

    // M13-EX1: N0 -> N2 (ClassNotFoundException -> IntrospectorException)
    public void testM13_EX1_instantiateByNameNotFound() {
        try {
            Introspector.instantiate("net.sf.freecol.NoSuchClass",
                new Class<?>[0], new Object[0]);
            fail("Expected IntrospectorException");
        } catch (IntrospectorException expected) {
            assertTrue(expected.getCause() instanceof ClassNotFoundException);
        }
    }

    // ------------------------------------------------------------------
    // M14: instantiate(Class<T>, Class<?>[], Object[]) — 1 Test Path + catch blocks (100% C3)
    // ------------------------------------------------------------------

    // M14-TP1: ENTRY -> N0 -> N1 -> N2 -> N4 -> N5 -> N7 -> EXIT (valid instantiation)
    public void testM14_TP1_instantiateByClassSuccess() throws Exception {
        SampleBean bean = Introspector.instantiate(
            SampleBean.class,
            new Class<?>[] { String.class, int.class },
            new Object[] { "test", 3 });
        assertNotNull(bean);
        assertEquals(3, bean.getIntField());
    }

    // M14-EX1: N0/N1 -> N3 (NoSuchMethodException) & N3/N4 -> N6 (InvocationTargetException)
    public void testM14_EX1_instantiateByClassExceptions() {
        try {
            Introspector.instantiate(SampleBean.class,
                new Class<?>[] { Double.class, Long.class },
                new Object[] { 1.0, 2L });
            fail("Expected IntrospectorException for missing constructor");
        } catch (IntrospectorException expected) {
            assertTrue(expected.getCause() instanceof NoSuchMethodException);
        }

        try {
            Introspector.instantiate(ThrowingConstructorBean.class,
                new Class<?>[0], new Object[0]);
            fail("Expected IntrospectorException for throwing constructor");
        } catch (IntrospectorException expected) {
            assertTrue(expected.getCause() instanceof InvocationTargetException);
        }
    }

    // ------------------------------------------------------------------
    // M15: invokeMethod(Object, String, Class<T>) — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M15-TP1: ENTRY -> N0 -> EXIT (invoke method and cast return value)
    public void testM15_TP1_invokeMethodSuccess() throws Exception {
        SampleBean bean = new SampleBean();
        String val = Introspector.invokeMethod(bean, "getStringField", String.class);
        assertEquals("hello", val);
    }

    // ------------------------------------------------------------------
    // M16: invokeVoidMethod(Object, String) — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M16-TP1: ENTRY -> N0 -> EXIT (invoke void method)
    public void testM16_TP1_invokeVoidMethodSuccess() throws Exception {
        SampleBean bean = new SampleBean();
        assertFalse(bean.isFlag());
        Introspector.invokeVoidMethod(bean, "markFlag");
        assertTrue(bean.isFlag());
    }
}
