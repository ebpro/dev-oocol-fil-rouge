package fr.ebruno.dev.oocol.pitfalls.bugs;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Fusion de grilles tarifaires — le piège du <b>{@code putIfAbsent} utilisé
 * comme un {@code put}</b> : la croyance répandue que « fusionner » veut dire
 * « mettre si absent », alors que {@code putIfAbsent} ne fait RIEN quand la
 * clé existe déjà.
 *
 * <p>Fusionner une grille de mise à jour dans la grille courante doit
 * <em>écraser</em> les valeurs communes (la source a raison). Avec
 * {@code putIfAbsent}, les clés déjà présentes gardent leur valeur périmée :
 * aucune exception, aucune trace — la fusion « réussit » à moitié. Issues :
 * {@code putAll}, ou {@code merge} pour une politique de combinaison explicite.</p>
 *
 * @author Emmanuel Bruno
 */
public class BuggyMapMerge {

    /**
     * Agrège des valeurs par code d'arrêt.
     *
     * <p>Cette méthode est <em>correcte</em> — elle sert de point de contrôle
     * dans la chasse aux bugs : tous les pièges de la classe sont dans
     * {@link #mergeIncorrect}. Le {@code get}-puis-{@code put} est verbeux
     * mais juste en contexte mono-thread (en streams, {@code Collectors.groupingBy}
     * fait mieux ; en concurrence, {@code computeIfAbsent} sur une
     * {@code ConcurrentHashMap}).</p>
     */
    public Map<String, List<Double>> aggregator(List<String> codes, List<Double> valeurs) {
        Map<String, List<Double>> result = new HashMap<>();
        for (int i = 0; i < codes.size(); i++) {
            String key = codes.get(i);
            List<Double> list = result.get(key);
            if (list == null) {
                list = new ArrayList<>();
            }
            list.add(valeurs.get(i));
            result.put(key, list);
        }
        return result;
    }

    /**
     * Fusionne {@code source} dans {@code cible} (les valeurs de {@code source}
     * doivent primer).
     *
     * <p>BUG PEDAGOGIQUE: {@code putIfAbsent} n'écrase pas les clés déjà
     * présentes — la cible garde ses valeurs périmées sur les clés communes.
     * (Corrigé dans fixed/FixedMapMerge : {@code putAll}/{@code merge}.)</p>
     */
    public void mergeIncorrect(Map<String, Double> cible, Map<String, Double> source) {
        for (var e : source.entrySet()) {
            cible.putIfAbsent(e.getKey(), e.getValue()); // BUG PEDAGOGIQUE: putIfAbsent != put — les clés communes ne sont jamais mises à jour
        }
    }
}
