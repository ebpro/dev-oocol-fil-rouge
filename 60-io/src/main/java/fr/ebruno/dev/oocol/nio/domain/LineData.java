package fr.ebruno.dev.oocol.nio.domain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * L'enregistrement d'une ligne tel qu'il est lu dans {@code lignes.csv} —
 * distinct de la {@code Line} métier des semaines 4 et 5 : ici la ligne est
 * une DONNÉE brute en cours de résolution, pas encore un objet du domaine
 * avec sa cadence et ses invariants d'exploitation.
 *
 * <p><strong>Construction en deux temps.</strong> Une ligne CSV d'arrêt ne
 * contient que des CODES ({@code Ligne A;GAR;UNI;HOP;AER}) : la résolution
 * code → {@link Station} exige l'index complet des arrêts, donc le fichier
 * {@code stations.csv} doit avoir été lu AVANT. {@link NetworkLoader}
 * applique le protocole : {@link #parse(String)} valide le format et mémorise
 * les codes, puis {@link #resoudre(Map)} lie chaque code à son arrêt — un
 * code inconnu est une anomalie du fichier (référentiel incomplet), donc une
 * {@link NetworkException} qui cite la ligne en cause.
 *
 * <p><strong>Format CSV attendu.</strong> {@code nom;codeStation1;codeStation2;...}
 * — au moins un arrêt, sinon la « ligne » ne dessert rien et n'a pas à exister.
 *
 * @author Emmanuel Bruno
 * @see NetworkLoader
 * @see Station
 */
public class LineData {

    /** Nom de la ligne (ex. {@code "Ligne A"}). Immuable. */
    private final String nom;

    /** Codes d'arrêts tels que lus dans le CSV, dans l'ordre des stations. */
    private final List<String> codesArrets;

    /** Arrêts résolus par {@link #resoudre(Map)} ; liste vide tant que non résolue. */
    private final List<Station> arrets;

    /**
     * Construit une ligne à partir de son nom et de ses arrêts déjà résolus.
     *
     * <p>Copie défensive : la liste passée peut être modifiée par l'appelant
     * après coup, l'état interne de la ligne n'en bouge pas (leçon W5,
     * appliquée aux données lues). Les codes sont dérivés des arrêts, dans
     * le même ordre — une ligne résolue sait toujours d'où elle vient.
     *
     * @param nom    nom de la ligne, non vide
     * @param arrets arrêts de la ligne, au moins un
     * @throws IllegalArgumentException si le nom est vide ou la liste d'arrêts vide/null
     */
    public LineData(String nom, List<Station> arrets) {
        this(validerNom(nom));
        if (arrets == null || arrets.isEmpty()) {
            throw new IllegalArgumentException("Une ligne dessert au moins un arrêt.");
        }
        for (Station arret : arrets) {
            this.arrets.add(arret);
            this.codesArrets.add(arret.getCode());
        }
    }

    /**
     * État intermédiaire privé : ligne nommée, zéro arrêt — c'est l'état
     * produit par {@link #parse(String)} avant résolution. Jamais exposé
     * publiquement : le {@link NetworkLoader} ne rend une {@code LineData}
     * au client qu'après {@link #resoudre(Map)}.
     *
     * @param nom nom déjà validé
     */
    private LineData(String nom) {
        this.nom = nom;
        this.codesArrets = new ArrayList<>();
        this.arrets = new ArrayList<>();
    }

    /** Garde de nom partagée par les deux chemins de construction. */
    private static String validerNom(String nom) {
        if (nom == null || nom.isBlank()) {
            throw new IllegalArgumentException("Le nom de ligne est obligatoire.");
        }
        return nom.trim();
    }

    /**
     * Fabrique une ligne NON résolue depuis une ligne CSV.
     *
     * <p>Le résultat ne connaît encore que des codes : appeler
     * {@link #getArrets()} avant {@link #resoudre(Map)} renvoie une liste
     * vide — c'est {@link NetworkLoader} qui orchestre les deux phases pour
     * ne jamais exposer une ligne à l'état intermédiaire.
     *
     * @param csvLine une ligne complète au format {@code nom;code1;code2;...}
     * @return la ligne avec ses codes en attente de résolution
     * @throws NetworkException si le format est invalide ou si aucun code n'est présent
     */
    public static LineData parse(String csvLine) throws NetworkException {
        String[] champs = csvLine.split(";", -1);
        if (champs.length < 2) {
            throw new NetworkException(
                    "Ligne de parcours invalide (attendu « nom;arrêt1;arrêt2;... ») : \u00AB "
                            + csvLine + " \u00BB");
        }
        if (champs[0].trim().isEmpty()) {
            throw new NetworkException("Ligne sans nom dans : \u00AB " + csvLine + " \u00BB");
        }
        LineData brute = new LineData(validerNom(champs[0]));
        for (int i = 1; i < champs.length; i++) {
            String code = champs[i].trim();
            if (code.isEmpty()) {
                throw new NetworkException("Code d'arrêt vide dans : \u00AB " + csvLine + " \u00BB");
            }
            brute.codesArrets.add(code);
        }
        return brute;
    }

    /**
     * Résout les codes d'arrêts contre l'index des stations lues.
     *
     * <p>Méthode PACKAGE-PRIVÉE : la résolution est un détail d'orchestration
     * du {@link NetworkLoader}, pas une opération offerte au client — une
     * {@code LineData} qui sort de ce module est TOUJOURS résolue. Un code
     * absent de l'index est une référence brisée entre deux fichiers :
     * {@link NetworkException} citant le code ET la ligne.
     *
     * @param index code → arrêt, construit par le chargeur depuis {@code stations.csv}
     * @throws NetworkException si un code de la ligne ne correspond à aucun arrêt chargé
     */
    void resoudre(Map<String, Station> index) throws NetworkException {
        for (String code : codesArrets) {
            Station station = index.get(code);
            if (station == null) {
                throw new NetworkException("Arrêt inconnu \u00AB " + code + " \u00BB référencé par la ligne \u00AB "
                        + nom + " \u00BB");
            }
            arrets.add(station);
        }
    }

    /**
     * @return le nom de la ligne
     */
    public String getNom() {
        return nom;
    }

    /**
     * @return les codes d'arrêts bruts du CSV, dans l'ordre (vue non modifiable)
     */
    public List<String> getCodesArrets() {
        return Collections.unmodifiableList(codesArrets);
    }

    /**
     * @return les arrêts résolus, dans l'ordre du parcours (vue non modifiable)
     */
    public List<Station> getArrets() {
        return Collections.unmodifiableList(arrets);
    }

    /**
     * Représentation lisible : {@code "Ligne A [GAR, UNI, HOP, AER]"}.
     *
     * @return le nom suivi des codes d'arrêts dans l'ordre
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(nom).append(" [");
        for (int i = 0; i < codesArrets.size(); i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(codesArrets.get(i));
        }
        return sb.append("]").toString();
    }
}
