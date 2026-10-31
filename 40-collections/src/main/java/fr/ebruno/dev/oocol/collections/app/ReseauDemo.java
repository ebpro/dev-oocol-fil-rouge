package fr.ebruno.dev.oocol.collections.app;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import fr.ebruno.dev.oocol.collections.domain.FareCalculator;
import fr.ebruno.dev.oocol.collections.domain.Line;
import fr.ebruno.dev.oocol.collections.domain.Network;
import fr.ebruno.dev.oocol.collections.domain.Station;
import fr.ebruno.dev.oocol.collections.domain.TripPlanner;
import fr.ebruno.dev.oocol.collections.domain.Zone;

/**
 * Démonstration des collections appliquées au réseau Trans'U.
 *
 * <p>Six scènes :
 * <ol>
 *   <li>construction — la paire de champs devient une {@code List<Station>} ;</li>
 *   <li>equals/hashCode — le bug motivateur, joué puis résolu ;</li>
 *   <li>TripPlanner — l'itinéraire le plus court par BFS sur une Queue ;</li>
 *   <li>correspondances — la Map d'adjacence qui répond en O(1) ;</li>
 *   <li>tris — ordre naturel (Comparable) puis ordre délégué (Comparator) ;</li>
 *   <li>tarifs — la zone la plus profonde traversée décide du prix.</li>
 * </ol>
 *
 * <p>Aucun lambda, aucun stream, aucun {@code Optional} : syntaxe
 * volontairement antérieure au module {@code 80-modern-java}, pour que la
 * version moderne se mesure en diff. Exécutable avec :
 *
 * <pre>
 * java -cp 40-collections/target/classes fr.ebruno.dev.oocol.collections.app.ReseauDemo
 * </pre>
 *
 * @author Emmanuel Bruno
 */
public final class ReseauDemo {

    /** Constructeur privé : classe exécutable, jamais instanciée. */
    private ReseauDemo() {
        throw new AssertionError("Point d'entr\u00E9e : pas d'instanciation.");
    }

    /**
     * Fil de la démonstration.
     *
     * @param args ignorés
     */
    public static void main(String[] args) {
        Zone zoneCentre = new Zone(1, "Centre-ville");
        Zone zonePeripherie = new Zone(2, "P\u00E9riph\u00E9rie");

        // Sept stations, deux zones. Les codes sont triés « à la main » pour
        // que la scène 5 puisse montrer un ordre par code DIFFÉRENT de
        // l'ordre par nom : le Stade Olympique s'appelle OID, le Parc des
        // Expositions PAR — deux lettres d'avance l'un sur l'autre selon
        // le critère choisi.
        Station aer = Station.of("AER", "Aéroport", zonePeripherie);
        Station gar = Station.of("GAR", "Gare Centrale", zoneCentre);
        Station hop = Station.of("HOP", "Hôpital", zonePeripherie);
        Station mar = Station.of("MAR", "Marché", zoneCentre);
        Station oid = Station.of("OID", "Stade Olympique", zonePeripherie);
        Station par = Station.of("PAR", "Parc des Expositions", zonePeripherie);
        Station uni = Station.of("UNI", "Université", zoneCentre);

        Network reseau = construireReseau(aer, gar, hop, mar, oid, par, uni);

        sceneConstruction(reseau);
        sceneEqualsHashCode(gar);
        sceneItineraire(reseau, gar, aer);
        sceneCorrespondances(reseau);
        sceneTris(reseau);
        sceneTarifs(zoneCentre, zonePeripherie);
    }

    /**
     * Assemble le réseau de démonstration : trois lignes, sept stations,
     * deux correspondances (GAR et HOP).
     *
     * @param aer l'Aéroport
     * @param gar la Gare Centrale
     * @param hop l'Hôpital
     * @param mar le Marché
     * @param oid le Stade Olympique
     * @param par le Parc des Expositions
     * @param uni l'Université
     * @return le réseau complet
     */
    private static Network construireReseau(Station aer, Station gar, Station hop, Station mar,
                                            Station oid, Station par, Station uni) {
        Line ligneA = new Line("Ligne A");
        ligneA.ajouterArret(gar);
        ligneA.ajouterArret(mar);
        ligneA.ajouterArret(hop);

        Line ligneB = new Line("Ligne B");
        ligneB.ajouterArret(gar);
        ligneB.ajouterArret(uni);
        ligneB.ajouterArret(par);

        Line ligneC = new Line("Ligne C");
        ligneC.ajouterArret(hop);
        ligneC.ajouterArret(oid);
        ligneC.ajouterArret(aer);

        Network reseau = new Network();
        reseau.ajouterLigne(ligneA);
        reseau.ajouterLigne(ligneB);
        reseau.ajouterLigne(ligneC);
        return reseau;
    }

    /**
     * Scène 1 — la ligne n'est plus une paire : c'est une liste ordonnée.
     *
     * @param reseau le réseau à décrire
     */
    private static void sceneConstruction(Network reseau) {
        section("Scène 1 — Construction : W3 (paire de champs) \u2192 W5 (List<Station>)");
        for (Line l : reseau.getLignes()) {
            System.out.println(l);
        }
        Line premiere = reseau.getLignes().get(0); // accès par index : la spécialité de la List
        System.out.println("Premier arrêt de la " + premiere.getNom() + " : " + premiere.premierArret().getNom());
        System.out.println("Dernier arrêt : " + premiere.dernierArret().getNom()
                + " \u2014 la liste se lit des deux bouts.");
    }

    /**
     * Scène 2 — equals/hashCode : le bug qui motive le contrat.
     *
     * @param gar la station Gare Centrale de référence
     */
    private static void sceneEqualsHashCode(Station gar) {
        section("Scène 2 — equals/hashCode : « la même » station sous deux exemplaires");
        System.out.println(Line.demontrerBugSansEquals());

        // La scène grandeur nature : un HashSet de stations.
        Set<Station> desservies = new HashSet<Station>();
        desservies.add(gar);
        Station reCreee = Station.of("GAR", "Gare Centrale (re-créée)", gar.getZone());
        System.out.println("HashSet.contains(autre instance de \"GAR\") : " + desservies.contains(reCreee));
        System.out.println("\u2014 sans equals/hashCode par code : false. Avec : true. C'est tout le contrat.");
    }

    /**
     * Scène 3 — le TripPlanner BFS : l'ondulation qui touche l'arrivée.
     *
     * @param reseau le réseau à explorer
     * @param depart la station de départ
     * @param arrivee la station d'arrivée
     */
    private static void sceneItineraire(Network reseau, Station depart, Station arrivee) {
        section("Scène 3 — TripPlanner : parcours en largeur (Queue + Set + Map + List)");
        System.out.println("Question : comment aller de " + depart.getCode() + " (" + depart.getNom()
                + ") à " + arrivee.getCode() + " (" + arrivee.getNom() + ") ?");
        List<Station> chemin = TripPlanner.planifier(reseau, depart, arrivee);
        StringBuilder affichage = new StringBuilder("Chemin : ");
        for (Station s : chemin) {
            affichage.append(s.getCode()).append(" (").append(s.getNom()).append(") \u2192 ");
        }
        System.out.println(affichage.substring(0, affichage.length() - 3)
                + "  [soit " + chemin.size() + " stations]");

        Station inconnue = Station.of("XXX", "Bout-du-Monde", depart.getZone());
        List<Station> sansChemin = TripPlanner.planifier(reseau, depart, inconnue);
        System.out.println("Vers une station hors réseau : " + sansChemin
                + " \u2014 liste vide, pas d'exception (W6 corrigera le contrat).");
    }

    /**
     * Scène 4 — les correspondances : la Map d'adjacence au travail.
     *
     * @param reseau le réseau à interroger
     */
    private static void sceneCorrespondances(Network reseau) {
        section("Scène 4 — Correspondances : quelles lignes passent ici ? (Map, O(1))");
        for (Station s : reseau.tousLesArrets()) {
            Set<Line> lignesIci = reseau.lignesA(s);
            if (lignesIci.size() > 1) {
                StringBuilder noms = new StringBuilder();
                for (Line l : lignesIci) {
                    noms.append(l.getNom()).append(", ");
                }
                System.out.println(s.getCode() + " (" + s.getNom() + ") : "
                        + noms.substring(0, noms.length() - 2) + " \u2014 correspondance !");
            }
        }
        System.out.println("Total des correspondances : " + reseau.nombreCorrespondances());
        System.out.println("Résumé : " + reseau);
    }

    /**
     * Scène 5 — les deux façons de trier : naturel vs délégué.
     *
     * @param reseau le réseau dont on trie les stations
     */
    private static void sceneTris(Network reseau) {
        section("Scène 5 — Trier : ordre naturel (Comparable) puis ordre délégué (Comparator)");

        // Le Set n'a pas d'ordre ; la List, si — on copie pour trier.
        List<Station> parCode = new ArrayList<Station>(reseau.tousLesArrets());
        Collections.sort(parCode); // le tri demande aux stations comment elles se rangent
        System.out.println("Par code (ordre naturel) : " + codes(parCode));

        List<Station> parNom = new ArrayList<Station>(reseau.tousLesArrets());
        Collections.sort(parNom, Station.parNom()); // la stratégie est UN OBJET passé en paramètre
        System.out.println("Par nom (Comparator anonyme) : " + noms(parNom));
        System.out.println("OID « Stade Olympique » et PAR « Parc des Expositions » ont échangé leurs places :");
        System.out.println("le critère de tri vient de DEHORS la station — Zone/Station n'ont pas été modifiées.");
    }

    /**
     * Scène 6 — le barème : la Map clé texte → prix, et le max d'une liste.
     *
     * @param zoneCentre la zone 1
     * @param zonePeripherie la zone 2
     */
    private static void sceneTarifs(Zone zoneCentre, Zone zonePeripherie) {
        section("Scène 6 — FareCalculator : le prix suit la zone la plus profonde traversée");

        List<Zone> univers = new ArrayList<Zone>();
        univers.add(zonePeripherie); // mise en désordre exprès : le constructeur trie
        univers.add(zoneCentre);
        Map<String, Double> bareme = new HashMap<String, Double>();
        bareme.put("1", Double.valueOf(2.00));
        bareme.put("2", Double.valueOf(3.50));

        FareCalculator calculateur = new FareCalculator(univers, bareme);
        System.out.println("Zones connues, triées à la construction : " + calculateur.getZones());
        System.out.println("Le barème : " + calculateur);

        List<Zone> trajetCentre = new ArrayList<Zone>();
        trajetCentre.add(zoneCentre);
        System.out.println("Trajet 100 % zone 1 : " + calculateur.calculerPrix(trajetCentre) + " \u20AC");

        List<Zone> trajetTraversant = new ArrayList<Zone>();
        trajetTraversant.add(zoneCentre);
        trajetTraversant.add(zonePeripherie);
        System.out.println("Trajet zone 1 \u2192 zone 2 : " + calculateur.calculerPrix(trajetTraversant)
                + " \u20AC (c'est la zone la plus profonde qui paie)");

        List<Zone> enVrac = new ArrayList<Zone>();
        enVrac.add(zonePeripherie);
        enVrac.add(zoneCentre);
        FareCalculator.trierParNumero(enVrac);
        System.out.println("trierParNumero (ordre naturel) : " + enVrac);
        FareCalculator.trierParNom(enVrac);
        System.out.println("trierParNom   (Comparator)    : " + enVrac);
    }

    /**
     * Titre de section, même gabarit pour les six scènes.
     *
     * @param titre le libellé de la scène
     */
    private static void section(String titre) {
        System.out.println();
        System.out.println("=== " + titre + " ===");
    }

    /**
     * Les codes d'une liste de stations, séparés par « &lt; » pour visualiser l'ordre.
     *
     * @param stations la liste triée à décrire
     * @return ex. {@code "AER < GAR < HOP"}
     */
    private static String codes(List<Station> stations) {
        StringBuilder sortie = new StringBuilder();
        for (Station s : stations) {
            sortie.append(s.getCode()).append(" < ");
        }
        return sortie.substring(0, sortie.length() - 3);
    }

    /**
     * Les noms d'une liste de stations, séparés par « &lt; ».
     *
     * @param stations la liste triée à décrire
     * @return ex. {@code "Aéroport < Gare Centrale"}
     */
    private static String noms(List<Station> stations) {
        StringBuilder sortie = new StringBuilder();
        for (Station s : stations) {
            sortie.append(s.getNom()).append(" < ");
        }
        return sortie.substring(0, sortie.length() - 3);
    }
}
