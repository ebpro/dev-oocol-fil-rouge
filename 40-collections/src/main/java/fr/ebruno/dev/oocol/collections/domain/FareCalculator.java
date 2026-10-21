package fr.ebruno.dev.oocol.collections.domain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Le calcul du prix d'un trajet : la zone la plus profonde traversée décide
 * du tarif — et la classe devient le support des DEUX façons de trier une
 * collection.
 *
 * <p><strong>Trier, deux philosophies :</strong>
 * <ul>
 *   <li>{@link #trierParNumero(List)} applique l'ordre <em>naturel</em> de
 *       {@link Zone} ({@code Comparable}) : {@code Collections.sort(liste)},
 *       un seul argument — la liste se range « comme elle sait le faire » ;</li>
 *   <li>{@link #trierParNom(List)} passe un ordre <em>délégué</em> :
 *       {@code Collections.sort(liste, comparateur)} — le patron
 *       <strong>Strategy</strong> sans le nommer : la stratégie de tri est un
 *       objet {@code Comparator} fourni par l'appelant, écrit ici en classe
 *       interne anonyme (le module 80 le remplacera par un lambda, une
 *       ligne). {@code Zone} reste inchangée : on peut trier ses zones par
 *       n'importe quel critère sans jamais toucher à sa classe — c'est ça,
 *       l'apport du Comparator.</li>
 * </ul>
 *
 * <p>Le tarif lui-même vit dans une {@code Map<String, Double>} : clé =
 * numéro de zone en texte ({@code "1"}, {@code "2"}…), valeur = prix. Le
 * lookup du tarif est donc le même geste {@code O(1)} que l'adjacence de
 * {@link Network} — la Map répétée dans deux contextes différents, pour
 * qu'elle devienne un réflexe et pas une curiosité.
 *
 * @author Emmanuel Bruno
 * @see Zone
 * @see Network
 */
public class FareCalculator {

    /** Zones connues du calculateur, rangées par numéro (ordre naturel de Zone). */
    private final List<Zone> zones = new ArrayList<Zone>();

    /** Table des tarifs : numéro de zone en clé texte → prix en euros. */
    private final Map<String, Double> tarifs = new HashMap<String, Double>();

    /**
     * Construit un calculateur à partir des zones et des tarifs donnés.
     *
     * <p>Les collections reçues sont COPIÉES puis triées : le calculateur ne
     * devient jamais le otage de la liste d'un appelant qui la modifierait
     * après coup (la leçon d'encapsulation de W3, appliquée aux conteneurs —
     * un getter qui expose la liste interne annule toute l'histoire).
     *
     * @param zonesInitiales  les zones du réseau, non vides
     * @param tarifsInitiaux  la table numéro de zone → prix, non nulle
     * @throws IllegalArgumentException si les arguments sont absents ou vides
     */
    public FareCalculator(List<Zone> zonesInitiales, Map<String, Double> tarifsInitiaux) {
        if (zonesInitiales == null || zonesInitiales.isEmpty()) {
            throw new IllegalArgumentException("Un calculateur de tarifs connaît au moins une zone.");
        }
        if (tarifsInitiaux == null) {
            throw new IllegalArgumentException("Un calculateur de tarifs reçoit une table de tarifs.");
        }
        this.zones.addAll(zonesInitiales);
        Collections.sort(this.zones); // ordre naturel via Zone.compareTo — la ligne la plus « ELO 06 » du fichier
        this.tarifs.putAll(tarifsInitiaux);
    }

    /**
     * Range une liste de zones selon leur ORDRE NATUREL (numéro croissant).
     *
     * <p>{@code Collections.sort(liste)} : un seul argument — le tri demande
     * aux éléments comment ils se rangent. Exige que le type implémente
     * {@code Comparable} ; compiler une liste d'éléments non comparables est
     * une erreur de compilation… ou presque (le bornage {@code <T extends
     * Comparable<? super T>>} fait le travail — la généricité avancée, on la
     * nomme sans l'exiger cette semaine).
     *
     * @param liste la liste à trier EN PLACE (les listes triées sont des
     *              {@code ArrayList} mutables — pas une vue {@code unmodifiable})
     */
    public static void trierParNumero(List<Zone> liste) {
        Collections.sort(liste);
    }

    /**
     * Range une liste de zones par NOM alphabétique, casse ignorée.
     *
     * <p>{@code Collections.sort(liste, comparateur)} : le deuxième argument
     * est un OBJET stratégie fourni par l'appelant. Écrit en classe interne
     * anonyme — pas de lambda dans ce module : le {@code new Comparator}
     * explicite garde visible le fait que la stratégie est un objet comme un
     * autre, passé en paramètre.
     *
     * @param liste la liste à trier EN PLACE
     */
    public static void trierParNom(List<Zone> liste) {
        Collections.sort(liste, new Comparator<Zone>() {
            @Override
            public int compare(Zone a, Zone b) {
                return a.getNom().compareToIgnoreCase(b.getNom());
            }
        });
    }

    /**
     * Prix d'un trajet : le tarif de la zone la plus profonde traversée.
     *
     * <p>Le « maximum » se cherche par parcours de {@code List} — un scan
     * {@code O(n)} sur la liste des zones du trajet (quelques unités) : le
     * coût est choisi, pas subi ; trier pour un max eût été du gaspillage.
     *
     * @param zonesTraversees les zones parcourues par le trajet, non vides
     * @return le prix en euros de la bande tarifaire correspondante
     * @throws IllegalArgumentException si la liste est vide/null, ou si le
     *         réseau n'a pas de tarif pour la zone maximale atteinte
     */
    public double calculerPrix(List<Zone> zonesTraversees) {
        if (zonesTraversees == null || zonesTraversees.isEmpty()) {
            throw new IllegalArgumentException("Un trajet traverse au moins une zone.");
        }
        int plusProfonde = 0;
        for (Zone z : zonesTraversees) {
            if (z.getNumero() > plusProfonde) {
                plusProfonde = z.getNumero();
            }
        }
        Double prix = tarifs.get(String.valueOf(plusProfonde)); // lookup Map par clé texte
        if (prix == null) {
            throw new IllegalArgumentException(
                    "Aucun tarif connu pour la zone " + plusProfonde + " — le réseau est mal réglé.");
        }
        return prix.doubleValue(); // déballage auto de la Double (le « boxing » des collections de types primitifs)
    }

    /**
     * @return les zones connues, rangées par numéro (vue en lecture seule)
     */
    public List<Zone> getZones() {
        return Collections.unmodifiableList(zones);
    }

    /**
     * Résumé lisible du barème.
     *
     * @return ex. {@code "Barème 2 zones : {1=2.0, 2=3.5}"}
     */
    @Override
    public String toString() {
        return "Bar\u00E8me " + zones.size() + " zones : " + tarifs;
    }
}
