# TP3 real AI assistance

This increment continues on `tp3` from `961b22510f9e58d44345a7b9d5504de291e46352`.
The `hw3` baseline remains unchanged. The implementation uses a real OpenAI Responses API
adapter; production has no canned-response fallback.

**Live-provider verification is pending configuration:** `OPENAI_API_KEY` is absent in the
implementation environment. Automated tests use an explicitly synthetic loopback HTTP fixture.
Those tests validate the actual HTTP adapter and GUI but do not establish that this project's
account can obtain a live response. The explicit live acceptance command below must be run with
an authorized API key before claiming the final AI execution demonstration passed.

## Implemented stories

| User workflow | Implementation | Verification |
| --- | --- | --- |
| Contributor improve writing | WRITE task, current draft, structured advice/core, explicit accept and reviewed save | `AiAssistanceTest`, `OpenAiClientTest.structuredAndFollowup`, AI GUI suite |
| Brainstorm missing context and alternatives | BRAINSTORM task, labeled generated suggestions and bounded follow-ups | `purposesAndPermissions`, `structuredPurposes`, live command pending |
| Find examples and resources | EXAMPLES task requests required `web_search`; actual provider citation annotations displayed as URLs | `webSearchAndCitations`, `missingAndUnsafeCitations`, AI GUI examples; live command pending |
| Curator refine core | REFINE task on a selected existing lesson, reviewed core saved through Curator rules | `reviewedSavesAndLocks`, AI GUI refinement |
| Curator suggest related terms | TERMS task, review/edit/add/remove lines, explicit existing normalized term save | `reviewedTerms`, AI GUI term review |
| Recover from failed/unconfigured AI | Corrective messages, retained draft, retry/manual editing, cancel, new conversation | `missingConfiguration`, `httpFailuresAreSafe`, `timeout`, `interruption`, AI GUI failure/cancel |
| Protect data and privacy | Current roles, Contributor ownership, reauthorization after in-flight requests and normal save-time lock checks | `ownership`, `revokedRoles`, `reviewedSavesAndLocks`, AI GUI revocation/locks |

Contributor Lessons now provides **AI New Lesson** and **AI Selected Lesson**. Curator Review
provides **AI Assistance** for the selected lesson, using the current unsaved core as context.
The normal inherited Create/Edit workflows remain available.

## Request, review and save lifecycle

1. `AiAssistanceService.prepare` runs on the application thread. It validates purpose, current
   assignment, selected lesson existence/ownership, draft context, prompt and conversation bounds.
   Missing or foreign records are rejected before any external request.
2. An immutable `AiPreparedRequest` supplies permitted context to a JavaFX Task. Generation
   performs no JDBC calls. No username, password, account list, foreign experience data, statistics
   or hidden role information is added to the provider request; only the selected draft and
   conversation are supplied as application context.
3. `OpenAiClient` sends HTTPS `POST https://api.openai.com/v1/responses`, using the configured
   model, `store:false`, current context and successful previous user/assistant turns. Non-example
   tasks request strict JSON-schema output; examples request the web-search tool and citation data.
4. Success returns **unsaved** `AiSuggestion` data. The FX callback rechecks authorization and
   selected context before displaying it. A task identity check excludes canceled/obsolete work.
5. The user reviews the generated area. Accept Suggestion Into Draft copies core or terms into
   editable draft controls. It writes nothing. Discard Suggestion clears only the candidate.
   New Conversation clears in-memory turns/candidates while preserving the draft.
6. Save Reviewed Draft delegates to existing Contributor create/update or Curator refinement.
   Current role, ownership, required input, 500-character title capacity and current protection
   flags are enforced. Save Reviewed Terms delegates to the existing atomic normalized Curator
   term replacement. A rejected operation retains the draft and leaves saved data unchanged.

Brainstormed and generated examples are labeled AI-generated. Retrieved links come only from
provider URL-citation annotations; arbitrary URLs mentioned in generated text are not promoted
into retrieved sources. With no citation annotations, the GUI explicitly says no retrieved
source links were supplied. URLs are displayed for the user to copy into a browser and inspect.
The prompt explicitly prohibits claiming invented examples came from Ed Discussion.

## Provider configuration

Production reads:

- `OPENAI_API_KEY`: required local provider credential, never committed or printed.
- `OPENAI_MODEL`: optional; default `gpt-4.1-mini`. Use a Responses-compatible model with structured
  output and web-search support and account access. Provider-side model/tool errors are reported
  as errors, never replaced with simulated suggestions.

In Eclipse:

1. Refresh `tp3`. Run > Run Configurations > Java Application > your `applicationMain.TP2` launch.
2. Open Environment > New. Name `OPENAI_API_KEY`; enter your authorized key locally as its value.
   Do not export/share a launch configuration containing that credential.
3. Optionally add `OPENAI_MODEL`. Keep existing JDK 25 / JavaFX / H2 configuration.
4. Apply and relaunch. Environment changes require a new application process.

API generation and web-search calls use the configured account's API access and quota. The code
does not infer access from a ChatGPT subscription. No key or live account availability is claimed
by the automated fixture tests.

## Bounds, failures and cancellation

- Prompt: at most 4,000 characters. Draft core context: at most 16,000 total characters.
- Previous conversation: at most 12 user/assistant turns, at most 16,000 characters each.
  Overall draft + prompt + history: at most 48,000 characters. These are documented application
  limits for an interactive course project, not claims about provider model context limits.
- Provider output cap: 2,048 tokens, bounded 256 KiB response receipt, at most 16,000 characters
  of generated output text. Incomplete, refused, empty or malformed output fails helpfully.
- Connection timeout: 10 seconds. Complete-response deadline: 45 seconds. Cancellation interrupts
  the worker and cancels its HTTP future; late callbacks cannot populate a different/current page.
- Duplicate submissions and saves are disabled during a request. Failure/cancellation never
  clears the user's current draft. Retry, manual editing and navigation remain available.
- The adapter follows no HTTP redirects and requires HTTPS for production; loopback HTTP is
  allowed only by the explicit constructor used in tests.
- Conversation history stays in memory for the current workflow. Only explicitly reviewed core
  and terms are persisted through the existing H2 tables. No new AI database migration is needed.

## Code, architecture and compatibility

- `aiAssistance/AiClient`, `OpenAiClient`: provider contract and actual HTTP adapter.
- `AiPurpose`, `AiRequest`, `AiTurn`, `AiPreparedRequest`, `AiSuggestion`, `AiSource`: immutable
  task, conversation, permitted context and unsaved response values with defensive copies.
- `AiAssistanceService`: application-thread authorization/preparation, worker-only generation,
  normal reviewed core saves and Curator term saves.
- `JsonCodec`: small bounded strict JSON codec for this wire protocol, with escaping/Unicode,
  invalid syntax and nesting tests. It avoids a new application library/framework.
- `guiAiAssistance/`: View/Model/Controller for requests, follow-ups, accept/discard, cancel,
  reviewed persistence and return navigation. Contributor table spacing accommodates two new buttons.
- `module-info.java`: adds the JDK's `java.net.http` module. No external production dependency
  is introduced; existing JavaFX/SQL requirements remain.
- New tests: `database/AiAssistanceTest`, `aiAssistance/OpenAiClientTest`, `AiProviderFixture`,
  `guiTools/TP3AiGuiTestingAutomation`, plus explicit `database/AiLiveVerification`.
- Test/Javadoc runners include all new sources. The live command is separate and never silently
  run, skipped-as-passed or substituted by a fixture.

The default application database remains `jdbc:h2:~/FoundationDatabase`. TP1 accounts/OTP/roles,
Contributor ownership/CRUD/protections, Viewer discovery, Curator core/terms/locks and persistent
statistics/visibility remain covered by the inherited suites. Existing core and term persistence
also continues to be verified by separate-process regression tests.

## Automated checks and live acceptance

Use the existing test dependency environment described in `TP3_STATISTICS.md`:

```bash
bash scripts/test_tp3_foundations.sh
export TP3_TEST_CLASSES=/path/to/test-output/classes
bash scripts/generate_tp3_javadocs.sh /path/to/new-javadoc-output
```

Headless runs use Monocle with a 1600x1000 test display and single-threaded rendering. This
resolved headless buffer-upload exceptions seen during an earlier run. The runner now also
rejects logged GUI runtime exceptions even when a Java process exits successfully. Native macOS
rendering still requires the manual checks below.

**Explicit live acceptance, after setting a key locally:**

```bash
# Set OPENAI_API_KEY locally without adding it to source files or command history.
# Reuse TP3_JDK, TP3_H2_JAR and TP3_TEST_CLASSES from the automated test build.
bash scripts/verify_live_ai.sh
```

This makes real provider requests in a newly created isolated temporary H2 database: initial
writing, a follow-up, brainstorming, cited example search, Curator refinement and terms. It
verifies reviewed saves, stable ownership and Contributor lock rejection. It prints LIVE PASS
only for executed successful calls and removes its own temporary database when complete.
It exits with an error if configuration or an actual request fails.

## Executed results — November 19, 2026

OpenJDK 25.0.1, JavaFX 25.0.1 Linux, H2 2.3.232, JUnit Platform 1.11.4 and Monocle
21.0.2 were used. All selected automated tests executed; none were skipped.

| Suite | Passed | Failed |
| --- | ---: | ---: |
| Nine inherited regression suites | 951 checks | 0 |
| Foundation JUnit | 20 | 0 |
| Discovery/Curator JUnit | 22 | 0 |
| Statistics/visibility JUnit | 25 | 0 |
| AI authorization/reviewed-save JUnit | 11 | 0 |
| Actual HTTP protocol/JSON/provider-error JUnit | 11 | 0 |
| Actual JavaFX role navigation | 22 | 0 |
| Actual JavaFX discovery/Curator | 42 | 0 |
| Actual JavaFX statistics/visibility | 36 | 0 |
| Actual JavaFX AI workflow with explicit HTTP fixture | 33 | 0 |
| Real provider acceptance | **Not executed: API key unavailable** | Not claimed passed |

Totals for executed automated checks: 951 inherited checks, 89 JUnit scenarios and 133 JavaFX
checks, zero failures. Modular and classpath compilation succeeded. Both production and test
Javadoc sets generated successfully with `-Xdoclint:all,-missing`. The inherited module-name
warning and the headless classpath unnamed-module warning remain. No GUI runtime exception was
logged in the final run. Native rendering and live AI behavior still require the checks below.

Exact final verification commands executed from `/workspace/scratch/c73983b04b44`:

```bash
export TP3_JDK=/workspace/scratch/c73983b04b44/tools/jdk-25.0.1
export TP3_JAVAFX_LIB=/workspace/scratch/c73983b04b44/tools/javafx
export TP3_H2_JAR=/workspace/scratch/c73983b04b44/tools/lib/h2.jar
export TP3_JUNIT_JAR=/workspace/scratch/c73983b04b44/tools/lib/junit-console.jar
export TP3_MONOCLE_JAR=/workspace/scratch/c73983b04b44/tools/lib/monocle.jar
export TP3_TEST_OUTPUT=/workspace/scratch/c73983b04b44/ai-final
bash repo/scripts/test_tp3_foundations.sh
export TP3_TEST_CLASSES=/workspace/scratch/c73983b04b44/ai-final/classes
bash repo/scripts/generate_tp3_javadocs.sh /workspace/scratch/c73983b04b44/ai-javadoc
```

Logs and JUnit XML are in the specified test output during verification; the portable runner
regenerates them on another machine. Source and report identify fixtures explicitly so their
results cannot be confused with live model responses.

## Eclipse manual demonstration still required

Use a dedicated application home for manual data and keep its path identical across restarts.
Run `applicationMain.TP2` after configuring the provider environment.

1. Create Contributor and Curator accounts using existing Admin invitation/role workflows.
2. As Contributor > Manage Lessons > AI New Lesson, enter title `Persistent H2 storage`, problem
   `Our course project lost lessons after restart`, lesson `Use file-backed H2 and transactions`.
   Choose WRITE and request `Improve clarity while preserving the facts and keeping a short title`.
   Expect actual generated advice/core, a responsive interface and no new lesson in storage yet.
3. Enter a follow-up `Make this more concise and add a restart verification step`. Expect a response
   using the previous successful exchange. Accept into draft, edit wording and Save Reviewed Draft.
   Confirm the owning Contributor's list shows the saved lesson with its assigned ID.
4. Choose BRAINSTORM and ask `What context and alternatives should our team add?`. Review generated
   questions/examples. Discard output; the current draft and saved lesson must remain unchanged.
5. Choose EXAMPLES and ask for official H2 persistent-URL/transaction examples with citations.
   Expect generated guidance plus actual returned source URLs. Open/copy the URLs and inspect
   them. Do not claim uncited generated examples are retrieved or are from Ed Discussion.
6. As Curator > Review Lessons, select the lesson > AI Assistance. Use REFINE, ask an initial
   request and a follow-up, accept/edit and explicitly save. Confirm original Contributor owner
   and ID remain. Use TERMS, accept suggestions, edit/add/remove lines and Save Reviewed Terms.
   Verify Viewer discovery finds the lesson using a saved related term.
7. Lock the lesson/title as Curator. In an older Contributor AI draft, attempt a changed reviewed
   save. Expect rejection and unchanged stored values. Curator refinement remains authorized.
8. Test blank requests, oversized requests and invalid reviewed required text/terms. Expect
   corrective feedback and retained drafts. With a missing/invalid key, expect configuration or
   access feedback; network/timeout/rate-limit failures must not fabricate successful output.
9. Cancel a pending request and navigate back. Expect immediate control and no late output replacing
   the draft or another page. Check keyboard focus, scrolling, readable controls and native layout.
10. Restart with the same application home. Confirm reviewed core and terms persist. Recheck TP1
    login/OTP/self-protection, Contributor CRUD, Viewer search and statistics sharing.

## Official API sources and remaining verification

The adapter follows the official Responses, structured output and web-search contracts:

- https://developers.openai.com/api/reference/resources/responses/methods/create
- https://developers.openai.com/api/docs/guides/tools-web-search
- https://developers.openai.com/api/docs/models/gpt-4.1-mini

A configured live run and native Eclipse demonstration remain required. Final submission DOCX/PDF
alignment, screenshots, screencasts and archive packaging are not claimed completed by this code
increment. The assignment's discussion-assessment wording still needs instructor clarification
against the linked Lessons Learned epics; unrelated discussion functionality is not added.
