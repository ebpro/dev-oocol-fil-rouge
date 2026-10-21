package fr.ebruno.dev.oocol.collections.domain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Le graphe du réseau : quelle station est desservie par quelles lignes.
 *
 * <p><strong>Le conteneur-vedette de la séance : la {@code Map}.</strong>
 * {@code Line.trouverParCode} montrait le lookup {@code O(n)} d'un parcours
 * manuel ; ici, la question « quelles lignes passent par cette station ? »
 * se pose des milliers de fois par seconde dans une vraie application
 * d'exploitation. La réponse est une table d'adjacence :
 *
 * <pre>
 * Map&lt;Station, Set&lt;Line&gt;&gt; correspondances
 * </pre>
 *
 * <p>Lisibilité de ce type : la CLÉ est une station, la VALEUR est
 * l'<em>ensemble</em> des lignes qui la desservent. Deux généricités
 * imbriquées, chacune choisie pour son usage : {@code Map} pour le lookup
 * par clé, {@code Set} parce qu'une ligne ne dessert pas deux fois la même
 * station — l'anti-doublon est la sémantique même de {@code Set}.
 *
 * <p>Et surtout : une {@code HashMap} retrouve une clé par
 * {@code hashCode} + {@code equals}. Tout le réseau repose donc sur le
 * contrat de {@link Station} — sans lui, deux instances de {@code "GAR"}
 * créeraient DEUX entrées distinctes et le graphe se fendrait en deux
 * moitiés incohérentes. Le test
 * {@code NetworkTest#correspondanceRetrouveeParAutreInstance} joue exactement
 * cette scène.
 *
 * @author Emmanuel Bruno
 * @see Station
 * @see Line
 * @see TripPlanner
 */
public class Network {

    /**
     * Table d'adjacence station → lignes qui la desservent.
     * Clé et valeur sont des collections de hachage : le contrat
     * {@code equals}/{@code hashCode} de {@link Station} est la condition de
     * correction de toute la classe.
     */
    private final Map<Station, Set<Line>> correspondances = new HashMap<>();

    /** Toutes les lignes du réseau, dans l'ordre d'ajout (accès par index utile aux stats). */
    private final List<Line> lignes = new ArrayList<>();

    /**
     * Enregistre une ligne et met à jour la table d'adjacence pour chacun de
     * ses arrêts.
     *
     * @param ligne la ligne à ajouter, non {@code null} et avec au moins un arrêt
     * @throws IllegalArgumentException si la ligne est {@code null} ou sans arrêt
     */
    public void ajouterLigne(Line ligne) {
        if (ligne == null) {
            throw new IllegalArgumentException("Impossible d'ajouter une ligne nulle au réseau.");
        }
        if (ligne.nombreArrets() == 0) {
            throw new IllegalArgumentException("La ligne " + ligne.getNom() + " n'a aucun arrêt.");
        }
        lignes.add(ligne);
        for (Station s : ligne.getArrets()) {
            // Pas de computeIfAbsent (lambda réservé au module 80) : la version
            // longue dit exactement la même chose — « si la clé est nouvelle,
            // créer son ensemble ; sinon lui adjoindre la ligne ».
            Set<Line> dessert = correspondances.get(s); // lookup O(1) — la promesse de la Map
            if (dessert == null) {
                dessert = new HashSet<>();
                correspondances.put(s, dessert);
            }
            dessert.add(ligne); // Set : ajouter deux fois la même ligne ne change rien
        }
    }

    /**
     * Les lignes qui desservent une station.
     *
     * <p>Pattern {@code Collections.emptySet()} : plutôt que {@code null}
     * pour une station inconnue, un ensemble VIDE — l'appelant itère sans
     * test défensif, et l'absence garde sa forme (« aucune ligne » se parcourt,
     * {@code null} se crash).
     *
     * @param s la station interrogée
     * @return l'ensemble (en lecture seule) des lignes desservant {@code s} ;
     *         vide si le réseau ne la connaît pas
     */
    public Set<Line> lignesA(Station s) {
        Set<Line> dessert = correspondances.get(s); // equals/hashCode de Station au travail
        if (dessert == null) {
            return Collections.<Line>emptySet();
        }
        return Collections.unmodifiableSet(dessert);
    }

    /**
     * Toutes les stations du réseau, sans doublon.
     *
     * <p>Leçon {@code Set} en acte : une station présente sur trois lignes
     * apparaît trois fois dans les listes d'arrêts ; l'union dans un
     * {@code HashSet} ne la garde qu'une fois — parce que le Set consulte
     * {@code equals} avant d'accepter. Retourner un {@code List} eût été
     * mentir sur la sémantique (« la collection des stations » n'a ni ordre
     * ni raison d'avoir des doublons).
     *
     * @return un ensemble (en lecture seule) de toutes les stations desservies
     */
    public Set<Station> tousLesArrets() {
        Set<Station> ensemble = new HashSet<>();
        for (Line l : lignes) {
            for (Station s : l.getArrets()) {
                ensemble.add(s); // doublons absorbés par equals — c'est le travail du Set
            }
        }
        return Collections.unmodifiableSet(ensemble);
    }

    /**
     * Retrouve une station depuis son code, en balayant le réseau.
     *
     * <p>Retourne {@code null} si inconnue — contrat documenté. Commentaire
     * pour la suite : le module {@code 80-modern-java} remplacera ce
     * {@code null} par un {@code Optional<Station>}, qui encode l'absence
     * dans le TYPE de retour au lieu d'une convention dans le Javadoc.
     *
     * @param code le code cherché (ex. {@code "GAR"})
     * @return la station, ou {@code null} si le réseau ne la dessert pas
     */
    public Station trouverStationParCode(String code) {
        for (Station s : tousLesArrets()) {
            if (s.getCode().equals(code)) {
                return s;
            }
        }
        return null;
    }

    /**
     * Toutes les lignes du réseau, dans l'ordre d'ajout.
     *
     * @return une vue (en lecture seule) de la liste des lignes
     */
    public List<Line> getLignes() {
        return Collections.unmodifiableList(lignes);
    }

    /**
     * Nombre de stations de correspondance : celles desservies par plus d'une ligne.
     *
     * <p>Itération sur {@code entrySet()} : on parcourt les PAIRES clé→valeur
     * d'une Map sans jamais manipuler de lambda — la syntaxe d'itération des
     * tables, avant {@code forEach} et les streams.
     *
     * @return le nombre de clés dont l'ensemble de lignes compte au moins deux lignes
     */
    public int nombreCorrespondances() {
        int total = 0;
        for (Map.Entry<Station, Set<Line>> entree : correspondances.entrySet()) {
            if (entree.getValue().size() > 1) {
                total++;
            }
        }
        return total;
    }

    /**
     * Résumé lisible du réseau.
     *
     * @return ex. {@code "Réseau Trans'U — 3 lignes, 7 stations, 2 correspondances"}
     */
    @Override
    public String toString() {
        return "R\u00E9seau Trans'U \u2014 " + lignes.size() + " lignes, "
                + tousLesArrets().size() + " stations, "
                + nombreCorrespondances() + " correspondances";
    }
}
