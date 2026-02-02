# Research: Code Generation Support Tools

**Feature Branch**: `001-codegen-support-tools`
**Date**: 2026-02-02
**Status**: Complete

## Research Tasks

### 1. Tool Invocation Pattern

**Decision**: Extend gRPC ToolInvoker service pattern from camel-integration-capability

**Rationale**:
- CamelTool in camel-integration-capability demonstrates the exact pattern needed
- Uses `ToolInvokerGrpc.ToolInvokerImplBase` for gRPC service
- URI-based tool routing via McpSpec definitions
- ProducerTemplate for Camel endpoint invocation

**Alternatives Considered**:
- REST endpoints: Rejected - would break consistency with existing gRPC infrastructure
- Direct method calls: Rejected - not discoverable by Wanaku agents

### 2. Resource Download Pattern

**Decision**: Adapt ResourceDownloaderCallback with new TarBz2Downloader

**Rationale**:
- ResourceDownloaderCallback provides proven post-registration download mechanism
- DownloaderFactory pattern allows adding new scheme handlers
- Need new downloader to handle tar.bz2 extraction (existing downloaders copy files, don't extract)

**Alternatives Considered**:
- Pre-startup download: Rejected - resources may not exist until service registered
- Lazy download on first tool use: Rejected - adds latency to tool invocation

### 3. Tar.bz2 Extraction Library

**Decision**: Use Apache Commons Compress

**Rationale**:
- Already used in Java ecosystem for archive handling
- Supports bzip2 decompression and tar extraction
- Pure Java, no native dependencies
- Constitution allows well-established utility libraries

**Alternatives Considered**:
- JDK GZIPInputStream: Rejected - doesn't support bzip2
- Process builder calling `tar`: Rejected - platform-dependent, not portable

### 4. Properties File Reading

**Decision**: Use java.util.Properties

**Rationale**:
- Standard JDK class, no additional dependencies
- Simple key=value format matches specification
- Handles encoding, escaping, and comments

**Alternatives Considered**:
- Custom parser: Rejected - over-engineering for simple format
- YAML: Rejected - spec explicitly mentions properties file format

### 5. Tool Registration Mechanism

**Decision**: Reuse WanakuToolTransformer and WanakuToolRuleProcessor patterns

**Rationale**:
- Proven pattern from camel-integration-capability
- Handles registration with ServicesHttpClient
- Includes shutdown hook for deregistration
- Creates proper ToolReference objects

**Alternatives Considered**:
- Direct HTTP calls: Rejected - duplicates existing logic
- New registration mechanism: Rejected - would break consistency

### 6. Directory Convention Resolution

**Decision**: Resolve relative to properties file location

**Rationale**:
- Spec states: "kamelets directory...same directory as properties file"
- Spec states: "templates/orchestration.txt within same directory as properties file"
- Consistent with archive structure expectation

**Pattern**:
```
{extracted-dir}/
├── config.properties
├── kamelets/
│   └── *.kamelet.yaml
└── templates/
    └── orchestration.txt
```

### 7. Service Context Template

**Decision**: Use String.format with the template from issue-13.md

**Rationale**:
- Template is well-defined in the issue
- Simple %s placeholder for service list
- No complex templating needed

**Template**:
```
# Context
- The list below contains Kamelets that can be used to assemble the orchestration.
- A Kamelet is a snippet for a Camel route.
- It is a snippet that can be used to invoke the service containing the information you are looking for.
- It cannot be used on its own. It MUST be a part of an orchestration flow.
- Kamelets can have arguments. Read the Kamelets before you use them.
- If you don't know what to do, get help.

---
%s
```

## Technical Findings

### Existing CCE Infrastructure to Leverage

| Component | Location | Reuse For |
|-----------|----------|-----------|
| DataStoreDownloader | downloader/ | Fetching archive from datastore |
| DownloaderFactory | downloader/ | Adding TarBz2Downloader |
| ResourceType | downloader/ | Add new type for CODEGEN_PACKAGE |
| CamelEngineMain | root | Extend initialization flow |

### New Components Required

| Component | Purpose |
|-----------|---------|
| TarBz2Downloader | Download and extract .tar.bz2 archives |
| CodeGenToolService | gRPC service implementing the three tools |
| CodeGenToolRegistrar | Register tools with Wanaku after initialization |
| CodeGenResourceLoader | Load properties, kamelets, templates from extracted dir |

### Dependency Addition

```xml
<dependency>
    <groupId>org.apache.commons</groupId>
    <artifactId>commons-compress</artifactId>
    <version>1.26.0</version>
</dependency>
```

## Open Questions Resolved

| Question | Resolution |
|----------|------------|
| Where to add tools? | After initialization, same point as McpSpec creation in camel-integration-capability |
| How to handle missing package? | Log warning, continue without code gen tools |
| Kamelet file naming? | `{name}.kamelet.yaml` convention |
| Tool URI scheme? | `codegen://{tool-name}` |
