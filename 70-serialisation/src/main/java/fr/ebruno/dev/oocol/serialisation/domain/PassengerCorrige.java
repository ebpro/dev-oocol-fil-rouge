package fr.ebruno.dev.oocol.serialisation.domain;

import java.io.Serializable;

/**
 * Le singleton réparé : même idée que {@link Passenger}, plus le hook
 * officiel {@code readResolve} — la version corrigée du point de vigilance.
 *
 * <p><strong>{@code readResolve}.</strong> Signature imposée :
 * {@code private Object readResolve()}. Le mécanisme l'appelle APRÈS avoir
 * reconstruit l'objet depuis le flux ; la valeur retournée REMPLACE l'objet
 * reconstruit. En retournant {@code INSTANCE}, on fait en sorte que quiconque
 * désérialise ce singleton reçoive TOUJOURS l'instance partagée : l'invariant
 * « une seule instance » survit au voyage binaire. (Le jumeau reconstruit est
 * tout de même alloué, puis jeté — c'est le prix de la correction, et la
 * raison pour laquelle le JDK recommande aussi {@code writeReplace} pour
 * sérialiser un proxy à la place ; mentionné dans le README, non implémenté ici.)
 *
 * <p>Référence classique : <em>Effective Java</em>, article 89 — « défendez la
 * concurrence d'accès avec readResolve lors de l'utilisation d'une instance
 * de contrôle ».
 *
 * @author Emmanuel Bruno
 * @see Passenger
 */
public final class PassengerCorrige implements Serializable {

    /** Version du format. */
    private static final long serialVersionUID = 1L;

    /** L'unique instance — statique, donc absente du flux comme pour {@link Passenger}. */
    private static final PassengerCorrige INSTANCE = new PassengerCorrige();

    /** Nom du passager. */
    private final String nom;

    /**
     * Constructeur PRIVÉ : la porte d'entrée unique est {@link #getInstance()} —
     * sans cette fermeture, le « singleton » n'en est pas un, hook ou pas hook.
     */
    private PassengerCorrige() {
        this.nom = "Passager par défaut";
    }

    /**
     * @return l'instance partagée — et la SEULE, désormais garantie par {@link #readResolve()}
     */
    public static PassengerCorrige getInstance() {
        return INSTANCE;
    }

    /**
     * @return le nom du passager
     */
    public String getNom() {
        return nom;
    }

    /**
     * Hook officiel anti-duplication : appelé par le mécanisme APRÈS reconstruction
     * depuis le flux, retourne l'instance partagée à la place du jumeau.
     *
     * @return {@link #INSTANCE}, toujours
     */
    private Object readResolve() {
        return INSTANCE;
    }

    /**
     * Représentation lisible : {@code "Passager corrigé Alice"}.
     *
     * @return le nom du passager corrigé
     */
    @Override
    public String toString() {
        return "Passager corrigé " + nom;
    }
}
