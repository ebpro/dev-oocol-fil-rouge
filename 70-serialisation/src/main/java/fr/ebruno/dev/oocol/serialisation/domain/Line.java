package fr.ebruno.dev.oocol.serialisation.domain;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Une ligne du réseau : un nom et la liste ordonnée de ses arrêts.
 *
 * <p><strong>La leçon cachée de cette classe : la sérialisation est
 * transitiv.</strong> {@code Line} est {@code Serializable} ; elle sérialise
 * donc aussi tout ce que sa liste contient. Deux conséquences démontrées en
 * tests :
 * <ul>
 *   <li>la liste elle-même doit être d'une implémentation sérialisable —
 *     {@code ArrayList}, {@code Vector}, les vues {@code unmodifiableList}
 *     le sont toutes (contre-intuitif mais vrai) ; en revanche
 *     {@code List.of(...)} et {@code Arrays.asList(...)} deviennent
 *     explosifs dès qu'un <em>élément</em> ne l'est pas — c'est le contenu,
 *     pas le conteneur, qui casse le contrat ;</li>
 *   <li>les {@link Station} voyagent avec leurs {@link Zone} : le graphe
 *     d'objets est écrit en entier, partagés compris (un objet référencé
 *     deux fois n'est écrit qu'une fois — la JVM suit les références).</li>
 * </ul>
 *
 * <p>Le champ est déclaré en {@code List} mais initialisé en
 * {@code ArrayList} : le type d'interface pour la souplesse, l'implémentation
 * choisie en connaissance de cause sérialisable.
 *
 * @author Emmanuel Bruno
 * @see Station
 * @see NetworkSnapshot
 */
public class Line implements Serializable {

    /** Version du format de la ligne. */
    private static final long serialVersionUID = 1L;

    /** Nom de la ligne (ex. {@code "Ligne A"}). */
    private String nom;

    /**
     * Arrêts dans l'ordre de parcours. Implémentation {@code ArrayList} :
     * sérialisable, contrairement à ce qu'on entend souvent.
     */
    private List<Station> arrets;

    /**
     * Crée une ligne vide (sans arrêt).
     *
     * @param nom nom de la ligne, non vide
     * @throws IllegalArgumentException si le nom est vide
     */
    public Line(String nom) {
        if (nom == null || nom.isBlank()) {
            throw new IllegalArgumentException("Le nom de ligne est obligatoire.");
        }
        this.nom = nom;
        this.arrets = new ArrayList<>();
    }

    /**
     * Ajoute un arrêt en bout de ligne.
     *
     * @param arret l'arrêt à ajouter, obligatoire
     * @throws IllegalArgumentException si l'arrêt est {@code null}
     */
    public void addStation(Station arret) {
        if (arret == null) {
            throw new IllegalArgumentException("Impossible d'ajouter un arrêt nul.");
        }
        arrets.add(arret);
    }

    /**
     * @return le nom de la ligne
     */
    public String getNom() {
        return nom;
    }

    /**
     * @return les arrêts dans l'ordre, en vue non modifiable (contrat W3 repris)
     */
    public List<Station> getStations() {
        return Collections.unmodifiableList(arrets);
    }

    /**
     * Représentation lisible : {@code "Ligne A (3 arrêts)"}.
     *
     * @return le nom et le nombre d'arrêts
     */
    @Override
    public String toString() {
        return nom + " (" + arrets.size() + " arrêts)";
    }
}
