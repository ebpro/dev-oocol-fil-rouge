package fr.ebruno.dev.oocol.pitfalls.bugs;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Tri alphabétique des arrêts — le piège du <b>comparateur incohérent</b> :
 * un {@code compare} qui renvoie {@code 1} au lieu de {@code 0} quand deux
 * éléments sont égaux.
 *
 * <p>Le contrat d'{@code Comparator} exige {@code compare(a,b) == 0} pour des
 * éléments égaux (et l'antisymétrie : {@code sgn(compare(a,b)) ==
 * -sgn(compare(b,a))}). Un « égal » déclaré « strictement plus grand » viole
 * les deux : sur de petits triages, l'ordre est préservé par chance, mais
 * TimSort finit par lever {@code IllegalArgumentException: Comparison method
 * violates its general contract!}, et toute stabilité du tri devient
 * fortuite. Les tests de détection attrapent la violation sur le comparateur
 * lui-même, qui est exposé ici volontairement.</p>
 *
 * @author Emmanuel Bruno
 */
public class BuggySortStable {

    /**
     * BUG PEDAGOGIQUE: {@code return 1} quand les noms sont égaux — le
     * comparateur prétend que tout est strictement ordonné alors qu'il existe
     * des égaux : antisymétrie et transitivité violées.
     * (Corrigé dans fixed/FixedSortStable : {@code return 0}.)
     */
    public Comparator<BuggyStation> comparateurParNom() {
        return (a, b) -> {
            int cmp = a.getNom().compareTo(b.getNom());
            if (cmp == 0) {
                return 1; // BUG PEDAGOGIQUE: « égal » doit rendre 0, jamais 1
            }
            return cmp;
        };
    }

    /** Trie les arrêts par nom (copie défensive : la liste source n'est pas modifiée). */
    public List<BuggyStation> trierParNom(List<BuggyStation> stations) {
        List<BuggyStation> copie = new ArrayList<>(stations);
        copie.sort(comparateurParNom());
        return copie;
    }
}
