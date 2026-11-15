
package net.sf.freecol.common.model;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import net.sf.freecol.common.io.FreeColDirectories;
import net.sf.freecol.common.io.FreeColXMLReader;
import net.sf.freecol.common.io.FreeColXMLWriter;
import net.sf.freecol.util.test.FreeColTestCase;


public class HighScoreCFTTest extends FreeColTestCase {

    private static final UUID ZERO_UUID = new UUID(0L, 0L);


    // Helpers

    private HighScore createScore(int score, UUID uuid)
            throws Exception {
        HighScore hs = new HighScore();
        setField(hs, "score", score);
        setField(hs, "gameUUID", uuid);
        setField(hs, "date", new Date(1000000L));
        setField(hs, "level", HighScore.ScoreLevel.PARASITIC_WORM);
        setField(hs, "playerName", "Test Player");
        setField(hs, "nationId", "test.nation");
        setField(hs, "nationTypeId", "test.nationType");
        setField(hs, "nationName", "Test Nation");
        setField(hs, "difficulty", "model.difficulty.medium");
        setField(hs, "newLandName", "Test World");
        return hs;
    }

    private void setField(Object target, String name, Object value)
            throws Exception {
        Field field = HighScore.class.getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }

    private void invokeTidyScores(List<HighScore> scores)
            throws Exception {
        Method method = HighScore.class.getDeclaredMethod(
            "tidyScores", List.class);
        method.setAccessible(true);
        method.invoke(null, scores);
    }

    private FreeColXMLReader createXMLReader(String xml)
            throws Exception {
        ByteArrayInputStream input = new ByteArrayInputStream(
            xml.getBytes(StandardCharsets.UTF_8));
        FreeColXMLReader xr = new FreeColXMLReader(input);
        xr.nextTag();
        return xr;
    }

    private HighScore readScoreAttributes(String xml) throws Exception {
        try (FreeColXMLReader xr = createXMLReader(xml)) {
            HighScore hs = new HighScore();
            hs.readAttributes(xr);
            return hs;
        }
    }

    private File createTemporaryUserDataDirectory()
            throws Exception {
        Path path = Files.createTempDirectory("freecol-highscore-cft");
        File directory = path.toFile();

        assertNull(FreeColDirectories.setUserDataDirectory(
            directory.getAbsolutePath()));

        return directory;
    }

    private File getOriginalUserDataDirectory() throws Exception {
        Field field = FreeColDirectories.class.getDeclaredField(
            "userDataDirectory");
        field.setAccessible(true);
        return (File) field.get(null);
    }

    private void restoreUserDataDirectory(File original)
            throws Exception {
        Field field = FreeColDirectories.class.getDeclaredField(
            "userDataDirectory");
        field.setAccessible(true);
        field.set(null, original);
    }

    private void deleteTemporaryDirectory(File directory)
            throws IOException {
        if (directory == null || !directory.exists()) return;

        try (java.util.stream.Stream<Path> paths =
                 Files.walk(directory.toPath())) {
            paths.sorted(Comparator.reverseOrder())
                .forEach(path -> {
                    try {
                        Files.deleteIfExists(path);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                });
        }
    }

    @SuppressWarnings("unchecked")
    private List<HighScore> invokeLoadHighScores()
            throws Exception {
        Method method = HighScore.class.getDeclaredMethod(
            "loadHighScores");
        method.setAccessible(true);
        return (List<HighScore>) method.invoke(null);
    }

    private boolean invokeSaveHighScores(List<HighScore> scores)
            throws Exception {
        Method method = HighScore.class.getDeclaredMethod(
            "saveHighScores", List.class);
        method.setAccessible(true);
        return (Boolean) method.invoke(null, scores);
    }


    // Constructors and basic properties

    public void testH01_defaultConstructor() {
        HighScore hs = new HighScore();

        assertNotNull(hs);
        assertEquals(-1, hs.getIndependenceTurn());
        assertEquals(0, hs.getRetirementTurn());
        assertEquals(0, hs.getScore());
        assertEquals(HighScore.TAG, hs.getXMLTagName());
    }

    public void testH02_xmlTagName() {
        assertEquals("highScore", new HighScore().getXMLTagName());
    }

    public void testH03_numberOfHighScores() {
        assertEquals(10, HighScore.NUMBER_OF_HIGH_SCORES);
    }


    // ScoreLevel

    public void testH04_scoreLevelMinimumScores() {
        for (HighScore.ScoreLevel level : HighScore.ScoreLevel.values()) {
            assertTrue(level.getMinimumScore() >= 0);
        }
    }

    public void testH05_scoreLevelOrdering() {
        HighScore.ScoreLevel[] levels = HighScore.ScoreLevel.values();

        for (int i = 1; i < levels.length; i++) {
            assertTrue(levels[i - 1].getMinimumScore()
                       >= levels[i].getMinimumScore());
        }
    }

    public void testH06_scoreLevelLowestBoundary() {
        assertEquals(0,
            HighScore.ScoreLevel.PARASITIC_WORM.getMinimumScore());
    }

    public void testH07_scoreLevelHighestBoundary() {
        assertEquals(40000,
            HighScore.ScoreLevel.CONTINENT.getMinimumScore());
    }


    // Getters

    public void testH08_getters() throws Exception {
        HighScore hs = createScore(5000, UUID.randomUUID());

        assertEquals(5000, hs.getScore());
        assertEquals("Test Player", hs.getPlayerName());
        assertEquals("test.nation", hs.getNationId());
        assertEquals("test.nationType", hs.getNationTypeId());
        assertEquals("Test Nation", hs.getNationName());
        assertEquals("Test World", hs.getNewLandName());
        assertEquals("model.difficulty.medium", hs.getDifficulty());
        assertEquals(HighScore.ScoreLevel.PARASITIC_WORM, hs.getLevel());
    }

    public void testH09_getDateReturnsCopy() throws Exception {
        HighScore hs = createScore(100, UUID.randomUUID());

        Date first = hs.getDate();
        Date second = hs.getDate();

        assertNotNull(first);
        assertNotSame(first, second);
        assertEquals(first.getTime(), second.getTime());
    }

    public void testH10_getDateString() throws Exception {
        HighScore hs = createScore(100, UUID.randomUUID());

        assertNotNull(hs.getDateString());
        assertFalse(hs.getDateString().isEmpty());
    }

    public void testH11_getDateStringWithDifferentDate()
            throws Exception {
        HighScore hs = createScore(100, UUID.randomUUID());
        setField(hs, "date", new Date(0L));

        assertNotNull(hs.getDateString());
    }


    // Comparator

    public void testH12_descendingScoreComparator() throws Exception {
        HighScore low = createScore(100, UUID.randomUUID());
        HighScore high = createScore(900, UUID.randomUUID());

        assertTrue(HighScore.descendingScoreComparator.compare(high, low) < 0);
        assertTrue(HighScore.descendingScoreComparator.compare(low, high) > 0);
    }

    public void testH13_descendingScoreComparatorEqual()
            throws Exception {
        HighScore first = createScore(500, UUID.randomUUID());
        HighScore second = createScore(500, UUID.randomUUID());

        assertEquals(0,
            HighScore.descendingScoreComparator.compare(first, second));
    }


    // checkHighScore - invalid and empty lists

    public void testH14_checkNegativeScore() throws Exception {
        HighScore hs = createScore(-1, ZERO_UUID);

        assertEquals(-1, HighScore.checkHighScore(hs,
                                                  new ArrayList<>()));
    }

    public void testH15_checkZeroScoreWithEmptyList()
            throws Exception {
        HighScore hs = createScore(0, ZERO_UUID);

        assertEquals(0, HighScore.checkHighScore(hs,
                                                 new ArrayList<>()));
    }

    public void testH16_checkPositiveScoreWithEmptyList()
            throws Exception {
        HighScore hs = createScore(100, ZERO_UUID);

        assertEquals(0, HighScore.checkHighScore(hs,
                                                 new ArrayList<>()));
    }


    // checkHighScore - list not full

    public void testH17_checkScoreWithNonFullList()
            throws Exception {
        HighScore hs = createScore(100, ZERO_UUID);
        List<HighScore> scores = new ArrayList<>();
        scores.add(createScore(50, ZERO_UUID));

        assertEquals(1, HighScore.checkHighScore(hs, scores));
    }

    public void testH18_checkScoreWithSeveralExistingScores()
            throws Exception {
        HighScore hs = createScore(1000, ZERO_UUID);
        List<HighScore> scores = Arrays.asList(
            createScore(900, ZERO_UUID),
            createScore(700, ZERO_UUID),
            createScore(500, ZERO_UUID)
        );

        assertEquals(3, HighScore.checkHighScore(hs, scores));
    }


    // checkHighScore - full list

    private List<HighScore> createFullList(int lowestScore)
            throws Exception {
        List<HighScore> scores = new ArrayList<>();

        for (int i = 0; i < HighScore.NUMBER_OF_HIGH_SCORES; i++) {
            scores.add(createScore(1000 - i * 100, ZERO_UUID));
        }

        setField(scores.get(scores.size() - 1), "score", lowestScore);
        return scores;
    }

    public void testH19_fullListHigherScoreReplacesLast()
            throws Exception {
        List<HighScore> scores = createFullList(100);
        HighScore hs = createScore(200, ZERO_UUID);

        assertEquals(9, HighScore.checkHighScore(hs, scores));
    }

    public void testH20_fullListLowerScoreRejected()
            throws Exception {
        List<HighScore> scores = createFullList(100);
        HighScore hs = createScore(50, ZERO_UUID);

        assertEquals(-1, HighScore.checkHighScore(hs, scores));
    }

    public void testH21_fullListEqualLowestScoreRejected()
            throws Exception {
        List<HighScore> scores = createFullList(100);
        HighScore hs = createScore(100, ZERO_UUID);

        assertEquals(-1, HighScore.checkHighScore(hs, scores));
    }


    // checkHighScore - UUID duplicate detection

    public void testH22_duplicateGameHigherScoreReplaces()
            throws Exception {
        UUID uuid = UUID.randomUUID();
        List<HighScore> scores = new ArrayList<>();
        scores.add(createScore(100, uuid));

        HighScore hs = createScore(200, uuid);

        assertEquals(0, HighScore.checkHighScore(hs, scores));
    }

    public void testH23_duplicateGameEqualScoreNotReplaced()
            throws Exception {
        UUID uuid = UUID.randomUUID();
        List<HighScore> scores = new ArrayList<>();
        scores.add(createScore(200, uuid));

        HighScore hs = createScore(200, uuid);

        assertEquals(1, HighScore.checkHighScore(hs, scores));
    }

    public void testH24_duplicateGameLowerScoreNotReplaced()
            throws Exception {
        UUID uuid = UUID.randomUUID();
        List<HighScore> scores = new ArrayList<>();
        scores.add(createScore(300, uuid));

        HighScore hs = createScore(200, uuid);

        assertEquals(1, HighScore.checkHighScore(hs, scores));
    }

    public void testH25_duplicateGameFoundAfterOtherEntries()
            throws Exception {
        UUID uuid = UUID.randomUUID();
        List<HighScore> scores = new ArrayList<>();
        scores.add(createScore(100, UUID.randomUUID()));
        scores.add(createScore(200, UUID.randomUUID()));
        scores.add(createScore(300, uuid));

        HighScore hs = createScore(400, uuid);

        assertEquals(2, HighScore.checkHighScore(hs, scores));
    }

    public void testH26_duplicateGameNotFound()
            throws Exception {
        List<HighScore> scores = new ArrayList<>();
        scores.add(createScore(100, UUID.randomUUID()));
        scores.add(createScore(200, UUID.randomUUID()));

        HighScore hs = createScore(300, UUID.randomUUID());

        assertEquals(2, HighScore.checkHighScore(hs, scores));
    }

    public void testH27_zeroUUIDSkipsDuplicateCheck()
            throws Exception {
        List<HighScore> scores = new ArrayList<>();
        scores.add(createScore(100, ZERO_UUID));

        HighScore hs = createScore(200, ZERO_UUID);

        assertEquals(1, HighScore.checkHighScore(hs, scores));
    }

    public void testH28_duplicateCheckWithNegativeScore()
            throws Exception {
        UUID uuid = UUID.randomUUID();
        List<HighScore> scores = new ArrayList<>();
        scores.add(createScore(100, uuid));

        HighScore hs = createScore(-1, uuid);

        assertEquals(-1, HighScore.checkHighScore(hs, scores));
    }


    // tidyScores

    public void testH29_tidyEmptyList() throws Exception {
        List<HighScore> scores = new ArrayList<>();

        invokeTidyScores(scores);

        assertTrue(scores.isEmpty());
    }

    public void testH30_tidySingleElement() throws Exception {
        List<HighScore> scores = new ArrayList<>();
        scores.add(createScore(100, ZERO_UUID));

        invokeTidyScores(scores);

        assertEquals(1, scores.size());
        assertEquals(100, scores.get(0).getScore());
    }

    public void testH31_tidySortsDescending() throws Exception {
        List<HighScore> scores = new ArrayList<>();
        scores.add(createScore(100, ZERO_UUID));
        scores.add(createScore(300, ZERO_UUID));
        scores.add(createScore(200, ZERO_UUID));

        invokeTidyScores(scores);

        assertEquals(300, scores.get(0).getScore());
        assertEquals(200, scores.get(1).getScore());
        assertEquals(100, scores.get(2).getScore());
    }

    public void testH32_tidyAlreadySortedList() throws Exception {
        List<HighScore> scores = new ArrayList<>();
        scores.add(createScore(300, ZERO_UUID));
        scores.add(createScore(200, ZERO_UUID));
        scores.add(createScore(100, ZERO_UUID));

        invokeTidyScores(scores);

        assertEquals(300, scores.get(0).getScore());
        assertEquals(100, scores.get(2).getScore());
    }

    public void testH33_tidyReverseSortedList() throws Exception {
        List<HighScore> scores = new ArrayList<>();
        scores.add(createScore(100, ZERO_UUID));
        scores.add(createScore(200, ZERO_UUID));
        scores.add(createScore(300, ZERO_UUID));

        invokeTidyScores(scores);

        assertEquals(300, scores.get(0).getScore());
        assertEquals(100, scores.get(2).getScore());
    }

    public void testH34_tidyExactlyTenScores() throws Exception {
        List<HighScore> scores = new ArrayList<>();

        for (int i = 0; i < 10; i++) {
            scores.add(createScore(i * 100, ZERO_UUID));
        }

        invokeTidyScores(scores);

        assertEquals(10, scores.size());
        assertEquals(900, scores.get(0).getScore());
        assertEquals(0, scores.get(9).getScore());
    }

    public void testH35_tidyMoreThanTenScores() throws Exception {
        List<HighScore> scores = new ArrayList<>();

        for (int i = 0; i < 15; i++) {
            scores.add(createScore(i * 100, ZERO_UUID));
        }

        invokeTidyScores(scores);

        assertEquals(15, scores.size());
        assertEquals(800, scores.get(0).getScore());
        assertEquals(700, scores.get(1).getScore());
        assertEquals(0, scores.get(8).getScore());
        assertEquals(900, scores.get(9).getScore());
        assertEquals(1400, scores.get(14).getScore());
    }


    // copyIn

    public void testH36_copyInNull() {
        HighScore target = new HighScore();

        assertFalse(target.copyIn(null));
    }

    public void testH37_copyInWrongType() {
        HighScore target = new HighScore();
        FreeColObject other = new FreeColObject() {
            @Override
            public String getXMLTagName() {
                return "other";
            }
        };

        target.setId("test.highScore");
        other.setId("test.highScore");

        assertFalse(target.copyIn(other));
    }

    public void testH38_copyInValidHighScore() throws Exception {
        HighScore source = createScore(900, UUID.randomUUID());
        HighScore target = createScore(100, UUID.randomUUID());

        source.setId("test.highScore");
        target.setId("test.highScore");

        assertTrue(target.copyIn(source));
        assertEquals(source.getScore(), target.getScore());
        assertEquals(source.getPlayerName(), target.getPlayerName());
        assertEquals(source.getNationId(), target.getNationId());
        assertEquals(source.getNationName(), target.getNationName());
    }

    public void testH39_copyInPreservesDateValue() throws Exception {
        HighScore source = createScore(900, UUID.randomUUID());
        HighScore target = createScore(100, UUID.randomUUID());

        Date sourceDate = new Date(123456789L);
        setField(source, "date", sourceDate);

        source.setId("test.highScore");
        target.setId("test.highScore");

        assertTrue(target.copyIn(source));
        assertNotNull(target.getDate());
        assertEquals(sourceDate.getTime(), target.getDate().getTime());
        assertNotSame(sourceDate, target.getDate());
    }


    // readAttributes

    public void testH40_readAttributesAllValues() throws Exception {
        String xml =
            "<highScore date=\"123456789\""
            + " retirementTurn=\"20\""
            + " independenceTurn=\"15\""
            + " playerName=\"Player A\""
            + " nationId=\"nation.test\""
            + " nationTypeId=\"nationType.test\""
            + " gameUUID=\"123e4567-e89b-12d3-a456-426614174000\""
            + " score=\"5000\""
            + " level=\"TREE\""
            + " nationName=\"Test Nation\""
            + " newLandName=\"Test World\""
            + " difficulty=\"model.difficulty.medium\""
            + " units=\"25\""
            + " colonies=\"8\"/>";

        HighScore hs = readScoreAttributes(xml);

        assertEquals(123456789L, hs.getDate().getTime());
        assertEquals(20, hs.getRetirementTurn());
        assertEquals(15, hs.getIndependenceTurn());
        assertEquals("Player A", hs.getPlayerName());
        assertEquals("nation.test", hs.getNationId());
        assertEquals("nationType.test", hs.getNationTypeId());
        assertEquals(UUID.fromString(
            "123e4567-e89b-12d3-a456-426614174000"),
            hs.getGameUUID());
        assertEquals(5000, hs.getScore());
        assertEquals(HighScore.ScoreLevel.TREE, hs.getLevel());
        assertEquals("Test Nation", hs.getNationName());
        assertEquals("Test World", hs.getNewLandName());
        assertEquals(25, hs.getUnitCount());
        assertEquals(8, hs.getColonyCount());
    }

    public void testH41_readAttributesFloatDateCompatibility()
            throws Exception {
        HighScore hs = readScoreAttributes(
            "<highScore date=\"123456.5\"/>");

        assertEquals(123456L, hs.getDate().getTime());
    }

    public void testH42_readAttributesInvalidDate()
            throws Exception {
        long before = System.currentTimeMillis();

        HighScore hs = readScoreAttributes(
            "<highScore date=\"invalid-date\"/>");

        long after = System.currentTimeMillis();

        assertNotNull(hs.getDate());
        assertTrue(hs.getDate().getTime() >= before);
        assertTrue(hs.getDate().getTime() <= after);
    }

    public void testH43_readAttributesNegativeDate()
            throws Exception {
        long before = System.currentTimeMillis();

        HighScore hs = readScoreAttributes(
            "<highScore date=\"-1\"/>");

        long after = System.currentTimeMillis();

        assertNotNull(hs.getDate());
        assertTrue(hs.getDate().getTime() >= before);
        assertTrue(hs.getDate().getTime() <= after);
    }

    public void testH44_readAttributesInvalidUUID()
            throws Exception {
        HighScore hs = readScoreAttributes(
            "<highScore gameUUID=\"invalid-uuid\"/>");

        assertEquals(ZERO_UUID, hs.getGameUUID());
    }

    public void testH45_readAttributesMissingAttributes()
            throws Exception {
        HighScore hs = readScoreAttributes("<highScore/>");

        assertEquals(0, hs.getRetirementTurn());
        assertEquals(0, hs.getIndependenceTurn());
        assertEquals("anonymous", hs.getPlayerName());
        assertNull(hs.getNationId());
        assertNull(hs.getNationTypeId());
        assertEquals(ZERO_UUID, hs.getGameUUID());
        assertEquals(0, hs.getScore());
        assertEquals(HighScore.ScoreLevel.PARASITIC_WORM,
                     hs.getLevel());
        assertEquals("Freedonia", hs.getNationName());
        assertEquals("New World", hs.getNewLandName());
        assertEquals(0, hs.getUnitCount());
        assertEquals(0, hs.getColonyCount());
    }


    // XML constructor

    public void testH46_xmlConstructor() throws Exception {
        String xml =
            "<highScore date=\"123456789\""
            + " playerName=\"XML Player\""
            + " score=\"2500\""
            + " gameUUID=\"123e4567-e89b-12d3-a456-426614174000\"/>";

        try (FreeColXMLReader xr = createXMLReader(xml)) {
            HighScore hs = new HighScore(xr);

            assertEquals("XML Player", hs.getPlayerName());
            assertEquals(2500, hs.getScore());
            assertEquals(123456789L, hs.getDate().getTime());
        }
    }


    // writeAttributes

    public void testH47_writeAttributesWithOptionalNames()
            throws Exception {
        HighScore hs = createScore(5000, UUID.randomUUID());
        ByteArrayOutputStream output = new ByteArrayOutputStream();

        try (FreeColXMLWriter xw = new FreeColXMLWriter(
                output, FreeColXMLWriter.WriteScope.toSave(), true)) {
            xw.writeStartDocument("UTF-8", "1.0");
            xw.writeStartElement("highScore");
            hs.writeAttributes(xw);
            xw.writeEndElement();
            xw.writeEndDocument();
        }

        String xml = output.toString(StandardCharsets.UTF_8);

        assertTrue(xml.contains("nationName"));
        assertTrue(xml.contains("newLandName"));
        assertTrue(xml.contains("score"));
        assertTrue(xml.contains("playerName"));
    }

    public void testH48_writeAttributesWithoutOptionalNames()
            throws Exception {
        HighScore hs = createScore(1000, UUID.randomUUID());
        setField(hs, "nationName", null);
        setField(hs, "newLandName", null);

        ByteArrayOutputStream output = new ByteArrayOutputStream();

        try (FreeColXMLWriter xw = new FreeColXMLWriter(
                output, FreeColXMLWriter.WriteScope.toSave(), true)) {
            xw.writeStartDocument("UTF-8", "1.0");
            xw.writeStartElement("highScore");
            hs.writeAttributes(xw);
            xw.writeEndElement();
            xw.writeEndDocument();
        }

        String xml = output.toString(StandardCharsets.UTF_8);

        assertFalse(xml.contains("nationName"));
        assertFalse(xml.contains("newLandName"));
        assertTrue(xml.contains("score"));
    }


    // getOldNationNameKey

    public void testH49_getOldNationNameKey() throws Exception {
        HighScore hs = createScore(100, UUID.randomUUID());

        assertNotNull(hs.getOldNationNameKey());
    }


    // saveHighScores

    public void testH50_saveHighScoresNullList() throws Exception {
        assertFalse(invokeSaveHighScores(null));
    }

    public void testH51_saveHighScoresEmptyList() throws Exception {
        File original = getOriginalUserDataDirectory();
        File temporary = createTemporaryUserDataDirectory();

        try {
            List<HighScore> scores = new ArrayList<>();

            assertTrue(invokeSaveHighScores(scores));

            File file = new File(temporary, "HighScores.xml");
            assertTrue(file.exists());
            assertTrue(file.isFile());
            assertTrue(file.length() > 0);
        } finally {
            restoreUserDataDirectory(original);
            deleteTemporaryDirectory(temporary);
        }
    }

    public void testH52_saveHighScoresWithOneEntry() throws Exception {
        File original = getOriginalUserDataDirectory();
        File temporary = createTemporaryUserDataDirectory();

        try {
            List<HighScore> scores = new ArrayList<>();
            scores.add(createScore(5000, UUID.randomUUID()));

            assertTrue(invokeSaveHighScores(scores));

            File file = new File(temporary, "HighScores.xml");
            assertTrue(file.exists());
            assertTrue(file.length() > 0);
        } finally {
            restoreUserDataDirectory(original);
            deleteTemporaryDirectory(temporary);
        }
    }

    public void testH53_saveHighScoresCannotWriteToDirectory()
            throws Exception {
        File original = getOriginalUserDataDirectory();
        File temporary = createTemporaryUserDataDirectory();

        try {
            File scoreFile = new File(temporary, "HighScores.xml");
            assertTrue(scoreFile.mkdir());

            List<HighScore> scores = new ArrayList<>();
            scores.add(createScore(1000, UUID.randomUUID()));

            assertFalse(invokeSaveHighScores(scores));
        } finally {
            restoreUserDataDirectory(original);
            deleteTemporaryDirectory(temporary);
        }
    }


    // loadHighScores

    public void testH54_loadHighScoresFileDoesNotExist()
            throws Exception {
        File original = getOriginalUserDataDirectory();
        File temporary = createTemporaryUserDataDirectory();

        try {
            File file = new File(temporary, "HighScores.xml");
            assertFalse(file.exists());

            List<HighScore> scores = invokeLoadHighScores();

            assertNotNull(scores);
            assertTrue(scores.isEmpty());
            assertTrue(file.exists());
        } finally {
            restoreUserDataDirectory(original);
            deleteTemporaryDirectory(temporary);
        }
    }

    public void testH55_loadHighScoresExistingValidFile()
            throws Exception {
        File original = getOriginalUserDataDirectory();
        File temporary = createTemporaryUserDataDirectory();

        try {
            List<HighScore> expected = new ArrayList<>();
            expected.add(createScore(7500, UUID.randomUUID()));

            assertTrue(invokeSaveHighScores(expected));

            List<HighScore> actual = invokeLoadHighScores();

            assertNotNull(actual);
            assertEquals(1, actual.size());
            assertEquals(7500, actual.get(0).getScore());
            assertEquals("Test Player", actual.get(0).getPlayerName());
        } finally {
            restoreUserDataDirectory(original);
            deleteTemporaryDirectory(temporary);
        }
    }

    public void testH56_loadHighScoresEmptyFile()
            throws Exception {
        File original = getOriginalUserDataDirectory();
        File temporary = createTemporaryUserDataDirectory();

        try {
            File file = new File(temporary, "HighScores.xml");
            assertTrue(file.createNewFile());

            List<HighScore> scores = invokeLoadHighScores();

            assertNotNull(scores);
            assertTrue(scores.isEmpty());
        } finally {
            restoreUserDataDirectory(original);
            deleteTemporaryDirectory(temporary);
        }
    }

}
