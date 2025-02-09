# "Курс "Java-разработчик"
## «Поисковый движок» 

# Описание:
- Данное приложение позволяет индексировать страницы сайтов и осуществлять быстрый поиск нужной информации. 
- Работа приложения основана на обработке HTTP-запросов и ответов, а также использовании REST/JSON для обмена данными между клиентом и сервером.
- Приложение разработано на языке Java с использованием Spring Framework.

![image](./image/3.png )

 ### После индексации страницы сайтов можно осуществлять быстрый поиск нужной информации:

![image](./image/4.png)
# Функционал сервиса:
- Перед запуском приложения в конфигурационном файле задаются адреса сайтов, по которым будет осуществляться поиск.
- Движок автоматически проходит по всем страницам указанных сайтов и индексирует их, чтобы затем находить наиболее релевантные результаты по любым поисковым запросам.
- Реализованы функции остановки индексации и индексации отдельных страниц.
- Веб-интерфейс доступен по адресу: http://localhost:8080 
- Нажатие кнопки Start Indexing запускает процесс обхода всех страниц заданных сайтов и их индексацию для последующего поиска по запросу "query".
- Пользователи могут отправлять запросы через API движка.
- На основе этих запросов находятся страницы, содержащие указанные слова.
- Результаты поиска возвращаются пользователю.
- Пользователь присылает запрос через API движка.
- Далее ищутся страницы, на которых встречаются все эти слова.
- Результаты поиска по запросу “query” отдаются пользователю.

## Требования

### JDK 17
Проект использует синтаксис Java 17.

### Docker
Для запуска проекта необходимо установить и запустить Docker. Для работы с базой данных (Postgresql) нужно запустить соответствующий контейнер.
- Необходимо указать Ваши параметры подключения ( username: ******  password:****** ) в **application.yaml**
- Необходимо выполните следующие команды:
```bash
cd docker
```
```bash
cd docker
```
```bash
docker-compose up
```
## Используемые технологии:
- SQL
- Java
- Spring
- Thymeleaf
- Postgresql
- JPA (Hibernate)
- Jsoup: Java HTML parser
- Fork/Join Framework in Java
- Spring-boot-starter-security

### IntelliJ IDEA

- Для локального запуска проекта откройте файл Application.java и запустите его main-метод.


## Database:
- Postgresql

![image](./image/5.jpg )


____
  ✉ Почта для обратной связи:
  <a href="">krp77@mail.ru</a>