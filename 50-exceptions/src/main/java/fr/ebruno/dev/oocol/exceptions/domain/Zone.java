package fr.ebruno.dev.oocol.exceptions.domain;

/**
 * Une zone tarifaire du réseau « Trans'U » (ex. zone 1 = centre-ville,
 * zone 2 = périphérie…).
 *
 * <p><strong>Instantané W6 — inchangé sur le fond.</strong> {@code Zone}
 * est recopiée du module 40 (principe « snapshot-per-week » : chaque module
 * est autonome, aucune dépendance inter-modules) parce que {@link Station}
 * en porte une référence et que {@link FareCalculator} en indexe les
 * tarifs. Rien n'est ajouté cette semaine : la zone n'est pas le sujet de
 * l'atelier exceptions — elle est là pour que le domaine reste entier.
 *
 * @author Emmanuel Bruno
 * @see Station
 * @see FareCalculator
 */
public class Zone implements Comparable<Zone> {

    /** Numéro de zone (1, 2, 3…). Immuable : écrit uniquement dans le constructeur. */
    private final int numero;

    /** Nom explicite de la zone (ex. {@code "Centre-ville"}). Immuable. */
    private final String nom;

    /**
     * Crée une zone tarifaire valide.
     *
     * <p>Validation dans le constructeur (constructeur gardien, repris du
     * module 20) : un objet {@code Zone} qui existe est un objet valide.
     * Une valeur interdite ici lève {@link IllegalArgumentException} —
     * W6 la relit comme le premier exemple du cours <em>unchecked par nature</em> :
     * un numéro {@code <= 0} est un bug de l'appelant, jamais une situation
     * à rattraper.
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
     * Ordre naturel des zones : par numéro croissant (repris du module 40).
     *
     * @param autre la zone comparée
     * @return un négatif si cette zone a un numéro plus petit, {@code 0} si
     *         même numéro, un positif sinon
     */
    @Override
    public int compareTo(Zone autre) {
        return this.numero - autre.numero;
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
     * Deux zones sont égales quand elles ont le même numéro (contrat W3 repris).
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
