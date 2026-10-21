# Module 60 — NIO.2 et gestion des ressources

Le réseau **Trans'U** quitte la mémoire : les zones, les arrêts et les lignes
vivent désormais dans des **fichiers CSV** que l'on charge, et l'exploitation
écrit en retour des **fichiers d'horaire** que d'autres programmes reliront.
La semaine 6 avait montré la fermeture sur un `StringReader` (une ressource
de pure convention) ; la semaine 7 imposait le try-with-resources comme
réflexe. Cette semaine, la ressource est **un fichier vrai** — et tout ce qui
pouvait rester abstrait devient concret : un charset mal choisi ment sans
crier, un reader non fermé casse sous Windows et survit sous Linux, et un
horaire écrit à la volée peut être lu à moitié. Le module exerce l'aller
mémoire ↔ disque : chargement CSV en trois phases, écriture atomique, chemins
composés, arborescences arpentées.

## Concepts exercés (ELO 08)

- **`Path` et `Files`, l'algèbre des chemins** — `Path.of` / `resolve` / `normalize` / `relativize` composent et nettoient des chemins SANS toucher le disque ; `Files.exists` / `readAllBytes` / `copy` sont les gestes côté système. Une chaîne de chemin ne se compare pas fiablement : seul `Path` connaît le séparateur du système où il vit ;
- **IO bufferisée** — `BufferedReader` / `BufferedWriter` sur `Files.newBufferedReader` / `newBufferedWriter` : la ligne à la ligne en lecture, le flux vidé en écriture — et vidé AVANT le moindre move, sinon l'atome déplace des octets périmés ;
- **try-with-resources, la frontière qui ferme pour de vrai** — chaque reader, writer et `DirectoryStream` du module est fermé par la structure, même quand le corps lève au milieu du fichier. L'anti-pattern `chargerSansFermer` joue l'inverse : « réussi » sous POSIX (fd qui fuit en invisibilité), `IOException` sous Windows — la grâce de l'OS n'est pas une garantie Java ;
- **Le charset est un paramètre, jamais un défaut** — le même mot « Hôpital » fait 8 octets en UTF-8 et 7 en Latin-1 ; relu dans le mauvais encoding, il MENT sans lever (`Ã´`), ou lève `MalformedInputException` — enrobée en `NetworkException`, la cause survit ;
- **Écriture atomique en deux gestes** — `ScheduleWriter` écrit dans un `.tmp` du MÊME dossier puis `Files.move(ATOMIC_MOVE, REPLACE_EXISTING)` : le lecteur voit l'ancien ou le nouveau, jamais l'état du milieu. Une panne entre les deux laisse l'ancien fichier intact ;
- **DirectoryStream et FileVisitor, la visite impérative** — `trouverFichiers` filtre par motif `*.csv` à la source (le système de fichiers travaille, pas une boucle) ; `tailleTotale` descend l'arborescence avec un `SimpleFileVisitor` ANONYME et un compteur `long[]`. Pas de `Files.walk()` : ce serait emprunter aux streams de la semaine 8 ;
- **Chargement en trois phases, dans l'ordre des dépendances** — zones (index numéro → `Zone`), arrêts (lien vers la VRAIE zone, index code → `Station`), lignes (codes résolus contre l'index). Permuter l'ordre fait lever « référence inconnue » sur un fichier pourtant valide.

## Comment lire le code

Dans cet ordre — de la feuille vers la scène :

1. **`Zone` / `Station`** — `parse(String)` : la ligne brute devient objet, et le message d'erreur CITE la ligne fautive. `Station.parse(csv, zone)` : la résolution n'est pas un numéro isolé, c'est le lien vers le vrai objet ;
2. **`LineData`** — la construction en deux temps : `parse` garde les codes, `resoudre` les échange contre les `Station` chargées. Une ligne ne peut pas exister à moitié ;
3. **`NetworkLoader`** — le cœur : les trois phases, le charset imposé, le try-with-resources de `lireLignes`. Puis `chargerSansFermer` — l'anti-pattern volontaire, à commenter en séance ;
4. **`ScheduleWriter`** — l'écriture honnête : validations d'abord (IAE = bug d'appelant, jamais un fichier à moitié écrit), `.tmp` ensuite, move atomique enfin ;
5. **`FileSystemWalker`** — les trois gestes NIO.2 du corpus : chercher (DirectoryStream), mesurer (walkFileTree + visiteur), composer (`resoudreChemine`) ;
6. **`IoDemo`** (`app`) — les six scènes exécutées dans un répertoire temporaire détruit en `finally` : charger, écrire, composer, encoder, arpenter, rattraper. Exécutez-le, modifiez-le, cassez-le.

## Lancer la démonstration

    ./mvnw -pl 60-io -am test          # les 36 tests (chargement, écriture, chemins, charset)
    ./mvnw -pl 60-io -am compile
    java -cp 60-io/target/classes fr.ebruno.dev.oocol.nio.app.IoDemo

## Pour la suite

- **W8 — la sérialisation binaire** : un autre type de flux, qui n'est plus du texte. Le `serialVersionUID` déjà posé sur `NetworkException` (W7) deviendra une décision, pas un réflexe — et l'on verra pourquoi UTF-8 n'a rien à y dire ;
- **W10 — `java.util.stream` et `Files.walk`** : la visite impérative de ce module (SimpleFileVisitor, `long[]`) est volontairement privée de streams. Ce sera la semaine 8/10 : quand `Files.walk` rendra l'arborescence en `Stream<Path>`, on saura exactement ce que l'on gagne — et ce qu'il faudra fermer.
