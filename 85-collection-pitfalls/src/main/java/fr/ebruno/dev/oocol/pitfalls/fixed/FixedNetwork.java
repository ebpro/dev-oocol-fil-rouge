package fr.ebruno.dev.oocol.pitfalls.fixed;

import java.util.ArrayList;
import java.util.List;

/**
 * La version <b>corrigée</b> de {@link fr.ebruno.dev.oocol.pitfalls.bugs.BuggyNetwork} :
 * le retrait de lignes utilise {@code removeIf}, plus de
 * {@code ConcurrentModificationException}.
 *
 * <p>{@code List.removeIf(predicate)} parcourt et retire dans la même
 * opération, sous contrôle de la liste elle-même : l'invariant de
 * l'itération n'est jamais violé. C'est l'issue idiomatique en Java moderne —
 * {@code Iterator.remove()} reste valable quand le prédicat a besoin de
 * l'index ou d'effets de bord.</p>
 *
 * @author Emmanuel Bruno
 */
public class FixedNetwork {

    private final List<FixedLine> lignes = new ArrayList<>();

    /** Ajoute une ligne au réseau. */
    public void ajouterLigne(FixedLine ligne) {
        lignes.add(ligne);
    }

    /** Vue instantanée des lignes du réseau. */
    public List<FixedLine> getLignes() {
        return List.copyOf(lignes);
    }

    /**
     * Retire toutes les lignes qui ne desservent pas l'arrêt {@code code}.
     *
     * <p>FIX: {@code removeIf} — suppression sans itérateur concurrent,
     * toutes les lignes concernées sont retirées en un seul passage.</p>
     */
    public void retirerLignesSansCorrespondance(String code) {
        lignes.removeIf(l -> l.getArrets().stream()
                .noneMatch(s -> s.getCode().equals(code)));
    }
}
