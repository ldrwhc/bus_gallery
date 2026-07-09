@echo off
setlocal

REM ============================================
REM BusGallery Desktop - Build Script
REM ============================================

REM Paths - adjust if needed
set QT_DIR=D:\env\QT\6.10.1\mingw_64
set MINGW_DIR=D:\env\QT\Tools\mingw1310_64
set CMAKE_DIR=D:\env\QT\Tools\CMake_64
set BUILD_DIR=build

REM Set PATH for MinGW and Qt DLLs
set PATH=%MINGW_DIR%\bin;%QT_DIR%\bin;%CMAKE_DIR%\bin;%PATH%

echo ============================================
echo Building BusGallery Desktop...
echo ============================================

REM Clean build
if exist %BUILD_DIR% rmdir /s /q %BUILD_DIR%
mkdir %BUILD_DIR%
cd %BUILD_DIR%

REM Configure
echo.
echo --- Configuring with CMake ---
cmake .. -G "MinGW Makefiles" ^
    -DCMAKE_PREFIX_PATH=%QT_DIR% ^
    -DCMAKE_MAKE_PROGRAM=%MINGW_DIR%\bin\mingw32-make.exe ^
    -DCMAKE_C_COMPILER=%MINGW_DIR%\bin\gcc.exe ^
    -DCMAKE_CXX_COMPILER=%MINGW_DIR%\bin\g++.exe

if %ERRORLEVEL% NEQ 0 (
    echo CMake configure failed!
    cd ..
    exit /b 1
)

REM Build
echo.
echo --- Building ---
mingw32-make -j4

if %ERRORLEVEL% NEQ 0 (
    echo Build failed!
    cd ..
    exit /b 1
)

echo.
echo ============================================
echo Build successful!
echo Executable: %CD%\BusGalleryDesktop.exe
echo ============================================

REM Copy Qt DLLs for distribution (windeployqt)
echo.
echo --- Deploying Qt DLLs ---
%QT_DIR%\bin\windeployqt.exe BusGalleryDesktop.exe --no-translations

echo.
echo ============================================
echo Deploy complete! Run BusGalleryDesktop.exe
echo ============================================

cd ..
endlocal
