import eu.bitwalker.useragentutils.UserAgent;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class QueryAnalyzer {
    public static class QueryMapper extends Mapper<Object, Text, Text, AggregationResult> {


        private final static String IpPattern = "ip(\\d+)";
        private final static String PayloadSizePattern = "(\".*?\"\\s[^3]\\d\\d\\s)(\\d+)";

        public void map(Object key, Text value, Context context
        ) throws IOException, InterruptedException {

            UserAgent userAgent = UserAgent.parseUserAgentString(value.toString());
            String browser = userAgent.getBrowser().getGroup().getName();
            context.getCounter("Browsers", browser).increment(1);

            Pattern regex = Pattern.compile(IpPattern);
            Matcher match = regex.matcher(value.toString());

            match.find();
            String ipKey = match.group(1);

            regex = Pattern.compile(PayloadSizePattern);
            match = regex.matcher(value.toString());

            int bytesCount = 0;
            if (match.find()) {
                bytesCount = Integer.parseInt(match.group(2));
            }

            context.write(new Text(ipKey), new AggregationResult(bytesCount, 1));
        }
    }

    public static class QueryCombiner extends Reducer<Text, AggregationResult, Text, AggregationResult> {

        public void reduce(Text key, Iterable<AggregationResult> values,
                           Context context
        ) throws IOException, InterruptedException {
            int bytesCount = 0;
            int count = 0;

            for (AggregationResult val : values) {
                bytesCount += val.getTotalBytesCount();
                count += val.getCount();
            }

            AggregationResult aggregateValue = new AggregationResult(bytesCount, count);
            context.write(key, aggregateValue);
        }
    }

    public static class QueryReducer extends Reducer<Text, AggregationResult, Text, AggregationResult> {

        public void reduce(Text key, Iterable<AggregationResult> values,
                           Context context
        ) throws IOException, InterruptedException {
            int bytesCount = 0;
            int count = 0;

            for (AggregationResult val : values) {
                bytesCount += val.getTotalBytesCount();
                count += val.getCount();
            }

            Text outKey = new Text("IP, " + key);
            AggregationResult aggregateValue = new AggregationResult(bytesCount, count);
            context.write(outKey, aggregateValue);
        }
    }

    public static void main(String[] args) throws Exception {
        Job job = new Job(new Configuration());
        job.setJarByClass(QueryAnalyzer.class);
        job.setMapperClass(QueryAnalyzer.QueryMapper.class);
        job.setCombinerClass(QueryAnalyzer.QueryCombiner.class);
        job.setReducerClass(QueryAnalyzer.QueryReducer.class);

        job.setOutputKeyClass(Text.class);
        job.setOutputValueClass(AggregationResult.class);

        FileInputFormat.addInputPath(job, new Path(args[1]));
        FileOutputFormat.setOutputPath(job, new Path(args[2]));
        System.exit(job.waitForCompletion(true) ? 0 : 1);
    }
}