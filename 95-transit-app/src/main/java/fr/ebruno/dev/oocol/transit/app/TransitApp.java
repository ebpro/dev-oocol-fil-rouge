package fr.ebruno.dev.oocol.transit.app;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.List;

import fr.ebruno.dev.oocol.transit.cli.TransitCli;
import fr.ebruno.dev.oocol.transit.domain.Line;
import fr.ebruno.dev.oocol.transit.domain.Network;
import fr.ebruno.dev.oocol.transit.domain.NetworkException;
import fr.ebruno.dev.oocol.transit.domain.Station;
import fr.ebruno.dev.oocol.transit.persistence.NetworkJdbcStore;
import fr.ebruno.dev.oocol.transit.persistence.NetworkLoader;

/**
 * L'application Trans'U — le livrable final du fil rouge (ELO 13).
 *
 * <p><strong>Tout le code du parcours assemblé → une application cohérente qui démarre,
 * charge, manipule, sauvegarde et affiche le réseau Trans'U.</strong> Ce main
 * est le point de confluence : le domaine des modules 20 à 80 (records,
 * immuabilité, collections, streams), le Builder du module 95, le CSV du
 * module 60, le JDBC du module 90, et une CLI faite de fonctions pures
 * — assemblés en cinq phases qui se lisent comme le sommaire du cours.
 *
 * <p><strong>Aucune interactivité</strong> (pas de {@code Scanner}, pas de
 * boucle {@code while}) : le programme s'exécute de bout en bout, en local
 * comme dans la CI, et se termine. L'interaction est une couche au-dessus
 * (une boucle qui appelle {@link TransitCli}), jamais une condition de
 * survie du noyau.
 *
 * <p><strong>SRP du main :</strong> il ORchestre, il ne ré-implémente rien.
 * Chaque phrase du scénario délègue à la couche qui la possède :
 * construction → Builder, affichage → CLI, SQL → {@code NetworkJdbcStore},
 * CSV → {@code NetworkLoader}.
 *
 * @author Emmanuel Bruno
 */
public final class TransitApp {

    /** Base H2 en mémoire de la démo — aucune installation, rien à nettoyer. */
    private static final String URL_H2 = "jdbc:h2:mem:transitapp;DB_CLOSE_DELAY=-1";

    /** Point d'entrée du capstone. {@code throws Exception} assumé : la démo doit mourir bruyamment. */
    public static void main(String[] args) throws Exception {
        section("Phase 1 — Construction du réseau (motif Builder)");
        Network reseau = reseauDemo();
        System.out.println(reseau);
        System.out.println("(le constructeur de Network est privé : ce texte n'a pu naître "
                + "que par Network.builder()…construire())");

        section("Phase 2 — Interrogation (CLI testable : des String, pas des print)");
        System.out.println(TransitCli.cmdStats(reseau));
        System.out.println();
        System.out.print(TransitCli.cmdListerStations(reseau));
        System.out.print(TransitCli.cmdListerLignes(reseau));
        System.out.println(TransitCli.cmdChercher(reseau, "UNI"));
        System.out.println(TransitCli.cmdChercher(reseau, "ZZZ"));
        System.out.println(TransitCli.cmdItineraire(reseau, "GAR", "AER"));

        section("Phase 3 — Persistance JDBC (H2 en mémoire, concept du module 90)");
        phaseJdbc(reseau);

        section("Phase 4 — Persistance CSV (concept du module 60, relecture par le Builder)");
        phaseCsv(reseau);

        section("Phase 5 — Rétrospective SOLID");
        retrospective();

        System.out.println();
        System.out.println("FIN DU FIL ROUGE — Bravo !");
    }

    /** Utilitaire de titre de phase : un banner, pas de mise en forme dans le flux métier. */
    private static void section(String titre) {
        System.out.println();
        System.out.println("══════════════════════════════════════════════════");
        System.out.println("  " + titre);
        System.out.println("══════════════════════════════════════════════════");
    }

    /**
     * Le réseau de démonstration, écrit comme une phrase.
     *
     * <p>C'est LA démonstration du Builder : dix-neuf briques chaînées,
     * déclaratives, dans l'ordre des dépendances — et {@code construire()}
     * qui garantit qu'aucune référence cassée n'a survécu à la fin de la
     * chaîne. La version « trois listes pré-remplies passées à un
     * constructeur public » est exactement ce que le constructeur privé de
     * {@code Network} rend désormais impossible.
     *
     * @return le réseau Trans'U complet, immuable et validé
     * @throws NetworkException si le script de construction est incohérent
     */
    public static Network reseauDemo() throws NetworkException {
        return Network.builder()
                .zone(1, "Centre-ville")
                .zone(2, "Périphérie")
                .zone(3, "Grande couronne")
                .station("GAR", "Gare Centrale", 1, true)
                .station("UNI", "Université", 1, true)
                .station("MAR", "Marché des Pins", 1, false)
                .station("HOP", "Hôpital Sud", 2, true)
                .station("PAR", "Parc Technologique", 2, true)
                .station("AER", "Aéroport", 3, true)
                .ligne("Ligne A", "GAR", "UNI", "HOP", "AER")
                .ligne("Ligne B", "GAR", "MAR", "PAR")
                .ligne("Ligne C", "UNI", "MAR", "AER")
                .construire();
    }

    /**
     * Phase 3 : sauver le réseau en base, le recharger, prouver l'identité.
     *
     * <p>Le round-trip compare les stations TRIÉES (la base rend un ordre
     * {@code ORDER BY code}, le Builder un ordre de déclaration — l'identité
     * des listes ne doit rien à cette différence) et l'ordre des arrêts de
     * chaque ligne : la colonne {@code position} du module 90 a survécu, sinon les
     * {@code Line} rechargées ne seraient pas structurellement égales aux
     * originales (l'égalité d'un record inclut sa liste d'arrêts, ordonnée).
     */
    private static void phaseJdbc(Network reseau) throws SQLException, NetworkException {
        try (Connection conn = DriverManager.getConnection(URL_H2)) {
            NetworkJdbcStore.creerSchema(conn);
            NetworkJdbcStore.sauvegarder(reseau, conn);
            Network reload = NetworkJdbcStore.chargerDepuisBase(conn);
            System.out.println("Base H2 : " + reseau.stations().size() + " stations écrites, "
                    + reload.stations().size() + " relues.");
            exiger(trierStations(reload).equals(trierStations(reseau)),
                    "round-trip JDBC : les stations diffèrent");
            exiger(new HashSet<>(reload.lignes()).equals(new HashSet<>(reseau.lignes())),
                    "round-trip JDBC : les lignes (noms et ordres d'arrêts) diffèrent");
            System.out.println("✔ Round-trip JDBC vérifié : rechargé == construit.");
        }
    }

    /**
     * Phase 4 : écrire les trois CSV depuis le réseau (l'inverse du loader),
     * recharger par {@link NetworkLoader}, prouver l'identité.
     *
     * <p>Deux stratégies de persistance (CSV et JDBC) produisent le MÊME
     * objet immuable, validé par le même Builder : c'est la preuve concrète
     * du DIP — le domaine est le point commun, les formats sont interchangeables.
     */
    private static void phaseCsv(Network reseau) throws IOException, NetworkException {
        Path dossier = Files.createTempDirectory("transit-csv");
        try {
            Files.writeString(dossier.resolve("zones.csv"), csvZones(reseau), StandardCharsets.UTF_8);
            Files.writeString(dossier.resolve("stations.csv"), csvStations(reseau),
                    StandardCharsets.UTF_8);
            Files.writeString(dossier.resolve("lignes.csv"), csvLignes(reseau),
                    StandardCharsets.UTF_8);
            Network reload = NetworkLoader.chargerDepuisRepertoire(dossier);
            System.out.println("CSV écrits dans " + dossier + ", rechargés via NetworkLoader.");
            exiger(trierStations(reload).equals(trierStations(reseau)),
                    "round-trip CSV : les stations diffèrent");
            exiger(new HashSet<>(reload.lignes()).equals(new HashSet<>(reseau.lignes())),
                    "round-trip CSV : les lignes diffèrent");
            System.out.println("✔ Round-trip CSV vérifié : rechargé == construit.");
        } finally {
            Files.deleteIfExists(dossier.resolve("zones.csv"));
            Files.deleteIfExists(dossier.resolve("stations.csv"));
            Files.deleteIfExists(dossier.resolve("lignes.csv"));
            Files.deleteIfExists(dossier);
        }
    }

    /** @return le contenu de {@code zones.csv} (format du module 60 : {@code numero;nom}) */
    private static String csvZones(Network reseau) {
        StringBuilder sb = new StringBuilder("# zones\n");
        for (var z : reseau.zones()) {
            sb.append(z.numero()).append(';').append(z.nom()).append('\n');
        }
        return sb.toString();
    }

    /** @return le contenu de {@code stations.csv} (format du module 60 : {@code code;nom;zone;accessible}) */
    private static String csvStations(Network reseau) {
        StringBuilder sb = new StringBuilder("# stations\n");
        for (Station s : reseau.stations()) {
            sb.append(s.code()).append(';').append(s.nom()).append(';')
              .append(s.zone().numero()).append(';').append(s.accessible()).append('\n');
        }
        return sb.toString();
    }

    /** @return le contenu de {@code lignes.csv} (format du module 60 : {@code nom;arret1;arret2;...}) */
    private static String csvLignes(Network reseau) {
        StringBuilder sb = new StringBuilder("# lignes\n");
        for (Line l : reseau.lignes()) {
            List<String> codes = l.arrets().stream().map(Station::code).toList();
            sb.append(l.nom()).append(';').append(String.join(";", codes)).append('\n');
        }
        return sb.toString();
    }

    /** Échec bruyant de la démo : un round-trip qui ne rond-tripe pas doit tuer le programme. */
    private static void exiger(boolean condition, String succesOuEchec) {
        if (!condition) {
            throw new AssertionError("ÉCHEC DE VÉRIFICATION : " + succesOuEchec);
        }
    }

    /** Les stations d'un réseau, dans l'ordre naturel (par code) — comparaison indépendante de l'ordre d'insertion. */
    private static List<Station> trierStations(Network n) {
        return n.stations().stream().sorted().toList();
    }

    /**
     * Phase 5 : la rétrospective — six blocs de texte, cinq principes, tout le parcours relu.
     *
     * <p>Chaque principe cite SA démonstration dans le fil rouge : le
     * SOLID n'est plus une définition à réciter, c'est l'inventaire de ce
     * que le code fait déjà — et de l'endroit exact où le revoir.
     */
    private static void retrospective() {
        System.out.println("""
                ╔════════════════════════════════════════╗
                ║  RÉTROSPECTIVE SOLID — 13 SEMAINES    ║
                ╚════════════════════════════════════════╝""");
        System.out.println("""
                ┌─ S — SRP : une classe, une raison de changer.
                │  Module 20 : Station ≠ Zone ≠ Line — chaque entité porte sa donnée.
                │  Module 95 : Network ignore CSV et SQL ; la CLI n'imprime pas, elle
                │  retourne ; le store ne questionne pas le domaine.
                └─""");
        System.out.println("""
                ┌─ O — OCP : s'étendre sans modifier le code fermé.
                │  Module 30 : VehicleFactory — un nouveau véhicule est une nouvelle
                │  classe, jamais une retouche des appelants.
                │  Module 95 : le Builder est le point d'extension ; Network, lui,
                │  n'a pas bougé d'une ligne depuis sa fermeture.
                └─""");
        System.out.println("""
                ┌─ L — LSP : substituer un sous-type sans casser le contrat.
                │  Module 30 : Bus, Métro, Tramway tiennent tous le contrat Vehicle.
                │  Module 80 : toute Propulsion (sealed) se met partout où une
                │  Propulsion est attendue — le switch exhaustif le prouve à la compilation.
                └─""");
        System.out.println("""
                ┌─ I — ISP : des interfaces petites, jamais de BigDao.
                │  Module 90 : StationDao et LineDao séparés — qui n'a besoin que des
                │  arrêts ne traîne pas le SQL des lignes.
                │  Module 95 : le Builder construit, Network interroge — deux rôles,
                │  deux faces d'API, aucun client forcé de voir l'autre.
                └─""");
        System.out.println("""
                ┌─ D — DIP : dépendre de l'abstraction, pas du détail.
                │  Module 90 : le domaine ne connaît pas JDBC — l'implémentation SQL
                │  est branchée par le haut.
                │  Module 95 : la CLI ne voit que des méthodes de Network ; CSV (W7)
                │  et JDBC (W12) sont deux stratégies interchangeables du même contrat.
                └─""");
    }

    /** Classe statique : pas d'instance. */
    private TransitApp() {
        throw new AssertionError("TransitApp est un point d'entrée statique : pas d'instanciation.");
    }
}
