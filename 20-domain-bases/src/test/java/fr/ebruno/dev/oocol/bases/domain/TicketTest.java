package fr.ebruno.dev.oocol.bases.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Locale;
import org.junit.jupiter.api.Test;

/**
 * Tests de {@link Ticket} — les fabriques statiques remplacent le
 * constructeur public : chaque test vérifie qu'une fabrique pose le bon type
 * et le bon prix, sans jamais écrire {@code new Ticket(...)} (impossible :
 * le constructeur est privé).
 *
 * @author Emmanuel Bruno
 */
class TicketTest {

    private static Zone zone1() {
        return new Zone(1, "Centre-ville");
    }

    @Test
    void fabriqueUnite() {
        Ticket ticket = Ticket.unite(zone1());

        assertThat(ticket.getType()).isEqualTo("Unit\u00E9"); // "Unité"
        assertThat(ticket.getPrix()).isEqualTo(1.80);
    }

    @Test
    void fabriqueJour() {
        Ticket ticket = Ticket.jour(zone1());

        assertThat(ticket.getType()).isEqualTo("Journ\u00E9e"); // "Journée"
        assertThat(ticket.getPrix()).isEqualTo(5.20);
    }

    @Test
    void fabriqueMois() {
        Ticket ticket = Ticket.mois(zone1());

        assertThat(ticket.getType()).isEqualTo("Abonnement mensuel");
        assertThat(ticket.getPrix()).isEqualTo(32.00);
    }

    @Test
    void fabriqueScolaire() {
        Ticket ticket = Ticket.scolaire(zone1());

        assertThat(ticket.getType()).isEqualTo("Scolaire");
        assertThat(ticket.getPrix()).isEqualTo(12.00);
    }

    @Test
    void tousLesPrixSontPositifs() {
        // Invariant métier : la grille tarifaire ne contient aucun prix négatif
        // ni nul — vérifié sur les QUATRE fabriques.
        for (Ticket ticket : new Ticket[] {
                Ticket.unite(zone1()), Ticket.jour(zone1()),
                Ticket.mois(zone1()), Ticket.scolaire(zone1()) }) {
            assertThat(ticket.getPrix()).isPositive();
        }
    }

    @Test
    void ticketEstImmuableAucunSetter() {
        // Réflexion : aucune méthode publique « set* » (type, prix, zone,
        // validité sont figés à l'émission par la fabrique).
        for (Method m : Ticket.class.getDeclaredMethods()) {
            boolean publique = Modifier.isPublic(m.getModifiers());
            assertThat(publique && m.getName().startsWith("set"))
                    .as("Ticket ne doit pas exposer de setter public, trouv\u00E9 : %s", m.getName())
                    .isFalse();
        }
    }

    @Test
    void constructeurEstPrive() {
        // Le constructeur existe (les fabriques l'utilisent) mais il est privé :
        // le monde extérieur ne peut pas inventer un type/prix hors grille.
        for (Constructor<?> c : Ticket.class.getDeclaredConstructors()) {
            assertThat(Modifier.isPrivate(c.getModifiers()))
                    .as("Le constructeur de Ticket doit rester priv\u00E9").isTrue();
        }
    }

    @Test
    void estValideVraiAEmission() {
        assertThat(Ticket.unite(zone1()).estValide()).isTrue();
    }

    @Test
    void toStringContientEuroEtZone() {
        Ticket ticket = Ticket.unite(zone1());

        // Le format dépend de la locale explicite (FRANCE) : on vérifie la
        // présence du symbole € et de la zone, pas la ponctuation décimale —
        // le point d'exclamation « virgule » exact relève du rendu local.
        assertThat(ticket.toString()).contains("\u20AC").contains("Zone 1");
        // Redondance pédagogique : avec Locale.FRANCE, 1.80 s'écrit bien « 1,80 ».
        String enFrancais = String.format(Locale.FRANCE, "%.2f", ticket.getPrix());
        assertThat(enFrancais).isEqualTo("1,80");
    }

    @Test
    void fabriquesZoneNulleRejetee() {
        assertThatThrownBy(() -> Ticket.jour(null))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
