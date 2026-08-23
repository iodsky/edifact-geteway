# EDIFACT Gateway

A UN/EDIFACT parser that reads a raw EDI interchange and returns its contents as
structured JSON. Built to understand the EDIFACT syntax rules from the byte
level up: delimiters, release-character escaping, and the interchange/message
envelope hierarchy.

```
raw EDIFACT text  ->  parser  ->  POJOs  ->  JSON
```

## What is EDIFACT?

[EDIFACT](https://unece.org/trade/uncefact/introducing-unedifact) (Electronic
Data Interchange For Administration, Commerce and Transport) is a UN standard
for machine-readable business documents. A transmission is a stream of
*segments* terminated by `'`. Each segment carries a three-letter *tag* (e.g.
`UNB`, `BGM`) followed by *data elements* separated by `+`. A data element may
itself be a *composite*, splitting into *components* with `:`. The optional
`UNA` *service string advice* can override these delimiters per transmission.

## What this project demonstrates

| Concept | How it's handled |
|---|---|
| **UNA service string advice** | `UNA:+.? '` defines delimiters by position; absent UNA falls back to the UN/EDIFACT defaults (`:`, `+`, `.`, `?`, `'`) |
| **Release-character escaping** | `?` escapes the next character so a delimiter can appear as data: `??`→`?`, `?+`→`+`, `?:`→`:`, `?'`→`'` |
| **Segment parsing** | tag + simple elements + composite elements, split on `+` then `:`, escape-aware |
| **Interchange envelope** | `UNB … UNZ` wrapping one or more `UNH … UNT` messages |
| **Strict validation** | malformed structure aborts with a structured error: HTTP 400 + a machine-readable `code` + 1-based `segmentIndex` |

### Error codes

`EMPTY_INPUT`, `INVALID_UNA`, `MISSING_UNB`, `UNCLOSED_MESSAGE`,
`UNEXPECTED_END`, `MISSING_UNZ`

`segmentIndex` points at the offending segment (1-based, EDIFACT convention),
and is `null` when the failure is an *absence* rather than a bad segment
(empty input, invalid UNA, missing `UNT`/`UNZ`).

## Example

Input:

```
UNA:+.? '
UNB+UNOA:3+SENDER+RECEIVER+240816:1200+1'
UNH+1+ORDERS:D:96A:UN'
BGM+220+ORD001'
UNT+3+1'
UNZ+1+1'
```

Output:

```json
{
  "una": "UNA:+.? '",
  "interchange": {
    "header": { "tag": "UNB", "elements": [["UNOA", "3"], "SENDER", "RECEIVER", ["240816", "1200"], "1"] },
    "messages": [
      {
        "header": { "tag": "UNH", "elements": ["1", ["ORDERS", "D", "96A", "UN"]] },
        "segments": [
          { "tag": "BGM", "elements": ["220", "ORD001"] }
        ],
        "trailer": { "tag": "UNT", "elements": ["3", "1"] }
      }
    ],
    "trailer": { "tag": "UNZ", "elements": ["1", "1"] }
  }
}
```

Simple elements serialize as strings; composites as nested arrays.

## Architecture

Two strictly separated layers:

- **`com.iodsky.edifact_gateway.edifact`** — pure Java parser, zero Spring
  imports. Reusable outside any HTTP context.
  - `Delimiters` — separators + defaults + `fromUna()`
  - `Parser` — tokenize → segments → elements (UNA detection, release-char
    escaping)
  - `EnvelopeBuilder` — flat segments → `interchange.header` / `messages[]` /
    `trailer`
  - `ParseException` — `code`, `message`, `segmentIndex`, `elementIndex`
- **`com.iodsky.edifact_gateway.api`** — the Spring HTTP shell (controller +
  DTOs + exception handler).

```
curl -> Controller (Spring) -> Parser (pure Java) -> POJOs -> DTO -> JSON
```

## Running it

```sh
./gradlew bootRun
```

```sh
curl --data-binary @sample.edi \
     -H 'Content-Type: text/plain' \
     http://localhost:8080/parse
```

## API contract

- `POST /parse`, `Content-Type: text/plain` (the raw EDIFACT is the whole body)
- Returns hierarchical JSON (single view, no query parameters)
- Malformed structure → HTTP 400:

```json
{
  "status": 400,
  "code": "UNCLOSED_MESSAGE",
  "message": "...",
  "segmentIndex": 3,
  "elementIndex": null
}
```

## Testing

```sh
./gradlew test
```

- Parser unit tests (plain JUnit, no Spring): UNA + defaults, release-char
  escaping, composite/simple splitting, envelope nesting, error cases.
- Controller tests (`@WebMvcTest`): endpoint wiring, `text/plain` ingestion,
  400 mapping.

## Scope (deliberately out for v1)

- UNG/UNE functional groups
- Persistence / database
- Message-type mapping / validation libraries
- Routing / forwarding

## Key insights

- **The release character is the subtlest part of tokenizing.** Splitting on
  `'` is trivial until you account for `?` — a value like `D?'Angelo` must not
  terminate its segment early, and `??` must collapse back to a single `?`.
  Any parser that ignores this silently corrupts punctuation-bearing real-world
  data (names, free-text address lines).
- **Strictness is a data-integrity feature, not a nicety.** Real EDI arrives
  truncated or malformed. Failing loudly with a specific `code` and a
  `segmentIndex` — instead of guessing — is what lets a downstream system
  quarantine a bad interchange rather than process it as if it were complete.
- **UNA is advice, not a segment.** It has no tag, no separators, and no
  terminator of its own; it is exactly nine characters, and when it's absent
  the parser must fall back to the standard defaults.
