package fr.ebruno.dev.oocol.serialisation.domain;

import java.io.Serializable;

/**
 * Le passager « singleton » du réseau — et le piège n° 1 de la sérialisation,
 * joué ici dans sa forme cassée (cf. {@link PassengerCorrige} pour le pansement).
 *
 * <p><strong>Pourquoi la sérialisation casse les singletons.</strong>
 * Le motif Singleton promet « une seule instance ». Mais le mécanisme de
 * sérialisation, lui, ne connaît qu'une règle : écrire les champs, puis
 * <em>allouer une NOUVELLE instance</em> à la relecture — sans jamais passer
 * par le constructeur privé. Résultat : sérialiser {@code INSTANCE} puis la
 * relire produit une <em>copie</em> distincte — l'invariant « une seule
 * instance » est mort, et avec lui tout ce qui en dépend (compteurs, caches,
 * connexions partagées). Démontré en test ({@code PitfallsTest}).
 *
 * <p><strong>Le second visage du problème.</strong> Les champs {@code static}
 * ne voyagent JAMAIS : {@code INSTANCE} est statique, donc le flux ne
 * contient pas « le singleton », seulement l'état de l'objet au moment de
 * l'écriture. La relecture recrée un objet neuf avec cet état — c'est
 * exactement ce qui produit la copie.
 *
 * <p><strong>Le remède officiel</strong> : {@code private Object readResolve()}
 * — hook appelé par le mécanisme APRÈS la reconstruction, dont la valeur
 * retournée remplace l'objet relu. Fourni dans {@link PassengerCorrige},
 * testé, et mentionné dans le README (piège n° 1).
 *
 * @author Emmanuel Bruno
 * @see PassengerCorrige
 * @see Subscription
 */
public class Passenger implements Serializable {

    /** Version du format du passager. */
    private static final long serialVersionUID = 1L;

    /**
     * L'instance « singleton » — statique, donc ABSENTE du flux sérialisé.
     * C'est le piège : le lecteur croit relire l'original, il fabrique un jumeau.
     */
    private static Passenger INSTANCE = new Passenger("Passager par défaut");

    /** Nom du passager. */
    private final String nom;

    /**
     * Abonnement en cours — {@code transient} : NE voyage PAS (piège n° 3,
     * cf. {@link Subscription}). Après désérialisation : {@code null}.
     */
    private transient Subscription abonnement;

    /**
     * Construit un passager nominatif.
     *
     * @param nom nom du passager, non vide
     * @throws IllegalArgumentException si le nom est vide
     */
    public Passenger(String nom) {
        if (nom == null || nom.isBlank()) {
            throw new IllegalArgumentException("Le nom du passager est obligatoire.");
        }
        this.nom = nom;
    }

    /**
     * @return l'instance partagée « singleton » (qui ne survit PAS à un aller-retour sérialisé — cf. Javadoc de classe)
     */
    public static Passenger getInstance() {
        return INSTANCE;
    }

    /**
     * @return le nom du passager
     */
    public String getNom() {
        return nom;
    }

    /**
     * @return l'abonnement courant, ou {@code null} (jamais sérialisé — cf. {@link #abonnement})
     */
    public Subscription getAbonnement() {
        return abonnement;
    }

    /**
     * Affecte l'abonnement en cours.
     *
     * @param abonnement le nouvel abonnement, nullable
     */
    public void setAbonnement(Subscription abonnement) {
        this.abonnement = abonnement;
    }

    /**
     * Représentation lisible : {@code "Passager Alice"}.
     *
     * @return le nom du passager
     */
    @Override
    public String toString() {
        return "Passager " + nom;
    }
}
