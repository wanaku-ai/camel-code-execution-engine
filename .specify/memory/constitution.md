<!--
SYNC IMPACT REPORT
==================
Version change: 0.0.0 → 1.0.0
Bump rationale: Initial constitution creation (MAJOR - first governance framework)

Modified principles: N/A (initial creation)

Added sections:
- Core Principles (5 principles)
- Technology Constraints
- Development Workflow
- Governance

Removed sections: N/A (initial creation)

Templates requiring updates:
- .specify/templates/plan-template.md ✅ (Constitution Check section aligns)
- .specify/templates/spec-template.md ✅ (Requirements and Success Criteria align)
- .specify/templates/tasks-template.md ✅ (Test-first workflow and phases align)

Follow-up TODOs: None
-->

# Camel Code Execution Engine Constitution

## Core Principles

### I. Plain Java Only

All code MUST be written in plain Java without application frameworks.

- Spring, Quarkus, Micronaut, and similar frameworks are FORBIDDEN
- Apache Camel is the ONLY framework-like dependency permitted (core to project purpose)
- Lombok is STRICTLY FORBIDDEN - all getters, setters, constructors, equals/hashCode MUST be written explicitly
- Picocli is permitted for CLI argument parsing
- Standard Java libraries and well-established utility libraries (e.g., SLF4J, Log4j) are permitted
- Rationale: Maintains simplicity, reduces magic, ensures full control over code behavior, and keeps the codebase understandable without framework knowledge

### II. Code Quality Standards

All code MUST adhere to strict quality standards ensuring maintainability and readability.

- Code formatting MUST use Palantir Java Format (enforced via Spotless Maven plugin)
- All public classes and methods MUST have meaningful Javadoc explaining purpose and usage
- Method complexity MUST be kept low - extract complex logic into well-named private methods
- Null handling MUST be explicit - use Optional where appropriate, never return null from public methods without documentation
- Exception handling MUST be intentional - no swallowing exceptions, proper logging required
- Unused imports and dead code MUST be removed before commit
- Class responsibilities MUST be single and clear - if a class description requires "and", split it
- Rationale: Clean code reduces bugs, speeds up onboarding, and makes maintenance sustainable

### III. Test-First Development

Tests MUST be written before implementation code and MUST fail before implementation begins.

- Red-Green-Refactor cycle is MANDATORY: write failing test → implement → refactor
- Unit tests MUST cover all public methods with meaningful assertions
- Integration tests MUST verify component interactions and external dependencies
- Contract tests MUST validate API endpoints and external service boundaries
- Test naming MUST follow pattern: `methodName_condition_expectedResult`
- Tests MUST be independent - no shared mutable state between test methods
- Mocking MUST be minimal - prefer real objects; mock only external dependencies
- Test coverage MUST be meaningful - measure behavior coverage, not line coverage
- Rationale: Test-first catches design issues early, ensures testable code, and provides living documentation

### IV. User Experience Consistency

All user-facing interfaces MUST provide consistent, predictable behavior.

- CLI output MUST follow consistent formatting: stdout for results, stderr for errors and logs
- Error messages MUST be actionable - include what failed, why, and how to fix
- Exit codes MUST follow conventions: 0 for success, non-zero for failures with documented meanings
- Progress feedback MUST be provided for long-running operations
- Configuration MUST support both CLI arguments and configuration files with clear precedence
- Breaking changes to user-facing behavior MUST be documented and versioned
- Help text MUST be comprehensive and include examples
- Rationale: Consistent UX reduces user frustration and support burden

### V. Performance Requirements

All code MUST meet performance standards appropriate for a code execution engine.

- Startup time MUST be minimized - lazy initialization where possible
- Memory usage MUST be bounded - avoid unbounded caches or collections
- Resource cleanup MUST be guaranteed - use try-with-resources, close handlers properly
- Camel routes MUST be optimized - avoid unnecessary data copies, use streaming where appropriate
- Blocking operations MUST be documented and minimized
- Performance-critical paths MUST have benchmarks in test suite
- Logging MUST be level-appropriate - DEBUG for detailed tracing, INFO for operations, WARN/ERROR for issues
- Rationale: Performance directly impacts user experience and operational costs

## Technology Constraints

This section defines the technology boundaries for the project.

**Permitted:**
- Java 21 (LTS) with preview features disabled
- Apache Camel (core, yaml-dsl, main, kamelet-main, direct, file components)
- Wanaku SDK (capabilities-common, discovery, data-files, exchange, runtime, services-client)
- Picocli for CLI
- SLF4J + Log4j2 for logging
- SnakeYAML for YAML parsing
- JGit for Git operations
- JUnit 5 for testing
- Maven for build management

**Forbidden:**
- Lombok (code generation obscures actual code)
- Spring Framework / Spring Boot (heavyweight, magic-heavy)
- Quarkus (build-time augmentation complexity)
- Micronaut (similar concerns to Spring/Quarkus)
- Annotation processors that generate code at compile time (except standard Java ones)
- Reflection-heavy libraries without explicit justification

**Justification Required:**
- Any new dependency MUST be justified in PR description
- Dependencies MUST be version-managed in POM properties
- Transitive dependency conflicts MUST be resolved explicitly

## Development Workflow

This section defines the mandatory workflow for all development activities.

**Before Starting Work:**
1. Verify understanding of requirements
2. Check constitution compliance of proposed approach
3. Write failing tests first

**During Development:**
1. Follow Red-Green-Refactor strictly
2. Commit frequently with meaningful messages
3. Run `mvn spotless:apply` before commits
4. Ensure all tests pass before pushing

**Before Merge:**
1. All tests MUST pass (unit, integration, contract)
2. Code formatting MUST pass Spotless check
3. No new compiler warnings introduced
4. Javadoc MUST be complete for public API changes
5. Performance impact MUST be assessed for critical paths

**Code Review Requirements:**
- Constitution compliance MUST be verified
- Test quality MUST be assessed (not just coverage)
- Error handling MUST be reviewed
- Resource management MUST be verified

## Governance

This constitution supersedes all other development practices and guidelines.

**Amendment Process:**
1. Propose amendment with rationale
2. Document impact on existing code
3. Create migration plan if breaking changes
4. Update version according to semantic versioning

**Versioning Policy:**
- MAJOR: Backward-incompatible changes, principle removals, fundamental redefinitions
- MINOR: New principles added, existing guidance materially expanded
- PATCH: Clarifications, wording improvements, non-semantic changes

**Compliance Review:**
- All PRs MUST verify constitution compliance
- Violations MUST be justified in Complexity Tracking section of plan
- Unjustified violations MUST block merge

**Guidance Files:**
- Runtime development guidance in project documentation
- Feature-specific guidance in spec files

**Version**: 1.0.0 | **Ratified**: 2026-02-02 | **Last Amended**: 2026-02-02
