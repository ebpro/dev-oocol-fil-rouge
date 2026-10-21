package fr.ebruno.dev.oocol.pitfalls.fixed;

import java.util.HashMap;
import java.util.Map;

/**
 * La version <b>corrigée</b> de {@link
 * fr.ebruno.dev.oocol.pitfalls.bugs.BuggyTicketCache} : les clés sont des
 * {@link FixedStation}, dont {@code hashCode} est cohérent avec
 * {@code equals} — le cache retrouve enfin ses propres entrées.
 *
 * <p>Double leçon : (1) la correction est dans la <em>clé</em>, pas dans le
 * cache ; (2) une clé de Map doit être immuable — ici {@code FixedStation}
 * est {@code final} avec champs {@code final}, donc son hachage ne bouge
 * jamais entre le {@code put} et le {@code get}.</p>
 *
 * @author Emmanuel Bruno
 */
public class FixedTicketCache {

    private final Map<FixedStation, Double> prixCache = new HashMap<>();

    /** Mémorise le prix d'un arrêt. */
    public void cache(FixedStation station, double prix) {
        prixCache.put(station, prix);
    }

    /**
     * Retrouve le prix d'un arrêt.
     *
     * <p>FIX: une station « égale » (même code, autre instance) retrouve le
     * prix stocké — {@code hashCode} cohérent = même seau, {@code equals} =
     * bonne entrée.</p>
     */
    public Double lookup(FixedStation station) {
        return prixCache.get(station);
    }

    /** Nombre d'entrées du cache (deux clés égales n'en font qu'une). */
    public int taille() {
        return prixCache.size();
    }
}
