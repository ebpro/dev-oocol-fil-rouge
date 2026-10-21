/**
 * Couche de présentation du module 95 — une CLI faite de fonctions pures.
 *
 * <p>{@link fr.ebruno.dev.oocol.transit.cli.TransitCli} ne contient ni
 * {@code Scanner}, ni {@code System.out}, ni {@code System.exit} : chaque
 * commande est une méthode statique {@code (réseau, args) → String}. Le
 * main imprime, les tests assertent, la CI ne bloque jamais — trois clients,
 * une seule implémentation.
 */
package fr.ebruno.dev.oocol.transit.cli;
