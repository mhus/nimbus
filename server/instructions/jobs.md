# Jobs

## Success / Error Trigger

Erweitere WJob so, dass beim beenden von einem Job ein neuer Job gestartet werden kann.

Dazu Wir WJob erweitert um
- onSuccess : NextJob
- onError : NextJob
NextJob:
- executor
- type
- parameters

[?] Wenn ein job beendet wird, wird ein neuer Job gestartet.
Im neuen job wird die id, result und errorMessage des letzten jobs als
parameter autoatich hinterlegt. Als worldId wird die gleiche id verwendet wie der letzte job.
- Siehe auch JobProcessingScheduler

[?] Auch im job-editor sollen die neuen Parameter bearbeitbar sein
- ../client/packages/controls
 onSuccess und onError sind optional, d.h. es soll einen aufklappbaren Bereich geben in dem die Parameter gesetzt werden können.

## Job Editor

[x] Im job Editor soll es moegluch sein einen job zu clonen (copy). dabei geht der 'neuer job' editor auf mit den werten des alten jobs.

[?] In WAnything können unter der Collection jobPreset gespeicherte jobs vordefiniert werden. Diese sollen im job editor als auswahl zur verfuegung stehen.
- filterung nach collection: jobPreset - Controller sollte schon existieren
- Daten aus data werden 'neuer Job' Dialog uebernommen.
