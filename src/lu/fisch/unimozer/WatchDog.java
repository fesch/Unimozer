/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package lu.fisch.unimozer;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.Paths;
import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;
import static java.nio.file.StandardWatchEventKinds.OVERFLOW;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;

/**
 *
 * @author robert.fisch
 */
public class WatchDog implements Runnable {
    
    private String dirname;
    
    private WatchKey watchPath;
    private WatchService watcher;
    
    private Thread thread;

    WatchDog(String directoryName) 
    {
       setDirname(directoryName);
       
       thread = new Thread(this);
       thread.start();
    }

    public void restart(String directoryName)
    {
        setDirname(dirname);
    }
    
    public void setDirname(String dirname)
    {
        this.dirname=dirname;
        
        try {
            watcher = FileSystems.getDefault().newWatchService();
            watchPath = Paths.get(dirname, "").register(watcher, ENTRY_MODIFY);
        } 
        catch (IOException ex) 
        {
           ex.printStackTrace();
        }
    }

    @Override
    public void run() 
    {
        for (;;) {

            // wait for key to be signaled
            WatchKey key;
            try {
                key = watcher.take();
            } catch (InterruptedException x) {
                return;
            }

            for (WatchEvent<?> event: key.pollEvents()) {
                WatchEvent.Kind<?> kind = event.kind();

                // This key is registered only
                // for ENTRY_CREATE events,
                // but an OVERFLOW event can
                // occur regardless if events
                // are lost or discarded.
                if (kind == OVERFLOW) {
                    continue;
                }

                // The filename is the
                // context of the event.
                WatchEvent<Path> ev = (WatchEvent<Path>)event;
                Path filename = ev.context();
                
                if (event.kind() == ENTRY_MODIFY) {
                    System.out.println("Modify: " + event.context().toString());
                }

                /*
                // Verify that the new
                //  file is a text file.
                try {
                    // Resolve the filename against the directory.
                    // If the filename is "test" and the directory is "foo",
                    // the resolved name is "test/foo".
                    Path child = watchPath.resolve(filename);
                    if (!Files.probeContentType(child).equals("text/plain")) {
                        System.err.format("New file '%s'" +
                            " is not a plain text file.%n", filename);
                        continue;
                    }
                } catch (IOException x) {
                    System.err.println(x);
                    continue;
                }


                // Email the file to the
                //  specified email alias.
                System.out.format("Emailing file %s%n", filename);
                //Details left to reader....
*/
            }

            // Reset the key -- this step is critical if you want to
            // receive further watch events.  If the key is no longer valid,
            // the directory is inaccessible so exit the loop.
            boolean valid = key.reset();
            if (!valid) {
                break;
            }
        }    
    }
    
}
