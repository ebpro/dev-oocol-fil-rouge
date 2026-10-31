package fr.ebruno.dev.oocol.serialisation.domain;

import java.io.Serializable;
import java.time.LocalDate;

/**
 * Un abonnement voyageur — l'objet que le champ {@code transient} va sacrifier.
 *
 * <p><strong>Rôle dans le module.</strong> {@code Passenger} porte un
 * {@code Subscription} marqué {@code transient} : l'abonnement ne voyage
 * PAS dans l'instantané sérialisé. C'est le point de vigilance {@code transient},
 * démontré en test : après relecture, le champ est {@code null} — et chaque appelant
 * qui suppose l'objet « revenu intact » prend un {@code NullPointerException}
 * six mois plus tard dans un contexte que plus personne n'a sous les yeux.
 * {@code transient} ne supprime pas une information, il la remplace par
 * {@code null} : c'est un choix à documenter et à tester, pas un détail
 * d'implémentation.
 *
 * <p>Soi-même, {@code Subscription} est sérialisable sans histoires : deux
 * champs ({@code String}, {@code LocalDate}) déjà sérialisables par la JVM.
 *
 * @author Emmanuel Bruno
 * @see Passenger
 */
public class Subscription implements Serializable {

    /** Version du format de l'abonnement. */
    private static final long serialVersionUID = 1L;

    /** Type d'abonnement (ex. {@code "Mensuel"}, {@code "Annuel"}). */
    private final String type;

    /** Date d'expiration. Immuable et sérialisable par nature. */
    private final LocalDate expireDate;

    /**
     * Construit un abonnement valide.
     *
     * @param type       type d'abonnement, non vide
     * @param expireDate date d'expiration, obligatoire
     * @throws IllegalArgumentException si un argument manque
     */
    public Subscription(String type, LocalDate expireDate) {
        if (type == null || type.isBlank()) {
            throw new IllegalArgumentException("Le type d'abonnement est obligatoire.");
        }
        if (expireDate == null) {
            throw new IllegalArgumentException("La date d'expiration est obligatoire.");
        }
        this.type = type;
        this.expireDate = expireDate;
    }

    /**
     * @return le type d'abonnement
     */
    public String getType() {
        return type;
    }

    /**
     * @return la date d'expiration
     */
    public LocalDate getExpireDate() {
        return expireDate;
    }

    /**
     * Représentation lisible : {@code "Abonnement Mensuel (échéance 2026-11-30)"}.
     *
     * @return le libellé de l'abonnement
     */
    @Override
    public String toString() {
        return "Abonnement " + type + " (échéance " + expireDate + ")";
    }
}
