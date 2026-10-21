package fr.ebruno.dev.oocol.modern.domain;

/**
 * Le matériel roulant du réseau — hiérarchie <em>scellée</em> (module 30, rejoué).
 *
 * <p><strong>Ce que {@code sealed} change.</strong> Au module 30,
 * {@code Vehicle} était une classe abstraite ouverte : n'importe qui pouvait
 * l'étendre, et le code qui traite un {@code Vehicle} devait prévoir
 * l'inconnu (le {@code else} final qui lance {@code IllegalStateException}
 * « type inattendu » — une branche morte par construction, un aveu
 * d'impuissance). Ici, la liste des implémentations est <em>écrite dans la
 * déclaration</em> ({@code permits MetroTrain, Bus, Tramway}) : le compilateur
 * la connaît, et tout switch sur un {@code Vehicle} doit couvrir exactement
 * ces trois cas — pas un de moins (faute de compilation), pas besoin d'un de
 * plus (le {@code default} devient une erreur d'alerte rouge).
 *
 * <p><strong>Interface plutôt que classe abstraite.</strong> Les trois
 * permissionnaires sont des {@code record}s ; un record ne peut pas hériter
 * d'une classe. L'abstraction devient donc une interface — et y conserve un
 * comportement utile : {@link #decrire()} y a une implémentation par défaut, ce que
 * la classe abstraite du module 30 faisait déjà, sans imposer l'héritage.
 *
 * <p><strong>Contrat scellé, extensions contrôlées.</strong> Ajouter un
 * {@code Funiculaire} est possible — mais c'est une décision : il faut
 * toucher la clause {@code permits} ET tous les switchs exhaustifs. Le
 * compilateur tient le registre des points d'impact. C'est exactement le
 * comportement que le module 50 simulait avec des exceptions.
 *
 * @author Emmanuel Bruno
 * @see MetroTrain
 * @see Bus
 * @see Tramway
 */
public sealed interface Vehicle permits MetroTrain, Bus, Tramway {

    /**
     * Identifiant technique du véhicule (parc).
     *
     * @return l'identifiant, jamais {@code null}
     */
    String identifiant();

    /**
     * Capacité en voyageurs.
     *
     * @return le nombre de places
     */
    int capacite();

    /**
     * Description lisible du véhicule.
     *
     * <p><strong>Default method.</strong> Le module 30 payait ce service en
     * couple abstract/concrete : une méthode abstraite obligatoire plus un
     * fourre-tout concret hérité. Une {@code default} d'interface donne le
     * comportement générique aux trois permissionnaires, qui ne surchargent
     * que ce qui les distingue vraiment.
     *
     * @return un libellé « identifiant (N places) »
     */
    default String decrire() {
        return identifiant() + " (" + capacite() + " places)";
    }
}
