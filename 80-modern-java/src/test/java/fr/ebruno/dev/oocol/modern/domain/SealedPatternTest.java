package fr.ebruno.dev.oocol.modern.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * La hiérarchie scellée vue du côté du consommateur : chaque switch de motifs
 * écrit ici est une <em>preuve de compilation</em> — il n'a ni {@code default}
 * ni {@code else} « type inattendu », parce que {@code permits} ferme le
 * monde et que le compilateur exige que chaque porte soit ouverte.
 *
 * <p>Le corollaire, rappelé en commentaire sous {@link #nouvelleVarianteCasseraitLesSwitchs()} :
 * ajouter une variante à un {@code permits} ne « marche pas » partout — ça
 * fait rougir chaque switch exhaustif du programme. C'est le comportement que
 * le module 50 payait en exceptions-runtime imprévisibles ; ici, c'est une
 * erreur de compilation, donc une surprise en moins en production.
 *
 * @author Emmanuel Bruno
 */
class SealedPatternTest {

    /** Le switch de motifs sur Vehicle — même corps que la scène 2 de la démo. */
    private static String rapport(Vehicle v) {
        return switch (v) {
            case MetroTrain m -> "M\u00e9tro %d voitures".formatted(m.nombreVoitures());
            case Bus b -> "Bus ligne " + b.ligne();
            case Tramway t -> "Tram %.0f m".formatted(t.longueurMetres());
        };
    }

    /** Le switch de motifs sur Propulsion — l'union sans méthode commune. */
    private static String decrirePropulsion(Propulsion p) {
        return switch (p) {
            case Electrique e -> "\u00e9lectrique %d kW".formatted(e.puissanceKW());
            case Diesel d -> "diesel %d cm3".formatted(d.cylindree());
            case Hydrogene h -> "hydrog\u00e8ne %d bar".formatted(h.pressionBar());
        };
    }

    @Test
    void switchExhaustifCouvreLesTroisVehicules() {
        List<Vehicle> parc = List.of(
                new MetroTrain("M-01", 160, 4, new Electrique(2400)),
                new Bus("B-12", 95, "A"),
                new Tramway("T-03", 200, new Hydrogene(350), 33.0));

        assertThat(parc).extracting(SealedPatternTest::rapport)
                .containsExactly("M\u00e9tro 4 voitures", "Bus ligne A", "Tram 33 m");
    }

    @Test
    void switchSurTarifCalculeLePrixParVariante() {
        Zone zone = new Zone(1, "Centre");
        assertThat(AnalyseReseau.prixDe(new TarifUnite(zone, 1.90))).isEqualTo(1.90);
        assertThat(AnalyseReseau.prixDe(new TarifJour(List.of(zone), 4.50))).isEqualTo(4.50);
        assertThat(AnalyseReseau.prixDe(
                new TarifAbonnement("Alice", LocalDate.of(2026, 10, 1), LocalDate.of(2027, 9, 30))))
                .isEqualTo(AnalyseReseau.PRIX_ABONNEMENT_MENSUEL);
    }

    @Test
    void instanceofPatternSansCastExplicite() {
        Vehicle v = new MetroTrain("M-02", 180, 6, new Electrique(3000));

        // Java 16 : le motif « type variable » lie ET transforme — le duo
        // « test instanceof puis transtypage explicite » du module 5 devient une ligne.
        if (v instanceof MetroTrain m) {
            assertThat(m.nombreVoitures()).isEqualTo(6);
        } else {
            throw new AssertionError("le motif devait matcher");
        }
    }

    @Test
    void propulsionSealedSeRaconteParSwitch() {
        assertThat(decrirePropulsion(new Electrique(2400))).isEqualTo("\u00e9lectrique 2400 kW");
        assertThat(decrirePropulsion(new Diesel(2000))).isEqualTo("diesel 2000 cm3");
        assertThat(decrirePropulsion(new Hydrogene(350))).isEqualTo("hydrog\u00e8ne 350 bar");
    }

    @Test
    void laScellureEstInscriteDansLaReflexion() {
        // « sealed » n'est pas un commentaire : c'est un fait vérifiable par le compilateur… et par nous.
        assertThat(Vehicle.class.isSealed()).isTrue();
        assertThat(Vehicle.class.getPermittedSubclasses())
                .extracting(Class::getSimpleName)
                .containsExactlyInAnyOrder("MetroTrain", "Bus", "Tramway");
        assertThat(Propulsion.class.isSealed()).isTrue();
        assertThat(Tarif.class.isSealed()).isTrue();
    }

    @Test
    void nouvelleVarianteCasseraitLesSwitchs() {
        // Preuve négative, jouable uniquement à la compilation — consignée ici pour la leçon :
        // déclarer un variant Funiculaire en plus (un record implémentant Vehicle)
        // ne compile PAS (hors clauses permits) ; et si on l'ajoute AUSSI à « permits »,
        // chaque switch de cette classe (et de la démo) devient une ERREUR de compilation :
        // « the switch expression does not cover all possible input values ».
        // Le compilateur tient le registre des points d'impact — c'est la définition même
        // d'une évolution de hiérarchie CONTROLLÉE, là où le module 30 laissait la hiérarchie ouverte.
        assertThat(rapport(new Bus("B-1", 60, "C")))
                .isEqualTo(VersionLegacy.rapport(new Bus("B-1", 60, "C")));
        // Dernière ligne : le switch moderne et la chaîne instanceof legacy rendent le même texte —
        // la différence n'est pas le résultat, c'est la garantie.
    }
}
