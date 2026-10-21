package fr.ebruno.dev.oocol.exceptions.domain;

import java.util.HashMap;
import java.util.Map;

/**
 * Le barème de l'opérateur : un prix par zone traversée, le titre facturée
 * étant le <em>plus grand</em> tarif des zones couvertes par le trajet.
 *
 * <p><strong>Instantané W6 — le second registre unchecked.</strong> Avec
 * {@link StationNotFoundException}, cette classe forme la famille des
 * <em>bugs</em> : un tarif négatif dans la table, une zone traversée sans
 * tarif enregistré, ce sont des incohérences du <em>code qui construit le
 * barème</em> — aucune borne de vente honnête ne peut « rattraper » un prix
 * négatif, elle doit mourir bruyamment et faire rougir la CI. D'où
 * {@link FareCalculationException}, unchecked, absente des signatures.
 *
 * <p>La méthode {@link #calculerPrixEntre(Station, Station)} est aussi le
 * point de rencontre des deux familles : elle valide ses arguments via
 * {@link Station#requireValide(Station, String)} — donc peut propager une
 * {@link StationNotFoundException} sans l'écrire dans sa signature — puis
 * parcourt le barème, où l'attend sa propre {@link FareCalculationException}.
 * Une seule méthode, deux unchecked distinctes, rattrapables ensemble par
 * un multi-catch : c'est la scène 6 de la démo.
 *
 * @author Emmanuel Bruno
 * @see FareCalculationException
 * @see Station#requireValide(Station, String)
 */
public class FareCalculator {

    /**
     * Table du barème : numéro de zone → tarif en euros.
     *
     * <p>{@code HashMap} (et pas {@code TreeMap}) : la recherche est en
     * {@code get}, l'ordre d'itération n'a aucun sens ici. Choix assumé,
     * hérité du module 40.
     */
    private final Map<Integer, Double> tarifsParZone = new HashMap<Integer, Double>();

    /**
     * Barème vide : à garnir avec {@link #enregistrerTarif(int, double)}.
     */
    public FareCalculator() {
        // table volontairement vide ; un calcul avant enregistrement est une
        // incohérence d'initialisation → FareCalculationException en pratique.
    }

    /**
     * Publie le tarif d'une zone du réseau.
     *
     * <p><strong>Le tarif négatif est un bug, pas une donnée métier.</strong>
     * Un prix négatif signifierait « on rembourse l'usager pour voyager » :
     * ça n'existe pas dans le domaine. Le refuser ici par
     * {@link FareCalculationException} — unchecked — plutôt que par une
     * valeur de retour, c'est le message de la séance appliqué au barème :
     * qui écrit {@code enregistrerTarif(2, -1.5)} a <em>un code faux</em>,
     * et rien en aval ne pourra rattraper cet appel.
     *
     * @param numeroZone numéro de zone, strictement positif
     * @param tarif      prix du titre dans cette zone, positif ou nul
     * @throws IllegalArgumentException    si le numéro de zone est invalide
     *                                    (paramètre aberrant — bug appelant classique)
     * @throws FareCalculationException   si le tarif est négatif
     *                                    (incohérence de barème — unchecked par nature)
     */
    public void enregistrerTarif(int numeroZone, double tarif) {
        if (numeroZone <= 0) {
            throw new IllegalArgumentException("Numéro de zone invalide : " + numeroZone);
        }
        if (tarif < 0) {
            throw new FareCalculationException(
                    "Tarif négatif pour la zone " + numeroZone + " : " + tarif
                            + " — un prix négatif est une incohérence du barème, pas une donnée métier.");
        }
        tarifsParZone.put(Integer.valueOf(numeroZone), Double.valueOf(tarif));
    }

    /**
     * Prix d'un trajet entre deux stations : le plus grand tarif des zones
     * traversées (du point le plus proche du centre au plus lointain).
     *
     * <p>Contrat W6 : les deux stations sont validées par
     * {@link Station#requireValide(Station, String)} — une {@code null} en
     * entrée propage une {@link StationNotFoundException} typée, immédiate
     * et localisée, au lieu d'une {@code NullPointerException} différée.
     * Ensuite chaque zone survolée doit avoir son tarif : une zone sans
     * barème est une incohérence de construction du calculateur, traitée en
     * {@link FareCalculationException}.
     *
     * @param depart station de montée (jamais {@code null} — {@code requireValide} veille)
     * @param arrivee station de descente (jamais {@code null})
     * @return le prix du trajet, le plus grand tarif des zones traversées
     * @throws StationNotFoundException  si {@code depart} ou {@code arrivee} est {@code null}
     * @throws FareCalculationException  si une zone traversée n'a pas de tarif enregistré
     */
    public double calculerPrixEntre(Station depart, Station arrivee) {
        Station d = Station.requireValide(depart, "calcul de tarif (départ)");
        Station a = Station.requireValide(arrivee, "calcul de tarif (arrivée)");

        Zone zd = d.getZone();
        Zone za = a.getZone();
        if (zd == null || za == null) {
            // Inatteignable par le constructeur de Station (il exige une zone) :
            // garde défensive contre un futur setter non validé — incohérence
            // de domaine → unchecked, pas un plan B checked.
            throw new FareCalculationException(
                    "Station sans zone tarifaire : " + d.getCode() + " / " + a.getCode());
        }

        int min = Math.min(zd.getNumero(), za.getNumero());
        int max = Math.max(zd.getNumero(), za.getNumero());

        double prix = 0.0;
        for (int z = min; z <= max; z++) {
            Double tarif = tarifsParZone.get(Integer.valueOf(z));
            if (tarif == null) {
                throw new FareCalculationException(
                        "Aucun tarif enregistré pour la zone " + z
                                + " (trajet " + d.getCode() + " \u2192 " + a.getCode() + ") — barème incomplet.");
            }
            if (tarif.doubleValue() > prix) {
                prix = tarif.doubleValue();
            }
        }
        return prix;
    }

    /**
     * Le barème connaît-il cette zone ?
     *
     * @param numeroZone numéro de zone testé
     * @return {@code true} si un tarif est enregistré pour cette zone
     */
    public boolean connaitZone(int numeroZone) {
        return tarifsParZone.containsKey(Integer.valueOf(numeroZone));
    }

    /**
     * @return le nombre de zones couvertes par le barème
     */
    public int nombreZones() {
        return tarifsParZone.size();
    }
}
