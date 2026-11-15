# `test_cft` — CFG-Driven Testing & JaCoCo Coverage Guide

> [!NOTE]
> **Status: hand-off specification.** `test_cft` is now a standard **Maven module**. The build has **not been executed yet**; expect small first-run adjustments (see §7).

## 1. Overview & scope

Build a **brand-new JUnit 4 suite** for FreeCol's **High-priority** code using Control-Flow Graphs, and measure it with **JaCoCo**.

- **In scope**: 231 files / 4,726 public methods / 126,485 LoC — list: [`config/high_priority_files.csv`](config/high_priority_files.csv), roots: [`config/high_priority_packages.txt`](config/high_priority_packages.txt).
- **Out of scope**: all Medium/Low modules and the legacy tests in `../test/`.
- Strategy & statistics: [`../test_docs/README.md`](../test_docs/README.md).

```
Stage A: extract CFG  ->  Stage B: LLM writes tests  ->  Stage C: mvn verify (run + JaCoCo)
                                      ^                              |
                                      +------ uncovered branches ----+
```

## 2. Source-root layout (the important part)

There are exactly **three source roots**. Any `.java` file outside them is **not compiled**.

| Role | Path | Content | Edit? |
|---|---|---|---|
| **Main** source root | `../src` (repo root `src/`) | FreeCol production code | **Never** |
| **Test** source root | `test_cft/src/test/java` | generated tests (`*CfgTest.java`) + copied fixtures | Yes |
| **Test** source root (extra) | `test_cft/src/tools/java` | CFG-extractor code (ASM/Soot, Stage A) | Yes |
| Test resources | `test_cft/src/test/resources` | extra test data if needed | Yes |

```text
freecol-master/
├─ src/                                  # MAIN root (production code, read-only here)
├─ data/  test/data/                     # runtime data; tests run with working dir = repo root
└─ test_cft/
   ├─ pom.xml                            # THE build definition (source roots, deps, surefire, JaCoCo)
   ├─ README.md
   ├─ config/
   │   ├─ high_priority_files.csv
   │   └─ high_priority_packages.txt     # keep in sync with <includes> of the JaCoCo report in pom.xml
   ├─ src/
   │   ├─ test/java/net/sf/freecol/...   # TEST root: package == package of the class under test
   │   │    ├─ common/model/ColonyCfgTest.java        (example name)
   │   │    ├─ util/test/ FreeColTestCase, FreeColTestUtils, MockMapGenerator, MockPseudoRandom  (copied fixtures)
   │   │    ├─ client/ClientTestHelper.java           (copied fixture)
   │   │    └─ server/ServerTestHelper.java           (copied fixture)
   │   ├─ test/resources/
   │   └─ tools/java/                    # CFG extractor sources (package e.g. cft.tool)
   ├─ graph/                             # Stage A output: <package>/<Class>.json (+ .dot)
   ├─ scripts/  compile.cmd  run.cmd  jacoco.cmd  resolve_subset_tests.ps1
   ├─ reports/                           # (generated) surefire/, jacoco/, and subsets/<csv_name>/
   └─ target/                            # (generated) Maven output
```

**Conventions**
1. **Same package as the class under test** (`net.sf.freecol.common.model.ColonyCfgTest` tests `...common.model.Colony`) so package-private members are reachable.
2. File name must end in **`CfgTest.java`** or **`CFTTest.java`** — surefire runs `**/*CfgTest.java` and `**/*CFTTest.java` (the copied fixtures are therefore not executed).
3. Tests extend `FreeColTestCase` when a game/spec is needed; legacy `*Test` cases are never used.
4. Do not put sources under `graph/`, `reports/` or `config/`.

## 3. Prerequisites

| Need | Version |
|---|---|
| JDK (with `javac`) | 11+ (21 verified present on the source machine; compiled with `release 11`) |
| Maven | 3.x (3.9.16 on the source machine) |
| Network | once, for Maven to download plugins/dependencies |

Ant is **not** needed. FreeCol's runtime jars come from `../jars/` (declared in `pom.xml` as `system` dependencies). Everything else (JUnit 4.13.2, ASM 9.7, Soot 4.5.0, JaCoCo 0.8.12, surefire 3.2.5, compiler plugin 3.13.0) is pinned in `pom.xml` and downloaded by Maven automatically — **no manual download step**.

## 4. IntelliJ IDEA setup (fixes "not in source root")

1. Open the **repository root** (`freecol-master`) as the project.
2. Right-click `test_cft/pom.xml` → **Add as Maven Project** (or Maven tool window → **+**). IntelliJ then marks `../src` as *Sources*, `src/test/java` and `src/tools/java` as *Test Sources*, `src/test/resources` as *Test Resources* automatically.
3. Reload Maven (🔄). Use JDK 11+ for the project SDK.
4. Fallback if you do not use Maven import: *Project Structure → Modules → Add content root `test_cft`* → mark `src/test/java` and `src/tools/java` as **Test Sources**, `src/test/resources` as **Test Resources**; add `junit-4.13.2` and the jars in `jars/` to the module dependencies.
5. When running a test from the IDE, set **Working directory = repository root** (otherwise `data/...` is not found).

## 5. Commands (from anywhere; Windows `cmd`/PowerShell)

| Goal | Command |
|---|---|
| Check the setup (compile main + tests, no run) | `test_cft\scripts\compile.cmd` |
| Run all `*CfgTest` & `*CFTTest` + JaCoCo report (`common` only) | `test_cft\scripts\jacoco.cmd` |
| Measure coverage for a CSV subset (explicit flag + Maven opts) | `test_cft\scripts\jacoco.cmd -csv config\suong_high_priority_files.csv -o` |
| Run one or more specific test classes + JaCoCo report | `test_cft\scripts\jacoco.cmd -Dtest=CompoundMissionCfgTest` |
| Run all tests via `run.cmd` | `test_cft\scripts\run.cmd` |
| Raw Maven equivalent | `mvn -f test_cft/pom.xml clean verify` |

### JaCoCo Coverage Measurement (`scripts\jacoco.cmd`)

- **Report scope (`net.sf.freecol.common.**` only):** `pom.xml` restricts both the JaCoCo agent and `jacoco:report` to `net/sf/freecol/common/**`. Packages outside `common` (`client`, `server`, `metaserver`, `tools`, and root `net.sf.freecol`) are excluded from the statistics table.
- **Subset measurement by CSV (measuring individual contributions):**
  1. Create a CSV listing the units assigned to a member (e.g. [`config/suong_high_priority_files.csv`](config/suong_high_priority_files.csv)). Supported formats:
     - Standard config CSV with `package,file,class_name,...` header (like `high_priority_files.csv` / `suong_high_priority_files.csv`; leading blank lines and `#` comments are ignored).
     - Simple headerless list with one class name, `.java` path, or test class name per line.
  2. Run `test_cft\scripts\jacoco.cmd <path-to-csv>` (accepts an absolute path, repo-relative path, `test_cft`-relative path, or a filename inside `test_cft\config\`).
  3. `scripts\resolve_subset_tests.ps1` matches each unit `<ClassName>` to its existing test files (`<ClassName>CfgTest.java`, `<ClassName>CFTTest.java`, or `<ClassName>Test.java` under `test_cft\src\test\java`) while preventing cross-package collisions (e.g. `client` vs `server`), prints matched/missing units, and passes `-Dtest=...` to Surefire.
  4. JaCoCo runs **only those subset tests** and reports coverage measured across **all of `net.sf.freecol.common`**. Failed tests do not abort report generation (`-Dmaven.test.failure.ignore=true`).
- **Outputs:**
  - `test_cft/reports/surefire/` — JUnit test results.
  - `test_cft/reports/jacoco/index.html`, `jacoco.xml`, `jacoco.csv` — latest JaCoCo report (restricted to `net.sf.freecol.common.**`).
  - `test_cft/reports/jacoco/common_summary.csv` — per-subpackage and total `common` coverage summary (also printed to the console).
  - `test_cft/reports/subsets/<csv_name>/` — preserved copy of the full JaCoCo report + `common_summary.csv` for each CSV subset run, so team members' results are not overwritten.

## 6. Pipeline guide

### Stage A — Extract CFGs (tool, no LLM)
- Code lives in `src/tools/java` (test-scope on the classpath: ASM 9.7 default, Soot optional). Input: compiled classes in `test_cft/target/classes`, filtered to `config/high_priority_files.csv`. JaCoCo does **not** export CFGs.
- Output `graph/<package_path>/<Class>.json` per class:

```json
{ "class": "net.sf.freecol.common.model.Colony",
  "methods": [{
    "name": "getUnitCount", "desc": "()I", "source_lines": [410, 428],
    "nodes": 7, "edges": 8, "cyclomatic": 3,
    "branches": [{"id": "B1", "line": 412, "cond": "units == null", "true_to": "N5", "false_to": "N2"}],
    "paths": [["N0","N1","B1:true","N5"], ["N0","N1","B1:false","N2","N3","N5"]] }] }
```
- Rules: `cyclomatic = E − N + 2`, cross-check with JaCoCo's `COMPLEXITY`; cap paths per method (~50) then target branch outcomes; **never paste raw bytecode/DOT into prompts** — give the LLM source lines + branch conditions + enumerated paths. Optional (budget): LLM-drafted CFG, accepted only if schema-valid and `cyclomatic` matches.

### Stage B — LLM generates tests
- Waves ([`../test_docs/README.md` §4](../test_docs/README.md)): `common/util` + `common/model/{pathfinding,production,mission}` → `common/model` → `server/model`+`server/control` → `server/ai/**` → `client/control`+`FreeCol`. Within a class, highest `cyclomatic` first.
- Prompt: class + method source, CFG entry, collaborator signatures, fixtures (`FreeColTestCase`, `MockMapGenerator`, `MockPseudoRandom`, `ServerTestHelper`, `ClientTestHelper`).
- Output: `src/test/java/net/sf/freecol/<package>/<Class>CfgTest.java`, each test marked `// covers B1:true`.
- Gates: compiles (`compile.cmd`), passes on current code (characterization), ≤ 2 repair rounds, else delete the failing test and list the class in `test_cft/needs_manual.txt`.

### Stage C — Measure & feed back
Run `run.cmd`; read `reports/jacoco/jacoco.xml`; for methods with missed branches (`<counter type="BRANCH" missed>0`) re-run Stage B once for those branches only.

## 7. Known unknowns (first run)
- Build never executed: the `system`-scope jars, `../src` as `sourceDirectory`, and JaCoCo/surefire wiring may need small fixes; record them in `CHANGES.md`.
- Whether all 785 sources compile under JDK 21 with `release 11`.
- `ServerTestHelper` reads `test/data/test.fsg` and `FreeColTestCase` reads `data/maps/*.fsm` — both rely on working dir = repo root (set in surefire).
- IntelliJ may warn that `../src` lies outside the module content root; it is harmless (the project already owns `src`).

## 8. Acceptance & deliverables
| Metric (High packages only) | Target |
|---|---:|
| Line coverage | ≥ 80 % |
| Branch coverage | ≥ 70 % |
| Method coverage | ≥ 90 % |
| Generated tests compiling & passing | ≥ 95 % |

Hand back: `graph/**` (+ tool/version note), `src/test/java/**`, `needs_manual.txt`, `reports/**` with a per-package summary (files, methods, Σ cyclomatic, tests, pass rate, line/branch/method coverage, LLM tokens), and `CHANGES.md` for any deviation.
