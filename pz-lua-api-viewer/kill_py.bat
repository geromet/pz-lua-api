@echo off
REM Kill all Python processes - call this when the server won't shut down cleanly
REM Usage: kill_py.bat
REM Or: call kill_py.bat && python serve.py (for auto-restart)

echo "Killing all Python processes..."
taskkill /F /IM python.exe 2>nul
echo "Python processes terminated."
exit /b 0
