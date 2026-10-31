package fr.ebruno.dev.oocol.jdbc.repo;

import java.sql.Connection;
import java.sql.SQLException;

import fr.ebruno.dev.oocol.jdbc.dao.LineDao;
import fr.ebruno.dev.oocol.jdbc.dao.StationDao;
import fr.ebruno.dev.oocol.jdbc.dao.impl.LineDaoJdbc;
import fr.ebruno.dev.oocol.jdbc.dao.impl.StationDaoJdbc;

/**
 * Façade du réseau persistant — le point d’entrée unique des services des modules 10 à 70.
 *
 * <p><strong>Facade</strong> — masque la complexité JDBC. Les classes de service des modules 10 à 70 ne changent pas, on remplace seulement le stockage : elles
 * manipulaient des {@code List} en mémoire, elles manipulent désormais des
 * DAO derrière cette façade, et c'est tout.
 *
 * <p>La façade compose les trois préoccupations du module :
 * <ul>
 *   <li>les DAO ({@link StationDao}, {@link LineDao}) pour les opérations ;</li>
 *   <li>la {@link Connection} partagée — les deux DAO écrivent sur la même
 *       connexion, donc dans la <em>même</em> transaction ;</li>
 *   <li>le pilotage des transactions ({@link #transactionSurement}).</li>
 * </ul>
 *
 * <p>Note de vocabulaire : en JPA, « repository » désigne plus ou moins ceci.
 * Le mot est choisi ici pour que la transition terminologique vers JPA soit
 * douce — mais ce repository-là est fait de JDBC brut, pas de magie.
 */
public final class NetworkRepository {

    private final Connection connection;
    private final StationDao stationDao;
    private final LineDao lineDao;

    /**
     * Ouvre la façade sur une connexion JDBC déjà établie.
     *
     * @param conn connexion ouverte — sa durée de vie reste l'affaire de l'appelant
     */
    public NetworkRepository(Connection conn) {
        this.connection = conn;
        // Le câblage concret (JDBC) n'existe qu'ici : les appelants ne voient
        // que les interfaces retournées par stationDao()/lineDao().
        this.stationDao = new StationDaoJdbc(conn);
        this.lineDao = new LineDaoJdbc(conn);
    }

    /** @return le DAO des arrêts (sous son interface, jamais son implémentation) */
    public StationDao stationDao() {
        return stationDao;
    }

    /** @return le DAO des lignes (sous son interface) */
    public LineDao lineDao() {
        return lineDao;
    }

    /** @return la connexion sous-jacente — exposée pour rendre les transactions visibles */
    public Connection connexion() {
        return connection;
    }

    /**
     * Débranche (ou rebranche) le mode « tout-valider-automatiquement ».
     *
     * <p>Par défaut, {@code autoCommit = true} : chaque instruction SQL est
     * sa propre transaction. Appeler {@code debrancherTransaction(true)} met
     * la connexion en mode manuel : rien n'est validé tant que
     * {@code commit()} n'a pas été appelé.
     *
     * @param active {@code true} pour le mode transactionnel manuel
     * @throws SQLException si la connexion refuse le changement de mode
     */
    public void debrancherTransaction(boolean active) throws SQLException {
        connection.setAutoCommit(!active);
    }

    /**
     * Exécute {@code action} comme une seule transaction atomique.
     *
     * <p>Le gabarit ACID complet, en cinq gestes :
     * <ol>
     *   <li>{@code setAutoCommit(false)} — ouverture de la transaction ;</li>
     *   <li>{@code action.run()} — les écritures s'accumulent sans être validées ;</li>
     *   <li>si tout passe : {@code commit()} — les effets deviennent visibles ;</li>
     *   <li>si quoi que ce soit lève : {@code rollback()} — la base retrouve son
     *       état d'avant, comme si l'action n'avait jamais eu lieu (A de Atomicité) ;</li>
     *   <li>finally : {@code setAutoCommit(true)} — on rend la connexion dans son
     *       état d'origine, l'exception d'origine est propagée sans être avalée.</li>
     * </ol>
     *
     * @param action les écritures à valider ensemble ou annuler ensemble
     * @throws RuntimeException l'exception levée par l'action, propagée après rollback
     */
    public void transactionSurement(Runnable action) {
        try {
            connection.setAutoCommit(false);
            action.run();
            connection.commit();
        } catch (SQLException | RuntimeException e) {
            // Rollback d'abord, propagation ensuite — jamais l'inverse, jamais avaler.
            try {
                connection.rollback();
            } catch (SQLException rollbackEx) {
                e.addSuppressed(rollbackEx);
            }
            if (e instanceof RuntimeException re) {
                throw re;
            }
            throw new IllegalStateException("Transaction échouée puis annulée", e);
        } finally {
            try {
                connection.setAutoCommit(true);
            } catch (SQLException ignore) {
                // La connexion meurt peut-être déjà (fermée par l'appelant) : rien à signaler.
            }
        }
    }
}
