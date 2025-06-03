Brokerage Note Reader
====================
Converts brokerage notes from formats to formats.

# Features
- Can read brazilian's SINACOR notes, either protected or not.
- Can output to [StatusInvest](https://statusinvest.com.br) xlsx format

# How is this repo structured?
- core: Contains the core logic for reading and converting brokerage notes.
- cmdrun: Contains the main entry point for the cli application. When building the cli, it'll generate a fat jar.

# Usage
```bash
java -jar cmdrunner-2.0.jar -i <input pdf file> -o <output xlsx file>
```
After reading the note, it'll ask for broker name and stock tickers. On the second run, it'll only check if you want to change them.
