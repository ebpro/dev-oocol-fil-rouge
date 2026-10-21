package fr.ebruno.dev.oocol.transit.app;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import fr.ebruno.dev.oocol.transit.domain.Network;
import fr.ebruno.dev.oocol.transit.domain.NetworkBuilder;
import fr.ebruno.dev.oocol.transit.domain.Station;
import fr.ebruno.dev.oocol.transit.domain.Zone;

/**
 * La rétrospective… vérifiée par réflexion.
 *
 * <p>Les principes SOLID ne sont pas des slogans qu'on imprime : cette
 * classe APPORTE LA PREUVE que le code les respecte — constructeur privé,
 * frontière du domaine étanche à JDBC, signature du Builder, listes
 * réellement immuables. La phase 5 du main raconte ; ces tests certifient.
 */
@DisplayName("Rétrospective SOLID — les preuves, pas les promesses")
class SolidRetrospectiveTest {

    @Test
    @DisplayName("Le main imprime bien les cinq principes (SRP, OCP, LSP, ISP, DIP)")
    void mainPrinteLesCinqPrincipes() throws Exception {
        PrintStream original = System.out;
        ByteArrayOutputStream capture = new ByteArrayOutputStream();
        try {
            System.setOut(new PrintStream(capture, true, StandardCharsets.UTF_8));
            TransitApp.main(new String[]{});
        } finally {
            System.setOut(original);
        }
        String sortie = capture.toString(StandardCharsets.UTF_8);
        assertThat(sortie).contains("SRP", "OCP", "LSP", "ISP", "DIP");
        assertThat(sortie).contains("RÉTROSPECTIVE SOLID");
        assertThat(sortie).contains("FIN DU FIL ROUGE");
    }

    @Test
    @DisplayName("Le constructeur de Network est privé : le Builder est la seule entrée (OCP/SRP)")
    void constructeurDeNetworkEstPrive() {
        Constructor<?>[] constructeurs = Network.class.getDeclaredConstructors();
        assertThat(constructeurs).hasSize(1);
        assertThat(Modifier.isPrivate(constructeurs[0].getModifiers())).isTrue();
    }

    @Test
    @DisplayName("Le domaine ne connaît ni JDBC ni la persistance (DIP, vérifié par réflexion)")
    void leDomaineIgnoreJdbcEtLaPersistance() {
        // Champs de Network : uniquement des collections java.util — jamais
        // Connection, ResultSet ou Statement. Et jamais un type du paquet
        // persistence : la dépendance ne circule que dans l'autre sens.
        for (Field f : Network.class.getDeclaredFields()) {
            String type = f.getType().getName();
            assertThat(type).doesNotStartWith("java.sql");
            assertThat(type).doesNotStartWith("fr.ebruno.dev.oocol.transit.persistence");
            assertThat(type).doesNotStartWith("fr.ebruno.dev.oocol.transit.cli");
        }
        // Idem pour les signatures des méthodes (retours ET paramètres).
        for (Method m : Network.class.getDeclaredMethods()) {
            assertThat(m.getReturnType().getName()).doesNotStartWith("java.sql");
            for (Class<?> p : m.getParameterTypes()) {
                assertThat(p.getName()).doesNotStartWith("java.sql");
            }
        }
    }

    @Test
    @DisplayName("builder() est public-statique, construire() retourne Network : le contrat du motif")
    void contratDuBuilder() throws NoSuchMethodException {
        Method builder = Network.class.getDeclaredMethod("builder");
        assertThat(Modifier.isPublic(builder.getModifiers())).isTrue();
        assertThat(Modifier.isStatic(builder.getModifiers())).isTrue();
        assertThat(builder.getReturnType()).isEqualTo(NetworkBuilder.class);

        Method construire = NetworkBuilder.class.getDeclaredMethod("construire");
        assertThat(construire.getReturnType()).isEqualTo(Network.class);
    }

    @Test
    @DisplayName("Network est immuable : ses listes rejettent add() (List.copyOf, module 85)")
    void reseauxImmuables() throws Exception {
        Network n = Network.builder()
                .zone(1, "Centre-ville")
                .station("GAR", "Gare Centrale", 1, true)
                .construire();
        Station intrus = new Station("XXX", "Intrus", new Zone(1, "Centre-ville"), true);
        assertThatThrownBy(() -> n.stations().add(intrus))
                .isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> n.zones().add(new Zone(9, "Fantôme")))
                .isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> n.lignes().clear())
                .isInstanceOf(UnsupportedOperationException.class);
    }
}
