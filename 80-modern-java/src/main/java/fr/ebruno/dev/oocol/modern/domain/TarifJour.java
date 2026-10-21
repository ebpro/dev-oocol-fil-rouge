package fr.ebruno.dev.oocol.modern.domain;

import java.util.List;

/**
 * Ticket journalier multi-zones — variante de {@link Tarif} avec une collection.
 *
 * <p>Même leçon défensive que {@link Ligne} : si le prix moyen du réseau
 * dépendait encore de la liste interne, une mutation par-derrière fausserait
 * les statistiques. La copie immuable est le réflexe standard des records à
 * composant collection.
 *
 * @param zonesValides les zones couvertes ce jour-là (copiées à la construction)
 * @param prix         le prix journalier, en euros
 * @author Emmanuel Bruno
 */
public record TarifJour(List<Zone> zonesValides, double prix) implements Tarif {

    /**
     * Constructeur compact : copie défensive de la liste des zones.
     */
    public TarifJour {
        zonesValides = List.copyOf(zonesValides);
    }
}
