#!/usr/bin/env bash
# Pick which versions to release to Modrinth and/or GitHub.
# Needs: gh logged in, and modrinth.token in ~/.gradle/gradle.properties (or MODRINTH_TOKEN set).
set -euo pipefail
cd "$(dirname "$0")"

repo="SpeedyCoder1192/sound-muter"

# Gradle needs Java 25; use the one in ~/.jdks if nothing else is set up
if [ -z "${JAVA_HOME:-}" ] && ! command -v java >/dev/null; then
	JAVA_HOME=$(ls -d ~/.jdks/jdk-25* 2>/dev/null | tail -1)
	[ -n "$JAVA_HOME" ] || { echo "No Java found. Install JDK 25 or set JAVA_HOME."; exit 1; }
	export JAVA_HOME
fi

command -v gh >/dev/null || { echo "GitHub CLI (gh) isn't installed."; exit 1; }
GITHUB_TOKEN="$(gh auth token 2>/dev/null)" || { echo "gh isn't logged in, run: gh auth login"; exit 1; }
export GITHUB_TOKEN
if [ -z "${MODRINTH_TOKEN:-}" ] && ! grep -q '^modrinth.token=' ~/.gradle/gradle.properties 2>/dev/null; then
	echo "No Modrinth token. Add modrinth.token=... to ~/.gradle/gradle.properties"
	exit 1
fi

version=$(grep '^mod.version=' gradle.properties | cut -d= -f2)
mapfile -t nodes < <(ls versions | sort -V)
released=$(gh release list --repo "$repo" --limit 200 --json tagName --jq '.[].tagName')

echo
echo "Sound Muter $version"
for i in "${!nodes[@]}"; do
	if grep -qxF "$version+${nodes[$i]}" <<<"$released"; then
		status="(released)"
	else
		status=""
	fi
	printf "  %2d) %-18s %s\n" $((i + 1)) "${nodes[$i]}" "$status"
done
echo

read -rp "Which ones? Numbers separated by spaces: " -a picks
[ ${#picks[@]} -eq 0 ] && { echo "Nothing picked."; exit 0; }

chosen=()
for p in "${picks[@]}"; do
	if ! [[ "$p" =~ ^[0-9]+$ ]] || [ "$p" -lt 1 ] || [ "$p" -gt ${#nodes[@]} ]; then
		echo "Not a valid number: $p"
		exit 1
	fi
	chosen+=("${nodes[$((p - 1))]}")
done

for node in "${chosen[@]}"; do
	if grep -qxF "$version+$node" <<<"$released"; then
		echo "$node is already released. To replace it, delete the old one first:"
		echo "  Modrinth: project page -> Versions -> $version+$node -> Delete"
		echo "  GitHub:   gh release delete '$version+$node' --repo $repo --cleanup-tag --yes"
		exit 1
	fi
done

echo
echo "Upload to:"
echo "  1) Modrinth + GitHub"
echo "  2) Modrinth only"
echo "  3) GitHub only"
read -rp "> " where
case "$where" in
	1) task=publishMods; target="Modrinth + GitHub" ;;
	2) task=publishModrinth; target="Modrinth" ;;
	3) task=publishGithub; target="GitHub" ;;
	*) echo "Pick 1, 2 or 3."; exit 1 ;;
esac

echo
read -rp "Changelog (leave empty to use CHANGELOG.md): " changelog

echo
echo "About to release to $target:"
printf "  %s\n" "${chosen[@]}"
echo "Changelog: ${changelog:-<CHANGELOG.md>}"
read -rp "Go? [y/N] " ok
[[ "$ok" =~ ^[yY]$ ]] || { echo "Cancelled."; exit 0; }

args=()
[ -n "$changelog" ] && args+=("-Pchangelog=$changelog")

for node in "${chosen[@]}"; do
	echo
	echo "== $node"
	# a tag left behind by a deleted release would point the new release at old code
	if [ "$task" != publishModrinth ] && gh api "repos/$repo/git/refs/tags/$version+$node" >/dev/null 2>&1; then
		echo "removing leftover tag $version+$node"
		gh api -X DELETE "repos/$repo/git/refs/tags/$version+$node" >/dev/null
	fi
	if ! ./gradlew --console=plain -q ":$node:$task" "${args[@]}"; then
		echo "Failed on $node, stopping here. Anything listed above it went through."
		exit 1
	fi
	echo "done"
done
