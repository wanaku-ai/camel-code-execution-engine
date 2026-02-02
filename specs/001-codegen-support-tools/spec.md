# Feature Specification: Code Generation Support Tools

**Feature Branch**: `001-codegen-support-tools`
**Created**: 2026-02-02
**Status**: Draft
**Input**: User description: "Add support tools for code generation assistance"
**GitHub Issue**: #13

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Search for Available Services (Priority: P1)

An AI agent needs to discover what integration services (Kamelets) are available to perform a task. The agent invokes the searchServicesTool which reads a properties file containing the available services list and returns them with context about how to use Kamelets in orchestrations.

**Why this priority**: Searching is the first step - agents cannot parameterize or assemble routes without knowing what services exist.

**Independent Test**: Can be fully tested by invoking the searchServicesTool and verifying it returns the formatted service list from the properties file.

**Acceptance Scenarios**:

1. **Given** a properties file exists with `available.services=kamelet:service1,kamelet:service2`, **When** an agent invokes searchServicesTool, **Then** the system returns the services interpolated into the context template with usage instructions.
2. **Given** the properties file contains no available.services, **When** an agent invokes searchServicesTool, **Then** the system returns an empty list within the context template.
3. **Given** the tool description is configured via properties, **When** the tool is registered with Wanaku, **Then** it uses the configured description instead of the default.

---

### User Story 2 - Read Kamelet Specification (Priority: P1)

An AI agent needs to understand the structure and parameters of a specific Kamelet to use it correctly. The agent invokes the readKamelet tool with a Kamelet name, and receives the complete Kamelet YAML content from the kamelets directory.

**Why this priority**: Reading Kamelet definitions is essential for parameterization - agents must understand the structure before assembling routes.

**Independent Test**: Can be fully tested by requesting a specific Kamelet by name and verifying the YAML content is returned.

**Acceptance Scenarios**:

1. **Given** a Kamelet file `http-source.kamelet.yaml` exists in the kamelets directory, **When** an agent requests "http-source", **Then** the system returns the complete YAML content of that Kamelet.
2. **Given** an agent requests a Kamelet that does not exist, **When** the request completes, **Then** the system returns a clear error message.
3. **Given** the kamelets directory is resolved by convention (sibling to properties file), **When** the tool is invoked, **Then** it reads from the correct directory.

---

### User Story 3 - Generate Orchestration Code (Priority: P1)

An AI agent needs a template for generating orchestration code. The agent invokes the generateOrchestrationCode tool and receives the content of the orchestration template file.

**Why this priority**: The template provides the structure agents need to assemble final routes - this is the culmination of the code generation workflow.

**Independent Test**: Can be fully tested by invoking the generateOrchestrationCode tool and verifying the template content is returned.

**Acceptance Scenarios**:

1. **Given** a template file exists at `templates/orchestration.txt`, **When** an agent invokes generateOrchestrationCode, **Then** the system returns the complete template content.
2. **Given** the template file does not exist, **When** the tool is invoked, **Then** the system returns an appropriate error message.
3. **Given** the templates directory is resolved by convention (relative to properties file), **When** the tool is invoked, **Then** it reads from the correct path.

---

### User Story 4 - Download Code Generation Package (Priority: P2)

The CCE downloads a code generation package (tar.bz2 archive) from the Wanaku data store after service registration. This package contains the properties file, kamelets directory, and templates directory needed by the three tools.

**Why this priority**: Package download is a prerequisite for tool operation but runs automatically during initialization.

**Independent Test**: Can be tested by configuring a datastore URI and verifying the package is downloaded and extracted after registration.

**Acceptance Scenarios**:

1. **Given** a datastore URI `datastore://code-gen-package.tar.bz`, **When** the service completes registration, **Then** the archive is downloaded and extracted to the data directory.
2. **Given** the datastore is unavailable, **When** download is attempted, **Then** the system logs an error and continues without code generation tools.
3. **Given** the archive is successfully extracted, **When** the tools are registered, **Then** they can access the properties file, kamelets directory, and templates directory.

---

### User Story 5 - Register Tools with Wanaku (Priority: P2)

After initialization completes and resources are downloaded, the CCE registers the three code generation tools (searchServicesTool, readKamelet, generateOrchestrationCode) with the Wanaku instance.

**Why this priority**: Tool registration makes the tools discoverable by AI agents through Wanaku.

**Independent Test**: Can be tested by starting the CCE and verifying all three tools appear in Wanaku's tool registry.

**Acceptance Scenarios**:

1. **Given** the CCE starts and downloads resources, **When** initialization completes, **Then** all three tools are registered with Wanaku.
2. **Given** tools are registered, **When** an agent queries Wanaku for available tools, **Then** searchServicesTool, readKamelet, and generateOrchestrationCode are listed.
3. **Given** the CCE shuts down, **When** the shutdown hook runs, **Then** the tools are deregistered from Wanaku.

---

### Edge Cases

- What happens when the properties file is malformed or missing required keys?
- How does the system handle Kamelet files with invalid YAML syntax?
- What happens if the code generation package archive is corrupted?
- How does the system handle concurrent requests to the same tool?

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: System MUST create and register a searchServicesTool at startup that reads services from a properties file.
- **FR-002**: System MUST return service list interpolated into the context template describing Kamelet usage.
- **FR-003**: System MUST support configurable tool description via properties, defaulting to "Searches for services to perform the tasks".
- **FR-004**: System MUST create and register a readKamelet tool that returns Kamelet YAML content by name.
- **FR-005**: System MUST resolve kamelets directory by convention (sibling to properties file).
- **FR-006**: System MUST create and register a generateOrchestrationCode tool that returns template file content.
- **FR-007**: System MUST resolve templates directory by convention (relative to properties file).
- **FR-008**: System MUST download the code generation package from Wanaku data store after service registration.
- **FR-009**: System MUST extract tar.bz2 archives to the data directory.
- **FR-010**: System MUST register all three tools after initialization completes.
- **FR-011**: System MUST deregister tools on shutdown.

### Key Entities

- **Code Generation Package**: A tar.bz2 archive from the data store containing properties file, kamelets directory, and templates directory.
- **Properties File**: Configuration file listing available services (`available.services=...`) and optional tool descriptions.
- **Kamelet**: A YAML file defining a Camel route snippet with parameters.
- **Orchestration Template**: A text file providing the structure for assembling routes.
- **Tool Registration**: The process of adding tools to Wanaku's tool registry.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: All three tools (searchServicesTool, readKamelet, generateOrchestrationCode) are registered with Wanaku after startup.
- **SC-002**: searchServicesTool returns the complete service list with context template in under 500ms.
- **SC-003**: readKamelet returns Kamelet YAML content in under 200ms.
- **SC-004**: generateOrchestrationCode returns template content in under 200ms.
- **SC-005**: Code generation package download and extraction completes within startup timeout.
- **SC-006**: Tools are successfully deregistered on graceful shutdown.

## Assumptions

- The code generation package follows a standard structure: properties file at root, kamelets/ subdirectory, templates/ subdirectory.
- The Wanaku data store endpoint is available and the package exists at the configured URI.
- The existing ResourceDownloaderCallback pattern from camel-integration-capability can be adapted for downloading and extracting packages.
- Tools are registered using the same WanakuToolTransformer/WanakuToolRuleProcessor pattern as camel-integration-capability.

## Dependencies

- Existing CCE gRPC server infrastructure
- Existing DataStoreDownloader for fetching from Wanaku data store
- Apache Commons Compress or similar for tar.bz2 extraction
- Wanaku SDK ServicesHttpClient for tool registration
- Pattern references from camel-integration-capability:
  - CamelTool for gRPC service implementation
  - ResourceDownloaderCallback for post-registration downloads
  - McpSpec/McpRulesManager for tool specification loading
