# CSV → AdminUserCreationDto Importer

This small Java program reads an `input.csv` and writes the data as JSON to `output.json`.  
Each line of the CSV is translated into an `AdminUserCreationDto` object.

---

## 1. Structure of `input.csv`

The file **must** be in the same directory where the program is executed.  
Columns are separated by `,` (comma).

| First Name | Last Name | Password |
|------------|-----------|----------|
| Max        | Mustermann | secret123 |
| Anna       | Müller     | password |

**Example `input.csv`:**
```

Max,Mustermann,secret123
Anna,Müller,password

````

---

## 2. Output

The output file is always named `output.json` and is also located in the working directory.  
Example output for the above CSV:

```json
[
  {
    "username": "max.mustermann",
    "email": "max.mustermann@example.com",
    "password": "secret123",
    "firstname": "Max",
    "lastname": "Mustermann"
  },
  {
    "username": "anna.müller",
    "email": "anna.müller@example.com",
    "password": "password",
    "firstname": "Anna",
    "lastname": "Müller"
  }
]
````

## 3. Execution

Prerequisites:

* Java 8 or newer
* No additional libraries required


```bash
cd importer
java CsvToAdminUserDto.java
```

After execution, you will find the `output.json` file in the same directory.

---

## 4. Notes

* The **Username** is always formed from `firstname.lastname` (all lowercase).
* The **Email** is automatically set as `username@example.com` as the CSV does not contain an email column.
* If a line in the CSV has fewer than 3 columns, it will be ignored.
