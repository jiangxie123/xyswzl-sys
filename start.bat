@echo off
chcp 65001 > nul
echo ========================================
echo   校园失物招领系统 - 一键启动
echo ========================================
echo.
echo [1/2] 正在启动后端服务 (端口 8080)...
echo.

start "后端服务 - 校园失物招领系统" cmd /k "cd /d %~dp0 && mvn spring-boot:run"

echo.
echo 后端启动中，等待 5 秒后打开浏览器...
timeout /t 5 /nobreak > nul

echo.
echo [2/2] 正在打开浏览器访问系统...
echo.
start http://localhost:8080/

echo ========================================
echo   系统启动完成！
echo   浏览器地址：http://localhost:8080/
echo   测试账号：admin / 123456
echo.
echo   关闭此窗口不会停止服务
echo   如需停止，请在服务窗口按 Ctrl+C
echo ========================================
pause
