# 🟩 Squares — консоль, REST-сервис и веб-интерфейс

Тестовое задание «Квадраты»: игра на поле N×N, два игрока (White/Black) по очереди ставят фишки своего цвета. Побеждает тот, кто первым составит квадрат из своих фишек (ориентация квадрата произвольная). Если поле заполнено и квадрата нет — ничья.

Проект состоит из трёх частей, использующих общий движок:
Rules, SquareDetector, Ai, GameState — ядро игры
CLI — консольная игра по потоку команд
REST-сервис (Задание 2) и веб-интерфейс (Задание 3)

## ⚡ Быстрый старт (одной командой поднимаем API и UI)

Выполнять из корня репозитория.
```bash windows
npm install
npm run dev:win
```
```bash macOS / Linux
npm install
npm run dev:unix
```
Доступы:
UI: http://127.0.0.1:5173
API: http://127.0.0.1:3000

## 🛠️ Сборка и тесты

Полная сборка:
```
./gradlew clean build     # Windows: gradlew.bat clean build
```

Тесты:
```
./gradlew test            # Windows: gradlew.bat test
```

Покрытие:
*SquareDetectorTest — корректность детектора квадратов
*RulesTest — применение ходов/валидация
*AiTest — базовые сценарии ИИ
*ApiServerIT, ApiMappingTest
