package fr.ebruno.dev.oocol.modern.domain;

/**
 * Propulsion hydrogène — un record d'une seule valeur dans l'union {@link Propulsion}.
 *
 * @param pressionBar pression de la réserve d'H2 en bars
 * @author Emmanuel Bruno
 */
public record Hydrogene(int pressionBar) implements Propulsion {
}
