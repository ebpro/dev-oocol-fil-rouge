/**
 * Façade de persistance du module 90 (ELO 12).
 *
 * <p>{@link fr.ebruno.dev.oocol.jdbc.repo.NetworkRepository} assemble les
 * DAO JDBC derrière une connexion partagée et fournit le gabarit
 * transactionnel {@code transactionSurement}. C'est le « composition root »
 * du module — le seul endroit qui connaît à la fois les interfaces et leurs
 * implémentations.
 */
package fr.ebruno.dev.oocol.jdbc.repo;
