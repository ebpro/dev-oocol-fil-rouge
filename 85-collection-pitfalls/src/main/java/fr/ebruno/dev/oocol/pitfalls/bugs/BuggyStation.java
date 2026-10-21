package fr.ebruno.dev.oocol.pitfalls.bugs;

import java.util.Objects;

/**
 * Un arrêt du réseau Trans'U — avec le piège le plus classique de Java :
 * <b>{@code equals} redéfini sans {@code hashCode}</b>.
 *
 * <p>Deux {@code BuggyStation} de même {@code code} sont « égales » au sens
 * d'{@code equals}, mais héritent du {@code hashCode} d'{@code Object}
 * (identifiant d'objet) : elles tombent dans des <em>seaux</em> différents
 * d'une {@code HashMap} ou d'un {@code HashSet}. Résultat : un
 * {@code get}/{@code contains} « perd » une entrée dont la clé est pourtant
 * égale. C'est la violation directe du contrat {@code equals}/{@code hashCode}.</p>
 *
 * @author Emmanuel Bruno
 */
public class BuggyStation {

    private final String code;
    private final String nom;

    public BuggyStation(String code, String nom) {
        this.code = Objects.requireNonNull(code, "code");
        this.nom = Objects.requireNonNull(nom, "nom");
    }

    public String getCode() {
        return code;
    }

    public String getNom() {
        return nom;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof BuggyStation s)) {
            return false;
        }
        return code.equals(s.code);
    }

    // BUG PEDAGOGIQUE: equals est redéfini mais PAS hashCode — deux stations
    // « égales » ont des hashCodes différents, donc les collections à base de
    // hachage (HashMap, HashSet) ne les retrouve jamais. Laisse le détection
    // des tests PitfallDetectionTest exploser. (Corrigé dans fixed/FixedStation.)
}
