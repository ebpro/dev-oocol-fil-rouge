package fr.ebruno.dev.oocol.heritage.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.lang.reflect.Modifier;

import org.junit.jupiter.api.Test;

/**
 * Tests de {@link Vehicle} — la classe abstraite : interdiction
 * d'instancier, gabarit partagé, polymorphisme de l'annonce.
 *
 * <p><strong>Compteur statique :</strong> même discipline qu'au module 20 —
 * les tests partagent la JVM : on ne compare JAMAIS le compteur à une valeur
 * absolue, mais à un <em>delta</em> mesuré avant/après.
 *
 * @author Emmanuel Bruno
 */
class VehicleTest {

    private static MetroTrain metroStandard() {
        return new MetroTrain("M-001", 180, 80.0, 3, false);
    }

    @Test
    void classeVehicleEstAbstraite() {
        // Preuve par réflexion : le MODIFICATEUR abstract est bien présent —
        // « new Vehicle(...) » est impossible à la compilation, et ceci le vérifie.
        assertThat(Modifier.isAbstract(Vehicle.class.getModifiers())).isTrue();
    }

    @Test
    void annoncerDuMetroContientLeTroisiemeRail() {
        // annoncer() est écrite dans le PARENT, decrirePropulsion() dans le FILS :
        // l'annonce du métro prouve que le parent a appelé la méthode abstraite
        // sur l'objet réel (dispatch virtuel).
        assertThat(metroStandard().annoncer())
                .contains("M-001")
                .contains("3rd rail")
                .contains("180 places");
    }

    @Test
    void annoncerDuBusDieselContientDiesel() {
        Bus bus = new Bus("B-001", 90, 50.0, false, 2);

        assertThat(bus.annoncer()).contains("Diesel");
        // Le même code parent, un autre enfant : pas de « 3rd rail » ici.
        assertThat(bus.annoncer()).doesNotContain("3rd rail");
    }

    @Test
    void estPlusRapideQueCompareSurLeTypeLarge() {
        MetroTrain rapide = new MetroTrain("M-002", 180, 90.0, 3, false);
        Bus lent = new Bus("B-002", 90, 50.0, true, 3);

        // Le paramètre est typé Vehicle : Bus convient aussi bien qu'un MetroTrain
        // (substituabilité LSP — la méthode ne regarde que vitesseMaxKmh, garanti par le gabarit).
        assertThat(rapide.estPlusRapideQue(lent)).isTrue();
        assertThat(lent.estPlusRapideQue(rapide)).isFalse();
        // Vitesses égales : strictement plus rapide = non.
        Bus jumeau = new Bus("B-003", 90, 50.0, false, 2);
        assertThat(lent.estPlusRapideQue(jumeau)).isFalse();
    }

    @Test
    void compteurStatiqueAugmenteAChaqueCreation() {
        int avant = Vehicle.nombreVehicules();

        new MetroTrain("M-010", 180, 80.0, 3, false);
        new Bus("B-010", 90, 50.0, false, 2);
        new Tramway("T-010", 210, 70.0, 33.0, 0.5);

        // +3 : les trois sous-classes exécutent le constructeur du parent (super(...)),
        // donc incrémentent TOUTES le même compteur de classe.
        assertThat(Vehicle.nombreVehicules()).isEqualTo(avant + 3);
    }

    @Test
    void capaciteNulleOuNegativeRejetee() {
        assertThatThrownBy(() -> new MetroTrain("M-ERR", 0, 80.0, 3, false))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("capacité");
        assertThatThrownBy(() -> new Bus("B-ERR", -5, 50.0, false, 2))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void vitesseMaximaleNonPositiveRejetee() {
        assertThatThrownBy(() -> new MetroTrain("M-ERR", 100, 0.0, 3, false))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("vitesse");
    }

    @Test
    void immatriculationBlancheRejetee() {
        assertThatThrownBy(() -> new Bus("   ", 90, 50.0, false, 2))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("immatriculation");
    }
}
