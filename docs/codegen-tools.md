# Code Generation Tools

The Code Execution Engine provides built-in tools for code generation that can be registered with Wanaku and invoked by AI agents.

## Overview

When configured with a code generation package, the engine registers three tools:

| Tool | Description |
|------|-------------|
| `searchServicesTool` | Lists available services (Kamelets) that can be used in orchestrations |
| `readKamelet` | Reads the YAML content of a specific Kamelet by name |
| `generateOrchestrationCode` | Returns the orchestration template for code generation |

## Configuration

### Command Line Option

```bash
java -jar camel-code-execution-engine.jar \
  --codegen-package <path-or-uri> \
  --name my-service \
  ...
```

### Environment Variable (Docker)

```bash
CODEGEN_PACKAGE=/path/to/package
```

### Input Formats

The `--codegen-package` option accepts two formats:

| Format | Example | Description |
|--------|---------|-------------|
| Local directory | `/path/to/my-package` | Uses a local directory directly |
| Datastore URI | `datastore-archive://package.tar.bz2` | Downloads and extracts from Wanaku data store |

## Package Structure

The code generation package must have the following structure:

```
my-package/
├── config.properties      # Required: Configuration file
├── kamelets/              # Required: Directory containing Kamelet YAML files
│   ├── http-source.kamelet.yaml
│   ├── kafka-sink.kamelet.yaml
│   └── ...
└── templates/             # Required: Directory containing templates
    └── orchestration.txt  # Required: Orchestration template
```

### config.properties

```properties
# Comma-separated list of available services
available.services=kamelet:http-source,kamelet:kafka-sink,kamelet:log-action

# Optional: Custom description for the search tool
search.tool.description=Searches for available integration services

# Optional: Namespace for tool registration (defaults to no namespace)
namespace=my.custom.namespace
```

| Property | Required | Default | Description |
|----------|----------|---------|-------------|
| `available.services` | Yes | - | Comma-separated list of available services |
| `search.tool.description` | No | "Searches for services to perform the tasks" | Description for the searchServicesTool |
| `namespace` | No | null (no namespace) | Namespace for tool registration in Wanaku |

### Kamelet Files

Each file in the `kamelets/` directory should be a valid Kamelet YAML file with the `.kamelet.yaml` extension:

```yaml
apiVersion: camel.apache.org/v1alpha1
kind: Kamelet
metadata:
  name: http-source
spec:
  definition:
    title: HTTP Source
    description: Fetches data from an HTTP endpoint
  # ... rest of Kamelet spec
```

### Orchestration Template

The `templates/orchestration.txt` file contains the template returned by the `generateOrchestrationCode` tool. This template guides the AI agent in generating orchestration code.

## Tool Registration

Tools are registered with Wanaku using the service name as the URI scheme:

```
<service-name>://searchServicesTool
<service-name>://readKamelet
<service-name>://generateOrchestrationCode
```

For example, with `--name code-execution-engine`:

```
code-execution-engine://searchServicesTool
code-execution-engine://readKamelet
code-execution-engine://generateOrchestrationCode
```

## Tool Details

### searchServicesTool

Returns a formatted list of available services with context explaining how to use Kamelets.

**Parameters:** None

**Response:**
```
# Context
- The list below contains Kamelets that can be used to assemble the orchestration.
- A Kamelet is a snippet for a Camel route.
- It cannot be used on its own. It MUST be a part of an orchestration flow.
- Kamelets can have arguments. Read the Kamelets before you use them.

---
kamelet:http-source
kamelet:kafka-sink
kamelet:log-action
```

### readKamelet

Reads and returns the complete YAML content of a Kamelet.

**Parameters:**

| Name | Type | Required | Description |
|------|------|----------|-------------|
| `name` | string | Yes | The Kamelet name (without `.kamelet.yaml` extension) |

**Response:** The full YAML content of the Kamelet.

### generateOrchestrationCode

Returns the orchestration template content.

**Parameters:** None

**Response:** The content of `templates/orchestration.txt`.

## Example Usage

### Using a Local Directory

For development and testing, you can point to a local directory:

```bash
java -jar camel-code-execution-engine.jar \
  --codegen-package /home/user/my-codegen-package \
  --name my-service \
  --registration-url http://localhost:8080 \
  --client-id my-client \
  --client-secret my-secret \
  --registration-announce-address localhost
```

### Using Datastore Archive

For production, upload a tar.bz2 archive to the Wanaku data store and reference it:

```bash
java -jar camel-code-execution-engine.jar \
  --codegen-package datastore-archive://codegen-package.tar.bz2 \
  --name code-execution-engine \
  --registration-url http://wanaku.example.com \
  --client-id my-client \
  --client-secret my-secret \
  --registration-announce-address auto
```

## Creating a Package Archive

To create a tar.bz2 archive for upload to the data store:

```bash
# From the parent directory of your package
tar -cjf codegen-package.tar.bz2 my-package/
```

Then upload `codegen-package.tar.bz2` to the Wanaku data store.
