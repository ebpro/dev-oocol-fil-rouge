package fr.ebruno.dev.oocol.modern.domain;

/**
 * Propulsion diesel — un record d'une seule valeur dans l'union {@link Propulsion}.
 *
 * @param cylindree cylindrée en centimètres cubes
 * @author Emmanuel Bruno
 */
public record Diesel(int cylindree) implements Propulsion {
}
