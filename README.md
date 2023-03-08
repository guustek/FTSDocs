### Wymagania:

<ol>
    <li>Java 17 (testowane na dystrybucji Oracle OpenJDK 17.0.3)</li>
    <li>Gradle 7.5</li>
</ol>

### Uruchamianie przez terminal

<ol>
    <li>Otwórz folder z projektem (FTSDocs) w terminalu</li>
    <li>
        Uruchom polecenie <b>./gradlew run</b> (Linux) lub <b>gradlew run</b> (Windows). 
        Przy pierwszym uruchomieniu zostanie pobrana odpowiednia wersja Gradle oraz potrzebne zależności, a następnie uruchomi się aplikacja.
        Jeśli polecenie na systemie Linux zwraca błąd "./gradlew: command not found" należy dodać pozwolenie na wykonywanie pliku poprzez komendę <b>chmod +x gradlew</b>.
    </li>
</ol>

### Uruchamianie w IDE (Intellij IDEA)

<ol>
    <li>Otwórz Intellij IDEA i wybierz "Open Project"</li>
    <li>
        W otwartym oknie wybierz folder z projektem (FTSDocs) lub plik build.gradle.kts w nim zawarty. 
        Projekt otworzy się w IDE, zostanie pobrana odpowiednia wersja Gradle oraz potrzebne zależności.
    </li>
    <li>Po zakończeniu ładowania projektu uruchom metodę main klasy <b>ftsdocs.Launcher</b> klikając na nią prawym przyciskiem myszy i wybierając opcję <b>Run 'Launcher.main()'</b></li>
</ol>

