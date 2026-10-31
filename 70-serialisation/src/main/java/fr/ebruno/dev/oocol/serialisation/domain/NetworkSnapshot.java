package fr.ebruno.dev.oocol.serialisation.domain;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * L'instantané complet du réseau Trans'U : le graphe d'objets que l'on fige en octets.
 *
 * <p><strong>CE QU'EST LA SÉRIALISATION JAVA.</strong> {@code Serializable}
 * n'est qu'une <em>classe marqueur</em> : aucun méthode à implémenter, la
 * JVM fait tout par réflexion. Elle écrit le descripteur de classe (nom +
 * {@code serialVersionUID} + liste des champs), puis la valeur des champs
 * non-{@code transient}, en parcourant le graphe d'objets — références
 * incluses. Un snapshot du réseau, c'est donc {@code Line} qui entraîne
 * {@code Station} qui entraîne {@code Zone} : tout l'arbre, une seule écriture.
 *
 * <p><strong>CE QUE CE N'EST PAS.</strong> Un format d'échange : les octets
 * produits ne parlent qu'à une JVM qui connaît CES classes-là, à CES
 * versions-là. C'est un format <em>privé</em>, couplé au code qui l'a écrit
 * — d'où le {@code serialVersionUID} et toute la tension du module (versionner
 * un format privé, c'est signer un chèque sans date). Pour échanger, on
 * utilise JSON, protobuf, etc. — hors sujet ici, et le README l'explique.
 *
 * <p><strong>{@code Instant} est sérialisable</strong> (java.time l'a prévu) :
 * l'horodatage voyage donc nativement, sans conversion « pratique » en long
 * qui perdrait la sémantique.
 *
 * @author Emmanuel Bruno
 * @see NetworkSnapshotV2
 * @see Line
 */
public class NetworkSnapshot implements Serializable {

    /**
     * Version du format de l'instantané. V1 = l'original ; la classe
     * {@link NetworkSnapshotV2} passe à 2 et paie cash l'addition.
     */
    private static final long serialVersionUID = 1L;

    /** Lignes du réseau (avec leurs arrêts et zones). Copie défensive à la construction. */
    private final List<Line> lignes;

    /** Zones tarifaires (feuilles du graphe). Copie défensive à la construction. */
    private final List<Zone> zones;

    /** Horodatage de la capture — sérialisable par nature. */
    private final Instant horodatage;

    /** Description humaine de l'instantané, facultative (peut être {@code null}). */
    private final String description;

    /**
     * Construit un instantané à partir de listes copiées (le snapshot ne se
     * laisse pas modifier après capture — la copie défensive vaut aussi pour les octets).
     *
     * @param lignes      les lignes du réseau, non {@code null}
     * @param zones       les zones du réseau, non {@code null}
     * @param horodatage  moment de la capture, non {@code null}
     * @param description description humaine, nullable
     * @throws IllegalArgumentException si une liste obligatoire manque
     */
    public NetworkSnapshot(List<Line> lignes, List<Zone> zones,
                           Instant horodatage, String description) {
        if (lignes == null || zones == null || horodatage == null) {
            throw new IllegalArgumentException(
                    "Lignes, zones et horodatage sont obligatoires.");
        }
        this.lignes = new ArrayList<>(lignes);
        this.zones = new ArrayList<>(zones);
        this.horodatage = horodatage;
        this.description = description;
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
     * Écrit l'instantané complet dans un fichier (format binaire Java).
     *
     * <p>Le couple {@code FileOutputStream} + {@code ObjectOutputStream} est
     * le B.A.-BA du module : le second enveloppe le premier (motif Decorator,
     * vu au module 60) et ajoute la couche « objets » à la couche « octets ».
     * Fermer le {@code ObjectOutputStream} ferme tout le monde — try-with-resources.
     *
     * @param fichier chemin cible (les parents doivent exister)
     * @throws IOException si le disque refuse
     */
    public void sauvegarder(Path fichier) throws IOException {
        try (OutputStream flot = Files.newOutputStream(fichier);
             ObjectOutputStream out = new ObjectOutputStream(flot)) {
            out.writeObject(this);
        }
    }

    /**
     * Relit un instantané écrit par {@link #sauvegarder}.
     *
     * <p><strong>Avertissement de sécurité — flux non fiables</strong> :
     * {@code readObject} reconstruit n'importe quelle classe du <em>classpath
     * du lecteur</em> dont le nom apparaît dans le flux. Un fichier
     * sérialisé n'est pas une donnée, c'est une <em>programme d'assemblage
     * d'objets</em> — ne jamais le lire depuis une source non fiable
     * (cf. README, section « cinq pièges »).
     *
     * @param fichier chemin source
     * @return l'instantané reconstruit
     * @throws IOException            si la lecture physique échoue
     * @throws ClassNotFoundException si une classe du graphe n'existe plus
     *                                (ou plus sous ce nom — cf. {@link NetworkSnapshotV2})
     */
    public static NetworkSnapshot charger(Path fichier)
            throws IOException, ClassNotFoundException {
        try (InputStream flot = Files.newInputStream(fichier);
             ObjectInputStream in = new ObjectInputStream(flot)) {
            return (NetworkSnapshot) in.readObject();
        }
    }

    /**
     * Représentation lisible : {@code "Instantané 'X' : 2 lignes, 2 zones, 2026-…"}.
     *
     * @return le résumé de l'instantané
     */
    @Override
    public String toString() {
        return "Instantané '" + description + "' : " + lignes.size() + " lignes, "
                + zones.size() + " zones, " + horodatage;
    }
}
