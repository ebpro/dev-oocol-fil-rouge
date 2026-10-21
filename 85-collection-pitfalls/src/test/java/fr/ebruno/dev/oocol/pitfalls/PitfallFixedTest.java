package fr.ebruno.dev.oocol.pitfalls;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import fr.ebruno.dev.oocol.pitfalls.fixed.FixedFareList;
import fr.ebruno.dev.oocol.pitfalls.fixed.FixedGrouping;
import fr.ebruno.dev.oocol.pitfalls.fixed.FixedLine;
import fr.ebruno.dev.oocol.pitfalls.fixed.FixedMapMerge;
import fr.ebruno.dev.oocol.pitfalls.fixed.FixedNetwork;
import fr.ebruno.dev.oocol.pitfalls.fixed.FixedNullKey;
import fr.ebruno.dev.oocol.pitfalls.fixed.FixedRemoveLoop;
import fr.ebruno.dev.oocol.pitfalls.fixed.FixedSortStable;
import fr.ebruno.dev.oocol.pitfalls.fixed.FixedStation;
import fr.ebruno.dev.oocol.pitfalls.fixed.FixedTicketCache;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Le miroir vert de {@link PitfallDetectionTest} : les mêmes assertions,
 * jouées contre le paquet {@code fixed.*}. Elles passent — c'est la preuve
 * que les attentes de la chasse aux bugs sont les bonnes, et le contrat de
 * l'étudiant : après sa correction, chaque test de {@code PitfallDetectionTest}
 * doit ressembler à son jumeau ici.
 *
 * @author Emmanuel Bruno
 */
class PitfallFixedTest {

    private static final FixedStation GAR1 = new FixedStation("GAR", "Gare Centrale");
    private static final FixedStation GAR2 = new FixedStation("GAR", "Gare Neuve");
    private static final FixedStation UNI1 = new FixedStation("UNI", "Universite");
    private static final FixedStation UNI2 = new FixedStation("UNI", "Campus");
    private static final FixedStation HOP1 = new FixedStation("HOP", "Hopital");
    private static final FixedStation HOP2 = new FixedStation("HOP", "CH");

    private static final FixedStation MUSEE_GAR = new FixedStation("GAR", "Musee");
    private static final FixedStation MUSEE_ARC = new FixedStation("ARC", "Musee");

    private static FixedNetwork reseauAvec(String... lignesEtCodes) {
        FixedNetwork reseau = new FixedNetwork();
        for (int i = 0; i < lignesEtCodes.length; i += 2) {
            FixedLine ligne = new FixedLine(lignesEtCodes[i]);
            ligne.ajouterArret(new FixedStation(lignesEtCodes[i + 1], "Arret " + lignesEtCodes[i + 1]));
            reseau.ajouterLigne(ligne);
        }
        return reseau;
    }

    // ---- Piège 1 corrigé : equals + hashCode cohérents -------------------

    @Test
    @DisplayName("Stations égales : même hashCode — le contrat Object est respecté")
    void stationsEgalesMemeHashCode() {
        assertThat(GAR1).isEqualTo(GAR2);
        assertThat(GAR1.hashCode()).isEqualTo(GAR2.hashCode());
    }

    @Test
    @DisplayName("HashMap retrouve la valeur via une clé égale")
    void hashMapRetrouveCleEgale() {
        Map<FixedStation, Double> carte = new HashMap<>();
        carte.put(GAR1, 2.0);

        assertThat(carte.get(GAR2)).isEqualTo(2.0);
    }

    @Test
    @DisplayName("Le HashSet déduplique les stations égales : trois instances, un seul élément")
    void hashSetContientStationEgale() {
        Set<FixedStation> arrets = new HashSet<>();
        arrets.add(GAR1);
        arrets.add(GAR2);
        arrets.add(new FixedStation("GAR", "Gare du Nord"));

        // hashCode = Objects.hash(code) : les trois tombent dans le même seau,
        // equals les reconnaît égales -> la seconde et la troisième insertion sont refusées.
        assertThat(arrets).hasSize(1);
    }

    @Test
    @DisplayName("Deux put avec clés égales : une seule entrée, valeur à jour")
    void putAvecCleEgaleNeDupliquePas() {
        Map<FixedStation, Double> carte = new HashMap<>();
        carte.put(GAR1, 2.0);
        carte.put(GAR2, 2.5);

        assertThat(carte).hasSize(1);
        assertThat(carte.get(GAR1)).isEqualTo(2.5);
    }

    // ---- Piège 2 corrigé : copie défensive --------------------------------

    @Test
    @DisplayName("getArrets() renvoie une liste immuable : add externe refusé")
    void getterDeLigneDoitEtreImmuable() {
        FixedLine ligne = new FixedLine("A");
        ligne.ajouterArret(GAR1);

        assertThatThrownBy(() -> ligne.getArrets().add(UNI1))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    @DisplayName("L'ajout externe refusé n'atteint pas l'intérieur de la ligne")
    void ajoutExterneNAtteintPasLInterne() {
        FixedLine ligne = new FixedLine("A");
        ligne.ajouterArret(GAR1);
        ligne.ajouterArret(UNI1);

        try {
            ligne.getArrets().add(HOP1);
        } catch (UnsupportedOperationException attendue) {
            // comportement correct : la mutation est refusée
        }

        assertThat(ligne.getArrets()).hasSize(2);
    }

    @Test
    @DisplayName("Vue capturée = instantané : les ajouts internes ultérieurs n'y paraissent pas")
    void vueCaptureeFigee() {
        FixedLine ligne = new FixedLine("A");
        ligne.ajouterArret(GAR1);
        List<FixedStation> vue = ligne.getArrets();

        ligne.ajouterArret(UNI1);

        assertThat(vue).hasSize(1);
    }

    // ---- Piège 3 corrigé : removeIf ---------------------------------------

    @Test
    @DisplayName("Retrait sans itérateur concurrent : aucune exception")
    void retirerSansCorrespondanceNeLevePas() {
        FixedNetwork reseau = reseauAvec("U", "UNI", "V", "GAR", "W", "HOP");

        assertThatCode(() -> reseau.retirerLignesSansCorrespondance("XXX"))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Seules les lignes desservant l'arrêt survivent")
    void retirerNeGardeQueLesLignesDesservies() {
        FixedNetwork reseau = reseauAvec("U", "UNI", "V", "GAR", "W", "GAR");

        reseau.retirerLignesSansCorrespondance("GAR");

        assertThat(reseau.getLignes()).extracting(FixedLine::getNom)
                .containsExactlyInAnyOrder("V", "W");
    }

    @Test
    @DisplayName("Aucune ligne ne desservant l'arrêt : le réseau se vide proprement")
    void retirerQuandRienNeCorrespond() {
        FixedNetwork reseau = reseauAvec("U", "UNI", "V", "GAR", "W", "HOP");

        reseau.retirerLignesSansCorrespondance("XXX");

        assertThat(reseau.getLignes()).isEmpty();
    }

    // ---- Piège 4 corrigé : clés à hashCode cohérent -----------------------

    @Test
    @DisplayName("lookup avec une station égale retrouve le prix")
    void lookupAvecStationEgale() {
        FixedTicketCache cache = new FixedTicketCache();
        cache.cache(GAR1, 2.0);

        assertThat(cache.lookup(GAR2)).isEqualTo(2.0);
    }

    @Test
    @DisplayName("Deux clés égales n'occupent qu'une entrée")
    void deuxClesEgalesUneEntree() {
        FixedTicketCache cache = new FixedTicketCache();
        cache.cache(GAR1, 2.0);
        cache.cache(GAR2, 2.5);

        assertThat(cache.taille()).isEqualTo(1);
    }

    // ---- Piège 5 corrigé : ArrayList modifiable ---------------------------

    @Test
    @DisplayName("La grille tarifaire accepte les ajouts")
    void tarifsAcceptentAjout() {
        List<Double> tarifs = new FixedFareList().tarifsStandard();

        assertThatCode(() -> tarifs.add(20.0)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Après un ajout légitime, la grille a grandi")
    void tarifsGrandissentApresAjout() {
        List<Double> tarifs = new FixedFareList().tarifsStandard();

        tarifs.add(20.0);

        assertThat(tarifs).containsExactly(2.0, 5.5, 12.0, 20.0);
    }

    @Test
    @DisplayName("La grille tarifaire accepte les retraits")
    void tarifsAcceptentRetrait() {
        List<Double> tarifs = new FixedFareList().tarifsStandard();

        assertThatCode(() -> tarifs.remove(Double.valueOf(5.5))).doesNotThrowAnyException();
        assertThat(tarifs).containsExactly(2.0, 12.0);
    }

    // ---- Piège 6 corrigé : membership fiable ------------------------------

    @Test
    @DisplayName("estDansZone reconnaît une station égale à un membre")
    void estDansZoneReconnaitEgal() {
        List<FixedStation> zone = List.of(GAR1, UNI1);

        assertThat(new FixedGrouping().estDansZone(zone, GAR2)).isTrue();
    }

    @Test
    @DisplayName("Chaque station égale à un membre de la zone est reconnue")
    void estDansZoneReconnaitChaqueMembre() {
        List<FixedStation> zone = List.of(GAR1, UNI1, HOP1);
        FixedGrouping grouping = new FixedGrouping();

        boolean toutesReconnues = grouping.estDansZone(zone, GAR2)
                && grouping.estDansZone(zone, UNI2)
                && grouping.estDansZone(zone, HOP2);

        assertThat(toutesReconnues).isTrue();
    }

    // ---- Piège 7 corrigé : comparateur au contrat -------------------------

    @Test
    @DisplayName("Noms égaux : le comparateur rend 0")
    void comparateurRendZeroSurEgaux() {
        assertThat(new FixedSortStable().comparateurParNom().compare(MUSEE_GAR, MUSEE_ARC))
                .isZero();
    }

    @Test
    @DisplayName("Antisymétrie : sgn(compare(a,b)) == -sgn(compare(b,a)))")
    void comparateurAntisymetrique() {
        var c = new FixedSortStable().comparateurParNom();

        assertThat(Integer.signum(c.compare(MUSEE_GAR, MUSEE_ARC)))
                .isEqualTo(-Integer.signum(c.compare(MUSEE_ARC, MUSEE_GAR)));
    }

    @Test
    @DisplayName("Réflexivité : compare(a,a) == 0")
    void comparateurReflexif() {
        assertThat(new FixedSortStable().comparateurParNom().compare(MUSEE_GAR, MUSEE_GAR))
                .isZero();
    }

    // ---- Piège 8 corrigé : null filtrés -----------------------------------

    @Test
    @DisplayName("Un null en entrée ne fait plus crasher la collecte triée")
    void codesUniquesAcceptentNull() {
        FixedNullKey dedoublonnage = new FixedNullKey();

        // Arrays.asList ET pas List.of : celle-ci refuse null à la création même.
        assertThatCode(() -> dedoublonnage.codesUniques(Arrays.asList("B", null, "A")))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Codes uniques, triés, sans null")
    void codesUniquesExcluentNullEtTrient() {
        Set<String> codes = new FixedNullKey().codesUniques(Arrays.asList("B", null, "A", "B"));

        assertThat(new ArrayList<>(codes)).containsExactly("A", "B");
    }

    // ---- Piège 9 corrigé : removeIf sur copie -----------------------------

    @Test
    @DisplayName("removeIf ne saute aucun voisin : plus aucun pair ne survit")
    void retirerPairsNeLaisseAucunPair() {
        List<Integer> resultat = new FixedRemoveLoop()
                .retirerPairs(new ArrayList<>(List.of(1, 2, 4, 3, 6, 5)));

        assertThat(resultat).containsExactly(1, 3, 5);
    }

    @Test
    @DisplayName("Liste entièrement paire → résultat vide (et argument intact)")
    void retirerPairsSurListeEntierementPair() {
        List<Integer> entree = new ArrayList<>(List.of(2, 4, 6, 8));

        List<Integer> resultat = new FixedRemoveLoop().retirerPairs(entree);

        assertThat(resultat).isEmpty();
        assertThat(entree).containsExactly(2, 4, 6, 8);
    }

    // ---- Piège 10 corrigé : putAll / merge --------------------------------

    @Test
    @DisplayName("La fusion écrase bien les clés communes")
    void fusionEcraseLesClesCommunes() {
        Map<String, Double> cible = new HashMap<>(Map.of("A", 1.0));
        Map<String, Double> source = Map.of("A", 2.0);

        new FixedMapMerge().fusionner(cible, source);

        assertThat(cible.get("A")).isEqualTo(2.0);
    }

    @Test
    @DisplayName("Après fusion, la cible contient toute la source à jour")
    void fusionAppliqueTouteLaSource() {
        Map<String, Double> cible = new HashMap<>(Map.of("A", 1.0));
        Map<String, Double> source = Map.of("A", 2.0, "B", 3.0);

        new FixedMapMerge().fusionner(cible, source);

        assertThat(cible).containsExactlyInAnyOrderEntriesOf(Map.of("A", 2.0, "B", 3.0));
    }

    @Test
    @DisplayName("Variante merge explicite : les doublons s'additionnent (putAll ne le sait pas faire)")
    void fusionEnSommantAdditionneLesDoublons() {
        Map<String, Double> cible = new HashMap<>(Map.of("A", 1.0));
        Map<String, Double> source = Map.of("A", 2.0, "B", 3.0);

        new FixedMapMerge().fusionnerEnSommant(cible, source);

        assertThat(cible).containsExactlyInAnyOrderEntriesOf(Map.of("A", 3.0, "B", 3.0));
    }
}
