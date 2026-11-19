# TP3 Viewer discovery and Curator core operations

This document records the second increment. Persistent statistics and visibility are now
implemented in the next increment; see [TP3_STATISTICS.md](TP3_STATISTICS.md).

This increment continues on `tp3` from foundation commit
`a0f2b9f21c683e01c34c9abd161ee6d5c7e95c4a`. The original `hw3` baseline remains
`bec1de6bdafad9a8962586ec02205203e96c1920`. Neither `hw3` nor `main` is changed.

## Implemented behavior

| Story / behavior | Implementation | Validation |
| --- | --- | --- |
| Viewer browse all cores, IDs and titles | `ViewerLessonService`, `ViewerLesson`, `guiViewerLessons` MVC | `discoveryAndPrivacy`, `collectionSizes`, JavaFX suite |
| Case-insensitive AND search across title, problem, lesson text and saved terms | Shared `DiscoveryAccess.search`, fresh full storage read per query | `andSearch`, `refreshedSearch`, JavaFX search/return |
| Read current core and return to originating query | Viewer read-only detail panel, stable selected ID and query retained | `invalidSelections`, `deletedSelections`, JavaFX stale-selection checks |
| Curator cross-owner review | `CuratorLessonService`, `guiCuratorLessons` MVC, experience IDs/parents/owners, elapsed minutes, member person-minutes and lock flags | `experienceReview`, JavaFX review |
| Reviewed manual core refinement | Dedicated Curator service path updates core only, even when Contributor protection is active | `refinementPreservesRelationships`, `invalidCore`, JavaFX save/reload |
| Reviewed related-term maintenance | Atomic complete-set replacement, trim/lower-case/deduplicate, explicit Clear Terms | `termsNormalizationAndClear`, `invalidTerms`, JavaFX saved-term search and clear |
| Whole lesson and independent core-field protection | `LessonLockState`, production `Database.updateLessonLocks`, Curator Protections tab | `independentLocks`, `wholeLessonProtection`, `concurrentLock`, JavaFX protections |
| Current roles, unchanged rejected operations and safe persistence | Existing role/transaction foundations reused by every new service operation | `wrongRoles`, `revokedRoles`, `sqlFailureRollback`, `enclosingFailureRollback`, `serviceFailureRollback`, `persistenceAcrossProcesses` |

Viewer responses contain only ID, title, problem/situation and lesson text. No raw experiences,
owner information, hidden role data or mutation route is returned by that service. Contributor
lists remain filtered by their existing ownership path. A Curator refinement retains the
original owner, ID, locks, experiences, efforts and terms. All mutations reload current role,
lock the parent row, validate, write and commit through `Database.inTransaction`. Failure
results and SQL exceptions roll back; no nested method commits an enclosing transaction.

Search uses whitespace-separated words and substring matching. Every word must occur in at
least one searchable field. A new query always starts with current storage, never an older
filtered subset. Blank query / Show All restores all lessons. Lists have no fixed item ceiling.
The term store permits at most 500 characters per normalized term; title uses the existing H2
500-character column limit. Required text, missing/invalid IDs and blank term entries are
rejected with corrective feedback before writes. An empty reviewed list means an explicit clear.

## Database, source and compatibility

- `src/database/Database.java`: additive `lessonSearchTerms(lessonId, term)` table. Composite
  primary key prevents duplicate terms; foreign key to `lessons` cascades deletion. Includes
  term reads, caller-transaction replacement, and production core-lock updates.
- `src/entityClasses/ViewerLesson.java` and `SearchTerm.java`: core-only read projection and
  persistent term relationship.
- `src/lessonManagement/ViewerLessonService.java`, `CuratorLessonService.java`,
  `DiscoveryAccess.java`, `LessonLockState.java`: current-role business rules and lock decisions.
- `src/guiViewerLessons/` and `src/guiCuratorLessons/`: View/Controller/Model workflows.
  Existing Viewer and Curator homes gain Discover Lessons / Review Lessons buttons.
- `test/database/TP3DiscoveryTest.java`, `TP3DiscoveryPersistenceProcess.java`, and
  `test/guiTools/TP3DiscoveryGuiTestingAutomation.java`: new automated verification.
- `scripts/test_tp3_foundations.sh` and `generate_tp3_javadocs.sh`: extended existing runners.

Existing tables, account data, Contributor entities/services and TP1 behavior are preserved.
`module-info.java` requires no additional dependencies. Default application storage remains
`jdbc:h2:~/FoundationDatabase`. Automated tests use temporary file databases and isolated GUI
homes; test data is invented course-project data. Repeatable populated migration and four
separate-JVM phases verify saved core, terms, locks, efforts and cascading deletion.

## Run and verify

Use the existing Eclipse JDK 25, JavaFX and H2 configuration. Refresh the project, then run
`applicationMain.TP2` as a Java Application. The launcher retains its existing name.

The same runner as the foundation implementation now includes this increment. Set dependency
paths for your machine (Linux verification used OpenJDK 25.0.1, JavaFX 25.0.1, H2 2.3.232,
JUnit Platform 1.11.4, and headless Monocle 21.0.2):

```bash
export TP3_JDK=/path/to/jdk-25
export TP3_JAVAFX_LIB=/path/to/javafx/lib
export TP3_H2_JAR=/path/to/h2.jar
export TP3_JUNIT_JAR=/path/to/junit-platform-console-standalone.jar
# Optional only when running without a graphical display:
export TP3_MONOCLE_JAR=/path/to/openjfx-monocle.jar
bash scripts/test_tp3_foundations.sh
```

The runner compiles the modular application and test classes, runs all nine inherited suites,
then both JUnit classes and both actual JavaFX integration suites. It exits nonzero on failure.
Logs and JUnit XML are retained in the output folder printed by the runner. Each run chooses a
new output directory rather than overwriting prior results.

To generate both Javadoc sets after a successful test build:

```bash
export TP3_TEST_CLASSES=/path/to/test-output/classes
bash scripts/generate_tp3_javadocs.sh /path/to/new-javadoc-output
```

Generated folders are `New Code Javadoc` and `Test Code Javadoc`. The existing module-name
warning about terminal digits in FoundationsF26 is inherited. Headless classpath JavaFX may
warn about unnamed modules; the separate modular compilation validates `module-info.java`.

## Executed verification results — November 19, 2026

All available automated suites were executed against this increment. No checks were skipped.

| Suite | Passed | Failed |
| --- | ---: | ---: |
| One-time password database | 18 | 0 |
| Delete-user database | 20 | 0 |
| List-users database | 13 | 0 |
| Add/remove roles database | 15 | 0 |
| Add/remove roles controller | 7 | 0 |
| HW2 CRUD/validation/persistence | 26 cases / 598 checks | 0 |
| TP2 boundaries | 12 cases / 204 checks | 0 |
| HW3 experience lock policy | 12 cases / 65 checks | 0 |
| Password evaluator | 11 | 0 |
| Foundation JUnit | 20 | 0 |
| Discovery/Curator JUnit | 22 | 0 |
| Actual JavaFX role navigation | 22 | 0 |
| Actual JavaFX discovery/Curator workflows | 42 | 0 |

That is 951 inherited checks, 42 JUnit scenarios, and 64 GUI checks, with zero failures.
Both modular and classpath compilation succeeded. Both production and testing Javadoc sets
were generated successfully using `-Xdoclint:all,-missing`. The module descriptor is unchanged.

Exact verification commands executed from `/workspace/scratch/c73983b04b44`:

```bash
export TP3_JDK=/workspace/scratch/c73983b04b44/tools/jdk-25.0.1
export TP3_JAVAFX_LIB=/workspace/scratch/c73983b04b44/tools/javafx
export TP3_H2_JAR=/workspace/scratch/c73983b04b44/tools/lib/h2.jar
export TP3_JUNIT_JAR=/workspace/scratch/c73983b04b44/tools/lib/junit-console.jar
export TP3_MONOCLE_JAR=/workspace/scratch/c73983b04b44/tools/lib/monocle.jar
export TP3_TEST_OUTPUT=/workspace/scratch/c73983b04b44/verification-1
bash repo/scripts/test_tp3_foundations.sh
export TP3_TEST_CLASSES=/workspace/scratch/c73983b04b44/verification-1/classes
bash repo/scripts/generate_tp3_javadocs.sh /workspace/scratch/c73983b04b44/javadoc-1
```

Generated output lives under the two supplied output directories during this verification.
Use the portable commands above to regenerate it on your machine. The standalone test and
Javadoc scripts expand the exact compiler and Java commands and retain per-suite logs.

## Eclipse manual checks still required

Automation covers actual JavaFX actions with a headless backend. Native macOS rendering,
keyboard focus and readable scrolling must still be checked in Eclipse; they are not claimed
as manually passed here. Use an isolated test home for these steps if preserving real accounts:
create `$HOME/cse360-tp3-manual-test` and put its expanded absolute path in Eclipse Run
Configurations > Arguments > VM arguments as `-Duser.home=/Users/yourname/cse360-tp3-manual-test`.
Keep that same path across restart tests.

1. Refresh `tp3` in Eclipse and run `applicationMain.TP2`. Create the initial Admin account if
   using a fresh home. Through Admin invitation/role controls, create separate Contributor,
   Viewer and Curator accounts. Give the administrator only Admin for the role restriction check.
2. Sign in as Contributor. Open Manage Lessons, add a lesson titled `H2 Persistence`, problem
   `A course project lost changes after restart`, and lesson `Validate input and use file-backed
   H2`. Add an experience with what `Verified restart`, how `Closed and reopened the app`, elapsed
   `12` minutes, and member `Sam` effort `10` person-minutes. Add another Contributor and lesson
   if checking cross-owner lists. These examples are invented, not Ed Discussion content.
3. Sign in as Viewer > Discover Lessons. Expect each saved ID/title once. Search `h2 validate`;
   expect the first lesson. Search `absentword`; expect an empty list and helpful guidance.
   Show All restores all. Select a lesson > Read Lesson; expect current core text only and no
   edit, delete, raw experience or save controls. Return to Results retains the query and ID.
4. Sign in as Curator > Review Lessons. Select that lesson; expect original owner and core.
   Experiences shows parent/experience IDs, what/how, elapsed minutes, member person-minutes and
   lock flags. Confirm scrolling and text are readable at normal display scaling.
5. In Core, clear the title and click Save Reviewed Core. Expect a corrective error, retained
   draft and unchanged saved core. Enter `Reviewed persistence`, save, and verify the owner and
   ID stay unchanged. Change the draft again without saving, then Reload Selected; expect the
   last saved text. The owning Contributor should see the refined text on refresh.
6. In Related Terms, enter `Rollback`, `ROLLBACK`, and `Recovery` on separate lines. Save Reviewed
   Terms; expect only `recovery` and `rollback`. Add a blank line and try saving; expect an error
   and the prior saved set unchanged. Viewer search `rollback` must find this lesson. As Curator,
   filter by `rollback`, select the lesson, then Clear Terms; expect the row to disappear from
   that subset and the details to clear. Show All returns the lesson.
7. In Protections, check Whole Lesson and Title, Save Protections. As Contributor, attempt core
   edit, lesson deletion, and experience create/update/delete; all must fail unchanged. As
   Curator, uncheck only Whole Lesson and save; Title must stay checked. Contributor may change
   an unlocked field when retaining the original title; changing the title or deleting the
   lesson must fail. Curator may still save a reviewed core refinement.
8. Close and relaunch using the same home. Verify core, terms and lock flags persist. Unlock all
   applicable protections as Curator, delete the lesson as its Contributor, restart again and
   confirm it stays absent from both discovery and Curator review.
9. Recheck account login, OTP replacement, delete-user self-protection, role self-protection,
   invitations and account-update return navigation. Check that a Contributor never sees
   another owner's lessons. Confirm an Admin without Curator cannot enter Curator operations.

## Scope remaining

This increment implements Viewer discovery and Curator **core** operations. Actual AI
suggestions/follow-up, statistics calculation/snapshots, summary visibility and their GUI flows
remain later backlog items. Manual core and term editing does not satisfy the full AI epic.
No Curator delete workflow or external LessonShare integration is added. The specification's
Task 4 discussion-assessment wording still needs instructor clarification against the linked
Lessons Learned epics. Existing DOCX diagrams and final submission PDFs still require a final
alignment pass after all increments; this change does not claim those artifacts are finalized.
