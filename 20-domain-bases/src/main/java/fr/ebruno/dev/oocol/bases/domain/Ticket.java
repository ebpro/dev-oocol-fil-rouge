package fr.ebruno.dev.oocol.bases.domain;

import java.util.Locale;

/**
 * Un titre de transport à l'unité (ticket « Unité », « Journée »,
 * abonnement « Mensuel », tarif « Scolaire »), émis pour une zone donnée.
 *
 * <p><strong>Classe de conclusion du module — les fabriques statiques.</strong>
 * Le constructeur de {@code Ticket} est <em>privé</em> : impossible d'écrire
 * {@code new Ticket(...)}. Pour créer un ticket, on passe par une méthode
 * fabrique statique : {@link #unite(Zone)}, {@link #jour(Zone)},
 * {@link #mois(Zone)}, {@link #scolaire(Zone)}.
 *
 * <p>Pourquoi c'est plus lisible qu'un constructeur ? Comparons :
 * <ul>
 *   <li>{@code Ticket.jour(zone)} — le <em>nom de la méthode</em> dit ce
 *       qu'on fabrique ;</li>
 *   <li>{@code new Ticket("Journée", 5.20, zone)} — trois arguments anonymes :
 *       quel est le prix du « Journée » ? Une inversion {@code type}/{@code prix}
 *       se lit sans broncher et passe le compilateur. Les fabriques suppriment
 *       cette classe d'erreur.</li>
 * </ul>
 *
 * <p>Tous les attributs sont {@code final} : un ticket émis ne change plus —
 * ni son type, ni son prix, ni sa zone (immuabilité, cf. {@link Zone}).
 *
 * @author Emmanuel Bruno
 * @see Zone
 */
public class Ticket {

    /** Type du titre, fixé par la fabrique qui a créé l'objet (ex. {@code "Unité"}). */
    private final String type;

    /** Prix du titre, en euros. Fixé par la fabrique — jamais par l'appelant. */
    private final double prix;

    /**
     * Validité du titre. Toujours {@code true} pour un ticket créé par une
     * fabrique : sans date d'expiration, le statut reste un booléen posé à la
     * construction ; avec une telle date, {@code estValide()} deviendrait un
     * vrai calcul.
     */
    private final boolean valide;

    /** Zone tarifaire pour laquelle le titre est émis. */
    private final Zone zone;

    /**
     * Constructeur privé — <em>tout</em> passe par les fabriques.
     *
     * <p>Un constructeur privé n'est pas une absence de constructeur : c'est
     * un point de contrôle unique. Le prix ne peut plus être inventé par
     * l'appelant, il est fixé par la grille tarifaire de la compagnie.
     *
     * @param type type du titre (posé par la fabrique)
     * @param prix prix en euros (posé par la fabrique)
     * @param zone zone d'émission
     */
    private Ticket(String type, double prix, Zone zone) {
        if (zone == null) {
            throw new IllegalArgumentException("Un ticket est émis pour une zone.");
        }
        this.type = type;
        this.prix = prix;
        this.zone = zone;
        // Un ticket sort fraîchement validé de la borne.
        this.valide = true;
    }

    /**
     * Émet un ticket « Unité » (voyage unique) pour la zone donnée.
     *
     * @param zone zone d'émission
     * @return le ticket Unité (1,80 €)
     */
    public static Ticket unite(Zone zone) {
        return new Ticket("Unité", 1.80, zone);
    }

    /**
     * Émet un ticket « Journée » (voyages illimités le jour même).
     *
     * @param zone zone d'émission
     * @return le ticket Journée (5,20 €)
     */
    public static Ticket jour(Zone zone) {
        return new Ticket("Journée", 5.20, zone);
    }

    /**
     * Émet un abonnement mensuel.
     *
     * @param zone zone d'émission
     * @return l'abonnement mensuel (32,00 €)
     */
    public static Ticket mois(Zone zone) {
        return new Ticket("Abonnement mensuel", 32.00, zone);
    }

    /**
     * Émet un titre scolaire (tarif étudiant/lycéen).
     *
     * @param zone zone d'émission
     * @return le titre Scolaire (12,00 €)
     */
    public static Ticket scolaire(Zone zone) {
        return new Ticket("Scolaire", 12.00, zone);
    }

    /**
     * @return le type du titre (ex. {@code "Unité"})
     */
    public String getType() {
        return type;
    }

    /**
     * @return le prix en euros
     */
    public double getPrix() {
        return prix;
    }

    /**
     * @return la zone d'émission du titre
     */
    public Zone getZone() {
        return zone;
    }

    /**
     * Le titre est-il valide ?
     *
     * <p>Toujours {@code true} en l'état (cf. le champ {@link #valide}) :
     * le titre n'a pas de date d'expiration ; s'il en avait une, cette
     * méthode comparerait la date courante à la date de fin.
     *
     * @return {@code true} si le titre est utilisable
     */
    public boolean estValide() {
        return valide;
    }

    /**
     * Représentation lisible, ex. {@code "Ticket Unité — 1,80 € — Zone 1"}.
     *
     * <p>Le formatage passe par {@link Locale#FRANCE} : la monnaie française
     * s'écrit avec une <em>virgule</em> décimale (« 1,80 € »), pas un point.
     * Sans locale explicite, le résultat dépendrait de la machine — un bug
     * internationalisation classique, que la méthode {@code toString()}
     * évite ici par contrat.
     *
     * @return le libellé du ticket
     */
    @Override
    public String toString() {
        return String.format(Locale.FRANCE, "Ticket %s — %.2f € — Zone %d",
                type, prix, zone.getNumero());
    }
}
