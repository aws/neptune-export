/*
Copyright 2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
Licensed under the Apache License, Version 2.0 (the "License").
You may not use this file except in compliance with the License.
A copy of the License is located at
    http://www.apache.org/licenses/LICENSE-2.0
or in the "license" file accompanying this file. This file is distributed
on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
express or implied. See the License for the specific language governing
permissions and limitations under the License.
*/

package com.amazonaws.services.neptune.export;

import com.amazonaws.services.neptune.cluster.Cluster;
import com.amazonaws.services.neptune.io.Directories;
import com.amazonaws.services.neptune.propertygraph.ExportStats;
import com.amazonaws.services.neptune.propertygraph.schema.GraphSchema;

public interface NeptuneExportEventHandler {

    NeptuneExportEventHandler NULL_EVENT_HANDLER = new NeptuneExportEventHandler() {

        @Override
        public void onError() {
            // Do nothing
        }

        @Override
        public void onExportComplete(Directories directories, ExportStats stats, Cluster cluster) throws Exception {
            //Do nothing
        }

        @Override
        public void onExportComplete(Directories directories, ExportStats stats, Cluster cluster, GraphSchema graphSchema) throws Exception {
            //Do nothing
        }
    };

    void onError();

    void onExportComplete(Directories directories, ExportStats stats, Cluster cluster) throws Exception;

    void onExportComplete(Directories directories, ExportStats stats, Cluster cluster, GraphSchema graphSchema) throws Exception;
}
