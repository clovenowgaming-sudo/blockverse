@echo off
setlocal
set VERSION=8.13
where gradle >nul 2>nul
if %ERRORLEVEL% EQU 0 (
  for /f "tokens=2" %%V in ('gradle --version ^| findstr /R "^Gradle"') do set FOUND_VERSION=%%V
  if "%FOUND_VERSION%"=="%VERSION%" (
    gradle %*
    exit /b %ERRORLEVEL%
  )
)
set BASE=%USERPROFILE%\.gradle\wrapper\dists\gradle-%VERSION%-bin\blockverse-bootstrap
set GRADLE_HOME=%BASE%\gradle-%VERSION%
if not exist "%GRADLE_HOME%\bin\gradle.bat" (
  if not exist "%BASE%" mkdir "%BASE%"
  echo Downloading official Gradle %VERSION% distribution...
  powershell -NoProfile -ExecutionPolicy Bypass -Command "Invoke-WebRequest -Uri 'https://services.gradle.org/distributions/gradle-%VERSION%-bin.zip' -OutFile '%BASE%\gradle-%VERSION%-bin.zip'; Expand-Archive -Force '%BASE%\gradle-%VERSION%-bin.zip' '%BASE%'"
  if errorlevel 1 exit /b %ERRORLEVEL%
)
call "%GRADLE_HOME%\bin\gradle.bat" %*
exit /b %ERRORLEVEL%
