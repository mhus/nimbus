
Es muessen nur DTOs umgestellt werden, die auch in Typescript verwendet werden.
Alle anderen bleiben in shared/dto.

Also die auf REST Pfaden `/*/user` oder `/*/player` oder `/*/editor` genutzt werden.

1. Umwandeln in typescript

```text
Die klasse soll als typescript interface in client_shared_src/dto nochmal erstellt werden.
```

2. Generierte Java DTOs aus typescript generieren

```shell
mvn clean install
```

3. Originale Java DTOs löschen.

4. Umstellen auf generierte Java DTOs

```text
die dependency die nicht compiliert werden koennen sind jetzt im projekt generated
unter src/main/java/de/mhus/nimbus/generated/dto.
```

5. Nochmal compilieren

```shell
mvn clean install
```
