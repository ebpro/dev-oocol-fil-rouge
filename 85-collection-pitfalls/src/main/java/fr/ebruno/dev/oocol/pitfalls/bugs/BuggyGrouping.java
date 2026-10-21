package fr.ebruno.dev.oocol.pitfalls.bugs;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Découpage du réseau en zones — le piège du <b>{@code Set.contains} silencieux</b> :
 * une question d'appartenance qui répond « faux » sans jamais lever d'exception.
 *
 * <p>{@code new HashSet<>(zones)} hache chaque {@link BuggyStation} avec le
 * {@code hashCode} hérité d'{@code Object} ; {@code contains(s)} hache la
 * requête — autre objet, autre seau → {@code false}, même quand la station
 * est « égale » à une membre de la zone. Aucun plantage, juste un résultat
 * faux : le pire type de bug de collection.</p>
 *
 * @author Emmanuel Bruno
 */
public class BuggyGrouping {

    /**
     * La station {@code s} appartient-elle à l'une des zones listées ?
     *
     * <p>BUG PEDAGOGIQUE: {@code HashSet} + clé sans {@code hashCode} cohérent
     * → {@code contains} renvoie {@code false} pour une station égale à une
     * membre. (Corrigé dans fixed/FixedGrouping, avec {@link
     * fr.ebruno.dev.oocol.pitfalls.fixed.FixedStation}.)</p>
     */
    public boolean estDansZone(List<BuggyStation> zones, BuggyStation s) {
        Set<BuggyStation> set = new HashSet<>(zones);
        return set.contains(s); // BUG PEDAGOGIQUE: equals ok, hashCode absent → réponse fausse, sans bruit
    }
}
