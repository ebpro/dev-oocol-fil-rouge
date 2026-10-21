# Module 40 — Collections framework

Le réseau **Trans'U** quitte les variables nommées (semaine 5, séances
CNAM S6-S7). Une ligne n'est plus une paire de stations mais une
`List<Station>` ; le graphe entier tient dans une `Map<Station, Set<Line>>` ;
un itinéraire se calcule en largeur avec une `Queue` ; et tout cela repose
sur le contrat `equals`/`hashCode` — la semaine où les collections cessent
d'être une boîte à objets pour devenir un **choix d'ingénierie**.

## Concepts exercés (ELO 06)

- **`List` (ordre + accès par position)** — `Line.arrets` : un parcours de bus EST une séquence ; `get(i)`, `indexOf`, premier/dernier — la spécialité de la liste ordonnée, là où `Set` aurait perdu le sens du trajet ;
- **`Set` (anti-doublon sémantique)** — `Network.tousLesArrets()` : une station présente sur trois lignes n'apparaît qu'une fois dans l'union, parce que le `HashSet` consulte `equals` avant d'accepter ;
- **`Map` (lookup par clé)** — `Network.correspondances` : la question « quelles lignes passent ici ? » se répond en O(1), contre le O(n) volontairement naïf de `Line.trouverParCode` — comparer les deux, c'est comprendre le choix du conteneur ;
- **`Queue` (file FIFO)** — la frontière du BFS, implémentée par `LinkedList` : le comportement « premier arrivé, premier exploré » est exactement la sémantique d'un itinéraire en largeur ;
- **Généricité** — `List<Station>`, `Map<Station, Set<Line>>`, `<T> List<T> inverser(List<T>)` : le type est vérifié à la compilation, aucun cast n'apparaît dans le code — et `TripPlanner.inverser` prouve qu'un algorithme de collection ne dépend pas du type qu'il manipule ;
- **Contrat `equals`/`hashCode`** — `Station` est la clé de TOUTES les collections du module : deux instances de même code doivent être égales ET avoir le même hash, sinon `contains`/`remove`/`HashMap.get` ratent **en silence**. `Line.demontrerBugSansEquals()` rejoue la scène, résultats à l'appui ;
- **Clé immuable** — `Station.code` est `final` (le setter du module 20 a disparu) : une clé de hachage modifiée après insertion « perd » son objet dans le mauvais tiroir ;
- **Ordre naturel (`Comparable`)** — `Collections.sort(liste)` sans second argument : la station se range par code, la zone par numéro — l'objet trié porte son ordre, cohérent avec son `equals` ;
- **Ordre délégué (`Comparator`, patron Strategy sans le nom)** — `Station.parNom()`, `FareCalculator.trierParNom` : un objet comparateur passé en paramètre permet de trier SANS toucher à la classe — la stratégie vient du dehors ;
- **Écriture protégée** — `Collections.unmodifiableList/Set` sur tous les getters de collection, `Collections.emptySet()` plutôt que `null` pour l'inconnu : l'appelant lit sans pouvoir corrompre, l'absence garde une forme itérable ;
- **Copie défensive à l'entrée** — `FareCalculator` copie puis trie les collections qu'on lui prête : il ne devient jamais l'otage d'une liste que l'appelant modifierait après coup (l'encapsulation de W3, appliquée aux conteneurs).

## Comment lire le code

Dans cet ordre — de la clé d'identité à l'algorithme :

1. **`Station`** — l'épicentre. Pourquoi le `code` est-il `final` ? Que comparent exactement `equals` et `compareTo`, et pourquoi leurs réponses doivent coïncider ? Lisez `parNom()` : le `Comparator` est un **objet** qu'on fabrique, pas un mot-clé.
2. **`Zone`** — le second `Comparable`, plus simple : la soustraction des numéros donne le signe voulu (et ses limites, discutées en séance).
3. **`Line`** — la transformation W3 → W5. Repérez les trois méthodes qui dépendent de `Station.equals` sans le dire : `ajouterArret` (via `contains`), `retirerArret`, `estTerminus`. Lisez `demontrerBugSansEquals()` en dernier — c'est la conclusion de la scène, pas l'ouverture.
4. **`Network`** — la `Map` vedette. Le type `Map<Station, Set<Line>>` se lit « station → ENSEMBLE des lignes qui la desservent » : deux généricités imbriquées, chacune justifiée. Pourquoi `lignesA` renvoie-t-il un ensemble VIDE plutôt que `null` ?
5. **`TripPlanner`** — le BFS, commenté pas à pas : la `Queue` explore, le `Set` protège des retours en arrière, la `Map` des prédécesseurs est le fil d'Ariane, la `List` rend le chemin. `inverser` est la généricité en acte : un `<T>` que l'appelant remplit.
6. **`FareCalculator`** — les deux philosophies du tri côte à côte : `trierParNumero` (un argument, l'ordre vient des objets) vs `trierParNom` (deux arguments, l'ordre vient d'un objet stratégie fourni par l'appelant).
7. **`ReseauDemo`** (`app`) — les six scènes exécutées. Exécutez-le, modifiez-le, cassez-le.

## Lancer la démonstration

```bash
./mvnw -pl 40-collections -am package
java -cp 40-collections/target/classes fr.ebruno.dev.oocol.collections.app.ReseauDemo
```

## Construire et tester le module

```bash
./mvnw -pl 40-collections test      # uniquement les tests du module
./mvnw -pl 40-collections verify    # tests + jacoco + analyse statique warn-only
./mvnw -B clean verify              # tout le projet (comme la CI)
```

## Schéma

`docs/diagrams/40-collections.puml` — le graphe des conteneurs (la `Map`
d'adjacence, la `List` d'arrêts, les `Comparable`, le `Comparator` stratégie
et la `Queue` du BFS) avec les notes equals/hashCode et W3→W5.

## Ce qui vient après

- **Module `50-exceptions`** : le « vide vaut échec » du `TripPlanner` et le `null` de `trouverParCode`/`trouverStationParCode` céderont la place à des exceptions métier explicites — les commentaires des deux méthodes l'annoncent déjà ;
- **Module `80-modern-java`** : les `Comparator` anonymes de `Station.parNom()` et `FareCalculator.trierParNom()` deviendront des **lambdas** en une ligne, le `null` de `trouverStationParCode` un `Optional<Station>`, et les boucles de copie des streams. Ce module s'en interdit volontairement l'usage pour que la modernité de W10 se mesure en diff ;
- **Ouverture (non exigée, non importée ici)** : *Eclipse Collections* (anciennement GS Collections) offre les mêmes rôles — `RichList`, `MutableSet`, `Bags`, `Maps` immuables — avec des méthodes d'instance au lieu de `Collections.xxx` statiques. Regarderez sa doc plus tard, vous retrouverez exactement les choix de conteneurs de cette semaine ; aucune dépendance ajoutée au module, volontairement.
