package fr.ebruno.dev.oocol.jdbc.dao.impl;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import fr.ebruno.dev.oocol.jdbc.dao.StationDao;
import fr.ebruno.dev.oocol.jdbc.domain.Station;

/**
 * Implémentation JDBC de {@link StationDao} — le SQL vit ici, et seulement ici.
 *
 * <p><strong>DAO = Data Access Object.</strong> Isole le SQL du domaine.
 * Interface = contrat ; implémentation = JDBC. Demain : JPA remplace
 * l'implémentation sans toucher l'interface.
 *
 * <p><strong>Les trois gestes de toute méthode JDBC.</strong>
 * <ol>
 *   <li>préparer la requête : {@code connection.prepareStatement(sql)} avec
 *       des {@code ?} — jamais de concaténation de valeurs ;</li>
 *   <li>lier les paramètres : {@code ps.setString(1, …)} (numérotation à
 *       partir de 1, dans l'ordre des {@code ?}) ;</li>
 *   <li>exécuter et mapper : {@code executeQuery}/{@code executeUpdate}, puis
 *       traduire chaque ligne du {@link ResultSet} en record de domaine.</li>
 * </ol>
 *
 * <p><strong>Cycle de vie de la connexion.</strong> Le constructeur reçoit une
 * {@link Connection} et se contente de la <em>prêter</em> : il ne l'ouvre pas
 * et ne la ferme jamais. C'est l'appelant (la démo, le test, la couche web
 * de demain) qui gère la durée de vie — sinon le premier DAO qui termine sa
 * méthode fermerait la connexion de tous les autres.
 *
 * <p><strong>try-with-resources partout.</strong> {@code PreparedStatement}
 * et {@code ResultSet} sont des ressources closes ; les laisser ouvertes,
 * c'est fuir des curseurs côté serveur.
 */
public final class StationDaoJdbc implements StationDao {

    private static final String SQL_INSERER =
            "INSERT INTO stations (code, nom, zone_numero, accessible) VALUES (?, ?, ?, ?)";
    private static final String SQL_PAR_CODE =
            "SELECT code, nom, zone_numero, accessible FROM stations WHERE code = ?";
    private static final String SQL_TOUS =
            "SELECT code, nom, zone_numero, accessible FROM stations ORDER BY code";
    private static final String SQL_METTRE_A_JOUR =
            "UPDATE stations SET nom = ?, zone_numero = ?, accessible = ? WHERE code = ?";
    private static final String SQL_SUPPRIMER =
            "DELETE FROM stations WHERE code = ?";
    private static final String SQL_PAR_ZONE =
            "SELECT code, nom, zone_numero, accessible FROM stations WHERE zone_numero = ? ORDER BY code";
    private static final String SQL_COMME_NOM =
            "SELECT code, nom, zone_numero, accessible FROM stations WHERE nom LIKE ? ORDER BY code";
    private static final String SQL_EXISTE =
            "SELECT 1 FROM stations WHERE code = ?";

    private final Connection connection;

    /**
     * @param connection connexion JDBC ouverte et validée — prête à être prêtée, jamais fermée par ce DAO
     */
    public StationDaoJdbc(Connection connection) {
        this.connection = connection;
    }

    @Override
    public void inserer(Station station) {
        try (PreparedStatement ps = connection.prepareStatement(SQL_INSERER)) {
            ps.setString(1, station.code());
            ps.setString(2, station.nom());
            ps.setInt(3, station.zoneNumero());
            ps.setBoolean(4, station.accessible());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw sansChecked("INSERT INTO stations (code=" + station.code() + ")", e);
        }
    }

    @Override
    public Optional<Station> trouverParId(String code) {
        try (PreparedStatement ps = connection.prepareStatement(SQL_PAR_CODE)) {
            ps.setString(1, code);
            try (ResultSet rs = ps.executeQuery()) {
                // Une clé primaire, c'est 0 ou 1 ligne : Optional, pas null.
                return rs.next() ? Optional.of(mapResultSet(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw sansChecked("SELECT station (code=" + code + ")", e);
        }
    }

    @Override
    public List<Station> trouverTous() {
        try (PreparedStatement ps = connection.prepareStatement(SQL_TOUS);
             ResultSet rs = ps.executeQuery()) {
            List<Station> resultat = new ArrayList<>();
            while (rs.next()) {
                resultat.add(mapResultSet(rs));
            }
            return List.copyOf(resultat);
        } catch (SQLException e) {
            throw sansChecked("SELECT tous les stations", e);
        }
    }

    @Override
    public void mettreAJour(Station station) {
        try (PreparedStatement ps = connection.prepareStatement(SQL_METTRE_A_JOUR)) {
            // Le code n'est PAS dans le SET : une clé primaire ne se mute pas, elle identifie la ligne.
            ps.setString(1, station.nom());
            ps.setInt(2, station.zoneNumero());
            ps.setBoolean(3, station.accessible());
            ps.setString(4, station.code());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw sansChecked("UPDATE stations (code=" + station.code() + ")", e);
        }
    }

    @Override
    public void supprimer(String code) {
        try (PreparedStatement ps = connection.prepareStatement(SQL_SUPPRIMER)) {
            ps.setString(1, code);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw sansChecked("DELETE station (code=" + code + ")", e);
        }
    }

    @Override
    public List<Station> trouverParZone(int zoneNumero) {
        try (PreparedStatement ps = connection.prepareStatement(SQL_PAR_ZONE)) {
            ps.setInt(1, zoneNumero);
            try (ResultSet rs = ps.executeQuery()) {
                List<Station> resultat = new ArrayList<>();
                while (rs.next()) {
                    resultat.add(mapResultSet(rs));
                }
                return List.copyOf(resultat);
            }
        } catch (SQLException e) {
            throw sansChecked("SELECT stations par zone " + zoneNumero, e);
        }
    }

    @Override
    public List<Station> chercherParNom(String motif) {
        try (PreparedStatement ps = connection.prepareStatement(SQL_COMME_NOM)) {
            // Les jokers %…% sont ajoutés DANS LA VALEUR transmise par ?, jamais dans la chaîne SQL.
            ps.setString(1, "%" + motif + "%");
            try (ResultSet rs = ps.executeQuery()) {
                List<Station> resultat = new ArrayList<>();
                while (rs.next()) {
                    resultat.add(mapResultSet(rs));
                }
                return List.copyOf(resultat);
            }
        } catch (SQLException e) {
            throw sansChecked("SELECT stations LIKE " + motif, e);
        }
    }

    @Override
    public boolean existe(String code) {
        try (PreparedStatement ps = connection.prepareStatement(SQL_EXISTE)) {
            ps.setString(1, code);
            try (ResultSet rs = ps.executeQuery()) {
                // SELECT 1 … WHERE : rs.next() dit lui-même si la ligne existe. Inutile de compter.
                return rs.next();
            }
        } catch (SQLException e) {
            throw sansChecked("EXISTS station (code=" + code + ")", e);
        }
    }

    /**
     * Mapping d'une ligne du {@link ResultSet} vers le record {@link Station}.
     *
     * <p>Ce geste — « la base parle en colonnes, le domaine parle en champs »
     * — s'appelle le <em>wire mapping</em>. JPA le génère ; en JDBC, c'est
     * nous qui l'écrivons, une colonne à la fois.
     */
    private static Station mapResultSet(ResultSet rs) throws SQLException {
        return new Station(
                rs.getString("code"),
                rs.getString("nom"),
                rs.getInt("zone_numero"),
                rs.getBoolean("accessible"));
    }

    /**
     * Enveloppe une {@code SQLException} vérifiée dans une {@code RuntimeException}
     * non vérifiée, en préservant la cause (chaînage, cf. ELO 07).
     *
     * <p>Le contrat {@link CrudDao} n'expose pas de {@code SQLException} :
     * l'infrastructure ne remonte pas vérifiée dans le vocabulaire du domaine.
     */
    private static RuntimeException sansChecked(String action, SQLException cause) {
        return new IllegalStateException("Opération JDBC échouée : " + action, cause);
    }
}
