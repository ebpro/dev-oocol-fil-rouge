/**
 * Implémentations JDBC des contrats DAO (ELO 12).
 *
 * <p>{@link fr.ebruno.dev.oocol.jdbc.dao.impl.StationDaoJdbc} et
 * {@link fr.ebruno.dev.oocol.jdbc.dao.impl.LineDaoJdbc} : tout le SQL du
 * module est ici — requêtes préparées à {@code ?}, liaisons de paramètres,
 * mapping des {@code ResultSet}, enveloppement des {@code SQLException}.
 *
 * <p>Ce paquet est le seul autorisé à importer {@code java.sql}. Le domaine
 * l'ignore, les contrats l'ignorent, la façade le cache : renverser la
 * dépendance était le but du motif DAO.
 */
package fr.ebruno.dev.oocol.jdbc.dao.impl;
