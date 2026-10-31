package fr.ebruno.dev.oocol.exceptions.domain;

/**
 * Une station du réseau « Trans'U », identifiée par son code d'exploitation
 * à trois lettres majuscules (ex. {@code "GAR"} pour Gare Centrale).
 *
 * <p><strong>La lecture « exceptions » d'une classe déjà vue.</strong>
 * Les champs, l'ordre {@link Comparable} et le contrat {@code equals}/
 * {@code hashCode} (par code) sont repris du module 40, inchangés. Ce que
 * ce module change est une façon de <em>nommer</em> ce qui était déjà là :
 * <ul>
 *   <li>les validations du constructeur et des setters lèvent des
 *       {@link IllegalArgumentException} — et la raison est directe : elles
 *       sont <em>unchecked par nature</em>, car un code
 *       malformé, un nom blanc, sont des paramètres invalides fournis par
 *       le code appelant, donc des <strong>bugs</strong>, pas des états du
 *       monde que l'on rattrape à l'exécution ;</li>
 *   <li>la station <em>absente</em> change de statut : le module 40 la
 *       rendait par {@code null} (contrat documenté, source d'oublis à l'usage) ;
 *       ce module la remplace par {@link StationNotFoundException} — voir
 *       {@link Line#trouverParCode(String)} et la fabrique de garde
 *       {@link #requireValide(Station, String)} ci-dessous.</li>
 * </ul>
 *
 * @author Emmanuel Bruno
 * @see Line
 * @see StationNotFoundException
 * @see Zone
 */
public class Station implements Comparable<Station> {

    /**
     * Code d'exploitation : exactement 3 lettres majuscules (ex. {@code "GAR"}).
     *
     * <p>{@code final} depuis le module 40 : c'est la clé d'identité, donc la clé de
     * hachage des collections qui contiennent la station.
     */
    private final String code;

    /** Nom commercial de la station (ex. {@code "Gare Centrale"}). Modifiable : donnée d'affichage. */
    private String nom;

    /** Accessibilité PMR. {@code false} par défaut (constructeur court). */
    private boolean accessible;

    /** Zone tarifaire à laquelle la station est rattachée. Modifiable : une station peut être re-zonée. */
    private Zone zone;

    /**
     * Constructeur « pratique » : une station non accessible (cas par défaut).
     *
     * @param nom  nom de la station, non vide
     * @param code code d'exploitation, 3 lettres majuscules
     * @param zone zone tarifaire de la station
     * @throws IllegalArgumentException si le nom est vide ou le code malformé
     *                                  (unchecked : le paramètre invalide est un bug appelant)
     */
    public Station(String nom, String code, Zone zone) {
        this(nom, code, zone, false);
    }

    /**
     * Constructeur complet : toutes les caractéristiques de la station.
     *
     * @param nom        nom de la station, non vide
     * @param code       code d'exploitation, exactement 3 lettres majuscules
     * @param zone       zone tarifaire de la station
     * @param accessible {@code true} si la station est accessible PMR
     * @throws IllegalArgumentException si le nom est vide, le code malformé ou la zone absente
     */
    public Station(String nom, String code, Zone zone, boolean accessible) {
        setNom(nom);
        // Le code est final : la validation lui est déléguée et RETOURNE la
        // valeur contrôlée — un champ final ne s'assigne qu'ici.
        this.code = codeValide(code);
        setZone(zone);
        this.accessible = accessible;
    }

    /**
     * Fabrique statique de convenance : {@code Station.of("GAR", "Gare Centrale", zone1)}.
     *
     * @param code code d'exploitation, 3 lettres majuscules
     * @param nom  nom de la station, non vide
     * @param zone zone tarifaire de la station
     * @return une station valide toute neuve
     * @throws IllegalArgumentException si un argument est invalide
     */
    public static Station of(String code, String nom, Zone zone) {
        return new Station(nom, code, zone);
    }

    /**
     * Méthode de garde : remplace le pattern « {@code return null} » par une levée
     * typée.
     *
     * <p>Avant ce module, une recherche ratée rendait {@code null} et chaque
     * appelant devait tester — un oubli de test déplaçait l'échec loin de sa
     * source (une {@code NullPointerException} au bout de la chaîne).
     * Maintenant : on passe le résultat éventuel dans {@code requireValide}
     * et l'échec est immédiat, typé ({@link StationNotFoundException}),
     * localisé au point d'usage. C'est la migration « null → throws » jouée
     * sur la station elle-même.
     *
     * <p>Particularité rapportée dans le « code » : la station absente n'a
     * pas de code à afficher (on ne peut pas interroger un objet qui
     * n'existe pas) — c'est le {@code context} d'appel qui joue le rôle de
     * diagnostic, ex. {@code "recherche depuis le réseau"}.
     *
     * @param s       la station à valider (peut être {@code null})
     * @param context où la recherche a eu lieu — cité dans le message
     * @return la station, si elle est présente
     * @throws StationNotFoundException si {@code s} est {@code null}
     *                                   (unchecked : bug de l'appelant qui a transmis l'absence)
     */
    public static Station requireValide(Station s, String context) {
        if (s == null) {
            throw new StationNotFoundException("absente (" + context + ")");
        }
        return s;
    }

    /**
     * Ordre naturel d'une station : par code alphabétique ({@code "AER" < "GAR"}).
     *
     * @param autre la station comparée
     * @return un négatif, {@code 0}, ou un positif selon l'ordre des codes
     */
    @Override
    public int compareTo(Station autre) {
        return this.code.compareTo(autre.code);
    }

    /**
     * @return le nom commercial de la station
     */
    public String getNom() {
        return nom;
    }

    /**
     * Change le nom de la station (donnée d'affichage : sans effet sur l'identité).
     *
     * @param nom le nouveau nom, non vide
     * @throws IllegalArgumentException si le nom est {@code null} ou blanc
     *                                  (unchecked : paramètre invalide = bug appelant, jamais
     *                                  une situation à rattraper par un try/catch « utile »)
     */
    public void setNom(String nom) {
        if (nom == null || nom.isBlank()) {
            throw new IllegalArgumentException("Le nom d'une station est obligatoire.");
        }
        this.nom = nom;
    }

    /**
     * @return le code d'exploitation (3 lettres majuscules) — la clé d'identité
     */
    public String getCode() {
        return code;
    }

    /**
     * Validation du code, appelée par le constructeur seul (champ {@code final}).
     *
     * @param code le code à valider
     * @return le code, s'il est conforme
     * @throws IllegalArgumentException si le code ne respecte pas le format
     */
    private static String codeValide(String code) {
        if (code == null || !code.matches("[A-Z]{3}")) {
            throw new IllegalArgumentException(
                    "Le code d'une station doit compter exactement 3 lettres majuscules, reçu : " + code);
        }
        return code;
    }

    /**
     * @return {@code true} si la station est accessible aux personnes à mobilité réduite
     */
    public boolean isAccessible() {
        return accessible;
    }

    /**
     * Définit l'accessibilité PMR de la station.
     *
     * @param accessible {@code true} pour une station accessible
     */
    public void setAccessible(boolean accessible) {
        this.accessible = accessible;
    }

    /**
     * @return la zone tarifaire de la station
     */
    public Zone getZone() {
        return zone;
    }

    /**
     * Rattache la station à une nouvelle zone tarifaire.
     *
     * @param zone la nouvelle zone, obligatoire
     * @throws IllegalArgumentException si la zone est {@code null}
     */
    public void setZone(Zone zone) {
        if (zone == null) {
            throw new IllegalArgumentException("Une station doit appartenir à une zone.");
        }
        this.zone = zone;
    }

    /**
     * Deux stations sont la MÊME station quand elles ont le même code
     * (contrat du module 40, inchangé).
     *
     * @param o l'objet à comparer
     * @return {@code true} si {@code o} est une station de même code
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true; // la même référence : succès rapide, cas fréquent
        }
        if (o == null || getClass() != o.getClass()) {
            return false; // null ou une autre classe ne peut pas être égale
        }
        Station autre = (Station) o;
        return code.equals(autre.code);
    }

    /**
     * Code de hachage cohérent avec {@link #equals(Object)} : dérivé du code.
     *
     * @return le code de hachage de la station
     */
    @Override
    public int hashCode() {
        return code.hashCode();
    }

    /**
     * Représentation lisible, ex. {@code "Station[GAR — Gare Centrale, Zone 1, ♿]"}.
     *
     * @return le libellé de la station
     */
    @Override
    public String toString() {
        StringBuilder libelle = new StringBuilder("Station[")
                .append(code).append(" \u2014 ").append(nom)
                .append(", Zone ").append(zone.getNumero());
        if (accessible) {
            libelle.append(", \u267F"); // ♿ : le symbole passe mieux en escape Unicode qu'en caractère brut
        }
        return libelle.append(']').toString();
    }
}
