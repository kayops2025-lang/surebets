@echo off
cd /d "%~dp0"
echo Lector de giros para slots en modo demo
echo.
echo 1. Abre el demo y deja visible el saldo.
echo 2. Escribe el nombre, la apuesta base y el extra jackpot (0 si no hay).
echo 3. Arrastra un recuadro SOLO sobre el numero del saldo.
echo 4. Ese recuadro negro se va. Queda una ventanita verde encima.
echo 5. Gira el demo. El contador vive en esa ventanita, no en esta consola.
echo 6. Marca BONUS INICIO y BONUS FIN a mano cuando salga el bonus.
echo 7. Pulsa Terminar cuando acabes.
echo.
set "JUEGO="
set "BASE="
set "EXTRA="
set /p JUEGO=Nombre del juego: 
set /p BASE=Apuesta base, por ejemplo 0.10 (Enter para detectarla sola): 
set /p EXTRA=Extra jackpot, por ejemplo 0.20 (Enter si no hay): 
echo.
python lector_demo.py --juego "%JUEGO%" --apuesta-base "%BASE%" --extra-jackpot "%EXTRA%"
if errorlevel 1 (
  echo.
  echo El lector se cerro con un error. Copia el mensaje de arriba y mandalo.
)
echo.
pause
