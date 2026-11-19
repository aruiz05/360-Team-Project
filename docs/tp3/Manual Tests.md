# Manual Tests

CSE 360 TP3 release-candidate procedures. Native Eclipse and real-provider checks below are pending; they are not reported as executed. Automated JavaFX checks ran headlessly on Linux with an explicit local HTTP provider fixture. Use invented course examples below, not material represented as copied from Ed Discussion.

## 1 Setup and safety

### 1.1 Import and launch in Eclipse

- Open the tp3 checkout with File > Import > Existing Projects into Workspace. Select the repository root. Use JDK 25 and the project's JavaFX user library; repair JavaFX and H2 library paths for your computer in Project > Properties > Java Build Path.
- Keep module-info.java. Run applicationMain.TP3 as a Java Application. In Run Configurations, place JavaFX on the module path and include --add-modules javafx.controls,javafx.fxml --enable-native-access=javafx.graphics if your Eclipse launch does not supply these automatically.
- For a disposable manual database, create an empty folder outside the checkout and set VM argument -Duser.home=/absolute/path/to/that/folder. The application creates FoundationDatabase.mv.db there. Do not point tests at an existing production home or delete an existing database.
- Create the first Administrator through the startup page. Use administrator workflows to create or invite accounts for Contributor Alice, Contributor Bob, Viewer Vera and Curator Casey. Choose valid credentials accepted by the existing validators. Record the actual usernames privately; do not put passwords or API keys in screenshots.
- If an account has multiple roles, select the intended role at login. Give one account both Contributor and Viewer to exercise the chooser. Confirm logout returns to login.

### 1.2 Evidence and recording convention

- Create a local evidence folder TP3/Manual Test Screenshots. This is a suggested evidence location, not an extra filename mandated by the specification.
- For each test, record date, OS, Java/JavaFX versions, tested commit, actor, actual result and pass/fail in a test log. Mark unexecuted cases pending.
- Suggested screenshots: role-chooser.png, viewer-results.png, curator-locks.png, statistics-preview.png, contributor-shared-statistics.png, ai-reviewed-draft.png and validation-error.png. Capture the relevant scene, feedback and result; hide secrets and unrelated personal information.

## 2 Accounts and inherited regressions

### 2.1 Account permissions

- Sign in as Administrator. List accounts and verify the four role names. Add Viewer and Curator assignments to test accounts, then remove an assignment and log in again.
- Expected: only assigned roles are offered; a removed role cannot open its workflow. An account with multiple assignments sees the role chooser.
- Attempt to delete the signed-in Administrator and remove its own Admin role. Expected: helpful rejection and the account/assignment remains.
- Issue a one-time password to a test account. Sign in with it, attempt to continue without replacement, replace it, log out and sign in with the replacement. Expected: forced replacement precedes role access; the old one-time password no longer authenticates.

### 2.2 Contributor CRUD and visibility

- As Alice create a lesson titled H2 transaction rollback, with a problem about a partially saved team record and core guidance to validate then save atomically. Add two attempts: elapsed 30 and 50 minutes; summed team effort 40 and 80 person-minutes. Use realistic member names and the form's required experience fields.
- As Bob create a separate JavaFX responsiveness lesson and an attempt with elapsed 70 and effort 120. Alice's list must exclude Bob's lesson, and vice versa.
- Select, edit and reopen Alice's lesson. Delete a disposable third lesson. Close the entire Java process, launch with the same isolated home and reopen the retained lesson.
- Expected: edits and attempts survive restart, the deleted lesson remains absent and ownership does not change. Lists update after create, edit and delete. Repeat with zero and one lessons in a disposable account.

## 3 Viewer discovery and Curator operations

### 3.1 Browse and search

- Sign in as Vera. Open lesson browsing, search H2 and select the transaction lesson. Search a term with mixed capitalization, multiple terms and a value that matches nothing. Clear the search.
- Expected: matching core content is readable, all search tokens constrain results, no-match and empty collections show helpful states, clearing restores all core lessons. No owner or raw experience details are shown and there are no Viewer mutations.
- As Casey select that lesson, add the term atomicity and save. Log in as Vera and search atomicity. Expected: the lesson is discoverable by its saved term.

### 3.2 Refine, protect and reject

- As Casey open the lesson and inspect its experience evidence. Refine its title and core text, save, and protect its title and experience records. Reopen the lesson to confirm the settings.
- As Alice attempt to alter the protected title and protected experience. Expected: rejection identifies the protection and the saved values remain unchanged. Permitted unprotected updates still work.
- As Casey remove protection, save, and retry as Alice. Expected: the allowed edit succeeds. Casey's refinements never change owner, identity or unrelated experience.
- Supply a blank term in a replacement list, then reopen the saved terms. Expected: replacement is rejected and the previous terms remain. Clear terms explicitly and verify removal from term-only search.

## 4 Persistent statistics and visibility

### 4.1 Review, save and share

- As Casey open statistics, choose Alice's destination lesson and Alice's two attempts. Preview. Expected elapsed count 2, mean 40, median 40, minimum 30, maximum 50; person-minute mean 60, median 60, minimum 40, maximum 80.
- Save, close the dialog and reopen the summary. Expected: captured source IDs, reviewed values and timestamp persist; visibility initially hidden.
- As Alice open shared statistics. Expected: that hidden summary is absent. As Casey enable sharing, then retry as Alice. Expected: aggregates are visible for Alice's destination, while raw source records, owners and team names are absent. Bob must not see Alice's shared summary.
- Restart the Java process and repeat the shared read. As Casey hide the summary; Alice's next read must omit it.

### 4.2 Changed or removed sources

- Preview a summary, then change a selected source through an authorized workflow before saving the preview. Expected: save requires a fresh preview; no partial summary is inserted.
- Save a summary including Bob's source, then delete that source through Bob's permitted workflow. Expected: the saved historical values remain and source availability is identified correctly.
- Delete a disposable destination lesson and restart. Expected: its summaries and dependent snapshot rows no longer appear. Try only one source, duplicate source selection and invalid numeric input; each rejection must leave counts unchanged.

## 5 AI assistance: live-provider acceptance pending

### 5.1 Configure and exercise real generation

- Configure OPENAI_API_KEY in the Eclipse launch Environment tab using a valid local credential. Optional OPENAI_MODEL selects the model. Do not commit this configuration, show the credential or put it in the test log.
- As Alice select an owned lesson and open AI assistance. Ask for clearer wording, read the reply, then ask a follow-up to make it shorter. Expected: distinct provider-generated results and interactive context, with the GUI responsive while waiting.
- Accept a suggestion into the draft and return without saving. Reopen persisted content: it must remain unchanged. Repeat and explicitly save a reviewed draft; the saved change should persist after restart.
- Exercise brainstorming and examples. Review retrieved citation links for examples and open them independently to check relevance. A link in generated prose alone is not proof of retrieved evidence.
- As Casey exercise refinement and search-term proposals; accept, edit and explicitly save. Verify Viewer discovery reflects saved terms.

### 5.2 Failures and stale access

- Run without a key and request assistance. Expected: configuration feedback; no canned success and no database changes.
- Cancel a slow request or navigate away before completion. Expected: no late result replaces the current workflow and no automatic save occurs.
- Attempt saving after a relevant protection or permission has changed. Expected: current rules reject the save and preserve stored content.
- Execute scripts/verify_live_ai.sh in the terminal with the same local configuration. Preserve its non-secret log separately and record the actual result. This optional live verifier is not part of the fixture-based passing total.

## 6 Input boundaries and native rendering

- Submit missing, empty and spaces-only required core fields. Try a 500-character title and a 501-character title. Expected: 500 accepted if otherwise valid; 501 rejected with corrective feedback before persistence.
- Try a 255-character team-member name and 256 characters; invalid IDs and a record removed since selection; negative/non-finite time values where the form allows typing. Expected: helpful rejection and unchanged saved counts/values.
- On the actual Eclipse desktop, inspect every role home, tables, details, statistics and AI dialogs at normal and enlarged window sizes. Verify text is readable, controls reachable, long lists scroll, errors are visible and navigation/logout works.
- Capture actual screenshots only after execution. Automated assertions and these procedures do not certify native display scaling or real network/provider behavior.

## 7 Automated correspondence

- Account and role procedures map to inherited account suites and TP3FoundationsTest.
- Contributor, ownership, validation and locks map to HW2TestingAutomation, TP2BoundaryCoverageTestingAutomation and ExperienceLockPolicyTestingAutomation.
- Discovery maps to TP3DiscoveryTest and TP3DiscoveryGuiTestingAutomation.
- Statistics maps to TP3StatisticsTest and TP3StatisticsGuiTestingAutomation.
- AI transport/service/GUI maps to OpenAiClientTest, AiAssistanceTest and TP3AiGuiTestingAutomation, with an explicitly synthetic provider fixture.
- Cross-feature persistence maps to TP3FinalIntegrationTest; actual named-module startup maps to TP3ModularSmoke. Exact executed commands and totals are in the Integration Report.
