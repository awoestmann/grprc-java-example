package mathservice.server;

import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.stub.StreamObserver;

import java.io.IOException;

import mathservice.models.Mathservice.AddRequest;
import mathservice.models.Mathservice.AddResponse;
import mathservice.models.Mathservice.DivideRequest;
import mathservice.models.Mathservice.DivideResponse;
import mathservice.models.Mathservice.ErrorMessage;
import mathservice.models.MathServiceGrpc;

/**
 * Simple gRPC server executing simple math methods
 * @author Alexander Woestmann
 */
public class MathServer {

    private int port;

    private Server server;

    /**
     * Constructor.
     */
    public MathServer(int port) {
        this.port = port;
        this.server = ServerBuilder.forPort(port)
                .addService(MathServiceGrpc.bindService(new MathServiceImpl()))
                .build();
    }

    /** 
     * Start service.
     */
    public void start() throws IOException {
        server.start();

        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                System.err.println("Shutting down server");
                MathServer.this.stop();
            }
        });
    }

    /**
     * Stop service.
     */
    public void stop() {
        if (server != null) {
            server.shutdown();
        }
    }

    /**
     * Block application until service is stopped.
     * Neccessary as grpc runs as a daemon.
     */
    private void blockUntilShutdown() throws InterruptedException {
        if (server != null) {
            server.awaitTermination();
        }
    }

    /**
     * Service Implementation.
     */
    private static class MathServiceImpl implements MathServiceGrpc.MathService{

        /**
         * Execute addition and fill response.
         */
        @Override
        public void add(AddRequest req, StreamObserver<AddResponse> respObserver) {
            final int summand1 = req.getSummand1();
            final int summand2 = req.getSummand2();
            final int result = summand1 + summand2;
            final String equation = summand1 + " + " + summand2 + " = " + result;
            System.out.println("Received add request:\n" + req);
            respObserver.onNext(AddResponse.newBuilder()
                    .setResult(result)
                    .setEquation(equation)
                    .build());
            respObserver.onCompleted();
        }

        /**
         * Execute divison and fill response.
         * Set errorcode if divisor is 0.
         */
        @Override
        public void divide(DivideRequest req, StreamObserver<DivideResponse> respObserver) {
            final float dividend = req.getDividend();
            final float divisor = req.getDivisor();
            final float result;
            final String equation;
            final ErrorMessage errorMessage;
            final int errorCode;
            final String errorString;

            if (divisor == 0) {
                result = 0;
                equation = dividend + " / " + divisor + " = N/A";
                errorCode = 1;
                errorString = "Invalid argument: Division by zero";
            } else {
                result = dividend / divisor;
                equation = dividend + " / " + divisor + " = " + result;
                errorCode = 0;
                errorString = "";
            }

            errorMessage = ErrorMessage.newBuilder()
                    .setErrorCode(errorCode)
                    .setErrorMessage(errorString)
                    .build();

            System.out.println("Received divide request:\n" + req);
            respObserver.onNext(DivideResponse.newBuilder()
                    .setResult(result)
                    .setEquation(equation)
                    .setErrorMessage(errorMessage)
                    .build());
            respObserver.onCompleted();
        }
    }

    /**
     * Entry point.
     */
    public static void main(String[] args){
        int port = 44556;
        MathServer mServ = new MathServer(port);
        System.out.println("Server started, port: " + port);
        try {
            mServ.start();
            mServ.blockUntilShutdown();
        } catch (IOException ioe) {
            System.err.println( "Error on starting server: " + ioe.getMessage());
        } catch (InterruptedException ie) {
            System.err.println(ie.getMessage());
        }
    }
}
