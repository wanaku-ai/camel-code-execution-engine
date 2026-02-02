# Tasks: Code Generation Support Tools

**Input**: Design documents from `/specs/001-codegen-support-tools/`
**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/

**Tests**: Test-first development per constitution (Principle III). Unit and integration tests included.

**Organization**: Tasks are grouped by user story to enable independent implementation and testing of each story.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2, US3)
- Include exact file paths in descriptions

## Path Conventions

- **Single project**: `src/main/java/ai/wanaku/code/engine/camel/` at repository root
- **Tests**: `src/test/java/ai/wanaku/code/engine/camel/`

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Project initialization and dependency configuration

- [x] T001 Add Apache Commons Compress dependency to pom.xml with version property
- [x] T002 [P] Create package directory src/main/java/ai/wanaku/code/engine/camel/codegen/
- [x] T003 [P] Create package directory src/main/java/ai/wanaku/code/engine/camel/codegen/tools/
- [x] T004 [P] Create test package directory src/test/java/ai/wanaku/code/engine/camel/codegen/
- [x] T005 [P] Create test package directory src/test/java/ai/wanaku/code/engine/camel/codegen/tools/
- [x] T006 [P] Create integration test directory src/test/java/ai/wanaku/code/engine/camel/integration/

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Core infrastructure that MUST be complete before ANY user story can be implemented

**⚠️ CRITICAL**: No user story work can begin until this phase is complete

### Tests (Foundational)

- [ ] T007 [P] Create test for ArchiveExtractor in src/test/java/ai/wanaku/code/engine/camel/util/ArchiveExtractorTest.java
- [ ] T008 [P] Create test for TarBz2Downloader in src/test/java/ai/wanaku/code/engine/camel/downloader/TarBz2DownloaderTest.java
- [ ] T009 [P] Create test for CodeGenConfig in src/test/java/ai/wanaku/code/engine/camel/codegen/CodeGenConfigTest.java
- [ ] T010 [P] Create test for CodeGenResourceLoader in src/test/java/ai/wanaku/code/engine/camel/codegen/CodeGenResourceLoaderTest.java

### Implementation (Foundational)

- [x] T011 Add CODEGEN_PACKAGE to ResourceType enum in src/main/java/ai/wanaku/code/engine/camel/downloader/ResourceType.java
- [x] T012 [P] Implement ArchiveExtractor utility in src/main/java/ai/wanaku/code/engine/camel/util/ArchiveExtractor.java
- [x] T013 Implement TarBz2Downloader in src/main/java/ai/wanaku/code/engine/camel/downloader/TarBz2Downloader.java (depends on T011, T012)
- [x] T014 Extend DownloaderFactory with TarBz2Downloader support in src/main/java/ai/wanaku/code/engine/camel/downloader/DownloaderFactory.java
- [x] T015 [P] Implement CodeGenConfig model in src/main/java/ai/wanaku/code/engine/camel/codegen/CodeGenConfig.java
- [x] T016 Implement CodeGenResourceLoader in src/main/java/ai/wanaku/code/engine/camel/codegen/CodeGenResourceLoader.java (depends on T015)

**Checkpoint**: Foundation ready - user story implementation can now begin in parallel

---

## Phase 3: User Story 1 - Search for Available Services (Priority: P1) 🎯 MVP

**Goal**: AI agents can discover available integration services (Kamelets) from the properties file

**Independent Test**: Invoke searchServicesTool and verify formatted service list is returned with context template

### Tests for User Story 1

> **NOTE: Write these tests FIRST, ensure they FAIL before implementation**

- [ ] T017 [P] [US1] Unit test for SearchServicesTool in src/test/java/ai/wanaku/code/engine/camel/codegen/tools/SearchServicesToolTest.java

### Implementation for User Story 1

- [x] T018 [US1] Implement SearchServicesTool in src/main/java/ai/wanaku/code/engine/camel/codegen/tools/SearchServicesTool.java
- [x] T019 [US1] Add context template constant for service list formatting in SearchServicesTool
- [x] T020 [US1] Add configurable description support reading from properties in SearchServicesTool

**Checkpoint**: At this point, SearchServicesTool should be fully functional and testable independently

---

## Phase 4: User Story 2 - Read Kamelet Specification (Priority: P1)

**Goal**: AI agents can retrieve complete Kamelet YAML content by name

**Independent Test**: Request a specific Kamelet by name and verify YAML content is returned

### Tests for User Story 2

- [ ] T021 [P] [US2] Unit test for ReadKameletTool in src/test/java/ai/wanaku/code/engine/camel/codegen/tools/ReadKameletToolTest.java

### Implementation for User Story 2

- [x] T022 [US2] Implement ReadKameletTool in src/main/java/ai/wanaku/code/engine/camel/codegen/tools/ReadKameletTool.java
- [x] T023 [US2] Add kamelet file resolution logic (name → {name}.kamelet.yaml) in ReadKameletTool
- [x] T024 [US2] Add error handling for missing/invalid kamelet names in ReadKameletTool

**Checkpoint**: At this point, ReadKameletTool should be fully functional and testable independently

---

## Phase 5: User Story 3 - Generate Orchestration Code (Priority: P1)

**Goal**: AI agents can retrieve the orchestration template for code generation

**Independent Test**: Invoke generateOrchestrationCode and verify template content is returned

### Tests for User Story 3

- [ ] T025 [P] [US3] Unit test for GenerateOrchestrationTool in src/test/java/ai/wanaku/code/engine/camel/codegen/tools/GenerateOrchestrationToolTest.java

### Implementation for User Story 3

- [x] T026 [US3] Implement GenerateOrchestrationTool in src/main/java/ai/wanaku/code/engine/camel/codegen/tools/GenerateOrchestrationTool.java
- [x] T027 [US3] Add template file reading from templates/orchestration.txt in GenerateOrchestrationTool
- [x] T028 [US3] Add error handling for missing template file in GenerateOrchestrationTool

**Checkpoint**: All three tool implementations should now be independently functional

---

## Phase 6: User Story 4 - Download Code Generation Package (Priority: P2)

**Goal**: CCE automatically downloads and extracts the code generation package from the data store

**Independent Test**: Configure datastore URI and verify package is downloaded/extracted after registration

### Tests for User Story 4

- [ ] T029 [P] [US4] Integration test for package download in src/test/java/ai/wanaku/code/engine/camel/integration/CodeGenPackageDownloadTest.java

### Implementation for User Story 4

- [ ] T030 [US4] Add --codegen-package CLI option to CamelEngineMain in src/main/java/ai/wanaku/code/engine/camel/CamelEngineMain.java
- [ ] T031 [US4] Add codegen package to ResourceListBuilder in CamelEngineMain
- [ ] T032 [US4] Handle download failure gracefully (log warning, continue without tools) in CamelEngineMain
- [ ] T033 [US4] Add CODEGEN_PACKAGE environment variable support to Dockerfile

**Checkpoint**: Package download should now be integrated into startup flow

---

## Phase 7: User Story 5 - Register Tools with Wanaku (Priority: P2)

**Goal**: Three tools are registered with Wanaku after initialization and deregistered on shutdown

**Independent Test**: Start CCE and verify all three tools appear in Wanaku's tool registry

### Tests for User Story 5

- [ ] T034 [P] [US5] Unit test for CodeGenToolRegistrar in src/test/java/ai/wanaku/code/engine/camel/codegen/CodeGenToolRegistrarTest.java
- [ ] T035 [P] [US5] Unit test for CodeGenToolService in src/test/java/ai/wanaku/code/engine/camel/codegen/CodeGenToolServiceTest.java

### Implementation for User Story 5

- [ ] T036 [US5] Implement CodeGenToolRegistrar in src/main/java/ai/wanaku/code/engine/camel/codegen/CodeGenToolRegistrar.java
- [ ] T037 [US5] Add tool definitions for all three tools (searchServicesTool, readKamelet, generateOrchestrationCode) in CodeGenToolRegistrar
- [ ] T038 [US5] Add shutdown hook for tool deregistration in CodeGenToolRegistrar
- [ ] T039 [US5] Implement CodeGenToolService extending ToolInvokerGrpc.ToolInvokerImplBase in src/main/java/ai/wanaku/code/engine/camel/codegen/CodeGenToolService.java
- [ ] T040 [US5] Add URI-based routing to appropriate tool handler in CodeGenToolService
- [ ] T041 [US5] Add error handling consistent with existing gRPC patterns in CodeGenToolService
- [ ] T042 [US5] Add CodeGenToolService to gRPC server in CamelEngineMain
- [ ] T043 [US5] Integrate CodeGenToolRegistrar with initialization flow in CamelEngineMain

**Checkpoint**: All tools should be registered and functional via gRPC

---

## Phase 8: Polish & Cross-Cutting Concerns

**Purpose**: Improvements that affect multiple user stories

- [ ] T044 [P] Create sample code generation package for testing in src/test/resources/codegen-package/
- [ ] T045 [P] Create integration test for full tool invocation flow in src/test/java/ai/wanaku/code/engine/camel/integration/CodeGenToolIntegrationTest.java
- [ ] T046 Add Javadoc to all public classes and methods in codegen package
- [ ] T047 Run mvn spotless:apply to format all new code
- [ ] T048 Verify all tests pass with mvn test
- [ ] T049 Update quickstart.md with actual tested examples

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies - can start immediately
- **Foundational (Phase 2)**: Depends on Setup completion - BLOCKS all user stories
- **User Stories (Phase 3-7)**: All depend on Foundational phase completion
  - US1, US2, US3 can proceed in parallel (tool implementations)
  - US4, US5 depend on tool implementations for integration
- **Polish (Phase 8)**: Depends on all user stories being complete

### User Story Dependencies

- **User Story 1 (P1)**: Can start after Foundational (Phase 2) - No dependencies on other stories
- **User Story 2 (P1)**: Can start after Foundational (Phase 2) - No dependencies on other stories
- **User Story 3 (P1)**: Can start after Foundational (Phase 2) - No dependencies on other stories
- **User Story 4 (P2)**: Can start after Foundational (Phase 2) - Independent but provides package for tools
- **User Story 5 (P2)**: Depends on US1, US2, US3 (needs tools to register)

### Within Each User Story

- Tests MUST be written and FAIL before implementation (Constitution Principle III)
- Core implementation before error handling
- Story complete before moving to next priority

### Parallel Opportunities

- **Phase 1**: T002-T006 can run in parallel (directory creation)
- **Phase 2**: T007-T010 can run in parallel (test files); T012, T015 can run in parallel (implementation)
- **Phase 3-5**: US1, US2, US3 can be worked on in parallel by different developers
- **Phase 7**: T034, T035 can run in parallel (test files)

---

## Parallel Example: Foundational Tests

```bash
# Launch all foundational tests together:
Task: "Create test for ArchiveExtractor in src/test/java/.../util/ArchiveExtractorTest.java"
Task: "Create test for TarBz2Downloader in src/test/java/.../downloader/TarBz2DownloaderTest.java"
Task: "Create test for CodeGenConfig in src/test/java/.../codegen/CodeGenConfigTest.java"
Task: "Create test for CodeGenResourceLoader in src/test/java/.../codegen/CodeGenResourceLoaderTest.java"
```

## Parallel Example: Tool Implementations (US1, US2, US3)

```bash
# Developer A - User Story 1:
Task: "Unit test for SearchServicesTool"
Task: "Implement SearchServicesTool"

# Developer B - User Story 2:
Task: "Unit test for ReadKameletTool"
Task: "Implement ReadKameletTool"

# Developer C - User Story 3:
Task: "Unit test for GenerateOrchestrationTool"
Task: "Implement GenerateOrchestrationTool"
```

---

## Implementation Strategy

### MVP First (User Stories 1-3 Only)

1. Complete Phase 1: Setup
2. Complete Phase 2: Foundational (CRITICAL - blocks all stories)
3. Complete Phase 3: User Story 1 (searchServicesTool)
4. Complete Phase 4: User Story 2 (readKamelet)
5. Complete Phase 5: User Story 3 (generateOrchestrationCode)
6. **STOP and VALIDATE**: Test all three tools independently
7. Continue to US4, US5 for full integration

### Incremental Delivery

1. Complete Setup + Foundational → Infrastructure ready
2. Add US1, US2, US3 → Three working tools (can test manually)
3. Add US4 → Package download integrated
4. Add US5 → Tools registered with Wanaku (full integration)
5. Add Polish → Production ready

### Parallel Team Strategy

With 3 developers after Foundational phase:

1. **Developer A**: User Story 1 → User Story 4
2. **Developer B**: User Story 2 → User Story 5 (CodeGenToolService)
3. **Developer C**: User Story 3 → User Story 5 (CodeGenToolRegistrar)

---

## Notes

- [P] tasks = different files, no dependencies
- [Story] label maps task to specific user story for traceability
- Each user story should be independently completable and testable
- Verify tests fail before implementing (Red-Green-Refactor per Constitution)
- Run `mvn spotless:apply` before commits
- Commit after each task or logical group
