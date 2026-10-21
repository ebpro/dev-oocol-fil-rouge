/**
 * Module 40 — Collections du réseau de transport « Trans'U ».
 *
 * <p>Ce paquet contient les classes du domaine enrichies pour la semaine 5 :
 * {@link fr.ebruno.dev.oocol.collections.domain.Zone} (ordre naturel),
 * {@link fr.ebruno.dev.oocol.collections.domain.Station} (Comparable,
 * equals/hashCode), {@link fr.ebruno.dev.oocol.collections.domain.Line}
 * ({@code List<Station>} à la place de la paire W3),
 * {@link fr.ebruno.dev.oocol.collections.domain.Network}
 * ({@code Map<Station, Set<Line>>}),
 * {@link fr.ebruno.dev.oocol.collections.domain.TripPlanner} (BFS avec
 * {@code Queue}) et {@link fr.ebruno.dev.oocol.collections.domain.FareCalculator}
 * (tri naturel + Strategy par {@code Comparator} anonyme).
 *
 * <p>Ce paquet exerce : les conteneurs {@code List}/{@code Set}/{@code Map}/
 * {@code Queue}, la généricité, {@code Comparable}/{@code Comparator}, le
 * contrat {@code equals}/{@code hashCode} (ELO 06).
 *
 * <p>Ordre de lecture conseillé : Zone (Comparable le plus simple) →
 * Station (equals/hashCode — LE bug motivateur) → Line (la paire W3 devient
 * une List) → Network (Map de hachage) → TripPlanner (Queue + BFS) →
 * FareCalculator (les deux façons de trier).
 *
 * <p>Rappel de style pédagogique : <strong>aucun lambda, aucun stream, aucun
 * {@code Optional}</strong> dans ce module — les {@code Comparator} sont des
 * classes internes anonymes, l'absence est représentée par {@code null}. Le
 * module {@code 80-modern-java} montrera la version moderne de ce même code,
 * pour que la différence se mesure plutôt qu'elle ne s'affirme.
 *
 * @author Emmanuel Bruno
 */
package fr.ebruno.dev.oocol.collections.domain;
