package fr.ebruno.dev.oocol.transit.domain;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Le réseau Trans'U : l'agrégat racine du fil rouge — zones, arrêts, lignes.
 *
 * <p><strong>Pourquoi ce n'est PAS un record.</strong> La règle posée au
 * module 80 : « données pures et immuables → record ; état ou comportement
 * riche → classe ». {@code Zone}, {@code Station} et {@code Line} sont des
 * données pures : ce sont des records. {@code Network} est un <em>agrégat</em>
 * qui porte des requêtes métier ({@link #stationsAccessibles()},
 * {@link #lignesDe(Station)}, {@link #parZone()}) et une invariant de
 * construction trop riche pour un constructeur : il reste une classe —
 * immuable, tous champs {@code final}, listes copiées.
 *
 * <p><strong>Constructeur privé : le Builder n'est pas une décoration.</strong>
 * Construire un réseau valide exige d'avoir résolu les zones avant les
 * stations et les stations avant les lignes. Plutôt que d'exposer un
 * constructeur à trois listes que l'appelant pourrait passer dans un ordre
 * incohérent, le constructeur est <em>privé</em> : la seule porte d'entrée
 * publique est {@link #builder()}. La validité d'un {@code Network} obtenu
 * par {@code new} n'a plus à être redoutée — elle est garantie par la
 * compilation.
 *
 * <p><strong>DIP (vu au module 90, vécu ici).</strong> Cette classe ne
 * connaît ni JDBC, ni CSV, ni la console : aucun import {@code java.sql},
 * aucun {@code System.out}. Elle ne dépend que de son propre domaine. Les
 * couches de persistance et de présentation dépendent d'elle — jamais
 * l'inverse.
 *
 * @author Emmanuel Bruno
 * @see NetworkBuilder
 */
public final class Network {

    private final List<Zone> zones;
    private final List<Station> stations;
    private final List<Line> lignes;

    /**
     * Construction légitime : uniquement appelée par {@link #couler}, la
     * fabrique package-private que {@link NetworkBuilder#construire()} est
     * seule à actionner.
     *
     * <p>Privée volontaire — passer par {@link Network#builder()} est la seule
     * voie publique. Les listes sont copiées ({@link List#copyOf}) pour que le
     * Builder puisse continuer à vivre (réutilisation, snapshots successifs)
     * sans que le réseau construit bouge avec lui.
     *
     * @param zones    les zones tarifaires
     * @param stations les arrêts
     * @param lignes   les lignes
     */
    private Network(List<Zone> zones, List<Station> stations, List<Line> lignes) {
        this.zones = List.copyOf(zones);
        this.stations = List.copyOf(stations);
        this.lignes = List.copyOf(lignes);
    }

    /**
     * Seule porte d'entrée publique vers la construction d'un réseau.
     *
     * @return un Builder neuf, à composer en chaînant les appels
     */
    public static NetworkBuilder builder() {
        return new NetworkBuilder();
    }

    /**
     * Fabrique de réseau, visibilité PAQUET — le seul endroit qui peut appeler
     * le constructeur privé, et donc le seul accessible à
     * {@link NetworkBuilder#construire()}.
     *
     * <p>Pourquoi une fabrique et non un constructeur package-private ? Le
     * mot-clé {@code private} protège une CLASSE, pas un paquet :
     * {@code NetworkBuilder}, classe voisine du même paquet, ne peut pas
     * invoquer {@code new Network(...)}. {@code couler} est le « trou de
     * serrure » refermé sur le paquet {@code domain} : hors de lui, la voie
     * publique unique reste {@code builder()…construire()} — le test par
     * réflexion ({@code SolidRetrospectiveTest}) vérifie d'ailleurs que le
     * CONSTRUCTEUR, lui, est bien privé.
     *
     * <p>Le verbe est celui du moule : le Builder est un moule, chaque appel
     * coule une pièce neuve — les listes sont copiées, le moule survit.
     *
     * @param zones    les zones tarifaires
     * @param stations les arrêts (zones déjà résolues)
     * @param lignes   les lignes (arrêts déjà résolues)
     * @return un {@code Network} immuable
     */
    static Network couler(List<Zone> zones, List<Station> stations, List<Line> lignes) {
        return new Network(zones, stations, lignes);
    }

    /**
     * Les zones, dans l'ordre de construction.
     *
     * @return liste immuable (jamais {@code null})
     */
    public List<Zone> zones() {
        return zones;
    }

    /**
     * Les arrêts, dans l'ordre de construction.
     *
     * @return liste immuable (jamais {@code null})
     */
    public List<Station> stations() {
        return stations;
    }

    /**
     * Les lignes, dans l'ordre de construction.
     *
     * @return liste immuable (jamais {@code null})
     */
    public List<Line> lignes() {
        return lignes;
    }

    /**
     * L'arrêt d'un code donné, s'il existe.
     *
     * <p>SRP : cette méthode ne fait qu'une seule chose — chercher. La
     * présentation du résultat (« trouvé » vs « Introuvable ») est dans
     * {@code TransitCli}, pas ici.
     *
     * <p>Module 50 vs module 80 : le retour est un {@link Optional}, pas
     * {@code null} — l'absence devient typée et l'appelant ne peut pas
     * l'ignorer par inattention.
     *
     * @param code le code d'arrêt (ex. {@code "GAR"})
     * @return l'arrêt, ou {@link Optional#empty()} si aucun code ne correspond
     */
    public Optional<Station> station(String code) {
        return stations.stream().filter(s -> s.code().equals(code)).findFirst();
    }

    /**
     * La ligne d'un nom donné, si elle existe.
     *
     * @param nom le nom de ligne (ex. {@code "Ligne A"})
     * @return la ligne, ou {@link Optional#empty()}
     */
    public Optional<Line> ligne(String nom) {
        return lignes.stream().filter(l -> l.nom().equals(nom)).findFirst();
    }

    /**
     * Les arrêts accessibles aux personnes à mobilité réduite.
     *
     * <p>SRP : cette méthode ne fait qu'une seule chose — filtrer. La
     * présentation est dans {@code TransitCli}.
     *
     * <p>Module 80 : un {@code filter}/{@code toList} remplace la boucle avec accumulateur des modules 40 à 60.
     *
     * @return les arrêts dont le composant {@code accessible} est vrai
     */
    public List<Station> stationsAccessibles() {
        return stations.stream().filter(Station::accessible).toList();
    }

    /**
     * Les arrêts regroupés par zone tarifaire.
     *
     * <p>SRP : regrouper, rien d'autre. La mise en page du groupement
     * (en-têtes, totaux par zone) appartient à la CLI.
     *
     * @return map zone → arrêts de cette zone (les listes suivent l'ordre d'insertion)
     */
    public Map<Zone, List<Station>> parZone() {
        return stations.stream().collect(Collectors.groupingBy(Station::zone));
    }

    /**
     * Les lignes qui desservent un arrêt donné.
     *
     * <p>{@code List#contains} compare par {@code equals} (principe du module 40,
     * verrouillée par l'égalité structurelle du record) : une autre instance
     * de {@code "GAR"} suffit à trouver la ligne.
     *
     * @param s l'arrêt interrogé
     * @return les lignes dont {@code s} est un arrêt (vide si aucune)
     */
    public List<Line> lignesDe(Station s) {
        return lignes.stream().filter(l -> l.arrets().contains(s)).toList();
    }

    /**
     * Résumé d'une ligne : le compte du réseau, en text block (module 80).
     *
     * <p>SRP : ce résumé est un fil de sécurité pour le debugger ; les
     * affichages destinés aux voyageurs sont dans {@code TransitCli}, pas ici.
     *
     * @return ex. {@code "Réseau Trans'U — 3 zones, 6 stations, 3 lignes"}
     */
    @Override
    public String toString() {
        return """
                Réseau Trans'U — %d zones, %d stations, %d lignes\
                """.formatted(zones.size(), stations.size(), lignes.size());
    }
}
