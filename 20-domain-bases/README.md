# Module 20 — Fondations du domaine

Premières classes du réseau de transport fictif **Trans'U** (semaine 3,
séances CNAM S4-S5). Quatre entités, zéro framework, zéro collection,
zéro héritage : uniquement le langage Java pour apprendre à *faire tenir*
un modèle entre ses mains.

## Concepts exercés (ELO 04)

- **Classes et objets** — déclarer, instancier (`new`), distinguer la classe de ses instances ;
- **Encapsulation** — attributs `private`, accesseurs/modificateurs, *validation dans le constructeur et les setters* : un objet qui existe est un objet valide ;
- **Constructeurs surchargés** — plusieurs façons de construire le même objet, avec délégation `this(...)` et valeurs par défaut centralisées ;
- **Membres statiques** — compteurs de classe (`Station.nombreStations()`, `Line.nombreLignes()`) et fabriques statiques (`Ticket.jour(zone)`) ;
- **`final`** — attributs immuables (`Zone`, `Ticket`) et constantes (`Line.FREQUENCE_DEFAUT_MIN`) ;
- **Paquets** — séparation `domain` (le modèle) / `app` (la démonstration) ;
- **Javadoc** — chaque classe et méthode publique est documentée en français ; le `package-info.java` de chaque paquet explique son rôle.

## Comment lire le code

Dans cet ordre — du plus simple au plus riche :

1. **`Zone`** — la classe minimale : deux champs `final`, validation, `equals`/`hashCode` cohérents. Pourquoi immuable ? Pourquoi `equals` et `hashCode` doivent-ils « correspondre » ?
2. **`Station`** — la classe d'encapsulation : pourquoi les champs sont `private`, que se passerait-il sans la validation du `code`, que compte exactement `compteurInstances` ?
3. **`Line`** — la composition : une ligne *tient* deux stations. Lisez le Javadoc de classe en entier : il explique la **tension** laissée volontairement ouverte (deux extrémités seulement).
4. **`Ticket`** — les fabriques statiques : le constructeur est privé ; comparez `Ticket.jour(zone)` et `new Ticket("Journée", 5.20, zone)` — lequel se relit sans effort dans six mois ?
5. **`ReseauDemo`** (`app`) — le tout exécuté : chaque bloc `println` démontre une notion. Exécutez-le, modifiez-le, cassez-le.

## Lancer la démonstration

```bash
./mvnw -pl 20-domain-bases -am package
java -cp 20-domain-bases/target/classes fr.ebruno.dev.oocol.bases.app.ReseauDemo
```

## Construire et tester le module

```bash
./mvnw -pl 20-domain-bases test      # uniquement les tests du module
./mvnw -pl 20-domain-bases verify    # tests + jacoco + analyse statique warn-only
./mvnw -B clean verify               # tout le projet (comme la CI)
```

## Ce qui vient après

- **W4 — module `30-heritage`** : abstractions et héritage (p. ex. un ancêtre commun aux titres, des comportements polymorphes) ;
- **W5 — module `40-collections`** : la tension de `Line` sera levée — `List<Station>` remplacera les deux extrémités fixes, et `distanceEstimeeKm()` deviendra un vrai calcul ;
- **W8** : la validité des tickets (`estValide()`) confrontera son booléen toujours-vrai à une vraie date d'expiration (sérialisation).

## Règles de ce module (à ne pas contourner)

- Pas d'héritage ni d'interface (module 30) ;
- Pas de collections `List`/`Set`/`Map` (module 40) ;
- Pas d'exceptions personnalisées : `IllegalArgumentException` suffit (module 50) ;
- Pas de `record`, `sealed` ni pattern matching : ce sera le module 80.
