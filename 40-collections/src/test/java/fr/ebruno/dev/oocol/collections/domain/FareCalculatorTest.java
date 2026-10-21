package fr.ebruno.dev.oocol.collections.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

/**
 * Tests de {@link FareCalculator} — les deux philosophies du tri
 * (ordre {@code Comparable} naturel vs {@code Comparator} délégué, Strategy
 * sans le nommer) et le tarif comme lookup {@code Map} par clé texte.
 *
 * @author Emmanuel Bruno
 */
class FareCalculatorTest {

    private static Zone zone1() {
        return new Zone(1, "Centre-ville");
    }

    private static Zone zone2() {
        return new Zone(2, "P\u00E9riph\u00E9rie");
    }

    private static Zone zone3() {
        return new Zone(3, "Grande couronne");
    }

    /** Barème : zone 1 → 2,00 € ; zone 2 → 3,50 €. La zone 3 n'a PAS de tarif. */
    private static Map<String, Double> baremeDeuxZones() {
        Map<String, Double> tarifs = new HashMap<String, Double>();
        tarifs.put("1", Double.valueOf(2.00));
        tarifs.put("2", Double.valueOf(3.50));
        return tarifs;
    }

    /** Calculateur standard : zones {2, 1} fournies EN VRAC, triées à la construction. */
    private static FareCalculator calculateurStandard() {
        List<Zone> vrac = new ArrayList<Zone>();
        vrac.add(zone2());
        vrac.add(zone1());
        return new FareCalculator(vrac, baremeDeuxZones());
    }

    private static List<Zone> trajete(Zone... zones) {
        List<Zone> trajet = new ArrayList<Zone>();
        for (Zone z : zones) {
            trajet.add(z);
        }
        return trajet;
    }

    @Test
    void leConstructeurTrieLesZonesParOrdreNaturel() {
        // Fournies en vrac [2, 1] — Collections.sort les range [1, 2] sans
        // qu'on fournisse le moindre comparateur : c'est Zone.compareTo qui parle.
        FareCalculator calculateur = calculateurStandard();

        assertThat(calculateur.getZones())
                .extracting("numero")
                .containsExactly(1, 2);
    }

    @Test
    void leGetZonesEstEnLectureSeule() {
        FareCalculator calculateur = calculateurStandard();

        assertThatThrownBy(() -> calculateur.getZones().add(zone3()))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void zoneUniqueDonneSonTarif() {
        FareCalculator calculateur = calculateurStandard();

        assertThat(calculateur.calculerPrix(trajete(zone1()))).isCloseTo(2.00, within(0.001));
    }

    @Test
    void trajetMultizonePaieLaPlusProfonde() {
        FareCalculator calculateur = calculateurStandard();

        // 1 → 2 : c'est la zone 2 qui décide (3,50 €), pas un cumul des deux.
        assertThat(calculateur.calculerPrix(trajete(zone1(), zone2()))).isCloseTo(3.50, within(0.001));

        // L'ordre de la liste n'y change rien : le calcul est un max, pas un dernier.
        assertThat(calculateur.calculerPrix(trajete(zone2(), zone1()))).isCloseTo(3.50, within(0.001));
    }

    @Test
    void zoneSansTarifRefuseLeCalcul() {
        FareCalculator calculateur = calculateurStandard();

        // La zone 3 est connue des zones du réseau… mais absente du barème :
        // le lookup Map rend null → échec bruyant, pas un prix de fantaisie.
        assertThatThrownBy(() -> calculateur.calculerPrix(trajete(zone3())))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Aucun tarif");
    }

    @Test
    void trajetVideOuNullEstRejete() {
        FareCalculator calculateur = calculateurStandard();

        assertThatThrownBy(() -> calculateur.calculerPrix(new ArrayList<Zone>()))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> calculateur.calculerPrix(null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void leConstructeurValideSesEntrees() {
        assertThatThrownBy(() -> new FareCalculator(null, baremeDeuxZones()))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new FareCalculator(new ArrayList<Zone>(), baremeDeuxZones()))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new FareCalculator(trajete(zone1()), null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void trierParNumeroUtiliseLOrdreNaturel() {
        List<Zone> vrac = trajete(zone3(), zone1(), zone2());

        FareCalculator.trierParNumero(vrac); // Collections.sort(liste), un seul argument

        assertThat(vrac).extracting("numero").containsExactly(1, 2, 3);
    }

    @Test
    void trierParNomUtiliseLeComparatorDelegue() {
        // Noms choisis pour que l'ordre alphabétique DIFFÈRE de l'ordre numérique :
        // « Centre-ville » < « Grande couronne » < « Périphérie » (C < G < P),
        // alors que les numéros sont 1 < 3 < 2.
        List<Zone> vrac = trajete(zone2(), zone3(), zone1());

        FareCalculator.trierParNom(vrac); // Collections.sort(liste, comparateur)

        assertThat(vrac).extracting("nom")
                .containsExactly("Centre-ville", "Grande couronne", "P\u00E9riph\u00E9rie");
    }

    @Test
    void lesDeuxTrisDonnentDesOrdresDifferents() {
        // La démonstration en un seul objet : MÊME liste, deux critères —
        // la stratégie de tri vient du DEHORS, la classe Zone reste muette.
        List<Zone> parNumero = trajete(zone3(), zone1(), zone2());
        List<Zone> parNom = trajete(zone3(), zone1(), zone2());

        Collections.sort(parNumero); // naturel : numéros
        Collections.sort(parNom, new Comparator<Zone>() { // délégué : noms
            @Override
            public int compare(Zone a, Zone b) {
                return a.getNom().compareToIgnoreCase(b.getNom());
            }
        });

        assertThat(parNumero).extracting("numero").containsExactly(1, 2, 3);
        assertThat(parNom).extracting("numero").containsExactly(1, 3, 2);
    }

    @Test
    void toStringAfficheLeBareme() {
        FareCalculator calculateur = calculateurStandard();

        assertThat(calculateur.toString())
                .startsWith("Bar\u00E8me 2 zones");
    }
}
