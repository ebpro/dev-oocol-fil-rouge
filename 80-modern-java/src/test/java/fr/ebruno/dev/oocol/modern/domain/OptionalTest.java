package fr.ebruno.dev.oocol.modern.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

/**
 * {@code Optional} enfin utilisé comme il doit l'être : pas un gadget autour
 * d'un {@code isPresent()} qui rejoue le {@code != null}, mais une chaîne
 * {@code map}/{@code flatMap}/{@code orElseThrow} où l'absence se propage
 * typée et le traitement ne s'écrit qu'une fois.
 *
 * <p>Le module 50 condamnait le {@code null} sans avoir le type pour le dire ;
 * ici la signature {@code Optional<Station>} de
 * {@link Ligne#trouverParCode(String)} est le contrat — l'appelant ne peut
 * pas « oublier » le cas vide, le compilateur le lui rappelle.
 *
 * @author Emmanuel Bruno
 */
class OptionalTest {

    private static final Zone Z1 = new Zone(1, "Centre");

    private static final Station GAR = new Station("GAR", "Gare Centrale", Z1, true);

    private static final Ligne A = new Ligne("A", List.of(GAR,
            new Station("UNI", "Universite", new Zone(2, "Campus"), false)));

    @Test
    void ofNullableTransformeLeNullEnVide() {
        String rien = null;
        assertThat(Optional.ofNullable(rien)).isEmpty();
        assertThat(Optional.ofNullable("GAR")).contains("GAR");
    }

    @Test
    void mapTransformeLaValeurSansDerangerLeVide() {
        assertThat(A.trouverParCode("GAR").map(Station::affichage))
                .contains("GAR \u2014 Gare Centrale");

        // Le map sur un vide ne s'exécute jamais : pas de NPE, pas de if.
        assertThat(A.trouverParCode("ZZZ").map(Station::affichage)).isEmpty();
    }

    @Test
    void orElseThrowLanceLexceptionFournie() {
        assertThat(A.trouverParCode("GAR")
                .orElseThrow(() -> new IllegalStateException("arret absent")))
                .isEqualTo(GAR);

        assertThatThrownBy(() -> A.trouverParCode("ZZZ")
                .orElseThrow(() -> new IllegalStateException("arret absent")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("arret absent");
    }

    @Test
    void ifPresentDelegueAuConsumer() {
        StringBuilder journal = new StringBuilder();
        A.trouverParCode("GAR").ifPresent(s -> journal.append("vue:").append(s.code()));
        A.trouverParCode("ZZZ").ifPresent(s -> journal.append("jamais"));
        assertThat(journal).hasToString("vue:GAR");
    }

    @Test
    void flatMapChaineDeuxOptionals() {
        // Optional<Ligne> -> Optional<Station> : map aurait donné Optional<Optional<Station>>.
        Optional<Ligne> ligneConnue = Optional.of(A);
        assertThat(ligneConnue.flatMap(l -> l.trouverParCode("GAR"))).contains(GAR);

        Optional<Station> introuvable = Optional.<Ligne>empty().flatMap(l -> l.trouverParCode("GAR"));
        assertThat(introuvable).isEmpty();
    }

    @Test
    void orElseDonneLeDefautQuandCEstVide() {
        Station secours = new Station("XXX", "Secours", Z1, false);
        assertThat(A.trouverParCode("ZZZ").orElse(secours)).isSameAs(secours);
        // (orElseGet attend un Supplier — évaluation paresseuse du défaut.)
    }

    @Test
    void optionalSurvitAuxCalculsDeDate() {
        // Simple clin d'œil à java.time (TarifAbonnement) : Optional et LocalDate
        // sont les deux « contrôles d'absence » du module — l'un sur la valeur,
        // l'autre sur le temps.
        TarifAbonnement abonnement = new TarifAbonnement(
                "Alice", LocalDate.of(2026, 10, 1), LocalDate.of(2027, 9, 30));
        Optional<TarifAbonnement> optionnel = Optional.of(abonnement);
        assertThat(optionnel.map(TarifAbonnement::titulaire)).contains("Alice");
        assertThat(optionnel.filter(t -> t.fin().isAfter(LocalDate.of(2027, 1, 1)))).isPresent();
    }
}
