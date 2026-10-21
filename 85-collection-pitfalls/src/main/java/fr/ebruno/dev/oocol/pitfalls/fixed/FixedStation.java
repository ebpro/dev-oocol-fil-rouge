package fr.ebruno.dev.oocol.pitfalls.fixed;

import java.util.Objects;

/**
 * La version <b>corrigée</b> de {@link fr.ebruno.dev.oocol.pitfalls.bugs.BuggyStation} :
 * {@code equals} ET {@code hashCode} cohérents.
 *
 * <p>Règle d'or : redéfinir {@code equals} impose de redéfinir
 * {@code hashCode}, avec la même notion d'égalité — ici le {@code code} de
 * l'arrêt. Deux stations « égales » tombent alors dans le même seau des
 * collections à base de hachage, et {@code get}/{@code contains} les
 * retrouvent.</p>
 *
 * @author Emmanuel Bruno
 */
public class FixedStation {

    private final String code;
    private final String nom;

    public FixedStation(String code, String nom) {
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
        if (!(o instanceof FixedStation s)) {
            return false;
        }
        return code.equals(s.code);
    }

    @Override
    public int hashCode() {
        // FIX: cohérent avec equals — uniquement le code, même base que ci-dessus.
        return Objects.hash(code);
    }
}
