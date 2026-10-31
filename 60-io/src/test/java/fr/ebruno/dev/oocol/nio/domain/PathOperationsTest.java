package fr.ebruno.dev.oocol.nio.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Tests de l'algèbre des chemins — le point où l'on continue de
 * raisonner en chaînes de caractères. Chaque assertion est une propriété que
 * {@code String} ne peut pas tenir : connaître le séparateur du système,
 * composer sans concaténer, normaliser les remontées, relativiser.
 *
 * @author Emmanuel Bruno
 */
class PathOperationsTest {

    @Test
    void pathOfComposeLesSegmentsSansConnaitreLeSeparateur() {
        Path chemin = Path.of("/tmp", "oocol", "zones.csv");
        assertThat(chemin.getFileName().toString()).isEqualTo("zones.csv");
        assertThat(chemin.getNameCount()).isEqualTo(3);
        assertThat(chemin.getName(0).toString()).isEqualTo("tmp");
        // toString est une VUE pour l'humain, pas un matériau de composition.
        assertThat(chemin.toString()).isEqualTo(Path.of("/tmp").resolve("oocol").resolve("zones.csv").toString());
    }

    @Test
    void normalizeSupprimeLesRemonteesQuandCestPossible() {
        Path brut = Path.of("/srv/app").resolve("../data/zones.csv");
        assertThat(brut.toString()).contains(".."); // avant : la remontée est là
        Path net = brut.normalize();
        assertThat(net).isEqualTo(Path.of("/srv/data/zones.csv"));
    }

    @Test
    void relativizeReplongeUnCheminSousUnAutre() {
        Path racine = Path.of("/reseau/export");
        Path cible = Path.of("/reseau/export/2026/semaine7/zones.csv");
        assertThat(racine.relativize(cible))
                .isEqualTo(Path.of("2026").resolve("semaine7").resolve("zones.csv"));
    }

    @Test
    void resoudreChemineEquautAPathOfEnPlusProgressif() {
        Path attendu = Path.of("/var", "teaching", "horaire.txt");
        assertThat(FileSystemWalker.resoudreChemine("/var", "teaching", "horaire.txt"))
                .isEqualTo(attendu);
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> FileSystemWalker.resoudreChemine(new String[0]));
    }

    @Test
    void trouverFichiersFiltreParExtensionEtTri(@TempDir Path dossier) throws Exception {
        toucher(dossier, "b.csv");
        toucher(dossier, "a.csv");
        toucher(dossier, "c.txt");
        // Un SOUS-dossier nommé *.csv ne doit pas être compté : isRegularFile filtre.
        Files.createDirectories(dossier.resolve("dossier-leurre.csv"));

        List<Path> trouves = FileSystemWalker.trouverFichiers(dossier, ".csv");
        assertThat(trouves).hasSize(2);
        assertThat(trouves.get(0).getFileName().toString()).isEqualTo("a.csv"); // trié
        assertThat(trouves.get(1).getFileName().toString()).isEqualTo("b.csv");
    }

    @Test
    void trouverFichiersSurVideRetourneListeVide(@TempDir Path dossier) throws NetworkException {
        assertThat(FileSystemWalker.trouverFichiers(dossier, ".csv")).isEmpty();
    }

    @Test
    void trouverFichiersSurDossierFantomeLeveNetworkException(@TempDir Path parent) {
        Path fantome = parent.resolve("absent");
        assertThatExceptionOfType(NetworkException.class)
                .isThrownBy(() -> FileSystemWalker.trouverFichiers(fantome, ".csv"));
    }

    @Test
    void tailleTotaleSommeTouteLArborescence(@TempDir Path dossier) throws Exception {
        Files.write(dossier.resolve("racine.txt"), "12345".getBytes(StandardCharsets.UTF_8)); // 5 o
        Path sous = Files.createDirectories(dossier.resolve("sous"));
        Files.write(sous.resolve("interieur.txt"), "6789012".getBytes(StandardCharsets.UTF_8)); // 7 o

        // La récursion du FileVisitor compte le niveau 0 ET le niveau 1.
        assertThat(FileSystemWalker.tailleTotale(dossier)).isEqualTo(12);
    }

    /** Crée un fichier vide dans le dossier donné. */
    private static void toucher(Path dossier, String nom) throws IOException {
        Files.createFile(dossier.resolve(nom));
    }
}
