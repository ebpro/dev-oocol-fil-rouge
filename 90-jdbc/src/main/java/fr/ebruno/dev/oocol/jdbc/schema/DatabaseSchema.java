package fr.ebruno.dev.oocol.jdbc.schema;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Le schéma relationnel du réseau Trans'U — le DDL du module.
 *
 * <p>Quatre tables, deux relations :
 * <ul>
 *   <li>{@code zones} (PK {@code numero}) — la référence tarifaire ;</li>
 *   <li>{@code stations} (PK {@code code}, FK {@code zone_numero → zones.numero})
 *       — un arrêt appartient à une zone ;</li>
 *   <li>{@code lignes} (PK {@code nom}) — une ligne, c'est un nom ;</li>
 *   <li>{@code ligne_arrets} (PK composée {@code (nom_ligne, code_station)},
 *       colonne {@code position}) — la jointure N:N ordinalisée entre lignes
 *       et stations.</li>
 * </ul>
 *
 * <p>En TD :<em>pourquoi la PK de {@code ligne_arrets} est composée ?</em>
 * Parce qu'un arrêt ne peut être desservi qu'une fois par ligne dans ce
 * modèle — la contrainte d'unicité EST le doublon. Et <em>pourquoi pas de
 * {@code ON DELETE CASCADE} ?</em> Pour que l'étudiant voie l'effet de la
 * contrainte de référence quand l'ordre de suppression est mauvais.
 */
public final class DatabaseSchema {

    private static final String DDL_ZONES =
            "CREATE TABLE IF NOT EXISTS zones ("
            + "numero INT PRIMARY KEY, "
            + "nom VARCHAR(100) NOT NULL)";

    private static final String DDL_STATIONS =
            "CREATE TABLE IF NOT EXISTS stations ("
            + "code VARCHAR(10) PRIMARY KEY, "
            + "nom VARCHAR(200) NOT NULL, "
            + "zone_numero INT REFERENCES zones(numero), "
            + "accessible BOOLEAN DEFAULT TRUE)";

    private static final String DDL_LIGNES =
            "CREATE TABLE IF NOT EXISTS lignes ("
            + "nom VARCHAR(100) PRIMARY KEY)";

    private static final String DDL_LIGNE_ARRETS =
            "CREATE TABLE IF NOT EXISTS ligne_arrets ("
            + "nom_ligne VARCHAR(100), "
            + "code_station VARCHAR(10), "
            + "position INT, "
            + "PRIMARY KEY (nom_ligne, code_station))";

    /** MERGE (pas INSERT) : insererZonesInit doit être rejouable sans doublon. */
    private static final String DML_ZONES_1 =
            "MERGE INTO zones (numero, nom) KEY(numero) VALUES (1, 'Centre')";
    private static final String DML_ZONES_2 =
            "MERGE INTO zones (numero, nom) KEY(numero) VALUES (2, 'Intermédiaire')";
    private static final String DML_ZONES_3 =
            "MERGE INTO zones (numero, nom) KEY(numero) VALUES (3, 'Périphérie')";

    /** Ordre inverse des dépendances : les enfants avant les parents. */
    private static final String[] DDL_VIDAGE = {
            "DELETE FROM ligne_arrets",
            "DELETE FROM lignes",
            "DELETE FROM stations",
            "DELETE FROM zones",
    };

    /** Pas d'instance : une classe d'utilitaires statiques. */
    private DatabaseSchema() {
    }

    /**
     * Crée toutes les tables (idempotent grâce à {@code IF NOT EXISTS}).
     *
     * @param conn connexion ouverte
     * @throws SQLException si le DDL est refusé par la base
     */
    public static void creer(Connection conn) throws SQLException {
        try (Statement st = conn.createStatement()) {
            st.executeUpdate(DDL_ZONES);
            st.executeUpdate(DDL_STATIONS);
            st.executeUpdate(DDL_LIGNES);
            st.executeUpdate(DDL_LIGNE_ARRETS);
        }
    }

    /**
     * Insère (ou re-confirme) les trois zones tarifaires de base.
     *
     * @param conn connexion ouverte
     * @throws SQLException si l'écriture est refusée
     */
    public static void insererZonesInit(Connection conn) throws SQLException {
        try (Statement st = conn.createStatement()) {
            st.executeUpdate(DML_ZONES_1);
            st.executeUpdate(DML_ZONES_2);
            st.executeUpdate(DML_ZONES_3);
        }
    }

    /**
     * Vide toutes les données (les tables restent).
     *
     * <p>Utilisé par les tests pour l'isolation et par la scène 6 de la démo.
     *
     * @param conn connexion ouverte
     * @throws SQLException si la suppression est refusée
     */
    public static void vider(Connection conn) throws SQLException {
        // Batch : une seule round-trip pour vider toutes les tables, pas une
        // par DELETE. Le résultat est identique — l'ordre des requêtes est
        // préservé (les contraintes de référence de W12 ne pardonnent pas).
        try (Statement st = conn.createStatement()) {
            for (String sql : DDL_VIDAGE) {
                st.addBatch(sql);
            }
            st.executeBatch();
        }
    }
}
