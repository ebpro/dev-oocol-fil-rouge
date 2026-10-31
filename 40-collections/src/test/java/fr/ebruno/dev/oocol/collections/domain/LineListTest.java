package fr.ebruno.dev.oocol.collections.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * Tests de {@link Line} — la paire de champs est devenue une
 * {@code List<Station>} : ordre préservé, refus des doublons par
 * {@code equals}, retrait par autre exemplaire, lookup O(n) documenté.
 *
 * @author Emmanuel Bruno
 */
class LineListTest {

    private static Zone zone1() {
        return new Zone(1, "Centre-ville");
    }

    private static Station gare() {
        return Station.of("GAR", "Gare Centrale", zone1());
    }

    private static Station universite() {
        return Station.of("UNI", "Universite", zone1());
    }

    private static Station hopital() {
        return Station.of("HOP", "Hopital", zone1());
    }

    /** Ligne à trois arrêts, le cas de test archétypal. */
    private static Line ligneTroisArrets() {
        Line ligne = new Line("Ligne A");
        ligne.ajouterArret(gare());
        ligne.ajouterArret(universite());
        ligne.ajouterArret(hopital());
        return ligne;
    }

    @Test
    void ordreDesArretsEstPreserve() {
        // C'est POUR ça qu'on a choisi une List : le parcours a un sens.
        Line ligne = ligneTroisArrets();

        assertThat(ligne.getArrets())
                .extracting("code")
                .containsExactly("GAR", "UNI", "HOP");
    }

    @Test
    void ajoutDeMemeCodeEstRejete() {
        Line ligne = ligneTroisArrets();

        // Autre instance, même code « GAR » : contains() la reconnaît (equals),
        // l'ajout est donc refusé — sans le contrat, le doublon passerait.
        boolean ajoute = ligne.ajouterArret(Station.of("GAR", "Gare bis", zone1()));

        assertThat(ajoute).isFalse();
        assertThat(ligne.nombreArrets()).isEqualTo(3);
    }

    @Test
    void retraitFonctionneParAutreExemplaireDeMemeCode() {
        Line ligne = ligneTroisArrets();

        boolean retiree = ligne.retirerArret(Station.of("UNI", "Universite (re-creee)", zone1()));

        assertThat(retiree).isTrue();
        assertThat(ligne.nombreArrets()).isEqualTo(2);
        assertThat(ligne.getArrets()).doesNotContain(universite());
    }

    @Test
    void retirerUnArretAbsentRenvoieFaux() {
        Line ligne = ligneTroisArrets();
        Station stade = Station.of("STP", "Stade", zone1());

        // Sémantique List.remove(Object) : false sur absence, pas d'exception.
        assertThat(ligne.retirerArret(stade)).isFalse();
        assertThat(ligne.retirerArret(null)).isFalse();
        assertThat(ligne.nombreArrets()).isEqualTo(3);
    }

    @Test
    void trouverParCodeRetourneLaStationPresente() {
        Line ligne = ligneTroisArrets();

        Station trouvee = ligne.trouverParCode("UNI");

        assertThat(trouvee).isNotNull();
        assertThat(trouvee.getNom()).isEqualTo("Universite");
    }

    @Test
    void trouverParCodeAbsentRetourneNull() {
        Line ligne = ligneTroisArrets();

        // Absence = null, contrat documenté de la méthode.
        assertThat(ligne.trouverParCode("ZZZ")).isNull();
    }

    @Test
    void premierEtDernierArretsSontLesBoutsDeLaListe() {
        Line ligne = ligneTroisArrets();

        assertThat(ligne.premierArret()).isEqualTo(gare()); // par equals, pas par référence
        assertThat(ligne.dernierArret()).isEqualTo(hopital());
    }

    @Test
    void estTerminusReconnaitLesDeuxExtremites() {
        Line ligne = ligneTroisArrets();

        assertThat(ligne.estTerminus(Station.of("GAR", "Gare (re-creee)", zone1()))).isTrue();
        assertThat(ligne.estTerminus(hopital())).isTrue();
        assertThat(ligne.estTerminus(universite())).isFalse(); // le milieu n'est jamais terminus
        assertThat(ligne.estTerminus(null)).isFalse();
    }

    @Test
    void toStringSuitLeFormatDeLaSeance() {
        Line ligne = ligneTroisArrets();

        assertThat(ligne.toString())
                .isEqualTo("Ligne A [GAR \u2192 UNI \u2192 HOP] (3 arr\u00EAts)");
    }

    @Test
    void ligneVideRefusePremierEtDernierArret() {
        Line ligne = new Line("Ligne Z");

        // Échec bruyant : pas de null qui se propage.
        assertThatThrownBy(ligne::premierArret)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("aucun arrêt");
        assertThatThrownBy(ligne::dernierArret)
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void ajoutDUnArretNullEstRejete() {
        Line ligne = new Line("Ligne Z");

        assertThatThrownBy(() -> ligne.ajouterArret(null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void laListeRetourneeEstEnLectureSeule() {
        Line ligne = ligneTroisArrets();

        // Collections.unmodifiableList : la vue refuse l'écriture — l'appelant
        // ne peut pas court-circuiter ajouterArret/retirerArret.
        List<Station> arrets = ligne.getArrets();
        assertThatThrownBy(() -> arrets.add(hopital()))
                .isInstanceOf(UnsupportedOperationException.class);
        assertThat(ligne.nombreArrets()).isEqualTo(3);
    }
}
