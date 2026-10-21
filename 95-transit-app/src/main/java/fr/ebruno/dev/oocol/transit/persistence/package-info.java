/**
 * Couche de persistance du module 95 — deux stratégies, un seul domaine.
 *
 * <p>{@link fr.ebruno.dev.oocol.transit.persistence.NetworkLoader} (CSV,
 * concept du module 60) et
 * {@link fr.ebruno.dev.oocol.transit.persistence.NetworkJdbcStore} (SQL,
 * concept du module 90) ne se connaissent pas et n'ont pas besoin de se
 * connaître : tous deux dépendent de la même abstraction — les méthodes
 * publiques de {@link fr.ebruno.dev.oocol.transit.domain.Network} — et
 * tous deux construisent par le même {@link
 * fr.ebruno.dev.oocol.transit.domain.NetworkBuilder}. Interchanger les
 * formats ne touche ni le domaine ni la CLI : c'est le DIP, démontré.
 */
package fr.ebruno.dev.oocol.transit.persistence;
