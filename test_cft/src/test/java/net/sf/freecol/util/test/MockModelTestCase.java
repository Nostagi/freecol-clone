package net.sf.freecol.util.test;

import org.junit.After;
import org.junit.Before;

import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.Specification;


public abstract class MockModelTestCase extends FreeColTestCase {

    protected Game game;
    protected Specification specification;

    @Before
    public void initializeMockModel() throws Exception {
        super.setUp();
        specification = spec();
        assertNotNull("The FreeCol specification must be loaded", specification);
        game = getStandardGame();
        assertNotNull("The standard game must be created", game);
    }

    @After
    public void releaseMockModel() throws Exception {
        super.tearDown();
    }
}
