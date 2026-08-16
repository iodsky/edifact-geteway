# EDIFACT Gateway — Design & Implementation Plan

Date: 2026-08-17

## Goal

A Spring Boot REST API that accepts raw EDIFACT text and returns parsed JSON.
Client posts raw text, server returns JSON. No persistence, no domain mapping,
no routing.

## Tech Stack

- Spring Boot 4.1.0, Java 21, Gradle
- Dependencies (already trimmed): `spring-boot-starter-webmvc`, `devtools`,
  `spring-boot-starter-webmvc-test`, `junit-platform-launcher`

## Architecture

Two cleanly separated layers:

1. **`edifact` package — pure Java, zero Spring imports** (the reusable parser)
2. **`api` package — the Spring HTTP shell**

Pipeline:

```
curl -> ParseController (Spring) -> EdifactParser (pure Java) -> POJOs -> DTO -> JSON
```

### Parser library — `com.iodsky.edifact_gateway.edifact`

| File | Responsibility |
|---|---|
| `Delimiters.java` | component/element separators, decimal mark, release char, terminator; defaults + `fromUna()` |
| `EdifactParseException.java` | `code`, `message`, `segmentIndex`, `elementIndex` |
| `Segment.java` | tag + elements (simple `String` / composite `List<String>`) |
| `Message.java` | header (UNH), segments, trailer (UNT) |
| `Interchange.java` | header (UNB), messages, trailer (UNZ) |
| `EdifactDocument.java` | `una` (nullable), `interchange` |
| `EdifactParser.java` | tokenize -> segments -> elements (UNA + defaults + release-char escaping) |
| `EnvelopeBuilder.java` | flat segments -> interchange/messages hierarchy |

### API layer — `com.iodsky.edifact_gateway.api`

| File | Responsibility |
|---|---|
| `ParseController.java` | `POST /parse`, `text/plain` body, returns hierarchical JSON |
| `dto/*.java` | hierarchical output DTO (Jackson-serialized) |
| `ApiExceptionHandler.java` | `EdifactParseException` -> `400` structured body |

## API Contract

- **Endpoint**: `POST /parse`
- **Content-Type**: `text/plain` (raw EDIFACT is the entire request body)
- **Response**: hierarchical JSON (single view, no query params)

### Delimiters

UNA overrides, else UN/EDIFACT defaults:

| Name | Default | UNA position |
|---|---|---|
| component separator | `:` | 1 |
| data element separator | `+` | 2 |
| decimal mark | `.` | 3 |
| release character | `?` | 4 |
| (reserved / space) | ` ` | 5 |
| segment terminator | `'` | 6 |

### Release-character escaping

- `??` -> `?`
- `?+` -> `+`
- `?:` -> `:`
- `?'` -> `'`

The release char applies to the next character only (stateful, not cumulative).

## Output Shape

Exact field names (locked). Always hierarchical:

```json
{
  "una": "UNA:+.? '",
  "interchange": {
    "header": { "tag": "UNB", "elements": [] },
    "messages": [
      {
        "header": { "tag": "UNH", "elements": [] },
        "segments": [],
        "trailer": { "tag": "UNT", "elements": [] }
      }
    ],
    "trailer": { "tag": "UNZ", "elements": [] }
  }
}
```

Composites are nested arrays; simple elements are strings.

## Error Handling

Strict. Any malformed structure aborts with HTTP 400 and:

```json
{
  "status": 400,
  "code": "UNCLOSED_MESSAGE",
  "message": "...",
  "segmentIndex": 3,
  "elementIndex": null
}
```

Error codes: `EMPTY_INPUT`, `INVALID_UNA`, `UNCLOSED_MESSAGE`, `MISSING_UNZ`,
`UNEXPECTED_END` (extend as needed).

## Scope (explicitly out for v1)

- UNG/UNE functional groups
- Persistence / JPA / database
- Validation library / message-type mapping
- Routing / forwarding

## Milestones (implement in order)

- **M1 — `Delimiters` + UNA detection**: defaults + `fromUna(String)`.
- **M2 — Tokenizer (text -> segments)**: split on terminator `'`, honor
  release-char escaping, handle `\r\n`.
- **M3 — Segment -> tag + elements + composites**: first element before `+`
  is tag; split rest on `+`; composites on `:` (release-char aware).
- **M4 — Envelope (interchange + messages)**: UNB..UNZ, UNH..UNT pairing.
- **M5 — Errors**: `EdifactParseException` + error codes.
- **M6 — Spring shell**: `ParseController`, hierarchical DTO, `ApiExceptionHandler`.
- **M7 — End-to-end**: `./gradlew bootRun` + `curl --data-binary`.

## Testing

- Parser unit tests (no Spring): UNA + defaults, release-char escaping,
  composite/simple splitting, envelope (nested messages), error cases
  (unclosed UNH, missing UNZ, invalid UNA).
- Controller tests (`@WebMvcTest`): endpoint wiring,
  `text/plain` ingestion, 400 mapping.
- Command: `./gradlew test`

## Sample EDIFACT (for testing)

```
UNA:+.? '
UNB+UNOA:3+SENDER+RECEIVER+240816:1200+1'
UNH+1+ORDERS:D:96A:UN'
BGM+220+ORD001'
UNT+3+1'
UNZ+1+1'
```
