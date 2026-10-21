package fr.ebruno.dev.oocol.bases.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

/**
 * Tests de {@link Station} — la classe centrale : encapsulation avec
 * validation, surcharge de constructeurs, compteur statique.
 *
 * <p><strong>Attention au compteur statique :</strong> les tests partagent
 * la même JVM et s'exécutent dans un ordre non garanti. On ne compare donc
 * JAMAIS {@code nombreStations()} à un chiffre absolu (fragile) mais à un
 * <em>delta</em> mesuré avant/après la création — seule assertion honnête
 * sur un état partagé.
 *
 * @author Emmanuel Bruno
 */
class StationTest {

    private static Zone zone1() {
        return new Zone(1, "Centre-ville");
    }

    @Test
    void surchargeConstructeurAccessibleParDefaut() {
        // Version courte (3 arguments) : la station n'est PAS accessible.
        Station universite = new Station("Universite", "UNI", zone1());

        assertThat(universite.isAccessible()).isFalse();
        assertThat(universite.estAccessible()).isFalse(); // l'alias français lit le même champ
    }

    @Test
    void constructeurCompletDefinitAccessible() {
        // Version longue (4 arguments) : accessibilité explicite.
        Station gare = new Station("Gare Centrale", "GAR", zone1(), true);

        assertThat(gare.getNom()).isEqualTo("Gare Centrale");
        assertThat(gare.getCode()).isEqualTo("GAR");
        assertThat(gare.estAccessible()).isTrue();
        assertThat(gare.getZone().getNumero()).isEqualTo(1);
    }

    @Test
    void codeMinusculeRejete() {
        assertThatThrownBy(() -> new Station("Fausse Gare", "gar", zone1()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("3 lettres majuscules");
    }

    @Test
    void codeTropLongRejete() {
        assertThatThrownBy(() -> new Station("Fausse Gare", "GARE", zone1()))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Station("Fausse Gare", "GA", zone1()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void nomVideRejete() {
        assertThatThrownBy(() -> new Station("  ", "GAR", zone1()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void setZoneChangeLaZone() {
        Station hopital = new Station("Hopital", "HOP", zone1());
        Zone zone2 = new Zone(2, "Peripherie");

        hopital.setZone(zone2); // re-zonage : la station suit le découpage tarifaire

        assertThat(hopital.getZone()).isSameAs(zone2);
        assertThat(hopital.getZone().getNumero()).isEqualTo(2);
    }

    @Test
    void compteurStatiqueAugmenteAChaqueCreation() {
        int avant = Station.nombreStations();

        new Station("Station Alpha", "SAA", zone1());
        new Station("Station Beta", "SBB", zone1(), true);

        // Assertion en DELTA : +2 stations créées, peu importe combien
        // d'autres tests en ont déjà créé dans cette JVM.
        assertThat(Station.nombreStations()).isEqualTo(avant + 2);
    }

    @Test
    void toStringContientCodeNomEtZone() {
        Station gare = new Station("Gare Centrale", "GAR", zone1(), true);

        String libelle = gare.toString();
        assertThat(libelle).contains("GAR").contains("Gare Centrale").contains("Zone 1");
        // Le pictogramme ♿ (U+267F) n'apparaît que si accessible.
        assertThat(libelle).contains("\u267F");
        assertThat(new Station("Hopital", "HOP", zone1()).toString()).doesNotContain("\u267F");
    }
}
