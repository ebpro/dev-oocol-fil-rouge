package fr.ebruno.dev.oocol.heritage.domain;

/**
 * Un tramway du réseau : véhicule électrique alimenté par caténaire, avec une
 * batterie de secours qui lui permet de franchir un tronçon non électrifié.
 *
 * <p><strong>La classe « tout-en-un » du module :</strong> elle montre qu'une
 * classe concrète peut à la fois <em>étendre</em> une classe
 * ({@code extends Vehicle} — l'héritage du commun) ET <em>implémenter</em> des
 * interfaces ({@code implements Capteur, Rechargeable} — des capacités
 * greffables). Un seul héritage de classes (Java l'impose : une seule flèche
 * pleine creuse), mais autant d'interfaces que nécessaire (flèches pointillées).
 *
 * <p><strong>Pourquoi {@link Capteur} et {@link Rechargeable} sont des
 * interfaces et pas des sous-classes de {@code Vehicle} ?</strong> Parce que
 * « avoir un capteur » n'est PAS une espèce de véhicule : un bus en aura un
 * demain, une borne de recharge aussi. L'expression « est-un » d'héritage
 * serait un mensonge de modèle ; « <em>est capable de</em> » se dit par une
 * interface — c'est l'Interface Segregation Principle (ISP) appliqué au domaine.
 *
 * @author Emmanuel Bruno
 * @see Vehicle
 * @see Capteur
 * @see Rechargeable
 */
public class Tramway extends Vehicle implements Capteur, Rechargeable {

    /** Longueur de la rame, en mètres. Strictement positive. */
    private double longueurM;

    /**
     * État de la batterie auxiliaire : proportion entre {@code 0.0} (vide) et
     * {@code 1.0} (pleine). Attribut privé, <em>modifié</em> par
     * {@link #recharger(double)} sous garde de plafond.
     */
    private double niveauBatterie;

    /**
     * Construit un tramway.
     *
     * @param immatriculation   immatriculation de la rame, non vide
     * @param capacitePassagers nombre de places, strictement positif (validé par le parent)
     * @param vitesseMaxKmh     vitesse maximale en km/h, strictement positive (validée par le parent)
     * @param longueurM         longueur de la rame en mètres, strictement positive
     * @param niveauBatterie    niveau initial de batterie, entre 0.0 et 1.0 inclus
     * @throws IllegalArgumentException si le parent rejette un de ses arguments, si
     *                                  {@code longueurM} est négative ou nulle, ou si
     *                                  le niveau de batterie sort de [0.0, 1.0]
     */
    public Tramway(String immatriculation, int capacitePassagers, double vitesseMaxKmh,
            double longueurM, double niveauBatterie) {
        super(immatriculation, capacitePassagers, vitesseMaxKmh);
        if (longueurM <= 0) {
            throw new IllegalArgumentException(
                    "La longueur d'un tramway doit être strictement positive (en mètres), reçue : " + longueurM);
        }
        if (niveauBatterie < 0.0 || niveauBatterie > 1.0) {
            throw new IllegalArgumentException(
                    "Le niveau de batterie doit être compris entre 0.0 et 1.0, reçu : " + niveauBatterie);
        }
        this.longueurM = longueurM;
        this.niveauBatterie = niveauBatterie;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Réponse du tramway : {@code "Électrique Caténaire + batterie (80%)"} —
     * le pourcentage est le niveau courant de la batterie arrondi à l'entier le
     * plus proche, donc cette description <em>change</em> après une recharge :
     * le polymorphisme lit l'état, pas le type.
     */
    @Override
    public String decrirePropulsion() {
        long pourcentage = Math.round(niveauBatterie * 100);
        return "Électrique Caténaire + batterie (" + pourcentage + "%)";
    }

    /**
     * Copie le tramway — retour covariant (comme {@link MetroTrain#copier()}).
     *
     * <p>La copie conserve le niveau de batterie : « mêmes valeurs, objet
     * distinct » est la définition d'une copie, pas celle d'un neuf.
     *
     * @return un nouveau tramway identique (mêmes valeurs, batterie comprise)
     */
    @Override
    public Tramway copier() {
        return new Tramway(immatriculation, capacitePassagers, vitesseMaxKmh,
                longueurM, niveauBatterie);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Version simplifiée assumée : le capteur de vitesse lit la vitesse
     * <em>maximale de conception</em> plutôt qu'une télémétrie temps réel —
     * l'essentiel est que la méthode du contrat {@link Capteur} soit
     * honorée.
     */
    @Override
    public double lireVitesse() {
        return vitesseMaxKmh; // champ protected du parent : visible ici, invisible dehors
    }

    /**
     * {@inheritDoc}
     *
     * <p>La recharge ajoute {@code unite} au niveau courant et <em>plafonne</em>
     * à 1.0 : personne n'a jamais vu une batterie pleine « déborder » de
     * quelques pourcents. Le niveau est aussi plancher à 0.0 — un mutateur
     * d'interface protège ses bornes, sinon l'incohérence devient atteignable.
     */
    @Override
    public void recharger(double unite) {
        niveauBatterie = Math.max(0.0, Math.min(1.0, niveauBatterie + unite));
    }

    /**
     * {@inheritDoc}
     *
     * @return le niveau de batterie, entre 0.0 et 1.0
     */
    @Override
    public double niveauActuel() {
        return niveauBatterie;
    }

    /**
     * @return la longueur de la rame en mètres
     */
    public double getLongueurM() {
        return longueurM;
    }
}
