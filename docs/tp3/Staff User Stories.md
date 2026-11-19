# Staff User Stories

CSE 360 Team Project Phase 3. This outline separates received role stories from acceptance details derived for implementation. TP1 and TP2 are inherited behavior; their regression coverage remains required. Bullet points intentionally replace story IDs.

## 1 Inherited behavior

### 1.1 Administrator and account workflows from TP1

- Administrators manage accounts, assign and remove roles, list accounts and issue one-time passwords.
- Administrators cannot delete themselves or remove their own Admin assignment. One-time passwords require replacement before role entry.
- Existing account data and login behavior remain persistent. The final increment extends dispatch and assignment to Contributor, Viewer and Curator.

### 1.2 Contributor workflows from TP2

- A Contributor creates, reads, edits and deletes owned lessons, supplies required core content and records course experience information.
- Contributor lists contain only that Contributor's lessons. Ownership, current role, validation and lock policies govern all service mutations.
- Experience includes problem-solving attempts, elapsed time and team-member effort. Existing data survives separate executions.

## 2 Phase 3 role stories

### 2.1 Contributor assistance and shared statistics

- As a Contributor, I want interactive AI help writing a clear lesson, brainstorming approaches and finding examples so I can improve a lesson without surrendering control of its content.
- I can refine a request through follow-up messages and review generated suggestions before accepting them into a draft. Accepting a draft does not save it.
- My selected lesson context must be owned by me. A later save obeys current ownership, role, input validation and protection rules.
- As a Contributor, I want to see useful statistics that a Curator has explicitly made visible for my lesson, while other Contributors' raw experience evidence stays private.
- Derived acceptance: hidden summaries stay hidden; sharing does not reveal source owners, member names or original experience records. Removing sharing or changing ownership/role is checked on subsequent requests.

### 2.2 Viewer discovery

- As a Viewer, I want to browse and search core Lessons Learned information so I can find useful guidance.
- I can select a result and read its core content. Viewer results exclude experience details and mutation controls.
- Derived acceptance: searches use case-insensitive terms across core text and Curator-saved search terms; all entered terms must match. Clearing search restores all visible core lessons. An empty result is handled without error.
- Derived acceptance: results reflect subsequent saves and deletions. Having a different role or a stale account object never authorizes Viewer service access.

### 2.3 Curator review and refinement

- As a Curator, I want to review lessons and associated experience across Contributors so I can improve the repository's quality.
- I can refine lesson core content, review and maintain helpful search terms, and protect core fields or experience information from later Contributor changes.
- I want AI assistance with lesson refinement and search-term proposals, with explicit review and acceptance before anything is saved.
- Derived acceptance: refinements preserve lesson identity, owner, unrelated experience and existing lock state. Protection changes are persistent and enforced in services, including calls made without the GUI.
- Derived acceptance: search terms are trimmed, normalized and deduplicated; invalid replacement leaves the previous saved set unchanged. An explicit empty replacement clears the terms.

### 2.4 Curator statistics and visibility

- As a Curator, I want to compute and save summary statistics from multiple problem-solving attempts so I can compare elapsed time and team effort using recorded evidence.
- I can choose sources, preview results, save a historical summary and control whether a Contributor sees the summary for the destination lesson.
- Derived acceptance: at least two distinct attempts are required. Count, mean, median, minimum and maximum are calculated for elapsed time and summed person-minutes per attempt.
- Derived acceptance: sources changed after review require a new preview. A save is atomic and stores reviewed historical inputs. Deleting a source preserves historical values and marks the live relationship unavailable; deleting the destination removes its dependent summaries.
- Derived acceptance: summaries start hidden. Contributor access is limited to explicitly shared aggregates for owned destination lessons.

## 3 Implementation-wide acceptance

- Validate missing, empty, whitespace-only, invalid and boundary input before persistence. Errors explain how to correct the input.
- Reject nonexistent IDs, unauthorized roles, foreign ownership and prohibited locked operations without changing stored data or relationships.
- Persist roles, lessons, terms, locks, experiences, summary snapshots and visibility across application executions. Tests use separate file databases and isolated GUI homes.
- Keep full collections and filtered subsets consistent after operations. No fixed collection-size ceiling is introduced.
- Provide Javadoc, internal explanations where needed, automated positive and negative checks, manual test procedures and architecture/design diagrams matching actual classes.

## 4 Working code versus anticipation

- Working Phase 3 code includes Viewer discovery, Curator core and protection operations, persistent statistics and sharing, and interactive AI assistance.
- OpenAI is the chosen provider adapter. The supplied stories do not mandate OpenAI, a specific vendor or an API key by name. A live provider credential and a successful real-provider run remain necessary to certify that part of acceptance.
- Future LessonShare integration is an architectural anticipation only; no external LessonShare connection is claimed.
- Team planning, stand-up evidence, final screencasts, team norms and individual peer evaluation are separate submission obligations, not application features.

## Sources

- Supplied final Phase 3 specification, particularly Tasks 3 through 8.
- Lessons Learned System Initial User Stories V1, August 19 2026, pages 1 through 3.
- Actual service contracts and tests in the tp3 branch. Derived acceptance details above describe the implemented decisions rather than additional quotations from the assignment.
