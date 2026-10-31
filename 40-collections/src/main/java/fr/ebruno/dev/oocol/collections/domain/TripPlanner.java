package fr.ebruno.dev.oocol.collections.domain;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

/**
 * Planificateur d'itinéraires : le chemin le plus court entre deux stations,
 * par parcours en largeur (BFS).
 *
 * <p><strong>Ici, chaque conteneur joue son rôle.</strong> Un même
 * algorithme mobilise les quatre familles de collections, chacune choisie
 * pour sa sémantique :
 * <ul>
 *   <li>{@code Queue<Station>} (implémentée par {@code LinkedList}) : la
 *       frontière à explorer — le BFS traite les stations dans l'ordre où il
 *       les a découvertes (premier arrivé, premier exploré) ;</li>
 *   <li>{@code Set<Station>} : les déjà-visitées — une station ne doit être
 *       explorée qu'une fois, et un Set répond exactement à « ai-je déjà vu
 *       CETTE station ? » ({@code contains} par {@code equals}) ;</li>
 *   <li>{@code Map<Station, Station>} : chaque station → sa prédécesseure
 *       dans l'exploration — c'est le fil d'Ariane qui permet de reconstruire
 *       le chemin une fois l'arrivée touchée ;</li>
 *   <li>{@code List<Station>} : le résultat — un itinéraire EST ordonné.</li>
 * </ul>
 *
 * <p>Le graphe exploré relie deux stations quand elles sont <em>consécutives
 * sur une même ligne</em> ({@link #voisinsDe}) : le BFS compte donc des
 * trajets d'arrêts, et le premier chemin trouvé est le plus court en nombre
 * d'arrêts intermédiaires. Le tri par « changements de ligne minimaux » est
 * hors du périmètre de ce module.
 *
 * <p><strong>Parcours en largeur, en français :</strong> on explore toutes
 * les stations à 1 arrêt de distance, puis toutes celles à 2, etc. — comme
 * une ondulation concentrique autour du départ. Quand l'ondulation touche
 * l'arrivée, le chemin découvert est nécessairement le plus court : rien
 * d'atteignable en moins d'étapes n'aurait pu rester insoupçonné, il eût
 * été touché avant.
 *
 * <p>Classe utilitaire : constructeur privé, méthodes statiques — planifier
 * ne « possède » aucun réseau, c'est une opération sur un réseau qu'on lui
 * prête. (Même discipline que {@code VehicleFactory} du module 30.)
 *
 * @author Emmanuel Bruno
 * @see Network
 * @see Station
 */
public final class TripPlanner {

    /**
     * Construit un chemin vide (le retour d'échec de {@link #planifier}).
     *
     * @return une nouvelle {@code List} vide — jamais {@code null}
     */
    private static List<Station> cheminVide() {
        return new ArrayList<>();
    }

    /**
     * Outil de la reconstruction : retourne une NOUVELLE liste contenant les
     * mêmes éléments, dans l'ordre inverse.
     *
     * <p>Écrite en générique {@code <T>} pour la leçon : l'algorithme
     * d'inversion ne dépend PAS de ce que la liste contient — {@code T} est
     * un blanc-seing que l'appelant remplit ({@code Station} ici, n'importe
     * quoi d'autre ailleurs). C'est exactement le sens de la généricité des
     * collections : le comportement est écrit une fois, les types vérifiés à
     * la compilation, sans un seul cast.
     *
     * @param <T>    le type des éléments
     * @param entree la liste à inverser (non modifiée)
     * @return une copie inversée
     */
    private static <T> List<T> inverser(List<T> entree) {
        List<T> sortie = new ArrayList<>();
        for (int i = entree.size() - 1; i >= 0; i--) {
            sortie.add(entree.get(i));
        }
        return sortie;
    }

    /** Constructeur privé : classe utilitaire, jamais instanciable. */
    private TripPlanner() {
        throw new AssertionError("TripPlanner est une classe utilitaire : pas d'instanciation.");
    }

    /**
     * Calcule le chemin le plus court (en arrêts) entre deux stations.
     *
     * <p>Retourne une liste VIDE quand aucun chemin n'existe — contrat
     * documenté. Commentaire pour la suite : le module {@code 50-exceptions}
     * remplacera ce « vide vaut échec » par une exception métier explicite ;
     * ici, vide-vs-introuvable est encore une ambiguïté assumée de la semaine.
     *
     * @param network le réseau à explorer
     * @param depart  station de départ (doit exister dans le réseau)
     * @param arrivee station d'arrivée (doit exister dans le réseau)
     * @return le chemin, départ inclus, arrivée incluse ; liste vide si
     *         impossible ; un singleton {@code [X]} si départ = arrivée
     * @throws IllegalArgumentException si le réseau est {@code null}
     */
    public static List<Station> planifier(Network network, Station depart, Station arrivee) {
        if (network == null) {
            throw new IllegalArgumentException("Impossible de planifier sans réseau.");
        }
        if (depart == null || arrivee == null) {
            return cheminVide();
        }
        // Les deux extrémités doivent être des stations connues du réseau.
        // Set.contains par equals : une autre instance de "GAR" suffit.
        Set<Station> connues = network.tousLesArrets();
        if (!connues.contains(depart) || !connues.contains(arrivee)) {
            return cheminVide();
        }
        if (depart.equals(arrivee)) {
            List<Station> allerSimple = new ArrayList<>();
            allerSimple.add(depart);
            return allerSimple;
        }

        Queue<Station> aExplorer = new LinkedList<>(); // LinkedList en rôle Queue : FIFO explicite
        Set<Station> visites = new HashSet<>();
        Map<Station, Station> predecesseurs = new HashMap<>();

        aExplorer.add(depart);
        visites.add(depart);

        boolean atteinte = false;
        while (!aExplorer.isEmpty()) {
            Station courante = aExplorer.poll(); // sors la plus anciennement découverte (FIFO)
            if (courante.equals(arrivee)) {
                atteinte = true;
                break;
            }
            for (Station voisin : voisinsDe(network, courante)) {
                if (!visites.contains(voisin)) {
                    visites.add(voisin);
                    predecesseurs.put(voisin, courante); // le fil d'Ariane : « qui m'a découverte ? »
                    aExplorer.add(voisin);
                }
            }
        }

        if (!atteinte) {
            return cheminVide(); // réseau débranché entre les deux points — vide = échec
        }

        // Reconstruction À L'ENVERS : de l'arrivée vers le départ via les
        // prédécesseurs, puis inversion générique. depart est sa propre
        // racine : predecesseurs.get(depart) vaut null, la boucle s'arrête là.
        List<Station> aEnvers = new ArrayList<>();
        Station etape = arrivee;
        while (etape != null) {
            aEnvers.add(etape);
            etape = predecesseurs.get(etape);
        }
        return inverser(aEnvers);
    }

    /**
     * Les stations adjacentes directes : le prédécesseur et le successeur de
     * {@code s} sur chacune des lignes qui la desservent.
     *
     * <p>Deux exercices de {@code List} déguisés : {@code indexOf} (qui
     * compare par {@code equals}, encore lui) pour situer la station dans le
     * parcours, puis {@code get(i ± 1)} pour lire les voisins — l'accès par
     * position, la spécialité de la liste ordonnée. Une station de
     * correspondance voit s'additionner les voisins de chaque ligne : c'est
     * ainsi que le BFS « change de ligne » sans le savoir.
     *
     * @param network le réseau
     * @param s       la station à rayonner
     * @return les stations directement atteignables en un arrêt (peut contenir
     *         des doublons si deux lignes se suivent — le Set de visites les avale)
     */
    private static List<Station> voisinsDe(Network network, Station s) {
        List<Station> voisins = new ArrayList<>();
        for (Line ligne : network.lignesA(s)) {
            List<Station> arrets = ligne.getArrets();
            int position = arrets.indexOf(s); // equals, encore et toujours
            if (position < 0) {
                continue; // ne devrait pas arriver : la ligne est indexée sur ses arrêts
            }
            if (position > 0) {
                voisins.add(arrets.get(position - 1));
            }
            if (position + 1 < arrets.size()) {
                voisins.add(arrets.get(position + 1));
            }
        }
        return voisins;
    }
}
