@echo off
echo ===================================================
echo Menjalankan Zona POS Backend dengan Java 17
echo ===================================================
set JAVA_HOME=C:\Program Files (x86)\Java\jdk-17.0.10
set PATH=%JAVA_HOME%\bin;%PATH%
java -version
mvn spring-boot:run
pause
