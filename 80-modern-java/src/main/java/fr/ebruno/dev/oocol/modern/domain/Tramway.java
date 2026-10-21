package fr.ebruno.dev.oocol.modern.domain;

/**
 * Un tramway — record scellé à deux dimensions (propulsion + longueur).
 *
 * <p>Démonstration de {@code String.formatted} (Java 12) dans la
 * {@link #decrire()} : l'équivalent orienté objet de {@code String.format},
 * sans la classe {@code String} en tête de ligne — la chaîne de format est
 * un message, le message est un récepteur.
 *
 * @author Emmanuel Bruno
 * @see Vehicle
 * @see Propulsion
 */
public record Tramway(String identifiant, int capacite, Propulsion propulsion,
                      double longueurMetres) implements Vehicle {

    /**
     * Libellé spécifique : « Tram T-03 (33.0 m) ».
     *
     * @return la description du tramway
     */
    @Override
    public String decrire() {
        return "Tram %s (%.1f m)".formatted(identifiant, longueurMetres);
    }
}
