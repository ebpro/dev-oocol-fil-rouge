/**
 * Domaine du module 90 — JDBC et motif DAO (ELO 12).
 *
 * <p>Trois records ({@link fr.ebruno.dev.oocol.jdbc.domain.Zone},
 * {@link fr.ebruno.dev.oocol.jdbc.domain.Station},
 * {@link fr.ebruno.dev.oocol.jdbc.domain.Line}) : les mêmes entités qu'au
 * module 80, vues cette fois sous l'angle de la persistance relationnelle —
 * chaque record correspond à une (ou plusieurs) lignes de tables.
 *
 * <p>Règle d'or du module : le domaine <strong>ne connaît pas le
 * SQL</strong>. Il ne sait pas qu'une base existe. Toute la plomberie JDBC
 * vit dans le paquet {@code dao}, sous contrat du paquet {@code dao}
 * (interfaces) — c'est ça, le motif DAO.
 */
package fr.ebruno.dev.oocol.jdbc.domain;
