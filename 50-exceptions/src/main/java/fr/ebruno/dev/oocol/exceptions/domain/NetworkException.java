package fr.ebruno.dev.oocol.exceptions.domain;

/**
 * Exception de base pour le domaine réseau. CHECKED : l'appelant DOIT gérer
 * (try/catch ou throws). Utilisée pour les erreurs RÉCUPÉRABLES (solde
 * insuffisant → recharger; station absente → en ajouter une).
 *
 * <p><strong>Pourquoi une racine MÉTIER et pas juste « Exception » ?</strong>
 * Un {@code catch (NetworkException e)} attrape toute la famille du domaine
 * (conflit d'horaire, solde insuffisant…) sans avaler le reste du monde :
 * un {@code NullPointerException} ou une {@code SQLException} remonte
 * intact, au lieu d'être étouffé par un {@code catch (Exception)} trop large.
 * Dériver de {@code Exception} (et non {@code RuntimeException}) est le
 * choix fort de la semaine : <em>checked</em> signifie que le compilateur
 * inscrit la gestion de l'erreur dans la signature de chaque méthode —
 * l'oublier est une erreur de compilation, pas une promesse dans un commentaire.
 *
 * <p>Le constructeur avec {@code cause} est là pour la scène du chaînage :
 * une exception bas niveau ({@code NumberFormatException} lue dans une
 * config) est ENROBÉE dans une exception métier qui préserve l'originale
 * derrière {@code getCause()} — la stack trace affichera « Caused by: » et
 * le diagnostic complet survivra au travers de l'abstraction.
 *
 * @author Emmanuel Bruno
 * @see ScheduleConflictException
 * @see InsufficientFundsException
 * @see StationNotFoundException
 */
public class NetworkException extends Exception {

    /**
     * Identifiant de sérialisation : requis par {@code Serializable} (dont
     * {@code Throwable} hérite). Le module {@code 70-serialisation} montrera
     * à quoi il sert vraiment quand une exception traverse un flux sérialisé.
     */
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
     * Décrit la situation ET préserve la cause d'origine (chaînage).
     *
     * @param message le message explicatif du point de vue métier
     * @param cause   l'exception de bas niveau enrobée, ou {@code null}
     */
    public NetworkException(String message, Throwable cause) {
        super(message, cause);
    }
}
