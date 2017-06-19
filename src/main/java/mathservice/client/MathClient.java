package mathservice.client;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;

import java.util.concurrent.TimeUnit;

import mathservice.models.Mathservice.AddRequest;
import mathservice.models.Mathservice.AddResponse;
import mathservice.models.Mathservice.DivideRequest;
import mathservice.models.Mathservice.DivideResponse;
import mathservice.models.Mathservice.ErrorMessage;
import mathservice.models.MathServiceGrpc;
/**
 * Simple gRPC client calling math methods on server
 * @author Alexander Woestmann
 */
public class MathClient{

    private String host;
    private int port;

    private final ManagedChannel channel;
    private final MathServiceGrpc.MathServiceBlockingClient blockingStub;

    /** 
     * Constructor
     */
    public MathClient(String host, int port) {
        this.host = host;
        this.port = port;

        this.channel = ManagedChannelBuilder.forAddress(host, port)
            .usePlaintext(true)
            .build();
        blockingStub = MathServiceGrpc.newBlockingStub(channel);
    }

    /** 
     * Shutdown client channel
     */
    public void shutdown() throws InterruptedException {
        channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }

    /**
     * Calls rpc methods to add to ints
     */
    public void add(int summand1, int summand2) {
        AddRequest request = AddRequest.newBuilder()
                .setSummand1(summand1)
                .setSummand2(summand2)
                .build();
        AddResponse response;
        try {
            response = blockingStub.add(request);
        } catch (StatusRuntimeException e) {
            System.err.println("RPC add request failed with status: " + e.getStatus());
            return;
        }
        System.out.println("RPC add result: " + response.getEquation());
    }

    /**
     * Call division rpc method and return result.
     * Throws exception on division by zero
     */
    public float divide(float dividend, float divisor) throws IllegalArgumentException, Exception {
        DivideRequest request = DivideRequest.newBuilder()
                .setDividend(dividend)
                .setDivisor(divisor)
                .build();
        DivideResponse response;
        try {
            response = blockingStub.divide(request);
        } catch (StatusRuntimeException e) {
            System.err.println("RPC divide request failed with status: " + e.getStatus());
            throw new Exception("RPC divide request failed with status: " + e.getStatus());
        }
        if (response.getErrorMessage().getErrorCode() == 0) {
            System.out.println("RPC divide result: " + response.getEquation());
            return response.getResult();
        } else {
            throw new IllegalArgumentException(response.getErrorMessage().getErrorMessage());
        }
    }

    /**
     * Entry point
     */
    public static void main(String[] args) throws InterruptedException{
        MathClient mClient = new MathClient("localhost", 44556);
        mClient.add(42, 42);
        float divResult1;
        float divResult2;

        try {
            divResult1 = mClient.divide(42.0f, 2.0f);
        } catch (Exception e) {
            System.err.println(e.getMessage());
        }

        try {
            divResult2 = mClient.divide(42.0f, 0.0f);
        } catch (IllegalArgumentException iae) {
            System.err.println("Invalid division arguments: " + iae.getMessage());
        } catch (Exception e) {
            System.err.println("Generic exception on divide: " + e.getMessage());
        }
        mClient.shutdown();
    }
}
