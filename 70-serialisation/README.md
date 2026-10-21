# Module 70 — Sérialisation Java

Le réseau **Trans'U** doit survire entre les sessions : l'instantané du graphe (lignes, arrêts, zones, horodatage) s'écrit dans un fichier binaire et se relit tel quel. Après les flux de *texte* de la semaine 7 (`BufferedReader`, CSV, charset), voici le flux d'**objets** : on ne pose plus des caractères, on fige un graphe — et c'est la JVM qui écrit le format, à partir de règles qu'il faut connaître pour ne pas se faire surprendre par le sien. Le module exerce le contrat `Serializable` dans les deux sens : tout ce qu'il donne gratuitement (une écriture, tout le graphe, références partagées incluses) et tout ce qu'il facture en silence (l'empreinte `serialVersionUID`, les exclusions `transient`, les invariants qu'aucun constructeur ne protège plus).

## Concepts exercés (ELO 09)

- **`Serializable`, le marqueur** — interface SANS méthode : elle signe le contrat « cette classe sait se figer ». Le graphe entier traverse (`NetworkSnapshot.sauvegarder`), les références partagées RESTENT partagées (le flux écrit chaque objet une fois), et un seul champ non marqué fait tout capoter (`NotSerializableException`, qui nomme le coupable) ;
- **`serialVersionUID`, l'empreinte de compatibilité** — déclarée, c'est un contrat que l'on respecte ; calculée, c'est une surprise. L'ajouter ou la changer = décréter périmés tous les fichiers écrits avant (`InvalidClassException`, message : « stream classdesc serialVersionUID = 1, local class serialVersionUID = 2 »). L'évolution propre : UID stable + champ de plus (l'ancien flux se relit, le nouveau champ prend son défaut) ;
- **`transient`, l'exclusion** — le cache de `Station` ne voyage pas et se recalcule à la demande ; l'abonnement de `Passenger` non plus, et là c'est un `null` qui attend le NPE six mois plus tard. Ne voyagent que les données irremplaçables ;
- **`writeObject` / `readObject`, les hooks privés** — la JVM les appelle par réflexion : `defaultWriteObject` / `defaultReadObject` pour le reste, plus la **revalidation** à la lecture — le constructeur n'est JAMAIS appelé par la désérialisation, un flux n'est pas une source de confiance (`InvalidObjectException` sur un fichier édité à la main) ;
- **`Externalizable`, le contrôle total** — `Ticket` écrit ses trois valeurs à la main : compact (91 octets contre ~300), mais constructeur public sans argument OBLIGATOIRE, format à documenter soi-même, et le UID reste vérifié — `Externalizable` n'immunise contre rien ;
- **`readResolve`, la réparation** — le piège n° 1 : le singleton sérialisé revient en COPIE (allocation sans constructeur). `PassengerCorrige` renvoie l'instance partagée, et la copie n'existe plus ;
- **Les cinq exceptions signatures** — `NotSerializableException` (le graphe menteur), `InvalidClassException` (l'empreinte rompue, ou le constructeur manquant), `InvalidObjectException` (l'invariant violé à la lecture), plus les deux invisibles : le `static` qui ne voyage jamais, le `transient` qui devient `null`.

## Serializable ou Externalizable ?

| | `Serializable` | `Externalizable` |
|---|---|---|
| Effort d'écriture | Quasi nul (marqueur + UID) | Total (écrire ET lire, dans l'ordre) |
| Contrôle du format | La JVM décide | On décide tout |
| Taille du binaire | Descripteurs + graphe complet | Ce qu'on veut (`Ticket` : trois valeurs) |
| Fragilité aux évolutions | UID + nom de champ = contrat | UID vérifié QUAND MÊME, plus le constructeur vide |
| Quand l'utiliser | Le défaut raisonnable | Protocole compact et stable, documenté |

## Comment lire le code

Dans cet ordre — de la feuille vers la scène :

1. **`Zone`** — le cas le plus simple : deux champs, tous deux irremplaçables, rien à exclure ;
2. **`Station`** — le module en miniature : `transient` (cache), les deux hooks, la revalidation, les compteurs `static` ;
3. **`Line`** — la composition : une `List<Station>` traverse telle quelle, l'ordre compris ;
4. **`NetworkSnapshot`** — le contrat complet : `sauvegarder` / `charger`, le fichier binaire comme état du réseau ;
5. **`NetworkSnapshotV2`** — l'erreur à ne pas commettre : champ de plus ET UID changé ; la `resolveClass` qui ne sauve rien ;
6. **`Ticket`** — `Externalizable` : `writeExternal` / `readExternal`, l'aplatissement assumé, la revalidation ;
7. **`Passenger` / `PassengerCorrige`** — le piège n° 1 et son pansement, joués en miroir ;
8. **`VersionnementDemo`** (`app`) — les six scènes exécutées dans un répertoire temporaire détruit en `finally`. Exécutez-le, modifiez-le, cassez-le.

## Lancer la démonstration

    ./mvnw -pl 70-serialisation -am clean verify   # les 27 tests + la démo compilée

## Pour la suite

- **W10 — le Java moderne** : les `records` redéfinissent la sérialisation — un format géré par le compilateur autour du *canonical constructor*, où le constructeur est enfin la seule porte d'entrée, en lecture comme en écriture ;
- **W12 — JDBC** : l'instantané binaire laisse place à une vraie base. Le problème de version de format ne disparaît pas — il change de nom (schéma de base) et d'outils (migrations).
