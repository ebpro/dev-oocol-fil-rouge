package fr.ebruno.dev.oocol.exceptions.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import java.lang.reflect.Field;

import org.junit.jupiter.api.Test;

/**
 * Tests de la HIERARCHIE elle-même (ELO 07, scène fondatrice) : qui hérite
 * de qui, checked vs unchecked à la lecture de l'arbre, cause préservée
 * derrière le chaînage, données utiles portées par les exceptions, et
 * serialVersionUID présent sur toute la famille.
 *
 * @author Emmanuel Bruno
 */
class ExceptionHierarchyTest {

    private static InsufficientFundsException soldeInsuffisant() {
        return new InsufficientFundsException(5.0, 12.0);
    }

    private static ScheduleConflictException conflitCadence() {
        return new ScheduleConflictException("cadence trop courte", "Ligne B", 0);
    }

    @Test
    void networkExceptionEstCheckedCarNeDerivePasDeRuntime() {
        NetworkException ne = new NetworkException("racine");

        // Le test VERITABLE du « checked » : une Exception qui n'est PAS un
        // RuntimeException. C'est ce qui force throws/catch à la compilation.
        assertThat(ne).isInstanceOf(Exception.class);
        assertThat(ne).isNotInstanceOf(RuntimeException.class);
    }

    @Test
    void scheduleConflictHeriteDeNetworkException() {
        assertThat(conflitCadence())
                .isInstanceOf(NetworkException.class)
                .isInstanceOf(Exception.class)
                .isNotInstanceOf(RuntimeException.class);
    }

    @Test
    void insufficientFundsHeriteDeNetworkException() {
        assertThat(soldeInsuffisant())
                .isInstanceOf(NetworkException.class)
                .isInstanceOf(Exception.class)
                .isNotInstanceOf(RuntimeException.class);
    }

    @Test
    void stationNotFoundEstUncheckedEtPasDeLaFamilleMetier() {
        StationNotFoundException snfe = new StationNotFoundException("ZZZ");

        // Le message clé de la séance : SNFE est une SŒUR des unchecked,
        // PAS une fille de NetworkException — un rattrapage typé NetworkException ne
        // l'attrape JAMAIS, volontairement.
        assertThat(snfe).isInstanceOf(RuntimeException.class);
        assertThat(snfe).isNotInstanceOf(NetworkException.class);
    }

    @Test
    void fareCalculationEstUncheckedEtPasDeLaFamilleMetier() {
        FareCalculationException fce = new FareCalculationException("bareme incomplet");

        assertThat(fce).isInstanceOf(RuntimeException.class);
        assertThat(fce).isNotInstanceOf(NetworkException.class);
    }

    @Test
    void laCauseSurvitAuChainage() {
        IllegalArgumentException basNiveau = new IllegalArgumentException("champ illisible");
        ScheduleConflictException enrobee = new ScheduleConflictException(
                "regle de cadence invalide", "A", 8, basNiveau);

        // L'enrobage ne JETTE PAS la trace technique, il la BRANCHE :
        // getCause() = l'originale, même instance.
        assertThat(enrobee.getCause()).isSameAs(basNiveau);
    }

    @Test
    void lesGettersPortentLaDonneeUtileSansParserLeMessage() {
        StationNotFoundException snfe = new StationNotFoundException("GAR");
        ScheduleConflictException sc = conflitCadence();
        InsufficientFundsException ife = soldeInsuffisant();

        // Une exception typée est un CANAL DE DONNEES : l'appelant lit les
        // champs, il ne découpe pas getMessage() comme un string de log.
        assertThat(snfe.getCode()).isEqualTo("GAR");
        assertThat(sc.getLigne()).isEqualTo("Ligne B");
        assertThat(sc.getHeureConflit()).isZero();
        assertThat(ife.getSolde()).isEqualTo(5.0);
        assertThat(ife.getPrix()).isEqualTo(12.0);
    }

    @Test
    void manquantCalculeCorrespondAuRechargementNecessaire() {
        InsufficientFundsException ife = soldeInsuffisant();

        // 12 demandé, 5 dispo : le plan B (Compte.crediter) recharge EXACTEMENT ça.
        assertThat(ife.getManquant()).isCloseTo(7.0, within(0.0001));
    }

    @Test
    void messagesPortentLeContexteSansLeDeviner() {
        assertThat(new StationNotFoundException("ZZZ").getMessage())
                .contains("ZZZ");
        assertThat(soldeInsuffisant().getMessage())
                .contains("Solde insuffisant")
                .contains("12.0")
                .contains("5.0");
    }

    @Test
    void touteLaHierarchiePorteUnSerialVersionUid() throws Exception {
        Class<?>[] famille = new Class<?>[] {
                NetworkException.class,
                ScheduleConflictException.class,
                InsufficientFundsException.class,
                StationNotFoundException.class,
                FareCalculationException.class,
        };
        for (Class<?> cible : famille) {
            Field champ = cible.getDeclaredField("serialVersionUID");
            champ.setAccessible(true);
            // Requis par Serializable (dont Throwable hérite) ; le module
            // 70-serialisation dira POURQUOI ce constant n'est pas du decor.
            assertThat(champ.getLong(null))
                    .as("serialVersionUID de %s", cible.getSimpleName())
                    .isEqualTo(1L);
        }
    }
}
