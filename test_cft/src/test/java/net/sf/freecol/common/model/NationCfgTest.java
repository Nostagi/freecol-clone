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

import java.awt.Color;
import java.io.StringReader;
import java.io.StringWriter;
import java.lang.reflect.Field;
import java.util.Random;

import net.sf.freecol.common.i18n.Messages;
import net.sf.freecol.common.io.FreeColXMLReader;
import net.sf.freecol.common.io.FreeColXMLWriter;
import net.sf.freecol.util.test.FreeColTestCase;


/**
 * CFG-driven test suite for {@link Nation} derived from
 * {@code test_cft/graph/net/sf/freecol/common/model/Nation.json}.
 *
 * Covers all 19 methods (M1..M19) and achieves 100% feasible C3 (node + edge)
 * coverage across 31 Test Paths.
 *
 * Infeasible edges documented:
 * - M15 ({@code copyIn}): {@code N1 -> N2} ({@code B2:false}) — {@code o} is a
 *   non-null {@code Nation} already validated by {@code copyInCast}, so
 *   {@code FreeColSpecObjectType.copyIn(o)} always returns {@code true}.
 */
public class NationCfgTest extends FreeColTestCase {

    private static final String[] EUROPEAN_NATION_IDS = {
        "model.nation.dutch", "model.nation.english",
        "model.nation.french", "model.nation.spanish",
        "model.nation.danish", "model.nation.portuguese",
        "model.nation.swedish", "model.nation.russian",
        "model.nation.austrian", "model.nation.german",
        "model.nation.prussian", "model.nation.turkish"
    };

    private Nation dutch;
    private Nation dutchRef;
    private Nation customNation;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        // Pre-condition: standard specification nations and a custom Nation instance
        dutch = spec().getNation("model.nation.dutch");
        dutchRef = spec().getNation("model.nation.dutchREF");
        customNation = new Nation("model.nation.cftTest", spec());
        customNation.setType(dutch.getType());
    }

    // ------------------------------------------------------------------
    // Pre-condition helpers & stubs
    // ------------------------------------------------------------------

    private Random fixedZeroRandom() {
        return new Random(0L) {
            @Override
            public int nextInt(int bound) {
                return 0;
            }
        };
    }

    private void addPlayerWithNationId(Game game, String playerId, String nationId)
            throws Exception {
        Player p = new Player(game, playerId);
        Field f = Player.class.getDeclaredField("nationId");
        f.setAccessible(true);
        f.set(p, nationId);
        game.addPlayer(p);
    }

    private void setField(Nation target, String name, Object value) throws Exception {
        Field f = Nation.class.getDeclaredField(name);
        f.setAccessible(true);
        f.set(target, value);
    }

    private String toXml(Nation target) throws Exception {
        StringWriter sw = new StringWriter();
        try (FreeColXMLWriter xw = new FreeColXMLWriter(sw,
                FreeColXMLWriter.WriteScope.toSave(), false)) {
            target.toXML(xw);
        }
        return sw.toString();
    }

    private FreeColXMLReader openReader(String xml) throws Exception {
        FreeColXMLReader xr = new FreeColXMLReader(new StringReader(xml));
        xr.nextTag();
        return xr;
    }

    // ------------------------------------------------------------------
    // M1: Nation(String, Specification) — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M1-TP1: ENTRY -> N0 -> EXIT (constructor with id and specification)
    public void testM1_TP1_constructor() {
        Nation n = new Nation("model.nation.cftInit", spec());
        assertEquals("model.nation.cftInit", n.getId());
        assertSame(spec(), n.getSpecification());
        assertEquals(0, n.getPreferredLatitude());
        assertTrue(n.getStartsOnEastCoast());
    }

    // ------------------------------------------------------------------
    // M2: isUnknownEnemy() — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M2-TP1: ENTRY -> N0 -> EXIT (check unknown enemy id)
    public void testM2_TP1_isUnknownEnemy() {
        assertFalse(dutch.isUnknownEnemy());
        Nation unknown = spec().getNation(Nation.UNKNOWN_NATION_ID);
        assertTrue(unknown.isUnknownEnemy());
    }

    // ------------------------------------------------------------------
    // M3: getType() — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M3-TP1: ENTRY -> N0 -> EXIT (return nation type)
    public void testM3_TP1_getType() {
        assertNotNull(dutch.getType());
    }

    // ------------------------------------------------------------------
    // M4: setType(NationType) — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M4-TP1: ENTRY -> N0 -> EXIT (set nation type)
    public void testM4_TP1_setType() {
        NationType nt = spec().getNationTypes().get(0);
        customNation.setType(nt);
        assertSame(nt, customNation.getType());
    }

    // ------------------------------------------------------------------
    // M5: isSelectable() — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M5-TP1: ENTRY -> N0 -> EXIT (return selectable)
    public void testM5_TP1_isSelectable() {
        assertTrue(dutch.isSelectable());
        assertFalse(dutchRef.isSelectable());
    }

    // ------------------------------------------------------------------
    // M6: getREFNation() — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M6-TP1: ENTRY -> N0 -> EXIT (return refNation)
    public void testM6_TP1_getREFNation() {
        assertSame(dutchRef, dutch.getREFNation());
    }

    // ------------------------------------------------------------------
    // M7: getRebelNation() — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M7-TP1: ENTRY -> N0 -> EXIT (find rebel nation matching REF nation)
    public void testM7_TP1_getRebelNation() {
        assertSame(dutch, dutchRef.getRebelNation());
        assertNull(dutch.getRebelNation());
    }

    // ------------------------------------------------------------------
    // M8: getPreferredLatitude() — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M8-TP1: ENTRY -> N0 -> EXIT (return preferredLatitude)
    public void testM8_TP1_getPreferredLatitude() {
        assertEquals(0, customNation.getPreferredLatitude());
    }

    // ------------------------------------------------------------------
    // M9: getStartsOnEastCoast() — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M9-TP1: ENTRY -> N0 -> EXIT (return startsOnEastCoast)
    public void testM9_TP1_getStartsOnEastCoast() {
        assertTrue(customNation.getStartsOnEastCoast());
    }

    // ------------------------------------------------------------------
    // M10: getColor() — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M10-TP1: ENTRY -> N0 -> EXIT (return color)
    public void testM10_TP1_getColor() {
        assertNotNull(dutch.getColor());
    }

    // ------------------------------------------------------------------
    // M11: setColor(Color) — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M11-TP1: ENTRY -> N0 -> EXIT (set color)
    public void testM11_TP1_setColor() {
        customNation.setColor(Color.RED);
        assertEquals(Color.RED, customNation.getColor());
    }

    // ------------------------------------------------------------------
    // M12: getRulerNameKey() — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M12-TP1: ENTRY -> N0 -> EXIT (return Messages.rulerKey(getId()))
    public void testM12_TP1_getRulerNameKey() {
        assertEquals(Messages.rulerKey(dutch.getId()), dutch.getRulerNameKey());
    }

    // ------------------------------------------------------------------
    // M13: getRulerName() — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M13-TP1: ENTRY -> N0 -> EXIT (return localized ruler name)
    public void testM13_TP1_getRulerName() {
        assertNotNull(dutch.getRulerName());
    }

    // ------------------------------------------------------------------
    // M14: getRandomNonPlayerNationNameKey(Game, Random) — 3 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M14-TP1: ENTRY -> N0 -> N1 -(B1:false)-> N2 -(B2:false)-> N3 -> EXIT (first nation unused)
    public void testM14_TP1_getRandomNonPlayerNationNameKeyFirstUnused() {
        Game game = new Game(spec());
        String key = Nation.getRandomNonPlayerNationNameKey(game, fixedZeroRandom());
        assertEquals(Messages.nameKey("model.nation.dutch"), key);
    }

    // M14-TP2: ENTRY -> N0 -> N1 -(B1:false)-> N2 -(B2:true)-> N4 -> N1 -(B1:false)-> N2 -(B2:false)-> N3 -> EXIT
    public void testM14_TP2_getRandomNonPlayerNationNameKeySkipsUsed() throws Exception {
        Game game = new Game(spec());
        game.setNationOptions(new NationOptions(spec()));
        addPlayerWithNationId(game, "player:0", "model.nation.dutch");
        String key = Nation.getRandomNonPlayerNationNameKey(game, fixedZeroRandom());
        assertEquals(Messages.nameKey("model.nation.english"), key);
    }

    // M14-TP3: ENTRY -> N0 -> N1 -(B1:false)-> N2 -(B2:true)-> N4 -> ... -> N1 -(B1:true)-> N5 -> EXIT (all used)
    public void testM14_TP3_getRandomNonPlayerNationNameKeyAllUsedReturnsEmpty() throws Exception {
        Game game = new Game(spec());
        game.setNationOptions(new NationOptions(spec()));
        for (int i = 0; i < EUROPEAN_NATION_IDS.length; i++) {
            addPlayerWithNationId(game, "player:" + i, EUROPEAN_NATION_IDS[i]);
        }
        assertEquals("", Nation.getRandomNonPlayerNationNameKey(game, fixedZeroRandom()));
    }

    // ------------------------------------------------------------------
    // M15: copyIn(FreeColObject) — 2 Test Paths (100% feasible C3)
    // ------------------------------------------------------------------

    // M15-TP1: ENTRY -> N0 -(B1:true)-> N2 -> EXIT (o == null -> returns false)
    public void testM15_TP1_copyInNullOrIncompatibleReturnsFalse() {
        assertFalse(customNation.copyIn(null));
        assertFalse(customNation.copyIn(spec().getGoodsType("model.goods.sugar")));
    }

    // M15-TP2: ENTRY -> N0 -(B1:false)-> N1 -(B2:true)-> N3 -> EXIT (o != null && super.copyIn(o))
    public void testM15_TP2_copyInValidNationCopiesFields() throws Exception {
        customNation.getFeatureContainer();
        Nation other = new Nation("model.nation.cftTest", spec());
        other.setType(dutch.getType());
        other.setColor(Color.BLUE);
        setField(other, "selectable", true);
        setField(other, "refNation", dutchRef);
        setField(other, "preferredLatitude", 45);
        setField(other, "startsOnEastCoast", false);

        assertTrue(customNation.copyIn(other));
        assertSame(dutch.getType(), customNation.getType());
        assertTrue(customNation.isSelectable());
        assertSame(dutchRef, customNation.getREFNation());
        assertEquals(45, customNation.getPreferredLatitude());
        assertFalse(customNation.getStartsOnEastCoast());
        assertEquals(Color.BLUE, customNation.getColor());
    }

    // ------------------------------------------------------------------
    // M16: writeAttributes(FreeColXMLWriter) — 4 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M16-TP1: ENTRY -> N0 -(B1:true)-> N2 -(B2:true)-> N4 -> EXIT (refNation == null && color == null)
    public void testM16_TP1_writeAttributesNoRefNoColor() throws Exception {
        String xml = toXml(customNation);
        assertFalse(xml.contains("ref="));
        assertFalse(xml.contains("color="));
    }

    // M16-TP2: ENTRY -> N0 -(B1:true)-> N2 -(B2:false)-> N3 -> N4 -> EXIT (refNation == null && color != null)
    public void testM16_TP2_writeAttributesColorOnly() throws Exception {
        customNation.setColor(Color.GREEN);
        String xml = toXml(customNation);
        assertFalse(xml.contains("ref="));
        assertTrue(xml.contains("color="));
    }

    // M16-TP3: ENTRY -> N0 -(B1:false)-> N1 -> N2 -(B2:true)-> N4 -> EXIT (refNation != null && color == null)
    public void testM16_TP3_writeAttributesRefOnly() throws Exception {
        setField(customNation, "refNation", dutchRef);
        String xml = toXml(customNation);
        assertTrue(xml.contains("ref=\"model.nation.dutchREF\""));
        assertFalse(xml.contains("color="));
    }

    // M16-TP4: ENTRY -> N0 -(B1:false)-> N1 -> N2 -(B2:false)-> N3 -> N4 -> EXIT (refNation != null && color != null)
    public void testM16_TP4_writeAttributesRefAndColor() throws Exception {
        setField(customNation, "refNation", dutchRef);
        customNation.setColor(Color.CYAN);
        String xml = toXml(customNation);
        assertTrue(xml.contains("ref=\"model.nation.dutchREF\""));
        assertTrue(xml.contains("color="));
    }

    // ------------------------------------------------------------------
    // M17: readAttributes(FreeColXMLReader) — 8 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M17-TP1: ENTRY -> N0 -(B1:true)-> N2 -> N3 -(B2:true)-> N5 -> N6 -(B3:true)-> N8 -> EXIT
    public void testM17_TP1_readAttributesNewTagsNoColor() throws Exception {
        String xml = "<nation id=\"model.nation.cftTest\" preferred-latitude=\"15\" "
            + "starts-on-east-coast=\"false\"/>";
        try (FreeColXMLReader xr = openReader(xml)) {
            customNation.readAttributes(xr);
        }
        assertEquals(15, customNation.getPreferredLatitude());
        assertFalse(customNation.getStartsOnEastCoast());
        assertNull(customNation.getColor());
    }

    // M17-TP2: ENTRY -> N0 -(B1:true)-> N2 -> N3 -(B2:true)-> N5 -> N6 -(B3:false)-> N7 -> N8 -> EXIT
    public void testM17_TP2_readAttributesNewTagsWithColor() throws Exception {
        String xml = "<nation id=\"model.nation.cftTest\" preferred-latitude=\"15\" "
            + "starts-on-east-coast=\"false\" color=\"" + Color.RED.getRGB() + "\"/>";
        try (FreeColXMLReader xr = openReader(xml)) {
            customNation.readAttributes(xr);
        }
        assertEquals(15, customNation.getPreferredLatitude());
        assertFalse(customNation.getStartsOnEastCoast());
        assertEquals(Color.RED, customNation.getColor());
    }

    // M17-TP3: ENTRY -> N0 -(B1:true)-> N2 -> N3 -(B2:false)-> N4 -> N6 -(B3:true)-> N8 -> EXIT
    public void testM17_TP3_readAttributesNewLatOldEastNoColor() throws Exception {
        String xml = "<nation id=\"model.nation.cftTest\" preferred-latitude=\"20\" "
            + "startsOnEastCoast=\"false\"/>";
        try (FreeColXMLReader xr = openReader(xml)) {
            customNation.readAttributes(xr);
        }
        assertEquals(20, customNation.getPreferredLatitude());
        assertFalse(customNation.getStartsOnEastCoast());
    }

    // M17-TP4: ENTRY -> N0 -(B1:true)-> N2 -> N3 -(B2:false)-> N4 -> N6 -(B3:false)-> N7 -> N8 -> EXIT
    public void testM17_TP4_readAttributesNewLatOldEastWithColor() throws Exception {
        String xml = "<nation id=\"model.nation.cftTest\" preferred-latitude=\"20\" "
            + "startsOnEastCoast=\"false\" color=\"" + Color.BLUE.getRGB() + "\"/>";
        try (FreeColXMLReader xr = openReader(xml)) {
            customNation.readAttributes(xr);
        }
        assertEquals(20, customNation.getPreferredLatitude());
        assertFalse(customNation.getStartsOnEastCoast());
        assertEquals(Color.BLUE, customNation.getColor());
    }

    // M17-TP5: ENTRY -> N0 -(B1:false)-> N1 -> N3 -(B2:true)-> N5 -> N6 -(B3:true)-> N8 -> EXIT
    public void testM17_TP5_readAttributesOldLatNewEastNoColor() throws Exception {
        String xml = "<nation id=\"model.nation.cftTest\" preferredLatitude=\"25\" "
            + "starts-on-east-coast=\"false\"/>";
        try (FreeColXMLReader xr = openReader(xml)) {
            customNation.readAttributes(xr);
        }
        assertEquals(25, customNation.getPreferredLatitude());
        assertFalse(customNation.getStartsOnEastCoast());
    }

    // M17-TP6: ENTRY -> N0 -(B1:false)-> N1 -> N3 -(B2:true)-> N5 -> N6 -(B3:false)-> N7 -> N8 -> EXIT
    public void testM17_TP6_readAttributesOldLatNewEastWithColor() throws Exception {
        String xml = "<nation id=\"model.nation.cftTest\" preferredLatitude=\"25\" "
            + "starts-on-east-coast=\"false\" color=\"" + Color.GREEN.getRGB() + "\"/>";
        try (FreeColXMLReader xr = openReader(xml)) {
            customNation.readAttributes(xr);
        }
        assertEquals(25, customNation.getPreferredLatitude());
        assertFalse(customNation.getStartsOnEastCoast());
        assertEquals(Color.GREEN, customNation.getColor());
    }

    // M17-TP7: ENTRY -> N0 -(B1:false)-> N1 -> N3 -(B2:false)-> N4 -> N6 -(B3:true)-> N8 -> EXIT
    public void testM17_TP7_readAttributesOldTagsNoColor() throws Exception {
        String xml = "<nation id=\"model.nation.cftTest\" preferredLatitude=\"30\" "
            + "startsOnEastCoast=\"false\"/>";
        try (FreeColXMLReader xr = openReader(xml)) {
            customNation.readAttributes(xr);
        }
        assertEquals(30, customNation.getPreferredLatitude());
        assertFalse(customNation.getStartsOnEastCoast());
    }

    // M17-TP8: ENTRY -> N0 -(B1:false)-> N1 -> N3 -(B2:false)-> N4 -> N6 -(B3:false)-> N7 -> N8 -> EXIT
    public void testM17_TP8_readAttributesOldTagsWithColor() throws Exception {
        String xml = "<nation id=\"model.nation.cftTest\" preferredLatitude=\"30\" "
            + "startsOnEastCoast=\"false\" color=\"" + Color.BLACK.getRGB() + "\"/>";
        try (FreeColXMLReader xr = openReader(xml)) {
            customNation.readAttributes(xr);
        }
        assertEquals(30, customNation.getPreferredLatitude());
        assertFalse(customNation.getStartsOnEastCoast());
        assertEquals(Color.BLACK, customNation.getColor());
    }

    // ------------------------------------------------------------------
    // M18: getXMLTagName() — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M18-TP1: ENTRY -> N0 -> EXIT (return TAG)
    public void testM18_TP1_getXMLTagName() {
        assertEquals(Nation.TAG, customNation.getXMLTagName());
    }

    // ------------------------------------------------------------------
    // M19: <clinit>() — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M19-TP1: ENTRY -> N0 -> EXIT (verify static constants initialized in <clinit>)
    public void testM19_TP1_clinitConstants() {
        assertEquals("model.nation.unknownEnemy", Nation.UNKNOWN_NATION_ID);
        assertEquals(Color.BLACK, Nation.UNKNOWN_NATION_COLOR);
    }
}
