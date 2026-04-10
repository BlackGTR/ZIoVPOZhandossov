Antivirus server, учебный проект. Бэкенд на Spring Boot. Для локального запуска нужны PostgreSQL, переменные окружения с паролем к БД, файл keystore antivirus.p12 в src/main/resources. Сервер слушает HTTPS на порту 8443.

Все запросы ниже — POST, тело в JSON. Где нужен Bearer, в заголовок Authorization подставляется access token после логина.

1) POST /auth/register. Тело: {"username":"admin","password":"твой_пароль","role":"ROLE_ADMIN"}

2) POST /auth/login. Тело: {"username":"admin","password":"твой_пароль"}. В ответе accessToken и refreshToken, дальше для защищённых методов используется Bearer.

3) POST /auth/refresh. Тело: {"refreshToken":"строка_из_логина"}

4) POST /licenses — только админ, Bearer. Тело: {"productId":1,"typeId":1,"ownerId":2,"deviceCount":2,"description":"Лицензия для отчёта"}. В ответе id и code (код активации).

5) POST /licenses/activate — Bearer владельца лицензии (тот же пользователь, что в ownerId). Тело: {"activationKey":"КОД","deviceMac":"AA-BB-CC-DD-EE-FF","deviceName":"Мой ПК","userId":2,"productId":1}. В ответе ticket и signature.

6) POST /licenses/check — Bearer пользователя. Тело: {"deviceMac":"AA-BB-CC-DD-EE-FF","userId":2,"productId":1}

7) POST /licenses/renew — Bearer владельца. Тело: {"activationKey":"КОД","userId":2}

Задание 3. Модуль ЭЦП.

3.1. По методичке схема такая: подпись считается по байтам канонического JSON тикета, алгоритм SHA256withRSA, строка подписи в Base64. Ключи и сертификат лежат в keystore формата PKCS12.

3.2. Файл antivirus.p12 лежит в src/main/resources (тот же keystore, что используется для TLS). В application.properties параметры с префиксом signature: путь к keystore, тип PKCS12, пароль хранилища, alias srv23399-antivirus, алгоритм SHA256withRSA. Если отдельно не задан signature.key-password, для ключа берётся тот же пароль, что и для хранилища.

3.3. Классы в пакете com.example.antivirus.signature: SignatureProperties читает настройки из properties; SignatureKeyStoreService один раз загружает keystore в память и отдаёт приватный ключ и сертификат; JsonCanonicalizer сериализует объект тикета в канонический JSON (ключи в фиксированном порядке) и даёт байты в UTF-8; TicketSigningService вызывает канонизацию, подписывает байты через SHA256withRSA и возвращает строку Base64.

3.4. В LicenseService ответы методов activate, check и renew собираются в TicketResponse с полями ticket и signature. Поле signature — это ЭЦП RSA по каноническому JSON тикета, а не подпись на основе хеша с секретом JWT как в прежней версии.

3.5. В модели тикета есть Instant и LocalDate, для корректной сериализации в JSON подключена зависимость jackson-datatype-jsr310 (регистрация JavaTimeModule).

3.6. Проверка подписи вне сервера: взять публичный ключ из сертификата в том же antivirus.p12 или экспортировать сертификат через keytool и проверить подпись по тем же байтам канонического JSON.

3.7. GitHub: в Secrets для CI нужны SSL_KEYSTORE_P12_B64 (keystore в Base64) и SSL_KEYSTORE_PASSWORD, чтобы в пайплайне появился файл p12 и приложение собиралось. В Variables можно задать SIGNATURE_PUBLIC_KEY (публичный ключ в Base64, SubjectPublicKeyInfo) для отчёта или скрипта проверки; само приложение при обычном запуске от этой переменной не зависит.

3.8. В Postman после activate в теле ответа видны ticket и длинная строка signature в Base64; по ним можно убедиться, что подпись приходит.
