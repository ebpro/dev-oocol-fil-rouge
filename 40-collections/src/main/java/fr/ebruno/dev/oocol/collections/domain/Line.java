package fr.ebruno.dev.oocol.collections.domain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Une ligne du réseau : une suite <em>ordonnée</em> d'arrêts.
 *
 * <p><strong>La paire de champs devient une liste.</strong> Dans le module
 * {@code 20-domain-bases}, {@code Line} ne reliait que deux stations — deux
 * champs fixes, {@code origine} et {@code destination} — une limite assumée.
 * Elle est levée ici : la paire est remplacée par une
 * {@code List<Station>}. Pourquoi une {@code List} précisément ? Parce
 * qu'un itinéraire de bus EST une séquence : l'ordre a un sens (le terminus
 * d'aller n'est pas le terminus de retour), les doublons sont refusés, le
 * accès par position ({@code get(i)}) est demandé par l'exploitation.
 * {@code Set} aurait perdu l'ordre ; {@code Map} n'a rien à indexer ici.
 * « Bon conteneur pour bon usage » : la règle de choix d'un conteneur.
 *
 * <p>La classe met en scène le contrat {@code equals}/{@code hashCode} de
 * {@link Station} là où il fait mal s'il manque : {@code contains},
 * {@code remove} et {@code indexOf} comparent des <em>objets</em>, pas des
 * références. Sans le contrat, {@link #retirerArret(Station)} échouerait en
 * silence sur un arrêt pourtant présent — {@link #demontrerBugSansEquals()}
 * rejoue la scène complète, résultats à l'appui.
 *
 * <p>Limite affichée :
 * {@link #trouverParCode(String)} parcourt la liste à la main — coût
 * {@code O(n)}. Le graphe {@link Network} qui suit montre la réponse
 * {@code O(1)} d'une {@code Map} ; la leçon n'est pas « List est mauvaise »
 * mais « chaque conteneur a son usage ».
 *
 * @author Emmanuel Bruno
 * @see Station
 * @see Network
 */
public class Line {

    /** Nom commercial de la ligne (ex. {@code "Ligne A"}). */
    private String nom;

    /**
     * Les arrêts, dans l'ordre de parcours.
     *
     * <p>Le champ est initialisé ici, une fois pour toutes : une {@code Line}
     * vivante a toujours une liste (vide au départ), jamais {@code null} —
     * l'alternative (liste nullable) polluerait chaque appelant d'un test.
     */
    private final List<Station> arrets = new ArrayList<>();

    /**
     * Crée une ligne sans arrêt : on la garnit ensuite avec
     * {@link #ajouterArret(Station)}, dans l'ordre voulu.
     *
     * @param nom nom commercial de la ligne, non vide
     * @throws IllegalArgumentException si le nom est {@code null} ou blanc
     */
    public Line(String nom) {
        setNom(nom);
    }

    /**
     * Ajoute un arrêt en fin de parcours.
     *
     * <p>La garde « pas deux fois le même arrêt » appelle
     * {@code arrets.contains(s)} — donc {@code Station.equals}. Pour
     * l'empêchement, on a besoin de {@code equals()} → d'où le contrat de
     * {@link Station} : sans lui, un arrêt {@code "GAR"} ajouté deux fois
     * sous deux instances différentes passerait inaperçu.
     *
     * @param s l'arrêt à ajouter, non {@code null}
     * @return {@code true} si l'arrêt a été ajouté, {@code false} s'il était déjà présent
     * @throws IllegalArgumentException si l'arrêt est {@code null}
     */
    public boolean ajouterArret(Station s) {
        if (s == null) {
            throw new IllegalArgumentException("Impossible d'ajouter un arrêt null à la ligne " + nom);
        }
        if (arrets.contains(s)) {
            return false; // déjà desservie (comparaison par equals, pas par référence)
        }
        return arrets.add(s);
    }

    /**
     * Retire un arrêt par identité métier (même code = même station).
     *
     * <p>{@code List.remove(Object)} cherche l'élément avec
     * {@code equals} : une <em>autre instance</em> de {@code "GAR"} retire
     * bien la station {@code "GAR"} de la ligne. Sans le contrat, ce serait
     * {@code false} — et personne ne le saurait.
     *
     * @param s l'arrêt à retirer (par code)
     * @return {@code true} si un arrêt a été retiré, {@code false} si absent (sémantique List)
     */
    public boolean retirerArret(Station s) {
        if (s == null) {
            return false; // rien à retirer : la liste ne contient jamais de null
        }
        return arrets.remove(s);
    }

    /**
     * Retrouve un arrêt depuis son code d'exploitation.
     *
     * <p>Parcours linéaire : à chaque appel on peut devoir lire toute la
     * liste — coût {@code O(n)}. C'est VOLONTAIREMENT simple : le lookup
     * manuel montre ici sa limite, et {@link Network} introduit dans la foulée
     * la {@code Map} qui le fait en {@code O(1)}. Comparer les deux, c'est
     * comprendre le choix du conteneur.
     *
     * @param code le code cherché (ex. {@code "GAR"})
     * @return la station correspondante, ou {@code null} si la ligne ne la dessert pas
     */
    public Station trouverParCode(String code) {
        for (Station s : arrets) { // for-each : sucre syntaxique autour de l'Iterator — pas un lambda
            if (s.getCode().equals(code)) {
                return s;
            }
        }
        return null; // absence = null, contrat documenté de la méthode
    }

    /**
     * @return le nombre d'arrêts de la ligne
     */
    public int nombreArrets() {
        return arrets.size();
    }

    /**
     * @return le premier arrêt du parcours (tête de la liste)
     * @throws IllegalStateException si la ligne n'a encore aucun arrêt
     */
    public Station premierArret() {
        if (arrets.isEmpty()) {
            // Échec BRUYANT : demander le premier arrêt d'une ligne vide est
            // une erreur du programme, pas une réponse métier (« null » ici
            // se propagerait trop loin).
            throw new IllegalStateException("La ligne " + nom + " n'a aucun arrêt.");
        }
        return arrets.get(0);
    }

    /**
     * @return le dernier arrêt du parcours (queue de la liste)
     * @throws IllegalStateException si la ligne n'a encore aucun arrêt
     */
    public Station dernierArret() {
        if (arrets.isEmpty()) {
            throw new IllegalStateException("La ligne " + nom + " n'a aucun arrêt.");
        }
        return arrets.get(arrets.size() - 1);
    }

    /**
     * Une station est-elle un terminus de cette ligne ?
     *
     * <p>Comparaison par {@code equals} : on teste l'identité métier (le
     * code), pas la référence — un exemplaire different de {@code "GAR"}
     * reconnu comme terminus, voilà ce que le contrat rend possible.
     *
     * @param s la station à tester
     * @return {@code true} si c'est le premier ou le dernier arrêt
     */
    public boolean estTerminus(Station s) {
        if (s == null || arrets.isEmpty()) {
            return false;
        }
        return s.equals(arrets.get(0)) || s.equals(arrets.get(arrets.size() - 1));
    }

    /**
     * @return le nom commercial de la ligne
     */
    public String getNom() {
        return nom;
    }

    /**
     * Change le nom de la ligne.
     *
     * @param nom le nouveau nom, non vide
     * @throws IllegalArgumentException si le nom est {@code null} ou blanc
     */
    public void setNom(String nom) {
        if (nom == null || nom.isBlank()) {
            throw new IllegalArgumentException("Le nom d'une ligne est obligatoire.");
        }
        this.nom = nom;
    }

    /**
     * Les arrêts, dans l'ordre de parcours — en <strong>lecture seule</strong>.
     *
     * <p>{@code Collections.unmodifiableList} renvoie une VUE qui refuse
     * toute écriture : le monde extérieur lit la liste, seul
     * {@link #ajouterArret(Station)} et {@link #retirerArret(Station)} la
     * modifient — l'encapsulation appliquée à une collection.
     * Tenter {@code getArrets().add(...)} lève une
     * {@code UnsupportedOperationException} : échec bruyant, pas corruption
     * silencieuse.
     *
     * @return une vue non modifiable des arrêts
     */
    public List<Station> getArrets() {
        return Collections.unmodifiableList(arrets);
    }

    /**
     * Démonstration : le bug qui motive le contrat {@code equals}/
     * {@code hashCode}.
     *
     * <p>Ajoute une station, puis la recherche et la retire via une AUTRE
     * instance de même code. Résultat affiché : les deux opérations
     * <strong>réussissent</strong> — parce que {@link Station} a un
     * {@code equals} par code. Version cassée (la station sans contrat,
     * {@code equals} d{@code Object} par référence) :
     *
     * <pre>
     * // Si Station N'IMPLÉMENTAIT PAS equals/hashCode :
     * //   arrets.contains(copie)  → false  (l'objet « n'existe pas » !)
     * //   arrets.remove(copie)    → false  (retrait silencieusement raté)
     * // Aucune exception, juste de fausses réponses : le pire des bugs.
     * </pre>
     *
     * @return un petit rapport de démonstration, prêt à imprimer
     */
    public static String demontrerBugSansEquals() {
        Zone zoneCentre = new Zone(1, "Centre-ville");
        Line ligneTest = new Line("Ligne démo");
        Station gare = Station.of("GAR", "Gare Centrale", zoneCentre);
        ligneTest.ajouterArret(gare);

        // Même code, autre instance, autre nom : « la même » station vu le métier.
        Station copie = Station.of("GAR", "Gare Centrale (re-créée)", zoneCentre);

        StringBuilder rapport = new StringBuilder();
        rapport.append("contains(copie de même code) : ").append(ligneTest.getArrets().contains(copie)).append('\n');
        rapport.append("retirerArret(copie)         : ").append(ligneTest.retirerArret(copie)).append('\n');
        rapport.append("Les deux réussissent GRÂCE à Station.equals/hashCode (par code). ");
        rapport.append("Sans eux, Object.equals compare les références : les deux afficheraient ");
        rapport.append("false — un lookup qui rate silencieusement. C'est le bug W5 motivateur.");
        return rapport.toString();
    }

    /**
     * Représentation lisible : {@code "Ligne A [GAR → UNI → HOP] (3 arrêts)"}.
     *
     * <p>Les codes (pas les objets) sont affichés : un toString de collection
     * lisible reste court — la classe {@code toString} des stations est
     * réservée au debug unitaire.
     *
     * @return le libellé de la ligne avec son parcours
     */
    @Override
    public String toString() {
        StringBuilder libelle = new StringBuilder(nom).append(" [");
        for (int i = 0; i < arrets.size(); i++) {
            if (i > 0) {
                libelle.append(" \u2192 "); // → : la flèche passe mieux en escape Unicode
            }
            libelle.append(arrets.get(i).getCode());
        }
        libelle.append("] (").append(arrets.size()).append(" arr\u00EAt");
        if (arrets.size() > 1) {
            libelle.append('s');
        }
        return libelle.append(')').toString();
    }
}
