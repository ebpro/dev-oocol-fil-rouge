package fr.ebruno.dev.oocol.exceptions.domain;

/**
 * CHECKED PAR EXCELLENCE : l'utilisateur peut recharger son compte. La
 * situation est anormale MAIS récupérable. On OBLIGE l'appelant à gérer.
 *
 * <p><strong>Le cas d'école du choix checked.</strong> Un solde insuffisant
 * n'est pas un bug du programme : c'est une situation d'affaire prévue, avec
 * une issue légitime (recharger, refuser poliment, proposer un autre moyen
 * de paiement). En la faisant dériver de {@link NetworkException} — donc
 * d'{@code Exception}, donc <em>checked</em> — on force le compilateur à
 * rappeler à chaque appelant de {@code Compte.debiter} qu'il DOIT traiter ce
 * cas : {@code catch} pour recharger, ou {@code throws} pour le faire
 * remonter à qui saura le faire. Oublier cette gestion ne compile même pas.
 * C'est la différence fondatrice avec {@link StationNotFoundException}
 * (unchecked : un bug ne se « gère » pas, il se corrige).
 *
 * <p>Elle porte le {@code solde} et le {@code prix} demandés et expose
 * {@link #getManquant()} : l'appelant qui rattrape sait exactement combien
 * recharger pour repasser — la donnée utile voyage dans l'objet, pas dans
 * une concaténation de message.
 *
 * @author Emmanuel Bruno
 * @see NetworkException
 * @see fr.ebruno.dev.oocol.exceptions.domain.Compte#debiter(double)
 */
public class InsufficientFundsException extends NetworkException {

    private static final long serialVersionUID = 1L;

    /** Solde disponible au moment du refus. Immuable. */
    private final double solde;

    /** Prix réclamé (le montant du débit). Immuable. */
    private final double prix;

    /**
     * Signale un débit refusé faute de solde suffisant.
     *
     * @param solde le solde disponible au moment du refus
     * @param prix  le montant réclamé
     */
    public InsufficientFundsException(double solde, double prix) {
        super("Solde insuffisant : " + prix + " demandé(s), " + solde + " disponible(s) — manquant "
                + (prix - solde));
        this.solde = solde;
        this.prix = prix;
    }

    /**
     * Signale un solde insuffisant ENROBANT une cause (chaînage).
     *
     * @param message le détail vu du métier
     * @param solde   le solde disponible
     * @param prix    le montant réclamé
     * @param cause   l'exception de bas niveau préservée, ou {@code null}
     */
    public InsufficientFundsException(String message, double solde, double prix, Throwable cause) {
        super(message, cause);
        this.solde = solde;
        this.prix = prix;
    }

    /**
     * @return le solde disponible au moment du refus
     */
    public double getSolde() {
        return solde;
    }

    /**
     * @return le prix réclamé (le montant du débit refusé)
     */
    public double getPrix() {
        return prix;
    }

    /**
     * De combien il faudrait créditer pour repasser.
     *
     * @return {@code prix - solde} — le manque, toujours positif ici
     */
    public double getManquant() {
        return prix - solde;
    }
}
