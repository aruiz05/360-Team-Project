# CSE 360 Lessons Learned — TP3

The final entry point is `applicationMain.TP3`. This branch builds on `hw3` and preserves the Foundations JavaFX MVC and persistent H2 design. See [the final integration report](docs/tp3/Integration%20Report.md) for implemented requirements, executed checks and outstanding acceptance evidence. Earlier TP3 increment reports are historical snapshots.

## Eclipse launch

1. Import this directory as an existing Eclipse Java project. Set a JDK 25 execution environment.
2. Configure the project's JavaFX user library and H2 user library for your machine. `.classpath` retains the inherited JavaFX library name; verification used JavaFX 25.0.1 and H2 2.3.232.
3. Run `src/applicationMain/TP3.java` as a Java Application with JavaFX on the module path. VM options, if needed: `--add-modules javafx.controls,javafx.fxml --enable-native-access=javafx.graphics`.
4. For safe manual tests, set `-Duser.home=/absolute/path/to/a/new/manual-test-home` in that launch. The application otherwise uses `jdbc:h2:~/FoundationDatabase`. Reuse that isolated home to verify persistence.
5. For live AI, set `OPENAI_API_KEY` privately in the launch environment. `OPENAI_MODEL` is optional. The provider is an implementation choice; credentials must never enter source or screenshots. Without configuration, AI reports an error rather than a fabricated response.

Detailed role-by-role inputs and expected results are in [Manual Tests](docs/tp3/Manual%20Tests.md). The production Eclipse source folder remains `src`; JUnit code under `test` is compiled by the script below without adding JUnit to the production module.

## Automated verification

Set absolute paths for your installed dependencies, then run from this directory:

```bash
export TP3_JDK=/absolute/path/to/jdk-25
export TP3_JAVAFX_LIB=/absolute/path/to/javafx-sdk/lib
export TP3_H2_JAR=/absolute/path/to/h2.jar
export TP3_JUNIT_JAR=/absolute/path/to/junit-platform-console-standalone.jar
# Optional on headless Linux only:
export TP3_MONOCLE_JAR=/absolute/path/to/openjfx-monocle.jar
export TP3_TEST_OUTPUT=/absolute/path/to/a/new/test-output
bash scripts/test_tp3_foundations.sh
export TP3_TEST_CLASSES="$TP3_TEST_OUTPUT/classes"
bash scripts/generate_tp3_javadocs.sh /absolute/path/to/a/new/javadoc-output
```

Each output directory must be fresh; scripts refuse to overwrite an existing build. The runner compiles the named application module, runs nine inherited regression suites, six JUnit classes, a named-module startup smoke and four JavaFX workflow suites. Databases and GUI homes are isolated from production data. See logs and JUnit XML under the requested output directory. AI tests use an explicitly synthetic loopback HTTP fixture; they do not establish live-provider acceptance.

With a privately configured key, run `bash scripts/verify_live_ai.sh` separately. Record its actual outcome; a passing fixture run never substitutes for this step.

## Documents and packaging

Markdown sources and actual class diagrams are in `docs/tp3`. Generate required PDFs with Python 3, ReportLab, Pillow and Graphviz:

```bash
python3 scripts/build_tp3_documents.py /absolute/path/to/new/pdf-output
python3 scripts/package_tp3.py \
  --pdfs /absolute/path/to/pdf-output \
  --javadoc /absolute/path/to/javadoc-output \
  --evidence "$TP3_TEST_OUTPUT" \
  --output /absolute/path/to/new/package-output
```

The package contains the required code/documentation subset plus an explicit readiness checklist. It is a review candidate until real-provider acceptance, native manual checks, team planning records, team norms, recordings and submission obligations are complete. Packaging does not invent team collaboration evidence or certify a Canvas submission.
