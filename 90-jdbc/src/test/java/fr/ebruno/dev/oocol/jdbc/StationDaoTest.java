package fr.ebruno.dev.oocol.jdbc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import fr.ebruno.dev.oocol.jdbc.dao.StationDao;
import fr.ebruno.dev.oocol.jdbc.dao.impl.StationDaoJdbc;
import fr.ebruno.dev.oocol.jdbc.domain.Station;
import fr.ebruno.dev.oocol.jdbc.schema.DatabaseSchema;

/**
 * Le DAO des stations, testé contre H2 en mémoire — pas de mock : le sujet
 * du module EST le SQL, le mocked SQL ne prouverait rien.
 *
 * <p>Isolation : chaque test ouvre sa PROPRE base nommée
 * ({@code jdbc:h2:mem:stations-N}) et la referme. {@code DB_CLOSE_DELAY=-1}
 * garderait la base vivante dans la JVM : sans nom unique, les tests se
 * marcheraient dessus — c'est aussi une leçon JDBC.
 */
@DisplayName("StationDaoJdbc — CRUD, mapping et requêtes paramétrées")
class StationDaoTest {

    private static final AtomicInteger COMPTEUR = new AtomicInteger();

    private Connection conn;
    private StationDao dao;

    @BeforeEach
    void creerBaseEtSeed() throws SQLException {
        String url = "jdbc:h2:mem:stations-" + COMPTEUR.incrementAndGet() + ";DB_CLOSE_DELAY=-1";
        conn = DriverManager.getConnection(url, "sa", "");
        DatabaseSchema.creer(conn);
        DatabaseSchema.insererZonesInit(conn);
        dao = new StationDaoJdbc(conn);
        dao.inserer(new Station("HUB", "Gare Centrale", 1, true));
        dao.inserer(new Station("PAR", "Parc Verdun", 1, true));
        dao.inserer(new Station("UNI", "Campus", 2, false));
        dao.inserer(new Station("MAR", "Vieux Marché", 2, true));
        dao.inserer(new Station("PLA", "Plage Sud", 3, true));
    }

    @AfterEach
    void fermerConnexion() throws SQLException {
        conn.close();
    }

    @Test
    @DisplayName("inserer puis trouverParId : les quatre champs font l'aller-retour domaine → table → domaine")
    void insererEtRelire() {
        dao.inserer(new Station("GAR", "Gare Maritime", 3, true));

        Optional<Station> relu = dao.trouverParId("GAR");

        assertThat(relu).contains(new Station("GAR", "Gare Maritime", 3, true));
    }

    @Test
    @DisplayName("trouverParId d'un code inconnu : Optional.empty, jamais un plantage ni un null")
    void introuvableEstVide() {
        assertThat(dao.trouverParId("XXX")).isEmpty();
    }

    @Test
    @DisplayName("trouverTous : les cinq stations du seed, dans l'ordre des codes")
    void trouverTousRetourneLeSeed() {
        List<Station> toutes = dao.trouverTous();

        assertThat(toutes).hasSize(5)
                .extracting(Station::code)
                .containsExactly("HUB", "MAR", "PAR", "PLA", "UNI");
    }

    @Test
    @DisplayName("mettreAJour : le nom change, la clé sert à cibler la ligne")
    void mettreAJourChangeLeNom() {
        dao.mettreAJour(new Station("UNI", "Campus Lenche", 2, false));

        assertThat(dao.trouverParId("UNI"))
                .get()
                .extracting(Station::nom)
                .isEqualTo("Campus Lenche");
    }

    @Test
    @DisplayName("supprimer : la ligne disparaît de la table")
    void supprimerFaitDisparaitre() {
        dao.supprimer("PLA");

        assertThat(dao.trouverParId("PLA")).isEmpty();
        assertThat(dao.trouverTous()).hasSize(4);
    }

    @Test
    @DisplayName("trouverParZone : WHERE zone_numero = ? filtre côté base, pas côté Java")
    void trouverParZoneFiltre() {
        assertThat(dao.trouverParZone(1))
                .extracting(Station::code)
                .containsExactly("HUB", "PAR");
        assertThat(dao.trouverParZone(3))
                .extracting(Station::code)
                .containsExactly("PLA");
        assertThat(dao.trouverParZone(99)).isEmpty();
    }

    @Test
    @DisplayName("chercherParNom : LIKE %motif% passe par un paramètre ? — le motif n'est jamais concaténé")
    void chercherParNomFaitUnLike() {
        assertThat(dao.chercherParNom("Gare"))
                .extracting(Station::code)
                .containsExactly("HUB");
        assertThat(dao.chercherParNom("zzz")).isEmpty();
        assertThat(dao.chercherParNom("r"))
                .extracting(Station::code)
                .containsExactly("HUB", "MAR", "PAR");
    }

    @Test
    @DisplayName("existe : true pour une clé présente, false pour une clé absente")
    void existeVraiFaux() {
        assertThat(dao.existe("HUB")).isTrue();
        assertThat(dao.existe("XXX")).isFalse();
    }

    @Test
    @DisplayName("PK violée : la SQLException remonte, enveloppée dans une RuntimeException (cause intacte)")
    void doublonLeveUneErreur() {
        assertThatThrownBy(() -> dao.inserer(new Station("HUB", "Gare bis", 1, true)))
                .isInstanceOf(RuntimeException.class)
                .hasRootCauseInstanceOf(SQLException.class);
    }

    @Test
    @DisplayName("FK violée : une station en zone 42, inexistante dans zones, est refusée par la base")
    void zoneInexistanteEstRefusee() {
        assertThatThrownBy(() -> dao.inserer(new Station("LOST", "Terminus Perdu", 42, true)))
                .isInstanceOf(RuntimeException.class)
                .hasRootCauseInstanceOf(SQLException.class);

        assertThat(dao.existe("LOST")).isFalse();
    }

    @Test
    @DisplayName("accessible=false fait l'aller-retour : le BOOLEAN JDBC ↔ boolean Java ne se perd pas")
    void booleenFaitLAllerRetour() {
        assertThat(dao.trouverParId("UNI"))
                .get()
                .extracting(Station::accessible)
                .isEqualTo(false);
    }
}
