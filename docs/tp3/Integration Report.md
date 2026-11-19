# Integration Report

CSE 360 TP3 release-candidate audit, November 19 2026 (Phoenix). Baseline hw3: bec1de6bdafad9a8962586ec02205203e96c1920. This increment starts from tp3 c0d315cef9764b50f93a34aeff1624b26aa73e52. The final commit is identified by Git history; no baseline branch is modified or merged.

## 1 Outcome and evidence boundary

The implemented application integrates four-role access, persistent Viewer/Curator workflows, protected Contributor data, historical statistics and reviewed AI-assisted saves. This final increment adds the TP3 launcher, validation boundaries, cross-feature integration tests, named-module startup verification and reproducible aligned documentation and packaging.

Automated code checks pass. Full specification acceptance remains pending real-provider AI execution, native Eclipse manual testing and actual team deliverables. No API credential, team meeting, board history, grader feedback, recording, peer evaluation or Canvas acceptance is fabricated.

## 2 Requirement comparison

| Requirement | Status | Repository evidence | Remaining work |
| --- | --- | --- | --- |
| TP1 accounts, OTP replacement, deletion and self-protection | Implemented; regression verified | Database, role/login/admin MVC; five inherited account suites | Native manual confirmation |
| TP2 owned lesson/experience CRUD and persistence | Implemented; regression verified | LessonService, validators, policies; HW2 and TP2 boundary suites | Native manual confirmation |
| Four roles and current assignment enforcement | Implemented; automated verified | RoleAccessService, RoleNames, dispatch; TP3FoundationsTest | Native chooser/launch review |
| Safe schema addition and rejected-write rollback | Implemented; automated verified | Database.inTransaction and lockLesson; migration/fault/concurrency tests | No production database was altered in testing |
| Viewer browse/read/search and private projection | Implemented; automated verified | ViewerLessonService, ViewerLesson, guiViewerLessons; discovery tests | Native rendering review |
| Curator review, refine, search terms and protections | Implemented; automated verified | CuratorLessonService, guiCuratorLessons; discovery/lock tests | Native workflow review |
| Persistent statistical summaries and visibility | Implemented; automated verified | StatisticsService/Calculator, entities and three tables; statistics tests | Native review with agreed demonstration data |
| Cross-record history and deletion behavior | Implemented; automated verified | Snapshot live link SET NULL; destination cascade; separate-JVM and final integration tests | None identified by executed tests |
| Interactive AI assistance with reviewed saves | Implemented; fixture verified; live acceptance pending | aiAssistance, guiAiAssistance; adapter/service/GUI tests; verify_live_ai.sh | Run with a valid private provider key and review actual examples/citations |
| Input validation, helpful errors and unchanged rejected data | Implemented; automated verified | LessonValidator, ExperienceValidator, terms/statistics/AI guards; boundary and rollback tests | Native feedback review |
| Staff User Stories.pdf | Generated from current role stories | docs/tp3/Staff User Stories.md | Team/instructor review of domain wording |
| Architecture.pdf and Detailed Design.pdf | Generated; aligned to actual classes | docs/tp3 diagrams, CRC, scenarios and method contracts | Incorporate any unavailable grader feedback |
| New Code Javadoc and Test Code Javadoc | Generated with doclint; test links included | scripts/generate_tp3_javadocs.sh and production/test source | Team review in browser |
| Manual Tests.pdf | Generated procedures; native/live execution pending | docs/tp3/Manual Tests.md | Execute and record actual results/screenshots |
| TP3 Source.zip and TP3 folder | Reproducibly packaged code/documentation subset | scripts/package_tp3.py; source includes test code | Add remaining genuine team evidence before final submission |
| Team norms PDF | Missing team evidence | No approved norms provided | Supply TP3 Team Norms.pdf; change bars/explanation if changed |
| HW3 high-risk partition and prioritized board | Not certified | Existing proposed plan is not evidence of agreement | Verify actual team partition and Taiga/Jira status |
| Initial backlog, plan and schedule PDF | Proposed content incomplete | Earlier DOCX has unconfirmed dates/assignments | Team agrees board, first-sprint owners/due dates and meeting schedule; export exact required PDF |
| Two stand-ups weekly; minimum four; >=47 hours apart | Missing recordings and actual schedule | No actual meeting evidence supplied | Conduct/verify real meetings and required summaries |
| Updated plan versions or truthful No Updates.pdf | Missing team evidence | No actual change history supplied | Preserve numbered versions starting 2 with change bars, or truthful no-updates explanation |
| Five final team screencasts and roughly equal speaking | Missing team evidence | No final recordings supplied | Produce requirements/design/code/testing/execution videos |
| Size checks and Canvas acceptance | Code subset small; final submission pending | Reproducible package; no videos included | Verify completed ZIP and upload acceptance; each member checks |
| Individual peer evaluation and acknowledgement PDF | Pending individual work | Outside application | Each member submits form and acknowledgement before deadline |
| Future LessonShare integration | Anticipation only | Provider interface and layered services | No working external integration required or added |

## 3 Changes in this final increment

- applicationMain.TP3 supplies a clearly named final launch target while retaining earlier entry points.
- LessonValidator rejects titles longer than the existing 500-character storage limit before any write. ExperienceValidator similarly checks the existing 255-character member-name limit. Exact valid boundaries and unchanged rejected data are tested.
- TP3FinalIntegrationTest covers generated-but-unsaved text, reviewed Curator refinement, persistent search terms and locks, Viewer discovery, summary sharing, source deletion, destination cascades and reopening the file database.
- TP3ModularSmoke exercises the real TP3 launcher under the named module in an isolated home. The test runner includes it and rejects asynchronous GUI error logs even when a process exits zero.
- OpenAiClientTest now asserts the worker interruption observation on the JUnit thread, so a worker-only assertion cannot silently pass.
- Documentation diagrams, outline PDFs, Javadoc inputs and archive scripts describe current behavior. Historical increment notes remain identified as historical.

No new database schema is needed in this increment. Earlier Phase 3 additive tables and Curator column are retained. No production data or baseline branches are changed.

## 4 Executed verification

### 4.1 Environment and commands

- JDK 25.0.1, JavaFX 25.0.1 Linux, H2 2.3.232, JUnit Platform 1.11.4 and optional Monocle 21.0.2. The native macOS Eclipse environment was not available.
- Dependencies were configured through TP3_JDK, TP3_JAVAFX_LIB, TP3_H2_JAR, TP3_JUNIT_JAR and TP3_MONOCLE_JAR, as shown in README.md.
- Main command: bash scripts/test_tp3_foundations.sh with TP3_TEST_OUTPUT pointing to a new isolated output directory. The package is also extracted and checked with this same command; exact local paths are captured in Verification Commands.txt beside the evidence.
- Javadoc command: TP3_TEST_CLASSES=<test-output>/classes bash scripts/generate_tp3_javadocs.sh <new-javadoc-output>.
- PDF command: python3 scripts/build_tp3_documents.py <pdf-output>. Packaging: python3 scripts/package_tp3.py --pdfs <pdf-output> --javadoc <javadoc-output> --evidence <test-output> --output <new-package-output>.

### 4.2 Results

| Executed group | Passed | Failed | Detail |
| --- | --- | --- | --- |
| Inherited TP1/TP2/HW3/password checks | 951 | 0 | Nine suites; readable expected/actual and totals |
| Phase 3 JUnit scenarios | 91 | 0 | Six selected classes, none skipped |
| JavaFX workflow checks | 133 | 0 | Role 22; discovery 42; statistics 36; AI fixture 33 |
| Named-module launch smoke | 3 | 0 | Actual TP3 launcher, scene and isolated file database |

The groups use different counting conventions and are intentionally not presented as a single number of JUnit tests. Tests exercise positive, negative, boundary, ownership, role, protection, missing-ID, stale-preview, rollback, count, relationship, large-list and separate-process persistence behavior. Detailed individual names, expected/actual values and results are preserved in logs and JUnit XML. The only inherited module naming warning concerns terminal digits in FoundationsF26; no GUI exception is accepted as a passing run.

Live AI verification was not run: OPENAI_API_KEY is unavailable. Fixture tests exercise real HTTP request/response handling against a synthetic loopback provider; they do not prove external service availability, generated quality or web citation relevance. Native manual cases and screenshots remain pending rather than assumed passed.

## 5 Exact remaining submission obligations

- TP3/TP3 Team Norms.pdf.
- TP3/TP3 Planning and Tracking/Initial TP3 Backlog, Plan, and Schedule.pdf, with prioritized backlog, first-sprint due dates, named owners and actual agreed meeting schedule.
- At least four genuine status summaries under TP3/TP3 Planning and Tracking/Status Summary Recordings/, named Status Summary 2026-MM-DD.mp4 or .mov. Each summary is at most five minutes, with approximately 45 seconds per member, audio and shared screen only, produced with Zoom. Actual stand-ups are roughly 15 minutes, twice each week and at least 47 hours apart; rubric recording dates also differ by at least two days.
- Updated plan PDFs named TP3 Backlog, Plan, and Schedule Version <n>.pdf, sequential from 2, retaining older versions and change bars, when updates occur; otherwise a truthful No Updates.pdf explaining why no changes were needed. Significant blockers require an issue-resolution meeting.
- Five final screencasts. Tasks 7.1 through 7.4 are each 5 to 10 minutes; Task 7.5 is 8 to 15 minutes. Team members present roughly equal amounts, with pairs desirable. Prioritize received stories, readable text, clear audio and realistic execution examples. Exact final-video basenames are not specified in the supplied excerpt; use clear task labels and the required viewable formats.
- The TP3 ZIP must be significantly smaller than 1 GB. Task 3 summary total is at most 30 minutes; Task 7 total is less than 55 minutes. Verify video size constraints before upload.
- Every member checks successful Canvas acceptance before the deadline. If still working on the due day, submit the current package at noon and improve it afterward. Each member separately submits the peer-evaluation form and its successful-submission acknowledgement PDF.

## 6 Instructor clarification and limits

- Task 4 mentions Staff Epics about discussion assessment while the supplied system stories concern Lessons Learned. This implementation follows the actual Lessons Learned role stories; ask the instructor to confirm the wording.
- The introduction's video-total limit of 100 MB conflicts with Task 8's less-than-300-MB combined sets. Follow the stricter 100-MB total pending clarification and retain each-video constraints.
- No actual grader feedback was supplied, so compliance with undisclosed feedback cannot be certified.
- Existing invitation-expiration behavior is inherited scope, not a new Phase 3 implementation claimed by this increment. Confirm any unresolved inherited acceptance issue with the team rather than silently expanding the new requirements.
- Full release acceptance must wait for the outstanding live/native/team evidence listed above. Passing automated tests establish the executed behavior, not a guarantee about unexecuted environments.
