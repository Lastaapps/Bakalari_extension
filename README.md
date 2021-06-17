# Bakaláři neoficiální Android klient

###### BAKALÁŘI software s.r.o. se nepodílela na vývoji tohoto projektu a nenese žádnou zodpovědnost za chyby způsobené programem.

### Co jsem?

Jsem rozpracovaný klient pro pouze studentský přístup k systému Bakaláři. Obsahuji všechny základní
funkce s novým vzhledem a umístěním informací. Přidávám chytrý offline režim, notifikaci s rozvrhem
či přehlednější propojení dat napříč aplikací.

### Jak na tom jsem?

Aplikace není dosud hotova, učil jsem se na ní moderní Android vývoj a doposud v ní je mnoho chyb a
chybějících funkcí. Vzhledově také dosud není úplně košér, ale i přesto z tohoto
ukázkového [videa](https://youtu.be/OYf0iPSldJA) můžete vidět myšlenku, na kterou aplikace míří -
rychlejší přístup k informacím, a to vše na jednom místě.

Pokud si budete chtít aplikaci vyzkoušet, v současné době si ji musíte sami zkompilovat, apk vydání
možná budou ve chvíli, kdy bude aplikace opravdu použitelná a na Google Play se aplikace dostane, až
bude hotová.

### Díky

Děkuji všem, co se podíleli na neoficiální dokumentaci API Bakalářů a odkud jsem dosti čerpal.
Bakaláři API: https://github.com/bakalari-api/bakalari-api-v3

### Přispívání do kódu

Pokud vás aplikace zaujme a budete chtít přidat funkci, opravit chybu či zlepšit vzhled, jste
vítáni. Primárně se ale prosím zaměřte na body z `TODO` seznamu. Příspěvky mohou být odmítnuty, pokud:

- kód nebude psán v Kotlinu
- nové sekce UI nebudou v Jetpack Compose
- kód nebude modularizovaný a dělen do vrstev
- kód bude padat a podobné jasné věci



### TODO

- modularizovat aplikaci
- aktualizace dat na pozadí
- hlášení změn rozvrhu, suplování, nové absence, ...
- přejít na Jetpack Compose
- push notifikace
- Komens modul
- GDPR modul