import org.apache.hadoop.io.Writable;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

public class AggregationResult implements Writable {

    private int bytesCount;
    private int count;

    public AggregationResult() {
    }

    public AggregationResult(int bytesCount, int count) {
        this.bytesCount = bytesCount;
        this.count = count;
    }

    public int getTotalBytesCount() {
        return this.bytesCount;
    }

    public int getCount() {
        return this.count;
    }

    public void write(DataOutput dataOutput) throws IOException {
        dataOutput.writeInt(bytesCount);
        dataOutput.writeInt(count);
    }

    public void readFields(DataInput dataInput) throws IOException {
        this.bytesCount = dataInput.readInt();
        this.count = dataInput.readInt();
    }

    @Override
    public String toString() {
        return bytesCount + ", " + bytesCount / count;
    }
}
