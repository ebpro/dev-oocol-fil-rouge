/**
 * Module d'amorçage du fil rouge DEV-OOCOL (semaines 1-2 / séances CNAM S1 à S3).
 *
 * <p>Ce package ne contient <strong>aucune règle métier</strong> : le domaine
 * (les entités du réseau de transport public « Trans'U » : station, ligne, trajet…)
 * n'apparaît qu'à partir de la semaine 3, avec le module {@code 20-domain-bases}.
 *
 * <p>Objectifs pédagogiques de ce module :
 * <ul>
 *   <li>vérifier la chaîne d'outils : JDK 21+, Maven wrapper ({@code ./mvnw}), Git ;</li>
 *   <li>comprendre la structure standard d'un projet Maven multi-module
 *       (POM parent, {@code src/main/java}, {@code src/test/java}) ;</li>
 *   <li>écrire et exécuter ses premiers tests JUnit 6 + AssertJ ;</li>
 *   <li>voir la CI GitHub Actions valider chaque poussée ({@code git push}).</li>
 * </ul>
 *
 * @author Emmanuel Bruno
 */
package fr.ebruno.dev.oocol.scaffold;
