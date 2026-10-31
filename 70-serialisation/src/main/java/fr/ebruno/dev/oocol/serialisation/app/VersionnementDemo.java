package fr.ebruno.dev.oocol.serialisation.app;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InvalidClassException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import fr.ebruno.dev.oocol.serialisation.domain.Line;
import fr.ebruno.dev.oocol.serialisation.domain.NetworkSnapshot;
import fr.ebruno.dev.oocol.serialisation.domain.NetworkSnapshotV2;
import fr.ebruno.dev.oocol.serialisation.domain.Passenger;
import fr.ebruno.dev.oocol.serialisation.domain.PassengerCorrige;
import fr.ebruno.dev.oocol.serialisation.domain.Station;
import fr.ebruno.dev.oocol.serialisation.domain.Ticket;
import fr.ebruno.dev.oocol.serialisation.domain.Zone;

/**
 * Les six scènes de la sérialisation, jouées en répertoire temporaire.
 *
 * <p><strong>Lecture.</strong> Chaque scène est une méthode privée autonome
 * que l'on peut commenter séparément ; {@link #main(String[])} les
 * enchaîne, titre, et garantit le grand ménage : le répertoire temporaire
 * créé en ouverture est supprimé récursivement dans le {@code finally},
 * quoi qu'il arrive — un principe du module vaut aussi pour ses propres
 * fichiers : ce qui est ouvert se referme, ce qui est créé se détruit.
 *
 * <p><strong>Zéro pollution.</strong> La démo n'écrit que sous {@code
 * Files.createTempDirectory} : ni le dépôt, ni le poste de développement ne
 * sont touchés, elle court donc partout, CI comprise.
 *
 * <p><strong>Scènes.</strong> 1 : figer et relire le réseau (le contrat
 * {@code Serializable} tenu) ; 2 : le {@code serialVersionUID} qui se
 * retourne contre son auteur (V1 relu par V2 → {@code InvalidClassException}) ;
 * 3 : {@code Externalizable} — le même ticket, écrit à la main, bien plus
 * compact ; 4 : le {@code transient} qui s'évapore puis se recalcule ;
 * 5 : le singleton dédoublé, puis réparé par {@code readResolve} ;
 * 6 : l'inventaire des cinq points de vigilance canoniques, un par ligne.
 *
 * @author Emmanuel Bruno
 * @see fr.ebruno.dev.oocol.serialisation.domain
 */
public final class VersionnementDemo {

    /** Utilitaire statique : pas d'instance. */
    private VersionnementDemo() {
    }

    /**
     * Enchaîne les six scènes dans un répertoire temporaire, puis le détruit.
     *
     * <p>Le {@code throws Exception} est un choix assumé au niveau du point
     * d'entrée : une checked qui remonte jusqu'ici n'est pas un état du
     * monde à rattraper (cf. modules 50-60) — ici, RIEN n'est censé
     * échouer, et tout ce qui échouerait mérite arrêt).
     *
     * @param args non utilisé
     * @throws Exception toute panne de disque, de flux ou de réflexion —
     *                   propagée sans rien avaler
     */
    public static void main(String[] args) throws Exception {
        Path racine = Files.createTempDirectory("oocol-serie-demo");
        try {
            Path fichierV1 = scene1FigerLeReseau(racine);
            scene2RuptureDeUid(fichierV1);
            scene3Externalizable();
            scene4Transient();
            scene5Singleton();
            scene6CinqPieges();
            System.out.println();
            System.out.println("=== Fin de la d\u00E9mo : s\u00E9rialiser, c'est signer un contrat de format. ===");
        } finally {
            detruireRecursivement(racine);
            System.out.println("(r\u00E9pertoire temporaire supprim\u00E9 : " + racine + ")");
        }
    }

    /** Titre de scène, même gabarit partout. */
    private static void titre(int numero, String nom) {
        System.out.println();
        System.out.println("=== Sc\u00E8ne " + numero + " \u2014 " + nom + " ===");
    }

    /**
     * Scène 1 — FIGER ET RELIRE : le graphe complet (lignes → arrêts →
     * zones, {@code Instant} compris) part en octets et revient intact.
     * C'est la promesse {@code Serializable} : une écriture, tout l'arbre.
     *
     * @param racine répertoire temporaire de la démo
     * @return le fichier d'instantané V1 — la scène 2 s'en servira comme pièce à conviction
     * @throws IOException            si le disque refuse
     * @throws ClassNotFoundException si une classe du graphe n'existe plus (ici : non)
     */
    private static Path scene1FigerLeReseau(Path racine) throws IOException, ClassNotFoundException {
        titre(1, "NetworkSnapshot : le graphe entier en un writeObject");
        Zone z1 = new Zone(1, "Centre-ville");
        Zone z2 = new Zone(2, "Périphérie");
        Station gar = new Station("GAR", "Gare Centrale", z1);
        Station uni = new Station("UNI", "Université", z1);
        Station hop = new Station("HOP", "Hôpital Sud", z2);
        List<Zone> zones = new ArrayList<>();
        zones.add(z1);
        zones.add(z2);
        Line ligneA = new Line("Ligne A");
        ligneA.addStation(gar);
        ligneA.addStation(uni);
        ligneA.addStation(hop);
        List<Line> lignes = new ArrayList<>();
        lignes.add(ligneA);
        NetworkSnapshot original = new NetworkSnapshot(lignes, zones,
                Instant.parse("2026-10-20T08:30:00Z"), "Rentr\u00E9e 2026");

        Path fichier = racine.resolve("reseau-v1.snapshot");
        original.sauvegarder(fichier);
        System.out.println("  -> \u00E9crit : " + fichier.getFileName() + " ("
                + Files.size(fichier) + " octets — descripteurs de classe + valeurs des champs)");

        NetworkSnapshot relu = NetworkSnapshot.charger(fichier);
        System.out.println("  -> relu  : " + relu);
        System.out.println("     m\u00EAme nombre de lignes : "
                + (relu.getLignes().size() == original.getLignes().size())
                + " ; m\u00EAme horodatage : " + relu.getHorodatage().equals(original.getHorodatage())
                + " ; m\u00EAme description : " + "Rentr\u00E9e 2026".equals(relu.getDescription()));
        System.out.println("     (le hook de Station a caus\u00E9 pendant l'\u00E9criture et la relecture ci-dessus)");
        return fichier;
    }

    /**
     * Scène 2 — LE UID QUI SE RETOURNE : la V2 a ajouté un champ
     * {@code version} ET passé le UID de 1 à 2. Résultat : le fichier de la
     * scène 1, écrit six mois plus tôt par la même équipe, est devenu
     * illisible. Le message de l'exception est le verdict :
     * {@code "local class incompatible: stream classdesc serialVersionUID = 1,
     * local class serialVersionUID = 2"}.
     *
     * @param fichierV1 l'instantané écrit en scène 1
     */
    private static void scene2RuptureDeUid(Path fichierV1) {
        titre(2, "serialVersionUID : changer le num\u00E9ro, c'est rompre le contrat");
        try {
            NetworkSnapshotV2 v2 = NetworkSnapshotV2.migrerDepuisV1(fichierV1);
            System.out.println("  -> inattendu : la V2 a lu du V1 ? " + v2);
        } catch (InvalidClassException e) {
            System.out.println("  -> InvalidClassException (attendue, c'est la le\u00E7on) : " + e.getMessage());
            System.out.println("     la remappe de nom par resolveClass n'a pas sauv\u00E9 la V2 :");
            System.out.println("     le UID se v\u00E9rifie AVANT le nom. La bonne \u00E9volution \u00E9tait :");
            System.out.println("     UID stable + nouveau champ \u00E0 d\u00E9faut (prouv\u00E9 par VersioningTest).");
        } catch (IOException | ClassNotFoundException e) {
            System.out.println("  -> autre panne (inattendue ici) : " + e);
        }
    }

    /**
     * Scène 3 — EXTERNALIZABLE EN MODE MANUEL : le même ticket, deux
     * formats. {@code Ticket} écrit trois valeurs aplaties (writeUTF,
     * writeDouble, writeInt) ; l'équivalent {@code Serializable} promène ses
     * descripteurs de classe et son graphe. La différence se mesure aux
     * octets.
     *
     * @throws IOException            si le flux mémoire refuse (ne devrait jamais arriver)
     * @throws ClassNotFoundException si une classe du graphe n'existe plus (ici : non)
     */
    private static void scene3Externalizable() throws IOException, ClassNotFoundException {
        titre(3, "Externalizable : \u00E9crit \u00E0 la main, donc \u00E9crit petit");
        Zone zone = new Zone(1, "Centre-ville");
        Ticket ticket = Ticket.unite(zone);

        byte[] octetsEx = versOctets(ticket);
        byte[] octetsSer = versOctets(new TicketSerializable(zone));
        System.out.println("  -> Ticket (Externalizable, 3 valeurs aplaties) : "
                + octetsEx.length + " octets");
        System.out.println("  -> \u00E9quivalent Serializable (descripteurs + graphe Zone) : "
                + octetsSer.length + " octets");
        System.out.println("  -> \u00E9conomie : " + (octetsSer.length - octetsEx.length)
                + " octets — MAIS le nom de la zone ne voyage pas :");
        Ticket relu = (Ticket) depuisOctets(octetsEx);
        System.out.println("     relu : " + relu.getType() + " " + relu.getPrix() + " \u20AC, zone n\u00B0"
                + relu.getZone().getNumero() + " nomm\u00E9e \u00AB " + relu.getZone().getNom() + " \u00BB");
    }

    /**
     * Scène 4 — LE TRANSIENT QUI S'ÉVAPORE : {@code Station} a chaudement
     * calculé son cache de présentation ; le flux, lui, n'emporte pas le
     * champ. À la relecture : {@code null} (prouvé par réflexion, le champ
     * est privé), puis {@link Station#getAffichage()} le recalcule à la
     * demande. Le principe : ne voyagent que les données irremplaçables.
     *
     * @throws IOException            si le flux mémoire refuse
     * @throws ClassNotFoundException si une classe du graphe n'existe plus (ici : non)
     * @throws NoSuchFieldException   si le champ cacheAffichage a disparu (ici : non)
     * @throws IllegalAccessException si la JVM refuse la réflexion (même module unnamed : non)
     */
    private static void scene4Transient() throws IOException, ClassNotFoundException,
            NoSuchFieldException, IllegalAccessException {
        titre(4, "transient : le cache s'\u00E9vapore, la m\u00E9thode le recalcule");
        Station original = new Station("GAR", "Gare Centrale", new Zone(1, "Centre-ville"));
        System.out.println("  -> avant \u00E9criture, le cache est chaud : \u00AB " + original.getAffichage() + " \u00BB");

        Station relu = (Station) depuisOctets(versOctets(original));
        Field cache = Station.class.getDeclaredField("cacheAffichage");
        cache.setAccessible(true);
        System.out.println("  -> juste apr\u00E8s relecture, cacheAffichage (transient) = " + cache.get(relu));
        System.out.println("  -> premier getAffichage() rel\u00E8ve le cache : \u00AB " + relu.getAffichage() + " \u00BB");
        System.out.println("     deuxi\u00E8me appel, m\u00EAme instance : "
                + (relu.getAffichage() == relu.getAffichage())); // NOSONAR java:S4973 — == sur String est le point : identité de référence, pas égalité
    }

    /**
     * Scène 5 — LE SINGLETON DÉDOUBLÉ : {@code Passenger.INSTANCE} traverse
     * le flux et revient en COPIE — le constructeur privé n'est jamais
     * appelé, la JVM alloue. {@code PassengerCorrige} et son
     * {@code readResolve} retournent l'instance partagée : le voyage ne
     * produit plus qu'une redirection.
     *
     * @throws IOException            si le flux mémoire refuse
     * @throws ClassNotFoundException si une classe du graphe n'existe plus (ici : non)
     */
    private static void scene5Singleton() throws IOException, ClassNotFoundException {
        titre(5, "Singleton : s\u00E9rialis\u00E9, clon\u00E9 — puis r\u00E9par\u00E9 par readResolve");
        Passenger original = Passenger.getInstance();
        Passenger relu = (Passenger) depuisOctets(versOctets(original));
        System.out.println("  -> Passenger (cass\u00E9) : relu == INSTANCE ? " + (relu == original)
                + " — deux objets distincts : " + System.identityHashCode(original)
                + " vs " + System.identityHashCode(relu));

        PassengerCorrige corrige = PassengerCorrige.getInstance();
        PassengerCorrige reluCorrige = (PassengerCorrige) depuisOctets(versOctets(corrige));
        System.out.println("  -> PassengerCorrige (readResolve) : relu == INSTANCE ? "
                + (reluCorrige == corrige) + " — le jumeau est allou\u00E9 puis jet\u00E9, seul l'original survit");
    }

    /**
     * Scène 6 — L'INVENTAIRE : les cinq points de vigilance canoniques,
     * un par ligne, chacun joué par une scène et prouvé par un test.
     */
    private static void scene6CinqPieges() {
        titre(6, "Les cinq pi\u00E8ges \u00E0 retenir");
        System.out.println("  1/ Le singleton \u00E9clate : la d\u00E9s\u00E9rialisation alloue SANS passer par le");
        System.out.println("     constructeur. Rem\u00E8de : readResolve (cf. PassengerCorrige, PitfallsTest).");
        System.out.println("  2/ readObject reconstruit n'importe quelle classe du classpath cit\u00E9e dans");
        System.out.println("     le flux : un fichier s\u00E9rialis\u00E9 n'est pas une donn\u00E9e, c'est un");
        System.out.println("     programme d'assemblage. Ne JAMAIS lire une source non fiable.");
        System.out.println("  3/ transient remplace une information par null : le NPE arrive six mois");
        System.out.println("     plus tard (cf. Passenger.abonnement, TransientTest).");
        System.out.println("  4/ Changer le serialVersionUID d\u00E9cr\u00E8te p\u00E9rim\u00E9s tous les fichiers");
        System.out.println("     \u00E9crits avant (cf. NetworkSnapshotV2, sc\u00E8ne 2).");
        System.out.println("  5/ Les champs static ne voyagent jamais : la relecture rejoue le");
        System.out.println("     initialiseur de classe, pas l'\u00E9tat fig\u00E9 \u00E0 l'\u00E9criture (VersioningTest).");
    }

    /* ===== outils de la scène ===== */

    /** Écrit un objet dans un tableau d'octets (le flux mémoire, pas le disque). */
    private static byte[] versOctets(Serializable objet) throws IOException {
        ByteArrayOutputStream tampon = new ByteArrayOutputStream();
        try (ObjectOutputStream out = new ObjectOutputStream(tampon)) {
            out.writeObject(objet);
        }
        return tampon.toByteArray();
    }

    /** Relit un objet depuis un tableau d'octets écrit par {@link #versOctets}. */
    private static Object depuisOctets(byte[] octets) throws IOException, ClassNotFoundException {
        try (ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(octets))) {
            return in.readObject();
        }
    }

    /**
     * Supprime une arborescence : les fichiers d'abord, le dossier ensuite —
     * le principe du module 60 vaut jusqu'au ménage du {@code finally}.
     */
    private static void detruireRecursivement(Path racine) throws IOException {
        if (!Files.exists(racine)) {
            return;
        }
        try (java.nio.file.DirectoryStream<Path> entrees = Files.newDirectoryStream(racine)) {
            for (Path entree : entrees) {
                if (Files.isDirectory(entree)) {
                    detruireRecursivement(entree);
                } else {
                    Files.deleteIfExists(entree);
                }
            }
        }
        Files.deleteIfExists(racine);
    }

    /**
     * L'équivalent {@code Serializable} du {@link Ticket} — même contenu,
     * format automatique : descripteur de classe, descripteur de Zone,
     * valeurs complètes. C'est la pièce à comparaison de la scène 3 : ce que
     * {@code Externalizable} économise en compactant, il le paie en
     * maintenance du format à la main.
     */
    private static final class TicketSerializable implements Serializable {

        /** Version du format. */
        private static final long serialVersionUID = 1L;

        /** Type du ticket. */
        private final String type;

        /** Prix en euros. */
        private final double prix;

        /** Zone complète — le graphe, pas un numéro aplati. */
        private final Zone zone;

        TicketSerializable(Zone zone) {
            this.type = "Unit\u00E9";
            this.prix = 1.80;
            this.zone = zone;
        }
    }
}
