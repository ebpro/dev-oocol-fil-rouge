package fr.ebruno.dev.oocol.heritage.domain;

/**
 * Capacité de lire une vitesse : ce que « un véhicule équipé d'un capteur »
 * sait faire, indépendamment de son espèce.
 *
 * <p><strong>Interface de capacité (ISP) :</strong> un véhicule n'est PAS
 * obligé d'avoir des capteurs. {@link Bus} ne l'implémente pas aujourd'hui,
 * {@link Tramway} si. Si « capteur » était glissé dans
 * {@link Vehicle}, chaque futur non-capé (une remorque, un vélo en libre
 * service) hériterait d'une méthode qu'il ne peut pas honorer — l'héritage
 * forcerait le mensonge. Une interface se <em>choisit</em> à l'implémentation :
 * le contrat n'est signé que par ceux qui le tiennent.
 *
 * <p><strong>Méthode par défaut :</strong> {@link #estEnSurvitesse(double)}
 * apporte son implémentation avec le contrat (Java 8+). Le code reste simple —
 * un {@code if} — pour montrer le mécanisme sans entrer dans les lambdas
 * (réservées au module 80). Une implémentation peut la réécrire ; sinon,
 * l'héritage du comportement vient de l'interface elle-même.
 *
 * @author Emmanuel Bruno
 * @see Tramway
 */
public interface Capteur {

    /**
     * Lit la vitesse courante.
     *
     * @return la vitesse lue par le capteur (même unité que la vitesse maximale du véhicule)
     */
    double lireVitesse();

    /**
     * Détermine si le capteur lit au-dessus d'une limite.
     *
     * <p>Méthode <em>par défaut</em> : son corps vit dans l'interface et
     * s'applique à toute implémentation qui ne la réécrit pas. Elle n'appelle
     * que {@link #lireVitesse()} — le contrat minimal suffit à bâtir le
     * service : une interface riche en méthodes par défaut, pauvre en
     * obligations.
     *
     * @param limite la vitesse limite à ne pas dépasser
     * @return {@code true} si la vitesse lue dépasse {@code limite}
     */
    default boolean estEnSurvitesse(double limite) {
        return lireVitesse() > limite;
    }
}
