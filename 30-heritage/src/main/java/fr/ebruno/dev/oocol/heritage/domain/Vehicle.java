package fr.ebruno.dev.oocol.heritage.domain;

/**
 * Véhicule du réseau « Trans'U » — la classe <em>abstraite</em> qui unifie ce
 * que métro, bus et tramway ont en commun : une immatriculation, une capacité,
 * une vitesse maximale, une annonce de service et un compteur de flotte.
 *
 * <p><strong>Pourquoi {@code abstract} ?</strong> On ne sait pas instancier un
 * « véhicule » générique : un véhicule sans type de propulsion n'existe pas
 * dans le monde réel. Le mot-clé {@code abstract} dit au compilateur :
 * « cette classe est un <em>gabarit</em>, elle ne se construit pas elle-même ;
 * seules ses sous-classes concrètes ({@link MetroTrain}, {@link Bus},
 * {@link Tramway}) s'instancient ». Tenter {@code new Vehicle(...)} est une
 * erreur de compilation — la notion elle-même est incomplète, seuls les
 * concepts généraux sont abstraits (UML : stéréotype {@code {abstract}}).
 *
 * <p><strong>Pourquoi un ancêtre commun ?</strong> Sans ancêtre commun, on écrivait
 * un {@code MetroLine} et un {@code BusLine} quasi identiques — duplication
 * pure. En remontant le commun dans {@code Vehicle}, la sous-classe ne garde
 * que sa différence (la propulsion, ses équipements). C'est le mécanisme que
 * la ligne {@link #decrirePropulsion()} rend visible : le <em>quand</em> est
 * décidé ici, le <em>comment</em> est délégué à chaque sous-classe.
 *
 * <p><strong>Et le LSP ?</strong> L'héritage signifie « est-un » <em>substituable</em> :
 * partout où un {@code Vehicle} est attendu, n'importe lequel de ses enfants
 * convient <em>sans casser le contrat</em>. Une {@link Line} ne « est-un »
 * véhicule : elle <em>a</em> un véhicule (composition). Voir le Javadoc de
 * {@link Line} pour la démonstration de la violation.
 *
 * @author Emmanuel Bruno
 * @see MetroTrain
 * @see Bus
 * @see Tramway
 */
public abstract class Vehicle {

    /**
     * Compteur global des véhicules créés — champ de classe {@code static},
     * unique pour toute la hiérarchie : métros, bus et tramways incrémentent
     * <em>tous</em> le même compteur, car ils exécutent tous le constructeur
     * de {@code Vehicle} (par {@code super(...)}).
     */
    private static int compteurVehicules = 0;

    /** Immatriculation commerciale du véhicule (ex. {@code "M-101"}). Protégée : accessible aux sous-classes. */
    protected String immatriculation;

    /** Nombre de places passagers. Strictement positif. */
    protected int capacitePassagers;

    /** Vitesse maximale de conception, en km/h. Strictement positive. */
    protected double vitesseMaxKmh;

    /**
     * Constructeur de gabarit : appelé par {@code super(...)} de chaque
     * sous-classe, il centralise la validation des trois attributs communs.
     * Un objet {@code Vehicle} qui existe est donc un objet valide — la même
     * promesse d'encapsulation qu'au module 20, héritée ici littéralement.
     *
     * @param immatriculation  immatriculation, non vide
     * @param capacitePassagers nombre de places, strictement positif
     * @param vitesseMaxKmh    vitesse maximale en km/h, strictement positive
     * @throws IllegalArgumentException si l'immatriculation est vide, la capacité
     *                                  négative ou nulle, ou la vitesse négative ou nulle
     */
    protected Vehicle(String immatriculation, int capacitePassagers, double vitesseMaxKmh) {
        if (immatriculation == null || immatriculation.isBlank()) {
            throw new IllegalArgumentException("L'immatriculation d'un véhicule est obligatoire.");
        }
        if (capacitePassagers <= 0) {
            throw new IllegalArgumentException(
                    "La capacité d'un véhicule doit être strictement positive, reçue : " + capacitePassagers);
        }
        if (vitesseMaxKmh <= 0) {
            throw new IllegalArgumentException(
                    "La vitesse maximale d'un véhicule doit être strictement positive, reçue : " + vitesseMaxKmh);
        }
        this.immatriculation = immatriculation;
        this.capacitePassagers = capacitePassagers;
        this.vitesseMaxKmh = vitesseMaxKmh;
        compteurVehicules++;
    }

    /**
     * Décrit la motorisation du véhicule — <strong>la méthode abstraite du
     * module</strong>. {@code Vehicle} ne peut pas répondre : « quel moteur ? »
     * n'a pas de sens pour un véhicule générique. Chaque sous-classe <em>doit</em>
     * fournir sa réponse (faute de quoi elle est abstract, et ne compile pas
     * si elle est instanciée). C'est l'appel {@link #annoncer()} qui invoquera
     * cette méthode — sans jamais savoir quelle sous-classe répond : c'est le
     * polymorphisme.
     *
     * @return le libellé de propulsion (ex. {@code "Diesel"}, {@code "Électrique 3rd rail, 4 voitures"})
     */
    public abstract String decrirePropulsion();

    /**
     * Copie ce véhicule. Déclarée abstraite sur le type large {@code Vehicle} :
     * chaque sous-classe l'implémente — et peut <em>rétrécir</em> le type de
     * retour vers son propre type (retour covariant, voir
     * {@link MetroTrain#copier()}) : l'appelant n'a alors aucun cast à écrire
     * quand le type statique de la variable est déjà la sous-classe.
     *
     * @return un nouveau véhicule aux mêmes caractéristiques que celui-ci
     */
    public abstract Vehicle copier();

    /**
     * Annonce de service affichée en station.
     *
     * <p>Méthode concrète héritée <em>telle quelle</em> par toutes les
     * sous-classes : elle est écrite une seule fois ici, mais le fragment
     * {@code decrirePropulsion()} qu'elle contient sera évalué dans la
     * sous-classe de l'objet réel — polymorphisme. {@code bus.annoncer()} et
     * {@code metro.annoncer()} exécutent le <em>même</em> code avec des
     * <em>résultats</em> différents.
     *
     * @return le libellé complet, ex. {@code "Véhicule M-101 — Électrique 3rd rail, 4 voitures — 180 places"}
     */
    public String annoncer() {
        return "Véhicule " + immatriculation + " — " + decrirePropulsion()
                + " — " + capacitePassagers + " places";
    }

    /**
     * Compare les vitesses maximales de deux véhicules.
     *
     * <p>Le paramètre est typé {@code Vehicle} — donc n'importe quel sous-type
     * convient (un bus face à un tramway, un métro face à un métro). À
     * l'intérieur de la méthode, impossible de savoir quels objets concrets
     * sont comparés : seul le contrat commun ({@code vitesseMaxKmh} existe
     * chez tous) est utilisé. C'est la substituabilité du LSP au service de
     * la réutilisation.
     *
     * @param autre l'autre véhicule, obligatoire
     * @return {@code true} si ce véhicule est strictement plus rapide que l'autre
     * @throws IllegalArgumentException si {@code autre} est {@code null}
     */
    public boolean estPlusRapideQue(Vehicle autre) {
        if (autre == null) {
            throw new IllegalArgumentException("Impossible de comparer à un véhicule absent.");
        }
        return this.vitesseMaxKmh > autre.vitesseMaxKmh;
    }

    /**
     * Nombre total de véhicules créés dans cette JVM, toutes sous-classes
     * confondues (le compteur vit dans le constructeur de {@code Vehicle},
     * que tous les enfants exécutent).
     *
     * @return le nombre de véhicules construits depuis le démarrage
     */
    public static int nombreVehicules() {
        return compteurVehicules;
    }

    /**
     * @return l'immatriculation du véhicule
     */
    public String getImmatriculation() {
        return immatriculation;
    }

    /**
     * @return le nombre de places passagers
     */
    public int getCapacitePassagers() {
        return capacitePassagers;
    }

    /**
     * @return la vitesse maximale en km/h
     */
    public double getVitesseMaxKmh() {
        return vitesseMaxKmh;
    }

    /**
     * Représentation lisible générique : {@code "Vehicle[M-101, 180 places, 80.0 km/h]"}.
     *
     * <p>Noter l'absence d'override dans les sous-classes : hériter un
     * {@code toString()} correct suffit — une sous-classe n'est obligée de
     * réécrire que ce qui change pour elle.
     *
     * @return le libellé du véhicule
     */
    @Override
    public String toString() {
        return "Vehicle[" + immatriculation + ", " + capacitePassagers + " places, "
                + vitesseMaxKmh + " km/h]";
    }
}
