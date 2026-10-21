# Module 90 — JDBC et motif DAO (ELO 12)

Semaine 12 (UTLN) / Séances S15-S16 (CNAM). **Le snapshot sérialisé de W8 est mort — vive la base de données !** Le `NetworkSnapshot` du module 70 était fragile (format binaire opaque, versionné à la main) et mono-poste : un fichier, un utilisateur à la fois. Une vraie base relationnelle résout d'un coup la concurrence d'accès, les requêtes (« quels arrêts en zone 2 ? ») et l'intégrité (clés primaires, étrangères, unicité). Ce module fait le passage en **JDBC brut** — sans ORM — pour que le SQL n'ait jamais de secret.

## Concepts exercés (ELO 12)

- **DriverManager / Connection** — l'URL JDBC (`jdbc:h2:mem:transU;DB_CLOSE_DELAY=-1`), l'ouverture en try-with-resources, la question du cycle de vie : qui ouvre ferme ;
- **PreparedStatement** — les trois gestes (préparer avec `?`, lier les paramètres, exécuter), la séparation plan/valeur, le `addBatch`/`executeBatch` de `LineDaoJdbc` ;
- **ResultSet** — curseur ligne à ligne, mapping explicite colonne → champ (`mapResultSet`) ; le `getInt`/`getBoolean` qui traverse la frontière types Java ↔ SQL ;
- **motif DAO** — `CrudDao<T, ID>` générique, interfaces `StationDao`/`LineDao`, implémentations JDBC dans `dao.impl` : le domaine ignore le SQL, demain JPA remplace l'implémentation sans toucher le contrat ;
- **mapping objet-relationnel d'un agrégat** — une `Line` vit sur DEUX tables (`lignes` + `ligne_arrets`) ; l'ordre de parcours devient une COLONNE (`position`) ; la suppression se fait enfants d'abord ;
- **transactions ACID** — `setAutoCommit(false)` / `commit()` / `rollback()`, et le gabarit inratable `transactionSurement` : tout ou rien, prouvé au compteur près par un doublon volontaire ;
- **injection SQL** — démontrée dans les deux sens : le test prouve que la version concaténée EST exploitée (tautologie `' OR '1'='1`, commentaire `' --`) et que la version paramétrée NE L'EST PAS ;
- **enveloppement des exceptions vérifiées** — `SQLException` remontée en `RuntimeException` à cause intacte (écho de l'ELO 07), jamais déclarée dans les contrats DAO.

## Comment lire le code

Dans cet ordre — le contrat d'abord, le SQL ensuite :

1. **`Zone` → `Station` → `Line`** (`domain`) — les records, cette fois lus comme des lignes de tables ;
2. **`CrudDao`** (`dao`) — le générique : quoi, pas comment ;
3. **`StationDao` / `LineDao`** (`dao`) — les requêtes métier sous interface ;
4. **`StationDaoJdbc` / `LineDaoJdbc`** (`dao.impl`) — tout le SQL du module est là ;
5. **`DatabaseSchema`** (`schema`) — le DDL des quatre tables et pourquoi la PK de la table-jonction est composée ;
6. **`NetworkRepository`** (`repo`) — la façade : branchement des DAO, connexion partagée, gabarit transactionnel ;
7. **`SqlInjectionDemo`** (`security`) — les deux écritures d'une même requête, côte à côte ;
8. **`JdbcDemo`** (`app`) — les six scènes exécutées.

## Lancer la démonstration

    ./mvnw -pl 90-jdbc -am clean verify   # les 28 tests + la démo compilée

Pour jouer les six scènes à la main, lancer `JdbcDemo` depuis l'IDE (la base H2
est en mémoire : aucune installation, aucun fichier, rien à nettoyer).

## Pour aller plus loin (non couvert, volontairement)

- **Pool de connexions** (HikariCP) : `DriverManager.getConnection` à la demande est juste pour la pédagogie ; en production, ouvrir une connexion par requête est le premier goulot. Les notions de borné, validation et attente d'un pool méritent leur propre séance.
- **ORM (JPA/Hibernate)** : le module génère ce que JDBC fait à la main — dont le N+1 aperçu dans `LineDaoJdbc.trouverTous`. Le contrat DAO ne bougera pas : c'était le but du motif.
- **Bases concurrentes** : isolation (`READ COMMITTED`…), verrous, deadlocks — au-delà de l'atomicité jouée ici en mono-connexion.

## Pour la suite

- **W13 — transit-app (module final)** : Builder pour assembler le réseau, rétrospective SOLID sur les treize semaines, et une vraie CLI qui lit la base de ce module — le fil rouge devient un livrable.
