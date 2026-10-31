package fr.ebruno.dev.oocol.modern.domain;

import java.util.Objects;

/**
 * Un arrêt du réseau — record avec validation, {@link Comparable} et comportement métier.
 *
 * <p><strong>Ce que le record remplace.</strong> Le {@code Station} des
 * modules 20 à 70 : quatre champs {@code private final}, constructeur
 * gardien, quatre getters, {@code equals}/{@code hashCode}/{@code toString}
 * recopiés à la main, plus l'implémentation de {@code Comparable}. Le record
 * absorbe toute la plomberie ; il ne reste que ce qui a une valeur de conception : les gardes du constructeur compact et les méthodes métier
 * ({@link #compareTo}, {@link #affichage()}).
 *
 * <p><strong>Un record n'est pas qu'un data-carrier muet.</strong> Il peut
 * implémenter des interfaces, porter des méthodes, valider ses invariants.
 * La règle du cours : <em>données pures et immuables → record ; état mutable
 * ou cycle de vie → classe</em>. Un arrêt ne change pas de code en cours de
 * route : c'est un record.
 *
 * <p>Le composant {@code accessible} (PMR) sera le prédicat des pipelines de
 * {@link AnalyseReseau} — un {@code boolean} dans la liste de composants
 * vaut mieux qu'un {@code Boolean} nullable et son NPE.
 *
 * @author Emmanuel Bruno
 * @see Zone
 * @see Ligne
 */
public record Station(String code, String nom, Zone zone, boolean accessible)
        implements Comparable<Station> {

    /**
     * Constructeur compact : le code et la zone sont obligatoires.
     *
     * @throws IllegalArgumentException si le code est vide
     * @throws NullPointerException     si la zone est {@code null}
     */
    public Station {
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("code requis");
        }
        Objects.requireNonNull(zone, "zone requise");
    }

    /**
     * Ordre naturel : par code, comme au module 40 (tri alphabétique des gares).
     *
     * @param autre la station comparée
     * @return le résultat de la comparaison des codes
     */
    @Override
    public int compareTo(Station autre) {
        return code.compareTo(autre.code);
    }

    /**
     * Libellé d'affichage : {@code "GAR — Gare Centrale"}.
     *
     * @return code et nom reliés par un tiret long
     */
    public String affichage() {
        return code + " \u2014 " + nom();
    }
}
