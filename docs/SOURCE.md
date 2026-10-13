# Zakres uproszczenia Taskmastera

Źródło: https://github.com/michalpotapiuk/Taskmaster.git
Rewizja odczytana: 202359fd90d03c598769d3d3de76631261ff0a59.

Przejrzano model ProjectEntity, TaskEntity, UserEntity, konfigurację SecurityConfig,
zależności Maven i strukturę warstw starego projektu.

| Taskmaster | Projectly |
|---|---|
| ProjectEntity z teams, users, diagram, notes i tasks | Project: owner, description, createdAt; osobne ProjectMember |
| TaskEntity z wieloma users, subtasks, issues, timeSpents i report | Task: jeden projekt, opcjonalnie jeden wykonawca, deadline |
| UserEntity z tabelą ról i wieloma kolekcjami | User: enum ADMIN/USER i enabled |
| @ManyToMany oraz liczne EAGER/cascade | Jawna encja członkostwa, LAZY i selektywne EntityGraph |
| Stateless HTTP Basic, CSRF wyłączone, szerokie permitAll | Sesyjne logowanie formularzem, CSRF i uprawnienia usług |
| REST, Gson, Swagger, Java 20, Spring Boot 3.1.1 | MVC, Thymeleaf, Validation, Java 21, Spring Boot 3.5.16 |

Wszystkie pliki implementacji Projectly zostały przygotowane dla nowego zakresu.
Stary projekt pobrano jako materiał źródłowy; nie jest wymagany do uruchomienia.
Nie przeniesiono konfiguracji dostępowej, starych tabel ani danych.

Ostateczny model i funkcje odpowiadają specyfikacji przekazanej w załączniku.

Zgodnie z późniejszą decyzją użytkownika baza została zmieniona z MySQL
na PostgreSQL uruchamiany przez Docker Compose. Aplikacja nadal działa
lokalnie jako Spring MVC z Thymeleaf. Zachowano pakiety org.example
przeniesione przez użytkownika.
