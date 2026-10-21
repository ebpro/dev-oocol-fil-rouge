package fr.ebruno.dev.oocol.modern.domain;

/**
 * Le mode de propulsion du matériel roulant — hiérarchie scellée imbriquée.
 *
 * <p>Trois possibilités, fermées : {@link Electrique}, {@link Diesel},
 * {@link Hydrogene}. Comme pour {@link Vehicle}, la valeur ajoutée du scellage
 * est pour le <em>consommateur</em> : un switch sur une {@code Propulsion}
 * (cf. la scène 2 de la démonstration) est vérifié exhaustif par le
 * compilateur. Le libellé d'une propulsion se calcule sans {@code else}
 * par défaut et sans branche « type inattendu » — l'univers des propulsions
 * autorisées est écrit dans l'interface.
 *
 * <p>Interface vide de méthodes : le scellage est ici la seule valeur — un
 * type somme (une « union de types »), ce que Java n'avait pas avant
 * les sealed interfaces combinées aux records.
 *
 * @author Emmanuel Bruno
 * @see Electrique
 * @see Diesel
 * @see Hydrogene
 */
public sealed interface Propulsion permits Electrique, Diesel, Hydrogene {
}
