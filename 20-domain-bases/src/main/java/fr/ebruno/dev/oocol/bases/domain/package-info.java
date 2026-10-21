/**
 * Module 20 — Fondations du domaine réseau de transport « Trans'U ».
 *
 * <p>Ce paquet contient les quatre premières entités du domaine
 * ({@link fr.ebruno.dev.oocol.bases.domain.Zone},
 * {@link fr.ebruno.dev.oocol.bases.domain.Station},
 * {@link fr.ebruno.dev.oocol.bases.domain.Line},
 * {@link fr.ebruno.dev.oocol.bases.domain.Ticket}).
 *
 * <p>Ce paquet exerce : encapsulation, surcharge de constructeurs, membres
 * statiques, {@code final}, et documentation Javadoc (ELO 04).
 *
 * <p>Ordre de lecture conseillé : Zone (la plus simple, immuabilité) →
 * Station (encapsulation, surcharge, compteur statique) → Line (composition,
 * tension vers les collections) → Ticket (fabriques statiques).
 *
 * @author Emmanuel Bruno
 */
package fr.ebruno.dev.oocol.bases.domain;
