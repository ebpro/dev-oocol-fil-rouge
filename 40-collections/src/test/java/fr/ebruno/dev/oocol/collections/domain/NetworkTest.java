package fr.ebruno.dev.oocol.collections.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Set;

import org.junit.jupiter.api.Test;

/**
 * Tests de {@link Network} — la table d'adjacence {@code Map<Station,
 * Set<Line>}} : union sans doublon via {@code Set}, lookup par {@code equals}
 * (l'autre instance de même code retrouve la bonne entrée), ensemble vide
 * plutôt que {@code null} pour l'inconnu.
 *
 * @author Emmanuel Bruno
 */
class NetworkTest {

    private static Zone zone1() {
        return new Zone(1, "Centre-ville");
    }

    private static Zone zone2() {
        return new Zone(2, "P\u00E9riph\u00E9rie");
    }

    private static Station gare() {
        return Station.of("GAR", "Gare Centrale", zone1());
    }

    private static Station universite() {
        return Station.of("UNI", "Universite", zone1());
    }

    private static Station hopital() {
        return Station.of("HOP", "Hopital", zone2());
    }

    private static Station aeroport() {
        return Station.of("AER", "Aeroport", zone2());
    }

    /** Réseau en croix : Ligne A (GAR→UNI→HOP) et Ligne C (HOP→AER). */
    private static Network reseauDeuxLignes() {
        Line ligneA = new Line("Ligne A");
        ligneA.ajouterArret(gare());
        ligneA.ajouterArret(universite());
        ligneA.ajouterArret(hopital());

        Line ligneC = new Line("Ligne C");
        ligneC.ajouterArret(hopital());
        ligneC.ajouterArret(aeroport());

        Network reseau = new Network();
        reseau.ajouterLigne(ligneA);
        reseau.ajouterLigne(ligneC);
        return reseau;
    }

    @Test
    void ajouterLignePeupleLaTableDAdjacence() {
        Network reseau = reseauDeuxLignes();

        // GAR n'est desservie QUE par A ; HOP par les deux (c'est la correspondance).
        assertThat(reseau.lignesA(gare()))
                .extracting("nom")
                .containsExactly("Ligne A");
        assertThat(reseau.lignesA(hopital()))
                .extracting("nom")
                .containsExactlyInAnyOrder("Ligne A", "Ligne C");
    }

    @Test
    void correspondanceRetrouveeParAutreInstance() {
        // LE test du contrat appliqué à la Map : HashMap.get consulte hashCode
        // puis equals. Une AUTRE instance de "HOP" (même code, nom recréé)
        // doit retrouver l'entrée déposée par l'instance d'origine — sinon le
        // graphe se fende en deux clés distinctes pour une seule station.
        Network reseau = reseauDeuxLignes();
        Station autreInstance = Station.of("HOP", "Hopital (re-cree)", zone2());

        assertThat(reseau.lignesA(autreInstance)).hasSize(2);
    }

    @Test
    void stationInconnueDonneUnEnsembleVideJamaisNull() {
        Network reseau = reseauDeuxLignes();
        Station inconnue = Station.of("XXX", "Bout-du-Monde", zone1());

        // Collections.emptySet : l'appelant peut itérer sans test défensif.
        Set<Line> lignes = reseau.lignesA(inconnue);
        assertThat(lignes).isEmpty();
        assertThat(lignes).isNotNull();
    }

    @Test
    void tousLesArretsEstUnSetSansDoublon() {
        Network reseau = reseauDeuxLignes();

        // HOP apparaît dans A ET dans C ; l'union la compte UNE fois (Set + equals).
        assertThat(reseau.tousLesArrets()).hasSize(4);
        assertThat(reseau.tousLesArrets()).contains(gare(), universite(), hopital(), aeroport());
    }

    @Test
    void nombreCorrespondancesCompteLesStationsMultiLignes() {
        Network reseau = reseauDeuxLignes();

        // Seule HOP est desservie par deux lignes.
        assertThat(reseau.nombreCorrespondances()).isEqualTo(1);
    }

    @Test
    void trouverStationParCodeRendLaReferenceDuReseau() {
        Network reseau = reseauDeuxLignes();

        // Le lookup rend l'exemplaire stocké (et non la copie demandée) :
        // la référence interne est la source de vérité pour la zone réelle.
        Station trouvee = reseau.trouverStationParCode("HOP");
        assertThat(trouvee).isNotNull();
        assertThat(trouvee.getNom()).isEqualTo("Hopital");
        assertThat(reseau.trouverStationParCode("ZZZ")).isNull();
    }

    @Test
    void leReseauRefuseLesLignesInvalides() {
        Network reseau = new Network();

        assertThatThrownBy(() -> reseau.ajouterLigne(null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> reseau.ajouterLigne(new Line("Ligne vide")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("aucun arrêt");
    }

    @Test
    void lesVuesRetourneesSontEnLectureSeule() {
        Network reseau = reseauDeuxLignes();

        // Ni la liste des lignes ni les ensembles de correspondances ne sont
        // modifiables de l'extérieur : seul ajouterLigne fait bouger le graphe.
        assertThatThrownBy(() -> reseau.getLignes().add(new Line("Ligne pirate")))
                .isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> reseau.lignesA(gare()).add(new Line("Ligne pirate")))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void toStringResumeLeReseau() {
        Network reseau = reseauDeuxLignes();

        assertThat(reseau.toString())
                .isEqualTo("R\u00E9seau Trans'U \u2014 2 lignes, 4 stations, 1 correspondances");
    }
}
