package fr.ebruno.dev.oocol.nio.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Tests de l'écriture d'horaire — et surtout de son INVERSE de lecture :
 * ce qui est écrit doit être relu fidèle (UTF-8 pour de vrai), et le
 * temporaire du protocole atomique ne doit JAMAIS survivre au move.
 *
 * @author Emmanuel Bruno
 */
class ScheduleWriterTest {

    /** Un réseau minimal construit à la main, sans passer par les fichiers. */
    private static List<LineData> reseauDemo() {
        Zone z1 = new Zone(1, "Centre-ville");
        Station gar = new Station("GAR", "Gare Centrale", z1, true);
        Station uni = new Station("UNI", "Universit\u00E9", z1, true);
        List<Station> parcours = new ArrayList<>();
        parcours.add(gar);
        parcours.add(uni);
        List<LineData> lignes = new ArrayList<>();
        lignes.add(new LineData("Ligne A", parcours));
        return lignes;
    }

    @Test
    void ecritLesDepartsAttendus(@TempDir Path dossier) throws Exception {
        Path cible = dossier.resolve("horaire.txt");
        ScheduleWriter.ecrireHoraire(cible, reseauDemo(), 360, 15); // 06:00, pas de 15 min

        List<String> lignes = Files.readAllLines(cible, StandardCharsets.UTF_8);
        assertThat(lignes).hasSize(2); // entête + Ligne A
        assertThat(lignes.get(1))
                .startsWith("Ligne A \u2014 D\u00E9parts: 06:00, 06:15, 06:30")
                .endsWith("23:45"); // dernier \u00E0 \u2264 23:59 : 1425 = 23:45
    }

    @Test
    void ecritureAtomiqueNeLaisseAucunTemporaire(@TempDir Path dossier) throws Exception {
        Path cible = dossier.resolve("horaire.txt");
        ScheduleWriter.ecrireHoraire(cible, reseauDemo(), 420, 30);

        assertThat(Files.exists(cible)).as("la cible existe apr\u00E8s le move").isTrue();
        assertThat(Files.exists(cible.resolveSibling("horaire.txt.tmp")))
                .as("le .tmp a \u00E9t\u00E9 d\u00E9plac\u00E9, pas copi\u00E9").isFalse();
    }

    @Test
    void contenuReluFidelementEnUtf8(@TempDir Path dossier) throws Exception {
        Path cible = dossier.resolve("horaire.txt");
        ScheduleWriter.ecrireHoraire(cible, reseauDemo(), 480, 60);

        String contenu = Files.readString(cible, StandardCharsets.UTF_8);
        assertThat(contenu).contains("D\u00E9parts");     // les accents ont surv\u00E9cu \u00E0 l'\u00E9criture
        assertThat(contenu).doesNotContain("D\u00C3\u00A9parts"); // et pas en mode double-encod\u00E9
    }

    @Test
    void listeVideProduitQuandMemeUnFichierAvecEntete(@TempDir Path dossier) throws Exception {
        Path cible = dossier.resolve("vide.txt");
        ScheduleWriter.ecrireHoraire(cible, Collections.<LineData>emptyList(), 360, 15);

        // Un horaire « rien aujourd'hui » EST un horaire : le fichier existe.
        assertThat(Files.exists(cible)).isTrue();
        List<String> lignes = Files.readAllLines(cible, StandardCharsets.UTF_8);
        assertThat(lignes).hasSize(1);
        assertThat(lignes.get(0)).contains("Trans'U");
    }

    @Test
    void reEcritureRemplaceLancienEnUnGeste(@TempDir Path dossier) throws Exception {
        Path cible = dossier.resolve("horaire.txt");
        ScheduleWriter.ecrireHoraire(cible, reseauDemo(), 360, 15);
        ScheduleWriter.ecrireHoraire(cible, reseauDemo(), 480, 60); // 08:00, horaires 2

        String contenu = Files.readString(cible, StandardCharsets.UTF_8);
        assertThat(contenu).contains("08:00").doesNotContain("06:00");
    }

    @Test
    void formaterHeureAuxTroisBornes() {
        assertThat(ScheduleWriter.formaterHeure(0)).isEqualTo("00:00");
        assertThat(ScheduleWriter.formaterHeure(390)).isEqualTo("06:30");
        assertThat(ScheduleWriter.formaterHeure(1439)).isEqualTo("23:59");
    }

    @Test
    void formaterHeureHorsJourneeEstUnBugDAppelant() {
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> ScheduleWriter.formaterHeure(1440));
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> ScheduleWriter.formaterHeure(-1));
    }

    @Test
    void calculerDepartsSarreteAvantMinuit() {
        List<Integer> departs = ScheduleWriter.calculerDeparts(360, 15);
        assertThat(departs.get(0)).isEqualTo(360);
        assertThat(departs.get(departs.size() - 1)).isEqualTo(1425); // 23:45 ; 1440 franchirait minuit
        assertThat(departs).hasSize(72); // (1425 - 360) / 15 + 1
    }

    @Test
    void frequenceNulleEstRefuseeAvantEcriture(@TempDir Path dossier) {
        Path cible = dossier.resolve("jamais.txt");
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> ScheduleWriter.ecrireHoraire(cible, reseauDemo(), 360, 0));
        assertThat(Files.exists(cible)).isFalse();
    }

    @Test
    void ecrireDansUnDossierFantomeLeveNetworkException(@TempDir Path dossier) {
        Path cible = dossier.resolve("absent").resolve("horaire.txt");
        assertThatExceptionOfType(NetworkException.class)
                .isThrownBy(() -> ScheduleWriter.ecrireHoraire(cible, reseauDemo(), 360, 15))
                .havingCause() // l'IOException technique survit dans le « Caused by: »
                .isInstanceOf(IOException.class);
    }
}
