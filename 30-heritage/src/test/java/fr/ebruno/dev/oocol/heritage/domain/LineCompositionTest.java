package fr.ebruno.dev.oocol.heritage.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

/**
 * Tests de {@link Line} dans ce module — la composition et la délégation, avec
 * la preuve réflexive que la ligne n'hérite pas du véhicule : le LSP est
 * respecté par construction.
 *
 * @author Emmanuel Bruno
 */
class LineCompositionTest {

    private static Tramway tramDeTest() {
        return new Tramway("T-700", 210, 70.0, 33.0, 0.8);
    }

    @Test
    void annoncerServiceDelegueAuVehicule() {
        Tramway tram = tramDeTest();
        Line ligne = new Line("Ligne T1", tram);

        // La ligne ne fabrique rien : elle TRANSMET la demande au véhicule.
        // L'annonce de la ligne EST l'annonce du véhicule (délégation pure).
        assertThat(ligne.annoncerService()).isEqualTo(tram.annoncer());
        assertThat(ligne.annoncerService()).contains("Caténaire").contains("T-700");
    }

    @Test
    void setVehiculeAffecteRemplaceLeMaterial() {
        Line ligne = new Line("Ligne T1", tramDeTest());
        Bus bus = new Bus("B-700", 90, 50.0, true, 3);

        ligne.setVehiculeAffecte(bus); // substitution via le type large : aucun if à écrire dans Line

        assertThat(ligne.getVehiculeAffecte()).isSameAs(bus);
        assertThat(ligne.annoncerService()).contains("Hybride diesel-électrique");
    }

    @Test
    void lineNeHeritePasDeVehicle() {
        // PREUVE RÉFLEXIVE DU LSP : le parent direct de Line est Object —
        // pas Vehicle. Une Line n'EST PAS un Vehicle, elle EN A un.
        assertThat(Line.class.getSuperclass()).isEqualTo(Object.class);
        assertThat(Vehicle.class.isAssignableFrom(Line.class)).isFalse();
    }

    @Test
    void ligneSansVehiculeOuSansNomRejetee() {
        assertThatThrownBy(() -> new Line("Ligne T9", null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Line("  ", tramDeTest()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void toStringAfficheLigneEtVehicleAffecte() {
        Line ligne = new Line("Ligne T1", tramDeTest());

        assertThat(ligne.toString()).contains("Ligne T1").contains("T-700");
    }
}
