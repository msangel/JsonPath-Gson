# JsonPath-Gson
JsonPath Gson 

Read about https://en.wikipedia.org/wiki/JSONPath 
Official document: https://www.rfc-editor.org/info/rfc9535/

Core idea is implementing as an iterator so it allow lazy evaluation.


## Other implementations
- For java: https://github.com/json-path/JsonPath (short overview: https://www.baeldung.com/java-json-path)

## Compatibility roadmap

The current implementation covers only a small JSONPath subset:

- root selector `$`
- dot child access, for example `$.store.book`
- bracket child access with single quotes, for example `$['store']['book']`
- array indexes, including negative indexes
- wildcard in brackets, for example `$[*]`
- recursive descent, for example `$..title`
- numeric index lists, for example `$[0,2,4]`
- array slices, for example `$[1:5:2]`

To reach an acceptable baseline for the common Goessner-style JSONPath syntax, these features still need to be implemented and tested:

- wildcard after dot notation: `$.store.*`
- recursive wildcard: `$..*`
- bracket child unions by name: `$['book','bicycle']`
- double-quoted bracket strings: `$["store"]["book"]`
- escaping rules for quoted bracket strings, including standard JSON string escapes
- filter expressions: `$[?(@.isbn)]`, `$[?(@.price < 10)]`
- script expressions in brackets: `$[(@.length-1)]`
- current-node selector `@` inside filters and scripts
- root references inside expressions, for example `$..book[?(@.price <= $['expensive'])]`
- comparisons across numbers, strings, booleans, and null
- boolean filter operators: `&&`, `||`, `!`
- regular-expression filters, for example `$[?(@.author =~ /.*REES/i)]`
- deterministic parse errors instead of `UnsupportedOperationException`
- a public API for parse/read/compile operations instead of package-private methods
- a clear return contract for missing definite and indefinite paths
- conformance tests using the sample expressions from the original JSONPath article and RFC 9535 where applicable

Jayway JsonPath compatibility is a larger goal. Features to consider after the baseline is stable:

- static read API: `JsonPath.read(json, path)`
- compiled path API and reusable read context API
- definite vs indefinite path detection
- configurable return behavior similar to `ALWAYS_RETURN_LIST`, `DEFAULT_PATH_LEAF_TO_NULL`, `SUPPRESS_EXCEPTIONS`, and `REQUIRE_PROPERTIES`
- path result mode similar to `AS_PATH_LIST`
- function support: `min()`, `max()`, `avg()`, `stddev()`, `length()`, `sum()`, `keys()`, `concat()`, `append()`, `first()`, `last()`, `index()`
- full filter operator set: `==`, `!=`, `<`, `<=`, `>`, `>=`, `=~`, `in`, `nin`, `subsetof`, `anyof`, `noneof`, `size`, `empty`
- placeholder predicates, for example `$[?]`, and a Java predicate API
- criteria/filter builder API similar to Jayway's `Filter` and `Criteria`
- mapping support from `JsonElement` results to Java types and generic type references
- pluggable JSON providers or at least a documented Gson-only provider boundary
- mutation operations: `set`, `add`, `put`, `replace`, and `delete`
- path cache SPI or a simpler cache strategy for compiled expressions
- stronger thread-safety guarantees for compiled expressions and path nodes


