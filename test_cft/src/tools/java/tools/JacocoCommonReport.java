package tools;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

/**
 * Summarizes JaCoCo CSV coverage strictly for {@code net.sf.freecol.common}
 * and its subpackages, printing a console table and writing {@code common_summary.csv}.
 */
public final class JacocoCommonReport {

    private static final String COMMON_ROOT = "net.sf.freecol.common";

    private static final class Stats {
        long instrMissed;
        long instrCovered;
        long branchMissed;
        long branchCovered;
        long lineMissed;
        long lineCovered;
        long cxtyMissed;
        long cxtyCovered;
        long methodMissed;
        long methodCovered;
        long classMissed;
        long classCovered;

        void addRow(long im, long ic, long bm, long bc, long lm, long lc,
                    long cm, long cc, long mm, long mc) {
            instrMissed += im;
            instrCovered += ic;
            branchMissed += bm;
            branchCovered += bc;
            lineMissed += lm;
            lineCovered += lc;
            cxtyMissed += cm;
            cxtyCovered += cc;
            methodMissed += mm;
            methodCovered += mc;
            if (mc > 0) {
                classCovered++;
            } else {
                classMissed++;
            }
        }

        void add(Stats other) {
            instrMissed += other.instrMissed;
            instrCovered += other.instrCovered;
            branchMissed += other.branchMissed;
            branchCovered += other.branchCovered;
            lineMissed += other.lineMissed;
            lineCovered += other.lineCovered;
            cxtyMissed += other.cxtyMissed;
            cxtyCovered += other.cxtyCovered;
            methodMissed += other.methodMissed;
            methodCovered += other.methodCovered;
            classMissed += other.classMissed;
            classCovered += other.classCovered;
        }
    }

    private JacocoCommonReport() {}

    public static void main(String[] args) throws IOException {
        if (args.length < 1) {
            System.err.println("Usage: JacocoCommonReport <jacoco-report-dir> [subset-label]");
            System.exit(1);
        }

        Path reportDir = Paths.get(args[0]);
        String subsetLabel = args.length >= 2 ? args[1].trim() : "";
        Path jacocoCsv = reportDir.resolve("jacoco.csv");
        if (!Files.isRegularFile(jacocoCsv)) {
            System.err.println("[JaCoCo] ERROR: jacoco.csv not found at " + jacocoCsv.toAbsolutePath());
            System.exit(1);
        }

        Map<String, Stats> byPackage = new TreeMap<>();
        Stats total = new Stats();
        int skippedPackages = 0;

        try (BufferedReader reader = Files.newBufferedReader(jacocoCsv, StandardCharsets.UTF_8)) {
            String header = reader.readLine();
            if (header == null) {
                System.err.println("[JaCoCo] ERROR: jacoco.csv is empty: " + jacocoCsv.toAbsolutePath());
                System.exit(1);
            }
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) {
                    continue;
                }
                String[] cols = splitCsvRow(line);
                if (cols.length < 13) {
                    continue;
                }
                String pkg = cols[1].trim();
                if (!isCommonPackage(pkg)) {
                    skippedPackages++;
                    continue;
                }
                long im = parseLong(cols[3]);
                long ic = parseLong(cols[4]);
                long bm = parseLong(cols[5]);
                long bc = parseLong(cols[6]);
                long lm = parseLong(cols[7]);
                long lc = parseLong(cols[8]);
                long cm = parseLong(cols[9]);
                long cc = parseLong(cols[10]);
                long mm = parseLong(cols[11]);
                long mc = parseLong(cols[12]);

                byPackage.computeIfAbsent(pkg, k -> new Stats())
                         .addRow(im, ic, bm, bc, lm, lc, cm, cc, mm, mc);
                total.addRow(im, ic, bm, bc, lm, lc, cm, cc, mm, mc);
            }
        }

        if (skippedPackages > 0) {
            System.out.println("[JaCoCo] WARNING: Ignored " + skippedPackages + " non-common rows in jacoco.csv.");
        }

        printSummary(byPackage, total, subsetLabel);
        writeSummaryCsv(reportDir.resolve("common_summary.csv"), byPackage, total, subsetLabel);
    }

    private static boolean isCommonPackage(String pkg) {
        return COMMON_ROOT.equals(pkg) || pkg.startsWith(COMMON_ROOT + ".");
    }

    private static String[] splitCsvRow(String line) {
        // JaCoCo CSV class names can contain commas inside anonymous/method descriptors;
        // the numeric counters are always the last 10 fields.
        String[] raw = line.split(",", -1);
        if (raw.length <= 13) {
            return raw;
        }
        String[] normalized = new String[13];
        normalized[0] = raw[0];
        normalized[1] = raw[1];
        StringBuilder cls = new StringBuilder(raw[2]);
        int tailStart = raw.length - 10;
        for (int i = 3; i < tailStart; i++) {
            cls.append(',').append(raw[i]);
        }
        normalized[2] = cls.toString();
        System.arraycopy(raw, tailStart, normalized, 3, 10);
        return normalized;
    }

    private static long parseLong(String s) {
        return Long.parseLong(s.trim());
    }

    private static String fmtRatio(long covered, long missed) {
        long tot = covered + missed;
        if (tot == 0) {
            return "0/0 (n/a)";
        }
        double pct = (100.0 * covered) / tot;
        return String.format(Locale.US, "%d/%d (%.2f%%)", covered, tot, pct);
    }

    private static String fmtPct(long covered, long missed) {
        long tot = covered + missed;
        if (tot == 0) {
            return "n/a";
        }
        return String.format(Locale.US, "%.2f%%", (100.0 * covered) / tot);
    }

    private static void printSummary(Map<String, Stats> byPackage, Stats total, String subsetLabel) {
        System.out.println();
        String title = subsetLabel.isEmpty()
                ? "JaCoCo Coverage Summary (scope: net.sf.freecol.common.** | tests: ALL)"
                : "JaCoCo Coverage Summary (scope: net.sf.freecol.common.** | subset: " + subsetLabel + ")";
        System.out.println("========================================================================================================================");
        System.out.println(title);
        System.out.println("========================================================================================================================");
        System.out.printf("%-40s | %-18s | %-18s | %-18s | %-18s%n",
                "Package", "Instructions", "Branches", "Lines", "Methods");
        System.out.println("-----------------------------------------+--------------------+--------------------+--------------------+--------------------");
        for (Map.Entry<String, Stats> entry : byPackage.entrySet()) {
            Stats s = entry.getValue();
            System.out.printf("%-40s | %-18s | %-18s | %-18s | %-18s%n",
                    entry.getKey(),
                    fmtRatio(s.instrCovered, s.instrMissed),
                    fmtRatio(s.branchCovered, s.branchMissed),
                    fmtRatio(s.lineCovered, s.lineMissed),
                    fmtRatio(s.methodCovered, s.methodMissed));
        }
        System.out.println("-----------------------------------------+--------------------+--------------------+--------------------+--------------------");
        System.out.printf("%-40s | %-18s | %-18s | %-18s | %-18s%n",
                "TOTAL (net.sf.freecol.common)",
                fmtRatio(total.instrCovered, total.instrMissed),
                fmtRatio(total.branchCovered, total.branchMissed),
                fmtRatio(total.lineCovered, total.lineMissed),
                fmtRatio(total.methodCovered, total.methodMissed));
        System.out.printf("Classes covered: %d/%d | Complexity covered: %s%n",
                total.classCovered,
                total.classCovered + total.classMissed,
                fmtRatio(total.cxtyCovered, total.cxtyMissed));
        System.out.println("========================================================================================================================");
    }

    private static void writeSummaryCsv(Path outFile, Map<String, Stats> byPackage,
                                        Stats total, String subsetLabel) throws IOException {
        try (BufferedWriter writer = Files.newBufferedWriter(outFile, StandardCharsets.UTF_8)) {
            writer.write("SUBSET,PACKAGE,INSTRUCTION_COVERED,INSTRUCTION_MISSED,INSTRUCTION_PCT,"
                    + "BRANCH_COVERED,BRANCH_MISSED,BRANCH_PCT,"
                    + "LINE_COVERED,LINE_MISSED,LINE_PCT,"
                    + "METHOD_COVERED,METHOD_MISSED,METHOD_PCT,"
                    + "COMPLEXITY_COVERED,COMPLEXITY_MISSED,COMPLEXITY_PCT,"
                    + "CLASS_COVERED,CLASS_MISSED\n");
            String label = subsetLabel.isEmpty() ? "ALL" : subsetLabel;
            for (Map.Entry<String, Stats> entry : byPackage.entrySet()) {
                writeCsvRow(writer, label, entry.getKey(), entry.getValue());
            }
            writeCsvRow(writer, label, "TOTAL", total);
        }
    }

    private static void writeCsvRow(BufferedWriter writer, String label, String pkg, Stats s) throws IOException {
        writer.write(String.format(Locale.US,
                "%s,%s,%d,%d,%s,%d,%d,%s,%d,%d,%s,%d,%d,%s,%d,%d,%s,%d,%d%n",
                csvEscape(label), csvEscape(pkg),
                s.instrCovered, s.instrMissed, fmtPct(s.instrCovered, s.instrMissed),
                s.branchCovered, s.branchMissed, fmtPct(s.branchCovered, s.branchMissed),
                s.lineCovered, s.lineMissed, fmtPct(s.lineCovered, s.lineMissed),
                s.methodCovered, s.methodMissed, fmtPct(s.methodCovered, s.methodMissed),
                s.cxtyCovered, s.cxtyMissed, fmtPct(s.cxtyCovered, s.cxtyMissed),
                s.classCovered, s.classMissed));
    }

    private static String csvEscape(String value) {
        if (value.contains(",") || value.contains("\"")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}
