package fr.ebruno.dev.oocol.exceptions.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.within;

import org.junit.jupiter.api.Test;

/**
 * Tests de {@link Compte} — le checked PAR EXCELLENCE (solde insuffisant)
 * et ses voisins IAE (paramètres aberrants), plus le scénario complet de
 * récupération : rattraper → lire le manque → recharger → réessayer.
 *
 * @author Emmanuel Bruno
 */
class CompteTest {

    private static Compte borne(double solde) {
        return new Compte("U-42", solde);
    }

    @Test
    void debitSuffisantPasseEtSoldeDiminue() throws InsufficientFundsException {
        Compte borne = borne(5.0);

        // L'appelant de PRODUCTION doit écrire throws (ou un catch) pour
        // chaque debiter — même quand le succès est évident. C'est ça,
        // l'obligation checked : la gestion est dans la signature.
        boolean ok = borne.debiter(2.0);

        assertThat(ok).isTrue();
        assertThat(borne.getSolde()).isCloseTo(3.0, within(0.0001));
    }

    @Test
    void soldeInsuffisantLeveLeCheckedType() {
        Compte borne = borne(5.0);

        assertThatExceptionOfType(InsufficientFundsException.class)
                .isThrownBy(() -> borne.debiter(12.0));
    }

    @Test
    void lExceptionPorteSoldePrixEtManquant() {
        Compte borne = borne(5.0);

        assertThatExceptionOfType(InsufficientFundsException.class)
                .isThrownBy(() -> borne.debiter(12.0))
                .satisfies(e -> {
                    assertThat(e.getSolde()).isEqualTo(5.0);
                    assertThat(e.getPrix()).isEqualTo(12.0);
                    // Le plan B lit la donnée dans L'OBJET, pas dans le message :
                    assertThat(e.getManquant()).isCloseTo(7.0, within(0.0001));
                });
    }

    @Test
    void debitNegatifEstUnBugDoncUneIAE() {
        Compte borne = borne(5.0);

        // Contraste interne à la même méthode : montant < 0 = paramètre
        // aberrant (unchecked), solde court = situation d'affaire (checked).
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> borne.debiter(-1.0));
    }

    @Test
    void creditPositifAugmenteLeSolde() {
        Compte borne = borne(5.0);

        assertThatCode(() -> borne.crediter(7.5)).doesNotThrowAnyException();
        assertThat(borne.getSolde()).isCloseTo(12.5, within(0.0001));
    }

    @Test
    void creditNulOuNegatifEstUnBugDoncUneIAE() {
        Compte borne = borne(5.0);

        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> borne.crediter(0.0));
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> borne.crediter(-3.0));
    }

    @Test
    void scenarioRecuperationCompletRechargeEtReessaie() throws InsufficientFundsException {
        Compte borne = borne(5.0);
        double prixTitre = 12.0;

        try {
            borne.debiter(prixTitre);
        } catch (InsufficientFundsException e) {
            // Le « plan B » que le compilateur forçait à écrire : recharger
            // EXACTEMENT le manque lu dans l'exception…
            borne.crediter(e.getManquant());
        }
        // …puis réessayer, cette fois SANS catch : le throws est le contrat.
        borne.debiter(prixTitre);

        assertThat(borne.getSolde()).isCloseTo(0.0, within(0.0001));
    }

    @Test
    void constructeurRefuseTitulaireBlancEtSoldeNegatif() {
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> new Compte("  "));
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> new Compte("U-1", -0.5));
    }

    @Test
    void constructeurCourtOuvreASoldeZero() {
        Compte borne = new Compte("U-9");

        assertThat(borne.getSolde()).isZero();
        assertThat(borne.getTitulaire()).isEqualTo("U-9");
    }

    @Test
    void toStringContientTitulaireEtSolde() {
        assertThat(borne(3.25).toString())
                .contains("U-42")
                .contains("3.25");
    }
}
