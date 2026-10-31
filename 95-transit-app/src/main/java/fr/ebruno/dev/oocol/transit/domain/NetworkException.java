package fr.ebruno.dev.oocol.transit.domain;

/**
 * Exception racine du domaine — héritière directe de celle du module 50.
 *
 * <p><strong>Checked, et ça change tout.</strong> Dériver de {@code Exception}
 * (et non {@code RuntimeException}) inscrit la gestion de l'erreur dans la
 * signature de chaque méthode : un {@code NetworkBuilder.construire()} qui
 * peut échouer DOIT être appelé sous {@code try/catch} ou {@code throws} —
 * l'oublier est une erreur de compilation, pas une promesse dans un
 * commentaire. Le module 50 avait fait ce choix pour les erreurs
 * récupérables ; le Builder s'y plie pour la même raison : un réseau
 * mal référencé se CORRIGE (on ajoute la zone oubliée), il ne plante pas
 * la JVM.
 *
 * <p>Le constructeur avec {@code cause} preserve le chaînage (ELO 07) : une
 * {@code NumberFormatException} lue dans un CSV survit intacte derrière le
 * {@code getCause()} de l'exception métier qui l'enrobe.
 *
 * @author Emmanuel Bruno
 * @see NetworkBuilder#construire()
 */
public class NetworkException extends Exception {

    /** Identifiant de sérialisation (module 70 : {@code Throwable} est {@code Serializable}). */
    private static final long serialVersionUID = 1L;

    /**
     * Décrit la situation anormale récupérable.
     *
     * @param message le message explicatif, pensé pour celui qui lira le log
     */
    public NetworkException(String message) {
        super(message);
    }

    /**
     * Décrit la situation ET préserve la cause d'origine (chaînage du module 50).
     *
     * @param message le message explicatif du point de vue métier
     * @param cause   l'exception de bas niveau enrobée, ou {@code null}
     */
    public NetworkException(String message, Throwable cause) {
        super(message, cause);
    }
}
