/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package wptest;

import client.Postgres;
import com.sun.javafx.css.StyleManager;
import hu.daq.fileservice.FileService;
import hu.daq.login.LoginService;
import hu.daq.login.fx.LoginServiceDialogFactory;
import hu.daq.servicehandler.ServiceHandler;
import hu.daq.settings.SettingsHandler;
import hu.daq.thriftconnector.client.WPController;
import hu.daq.thriftconnector.connector.ThriftConnector;
import hu.daq.thriftconnector.talkback.WPTalkBackServer;
import hu.daq.wp.fx.screens.IntroductionScreen;
import hu.daq.wp.fx.screens.MainPage;
import hu.daq.wp.fx.screens.MainPageWMenu;
import hu.daq.wp.fx.screens.MatchScreen;
import hu.daq.wp.fx.screens.SettingsScreen;
import hu.daq.wp.fx.screens.TeamsScreen;
import hu.daq.wp.matchorganizer.OrganizerBuilder;
import java.io.FileNotFoundException;
import java.net.MalformedURLException;
import java.nio.file.Paths;
import java.util.List;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.input.KeyEvent;
import javafx.stage.Stage;
import org.apache.log4j.BasicConfigurator;
import org.apache.thrift.transport.TTransportException;
import org.json.JSONException;

/**
 *
 * @author DAQ
 */
public class WPTest extends Application {

    TeamsScreen ts;

    @Override
    public void start(Stage primaryStage) throws FileNotFoundException, TTransportException, JSONException, MalformedURLException {
        final Parameters params = getParameters();
        final List<String> parameters = params.getRaw();
        SettingsHandler settings = ServiceHandler.getInstance().getSettings();
        try {
            settings.loadProps(parameters.get(0));
        } catch (Exception ex) {
            settings.loadProps("settings.cfg");
        }
        Application.setUserAgentStylesheet(Application.STYLESHEET_MODENA);
        StyleManager.getInstance().addUserAgentStylesheet(Paths.get(settings.getProperty("css_path"), "controllerstyle.css").toUri().toURL().toExternalForm());
        BasicConfigurator.configure();
        //String jsonstr = "{\"numlegs\":2,\"legduration\":40000,\"numovertimes\":0,\"overtimeduration\":20000}";    
        LoginService ls = LoginService.getInst();
        String dburi = "jdbc:postgresql://"
                + settings.getProperty("database_url") + "/"
                + settings.getProperty("database_db") + "?tcpKeepAlive=true";
        //"?ssl=true&tcpKeepAlive=true&sslfactory=org.postgresql.ssl.NonValidatingFactory";
        ls.setDburi(dburi);
        ServiceHandler.getInstance().registerCleanup(primaryStage);
        MainPageWMenu root = new MainPageWMenu(LoginServiceDialogFactory.getLoginDialogWorking(ls));
        //root.addEventHandler(KeyEvent.KEY_PRESSED,ServiceHandler.getInstance().getKeyEventHandler());
        Postgres db = ls.getDb();
        //db.setLogging(true);
        ServiceHandler.getInstance().setDb(db);
        FileService fs = FileService.getInst();
        fs.setDb(db);
        ts = new TeamsScreen();
        MatchScreen ms = new MatchScreen(db);
        ms.addEventFilter(KeyEvent.KEY_PRESSED, ServiceHandler.getInstance().getKeyEventHandler());
        SettingsScreen ss = new SettingsScreen();
        IntroductionScreen is = new IntroductionScreen(db);
        //ServiceHandler.getInstance().setThriftClient(new WPController("192.168.71.174",19999,9998));
        //Registering two way thrift (If this works...)
        //ServiceHandler.getInstance().setOrganizer(OrganizerBuilder.build(jsonstr, ms));   
        //Setting the currentphasenum to -1 to get in sync with the display
        //ServiceHandler.getInstance().getOrganizer().setCurrentPhase(-1);
        ThriftConnector<WPTalkBackServer, WPController> tc = new ThriftConnector<WPTalkBackServer, WPController>(
                new WPTalkBackServer(ms, settings.getIntProperty("display_talkback_port")),
                //new WPController("192.168.71.174",19999,9998));
                new WPController(settings.getProperty("display_ip"),
                        settings.getIntProperty("display_port"),
                        settings.getIntProperty("display_talkback_port")));
//new WPController("localhost",19999,9998)        
        tc.getServer().startServer();
        ServiceHandler.getInstance().setThriftconnector(tc);
        //ServiceHandler.getInstance().registerCleanup(primaryStage);       

//System.out.println(db.stateProperty().get());
        //db.connect("jdbc:postgresql://192.168.71.213/waterpolo?ssl=true&tcpKeepAlive=true&sslfactory=org.postgresql.ssl.NonValidatingFactory", "daq", "nemis");
        //db.connect("jdbc:pgsql://192.168.71.213/waterpolo?ssl.mode=Require&tcpKeepAlive=true&sslfactory=org.postgresql.ssl.NonValidatingFactory", "daq","nemis");
        Scene scene = new Scene(root, 1280, 800);
        primaryStage.setTitle("Eredményjelző vezérlő");
        primaryStage.setScene(scene);
        primaryStage.show();
        root.addScreen(ts, "Csapatok/játékosok");
        root.addScreen(ms, "Meccs");
        root.addScreen(is, "Bemutatás");
        root.addScreen(ss, "Beállítások");
        root.showLogin();

    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        launch(args);
    }

}
