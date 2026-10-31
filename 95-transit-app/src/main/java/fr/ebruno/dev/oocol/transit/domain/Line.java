package fr.ebruno.dev.oocol.transit.domain;

import java.util.List;

/**
 * Une ligne du réseau : un nom et la liste ORDONNÉE de ses arrêts.
 *
 * <p><strong>L'immuabilité défensive du module 80, appliquée aux collections
 * du module 40.</strong> Un record protège ses composants par affectation,
 * mais une {@code List} transmise au constructeur reste partageable :
 * l'appelant qui modifie sa liste après coup modifierait la ligne « par
 * télécommande ». Le constructeur compact applique donc {@link List#copyOf} :
 * copie défensive, immuable, qui refuse en plus les {@code null}. Rappelé par le module 85 (fuites mutables) : un objet immuable qui
 * encapsule une collection mutable ne l'est pas.
 *
 * @param nom    identifiant de la ligne (ex. {@code "Ligne A"})
 * @param arrets les arrêts desservis, dans l'ordre de parcours (copie immuable)
 * @author Emmanuel Bruno
 */
public record Line(String nom, List<Station> arrets) {

    /**
     * Constructeur compact : la liste est copiée puis rendue immuable.
     *
     * @throws NullPointerException si {@code arrets} ou l'un de ses éléments est {@code null}
     */
    public Line {
        arrets = List.copyOf(arrets);
    }
}
