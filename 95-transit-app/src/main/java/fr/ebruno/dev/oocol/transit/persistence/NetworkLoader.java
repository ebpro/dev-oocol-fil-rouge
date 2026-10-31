package fr.ebruno.dev.oocol.transit.persistence;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import fr.ebruno.dev.oocol.transit.domain.Network;
import fr.ebruno.dev.oocol.transit.domain.NetworkBuilder;
import fr.ebruno.dev.oocol.transit.domain.NetworkException;

/**
 * Charge un réseau depuis trois fichiers CSV — le concept du module 60,
 * raconté cette fois à travers le Builder.
 *
 * <p><strong>Comparez avec le module 60 : même parsing CSV, mais la construction passe
 * par le Builder → plus lisible.</strong> Le {@code NetworkLoader} du module
 * 60 devait maintenir lui-même les index {@code numéro → Zone} et
 * {@code code → Station}, faire la résolution des lignes à la main et
 * assembler le résultat dans un objet conteneur. Ici, le loader ne fait PLUS
 * que ce qui est propre au CSV : lire des lignes, les découper en champs,
 * les vérifier. La cohérence référentielle (zone inconnue, code dupliqué,
 * arrêt introuvable) est déléguée au {@link NetworkBuilder#construire()} —
 * une seule implémentation de ces règles pour le CSV, le JDBC et le code
 * duré. SRP gagnée sur toute la ligne.
 *
 * <p><strong>Contrat du répertoire (identique au module 60).</strong>
 * <ul>
 *   <li>{@code zones.csv} — une zone par ligne : {@code numero;nom} ;</li>
 *   <li>{@code stations.csv} — un arrêt par ligne : {@code code;nom;zoneNumero;accessible} ;</li>
 *   <li>{@code lignes.csv} — une ligne par ligne : {@code nom;codeStation1;codeStation2;...}.</li>
 * </ul>
 * Lignes vides et commentaires ({@code #} en premier caractère) ignorés ;
 * lecture en UTF-8 explicite (module 60 : <em>un octet n'a de sens que dans un
 * encoding</em>).
 *
 * @author Emmanuel Bruno
 * @see NetworkJdbcStore — l'autre stratégie de chargement, à JDBC
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
        throw new AssertionError("NetworkLoader est une classe utilitaire : pas d'instanciation.");
    }

    /**
     * Charge le réseau depuis un répertoire de trois CSV, en UTF-8.
     *
     * <p>Les lignes sont empilées dans un {@link NetworkBuilder} au fil de
     * l'eau — le code du chargement se lit comme l'inventaire du fichier :
     * une zone, une autre, un arrêt, une autre, une ligne… puis
     * {@code construire()} tranche : soit le réseau est cohérent, soit une
     * {@link NetworkException} nomme le défaut.
     *
     * @param repertoire dossier contenant {@code zones.csv}, {@code stations.csv}
     *                   et {@code lignes.csv}
     * @return le réseau chargé, validé par le Builder
     * @throws NetworkException si un fichier est absent, une ligne malformée,
     *                          ou la cohérence référentielle rompue
     */
    public static Network chargerDepuisRepertoire(Path repertoire) throws NetworkException {
        NetworkBuilder builder = Network.builder();

        for (String ligne : lireLignes(repertoire.resolve(FICHIER_ZONES))) {
            String[] champs = ligne.split(";", -1);
            if (champs.length != 2) {
                throw new NetworkException("Ligne de zone attendue \u00AB numero;nom \u00BB : "
                        + ligne + " (" + FICHIER_ZONES + ")");
            }
            builder.zone(entier(champs[0].trim(), ligne, FICHIER_ZONES), champs[1].trim());
        }

        for (String ligne : lireLignes(repertoire.resolve(FICHIER_STATIONS))) {
            String[] champs = ligne.split(";", -1);
            if (champs.length != 4) {
                throw new NetworkException(
                        "Ligne d'arrêt attendue \u00AB code;nom;zone;accessible \u00BB : "
                        + ligne + " (" + FICHIER_STATIONS + ")");
            }
            boolean accessible = Boolean.parseBoolean(champs[3].trim());
            builder.station(champs[0].trim(), champs[1].trim(),
                    entier(champs[2].trim(), ligne, FICHIER_STATIONS), accessible);
        }

        for (String ligne : lireLignes(repertoire.resolve(FICHIER_LIGNES))) {
            String[] champs = ligne.split(";", -1);
            if (champs.length < 2) {
                throw new NetworkException("Ligne attendue \u00AB nom;arret1;arret2;... \u00BB : "
                        + ligne + " (" + FICHIER_LIGNES + ")");
            }
            String nom = champs[0].trim();
            String[] codes = new String[champs.length - 1];
            for (int i = 1; i < champs.length; i++) {
                codes[i - 1] = champs[i].trim();
            }
            builder.ligne(nom, codes);
        }

        return builder.construire();
    }

    /**
     * Parse un entier en enrobant l'échec dans l'exception métier — la scène
     * de chaînage du module 50, version CSV : le {@code NumberFormatException}
     * d'origine survit derrière {@code getCause()}, le message cite le fichier
     * et la ligne fautifs.
     */
    private static int entier(String brut, String ligne, String fichier) throws NetworkException {
        try {
            return Integer.parseInt(brut);
        } catch (NumberFormatException e) {
            throw new NetworkException("Nombre attendu, lu \u00AB " + brut + " \u00BB dans "
                    + fichier + " : " + ligne, e);
        }
    }

    /**
     * Lit un fichier et n'en retient que les lignes significatives : vides et
     * commentaires ({@code #}) retirés, fins normalisées. Try-with-resources :
     * le reader ferme même en cas de levée (modules 50-60).
     */
    private static List<String> lireLignes(Path fichier) throws NetworkException {
        if (!Files.isRegularFile(fichier)) {
            throw new NetworkException("Fichier introuvable ou irrégulier : " + fichier);
        }
        List<String> lignes = new ArrayList<>();
        try (BufferedReader reader = Files.newBufferedReader(fichier, StandardCharsets.UTF_8)) {
            String ligne;
            while ((ligne = reader.readLine()) != null) {
                String nette = ligne.trim();
                if (nette.isEmpty() || nette.charAt(0) == '#') {
                    continue; // ligne blanche ou commentaire : le fichier respire
                }
                lignes.add(nette);
            }
        } catch (IOException e) {
            throw new NetworkException("Lecture impossible : " + fichier, e);
        }
        return lignes;
    }
}
