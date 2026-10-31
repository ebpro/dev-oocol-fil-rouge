package fr.ebruno.dev.oocol.serialisation.domain;

import java.io.Serializable;

/**
 * Une zone tarifaire du réseau « Trans'U », cette fois <em>sérialisable</em>.
 *
 * <p><strong>Version sérialisable.</strong> {@code Zone} revient du module 60 avec
 * un seul ajout en apparence anodin : {@code implements Serializable} et sa
 * constante {@code serialVersionUID}. Apparence anodine, car ce couple est
 * un <em>contrat binaire</em> : dès qu'un objet traverse un flux d'octets
 * (fichier, socket, cache), sa classe s'engage sur un format. Le numéro de
 * version de ce format, c'est {@code serialVersionUID} — et tout le module
 * entier explore ce qui se passe quand on rompt l'engagement.
 *
 * <p><strong>Pourquoi une constante écrite à la main ?</strong> Sans
 * déclaration explicite, la JVM <em>calcule</em> le UID à partir de la
 * structure de la classe (nom, champs, méthodes). Ajouter un champ change
 * alors le UID calculé — et tous les objets déjà écrits deviennent
 * illisibles. Écrire {@code = 1L} à la main, c'est choisir que la classe
 * reste « version 1 » quelle que soit l'évolution de son squelette, et
 * garder la maîtrise de la compatibilité (cf. {@link NetworkSnapshotV2}
 * pour l'histoire inverse).
 *
 * <p>Les deux champs sont {@code String} et {@code int} : des types déjà
 * sérialisables par la JVM. Un objet n'est sérialisable que si <em>tout</em>
 * son graphe l'est — {@code Zone} est la feuille de l'arbre, les autres
 * classes du module héritent d'elle cette sécurité.
 *
 * @author Emmanuel Bruno
 * @see NetworkSnapshot
 * @see Station
 */
public class Zone implements Serializable {

    /**
     * Numéro de version du format sérialisé. Déclaré explicitement (jamais
     * laissé au calcul automatique) : c'est LA décision d'ELO 09.
     */
    private static final long serialVersionUID = 1L;

    /** Numéro de zone (1, 2, 3…). Immuable. */
    private final int numero;

    /** Nom explicite de la zone (ex. {@code "Centre-ville"}). Immuable. */
    private final String nom;

    /**
     * Crée une zone tarifaire valide (constructeur gardien, repris des modules 20 et 60).
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
     * @return le libellé complet de la zone
     */
    @Override
    public String toString() {
        return "Zone " + numero + " \u2014 " + nom;
    }

    /**
     * Deux zones sont égales quand elles ont le même numéro (contrat du module 20 repris).
     *
     * @param o l'objet à comparer
     * @return {@code true} si {@code o} est une zone de même numéro
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
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
