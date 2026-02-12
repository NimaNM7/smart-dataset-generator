# DatasetGeneratorV2 Notes

## Terminology

- `execution strategy`: the high-level algorithm used to generate rows.
- `generation stages`: the ordered list of column groups generated one stage at a time.
- `tools`: reusable `validate/fix` handlers operating on `Map<String, Object>`.

In `DatasetGeneratorV2`, the execution strategy is "SCC-staged generation".
The `generation stages` are produced from SCCs of the dependency graph.

## Execution Strategy (SCC-Staged Generation)

1. Build dependency graph from rules.
2. Compute SCCs.
3. Topologically sort SCCs to get ordered generation stages.
4. For each row:
   - generate values only for columns in the current stage,
   - run `validate/fix` for rules whose required columns are already available,
   - repeat for next stage.
5. Re-run full-row stabilization for intra-row rules.

## Columns Outside Rules

Columns not referenced by any dependency/rule are appended as singleton stages after rule-driven stages.
For the current intra-row scope, placing them first or last is equivalent for correctness.
They are placed last to keep rule-driven generation flow easier to follow while debugging.

## Why We Still Topologically Sort SCCs

JGraphT's `KosarajuStrongConnectivityInspector#stronglyConnectedSets()` often returns an order
that looks topological for that implementation, but this is not guaranteed by the
`StrongConnectivityAlgorithm` interface contract.

For safety and determinism, `DependencySolver#solveTopologically` explicitly builds SCC DAG edges
and applies topological sorting.
