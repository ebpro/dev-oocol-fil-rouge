package fr.ebruno.dev.oocol.modern.domain;

import java.util.List;
import java.util.Optional;

/**
 * Une ligne du réseau : un nom et une liste d'arrêts — record à composant collection.
 *
 * <p><strong>Le piège que le constructeur compact désamorce.</strong> Un
 * record rend ses composants accessibles, mais ne les rend pas immuables
 * <em>pour de bon</em> si le composant est mutable : passer un
 * {@code ArrayList} au constructeur laisserait l'appelant modifier la liste
 * <em>après coup</em>, « par-derrière » l'objet censé être figé. La parade
 * tient en une ligne : {@code List.copyOf} dans le constructeur compact —
 * copie défensive plus liste non modifiable. C'est la leçon d'encapsulation
 * des semaines 3-5, enfin pliable en une instruction.
 *
 * <p><strong>{@link Optional} au lieu de {@code null}.</strong>
 * {@link #trouverParCode} renvoie un {@code Optional<Station>} : le module 50
 * avait enseigné à ne pas renvoyer {@code null} sans le dire ; Java 21 sait
 * le dire dans le type même de la méthode, et l'appelant est forcé de
 * traiter l'absence (cf. scène 5 de la démonstration).
 *
 * @author Emmanuel Bruno
 * @see Station
 * @see AnalyseReseau
 */
public record Ligne(String nom, List<Station> arrets) {

    /**
     * Constructeur compact : copie défensive — la liste interne ne peut plus bouger.
     */
    public Ligne {
        arrets = List.copyOf(arrets);
    }

    /**
     * Recherche un arrêt par son code, sans jamais renvoyer {@code null}.
     *
     * @param code le code d'arrêt recherché (ex. {@code "GAR"})
     * @return l'arrêt trouvé, ou {@link Optional#empty()} si la ligne ne le dessert pas
     */
    public Optional<Station> trouverParCode(String code) {
        return arrets.stream()
                .filter(s -> s.code().equals(code))
                .findFirst();
    }
}
