/**
 * Contrats DAO du module 90 (ELO 12).
 *
 * <p>Ce paquet ne contient <strong>que des interfaces</strong> :
 * {@link fr.ebruno.dev.oocol.jdbc.dao.CrudDao} (le générique CRUD),
 * {@link fr.ebruno.dev.oocol.jdbc.dao.StationDao} et
 * {@link fr.ebruno.dev.oocol.jdbc.dao.LineDao} (les requêtes métier).
 * Aucune ligne de SQL ici — le SQL est un détail d'implémentation qui vit
 * dans {@code dao.impl}.
 *
 * <p>Le jour où l'on remplace JDBC par JPA, ce paquet ne bouge pas : seules
 * les implémentations sont remplacées. C'est la définition même de la
 * frontière de persistance.
 */
package fr.ebruno.dev.oocol.jdbc.dao;
