/**
 * Le domaine « Trans'U » relu par le prisme des fichiers (ELO 08).
 *
 * <p><strong>Ce que ce package enseigne.</strong> Le réseau cesse d'être
 * un littéral du code : il vit dans trois fichiers CSV que quelqu'un a
 * écrits, exportés, édités — donc qu'on ne maîtrise plus. Trois gestes
 * structurent la semaine :
 * <ul>
 *   <li><strong>lire</strong> : {@link fr.ebruno.dev.oocol.nio.domain.NetworkLoader}
 *     ouvre chaque fichier en UTF-8 explicite via
 *     {@code Files.newBufferedReader}, ignore commentaires et lignes vides,
 *     résout les références entre fichiers (numéro de zone lu dans
 *     {@code stations.csv} → objet {@link fr.ebruno.dev.oocol.nio.domain.Zone}
 *     lu dans {@code zones.csv} ; code d'arrêt lu dans {@code lignes.csv} →
 *     {@link fr.ebruno.dev.oocol.nio.domain.Station}), et transforme toute
 *     anomalie de données en {@link fr.ebruno.dev.oocol.nio.domain.NetworkException}
 *     qui cite le fichier ET la ligne ;</li>
 *   <li><strong>écrire</strong> : {@link fr.ebruno.dev.oocol.nio.domain.ScheduleWriter}
 *     produit un fichier d'horaires en deux temps — temporaire puis
 *     {@code Files.move(ATOMIC_MOVE)} — pour qu'un lecteur ne voie jamais
 *     une écriture à moitié finie ;</li>
 *   <li><strong>explorer</strong> : {@link fr.ebruno.dev.oocol.nio.domain.FileSystemWalker}
 *     compose, filtre et mesure des chemins avec {@code DirectoryStream} et
 *     {@code walkFileTree} — les outils pré-Streams, {@code Files.walk()}
 *     étant réservé au module 80.</li>
 * </ul>
 *
 * <p><strong>La tension de la semaine, en deux méthodes.</strong>
 * {@link fr.ebruno.dev.oocol.nio.domain.NetworkLoader#chargerSansFermer(java.nio.file.Path)} :
 * la ressource non fermée qui « marche quand même » sous POSIX et corrompt
 * ailleurs, le pendant exact du FileWriter laissé ouvert. Et
 * {@link fr.ebruno.dev.oocol.nio.domain.NetworkLoader#chargerAvecEncodage(java.nio.file.Path, java.nio.charset.Charset)}
 * montre qu'un fichier n'est PAS un texte : il est un texte DANS un encoding,
 * et le mauvais choix transforme « Aéroport » en « AÃ©roport » sous vos yeux.
 *
 * <p><strong>Ordre de lecture conseillé.</strong> {@code NetworkException}
 * (la racine checked, instantanée W7) → {@code Zone} et sa fabrique
 * {@code parse} (le geste fondamental : CSV → objet, avec diagnostic) →
 * {@code Station} (parse en deux temps, intégrité référentielle) →
 * {@code LineData} (la référence différée, résolue contre un index) →
 * {@code NetworkLoadResult} (le DTO de lecture, vues non modifiables) →
 * {@code NetworkLoader} (l'orchestrateur : try-with-resources partout,
 * ordre des dépendances) → {@code ScheduleWriter} (le geste inverse,
 * atomique) → {@code FileSystemWalker} (Path en outil quotidien).
 *
 * @author Emmanuel Bruno
 */
package fr.ebruno.dev.oocol.nio.domain;
