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

import net.sf.freecol.util.test.FreeColTestCase;


/**
 * CFG-driven test suite for {@link ObjectWas} derived from
 * {@code test_cft/graph/net/sf/freecol/common/model/ObjectWas.json}.
 *
 * Covers all 1 method (M1: {@code <init>()V}) and achieves 100% C3 (node + edge)
 * coverage across 1 Test Path.
 */
public class ObjectWasCfgTest extends FreeColTestCase {

    @Override
    public void setUp() throws Exception {
        super.setUp();
        // Pre-condition: Abstract base class; no game state required
    }

    // ------------------------------------------------------------------
    // Pre-condition helpers & stubs
    // ------------------------------------------------------------------

    private static class StubObjectWas extends ObjectWas {
        @Override
        public boolean fireChanges() {
            return true;
        }
    }

    // ------------------------------------------------------------------
    // M1: <init>()V — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M1-TP1: ENTRY -> N0 -> EXIT (default constructor invoked via concrete stub)
    public void testM1_TP1_Constructor() {
        ObjectWas was = new StubObjectWas();
        assertTrue(was.fireChanges());
    }
}
