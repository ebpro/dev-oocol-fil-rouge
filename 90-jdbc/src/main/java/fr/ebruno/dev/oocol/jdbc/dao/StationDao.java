package fr.ebruno.dev.oocol.jdbc.dao;

import java.util.List;

import fr.ebruno.dev.oocol.jdbc.domain.Station;

/**
 * Contrat d'accès aux arrêts du réseau.
 *
 * <p>L'interface porte les requêtes <em>métier</em> (par zone, recherche par
 * nom) en plus du CRUD générique. Un appelant qui dépend de cette interface
 * ignore tout du support de stockage — c'est ce qui rendra possible le
 * remplacement de JDBC par JPA sans toucher aux services (promesse tenue par
 * la javadoc de {@link fr.ebruno.dev.oocol.jdbc.repo.NetworkRepository}).
 */
public interface StationDao extends CrudDao<Station, String> {

    /**
     * Retourne les arrêts d'une zone tarifaire donnée.
     *
     * @param zoneNumero le numéro de zone (FK {@code stations.zone_numero})
     * @return les stations de la zone, dans l'ordre des codes
     */
    List<Station> trouverParZone(int zoneNumero);

    /**
     * Recherche partielle par nom (sémantique SQL {@code LIKE}).
     *
     * <p>Le motif est une <em>valeur</em> transmise par {@code ?} — jamais
     * concaténée dans le SQL. C'est la différence entre cette méthode et le
     * code vulnérable de {@link fr.ebruno.dev.oocol.jdbc.security.SqlInjectionDemo}.
     *
     * @param motif sous-chaîne recherchée (le DAO ajoute les jokers {@code %…%})
     * @return les stations dont le nom contient {@code motif}
     */
    List<Station> chercherParNom(String motif);

    /**
     * Teste l'existence d'un code d'arrêt.
     *
     * @param code la clé primaire
     * @return {@code true} si une station porte ce code
     */
    boolean existe(String code);
}
