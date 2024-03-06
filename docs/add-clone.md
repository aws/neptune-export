    NAME
            neptune-export.sh add-clone - Clone an Amazon Neptune database cluster.
    
    SYNOPSIS
            neptune-export.sh add-clone
                    [ --clone-cluster-correlation-id <cloneCorrelationId> ]
                    [ --clone-cluster-enable-audit-logs ]
                    [ --clone-cluster-id <targetClusterId> ]
                    [ --clone-cluster-instance-type <cloneClusterInstanceType> ]
                    [ --clone-cluster-replica-count <replicaCount> ]
                    --source-cluster-id <sourceClusterId>
    
    OPTIONS
            --clone-cluster-correlation-id <cloneCorrelationId>
                Correlation ID to be added to a correlation-id tag on the cloned
                cluster.
    
                This option may occur a maximum of 1 times
    
    
            --clone-cluster-enable-audit-logs
                Enables audit logging on the cloned cluster

                This option may occur a maximum of 1 times


            --clone-cluster-id <targetClusterId>
                Cluster ID of the cloned Amazon Neptune database cluster.
    
                This option may occur a maximum of 1 times
    
    
            --clone-cluster-instance-type <cloneClusterInstanceType>
                Instance type for cloned cluster (by default neptune-export will
                use the same instance type as the source cluster).
    
                This options value is restricted to the following set of values:
                    db.r4.large
                    db.r4.xlarge
                    db.r4.2xlarge
                    db.r4.4xlarge
                    db.r4.8xlarge
                    db.r5.large
                    db.r5.xlarge
                    db.r5.2xlarge
                    db.r5.4xlarge
                    db.r5.8xlarge
                    db.r5.12xlarge
                    db.r5.16xlarge
                    db.r5.24xlarge
                    db.r5d.large
                    db.r5d.xlarge
                    db.r5d.2xlarge
                    db.r5d.4xlarge
                    db.r5d.8xlarge
                    db.r5d.12xlarge
                    db.r5d.16xlarge
                    db.r5d.24xlarge
                    db.r6g.large
                    db.r6g.xlarge
                    db.r6g.2xlarge
                    db.r6g.4xlarge
                    db.r6g.8xlarge
                    db.r6g.12xlarge
                    db.r6g.16xlarge
                    db.x2g.large
                    db.x2g.xlarge
                    db.x2g.2xlarge
                    db.x2g.4xlarge
                    db.x2g.8xlarge
                    db.x2g.12xlarge
                    db.x2g.16xlarge
                    db.t3.medium
                    db.t4g.medium
                    r4.large
                    r4.xlarge
                    r4.2xlarge
                    r4.4xlarge
                    r4.8xlarge
                    r5.large
                    r5.xlarge
                    r5.2xlarge
                    r5.4xlarge
                    r5.8xlarge
                    r5.12xlarge
                    r5.16xlarge
                    r5.24xlarge
                    r5d.large
                    r5d.xlarge
                    r5d.2xlarge
                    r5d.4xlarge
                    r5d.8xlarge
                    r5d.12xlarge
                    r5d.16xlarge
                    r5d.24xlarge
                    r6g.large
                    r6g.xlarge
                    r6g.2xlarge
                    r6g.4xlarge
                    r6g.8xlarge
                    r6g.12xlarge
                    r6g.16xlarge
                    x2g.large
                    x2g.xlarge
                    x2g.2xlarge
                    x2g.4xlarge
                    x2g.8xlarge
                    x2g.12xlarge
                    x2g.16xlarge
                    t3.medium
                    t4g.medium
    
                This option may occur a maximum of 1 times
    
    
            --clone-cluster-replica-count <replicaCount>
                Number of read replicas to add to the cloned cluster (default, 0).
    
                This option may occur a maximum of 1 times
    
    
                This options value must fall in the following range: 0 <= value <= 15
    
    
            --source-cluster-id <sourceClusterId>
                Cluster ID of the source Amazon Neptune database cluster.
    
                This option may occur a maximum of 1 times
    
    
