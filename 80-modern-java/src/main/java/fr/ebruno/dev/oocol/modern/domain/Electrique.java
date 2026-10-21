package fr.ebruno.dev.oocol.modern.domain;

/**
 * Propulsion électrique — un record d'une seule valeur dans l'union {@link Propulsion}.
 *
 * @param puissanceKW puissance installée en kilowatts
 * @author Emmanuel Bruno
 */
public record Electrique(int puissanceKW) implements Propulsion {
}
