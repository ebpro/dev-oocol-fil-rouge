package fr.ebruno.dev.oocol.transit.domain;

/**
 * Une zone tarifaire du réseau « Trans'U » — la forme finale du module 95.
 *
 * <p><strong>Treize semaines d'évolution en un record.</strong> Le {@code Zone}
 * du module 20 était une classe avec champs privés, constructeur gardien,
 * getters et {@code equals}/{@code hashCode} écrits à la main. Celui du module
 * 80 devient un record. Ici, la même ligne de déclaration scelle la forme
 * définitive du fil rouge : deux composants, une garde, rien d'autre.
 *
 * <p>SRP (la rétrospective de la phase 5 y repassera) : une zone ne porte
 * qu'une raison de changer — sa définition tarifaire. Ni affichage, ni CSV,
 * ni SQL : tout cela vit ailleurs.
 *
 * @param numero identifiant de la zone (entier positif)
 * @param nom    libellé affiché aux voyageurs
 * @author Emmanuel Bruno
 */
public record Zone(int numero, String nom) {

    /**
     * Constructeur compact : aucune instance invalide ne peut exister.
     *
     * @throws IllegalArgumentException si le numéro est inférieur à 1 ou le nom absent
     */
    public Zone {
        if (numero < 1) {
            throw new IllegalArgumentException("zone >= 1, reçu : " + numero);
        }
        if (nom == null || nom.isBlank()) {
            throw new IllegalArgumentException("nom requis");
        }
    }
}
