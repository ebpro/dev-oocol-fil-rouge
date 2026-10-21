package fr.ebruno.dev.oocol.bases.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import org.junit.jupiter.api.Test;

/**
 * Tests de {@link Zone} — la plus simple des classes du module : immuabilité
 * par {@code final}, validation, égalité par numéro.
 *
 * <p>Convention de nommage des tests : {@code situationAttendue} en français
 * non accentué (les accents dans les identifiants sont déconseillés : ils
 * dépendent de l'encodage de la machine).
 *
 * @author Emmanuel Bruno
 */
class ZoneTest {

    @Test
    void constructionValide() {
        Zone zone = new Zone(1, "Centre-ville");

        assertThat(zone.getNumero()).isEqualTo(1);
        assertThat(zone.getNom()).isEqualTo("Centre-ville");
    }

    @Test
    void numeroNegatifOuNulRejete() {
        // La validation est testée AUX LIMITES : 0 (nul) et -1 (négatif).
        assertThatThrownBy(() -> new Zone(0, "Centre-ville"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("strictement positif");
        assertThatThrownBy(() -> new Zone(-1, "Interdit"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void nomVideRejete() {
        assertThatThrownBy(() -> new Zone(1, "   "))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Zone(1, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void equalsEtHashCodeParNumero() {
        Zone zone1 = new Zone(1, "Centre-ville");
        Zone zone1Renommee = new Zone(1, "Coeur de ville"); // même numéro, nom différent
        Zone zone2 = new Zone(2, "Peripherie");

        // Deux zones du même numéro sont ÉGALES, quel que soit le nom.
        assertThat(zone1).isEqualTo(zone1Renommee).hasSameHashCodeAs(zone1Renommee);
        // Contrat equals/hashCode : égalité ⇒ mêmes codes de hachage (sinon les
        // objets « disparaissent » des HashMap/HashSet — cf. module 40).
        assertThat(zone1).isNotEqualTo(zone2);
    }

    @Test
    void zoneEstImmuableAucunSetter() {
        // Vérification par réflexion : aucune méthode PUBLIQUE dont le nom
        // commence par « set ». Le `final` des attributs rendrait d'ailleurs
        // tout setter compilable impossible sans réécrire la classe.
        for (Method m : Zone.class.getDeclaredMethods()) {
            boolean publique = Modifier.isPublic(m.getModifiers());
            assertThat(publique && m.getName().startsWith("set"))
                    .as("Zone ne doit pas exposer de setter public, trouvé : %s", m.getName())
                    .isFalse();
        }
    }
}
