package fr.ebruno.dev.oocol.jdbc.dao.impl;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import fr.ebruno.dev.oocol.jdbc.dao.LineDao;
import fr.ebruno.dev.oocol.jdbc.domain.Line;

/**
 * Implémentation JDBC de {@link LineDao} — le cas « agrégat » du mapping objet-relationnel.
 *
 * <p>Une {@link Line} vit sur <strong>deux tables</strong> :
 * {@code lignes} (une ligne par nom) et {@code ligne_arrets} (une ligne par
 * arrêt, avec une colonne {@code position} qui porte l'ordre de parcours).
 * C'est la relation N:N ordinalisée : ce que les collections du module 40 ne pouvaient pas exprimer seules — <em>en SQL, l'ordre est une colonne</em>.
 *
 * <p>Écrire l'agrégat = insérer dans {@code lignes} puis dans
 * {@code ligne_arrets} ; le relire = requête parente + jointure triée par
 * {@code position}. Supprimer l'agrégat = faire les enfants d'abord, le
 * parent ensuite : sans {@code ON DELETE CASCADE} dans le schéma, c'est au
 * DAO de connaître l'ordre (contrainte de référence oblige).
 */
public final class LineDaoJdbc implements LineDao {

    private static final String SQL_INSERER_LIGNE = "INSERT INTO lignes (nom) VALUES (?)";
    private static final String SQL_INSERER_ARRET =
            "INSERT INTO ligne_arrets (nom_ligne, code_station, position) VALUES (?, ?, ?)";
    private static final String SQL_LIGNE_EXISTE = "SELECT 1 FROM lignes WHERE nom = ?";
    private static final String SQL_ARRETS_DUNE_LIGNE =
            "SELECT code_station FROM ligne_arrets WHERE nom_ligne = ? ORDER BY position";
    private static final String SQL_TOUS = "SELECT nom FROM lignes ORDER BY nom";
    private static final String SQL_SUPPRIMER_ARRETS = "DELETE FROM ligne_arrets WHERE nom_ligne = ?";
    private static final String SQL_SUPPRIMER_LIGNE = "DELETE FROM lignes WHERE nom = ?";
    private static final String SQL_PAR_STATION =
            "SELECT DISTINCT l.nom FROM lignes l "
            + "JOIN ligne_arrets a ON l.nom = a.nom_ligne "
            + "WHERE a.code_station = ? ORDER BY l.nom";

    private final Connection connection;

    /**
     * @param connection connexion JDBC ouverte — prêtée, jamais fermée par ce DAO
     */
    public LineDaoJdbc(Connection connection) {
        this.connection = connection;
    }

    @Override
    public void inserer(Line line) {
        try {
            try (PreparedStatement ps = connection.prepareStatement(SQL_INSERER_LIGNE)) {
                ps.setString(1, line.nom());
                ps.executeUpdate();
            }
            // Puis les enfants, dans l'ordre du record : position = index dans stationCodes.
            try (PreparedStatement ps = connection.prepareStatement(SQL_INSERER_ARRET)) {
                List<String> codes = line.stationCodes();
                for (int i = 0; i < codes.size(); i++) {
                    ps.setString(1, line.nom());
                    ps.setString(2, codes.get(i));
                    ps.setInt(3, i);
                    ps.addBatch();
                }
                ps.executeBatch(); // un seul aller-retour pour N arrêts (cf. ELO 12, optimisation JDBC)
            }
        } catch (SQLException e) {
            throw sansChecked("INSERT INTO lignes (nom=" + line.nom() + ")", e);
        }
    }

    @Override
    public Optional<Line> trouverParId(String nom) {
        try {
            if (!lignePresente(nom)) {
                return Optional.empty();
            }
            return Optional.of(reconstruire(nom));
        } catch (SQLException e) {
            throw sansChecked("SELECT ligne (nom=" + nom + ")", e);
        }
    }

    @Override
    public List<Line> trouverTous() {
        try (PreparedStatement ps = connection.prepareStatement(SQL_TOUS)) {
            // On collecte d'abord les noms : reconstruire pendant la lecture du premier
            // ResultSet réutiliserait la même connexion en cours de requête — fragile.
            List<String> noms = new ArrayList<>();
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    noms.add(rs.getString("nom"));
                }
            }
            List<Line> resultat = new ArrayList<>();
            for (String nom : noms) {
                resultat.add(reconstruire(nom));
            }
            // C’est un N+1 (1 requête + 1 par ligne). Inoffensif en démo locale,
            // mortel en production — d'où les pools, le batching et JPA « pour aller plus loin ».
            return List.copyOf(resultat);
        } catch (SQLException e) {
            throw sansChecked("SELECT toutes les lignes", e);
        }
    }

    @Override
    public void mettreAJour(Line line) {
        // Le nom est la clé primaire : il ne change pas. « Mettre à jour la ligne A »,
        // c'est remplacer ses arrêts — delete des enfants + re-insert (pattern classique).
        try {
            if (!lignePresente(line.nom())) {
                throw new IllegalStateException("La ligne « " + line.nom() + " » n'existe pas");
            }
            try (PreparedStatement ps = connection.prepareStatement(SQL_SUPPRIMER_ARRETS)) {
                ps.setString(1, line.nom());
                ps.executeUpdate();
            }
            try (PreparedStatement ps = connection.prepareStatement(SQL_INSERER_ARRET)) {
                List<String> codes = line.stationCodes();
                for (int i = 0; i < codes.size(); i++) {
                    ps.setString(1, line.nom());
                    ps.setString(2, codes.get(i));
                    ps.setInt(3, i);
                    ps.addBatch();
                }
                ps.executeBatch();
            }
        } catch (SQLException e) {
            throw sansChecked("UPDATE ligne (nom=" + line.nom() + ")", e);
        }
    }

    @Override
    public void supprimer(String nom) {
        try {
            // Les enfants d'abord : ligne_arrets référence lignes.nom (PK composée).
            try (PreparedStatement ps = connection.prepareStatement(SQL_SUPPRIMER_ARRETS)) {
                ps.setString(1, nom);
                ps.executeUpdate();
            }
            try (PreparedStatement ps = connection.prepareStatement(SQL_SUPPRIMER_LIGNE)) {
                ps.setString(1, nom);
                ps.executeUpdate();
            }
        } catch (SQLException e) {
            throw sansChecked("DELETE ligne (nom=" + nom + ")", e);
        }
    }

    @Override
    public List<Line> trouverParStation(String codeStation) {
        try (PreparedStatement ps = connection.prepareStatement(SQL_PAR_STATION)) {
            ps.setString(1, codeStation);
            List<String> noms = new ArrayList<>();
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    noms.add(rs.getString("nom"));
                }
            }
            List<Line> resultat = new ArrayList<>();
            for (String nom : noms) {
                resultat.add(reconstruire(nom));
            }
            return List.copyOf(resultat);
        } catch (SQLException e) {
            throw sansChecked("SELECT lignes desservant " + codeStation, e);
        }
    }

    @Override
    public void ajouterArret(String lineName, String stationCode, int position) {
        try (PreparedStatement ps = connection.prepareStatement(SQL_INSERER_ARRET)) {
            ps.setString(1, lineName);
            ps.setString(2, stationCode);
            ps.setInt(3, position);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw sansChecked("INSERT arrêt " + stationCode + " sur la ligne " + lineName, e);
        }
    }

    /** Sonde : la table {@code lignes} contient-elle ce nom ? */
    private boolean lignePresente(String nom) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(SQL_LIGNE_EXISTE)) {
            ps.setString(1, nom);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    /** Reconstruit l'agrégat : le nom connu, on relit les arrêts triés par position. */
    private Line reconstruire(String nom) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(SQL_ARRETS_DUNE_LIGNE)) {
            ps.setString(1, nom);
            List<String> codes = new ArrayList<>();
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    codes.add(rs.getString("code_station"));
                }
            }
            return new Line(nom, codes);
        }
    }

    /** Enveloppe la {@code SQLException} vérifiée — même politique que {@link StationDaoJdbc}. */
    private static RuntimeException sansChecked(String action, SQLException cause) {
        return new IllegalStateException("Opération JDBC échouée : " + action, cause);
    }
}
