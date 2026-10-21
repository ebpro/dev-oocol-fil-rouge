package fr.ebruno.dev.oocol.modern.app;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.stream.Stream;

import fr.ebruno.dev.oocol.modern.domain.AnalyseReseau;
import fr.ebruno.dev.oocol.modern.domain.Bus;
import fr.ebruno.dev.oocol.modern.domain.Electrique;
import fr.ebruno.dev.oocol.modern.domain.Hydrogene;
import fr.ebruno.dev.oocol.modern.domain.Ligne;
import fr.ebruno.dev.oocol.modern.domain.MetroTrain;
import fr.ebruno.dev.oocol.modern.domain.Station;
import fr.ebruno.dev.oocol.modern.domain.Tarif;
import fr.ebruno.dev.oocol.modern.domain.TarifAbonnement;
import fr.ebruno.dev.oocol.modern.domain.TarifJour;
import fr.ebruno.dev.oocol.modern.domain.TarifUnite;
import fr.ebruno.dev.oocol.modern.domain.Tramway;
import fr.ebruno.dev.oocol.modern.domain.Vehicle;
import fr.ebruno.dev.oocol.modern.domain.VersionLegacy;
import fr.ebruno.dev.oocol.modern.domain.Zone;

/**
 * Les sept scènes du Java moderne, jouées sur le réseau Trans'U.
 *
 * <p><strong>Lecture.</strong> Chaque scène est une méthode privée autonome,
 * titrée, commentable séparément en TD. Le fil rouge de la démo tient en une
 * phrase : <em>le code de W3-W7 est verbeux et défensif, les mêmes opérations
 * en Java 21 tiennent en trois lignes — le diff est la leçon</em>. Les scènes
 * 4 à 6 jouent d'ailleurs le duo {@link AnalyseReseau} / {@link VersionLegacy}
 * côte à côte et impriment l'égalité des résultats.
 *
 * <p><strong>Zéro pollution.</strong> La scène 7 écrit un CSV dans un fichier
 * temporaire détruit dans son {@code finally} : la démo tourne aussi bien en
 * local que dans la CI.
 *
 * <p><strong>Scènes.</strong> 1 : records (plomberie générée, {@code var},
 * immutabilité) ; 2 : {@code sealed} + switch de motifs sur {@code Vehicle}
 * (exhaustivité vérifiée) ; 3 : pattern matching sur {@code Tarif} ; 4 :
 * pipelines Streams contre boucles W4-W6 ; 5 : {@code Optional} en chaîne,
 * adieu le {@code null} + {@code if} ; 6 : blocs de texte {@code """} ;
 * 7 : {@code Files.lines} en stream — déverrouillé ici, interdit au module 60.
 *
 * @author Emmanuel Bruno
 * @see fr.ebruno.dev.oocol.modern.domain
 */
public final class ModernJavaDemo {

    /** Pas d'instance : une classe à {@code main}, comme dans tous les modules. */
    private ModernJavaDemo() {
    }

    /**
     * Enchaîne les sept scènes.
     *
     * @param args ignorés
     * @throws IOException si le fichier temporaire de la scène 7 résiste
     */
    public static void main(String[] args) throws IOException {
        System.out.println("=== Module 80 : le réseau Trans'U à Java 21 (ELO 11) ===");
        scene1Records();
        scene2SealedEtSwitch();
        scene3TarifsPatternMatching();
        scene4StreamsContreBoucles();
        scene5Optional();
        scene6TextBlocks();
        scene7FilesLines();
        System.out.println();
        System.out.println("=== Fin de la d\u00e9mo : m\u00eame r\u00e9seau, m\u00eames r\u00e9sultats, dix fois moins de lignes. ===");
    }

    /** Entête de scène. */
    private static void scene(int numero, String titre) {
        System.out.println();
        System.out.println("=== Sc\u00e8ne " + numero + " \u2014 " + titre + " ===");
    }

    /** Scène 1 : le record remplace le POJO ; {@code var} quand le type est évident. */
    private static void scene1Records() {
        scene(1, "records \u2014 le POJO immuable en une ligne");

        // var : le type est lisible à droite du « = », l'écrire deux fois est du bruit.
        var zone = new Zone(1, "Centre");
        var gare = new Station("GAR", "Gare Centrale", zone, true);
        var uni = new Station("UNI", "Universite", new Zone(2, "Campus"), false);
        var ligneA = new Ligne("A", List.of(gare, uni));

        System.out.println("  toString g\u00e9n\u00e9r\u00e9 : " + zone);
        System.out.println("  accesseurs : numero() = " + zone.numero() + ", nom() = " + zone.nom());

        var jumeau = new Zone(1, "Centre");
        System.out.println("  equals par valeur : zone.equals(jumeau) = " + zone.equals(jumeau));
        System.out.println("  hashCode par valeur : " + (zone.hashCode() == jumeau.hashCode()));
        // Au module 3, equals/hashCode/toString faisaient 30 lignes recopiées. Ici : 0.

        System.out.println("  la ligne (toString imbriqué, gratuit) : " + ligneA);
    }

    /** Scène 2 : hiérarchie scellée + switch de motifs exhaustif sur Vehicle. */
    private static void scene2SealedEtSwitch() {
        scene(2, "sealed + pattern matching \u2014 le switch sans else pi\u00e8ge");

        List<Vehicle> parc = List.of(
                new MetroTrain("M-01", 160, 4, new Electrique(2400)),
                new Bus("B-12", 95, "A"),
                new Tramway("T-03", 200, new Hydrogene(350), 33.0));

        for (Vehicle v : parc) {
            // Le compilateur vérifie l'exhaustivité. Si on ajoute un Funiculaire à
            // « permits », ce code ne compile plus — c'est voulu : la compilation
            // tient le registre de tous les points d'impact. Pas de default, pas de cast.
            String rapport = switch (v) {
                case MetroTrain m -> "M\u00e9tro %d voitures".formatted(m.nombreVoitures());
                case Bus b -> "Bus ligne " + b.ligne();
                case Tramway t -> "Tram %.0f m".formatted(t.longueurMetres());
            };
            System.out.println("  " + v.decrire() + "  ->  " + rapport);
        }
    }

    /** Scène 3 : switch de motifs sur Tarif — la tarification comme décision centralisée. */
    private static void scene3TarifsPatternMatching() {
        scene(3, "pattern matching sur Tarif \u2014 une d\u00e9cision, pas une dispersion");

        List<Tarif> tarifs = List.of(
                new TarifUnite(new Zone(1, "Centre"), 1.90),
                new TarifJour(List.of(new Zone(1, "Centre"), new Zone(2, "Campus")), 4.50),
                new TarifAbonnement("Alice", LocalDate.of(2026, 10, 1), LocalDate.of(2027, 9, 30)));

        for (Tarif t : tarifs) {
            double prix = switch (t) {
                case TarifUnite tu -> tu.prix();
                case TarifJour tj -> tj.prix();
                // L'abonnement n'a pas de champ prix : le switch exhaustif FORCE la
                // décision (la valoriser), là où un default l'aurait noyée.
                case TarifAbonnement ta -> AnalyseReseau.PRIX_ABONNEMENT_MENSUEL;
            };
            System.out.println("  " + t.getClass().getSimpleName() + " -> " + prix + " EUR");
        }
    }

    /** Scène 4 : le même calcul en pipeline et en boucle — résultats identiques. */
    private static void scene4StreamsContreBoucles() {
        scene(4, "Streams contre VersionLegacy \u2014 identiques, 3x plus verbeux");

        var zone1 = new Zone(1, "Centre");
        var zone2 = new Zone(2, "Campus");
        var ligneA = new Ligne("A", List.of(
                new Station("GAR", "Gare Centrale", zone1, true),
                new Station("THE", "Theatre", zone1, false)));
        var ligneB = new Ligne("B", List.of(
                new Station("UNI", "Universite", zone2, true),
                new Station("HOP", "Hopital", zone2, true)));
        List<Ligne> reseau = List.of(ligneA, ligneB);

        List<Station> moderne = AnalyseReseau.stationsAccessibles(ligneB);
        List<Station> legacy = VersionLegacy.stationsAccessibles(ligneB);
        System.out.println("  accessibles (stream)  : " + moderne);
        System.out.println("  accessibles (boucle)  : " + legacy);
        System.out.println("  identiques : " + moderne.equals(legacy) + " \u2014 3 lignes vs 8 lignes");

        Map<Zone, List<Station>> groupage = AnalyseReseau.grouperParZone(reseau);
        System.out.println("  grouperParZone (flatMap + groupingBy) : " + groupage.size() + " zones");

        List<Tarif> tarifs = List.of(
                new TarifUnite(zone1, 1.90),
                new TarifJour(List.of(zone1, zone2), 4.50));
        OptionalDouble moyen = AnalyseReseau.prixMoyen(tarifs);
        System.out.println("  prixMoyen : " + moyen + " | legacy : " + VersionLegacy.prixMoyen(tarifs));

        // Tri : la classe interne anonyme du module 40 contre la référence de méthode.
        List<Station> aTrier = new ArrayList<>(List.of(ligneB.arrets().get(1), ligneB.arrets().get(0)));
        List<Station> triLegacy = VersionLegacy.trierParCode(aTrier);
        List<Station> triModerne = AnalyseReseau.trierParCode(aTrier);
        System.out.println("  tri\u00e9 identique : " + triLegacy.equals(triModerne)
                + " (Comparator anonyme vs .sorted() sur l'ordre naturel)");
        // équivalent λ direct, sans passer par le service :
        List<Station> triLambda = aTrier.stream().sorted(Comparator.comparing(Station::code)).toList();
        System.out.println("  tri par lambda : " + triLambda.equals(triModerne));
    }

    /** Scène 5 : Optional dans la signature — l'absence devient un fait de type. */
    private static void scene5Optional() {
        scene(5, "Optional \u2014 la fin du null + if");

        var zone = new Zone(1, "Centre");
        var ligne = new Ligne("A", List.of(new Station("GAR", "Gare Centrale", zone, true)));

        // Le W5-way : « Station trouver(...) » renvoyait null, et l'appelant
        // devinait — le NPE au bout du doigt. Ici l'absence est dans le type.
        Optional<Station> trouvee = ligne.trouverParCode("GAR");
        trouvee.map(Station::affichage)
                .ifPresent(libelle -> System.out.println("  trouv\u00e9 : " + libelle));

        ligne.trouverParCode("ZZZ").ifPresent(s -> System.out.println("  jamais imprim\u00e9"));
        System.out.println("  absent : Optional.empty() = " + ligne.trouverParCode("ZZZ").isEmpty());

        try {
            ligne.trouverParCode("ZZZ")
                    .orElseThrow(() -> new IllegalArgumentException("Arr\u00eat inconnu : ZZZ"));
        } catch (IllegalArgumentException e) {
            System.out.println("  orElseThrow -> " + e.getMessage());
        }
    }

    /** Scène 6 : le bloc de texte contre la concaténation à \n et \" . */
    private static void scene6TextBlocks() {
        scene(6, "text blocks \u2014 le multi-ligne sans \\\\n ni \\\\\"" );

        String avant = "== R\u00e9sum\u00e9 ==\n"
                + "  ligne avec \"guillemets\"\n";
        String apres = """
                == Résumé ==
                  ligne avec "guillemets"
                """;
        System.out.println("  concaténation vs bloc, identiques : " + avant.equals(apres));

        var zone = new Zone(1, "Centre");
        List<Ligne> reseau = List.of(new Ligne("A", List.of(
                new Station("GAR", "Gare Centrale", zone, true),
                new Station("THE", "Theatre", zone, false))));

        System.out.println("  --- resumerReseau (bloc de texte) ---");
        System.out.print(AnalyseReseau.resumerReseau(reseau));
        System.out.println("  octet pour octet égal au StringBuilder legacy : "
                + AnalyseReseau.resumerReseau(reseau).equals(VersionLegacy.resumerReseau(reseau)));
    }

    /** Scène 7 : Files.lines en stream — l'interdit du module 60, déverrouillé. */
    private static void scene7FilesLines() throws IOException {
        scene(7, "Files.lines \u2014 le CSV devient un flux d'objets");

        Path csv = Files.createTempFile("arrets", ".csv");
        try {
            Files.writeString(csv, """
                    code;nom;zone;accessible
                    GAR;Gare Centrale;1;true
                    UNI;Universite;2;false
                    """);

            // Au module 60 : while ((line = reader.readLine()) != null) + ArrayList + split
            // à la main. Ici Files.lines renvoie un Stream<String> — pipelet complet :
            List<Station> lues;
            try (Stream<String> flux = Files.lines(csv)) {
                lues = flux.skip(1)
                        .map(ligne -> ligne.split(";"))
                        .map(p -> new Station(p[0], p[1],
                                new Zone(Integer.parseInt(p[2]), "Zone " + p[2]),
                                Boolean.parseBoolean(p[3])))
                        .toList();
            }
            System.out.println("  relues depuis le CSV : " + lues.size() + " arr\u00eats");
            for (Station s : lues) {
                System.out.println("    " + s.affichage() + " [" + s.zone().nom() + "]");
            }
        } finally {
            Files.deleteIfExists(csv);
        }
    }
}
