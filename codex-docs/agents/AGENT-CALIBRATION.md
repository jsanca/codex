# Clio Calibration Notes

## Preferences learned

- Prefer explicit value objects over raw strings.
- Prefer service decorators over direct infrastructure injection.
- Prefer Map<FieldKey, Field> over List<Field> for schema fields.
- Do not introduce dynamic runtime behavior unless explicitly requested.
- If a task repeats an existing pattern, preserve the pattern closely.

## Corrections

- Avoid direct repository access from indexing subscribers; use projection sources.
- Avoid broad runtime builders unless the task asks for them.
- Do not include content values in events.
- Do not rely on unordered repository results in tests.
- If a method is idempotent, do not update updatedAt/updatedBy.

## Linter / formatting

- If code style suggests additional null validations, apply them if they preserve semantics.
- Do not let formatting changes expand into unrelated refactors.
- 
## Backlog classification rule

- Active backlog: may be implemented in the next 1-3 tasks.
- Near-future: design-aware, do not implement unless task explicitly says so.
- Future-forward: document only; do not add code.