package fr.ebruno.dev.oocol.nio.domain;

/**
 * Un arrêt du réseau « Trans'U », identifié par son code (ex. {@code GAR}).
 *
 * <p><strong>Instantané W7.</strong> {@code Station} revient du module 50
 * avec deux nouveautés dues aux fichiers : une fabrique d'analyse
 * {@link #parse(String)} au format CSV, et l'implémentation de {@link
 * Comparable} — quand un réseau est lu dans le désordre d'un fichier,
 * trier par code est le premier réflexe pour obtenir une sortie stable,
 * indépendante de l'ordre des lignes.
 *
 * <p><strong>Format CSV attendu.</strong> {@code code;nom;zoneNumero;accessible},
 * par exemple {@code GAR;Gare Centrale;1;true}. Le champ {@code accessible}
 * vaut {@code true}/{@code false} (accessibilité PMR) ; le champ
 * {@code zoneNumero} ne référence la zone que par son numéro — la résolution
 * de la référence (numéro → vrai objet {@link Zone} lu dans un AUTRE
 * fichier) est le travail du {@link NetworkLoader}, voir
 * {@link #parse(String, Zone)}.
 *
 * @author Emmanuel Bruno
 * @see NetworkLoader
 * @see Zone
 */
public class Station implements Comparable<Station> {

    /** Code d'identification (ex. {@code "GAR"}). Immuable, clé d'égalité. */
    private final String code;

    /** Nom commercial de l'arrêt (ex. {@code "Gare Centrale"}). Immuable. */
    private final String nom;

    /** Zone tarifaire de l'arrêt. Immuable, jamais {@code null}. */
    private final Zone zone;

    /** Accessibilité PMR. Immuable. */
    private final boolean accessible;

    /**
     * Crée un arrêt valide.
     *
     * @param code       code de l'arrêt, non vide
     * @param nom        nom de l'arrêt, non vide
     * @param zone       zone tarifaire, obligatoire — l'absence de zone est
     *                   un bug d'appelant ({@code null} refusé), jamais un état du monde
     * @param accessible {@code true} si l'arrêt est accessible PMR
     * @throws IllegalArgumentException si un argument est vide ou {@code null}
     */
    public Station(String code, String nom, Zone zone, boolean accessible) {
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("Le code d'arrêt est obligatoire.");
        }
        if (nom == null || nom.isBlank()) {
            throw new IllegalArgumentException("Le nom d'arrêt est obligatoire.");
        }
        if (zone == null) {
            throw new IllegalArgumentException("Un arrêt est nécessairement dans une zone.");
        }
        this.code = code.trim();
        this.nom = nom.trim();
        this.zone = zone;
        this.accessible = accessible;
    }

    /**
     * Fabrique un arrêt depuis une ligne CSV, SANS résolution de zone.
     *
     * <p>La zone n'est pas oubliée : elle est présente dans le CSV par son
     * numéro, mais le numéro ne suffit pas à reconstituer le NOM de la zone
     * (il vit dans un autre fichier, {@code zones.csv}). Cette fabrique
     * construit donc un {@link Zone} « tige » ({@code nom = "zone-N"}) qui
     * porte le bon numéro — utile pour valider le format d'une ligne de
     * façon autonome. Le {@link NetworkLoader}, lui, utilise la version
     * {@link #parse(String, Zone)} qui lie la VRAIE zone.
     *
     * @param csv une ligne complète au format {@code code;nom;zoneNumero;accessible}
     * @return l'arrêt décrit (zone réduite à son numéro)
     * @throws NetworkException si la ligne est malformée — le message cite la ligne brute
     */
    public static Station parse(String csv) throws NetworkException {
        int numero = extraireNumeroZone(csv);
        Zone tige;
        try {
            tige = new Zone(numero, "zone-" + numero);
        } catch (IllegalArgumentException e) {
            // Donnée fautive (zone <= 0 dans le fichier) : canal checked, pas IAE brut.
            throw new NetworkException("Numéro de zone invalide dans \u00AB " + csv + " \u00BB", e);
        }
        return parse(csv, tige);
    }

    /**
     * Fabrique un arrêt depuis une ligne CSV en liant la zone résolue.
     *
     * <p><strong>Intégrité référentielle.</strong> Le numéro lu dans la
     * ligne DOIT coïncider avec celui de la zone fournie : c'est la
     * dernière ligne de défense contre un {@code zones.csv} et un
     * {@code stations.csv} qui n'auraient pas été exportés du même
     * référentiel. Une divergence est une anomalie récupérable (on
     * ré-exporte les fichiers), donc une {@link NetworkException} — pas un
     * {@code AssertionError} de compilation.
     *
     * @param csv  une ligne complète au format {@code code;nom;zoneNumero;accessible}
     * @param zone la zone réellement lue dans {@code zones.csv}, de même numéro
     *             que celui porté par la ligne
     * @return l'arrêt décrit, lié à la vraie zone
     * @throws NetworkException si la ligne est malformée ou si le numéro de
     *                          zone contredit la zone fournie
     */
    public static Station parse(String csv, Zone zone) throws NetworkException {
        String[] champs = csv.split(";", -1);
        if (champs.length < 4) {
            throw new NetworkException(
                    "Ligne d'arrêt invalide (attendu « code;nom;zone;accessible ») : \u00AB " + csv + " \u00BB");
        }
        int numeroDeclare = extraireNumeroZone(csv);
        if (numeroDeclare != zone.getNumero()) {
            throw new NetworkException("Incohérence de zone dans \u00AB " + csv
                    + " \u00BB : la ligne référence la zone " + numeroDeclare
                    + ", la zone chargée porte le numéro " + zone.getNumero());
        }
        boolean accessible;
        if ("true".equalsIgnoreCase(champs[3].trim())) {
            accessible = true;
        } else if ("false".equalsIgnoreCase(champs[3].trim())) {
            accessible = false;
        } else {
            throw new NetworkException("Le champ accessible doit valoir true/false, lu : \u00AB "
                    + champs[3].trim() + " \u00BB dans \u00AB " + csv + " \u00BB");
        }
        try {
            return new Station(champs[0], champs[1], zone, accessible);
        } catch (IllegalArgumentException e) {
            throw new NetworkException("Arrêt refusé par le contrat métier : \u00AB " + csv + " \u00BB", e);
        }
    }

    /**
     * Extrait (et valide) le numéro de zone d'une ligne CSV d'arrêt.
     *
     * @param csv la ligne brute
     * @return le numéro de zone déclaré par la ligne
     * @throws NetworkException si le champ zone n'est pas un entier
     */
    private static int extraireNumeroZone(String csv) throws NetworkException {
        String[] champs = csv.split(";", -1);
        if (champs.length < 3) {
            throw new NetworkException(
                    "Ligne d'arrêt invalide (attendu « code;nom;zone;accessible ») : \u00AB " + csv + " \u00BB");
        }
        try {
            return Integer.parseInt(champs[2].trim());
        } catch (NumberFormatException e) {
            throw new NetworkException("Numéro de zone non entier dans \u00AB " + csv + " \u00BB", e);
        }
    }

    /**
     * @return le code d'identification de l'arrêt
     */
    public String getCode() {
        return code;
    }

    /**
     * @return le nom commercial de l'arrêt
     */
    public String getNom() {
        return nom;
    }

    /**
     * @return la zone tarifaire de l'arrêt, jamais {@code null}
     */
    public Zone getZone() {
        return zone;
    }

    /**
     * @return {@code true} si l'arrêt est accessible aux personnes à mobilité réduite
     */
    public boolean isAccessible() {
        return accessible;
    }

    /**
     * Ordre naturel : par code croissant — une lecture de fichier arrive
     * dans l'ordre du hasard, trier par code rend toute sortie reproductible.
     *
     * @param autre l'arrêt comparé
     * @return le résultat de {@code this.code.compareTo(autre.code)}
     */
    @Override
    public int compareTo(Station autre) {
        return this.code.compareTo(autre.code);
    }

    /**
     * Représentation lisible : {@code "GAR Gare Centrale (zone 1, accessible)"}.
     *
     * @return le libellé de l'arrêt
     */
    @Override
    public String toString() {
        return code + " " + nom + " (zone " + zone.getNumero()
                + (accessible ? ", accessible" : ", non accessible") + ")";
    }

    /**
     * Deux arrêts sont égaux quand ils ont le même code — le code est
     * l'identité métier, le nom peut changer sans changer l'arrêt.
     *
     * @param o l'objet à comparer
     * @return {@code true} si {@code o} est un arrêt de même code
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Station autre = (Station) o;
        return code.equals(autre.code);
    }

    /**
     * Code de hachage cohérent avec {@link #equals(Object)} : basé sur le code.
     *
     * @return le code de hachage de l'arrêt
     */
    @Override
    public int hashCode() {
        return code.hashCode();
    }
}
