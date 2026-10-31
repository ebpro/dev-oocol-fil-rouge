package fr.ebruno.dev.oocol.nio.domain;

/**
 * Exception de base du domaine réseau, version fichiers. CHECKED : l'appelant
 * DOIT gérer (try/catch ou throws).
 *
 * <p><strong>Même racine, nouveau terrain de jeu.</strong>
 * {@code NetworkException} est recopiée du module 50 (principe
 * d'autonomie des modules : chaque module est autonome, aucune dépendance
 * inter-modules). Ce qui change ici : sa raison d'être principale.
 * Avec les fichiers, l'erreur récupérable cesse d'être rare — un fichier
 * absent, une ligne malformée, un octet illisible selon le mauvais charset
 * sont des états du MONDE (l'opérateur peut corriger le CSV), jamais des
 * bugs du code appelant. C'est donc la famille checked qui officie, et le
 * chaînage de causes appris au module 50 devient le réflexe de base : chaque levée
 * emballe l'{@code IOException} d'origine derrière {@code getCause()} pour
 * que le « Caused by: » survive jusqu'au log.
 *
 * @author Emmanuel Bruno
 * @see NetworkLoader
 * @see ScheduleWriter
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
     * Décrit la situation ET préserve la cause d'origine (chaînage des causes).
     *
     * @param message le message explicatif du point de vue métier
     * @param cause   l'exception de bas niveau enrobée ({@code IOException},
     *                {@code MalformedInputException}…), ou {@code null}
     */
    public NetworkException(String message, Throwable cause) {
        super(message, cause);
    }
}
