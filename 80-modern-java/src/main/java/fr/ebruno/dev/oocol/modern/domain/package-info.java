/**
 * Domaine du module 80 — Java moderne (ELO 11).
 *
 * <p>Le réseau Trans'U réécrit avec les outils de Java 21 : les entités
 * pouilleuses des semaines 2 à 8 deviennent des {@code record}s
 * ({@link fr.ebruno.dev.oocol.modern.domain.Zone},
 * {@link fr.ebruno.dev.oocol.modern.domain.Station},
 * {@link fr.ebruno.dev.oocol.modern.domain.Ligne}), les abstractions ouvertes
 * deviennent des hiérarchies {@code sealed}
 * ({@link fr.ebruno.dev.oocol.modern.domain.Vehicle},
 * {@link fr.ebruno.dev.oocol.modern.domain.Propulsion},
 * {@link fr.ebruno.dev.oocol.modern.domain.Tarif}) et les services de
 * collecte deviennent des pipelines Streams
 * ({@link fr.ebruno.dev.oocol.modern.domain.AnalyseReseau}).
 *
 * <p>Le dispositif pédagogique tient en un duo :
 * {@link fr.ebruno.dev.oocol.modern.domain.AnalyseReseau} (le code « après »,
 * 1-3 lignes par opération) contre
 * {@link fr.ebruno.dev.oocol.modern.domain.VersionLegacy} (le code « avant »,
 * style W4-W7, résultats strictement identiques) — le diff EST la leçon, et
 * les tests la prouvent.
 *
 * <p>Règle du cours : la modernité n'est pas une mode, c'est une contraction
 * d'invariants. Ce que le compilateur vérifie ici (exhaustivité des switchs,
 * immutabilité des composants, {@code Optional} dans les signatures) était
 * auparavant à la charge du soin de l'auteur — et de ses tests.
 */
package fr.ebruno.dev.oocol.modern.domain;
