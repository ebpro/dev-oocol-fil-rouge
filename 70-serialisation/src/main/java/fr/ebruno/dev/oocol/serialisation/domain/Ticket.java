package fr.ebruno.dev.oocol.serialisation.domain;

import java.io.Externalizable;
import java.io.IOException;
import java.io.InvalidObjectException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

/**
 * Un ticket du réseau, sérialisé « à la main » via {@link Externalizable}.
 *
 * <p><strong>{@code Externalizable} = {@code Serializable} en mode manuel.</strong>
 * La JVM ne touche PLUS aux champs : c'est le codeur qui écrit chaque valeur
 * dans {@link #writeExternal} et les relit dans {@link #readExternal}, dans
 * l'ordre de son choix, avec les primitives du flux ({@code writeUTF},
 * {@code writeDouble}, {@code writeInt}…). Le gain : un contrôle total du
 * format — ici le ticket ne voyage qu'en trois champs aplatis, pas en
 * descripteur de classe + graphe. Le prix, à lire deux fois :
 * <ul>
 *   <li>on réinvente la sérialisation — l'aplatissement de la {@link Zone}
 *     ci-dessous perd déjà son {@code nom} : à la relecture, la zone est
 *     « reconstruite » avec un libellé placeholder ;</li>
 *   <li>le constructeur public sans argument est OBLIGATOIRE : la
 *     désérialisation passe par lui, et sans lui c'est
 *     {@code InvalidClassException: "no valid constructor"} (démontré en test) ;</li>
 *   <li>contre-intuitif mais vrai, démontré en test : le UID est TOUTE FOIS
 *     vérifié — {@code Externalizable} n'immunise PAS contre la rupture de
 *     format. Il change qui écrit les champs, pas la signature du format.</li>
 * </ul>
 *
 * <p><strong>Quand l'utiliser ?</strong> Rarement : format compact et stable
 * exigé, protocole à documenter soi-même. Le défaut raisonnable reste
 * {@code Serializable} + {@code writeObject/readObject} (cf. {@link Station}),
 * qui donne la main sur le contenu SANS retirer celle de la JVM sur le conteneur.
 *
 * @author Emmanuel Bruno
 * @see Station
 * @see Zone
 */
public class Ticket implements Externalizable {

    /**
     * Version du format externe. Comme pour {@code Serializable}, la JVM la
     * compare à la relecture — l'illusion « Externalizable n'a pas de UID
     * utile » est le piège que le test {@code ExternalizableTest} démonte.
     */
    private static final long serialVersionUID = 1L;

    /** Type de ticket (ex. {@code "Unité"}, {@code "Journée"}). */
    private String type;

    /** Prix à l'unité, en euros. Jamais négatif (invariant revalidé à la lecture). */
    private double prix;

    /**
     * Zone de validité. APLATIE dans le flux (seul son numéro voyage) :
     * c'est le choix de {@code Externalizable} — compact, mais le nom de la
     * zone se perd à la relecture.
     */
    private Zone zone;

    /**
     * Constructeur public sans argument — OBLIGATOIRE avec {@code Externalizable} :
     * {@code readExternal} remplit l'objet À PARTIR DE CET INSTANCE vide.
     * Supprimer ce constructeur fait échouer la désérialisation d'un seul coup
     * ({@code InvalidClassException: no valid constructor}), y compris pour des
     * objets écrits avant le changement.
     */
    public Ticket() {
        // requis par le contrat Externalizable — ne jamais retirer
    }

    /**
     * Constructeur complet (privé : les fabriques publiques sont la seule porte d'entrée).
     *
     * @param type type de ticket, non vide
     * @param prix prix en euros, positif ou nul
     * @param zone zone de validité, obligatoire
     */
    private Ticket(String type, double prix, Zone zone) {
        if (type == null || type.isBlank()) {
            throw new IllegalArgumentException("Le type de ticket est obligatoire.");
        }
        if (prix < 0) {
            throw new IllegalArgumentException("Le prix d'un ticket ne peut pas être négatif.");
        }
        if (zone == null) {
            throw new IllegalArgumentException("Un ticket a une zone de validité.");
        }
        this.type = type;
        this.prix = prix;
        this.zone = zone;
    }

    /**
     * Fabrique le ticket « Unité » (voyage unique), prix réseau 1,80 €.
     *
     * @param zone zone de validité
     * @return le ticket Unité
     */
    public static Ticket unite(Zone zone) {
        return new Ticket("Unité", 1.80, zone);
    }

    /**
     * Fabrique le ticket « Journée » (voyages illimités 24 h), prix réseau 5,20 €.
     *
     * @param zone zone de validité
     * @return le ticket Journée
     */
    public static Ticket jour(Zone zone) {
        return new Ticket("Journée", 5.20, zone);
    }

    /**
     * @return le type du ticket
     */
    public String getType() {
        return type;
    }

    /**
     * @return le prix en euros
     */
    public double getPrix() {
        return prix;
    }

    /**
     * @return la zone de validité (nom « reconstruite » après relecture — cf. Javadoc de classe)
     */
    public Zone getZone() {
        return zone;
    }

    /**
     * Écrit le ticket dans le flux, champ par champ, à la main.
     *
     * <p>Ordre d'écriture = ordre de lecture : c'est NOUS qui décidons du
     * format. Ici trois valeurs aplaties : {@code writeUTF} pour le type
     * (chaîne encodée en UTF-8 préfixé), {@code writeDouble}, {@code writeInt}.
     * La {@link Zone} n'est PAS écrite comme objet — seul son numéro voyage.
     *
     * @param out le flux de sortie
     * @throws IOException si le flux refuse l'écriture
     */
    @Override
    public void writeExternal(ObjectOutput out) throws IOException {
        out.writeUTF(type);
        out.writeDouble(prix);
        out.writeInt(zone.getNumero());
    }

    /**
     * Relit le ticket, dans l'ordre exact de {@link #writeExternal}.
     *
     * <p>Revalidation à la lecture, comme dans {@code Station.readObject} :
     * les octets ne sont pas fiables. Un numéro de zone négatif ou un prix
     * sous zéro dans le flux = {@link InvalidObjectException}, pas un objet
     * invalide en mémoire.
     *
     * @param in le flux d'entrée
     * @throws IOException            si la lecture échoue ou si le flux est invalide
     * @throws ClassNotFoundException jamais théoriquement (aucun objet lu), imposé par la signature
     */
    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        type = in.readUTF();
        prix = in.readDouble();
        int numero = in.readInt();
        if (prix < 0) {
            throw new InvalidObjectException("Ticket de prix négatif lu dans le flux : " + prix);
        }
        if (numero <= 0) {
            throw new InvalidObjectException("Ticket sans zone valide, numéro lu : " + numero);
        }
        // Choix assumé de l'aplatissement : le nom de la zone ne voyageait pas, on en fabrique un.
        zone = new Zone(numero, "reconstruite");
    }

    /**
     * Représentation lisible : {@code "Ticket Unité 1,80 € (Zone 1)"}.
     *
     * @return le libellé du ticket
     */
    @Override
    public String toString() {
        return "Ticket " + type + " " + prix + " \u20AC (" + zone.getNom() + " n°"
                + zone.getNumero() + ")";
    }
}
