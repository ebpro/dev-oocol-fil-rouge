package fr.ebruno.dev.oocol.scaffold;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Tests du module d'amorçage.
 *
 * <p>Deux objectifs pédagogiques en plus de la vérification :
 * <ol>
 *   <li>montrer le triptyque <em>Arrange / Act / Assert</em> avec AssertJ
 *       ({@code assertThat(...).isEqualTo(...)} se lit comme une phrase) ;</li>
 *   <li>montrer qu'on teste une <strong>fonction pure</strong> ({@code greeting})
 *       beaucoup plus facilement qu'un affichage — capter {@code System.out}
 *       (test {@link #mainPrintsWelcomeOnStdout()}) est une technique de dernier
 *       recours, présentée ici pour démythifier la capture de sortie.</li>
 * </ol>
 */
class TransitHelloTest {

    private final ByteArrayOutputStream capturedStdout = new ByteArrayOutputStream();
    private PrintStream originalStdout;

    /**
     * Exécuté avant chaque test : redirige System.out vers un tampon mémoire.
     */
    @BeforeEach
    void redirectStdout() {
        originalStdout = System.out;
        System.setOut(new PrintStream(capturedStdout, true, StandardCharsets.UTF_8));
    }

    /**
     * Exécuté après chaque test : RESTAURE System.out.
     * Un test qui ne restaure pas l'état global casse les tests suivants
     * (et la sortie Maven) : c'est la première leçon d'hygiène de test.
     */
    @AfterEach
    void restoreStdout() {
        System.setOut(originalStdout);
    }

    @Test
    @DisplayName("greeting() mentionne la ligne demandée et le réseau Trans'U")
    void greetingContainsLineAndNetwork() {
        String message = TransitHello.greeting("B");

        assertThat(message)
                .contains("Bienvenue")
                .contains("ligne B")
                .contains(TransitHello.NETWORK_NAME);
    }

    @Test
    @DisplayName("greeting() est une fonction pure : même entrée, même sortie")
    void greetingIsPureFunction() {
        // Deux appels, même argument, doivent produire exactement le même résultat.
        assertThat(TransitHello.greeting("A")).isEqualTo(TransitHello.greeting("A"));
    }

    @Test
    @DisplayName("main() affiche l'accueil et le parcours de la ligne A")
    void mainPrintsWelcomeOnStdout() {
        TransitHello.main(new String[] {});

        String output = capturedStdout.toString(StandardCharsets.UTF_8);
        assertThat(output)
                .contains("Bienvenue à bord de la ligne A")
                .contains("Arrêt 1/4")
                .contains("Gare centrale");
    }

    @Test
    @DisplayName("la propriété système java.version est accessible et non vide")
    void javaVersionPropertyIsAccessible() {
        // Le module 00 sert aussi à vérifier l'environnement : cette propriété
        // doit être fournie par toute JVM, en local comme en CI.
        assertThat(System.getProperty("java.version"))
                .isNotBlank();
    }
}
