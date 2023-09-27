    NAME
            neptune-export.sh export-svc - Use Neptune Export Service to manage Export Job.
            Can optionally copy results to an Amazon S3 bucket.
    
    SYNOPSIS
            neptune-export.sh nesvc
                    --root-path
                    --json

    
    OPTIONS
            --json
                A json object describing the export job to be performed (see JSON SYNOPSIS and JSON OPTIONS)

                This option must occur exactly 1 time


            --root-path
                Root directory for output.
    
                This option must occur exactly 1 time

                This options value must be a path to a directory. The provided path
                must be readable and writable.
           

    JSON SYNOPSIS
            --json '{
                        "command" : <command>,
                        "params" : <params>,
                        [ "additionalParams" : <additionalParams>, ]
                        [ "completionFilePayload" : <completionFilePayload>, ]
                        [ "completionFileS3Path" : <completionFileS3Path>, ]
                        [ "configFileS3Path" : <configFileS3Path>, ]
                        [ "createExportSubdirectory" : <createExportSubdirectory>, ]
                        [ "jobSize" : <jobSize>, ]
                        [ "outputS3Path" : <outputS3Path>, ]
                        [ "overwriteExisting" : <overwriteExisting>, ]
                        [ "queriesFileS3Path" : <queriesFileS3Path>, ]
                        [ "s3Region" : <s3Region>, ]
                        [ "s3RoleArn" : <s3RoleArn>, ]
                        [ "s3RoleExternalId" : <s3RoleExternalId>, ]
                        [ "s3RoleSessionName":  <s3RoleSessionName>, ]
                        [ "sseKmsKeyId" : <sseKmsKeyId>, ]
                        [ "uploadToS3OnError" : <uploadToS3OnError>, ]
                    }'


    JSON OPTIONS
            "command" : <command>
                The Neptune Export command to be executed by the service. Must be one of
                "export-pg", "export-rdf", "export-pg-from-queries", "export-pg-from-comfig",
                "create-pg-config", "add-clone", "remove-clone"

                This option must occur exactly 1 time


            "params" : <params>
                A JSON object containing parameter mappings to be passed to the internal export job.
                All of the CLI options for the underlying export command can be included, although the
                parameter name should be translated to camelCase (--clone-cluster becomes "cloneCluster").

                Eg.
                    "params": {
                        "endpoint" : "(your Neptune endpoint DNS name)",
                        "cloneCluser" : true,
                        "format" : "csv"
                    }
                
                This option must occur exactly 1 time


            "additionalParams" : <additionalParams>
                Additional parameters to be used in conjunction with a Neptune ML export. See
                https://docs.aws.amazon.com/neptune/latest/userguide/machine-learning-data-export.html#machine-learning-additionalParams for more details.

                This option must occur exactly 1 time


            "completionFilePayload" : <completionFilePayload>
                Payload to be used when writing completion file.

                This option may occur a maximum of 1 times


            "completionFileS3Path" : <completionFileS3Path>
                Amazon S3 file path for a completion file.

                This option may occur a maximum of 1 times


            "configFileS3Path" : <configFileS3Path>
                Amazon S3 file path pointing to a config file.

                This option may occur a maximum of 1 times


            "createExportSubdirectory" : <createExportSubdirectory>
                If enabled, Neptune Export will create a subdirectory with a random identifier
                in the target Amazon S3 bucket. Defaults to true.
                
                This option may occur a maximum of 1 times


            "outputS3Path" : <outputS3Path>
                S3 URI for a target Amazon S3 bucket. Eg. s3://example-bucket/example-dir/

                This option may occur a maximum of 1 times


            "overwriteExisting" : <overwriteExisting>
                If set to true, any existing contents in the target Amazon S3 bucket will
                be overwritten. Defaults to false.

                This option may occur a maximum of 1 times


            "queriesFileS3Path" : <queriesFileS3Path>
                Amazon S3 file path pointing to a queries file.

                This option may occur a maximum of 1 times


            "s3Region" : <s3Region>
                Amazon S3 bucket region.

                This option may occur a maximum of 1 times


            "s3RoleArn" : <s3RoleArn>
                Role to be assumed when uploading results to an Amazon S3 bucket.
                If this options is unused, upload to S3 bucket will use credentials found by
                the DefaultAWSCredentialsProviderChain.

                This option may occur a maximum of 1 times


            "s3RoleExternalId" : <s3RoleExternalId>
                External Id to be used when assuming the role defined by s3RoleArn

                This option may occur a maximum of 1 times


            "s3RoleSessionName" : <s3RoleSessionName>
                Session name to be used when assuming the role defined by s3RoleArn

                This option may occur a maximum of 1 times


            "sseKmsKeyId" : <sseKmsKeyId>
                sseKmsKeyId to be used with the Amazon S3 bucket.

                This option may occur a maximum of 1 times


            "uploadToS3OnError" : <uploadToS3OnError>
                Set as True to upload partial results to Amazon S3 if the export job fails 

                This option may occur a maximum of 1 times
