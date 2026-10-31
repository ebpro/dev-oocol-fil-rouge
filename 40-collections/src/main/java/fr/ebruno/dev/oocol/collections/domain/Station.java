package fr.ebruno.dev.oocol.collections.domain;

import java.util.Comparator;

/**
 * Une station du réseau « Trans'U », identifiée par son code d'exploitation
 * à trois lettres majuscules (ex. {@code "GAR"} pour Gare Centrale).
 *
 * <p><strong>La classe du bug motivateur.</strong> La station n'est plus
 * seulement un objet encapsulé ; elle devient une <em>clé de collection</em> :
 * elle entre dans des {@code HashSet}, sert d'index dans des
 * {@code HashMap} et se cherche dans des {@code List}. Or une collection de
 * hachage ne compare pas les objets champ par champ : elle demande leur
 * {@code hashCode()} pour choisir « dans quel tiroir regarder », puis leur
 * {@code equals()} pour trancher entre les voisins du tiroir. Sans cette
 * paire redéfinie, c'est {@code Object.equals} qui répond — une comparaison
 * d'<em>identité référence</em> — et {@code contains()} <strong>rate
 * silencieusement</strong> une station présente sous un autre exemplaire.
 * Aucun message d'erreur, juste un {@code false} injustifié : le pire genre
 * de bug. D'où le contrat {@code equals}/{@code hashCode} ci-dessous.
 *
 * <p>Choix métier explicite : <strong>la clé d'identité est le
 * {@code code}</strong>, pas le {@code nom}. Deux objets {@code Station} de
 * code {@code "GAR"} sont la même station, même si l'un affiche un nom
 * orthographié autrement (le nom est une donnée d'affichage, le code est
 * l'identifiant métier). Le {@code code} devient donc {@code final} : une
 * clé qui a changé de valeur une fois rangée dans un {@code HashSet} devient
 * introuvable — le tiroir calculé à l'insertion ne correspond plus à celui
 * consulté à la recherche. Une clé de hachage doit être immuable ; c'est
 * aussi pour ça que {@code setCode} a disparu de cette classe.
 *
 * <p>La station gagne aussi deux façons d'être ordonnée :
 * <ul>
 *   <li>l'ordre <strong>naturel</strong> ({@link Comparable}), par code —
 *       c'est l'ordre « par défaut » d'une {@code List<Station>} triée ;</li>
 *   <li>l'ordre <strong>délégué</strong> ({@link #parNom()}), par nom sans
 *       tenir compte de la casse — un {@code Comparator} écrit en classe
 *       interne <em>anonyme</em>, la forme « avant les lambdas », qui dit
 *       exactement ce qu'un lambda exprime.</li>
 * </ul>
 *
 * @author Emmanuel Bruno
 * @see Line
 * @see Network
 * @see Zone
 */
public class Station implements Comparable<Station> {

    /**
     * Code d'exploitation : exactement 3 lettres majuscules (ex. {@code "GAR"}).
     *
     * <p>Champ {@code final} : c'est la clé d'identité de la station, donc la
     * clé de hachage de toutes les collections qui la contiennent. Modifier
     * une clé après insertion « perd » l'objet dans la table — le setter
     * {@code setCode} du module 20 est volontairement supprimé ici.
     */
    private final String code;

    /** Nom commercial de la station (ex. {@code "Gare Centrale"}). Modifiable : donnée d'affichage. */
    private String nom;

    /** Accessibilité PMR. {@code false} par défaut (constructeur court). */
    private boolean accessible;

    /** Zone tarifaire à laquelle la station est rattachée. Modifiable : une station peut être re-zonée. */
    private Zone zone;

    /**
     * Constructeur « pratique » : une station non accessible (cas par défaut).
     *
     * @param nom  nom de la station, non vide
     * @param code code d'exploitation, 3 lettres majuscules
     * @param zone zone tarifaire de la station
     * @throws IllegalArgumentException si le nom est vide ou le code malformé
     */
    public Station(String nom, String code, Zone zone) {
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
        setNom(nom);
        // Le code est final : la validation lui est déléguée et RETOURNE la
        // valeur contrôlée — un champ final ne s'assigne qu'ici, dans le
        // constructeur (une méthode auxiliaire n'en a pas le droit).
        this.code = codeValide(code);
        setZone(zone);
        this.accessible = accessible;
    }

    /**
     * Fabrique statique de convenance : {@code Station.of("GAR", "Gare Centrale", zone1)}.
     *
     * <p>Le code passe <em>en premier</em> ici, contrairement au constructeur :
     * choix délibéré, puisque le code EST l'identité — quand on nomme une
     * station, on commence par son identifiant métier. Une fabrique ne fait
     * que passer un message au constructeur ; elle ne remplace pas le
     * {@code new}, elle le masque derrière une intention.
     *
     * @param code code d'exploitation, 3 lettres majuscules
     * @param nom  nom de la station, non vide
     * @param zone zone tarifaire de la station
     * @return une station valide toute neuve
     * @throws IllegalArgumentException si un argument est invalide
     */
    public static Station of(String code, String nom, Zone zone) {
        return new Station(nom, code, zone);
    }

    /**
     * Comparateur « par nom », sans tenir compte de la casse.
     *
     * <p>Écrit en <strong>classe interne anonyme</strong> — la syntaxe
     * d'avant les lambdas : un {@code new Comparator<Station>() {...}} qui
     * implémente {@code compare} à la volée. Le module 80 écrira la même
     * chose en une ligne ; pour l'instant, la version longue garde visible
     * le fait qu'un {@code Comparator} est un <em>objet</em> qu'on passe,
     * pas un mot-clé du langage.
     *
     * @return un comparateur qui range les stations par nom alphabétique,
     *         majuscules et minuscules confondues
     */
    public static Comparator<Station> parNom() {
        return new Comparator<Station>() {
            @Override
            public int compare(Station a, Station b) {
                // compareToIgnoreCase : « Gare » et « gare » se valent à l'ordre près.
                return a.nom.compareToIgnoreCase(b.nom);
            }
        };
    }

    /**
     * Ordre naturel d'une station : par code alphabétique ({@code "AER" < "GAR"}).
     *
     * <p>C'est l'ordre que {@code Collections.sort(List<Station>)} applique
     * sans qu'on lui fournisse de comparateur — la station « sait » comment
     * se ranger. Cohérent avec {@link #equals(Object)} : deux stations
     * comparent à {@code 0} si et seulement si elles sont égales.
     *
     * @param autre la station comparée
     * @return un négatif, {@code 0}, ou un positif selon l'ordre des codes
     */
    @Override
    public int compareTo(Station autre) {
        return this.code.compareTo(autre.code);
    }

    /**
     * @return le nom commercial de la station
     */
    public String getNom() {
        return nom;
    }

    /**
     * Change le nom de la station (donnée d'affichage : sans effet sur l'identité).
     *
     * @param nom le nouveau nom, non vide
     * @throws IllegalArgumentException si le nom est {@code null} ou blanc
     */
    public void setNom(String nom) {
        if (nom == null || nom.isBlank()) {
            throw new IllegalArgumentException("Le nom d'une station est obligatoire.");
        }
        this.nom = nom;
    }

    /**
     * @return le code d'exploitation (3 lettres majuscules) — la clé d'identité
     */
    public String getCode() {
        return code;
    }

    /**
     * Validation du code, appelée par le constructeur seul — le champ étant
     * {@code final}, il n'existe plus de setter public (voir le Javadoc de
     * {@link #code}). Statique et retournant la valeur : c'est la forme
     * qu'impose {@code final} — valider AVANT d'assigner, dans le constructeur.
     *
     * @param code le code à valider
     * @return le code, s'il est conforme
     * @throws IllegalArgumentException si le code ne respecte pas le format
     */
    private static String codeValide(String code) {
        if (code == null || !code.matches("[A-Z]{3}")) {
            throw new IllegalArgumentException(
                    "Le code d'une station doit compter exactement 3 lettres majuscules, reçu : " + code);
        }
        return code;
    }

    /**
     * @return {@code true} si la station est accessible aux personnes à mobilité réduite
     */
    public boolean isAccessible() {
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
     * Deux stations sont la MÊME station quand elles ont le même code.
     *
     * <p><strong>Sans cette paire (equals + hashCode), {@code HashSet.contains()}
     * et {@code HashMap.get()} ratent silencieusement — c'est le bug
     * motivant.</strong> Concrètement : {@code Line.retirerArret} renverrait
     * {@code false} pour un arrêt pourtant présent, {@code Network} verrait
     * la même station sous deux clés différentes, et le BFS du
     * {@code TripPlanner} bouclerait sur des doublons. La méthode
     * {@link Line#demontrerBugSansEquals()} joue la scène en entier.
     *
     * <p>Le {@code nom}, l'accessibilité et la zone n'entrent PAS dans la
     * comparaison : le code est l'identifiant métier (cf. Javadoc de classe).
     *
     * @param o l'objet à comparer
     * @return {@code true} si {@code o} est une station de même code
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true; // la même référence : succès rapide, cas fréquent
        }
        if (o == null || getClass() != o.getClass()) {
            return false; // null ou une autre classe ne peut pas être égale
        }
        Station autre = (Station) o;
        return code.equals(autre.code);
    }

    /**
     * Code de hachage <em>cohérent</em> avec {@link #equals(Object)} : dérivé
     * du code uniquement.
     *
     * <p>Règle d'or : deux objets égaux DOIVENT avoir le même {@code hashCode}
     * — sinon la collection de hachage range l'objet dans un tiroir et le
     * cherche dans un autre. L'inverse n'est pas exigé (deux inégaux peuvent
     * partager un tiroir : {@code equals} tranche alors).
     *
     * @return le code de hachage de la station
     */
    @Override
    public int hashCode() {
        return code.hashCode();
    }

    /**
     * Représentation lisible, ex. {@code "Station[GAR — Gare Centrale, Zone 1, ♿]"}.
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
