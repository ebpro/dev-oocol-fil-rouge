package fr.ebruno.dev.oocol.jdbc;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import fr.ebruno.dev.oocol.jdbc.dao.LineDao;
import fr.ebruno.dev.oocol.jdbc.dao.StationDao;
import fr.ebruno.dev.oocol.jdbc.dao.impl.LineDaoJdbc;
import fr.ebruno.dev.oocol.jdbc.dao.impl.StationDaoJdbc;
import fr.ebruno.dev.oocol.jdbc.domain.Line;
import fr.ebruno.dev.oocol.jdbc.domain.Station;
import fr.ebruno.dev.oocol.jdbc.schema.DatabaseSchema;

/**
 * Le DAO des lignes — l'agrégat réparti sur deux tables
 * ({@code lignes} + {@code ligne_arrets} ordinalisée par {@code position}).
 */
@DisplayName("LineDaoJdbc — l'agrégat ligne/arrêts sur sa table-jonction")
class LineDaoTest {

    private static final AtomicInteger COMPTEUR = new AtomicInteger();

    private Connection conn;
    private StationDao stationDao;
    private LineDao dao;

    @BeforeEach
    void creerBaseEtSeed() throws SQLException {
        String url = "jdbc:h2:mem:lignes-" + COMPTEUR.incrementAndGet() + ";DB_CLOSE_DELAY=-1";
        conn = DriverManager.getConnection(url, "sa", "");
        DatabaseSchema.creer(conn);
        DatabaseSchema.insererZonesInit(conn);
        stationDao = new StationDaoJdbc(conn);
        stationDao.inserer(new Station("HUB", "Hub Central", 1, true));
        stationDao.inserer(new Station("PAR", "Parc Verdun", 1, true));
        stationDao.inserer(new Station("UNI", "Campus", 2, false));
        stationDao.inserer(new Station("MAR", "Vieux Marché", 2, true));
        dao = new LineDaoJdbc(conn);
    }

    @AfterEach
    void fermerConnexion() throws SQLException {
        conn.close();
    }

    @Test
    @DisplayName("inserer une ligne de 3 arrêts puis la relire : l'ordre de parcours est un invariant de la table-jonction")
    void insererAvecTroisArretsPuisRelire() {
        dao.inserer(new Line("A", List.of("HUB", "PAR", "UNI")));

        assertThat(dao.trouverParId("A"))
                .get()
                .extracting(Line::stationCodes)
                .isEqualTo(List.of("HUB", "PAR", "UNI"));
    }

    @Test
    @DisplayName("trouverParId d'une ligne inconnue : Optional.empty")
    void ligneInconnueEstVide() {
        assertThat(dao.trouverParId("Z")).isEmpty();
    }

    @Test
    @DisplayName("trouverParStation : la jointure retrouve les lignes qui desservent un arrêt")
    void trouverParStationRetourneLesLignes() {
        dao.inserer(new Line("A", List.of("HUB", "PAR")));
        dao.inserer(new Line("B", List.of("HUB", "UNI")));
        dao.inserer(new Line("C", List.of("MAR", "UNI")));

        assertThat(dao.trouverParStation("HUB"))
                .extracting(Line::nom)
                .containsExactly("A", "B");
        assertThat(dao.trouverParStation("MAR"))
                .extracting(Line::nom)
                .containsExactly("C");
        assertThat(dao.trouverParStation("XXX")).isEmpty();
    }

    @Test
    @DisplayName("ajouterArret : la ligne grandit, position fait l'ordre")
    void ajouterArretAgranditLaLigne() {
        dao.inserer(new Line("A", List.of("HUB", "PAR")));

        dao.ajouterArret("A", "UNI", 2);

        assertThat(dao.trouverParId("A"))
                .get()
                .extracting(Line::stationCodes)
                .isEqualTo(List.of("HUB", "PAR", "UNI"));
    }

    @Test
    @DisplayName("supprimer une ligne nettoie la table-jonction — et la station survit, elle est partagée")
    void supprimerLigneNettoieLaJonctionPasLesStations() {
        dao.inserer(new Line("A", List.of("HUB", "PAR")));

        dao.supprimer("A");

        assertThat(dao.trouverParId("A")).isEmpty();
        assertThat(dao.trouverParStation("HUB")).isEmpty();
        // Les stations ne sont pas des sous-objets de la ligne : elles restent.
        assertThat(stationDao.existe("HUB")).isTrue();
        assertThat(stationDao.existe("PAR")).isTrue();
    }

    @Test
    @DisplayName("ligne vide (aucun arrêt) : l'aller-retour préserve la liste vide")
    void ligneVideFaitLAllerRetour() {
        dao.inserer(new Line("NAVETTE", List.of()));

        assertThat(dao.trouverParId("NAVETTE"))
                .get()
                .isEqualTo(new Line("NAVETTE", List.of()));
    }

    @Test
    @DisplayName("mettreAJour remplace les arrêts d'une ligne sans toucher à sa clé")
    void mettreAJourRemplaceLesArrets() {
        dao.inserer(new Line("A", List.of("HUB", "PAR", "UNI")));

        dao.mettreAJour(new Line("A", List.of("MAR", "HUB")));

        assertThat(dao.trouverParId("A"))
                .get()
                .extracting(Line::stationCodes)
                .isEqualTo(List.of("MAR", "HUB"));
    }

    @Test
    @DisplayName("trouverTous : toutes les lignes reconstruites, triées par nom")
    void trouverTousRetourneToutesLesLignes() {
        dao.inserer(new Line("B", List.of("HUB", "UNI")));
        dao.inserer(new Line("A", List.of("PAR")));

        assertThat(dao.trouverTous())
                .extracting(Line::nom)
                .containsExactly("A", "B");
    }
}
