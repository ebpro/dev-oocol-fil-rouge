package fr.ebruno.dev.oocol.pitfalls.bugs;

import java.util.ArrayList;
import java.util.List;

/**
 * Le réseau Trans'U complet — avec le piège du <b>{@code ConcurrentModificationException}</b> :
 * retirer un élément d'une liste pendant un {@code for}-each.
 *
 * <p>L'itérateur d'{@code ArrayList} enregistre le {@code modCount} à la création ;
 * le {@code List.remove(l)} appelé « par-dessus » l'itération le modifie sans
 * prévenir l'itérateur, qui lève {@code ConcurrentModificationException} au
 * {@code next()} suivant. Les issues : {@code Iterator.remove()} ou, mieux en
 * Java moderne, {@code List.removeIf(predicate)}.</p>
 *
 * @author Emmanuel Bruno
 */
public class BuggyNetwork {

    private final List<BuggyLine> lignes = new ArrayList<>();

    /** Ajoute une ligne au réseau. */
    public void ajouterLigne(BuggyLine ligne) {
        lignes.add(ligne);
    }

    /**
     * Vue instantanée des lignes du réseau (hors-périmètre du piège :
     * ici on copie, la fuite de référence est illustrée par {@link BuggyLine}).
     */
    public List<BuggyLine> getLignes() {
        return List.copyOf(lignes);
    }

    /**
     * Retire toutes les lignes qui ne desservent pas l'arrêt {@code code}.
     *
     * <p>BUG PEDAGOGIQUE: {@code lignes.remove(l)} à l'intérieur d'un
     * {@code for}-each sur {@code lignes} → ConcurrentModificationException
     * dès qu'un retrait est suivi d'une nouvelle itération.
     * (Corrigé dans fixed/FixedNetwork : {@code removeIf}.)</p>
     */
    public void retirerLignesSansCorrespondance(String code) {
        for (BuggyLine l : lignes) {
            if (l.getArrets().stream().noneMatch(s -> s.getCode().equals(code))) {
                lignes.remove(l); // BUG PEDAGOGIQUE: mutation pendant l'itération
            }
        }
    }
}
