package fr.ebruno.dev.oocol.nio.domain;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Charge le réseau « Trans'U » depuis un répertoire de trois fichiers CSV.
 *
 * <p><strong>Contrat du répertoire.</strong>
 * <ul>
 *   <li>{@code zones.csv} — une zone par ligne : {@code numero;nom} ;</li>
 *   <li>{@code stations.csv} — un arrêt par ligne : {@code code;nom;zoneNumero;accessible} ;</li>
 *   <li>{@code lignes.csv} — une ligne par ligne : {@code nom;codeStation1;codeStation2;...}.</li>
 * </ul>
 * Chaque fichier peut porter des lignes de commentaire (premier caractère
 * {@code #}) et des lignes vides : elles sont ignorées — un fichier de
 * configuration que l'on ne peut pas annoter est un fichier que l'on documente
 * ailleurs, c'est-à-dire nulle part.
 *
 * <p><strong>Lecture en trois phases, dans cet ordre.</strong> D'abord les
 * zones (index numéro → {@link Zone}), puis les arrêts — chaque arrêt lie
 * sa vraie zone et alimente l'index code → {@link Station} — enfin les
 * lignes, dont les codes sont résolus contre cet index. L'ordre n'est pas
 * un choix de style : c'est l'ordre des dépendances référentielles, et une
 * tentative de le permuter se traduirait par des {@link NetworkException}
 * « référence inconnue » sur un fichier pourtant valide.
 *
 * <p><strong>Le charset est un paramètre, jamais un défaut.</strong> Toute
 * lecture passe par {@link Files#newBufferedReader(Path, Charset)} avec un
 * {@link Charset} explicite. Oublier le charset, c'est hériter du
 * {@code charset.default} de la JVM — le même fichier lit alors « Gare
 * Centrale » correctement sur la machine d'un étudiant en UTF-8 et
 * « Gare CentrÃ© » sur un vieux poste Windows en Windows-1252. La leçon
 * tient en une phrase : <em>un octet n'a de sens que dans un encoding.</em>
 *
 * @author Emmanuel Bruno
 * @see Zone#parse(String)
 * @see Station#parse(String, Zone)
 * @see LineData#parse(String)
 */
public final class NetworkLoader {

    /** Nom du fichier des zones tarifaires. */
    private static final String FICHIER_ZONES = "zones.csv";

    /** Nom du fichier des arrêts. */
    private static final String FICHIER_STATIONS = "stations.csv";

    /** Nom du fichier des lignes. */
    private static final String FICHIER_LIGNES = "lignes.csv";

    /** Utilitaire statique : pas d'instance. */
    private NetworkLoader() {
    }

    /**
     * Charge le réseau depuis un répertoire, en UTF-8 (le contrat d'export).
     *
     * @param repertoire dossier contenant {@code zones.csv}, {@code stations.csv}
     *                   et {@code lignes.csv}
     * @return le réseau lu : zones, arrêts, lignes résolues
     * @throws NetworkException si un fichier est absent, illisible, ou contient
     *                          une ligne malformée — le message cite le fichier
     *                          et la ligne en cause
     */
    public static NetworkLoadResult charger(Path repertoire) throws NetworkException {
        return chargerAvecEncodage(repertoire, StandardCharsets.UTF_8);
    }

    /**
     * Charge le réseau en imposant le charset de lecture — la démonstration
     * que le même octet est DEUX textes différents selon l'encoding choisi.
     *
     * <p><strong>Pourquoi {@link StandardCharsets#ISO_8859_1} peut être le bon choix.</strong>
     * Un export vieux-jeu (Base de données géographique des années 2000, poste
     * Windows sans UTF-8) encode « Gare Centre » avec l'accent en UN octet
     * (0xE9 pour é). Lu en UTF-8, ces octets sont soit invalides (rejet :
     * {@code MalformedInputException} enrobée en {@link NetworkException}),
     * soit détournés : « Gare CentrÃ© ». Lu en ISO-8859-1, il retrouve son
     * sens. Sans le bon charset, « Gare Centre » devient « Gare CentrÃ© » —
     * littéralement : chaque octet UTF-8 de l'accent se traduit en un
     * caractère Latin-1 parasite.
     *
     * @param repertoire dossier des trois CSV
     * @param charset    encoding à utiliser pour les TROIS fichiers
     * @return le réseau lu
     * @throws NetworkException si un fichier est absent, illisible, ou si les
     *                          octets ne sont pas décodables dans le charset promis
     */
    public static NetworkLoadResult chargerAvecEncodage(Path repertoire, Charset charset)
            throws NetworkException {
        Map<Integer, Zone> zonesParNumero = new HashMap<>();
        List<Zone> zones = new ArrayList<>();
        for (String ligne : lireLignes(repertoire.resolve(FICHIER_ZONES), charset)) {
            Zone zone = Zone.parse(ligne);
            zones.add(zone);
            if (zonesParNumero.put(zone.getNumero(), zone) != null) {
                throw new NetworkException("Zone dupliquée : le numéro " + zone.getNumero()
                        + " apparaît deux fois dans " + FICHIER_ZONES);
            }
        }

        Map<String, Station> stationsParCode = new HashMap<>();
        List<Station> stations = new ArrayList<>();
        for (String ligne : lireLignes(repertoire.resolve(FICHIER_STATIONS), charset)) {
            Station brut = Station.parse(ligne); // phase 1 : le format, la zone par son numéro
            Zone zone = zonesParNumero.get(brut.getZone().getNumero());
            if (zone == null) {
                throw new NetworkException("Arrêt référencant une zone inconnue (numéro "
                        + brut.getZone().getNumero() + ") : \u00AB " + ligne + " \u00BB dans "
                        + FICHIER_STATIONS);
            }
            Station station = Station.parse(ligne, zone); // phase 2 : lien vers la VRAIE zone
            stations.add(station);
            if (stationsParCode.put(station.getCode(), station) != null) {
                throw new NetworkException("Arrêt dupliqué : le code " + station.getCode()
                        + " apparaît deux fois dans " + FICHIER_STATIONS);
            }
        }

        List<LineData> lignes = new ArrayList<>();
        for (String ligne : lireLignes(repertoire.resolve(FICHIER_LIGNES), charset)) {
            LineData ligneData = LineData.parse(ligne);
            ligneData.resoudre(stationsParCode); // peut lever : code inconnu
            lignes.add(ligneData);
        }

        return new NetworkLoadResult(zones, stations, lignes);
    }

    /**
     * Anti-pattern volontaire : lire SANS fermer, puis vouloir supprimer.
     *
     * <p>Ouvre un {@link BufferedReader} sur le fichier, lit la première
     * ligne, ne ferme JAMAIS le reader, puis tente {@link Files#delete}.
     * Le résultat est PLATFORM-DÉPENDANT, et c'est la leçon :
     * <ul>
     *   <li>Windows verrouille le fichier ouvert → {@code IOException} de
     *       suppression : la fuite est <em>visible</em> ;</li>
     *   <li>Linux/macOS (POSIX) supprime l'entrée de répertoire sans
     *       broncher → la méthode « réussit » : la fuite est <em>invisible</em>,
     *       l'inode (et son descripteur) restent vivants tant que le JVM
     *       tourne.</li>
     * </ul>
     * Ce succès POSIX est une grâce du système de fichiers, pas une garantie
     * Java : le jour où le même code croise un antivirus, un partage réseau
     * ou Windows, il casse. <em>La seule écriture qui ne dépend pas du bon
     * vouloir de l'OS est le try-with-resources</em> — W6 l'avait montré sur
     * {@code StringReader}, W7 l'impose sur de vrais fichiers.
     *
     * @param fichier le CSV à ouvrir (puis à tenter de supprimer)
     * @return la première ligne du fichier
     * @throws NetworkException si la lecture échoue, ou si la suppression est
     *                          refusée parce que le fichier est encore ouvert
     */
    public static String chargerSansFermer(Path fichier) throws NetworkException {
        BufferedReader reader;
        String premiereLigne;
        // ANTI-PATTERN VOLONTAIRE : pas de try-with-resources ici.
        try {
            reader = Files.newBufferedReader(fichier, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new NetworkException("Ouverture impossible : " + fichier, e);
        }
        try {
            premiereLigne = reader.readLine();
        } catch (IOException e) {
            throw new NetworkException("Lecture impossible : " + fichier, e);
        }
        // ... et reader n'est JAMAIS fermé : le descripteur reste ouvert.
        try {
            Files.delete(fichier);
        } catch (IOException e) {
            throw new NetworkException("Suppression refusée — le fichier est encore ouvert "
                    + "(fd fuite) : " + fichier, e);
        }
        return premiereLigne;
    }

    /**
     * Lit un fichier et n'en retient que les lignes « significatives » :
     * vides et commentaires (premier caractère {@code #}) retirés, fins de
     * ligne normalisées.
     *
     * <p>Le {@link BufferedReader} est fermé par try-with-resources — même
     * en cas de levée au milieu du fichier. C'est LA structure qui remplace
     * le {@code finally { reader.close(); }} que l'on oublie (ou que l'on
     * écrit faux : close() dans le finally peut masquer l'exception
     * primaire, cf. W6 « suppressed »).
     *
     * @param fichier le CSV à lire
     * @param charset encoding du fichier
     * @return les lignes significatives, dans l'ordre du fichier
     * @throws NetworkException si le fichier est absent ou ses octets indécodables
     */
    private static List<String> lireLignes(Path fichier, Charset charset) throws NetworkException {
        if (!Files.isRegularFile(fichier)) {
            throw new NetworkException("Fichier introuvable ou irrégulier : " + fichier);
        }
        List<String> lignes = new ArrayList<>();
        try (BufferedReader reader = Files.newBufferedReader(fichier, charset)) {
            String ligne;
            while ((ligne = reader.readLine()) != null) {
                String nette = ligne.trim();
                if (nette.isEmpty() || nette.charAt(0) == '#') {
                    continue; // ligne blanche ou commentaire : le fichier respire
                }
                lignes.add(nette);
            }
        } catch (IOException e) {
            // Charset incompatible → MalformedInputException (une IOException) :
            // enrobée, la cause technique survit dans le « Caused by: ».
            throw new NetworkException("Lecture impossible (" + charset + ") : " + fichier, e);
        }
        return lignes;
    }
}
