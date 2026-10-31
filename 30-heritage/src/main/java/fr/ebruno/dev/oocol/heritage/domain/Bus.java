package fr.ebruno.dev.oocol.heritage.domain;

/**
 * Un bus du réseau : véhicule routier diesel ou hybride, avec un nombre de
 * portes d'embarquement.
 *
 * <p><strong>Ce que cette classe démontre :</strong>
 * <ul>
 *   <li>une autre implémentation de {@link Vehicle#decrirePropulsion()} —
 *       même contrat abstrait, réponse totalement différente : c'est tout
 *       l'intérêt du polymorphisme ;</li>
 *   <li>un {@link #copier()} volontairement <em>non covariant</em> : il
 *       renvoie {@code Vehicle}, le type du parent. Comparer avec
 *       {@link MetroTrain#copier()} — le contraste est la leçon ;</li>
 *   <li>une capacité propre ({@link #peutDoubler()}) que le parent ne
 *       connaît pas : le code polymorphe qui manipule des {@code Vehicle}
 *       l'ignore, et c'est très bien ainsi (voir le Javadoc de la méthode).</li>
 * </ul>
 *
 * @author Emmanuel Bruno
 * @see Vehicle
 */
public class Bus extends Vehicle {

    /** {@code true} si le bus est hybride diesel-électrique, {@code false} s'il est diesel pur. */
    private boolean hybride;

    /** Nombre de portes d'embarquement. Au moins 1. */
    private int nombrePortes;

    /**
     * Construit un bus.
     *
     * @param immatriculation   immatriculation du bus, non vide
     * @param capacitePassagers nombre de places, strictement positif (validé par le parent)
     * @param vitesseMaxKmh     vitesse maximale en km/h, strictement positive (validée par le parent)
     * @param hybride           {@code true} pour un bus hybride, {@code false} pour un diesel
     * @param nombrePortes      nombre de portes, au moins 1
     * @throws IllegalArgumentException si le parent rejette un de ses arguments ou si
     *                                  {@code nombrePortes} est inférieur à 1
     */
    public Bus(String immatriculation, int capacitePassagers, double vitesseMaxKmh,
            boolean hybride, int nombrePortes) {
        super(immatriculation, capacitePassagers, vitesseMaxKmh);
        if (nombrePortes < 1) {
            throw new IllegalArgumentException(
                    "Un bus a au moins 1 porte, reçue : " + nombrePortes);
        }
        this.hybride = hybride;
        this.nombrePortes = nombrePortes;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Réponse du bus : {@code "Hybride diesel-électrique"} ou
     * {@code "Diesel"} selon sa motorisation. Le {@code annoncer()} hérité
     * du parent composera l'annonce sans savoir laquelle des deux s'est
     * déclenchée.
     */
    @Override
    public String decrirePropulsion() {
        return hybride ? "Hybride diesel-électrique" : "Diesel";
    }

    /**
     * Copie le bus — <strong>sans retour covariant, volontairement</strong>.
     *
     * <p>Le type de retour reste {@code Vehicle}, exactement celui du parent.
     * Résultat côté appelant : {@code Bus r = bus.copier()} ne compile PAS,
     * il faut écrire {@code Bus r = (Bus) bus.copier()}. Comparer avec
     * {@link MetroTrain#copier()} : même travail, signature meilleure. Le
     * contraste indique la réparation : déclarer {@code Bus copier()}, le
     * retour covariant.
     *
     * @return un nouveau bus identique, typé seulement comme {@code Vehicle}
     */
    @Override
    public Vehicle copier() {
        return new Bus(immatriculation, capacitePassagers, vitesseMaxKmh, hybride, nombrePortes);
    }

    /**
     * Le bus peut-il doubler la file par une voie dédiée ?
     *
     * <p><strong>Leçon polymorphisme :</strong> le parent {@code Vehicle}
     * n'a pas cette méthode — le code générique
     * {@code serviceAnnonce(Vehicle v)} ne peut pas l'appeler sans savoir le
     * type concret. Ce n'est pas un défaut : les capacités <em>spécifiques</em>
     * restent dans la sous-classe ; le commun tient dans le parent. Vouloir
     * tout remonter dans {@code Vehicle} (« et si un tram aussi peut doubler ? »)
     * diluerait l'abstraction — c'est la pente qui mène à ISP, traitée par
     * les interfaces {@link Capteur} et {@link Rechargeable}.
     *
     * @return {@code true} — les bus du réseau empruntent les voies dédiées
     */
    public boolean peutDoubler() {
        return true;
    }

    /**
     * @return {@code true} si le bus est hybride
     */
    public boolean estHybride() {
        return hybride;
    }

    /**
     * @return le nombre de portes du bus
     */
    public int getNombrePortes() {
        return nombrePortes;
    }
}
