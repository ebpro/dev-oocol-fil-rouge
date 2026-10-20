# Progression du fil rouge — DEV-OOCOL

Le fil rouge est un projet Java multi-module qui **avance avec le cours**. Chaque
module est un instantané autonome (« snapshot-per-week ») du réseau de transport
public fictif **Trans'U** : aucune dépendance entre modules, chacun se construit
et se teste seul (`./mvnw -pl <module> verify`).

- **Colonne CNAM** : numéro de séance du plan de cours (source : blueprint du cours,
  fait).
- **Colonne UTLN** : semaine d'introduction dans le déroulé en 12 sections du cours
  [`lecture-UTLN-M1_Info_Math-DEV-OOCOL`](https://github.com/ebpro/lecture-UTLN-M1_Info_Math-DEV-OOCOL)
  (mapping indicatif, ajusté chaque année par l'enseignant).

## Tableau des modules

| Module | Thème / apport pédagogique | Séances CNAM | Semaines UTLN (indicatif) | Section ELO du cours |
|---|---|---|---|---|
| `00-scaffold` | Chaîne d'outils : JDK 21, Maven wrapper, JUnit 6 + AssertJ, CI ; hello-world « Trans'U ». Aucune entité métier. | S1-S3 | S1-S2 | 01 introduction, 02 install, 03 maven |
| `20-domain-bases` | Bases objet sur le domaine : premières entités (Station, Ligne…), encapsulation, identité `equals`/`hashCode` | S4 | S3 | 04 bases |
| `30-heritage` | Héritage, substitution, méthodes abstraites ; Ligne/LigneUrbaine/LigneFerroviaire | S5-S6 | S4 | 05 heritage |
| `40-collections` | Collections : List/Set/Map des arrêts et correspondances ; ordre, doublons, complexité | S7-S8 | S5-S6 | 06 collections |
| `50-exceptions` | Exceptions métier (BilletExpiréException, TerminalIntrouvableException… noms provisoires) ; contrat d'appel | S9 | S7 | 07 exceptions |
| `60-io` | Entrées/sorties : lecture de fichiers d'horaire, flux, try-with-resources | S10 | S8 | 08 io |
| `70-serialisation` | Sérialisation : persistance binaire vs texte du réseau ; transitoire, versionnage | S11 | S9 (début) | 09 serialisation |
| `W9-git-sprint` | **Sprint process (pas de module de code)** : travail collaboratif Git — branches, PR, revues, conflits, Conventional Commits | S12 | S9 (fin) | 10 git |
| `80-modern-java` | Java 8+ : lambdas, Stream API, Optional, records, sealed — réécriture lisible des parcours de lignes | S13-S15 | S10 | 11 after8 |
| `85-collection-pitfalls` | Pièges des collections : vues, `remove` en parcours, clés mutables, comparateurs incohérents | S16 (1re partie) | S11 (début) | 11 after8 |
| `90-jdbc` | JDBC : le réseau en base Postgres — DAO, requêtes préparées, transactions ; matrice CI 21/25 | S16-S17 | S11-S12 | 12 jdbc |
| `95-transit-app` | **Intégration fil rouge** : petite application transport qui rejoue tout le semestre (objet → collections → exceptions → I/O → modern → JDBC) | S18 | S12 (fin) | 12 jdbc |

Séances **S19-S20** : pas de module — révision, soutenance des projets étudiants
et évaluation (aucun code à produire dans ce dépôt).

## Convention de numérotation

Préfixes **dix par dix** avec trous d'insertion (00, 20, 30, …, 95). Un module
supplémentaire (ex. tests : `45-testing`) peut s'insérer sans renuméroter le semestre.

> Les noms d'entités cités en colonne « Thème » sont **illustratifs** ; le vocabulaire
> définitif du domaine est fixé avec le module `20-domain-bases` (séance S4).

## Cycle de vie d'un module

1. **Ouvert** : la semaine correspondante du cours, le module est créé/étendu en CM-TD
   (l'enseignant pousse l'état « guidé »).
2. **Gel** : en fin de séance de travaux dirigés, le module est figé (tag Git
   `<module>-semaine-N`) ; les étudiants qui reprennent le fil rouge partent du tag.
3. **Lecture seule ensuite** : les modules antérieurs ne sont plus modifiés par la
   suite du semestre (principe « snapshot-per-week ») ; les corrections passent par
   issues/PR étudiantes, ce qui alimente le sprint Git (S12).

## État d'implémentation

- [x] `00-scaffold` — **en place** (cette itération)
- [ ] `20-domain-bases` … `95-transit-app` — à créer séance par séance au fil du semestre
