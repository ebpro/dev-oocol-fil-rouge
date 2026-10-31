# dev-oocol-fil-rouge

**Fil rouge du cours DEV-OOCOL** (M1 Informatique & Mathématiques, UTLN / CNAM).

Projet Java **multi-module Maven** qui grandit avec le semestre : chaque module est un
**instantané autonome** du réseau de transport public fictif **Trans'U**, construit
autour de la notion pédagogique travaillée cette semaine-là (objets, héritage,
collections, exceptions, E/S, sérialisation, modern Java, JDBC…).

- Cours support (notebooks Quarto) : [`ebpro/lecture-UTLN-M1_Info_Math-DEV-OOCOL`](https://github.com/ebpro/lecture-UTLN-M1_Info_Math-DEV-OOCOL)
- Progression complète (11 modules, 12 semaines UTLN / 20 séances CNAM) : [`docs/progression.md`](docs/progression.md)
- Style de commits imposé : [Conventional Commits](https://www.conventionalcommits.org/) (hook `commit-msg`)

## Prérequis

| Outil | Version | Remarque |
|---|---|---|
| JDK | **25** | un seul JDK suffit pour travailler |
| Maven | **aucune installation requise** | le dépôt livre le wrapper `./mvnw` (Maven 3.10.0) |
| Git | récent | hooks optionnels : `git config core.hooksPath .githooks` |

Aucun plugin d'IDE n'est requis : tout se vérifie en ligne de commande. L'IDE
(IntelliJ, VS Code+Java, Eclipse) est une commodité pour lire le code.

## Construire

```bash
# Tout le projet (les 11 modules du fil rouge)
./mvnw verify

# Un seul module, sans ses dépendants (les modules sont indépendants)
./mvnw -pl 00-scaffold verify

# Compiler puis lancer le programme de démonstration
./mvnw -q -pl 00-scaffold compile
java -cp 00-scaffold/target/classes fr.ebruno.dev.oocol.scaffold.TransitHello
```

`verify` enchaîne : compilation → tests JUnit 6 + AssertJ → couverture JaCoCo →
Checkstyle / PMD / SpotBugs **en mode avertissement uniquement** (jamais bloquant :
les étudiants apprennent à lire ces rapports progressivement, ils ne cassent pas
leur build).

## Ouvrir dans un IDE

- **Fichier → Ouvrir → `pom.xml`** (racine) — jamais un sous-module isolé : c'est le
  POM parent qui centralise versions de plugins et dépendances.
- **VS Code** : ouvrir le dossier à la racine du dépôt, l'extension Java détecte le
  projet Maven.
- **Conteneur de développement** : `.devcontainer/` fournit JDK 25, Git et le
  hook path déjà configurés (Reopen in Container).

## IntelliJ IDEA

À partir de **IntelliJ IDEA 2025.2+** (niveau de langage Java 25 supporté). Les
intégrations Maven, Gradle, debugger et tests sont **incluses** : aucun plugin
requis. Dans `File → Project Structure`, définir le **Project SDK sur JDK 25**
(IDEA détecte automatiquement les JDK installés via sdkman). Le conteneur de
développement est destiné aux utilisateurs de VS Code ; avec IDEA, travailler
directement sur l'hôte.

## Structure

```
pom.xml                  ← parent : pluginManagement + dependencyManagement + enforcer
.github/workflows/       ← CI : build.yml (matrice JDK 25), codeql.yml, dependabot
.githooks/               ← commit-msg (Conventional Commits), pre-commit, pre-push
build-tools/             ← checkstyle.xml, pmd-ruleset.xml (warn-only)
docs/progression.md      ← mapping modules × semaines UTLN × séances CNAM
00-scaffold/             ← S1-S3 / semaines 1-2 : chaîne d'outils, TransitHello
```

## Convention de nommage des modules

`NN-thème` : préfixe numérique **dix par dix** (00, 20, 30, … 95) pour garder des
trous d'insertion entre deux séances. Les modules n'ont **aucune dépendance entre
eux** (architecture « snapshot-per-week ») : reprendre le fil rouge à la semaine N
ne demande que les modules ≥NN de la semaine N.

## Licence

MIT.
