# Module 30 — Héritage et polymorphisme

La flotte du réseau **Trans'U** prend une hiérarchie (semaine 4, séances
CNAM S4-S5). Une classe abstraite commune, trois espèces de véhicules,
deux interfaces de capacité et une fabrique : le module où l'on apprend à
*remplacer les `if` par des types*.

## Concepts exercés (ELO 05)

- **Classe abstraite** — `Vehicle` : le gabarit non instanciable ; `new Vehicle(...)` est refusé à la compilation, les sous-classes *doivent* répondre aux méthodes `{abstract}` ;
- **Héritage (`extends`)** — `MetroTrain`, `Bus`, `Tramway` héritent des champs `protected` et des méthodes concrètes du parent : un seul `annoncer()` écrit, trois réponses ;
- **Polymorphisme** — `serviceAnnonce(Vehicle)` : la méthode résolue dépend de l'objet **réel** (dispatch virtuel), jamais du type de la variable ;
- **Substituabilité (LSP)** — tout `Vehicle` convient là où un `Vehicle` est attendu ; `Line` est le contre-exemple *résolu* : une ligne **a** un véhicule, elle n'**est** pas un véhicule (composition, pas héritage) ;
- **Séparation des interfaces (ISP)** — `Capteur` et `Rechargeable` sont des *capacités* optionnelles : `Tramway` les implémente, `Bus` n'est **pas obligé** de le faire ;
- **Méthode par défaut d'interface** — `Capteur.estEnSurvitesse()` : du comportement partagé dans l'interface, sans réécriture ;
- **Retour covariant** — `MetroTrain.copier()` renvoie `MetroTrain` : légal (sous-type au retour), l'appelant n'écrit aucun cast ;
- **Membres statiques et masquage** — `Vehicle.nombreVehicules()` est partagé par toute la flotte ; une statique ne se surcharge jamais par le bas : elle se **masque** (résolution par le type statique, zéro polymorphisme — la scène 6 de la démo) ;
- **Patron Factory** — `VehicleFactory.creer("metro", ...)` : le `switch` d'espèce est enfermé dans la fabrique, le code client ne voit que le type large ;
- **Composition et délégation** — `Line.annoncerService()` transmet la demande à son véhicule affecté : changer le matériel change l'annonce sans rien réécrire dans `Line`.

## Comment lire le code

Dans cet ordre — du gabarit aux patrons :

1. **`Vehicle`** — qu'est-ce que `{abstract}` interdit ? Pourquoi `immatriculation` est-elle `protected` et `compteurVehicules` `private static` ? Où exactement le constructeur du parent est-il exécuté par les enfants ?
2. **`MetroTrain`** — l'enfant modèle : `super(...)` en tête, `@Override` partout, et surtout `copier() : MetroTrain` — le **retour covariant** (notez le Javadoc, il explique pourquoi c'est légal alors que le paramètre covariant ne l'est jamais).
3. **`Bus`** — le contraste voulu : son `copier()` renvoie `Vehicle`. Comparez avec le métro : qui doit écrire un cast, et pourquoi ? `peutDoubler()` rappelle qu'une capacité spécifique n'appartient pas au parent.
4. **`Capteur` / `Rechargeable`** — deux interfaces, deux responsabilités. Pourquoi ne pas les fusionner en une seule `VehiculeIntelligent` ? (Réponse : ISP.)
5. **`Tramway`** — l'enfant cumule : un `extends` **et** deux `implements`. Lisez l'ordre : `extends` d'abord, `implements` ensuite — l'héritage d'espèce avant les capacités.
6. **`VehicleFactory`** — classe utilitaire (constructeur privé qui lève `AssertionError`) : la création cachée derrière le type large.
7. **`Line`** — la leçon LSP par la négative : le Javadoc de classe explique pourquoi `Line extends Vehicle` serait un piège (une ligne n'a ni vitesse ni immatriculation). Le test `lineNeHeritePasDeVehicle()` le prouve par réflexion.
8. **`PatronDemo`** (`app`) — les six scènes exécutées. Exécutez-le, modifiez-le, cassez-le.

## Lancer la démonstration

```bash
./mvnw -pl 30-heritage -am package
java -cp 30-heritage/target/classes fr.ebruno.dev.oocol.heritage.app.PatronDemo
```

## Construire et tester le module

```bash
./mvnw -pl 30-heritage test      # uniquement les tests du module
./mvnw -pl 30-heritage verify    # tests + jacoco + analyse statique warn-only
./mvnw -B clean verify           # tout le projet (comme la CI)
```

## Schéma

`docs/diagrams/30-heritage.puml` — la hiérarchie complète (héritage,
réalisations d'interfaces, fabrique, composition) avec les notes LSP/ISP
et la légende des flèches.

## Ce qui vient après

- **W5 — module `40-collections`** : la flotte sort des variables nommées — `List<Vehicle>`, parcours polymorphes d'un `annoncer()` sur toute la flotte, tri par `estPlusRapideQue` via un `Comparator` ; le tableau de `PatronDemo` deviendra une vraie collection ;
- **Module `50-exceptions`** : `IllegalArgumentException` cédera la place à des exceptions métier (`ImmatriculationInvalideException`) et à un contrat d'appel explicite ;
- **Module `70-serialisation`** : sauvegarder la flotte — le compteur statique `compteurVehicules` y révèlera son vrai visage (il ne se sérialise pas : pourquoi ?) ;
- **Module `80-modern-java`** : `record`, `sealed` et le *pattern matching* sur `instanceof` (`if (v instanceof Capteur c)`) — ce module s'en interdit volontairement l'usage pour que vous en mesuriez l'apport.

## Règles de ce module (à ne pas contourner)

- Pas de collections `List`/`Set`/`Map` (module 40) — la démo utilise un simple tableau ;
- Pas de generics écrits par vous (module 40) ;
- Pas de `record`, `sealed`, ni pattern matching `instanceof Type t` : module 80 ;
- Pas de lambdas ni de streams : module 80 ;
- Pas d'exceptions personnalisées : `IllegalArgumentException` suffit (module 50).
