/**
 * La démonstration exécutable du module 70 : six scènes qui jouent le
 * graphe d'objets devenu octets — et tout ce qui peut mal tourner.
 *
 * <p><strong>Ce que ce package enseigne.</strong> {@code VersionnementDemo}
 * est le script de séance : chaque scène est une méthode privée autonome,
 * titrée, commentable séparément en TD. L'aller-retour disque (scène 1), la
 * rupture de {@code serialVersionUID} (scène 2), {@code Externalizable} en
 * mode manuel et compact (scène 3), le {@code transient} qui s'évapore
 * (scène 4), le singleton dédoublé puis réparé (scène 5), et l'inventaire
 * des cinq pièges canoniques (scène 6). Tout se déroule dans un répertoire
 * temporaire créé par {@code Files.createTempDirectory} et détruit dans le
 * {@code finally} du {@code main} — la démo ne touche ni le dépôt ni le home
 * de l'étudiant, elle tourne donc aussi bien en local que dans la CI.
 *
 * <p><strong>Lecture croisée.</strong> Chaque scène correspond à une classe
 * du domaine ({@link fr.ebruno.dev.oocol.serialisation.domain.NetworkSnapshot},
 * {@link fr.ebruno.dev.oocol.serialisation.domain.NetworkSnapshotV2},
 * {@link fr.ebruno.dev.oocol.serialisation.domain.Ticket},
 * {@link fr.ebruno.dev.oocol.serialisation.domain.Station},
 * {@link fr.ebruno.dev.oocol.serialisation.domain.Passenger} /
 * {@link fr.ebruno.dev.oocol.serialisation.domain.PassengerCorrige}) et à un
 * test qui en fait une preuve.
 *
 * @author Emmanuel Bruno
 * @see fr.ebruno.dev.oocol.serialisation.domain
 */
package fr.ebruno.dev.oocol.serialisation.app;
