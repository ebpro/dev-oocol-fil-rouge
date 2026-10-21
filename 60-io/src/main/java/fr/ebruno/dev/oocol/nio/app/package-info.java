/**
 * La démonstration exécutable du module 60 : six scènes qui jouent
 * l'aller-retour mémoire ↔ disque.
 *
 * <p><strong>Ce que ce package enseigne.</strong> {@code IoDemo} est le
 * script de séance : chaque scène est une méthode privée autonome, titrée,
 * commentable séparément en TD. Toute l'agitation se déroule dans un
 * répertoire temporaire créé par {@code Files.createTempDirectory} et
 * supprimé dans le bloc {@code finally} du {@code main} — la démo ne
 * touche jamais le dépôt, jamais le home de l'étudiant, et tourne donc
 * aussi bien en local que dans la CI.
 *
 * <p><strong>Ordre de lecture.</strong> Scène 1 (écrire puis relire des
 * fixtures : le aller simple/retour du {@code NetworkLoader}), scène 2
 * (l'horaire atomique du {@code ScheduleWriter}), scène 3 (l'algèbre des
 * {@code Path}), scène 4 (le charset comme contractant entre deux
 * programmes), scène 5 (la descente d'arborescence par {@code FileVisitor}),
 * scène 6 (l'erreur récupérable — le fichier absent — et l'anti-pattern du
 * reader non fermé).
 *
 * @author Emmanuel Bruno
 * @see fr.ebruno.dev.oocol.nio.domain
 */
package fr.ebruno.dev.oocol.nio.app;
