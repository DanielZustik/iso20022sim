# Domain Docs

How the engineering skills should consume this repo's domain documentation when exploring the codebase.

## Layout

This repo uses a single-context layout:

- `CONTEXT.md` at the repo root
- `docs/adr/` at the repo root

## Before exploring, read these

- `CONTEXT.md` for project domain language and glossary terms.
- Relevant ADRs under `docs/adr/` for architectural decisions that touch the work area.

If any of these files do not exist, proceed silently.

## Use the glossary's vocabulary

When output names a domain concept, use the term as defined in `CONTEXT.md`. Do not drift to synonyms the glossary explicitly avoids.

If the concept you need is not in the glossary yet, note it as a possible documentation gap for `grill-with-docs`.

## Flag ADR conflicts

If output contradicts an existing ADR, surface it explicitly rather than silently overriding it.
