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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import net.sf.freecol.util.test.FreeColTestCase;


/**
 * CFG-driven test suite for {@link Consumer} derived from
 * {@code test_cft/graph/net/sf/freecol/common/model/Consumer.json}.
 *
 * Covers all 1 method (M1: {@code <clinit>()V}) and achieves 100% C3 (node + edge)
 * coverage across 1 Test Path.
 */
public class ConsumerCfgTest extends FreeColTestCase {

    @Override
    public void setUp() throws Exception {
        super.setUp();
        // Pre-condition: Interface with static comparator and priority constants; no game state required
    }

    // ------------------------------------------------------------------
    // Pre-condition helpers & stubs
    // ------------------------------------------------------------------

    private static class StubConsumer implements Consumer {
        private final int priority;

        StubConsumer(int priority) {
            this.priority = priority;
        }

        @Override
        public List<AbstractGoods> getConsumedGoods() {
            return Collections.emptyList();
        }

        @Override
        public int getPriority() {
            return priority;
        }

        @Override
        public Stream<Modifier> getConsumptionModifiers(String id) {
            return Stream.empty();
        }
    }

    // ------------------------------------------------------------------
    // M1: <clinit>()V — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M1-TP1: ENTRY -> N0 -> EXIT (static initializer initializes Consumer.COMPARATOR)
    public void testM1_TP1_ClinitComparator() {
        assertNotNull(Consumer.COMPARATOR);

        Consumer low = new StubConsumer(Consumer.POPULATION_PRIORITY);
        Consumer mid = new StubConsumer(Consumer.COLONY_PRIORITY);
        Consumer high = new StubConsumer(Consumer.UNIT_PRIORITY);

        List<Consumer> consumers = Arrays.asList(low, mid, high);
        consumers.sort(Consumer.COMPARATOR);

        assertSame(high, consumers.get(0));
        assertSame(mid, consumers.get(1));
        assertSame(low, consumers.get(2));
        assertEquals(0, Consumer.COMPARATOR.compare(mid, new StubConsumer(Consumer.COLONY_PRIORITY)));
    }
}
