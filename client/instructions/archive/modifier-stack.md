
## Stack Modifier

## ModifierStack entwickeln

Als generelle Klasse:
- ggf. ModifierService, verwaltet mehrere ModifierStacks

ModifierStack:
- type boolean, number, string, ... any
- Es gibt eine Liste von Modifiers mit prio, es kann auch gleiche prio geben, dann der letzte (jüngste) hat Vorrang
- Modifiers sind Objekte
- Es gibt einen default wert der angewendet wird, wenn kein Modifier in der Liste ist
- Es wird ein Action Lambda beim erzeugen des Modifiers angegeben, der wird aufgerufen, wenn sich der Wert aendern muss
- defaultModifier (fallback, muss immer vorhanden sein)
- Function: update(force) - calculiert bei jeder den neuen wert, wird bei jeder aenderung aufgerufen. Bei aenderung
  (oder force) des wertes wird 'action' ausgefuehrt. Ist public.
- getDefaultModifier() : Modifier
- Hat intern eine Liste von Modifiers und sortiert diese nach prio und created. Der Erste gewinnt.

Modifier:
- type boolean, number, string, ...
- value, setValue()
- Prio : number
- created (timestamp for ordering)
- ModifierStack
- close

ModifierService:
- createModiferStack(stackName, defaultModifier, action)
- addModifier(stackName, modifier)
- removeModifier(modifier)
- removeStack(stackName)
- getModifierStack(stackName)

Beispiel:
```typescript
modifierService.createModifierStack<number>(
  "windForce",
  0, // Erstellt automatisch einen Modifier mit dem Wert und prio MAX_PRIORITY
  (newValue: number) => {
    environmentService.setWindForce(newValue);
  }); // results in windForce = 0

const windModifierByWeater = modifierService.addModifier<number>(
  "windForce",
  {
    value: 2,
    prio: 1000,
  }); // results in windForce = 2

const windModifierByEffect = modifierService.addModifier<number>(
  "windForce",
  {
    value: 10,
    prio: 100,
  }); // results in windForce = 10

...
windModifierByEffect.setValue(20); // results in windForce = 20
...
windModifierByWeater.setValue(5); // results in windForce = 20 (overridden by windModifierByEffect)
...
windModifierByEffect.setValue(40); // results in windForce = 40
...
windModifierByEffect.close() // results in windForce = 5 (next lower prio is windModifierByWeater, current value is 5)
```

[x] ModifierService implementieren und in AppContext registrieren.
