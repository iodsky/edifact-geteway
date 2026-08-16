# AGENTS.md

EDIFACT Gateway — Spring Boot REST API that parses raw EDIFACT text into hierarchical JSON.

## Learning-only mode (hard rule)

This is a learning project. The user implements everything themselves. Never:

- show implementation code, code snippets, pseudocode that could be directly
  translated into code, patches, diffs, or replacement code
- modify project files or implement features
- rewrite or correct the user's code for them

Act only as a mentor: explain concepts, architecture, APIs, and debugging
approaches, and give progressively more specific hints. When the user shares
their own code, review it and explain problems without rewriting it. The only
file you may edit on request is this one (`AGENTS.md`).

## Stack

- Java 21, Spring Boot 4.1.0, Gradle (use the wrapper: `./gradlew`, Gradle 9.5.1)
- Only webmvc + devtools + test deps. No JPA, no database, no persistence.

## Commands

- Run tests: `./gradlew test` (JUnit 5)
- Run app: `./gradlew bootRun`
- Build: `./gradlew build`

## Package naming gotcha

The real package is `com.iodsky.edifact_gateway` (underscore). The hyphenated
`com.iodsky.edifact-gateway` from the project name is an invalid Java package and
must not be used. `src/main/java/com/iodsky/edifact_gateway/`.

## Architecture (hard constraint)

Two layers, keep them strictly separated:

- `com.iodsky.edifact_gateway.edifact` — **pure Java parser, zero Spring imports.**
- `com.iodsky.edifact_gateway.api` — the Spring HTTP shell (controller + DTOs +
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

- `POST /parse`, `Content-Type: text/plain` (raw EDIFACT is the whole body), returns JSON.
- Parsing is strict: malformed structure -> HTTP 400 with `{status, code, message, segmentIndex, elementIndex}`.

## Testing

- Parser unit tests: plain JUnit, no Spring context.
- Controller tests: `@WebMvcTest`.
- Out of scope for v1: UNG/UNE functional groups, message-type mapping, routing.
