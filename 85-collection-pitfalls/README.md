# Module 85 — Pièges des collections : la chasse aux bugs (W11)

Semaine 11 — enrichissement de l'ELO 06 (collections). Après huit semaines à *utiliser* les collections, une séance à les *casser exprès*. Le module livre **dix classes volontairement buggées** (paquet `bugs/`) et leurs **dix miroirs corrigés** (paquet `fixed/`). Ta mission : diagnostiquer chaque piège depuis l'échec d'un test, puis corriger la classe buggée.

Le domaine reste Trans'U : arrêts, lignes, zones, tarifs, cache de prix — exactement les objets des modules précédents, exactement les erreurs que font les vrais développeurs.

## Les dix pièges

| # | Classe buggée | Piège en une ligne |
|---|---|---|
| 1 | `BuggyStation` | `equals` redéfini sans `hashCode` : deux objets « égaux » tombent dans des seaux différents — `HashMap`/`HashSet` les perdent. |
| 2 | `BuggyLine` | Le getter renvoie la liste mutable interne : l'appelant modifie l'objet par-derrière, sans validation. |
| 3 | `BuggyNetwork` | `List.remove` dans un `for`-each sur la même liste : `ConcurrentModificationException`. |
| 4 | `BuggyTicketCache` | Clé instable dans une `HashMap` : le cache « manque » ses propres entrées (héritage du piège 1). |
| 5 | `BuggyFareList` | `Arrays.asList` = vue de taille fixe déguisée en `List` : `add`/`remove` lèvent `UnsupportedOperationException`. |
| 6 | `BuggyGrouping` | `Set.contains` qui répond `false` sans jamais d'exception : le résultat *faux silencieux*, le pire des bugs. |
| 7 | `BuggySortStable` | Comparateur qui rend `1` au lieu de `0` pour des égaux : antisymétrie violée — TimSort finit par jeter l'éponge. |
| 8 | `BuggyNullKey` | `null` dans la liste d'entrée d'un `TreeSet` : `NullPointerException` à l'insertion. |
| 9 | `BuggyRemoveLoop` | `remove(i)` dans un `for` croissant : l'élément voisin est décalé puis sauté — des pairs survivent au nettoyage. |
| 10 | `BuggyMapMerge` | `putIfAbsent` cru pour un `put` : la fusion « réussit » sans jamais écraser les clés communes. |

## Déroulé de la chasse

1. **Décommente** les tests de `PitfallDetectionTest` (retire les annotations `@Disabled`, l'une après l'autre — un piège à la fois).
2. **Exécute** :

       ./mvnw -pl 85-collection-pitfalls test

   Les tests décommentés échouent contre le code de `bugs/` : le message d'assertion (ou l'exception) *est* l'énoncé du bug.
3. **Diagnostique** : de quel piège s'agit-il ? Où est la ligne `// BUG PEDAGOGIQUE:` correspondante ?
4. **Corrige** la classe du paquet `bugs/` (pas le test, pas le miroir `fixed/`).
5. **Re-exécute** — le test passe. Compare ensuite ta correction au miroir `fixed/` : il y a souvent deux façons correctes de faire, et le miroir en montre une avec ses raisons en Javadoc.

> Les deux classes de tests coexistent volontairement : `PitfallFixedTest` (actif, vert) joue les *mêmes* assertions contre le paquet `fixed/` — c'est la preuve que les attentes de la chasse sont les bonnes, et ton contrat de sortie : chaque test corrigé de `PitfallDetectionTest` doit ressembler à son jumeau là.

## Comment lire le code

Dans cet ordre :

1. **`PitfallFixedTest`** — d'abord ce qui est *attendu* (chaque test porte le nom du piège en `@DisplayName`) ;
2. **`bugs/BuggyStation` → `fixed/FixedStation`** — le couple fondateur : `equals`/`hashCode`, tous les autres en découlent ;
3. **`BuggyLine` → `FixedLine`** — la fuite mutable, et pourquoi la copie `List.copyOf` est choisie plutôt que `unmodifiableList` (instantané vs vue) ;
4. **`BuggyNetwork` → `FixedNetwork`** — la CME, puis `removeIf` ;
5. **`BuggyTicketCache`, `BuggyGrouping`** — les conséquences *silencieuses* du piège 1 (résultats faux, aucune exception) ;
6. **`BuggyFareList` → `FixedFareList`** — les collections taille fixe ;
7. **`BuggySortStable` → `FixedSortStable`** — le contrat `Comparator` ;
8. **`BuggyNullKey`, `BuggyRemoveLoop`, `BuggyMapMerge`** — les trois derniers, chacun avec son issue idiomatique ;
9. **`app/PitfallDemo`** — trois pièges joués en direct (trace CME, cache manquant, pairs survivants) puis corrigés.

## Diagramme

`docs/diagrams/85-collection-pitfalls.puml` — les trois mécaniques dessinées : les seaux du `HashMap` qui séparent deux clés « égales », l'itérateur qui explose sur `remove`, la vue taille fixe qui refuse `add`.

## Build

    ./mvnw -pl 85-collection-pitfalls -am clean verify   # 27 tests actifs (miroirs) + analyses warn-only
    ./mvnw -B clean verify                               # tout le fil rouge, 00 → 85

## Pour la suite

- **W12 — JDBC** : la vraie persistance remplace les caches en mémoire — et les clés instables du piège 1 ressurgiront côté `equals` des entités chargées depuis la base.
