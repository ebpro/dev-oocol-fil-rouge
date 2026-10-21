package fr.ebruno.dev.oocol.nio.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Tests du charset — la leçon que « le texte » n'existe pas sur disque :
 * il n'y a que des octets, et un encoding est le contrat qui décide lesquels
 * signifient quoi. Deux fichiers BYTE-différents peuvent relire le MÊME
 * texte (pourvu qu'on connaisse leur contrat) ; un même fichier peut relire
 * DEUX textes différents selon l'encoding qu'on lui impose.
 *
 * @author Emmanuel Bruno
 */
class CharsetDemoTest {

    /** Le mot qui splitte : « ô » coûte 2 octets en UTF-8, 1 seul en Latin-1. */
    private static final String MOT = "H\u00F4pital";

    @Test
    void utf8FaitLAllerRetour(@TempDir Path dossier) throws IOException {
        Path fichier = dossier.resolve("mot-utf8.txt");
        Files.write(fichier, MOT.getBytes(StandardCharsets.UTF_8));

        assertThat(Files.readString(fichier, StandardCharsets.UTF_8)).isEqualTo(MOT);
    }

    @Test
    void latin1FaitLAllerRetourDansSonPropreContrat(@TempDir Path dossier) throws IOException {
        Path fichier = dossier.resolve("mot-latin1.txt");
        Files.write(fichier, MOT.getBytes(StandardCharsets.ISO_8859_1));

        // Le même mot, relu dans SON encoding, est intact…
        assertThat(Files.readString(fichier, StandardCharsets.ISO_8859_1)).isEqualTo(MOT);
        // …mais l'octet, lui, n'est PAS le même : 8 en UTF-8, 7 en Latin-1.
        assertThat(Files.readAllBytes(fichier)).hasSize(7);
        assertThat(MOT.getBytes(StandardCharsets.UTF_8)).hasSize(8);
    }

    @Test
    void utf8MalLuDevientAutreChoseSansNecessairementCrier(@TempDir Path dossier) throws IOException {
        Path fichier = dossier.resolve("mot-utf8.txt");
        Files.write(fichier, MOT.getBytes(StandardCharsets.UTF_8));

        // Les 2 octets de « ô » (0xC3 0xB4) lus en Latin-1 donnent « Ã´ » :
        // la relecture RÉUSSIT et MENT — le pire mode de panne, silencieuse.
        String menteur = Files.readString(fichier, StandardCharsets.ISO_8859_1);
        assertThat(menteur).isNotEqualTo(MOT);
        assertThat(menteur).isEqualTo("H\u00C3\u00B4pital");
    }

    @Test
    void leChargeurAccepteLeCharsetDuContrat(@TempDir Path dossier) throws Exception {
        // Export vieux-jeu : zones.csv est en Latin-1, stations/lignes ASCII purs
        // (l'ASCII est valide dans TOUS les charsets — c'est son rôle de noyau).
        ecrireEn(dossier, "zones.csv", "1;Centre-ville\n2;P\u00E9riph\u00E9rie\n3;A\u00E9roport\n",
                StandardCharsets.ISO_8859_1);
        ecrireEn(dossier, "stations.csv", "AER;Aeroport;3;true\n", StandardCharsets.ISO_8859_1);
        ecrireEn(dossier, "lignes.csv", "Ligne A;AER\n", StandardCharsets.ISO_8859_1);

        NetworkLoadResult resau = NetworkLoader.chargerAvecEncodage(dossier, StandardCharsets.ISO_8859_1);
        assertThat(resau.getZones().get(2).getNom()).isEqualTo("A\u00E9roport");
    }

    @Test
    void leChargeurRefuseLesOctetsIndecodables(@TempDir Path dossier) throws Exception {
        // Même export Latin-1, mais relu avec le charset par défaut du chargeur
        // (UTF-8) : « éroport » commence par 0xE9, octet initiateur UTF-8 qui
        // attend deux octets de suite — le décodage est IMPOSSIBLE.
        ecrireEn(dossier, "zones.csv", "3;A\u00E9roport\n", StandardCharsets.ISO_8859_1);
        ecrireEn(dossier, "stations.csv", "GAR;Gare;3;true\n", StandardCharsets.ISO_8859_1);
        ecrireEn(dossier, "lignes.csv", "Ligne A;GAR\n", StandardCharsets.ISO_8859_1);

        assertThatExceptionOfType(NetworkException.class)
                .isThrownBy(() -> NetworkLoader.charger(dossier))
                .havingCause() // MalformedInputException : une IOException, enrobée — jamais perdue
                .isInstanceOf(IOException.class);
    }

    /** Écrit un contenu dans l'encoding imposé, dans le dossier donné. */
    private static void ecrireEn(Path dossier, String nom, String contenu,
            java.nio.charset.Charset charset) throws IOException {
        Files.write(dossier.resolve(nom), contenu.getBytes(charset));
    }
}
