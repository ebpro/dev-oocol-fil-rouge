package fr.ebruno.dev.oocol.nio.domain;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

/**
 * Écrit les horaires du réseau dans un fichier texte — le geste INVERSE du
 * {@link NetworkLoader} : produire des octets que d'autres liront.
 *
 * <p><strong>Écriture atomique : la raison d'être du {@code .tmp}.</strong>
 * {@link #ecrireHoraire} écrit dans un fichier temporaire du même dossier,
 * puis le déplace sur la cible par {@link Files#move(Path, Path,
 * StandardCopyOption...) Files.move(..., ATOMIC_MOVE, REPLACE_EXISTING)}.
 * Pourquoi ce détour ? Parce qu'un {@code BufferedWriter} ouvert directement
 * sur la cible produit un état intermédiaire OBSERVABLE : si le processus
 * meurt (kill, panne, quota) en plein milieu, le fichier cible est là,
 * tronqué — pire qu'absent, car faux. Avec le détour par le temporaire :
 * soit le move passe, et le lecteur voit TOUJOURS un horaire complet (l'ancien
 * ou le nouveau, jamais un demi) ; soit il ne passe pas, et l'ancien survit.
 * Sur un même système de fichiers, {@code ATOMIC_MOVE} est le petit frère
 * du « tout ou rien » des transactions — la leçon W6 « une ressource non
 * fermée corrompt » devient ici « une écriture non atomique corrompt aussi,
 * même parfaitement fermée ».
 *
 * <p><strong>Format de sortie.</strong> Par ligne :
 * {@code Ligne A \u2014 Départs: 06:00, 06:15, 06:30, ...}, borné à 23:59 —
 * un départ calculé au-delà de minuit n'est pas « le lendemain », c'est un
 * bug de cadence qu'on n'expose pas aux usagers.
 *
 * @author Emmanuel Bruno
 * @see NetworkLoader
 */
public final class ScheduleWriter {

    /** Minute de la dernière représentation valide d'une journée (23:59). */
    private static final int FIN_DE_JOURNEE = 23 * 60 + 59;

    /** Entête de tout fichier d'horaires écrit. */
    private static final String EN_TETE = "Horaires du r\u00E9seau Trans'U";

    /** Le fichier temporaire porte ce suffixe, collé au nom de la cible. */
    private static final String SUFFIXE_TMP = ".tmp";

    /** Utilitaire statique : pas d'instance. */
    private ScheduleWriter() {
    }

    /**
     * Écrit les horaires de début de soirée de chaque ligne, atomiquement.
     *
     * <p>Le fichier est produit en UTF-8 ({@link StandardCharsets#UTF_8}
     * explicite, même plaidoyer qu'à la lecture : le charset est un contrat
     * entre l'écrivain et le lecteur, jamais le hasard de la JVM). Les
     * départs démarrent à {@code premierDepart} et avancent de
     * {@code frequenceMin} minutes, tant qu'ils restent dans la journée.
     * Une liste de lignes vide produit QUAND MÊME un fichier : un entête
     * sans ligne est un horaire honnête (« aujourd'hui, rien »), pas une
     * absence — la différence compte pour le script qui testera
     * {@code Files.exists} en aval.
     *
     * @param fichier       chemin cible (le parent doit exister)
     * @param lignes        lignes dont les départs doivent être écrits
     * @param premierDepart heure de premier départ, en minutes depuis minuit (0..1439)
     * @param frequenceMin  écart entre deux départs, en minutes, strictement positif
     * @throws NetworkException si l'écriture du temporaire ou le move atomique échouent
     * @throws IllegalArgumentException si {@code fichier}/{@code lignes} est {@code null},
     *                                  le départ hors journée ou la fréquence non positive (bug d'appelant)
     */
    public static void ecrireHoraire(Path fichier, List<LineData> lignes, int premierDepart, int frequenceMin)
            throws NetworkException {
        if (fichier == null) {
            throw new IllegalArgumentException("Le chemin cible est obligatoire.");
        }
        if (lignes == null) {
            throw new IllegalArgumentException("La liste des lignes est obligatoire (vide \u2260 null).");
        }
        if (premierDepart < 0 || premierDepart > FIN_DE_JOURNEE) {
            throw new IllegalArgumentException(
                    "Le premier départ doit être dans la journée (0.." + FIN_DE_JOURNEE + "), reçu : " + premierDepart);
        }
        if (frequenceMin <= 0) {
            throw new IllegalArgumentException("La fréquence doit être strictement positive, reçu : " + frequenceMin);
        }

        // Étape 1 — le temporaire, dans le MÊME dossier que la cible :
        // ATOMIC_MOVE n'est garanti que entre chemins d'un même système de fichiers.
        Path temporaire = fichier.toAbsolutePath().resolveSibling(
                fichier.getFileName().toString() + SUFFIXE_TMP);
        try (BufferedWriter writer = Files.newBufferedWriter(temporaire, StandardCharsets.UTF_8)) {
            writer.write(EN_TETE);
            writer.newLine();
            for (LineData ligne : lignes) {
                List<Integer> departs = calculerDeparts(premierDepart, frequenceMin);
                StringBuilder ligneDeSortie = new StringBuilder(ligne.getNom())
                        .append(" \u2014 D\u00E9parts: ");
                for (int i = 0; i < departs.size(); i++) {
                    if (i > 0) {
                        ligneDeSortie.append(", ");
                    }
                    ligneDeSortie.append(formaterHeure(departs.get(i)));
                }
                writer.write(ligneDeSortie.toString());
                writer.newLine();
            }
            // writer est fermé (et donc vidé sur le disque) à la sortie du bloc
            // try-with-resources — AVANT tout mouvement : un move sur un fichier
            // non vidé déplacerait des octets périmés.
        } catch (IOException e) {
            nettoyerSilencieusement(temporaire);
            throw new NetworkException("\u00C9criture impossible vers le temporaire : " + temporaire, e);
        }

        // Étape 2 — le basculement atomique. ATOMIC_MOVE : le lecteur voit
        // l'ancien ou le nouveau fichier, jamais l'état du milieu.
        // REPLACE_EXISTING : ré-écrire un horaire remplace l'ancien en un geste.
        try {
            Files.move(temporaire, fichier,
                    StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            nettoyerSilencieusement(temporaire);
            throw new NetworkException("Le basculement atomique a échoué vers : " + fichier, e);
        }
    }

    /**
     * Formate une heure de la journée en {@code HH:mm}.
     *
     * @param minutesDepuisMinuit instant dans la journée, 0 (00:00) à 1439 (23:59)
     * @return la chaîne {@code "HH:mm"} correspondante
     * @throws IllegalArgumentException si l'instant sort de la journée
     */
    public static String formaterHeure(int minutesDepuisMinuit) {
        if (minutesDepuisMinuit < 0 || minutesDepuisMinuit > FIN_DE_JOURNEE) {
            throw new IllegalArgumentException("Heure hors journée (0.." + FIN_DE_JOURNEE + ") : "
                    + minutesDepuisMinuit);
        }
        int heures = minutesDepuisMinuit / 60;
        int minutes = minutesDepuisMinuit % 60;
        return String.format("%02d:%02d", heures, minutes);
    }

    /**
     * Calcule la suite des départs d'une ligne : {@code premierDepart},
     * {@code premierDepart + frequence}, ... tant que ≤ 23:59.
     *
     * <p>Packagée (et non privée) pour être testable directement : la
     * borne « le dernier départ de la soirée n'est pas minuit passé » est
     * exactement le genre de condition qui bogue silencieusement dans un
     * fichier écrit à 3 h du matin par un cron.
     *
     * @param premierDepart premier départ, minutes depuis minuit
     * @param frequenceMin  écart entre deux départs, strictement positif
     * @return la liste ordonnée des départs, jamais vide (contient au moins premierDepart)
     */
    static List<Integer> calculerDeparts(int premierDepart, int frequenceMin) {
        List<Integer> departs = new ArrayList<>();
        for (int t = premierDepart; t <= FIN_DE_JOURNEE; t += frequenceMin) {
            departs.add(t);
        }
        return departs;
    }

    /** Best-effort : un temporaire orphelin ne doit pas masquer l'erreur primaire. */
    private static void nettoyerSilencieusement(Path temporaire) {
        try {
            Files.deleteIfExists(temporaire);
        } catch (IOException ignored) {
            // Le fichier résiduel se superposera au prochain essai (REPLACE_EXISTING
            // du move ne concerne que la cible) ; rien de plus à faire ici.
        }
    }
}
