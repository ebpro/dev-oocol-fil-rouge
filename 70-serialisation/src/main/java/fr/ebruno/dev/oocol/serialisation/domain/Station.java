package fr.ebruno.dev.oocol.serialisation.domain;

import java.io.IOException;
import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

/**
 * Un arrêt du réseau, sérialisable, avec un <em>cache de présentation</em>
 * transitoire et un protocole de lecture écrit à la main.
 *
 * <p><strong>Le mot-clé {@code transient}.</strong> {@code transient} =
 * « ne pas sérialiser ce champ » — ici un cache de présentation. Après
 * désérialisation, il est {@code null} et sera recalculé au premier appel
 * de {@link #getAffichage()}. Le principe : ne voyagent que les données
 * <em>irremplaçables</em> ; tout ce qu'une méthode peut reconstituer n'a
 * rien à faire dans le flux (gain de taille, et surtout pas de cache
 * périmé qui survit à une évolution du format).
 *
 * <p><strong>{@code writeObject} / {@code readObject} : laHook privé.</strong>
 * Ces deux méthodes <em>privées</em>, de signature imposée, sont appelées
 * par le mécanisme de sérialisation par réflexion : la classe garde la main
 * sur son format sans cesser d'être {@code Serializable}. Le motif complet,
 * celui de la documentation officielle : {@code defaultWriteObject} (laisser
 * la JVM écrire les champs non-transient), puis {@code defaultReadObject}
 * suivi d'une <em>revalidation</em> des invariants. Car un flux n'est pas
 * une source de confiance : personne n'a signé ces octets, un fichier peut
 * avoir été édité, tronqué, fabriqué à la main. {@link #readObject} vérifie
 * donc ce que le constructeur a déjà vérifié à la création — et refuse par
 * {@link InvalidObjectException} ce qui ne respecte pas le contrat.
 *
 * <p><strong>Les compteurs statiques</strong> ({@code compteurEcritures},
 * {@code compteurLectures}) servent aux tests : ils prouvent que le hook
 * est bien appelé par le mécanisme, jamais à la main. Ironie pédagogique :
 * étant {@code static}, ils ne voyagent pas dans le flux (cf.
 * {@code VersioningTest}).
 *
 * @author Emmanuel Bruno
 * @see Zone
 * @see Line
 */
public class Station implements Serializable {

    /** Version du format : 1, et l'intention est de la garder longtemps. */
    private static final long serialVersionUID = 1L;

    /** Combien de fois le hook d'écriture a été appelé (champ STATIQUE : jamais sérialisé). */
    static int compteurEcritures = 0;

    /** Combien de fois le hook de lecture a été appelé (champ STATIQUE : jamais sérialisé). */
    static int compteurLectures = 0;

    /** Code court de l'arrêt (ex. {@code "GAR"}). Immuable. */
    private final String code;

    /** Nom explicite de l'arrêt (ex. {@code "Gare Centrale"}). Immuable. */
    private final String nom;

    /** Zone tarifaire de l'arrêt. Immuable — et sérialisable (cf. {@link Zone}). */
    private final Zone zone;

    /**
     * Cache de présentation (« GAR — Gare Centrale »), calculé à la demande.
     * {@code transient} : il ne voyage PAS — il se recalcule tout seul.
     */
    private transient String cacheAffichage;

    /**
     * Crée un arrêt valide.
     *
     * @param code code court, non vide
     * @param nom  nom explicite, non vide
     * @param zone zone tarifaire, obligatoire
     * @throws IllegalArgumentException si un argument manque
     */
    public Station(String code, String nom, Zone zone) {
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("Le code d'arrêt est obligatoire.");
        }
        if (nom == null || nom.isBlank()) {
            throw new IllegalArgumentException("Le nom d'arrêt est obligatoire.");
        }
        if (zone == null) {
            throw new IllegalArgumentException("Un arrêt appartient à une zone.");
        }
        this.code = code;
        this.nom = nom;
        this.zone = zone;
    }

    /**
     * Libellé de présentation, calculé paresseusement et mis en cache.
     *
     * <p>Après une désérialisation, {@code cacheAffichage} vaut {@code null}
     * (champ {@code transient}) : le premier appel le recalcule. C'est tout
     * l'intérêt du pattern : le flux ne charrie que l'irremplaçable.
     *
     * @return « CODE — Nom » (ex. {@code "GAR — Gare Centrale"})
     */
    public String getAffichage() {
        if (cacheAffichage == null) {
            cacheAffichage = code + " \u2014 " + nom;
        }
        return cacheAffichage;
    }

    /**
     * @return le code court de l'arrêt
     */
    public String getCode() {
        return code;
    }

    /**
     * @return le nom explicite de l'arrêt
     */
    public String getNom() {
        return nom;
    }

    /**
     * @return la zone tarifaire de l'arrêt
     */
    public Zone getZone() {
        return zone;
    }

    /**
     * Représentation lisible : le code de l'arrêt, sobre par design.
     *
     * @return le code de l'arrêt entre crochets, avec son nom
     */
    @Override
    public String toString() {
        return "Station[" + code + " " + nom + ", " + zone + "]";
    }

    /**
     * Hook d'écriture appelé par le mécanisme de sérialisation (jamais à la main).
     *
     * <p>Le corps type : journaliser, puis {@code defaultWriteObject} —
     * c'est la JVM qui écrit les champs non-transient, dans l'ordre
     * binulaire canonique. On n'aurait aucune raison de réécrire cela à la
     * main : c'est exactement le rôle de {@code Serializable} que de le
     * faire pour nous.
     *
     * @param out le flux de sortie
     * @throws IOException si le disque ou le réseau refuse
     */
    private void writeObject(ObjectOutputStream out) throws IOException {
        compteurEcritures++;
        System.out.println("  [Station] writing Station " + code);
        out.defaultWriteObject();
    }

    /**
     * Hook de lecture, miroir de {@link #writeObject}, plus la revalidation.
     *
     * <p>Le constructeur n'est JAMAIS appelé par la désérialisation standard :
     * c'est {@code defaultReadObject} qui remplit les champs, par réflexion,
     * sans passer par nos validations. D'où la revalidation explicite :
     * le invariant « un arrêt a un code et un nom » doit tenir APRÈS lecture,
     * même si le flux vient d'un fichier édité à la main. Refus poli :
     * {@link InvalidObjectException} (sous-classe de {@code ObjectInputException},
     * elle-même {@code IOException}).
     *
     * @param in le flux d'entrée
     * @throws IOException            si la lecture physique échoue
     * @throws ClassNotFoundException si une classe du graphe n'existe plus
     * @throws InvalidObjectException si l'objet relu viole les invariants
     */
    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        compteurLectures++;
        in.defaultReadObject();
        // Revalidation : le constructeur n'est pas passé par ici, la confiance non plus.
        if (nom == null || code == null) {
            throw new InvalidObjectException(
                    "Station sans code ni nom : flux corrompu ou fabriqué à la main");
        }
    }
}
