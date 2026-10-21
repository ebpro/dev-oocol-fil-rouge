package fr.ebruno.dev.oocol.bases.domain;

/**
 * Une station du réseau « Trans'U » : un arrêt nommé, identifié par un code
 * à trois lettres (ex. {@code "GAR"} pour Gare Centrale), rattaché à une zone
 * tarifaire et éventuellement accessible aux personnes à mobilité réduite
 * (PMR).
 *
 * <p><strong>Classe centrale du module — l'exemple de l'encapsulation.</strong>
 * Tous les attributs sont {@code private} : le monde extérieur n'y a pas
 * accès direct. Pourquoi ? Parce qu'un attribut public est une donnée
 * <em>sans surveillance</em> — n'importe quel code pourrait écrire
 * {@code gare.code = "garbage!"} et produire une station impossible à
 * utiliser. En passant par des accesseurs et des modificateurs, la classe
 * <em>contrôle</em> chaque écriture : c'est ça, encapsuler.
 *
 * <p>La classe démontre aussi :
 * <ul>
 *   <li>la <strong>surcharge de constructeurs</strong> : deux façons de créer
 *       une station (avec ou sans accessibilité PMR), la version courte
 *       déléguant à la version complète pour éviter la duplication ;</li>
 *   <li>un <strong>membre statique</strong> : {@link #compteurInstances} est
 *       partagé par <em>toutes</em> les stations — il compte les instances
 *       créées depuis le démarrage de la JVM, pas les instances d'une
 *       station particulière.</li>
 * </ul>
 *
 * @author Emmanuel Bruno
 * @see Zone
 * @see Line
 */
public class Station {

    /**
     * Compteur global des stations créées.
     *
     * <p>Le mot-clé {@code static} signifie : « une seule copie de ce champ,
     * pour toute la classe », contrairement aux champs d'instance
     * ({@code nom}, {@code code}…) qui existent en un exemplaire
     * <em>par objet</em>. C'est l'attribut idéal pour une statistique
     * transversale — ici, le nombre de stations du réseau.
     *
     * <p>Limite assumée (et discutée en séance) : le compteur ne fait
     * qu'augmenter — Java ne fournit pas de « destructeur » fiable pour le
     * décrémenter quand une station est oubliée par le programme.
     */
    private static int compteurInstances = 0;

    /** Nom commercial de la station (ex. {@code "Gare Centrale"}). */
    private String nom;

    /** Code d'exploitation : exactement 3 lettres majuscules (ex. {@code "GAR"}). */
    private String code;

    /** Accessibilité PMR. {@code false} par défaut (constructeur court). */
    private boolean accessible;

    /** Zone tarifaire à laquelle la station est rattachée. Modifiable : une station peut être re-zonée. */
    private Zone zone;

    /**
     * Constructeur « pratique » : une station non accessible (cas par défaut).
     *
     * <p>Démonstration de la <strong>surcharge</strong> : même nom,
     * signature différente. Il délègue au constructeur complet avec
     * {@code accessible = false} — une seule chaîne de délégation
     * ({@code this(...)}), donc une seule copie de la logique de validation.
     *
     * @param nom  nom de la station, non vide
     * @param code code d'exploitation, 3 lettres majuscules
     * @param zone zone tarifaire de la station
     * @throws IllegalArgumentException si le nom est vide ou le code malformé
     */
    public Station(String nom, String code, Zone zone) {
        // Délégation au constructeur « complet » : pas de duplication de code.
        this(nom, code, zone, false);
    }

    /**
     * Constructeur complet : toutes les caractéristiques de la station.
     *
     * @param nom        nom de la station, non vide
     * @param code       code d'exploitation, exactement 3 lettres majuscules
     * @param zone       zone tarifaire de la station
     * @param accessible {@code true} si la station est accessible PMR
     * @throws IllegalArgumentException si le nom est vide, le code malformé ou la zone absente
     */
    public Station(String nom, String code, Zone zone, boolean accessible) {
        // Validation centralisée : tout objet Station qui existe est valide.
        setNom(nom);
        setCode(code);
        setZone(zone);
        this.accessible = accessible;
        compteurInstances++;
    }

    /**
     * Nombre total de stations créées dans cette JVM (statistique de classe).
     *
     * <p>Méthode {@code static} : elle s'appelle sur la classe, pas sur une
     * instance — {@code Station.nombreStations()}, jamais
     * {@code gare.nombreStations()}.
     *
     * @return le nombre de stations construites depuis le démarrage
     */
    public static int nombreStations() {
        return compteurInstances;
    }

    /**
     * @return le nom commercial de la station
     */
    public String getNom() {
        return nom;
    }

    /**
     * Change le nom de la station.
     *
     * @param nom le nouveau nom, non vide
     * @throws IllegalArgumentException si le nom est {@code null} ou blanc
     */
    public void setNom(String nom) {
        if (nom == null || nom.isBlank()) {
            // Sans cette garde, une station « fantôme » au nom vide circulerait
            // dans tout le programme et exploserait beaucoup plus loin, au
            // moment le plus difficile à diagnostiquer.
            throw new IllegalArgumentException("Le nom d'une station est obligatoire.");
        }
        this.nom = nom;
    }

    /**
     * @return le code d'exploitation (3 lettres majuscules)
     */
    public String getCode() {
        return code;
    }

    /**
     * Change le code d'exploitation de la station.
     *
     * @param code le nouveau code : exactement 3 lettres majuscules (ex. {@code "GAR"})
     * @throws IllegalArgumentException si le code ne respecte pas le format
     */
    public void setCode(String code) {
        if (code == null || !code.matches("[A-Z]{3}")) {
            throw new IllegalArgumentException(
                    "Le code d'une station doit compter exactement 3 lettres majuscules, reçu : " + code);
        }
        this.code = code;
    }

    /**
     * @return {@code true} si la station est accessible aux personnes à mobilité réduite
     */
    public boolean isAccessible() {
        return accessible;
    }

    /**
     * Alias français de {@link #isAccessible()} — purement pédagogique :
     * il rappelle que la langue des identifiants est un choix d'équipe.
     * Les deux méthodes lisent le même attribut ; en production, on n'en
     * garde qu'un seul.
     *
     * @return {@code true} si la station est accessible PMR
     */
    public boolean estAccessible() {
        return accessible;
    }

    /**
     * Définit l'accessibilité PMR de la station.
     *
     * @param accessible {@code true} pour une station accessible
     */
    public void setAccessible(boolean accessible) {
        this.accessible = accessible;
    }

    /**
     * @return la zone tarifaire de la station
     */
    public Zone getZone() {
        return zone;
    }

    /**
     * Rattache la station à une nouvelle zone tarifaire.
     *
     * <p>Pédagogie : le setter valide avant d'affecter — la zone ne peut
     * jamais être {@code null} dans un objet {@code Station} vivant.
     *
     * @param zone la nouvelle zone, obligatoire
     * @throws IllegalArgumentException si la zone est {@code null}
     */
    public void setZone(Zone zone) {
        if (zone == null) {
            throw new IllegalArgumentException("Une station doit appartenir à une zone.");
        }
        this.zone = zone;
    }

    /**
     * Représentation lisible, ex. {@code "Station[GAR — Gare Centrale, Zone 1, ♿]"}.
     *
     * <p>Le pictogramme {@code ♿} n'apparaît que si la station est
     * accessible : {@code toString()} montre l'état utile, pas tous les champs.
     *
     * @return le libellé de la station
     */
    @Override
    public String toString() {
        StringBuilder libelle = new StringBuilder("Station[")
                .append(code).append(" — ").append(nom)
                .append(", Zone ").append(zone.getNumero());
        if (accessible) {
            libelle.append(", \u267F"); // ♿ : le symbole passe mieux en escape Unicode qu'en caractère brut
        }
        return libelle.append(']').toString();
    }
}
