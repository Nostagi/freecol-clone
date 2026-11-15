package net.sf.freecol.common.model.pathfinding;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import junit.framework.TestCase;
import net.sf.freecol.common.model.Location;
import net.sf.freecol.common.model.Unit;

public class CostDeciderCfgTest extends TestCase {
    public void testTC001PublicInterface() {
        assertTrue(CostDecider.class.isInterface());
        assertTrue(Modifier.isPublic(CostDecider.class.getModifiers()));
        assertEquals(3, CostDecider.class.getDeclaredMethods().length);
        assertEquals(1, CostDecider.class.getDeclaredFields().length);
    }

    public void testTC002IllegalMoveConstant() throws ReflectiveOperationException {
        Field field = CostDecider.class.getDeclaredField("ILLEGAL_MOVE");
        assertSame(int.class, field.getType());
        assertTrue(Modifier.isPublic(field.getModifiers()));
        assertTrue(Modifier.isStatic(field.getModifiers()));
        assertTrue(Modifier.isFinal(field.getModifiers()));
        assertEquals(-1, field.getInt(null));
    }

    public void testTC003GetCostDeclaration() throws NoSuchMethodException {
        Method method = CostDecider.class.getDeclaredMethod("getCost",
                Unit.class, Location.class, Location.class, int.class);
        assertSame(int.class, method.getReturnType());
        assertEquals(4, method.getParameterTypes().length);
        assertSame(Unit.class, method.getParameterTypes()[0]);
        assertSame(Location.class, method.getParameterTypes()[1]);
        assertSame(Location.class, method.getParameterTypes()[2]);
        assertSame(int.class, method.getParameterTypes()[3]);
        assertTrue(Modifier.isPublic(method.getModifiers()));
        assertTrue(Modifier.isAbstract(method.getModifiers()));
        assertFalse(method.isDefault());
    }

    public void testTC004GetMovesLeftDeclaration() throws NoSuchMethodException {
        Method method = CostDecider.class.getDeclaredMethod("getMovesLeft");
        assertSame(int.class, method.getReturnType());
        assertEquals(0, method.getParameterTypes().length);
        assertTrue(Modifier.isPublic(method.getModifiers()));
        assertTrue(Modifier.isAbstract(method.getModifiers()));
        assertFalse(method.isDefault());
    }

    public void testTC005GetNewTurnsDeclaration() throws NoSuchMethodException {
        Method method = CostDecider.class.getDeclaredMethod("getNewTurns");
        assertSame(int.class, method.getReturnType());
        assertEquals(0, method.getParameterTypes().length);
        assertTrue(Modifier.isPublic(method.getModifiers()));
        assertTrue(Modifier.isAbstract(method.getModifiers()));
        assertFalse(method.isDefault());
    }
}
