package fr.ebruno.dev.oocol.nio.domain;

/**
 * Une zone tarifaire du réseau « Trans'U » (ex. zone 1 = centre-ville).
 *
 * <p><strong>La classe vue depuis les CSV.</strong> {@code Zone} revient du module 50 en
 * version minimale, augmentée d'une seule chose : une fabrique d'analyse
 * {@link #parse(String)} capable de faire naître une zone depuis UNE ligne
 * de CSV. C'est le geste nouveau du module — l'objet ne vient plus d'un
 * {@code new} écrit dans le code, il vient d'un fichier que quelqu'un d'autre
 * a saisi, avec les fautes que cela implique.
 *
 * <p><strong>Format CSV attendu.</strong> {@code numero;nom}, par exemple
 * {@code 1;Centre-ville}. Un séparateur {@code ;} et non {@code ,} : les
 * noms français contiennent des virgules, et l'export tableur européen
 * sépare par point-virgule.
 *
 * @author Emmanuel Bruno
 * @see NetworkLoader
 * @see Station
 */
public class Zone {

    /** Numéro de zone (1, 2, 3…). Immuable : écrit uniquement dans le constructeur. */
    private final int numero;

    /** Nom explicite de la zone (ex. {@code "Centre-ville"}). Immuable. */
    private final String nom;

    /**
     * Crée une zone tarifaire valide.
     *
     * <p>Validation dans le constructeur (constructeur gardien, repris du
     * module 20) : un objet {@code Zone} qui existe est un objet valide.
     * Un numéro {@code <= 0} ou un nom vide est un bug de l'appelant direct
     * — {@link IllegalArgumentException}. Quand la donnée vient d'un
     * fichier, c'est {@link #parse(String)} qui joue le rôle de garde, et
     * lui lève la checked {@link NetworkException} : une ligne fautive est
     * un état du monde rattrapable (on corrige le CSV), pas un bug de code.
     *
     * @param numero numéro de zone, strictement positif
     * @param nom    nom de la zone, non vide
     * @throws IllegalArgumentException si le numéro est négatif ou nul, ou si le nom est vide
     */
    public Zone(int numero, String nom) {
        if (numero <= 0) {
            throw new IllegalArgumentException(
                    "Le numéro de zone doit être strictement positif, reçu : " + numero);
        }
        if (nom == null || nom.isBlank()) {
            throw new IllegalArgumentException("Le nom de zone est obligatoire.");
        }
        this.numero = numero;
        this.nom = nom;
    }

    /**
     * Fabrique une zone depuis une ligne CSV au format {@code numero;nom}.
     *
     * <p>Trois niveaux de validation, dans l'ordre : le nombre de champs
     * (sinon la ligne n'est pas du format promis), la convertibilité du
     * numéro ({@link Integer#parseInt(String)} lève un {@code
     * NumberFormatException} — un checked métier l'enrobe, chaînage des causes
     * oblige), puis la validité métier via le constructeur. Le message
     * d'erreur cite TOUJOURS la ligne brute : diagnostiquer un fichier
     * sans savoir de quelle ligne il s'agit, c'est deviner.
     *
     * @param csv une ligne complète, sans le séparateur de fin de ligne
     * @return la zone décrite par la ligne
     * @throws NetworkException si la ligne est malformée (champs manquants,
     *                          numéro non entier) — le message inclut la ligne en cause
     */
    public static Zone parse(String csv) throws NetworkException {
        String[] champs = csv.split(";", -1);
        if (champs.length < 2) {
            throw new NetworkException(
                    "Ligne de zone invalide (attendu « numero;nom ») : \u00AB " + csv + " \u00BB");
        }
        int numero;
        try {
            numero = Integer.parseInt(champs[0].trim());
        } catch (NumberFormatException e) {
            // Enrobage : l'erreur de bas niveau devient la CAUSE, pas un déchet.
            throw new NetworkException(
                    "Numéro de zone non entier dans \u00AB " + csv + " \u00BB", e);
        }
        try {
            return new Zone(numero, champs[1].trim());
        } catch (IllegalArgumentException e) {
            throw new NetworkException("Zone refusée par le contrat métier : \u00AB " + csv + " \u00BB", e);
        }
    }

    /**
     * @return le numéro de la zone (1, 2, 3…)
     */
    public int getNumero() {
        return numero;
    }

    /**
     * @return le nom explicite de la zone
     */
    public String getNom() {
        return nom;
    }

    /**
     * Représentation lisible : {@code "Zone 1 — Centre-ville"}.
     *
     * @return le libellé complet de la zone
     */
    @Override
    public String toString() {
        return "Zone " + numero + " \u2014 " + nom;
    }

    /**
     * Deux zones sont égales quand elles ont le même numéro (contrat hérité, inchangé).
     *
     * @param o l'objet à comparer
     * @return {@code true} si {@code o} est une zone de même numéro
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true; // la même référence : succès rapide, cas fréquent
        }
        if (o == null || getClass() != o.getClass()) {
            return false; // null ou une autre classe ne peut pas être égale
        }
        Zone autre = (Zone) o;
        return numero == autre.numero;
    }

    /**
     * Code de hachage cohérent avec {@link #equals(Object)} : basé sur le numéro.
     *
     * @return le code de hachage de la zone
     */
    @Override
    public int hashCode() {
        return Integer.hashCode(numero);
    }
}
