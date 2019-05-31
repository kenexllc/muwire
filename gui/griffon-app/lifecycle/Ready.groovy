import griffon.core.GriffonApplication
import groovy.util.logging.Log

import org.codehaus.griffon.runtime.core.AbstractLifecycleHandler

import com.muwire.core.Core
import com.muwire.core.MuWireSettings

import javax.annotation.Nonnull
import javax.inject.Inject
import javax.swing.JFileChooser
import javax.swing.JOptionPane

import static griffon.util.GriffonApplicationUtils.isMacOSX
import static groovy.swing.SwingBuilder.lookAndFeel

import java.beans.PropertyChangeEvent

@Log
class Ready extends AbstractLifecycleHandler {
    @Inject
    Ready(@Nonnull GriffonApplication application) {
        super(application)
    }

    @Override
    void execute() {
        log.info "starting core services"
        def home = System.getProperty("user.home") + File.separator + ".MuWire"
        home = new File(home)
        if (!home.exists()) {
            log.info("creating home dir")
            home.mkdir()
        }
        
        def props = new Properties()
        def propsFile = new File(home, "MuWire.properties")
        if (propsFile.exists()) {
            log.info("loading existing props file")
            propsFile.withInputStream {
                props.load(it)
            }
            props = new MuWireSettings(props)
        } else {
            log.info("creating new properties")
            props = new MuWireSettings()
            def nickname
            while (true) {
                nickname = JOptionPane.showInputDialog(null,
                        "Your nickname is displayed when you send search results so other MuWire users can choose to trust you",
                        "Please choose a nickname", JOptionPane.PLAIN_MESSAGE)
                if (nickname == null || nickname.trim().length() == 0) {
                    JOptionPane.showMessageDialog(null, "Nickname cannot be empty", "Select another nickname", 
                        JOptionPane.WARNING_MESSAGE)
                    continue
                }
                if (nickname.contains("@")) {
                    JOptionPane.showMessageDialog(null, "Nickname cannot contain @, choose another", 
                        "Select another nickname", JOptionPane.WARNING_MESSAGE)
                    continue
                }
                nickname = nickname.trim()
                break
            }
            
            while(true) {
                def chooser = new JFileChooser()
                chooser.setDialogTitle("Select a directory where downloads will be saved")
                chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY)
                int rv = chooser.showOpenDialog(null)
                if (rv != JFileChooser.APPROVE_OPTION) {
                    JOptionPane.showMessageDialog(null, "MuWire will now exit")
                    System.exit(0)
                }
                props.downloadLocation = chooser.getSelectedFile()
                break
            }
            props.setNickname(nickname)
            propsFile.withOutputStream {
                props.write(it)
            }
        }
        
        Core core = new Core(props, home)
        core.startServices()
        application.context.put("muwire-settings", props)
        application.context.put("core",core)
        application.getPropertyChangeListeners("core").each { 
            it.propertyChange(new PropertyChangeEvent(this, "core", null, core)) 
        }
    }
}

