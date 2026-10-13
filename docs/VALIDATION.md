# Weryfikacja Projectly

Data: 13 października 2026.

Środowisko: macOS arm64, JDK 21, Maven 3.9.11 (dołączony Wrapper),
Spring Boot 3.5.16, Hibernate, H2 w trybie zgodności z PostgreSQL.

Polecenie: `./mvnw clean verify`.

Wynik: 37 testów, 0 błędów, 0 niepowodzeń, 0 pominiętych testów.

| Klasa | Liczba | Zakres |
|---|---:|---|
| UserServiceTest | 8 | BCrypt, normalizacja, unikalność, edycja hasła, bezpieczne usuwanie, ostatni ADMIN, uprawnienia |
| ProjectServiceTest | 8 | Utworzenie, członkostwo, duplikaty, właściciel, odpinanie zadań, usuwanie kaskadowe, dostęp |
| TaskServiceTest | 8 | Utworzenie, wykonawca, brak projektu, filtrowanie, status, cudze zadania, blokada konta |
| WebFlowTest | 13 | Logowanie, widoki, CSRF, walidacja, 400/403/404, izolacja projektów, pełny CRUD |

Testy usług używają rzeczywistych repozytoriów i transakcji z wycofaniem danych.
Testy MVC nie mają transakcji obejmującej cały test: szablony renderują się po
zakończeniu transakcji usługi, przy `spring.jpa.open-in-view=false`. Dzięki temu
wykrywają również brak pobrania relacji potrzebnych widokom. Każdy kontekst
testowy otrzymuje oddzielną bazę H2.

Testy korzystają ze standardowego mechanizmu Mockito typu subclass, który
nie wymaga dynamicznego dołączania agenta do JVM. Pozwala to uruchomić
weryfikację również w środowisku z ograniczeniami tego mechanizmu.

Pełny scenariusz MVC tworzy użytkownika i projekt, dodaje członka, tworzy
i edytuje zadanie, zmienia jego status jako wykonawca, usuwa członkostwo
i sprawdza odpięcie zadania, edytuje i usuwa projekt, weryfikuje kaskadę
usuwania zadania oraz usuwa niepowiązanego użytkownika.

Dodatkowo uruchomiono aplikację z pliku JAR w profilu demo i sprawdzono
logowanie administratora oraz widoki pulpitu i projektów w przeglądarce.
Zapisano podgląd pulpitu w `screenshots/dashboard.png`.

Konfigurację aplikacji przełączono na sterownik PostgreSQL. Dodano Docker Compose
z obrazem postgres:17-alpine, trwałym wolumenem i kontrolą gotowości. Polecenie
`docker compose config --quiet` zakończyło się poprawnie.

Po zmianie bazy i dopasowaniu importów do przeniesionych pakietów org.example
ponownie wykonano pełne budowanie: 37 testów zakończyło się poprawnie.

Po uruchomieniu Docker Desktop wykonano `docker compose up -d --wait`.
Kontener `projectly-postgres-1` działa jako healthy, z PostgreSQL 17.11
na porcie 127.0.0.1:5432 i wolumenem `projectly_postgres_data`.

Uruchomiono zbudowany plik JAR bez profilu demo i bez ręcznego eksportowania
zmiennych. Aplikacja wczytała lokalny plik .env, połączyła się z PostgreSQL
i utworzyła cztery tabele wraz z danymi początkowymi.

Przez rzeczywiste żądania HTTP sprawdzono logowanie administratora, dashboard,
tworzenie projektu, członkostwo, odrzucenie duplikatu członkostwa, tworzenie
zadania, zmianę jego statusu oraz kaskadowe usunięcie projektu i zadania.
Wynik sprawdzenia: poprawny. Tymczasowe dane sprawdzenia zostały usunięte.

Na koniec baza zawierała 3 użytkowników, 2 projekty, 3 członkostwa i 4 zadania
demonstracyjne. Kontener PostgreSQL pozostawiono uruchomiony.
