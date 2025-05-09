package il.cshaifasweng.OCSFMediatorExample.client;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.stage.Stage;

import java.io.IOException;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

/**
 * JavaFX App
 */
public class App extends Application {

    private static Scene scene;
    private SimpleClient client;

    @Override
    public void start(Stage stage) throws IOException {
    	EventBus.getDefault().register(this);
    	// Load the connection screen instead of automatically connecting
        scene = new Scene(loadFXML("ConnectionScreen"), 600, 400);
        stage.setScene(scene);
        stage.setTitle("Tic-Tac-Toe Game");
        stage.show();
    }

    static void setRoot(String fxml) throws IOException {
        scene.setRoot(loadFXML(fxml));
    }

    private static Parent loadFXML(String fxml) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(App.class.getResource(fxml + ".fxml"));
        return fxmlLoader.load();
    }
    
    @Override
	public void stop() throws Exception {
    	EventBus.getDefault().unregister(this);
        // Only try to disconnect if client is connected
        client = SimpleClient.getClient();
        if (client.isConnected()) {
            client.sendToServer("remove client");
            client.closeConnection();
        }
		super.stop();
	}
    
    @Subscribe
    public void onWarningEvent(WarningEvent event) {
    	Platform.runLater(() -> {
    		Alert alert = new Alert(AlertType.WARNING,
        			String.format("Message: %s\nTimestamp: %s\n",
        					event.getWarning().getMessage(),
        					event.getWarning().getTime().toString())
        	);
        	alert.show();
    	});
    }
    
    @Subscribe
    public void onGameEvent(GameEvent event) {
        // Handle game events at the application level if needed
        // This is useful for events that should be handled regardless of which controller is active
    }

	public static void main(String[] args) {
        launch();
    }

}