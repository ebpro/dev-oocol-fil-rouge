package fr.ebruno.dev.oocol.modern.domain;

/**
 * Les titres de transport — support de la démonstration de pattern matching.
 *
 * <p><strong>Pourquoi un switch et pas du polymorphisme.</strong> Le module 30
 * répondait à « quel prix ? » par une méthode {@code getPrix()} redéfinie
 * chez chaque sous-type : correct, mais la logique est dispersée chez les
 * données, et chaque nouvel appelant doit connaître l'API de chaque tarif.
 * Avec une hiérarchie scellée, une <em>expression</em> switch à filtrage de
 * motifs regroupe toute la règle de décision en un seul endroit, lisible
 * comme un tableau, vérifiée exhaustive par le compilateur — et les
 * composants de chaque variante sont déstructurés ({@code case TarifUnite t})
 * sans un seul {@code instanceof} ni cast.
 *
 * <p>C'est le « choix de design » de la semaine : le polymorphisme pour un
 * comportement qui <em>appartient</em> au type, le switch scellé pour une
 * opération qui <em>traverse</em> la hiérarchie (ici : la tarification est une
 * politique du réseau, pas une propriété intrinsèque du ticket).
 *
 * @author Emmanuel Bruno
 * @see TarifUnite
 * @see TarifJour
 * @see TarifAbonnement
 * @see AnalyseReseau#prixDe(Tarif)
 */
public sealed interface Tarif permits TarifUnite, TarifJour, TarifAbonnement {
}
