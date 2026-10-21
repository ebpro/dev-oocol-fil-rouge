package fr.ebruno.dev.oocol.modern.domain;

/**
 * Ticket unité pour une zone — variante simple de {@link Tarif}.
 *
 * @param zone la zone couverte
 * @param prix le prix à l'unité, en euros
 * @author Emmanuel Bruno
 */
public record TarifUnite(Zone zone, double prix) implements Tarif {
}
