package fr.ebruno.dev.oocol.bases.domain;

/**
 * Une zone tarifaire du réseau « Trans'U » (ex. zone 1 = centre-ville,
 * zone 2 = périphérie…).
 *
 * <p><strong>Première classe du domaine — l'exemple de l'immuabilité par
 * {@code final}.</strong> Une zone ne change ni de numéro ni de nom une fois
 * créée : ses deux attributs sont déclarés {@code final}. Conséquence :
 *
 * <ul>
 *   <li>le constructeur est le <em>seul</em> endroit où les attributs sont
 *       écrits — toute la validation y tient en une ligne ;</li>
 *   <li>il n'existe <em>aucun</em> setter : impossible de corrompre un objet
 *       {@code Zone} après sa création ;</li>
 *   <li>un objet {@code Zone} peut être partagé partout sans risque (il est
 *       immuable, comme {@code String}).</li>
 * </ul>
 *
 * <p>La classe définit aussi {@link #equals(Object)} et {@link #hashCode()} :
 * deux zones sont identiques <em>si et seulement si</em> elles ont le même
 * numéro. Règle d'or : <strong>ces deux méthodes doivent toujours
 * correspondre</strong> — redéfinir {@code equals} sans {@code hashCode} casse
 * silencieusement les collections ({@code HashSet}, {@code HashMap}), qui
 * cherchent un objet dans « le mauvais tiroir ».
 *
 * @author Emmanuel Bruno
 * @see Station
 */
public class Zone {

    /** Numéro de zone (1, 2, 3…). Immuable : écrit uniquement ici, dans le constructeur. */
    private final int numero;

    /** Nom explicite de la zone (ex. {@code "Centre-ville"}). Immuable. */
    private final String nom;

    /**
     * Crée une zone tarifaire valide.
     *
     * <p>La validation vit <em>dans</em> le constructeur : un objet {@code Zone}
     * qui existe est un objet valide. C'est le principe du
     * <strong>constructeur gardien</strong> — mieux vaut refuser une donnée
     * invalide tout de suite que la propager pendant toute la durée de vie
     * de l'objet.
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
     * Représentation lisible : {@code "Zone 1 — Centre-ville"}.
     *
     * <p>Contrairement à {@code toString()} par défaut (qui affiche
     * {@code fr.ebruno.dev.oocol.bases.domain.Zone@1f}), celle-ci est écrite
     * pour les humains : affichages et journaux de debug.
     *
     * @return le libellé complet de la zone
     */
    @Override
    public String toString() {
        return "Zone " + numero + " — " + nom;
    }

    /**
     * Deux zones sont égales quand elles ont le même numéro.
     *
     * <p>Le <em>nom</em> n'entre pas dans la comparaison : une zone 1
     * renommée « Cœur de ville » reste la zone 1. C'est le numéro qui
     * identifie la zone dans le système tarifaire.
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
     * Code de hachage cohérent avec {@link #equals(Object)} : basé sur le
     * numéro uniquement.
     *
     * <p>La règle « {@code a.equals(b)} implique
     * {@code a.hashCode() == b.hashCode()} » doit être respectée, sinon les
     * objets « disparaissent » des tables de hachage.
     *
     * @return le code de hachage de la zone
     */
    @Override
    public int hashCode() {
        return Integer.hashCode(numero);
    }
}
