package fr.ebruno.dev.oocol.serialisation.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

import org.junit.jupiter.api.Test;

/**
 * Les points de vigilance canoniques de la sérialisation, joués jusqu'à la
 * preuve. Le singleton qui éclate en copie (puis se répare par {@code readResolve}), la
 * super-classe non sérialisable dont le constructeur se rejoue à chaque
 * lecture, les hooks {@code writeObject} / {@code readObject} que seul le
 * mécanisme appelle — et l'invariant qu'ils gardent.
 *
 * <p><strong>Point de vigilance — la sécurité.</strong> Il ne se « teste » pas ici :
 * il se CONSTATE. {@code readObject} reconstruit n'importe quelle classe
 * citée dans le flux, avec n'importe quel état. Un fichier sérialisé n'est
 * pas une donnée, c'est un programme d'assemblage. La seule défense est
 * avant la lecture (ne jamais désérialiser une source non fiable) — d'où le
 * silence du fichier de tests, qui est déjà une démonstration.
 *
 * @author Emmanuel Bruno
 */
class PitfallsTest {

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

    @Test
    void singletonEclateSansReadResolve() throws Exception {
        Passenger original = Passenger.getInstance();

        Passenger relu = (Passenger) roundTrip(original);

        assertThat(relu).isNotSameAs(original);
        assertThat(relu.getNom()).isEqualTo(original.getNom());
        // Deux instances, mêmes champs : la promesse « une seule instance » est
        // morte — la JVM alloue sans passer par le constructeur privé.
    }

    @Test
    void readResolvePreserveLeSingleton() throws Exception {
        PassengerCorrige original = PassengerCorrige.getInstance();

        PassengerCorrige relu = (PassengerCorrige) roundTrip(original);

        assertThat(relu).isSameAs(original);
        // readResolve : le jumeau est bien alloué, puis jeté — le hook renvoie
        // l'instance partagée, et le flux ne voit jamais le doublon.
    }

    @Test
    void superClasseNonSerialisableRejoueSonConstructeur() throws Exception {
        Fille original = new Fille(11871);
        original.etat = "modifie"; // l'héritage du Socle, volontairement mué

        Fille relu = (Fille) roundTrip(original);

        assertThat(relu.id).isEqualTo(11871); // la partie sérialisable traverse
        assertThat(relu.etat).isEqualTo("construit"); // la partie Socle se REJOUUE
        // Le constructeur de la super-classe non sérialisable est rappelé à la
        // lecture : l'état d'héritage revient à ses valeurs de création, pas à
        // celles figées à l'écriture. Un état à préserver doit être sérialisable.
    }

    @Test
    void hooksWriteObjectReadObjectSontAppeles() throws Exception {
        Station originale = new Station("GAR", "Gare Centrale", new Zone(1, "Centre-ville"));
        int ecrituresAvant = Station.compteurEcritures;
        int lecturesAvant = Station.compteurLectures;

        Station relu = (Station) roundTrip(originale);

        assertThat(Station.compteurEcritures).isEqualTo(ecrituresAvant + 1);
        assertThat(Station.compteurLectures).isEqualTo(lecturesAvant + 1);
        // Personne n'a appelé writeObject / readObject à la main : c'est le
        // mécanisme, par réflexion, sur la signature imposée. La preuve par
        // compteur — et le compteur lui-même, static, ne voyage pas.
        assertThat(relu.getCode()).isEqualTo("GAR");
    }

    /** Le socle oublié de {@code Serializable} — ses champs ne voyagent pas. */
    static class Socle {

        /** État d'héritage, posée par le constructeur — donc rejouée à la lecture. */
        String etat = "construit";
    }

    /** La fille sérialisable, héritière d'un socle non sérialisable. */
    static final class Fille extends Socle implements Serializable {

        /** Version du format. */
        private static final long serialVersionUID = 1L;

        /** Identité sérialisée, elle. */
        int id;

        Fille(int id) {
            this.id = id;
        }
    }
}
