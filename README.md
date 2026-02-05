# Camel Code Execution Engine

[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)
[![Java](https://img.shields.io/badge/Java-21+-orange.svg)](https://openjdk.java.net/)

A microservice that executes Apache Camel integration routes and provides code generation tools for AI agents. It integrates with the [Wanaku](https://github.com/wanaku-ai/wanaku) platform to enable AI-driven integration discovery and orchestration.

## What is This?

The Camel Code Execution Engine (CCE) serves two primary functions:

1. **Dynamic Route Execution**: Receives YAML-based Apache Camel routes via gRPC, dynamically resolves Maven dependencies, executes routes in isolated workspaces, and streams results back to clients.

2. **Code Generation Tools**: Exposes MCP-compliant tools that help AI agents discover available integration services (Kamelets) and generate orchestration code.

## Architecture

```
┌─────────────────┐     ┌─────────────────────┐     ┌──────────────────┐
│    AI Agent     │────▶│  Wanaku MCP Router  │────▶│  Code Execution  │
│                 │     │                     │     │     Engine       │
└─────────────────┘     └─────────────────────┘     └────────┬─────────┘
                                                             │
                              ┌──────────────────────────────┼──────────────────────────────┐
                              │                              │                              │
                              ▼                              ▼                              ▼
                    ┌─────────────────┐           ┌─────────────────┐           ┌─────────────────┐
                    │  searchServices │           │   readKamelet   │           │   generateCode  │
                    │      Tool       │           │      Tool       │           │      Tool       │
                    └─────────────────┘           └─────────────────┘           └─────────────────┘
```

## Features

- **Dynamic YAML Route Execution**: Execute Camel routes defined in YAML at runtime
- **Automatic Dependency Resolution**: Downloads Maven dependencies on-the-fly
- **Code Generation Tools**: Three built-in tools for AI-assisted integration development
  - `searchServicesTool` - Lists available Kamelets/integration services
  - `readKamelet` - Returns complete YAML definition of a Kamelet
  - `generateOrchestrationCode` - Returns templates for assembling routes
- **Service Discovery**: Automatic registration with Wanaku discovery service
- **OAuth2/OIDC Authentication**: Secure service registration
- **Git Initialization**: Optional repository cloning at startup
- **Multi-platform Deployment**: Local, Docker, Kubernetes/OpenShift

> [NOTE]
> The MCP tools are not YET functional. The agent MUST provide and the execution workflow.

## Quick Start

### Requirements

- Java 21+
- Maven 3.6+
- Access to a Wanaku router instance
- OAuth2/OIDC provider credentials

### Building from Source

```bash
git clone https://github.com/wanaku-ai/camel-code-execution-engine.git
cd camel-code-execution-engine
mvn clean package
```

### Running the Service

```bash
java -jar target/camel-code-execution-engine.jar \
  --registration-url http://wanaku:8080 \
  --registration-announce-address localhost \
  --client-id my-client-id \
  --client-secret my-secret \
  --codegen-package /path/to/codegen-package
```

### Verify Registration

Once started, the service registers with the Wanaku router and exposes its tools for AI agent consumption.

## Configuration

### Command-Line Options

| Option | Default | Description |
|--------|---------|-------------|
| `--registration-url` | *required* | URL of Wanaku registration service |
| `--registration-announce-address` | *required* | Service address for registration (or "auto") |
| `--client-id` | *required* | OAuth2 client ID |
| `--client-secret` | *required* | OAuth2 client secret |
| `--codegen-package` | *required* | Path to code generation package |
| `--grpc-port` | 9190 | gRPC server port |
| `--name` | code-execution-engine | Service name |
| `--retries` | 12 | Registration retry count |
| `--wait-seconds` | 5 | Retry wait time |
| `--data-dir` | /tmp/cee | Workspace and data directory |
| `--init-from` | - | Git repository URL to clone at startup |
| `--repositories` | - | Maven repositories for dependency resolution |

### Code Generation Package Structure

The `--codegen-package` must point to a directory or archive containing:

```
package/
├── config.properties          # Service list configuration
├── kamelets/                  # Kamelet YAML definitions
│   ├── service1.kamelet.yaml
│   └── service2.kamelet.yaml
└── templates/                 # Orchestration templates
    └── orchestration.txt
```

**config.properties** format:
```properties
available.services=kamelet:service1,kamelet:service2
search.tool.description=Custom description for search tool
namespace=optional.namespace
```

### Resource URI Schemes

| Scheme | Description |
|--------|-------------|
| `datastore://` | Wanaku DataStore access |
| `datastore-archive://` | Wanaku DataStore with tar.bz2 extraction |
| `file://` | Local filesystem (absolute paths) |

## Deployment

### Local

Run directly with Java for development:

```bash
java -jar camel-code-execution-engine.jar [options]
```

### Docker

Build and run using Docker:

```bash
docker build -t camel-code-execution-engine .
docker run -e REGISTRATION_URL=http://wanaku:8080 \
           -e CLIENT_ID=my-client \
           -e CLIENT_SECRET=my-secret \
           -e CODEGEN_PACKAGE=/data/package \
           camel-code-execution-engine
```

### Kubernetes/OpenShift

Deploy using the Wanaku operator for managed deployments.

## Documentation

- [Code Generation Tools Guide](docs/codegen-tools.md) - Detailed tool usage documentation

## Related Projects

- [Wanaku](https://github.com/wanaku-ai/wanaku) - AI MCP Router and orchestration platform
- [Camel Integration Capability](https://github.com/wanaku-ai/camel-integration-capability) - Expose Camel routes as MCP tools
- [Apache Camel](https://camel.apache.org/) - Integration framework
- [Kaoto](https://kaoto.io/) - Visual integration designer

## Contributing

We welcome contributions. Please see our contributing guidelines for more information.

## License

This project is licensed under the Apache License 2.0 - see the [LICENSE](LICENSE) file for details.

## Support

- **Issues**: [GitHub Issues](https://github.com/wanaku-ai/camel-code-execution-engine/issues)
- **Community**: [Wanaku Community](https://github.com/wanaku-ai)
