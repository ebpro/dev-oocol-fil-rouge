package fr.ebruno.dev.oocol.pitfalls.fixed;

import java.util.ArrayList;
import java.util.List;

/**
 * La version <b>corrigée</b> de {@link
 * fr.ebruno.dev.oocol.pitfalls.bugs.BuggyRemoveLoop} : {@code removeIf} sur
 * une copie — plus aucun élément sauté, plus aucune mutation surprise de
 * l'argument.
 *
 * <p>Double correction : (1) structurelle, {@code removeIf(predicate)} fait
 * le tri en un passage interne, sans index à maintenir ; (2) contractuelle,
 * la méthode ne modifie plus la liste reçue — elle renvoie un nouveau
 * résultat, conformément à l'attente raisonnable d'un appelant qui tient sa
 * liste d'entrée.</p>
 *
 * @author Emmanuel Bruno
 */
public class FixedRemoveLoop {

    /**
     * Retourne une copie de {@code nombres} sans les valeurs paires
     * (l'argument n'est pas modifié).
     *
     * <p>FIX: {@code removeIf} — le prédicat décide, la liste gère elle-même
     * sa structure ; l'« élément d'après » n'a plus jamais l'occasion d'être
     * sauté.</p>
     */
    public List<Integer> retirerPairs(List<Integer> nombres) {
        List<Integer> copie = new ArrayList<>(nombres);
        copie.removeIf(n -> n % 2 == 0);
        return copie;
    }
}
