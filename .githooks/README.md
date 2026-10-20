# Hooks Git — dev-oocol-fil-rouge

Ce répertoire contient les hooks Git du fil rouge : messages de commit normalisés
et vérifications automatiques avant commit/push.

## Installation (une seule fois, à la racine du dépôt)

```bash
git config core.hooksPath .githooks
```

Le conteneur de développement (`.devcontainer/devcontainer.json`) exécute déjà cette
commande à sa création.

## Hooks disponibles

### `commit-msg` — Conventional Commits

Valide le format `<type>(<portée>): <description>` (72 caractères max), cf.
[conventionalcommits.org](https://www.conventionalcommits.org/).

Types acceptés : `feat`, `fix`, `docs`, `style`, `refactor`, `test`, `chore`, `ci`,
`perf`, `build`, `revert`.

Exemples :

```
feat(00-scaffold): add transit greeting
fix(40-collections): keep stop order stable in Line#stops()
docs: update progression table for week 7
test(30-heritage): add covariant-return regression test
```

### `pre-commit` — compile + tests

Lance `./mvnw clean compile` puis `./mvnw test`. Un commit de code qui ne compile
pas ou dont les tests sont rouges est rejeté — la CI ne devrait jamais découvrir
une erreur que le hook voyait déjà en local.

### `pre-push` — vérification complète

Lance `./mvnw clean verify` (tests + jacoco + analyses statiques warn-only) et
signale les marqueurs `TODO`/`FIXME` des fichiers `.java` indexés (informatif).

## Désactiver temporairement (avec parcimonie !)

```bash
git commit --no-verify -m "..."
git push --no-verify
```

À réserver aux commits « travaux en cours » explicitement autorisés par l'enseignant ;
le hook existe pour que la CI reste le dernier filet, pas le premier contrôle.
