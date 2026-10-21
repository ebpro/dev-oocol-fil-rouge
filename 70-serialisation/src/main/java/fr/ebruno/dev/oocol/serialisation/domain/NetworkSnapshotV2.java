package fr.ebruno.dev.oocol.serialisation.domain;

import java.io.IOException;
import java.io.InputStream;
import java.io.InvalidClassException;
import java.io.ObjectInputStream;
import java.io.ObjectStreamClass;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * La V2 de l'instantané : le même graphe, un champ de plus
 * ({@code version}), et un {@code serialVersionUID} passé de 1 à 2.
 *
 * <p><strong>L'histoire que cette classe raconte.</strong> Six mois après la
 * V1, l'équipe ajoute un champ {@code version} et, « pour marquer le coup »,
 * change le UID à 2. Résultat : tous les fichiers V1 déjà écrits deviennent
 * illisibles — {@code readObject} lève une {@link InvalidClassException}
 * dont le message est un réquisitoire :
 * {@code "local class incompatible: stream classdesc serialVersionUID = 1,
 * local class serialVersionUID = 2"}. Changer le UID, c'est ROMPRE le
 * contrat de format ; ajouter un champ SANS changer le UID, c'est le faire
 * évoluer proprement (le nouveau champ prend sa valeur par défaut à la
 * relecture des anciens flux — démontré dans {@code VersioningTest}).
 *
 * <p><strong>La migration {@link #migrerDepuisV1} est volontairement
 * spectaculaire.</strong> Elle remappe le nom de classe du flux vers cette
 * V2 par {@code resolveClass} — ce qui simule « la V2 a remplacé la V1 sous
 * le même nom ». La JVM refuse QUAND MÊME, avec la vraie exception de
 * mismatch de UID : la vérification du UID est antérieure à celle du nom.
 * Prouvé par expérience en amont de ce cours — c'est la preuve que le UID,
 * pas le nom, est la signature du format.
 *
 * @author Emmanuel Bruno
 * @see NetworkSnapshot
 */
public class NetworkSnapshotV2 implements Serializable {

    /**
     * Version du format : 2. C'est PRÉCISÉMENT l'erreur que cette classe
     * incarne — passer le UID à 2 a décrété tous les fichiers V1 périmés.
     */
    private static final long serialVersionUID = 2L;

    /** Les lignes, inchangées depuis la V1. */
    private final List<Line> lignes;

    /** Les zones, inchangées depuis la V1. */
    private final List<Zone> zones;

    /** L'horodatage, inchangé depuis la V1. */
    private final Instant horodatage;

    /** La description, inchangée depuis la V1 (nullable). */
    private final String description;

    /** LE nouveau champ de la V2 : d'où la tentation (funeste) de passer le UID à 2. */
    private final String version;

    /**
     * Construit un instantané V2 complet.
     *
     * @param lignes      les lignes du réseau, non {@code null}
     * @param zones       les zones du réseau, non {@code null}
     * @param horodatage  moment de la capture, non {@code null}
     * @param description description humaine, nullable
     * @param version     version déclarative du contenu (ex. {@code "2.0"}), non vide
     * @throws IllegalArgumentException si un champ obligatoire manque
     */
    public NetworkSnapshotV2(List<Line> lignes, List<Zone> zones, Instant horodatage,
                             String description, String version) {
        if (lignes == null || zones == null || horodatage == null
                || version == null || version.isBlank()) {
            throw new IllegalArgumentException(
                    "Lignes, zones, horodatage et version sont obligatoires.");
        }
        this.lignes = new ArrayList<>(lignes);
        this.zones = new ArrayList<>(zones);
        this.horodatage = horodatage;
        this.description = description;
        this.version = version;
    }

    /**
     * @return les lignes capturées, en vue non modifiable
     */
    public List<Line> getLignes() {
        return Collections.unmodifiableList(lignes);
    }

    /**
     * @return les zones capturées, en vue non modifiable
     */
    public List<Zone> getZones() {
        return Collections.unmodifiableList(zones);
    }

    /**
     * @return le moment de la capture
     */
    public Instant getHorodatage() {
        return horodatage;
    }

    /**
     * @return la description humaine, ou {@code null}
     */
    public String getDescription() {
        return description;
    }

    /**
     * @return la version déclarative du contenu (nouveau champ V2)
     */
    public String getVersion() {
        return version;
    }

    /**
     * Tente de relire un fichier écrit au format V1 comme s'il était une V2.
     *
     * <p>Mécanisme : un {@code ObjectInputStream} anonyme dont
     * {@code resolveClass} remplace la classe demandée {@code NetworkSnapshot}
     * par {@code NetworkSnapshotV2} — on simule le déploiement où la V2 a
     * remplacé la V1 dans l'application, sous le même nom de fichier.
     *
     * <p>Résultat garanti : {@link InvalidClassException}, message
     * {@code "local class incompatible: stream classdesc serialVersionUID = 1,
     * local class serialVersionUID = 2"}. La remappe de nom ne sauve rien :
     * le UID se vérifie AVANT le nom. Cette méthode est le support du test
     * « le UID s'est retourné contre son auteur » — elle ne doit JAMAIS être
     * utilisée en production, où la bonne stratégie est : UID stable +
     * {@code readObject} tolérant, ou migration explicite par un lecteur V1
     * séparé.
     *
     * @param fichierV1 instantané écrit par {@link NetworkSnapshot#sauvegarder}
     * @return ne retourne jamais — lève toujours {@code InvalidClassException}
     * @throws IOException            si la lecture physique échoue
     * @throws ClassNotFoundException si le flux référence une classe absente du classpath
     * @throws InvalidClassException  toujours : mismatch de serialVersionUID V1 (1) vs V2 (2)
     */
    public static NetworkSnapshotV2 migrerDepuisV1(Path fichierV1)
            throws IOException, ClassNotFoundException {
        try (InputStream flot = Files.newInputStream(fichierV1);
             ObjectInputStream in = new ObjectInputStream(flot) {
                 @Override
                 protected Class<?> resolveClass(ObjectStreamClass desc)
                         throws IOException, ClassNotFoundException {
                     if (NetworkSnapshot.class.getName().equals(desc.getName())) {
                         // « La V2 a remplacé la V1 » : on lui donne quand même à lire.
                         return NetworkSnapshotV2.class;
                     }
                     return super.resolveClass(desc);
                 }
             }) {
            return (NetworkSnapshotV2) in.readObject();
        }
    }

    /**
     * Représentation lisible, alignée sur la V1 avec la version en plus.
     *
     * @return le résumé de l'instantané V2
     */
    @Override
    public String toString() {
        return "Instantané V" + version + " '" + description + "' : "
                + lignes.size() + " lignes, " + zones.size() + " zones, " + horodatage;
    }
}
