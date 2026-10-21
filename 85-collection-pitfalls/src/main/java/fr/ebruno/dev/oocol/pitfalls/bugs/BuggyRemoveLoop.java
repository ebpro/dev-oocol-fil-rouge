package fr.ebruno.dev.oocol.pitfalls.bugs;

import java.util.List;

/**
 * Nettoyage d'une liste de comptages — le piège de la <b>suppression par index
 * dans une boucle {@code for} croissante</b> : chaque retrait fait glisser les
 * éléments suivants vers la gauche, et l'incrément {@code i++} saute le
 * voisin.
 *
 * <p>{@code nombres.remove(i)} décale la fin de la liste d'un cran ; le
 * {@code i++} qui suit passe directement à l'élément <em>d'après</em> celui
 * qui a pris la place. Deux éléments pairs consécutifs : le deuxième survit.
 * Pas d'exception, juste un résultat faux et difficile à repérer. Issues :
 * {@code removeIf}, itérateur explicite, ou boucle décroissante.</p>
 *
 * @author Emmanuel Bruno
 */
public class BuggyRemoveLoop {

    /**
     * Retire les nombres pairs de la liste (la modifie en place).
     *
     * <p>BUG PEDAGOGIQUE: {@code remove(i)} dans un {@code for} croissant →
     * l'élément qui suit immédiatement un retrait est sauté et survit.
     * (Corrigé dans fixed/FixedRemoveLoop : {@code removeIf} sur copie.)</p>
     */
    public List<Integer> retirerPairs(List<Integer> nombres) {
        for (int i = 0; i < nombres.size(); i++) {
            if (nombres.get(i) % 2 == 0) {
                nombres.remove(i); // BUG PEDAGOGIQUE: décale la liste, puis i++ saute le voisin
            }
        }
        return nombres;
    }
}
