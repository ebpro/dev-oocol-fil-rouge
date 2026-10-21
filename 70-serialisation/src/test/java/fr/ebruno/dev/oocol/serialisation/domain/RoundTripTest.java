package fr.ebruno.dev.oocol.serialisation.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.NotSerializableException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * L'aller-retour disque — la promesse {@code Serializable} : une écriture,
 * tout le graphe. Chaque test saisit un objet, le fige en octets, le relit,
 * et compare le revenant à l'original.
 *
 * <p>Les cas tordus valent le détour : le {@code transient} qui revient
 * {@code null} (prouvé par réflexion, le champ est privé), la référence
 * partagée qui le reste (le flux n'écrit chaque objet qu'une fois), le
 * {@code null} qui traverse un {@code writeObject} sans broncher, et le
 * champ non sérialisable qui fait tout capoter avec un message qui nomme le
 * coupable.
 *
 * <p>Le gabarit {@link #roundTrip} est le seul outil : un
 * {@code ByteArrayOutputStream}, un {@code ObjectOutputStream}, et le
 * voyage aller-retour est joué — le disque n'intervient que là où le test
 * veut prouver le contrat de fichier ({@link #snapshotFideleSurFichier}).
 *
 * @author Emmanuel Bruno
 */
class RoundTripTest {

    /** Fige un objet et le relit, dans la mémoire — le trajet aller-retour nu. */
    private static Object roundTrip(Object original) throws IOException, ClassNotFoundException {
        ByteArrayOutputStream tampon = new ByteArrayOutputStream();
        try (ObjectOutputStream sortie = new ObjectOutputStream(tampon)) {
            sortie.writeObject(original);
        }
        try (ObjectInputStream entree = new ObjectInputStream(new ByteArrayInputStream(tampon.toByteArray()))) {
            return entree.readObject();
        }
    }

    /** Le champ privé d'un objet, sans passer par son API — la preuve par réflexion. */
    private static Object champPrive(Object cible, String nom) throws Exception {
        Field champ = cible.getClass().getDeclaredField(nom);
        champ.setAccessible(true);
        return champ.get(cible);
    }

    @Test
    void stationSurviveAUnAllerRetour() throws Exception {
        Station originale = new Station("GAR", "Gare Centrale", new Zone(1, "Centre-ville"));

        Station relu = (Station) roundTrip(originale);

        assertThat(relu).isNotSameAs(originale); // un nouvel objet, pas la même référence
        assertThat(relu.getCode()).isEqualTo("GAR");
        assertThat(relu.getNom()).isEqualTo("Gare Centrale");
        assertThat(relu.getZone().getNumero()).isEqualTo(1);
        assertThat(relu.getZone().getNom()).isEqualTo("Centre-ville");
    }

    @Test
    void cacheAffichageEstNullApresRelecture() throws Exception {
        Station originale = new Station("UNI", "Universite", new Zone(2, "Campus"));
        originale.getAffichage(); // le cache devient chaud avant le voyage

        Station relu = (Station) roundTrip(originale);

        assertThat(champPrive(relu, "cacheAffichage")).isNull(); // transient : évaporé
        assertThat(relu.getAffichage()).isEqualTo("UNI \u2014 Universite"); // et recalculé à la demande
    }

    @Test
    void ligneAvecArretsRoundTrip() throws Exception {
        Zone commune = new Zone(1, "Centre-ville");
        Line originale = new Line("Ligne A");
        originale.addStation(new Station("A1", "Nord", commune));
        originale.addStation(new Station("A2", "Sud", commune));

        Line relu = (Line) roundTrip(originale);

        assertThat(relu.getNom()).isEqualTo("Ligne A");
        List<String> codes = new ArrayList<>();
        for (Station arret : relu.getStations()) {
            codes.add(arret.getCode());
        }
        assertThat(codes).containsExactly("A1", "A2"); // l'ordre du trajet est celui de l'énumération
    }

    @Test
    void snapshotFideleSurFichier(@TempDir Path dossier) throws Exception {
        Zone z1 = new Zone(1, "Centre-ville");
        List<Zone> zones = new ArrayList<>();
        zones.add(z1);
        Line ligne = new Line("Ligne A");
        ligne.addStation(new Station("GAR", "Gare Centrale", z1));
        List<Line> lignes = new ArrayList<>();
        lignes.add(ligne);
        NetworkSnapshot original = new NetworkSnapshot(lignes, zones,
                Instant.parse("2026-10-20T08:30:00Z"), "Rentree 2026");

        Path fichier = dossier.resolve("reseau.snapshot"); // le contrat passe par le disque
        original.sauvegarder(fichier);
        NetworkSnapshot relu = NetworkSnapshot.charger(fichier);

        assertThat(relu.getLignes()).hasSize(1);
        assertThat(relu.getLignes().get(0).getStations()).hasSize(1);
        assertThat(relu.getZones()).hasSize(1);
        assertThat(relu.getHorodatage()).isEqualTo(Instant.parse("2026-10-20T08:30:00Z"));
        assertThat(relu.getDescription()).isEqualTo("Rentree 2026");
    }

    @Test
    void descriptionNulleTraverseLeFlux() throws Exception {
        Zone z1 = new Zone(1, "Centre-ville");
        List<Zone> zones = new ArrayList<>();
        zones.add(z1);
        List<Line> lignes = new ArrayList<>();
        NetworkSnapshot original = new NetworkSnapshot(lignes, zones,
                Instant.parse("2026-10-20T08:30:00Z"), null); // description absente

        NetworkSnapshot relu = (NetworkSnapshot) roundTrip(original);

        assertThat(relu.getDescription()).isNull(); // le null est une valeur, le flux la respecte
        assertThat(relu.getLignes()).isEmpty();
    }

    @Test
    void abonnementRoundTrip() throws Exception {
        Subscription original = new Subscription("Mensuel", LocalDate.of(2026, 11, 30));

        Subscription relu = (Subscription) roundTrip(original);

        assertThat(relu.getType()).isEqualTo("Mensuel");
        assertThat(relu.getExpireDate()).isEqualTo(LocalDate.of(2026, 11, 30));
    }

    @Test
    void referencesPartageesRestentPartagees() throws Exception {
        Zone commune = new Zone(1, "Centre-ville"); // UN objet, DEUX stations le pointent
        Line ligne = new Line("Ligne A");
        ligne.addStation(new Station("A1", "Nord", commune));
        ligne.addStation(new Station("A2", "Sud", commune));

        Line relu = (Line) roundTrip(ligne);

        assertThat(relu.getStations().get(0).getZone())
                .isSameAs(relu.getStations().get(1).getZone());
        // le flux écrit chaque objet une seule fois et référence le reste par
        // handle : le graphe d'objets n'est pas une liste de copies
    }

    @Test
    void champNonSerialisableLeveNotSerializable() throws Exception {
        PorteCarte carte = new PorteCarte();

        try {
            roundTrip(carte);
            throw new AssertionError("attendu : NotSerializableException");
        } catch (NotSerializableException attendu) {
            assertThat(attendu.getMessage()).contains("Portefeuille");
        }
        // le marqueur Serializable n'est PAS hérité du sens inverse : un seul
        // champ non marqué dans le graphe suffit à faire capoter l'écriture —
        // et l'exception nomme le coupable, pas le porteur
    }

    /** Le portefeuille — oublié de marquer {@code Serializable} : le grain de sable. */
    static final class Portefeuille {
        int solde = 10;
    }

    /** La carte — marquée {@code Serializable}, mais son portefeuille ne l'est pas. */
    static final class PorteCarte implements Serializable {

        /** Version du format. */
        private static final long serialVersionUID = 1L;

        /** Titulaire, sérialisable. */
        private final String titulaire = "Anna";

        /** Le grain de sable : un objet sans marqueur dans le graphe. */
        private final Portefeuille portefeuille = new Portefeuille();
    }
}
