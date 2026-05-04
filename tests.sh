#!/usr/bin/env bash

set -u

tests_dir="./tests"
scripts_dir="./scripts"
expected_dir="./expectedOutputs"

status=0

for i in 1 2 3; do
    test_path="$tests_dir/test${i}.txt"
    script_path="$scripts_dir/script${i}.txt"
    expected_path="$expected_dir/expectedOutput${i}.txt"
    actual_path="$(mktemp)"

    java lab3 "$test_path" "$script_path" > "$actual_path"

    if diff -w -B "$actual_path" "$expected_path" > /dev/null; then
        echo "PASS test${i}"
    else
        echo "FAIL test${i}"
        diff -w -B "$actual_path" "$expected_path"
        status=1
    fi

    rm -f "$actual_path"
done

exit "$status"
