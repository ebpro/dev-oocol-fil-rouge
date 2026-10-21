package fr.ebruno.dev.oocol.jdbc.security;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import fr.ebruno.dev.oocol.jdbc.dao.StationDao;
import fr.ebruno.dev.oocol.jdbc.dao.impl.StationDaoJdbc;
import fr.ebruno.dev.oocol.jdbc.domain.Station;

/**
 * Démonstration pédagogique. Le code vulnérable est là pour être COMPARE, pas copié.
 *
 * <p><strong>Le mécanisme de l'injection.</strong> Une requête SQL est du
 * <em>code</em> que l'on envoie exécuter au serveur. Concaténer une entrée
 * utilisateur dans cette chaîne, c'est lui laisser la possibilité d'écrire
 * elle-même du code : avec le motif {@code ' OR '1'='1}, la condition
 * {@code WHERE code = ''} devient {@code WHERE code = '' OR '1'='1'} —
 * toujours vraie — et le {@code WHERE} cesse de filtrer. L'attaquant n'a pas
 * contourné la base, il a <em>rédigé</em> la requête à la place du programme.
 *
 * <p><strong>Pourquoi {@code ?} immunise.</strong> Une
 * {@link PreparedStatement} sépare le <em>plan</em> (le SQL, envoyé d'abord)
 * des <em>valeurs</em> (envoyées ensuite, typées). Le guillemet de l'attaque
 * n'est plus de la syntaxe : c'est un caractère parmi d'autres dans une
 * valeur comparée à {@code code}. {@code ' OR '1'='1} ne code pas, ça ne
 * matche rien — point.
 *
 * <p>Ce qui n'est PAS simulé ici, pour rester dans le cadre du cours : les
 * injections via {@code UNION} (exfiltration d'autres tables), les stacks
 * {@code ;} multi-requêtes (refusées par {@code executeQuery}), et
 * l'aveugle (blind). Le principe démontré — code vs valeur — est le même.
 */
public final class SqlInjectionDemo {

    /** L'attaque classique : elle transforme le WHERE en tautologie. */
    public static final String ATTAQUE = "' OR '1'='1";

    /** Pas d'instance : une classe de démonstration à méthodes statiques. */
    private SqlInjectionDemo() {
    }

    /**
     * Version VOLONTAIREMENT VULNÉRABLE — concaténation de la valeur dans le SQL.
     *
     * @param conn        connexion ouverte
     * @param codeStation la valeur fournie par « l'utilisateur » (ici : l'attaquant)
     * @return les lignes trouvées, formatées {@code code|nom} séparées par {@code " ; "} ; chaîne vide si 0 ligne
     * @throws SQLException si le SQL concaténé devient syntaxiquement invalide
     */
    public static String vulnérable(Connection conn, String codeStation) throws SQLException {
        // NE JAMAIS FAIRE CECI EN PRODUCTION : la valeur est collée DANS le code SQL.
        String sql = "SELECT * FROM stations WHERE code = '" + codeStation + "'";
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) { // NOSONAR java:S2077 — leçon didactique : la faille est le sujet, pas un défaut
            return formaterLignes(rs);
        }
    }

    /**
     * Version SÉCURISÉE — même question, posée avec un paramètre {@code ?}.
     *
     * @param conn        connexion ouverte
     * @param codeStation la valeur, si malveillante soit-elle, reste une valeur
     * @return les lignes trouvées, formatées comme dans {@link #vulnérable}
     * @throws SQLException si la base refuse la requête
     */
    public static String securise(Connection conn, String codeStation) throws SQLException {
        String sql = "SELECT * FROM stations WHERE code = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, codeStation);
            try (ResultSet rs = ps.executeQuery()) {
                return formaterLignes(rs);
            }
        }
    }

    /**
     * Joue la scène complète : donnée d'exemple, attaque, comparatif des deux requêtes.
     *
     * @param conn connexion ouverte sur un schéma déjà créé (voir
     *             {@link fr.ebruno.dev.oocol.jdbc.schema.DatabaseSchema#creer})
     * @throws SQLException si la base refuse l'une des opérations
     */
    public static void demonstrer(Connection conn) throws SQLException {
        StationDao dao = new StationDaoJdbc(conn);
        if (!dao.existe("GAR")) {
            dao.inserer(new Station("GAR", "Gare", 1, true));
        }
        System.out.println("Donnée d'exemple : Station(\"GAR\", \"Gare\", 1, true). "
                + "Attaque : " + ATTAQUE);

        String resultatVuln = vulnérable(conn, ATTAQUE);
        int lignesVuln = resultatVuln.isEmpty() ? 0 : resultatVuln.split(" ; ").length;
        System.out.println("Requête concaténée → RÉSULTAT: " + lignesVuln
                + " ligne(s) retournée(s) — l'injection a fonctionné ! "
                + "Le WHERE a été contourné, toute la table fuit : " + resultatVuln);

        String resultatSur = securise(conn, ATTAQUE);
        System.out.println("Requête paramétrée → "
                + (resultatSur.isEmpty()
                    ? "0 ligne retournée. ' OR '1'='1 n'est pas un code de station, "
                      + "c'est une chaîne littérale comparée à code. Le plan SQL n'a pas changé."
                    : "lignes : " + resultatSur));

        System.out.println("Même base, même attaque : la différence est uniquement "
                + "dans la façon d'écrire la requête.");
    }

    /** Mise en forme pédagogique des lignes d'un ResultSet : {@code code|nom}. */
    private static String formaterLignes(ResultSet rs) throws SQLException {
        StringBuilder sb = new StringBuilder();
        while (rs.next()) {
            if (!sb.isEmpty()) {
                sb.append(" ; ");
            }
            sb.append(rs.getString("code")).append('|').append(rs.getString("nom"));
        }
        return sb.toString();
    }
}
