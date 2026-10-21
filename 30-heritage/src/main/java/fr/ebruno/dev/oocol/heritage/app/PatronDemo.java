package fr.ebruno.dev.oocol.heritage.app;

import fr.ebruno.dev.oocol.heritage.domain.Bus;
import fr.ebruno.dev.oocol.heritage.domain.Capteur;
import fr.ebruno.dev.oocol.heritage.domain.Line;
import fr.ebruno.dev.oocol.heritage.domain.MetroTrain;
import fr.ebruno.dev.oocol.heritage.domain.Rechargeable;
import fr.ebruno.dev.oocol.heritage.domain.Tramway;
import fr.ebruno.dev.oocol.heritage.domain.Vehicle;
import fr.ebruno.dev.oocol.heritage.domain.VehicleFactory;

/**
 * Démonstration exécutable du module 30 : la hiérarchie {@link Vehicle} jouée
 * scène par scène — Factory, polymorphisme, composition/délégation,
 * interfaces (ISP), retour covariant et masquage de méthodes statiques.
 *
 * <p>Exécution :
 *
 * <pre>{@code
 * ./mvnw -pl 30-heritage -am package
 * java -cp 30-heritage/target/classes fr.ebruno.dev.oocol.heritage.app.PatronDemo
 * }</pre>
 *
 * <p>Lisez de haut en bas : chaque bloc est une scène pédagogique annoncée
 * par son entête {@code println}.
 *
 * @author Emmanuel Bruno
 */
public final class PatronDemo {

    /**
     * Classe utilitaire (point d'entrée {@code main} seul) : instantiation
     * interdite — même discipline que {@code ReseauDemo} au module 20.
     */
    private PatronDemo() {
        throw new AssertionError("Classe utilitaire : ne s'instancie pas.");
    }

    // ==================================================================
    // Auxiliaires de la scène 6 — MASQUAGE de méthodes statiques.
    // Deux classes imbriquées, uniquement pour la démo : elles ne font
    // PAS partie du domaine (jamais de piège de masquage dans domain/).
    // ==================================================================

    /** Scène 6 : classe de base portant une méthode statique {@code rapporter()}. */
    static class StatFlotte {

        /**
         * @return le rapport de la flotte entière
         */
        static String rapporter() {
            return "Flotte entière (tous véhicules)";
        }
    }

    /**
     * Scène 6 : sous-classe qui MASQUE {@code rapporter()} — le mot
     * « override » n'existe pas pour du {@code static} : la méthode du fils
     * cache celle du père selon le TYPE STATIQUE employé à l'appel, jamais
     * selon l'objet réel. Aucune polymorphie possible.
     */
    static class StatMetro extends StatFlotte {

        /**
         * Masque (n'écrase pas) {@link StatFlotte#rapporter()} : même
         * signature, résolution à la compilation par le nom de classe.
         *
         * @return le rapport des métros seulement
         */
        static String rapporter() {
            return "Métros uniquement";
        }
    }

    /**
     * Scène 2 : le service annonce un véhicule — SANS savoir lequel.
     *
     * <p>Méthode écrite une seule fois, appelée avec un métro, un bus, un
     * tramway : la substitution (LSP) garantit que tout {@code Vehicle}
     * convient, le polymorphisme garantit que chacun répond dans sa langue.
     * Aucun {@code instanceof}, aucune arborescence de types — le code qui
     * « connaît les espèces » est exactement ce que l'abstraction doit tuer.
     *
     * @param v le véhicule à annoncer (n'importe lequel)
     */
    private static void serviceAnnonce(Vehicle v) {
        System.out.println("  [serviceAnnonce] " + v.annoncer());
    }

    /**
     * Joue les six scènes du module et affiche chaque notion démontrée.
     *
     * @param args non utilisés
     */
    public static void main(String[] args) {
        // ------------------------------------------------------------------
        // 1. LA FABRIQUE — créer sans connaître les sous-classes.
        //    L'appelant écrit « metro », reçoit un Vehicle ; le switch de
        //    l'espèce concrète est enfermé dans VehicleFactory.
        // ------------------------------------------------------------------
        System.out.println("=== 1. Patron Factory : creer(type,...) rend un Vehicle, quel que soit l'enfant ===");
        Vehicle metro = VehicleFactory.creer("metro", "M-101", 180, 80.0);
        Vehicle bus = VehicleFactory.creer("bus", "B-202", 90, 50.0);
        Vehicle tram = VehicleFactory.creer("tram", "T-303", 210, 70.0);
        System.out.println("  creer(\"metro\") rend l'objet concret : " + metro.getClass().getSimpleName());
        System.out.println("  creer(\"bus\")   rend l'objet concret : " + bus.getClass().getSimpleName());
        System.out.println("  creer(\"tram\")  rend l'objet concret : " + tram.getClass().getSimpleName());
        System.out.println("  … mais les trois variables sont typées Vehicle (upcast implicite) :");
        System.out.println("  Vehicle metro = VehicleFactory.creer(\"metro\", ...); // le type STATIQUE est le parent");

        // ------------------------------------------------------------------
        // 2. LE POLYMORPHISME — une méthode, trois réponses.
        //    serviceAnnonce(Vehicle) appelle annoncer(), qui appelle
        //    decrirePropulsion() : la méthode abstraite est résolue sur
        //    l'objet RÉEL, pas sur le type de la variable.
        // ------------------------------------------------------------------
        System.out.println();
        System.out.println("=== 2. Polymorphisme : serviceAnnonce(Vehicle) sans un seul instanceof ===");
        serviceAnnonce(metro);
        serviceAnnonce(bus);
        serviceAnnonce(tram);

        // ------------------------------------------------------------------
        // 3. COMPOSITION + DÉLÉGATION — la ligne A un véhicule.
        //    Line n'étend PAS Vehicle (LSP) ; annoncerService() délègue.
        //    Changer le matériel change l'annonce sans changer la ligne.
        // ------------------------------------------------------------------
        System.out.println();
        System.out.println("=== 3. Composition et délégation : Line o-- Vehicle (pas Line extends Vehicle) ===");
        Tramway rameT1 = new Tramway("T-310", 210, 70.0, 33.0, 0.55);
        Line ligneT1 = new Line("Ligne T1", rameT1);
        System.out.println("  " + ligneT1.getNom() + " annonce : " + ligneT1.annoncerService());
        rameT1.recharger(0.25); // 55 % → 80 %
        System.out.println("  après recharge à " + Math.round(rameT1.niveauActuel() * 100) + " % : "
                + ligneT1.annoncerService());
        System.out.println("  (la ligne n'a RIEN réécrit : c'est son véhicule qui répond — délégation)");

        // ------------------------------------------------------------------
        // 4. INTERFACES / ISP — des capacités, pas des espèces.
        //    instanceof classique (forme traditionnelle + cast explicite ;
        //    le pattern matching « instanceof Capteur c » est réservé au module 80).
        // ------------------------------------------------------------------
        System.out.println();
        System.out.println("=== 4. Interfaces de capacité (ISP) : Capteur et Rechargeable ===");
        Vehicle[] flotte = {metro, bus, tram};
        for (Vehicle v : flotte) {
            System.out.print("  " + v.getImmatriculation() + " : ");
            if (v instanceof Capteur) {
                Capteur capteur = (Capteur) v;
                System.out.print("capteur OK (vitesse lue " + capteur.lireVitesse()
                        + " km/h, survitesse à 75 ? " + capteur.estEnSurvitesse(75.0) + ") — ");
            } else {
                System.out.print("sans capteur — ");
            }
            if (v instanceof Rechargeable) {
                Rechargeable stockage = (Rechargeable) v;
                System.out.print("rechargeable (niveau "
                        + Math.round(stockage.niveauActuel() * 100) + " %)");
            } else {
                System.out.print("non rechargeable");
            }
            System.out.println();
        }
        System.out.println("  Le bus n'implémente aucune des deux interfaces : le code ci-dessus");
        System.out.println("  ne lui demande jamais ce qu'il ne sait pas faire — c'est ISP.");

        // ------------------------------------------------------------------
        // 5. RETOUR COVARIANT — copier() qui connaît son type.
        //    MetroTrain.copier() renvoie MetroTrain (légal : sous-type au
        //    retour) ; Bus.copier() reste Vehicle (contraste voulu).
        // ------------------------------------------------------------------
        System.out.println();
        System.out.println("=== 5. Retour covariant : MetroTrain.copier() rend un MetroTrain, sans cast ===");
        MetroTrain original = new MetroTrain("M-900", 200, 90.0, 4, true);
        MetroTrain copie = original.copier(); // type de retour : MetroTrain — AUCUN cast écrit
        System.out.println("  original : " + original.annoncer());
        System.out.println("  copie    : " + copie.annoncer());
        System.out.println("  même objet ? " + (original == copie) + " — mêmes valeurs, instances distinctes");
        Vehicle enTantQueVehicle = original;
        MetroTrain reCopie = (MetroTrain) enTantQueVehicle.copier();
        System.out.println("  via une variable Vehicle : le cast redevient nécessaire ("
                + reCopie.getImmatriculation() + ") — la covariance aide le type précis, pas le type large.");
        Bus busCopie = (Bus) bus.copier(); // Bus.copier() renvoie Vehicle : le cast est LA leçon (contraste)
        System.out.println("  Bus (covariant NON appliqué) : Bus b = (Bus) bus.copier(); obligatoire → "
                + busCopie.annoncer());

        // ------------------------------------------------------------------
        // 6. MASQUAGE STATIQUE — le faux ami du polymorphisme.
        //    Vehicle.nombreVehicules() compte TOUTE la flotte (métros, bus,
        //    trams) : une méthode statique n'appartient pas à l'objet, elle
        //    ne peut donc pas être « surchargée par le bas ». Si une
        //    sous-classe déclarait la même méthode statique, elle la
        //    MASQUERAIT : le compilateur choisit selon le nom de classe
        //    écrit, jamais selon l'objet. Démonstration avec StatFlotte /
        //    StatMetro (auxiliaires ci-dessus, hors domaine).
        // ------------------------------------------------------------------
        System.out.println();
        System.out.println("=== 6. Méthodes statiques : comptage global et masquage (hide) ===");
        System.out.println("  Vehicle.nombreVehicules() = " + Vehicle.nombreVehicules()
                + " — toutes les sous-classes exécutent le constructeur du parent, le compteur est partagé.");
        System.out.println("  StatFlotte.rapporter() : " + StatFlotte.rapporter());
        System.out.println("  StatMetro.rapporter()  : " + StatMetro.rapporter());
        System.out.println("  Même nom, deux méthodes SANS lien : pas d'@Override possible sur du static,");
        System.out.println("  pas de polymorphisme — d'où : ne jamais masquer une statique du parent (piège de lecture).");

        System.out.println();
        System.out.println("Fin de la démonstration — relisez chaque classe du paquet domain pour");
        System.out.println("retrouver le code qui produit exactement ces lignes.");
    }
}
