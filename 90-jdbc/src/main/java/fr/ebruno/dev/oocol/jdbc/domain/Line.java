package fr.ebruno.dev.oocol.jdbc.domain;

import java.util.List;
import java.util.Objects;

/**
 * Une ligne du réseau Trans'U, définie par son nom et la liste ordonnée de ses arrêts.
 *
 * <p>Côté base de données, une ligne, c'est <em>deux tables</em> :
 * {@code lignes} (le nom) et {@code ligne_arrets} (la relation N:N avec les
 * stations, ordinalisée par la colonne {@code position}). Reconstruire le
 * record depuis ces deux tables — et l'inverse — est exactement le travail
 * d'un DAO sur une relation complexe.
 *
 * @param nom          nom de la ligne, clé primaire de la table {@code lignes} (ex. {@code "A"})
 * @param stationCodes codes des arrêts, dans l'ordre de parcours (copie défensive immuable)
 */
public record Line(String nom, List<String> stationCodes) {

    /**
     * Constructeur compact : validation et copie défensive.
     *
     * @throws NullPointerException     si {@code nom} ou {@code stationCodes} est {@code null}
     * @throws IllegalArgumentException si {@code stationCodes} contient un élément {@code null}
     */
    public Line {
        Objects.requireNonNull(nom, "Le nom d'une ligne ne peut pas être null");
        Objects.requireNonNull(stationCodes, "La liste des arrêts ne peut pas être null");
        // Copie défensive : le record reste immuable même si l'appelant mute sa List d'origine.
        stationCodes = List.copyOf(stationCodes);
    }
}
