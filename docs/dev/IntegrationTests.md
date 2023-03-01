# Integration Tests

## Overview

The integration tests for Neptune Export run a set of full export jobs against a Neptune instance and validate that the output is correct. A user must provide their own Neptune instance with the correct sample data pre-loaded in order to run these tests.

## Setup

- Setup a clean Neptune instance to run integration tests against.
- Using the Neptune bulk loader, fill the database with Kelvin Lawrence's air-routes-small graph data [found here](https://github.com/krlawrence/graph/blob/master/sample-data/air-routes-small.graphml). GraphML files can be run through [graphml2csv](https://github.com/awslabs/amazon-neptune-tools/tree/master/graphml2csv) for property graph loading and csv files can go through [csv to rdf](https://github.com/aws/amazon-neptune-csv-to-rdf-converter) for RDF loading.
- Ensure your Neptune instance is accessible to your testing environment (either run tests from within the
same VPC as your instance or connect through a bastion host)

## Running Tests

- Set a `NEPTUNE_ENDPOINT` environment variable to your Neptune cluster endpoint
- Run Junit integration tests through your IDE