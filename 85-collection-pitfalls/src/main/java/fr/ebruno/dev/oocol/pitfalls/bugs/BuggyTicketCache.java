package fr.ebruno.dev.oocol.pitfalls.bugs;

import java.util.HashMap;
import java.util.Map;

/**
 * Cache de prix par arrêt — le piège de la <b>clé instable dans une HashMap</b>,
 * conséquence directe du piège n° 1 ({@link BuggyStation} sans {@code hashCode}).
 *
 * <p>Le cache stocke le prix sous une instance de {@code BuggyStation} ;
 * la lookup avec une instance « égale » (même code) échoue, car le hachage
 * d'{@code Object} place les deux clés dans des seaux différents. Et si la
 * clé était <em>mutable</em> et modifiée après le {@code put}, l'entrée
 * deviendrait définitivement injoignable — une clé de Map doit être immuable
 * ET avoir un {@code hashCode} cohérent avec {@code equals}.</p>
 *
 * @author Emmanuel Bruno
 */
public class BuggyTicketCache {

    private final Map<BuggyStation, Double> prixCache = new HashMap<>();

    /** Mémorise le prix d'un arrêt. */
    public void cache(BuggyStation station, double prix) {
        prixCache.put(station, prix);
    }

    /**
     * Retrouve le prix d'un arrêt.
     *
     * <p>BUG PEDAGOGIQUE (hérité de {@link BuggyStation}): renvoie
     * {@code null} pour une station « égale » à celle stockée — la clé n'a
     * pas de {@code hashCode} cohérent, le cache « manque » ses propres
     * entrées. (Corrigé dans fixed/FixedTicketCache.)</p>
     */
    public Double lookup(BuggyStation station) {
        return prixCache.get(station);
    }

    /** Nombre d'entrées du cache (deux clés égales ne doivent faire qu'une entrée). */
    public int taille() {
        return prixCache.size();
    }
}
