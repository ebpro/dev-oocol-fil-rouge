package fr.ebruno.dev.oocol.pitfalls.bugs;

import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * Dédoublonnage trié des codes d'arrêts — le piège du <b>{@code null} dans un
 * {@code TreeSet}</b>.
 *
 * <p>{@code TreeSet} ordonne ses éléments : y insérer {@code null} exige un
 * {@code compareTo} sur {@code null} → {@code NullPointerException}. Un CSV
 * importé avec une cellule vide, une liste filtrée à la trame : un seul
 * {@code null} et toute la collecte explose. Issues : filtrer les {@code null}
 * en amont, ou accepter le désordre et utiliser un {@code HashSet} (qui,
 * lui, tolère un {@code null}).</p>
 *
 * @author Emmanuel Bruno
 */
public class BuggyNullKey {

    /**
     * Codes uniques, triés.
     *
     * <p>BUG PEDAGOGIQUE: {@code new TreeSet<>(codes)} propage chaque élément
     * à {@code compare} — le moindre {@code null} de la liste d'entrée lève un
     * NPE. (Corrigé dans fixed/FixedNullKey : filtrage des {@code null}.)</p>
     */
    public Set<String> codesUniques(List<String> codes) {
        return new TreeSet<>(codes); // BUG PEDAGOGIQUE: null toléré en entrée, pas par TreeSet
    }
}
