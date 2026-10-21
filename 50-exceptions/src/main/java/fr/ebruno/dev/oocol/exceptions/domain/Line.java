package fr.ebruno.dev.oocol.exceptions.domain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Une ligne du réseau : une suite <em>ordonnée</em> d'arrêts.
 *
 * <p><strong>Instantané W6 — la migration guidée « null → throws ».</strong>
 * La structure {@code List<Station>} du module 40 est conservée ; trois
 * décisions d'appel changent, et ce sont elles qui rougissent les tests W5
 * (volontairement — c'est la tension pédagogique de la séance) :
 * <ul>
 *   <li>{@link #trouverParCode(String)} ne <em>retourne plus</em>
 *       {@code null} sur absence : elle lève {@link StationNotFoundException}
 *       (unchecked — demander une station que la ligne ne dessert pas est un
 *       bug de l'appelant, pas une réponse métier) ;</li>
 *   <li>{@link #ajouterArret(Station)} ne <em>retourne plus</em>
 *       {@code false} sur doublon : elle lève {@link IllegalArgumentException}
 *       (IAE = paramètre invalide = bug appelant → unchecked par nature) ;</li>
 *   <li>{@link #valider()} apparaît : contrôle d'exploitabilité de la ligne,
 *       elle lève {@link ScheduleConflictException} — une erreur CHECKED,
 *       récupérable par re-planification. Le contraste avec les deux
 *       précédentes <em>est</em> la leçon.</li>
 * </ul>
 *
 * <p>Contrepoint assumé : {@link #retirerArret(Station)} garde son
 * {@code boolean}. Retirer un arrêt absent est une réponse métier valide
 * (« non, rien à retirer ») — <strong>tout {@code null}/{@code false} de
 * W5 ne mérite pas une exception</strong> ; transformer un contrôle normal
 * en signal d'erreur, c'est l'anti-pattern « exceptions pour le contrôle de
 * flux » que le module enseigne contre.
 *
 * @author Emmanuel Bruno
 * @see Station
 * @see ScheduleConflictException
 * @see StationNotFoundException
 */
public class Line {

    /** Cadence par défaut d'une ligne neuve : un départ toutes les 10 minutes. */
    private static final int FREQUENCE_DEFAUT = 10;

    /** En dessous, la cadence est irréaliste : {@link #valider()} la refuse. */
    private static final int FREQUENCE_MIN_REALISTE = 3;

    /** Nom commercial de la ligne (ex. {@code "Ligne A"}). */
    private String nom;

    /**
     * Les arrêts, dans l'ordre de parcours. Initialisé à la déclaration :
     * jamais {@code null} (leçon W5 conservée telle quelle).
     */
    private final List<Station> arrets = new ArrayList<>();

    /**
     * Intervalle minimal entre deux départs, en minutes. W6 l'introduit pour
     * avoir une vraie règle d'exploitabilité à <em>valider</em> — et donc une
     * vraie checked exception à lever ({@link ScheduleConflictException}).
     */
    private int frequenceMin = FREQUENCE_DEFAUT;

    /**
     * Crée une ligne sans arrêt, cadence par défaut (10 min) : on la garnit
     * ensuite avec {@link #ajouterArret(Station)}.
     *
     * @param nom nom commercial de la ligne, non vide
     * @throws IllegalArgumentException si le nom est {@code null} ou blanc
     */
    public Line(String nom) {
        setNom(nom);
    }

    /**
     * Crée une ligne avec une cadence explicite.
     *
     * @param nom          nom commercial de la ligne, non vide
     * @param frequenceMin intervalle minimal entre départs, en minutes
     *                     (&gt; 0 ; &lt; 3 passera mais sera refusé par {@link #valider()})
     * @throws IllegalArgumentException si le nom est blanc ou la cadence non positive
     */
    public Line(String nom, int frequenceMin) {
        setNom(nom);
        setFrequenceMin(frequenceMin);
    }

    /**
     * Ajoute un arrêt en fin de parcours.
     *
     * <p><strong>Migration W5 → W6.</strong> En W5, un doublon rendait
     * {@code false} — une réponse silencieuse que l'appelant pouvait
     * ignorer. En W6, il lève {@link IllegalArgumentException} : ajouter
     * deux fois la même station est un paramètre invalide, donc un bug de
     * l'appelant — unchecked par nature, documenté comme tel dans la séance.
     *
     * @param s l'arrêt à ajouter, non {@code null} et non déjà présent
     * @return {@code true} si l'arrêt a été ajouté (le {@code false} W5 n'existe plus)
     * @throws IllegalArgumentException si l'arrêt est {@code null} OU déjà desservi
     *                                  (les deux sont des erreurs du code appelant)
     */
    public boolean ajouterArret(Station s) {
        if (s == null) {
            throw new IllegalArgumentException("Impossible d'ajouter un arrêt null à la ligne " + nom);
        }
        if (arrets.contains(s)) { // comparaison par Station.equals (le contrat W5 œuvre toujours)
            throw new IllegalArgumentException(
                    "La ligne " + nom + " dessert déjà l'arrêt " + s.getCode() + " — doublon = bug de l'appelant.");
        }
        return arrets.add(s);
    }

    /**
     * Retire un arrêt par identité métier (même code = même station).
     *
     * <p>Contrepoint pédagogique : cette méthode GARDE son {@code boolean}
     * du module 40. « Rien à retirer » est une réponse métier légitime que
     * l'appelant traite par un {@code if} normal — l'en transformer en
     * exception serait utiliser les exceptions pour le contrôle de flux,
     * l'anti-pattern précisément combattu par la semaine.
     *
     * @param s l'arrêt à retirer (par code)
     * @return {@code true} si un arrêt a été retiré, {@code false} si absent
     */
    public boolean retirerArret(Station s) {
        if (s == null) {
            return false; // rien à retirer : la liste ne contient jamais de null
        }
        return arrets.remove(s);
    }

    /**
     * Retrouve un arrêt depuis son code d'exploitation.
     *
     * <p><strong>LA migration de la séance.</strong> Le module 40 rendait
     * {@code null} sur absence (contrat documenté… et oublié par les
     * appelants — le test W5 {@code trouverParCodeAbsentRetourneNull} le
     * figeait encore). Le module 50 lève {@link StationNotFoundException} :
     * une ligne qui ne dessert pas la station demandée signale un code
     * appelant mal construit, et l'échec frappe <em>au point de la demande</em>,
     * avec le code cherché dans le message — plus de NPE différée trois
     * méthodes plus loin.
     *
     * @param code le code cherché (ex. {@code "GAR"})
     * @return la station correspondante (jamais {@code null} — le contrat
     *         « nullable » a disparu de la signature)
     * @throws StationNotFoundException si la ligne ne dessert pas ce code
     *                                   (unchecked : c'est un bug, pas une condition de contrôle)
     */
    public Station trouverParCode(String code) {
        for (Station s : arrets) { // for-each : pas un lambda (leçon W5, inchangée)
            if (s.getCode().equals(code)) {
                return s;
            }
        }
        throw new StationNotFoundException(code); // W5 : return null — W6 : levée typée
    }

    /**
     * Contrôle d'exploitabilité de la ligne : sa cadence est-elle réaliste ?
     *
     * <p><strong>La checked exception par contraste.</strong> Une cadence
     * de 1 minute est irréaliste MAIS récupérable : l'exploitation peut
     * re-planifier les départs. C'est exactement le critère de choix
     * checked/unchecked de la séance — on OBLIGE l'appelant à prévoir la
     * gestion (try/catch ou {@code throws}), parce qu'il y a un plan B.
     * Le {@code heureConflit} vaut 0 : le conflit est structurel (la
     * cadence elle-même), pas lié à une heure de passage particulière —
     * l'appelant lit {@link ScheduleConflictException#getLigne()} pour
     * re-planifier sans parser le message.
     *
     * @throws ScheduleConflictException si {@code frequenceMin} est inférieur
     *                                   à 3 minutes (cadence irréaliste, récupérable par re-planification)
     */
    public void valider() throws ScheduleConflictException {
        if (frequenceMin < FREQUENCE_MIN_REALISTE) {
            throw new ScheduleConflictException(
                    "Cadence irréaliste sur " + nom + " : " + frequenceMin
                            + " min (< " + FREQUENCE_MIN_REALISTE + " min attendues) — re-planifier les départs.",
                    nom, 0);
        }
    }

    /**
     * @return le nombre d'arrêts de la ligne
     */
    public int nombreArrets() {
        return arrets.size();
    }

    /**
     * @return le premier arrêt du parcours (tête de la liste)
     * @throws IllegalStateException si la ligne n'a encore aucun arrêt
     *                               (unchecked : l'état demandé est incohérent — bug appelant, promis W5)
     */
    public Station premierArret() {
        if (arrets.isEmpty()) {
            throw new IllegalStateException("La ligne " + nom + " n'a aucun arrêt.");
        }
        return arrets.get(0);
    }

    /**
     * @return le dernier arrêt du parcours (queue de la liste)
     * @throws IllegalStateException si la ligne n'a encore aucun arrêt
     */
    public Station dernierArret() {
        if (arrets.isEmpty()) {
            throw new IllegalStateException("La ligne " + nom + " n'a aucun arrêt.");
        }
        return arrets.get(arrets.size() - 1);
    }

    /**
     * Une station est-elle un terminus de cette ligne ?
     *
     * @param s la station à tester
     * @return {@code true} si c'est le premier ou le dernier arrêt
     */
    public boolean estTerminus(Station s) {
        if (s == null || arrets.isEmpty()) {
            return false;
        }
        return s.equals(arrets.get(0)) || s.equals(arrets.get(arrets.size() - 1));
    }

    /**
     * @return le nom commercial de la ligne
     */
    public String getNom() {
        return nom;
    }

    /**
     * Change le nom de la ligne.
     *
     * @param nom le nouveau nom, non vide
     * @throws IllegalArgumentException si le nom est {@code null} ou blanc
     */
    public void setNom(String nom) {
        if (nom == null || nom.isBlank()) {
            throw new IllegalArgumentException("Le nom d'une ligne est obligatoire.");
        }
        this.nom = nom;
    }

    /**
     * @return l'intervalle minimal entre deux départs, en minutes
     */
    public int getFrequenceMin() {
        return frequenceMin;
    }

    /**
     * Règle la cadence de la ligne.
     *
     * <p>Validation en deux temps, thème de la séance : une cadence
     * {@code <= 0} est <em>impossible</em> → {@link IllegalArgumentException}
     * immédiate (bug appelant, unchecked) ; une cadence de 1-2 minutes est
     * <em>possible mais irréaliste</em> → acceptée ici, refusée plus tard
     * par {@link #valider()} en {@link ScheduleConflictException} (checked,
     * récupérable par re-planification). Les deux niveaux de gravité ont
     * deux canaux d'erreur distincts — c'est le choix à justifier en séance.
     *
     * @param frequenceMin l'intervalle en minutes, strictement positif
     * @throws IllegalArgumentException si la cadence est nulle ou négative
     */
    public void setFrequenceMin(int frequenceMin) {
        if (frequenceMin <= 0) {
            throw new IllegalArgumentException(
                    "Une cadence de " + frequenceMin + " min est impossible (<= 0) — bug de réglage.");
        }
        this.frequenceMin = frequenceMin;
    }

    /**
     * Les arrêts, dans l'ordre de parcours — en lecture seule (contrat W5 inchangé).
     *
     * @return une vue non modifiable des arrêts
     */
    public List<Station> getArrets() {
        return Collections.unmodifiableList(arrets);
    }

    /**
     * Représentation lisible : {@code "Ligne A [GAR → UNI → HOP] (3 arrêts)"}.
     *
     * @return le libellé de la ligne avec son parcours
     */
    @Override
    public String toString() {
        StringBuilder libelle = new StringBuilder(nom).append(" [");
        for (int i = 0; i < arrets.size(); i++) {
            if (i > 0) {
                libelle.append(" \u2192 "); // → : la flèche passe mieux en escape Unicode
            }
            libelle.append(arrets.get(i).getCode());
        }
        libelle.append("] (").append(arrets.size()).append(" arr\u00EAt");
        if (arrets.size() > 1) {
            libelle.append('s');
        }
        return libelle.append(')').toString();
    }
}
