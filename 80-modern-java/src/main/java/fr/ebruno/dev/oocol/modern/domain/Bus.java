package fr.ebruno.dev.oocol.modern.domain;

/**
 * Un bus du réseau — le plus simple des trois véhicules scellés.
 *
 * <p>Pas de {@code Propulsion} ici : un bus se rattache à une
 * {@code ligne} (String, pour la démo). C'est aussi un cas d'école de scellage :
 * les permissionnaires d'une hiérarchie {@code sealed} n'ont aucune obligation
 * de se ressembler — {@link MetroTrain} et {@link Tramway} portent une
 * propulsion, {@code Bus} non. Ce que le switch exhaustif garantit, c'est la
 * <em>fermeture des cas</em>, pas l'uniformité des cas.
 *
 * @author Emmanuel Bruno
 * @see Vehicle
 */
public record Bus(String identifiant, int capacite, String ligne) implements Vehicle {

    /**
     * Libellé spécifique : « Bus B-12 → Ligne A ».
     *
     * @return la description du bus
     */
    @Override
    public String decrire() {
        return "Bus %s \u2192 Ligne %s".formatted(identifiant, ligne);
    }
}
