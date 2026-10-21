package fr.ebruno.dev.oocol.pitfalls.fixed;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * La version <b>corrigée</b> de {@link
 * fr.ebruno.dev.oocol.pitfalls.bugs.BuggyGrouping} : les zones sont des
 * {@link FixedStation} et le {@code Set.contains} répond enfin juste.
 *
 * <p>Aucune magie : le code de la méthode est identique à celui du buggy —
 * c'est la <em>clé</em> qui a changé. Message pédagogique central : quand
 * une opération de collection donne un résultat faux sans exception,
 * soupçonne {@code equals}/{@code hashCode} des éléments, pas l'algorithme.</p>
 *
 * @author Emmanuel Bruno
 */
public class FixedGrouping {

    /**
     * La station {@code s} appartient-elle à l'une des zones listées ?
     *
     * <p>FIX: {@link FixedStation} a un {@code hashCode} cohérent avec
     * {@code equals} → une instance « égale » à une membre est retrouvée.</p>
     */
    public boolean estDansZone(List<FixedStation> zones, FixedStation s) {
        Set<FixedStation> set = new HashSet<>(zones);
        return set.contains(s);
    }
}
