/**
 * Schéma relationnel du module 90 (ELO 12).
 *
 * <p>{@link fr.ebruno.dev.oocol.jdbc.schema.DatabaseSchema} : le DDL des
 * quatre tables ({@code zones}, {@code stations}, {@code lignes},
 * {@code ligne_arrets}), les zones initiales et le vidage d'isolation.
 *
 * <p>Séparer le schéma des DAO, c'est déjà faire du Spring : le conteneur de
 * persistance (ici, la démo ou le test) décide <em>quand</em> la base est
 * créée ; les DAO supposent qu'elle l'est.
 */
package fr.ebruno.dev.oocol.jdbc.schema;
