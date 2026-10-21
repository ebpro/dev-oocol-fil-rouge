package fr.ebruno.dev.oocol.collections.domain;

/**
 * Une zone tarifaire du réseau « Trans'U » (ex. zone 1 = centre-ville,
 * zone 2 = périphérie…).
 *
 * <p><strong>Instantané W5 — la zone gagne un ordre naturel.</strong> Les
 * attributs, la validation et le contrat {@code equals}/{@code hashCode}
 * (par numéro) sont repris du module 20 ; la nouveauté est l'implémentation
 * de {@link Comparable} : {@code Zone implements Comparable<Zone>}, avec la
 * comparaison portée par le numéro. Conséquence immédiate : une
 * {@code List<Zone>} peut être triée « sans rien dire » par
 * {@code Collections.sort(liste)} — c'est l'ordre naturel que découvre alors
 * la méthode de tri. Le {@link FareCalculator} s'en sert pour ranger les
 * zones du réseau, et le cours montre en miroir la seconde façon de trier :
 * un {@link java.util.Comparator} fourni par l'appelant.
 *
 * <p>Pédagogie du {@code compareTo} : la contract est un signe —
 * {@code négatif / zéro / positif} selon que {@code this} passe
 * <em>avant</em>, <em>comme</em> ou <em>après</em> l'argument. Comme pour
 * {@code equals}, la cohérence avec l'égalité compte : ici deux zones de
 * même numéro comparent à {@code 0}, exactement ce que dit
 * {@link #equals(Object)}.
 *
 * @author Emmanuel Bruno
 * @see FareCalculator
 * @see Station
 */
public class Zone implements Comparable<Zone> {

    /** Numéro de zone (1, 2, 3…). Immuable : écrit uniquement dans le constructeur. */
    private final int numero;

    /** Nom explicite de la zone (ex. {@code "Centre-ville"}). Immuable. */
    private final String nom;

    /**
     * Crée une zone tarifaire valide.
     *
     * <p>Validation dans le constructeur (principe du constructeur gardien,
     * hérité du module 20) : un objet {@code Zone} qui existe est un objet valide.
     *
     * @param numero numéro de zone, strictement positif
     * @param nom    nom de la zone, non vide
     * @throws IllegalArgumentException si le numéro est négatif ou nul, ou si le nom est vide
     */
    public Zone(int numero, String nom) {
        if (numero <= 0) {
            throw new IllegalArgumentException(
                    "Le numéro de zone doit être strictement positif, reçu : " + numero);
        }
        if (nom == null || nom.isBlank()) {
            throw new IllegalArgumentException("Le nom de zone est obligatoire.");
        }
        this.numero = numero;
        this.nom = nom;
    }

    /**
     * @return le numéro de la zone (1, 2, 3…)
     */
    public int getNumero() {
        return numero;
    }

    /**
     * @return le nom explicite de la zone
     */
    public String getNom() {
        return nom;
    }

    /**
     * Ordre naturel des zones : par numéro croissant.
     *
     * <p>C'est CETTE méthode que {@code Collections.sort(List<Zone>)} appelle,
     * sans qu'on lui fournisse aucun comparateur — le « tri par défaut » vient
     * de l'objet trié, pas du trieur.
     *
     * @param autre la zone comparée
     * @return un négatif si cette zone est moins chère (numéro plus petit),
     *         {@code 0} si même numéro, un positif sinon
     */
    @Override
    public int compareTo(Zone autre) {
        // Astuce sans méthode de comparaison « moderne » : la soustraction des
        // entiers donne le signe voulu. (Ici les numéros restent petits :
        // pas de risque de débordement — limite à discuter en séance.)
        return this.numero - autre.numero;
    }

    /**
     * Représentation lisible : {@code "Zone 1 — Centre-ville"}.
     *
     * @return le libellé complet de la zone
     */
    @Override
    public String toString() {
        return "Zone " + numero + " — " + nom;
    }

    /**
     * Deux zones sont égales quand elles ont le même numéro (contrat W3 repris).
     *
     * @param o l'objet à comparer
     * @return {@code true} si {@code o} est une zone de même numéro
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true; // la même référence : succès rapide, cas fréquent
        }
        if (o == null || getClass() != o.getClass()) {
            return false; // null ou une autre classe ne peut pas être égale
        }
        Zone autre = (Zone) o;
        return numero == autre.numero;
    }

    /**
     * Code de hachage cohérent avec {@link #equals(Object)} : basé sur le
     * numéro uniquement — la règle que le module 40 rend enfin visible,
     * puisque les zones voyagent désormais dans des collections.
     *
     * @return le code de hachage de la zone
     */
    @Override
    public int hashCode() {
        return Integer.hashCode(numero);
    }
}
