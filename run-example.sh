#!/usr/bin/env sh
set -eu

OUT="${TMPDIR:-/tmp}/course-delivery-java-classes"
rm -rf "$OUT"
mkdir -p "$OUT"
find src/main/java -name '*.java' -print > "$OUT/main-sources"
javac -d "$OUT" @"$OUT/main-sources"

case "${1:-run}" in
  run) java -cp "$OUT" example.edtech.CourseDeliveryApplication ;;
  test)
    find src/test/java -name '*.java' -print > "$OUT/test-sources"
    javac -cp "$OUT" -d "$OUT" @"$OUT/test-sources"
    java -cp "$OUT" example.edtech.CourseDeliveryServiceTest
    ;;
  *) echo "usage: ./run-example.sh [run|test]" >&2; exit 2 ;;
esac
