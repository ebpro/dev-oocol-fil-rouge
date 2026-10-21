package fr.ebruno.dev.oocol.jdbc.domain;

/**
 * Une zone tarifaire du réseau Trans'U.
 *
 * <p>Depuis le module 80, les données pures sont des {@code record} :
 * immuables, avec {@code equals}/{@code hashCode}/{@code toString} générés.
 * Côté base de données, ce record correspond à <em>une ligne</em> de la table
 * {@code zones} — le mapping ResultSet → objet est la responsabilité du DAO,
 * jamais du domaine.
 *
 * @param numero identifiant de la zone (clé primaire de la table {@code zones})
 * @param nom    libellé affiché aux voyageurs
 */
public record Zone(int numero, String nom) {
}
