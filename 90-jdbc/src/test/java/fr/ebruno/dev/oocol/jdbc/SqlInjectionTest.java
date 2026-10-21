package fr.ebruno.dev.oocol.jdbc;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import fr.ebruno.dev.oocol.jdbc.domain.Station;
import fr.ebruno.dev.oocol.jdbc.dao.impl.StationDaoJdbc;
import fr.ebruno.dev.oocol.jdbc.schema.DatabaseSchema;
import fr.ebruno.dev.oocol.jdbc.security.SqlInjectionDemo;

/**
 * La preuve, pas la promesse : ces tests montrent que la version concaténée
 * EST exploitée et que la version paramétrée NE L'EST PAS, sur la même base,
 * la même requête et la même attaque.
 *
 * <p>Cadre pédagogique : H2 en mémoire, données fictives, attaque connue
 * (tautologie {@code ' OR '1'='1}). Rien ici n'est un mode d'emploi
 * d'intrusion — c'est une démonstration de la frontière code/valeur.
 */
@DisplayName("Injection SQL — concaténation exploitée, paramètre immunisé")
class SqlInjectionTest {

    private static final AtomicInteger COMPTEUR = new AtomicInteger();

    private Connection conn;

    @BeforeEach
    void creerBaseEtSeed() throws SQLException {
        String url = "jdbc:h2:mem:injection-" + COMPTEUR.incrementAndGet() + ";DB_CLOSE_DELAY=-1";
        conn = DriverManager.getConnection(url, "sa", "");
        DatabaseSchema.creer(conn);
        DatabaseSchema.insererZonesInit(conn);
        var dao = new StationDaoJdbc(conn);
        dao.inserer(new Station("HUB", "Hub Central", 1, true));
        dao.inserer(new Station("PAR", "Parc Verdun", 1, true));
        dao.inserer(new Station("UNI", "Campus", 2, false));
    }

    @AfterEach
    void fermerConnexion() throws SQLException {
        conn.close();
    }

    @Test
    @DisplayName("L'ATTAQUE PASSE sur la requête concaténée : le WHERE contourné ramène TOUTE la table")
    void attaqueSurRequeteConcateneeRameneTout() throws SQLException {
        String resultat = SqlInjectionDemo.vulnérable(conn, "' OR '1'='1");

        // Preuve de l'exfiltration : les trois stations fuient, y compris celles
        // que le code demandé ('…' OR …) n'aurait jamais dû désigner.
        assertThat(resultat)
                .contains("HUB|")
                .contains("PAR|")
                .contains("UNI|");
    }

    @Test
    @DisplayName("La MÊME ATTAQUE échoue sur la requête paramétrée : 0 ligne, le SQL n'a pas changé de plan")
    void requeteParametreeNeutraliseLAttaque() throws SQLException {
        String resultat = SqlInjectionDemo.securise(conn, "' OR '1'='1");

        assertThat(resultat).isEmpty();
    }

    @Test
    @DisplayName("Usage normal sur la requête paramétrée : 'HUB' renvoie bien la ligne attendue")
    void usageNormalFonctionne() throws SQLException {
        String resultat = SqlInjectionDemo.securise(conn, "HUB");

        assertThat(resultat).isEqualTo("HUB|Hub Central");
    }

    @Test
    @DisplayName("Injection par commentaire ('HUB' -- ) : la concaténation l'accepte et redéfinit la requête")
    void injectionParCommentaireFonctionneSurVersionVulnerables() throws SQLException {
        // `--` commente la fin de la ligne SQL : le guillemet fermant disparaît,
        // la requête reste valide — l'attaquant dicte le SQL, pas seulement sa valeur.
        String resultat = SqlInjectionDemo.vulnérable(conn, "HUB' --");

        assertThat(resultat).isEqualTo("HUB|Hub Central");

        // La version saine, elle, cherche littéralement le code « HUB' -- » : rien.
        assertThat(SqlInjectionDemo.securise(conn, "HUB' --")).isEmpty();
    }
}
