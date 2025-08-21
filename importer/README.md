
# CSV → AdminUserCreationDto Importer

Dieses kleine Java-Programm liest eine `input.csv` ein und schreibt die Daten als JSON in `output.json`.  
Jede Zeile der CSV wird in ein `AdminUserCreationDto`-Objekt übersetzt.

---

## 1. Aufbau der `input.csv`

Die Datei **muss** im gleichen Verzeichnis liegen, in dem das Programm ausgeführt wird.  
Spalten sind durch `,` (Komma) getrennt.

| Vorname | Nachname | Passwort |
|---------|----------|----------|
| Max     | Mustermann | geheim123 |
| Anna    | Müller     | passwort |

**Beispiel `input.csv`:**
```

Max,Mustermann,geheim123
Anna,Müller,passwort

````

---

## 2. Output

Die Ausgabedatei heißt immer `output.json` und liegt ebenfalls im Arbeitsverzeichnis.  
Beispielausgabe für obiges CSV:

```json
[
  {
    "username": "max.mustermann",
    "email": "max.mustermann@example.com",
    "password": "geheim123",
    "firstname": "Max",
    "lastname": "Mustermann"
  },
  {
    "username": "anna.müller",
    "email": "anna.müller@example.com",
    "password": "passwort",
    "firstname": "Anna",
    "lastname": "Müller"
  }
]
````

## 3. Ausführen

Voraussetzungen:

* Java 8 oder neuer
* Keine zusätzlichen Libraries nötig


```bash
cd importer
java CsvToAdminUserDto.java
```

Danach findest du die Datei `output.json` im gleichen Verzeichnis.

---

## 4. Anmerkungen

* Der **Username** wird immer aus `vorname.nachname` gebildet (alles kleingeschrieben).
* Die **Email** wird automatisch als `username@example.com` gesetzt, da die CSV keine E-Mail-Spalte enthält.
* Falls eine Zeile in der CSV weniger als 3 Spalten hat, wird sie ignoriert.
