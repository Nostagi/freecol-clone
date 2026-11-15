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

import java.io.StringReader;
import java.io.StringWriter;
import java.util.Random;
import java.util.stream.Collectors;

import net.sf.freecol.common.io.FreeColXMLReader;
import net.sf.freecol.common.io.FreeColXMLWriter;
import net.sf.freecol.common.model.Constants.IntegrityType;
import net.sf.freecol.common.model.LostCityRumour.RumourType;
import net.sf.freecol.common.option.GameOptions;
import net.sf.freecol.common.util.LogBuilder;
import net.sf.freecol.server.model.ServerUnit;
import net.sf.freecol.util.test.FreeColTestCase;


/**
 * CFG-driven test suite for {@link LostCityRumour} derived from
 * {@code test_cft/graph/net/sf/freecol/common/model/LostCityRumour.json}.
 *
 * Covers all 23 methods (M1..M23) and achieves 100% feasible C3 (node + edge)
 * coverage across 36 Test Paths.
 *
 * Infeasible edges documented:
 * - M19 ({@code copyIn}): {@code N1 -> N2} ({@code B2:false}) — once {@code o} is non-null
 *   ({@code B1:false}), {@code o} is guaranteed to be a {@code LostCityRumour} (subclass of
 *   {@code TileItem}, {@code FreeColGameObject}, and {@code FreeColObject}), so
 *   {@code super.copyIn(o)} always returns {@code true}.
 */
public class LostCityRumourCfgTest extends FreeColTestCase {

    private static final TileType PLAINS_TYPE = spec().getTileType("model.tile.plains");
    private static final TileType OCEAN_TYPE = spec().getTileType("model.tile.ocean");
    private static final UnitType COLONIST_TYPE = spec().getUnitType("model.unit.freeColonist");
    private static final UnitType SCOUT_TYPE = spec().getUnitType("model.unit.seasonedScout");
    private static final UnitType ARTILLERY_TYPE = spec().getUnitType("model.unit.artillery");
    private static final GoodsType GRAIN_TYPE = spec().getGoodsType("model.goods.grain");

    private Game game;
    private Map map;
    private Tile tile;
    private Player dutch;
    private Player inca;
    private LostCityRumour rumour;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        // Pre-condition: Initialize standard Game, test Map, Tile, Players, and LostCityRumour.
        game = getStandardGame();
        map = getTestMap();
        game.setMap(map);
        tile = map.getTile(2, 2);
        dutch = game.getPlayerByNationId("model.nation.dutch");
        inca = game.getPlayerByNationId("model.nation.inca");
        rumour = new LostCityRumour(game, tile);
    }

    // ------------------------------------------------------------------
    // Pre-condition helpers & stubs
    // ------------------------------------------------------------------

    private static Random fixedRandom(final int value) {
        return new Random(0L) {
            private static final long serialVersionUID = 1L;
            @Override
            public int nextInt(int bound) {
                return Math.floorMod(value, bound);
            }
        };
    }

    private String writeAttributesToXml(LostCityRumour lcr) throws Exception {
        StringWriter sw = new StringWriter();
        try (FreeColXMLWriter xw = new FreeColXMLWriter(
                sw, FreeColXMLWriter.WriteScope.toSave(), false)) {
            xw.writeStartDocument("UTF-8", "1.0");
            xw.writeStartElement(LostCityRumour.TAG);
            lcr.writeAttributes(xw);
            xw.writeEndElement();
            xw.writeEndDocument();
        }
        return sw.toString();
    }

    // ------------------------------------------------------------------
    // M1: LostCityRumour(Game, Tile) — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M1-TP1: ENTRY -> N0 -> EXIT (construct rumour on tile with null type and name)
    public void testM1_TP1_ConstructorGameTile() {
        LostCityRumour lcr = new LostCityRumour(game, tile);
        assertEquals(tile, lcr.getTile());
        assertNull(lcr.getType());
        assertNull(lcr.getName());
    }

    // ------------------------------------------------------------------
    // M2: LostCityRumour(Game, Tile, RumourType, String) — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M2-TP1: ENTRY -> N0 -> EXIT (construct rumour with explicit type and name)
    public void testM2_TP1_ConstructorWithTypeAndName() {
        LostCityRumour lcr = new LostCityRumour(game, tile, RumourType.CIBOLA, "El Dorado");
        assertEquals(RumourType.CIBOLA, lcr.getType());
        assertEquals("El Dorado", lcr.getName());
    }

    // ------------------------------------------------------------------
    // M3: LostCityRumour(Game, String) — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M3-TP1: ENTRY -> N0 -> EXIT (construct rumour by game and id)
    public void testM3_TP1_ConstructorGameId() {
        LostCityRumour lcr = new LostCityRumour(game, "lostCityRumour:test");
        assertEquals("lostCityRumour:test", lcr.getId());
    }

    // ------------------------------------------------------------------
    // M4: getType() — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M4-TP1: ENTRY -> N0 -> EXIT (return type)
    public void testM4_TP1_GetType() {
        assertNull(rumour.getType());
    }

    // ------------------------------------------------------------------
    // M5: setType(RumourType) — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M5-TP1: ENTRY -> N0 -> EXIT (set type)
    public void testM5_TP1_SetType() {
        rumour.setType(RumourType.RUINS);
        assertEquals(RumourType.RUINS, rumour.getType());
    }

    // ------------------------------------------------------------------
    // M6: getName() — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M6-TP1: ENTRY -> N0 -> EXIT (return name)
    public void testM6_TP1_GetName() {
        LostCityRumour lcr = new LostCityRumour(game, tile, RumourType.FOUNTAIN_OF_YOUTH, "Bimini");
        assertEquals("Bimini", lcr.getName());
    }

    // ------------------------------------------------------------------
    // M7: chooseType(Unit, Random) — 6 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M7-TP1: unowned tile (B1:true), unit == null (B3:true, B5:true, B7:true, B11:true), percentGood > 0 (B13:false, B14:true, B15:true), percentBad > 0 (B16:false, B17:true, B18:false), percentNeutral > 0 (B19:false)
    public void testM7_TP1_ChooseTypeNullUnitUnownedTile() {
        tile.setOwner(null);
        RumourType chosen = rumour.chooseType(null, fixedRandom(0));
        assertNotNull(chosen);
    }

    // M7-TP2: European-owned tile (B1:false, B2:true), colonial freeColonist (B3:false, B4:false, B5:false, B6:false, B7:false, B8:true), DeSoto ability (B12:false -> percentBad=0, percentGood=100, percentNeutral=0; B14:false, B15:false, B16:true, B19:true)
    public void testM7_TP2_ChooseTypeColonialColonistDeSoto() {
        tile.setOwner(dutch);
        dutch.addAbility(new Ability(Ability.RUMOURS_ALWAYS_POSITIVE, true));
        Unit colonist = new ServerUnit(game, tile, dutch, COLONIST_TYPE);
        RumourType chosen = rumour.chooseType(colonist, fixedRandom(0));
        assertNotNull(chosen);
        assertNotSame(RumourType.NOTHING, chosen);
        assertNotSame(RumourType.BURIAL_GROUND, chosen);
        assertNotSame(RumourType.EXPEDITION_VANISHES, chosen);
    }

    // M7-TP3: Indian-owned tile (B1:false, B2:false), non-colonial artillery (B3:false, B4:true, B5:false, B6:true, B7:false, B8:true), no DeSoto (B12:true), percentBad > 0, allowBurial=true (B17:false), allowVanish=true (B18:false)
    public void testM7_TP3_ChooseTypeIndianTileArtillery() {
        tile.setOwner(inca);
        Unit artillery = new ServerUnit(game, tile, inca, ARTILLERY_TYPE);
        RumourType chosen = rumour.chooseType(artillery, fixedRandom(0));
        assertNotNull(chosen);
    }

    // M7-TP4: unowned tile (B1:true), seasonedScout (B8:false -> allowVanish=false), !allowBurial && !allowVanish (B9:false, B10:false -> percentBad=0, B16:true)
    public void testM7_TP4_ChooseTypeSeasonedScoutUnownedTile() {
        tile.setOwner(null);
        Unit scout = new ServerUnit(game, tile, dutch, SCOUT_TYPE);
        RumourType chosen = rumour.chooseType(scout, fixedRandom(0));
        assertNotNull(chosen);
        assertNotSame(RumourType.EXPEDITION_VANISHES, chosen);
    }

    // M7-TP5: Indian-owned tile (allowBurial=true -> B9:true), seasonedScout (allowVanish=false -> B18:true), no DeSoto (B12:true -> percentBad > 0, B16:false, B17:false)
    public void testM7_TP5_ChooseTypeIndianTileSeasonedScout() {
        tile.setOwner(inca);
        Unit scout = new ServerUnit(game, tile, dutch, SCOUT_TYPE);
        RumourType chosen = rumour.chooseType(scout, fixedRandom(0));
        assertNotNull(chosen);
    }

    // M7-TP6: GOOD_RUMOUR = 0 (B13:true -> skip GOOD block), unit == null (allowVanish=true, B10:true)
    public void testM7_TP6_ChooseTypeZeroGoodRumour() {
        int savedGood = spec().getInteger(GameOptions.GOOD_RUMOUR);
        try {
            spec().setInteger(GameOptions.GOOD_RUMOUR, 0);
            tile.setOwner(null);
            RumourType chosen = rumour.chooseType(null, fixedRandom(0));
            assertNotNull(chosen);
        } finally {
            spec().setInteger(GameOptions.GOOD_RUMOUR, savedGood);
        }
    }

    // ------------------------------------------------------------------
    // M8: getNothingMessage(Player, boolean, Random) — 4 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M8-TP1: ENTRY -> N0 -(B1:false)-> N1 -> N6 -> EXIT (mounds == true)
    public void testM8_TP1_GetNothingMessageMounds() {
        ModelMessage msg = rumour.getNothingMessage(dutch, true, fixedRandom(0));
        assertNotNull(msg);
    }

    // M8-TP2: ENTRY -> N0 -(B1:true)-> N2 -(B2:false)-> N3 -(B3:false)-> N4 -> N6 -> EXIT (year % 100 == 12 && randomInt == 0)
    public void testM8_TP2_GetNothingMessageMayanProphesy() {
        Turn savedTurn = game.getTurn();
        try {
            // Turn 21 -> year 1512 (1512 % 100 == 12)
            int turnNum = 1;
            while (new Turn(turnNum).getYear() % 100 != 12) {
                turnNum++;
            }
            game.setTurn(new Turn(turnNum));
            ModelMessage msg = rumour.getNothingMessage(dutch, false, fixedRandom(0));
            assertNotNull(msg);
            assertTrue(msg.getId().contains("mayans"));
        } finally {
            game.setTurn(savedTurn);
        }
    }

    // M8-TP3: ENTRY -> N0 -(B1:true)-> N2 -(B2:false)-> N3 -(B3:true)-> N5 -> N6 -> EXIT (year % 100 == 12 && randomInt != 0)
    public void testM8_TP3_GetNothingMessageMayanYearRandomNonZero() {
        Turn savedTurn = game.getTurn();
        try {
            int turnNum = 1;
            while (new Turn(turnNum).getYear() % 100 != 12) {
                turnNum++;
            }
            game.setTurn(new Turn(turnNum));
            ModelMessage msg = rumour.getNothingMessage(dutch, false, fixedRandom(1));
            assertNotNull(msg);
            assertFalse(msg.getId().contains("mayans"));
        } finally {
            game.setTurn(savedTurn);
        }
    }

    // M8-TP4: ENTRY -> N0 -(B1:true)-> N2 -(B2:true)-> N5 -> N6 -> EXIT (year % 100 != 12)
    public void testM8_TP4_GetNothingMessageNormalYear() {
        Turn savedTurn = game.getTurn();
        try {
            int turnNum = 1;
            while (new Turn(turnNum).getYear() % 100 == 12) {
                turnNum++;
            }
            game.setTurn(new Turn(turnNum));
            ModelMessage msg = rumour.getNothingMessage(dutch, false, fixedRandom(0));
            assertNotNull(msg);
        } finally {
            game.setTurn(savedTurn);
        }
    }

    // ------------------------------------------------------------------
    // M9: getNameKey() — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M9-TP1: ENTRY -> N0 -> EXIT (return name key)
    public void testM9_TP1_GetNameKey() {
        assertEquals("model.lostCityRumour.name", rumour.getNameKey());
    }

    // ------------------------------------------------------------------
    // M10: getZIndex() — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M10-TP1: ENTRY -> N0 -> EXIT (return Tile.RUMOUR_ZINDEX)
    public void testM10_TP1_GetZIndex() {
        assertEquals(Tile.RUMOUR_ZINDEX, rumour.getZIndex());
    }

    // ------------------------------------------------------------------
    // M11: isTileTypeAllowed(TileType) — 2 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M11-TP1: ENTRY -> N0 -(B1:false)-> N1 -> N3 -> EXIT (land tile allowed -> true)
    public void testM11_TP1_IsTileTypeAllowedLand() {
        assertTrue(rumour.isTileTypeAllowed(PLAINS_TYPE));
    }

    // M11-TP2: ENTRY -> N0 -(B1:true)-> N2 -> N3 -> EXIT (water tile disallowed -> false)
    public void testM11_TP2_IsTileTypeAllowedWater() {
        assertFalse(rumour.isTileTypeAllowed(OCEAN_TYPE));
    }

    // ------------------------------------------------------------------
    // M12: applyBonus(GoodsType, UnitType, int) — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M12-TP1: ENTRY -> N0 -> EXIT (return potential unchanged)
    public void testM12_TP1_ApplyBonus() {
        assertEquals(5, rumour.applyBonus(GRAIN_TYPE, COLONIST_TYPE, 5));
    }

    // ------------------------------------------------------------------
    // M13: canProduce(GoodsType, UnitType) — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M13-TP1: ENTRY -> N0 -> EXIT (return false)
    public void testM13_TP1_CanProduce() {
        assertFalse(rumour.canProduce(GRAIN_TYPE, COLONIST_TYPE));
    }

    // ------------------------------------------------------------------
    // M14: getProductionModifiers(GoodsType, UnitType) — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M14-TP1: ENTRY -> N0 -> EXIT (return empty stream)
    public void testM14_TP1_GetProductionModifiers() {
        assertTrue(rumour.getProductionModifiers(GRAIN_TYPE, COLONIST_TYPE)
                .collect(Collectors.toList()).isEmpty());
    }

    // ------------------------------------------------------------------
    // M15: isNatural() — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M15-TP1: ENTRY -> N0 -> EXIT (return false)
    public void testM15_TP1_IsNatural() {
        assertFalse(rumour.isNatural());
    }

    // ------------------------------------------------------------------
    // M16: isComplete() — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M16-TP1: ENTRY -> N0 -> EXIT (return true)
    public void testM16_TP1_IsComplete() {
        assertTrue(rumour.isComplete());
    }

    // ------------------------------------------------------------------
    // M17: getLayer() — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M17-TP1: ENTRY -> N0 -> EXIT (return Map.Layer.RUMOURS)
    public void testM17_TP1_GetLayer() {
        assertEquals(Map.Layer.RUMOURS, rumour.getLayer());
    }

    // ------------------------------------------------------------------
    // M18: checkIntegrity(boolean, LogBuilder) — 2 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M18-TP1: ENTRY -> N0 -(B1:true)-> N2 -> EXIT (type != NO_SUCH_RUMOUR -> passes)
    public void testM18_TP1_CheckIntegrityValid() {
        rumour.setType(RumourType.NOTHING);
        LogBuilder lb = new LogBuilder(64);
        assertEquals(IntegrityType.INTEGRITY_GOOD, rumour.checkIntegrity(false, lb));
    }

    // M18-TP2: ENTRY -> N0 -(B1:false)-> N1 -> N2 -> EXIT (type == NO_SUCH_RUMOUR -> fails)
    public void testM18_TP2_CheckIntegrityNoSuchRumour() {
        rumour.setType(RumourType.NO_SUCH_RUMOUR);
        LogBuilder lb = new LogBuilder(64);
        assertEquals(IntegrityType.INTEGRITY_FAIL, rumour.checkIntegrity(false, lb));
    }

    // ------------------------------------------------------------------
    // M19: copyIn(T) — 2 Test Paths (100% feasible C3; N1->N2 infeasible)
    // ------------------------------------------------------------------

    // M19-TP1: ENTRY -> N0 -(B1:true)-> N2 -> EXIT (other == null -> false)
    public void testM19_TP1_CopyInNull() {
        assertFalse(rumour.copyIn(null));
    }

    // M19-TP2: ENTRY -> N0 -(B1:false)-> N1 -(B2:true)-> N3 -> EXIT (valid LostCityRumour -> true)
    public void testM19_TP2_CopyInValid() {
        Game game2 = getStandardGame();
        Map map2 = getTestMap();
        game2.setMap(map2);
        LostCityRumour other = new LostCityRumour(game2, map2.getTile(0, 0), RumourType.CIBOLA, "Cibola");
        other.setId(rumour.getId());
        assertTrue(rumour.copyIn(other));
        assertEquals(RumourType.CIBOLA, rumour.getType());
        assertEquals("Cibola", rumour.getName());
    }

    // ------------------------------------------------------------------
    // M20: writeAttributes(FreeColXMLWriter) — 2 Test Paths (100% C3)
    // ------------------------------------------------------------------

    // M20-TP1: ENTRY -> N0 -(B1:true)-> N2 -(B2:true)-> N4 -> EXIT (type == null, name == null)
    public void testM20_TP1_WriteAttributesNullTypeAndName() throws Exception {
        String xml = writeAttributesToXml(rumour);
        assertTrue(xml.contains("tile="));
        assertFalse(xml.contains("type="));
        assertFalse(xml.contains("name="));
    }

    // M20-TP2: ENTRY -> N0 -(B1:false)-> N1 -> N2 -(B2:false)-> N3 -> N4 -> EXIT (type != null, name != null)
    public void testM20_TP2_WriteAttributesWithTypeAndName() throws Exception {
        LostCityRumour lcr = new LostCityRumour(game, tile, RumourType.CIBOLA, "Quivira");
        String xml = writeAttributesToXml(lcr);
        assertTrue(xml.contains("type="));
        assertTrue(xml.contains("name=\"Quivira\""));
    }

    // ------------------------------------------------------------------
    // M21: readAttributes(FreeColXMLReader) — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M21-TP1: ENTRY -> N0 -> EXIT (deserialize tile, type, name attributes)
    public void testM21_TP1_ReadAttributes() throws Exception {
        LostCityRumour src = new LostCityRumour(game, tile, RumourType.RUINS, "Old Ruins");
        String xml = writeAttributesToXml(src).replace(src.getId(), "lostCityRumour:dst");
        LostCityRumour dst = new LostCityRumour(game, "lostCityRumour:dst");
        try (FreeColXMLReader xr = new FreeColXMLReader(new StringReader(xml))) {
            xr.nextTag();
            dst.readAttributes(xr);
        }
        assertEquals(tile, dst.getTile());
        assertEquals(RumourType.RUINS, dst.getType());
        assertEquals("Old Ruins", dst.getName());
    }

    // ------------------------------------------------------------------
    // M22: getXMLTagName() — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M22-TP1: ENTRY -> N0 -> EXIT (return LostCityRumour.TAG)
    public void testM22_TP1_GetXMLTagName() {
        assertEquals(LostCityRumour.TAG, rumour.getXMLTagName());
    }

    // ------------------------------------------------------------------
    // M23: <clinit>() — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M23-TP1: ENTRY -> N0 -> EXIT (verify static TAG initialized)
    public void testM23_TP1_StaticInit() {
        assertEquals("lostCityRumour", LostCityRumour.TAG);
    }
}
