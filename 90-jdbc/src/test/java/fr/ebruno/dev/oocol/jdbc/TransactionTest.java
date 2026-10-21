package fr.ebruno.dev.oocol.jdbc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import fr.ebruno.dev.oocol.jdbc.domain.Station;
import fr.ebruno.dev.oocol.jdbc.repo.NetworkRepository;
import fr.ebruno.dev.oocol.jdbc.schema.DatabaseSchema;

/**
 * Les transactions JDBC : {@code setAutoCommit(false)}, {@code commit()},
 * {@code rollback()} — et le gabarit {@link NetworkRepository#transactionSurement}
 * qui rend le pattern impossible à mal écrire.
 *
 * <p>Le scénario central : deux insertions valides puis un doublon volontaire.
 * Sans transaction, les deux premières seraient quand même passées
 * (autoCommit = chaque instruction est sa propre transaction). Avec, le lot
 * est annulé EN BLOC — c'est le A de ACID, observable au compteur près.
 */
@DisplayName("Transactions — commit, rollback, atomicité")
class TransactionTest {

    private static final AtomicInteger COMPTEUR = new AtomicInteger();

    private Connection conn;
    private NetworkRepository repo;

    @BeforeEach
    void creerBaseEtSeed() throws SQLException {
        String url = "jdbc:h2:mem:transactions-" + COMPTEUR.incrementAndGet() + ";DB_CLOSE_DELAY=-1";
        conn = DriverManager.getConnection(url, "sa", "");
        DatabaseSchema.creer(conn);
        DatabaseSchema.insererZonesInit(conn);
        repo = new NetworkRepository(conn);
        repo.stationDao().inserer(new Station("HUB", "Hub Central", 1, true));
    }

    @AfterEach
    void fermerConnexion() throws SQLException {
        conn.close();
    }

    @Test
    @DisplayName("transaction qui réussit : tout est commité, le compteur augmente")
    void transactionValideeCommite() {
        int avant = repo.stationDao().trouverTous().size();

        repo.transactionSurement(() -> {
            repo.stationDao().inserer(new Station("PAR", "Parc", 1, true));
            repo.stationDao().inserer(new Station("UNI", "Campus", 2, false));
        });

        assertThat(repo.stationDao().trouverTous()).hasSize(avant + 2);
        assertThat(repo.stationDao().existe("PAR")).isTrue();
    }

    @Test
    @DisplayName("doublon en cours d'action : rollback complet, le compteur ne bouge PAS")
    void echecEnCoursDeRouteAnnuleTout() {
        int avant = repo.stationDao().trouverTous().size();

        assertThatThrownBy(() -> repo.transactionSurement(() -> {
            repo.stationDao().inserer(new Station("NOR", "Quai Nord", 1, true));
            repo.stationDao().inserer(new Station("SUD", "Quai Sud", 2, true));
            // Volontairement NOR deux fois : la PK explose sur la troisième instruction.
            repo.stationDao().inserer(new Station("NOR", "Faux doublon", 1, false));
        }))
                .isInstanceOf(RuntimeException.class)
                .hasRootCauseInstanceOf(SQLException.class);

        // NOR et SUD n'auraient jamais dû être visibles : annulés avec le doublon.
        assertThat(repo.stationDao().trouverTous()).hasSize(avant);
        assertThat(repo.stationDao().existe("NOR")).isFalse();
        assertThat(repo.stationDao().existe("SUD")).isFalse();
    }

    @Test
    @DisplayName("mode manuel sans gabarit : inserer + rollback à la main → la station n'existe pas")
    void rollbackManuelAnnuleLInsertion() throws SQLException {
        repo.debrancherTransaction(true);
        assertThat(conn.getAutoCommit()).isFalse();

        repo.stationDao().inserer(new Station("TEMP", "Arrêt éphémère", 3, true));
        conn.rollback();
        conn.setAutoCommit(true);

        assertThat(repo.stationDao().trouverParId("TEMP")).isEmpty();
    }

    @Test
    @DisplayName("autoCommit branché : chaque instruction est validée seule — le comportement par défaut qu'on quitte en transaction")
    void autoCommitValideChaqueInstruction() throws SQLException {
        assertThat(conn.getAutoCommit()).isTrue();

        repo.stationDao().inserer(new Station("AUTO", "Arrêt direct", 1, true));

        // Pas de commit explicite, pas de gabarit : déjà validé, déjà visible.
        assertThat(repo.stationDao().existe("AUTO")).isTrue();
    }

    @Test
    @DisplayName("après un rollback, la connexion reste saine : pas de ressource fuyée, transactions suivantes OK")
    void connexionSurvitAuRollback() {
        assertThatThrownBy(() -> repo.transactionSurement(() -> {
            repo.stationDao().inserer(new Station("X1", "Un", 1, true));
            throw new IllegalStateException("échec simulé en cours d'action");
        })).isInstanceOf(IllegalStateException.class);

        assertThat(repo.stationDao().existe("X1")).isFalse();

        // Une transaction postérieure réussit sur la même connexion — preuve implicite
        // qu'aucun verrou ni ressource n'a fuité du rollback.
        assertThatCode(() -> repo.transactionSurement(
                () -> repo.stationDao().inserer(new Station("X2", "Deux", 1, true))))
                .doesNotThrowAnyException();
        assertThat(repo.stationDao().existe("X2")).isTrue();
    }
}
