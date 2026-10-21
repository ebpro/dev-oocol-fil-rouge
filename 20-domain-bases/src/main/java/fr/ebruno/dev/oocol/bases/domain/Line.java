package fr.ebruno.dev.oocol.bases.domain;

/**
 * Une ligne du réseau : elle relie une station d'origine à une station de
 * destination, avec une fréquence de passage exprimée en minutes (ex. « une
 * rame toutes les 15 minutes »).
 *
 * <p><strong>Tension pédagogique assumée — lisez ceci en entier.</strong>
 * Cette classe ne peut relier que <em>DEUX</em> stations : un champ
 * {@code origine} et un champ {@code destination}, point. Or un vrai réseau a
 * des <em>arrêts intermédiaires</em> en nombre variable — 3, 8, 20… Impossible
 * de le modéliser avec deux champs fixes. Le code actuel est donc
 * volontairement <em>insuffisant</em> : c'est le sentiment qu'il faut
 * expérimenter avant d'introduire les collections. La séance W5
 * (module {@code 40-collections}) remplacera ces deux champs par une vraie
 * {@code List<Station>} d'arrêts ordonnés, et {@link #distanceEstimeeKm()}
 * deviendra un calcul réel sur la liste.
 *
 * <p>En attendant, la classe exerce : la composition (une {@code Line} « tient »
 * deux {@code Station}), la surcharge de constructeurs avec valeur par défaut,
 * et un compteur {@code static} de plus (pour comparer avec
 * {@link Station#nombreStations()}).
 *
 * @author Emmanuel Bruno
 * @see Station
 */
public class Line {

    /** Fréquence par défaut entre deux passages, en minutes (W5 la rendra réglable par la direction). */
    private static final int FREQUENCE_DEFAUT_MIN = 15;

    /**
     * Compteur global des lignes créées — même mécanisme que
     * {@link Station#compteurInstances}, pour ancrer la notion de champ de classe.
     */
    private static int compteurLignes = 0;

    /** Nom commercial de la ligne (ex. {@code "Ligne A"}). */
    private String nom;

    /** Station de départ de la ligne. */
    private Station origine;

    /** Station d'arrivée de la ligne. */
    private Station destination;

    /**
     * Zone commune aux deux extrémités, ou {@code null} si la ligne traverse
     * plusieurs zones. Seul attribut <em>facultatif</em> de la classe —
     * première approche explicite de l'absence (« null veut dire : on ne sait pas / sans objet »).
     */
    private Zone zoneCommune;

    /** Intervalle entre deux passages, en minutes. Strictement positif. */
    private int frequenceMin;

    /**
     * Constructeur « pratique » : une ligne toutes les 15 minutes par défaut.
     *
     * <p>La surcharge sert ici à poser une <strong>valeur par défaut</strong>
     * unique et centralisée ({@link #FREQUENCE_DEFAUT_MIN}) : le code qui
     * crée une ligne sans réfléchir à la fréquence obtient une ligne plausible.
     *
     * @param nom         nom de la ligne (ex. {@code "Ligne A"})
     * @param origine     station de départ
     * @param destination station d'arrivée
     * @throws IllegalArgumentException si un argument obligatoire est absent
     */
    public Line(String nom, Station origine, Station destination) {
        // Délégation au constructeur complet : la valeur par défaut vit en un seul endroit.
        this(nom, origine, destination, FREQUENCE_DEFAUT_MIN);
    }

    /**
     * Constructeur complet : fréquence de passage personnalisée.
     *
     * @param nom          nom de la ligne (ex. {@code "Ligne A"})
     * @param origine      station de départ
     * @param destination  station d'arrivée
     * @param frequenceMin intervalle entre deux passages, en minutes, strictement positif
     * @throws IllegalArgumentException si la fréquence est négative ou nulle, ou si un
     *                                  argument obligatoire est absent
     */
    public Line(String nom, Station origine, Station destination, int frequenceMin) {
        setNom(nom);
        setOrigine(origine);
        setDestination(destination);
        // Validation dupliquée exprès dans le setter ET ici ? Non : on passe
        // par le setter, qui porte la garde. Le constructeur ne fait que
        // déléguer — principe : une seule porte d'entrée par règle.
        setFrequenceMin(frequenceMin);
        compteurLignes++;
    }

    /**
     * Nombre total de lignes créées dans cette JVM.
     *
     * @return le nombre de lignes construites depuis le démarrage
     */
    public static int nombreLignes() {
        return compteurLignes;
    }

    /**
     * @return le nom commercial de la ligne
     */
    public String getNom() {
        return nom;
    }

    /**
     * @param nom le nouveau nom de la ligne, non vide
     * @throws IllegalArgumentException si le nom est {@code null} ou blanc
     */
    public void setNom(String nom) {
        if (nom == null || nom.isBlank()) {
            throw new IllegalArgumentException("Le nom d'une ligne est obligatoire.");
        }
        this.nom = nom;
    }

    /**
     * @return la station d'origine
     */
    public Station getOrigine() {
        return origine;
    }

    /**
     * @param origine la nouvelle station de départ, obligatoire
     * @throws IllegalArgumentException si l'origine est {@code null}
     */
    public void setOrigine(Station origine) {
        if (origine == null) {
            throw new IllegalArgumentException("Une ligne a une station d'origine.");
        }
        this.origine = origine;
    }

    /**
     * @return la station de destination
     */
    public Station getDestination() {
        return destination;
    }

    /**
     * @param destination la nouvelle station d'arrivée, obligatoire
     * @throws IllegalArgumentException si la destination est {@code null}
     */
    public void setDestination(Station destination) {
        if (destination == null) {
            throw new IllegalArgumentException("Une ligne a une station de destination.");
        }
        this.destination = destination;
    }

    /**
     * @return la zone commune aux extrémités, ou {@code null} si la ligne est multi-zones
     */
    public Zone getZoneCommune() {
        return zoneCommune;
    }

    /**
     * @param zoneCommune la zone commune, ou {@code null} pour une ligne multi-zones
     */
    public void setZoneCommune(Zone zoneCommune) {
        // Attribut volontairement nullable : « null » est une information
        // (aucune zone commune), pas une erreur. Contrairement aux autres
        // setters, celui-ci ne rejette pas null.
        this.zoneCommune = zoneCommune;
    }

    /**
     * @return l'intervalle entre deux passages, en minutes
     */
    public int getFrequenceMin() {
        return frequenceMin;
    }

    /**
     * @param frequenceMin le nouvel intervalle, en minutes, strictement positif
     * @throws IllegalArgumentException si la fréquence est négative ou nulle
     */
    public void setFrequenceMin(int frequenceMin) {
        if (frequenceMin <= 0) {
            throw new IllegalArgumentException(
                    "La fréquence d'une ligne doit être strictement positive (en minutes), reçue : " + frequenceMin);
        }
        this.frequenceMin = frequenceMin;
    }

    /**
     * Indique si la station donnée est une extrémité de la ligne.
     *
     * <p>La comparaison se fait par <strong>code d'exploitation</strong>, pas
     * par référence : deux objets {@code Station} distincts créés avec le
     * même code « GAR » désignent la même station réelle. Comparer avec
     * {@code ==} ne testerait l'identité qu'<em>à l'intérieur d'un même
     * objet</em> — un piège classique quand les stations proviennent de
     * sources différentes (future base de données, lecture d'un fichier…).
     *
     * @param s la station à tester, peut être {@code null}
     * @return {@code true} si {@code s} est l'origine ou la destination de la ligne
     */
    public boolean estDirecte(Station s) {
        if (s == null) {
            return false;
        }
        return s.getCode().equals(origine.getCode())
                || s.getCode().equals(destination.getCode());
    }

    /**
     * Distance estimée de la ligne, en kilomètres.
     *
     * <p><strong>Placeholder pédagogique :</strong> W5 remplacera cette
     * valeur fixe par une vraie liste de stations intermédiaires — pour
     * l'instant, une ligne relie deux extrémités et rien d'autre, donc la
     * « distance » ne peut être qu'une convention : 1 km par défaut.
     *
     * @return la distance estimée (valeur factice : {@code 1.0})
     */
    public double distanceEstimeeKm() {
        return 1.0;
    }

    /**
     * Représentation lisible, ex. {@code "Ligne A: Gare Centrale → Université (toutes les 15 min)"}.
     *
     * @return le libellé de la ligne
     */
    @Override
    public String toString() {
        return nom + ": " + origine.getNom() + " \u2192 " + destination.getNom()
                + " (toutes les " + frequenceMin + " min)";
    }
}
