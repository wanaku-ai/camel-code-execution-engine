# Data Model: Code Generation Support Tools

**Feature Branch**: `001-codegen-support-tools`
**Date**: 2026-02-02

## Entities

### CodeGenConfig

Properties file configuration for code generation tools.

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| availableServices | List<String> | Yes | Comma-separated list of available services |
| searchToolDescription | String | No | Custom description for searchServicesTool |

**Source**: Loaded from `config.properties` in extracted package

**Properties File Format**:
```properties
available.services=kamelet:service1,kamelet:service2,kamelet:service3
search.tool.description=Custom description for search tool
```

### Kamelet

A Camel route snippet definition loaded from YAML file.

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| name | String | Yes | Kamelet identifier (derived from filename) |
| content | String | Yes | Raw YAML content |
| filePath | Path | Yes | Absolute path to the file |

**Naming Convention**: `{name}.kamelet.yaml`

### OrchestrationTemplate

Template file for generating orchestration code.

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| content | String | Yes | Raw template content |
| filePath | Path | Yes | Absolute path to the file |

**Location Convention**: `templates/orchestration.txt`

### CodeGenPackage

Archive containing all code generation resources.

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| sourceUri | URI | Yes | Datastore URI (e.g., `datastore://code-gen-package.tar.bz`) |
| extractedPath | Path | Yes | Local path where extracted |
| configPath | Path | Yes | Path to properties file |
| kameletsDir | Path | Yes | Path to kamelets directory |
| templatesDir | Path | Yes | Path to templates directory |

**Archive Structure**:
```
code-gen-package.tar.bz/
├── config.properties
├── kamelets/
│   ├── http-source.kamelet.yaml
│   ├── kafka-sink.kamelet.yaml
│   └── ...
└── templates/
    └── orchestration.txt
```

### CodeGenTool

Tool registration with Wanaku.

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| name | String | Yes | Tool identifier (searchServicesTool, readKamelet, generateOrchestrationCode) |
| description | String | Yes | Tool description for agents |
| uri | String | Yes | Tool invocation URI (e.g., `codegen://searchServicesTool`) |
| inputSchema | InputSchema | Yes | Parameters accepted by the tool |

## Entity Relationships

```
CodeGenPackage (1) ──contains──> (1) CodeGenConfig
CodeGenPackage (1) ──contains──> (*) Kamelet
CodeGenPackage (1) ──contains──> (1) OrchestrationTemplate

CodeGenConfig ──configures──> CodeGenTool[searchServicesTool]
Kamelet ──accessed-by──> CodeGenTool[readKamelet]
OrchestrationTemplate ──accessed-by──> CodeGenTool[generateOrchestrationCode]
```

## State Transitions

### Package Lifecycle

```
[Not Downloaded]
    ── registration complete ──> [Downloading]
    ── download success ──> [Extracting]
    ── extract success ──> [Ready]
    ── download/extract failure ──> [Failed]
```

### Tool Lifecycle

```
[Not Registered]
    ── package ready ──> [Registering]
    ── registration success ──> [Active]
    ── registration failure ──> [Failed]
    ── shutdown signal ──> [Deregistering]
    ── deregistration complete ──> [Terminated]
```

## Validation Rules

### CodeGenConfig
- `available.services` must be non-null (can be empty)
- Each service entry must follow format `kamelet:{name}` or similar

### Kamelet
- Filename must end with `.kamelet.yaml`
- Content must be valid YAML
- Name derived by removing `.kamelet.yaml` suffix

### OrchestrationTemplate
- File must exist at `templates/orchestration.txt`
- Content must be non-empty

### CodeGenPackage
- Archive must be valid bzip2-compressed tar
- Must contain `config.properties` at root
- `kamelets/` directory must exist (can be empty)
- `templates/` directory must exist
- `templates/orchestration.txt` must exist
