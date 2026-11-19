#!/usr/bin/env bash
# Generate the Phase 3 application and test Javadoc sets using the tested build dependencies.
set -euo pipefail
: "${TP3_JDK:?Set TP3_JDK}"
: "${TP3_JAVAFX_LIB:?Set TP3_JAVAFX_LIB}"
: "${TP3_H2_JAR:?Set TP3_H2_JAR}"
: "${TP3_JUNIT_JAR:?Set TP3_JUNIT_JAR}"
: "${TP3_TEST_CLASSES:?Set TP3_TEST_CLASSES to the classes directory from the test runner}"
if [[ $# != 1 ]]; then echo "Usage: $0 <new-output-folder>" >&2; exit 1; fi
project_root=$(cd "$(dirname "$0")/.." && pwd)
output_root=$1
for name in 'New Code Javadoc' 'Test Code Javadoc'; do
    if [[ -e "$output_root/$name" ]]; then echo "Output already exists: $output_root/$name" >&2; exit 1; fi
done
mkdir -p "$output_root"
production_sources=()
for relative in database/Database.java entityClasses/User.java entityClasses/SearchTerm.java entityClasses/StatisticsValues.java entityClasses/StatisticsRequest.java \
    entityClasses/StatisticsPreview.java entityClasses/StatisticsSummary.java entityClasses/StatisticsSourceSnapshot.java entityClasses/SharedStatistics.java entityClasses/ViewerLesson.java guiTools/RoleNames.java \
    guiTools/GUISingleRoleDispatch.java lessonManagement/LessonService.java lessonManagement/DiscoveryAccess.java lessonManagement/StatisticsCalculator.java lessonManagement/StatisticsService.java \
    guiContributorLessonDetails/ViewContributorLessonDetails.java guiContributorLessons/ViewContributorLessons.java \
    lessonManagement/ViewerLessonService.java lessonManagement/CuratorLessonService.java lessonManagement/LessonLockState.java \
    guiAddRemoveRoles/ControllerAddRemoveRoles.java guiAddRemoveRoles/ViewAddRemoveRoles.java \
    guiAdminHome/ControllerAdminHome.java guiAdminHome/ViewAdminHome.java \
    guiListUsers/ViewListUsers.java guiMultipleRoleDispatch/ControllerMultipleRoleDispatch.java \
    guiMultipleRoleDispatch/ViewMultipleRoleDispatch.java guiNewAccount/ControllerNewAccount.java \
    guiRole1/ViewRole1Home.java guiUserLogin/ControllerUserLogin.java \
    guiUserUpdate/ControllerUserUpdate.java; do production_sources+=("$project_root/src/$relative"); done
while IFS= read -r source; do production_sources+=("$source"); done \
    < <(find "$project_root/src/guiViewer" "$project_root/src/guiCurator" "$project_root/src/roleManagement" "$project_root/src/guiViewerLessons" "$project_root/src/guiCuratorLessons" "$project_root/src/guiCuratorStatistics" "$project_root/src/guiContributorStatistics" "$project_root/src/aiAssistance" "$project_root/src/guiAiAssistance" -name '*.java' -print | sort)
"$TP3_JDK/bin/javadoc" -quiet -private -Xdoclint:all,-missing \
    --module-path "$TP3_JAVAFX_LIB" --add-modules javafx.controls,javafx.fxml \
    -classpath "$TP3_H2_JAR" -sourcepath "$project_root/src" \
    -d "$output_root/New Code Javadoc" "${production_sources[@]}"
test_sources=()
while IFS= read -r source; do test_sources+=("$source"); done \
    < <(find "$project_root/test" -name '*.java' -print | sort)
while IFS= read -r source; do test_sources+=("$source"); done \
    < <(find "$project_root/src" -name '*TestingAutomation.java' -print | sort)
test_sources+=("$project_root/src/database/HW2PersistenceProcess.java"
    "$project_root/src/database/ContributorTestAccounts.java"
    "$project_root/src/database/ContributorManualTesting.java")
"$TP3_JDK/bin/javadoc" -quiet -private -Xdoclint:all,-missing \
    -classpath "$TP3_TEST_CLASSES:$TP3_JAVAFX_LIB/*:$TP3_H2_JAR:$TP3_JUNIT_JAR" \
    -d "$output_root/Test Code Javadoc" "${test_sources[@]}"
echo "Generated: $output_root/New Code Javadoc and $output_root/Test Code Javadoc"
