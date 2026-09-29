@echo off
rem Копирует weather_station.py на малину по scp.
rem Первый раз спросит подтверждение хоста - ответить: yes
rem Затем - пароль пользователя asadullin на малине.
scp "D:\Projects\hse-arduino\work2_6_raspberry_app\weather_station.py" asadullin@10.83.178.100:/home/asadullin/
echo.
if errorlevel 1 (echo *** FAILED ***) else (echo OK - file is at /home/asadullin/weather_station.py)
pause
