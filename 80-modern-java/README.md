# Module 80 — Java Moderne (ELO 11)

Semaine 10-11 (UTLN) / Séances S12-S14 (CNAM). Le réseau Trans'U existe depuis W2 : lignes, arrêts, zones, véhicules, tarifs — le tout écrit dans le Java verbeux et défensif des semaines précédentes. Cette semaine, **on le réécrit avec les outils de Java 21. Le diff EST la leçon** : les mêmes opérations, les mêmes résultats (prouvés égaux en tests), dix fois moins de lignes — et trois garanties que le compilateur prend enfin à sa charge.

## Concepts exercés (ELO 11)

- **records** — données pures immuables : constructeur canonique, accesseurs, `equals`/`hashCode`/`toString` générés ; le constructeur compact pour les gardes (`Zone`, `Station`, `Ligne` et leurs 40 lignes d'avant tenues en 5) ;
- **sealed** — hiérarchies fermées (`Vehicle permits MetroTrain, Bus, Tramway` ; `Propulsion` ; `Tarif`) : la liste des sous-types est un fait de compilation, pas une convention d'équipe ;
- **pattern matching for instanceof** — `if (v instanceof MetroTrain m)` : tester, transformer et lier en une fois ; la fin du couple `instanceof` + cast ;
- **switch expressions** — flèches, valeur de retour, `yield` implicite ; sur types scellés, le switch est **vérifié exhaustif** : pas de `default`, pas de branche « type inattendu » ;
- **Streams API** — pipeline paresseux (source → intermédiaires → terminale) : `filter`, `map`, `flatMap`, `sorted`, `distinct`, `min`, `count`, `mapToDouble().average()`, `Collectors.groupingBy`, `toList()` ;
- **lambdas + method references** — `s -> s.accessible()`, `Station::zone`, `AnalyseReseau::prixDe` : dire *quoi* calculer, plus *comment* itérer ;
- **var** — inférence locale, là où le type saute aux yeux (`var zone = new Zone(1, "Centre")`) ; jamais sur les paramètres ni les types de retour ;
- **text blocks** — `"""` : le multi-lignes sans `\n` ni `\"` (cf. `AnalyseReseau.resumerReseau` contre son jumeau `StringBuilder`) ;
- **Optional** — `trouverParCode` renvoie un contrat typé, plus un `null` deviné ; `map`/`flatMap`/`orElseThrow`/`ifPresent` en chaîne ;
- **List.of / Set.of / Map.of** — collections immuables en un mot (`List.copyOf` dans les constructeurs compacts pour la copie défensive).

## Records ou classe ?

| Situation | Choix |
|---|---|
| Données pures, sans état mutable (arrêts, zones, tarifs) | **record** |
| Comportement riche + état qui évolue (cycle de vie, cache) | **classe** |
| Variantes fermées d'un domaine (véhicules, propulsions) | **interface sealed + records internes** |
| Ajout fréquent de sous-types par des tiers | classe ouverte (mais est-ce vraiment le besoin ?) |

## Comment lire le code

Dans cet ordre — le « après » d'abord, le miroir « avant » ensuite :

1. **`Zone` → `Station` → `Ligne`** — les trois records : gardes du constructeur compact, `List.copyOf`, `Optional` dans la signature ;
2. **`Vehicle` + `MetroTrain` / `Bus` / `Tramway`** — la hiérarchie scellée ; `Propulsion` + ses trois records — l'union sans méthodes ;
3. **`Tarif` + `TarifUnite` / `TarifJour` / `TarifAbonnement`** — le support du switch de motifs ;
4. **`AnalyseReseau`** — chaque méthode = 1-3 lignes de stream ;
5. **`VersionLegacy`** — le miroir W4-W7 : boucles, casts, `Map.put`, `StringBuilder`, `Comparator` anonyme. Résultat **strictement identique** (prouvé par `StreamsTest`) ;
6. **`ModernJavaDemo`** (`app`) — les sept scènes exécutées, commentaires en français, fichier temporaire détruit en `finally`.

## Lancer la démonstration

    ./mvnw -pl 80-modern-java -am clean verify   # les 32 tests + la démo compilée

## Pour la suite

- **W12 — JDBC** : la vraie persistance remplace le snapshot sérialisé de W8 ; les records du réseau deviennent des lignes de tables, et la question « avant/après » se reposera au niveau du SQL embarqué.
