package fr.ebruno.dev.oocol.modern.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

/**
 * Le record prouvé composant par composant : le constructeur canonique
 * valide, les accesseurs répondent, {@code equals}/{@code hashCode}/
 * {@code toString} sont générés par valeur, et la copie défensive de
 * {@link Ligne} rend l'immuabilité réelle et pas seulement décorative.
 *
 * <p>Chaque test est une promesse que le module 20 écrivait à la main —
 * ici, tenue par le compilateur. La « preuve d'immutabilité » est
 * structurelle : un record n'a pas de setter, il suffit de vérifier que
 * l'accesseur renvoie le composant fourni à la construction.
 *
 * @author Emmanuel Bruno
 */
class RecordTest {

    @Test
    void zoneRejetteNumeroInferieurAUn() {
        assertThatThrownBy(() -> new Zone(0, "Centre"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("numero >= 1");
        assertThatThrownBy(() -> new Zone(-3, "Banlieue"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void zoneRejetteNomVide() {
        assertThatThrownBy(() -> new Zone(1, "   "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("nom requis");
        assertThatThrownBy(() -> new Zone(1, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void stationRejetteCodeVideEtZoneNulle() {
        Zone zone = new Zone(1, "Centre");
        assertThatThrownBy(() -> new Station("", "Sans nom", zone, true))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("code requis");
        assertThatThrownBy(() -> new Station("X", "Nulle", null, true))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("zone requise");
    }

    @Test
    void egauxEtHachesParValeur() {
        Zone une = new Zone(1, "Centre");
        Zone autre = new Zone(1, "Centre");
        Zone differente = new Zone(2, "Campus");

        // Égalité structurelle : le module 20 passait 15 lignes à l'écrire.
        assertThat(une).isEqualTo(autre).isNotEqualTo(differente);
        assertThat(une).hasSameHashCodeAs(autre);
        // equals porte sur TOUS les composants (nuance vs le module 20 qui ne regardait que numero).
        assertThat(new Zone(1, "A")).isNotEqualTo(new Zone(1, "B"));
    }

    @Test
    void toStringGenereParLeCompilateur() {
        assertThat(new Zone(1, "Centre")).hasToString("Zone[numero=1, nom=Centre]");
    }

    @Test
    void accesseursSansSetterImmutables() {
        Zone zone = new Zone(3, "Est");
        // Pas de setter possible : un record n'expose que ses accesseurs en lecture.
        assertThat(zone.numero()).isEqualTo(3);
        assertThat(zone.nom()).isEqualTo("Est");
        assertThat(List.of(zone.getClass().getMethods()))
                .noneMatch(m -> m.getName().startsWith("set"));
    }

    @Test
    void trouverParCodeRenvoieOptionalPresentOuVide() {
        Zone zone = new Zone(1, "Centre");
        Station gare = new Station("GAR", "Gare Centrale", zone, true);
        Ligne ligne = new Ligne("A", List.of(gare));

        Optional<Station> trouvee = ligne.trouverParCode("GAR");
        assertThat(trouvee).isPresent().contains(gare);
        assertThat(ligne.trouverParCode("ZZZ")).isEmpty();
    }

    @Test
    void arretsDeLigneSontCopiesEtNonModifiables() {
        List<Station> modifiable = new ArrayList<>();
        Zone zone = new Zone(1, "Centre");
        modifiable.add(new Station("GAR", "Gare", zone, true));

        Ligne ligne = new Ligne("A", modifiable);

        // 1) L'appelant ne peut plus muter « par derrière » : la vue interne est figée.
        assertThatThrownBy(() -> ligne.arrets().add(new Station("THE", "Theatre", zone, false)))
                .isInstanceOf(UnsupportedOperationException.class);

        // 2) Modifier la liste d'origine ne touche la ligne : copie défensive, pas alias.
        modifiable.add(new Station("UNI", "Universite", zone, false));
        assertThat(ligne.arrets()).hasSize(1);
    }

    @Test
    void stationImplementeComparableViaRecord() {
        Zone zone = new Zone(1, "Centre");
        Station a = new Station("AAA", "A", zone, true);
        Station b = new Station("BBB", "B", zone, true);

        assertThat(a).isLessThan(b);
        assertThat(a.affichage()).isEqualTo("AAA \u2014 A");
    }
}
