/**
 * Domaine du module 95 — l'agrégat Trans'U sous sa forme définitive.
 *
 * <p>Quatre types de données ({@link fr.ebruno.dev.oocol.transit.domain.Zone},
 * {@link fr.ebruno.dev.oocol.transit.domain.Station},
 * {@link fr.ebruno.dev.oocol.transit.domain.Line} — records ; 
 * {@link fr.ebruno.dev.oocol.transit.domain.Network} — classe immuable à
 * constructeur privé) et deux artisans :
 * {@link fr.ebruno.dev.oocol.transit.domain.NetworkBuilder} (la seule voie
 * de construction) et {@link fr.ebruno.dev.oocol.transit.domain.NetworkException}
 * (la voie d'échec, checked, fidèle au choix du module 50).
 *
 * <p>Règle d'or héritée du module 90 et verrouillée ici : le domaine ne
 * connaît ni JDBC, ni CSV, ni console. Les paquets {@code persistence} et
 * {@code cli} dépendent de lui ; jamais l'inverse — c'est le DIP de la
 * rétrospective finale.
 */
package fr.ebruno.dev.oocol.transit.domain;
