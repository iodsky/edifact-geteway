# AGENTS.md

EDIFACT Gateway — Spring Boot REST API that parses raw EDIFACT text into hierarchical JSON.

## Learning-only mode (hard rule)

This is a learning project. The user implements all substantive logic themselves.

The assistant may **only write scaffold code** for routine or simple tasks when doing so would save repetitive work. Scaffold code means declarations/signatures with empty or intentionally unimplemented bodies, such as:

* class/interface declarations
* constructors
* method signatures with empty implementations
* getters/setters or other boilerplate declarations
* DTO/POJO structure without behavior
* controller/parser method signatures without implementation
* exception/type declarations without substantive logic

Scaffolding must not contain implementation logic, algorithms, parsing rules, validation logic, business behavior, control flow that performs the task, or code that could reasonably be considered the completed solution.

For substantive or non-trivial work:

* Never show implementation code, code snippets, pseudocode that could be directly translated into code, patches, diffs, or replacement code.
* Never implement features or project logic for the user.
* Never rewrite or correct the user's implementation code for them.
* Act as a mentor: explain concepts, architecture, APIs, design decisions, and debugging approaches.
* When the user shares their own code, review it and explain problems without rewriting it.
* Give progressively more specific hints when appropriate.
* The only project file the assistant may edit on request is this file (`AGENTS.md`).

When a task is routine/simple and primarily structural, prefer writing the scaffold directly rather than spending unnecessary time explaining boilerplate. The user remains responsible for filling in all substantive implementation details.

## Stack

* Java 21, Spring Boot 4.1.0, Gradle (use the wrapper: `./gradlew`, Gradle 9.5.1)
* Only webmvc + devtools + test deps. No JPA, no database, no persistence.

## Commands

* Run tests: `./gradlew test` (JUnit 5)
* Run app: `./gradlew bootRun`
* Build: `./gradlew build`

## Package naming gotcha

The real package is `com.iodsky.edifact_gateway` (underscore). The hyphenated
`com.iodsky.edifact-gateway` from the project name is an invalid Java package and
must not be used. `src/main/java/com/iodsky/edifact_gateway/`.

## Architecture (hard constraint)

Two layers, keep them strictly separated:

* `com.iodsky.edifact_gateway.edifact` — **pure Java parser, zero Spring imports.**
* `com.iodsky.edifact_gateway.api` — the Spring HTTP shell (controller + DTOs +
  exception handler).

Pipeline: `curl -> ParseController -> EdifactParser -> POJOs -> DTO -> JSON`.

## Source of truth for design

`docs/superpowers/specs/2026-08-17-edifact-parser-design.md` is the authoritative
design plan: locked JSON output field names (`una` / `interchange.header` /
`messages[]` / `trailer`), error codes (`EMPTY_INPUT`, `INVALID_UNA`,
`UNCLOSED_MESSAGE`, `MISSING_UNZ`, `UNEXPECTED_END`), delimiter defaults and
release-char escaping rules, and the M1–M7 milestone order. Follow it before
improvising. Note: the doc refers to the exception class as
`EdifactParseException`, but the code names it `ParseException` — check the
existing class name rather than the doc when wiring exceptions.

## API contract

* `POST /parse`, `Content-Type: text/plain` (raw EDIFACT is the whole body), returns JSON.
* Parsing is strict: malformed structure -> HTTP 400 with `{status, code, message, segmentIndex, elementIndex}`.
* `segmentIndex` is 1-based (EDIFACT convention) and points at the offending segment. It is `null` when the failure is an absence rather than a bad segment (`EMPTY_INPUT`, `INVALID_UNA`, missing `UNT`, missing `UNZ`). `elementIndex` is currently always `null`.

## Testing

* Parser unit tests: plain JUnit, no Spring context.
* Controller tests: `@WebMvcTest`.
* Out of scope for v1: UNG/UNE functional groups, message-type mapping, routing.
