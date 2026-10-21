package fr.ebruno.dev.oocol.heritage.domain;

/**
 * Une rame de métro du réseau : un {@link Vehicle} électrifié par troisième
 * rail, composé d'un nombre fixe de voitures, conduite avec ou sans
 * machiniste.
 *
 * <p><strong>Ce que cette classe démontre (ELO 05) :</strong>
 * <ul>
 *   <li><em>héritage simple</em> : {@code extends Vehicle} — la rame récupère
 *       immatriculation, capacité, vitesse, compteur, {@code annoncer()} ;
 *       elle n'ajoute que sa différence ;</li>
 *   <li><em>implémentation des méthodes abstraites</em> :
 *       {@link #decrirePropulsion()} répond à la question que le parent
 *       ne pouvait pas poser ;</li>
 *   <li><em>retour covariant</em> : {@link #copier()} renvoie
 *       {@code MetroTrain} là où le parent promet {@code Vehicle} —
 *       un sous-type au retour est autorisé (jamais un sur-type), et l'appelant
 *       gagne un objet exploitable sans cast.</li>
 * </ul>
 *
 * <p><strong>Note — le masquage de méthodes statiques (à lire une fois).</strong>
 * On serait tenté d'écrire ici {@code public static int nombreVehicules()}
 * pour « compter les métros seulement ». Attention : une méthode {@code static}
 * n'est <em>pas</em> liée à l'objet, donc il n'y a pas d'override possible —
 * la méthode du fils <em>MASQUE</em> (hide) celle du père : le compilateur
 * choisit laquelle exécuter selon le <em>type statique de la variable</em>,
 * jamais selon l'objet réel. Zéro polymorphisme. {@code Vehicle.nombreVehicules()}
 * continue de compter TOUTE la flotte ; {@code MetroTrain.nombreVehicules()}
 * compterait les métros — deux méthodes homonymes sans lien, piège de lecture
 * garanti. La démonstration de ce comportement vit dans
 * {@link fr.ebruno.dev.oocol.heritage.app.PatronDemo} (classes auxiliaires
 * {@code StatFlotte}/{@code StatMetro}) ; elle n'est volontairement PAS
 * implantée ici, pour que la hiérarchie reste un modèle à imiter, pas un piège.
 *
 * @author Emmanuel Bruno
 * @see Vehicle
 */
public class MetroTrain extends Vehicle {

    /** Nombre de voitures qui composent la rame. Au moins 1. */
    private int nombreVoitures;

    /** {@code true} si la rame roule sans machiniste (conduite automatique). */
    private boolean automatique;

    /**
     * Construit une rame.
     *
     * <p>{@code super(...)} doit être la <strong>tout première</strong>
     * instruction du constructeur : les attributs communs sont validés par le
     * parent AVANT que la sous-classe n'ajoute les siens — la chaîne
     * d'initialisation va du général au particulier, dans les deux sens
     * (constructeur) et l'autre (destruction par le ramasse-miettes).
     *
     * @param immatriculation   immatriculation de la rame, non vide
     * @param capacitePassagers nombre de places, strictement positif (validé par le parent)
     * @param vitesseMaxKmh     vitesse maximale en km/h, strictement positive (validée par le parent)
     * @param nombreVoitures    nombre de voitures de la rame, au moins 1
     * @param automatique       {@code true} pour une rame sans machiniste
     * @throws IllegalArgumentException si le parent rejette un de ses arguments ou si
     *                                  {@code nombreVoitures} est inférieur à 1
     */
    public MetroTrain(String immatriculation, int capacitePassagers, double vitesseMaxKmh,
            int nombreVoitures, boolean automatique) {
        // Délégation AU parent : la validation commune vit en un seul endroit.
        super(immatriculation, capacitePassagers, vitesseMaxKmh);
        if (nombreVoitures < 1) {
            throw new IllegalArgumentException(
                    "Une rame de métro compte au moins 1 voiture, reçue : " + nombreVoitures);
        }
        this.nombreVoitures = nombreVoitures;
        this.automatique = automatique;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Réponse du métro à la question posée par le parent :
     * {@code "Électrique 3rd rail, 4 voitures"}.
     */
    @Override
    public String decrirePropulsion() {
        return "Électrique 3rd rail, " + nombreVoitures + " voitures";
    }

    /**
     * Copie la rame.
     *
     * <p><strong>Retour covariant :</strong> le parent promet
     * {@code Vehicle copier()} ; ici le type de retour est <em>rétréci</em> à
     * {@code MetroTrain}. C'est légal (un {@code MetroTrain} EST un
     * {@code Vehicle} substituable partout) et utile : depuis une variable
     * typée {@code MetroTrain}, l'appel {@code metro.copier()} se range dans
     * un {@code MetroTrain} sans cast. Sans covariance, il faudrait écrire
     * {@code (MetroTrain) metro.copier()} — un cast qui dit « le compilateur
     * ne sait pas, je sais à sa place », et qui peut mentir.
     *
     * @return une nouvelle rame identique (mêmes valeurs, objet distinct)
     */
    @Override
    public MetroTrain copier() {
        return new MetroTrain(immatriculation, capacitePassagers, vitesseMaxKmh,
                nombreVoitures, automatique);
    }

    /**
     * @return le nombre de voitures de la rame
     */
    public int getNombreVoitures() {
        return nombreVoitures;
    }

    /**
     * @return {@code true} si la rame roule sans machiniste
     */
    public boolean estAutomatique() {
        return automatique;
    }
}
