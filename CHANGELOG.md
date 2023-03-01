# Amazon Neptune Export CHANGELOG

## Neptune Export v1.0.0 (Release Date: February 28, 2023):

Neptune Export is a tool to perform bulk data exports from AWS Neptune. Neptune Export is migrated from the AWS Labs [Amazon Neptune Tools](https://github.com/awslabs/amazon-neptune-tools) repository, and the old module is now deprecated. In this release, the release artifact `neptune-export.jar` has been renamed to `neptune-export-1.0.0-all.jar`. Going forward, Neptune Export will be following this new versioned naming scheme.

Instructions for running export jobs can be found in the docs/ directory.

A few changes are included since the migration. 

### Bug Fixes:

- Corrected r6g instance type prefix (used to be listed as r6d).

### New Features and Improvements:

- Added a new optional parameter to use customer managed KMS key for S3 server-side encryption.
- Added integration tests for developers requiring manual setup (see docs/dev/IntegrationTests.md).