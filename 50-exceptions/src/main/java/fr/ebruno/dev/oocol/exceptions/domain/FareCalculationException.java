package fr.ebruno.dev.oocol.exceptions.domain;

/**
 * UNCHECKED : prix négatif = incohérence logique du programme, pas une
 * erreur utilisateur.
 *
 * <p>Comme {@link StationNotFoundException}, elle hérite directement de
 * {@code RuntimeException} : un tarif négatif ou une table de tarifs absente
 * signale que <em>le code qui construit le barème est faux</em>, non une
 * situation qu'un appelant pourrait rattraper à l'exécution. Personne ne
 * « recharge » un prix négatif ; on corrige la donnée fautive en amont.
 * La propager bruyamment jusqu'à la CI est exactement le comportement voulu.
 *
 * @author Emmanuel Bruno
 * @see FareCalculator
 * @see StationNotFoundException
 */
public class FareCalculationException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * Signale une incohérence de calcul du tarif.
     *
     * @param message le détail de l'incohérence (prix négatif, zone absente…)
     */
    public FareCalculationException(String message) {
        super(message);
    }

    /**
     * Signale une incohérence de tarif ENROBANT une cause (chaînage).
     *
     * @param message le détail vu du métier
     * @param cause   l'exception de bas niveau préservée, ou {@code null}
     */
    public FareCalculationException(String message, Throwable cause) {
        super(message, cause);
    }
}
