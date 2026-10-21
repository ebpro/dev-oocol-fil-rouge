# Module 50 — Exceptions checked/unchecked

Le réseau **Trans'U** cesse de mentir par `null` (semaine 5, séance CNAM S9).
Une station que la ligne ne dessert plus ne « revient » plus : elle lève. Un
solde trop court ne se déduit plus en silence : il s'annonce et s'annote. Et
le barème qui rembourse les usagers (tarif négatif) meurt bruyamment à la
CI. Le module introduit la **hiérarchie d'exceptions métier** de l'application —
`NetworkException` racine checked, ses deux filles récupérables, et les deux
unchecked qui nomment enfin les bugs — plus les gestes d'appel qui vont avec :
propager, récupérer, enrober, ordonner, et fermer sans `finally`.

## Concepts exercés (ELO 07)

- **Checked vs unchecked, la VRAIE frontière** — une erreur récupérable (il existe un plan B : recharger, re-planifier) est **checked** et le compilateur force sa gestion ; un paramètre aberrant est un **bug** de l'appelant, donc **unchecked**, et se propage jusqu'au point d'entrée. Le module joue les deux registres côte à côte, sur la MÊME méthode parfois (`Compte.debiter` : montant `< 0` → IAE, solde court → checked) ;
- **Hiérarchie métier** — `NetworkException extends Exception`, puis `ScheduleConflictException` (ligne + heure) et `InsufficientFundsException` (solde + prix + `getManquant()`) ; en face, `StationNotFoundException` (code) et `FareCalculationException` dérivent directement de `RuntimeException` et NE SONT PAS de la famille `NetworkException` — un `catch (NetworkException)` n'avale aucune unchecked, par conception ;
- **La migration guidée « null → throws »** — `Line.trouverParCode` ne retourne plus `null` (le contrat W5 rougit volontairement) : elle lève une `StationNotFoundException` typée, immédiate, localisée au point de la demande. `Station.requireValide` généralise le geste en fabrique de garde ;
- **Le contrepoint assumé** — `retirerArret` GARDE son `boolean` : « rien à retirer » est une réponse métier valide, pas une anomalie. Transformer tout `null`/`false` en exception, c'est l'anti-pattern *exceptions pour le contrôle de flux* que la séance combat ;
- **try-with-resources** — la scène 3 ferme un `BufferedReader` sur `StringReader` (zéro fichier, zéro réseau : la démo court partout, CI comprise) ; le test 4 prouve la garantie structurelle : `close()` appelé même quand le corps lève ;
- **Suppressed** — quand le corps ET `close()` lèvent, la primaire reste celle du corps, la fermeture part dans `getSuppressed()`, jamais perdue (`ResourceDemoTest`) ;
- **Chaînage de causes** — `NetworkException(String, Throwable)` et `ScheduleConflictException(..., cause)` : la `NumberFormatException` lue dans une config est ENROBÉE, pas jetée — le « Caused by: » survit au travers de l'abstraction (scène 5) ;
- **Ordre des catch** — spécifique avant racine, sinon le compilateur refuse (`unreachable catch`) ; la scène 4 montre aussi qu'une même exception se fait border par sa clause propre OU par la racine `NetworkException` — l'héritage en acte ;
- **Multi-catch** — `catch (StationNotFoundException | FareCalculationException e)` borde les deux unchecked sœurs sans inventer de classe commune ni rien avaler de plus (scène 6) ;
- **`serialVersionUID`** — posé sur toute la famille (dont `Throwable` hérite la sérialisation) : un test le vérifie présent et à 1L, avant que le module `70-serialisation` n'explique pourquoi ce n'est pas du décor.

## Le tableau de décision (à justifier en séance)

| Situation | Choix | Classe | Pourquoi |
|---|---|---|---|
| Station absente du parcours | unchecked | `StationNotFoundException` | l'appelant a demandé ce que le réseau ne dessert pas : SON code est faux |
| Tarif négatif, barème troué | unchecked | `FareCalculationException` | incohérence logique de construction : aucun plan B en aval |
| Paramètre `null`/négatif/doublon | unchecked | `IllegalArgumentException` | contrat de validité violé = bug (promis depuis W2) |
| Ligne vide, état demandé absent | unchecked | `IllegalStateException` | l'objet existe mais pas dans l'état requis (W5) |
| Solde insuffisant | **checked** | `InsufficientFundsException` | plan B : recharger `getManquant()` puis réessayer |
| Cadence irréaliste | **checked** | `ScheduleConflictException` | plan B : re-planifier les départs |
| « rien à retirer » | valeur de retour | `boolean` | réponse métier normale : l'exception serait du contrôle de flux |

## Comment lire le code

Dans cet ordre — de la racine vers les scènes :

1. **`NetworkException`** — la racine checked. Pourquoi dériver d'`Exception` (et pas de `RuntimeException`) : le `throws` FORCÉ dans les signatures. Les deux constructeurs : avec et sans cause.
2. **`ScheduleConflictException` / `InsufficientFundsException`** — les filles checked, chacune porte la donnée du plan B (`ligne`/`heure`, `solde`/`prix`/`manquant`) : une exception typée est un canal de données.
3. **`StationNotFoundException` / `FareCalculationException`** — les sœurs unchecked. Lisez leurs Javadoc : c'est LÀ que la séance verbalise pourquoi elles n'héritent PAS de `NetworkException`.
4. **`Station`** — la fabrique de garde `requireValide` : le pattern qui remplace `return null` partout dans le projet.
5. **`Line`** — le cœur de la migration : `trouverParCode` (null → levée), `ajouterArret` (false → IAE), `valider()` (la checked par contraste), `retirerArret` (le contrepoint qui ne bouge PAS).
6. **`Compte`** — le checked par excellence, avec son scénario complet catch → recharger → réessayer.
7. **`FareCalculator`** — les deux unchecked qui se croisent dans `calculerPrixEntre` : `requireValide` en amont, barème en aval — le multi-catch de la scène 6 naît ici.
8. **`ExceptionDemo`** (`app`) — les six scènes exécutées : propager, récupérer, fermer, ordonner, enrober, ramasser. Exécutez-le, modifiez-le, cassez-le.

## Lancer la démonstration

```bash
./mvnw -pl 50-exceptions -am package
java -cp 50-exceptions/target/classes fr.ebruno.dev.oocol.exceptions.app.ExceptionDemo
```

## Construire et tester le module

```bash
./mvnw -pl 50-exceptions test      # uniquement les tests du module
./mvnw -pl 50-exceptions verify    # tests + jacoco + analyse statique warn-only
./mvnw -B clean verify             # tout le projet (comme la CI)
```

## Schéma

`docs/diagrams/50-exceptions.puml` — l'arbre des deux familles (traits
pleins = checked, pointillés = unchecked), les garde-fous du domaine
(`Compte ..> InsufficientFundsException : «throws»`) et les notes
« récupérable vs bug ».

## Ce qui vient après

- **Module `60-io`** : le try-with-resources passe de la démonstration à l'usage — lecteurs et flux réels, dont les `IOException` checked justifient enfin le mot « checked » tout seul ;
- **Module `70-serialisation`** : le `serialVersionUID` posé sur toute la hiérarchie devient un acteur — une exception sérialisée/désérialisée, ça se regarde ;
- **Module `80-modern-java`** : `try-with-resources` sur des ressources maison, `Optional` comme alternative DÉCLARATIVE au « null → throws » pour les absences normales, et les méthodes-usine d'exception (`Objects.requireNonNullElseGet`) — la modernité de W10 se mesurera en diff, comme promis.
