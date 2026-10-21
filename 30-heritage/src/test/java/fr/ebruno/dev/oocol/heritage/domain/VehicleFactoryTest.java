package fr.ebruno.dev.oocol.heritage.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

/**
 * Tests de {@link VehicleFactory} — le patron Factory : le type logique
 * choisit l'espèce concrète, le type rendu reste large, l'inconnu est rejeté.
 *
 * @author Emmanuel Bruno
 */
class VehicleFactoryTest {

    @Test
    void fabriqueCreeUnMetroTrain() {
        Vehicle v = VehicleFactory.creer("metro", "F-M1", 180, 80.0);

        assertThat(v).isInstanceOf(MetroTrain.class);
        // Le standard de la fabrique : 3 voitures, conduite avec machiniste.
        assertThat(((MetroTrain) v).getNombreVoitures()).isEqualTo(3);
    }

    @Test
    void fabriqueCreeUnBus() {
        Vehicle v = VehicleFactory.creer("bus", "F-B1", 90, 50.0);

        assertThat(v).isInstanceOf(Bus.class);
        assertThat(((Bus) v).estHybride()).isFalse();
        assertThat(((Bus) v).getNombrePortes()).isEqualTo(2);
    }

    @Test
    void fabriqueCreeUnTramway() {
        Vehicle v = VehicleFactory.creer("tram", "F-T1", 210, 70.0);

        assertThat(v).isInstanceOf(Tramway.class);
        assertThat(((Tramway) v).getLongueurM()).isEqualTo(33.0);
        assertThat(((Tramway) v).niveauActuel()).isEqualTo(0.8);
    }

    @Test
    void typeInconnuOuAbsentRejete() {
        assertThatThrownBy(() -> VehicleFactory.creer("avion", "F-A1", 100, 900.0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("avion");
        assertThatThrownBy(() -> VehicleFactory.creer(null, "F-N1", 100, 50.0))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void fabriqueAccepteLesCasIndifferents() {
        // Normalisation Locale.ROOT : « METRO » et « Tram » tombent sur les mêmes clés.
        assertThat(VehicleFactory.creer("METRO", "F-M2", 180, 80.0)).isInstanceOf(MetroTrain.class);
        assertThat(VehicleFactory.creer("Tram", "F-T2", 210, 70.0)).isInstanceOf(Tramway.class);
    }

    @Test
    void creationsEnCascadeIncrementeLeCompteurCommun() {
        int avant = Vehicle.nombreVehicules();

        VehicleFactory.creer("metro", "F-M3", 180, 80.0);
        VehicleFactory.creer("bus", "F-B3", 90, 50.0);
        VehicleFactory.creer("tram", "F-T3", 210, 70.0);

        // Delta : +3 — la fabrique passe par les constructeurs concrets, qui
        // exécutent tous le constructeur de Vehicle (compteur de classe partagé).
        assertThat(Vehicle.nombreVehicules()).isEqualTo(avant + 3);
    }
}
