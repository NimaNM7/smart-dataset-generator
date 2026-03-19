Below is a compact but complete design summary of the system we designed in this chat.
It is written so you can give it directly as context to an implementation agent (like Codex CLI).

The goal is to implement Version 1 of a fast synthetic dataset generator based on constraint-aware generation, dependency ordering, and local SCC solvers, instead of global solving like Choco Solver.

---

# Synthetic Dataset Generator — Architecture Specification (V1)

## Goal

Generate large synthetic datasets quickly while satisfying a set of data quality rules / constraints.

The generator must:

* Avoid global backtracking
* Run in O(N × M) time (rows × columns)
* Use domain narrowing instead of validate-and-repair
* Solve cyclic constraints locally using SCC solvers

---

# 1. Core Generation Strategy

Instead of:

generate → validate → repair
we do:

compute allowed domain → sample value inside domain
Domains are narrowed using constraints and already generated values.

This ensures most constraints are never violated during generation.

---

# 2. High Level Architecture

DatasetGenerator
├── RulePlanner
├── ExecutionPlan
├── RowGenerationEngine
└── RuleProcessingContext
Flow:

rules + schema
↓
RulePlanner
↓
ExecutionPlan
↓
DatasetGenerator
↓
RowGenerationEngine
↓
generated dataset
---

# 3. Rule Types

The system supports rule types grouped by how they affect generation.

## Group A — Cell Rules

Single column rules.

Examples

regex
format rules
syntactic validation
Strategy

Generate values directly from valid domain.

---

## Group B — Row Rules

Multiple columns in the same row.

Examples

A ≤ B
A ≥ B
start_date ≤ end_date
status → release_date not null
These produce column dependencies.

Example

start_date → end_date
price → discount
---

## Group C — Identity Rules

Consistency across rows.

Examples

functional dependency
postcode → city
unique keys
duplicate rules
Handled using memory in context.

---

## Group D — Structural Rules

Relationships between rows.

Examples

parent-child hierarchy
child_budget ≤ parent_budget
Handled via row generation order.

---

## Group E — Global Statistical Rules

Examples

sum(quota) ≤ 100
monotonic values
outlier bounds
Handled using incremental statistics in context.

---

# 4. RulePlanner

The RulePlanner analyzes rules and produces the ExecutionPlan.

Responsibilities:

1 classify rules
2 build column dependency graph
3 detect SCCs
4 compute column generation order
5 build SCC solvers
6 build constraint index
---

# 5. Column Dependency Graph

Directed graph between columns.

Example:

Status → Release_Date
Price → Discounted_Price
Postcode → City
Meaning:

Release_Date depends on Status
Discounted_Price depends on Price
City depends on Postcode
---

# 6. SCC Detection

Cycles are detected using Tarjan SCC algorithm.

Example:

A + B = 100
A ≤ B
Graph:

A ↔ B
SCC:

{A,B}
These columns must be solved together.

---

# 7. ExecutionPlan Structure

ExecutionPlan
├ columnTopoOrder
├ sccComponents
├ sccSolvers
├ constraintIndex
├ rowOrderStrategy
└ ruleMetadata
columnTopoOrder = order of components after SCC compression.

Example

[Status]
[Price]
[Postcode]
[A,B]  ← SCC
[Discount]
[City]
---

# 8. RuleProcessingContext

Stores runtime memory.

RuleProcessingContext
├ fdMaps
├ uniqueKeySets
├ hierarchyInfo
├ aggregateStats
├ monotonicStats
└ randomGenerator
Examples

Map<Postcode, City>
Set<CustomerID>
Map<ProjectID, RemainingQuota>
Map<NodeID, ParentID>
All operations must be O(1).

---

# 9. RowGenerationEngine

Main dataset generation loop.

for row in rows:

    create RowGenerationState

    for component in columnTopoOrder:

        if component size == 1:
            generateColumn()

        else:
            run SCCSolver
---

# 10. RowGenerationState
Temporary state for generating a row.

RowGenerationState
├ values[column]
├ domains[column]
└ context reference
Example

values:
Status = Released

domains:
Release_Date ∈ [2000..2026]
Discounted_Price ∈ [0..1000]
Domains shrink during generation.

---

# 11. Domain Object

Represents allowed values for a column.

Domain
├ min
├ max
├ allowedSet
└ exclusions
Examples

Numeric

Price ∈ [0,1000]
Categorical

Status ∈ {Draft, Released, Archived}
---

# 12. Domain Narrowing Engine

Each rule is implemented as a DomainConstraint.

Interface:

DomainConstraint
├ involvedColumns
└ narrowDomain(state)
Example

Rule

discount ≤ price
Implementation

narrowDomain():
price = state.value(Price)
if price exists:
domain(Discount).max = price
---

# 13. Constraint Index

Constraints are indexed by column for fast lookup.

constraintIndex:

column → list of constraints
Example

Price → [DiscountConstraint]
Discount → [DiscountConstraint]
Status → [ReleaseDateConstraint]
So narrowing is O(number_of_constraints_for_column).

---

# 14. Sampling

After domain narrowing, values are sampled.

Numeric

random(min,max)
Categorical

random element from allowed set
---

# 15. Handling Functional Dependencies

Example

postcode → city
Algorithm

postcode = generate()

if postcode in fdMap:
city = fdMap[postcode]
else:
city = generateCity()
fdMap[postcode] = city
---

# 16. Unique Key Constraints

Context stores used keys.

Set<Key>
Algorithm

generate candidate
while candidate exists in set:
regenerate

add to set
---

# 17. Aggregate Constraints

Example

sum(quota) ≤ 100
Context stores

remainingQuota
Generation

quota ∈ [0 , remainingQuota]
remainingQuota -= quota
---

# 18. Hierarchical Constraints

Example

child_budget ≤ parent_budget
Context stores

parentBudget
Domain narrowing

child_budget.max = parentBudget
---

# 19. SCC Solver

Used when column dependencies form cycles.

Structure

SCCSolver
├ variables
├ constraints
├ propagationEngine
└ solutionSampler
Typical SCC size: 2–5 variables.

---

# 20. SCC Domain Propagation

Iteratively shrink domains.

Algorithm

repeat until stable:
for constraint:
narrow domains
Example

Constraints

A + B = 100
A ≤ B
Domains become

A ∈ [0,50]
B ∈ [50,100]
---

# 21. SCC Solution Sampling

Steps

1 choose pivot variable
2 sample value
3 derive dependent variables
4 validate domains
5 retry if necessary
Example

sample A = 20
B = 100 - A = 80
---

# 22. Final Generation Algorithm

for each row:

    state = new RowGenerationState

    for component in columnTopoOrder:

        if component.size == 1:
            generateColumn(state)

        else:
            sccSolver.solve(state)

    update RuleProcessingContext
---

# 23. Complexity

Let

N = rows
M = columns
Generation complexity

O(N × M)
Because

constraint checks = constant
context lookups = O(1)
No global solving or search trees.

---

# 24. Design Principles

The system is based on:

dependency ordering
domain narrowing
incremental statistics
local SCC solving
instead of global constraint solving like Choco Solver.

This keeps the generator fast and scalable for large datasets.

---

This is the complete specification for Version 1 implementation.

Next iterations may add:

constraint compilation
advanced sampling distributions
parallel generation
rule optimization