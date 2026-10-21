package fr.ebruno.dev.oocol.transit.cli;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import fr.ebruno.dev.oocol.transit.domain.Line;
import fr.ebruno.dev.oocol.transit.domain.Network;
import fr.ebruno.dev.oocol.transit.domain.Station;

/**
 * Commandes en ligne du réseau Trans'U — la couche de présentation, version
 * testable.
 *
 * <p><strong>Pourquoi des méthodes qui retournent des {@code String} plutôt
 * qu'un {@code Scanner} dans une boucle {@code while}.</strong> Une CLI
 * interactive est triple-ennemie : elle bloque en attente d'un clavier que
 * la CI n'a pas, elle imprime dans un flux que les assertions ne voient pas,
 * et elle mélange deux responsabilités (lire l'intention, produire le
 * texte). Chaque commande ici est une fonction pure {@code (réseau, args) →
 * String} : le MAIN décide d'imprimer, les TESTS décident d'asserter — la
 * même sortie sert les deux, sans aucune redirection de {@code System.out}.
 * Un vrai shell interactif ne serait plus qu'une boucle {@code while} autour
 * de ces méthodes ; il n'alourdirait ni la coverage, ni le graphe de dépendances.
 *
 * <p><strong>DIP — le CLI dépend d'une abstraction (les méthodes de
 * {@code Network}), pas de JDBC.</strong> Ces méthodes ne savent pas si le
 * réseau vient d'un Builder duré, d'un CSV (W7) ou d'une base (W12) : elles
 * n'appellent que des requêtes du domaine. On peut changer la persistance
 * sans toucher l'affichage — la preuve vivante de la rétrospective de la
 * phase 5 : trois sources de données, zéro ligne de CLI écrite deux fois.
 *
 * @author Emmanuel Bruno
 * @see fr.ebruno.dev.oocol.transit.app.TransitApp — qui imprime ces chaînes
 */
public final class TransitCli {

    /** Séparateur d'arrêts dans un parcours, homogène avec l'affichage des lignes. */
    private static final String FLECHE = " \u2192 ";

    /** Utilitaire statique : pas d'instance. */
    private TransitCli() {
        throw new AssertionError("TransitCli est une classe utilitaire : pas d'instanciation.");
    }

    /**
     * Liste tous les arrêts, triés par code (ordre naturel de {@link Station},
     * module 40).
     *
     * @param n le réseau à afficher
     * @return l'inventaire complet, un arrêt par ligne
     */
    public static String cmdListerStations(Network n) {
        StringBuilder sb = new StringBuilder();
        sb.append("Arrêts du réseau (").append(n.stations().size()).append(") :\n");
        n.stations().stream().sorted().forEach(s -> sb.append("  ")
                .append(s.affichage())
                .append(" (zone ").append(s.zone().numero()).append(", ")
                .append(s.accessible() ? "accessible" : "non accessible")
                .append(")\n"));
        return sb.toString();
    }

    /**
     * Liste toutes les lignes avec leurs arrêts dans l'ordre de parcours.
     *
     * @param n le réseau à afficher
     * @return une ligne par ligne de sortie : {@code "  Ligne A : GAR → UNI → …"}
     */
    public static String cmdListerLignes(Network n) {
        StringBuilder sb = new StringBuilder();
        sb.append("Lignes du réseau (").append(n.lignes().size()).append(") :\n");
        for (Line l : n.lignes()) {
            String parcours = String.join(FLECHE,
                    l.arrets().stream().map(Station::code).toList());
            sb.append("  ").append(l.nom()).append(" : ").append(parcours).append('\n');
        }
        return sb.toString();
    }

    /**
     * Cherche un arrêt par son code.
     *
     * <p>{@link Optional} en action (module 80) : le domaine retourne
     * « peut-être un arrêt », la présentation choisit les deux messages —
     * trouvé ET introuvable sont des chemins normaux, pas des exceptions.
     *
     * @param n    le réseau à interroger
     * @param code le code d'arrêt cherché
     * @return le message « Trouvé : … » ou « Introuvable : … »
     */
    public static String cmdChercher(Network n, String code) {
        Optional<Station> trouvee = n.station(code);
        if (trouvee.isEmpty()) {
            return "Introuvable : " + code;
        }
        Station s = trouvee.orElseThrow();
        return "Trouvé : " + s.affichage() + " (zone " + s.zone().numero() + ", "
                + (s.accessible() ? "accessible" : "non accessible") + ")";
    }

    /**
     * Statistiques du réseau : compteurs et part d'accessibilité, en text
     * block (module 80).
     *
     * @param n le réseau à résumer
     * @return le bloc de statistiques
     */
    public static String cmdStats(Network n) {
        int total = n.stations().size();
        int accessibles = n.stationsAccessibles().size();
        int part = total == 0 ? 0 : accessibles * 100 / total;
        return """
                ── Statistiques du réseau ──
                Zones       : %d
                Stations    : %d
                Lignes      : %d
                Accessibles : %d/%d (%d %%)\
                """.formatted(n.zones().size(), total, n.lignes().size(), accessibles, total, part);
    }

    /**
     * Itinéraire le plus court (en segments) entre deux codes d'arrêt.
     *
     * <p><strong>Le BFS du module 40 ({@code TripPlanner}), rejoué avec les
     * outils de la semaine 13.</strong> Même algorithme — ondulation
     * concentrique depuis le départ, file des découvertes, ensemble des
     * visites, map des prédécesseurs (fil d'Ariane) — mais la graphie
     * rajeunit : {@link Deque} en interface (le {@code LinkedList}-concret de
     * W5 est devenu un détail), texte composé en stream, deux stations
     * adjacentes quand elles sont <em>consécutives sur une même ligne</em>.
     *
     * @param n       le réseau à explorer
     * @param depart  code de l'arrêt de départ
     * @param arrivee code de l'arrêt d'arrivée
     * @return le chemin trouvé, ou le message d'échec (départ/arrivée inconnus,
     *         ou réseau débranché entre les deux)
     */
    public static String cmdItineraire(Network n, String depart, String arrivee) {
        Optional<Station> d = n.station(depart);
        Optional<Station> a = n.station(arrivee);
        if (d.isEmpty()) {
            return "Station de départ inconnue : " + depart;
        }
        if (a.isEmpty()) {
            return "Station d'arrivée inconnue : " + arrivee;
        }
        List<Station> chemin = plusCourtChemin(n, d.get(), a.get());
        if (chemin.isEmpty()) {
            return "Aucun itinéraire entre " + depart + " et " + arrivee + ".";
        }
        String parcours = String.join(FLECHE,
                chemin.stream().map(Station::code).toList());
        return "Itinéraire " + depart + " \u2192 " + arrivee + " : " + parcours
                + " (" + chemin.size() + " arr\u00EAts, " + (chemin.size() - 1) + " segments)";
    }

    /**
     * BFS pur : le chemin (départ et arrivée inclus) ou la liste vide si
     * aucun chemin n'existe.
     */
    private static List<Station> plusCourtChemin(Network n, Station depart, Station arrivee) {
        if (depart.equals(arrivee)) {
            return List.of(depart);
        }
        Deque<Station> aExplorer = new ArrayDeque<>();
        Set<Station> visites = new HashSet<>();
        Map<Station, Station> predecesseurs = new HashMap<>();
        aExplorer.add(depart);
        visites.add(depart);

        while (!aExplorer.isEmpty()) {
            Station courante = aExplorer.poll();
            for (Station voisin : voisinsDe(n, courante)) {
                if (visites.add(voisin)) { // add() renvoie false si déjà vue — le Set garde la visite
                    predecesseurs.put(voisin, courante);
                    if (voisin.equals(arrivee)) {
                        return reconstruire(predecesseurs, arrivee);
                    }
                    aExplorer.add(voisin);
                }
            }
        }
        return List.of();
    }

    /** Voisins directs : prédécesseur et successeur de {@code s} sur chaque ligne qui la desserre. */
    private static List<Station> voisinsDe(Network n, Station s) {
        List<Station> voisins = new ArrayList<>();
        for (Line l : n.lignesDe(s)) {
            List<Station> arrets = l.arrets();
            int position = arrets.indexOf(s);
            if (position > 0) {
                voisins.add(arrets.get(position - 1));
            }
            if (position + 1 < arrets.size()) {
                voisins.add(arrets.get(position + 1));
            }
        }
        return voisins;
    }

    /** Remonte le fil d'Ariane de l'arrivée au départ, puis inverse. */
    private static List<Station> reconstruire(Map<Station, Station> predecesseurs,
            Station arrivee) {
        List<Station> aEnvers = new ArrayList<>();
        Station etape = arrivee;
        while (etape != null) {
            aEnvers.add(etape);
            etape = predecesseurs.get(etape);
        }
        Collections.reverse(aEnvers);
        return List.copyOf(aEnvers);
    }
}
