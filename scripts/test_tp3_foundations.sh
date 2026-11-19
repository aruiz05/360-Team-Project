#!/usr/bin/env bash
# Compile the modular application, then run isolated classpath tests without adding JUnit to
# the production module. See TP3_FOUNDATIONS.md for the dependency paths and Eclipse setup.
set -euo pipefail
: "${TP3_JDK:?Set TP3_JDK to a JDK 25 installation}"
: "${TP3_JAVAFX_LIB:?Set TP3_JAVAFX_LIB to the JavaFX lib directory}"
: "${TP3_H2_JAR:?Set TP3_H2_JAR to the H2 jar}"
: "${TP3_JUNIT_JAR:?Set TP3_JUNIT_JAR to the JUnit Platform standalone jar}"
project_root=$(cd "$(dirname "$0")/.." && pwd)
output_root=${TP3_TEST_OUTPUT:-$(mktemp -d "${TMPDIR:-/tmp}/tp3-foundations.XXXXXX")}
mkdir -p "$output_root"
if [[ -e "$output_root/classes" ]]; then
    echo "Choose a new TP3_TEST_OUTPUT directory; existing test output will not be overwritten." >&2
    exit 1
fi
mkdir -p "$output_root/classes" "$output_root/module" "$output_root/logs" "$output_root/gui-home"
production_sources=()
all_sources=()
while IFS= read -r source; do
    production_sources+=("$source")
    [[ "$source" == */module-info.java ]] || all_sources+=("$source")
done < <(find "$project_root/src" -name '*.java' -print | sort)
while IFS= read -r source; do all_sources+=("$source"); done < <(find "$project_root/test" -name '*.java' -print | sort)
"$TP3_JDK/bin/javac" --module-path "$TP3_JAVAFX_LIB" --add-modules javafx.controls,javafx.fxml \
    -cp "$TP3_H2_JAR" -d "$output_root/module" "${production_sources[@]}" \
    > "$output_root/logs/module-compile.log" 2>&1
"$TP3_JDK/bin/javac" -cp "$TP3_JAVAFX_LIB/*:$TP3_H2_JAR:$TP3_JUNIT_JAR" \
    -d "$output_root/classes" "${all_sources[@]}" > "$output_root/logs/test-compile.log" 2>&1
cp "$project_root/src/applicationMain/application.css" "$output_root/classes/applicationMain/"
classpath="$output_root/classes:$TP3_H2_JAR"
regressions=(
    database.OneTimePasswordDatabaseTestingAutomation
    database.DeleteUserDatabaseTestingAutomation
    database.ListUsersDatabaseTestingAutomation
    database.AddRemoveRolesDatabaseTestingAutomation
    guiAddRemoveRoles.AddRemoveRolesTestingAutomation
    database.HW2TestingAutomation
    database.TP2BoundaryCoverageTestingAutomation
    lessonManagement.ExperienceLockPolicyTestingAutomation
    inputValidation.PasswordEvaluatorTestingAutomation
)
for test in "${regressions[@]}"; do
    "$TP3_JDK/bin/java" -cp "$classpath" "$test" > "$output_root/logs/$test.log" 2>&1
    echo "PASS $test"
done
"$TP3_JDK/bin/java" -Dtp3.test.classpath="$classpath" -jar "$TP3_JUNIT_JAR" execute \
    --class-path "$classpath" --select-class database.TP3FoundationsTest --select-class database.TP3DiscoveryTest --select-class database.TP3StatisticsTest --select-class database.AiAssistanceTest --select-class aiAssistance.OpenAiClientTest --select-class database.TP3FinalIntegrationTest --details tree \
    --disable-ansi-colors --fail-if-no-tests --reports-dir "$output_root/junit" \
    > "$output_root/logs/junit.log" 2>&1
echo "PASS database.TP3FoundationsTest, database.TP3DiscoveryTest, database.TP3StatisticsTest, database.AiAssistanceTest, aiAssistance.OpenAiClientTest and database.TP3FinalIntegrationTest"
mkdir -p "$output_root/modular-smoke-classes" "$output_root/modular-home"
"$TP3_JDK/bin/javac" --module-path "$output_root/module:$TP3_JAVAFX_LIB" --add-modules FoundationsF26 \
    --add-exports FoundationsF26/applicationMain=ALL-UNNAMED --add-exports FoundationsF26/database=ALL-UNNAMED \
    -d "$output_root/modular-smoke-classes" "$project_root/test/integration/TP3ModularSmoke.java"
module_options=()
if [[ -n "${TP3_MONOCLE_JAR:-}" ]]; then
    module_options+=(--patch-module "javafx.graphics=$TP3_MONOCLE_JAR"
        -Dglass.platform=Monocle -Dmonocle.platform=Headless -Dprism.order=sw
        -Dheadless.geometry=1600x1000-32 -Dquantum.multithreaded=false)
fi
"$TP3_JDK/bin/java" --module-path "$output_root/module:$TP3_JAVAFX_LIB" --add-modules FoundationsF26 \
    --add-exports FoundationsF26/applicationMain=ALL-UNNAMED --add-exports FoundationsF26/database=ALL-UNNAMED \
    --enable-native-access=javafx.graphics "${module_options[@]}" -Duser.home="$output_root/modular-home" \
    -cp "$output_root/modular-smoke-classes:$TP3_H2_JAR" integration.TP3ModularSmoke > "$output_root/logs/modular-launch.log" 2>&1
echo "PASS integration.TP3ModularSmoke"
gui_options=()
gui_classpath="$classpath:$TP3_JAVAFX_LIB/*"
if [[ -n "${TP3_MONOCLE_JAR:-}" ]]; then
    gui_options+=(-Dglass.platform=Monocle -Dmonocle.platform=Headless -Dprism.order=sw -Dheadless.geometry=1600x1000-32 -Dquantum.multithreaded=false)
    gui_classpath="$gui_classpath:$TP3_MONOCLE_JAR"
fi
"$TP3_JDK/bin/java" --enable-native-access=ALL-UNNAMED \
    -Duser.home="$output_root/gui-home" -Dtp3.isolated.gui.test=true "${gui_options[@]}" \
    -cp "$gui_classpath" guiTools.TP3RoleNavigationTestingAutomation > "$output_root/logs/gui.log" 2>&1
echo "PASS guiTools.TP3RoleNavigationTestingAutomation"
mkdir -p "$output_root/discovery-gui-home"
"$TP3_JDK/bin/java" --enable-native-access=ALL-UNNAMED \
    -Duser.home="$output_root/discovery-gui-home" -Dtp3.isolated.gui.test=true "${gui_options[@]}" \
    -cp "$gui_classpath" guiTools.TP3DiscoveryGuiTestingAutomation > "$output_root/logs/discovery-gui.log" 2>&1
echo "PASS guiTools.TP3DiscoveryGuiTestingAutomation"
mkdir -p "$output_root/statistics-gui-home"
"$TP3_JDK/bin/java" --enable-native-access=ALL-UNNAMED \
    -Duser.home="$output_root/statistics-gui-home" -Dtp3.isolated.gui.test=true "${gui_options[@]}" \
    -cp "$gui_classpath" guiTools.TP3StatisticsGuiTestingAutomation > "$output_root/logs/statistics-gui.log" 2>&1
echo "PASS guiTools.TP3StatisticsGuiTestingAutomation"
mkdir -p "$output_root/ai-gui-home"
"$TP3_JDK/bin/java" --enable-native-access=ALL-UNNAMED \
    -Duser.home="$output_root/ai-gui-home" -Dtp3.isolated.gui.test=true "${gui_options[@]}" \
    -cp "$gui_classpath" guiTools.TP3AiGuiTestingAutomation > "$output_root/logs/ai-gui.log" 2>&1
echo "PASS guiTools.TP3AiGuiTestingAutomation (explicit loopback provider fixture)"
# An asynchronous renderer error may not change the Java process exit code.
for gui_log in "$output_root/logs/modular-launch.log" "$output_root/logs/gui.log" "$output_root/logs/discovery-gui.log" "$output_root/logs/statistics-gui.log" "$output_root/logs/ai-gui.log"; do
    while IFS= read -r line; do
        if [[ "$line" =~ ^[a-zA-Z_][a-zA-Z0-9_$.]*(Exception|Error)(:|$) ]]; then
            echo "GUI runtime error in $gui_log: $line" >&2
            exit 1
        fi
    done < "$gui_log"
done
echo "All automated suites passed. Live AI verification requires scripts/verify_live_ai.sh. Logs: $output_root"
