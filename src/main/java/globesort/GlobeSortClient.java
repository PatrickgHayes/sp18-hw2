package globesort;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import net.sourceforge.argparse4j.*;
import net.sourceforge.argparse4j.inf.*;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.lang.RuntimeException;
import java.lang.Exception;
import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.CountDownLatch;
import java.util.logging.Level;
import java.util.logging.Logger;

public class GlobeSortClient {

    private final ManagedChannel serverChannel;
    private final GlobeSortGrpc.GlobeSortBlockingStub serverStub;

	private static int MAX_MESSAGE_SIZE = 100 * 1024 * 1024;

    private String serverStr;

    public GlobeSortClient(String ip, int port) {
        this.serverChannel = ManagedChannelBuilder.forAddress(ip, port)
				.maxInboundMessageSize(MAX_MESSAGE_SIZE)
                .usePlaintext(true).build();
        this.serverStub = GlobeSortGrpc.newBlockingStub(serverChannel);

        this.serverStr = ip + ":" + port;
    }

    public void run(Integer[] values) throws Exception {
        System.out.println("Pinging " + serverStr + "...");
	long start_ping = System.currentTimeMillis();
        serverStub.ping(Empty.newBuilder().build());
	long end_ping = System.currentTimeMillis();
        System.out.println("Ping successful.");
	System.out.println("Round Trip Latency " + (end_ping - start_ping) + " milliseconds");
	System.out.println("");

        System.out.println("Requesting server to sort array");
	long start_sort = System.currentTimeMillis();
        IntArray request = IntArray.newBuilder().addAllValues(Arrays.asList(values)).build();
        IntArrayResponse response = serverStub.sortIntegers(request);
	long end_sort = System.currentTimeMillis();
	long sort_took = response.getTime();
        System.out.println("Sorted array");
	System.out.println("Application Throughput " + (values.length * 1000 /(end_sort - start_sort)) + " integers/second");
	System.out.println("One Way Throughput " + (values.length * 1000 / ((end_sort - start_sort) - sort_took))/2 + " integers/second") ;
	System.out.println("");
	System.out.println("The sort took " + sort_took + " milliseconds");
	System.out.println("");
    }

    public void shutdown() throws InterruptedException {
        serverChannel.shutdown().awaitTermination(2, TimeUnit.SECONDS);
    }

    private static Integer[] genValues(int numValues) {
        ArrayList<Integer> vals = new ArrayList<Integer>();
        Random randGen = new Random();
        for(int i : randGen.ints(numValues).toArray()){
            vals.add(i);
        }
        return vals.toArray(new Integer[vals.size()]);
    }

    private static Namespace parseArgs(String[] args) {
        ArgumentParser parser = ArgumentParsers.newFor("GlobeSortClient").build()
                .description("GlobeSort client");
        parser.addArgument("server_ip").type(String.class)
                .help("Server IP address");
        parser.addArgument("server_port").type(Integer.class)
                .help("Server port");
        parser.addArgument("num_values").type(Integer.class)
                .help("Number of values to sort");

        Namespace res = null;
        try {
            res = parser.parseArgs(args);
        } catch (ArgumentParserException e) {
            parser.handleError(e);
            System.exit(1);
        }
        return res;
    }

    public static void main(String[] args) throws Exception {
        Namespace cmd_args = parseArgs(args);
        if (cmd_args == null) {
            throw new RuntimeException("Argument parsing failed");
        }

        Integer[] values = genValues(cmd_args.getInt("num_values"));
	
        GlobeSortClient client = new GlobeSortClient(cmd_args.getString("server_ip"), cmd_args.getInt("server_port"));
	
        try {
            client.run(values);
        } finally {
            client.shutdown();
        }
    }
}
