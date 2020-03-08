import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Counter;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import static junit.framework.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;


public class RequestAnalyzerTest {

    private static final Map<Integer, String> _mapperFixture = new HashMap<Integer, String>() {
        {
            put(40028, "ip1 - - [24/Apr/2011:04:06:01 -0400] \"GET /~strabal/grease/photo9/927-3.jpg HTTP/1.1\" 200 40028 \"-\" \"Mozilla/5.0 (compatible; YandexImages/3.0; +http://yandex.com/bots)\"");
            put(56928, "ip1 - - [24/Apr/2011:04:10:19 -0400] \"GET /~strabal/grease/photo1/97-13.jpg HTTP/1.1\" 200 56928 \"-\" \"Mozilla/5.0 (compatible; YandexImages/3.0; +http://yandex.com/bots)\"");
            put(42011, "ip1 - - [24/Apr/2011:04:14:36 -0400] \"GET /~strabal/grease/photo9/927-5.jpg HTTP/1.1\" 200 42011 \"-\" \"Mozilla/5.0 (compatible; YandexImages/3.0; +http://yandex.com/bots)\"");
            put(6244, "ip1 - - [24/Apr/2011:04:18:54 -0400] \"GET /~strabal/grease/photo1/T97-4.jpg HTTP/1.1\" 200 6244 \"-\" \"Mozilla/5.0 (compatible; YandexImages/3.0; +http://yandex.com/bots)\"");
            put(14917, "ip2 - - [24/Apr/2011:04:20:11 -0400] \"GET /sun_ss5/ HTTP/1.1\" 200 14917 \"http://www.stumbleupon.com/refer.php?url=http%3A%2F%host1%2Fsun_ss5%2F\" \"Mozilla/5.0 (Windows; U; Windows NT 6.1; en-US; rv:1.9.2.16) Gecko/20110319 Firefox/3.6.16\"");
        }
    };

    private static final Map<Text, Iterable<AggregationResult>> _combinerReducerFixture = new HashMap<Text, Iterable<AggregationResult>>() {
        {
            put(new Text("1"), new ArrayList<AggregationResult>() {
                {
                    add(new AggregationResult(7000, 1));
                    add(new AggregationResult(3000, 1));
                    add(new AggregationResult(15000, 1));
                }
            });
            put(new Text("131"), new ArrayList<AggregationResult>() {
                {
                    add(new AggregationResult(1000, 1));
                    add(new AggregationResult(100, 1));
                    add(new AggregationResult(900, 1));
                    add(new AggregationResult(8000, 1));
                }
            });
            put(new Text("23"), new ArrayList<AggregationResult>() {
                {
                    add(new AggregationResult(400, 1));
                    add(new AggregationResult(1600, 1));
                }
            });
        }
    };

    @Test
    public void testMapper() throws IOException, InterruptedException {
        //Arrange
        Mapper<Object, Text, Text, AggregationResult>.Context contextMock = (Mapper<Object, Text, Text, AggregationResult>.Context) mock(Mapper.Context.class);
        Counter counterMock = mock(Counter.class);
        when(contextMock.getCounter(anyString(), anyString())).thenReturn(counterMock);

        QueryAnalyzer.QueryMapper mapper = new QueryAnalyzer.QueryMapper();

        for (Map.Entry<Integer, String> entry : _mapperFixture.entrySet()) {
            doAnswer(invocation -> {
                AggregationResult result = invocation.getArgumentAt(1, AggregationResult.class);

                //Assert
                assertEquals(java.util.Optional.of(entry.getKey()), java.util.Optional.of(result.getTotalBytesCount()));
                return result;
            }).when(contextMock).write(any(Text.class), any(AggregationResult.class));

            //Act
            mapper.map(new Object(), new Text(entry.getValue()), contextMock);
        }
    }

    @Test
    public void testCombiner() throws IOException, InterruptedException {
        //Arrange
        Reducer<Text, AggregationResult, Text, AggregationResult>.Context contextMock = (Reducer<Text, AggregationResult, Text, AggregationResult>.Context) mock(Reducer.Context.class);
        QueryAnalyzer.QueryCombiner combiner = new QueryAnalyzer.QueryCombiner();

        for (Map.Entry<Text, Iterable<AggregationResult>> entry : _combinerReducerFixture.entrySet()) {
            doAnswer(invocation -> {

                Text key = invocation.getArgumentAt(0, Text.class);
                AggregationResult value = invocation.getArgumentAt(1, AggregationResult.class);

                int calculatedTotalBytes = 0;
                int calculatedCount = 0;
                for (AggregationResult item : entry.getValue()) {
                    calculatedTotalBytes += item.getTotalBytesCount();
                    calculatedCount += item.getCount();
                }

                //Assert
                assertEquals(java.util.Optional.of(entry.getKey()), java.util.Optional.of(key));
                assertEquals(java.util.Optional.of(calculatedTotalBytes), java.util.Optional.of(value.getTotalBytesCount()));
                assertEquals(java.util.Optional.of(calculatedCount), java.util.Optional.of(value.getCount()));
                return value;
            }).when(contextMock).write(any(Text.class), any(AggregationResult.class));

            //Act
            combiner.reduce(new Text(entry.getKey()), entry.getValue(), contextMock);
        }
    }

    @Test
    public void testReducer() throws IOException, InterruptedException {
        //Arrange
        Reducer<Text, AggregationResult, Text, AggregationResult>.Context contextMock = (Reducer<Text, AggregationResult, Text, AggregationResult>.Context) mock(Reducer.Context.class);
        QueryAnalyzer.QueryReducer reducer = new QueryAnalyzer.QueryReducer();

        for (Map.Entry<Text, Iterable<AggregationResult>> entry : _combinerReducerFixture.entrySet()) {
            doAnswer(invocation -> {

                Text key = invocation.getArgumentAt(0, Text.class);
                AggregationResult value = invocation.getArgumentAt(1, AggregationResult.class);

                int calculatedTotalBytes = 0;
                int calculatedCount = 0;
                for (AggregationResult item : entry.getValue()) {
                    calculatedTotalBytes += item.getTotalBytesCount();
                    calculatedCount += item.getCount();
                }

                //Assert
                String expected = ("IP, " + entry.getKey());
                assertEquals(expected, key.toString());
                assertEquals(java.util.Optional.of(calculatedTotalBytes), java.util.Optional.of(value.getTotalBytesCount()));
                assertEquals(java.util.Optional.of(calculatedCount), java.util.Optional.of(value.getCount()));
                return value;
            }).when(contextMock).write(any(Text.class), any(AggregationResult.class));

            //Act
            reducer.reduce(new Text(entry.getKey()), entry.getValue(), contextMock);
        }
    }
}

