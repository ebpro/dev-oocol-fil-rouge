package fr.ebruno.dev.oocol.transit.persistence;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import fr.ebruno.dev.oocol.transit.domain.Network;
import fr.ebruno.dev.oocol.transit.domain.NetworkBuilder;
import fr.ebruno.dev.oocol.transit.domain.NetworkException;
import fr.ebruno.dev.oocol.transit.domain.Station;
import fr.ebruno.dev.oocol.transit.domain.Zone;

/**
 * Persistance JDBC du réseau — le concept du module 90 (W12), simplifié et
 * raconté à travers le Builder.
 *
 * <p><strong>SRP : ce qui lit/écrit SQL est ICI, pas dans {@code Network}.</strong>
 * Le domaine ne connaît pas JDBC — aucun import {@code java.sql} ne franchit
 * la frontière du paquet {@code domain}. Ce que {@code Network} ignore, ce
 * que la classe {@link Network} documente sous "DIP" : les deux dépendent de
 * la même abstraction (les méthodes du domaine), la base peut passer de H2 à
 * Postgres — ou disparaître au profit d'un fichier — sans qu'une seule ligne
 * du domaine ou de la CLI ne bouge.
 *
 * <p><strong>Différence assumée avec W12.</strong> Le module 90 décortiquait
 * le motif DAO : petits contrats {@code StationDao}/{@code LineDao}, façade
 * {@code NetworkRepository}, gabarit transactionnel. À la semaine 13, le fil
 * rouge a besoin d'UNE opération « sauver tout l'agrégat / recharger tout
 * l'agrégat » — deux méthodes statiques suffisent, le vocabulaire JDBC reste
 * le même : {@link PreparedStatement} partout (jamais de concaténation —
 * W12 a prouvé pourquoi), try-with-resources partout, batch pour les
 * insertions, transaction pour l'atomicité du remplacement complet.
 *
 * <p><strong>Le retour du Builder.</strong> {@link #chargerDepuisBase(Connection)}
 * ne connaît pas le constructeur privé de {@code Network} — et n'en a pas
 * besoin : il empile les lignes des tables dans un {@link NetworkBuilder} et
 * laisse {@code construire()} valider. Base corrompue (FK orpheline, doublon)
 * ? Une {@link NetworkException} explicite plutôt qu'un objet bancal.
 *
 * @author Emmanuel Bruno
 * @see NetworkLoader — l'autre stratégie : mêmes données, format CSV
 */
public final class NetworkJdbcStore {

    /** Schéma identique au module 90 : quatre tables, deux relations. */
    private static final String[] DDL = {
        "CREATE TABLE IF NOT EXISTS zones ("
                + "numero INT PRIMARY KEY, "
                + "nom VARCHAR(100) NOT NULL)",
        "CREATE TABLE IF NOT EXISTS stations ("
                + "code VARCHAR(10) PRIMARY KEY, "
                + "nom VARCHAR(200) NOT NULL, "
                + "zone_numero INT REFERENCES zones(numero), "
                + "accessible BOOLEAN DEFAULT TRUE)",
        "CREATE TABLE IF NOT EXISTS lignes ("
                + "nom VARCHAR(100) PRIMARY KEY)",
        "CREATE TABLE IF NOT EXISTS ligne_arrets ("
                + "nom_ligne VARCHAR(100), "
                + "code_station VARCHAR(10), "
                + "position INT, "
                + "PRIMARY KEY (nom_ligne, code_station))",
    };

    /** Vidage enfants d'abord : la contrainte de référence ne pardonne pas l'ordre inverse (W12). */
    private static final String[] VIDAGE = {
        "DELETE FROM ligne_arrets",
        "DELETE FROM lignes",
        "DELETE FROM stations",
        "DELETE FROM zones",
    };

    /** Utilitaire statique : pas d'instance. */
    private NetworkJdbcStore() {
        throw new AssertionError("NetworkJdbcStore est une classe utilitaire : pas d'instanciation.");
    }

    /**
     * Crée les quatre tables si absentes (idempotent, {@code IF NOT EXISTS}).
     *
     * @param conn connexion ouverte
     * @throws SQLException si le DDL est refusé par la base
     */
    public static void creerSchema(Connection conn) throws SQLException {
        try (Statement st = conn.createStatement()) {
            for (String sql : DDL) {
                st.executeUpdate(sql);
            }
        }
    }

    /**
     * Sauvegarde l'intégralité du réseau : la base devient un miroir exact de
     * l'objet, en UNE transaction.
     *
     * <p>Le remplacement est total (vider puis insérer) et atomique : si la
     * 700<sup>e</sup> insertion échoue, les anciennes données sont toujours là
     * ({@code rollback}) — jamais un demi-réseau. C'est la scène ACID du
     * module 90 rejouée à l'échelle de l'agrégat. Les insertions passent par
     * {@code addBatch}/{@code executeBatch} : un aller-retour par table, pas
     * par ligne.
     *
     * @param network le réseau à écrire (immuable — il ne change rien en cours de route)
     * @param conn    connexion ouverte, schéma déjà créé
     * @throws SQLException si la base refuse une écriture (la transaction est annulée)
     */
    public static void sauvegarder(Network network, Connection conn) throws SQLException {
        boolean autoCommitOriginal = conn.getAutoCommit();
        conn.setAutoCommit(false);
        try {
            try (Statement st = conn.createStatement()) {
                for (String sql : VIDAGE) {
                    st.executeUpdate(sql);
                }
            }
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO zones (numero, nom) VALUES (?, ?)")) {
                for (Zone z : network.zones()) {
                    ps.setInt(1, z.numero());
                    ps.setString(2, z.nom());
                    ps.addBatch();
                }
                ps.executeBatch();
            }
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO stations (code, nom, zone_numero, accessible) VALUES (?, ?, ?, ?)")) {
                for (Station s : network.stations()) {
                    ps.setString(1, s.code());
                    ps.setString(2, s.nom());
                    ps.setInt(3, s.zone().numero());
                    ps.setBoolean(4, s.accessible());
                    ps.addBatch();
                }
                ps.executeBatch();
            }
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO lignes (nom) VALUES (?)")) {
                for (var l : network.lignes()) {
                    ps.setString(1, l.nom());
                    ps.addBatch();
                }
                ps.executeBatch();
            }
            // L'ordre de parcours des arrêts devient une COLONNE (leçon W12 :
            // une Line vit sur DEUX tables ; sans « position », le SELECT du
            // retour ne saurait pas dans quel ordre les relier).
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO ligne_arrets (nom_ligne, code_station, position) VALUES (?, ?, ?)")) {
                for (var l : network.lignes()) {
                    List<Station> arrets = l.arrets();
                    for (int i = 0; i < arrets.size(); i++) {
                        ps.setString(1, l.nom());
                        ps.setString(2, arrets.get(i).code());
                        ps.setInt(3, i);
                        ps.addBatch();
                    }
                }
                ps.executeBatch();
            }
            conn.commit();
        } catch (SQLException e) {
            conn.rollback();
            throw e;
        } finally {
            conn.setAutoCommit(autoCommitOriginal);
        }
    }

    /**
     * Recharge le réseau complet : SELECT des quatre tables, puis reconstruction
     * par Builder.
     *
     * <p>Chaque table est lue dans un ordre DÉTERMINISTE ({@code ORDER BY}) —
     * sans clause d'ordre, un SGBD relationnel ne promet rien : le même
     * {@code SELECT} peut rendre les lignes dans un ordre différent d'une
     * exécution à l'autre. Ici la déterminisation sert aussi la pédagogie :
     * le réseau rechargé se compare terme à terme au réseau sauvegardé.
     *
     * @param conn connexion ouverte, schéma déjà créé
     * @return le réseau reconstitué, validé par le Builder
     * @throws SQLException     si la base refuse une lecture
     * @throws NetworkException si le contenu des tables est incohérent
     *                          (zone orpheline, doublon — base corrompue)
     */
    public static Network chargerDepuisBase(Connection conn) throws SQLException, NetworkException {
        NetworkBuilder builder = Network.builder();

        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("SELECT numero, nom FROM zones ORDER BY numero")) {
            while (rs.next()) {
                builder.zone(rs.getInt("numero"), rs.getString("nom"));
            }
        }
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(
                     "SELECT code, nom, zone_numero, accessible FROM stations ORDER BY code")) {
            while (rs.next()) {
                builder.station(rs.getString("code"), rs.getString("nom"),
                        rs.getInt("zone_numero"), rs.getBoolean("accessible"));
            }
        }
        // Une seule jointure trie « par ligne puis par position » ; le
        // LinkedHashMap groupe sans casser l'ordre des clés ni des valeurs.
        Map<String, List<String>> codesParLigne = new LinkedHashMap<>();
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(
                     "SELECT nom_ligne, code_station FROM ligne_arrets "
                     + "ORDER BY nom_ligne, position")) {
            while (rs.next()) {
                codesParLigne.computeIfAbsent(rs.getString("nom_ligne"), k -> new ArrayList<>())
                        .add(rs.getString("code_station"));
            }
        }
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("SELECT nom FROM lignes ORDER BY nom")) {
            while (rs.next()) {
                String nom = rs.getString("nom");
                List<String> codes = codesParLigne.getOrDefault(nom, List.of());
                builder.ligne(nom, codes.toArray(String[]::new));
            }
        }
        return builder.construire();
    }
}
