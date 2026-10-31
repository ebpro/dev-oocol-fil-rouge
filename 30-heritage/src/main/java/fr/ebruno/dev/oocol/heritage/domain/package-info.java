/**
 * Module 30 — Héritage et polymorphisme du domaine « Trans'U ».
 *
 * <p>Ce paquet contient la hiérarchie de véhicules du réseau :
 * {@link fr.ebruno.dev.oocol.heritage.domain.Vehicle} (abstraite, le commun),
 * ses trois sous-classes concrètes
 * {@link fr.ebruno.dev.oocol.heritage.domain.MetroTrain},
 * {@link fr.ebruno.dev.oocol.heritage.domain.Bus},
 * {@link fr.ebruno.dev.oocol.heritage.domain.Tramway},
 * les interfaces de capacité
 * {@link fr.ebruno.dev.oocol.heritage.domain.Capteur} et
 * {@link fr.ebruno.dev.oocol.heritage.domain.Rechargeable} (ISP),
 * la fabrique {@link fr.ebruno.dev.oocol.heritage.domain.VehicleFactory}
 * (patron Factory) et la {@link fr.ebruno.dev.oocol.heritage.domain.Line}
 * version en composition (composition + délégation, démonstration LSP).
 *
 * <p>Ordre de lecture conseillé : Vehicle (l'abstraction, la méthode
 * abstraite) → MetroTrain (héritage + retour covariant) → Bus (contraste :
 * covariant non appliqué, capacité spécifique) → Tramway (héritage ET
 * implémentations d'interfaces) → Capteur / Rechargeable (capacités
 * séparées, ISP) → VehicleFactory (la création cachée derrière le type
 * large) → Line (pourquoi composer et non hériter : LSP).
 *
 * @author Emmanuel Bruno
 */
package fr.ebruno.dev.oocol.heritage.domain;
