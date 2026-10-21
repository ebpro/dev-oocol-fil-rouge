package fr.ebruno.dev.oocol.heritage.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

import org.junit.jupiter.api.Test;

/**
 * Tests des interfaces {@link Capteur} et {@link Rechargeable} sur
 * {@link Tramway} — les capacités choisies (ISP) : un enfant implémente,
 * un autre ne doit PAS être obligé de le faire.
 *
 * <p>Forme traditionnelle de l'{@code instanceof} + cast explicite : le
 * pattern matching ({@code if (v instanceof Capteur c)}) est réservé au module 80.
 *
 * @author Emmanuel Bruno
 */
class InterfacesTest {

    private static Tramway tramCharge(double niveau) {
        return new Tramway("T-500", 210, 70.0, 33.0, niveau);
    }

    @Test
    void tramwayEstUnCapteurEtLitSaVitesse() {
        Tramway tram = tramCharge(0.5);

        assertThat(tram instanceof Capteur).isTrue();
        Capteur capteur = (Capteur) tram;
        // Version simplifiée assumée par Tramway#lireVitesse : le capteur lit la vitesse max.
        assertThat(capteur.lireVitesse()).isEqualTo(70.0);
    }

    @Test
    void methodeParDefautDeSurvitesseFonctionneSansReecriture() {
        Tramway tram = tramCharge(0.5);
        Capteur capteur = (Capteur) tram;

        // estEnSurvitesse n'est écrite AUCUNE PART dans Tramway : comportement
        // hérité de l'interface (default method). 70 > 60 : survitesse ; 70 > 80 : non.
        assertThat(capteur.estEnSurvitesse(60.0)).isTrue();
        assertThat(capteur.estEnSurvitesse(80.0)).isFalse();
    }

    @Test
    void rechargerAugmenteLeNiveauActuel() {
        Tramway tram = tramCharge(0.5);
        Rechargeable stockage = (Rechargeable) tram;

        stockage.recharger(0.3);

        assertThat(tram instanceof Rechargeable).isTrue();
        assertThat(stockage.niveauActuel()).isCloseTo(0.8, within(0.0001));
    }

    @Test
    void rechargerPlafonneALaCapacite() {
        Tramway tram = tramCharge(0.9);
        Rechargeable stockage = (Rechargeable) tram;

        stockage.recharger(0.5); // 0.9 + 0.5 = 1.4 → plafonné à 1.0

        assertThat(stockage.niveauActuel()).isEqualTo(1.0);
        // Et la description de propulsion suit l'état : le polymorphisme lit le vivant.
        assertThat(tram.decrirePropulsion()).contains("(100%)");
    }

    @Test
    void busNaPasDeCapteur() {
        Bus bus = new Bus("B-500", 90, 50.0, false, 2);

        // ISP : « avoir un capteur » n'est pas une obligation de l'espèce.
        // Le bus est un Vehicle parfaitement valide SANS implémenter Capteur.
        assertThat(bus instanceof Capteur).isFalse();
        assertThat(bus instanceof Rechargeable).isFalse();
    }

    @Test
    void niveauDeBatterieHorsIntervalleRejete() {
        assertThatThrownBy(() -> new Tramway("T-ERR", 200, 70.0, 33.0, 1.5))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Tramway("T-ERR", 200, 70.0, 33.0, -0.1))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
