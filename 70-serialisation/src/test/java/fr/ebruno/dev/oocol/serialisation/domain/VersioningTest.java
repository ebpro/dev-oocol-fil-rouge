package fr.ebruno.dev.oocol.serialisation.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InvalidClassException;
import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * La version du format — le {@code serialVersionUID} comme empreinte de
 * compatibilité, et la ligne de crête entre faire ÉVOLUER un format (le
 * champ de plus, UID stable, valeur par défaut à la relecture) et le ROMPRE
 * (UID changé : tous les fichiers écrits avant sont décrétés périmés, avec
 * un message qui cite les deux numéros comme un verdict).
 *
 * <p><strong>Comment simuler « le fichier d'avant » dans un seul JVM.</strong>
 * Impossible d'avoir deux versions d'une même classe sous le même nom. La
 * solution du cours est chirurgicale : écrire le binaire de la version
 * {@code InstantV1}, puis repeindre son nom de classe en {@code InstantV2}
 * — MÊME longueur, donc aucun décalage — et relire. C'est exactement le
 * fichier que la V1 écrivait six mois plus tôt, et c'est la V2 d'aujourd'hui
 * qui le lit. Le UID, lui, est resté 1 : le contrat tient.
 *
 * @author Emmanuel Bruno
 */
class VersioningTest {

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

    /** Les octets d'un objet écrit. */
    private static byte[] versOctets(Object original) throws IOException {
        ByteArrayOutputStream tampon = new ByteArrayOutputStream();
        try (ObjectOutputStream sortie = new ObjectOutputStream(tampon)) {
            sortie.writeObject(original);
        }
        return tampon.toByteArray();
    }

    /**
     * Repeint {@code ancien} par {@code nouveau} dans le binaire — MÊME
     * longueur exigée : on ne décale rien, on change juste le nom que le
     * descripteur porte.
     */
    private static byte[] renommer(byte[] octets, String ancien, String nouveau) {
        byte[] cible = ancien.getBytes(StandardCharsets.UTF_8);
        byte[] remplacement = nouveau.getBytes(StandardCharsets.UTF_8);
        assertThat(remplacement.length).as("noms de longueurs différentes : décalage interdit").isEqualTo(cible.length);
        ByteArrayOutputStream sortie = new ByteArrayOutputStream();
        int i = 0;
        while (i < octets.length) {
            if (i + cible.length <= octets.length) {
                boolean correspond = true;
                for (int j = 0; j < cible.length; j++) {
                    if (octets[i + j] != cible[j]) {
                        correspond = false;
                        break;
                    }
                }
                if (correspond) {
                    sortie.writeBytes(remplacement);
                    i += cible.length;
                    continue;
                }
            }
            sortie.write(octets[i]);
            i++;
        }
        return sortie.toByteArray();
    }

    /** Relit depuis un binaire déjà en main. */
    private static Object depuisOctets(byte[] octets) throws IOException, ClassNotFoundException {
        try (ObjectInputStream entree = new ObjectInputStream(new ByteArrayInputStream(octets))) {
            return entree.readObject();
        }
    }

    @Test
    void uidDifferentsRompentLeFormat(@TempDir Path dossier) throws Exception {
        List<Line> lignes = new ArrayList<>();
        List<Zone> zones = new ArrayList<>();
        zones.add(new Zone(1, "Centre-ville"));
        NetworkSnapshot v1 = new NetworkSnapshot(lignes, zones, Instant.parse("2026-10-20T08:30:00Z"), "Avant");
        Path fichier = dossier.resolve("reseau-v1.snapshot");
        v1.sauvegarder(fichier); // le fichier « d'il y a six mois »

        try {
            NetworkSnapshotV2.migrerDepuisV1(fichier); // la V2, UID 2, tente de le lire
            throw new AssertionError("attendu : InvalidClassException");
        } catch (InvalidClassException attendu) {
            assertThat(attendu.getMessage()).contains("serialVersionUID = 1").contains("serialVersionUID = 2");
        }
        // La remappe de nom par resolveClass ne sauve rien : le UID se vérifie
        // AVANT le nom. Changer le UID, c'est trahir tous les fichiers d'avant.
    }

    @Test
    void evolutionCompatibleNouveauChampPrendSaValeurParDefaut() throws Exception {
        byte[] octetsV1 = versOctets(new InstantV1(3, "Rentrée"));

        // Repeinture : le binaire porte désormais le nom de la classe d'après,
        // le UID (1L) est resté le même, et le champ supplement n'existait pas.
        InstantV2 relu = (InstantV2) depuisOctets(renommer(octetsV1, "InstantV1", "InstantV2"));

        assertThat(relu.getNumero()).isEqualTo(3);
        assertThat(relu.getNom()).isEqualTo("Rentrée");
        assertThat(relu.getSupplement()).isNull(); // absent du flux : défaut, pas exception
        // C'est ÇA, l'évolution compatible : UID stable + champ de plus. Les
        // anciens fichiers se lisent ; le nouveau champ prend son défaut.
    }

    @Test
    void champsStatiquesNeVoyagentJamais() throws Exception {
        Compteur.compteur = 42; // l'état « au moment de l'écriture »
        byte[] octets = versOctets(new Compteur(7));

        Compteur.compteur = 99; // le présent de la JVM lectrice

        Compteur relu = (Compteur) depuisOctets(octets);

        assertThat(relu.valeur).isEqualTo(7); // l'instance voyage
        assertThat(Compteur.compteur).isEqualTo(99); // le static reste dans la JVM
        relu.valeur = 7; // (les assertions portent sur le revenant et sur la classe)
        // Un static appartient à la classe, pas à l'objet : le flux ne l'emporte
        // jamais, et la relecture ne l'écrase pas. INSTANCE du singleton : même loi.
    }

    @Test
    void readObjectRefuseUnFluxCorrompu() throws Exception {
        Station originale = new Station("GAR", "Gare Centrale", new Zone(1, "Centre-ville"));
        Field nom = Station.class.getDeclaredField("nom");
        nom.setAccessible(true);
        nom.set(originale, null); // la corruption : un fichier édité à la main, ou pire

        byte[] octets = versOctets(originale);

        try {
            depuisOctets(octets);
            throw new AssertionError("attendu : InvalidObjectException");
        } catch (InvalidObjectException attendu) {
            assertThat(attendu.getMessage()).contains("Station");
        }
        // Sans le hook readObject, la JVM aurait aligné un objet sans nom en
        // mémoire — le constructeur n'est jamais passé par là. La revalidation
        // des invariants à la lecture est la digue contre un flux non fiable.
    }

    /** La V1 du binaire répété : deux champs, UID 1. */
    static final class InstantV1 implements Serializable {

        /** Version du format. */
        private static final long serialVersionUID = 1L;

        /** Numéro de la version enregistrée. */
        private final int numero;

        /** Libellé humain. */
        private final String nom;

        InstantV1(int numero, String nom) {
            this.numero = numero;
            this.nom = nom;
        }
    }

    /** La V2 : un champ de plus, le MÊME UID — l'évolution propre. */
    static final class InstantV2 implements Serializable {

        /** Version du format — restée à 1 : c'est le cœur de la leçon. */
        private static final long serialVersionUID = 1L;

        /** Numéro de la version enregistrée (non final : la JVM le pose par réflexion). */
        private int numero;

        /** Libellé humain. */
        private String nom;

        /** Le champ ajouté six mois plus tard — absent des anciens flux. */
        private String supplement;

        int getNumero() {
            return numero;
        }

        String getNom() {
            return nom;
        }

        String getSupplement() {
            return supplement;
        }
    }

    /** Le porteur de static — pour prouver qu'un static ne voyage jamais. */
    static final class Compteur implements Serializable {

        /** Version du format. */
        private static final long serialVersionUID = 1L;

        /** Le static : propriété de la classe, jamais du flux. */
        static int compteur = 0;

        /** Le champ d'instance, lui, voyage. */
        int valeur;

        Compteur(int valeur) {
            this.valeur = valeur;
        }
    }
}
