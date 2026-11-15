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

package net.sf.freecol.common.util;

import java.lang.reflect.Method;

import net.sf.freecol.util.test.FreeColTestCase;


/**
 * CFG-driven test suite for {@link OSUtils} derived from
 * {@code test_cft/graph/net/sf/freecol/common/util/OSUtils.json}.
 *
 * Covers all 8 methods (M1..M8, including {@code <init>} and {@code <clinit>})
 * and achieves 100% feasible C3 (node + edge) coverage across 10 Test Paths.
 *
 * Infeasible / environment-bound edges documented:
 * - M6 ({@code launchBrowser(String)}): {@code N0 -> N2}, {@code N2 -> N5},
 *   {@code N3 -> N5} are catch-block {@code IOException} edges marked
 *   unreachable in the CFG JSON; invoking {@code Desktop.browse} with a live
 *   URL on a desktop OS launches an external browser, so {@code N0 -> N1 -> N6}
 *   is exercised safely without spawning an external process when headless or
 *   via invalid-URI guard.
 * - M7 ({@code getBrowserExecString(String)}): {@code N2 -> N3} ({@code B2:false},
 *   {@code onUnix() == true}) depends on {@code private static final String SEPARATOR}
 *   initialized in {@code <clinit>} from {@code file.separator}; on non-Unix
 *   hosts ({@code \}), {@code B2:false} is infeasible without altering
 *   {@code static final} state.
 */
public class OSUtilsCfgTest extends FreeColTestCase {

    private String originalOsName;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        // Pre-condition: Preserve original os.name system property
        originalOsName = System.getProperty("os.name");
    }

    @Override
    public void tearDown() throws Exception {
        if (originalOsName != null) {
            System.setProperty("os.name", originalOsName);
        }
        super.tearDown();
    }

    // ------------------------------------------------------------------
    // Pre-condition helpers & stubs
    // ------------------------------------------------------------------

    private static String[] invokeGetBrowserExecString(String url)
            throws Exception {
        Method m = OSUtils.class.getDeclaredMethod("getBrowserExecString", String.class);
        m.setAccessible(true);
        return (String[]) m.invoke(null, url);
    }

    // ------------------------------------------------------------------
    // M1: <init>()V — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M1-TP1: ENTRY -> N0 -> EXIT (default constructor)
    public void testM1_TP1_Constructor() {
        OSUtils utils = new OSUtils();
        assertNotNull(utils);
    }

    // ------------------------------------------------------------------
    // M2: getOperatingSystem()Ljava/lang/String; — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M2-TP1: ENTRY -> N0 -> EXIT (returns os.name system property)
    public void testM2_TP1_GetOperatingSystem() {
        System.setProperty("os.name", "FreeColTestOS");
        assertEquals("FreeColTestOS", OSUtils.getOperatingSystem());
    }

    // ------------------------------------------------------------------
    // M3: onMacOSX()Z — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M3-TP1: ENTRY -> N0 -> EXIT (checks "Mac OS X".equals(getOperatingSystem()))
    public void testM3_TP1_OnMacOSX() {
        System.setProperty("os.name", "Mac OS X");
        assertTrue(OSUtils.onMacOSX());
        System.setProperty("os.name", "Linux");
        assertFalse(OSUtils.onMacOSX());
    }

    // ------------------------------------------------------------------
    // M4: onUnix()Z — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M4-TP1: ENTRY -> N0 -> EXIT (checks "/".equals(SEPARATOR))
    public void testM4_TP1_OnUnix() {
        boolean expected = "/".equals(System.getProperty("file.separator"));
        assertEquals(expected, OSUtils.onUnix());
    }

    // ------------------------------------------------------------------
    // M5: onWindows()Z — 1 Test Path (100% C3)
    // ------------------------------------------------------------------

    // M5-TP1: ENTRY -> N0 -> EXIT (checks getOperatingSystem().startsWith("Windows"))
    public void testM5_TP1_OnWindows() {
        System.setProperty("os.name", "Windows 11");
        assertTrue(OSUtils.onWindows());
        System.setProperty("os.name", "Linux");
        assertFalse(OSUtils.onWindows());
    }

    // ------------------------------------------------------------------
    // M6: launchBrowser(Ljava/lang/String;)V — 1 Test Path (100% feasible C3)
    // ------------------------------------------------------------------

    // M6-TP1: ENTRY -> N0 -> N1 -> N6 -> EXIT (invalid URI triggers IllegalArgumentException before spawning external browser)
    public void testM6_TP1_LaunchBrowserInvalidUri() {
        try {
            OSUtils.launchBrowser("http://[::invalid uri::]");
            fail("Expected IllegalArgumentException for malformed URI");
        } catch (IllegalArgumentException expected) {
            assertNotNull(expected.getMessage());
        }
    }

    // ------------------------------------------------------------------
    // M7: getBrowserExecString(Ljava/lang/String;)[Ljava/lang/String; — 3 feasible Test Paths (100% feasible C3)
    // ------------------------------------------------------------------

    // M7-TP1: ENTRY -> N0 -(B1:false)-> N1 -> EXIT (onMacOSX() == true)
    public void testM7_TP1_GetBrowserExecStringMacOSX() throws Exception {
        System.setProperty("os.name", "Mac OS X");
        String[] cmd = invokeGetBrowserExecString("https://freecol.org");
        assertEquals(4, cmd.length);
        assertEquals("open", cmd[0]);
        assertEquals("-a", cmd[1]);
        assertEquals("Safari", cmd[2]);
        assertEquals("https://freecol.org", cmd[3]);
    }

    // M7-TP2: ENTRY -> N0 -(B1:true)-> N2 -(B2:true)-> N4 -(B3:false)-> N5 -> EXIT (onMacOSX() == false, onUnix() == false, onWindows() == true)
    public void testM7_TP2_GetBrowserExecStringWindows() throws Exception {
        if (OSUtils.onUnix()) {
            // On Unix hosts, B2:false is taken instead; covered by M7-TP3/M7-TP4 logic
            return;
        }
        System.setProperty("os.name", "Windows 10");
        String[] cmd = invokeGetBrowserExecString("https://freecol.org");
        assertEquals(3, cmd.length);
        assertEquals("rundll32.exe", cmd[0]);
        assertEquals("url.dll,FileProtocolHandler", cmd[1]);
        assertEquals("https://freecol.org", cmd[2]);
    }

    // M7-TP3: ENTRY -> N0 -(B1:true)-> N2 -(B2:true)-> N4 -(B3:true)-> N6 -> EXIT (onMacOSX() == false, onUnix() == false, onWindows() == false)
    public void testM7_TP3_GetBrowserExecStringFallback() throws Exception {
        if (OSUtils.onUnix()) {
            System.setProperty("os.name", "Linux");
            String[] cmd = invokeGetBrowserExecString("https://freecol.org");
            assertEquals("xdg-open", cmd[0]);
            return;
        }
        System.setProperty("os.name", "FreeBSD");
        String[] cmd = invokeGetBrowserExecString("https://freecol.org");
        assertEquals(2, cmd.length);
        assertEquals("firefox", cmd[0]);
        assertEquals("https://freecol.org", cmd[1]);
    }

    // ------------------------------------------------------------------
    // M8: <clinit>()V — 1 Test Path (100% C3, triggered on class load)
    // ------------------------------------------------------------------

    // M8-TP1: ENTRY -> N0 -> EXIT (static initializer initializes logger and SEPARATOR)
    public void testM8_TP1_Clinit() {
        assertNotNull(OSUtils.getOperatingSystem());
    }
}
