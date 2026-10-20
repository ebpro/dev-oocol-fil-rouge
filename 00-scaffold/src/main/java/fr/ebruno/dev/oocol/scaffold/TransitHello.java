package fr.ebruno.dev.oocol.scaffold;

/**
 * Point d'entrée du fil rouge — semaines 1 et 2 : « prise en main de la chaîne d'outils ».
 *
 * <p>Cette classe est volontairement minimaliste. Elle sert de « patte de lapin »
 * (hello-world pédagogique) autour du thème fil rouge : le réseau de transport
 * public fictif <em>Trans'U</em> de l'agglomération. Elle démontre, sans aucune
 * dépendance externe :
 *
 * <ul>
 *   <li>la lecture des propriétés système de la JVM ({@code java.version},
 *       {@code java.vm.name}, {@code java.vm.version}) ;</li>
 *   <li>une structure de contrôle simple (parcours d'un tableau d'arrêts) ;</li>
 *   <li>la séparation entre une fonction pure ({@link #greeting(String)}, testable
 *       sans effet de bord) et la méthode {@code main} (qui affiche) ;</li>
 *   <li>le fait qu'un module du fil rouge se construit et se teste isolément :
 *       {@code ./mvnw -pl 00-scaffold verify}.</li>
 * </ul>
 *
 * <p>Les vraies entités du domaine (Station, Ligne, Trajet…) arrivent au module
 * {@code 20-domain-bases} (séance CNAM S4). Ici, rien à modéliser : seulement
 * vérifier que l'outillage — compilateur, tests, analyse statique, CI — fonctionne
 * de bout en bout.
 *
 * @author Emmanuel Bruno
 */
public final class TransitHello {

    /** Nom du réseau fictif qui sert de fil conducteur au semestre. */
    static final String NETWORK_NAME = "Trans'U";

    /** Arrêts desservis par la « ligne A » de démonstration (ordre de parcours). */
    static final String[] LINE_A_STOPS = {"Gare centrale", "Port", "Campus", "CH Sud"};

    /**
     * Classe utilitaire : instantiation interdite.
     *
     * <p>Première notion de conception introduite furtivement : quand une classe
     * n'apporte que des méthodes statiques, on rend le constructeur privé pour
     * interdire toute création d'instance inutile.
     */
    private TransitHello() {
        throw new AssertionError("Classe utilitaire : ne s'instancie pas.");
    }

    /**
     * Construit le message d'accueil pour une ligne donnée.
     *
     * <p>Fonction <em>pure</em> : aucun affichage, aucun effet de bord — c'est elle
     * que le test unitaire vérifie. {@code main}, lui, se contente d'orchestrer
     * l'affichage.
     *
     * @param lineName nom de la ligne (ex. {@code "A"})
     * @return le message d'accueil complet
     */
    public static String greeting(String lineName) {
        return "Bienvenue à bord de la ligne " + lineName + " du réseau " + NETWORK_NAME + ".";
    }

    /**
     * Affiche l'accueil, la version de la JVM, puis le parcours de la ligne A.
     *
     * @param args non utilisés à ce stade du fil rouge
     */
    public static void main(String[] args) {
        // 1. Le message d'accueil (construit par la fonction pure testée, affiché ici).
        System.out.println(greeting("A"));

        // 2. La JVM est une machine virtuelle paramétrée par des propriétés système.
        //    C'est la source de vérification des tests : java.version doit exister.
        System.out.println("Version Java : " + System.getProperty("java.version"));
        System.out.println("Machine virtuelle : " + System.getProperty("java.vm.name")
                + " (" + System.getProperty("java.vm.version") + ")");

        // 3. Boucle simple : le parcours complet de la ligne A, arrêt par arrêt.
        //    (Aucune collection ici : c'est l'objet du module 40-collections.)
        for (int stopIndex = 0; stopIndex < LINE_A_STOPS.length; stopIndex++) {
            System.out.printf("Arrêt %d/%d : %s%n",
                    stopIndex + 1, LINE_A_STOPS.length, LINE_A_STOPS[stopIndex]);
        }
    }
}
