package cherrybot.wot;

import java.io.IOException;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import java.lang.Math;

import ch.unisg.ics.interactions.wot.td.affordances.ActionAffordance;
import ch.unisg.ics.interactions.wot.td.affordances.Form;
import ch.unisg.ics.interactions.wot.td.affordances.PropertyAffordance;
import ch.unisg.ics.interactions.wot.td.io.TDGraphReader;
import ch.unisg.ics.interactions.wot.td.schemas.DataSchema;
import ch.unisg.ics.interactions.wot.td.schemas.ObjectSchema;
import ch.unisg.ics.interactions.wot.td.security.SecurityScheme;
import ch.unisg.ics.interactions.wot.td.security.APIKeySecurityScheme;
import ch.unisg.ics.interactions.wot.td.ThingDescription;
import ch.unisg.ics.interactions.wot.td.ThingDescription.TDFormat;
import ch.unisg.ics.interactions.wot.td.vocabularies.TD;
import ch.unisg.ics.interactions.wot.td.vocabularies.WoTSec;

import ch.unisg.ics.interactions.wot.td.clients.TDHttpRequest;
import ch.unisg.ics.interactions.wot.td.clients.TDHttpResponse;

public class Cherrybot {

    static ThingDescription td;
    static Optional<SecurityScheme> securityScheme;

    // Define TD URI
    static {
        try {
            td = TDGraphReader.readFromURL(TDFormat.RDF_TURTLE, "http://yggdrasil.interactions.ics.unisg.ch/environments/61/workspaces/102/artifacts/cherrybot");
            securityScheme = td.getFirstSecuritySchemeByType(WoTSec.APIKeySecurityScheme);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Just to make it easier to change the time of the sleep()
    static void waitTime() throws InterruptedException {
        Thread.sleep(1000);
    }

    // auxiliar function to get the operator's token
    static String getOperatorToken() throws IOException, InterruptedException {

        String token = "";
        String operatorAffordanceType = "https://interactions.ics.unisg.ch/cherrybot#Operator";

        Optional<PropertyAffordance> operatorProperty = td.getFirstPropertyBySemanticType(operatorAffordanceType);

        if (operatorProperty.isPresent()) {

            Optional<Form> operatorForm = operatorProperty.get().getFirstFormForOperationType(TD.readProperty);

            if (operatorForm.isPresent()) {

                TDHttpRequest request = new TDHttpRequest(operatorForm.get(), TD.readProperty);
                waitTime();
                TDHttpResponse response = request.execute();

                Map<String, Object> payload = response.getPayloadAsObject((ObjectSchema) operatorProperty.get().getDataSchema());
                token = payload.get("https://interactions.ics.unisg.ch/cherrybot#OperatorToken").toString();

                System.out.println("Current operator: " + response.getPayload().toString());
                System.out.println("Token: " + token);

            }

        }

        return token;

    }

    // initialize cherry bot
    static String initialize() throws IOException, InterruptedException {

        boolean isAvailable = false;
        String token = "not initialized";

        // check if cherrybot is available
        String operatorAffordanceType = "https://interactions.ics.unisg.ch/cherrybot#Operator";

        Optional<PropertyAffordance> operatorProperty = td.getFirstPropertyBySemanticType(operatorAffordanceType);

        if (operatorProperty.isPresent()) {

            Optional<Form> operatorForm = operatorProperty.get().getFirstFormForOperationType(TD.readProperty);

            if (operatorForm.isPresent()) {

                TDHttpRequest request = new TDHttpRequest(operatorForm.get(), TD.readProperty);
                waitTime();
                TDHttpResponse response = request.execute();
                isAvailable = response.getStatusCode() == 204;

            }

        }

        System.out.println("\nis available: " + isAvailable);

        if (isAvailable) {

            //if cherrybot is available, post new operator
            String registerOperatorAffordanceType = "https://interactions.ics.unisg.ch/cherrybot#RegisterOperator";

            Optional<ActionAffordance> registerOperatorAction = td.getFirstActionBySemanticType(registerOperatorAffordanceType);

            if (registerOperatorAction.isPresent()) {

                Optional<Form> registerOperatorForm = registerOperatorAction.get().getFirstFormForOperationType(TD.invokeAction);

                if (registerOperatorForm.isPresent()) {

                    Map<String, Object> payload = new HashMap<>();
                    payload.put("http://xmlns.com/foaf/0.1/Name", "Bruno Souza de Lima");
                    payload.put("http://xmlns.com/foaf/0.1/Mbox", "bruno.szdl@gmail.com");

                    Optional<DataSchema> inputSchema = registerOperatorAction.get().getInputSchema();

                    if (inputSchema.isPresent()) {

                        TDHttpRequest request = new TDHttpRequest(registerOperatorForm.get(), TD.invokeAction);
                        request.setObjectPayload((ObjectSchema) inputSchema.get(), payload);
                        waitTime();
                        TDHttpResponse response = request.execute();
                        System.out.println("Received POST operator response with status code: " + response.getStatusCode());

                        if (response.getStatusCode() == 200) {

                            // get operator's token
                            token = getOperatorToken();

                        }

                    }

                }

            }

            // initialize cherrybot
            String initializeAffordanceType = "https://interactions.ics.unisg.ch/cherrybot#Initialize";

            Optional<ActionAffordance> initializeAction = td.getFirstActionBySemanticType(initializeAffordanceType);

            if (initializeAction.isPresent()) {

                Optional<Form> registerOperatorForm = initializeAction.get().getFirstFormForOperationType(TD.invokeAction);

                if (registerOperatorForm.isPresent()) {

                    TDHttpRequest request = new TDHttpRequest(registerOperatorForm.get(), TD.invokeAction);

                    if (securityScheme.isPresent()) {

                        request.setAPIKey((APIKeySecurityScheme) securityScheme.get(), token);
                        waitTime();
                        TDHttpResponse response = request.execute();
                        System.out.println("Received PUT initialize response with status code: " + response.getStatusCode());

                    }

                }

            }

        } else {

            //if cherrybot is not available, just define an invalid token
            token = "not initialized";

        }

        return token;

    }

    // set target for cherrybot's arm
    static void setTarget(String token, double x, double y, double z, double roll, double pitch, double yaw, int speed)
            throws IOException, InterruptedException {

        String setTargetAffordanceType = "https://interactions.ics.unisg.ch/cherrybot#SetTarget";

        Optional<ActionAffordance> setTargetAction = td.getFirstActionBySemanticType(setTargetAffordanceType);

        if (setTargetAction.isPresent()) {

            Optional<Form> setTargetForm = setTargetAction.get().getFirstFormForOperationType(TD.invokeAction);

            if (setTargetForm.isPresent()) {

                Map<String, Object> coordinate = new HashMap<>();
                coordinate.put("https://interactions.ics.unisg.ch/cherrybot#XCoordinate", x);
                coordinate.put("https://interactions.ics.unisg.ch/cherrybot#YCoordinate", y);
                coordinate.put("https://interactions.ics.unisg.ch/cherrybot#ZCoordinate", z);

                Map<String, Object> rotation = new HashMap<>();
                rotation.put("https://interactions.ics.unisg.ch/cherrybot#Roll", roll);
                rotation.put("https://interactions.ics.unisg.ch/cherrybot#Pitch", pitch);
                rotation.put("https://interactions.ics.unisg.ch/cherrybot#Yaw", yaw);

                Map<String, Object> target = new HashMap<>();
                target.put("https://interactions.ics.unisg.ch/cherrybot#CoordinatesSchema", coordinate);
                target.put("https://interactions.ics.unisg.ch/cherrybot#RotationSchema", rotation);

                Map<String, Object> payload = new HashMap<>();
                payload.put("https://interactions.ics.unisg.ch/cherrybot#TcpTargetSchema", target);
                payload.put("http://www.w3.org/2001/XMLSchema#int", speed);

                System.out.println(payload);

                Optional<DataSchema> inputSchema = setTargetAction.get().getInputSchema();

                if (inputSchema.isPresent()) {

                    TDHttpRequest request = new TDHttpRequest(setTargetForm.get(), TD.invokeAction);

                    if (securityScheme.isPresent()) {

                        request.setAPIKey((APIKeySecurityScheme) securityScheme.get(), token);
                        request.setObjectPayload((ObjectSchema) inputSchema.get(), payload);
                        waitTime();
                        TDHttpResponse response = request.execute();
                        System.out.println("Received PUT Target response with status code: " + response.getStatusCode());

                    }

                }

            }

        }

    }

    // set gripper's value
    static void setGripper(String token, int value) throws IOException, InterruptedException {

        String setGripperAffordanceType = "https://interactions.ics.unisg.ch/cherrybot#SetGripper";

        Optional<ActionAffordance> setGripperAction = td.getFirstActionBySemanticType(setGripperAffordanceType);

        if (setGripperAction.isPresent()) {

            Optional<Form> setGripperForm = setGripperAction.get().getFirstFormForOperationType(TD.invokeAction);

            if (setGripperForm.isPresent()) {

                Map<String, Object> payload = new HashMap<>();
                payload.put("https://interactions.ics.unisg.ch/cherrybot#GripperValue", value);

                Optional<DataSchema> inputSchema = setGripperAction.get().getInputSchema();

                if (inputSchema.isPresent()) {

                    TDHttpRequest request = new TDHttpRequest(setGripperForm.get(), TD.invokeAction);

                    if (securityScheme.isPresent()) {

                        request.setAPIKey((APIKeySecurityScheme) securityScheme.get(), token);
                        request.setObjectPayload((ObjectSchema) inputSchema.get(), payload);
                        waitTime();
                        TDHttpResponse response = request.execute();
                        System.out.println("Received PUT Gripper response with status code: " + response.getStatusCode());

                    }

                }

            }

        }

    }

    // wait for the arm to get to target position
    static void waitForMovement(String token) throws IOException, InterruptedException {

        final double TOLERANCE = 0.01;

        double targetX = 0;
        double targetY = 0;
        double targetZ = 0;
        double targetRoll = 0;
        double targetPitch = 0;
        double targetYaw = 0;

        double currentX = 0;
        double currentY = 0;
        double currentZ = 0;
        double currentRoll = 0;
        double currentPitch = 0;
        double currentYaw = 0;

        String tcpTargetAffordanceType = "https://interactions.ics.unisg.ch/cherrybot#TcpTarget";

        Optional<PropertyAffordance> tcpTargetProperty = td.getFirstPropertyBySemanticType(tcpTargetAffordanceType);

        if (tcpTargetProperty.isPresent()) {

            Optional<Form> tcpTargetForm = tcpTargetProperty.get().getFirstFormForOperationType(TD.readProperty);

            if (tcpTargetForm.isPresent()) {

                TDHttpRequest request = new TDHttpRequest(tcpTargetForm.get(), TD.readProperty);

                if (securityScheme.isPresent()) {

                    request.setAPIKey((APIKeySecurityScheme) securityScheme.get(), token);
                    waitTime();
                    TDHttpResponse response = request.execute();
                    System.out.println("Received GET TCP Target response with status code: " + response.getStatusCode());

                    Map<String, Object> tcpTargetPayload = response.getPayloadAsObject((ObjectSchema) tcpTargetProperty.get().getDataSchema());
                    System.out.println(tcpTargetPayload);
                    targetX = (double) ((Map) tcpTargetPayload.get("https://interactions.ics.unisg.ch/cherrybot#CoordinatesSchema")).get("https://interactions.ics.unisg.ch/cherrybot#XCoordinate");
                    targetY = (double) ((Map) tcpTargetPayload.get("https://interactions.ics.unisg.ch/cherrybot#CoordinatesSchema")).get("https://interactions.ics.unisg.ch/cherrybot#YCoordinate");
                    targetZ = (double) ((Map) tcpTargetPayload.get("https://interactions.ics.unisg.ch/cherrybot#CoordinatesSchema")).get("https://interactions.ics.unisg.ch/cherrybot#ZCoordinate");
                    targetRoll = (double) ((Map) tcpTargetPayload.get("https://interactions.ics.unisg.ch/cherrybot#RotationSchema")).get("https://interactions.ics.unisg.ch/cherrybot#Roll");
                    targetPitch = (double) ((Map) tcpTargetPayload.get("https://interactions.ics.unisg.ch/cherrybot#RotationSchema")).get("https://interactions.ics.unisg.ch/cherrybot#Pitch");
                    targetYaw = (double) ((Map) tcpTargetPayload.get("https://interactions.ics.unisg.ch/cherrybot#RotationSchema")).get("https://interactions.ics.unisg.ch/cherrybot#Yaw");

                }

            }

        }

        while (true){

            String tcpAffordanceType = "https://interactions.ics.unisg.ch/cherrybot#Tcp";

            Optional<PropertyAffordance> tcpProperty = td.getFirstPropertyBySemanticType(tcpAffordanceType);

            if (tcpProperty.isPresent()) {

                Optional<Form> tcpForm = tcpProperty.get().getFirstFormForOperationType(TD.readProperty);

                if (tcpForm.isPresent()) {

                    TDHttpRequest request = new TDHttpRequest(tcpForm.get(), TD.readProperty);

                    if (securityScheme.isPresent()) {

                        request.setAPIKey((APIKeySecurityScheme) securityScheme.get(), token);
                        waitTime();
                        TDHttpResponse response = request.execute();
                        System.out.println("Received GET TCP response with status code: " + response.getStatusCode());

                        Map<String, Object> tcpPayload = response.getPayloadAsObject((ObjectSchema) tcpProperty.get().getDataSchema());
                        currentX = (double) ((Map) tcpPayload.get("https://interactions.ics.unisg.ch/cherrybot#CoordinatesSchema")).get("https://interactions.ics.unisg.ch/cherrybot#XCoordinate");
                        currentY = (double) ((Map) tcpPayload.get("https://interactions.ics.unisg.ch/cherrybot#CoordinatesSchema")).get("https://interactions.ics.unisg.ch/cherrybot#YCoordinate");
                        currentZ = (double) ((Map) tcpPayload.get("https://interactions.ics.unisg.ch/cherrybot#CoordinatesSchema")).get("https://interactions.ics.unisg.ch/cherrybot#ZCoordinate");
                        currentRoll = (double) ((Map) tcpPayload.get("https://interactions.ics.unisg.ch/cherrybot#RotationSchema")).get("https://interactions.ics.unisg.ch/cherrybot#Roll");
                        currentPitch = (double) ((Map) tcpPayload.get("https://interactions.ics.unisg.ch/cherrybot#RotationSchema")).get("https://interactions.ics.unisg.ch/cherrybot#Pitch");
                        currentYaw = (double) ((Map) tcpPayload.get("https://interactions.ics.unisg.ch/cherrybot#RotationSchema")).get("https://interactions.ics.unisg.ch/cherrybot#Yaw");

                    }

                }

            }

            if (Math.abs(currentX - targetX) <= TOLERANCE &&
                    Math.abs(currentY - targetY) <= TOLERANCE &&
                    Math.abs(currentZ - targetZ) <= TOLERANCE &&
                    Math.abs(currentRoll - targetRoll) <= TOLERANCE &&
                    Math.abs(currentPitch - targetPitch) <= TOLERANCE &&
                    Math.abs(currentYaw - targetYaw) <= TOLERANCE
            ) {

                return;

            }

        }

    }


    public static void main(String[] args) throws IOException, InterruptedException {

        final int SPEED = 400;

        // if you have not already initialized the cherrybot, uncomment the line below and initialize it
        //final String token = initialize();

        //if you have already initialized the cherry bot, uncomment the line below and define the operator's token
        final String token = "f107008e4d444297ba91a84d760a4192";

        setTarget(token, 400, 0, 400, 180, 0, 0, SPEED);
        //setTarget(token, 0, 400, 400, 180, 0, 0, SPEED);
        //setTarget(token, 400, 0, 400, 180, 0, 90, SPEED);

        //waitForMovement(token);

        //setGripper(token, 20);

    }

}
