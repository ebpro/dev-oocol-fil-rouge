package fr.ebruno.dev.oocol.heritage.domain;

/**
 * Capacité de stocker puis restituer de l'énergie : ce que « un véhicule
 * rechargeable » sait faire, quel que soit son moteur.
 *
 * <p><strong>Interface de capacité (ISP) :</strong> la recharge est séparée
 * du comportement général du véhicule. Un {@link MetroTrain} roule sans
 * batterie embarquée (le troisième rail alimente en continu) ; un
 * {@link Tramway} en a une ; demain, une trolley-bus. Tous sont des
 * {@code Vehicle}, un seul est {@code Rechargeable} : la capacité ne peut pas
 * vivre dans la classe de base sans imposer ses méthodes à qui n'en a pas —
 * ce serait violer le principe de ségrégation des interfaces (nul ne doit
 * dépendre de méthodes qu'il n'utilise pas).
 *
 * @author Emmanuel Bruno
 * @see Tramway
 */
public interface Rechargeable {

    /**
     * Recharge le stockage d'énergie d'une quantité donnée.
     *
     * <p>Le contrat ne dit PAS ce que « recharger » fait en interne (plafond,
     * courbe de charge, température…) : seule l'implémentation le sait.
     * L'appelant, lui, connaît deux verbes : recharger et consulter.
     *
     * @param unite la quantité d'énergie à ajouter (négative : délestage autorisé,
     *              l'implémentation protège ses bornes)
     */
    void recharger(double unite);

    /**
     * Consulte l'état courant du stockage d'énergie.
     *
     * @return le niveau actuel, dans l'intervalle géré par l'implémentation
     *         (pour un tramway : 0.0 vide à 1.0 plein)
     */
    double niveauActuel();
}
