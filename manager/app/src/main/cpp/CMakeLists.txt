# For more information about using CMake with Android Studio, read the
# documentation: https://d.android.com/studio/projects/add-native-code.html

# Sets the minimum version of CMake required to build the native library.
cmake_minimum_required(VERSION 3.18.1)

project("kernelsu")

find_package(cxx REQUIRED CONFIG)

include_directories(${CMAKE_CURRENT_SOURCE_DIR}/../../../../..)

find_library(log-lib log)

add_library(kernelsu
        SHARED
        jni.cc
        ksu.cc
        )

target_include_directories(kernelsu PRIVATE .)
target_link_libraries(kernelsu PRIVATE cxx::cxx ${log-lib})

add_library(adbroot SHARED adbroot.cc)
target_include_directories(adbroot PRIVATE .)
target_link_libraries(adbroot PRIVATE cxx::cxx ${log-lib})
