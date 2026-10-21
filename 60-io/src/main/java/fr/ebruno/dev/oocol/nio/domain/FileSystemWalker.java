package fr.ebruno.dev.oocol.nio.domain;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Boîte à outils autour de {@link Path} : chercher, mesurer, composer des
 * chemins. Trois gestes NIO.2 qui couvrent l'essentiel de la semaine.
 *
 * <p><strong>Pourquoi pas {@code Files.walk()} ?</strong> {@code Files.walk}
 * retourne un {@code java.util.stream.Stream} — la API Streams est le
 * module 80 du fil rouge, et l'utiliser ici serait tricher deux fois :
 * d'abord avec le programme, ensuite avec la raison d'être de ces outils
 * (un {@code Stream} sur des fichiers DOIT être fermé — {@code try (Stream...)}
 * — une subtilité que la semaine 8 enseignera avec le recul nécessaire).
 * Ici on utilise donc {@link Files#newDirectoryStream} (pré-streams, lui
 * aussi {@code Closeable}, mais fermé par le même try-with-resources déjà
 * appris en W6/W7) et {@link Files#walkFileTree} avec un {@link
 * SimpleFileVisitor} anonyme : la visite impérative, explicite, ligne à
 * ligne — exactement le niveau d'abstraction de la semaine.
 *
 * @author Emmanuel Bruno
 * @see NetworkLoader
 */
public final class FileSystemWalker {

    /** Utilitaire statique : pas d'instance. */
    private FileSystemWalker() {
    }

    /**
     * Liste les fichiers d'un répertoire dont le nom se termine par
     * {@code extension}, triés par chemin.
     *
     * <p>Le motif de filtrage est construit en {@code "*" + extension} et
     * passé au {@link DirectoryStream} : c'est le système de fichiers qui
     * filtre à la source, pas une boucle qui filtre après coup. Le stream
     * est une ressource fermée par try-with-resources — un
     * {@code DirectoryStream} non fermé garde un descripteur de dossier
     * ouvert, le même piège que le reader de {@link
     * NetworkLoader#chargerSansFermer(Path)}.
     *
     * @param repertoire dossier à parcourir (NON récursif — {@link #tailleTotale(Path)}
     *                   montre la récursion par visiteur)
     * @param extension  suffixe recherché, point compris (ex. {@code ".csv"})
     * @return les chemins triés correspondant au motif, liste vide si rien ne matche
     * @throws NetworkException si le répertoire est illisible
     */
    public static List<Path> trouverFichiers(Path repertoire, String extension) throws NetworkException {
        List<Path> trouves = new ArrayList<>();
        String motif = "*" + extension;
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(repertoire, motif)) {
            for (Path entree : stream) {
                if (Files.isRegularFile(entree)) {
                    trouves.add(entree);
                }
            }
        } catch (IOException e) {
            throw new NetworkException("Exploration impossible : " + repertoire, e);
        }
        Collections.sort(trouves);
        return trouves;
    }

    /**
     * Somme des tailles (en octets) de TOUS les fichiers d'une arborescence,
     * racine comprise, récursion incluse.
     *
     * <p>L'outil est {@link Files#walkFileTree} : le JDK descend
     * lui-même l'arborescence et rappelle le visiteur à chaque entrée. Le
     * visiteur est un {@link SimpleFileVisitor} ANONYME — pas une lambda
     * (module 80) ni une classe nommée perdue dans le package : la visite
     * n'existe que pour cette somme. {@code visitFileFailed} retourne
     * {@code CONTINUE} : un fichier illisible (droits, lien brisé) est
     * ignoré sans interrompre la descente — la taille d'un réseau qu'on ne
     * peut pas tout à fait lire reste une information utile.
     *
     * @param repertoire racine de l'arborescence
     * @return la somme des tailles des fichiers réguliers, en octets
     * @throws NetworkException si la racine elle-même est inaccessible
     */
    public static long tailleTotale(Path repertoire) throws NetworkException {
        // Le compteur survit aux appels du visiteur via ce tableau à une case :
        // une variable locale ne peut pas être « mutée » depuis une classe anonyme
        // (elle devrait être effectively final — concept de la semaine 8).
        final long[] total = new long[1];
        try {
            Files.walkFileTree(repertoire, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path fichier, BasicFileAttributes attrs) throws IOException {
                    total[0] += Files.size(fichier);
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFileFailed(Path fichier, IOException exc) {
                    return FileVisitResult.CONTINUE; // entrée illisible : on passe, on descend quand même
                }
            });
        } catch (IOException e) {
            throw new NetworkException("Arpentage impossible : " + repertoire, e);
        }
        return total[0];
    }

    /**
     * Compose un chemin segment par segment — la démonstration de {@link
     * Path#of(String, String...)} et de {@link Path#resolve(Path)} /
     * {@link Path#resolve(String)}.
     *
     * <p>Pourquoi composer plutôt que concaténer des chaînes ? Parce que le
     * séparateur n'est pas le même partout ({@code /}, {@code \}), qu'un
     * {@code ..} doit pouvoir être normalisé, et qu'une chaîne de chemin ne
     * se compare ni ne se résout fiablement. {@link Path} est le seul type
     * qui connaît le système de fichiers sur lequel il vit.
     *
     * @param segments au moins un segment ; le premier fonde le chemin de base,
     *                 les suivants sont résolus les uns après les autres
     * @return le chemin composé, NON normalisé (le client appelle {@code normalize()}
     *         quand il le souhaite — normaliser sans qu'on le demande efface de
     *         l'information sur les liens symboliques)
     * @throws IllegalArgumentException si aucun segment n'est fourni (bug d'appelant)
     */
    public static Path resoudreChemine(String... segments) {
        if (segments == null || segments.length == 0) {
            throw new IllegalArgumentException("Il faut au moins un segment pour fonder un chemin.");
        }
        Path chemin = Path.of(segments[0]);
        for (int i = 1; i < segments.length; i++) {
            chemin = chemin.resolve(segments[i]);
        }
        return chemin;
    }
}
