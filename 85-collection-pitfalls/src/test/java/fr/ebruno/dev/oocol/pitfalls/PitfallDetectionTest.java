package fr.ebruno.dev.oocol.pitfalls;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import fr.ebruno.dev.oocol.pitfalls.bugs.BuggyFareList;
import fr.ebruno.dev.oocol.pitfalls.bugs.BuggyGrouping;
import fr.ebruno.dev.oocol.pitfalls.bugs.BuggyLine;
import fr.ebruno.dev.oocol.pitfalls.bugs.BuggyMapMerge;
import fr.ebruno.dev.oocol.pitfalls.bugs.BuggyNetwork;
import fr.ebruno.dev.oocol.pitfalls.bugs.BuggyNullKey;
import fr.ebruno.dev.oocol.pitfalls.bugs.BuggyRemoveLoop;
import fr.ebruno.dev.oocol.pitfalls.bugs.BuggySortStable;
import fr.ebruno.dev.oocol.pitfalls.bugs.BuggyStation;
import fr.ebruno.dev.oocol.pitfalls.bugs.BuggyTicketCache;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Chasse aux bugs — chaque test affirme le comportement <em>correct</em> que
 * la classe {@code bugs.*} correspondante ne rend pas. Tels quels, ces tests
 * ÉCHOUENT : c'est voulu. Désactive-les un à un (retire l'annotation
 * {@code @Disabled}), lis l'échec, diagnostique le piège, corrige la classe
 * buggée, re-exécutez — la correction est bonne quand le test passe.
 *
 * <p>Le miroir {@link PitfallFixedTest} joue les mêmes assertions contre le
 * paquet {@code fixed.*} et doit rester vert : c'est lui qui prouve que les
 * attentes sont les bonnes.</p>
 *
 * @author Emmanuel Bruno
 */
class PitfallDetectionTest {

    // Deux instances « égales » (même code) : le couple de test de tous les pièges à base de hachage.
    private static final BuggyStation GAR1 = new BuggyStation("GAR", "Gare Centrale");
    private static final BuggyStation GAR2 = new BuggyStation("GAR", "Gare Neuve");
    private static final BuggyStation UNI1 = new BuggyStation("UNI", "Universite");
    private static final BuggyStation UNI2 = new BuggyStation("UNI", "Campus");
    private static final BuggyStation HOP1 = new BuggyStation("HOP", "Hopital");
    private static final BuggyStation HOP2 = new BuggyStation("HOP", "CH");

    // Deux stations de noms différents mais égaux entre elles : fixtures du comparateur.
    private static final BuggyStation MUSEE_GAR = new BuggyStation("GAR", "Musee");
    private static final BuggyStation MUSEE_ARC = new BuggyStation("ARC", "Musee");

    private static BuggyNetwork reseauAvec(String... lignesEtCodes) {
        // lignesEtCodes : paires (nomLigne, codeArret) — ex. "U", "UNI", "V", "GAR".
        BuggyNetwork reseau = new BuggyNetwork();
        for (int i = 0; i < lignesEtCodes.length; i += 2) {
            BuggyLine ligne = new BuggyLine(lignesEtCodes[i]);
            ligne.ajouterArret(new BuggyStation(lignesEtCodes[i + 1], "Arret " + lignesEtCodes[i + 1]));
            reseau.ajouterLigne(ligne);
        }
        return reseau;
    }

    // ---- Piège 1 : equals sans hashCode (BuggyStation) -------------------

    @Test
    @Disabled("Étape W11 — décommentez après correction")
    @DisplayName("equals sans hashCode : deux stations égales doivent avoir le même hashCode (contrat Object)")
    void stationsEgalesMemeHashCode() {
        assertThat(GAR1).isEqualTo(GAR2); // passe : equals est défini…
        assertThat(GAR1.hashCode()).isEqualTo(GAR2.hashCode()); // …mais le hashCode suit l'identité
    }

    @Test
    @Disabled("Étape W11 — décommentez après correction")
    @DisplayName("HashMap perd la valeur : get avec une clé « égale » renvoie null")
    void hashMapRetrouveCleEgale() {
        Map<BuggyStation, Double> carte = new HashMap<>();
        carte.put(GAR1, 2.0);

        assertThat(carte.get(GAR2)).isEqualTo(2.0);
    }

    @Test
    @Disabled("Étape W11 — décommentez après correction")
    @DisplayName("Le HashSet devrait dédupliquer les stations égales — il garde les 3 doublons")
    void hashSetContientStationEgale() {
        Set<BuggyStation> arrets = new HashSet<>();
        arrets.add(GAR1);
        arrets.add(GAR2);
        arrets.add(new BuggyStation("GAR", "Gare du Nord"));

        // Attendu : taille 1 (les trois instances sont « égales »).
        // Rendu : taille 3 — l'échec « expected 1 but was 3 » EST la preuve des doublons.
        // Avec trois instances distinctes, une collision fortuite de seaux ne suffit
        // plus à faire passer le test (1/16 par paire perdue, ~1/256 pour les deux).
        assertThat(arrets).hasSize(1);
    }

    @Test
    @Disabled("Étape W11 — décommentez après correction")
    @DisplayName("Deux put avec clés égales créent DEUX entrées : la Map se duplique")
    void putAvecCleEgaleNeDupliquePas() {
        Map<BuggyStation, Double> carte = new HashMap<>();
        carte.put(GAR1, 2.0);
        carte.put(GAR2, 2.5);

        assertThat(carte).hasSize(1);
        assertThat(carte.get(GAR1)).isEqualTo(2.5);
    }

    // ---- Piège 2 : référence mutable qui fuit (BuggyLine) ----------------

    @Test
    @Disabled("Étape W11 — décommentez après correction")
    @DisplayName("Fuite mutable : getArrets() devrait refuser la modification externe (UnsupportedOperationException)")
    void getterDeLigneDoitEtreImmuable() {
        BuggyLine ligne = new BuggyLine("A");
        ligne.ajouterArret(GAR1);

        assertThatThrownBy(() -> ligne.getArrets().add(UNI1))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    @Disabled("Étape W11 — décommentez après correction")
    @DisplayName("Ajout externe via le getter : l'intérieur de la ligne ne doit pas changer")
    void ajoutExterneNAtteintPasLInterne() {
        BuggyLine ligne = new BuggyLine("A");
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
    @Disabled("Étape W11 — décommentez après correction")
    @DisplayName("Vue capturée : une liste obtenue avant un ajout interne ne doit pas le voir")
    void vueCaptureeFigee() {
        BuggyLine ligne = new BuggyLine("A");
        ligne.ajouterArret(GAR1);
        List<BuggyStation> vue = ligne.getArrets();

        ligne.ajouterArret(UNI1);

        assertThat(vue).hasSize(1);
    }

    // ---- Piège 3 : ConcurrentModificationException (BuggyNetwork) --------

    @Test
    @Disabled("Étape W11 — décommentez après correction")
    @DisplayName("CME : retirer pendant le for-each lève ConcurrentModificationException")
    void retirerSansCorrespondanceNeLevePas() {
        BuggyNetwork reseau = reseauAvec("U", "UNI", "V", "GAR", "W", "HOP");

        assertThatCode(() -> reseau.retirerLignesSansCorrespondance("XXX"))
                .doesNotThrowAnyException();
    }

    @Test
    @Disabled("Étape W11 — décommentez après correction")
    @DisplayName("CME bis : seules les lignes desservant l'arrêt doivent survivre")
    void retirerNeGardeQueLesLignesDesservies() {
        BuggyNetwork reseau = reseauAvec("U", "UNI", "V", "GAR", "W", "GAR");

        reseau.retirerLignesSansCorrespondance("GAR");

        assertThat(reseau.getLignes()).extracting(BuggyLine::getNom)
                .containsExactlyInAnyOrder("V", "W");
    }

    @Test
    @Disabled("Étape W11 — décommentez après correction")
    @DisplayName("CME ter : aucune ligne ne desservant l'arrêt, le réseau doit se vider sans exploser")
    void retirerQuandRienNeCorrespond() {
        BuggyNetwork reseau = reseauAvec("U", "UNI", "V", "GAR", "W", "HOP");

        reseau.retirerLignesSansCorrespondance("XXX");

        assertThat(reseau.getLignes()).isEmpty();
    }

    // ---- Piège 4 : clé instable dans le cache (BuggyTicketCache) ---------

    @Test
    @Disabled("Étape W11 — décommentez après correction")
    @DisplayName("Cache manqué : lookup avec une station égale à la clé stockée doit retrouver le prix")
    void lookupAvecStationEgale() {
        BuggyTicketCache cache = new BuggyTicketCache();
        cache.cache(GAR1, 2.0);

        assertThat(cache.lookup(GAR2)).isEqualTo(2.0);
    }

    @Test
    @Disabled("Étape W11 — décommentez après correction")
    @DisplayName("Cache doublon : deux clés égales ne doivent occuper qu'une seule entrée")
    void deuxClesEgalesUneEntree() {
        BuggyTicketCache cache = new BuggyTicketCache();
        cache.cache(GAR1, 2.0);
        cache.cache(GAR2, 2.5);

        assertThat(cache.taille()).isEqualTo(1);
    }

    // ---- Piège 5 : Arrays.asList taille fixe (BuggyFareList) -------------

    @Test
    @Disabled("Étape W11 — décommentez après correction")
    @DisplayName("Arrays.asList déguisé : add sur la liste renvoyée ne doit pas lever UnsupportedOperationException")
    void tarifsAcceptentAjout() {
        List<Double> tarifs = new BuggyFareList().tarifsStandard();

        assertThatCode(() -> tarifs.add(20.0)).doesNotThrowAnyException();
    }

    @Test
    @Disabled("Étape W11 — décommentez après correction")
    @DisplayName("Taille attendue : après un ajout légitime, la grille tarifaire doit grandir")
    void tarifsGrandissentApresAjout() {
        List<Double> tarifs = new BuggyFareList().tarifsStandard();

        tarifs.add(20.0);

        assertThat(tarifs).containsExactly(2.0, 5.5, 12.0, 20.0);
    }

    @Test
    @Disabled("Étape W11 — décommentez après correction")
    @DisplayName("Arrays.asList bis : remove sur la liste renvoyée ne doit pas lever non plus")
    void tarifsAcceptentRetrait() {
        List<Double> tarifs = new BuggyFareList().tarifsStandard();

        assertThatCode(() -> tarifs.remove(Double.valueOf(5.5))).doesNotThrowAnyException();
        assertThat(tarifs).containsExactly(2.0, 12.0);
    }

    // ---- Piège 6 : Set.contains silencieux (BuggyGrouping) ---------------

    @Test
    @Disabled("Étape W11 — décommentez après correction")
    @DisplayName("Appartenance fausse : estDansZone doit reconnaître une station égale à un membre")
    void estDansZoneReconnaitEgal() {
        List<BuggyStation> zone = List.of(GAR1, UNI1);

        assertThat(new BuggyGrouping().estDansZone(zone, GAR2)).isTrue();
    }

    @Test
    @Disabled("Étape W11 — décommentez après correction")
    @DisplayName("Appartenance bis : chaque station égale à un membre de la zone doit être reconnue")
    void estDansZoneReconnaitChaqueMembre() {
        List<BuggyStation> zone = List.of(GAR1, UNI1, HOP1);
        BuggyGrouping grouping = new BuggyGrouping();

        boolean toutesReconnues = grouping.estDansZone(zone, GAR2)
                && grouping.estDansZone(zone, UNI2)
                && grouping.estDansZone(zone, HOP2);

        assertThat(toutesReconnues).isTrue();
    }

    // ---- Piège 7 : comparateur incohérent (BuggySortStable) --------------

    @Test
    @Disabled("Étape W11 — décommentez après correction")
    @DisplayName("Contrat Comparator : deux noms égaux doivent rendre 0, jamais 1")
    void comparateurRendZeroSurEgaux() {
        assertThat(new BuggySortStable().comparateurParNom().compare(MUSEE_GAR, MUSEE_ARC))
                .isZero();
    }

    @Test
    @Disabled("Étape W11 — décommentez après correction")
    @DisplayName("Antisymétrie : sgn(compare(a,b)) doit valoir -sgn(compare(b,a)) — ici 1 vs 1")
    void comparateurAntisymetrique() {
        var c = new BuggySortStable().comparateurParNom();

        assertThat(Integer.signum(c.compare(MUSEE_GAR, MUSEE_ARC)))
                .isEqualTo(-Integer.signum(c.compare(MUSEE_ARC, MUSEE_GAR)));
    }

    @Test
    @Disabled("Étape W11 — décommentez après correction")
    @DisplayName("Réflexivité : un élément doit se comparer égal à lui-même (compare(a,a) == 0)")
    void comparateurReflexif() {
        assertThat(new BuggySortStable().comparateurParNom().compare(MUSEE_GAR, MUSEE_GAR))
                .isZero();
    }

    // ---- Piège 8 : null dans un TreeSet (BuggyNullKey) -------------------

    @Test
    @Disabled("Étape W11 — décommentez après correction")
    @DisplayName("TreeSet + null : un code manquant dans la liste d'entrée ne doit pas faire crasher la collecte")
    void codesUniquesAcceptentNull() {
        BuggyNullKey dedoublonnage = new BuggyNullKey();

        // Arrays.asList ET pas List.of : celle-ci refuse null à la création même.
        assertThatCode(() -> dedoublonnage.codesUniques(Arrays.asList("B", null, "A")))
                .doesNotThrowAnyException();
    }

    @Test
    @Disabled("Étape W11 — décommentez après correction")
    @DisplayName("Résultat attendu : codes uniques, triés, sans null")
    void codesUniquesExcluentNullEtTrient() {
        Set<String> codes = new BuggyNullKey().codesUniques(Arrays.asList("B", null, "A", "B"));

        assertThat(new ArrayList<>(codes)).containsExactly("A", "B");
    }

    // ---- Piège 9 : suppression par index dans for croissant (BuggyRemoveLoop)

    @Test
    @Disabled("Étape W11 — décommentez après correction")
    @DisplayName("Index skip : après un retrait, l'élément voisin est sauté — 4 survit à [1,2,4,3,6,5]")
    void retirerPairsNeLaisseAucunPair() {
        List<Integer> resultat = new BuggyRemoveLoop()
                .retirerPairs(new ArrayList<>(List.of(1, 2, 4, 3, 6, 5)));

        assertThat(resultat).containsExactly(1, 3, 5);
    }

    @Test
    @Disabled("Étape W11 — décommentez après correction")
    @DisplayName("Index skip bis : liste entièrement paire → moitié des pairs survivent")
    void retirerPairsSurListeEntierementPair() {
        List<Integer> resultat = new BuggyRemoveLoop()
                .retirerPairs(new ArrayList<>(List.of(2, 4, 6, 8)));

        assertThat(resultat).isEmpty();
    }

    // ---- Piège 10 : putIfAbsent cru pour un put (BuggyMapMerge) ----------

    @Test
    @Disabled("Étape W11 — décommentez après correction")
    @DisplayName("Fusion qui n'écrase pas : putIfAbsent laisse la valeur périmée sur les clés communes")
    void fusionEcraseLesClesCommunes() {
        Map<String, Double> cible = new HashMap<>(Map.of("A", 1.0));
        Map<String, Double> source = Map.of("A", 2.0);

        new BuggyMapMerge().mergeIncorrect(cible, source);

        assertThat(cible.get("A")).isEqualTo(2.0);
    }

    @Test
    @Disabled("Étape W11 — décommentez après correction")
    @DisplayName("Fusion complète : après fusion, la cible doit contenir toute la source, valeurs à jour")
    void fusionAppliqueTouteLaSource() {
        Map<String, Double> cible = new HashMap<>(Map.of("A", 1.0));
        Map<String, Double> source = Map.of("A", 2.0, "B", 3.0);

        new BuggyMapMerge().mergeIncorrect(cible, source);

        assertThat(cible).containsExactlyInAnyOrderEntriesOf(Map.of("A", 2.0, "B", 3.0));
    }
}
