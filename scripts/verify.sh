#!/usr/bin/env bash
# ==============================================================================
# ShilpLoka (Java Edition) — Local Verification Script
# Runs build, unit tests, code style checks, and asset/term grep audits.
# ==============================================================================
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"

cd "${PROJECT_ROOT}"

echo "========================================================================"
echo " [ShilpLoka] Starting Comprehensive Local Verification Pipeline"
echo "========================================================================"

# Step 1: Execute Gradle Build & Unit Tests
echo "--> 1/3 Running Gradle build & tests..."
./gradlew build test --info

# Step 2: Zero Minecraft Terms & Assets Audit
echo "--> 2/3 Executing Zero-Minecraft Grep Audit..."
FORBIDDEN_TERMS=(
    "minecraft:"
    "creeper"
    "enderman"
    "ender_dragon"
    "nether"
    "the_end"
    "redstone"
    "herobrine"
)

FOUND_MATCHES=0
for term in "${FORBIDDEN_TERMS[@]}"; do
    if grep -rnwi "${PROJECT_ROOT}/src" "${PROJECT_ROOT}/assets" -e "${term}" 2>/dev/null; then
        echo "ERROR: Forbidden term '${term}' found in codebase or assets!"
        FOUND_MATCHES=1
    fi
done

if [ ${FOUND_MATCHES} -ne 0 ]; then
    echo "FAIL: Zero-Minecraft audit failed. Cleanse all forbidden terms."
    exit 1
fi
echo "✓ Zero-Minecraft audit passed (0 matches)."

# Step 3: Documentation & Specification Audit
echo "--> 3/3 Checking documentation suite integrity..."
REQUIRED_DOCS=(
    "docs/PRD.md"
    "docs/TRD.md"
    "docs/UI_UX_BRIEF.md"
    "docs/IMPLEMENTATION_PLAN.md"
    "docs/DATA_MODELS.md"
    "docs/CODING_CONVENTIONS.md"
    "docs/TESTING_STRATEGY.md"
    "docs/ASSET_LICENSES.md"
    "docs/HISTORY_SOURCES.md"
    "docs/OPEN_QUESTIONS.md"
)

for doc in "${REQUIRED_DOCS[@]}"; do
    if [ ! -f "${PROJECT_ROOT}/${doc}" ]; then
        echo "ERROR: Missing mandatory context document: ${doc}"
        exit 1
    fi
done
echo "✓ All core context documents present."

echo "========================================================================"
echo " [ShilpLoka] Verification Complete: All Checks Passed Successfully! ✓"
echo "========================================================================"
