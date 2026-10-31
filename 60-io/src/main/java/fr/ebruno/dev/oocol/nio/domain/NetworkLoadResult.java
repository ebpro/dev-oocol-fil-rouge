package fr.ebruno.dev.oocol.nio.domain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Le contenu d'un réseau tel qu'il a été lu dans les trois fichiers CSV —
 * le résultat brut d'un {@link NetworkLoader#charger(java.nio.file.Path) chargement},
 * sans index ni invariants d'exploitation.
 *
 * <p><strong>Rôle.</strong> Ce type est l'exemple du « DTO de
 * lecture » : les listes sont défensivement copiées à la construction puis
 * exposées en vues non modifiables (contrat du module 40 — le chargeur garde la
 * maîtrise de ce que contiennent les collections). Un {@code toString}
 * condensé permet d'afficher un chargement entier en une ligne de log.
 *
 * @author Emmanuel Bruno
 * @see NetworkLoader
 */
public class NetworkLoadResult {

    /** Zones lues dans {@code zones.csv}, dans l'ordre du fichier. */
    private final List<Zone> zones;

    /** Arrêts lus dans {@code stations.csv}, dans l'ordre du fichier. */
    private final List<Station> stations;

    /** Lignes lues dans {@code lignes.csv}, arrêts résolus. */
    private final List<LineData> lignes;

    /**
     * Assemble un résultat de chargement.
     *
     * @param zones    zones tarifaires lues
     * @param stations arrêts lus
     * @param lignes   lignes lues et résolues
     * @throws IllegalArgumentException si l'une des listes est {@code null}
     */
    public NetworkLoadResult(List<Zone> zones, List<Station> stations, List<LineData> lignes) {
        if (zones == null || stations == null || lignes == null) {
            throw new IllegalArgumentException("Un résultat de chargement comporte les trois listes.");
        }
        this.zones = new ArrayList<>(zones);
        this.stations = new ArrayList<>(stations);
        this.lignes = new ArrayList<>(lignes);
    }

    /**
     * @return les zones lues (vue non modifiable)
     */
    public List<Zone> getZones() {
        return Collections.unmodifiableList(zones);
    }

    /**
     * @return les arrêts lus (vue non modifiable)
     */
    public List<Station> getStations() {
        return Collections.unmodifiableList(stations);
    }

    /**
     * @return les lignes lues, arrêts résolus (vue non modifiable)
     */
    public List<LineData> getLignes() {
        return Collections.unmodifiableList(lignes);
    }

    /**
     * Résumé en une ligne : {@code "réseau : 3 zones, 7 arrêts, 3 lignes"}.
     *
     * @return le condensé du chargement
     */
    @Override
    public String toString() {
        return "réseau : " + zones.size() + " zones, " + stations.size()
                + " arr\u00EAts, " + lignes.size() + " lignes";
    }
}
