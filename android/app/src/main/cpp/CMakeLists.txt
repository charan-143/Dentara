cmake_minimum_required(VERSION 3.22)

project(dentara_whisper C CXX)

set(CMAKE_C_STANDARD 11)
set(CMAKE_CXX_STANDARD 17)

set(WHISPER_LIB_DIR ${CMAKE_CURRENT_SOURCE_DIR}/whisper.cpp)

if (NOT EXISTS ${WHISPER_LIB_DIR}/src/whisper.cpp)
    message(FATAL_ERROR
        "whisper.cpp sources are missing. Run: git submodule update --init --recursive")
endif ()

# Pinned by the submodule; see .gitmodules. Kept as a literal so the build does not
# depend on parsing the upstream CMakeLists.
set(WHISPER_VERSION "1.9.4")

find_library(LOG_LIB log)

include(FetchContent)
FetchContent_Declare(ggml SOURCE_DIR ${WHISPER_LIB_DIR}/ggml)
FetchContent_MakeAvailable(ggml)

add_library(dentara_whisper SHARED
    ${WHISPER_LIB_DIR}/src/whisper.cpp
    ${CMAKE_CURRENT_SOURCE_DIR}/whisper_jni.cpp
)

target_compile_definitions(dentara_whisper PUBLIC GGML_USE_CPU)
target_compile_definitions(dentara_whisper PRIVATE WHISPER_VERSION="${WHISPER_VERSION}")

target_include_directories(dentara_whisper PRIVATE
    ${WHISPER_LIB_DIR}
    ${WHISPER_LIB_DIR}/src
    ${WHISPER_LIB_DIR}/include
    ${WHISPER_LIB_DIR}/ggml/include
    ${WHISPER_LIB_DIR}/ggml/src
    ${WHISPER_LIB_DIR}/ggml/src/ggml-cpu
)

# Deliberately built for the baseline ABI rather than armv8.2-a+fp16. Upstream ships
# per-feature variants and selects at runtime; a clinic fleet is mixed hardware and a
# SIGILL on an older handset is a worse outcome than a slower transcription. Revisit
# with runtime CPU detection if latency becomes the constraint.
if (NOT CMAKE_BUILD_TYPE STREQUAL "Debug")
    target_compile_options(dentara_whisper PRIVATE
        -O3
        -fvisibility=hidden
        -fvisibility-inlines-hidden
        -ffunction-sections
        -fdata-sections
    )
    target_link_options(dentara_whisper PRIVATE
        -Wl,--gc-sections
        -Wl,--exclude-libs,ALL
    )
endif ()

target_link_libraries(dentara_whisper ${LOG_LIB} android ggml)
