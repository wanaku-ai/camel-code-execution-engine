# Quickstart: Code Generation Support Tools

**Feature Branch**: `001-codegen-support-tools`
**Date**: 2026-02-02

## Prerequisites

1. CCE built and available
2. Wanaku instance running with data store configured
3. Code generation package uploaded to data store

## Package Preparation

### 1. Create Package Structure

```bash
mkdir -p code-gen-package/{kamelets,templates}

# Create properties file
cat > code-gen-package/config.properties << 'EOF'
available.services=kamelet:http-source,kamelet:kafka-sink,kamelet:log-action
search.tool.description=Searches for available services to build integrations
EOF

# Create sample Kamelet
cat > code-gen-package/kamelets/http-source.kamelet.yaml << 'EOF'
apiVersion: camel.apache.org/v1alpha1
kind: Kamelet
metadata:
  name: http-source
spec:
  definition:
    title: HTTP Source
    description: Fetches data from HTTP endpoint
    properties:
      url:
        type: string
        title: URL
        description: The URL to fetch
EOF

# Create orchestration template
cat > code-gen-package/templates/orchestration.txt << 'EOF'
# Orchestration Template
# Use the following structure to assemble your integration:

- route:
    from:
      uri: "direct:start"
      steps:
        # Add your Kamelets here
        - to: "{{kamelet:source}}"
        - to: "{{kamelet:sink}}"
EOF
```

### 2. Create Archive

```bash
cd code-gen-package
tar -cvjf ../code-gen-package.tar.bz ..
cd ..
```

### 3. Upload to Data Store

Upload `code-gen-package.tar.bz` to Wanaku data store with name `code-gen-package.tar.bz`.

## Running CCE with Code Generation Tools

### Command Line

```bash
java -jar camel-code-execution-engine-app.jar \
  --registration-url http://localhost:8080 \
  --client-id your-client-id \
  --client-secret your-client-secret \
  --data-dir /tmp/cce \
  --codegen-package datastore://code-gen-package.tar.bz
```

### Docker

```bash
docker run -it \
  -e REGISTRATION_URL=http://wanaku:8080 \
  -e CLIENT_ID=your-client-id \
  -e CLIENT_SECRET=your-client-secret \
  -e CODEGEN_PACKAGE=datastore://code-gen-package.tar.bz \
  -v /tmp/cce:/data \
  camel-code-execution-engine:latest
```

## Verifying Tool Registration

After CCE starts, verify tools are registered:

```bash
# List registered tools (via Wanaku API)
curl http://localhost:8080/api/v1/tools | jq '.[] | select(.type == "codegen")'
```

Expected output:
```json
[
  {
    "name": "searchServicesTool",
    "description": "Searches for available services to build integrations",
    "uri": "codegen://searchServicesTool",
    "type": "codegen"
  },
  {
    "name": "readKamelet",
    "description": "Reads the content of a Kamelet by name",
    "uri": "codegen://readKamelet",
    "type": "codegen"
  },
  {
    "name": "generateOrchestrationCode",
    "description": "Returns the orchestration template for code generation",
    "uri": "codegen://generateOrchestrationCode",
    "type": "codegen"
  }
]
```

## Using the Tools

### Search for Services

```bash
grpcurl -plaintext -d '{
  "uri": "codegen://searchServicesTool"
}' localhost:9190 wanaku.capabilities.ToolInvoker/InvokeTool
```

### Read a Kamelet

```bash
grpcurl -plaintext -d '{
  "uri": "codegen://readKamelet",
  "arguments": {"name": "http-source"}
}' localhost:9190 wanaku.capabilities.ToolInvoker/InvokeTool
```

### Get Orchestration Template

```bash
grpcurl -plaintext -d '{
  "uri": "codegen://generateOrchestrationCode"
}' localhost:9190 wanaku.capabilities.ToolInvoker/InvokeTool
```

## Troubleshooting

### Tools Not Registered

Check CCE logs for:
- "Failed to download code generation package" - verify datastore URI and package exists
- "Failed to extract archive" - verify archive is valid tar.bz2

### Kamelet Not Found

Verify:
- Kamelet file exists in `kamelets/` directory with `.kamelet.yaml` extension
- Name matches without the extension (e.g., `http-source` for `http-source.kamelet.yaml`)

### Empty Service List

Check `config.properties` contains `available.services` key with comma-separated values.
