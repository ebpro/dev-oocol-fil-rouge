# CI Étudiant — Activation et utilisation

## Qu'est-ce que c'est ?

Un pipeline GitHub Actions **gratuit, sans infrastructure**, qui compile, teste
et analyse votre code automatiquement à chaque push.

## Activation (2 minutes)

1. **Forkez** le dépôt `ebpro/dev-oocol-fil-rouge` (bouton Fork, en haut à droite)
2. Clonez votre fork : `git clone git@github.com:<votre-username>/dev-oocol-fil-rouge.git`
3. Allez dans **Settings → Secrets and variables → Actions → Variables**
4. Cliquez **New repository variable** :
   - Name : `ENABLE_STUDENT_CI`
   - Value : `true`
5. Poussez un commit — le workflow **Student CI** se lance automatiquement.

## Que vérifie-t-il ?

| Étape | Commande | Blocante ? |
|-------|----------|-----------|
| Compilation | `mvnw compile` | ✅ Oui |
| Tests | `mvnw test` | ✅ Oui |
| Qualité (PMD, SpotBugs, Checkstyle) | `mvnw verify` | ⚠️ Non (avertissements) |

## Résolution des problèmes

- **Build rouge** → lisez le log dans l'onglet Actions. L'étape qui a échoué est surlignée en rouge.
- **Timeout** → le cache Maven est activé ; réessayez (le premier build est lent, les suivants rapides).
- **Vous voulez tout vérifier en local** → `./mvnw clean verify`.

## Périmètre du Student CI

Le Student CI est volontairement simplifié : JDK 25, aucune dépendance externe.
