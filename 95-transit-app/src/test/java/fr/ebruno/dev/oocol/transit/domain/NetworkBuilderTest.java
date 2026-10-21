package fr.ebruno.dev.oocol.transit.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Tests du Builder — le contrat de validation est AUSSI important que le
 * chaînement : chaque {@link NetworkException} levée est un réseau bancal
 * qui ne verra jamais le jour.
 */
@DisplayName("NetworkBuilder — construction pas-à-pas et validation fail-fast")
class NetworkBuilderTest {

    @Test
    @DisplayName("Un réseau valide se construit avec les bons comptes")
    void reseauValideComptesCorrects() throws NetworkException {
        Network n = Network.builder()
                .zone(1, "Centre-ville")
                .zone(2, "Périphérie")
                .station("GAR", "Gare Centrale", 1, true)
                .station("HOP", "Hôpital Sud", 2, false)
                .ligne("Ligne A", "GAR", "HOP")
                .construire();

        assertThat(n.zones()).hasSize(2);
        assertThat(n.stations()).hasSize(2);
        assertThat(n.lignes()).hasSize(1);
        // Le numéro paresseux est devenu une vraie instance Zone (partagée, pas recréée).
        assertThat(n.stations().get(0).zone()).isEqualTo(n.zones().get(0));
        // L'ordre de parcours survit à la résolution.
        assertThat(n.ligne("Ligne A").orElseThrow().arrets())
                .extracting(Station::code).containsExactly("GAR", "HOP");
    }

    @Test
    @DisplayName("Une station référençant une zone inconnue est refusée")
    void zoneInconnueLeve() {
        NetworkBuilder b = Network.builder()
                .zone(1, "Centre-ville")
                .station("GAR", "Gare Centrale", 7, true);
        assertThatThrownBy(b::construire)
                .isInstanceOf(NetworkException.class)
                .hasMessageContaining("zone inconnue")
                .hasMessageContaining("GAR");
    }

    @Test
    @DisplayName("Un code d'arrêt dupliqué est refusé")
    void codeStationDupliqueLeve() {
        NetworkBuilder b = Network.builder()
                .zone(1, "Centre-ville")
                .station("GAR", "Gare Centrale", 1, true)
                .station("GAR", "Gare Nord", 1, false);
        assertThatThrownBy(b::construire)
                .isInstanceOf(NetworkException.class)
                .hasMessageContaining("dupliqué")
                .hasMessageContaining("GAR");
    }

    @Test
    @DisplayName("Le réseau vide se construit : 0 zone, 0 station, 0 ligne")
    void reseauVideConstruit() throws NetworkException {
        Network n = Network.builder().construire();
        assertThat(n.zones()).isEmpty();
        assertThat(n.stations()).isEmpty();
        assertThat(n.lignes()).isEmpty();
    }

    @Test
    @DisplayName("construire() est un snapshot : le Builder se réutilise sans corrompre le produit")
    void construireEstUnSnapshot() throws NetworkException {
        NetworkBuilder b = Network.builder()
                .zone(1, "Centre-ville")
                .station("GAR", "Gare Centrale", 1, true)
                .ligne("Ligne A", "GAR");

        Network premier = b.construire();
        Network second = b.construire();
        // Même état → réseaux structurellement identiques…
        assertThat(second.stations()).isEqualTo(premier.stations());
        assertThat(second.lignes()).isEqualTo(premier.lignes());

        // …mais continuer à alimenter le Builder n'affecte PAS le premier.
        b.zone(2, "Périphérie").station("HOP", "Hôpital Sud", 2, true);
        Network troisieme = b.construire();
        assertThat(premier.stations()).hasSize(1);
        assertThat(second.stations()).hasSize(1);
        assertThat(troisieme.stations()).hasSize(2);
        assertThat(troisieme.zones()).hasSize(2);
    }

    @Test
    @DisplayName("Une ligne référençant un arrêt inconnu est refusée")
    void ligneReferencantStationInconnueLeve() {
        NetworkBuilder b = Network.builder()
                .zone(1, "Centre-ville")
                .station("GAR", "Gare Centrale", 1, true)
                .ligne("Ligne A", "GAR", "FANTOME");
        assertThatThrownBy(b::construire)
                .isInstanceOf(NetworkException.class)
                .hasMessageContaining("arrêt inconnu")
                .hasMessageContaining("FANTOME")
                .hasMessageContaining("Ligne A");
    }

    @Test
    @DisplayName("Un nom de ligne dupliqué est refusé")
    void nomLigneDupliqueLeve() {
        NetworkBuilder b = Network.builder()
                .zone(1, "Centre-ville")
                .station("GAR", "Gare Centrale", 1, true)
                .ligne("Ligne A", "GAR")
                .ligne("Ligne A", "GAR");
        assertThatThrownBy(b::construire)
                .isInstanceOf(NetworkException.class)
                .hasMessageContaining("Ligne dupliquée");
    }

    @Test
    @DisplayName("Une zone dupliquée (même numéro deux fois) est refusée")
    void numeroZoneDupliqueLeve() {
        NetworkBuilder b = Network.builder()
                .zone(1, "Centre-ville")
                .zone(1, "Centre-ville bis");
        assertThatThrownBy(b::construire)
                .isInstanceOf(NetworkException.class)
                .hasMessageContaining("Zone dupliquée");
    }
}
