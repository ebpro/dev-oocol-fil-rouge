package fr.ebruno.dev.oocol.pitfalls.fixed;

import java.util.ArrayList;
import java.util.List;

/**
 * La version <b>corrigée</b> de {@link
 * fr.ebruno.dev.oocol.pitfalls.bugs.BuggyFareList} : la grille tarifaire
 * renvoyée est une vraie {@code ArrayList} modifiable.
 *
 * <p>{@code new ArrayList<>(List.of(...))} : les valeurs de départ tiennent
 * en une ligne, et le contrat de taille fixe d'{@code List.of} est éliminé
 * par la copie. Règle : le type de retour {@code List} doit correspondre à
 * ce que l'appelant a le droit de faire — si la liste appelée doit grandir,
 * on renvoie une liste qui grandit.</p>
 *
 * @author Emmanuel Bruno
 */
public class FixedFareList {

    /**
     * Tarifs standard (unité, journée, abonnement).
     *
     * <p>FIX: copie modifiable — {@code add}/{@code remove} côté appelant
     * fonctionnent sans surprise.</p>
     */
    public List<Double> tarifsStandard() {
        return new ArrayList<>(List.of(2.0, 5.5, 12.0));
    }
}
