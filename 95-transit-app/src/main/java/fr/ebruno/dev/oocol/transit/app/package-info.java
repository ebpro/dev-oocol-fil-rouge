/**
 * Point d'entrée du module 95 — l'assemblage final du fil rouge.
 *
 * <p>{@link fr.ebruno.dev.oocol.transit.app.TransitApp} orchestre cinq
 * phases : construction par Builder, interrogation par la CLI, aller-retour
 * JDBC (H2 en mémoire), aller-retour CSV, rétrospective SOLID. Le main ne
 * ré-implémente rien : chaque phrase délègue à la couche qui la possède —
 * c'est déjà, dans sa structure, le SRP qu'il célèbre en phase 5.
 */
package fr.ebruno.dev.oocol.transit.app;
