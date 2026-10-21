package fr.ebruno.dev.oocol.exceptions.domain;

/**
 * CHECKED : un conflit d'horaire est récupérable — on peut re-planifier le
 * départ. L'appelant doit gérer.
 *
 * <p>Elle hérite de {@link NetworkException} (donc de {@code Exception},
 * donc <em>checked</em>) : contrairement à une station inconnue, une
 * collision d'horaire a un plan B légitime — décaler le départ, doubler la
 * course, prévenir l'exploitation. Le compilateur OBLIGE chaque méthode de
 * la chaîne à écrire {@code throws ScheduleConflictException} ou à
 * {@code catch} : personne ne peut « oublier » d'avoir un plan.
 *
 * <p>Elle porte la {@code ligne} et l'{@code heureConflit} pour que
 * l'appelant puisse re-planifier <em>sans re-parser le message</em> : une
 * exception typée est un canal de données, pas qu'un cri. Elle accepte
 * aussi une {@code cause} : la scène 5 de la démo l'utilise pour enrober
 * une {@code NumberFormatException} de lecture de config et montrer le
 * chaînage (« Caused by: »).
 *
 * @author Emmanuel Bruno
 * @see NetworkException
 */
public class ScheduleConflictException extends NetworkException {

    private static final long serialVersionUID = 1L;

    /** Nom de la ligne en conflit. Immuable. */
    private final String ligne;

    /** Heure de départ (0-23) où le conflit apparaît. Immuable. */
    private final int heureConflit;

    /**
     * Conflit d'horaire, sans cause de bas niveau.
     *
     * @param message     le détail du conflit
     * @param ligne       la ligne concernée
     * @param heureConflit l'heure de départ en collision
     */
    public ScheduleConflictException(String message, String ligne, int heureConflit) {
        super(message);
        this.ligne = ligne;
        this.heureConflit = heureConflit;
    }

    /**
     * Conflit d'horaire ENROBANT une cause (démonstration du chaînage).
     *
     * @param message      le détail du conflit, vu du métier
     * @param ligne        la ligne concernée
     * @param heureConflit l'heure de départ en collision
     * @param cause        l'exception de bas niveau préservée (lecture de
     *                     config, parsing…), ou {@code null}
     */
    public ScheduleConflictException(String message, String ligne, int heureConflit, Throwable cause) {
        super(message, cause);
        this.ligne = ligne;
        this.heureConflit = heureConflit;
    }

    /**
     * @return le nom de la ligne en conflit
     */
    public String getLigne() {
        return ligne;
    }

    /**
     * @return l'heure de départ (0-23) où le conflit apparaît
     */
    public int getHeureConflit() {
        return heureConflit;
    }
}
