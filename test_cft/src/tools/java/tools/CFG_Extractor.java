package tools;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LineNumberNode;
import org.objectweb.asm.tree.LookupSwitchInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TableSwitchInsnNode;
import org.objectweb.asm.tree.TryCatchBlockNode;
import org.objectweb.asm.util.Printer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;

/**
 * Extracts method-level control-flow graphs from compiled Java class files.
 *
 * Supported input modes:
 *   1) Two-input CLI mode: <file|config|1|2> <target>
 *      file   net.sf.freecol.common.model.mission.CompoundMission
 *      config high_priority_files.csv
 *
 *   2) Interactive mode: run with no arguments, then enter:
 *      - Option 1 (file/config or 1/2) — validated immediately before asking for target
 *      - Option 2 (FQCN / .class path for file mode, or CSV filename / relative path for config mode)
 *
 *   3) Legacy flag mode (--class / --config) for backward compatibility.
 *
 * Default project paths resolved automatically from the repository / test_cft root:
 *   - Compiled bytecode root: out/production/freecol-master (fallback: test_cft/target/classes)
 *   - Config directory      : test_cft/config
 *   - CFG output directory  : test_cft/graph
 */
public final class CFG_Extractor {
    private static final String TOOL_VERSION = "2.2.0";
    private static final String ASM_VERSION = "9.7";
    private static final int DEFAULT_MAX_PATHS = 50;
    private static final String ROOT_PACKAGE_PREFIX = "out.production.freecol-master.net.sf.freecol";
    private static final Pattern FQCN = Pattern.compile(
            "[A-Za-z_$][A-Za-z0-9_$]*(\\.[A-Za-z_$][A-Za-z0-9_$]*)+");

    private CFG_Extractor() {
    }

    public static void main(String[] args) {
        try {
            if (args == null || args.length == 0) {
                runInteractive();
                return;
            }

            if (args.length == 1 && ("--help".equalsIgnoreCase(args[0]) || "-h".equalsIgnoreCase(args[0]))) {
                printUsage();
                return;
            }

            // If arguments do not start with legacy "--" flags, validate Option 1 immediately.
            if (!args[0].startsWith("--")) {
                String mode = normalizeInputMode(args[0]);
                if (args.length != 2) {
                    throw new IllegalArgumentException(
                            "Two arguments are required: <file|config> <target>. Got " + args.length + " argument(s).");
                }
                runInputMode(mode, args[1]);
                return;
            }

            runLegacyOptionMode(args);
        } catch (Exception exception) {
            System.err.println("CFG_Extractor: ERROR: " + exception.getMessage());
            System.exit(1);
        }
    }

    // ---------------------------------------------------------------------
    // Main input interface (Interactive & 2-argument CLI)
    // ---------------------------------------------------------------------

    private static void runInteractive() throws IOException {
        BufferedReader input = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8));

        System.out.println("CFG_Extractor " + TOOL_VERSION + " (ASM " + ASM_VERSION + ")");
        System.out.println("Choose input type:");
        System.out.println("  1. file   - extract CFG for one class (e.g. net.sf.freecol.common.model.mission.CompoundMission)");
        System.out.println("  2. config - extract CFGs from a CSV config in test_cft/config (e.g. high_priority_files.csv)");
        System.out.print("Input type (file/config or 1/2): ");
        String rawMode = input.readLine();
        // Requirement 1: Validate Option 1 immediately and report error if invalid.
        String mode = normalizeInputMode(rawMode);

        if ("file".equals(mode)) {
            System.out.print("Class name (e.g. net.sf.freecol.common.model.mission.CompoundMission): ");
        } else {
            System.out.print("Config CSV filename (e.g. high_priority_files.csv): ");
        }
        String rawTarget = input.readLine();
        if (rawTarget == null || rawTarget.trim().isEmpty()) {
            throw new IllegalArgumentException("Input target was not provided.");
        }

        runInputMode(mode, rawTarget.trim());
    }

    private static String normalizeInputMode(String rawMode) {
        if (rawMode == null || rawMode.trim().isEmpty()) {
            throw new IllegalArgumentException(
                    "Invalid input type: empty. Expected 'file' or 'config' (or '1' / '2').");
        }
        String mode = stripOuterQuotes(rawMode.trim()).toLowerCase(Locale.ROOT);
        if ("file".equals(mode) || "1".equals(mode)) return "file";
        if ("config".equals(mode) || "2".equals(mode)) return "config";
        throw new IllegalArgumentException(
                "Invalid input type '" + rawMode.trim() + "'. Expected 'file' or 'config' (or '1' / '2').");
    }

    /** Runs the two-input interface using standardized project path resolution. */
    private static void runInputMode(String normalizedMode, String rawTarget) throws IOException {
        String mode = normalizeInputMode(normalizedMode);
        String targetValue = stripOuterQuotes(rawTarget == null ? "" : rawTarget.trim());
        if (targetValue.isEmpty()) {
            throw new IllegalArgumentException("Input target must not be empty.");
        }

        ProjectPaths projectPaths = resolveProjectPaths(null);
        Path outputDirectory = projectPaths.graphDirectory;

        if ("file".equals(mode)) {
            ResolvedClassInput resolved = resolveClassInput(targetValue, projectPaths);

            System.out.println("CFG_Extractor " + TOOL_VERSION + " | SINGLE-FILE MODE");
            System.out.println("Class        : " + resolved.className);
            System.out.println("Class file   : " + resolved.classFile.toAbsolutePath());
            System.out.println("Classes root : " + projectPaths.classesDirectory.toAbsolutePath());
            System.out.println("Output       : " + outputDirectory.toAbsolutePath());
            ExtractionResult result = extractOne(resolved.classFile, resolved.className,
                    outputDirectory, DEFAULT_MAX_PATHS);
            printResult(result);
        } else {
            Path configFile = resolveConfigInput(targetValue, projectPaths);

            System.out.println("CFG_Extractor " + TOOL_VERSION + " | CONFIG MODE");
            System.out.println("Config       : " + configFile.toAbsolutePath());
            System.out.println("Classes root : " + projectPaths.classesDirectory.toAbsolutePath());
            System.out.println("Output       : " + outputDirectory.toAbsolutePath());
            runConfig(configFile, projectPaths.classesDirectory,
                    outputDirectory, DEFAULT_MAX_PATHS);
        }
    }

    /**
     * Requirement 2: Standardizes class input resolution around fully-qualified
     * class names like "net.sf.freecol.common.model.mission.CompoundMission"
     * and automatically locates the compiled .class file under the project's
     * out directory (or fallback classes directories).
     */
    private static ResolvedClassInput resolveClassInput(String rawInput,
            ProjectPaths projectPaths) throws IOException {
        String value = stripOuterQuotes(rawInput == null ? "" : rawInput.trim());
        if (value.isEmpty()) {
            throw new IllegalArgumentException("Class name must not be empty.");
        }
        if (value.toLowerCase(Locale.ROOT).endsWith(".java")) {
            value = value.substring(0, value.length() - ".java".length());
        }

        boolean hasClassSuffix = value.toLowerCase(Locale.ROOT).endsWith(".class");
        boolean pathLike = value.indexOf('/') >= 0 || value.indexOf('\\') >= 0
                || isPathWithDriveOrRoot(value);

        // If the user explicitly supplied a filesystem path to an existing .class file:
        if (hasClassSuffix && pathLike) {
            Path suppliedPath = Paths.get(value);
            List<Path> candidates = new ArrayList<>();
            if (suppliedPath.isAbsolute()) {
                candidates.add(suppliedPath.normalize());
            } else {
                candidates.add(Paths.get("").toAbsolutePath().resolve(suppliedPath).normalize());
                candidates.add(projectPaths.repoRoot.resolve(suppliedPath).normalize());
                candidates.add(projectPaths.testCftRoot.resolve(suppliedPath).normalize());
                for (Path classesDir : projectPaths.candidateClassDirectories) {
                    candidates.add(classesDir.resolve(suppliedPath).normalize());
                }
            }
            for (Path candidate : candidates) {
                if (Files.isRegularFile(candidate)) {
                    return new ResolvedClassInput(candidate, null);
                }
            }
        }

        // Standard FQCN resolution (e.g. "net.sf.freecol.common.model.mission.CompoundMission")
        String classNameCandidate = hasClassSuffix
                ? value.substring(0, value.length() - ".class".length())
                : value;
        String className = normalizeClassName(classNameCandidate);
        if (!FQCN.matcher(className).matches() && !isSimpleJavaIdentifier(className)) {
            throw new IllegalArgumentException(
                    "Invalid class identifier '" + rawInput + "'. Expected a fully-qualified class name "
                    + "such as 'net.sf.freecol.common.model.mission.CompoundMission'.");
        }
        if (!className.startsWith(ROOT_PACKAGE_PREFIX + ".") && !className.equals("FreeCol")) {
            if (className.startsWith("client.") || className.startsWith("common.")
                    || className.startsWith("server.") || className.startsWith("tools.")
                    || className.startsWith("metaserver.")) {
                className = ROOT_PACKAGE_PREFIX + "." + className;
            }
        } else if (className.equals("FreeCol")) {
            className = ROOT_PACKAGE_PREFIX + ".FreeCol";
        }

        String relativeClassPath = className.replace('.', '/') + ".class";
        for (Path classesDir : projectPaths.candidateClassDirectories) {
            Path candidate = classesDir.resolve(relativeClassPath).normalize();
            if (Files.isRegularFile(candidate)) {
                return new ResolvedClassInput(candidate, className);
            }
        }

        Path expectedPrimary = projectPaths.classesDirectory.resolve(relativeClassPath).normalize();
        throw missingCompiledClass(rawInput, className, projectPaths.classesDirectory, expectedPrimary);
    }

    /**
     * Requirement 3: Resolves config CSV input by filename or relative path
     * (e.g. "high_priority_files.csv", "high_priority_files", or "config/high_priority_files.csv")
     * without requiring an absolute path.
     */
    private static Path resolveConfigInput(String rawConfig, ProjectPaths projectPaths)
            throws IOException {
        String value = stripOuterQuotes(rawConfig == null ? "" : rawConfig.trim());
        if (value.isEmpty()) {
            throw new IllegalArgumentException("Config CSV path must not be empty.");
        }

        List<String> nameVariants = new ArrayList<>();
        nameVariants.add(value);
        if (!value.toLowerCase(Locale.ROOT).endsWith(".csv")) {
            nameVariants.add(value + ".csv");
        }

        Path cwd = Paths.get("").toAbsolutePath().normalize();
        List<Path> searched = new ArrayList<>();

        for (String variant : nameVariants) {
            Path p = Paths.get(variant);
            if (p.isAbsolute()) {
                Path normalized = p.normalize();
                if (Files.isRegularFile(normalized)) return normalized;
                searched.add(normalized);
            } else {
                Path inConfigDir = projectPaths.configDirectory.resolve(p).normalize();
                if (Files.isRegularFile(inConfigDir)) return inConfigDir;
                searched.add(inConfigDir);

                Path inTestCft = projectPaths.testCftRoot.resolve(p).normalize();
                if (Files.isRegularFile(inTestCft)) return inTestCft;
                searched.add(inTestCft);

                Path inCwd = cwd.resolve(p).normalize();
                if (Files.isRegularFile(inCwd)) return inCwd;
                searched.add(inCwd);

                Path inRepoRoot = projectPaths.repoRoot.resolve(p).normalize();
                if (Files.isRegularFile(inRepoRoot)) return inRepoRoot;
                searched.add(inRepoRoot);
            }
        }

        throw new IOException("Config CSV file not found for '" + rawConfig + "'. Looked in: "
                + projectPaths.configDirectory.toAbsolutePath()
                + " (tried: " + searched.get(0).toAbsolutePath() + ")");
    }

    private static boolean isPathWithDriveOrRoot(String value) {
        if (value == null || value.isEmpty()) return false;
        if (value.startsWith("/") || value.startsWith("\\")) return true;
        return value.length() >= 2 && Character.isLetter(value.charAt(0)) && value.charAt(1) == ':';
    }

    private static IOException missingCompiledClass(String suppliedValue,
            String className, Path classesDirectory, Path expectedFile) {
        String subject = className == null ? suppliedValue : className;
        return new IOException("Compiled class not found for '" + subject + "'. Expected: "
                + expectedFile.toAbsolutePath()
                + ". Build the project in IntelliJ (out/production/freecol-master) or run scripts/compile.cmd first. "
                + "Classes root: " + classesDirectory.toAbsolutePath());
    }

    private static String stripOuterQuotes(String value) {
        if (value.length() >= 2) {
            char first = value.charAt(0);
            char last = value.charAt(value.length() - 1);
            if ((first == '"' && last == '"') || (first == '\'' && last == '\'')) {
                return value.substring(1, value.length() - 1);
            }
        }
        return value;
    }

    /**
     * Requirement 4: Resolves the repository root, test_cft root, and compiled
     * bytecode directories located OUTSIDE test_cft (such as IntelliJ's
     * out/production/freecol-master) as primary, with Maven's target/classes
     * as fallback.
     */
    private static ProjectPaths resolveProjectPaths(Path hintPath) {
        Path workingDirectory = Paths.get("").toAbsolutePath().normalize();
        Path testCftRoot = findAncestorNamed(hintPath, "test_cft");

        if (testCftRoot == null) {
            testCftRoot = findAncestorNamed(workingDirectory, "test_cft");
        }
        if (testCftRoot == null && "test_cft".equalsIgnoreCase(fileName(workingDirectory))) {
            testCftRoot = workingDirectory;
        }
        if (testCftRoot == null && Files.isDirectory(workingDirectory.resolve("test_cft"))) {
            testCftRoot = workingDirectory.resolve("test_cft");
        }
        if (testCftRoot == null) {
            testCftRoot = workingDirectory.resolve("test_cft");
        }
        testCftRoot = testCftRoot.toAbsolutePath().normalize();

        Path repoRoot = testCftRoot.getParent() != null
                ? testCftRoot.getParent().normalize()
                : workingDirectory;

        // Ordered priority list of compiled bytecode directories:
        // 1. Repo-level IntelliJ output: <repo>/out/production/freecol-master
        // 2. Repo-level IntelliJ output: <repo>/out/production/freecol
        // 3. Any subdirectory under <repo>/out/production/* containing net/sf/freecol
        // 4. Repo-level <repo>/out/classes or <repo>/out or <repo>/build/classes
        // 5. Maven output inside test_cft: <test_cft>/target/classes
        List<Path> candidates = new ArrayList<>();
        candidates.add(repoRoot.resolve("out/production/freecol-master").normalize());
        candidates.add(repoRoot.resolve("out/production/freecol").normalize());

        Path outProduction = repoRoot.resolve("out/production");
        if (Files.isDirectory(outProduction)) {
            try {
                for (Path child : Files.newDirectoryStream(outProduction)) {
                    if (Files.isDirectory(child)) {
                        Path normalized = child.normalize();
                        if (!candidates.contains(normalized)) {
                            candidates.add(normalized);
                        }
                    }
                }
            } catch (IOException ignored) {
            }
        }

        candidates.add(repoRoot.resolve("out/classes").normalize());
        candidates.add(repoRoot.resolve("out").normalize());
        candidates.add(repoRoot.resolve("build/classes").normalize());
        candidates.add(testCftRoot.resolve("target/classes").normalize());

        Path selectedClassesDir = null;
        for (Path candidate : candidates) {
            if (Files.isDirectory(candidate.resolve("net/sf/freecol"))) {
                selectedClassesDir = candidate;
                break;
            }
        }
        if (selectedClassesDir == null) {
            for (Path candidate : candidates) {
                if (Files.isDirectory(candidate)) {
                    selectedClassesDir = candidate;
                    break;
                }
            }
        }
        if (selectedClassesDir == null) {
            selectedClassesDir = repoRoot.resolve("out/production/freecol-master").normalize();
        }

        Path configDir = testCftRoot.resolve("config").normalize();
        Path graphDir = testCftRoot.resolve("graph").normalize();

        return new ProjectPaths(repoRoot, testCftRoot, selectedClassesDir,
                candidates, configDir, graphDir);
    }

    private static Path findAncestorNamed(Path path, String directoryName) {
        if (path == null) return null;
        Path current = Files.isDirectory(path) ? path : path.getParent();
        while (current != null) {
            if (directoryName.equalsIgnoreCase(fileName(current))) return current;
            current = current.getParent();
        }
        return null;
    }

    private static String fileName(Path path) {
        if (path == null || path.getFileName() == null) return "";
        return path.getFileName().toString();
    }

    // ---------------------------------------------------------------------
    // Backward-compatible option interface
    // ---------------------------------------------------------------------

    private static void runLegacyOptionMode(String[] args) throws IOException {
        Map<String, String> options = parseArguments(args);
        if (options.containsKey("--help")) {
            printUsage();
            return;
        }

        boolean singleMode = options.containsKey("--class");
        boolean configMode = options.containsKey("--config");
        if (singleMode == configMode) {
            throw new IllegalArgumentException(
                    "Use: file <FQCN> OR config <csv_name>. Legacy syntax: "
                    + "--class <FQCN|file.class> OR --config <file.csv>.");
        }

        ProjectPaths projectPaths = resolveProjectPaths(null);
        Path outputDirectory = options.containsKey("--out")
                ? Paths.get(options.get("--out")).toAbsolutePath().normalize()
                : projectPaths.graphDirectory;
        int maxPaths = parseNonNegativeInt(
                options.getOrDefault("--max-paths", String.valueOf(DEFAULT_MAX_PATHS)),
                "--max-paths");

        if (singleMode) {
            String rawClassInput = required(options, "--class");
            ResolvedClassInput resolved = resolveClassInput(rawClassInput, projectPaths);
            String expectedName = options.getOrDefault("--class-name", resolved.className);
            ExtractionResult result = extractOne(resolved.classFile, expectedName,
                    outputDirectory, maxPaths);
            printResult(result);
        } else {
            Path configFile = resolveConfigInput(required(options, "--config"), projectPaths);
            Path classesDirectory = options.containsKey("--classes-dir")
                    ? Paths.get(options.get("--classes-dir")).toAbsolutePath().normalize()
                    : projectPaths.classesDirectory;
            runConfig(configFile, classesDirectory, outputDirectory, maxPaths);
        }
    }

    private static Map<String, String> parseArguments(String[] args) {
        Map<String, String> options = new LinkedHashMap<>();
        Set<String> accepted = new HashSet<>(Arrays.asList(
                "--class", "--class-name", "--config", "--classes-dir",
                "--out", "--max-paths", "--help"));

        for (int i = 0; i < args.length; i++) {
            String option = args[i];
            if (!accepted.contains(option)) {
                throw new IllegalArgumentException("Unknown option: " + option);
            }
            if ("--help".equals(option)) {
                options.put(option, "true");
                continue;
            }
            if (i + 1 >= args.length || args[i + 1].startsWith("--")) {
                throw new IllegalArgumentException("Missing value for " + option);
            }
            options.put(option, args[++i]);
        }
        return options;
    }

    private static String required(Map<String, String> options, String key) {
        String value = options.get(key);
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("Required option: " + key);
        }
        return value;
    }

    private static int parseNonNegativeInt(String raw, String option) {
        try {
            int value = Integer.parseInt(raw);
            if (value < 0) {
                throw new IllegalArgumentException(option + " must be >= 0");
            }
            return value;
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException(option + " must be an integer: " + raw);
        }
    }

    private static void runConfig(Path configFile, Path classesDirectory,
            Path outputDirectory, int maxPaths) throws IOException {
        if (!Files.isRegularFile(configFile)) {
            throw new IOException("Config file not found: " + configFile.toAbsolutePath());
        }
        if (!Files.isDirectory(classesDirectory)) {
            throw new IOException("Compiled classes directory not found: "
                    + classesDirectory.toAbsolutePath()
                    + ". Build the project in IntelliJ (out/production/freecol-master) or run scripts/compile.cmd first.");
        }

        List<ClassTarget> targets = readClassTargets(configFile);
        if (targets.isEmpty()) {
            throw new IOException("No class entries could be read from: " + configFile);
        }

        List<BatchResult> results = new ArrayList<>();
        int succeeded = 0;
        int failed = 0;

        System.out.println("Targets      : " + targets.size());
        System.out.println();

        for (ClassTarget target : targets) {
            Path classFile = classesDirectory.resolve(
                    target.className.replace('.', '/') + ".class").normalize();
            try {
                if (!Files.isRegularFile(classFile)) {
                    throw new IOException("Compiled class not found: " + classFile);
                }
                ExtractionResult result = extractOne(classFile, target.className,
                        outputDirectory, maxPaths);
                results.add(new BatchResult(target.className, "OK", result.jsonFile.toString()));
                succeeded++;
                System.out.println("[OK]   " + target.className
                        + " | methods=" + result.methodCount
                        + " | " + result.jsonFile);
            } catch (Exception ex) {
                results.add(new BatchResult(target.className, "FAILED", ex.getMessage()));
                failed++;
                System.err.println("[FAIL] " + target.className + " | " + ex.getMessage());
            }
        }

        Files.createDirectories(outputDirectory);
        Path summary = outputDirectory.resolve("cfg_extraction_summary.csv");
        writeBatchSummary(summary, results);

        System.out.println();
        System.out.println("Batch extraction finished.");
        System.out.println("Succeeded : " + succeeded);
        System.out.println("Failed    : " + failed);
        System.out.println("Summary   : " + summary.toAbsolutePath());
        if (failed > 0) {
            throw new IOException(failed + " class(es) failed. See " + summary);
        }
    }

    private static ExtractionResult extractOne(Path classFile, String expectedClassName,
            Path outputDirectory, int maxPaths) throws IOException {
        if (!Files.isRegularFile(classFile)) {
            throw new IOException("Class file not found: " + classFile.toAbsolutePath());
        }

        ClassNode classNode = new ClassNode();
        ClassReader reader = new ClassReader(Files.readAllBytes(classFile));
        reader.accept(classNode, ClassReader.EXPAND_FRAMES);

        String actualClassName = classNode.name.replace('/', '.');
        if (expectedClassName != null && !expectedClassName.trim().isEmpty()) {
            String normalizedExpected = normalizeClassName(expectedClassName);
            if (!actualClassName.equals(normalizedExpected)) {
                throw new IOException("Class name mismatch. Expected '"
                        + normalizedExpected + "' but bytecode declares '"
                        + actualClassName + "'.");
            }
        }

        List<MethodGraph> methodGraphs = new ArrayList<>();
        for (MethodNode method : classNode.methods) {
            if ((method.access & (Opcodes.ACC_ABSTRACT | Opcodes.ACC_NATIVE)) != 0) {
                continue;
            }
            if (method.instructions == null || method.instructions.size() == 0) {
                continue;
            }
            methodGraphs.add(buildMethodGraph(method, maxPaths));
        }

        Path packageDirectory = outputDirectory.resolve(
                packagePath(actualClassName)).normalize();
        Files.createDirectories(packageDirectory);
        String simpleClassName = simpleClassName(actualClassName);
        Path jsonFile = packageDirectory.resolve(simpleClassName + ".json");
        Path dotFile = packageDirectory.resolve(simpleClassName + ".dot");

        Files.write(jsonFile, renderJson(actualClassName, classNode.sourceFile, methodGraphs)
                .getBytes(StandardCharsets.UTF_8));
        Files.write(dotFile, renderDot(actualClassName, methodGraphs)
                .getBytes(StandardCharsets.UTF_8));

        return new ExtractionResult(actualClassName, methodGraphs.size(), jsonFile, dotFile,
                methodGraphs);
    }

    private static void printResult(ExtractionResult result) {
        System.out.println("CFG extraction completed.");
        System.out.println("Class             : " + result.className);
        System.out.println("Methods extracted : " + result.methodCount);
        System.out.println("JSON              : " + result.jsonFile.toAbsolutePath());
        System.out.println("DOT               : " + result.dotFile.toAbsolutePath());
        for (MethodGraph graph : result.methods) {
            System.out.println("  " + graph.name + graph.descriptor
                    + " | nodes=" + graph.nodeCount
                    + " | edges=" + graph.edgeCount
                    + " | V(G)=" + graph.cyclomatic
                    + " | branches=" + graph.branches.size());
        }
    }

    // ---------------------------------------------------------------------
    // CSV configuration reader
    // ---------------------------------------------------------------------

    /**
     * Reads class targets from high_priority_files.csv or similar CSV files.
     * Prioritizes the `file` column (e.g. net/sf/freecol/common/model/Colony.java)
     * and properly maps `(root)` or relative package names under `net.sf.freecol`.
     */
    private static List<ClassTarget> readClassTargets(Path configFile) throws IOException {
        List<String> lines = Files.readAllLines(configFile, StandardCharsets.UTF_8);
        List<List<String>> rows = new ArrayList<>();
        for (String line : lines) {
            if (line == null || line.trim().isEmpty() || line.trim().startsWith("#")) {
                continue;
            }
            rows.add(parseCsvLine(line));
        }
        if (rows.isEmpty()) {
            return Collections.emptyList();
        }

        List<String> first = rows.get(0);
        Map<String, Integer> header = detectHeader(first);
        int startRow = header.isEmpty() ? 0 : 1;
        Integer classColumn = findHeaderColumn(header,
                "class_name", "fully_qualified_class_name", "fqcn", "qualified_name",
                "binary_name", "class");
        Integer packageColumn = findHeaderColumn(header, "package_name", "package", "package_path");
        Integer pathColumn = findHeaderColumn(header,
                "source_path", "source_file", "java_file", "class_file",
                "relative_path", "file_path", "path", "file", "source");

        LinkedHashMap<String, ClassTarget> unique = new LinkedHashMap<>();
        for (int rowIndex = startRow; rowIndex < rows.size(); rowIndex++) {
            List<String> row = rows.get(rowIndex);
            String packageName = valueAt(row, packageColumn);
            String className = null;

            // 1. Prefer the full relative file path column (e.g. net/sf/freecol/.../Foo.java)
            String pathValue = valueAt(row, pathColumn);
            if (pathValue != null) {
                className = classNameFromPathOrName(pathValue);
            }

            // 2. Otherwise check the class_name column + package column
            String classValue = valueAt(row, classColumn);
            if (className == null && classValue != null) {
                String candidate = classNameFromPathOrName(classValue);
                if (candidate == null && packageName != null) {
                    candidate = normalizeSimpleWithPackage(classValue, packageName);
                }
                if (candidate != null) className = candidate;
            }

            // 3. Fallback scan across cells
            if (className == null) {
                for (String cell : row) {
                    String candidate = classNameFromPathOrName(cell);
                    if (candidate != null) {
                        className = candidate;
                        break;
                    }
                }
            }
            if (className == null && packageName != null) {
                for (String cell : row) {
                    if (isSimpleJavaIdentifier(cell)) {
                        className = normalizeSimpleWithPackage(cell, packageName);
                        break;
                    }
                }
            }

            if (className != null) {
                unique.putIfAbsent(className, new ClassTarget(className, rowIndex + 1));
            }
        }
        return new ArrayList<>(unique.values());
    }

    private static Map<String, Integer> detectHeader(List<String> cells) {
        Map<String, Integer> result = new HashMap<>();
        Set<String> known = new HashSet<>(Arrays.asList(
                "class_name", "fully_qualified_class_name", "fqcn", "qualified_name",
                "binary_name", "class", "package_name", "package", "package_path",
                "source_path", "source_file", "java_file", "class_file",
                "relative_path", "file_path", "path", "file", "source"));
        for (int i = 0; i < cells.size(); i++) {
            String normalized = normalizeHeader(cells.get(i));
            if (known.contains(normalized)) result.put(normalized, i);
        }
        return result;
    }

    private static Integer findHeaderColumn(Map<String, Integer> header, String... names) {
        for (String name : names) {
            if (header.containsKey(name)) return header.get(name);
        }
        return null;
    }

    private static String normalizeHeader(String value) {
        if (value == null) return "";
        return value.replace("\uFEFF", "").trim().toLowerCase(Locale.ROOT)
                .replace('-', '_').replace(' ', '_');
    }

    private static String valueAt(List<String> values, Integer index) {
        if (index == null || index < 0 || index >= values.size()) return null;
        String value = values.get(index);
        return value == null || value.trim().isEmpty() ? null : value.trim();
    }

    private static List<String> parseCsvLine(String line) {
        List<String> fields = new ArrayList<>();
        StringBuilder field = new StringBuilder();
        boolean quoted = false;
        for (int i = 0; i < line.length(); i++) {
            char ch = line.charAt(i);
            if (ch == '"') {
                if (quoted && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    field.append('"');
                    i++;
                } else {
                    quoted = !quoted;
                }
            } else if (ch == ',' && !quoted) {
                fields.add(field.toString().trim());
                field.setLength(0);
            } else {
                field.append(ch);
            }
        }
        fields.add(field.toString().trim());
        return fields;
    }

    private static String classNameFromPathOrName(String raw) {
        if (raw == null) return null;
        String value = raw.trim().replace("\uFEFF", "");
        if (value.isEmpty()) return null;
        value = stripOuterQuotes(value).trim();
        value = value.replace('\\', '/');

        if (value.endsWith(".java") || value.endsWith(".class")) {
            String noExtension = value.substring(0, value.lastIndexOf('.'));
            int knownRoot = noExtension.indexOf("net/sf/freecol/");
            if (knownRoot >= 0) {
                return noExtension.substring(knownRoot).replace('/', '.');
            }
            int src = noExtension.lastIndexOf("/src/");
            if (src >= 0) {
                noExtension = noExtension.substring(src + 5);
            } else if (noExtension.startsWith("src/")) {
                noExtension = noExtension.substring(4);
            }
            if (noExtension.startsWith("main/java/")) {
                noExtension = noExtension.substring("main/java/".length());
            } else if (noExtension.startsWith("test/java/")) {
                noExtension = noExtension.substring("test/java/".length());
            }
            while (noExtension.startsWith("./")) noExtension = noExtension.substring(2);
            if (noExtension.contains("/")) {
                String candidate = noExtension.replace('/', '.');
                return ensureRootPackagePrefix(candidate);
            }
            return null;
        }

        if (value.contains("/")) {
            int knownRoot = value.indexOf("net/sf/freecol/");
            if (knownRoot >= 0) return value.substring(knownRoot).replace('/', '.');
            return null;
        }

        value = stripClassSuffix(value);
        if (FQCN.matcher(value).matches()) {
            return ensureRootPackagePrefix(value);
        }
        return null;
    }

    private static String normalizeSimpleWithPackage(String classValue, String packageValue) {
        String simple = stripClassSuffix(classValue.trim());
        if (!isSimpleJavaIdentifier(simple)) return null;

        String pkg = packageValue == null ? "" : packageValue.trim().replace('\\', '.').replace('/', '.');
        if ("(root)".equalsIgnoreCase(pkg) || "root".equalsIgnoreCase(pkg) || ".".equals(pkg)) {
            return ROOT_PACKAGE_PREFIX + "." + simple;
        }
        while (pkg.startsWith(".")) pkg = pkg.substring(1);
        while (pkg.endsWith(".")) pkg = pkg.substring(0, pkg.length() - 1);
        if (pkg.isEmpty()) return ROOT_PACKAGE_PREFIX + "." + simple;

        String combined = pkg + "." + simple;
        return ensureRootPackagePrefix(combined);
    }

    private static String ensureRootPackagePrefix(String className) {
        if (className == null || className.isEmpty()) return className;
        if (className.startsWith("(root).")) {
            return ROOT_PACKAGE_PREFIX + "." + className.substring("(root).".length());
        }
        if (!className.startsWith(ROOT_PACKAGE_PREFIX + ".")
                && (className.startsWith("client.")
                || className.startsWith("common.")
                || className.startsWith("server.")
                || className.startsWith("tools.")
                || className.startsWith("metaserver."))) {
            return ROOT_PACKAGE_PREFIX + "." + className;
        }
        return className;
    }

    private static String normalizeClassName(String value) {
        String normalized = classNameFromPathOrName(value);
        if (normalized != null) return normalized;
        String fallback = stripClassSuffix(value.trim()).replace('/', '.').replace('\\', '.');
        return ensureRootPackagePrefix(fallback);
    }

    private static String stripClassSuffix(String value) {
        if (value.endsWith(".java") || value.endsWith(".class")) {
            return value.substring(0, value.lastIndexOf('.'));
        }
        return value;
    }

    private static boolean isSimpleJavaIdentifier(String value) {
        if (value == null) return false;
        String candidate = stripClassSuffix(value.trim());
        if (candidate.isEmpty() || candidate.contains(".") || candidate.contains("/")) return false;
        if (!Character.isJavaIdentifierStart(candidate.charAt(0))) return false;
        for (int i = 1; i < candidate.length(); i++) {
            if (!Character.isJavaIdentifierPart(candidate.charAt(i))) return false;
        }
        return true;
    }

    private static void writeBatchSummary(Path summaryFile, List<BatchResult> results)
            throws IOException {
        StringBuilder csv = new StringBuilder("class,status,detail\n");
        for (BatchResult result : results) {
            csv.append(csvQuote(result.className)).append(',')
                    .append(csvQuote(result.status)).append(',')
                    .append(csvQuote(result.detail)).append('\n');
        }
        Files.write(summaryFile, csv.toString().getBytes(StandardCharsets.UTF_8));
    }

    private static String csvQuote(String value) {
        if (value == null) return "\"\"";
        return "\"" + value.replace("\"", "\"\"") + "\"";
    }

    // ---------------------------------------------------------------------
    // Bytecode -> basic blocks -> CFG
    // ---------------------------------------------------------------------

    private static MethodGraph buildMethodGraph(MethodNode method, int maxPaths) {
        AbstractInsnNode[] instructions = method.instructions.toArray();
        int instructionCount = instructions.length;
        IdentityHashMap<LabelNode, Integer> labelIndexes = new IdentityHashMap<>();

        for (int i = 0; i < instructionCount; i++) {
            if (instructions[i] instanceof LabelNode) {
                labelIndexes.put((LabelNode) instructions[i], i);
            }
        }
        IdentityHashMap<LabelNode, Integer> executableLabelIndexes = new IdentityHashMap<>();
        for (Map.Entry<LabelNode, Integer> entry : labelIndexes.entrySet()) {
            int executable = nextExecutable(instructions, entry.getValue(), instructionCount);
            if (executable >= 0) executableLabelIndexes.put(entry.getKey(), executable);
        }

        int firstExecutable = nextExecutable(instructions, 0, instructionCount);
        if (firstExecutable < 0) return MethodGraph.empty(method.name, method.desc);

        TreeSet<Integer> leaders = new TreeSet<>();
        leaders.add(firstExecutable);

        for (int i = 0; i < instructionCount; i++) {
            AbstractInsnNode insn = instructions[i];
            int opcode = insn.getOpcode();

            if (insn instanceof JumpInsnNode) {
                JumpInsnNode jump = (JumpInsnNode) insn;
                addLabelLeader(leaders, jump.label, labelIndexes, instructions);
                addLeader(leaders, nextExecutable(instructions, i + 1, instructionCount));
            } else if (insn instanceof TableSwitchInsnNode) {
                TableSwitchInsnNode sw = (TableSwitchInsnNode) insn;
                addLabelLeader(leaders, sw.dflt, labelIndexes, instructions);
                for (LabelNode label : sw.labels) addLabelLeader(leaders, label, labelIndexes, instructions);
                addLeader(leaders, nextExecutable(instructions, i + 1, instructionCount));
            } else if (insn instanceof LookupSwitchInsnNode) {
                LookupSwitchInsnNode sw = (LookupSwitchInsnNode) insn;
                addLabelLeader(leaders, sw.dflt, labelIndexes, instructions);
                for (LabelNode label : sw.labels) addLabelLeader(leaders, label, labelIndexes, instructions);
                addLeader(leaders, nextExecutable(instructions, i + 1, instructionCount));
            } else if (isTerminator(opcode)) {
                addLeader(leaders, nextExecutable(instructions, i + 1, instructionCount));
            }
        }

        // Exception table boundaries and handler entry points are basic-block leaders.
        for (TryCatchBlockNode tcb : method.tryCatchBlocks) {
            addLabelLeader(leaders, tcb.start, labelIndexes, instructions);
            addLabelLeader(leaders, tcb.end, labelIndexes, instructions);
            addLabelLeader(leaders, tcb.handler, labelIndexes, instructions);
        }

        int[] lineAt = calculateSourceLines(instructions);
        List<Integer> leaderList = new ArrayList<>(leaders);
        List<Block> blocks = new ArrayList<>();
        for (int i = 0; i < leaderList.size(); i++) {
            int start = leaderList.get(i);
            int end = i + 1 < leaderList.size() ? leaderList.get(i + 1) : instructionCount;
            int first = nextExecutable(instructions, start, end);
            int last = previousExecutable(instructions, start, end);
            if (first < 0 || last < 0) continue;
            blocks.add(new Block("N" + blocks.size(), start, end, first, last,
                    lineAt[first], lineAt[last]));
        }
        if (blocks.isEmpty()) return MethodGraph.empty(method.name, method.desc);

        int[] blockForInstruction = new int[instructionCount];
        Arrays.fill(blockForInstruction, -1);
        for (int blockIndex = 0; blockIndex < blocks.size(); blockIndex++) {
            Block block = blocks.get(blockIndex);
            for (int i = block.start; i < block.end; i++) blockForInstruction[i] = blockIndex;
        }

        MethodGraph graph = new MethodGraph(method.name, method.desc);
        graph.blocks.addAll(blocks);
        graph.normalEdges.add(new Edge("ENTRY", blocks.get(0).id, "entry"));

        for (int blockIndex = 0; blockIndex < blocks.size(); blockIndex++) {
            Block block = blocks.get(blockIndex);
            AbstractInsnNode last = instructions[block.lastExecutable];
            int opcode = last.getOpcode();
            String fallThrough = blockIndex + 1 < blocks.size()
                    ? blocks.get(blockIndex + 1).id : "EXIT";

            if (last instanceof JumpInsnNode) {
                JumpInsnNode jump = (JumpInsnNode) last;
                String target = blockForLabel(jump.label, executableLabelIndexes, blockForInstruction, blocks);
                if (isConditionalJump(opcode)) {
                    graph.normalEdges.add(new Edge(block.id, target, "true"));
                    graph.normalEdges.add(new Edge(block.id, fallThrough, "false"));
                    graph.addBranch(block.id, opcodeName(opcode), block.sourceEnd,
                            Arrays.asList(new Outcome("true", target),
                                    new Outcome("false", fallThrough)));
                } else {
                    graph.normalEdges.add(new Edge(block.id, target,
                            opcode == Opcodes.JSR ? "jsr" : "goto"));
                }
            } else if (last instanceof TableSwitchInsnNode) {
                TableSwitchInsnNode sw = (TableSwitchInsnNode) last;
                List<Outcome> outcomes = new ArrayList<>();
                String defaultTarget = blockForLabel(sw.dflt, executableLabelIndexes, blockForInstruction, blocks);
                graph.normalEdges.add(new Edge(block.id, defaultTarget, "default"));
                outcomes.add(new Outcome("default", defaultTarget));
                for (int i = 0; i < sw.labels.size(); i++) {
                    String label = "case:" + (sw.min + i);
                    String target = blockForLabel(sw.labels.get(i), executableLabelIndexes,
                            blockForInstruction, blocks);
                    graph.normalEdges.add(new Edge(block.id, target, label));
                    outcomes.add(new Outcome(label, target));
                }
                graph.addBranch(block.id, "TABLESWITCH", block.sourceEnd, outcomes);
            } else if (last instanceof LookupSwitchInsnNode) {
                LookupSwitchInsnNode sw = (LookupSwitchInsnNode) last;
                List<Outcome> outcomes = new ArrayList<>();
                String defaultTarget = blockForLabel(sw.dflt, executableLabelIndexes, blockForInstruction, blocks);
                graph.normalEdges.add(new Edge(block.id, defaultTarget, "default"));
                outcomes.add(new Outcome("default", defaultTarget));
                for (int i = 0; i < sw.labels.size(); i++) {
                    String label = "case:" + sw.keys.get(i);
                    String target = blockForLabel(sw.labels.get(i), executableLabelIndexes,
                            blockForInstruction, blocks);
                    graph.normalEdges.add(new Edge(block.id, target, label));
                    outcomes.add(new Outcome(label, target));
                }
                graph.addBranch(block.id, "LOOKUPSWITCH", block.sourceEnd, outcomes);
            } else if (isReturn(opcode)) {
                graph.normalEdges.add(new Edge(block.id, "EXIT", "return"));
            } else if (opcode == Opcodes.ATHROW) {
                graph.normalEdges.add(new Edge(block.id, "EXIT", "throw"));
            } else if (opcode == Opcodes.RET) {
                graph.normalEdges.add(new Edge(block.id, "EXIT", "ret_unknown_target"));
            } else if (!"EXIT".equals(fallThrough)) {
                graph.normalEdges.add(new Edge(block.id, fallThrough, "next"));
            } else {
                graph.normalEdges.add(new Edge(block.id, "EXIT", "fallthrough"));
            }
        }

        // Conservative exception flow: every block intersecting a protected range
        // is connected to its handler. These edges do not enter V(G).
        for (TryCatchBlockNode tcb : method.tryCatchBlocks) {
            Integer protectedStart = labelIndexes.get(tcb.start);
            Integer protectedEnd = labelIndexes.get(tcb.end);
            if (protectedStart == null || protectedEnd == null) continue;
            String handler = blockForLabel(tcb.handler, executableLabelIndexes, blockForInstruction, blocks);
            String catchType = tcb.type == null ? "finally_or_any" : tcb.type.replace('/', '.');
            for (Block block : blocks) {
                if (block.start < protectedEnd && block.end > protectedStart) {
                    graph.exceptionEdges.add(new Edge(block.id, handler, catchType));
                }
            }
        }

        calculateReachableCountsAndComplexity(graph);
        enumeratePaths(graph, maxPaths);
        return graph;
    }

    private static int[] calculateSourceLines(AbstractInsnNode[] instructions) {
        int[] lineAt = new int[instructions.length];
        int currentLine = -1;
        for (int i = 0; i < instructions.length; i++) {
            if (instructions[i] instanceof LineNumberNode) {
                currentLine = ((LineNumberNode) instructions[i]).line;
            }
            lineAt[i] = currentLine;
        }
        return lineAt;
    }

    private static void addLabelLeader(Set<Integer> leaders, LabelNode label,
            IdentityHashMap<LabelNode, Integer> indexes, AbstractInsnNode[] instructions) {
        Integer index = indexes.get(label);
        if (index != null) addLeader(leaders, nextExecutable(instructions, index, instructions.length));
    }

    private static void addLeader(Set<Integer> leaders, int index) {
        if (index >= 0) leaders.add(index);
    }

    private static int nextExecutable(AbstractInsnNode[] instructions, int start, int end) {
        for (int i = Math.max(0, start); i < Math.min(end, instructions.length); i++) {
            if (instructions[i].getOpcode() >= 0) return i;
        }
        return -1;
    }

    private static int previousExecutable(AbstractInsnNode[] instructions, int start, int end) {
        for (int i = Math.min(end, instructions.length) - 1; i >= Math.max(0, start); i--) {
            if (instructions[i].getOpcode() >= 0) return i;
        }
        return -1;
    }

    private static String blockForLabel(LabelNode label,
            IdentityHashMap<LabelNode, Integer> executableLabelIndexes,
            int[] blockForInstruction, List<Block> blocks) {
        Integer executable = executableLabelIndexes.get(label);
        if (executable == null || executable < 0 || executable >= blockForInstruction.length) {
            return "EXIT";
        }
        int blockIndex = blockForInstruction[executable];
        return blockIndex >= 0 && blockIndex < blocks.size()
                ? blocks.get(blockIndex).id : "EXIT";
    }

    private static boolean isConditionalJump(int opcode) {
        return (opcode >= Opcodes.IFEQ && opcode <= Opcodes.IF_ACMPNE)
                || opcode == Opcodes.IFNULL || opcode == Opcodes.IFNONNULL;
    }

    private static boolean isReturn(int opcode) {
        return opcode >= Opcodes.IRETURN && opcode <= Opcodes.RETURN;
    }

    private static boolean isTerminator(int opcode) {
        return isReturn(opcode) || opcode == Opcodes.ATHROW
                || opcode == Opcodes.GOTO || opcode == Opcodes.JSR || opcode == Opcodes.RET;
    }

    private static String opcodeName(int opcode) {
        if (opcode >= 0 && opcode < Printer.OPCODES.length) {
            return Printer.OPCODES[opcode];
        }
        return "OPCODE_" + opcode;
    }

    private static void calculateReachableCountsAndComplexity(MethodGraph graph) {
        Map<String, List<Edge>> outgoing = new HashMap<>();
        for (Edge edge : graph.normalEdges) {
            outgoing.computeIfAbsent(edge.from, ignored -> new ArrayList<>()).add(edge);
        }

        Set<String> reachable = new LinkedHashSet<>();
        Deque<String> queue = new ArrayDeque<>();
        queue.add("ENTRY");
        while (!queue.isEmpty()) {
            String current = queue.removeFirst();
            if (!reachable.add(current)) continue;
            for (Edge edge : outgoing.getOrDefault(current, Collections.emptyList())) {
                if (!reachable.contains(edge.to)) queue.addLast(edge.to);
            }
        }

        int reachableEdges = 0;
        for (Edge edge : graph.normalEdges) {
            if (reachable.contains(edge.from) && reachable.contains(edge.to)) reachableEdges++;
        }
        graph.reachableNodes.addAll(reachable);
        graph.nodeCount = reachable.size();
        graph.edgeCount = reachableEdges;
        graph.cyclomatic = Math.max(1, reachableEdges - reachable.size() + 2);
    }

    // ---------------------------------------------------------------------
    // Bounded simple-path enumeration
    // ---------------------------------------------------------------------

    private static void enumeratePaths(MethodGraph graph, int maxPaths) {
        if (maxPaths == 0) {
            graph.pathStatus = "disabled";
            return;
        }
        if (graph.blocks.isEmpty()) {
            graph.pathStatus = "no_basic_blocks";
            return;
        }

        Map<String, List<Edge>> outgoing = new LinkedHashMap<>();
        for (Edge edge : graph.normalEdges) {
            if (!"ENTRY".equals(edge.from)) {
                outgoing.computeIfAbsent(edge.from, ignored -> new ArrayList<>()).add(edge);
            }
        }
        Map<String, String> branchTokens = new HashMap<>();
        for (Branch branch : graph.branches) {
            for (Outcome outcome : branch.outcomes) {
                branchTokens.put(branch.node + "|" + outcome.label,
                        branch.id + ":" + outcome.label);
            }
        }

        PathCollector collector = new PathCollector(maxPaths);
        Deque<String> path = new ArrayDeque<>();
        Set<String> onPath = new HashSet<>();
        String entryBlock = graph.blocks.get(0).id;
        path.addLast(entryBlock);
        onPath.add(entryBlock);
        enumerateDfs(entryBlock, outgoing, branchTokens, path, onPath, collector);

        if (collector.truncated) {
            graph.paths.clear();
            graph.pathStatus = "branch_outcomes_only_limit_exceeded";
        } else {
            graph.paths.addAll(collector.paths);
            graph.pathStatus = "simple_paths_no_loop_unrolling";
        }
    }

    private static void enumerateDfs(String current,
            Map<String, List<Edge>> outgoing,
            Map<String, String> branchTokens,
            Deque<String> path,
            Set<String> onPath,
            PathCollector collector) {
        if (collector.truncated) return;
        List<Edge> edges = outgoing.getOrDefault(current, Collections.emptyList());
        boolean hadExit = false;
        for (Edge edge : edges) {
            if ("EXIT".equals(edge.to)) {
                hadExit = true;
                List<String> completed = new ArrayList<>(path);
                String token = branchTokens.get(edge.from + "|" + edge.label);
                if (token != null) completed.add(token);
                collector.add(completed);
                if (collector.truncated) return;
            } else if (!onPath.contains(edge.to)) {
                String token = branchTokens.get(edge.from + "|" + edge.label);
                if (token != null) path.addLast(token);
                path.addLast(edge.to);
                onPath.add(edge.to);
                enumerateDfs(edge.to, outgoing, branchTokens, path, onPath, collector);
                onPath.remove(edge.to);
                path.removeLast();
                if (token != null) path.removeLast();
                if (collector.truncated) return;
            }
        }
        if (edges.isEmpty() && !hadExit) collector.add(new ArrayList<>(path));
    }

    // ---------------------------------------------------------------------
    // JSON and DOT output
    // ---------------------------------------------------------------------

    private static String renderJson(String className, String sourceFile,
            List<MethodGraph> methods) {
        StringBuilder json = new StringBuilder();
        json.append("{\n");
        json.append("  \"class\": ").append(jsonString(className)).append(",\n");
        json.append("  \"source_file\": ").append(jsonString(sourceFile == null ? "" : sourceFile)).append(",\n");
        json.append("  \"tool\": {\"name\": \"ASM\", \"version\": ")
                .append(jsonString(ASM_VERSION)).append(", \"extractor_version\": ")
                .append(jsonString(TOOL_VERSION)).append("},\n");
        json.append("  \"methods\": [\n");

        for (int mi = 0; mi < methods.size(); mi++) {
            MethodGraph graph = methods.get(mi);
            if (mi > 0) json.append(",\n");
            int[] methodLines = methodSourceRange(graph);
            json.append("    {\n");
            json.append("      \"name\": ").append(jsonString(graph.name)).append(",\n");
            json.append("      \"desc\": ").append(jsonString(graph.descriptor)).append(",\n");
            json.append("      \"source_lines\": [").append(methodLines[0]).append(", ")
                    .append(methodLines[1]).append("],\n");
            json.append("      \"nodes\": ").append(graph.nodeCount).append(",\n");
            json.append("      \"edges\": ").append(graph.edgeCount).append(",\n");
            json.append("      \"cyclomatic\": ").append(graph.cyclomatic).append(",\n");
            json.append("      \"basic_block_count\": ").append(graph.blocks.size()).append(",\n");
            int reachableBlocks = 0;
            for (Block block : graph.blocks) if (graph.reachableNodes.contains(block.id)) reachableBlocks++;
            json.append("      \"reachable_basic_block_count\": ").append(reachableBlocks).append(",\n");
            json.append("      \"basic_blocks\": [\n");
            for (int bi = 0; bi < graph.blocks.size(); bi++) {
                Block block = graph.blocks.get(bi);
                if (bi > 0) json.append(",\n");
                json.append("        {\"id\": ").append(jsonString(block.id))
                        .append(", \"instruction_range\": [").append(block.start).append(", ")
                        .append(block.end - 1).append("], \"source_lines\": [")
                        .append(block.sourceStart).append(", ").append(block.sourceEnd)
                        .append("], \"reachable\": ")
                        .append(graph.reachableNodes.contains(block.id)).append("}");
            }
            json.append("\n      ],\n");

            appendNormalEdges(json, graph);
            json.append(",\n");
            appendBranches(json, graph);
            json.append(",\n");
            appendExceptionEdges(json, graph.exceptionEdges);
            json.append(",\n      \"paths\": [\n");
            for (int pi = 0; pi < graph.paths.size(); pi++) {
                if (pi > 0) json.append(",\n");
                json.append("        ").append(jsonArray(graph.paths.get(pi)));
            }
            json.append("\n      ],\n");
            json.append("      \"path_status\": ").append(jsonString(graph.pathStatus)).append("\n");
            json.append("    }");
        }
        json.append("\n  ]\n}\n");
        return json.toString();
    }

    private static int[] methodSourceRange(MethodGraph graph) {
        int min = Integer.MAX_VALUE;
        int max = -1;
        for (Block block : graph.blocks) {
            if (block.sourceStart >= 0) min = Math.min(min, block.sourceStart);
            if (block.sourceEnd >= 0) max = Math.max(max, block.sourceEnd);
        }
        if (min == Integer.MAX_VALUE) return new int[]{-1, -1};
        return new int[]{min, Math.max(min, max)};
    }

    private static void appendNormalEdges(StringBuilder json, MethodGraph graph) {
        json.append("      \"edges\": [\n");
        boolean first = true;
        for (Edge edge : graph.normalEdges) {
            if (!graph.reachableNodes.contains(edge.from) || !graph.reachableNodes.contains(edge.to)) {
                continue;
            }
            if (!first) json.append(",\n");
            first = false;
            json.append("        {\"from\": ").append(jsonString(edge.from))
                    .append(", \"to\": ").append(jsonString(edge.to))
                    .append(", \"kind\": ").append(jsonString(edge.label)).append("}");
        }
        json.append("\n      ]");
    }

    private static void appendBranches(StringBuilder json, MethodGraph graph) {
        json.append("      \"branches\": [\n");
        boolean first = true;
        for (Branch branch : graph.branches) {
            if (!graph.reachableNodes.contains(branch.node)) continue;
            if (!first) json.append(",\n");
            first = false;
            json.append("        {\"id\": ").append(jsonString(branch.id))
                    .append(", \"line\": ").append(branch.line)
                    .append(", \"cond\": ").append(jsonString("bytecode: " + branch.condition));
            if (branch.trueTo != null) {
                json.append(", \"true_to\": ").append(jsonString(branch.trueTo))
                        .append(", \"false_to\": ").append(jsonString(branch.falseTo));
            }
            json.append(", \"node\": ").append(jsonString(branch.node))
                    .append(", \"outcomes\": [");
            for (int oi = 0; oi < branch.outcomes.size(); oi++) {
                Outcome outcome = branch.outcomes.get(oi);
                if (oi > 0) json.append(", ");
                json.append("{\"label\": ").append(jsonString(outcome.label))
                        .append(", \"to\": ").append(jsonString(outcome.target)).append("}");
            }
            json.append("]}");
        }
        json.append("\n      ]");
    }

    private static void appendExceptionEdges(StringBuilder json, List<Edge> edges) {
        json.append("      \"exception_edges\": [\n");
        for (int i = 0; i < edges.size(); i++) {
            Edge edge = edges.get(i);
            if (i > 0) json.append(",\n");
            json.append("        {\"from\": ").append(jsonString(edge.from))
                    .append(", \"to\": ").append(jsonString(edge.to))
                    .append(", \"catch_type\": ").append(jsonString(edge.label)).append("}");
        }
        json.append("\n      ]");
    }

    private static String renderDot(String className, List<MethodGraph> methods) {
        StringBuilder dot = new StringBuilder();
        dot.append("digraph CFG {\n")
                .append("  graph [rankdir=TB, compound=true];\n")
                .append("  node [fontname=\"Arial\"];\n")
                .append("  edge [fontname=\"Arial\", fontsize=9];\n")
                .append("  label=").append(dotString(className)).append(";\n")
                .append("  labelloc=top;\n");

        for (MethodGraph graph : methods) {
            String prefix = safeIdentifier(graph.name + graph.descriptor);
            dot.append("  subgraph cluster_").append(prefix).append(" {\n")
                    .append("    label=").append(dotString(graph.name + graph.descriptor)).append(";\n")
                    .append("    color=gray;\n");
            dot.append("    ").append(dotNode(prefix, "ENTRY"))
                    .append(" [shape=Mdiamond, label=\"ENTRY\"];\n");
            dot.append("    ").append(dotNode(prefix, "EXIT"))
                    .append(" [shape=Msquare, label=\"EXIT\"];\n");
            for (Block block : graph.blocks) {
                String label = block.id + "\nlines " + block.sourceStart + "-" + block.sourceEnd;
                dot.append("    ").append(dotNode(prefix, block.id))
                        .append(" [shape=box, label=").append(dotString(label)).append("];\n");
            }
            for (Edge edge : graph.normalEdges) {
                dot.append("    ").append(dotNode(prefix, edge.from)).append(" -> ")
                        .append(dotNode(prefix, edge.to))
                        .append(" [label=").append(dotString(edge.label)).append("];\n");
            }
            for (Edge edge : graph.exceptionEdges) {
                dot.append("    ").append(dotNode(prefix, edge.from)).append(" -> ")
                        .append(dotNode(prefix, edge.to))
                        .append(" [style=dashed, color=red, label=")
                        .append(dotString("exception: " + edge.label)).append("];\n");
            }
            dot.append("  }\n");
        }
        dot.append("}\n");
        return dot.toString();
    }

    private static String packagePath(String className) {
        int dot = className.lastIndexOf('.');
        return dot < 0 ? "" : className.substring(0, dot).replace('.', '/');
    }

    private static String simpleClassName(String className) {
        int dot = className.lastIndexOf('.');
        return dot < 0 ? className : className.substring(dot + 1);
    }

    private static String dotNode(String prefix, String node) {
        return prefix + "_" + safeIdentifier(node);
    }

    private static String safeIdentifier(String value) {
        return value.replaceAll("[^A-Za-z0-9_]", "_");
    }

    private static String jsonString(String value) {
        if (value == null) return "null";
        return "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"")
                .replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t") + "\"";
    }

    private static String dotString(String value) {
        return jsonString(value);
    }

    private static String jsonArray(List<String> values) {
        StringBuilder json = new StringBuilder("[");
        for (int i = 0; i < values.size(); i++) {
            if (i > 0) json.append(", ");
            json.append(jsonString(values.get(i)));
        }
        return json.append(']').toString();
    }

    private static void printUsage() {
        System.out.println("CFG_Extractor " + TOOL_VERSION + " (ASM " + ASM_VERSION + ")");
        System.out.println();
        System.out.println("Preferred two-input interface:");
        System.out.println("  tools.CFG_Extractor file   net.sf.freecol.common.model.mission.CompoundMission");
        System.out.println("  tools.CFG_Extractor config high_priority_files.csv");
        System.out.println();
        System.out.println("Interactive interface:");
        System.out.println("  Run tools.CFG_Extractor with no arguments, then enter:");
        System.out.println("    1) file or config (or 1 / 2)");
        System.out.println("    2) FQCN (e.g. net.sf.freecol.common.model.mission.CompoundMission) or CSV name (e.g. high_priority_files.csv)");
        System.out.println();
        System.out.println("Default directories resolved automatically:");
        System.out.println("  compiled classes: out/production/freecol-master (fallback: test_cft/target/classes)");
        System.out.println("  config directory: test_cft/config");
        System.out.println("  CFG output      : test_cft/graph");
    }

    // ---------------------------------------------------------------------
    // Small value classes
    // ---------------------------------------------------------------------

    private static final class Block {
        final String id;
        final int start;
        final int end;
        final int firstExecutable;
        final int lastExecutable;
        final int sourceStart;
        final int sourceEnd;

        Block(String id, int start, int end, int firstExecutable, int lastExecutable,
                int sourceStart, int sourceEnd) {
            this.id = id;
            this.start = start;
            this.end = end;
            this.firstExecutable = firstExecutable;
            this.lastExecutable = lastExecutable;
            this.sourceStart = sourceStart;
            this.sourceEnd = sourceEnd;
        }
    }

    private static final class Edge {
        final String from;
        final String to;
        final String label;

        Edge(String from, String to, String label) {
            this.from = from;
            this.to = to;
            this.label = label;
        }
    }

    private static final class Outcome {
        final String label;
        final String target;

        Outcome(String label, String target) {
            this.label = label;
            this.target = target;
        }
    }

    private static final class Branch {
        final String id;
        final String node;
        final String condition;
        final int line;
        final String trueTo;
        final String falseTo;
        final List<Outcome> outcomes;

        Branch(String id, String node, String condition, int line,
                String trueTo, String falseTo, List<Outcome> outcomes) {
            this.id = id;
            this.node = node;
            this.condition = condition;
            this.line = line;
            this.trueTo = trueTo;
            this.falseTo = falseTo;
            this.outcomes = outcomes;
        }
    }

    private static final class MethodGraph {
        final String name;
        final String descriptor;
        final List<Block> blocks = new ArrayList<>();
        final List<Edge> normalEdges = new ArrayList<>();
        final List<Edge> exceptionEdges = new ArrayList<>();
        final List<Branch> branches = new ArrayList<>();
        final Set<String> reachableNodes = new HashSet<>();
        final List<List<String>> paths = new ArrayList<>();
        int nodeCount;
        int edgeCount;
        int cyclomatic = 1;
        String pathStatus = "not_computed";

        MethodGraph(String name, String descriptor) {
            this.name = name;
            this.descriptor = descriptor;
        }

        static MethodGraph empty(String name, String descriptor) {
            MethodGraph graph = new MethodGraph(name, descriptor);
            graph.pathStatus = "empty_method";
            return graph;
        }

        void addBranch(String node, String condition, int line, List<Outcome> outcomes) {
            String id = "B" + (branches.size() + 1);
            String trueTo = null;
            String falseTo = null;
            if (outcomes.size() == 2
                    && "true".equals(outcomes.get(0).label)
                    && "false".equals(outcomes.get(1).label)) {
                trueTo = outcomes.get(0).target;
                falseTo = outcomes.get(1).target;
            }
            branches.add(new Branch(id, node, condition, line, trueTo, falseTo, outcomes));
        }
    }

    private static final class PathCollector {
        final int maximum;
        final List<List<String>> paths = new ArrayList<>();
        boolean truncated;

        PathCollector(int maximum) {
            this.maximum = maximum;
        }

        void add(List<String> path) {
            if (paths.size() >= maximum) {
                truncated = true;
                return;
            }
            paths.add(path);
        }
    }

    private static final class ResolvedClassInput {
        final Path classFile;
        final String className;

        ResolvedClassInput(Path classFile, String className) {
            this.classFile = classFile;
            this.className = className;
        }
    }

    private static final class ProjectPaths {
        final Path repoRoot;
        final Path testCftRoot;
        final Path classesDirectory;
        final List<Path> candidateClassDirectories;
        final Path configDirectory;
        final Path graphDirectory;

        ProjectPaths(Path repoRoot, Path testCftRoot, Path classesDirectory,
                List<Path> candidateClassDirectories, Path configDirectory, Path graphDirectory) {
            this.repoRoot = repoRoot;
            this.testCftRoot = testCftRoot;
            this.classesDirectory = classesDirectory;
            this.candidateClassDirectories = candidateClassDirectories;
            this.configDirectory = configDirectory;
            this.graphDirectory = graphDirectory;
        }
    }

    private static final class ClassTarget {
        final String className;
        final int configLine;

        ClassTarget(String className, int configLine) {
            this.className = className;
            this.configLine = configLine;
        }
    }

    private static final class BatchResult {
        final String className;
        final String status;
        final String detail;

        BatchResult(String className, String status, String detail) {
            this.className = className;
            this.status = status;
            this.detail = detail;
        }
    }

    private static final class ExtractionResult {
        final String className;
        final int methodCount;
        final Path jsonFile;
        final Path dotFile;
        final List<MethodGraph> methods;

        ExtractionResult(String className, int methodCount, Path jsonFile, Path dotFile,
                List<MethodGraph> methods) {
            this.className = className;
            this.methodCount = methodCount;
            this.jsonFile = jsonFile;
            this.dotFile = dotFile;
            this.methods = methods;
        }
    }
}
