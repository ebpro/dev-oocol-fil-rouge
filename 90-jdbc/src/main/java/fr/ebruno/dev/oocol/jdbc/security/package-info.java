/**
 * Sécurité SQL du module 90 (ELO 12).
 *
 * <p>{@link fr.ebruno.dev.oocol.jdbc.security.SqlInjectionDemo} place côte à
 * côte les deux écritures d'une même requête : concaténation (vulnérable,
 * démontrée exploitée) et {@code PreparedStatement} (saine, attaquée sans
 * effet). Les tests du module <em>prouvent</em> l'exploitation de l'une et
 * l'échec de l'autre — la sécurité n'est pas une consigne ici, c'est une
 * observable.
 */
package fr.ebruno.dev.oocol.jdbc.security;
