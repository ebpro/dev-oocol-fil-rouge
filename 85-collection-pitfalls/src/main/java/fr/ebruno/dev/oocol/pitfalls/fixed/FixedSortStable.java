package fr.ebruno.dev.oocol.pitfalls.fixed;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * La version <b>corrigée</b> de {@link
 * fr.ebruno.dev.oocol.pitfalls.bugs.BuggySortStable} : le comparateur rend
 * {@code 0} quand les noms sont égaux.
 *
 * <p>{@code Comparator.comparing(FixedStation::getNom)} exprime exactement la
 * même chose que le lambda corrigé — référence de méthode plutôt que
 * comparateur manuel. Avec un {@code compare} fidèle au contrat, le tri est
 * cohérent, et sa stabilité (l'ordre d'origine des égaux est conservé,
 * garantie de TimSort) devient un fait contractuel plutôt qu'un accident.</p>
 *
 * @author Emmanuel Bruno
 */
public class FixedSortStable {

    /**
     * FIX: égalité de noms → {@code 0} ; antisymétrie et transitivité du
     * contrat {@code Comparator} sont respectées.
     */
    public Comparator<FixedStation> comparateurParNom() {
        return Comparator.comparing(FixedStation::getNom);
    }

    /** Trie les arrêts par nom (copie défensive : la liste source n'est pas modifiée). */
    public List<FixedStation> trierParNom(List<FixedStation> stations) {
        List<FixedStation> copie = new ArrayList<>(stations);
        copie.sort(comparateurParNom());
        return copie;
    }
}
