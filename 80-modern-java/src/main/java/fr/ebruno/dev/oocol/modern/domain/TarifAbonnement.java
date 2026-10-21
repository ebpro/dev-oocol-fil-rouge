package fr.ebruno.dev.oocol.modern.domain;

import java.time.LocalDate;

/**
 * Abonnement nominatif à durée — variante de {@link Tarif} sans prix nominal.
 *
 * <p>Le cas qui force la réflexion dans le switch de tarification : un
 * abonnement n'a pas de {@code prix()} dans ses composants (c'est un contrat
 * mensuel externalisé, pas une donnée du titre). Le switch exhaustif
 * <em>oblige</em> à décider que faire de ce cas — impossible de l'oublier,
 * ce que le {@code default} du module 30 cachait. {@link AnalyseReseau} le
 * valorise au tarif mensuel de référence.
 *
 * @param titulaire nom du porteur de l'abonnement
 * @param debut     premier jour de validité (inclus)
 * @param fin       dernier jour de validité (inclus)
 * @author Emmanuel Bruno
 */
public record TarifAbonnement(String titulaire, LocalDate debut, LocalDate fin) implements Tarif {
}
