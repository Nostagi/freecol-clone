package net.sf.freecol.common.model.pathfinding;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import junit.framework.TestCase;
import net.sf.freecol.common.model.PathNode;
import net.sf.freecol.common.model.Unit;

public class GoalDeciderCfgTest extends TestCase {
    public void testTC001PublicInterface() {
        assertTrue(GoalDecider.class.isInterface());
        assertTrue(Modifier.isPublic(GoalDecider.class.getModifiers()));
        assertEquals(3, GoalDecider.class.getDeclaredMethods().length);
    }

    public void testTC002GetGoalDeclaration() throws NoSuchMethodException {
        Method method = GoalDecider.class.getDeclaredMethod("getGoal");
        assertSame(PathNode.class, method.getReturnType());
        assertEquals(0, method.getParameterTypes().length);
        assertTrue(Modifier.isPublic(method.getModifiers()));
        assertTrue(Modifier.isAbstract(method.getModifiers()));
        assertFalse(method.isDefault());
    }

    public void testTC003HasSubGoalsDeclaration() throws NoSuchMethodException {
        Method method = GoalDecider.class.getDeclaredMethod("hasSubGoals");
        assertSame(boolean.class, method.getReturnType());
        assertEquals(0, method.getParameterTypes().length);
        assertTrue(Modifier.isPublic(method.getModifiers()));
        assertTrue(Modifier.isAbstract(method.getModifiers()));
        assertFalse(method.isDefault());
    }

    public void testTC004CheckDeclaration() throws NoSuchMethodException {
        Method method = GoalDecider.class.getDeclaredMethod("check", Unit.class, PathNode.class);
        assertSame(boolean.class, method.getReturnType());
        assertEquals(2, method.getParameterTypes().length);
        assertSame(Unit.class, method.getParameterTypes()[0]);
        assertSame(PathNode.class, method.getParameterTypes()[1]);
        assertTrue(Modifier.isPublic(method.getModifiers()));
        assertTrue(Modifier.isAbstract(method.getModifiers()));
        assertFalse(method.isDefault());
    }
}
