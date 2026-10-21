package fr.ebruno.dev.oocol.transit.domain;

import java.util.Objects;

/**
 * Un arrêt du réseau « Trans'U » — record immuable, ordonné par code.
 *
 * <p><strong>Le contrat du module 40, la forme du module 80.</strong> Le tri
 * naturel par code (alphabétique, comme à la semaine des collections) survit
 * sous {@link Comparable} ; l'égalité structurelle du record remplace les
 * quarante lignes de {@code equals}/{@code hashCode} recopiés. Le composant
 * {@code accessible} (PMR) est un {@code boolean} — jamais un
 * {@code Boolean} nullable et son NPE.
 *
 * <p>SRP : un arrêt décrit UN lieu. Il ne sait ni se lire dans un CSV
 * ({@code NetworkLoader}), ni s'écrire en SQL ({@code NetworkJdbcStore}),
 * ni s'afficher ({@code TransitCli}).
 *
 * @param code       identifiant court affiché aux voyageurs (ex. {@code "GAR"})
 * @param nom        libellé complet de l'arrêt
 * @param zone       zone tarifaire de rattachement (instance résolue par le Builder)
 * @param accessible vrai si l'arrêt est accessible aux personnes à mobilité réduite
 * @author Emmanuel Bruno
 */
public record Station(String code, String nom, Zone zone, boolean accessible)
        implements Comparable<Station> {

    /**
     * Constructeur compact : code et zone obligatoires.
     *
     * @throws IllegalArgumentException si le code est vide
     * @throws NullPointerException     si la zone est {@code null}
     */
    public Station {
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("code requis");
        }
        Objects.requireNonNull(zone, "zone requise");
        if (nom == null || nom.isBlank()) {
            throw new IllegalArgumentException("nom requis");
        }
    }

    /**
     * Ordre naturel : par code — le même choix qu'au module 40, pour que les
     * affichages de la CLI soient déterministes.
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
        return code + " \u2014 " + nom;
    }
}
