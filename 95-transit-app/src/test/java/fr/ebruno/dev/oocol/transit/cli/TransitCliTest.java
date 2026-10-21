package fr.ebruno.dev.oocol.transit.cli;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import fr.ebruno.dev.oocol.transit.app.TransitApp;
import fr.ebruno.dev.oocol.transit.domain.Network;
import fr.ebruno.dev.oocol.transit.domain.NetworkException;

/**
 * Tests de la CLI — la preuve vivante du choix « String au lieu de
 * System.out » : chaque commande s'asserte comme une fonction pure, sans
 * capturer le moindre flux.
 */
@DisplayName("TransitCli — commandes testables, sorties déterministes")
class TransitCliTest {

    /** Le réseau de la démo sert aussi de fixture : mêmes données, deux clients. */
    private static Network reseau;

    @BeforeAll
    static void construire() throws NetworkException {
        reseau = TransitApp.reseauDemo();
    }

    @Test
    @DisplayName("cmdListerStations contient codes, noms et zones de chaque arrêt")
    void cmdListerStations() {
        String sortie = TransitCli.cmdListerStations(reseau);
        assertThat(sortie).contains("Arrêts du réseau (6)");
        assertThat(sortie).contains("GAR — Gare Centrale (zone 1, accessible)");
        assertThat(sortie).contains("MAR — Marché des Pins (zone 1, non accessible)");
        assertThat(sortie).contains("AER — Aéroport (zone 3, accessible)");
    }

    @Test
    @DisplayName("cmdListerLignes contient chaque ligne et son parcours ordonné")
    void cmdListerLignes() {
        String sortie = TransitCli.cmdListerLignes(reseau);
        assertThat(sortie).contains("Lignes du réseau (3)");
        assertThat(sortie).contains("Ligne A : GAR → UNI → HOP → AER");
        assertThat(sortie).contains("Ligne B : GAR → MAR → PAR");
    }

    @Test
    @DisplayName("cmdChercher sur un code existant nomme l'arrêt")
    void cmdChercherTrouve() {
        String sortie = TransitCli.cmdChercher(reseau, "UNI");
        assertThat(sortie).startsWith("Trouvé :").contains("Université");
    }

    @Test
    @DisplayName("cmdChercher sur un code absent répond Introuvable (pas d'exception)")
    void cmdChercherIntrouvable() {
        String sortie = TransitCli.cmdChercher(reseau, "ZZZ");
        assertThat(sortie).isEqualTo("Introuvable : ZZZ");
    }

    @Test
    @DisplayName("cmdStats affiche les compteurs exacts et la part d'accessibilité")
    void cmdStats() {
        String sortie = TransitCli.cmdStats(reseau);
        assertThat(sortie).contains("Zones       : 3");
        assertThat(sortie).contains("Stations    : 6");
        assertThat(sortie).contains("Lignes      : 3");
        // 5 arrêts accessibles sur 6 → 83 % (division entière, arrondi à l'inférieur).
        assertThat(sortie).contains("Accessibles : 5/6 (83 %)");
    }

    @Test
    @DisplayName("cmdItineraire trouve le chemin le plus court (GAR → MAR → AER, 2 segments)")
    void cmdItineraireChemin() {
        String sortie = TransitCli.cmdItineraire(reseau, "GAR", "AER");
        assertThat(sortie).startsWith("Itinéraire GAR → AER :");
        // Le BFS passe par MAR (Ligne B puis Ligne C), plus court que par HOP.
        assertThat(sortie).contains("GAR → MAR → AER");
        assertThat(sortie).contains("3 arr\u00EAts, 2 segments");
    }

    @Test
    @DisplayName("cmdItineraire signale une extrémité inconnue sans lever")
    void cmdItineraireExtrémitéInconnue() {
        assertThat(TransitCli.cmdItineraire(reseau, "LUNE", "GAR"))
                .isEqualTo("Station de départ inconnue : LUNE");
        assertThat(TransitCli.cmdItineraire(reseau, "GAR", "LUNE"))
                .isEqualTo("Station d'arrivée inconnue : LUNE");
    }

    @Test
    @DisplayName("cmdItineraire sur un réseau débranché répond Aucun itinéraire")
    void cmdItineraireAucunChemin() throws NetworkException {
        Network isole = Network.builder()
                .zone(1, "Centre-ville")
                .station("GAR", "Gare Centrale", 1, true)
                .station("ISO", "Hameau Perdu", 1, false)
                .ligne("Ligne Unique", "GAR")
                .construire();
        assertThat(TransitCli.cmdItineraire(isole, "GAR", "ISO"))
                .isEqualTo("Aucun itinéraire entre GAR et ISO.");
    }
}
