package fr.ebruno.dev.oocol.jdbc.domain;

/**
 * Un arrêt du réseau Trans'U.
 *
 * <p>Ici, la zone n'est portée que par son {@code zoneNumero} : c'est la
 * traduction directe de la colonne {@code zone_numero} (clé étrangère vers
 * {@code zones.numero}). Un record de domaine n'a pas besoin de charger
 * l'objet référencé — c'est au DAO de faire la jointure quand le besoin
 * exprimé le demande.
 *
 * @param code        code d'exploitation, clé primaire de la table {@code stations} (ex. {@code "HUB"})
 * @param nom         nom commercial de l'arrêt
 * @param zoneNumero  numéro de la zone tarifaire (FK vers {@code zones.numero})
 * @param accessible accessibilité PMR
 */
public record Station(String code, String nom, int zoneNumero, boolean accessible) {
}
