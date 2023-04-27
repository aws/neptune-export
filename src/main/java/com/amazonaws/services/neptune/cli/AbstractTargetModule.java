package com.amazonaws.services.neptune.cli;

import com.amazonaws.services.neptune.io.CommandWriter;
import com.amazonaws.services.neptune.io.Directories;
import com.amazonaws.services.neptune.io.DirectoryStructure;
import com.amazonaws.services.neptune.io.LargeStreamRecordHandlingStrategy;
import com.amazonaws.services.neptune.io.Target;
import com.github.rvesse.airline.annotations.Option;
import com.github.rvesse.airline.annotations.restrictions.AllowedEnumValues;
import com.github.rvesse.airline.annotations.restrictions.Once;
import com.github.rvesse.airline.annotations.restrictions.PathKind;
import com.github.rvesse.airline.annotations.restrictions.Required;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

public abstract class AbstractTargetModule implements CommandWriter {

    @Option(name = {"-d", "--dir"}, description = "Root directory for output.")
    @Required
    @com.github.rvesse.airline.annotations.restrictions.Path(mustExist = false, kind = PathKind.DIRECTORY)
    @Once
    private File directory;

    @Option(name = {"-t", "--tag"}, description = "Directory prefix (optional).")
    @Once
    private String tag = "";

    @Option(name = {"-o", "--output"}, description = "Output target (optional, default 'file').")
    @Once
    @AllowedEnumValues(Target.class)
    private Target output = Target.files;

    @Option(name = {"--stream-name"}, description = "Name of an Amazon Kinesis Data Stream.")
    @Once
    private String streamName;

    @Option(name = {"--region", "--stream-region"}, description = "AWS Region in which your Amazon Kinesis Data Stream is located.")
    @Once
    private String region;

    @Option(name = {"--stream-large-record-strategy"}, description = "Strategy for dealing with records to be sent to Amazon Kinesis that are larger than 1 MB.")
    @Once
    @AllowedEnumValues(LargeStreamRecordHandlingStrategy.class)
    private LargeStreamRecordHandlingStrategy largeStreamRecordHandlingStrategy = LargeStreamRecordHandlingStrategy.splitAndShred;

    @Option(name = {"--disable-stream-aggregation"}, description = "Disable aggregation of Kinesis Data Stream records).")
    @Once
    private boolean disableAggregation = false;

    @Option(name = {"--export-id"}, description = "Export ID")
    @Once
    private String exportId = UUID.randomUUID().toString().replace("-", "");

    @Option(name = {"--partition-directories"}, description = "Partition directory path (e.g. 'year=2021/month=07/day=21').")
    @Once
    private String partitionDirectories = "";

    public AbstractTargetModule(){}
    public AbstractTargetModule(Target target) {
        this.output =  target;
    }

    public File getDirectory() {
        return directory;
    }

    public String getTag() {
        return tag;
    }

    public Target getOutput() {
        return output;
    }

    public String getStreamName() {
        return streamName;
    }

    public String getRegion() {
        return region;
    }

    public LargeStreamRecordHandlingStrategy getLargeStreamRecordHandlingStrategy() {
        return largeStreamRecordHandlingStrategy;
    }

    public boolean isEnableAggregation() {
        return !disableAggregation;
    }

    public Directories createDirectories() throws IOException {
        return Directories.createFor(directoryStructure(), directory, exportId, tag, partitionDirectories );
    }

    public Directories createDirectories(DirectoryStructure directoryStructure) throws IOException {
        return Directories.createFor(directoryStructure, directory, exportId, tag, partitionDirectories );
    }

    @Override
    public void writeReturnValue(String value){
        output.writeReturnValue(value);
    }

    @Override
    public void writeMessage(String value) {
        output.writeMessage(value);
    }

    protected abstract DirectoryStructure directoryStructure();
}
