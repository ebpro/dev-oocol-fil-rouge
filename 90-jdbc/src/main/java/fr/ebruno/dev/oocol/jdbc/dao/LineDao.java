package fr.ebruno.dev.oocol.jdbc.dao;

import java.util.List;

import fr.ebruno.dev.oocol.jdbc.domain.Line;

/**
 * Contrat d'accès aux lignes du réseau.
 *
 * <p>Une ligne est une entité <em>aggregate</em> : son identité vit dans la
 * table {@code lignes}, ses arrêts dans la table-jonction
 * {@code ligne_arrets}. Le DAO a la charge d'écrire et de relire les deux
 * tables comme si ce n'était qu'un seul objet — c'est la leçon « mapping
 * objet-relationnel à la main » de l'ELO 12.
 */
public interface LineDao extends CrudDao<Line, String> {

    /**
     * Retourne les lignes qui desservent un arrêt donné.
     *
     * @param codeStation le code de la station recherchée
     * @return les lignes contenant cet arrêt, dans l'ordre des noms
     */
    List<Line> trouverParStation(String codeStation);

    /**
     * Ajoute un arrêt à une ligne existante, à la position donnée.
     *
     * @param lineName    le nom de la ligne
     * @param stationCode le code de la station à insérer
     * @param position    le rang de l'arrêt dans le parcours
     * @throws RuntimeException enveloppant une {@code SQLException}
     */
    void ajouterArret(String lineName, String stationCode, int position);
}
