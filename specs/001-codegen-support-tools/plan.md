# Implementation Plan: Code Generation Support Tools

**Branch**: `001-codegen-support-tools` | **Date**: 2026-02-02 | **Spec**: [spec.md](./spec.md)
**Input**: Feature specification from `/specs/001-codegen-support-tools/spec.md`

## Summary

Add three code generation tools (searchServicesTool, readKamelet, generateOrchestrationCode) to CCE that AI agents can use for code generation assistance. Tools are registered with Wanaku after initialization and resources are downloaded from the data store as a tar.bz2 package.

## Technical Context

**Language/Version**: Java 21
**Primary Dependencies**: Apache Camel 4.17.0, Wanaku SDK 0.1.0-SNAPSHOT, Apache Commons Compress, gRPC
**Storage**: Local filesystem (extracted package in data directory)
**Testing**: JUnit 5
**Target Platform**: Linux server, Docker container
**Project Type**: Single Maven project
**Performance Goals**: Tool responses < 500ms, startup < 30s with package download
**Constraints**: No Spring/Quarkus/Lombok per constitution, plain Java only
**Scale/Scope**: 3 tools, single package download, ~10 files modified/added

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principle | Status | Notes |
|-----------|--------|-------|
| I. Plain Java Only | ✅ PASS | No frameworks used; Apache Commons Compress is utility library |
| II. Code Quality Standards | ✅ PASS | Palantir format, Javadoc required, single responsibility |
| III. Test-First Development | ✅ PASS | Tests planned before implementation |
| IV. User Experience Consistency | ✅ PASS | gRPC responses consistent with existing tools |
| V. Performance Requirements | ✅ PASS | Lazy initialization, proper resource cleanup |

**Permitted Technologies Used**:
- Java 21 ✅
- Apache Camel ✅ (already in project)
- Wanaku SDK ✅ (already in project)
- SLF4J + Log4j2 ✅ (already in project)
- JUnit 5 ✅ (already in project)

**New Dependency Required**:
- Apache Commons Compress: Justified for tar.bz2 extraction (standard utility library)

## Project Structure

### Documentation (this feature)

```text
specs/001-codegen-support-tools/
├── plan.md              # This file
├── research.md          # Research findings
├── data-model.md        # Entity definitions
├── quickstart.md        # Usage guide
├── contracts/           # API contracts
│   └── tools-api.md     # Tool invocation contract
└── tasks.md             # Implementation tasks (via /speckit.tasks)
```

### Source Code (repository root)

```text
src/main/java/ai/wanaku/code/engine/camel/
├── CamelEngineMain.java                    # Modify: add codegen package CLI option
├── codegen/                                # NEW: Code generation package
│   ├── CodeGenToolService.java             # gRPC service for the 3 tools
│   ├── CodeGenResourceLoader.java          # Load properties, kamelets, template
│   ├── CodeGenToolRegistrar.java           # Register/deregister tools with Wanaku
│   ├── CodeGenConfig.java                  # Properties file model
│   └── tools/                              # Tool implementations
│       ├── SearchServicesTool.java         # searchServicesTool impl
│       ├── ReadKameletTool.java            # readKamelet impl
│       └── GenerateOrchestrationTool.java  # generateOrchestrationCode impl
├── downloader/
│   ├── DownloaderFactory.java              # Modify: add TarBz2Downloader
│   ├── TarBz2Downloader.java               # NEW: Download & extract archives
│   └── ResourceType.java                   # Modify: add CODEGEN_PACKAGE type
└── util/
    └── ArchiveExtractor.java               # NEW: Tar.bz2 extraction utility

src/test/java/ai/wanaku/code/engine/camel/
├── codegen/
│   ├── CodeGenToolServiceTest.java         # Unit tests
│   ├── CodeGenResourceLoaderTest.java      # Unit tests
│   └── tools/
│       ├── SearchServicesToolTest.java     # Unit tests
│       ├── ReadKameletToolTest.java        # Unit tests
│       └── GenerateOrchestrationToolTest.java # Unit tests
├── downloader/
│   └── TarBz2DownloaderTest.java           # Unit tests
└── integration/
    └── CodeGenToolIntegrationTest.java     # Integration tests
```

**Structure Decision**: Extend existing single-project Maven structure. New `codegen` package for all code generation tool functionality. Tests follow existing `src/test/java` structure.

## Implementation Phases

### Phase 1: Infrastructure (Downloader Extension)

1. Add Apache Commons Compress dependency to pom.xml
2. Add ResourceType.CODEGEN_PACKAGE enum value
3. Implement ArchiveExtractor utility for tar.bz2
4. Implement TarBz2Downloader
5. Extend DownloaderFactory to support tar.bz2

### Phase 2: Resource Loading

1. Implement CodeGenConfig model
2. Implement CodeGenResourceLoader
   - Load properties file
   - Resolve kamelets directory
   - Resolve templates directory
   - Cache kamelet contents

### Phase 3: Tool Implementations

1. Implement SearchServicesTool
   - Read available.services from properties
   - Format with context template
   - Configurable description
2. Implement ReadKameletTool
   - Find kamelet by name
   - Return YAML content
3. Implement GenerateOrchestrationTool
   - Return template file content

### Phase 4: gRPC Service & Registration

1. Implement CodeGenToolService (extends ToolInvokerGrpc.ToolInvokerImplBase)
   - Route by URI to appropriate tool
   - Handle errors consistently
2. Implement CodeGenToolRegistrar
   - Register tools after initialization
   - Deregister on shutdown
3. Modify CamelEngineMain
   - Add --codegen-package CLI option
   - Integrate with ResourceDownloaderCallback
   - Add CodeGenToolService to gRPC server

### Phase 5: Testing & Integration

1. Unit tests for all components
2. Integration test with sample package
3. Manual verification with grpcurl

## Component Interactions

```
CamelEngineMain
    │
    ├── ResourceDownloaderCallback
    │       │
    │       └── TarBz2Downloader ──> ArchiveExtractor
    │                                     │
    │                                     ▼
    │                              [Extracted Package]
    │                                     │
    │                                     ▼
    ├── CodeGenResourceLoader ────────────┘
    │       │
    │       ├── CodeGenConfig (properties)
    │       ├── Kamelets (YAML files)
    │       └── OrchestrationTemplate (text file)
    │
    ├── CodeGenToolRegistrar ──> ServicesHttpClient ──> Wanaku
    │
    └── gRPC Server
            │
            └── CodeGenToolService
                    │
                    ├── SearchServicesTool
                    ├── ReadKameletTool
                    └── GenerateOrchestrationTool
```

## Complexity Tracking

> No constitution violations identified.

| Violation | Why Needed | Simpler Alternative Rejected Because |
|-----------|------------|-------------------------------------|
| N/A | N/A | N/A |

## Risks & Mitigations

| Risk | Impact | Mitigation |
|------|--------|------------|
| Commons Compress adds transitive dependencies | Build bloat | Exclude unused transitive deps in pom.xml |
| Archive extraction security (path traversal) | Security vulnerability | Validate extracted paths stay within data dir |
| Large kamelets directory | Memory pressure | Lazy load kamelet content on request |

## Next Steps

1. Run `/speckit.tasks` to generate detailed implementation tasks
2. Implement following test-first development (Red-Green-Refactor)
3. Run integration tests before PR
