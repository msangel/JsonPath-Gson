# JsonPath-Gson

An experimental JSONPath evaluator for Gson. Queries are parsed with ANTLR and
evaluated as a pipeline of `PathNode` iterators over `JsonElement` values.

The normative JSONPath specification is [RFC 9535](https://www.rfc-editor.org/rfc/rfc9535.html).
This project currently implements a useful subset, but **is not yet RFC 9535 conformant**.

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

Evaluation is iterator-based internally, but the current public execution methods
collect every result into a list. There is no public lazy-result API yet. Also,
`JsonPath.parseExpression` and `AntlrParser` are package-private, so the project does
not yet expose a stable public entry point for applications outside this package.

## RFC 9535 feature status

| RFC feature | Status | Current behavior |
| --- | --- | --- |
| Root identifier | Implemented | Queries start with `$`; the root-only query `$` returns the complete input document as one result. |
| Child name selector | Implemented | Supports RFC dot shorthand, including Unicode names such as `$.ключ` and `$.日本語`, and quoted bracket names such as `$['store']` and `$["store"]`. |
| Quoted name escapes | Implemented | Supports every RFC escape (`\b`, `\f`, `\n`, `\r`, `\t`, `\/`, `\\`, escaped delimiters, and `\uXXXX`), validates control characters and surrogate pairs, and decodes supplementary Unicode characters. |
| Wildcard selector | Implemented | Supports `[*]` and `.*` for arrays and objects. Gson insertion order is currently observed for objects, although RFC 9535 does not define object result order. |
| Array index selector | Implemented | Supports the full I-JSON exact-integer range, negative indexes, and empty results for out-of-range indexes; rejects leading zeros and `-0`. |
| Array slice selector | Implemented | Implements RFC bounds normalization, omitted bounds, positive/negative steps, reverse traversal, empty results for step `0`, and selection from arrays only. |
| Multiple selectors in one child segment | Implemented | Names, indexes, slices, wildcards, and filters can be freely mixed; selector order and duplicate results are preserved. |
| Descendant segment | Implemented | Supports `..name`, `..*`, and bracket forms such as `$..[0]`, `$..['name']`, `$..[0:2,5]`, and `$..[*]`, applying selectors to the input node and each descendant in RFC traversal order. |
| Filter selector | Implemented | The main ANTLR grammar compiles filters as part of the complete JSONPath parse tree. Supports current (`@`) and root (`$`) queries, nested filters, existence tests, parentheses, `!`, `&&`, `\|\|`, `==`, `!=`, `<`, `<=`, `>`, `>=`, and string, number, boolean, and `null` literals. Function expressions remain unavailable as documented below. |
| Function extensions | Missing | RFC functions `length()`, `count()`, `match()`, `search()`, and `value()` are not implemented. |

The RFC allows a query to contain zero or more segments, defines general
comma-separated selector sequences, and specifies filters and standard function
extensions. See the RFC sections for the
[root identifier](https://www.rfc-editor.org/rfc/rfc9535.html#section-2.2),
[selectors](https://www.rfc-editor.org/rfc/rfc9535.html#section-2.3),
[segments](https://www.rfc-editor.org/rfc/rfc9535.html#section-2.5), and
[functions](https://www.rfc-editor.org/rfc/rfc9535.html#section-2.4).

## Accepted subset examples

```text
$
$.store.book
$.корінь.ключ
$['store']["book"]
$.store.*
$[*]
$[0]
$[-1]
$[0,2,4]
$['book','bicycle']
$[0:2,5]
$[2,0:2,*]
$[1:5]
$[1:9:2]
$[::-1]
$..title
$..*
$..['title','name']
$..[0]
$.store.book[?@.price < 10]
$.store.book[?@.isbn]
$.store.book[?@.price <= $.maxPrice && @.available == true]
$.groups[?@.items[?@.active == true]]
$..[?@.status == 'ready']
```

These examples are accepted by the current grammar; partially implemented features
remain subject to the semantic caveats in the table above.

Missing object members and out-of-range array indexes produce no result rather than
an exception. Wildcards and filters operate on both array elements and object member
values. Selector lists preserve selector order and repeated matches. In filter
comparisons, two absent singular-query results compare equal, matching the RFC
`Nothing` rules; an existence test is true when its query selects at least one node,
even when the selected JSON value is `null`.

## RFC conformance roadmap

1. Tighten whitespace and segment grammar to the RFC.
2. Implement the standard `length()`, `count()`, `match()`, `search()`, and `value()`
   function extensions.
3. Add an RFC 9535 conformance suite, including ordering, duplicate-result,
   Unicode, normalized-path, and invalid-query cases.

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
