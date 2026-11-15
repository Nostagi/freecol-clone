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
import java.util.List;

import javax.xml.stream.XMLStreamException;

import net.sf.freecol.common.io.FreeColXMLReader;
import net.sf.freecol.common.io.FreeColXMLWriter;
import net.sf.freecol.util.test.FreeColTestCase;


/**
 * CFG-driven test suite for {@link Area} derived from
 * {@code test_cft/graph/net/sf/freecol/common/model/Area.json}.
 *
 * Covers all 21 methods (M1..M21) and achieves 100% feasible C3 (node + edge)
 * coverage across 31 Test Paths.
 *
 * Infeasible edges documented:
 * - M14 ({@code copyIn}): {@code N1 -> N2} ({@code B2:false}) — once {@code o} is non-null
 *   ({@code B1:false}), {@code o} is guaranteed to be an {@code Area} (subclass of
 *   {@code FreeColGameObject} and {@code FreeColObject}), so {@code super.copyIn(o)}
 *   always returns {@code true}.
 */
public class AreaCfgTest extends FreeColTestCase {

    private Game game;
    private Map map;
    private Tile tile0;
    private Tile tile1;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        // Pre-condition: Initialize standard Game with a test Map and sample Tiles
        game = getStandardGame();
        map = getTestMap();
        game.setMap(map);
        tile0 = map.getTile(0, 0);
        tile1 = map.getTile(1, 1);
    }

    // ------------------------------------------------------------------
    // Pre-condition helpers & stubs
    // ------------------------------------------------------------------

    private String writeAreaAttributesToXml(Area area) throws Exception {
        StringWriter sw = new StringWriter();
        try (FreeColXMLWriter xw = new FreeColXMLWriter(
                sw, FreeColXMLWriter.WriteScope.toSave(), false)) {
            xw.writeStartDocument("UTF-8", "1.0");
            xw.writeStartElement(Area.TAG);
            area.writeAttributes(xw);
            xw.writeEndElement();
            xw.writeEndDocument();
        }
        return sw.toString();
    }

    private String writeAreaChildrenToXml(Area area) throws Exception {
        StringWriter sw = new StringWriter();
        try (FreeColXMLWriter xw = new FreeColXMLWriter(
                sw, FreeColXMLWriter.WriteScope.toSave(), false)) {
            xw.writeStartDocument("UTF-8", "1.0");
            xw.writeStartElement(Area.TAG);
            area.writeChildren(xw);
            xw.writeEndElement();
            xw.writeEndDocument();
        }
        return sw.toString();
    }

    private FreeColXMLReader openAtRootElement(String xml) throws Exception {
        FreeColXMLReader xr = new FreeColXMLReader(new StringReader(xml));
        xr.nextTag();
        return xr;
    }

    // ------------------------------------------------------------------
    // M1: <init>(Game)V — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M1-TP1: ENTRY -> N0 -> EXIT (construct Area with game only)
    public void testM1_TP1_ConstructorWithGame() {
        Area area = new Area(game);
        assertNotNull(area);
        assertEquals(game, area.getGame());
        assertTrue(area.isEmpty());
    }

    // ------------------------------------------------------------------
    // M2: <init>(Game, String)V — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M2-TP1: ENTRY -> N0 -> EXIT (construct Area with game and id)
    public void testM2_TP1_ConstructorWithGameAndId() {
        Area area = new Area(game, "area:test");
        assertEquals("area:test", area.getId());
        assertNull(area.getNameKey());
    }

    // ------------------------------------------------------------------
    // M3: <init>(Game, String, String)V — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M3-TP1: ENTRY -> N0 -> EXIT (construct Area with game, id, and nameKey)
    public void testM3_TP1_ConstructorWithGameIdAndNameKey() {
        Area area = new Area(game, "area:keyed", "model.area.keyed");
        assertEquals("area:keyed", area.getId());
        assertEquals("model.area.keyed", area.getNameKey());
    }

    // ------------------------------------------------------------------
    // M4: <init>(Game, Area)V — 2 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M4-TP1: ENTRY -> N0 -> N1 -(B1:true)-> N3 -> EXIT (copyFrom has no tiles)
    public void testM4_TP1_CopyConstructorEmptyTiles() {
        Area src = new Area(game, "area:src");
        src.setName("Source Area");

        Game game2 = getStandardGame();
        Area copy = new Area(game2, src);
        assertEquals("area:src", copy.getId());
        assertEquals("Source Area", copy.getName());
        assertTrue(copy.isEmpty());
    }

    // M4-TP2: ENTRY -> N0 -> N1 -(B1:false)-> N2 -> N1 -(B1:true)-> N3 -> EXIT (copyFrom has tiles)
    public void testM4_TP2_CopyConstructorWithTiles() {
        Area src = new Area(game, "area:src", "model.area.src");
        src.addTile(tile0);
        src.addTile(tile1);

        Game game2 = getStandardGame();
        Map map2 = getTestMap();
        game2.setMap(map2);
        Area copy = new Area(game2, src);
        assertEquals("model.area.src", copy.getNameKey());
        assertEquals(2, copy.getTiles().size());
        assertTrue(copy.containsTile(map2.getTile(0, 0)));
        assertTrue(copy.containsTile(map2.getTile(1, 1)));
    }

    // ------------------------------------------------------------------
    // M5: getTiles()Ljava/util/List; — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M5-TP1: ENTRY -> N0 -> EXIT (returns unmodifiable list of tiles)
    public void testM5_TP1_GetTilesReturnsUnmodifiableList() {
        Area area = new Area(game, "area:tiles");
        area.addTile(tile0);
        List<Tile> tiles = area.getTiles();
        assertEquals(1, tiles.size());
        assertEquals(tile0, tiles.get(0));
        try {
            tiles.add(tile1);
            fail("Expected UnsupportedOperationException");
        } catch (UnsupportedOperationException expected) {
            // expected
        }
    }

    // ------------------------------------------------------------------
    // M6: addTile(Tile)V — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M6-TP1: ENTRY -> N0 -> EXIT (adds tile to area)
    public void testM6_TP1_AddTile() {
        Area area = new Area(game, "area:add");
        area.addTile(tile0);
        assertTrue(area.containsTile(tile0));
    }

    // ------------------------------------------------------------------
    // M7: containsTile(Tile)Z — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M7-TP1: ENTRY -> N0 -> EXIT (checks tile membership)
    public void testM7_TP1_ContainsTile() {
        Area area = new Area(game, "area:contains");
        assertFalse(area.containsTile(tile0));
        area.addTile(tile0);
        assertTrue(area.containsTile(tile0));
    }

    // ------------------------------------------------------------------
    // M8: removeTile(Tile)V — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M8-TP1: ENTRY -> N0 -> EXIT (removes tile from area)
    public void testM8_TP1_RemoveTile() {
        Area area = new Area(game, "area:remove");
        area.addTile(tile0);
        area.removeTile(tile0);
        assertFalse(area.containsTile(tile0));
    }

    // ------------------------------------------------------------------
    // M9: getName()Ljava/lang/String; — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M9-TP1: ENTRY -> N0 -> EXIT (returns area name)
    public void testM9_TP1_GetName() {
        Area area = new Area(game, "area:name");
        assertNull(area.getName());
        area.setName("Custom");
        assertEquals("Custom", area.getName());
    }

    // ------------------------------------------------------------------
    // M10: setName(String)V — 2 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M10-TP1: ENTRY -> N0 -(B1:true)-> N2 -> EXIT (nameKey == null -> sets name)
    public void testM10_TP1_SetNameWhenNameKeyNull() {
        Area area = new Area(game, "area:setname");
        area.setName("My Area");
        assertEquals("My Area", area.getName());
    }

    // M10-TP2: ENTRY -> N0 -(B1:false)-> N1 -> EXIT (nameKey != null -> throws IllegalArgumentException)
    public void testM10_TP2_SetNameWhenNameKeyNonNullThrows() {
        Area area = new Area(game, "area:fixed", "model.area.fixed");
        try {
            area.setName("Override");
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException expected) {
            // expected
        }
    }

    // ------------------------------------------------------------------
    // M11: getNameKey()Ljava/lang/String; — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M11-TP1: ENTRY -> N0 -> EXIT (returns nameKey)
    public void testM11_TP1_GetNameKey() {
        Area area = new Area(game, "area:key", "model.area.key");
        assertEquals("model.area.key", area.getNameKey());
    }

    // ------------------------------------------------------------------
    // M12: isEmpty()Z — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M12-TP1: ENTRY -> N0 -> EXIT (returns whether tiles list is empty)
    public void testM12_TP1_IsEmpty() {
        Area area = new Area(game, "area:empty");
        assertTrue(area.isEmpty());
        area.addTile(tile0);
        assertFalse(area.isEmpty());
    }

    // ------------------------------------------------------------------
    // M13: getColor()Ljava/awt/Color; — 3 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M13-TP1: ENTRY -> N0 -(B1:true)-> N3 -> EXIT (id does not start with starting prefix)
    public void testM13_TP1_GetColorFromHashCodeWhenNotStartingArea() {
        Area area = new Area(game, "area");
        int h = "area".hashCode();
        Color expected = new Color(h % 256, (h / 256) % 256, (h / (256 * 256)) % 256);
        assertEquals(expected, area.getColor());
    }

    // M13-TP2: ENTRY -> N0 -(B1:false)-> N1 -(B2:true)-> N3 -> EXIT
    // (id starts with starting prefix, but nation is null -> falls back to hashCode)
    public void testM13_TP2_GetColorStartingPrefixWithUnknownNation() {
        String id = Area.PREFIX_PLAYER_STARTING_POSITION + "x";
        Area area = new Area(game, id);
        int h = id.hashCode();
        Color expected = new Color(h % 256, (h / 256) % 256, (h / (256 * 256)) % 256);
        assertEquals(expected, area.getColor());
    }

    // M13-TP3: ENTRY -> N0 -(B1:false)-> N1 -(B2:false)-> N2 -> EXIT
    // (id starts with starting prefix and nation exists -> returns nation color)
    public void testM13_TP3_GetColorStartingPrefixWithValidNation() {
        Nation dutch = spec().getNation("model.nation.dutch");
        Area area = new Area(game, Area.PREFIX_PLAYER_STARTING_POSITION + dutch.getId());
        assertEquals(dutch.getColor(), area.getColor());
    }

    // ------------------------------------------------------------------
    // M14: copyIn(FreeColObject)Z — 2 feasible Test Paths (100% feasible C3)
    // ------------------------------------------------------------------

    // M14-TP1: ENTRY -> N0 -(B1:true)-> N2 -> EXIT (other is not an Area -> o == null -> false)
    public void testM14_TP1_CopyInNonAreaReturnsFalse() {
        Area area = new Area(game, "area:target");
        assertFalse(area.copyIn(tile0));
    }

    // M14-TP2: ENTRY -> N0 -(B1:false)-> N1 -(B2:true)-> N3 -> EXIT (other is Area -> true)
    public void testM14_TP2_CopyInAreaReturnsTrue() {
        Area target = new Area(game, "area:copyin");
        target.addTile(tile0);
        Game game2 = getStandardGame();
        Area source = new Area(game2, "area:copyin");
        assertTrue(target.copyIn(source));
        assertTrue(target.containsTile(tile0));
    }

    // ------------------------------------------------------------------
    // M15: writeAttributes(FreeColXMLWriter)V — 4 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M15-TP1: ENTRY -> N0 -(B1:true)-> N2 -(B2:true)-> N4 -> EXIT (nameKey == null && name == null)
    public void testM15_TP1_WriteAttributesNeitherNameKeyNorName() throws Exception {
        Area area = new Area(game, "area:w1");
        String xml = writeAreaAttributesToXml(area);
        assertFalse(xml.contains("nameKey="));
        assertFalse(xml.contains("name="));
    }

    // M15-TP2: ENTRY -> N0 -(B1:true)-> N2 -(B2:false)-> N3 -> N4 -> EXIT (nameKey == null && name != null)
    public void testM15_TP2_WriteAttributesNameOnly() throws Exception {
        Area area = new Area(game, "area:w2");
        area.setName("PlainName");
        String xml = writeAreaAttributesToXml(area);
        assertFalse(xml.contains("nameKey="));
        assertTrue(xml.contains("name=\"PlainName\""));
    }

    // M15-TP3: ENTRY -> N0 -(B1:false)-> N1 -> N2 -(B2:true)-> N4 -> EXIT (nameKey != null && name == null)
    public void testM15_TP3_WriteAttributesNameKeyOnly() throws Exception {
        Area area = new Area(game, "area:w3", "model.area.w3");
        String xml = writeAreaAttributesToXml(area);
        assertTrue(xml.contains("nameKey=\"model.area.w3\""));
        assertFalse(xml.contains("name="));
    }

    // M15-TP4: ENTRY -> N0 -(B1:false)-> N1 -> N2 -(B2:false)-> N3 -> N4 -> EXIT (nameKey != null && name != null)
    public void testM15_TP4_WriteAttributesBothNameKeyAndName() throws Exception {
        Area area = new Area(game, "area:w4");
        String inputXml = "<area id=\"area:w4\" nameKey=\"model.area.w4\" name=\"Both\"/>";
        try (FreeColXMLReader xr = openAtRootElement(inputXml)) {
            area.readAttributes(xr);
        }
        String xml = writeAreaAttributesToXml(area);
        assertTrue(xml.contains("nameKey=\"model.area.w4\""));
        assertTrue(xml.contains("name=\"Both\""));
    }

    // ------------------------------------------------------------------
    // M16: writeChildren(FreeColXMLWriter)V — 2 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M16-TP1: ENTRY -> N0 -> N1 -(B1:true)-> N3 -> EXIT (tiles empty)
    public void testM16_TP1_WriteChildrenEmptyTiles() throws Exception {
        Area area = new Area(game, "area:wc1");
        String xml = writeAreaChildrenToXml(area);
        assertFalse(xml.contains("tileReference"));
    }

    // M16-TP2: ENTRY -> N0 -> N1 -(B1:false)-> N2 -> N1 -(B1:true)-> N3 -> EXIT (tiles non-empty)
    public void testM16_TP2_WriteChildrenWithTiles() throws Exception {
        Area area = new Area(game, "area:wc2");
        area.addTile(tile0);
        String xml = writeAreaChildrenToXml(area);
        assertTrue(xml.contains("tileReference"));
        assertTrue(xml.contains(tile0.getId()));
    }

    // ------------------------------------------------------------------
    // M17: readAttributes(FreeColXMLReader)V — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M17-TP1: ENTRY -> N0 -> EXIT (reads nameKey and name attributes)
    public void testM17_TP1_ReadAttributes() throws Exception {
        Area area = new Area(game);
        String xml = "<area id=\"area:ra\" nameKey=\"model.area.ra\" name=\"ReadName\"/>";
        try (FreeColXMLReader xr = openAtRootElement(xml)) {
            area.readAttributes(xr);
        }
        assertEquals("area:ra", area.getId());
        assertEquals("model.area.ra", area.getNameKey());
        assertEquals("ReadName", area.getName());
    }

    // ------------------------------------------------------------------
    // M18: readChildren(FreeColXMLReader)V — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M18-TP1: ENTRY -> N0 -> EXIT (clears tiles and reads children)
    public void testM18_TP1_ReadChildren() throws Exception {
        Area area = new Area(game, "area:rc");
        area.addTile(tile0);
        String xml = "<area id=\"area:rc\"><tileReference id=\"" + tile1.getId() + "\"/></area>";
        try (FreeColXMLReader xr = openAtRootElement(xml)) {
            area.readChildren(xr);
        }
        assertEquals(1, area.getTiles().size());
        assertFalse(area.containsTile(tile0));
        assertTrue(area.containsTile(tile1));
    }

    // ------------------------------------------------------------------
    // M19: readChild(FreeColXMLReader)V — 2 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M19-TP1: ENTRY -> N0 -(B1:false)-> N1 -> N3 -> EXIT (tag equals "tileReference")
    public void testM19_TP1_ReadChildTileReference() throws Exception {
        Area area = new Area(game, "area:child1");
        String xml = "<area id=\"area:child1\"><tileReference id=\"" + tile0.getId() + "\"/></area>";
        try (FreeColXMLReader xr = openAtRootElement(xml)) {
            xr.nextTag();
            area.readChild(xr);
        }
        assertTrue(area.containsTile(tile0));
    }

    // M19-TP2: ENTRY -> N0 -(B1:true)-> N2 -> N3 -> EXIT (tag != "tileReference" -> super.readChild throws)
    public void testM19_TP2_ReadChildUnknownTagDelegatesToSuper() throws Exception {
        Area area = new Area(game, "area:child2");
        String xml = "<area id=\"area:child2\"><unknownTag/></area>";
        try (FreeColXMLReader xr = openAtRootElement(xml)) {
            xr.nextTag();
            try {
                area.readChild(xr);
                fail("Expected XMLStreamException");
            } catch (XMLStreamException expected) {
                // expected from FreeColObject.readChild
            }
        }
    }

    // ------------------------------------------------------------------
    // M20: getXMLTagName()Ljava/lang/String; — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M20-TP1: ENTRY -> N0 -> EXIT (returns TAG)
    public void testM20_TP1_GetXMLTagName() {
        Area area = new Area(game, "area:tag");
        assertEquals(Area.TAG, area.getXMLTagName());
        assertEquals("area", area.getXMLTagName());
    }

    // ------------------------------------------------------------------
    // M21: toString()Ljava/lang/String; — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M21-TP1: ENTRY -> N0 -> EXIT (returns "[id]")
    public void testM21_TP1_ToString() {
        Area area = new Area(game, "area:str");
        assertEquals("[area:str]", area.toString());
    }
}
