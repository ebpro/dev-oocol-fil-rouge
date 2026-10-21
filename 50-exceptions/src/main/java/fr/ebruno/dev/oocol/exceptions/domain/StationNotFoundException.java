package fr.ebruno.dev.oocol.exceptions.domain;

/**
 * UNCHECKED : c'est un BUG, pas une condition de contrôle. Si le code est
 * inconnu, le programme est mal construit — on ne « rattrape » pas, on
 * corrige le code appelant.
 *
 * <p><strong>Le contre-exemple pédagogique de la hiérarchie.</strong> Elle
 * n'hérite PAS de {@link NetworkException} — elle hérite directement de
 * {@code RuntimeException}. Ce choix est le message de la semaine :
 * « station inconnue » n'est pas une situation que l'appelant peut réparer
 * (il n'a rien à re-planifier, rien à recharger — il a demandé une station
 * que le réseau ne connaît pas, donc <em>son</em> code est faux). Obliger
 * chaque signature à écrire {@code throws StationNotFoundException} polluerait
 * le contrat d'appel d'erreurs qu'aucun appelant honnête ne peut gérer ;
 * laissez-la propager, la stack trace nommera le coupable.
 *
 * <p>Elle porte le {@code code} cherché : un appelant qui <em>a une bonne
 * raison</em> d'anticiper l'absence (un import de fichier malveillant, par
 * exemple — module {@code 60-io}) peut la rattraper et lire la donnée utile
 * sans parser le message.
 *
 * @author Emmanuel Bruno
 * @see NetworkException
 * @see Station#requireValide(Station, String)
 */
public class StationNotFoundException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /** Le code d'exploitation qui n'a rien trouvé (ex. {@code "ZZZ"}). Immuable. */
    private final String code;

    /**
     * Signale qu'aucune station ne porte ce code.
     *
     * @param code le code cherché, intégré au message sous la forme
     *             {@code "Station inconnue : [code]"}
     */
    public StationNotFoundException(String code) {
        super("Station inconnue : [" + code + "]");
        this.code = code;
    }

    /**
     * Signale une station inconnue ENROBANT une cause (chaînage) : par
     * exemple un index {@code HashMap} qui renvoie {@code null} et qu'on
     * traduit en exception métier sans jeter la trace d'origine.
     *
     * @param code  le code cherché
     * @param cause l'exception de bas niveau préservée, ou {@code null}
     */
    public StationNotFoundException(String code, Throwable cause) {
        super("Station inconnue : [" + code + "]", cause);
        this.code = code;
    }

    /**
     * @return le code d'exploitation recherché sans succès
     */
    public String getCode() {
        return code;
    }
}
