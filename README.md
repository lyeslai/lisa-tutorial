# IntervalsWithOverflowDomain

Ce domaine abstrait `IntervalsWithOverflowDomain` représente des intervalles d’entiers avec **gestion explicite du dépassement de capacité** (overflow 32 bits signé). Il améliore la précision de l’analyse numérique tout en assurant une convergence plus contrôlée dans les boucles.

## Structure

Cette classe étend `BaseNonRelationalValueDomain<IntervalsWithOverflowDomain>` et représente un intervalle `[low, high]` avec un wrap-around automatique sur les entiers 32 bits.

```java
private final int low;
private final int high;
```

---

## Fonctionnalités Implémentées

### 1. Wrap-around 32 bits

Toutes les opérations binaires arithmétiques prennent en charge le débordement 32 bits à l’aide de :

```java
private long wrapAround32Bit(long value) {
    return ((value - MIN) % RANGE_32BIT + RANGE_32BIT) % RANGE_32BIT + MIN;
}
```

Cela permet d'émuler fidèlement le comportement des entiers en Java.

---

### 2. `evalBinaryExpression`

Cette méthode centralise l’évaluation de toutes les opérations binaires :

- **Addition (`+`)** : `add(IntervalsWithOverflowDomain left, right)`
- **Soustraction (`-`)** : `sub(...)`
- **Multiplication (`*`)** : `mul(...)` avec gestion explicite des combinaisons
- **Division (`/`)** : `div(...)` avec détection de la division par zéro

Chaque opération applique la logique de wrap-around si un débordement est détecté.

---

### 3. `assumeBinaryExpression`

Permet d’affiner les intervalles selon des hypothèses conditionnelles :

| Opérateur     | Action sur l’intervalle (automatique)       |
|---------------|---------------------------------------------|
| `x < y`       | `high = min(high, y.low - 1)`               |
| `x <= y`      | `high = min(high, y.high)`                 |
| `x > y`       | `low  = max(low, y.high + 1)`              |
| `x >= y`      | `low  = max(low, y.low)`                   |
| `x == y`      | si inclus dans l’autre, alors égalisation  |
| `x != y`      | pas d’affinement                           |

Cette méthode utilise la **forme canonique de la comparaison** pour ajuster les bornes automatiquement, sans intervention manuelle.

---

### 4. Widening progressif

La méthode `wideningAux` applique un **élargissement contrôlé** à l’aide d’un tableau de seuils :

```java
private static final int[] THRESHOLDS = {
    Integer.MIN_VALUE, -10000, -1000, -100, -10, 0,
    10, 100, 1000, 10000, Integer.MAX_VALUE
};
```

À chaque itération, si un élargissement est requis, on monte seulement jusqu'au prochain seuil supérieur — évitant ainsi un passage brutal à `top`.

---

### 5. Robustesse

- **bottom()** : retourné si l’intervalle est incohérent (`low > high`)
- **top()** : `[MIN_VALUE, MAX_VALUE]` pour un intervalle inconnu
- **Division par zéro** : produit ⊥ (`bottom`)
- **Widening/Égalité** : s’assurent que l’intervalle reste cohérent sans sur-approximation immédiate

---

## Limites

- La précision peut être perdue en cas de **wrap-around multiple** (e.g., `Integer.MAX_VALUE + 100000`).
- Actuellement, seule une borne est raffinée à la fois (`x` ou `res`), mais pas les deux simultanément dans un contexte `loop`.

---

## Objectif

- Offrir une alternative réaliste aux intervalles classiques avec **wrap-around natif**
- Éviter les non-convergences par **widening à seuils**
- Être facilement combinable via `ValueEnvironment` et le produit cartésien

---

## Exemple

```imp
int x, res;
x = 0;
res = 0;
while (x < 10) {
    res = res + 100;
    x = x + 1;
}
```

Ce programme doit inférer :

- `x ∈ [0, 10]`
- `res ∈ [0, 1000]`

grâce à l’élargissement progressif et l’affinement dans `assumeBinaryExpression`.



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

