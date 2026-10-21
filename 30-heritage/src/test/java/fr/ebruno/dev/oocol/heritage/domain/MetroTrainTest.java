package fr.ebruno.dev.oocol.heritage.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

/**
 * Tests de {@link MetroTrain} — l'enfant modèle : héritage du gabarit,
 * validation propre, retour covariant de {@code copier()}.
 *
 * @author Emmanuel Bruno
 */
class MetroTrainTest {

    @Test
    void copierRetourneUnMetroTrainSansCast() {
        MetroTrain original = new MetroTrain("M-100", 180, 80.0, 4, true);

        // PREUVE DE LA COVARIANCE : la ligne ci-dessous range le résultat dans
        // un MetroTrain sans le moindre cast. Avec le type de retour du parent
        // (Vehicle), elle serait REFUSÉE À LA COMPILATION.
        MetroTrain copie = original.copier();

        assertThat(copie).isNotSameAs(original); // copie = nouvel objet
        assertThat(copie.getImmatriculation()).isEqualTo(original.getImmatriculation());
        assertThat(copie.getCapacitePassagers()).isEqualTo(original.getCapacitePassagers());
        assertThat(copie.getVitesseMaxKmh()).isEqualTo(original.getVitesseMaxKmh());
        assertThat(copie.getNombreVoitures()).isEqualTo(original.getNombreVoitures());
        assertThat(copie.estAutomatique()).isEqualTo(original.estAutomatique());
    }

    @Test
    void nombreVoituresInferieurAUnRejete() {
        assertThatThrownBy(() -> new MetroTrain("M-ERR", 100, 80.0, 0, false))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("au moins 1 voiture");
        assertThatThrownBy(() -> new MetroTrain("M-ERR", 100, 80.0, -2, false))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void estAutomatiqueRefleteLeConstructeur() {
        MetroTrain sansMachiniste = new MetroTrain("M-201", 150, 75.0, 2, true);
        MetroTrain avecMachiniste = new MetroTrain("M-202", 150, 75.0, 2, false);

        assertThat(sansMachiniste.estAutomatique()).isTrue();
        assertThat(avecMachiniste.estAutomatique()).isFalse();
    }

    @Test
    void hebergeLesAttributsDuParent() {
        // Les champs communs sont initialisés par super(...) : la rame répond
        // aux getters de Vehicle sans que MetroTrain n'écrive un seul getter.
        MetroTrain rame = new MetroTrain("M-300", 180, 80.0, 3, false);

        assertThat(rame.getImmatriculation()).isEqualTo("M-300");
        assertThat(rame.getCapacitePassagers()).isEqualTo(180);
        assertThat(rame.getVitesseMaxKmh()).isEqualTo(80.0);
        assertThat(rame).isInstanceOf(Vehicle.class); // « est-un » Vehicle, officiellement
    }
}
