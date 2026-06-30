cmake_minimum_required(VERSION 3.18)
project(grayjp2 C)

# Build OpenJPEG (openjp2) from source, fetched at configure time. Only the core codec
# library is needed — no command-line codec, tests, or docs.
include(FetchContent)

set(BUILD_CODEC OFF CACHE BOOL "" FORCE)
set(BUILD_DOC OFF CACHE BOOL "" FORCE)
set(BUILD_TESTING OFF CACHE BOOL "" FORCE)
set(BUILD_SHARED_LIBS OFF CACHE BOOL "" FORCE)
set(BUILD_PKGCONFIG_FILES OFF CACHE BOOL "" FORCE)

FetchContent_Declare(
        openjpeg
        GIT_REPOSITORY https://github.com/uclouvain/openjpeg.git
        GIT_TAG v2.5.0
)
FetchContent_MakeAvailable(openjpeg)

add_library(grayjp2 SHARED gray_jp2.c)

# openjpeg.h lives in the source tree; opj_config.h is generated into the build tree.
target_include_directories(grayjp2 PRIVATE
        ${openjpeg_SOURCE_DIR}/src/lib/openjp2
        ${openjpeg_BINARY_DIR}/src/lib/openjp2)

target_link_libraries(grayjp2 openjp2)
