package fr.ebruno.dev.oocol.modern.domain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.OptionalDouble;
import java.util.Set;

/**
 * Le miroir verbeux : les mêmes opérations qu'{@link AnalyseReseau}, écrites
 * « à la main » dans le style des modules 30 à 60 — boucles for-each,
 * {@code instanceof} + cast, {@code Map.put} manuel, {@code StringBuilder}
 * et {@code Comparator} en classe interne anonyme.
 *
 * <p><strong>Ce fichier n'est PAS le code correct.</strong> Il compile, il
 * tourne, et il produit des résultats <em>strictement identiques</em> à
 * {@link AnalyseReseau} — c'est précisément la démonstration : le diff
 * entre les deux fichiers EST la démonstration (3 lignes de pipeline contre 15 lignes
 * de boucle, à résultat égal). On le garde dans le dépôt comme borne
 * « avant », pour compter les lignes soi-même plutôt que de croire le commentaire sur parole.
 *
 * <p><strong>Pourquoi « défensif ».</strong> Chaque {@code else} final qui
 * lance {@code IllegalStateException} (« type inattendu ») est une branche
 * que le compilateur aurait pu supprimer — c'est exactement ce que font les
 * hiérarchies {@code sealed} et les switchs exhaustifs d'{@link AnalyseReseau}.
 *
 * @author Emmanuel Bruno
 * @see AnalyseReseau
 */
public final class VersionLegacy {

    /** Aucune instance : fonctions pures, comme leur équivalent moderne. */
    private VersionLegacy() {
    }

    // ANCIENNE FAÇON (style des modules précédents) — ~8 lignes. Comparez avec AnalyseReseau.stationsAccessibles() : 3 lignes.
    /**
     * Filtre des arrêts accessibles par boucle et variable tampon.
     *
     * @param ligne la ligne analysée
     * @return les arrêts accessibles, dans l'ordre de la ligne
     */
    public static List<Station> stationsAccessibles(Ligne ligne) {
        List<Station> resultat = new ArrayList<>();
        for (Station s : ligne.arrets()) {
            if (s.accessible()) {
                resultat.add(s);
            }
        }
        return resultat;
    }

    // ANCIENNE FAÇON (style des modules précédents) — ~15 lignes, double boucle + put manuel. Comparez avec AnalyseReseau.grouperParZone() : 4 lignes.
    /**
     * Regroupe les arrêts par zone avec deux boucles imbriquées et une
     * gestion manuelle de la carte (containsKey / get / put).
     *
     * @param lignes les lignes analysées
     * @return la carte zone → arrêts
     */
    public static Map<Zone, List<Station>> grouperParZone(List<Ligne> lignes) {
        Map<Zone, List<Station>> resultat = new HashMap<>();
        for (Ligne l : lignes) {
            for (Station s : l.arrets()) {
                Zone z = s.zone();
                if (resultat.containsKey(z)) {
                    List<Station> existantes = resultat.get(z);
                    existantes.add(s);
                } else {
                    List<Station> nouvelle = new ArrayList<>();
                    nouvelle.add(s);
                    resultat.put(z, nouvelle);
                }
            }
        }
        return resultat;
    }

    // ANCIENNE FAÇON (style des modules précédents) — instanceof + cast à la chaîne, avec le "else" qui jette. Comparez avec AnalyseReseau.prixDe() : un switch scellé sans cast.
    /**
     * Prix d'un tarif par chaîne {@code instanceof}/{@code cast} : le compilateur
     * ne vérifie rien, d'où le {@code else} final « type inattendu » — branche
     * morte depuis que la hiérarchie est scellée, mais écrite quand même.
     *
     * @param tarif le titre à valoriser
     * @return le prix en euros
     */
    public static double prixDe(Tarif tarif) {
        if (tarif instanceof TarifUnite) {
            TarifUnite tu = (TarifUnite) tarif;
            return tu.prix();
        } else if (tarif instanceof TarifJour) {
            TarifJour tj = (TarifJour) tarif;
            return tj.prix();
        } else if (tarif instanceof TarifAbonnement) {
            return AnalyseReseau.PRIX_ABONNEMENT_MENSUEL;
        } else {
            throw new IllegalStateException("Type de tarif inconnu : " + tarif.getClass());
        }
    }

    // ANCIENNE FAÇON (style des modules précédents) — accumulateur double + garde liste vide. Comparez avec AnalyseReseau.prixMoyen() : mapToDouble().average().
    /**
     * Prix moyen par accumulateur explicite (somme, taille, division) — le
     * {@code OptionalDouble} n'apparaît qu'à la fin, « emballé » à la main.
     *
     * @param tarifs les tarifs analysés
     * @return la moyenne, ou vide si la liste est vide
     */
    public static OptionalDouble prixMoyen(List<Tarif> tarifs) {
        if (tarifs.isEmpty()) {
            return OptionalDouble.empty();
        }
        double somme = 0.0;
        for (Tarif t : tarifs) {
            somme += prixDe(t);
        }
        return OptionalDouble.of(somme / tarifs.size());
    }

    // ANCIENNE FAÇON (style des modules précédents) — Comparator en classe interne anonyme sur un ArrayList trié en place. Comparez avec AnalyseReseau.trierParCode() : sorted().
    /**
     * Trie une copie des stations par code, avec le {@code Comparator}
     * anonyme des débuts : cinq lignes pour exprimer ce que
     * {@code Station::compareTo} dit tout seul.
     *
     * @param stations les stations à trier (la liste d'origine n'est pas modifiée)
     * @return la liste triée
     */
    public static List<Station> trierParCode(List<Station> stations) {
        List<Station> copie = new ArrayList<>(stations);
        Collections.sort(copie, new Comparator<Station>() {
            @Override
            public int compare(Station a, Station b) {
                return a.code().compareTo(b.code());
            }
        });
        return copie;
    }

    // ANCIENNE FAÇON (style des modules précédents) — instanceof + cast + concaténation. Comparez avec le switch de motifs sur Vehicle (scène 2 de la démo) : sans cast, vérifié exhaustif.
    /**
     * Rapport technique d'un véhicule, dans le style pré-16 : trois
     * {@code instanceof} suivis de trois casts, plus le {@code throw} de
     * « type inattendu » qu'on écrivait par réflexe.
     *
     * @param v le véhicule à décrire
     * @return le rapport, au format « Métro N voitures » / « Bus ligne X » / « Tram N m »
     */
    public static String rapport(Vehicle v) {
        if (v instanceof MetroTrain) {
            MetroTrain m = (MetroTrain) v;
            return "M\u00e9tro " + m.nombreVoitures() + " voitures";
        } else if (v instanceof Bus) {
            Bus b = (Bus) v;
            return "Bus ligne " + b.ligne();
        } else if (v instanceof Tramway) {
            Tramway t = (Tramway) v;
            return "Tram " + String.format("%.0f m", t.longueurMetres());
        } else {
            throw new IllegalStateException("Véhicule inconnu : " + v.getClass());
        }
    }

    // ANCIENNE FAÇON (style des modules précédents) — StringBuilder ligne par ligne, compteurs manuels. Comparez avec AnalyseReseau.resumerReseau() : un bloc """ et trois pipelines.
    /**
     * Résumé du réseau par {@code StringBuilder} : chaque ligne du texte est
     * un {@code append} avec son {@code \n} explicite — le résultat est
     * <em>octet pour octet</em> celui du bloc de texte moderne (prouvé en test).
     *
     * @param lignes les lignes du réseau
     * @return le résumé multi-lignes (terminé par un saut de ligne)
     */
    public static String resumerReseau(List<Ligne> lignes) {
        int nbArrets = 0;
        int nbAccessibles = 0;
        Set<Zone> zones = new HashSet<>();
        for (Ligne l : lignes) {
            for (Station s : l.arrets()) {
                nbArrets++;
                if (s.accessible()) {
                    nbAccessibles++;
                }
                zones.add(s.zone());
            }
        }
        StringBuilder texte = new StringBuilder();
        texte.append("== Résumé du réseau ==\n");
        texte.append("Lignes      : ").append(lignes.size()).append('\n');
        texte.append("Arrêts      : ").append(nbArrets).append('\n');
        texte.append("Accessibles : ").append(nbAccessibles).append('\n');
        texte.append("Zones       : ").append(zones.size()).append('\n');
        return texte.toString();
    }
}
