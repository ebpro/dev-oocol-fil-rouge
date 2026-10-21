package fr.ebruno.dev.oocol.serialisation.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Externalizable;
import java.io.IOException;
import java.io.InvalidClassException;
import java.io.InvalidObjectException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

/**
 * {@code Externalizable} en mode manuel — le contrat que {@link Ticket}
 * signe à la main, et les trois clauses que personne n'oublie sans brûler :
 * le constructeur public sans argument, la revalidation des invariants à la
 * lecture, et le {@code serialVersionUID} — oui, même ici.
 *
 * <p><strong>Le test qui tue l'illusion.</strong> « {@code Externalizable}
 * n'a pas de UID utile » : faux. Les jumeaux {@link TicketU1} /
 * {@link TicketU2} portent le même nom, le même format manuel, deux UID
 * différents — et la JVM refuse. {@code Externalizable} change QUI écrit les
 * champs, pas la signature du format.
 *
 * <p><strong>La corruption comme méthode.</strong> Deux tests prennent le
 * binaire légitime d'un {@link Ticket} et en retournent des octets — le
 * double du prix, l'entier du numéro de zone — pour prouver que
 * {@code readExternal} est un portier, pas une éponge.
 *
 * @author Emmanuel Bruno
 */
class ExternalizableTest {

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

    /** Les octets d'un objet écrit, seuls matériaux des tests de corruption. */
    private static byte[] versOctets(Object original) throws IOException {
        ByteArrayOutputStream tampon = new ByteArrayOutputStream();
        try (ObjectOutputStream sortie = new ObjectOutputStream(tampon)) {
            sortie.writeObject(original);
        }
        return tampon.toByteArray();
    }

    /** Index de la première occurrence de {@code motif} dans {@code octets}, ou -1. */
    private static int indexOf(byte[] octets, byte[] motif) {
        for (int i = 0; i + motif.length <= octets.length; i++) {
            boolean ok = true;
            for (int j = 0; j < motif.length; j++) {
                if (octets[i + j] != motif[j]) {
                    ok = false;
                    break;
                }
            }
            if (ok) {
                return i;
            }
        }
        return -1;
    }

    /** Les {@code n} octets de poids fort en premier d'une valeur longue (big-endian). */
    private static byte[] bigEndian(long valeur, int n) {
        byte[] octets = new byte[n];
        for (int i = 0; i < n; i++) {
            octets[n - 1 - i] = (byte) (valeur >>> (8 * i));
        }
        return octets;
    }

    /**
     * Remplace toute occurrence de {@code ancien} par {@code nouveau} dans le
     * binaire — MÊME longueur exigée : on ne décale rien, on repeint. C'est
     * ainsi que le cours simule « le fichier a été écrit par la version
     * précédente de cette classe » sans quitter le JVM.
     */
    private static byte[] renommer(byte[] octets, String ancien, String nouveau) {
        byte[] cible = ancien.getBytes(StandardCharsets.UTF_8);
        byte[] remplacement = nouveau.getBytes(StandardCharsets.UTF_8);
        assertThat(remplacement.length).as("noms de longueurs différentes : décalage interdit").isEqualTo(cible.length);
        ByteArrayOutputStream sortie = new ByteArrayOutputStream();
        int i = 0;
        while (i < octets.length) {
            if (i + cible.length <= octets.length && indexOf(java.util.Arrays.copyOfRange(octets, i, i + cible.length), cible) == 0) {
                sortie.writeBytes(remplacement);
                i += cible.length;
            } else {
                sortie.write(octets[i]);
                i++;
            }
        }
        return sortie.toByteArray();
    }

    @Test
    void ticketVoyageEtSeReconstruit() throws Exception {
        Ticket original = Ticket.unite(new Zone(1, "Centre-ville"));

        Ticket relu = (Ticket) roundTrip(original);

        assertThat(relu.getType()).isEqualTo("Unité");
        assertThat(relu.getPrix()).isEqualTo(1.80);
        assertThat(relu.getZone().getNumero()).isEqualTo(1);
        // L'aplatissement assumé : seul le NUMÉRO de la zone voyage ; le nom
        // est FABRIQUÉ par readExternal — Externalizable n'a pas de graphe gratuit.
        assertThat(relu.getZone().getNom()).isEqualTo("reconstruite");
    }

    @Test
    void fabriqueJourneeFidele() throws Exception {
        Ticket original = Ticket.jour(new Zone(2, "Périphérie"));

        Ticket relu = (Ticket) roundTrip(original);

        assertThat(relu.getType()).isEqualTo("Journée");
        assertThat(relu.getPrix()).isEqualTo(5.20);
        assertThat(relu.getZone().getNumero()).isEqualTo(2);
    }

    @Test
    void binairePlusPetitQuAvecSerializable() throws Exception {
        Zone zone = new Zone(1, "Centre-ville");

        byte[] octetsEx = versOctets(Ticket.unite(zone));
        byte[] octetsSer = versOctets(new TicketSerializable(zone));

        assertThat(octetsEx.length).isLessThan(octetsSer.length);
        // Le même contenu : 91 octets écrits à la main (trois valeurs aplaties)
        // contre ~300 octets automatiques (deux descripteurs de classe + graphe).
    }

    @Test
    void changerLeUidRompLeFormatMemeAvecExternalizable() throws Exception {
        TicketU1 avant = new TicketU1(7);
        byte[] octets = versOctets(avant);

        // Rejoue l'histoire de NetworkSnapshotV2 en miniature : le MÊME format
        // manuel, écrit sous le UID 1, relu par la classe devenue UID 2 — en
        // repeignant le nom du descripteur (même longueur, pas de décalage).
        byte[] repeint = renommer(octets, "TicketU1", "TicketU2");

        try {
            roundTripDepuisOctets(repeint);
            throw new AssertionError("attendu : InvalidClassException");
        } catch (InvalidClassException attendu) {
            assertThat(attendu.getMessage()).contains("serialVersionUID = 1").contains("serialVersionUID = 2");
        }
        // Externalizable ne désarme PAS la vérification du UID : le format est
        // un contrat, qu'on l'écrive à la main ou non.
    }

    @Test
    void constructeurSansArgObligatoire() throws Exception {
        SansCtorVide original = new SansCtorVide("clé");

        byte[] octets = versOctets(original); // l'écriture passe sans souci

        try {
            roundTripDepuisOctets(octets);
            throw new AssertionError("attendu : InvalidClassException");
        } catch (InvalidClassException attendu) {
            assertThat(attendu.getMessage()).contains("no valid constructor");
        }
        // readExternal remplit une instance VIDE : sans constructeur public sans
        // argument, la JVM ne sait pas la fabriquer. Le format écrit est mort.
    }

    @Test
    void readExternalRefuseUnPrixNegatif() throws Exception {
        byte[] octets = versOctets(Ticket.unite(new Zone(1, "Centre-ville")));

        // Corruption ciblée : le double 1.80 s'est écrit en big-endian dans le
        // flux — on le repeint en -1.80, MÊME taille d'octets, pas de décalage.
        int index = indexOf(octets, bigEndian(Double.doubleToRawLongBits(1.80), 8));
        assertThat(index).as("les octets du prix doivent se trouver dans le binaire").isGreaterThanOrEqualTo(0);
        byte[] corrompus = bigEndian(Double.doubleToRawLongBits(-1.80), 8);
        for (int j = 0; j < 8; j++) {
            octets[index + j] = corrompus[j];
        }

        try {
            roundTripDepuisOctets(octets);
            throw new AssertionError("attendu : InvalidObjectException");
        } catch (InvalidObjectException attendu) {
            assertThat(attendu.getMessage()).contains("prix négatif");
        }
        // readExternal est un portier : le flux n'est pas une source de confiance.
    }

    @Test
    void readExternalRefuseUnNumeroDeZoneInvalide() throws Exception {
        byte[] octets = versOctets(Ticket.unite(new Zone(1, "Centre-ville")));

        // Même principe : l'entier du numéro de zone suit le double du prix
        // dans l'ordre d'écriture (writeUTF, writeDouble, writeInt). On le
        // repeint en 0 — zone inexistante, invariant violé.
        int indexPrix = indexOf(octets, bigEndian(Double.doubleToRawLongBits(1.80), 8));
        assertThat(indexPrix).as("les octets du prix doivent se trouver dans le binaire").isGreaterThanOrEqualTo(0);
        int indexNumero = indexPrix + 8;
        byte[] zero = bigEndian(0L, 4);
        for (int j = 0; j < 4; j++) {
            octets[indexNumero + j] = zero[j];
        }

        try {
            roundTripDepuisOctets(octets);
            throw new AssertionError("attendu : InvalidObjectException");
        } catch (InvalidObjectException attendu) {
            assertThat(attendu.getMessage()).contains("sans zone valide");
        }
    }

    /** Relit depuis un binaire déjà en main — la porte des tests de corruption. */
    private static Object roundTripDepuisOctets(byte[] octets) throws IOException, ClassNotFoundException {
        try (ObjectInputStream entree = new ObjectInputStream(new ByteArrayInputStream(octets))) {
            return entree.readObject();
        }
    }

    /** Jumeau UID 1 du format manuel — écriture de la pièce à conviction. */
    static final class TicketU1 implements Externalizable {

        /** Version du format : 1. */
        private static final long serialVersionUID = 1L;

        /** Valeur unique du format manuel. */
        private int valeur;

        TicketU1() {
        }

        TicketU1(int valeur) {
            this.valeur = valeur;
        }

        @Override
        public void writeExternal(ObjectOutput out) throws IOException {
            out.writeInt(valeur);
        }

        @Override
        public void readExternal(ObjectInput in) throws IOException {
            valeur = in.readInt();
        }
    }

    /** Jumeau UID 2 — même nom (à un caractère près), même format, UID différent. */
    static final class TicketU2 implements Externalizable {

        /** Version du format : 2 — la rupture. */
        private static final long serialVersionUID = 2L;

        /** Valeur unique du format manuel. */
        private int valeur;

        TicketU2() {
        }

        @Override
        public void writeExternal(ObjectOutput out) throws IOException {
            out.writeInt(valeur);
        }

        @Override
        public void readExternal(ObjectInput in) throws IOException {
            valeur = in.readInt();
        }
    }

    /** L'oubli classique : Externalizable SANS constructeur public sans argument. */
    static final class SansCtorVide implements Externalizable {

        /** Version du format. */
        private static final long serialVersionUID = 1L;

        /** Une valeur, écrite à la main. */
        private String cle;

        /** Le seul constructeur — avec argument : la JVM restera bloquée à la lecture. */
        SansCtorVide(String cle) {
            this.cle = cle;
        }

        @Override
        public void writeExternal(ObjectOutput out) throws IOException {
            out.writeUTF(cle);
        }

        @Override
        public void readExternal(ObjectInput in) throws IOException {
            cle = in.readUTF();
        }
    }

    /** L'équivalent {@code Serializable} du ticket — la pièce de comparaison de taille. */
    static final class TicketSerializable implements Serializable {

        /** Version du format. */
        private static final long serialVersionUID = 1L;

        /** Type du ticket. */
        private final String type;

        /** Prix en euros. */
        private final double prix;

        /** La zone complète — le graphe, pas un numéro aplati. */
        private final Zone zone;

        TicketSerializable(Zone zone) {
            this.type = "Unité";
            this.prix = 1.80;
            this.zone = zone;
        }
    }
}
