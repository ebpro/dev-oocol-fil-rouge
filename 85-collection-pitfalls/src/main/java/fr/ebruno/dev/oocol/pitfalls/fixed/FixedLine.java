package fr.ebruno.dev.oocol.pitfalls.fixed;

import java.util.ArrayList;
import java.util.List;

/**
 * La version <b>corrigée</b> de {@link fr.ebruno.dev.oocol.pitfalls.bugs.BuggyLine} :
 * le getter ne fuit plus — il renvoie une <b>copie immuable</b>.
 *
 * <p>{@code List.copyOf(arrets)} : l'appelant voit l'état du moment, ne peut
 * pas le modifier, et toute mutation interne ultérieure n'apparaît pas dans
 * une vue déjà capturée (sémantique « instantané »). Alternative valable :
 * {@code Collections.unmodifiableList(arrets)} — vue non modifiable mais
 * <em>toujours liée</em> à l'interne ; le choix est donc documenté ici.</p>
 *
 * @author Emmanuel Bruno
 */
public class FixedLine {

    private final String nom;
    private final List<FixedStation> arrets = new ArrayList<>();

    public FixedLine(String nom) {
        this.nom = nom;
    }

    public String getNom() {
        return nom;
    }

    /** Ajoute un arrêt « par la porte » — seule voie de mutation. */
    public void ajouterArret(FixedStation arret) {
        arrets.add(arret);
    }

    /**
     * FIX: copie défensive immuable — plus de référence mutable interne qui
     * fuit ; la vue capturée ne voit pas les ajouts ultérieurs.
     */
    public List<FixedStation> getArrets() {
        return List.copyOf(arrets);
    }
}
