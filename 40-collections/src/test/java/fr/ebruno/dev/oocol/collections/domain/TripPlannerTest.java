package fr.ebruno.dev.oocol.collections.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * Tests de {@link TripPlanner} — le BFS comme démonstration intégrée des
 * quatre familles de conteneurs ({@code Queue} frontière, {@code Set}
 * visites, {@code Map} prédécesseurs, {@code List} résultat) : chemin le
 * plus court en arrêts, correspondances traversées, et retour « liste vide »
 * sur l'impossible (contrat documenté : vide = échec).
 *
 * @author Emmanuel Bruno
 */
class TripPlannerTest {

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

    /** Ligne A : GAR→UNI→HOP ; Ligne C : HOP→AER (correspondance en HOP). */
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
    void cheminDirectSurMemeLigne() {
        Network reseau = reseauDeuxLignes();

        // Voisins consécutifs sur A : un seul arc, le BFS les touche au 1er rayon.
        List<Station> chemin = TripPlanner.planifier(reseau, gare(), universite());

        assertThat(chemin).containsExactly(gare(), universite());
    }

    @Test
    void cheminIndirectTraverseLaCorrespondance() {
        Network reseau = reseauDeuxLignes();

        // GAR → AER impose le passage par HOP (seule jonction A↔C) :
        // [GAR, UNI, HOP, AER] — 4 stations, 3 arcs, le seul chemin existant.
        List<Station> chemin = TripPlanner.planifier(reseau, gare(), aeroport());

        assertThat(chemin).containsExactly(gare(), universite(), hopital(), aeroport());
    }

    @Test
    void leBFSDonneLePlusCourtEnArrets() {
        // Réseau en étoile + contournement : A—B—C en chaîne, plus A—C directe.
        // Le BFS doit rendre [A, C] (1 arc) et JAMAIS [A, B, C] (2 arcs) —
        // c'est la propriété du parcours en largeur, testée ici nommément.
        Station a = gare();
        Station b = universite();
        Station c = hopital();

        Line chaine = new Line("Chainette");
        chaine.ajouterArret(a);
        chaine.ajouterArret(b);
        chaine.ajouterArret(c);

        Line directe = new Line("Express");
        directe.ajouterArret(a);
        directe.ajouterArret(c);

        Network reseau = new Network();
        reseau.ajouterLigne(chaine);
        reseau.ajouterLigne(directe);

        List<Station> chemin = TripPlanner.planifier(reseau, a, c);

        assertThat(chemin).containsExactly(a, c);
    }

    @Test
    void allerRetourDonneLeCheminInverse() {
        Network reseau = reseauDeuxLignes();

        List<Station> aller = TripPlanner.planifier(reseau, gare(), aeroport());
        List<Station> retour = TripPlanner.planifier(reseau, aeroport(), gare());

        assertThat(retour).containsExactly(aeroport(), hopital(), universite(), gare());
        assertThat(retour).hasSameSizeAs(aller);
    }

    @Test
    void memeDepartEtArriveeDonneUnSingleton() {
        Network reseau = reseauDeuxLignes();

        // Cas limite du BFS : l'ondulation touche l'arrivée au rayon 0.
        assertThat(TripPlanner.planifier(reseau, gare(), gare()))
                .containsExactly(gare());
    }

    @Test
    void stationHorsReseauDonneUneListeVide() {
        Network reseau = reseauDeuxLignes();
        Station inconnue = Station.of("XXX", "Bout-du-Monde", zone1());

        // Contrat documenté : vide = échec (ambiguïté assumée par la méthode).
        assertThat(TripPlanner.planifier(reseau, gare(), inconnue)).isEmpty();
        assertThat(TripPlanner.planifier(reseau, inconnue, gare())).isEmpty();
    }

    @Test
    void extremiteNulleDonneUneListeVideReseauNulExplose() {
        Network reseau = reseauDeuxLignes();

        assertThat(TripPlanner.planifier(reseau, gare(), null)).isEmpty();
        assertThat(TripPlanner.planifier(reseau, null, gare())).isEmpty();

        // Un réseau absent est une erreur de programme, pas une absence de chemin.
        assertThatThrownBy(() -> TripPlanner.planifier(null, gare(), universite()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void leCheminCommenceAuDepartEtFinitALArrivee() {
        Network reseau = reseauDeuxLignes();

        // Invariant structurant du résultat (ordre List) : bornes incluses.
        List<Station> chemin = TripPlanner.planifier(reseau, gare(), aeroport());

        assertThat(chemin.get(0)).isEqualTo(gare());
        assertThat(chemin.get(chemin.size() - 1)).isEqualTo(aeroport());
    }
}
