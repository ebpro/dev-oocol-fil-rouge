package fr.ebruno.dev.oocol.serialisation.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.Field;
import java.time.LocalDate;

import org.junit.jupiter.api.Test;

/**
 * Le mot-clé {@code transient} — « ce champ ne voyage pas ». Un cache de
 * présentation se recalculera tout seul : l'écrire serait gaspiller des
 * octets et, pire, promener un état périmé à travers les versions du format.
 * Mais un abonnement qui ne voyage pas devient {@code null} — et le NPE
 * n'arrive que six mois plus tard. {@code transient} est une PROMESSE SUR LE
 * LECTEUR : ce qui est exclu doit être reconstitué, documenté, testé.
 *
 * @author Emmanuel Bruno
 */
class TransientTest {

    /** Fige un objet et le relit, dans la mémoire. */
    private static Object roundTrip(Object original) throws IOException, ClassNotFoundException {
        ByteArrayOutputStream tampon = new ByteArrayOutputStream();
        try (ObjectOutputStream sortie = new ObjectOutputStream(tampon)) {
            sortie.writeObject(original);
        }
        try (ObjectInputStream entree = new ObjectInputStream(new ByteArrayInputStream(tampon.toByteArray()))) {
            return entree.readObject();
        }
    }

    /** Le champ privé d'un objet, sans passer par son API — la preuve par réflexion. */
    private static Object champPrive(Object cible, String nom) throws Exception {
        Field champ = cible.getClass().getDeclaredField(nom);
        champ.setAccessible(true);
        return champ.get(cible);
    }

    @Test
    void champTransientNeVoyagePas() throws Exception {
        Station originale = new Station("GAR", "Gare Centrale", new Zone(1, "Centre-ville"));
        originale.getAffichage(); // le cache est chaud avant le voyage

        Station relu = (Station) roundTrip(originale);

        assertThat(champPrive(relu, "cacheAffichage")).isNull();
        // Le flux n'a PAS écrit null — il n'a rien écrit du tout : un champ
        // transient est ABSENT du binaire, la relecture laisse le défaut.
    }

    @Test
    void champsNonTransientSurvivent() throws Exception {
        Station originale = new Station("UNI", "Universite", new Zone(2, "Campus"));

        Station relu = (Station) roundTrip(originale);

        assertThat(relu.getCode()).isEqualTo("UNI");
        assertThat(relu.getNom()).isEqualTo("Universite");
        assertThat(relu.getZone().getNumero()).isEqualTo(2);
        assertThat(relu.getZone().getNom()).isEqualTo("Campus");
        // Seule la partie transient s'évapore ; le reste du graphe traverse intact.
    }

    @Test
    void getAffichageRecalculeApresRelecture() throws Exception {
        Station relu = (Station) roundTrip(new Station("HOP", "Hopital Sud", new Zone(3, "Sud")));

        String premier = relu.getAffichage();

        assertThat(premier).isNotNull();
        assertThat(premier).contains("HOP");
        assertThat(relu.getAffichage()).isSameAs(premier);
        // Paresse : le premier appel recalcule et met en cache, le second
        // renvoie la même instance — le transient n'était qu'un raccourci perdu.
    }

    @Test
    void abonnementTransientDevientNull() throws Exception {
        Passenger passager = new Passenger("Alice");
        passager.setAbonnement(new Subscription("Mensuel", LocalDate.of(2026, 11, 30)));

        Passenger relu = (Passenger) roundTrip(passager);

        assertThat(relu.getNom()).isEqualTo("Alice");
        assertThat(relu.getAbonnement()).isNull();
        // Piège n° 3 du module : le champ exclu devient null, sans bruit, sans
        // exception. Le NPE se déclarera à l'usage, plus tard, ailleurs.
    }
}
