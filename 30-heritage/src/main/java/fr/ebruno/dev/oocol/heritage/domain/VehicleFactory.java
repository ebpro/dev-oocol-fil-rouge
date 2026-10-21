package fr.ebruno.dev.oocol.heritage.domain;

import java.util.Locale;

/**
 * Fabrique de véhicules : un point unique qui sait <em>quel</em> sous-type
 * construire, et qui rend le résultat sous le type large {@link Vehicle}.
 *
 * <p><strong>Pourquoi une fabrique (patron Factory) ?</strong> Sans elle,
 * chaque coin du code écrit {@code new MetroTrain(...)}, {@code new Bus(...)}
 * — et surtout chaque coin doit <em>connaître</em> la liste des sous-classes.
 * Avec elle : l'appelant fournit un type logique ({@code "metro"}) et reçoit
 * un {@code Vehicle} ; la connaissance des sous-classes concrètes est
 * <em>enfermée</em> dans une seule méthode. Le jour où le réseau achète un
 * trolleybus, on ne modifie qu'ICI — l'appelant, lui, n'a jamais écrit le nom
 * de la classe. C'est le couplage par le type large ( {@code Vehicle} ) plutôt
 * que par l'espèce concrète, rendu possible par le polymorphisme d'héritage.
 *
 * <p><strong>La fabrique valide aussi :</strong> un type inconnu n'accouche
 * pas d'un {@code null} silencieux — {@link IllegalArgumentException} immédiat,
 * message explicite. Une fabrique qui ne rejette rien n'est qu'un
 * {@code new} déguisé.
 *
 * <p>Classe utilitaire : constructeur privé, seule méthode statique — même
 * discipline que {@code Ticket} au module 20 (fabriques statiques).
 *
 * @author Emmanuel Bruno
 * @see Vehicle
 */
public final class VehicleFactory {

    /**
     * Classe utilitaire (point d'entrée {@link #creer} seul) : instantiation
     * interdite — la fabrique n'a pas d'état, seulement du savoir-faire.
     */
    private VehicleFactory() {
        throw new AssertionError("Classe utilitaire : ne s'instancie pas.");
    }

    /**
     * Construit le véhicule concret correspondant au type logique demandé.
     *
     * <p>Le type accepté est insensible à la casse ({@code "metro"},
     * {@code "METRO"}, {@code "Metro"}…). Chaque branche applique des valeurs
     * par défaut propres à l'espèce (un métro standard de 3 voitures, un bus
     * diesel 2 portes, un tram de 33 m chargé à 80 %) : c'est précisément
     * l'objet de la fabrique que de <em>connaître</em> ces standards, pour que
     * l'appelant n'ait jamais à les connaître.
     *
     * @param type     le type logique : {@code "metro"}, {@code "bus"} ou {@code "tram"}
     * @param immat    immatriculation du véhicule, non vide
     * @param capacite nombre de places, strictement positif
     * @param vitesse  vitesse maximale en km/h, strictement positive
     * @return un véhicule concret ({@link MetroTrain}, {@link Bus} ou
     *         {@link Tramway}) rendu sous le type large {@link Vehicle}
     * @throws IllegalArgumentException si le type est inconnu/absent, ou si un
     *                                  argument viole les gardes de {@link Vehicle}
     */
    public static Vehicle creer(String type, String immat, int capacite, double vitesse) {
        if (type == null) {
            throw new IllegalArgumentException("La fabrique exige un type de véhicule.");
        }
        // Normalisation Locale.ROOT : « METRO » (turc, allemand, français…) tombe
        // toujours sur la même clé — pas de mauvaise surprise selon la langue de la JVM.
        switch (type.toLowerCase(Locale.ROOT)) {
            case "metro":
                return new MetroTrain(immat, capacite, vitesse, 3, false);
            case "bus":
                return new Bus(immat, capacite, vitesse, false, 2);
            case "tram":
                return new Tramway(immat, capacite, vitesse, 33.0, 0.8);
            default:
                // Message pédagogique : nommer l'inconnu ET la liste des connus.
                throw new IllegalArgumentException(
                        "Type de véhicule inconnu : " + type + " (attendu : metro, bus ou tram)");
        }
    }
}
