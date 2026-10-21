package fr.ebruno.dev.oocol.modern.domain;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.stream.Collectors;

/**
 * Analyse du réseau en pipelines Streams — la version « après ».
 *
 * <p><strong>Chaque méthode tient en 1-3 lignes là où la version W4-W6 en
 * prenait 10-20</strong> (cf. {@link VersionLegacy}, son miroir verbeux, qui
 * calcule des résultats <em>strictement identiques</em> — prouvé en test).
 * Le pipeline (source → opérations intermédiaires paresseuses → opération
 * terminale) remplace la boucle : {@code filter} remplace le {@code if},
 * {@code map} remplace la variable tampon, {@code flatMap} remplace la boucle
 * imbriquée, {@code groupingBy} remplace le {@code Map.put} à la main.
 *
 * <p>Le vocabulaire des λ et des références de méthode ({@code Station::zone},
 * {@code s -> s.accessible()}) n'est pas du sucre : c'est la déclaration de
 * <em>quoi</em> calculer plutôt que de <em>comment</em> itérer — la
 * séparation des responsabilités appliquée à la boucle.
 *
 * @author Emmanuel Bruno
 * @see VersionLegacy
 * @see Ligne
 */
public final class AnalyseReseau {

    /** Valorisation mensuelle de référence d'un abonnement (voir {@link TarifAbonnement}). */
    public static final double PRIX_ABONNEMENT_MENSUEL = 22.00;

    /** Aucune instance : fonctions pures sur le domaine, comme au module 20 le voulait le sens commun. */
    private AnalyseReseau() {
    }

    /**
     * Les arrêts accessibles (PMR) d'une ligne — {@code filter} contre le {@code if} dans la boucle.
     *
     * @param ligne la ligne analysée
     * @return les arrêts accessibles, dans l'ordre de la ligne
     */
    public static List<Station> stationsAccessibles(Ligne ligne) {
        return ligne.arrets().stream()
                .filter(Station::accessible)
                .toList();
    }

    /**
     * Regroupe tous les arrêts du réseau par zone — {@code flatMap} + {@code groupingBy}
     * contre la double boucle et le {@code Map.put} à la main.
     *
     * @param lignes les lignes analysées
     * @return la carte zone → arrêts (une zone sans arrêt n'apparaît pas)
     */
    public static Map<Zone, List<Station>> grouperParZone(List<Ligne> lignes) {
        return lignes.stream()
                .flatMap(l -> l.arrets().stream())
                .collect(Collectors.groupingBy(Station::zone));
    }

    /**
     * Les stations triées par code — {@code sorted()} sur l'ordre naturel du
     * module 40 ({@link Station#compareTo}), sans la classe interne anonyme de
     * {@link VersionLegacy#trierParCode(List)}.
     *
     * @param stations les stations à trier
     * @return la liste triée, immuable
     */
    public static List<Station> trierParCode(List<Station> stations) {
        return stations.stream()
                .sorted()
                .toList();
    }

    /**
     * L'arrêt dont le code est le plus « proche » du code donné — au sens de la
     * distance {@link String#compareTo} : {@code min} avec un comparateur λ,
     * contre la variable « meilleur » tripatouillée du module 40.
     *
     * @param code     le code de référence
     * @param stations les candidats
     * @return la station la plus proche, ou {@link Optional#empty()} si la liste est vide
     */
    public static Optional<Station> stationLaPlusProcheDe(String code, List<Station> stations) {
        return stations.stream()
                .min(Comparator.comparingInt(s -> Math.abs(s.code().compareTo(code))));
    }

    /**
     * Prix moyen d'une liste de tarifs — {@code mapToDouble} + {@code average}
     * contre l'accumulateur {@code double somme} du module 40.
     *
     * @param tarifs les tarifs analysés
     * @return la moyenne, ou {@link OptionalDouble#empty()} si la liste est vide
     */
    public static OptionalDouble prixMoyen(List<Tarif> tarifs) {
        return tarifs.stream()
                .mapToDouble(AnalyseReseau::prixDe)
                .average();
    }

    /**
     * Le prix d'un tarif — switch <em>exhaustif</em> sur la hiérarchie scellée
     * {@link Tarif}. Pas de {@code default} : ajouter une variante à
     * {@code permits} casserait la compilation d'ici, et c'est voulu.
     * L'abonnement, qui n'a pas de champ {@code prix}, est valorisé au tarif
     * mensuel de référence — le switch force la décision.
     *
     * @param tarif le titre à valoriser
     * @return le prix en euros
     */
    public static double prixDe(Tarif tarif) {
        return switch (tarif) {
            case TarifUnite t -> t.prix();
            case TarifJour t -> t.prix();
            case TarifAbonnement a -> PRIX_ABONNEMENT_MENSUEL;
        };
    }

    /**
     * Résumé du réseau en un bloc de texte — {@code """} text block contre le
     * {@code StringBuilder} ligne par ligne de {@link VersionLegacy#resumerReseau(List)}.
     *
     * @param lignes les lignes du réseau
     * @return le résumé multi-lignes (terminé par un saut de ligne)
     */
    public static String resumerReseau(List<Ligne> lignes) {
        long nbArrets = lignes.stream().flatMap(l -> l.arrets().stream()).count();
        long nbAccessibles = lignes.stream()
                .flatMap(l -> l.arrets().stream())
                .filter(Station::accessible)
                .count();
        long nbZones = lignes.stream()
                .flatMap(l -> l.arrets().stream())
                .map(Station::zone)
                .distinct()
                .count();
        return """
                == Résumé du réseau ==
                Lignes      : %d
                Arrêts      : %d
                Accessibles : %d
                Zones       : %d
                """.formatted(lignes.size(), nbArrets, nbAccessibles, nbZones);
    }
}
