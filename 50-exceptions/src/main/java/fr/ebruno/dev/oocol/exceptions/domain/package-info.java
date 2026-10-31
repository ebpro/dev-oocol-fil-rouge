/**
 * Le domaine « Trans'U » relu par le prisme des exceptions (ELO 07).
 *
 * <p><strong>Ce que couvre ce package.</strong> Les entités des modules
 * précédents (Zone, Station, Line, FareCalculator) reviennent en instantané —
 * même domaine, nouveau vocabulaire d'erreur. Trois canaux coexistent,
 * et les choisir <em>est</em> la compétence centrale du module :
 * <ul>
 *   <li><strong>unchecked « bug »</strong> : {@link
 *     fr.ebruno.dev.oocol.exceptions.domain.StationNotFoundException} et
 *     {@link fr.ebruno.dev.oocol.exceptions.domain.FareCalculationException}
 *     — paramètre invalide, code appelant faux ; on propage, on ne rattrape pas ;</li>
 *   <li><strong>checked « récupérable »</strong> : {@link
 *     fr.ebruno.dev.oocol.exceptions.domain.NetworkException} et ses filles
 *     {@link fr.ebruno.dev.oocol.exceptions.domain.ScheduleConflictException}
 *     / {@link fr.ebruno.dev.oocol.exceptions.domain.InsufficientFundsException}
 *     — situation d'affaire prévue avec plan B ; le compilateur force sa gestion ;</li>
 *   <li><strong>valeurs de retour « réponse normale »</strong> : {@code boolean}
 *     de {@code retirerArret}, vue non modifiable, etc. — tout ce qui n'est
 *     pas une anomalie garde son contrat du module 40.</li>
 * </ul>
 *
 * <p><strong>Ordre de lecture conseillé.</strong> {@code NetworkException}
 * (la racine checked) → les deux filles checked ({@code ScheduleConflict},
 * {@code InsufficientFunds}) → les deux unchecked ({@code
 * StationNotFound}, {@code FareCalculation}) → {@code Station} et sa
 * fabrique de garde {@code requireValide} → {@code Line} (la migration
 * null → throws jouée méthode par méthode) → {@code Compte} (le checked
 * par excellence, avec son {@code getManquant()} qui fait le plan B) →
 * {@code FareCalculator} (les deux unchecked qui se croignent dans
 * {@code calculerPrixEntre}).
 *
 * @author Emmanuel Bruno
 */
package fr.ebruno.dev.oocol.exceptions.domain;
