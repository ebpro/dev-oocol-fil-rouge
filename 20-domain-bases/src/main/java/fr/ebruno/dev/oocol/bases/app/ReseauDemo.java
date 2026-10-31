package fr.ebruno.dev.oocol.bases.app;

import fr.ebruno.dev.oocol.bases.domain.Line;
import fr.ebruno.dev.oocol.bases.domain.Station;
import fr.ebruno.dev.oocol.bases.domain.Ticket;
import fr.ebruno.dev.oocol.bases.domain.Zone;

/**
 * Démonstration exécutable du module 20 : un mini-réseau « Trans'U » construit
 * avec les quatre classes du paquet {@code domain}.
 *
 * <p>Objectif : voir le code <em>se comporter</em> comme le promet la
 * documentation — objets immuables, compteurs statiques, validation qui
 * rejette les données invalides. Exécution :
 *
 * <pre>{@code
 * ./mvnw -pl 20-domain-bases -am package
 * java -cp 20-domain-bases/target/classes fr.ebruno.dev.oocol.bases.app.ReseauDemo
 * }</pre>
 *
 * <p>Lisez le code de haut en bas : chaque bloc {@code println} est un
 * chapitre (zones → stations → lignes → tickets → compteurs → validation).
 *
 * @author Emmanuel Bruno
 */
public final class ReseauDemo {

    /**
     * Classe utilitaire (point d'entrée {@code main} seul) : instantiation
     * interdite — même motif que {@code TransitHello} au module 00.
     */
    private ReseauDemo() {
        throw new AssertionError("Classe utilitaire : ne s'instancie pas.");
    }

    /**
     * Construit le mini-réseau et affiche chaque notion démontrée.
     *
     * @param args non utilisés
     */
    public static void main(String[] args) {
        // ------------------------------------------------------------------
        // 1. DES ZONES — objets immuables grâce à `final` (aucun setter).
        // ------------------------------------------------------------------
        System.out.println("=== 1. Des zones tarifaires (attributs final = immuables) ===");
        Zone zone1 = new Zone(1, "Centre-ville");
        Zone zone2 = new Zone(2, "Périphérie");
        System.out.println(zone1);
        System.out.println(zone2);

        // ------------------------------------------------------------------
        // 2. DES STATIONS — encapsulation + surcharge de constructeurs.
        //    Deux constructeurs : (nom, code, zone) et (nom, code, zone, accessible).
        //    Le premier délègue au second avec accessible = false.
        // ------------------------------------------------------------------
        System.out.println();
        System.out.println("=== 2. Des stations (constructeurs surchargés, validation du code) ===");
        Station gare = new Station("Gare Centrale", "GAR", zone1, true);  // version complète
        Station universite = new Station("Université", "UNI", zone2);     // version courte → non accessible
        Station hopital = new Station("Hôpital", "HOP", zone1, true);     // version complète
        System.out.println(gare);
        System.out.println(universite);
        System.out.println(hopital);
        System.out.println("La gare est-elle accessible PMR ? " + gare.estAccessible()
                + " — L'université ? " + universite.estAccessible());

        // ------------------------------------------------------------------
        // 3. DES LIGNES — composition : une Ligne « tient » deux Stations.
        //    Limite connue : deux extrémités seulement, pas d'arrêts
        //    intermédiaires — une collection d'arrêts règlerait ça.
        // ------------------------------------------------------------------
        System.out.println();
        System.out.println("=== 3. Des lignes (composition, valeur par défaut 15 min) ===");
        Line ligneA = new Line("Ligne A", gare, universite);              // fréquence par défaut : 15 min
        Line ligneB = new Line("Ligne B", gare, hopital, 8);              // fréquence personnalisée : 8 min
        System.out.println(ligneA);
        System.out.println(ligneB);
        System.out.println("La ligne A dessert-elle directement l'Hôpital ? " + ligneA.estDirecte(hopital));
        System.out.println("La ligne B dessert-elle directement la Gare ?   " + ligneB.estDirecte(gare));
        // estDirecte compare par CODE : une autre instance « GAR » (créée ailleurs) compte comme la gare.
        Station gareBis = new Station("Gare Centrale (autre objet)", "GAR", zone1);
        System.out.println("La ligne A et un AUTRE objet Station de code GAR ? " + ligneA.estDirecte(gareBis)
                + "  (comparaison par code, pas par référence)");

        // ------------------------------------------------------------------
        // 4. DES TICKETS — fabriques statiques : le constructeur est privé,
        //    on écrit Ticket.jour(zone), pas new Ticket("Journée", 5.20, zone).
        // ------------------------------------------------------------------
        System.out.println();
        System.out.println("=== 4. Des tickets (fabriques statiques, constructeur privé) ===");
        Ticket tUnite = Ticket.unite(zone1);
        Ticket tJour = Ticket.jour(zone1);
        Ticket tScolaire = Ticket.scolaire(zone2);
        System.out.println(tUnite);
        System.out.println(tJour);
        System.out.println(tScolaire);
        System.out.println("Le ticket Unité est-il valide ? " + tUnite.estValide()
                + "  (W8 ajoutera une date d'expiration)");

        // ------------------------------------------------------------------
        // 5. LES COMPTEURS STATIQUES — des chiffres de CLASSE, pas d'objet :
        //    ils s'appellent sur la classe (Station.nombreStations()).
        //    Attention : gareBis a été créée aussi → la démo compte 4 stations.
        // ------------------------------------------------------------------
        System.out.println();
        System.out.println("=== 5. Compteurs statiques (une statistique pour toute la classe) ===");
        System.out.println("Stations créées dans ce programme : " + Station.nombreStations());
        System.out.println("Lignes créées dans ce programme   : " + Line.nombreLignes());

        // ------------------------------------------------------------------
        // 6. LA VALIDATION À L'OEUVRE — le code « gar » (minuscules) est
        //    rejeté À LA CRÉATION, pas trois semaines plus tard.
        //    (Le try/catch n'est PAS le sujet du module — module 50 pour les
        //    exceptions. Ici, une seule ligne pour prouver que la garde tient.)
        // ------------------------------------------------------------------
        System.out.println();
        System.out.println("=== 6. La validation rejette une donnée invalide à la source ===");
        try {
            new Station("Fausse Gare", "gar", zone1); // « gar » : minuscules → interdit
            System.out.println("BOUGUÉ : cette ligne ne devrait jamais s'afficher.");
        } catch (IllegalArgumentException e) {
            System.out.println("Création refusée, comme prévu : " + e.getMessage());
        }

        System.out.println();
        System.out.println("Fin de la démonstration — relisez chaque classe du paquet domain pour");
        System.out.println("retrouver le code qui produit exactement ces lignes.");
    }
}
