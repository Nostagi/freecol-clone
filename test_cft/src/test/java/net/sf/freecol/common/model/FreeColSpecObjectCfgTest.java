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

import net.sf.freecol.common.io.FreeColXMLReader;
import net.sf.freecol.common.io.FreeColXMLWriter;
import net.sf.freecol.util.test.FreeColTestCase;


/**
 * CFG-driven test suite for {@link FreeColSpecObject} derived from
 * {@code test_cft/graph/net/sf/freecol/common/model/FreeColSpecObject.json}.
 *
 * Covers all 4 methods (M1..M4, including {@code <init>}) and achieves 100%
 * feasible C3 (node + edge) coverage across 5 Test Paths.
 *
 * Infeasible edges documented:
 * - M4 ({@code copyIn}): {@code N1 -> N2} ({@code B2:false}, {@code !super.copyIn(o)}) —
 *   When {@code o != null}, {@code copyInCast(other, FreeColSpecObject.class)} has
 *   already verified {@code other != null}, {@code idEquals(other)}, and
 *   {@code o instanceof FreeColSpecObject}; {@link FreeColObject#copyIn(FreeColObject)}
 *   only checks {@code copyInCast(o, FreeColObject.class) != null}, which is
 *   guaranteed {@code true}.
 */
public class FreeColSpecObjectCfgTest extends FreeColTestCase {

    private Specification spec;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        // Pre-condition: Obtain standard specification from FreeColTestCase
        spec = spec();
    }

    // ------------------------------------------------------------------
    // Pre-condition helpers & stubs
    // ------------------------------------------------------------------

    /**
     * Concrete stub for abstract {@link FreeColSpecObject}.
     */
    private static class StubFreeColSpecObject extends FreeColSpecObject {

        StubFreeColSpecObject(Specification specification, String id) {
            super(specification);
            setId(id);
        }

        @Override
        public String getXMLTagName() {
            return "stubFreeColSpecObject";
        }
    }

    /**
     * Minimal {@link FreeColObject} stub (not a {@link FreeColSpecObject})
     * used to test {@code copyInCast} type mismatch.
     */
    private static class StubPlainFreeColObject extends FreeColObject {

        StubPlainFreeColObject(String id) {
            setId(id);
        }

        @Override
        public String getXMLTagName() {
            return "stubPlainFreeColObject";
        }

        @Override
        protected void writeAttributes(FreeColXMLWriter xw) {}

        @Override
        protected void writeChildren(FreeColXMLWriter xw) {}

        @Override
        protected void readAttributes(FreeColXMLReader xr) {}

        @Override
        protected void readChildren(FreeColXMLReader xr) {}
    }

    // ------------------------------------------------------------------
    // M1: <init>(Lnet/sf/freecol/common/model/Specification;)V — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M1-TP1: ENTRY -> N0 -> EXIT (Construct FreeColSpecObject with specification)
    public void testM1_TP1_ConstructorSetsSpecification() {
        StubFreeColSpecObject obj = new StubFreeColSpecObject(spec, "id1");
        assertSame(spec, obj.getSpecification());
    }

    // ------------------------------------------------------------------
    // M2: getSpecification()Lnet/sf/freecol/common/model/Specification; — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M2-TP1: ENTRY -> N0 -> EXIT (Return current specification, including null)
    public void testM2_TP1_GetSpecification() {
        StubFreeColSpecObject withSpec = new StubFreeColSpecObject(spec, "id1");
        assertSame(spec, withSpec.getSpecification());

        StubFreeColSpecObject withoutSpec = new StubFreeColSpecObject(null, "id2");
        assertNull(withoutSpec.getSpecification());
    }

    // ------------------------------------------------------------------
    // M3: setSpecification(Lnet/sf/freecol/common/model/Specification;)V — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M3-TP1: ENTRY -> N0 -> EXIT (Update specification field via protected setter)
    public void testM3_TP1_SetSpecification() {
        StubFreeColSpecObject obj = new StubFreeColSpecObject(null, "id1");
        assertNull(obj.getSpecification());

        obj.setSpecification(spec);
        assertSame(spec, obj.getSpecification());
    }

    // ------------------------------------------------------------------
    // M4: copyIn(Lnet/sf/freecol/common/model/FreeColObject;)Z — 2 feasible Test Paths (100% feasible C3)
    // ------------------------------------------------------------------

    // M4-TP1: ENTRY -> N0 -(B1:true)-> N2 -> EXIT (o == null via incompatible type / null / mismatched ID)
    public void testM4_TP1_CopyInReturnsFalseWhenCastNull() {
        StubFreeColSpecObject target = new StubFreeColSpecObject(spec, "sameId");

        assertFalse(target.copyIn(null));
        assertFalse(target.copyIn(new StubPlainFreeColObject("sameId")));
        assertFalse(target.copyIn(new StubFreeColSpecObject(null, "differentId")));
        assertSame(spec, target.getSpecification());
    }

    // M4-TP2: ENTRY -> N0 -(B1:false)-> N1 -(B2:true)-> N3 -> EXIT (o != null && super.copyIn(o) == true)
    public void testM4_TP2_CopyInCopiesSpecificationWhenCompatible() {
        StubFreeColSpecObject target = new StubFreeColSpecObject(null, "sameId");
        StubFreeColSpecObject source = new StubFreeColSpecObject(spec, "sameId");

        assertTrue(target.copyIn(source));
        assertSame(spec, target.getSpecification());
    }
}
