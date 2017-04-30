/**
 * @author Andrew Davison 2007
 * @link   http://fivedots.coe.psu.ac.th/~ad/jg/javaArt1/onTheFlyArt1.pdf
 */

package lu.fisch.unimozer.compilation;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import javax.tools.FileObject;
import javax.tools.ForwardingJavaFileManager;
import javax.tools.JavaFileObject;
import javax.tools.JavaFileObject.Kind;
import javax.tools.StandardJavaFileManager;

public class ByteJavaFileManager extends ForwardingJavaFileManager
{
    // global
    private Map<String, JavaFileObject> store = new HashMap<String, JavaFileObject>();
    // maps class names to JFOs containing the classes' byte codes

    public ByteJavaFileManager(StandardJavaFileManager fileManager, Map<String, JavaFileObject> str)
    {
        super(fileManager);
        store = str;
    }  // end of ByteJavaFileManager()

    @Override
    public JavaFileObject getJavaFileForOutput(Location location, String className, Kind kind, FileObject sibling) throws IOException
    {
      try
      {
        JavaFileObject jfo = new ByteArrayJFO(className, kind);
        store.put(className, jfo);
        return jfo;
      }
      catch(Exception e)
      {
          System.out.println(e);
          return null;
      }
    } // end of getJavaFileForOutput()

}
