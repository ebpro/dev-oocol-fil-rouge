package fr.ebruno.dev.oocol.transit.domain;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Constructeur pas-à-pas du réseau — la démonstration centrale de l'ELO 13.
 *
 * <p><strong>Le problème que le Builder résout.</strong> {@code Network} est
 * un agrégat : trois listes qui doivent être cohérentes entre elles (toute
 * zone référencée par une station doit exister, tout code cité par une ligne
 * doit désigner un arrêt connu). Un constructeur
 * {@code new Network(zone1, zone2, ..., station1, station2, ..., ligne1, ...)}
 * à vingt paramètres — ou à trois listes que l'appelant remplit lui-même —
 * déplace cette cohérence SUR l'appelant : chaque site de construction
 * redevient responsable de l'invariant, et l'erreur d'ordre ou d'oubli
 * redevient possible.
 *
 * <p><strong>La réponse du Builder.</strong> Au lieu de cela, on écrit du
 * code qui se lit comme une phrase :
 * <pre>{@code
 * Network r = Network.builder()
 *         .zone(1, "Centre-ville")
 *         .station("GAR", "Gare Centrale", 1, true)
 *         .ligne("Ligne A", "GAR")
 *         .construire();
 * }</pre>
 * Chaque appel fluent ajoute UNE brique ; {@link #construire()} est le seul
 * moment où la cohérence globale est vérifiée (fail-fast : la première
 * référence cassée lève une {@link NetworkException} qui NOMME le coupable),
 * et le seul qui rende un {@code Network} immuable.
 *
 * <p><strong>OCP — on peut étendre le Builder sans modifier {@code Network}.</strong>
 * Demain une composante {@code couleur} par ligne ? On ajoute une méthode
 * {@code couleur(...)} ici ; {@code Network} et tous ses appelants existants
 * restent intacts tant qu'ils n'utilisent pas la nouveauté. Le Builder est
 * le point d'extension, le produit reste fermé.
 *
 * <p><strong>ISP — le Builder n'expose que les méthodes de construction, pas
 * les requêtes.</strong> {@code Network} porte {@code stationsAccessibles()},
 * {@code parZone()}… le Builder ne les redéclare pas : qui assemble n'a pas
 * à interroger, qui interroge n'a plus à assembler. Deux interfaces petites
 * et honnêtes plutôt qu'un {@code BigNetwork} qui fait tout.
 *
 * <p><strong>Sémantique de snapshot.</strong> {@link #construire()} ne vide
 * PAS le Builder : deux appels successifs sans modification intermédiaire
 * produisent des réseaux équivalents ; continuer d'ajouter après un
 * {@code construire()} produit un réseau enrichi sans toucher au précédent
 * (les listes sont copiées à la construction — cf. module 85, fuites
 * mutables). Le Builder est un <em>moule</em>, pas un stock : on peut couler
 * plusieurs pièces dans le même moule.
 *
 * @author Emmanuel Bruno
 * @see Network#builder()
 */
public final class NetworkBuilder {

    /**
     * Une station déclarée mais pas encore résolue : le numéro de zone sera
     * remplacé par la vraie instance {@link Zone} au moment de construire.
     */
    private record StationSpec(String code, String nom, int zoneNumero, boolean accessible) {
    }

    /** Une ligne déclarée par ses codes, pas encore résolue en {@link Station}. */
    private record LineSpec(String nom, List<String> codes) {
    }

    private final List<Zone> zones = new ArrayList<>();
    private final List<StationSpec> stations = new ArrayList<>();
    private final List<LineSpec> lignes = new ArrayList<>();

    /**
     * Déclare une zone tarifaire.
     *
     * <p>Validation immédiate (fail-fast) : le compact constructeur de
     * {@link Zone} rejette un numéro invalide DES CET APPEL, pas au
     * {@link #construire()} — la ligne fautive reste visible dans la stack.
     *
     * @param numero identifiant positif de la zone
     * @param nom    libellé de la zone
     * @return ce Builder, pour chaîner
     * @throws IllegalArgumentException si la zone est invalide
     */
    public NetworkBuilder zone(int numero, String nom) {
        zones.add(new Zone(numero, nom));
        return this;
    }

    /**
     * Déclare un arrêt rattaché à la zone de numéro {@code zoneNumero}.
     *
     * <p>Le lien est exprimé par NUMÉRO (une donnée plate, facile à écrire et
     * à sérialiser en CSV/SQL) et résolu en véritable instance {@link Zone}
     * uniquement dans {@link #construire()} — c'est le travail du Builder que
     * de convertir des références paresseuses en objet cohérent.
     *
     * @param code        identifiant court de l'arrêt (unique dans le réseau)
     * @param nom         libellé complet
     * @param zoneNumero  la zone de rattachement, doit avoir été déclarée avant {@link #construire()}
     * @param accessible  vrai si l'arrêt est PMR-accessible
     * @return ce Builder, pour chaîner
     * @throws IllegalArgumentException si le code ou le nom est vide
     */
    public NetworkBuilder station(String code, String nom, int zoneNumero, boolean accessible) {
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("code de station requis");
        }
        if (nom == null || nom.isBlank()) {
            throw new IllegalArgumentException("nom de station requis pour " + code);
        }
        stations.add(new StationSpec(code, nom, zoneNumero, accessible));
        return this;
    }

    /**
     * Déclare une ligne : un nom et ses arrêts dans l'ordre de parcours.
     *
     * @param nom          identifiant de la ligne (unique)
     * @param codesStations les codes des arrêts desservis, dans l'ordre
     * @return ce Builder, pour chaîner
     * @throws IllegalArgumentException si le nom est vide
     */
    public NetworkBuilder ligne(String nom, String... codesStations) {
        if (nom == null || nom.isBlank()) {
            throw new IllegalArgumentException("nom de ligne requis");
        }
        lignes.add(new LineSpec(nom, List.of(codesStations)));
        return this;
    }

    /**
     * Vérifie la cohérence globale et coule le réseau immuable.
     *
     * <p>Trois passes, dans l'ordre des dépendances référentielles (le même
     * argument qu'au module 60) : index des zones par numéro, puis résolution
     * des stations (zone inconnue ou code dupliqué = échec), puis résolution
     * des lignes (code inconnu ou nom dupliqué = échec). Le premier défaut
     * lève une {@link NetworkException} dont le message nomme l'élément fautif :
     * fail-fast, pas de réseau « presque valide » en circulation.
     *
     * <p>Le Builder n'est PAS réinitialisé : chaque appel à {@code construire()}
     * photographie l'état du moment (voir la sémantique de snapshot, en tête
     * de classe).
     *
     * @return un {@code Network} immuable, validé de bout en bout
     * @throws NetworkException si une référence est inconnue ou un identifiant dupliqué
     */
    public Network construire() throws NetworkException {
        // Passe 1 — les zones : numéro → instance, doublon refusé.
        Map<Integer, Zone> zonesParNumero = new HashMap<>();
        for (Zone z : zones) {
            if (zonesParNumero.put(z.numero(), z) != null) {
                throw new NetworkException("Zone dupliquée : le numéro " + z.numero()
                        + " est déclaré deux fois");
            }
        }

        // Passe 2 — les stations : le numéro devient une référence, le code doit être unique.
        Map<String, Station> stationsParCode = new HashMap<>();
        List<Station> resolues = new ArrayList<>();
        for (StationSpec spec : stations) {
            Zone zone = zonesParNumero.get(spec.zoneNumero());
            if (zone == null) {
                throw new NetworkException("Arrêt \u00AB " + spec.code() + " \u00BB référencant "
                        + "une zone inconnue (numéro " + spec.zoneNumero() + ")");
            }
            Station s = new Station(spec.code(), spec.nom(), zone, spec.accessible());
            if (stationsParCode.put(s.code(), s) != null) {
                throw new NetworkException("Arrêt dupliqué : le code " + s.code()
                        + " est déclaré deux fois");
            }
            resolues.add(s);
        }

        // Passe 3 — les lignes : les codes deviennent des arrêts, dans l'ordre cité.
        List<Line> lignesResolues = new ArrayList<>();
        Map<String, Line> lignesParNom = new HashMap<>();
        for (LineSpec spec : lignes) {
            List<Station> arrets = new ArrayList<>();
            for (String code : spec.codes()) {
                Station s = stationsParCode.get(code);
                if (s == null) {
                    throw new NetworkException("Ligne \u00AB " + spec.nom()
                            + " \u00BB référençant un arrêt inconnu (code " + code + ")");
                }
                arrets.add(s);
            }
            Line l = new Line(spec.nom(), arrets);
            if (lignesParNom.put(l.nom(), l) != null) {
                throw new NetworkException("Ligne dupliquée : le nom \u00AB " + l.nom()
                        + " \u00BB est déclaré deux fois");
            }
            lignesResolues.add(l);
        }

        return Network.couler(List.copyOf(zones), List.copyOf(resolues), List.copyOf(lignesResolues));
    }
}
