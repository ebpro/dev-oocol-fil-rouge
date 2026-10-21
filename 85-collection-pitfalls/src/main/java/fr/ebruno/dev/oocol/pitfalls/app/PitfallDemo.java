package fr.ebruno.dev.oocol.pitfalls.app;

import fr.ebruno.dev.oocol.pitfalls.bugs.BuggyLine;
import fr.ebruno.dev.oocol.pitfalls.bugs.BuggyNetwork;
import fr.ebruno.dev.oocol.pitfalls.bugs.BuggyRemoveLoop;
import fr.ebruno.dev.oocol.pitfalls.bugs.BuggyStation;
import fr.ebruno.dev.oocol.pitfalls.bugs.BuggyTicketCache;
import fr.ebruno.dev.oocol.pitfalls.fixed.FixedLine;
import fr.ebruno.dev.oocol.pitfalls.fixed.FixedNetwork;
import fr.ebruno.dev.oocol.pitfalls.fixed.FixedRemoveLoop;
import fr.ebruno.dev.oocol.pitfalls.fixed.FixedStation;
import fr.ebruno.dev.oocol.pitfalls.fixed.FixedTicketCache;

import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.List;

/**
 * Démonstration exécutable de trois des dix pièges du module : chaque scène
 * joue d'abord l'opération sur le code buggé (trace d'erreur ou résultat
 * faux imprimée à l'écran), puis sur le miroir corrigé (succès).
 *
 * <p>But pédagogique : montrer qu'un « bug de collections » n'est pas un
 * concept abstrait — soit ça explose ({@code ConcurrentModificationException}),
 * soit ça ment silencieusement ({@code null} là où il y a une valeur, élément
 * pair survivant au nettoyage). Les deux formes sont dangereuses, la seconde
 * plus que la première.</p>
 *
 * @author Emmanuel Bruno
 */
public final class PitfallDemo {

    private static final String BANDEAU = """
            ============================================================
             Module 85 - Chasse aux bugs : pieges des collections (W11)
            ------------------------------------------------------------
             Ce programme rejoue trois des dix pieges sur le code buggé
             (paquet bugs/), puis la meme operation sur le miroir corrige
             (paquet fixed/). Lecture attendue : d'abord une trace ou un
             resultat faux, ensuite le comportement correct.
            ============================================================
            """;

    private PitfallDemo() {
        // Point d'entrée de démonstration — pas d'instanciation.
    }

    /**
     * Scène 1 : CME (retrait pendant for-each) ; scène 2 : HashMap qui perd
     * ses clés « égales » ; scène 3 : boucle d'index qui saute des éléments.
     */
    public static void main(String[] args) {
        System.out.println(BANDEAU);
        scene1ConcurrentModification();
        scene2CacheManque();
        scene3BoucleDIndex();
        System.out.println("""
                ------------------------------------------------------------
                Trois pièges joués, trois miroirs qui les corrigent.
                A toi de jouer : PitfallDetectionTest attend tes corrections.
                ------------------------------------------------------------
                """);
    }

    private static void scene1ConcurrentModification() {
        System.out.println("[Scène 1] Retirer des lignes pendant un for-each");

        BuggyNetwork reseauBugge = new BuggyNetwork();
        BuggyLine u = new BuggyLine("U");
        u.ajouterArret(new BuggyStation("GAR", "Gare"));
        BuggyLine v = new BuggyLine("V");
        v.ajouterArret(new BuggyStation("UNI", "Universite"));
        BuggyLine w = new BuggyLine("W");
        w.ajouterArret(new BuggyStation("HOP", "Hopital"));
        reseauBugge.ajouterLigne(u);
        reseauBugge.ajouterLigne(v);
        reseauBugge.ajouterLigne(w);
        try {
            reseauBugge.retirerLignesSansCorrespondance("XXX");
            System.out.println("  buggé : ??? aucune exception (inattendu)");
        } catch (ConcurrentModificationException e) {
            System.out.println("  buggé : ConcurrentModificationException -> " + e);
        }

        FixedNetwork reseauFixe = new FixedNetwork();
        FixedLine fu = new FixedLine("U");
        fu.ajouterArret(new FixedStation("GAR", "Gare"));
        FixedLine fv = new FixedLine("V");
        fv.ajouterArret(new FixedStation("UNI", "Universite"));
        FixedLine fw = new FixedLine("W");
        fw.ajouterArret(new FixedStation("HOP", "Hopital"));
        reseauFixe.ajouterLigne(fu);
        reseauFixe.ajouterLigne(fv);
        reseauFixe.ajouterLigne(fw);
        reseauFixe.retirerLignesSansCorrespondance("XXX");
        System.out.println("  fixé : retrait sans exception, reste "
                + reseauFixe.getLignes().size() + " ligne(s)");
        System.out.println();
    }

    private static void scene2CacheManque() {
        System.out.println("[Scène 2] Cache de prix indexé par station « égale »");

        BuggyTicketCache cacheBugge = new BuggyTicketCache();
        cacheBugge.cache(new BuggyStation("GAR", "Gare Centrale"), 2.0);
        Double prixBugge = cacheBugge.lookup(new BuggyStation("GAR", "Gare Neuve"));
        System.out.println("  buggé : lookup(GAR, autre instance) -> " + prixBugge
                + "  (le prix existe, mais la clé est injoignable)");

        FixedTicketCache cacheFixe = new FixedTicketCache();
        cacheFixe.cache(new FixedStation("GAR", "Gare Centrale"), 2.0);
        Double prixFixe = cacheFixe.lookup(new FixedStation("GAR", "Gare Neuve"));
        System.out.println("  fixé : lookup(GAR, autre instance) -> " + prixFixe);
        System.out.println();
    }

    private static void scene3BoucleDIndex() {
        System.out.println("[Scène 3] Retirer les pairs dans une boucle for croissante");

        BuggyRemoveLoop boucleBuggee = new BuggyRemoveLoop();
        List<Integer> avant = new ArrayList<>(List.of(2, 4, 1, 3));
        List<Integer> apres = boucleBuggee.retirerPairs(avant);
        System.out.println("  buggé : [2, 4, 1, 3] -> " + apres
                + "  (4 survit : il a été sauté après le retrait de 2)");

        FixedRemoveLoop boucleFixee = new FixedRemoveLoop();
        List<Integer> avantFix = new ArrayList<>(List.of(2, 4, 1, 3));
        List<Integer> apresFix = boucleFixee.retirerPairs(avantFix);
        System.out.println("  fixé : [2, 4, 1, 3] -> " + apresFix
                + "  (entrée inchangée : " + avantFix + ")");
        System.out.println();
    }
}
