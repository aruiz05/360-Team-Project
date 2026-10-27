#!/usr/bin/env bash

set -euo pipefail

usage() {
	printf '%s\n' \
		"Usage:" \
		"  $0 <JavaFX-lib-folder> <TP2-output-folder> [javadoc-command]" \
		"" \
		"Example:" \
		"  $0 /Users/aruiz/javafx-sdk-25.0.4/lib /Users/aruiz/Desktop/TP2"
}

if [[ $# -lt 2 || $# -gt 3 ]]; then
	usage
	exit 1
fi

javafx_lib=$1
output_root=$2

if [[ $# -eq 3 ]]; then
	javadoc_command=$3
elif command -v javadoc >/dev/null 2>&1; then
	javadoc_command=$(command -v javadoc)
elif [[ -x /usr/libexec/java_home ]]; then
	javadoc_command=$(/usr/libexec/java_home)/bin/javadoc
else
	printf '%s\n' "Javadoc was not found. Pass the full path to the JDK javadoc command."
	exit 1
fi

if [[ ! -x "$javadoc_command" ]]; then
	printf '%s\n' "Javadoc is not executable: $javadoc_command"
	exit 1
fi

if [[ ! -d "$javafx_lib" || ! -f "$javafx_lib/javafx-controls.jar" ]]; then
	printf '%s\n' "The JavaFX lib folder is invalid: $javafx_lib"
	printf '%s\n' "It must contain javafx-controls.jar."
	exit 1
fi

script_directory=$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)
project_root=$(cd "$script_directory/.." && pwd)
task3_output="$output_root/Lessons_Learned_Javadocs"
task4_output="$output_root/Contributor_Javadocs"

for output_directory in "$task3_output" "$task4_output"; do
	if [[ -e "$output_directory/index.html" ]]; then
		printf '%s\n' "Existing generated Javadoc found: $output_directory"
		printf '%s\n' "Move or rename that folder before running this script again."
		exit 1
	fi
	mkdir -p "$output_directory"
done

task3_sources=()
task4_sources=()

while IFS= read -r relative_path; do
	[[ -z "$relative_path" ]] && continue
	if [[ ! -f "$project_root/$relative_path" ]]; then
		printf '%s\n' "Required source file is missing: $relative_path"
		exit 1
	fi
	task3_sources+=("$project_root/$relative_path")
done < "$script_directory/task3_sources.txt"

while IFS= read -r relative_path; do
	[[ -z "$relative_path" ]] && continue
	if [[ ! -f "$project_root/$relative_path" ]]; then
		printf '%s\n' "Required source file is missing: $relative_path"
		exit 1
	fi
	task4_sources+=("$project_root/$relative_path")
done < "$script_directory/task4_sources.txt"

common_options=(
	-private
	-encoding UTF-8
	-docencoding UTF-8
	-charset UTF-8
	-sourcepath "$project_root/src"
	--module-path "$javafx_lib"
	--add-modules javafx.controls
)

printf '%s\n' "Generating Task 3 Javadoc..."
"$javadoc_command" "${common_options[@]}" \
	-d "$task3_output" "${task3_sources[@]}"
if [[ ! -f "$task3_output/index.html" ]]; then
	printf '%s\n' "Task 3 Javadoc did not produce index.html. Review the errors above."
	exit 1
fi

printf '%s\n' "Generating Task 4 Javadoc..."
"$javadoc_command" "${common_options[@]}" \
	-d "$task4_output" "${task4_sources[@]}"
if [[ ! -f "$task4_output/index.html" ]]; then
	printf '%s\n' "Task 4 Javadoc did not produce index.html. Review the errors above."
	exit 1
fi

mkdir -p "$task3_output/Screenshots" "$task4_output/Screenshots"

printf '%s\n' \
	"Javadoc generation completed." \
	"Task 3: $task3_output/index.html" \
	"Task 4: $task4_output/index.html" \
	"Add the rubric screenshots to each generated Screenshots folder."
