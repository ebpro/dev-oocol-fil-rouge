package fr.ebruno.dev.oocol.bases.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

/**
 * Tests de {@link Line} — valeurs par défaut, validation de la fréquence,
 * et surtout comparaison par <em>code</em> (et non par référence) dans
 * {@link Line#estDirecte(Station)}.
 *
 * @author Emmanuel Bruno
 */
class LineTest {

    private static Zone zone1() {
        return new Zone(1, "Centre-ville");
    }

    private static Station gare() {
        return new Station("Gare Centrale", "GAR", zone1());
    }

    private static Station universite() {
        return new Station("Universite", "UNI", zone1());
    }

    @Test
    void frequenceParDefautEst15Minutes() {
        // Constructeur court (3 arguments) : la valeur par défaut s'applique.
        Line ligneA = new Line("Ligne A", gare(), universite());

        assertThat(ligneA.getFrequenceMin()).isEqualTo(15);
    }

    @Test
    void frequencePersonnaliseeAcceptee() {
        Line ligneB = new Line("Ligne B", gare(), universite(), 8);

        assertThat(ligneB.getFrequenceMin()).isEqualTo(8);
    }

    @Test
    void frequenceNulleOuNegativeRejetee() {
        assertThatThrownBy(() -> new Line("Ligne X", gare(), universite(), 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("strictement positive");
        assertThatThrownBy(() -> new Line("Ligne Y", gare(), universite(), -5))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void estDirecteCompareParCodePasParReference() {
        Line ligneA = new Line("Ligne A", gare(), universite());

        // AUTRE instance, NOM différent, MAIS même code « GAR » :
        // estDirecte doit répondre true — ce qui compte est l'identité métier
        // (le code d'exploitation), pas l'identité mémoire (==).
        Station gareAutreInstance = new Station("Gare (copie)", "GAR", zone1());

        assertThat(ligneA.estDirecte(gareAutreInstance)).isTrue();
    }

    @Test
    void estDirecteFauxPourStationEtrangere() {
        Line ligneA = new Line("Ligne A", gare(), universite());
        Station hopital = new Station("Hopital", "HOP", zone1());

        assertThat(ligneA.estDirecte(hopital)).isFalse();
        // Conventions de robustesse : null n'est jamais une extrémité.
        assertThat(ligneA.estDirecte(null)).isFalse();
    }

    @Test
    void toStringAfficheExtremitesEtFrequence() {
        Line ligneA = new Line("Ligne A", gare(), universite());

        assertThat(ligneA.toString())
                .contains("Ligne A")
                .contains("Gare Centrale")
                .contains("Universite")
                .contains("15 min");
    }
}
