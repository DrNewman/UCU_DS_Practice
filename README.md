Для запуску проекту потрібно з кореневого каталога проекту ("untitled"):

1. Виконати збірку docker-image:

docker build -t ucu-ds-pract .

2. Запустити ноди у Docker:

docker compose up

Для завершення роботи з застосунком, - Ctrl+C у консолі, та команда очищення усіх зупинених контейнерів:

docker container prune


Після старту роботи застосунку взаємодія з нодами відбувається через GET- та POST-запити (наприклад через Postman).

GET {{base_url}}/new_message - відправка нового повідомлення для зберігання. Працює тільки для leader-ноди. Body - row text.
POST {{base_url}}/all_saved_messages - отримання списку всіх збережених на ноді повідомлень

У якості {{base_url}} доступні:
1. http://localhost:8081 - node-leader
2. http://localhost:8082 - node-follower-1
3. http://localhost:8083 - node-follower-2

Також отримати список усіх збережених на ноді повідомлень можна через браузер:
1. http://localhost:8081/all_saved_messages - node-leader
2. http://localhost:8082/all_saved_messages - node-follower-1
3. http://localhost:8083/all_saved_messages - node-follower-2

Для провокації затримки у збереженні повідомлення (60 сек.) на follower-ноді потрібно що б у тескті повідомлення був фрагмент "wait" та фрагмент з id ноди ("node-follower-1" або "node-follower-2"), на якій потрібно спровокувати затримку.
Нприклад: "testMSGwaitOnnode-follower-1"

Write concern задається як параметр write_concern запиту /new_message

Для перевірки упорядкумання повідомлень у нодах можна послудовно відправити на leader-ноду повідомлення:
"First", "Second_wait_node-follower-1", "Third" де write concern для другого повідомлення не більше 2.
Після 3 повідомлення на нодах node-leader та node-follower-2 будуть всі 3 повідомлення, як відправлялись. На ноді node-follower-1 тільки перше та третє. Через 60 сек після відправки другого повідомлення, друге повідомлення також з'яаиться на ноді node-follower-2, на другому місці.
