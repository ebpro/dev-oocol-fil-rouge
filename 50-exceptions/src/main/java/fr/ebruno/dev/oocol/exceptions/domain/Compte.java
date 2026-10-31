package fr.ebruno.dev.oocol.exceptions.domain;

/**
 * Le compte d'un usager du réseau : un solde en euros que la borne
 * {@link #debiter(double)} puise pour payer les titres.
 *
 * <p><strong>Nouvelle entité de ce module — le support vivant du checked.</strong>
 * {@code Compte} n'existait pas dans les modules précédents : il est créé
 * parce qu'il fallait une opération dont l'échec est <em>récupérable par
 * nature</em>. Débit refusé faute de solde ? L'appelant a un plan B réel :
 * recharger puis réessayer. {@link #debiter(double)} l'inscrit donc dans sa
 * signature ({@code throws InsufficientFundsException}) et le compilateur
 * force chaque appelant à choisir : rattraper et recharger, ou propager à
 * qui saura le faire. C'est le pendant exact de {@link Line#valider()} —
 * deux checked, deux récupérables, deux plans B.
 *
 * <p>À l'inverse, {@link #crediter(double)} un montant négatif est un bug
 * de l'appelant : {@link IllegalArgumentException}, unchecked, immédiate.
 * La classe joue les deux registres côte à côte — c'est voulu.
 *
 * <p>Pas de concurrence ici (module mono-thread) : un solde en
 * {@code double} suffit à la démonstration ; la monnaie strictement exacte
 * ({@code BigDecimal}) est un sujet hors ELO 07.
 *
 * @author Emmanuel Bruno
 * @see InsufficientFundsException
 * @see Line#valider()
 */
public class Compte {

    /** Identifiant de l'usager (ex. {@code "U-42"}). Immuable : la clé du compte. */
    private final String titulaire;

    /** Solde courant en euros. Initialement 0. */
    private double solde;

    /**
     * Ouvre un compte à solde nul.
     *
     * @param titulaire identifiant de l'usager, non vide
     * @throws IllegalArgumentException si le titulaire est {@code null} ou blanc
     */
    public Compte(String titulaire) {
        this(titulaire, 0.0);
    }

    /**
     * Ouvre un compte avec un solde initial (rechargé d'avance).
     *
     * @param titulaire    identifiant de l'usager, non vide
     * @param soldeInitial solde de départ, positif ou nul
     * @throws IllegalArgumentException si le titulaire est blanc ou le solde négatif
     */
    public Compte(String titulaire, double soldeInitial) {
        if (titulaire == null || titulaire.isBlank()) {
            throw new IllegalArgumentException("Un compte doit avoir un titulaire.");
        }
        if (soldeInitial < 0) {
            throw new IllegalArgumentException(
                    "Solde initial négatif impossible : " + soldeInitial);
        }
        this.titulaire = titulaire;
        this.solde = soldeInitial;
    }

    /**
     * Puise un montant sur le compte — OU lève une exception récupérable.
     *
     * <p><strong>Le contrat au coeur du module.</strong> Trois issues,
     * trois canaux :
     * <ul>
     *   <li>montant négatif → {@link IllegalArgumentException} (unchecked :
     *       demander un débit négatif est un bug de la borne appelante) ;</li>
     *   <li>solde insuffisant → {@link InsufficientFundsException} (CHECKED :
     *       situation d'affaire prévue, plan B légitime — recharger — que le
     *       compilateur force l'appelant à considérer) ;</li>
     *   <li>solde suffisant → débit, {@code true} retourné (le succès banal).</li>
     * </ul>
     * L'appelant qui rattrape le checked dispose de {@link
     * InsufficientFundsException#getManquant()} pour recharger <em>exactement</em>
     * ce qu'il faut : la donnée utile voyage dans l'objet d'erreur, pas dans
     * un message à parser.
     *
     * @param montant le prix à débiter, positif ou nul
     * @return {@code true} si le débit est passé
     * @throws IllegalArgumentException     si le montant est négatif (bug appelant)
     * @throws InsufficientFundsException   si le solde ne couvre pas le montant
     *                                      (checked : à rattraper ou à propager explicitement)
     */
    public boolean debiter(double montant) throws InsufficientFundsException {
        if (montant < 0) {
            throw new IllegalArgumentException("Débit négatif impossible : " + montant);
        }
        if (solde < montant) {
            throw new InsufficientFundsException(solde, montant);
        }
        solde = solde - montant;
        return true;
    }

    /**
     * Recharge le compte — le plan B du checked précédent.
     *
     * @param montant le crédit à passer, strictement positif
     * @throws IllegalArgumentException si le montant est nul ou négatif
     *                                  (unchecked : un crédit de 0 ou −5 est un bug appelant)
     */
    public void crediter(double montant) {
        if (montant <= 0) {
            throw new IllegalArgumentException("Crédit nul ou négatif impossible : " + montant);
        }
        solde = solde + montant;
    }

    /**
     * @return le solde courant en euros
     */
    public double getSolde() {
        return solde;
    }

    /**
     * @return l'identifiant du titulaire
     */
    public String getTitulaire() {
        return titulaire;
    }

    /**
     * Représentation lisible : {@code "Compte[U-42 : 12,50 €]"}.
     *
     * @return le libellé du compte avec son solde
     */
    @Override
    public String toString() {
        return "Compte[" + titulaire + " : " + solde + " \u20AC]";
    }
}
