package fr.ebruno.dev.oocol.pitfalls.fixed;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * La version <b>corrigée</b> de {@link
 * fr.ebruno.dev.oocol.pitfalls.bugs.BuggyMapMerge} : la fusion utilise
 * {@code putAll} — les valeurs de la source écrasent bien celles de la cible
 * sur les clés communes.
 *
 * <p>Quand la politique n'est pas « la source gagne » mais « combiner »,
 * {@code Map.merge(key, valeur, remapping)} est l'outil explicite. Le piège
 * n'était pas une faute de frappe : {@code putIfAbsent} est un {@code put}
 * <em>conditionnel</em> — l'utiliser pour fusionner, c'est croire à une mise
 * à jour que le code ne fait jamais.</p>
 *
 * @author Emmanuel Bruno
 */
public class FixedMapMerge {

    /**
     * Agrège des valeurs par code d'arrêt (identique à la version buggy —
     * le piège n'a jamais été là). En code idiomatique, l'équivalent stream :
     * {@code codes.stream().collect(groupingBy(...))}.
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
     * priment).
     *
     * <p>FIX: {@code putAll} — toutes les clés de la source atterrissent dans
     * la cible, les communes sont écrasées.</p>
     */
    public void fusionner(Map<String, Double> cible, Map<String, Double> source) {
        cible.putAll(source);
    }

    /**
     * Variante « politique de combinaison » : additionne les doublons plutôt
     * que les écraser — ce que {@code put}/{@code putAll} ne savent pas faire.
     */
    public void fusionnerEnSommant(Map<String, Double> cible, Map<String, Double> source) {
        source.forEach((cle, valeur) -> cible.merge(cle, valeur, Double::sum));
    }
}
