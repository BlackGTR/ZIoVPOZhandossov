1) POST /auth/register
body:
{"username":"admin","password":"твой_пароль","role":"ROLE_ADMIN"}

2) POST /auth/login
body:
{"username":"admin","password":"твой_пароль"}
В ответе accessToken и refreshToken.

3) POST /auth/refresh
body:
{"refreshToken":"строка_из_логина"}

4) POST /licenses (Bearer админ)
body:
{"productId":1,"typeId":1,"ownerId":2,"deviceCount":2,"description":"Лицензия для отчёта"}
В ответе id и code (код активации).

5) POST /licenses/activate — Bearer токен владельца лицензии (того же что ownerId)
body:
{"activationKey":"КОД_ИЗ_СОЗДАНИЯ","deviceMac":"AA-BB-CC-DD-EE-FF","deviceName":"Мой ПК","userId":2,"productId":1}
Ответ: ticket и signature.

6) POST /licenses/check (Bearer пользователь)
body:
{"deviceMac":"AA-BB-CC-DD-EE-FF","userId":2,"productId":1}

7) POST /licenses/renew (Bearer владелец)
body:
{"activationKey":"КОД","userId":2}
