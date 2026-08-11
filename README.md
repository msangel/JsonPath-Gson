# JsonPath-Gson

An experimental JSONPath evaluator for Gson. Queries are parsed with ANTLR and
evaluated as a pipeline of `PathNode` iterators over `JsonElement` values.

The normative JSONPath specification is [RFC 9535](https://www.rfc-editor.org/rfc/rfc9535.html).
This project implements all RFC selectors and all currently registered standard
function extensions. It **does not yet claim full RFC 9535 conformance** because
two I-Regexp result cases still differ from the upstream compliance suite, and
that suite has not yet been integrated into the Maven build.

## Build and runtime

- Maven wrapper: `./mvnw clean install`
- build JDK: 11 or newer (required by the ANTLR tool)
- produced bytecode and runtime dependencies: Java 8 compatible
- JSON model: Gson `JsonElement`
- test suite: JUnit parameterized tests

## Evaluation model and API

`AntlrParser` turns a query into an `Expression` containing executable path nodes.
`Expression.exec(String)` and `Expression.exec(JsonElement)` return a materialized
`List<JsonElement>`.

Filter selectors are not interpreted as strings while walking the JSON document.
The same ANTLR lexer and parser handle both JSONPath segments and filter expressions,
and the parser visitor compiles each filter subtree into a reusable `FilterExpression`
object. `FilterPathNode` evaluates that object for each array element or object member
value, while preserving both the current value (`@`) and document root (`$`).
The five RFC function names are fixed lexer tokens, and their signatures are encoded
in the grammar, so non-well-typed calls are rejected while parsing rather than during
document evaluation.

Evaluation is iterator-based internally, but the current public execution methods
collect every result into a list. There is no public lazy-result API yet. Also,
`JsonPath.parseExpression` and `AntlrParser` are package-private, so the project does
not yet expose a stable public entry point for applications outside this package.

## RFC 9535 feature status

| RFC feature | Status | Current behavior |
| --- | --- | --- |
| Query well-formedness | Implemented | RFC blank space is accepted only at the grammar's `S` positions. Invalid forms such as `$. name`, `$.. [0]`, `length (@)`, and the legacy `$.[0]` raise `JsonPathException`. |
| Root identifier | Implemented | Queries start with `$`; the root-only query `$` returns the complete input document as one result. |
| Child name selector | Implemented | Supports RFC dot shorthand, including Unicode names such as `$.ключ` and `$.日本語`, and quoted bracket names such as `$['store']` and `$["store"]`. |
| Quoted name escapes | Implemented | Supports every RFC escape (`\b`, `\f`, `\n`, `\r`, `\t`, `\/`, `\\`, escaped delimiters, and `\uXXXX`), validates control characters and surrogate pairs, and decodes supplementary Unicode characters. |
| Wildcard selector | Implemented | Supports `[*]` and `.*` for arrays and objects. Gson insertion order is currently observed for objects, although RFC 9535 does not define object result order. |
| Array index selector | Implemented | Supports the full I-JSON exact-integer range, negative indexes, and empty results for out-of-range indexes; rejects leading zeros and `-0`. |
| Array slice selector | Implemented | Implements RFC bounds normalization, omitted bounds, positive/negative steps, reverse traversal, empty results for step `0`, and selection from arrays only. |
| Multiple selectors in one child segment | Implemented | Names, indexes, slices, wildcards, and filters can be freely mixed; selector order and duplicate results are preserved. |
| Descendant segment | Implemented | Supports `..name`, `..*`, and bracket forms such as `$..[0]`, `$..['name']`, `$..[0:2,5]`, and `$..[*]`, applying selectors to the input node and each descendant in RFC traversal order. |
| Filter selector | Implemented | The main ANTLR grammar compiles filters as part of the complete JSONPath parse tree. Supports current (`@`) and root (`$`) queries, nested filters, existence tests, parentheses, `!`, `&&`, `\|\|`, `==`, `!=`, `<`, `<=`, `>`, `>=`, and string, number, boolean, and `null` literals. |
| Function extensions | Implemented | Implements all five functions in the current [IANA JSONPath Function Extensions registry](https://www.iana.org/assignments/jsonpath/jsonpath.xhtml): `length()` for strings, arrays, and objects; `count()` for nodelists; `match()` and `search()` with checked [RFC 9485 I-Regexp](https://www.rfc-editor.org/rfc/rfc9485.html) syntax; and `value()` for converting a single-node nodelist to a value. Function signatures and ValueType/LogicalType/NodesType placement are enforced by the grammar. |

The RFC allows a query to contain zero or more segments, defines general
comma-separated selector sequences, and specifies filters and standard function
extensions. See the RFC sections for the
[root identifier](https://www.rfc-editor.org/rfc/rfc9535.html#section-2.2),
[selectors](https://www.rfc-editor.org/rfc/rfc9535.html#section-2.3),
[segments](https://www.rfc-editor.org/rfc/rfc9535.html#section-2.5), and
[functions](https://www.rfc-editor.org/rfc/rfc9535.html#section-2.4).

This status was checked against RFC 9535, the current
[RFC 9535 errata](https://errata.rfc-editor.org/search/?rfc_number=9535), the
[IANA function registry](https://www.iana.org/assignments/jsonpath/jsonpath.xhtml),
and the upstream
[JSONPath Compliance Test Suite](https://github.com/jsonpath-standard/jsonpath-compliance-test-suite).
The held errata concern alternative ordering in a generic PEG grammar; they do
not change the behavior of the five explicitly typed function productions used
by this project.

A one-off values-only audit against compliance-suite revision
[`7be7c1f`](https://github.com/jsonpath-standard/jsonpath-compliance-test-suite/tree/7be7c1fc28057c91e8eefaf197060fba7ed43acd)
passed 701 of 703 cases, with all syntax acceptance and rejection checks passing.
The two remaining result differences expect unescaped `^` and `$` to act as
regular-expression anchors, while this project treats them as ordinary I-Regexp
characters according to the normative XML Schema semantics referenced by RFC
9485. That interpretation difference needs to be resolved and documented before
the project claims full conformance. This audit is informational for now; the
suite is not yet part of the Maven build.

## Examples

### Root, names, escapes, and wildcards

```text
$
$.store.book
$.корінь.ключ
$.日本語
$['store']["book"]
$.store.book[*].title
$['a.b']
$['quote\'and\\slash']
$["line\nbreak"]
$['\uD83D\uDE00']
$.length
$.store.*
$[*]
```

Names containing punctuation, whitespace, quotes, backslashes, or other
characters unavailable in dot shorthand use a quoted name selector. Function
keywords remain valid property names, as shown by `$.length`.

### Indexes, slices, and selector lists

```text
$[0]
$[-1]
$[0,2,4]
$[0,0]
$['book','bicycle']
$[0:2,5]
$[2,0:2,*]
$[1:5]
$[1:9:2]
$[:3]
$[5:]
$[5:1:-2]
$[::-1]
$[::0]
```

Selector order and duplicate results are preserved. A zero slice step is valid
and selects no elements.

### Descendant segments

```text
$..title
$..*
$..['title','name']
$..[0]
$..[0:2,5]
$..[?@.status == 'ready']
```

### Filters and comparisons

```text
$.values[?@ > 3.5]
$.store.book[?@.price < 10]
$.store.book[?@.isbn]
$.store.book[?!@.isbn]
$.items[?@.value == null]
$.items[?(@.price < 10 || @.featured == true)]
$.store.book[?@.price <= $.maxPrice && @.available == true]
$.groups[?@.items[?@.active == true]]
$..[?@.status == 'ready']
$.items[?@.missing == $.alsoMissing]
```

The last example demonstrates the RFC `Nothing` rule: two absent singular-query
results compare equal. Existence tests depend on whether a node is selected, not
on whether its JSON value is truthy; a selected `null` or `false` value therefore
still exists.

### Standard function extensions

```text
$.store.book[?length(@.author) > 10]
$.collections[?length(@) >= 3]
$.groups[?count(@.items[*]) >= 2]
$.objects[?count(@.*) == 1]
$.events[?match(@.date, '2026-..-..')]
$.zones[?match(@.timezone, 'Europe/.*')]
$.items[?match(@.code, '[A-Z]{2}[0-9]{3}')]
$.store.book[?search(@.author, '[BR]ob')]
$.items[?search(@.name, '\\p{L}+')]
$[?value(@..color) == 'red']
$.groups[?length(value(@..label)) > 3]
```

These examples are accepted by the current grammar. Inside a quoted JSONPath
string, an I-Regexp backslash must itself be escaped, hence `'\\p{L}+'` for the
Unicode letter category.

Missing object members and out-of-range array indexes produce no result rather than
an exception. Wildcards and filters operate on both array elements and object member
values. Selector lists preserve selector order and repeated matches. In filter
comparisons, two absent singular-query results compare equal, matching the RFC
`Nothing` rules; an existence test is true when its query selects at least one node,
even when the selected JSON value is `null`.

## RFC conformance roadmap

1. Resolve and document the RFC 9485/CTS interpretation difference for
   unescaped `^` and `$` in `match()` and `search()` patterns, then pin the
   intended behavior with tests.
2. Integrate a pinned revision of the upstream JSONPath Compliance Test Suite,
   covering valid results, invalid-query rejection, Unicode, functions,
   ordering, duplicate results, and permitted non-deterministic object order.
3. Resolve every value-result and validation failure from that suite and publish
   the tested suite revision and pass rate before claiming RFC conformance.

RFC 9535 explicitly permits an API to return values, Normalized Paths, both, or
another representation. Therefore, Normalized Path output remains an API feature
below rather than a blocker for value-result conformance. When path output is
implemented, the suite's `result_paths` cases should be enabled as well.

## API roadmap

- public parse/read/compile entry points
- reusable compiled expressions and a public lazy result iterator
- a documented contract for definite/indefinite and missing paths
- optional normalized-path result output
- mapping from `JsonElement` results to Java types and generic type references
- documented thread-safety and caching behavior

## Non-standard compatibility ideas

These are useful compatibility features, but they are not requirements of RFC 9535
and should remain separate from the conformance roadmap:

- Jayway-style `JsonPath.read`, read contexts, predicates, and return options such
  as `ALWAYS_RETURN_LIST`, `DEFAULT_PATH_LEAF_TO_NULL`, and `AS_PATH_LIST`
- Jayway operators such as `=~`, `in`, `nin`, `subsetof`, `anyof`, `noneof`,
  `size`, and `empty`
- aggregation and utility functions such as `min`, `max`, `avg`, `sum`, `keys`,
  `concat`, `first`, and `last`
- legacy script expressions such as `$[(@.length-1)]`

## Other implementations

- [Jayway JsonPath](https://github.com/json-path/JsonPath)
