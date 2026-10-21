package fr.ebruno.dev.oocol.exceptions.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.within;

import org.junit.jupiter.api.Test;

/**
 * Tests de {@link FareCalculator} version W6 — le second registre unchecked :
 * le tarif négatif et la zone sans barème sont des BUGS de construction du
 * calculateur (FCE), pendant que l'argument {@code null} relève de
 * {@link StationNotFoundException} propagé par {@code requireValide}. Une
 * seule méthode, deux unchecked : le multi-catch de la scène 6 les borde.
 *
 * @author Emmanuel Bruno
 */
class FareCalculationExceptionTest {

    private static Zone zone1() {
        return new Zone(1, "Centre-ville");
    }

    private static Zone zone2() {
        return new Zone(2, "Périphérie");
    }

    private static Zone zone3() {
        return new Zone(3, "Grande couronne");
    }

    private static Station gare() {
        return Station.of("GAR", "Gare Centrale", zone1());
    }

    private static Station universite() {
        return Station.of("UNI", "Universite", zone2());
    }

    private static Station aeroport() {
        return Station.of("AER", "Aeroport", zone3());
    }

    /** Barème complet 1-2-3 : 2.00 / 3.50 / 4.50 €. */
    private static FareCalculator baremeComplet() {
        FareCalculator bareme = new FareCalculator();
        bareme.enregistrerTarif(1, 2.0);
        bareme.enregistrerTarif(2, 3.5);
        bareme.enregistrerTarif(3, 4.5);
        return bareme;
    }

    @Test
    void tarifNegatifEstUneIncoherenceDeBaremeDoncFCE() {
        FareCalculator bareme = new FareCalculator();

        // Pas une IAE : « -1.5 € » n'est pas un FORMAT de paramètre invalide,
        // c'est une incohérence LOGIQUE du domaine — la FCE le dit mieux.
        assertThatExceptionOfType(FareCalculationException.class)
                .isThrownBy(() -> bareme.enregistrerTarif(2, -1.5))
                .withMessageContaining("négatif");
    }

    @Test
    void numeroDeZoneInvalideResteUneIAE() {
        FareCalculator bareme = new FareCalculator();

        // Contraste interne à enregistrerTarif : numéro <= 0 = paramètre
        // aberrant (IAE) ; tarif < 0 = incohérence de domaine (FCE).
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> bareme.enregistrerTarif(0, 2.0));
    }

    @Test
    void trajetValideRetourneLePlusGrandTarifDesZonesTraversees() {
        // z1 → z2 : zones {1, 2} traversées, max(2.0, 3.5) = 3.5.
        assertThat(baremeComplet().calculerPrixEntre(gare(), universite()))
                .isCloseTo(3.5, within(0.0001));
    }

    @Test
    void trajetDansMemeZoneRetourneLeTarifDeLaZone() {
        assertThat(baremeComplet().calculerPrixEntre(gare(), gare()))
                .isCloseTo(2.0, within(0.0001));
    }

    @Test
    void zoneTraverseeSansTarifLeveFCEAvecLaZoneEnCause() {
        FareCalculator baremeIncomplete = new FareCalculator();
        baremeIncomplete.enregistrerTarif(1, 2.0);
        baremeIncomplete.enregistrerTarif(3, 4.5);
        // Zone 2 absente : le trajet z1 → z3 la traverse et bute dessus.

        assertThatExceptionOfType(FareCalculationException.class)
                .isThrownBy(() -> baremeIncomplete.calculerPrixEntre(gare(), aeroport()))
                .withMessageContaining("zone 2");
    }

    @Test
    void stationDepartNullePropageStationNotFound() {
        // requireValide fait le travail en amont du barème : la SNFE traverse
        // calculerPrixEntre SANS que la signature la mentionne — unchecked.
        assertThatExceptionOfType(StationNotFoundException.class)
                .isThrownBy(() -> baremeComplet().calculerPrixEntre(null, universite()));
    }

    @Test
    void stationArriveeNullePropageStationNotFound() {
        assertThatExceptionOfType(StationNotFoundException.class)
                .isThrownBy(() -> baremeComplet().calculerPrixEntre(gare(), null));
    }

    @Test
    void lesDeuxUncheckedSontSoeursSousRuntimeException() {
        Object[] couple = new Object[] {
                new FareCalculationException("x"),
                new StationNotFoundException("GAR"),
        };
        for (Object e : couple) {
            assertThat(e).isInstanceOf(RuntimeException.class);
            assertThat(e).isNotInstanceOf(NetworkException.class);
        }
        // Sœurs sous RuntimeException, jamais sous NetworkException : c'est ce
        // qui rend le multi-catch de la scène 6 légal SANS dériver quoi que ce soit.
    }

    @Test
    void connaitZoneRefleteLeBareme() {
        FareCalculator bareme = baremeComplet();

        assertThat(bareme.connaitZone(2)).isTrue();
        assertThat(bareme.connaitZone(4)).isFalse();
    }
}
