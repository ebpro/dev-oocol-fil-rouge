# Diagrammes UML du fil rouge

Ces diagrammes illustrent l'évolution du domaine à chaque module. Ils sont rendus
via PlantUML.

Chaque fichier `.puml` est une **photographie pédagogique** d'un instantané du
réseau fictif **Trans'U** (cf. [`../progression.md`](../progression.md)). Il
n'exécute rien et ne remplace jamais le code : le code source reste l'autorité.

## Comment rendre un diagramme

Plusieurs voies, de la plus confortable à la plus ponctuelle :

- **VS Code** — extension *PlantUML* : ouvrir le `.puml` puis `Alt+D`
  (prévisualisation live). Nécessite Java + Graphviz (`dot`) sur la machine.
- **Ligne de commande** :

      java -jar plantuml.jar docs/diagrams/*.puml -tpng

  génère un PNG par fichier dans le même dossier.
- **En ligne** — coller le contenu du fichier sur
  [plantuml.com/plantuml](https://www.plantuml.com/plantuml/uml/).
- **Notebooks Quarto** — la magic `%%plantUML SVG` du noyau IJava intègre le
  diagramme directement dans la séance rendue (à réserver aux notebooks du
  cours, pas à ce dépôt Java).

## Convention

- **Un `.puml` par module**, nommé `<NN>-<concept>.puml`, exactement comme le
  module Maven correspondant (`20-domain-bases` → `20-domain-bases.puml`).
- Le préfixe numérique reprend la numérotation « dix par dix » du fil rouge.
- Un diagramme reste centré sur **le domaine** : quelques classes, les relations
  porteuses de sens, et des notes qui annoncent la suite (W5, W8…). Ce n'est pas
  une documentation d'architecture exhaustive.

## Diagrammes planifiés

| Fichier | Module | Montre |
|---|---|---|
| `20-domain-bases.puml` | W3 | Domaine initial (POJO, encapsulation) |
| `30-heritage.puml` | W4 | Hiérarchie de véhicules, interfaces, Factory *(planifié)* |
| `40-collections.puml` | W5 | Graphe réseau, `List<Station>`, TripPlanner *(planifié)* |
| `50-exceptions.puml` | W6 | Hiérarchie d'exceptions *(planifié)* |
| `70-serialisation.puml` | W8 | Structure du snapshot *(planifié)* |
| `80-modern-java.puml` | W10 | Records + sealed + pattern matching *(planifié)* |
| `90-jdbc.puml` | W12 | Motif DAO, Repository, mapping de tables *(planifié)* |
| `95-transit-app.puml` | Final | Builder, Strategy, assemblage complet *(planifié)* |

> Seul `20-domain-bases.puml` existe à ce stade ; les autres sont des pistes
> pour les séances suivantes et seront ajoutés quand le module correspondant
> sera gelé.

---

*Ces diagrammes ne sont pas du code exécutable — ce sont des illustrations
pédagogiques. Le code source reste l'autorité.*
