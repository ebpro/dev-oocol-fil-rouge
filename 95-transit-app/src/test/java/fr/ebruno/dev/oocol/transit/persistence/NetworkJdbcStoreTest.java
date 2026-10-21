package fr.ebruno.dev.oocol.transit.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import fr.ebruno.dev.oocol.transit.domain.Network;
import fr.ebruno.dev.oocol.transit.domain.NetworkException;
import fr.ebruno.dev.oocol.transit.domain.Station;

/**
 * Tests du store JDBC — la scène du module 90 rejouée à l'échelle de
 * l'agrégat : sauver, recharger, prouver que rien ne s'est perdu en route.
 *
 * <p>Chaque test ouvre sa PROPRE base H2 en mémoire (nom UUID) : isolation
 * totale, aucun test ne peut voir les tables d'un voisin — et rien ne
 * survit à la JVM.
 */
@DisplayName("NetworkJdbcStore — aller-retour complet, ordre inclus")
class NetworkJdbcStoreTest {

    private Connection conn;

    @BeforeEach
    void ouvrirBase() throws SQLException {
        conn = DriverManager.getConnection(
                "jdbc:h2:mem:test95-" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1");
        NetworkJdbcStore.creerSchema(conn);
    }

    @AfterEach
    void fermerBase() throws SQLException {
        conn.close();
    }

    /** Le réseau fixture : 2 zones, 3 stations, 1 ligne de 3 arrêts — assez pour tout casser. */
    private Network reseau() throws NetworkException {
        return Network.builder()
                .zone(1, "Centre-ville")
                .zone(2, "Périphérie")
                .station("GAR", "Gare Centrale", 1, true)
                .station("UNI", "Université", 1, true)
                .station("HOP", "Hôpital Sud", 2, false)
                .ligne("Ligne A", "GAR", "UNI", "HOP")
                .construire();
    }

    @Test
    @DisplayName("sauvegarder puis charger : même nombre de stations")
    void sauvegarderPuisChargerMemeCompte() throws Exception {
        Network original = reseau();
        NetworkJdbcStore.sauvegarder(original, conn);
        Network reload = NetworkJdbcStore.chargerDepuisBase(conn);
        assertThat(reload.stations()).hasSameSizeAs(original.stations());
        assertThat(reload.lignes()).hasSameSizeAs(original.lignes());
    }

    @Test
    @DisplayName("L'ordre des arrêts survit au round-trip (colonne position, leçon W12)")
    void ordreDesArretsSurvit() throws Exception {
        NetworkJdbcStore.sauvegarder(reseau(), conn);
        Network reload = NetworkJdbcStore.chargerDepuisBase(conn);
        assertThat(reload.ligne("Ligne A").orElseThrow().arrets())
                .extracting(Station::code)
                .containsExactly("GAR", "UNI", "HOP");
    }

    @Test
    @DisplayName("Un réseau vide se sauvegarde et se recharge sans erreur")
    void reseauVideRoundTrip() throws Exception {
        NetworkJdbcStore.sauvegarder(Network.builder().construire(), conn);
        Network reload = NetworkJdbcStore.chargerDepuisBase(conn);
        assertThat(reload.zones()).isEmpty();
        assertThat(reload.stations()).isEmpty();
        assertThat(reload.lignes()).isEmpty();
    }

    @Test
    @DisplayName("Les zones survivent au round-trip (numéro ET nom)")
    void zonesSurvivent() throws Exception {
        Network original = reseau();
        NetworkJdbcStore.sauvegarder(original, conn);
        Network reload = NetworkJdbcStore.chargerDepuisBase(conn);
        assertThat(reload.zones())
                .containsExactlyInAnyOrderElementsOf(original.zones());
    }

    @Test
    @DisplayName("Identité structurelle : stations rechargées == stations construites")
    void stationsStructurellementIdentiques() throws Exception {
        // Comparaison INDÉPENDANTE DE L'ORDRE : la base rend un ORDER BY code,
        // le Builder un ordre de déclaration — l'égalité des ensembles, elle,
        // est celle des records (code, nom, zone résolue, accessible).
        Network original = reseau();
        NetworkJdbcStore.sauvegarder(original, conn);
        Network reload = NetworkJdbcStore.chargerDepuisBase(conn);
        assertThat(reload.stations())
                .containsExactlyInAnyOrderElementsOf(original.stations());
    }

    @Test
    @DisplayName("sauvegarder est un REMPLACEMENT, pas un ajout (double appel = mêmes comptes)")
    void sauvegarderEstIdempotent() throws Exception {
        Network reseau = reseau();
        NetworkJdbcStore.sauvegarder(reseau, conn);
        NetworkJdbcStore.sauvegarder(reseau, conn);
        Network reload = NetworkJdbcStore.chargerDepuisBase(conn);
        assertThat(reload.stations()).hasSize(3);
        assertThat(reload.lignes()).hasSize(1);
    }
}
