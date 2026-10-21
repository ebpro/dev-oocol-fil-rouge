package fr.ebruno.dev.oocol.exceptions.app;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;

import fr.ebruno.dev.oocol.exceptions.domain.Compte;
import fr.ebruno.dev.oocol.exceptions.domain.FareCalculationException;
import fr.ebruno.dev.oocol.exceptions.domain.FareCalculator;
import fr.ebruno.dev.oocol.exceptions.domain.InsufficientFundsException;
import fr.ebruno.dev.oocol.exceptions.domain.Line;
import fr.ebruno.dev.oocol.exceptions.domain.NetworkException;
import fr.ebruno.dev.oocol.exceptions.domain.ScheduleConflictException;
import fr.ebruno.dev.oocol.exceptions.domain.Station;
import fr.ebruno.dev.oocol.exceptions.domain.StationNotFoundException;
import fr.ebruno.dev.oocol.exceptions.domain.Zone;

/**
 * Les six gestes d'exception du développeur Trans'U, joués en scène ouvertes.
 *
 * <p><strong>Lecture.</strong> Chaque scène est une méthode privée autonome
 * que l'on peut commenter séparément en TD ; {@link #main(String[])} les
 * enchaîne et titre. Les signatures {@code throws} portées par les scènes
 * (et résumées sur {@code main}) décrivent le PIRE CAS de chaque scène —
 * exactement ce qu'un appelant de production devrait écrire s'il ne
 * rattrapait pas ; la démo, elle, rattrape pour pouvoir tout montrer.
 *
 * <p>Pas de flux réseau, pas de fichier : la ressource de la scène 3 est un
 * {@link StringReader} en mémoire — le try-with-resources se démontre sans
 * dépendre du système de fichiers, donc la démo court partout, CI comprise.
 *
 * @author Emmanuel Bruno
 * @see fr.ebruno.dev.oocol.exceptions.domain
 */
public final class ExceptionDemo {

    /** Utilitaire statique : pas d'instance. */
    private ExceptionDemo() {
    }

    /**
     * Enchaîne les six scènes, titrées.
     *
     * @param args non utilisé
     * @throws IOException               pire cas de la scène 3 (fermeture de ressource)
     * @throws InsufficientFundsException pire cas de la scène 2 (débit sans rattrapage)
     */
    public static void main(String[] args) throws IOException, InsufficientFundsException {
        scene1Propagation();
        scene2Recuperation();
        scene3TryWithResources();
        scene4OrdreDesCatch();
        scene5ChainageDeCause();
        scene6MultiCatch();
        System.out.println();
        System.out.println("=== Fin de la démo : les six gestes sont joués. ===");
    }

    /** Titre de scène, même gabarit partout. */
    private static void titre(int numero, String nom) {
        System.out.println();
        System.out.println("=== Scène " + numero + " \u2014 " + nom + " ===");
    }

    /**
     * Scène 1 — PROPAGER : l'unchecked n'est pas rattrapé, elle monte.
     *
     * <p>Ici on la rattrape UNE fois pour pouvoir afficher la leçon sans
     * tuer la démo ; en production, le bon geste est de la LAISSER monter
     * jusqu'au point d'entrée (ou jusqu'à la CI) et d'y imprimer la stack
     * trace — {@code e.printStackTrace()} montre le « au point même de la
     * demande », plus de NPE différée trois méthodes plus loin.
     */
    private static void scene1Propagation() {
        titre(1, "Propagation : l'unchecked remonte, on ne négocie pas");
        Zone z1 = new Zone(1, "Centre-ville");
        Station gar = new Station("Gare Centrale", "GAR", z1);
        Station uni = new Station("Universit\u00E9", "UNI", z1);
        Line ligneA = new Line("Ligne A");
        ligneA.ajouterArret(gar);
        ligneA.ajouterArret(uni);

        try {
            // W5 : Station introuvable = return null = NPE trois méthodes plus loin.
            // W6 : levée immédiate, typée, localisée, avec le code cherché dedans.
            Station fantome = ligneA.trouverParCode("ZZZ");
            System.out.println("Jamais affiché : " + fantome);
        } catch (StationNotFoundException e) {
            System.out.println("rattrap\u00E9 : " + e.getMessage());
            System.out.println("  -> code cherché : " + e.getCode()
                    + " ; la ligne ne dessert pas ce code : c'est UN BUG de l'appelant, pas un état du monde.");
            System.out.println("  -> en production, on ne rattrape pas : on LAISSE MONTER et on corrige l'appel.");
        }
    }

    /**
     * Scène 2 — RÉCUPÉRER : le checked a un plan B, on le joue.
     *
     * <p>Solde insuffisant → rattraper → lire {@code getManquant()} →
     * recharger <em>exactement</em> ce manque → réessayer → succès. Le
     * compilateur avait FORCÉ l'écriture du catch : personne n'a « oublié »
     * la situation. C'est tout l'inverse de la scène 1 : ici rattraper est
     * LE comportement correct.
     *
     * @throws InsufficientFundsException si la dernière tentative échoue
     *                                    (neuf cas sur dix : la démo la déclare, ne l'atteint pas)
     */
    private static void scene2Recuperation() throws InsufficientFundsException {
        titre(2, "Récupération : le checked se rattrape AVEC un plan B");
        Compte borne = new Compte("U-42", 5.0);
        double prixTitre = 12.0;

        try {
            borne.debiter(prixTitre);
            System.out.println("Débit direct accepté (inattendu ici).");
        } catch (InsufficientFundsException e) {
            System.out.println("rattrap\u00E9 : " + e.getMessage());
            double manque = e.getManquant();
            System.out.println("  -> plan B : recharger exactement " + manque + " \u20AC puis réessayer.");
            borne.crediter(manque);
        }
        // Deuxième tentative, hors catch cette fois : le throws de la scène
        // dit « cette ligne PEUT échouer » — l'appelant de production, lui,
        // devra vraiment gérer. C'est ça, le contrat checked.
        borne.debiter(prixTitre);
        System.out.println("  -> re-essai après recharge : débit OK, solde restant = " + borne.getSolde() + " \u20AC");
    }

    /**
     * Scène 3 — FERMER : try-with-resources, la ressource se ferme seule.
     *
     * <p>Un {@link BufferedReader} sur un {@link StringReader} (les arrêts
     * d'une ligne, « en mémoire ») : pas de {@code finally} à écrire, la
     * fermeture est garantie par la structure — même en cas d'exception
     * dans le bloc. La seconde partie de la scène PAYE cette garantie : on
     * relit le reader FERMÉ, l'{@link IOException} qui suit est celle que
     * le try-with-resources rend inévitable et donc visible, typée, tôt.
     *
     * @throws IOException si la fermeture de la ressource échoue (neuf cas
     *                     sur dix : {@code StringReader} ne meurt jamais en fermant)
     */
    private static void scene3TryWithResources() throws IOException {
        titre(3, "try-with-resources : la fermeture est garantie par la structure");
        String contenu = "GAR\nUNI\nHOP\n"; // trois arrêts, un par ligne — zéro fichier

        int lignes = 0;
        String premier = null;
        // Le reader est DÉCLARÉ dans l'en-tête du try : fermé automatiquement,
        // même si le corps lève — plus jamais de "finally { if (r != null) r.close(); }".
        try (BufferedReader reader = new BufferedReader(new StringReader(contenu))) {
            String ligne = reader.readLine();
            while (ligne != null) { // while explicite : pas de stream (contrainte W6 inchangée)
                lignes++;
                if (premier == null) {
                    premier = ligne;
                }
                ligne = reader.readLine();
            }
        }
        System.out.println("lu " + lignes + " arr\u00EAts, premier = " + premier
                + " ; le reader a été fermé tout seul en sortant du bloc.");

        try {
            readerApresFermeture();
        } catch (IOException e) {
            System.out.println("  -> relire une ressource ferm\u00E9e l\u00E8ve bien : " + e);
            System.out.println("  -> le try-with-resources ne rend pas l'erreur impossible : il la rend precoce et typ\u00E9e.");
        }
    }

    /** Lit dans un reader déjà fermé — l'{@link IOException} remonte sans être rattrapée ici. */
    private static void readerApresFermeture() throws IOException {
        String contenu = "GAR\n";
        BufferedReader ferme = new BufferedReader(new StringReader(contenu));
        ferme.close(); // fermeture manuelle : le cas « oubli de finally » d'avant Java 7
        ferme.readLine(); // IOException : le stream est fermé
    }

    /**
     * Scène 4 — ORDONNER : du plus spécifique au plus large, ou ça ne compile pas.
     *
     * <p>Trois tentatives, trois rattrapages : (A) la clause spécifique
     * {@code ScheduleConflictException} gagne sur la racine {@code
     * NetworkException} parce qu'elle est écrite AVANT elle — inverser
     * l'ordre = unreachable catch <em>refusé par le compilateur</em>,
     * l'ordre n'est pas une convention, c'est une loi ; (B) la même
     * exception, cette fois bordée PAR LA RACINE seule — preuve vivante de
     * l'héritage : une SCE {@code instanceof NetworkException} ; (C) la
     * sœur {@code InsufficientFundsException} par sa clause propre.
     */
    private static void scene4OrdreDesCatch() {
        titre(4, "Ordre des catch : spécifique d'abord, la racine en dernier");
        Line ligneB = new Line("Ligne B", 2); // cadence 2 min : possible mais irréaliste
        Compte borne = new Compte("U-7", 1.0);

        // (A) Deux checked en puissance, la clause la plus proche d'abord.
        try {
            ligneB.valider(); // ScheduleConflictException (checked) : première levée, le débit ne suit pas
            borne.debiter(9.0); // InsufficientFundsException (checked) : ne sera pas atteinte ici
        } catch (ScheduleConflictException e) {
            System.out.println("(A) rattrap\u00E9 par la clause sp\u00E9cifique : conflit sur " + e.getLigne()
                    + " \u00E0 l'heure " + e.getHeureConflit() + " -> " + e.getMessage());
        } catch (InsufficientFundsException e) {
            System.out.println("(A) rattrap\u00E9 (sp\u00E9cifique 2) : il manque " + e.getManquant() + " \u20AC");
        } catch (NetworkException e) {
            // En dernier : la RACINE du domaine. Jamais atteinte dans ce try
            // (les deux filles sont traitées avant), toujours atteignable
            // ailleurs — et surtout : ce catch n'avale AUCUNE unchecked, il
            // ne borde que le checked du réseau.
            System.out.println("(A) rattrap\u00E9 par la racine : " + e.getMessage());
        }

        // (B) La MÊME exception, bordée par la racine seule : la hiérarchie
        // fait qu'une SCE tombe aussi dans un catch (NetworkException).
        try {
            ligneB.valider();
        } catch (NetworkException e) {
            System.out.println("(B) la racine seule suffit : " + e.getClass().getSimpleName()
                    + " est un(e) NetworkException \u2014 catch large = catch POUR toute la famille checked.");
        }

        // (C) La sœur, par sa clause propre.
        try {
            borne.debiter(9.0);
        } catch (InsufficientFundsException e) {
            System.out.println("(C) rattrap\u00E9 par la clause propre \u00E0 la s\u0153ur : il manque "
                    + e.getManquant() + " \u20AC");
        }
    }

    /**
     * Scène 5 — ENROBER : une exception de bas niveau devient une cause.
     *
     * <p>La config {@code "min=1x"} est illisible → {@link NumberFormatException}.
     * L'appelant honnête ne veut pas exposer une NFE au-dessus de sa
     * couche : il l'ENROBE dans une {@code ScheduleConflictException} — le
     * vocabulaire du domaine remonte, la trace technique reste branchée
     * derrière {@code getCause()}. {@code printStackTrace()} prouve que le
     * « Caused by: » survit au travers de l'abstraction.
     */
    private static void scene5ChainageDeCause() {
        titre(5, "Chaînage : l'erreur de bas niveau devient une CAUSE, pas un déchet");
        String regle = "min=1x"; // lu dans une config hypothétique
        NumberFormatException basNiveau = null;
        try {
            int minutes = Integer.parseInt(regle);
            System.out.println("Jamais affiché : " + minutes);
        } catch (NumberFormatException e) {
            basNiveau = e;
            System.out.println("bas niveau capt\u00E9 : " + e);
        }
        try {
            throw new ScheduleConflictException(
                    "R\u00E8gle de cadence illisible pour la ligne A", "A", 8, basNiveau);
        } catch (ScheduleConflictException e) {
            System.out.println("enrob\u00E9e m\u00E9tier : " + e.getMessage()
                    + " [ligne=" + e.getLigne() + ", heure=" + e.getHeureConflit() + "]");
            System.out.println("  -> getCause() = " + e.getCause());
            System.out.println("  -> trace complète, avec le « Caused by: » qui a surv\u00E9cu :");
            e.printStackTrace(System.out);
        }
    }

    /**
     * Scène 6 — RAMASSER : deux unchecked parentes, une seule clause.
     *
     * <p>{@code StationNotFoundException} et {@code FareCalculationException}
     * n'ont pas de classe commune autre que {@code RuntimeException} — un
     * multi-catch les borde SANS dériver quoi que ce soit, sans avaler
     * davantage : la clause ne parle que d'elles deux. Le cas traité : la
     * borne calcule un tarif entre une station réelle et une {@code null}
     * (→ {@code StationNotFoundException} propagée par {@code requireValide}),
     * puis entre deux stations dont la zone du milieu n'a pas de barème
     * (→ {@code FareCalculationException}). Les deux, une fois.
     */
    private static void scene6MultiCatch() {
        titre(6, "Multi-catch : deux unchecked, une clause, zéro héritage forcé");
        Zone z1 = new Zone(1, "Centre-ville");
        Zone z3 = new Zone(3, "P\u00E9riph\u00E9rie lointaine");
        Station gar = new Station("Gare Centrale", "GAR", z1);
        Station aer = new Station("A\u00E9roport", "AER", z3);

        FareCalculator bareme = new FareCalculator();
        bareme.enregistrerTarif(1, 2.0); // zone 2 volontairement absente
        bareme.enregistrerTarif(3, 4.5);

        // Deux trajets fautifs, deux familles d'erreur, un seul rattrapage.
        try {
            bareme.calculerPrixEntre(gar, null); // -> StationNotFoundException
        } catch (StationNotFoundException | FareCalculationException e) {
            System.out.println("multi-rattrap\u00E9 : " + e);
        }
        try {
            bareme.calculerPrixEntre(gar, aer); // zone 2 sans tarif -> FareCalculationException
        } catch (StationNotFoundException | FareCalculationException e) {
            System.out.println("multi-rattrap\u00E9 : " + e);
        }
        System.out.println("  -> les deux unchecked sont s\u0153urs sous RuntimeException :");
        System.out.println("     le multi-catch les borde sans inventer de classe commune, sans rien avaler d'autre.");
    }
}
