package fr.ebruno.dev.oocol.modern.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalDouble;

import org.junit.jupiter.api.Test;

/**
 * Les pipelines d'{@link AnalyseReseau}, testés un par un — puis jugés face à
 * leur miroir : {@link VersionLegacy} doit rendre <em>exactement</em> les mêmes
 * résultats, preuve que le passage aux Streams ne change pas le calcul, seule
 * la façon de le dire change. C'est cette égalité (derniers tests) qui fait du
 * diff entre les deux classes la leçon elle-même.
 *
 * @author Emmanuel Bruno
 */
class StreamsTest {

    private static final Zone Z1 = new Zone(1, "Centre");
    private static final Zone Z2 = new Zone(2, "Campus");

    private static final Station GAR = new Station("GAR", "Gare Centrale", Z1, true);
    private static final Station THE = new Station("THE", "Theatre", Z1, false);
    private static final Station UNI = new Station("UNI", "Universite", Z2, true);
    private static final Station HOP = new Station("HOP", "Hopital", Z2, true);

    private static final Ligne A = new Ligne("A", List.of(GAR, THE));
    private static final Ligne B = new Ligne("B", List.of(UNI, HOP));
    private static final List<Ligne> RESEAU = List.of(A, B);

    @Test
    void stationsAccessiblesFiltreLesArretsPmr() {
        assertThat(AnalyseReseau.stationsAccessibles(A)).containsExactly(GAR);
        assertThat(AnalyseReseau.stationsAccessibles(B)).containsExactly(UNI, HOP);
    }

    @Test
    void grouperParZoneAplatitPuisRegroupe() {
        Map<Zone, List<Station>> parZone = AnalyseReseau.grouperParZone(RESEAU);

        // flatMap : deux lignes → un flux d'arrêts ; groupingBy(Station::zone) : la carte.
        assertThat(parZone).containsOnlyKeys(Z1, Z2);
        assertThat(parZone.get(Z1)).containsExactly(GAR, THE);
        assertThat(parZone.get(Z2)).containsExactly(UNI, HOP);
    }

    @Test
    void prixMoyenFaitLaMoyenne() {
        List<Tarif> tarifs = List.of(
                new TarifUnite(Z1, 2.00),
                new TarifJour(List.of(Z1, Z2), 4.50),
                new TarifAbonnement("Alice", LocalDate.now(), LocalDate.now()));

        // (2.00 + 4.50 + 22.00 [abbonnement valorisé] ) / 3 = 9.50 — pile représentable en binaire.
        assertThat(AnalyseReseau.prixMoyen(tarifs)).isPresent().hasValue(9.50);
    }

    @Test
    void prixMoyenVideDonneOptionalDoubleVide() {
        assertThat(AnalyseReseau.prixMoyen(List.of())).isEqualTo(OptionalDouble.empty());
    }

    @Test
    void stationLaPlusProcheDeMinParDistanceDeCode() {
        // "GAX" est à distance 1 de "GAR", à 5 de "THE"… : min par |compareTo|.
        Optional<Station> proche = AnalyseReseau.stationLaPlusProcheDe("GAX", List.of(THE, UNI, GAR));
        assertThat(proche).contains(GAR);

        assertThat(AnalyseReseau.stationLaPlusProcheDe("GAR", List.of())).isEmpty();
    }

    @Test
    void mapEtToListTransformentLeFlux() {
        List<String> libelles = RESEAU.stream()
                .flatMap(l -> l.arrets().stream())
                .map(Station::affichage)
                .toList();
        assertThat(libelles).containsExactly(
                "GAR \u2014 Gare Centrale", "THE \u2014 Theatre",
                "UNI \u2014 Universite", "HOP \u2014 Hopital");
    }

    @Test
    void filterEtCountComptentSansIntermediaire() {
        long accessibles = RESEAU.stream()
                .flatMap(l -> l.arrets().stream())
                .filter(Station::accessible)
                .count();
        assertThat(accessibles).isEqualTo(3);
    }

    @Test
    void legacyEtModerneDonnentLesMemesStationsAccessibles() {
        for (Ligne ligne : RESEAU) {
            assertThat(VersionLegacy.stationsAccessibles(ligne))
                    .isEqualTo(AnalyseReseau.stationsAccessibles(ligne));
        }
    }

    @Test
    void legacyEtModerneDonnentLeMemeGroupage() {
        assertThat(VersionLegacy.grouperParZone(RESEAU))
                .isEqualTo(AnalyseReseau.grouperParZone(RESEAU));
    }

    @Test
    void legacyEtModerneDonnentLeMemePrixMoyenEtLeMemeResume() {
        List<Tarif> tarifs = List.of(new TarifUnite(Z1, 2.00), new TarifJour(List.of(Z1), 4.00));
        assertThat(VersionLegacy.prixMoyen(tarifs)).isEqualTo(AnalyseReseau.prixMoyen(tarifs));

        // Octet pour octet : le StringBuilder et le bloc de texte écrivent la même page.
        assertThat(VersionLegacy.resumerReseau(RESEAU))
                .isEqualTo(AnalyseReseau.resumerReseau(RESEAU));

        assertThat(VersionLegacy.trierParCode(List.of(HOP, GAR, UNI)))
                .isEqualTo(AnalyseReseau.trierParCode(List.of(HOP, GAR, UNI)));
    }
}
