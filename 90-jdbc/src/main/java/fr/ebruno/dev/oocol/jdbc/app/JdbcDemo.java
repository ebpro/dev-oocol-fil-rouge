package fr.ebruno.dev.oocol.jdbc.app;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.List;

import fr.ebruno.dev.oocol.jdbc.domain.Line;
import fr.ebruno.dev.oocol.jdbc.domain.Station;
import fr.ebruno.dev.oocol.jdbc.repo.NetworkRepository;
import fr.ebruno.dev.oocol.jdbc.schema.DatabaseSchema;
import fr.ebruno.dev.oocol.jdbc.security.SqlInjectionDemo;

/**
 * Les six scènes du JDBC sur le réseau Trans'U (ELO 12).
 *
 * <p><strong>Le fil narratif.</strong> Semaine 12 : le {@code NetworkSnapshot}
 * sérialisé de W8 était fragile (format binaire opaque) et mono-poste (un
 * fichier, un utilisateur à la fois). Une vraie base de données résout la
 * concurrence, les requêtes et l'intégrité — cette démo joue ce passage.
 *
 * <p><strong>Scènes.</strong> 1 : ouverture de la connexion H2 en mémoire et
 * création du schéma ; 2 : alimentation et lecture par les DAO (le mapping
 * records ↔ tables) ; 3 : l'injection SQL démontrée, concaténation contre
 * {@code PreparedStatement} ; 4 : la transaction — deux insertions valides
 * suivies d'un doublon volontaire, le tout annulé en bloc par le rollback ;
 * 5 : la recherche {@code LIKE} paramétrée ; 6 : suppression massive, compteur
 * à zéro — la base est un état comme un autre, elle se vide.
 *
 * <p><strong>Zéro pollution.</strong> La base est {@code jdbc:h2:mem:} : en
 * mémoire, sans fichier, détruite avec la JVM. Elle tourne identiquement en
 * TD et dans la CI.
 *
 * @author Emmanuel Bruno
 * @see fr.ebruno.dev.oocol.jdbc.dao
 */
public final class JdbcDemo {

    /** URL H2 en mémoire : {@code DB_CLOSE_DELAY=-1} garde la base vivante tant que la JVM tourne. */
    private static final String URL_H2 = "jdbc:h2:mem:transU;DB_CLOSE_DELAY=-1";

    /** Pas d'instance : une classe à {@code main}, comme dans tous les modules. */
    private JdbcDemo() {
    }

    /**
     * Enchaîne les six scènes.
     *
     * @param args ignorés
     * @throws SQLException si la base refuse une opération hors scène transactionnelle
     */
    public static void main(String[] args) throws SQLException {
        System.out.println("=== Module 90 : le réseau Trans'U passe en base de données (ELO 12) ===");
        // try-with-resources SUR la connexion : c'est l'appelant qui l'ouvre, c'est lui qui la ferme.
        try (Connection conn = DriverManager.getConnection(URL_H2, "sa", "")) {
            scene1Creation(conn);
            NetworkRepository repo = scene2Alimentation(conn);
            scene3Injection(conn);
            scene4Transaction(repo);
            scene5Recherche(repo);
            scene6Vidage(repo);
        }
        System.out.println();
        System.out.println("=== Fin de la démo : le snapshot de W8 est mort — vive la base de données. ===");
    }

    /** Scène 1 : DriverManager + DDL. */
    private static void scene1Creation(Connection conn) throws SQLException {
        System.out.println();
        System.out.println("--- Scène 1 : ouverture et schéma ---");
        System.out.println("Connexion JDBC ouverte : " + conn.getMetaData().getURL()
                + " (" + conn.getMetaData().getDatabaseProductName() + " "
                + conn.getMetaData().getDatabaseProductVersion() + ")");
        DatabaseSchema.creer(conn);
        DatabaseSchema.insererZonesInit(conn);
        System.out.println("Base de données créée : zones, stations, lignes, ligne_arrets.");
    }

    /** Scène 2 : insertion et relecture par les DAO. */
    private static NetworkRepository scene2Alimentation(Connection conn) {
        System.out.println();
        System.out.println("--- Scène 2 : les DAO font le sale travail ---");
        NetworkRepository repo = new NetworkRepository(conn);

        repo.stationDao().inserer(new Station("HUB", "Hub Central", 1, true));
        repo.stationDao().inserer(new Station("PAR", "Parc Verdun", 1, true));
        repo.stationDao().inserer(new Station("UNI", "Campus", 2, false));
        repo.stationDao().inserer(new Station("MAR", "Vieux Marché", 2, true));
        repo.stationDao().inserer(new Station("PLA", "Plage Sud", 3, true));

        repo.lineDao().inserer(new Line("A", List.of("HUB", "PAR", "UNI")));
        repo.lineDao().inserer(new Line("B", List.of("HUB", "MAR", "PLA")));

        System.out.println("5 stations et 2 lignes insérées via les DAO (PreparedStatement, jamais de concaténation).");
        System.out.println("trouverParId(\"UNI\") → " + repo.stationDao().trouverParId("UNI").orElseThrow());
        System.out.println("trouverParZone(1)   → " + repo.stationDao().trouverParZone(1));
        System.out.println("ligne A relue depuis les 2 tables → " + repo.lineDao().trouverParId("A").orElseThrow());
        System.out.println("HUB est desservi par " + repo.lineDao().trouverParStation("HUB").size()
                + " lignes (jointure via ligne_arrets).");
        return repo;
    }

    /** Scène 3 : l'injection SQL, jouée puis neutralisée. */
    private static void scene3Injection(Connection conn) throws SQLException {
        System.out.println();
        System.out.println("--- Scène 3 : injection SQL — le code vulnérable est là pour être COMPARÉ ---");
        SqlInjectionDemo.demonstrer(conn);
    }

    /** Scène 4 : transaction ACID — commit implicite du succès, rollback du lot fautif. */
    private static void scene4Transaction(NetworkRepository repo) {
        System.out.println();
        System.out.println("--- Scène 4 : tout ou rien ---");
        int avant = repo.stationDao().trouverTous().size();
        System.out.println("Avant la transaction : " + avant + " stations.");
        try {
            repo.transactionSurement(() -> {
                repo.stationDao().inserer(new Station("NOR", "Quai Nord", 1, true));
                repo.stationDao().inserer(new Station("SUD", "Quai Sud", 2, true));
                // Échec VOLONTAIRE : NOR en doublon viole la PK → SQLException → RuntimeException.
                repo.stationDao().inserer(new Station("NOR", "Faux doublon", 1, false));
            });
            System.out.println("Transaction validée (commit).");
        } catch (RuntimeException e) {
            System.out.println("Échec en cours de transaction : " + e.getCause().getMessage());
            System.out.println("→ transactionSurement a fait rollback() : les deux premières insertions sont annulées.");
        }
        int apres = repo.stationDao().trouverTous().size();
        System.out.println("Après le rollback : " + apres + " stations — "
                + (avant == apres ? "inchangé, l'atomicité (le A de ACID) est démontrée." : "INATTENDU !"));
    }

    /** Scène 5 : recherche partielle LIKE, entièrement paramétrée. */
    private static void scene5Recherche(NetworkRepository repo) {
        System.out.println();
        System.out.println("--- Scène 5 : chercherParNom(\"Gare\") ---");
        List<Station> trouvées = repo.stationDao().chercherParNom("Gare");
        System.out.println(trouvées.size() + " résultat : " + trouvées);
        System.out.println("Le motif est devenu la valeur \"%Gare%\" transmise par ? — le SQL, lui, n'a pas bougé.");
    }

    /** Scène 6 : suppression de tout, compteur à zéro. */
    private static void scene6Vidage(NetworkRepository repo) {
        System.out.println();
        System.out.println("--- Scène 6 : nettoyer ---");
        // Les lignes d'abord (leur table-jonction référence les stations), puis les arrêts.
        for (Line l : repo.lineDao().trouverTous()) {
            repo.lineDao().supprimer(l.nom());
        }
        for (Station s : repo.stationDao().trouverTous()) {
            repo.stationDao().supprimer(s.code());
        }
        System.out.println("Après suppression : " + repo.stationDao().trouverTous().size()
                + " station, " + repo.lineDao().trouverTous().size() + " ligne.");
    }
}
