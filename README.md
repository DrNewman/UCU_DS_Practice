Для запуску проєкт потрібно з кореневого каталогу проєкту:

1. Виконати збірку docker-image та запуск ноди у Docker:

docker compose up

2. Для завершення роботи з застосунком, - Ctrl+C у консолі, та команда очищення усіх зупинених контейнерів:

docker container prune


Після старту роботи застосунку взаємодія з нодами відбувається через GET- та POST-запити (наприклад, через Postman):

POST {{base_url}}/new_message - відправка нового повідомлення для зберігання. Працює тільки для leader-ноди. Body - row text.
GET {{base_url}}/all_saved_messages - отримання списку всіх збережених на ноді повідомлень
GET {{base_url}}/health - отримання статусу follower-нод з точки зору leader-ноди
POST {{base_url}}/command - команда на follower-ноду, для вказання режиму роботи. Задається параметром запиту command.
    Можна передавати команди:
    - fast - нода виконує запити без затримок, heart-beat відправляється щосекунди
    - slow - нода виконує запити з затримкою, heart-beat відправляється щосекунди
    - pause - нода не виконує запити, heart-beat не відправляється
    при відправці команди slow, додатково може бути вказаний параметр delay_time, що задасть час затримки обробки повідомлення.

У якості {{base_url}} доступні:
1. http://localhost:8081 - node-leader
2. http://localhost:8082 - node-follower-1
3. http://localhost:8083 - node-follower-2

Також отримати список усіх збережених на ноді повідомлень можна через браузер:
1. http://localhost:8081/all_saved_messages - node-leader
2. http://localhost:8082/all_saved_messages - node-follower-1
3. http://localhost:8083/all_saved_messages - node-follower-2


Або через curl-команди:

1. Відправка повідомлення на leader-ноду (стандартний write_concern=3)

curl -X POST -H "Content-Type: text/plain" http://localhost:8081/new_message -d "msg1"

1.1 Відправка повідомлення з конкретним write_concern (наприклад, 2)

curl -X POST -H "Content-Type: text/plain" "http://localhost:8081/new_message?write_concern=2" -d "msg2_wc2"

2. Перевірка повідомлень на leader-ноді

curl http://localhost:8081/all_saved_messages

3. Перевірка повідомлень на follower-ноді 1

curl http://localhost:8082/all_saved_messages

4. Перевірка повідомлень на follower-ноді 2

curl http://localhost:8083/all_saved_messages

5. Отримання статусу follower-нод (health check)

curl http://localhost:8081/health

6. Відправка команди на follower-ноду (наприклад, pause на ноду 1)

curl -X POST -H "Content-Type: text/plain" "http://localhost:8082/command?command=pause" -d ""

7. Відправка команди slow із затримкою (наприклад, 5 сек на ноду 2)

curl -X POST -H "Content-Type: text/plain" "http://localhost:8083/command?command=slow&delay_time=5" -d ""

8. Відправка команди fast (відновлення роботи ноди 1)

curl -X POST -H "Content-Type: text/plain" "http://localhost:8082/command?command=fast" -d ""