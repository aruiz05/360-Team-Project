# TP3 Role Access and Persistence Foundations

This increment implements the role-access and safe-persistence step of the approved Phase 3
plan. The baseline is `hw3` commit `bec1de6bdafad9a8962586ec02205203e96c1920`.

## Implemented scope

- Admin, Contributor, Viewer, and Curator assignments are included in registration, account
  listing, role administration, invitations, login dispatch, and account-return navigation.
- Legacy `Role1` / `newRole1` remains Contributor. Legacy `Role2` / `newRole2` now represents
  Viewer. Old account and invitation values remain usable. Curator is an independent
  `curatorRole BOOL DEFAULT FALSE NOT NULL` column added conditionally at connection time.
  Existing accounts never acquire Curator through migration.
- Viewer and Curator have Foundations-style MVC home pages with account update and logout.
  Viewer lesson browsing/search, Curator lesson tools, statistics, and AI assistance belong to
  later increments and are not implemented by these landing pages.
- `RoleAccessService` reads current non-secret assignments without changing the Foundations
  current-user cache. Missing accounts, unsupported/unassigned roles, unavailable databases,
  and accounts awaiting forced password replacement fail closed.
- `LessonService` now checks persisted Contributor assignment on every public CRUD/list call.
  Existing ownership, validation, filtered collections, and lock decisions remain in place.
- Production role mutations and invitation creation require the authenticated administrator's
  username. The old three-argument role setter is package-visible for trusted database fixtures;
  application controllers use the authorized four-argument method. Self Admin removal and final
  assigned-role removal are rejected in persistence logic.
- `Database.inTransaction` coordinates operations on the shared connection. Nested calls use
  savepoints; they do not commit outer work. SQL/unchecked failures and unsuccessful
  `OperationResult` values roll back. Rejected work also restores legacy cached account details.
  Experience creation/replacement participates in this boundary. Contributor mutations lock their
  parent lesson before checking and saving. Future Curator lock operations must follow that same
  parent-row convention.

The authenticated actor comes from the existing login and MVC context. Do not use a form-entered
owner or a database cache left by inspecting another account as the acting identity. The shared
JDBC connection stays on the application execution path; it must not be used by future AI tasks.

## Files and compatibility

- `src/database/Database.java`: additive schema change, current-role lookup, authorized role and
  invitation mutations, parent locking, composable transactions, and cache rollback.
- `src/entityClasses/User.java` and `src/guiTools/RoleNames.java`: four-role representation,
  legacy aliases, role counts, and shared display labels. The existing User constructor remains.
- `src/roleManagement/RoleAccessService.java`: reusable business authorization and role choices.
- `src/guiViewer/` and `src/guiCurator/`: new home MVC families.
- Existing login, administrator, role chooser/editor, user listing, Contributor home, and account
  update packages: new-role integration and checked dispatch.
- `src/lessonManagement/LessonService.java`: current-role and transaction entry points surrounding
  the existing Contributor operations.
- Existing lesson test fixtures and the HW3 policy test double: adjusted to the new authorization
  and transaction contract. Existing checks were retained; registered Contributor fixtures replace
  usernames that previously had no account. The policy double still tests policy integration;
  real H2 transaction behavior is tested separately.
- `test/database/`: new JUnit foundation tests and a separate-JVM persistence helper.
- `test/guiTools/`: actual JavaFX startup, login, registration, administration, and navigation checks.
- `scripts/`: repeatable verification and Javadoc generation.

`module-info.java` stays unchanged: this increment needs only the existing JavaFX controls and
SQL dependencies. JUnit and the optional headless JavaFX backend are test dependencies, not
application runtime dependencies. The default application database remains
`jdbc:h2:~/FoundationDatabase`; tests never use that production path without an isolated home.

## Run all automated checks

Requirements: JDK 25, JavaFX 25 libraries for the host platform, H2, and the JUnit Platform
standalone jar. The verified environment used OpenJDK 25.0.1, JavaFX 25.0.1 for Linux, H2 2.3.232,
JUnit Platform 1.11.4 / Jupiter 5.11.4, and optional TestFX Monocle 21.0.2 for headless GUI tests.
Keep the Eclipse project's existing library paths if they are already working.

Set these paths to your local installations:

```bash
export TP3_JDK=/path/to/jdk-25
export TP3_JAVAFX_LIB=/path/to/javafx/lib
export TP3_H2_JAR=/path/to/h2.jar
export TP3_JUNIT_JAR=/path/to/junit-platform-console-standalone.jar
# Optional on a machine with no graphical display:
export TP3_MONOCLE_JAR=/path/to/openjfx-monocle.jar
bash scripts/test_tp3_foundations.sh
```

The runner first compiles the full modular application, then compiles a separate classpath test
build. It runs all nine inherited executable test suites, the new JUnit suite, and the actual
JavaFX integration harness. It exits nonzero on any failure and prints the temporary output/log
folder. GUI tests get their own `user.home` and persistent database. Each persistence test uses
its own temporary file-backed database, including four independent Java processes in the new
persistence case. Existing in-memory unit tests remain isolated and are supplemented by those
file-backed persistence tests. `ContributorManualTesting` is an instruction printer, not an
additional executable assertion suite.

The exact verification invocation used for this increment was:

```bash
TP3_JDK=/tmp/tp3-tools/jdk-25.0.1 \
TP3_JAVAFX_LIB=/tmp/tp3-tools/javafx \
TP3_H2_JAR=/tmp/tp3-tools/lib/h2.jar \
TP3_JUNIT_JAR=/tmp/tp3-tools/lib/junit-console.jar \
TP3_MONOCLE_JAR=/tmp/tp3-tools/lib/monocle.jar \
TP3_TEST_OUTPUT=/tmp/tp3-final3 \
scripts/test_tp3_foundations.sh
```

## Executed results

All suites below completed with zero failures. Detailed checks within a numbered test are not
additional independent numbered test cases; both measures are shown where the suite reports them.

| Suite | Passed | Failed |
|---|---:|---:|
| OneTimePasswordDatabaseTestingAutomation | 18 checks | 0 |
| DeleteUserDatabaseTestingAutomation | 20 checks | 0 |
| ListUsersDatabaseTestingAutomation | 13 checks | 0 |
| AddRemoveRolesDatabaseTestingAutomation | 15 checks | 0 |
| AddRemoveRolesTestingAutomation | 7 checks | 0 |
| HW2TestingAutomation | 26 cases / 598 checks | 0 |
| TP2BoundaryCoverageTestingAutomation | 12 cases / 204 checks | 0 |
| ExperienceLockPolicyTestingAutomation | 12 cases / 65 checks | 0 |
| PasswordEvaluatorTestingAutomation | 11 checks | 0 |
| TP3FoundationsTest | 20 JUnit tests | 0 |
| TP3RoleNavigationTestingAutomation | 22 GUI checks | 0 |

The GUI harness exercises clean first-run startup, real login for all four roles, multi-role
selection, unassigned-role rejection, account-update return paths, logout, Admin add/remove role
controls, and Viewer/Curator invitation registration. Tests ran through a headless JavaFX backend;
this does not establish pixel appearance or native window behavior in Eclipse on another OS.
Compilation retains the baseline warning about the module name ending in digits. The classpath
GUI test backend may print a JavaFX unnamed-module warning; the production modular build is
checked separately.

## Javadoc

After running tests, set `TP3_TEST_CLASSES` to that run's `classes` directory:

```bash
export TP3_TEST_CLASSES=/path/from/test-run/classes
bash scripts/generate_tp3_javadocs.sh /path/to/new/documentation-output
```

This produces `New Code Javadoc` for the new/changed production elements and `Test Code Javadoc`
for the new and inherited tests. Both sets were generated successfully with doclint enabled
except missing-documentation warnings on inherited elements. The exact verified invocation used
the dependency paths above, `TP3_TEST_CLASSES=/tmp/tp3-final3/classes`, and output
`/tmp/tp3-docs-final`. Generated HTML and third-party jars are not committed to source control.

## Eclipse launch and remaining visual checks

1. Check out `tp3` and import the existing Eclipse project. Preserve working JavaFX and H2 User
   Libraries and use JDK 25 as required by the existing project settings.
2. Run `applicationMain.TP2` as the existing Java application, keeping its JavaFX VM arguments.
   For manual tests, add `-Duser.home=/absolute/path/to/a/new/manual-test-home` so H2 does not
   open your normal application data. Use one application process at a time.
3. Complete first-user Admin setup. Invite separate Viewer and Curator accounts, finish account
   setup, log out, and log in as each. Verify direct role entry, labels, account return, and logout.
4. As Admin assign both Viewer and Curator to one account. Log in as that account and verify
   that only its assigned roles appear and each opens the correct home.
5. As Admin remove one of that account's roles and sign in again. Verify its role list changes.
   Check that self Admin removal and removal of an account's last role show helpful errors.
6. Create and manage a Contributor lesson and experience. Restart the application with the same
   test home and verify the accounts, roles, lesson, and experience remain.
7. Inspect the maroon/gold theme, control spacing, keyboard focus, message readability, and window
   sizing on your OS. These visual checks remain manual; automated GUI navigation already passed.

To run the new JUnit class directly in Eclipse, create a separate JUnit test configuration/source
folder for `test` with JUnit 5 on its test build path. Do not add JUnit requirements to the
production module. The supplied command-line runner is the reproducible reference configuration.

The existing Architecture, Detailed Design, and Manual Tests documents describe the complete
proposed Phase 3 system. Before final submission, align their role storage description with the
implemented legacy alias mapping and mark later feature workflows according to actual completion.
