# Code Generation Tools API Contract

**Feature Branch**: `001-codegen-support-tools`
**Date**: 2026-02-02

## Overview

These tools are exposed via gRPC through the existing ToolInvoker service. Tools are invoked using the standard `ToolInvokeRequest`/`ToolInvokeReply` messages defined in the Wanaku SDK.

## Tool Definitions

### searchServicesTool

**URI**: `codegen://searchServicesTool`

**Description**: (Configurable) Default: "Searches for services to perform the tasks"

**Input Schema**:
```json
{
  "type": "object",
  "properties": {},
  "required": []
}
```

**Response**:
```
# Context
- The list below contains Kamelets that can be used to assemble the orchestration.
- A Kamelet is a snippet for a Camel route.
- It is a snippet that can be used to invoke the service containing the information you are looking for.
- It cannot be used on its own. It MUST be a part of an orchestration flow.
- Kamelets can have arguments. Read the Kamelets before you use them.
- If you don't know what to do, get help.

---
kamelet:service1
kamelet:service2
kamelet:service3
```

**Error Responses**:
| Condition | Error Message |
|-----------|---------------|
| Properties file not found | "Code generation package not loaded" |
| Properties file malformed | "Invalid configuration: {details}" |

---

### readKamelet

**URI**: `codegen://readKamelet`

**Description**: "Reads the content of a Kamelet by name"

**Input Schema**:
```json
{
  "type": "object",
  "properties": {
    "name": {
      "type": "string",
      "description": "The name of the Kamelet to read (without .kamelet.yaml suffix)"
    }
  },
  "required": ["name"]
}
```

**Response**: Raw YAML content of the requested Kamelet

**Example Request**:
```json
{
  "arguments": {
    "name": "http-source"
  }
}
```

**Example Response**:
```yaml
apiVersion: camel.apache.org/v1alpha1
kind: Kamelet
metadata:
  name: http-source
spec:
  definition:
    title: HTTP Source
    description: Periodically fetches data from an HTTP endpoint
    properties:
      url:
        title: URL
        description: The URL to fetch
        type: string
  template:
    from:
      uri: timer:tick
      steps:
        - to:
            uri: "http://{{url}}"
```

**Error Responses**:
| Condition | Error Message |
|-----------|---------------|
| Kamelet not found | "Kamelet '{name}' not found" |
| Invalid Kamelet name | "Invalid Kamelet name: {name}" |
| Kamelets directory missing | "Code generation package not loaded" |

---

### generateOrchestrationCode

**URI**: `codegen://generateOrchestrationCode`

**Description**: "Returns the orchestration template for code generation"

**Input Schema**:
```json
{
  "type": "object",
  "properties": {},
  "required": []
}
```

**Response**: Raw content of the orchestration template file

**Error Responses**:
| Condition | Error Message |
|-----------|---------------|
| Template file not found | "Orchestration template not found" |
| Code gen package not loaded | "Code generation package not loaded" |

---

## Tool Registration Schema

Tools are registered with Wanaku using the following structure:

```json
{
  "name": "searchServicesTool",
  "description": "Searches for services to perform the tasks",
  "uri": "codegen://searchServicesTool",
  "type": "codegen",
  "namespace": "ai.wanaku.codegen",
  "inputSchema": {
    "type": "object",
    "properties": {},
    "required": []
  }
}
```

## gRPC Message Reference

Uses existing Wanaku SDK messages:

### ToolInvokeRequest
```protobuf
message ToolInvokeRequest {
  string uri = 1;
  string body = 2;
  map<string, string> arguments = 3;
}
```

### ToolInvokeReply
```protobuf
message ToolInvokeReply {
  string content = 1;
  string error = 2;
  bool is_error = 3;
}
```

## Invocation Flow

```
Agent
  │
  ▼ ToolInvokeRequest(uri="codegen://searchServicesTool")
  │
Wanaku Router
  │
  ▼ Route to CCE based on tool registration
  │
CCE (CodeGenToolService)
  │
  ├── Parse URI to extract tool name
  ├── Dispatch to appropriate handler
  ├── Read from extracted package resources
  └── Return ToolInvokeReply
```
