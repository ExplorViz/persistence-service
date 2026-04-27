#!/bin/bash

cleanup() {
  local result=$?

  if [ "$old_stash" != "$new_stash" ] && [ "$new_stash" != "" ]
  then
    git stash pop --index --quiet
    if [ $? -ne 0 ]
    then
      echo "Failed to restore unstaged changes. Try to restore your changes manually using:"
      echo "    git stash pop --index"
    else
      echo "Restored unstaged changes successfully"
    fi
  else
    echo "No unstaged changes to restore"
  fi

  exit $result
}

echo "Running pre-commit hook"
echo
echo "WARNING: To ensure the pre-commit checks are only run on staged changes, \
your unstaged changes will be stashed."
echo "This script should automatically restore the unstaged changes when exiting."
echo "In the event that this fails, you should be able to manually restore the changes using:"
echo "    git stash pop --index"
echo

trap cleanup EXIT

old_stash=$(git rev-parse --quiet --verify refs/stash)
git stash --quiet --keep-index --include-untracked
new_stash=$(git rev-parse --quiet --verify refs/stash)

./gradlew spotlessCheck checkstyleMain pmdMain test

if [ $? -ne 0 ]
then
  echo "Pre-commit hook failed"
  exit 1
fi

echo "Pre-commit hook passed"
exit 0
