#!/bin/bash

echo "$DOCKER_PASSWORD" | docker login -u "$DOCKER_USERNAME" --password-stdin
docker push airsonicadvanced/airsonic-advanced:latest
docker push airsonicadvanced/airsonic-advanced:edge/latest
docker push airsonicadvanced/airsonic-advanced:edge/$TRAVIS_TAG
docker push airsonicadvanced/airsonic-advanced:gitcommit/$TRAVIS_COMMIT
docker push airsonicadvanced/airsonic-advanced:travisbuild/$TRAVIS_BUILD_NUMBER

echo "Successfully pushed Docker image to repo airsonicadvanced/airsonic-advanced with tags: latest, edge/latest, edge/$TRAVIS_TAG, gitcommit/$TRAVIS_COMMIT, travisbuild/$TRAVIS_BUILD_NUMBER"