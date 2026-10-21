package fr.ebruno.dev.oocol.exceptions.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import org.junit.jupiter.api.Test;

/**
 * Tests de {@link Line} version W6 — la migration « null → throws » jouée
 * méthode par méthode : lookup typé, doublons en IAE, cadence en checked,
 * ET le contrepoint qui GARDE son boolean ({@code retirerArret}) parce que
 * « rien à retirer » est une réponse métier, pas une anomalie.
 *
 * @author Emmanuel Bruno
 */
class LineExceptionsTest {

    private static Zone zone1() {
        return new Zone(1, "Centre-ville");
    }

    private static Station gare() {
        return Station.of("GAR", "Gare Centrale", zone1());
    }

    private static Station universite() {
        return Station.of("UNI", "Universite", zone1());
    }

    /** Ligne archétype : 3 arrêts, cadence par défaut (10 min). */
    private static Line ligneTroisArrets() {
        Line ligne = new Line("Ligne A");
        ligne.ajouterArret(gare());
        ligne.ajouterArret(universite());
        ligne.ajouterArret(Station.of("HOP", "Hopital", zone1()));
        return ligne;
    }

    @Test
    void trouverParCodePresentRetourneLaStation() {
        Line ligne = ligneTroisArrets();

        Station trouvee = ligne.trouverParCode("UNI");

        assertThat(trouvee.getCode()).isEqualTo("UNI");
    }

    @Test
    void trouverParCodeAbsentLeveStationNotFoundAvecLeCode() {
        Line ligne = ligneTroisArrets();

        // W5 : return null (piège différé). W6 : levée immédiate, typée,
        // ET le code cherché voyage dans l'objet, pas qu'en concaténation.
        assertThatExceptionOfType(StationNotFoundException.class)
                .isThrownBy(() -> ligne.trouverParCode("ZZZ"))
                .withMessageContaining("ZZZ")
                .satisfies(e -> assertThat(e.getCode()).isEqualTo("ZZZ"));
    }

    @Test
    void ajouterArretNullEstUnBugDoncUneIAE() {
        Line ligne = ligneTroisArrets();

        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> ligne.ajouterArret(null));
    }

    @Test
    void ajouterArretDoublonEstUnBugDoncUneIAE() {
        Line ligne = ligneTroisArrets();

        // W5 rendait false (silencieux). W6 lève : même code = même station
        // (le contrat equals W5 œuvre toujours dans contains()).
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> ligne.ajouterArret(Station.of("GAR", "Gare bis", zone1())))
                .withMessageContaining("GAR");
        assertThat(ligne.nombreArrets()).isEqualTo(3);
    }

    @Test
    void validerAccepteLaCadenceParDefaut() {
        Line ligne = ligneTroisArrets();

        assertThatCode(() -> ligne.valider()).doesNotThrowAnyException();
    }

    @Test
    void validerRefuseLaCadenceIrrealisteEnChecked() {
        Line ligneRapide = new Line("Ligne X", 2); // 2 min : possible mais irréaliste

        assertThatExceptionOfType(ScheduleConflictException.class)
                .isThrownBy(() -> ligneRapide.valider())
                .satisfies(e -> {
                    // Conflit STRUCTUREL : heureConflit = 0, documenté dans valider().
                    assertThat(e.getLigne()).isEqualTo("Ligne X");
                    assertThat(e.getHeureConflit()).isZero();
                    assertThat(e).isInstanceOf(NetworkException.class);
                });
    }

    @Test
    void setFrequenceMinImpossibleEstUnBugDoncUneIAE() {
        Line ligne = ligneTroisArrets();

        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> ligne.setFrequenceMin(0));
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> ligne.setFrequenceMin(-5));
    }

    @Test
    void requireValideRetourneLaStationPresente() {
        Station gare = gare();

        assertThat(Station.requireValide(gare, "test unitaire")).isSameAs(gare);
    }

    @Test
    void requireValideLeveSurNullAvecLeContexte() {
        // Le contexte d'appel joue le rôle de diagnostic (la station absente
        // n'a pas de code à afficher) — documenté dans Station#requireValide.
        assertThatExceptionOfType(StationNotFoundException.class)
                .isThrownBy(() -> Station.requireValide(null, "recherche depuis le réseau"))
                .withMessageContaining("absente")
                .withMessageContaining("recherche depuis le réseau");
    }

    @Test
    void retirerArretAbsentRetourneFauxSANSException() {
        Line ligne = ligneTroisArrets();
        Station stade = Station.of("STP", "Stade", zone1());

        // LE contrepoint de la séance : une réponse métier normale garde son
        // boolean. En faire une exception = exceptions pour le contrôle de
        // flux, l'anti-pattern combattu.
        assertThatCode(() -> ligne.retirerArret(stade)).doesNotThrowAnyException();
        assertThat(ligne.retirerArret(stade)).isFalse();
    }

    @Test
    void retirerArretPresentRetourneVrai() {
        Line ligne = ligneTroisArrets();

        boolean retiree = ligne.retirerArret(Station.of("UNI", "Universite (re-creee)", zone1()));

        assertThat(retiree).isTrue();
        assertThat(ligne.nombreArrets()).isEqualTo(2);
    }

    @Test
    void premierArretSurLigneVideLeveIllegalState() {
        // Ligne sans arrêt : l'état demandé est incohérent — bug de l'appelant,
        // unchecked par nature (promis et joué depuis le module 40).
        Line ligneVide = new Line("Ligne fantôme");

        assertThatExceptionOfType(IllegalStateException.class)
                .isThrownBy(() -> ligneVide.premierArret());
    }
}
