cmake_minimum_required(VERSION 3.16)
project(BusGalleryDesktop VERSION 1.0.0 LANGUAGES CXX)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
set(CMAKE_AUTOMOC ON)
set(CMAKE_AUTORCC ON)
set(CMAKE_AUTOUIC ON)

# Qt 6
find_package(Qt6 REQUIRED COMPONENTS Widgets Network Concurrent)

set(SOURCES
    main.cpp
    utils/Config.h
    utils/ChinaRegions.h
    utils/ThemeManager.h
    utils/ThemeManager.cpp
    utils/ImageProcessor.h
    utils/ImageProcessor.cpp
    models/CatalogItem.h
    models/UserProfile.h
    models/VehiclePayload.h
    api/ApiClient.h
    api/ApiClient.cpp
    api/AuthApi.h
    api/AuthApi.cpp
    api/UploadApi.h
    api/UploadApi.cpp
    api/CatalogApi.h
    api/CatalogApi.cpp
    ui/LoginDialog.h
    ui/LoginDialog.cpp
    ui/MainWindow.h
    ui/MainWindow.cpp
    ui/RegionPicker.h
    ui/RegionPicker.cpp
    ui/ImageDropZone.h
    ui/ImageDropZone.cpp
    ui/AutocompleteField.h
    ui/AutocompleteField.cpp
    api/PlateAssistantBridge.h
    api/PlateAssistantBridge.cpp
)

add_executable(${PROJECT_NAME} ${SOURCES})

target_include_directories(${PROJECT_NAME} PRIVATE ${CMAKE_CURRENT_SOURCE_DIR})

target_link_libraries(${PROJECT_NAME} PRIVATE
    Qt6::Widgets
    Qt6::Network
    Qt6::Concurrent
)

# Windows: hide console window
set_target_properties(${PROJECT_NAME} PROPERTIES
    WIN32_EXECUTABLE TRUE
)
