/**
 * La démonstration exécutable du module 80 : sept scènes qui rejouent le
 * réseau Trans'U à Java 21 — chaque scène montre l'ancien réflexe et son
 * équivalent moderne côte à côte.
 *
 * <p><strong>Ce que couvre ce package.</strong> {@code ModernJavaDemo} est
 * la démonstration exécutable : les records et leur plomberie gratuite (scène 1), la
 * hiérarchie scellée et le switch exhaustif sur {@code Vehicle} (scène 2), le
 * pattern matching sur {@code Tarif} (scène 3), les pipelines Streams contre
 * {@link fr.ebruno.dev.oocol.modern.domain.VersionLegacy} (scène 4),
 * {@code Optional} en chaîne plutôt que {@code null} + {@code if} (scène 5),
 * les blocs de texte {@code """} contre le {@code StringBuilder} (scène 6),
 * et {@code Files.lines} en stream — déverrouillé ici après l'interdit du
 * module 60 (scène 7). Tout écrit dans un fichier temporaire détruit avant la
 * fin du {@code main} : la démo tourne aussi bien en local que dans la CI.
 *
 * <p><strong>Lecture croisée.</strong> Chaque scène correspond à une classe
 * du domaine et à un test qui en fait une preuve
 * ({@code RecordTest}, {@code SealedPatternTest}, {@code StreamsTest},
 * {@code OptionalTest}).
 *
 * @author Emmanuel Bruno
 * @see fr.ebruno.dev.oocol.modern.domain
 */
package fr.ebruno.dev.oocol.modern.app;
