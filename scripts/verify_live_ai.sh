#!/usr/bin/env bash
# Explicit live acceptance; never run automatically or replace missing credentials with fixtures.
set -euo pipefail
: "${OPENAI_API_KEY:?Configure OPENAI_API_KEY locally; do not put it in source control}"
: "${TP3_JDK:?Set TP3_JDK}"
: "${TP3_H2_JAR:?Set TP3_H2_JAR}"
: "${TP3_TEST_CLASSES:?Set TP3_TEST_CLASSES to the full test runner's classes directory}"
"$TP3_JDK/bin/java" -cp "$TP3_TEST_CLASSES:$TP3_H2_JAR" database.AiLiveVerification
