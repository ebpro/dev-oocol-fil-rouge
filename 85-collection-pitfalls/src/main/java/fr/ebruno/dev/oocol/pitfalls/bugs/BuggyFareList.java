package fr.ebruno.dev.oocol.pitfalls.bugs;

import java.util.Arrays;
import java.util.List;

/**
 * Grille tarifaire — le piège de <b>{@code Arrays.asList} : une liste de
 * taille fixe qui se fait passer pour une {@code List} mutable</b>.
 *
 * <p>{@code Arrays.asList(...)} renvoie une vue {@code java.util.Arrays$ArrayList}
 * <em>fixe</em> : {@code set} est autorisé, mais {@code add}/{@code remove}
 * lèvent {@code UnsupportedOperationException} — le type de retour
 * ({@code List}) ne le dit pas, et l'appelant l'apprend à l'exécution.
 * Issue : renvoyer {@code new ArrayList<>(List.of(...))} quand la liste
 * appelée doit être modifiable.</p>
 *
 * @author Emmanuel Bruno
 */
public class BuggyFareList {

    /**
     * Tarifs standard (unité, journée, abonnement).
     *
     * <p>BUG PEDAGOGIQUE: {@code Arrays.asList} = taille fixe ; tout
     * {@code add}/{@code remove} de l'appelant explose en
     * UnsupportedOperationException. (Corrigé dans fixed/FixedFareList.)</p>
     */
    public List<Double> tarifsStandard() {
        return Arrays.asList(2.0, 5.5, 12.0); // BUG PEDAGOGIQUE: vue de taille fixe déguisée en List mutable
    }
}
