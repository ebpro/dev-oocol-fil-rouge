package fr.ebruno.dev.oocol.modern.domain;

/**
 * Une rame de métro — record dans une hiérarchie scellée.
 *
 * <p>Le module 30 faisait de chaque type de véhicule une classe : champs
 * {@code private final}, constructeur, getters, {@code equals},
 * {@code hashCode}, {@code toString}. Ici, la déclaration du record fournit
 * tout cela, et les composants {@code identifiant} et {@code capacite}
 * <em>sont</em> les implémentations des méthodes de {@link Vehicle} —
 * l'accessoire généré par le compilateur satisfait le contrat de l'interface,
 * sans une ligne de {@code @Override} de plomberie.
 *
 * <p>Le composant {@code propulsion} est typé {@link Propulsion} : une
 * deuxième hiérarchie scellée, imbriquée — le véhicule <em>a</em> une
 * propulsion, il n'en <em>est</em> pas une (composition, leçon du module 20).
 *
 * @author Emmanuel Bruno
 * @see Vehicle
 * @see Propulsion
 */
public record MetroTrain(String identifiant, int capacite, int nombreVoitures,
                         Propulsion propulsion) implements Vehicle {

    /**
     * Libellé spécifique : « Métro M-01 (4 voitures) ».
     *
     * @return la description de la rame
     */
    @Override
    public String decrire() {
        return "M\u00e9tro %s (%d voitures)".formatted(identifiant, nombreVoitures);
    }
}
