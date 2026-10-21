package fr.ebruno.dev.oocol.modern.domain;

/**
 * Une zone tarifaire du réseau « Trans'U » — version <em>record</em>.
 *
 * <p><strong>Un record = un POJO immuable en une ligne.</strong> Comparez avec
 * le {@code Zone} du module 70 : champs {@code private final}, constructeur
 * public, deux getters, {@code equals}, {@code hashCode} et {@code toString}
 * écrits à la main — quarante lignes de plomberie. Ici, la déclaration de la
 * liste de composants ({@code int numero, String nom}) suffit : le compilateur
 * génère le constructeur canonique, les accesseurs (qui s'appellent
 * {@code numero()} et {@code nom()}, plus {@code getNumero()}), l'égalité
 * structurelle, le hachage et l'affichage. Avant : 40 lignes (W2-W8),
 * maintenant : 5.
 *
 * <p><strong>Le constructeur compact</strong> (bloc {@code public Zone { … }}
 * sans signature répétée) est la porte d'entrée unique : il s'exécute
 * <em>avant</em> l'affectation des champs, donc aucune instance invalide ne
 * peut exister. C'est le constructeur gardien des semaines précédentes,
 * enfin imposé par le langage — la désérialisation par-dessus (module 70)
 * ne peut plus le contourner, le record ayant son propre format géré par le
 * compilateur autour de ce constructeur.
 *
 * <p><strong>Immutabilité vraie.</strong> Les composants sont {@code final}
 * par construction : pas de setter, pas de mutation possible. Un
 * {@code Zone} créé invalide n'existe pas ; un {@code Zone} créé valide le
 * reste pour toujours.
 *
 * @author Emmanuel Bruno
 * @see Station
 * @see Ligne
 */
public record Zone(int numero, String nom) {

    /**
     * Constructeur compact : validation exécutée avant l'affectation des composants.
     *
     * @throws IllegalArgumentException si le numéro est inférieur à 1 ou si le nom est vide
     */
    public Zone {
        if (numero < 1) {
            throw new IllegalArgumentException("numero >= 1, reçu : " + numero);
        }
        if (nom == null || nom.isBlank()) {
            throw new IllegalArgumentException("nom requis");
        }
    }
}
