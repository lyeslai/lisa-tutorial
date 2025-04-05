# Intervals With Overflow
## Description
Ce domaine abstrait IntervalsWithOverflowDomain représente les intervalles d’entiers avec gestion explicite du dépassement de capacité (overflow 32 bits). Il permet une analyse plus réaliste dans des environnements où les variables entières peuvent dépasser la borne maximale ou minimale d’un int.

## Fonctionnalités clés
- **Wrap-around** sur 32 bits signé (**Integer.MIN_VALUE** à **Integer.MAX_VALUE**) pour toutes les opérations binaires (+, -, *, /).

- Évaluation précise des bornes dans les comparaisons (<, <=, >, >=, ==, !=) dans **assumeBinaryExpression**.

- **Widening** avec seuils progressifs (**thresholds**) pour éviter les boucles infinies tout en maintenant la précision.

- Gestion robuste des divisions par zéro (⊥).

- Compatible avec ValueEnvironment.

## Objectif
- Détecter des erreurs numériques potentielles (overflow).

- Offrir un compromis entre précision et convergence rapide pour les boucles et les affectations répétées.

## Limites
- En cas de wrap-around extrême (proche de ±2³¹), la précision peut se dégrader.


# Domaine d'Égalité (EqualsDomain)

Ce projet implémente un domaine d'égalité (**EqualsDomain**) pour l'analyse statique de programmes en utilisant le framework **LiSA** (*Library for Static Analysis*). Le domaine d'égalité est conçu pour suivre les relations d'égalité entre les variables et leurs valeurs concrètes dans un programme.

## Structure du Code

Le code est organisé autour de la classe `EqualsDomain`, qui étend `FunctionalLattice` de LiSA. Cette classe utilise une sous-classe `SetOfElements` pour représenter les ensembles d'éléments (variables) qui sont égaux et leurs valeurs concrètes associées.

## Classes Principales

- **`EqualsDomain`** : Représente le domaine d'égalité lui-même, gérant les mappings entre les identifiants (variables) et leurs ensembles d'éléments égaux.
- **`SetOfElements`** : Une sous-classe de `InverseSetLattice` qui encapsule un ensemble d'identifiants considérés comme égaux et une valeur concrète optionnelle.

## Fonctionnalités Implémentées

### 1. Gestion des Affectations (`assign`)

La méthode `assign` gère les affectations de la forme `identifier = valueExpression`. Elle traite trois cas principaux :

- **Affectation d'une variable à une autre** *(e.g., `z = x`)* : Met à jour les ensembles d'égalité pour inclure la nouvelle relation.
- **Affectation d'une constante** *(e.g., `x = 5`)* : Associe la valeur concrète à la variable et met à jour son ensemble d'égalité.
- **Affectation d'une expression binaire** *(e.g., `k = y + 1`)* : Tente de calculer la valeur concrète si possible, sinon perd la précision en ne gardant aucune valeur concrète.

### 2. Gestion des Hypothèses (`assume`)

La méthode `assume` traite les conditions de la forme `valueExpression`. Elle gère les comparaisons d'égalité (`==`) entre :

- **Deux variables** *(e.g., `x == y`)* : Fusionne leurs ensembles d'égalité et vérifie la cohérence des valeurs concrètes.
- **Une variable et une constante** *(e.g., `x == 5`)* : Met à jour la valeur concrète de la variable si elle est compatible.
- **Une variable et une expression binaire** *(e.g., `x == y + 1`)* : Vérifie si la condition peut être satisfaite en fonction de l'état actuel.

### 3. Fermeture (`close`)

La méthode `close` est utilisée pour fusionner les groupes d'égalité basés sur les valeurs concrètes. Elle assure que tous les identifiants qui partagent la même valeur concrète sont dans le même ensemble d'égalité.

### 4. Oubli d'Identifiant (`forgetIdentifier`)

La méthode `forgetIdentifier` permet de supprimer toutes les informations associées à un identifiant donné dans l'état actuel du domaine abstrait. Elle met à jour les autres identifiants pour supprimer toute relation d'égalité avec l'identifiant oublié.

## Choix de Conception

- La méthode `computeBinaryOperation` prend en charge les opérations arithmétiques (`+`, `-`, `*`, `/`, `%`) pour les nombres, avec une gestion basique des erreurs *(ex. division par zéro retourne `null`).*

## Limite

Actuellement, le domaine d'égalité ne prend pas en charge l'analyse des boucles. Toute tentative d'analyser une boucle ne mettra pas à jour correctement les relations d'égalité entre les variables, ce qui limite son applicabilité aux programmes contenant des structures répétitives.

