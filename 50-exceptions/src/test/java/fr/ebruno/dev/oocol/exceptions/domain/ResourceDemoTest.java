package fr.ebruno.dev.oocol.exceptions.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import org.junit.jupiter.api.Test;

/**
 * Tests du try-with-resources (scène 3 de la démo) sur des témoins
 * {@link AutoCloseable} maison, à compteur de fermetures : ce que l'on
 * PROUVE ici, c'est la GARANTIE structurelle — close() appelé exactement
 * une fois, en sortie normale COMME en sortie par exception — et le sort de
 * l'exception de fermeture quand le corps en lève déjà une : {@code suppressed}.
 *
 * @author Emmanuel Bruno
 */
class ResourceDemoTest {

    /** Ressource témoin : compte ses fermetures, sans jamais refuser de mourir. */
    private static final class RessourceTemoine implements AutoCloseable {

        private int fermetures;

        @Override
        public void close() {
            fermetures++;
        }

        int nombreDeFermetures() {
            return fermetures;
        }
    }

    /** Ressource réfractaire : son close() LÈVE — le cas qui révèle suppressed. */
    private static final class RessourceRefractaire implements AutoCloseable {

        @Override
        public void close() {
            throw new IllegalStateException("close refuse de mourir en paix");
        }
    }

    @Test
    void tryWithResourcesFermeEnSortieNormale() {
        RessourceTemoine r;
        try (RessourceTemoine ressource = new RessourceTemoine()) {
            r = ressource;
            assertThat(r.nombreDeFermetures()).isZero(); // encore ouverte DANS le bloc
        }
        // Sortie normale : close() a été appelé, une seule fois, par la structure.
        assertThat(r.nombreDeFermetures()).isEqualTo(1);
    }

    @Test
    void tryWithResourcesFermeMemeQuandLeCorpsLeve() {
        RessourceTemoine r = new RessourceTemoine();

        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> {
                    try (RessourceTemoine ressource = r) {
                        throw new IllegalArgumentException("echec dans le corps");
                    }
                });

        // C'EST LA LEÇON : l'ancien « finally { r.close(); } » s'oubliait ;
        // celui-ci ne s'oublie pas — même dans la tourmente, une fermeture.
        assertThat(r.nombreDeFermetures()).isEqualTo(1);
    }

    @Test
    void corpsSansErreurLExceptionDeCloseRemonteEnPrimaire() {
        // Corps tranquille : la levée de close() devient L'exception du bloc,
        // rien ne se perd — try-with-resources ne dévore jamais une fermeture.
        assertThatExceptionOfType(IllegalStateException.class)
                .isThrownBy(() -> {
                    try (RessourceRefractaire r = new RessourceRefractaire()) {
                        // rien : le close() au sortir du bloc fera des siennes
                    }
                })
                .withMessageContaining("close");
    }

    @Test
    void corpsEtCloseLeventLeCorpsRestePrimaireEtCloseEstSuppressed() {
        try (RessourceRefractaire r = new RessourceRefractaire()) {
            throw new IllegalArgumentException("la vraie panne est dans le corps");
            // close() LÈVE AUSSI en sortant : deux candidates, une priorité.
        } catch (IllegalArgumentException e) {
            // Règle Java : l'exception du CORPS est primaire ; celle de
            // close() est ENFILAIS dans getSuppressed(), jamais perdue.
            assertThat(e.getMessage()).contains("corps");
            assertThat(e.getSuppressed()).hasSize(1);
            assertThat(e.getSuppressed()[0])
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("close");
        }
    }
}
