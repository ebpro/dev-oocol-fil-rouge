package fr.ebruno.dev.oocol.pitfalls.fixed;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

/**
 * La version <b>corrigée</b> de {@link
 * fr.ebruno.dev.oocol.pitfalls.bugs.BuggyNullKey} : les {@code null} sont
 * filtrés avant l'insertion dans le {@code TreeSet}.
 *
 * <p>{@code TreeSet} trie par {@code compareTo} : {@code null} n'a pas de
 * {@code compareTo}, l'insérer est une NPE garantie. Le fix assume la
 * sémantique métier — « codes uniques triés » n'a jamais voulu dire « le
 * null est un code » — et écarte explicitement les valeurs absentes.
 * Alternative : {@code HashSet} tolère un {@code null} mais perd l'ordre.</p>
 *
 * @author Emmanuel Bruno
 */
public class FixedNullKey {

    /**
     * Codes uniques, triés, sans {@code null}.
     *
     * <p>FIX: {@code filter(Objects::nonNull)} avant collecte — la présence
     * d'un {@code null} en entrée est un bruit de données, plus un crash.</p>
     */
    public Set<String> codesUniques(List<String> codes) {
        return codes.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(TreeSet::new));
    }
}
