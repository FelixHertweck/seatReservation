#!/bin/bash
mvn -f pom.xml test -Dtest=AuthServiceTest#testAuthenticateFailureUserNotFound
mvn -f pom.xml test -Dtest=LoginRateLimitingTest#testNonExistentUserRecordsFailedAttempt
