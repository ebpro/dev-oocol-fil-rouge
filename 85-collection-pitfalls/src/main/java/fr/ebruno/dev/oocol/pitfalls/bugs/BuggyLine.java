package fr.ebruno.dev.oocol.pitfalls.bugs;

import java.util.ArrayList;
import java.util.List;

/**
 * Une ligne du réseau Trans'U — avec le piège de la <b>référence mutable
 * qui fuit</b> hors de l'objet.
 *
 * <p>{@code getArrets()} renvoie la liste interne <em>elle-même</em> :
 * n'importe quel appelant peut y ajouter ou retirer des arrêts sans passer
 * par l'objet, contournant toute validation et détruisant l'invariant de la
 * ligne. Un getter ne doit jamais exposer la collection mutable interne —
 * il renvoie une copie immuable (ou une vue {@code unmodifiableList}).</p>
 *
 * @author Emmanuel Bruno
 */
public class BuggyLine {

    private final String nom;
    private final List<BuggyStation> arrets = new ArrayList<>();

    public BuggyLine(String nom) {
        this.nom = nom;
    }

    public String getNom() {
        return nom;
    }

    /** Ajoute un arrêt « par la porte » — cette mutation-là est légitime. */
    public void ajouterArret(BuggyStation arret) {
        arrets.add(arret);
    }

    /**
     * BUG PEDAGOGIQUE: renvoie la référence mutable interne — l'appelant
     * peut modifier l'intérieur de la ligne sans aucune validation ni
     * notification. (Corrigé dans fixed/FixedLine : copie défensive.)
     */
    public List<BuggyStation> getArrets() {
        return arrets;
    }
}
