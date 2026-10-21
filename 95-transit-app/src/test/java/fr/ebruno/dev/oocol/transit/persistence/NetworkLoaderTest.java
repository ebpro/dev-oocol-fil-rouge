package fr.ebruno.dev.oocol.transit.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import fr.ebruno.dev.oocol.transit.domain.Network;
import fr.ebruno.dev.oocol.transit.domain.NetworkException;

/**
 * Tests du loader CSV — format et contrat identiques au module 60, mais la
 * construction passe désormais par le Builder : les règles de cohérence
 * référentielle ne vivent plus À DEUX ENDROITS (leçon SRP du module 95).
 */
@DisplayName("NetworkLoader — trois CSV, un Builder, un réseau")
class NetworkLoaderTest {

    @TempDir
    Path dossier;

    /** Écrit le trio de fichiers au format W7 ({@code ;}, commentaires {@code #}, UTF-8). */
    private void ecrireTrio() throws IOException {
        Files.writeString(dossier.resolve("zones.csv"), """
                # zones
                1;Centre-ville
                2;Périphérie
                """, StandardCharsets.UTF_8);
        Files.writeString(dossier.resolve("stations.csv"), """
                # stations
                GAR;Gare Centrale;1;true
                UNI;Université;1;true
                HOP;Hôpital Sud;2;false
                """, StandardCharsets.UTF_8);
        Files.writeString(dossier.resolve("lignes.csv"), """
                # lignes
                Ligne A;GAR;UNI;HOP
                """, StandardCharsets.UTF_8);
    }

    @Test
    @DisplayName("Le trio valide se charge avec les bons comptes")
    void chargementValide() throws IOException, NetworkException {
        ecrireTrio();
        Network n = NetworkLoader.chargerDepuisRepertoire(dossier);
        assertThat(n.zones()).hasSize(2);
        assertThat(n.stations()).hasSize(3);
        assertThat(n.lignes()).hasSize(1);
        assertThat(n.ligne("Ligne A").orElseThrow().arrets())
                .extracting(s -> s.code()).containsExactly("GAR", "UNI", "HOP");
        assertThat(n.station("HOP").orElseThrow().zone().numero()).isEqualTo(2);
    }

    @Test
    @DisplayName("Équivalence CSV == Builder : deux voies, un seul réseau")
    void equivalenceAvecConstructionDirecte() throws IOException, NetworkException {
        ecrireTrio();
        Network viaCsv = NetworkLoader.chargerDepuisRepertoire(dossier);
        Network viaBuilder = Network.builder()
                .zone(1, "Centre-ville")
                .zone(2, "Périphérie")
                .station("GAR", "Gare Centrale", 1, true)
                .station("UNI", "Université", 1, true)
                .station("HOP", "Hôpital Sud", 2, false)
                .ligne("Ligne A", "GAR", "UNI", "HOP")
                .construire();
        assertThat(viaCsv.stations())
                .containsExactlyInAnyOrderElementsOf(viaBuilder.stations());
        assertThat(viaCsv.lignes())
                .containsExactlyInAnyOrderElementsOf(viaBuilder.lignes());
    }

    @Test
    @DisplayName("Un fichier absent du trio lève NetworkException (message nomme le fichier)")
    void fichierManquantLeve() throws IOException {
        Files.writeString(dossier.resolve("zones.csv"), "1;Centre-ville\n",
                StandardCharsets.UTF_8);
        // stations.csv et lignes.csv manquent délibérément.
        assertThatThrownBy(() -> NetworkLoader.chargerDepuisRepertoire(dossier))
                .isInstanceOf(NetworkException.class)
                .hasMessageContaining("stations.csv");
    }

    @Test
    @DisplayName("Une ligne d'arrêt au mauvais nombre de champs lève NetworkException")
    void ligneMalformeeLeve() throws IOException {
        Files.writeString(dossier.resolve("zones.csv"), "1;Centre-ville\n",
                StandardCharsets.UTF_8);
        Files.writeString(dossier.resolve("stations.csv"), "GAR;Gare Centrale;1\n",
                StandardCharsets.UTF_8); // 3 champs, attendu : 4
        Files.writeString(dossier.resolve("lignes.csv"), "Ligne A;GAR\n",
                StandardCharsets.UTF_8);
        assertThatThrownBy(() -> NetworkLoader.chargerDepuisRepertoire(dossier))
                .isInstanceOf(NetworkException.class)
                .hasMessageContaining("Ligne d'arrêt attendue");
    }

    @Test
    @DisplayName("Une zone inconnue dans le CSV est refusée PAR LE BUILDER (règle unique)")
    void zoneInconnueDansCsvLeve() throws IOException {
        Files.writeString(dossier.resolve("zones.csv"), "1;Centre-ville\n",
                StandardCharsets.UTF_8);
        Files.writeString(dossier.resolve("stations.csv"), "GAR;Gare Centrale;9;true\n",
                StandardCharsets.UTF_8);
        Files.writeString(dossier.resolve("lignes.csv"), "Ligne A;GAR\n",
                StandardCharsets.UTF_8);
        assertThatThrownBy(() -> NetworkLoader.chargerDepuisRepertoire(dossier))
                .isInstanceOf(NetworkException.class)
                .hasMessageContaining("zone inconnue");
    }

    @Test
    @DisplayName("Le NumberFormatException CSV survit comme cause (chaînage du module 50)")
    void nombreInvalideChaineLaCause() throws IOException {
        Files.writeString(dossier.resolve("zones.csv"), "abc;Centre-ville\n",
                StandardCharsets.UTF_8);
        assertThatThrownBy(() -> NetworkLoader.chargerDepuisRepertoire(dossier))
                .isInstanceOf(NetworkException.class)
                .hasCauseInstanceOf(NumberFormatException.class);
    }
}
