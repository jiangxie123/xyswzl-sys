@echo off
chcp 65001 > nul
echo ========================================
echo   重新打包前端到后端
echo ========================================
echo.
echo 正在进入前端目录...
cd /d %~dp0frontend
echo.
echo 执行 npm run build 打包...
echo.
call npm run build
echo.
echo ========================================
echo   前端打包完成！
echo   前端文件已输出到 src\main\resources\static\
echo.
echo   下一步：
echo   1. 在 IDEA 中停止后端（红色■按钮）
echo   2. 重新启动后端（绿色▶按钮）
echo   3. 浏览器访问 http://localhost:8080/
echo ========================================
pause
