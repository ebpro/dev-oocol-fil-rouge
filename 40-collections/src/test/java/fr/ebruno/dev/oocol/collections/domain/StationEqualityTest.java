package fr.ebruno.dev.oocol.collections.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

/**
 * Tests du contrat d'identité de {@link Station} — la leçon centrale de W5 :
 * equality par code, cohérence du hashCode, ordre naturel et comparateur
 * délégué. Le test {@link #hashSetRetrouveUnAutreExemplaireDeMemeCode()} EST
 * la preuve que le bug motivateur est fermé.
 *
 * @author Emmanuel Bruno
 */
class StationEqualityTest {

    private static Zone zone1() {
        return new Zone(1, "Centre-ville");
    }

    /** {@code "GAR"} original. */
    private static Station gare() {
        return Station.of("GAR", "Gare Centrale", zone1());
    }

    /** Autre exemplaire de {@code "GAR"} : même code, nom différent, zone distincte. */
    private static Station gareCopie() {
        return Station.of("GAR", "Gare Centrale (re-creee)", new Zone(1, "Centre"));
    }

    @Test
    void deuxStationsDeMemeCodeSontEgales() {
        // Le code est la clé métier : nom et zone ne comptent PAS dans equals.
        assertThat(gare()).isEqualTo(gareCopie());
    }

    @Test
    void deuxCodesDifferentsNeSontPasEgaux() {
        Station hopital = Station.of("HOP", "Hopital", zone1());
        assertThat(gare()).isNotEqualTo(hopital);
    }

    @Test
    void equalsEstReflexifEtResisteAuxEtranges() {
        Station gare = gare();
        assertThat(gare).isEqualTo(gare); // même référence : succès rapide
        assertThat(gare).isNotEqualTo(null); // null n'est jamais égal à rien
        assertThat(gare.equals("GAR")).isFalse(); // une String n'est pas une Station
    }

    @Test
    void objetsEgauxOntToujoursLeMemeHashCode() {
        // La règle que les collections de hachage exigent :
        // a.equals(b)  ⟹  a.hashCode() == b.hashCode()
        assertThat(gare().hashCode()).isEqualTo(gareCopie().hashCode());
    }

    @Test
    void hashSetRetrouveUnAutreExemplaireDeMemeCode() {
        // PREUVE DU BUG FERMÉ : sans equals/hashCode par code, le HashSet
        // comparerait les références et contains(copie) vaudrait false —
        // l'objet « présent » serait déclaré absent, sans le moindre cri.
        Set<Station> desservies = new HashSet<Station>();
        desservies.add(gare());

        assertThat(desservies).contains(gareCopie());
        assertThat(desservies.remove(gareCopie())).isTrue();
        assertThat(desservies).isEmpty();
    }

    @Test
    void ordreNaturelRangeParCode() {
        Station aer = Station.of("AER", "Aeroport", zone1());
        Station hop = Station.of("HOP", "Hopital", zone1());
        List<Station> vrac = new ArrayList<Station>();
        vrac.add(hop);
        vrac.add(gare());
        vrac.add(aer);

        Collections.sort(vrac); // aucun comparateur fourni : c'est l'ordre naturel de Station

        assertThat(vrac).extracting("code").containsExactly("AER", "GAR", "HOP");
    }

    @Test
    void comparateurParNomIgnoreLaCasse() {
        // compareToIgnoreCase : « academie » passe avant « Bastille » malgré
        // le a minuscule — un compareTo brut les eût inversés ('B' < 'a').
        Station bastille = Station.of("BAS", "Bastille", zone1());
        Station academie = Station.of("ACA", "academie des sciences", zone1());
        Station casino = Station.of("CAS", "Casino", zone1());

        Comparator<Station> parNom = Station.parNom();
        assertThat(parNom.compare(academie, bastille)).isNegative();
        assertThat(parNom.compare(casino, academie)).isPositive();
    }

    @Test
    void fabriqueOfPoseIdentiteComplete() {
        Station gare = gare();

        assertThat(gare.getCode()).isEqualTo("GAR");
        assertThat(gare.getNom()).isEqualTo("Gare Centrale");
        assertThat(gare.getZone()).isEqualTo(zone1()); // Zone.equals : par numéro
        assertThat(gare.isAccessible()).isFalse(); // le constructeur court pose PMR = false
    }
}
