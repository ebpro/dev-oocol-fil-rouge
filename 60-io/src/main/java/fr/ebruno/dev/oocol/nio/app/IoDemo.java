package fr.ebruno.dev.oocol.nio.app;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Arrays;
import java.util.List;

import fr.ebruno.dev.oocol.nio.domain.FileSystemWalker;
import fr.ebruno.dev.oocol.nio.domain.LineData;
import fr.ebruno.dev.oocol.nio.domain.NetworkException;
import fr.ebruno.dev.oocol.nio.domain.NetworkLoadResult;
import fr.ebruno.dev.oocol.nio.domain.NetworkLoader;
import fr.ebruno.dev.oocol.nio.domain.ScheduleWriter;
import fr.ebruno.dev.oocol.nio.domain.Station;
import fr.ebruno.dev.oocol.nio.domain.Zone;

/**
 * Les six scènes du fichier, jouées en répertoire temporaire.
 *
 * <p><strong>Lecture.</strong> Chaque scène est une méthode privée autonome
 * que l'on peut commenter séparément ; {@link #main(String[])} les
 * enchaîne, titre, et garantit le grand ménage : le répertoire temporaire
 * créé en ouverture est supprimé récursivement dans le {@code finally},
 * quoi qu'il arrive — le principe « une ressource se ferme » s'applique aussi
 * aux arborescences que l'on crée.
 *
 * <p><strong>Zéro pollution.</strong> La démo n'écrit que sous {@code
 * Files.createTempDirectory} : ni le dépôt, ni le poste de développement ne
 * sont touchés, elle court donc partout, CI comprise.
 *
 * @author Emmanuel Bruno
 * @see fr.ebruno.dev.oocol.nio.domain
 */
public final class IoDemo {

    /** Utilitaire statique : pas d'instance. */
    private IoDemo() {
    }

    /**
     * Enchaîne les six scènes dans un répertoire temporaire, puis le détruit.
     *
     * @param args non utilisé
     * @throws IOException      pire cas des créations temporaires (le disque refuse)
     * @throws NetworkException scène 1 ou 2 : un CSV refusé abort la démo — à ce
     *                          niveau d'entrée, propager la checked est le choix
     *                          honnête (pas de catch qui avale ; cf. modules 50-60)
     */
    public static void main(String[] args) throws IOException, NetworkException {
        Path racine = Files.createTempDirectory("oocol-io-demo");
        try {
            scene1ChargerUnReseau(racine);
            scene2EcrireUnHoraire(racine);
            scene3AlgebreDesChemins(racine);
            scene4Charset(racine);
            scene5VisiteArborescence(racine);
            scene6ErreurEtAntiPattern(racine);
            System.out.println();
            System.out.println("=== Fin de la d\u00E9mo : le fichier est un partenaire, pas une variable. ===");
        } finally {
            detuireRecursivement(racine);
            System.out.println("(répertoire temporaire supprimé : " + racine + ")");
        }
    }

    /** Titre de scène, même gabarit partout. */
    private static void titre(int numero, String nom) {
        System.out.println();
        System.out.println("=== Scène " + numero + " \u2014 " + nom + " ===");
    }

    /**
     * Scène 1 — ÉCRIRE PUIS RELIRE : les fixtures naissent sous la main,
     * le chargeur les retrouve comme un import réel les trouverait.
     */
    private static void scene1ChargerUnReseau(Path racine) throws IOException, NetworkException {
        titre(1, "Charger un réseau depuis trois CSV");
        Path reseau = racine.resolve("reseau");
        Files.createDirectories(reseau);
        ecrireTexte(reseau.resolve("zones.csv"), StandardCharsets.UTF_8,
                "# Zones tarifaires du réseau trans'U\n"
                        + "1;Centre-ville\n"
                        + "2;P\u00E9riph\u00E9rie\n"
                        + "3;Grande couronne\n");
        ecrireTexte(reseau.resolve("stations.csv"), StandardCharsets.UTF_8,
                "# Arr\u00EAts du r\u00E9seau (code;nom;zone;accessible)\n"
                        + "GAR;Gare Centrale;1;true\n"
                        + "UNI;Universit\u00E9;1;true\n"
                        + "HOP;H\u00F4pital Sud;2;true\n"
                        + "AER;A\u00E9roport;3;true\n");
        ecrireTexte(reseau.resolve("lignes.csv"), StandardCharsets.UTF_8,
                "# Lignes (nom;station1;station2;...)\n"
                        + "Ligne A;GAR;UNI;HOP;AER\n");

        NetworkLoadResult resau = NetworkLoader.charger(reseau);
        System.out.println("  -> " + resau);
        for (Zone zone : resau.getZones()) {
            System.out.println("     " + zone);
        }
        for (Station arret : resau.getStations()) {
            System.out.println("     " + arret);
        }
        for (LineData ligne : resau.getLignes()) {
            System.out.println("     " + ligne + " (arr\u00EAts r\u00E9solus : "
                    + ligne.getArrets().size() + ")");
        }
    }

    /**
     * Scène 2 — ÉCRIRE UN HORAIRE : le ScheduleWriter produit le fichier,
     * on le relit ligne à ligne pour preuve.
     */
    private static void scene2EcrireUnHoraire(Path racine) throws IOException, NetworkException {
        titre(2, "\u00C9crire un horaire, atomiquement, puis le relire");
        NetworkLoadResult resau = NetworkLoader.charger(racine.resolve("reseau"));
        Path cible = racine.resolve("horaire-soir.txt");
        // Premier départ 18:00 (= 1080 min), toutes les 30 min jusqu'à 23:30.
        ScheduleWriter.ecrireHoraire(cible, resau.getLignes(), 18 * 60, 30);
        System.out.println("  -> cible \u00E9crite : " + cible.getFileName()
                + " ; temporaire r\u00E9siduel : "
                + Files.exists(Path.of(cible.toString() + ".tmp")) + " (false = move atomique r\u00E9ussi)");
        try (BufferedReader reader = Files.newBufferedReader(cible, StandardCharsets.UTF_8)) {
            String ligne;
            while ((ligne = reader.readLine()) != null) {
                System.out.println("     | " + ligne);
            }
        }
    }

    /**
     * Scène 3 — L'ALGÈBRE DES CHEMINS : resolve, normalize, relativize.
     */
    private static void scene3AlgebreDesChemins(Path racine) {
        titre(3, "Path : composer, normaliser, relativiser");
        Path base = FileSystemWalker.resoudreChemine(racine.toString(), "reseau", "zones.csv");
        System.out.println("  -> compos\u00E9 : " + base);
        System.out.println("     getFileName()   = " + base.getFileName());
        System.out.println("     getNameCount()  = " + base.getNameCount());
        System.out.println("     getName(0)      = " + base.getName(0));
        Path avecRemontee = racine.resolve("reseau/../horaire-soir.txt");
        System.out.println("  -> \u00AB " + avecRemontee.getFileName() + " \u00BB \u00E9crit avec un .. :");
        System.out.println("     brut     : " + avecRemontee);
        System.out.println("     normalis\u00E9 : " + avecRemontee.normalize());
        System.out.println("  -> relativiser depuis la racine : " + racine.relativize(base));
    }

    /**
     * Scène 4 — LE CHARSET : le même texte, deux fichiers différents ; et
     * un octet lu dans le mauvais encoding est un autre caractère.
     */
    private static void scene4Charset(Path racine) throws IOException {
        titre(4, "Charset : \u00AB H\u00F4pital \u00BB vaut deux fichiers distincts");
        String texte = "H\u00F4pital";
        Path enUtf8 = racine.resolve("hopital-utf8.txt");
        Path enLatin1 = racine.resolve("hopital-latin1.txt");
        ecrireTexte(enUtf8, StandardCharsets.UTF_8, texte);
        ecrireTexte(enLatin1, StandardCharsets.ISO_8859_1, texte);

        byte[] octetsUtf8 = Files.readAllBytes(enUtf8);
        byte[] octetsLatin1 = Files.readAllBytes(enLatin1);
        System.out.println("  -> \u00AB " + texte + " \u00BB en UTF-8    : " + octetsUtf8.length
                + " octets " + Arrays.toString(octetsUtf8));
        System.out.println("  -> \u00AB " + texte + " \u00BB en Latin-1 : " + octetsLatin1.length
                + " octets " + Arrays.toString(octetsLatin1));
        // L'inverse EST possible et instructif : relire le Latin-1 comme s'il
        // était de l'UTF-8 ne lève même pas toujours — mais le texte ment.
        String reluEnMauvais = new String(octetsLatin1, StandardCharsets.UTF_8);
        System.out.println("  -> octets Latin-1 d\u00E9cod\u00E9s en UTF-8 : \u00AB " + reluEnMauvais
                + " \u00BB (le \u00BB n'est pas le \u00AB \u00BB : m\u00EAme octet, deux lectures)");
        String correctUtf8 = Files.readString(enUtf8, StandardCharsets.UTF_8);
        String correctLatin1 = Files.readString(enLatin1, StandardCharsets.ISO_8859_1);
        System.out.println("  -> chacun relu DANS SON encoding retrouve \u00AB " + texte + " \u00BB : "
                + (texte.equals(correctUtf8) && texte.equals(correctLatin1)));
    }

    /**
     * Scène 5 — DESCENDRE L'ARBORESCENCE : un FileVisitor anonyme énumère
     * tout ce que la démo a produit, et le total s'affiche.
     */
    private static void scene5VisiteArborescence(Path racine) throws NetworkException {
        titre(5, "FileVisitor : la d\u00E9mo compte ses propres fichiers");
        System.out.println("  -> CSV trouv\u00E9s dans reseau/ : "
                + FileSystemWalker.trouverFichiers(racine.resolve("reseau"), ".csv").size());
        try {
            Files.walkFileTree(racine, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path fichier, BasicFileAttributes attrs) {
                    System.out.println("     " + racine.relativize(fichier));
                    return FileVisitResult.CONTINUE;
                }
            });
            System.out.println("  -> taille totale : " + FileSystemWalker.tailleTotale(racine) + " octets");
        } catch (IOException e) {
            throw new NetworkException("Visite interrompue : " + racine, e);
        }
    }

    /**
     * Scène 6 — L'ERREUR RÉCUPÉRABLE ET L'ANTI-PATTERN : le fichier absent
     * est un état du monde (on vérifie l'export, on recommence), le reader
     * non fermé est une faute (et qui plus est, plateforme-dépendante).
     */
    private static void scene6ErreurEtAntiPattern(Path racine) throws IOException {
        titre(6, "NetworkException sur fichier absent ; puis l'anti-pattern sans fermer");
        try {
            NetworkLoader.charger(racine.resolve("repertoire-qui-n-existe-pas"));
            System.out.println("  -> inattendu : jamais on ne lit un dossier fant\u00F4me.");
        } catch (NetworkException e) {
            System.out.println("  -> rattrap\u00E9 (checked, comme promis en W6) : " + e.getMessage());
            System.out.println("     le plan B m\u00E9tier est \u00E9crit : v\u00E9rifier l'export, relancer la lecture.");
        }

        // L'anti-pattern, joué sur une COPIE jetable : chargerSansFermer supprime
        // ce qu'il a lu — hors de question de sacrifier les fixtures de la scène 1.
        Path original = racine.resolve("reseau/zones.csv");
        Path copie = racine.resolve("zones-copie.csv");
        Files.copy(original, copie);
        try {
            String premiere = NetworkLoader.chargerSansFermer(copie);
            System.out.println("  -> anti-pattern « r\u00E9ussi » : premi\u00E8re ligne lue = \u00AB " + premiere
                    + " \u00BB, fichier supprim\u00E9 malgr\u00E9 le reader ouvert.");
            System.out.println("    sous POSIX c'est une GR\u00C2CE (fd fuite invisible), sous Windows une "
                    + "IOException : le try-with-resources est la seule \u00E9criture qui ne n\u00E9gocie pas l'OS.");
        } catch (NetworkException e) {
            System.out.println("  -> anti-pattern PAYANT : " + e.getMessage());
            System.out.println("     la suppression a \u00E9t\u00E9 refus\u00E9e parce que le reader n'a jamais \u00E9t\u00E9 ferm\u00E9.");
        }
    }

    /** Écrit un texte complet dans un fichier, UTF-8 ou charset imposé. */
    private static void ecrireTexte(Path fichier, Charset charset, String contenu) throws IOException {
        try (BufferedWriter writer = Files.newBufferedWriter(fichier, charset)) {
            writer.write(contenu);
        }
    }

    /**
     * Supprime une arborescence : les fichiers d'abord, le dossier ensuite —
     * un {@code delete} sur un dossier non vide lève {@code DirectoryNotEmptyException},
     * d'où la récursion avant le geste (ou {@code walkFileTree} en post-ordre :
     * même le ménage a son ordre de passage).
     */
    private static void detuireRecursivement(Path racine) throws IOException {
        if (!Files.exists(racine)) {
            return;
        }
        try (DirectoryStream<Path> entrees = Files.newDirectoryStream(racine)) {
            for (Path entree : entrees) {
                if (Files.isDirectory(entree)) {
                    detuireRecursivement(entree);
                } else {
                    Files.delete(entree);
                }
            }
        }
        Files.delete(racine);
    }
}
