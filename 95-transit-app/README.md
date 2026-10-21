# Module 95 — Application Trans'U (Synthèse)

13 semaines de travail aboutissent ici. Le réseau Trans'U a une application
complète : construction (Builder), chargement (CSV + JDBC), interrogation
(CLI), et visualisation.

## Ce que démontre ce module

### Le motif Builder — quand et pourquoi

`Network` est un agrégat à trois composantes liées par des références croisées
(une station cite un numéro de zone, une ligne cite des codes d'arrêt). Un
constructeur à vingt paramètres — ou trois listes pré-remplies par l'appelant —
repose la cohérence sur CHAQUE site de construction. Le Builder la recentre :

```java
Network r = Network.builder()
        .zone(1, "Centre-ville")
        .station("GAR", "Gare Centrale", 1, true)
        .ligne("Ligne A", "GAR")
        .construire();
```

Le constructeur de `Network` est **privé** : `construire()` est la seule voie
d'existence d'un réseau, et c'est là — un seul endroit — que les références
sont résolues et les doublons refusés.

### La rétrospective SOLID

Le main la phase 5 la raconte ; `SolidRetrospectiveTest` la vérifie par
réflexion (constructeur privé, frontière du domaine étanche à `java.sql`,
listes réellement immuables) :

| Principe | Preuve dans le fil rouge |
|---|---|
| S — SRP | M20 (Station ≠ Zone ≠ Line) ; M95 (Network ignore CSV/SQL, la CLI n'imprime pas) |
| O — OCP | M30 (VehicleFactory) ; M95 (le Builder s'étend, Network reste fermé) |
| L — LSP | M30 (Bus/Métro/Tramway tiennent le contrat Vehicle) ; M80 (Propulsion sealed) |
| I — ISP | M90 (StationDao / LineDao séparés, pas de BigDao) ; M95 (construire ≠ interroger) |
| D — DIP | M90 (le domaine ne connaît pas JDBC) ; M95 (CSV et JDBC : deux stratégies, un contrat) |

### La CLI testable

`TransitCli` ne contient ni `Scanner`, ni `System.out`, ni `System.exit` :
chaque commande est une fonction pure `(réseau, args) → String`. Le main
imprime, les tests assertent, la CI ne bloque jamais.

### La persistance en couches

Deux stratégies indépendantes — `NetworkLoader` (CSV, concept M60) et
`NetworkJdbcStore` (H2, concept M90) — qui ne se connaissent pas, ne partagent
qu'une chose : les méthodes publiques de `Network` et le `NetworkBuilder`.
Changer de format ne touche ni le domaine, ni la CLI. C'est le DIP, démontré.

## Ordre de lecture conseillé

1. `domain/Zone.java`, `Station.java`, `Line.java` — les records (M80)
2. `domain/Network.java` — l'agrégat immuable, constructeur privé
3. `domain/NetworkBuilder.java` — le motif, le cœur du module
4. `persistence/NetworkJdbcStore.java` — l'aller-retour SQL (rappeler M90)
5. `persistence/NetworkLoader.java` — l'aller-retour CSV (rappeler M60)
6. `cli/TransitCli.java` — la présentation sans effets de bord
7. `app/TransitApp.java` — les cinq phases qui assemblent tout

## Construire

```bash
./mvnw -pl 95-transit-app -am clean verify
```

## Exécuter la démo

```bash
java -cp 95-transit-app/target/classes:$(ls ~/.m2/repository/com/h2database/h2/2.3.232/h2-2.3.232.jar) \
     fr.ebruno.dev.oocol.transit.app.TransitApp
```

Le programme traverse les cinq phases sans interactivité et se termine sur :

**FIN DU FIL ROUGE — Bravo !**
