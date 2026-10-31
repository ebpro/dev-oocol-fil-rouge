package fr.ebruno.dev.oocol.heritage.domain;

/**
 * Une ligne de service dans ce module : un nom commercial et UN véhicule
 * affecté. Ce n'est pas la {@code Line} du module 20 (deux stations, une
 * fréquence) : le domaine est rejoué dans chaque module avec une structure
 * différente. La ligne illustre ici <em>ce qui est composition, ce qui est
 * héritage</em>.
 *
 * <p><strong>Pourquoi {@code Line} ne peut PAS étendre {@link Vehicle}
 * (démonstration LSP) :</strong> la tentation existe — une ligne « roule »,
 * « s'annonce », « circule » comme un véhicule, hériter rendrait tous ces
 * verbes disponibles gratuitement. Mais l'héritage est un contrat de
 * <em>substituabilité</em> (Liskov Substitution Principle) : si
 * {@code Line extends Vehicle}, alors le code suivant EST COMPILABLE :
 *
 * <pre>{@code
 * serviceAnnonce(new Line("Ligne T1", unTramway)); // compile… et c'est absurde
 * }</pre>
 *
 * … alors que {@code serviceAnnonce(Vehicle)} promet un véhicule — afficher
 * « Véhicule Ligne T1 — [propulsion du véhicule affecté ?] — [capacité de
 * qui ?] » n'a aucun sens : la ligne n'a pas de propulsion propre, pas de
 * capacité propre. Le sous-type brise la garantie du sur-type : c'est
 * exactement une violation de LSP, et le compilateur ne peut pas nous en
 * protéger — seule la modélisation le fait.
 *
 * <p><strong>La bonne relation est la composition :</strong> une ligne A UN
 * véhicule affecté ({@code private Vehicle vehiculeAffecte}). Le UML s'en
 * souvient : losange plein (composition) et non flèche creuse (héritage).
 * Avantage mesurable : {@link #annoncerService()} <em>délègue</em> au
 * véhicule — le polymorphisme travaille pour la ligne sans que la ligne
 * soit un véhicule.
 *
 * @author Emmanuel Bruno
 * @see Vehicle
 */
public class Line {

    /** Nom commercial de la ligne (ex. {@code "Ligne T1"}). */
    private String nom;

    /**
     * Le véhicule affecté au service. COMPOSITION : la ligne <em>tient</em>
     * un véhicule, elle n'<em>est</em> pas un véhicule. Typé sur le sur-type
     * {@link Vehicle} : n'importe quelle sous- classe convient, et la ligne
     * n'a rien à savoir de plus.
     */
    private Vehicle vehiculeAffecte;

    /**
     * Construit une ligne avec son véhicule de service.
     *
     * @param nom             nom de la ligne, non vide
     * @param vehiculeAffecte le véhicule affecté, obligatoire (une ligne sans véhicule ne roule pas)
     * @throws IllegalArgumentException si le nom est vide ou le véhicule absent
     */
    public Line(String nom, Vehicle vehiculeAffecte) {
        setNom(nom);
        setVehiculeAffecte(vehiculeAffecte);
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
     * @return le véhicule actuellement affecté au service
     */
    public Vehicle getVehiculeAffecte() {
        return vehiculeAffecte;
    }

    /**
     * Affecte un véhicule au service (le matériel roule, la ligne reste).
     *
     * @param vehiculeAffecte le nouveau véhicule, obligatoire
     * @throws IllegalArgumentException si le véhicule est {@code null}
     */
    public void setVehiculeAffecte(Vehicle vehiculeAffecte) {
        if (vehiculeAffecte == null) {
            throw new IllegalArgumentException("Une ligne a un véhicule affecté.");
        }
        this.vehiculeAffecte = vehiculeAffecte;
    }

    /**
     * Annonce le service en station — par <strong>délégation</strong>.
     *
     * <p>La ligne ne sait pas décrire une propulsion, une capacité, une
     * immatriculation : elle <em>transmet</em> la demande à son véhicule
     * affecté. C'est le pendant « entre objets liés par composition » de ce
     * que l'héritage fait « entre objets liés par une hiérarchie » : le
     * travail est rendu par l'objet qui sait le faire. La substitution du
     * tramway par un bus ( {@link #setVehiculeAffecte} ) change
     * l'annonce sans changer une ligne de cette méthode — le polymorphisme
     * traverse la composition.
     *
     * @return l'annonce du véhicule affecté (voir {@link Vehicle#annoncer()})
     */
    public String annoncerService() {
        return vehiculeAffecte.annoncer();
    }

    /**
     * Représentation lisible : {@code "Ligne T1 (service : Vehicle[M-101, ...])"}.
     *
     * @return le libellé de la ligne et de son véhicule affecté
     */
    @Override
    public String toString() {
        return nom + " (service : " + vehiculeAffecte + ")";
    }
}
