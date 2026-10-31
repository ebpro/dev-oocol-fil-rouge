package fr.ebruno.dev.oocol.nio.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Tests du chargeur CSV — le cœur du module : ce qui entre par un fichier
 * doit en sortir fidèle, ou être refusé avec un diagnostic qui cite la
 * ligne en cause.
 *
 * <p>Les fixtures « heureuses » vivent dans le classpath de test ({@code
 * src/test/resources/reseaux/}, copié par Maven dans {@code
 * target/test-classes/}) : un jeu de données de référence versionné, relu à
 * chaque exécution — exactement les « fixtures de test dans
 * src/test/resources » que l'ELO 08 promet. Les cas d'erreur, eux, fabriquent
 * des dossiers éphémères ({@code @TempDir}) : on ne casse que ce que l'on a
 * créé soi-même.
 *
 * @author Emmanuel Bruno
 */
class NetworkLoaderTest {

    /** Le répertoire des fixtures, résolu depuis le classpath. */
    private static Path repertoireFixtures() {
        URL url = NetworkLoaderTest.class.getResource("/reseaux");
        assertThat(url).as("le classpath de test doit contenir /reseaux").isNotNull();
        try {
            return Path.of(url.toURI());
        } catch (URISyntaxException e) {
            throw new IllegalStateException("URL de fixture inexploitable : " + url, e);
        }
    }

    /** Écrit un CSV complet dans un fichier de {@code @TempDir}. */
    private static Path ecrireCsv(Path repertoire, String nom, String contenu) throws IOException {
        Path fichier = repertoire.resolve(nom);
        Files.write(fichier, contenu.getBytes(StandardCharsets.UTF_8));
        return fichier;
    }

    /** Un trio de fichiers minimal et valide, pour les tests d'erreur ciblés. */
    private static void reseauValideSans(Path repertoire, String fichierOMIS) throws IOException {
        if (!"zones.csv".equals(fichierOMIS)) {
            ecrireCsv(repertoire, "zones.csv", "1;Centre-ville\n2;Périphérie\n3;Grande couronne\n");
        }
        if (!"stations.csv".equals(fichierOMIS)) {
            ecrireCsv(repertoire, "stations.csv", "GAR;Gare Centrale;1;true\n");
        }
        if (!"lignes.csv".equals(fichierOMIS)) {
            ecrireCsv(repertoire, "lignes.csv", "Ligne A;GAR\n");
        }
    }

    @Test
    void chargeLesFixturesDuClasspath() throws NetworkException {
        NetworkLoadResult resau = NetworkLoader.charger(repertoireFixtures());
        assertThat(resau.getZones()).hasSize(3);
        assertThat(resau.getStations()).hasSize(7);
        assertThat(resau.getLignes()).hasSize(3);
    }

    @Test
    void zoneDeGarEstResolueAvecSonVeritableObjet() throws NetworkException {
        NetworkLoadResult resau = NetworkLoader.charger(repertoireFixtures());
        Station gar = null;
        for (Station s : resau.getStations()) {
            if ("GAR".equals(s.getCode())) {
                gar = s;
            }
        }
        assertThat(gar).as("GAR est dans les fixtures").isNotNull();
        assertThat(gar.getZone().getNumero()).isEqualTo(1);
        // La résolution n'est pas qu'un numéro : c'est le NOM lu dans zones.csv.
        assertThat(gar.getZone().getNom()).isEqualTo("Centre-ville");
    }

    @Test
    void lignesOntLeursArretsResolusDansLordre() throws NetworkException {
        NetworkLoadResult resau = NetworkLoader.charger(repertoireFixtures());
        LineData ligneA = resau.getLignes().get(0);
        assertThat(ligneA.getNom()).isEqualTo("Ligne A");
        assertThat(ligneA.getArrets()).hasSize(4);
        assertThat(ligneA.getArrets().get(0).getCode()).isEqualTo("GAR");
        assertThat(ligneA.getArrets().get(3).getCode()).isEqualTo("AER");
        // Un arrêt résolu EST l'objet station chargé (mêmes références métier).
        assertThat(ligneA.getArrets().get(0).getZone().getNom()).isEqualTo("Centre-ville");
    }

    @Test
    void commentairesEtLignesVidesSontIgnores(@TempDir Path dossier) throws Exception {
        ecrireCsv(dossier, "zones.csv",
                "# une ligne de commentaire\n"
                        + "\n"
                        + "   \n"
                        + "1;Centre-ville\n"
                        + "# un autre commentaire\n"
                        + "2;Périphérie\n");
        reseauValideSans(dossier, "zones.csv");
        ecrireCsv(dossier, "stations.csv",
                "\n# commentaire\nGAR;Gare Centrale;1;true\nUNI;Université;2;false\n");
        ecrireCsv(dossier, "lignes.csv", "Ligne A;GAR;UNI\n");

        NetworkLoadResult resau = NetworkLoader.charger(dossier);
        assertThat(resau.getZones()).hasSize(2);
        assertThat(resau.getStations()).hasSize(2);
        assertThat(resau.getLignes()).hasSize(1);
    }

    @Test
    void fichierManquantLeveNetworkException(@TempDir Path dossierVide) {
        assertThatExceptionOfType(NetworkException.class)
                .isThrownBy(() -> NetworkLoader.charger(dossierVide))
                .withMessageContaining("zones.csv");
    }

    @Test
    void repertoireInexistantLeveNetworkException(@TempDir Path parent) {
        Path fantome = parent.resolve("nulle-part");
        assertThatExceptionOfType(NetworkException.class)
                .isThrownBy(() -> NetworkLoader.charger(fantome))
                .withMessageContaining("introuvable");
    }

    @Test
    void ligneZoneMalformeeCiteLaLigneBrute(@TempDir Path dossier) throws Exception {
        ecrireCsv(dossier, "zones.csv", "1;Centre-ville\n4\n");
        reseauValideSans(dossier, "zones.csv");

        assertThatExceptionOfType(NetworkException.class)
                .isThrownBy(() -> NetworkLoader.charger(dossier))
                .withMessageContaining("4"); // le diagnostic NOMME la ligne fautive
    }

    @Test
    void numeroZoneNonEntierEstEnrobeAvecSaCause(@TempDir Path dossier) throws Exception {
        ecrireCsv(dossier, "zones.csv", "X;Centre-ville\n");
        reseauValideSans(dossier, "zones.csv");

        assertThatExceptionOfType(NetworkException.class)
                .isThrownBy(() -> NetworkLoader.charger(dossier))
                .havingCause() // chaînage des causes : le NumberFormatException survit derrière
                .isInstanceOf(NumberFormatException.class)
                .withMessageContaining("X");
    }

    @Test
    void stationReferencantUneZoneInconnueEstRefusee(@TempDir Path dossier) throws Exception {
        reseauValideSans(dossier, "stations.csv");
        ecrireCsv(dossier, "stations.csv", "GAR;Gare Centrale;9;true\n");

        assertThatExceptionOfType(NetworkException.class)
                .isThrownBy(() -> NetworkLoader.charger(dossier))
                .withMessageContaining("zone inconnue");
    }

    @Test
    void champAccessibleNonBooleenEstRefuse(@TempDir Path dossier) throws Exception {
        reseauValideSans(dossier, "stations.csv");
        ecrireCsv(dossier, "stations.csv", "GAR;Gare Centrale;1;peut-etre\n");

        assertThatExceptionOfType(NetworkException.class)
                .isThrownBy(() -> NetworkLoader.charger(dossier))
                .withMessageContaining("accessible");
    }

    @Test
    void ligneReferencantUnArretInconnuEstRefusee(@TempDir Path dossier) throws Exception {
        reseauValideSans(dossier, "lignes.csv");
        ecrireCsv(dossier, "lignes.csv", "Ligne Z;GAR;XYZ\n");

        assertThatExceptionOfType(NetworkException.class)
                .isThrownBy(() -> NetworkLoader.charger(dossier))
                .withMessageContaining("XYZ");
    }

    @Test
    void arretDupliqueEstRefuse(@TempDir Path dossier) throws Exception {
        reseauValideSans(dossier, "stations.csv");
        ecrireCsv(dossier, "stations.csv",
                "GAR;Gare Centrale;1;true\nGAR;Gare Bis;1;false\n");

        assertThatExceptionOfType(NetworkException.class)
                .isThrownBy(() -> NetworkLoader.charger(dossier))
                .withMessageContaining("dupliqué");
    }

    @Test
    void resultatExposeDesVuesNonModifiables(@TempDir Path dossier) throws Exception {
        reseauValideSans(dossier, "aucun");
        NetworkLoadResult resau = NetworkLoader.charger(dossier);

        List<Zone> zones = resau.getZones();
        assertThatExceptionOfType(UnsupportedOperationException.class)
                .isThrownBy(() -> zones.add(new Zone(9, " contre le contrat ")));
    }
}
