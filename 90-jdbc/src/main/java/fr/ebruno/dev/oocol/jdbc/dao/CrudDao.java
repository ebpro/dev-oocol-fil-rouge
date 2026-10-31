package fr.ebruno.dev.oocol.jdbc.dao;

import java.util.List;
import java.util.Optional;

/**
 * Contrat générique d'un DAO : Create, Read, Update, Delete.
 *
 * <p>DAO = <em>Data Access Object</em>. Un DAO est le seul endroit du projet
 * qui a le droit de parler SQL. Ici, le <em>quoi</em> (insérer, retrouver,
 * modifier, supprimer des entités {@code T} identifiées par {@code ID}) est
 * séparé du <em>comment</em> (JDBC aujourd'hui, JPA demain) — c'est le
 * principe d'inversion de dépendance appliqué à la persistance.
 *
 * <p>Choix de conception : les méthodes ne déclarent pas
 * {@code SQLException}. Une {@code SQLException} est une exception
 * <em>vérifiée</em> propre à JDBC ; l'exposer ici contaminerait tous les
 * appelants avec du vocabulaire d'infrastructure. Les implémentations
 * l'enveloppent dans une {@code RuntimeException} (cf. ELO 07, chaînage de
 * causes) — la cause reste consultable dans le {@code getCause()}.
 *
 * @param <T>  type d'entité persistée (record de domaine)
 * @param <ID> type de l'identifiant (clé primaire)
 */
public interface CrudDao<T, ID> {

    /**
     * Insère une nouvelle entité.
     *
     * @param entite l'entité à persister
     * @throws RuntimeException enveloppant une {@code SQLException} (violation de contrainte, doublon…)
     */
    void inserer(T entite);

    /**
     * Recherche une entité par son identifiant.
     *
     * @param id la clé primaire
     * @return un {@link Optional} — pas de {@code null} deviné (cf. ELO 11)
     */
    Optional<T> trouverParId(ID id);

    /**
     * Retourne toutes les entités de la table.
     *
     * @return la liste complète, dans un ordre déterministe
     */
    List<T> trouverTous();

    /**
     * Met à jour une entité existante (identifiée par sa clé).
     *
     * @param entite la nouvelle valeur complète de l'entité
     * @throws RuntimeException enveloppant une {@code SQLException}
     */
    void mettreAJour(T entite);

    /**
     * Supprime l'entité correspondant à cet identifiant.
     *
     * @param id la clé primaire
     * @throws RuntimeException enveloppant une {@code SQLException}
     */
    void supprimer(ID id);
}
