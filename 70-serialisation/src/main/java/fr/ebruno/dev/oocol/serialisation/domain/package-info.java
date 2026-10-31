/**
 * Domaine du module 70 — sérialisation Java (ELO 09).
 *
 * <p>Le réseau Trans'U version « octets » : des entités qui signent un
 * contrat de format binaire. {@link fr.ebruno.dev.oocol.serialisation.domain.Zone},
 * {@link fr.ebruno.dev.oocol.serialisation.domain.Station} et
 * {@link fr.ebruno.dev.oocol.serialisation.domain.Line} forment le graphe
 * sérialisé ; {@link fr.ebruno.dev.oocol.serialisation.domain.NetworkSnapshot}
 * est l'instantané V1 que l'on fige sur disque ;
 * {@link fr.ebruno.dev.oocol.serialisation.domain.NetworkSnapshotV2} raconte
 * la rupture de {@code serialVersionUID} ;
 * {@link fr.ebruno.dev.oocol.serialisation.domain.Ticket} joue la carte
 * {@code Externalizable} ; et le duo
 * {@link fr.ebruno.dev.oocol.serialisation.domain.Passenger} /
 * {@link fr.ebruno.dev.oocol.serialisation.domain.PassengerCorrige} met en
 * scène la rupture de l'invariant du singleton.
 *
 * <p>Règle du cours : la sérialisation est un <em>mécanisme d'infrastructure</em>
 * que le domaine subit, pas un outil de conception. Ici, on la découvre en
 * l'exposant — pas en la célébrant.
 */
package fr.ebruno.dev.oocol.serialisation.domain;
